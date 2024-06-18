package com.novenosemestre.ai_lens.RA_Objects

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.TextureView
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.novenosemestre.ai_lens.R
import com.novenosemestre.ai_lens.ml.SsdMobilenetV11Metadata1
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp


class TextureViewActivity : AppCompatActivity() {

    var colors = listOf<Int>(
        Color.BLUE, Color.GREEN, Color.RED, Color.CYAN, Color.GRAY, Color.BLACK,
        Color.DKGRAY, Color.MAGENTA, Color.YELLOW, Color.RED)
    val paint = Paint()
    lateinit var labels:List<String>
    private lateinit var imageProcessor: ImageProcessor
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraDevice: CameraDevice
    private lateinit var handler: Handler
    private lateinit var bitmap: Bitmap
    private lateinit var model: SsdMobilenetV11Metadata1

   /**
    * Called when the activity is starting.
    * This method is a part of the Android activity lifecycle and it's the first method to be called when the activity is created.
    * It's where most initialization happens: calling the super class's onCreate method, setting the user interface layout for this activity,
    * loading labels from a file, building an image processor, creating a new instance of the model, starting a handler thread, and setting up a texture view.
    *
    * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle). Note: Otherwise it is null.
    */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.detection_object)

        // Load labels from a file
        labels = FileUtil.loadLabels(this, "labels.txt")

        // Build an image processor
        imageProcessor = ImageProcessor.Builder().add(ResizeOp(300, 300, ResizeOp.ResizeMethod.BILINEAR)).build()

        // Create a new instance of the model
        model = SsdMobilenetV11Metadata1.newInstance(this)

        // Start a handler thread
        val handlerThread = HandlerThread("videoThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)

        // Set up a texture view
        val textureView = findViewById<TextureView>(R.id.textureView)
        val imageView = findViewById<ImageView>(R.id.imageView)

        // Set a listener for the texture view's surface texture
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            // Open the camera when the surface texture is available
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                open_camera()
            }

            // Do nothing when the surface texture size changes
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            }

            // Do nothing when the surface texture is destroyed
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                return false
            }

            // Process the image and draw the results on the image view when the surface texture is updated
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                // Process the image
                bitmap = textureView.bitmap!!
                var image = TensorImage.fromBitmap(bitmap)
                image = imageProcessor.process(image)

                // Get the results of the model
                val outputs = model.process(image)
                val locations = outputs.locationsAsTensorBuffer.floatArray
                val classes = outputs.classesAsTensorBuffer.floatArray
                val scores = outputs.scoresAsTensorBuffer.floatArray
                val numberOfDetections = outputs.numberOfDetectionsAsTensorBuffer.floatArray

                // Draw the results on the image view
                var mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                var canvas = Canvas(mutable)
                val h = mutable.height
                val w = mutable.width
                paint.textSize = h/15f
                paint.strokeWidth = h/85f
                var x = 0
                scores.forEachIndexed { index, fl ->
                    x = index
                    x *= 4
                    if(fl > 0.5){
                        paint.setColor(colors.get(index))
                        paint.style = Paint.Style.STROKE
                        canvas.drawRect(RectF(locations.get(x+1)*w, locations.get(x)*h, locations.get(x+3)*w, locations.get(x+2)*h), paint)
                        paint.style = Paint.Style.FILL
                        canvas.drawText(labels.get(classes.get(index).toInt())+" "+fl.toString(), locations.get(x+1)*w, locations.get(x)*h, paint)
                    }
                }

                // Set the image view's bitmap
                imageView.setImageBitmap(mutable)
            }
        }

        // Get the camera manager
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    /**
     * Called before the activity is destroyed.
     * This method is a part of the Android activity lifecycle and it's the last method to be called before the activity is destroyed.
     * It's where most cleanup happens: calling the super class's onDestroy method and closing the model.
     */
    override fun onDestroy() {
        super.onDestroy()
        model.close()
    }

    /**
     * Opens the camera and starts a capture session.
     * This function is annotated with SuppressLint for "MissingPermission" because it's assumed that the necessary camera permissions are handled elsewhere in the code.
     * It first opens the camera using the camera manager. The camera manager's openCamera method takes three parameters: the camera ID, a state callback, and a handler.
     * The state callback is an anonymous class that overrides three methods: onOpened, onDisconnected, and onError.
     * The onOpened method is called when the camera is opened. It sets the camera device, creates a surface from the texture view's surface texture, creates a capture request that targets the surface, and starts a capture session.
     * The capture session's state callback is another anonymous class that overrides two methods: onConfigured and onConfigureFailed.
     * The onConfigured method is called when the capture session is configured. It sets a repeating request for the capture session.
     * The onConfigureFailed method is called when the capture session fails to configure. It doesn't do anything.
     * The onDisconnected and onError methods are called when the camera is disconnected or an error occurs. They don't do anything.
     */
    @SuppressLint("MissingPermission")
    fun open_camera(){
        cameraManager.openCamera(cameraManager.cameraIdList[0], object: CameraDevice.StateCallback(){
            override fun onOpened(p0: CameraDevice) {
                cameraDevice = p0

                var surfaceTexture = findViewById<TextureView>(R.id.textureView).surfaceTexture
                var surface = Surface(surfaceTexture)

                var captureRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                captureRequest.addTarget(surface)

                cameraDevice.createCaptureSession(listOf(surface), object: CameraCaptureSession.StateCallback(){
                    override fun onConfigured(p0: CameraCaptureSession) {
                        p0.setRepeatingRequest(captureRequest.build(), null, null)
                    }
                    override fun onConfigureFailed(p0: CameraCaptureSession) {
                    }
                }, handler)
            }

            override fun onDisconnected(p0: CameraDevice) {

            }

            override fun onError(p0: CameraDevice, p1: Int) {

            }
        }, handler)
    }
}