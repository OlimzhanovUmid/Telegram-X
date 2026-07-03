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
import android.graphics.Paint
import android.view.View

import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Paints
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.tool.Views

import me.vkryl.android.animator.FactorAnimator
import me.vkryl.android.DECELERATE_INTERPOLATOR

class CheckCircle (context: Context) : View(context), FactorAnimator.Target {
  companion object {
    private var strokePaint: Paint? = null
    private const val SWITCH_FACTOR = .5f
  }

  private @ColorId var colorId: Int = 0

  init {
    if (strokePaint == null) {
      strokePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
        strokeWidth = Screen.dp(2f).toFloat()
        style = Paint.Style.STROKE
      }
    }
    Views.setClickable(this)
  }

  fun setColorId (color: Int) {
    this.colorId = color
  }

  private var isChecked = false

  fun setChecked (isChecked: Boolean, animated: Boolean) {
    if (this.isChecked != isChecked) {
      this.isChecked = isChecked
      if (animated) {
        animateFactor(if (isChecked) 1f else 0f)
      } else {
        forceFactor(if (isChecked) 1f else 0f)
      }
    }
  }

  private var factor = 0f
  private var animator: FactorAnimator? = null

  private fun animateFactor (toFactor: Float) {
    if (animator == null) {
      if (factor == toFactor) {
        return
      }
      animator = FactorAnimator(0, this, DECELERATE_INTERPOLATOR, 180, factor)
    }
    animator?.animateTo(toFactor)
  }

  private fun forceFactor (factor: Float) {
    animator?.forceFactor(factor)
    setFactor(factor)
  }

  private fun setFactor (factor: Float) {
    if (this.factor != factor) {
      this.factor = factor
      invalidate()
    }
  }

  override fun onFactorChanged (id: Int, factor: Float, fraction: Float, callee: FactorAnimator?) {
    setFactor(factor)
  }

  override fun onFactorChangeFinished (id: Int, finalFactor: Float, callee: FactorAnimator?) { }

  override fun onDraw (c: Canvas) {
    val cx = measuredWidth / 2
    val cy = measuredHeight / 2

    val fullRadius = Screen.dp(10f)
    val innerRadius = Screen.dp(5f)
    val eraseRadius = Screen.dp(8f)

    val color = Theme.getColor(colorId)
    strokePaint!!.color = color

    c.drawCircle(cx.toFloat(), cy.toFloat(), fullRadius - strokePaint!!.strokeWidth / 2, strokePaint!!)

    val factor = 1f - this.factor

    val factor1 = if (factor <= SWITCH_FACTOR) factor / SWITCH_FACTOR else 1f
    val factor2 = if (factor > SWITCH_FACTOR) (factor - SWITCH_FACTOR) / (1f - SWITCH_FACTOR) else 0f

    c.drawCircle(cx.toFloat(), cy.toFloat(), innerRadius + (fullRadius - innerRadius) * factor1, Paints.fillingPaint(color))

    if (factor2 > 0f) {
      c.drawCircle(cx.toFloat(), cy.toFloat(), (eraseRadius * factor2).toInt().toFloat(), Paints.fillingPaint(0xff000000.toInt()))
    }
  }
}
