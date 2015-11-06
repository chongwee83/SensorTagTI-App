package com.example.cyril.sensortagti;

import android.bluetooth.BluetoothDevice;

/**
 * Implements a special Pair class.
 */
public class Pair implements Comparable
{

    private BluetoothDevice key;
    private Integer value;

    /**
     * Basic constructor.
     */
    public Pair(BluetoothDevice key,Integer value)
    {
        this.key=key;
        this.value=value;
    }

    public Integer getValue()
    {
        return value;
    }


    /**
     * compareTo.
     */
    @Override
    public int compareTo(Object o)
    {
        return this.getValue().compareTo(((Pair)o).getValue());
    }

    /**
     * toString.
     */
    @Override
    public String toString()
    {
        return this.key.getName()+" "+this.value.toString();
    }

}
