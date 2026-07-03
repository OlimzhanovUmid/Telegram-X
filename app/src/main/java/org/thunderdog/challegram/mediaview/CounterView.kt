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
 * File created on 10/12/2016
 */
package org.thunderdog.challegram.mediaview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View

import org.thunderdog.challegram.config.Config
import org.thunderdog.challegram.navigation.CounterHeaderView
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.tool.Paints
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.tool.Views

class CounterView (context: Context) : CounterHeaderView(context) {
  init {
    init(14f, ColorId.white, Screen.dp(16f), 0, Screen.dp(9f) + Screen.dp(10f))
    if (Config.HARDWARE_CLIP_PATH_FIX) {
      Views.setLayerType(this, View.LAYER_TYPE_HARDWARE)
    }
  }

  override fun needSpecial (): Boolean {
    return true
  }

  private var path: Path? = Path()
  private var lastLeft = 0f
  private var lastRight = 0f

  override fun alignRight (): Boolean {
    return false
  }

  override fun drawSpecial (c: Canvas, width: Int, add: Int) {
    c.save()

    val viewWidth = measuredWidth
    val viewHeight = measuredHeight
    val radius = viewHeight / 2

    val rectF = Paints.getRectF()

    val strokeWidth = Screen.dp(2f).toFloat()
    val left = viewWidth - radius - radius - add / 2 + strokeWidth / 2
    val right = viewWidth - strokeWidth / 2
    rectF.set(left, strokeWidth / 2, right, viewHeight - strokeWidth / 2)
    c.drawRoundRect(rectF, radius.toFloat(), radius.toFloat(), Paints.fillingPaint(0xa0000000.toInt()))
    c.drawRoundRect(rectF, radius.toFloat(), radius.toFloat(), Paints.getProgressPaint(0xffffffff.toInt(), strokeWidth))

    if (path != null) {
      if (lastLeft != left || lastRight != right) {
        lastLeft = left
        lastRight = right
        path!!.reset()
        path!!.addRoundRect(rectF, radius.toFloat(), radius.toFloat(), Path.Direction.CCW)
      }
      try {
        c.clipPath(path!!)
      } catch (ignored: Throwable) { }
    }

    val textLeft = (left + right).toInt() / 2 - width / 2
    c.translate(textLeft.toFloat(), 0f)
  }

  override fun restoreSpecial (c: Canvas) {
    c.restore()
  }

  override fun onMeasure (widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val size = layoutParams.height
    setMeasuredDimension(size + fullWidth, heightMeasureSpec)
    pivotX = (measuredWidth - size / 2).toFloat()
    pivotY = (measuredHeight / 2).toFloat()
  }

  override fun onTouchEvent (event: MotionEvent): Boolean {
    return Views.isValid(this) && super.onTouchEvent(event)
  }
}
