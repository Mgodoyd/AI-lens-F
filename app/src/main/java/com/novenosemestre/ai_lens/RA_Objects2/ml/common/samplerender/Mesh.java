package com.novenosemestre.ai_lens.RA_Objects2.ml.common.samplerender;

import android.opengl.GLES30;
import android.util.Log;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import de.javagl.obj.Obj;
import de.javagl.obj.ObjData;
import de.javagl.obj.ObjReader;
import de.javagl.obj.ObjUtils;


public class Mesh implements Closeable {
  private static final String TAG = Mesh.class.getSimpleName();


  public enum PrimitiveMode {
    POINTS(GLES30.GL_POINTS),
    LINE_STRIP(GLES30.GL_LINE_STRIP),
    LINE_LOOP(GLES30.GL_LINE_LOOP),
    LINES(GLES30.GL_LINES),
    TRIANGLE_STRIP(GLES30.GL_TRIANGLE_STRIP),
    TRIANGLE_FAN(GLES30.GL_TRIANGLE_FAN),
    TRIANGLES(GLES30.GL_TRIANGLES);

    /* package-private */
    final int glesEnum;

    private PrimitiveMode(int glesEnum) {
      this.glesEnum = glesEnum;
    }
  }

  private final int[] vertexArrayId = {0};
  private final PrimitiveMode primitiveMode;
  private final IndexBuffer indexBuffer;
  private final VertexBuffer[] vertexBuffers;

  /**
   * Constructor for the Mesh class.
   * Initializes the Mesh with the given primitive mode, index buffer, and array of vertex buffers.
   * If the array of vertex buffers is null or empty, an IllegalArgumentException is thrown.
   * A vertex array is then generated and bound.
   * If the index buffer is not null, it is bound to the vertex array.
   * Each vertex buffer in the array of vertex buffers is then bound to the vertex array and enabled.
   * If an error occurs while initializing the Mesh, the Mesh is closed and the error is rethrown.
   *
   * @param render The SampleRender instance associated with this Mesh.
   * @param primitiveMode The primitive mode of this Mesh.
   * @param indexBuffer The index buffer of this Mesh.
   * @param vertexBuffers The array of vertex buffers of this Mesh.
   * @throws IllegalArgumentException If the array of vertex buffers is null or empty.
   * @throws "GLException" If an OpenGL error occurs while initializing the Mesh.
   */
  public Mesh(
      SampleRender render,
      PrimitiveMode primitiveMode,
      IndexBuffer indexBuffer,
      VertexBuffer[] vertexBuffers) {
    if (vertexBuffers == null || vertexBuffers.length == 0) {
      throw new IllegalArgumentException("Must pass at least one vertex buffer");
    }

    this.primitiveMode = primitiveMode;
    this.indexBuffer = indexBuffer;
    this.vertexBuffers = vertexBuffers;

    try {
      // Create vertex array
      GLES30.glGenVertexArrays(1, vertexArrayId, 0);
      GLError.maybeThrowGLException("Failed to generate a vertex array", "glGenVertexArrays");

      // Bind vertex array
      GLES30.glBindVertexArray(vertexArrayId[0]);
      GLError.maybeThrowGLException("Failed to bind vertex array object", "glBindVertexArray");

      if (indexBuffer != null) {
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, indexBuffer.getBufferId());
      }

      for (int i = 0; i < vertexBuffers.length; ++i) {
        // Bind each vertex buffer to vertex array
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vertexBuffers[i].getBufferId());
        GLError.maybeThrowGLException("Failed to bind vertex buffer", "glBindBuffer");
        GLES30.glVertexAttribPointer(
            i, vertexBuffers[i].getNumberOfEntriesPerVertex(), GLES30.GL_FLOAT, false, 0, 0);
        GLError.maybeThrowGLException(
            "Failed to associate vertex buffer with vertex array", "glVertexAttribPointer");
        GLES30.glEnableVertexAttribArray(i);
        GLError.maybeThrowGLException(
            "Failed to enable vertex buffer", "glEnableVertexAttribArray");
      }
    } catch (Throwable t) {
      close();
      throw t;
    }
  }

  /**
   * Creates a Mesh from an asset file.
   * The asset file is opened as an InputStream, which is then read into an Obj.
   * The Obj is converted into a renderable format.
   * The data from the Obj is obtained as direct buffers, including the vertex indices, local coordinates, texture coordinates, and normals.
   * Vertex buffers are created for the local coordinates, texture coordinates, and normals.
   * An index buffer is created for the vertex indices.
   * A new Mesh is then created with the SampleRender instance, the primitive mode set to TRIANGLES, the index buffer, and the array of vertex buffers.
   * The new Mesh is returned.
   *
   * @param render The SampleRender instance associated with this Mesh.
   * @param assetFileName The name of the asset file to create the Mesh from.
   * @return The new Mesh created from the asset file.
   * @throws IOException If an I/O error occurs while reading the asset file.
   */
  public static Mesh createFromAsset(SampleRender render, String assetFileName) throws IOException {
    try (InputStream inputStream = render.getAssets().open(assetFileName)) {
      Obj obj = ObjUtils.convertToRenderable(ObjReader.read(inputStream));

      // Obtain the data from the OBJ, as direct buffers:
      IntBuffer vertexIndices = ObjData.getFaceVertexIndices(obj, /*numVerticesPerFace=*/ 3);
      FloatBuffer localCoordinates = ObjData.getVertices(obj);
      FloatBuffer textureCoordinates = ObjData.getTexCoords(obj, /*dimensions=*/ 2);
      FloatBuffer normals = ObjData.getNormals(obj);

      VertexBuffer[] vertexBuffers = {
        new VertexBuffer(render, 3, localCoordinates),
        new VertexBuffer(render, 2, textureCoordinates),
        new VertexBuffer(render, 3, normals),
      };

      IndexBuffer indexBuffer = new IndexBuffer(render, vertexIndices);

      return new Mesh(render, PrimitiveMode.TRIANGLES, indexBuffer, vertexBuffers);
    }
  }

  /**
   * Closes the Mesh.
   * If the vertex array ID is not 0, the vertex array is deleted and any errors are logged.
   */
  @Override
  public void close() {
    if (vertexArrayId[0] != 0) {
      GLES30.glDeleteVertexArrays(1, vertexArrayId, 0);
      GLError.maybeLogGLError(
          Log.WARN, TAG, "Failed to free vertex array object", "glDeleteVertexArrays");
    }
  }

  /**
   * Performs a low-level draw operation on the Mesh.
   * If the vertex array ID is 0, an IllegalStateException is thrown.
   * The vertex array is then bound.
   * If the index buffer is null, a sanity check is performed to ensure that all vertex buffers have the same number of vertices.
   * If any vertex buffers have a different number of vertices, an IllegalStateException is thrown.
   * The vertex array is then drawn using the primitive mode and the number of vertices.
   * If the index buffer is not null, the vertex array is drawn using the primitive mode, the size of the index buffer, and the UNSIGNED_INT type.
   * If an OpenGL error occurs while performing the draw operation, a GLException is thrown.
   *
   * @throws IllegalStateException If the vertex array ID is 0 or if any vertex buffers have a different number of vertices.
   * @throws "GLException" If an OpenGL error occurs while performing the draw operation.
   */
  public void lowLevelDraw() {
    if (vertexArrayId[0] == 0) {
      throw new IllegalStateException("Tried to draw a freed Mesh");
    }

    GLES30.glBindVertexArray(vertexArrayId[0]);
    GLError.maybeThrowGLException("Failed to bind vertex array object", "glBindVertexArray");
    if (indexBuffer == null) {
      // Sanity check for debugging
      int numberOfVertices = vertexBuffers[0].getNumberOfVertices();
      for (int i = 1; i < vertexBuffers.length; ++i) {
        if (vertexBuffers[i].getNumberOfVertices() != numberOfVertices) {
          throw new IllegalStateException("Vertex buffers have mismatching numbers of vertices");
        }
      }
      GLES30.glDrawArrays(primitiveMode.glesEnum, 0, numberOfVertices);
      GLError.maybeThrowGLException("Failed to draw vertex array object", "glDrawArrays");
    } else {
      GLES30.glDrawElements(
          primitiveMode.glesEnum, indexBuffer.getSize(), GLES30.GL_UNSIGNED_INT, 0);
      GLError.maybeThrowGLException(
          "Failed to draw vertex array object with indices", "glDrawElements");
    }
  }
}
