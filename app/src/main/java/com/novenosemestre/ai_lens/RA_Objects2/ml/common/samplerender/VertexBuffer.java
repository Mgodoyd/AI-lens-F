package com.novenosemestre.ai_lens.RA_Objects2.ml.common.samplerender;

import android.opengl.GLES30;

import java.io.Closeable;
import java.nio.FloatBuffer;


public class VertexBuffer implements Closeable {
  private final GpuBuffer buffer;
  private final int numberOfEntriesPerVertex;

   /**
   * Constructor for the VertexBuffer class.
   * Initializes the VertexBuffer with the given SampleRender instance, number of entries per vertex, and FloatBuffer of entries.
   * If the entries are not null and the limit of the entries is not divisible by the number of entries per vertex, an IllegalArgumentException is thrown.
   * The number of entries per vertex is then set and a new GpuBuffer is created with the ARRAY_BUFFER target, the size of a float, and the entries.
   *
   * @param render The SampleRender instance associated with this VertexBuffer.
   * @param numberOfEntriesPerVertex The number of entries per vertex for this VertexBuffer.
   * @param entries The FloatBuffer of entries for this VertexBuffer.
   * @throws IllegalArgumentException If the entries are not null and the limit of the entries is not divisible by the number of entries per vertex.
   */
  public VertexBuffer(SampleRender render, int numberOfEntriesPerVertex, FloatBuffer entries) {
    if (entries != null && entries.limit() % numberOfEntriesPerVertex != 0) {
      throw new IllegalArgumentException(
          "If non-null, vertex buffer data must be divisible by the number of data points per"
              + " vertex");
    }

    this.numberOfEntriesPerVertex = numberOfEntriesPerVertex;
    buffer = new GpuBuffer(GLES30.GL_ARRAY_BUFFER, GpuBuffer.FLOAT_SIZE, entries);
  }

  /**
   * Sets the entries for this VertexBuffer.
   * If the entries are not null and the limit of the entries is not divisible by the number of entries per vertex, an IllegalArgumentException is thrown.
   * The entries of the GpuBuffer are then set.
   *
   * @param entries The new FloatBuffer of entries for this VertexBuffer.
   * @throws IllegalArgumentException If the entries are not null and the limit of the entries is not divisible by the number of entries per vertex.
   */
  public void set(FloatBuffer entries) {
    if (entries != null && entries.limit() % numberOfEntriesPerVertex != 0) {
      throw new IllegalArgumentException(
          "If non-null, vertex buffer data must be divisible by the number of data points per"
              + " vertex");
    }
    buffer.set(entries);
  }

  /**
   * Closes this VertexBuffer.
   * The GpuBuffer is freed.
   */
  @Override
  public void close() {
    buffer.free();
  }

  /**
   * Returns the buffer ID of this VertexBuffer.
   * This method is package-private, meaning it can only be accessed within the same package.
   *
   * @return The buffer ID of this VertexBuffer.
   */
  /* package-private */
  int getBufferId() {
    return buffer.getBufferId();
  }

  /**
   * Returns the number of entries per vertex of this VertexBuffer.
   * This method is package-private, meaning it can only be accessed within the same package.
   *
   * @return The number of entries per vertex of this VertexBuffer.
   */
  /* package-private */
  int getNumberOfEntriesPerVertex() {
    return numberOfEntriesPerVertex;
  }

  /**
   * Returns the number of vertices of this VertexBuffer.
   * This method is package-private, meaning it can only be accessed within the same package.
   *
   * @return The number of vertices of this VertexBuffer.
   */
  /* package-private */
  int getNumberOfVertices() {
    return buffer.getSize() / numberOfEntriesPerVertex;
  }
}
