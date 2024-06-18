package com.novenosemestre.ai_lens.RA_Objects2.ml.common.samplerender;

import android.content.res.AssetManager;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class SampleRender {
  private static final String TAG = SampleRender.class.getSimpleName();

  private final AssetManager assetManager;

  private int viewportWidth = 1;
  private int viewportHeight = 1;

   /**
   * Constructor for the SampleRender class.
   * Initializes the SampleRender with the given GLSurfaceView, Renderer, and AssetManager.
   * Sets the EGL context client version to 3 and the EGL config chooser to 8, 8, 8, 8, 16, 0.
   * Sets the renderer to a new GLSurfaceView.Renderer, which enables blending when the surface is created,
   * updates the viewport dimensions when the surface is changed, and clears the framebuffer and draws the frame when a frame is drawn.
   * Sets the render mode to RENDERMODE_CONTINUOUSLY and sets the GLSurfaceView to draw.
   *
   * @param glSurfaceView The GLSurfaceView associated with this SampleRender.
   * @param renderer The Renderer associated with this SampleRender.
   * @param assetManager The AssetManager associated with this SampleRender.
   */
  public SampleRender(GLSurfaceView glSurfaceView, Renderer renderer, AssetManager assetManager) {
    this.assetManager = assetManager;
    glSurfaceView.setPreserveEGLContextOnPause(true);
    glSurfaceView.setEGLContextClientVersion(3);
    glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
    glSurfaceView.setRenderer(
        new GLSurfaceView.Renderer() {
          @Override
          public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            GLES30.glEnable(GLES30.GL_BLEND);
            GLError.maybeThrowGLException("Failed to enable blending", "glEnable");
            renderer.onSurfaceCreated(SampleRender.this);
          }

          @Override
          public void onSurfaceChanged(GL10 gl, int w, int h) {
            viewportWidth = w;
            viewportHeight = h;
            renderer.onSurfaceChanged(SampleRender.this, w, h);
          }

          @Override
          public void onDrawFrame(GL10 gl) {
            clear(/*framebuffer=*/ null, 0f, 0f, 0f, 1f);
            renderer.onDrawFrame(SampleRender.this);
          }
        });
    glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    glSurfaceView.setWillNotDraw(false);
  }

  /**
   * Draws the given Mesh with the given Shader.
   * This method is a convenience method that calls the draw method with the framebuffer set to null.
   *
   * @param mesh The Mesh to draw.
   * @param shader The Shader to draw the Mesh with.
   */
  public void draw(Mesh mesh, Shader shader) {
    draw(mesh, shader, /*framebuffer=*/ null);
  }

  /**
   * Draws the given Mesh with the given Shader and Framebuffer.
   * The Framebuffer is used, the Shader is used at a low level, and the Mesh is drawn at a low level.
   *
   * @param mesh The Mesh to draw.
   * @param shader The Shader to draw the Mesh with.
   * @param framebuffer The Framebuffer to use when drawing the Mesh.
   */
  public void draw(Mesh mesh, Shader shader, Framebuffer framebuffer) {
    useFramebuffer(framebuffer);
    shader.lowLevelUse();
    mesh.lowLevelDraw();
  }

   /**
   * Clears the framebuffer with the given color.
   * If the framebuffer is null, the default framebuffer is used.
   * The framebuffer is cleared with the given color and the depth buffer is also cleared.
   *
   * @param framebuffer The Framebuffer to clear. If null, the default framebuffer is used.
   * @param r The red component of the clear color.
   * @param g The green component of the clear color.
   * @param b The blue component of the clear color.
   * @param a The alpha component of the clear color.
   * @throws "GLException" If an OpenGL error occurs while clearing the framebuffer.
   */
  public void clear(Framebuffer framebuffer, float r, float g, float b, float a) {
    useFramebuffer(framebuffer);
    GLES30.glClearColor(r, g, b, a);
    GLError.maybeThrowGLException("Failed to set clear color", "glClearColor");
    GLES30.glDepthMask(true);
    GLError.maybeThrowGLException("Failed to set depth write mask", "glDepthMask");
    GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
    GLError.maybeThrowGLException("Failed to clear framebuffer", "glClear");
  }

  /**
   * Interface for a Renderer.
   * A Renderer is responsible for creating a surface, changing the surface, and drawing a frame.
   */
  public static interface Renderer {

    /**
     * Called when the surface is created.
     * @param render The SampleRender instance associated with this Renderer.
     */
    public void onSurfaceCreated(SampleRender render);

    /**
     * Called when the surface changes.
     * @param render The SampleRender instance associated with this Renderer.
     * @param width The new width of the surface.
     * @param height The new height of the surface.
     */
    public void onSurfaceChanged(SampleRender render, int width, int height);

    /**
     * Called when a frame is drawn.
     * @param render The SampleRender instance associated with this Renderer.
     */
    public void onDrawFrame(SampleRender render);
  }

  /**
   * Returns the AssetManager associated with this SampleRender.
   * This method is package-private, meaning it can only be accessed within the same package.
   *
   * @return The AssetManager associated with this SampleRender.
   */
  /* package-private */
  AssetManager getAssets() {
    return assetManager;
  }

  /**
   * Uses the given Framebuffer.
   * If the framebuffer is null, the default framebuffer is used.
   * The viewport dimensions are set to the dimensions of the framebuffer.
   * If the framebuffer is null, the viewport dimensions are set to the dimensions of the viewport.
   *
   * @param framebuffer The Framebuffer to use. If null, the default framebuffer is used.
   * @throws "GLException" If an OpenGL error occurs while using the framebuffer.
   */
  private void useFramebuffer(Framebuffer framebuffer) {
    int framebufferId;
    int viewportWidth;
    int viewportHeight;
    if (framebuffer == null) {
      framebufferId = 0;
      viewportWidth = this.viewportWidth;
      viewportHeight = this.viewportHeight;
    } else {
      framebufferId = framebuffer.getFramebufferId();
      viewportWidth = framebuffer.getWidth();
      viewportHeight = framebuffer.getHeight();
    }
    GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, framebufferId);
    GLError.maybeThrowGLException("Failed to bind framebuffer", "glBindFramebuffer");
    GLES30.glViewport(0, 0, viewportWidth, viewportHeight);
    GLError.maybeThrowGLException("Failed to set viewport dimensions", "glViewport");
  }
}
