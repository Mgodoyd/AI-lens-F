package com.novenosemestre.ai_lens.RA_Objects2.ml.common.helpers;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManager.DisplayListener;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import com.google.ar.core.Session;

  /**
   * Helper to track the display rotations. In particular, the 180 degree rotations are not notified
   * by the onSurfaceChanged() callback, and thus they require listening to the android display
   * events.
   */
  public final class DisplayRotationHelper implements DisplayListener {
    private boolean viewportChanged;
    private int viewportWidth;
    private int viewportHeight;
    private final Display display;
    private final DisplayManager displayManager;
    private final CameraManager cameraManager;

    /**
   * Constructor for DisplayRotationHelper.
   * Initializes the displayManager, cameraManager and display.
   *
   * @param context The context of the application.
   */
  public DisplayRotationHelper(Context context) {
    displayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
    cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    display = windowManager.getDefaultDisplay();
  }

  /**
   * Registers the DisplayRotationHelper as a listener for display changes.
   */
  public void onResume() {
    displayManager.registerDisplayListener(this, null);
  }

  /**
   * Unregisters the DisplayRotationHelper as a listener for display changes.
   */
  public void onPause() {
    displayManager.unregisterDisplayListener(this);
  }

  /**
   * Called when the surface changes size.
   * Updates the viewport width, height and sets the viewportChanged flag to true.
   *
   * @param width The new width of the surface.
   * @param height The new height of the surface.
   */
  public void onSurfaceChanged(int width, int height) {
    viewportWidth = width;
    viewportHeight = height;
    viewportChanged = true;
  }

  /**
   * Updates the session's display geometry if the viewport has changed.
   *
   * @param session The AR session to update.
   */
  public void updateSessionIfNeeded(Session session) {
    if (viewportChanged) {
      int displayRotation = display.getRotation();
      session.setDisplayGeometry(displayRotation, viewportWidth, viewportHeight);
      viewportChanged = false;
    }
  }

  /**
   * Calculates the aspect ratio of the viewport relative to the camera sensor orientation.
   *
   * @param cameraId The ID of the camera.
   * @return The aspect ratio of the viewport.
   */
  public float getCameraSensorRelativeViewportAspectRatio(String cameraId) {
    float aspectRatio;
    int cameraSensorToDisplayRotation = getCameraSensorToDisplayRotation(cameraId);
    switch (cameraSensorToDisplayRotation) {
      case 90:
      case 270:
        aspectRatio = (float) viewportHeight / (float) viewportWidth;
        break;
      case 0:
      case 180:
        aspectRatio = (float) viewportWidth / (float) viewportHeight;
        break;
      default:
        throw new RuntimeException("Unhandled rotation: " + cameraSensorToDisplayRotation);
    }
    return aspectRatio;
  }

  /**
   * Calculates the rotation of the camera sensor relative to the current display orientation.
   *
   * @param cameraId The ID of the camera.
   * @return The rotation of the camera sensor in degrees.
   */
  public int getCameraSensorToDisplayRotation(String cameraId) {
    CameraCharacteristics characteristics;
    try {
      characteristics = cameraManager.getCameraCharacteristics(cameraId);
    } catch (CameraAccessException e) {
      throw new RuntimeException("Unable to determine display orientation", e);
    }

    // Camera sensor orientation.
    int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

    // Current display orientation.
    int displayOrientation = toDegrees(display.getRotation());

    // Make sure we return 0, 90, 180, or 270 degrees.
    return (sensorOrientation - displayOrientation + 360) % 360;
  }

  /**
   * Converts the rotation of the display from the Surface constants to degrees.
   *
   * @param rotation The rotation of the display.
   * @return The rotation in degrees.
   */
  private int toDegrees(int rotation) {
    switch (rotation) {
      case Surface.ROTATION_0:
        return 0;
      case Surface.ROTATION_90:
        return 90;
      case Surface.ROTATION_180:
        return 180;
      case Surface.ROTATION_270:
        return 270;
      default:
        throw new RuntimeException("Unknown rotation " + rotation);
    }
  }

  /**
   * Called when a new display has been added.
   * Currently does nothing.
   *
   * @param displayId The ID of the new display.
   */
  @Override
  public void onDisplayAdded(int displayId) {}

  /**
   * Called when a display has been removed.
   * Currently does nothing.
   *
   * @param displayId The ID of the removed display.
   */
  @Override
  public void onDisplayRemoved(int displayId) {}

  /**
   * Called when a display has changed.
   * Sets the viewportChanged flag to true.
   *
   * @param displayId The ID of the changed display.
   */
  @Override
  public void onDisplayChanged(int displayId) {
    viewportChanged = true;
  }
}
