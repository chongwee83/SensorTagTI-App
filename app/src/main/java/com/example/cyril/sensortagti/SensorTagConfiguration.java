package com.example.cyril.sensortagti;

import java.io.Serializable;

/**
 * Created by chongwee on 10/1/2016.
 */
public class SensorTagConfiguration implements Serializable {

    public SensorTagConfiguration(SensorType sensorType) {
        this.sensorType = sensorType;
    }

    public SensorType getSensorType() {
        return sensorType;
    }

    public void setSensorType(SensorType sensorType) {
        this.sensorType = sensorType;
    }

    public enum SensorType {
        TEMPERATURE, BRIGHTNESS, HUMIDITY, MOTION, PRESSURE
    }

    private SensorType sensorType;

    private static final long serialVersionUID = 164612164061232103L;
}
