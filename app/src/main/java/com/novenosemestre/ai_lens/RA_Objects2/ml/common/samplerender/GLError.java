package com.novenosemestre.ai_lens.RA_Objects2.ml.common.samplerender;

import android.opengl.GLES30;
import android.opengl.GLException;
import android.opengl.GLU;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GLError {
   /**
   * Checks for OpenGL errors and throws a GLException if any are found.
   * The exception message includes the reason, the API call, and the error codes.
   *
   * @param reason The reason for the potential GLException.
   * @param api The API call that may have caused the GLException.
   * @throws GLException If any OpenGL errors are found.
   */
  public static void maybeThrowGLException(String reason, String api) {
    List<Integer> errorCodes = getGlErrors();
    if (errorCodes != null) {
      throw new GLException(errorCodes.get(0), formatErrorMessage(reason, api, errorCodes));
    }
  }

  /**
   * Checks for OpenGL errors and logs them if any are found.
   * The log message includes the reason, the API call, and the error codes.
   *
   * @param priority The priority level of the log message.
   * @param tag The tag of the log message.
   * @param reason The reason for the potential OpenGL errors.
   * @param api The API call that may have caused the OpenGL errors.
   */
  public static void maybeLogGLError(int priority, String tag, String reason, String api) {
    List<Integer> errorCodes = getGlErrors();
    if (errorCodes != null) {
      Log.println(priority, tag, formatErrorMessage(reason, api, errorCodes));
    }
  }

  /**
   * Formats an error message for OpenGL errors.
   * The message includes the reason, the API call, and the error codes.
   *
   * @param reason The reason for the OpenGL errors.
   * @param api The API call that caused the OpenGL errors.
   * @param errorCodes The OpenGL error codes.
   * @return The formatted error message.
   */
  private static String formatErrorMessage(String reason, String api, List<Integer> errorCodes) {
    StringBuilder builder = new StringBuilder(String.format("%s: %s: ", reason, api));
    Iterator<Integer> iterator = errorCodes.iterator();
    while (iterator.hasNext()) {
      int errorCode = iterator.next();
      builder.append(String.format("%s (%d)", GLU.gluErrorString(errorCode), errorCode));
      if (iterator.hasNext()) {
        builder.append(", ");
      }
    }
    return builder.toString();
  }

  /**
   * Checks for OpenGL errors and returns a list of error codes if any are found.
   * If no errors are found, it returns null.
   *
   * @return A list of OpenGL error codes, or null if no errors are found.
   */
  private static List<Integer> getGlErrors() {
    int errorCode = GLES30.glGetError();
    // Shortcut for no errors
    if (errorCode == GLES30.GL_NO_ERROR) {
      return null;
    }
    List<Integer> errorCodes = new ArrayList<>();
    errorCodes.add(errorCode);
    while (true) {
      errorCode = GLES30.glGetError();
      if (errorCode == GLES30.GL_NO_ERROR) {
        break;
      }
      errorCodes.add(errorCode);
    }
    return errorCodes;
  }

  /**
   * Private constructor to prevent instantiation of this utility class.
   */
  private GLError() {}
  }
