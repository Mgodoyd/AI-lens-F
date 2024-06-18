package com.google.ar.core.examples.java.ml.render

import com.google.ar.core.PointCloud
import com.novenosemestre.ai_lens.RA_Objects2.ml.common.samplerender.Mesh
import com.novenosemestre.ai_lens.RA_Objects2.ml.common.samplerender.SampleRender
import com.novenosemestre.ai_lens.RA_Objects2.ml.common.samplerender.Shader
import com.novenosemestre.ai_lens.RA_Objects2.ml.common.samplerender.VertexBuffer

class PointCloudRender {
  lateinit var pointCloudVertexBuffer: VertexBuffer
  lateinit var pointCloudMesh: Mesh
  lateinit var pointCloudShader: Shader

  var lastPointCloudTimestamp: Long = 0

  /**
   * Initializes the point cloud rendering components.
   *
   * This function is called when the surface is created. It initializes the shader, vertex buffer, and mesh for the point cloud.
   * The shader is created from the vertex and fragment shaders specified in the assets. The color and point size for the point cloud are also set.
   * The vertex buffer is created with four entries per vertex (X, Y, Z, confidence).
   * The mesh is created with the vertex buffer and set to render points.
   *
   * @param render The SampleRender object for rendering.
   */
  fun onSurfaceCreated(render: SampleRender) {
    // Point cloud
    pointCloudShader = Shader.createFromAssets(
      render, "shaders/point_cloud.vert", "shaders/point_cloud.frag",  /*defines=*/null
    )
      .setVec4(
        "u_Color", floatArrayOf(31.0f / 255.0f, 188.0f / 255.0f, 210.0f / 255.0f, 1.0f)
      )
      .setFloat("u_PointSize", 5.0f)

    // four entries per vertex: X, Y, Z, confidence
    pointCloudVertexBuffer = VertexBuffer(render, 4, null)
    val pointCloudVertexBuffers = arrayOf(pointCloudVertexBuffer)
    pointCloudMesh = Mesh(
      render, Mesh.PrimitiveMode.POINTS, null, pointCloudVertexBuffers
    )
  }

  /**
   * Draws the point cloud.
   *
   * This function draws the point cloud if the timestamp of the point cloud is greater than the last timestamp.
   * It sets the points of the vertex buffer to the points of the point cloud and updates the last timestamp.
   * It then sets the model view projection matrix of the shader and draws the mesh.
   *
   * @param render The SampleRender object for rendering.
   * @param pointCloud The PointCloud object to draw.
   * @param modelViewProjectionMatrix The model view projection matrix.
   */
  fun drawPointCloud(
    render: SampleRender,
    pointCloud: PointCloud,
    modelViewProjectionMatrix: FloatArray
  ) {
    if (pointCloud.timestamp > lastPointCloudTimestamp) {
      pointCloudVertexBuffer.set(pointCloud.points)
      lastPointCloudTimestamp = pointCloud.timestamp
    }
    pointCloudShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix)
    render.draw(pointCloudMesh, pointCloudShader)
  }
}