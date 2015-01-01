package com.sensiya.flashlightdemo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import com.sensiya.brainssdk.api.BrainsAPI;
import com.sensiya.brainssdk.api.BrainsAPICallback;
import com.sensiya.brainssdk.api.UserData;

import java.io.IOException;
import java.util.List;

public class MainActivity extends Activity implements SurfaceHolder.Callback {

  private static final String TAG = MainActivity.class.getSimpleName();

  private Camera mCamera;
  private boolean mIsLightOn;
  private boolean mIsPreview;
  private ImageButton mFlashlightButton;

  private WakeLock mWakeLock;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    BrainsAPI.start(getApplicationContext(), new BrainsAPICallback() {
      @Override
      public void onConnected() {
        Log.d(TAG, "Sensiya SDK connected.");
      }

      @Override
      public void onError(Error error) {
        Log.d(TAG, String.format("Error occurred (%d) %s", error.getCode(), error.getMessage()));
      }

      @Override
      public void onDisconnected() {
        Log.d(TAG, "Sensiya SDK disconnected.");
      }

      @Override
      public void onUserDataReady(UserData user) {
        Log.d(TAG, "User data is ready.");

        Log.d(TAG, String.format("User gender is %s", UserData.getGenderString(user.getGender())));
        Log.d(TAG, String.format("User age range is %s", user.getAgeRange()));
      }
    });

    mFlashlightButton = (ImageButton) findViewById(R.id.btn_flashlight);
    mFlashlightButton.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        toggleLight();
      }
    });

    SurfaceView surfaceView = (SurfaceView) this.findViewById(R.id.surface_view);
    SurfaceHolder surfaceHolder = surfaceView.getHolder();
    surfaceHolder.addCallback(this);
    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
      surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    startService(new Intent(this, SuddenLightsOffService.class));
  }

  @Override
  public void onAttachedToWindow() {
    super.onAttachedToWindow();
    disablePhoneSleep();
  }

  private void getCamera() {
    if (mCamera == null) {
      try {
        mCamera = Camera.open();
      } catch (RuntimeException e) {
        Log.e(TAG, "FLASH_MODE_TORCH not supported");
      }
    }
  }

  private void toggleLight() {
    if (mIsLightOn) {
      turnLightOff();
    } else {
      turnLightOn();
    }
  }

  private void turnLightOn() {

    if (mCamera == null) {
      Toast.makeText(this, "Camera not found", Toast.LENGTH_LONG).show();
      return;
    }
    mIsLightOn = true;
    Parameters parameters = mCamera.getParameters();

    List<String> flashModes = parameters.getSupportedFlashModes();
    // Check if camera flash exists
    if (flashModes == null) {
      return;
    }

    String flashMode = parameters.getFlashMode();
    if (!Parameters.FLASH_MODE_TORCH.equals(flashMode)) {
      // Turn on the flash
      if (flashModes.contains(Parameters.FLASH_MODE_TORCH)) {
        parameters.setFlashMode(Parameters.FLASH_MODE_TORCH);
        mCamera.setParameters(parameters);
        mFlashlightButton.setImageResource(R.drawable.on);
        startWakeLock();
      } else {
        Log.e(TAG, "FLASH_MODE_TORCH not supported");
      }
    }
  }

  private void turnLightOff() {
    if (mIsLightOn) {

      mFlashlightButton.setImageResource(R.drawable.off);
      mIsLightOn = false;
      if (mCamera == null) {
        return;
      }
      Parameters parameters = mCamera.getParameters();
      List<String> flashModes = parameters.getSupportedFlashModes();
      String flashMode = parameters.getFlashMode();
      // Check if camera flash exists
      if (flashModes == null) {
        return;
      }
      if (!Parameters.FLASH_MODE_OFF.equals(flashMode)) {
        // Turn off the flash
        if (flashModes.contains(Parameters.FLASH_MODE_OFF)) {
          parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
          mCamera.setParameters(parameters);
          stopWakeLock();
        } else {
          Log.e(TAG, "FLASH_MODE_OFF not supported");
        }
      }
    }
  }

  private void startPreview() {
    if (!mIsPreview && mCamera != null) {
      mCamera.startPreview();
      mIsPreview = true;
    }
  }

  private void stopPreview() {
    if (mIsPreview && mCamera != null) {
      mCamera.stopPreview();
      mIsPreview = false;
    }
  }

  private void startWakeLock() {
    if (mWakeLock == null) {
      PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
      mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TORCH_WAKE_LOCK");
    }
    mWakeLock.acquire();
  }

  private void stopWakeLock() {
    if (mWakeLock != null) {
      mWakeLock.release();
    }
  }

  private void disablePhoneSleep() {
    Window window = this.getWindow();
    window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
    window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
    window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
  }

  @Override
  public void onStart() {
    super.onStart();
    getCamera();
    startPreview();
  }

  @Override
  public void onResume() {
    super.onResume();
    turnLightOn();
  }

  @Override
  public void onPause() {
    super.onPause();
    turnLightOff();
  }

  @Override
  public void onStop() {
    super.onStop();
    if (mCamera != null) {
      stopPreview();
      mCamera.release();
      mCamera = null;
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (mCamera != null) {
      turnLightOff();
      stopPreview();
      mCamera.release();
    }
  }

  @Override
  public void surfaceChanged(SurfaceHolder holder, int I, int J, int K) {
  }

  @Override
  public void surfaceCreated(SurfaceHolder holder) {
    try {
      mCamera.setPreviewDisplay(holder);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void surfaceDestroyed(SurfaceHolder holder) {
  }
}
