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

package com.google.ar.core.examples.java.ml

import android.app.Activity
import android.widget.Toast
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Session
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.novenosemestre.ai_lens.RA_Objects2.ml.common.helpers.CameraPermissionHelper

class ARCoreSessionLifecycleHelper(
  val activity: Activity,
  val features: Set<Session.Feature> = setOf()
) : DefaultLifecycleObserver {
  var installRequested = false
  var sessionCache: Session? = null
    private set

  var exceptionCallback: ((Exception) -> Unit)? = null

  var beforeSessionResume: ((Session) -> Unit)? = null

  fun tryCreateSession(): Session? {
    when (ArCoreApk.getInstance().requestInstall(activity, !installRequested)!!) {
      ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
        installRequested = true
        return null
      }
      ArCoreApk.InstallStatus.INSTALLED -> {
      }
    }

    return try {
      Session(activity, features)
    } catch (e: Exception) {
      exceptionCallback?.invoke(e)
      null
    }
  }

  override fun onResume(owner: LifecycleOwner) {
    if (!CameraPermissionHelper.hasCameraPermission(activity)) {
      CameraPermissionHelper.requestCameraPermission(activity)
      return
    }

    val session = tryCreateSession() ?: return
    try {
      beforeSessionResume?.invoke(session)
      session.resume()
      sessionCache = session
    } catch (e: CameraNotAvailableException) {
      exceptionCallback?.invoke(e)
    }
  }

  override fun onPause(owner: LifecycleOwner) {
    sessionCache?.pause()
  }

  override fun onDestroy(owner: LifecycleOwner) {
    sessionCache?.close()
    sessionCache = null
  }

  fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
  ) {
    if (!CameraPermissionHelper.hasCameraPermission(activity)) {
      Toast.makeText(activity, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
        .show()
      if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(activity)) {
        CameraPermissionHelper.launchPermissionSettings(activity)
      }
      activity.finish()
    }
  }
}
