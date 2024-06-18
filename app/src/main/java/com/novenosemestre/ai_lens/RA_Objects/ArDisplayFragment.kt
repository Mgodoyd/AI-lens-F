package com.novenosemestre.ai_lens.RA_Objects
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.filament.ColorGrading
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.EngineInstance
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import com.google.mlkit.vision.objects.DetectedObject
import com.novenosemestre.ai_lens.R
import com.novenosemestre.ai_lens.databinding.ArFragmentBinding
import com.novenosemestre.ai_lens.databinding.ViewnodeRenderBinding

typealias IdAnalyzer = (detectedObject: DetectedObject) -> Unit
class ArDisplayFragment : Fragment(R.layout.ar_fragment), Scene.OnUpdateListener {

    private val TAG = "arfragment"
    private val faceInfo = Node()
    private lateinit var binding: ArFragmentBinding // ViewBinding
    private var callbackThread = HandlerThread("callback-worker") // Background thread
    private lateinit var callbackHandler: Handler
    private var viewRenderable: ViewRenderable? = null
    private var isInitialized = false
    private var arFragment: ArFragment? = null

   /**
     * This function is called when the view is created.
     * It initializes the binding and the AR fragment, starts a background thread if not already started,
     * and sets up click listeners for two text views.
     *
     * @param view The view that was created.
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle). Note: Otherwise it is null.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bind the view with the ArFragmentBinding
        binding = ArFragmentBinding.bind(view)

        // Find the AR fragment in the child fragment manager
        arFragment = childFragmentManager.findFragmentById(R.id.ux_fragment) as? ArFragment

        // If the callback thread is not initialized, start it and set the callback handler
        if (!isInitialized) {
            callbackThread.start()
            callbackHandler = Handler(callbackThread.looper)
            isInitialized = true
        }

        // Set a click listener for textView2 that adds an update listener to the AR scene and makes the trackingState view visible
        binding.textView2.setOnClickListener {
            arFragment?.arSceneView?.scene?.addOnUpdateListener(this)
            binding.trackingState.visibility = View.VISIBLE
        }

        // Set a click listener for textView3 that removes the update listener from the AR scene
        binding.textView3.setOnClickListener {
            arFragment?.arSceneView?.scene?.removeOnUpdateListener(this)
        }

        setupRenderer()
    }

    /**
     * Sets up the renderer for the AR scene.
     * This function is called in onViewCreated. It gets the renderer from the AR scene view and sets the color grading.
     * The color grading is set to FILMIC tone mapping for a cinematic look.
     */
    private fun setupRenderer() {
        arFragment?.arSceneView?.let { arSceneView ->
            val renderer = arSceneView.renderer
            renderer?.filamentView?.colorGrading = ColorGrading.Builder()
                .toneMapping(ColorGrading.ToneMapping.FILMIC)
                .build(EngineInstance.getEngine().filamentEngine)
        }
    }

    /**
     * Called on each frame update.
     * This function is currently commented out and does nothing.
     *
     * @param frameTime The time passed since the last frame.
     */
    override fun onUpdate(frameTime: FrameTime?) {
     //   onUpdateFrame(frameTime)
    }

    /**
     * Called when the fragment is resumed.
     * This function calls the super class's onResume method and also resumes the AR fragment.
     */
    override fun onResume() {
        super.onResume()
        arFragment?.onResume()
    }

    /**
     * Called when the fragment is paused.
     * This function calls the super class's onPause method and also pauses the AR fragment.
     */
    override fun onPause() {
        super.onPause()
        arFragment?.onPause()
    }

    /**
     * Called when the fragment is detached from its activity.
     * This function calls the super class's onDetach method and also detaches the AR fragment.
     */
    override fun onDetach() {
        super.onDetach()
        arFragment?.onDetach()
    }

    /**
     * Called before the fragment is destroyed.
     * This function calls the super class's onDestroy method and also detaches the AR fragment.
     */
    override fun onDestroy() {
        super.onDestroy()
        arFragment?.onDetach()
    }

    /*  private fun onUpdateFrame(frameTime: FrameTime?) {
        val arFrame = arFragment?.arSceneView?.arFrame ?: return
        copyPixelFromView(arFragment!!.arSceneView) { bitmap ->
            val targetBitmap = Bitmap.createBitmap(bitmap)
            val detector = ObjectDetector(image = targetBitmap) { detections ->
                detections.forEach { detection: DetectedObject ->
                    Toast.makeText(
                        requireContext(),
                        "Tracking ${detection.trackingId} ${detection.labels[0].text}",
                        Toast.LENGTH_SHORT
                    ).show()
                    arFragment?.arSceneView?.scene?.removeOnUpdateListener(this)
                    loadModels(detection.labels[0].text, detection.boundingBox)
                }
            }
            detector.useCustomObjectDetector()
        }
    }*/


    /**
     * Copies pixels from a given view into a bitmap and passes the bitmap to a callback function.
     * This function creates a bitmap with the same dimensions as the view and requests a pixel copy from the view into the bitmap.
     * If the pixel copy is successful, the callback function is called with the bitmap.
     * If the pixel copy fails, an error message is logged.
     *
     * @param view The view to copy pixels from.
     * @param callback The function to call with the copied bitmap.
     */
    private fun copyPixelFromView(view: SurfaceView, callback: (Bitmap) -> Unit) {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        PixelCopy.request(view, bitmap, { copyResult ->
            if (copyResult == PixelCopy.SUCCESS) {
                callback(bitmap)
            } else {
                Log.e(TAG, "Failed to copy ArFragment view.")
            }
        }, callbackHandler)
    }

    /**
     * Loads a model with a given text and bounding box.
     * This function inflates a ViewnodeRenderBinding, sets the text of the label in the binding, and builds a ViewRenderable with the root of the binding.
     * If the ViewRenderable is built successfully, the renderable of faceInfo is set to the ViewRenderable and the addDrawable function is called with the bounding box.
     * If the ViewRenderable fails to build, a toast message is shown.
     *
     * @param text The text to set in the label of the ViewnodeRenderBinding.
     * @param boundingBox The bounding box to pass to the addDrawable function.
     */
    private fun loadModels(text: String, boundingBox: RectF) {
        val root = ViewnodeRenderBinding.inflate(layoutInflater)
        root.label.text = text
        ViewRenderable.builder()
            .setView(requireContext(), root.root)
            .build()
            .thenAccept { view ->
                viewRenderable = view
                viewRenderable?.isShadowCaster = false
                viewRenderable?.isShadowReceiver = false
                faceInfo.renderable = viewRenderable
                addDrawable(boundingBox)
            }
            .exceptionally {
                Toast.makeText(requireContext(), "Unable to load model", Toast.LENGTH_LONG).show()
                null
            }
    }

    /**
     * Adds a drawable to the AR scene with a given bounding box.
     * This function gets the AR frame from the AR scene view and performs a hit test at the screen center.
     * It creates an anchor at the hit pose and adds a TransformableNode with the viewRenderable to the anchor.
     * The position and scale of the TransformableNode are adjusted based on the bounding box.
     *
     * @param boundingBox The bounding box to adjust the position and scale of the TransformableNode.
     */
    private fun addDrawable(boundingBox: RectF) {
        val frame = arFragment?.arSceneView?.arFrame ?: return
        val hitTest = frame.hitTest(screenCenter().x, screenCenter().y)
        val hitResult = hitTest[0]
        val modelAnchor = arFragment!!.arSceneView.session!!.createAnchor(hitResult.hitPose)
        val anchorNode = AnchorNode(modelAnchor)
        anchorNode.setParent(arFragment!!.arSceneView.scene)

        val transformableNode = TransformableNode(arFragment!!.transformationSystem)
        transformableNode.scaleController.maxScale = 0.5f
        transformableNode.scaleController.minScale = 0.1f
        transformableNode.setParent(anchorNode)
        transformableNode.renderable = viewRenderable

        // Adjust position and scale based on bounding box
        val position = hitResult.hitPose.transformPoint(floatArrayOf(
            boundingBox.centerX(),
            boundingBox.centerY(),
            0f
        ))

        transformableNode.worldPosition = Vector3(
            position[0],
            position[1],
            position[2]
        )
    }

    /**
     * Returns the screen center of the frame.
     * This function gets the root of the binding and returns a Vector3 with the x and y coordinates set to the center of the root's width and height respectively.
     *
     * @return A Vector3 representing the screen center of the frame.
     */
    private fun screenCenter(): Vector3 {
        val vw = binding.root
        return Vector3(vw.width / 2f, vw.height / 2f, 0f)
    }
}
