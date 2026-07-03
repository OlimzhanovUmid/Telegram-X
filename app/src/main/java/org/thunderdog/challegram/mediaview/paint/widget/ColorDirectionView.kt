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
 * File created on 11/05/2017
 */
package org.thunderdog.challegram.mediaview.paint.widget

import android.content.Context
import android.graphics.Canvas
import android.view.View

import org.thunderdog.challegram.tool.Paints
import org.thunderdog.challegram.tool.Screen

import me.vkryl.core.distance

class ColorDirectionView (context: Context) : View(context) {
  private var factor = 0f
  private var h = 0f
  private var s = 0f
  private var v = 0f

  fun setHsv (h: Float, hsv: FloatArray) {
    if (this.h != h || this.s != hsv[1] || this.v != hsv[2]) {
      this.h = h
      this.s = hsv[1]
      this.v = hsv[2]
      if (factor > 0f) {
        invalidate()
      }
    }
  }

  fun setFactor (factor: Float) {
    if (this.factor != factor) {
      this.factor = factor
      invalidate()
    }
  }

  private var pickerWidth = 0
  private var pickerLeft = 0

  fun setPickerLeft (left: Int) {
    if (this.pickerLeft != left) {
      this.pickerLeft = left
      if (factor > 0f) {
        invalidate()
      }
    }
  }

  fun setPickerWidth (width: Int) {
    if (this.pickerWidth != width) {
      this.pickerWidth = width
      if (factor > 0f) {
        invalidate()
      }
    }
  }

  override fun onDraw (c: Canvas) {
    if (factor == 0f) {
      return
    }

    val viewWidth = measuredWidth.toFloat()
    val viewHeight = measuredHeight.toFloat()

    val areaHeight = viewHeight - paddingTop

    val fromX = pickerLeft + pickerWidth * h
    val fromY = viewHeight

    val toX = viewWidth * s
    val toY = viewHeight - Screen.dp(18f) - (areaHeight - Screen.dp(18f)) * v

    val smallRadius = Screen.dp(1f).toFloat()
    val bigRadius = Screen.dp(3f).toFloat()

    val color = 0xe0ffffff.toInt()

    val spacing = Screen.dp(6f).toFloat()

    val dist = distance(fromX, fromY, toX, toY)

    val circleCount = Math.floor((dist / spacing).toDouble()).toInt()

    var diffX = (toX - fromX) / circleCount
    var diffY = (toY - fromY) / circleCount

    val half = (Screen.dp(48f) / 2).toFloat()
    val toX2 = Math.max(half + Screen.dp(8f), Math.min(viewWidth - half - Screen.dp(8f), toX))
    val toY2 = toY - Screen.dp(ColorPreviewView.MARGIN_DISTANCE) + half

    val dist2 = distance(toX, toY, toX2, toY2)
    val circleCount2 = Math.floor((dist2 / spacing).toDouble()).toInt()

    val factorPerCircle = 1f / (circleCount + circleCount2 + 1f)
    var remainingFactor = factor

    var remainingDistance = Math.abs(dist - dist / circleCount)
    var cx: Float
    var cy: Float

    cx = fromX + diffX
    cy = fromY + diffY
    for (i in 1 until circleCount) {
      val circleFactor = if (remainingFactor > factorPerCircle) 1f else remainingFactor / factorPerCircle

      c.drawCircle(cx, cy, smallRadius * circleFactor, Paints.fillingPaint(color))

      cx += diffX
      cy += diffY

      remainingDistance -= Math.abs(dist / circleCount)
      remainingFactor -= factorPerCircle

      if (remainingFactor <= 0) {
        return
      }

      if (remainingDistance < bigRadius * 2) {
        break
      }
    }

    val pointFactor = if (remainingFactor > factorPerCircle) 1f else remainingFactor / factorPerCircle
    c.drawCircle(toX, toY, bigRadius * pointFactor, Paints.fillingPaint(color))
    remainingFactor -= factorPerCircle

    remainingDistance = Math.abs(dist2 - dist2 / circleCount2)
    diffX = (toX2 - toX) / circleCount2
    diffY = (toY2 - toY) / circleCount2

    cx = toX + diffX
    cy = toY + diffY - bigRadius

    for (i in 0 until circleCount2) {
      val circleFactor = if (remainingFactor > factorPerCircle) 1f else remainingFactor / factorPerCircle

      c.drawCircle(cx, cy, smallRadius * circleFactor, Paints.fillingPaint(color))

      cx += diffX
      cy += diffY

      remainingDistance -= Math.abs(dist2 / circleCount2)
      remainingFactor -= factorPerCircle

      if (remainingFactor <= 0f) {
        return
      }
      if (remainingDistance < smallRadius * 2) {
        break
      }
    }
  }
}
