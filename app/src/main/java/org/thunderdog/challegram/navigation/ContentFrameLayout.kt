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
 * File created on 15/05/2015 at 23:30
 */
package org.thunderdog.challegram.navigation

import android.content.Context
import android.graphics.Canvas
import android.view.View

import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.unsorted.Size

import me.vkryl.android.widget.FrameLayoutFix

class ContentFrameLayout (context: Context) : FrameLayoutFix(context) {
  private var lastWidth = 0
  private var lastHeight = 0
  private var targetDiff = 0f
  private var currentTarget = 0f
  private var factor = 0f

  var clipLeft: Int = 0
    set (value) {
      if (field != value) {
        if (value < field) {
          field = value
          invalidate()
        } else {
          field = value
        }
      }
    }

  override fun onMeasure (widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    if (CUT_ENABLED) {
      lastWidth = measuredWidth
      lastHeight = measuredHeight
      currentTarget = -(Screen.currentWidth().toFloat() / Size.NAVIGATION_PREVIEW_TRANSLATE_FACTOR)
      targetDiff = lastWidth + currentTarget
    }
  }

  override fun setTranslationX (x: Float) {
    super.setTranslationX(x)
    if (CUT_ENABLED) {
      factor = x / currentTarget
      if (x <= 0f) {
        invalidate()
      }
    }
  }

  override fun drawChild (c: Canvas, v: View, drawingTime: Long): Boolean {
    if (CUT_ENABLED) {
      if (clipLeft != 0) {
        c.save()
        c.clipRect(clipLeft, 0, lastWidth, lastHeight)
        val result = super.drawChild(c, v, drawingTime)
        c.restore()
        return result
      }
      val x = translationX
      if (x < 0f) {
        c.save()
        c.clipRect(0f, 0f, lastWidth - factor * targetDiff, lastHeight.toFloat())
        val result = super.drawChild(c, v, drawingTime)
        c.restore()
        return result
      } else {
        return super.drawChild(c, v, drawingTime)
      }
    } else {
      return super.drawChild(c, v, drawingTime)
    }
  }

  companion object {
    const val CUT_ENABLED = false
  }
}
