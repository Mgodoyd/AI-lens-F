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

  /**
   * Converts a YUV image to RGB and stores the result in a bitmap.
   *
   * This function first checks if the YUV buffer is initialized. If not, it calculates the pixel count
   * and the pixel size in bits, and initializes the YUV buffer.
   * Then, it converts the image to a byte array and stores the result in the YUV buffer.
   * If the input and output allocations are not initialized, it creates them.
   * Finally, it converts the NV21 format YUV image to RGB and stores the result in the output bitmap.
   *
   * @param image The YUV image to convert.
   * @param output The bitmap to store the result.
   */
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

  /**
   * Converts an image to a byte array and stores the result in a buffer.
   *
   * This function first asserts that the image format is YUV_420_888.
   * Then, it iterates over the image planes and for each plane, it calculates the output stride and offset,
   * gets the plane buffer, row stride, and pixel stride, and calculates the plane crop, width, and height.
   * It also creates a row buffer and calculates the row length.
   * Finally, it iterates over the rows and columns of the plane, and stores the result in the output buffer.
   *
   * @param image The image to convert.
   * @param outputBuffer The buffer to store the result.
   */
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