package com.example.cyril.sensortagti;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;


/**
 * Activity to record samples of drawer actions as training examples.
 */
public class ChooserActivity extends Activity
{

    // EXTRA.
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    // Views.
    private String mDeviceName;
    private String mDeviceAddress;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chooser);
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
        ((TextView)findViewById(R.id.device_address)).setText(mDeviceAddress);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        return super.onOptionsItemSelected(item);
    }

    /**
     * Gets called when the user presses the measurements button.
     */
    public void measurements(View v)
    {
        final Intent intent=new Intent(this,DeviceControlActivity.class);
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME,mDeviceName);
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS,mDeviceAddress);
        startActivity(intent);
    }

    /**
     * Gets called when the user presses the drawer training button.
     */
    public void drawerTraining(View v)
    {
        final Intent intent=new Intent(this,DrawerTrainingActivity.class);
        intent.putExtra(DrawerTrainingActivity.EXTRAS_DEVICE_NAME,mDeviceName);
        intent.putExtra(DrawerTrainingActivity.EXTRAS_DEVICE_ADDRESS,mDeviceAddress);
        startActivity(intent);
    }

    /**
     * Gets called when the user presses the drawer recording button.
     */
    public void drawerRecording(View v)
    {
        final Intent intent=new Intent(this,DrawerRecordingActivity.class);
        intent.putExtra(DrawerRecordingActivity.EXTRAS_DEVICE_NAME,mDeviceName);
        intent.putExtra(DrawerRecordingActivity.EXTRAS_DEVICE_ADDRESS,mDeviceAddress);
        startActivity(intent);
    }

    /**
     * Gets called when the user presses the light recording button.
     */
    public void lightRecording(View v)
    {
        final Intent intent=new Intent(this,LightRecordingActivity.class);
        intent.putExtra(LightRecordingActivity.EXTRAS_DEVICE_NAME,mDeviceName);
        intent.putExtra(LightRecordingActivity.EXTRAS_DEVICE_ADDRESS,mDeviceAddress);
        startActivity(intent);
    }

    /**
     * Gets called when the user presses the door training button.
     */
    public void doorTraining(View v)
    {
        final Intent intent=new Intent(this,DoorTrainingActivity.class);
        intent.putExtra(DoorTrainingActivity.EXTRAS_DEVICE_NAME,mDeviceName);
        intent.putExtra(DoorTrainingActivity.EXTRAS_DEVICE_ADDRESS,mDeviceAddress);
        startActivity(intent);
    }

    /**
     * Gets called when the user presses the door recording button.
     */
    public void doorRecording(View v)
    {
        final Intent intent=new Intent(this,DoorRecordingActivity.class);
        intent.putExtra(DoorRecordingActivity.EXTRAS_DEVICE_NAME,mDeviceName);
        intent.putExtra(DoorRecordingActivity.EXTRAS_DEVICE_ADDRESS,mDeviceAddress);
        startActivity(intent);
    }

    /**
     * Gets called when the user presses the bed training button.
     */
    public void bedTraining(View v)
    {
        final Intent intent=new Intent(this,BedTrainingActivity.class);
        intent.putExtra(BedTrainingActivity.EXTRAS_DEVICE_NAME,mDeviceName);
        intent.putExtra(BedTrainingActivity.EXTRAS_DEVICE_ADDRESS,mDeviceAddress);
        startActivity(intent);
    }

    /**
     * Gets called when the user presses the bed recording button.
     */
    public void bedRecording(View v)
    {
        final Intent intent=new Intent(this,BedRecordingActivity.class);
        intent.putExtra(BedRecordingActivity.EXTRAS_DEVICE_NAME,mDeviceName);
        intent.putExtra(BedRecordingActivity.EXTRAS_DEVICE_ADDRESS,mDeviceAddress);
        startActivity(intent);
    }

    /**
     * Gets called when the user presses the couch training button.
     */
    public void couchTraining(View v)
    {
        final Intent intent=new Intent(this,CouchTrainingActivity.class);
        intent.putExtra(CouchTrainingActivity.EXTRAS_DEVICE_NAME,mDeviceName);
        intent.putExtra(CouchTrainingActivity.EXTRAS_DEVICE_ADDRESS,mDeviceAddress);
        startActivity(intent);
    }

    /**
     * Gets called when the user presses the couch recording button.
     */
    public void couchRecording(View v)
    {
        final Intent intent=new Intent(this,CouchRecordingActivity.class);
        intent.putExtra(CouchRecordingActivity.EXTRAS_DEVICE_NAME,mDeviceName);
        intent.putExtra(CouchRecordingActivity.EXTRAS_DEVICE_ADDRESS,mDeviceAddress);
        startActivity(intent);
    }

    /**
     * Gets called when the user presses the fridge training button.
     */
    public void fridgeTraining(View v)
    {
        final Intent intent=new Intent(this,FridgeTrainingActivity.class);
        intent.putExtra(FridgeTrainingActivity.EXTRAS_DEVICE_NAME,mDeviceName);
        intent.putExtra(FridgeTrainingActivity.EXTRAS_DEVICE_ADDRESS,mDeviceAddress);
        startActivity(intent);
    }

    /**
     * Gets called when the user presses the fridge recording button.
     */
    public void fridgeRecording(View v)
    {
        final Intent intent=new Intent(this,FridgeRecordingActivity.class);
        intent.putExtra(FridgeRecordingActivity.EXTRAS_DEVICE_NAME,mDeviceName);
        intent.putExtra(FridgeRecordingActivity.EXTRAS_DEVICE_ADDRESS,mDeviceAddress);
        startActivity(intent);
    }

    /**
     * Gets called when the user presses the stove training button.
     */
    public void stoveTraining(View v)
    {
        final Intent intent=new Intent(this,StoveTrainingActivity.class);
        intent.putExtra(StoveTrainingActivity.EXTRAS_DEVICE_NAME,mDeviceName);
        intent.putExtra(StoveTrainingActivity.EXTRAS_DEVICE_ADDRESS,mDeviceAddress);
        startActivity(intent);
    }

    /**
     * Gets called when the user presses the stove recording button.
     */
    public void stoveRecording(View v)
    {
        final Intent intent=new Intent(this,StoveRecordingActivity.class);
        intent.putExtra(StoveRecordingActivity.EXTRAS_DEVICE_NAME,mDeviceName);
        intent.putExtra(StoveRecordingActivity.EXTRAS_DEVICE_ADDRESS,mDeviceAddress);
        startActivity(intent);
    }

    /**
     * Gets called when the user presses the shower recording button.
     */
    public void showerRecording(View v)
    {
        final Intent intent=new Intent(this,ShowerRecordingActivity.class);
        intent.putExtra(ShowerRecordingActivity.EXTRAS_DEVICE_NAME,mDeviceName);
        intent.putExtra(ShowerRecordingActivity.EXTRAS_DEVICE_ADDRESS,mDeviceAddress);
        startActivity(intent);
    }

}
