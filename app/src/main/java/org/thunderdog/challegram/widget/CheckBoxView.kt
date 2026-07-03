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
 * File created on 14/02/2016 at 22:17
 */
package org.thunderdog.challegram.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.Gravity
import android.view.View

import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Paints
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.tool.Views

import me.vkryl.android.animator.BoolAnimator
import me.vkryl.android.widget.FrameLayoutFix
import me.vkryl.android.DECELERATE_INTERPOLATOR
import me.vkryl.core.alphaColor
import me.vkryl.core.fromToArgb

class CheckBoxView (context: Context) : View(context) {
  private val isChecked = BoolAnimator(this, DECELERATE_INTERPOLATOR, 165L)
  private val isHidden = BoolAnimator(this, DECELERATE_INTERPOLATOR, 165L)
  private val isPartially = BoolAnimator(this, DECELERATE_INTERPOLATOR, 165L)
  private val isDisabled = BoolAnimator(this, DECELERATE_INTERPOLATOR, 165L)
  // TODO isIntermediate state, when check angle smoothly changes from 90 to 180 degrees

  private val rect: RectF
  private val outerPaint: Paint

  init {
    outerPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    outerPaint.color = Theme.radioOutlineColor()
    outerPaint.style = Paint.Style.STROKE

    rect = RectF()
  }

  fun setChecked (checked: Boolean, animated: Boolean) {
    isChecked.setValue(checked, animated)
  }

  fun setHidden (hidden: Boolean, animated: Boolean) {
    isHidden.setValue(hidden, animated)
  }

  fun setPartially (partially: Boolean, animated: Boolean) {
    isPartially.setValue(partially, animated)
  }

  fun toggle (): Boolean {
    return isChecked.toggleValue(true)
  }

  fun setDisabled (disabled: Boolean, animated: Boolean) {
    isDisabled.setValue(disabled, animated)
  }

  // Internal

  override fun onDraw (c: Canvas) {
    val showFactor = 1f - isHidden.getFloatValue()

    if (showFactor == 0f) {
      return
    }

    val factor = isChecked.getFloatValue()
    val alpha = (255f * showFactor).toInt()

    val x1 = Screen.dp(4f)
    val y1 = Screen.dp(11f)
    val lineSize = Screen.dp(1.5f)

    val rectFactor = Math.min(factor / FACTOR_DIFF, 1f)
    val checkFactor = if (factor <= FACTOR_DIFF) 0f else (factor - FACTOR_DIFF) / (1f - FACTOR_DIFF)
    val partFactor = isPartially.getFloatValue() // <= FACTOR_DIFF ? 0f : (partiallyFactor - FACTOR_DIFF) / (1f - FACTOR_DIFF);
    val scaleFactor = 1f - (if (rectFactor == 1f) 1f - checkFactor else rectFactor) * SCALE_DIFF

    val radius = Screen.dp(2f)
    val offset = (radius.toFloat() * .5f).toInt()

    outerPaint.strokeWidth = radius.toFloat()

    val size = Math.min(measuredWidth, measuredHeight)

    rect.left = offset.toFloat()
    rect.top = offset.toFloat()
    rect.right = (size - offset * 2).toFloat()
    rect.bottom = (size - offset * 2).toFloat()

    val cx = (rect.left + rect.right) * .5f
    val cy = (rect.top + rect.bottom) * .5f

    val restoreToCount = Views.save(c)
    c.scale(scaleFactor, scaleFactor, cx, cy)

    val color = fromToArgb(Theme.radioOutlineColor(), Theme.radioFillingColor(), rectFactor * (1f - isDisabled.getFloatValue()))
    outerPaint.color = color
    outerPaint.alpha = alpha
    c.drawRoundRect(rect, radius.toFloat(), radius.toFloat(), outerPaint)

    if (rectFactor != 0f) {
      val w = ((rect.right - rect.left - offset * 2) * .5f * rectFactor).toInt()
      val h = ((rect.bottom - rect.top - offset * 2) * .5f * rectFactor).toInt()

      val left = (rect.left + offset + w).toInt()
      val right = (rect.right - offset - w).toInt()

      val alphaColor = alphaColor(showFactor, color)

      c.drawRect(rect.left + offset, rect.top + offset, left.toFloat(), rect.bottom - offset, Paints.fillingPaint(alphaColor))
      c.drawRect(right.toFloat(), rect.top + offset, rect.right - offset, rect.bottom - offset, Paints.fillingPaint(alphaColor))
      c.drawRect(left.toFloat(), rect.top + offset, right.toFloat(), rect.top + offset + h, Paints.fillingPaint(alphaColor))
      c.drawRect(left.toFloat(), rect.bottom - offset - h, right.toFloat(), rect.bottom - offset, Paints.fillingPaint(alphaColor))

      if (checkFactor != 0f) {
        c.translate(-Screen.dp(.5f) * (1f - partFactor), 0f)
        c.translate(-Screen.dp(1.5f) * partFactor, -Screen.dp(1.5f) * partFactor)
        c.rotate(-45f * (1f - partFactor), cx, cy)

        val w2 = (Screen.dp(12f).toFloat() * checkFactor).toInt()
        val h1 = (Screen.dp(6f).toFloat() * checkFactor * (1f - partFactor)).toInt()

        val checkColor = alphaColor(showFactor, Theme.radioCheckColor())
        c.drawRect(x1.toFloat(), (y1 - h1).toFloat(), (x1 + lineSize).toFloat(), y1.toFloat(), Paints.fillingPaint(checkColor))
        c.drawRect(x1.toFloat(), (y1 - lineSize).toFloat(), (x1 + w2).toFloat(), y1.toFloat(), Paints.fillingPaint(checkColor))
      }
    }

    Views.restore(c, restoreToCount)
  }

  companion object {
    private const val FACTOR_DIFF = .65f
    private const val SCALE_DIFF = .15f

    @JvmStatic
    fun simpleCheckBox (context: Context): CheckBoxView {
      return simpleCheckBox(context, Lang.rtl())
    }

    @JvmStatic
    fun simpleCheckBox (context: Context, rtl: Boolean): CheckBoxView {
      val params = FrameLayoutFix.newParams(Screen.dp(18f), Screen.dp(18f))
      params.gravity = Gravity.CENTER_VERTICAL or (if (rtl) Gravity.LEFT else Gravity.RIGHT)
      params.leftMargin = Screen.dp(19f)
      params.rightMargin = Screen.dp(19f)

      val checkBox = CheckBoxView(context)
      checkBox.layoutParams = params

      return checkBox
    }
  }
}
