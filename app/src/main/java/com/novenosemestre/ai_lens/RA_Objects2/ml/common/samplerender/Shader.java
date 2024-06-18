package com.novenosemestre.ai_lens.RA_Objects2.ml.common.samplerender;

import static java.nio.charset.StandardCharsets.UTF_8;

import android.content.res.AssetManager;
import android.opengl.GLES30;
import android.opengl.GLException;
import android.util.Log;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;


public class Shader implements Closeable {
  private static final String TAG = Shader.class.getSimpleName();


  public static enum BlendFactor {
    ZERO(GLES30.GL_ZERO),
    ONE(GLES30.GL_ONE),
    SRC_COLOR(GLES30.GL_SRC_COLOR),
    ONE_MINUS_SRC_COLOR(GLES30.GL_ONE_MINUS_SRC_COLOR),
    DST_COLOR(GLES30.GL_DST_COLOR),
    ONE_MINUS_DST_COLOR(GLES30.GL_ONE_MINUS_DST_COLOR),
    SRC_ALPHA(GLES30.GL_SRC_ALPHA),
    ONE_MINUS_SRC_ALPHA(GLES30.GL_ONE_MINUS_SRC_ALPHA),
    DST_ALPHA(GLES30.GL_DST_ALPHA),
    ONE_MINUS_DST_ALPHA(GLES30.GL_ONE_MINUS_DST_ALPHA),
    CONSTANT_COLOR(GLES30.GL_CONSTANT_COLOR),
    ONE_MINUS_CONSTANT_COLOR(GLES30.GL_ONE_MINUS_CONSTANT_COLOR),
    CONSTANT_ALPHA(GLES30.GL_CONSTANT_ALPHA),
    ONE_MINUS_CONSTANT_ALPHA(GLES30.GL_ONE_MINUS_CONSTANT_ALPHA);

    /* package-private */
    final int glesEnum;

    private BlendFactor(int glesEnum) {
      this.glesEnum = glesEnum;
    }
  }

  private int programId = 0;
  private final Map<Integer, Uniform> uniforms = new HashMap<>();
  private int maxTextureUnit = 0;

  private final Map<String, Integer> uniformLocations = new HashMap<>();
  private final Map<Integer, String> uniformNames = new HashMap<>();

  private boolean depthTest = true;
  private boolean depthWrite = true;
  private BlendFactor sourceRgbBlend = BlendFactor.ONE;
  private BlendFactor destRgbBlend = BlendFactor.ZERO;
  private BlendFactor sourceAlphaBlend = BlendFactor.ONE;
  private BlendFactor destAlphaBlend = BlendFactor.ZERO;


   /**
   * Constructor for the Shader class.
   * Initializes the Shader with the given SampleRender instance, vertex shader code, fragment shader code, and map of defines.
   * The map of defines is converted into a string of shader defines code.
   * A vertex shader and a fragment shader are then created with the vertex shader code and fragment shader code, respectively, with the shader defines code inserted.
   * A shader program is created and the vertex shader and fragment shader are attached to it.
   * The shader program is then linked.
   * If the link status is false, the shader program info log is retrieved and a GLException is thrown with the info log.
   * If an error occurs while initializing the Shader, the Shader is closed and the error is rethrown.
   * After the Shader is initialized, the vertex shader and fragment shader are flagged for deletion.
   *
   * @param render The SampleRender instance associated with this Shader.
   * @param vertexShaderCode The vertex shader code for this Shader.
   * @param fragmentShaderCode The fragment shader code for this Shader.
   * @param defines The map of defines for this Shader.
   * @throws GLException If a OpenGL error occurs while initializing the Shader.
   */
  public Shader(
      SampleRender render,
      String vertexShaderCode,
      String fragmentShaderCode,
      Map<String, String> defines) {
    int vertexShaderId = 0;
    int fragmentShaderId = 0;
    String definesCode = createShaderDefinesCode(defines);
    try {
      vertexShaderId =
          createShader(
              GLES30.GL_VERTEX_SHADER, insertShaderDefinesCode(vertexShaderCode, definesCode));
      fragmentShaderId =
          createShader(
              GLES30.GL_FRAGMENT_SHADER, insertShaderDefinesCode(fragmentShaderCode, definesCode));

      programId = GLES30.glCreateProgram();
      GLError.maybeThrowGLException("Shader program creation failed", "glCreateProgram");
      GLES30.glAttachShader(programId, vertexShaderId);
      GLError.maybeThrowGLException("Failed to attach vertex shader", "glAttachShader");
      GLES30.glAttachShader(programId, fragmentShaderId);
      GLError.maybeThrowGLException("Failed to attach fragment shader", "glAttachShader");
      GLES30.glLinkProgram(programId);
      GLError.maybeThrowGLException("Failed to link shader program", "glLinkProgram");

      final int[] linkStatus = new int[1];
      GLES30.glGetProgramiv(programId, GLES30.GL_LINK_STATUS, linkStatus, 0);
      if (linkStatus[0] == GLES30.GL_FALSE) {
        String infoLog = GLES30.glGetProgramInfoLog(programId);
        GLError.maybeLogGLError(
            Log.WARN, TAG, "Failed to retrieve shader program info log", "glGetProgramInfoLog");
        throw new GLException(0, "Shader link failed: " + infoLog);
      }
    } catch (Throwable t) {
      close();
      throw t;
    } finally {
      // Shader objects can be flagged for deletion immediately after program creation.
      if (vertexShaderId != 0) {
        GLES30.glDeleteShader(vertexShaderId);
        GLError.maybeLogGLError(Log.WARN, TAG, "Failed to free vertex shader", "glDeleteShader");
      }
      if (fragmentShaderId != 0) {
        GLES30.glDeleteShader(fragmentShaderId);
        GLError.maybeLogGLError(Log.WARN, TAG, "Failed to free fragment shader", "glDeleteShader");
      }
    }
  }

   /**
   * Creates a Shader from asset files.
   * This method reads the vertex and fragment shader files from the assets, converts them to strings, and creates a new Shader with the SampleRender instance and the shader code.
   * The map of defines is also passed to the Shader constructor.
   *
   * @param render The SampleRender instance associated with this Shader.
   * @param vertexShaderFileName The name of the vertex shader file in the assets.
   * @param fragmentShaderFileName The name of the fragment shader file in the assets.
   * @param defines The map of defines for this Shader.
   * @return The new Shader created from the asset files.
   * @throws IOException If an I/O error occurs while reading the asset files.
   */
  public static Shader createFromAssets(
      SampleRender render,
      String vertexShaderFileName,
      String fragmentShaderFileName,
      Map<String, String> defines)
      throws IOException {
    AssetManager assets = render.getAssets();
    return new Shader(
        render,
        inputStreamToString(assets.open(vertexShaderFileName)),
        inputStreamToString(assets.open(fragmentShaderFileName)),
        defines);
  }

  /**
   * Closes the Shader.
   * If the program ID is not 0, the shader program is deleted.
   */
  @Override
  public void close() {
    if (programId != 0) {
      GLES30.glDeleteProgram(programId);
      programId = 0;
    }
  }

  /**
   * Sets the depth test flag for this Shader.
   * @param depthTest The new depth test flag.
   * @return This Shader, for chaining calls.
   */
  public Shader setDepthTest(boolean depthTest) {
    this.depthTest = depthTest;
    return this;
  }

  /**
   * Sets the depth write flag for this Shader.
   * @param depthWrite The new depth write flag.
   * @return This Shader, for chaining calls.
   */
  public Shader setDepthWrite(boolean depthWrite) {
    this.depthWrite = depthWrite;
    return this;
  }

  /**
   * Sets the blend factors for this Shader.
   * @param sourceBlend The new source blend factor.
   * @param destBlend The new destination blend factor.
   * @return This Shader, for chaining calls.
   */
  public Shader setBlend(BlendFactor sourceBlend, BlendFactor destBlend) {
    this.sourceRgbBlend = sourceBlend;
    this.destRgbBlend = destBlend;
    this.sourceAlphaBlend = sourceBlend;
    this.destAlphaBlend = destBlend;
    return this;
  }

  /**
   * Sets the blend factors for this Shader.
   * @param sourceRgbBlend The new source RGB blend factor.
   * @param destRgbBlend The new destination RGB blend factor.
   * @param sourceAlphaBlend The new source alpha blend factor.
   * @param destAlphaBlend The new destination alpha blend factor.
   * @return This Shader, for chaining calls.
   */
  public Shader setBlend(
      BlendFactor sourceRgbBlend,
      BlendFactor destRgbBlend,
      BlendFactor sourceAlphaBlend,
      BlendFactor destAlphaBlend) {
    this.sourceRgbBlend = sourceRgbBlend;
    this.destRgbBlend = destRgbBlend;
    this.sourceAlphaBlend = sourceAlphaBlend;
    this.destAlphaBlend = destAlphaBlend;
    return this;
  }

  /**
   * Sets a texture uniform for this Shader.
   * If replacing an existing texture uniform, the texture unit is reused.
   * @param name The name of the uniform.
   * @param texture The new Texture for the uniform.
   * @return This Shader, for chaining calls.
   */
  public Shader setTexture(String name, Texture texture) {
    // Special handling for Textures. If replacing an existing texture uniform, reuse the texture
    // unit.
    int location = getUniformLocation(name);
    Uniform uniform = uniforms.get(location);
    int textureUnit;
    if (!(uniform instanceof UniformTexture)) {
      textureUnit = maxTextureUnit++;
    } else {
      UniformTexture uniformTexture = (UniformTexture) uniform;
      textureUnit = uniformTexture.getTextureUnit();
    }
    uniforms.put(location, new UniformTexture(textureUnit, texture));
    return this;
  }

  /**
   * Sets a boolean uniform for this Shader.
   * @param name The name of the uniform.
   * @param v0 The new boolean value for the uniform.
   * @return This Shader, for chaining calls.
   */
  public Shader setBool(String name, boolean v0) {
    int[] values = {v0 ? 1 : 0};
    uniforms.put(getUniformLocation(name), new UniformInt(values));
    return this;
  }

  /**
   * Sets an integer uniform for this Shader.
   * @param name The name of the uniform.
   * @param v0 The new integer value for the uniform.
   * @return This Shader, for chaining calls.
   */
  public Shader setInt(String name, int v0) {
    int[] values = {v0};
    uniforms.put(getUniformLocation(name), new UniformInt(values));
    return this;
  }

  /**
   * Sets a float uniform for this Shader.
   * @param name The name of the uniform.
   * @param v0 The new float value for the uniform.
   * @return This Shader, for chaining calls.
   */
  public Shader setFloat(String name, float v0) {
    float[] values = {v0};
    uniforms.put(getUniformLocation(name), new Uniform1f(values));
    return this;
  }

  /**
   * Sets a 2D vector uniform for this Shader.
   * @param name The name of the uniform.
   * @param values The new 2D vector values for the uniform.
   * @return This Shader, for chaining calls.
   * @throws IllegalArgumentException If the length of the values array is not 2.
   */
  public Shader setVec2(String name, float[] values) {
    if (values.length != 2) {
      throw new IllegalArgumentException("Value array length must be 2");
    }
    uniforms.put(getUniformLocation(name), new Uniform2f(values.clone()));
    return this;
  }
    public Shader setVec3(String name, float[] values) {
      if (values.length != 3) {
        throw new IllegalArgumentException("Value array length must be 3");
      }
      uniforms.put(getUniformLocation(name), new Uniform3f(values.clone()));
      return this;
    }


  /**
   * Sets a 4D vector uniform for this Shader.
   * @param name The name of the uniform.
   * @param values The new 4D vector values for the uniform.
   * @return This Shader, for chaining calls.
   * @throws IllegalArgumentException If the length of the values array is not 4.
   */
  public Shader setVec4(String name, float[] values) {
    if (values.length != 4) {
      throw new IllegalArgumentException("Value array length must be 4");
    }
    uniforms.put(getUniformLocation(name), new Uniform4f(values.clone()));
    return this;
  }

  /**
   * Sets a 2x2 matrix uniform for this Shader.
   * @param name The name of the uniform.
   * @param values The new 2x2 matrix values for the uniform.
   * @return This Shader, for chaining calls.
   * @throws IllegalArgumentException If the length of the values array is not 4 (2x2).
   */
  public Shader setMat2(String name, float[] values) {
    if (values.length != 4) {
      throw new IllegalArgumentException("Value array length must be 4 (2x2)");
    }
    uniforms.put(getUniformLocation(name), new UniformMatrix2f(values.clone()));
    return this;
  }

  /**
   * Sets a 3x3 matrix uniform for this Shader.
   * @param name The name of the uniform.
   * @param values The new 3x3 matrix values for the uniform.
   * @return This Shader, for chaining calls.
   * @throws IllegalArgumentException If the length of the values array is not 9 (3x3).
   */
  public Shader setMat3(String name, float[] values) {
    if (values.length != 9) {
      throw new IllegalArgumentException("Value array length must be 9 (3x3)");
    }
    uniforms.put(getUniformLocation(name), new UniformMatrix3f(values.clone()));
    return this;
  }

  /**
   * Sets a 4x4 matrix uniform for this Shader.
   * @param name The name of the uniform.
   * @param values The new 4x4 matrix values for the uniform.
   * @return This Shader, for chaining calls.
   * @throws IllegalArgumentException If the length of the values array is not 16 (4x4).
   */
  public Shader setMat4(String name, float[] values) {
    if (values.length != 16) {
      throw new IllegalArgumentException("Value array length must be 16 (4x4)");
    }
    uniforms.put(getUniformLocation(name), new UniformMatrix4f(values.clone()));
    return this;
  }

  /**
   * Sets a boolean array uniform for this Shader.
   * @param name The name of the uniform.
   * @param values The new boolean array values for the uniform.
   * @return This Shader, for chaining calls.
   */
  public Shader setBoolArray(String name, boolean[] values) {
    int[] intValues = new int[values.length];
    for (int i = 0; i < values.length; ++i) {
      intValues[i] = values[i] ? 1 : 0;
    }
    uniforms.put(getUniformLocation(name), new UniformInt(intValues));
    return this;
  }

   /**
   * Sets an integer array uniform for this Shader.
   * @param name The name of the uniform.
   * @param values The new integer array values for the uniform.
   * @return This Shader, for chaining calls.
   */
  public Shader setIntArray(String name, int[] values) {
    uniforms.put(getUniformLocation(name), new UniformInt(values.clone()));
    return this;
  }

  /**
   * Sets a float array uniform for this Shader.
   * @param name The name of the uniform.
   * @param values The new float array values for the uniform.
   * @return This Shader, for chaining calls.
   */
  public Shader setFloatArray(String name, float[] values) {
    uniforms.put(getUniformLocation(name), new Uniform1f(values.clone()));
    return this;
  }

  /**
   * Sets a 2D vector array uniform for this Shader.
   * @param name The name of the uniform.
   * @param values The new 2D vector array values for the uniform.
   * @return This Shader, for chaining calls.
   * @throws IllegalArgumentException If the length of the values array is not divisible by 2.
   */
  public Shader setVec2Array(String name, float[] values) {
    if (values.length % 2 != 0) {
      throw new IllegalArgumentException("Value array length must be divisible by 2");
    }
    uniforms.put(getUniformLocation(name), new Uniform2f(values.clone()));
    return this;
  }

  /**
   * Sets a 3D vector array uniform for this Shader.
   * @param name The name of the uniform.
   * @param values The new 3D vector array values for the uniform.
   * @return This Shader, for chaining calls.
   * @throws IllegalArgumentException If the length of the values array is not divisible by 3.
   */
  public Shader setVec3Array(String name, float[] values) {
    if (values.length % 3 != 0) {
      throw new IllegalArgumentException("Value array length must be divisible by 3");
    }
    uniforms.put(getUniformLocation(name), new Uniform3f(values.clone()));
    return this;
  }

  /**
   * Sets a 4D vector array uniform for this Shader.
   * @param name The name of the uniform.
   * @param values The new 4D vector array values for the uniform.
   * @return This Shader, for chaining calls.
   * @throws IllegalArgumentException If the length of the values array is not divisible by 4.
   */
  public Shader setVec4Array(String name, float[] values) {
    if (values.length % 4 != 0) {
      throw new IllegalArgumentException("Value array length must be divisible by 4");
    }
    uniforms.put(getUniformLocation(name), new Uniform4f(values.clone()));
    return this;
  }

  /**
   * Sets a 2x2 matrix array uniform for this Shader.
   * @param name The name of the uniform.
   * @param values The new 2x2 matrix array values for the uniform.
   * @return This Shader, for chaining calls.
   * @throws IllegalArgumentException If the length of the values array is not divisible by 4 (2x2).
   */
  public Shader setMat2Array(String name, float[] values) {
    if (values.length % 4 != 0) {
      throw new IllegalArgumentException("Value array length must be divisible by 4 (2x2)");
    }
    uniforms.put(getUniformLocation(name), new UniformMatrix2f(values.clone()));
    return this;
  }

  /**
   * Sets a 3x3 matrix array uniform for this Shader.
   * @param name The name of the uniform.
   * @param values The new 3x3 matrix array values for the uniform.
   * @return This Shader, for chaining calls.
   * @throws IllegalArgumentException If the length of the values array is not divisible by 9 (3x3).
   */
  public Shader setMat3Array(String name, float[] values) {
    if (values.length % 9 != 0) {
      throw new IllegalArgumentException("Values array length must be divisible by 9 (3x3)");
    }
    uniforms.put(getUniformLocation(name), new UniformMatrix3f(values.clone()));
    return this;
  }

  /**
   * Sets a 4x4 matrix array uniform for this Shader.
   * @param name The name of the uniform.
   * @param values The new 4x4 matrix array values for the uniform.
   * @return This Shader, for chaining calls.
   * @throws IllegalArgumentException If the length of the values array is not divisible by 16 (4x4).
   */
  public Shader setMat4Array(String name, float[] values) {
    if (values.length % 16 != 0) {
      throw new IllegalArgumentException("Value array length must be divisible by 16 (4x4)");
    }
    uniforms.put(getUniformLocation(name), new UniformMatrix4f(values.clone()));
    return this;
  }

  /**
   * Applies the shader program and sets the uniforms.
   * If the program ID is 0, an IllegalStateException is thrown.
   * The shader program is used, the blend mode is set, the depth write mask is set, and the depth test is enabled or disabled.
   * All non-texture uniforms are removed from the map after setting them, since they're stored as part of the program.
   * If a GLException occurs while setting a uniform, an IllegalArgumentException is thrown with the name of the uniform and the GLException.
   * After all uniforms are set, the active texture is set to TEXTURE0.
   * If a GLException occurs while setting the active texture, a warning is logged.
   */
  public void lowLevelUse() {
    // Make active shader/set uniforms
    if (programId == 0) {
      throw new IllegalStateException("Attempted to use freed shader");
    }
    GLES30.glUseProgram(programId);
    GLError.maybeThrowGLException("Failed to use shader program", "glUseProgram");
    GLES30.glBlendFuncSeparate(
        sourceRgbBlend.glesEnum,
        destRgbBlend.glesEnum,
        sourceAlphaBlend.glesEnum,
        destAlphaBlend.glesEnum);
    GLError.maybeThrowGLException("Failed to set blend mode", "glBlendFuncSeparate");
    GLES30.glDepthMask(depthWrite);
    GLError.maybeThrowGLException("Failed to set depth write mask", "glDepthMask");
    if (depthTest) {
      GLES30.glEnable(GLES30.GL_DEPTH_TEST);
      GLError.maybeThrowGLException("Failed to enable depth test", "glEnable");
    } else {
      GLES30.glDisable(GLES30.GL_DEPTH_TEST);
      GLError.maybeThrowGLException("Failed to disable depth test", "glDisable");
    }
    try {
      // Remove all non-texture uniforms from the map after setting them, since they're stored as
      // part of the program.
      ArrayList<Integer> obsoleteEntries = new ArrayList<>(uniforms.size());
      for (Map.Entry<Integer, Uniform> entry : uniforms.entrySet()) {
        try {
          entry.getValue().use(entry.getKey());
          if (!(entry.getValue() instanceof UniformTexture)) {
            obsoleteEntries.add(entry.getKey());
          }
        } catch (GLException e) {
          String name = uniformNames.get(entry.getKey());
          throw new IllegalArgumentException("Error setting uniform `" + name + "'", e);
        }
      }
      uniforms.keySet().removeAll(obsoleteEntries);
    } finally {
      GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
      GLError.maybeLogGLError(Log.WARN, TAG, "Failed to set active texture", "glActiveTexture");
    }
  }
   /**
   * Interface representing a uniform to be used in a shader.
   */
  private static interface Uniform {
    /**
     * Method to use the uniform at a given location.
     * @param location The location to use the uniform at.
     */
    public void use(int location);
  }

  /**
   * Class representing a texture uniform.
   */
  private static class UniformTexture implements Uniform {
    private final int textureUnit;
    private final Texture texture;

    /**
     * Constructor for the UniformTexture class.
     * @param textureUnit The texture unit for this UniformTexture.
     * @param texture The Texture for this UniformTexture.
     */
    public UniformTexture(int textureUnit, Texture texture) {
      this.textureUnit = textureUnit;
      this.texture = texture;
    }

    /**
     * Getter for the texture unit.
     * @return The texture unit.
     */
    public int getTextureUnit() {
      return textureUnit;
    }

    /**
     * Method to use the texture uniform at a given location.
     * @param location The location to use the texture uniform at.
     */
    @Override
    public void use(int location) {
      if (texture.getTextureId() == 0) {
        throw new IllegalStateException("Tried to draw with freed texture");
      }
      GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + textureUnit);
      GLError.maybeThrowGLException("Failed to set active texture", "glActiveTexture");
      GLES30.glBindTexture(texture.getTarget().glesEnum, texture.getTextureId());
      GLError.maybeThrowGLException("Failed to bind texture", "glBindTexture");
      GLES30.glUniform1i(location, textureUnit);
      GLError.maybeThrowGLException("Failed to set shader texture uniform", "glUniform1i");
    }
  }

  /**
   * Class representing an integer uniform.
   */
  private static class UniformInt implements Uniform {
    private final int[] values;

    /**
     * Constructor for the UniformInt class.
     * @param values The integer values for this UniformInt.
     */
    public UniformInt(int[] values) {
      this.values = values;
    }

    /**
     * Method to use the integer uniform at a given location.
     * @param location The location to use the integer uniform at.
     */
    @Override
    public void use(int location) {
      GLES30.glUniform1iv(location, values.length, values, 0);
      GLError.maybeThrowGLException("Failed to set shader uniform 1i", "glUniform1iv");
    }
  }

  /**
   * Class representing a float uniform.
   */
  private static class Uniform1f implements Uniform {
    private final float[] values;

    /**
     * Constructor for the Uniform1f class.
     * @param values The float values for this Uniform1f.
     */
    public Uniform1f(float[] values) {
      this.values = values;
    }

    /**
     * Method to use the float uniform at a given location.
     * @param location The location to use the float uniform at.
     */
    @Override
    public void use(int location) {
      GLES30.glUniform1fv(location, values.length, values, 0);
      GLError.maybeThrowGLException("Failed to set shader uniform 1f", "glUniform1fv");
    }
  }

  /**
   * Class representing a 2D vector uniform.
   */
  private static class Uniform2f implements Uniform {
    private final float[] values;

    /**
     * Constructor for the Uniform2f class.
     * @param values The 2D vector values for this Uniform2f.
     */
    public Uniform2f(float[] values) {
      this.values = values;
    }

    /**
     * Method to use the 2D vector uniform at a given location.
     * @param location The location to use the 2D vector uniform at.
     */
    @Override
    public void use(int location) {
      GLES30.glUniform2fv(location, values.length / 2, values, 0);
      GLError.maybeThrowGLException("Failed to set shader uniform 2f", "glUniform2fv");
    }
  }

  /**
   * Class representing a 3D vector uniform.
   */
  private static class Uniform3f implements Uniform {
    private final float[] values;

    /**
     * Constructor for the Uniform3f class.
     * @param values The 3D vector values for this Uniform3f.
     */
    public Uniform3f(float[] values) {
      this.values = values;
    }

    /**
     * Method to use the 3D vector uniform at a given location.
     * @param location The location to use the 3D vector uniform at.
     */
    @Override
    public void use(int location) {
      GLES30.glUniform3fv(location, values.length / 3, values, 0);
      GLError.maybeThrowGLException("Failed to set shader uniform 3f", "glUniform3fv");
    }
  }

  /**
   * Class representing a 4D vector uniform.
   */
  private static class Uniform4f implements Uniform {
    private final float[] values;

    /**
     * Constructor for the Uniform4f class.
     * @param values The 4D vector values for this Uniform4f.
     */
    public Uniform4f(float[] values) {
      this.values = values;
    }

    /**
     * Method to use the 4D vector uniform at a given location.
     * @param location The location to use the 4D vector uniform at.
     */
    @Override
    public void use(int location) {
      GLES30.glUniform4fv(location, values.length / 4, values, 0);
      GLError.maybeThrowGLException("Failed to set shader uniform 4f", "glUniform4fv");
    }
  }

  /**
   * Class representing a 2x2 matrix uniform.
   */
  private static class UniformMatrix2f implements Uniform {
    private final float[] values;

    /**
     * Constructor for the UniformMatrix2f class.
     * @param values The 2x2 matrix values for this UniformMatrix2f.
     */
    public UniformMatrix2f(float[] values) {
      this.values = values;
    }

    /**
     * Method to use the 2x2 matrix uniform at a given location.
     * @param location The location to use the 2x2 matrix uniform at.
     */
    @Override
    public void use(int location) {
      GLES30.glUniformMatrix2fv(location, values.length / 4, /*transpose=*/ false, values, 0);
      GLError.maybeThrowGLException("Failed to set shader uniform matrix 2f", "glUniformMatrix2fv");
    }
  }

  /**
   * Class representing a 3x3 matrix uniform.
   */
  private static class UniformMatrix3f implements Uniform {
    private final float[] values;

    /**
     * Constructor for the UniformMatrix3f class.
     * @param values The 3x3 matrix values for this UniformMatrix3f.
     */
    public UniformMatrix3f(float[] values) {
      this.values = values;
    }

    /**
     * Method to use the 3x3 matrix uniform at a given location.
     * @param location The location to use the 3x3 matrix uniform at.
     */
    @Override
    public void use(int location) {
      GLES30.glUniformMatrix3fv(location, values.length / 9, /*transpose=*/ false, values, 0);
      GLError.maybeThrowGLException("Failed to set shader uniform matrix 3f", "glUniformMatrix3fv");
    }
  }

  /**
   * Class representing a 4x4 matrix uniform.
   */
  private static class UniformMatrix4f implements Uniform {
    private final float[] values;

    /**
     * Constructor for the UniformMatrix4f class.
     * @param values The 4x4 matrix values for this UniformMatrix4f.
     */
    public UniformMatrix4f(float[] values) {
      this.values = values;
    }

    /**
     * Method to use the 4x4 matrix uniform at a given location.
     * @param location The location to use the 4x4 matrix uniform at.
     */
    @Override
    public void use(int location) {
      GLES30.glUniformMatrix4fv(location, values.length / 16, /*transpose=*/ false, values, 0);
      GLError.maybeThrowGLException("Failed to set shader uniform matrix 4f", "glUniformMatrix4fv");
    }
  }

  /**
   * Retrieves the location of a uniform variable within a shader.
   * @param name The name of the uniform variable whose location is to be queried.
   * @return The location of the uniform variable.
   */
  private int getUniformLocation(String name) {
    Integer locationObject = uniformLocations.get(name);
    if (locationObject != null) {
      return locationObject;
    }
    int location = GLES30.glGetUniformLocation(programId, name);
    GLError.maybeThrowGLException("Failed to find uniform", "glGetUniformLocation");
    if (location == -1) {
      throw new IllegalArgumentException("Shader uniform does not exist: " + name);
    }
    uniformLocations.put(name, Integer.valueOf(location));
    uniformNames.put(Integer.valueOf(location), name);
    return location;
  }

  /**
   * Creates a shader object of the specified type and compiles the source code strings that are loaded into it.
   * @param type The type of shader to be created.
   * @param code The source code of the shader.
   * @return The shader object.
   */
  private static int createShader(int type, String code) {
    int shaderId = GLES30.glCreateShader(type);
    GLError.maybeThrowGLException("Shader creation failed", "glCreateShader");
    GLES30.glShaderSource(shaderId, code);
    GLError.maybeThrowGLException("Shader source failed", "glShaderSource");
    GLES30.glCompileShader(shaderId);
    GLError.maybeThrowGLException("Shader compilation failed", "glCompileShader");

    final int[] compileStatus = new int[1];
    GLES30.glGetShaderiv(shaderId, GLES30.GL_COMPILE_STATUS, compileStatus, 0);
    if (compileStatus[0] == GLES30.GL_FALSE) {
      String infoLog = GLES30.glGetShaderInfoLog(shaderId);
      GLError.maybeLogGLError(
          Log.WARN, TAG, "Failed to retrieve shader info log", "glGetShaderInfoLog");
      GLES30.glDeleteShader(shaderId);
      GLError.maybeLogGLError(Log.WARN, TAG, "Failed to free shader", "glDeleteShader");
      throw new GLException(0, "Shader compilation failed: " + infoLog);
    }

    return shaderId;
  }

  /**
   * Creates a string of shader defines code from a map of defines.
   * @param defines The map of defines.
   * @return The string of shader defines code.
   */
  private static String createShaderDefinesCode(Map<String, String> defines) {
    if (defines == null) {
      return "";
    }
    StringBuilder builder = new StringBuilder();
    for (Map.Entry<String, String> entry : defines.entrySet()) {
      builder.append("#define " + entry.getKey() + " " + entry.getValue() + "\n");
    }
    return builder.toString();
  }

  /**
   * Inserts shader defines code into the source code of a shader.
   * @param sourceCode The source code of the shader.
   * @param definesCode The shader defines code to be inserted.
   * @return The source code of the shader with the shader defines code inserted.
   */
  private static String insertShaderDefinesCode(String sourceCode, String definesCode) {
    String result =
        sourceCode.replaceAll(
            "(?m)^(\\s*#\\s*version\\s+.*)$", "$1\n" + Matcher.quoteReplacement(definesCode));
    if (result.equals(sourceCode)) {
      // No #version specified, so just prepend source
      return definesCode + sourceCode;
    }
    return result;
  }

  /**
   * Converts an InputStream into a String.
   * @param stream The InputStream to be converted.
   * @return The String representation of the InputStream.
   * @throws IOException If an I/O error occurs.
   */
  private static String inputStreamToString(InputStream stream) throws IOException {
    InputStreamReader reader = new InputStreamReader(stream, UTF_8.name());
    char[] buffer = new char[1024 * 4];
    StringBuilder builder = new StringBuilder();
    int amount = 0;
    while ((amount = reader.read(buffer)) != -1) {
      builder.append(buffer, 0, amount);
    }
    reader.close();
    return builder.toString();
  }
}
