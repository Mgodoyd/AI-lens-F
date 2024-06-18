package com.novenosemestre.ai_lens.RA_Objects2.ml.common.samplerender;

import android.opengl.GLES30;
import android.util.Log;

import java.io.Closeable;

/** A framebuffer associated with a texture. */
public class Framebuffer implements Closeable {
  private static final String TAG = Framebuffer.class.getSimpleName();

  private final int[] framebufferId = {0};
  private final Texture colorTexture;
  private final Texture depthTexture;
  private int width = -1;
  private int height = -1;

  public Framebuffer(SampleRender render, int width, int height) {
    try {
      colorTexture =
          new Texture(
              render,
              Texture.Target.TEXTURE_2D,
              Texture.WrapMode.CLAMP_TO_EDGE,
              /*useMipmaps=*/ false);
      depthTexture =
          new Texture(
              render,
              Texture.Target.TEXTURE_2D,
              Texture.WrapMode.CLAMP_TO_EDGE,
              /*useMipmaps=*/ false);

      // Set parameters of the depth texture so that it's readable by shaders.
      GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, depthTexture.getTextureId());
      GLError.maybeThrowGLException("Failed to bind depth texture", "glBindTexture");
      GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_COMPARE_MODE, GLES30.GL_NONE);
      GLError.maybeThrowGLException("Failed to set texture parameter", "glTexParameteri");
      GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
      GLError.maybeThrowGLException("Failed to set texture parameter", "glTexParameteri");
      GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);
      GLError.maybeThrowGLException("Failed to set texture parameter", "glTexParameteri");

      // Set initial dimensions.
      resize(width, height);

      // Create framebuffer object and bind to the color and depth textures.
      GLES30.glGenFramebuffers(1, framebufferId, 0);
      GLError.maybeThrowGLException("Framebuffer creation failed", "glGenFramebuffers");
      GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, framebufferId[0]);
      GLError.maybeThrowGLException("Failed to bind framebuffer", "glBindFramebuffer");
      GLES30.glFramebufferTexture2D(
          GLES30.GL_FRAMEBUFFER,
          GLES30.GL_COLOR_ATTACHMENT0,
          GLES30.GL_TEXTURE_2D,
          colorTexture.getTextureId(),
          /*level=*/ 0);
      GLError.maybeThrowGLException(
          "Failed to bind color texture to framebuffer", "glFramebufferTexture2D");
      GLES30.glFramebufferTexture2D(
          GLES30.GL_FRAMEBUFFER,
          GLES30.GL_DEPTH_ATTACHMENT,
          GLES30.GL_TEXTURE_2D,
          depthTexture.getTextureId(),
          /*level=*/ 0);
      GLError.maybeThrowGLException(
          "Failed to bind depth texture to framebuffer", "glFramebufferTexture2D");

      int status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER);
      if (status != GLES30.GL_FRAMEBUFFER_COMPLETE) {
        throw new IllegalStateException("Framebuffer construction not complete: code " + status);
      }
    } catch (Throwable t) {
      close();
      throw t;
    }
  }

   /**
   * Closes the Framebuffer and releases its resources.
   * If the framebuffer is not already closed, it deletes the framebuffer from OpenGL and logs any errors.
   * It then closes the color and depth textures associated with this framebuffer.
   */
  @Override
  public void close() {
    // Check if the framebuffer is not already closed
    if (framebufferId[0] != 0) {
      // Delete the framebuffer from OpenGL
      GLES30.glDeleteFramebuffers(1, framebufferId, 0);
      // Log any errors that occurred while deleting the framebuffer
      GLError.maybeLogGLError(Log.WARN, TAG, "Failed to free framebuffer", "glDeleteFramebuffers");
      // Mark the framebuffer as closed
      framebufferId[0] = 0;
    }
    // Close the color texture
    colorTexture.close();
    // Close the depth texture
    depthTexture.close();
  }

  public void resize(int width, int height) {
    if (this.width == width && this.height == height) {
      return;
    }
    this.width = width;
    this.height = height;

    // Color texture
    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, colorTexture.getTextureId());
    GLError.maybeThrowGLException("Failed to bind color texture", "glBindTexture");
    GLES30.glTexImage2D(
        GLES30.GL_TEXTURE_2D,
        /*level=*/ 0,
        GLES30.GL_RGBA,
        width,
        height,
        /*border=*/ 0,
        GLES30.GL_RGBA,
        GLES30.GL_UNSIGNED_BYTE,
        /*pixels=*/ null);
    GLError.maybeThrowGLException("Failed to specify color texture format", "glTexImage2D");

    // Depth texture
    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, depthTexture.getTextureId());
    GLError.maybeThrowGLException("Failed to bind depth texture", "glBindTexture");
    GLES30.glTexImage2D(
        GLES30.GL_TEXTURE_2D,
        /*level=*/ 0,
        GLES30.GL_DEPTH_COMPONENT32F,
        width,
        height,
        /*border=*/ 0,
        GLES30.GL_DEPTH_COMPONENT,
        GLES30.GL_FLOAT,
        /*pixels=*/ null);
    GLError.maybeThrowGLException("Failed to specify depth texture format", "glTexImage2D");
  }

  /**
   * Returns the color texture associated with this framebuffer.
   *
   * @return The color texture.
   */
  public Texture getColorTexture() {
    return colorTexture;
  }

  /**
   * Returns the depth texture associated with this framebuffer.
   *
   * @return The depth texture.
   */
  public Texture getDepthTexture() {
    return depthTexture;
  }

  /**
   * Returns the width of the framebuffer.
   *
   * @return The width of the framebuffer.
   */
  public int getWidth() {
    return width;
  }

  /**
   * Returns the height of the framebuffer.
   *
   * @return The height of the framebuffer.
   */
  public int getHeight() {
    return height;
  }

  /**
   * Returns the ID of the framebuffer.
   * This method is package-private, meaning it can only be accessed within the same package.
   *
   * @return The ID of the framebuffer.
   */
  /* package-private */
  int getFramebufferId() {
    return framebufferId[0];
  }
}
