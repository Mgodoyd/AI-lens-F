package com.google.ar.core.examples.java.ml.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.opengl.GLES30
import com.novenosemestre.ai_lens.RA_Objects2.ml.common.samplerender.GLError
import com.novenosemestre.ai_lens.RA_Objects2.ml.common.samplerender.SampleRender
import com.novenosemestre.ai_lens.RA_Objects2.ml.common.samplerender.Texture
import java.nio.ByteBuffer

class TextTextureCache {
  companion object {
    private const val TAG = "TextTextureCache"
  }

  private val cacheMap = mutableMapOf<String, Texture>()

  /**
   * Retrieves a texture from the cache or generates a new one if it doesn't exist.
   *
   * @param render The SampleRender object for rendering.
   * @param string The string to be rendered as a texture.
   * @return The texture corresponding to the input string.
   */
  fun get(render: SampleRender, string: String): Texture {
    return cacheMap.computeIfAbsent(string) {
      generateTexture(render, string)
    }
  }

  /**
   * Generates a texture from a string.
   *
   * This function creates a new texture, generates a bitmap from the input string, and populates the texture with the bitmap data.
   * It also generates mipmaps for the texture.
   *
   * @param render The SampleRender object for rendering.
   * @param string The string to be rendered as a texture.
   * @return The generated texture.
   */
  private fun generateTexture(render: SampleRender, string: String): Texture {
    val texture = Texture(render, Texture.Target.TEXTURE_2D, Texture.WrapMode.CLAMP_TO_EDGE)

    val bitmap = generateBitmapFromString(string)
    val buffer = ByteBuffer.allocateDirect(bitmap.byteCount)
    bitmap.copyPixelsToBuffer(buffer)
    buffer.rewind()

    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture.textureId)
    GLError.maybeThrowGLException("Failed to bind texture", "glBindTexture")
    GLES30.glTexImage2D(
      GLES30.GL_TEXTURE_2D,
      0,
      GLES30.GL_RGBA8,
      bitmap.width,
      bitmap.height,
      0,
      GLES30.GL_RGBA,
      GLES30.GL_UNSIGNED_BYTE,
      buffer
    )
    GLError.maybeThrowGLException("Failed to populate texture data", "glTexImage2D")
    GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D)
    GLError.maybeThrowGLException("Failed to generate mipmaps", "glGenerateMipmap")

    return texture
  }

  // Paint object for rendering the text in the texture.
  val textPaint = Paint().apply {
    textSize = 26f
    setARGB(0xff, 0xea, 0x43, 0x35)
    style = Paint.Style.FILL
    isAntiAlias = true
    textAlign = Paint.Align.CENTER
    typeface = Typeface.DEFAULT_BOLD
    strokeWidth = 2f
  }

  // Paint object for rendering the stroke of the text in the texture.
  val strokePaint = Paint(textPaint).apply {
    setARGB(0xff, 0x00, 0x00, 0x00)
    style = Paint.Style.STROKE
  }

  /**
   * Generates a bitmap from a string.
   *
   * This function creates a new bitmap, sets its color to transparent, and draws the input string onto it.
   * The string is drawn twice: once for the stroke and once for the text itself.
   *
   * @param string The string to be rendered as a bitmap.
   * @return The generated bitmap.
   */
  private fun generateBitmapFromString(string: String): Bitmap {
    val w = 256
    val h = 256
    return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
      eraseColor(0)

      Canvas(this).apply {
        drawText(string, w / 2f, h / 2f, strokePaint)

        drawText(string, w / 2f, h / 2f, textPaint)
      }
    }
  }
}