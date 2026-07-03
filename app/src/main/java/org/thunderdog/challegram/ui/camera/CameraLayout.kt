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
 */
package org.thunderdog.challegram.ui.camera

import android.content.Context

import org.thunderdog.challegram.unsorted.Settings

import me.vkryl.android.widget.FrameLayoutFix
import me.vkryl.core.aspectRatio

class CameraLayout (context: Context) : FrameLayoutFix(context) {
  private lateinit var parent: CameraController

  fun setParent (parent: CameraController) {
    this.parent = parent
  }

  private var lastWidth = 0
  private var lastHeight = 0

  private var disallowRatioChanges = false

  fun setDisallowRatioChanges (disallowRatioChanges: Boolean) {
    if (this.disallowRatioChanges != disallowRatioChanges) {
      this.disallowRatioChanges = disallowRatioChanges
      requestLayout()
    }
  }

  override fun onMeasure (widthMeasureSpec: Int, heightMeasureSpec: Int) {
    var viewWidth = getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
    var viewHeight = getDefaultSize(suggestedMinimumHeight, heightMeasureSpec)

    parent.manager.setParentSize(viewWidth, viewHeight)

    if (!disallowRatioChanges) {
      val maxAspectRatio = Settings.instance().cameraAspectRatio
      if (maxAspectRatio != 0f) {
        val aspectRatio = aspectRatio(viewWidth, viewHeight)
        if (aspectRatio > maxAspectRatio) {
          if (viewWidth > viewHeight) {
            viewWidth = (viewWidth.toFloat() / aspectRatio * maxAspectRatio).toInt()
          } else {
            viewHeight = (viewHeight.toFloat() / aspectRatio * maxAspectRatio).toInt()
          }
        }
      }
    }

    super.onMeasure(
      MeasureSpec.makeMeasureSpec(viewWidth, MeasureSpec.getMode(widthMeasureSpec)),
      MeasureSpec.makeMeasureSpec(viewHeight, MeasureSpec.getMode(heightMeasureSpec))
    )

    val width = measuredWidth
    val height = measuredHeight

    if (lastWidth != width || lastHeight != height) {
      lastWidth = width
      lastHeight = height

      parent.checkDisplayRotation()
    }
  }
}
