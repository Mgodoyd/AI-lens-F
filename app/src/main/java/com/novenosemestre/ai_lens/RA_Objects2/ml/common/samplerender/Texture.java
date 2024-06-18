package com.novenosemestre.ai_lens.RA_Objects2.ml.common.samplerender;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES11Ext;
import android.opengl.GLES30;
import android.util.Log;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

public class Texture implements Closeable {
  private static final String TAG = Texture.class.getSimpleName();

  private final int[] textureId = {0};
  private final Target target;

  /**
   * Enum representing the wrap mode of a texture.
   * The wrap mode determines how the texture is sampled when a coordinate outside the range of 0 to 1 is used.
   */
  public enum WrapMode {

    /**
     * Represents the GL_CLAMP_TO_EDGE wrap mode.
     * In this mode, the texture coordinates are clamped to the range [1/(2N), 1 - 1/(2N)], where N is the size of the texture in the direction of clamping.
     */
    CLAMP_TO_EDGE(GLES30.GL_CLAMP_TO_EDGE),

    /**
     * Represents the GL_MIRRORED_REPEAT wrap mode.
     * In this mode, the texture coordinates are set to the fractional part of the texture coordinate if the integer part of s is even; if the integer part of s is odd, the texture coordinate is set to m - frac(s), where m is the maximum texture coordinate value.
     */
    MIRRORED_REPEAT(GLES30.GL_MIRRORED_REPEAT),

    /**
     * Represents the GL_REPEAT wrap mode.
     * In this mode, the texture coordinates are set to the fractional part of the texture coordinate, creating a repeating pattern.
     */
    REPEAT(GLES30.GL_REPEAT);

    /* package-private */
    final int glesEnum;

    private WrapMode(int glesEnum) {
      this.glesEnum = glesEnum;
    }
  }


  /**
   * Enum representing the target of a texture.
   * The target determines the type of texture.
   */
  public enum Target {
    /**
     * Represents the GL_TEXTURE_2D target.
     * This target is used for 2D textures.
     */
    TEXTURE_2D(GLES30.GL_TEXTURE_2D),


    /**
     * Represents the GL_TEXTURE_EXTERNAL_OES target.
     * This target is used for external textures.
     */
    TEXTURE_EXTERNAL_OES(GLES11Ext.GL_TEXTURE_EXTERNAL_OES),


    /**
     * Represents the GL_TEXTURE_CUBE_MAP target.
     * This target is used for cube map textures.
     */
    TEXTURE_CUBE_MAP(GLES30.GL_TEXTURE_CUBE_MAP);

    final int glesEnum;

    private Target(int glesEnum) {
      this.glesEnum = glesEnum;
    }
  }

  /**
   * Enum representing the color format of a texture.
   * The color format determines how the color data of the texture is stored.
   */
  public enum ColorFormat {
    /**
     * Represents the GL_RGBA8 color format.
     * This format stores the red, green, blue, and alpha components as 8-bit unsigned integers.
     */
    LINEAR(GLES30.GL_RGBA8),

    /**
     * Represents the GL_SRGB8_ALPHA8 color format.
     * This format stores the red, green, blue components in sRGB format and the alpha component as an 8-bit unsigned integer.
     */
    SRGB(GLES30.GL_SRGB8_ALPHA8);

    final int glesEnum;

    private ColorFormat(int glesEnum) {
      this.glesEnum = glesEnum;
    }
  }


  /**
   * Constructor for the Texture class.
   * Initializes the Texture with the given SampleRender instance, target, wrap mode, and useMipmaps flag.
   * The texture ID is generated and the texture parameters are set.
   * If an error occurs while initializing the Texture, the Texture is closed and the error is rethrown.
   *
   * @param render The SampleRender instance associated with this Texture.
   * @param target The target for this Texture.
   * @param wrapMode The wrap mode for this Texture.
   * @param "useMipmaps" The flag indicating whether to use mipmaps for this Texture.
   */
  public Texture(SampleRender render, Target target, WrapMode wrapMode) {
    this(render, target, wrapMode, /*useMipmaps=*/ true);
  }

  public Texture(SampleRender render, Target target, WrapMode wrapMode, boolean useMipmaps) {
    this.target = target;

    GLES30.glGenTextures(1, textureId, 0);
    GLError.maybeThrowGLException("Texture creation failed", "glGenTextures");

    int minFilter = useMipmaps ? GLES30.GL_LINEAR_MIPMAP_LINEAR : GLES30.GL_LINEAR;

    try {
      GLES30.glBindTexture(target.glesEnum, textureId[0]);
      GLError.maybeThrowGLException("Failed to bind texture", "glBindTexture");
      GLES30.glTexParameteri(target.glesEnum, GLES30.GL_TEXTURE_MIN_FILTER, minFilter);
      GLError.maybeThrowGLException("Failed to set texture parameter", "glTexParameteri");
      GLES30.glTexParameteri(target.glesEnum, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
      GLError.maybeThrowGLException("Failed to set texture parameter", "glTexParameteri");

      GLES30.glTexParameteri(target.glesEnum, GLES30.GL_TEXTURE_WRAP_S, wrapMode.glesEnum);
      GLError.maybeThrowGLException("Failed to set texture parameter", "glTexParameteri");
      GLES30.glTexParameteri(target.glesEnum, GLES30.GL_TEXTURE_WRAP_T, wrapMode.glesEnum);
      GLError.maybeThrowGLException("Failed to set texture parameter", "glTexParameteri");
    } catch (Throwable t) {
      close();
      throw t;
    }
  }

  /**
   * Creates a Texture from an asset file.
   * This method loads a bitmap from the asset file, converts it to the specified color format, and creates a new Texture with the bitmap data.
   * The texture is created with the specified wrap mode and the target is set to TEXTURE_2D.
   * If an error occurs while creating the texture, the texture is closed and the error is rethrown.
   * If the bitmap is not null after the texture is created, the bitmap is recycled.
   *
   * @param render The SampleRender instance associated with this Texture.
   * @param assetFileName The name of the asset file to load the bitmap from.
   * @param wrapMode The wrap mode for this Texture.
   * @param colorFormat The color format for this Texture.
   * @return The new Texture created from the asset file.
   * @throws IOException If an I/O error occurs while loading the bitmap from the asset file.
   */
  public static Texture createFromAsset(
      SampleRender render, String assetFileName, WrapMode wrapMode, ColorFormat colorFormat)
      throws IOException {
    Texture texture = new Texture(render, Target.TEXTURE_2D, wrapMode);
    Bitmap bitmap = null;
    try {
      // The following lines up to glTexImage2D could technically be replaced with
      // GLUtils.texImage2d, but this method does not allow for loading sRGB images.

      // Load and convert the bitmap and copy its contents to a direct ByteBuffer. Despite its name,
      // the ARGB_8888 config is actually stored in RGBA order.
      bitmap =
          convertBitmapToConfig(
              BitmapFactory.decodeStream(render.getAssets().open(assetFileName)),
              Bitmap.Config.ARGB_8888);
      ByteBuffer buffer = ByteBuffer.allocateDirect(bitmap.getByteCount());
      bitmap.copyPixelsToBuffer(buffer);
      buffer.rewind();

      GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture.getTextureId());
      GLError.maybeThrowGLException("Failed to bind texture", "glBindTexture");
      GLES30.glTexImage2D(
          GLES30.GL_TEXTURE_2D,
          /*level=*/ 0,
          colorFormat.glesEnum,
          bitmap.getWidth(),
          bitmap.getHeight(),
          /*border=*/ 0,
          GLES30.GL_RGBA,
          GLES30.GL_UNSIGNED_BYTE,
          buffer);
      GLError.maybeThrowGLException("Failed to populate texture data", "glTexImage2D");
      GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D);
      GLError.maybeThrowGLException("Failed to generate mipmaps", "glGenerateMipmap");
    } catch (Throwable t) {
      texture.close();
      throw t;
    } finally {
      if (bitmap != null) {
        bitmap.recycle();
      }
    }
    return texture;
  }

  /**
   * Closes this Texture.
   * If the texture ID is not 0, the texture is deleted and the texture ID is set to 0.
   */
  @Override
  public void close() {
    if (textureId[0] != 0) {
      GLES30.glDeleteTextures(1, textureId, 0);
      GLError.maybeLogGLError(Log.WARN, TAG, "Failed to free texture", "glDeleteTextures");
      textureId[0] = 0;
    }
  }

  /**
   * Returns the texture ID of this Texture.
   *
   * @return The texture ID of this Texture.
   */
  public int getTextureId() {
    return textureId[0];
  }

  /* package-private */
  Target getTarget() {
    return target;
  }


  /**
   * Returns the target of this Texture.
   * This method is package-private, meaning it can only be accessed within the same package.
   *
   * @return The target of this Texture.
   */
  private static Bitmap convertBitmapToConfig(Bitmap bitmap, Bitmap.Config config) {
    // We use this method instead of BitmapFactory.Options.outConfig to support a minimum of Android
    // API level 24.
    if (bitmap.getConfig() == config) {
      return bitmap;
    }
    Bitmap result = bitmap.copy(config, /*isMutable=*/ false);
    bitmap.recycle();
    return result;
  }
}
