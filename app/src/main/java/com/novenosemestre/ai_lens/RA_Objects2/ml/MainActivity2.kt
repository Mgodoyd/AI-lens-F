package com.novenosemestre.ai_lens.RA_Objects2

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.CameraConfig
import com.google.ar.core.CameraConfigFilter
import com.google.ar.core.Config
import com.google.ar.core.examples.java.ml.ARCoreSessionLifecycleHelper
import com.google.ar.core.examples.java.ml.AppRenderer
import com.google.ar.core.examples.java.ml.MainActivityView
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import com.novenosemestre.ai_lens.R
import com.novenosemestre.ai_lens.RA_Objects2.ml.common.helpers.FullScreenHelper


class MainActivity2 : AppCompatActivity() {
    val TAG = "MainActivity2"
    lateinit var arCoreSessionHelper: ARCoreSessionLifecycleHelper

    lateinit var renderer: AppRenderer
    lateinit var view: MainActivityView

   /**
    * Called when the activity is being created.
    * This function sets the content view, initializes the ARCore session helper and sets its exception callback,
    * configures the AR session, initializes the renderer and the view, and adds them as lifecycle observers.
    *
    * @param savedInstanceState The saved instance state bundle.
    */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main3)

        // Initialize the ARCore session helper and set its exception callback
        arCoreSessionHelper = ARCoreSessionLifecycleHelper(this)
        arCoreSessionHelper.exceptionCallback = { exception ->
            // Determine the error message based on the exception type
            val message = when (exception) {
                is UnavailableArcoreNotInstalledException,
                is UnavailableUserDeclinedInstallationException -> "Please install ARCore"
                is UnavailableApkTooOldException -> "Please update ARCore"
                is UnavailableSdkTooOldException -> "Please update this app"
                is UnavailableDeviceNotCompatibleException -> "This device does not support AR"
                is CameraNotAvailableException -> "Camera not available. Try restarting the app."
                else -> "Failed to create AR session: $exception"
            }
            Log.e(TAG, message, exception)
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }

        // Configure the AR session before resuming
        arCoreSessionHelper.beforeSessionResume = { session ->
            session.configure(
                session.config.apply {
                    focusMode = Config.FocusMode.AUTO
                    if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                        depthMode = Config.DepthMode.AUTOMATIC
                    }
                }
            )

            // Set the camera configuration
            val filter = CameraConfigFilter(session)
                .setFacingDirection(CameraConfig.FacingDirection.BACK)
            val configs = session.getSupportedCameraConfigs(filter)
            val sort = compareByDescending<CameraConfig> { it.imageSize.width }
                .thenByDescending { it.imageSize.height }
            session.cameraConfig = configs.sortedWith(sort)[0]
        }
        lifecycle.addObserver(arCoreSessionHelper)

        // Initialize the renderer and the view, and add them as lifecycle observers
        renderer = AppRenderer(this)
        lifecycle.addObserver(renderer)
        view = MainActivityView(this, renderer)
        setContentView(view.root)
        renderer.bindView(view)
        lifecycle.addObserver(view)
    }

    /**
     * Handles the result of the permission request.
     * If the camera permission is not granted, it shows a toast message and finishes the activity.
     * If the user has chosen not to be asked again for the permission, it launches the permission settings.
     *
     * @param requestCode The request code passed in requestPermissions().
     * @param permissions The requested permissions.
     * @param grantResults The grant results for the corresponding permissions.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        arCoreSessionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    /**
     * Called when the window focus changes.
     * It sets the activity to full screen if it has focus.
     *
     * @param hasFocus Whether the window has focus.
     */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus)
    }
}