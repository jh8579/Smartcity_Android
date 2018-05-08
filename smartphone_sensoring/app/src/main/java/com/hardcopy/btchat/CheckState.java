package com.hardcopy.btchat;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;

/**
 * Created by USER on 2017-12-10.
 */

public class CheckState {
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private SensorManager sSensorManager;
    private Sensor sSensor;

    float gravity[] = new float[3];
    float linear_acceleration[] = new float[3];
    double gravity_sum;

    int gravity_count = 0;
    int sound = 1;

    public CheckState(){
//        mSensorManager = (SensorManager).getSystemService(Context.SENSOR_SERVICE);
//        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
//        mSensorManager.registerListener(this, mSensor,SensorManager.SENSOR_DELAY_NORMAL);
    }
}
