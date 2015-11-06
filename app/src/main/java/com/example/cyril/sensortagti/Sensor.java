package com.example.cyril.sensortagti;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.Handler;
import android.util.Log;

import java.util.UUID;

/**
 * Implements any sensor in the TI SensorTag.
 */
public abstract class Sensor
{

    private final static String TAG=Sensor.class.getSimpleName();

    // Indicator.
    public boolean wasInitialized=false;
    // Service & Characteristics UUIDs.
    private final UUID serviceUuid;
    // Bluetooth instances.
    private BluetoothLeService mBluetoothLeService;
    //Bluetooth address for this particular sensor
    private String mBluetoothLeDeviceAddress;
    // Special for motion sensor.
    public int measure; //1=acc,2=gyr,3=mag
    // Check for a received notification every second.
    private Handler handler;
    private boolean wasNotified;
    public void checkForNotification()
    {
        handler.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                if(!wasNotified)
                {
                    Log.w(TAG,"no notifications");
                    turnOnService();
                    unableNotifications();
                }
                else
                    Log.w(TAG,"notifications");
                wasNotified=false;
                handler.postDelayed(this,1000);
            }
        },1000);
    }
    public void receiveNotification()
    {
        this.wasNotified=true;
    }

    /**
     * Disable sensor.
     */
    public void disable()
    {
        this.handler.removeCallbacksAndMessages(null);
    }

    /**
     * Basic constructor.
     */
    public Sensor(UUID serviceUuid,BluetoothLeService mBluetoothLeService,String address)
    {
        // Initialize the Service & Characteristics UUIDs.
        this.serviceUuid=serviceUuid;
        // Initialize the Bluetooth instances.
        this.mBluetoothLeService=mBluetoothLeService;
        // Initialize the device address
        this.mBluetoothLeDeviceAddress = address;

        // Turns on this sensor's service.
        turnOnService();
        if(!this.wasInitialized)
            return;
        // Unable this sensor's notifications.
        unableNotifications();
        // Set this sensor's period.
        setPeriod();
        // Start the notifications checking.
        this.handler=new Handler();
        this.wasNotified=false;
        checkForNotification();
    }

    /**
     * Turns on this sensor's service.
     */
    public void turnOnService()
    {
        BluetoothGattService service=this.mBluetoothLeService.getService(this.serviceUuid,mBluetoothLeDeviceAddress);
        if(service==null)return;
        UUID configUuid=UUID.fromString(SensorTagGattAttributes.servToConfig(this.serviceUuid.toString(),"Default"));
        BluetoothGattCharacteristic configCharacteristic=service.getCharacteristic(configUuid);
        configCharacteristic.setValue(new byte[]{1});
        // Special case: Movement
        if("f000aa80-0451-4000-b000-000000000000".equals(this.serviceUuid.toString()))
            configCharacteristic.setValue(new byte[] {0x7F,0x02});
        this.mBluetoothLeService.writeCharacteristic(configCharacteristic,mBluetoothLeDeviceAddress);
        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        this.wasInitialized=true;
    }

    /**
     * Unable this sensor's notifications.
     */
    public void unableNotifications()
    {
        BluetoothGattService service=this.mBluetoothLeService.getService(this.serviceUuid,mBluetoothLeDeviceAddress);
        if(service==null)return;
        UUID dataUuid=UUID.fromString(SensorTagGattAttributes.servToData(serviceUuid.toString(), "Default"));
        BluetoothGattCharacteristic dataCharacteristic=service.getCharacteristic(dataUuid);
        this.mBluetoothLeService.setCharacteristicNotification(dataCharacteristic, true,mBluetoothLeDeviceAddress); // Enabled locally.
        this.mBluetoothLeService.writeDescriptor(dataCharacteristic,mBluetoothLeDeviceAddress); // Enabled remotely.
        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Set this sensor's period.
     */
    public void setPeriod()
    {
        BluetoothGattService service=this.mBluetoothLeService.getService(this.serviceUuid,mBluetoothLeDeviceAddress);
        if(service==null)return;
        UUID periUuid=UUID.fromString(SensorTagGattAttributes.servToPeri(this.serviceUuid.toString(), "default"));
        BluetoothGattCharacteristic periodCharacteristic=service.getCharacteristic(periUuid);
        periodCharacteristic.setValue(new byte[] {SensorTagGattAttributes.optimalPeriod(this.serviceUuid.toString())});
        this.mBluetoothLeService.writeCharacteristic(periodCharacteristic,mBluetoothLeDeviceAddress);
        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gyroscope, Magnetometer, Barometer, IR temperature all store 16 bit two's complement values as LSB MSB, which cannot be directly parsed
     * as getIntValue(FORMAT_SINT16, offset) because the bytes are stored as little-endian.
     *
     * This function extracts these 16 bit two's complement values.
     * */
    protected static Integer shortSignedAtOffset(byte[] c, int offset)
    {
        Integer lowerByte = (int) c[offset] & 0xFF;
        Integer upperByte = (int) c[offset+1]; // // Interpret MSB as signed
        return (upperByte << 8) + lowerByte;
    }

    protected static Integer shortUnsignedAtOffset(byte[] c, int offset)
    {
        Integer lowerByte = (int) c[offset] & 0xFF;
        Integer upperByte = (int) c[offset + 1] & 0xFF; // // Interpret MSB as signed
        return (upperByte << 8) + lowerByte;
    }

    protected static Integer twentyFourBitUnsignedAtOffset(byte[] c, int offset)
    {
        Integer lowerByte = (int) c[offset] & 0xFF;
        Integer mediumByte = (int) c[offset+1] & 0xFF;
        Integer upperByte = (int) c[offset + 2] & 0xFF;
        return (upperByte << 16) + (mediumByte << 8) + lowerByte;
    }

    /**
     * Converts the byte array to an actual measured value.
     */
    public abstract Point3D convert(byte[] value);

    @Override
    public String toString()
    {
        throw new UnsupportedOperationException("Error: shouldn't be called.");
    }

}
