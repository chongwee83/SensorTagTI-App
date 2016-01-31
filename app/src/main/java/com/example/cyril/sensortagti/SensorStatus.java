package com.example.cyril.sensortagti;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by chongwee on 10/1/2016.
 */
public class SensorStatus {
    private long readingsCount = 0;
    private String latestReading = "";
    private Date latestReadingTimestamp = new Date();

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

    public Date getLatestReadingTimestamp() {
        return latestReadingTimestamp;
    }

    public String getLatestReadingTimestampString() { return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(latestReadingTimestamp); }

    public void setLatestReadingTimestamp(Date latestReadingTimestamp) {
        this.latestReadingTimestamp = latestReadingTimestamp;
    }
}
