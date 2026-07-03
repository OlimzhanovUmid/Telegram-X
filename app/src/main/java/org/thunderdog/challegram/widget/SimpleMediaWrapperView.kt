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
 * File created on 27/02/2017
 */
package org.thunderdog.challegram.widget

import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent

import org.thunderdog.challegram.data.MediaWrapper
import org.thunderdog.challegram.loader.DoubleImageReceiver
import org.thunderdog.challegram.loader.ImageReceiver
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Paints

import me.vkryl.android.util.SingleViewProvider
import me.vkryl.android.util.ViewProvider

class SimpleMediaWrapperView (context: Context) : SparseDrawableView(context) {
  private val preview: DoubleImageReceiver = DoubleImageReceiver(this, 0)
  private val receiver: ImageReceiver = ImageReceiver(this, 0)
  private val provider: ViewProvider = object : SingleViewProvider(this) {
    override fun invalidateContent (cause: Any?): Boolean {
      if (wrapper != null) {
        wrapper!!.requestImage(receiver)
      }
      return true
    }
  }

  private @ColorId var backgroundColorId: Int = 0

  fun setBackgroundColorId (@ColorId backgroundColorId: Int) {
    this.backgroundColorId = backgroundColorId
  }

  private var wrapper: MediaWrapper? = null

  fun setWrapper (wrapper: MediaWrapper?) {
    if (this.wrapper !== wrapper) {
      val previousWrapper = this.wrapper
      if (previousWrapper != null) {
        previousWrapper.setViewProvider(null)
      }
      this.wrapper = wrapper
      if (wrapper != null) {
        layoutWrapper()
        requestFiles()
        wrapper.fileProgress.downloadAutomatically()
        wrapper.setViewProvider(provider)
      }
    }
  }

  fun requestFiles () {
    if (this.wrapper != null) {
      wrapper!!.requestPreview(preview)
      wrapper!!.requestImage(receiver)
    } else {
      preview.clear()
      receiver.clear()
    }
  }

  override fun onMeasure (widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    layoutWrapper()
  }

  private var fitsBounds = false

  fun setFitsBounds () {
    fitsBounds = true
  }

  private fun layoutWrapper () {
    val viewWidth = measuredWidth
    val viewHeight = measuredHeight
    if (wrapper != null && viewWidth > 0 && viewHeight > 0) {
      if (fitsBounds) {
        var photoWidth = wrapper!!.contentWidth
        var photoHeight = wrapper!!.contentHeight

        val ratio = Math.min(viewWidth.toFloat() / photoWidth.toFloat(), viewHeight.toFloat() / photoHeight.toFloat())

        photoWidth = (photoWidth * ratio).toInt()
        photoHeight = (photoHeight * ratio).toInt()

        wrapper!!.buildContent(photoWidth, photoHeight)
      } else {
        wrapper!!.buildContent(viewWidth, viewHeight)
      }
    }
  }

  fun attach () {
    receiver.attach()
    preview.attach()
  }

  fun detach () {
    receiver.detach()
    preview.detach()
  }

  fun clear () {
    setWrapper(null)
  }

  override fun onTouchEvent (e: MotionEvent): Boolean {
    return wrapper != null && wrapper!!.onTouchEvent(this, e)
  }

  override fun onDraw (c: Canvas) {
    if (backgroundColorId != 0) {
      c.drawRect(paddingLeft.toFloat(), paddingTop.toFloat(), (measuredWidth - paddingRight).toFloat(), (measuredHeight - paddingBottom).toFloat(), Paints.fillingPaint(Theme.getColor(backgroundColorId)))
    }
    if (wrapper != null) {
      val centerX = paddingLeft + (measuredWidth - paddingLeft - paddingRight) / 2

      wrapper!!.draw(this, c, centerX - wrapper!!.cellWidth / 2, paddingTop, preview, receiver, 1f)
    }
  }
}
