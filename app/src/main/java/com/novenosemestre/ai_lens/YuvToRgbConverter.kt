/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.novenosemestre.ai_lens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import android.media.Image
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicYuvToRGB
import android.renderscript.Type

class YuvToRgbConverter(context: Context) {
  private val rs = RenderScript.create(context)
  private val scriptYuvToRgb = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs))

  private var pixelCount: Int = -1
  private lateinit var yuvBuffer: ByteArray
  private lateinit var inputAllocation: Allocation
  private lateinit var outputAllocation: Allocation

  @Synchronized
  fun yuvToRgb(image: Image, output: Bitmap) {

    if (!::yuvBuffer.isInitialized) {
      pixelCount = image.width * image.height
      val pixelSizeBits = ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888)
      yuvBuffer = ByteArray(pixelCount * pixelSizeBits / 8)
    }

    imageToByteArray(image, yuvBuffer)

    if (!::inputAllocation.isInitialized) {
      val elemType = Type.Builder(rs, Element.YUV(rs)).setYuvFormat(ImageFormat.NV21).create()
      inputAllocation = Allocation.createSized(rs, elemType.element, yuvBuffer.size)
    }
    if (!::outputAllocation.isInitialized) {
      outputAllocation = Allocation.createFromBitmap(rs, output)
    }

    // Convert NV21 format YUV to RGB
    inputAllocation.copyFrom(yuvBuffer)
    scriptYuvToRgb.setInput(inputAllocation)
    scriptYuvToRgb.forEach(outputAllocation)
    outputAllocation.copyTo(output)
  }

  private fun imageToByteArray(image: Image, outputBuffer: ByteArray) {
    assert(image.format == ImageFormat.YUV_420_888)

    val imageCrop = Rect(0, 0, image.width, image.height)
    val imagePlanes = image.planes

    imagePlanes.forEachIndexed { planeIndex, plane ->
      val outputStride: Int
      var outputOffset: Int

      when (planeIndex) {
        0 -> {
          outputStride = 1
          outputOffset = 0
        }
        1 -> {
          outputStride = 2
          outputOffset = pixelCount + 1
        }
        2 -> {
          outputStride = 2
          outputOffset = pixelCount
        }
        else -> {
          return@forEachIndexed
        }
      }

      val planeBuffer = plane.buffer
      val rowStride = plane.rowStride
      val pixelStride = plane.pixelStride

      val planeCrop = if (planeIndex == 0) {
        imageCrop
      } else {
        Rect(
          imageCrop.left / 2,
          imageCrop.top / 2,
          imageCrop.right / 2,
          imageCrop.bottom / 2
        )
      }

      val planeWidth = planeCrop.width()
      val planeHeight = planeCrop.height()

      val rowBuffer = ByteArray(plane.rowStride)

      val rowLength = if (pixelStride == 1 && outputStride == 1) {
        planeWidth
      } else {
        (planeWidth - 1) * pixelStride + 1
      }

      for (row in 0 until planeHeight) {
        planeBuffer.position(
          (row + planeCrop.top) * rowStride + planeCrop.left * pixelStride
        )

        if (pixelStride == 1 && outputStride == 1) {
          planeBuffer.get(outputBuffer, outputOffset, rowLength)
          outputOffset += rowLength
        } else {
          planeBuffer.get(rowBuffer, 0, rowLength)
          for (col in 0 until planeWidth) {
            outputBuffer[outputOffset] = rowBuffer[col * pixelStride]
            outputOffset += outputStride
          }
        }
      }
    }
  }
}