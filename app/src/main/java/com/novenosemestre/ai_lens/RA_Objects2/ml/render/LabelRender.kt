package com.google.ar.core.examples.java.ml.render

import com.google.ar.core.Pose
import com.novenosemestre.ai_lens.RA_Objects2.ml.common.samplerender.Mesh
import com.novenosemestre.ai_lens.RA_Objects2.ml.common.samplerender.SampleRender
import com.novenosemestre.ai_lens.RA_Objects2.ml.common.samplerender.Shader
import com.novenosemestre.ai_lens.RA_Objects2.ml.common.samplerender.VertexBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Draws a label. See [draw].
 */
class LabelRender {
 /**
  * Companion object for the LabelRender class.
  * It contains constants and buffers used for rendering labels.
  */
  companion object {
    // Tag for logging.
    private const val TAG = "LabelRender"

    // Size of the coordinates buffer.
    val COORDS_BUFFER_SIZE = 2 * 4 * 4

    /**
     * Buffer for normalized device coordinates (NDC) of a quad.
     * The quad is defined by four 2D points.
     */
    val NDC_QUAD_COORDS_BUFFER =
      ByteBuffer.allocateDirect(COORDS_BUFFER_SIZE).order(
        ByteOrder.nativeOrder()
      ).asFloatBuffer().apply {
        put(
          floatArrayOf(
            /*0:*/ -1.5f, -1.5f,
            /*1:*/ 1.5f, -1.5f,
            /*2:*/ -1.5f, 1.5f,
            /*3:*/ 1.5f, 1.5f,
          )
        )
      }

    /**
     * Buffer for texture coordinates of a square.
     * The square is defined by four 2D points.
     */
    val SQUARE_TEX_COORDS_BUFFER =
      ByteBuffer.allocateDirect(COORDS_BUFFER_SIZE).order(
        ByteOrder.nativeOrder()
      ).asFloatBuffer().apply {
        put(
          floatArrayOf(
            /*0:*/ 0f, 0f,
            /*1:*/ 1f, 0f,
            /*2:*/ 0f, 1f,
            /*3:*/ 1f, 1f,
          )
        )
      }
  }

  // Cache for text textures.
  val cache = TextTextureCache()

  // Mesh for rendering.
  lateinit var mesh: Mesh

  // Shader for rendering.
  lateinit var shader: Shader

  /**
   * Called when the surface is created.
   * It initializes the shader and the mesh.
   *
   * @param render The SampleRender object for rendering.
   */
  fun onSurfaceCreated(render: SampleRender) {
    shader = Shader.createFromAssets(render, "shaders/label.vert", "shaders/label.frag", null)
      .setBlend(
        Shader.BlendFactor.ONE, // ALPHA (src)
        Shader.BlendFactor.ONE_MINUS_SRC_ALPHA // ALPHA (dest)
      )
      .setDepthTest(false)
      .setDepthWrite(false)

    val vertexBuffers = arrayOf(
      VertexBuffer(render, 2, NDC_QUAD_COORDS_BUFFER),
      VertexBuffer(render, 2, SQUARE_TEX_COORDS_BUFFER),
    )
    mesh = Mesh(render, Mesh.PrimitiveMode.TRIANGLE_STRIP, null, vertexBuffers)
  }

  // Origin of the label.
  val labelOrigin = FloatArray(3)

  /**
   * Draws a label quad with text at a given pose.
   * The label will rotate to face the camera pose around the Y-axis.
   *
   * @param render The SampleRender object for rendering.
   * @param viewProjectionMatrix The view projection matrix.
   * @param pose The pose where the label will be drawn.
   * @param cameraPose The pose of the camera.
   * @param label The text to be drawn.
   */
  fun draw(
    render: SampleRender,
    viewProjectionMatrix: FloatArray,
    pose: Pose,
    cameraPose: Pose,
    label: String
  ) {
    labelOrigin[0] = pose.tx()
    labelOrigin[1] = pose.ty()
    labelOrigin[2] = pose.tz()
    shader
      .setMat4("u_ViewProjection", viewProjectionMatrix)
      .setVec3("u_LabelOrigin", labelOrigin)
      .setVec3("u_CameraPos", cameraPose.translation)
      .setTexture("uTexture", cache.get(render, label))
    render.draw(mesh, shader)
  }
}