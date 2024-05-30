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
import com.google.ar.core.Frame
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
    private lateinit var binding: ArFragmentBinding
    private var callbackThread = HandlerThread("callback-worker")
    private lateinit var callbackHandler: Handler
    private var viewRenderable: ViewRenderable? = null
    private var isInitialized = false
    private var arFragment: ArFragment? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = ArFragmentBinding.bind(view)
        arFragment = childFragmentManager.findFragmentById(R.id.ux_fragment) as? ArFragment

        if (!isInitialized) {
            callbackThread.start()
            callbackHandler = Handler(callbackThread.looper)
            isInitialized = true
        }

        binding.textView2.setOnClickListener {
            arFragment?.arSceneView?.scene?.addOnUpdateListener(this)
            binding.trackingState.visibility = View.VISIBLE
        }

        binding.textView3.setOnClickListener {
            arFragment?.arSceneView?.scene?.removeOnUpdateListener(this)
        }

        setupRenderer()
    }

    private fun setupRenderer() {
        arFragment?.arSceneView?.let { arSceneView ->
            val renderer = arSceneView.renderer
            renderer?.filamentView?.colorGrading = ColorGrading.Builder()
                .toneMapping(ColorGrading.ToneMapping.FILMIC)
                .build(EngineInstance.getEngine().filamentEngine)
        }
    }

    override fun onUpdate(frameTime: FrameTime?) {
     //   onUpdateFrame(frameTime)
    }

    override fun onResume() {
        super.onResume()
        arFragment?.onResume()
    }

    override fun onPause() {
        super.onPause()
        arFragment?.onPause()
    }

    override fun onDetach() {
        super.onDetach()
        arFragment?.onDetach()
    }

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

    private fun addDrawable(boundingBox: RectF) {
        val frame = arFragment?.arSceneView?.arFrame ?: return
        val hitTest = frame.hitTest(frame.screenCenter().x, frame.screenCenter().y)
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

    private fun Frame.screenCenter(): Vector3 {
        val vw = binding.root
        return Vector3(vw.width / 2f, vw.height / 2f, 0f)
    }
}
