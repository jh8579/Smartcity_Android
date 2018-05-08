package com.hardcopy.btchat;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.hardcopy.btchat.fragments.IFragmentListener;

import java.util.TimerTask;

import static java.lang.Math.sqrt;

public class CheckStateActivity extends Activity implements SensorEventListener {
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private SensorManager sSensorManager;
    private Sensor sSensor;

    float gravity[] = new float[3];
    float linear_acceleration[] = new float[3];
    double gravity_sum;

    int gravity_count = 0;
    int sound = 1;

    private Context mContext = null;
    private IFragmentListener mFragmentListener = null;
    private Handler mActivityHandler = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_state);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mSensorManager.registerListener(this, mSensor,SensorManager.SENSOR_DELAY_NORMAL);

    }

    public void onSensorChanged(SensorEvent event){
        // In this example, alpha is calculated as t / (t + dT),
        // where t is the low-pass filter's time-constant and
        // dT is the event delivery rate.
        if(event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION){
            final float alpha = 0.8f;

            // Isolate the force of gravity with the low-pass filter.
            gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
            gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
            gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

            // Remove the gravity contribution with the high-pass filter.
            linear_acceleration[0] = event.values[0] - gravity[0];
            linear_acceleration[1] = event.values[1] - gravity[1];
            linear_acceleration[2] = event.values[2] - gravity[2];

            gravity_sum = sqrt(linear_acceleration[0]*linear_acceleration[0]+linear_acceleration[1]*linear_acceleration[1]+linear_acceleration[2]*linear_acceleration[2]);

            SoundMeter s = new SoundMeter();
            s.start();
            while(sound > 0) {
                double db = Math.log10(s.getAmplitude()) * 10;
                db += 25;
                Log.d("소리값", String.valueOf(db));
                sound--;
            }
            s.stop();


            if(gravity_sum > 0.08){
                gravity_count++;
                if(gravity_count == 200){
                    gravity_count = 0;
                }
            }


//            Log.d("sum값",String.valueOf(gravity_sum));
//            Log.d("x값",String.valueOf(linear_acceleration[0]));
//            Log.d("y값",String.valueOf(linear_acceleration[1]));
//            Log.d("z값",String.valueOf(linear_acceleration[2]));
            Log.d("sum값",String.valueOf(gravity_count));
            sound = 1;
            sendMessage(String.valueOf(gravity_count));
        }

    }
    public void onAccuracyChanged(Sensor se, int event)
    {


    }
    private void sendMessage(String message) {
        if(message == null || message.length() < 1)
            return;
        // send to remote
        if(mFragmentListener != null)
            mFragmentListener.OnFragmentCallback(IFragmentListener.CALLBACK_SEND_MESSAGE, 0, 0, message, null,null);
        else
            return;
        // show on text view

    }
}
