package com.example.cyril.sensortagti;

import static java.lang.Math.pow;
import java.util.UUID;

/**
 * Implements the IR Temperature Sensor.
 */
public class IRTSensor extends Sensor
{

    private double ambient;
    private double objectOld;
    private double objectNew;

    public IRTSensor(UUID serviceUuid,BluetoothLeService mBluetoothLeService,String address)
    {
        super(serviceUuid,mBluetoothLeService,address);
        this.ambient=0.0;
        this.objectOld=0.0;
        this.objectNew=0.0;
    }

    @Override
    public Point3D convert(byte[] value)
    {
        this.ambient=extractAmbientTemperature(value);
        this.objectOld=extractTargetTemperature(value, ambient);
        this.objectNew=extractTargetTemperatureTMP007(value);
        return new Point3D(this.ambient,this.objectOld,this.objectNew);
    }

    /**
     * Extracts ambient temperature.
     */
    private double extractAmbientTemperature(byte [] v) {
        int offset = 2;
        return shortUnsignedAtOffset(v, offset) / 128.0;
    }

    /**
     * Extracts object temperature (old sensor).
     * TODO:
     * For some reason this way of extracting the target temperature gives bad values.
     * Better to stick to the new extraction method (next).
     */
    private double extractTargetTemperature(byte [] v, double ambient) {
        Integer twoByteValue = shortSignedAtOffset(v, 0);
        double Vobj2 = twoByteValue.doubleValue();
        Vobj2 *= 0.00000015625;
        double Tdie = ambient + 273.15;
        double S0 = 5.593E-14; // Calibration factor
        double a1 = 1.75E-3;
        double a2 = -1.678E-5;
        double b0 = -2.94E-5;
        double b1 = -5.7E-7;
        double b2 = 4.63E-9;
        double c2 = 13.4;
        double Tref = 298.15;
        double S = S0 * (1 + a1 * (Tdie - Tref) + a2 * pow((Tdie - Tref), 2));
        double Vos = b0 + b1 * (Tdie - Tref) + b2 * pow((Tdie - Tref), 2);
        double fObj = (Vobj2 - Vos) + c2 * pow((Vobj2 - Vos), 2);
        double tObj = pow(pow(Tdie, 4) + (fObj / S), .25);
        return tObj - 273.15;
    }

    /**
     * Extracts object temperature (new sensor).
     */
    private double extractTargetTemperatureTMP007(byte[] v){
        int offset=0;
        return shortUnsignedAtOffset(v,offset)/128.0;
    }

    @Override
    public String toString()
    {
        return "Ambient Temperature="+this.ambient+" C\nObject Temperature="+this.objectNew+" C";
    }

}
