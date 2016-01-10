package com.example.cyril.sensortagti;

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
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
    private static final long SCAN_INTERVAL = 30000; // Performs automatic scans every 2 minutes.
    private HashMap<String, BluetoothDevice> bleDeviceMap = new HashMap<String, BluetoothDevice>();
    private HashMap<String, SensorTagConfiguration> bleTargetDevicesMap = new HashMap<String, SensorTagConfiguration>();
    private HashMap<String, Sensor> sensors = new HashMap<String, Sensor>();
    private boolean isAutomaticMode = false;
    File csvPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    File csvFile;
    CSVWriter writer;

    //private String mBluetoothDeviceAddress;
    //private BluetoothGatt mBluetoothGatt;
    private HashMap<String, BluetoothGatt> mBluetoothGattMap = new HashMap<String, BluetoothGatt>();
    private ArrayList<String> mBluetoothDeviceAddressList = new ArrayList<String>();
    //private boolean isAutomaticMode = false;

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
        //if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null)
        if (mBluetoothDeviceAddressList.contains(address) && mBluetoothGattMap.containsKey(address)) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            BluetoothGatt gatt = mBluetoothGattMap.get(address);
            if (gatt.connect())
                return true;
            /*
            if (mBluetoothGatt.connect())
                return true;
                */
        }
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.

        mBluetoothGattMap.put(address, device.connectGatt(this, false, mGattCallback));
        //mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddressList.add(address);
        //mBluetoothDeviceAddress = address;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    //TODO: allow disconnection of individual instances
    public void disconnect() {
        // Shut down the relevant Bluetooth instances.
        if (mBluetoothAdapter == null || mBluetoothGattMap.isEmpty()) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        for (BluetoothGatt gatt : mBluetoothGattMap.values()) {
            gatt.disconnect();
        }

        //mBluetoothGattMap.clear();
        //mBluetoothDeviceAddressList.clear();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    //TODO: allow closing of individual instances
    public void close() {
        if (mBluetoothGattMap.isEmpty()) {
            return;
        }

        for (BluetoothGatt gatt : mBluetoothGattMap.values()) {
            gatt.close();
        }

        mBluetoothGattMap.clear();
        mBluetoothDeviceAddressList.clear();

        //mBluetoothGatt.close();
        //mBluetoothGatt = null;
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
            Log.w(TAG, "BluetoothAdapter not initialized or GATT does not exist");
            return;
        }
        mBluetoothGattMap.get(address).setCharacteristicNotification(characteristic, enabled); // Enabled locally.
    }

    /**
     * Writes the Descriptor for the input characteristic.
     */
    public void writeDescriptor(BluetoothGattCharacteristic characteristic, String address) {
        if (mBluetoothAdapter == null || !mBluetoothGattMap.containsKey(address)) {
            Log.w(TAG, "BluetoothAdapter not initialized or GATT does not exist");
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
            Log.w(TAG, "BluetoothAdapter not initialized or GATT does not exist");
            return null;
        }

        return mBluetoothGattMap.get(address).getServices();
    }

    /**
     * Retrieves the service corresponding to the input UUID.
     */
    public BluetoothGattService getService(UUID servUuid, String address) {
        if (mBluetoothAdapter == null || !mBluetoothGattMap.containsKey(address)) {
            Log.w(TAG, "BluetoothAdapter not initialized or GATT does not exist");
            return null;
        }
        return mBluetoothGattMap.get(address).getService(servUuid);
    }


    //CW: Subsequent sections deal with background scanning and connection to pre-defined BLE sensortags
    private Runnable mStartAutomaticRunnable = new Runnable() {
        @Override
        public void run() {
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
        //Scan for Bluetooth devices with specified MAC
        //noinspection deprecation
        mBluetoothAdapter.startLeScan(mLeScanCallback);
        mHandler.postDelayed(mStopAutomaticRunnable, SCAN_PERIOD);
    }

    private void stopAutomaticScan() {
        //Toast.makeText(this, "Not Scanning", Toast.LENGTH_SHORT).show();
        //writeToCSV("Scheduled Bluetooth scan stopped.");
        Log.d(TAG, "Scheduled Bluetooth scan stopped.");
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
            stopAutomaticScan();
            writer.flushQuietly();
            try {
                writer.close();
            } catch (IOException e) {
                //unable to close csv file
                e.printStackTrace();
            }
            mHandler.removeCallbacks(mStartAutomaticRunnable);

            disconnect();
            close();

        } else {
            //automatic mode enabled
            csvFile = new File(csvPath, "sensortag_debug_" + getCurrentTimeStampForFilename() + ".csv");
            try {
                if (writer != null) {
                    writer.close();
                }
                writer = new CSVWriter(new FileWriter(csvFile, false)); //set true to append, false to overwrite file
            } catch (IOException e) {
                //unable to create/write to csv file
                e.printStackTrace();
                Log.e(TAG, "Unable to create output csv file.");
            }

            //populate target devices hashmap
            bleTargetDevicesMap = loadMap();

            //writeToCSV("Automatic mode started.");
            Log.d(TAG, "Automatic mode started.");
            startAutomaticScan();
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

        if (isAutomaticMode && bleTargetDevicesMap.containsKey(device.getAddress())) {
            if (!bleDeviceMap.containsKey(device.getAddress()) && connect(device.getAddress())) {
                // Add the device to the adapter.
                bleDeviceMap.put(device.getAddress(), device);
                Log.d(TAG, "New SensorTag connected - " + device.getAddress());
            }
        }
    }

    public void createSensors(BluetoothDevice device) {
        //latestSensorReadingMap.clear();

        if (isAutomaticMode && bleTargetDevicesMap.containsKey(device.getAddress())) {

            Sensor sensor = null;
            String address = device.getAddress();
            for (BluetoothGattService service : getSupportedGattServices(address)) {
                Log.d(TAG, "GATT Service UUID - " + service.getUuid().toString() + " - " + address);

                if (bleTargetDevicesMap.get(address).getSensorType() == SensorTagConfiguration.SensorType.TEMPERATURE && "f000aa00-0451-4000-b000-000000000000".equals(service.getUuid().toString())) {
                    sensor = new IRTSensor(service.getUuid(), this, address);
                    Log.d(TAG, "New IRT sensor created - " + address);
                } else if (bleTargetDevicesMap.get(address).getSensorType() == SensorTagConfiguration.SensorType.HUMIDITY && "f000aa20-0451-4000-b000-000000000000".equals(service.getUuid().toString())) {
                    sensor = new HumiditySensor(service.getUuid(), this, address);
                    Log.d(TAG, "New humidity sensor created - " + address);
                } else if (bleTargetDevicesMap.get(address).getSensorType() == SensorTagConfiguration.SensorType.PRESSURE && "f000aa40-0451-4000-b000-000000000000".equals(service.getUuid().toString())) {
                    sensor = new BarometerSensor(service.getUuid(), this, address);
                    Log.d(TAG, "New barometer sensor created - " + address);
                } else if (bleTargetDevicesMap.get(address).getSensorType() == SensorTagConfiguration.SensorType.BRIGHTNESS && "f000aa70-0451-4000-b000-000000000000".equals(service.getUuid().toString())) {
                    sensor = new LuxometerSensor(service.getUuid(), this, address);
                    Log.d(TAG, "New luxometer sensor created - " + address);
                } else if (bleTargetDevicesMap.get(address).getSensorType() == SensorTagConfiguration.SensorType.MOTION && "f000aa80-0451-4000-b000-000000000000".equals(service.getUuid().toString())) {
                    sensor = new MotionSensor(service.getUuid(), this, address);
                    Log.d(TAG, "New motion sensor created - " + address);
                }

            }
            if (sensor != null) {
                sensors.put(address, sensor);
            }
        }
    }

    private void updateSensorReading(byte[] value, String deviceAddress) {

        if (isAutomaticMode) {
            Sensor s = sensors.get(deviceAddress);
            s.convert(value);

            //for debugging/display purposes only
            s.getStatus().setLatestReading(s.toString());
            s.getStatus().setLatestReadingTimestamp(getCurrentShortTimeStamp());
            s.getStatus().incrementReadingsCount();

            String output = deviceAddress + "," + s.toString();
            Log.d(TAG, output);
            writeToCSV(output);
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
                Log.d(TAG, key + " - " + outputMap.get(key).getSensorType());
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

    private void writeToCSV(String s) {
        if (writer != null) {
            s = getCurrentTimeStamp() + "," + s;
            writer.writeNext(s.split(","));
            try {
                writer.flush();
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
        statusUpdates.add(bleDeviceMap.size() + " SensorTags connected this session.");
        for (BluetoothDevice device : bleDeviceMap.values()) {
            Sensor s = sensors.get(device.getAddress());
            statusUpdates.add(device.getAddress() + " - " + bleTargetDevicesMap.get(device.getAddress()).getSensorType());
            statusUpdates.add(device.getAddress() + " - Updated: " + s.getStatus().getLatestReadingTimestamp());
            statusUpdates.add(device.getAddress() + " - Total Readings: " + s.getStatus().getReadingsCount());
        }
        return statusUpdates;
    }

    public String getCurrentTimeStamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
    }

    public String getCurrentShortTimeStamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    public String getCurrentTimeStampForFilename() {
        return new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
    }
}
