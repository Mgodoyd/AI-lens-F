package com.google.ar.core.examples.java.ml.classification

/**
 * Data class representing a detected object result.
 *
 * @property confidence The confidence score of the detected object. This is a float value between 0 and 1, where 1 represents the highest confidence.
 * @property label The label of the detected object. This is a string representing the class or category of the detected object.
 * @property centerCoordinate A pair of integers representing the center coordinates of the detected object in the image. The first integer is the x-coordinate and the second integer is the y-coordinate.
 */
data class DetectedObjectResult(
  val confidence: Float,
  val label: String,
  val centerCoordinate: Pair<Int, Int>
)