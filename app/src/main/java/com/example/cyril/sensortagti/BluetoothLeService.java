package com.example.cyril.sensortagti;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {

    private final static String TAG = BluetoothLeService.class.getSimpleName();
    // Bluetooth instances.
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler = new Handler();

    private static final long SCAN_PERIOD = 6000; // Stops scanning after 6 seconds.
    private static final long SCAN_INTERVAL = 60000; // Performs automatic scans every 1 minutes
    private static final long SENSOR_INACTIVITY_THRESHOLD = 120000; // Set threshold for sensor inactivity to 2 minutes
    private static final long FILE_SPLIT_INTERVAL = 7200000*6; // Set interval for file splitting to 12 hours
    private static final long CSV_WRITE_INTERVAL = 300000;
    private HashMap<String, BluetoothDevice> mBluetoothDeviceMap = new HashMap<String, BluetoothDevice>();
    private HashMap<String, BluetoothGatt> mBluetoothGattMap = new HashMap<String, BluetoothGatt>();
    private HashMap<String, SensorTagConfiguration> mBluetoothTargetDevicesMap = new HashMap<String, SensorTagConfiguration>();
    private HashMap<String, ArrayList<Sensor>> mSensorsMap = new HashMap<String, ArrayList<Sensor>>();
    private boolean isAutomaticMode = false;
    private boolean outputDebug = false;
    private Date mFileCreatedTime;
    private Date mFileLastWriteTime;
    File csvPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    File csvFile;
    CSVWriter writer;
    File debugFile;
    CSVWriter debugWriter;
    private ArrayList<String> csvBuffer = new ArrayList<String>();

    // Actions.
    public final static String ACTION_GATT_CONNECTED =
            "com.example.cyril.sensortagti.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.cyril.sensortagti.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.cyril.sensortagti.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_READ =
            "com.example.cyril.sensortagti.ACTION_DATA_READ";
    public final static String ACTION_DATA_NOTIFY =
            "com.example.cyril.sensortagti.ACTION_DATA_NOTIFY";
    public final static String ACTION_DATA_WRITE =
            "com.example.cyril.sensortagti.ACTION_DATA_WRITE";
    public final static String EXTRA_DATA =
            "com.example.cyril.sensortagti.EXTRA_DATA";
    public final static String EXTRA_UUID =
            "com.example.cyril.sensortagti.EXTRA_UUID";
    public final static String EXTRA_DEVICEADDRESS =
            "com.example.cyril.sensortagti.EXTRA_DEVICEADDRESS";

    /**
     * Implements callback methods for GATT events that the app cares about.
     * For example, connection change and services discovered.
     */
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" + gatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);

                String address = gatt.getDevice().getAddress();
                if (outputDebug) {
                    writeToDebugCSV(address + "," + "BluetoothGattCallback(STATE_DISCONNECTED)");
                }
                closeDevice(address);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                boolean isGood = true;
                for (int i = 0; i < gatt.getServices().size(); i++) {
                    BluetoothGattService bgs = gatt.getServices().get(i);
                    Log.w(TAG, "found service " + bgs.getUuid().toString());
                    Log.w(TAG, bgs.getCharacteristics().toString());
                    if (bgs.getCharacteristics().size() == 0)
                        isGood = false;
                }
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                createSensors(gatt.getDevice());
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic,
                                          int status) {
            Log.w(TAG, "onCharacteristicWrite received: " + status);
            broadcastUpdate(ACTION_DATA_WRITE, characteristic, gatt.getDevice().getAddress());
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "onCharacteristicRead received: " + status);
                broadcastUpdate(ACTION_DATA_READ, characteristic, gatt.getDevice().getAddress());
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            Log.i(TAG, "onCharacteristicChanged received: ");
            broadcastUpdate(ACTION_DATA_NOTIFY, characteristic, gatt.getDevice().getAddress());
            updateSensorReading(characteristic.getValue(), gatt.getDevice().getAddress());
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.i(TAG, "onDescriptorRead received: " + descriptor.getUuid().toString());
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.i(TAG, "onDescriptorWrite received: " + descriptor.getUuid().toString());
        }

    };

    /**
     * Broadcast update.
     */
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    /**
     * Broadcast update.
     */
    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic, String deviceAddress) {
        final Intent intent = new Intent(action);
        final byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {
            intent.putExtra(EXTRA_DATA, characteristic.getValue());
            intent.putExtra(EXTRA_UUID, characteristic.getUuid().toString());
            intent.putExtra(EXTRA_DEVICEADDRESS, deviceAddress);
        }
        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
// After using a given device, you should make sure that BluetoothGatt.close() is called
// such that resources are cleaned up properly.  In this particular example, close() is
// invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (outputDebug) {
            writeToDebugCSV("NA," + "onStartCommand()");
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        Boolean startInAutomaticMode = prefs.getBoolean("automaticModeEnabled", false);
        if (startInAutomaticMode.booleanValue()) {
            setAutomaticMode(true);
        }

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        writeToCSV();
        if (outputDebug) {
            writeToDebugCSV("NA," + "onDestroy()");
        }
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceMap.containsKey(address) && mBluetoothGattMap.containsKey(address)) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            BluetoothGatt gatt = mBluetoothGattMap.get(address);
            if (gatt.connect())
                return true;
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.

        mBluetoothGattMap.put(address, device.connectGatt(this, false, mGattCallback));

        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceMap.put(address, device);

        if (outputDebug) {
            writeToDebugCSV(address + "," + "connect()");
        }

        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        // Disconnect all relevant Bluetooth GATT instances.
        if (mBluetoothAdapter == null || mBluetoothGattMap.isEmpty()) {
            Log.w(TAG, "Disconnect: BluetoothAdapter not initialized");
            return;
        }

        for (BluetoothGatt gatt : mBluetoothGattMap.values()) {
            gatt.disconnect();
        }
    }

    public void disconnectDevice(String address) {
        // Disconnect the relevant Bluetooth GATT instance.
        // Disconnect preserves the GATT service for use again, and results in a callback for DISCONNECTED
        if (mBluetoothAdapter == null || mBluetoothGattMap.isEmpty()) {
            Log.w(TAG, "DisconnectDevice: BluetoothAdapter not initialized");
            return;
        }

        BluetoothGatt gatt = mBluetoothGattMap.get(address);
        gatt.disconnect();

        if (outputDebug) {
            writeToDebugCSV(address + "," + "disconnectDevice()");
        }
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        // Close all relevant Bluetooth instances.
        if (mBluetoothGattMap.isEmpty()) {
            return;
        }

        ArrayList<String> addresses = new ArrayList<String>();
        for (String address : mBluetoothGattMap.keySet()) {
            addresses.add(address);
        }
        for (String address : addresses) {
            closeDevice(address);
        }
    }

    public void closeDevice(String address) {
        // Close the relevant Bluetooth instance.
        // This is one level higher than disconnect and releases all resources (including the GATT service)
        // However, it does not trigger a DISCONNECTED callback
        if (mBluetoothGattMap.isEmpty()) {
            return;
        }

        BluetoothGatt gatt = mBluetoothGattMap.get(address);
        gatt.close();

        //clear out all existence of the remote device upon closure of the connection
        mBluetoothGattMap.remove(address);
        mBluetoothDeviceMap.remove(address);
        ArrayList<Sensor> sensors = mSensorsMap.get(address);
        if (sensors != null) {
            for (Sensor sensor : sensors) {
                if (sensor != null) {
                    sensor.disable();
                }
            }
        }
        mSensorsMap.remove(address);

        if (outputDebug) {
            writeToDebugCSV(address + "," + "closeDevice()");
        }
    }

    /**
     * Writes a characteristic.
     */
    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic, String address) {
        return mBluetoothGattMap.get(address).writeCharacteristic(characteristic);
        //return true;
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled, String address) {
        if (mBluetoothAdapter == null || !mBluetoothGattMap.containsKey(address)) {
            Log.w(TAG, address + ": setCharacteristicNotification - BluetoothAdapter not initialized or GATT does not exist");
            return;
        }
        mBluetoothGattMap.get(address).setCharacteristicNotification(characteristic, enabled); // Enabled locally.
    }

    /**
     * Writes the Descriptor for the input characteristic.
     */
    public void writeDescriptor(BluetoothGattCharacteristic characteristic, String address) {
        if (mBluetoothAdapter == null || !mBluetoothGattMap.containsKey(address)) {
            Log.w(TAG, address + ": writeDescriptor - BluetoothAdapter not initialized or GATT does not exist");
            return;
        }
        BluetoothGattDescriptor clientConfig = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
        clientConfig.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGattMap.get(address).writeDescriptor(clientConfig);
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices(String address) {
        if (mBluetoothAdapter == null || !mBluetoothGattMap.containsKey(address)) {
            Log.w(TAG,  address + ": getSupportedGattServices - BluetoothAdapter not initialized or GATT does not exist");
            return null;
        }

        return mBluetoothGattMap.get(address).getServices();
    }

    /**
     * Retrieves the service corresponding to the input UUID.
     */
    public BluetoothGattService getService(UUID servUuid, String address) {
        if (mBluetoothAdapter == null || !mBluetoothGattMap.containsKey(address)) {
            Log.w(TAG,  address + ": getService: BluetoothAdapter not initialized or GATT does not exist");
            return null;
        }
        return mBluetoothGattMap.get(address).getService(servUuid);
    }

    //CW: Subsequent sections deal with background scanning and connection to pre-defined BLE sensortags
    private Runnable mStartAutomaticRunnable = new Runnable() {
        @Override
        public void run() {
            //first check whether there are inactive sensortags that needs to be disconnected
            checkSensorTagInactivity();

            //write to CSV at fixed intervals
            if ((new Date()).getTime() - mFileLastWriteTime.getTime() > CSV_WRITE_INTERVAL) {
                writeToCSV();
                mFileLastWriteTime = new Date();
            }

            //check whether CSV file is due for renewal
            if ((new Date()).getTime() - mFileCreatedTime.getTime() > FILE_SPLIT_INTERVAL) {
                createNewCSVFile();
            }

            //next begin to scan for advertising sensortags
            startAutomaticScan();
        }
    };

    private Runnable mStopAutomaticRunnable = new Runnable() {
        @Override
        public void run() {
            stopAutomaticScan();
        }
    };

    private void startAutomaticScan() {
        //Toast.makeText(this, "Scanning", Toast.LENGTH_SHORT).show();
        //writeToCSV("Scheduled Bluetooth scan started.");
        Log.d(TAG, "Scheduled Bluetooth scan started.");
        if (outputDebug) {
            writeToDebugCSV("NA," + "startAutomaticScan()");
        }
        //Scan for Bluetooth devices with specified MAC
        //noinspection deprecation
        mBluetoothAdapter.startLeScan(mLeScanCallback);
        mHandler.postDelayed(mStopAutomaticRunnable, SCAN_PERIOD);
    }

    private void stopAutomaticScan() {
        //Toast.makeText(this, "Not Scanning", Toast.LENGTH_SHORT).show();
        //writeToCSV("Scheduled Bluetooth scan stopped.");
        Log.d(TAG, "Scheduled Bluetooth scan stopped.");
        if (outputDebug) {
            writeToDebugCSV("NA," + "stopAutomaticScan()");
        }
        //noinspection deprecation
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
        if (isAutomaticMode) {
            //only schedule next scan if automatic mode is enabled
            mHandler.postDelayed(mStartAutomaticRunnable, SCAN_INTERVAL);
            //writeToCSV("Next Bluetooth scan scheduled.");
            Log.d(TAG, "Next Bluetooth scan scheduled.");
        }
    }

    public void setAutomaticMode(boolean mode) {
        this.isAutomaticMode = mode;
        if (!mode) {
            //prevent any previously scheduled scan from starting
            //writeToCSV("Automatic mode stopped.");
            Log.d(TAG, "Automatic mode stopped.");
            if (outputDebug) {
                writeToDebugCSV("NA," + "setAutomaticMode(false)");
            }
            writeToCSV();
            stopAutomaticScan();
            if (writer != null) {
                try {
                    writer.flush();
                    writer.close();
                } catch (IOException e) {
                    //unable to close csv file
                    e.printStackTrace();
                }
            }
            mHandler.removeCallbacks(mStartAutomaticRunnable);

            //remove foreground notification
            stopForeground(true);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
            prefs.edit().putBoolean("automaticModeEnabled", false).commit();


            disconnect();
            close();

        } else {

            if (outputDebug) {
                debugFile = new File(csvPath, "sensortag_debug_" + getCurrentTimeStampForFilename() + ".csv");
                try {
                    if (debugWriter != null) {
                        debugWriter.close();
                    }
                    debugWriter = new CSVWriter(new FileWriter(debugFile, false)); //set true to append, false to overwrite file

                } catch (IOException e) {
                    //unable to create/write to csv file
                    e.printStackTrace();
                    Log.e(TAG, "Unable to create output csv file.");
                }
            }

            if (outputDebug) {
                writeToDebugCSV("NA," + "setAutomaticMode(true)");
            }

            //create new CSV file
            createNewCSVFile();
            mFileLastWriteTime = new Date();

            //populate target devices hashmap
            mBluetoothTargetDevicesMap = loadMap();

            //writeToCSV("Automatic mode started.");
            Log.d(TAG, "Automatic mode started.");
            startAutomaticScan();

            Notification.Builder builder = new Notification.Builder(this.getApplicationContext());

            Intent intent = new Intent(this.getApplicationContext(), BluetoothLeService.class);
            PendingIntent pendingIntent
                    = PendingIntent.getActivity(this.getApplicationContext(), 0, intent, 0);

            Notification notification = builder
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("SensorTagTI Data Capture")
                    .setContentText("Automatic Mode Enabled")
                    .setContentInfo("ContentInfo")
                    .setAutoCancel(true)
                    .build();

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
            prefs.edit().putBoolean("automaticModeEnabled", true).commit();

            startForeground(54330216, notification);
        }
    }

    private void createNewCSVFile() {
        //automatic mode enabled

        csvFile = new File(csvPath, "sensortag_datacapture_" + getCurrentTimeStampForFilename() + ".csv");
        try {
            if (writer != null) {
                writer.close();
            }
            writer = new CSVWriter(new FileWriter(csvFile, false)); //set true to append, false to overwrite file
            mFileCreatedTime = new Date();

            if (outputDebug) {
                writeToDebugCSV("NA," + "createNewCSVFile()");
            }

        } catch (IOException e) {
            //unable to create/write to csv file
            e.printStackTrace();
            Log.e(TAG, "Unable to create output csv file.");
        }
    }

    /**
     * Device scan callback.
     */
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {

            // Check if the device is a SensorTag.
            String deviceName = device.getName();
            if (deviceName == null)
                return;
            if (!(deviceName.equals("SensorTag") || deviceName.equals("TI BLE Sensor Tag") || deviceName.equals("CC2650 SensorTag")))
                return;

            //writeToCSV(device.getAddress() + " found.");
            Log.d(TAG, device.getAddress() + " found.");

            connectDevice(device);
        }
    };

    public void connectDevice(BluetoothDevice device) {

        if (isAutomaticMode && mBluetoothTargetDevicesMap.containsKey(device.getAddress())) {
            if (connect(device.getAddress())) {
                Log.d(TAG, "New SensorTag connected - " + device.getAddress());
            }
        }
    }

    public void createSensors(BluetoothDevice device) {
        if (isAutomaticMode && mBluetoothTargetDevicesMap.containsKey(device.getAddress())) {
            ArrayList<Sensor> sensors = new ArrayList<Sensor>();
            String address = device.getAddress();

            for (BluetoothGattService service : getSupportedGattServices(address)) {
                Sensor sensor = null;
                for (SensorTagConfiguration.SensorType sensorType : mBluetoothTargetDevicesMap.get(address).getSensorTypes()) {
                    Log.d(TAG, "GATT Service UUID - " + service.getUuid().toString() + " - " + address);
                    if (sensorType == SensorTagConfiguration.SensorType.TEMPERATURE && "f000aa00-0451-4000-b000-000000000000".equals(service.getUuid().toString())) {
                        sensor = new IRTSensor(service.getUuid(), this, address);
                        Log.d(TAG, "New IRT sensor created - " + address);
                    } else if (sensorType == SensorTagConfiguration.SensorType.HUMIDITY && "f000aa20-0451-4000-b000-000000000000".equals(service.getUuid().toString())) {
                        sensor = new HumiditySensor(service.getUuid(), this, address);
                        Log.d(TAG, "New humidity sensor created - " + address);
                    } else if (sensorType == SensorTagConfiguration.SensorType.PRESSURE && "f000aa40-0451-4000-b000-000000000000".equals(service.getUuid().toString())) {
                        sensor = new BarometerSensor(service.getUuid(), this, address);
                        Log.d(TAG, "New barometer sensor created - " + address);
                    } else if (sensorType == SensorTagConfiguration.SensorType.BRIGHTNESS && "f000aa70-0451-4000-b000-000000000000".equals(service.getUuid().toString())) {
                        sensor = new LuxometerSensor(service.getUuid(), this, address);
                        Log.d(TAG, "New luxometer sensor created - " + address);
                    } else if (sensorType == SensorTagConfiguration.SensorType.MOTION && "f000aa80-0451-4000-b000-000000000000".equals(service.getUuid().toString())) {
                        sensor = new MotionSensor(service.getUuid(), this, address);
                        Log.d(TAG, "New motion sensor created - " + address);
                    }
                }
                if (sensor != null) {
                    sensors.add(sensor);

                    if (outputDebug) {
                        writeToDebugCSV(address + "," + "createSensors() - " + sensor.getSensorType());
                    }
                }
            }

            mSensorsMap.put(address, sensors);
        }
    }

    private void updateSensorReading(byte[] value, String deviceAddress) {

        if (isAutomaticMode) {

            ArrayList<Sensor> sensors = mSensorsMap.get(deviceAddress);
            if (sensors != null) {
                for (Sensor s : sensors) {
                    s.receiveNotification();
                    s.convert(value);
                    //for debugging/display purposes only
                    s.getStatus().setLatestReading(s.toString());
                    s.getStatus().setLatestReadingTimestamp(new Date());
                    s.getStatus().incrementReadingsCount();

                    String output = deviceAddress + "," + s.toString();
                    Log.d(TAG, output);
                    //writeToCSV(output);
                    addToCSVBuffer(output);
                }
            }
            //latestSensorReadingMap.put(deviceAddress,s.toString());
        }
    }

    private HashMap<String, SensorTagConfiguration> loadMap() {
        HashMap<String, SensorTagConfiguration> outputMap = new HashMap<String, SensorTagConfiguration>();
        try {
            FileInputStream fis = getApplicationContext().openFileInput("configdata");
            ObjectInputStream is = new ObjectInputStream(fis);
            outputMap = (HashMap<String, SensorTagConfiguration>) is.readObject();
            Log.d(TAG, "Verifying config data");
            for (String key : outputMap.keySet()) {
                Log.d(TAG, key + " - " + outputMap.get(key).getSensorTypes());
            }
            is.close();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return outputMap;
    }

    private void addToCSVBuffer(String s) {
        s = getCurrentTimeStamp() + "," + s;
        csvBuffer.add(s);
    }

    private void writeToCSV() {

        ArrayList<String> outputBuffer = csvBuffer;
        csvBuffer = new ArrayList<String>();

        if (writer != null) {
            for (String line : outputBuffer) {
                writer.writeNext(line.split(","));
            }
            try {
                writer.flush();
                outputBuffer.clear();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private void writeToDebugCSV(String s) {
        if (debugWriter != null) {
            s = getCurrentTimeStamp() + "," + s;
            debugWriter.writeNext(s.split(","));
            try {
                debugWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public boolean isAutomaticMode() {
        return isAutomaticMode;
    }

    public ArrayList<String> getStatusUpdates() {
        ArrayList<String> statusUpdates = new ArrayList<String>();
        statusUpdates.add(mBluetoothDeviceMap.size() + " SensorTags connected.");
        for (BluetoothDevice device : mBluetoothDeviceMap.values()) {
            ArrayList<Sensor> sensors = mSensorsMap.get(device.getAddress());
            if (sensors != null) {
                for (Sensor s : sensors) {
                    statusUpdates.add("Device: " + device.getAddress());
                    statusUpdates.add("Type: " + s.getSensorType());
                    statusUpdates.add("Updated: " + s.getStatus().getLatestReadingTimestampString());
                    statusUpdates.add("Total Readings: " + s.getStatus().getReadingsCount());
                }
            }
        }
        return statusUpdates;
    }

    public void checkSensorTagInactivity() {

        if (outputDebug) {
            writeToDebugCSV("NA," + "checkSensorTagInactivity()");
        }

        Date currentTime = new Date();
        ArrayList<String> inactiveDevices = new ArrayList<String>();
        for (String address : mBluetoothDeviceMap.keySet()) {
            boolean inactive = false;

            ArrayList<Sensor> sensors = mSensorsMap.get(address);
            if (sensors != null) {
                for (Sensor sensor : sensors) {
                    if (sensor != null) {
                        Date lastUpdated = sensor.getStatus().getLatestReadingTimestamp();
                        if (currentTime.getTime() - lastUpdated.getTime() > SENSOR_INACTIVITY_THRESHOLD) {
                            //check if the last updated time has elapsed by more than the threshold
                            if (outputDebug) {
                                writeToDebugCSV(address + "," + "SensorTag Inactive," + getCurrentShortTimeStamp(currentTime) + "," + getCurrentShortTimeStamp(lastUpdated));
                            }
                            inactive = true;
                            break;
                        }
                    } else {
                        //unlikely to be null since we created it previously.
                        //but set to inactive as a failsafe if it does occur so we can reconnect properly
                        inactive = true;
                    }
                }
            }
            if (inactive) {
                inactiveDevices.add(address);
            }
        }

        for (String address : inactiveDevices) {
            //close the bluetooth connection if sensortag has been inactive beyond desired threshold
            //upon closing, the sensortag should enter advertising mode, giving us the chance to reconnect to it
            disconnectDevice(address);
            closeDevice(address);
        }
    }


    public String getCurrentTimeStamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
    }

    public String getCurrentShortTimeStamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    public String getCurrentShortTimeStamp(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }

    public String getCurrentTimeStampForFilename() {
        return new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
    }
}
