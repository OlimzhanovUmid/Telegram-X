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
 * File created on 22/09/2017
 */
package org.thunderdog.challegram.ui.camera

import android.content.Context
import android.graphics.Canvas
import android.view.View

import org.thunderdog.challegram.tool.Paints
import org.thunderdog.challegram.tool.Screen

import me.vkryl.android.animator.FactorAnimator
import me.vkryl.android.widget.FrameLayoutFix
import me.vkryl.android.DECELERATE_INTERPOLATOR
import me.vkryl.core.alphaColor

class CameraBlurView (context: Context) : View(context), FactorAnimator.Target {
  companion object {
    private const val COLOR = 0x70eaeaea
  }

  init {
    setLayoutParams(FrameLayoutFix.newParams(Screen.dp(100f), Screen.dp(100f)))
  }

  private var factor = 0f

  fun setExpandFactor (factor: Float) {
    if (this.factor != factor) {
      this.factor = factor
      invalidate()
    }
  }

  private var successAnimator: FactorAnimator? = null

  fun performSuccessHint () {
    val animator = successAnimator
    if (animator == null) {
      successAnimator = FactorAnimator(0, this, DECELERATE_INTERPOLATOR, 400L)
    } else {
      animator.forceFactor(0f)
    }
    successAnimator?.animateTo(1f)
  }

  override fun onFactorChanged (id: Int, factor: Float, fraction: Float, callee: FactorAnimator?) {
    when (id) {
      0 -> invalidate()
    }
  }

  override fun onFactorChangeFinished (id: Int, finalFactor: Float, callee: FactorAnimator?) {
    when (id) {
      0 -> successAnimator?.forceFactor(0f)
    }
  }

  override fun onDraw (c: Canvas) {
    val viewWidth = measuredWidth
    val viewHeight = measuredHeight

    val cx = viewWidth / 2
    val cy = viewHeight / 2

    val radius = Screen.dp(40f + 10f * factor)
    c.drawCircle(cx.toFloat(), cy.toFloat(), radius.toFloat(), Paints.fillingPaint(COLOR))

    val successAnimator = this.successAnimator
    if (successAnimator != null) {
      val factor = successAnimator.getFactor()
      if (factor != 0f && factor != 1f) {
        val expandFactor = if (factor < .5f) factor / .5f else 1f
        val alpha = if (factor >= .4f) 1f - ((factor - .4f) / .6f) else 1f
        if (alpha != 0f) {
          c.drawCircle(cx.toFloat(), cy.toFloat(), radius * expandFactor, Paints.fillingPaint(alphaColor(alpha, COLOR)))
        }
      }
    }
  }
}
