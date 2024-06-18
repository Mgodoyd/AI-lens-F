package com.google.ar.core.examples.java.ml.classification.utils

import android.graphics.Bitmap
import android.graphics.Matrix
import java.io.ByteArrayOutputStream

object ImageUtils {
  /**
   * Rotates a bitmap by a specified angle.
   *
   * @param bitmap The bitmap to rotate.
   * @param rotation The angle to rotate the bitmap by. This is measured in degrees.
   * @return The rotated bitmap. If the rotation angle is 0, the original bitmap is returned.
   */
  fun rotateBitmap(bitmap: Bitmap, rotation: Int): Bitmap {
    if (rotation == 0) return bitmap

    val matrix = Matrix()
    matrix.postRotate(rotation.toFloat())
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, false)
  }

  /**
   * Converts a Bitmap to a ByteArray.
   *
   * @receiver The Bitmap to convert.
   * @return The ByteArray representation of the Bitmap. The Bitmap is compressed as a JPEG with quality 100.
   */
  fun Bitmap.toByteArray(): ByteArray = ByteArrayOutputStream().use { stream ->
    this.compress(Bitmap.CompressFormat.JPEG, 100, stream)
    stream.toByteArray()
  }
}