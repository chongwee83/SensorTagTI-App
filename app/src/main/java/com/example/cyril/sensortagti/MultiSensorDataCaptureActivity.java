package com.example.cyril.sensortagti;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MultiSensorDataCaptureActivity extends Activity {

    private final static String TAG = DeviceScanActivity.class.getSimpleName();

    // Scan instances.
    //private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    // Scan parameters.
    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 6000; // Stops scanning after 6 seconds.
    //private ArrayList<String> mDeviceAddressList = new ArrayList<>();

    private HashMap<String, Integer> uuidToIndex = new HashMap<>(); // dataUuid to index
    private HashMap<String, SensorTagConfiguration> bleDeviceConfigMap = new HashMap<String, SensorTagConfiguration>();
    private HashMap<String, String> latestSensorReadingMap = new HashMap<String, String>();

    //CW
    private BluetoothLeService mBluetoothLeService;


    /**
     * Code to manage Service lifecycle.
     */
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
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
        setContentView(R.layout.activity_multi_sensor_datacapture);

        mHandler = new Handler();
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Bind BluetoothLeService.
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    public void startAutomaticMode(View v) {
        mBluetoothLeService.setAutomaticMode(true);
    }

    public void stopAutomaticMode(View v) {
        mBluetoothLeService.setAutomaticMode(false);
    }

    public void configureNewDevice(View v) {
        mBluetoothLeService.close();
        scanLeDevice(true);
    }

    public void resetConfiguration(View v) {
        saveMap(new HashMap<String, SensorTagConfiguration>());
        bleDeviceConfigMap = loadMap();
        //refreshConfigMultiLineText();
    }

    public void displayConfiguration(View v) {

        String configText = "";
        for (Map.Entry entry : bleDeviceConfigMap.entrySet()) {
            String address = (String) entry.getKey();
            SensorTagConfiguration config = (SensorTagConfiguration) entry.getValue();
            configText = configText + address + " - \n" + config.getSensorTypes() + "\n";
        }

        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("SensorTag Configuration");
        alertDialog.setMessage(configText);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    public void showLatestStatus(View v) {

        String updateText = "";
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();


        if (mBluetoothLeService.isAutomaticMode()) {
            //txtView.setText("Automatic Mode Enabled");
            alertDialog.setTitle("Automatic Mode Enabled");
            for (String update : mBluetoothLeService.getStatusUpdates()) {
                //txtView.setText(txtView.getText() + "\n" + update);
                updateText = updateText + "\n" + update;
            }
        } else {
            //txtView.setText("Automatic Mode Disabled");
            alertDialog.setTitle("Automatic Mode Disabled");
            updateText = "Automatic Mode is currently not running.";
        }


        alertDialog.setMessage(updateText);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();


    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        // Read configuration file
        bleDeviceConfigMap = loadMap();
        //refreshConfigMultiLineText();

        // Register for the BroadcastReceiver.
        //registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    /*
    public void refreshConfigMultiLineText() {
        editText.setText("");
        for (Map.Entry entry : bleDeviceConfigMap.entrySet()) {
            String address = (String) entry.getKey();
            SensorTagConfiguration config = (SensorTagConfiguration) entry.getValue();
            editText.setText(editText.getText() + address + " - " + config.getSensorTypes() + "\n");
        }
    }*/

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
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
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Check if the device is a SensorTag.
                    String deviceName = device.getName();
                    if (deviceName == null)
                        return;
                    if (!(deviceName.equals("SensorTag") || deviceName.equals("TI BLE Sensor Tag") || deviceName.equals("CC2650 SensorTag")))
                        return;

                    addConfiguration(device);
                }
            });
        }
    };


    private void addConfiguration(BluetoothDevice device) {
        //Display dialog for user to select desired sensortype
        final String address = device.getAddress();
        showMultiSelectDialog(address);

        /*
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Desired Sensor Type (" + address + ")");
        CharSequence options[] = new CharSequence[]{"Motion", "Brightness", "Humidity", "Temperature", "Pressure", "Cancel and Exit"};
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int index) {
                SensorTagConfiguration config = new SensorTagConfiguration();
                switch (index) {
                    case 0:
                        config.addSensorType(SensorTagConfiguration.SensorType.MOTION);
                        break;
                    case 1:
                        config.addSensorType(SensorTagConfiguration.SensorType.BRIGHTNESS);
                        break;
                    case 2:
                        config.addSensorType(SensorTagConfiguration.SensorType.HUMIDITY);
                        break;
                    case 3:
                        config.addSensorType(SensorTagConfiguration.SensorType.TEMPERATURE);
                        break;
                    case 4:
                        config.addSensorType(SensorTagConfiguration.SensorType.PRESSURE);
                        break;
                    case 5:
                        break;
                }
                if (!config.getSensorTypes().isEmpty()) {
                    bleDeviceConfigMap.put(address, config);
                }
                saveMap(bleDeviceConfigMap);
                refreshConfigMultiLineText();
            }
        });
        builder.show();
        */
    }

    private void showMultiSelectDialog(String devAddress){
        final CharSequence[] items = {"Motion", "Brightness", "Humidity", "Temperature", "Pressure"};

        final String address = devAddress;

        // arraylist to keep the selected items
        final ArrayList<Integer> selectedItems=new ArrayList<Integer>();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Desired Sensor Type (" + address + ")");
        builder.setMultiChoiceItems(items, null,
                new DialogInterface.OnMultiChoiceClickListener() {
                    // indexSelected contains the index of item (of which checkbox checked)
                    @Override
                    public void onClick(DialogInterface dialog, int indexSelected, boolean isChecked) {
                        if (isChecked) {
                            // If the user checked the item, add it to the selected items
                            // write your code when user checked the checkbox
                            selectedItems.add(indexSelected);
                        } else if (selectedItems.contains(indexSelected)) {
                            // Else, if the item is already in the array, remove it
                            // write your code when user Unchecked the checkbox
                            selectedItems.remove(Integer.valueOf(indexSelected));
                        }
                    }
                })
                // Set the action buttons
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        //  Your code when user clicked on OK
                        //  You can write the code to save the selected item here
                        SensorTagConfiguration config = new SensorTagConfiguration();
                        for (Integer selected : selectedItems) {
                            switch (selected.intValue()) {
                                case 0:
                                    config.addSensorType(SensorTagConfiguration.SensorType.MOTION);
                                    break;
                                case 1:
                                    config.addSensorType(SensorTagConfiguration.SensorType.BRIGHTNESS);
                                    break;
                                case 2:
                                    config.addSensorType(SensorTagConfiguration.SensorType.HUMIDITY);
                                    break;
                                case 3:
                                    config.addSensorType(SensorTagConfiguration.SensorType.TEMPERATURE);
                                    break;
                                case 4:
                                    config.addSensorType(SensorTagConfiguration.SensorType.PRESSURE);
                                    break;
                                case 5:
                                    break;
                            }
                        }
                        if (!config.getSensorTypes().isEmpty()) {
                            bleDeviceConfigMap.put(address, config);
                        }
                        saveMap(bleDeviceConfigMap);
                        //refreshConfigMultiLineText();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        //  Your code when user clicked on Cancel

                    }
                });

        AlertDialog dialog = builder.create();//AlertDialog dialog; create like this outside onClick
        dialog.show();
    }


    private void saveMap(Map<String, SensorTagConfiguration> inputMap) {
        try {
            FileOutputStream fos = getApplicationContext().openFileOutput("configdata", Context.MODE_PRIVATE);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(inputMap);
            os.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private HashMap<String, SensorTagConfiguration> loadMap() {
        HashMap<String, SensorTagConfiguration> outputMap = new HashMap<String, SensorTagConfiguration>();
        try {
            FileInputStream fis = getApplicationContext().openFileInput("configdata");
            ObjectInputStream is = new ObjectInputStream(fis);
            outputMap = (HashMap<String, SensorTagConfiguration>) is.readObject();
            is.close();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return outputMap;
    }

/*
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                // Nothing to do.
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                // Nothing to do.
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Nothing to do.
            } else if (BluetoothLeService.ACTION_DATA_READ.equals(action)) {
                // Nothing to do.
            } else if (BluetoothLeService.ACTION_DATA_NOTIFY.equals(action)) {
                // Nothing to do.
            }
        }
    };
*/
    /*
    private void displayLatestReadings() {
        txtView.setText("Sensor Readings");
        for (Map.Entry<String, String> entry : latestSensorReadingMap.entrySet()) {
            txtView.append("\n" + "Device: " + entry.getKey() + " - Lux: " + entry.getValue());
        }
    }*/

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
