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
 * File created on 19/09/2017
 */
package org.thunderdog.challegram.ui.camera

import android.content.Context
import android.view.TextureView
import org.thunderdog.challegram.Log

class CameraTextureView (context: Context) : TextureView(context) {
  private lateinit var manager: CameraManagerTexture

  private var ratioWidth = 0
  private var ratioHeight = 0
  @JvmField
  var scaledImageWidth = 0

  fun setManager (manager: CameraManagerTexture) {
    this.manager = manager
  }

  private var aspectRatioRequested = false

  fun setAspectRatio (width: Int, height: Int) {
    if (ratioWidth != width || ratioHeight != height) {
      ratioWidth = width
      ratioHeight = height
      if (!aspectRatioRequested) {
        requestLayout()
      }
    }
  }

  private var ignoreAspectRatio = false

  fun setIgnoreAspectRatio (ignore: Boolean) {
    if (this.ignoreAspectRatio != ignore) {
      this.ignoreAspectRatio = ignore
      requestLayout()
    }
  }

  override fun onMeasure (widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val viewWidth = getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
    val viewHeight = getDefaultSize(suggestedMinimumHeight, heightMeasureSpec)
    aspectRatioRequested = true
    manager.setPreviewSize(viewWidth, viewHeight)
    aspectRatioRequested = false

    if (!ignoreAspectRatio && ratioWidth > 0 && ratioHeight > 0) {
      var width: Int
      var height: Int
      if (viewWidth < viewHeight * (ratioWidth.toFloat() / ratioHeight.toFloat())) {
        width = viewWidth
        height = (viewWidth.toFloat() * (ratioHeight.toFloat() / ratioWidth.toFloat())).toInt()
      } else {
        width = (viewHeight.toFloat() * (ratioWidth.toFloat() / ratioHeight.toFloat())).toInt()
        height = viewHeight
      }

      val ratio = Math.max(viewWidth.toFloat() / width.toFloat(), viewHeight.toFloat() / height.toFloat())
      if (ratio != 1f) {
        width = (width * ratio).toInt()
        height = (height * ratio).toInt()
      }

      scaledImageWidth = width
      setMeasuredDimension(width, height)
    } else {
      setMeasuredDimension(viewWidth, viewHeight)
    }

    manager.prepareBitmaps(measuredWidth, measuredHeight)

    Log.i(Log.TAG_CAMERA, "CameraTextureView: onMeasure %d %d", measuredWidth, measuredHeight)
  }
}
