/*
 * This file is a part of Telegram X
 * Copyright © 2014 (tgx-android@pm.me)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * File created on 21/09/2017
 */
package org.thunderdog.challegram.ui.camera

import android.content.Context
import android.view.MotionEvent
import org.thunderdog.challegram.U
import org.thunderdog.challegram.component.chat.InvisibleImageView
import org.thunderdog.challegram.navigation.ViewController
import org.thunderdog.challegram.tool.UI
import org.thunderdog.challegram.ui.MessagesController
import org.thunderdog.challegram.unsorted.Settings

class CameraAccessImageView (context: Context, private val c: MessagesController) : InvisibleImageView(context) {
  private var hasAnyCamera = U.deviceHasAnyCamera(context)

  private var cameraOpenOptions: ViewController.CameraOpenOptions? = null

  override fun onTouchEvent (event: MotionEvent): Boolean {
    val res = super.onTouchEvent(event)
    if (Settings.instance().cameraType != Settings.CAMERA_TYPE_SYSTEM && res && event.action == MotionEvent.ACTION_DOWN) {
      if (!hasAnyCamera) {
        hasAnyCamera = U.deviceHasAnyCamera(context)
      }
      if (hasAnyCamera && c.canSendPhotosAndVideos()) {
        if (cameraOpenOptions == null)
          cameraOpenOptions = ViewController.CameraOpenOptions()
        UI.getContext(context).prepareCameraDragByTouchDown(cameraOpenOptions, true)
      }
    }
    return res
  }

  fun setCameraOpenOptions (options: ViewController.CameraOpenOptions?) {
    this.cameraOpenOptions = options
  }
}
