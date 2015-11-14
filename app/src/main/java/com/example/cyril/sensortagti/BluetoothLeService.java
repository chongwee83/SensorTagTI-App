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
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
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
    //private String mBluetoothDeviceAddress;
    //private BluetoothGatt mBluetoothGatt;
    private HashMap<String,BluetoothGatt> mBluetoothGattMap = new HashMap<String,BluetoothGatt>();
    private ArrayList<String> mBluetoothDeviceAddressList = new ArrayList<String>();

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
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback()
    {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
        {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED)
            {
                intentAction = ACTION_GATT_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG,"Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG,"Attempting to start service discovery:"+gatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED)
            {
                intentAction = ACTION_GATT_DISCONNECTED;
                Log.i(TAG,"Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status)
        {
            if (status==BluetoothGatt.GATT_SUCCESS)
            {
                boolean isGood=true;
                for(int i=0;i<gatt.getServices().size();i++)
                {
                    BluetoothGattService bgs=gatt.getServices().get(i);
                    Log.w(TAG,"found service "+bgs.getUuid().toString());
                    Log.w(TAG,bgs.getCharacteristics().toString());
                    if(bgs.getCharacteristics().size()==0)
                        isGood=false;
                }
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else
            {
                Log.w(TAG,"onServicesDiscovered received: "+status);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic,
                                          int status)
        {
            Log.w(TAG,"onCharacteristicWrite received: " + status);
            broadcastUpdate(ACTION_DATA_WRITE,characteristic,gatt.getDevice().getAddress());
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status)
        {
            if (status == BluetoothGatt.GATT_SUCCESS)
            {
                Log.i(TAG,"onCharacteristicRead received: " + status);
                broadcastUpdate(ACTION_DATA_READ,characteristic,gatt.getDevice().getAddress());
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            Log.i(TAG,"onCharacteristicChanged received: ");
            broadcastUpdate(ACTION_DATA_NOTIFY, characteristic, gatt.getDevice().getAddress());
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt,BluetoothGattDescriptor descriptor, int status)
        {
            Log.i(TAG, "onDescriptorRead received: " + descriptor.getUuid().toString());
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,BluetoothGattDescriptor descriptor, int status)
        {
            Log.i(TAG, "onDescriptorWrite received: " + descriptor.getUuid().toString());
        }

    };

    /**
     * Broadcast update.
     */
    private void broadcastUpdate(final String action)
    {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    /**
     * Broadcast update.
     */
    private void broadcastUpdate(final String action,final BluetoothGattCharacteristic characteristic, String deviceAddress)
    {
        final Intent intent = new Intent(action);
        final byte[] data = characteristic.getValue();
        if (data != null && data.length > 0)
        {
            intent.putExtra(EXTRA_DATA,characteristic.getValue());
            intent.putExtra(EXTRA_UUID,characteristic.getUuid().toString());
            intent.putExtra(EXTRA_DEVICEADDRESS,deviceAddress);
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
    public boolean initialize()
    {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null)
        {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null)
        {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        // Previously connected device.  Try to reconnect.
        //if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null)
        if (mBluetoothDeviceAddressList.contains(address) && mBluetoothGattMap.containsKey(address))
        {
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
    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic, String address)
    {
        return mBluetoothGattMap.get(address).writeCharacteristic(characteristic);
        //return true;
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,boolean enabled,String address)
    {
        if (mBluetoothAdapter == null || !mBluetoothGattMap.containsKey(address))
        {
            Log.w(TAG, "BluetoothAdapter not initialized or GATT does not exist");
            return;
        }
        mBluetoothGattMap.get(address).setCharacteristicNotification(characteristic, enabled); // Enabled locally.
    }

    /**
     * Writes the Descriptor for the input characteristic.
     */
    public void writeDescriptor(BluetoothGattCharacteristic characteristic,String address)
    {
        if (mBluetoothAdapter == null || !mBluetoothGattMap.containsKey(address))
        {
            Log.w(TAG, "BluetoothAdapter not initialized or GATT does not exist");
            return;
        }
        BluetoothGattDescriptor clientConfig=characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
        clientConfig.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGattMap.get(address).writeDescriptor(clientConfig);
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices(String address)
    {
        if (mBluetoothAdapter == null || !mBluetoothGattMap.containsKey(address))
        {
            Log.w(TAG, "BluetoothAdapter not initialized or GATT does not exist");
            return null;
        }

        return mBluetoothGattMap.get(address).getServices();
    }

    /**
     * Retrieves the service corresponding to the input UUID.
     */
    public BluetoothGattService getService(UUID servUuid,String address)
    {
        if (mBluetoothAdapter == null || !mBluetoothGattMap.containsKey(address))
        {
            Log.w(TAG, "BluetoothAdapter not initialized or GATT does not exist");
            return null;
        }
        return mBluetoothGattMap.get(address).getService(servUuid);
    }

}
