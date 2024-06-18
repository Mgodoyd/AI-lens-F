package com.google.ar.core.examples.java.ml

import android.app.Activity
import android.widget.Toast
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Session
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.novenosemestre.ai_lens.RA_Objects2.ml.common.helpers.CameraPermissionHelper

class ARCoreSessionLifecycleHelper(
  val activity: Activity,
  val features: Set<Session.Feature> = setOf()
) : DefaultLifecycleObserver {
  var installRequested = false
  var sessionCache: Session? = null
    private set

  var exceptionCallback: ((Exception) -> Unit)? = null

  var beforeSessionResume: ((Session) -> Unit)? = null

  /**
   * Tries to create a new ARCore session.
   *
   * This function requests the installation of ARCore if it's not installed yet.
   * If the installation is requested, it sets the installRequested flag to true and returns null.
   * If ARCore is already installed, it tries to create a new session with the specified features.
   * If the session creation fails, it invokes the exception callback and returns null.
   *
   * @return The created session, or null if the session couldn't be created.
   */
  fun tryCreateSession(): Session? {
    when (ArCoreApk.getInstance().requestInstall(activity, !installRequested)!!) {
      ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
        installRequested = true
        return null
      }
      ArCoreApk.InstallStatus.INSTALLED -> {
      }
    }

    return try {
      Session(activity, features)
    } catch (e: Exception) {
      exceptionCallback?.invoke(e)
      null
    }
  }

  /**
   * Called when the activity is resumed.
   *
   * This function checks if the camera permission is granted.
   * If the permission is not granted, it requests the permission and returns.
   * If the permission is granted, it tries to create a new session and resumes it.
   * It also sets the session cache to the created session.
   *
   * @param owner The LifecycleOwner whose lifecycle is being observed.
   */
  override fun onResume(owner: LifecycleOwner) {
    if (!CameraPermissionHelper.hasCameraPermission(activity)) {
      CameraPermissionHelper.requestCameraPermission(activity)
      return
    }

    val session = tryCreateSession() ?: return
    try {
      beforeSessionResume?.invoke(session)
      session.resume()
      sessionCache = session
    } catch (e: CameraNotAvailableException) {
      exceptionCallback?.invoke(e)
    }
  }

  /**
   * Called when the activity is paused.
   * It pauses the session in the session cache.
   *
   * @param owner The LifecycleOwner whose lifecycle is being observed.
   */
  override fun onPause(owner: LifecycleOwner) {
    sessionCache?.pause()
  }

  /**
   * Called when the activity is destroyed.
   * It closes the session in the session cache and sets the session cache to null.
   *
   * @param owner The LifecycleOwner whose lifecycle is being observed.
   */
  override fun onDestroy(owner: LifecycleOwner) {
    sessionCache?.close()
    sessionCache = null
  }

  /**
   * Handles the result of the permission request.
   *
   * This function checks if the camera permission is granted.
   * If the permission is not granted, it shows a toast message and finishes the activity.
   * If the user has chosen not to be asked again for the permission, it launches the permission settings.
   *
   * @param requestCode The request code passed in requestPermissions().
   * @param permissions The requested permissions.
   * @param grantResults The grant results for the corresponding permissions.
   */
  fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
  ) {
    if (!CameraPermissionHelper.hasCameraPermission(activity)) {
      Toast.makeText(activity, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
        .show()
      if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(activity)) {
        CameraPermissionHelper.launchPermissionSettings(activity)
      }
      activity.finish()
    }
  }
}
