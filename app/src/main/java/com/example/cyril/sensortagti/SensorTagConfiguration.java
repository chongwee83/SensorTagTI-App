package com.example.cyril.sensortagti;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by chongwee on 10/1/2016.
 */
public class SensorTagConfiguration implements Serializable {

    public SensorTagConfiguration() {
        sensorTypes = new ArrayList<SensorType>();
    }

    public void addSensorType(SensorType sensorType) {
        sensorTypes.add(sensorType);
    }

    public ArrayList<SensorType> getSensorTypes() {
        return sensorTypes;
    }

    public enum SensorType {
        TEMPERATURE, BRIGHTNESS, HUMIDITY, MOTION, PRESSURE
    }

    private ArrayList<SensorType> sensorTypes;

    private static final long serialVersionUID = 1646121640643232103L;
}
