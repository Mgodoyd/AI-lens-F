package com.novenosemestre.ai_lens.RA_Objects2.ml.common.samplerender;

import android.opengl.GLES30;

import java.io.Closeable;
import java.nio.IntBuffer;


public class IndexBuffer implements Closeable {
  // The GPU buffer used by the IndexBuffer.
  private final GpuBuffer buffer;

  /**
   * Constructor for the IndexBuffer class.
   * Initializes the IndexBuffer with a new GpuBuffer using the given entries.
   *
   * @param render The SampleRender instance associated with this IndexBuffer.
   * @param entries The entries to populate the GpuBuffer with.
   */
  public IndexBuffer(SampleRender render, IntBuffer entries) {
    buffer = new GpuBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, GpuBuffer.INT_SIZE, entries);
  }

  /**
   * Sets the entries of the GpuBuffer.
   *
   * @param entries The entries to set in the GpuBuffer.
   */
  public void set(IntBuffer entries) {
    buffer.set(entries);
  }

  /**
   * Frees the GpuBuffer.
   * This method is called when the IndexBuffer is closed.
   */
  @Override
  public void close() {
    buffer.free();
  }

  /**
   * Returns the ID of the GpuBuffer.
   * This method is package-private, meaning it can only be accessed within the same package.
   *
   * @return The ID of the GpuBuffer.
   */
  /* package-private */
  int getBufferId() {
    return buffer.getBufferId();
  }

  /**
   * Returns the size of the GpuBuffer.
   * This method is package-private, meaning it can only be accessed within the same package.
   *
   * @return The size of the GpuBuffer.
   */
  /* package-private */
  int getSize() {
    return buffer.getSize();
  }
}
