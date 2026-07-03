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
 * File created on 09/01/2017
 */
package org.thunderdog.challegram.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Outline
import android.view.View
import android.view.ViewOutlineProvider

import org.thunderdog.challegram.config.Config
import org.thunderdog.challegram.tool.Views

import me.vkryl.android.widget.FrameLayoutFix

class RectFrameLayout (context: Context) : FrameLayoutFix(context) {
  // Named clipTop/clipBottom (not top/bottom) to avoid shadowing View's inherited top/bottom properties.
  private var clipTop = 0
  private var clipBottom = 0

  init {
    if (!Config.DEBUG_CLIPPING) {
      outlineProvider = object : ViewOutlineProvider () {
        override fun getOutline (view: View, outline: Outline) {
          outline.setRect(0, clipTop, view.measuredWidth, view.measuredHeight - clipBottom)
        }
      }
      clipToOutline = true
    }
  }

  override fun onMeasure (widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    checkOutline()
  }

  private fun checkOutline () {
    if (!Config.DEBUG_CLIPPING) {
      invalidateOutline()
    }
  }

  fun setClip (top: Int, bottom: Int) {
    if (this.clipTop != top || this.clipBottom != bottom) {
      this.clipTop = top
      this.clipBottom = bottom
      checkOutline()
    }
  }

  override fun dispatchDraw (c: Canvas) {
    if (!Config.DEBUG_CLIPPING) {
      super.dispatchDraw(c)
    } else {
      val saveCount = Views.save(c)
      c.clipRect(0, clipTop, measuredWidth, measuredHeight - clipBottom)
      super.dispatchDraw(c)
      Views.restore(c, saveCount)
    }
  }
}
