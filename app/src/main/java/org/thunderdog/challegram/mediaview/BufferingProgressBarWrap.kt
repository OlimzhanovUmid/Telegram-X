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
 */
package org.thunderdog.challegram.mediaview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.view.View

import org.thunderdog.challegram.tool.Paints
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.tool.UI
import org.thunderdog.challegram.widget.ProgressComponent

import me.vkryl.android.ACCELERATE_DECELERATE_INTERPOLATOR
import me.vkryl.android.animator.BoolAnimator
import me.vkryl.android.animator.FactorAnimator
import me.vkryl.android.util.SingleViewProvider
import me.vkryl.core.color
import me.vkryl.core.lambda.Destroyable

class BufferingProgressBarWrap (context: Context) : View(context), Destroyable {
  private val fakeDialogFrame = RectF()
  private lateinit var progressComponent: ProgressComponent

  private val progressVisible = BoolAnimator(0, FactorAnimator.Target { id: Int, factor: Float, fraction: Float, callee: FactorAnimator? ->
    progressComponent.setAlpha(factor)
  }, ACCELERATE_DECELERATE_INTERPOLATOR, 210L)

  private var progressDelayRunnable: Runnable? = null

  init {
    setWillNotDraw(false)
    progressComponent = ProgressComponent(UI.getContext(context), Screen.dp(18f))
    progressComponent.forceColor(Color.WHITE)
    progressComponent.setUseStupidInvalidate()
    progressComponent.setUseLargerPaint(Screen.dp(4f).toFloat())
    progressComponent.setViewProvider(SingleViewProvider(this))
    progressComponent.setAlpha(0f)
    progressComponent.attachToView(this)
  }

  override fun onMeasure (widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)

    val cx = measuredWidth / 2
    val cy = measuredHeight / 2
    val dSize = Screen.dp(42f)

    progressComponent.setBounds(0, 0, measuredWidth, measuredHeight)
    fakeDialogFrame.set((cx - dSize).toFloat(), (cy - dSize).toFloat(), (cx + dSize).toFloat(), (cy + dSize).toFloat())
  }

  override fun performDestroy () {
    progressComponent.detachFromView(this)
    progressComponent.performDestroy()
  }

  fun setProgressVisibleInstant (value: Boolean) {
    if (progressDelayRunnable != null) {
      removeCallbacks(progressDelayRunnable!!)
      progressDelayRunnable = null
    }

    progressVisible.setValue(value, false)
  }

  fun setProgressVisible (value: Boolean, delay: Boolean) {
    if (value) {
      if (!delay) {
        progressVisible.setValue(true, true)
      } else if (progressDelayRunnable == null) {
        progressDelayRunnable = Runnable {
          progressVisible.setValue(true, true)
          progressDelayRunnable = null
        }

        postDelayed(progressDelayRunnable!!, 350L)
      }
    } else {
      if (progressDelayRunnable != null) {
        removeCallbacks(progressDelayRunnable!!)
        progressDelayRunnable = null
      }

      progressVisible.setValue(false, true)
    }
  }

  override fun onDraw (canvas: Canvas) {
    super.onDraw(canvas)

    val overlayColor = 0x4c000000
    val bgColor = color((Color.alpha(overlayColor) * progressVisible.getFloatValue()).toInt(), overlayColor)

    canvas.drawRoundRect(fakeDialogFrame, Screen.dp(12f).toFloat(), Screen.dp(12f).toFloat(), Paints.getPorterDuffPaint(bgColor))
    progressComponent.draw(canvas)
  }
}
