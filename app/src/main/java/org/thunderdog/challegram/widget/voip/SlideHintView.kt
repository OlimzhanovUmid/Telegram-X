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
 * File created on 06/08/2017
 */
package org.thunderdog.challegram.widget.voip

import android.content.Context
import android.graphics.Canvas
import android.os.SystemClock
import android.view.View
import androidx.annotation.FloatRange
import org.thunderdog.challegram.tool.DrawAlgorithms
import org.thunderdog.challegram.tool.Screen
import me.vkryl.android.DECELERATE_INTERPOLATOR
import me.vkryl.core.clamp
import me.vkryl.core.color

class SlideHintView (context: Context) : View(context), Runnable {
  private var isLooping = false

  override fun setAlpha (@FloatRange(from = 0.0, to = 1.0) alpha: Float) {
    super.setAlpha(alpha)
    checkLooping()
  }

  override fun onMeasure (widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    checkLooping()
  }

  override fun setVisibility (visibility: Int) {
    super.setVisibility(visibility)
    checkLooping()
  }

  private fun checkLooping () {
    setIsLooping(visibility == View.VISIBLE && alpha > 0f && measuredWidth != 0 && measuredHeight != 0)
  }

  private fun setIsLooping (isLooping: Boolean) {
    if (this.isLooping != isLooping) {
      this.isLooping = isLooping
      if (isLooping) {
        postDelayed(this, 18L)
      } else {
        removeCallbacks(this)
      }
    }
  }

  override fun run () {
    invalidate()
    if (isLooping) {
      postDelayed(this, 18L)
    }
  }

  override fun onDraw (c: Canvas) {
    val remain = SystemClock.elapsedRealtime() % 1200
    val timeFactor = if (remain <= 300) 0f else DECELERATE_INTERPOLATOR.getInterpolation((remain - 300).toFloat() / 900f)

    val minAlpha = .4f

    // 1 2 3


    // 0 0 0
    // 1 0 0
    // 2 1 0
    // 1 2 1
    // 0 1 2
    // 0 0 1
    // 0 0 0

    var cx = Screen.dp(22f)
    val cy = measuredHeight / 2

    val position = -2f + timeFactor * 8f

    for (i in 0 until 3) {
      val factor = 1f - clamp(Math.abs(position - i - 1) / 3f)
      val alpha = minAlpha + (1f - minAlpha) * factor

      val color = color((255f * alpha).toInt(), 0xffffff)
      DrawAlgorithms.drawHorizontalDirection(c, cx.toFloat(), cy.toFloat(), color, true)
      DrawAlgorithms.drawHorizontalDirection(c, (measuredWidth - cx).toFloat(), cy.toFloat(), color, false)
      cx += Screen.dp(16f)
    }
  }
}
