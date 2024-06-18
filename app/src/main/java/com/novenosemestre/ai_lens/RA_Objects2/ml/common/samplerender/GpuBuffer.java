package com.novenosemestre.ai_lens.RA_Objects2.ml.common.samplerender;

import android.opengl.GLES30;
import android.util.Log;

import java.nio.Buffer;

/* package-private */
class GpuBuffer {
  private static final String TAG = GpuBuffer.class.getSimpleName();

  public static final int INT_SIZE = 4;
  public static final int FLOAT_SIZE = 4;

  private final int target;
  private final int numberOfBytesPerEntry;
  private final int[] bufferId = {0};
  private int size;
  private int capacity;

   /**
   * Constructor for the GpuBuffer class.
   * Initializes the GPU buffer with the given target, number of bytes per entry, and entries.
   * If the entries buffer is not null, it must be a direct buffer and its limit must not be 0.
   * If the entries buffer is null or its limit is 0, the size and capacity of the GPU buffer are set to 0.
   * Otherwise, the size and capacity of the GPU buffer are set to the limit of the entries buffer.
   * The GPU buffer is then populated with the entries buffer.
   *
   * @param target The target of the GPU buffer.
   * @param numberOfBytesPerEntry The number of bytes per entry in the GPU buffer.
   * @param entries The entries to populate the GPU buffer with.
   * @throws IllegalArgumentException If the entries buffer is not null and it is not a direct buffer.
   * @throws "GLException" If an OpenGL error occurs while initializing the GPU buffer.
   */
  public GpuBuffer(int target, int numberOfBytesPerEntry, Buffer entries) {
    if (entries != null) {
      if (!entries.isDirect()) {
        throw new IllegalArgumentException("If non-null, entries buffer must be a direct buffer");
      }
      if (entries.limit() == 0) {
        entries = null;
      }
    }

    this.target = target;
    this.numberOfBytesPerEntry = numberOfBytesPerEntry;
    if (entries == null) {
      this.size = 0;
      this.capacity = 0;
    } else {
      this.size = entries.limit();
      this.capacity = entries.limit();
    }

    try {
      // Clear VAO to prevent unintended state change.
      GLES30.glBindVertexArray(0);
      GLError.maybeThrowGLException("Failed to unbind vertex array", "glBindVertexArray");

      GLES30.glGenBuffers(1, bufferId, 0);
      GLError.maybeThrowGLException("Failed to generate buffers", "glGenBuffers");

      GLES30.glBindBuffer(target, bufferId[0]);
      GLError.maybeThrowGLException("Failed to bind buffer object", "glBindBuffer");

      if (entries != null) {
        entries.rewind();
        GLES30.glBufferData(
            target, entries.limit() * numberOfBytesPerEntry, entries, GLES30.GL_DYNAMIC_DRAW);
      }
      GLError.maybeThrowGLException("Failed to populate buffer object", "glBufferData");
    } catch (Throwable t) {
      free();
      throw t;
    }
  }

  /**
   * Sets the entries of the GPU buffer.
   * If the entries buffer is null or its limit is 0, the size of the GPU buffer is set to 0.
   * Otherwise, the entries buffer must be a direct buffer.
   * If the limit of the entries buffer is less than or equal to the capacity of the GPU buffer,
   * the entries buffer is used to update the existing data in the GPU buffer and the size of the GPU buffer is set to the limit of the entries buffer.
   * Otherwise, the entries buffer is used to populate the GPU buffer and the size and capacity of the GPU buffer are set to the limit of the entries buffer.
   *
   * @param entries The entries to set in the GPU buffer.
   * @throws IllegalArgumentException If the entries buffer is not null and it is not a direct buffer.
   * @throws "GLException" If an OpenGL error occurs while setting the entries of the GPU buffer.
   */
  public void set(Buffer entries) {
    if (entries == null || entries.limit() == 0) {
      size = 0;
      return;
    }
    if (!entries.isDirect()) {
      throw new IllegalArgumentException("If non-null, entries buffer must be a direct buffer");
    }
    GLES30.glBindBuffer(target, bufferId[0]);
    GLError.maybeThrowGLException("Failed to bind vertex buffer object", "glBindBuffer");

    entries.rewind();

    if (entries.limit() <= capacity) {
      GLES30.glBufferSubData(target, 0, entries.limit() * numberOfBytesPerEntry, entries);
      GLError.maybeThrowGLException("Failed to populate vertex buffer object", "glBufferSubData");
      size = entries.limit();
    } else {
      GLES30.glBufferData(
          target, entries.limit() * numberOfBytesPerEntry, entries, GLES30.GL_DYNAMIC_DRAW);
      GLError.maybeThrowGLException("Failed to populate vertex buffer object", "glBufferData");
      size = entries.limit();
      capacity = entries.limit();
    }
  }

  /**
   * Frees the GPU buffer.
   * If the GPU buffer is not already freed, it deletes the buffer from OpenGL and logs any errors.
   * After deletion, the buffer ID is reset to 0.
   */
  public void free() {
    if (bufferId[0] != 0) {
      GLES30.glDeleteBuffers(1, bufferId, 0);
      GLError.maybeLogGLError(Log.WARN, TAG, "Failed to free buffer object", "glDeleteBuffers");
      bufferId[0] = 0;
    }
  }

   /**
   * Returns the ID of the GPU buffer.
   * This method is package-private, meaning it can only be accessed within the same package.
   *
   * @return The ID of the GPU buffer.
   */
  public int getBufferId() {
    return bufferId[0];
  }

  /**
   * Returns the size of the GPU buffer.
   * This method is package-private, meaning it can only be accessed within the same package.
   *
   * @return The size of the GPU buffer.
   */
  public int getSize() {
    return size;
  }
}
