/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.novenosemestre.ai_lens.RA_Objects2.ml.common.samplerender;

import android.opengl.GLES30;

import java.io.Closeable;
import java.nio.IntBuffer;


public class IndexBuffer implements Closeable {
  private final GpuBuffer buffer;


  public IndexBuffer(SampleRender render, IntBuffer entries) {
    buffer = new GpuBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, GpuBuffer.INT_SIZE, entries);
  }


  public void set(IntBuffer entries) {
    buffer.set(entries);
  }

  @Override
  public void close() {
    buffer.free();
  }

  /* package-private */
  int getBufferId() {
    return buffer.getBufferId();
  }

  /* package-private */
  int getSize() {
    return buffer.getSize();
  }
}
