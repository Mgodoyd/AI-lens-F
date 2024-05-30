/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ar.core.examples.java.ml.classification

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

  val credentials = try {
    val res = activity.resources.getIdentifier("credential", "raw", activity.packageName)
    if (res == 0) {
      Log.e(TAG, "Missing GCP credentials in res/raw/credentials.json.")
      error("Missing GCP credentials in res/raw/credentials.json.")
    }
    val inputStream = activity.resources.openRawResource(res)
    val jsonContent = inputStream.bufferedReader().use { it.readText() }
    Log.d(TAG, "Credentials JSON: $jsonContent")
    activity.resources.openRawResource(res).use { GoogleCredentials.fromStream(it) }
  } catch (e: Exception) {
    Log.e(TAG, "Unable to create Google credentials from res/raw/credentials.json. Cloud ML will be disabled.", e)
    null
  }


  val settings = ImageAnnotatorSettings.newBuilder().setCredentialsProvider { credentials }.build()
  val vision = ImageAnnotatorClient.create(settings)

  override suspend fun analyze(image: Image, imageRotation: Int): List<DetectedObjectResult> {
    val convertYuv = convertYuv(image)

    val rotatedImage = ImageUtils.rotateBitmap(convertYuv, imageRotation)

    // Perform request on Google Cloud Vision APIs.
    val request = createAnnotateImageRequest(rotatedImage.toByteArray())
    val response = vision.batchAnnotateImages(listOf(request))

    // Process result and map to DetectedObjectResult.
    val objectAnnotationsResult = response.responsesList.first().localizedObjectAnnotationsList
    return objectAnnotationsResult.map {
      val center = it.boundingPoly.normalizedVerticesList.calculateAverage()
      val absoluteCoordinates = center.toAbsoluteCoordinates(rotatedImage.width, rotatedImage.height)
      val rotatedCoordinates = absoluteCoordinates.rotateCoordinates(rotatedImage.width, rotatedImage.height, imageRotation)
      DetectedObjectResult(it.score, it.name, rotatedCoordinates)
    }
  }
  private fun createAnnotateImageRequest(imageBytes: ByteArray): AnnotateImageRequest {
    val image = GCVImage.newBuilder().setContent(ByteString.copyFrom(imageBytes))
    val features = Feature.newBuilder().setType(Feature.Type.OBJECT_LOCALIZATION)
    return AnnotateImageRequest.newBuilder()
      .setImage(image)
      .addFeatures(features)
      .build()
  }
}