package com.example.cyril.sensortagti;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Activity to record samples of drawer actions as training examples.
 */
public class DrawerRecordingActivity extends Activity
{

    private final static String TAG = DrawerRecordingActivity.class.getSimpleName();
    // EXTRA.
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    // Views.
    private TextView mConnectionState;
    private String mDeviceName;
    private String mDeviceAddress;
    private TextView mAction;
    private TextView mState;
    // Bluetooth instances.
    private BluetoothLeService mBluetoothLeService;
    private boolean mConnected = false;
    // Relevant sensors & measurements.
    private Sensor s1;
    private Point3D accelerometer;
    private Sensor s2;
    private Point3D luxometer;


    /**
     * Code to manage Service lifecycle.
     */
    private final ServiceConnection mServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize())
            {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName)
        {
            mBluetoothLeService = null;
        }
    };

    /**
     * Handles various events fired by the Service.
     * ACTION_GATT_CONNECTED: connected to a GATT server.
     * ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
     * ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
     * ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
     *                       or notification operations.
     * ACTION_DATA_WRITE: wrote to the descriptor
     */
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action))
            {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action))
            {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action))
            {
                // Connect to the relevant sensors.
                UUID motionUuid=UUID.fromString(SensorTagGattAttributes.UUID_MOV_SERV);
                s1=new MotionSensor(motionUuid,mBluetoothLeService,mDeviceAddress);
                UUID luxometerUuid=UUID.fromString(SensorTagGattAttributes.UUID_OPT_SERV);
                s2=new LuxometerSensor(luxometerUuid,mBluetoothLeService,mDeviceAddress);
            } else if (BluetoothLeService.ACTION_DATA_NOTIFY.equals(action))
            {
                String uuidStr=intent.getStringExtra(BluetoothLeService.EXTRA_UUID);
                byte[] value = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                onCharacteristicChanged(uuidStr,value);
            }
        }
    };

    /**
     * Gets called when there is a data notification.
     */
    private void onCharacteristicChanged(String uuidStr,byte[] value)
    {
        if(uuidStr.equals(SensorTagGattAttributes.UUID_MOV_DATA))
        {
            this.s1.measure=1;
            this.accelerometer=this.s1.convert(value);
            this.s1.receiveNotification();
        }
        else
        {
            this.luxometer=this.s2.convert(value);
            this.s2.receiveNotification();
            // Predict the state.
            if(this.luxometer.x>0)
                this.mState.setText("Opened "+this.luxometer.x+">0");
            else
                this.mState.setText("Closed "+this.luxometer.x+"<=0");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drawer_recording);
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
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mAction=(TextView)findViewById(R.id.current_action);
        mState=(TextView)findViewById(R.id.current_state);
        // Bind BluetoothLeService.
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        // Initialize the measure.
        this.accelerometer=new Point3D(0.0,0.0,0.0);
        this.luxometer=new Point3D(0.0,0.0,0.0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register for the BroadcastReceiver.
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null)
        {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
        // Start predicting actions.
        this.timer.start();
    }

    /**
     * Predicts actions every second.
     */
    CountDownTimer timer=new CountDownTimer(1250,100)
    {
        // Initialize the relevant recorders.
        ArrayList<Point3D> accRecorder=new ArrayList<>();
        @Override
        public void onTick(long millisUntilFinished)
        {
            // Add measures to respective containers.
            accRecorder.add(accelerometer);
        }
        @Override
        public void onFinish()
        {
            // Initialize the current training example.
            ArrayList<Double> X=new ArrayList<>();

            // Filter the noisy measurements.
            Statistics.filter(accRecorder, 1);
            // Increase the sensitivity of the measurements.
            Statistics.sensitize(accRecorder,3);
            // Scale the measurements.
            Statistics.scale(accRecorder);

            // 1-CALIBRATED CURVE.
            // Add the relevant quantities to the training example.
            addRelevantQuantities(accRecorder, X);

            // 2-FIRST ORDER DERIVATIVE.
            // Get the change in the measurements (derivative).
            ArrayList<Point3D> accChanges=new ArrayList<>();
            Statistics.getChanges(accRecorder, accChanges);
            // Add the relevant quantities to the training example.
            addRelevantQuantities(accChanges, X);

            // 3-FIRST ORDER INTEGRAL.
            // Get the area under the curve in the measurements (integral).
            ArrayList<Point3D> accArea=new ArrayList<>();
            Statistics.getArea(accRecorder, accArea);
            // Add the relevant quantities to the training example.
            addRelevantQuantities(accArea, X);

            // 4-SECOND ORDER DERIVATIVE.
            // Get the change in the measurements (derivative).
            ArrayList<Point3D> accChanges2=new ArrayList<>();
            Statistics.getChanges(accChanges, accChanges2);
            // Add the relevant quantities to the training example.
            addRelevantQuantities(accChanges2, X);

            // 5-SECOND ORDER INTEGRAL.
            // Get the area under the curve in the measurements (integral).
            ArrayList<Point3D> accArea2=new ArrayList<>();
            Statistics.getArea(accArea, accArea2);
            // Add the relevant quantities to the training example.
            addRelevantQuantities(accArea2,X);

            // Predict.
            if(X.get(2)<=-0.0834)
            {
                if(X.get(15)<=2.5)
                {
                    mAction.setText("In use ("+X.get(2)+"<=-0.0834) ("+X.get(15)+"<=2.5)");
                }
                else
                {
                    if(X.get(12)<=2.5)
                    {
                        if(X.get(29)<=0.4591)
                        {
                            mAction.setText("In use ("+X.get(2)+">0.0834) ("+X.get(12)+"<=2.5) ("+X.get(29)+"<=0.4591)");
                        }
                        else
                        {
                            mAction.setText("Vibrating ("+X.get(2)+">0.0834) ("+X.get(12)+"<=2.5) ("+X.get(29)+">0.4591)");
                        }
                    }
                    else
                    {
                        mAction.setText("Vibrating ("+X.get(2)+">0.0834) ("+X.get(12)+">2.5)");
                    }
                }
            }
            else
            {
                mAction.setText("No action "+X.get(2)+">-0.0834");
            }

            // Re-initialize.
            accRecorder.clear();
            X.clear();
            // R e-call.
            start();
        }
    };

    /**
     * Adds the relevant quantities to the training example.
     */
    private void addRelevantQuantities(ArrayList<Point3D> recorder,ArrayList<Double> example)
    {
        // 1st order statistics information.
        addPoint(Statistics.min(recorder),example);
        addPoint(Statistics.max(recorder),example);
        addPoint(Statistics.mean(recorder),example);
        // 2nd order statistics information.
        addPoint(Statistics.variance(recorder),example);
        // Oscillation information.
        example.add((double)Statistics.lowSaddleX(recorder));
        example.add((double)Statistics.highSaddleX(recorder));
        example.add((double)Statistics.lowSaddleY(recorder));
        example.add((double)Statistics.highSaddleY(recorder));
        example.add((double)Statistics.lowSaddleZ(recorder));
        example.add((double)Statistics.highSaddleZ(recorder));
        // Peaks information.
        example.add((double)Statistics.minX(recorder));
        example.add((double)Statistics.maxX(recorder));
        example.add((double)Statistics.minY(recorder));
        example.add((double)Statistics.maxY(recorder));
        example.add((double)Statistics.minZ(recorder));
        example.add((double)Statistics.maxZ(recorder));
    }

    /**
     * Adds a point to the training example.
     */
    private void addPoint(Point3D p,ArrayList<Double> example)
    {
        example.add(p.x);
        example.add(p.y);
        example.add(p.z);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
        this.s1.disable();
        this.s2.disable();
        this.timer.cancel();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        unbindService(this.mServiceConnection);
        this.mBluetoothLeService = null;
        this.s1.disable();
        this.s2.disable();
        this.timer.cancel();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected)
        {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else
        {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_READ);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_NOTIFY);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_WRITE);
        return intentFilter;
    }

}
