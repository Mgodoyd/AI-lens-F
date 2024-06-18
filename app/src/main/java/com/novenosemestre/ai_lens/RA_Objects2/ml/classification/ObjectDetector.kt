package com.google.ar.core.examples.java.ml.classification

import android.content.Context
import android.graphics.Bitmap
import android.media.Image
import com.novenosemestre.ai_lens.YuvToRgbConverter

abstract class ObjectDetector(val context: Context) {
  val yuvConverter = YuvToRgbConverter(context)

  /**
   * Analyzes an image and returns a list of detected objects.
   *
   * This is an abstract function that needs to be implemented by subclasses.
   * The implementation should analyze the image and detect objects in it.
   * The detected objects should be returned as a list of DetectedObjectResult.
   *
   * @param image The image to analyze. This is an android.media.Image object.
   * @param imageRotation The rotation of the image in degrees. This is an integer.
   * @return A list of DetectedObjectResult representing the detected objects.
   */
  abstract suspend fun analyze(image: Image, imageRotation: Int): List<DetectedObjectResult>

  /**
   * Converts a YUV image to a Bitmap.
   *
   * This function uses a YuvToRgbConverter to convert the image from YUV format to RGB format.
   * The converted image is returned as a Bitmap.
   *
   * @param image The image to convert. This is an android.media.Image object in YUV format.
   * @return The converted image as a Bitmap.
   */
  fun convertYuv(image: Image): Bitmap {
    return Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888).apply {
      yuvConverter.yuvToRgb(image, this)
    }
  }
}