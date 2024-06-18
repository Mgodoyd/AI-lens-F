package com.google.ar.core.examples.java.ml.classification.utils

import com.google.cloud.vision.v1.NormalizedVertex

object VertexUtils {
  /**
   * Converts the NormalizedVertex's coordinates from relative to absolute.
   * The NormalizedVertex's x and y coordinates are multiplied by the image's width and height respectively.
   *
   * @param imageWidth The width of the image.
   * @param imageHeight The height of the image.
   * @return A pair of integers representing the absolute x and y coordinates.
   */
  fun NormalizedVertex.toAbsoluteCoordinates(
    imageWidth: Int,
    imageHeight: Int,
  ): Pair<Int, Int> {
    return (x * imageWidth).toInt() to (y * imageHeight).toInt()
  }

  /**
   * Rotates the coordinates based on the image rotation.
   * The rotation is applied in a clockwise direction.
   *
   * @param imageWidth The width of the image.
   * @param imageHeight The height of the image.
   * @param imageRotation The rotation of the image in degrees. This should be one of the following: 0, 90, 180, 270.
   * @return A pair of integers representing the rotated x and y coordinates.
   * @throws IllegalArgumentException If the imageRotation is not one of the following: 0, 90, 180, 270.
   */
  fun Pair<Int, Int>.rotateCoordinates(
    imageWidth: Int,
    imageHeight: Int,
    imageRotation: Int,
  ): Pair<Int, Int> {
    val (x, y) = this
    return when (imageRotation) {
      0 -> x to y
      180 -> imageWidth - x to imageHeight - y
      90 -> y to imageWidth - x
      270 -> imageHeight - y to x
      else -> error("Invalid imageRotation $imageRotation")
    }
  }

  /**
   * Calculates the average x and y coordinates of a list of NormalizedVertex.
   * The average is calculated by summing up all the x and y coordinates and dividing by the size of the list.
   *
   * @return A NormalizedVertex representing the average x and y coordinates.
   */
  fun List<NormalizedVertex>.calculateAverage(): NormalizedVertex {
    var averageX = 0f
    var averageY = 0f
    for (vertex in this) {
      averageX += vertex.x / size
      averageY += vertex.y / size
    }
    return NormalizedVertex.newBuilder().setX(averageX).setY(averageY).build()
  }
}