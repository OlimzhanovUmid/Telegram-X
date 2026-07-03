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
 * File created on 30/08/2017
 */
package org.thunderdog.challegram.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View

import org.thunderdog.challegram.loader.ImageReceiver
import org.thunderdog.challegram.tool.DrawAlgorithms

import me.vkryl.core.lambda.Destroyable

class ImageReceiverView (context: Context) : View(context), Destroyable, AttachDelegate {
  private val receiver: ImageReceiver = ImageReceiver(this, 0)
  private var isCircular = false

  private var overlayBitmap: Bitmap? = null

  fun setOverlayBitmap (bitmap: Bitmap?) {
    if (this.overlayBitmap != bitmap) {
      this.overlayBitmap = bitmap
      invalidate()
    }
  }

  fun setCircular (circular: Boolean) {
    isCircular = circular
  }

  fun getReceiver (): ImageReceiver {
    return receiver
  }

  override fun onMeasure (widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    val viewWidth = measuredWidth
    val viewHeight = measuredHeight
    receiver.setBounds(paddingLeft, paddingTop, viewWidth - paddingRight, viewHeight - paddingBottom)
    if (isCircular) {
      receiver.setRadius(Math.min(viewWidth, viewHeight) / 2f)
    }
  }

  override fun onDraw (c: Canvas) {
    receiver.draw(c)
    DrawAlgorithms.drawScaledBitmap(this, c, overlayBitmap)
  }

  override fun attach () {
    receiver.attach()
  }

  override fun detach () {
    receiver.detach()
  }

  override fun performDestroy () {
    receiver.destroy()
  }
}
