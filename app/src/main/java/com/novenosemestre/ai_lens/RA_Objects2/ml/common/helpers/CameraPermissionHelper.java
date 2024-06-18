package com.novenosemestre.ai_lens.RA_Objects2.ml.common.helpers;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public final class CameraPermissionHelper {
  // The permission code for the camera.
private static final int CAMERA_PERMISSION_CODE = 0;

  // The permission string for the camera.
  private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;

  /**
   * Checks if the camera permission has been granted for the activity.
   *
   * @param activity The activity for which to check the permission.
   * @return true if the permission has been granted, false otherwise.
   */
  public static boolean hasCameraPermission(Activity activity) {
    return ContextCompat.checkSelfPermission(activity, CAMERA_PERMISSION)
        == PackageManager.PERMISSION_GRANTED;
  }

  /**
   * Requests the camera permission for the activity.
   *
   * @param activity The activity for which to request the permission.
   */
  public static void requestCameraPermission(Activity activity) {
    ActivityCompat.requestPermissions(
        activity, new String[] {CAMERA_PERMISSION}, CAMERA_PERMISSION_CODE);
  }

  /**
   * Checks if the app should show UI with rationale for requesting a permission.
   *
   * @param activity The activity for which to check the permission.
   * @return true if the app can show UI with rationale for requesting a permission, false otherwise.
   */
  public static boolean shouldShowRequestPermissionRationale(Activity activity) {
    return ActivityCompat.shouldShowRequestPermissionRationale(activity, CAMERA_PERMISSION);
  }

  /**
   * Launches the application details settings for the activity's package.
   *
   * @param activity The activity for which to launch the settings.
   */
  public static void launchPermissionSettings(Activity activity) {
    Intent intent = new Intent();
    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
    intent.setData(Uri.fromParts("package", activity.getPackageName(), null));
    activity.startActivity(intent);
  }
}
