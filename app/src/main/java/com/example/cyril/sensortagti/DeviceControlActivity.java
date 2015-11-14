package com.example.cyril.sensortagti;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity
{

    private final static String TAG = DeviceControlActivity.class.getSimpleName();
    // EXTRA.
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    // Views.
    private TextView mConnectionState;
    private ArrayList<TextView> mDataLabels;
    private ArrayList<TextView> mDataValues;
    private ExpandableListView mGattServicesList;
    private int index=0;
    private final int MAX_SENSORS=4;
    private String mDeviceName;
    private String mDeviceAddress;
    // Bluetooth instances.
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<BluetoothGattService> mGattServices=new ArrayList<>();
    private boolean mConnected = false;
    // Relevant quantities.
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    // Helper data structures.
    private HashSet<String> connectedServices=new HashSet<>();
    private HashMap<String,Integer> uuidToIndex=new HashMap<>(); // dataUuid to index
    private ArrayList<Sensor> sensors=new ArrayList<>();

    File csvPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    File csvFile;
    CSVWriter writer;

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
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    //unable to close csv file
                    e.printStackTrace();
                    Log.e(TAG, "Unable to close output csv file.");
                }
            }

        }
    };

    /**
     * Handles various events fired by the Service.
     * ACTION_GATT_CONNECTED: connected to a GATT server.
     * ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
     * ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
     * ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
     *                       or notification operations.
     * ACTION_DATA_WRITE: wrote to the descriptor
     */
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action))
            {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
                //create a new file for every new connection
                csvFile = new File(csvPath, "sensortag_output_" + getCurrentTimeStampForFilename() + ".csv");
                try {
                    if (writer != null) {
                        writer.close();
                    }
                    writer = new CSVWriter(new FileWriter(csvFile,false)); //set true to append, false to overwrite file
                } catch (IOException e) {
                    //unable to create/write to csv file
                    e.printStackTrace();
                    Log.e(TAG, "Unable to create output csv file.");
                }
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action))
            {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
                for(Sensor s:sensors)
                    s.disable();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action))
            {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices(mDeviceAddress));
            } else if (BluetoothLeService.ACTION_DATA_READ.equals(action))
            {
                // Nothing to do.
            } else if (BluetoothLeService.ACTION_DATA_NOTIFY.equals(action))
            {
                String uuidStr = intent.getStringExtra(BluetoothLeService.EXTRA_UUID);
                byte[] value = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                onCharacteristicChanged(uuidStr, value);
            }
        }
    };

    /**
     * Gets called when there is a data notification.
     */
    private void onCharacteristicChanged(String uuidStr,byte[] value)
    {
        int index=this.uuidToIndex.get(uuidStr);
        Sensor s=this.sensors.get(index);
        s.receiveNotification();
        s.convert(value);
        this.mDataValues.get(index).setText(s.toString());
        writeToCSV(s.toString());
    }

    /**
     * Gets called when a service is expanded.
     * It registers mBluetoothLeService for this service.
     */
    private final ExpandableListView.OnGroupClickListener groupClickListener=new ExpandableListView.OnGroupClickListener()
    {
        @Override
        public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
            if(mGattServices!=null && index<MAX_SENSORS)
            {
                final BluetoothGattService service=mGattServices.get(groupPosition);
                UUID serviceUuid=service.getUuid();
                // Check if already added
                if(!connectedServices.contains(serviceUuid.toString())&&SensorTagGattAttributes.validService(serviceUuid.toString()))
                {
                    // Add the current sensor to all helper data structures.
                    connectedServices.add(serviceUuid.toString());
                    Sensor sensor=null; // just to initialize
                    if("f000aa00-0451-4000-b000-000000000000".equals(serviceUuid.toString()))
                        sensor=new IRTSensor(serviceUuid,mBluetoothLeService,mDeviceAddress);
                    else if("f000aa20-0451-4000-b000-000000000000".equals(serviceUuid.toString()))
                        sensor=new HumiditySensor(serviceUuid,mBluetoothLeService,mDeviceAddress);
                    else if("f000aa40-0451-4000-b000-000000000000".equals(serviceUuid.toString()))
                        sensor=new BarometerSensor(serviceUuid,mBluetoothLeService,mDeviceAddress);
                    else if("f000aa70-0451-4000-b000-000000000000".equals(serviceUuid.toString()))
                        sensor=new LuxometerSensor(serviceUuid,mBluetoothLeService,mDeviceAddress);
                    else if("f000aa80-0451-4000-b000-000000000000".equals(serviceUuid.toString()))
                        sensor=new MotionSensor(serviceUuid,mBluetoothLeService,mDeviceAddress);
                    if(!sensor.wasInitialized)finish();
                    sensors.add(sensor);
                    mDataLabels.get(index).setText(SensorTagGattAttributes.lookup(serviceUuid.toString(),"Default"));
                    UUID dataUuid=UUID.fromString(SensorTagGattAttributes.servToData(serviceUuid.toString(), "Default"));
                    uuidToIndex.put(dataUuid.toString(),index);
                    // Update the number of connected sensors.
                    index++;
                    return true;
                }
                return false;
            }
            return false;
        }
    };

    /**
     * Clears the UI.
     */
    private void clearUI() {
        mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
        for(int idx=0;idx<this.index;idx++)
            this.mDataValues.get(idx).setText(R.string.no_data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);
        if(getActionBar()!=null)
        {
            getActionBar().setTitle(mDeviceName);
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
        // Get the intent extras.
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
        mGattServicesList.setOnGroupClickListener(groupClickListener);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        this.mDataLabels=new ArrayList<>();
        this.mDataValues=new ArrayList<>();
        this.mDataLabels.add((TextView)findViewById(R.id.data_label_1));
        this.mDataLabels.add((TextView)findViewById(R.id.data_label_2));
        this.mDataLabels.add((TextView)findViewById(R.id.data_label_3));
        this.mDataLabels.add((TextView)findViewById(R.id.data_label_4));
        this.mDataValues.add((TextView)findViewById(R.id.data_value_1));
        this.mDataValues.add((TextView)findViewById(R.id.data_value_2));
        this.mDataValues.add((TextView)findViewById(R.id.data_value_3));
        this.mDataValues.add((TextView)findViewById(R.id.data_value_4));
        // Bind BluetoothLeService.
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register for the BroadcastReceiver.
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null)
        {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
        for(Sensor s: sensors)
            if(s.wasInitialized)
                s.disable();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
        for(Sensor s:sensors)
            if(s.wasInitialized)
                s.disable();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    /**
     * Demonstrates how to iterate through the supported GATT Services/Characteristics.
     * In this sample, we populate the data structure that is bound to the ExpandableListView
     * on the UI.
     */
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String,String>> gattServiceData = new ArrayList<>();
        ArrayList<ArrayList<HashMap<String,String>>>gattCharacteristicData=new ArrayList<>();
        // Loops through available GATT Services.
        for (BluetoothGattService gattService:gattServices)
        {
            HashMap<String,String> currentServiceData = new HashMap<>();
            String uuid=gattService.getUuid().toString();
            currentServiceData.put(LIST_NAME,SensorTagGattAttributes.lookup(uuid,unknownServiceString));
            currentServiceData.put(LIST_UUID,uuid);
            gattServiceData.add(currentServiceData);
            ArrayList<HashMap<String, String>> gattCharacteristicGroupData=new ArrayList<>();
            List<BluetoothGattCharacteristic> gattCharacteristics=gattService.getCharacteristics();
            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics)
            {
                HashMap<String, String> currentCharaData = new HashMap<>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(LIST_NAME,SensorTagGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID,uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            gattCharacteristicData.add(gattCharacteristicGroupData);
            mGattServices.add(gattService);
        }

        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME,LIST_UUID},
                new int[]{android.R.id.text1, android.R.id.text2},
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME,LIST_UUID},
                new int[]{android.R.id.text1, android.R.id.text2}
        );
        mGattServicesList.setAdapter(gattServiceAdapter);
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

    private void writeToCSV(String s) {
        if (writer != null) {
            s = getCurrentTimeStamp() + "," + s;
            writer.writeNext(s.split(","));
        }
    }

    public String getCurrentTimeStamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
    }

    public String getCurrentTimeStampForFilename() {
        return new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
    }
}
