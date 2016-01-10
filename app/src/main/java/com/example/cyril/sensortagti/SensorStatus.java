package com.example.cyril.sensortagti;

/**
 * Created by chongwee on 10/1/2016.
 */
public class SensorStatus {
    private long readingsCount = 0;
    private String latestReading = "";
    private String latestReadingTimestamp = "";

    public long getReadingsCount() {
        return readingsCount;
    }

    public void setReadingsCount(long readingsCount) {
        this.readingsCount = readingsCount;
    }

    public void incrementReadingsCount() {
        readingsCount++;
    }


    public String getLatestReading() {
        return latestReading;
    }

    public void setLatestReading(String latestReading) {
        this.latestReading = latestReading;
    }

    public String getLatestReadingTimestamp() {
        return latestReadingTimestamp;
    }

    public void setLatestReadingTimestamp(String latestReadingTimestamp) {
        this.latestReadingTimestamp = latestReadingTimestamp;
    }
}
