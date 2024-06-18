package com.novenosemestre.ai_lens.RA_Objects2.ml.common.samplerender.arcore;

import android.media.Image;
import android.opengl.GLES30;

import com.google.ar.core.Coordinates2d;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.novenosemestre.ai_lens.RA_Objects2.ml.common.samplerender.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class BackgroundRenderer {
  private static final String TAG = BackgroundRenderer.class.getSimpleName();

  // components_per_vertex * number_of_vertices * float_size
  private static final int COORDS_BUFFER_SIZE = 2 * 4 * 4;

  private static final FloatBuffer NDC_QUAD_COORDS_BUFFER =
      ByteBuffer.allocateDirect(COORDS_BUFFER_SIZE).order(ByteOrder.nativeOrder()).asFloatBuffer();

  private static final FloatBuffer VIRTUAL_SCENE_TEX_COORDS_BUFFER =
      ByteBuffer.allocateDirect(COORDS_BUFFER_SIZE).order(ByteOrder.nativeOrder()).asFloatBuffer();

  // Static block to initialize NDC_QUAD_COORDS_BUFFER and VIRTUAL_SCENE_TEX_COORDS_BUFFER
static {
  // NDC_QUAD_COORDS_BUFFER is used to store normalized device coordinates for a quad.
  NDC_QUAD_COORDS_BUFFER.put(
      new float[] {
        /*0:*/ -1f, -1f, /*1:*/ +1f, -1f, /*2:*/ -1f, +1f, /*3:*/ +1f, +1f,
      });
  // VIRTUAL_SCENE_TEX_COORDS_BUFFER is used to store texture coordinates for a quad.
  VIRTUAL_SCENE_TEX_COORDS_BUFFER.put(
      new float[] {
        /*0:*/ 0f, 0f, /*1:*/ 1f, 0f, /*2:*/ 0f, 1f, /*3:*/ 1f, 1f,
      });
}

// Buffer to store camera texture coordinates
private final FloatBuffer cameraTexCoords =
    ByteBuffer.allocateDirect(COORDS_BUFFER_SIZE).order(ByteOrder.nativeOrder()).asFloatBuffer();

// Mesh object to hold vertex data for rendering
private final Mesh mesh;
// VertexBuffer to hold camera texture coordinates
private final VertexBuffer cameraTexCoordsVertexBuffer;
// Shader for rendering the background
private Shader backgroundShader;
// Shader for rendering occlusion
private Shader occlusionShader;
// Texture for camera depth data
private final Texture cameraDepthTexture;
// Texture for camera color data
private final Texture cameraColorTexture;

// Flag to indicate whether depth visualization is used
private boolean useDepthVisualization;
// Flag to indicate whether occlusion is used
private boolean useOcclusion;
// Aspect ratio of the camera
private float aspectRatio;

  /**
   * Constructor for BackgroundRenderer.
   * Initializes textures, vertex buffers and mesh.
   *
   * @param render The SampleRender object used for rendering.
   */
  public BackgroundRenderer(SampleRender render) {
    // Initialize camera color texture
    cameraColorTexture =
        new Texture(
            render,
            Texture.Target.TEXTURE_EXTERNAL_OES,
            Texture.WrapMode.CLAMP_TO_EDGE,
            /*useMipmaps=*/ false);
    // Initialize camera depth texture
    cameraDepthTexture =
        new Texture(
            render,
            Texture.Target.TEXTURE_2D,
            Texture.WrapMode.CLAMP_TO_EDGE,
            /*useMipmaps=*/ false);

    // Create a Mesh with three vertex buffers: one for the screen coordinates (normalized device
    // coordinates), one for the camera texture coordinates (to be populated with proper data later
    // before drawing), and one for the virtual scene texture coordinates (unit texture quad)
    VertexBuffer screenCoordsVertexBuffer =
        new VertexBuffer(render, /* numberOfEntriesPerVertex=*/ 2, NDC_QUAD_COORDS_BUFFER);
    cameraTexCoordsVertexBuffer =
        new VertexBuffer(render, /*numberOfEntriesPerVertex=*/ 2, /*entries=*/ null);
    VertexBuffer virtualSceneTexCoordsVertexBuffer =
        new VertexBuffer(render, /* numberOfEntriesPerVertex=*/ 2, VIRTUAL_SCENE_TEX_COORDS_BUFFER);
    VertexBuffer[] vertexBuffers = {
      screenCoordsVertexBuffer, cameraTexCoordsVertexBuffer, virtualSceneTexCoordsVertexBuffer,
    };
    mesh =
        new Mesh(render, Mesh.PrimitiveMode.TRIANGLE_STRIP, /*indexBuffer=*/ null, vertexBuffers);
  }

  /**
   * Sets whether to use depth visualization.
   * If the flag is changed, the background shader is recreated.
   *
   * @param render The SampleRender object used for rendering.
   * @param useDepthVisualization The flag indicating whether to use depth visualization.
   * @throws IOException If an error occurs while creating the shader.
   */
  public void setUseDepthVisualization(SampleRender render, boolean useDepthVisualization)
      throws IOException {
    if (backgroundShader != null) {
      if (this.useDepthVisualization == useDepthVisualization) {
        return;
      }
      backgroundShader.close();
      backgroundShader = null;
      this.useDepthVisualization = useDepthVisualization;
    }
    if (useDepthVisualization) {
      backgroundShader =
          Shader.createFromAssets(
                  render,
                  "shaders/background_show_depth_color_visualization.vert",  // path assets/shaders
                  "shaders/background_show_depth_color_visualization.frag",
                  /*defines=*/ null)
              .setTexture("u_CameraDepthTexture", cameraDepthTexture)
              .setDepthTest(false)
              .setDepthWrite(false);
    } else {
      backgroundShader =
          Shader.createFromAssets(
                  render,
                  "shaders/background_show_camera.vert",
                  "shaders/background_show_camera.frag",
                  /*defines=*/ null)
              .setTexture("u_CameraColorTexture", cameraColorTexture)
              .setDepthTest(false)
              .setDepthWrite(false);
    }
  }

  /**
   * Updates the display geometry if it has changed.
   * Transforms the coordinates from normalized device coordinates to texture normalized coordinates.
   * Sets the camera texture coordinates vertex buffer with the transformed coordinates.
   *
   * @param frame The AR frame for which to update the display geometry.
   */
  public void updateDisplayGeometry(Frame frame) {
    if (frame.hasDisplayGeometryChanged()) {
      frame.transformCoordinates2d(
          Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES,
          NDC_QUAD_COORDS_BUFFER,
          Coordinates2d.TEXTURE_NORMALIZED,
          cameraTexCoords);
      cameraTexCoordsVertexBuffer.set(cameraTexCoords);
    }
  }

  /**
   * Updates the camera depth texture with the given image.
   * Binds the texture to GL_TEXTURE_2D and sets the texture image to the given image.
   * If occlusion is used, it calculates the aspect ratio of the image and sets it in the occlusion shader.
   *
   * @param image The image to update the camera depth texture with.
   */
  public void updateCameraDepthTexture(Image image) {
    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, cameraDepthTexture.getTextureId());
    GLES30.glTexImage2D(
        GLES30.GL_TEXTURE_2D,
        0,
        GLES30.GL_RG8,
        image.getWidth(),
        image.getHeight(),
        0,
        GLES30.GL_RG,
        GLES30.GL_UNSIGNED_BYTE,
        image.getPlanes()[0].getBuffer());
    if (useOcclusion) {
      aspectRatio = (float) image.getWidth() / (float) image.getHeight();
      occlusionShader.setFloat("u_DepthAspectRatio", aspectRatio);
    }
  }

  /**
   * Draws the background using the background shader.
   *
   * @param render The SampleRender object used for rendering.
   */
  public void drawBackground(SampleRender render) {
    render.draw(mesh, backgroundShader);
  }

  /**
   * Draws the virtual scene using the occlusion shader.
   * Sets the virtual scene color texture and, if occlusion is used, the virtual scene depth texture, zNear and zFar in the occlusion shader.
   *
   * @param render The SampleRender object used for rendering.
   * @param virtualSceneFramebuffer The framebuffer of the virtual scene.
   * @param zNear The near clipping plane distance.
   * @param zFar The far clipping plane distance.
   */
  public void drawVirtualScene(
      SampleRender render, Framebuffer virtualSceneFramebuffer, float zNear, float zFar) {
    occlusionShader.setTexture(
        "u_VirtualSceneColorTexture", virtualSceneFramebuffer.getColorTexture());
    if (useOcclusion) {
      occlusionShader
          .setTexture("u_VirtualSceneDepthTexture", virtualSceneFramebuffer.getDepthTexture())
          .setFloat("u_ZNear", zNear)
          .setFloat("u_ZFar", zFar);
    }
    render.draw(mesh, occlusionShader);
  }

  /**
   * Returns the camera color texture.
   *
   * @return The camera color texture.
   */
  public Texture getCameraColorTexture() {
    return cameraColorTexture;
  }

  /**
   * Returns the camera depth texture.
   *
   * @return The camera depth texture.
   */
  public Texture getCameraDepthTexture() {
    return cameraDepthTexture;
  }
}
