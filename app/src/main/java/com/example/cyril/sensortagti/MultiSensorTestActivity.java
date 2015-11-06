package com.example.cyril.sensortagti;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MultiSensorTestActivity extends Activity {

    private final static String TAG = DeviceScanActivity.class.getSimpleName();

    // Scan instances.
    //private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    // Scan parameters.
    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 6000; // Stops scanning after 3 seconds.
    //private ArrayList<String> mDeviceAddressList = new ArrayList<>();

    private HashMap<String,Integer> uuidToIndex=new HashMap<>(); // dataUuid to index
    private HashMap<String,Sensor> sensors=new HashMap<String,Sensor>();
    private HashMap<String,BluetoothDevice> bleDeviceMap = new HashMap<String,BluetoothDevice>();
    private HashMap<String,String> latestSensorReadingMap= new HashMap<String,String>();
    private TextView txtView;

    //CW
    private BluetoothLeService mBluetoothLeService;


    /**
     * Code to manage Service lifecycle.
     */
    private final ServiceConnection mServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize())
            {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_sensor_test);

        txtView = (TextView) findViewById(R.id.outputTextView);

        mHandler = new Handler();
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
        {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager=(BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter=bluetoothManager.getAdapter();
        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null)
        {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Bind BluetoothLeService.
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    public void initiateNewScan(View v) {
        mBluetoothLeService.close();
        bleDeviceMap.clear();
        sensors.clear();
        txtView.setText("");
        scanLeDevice(true);
    }


    public void connectAllLeDevice(View v) {
        txtView.setText("");
        for (String address : bleDeviceMap.keySet()) {
            if (mBluetoothLeService.connect(address)) {
                //Log.d(TAG, "New sensor connected - " + address);
                txtView.setText(txtView.getText()+"\n"+ "New device connected - " + address);
            }
        }
    }

    public void createSensors(View v) {
        txtView.setText("");
        latestSensorReadingMap.clear();
        for (String address : bleDeviceMap.keySet()) {
            Sensor sensor = null;
            for (BluetoothGattService service : mBluetoothLeService.getSupportedGattServices(address)) {
                Log.d(TAG, "GATT Service UUID - " + service.getUuid().toString() + " - " + address);
                if ("f000aa70-0451-4000-b000-000000000000".equals(service.getUuid().toString())) {
                    sensor = new LuxometerSensor(service.getUuid(),mBluetoothLeService,address);
                    //Log.d(TAG, "New lux sensor created - " + address);
                    txtView.setText(txtView.getText()+"\n"+ "New lux sensor created - " + address);
                    break;
                }
            }
            if (sensor != null) {
                sensors.put(address,sensor);
            }
        }
    }


    public void disconnectAllLeDevice(View v) {
        txtView.setText("");
        mBluetoothLeService.disconnect();
    }

    public void closeAllLeDevice(View v) {
        mBluetoothLeService.close();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled())
        {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        // Initializes list view adapter.
        bleDeviceMap=new HashMap<String,BluetoothDevice>();

        // Register for the BroadcastReceiver.
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED)
        {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        scanLeDevice(false);
        bleDeviceMap.clear();
    }

    private void scanLeDevice(final boolean enable)
    {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    mScanning = false;
                    //noinspection deprecation
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);
            mScanning = true;
            //noinspection deprecation
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            //noinspection deprecation
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    /**
     * Device scan callback.
     */
    private BluetoothAdapter.LeScanCallback mLeScanCallback=new BluetoothAdapter.LeScanCallback()
    {
        @Override
        public void onLeScan(final BluetoothDevice device,final int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    // Check if the device is a SensorTag.
                    String deviceName=device.getName();
                    if(deviceName==null)
                        return;
                    if(!(deviceName.equals("SensorTag")||deviceName.equals("TI BLE Sensor Tag")||deviceName.equals("CC2650 SensorTag")))
                        return;

                    // Add the device to the adapter.
                    //mDeviceAddressList.add(device.getAddress());
                    bleDeviceMap.put(device.getAddress(), device);
                    displayAvailableDevices();
                    //txtView.setText(txtView.getText() + "\n" + "Device: " + device.getAddress());

                }
            });
        }
    };


    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action))
            {

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action))
            {
                for(Sensor s:sensors.values())
                    s.disable();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action))
            {
                // Nothing to do.
            } else if (BluetoothLeService.ACTION_DATA_READ.equals(action))
            {
                // Nothing to do.
            } else if (BluetoothLeService.ACTION_DATA_NOTIFY.equals(action))
            {
                String uuidStr = intent.getStringExtra(BluetoothLeService.EXTRA_UUID);
                byte[] value = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                String deviceAddress = intent.getStringExtra(BluetoothLeService.EXTRA_DEVICEADDRESS);
                onCharacteristicChanged(uuidStr, value, deviceAddress);
            }
        }
    };

    /**
     * Gets called when there is a data notification.
     */
    private void onCharacteristicChanged(String uuidStr,byte[] value, String deviceAddress) {
        Sensor s = sensors.get(deviceAddress);
        s.receiveNotification();
        s.convert(value);

        //String output = "Device: " + deviceAddress +  " - Lux: " + s.toString();
        //Log.d(TAG, output);
        latestSensorReadingMap.put(deviceAddress,s.toString());
        displayLatestReadings();
        //this.mDataValues.get(index).setText(s.toString());
    }


    private void displayAvailableDevices() {
        txtView.setText("Devices Available");
        for (String entry : bleDeviceMap.keySet()) {
            txtView.append("\n" + "Device: " + entry);
        }
    }

    private void displayLatestReadings() {
        txtView.setText("Sensor Readings");
        for (Map.Entry<String, String> entry : latestSensorReadingMap.entrySet()) {
            txtView.append("\n" + "Device: " + entry.getKey() +  " - Lux: " + entry.getValue());
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_READ);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_NOTIFY);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_WRITE);
        return intentFilter;
    }

}
