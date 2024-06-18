package com.google.ar.core.examples.java.ml

import android.opengl.GLSurfaceView
import android.view.View
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.novenosemestre.ai_lens.R
import com.novenosemestre.ai_lens.RA_Objects2.MainActivity2
import com.novenosemestre.ai_lens.RA_Objects2.ml.common.helpers.SnackbarHelper
import com.novenosemestre.ai_lens.RA_Objects2.ml.common.samplerender.SampleRender

class MainActivityView(val activity: MainActivity2, renderer: AppRenderer) : DefaultLifecycleObserver {
  val root = View.inflate(activity, R.layout.activity_main3, null)
  val surfaceView = root.findViewById<GLSurfaceView>(R.id.surfaceview).apply {
    SampleRender(this, renderer, activity.assets)
  }
  val useCloudMlSwitch = root.findViewById<SwitchCompat>(R.id.useCloudMlSwitch)
  val scanButton = root.findViewById<AppCompatButton>(R.id.scanButton)
  val resetButton = root.findViewById<AppCompatButton>(R.id.clearButton)
  val snackbarHelper = SnackbarHelper().apply {
    setParentView(root.findViewById(R.id.coordinatorLayout))
    setMaxLines(6)
  }

  /**
   * Called when the activity is resumed.
   * It resumes the GLSurfaceView.
   *
   * @param owner The LifecycleOwner whose lifecycle is being observed.
   */
  override fun onResume(owner: LifecycleOwner) {
    surfaceView.onResume()
  }

  /**
   * Called when the activity is paused.
   * It pauses the GLSurfaceView.
   *
   * @param owner The LifecycleOwner whose lifecycle is being observed.
   */
  override fun onPause(owner: LifecycleOwner) {
    surfaceView.onPause()
  }

  /**
   * Posts a Runnable to the view's message queue.
   * The Runnable will be run on the user interface thread.
   *
   * @param action The Runnable to be added to the message queue.
   */
  fun post(action: Runnable) = root.post(action)

  /**
   * Sets the scanning state of the application.
   * If the state is active, it disables the scan button and changes its text to "Scanning...".
   * If the state is not active, it enables the scan button and changes its text to "Scan".
   *
   * @param active The scanning state.
   */
  fun setScanningActive(active: Boolean) = when(active) {
    true -> {
      scanButton.isEnabled = false
      scanButton.setText(activity.getString(R.string.scan_busy))
    }
    false -> {
      scanButton.isEnabled = true
      scanButton.setText(activity.getString(R.string.scan_available))
    }
  }
}