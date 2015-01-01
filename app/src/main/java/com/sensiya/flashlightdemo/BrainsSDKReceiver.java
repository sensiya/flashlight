package com.sensiya.flashlightdemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.sensiya.brainssdk.api.BrainsAPI;
import com.sensiya.brainssdk.api.BrainsIntent;

/**
 * Receiver that listens to Intents coming from SensiyaSDK.
 */
public class BrainsSDKReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {

    // Show the flashlight only if the user is not driving
    if(BrainsIntent.ACTION_USER_ACTIVITY_CHANGED.equalsIgnoreCase(intent.getAction())){

      int activityType = intent.getIntExtra(BrainsIntent.EXTRA_ACTIVITY_TYPE, BrainsIntent.ACTIVITY_UNKNOWN);
      if (activityType == BrainsIntent.ACTIVITY_DRIVING) {
        SuddenLightsOffService.Enabled = false;
      } else {
        SuddenLightsOffService.Enabled = true;
      }
    }
  }
}
