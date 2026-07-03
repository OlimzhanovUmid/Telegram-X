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
 * File created on 28/08/2017
 */
package org.thunderdog.challegram.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.view.View
import android.view.ViewOutlineProvider

import org.thunderdog.challegram.config.Config

import me.vkryl.android.widget.FrameLayoutFix

open class CircleFrameLayout (context: Context) : FrameLayoutFix(context) {
  private val path: Path?
  private val aspectPaint: Paint?

  private var transparentOutline = true

  init {
    if (!Config.DEBUG_CLIPPING) {
      aspectPaint = null

      path = null
      outlineProvider = object : ViewOutlineProvider() {
        override fun getOutline (view: View, outline: Outline) {
          val viewWidth = view.measuredWidth
          outline.setRoundRect(0, 0, viewWidth, view.measuredHeight, (viewWidth / 2).toFloat())
          outline.alpha = if (transparentOutline) 0f else 1f
        }
      }
      clipToOutline = true
    } else {
      aspectPaint = Paint(Paint.ANTI_ALIAS_FLAG)
      aspectPaint.color = -0x1000000
      aspectPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
      setLayerType(LAYER_TYPE_HARDWARE, null)

      path = Path()
    }
  }

  fun setTransparentOutline (transparentOutline: Boolean) {
    if (!Config.DEBUG_CLIPPING) {
      if (this.transparentOutline != transparentOutline) {
        this.transparentOutline = transparentOutline
        invalidateOutline()
      }
    }
  }

  private var lastWidth = 0
  private var lastHeight = 0

  protected open override fun onMeasure (widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    val newWidth = measuredWidth
    val newHeight = measuredHeight
    if (lastWidth != newWidth || lastHeight != newHeight) {
      lastWidth = newWidth
      lastHeight = newHeight
      if (!Config.DEBUG_CLIPPING) {
        invalidateOutline()
      } else {
        path!!.reset()
        path.addCircle((lastWidth / 2).toFloat(), (lastHeight / 2).toFloat(), (lastWidth / 2).toFloat(), Path.Direction.CW)
        path.toggleInverseFillType()
      }
    }
  }

  protected override fun dispatchDraw (c: Canvas) {
    if (!Config.DEBUG_CLIPPING) {
      super.dispatchDraw(c)
    } else {
      super.dispatchDraw(c)
      c.drawPath(path!!, aspectPaint!!)
    }
  }
}
