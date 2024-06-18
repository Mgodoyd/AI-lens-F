package com.google.ar.core.examples.java.ml

import android.opengl.Matrix
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.ar.core.Anchor
import com.google.ar.core.Coordinates2d
import com.google.ar.core.Frame
import com.google.ar.core.TrackingState
import com.google.ar.core.examples.java.ml.classification.DetectedObjectResult
import com.google.ar.core.examples.java.ml.classification.GoogleCloudVisionDetector
import com.google.ar.core.examples.java.ml.classification.MLKitObjectDetector
import com.google.ar.core.examples.java.ml.classification.ObjectDetector
import com.google.ar.core.examples.java.ml.render.LabelRender
import com.google.ar.core.examples.java.ml.render.PointCloudRender
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.NotYetAvailableException
import com.novenosemestre.ai_lens.RA_Objects2.MainActivity2
import com.novenosemestre.ai_lens.RA_Objects2.ml.common.helpers.DisplayRotationHelper
import com.novenosemestre.ai_lens.RA_Objects2.ml.common.samplerender.SampleRender
import com.novenosemestre.ai_lens.RA_Objects2.ml.common.samplerender.arcore.BackgroundRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.util.Collections

class AppRenderer(val activity: MainActivity2) : DefaultLifecycleObserver, SampleRender.Renderer, CoroutineScope by MainScope() {
  companion object {
    val TAG = "HelloArRenderer"
  }

  lateinit var view: MainActivityView

  val displayRotationHelper = DisplayRotationHelper(activity)
  lateinit var backgroundRenderer: BackgroundRenderer
  val pointCloudRender = PointCloudRender()
  val labelRenderer = LabelRender()

  val viewMatrix = FloatArray(16)
  val projectionMatrix = FloatArray(16)
  val viewProjectionMatrix = FloatArray(16)

  val arLabeledAnchors = Collections.synchronizedList(mutableListOf<ARLabeledAnchor>())
  var scanButtonWasPressed = false

  val mlKitAnalyzer = MLKitObjectDetector(activity)
  val gcpAnalyzer = GoogleCloudVisionDetector(activity)

  var currentAnalyzer: ObjectDetector = gcpAnalyzer

 /**
   * Called when the activity is resumed.
   * It resumes the display rotation helper.
   *
   * @param owner The LifecycleOwner whose lifecycle is being observed.
   */
  override fun onResume(owner: LifecycleOwner) {
    displayRotationHelper.onResume()
  }

  /**
   * Called when the activity is paused.
   * It pauses the display rotation helper.
   *
   * @param owner The LifecycleOwner whose lifecycle is being observed.
   */
  override fun onPause(owner: LifecycleOwner) {
    displayRotationHelper.onPause()
  }

  /**
   * Binds the view to the renderer.
   * It sets up the click listeners for the scan button, the Cloud ML switch, and the reset button.
   *
   * @param view The MainActivityView to bind.
   */
  fun bindView(view: MainActivityView) {
    this.view = view

    // Set up the click listener for the scan button
    view.scanButton.setOnClickListener {
      scanButtonWasPressed = true
      view.setScanningActive(true)
      hideSnackbar()
    }

    // Set up the checked change listener for the Cloud ML switch
    view.useCloudMlSwitch.setOnCheckedChangeListener { _, isChecked ->
      currentAnalyzer = if (isChecked) gcpAnalyzer else mlKitAnalyzer
    }

    // Check if Google Cloud Vision is configured
    val gcpConfigured = gcpAnalyzer.credentials != null
    view.useCloudMlSwitch.isChecked = gcpConfigured
    view.useCloudMlSwitch.isEnabled = gcpConfigured
    currentAnalyzer = if (gcpConfigured) gcpAnalyzer else mlKitAnalyzer

    // Show a snackbar if Google Cloud Vision isn't configured
    if (!gcpConfigured) {
      showSnackbar("Google Cloud Vision isn't configured (see README). The Cloud ML switch will be disabled.")
    }

    // Set up the click listener for the reset button
    view.resetButton.setOnClickListener {
      arLabeledAnchors.clear()
      view.resetButton.isEnabled = false
      hideSnackbar()
    }
  }

  /**
   * Called when the surface is created.
   * It initializes the background renderer, the point cloud renderer, and the label renderer.
   *
   * @param render The SampleRender object for rendering.
   */
  override fun onSurfaceCreated(render: SampleRender) {
    backgroundRenderer = BackgroundRenderer(render).apply {
      setUseDepthVisualization(render, false)
    }
    pointCloudRender.onSurfaceCreated(render)
    labelRenderer.onSurfaceCreated(render)
  }

  /**
   * Called when the surface is changed.
   * It updates the display rotation helper.
   *
   * @param render The SampleRender object for rendering.
   * @param width The new width of the surface.
   * @param height The new height of the surface.
   */
  override fun onSurfaceChanged(render: SampleRender?, width: Int, height: Int) {
    displayRotationHelper.onSurfaceChanged(width, height)
  }

  // The results of the object detection
  var objectResults: List<DetectedObjectResult>? = null


 /**
  * Called for each frame to be rendered.
  * It updates the session, draws the background, and performs object detection if the scan button was pressed.
  * It also draws labels at the anchor positions of the detected objects.
  *
  * @param render The SampleRender object for rendering.
  */
  override fun onDrawFrame(render: SampleRender) {
    // Get the AR session or return if it's null
    val session = activity.arCoreSessionHelper.sessionCache ?: return

    // Set the texture names for the camera
    session.setCameraTextureNames(intArrayOf(backgroundRenderer.cameraColorTexture.textureId))

    // Update the session if needed based on the display rotation
    displayRotationHelper.updateSessionIfNeeded(session)

    // Update the AR session and handle any exceptions
    val frame = try {
      session.update()
    } catch (e: CameraNotAvailableException) {
      Log.e(TAG, "Camera not available during onDrawFrame", e)
      showSnackbar("Camera not available. Try restarting the app.")
      return
    }

    // Update the display geometry and draw the background
    backgroundRenderer.updateDisplayGeometry(frame)
    backgroundRenderer.drawBackground(render)

    // Get the camera and calculate the view and projection matrices
    val camera = frame.camera
    camera.getViewMatrix(viewMatrix, 0)
    camera.getProjectionMatrix(projectionMatrix, 0, 0.01f, 100.0f)
    Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

    // If the camera is not tracking, return
    if (camera.trackingState != TrackingState.TRACKING) {
      return
    }

    // Draw the point cloud
    frame.acquirePointCloud().use { pointCloud ->
      pointCloudRender.drawPointCloud(render, pointCloud, viewProjectionMatrix)
    }

    // If the scan button was pressed, perform object detection
    if (scanButtonWasPressed) {
      scanButtonWasPressed = false
      val cameraImage = frame.tryAcquireCameraImage()
      if (cameraImage != null) {
        launch(Dispatchers.IO) {
          val cameraId = session.cameraConfig.cameraId
          val imageRotation = displayRotationHelper.getCameraSensorToDisplayRotation(cameraId)
          objectResults = currentAnalyzer.analyze(cameraImage, imageRotation)
          cameraImage.close()
        }
      }
    }

    // If there are object detection results, process them
    val objects = objectResults
    if (objects != null) {
      objectResults = null
      Log.i(TAG, "$currentAnalyzer got objects: $objects")
      val anchors = objects.mapNotNull { obj ->
        val (atX, atY) = obj.centerCoordinate
        val anchor = createAnchor(atX.toFloat(), atY.toFloat(), frame) ?: return@mapNotNull null
        Log.i(TAG, "Created anchor ${anchor.pose} from hit test")
        ARLabeledAnchor(anchor, obj.label)
      }
      arLabeledAnchors.addAll(anchors)
      view.post {
        view.resetButton.isEnabled = arLabeledAnchors.isNotEmpty()
        view.setScanningActive(false)
        when {
          objects.isEmpty() && currentAnalyzer == mlKitAnalyzer && !mlKitAnalyzer.hasCustomModel() ->
            showSnackbar("Default ML Kit classification model returned no results. " +
              "For better classification performance, see the README to configure a custom model.")
          objects.isEmpty() ->
            showSnackbar("Classification model returned no results.")
          anchors.size != objects.size ->
            showSnackbar("Objects were classified, but could not be attached to an anchor. " +
              "Try moving your device around to obtain a better understanding of the environment.")
        }
      }
    }

    // Draw labels at their anchor position.
    for (arDetectedObject in arLabeledAnchors) {
      val anchor = arDetectedObject.anchor
      if (anchor.trackingState != TrackingState.TRACKING) continue
      labelRenderer.draw(
        render,
        viewProjectionMatrix,
        anchor.pose,
        camera.pose,
        arDetectedObject.label
      )
    }
  }

  /**
   * Utility method for [Frame.acquireCameraImage] that maps [NotYetAvailableException] to `null`.
   */
  fun Frame.tryAcquireCameraImage() = try {
    acquireCameraImage()
  } catch (e: NotYetAvailableException) {
    null
  } catch (e: Throwable) {
    throw e
  }

  private fun showSnackbar(message: String): Unit =
    activity.view.snackbarHelper.showError(activity, message)

  private fun hideSnackbar() = activity.view.snackbarHelper.hide(activity)

  /**
   * Temporary arrays to prevent allocations in [createAnchor].
   */
  private val convertFloats = FloatArray(4)
  private val convertFloatsOut = FloatArray(4)

  /** Create an anchor using (x, y) coordinates in the [Coordinates2d.IMAGE_PIXELS] coordinate space. */
  fun createAnchor(xImage: Float, yImage: Float, frame: Frame): Anchor? {
    // IMAGE_PIXELS -> VIEW
    convertFloats[0] = xImage
    convertFloats[1] = yImage
    frame.transformCoordinates2d(
      Coordinates2d.IMAGE_PIXELS,
      convertFloats,
      Coordinates2d.VIEW,
      convertFloatsOut
    )

    // Conduct a hit test using the VIEW coordinates
    val hits = frame.hitTest(convertFloatsOut[0], convertFloatsOut[1])
    val result = hits.getOrNull(0) ?: return null
    return result.trackable.createAnchor(result.hitPose)
  }
}

data class ARLabeledAnchor(val anchor: Anchor, val label: String)