package com.example.cyril.sensortagti;

import java.util.HashMap;

/**
 * Implements useful functions for the SensorTag services & their characteristics
 */
public class SensorTagGattAttributes
{

    // Useful maps.
    private static HashMap<String,String> attributes=new HashMap<>();
    private static HashMap<String,String> servtoconfig=new HashMap<>();
    private static HashMap<String,String> servtodata=new HashMap<>();
    private static HashMap<String,String> servtoperi=new HashMap<>();

    /** UUIDs: */
    // Device.
    public static String UUID_DEVINFO_SERV="0000180a-0000-1000-8000-00805f9b34fb";
    public static String UUID_DEVINFO_FWREV="00002A26-0000-1000-8000-00805f9b34fb";
    // IRT.
    public static String UUID_IRT_SERV="f000aa00-0451-4000-b000-000000000000";
    public static String UUID_IRT_DATA="f000aa01-0451-4000-b000-000000000000";
    public static String UUID_IRT_CONF="f000aa02-0451-4000-b000-000000000000"; // 0: disable, 1: enable
    public static String UUID_IRT_PERI="f000aa03-0451-4000-b000-000000000000"; // Period in tens of milliseconds
    // Accelerometer.
    public static String UUID_ACC_SERV="f000aa10-0451-4000-b000-000000000000";
    public static String UUID_ACC_DATA="f000aa11-0451-4000-b000-000000000000";
    public static String UUID_ACC_CONF="f000aa12-0451-4000-b000-000000000000"; // 0: disable, 1: enable
    public static String UUID_ACC_PERI="f000aa13-0451-4000-b000-000000000000"; // Period in tens of milliseconds
    // Humidity.
    public static String UUID_HUM_SERV="f000aa20-0451-4000-b000-000000000000";
    public static String UUID_HUM_DATA="f000aa21-0451-4000-b000-000000000000";
    public static String UUID_HUM_CONF="f000aa22-0451-4000-b000-000000000000"; // 0: disable, 1: enable
    public static String UUID_HUM_PERI="f000aa23-0451-4000-b000-000000000000"; // Period in tens of milliseconds
    // Magnetometer.
    public static String UUID_MAG_SERV="f000aa30-0451-4000-b000-000000000000";
    public static String UUID_MAG_DATA="f000aa31-0451-4000-b000-000000000000";
    public static String UUID_MAG_CONF="f000aa32-0451-4000-b000-000000000000"; // 0: disable, 1: enable
    public static String UUID_MAG_PERI="f000aa33-0451-4000-b000-000000000000"; // Period in tens of milliseconds
    // Barometer.
    public static String UUID_BAR_SERV="f000aa40-0451-4000-b000-000000000000";
    public static String UUID_BAR_DATA="f000aa41-0451-4000-b000-000000000000";
    public static String UUID_BAR_CONF="f000aa42-0451-4000-b000-000000000000"; // 0: disable, 1: enable
    public static String UUID_BAR_CALI="f000aa43-0451-4000-b000-000000000000"; // Calibration characteristic
    public static String UUID_BAR_PERI="f000aa44-0451-4000-b000-000000000000"; // Period in tens of milliseconds
    // Gyroscope.
    public static String UUID_GYR_SERV="f000aa50-0451-4000-b000-000000000000";
    public static String UUID_GYR_DATA="f000aa51-0451-4000-b000-000000000000";
    public static String UUID_GYR_CONF="f000aa52-0451-4000-b000-000000000000"; // 0: disable, bit 0: enable x, bit 1: enable y, bit 2: enable z
    public static String UUID_GYR_PERI="f000aa53-0451-4000-b000-000000000000"; // Period in tens of milliseconds
    // Luxometer.
    public static String UUID_OPT_SERV="f000aa70-0451-4000-b000-000000000000";
    public static String UUID_OPT_DATA="f000aa71-0451-4000-b000-000000000000";
    public static String UUID_OPT_CONF="f000aa72-0451-4000-b000-000000000000"; // 0: disable, 1: enable
    public static String UUID_OPT_PERI="f000aa73-0451-4000-b000-000000000000"; // Period in tens of milliseconds
    // Movement.
    public static String UUID_MOV_SERV="f000aa80-0451-4000-b000-000000000000";
    public static String UUID_MOV_DATA="f000aa81-0451-4000-b000-000000000000";
    public static String UUID_MOV_CONF="f000aa82-0451-4000-b000-000000000000"; // 0: disable, bit 0: enable x, bit 1: enable y, bit 2: enable z
    public static String UUID_MOV_PERI="f000aa83-0451-4000-b000-000000000000"; // Period in tens of milliseconds
    // Keys.
    public static String UUID_KEY_SERV="0000ffe0-0000-1000-8000-00805f9b34fb";
    public static String UUID_KEY_DATA="0000ffe1-0000-1000-8000-00805f9b34fb";

    static
    {
        // Sample Services.
        attributes.put(UUID_DEVINFO_SERV,"Device Information");
        attributes.put(UUID_IRT_SERV,"IRT Temperature Sensor");
        attributes.put(UUID_ACC_SERV,"Accelerometer");
        attributes.put(UUID_HUM_SERV,"Humidity Sensor");
        attributes.put(UUID_MAG_SERV,"Magnetometer");
        attributes.put(UUID_BAR_SERV,"Barometric Pressure Sensor");
        attributes.put(UUID_GYR_SERV,"Gyroscope");
        attributes.put(UUID_OPT_SERV,"Luxometer");
        attributes.put(UUID_MOV_SERV,"Motion Sensor");
        attributes.put(UUID_KEY_SERV,"Keys");
        // Sample Characteristics.
        attributes.put(UUID_DEVINFO_FWREV,"Device Revision");
        attributes.put(UUID_IRT_DATA,"Data");
        attributes.put(UUID_IRT_CONF,"Configuration");
        attributes.put(UUID_IRT_PERI,"Period");
        attributes.put(UUID_ACC_DATA,"Data");
        attributes.put(UUID_ACC_CONF,"Configuration");
        attributes.put(UUID_ACC_PERI,"Period");
        attributes.put(UUID_HUM_DATA,"Data");
        attributes.put(UUID_HUM_CONF,"Configuration");
        attributes.put(UUID_HUM_PERI,"Period");
        attributes.put(UUID_MAG_DATA,"Data");
        attributes.put(UUID_MAG_CONF,"Configuration");
        attributes.put(UUID_MAG_PERI,"Period");
        attributes.put(UUID_BAR_DATA,"Data");
        attributes.put(UUID_BAR_CONF,"Configuration");
        attributes.put(UUID_BAR_CALI,"Calibration");
        attributes.put(UUID_BAR_PERI,"Period");
        attributes.put(UUID_GYR_DATA,"Data");
        attributes.put(UUID_GYR_CONF,"Configuration");
        attributes.put(UUID_GYR_PERI,"Period");
        attributes.put(UUID_OPT_DATA,"Data");
        attributes.put(UUID_OPT_CONF,"Configuration");
        attributes.put(UUID_OPT_PERI,"Period");
        attributes.put(UUID_MOV_DATA,"Data");
        attributes.put(UUID_MOV_PERI,"Period");
        attributes.put(UUID_MOV_CONF,"Configuration");
        attributes.put(UUID_KEY_DATA,"Data");
        // Service UUID to configuration UUID map.
        servtoconfig.put(UUID_IRT_SERV,UUID_IRT_CONF);
        servtoconfig.put(UUID_ACC_SERV,UUID_ACC_CONF);
        servtoconfig.put(UUID_HUM_SERV,UUID_HUM_CONF);
        servtoconfig.put(UUID_MAG_SERV,UUID_MAG_CONF);
        servtoconfig.put(UUID_BAR_SERV,UUID_BAR_CONF);
        servtoconfig.put(UUID_GYR_SERV,UUID_GYR_CONF);
        servtoconfig.put(UUID_OPT_SERV,UUID_OPT_CONF);
        servtoconfig.put(UUID_MOV_SERV,UUID_MOV_CONF);
        // Service UUID to data UUID map.
        servtodata.put(UUID_IRT_SERV,UUID_IRT_DATA);
        servtodata.put(UUID_ACC_SERV,UUID_ACC_DATA);
        servtodata.put(UUID_HUM_SERV,UUID_HUM_DATA);
        servtodata.put(UUID_MAG_SERV,UUID_MAG_DATA);
        servtodata.put(UUID_BAR_SERV,UUID_BAR_DATA);
        servtodata.put(UUID_GYR_SERV,UUID_GYR_DATA);
        servtodata.put(UUID_OPT_SERV,UUID_OPT_DATA);
        servtodata.put(UUID_MOV_SERV,UUID_MOV_DATA);
        servtodata.put(UUID_KEY_SERV,UUID_KEY_DATA);
        // Service UUID to period UUID map.
        servtoperi.put(UUID_IRT_SERV,UUID_IRT_PERI);
        servtoperi.put(UUID_ACC_SERV,UUID_ACC_PERI);
        servtoperi.put(UUID_HUM_SERV,UUID_HUM_PERI);
        servtoperi.put(UUID_MAG_SERV,UUID_MAG_PERI);
        servtoperi.put(UUID_BAR_SERV,UUID_BAR_PERI);
        servtoperi.put(UUID_GYR_SERV,UUID_GYR_PERI);
        servtoperi.put(UUID_OPT_SERV,UUID_OPT_PERI);
        servtoperi.put(UUID_MOV_SERV,UUID_MOV_PERI);
    }

    /**
     * Gives the name of the service corresponding to the input service UUID.
     */
    public static String lookup(String serviceUuid,String defaultName)
    {
        String name=attributes.get(serviceUuid);
        return name==null?defaultName:name;
    }

    /**
     * Gives the configure UUID corresponding to the input service UUID (as String).
     */
    public static String servToConfig(String serviceUuid,String defaultName)
    {
        String name=servtoconfig.get(serviceUuid);
        return name==null?defaultName:name;
    }

    /**
     * Gives the data UUID corresponding to the input service UUID (as String).
     */
    public static String servToData(String serviceUuid,String defaultName)
    {
        String name=servtodata.get(serviceUuid);
        return name==null?defaultName:name;
    }

    /**
     * Gives the period UUID corresponding to the input service UUID (as String).
     */
    public static String servToPeri(String serviceUuid,String defaultName)
    {
        String name=servtoperi.get(serviceUuid);
        return name==null?defaultName:name;
    }

    /**
     * Gives the optimal period for the input service.
     */
    public static byte optimalPeriod(String serviceUuid)
    {
        if(serviceUuid.equals(UUID_IRT_SERV))return 10;
        else if(serviceUuid.equals(UUID_HUM_SERV))return 10;
        else if(serviceUuid.equals(UUID_BAR_SERV))return 10;
        else if(serviceUuid.equals(UUID_OPT_SERV))return 10;
        else if(serviceUuid.equals(UUID_MOV_SERV))return 10;
        else return 0;
    }

    /**
     * Checks whether the input UUID corresponds to a service the user can ask for.
     */
    public static boolean validService(String uuid)
    {
        if(uuid.equals(UUID_IRT_SERV))return true;
        else if(uuid.equals(UUID_HUM_SERV))return true;
        else if(uuid.equals(UUID_BAR_SERV))return true;
        else if(uuid.equals(UUID_OPT_SERV))return true;
        else if(uuid.equals(UUID_MOV_SERV))return true;
        return false;
    }

}
