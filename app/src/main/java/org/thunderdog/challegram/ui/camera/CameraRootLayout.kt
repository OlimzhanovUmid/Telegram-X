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
 * File created on 18/09/2017
 */
package org.thunderdog.challegram.ui.camera

import android.content.Context
import android.graphics.RectF
import android.view.MotionEvent

import androidx.annotation.StringRes

import org.thunderdog.challegram.navigation.ViewController
import org.thunderdog.challegram.tool.UI

import me.vkryl.android.widget.FrameLayoutFix

open class CameraRootLayout (context: Context) : FrameLayoutFix(context) {
  @JvmField
  protected var controller: ViewController<*>? = null

  fun setController (controller: ViewController<*>) {
    this.controller = controller
  }

  protected override fun onLayout (changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
    super.onLayout(changed, left, top, right, bottom)
    controller?.executeScheduledAnimation()
  }

  override fun onTouchEvent (event: MotionEvent): Boolean {
    when (event.action) {
      MotionEvent.ACTION_DOWN -> {
        if (controller!!.isFocused()) {
          UI.getContext(context).prepareCameraDragByTouchDown(null, false)
        }
      }
    }
    return true
  }

  open fun setQrCorner (boundingBox: RectF?, height: Int, width: Int, rotation: Int, isLegacyZxing: Boolean) {}
  open fun resetQrCorner () {}
  open fun setQrMode (enable: Boolean, qrModeDebug: Boolean) {}
  open fun setQrModeSubtitle (@StringRes subtitleRes: Int) {}
  open fun onCameraClosed () {}
  open fun setComponentRotation (rotation: Float) {}
}
