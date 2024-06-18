package com.google.ar.core.examples.java.ml.classification

import android.annotation.SuppressLint
import android.media.Image
import android.util.Log
import com.google.ar.core.examples.java.ml.classification.utils.ImageUtils
import com.google.ar.core.examples.java.ml.classification.utils.ImageUtils.toByteArray
import com.google.ar.core.examples.java.ml.classification.utils.VertexUtils.calculateAverage
import com.google.ar.core.examples.java.ml.classification.utils.VertexUtils.rotateCoordinates
import com.google.ar.core.examples.java.ml.classification.utils.VertexUtils.toAbsoluteCoordinates
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.vision.v1.AnnotateImageRequest
import com.google.cloud.vision.v1.Feature
import com.google.cloud.vision.v1.ImageAnnotatorClient
import com.google.cloud.vision.v1.ImageAnnotatorSettings
import com.google.protobuf.ByteString
import com.novenosemestre.ai_lens.RA_Objects2.MainActivity2
import com.google.cloud.vision.v1.Image as GCVImage

/**
 * https://cloud.google.com/vision/docs/object-localizer
 *
 * Finds detected objects ([DetectedObjectResult]s) given an [android.media.Image].
 */
class GoogleCloudVisionDetector(val activity: MainActivity2) : ObjectDetector(activity) {
  companion object {
    val TAG = "GoogleCloudVisionDetector"
  }

    // Initialize Google Cloud Vision credentials
    @SuppressLint("DiscouragedApi")
    val credentials = try {
    // Get the resource ID for the credentials file
    val res = activity.resources.getIdentifier("credential", "raw", activity.packageName)
    if (res == 0) {
      // Log an error and throw an exception if the credentials file is missing
      Log.e(TAG, "Missing GCP credentials in res/raw/credentials.json.")
      error("Missing GCP credentials in res/raw/credentials.json.")
    }
    // Open the credentials file and read its content
    val inputStream = activity.resources.openRawResource(res)
    val jsonContent = inputStream.bufferedReader().use { it.readText() }
    Log.d(TAG, "Credentials JSON: $jsonContent")
    // Create Google credentials from the credentials file
    activity.resources.openRawResource(res).use { GoogleCredentials.fromStream(it) }
  } catch (e: Exception) {
    // Log an error and disable Cloud ML if the credentials cannot be created
    Log.e(TAG, "Unable to create Google credentials from res/raw/credentials.json. Cloud ML will be disabled.", e)
    null
  }

  // Create ImageAnnotatorSettings with the Google Cloud Vision credentials
  val settings = ImageAnnotatorSettings.newBuilder().setCredentialsProvider { credentials }.build()
  // Create an ImageAnnotatorClient with the settings
  val vision = ImageAnnotatorClient.create(settings)

  /**
   * Analyzes an image and returns a list of detected objects.
   *
   * @param image The image to analyze.
   * @param imageRotation The rotation of the image in degrees.
   * @return A list of DetectedObjectResult representing the detected objects.
   */
  override suspend fun analyze(image: Image, imageRotation: Int): List<DetectedObjectResult> {
    // Convert the image to YUV format
    val convertYuv = convertYuv(image)

    // Rotate the image
    val rotatedImage = ImageUtils.rotateBitmap(convertYuv, imageRotation)

    // Create a request for the Google Cloud Vision APIs
    val request = createAnnotateImageRequest(rotatedImage.toByteArray())
    // Send the request and get the response
    val response = vision.batchAnnotateImages(listOf(request))

    // Process the response and map it to DetectedObjectResult
    val objectAnnotationsResult = response.responsesList.first().localizedObjectAnnotationsList
    return objectAnnotationsResult.map {
      // Calculate the center of the bounding polygon
      val center = it.boundingPoly.normalizedVerticesList.calculateAverage()
      // Convert the center's coordinates from relative to absolute
      val absoluteCoordinates = center.toAbsoluteCoordinates(rotatedImage.width, rotatedImage.height)
      // Rotate the coordinates
      val rotatedCoordinates = absoluteCoordinates.rotateCoordinates(rotatedImage.width, rotatedImage.height, imageRotation)
      // Create a DetectedObjectResult
      DetectedObjectResult(it.score, it.name, rotatedCoordinates)
    }
  }

  /**
   * Creates an AnnotateImageRequest for the Google Cloud Vision APIs.
   *
   * @param imageBytes The bytes of the image.
   * @return An AnnotateImageRequest.
   */
  private fun createAnnotateImageRequest(imageBytes: ByteArray): AnnotateImageRequest {
    // Create a Google Cloud Vision Image from the image bytes
    val image = GCVImage.newBuilder().setContent(ByteString.copyFrom(imageBytes))
    // Create a Feature for object localization
    val features = Feature.newBuilder().setType(Feature.Type.OBJECT_LOCALIZATION)
    // Build the AnnotateImageRequest
    return AnnotateImageRequest.newBuilder()
      .setImage(image)
      .addFeatures(features)
      .build()
  }
}