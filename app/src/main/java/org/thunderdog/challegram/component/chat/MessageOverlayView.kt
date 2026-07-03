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
package org.thunderdog.challegram.component.chat

import android.content.Context
import android.graphics.Canvas
import android.graphics.Outline
import android.view.View
import android.view.ViewOutlineProvider
import org.thunderdog.challegram.data.TGMessage
import org.thunderdog.challegram.data.TGMessageVideo

class MessageOverlayView (context: Context) : View(context) {
  private var boundView: MessageView? = null

  init {
    outlineProvider = object : ViewOutlineProvider() {
      override fun getOutline (view: View, outline: Outline) {
        outline.setEmpty()
      }
    }
  }

  fun setBoundView (boundView: MessageView): MessageOverlayView {
    this.boundView = boundView
    return this
  }

  override fun onMeasure (widthMeasureSpec: Int, heightMeasureSpec: Int) {
    if (parent is MessageViewGroup) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    } else {
      val msg = this.msg
      msg?.buildLayout(measuredWidth)

      var height = msg?.getHeight() ?: 0
      if (msg is TGMessageVideo) {
        height = msg.getVideoMessageTargetHeight(true)
      }

      val newHeightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
      setMeasuredDimension(widthMeasureSpec, newHeightMeasureSpec)
    }
  }

  private var msg: TGMessage? = null

  fun setMessage (msg: TGMessage?) {
    if (this.msg !== msg) {
      if (this.msg != null) {
        this.msg?.onDetachedFromOverlayView(this)
      }
      this.msg = msg
      if (msg != null) {
        this.msg?.onAttachedToOverlayView(this)
      }
      invalidate()
    }
  }

  override fun onDraw (c: Canvas) {
    val msg = this.msg
    if (msg != null) {
      val boundView = this.boundView
      if (boundView != null) {
        msg.drawOverlay(boundView, c) // Full overlay
      }
    }
  }
}
