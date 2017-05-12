package jp.ac.titech.itpro.sdl.accelgraph;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends Activity implements SensorEventListener {

    private final static String TAG = "MainActivity";

    private TextView rateView, accuracyView;
    private GraphView xView, yView, zView;

    private SensorManager sensorMgr;


    private Sensor proximitySonsor;
    private Sensor lightSensor;
    private Sensor gravitySensor;

    private final static long GRAPH_REFRESH_WAIT_MS = 20;

    private GraphRefreshThread th = null;
    private Handler handler;

    private float vx, vy, vz;

    private float lightVal, gravityVal, proxiVal;
    private float rate;
    private int accuracy;
    private long prevts;

    private final static float alpha = 0.75F;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        setContentView(R.layout.activity_main);

        rateView = (TextView) findViewById(R.id.rate_view);
        accuracyView = (TextView) findViewById(R.id.accuracy_view);
        xView = (GraphView) findViewById(R.id.x_view);
        yView = (GraphView) findViewById(R.id.y_view);
        zView = (GraphView) findViewById(R.id.z_view);

        sensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);

        lightSensor = sensorMgr.getDefaultSensor(Sensor.TYPE_LIGHT);
        gravitySensor = sensorMgr.getDefaultSensor(Sensor.TYPE_GRAVITY);
        proximitySonsor = sensorMgr.getDefaultSensor(Sensor.TYPE_PROXIMITY);



        if(lightSensor == null)
        {
            Toast.makeText(this, getString(R.string.toast_no_light_error),
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if(gravitySensor == null)
        {
            Toast.makeText(this, getString(R.string.toast_no_gravity_error),
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if(proximitySonsor == null)
        {
            Toast.makeText(this, getString(R.string.toast_no_proximity_error),
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }



        handler = new Handler();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");

        sensorMgr.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_FASTEST);
        sensorMgr.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_FASTEST);
        sensorMgr.registerListener(this, proximitySonsor, SensorManager.SENSOR_DELAY_FASTEST);
        th = new GraphRefreshThread();
        th.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
        th = null;
        sensorMgr.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

    if(event.sensor.getType() == Sensor.TYPE_PROXIMITY)
    {
        proxiVal = event.values[0];
    }
    else if (event.sensor.getType() == Sensor.TYPE_LIGHT)
    {
        lightVal = alpha * lightVal + (1-alpha)*event.values[0];
        lightVal /= 6;
        lightVal *= -1;

    }
    else if (event.sensor.getType() == Sensor.TYPE_GRAVITY)
    {
        gravityVal = alpha * gravityVal + (1-alpha)*event.values[0];
    }
        rate = ((float) (event.timestamp - prevts)) / (1000 * 1000);
        prevts = event.timestamp;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.i(TAG, "onAccuracyChanged: ");
        this.accuracy = accuracy;
    }

    private class GraphRefreshThread extends Thread {
        public void run() {
            try {
                while (th != null) {
                    handler.post(new Runnable() {
                        public void run() {
                            rateView.setText(String.format(Locale.getDefault(), "%f", rate));
                            accuracyView.setText(String.format(Locale.getDefault(), "%d", accuracy));
                            xView.addData(proxiVal, true);
                            yView.addData(lightVal, true);
                            zView.addData(gravityVal, true);
                        }
                    });
                    Thread.sleep(GRAPH_REFRESH_WAIT_MS);
                }
            }
            catch (InterruptedException e) {
                Log.e(TAG, e.toString());
                th = null;
            }
        }
    }
}
