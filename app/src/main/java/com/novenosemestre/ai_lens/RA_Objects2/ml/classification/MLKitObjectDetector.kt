package com.google.ar.core.examples.java.ml.classification

import android.app.Activity
import android.media.Image
import com.google.ar.core.examples.java.ml.classification.utils.ImageUtils
import com.google.ar.core.examples.java.ml.classification.utils.VertexUtils.rotateCoordinates
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.coroutines.tasks.asDeferred
/**
 * Analyzes an image using ML Kit.
 */
class MLKitObjectDetector(context: Activity) : ObjectDetector(context) {
  val builder = ObjectDetectorOptions.Builder()

  // Options for the object detector.
  // The detector is set to single image mode, which means it processes one image at a time.
  // Classification is enabled, which means the detector will try to classify the detected objects into predefined categories.
  // Multiple objects detection is enabled, which means the detector will try to detect more than one object in the image.
  private val options = builder
    .setDetectorMode(CustomObjectDetectorOptions.SINGLE_IMAGE_MODE)
    .enableClassification()
    .enableMultipleObjects()
    .build()

  // The object detector client, which is used to process the images.
  private val detector = ObjectDetection.getClient(options)

  /**
   * Analyzes an image and returns a list of detected objects.
   *
   * @param image The image to analyze. The image is in YUV format.
   * @param imageRotation The rotation of the image in degrees.
   * @return A list of DetectedObjectResult representing the detected objects.
   */
  override suspend fun analyze(image: Image, imageRotation: Int): List<DetectedObjectResult> {
    // Convert the image from YUV to RGB format
    val convertYuv = convertYuv(image)
    // Rotate the image
    val rotatedImage = ImageUtils.rotateBitmap(convertYuv, imageRotation)

    // Create an input image from the rotated bitmap
    val inputImage = InputImage.fromBitmap(rotatedImage, 0)

    // Process the image with the detector and get the detected objects
    val mlKitDetectedObjects = detector.process(inputImage).asDeferred().await()
    // Map the detected objects to DetectedObjectResult
    return mlKitDetectedObjects.mapNotNull { obj ->
      // Get the label with the highest confidence
      val bestLabel = obj.labels.maxByOrNull { label -> label.confidence } ?: return@mapNotNull null
      // Get the center coordinates of the bounding box of the detected object
      val coords = obj.boundingBox.exactCenterX().toInt() to obj.boundingBox.exactCenterY().toInt()
      // Rotate the coordinates
      val rotatedCoordinates = coords.rotateCoordinates(rotatedImage.width, rotatedImage.height, imageRotation)
      // Create a DetectedObjectResult
      DetectedObjectResult(bestLabel.confidence, bestLabel.text, rotatedCoordinates)
    }
  }

  /**
   * Checks if the builder is of type CustomObjectDetectorOptions.Builder.
   *
   * @return True if the builder is of type CustomObjectDetectorOptions.Builder, false otherwise.
   */
  @Suppress("USELESS_IS_CHECK")
  fun hasCustomModel() = builder is CustomObjectDetectorOptions.Builder
}