package com.sensiya.flashlightdemo;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import com.sensiya.brainssdk.api.BrainsAPI;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by michael on 1/1/2015.
 */
public class SuddenLightsOffService extends Service implements SensorEventListener {

  private ScheduledExecutorService mExecutionService = Executors.newScheduledThreadPool(2);

  private static final float PROXIMITY_NEAR = 0f;
  private static final float PROXIMITY_NEAR_MOTO_X = 3f;

  private static final float LIGHT_DARK_THRESHOLD = 5f;
  private static final float LIGHT_LIGHT_THRESHOLD = 15f;
  private static final int SECONDS_TO_CHECK_PROXIMITY = 4;

  private float mCurrentLight = 0;
  private volatile boolean mIsBlocking = false;
  private volatile boolean mCheckingProximity = false;

  public static boolean Enabled = true;

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public void onCreate() {
    super.onCreate();

    registerSensor(Sensor.TYPE_LIGHT);
  }

  @Override
  public void onDestroy() {
    unregisterSensor(Sensor.TYPE_LIGHT);
    super.onDestroy();
  }

  private void registerSensor(int type) {
    SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    Sensor sensor = sensorManager.getDefaultSensor(type);
    if(sensor != null) {
      sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
    }
  }

  private void unregisterSensor(int type){
    SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    Sensor sensor = sensorManager.getDefaultSensor(type);
    if(sensor != null) {
      sensorManager.unregisterListener(this, sensor);
    }
  }

  @Override
  public void onSensorChanged(SensorEvent event) {

    if (event.sensor.getType() == Sensor.TYPE_LIGHT){
      handleLightSensorData(event);
    } else if (event.sensor.getType() == Sensor.TYPE_PROXIMITY){
      handleProximitySensorData(event);
    }
  }

  private void handleProximitySensorData(SensorEvent event) {
    float value = event.values[0];

    if ((value == PROXIMITY_NEAR || ("ghost".equals(Build.DEVICE) && value == PROXIMITY_NEAR_MOTO_X))){
      //blocking
      if (!mIsBlocking){
        //if previous state is not blocking, notify about state change
        mIsBlocking = true;
      }
    } else {
      //not blocking
      if (mIsBlocking){
        //if previous state is not blocking, notify about state change
        mIsBlocking = false;
      }
    }
  }

  private void handleLightSensorData(SensorEvent event) {
    float prevLight = mCurrentLight;
    mCurrentLight = event.values[0];

    if (mCurrentLight < LIGHT_DARK_THRESHOLD && prevLight > LIGHT_LIGHT_THRESHOLD && !mCheckingProximity) {
      try {
        mCheckingProximity = true;

        registerSensor(Sensor.TYPE_PROXIMITY);

        mExecutionService.schedule(new Runnable() {

          @Override
          public void run() {
            if (!mIsBlocking) {
              if (mCurrentLight < LIGHT_DARK_THRESHOLD) {
                if (Enabled) {
                  Intent flashlightIntent = new Intent(SuddenLightsOffService.this, MainActivity.class);
                  flashlightIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                  SuddenLightsOffService.this.startActivity(flashlightIntent);
                }
              }
            }

            mCheckingProximity = false;
            mIsBlocking = false;
            unregisterSensor(Sensor.TYPE_PROXIMITY);
          }
        }, SECONDS_TO_CHECK_PROXIMITY, TimeUnit.SECONDS);

      }catch (Exception e){
        Log.e("TAG", "Check failed");
      }
    }
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {

  }
}
