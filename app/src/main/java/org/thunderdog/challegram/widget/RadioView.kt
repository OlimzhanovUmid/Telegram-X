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
 * File created on 05/02/2016 at 19:13
 */
package org.thunderdog.challegram.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.Gravity
import android.view.View

import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Paints
import org.thunderdog.challegram.tool.Screen

import me.vkryl.android.DECELERATE_INTERPOLATOR
import me.vkryl.android.animator.BoolAnimator
import me.vkryl.android.animator.FactorAnimator
import me.vkryl.android.widget.FrameLayoutFix
import me.vkryl.core.clamp
import me.vkryl.core.fromToArgb

class RadioView (context: Context) : View(context), FactorAnimator.Target {
  private val radioPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
    strokeWidth = Screen.dp(2f).toFloat()
    style = Paint.Style.STROKE
  }

  private val checkAnimator = BoolAnimator(0, this, DECELERATE_INTERPOLATOR, 192L, false)
  private val activeAnimator = BoolAnimator(1, this, DECELERATE_INTERPOLATOR, 180L, true)

  /*private float selectFactor;

  private void setSelectFactor (float selectFactor) {
    if (this.selectFactor != selectFactor) {
      this.selectFactor = selectFactor;
      invalidate();
    }
  }*/

  override fun onFactorChanged (id: Int, factor: Float, fraction: Float, callee: FactorAnimator?) {
    invalidate()
  }

  override fun onFactorChangeFinished (id: Int, finalFactor: Float, callee: FactorAnimator?) {
    invalidate()
  }

  fun isChecked (): Boolean {
    return checkAnimator.getValue()
  }

  fun setChecked (checked: Boolean, isAnimated: Boolean) {
    checkAnimator.setValue(checked, isAnimated)
  }

  fun setActive (active: Boolean, animated: Boolean) {
    activeAnimator.setValue(active, animated)
  }

  private var colorId: Int = 0

  fun setColorId (colorId: Int) {
    if (this.colorId != colorId) {
      this.colorId = colorId
      invalidate()
    }
  }

  fun toggleChecked (): Boolean {
    setChecked(!checkAnimator.getValue(), true)
    return isChecked()
  }

  private var useColor: Boolean = false

  fun setApplyColor (useColor: Boolean) {
    if (this.useColor != useColor) {
      this.useColor = useColor
      invalidate()
    }
  }

  override fun onDraw (c: Canvas) {
    val paddingLeft = paddingLeft
    val paddingTop = paddingTop

    val cx = (paddingLeft + (measuredWidth - paddingLeft - paddingRight) / 2).toFloat()
    val cy = (paddingTop + (measuredHeight - paddingTop - paddingBottom) / 2).toFloat()

    val radius = Screen.dp(9f).toFloat()
    val innerRadius = Screen.dp(5f).toFloat()

    val color = Theme.getColor(if (colorId != 0) colorId else ColorId.controlInactive)
    val radioFillingColor = fromToArgb(color, if (colorId != 0 && useColor) color else Theme.radioFillingColor(), activeAnimator.getFloatValue())

    val selectFactor = checkAnimator.getFloatValue()

    if (selectFactor == 0f || selectFactor == 1f) {
      radioPaint.color = fromToArgb(color, radioFillingColor, selectFactor)
      c.drawCircle(cx, cy, radius, radioPaint)
    }

    val factor = 1f - selectFactor

    if (factor == 0f) {
      c.drawCircle(cx, cy, innerRadius, Paints.fillingPaint(radioFillingColor))
    } else if (factor != 1f) {
      val addRadius = Screen.dp(4f).toFloat()
      val totalRadius = innerRadius + radius

      val currentRadius = factor * totalRadius
      val fillRadius = Math.max(0f, currentRadius - addRadius)

      val radioFactor = DECELERATE_INTERPOLATOR.getInterpolation(1f - clamp(fillRadius / (totalRadius - addRadius)))
      val radioColor = fromToArgb(color, radioFillingColor, radioFactor)

      c.drawCircle(cx, cy, innerRadius + Math.min(addRadius, currentRadius), Paints.fillingPaint(radioColor))
      c.drawCircle(cx, cy, fillRadius, Paints.fillingPaint(Theme.fillingColor()))

      radioPaint.color = radioColor
      c.drawCircle(cx, cy, radius, radioPaint)
    }
  }

  companion object {
    @JvmStatic
    fun simpleRadioView (context: Context): RadioView {
      return simpleRadioView(context, Lang.rtl())
    }

    @JvmStatic
    fun simpleRadioView (context: Context, alignLeft: Boolean): RadioView {
      val params = FrameLayoutFix.newParams(Screen.dp(22f), Screen.dp(22f))
      params.gravity = Gravity.CENTER_VERTICAL or (if (alignLeft) Gravity.LEFT else Gravity.RIGHT)
      params.rightMargin = Screen.dp(18f)
      params.leftMargin = Screen.dp(18f)

      val radioView = RadioView(context)
      radioView.layoutParams = params

      return radioView
    }
  }
}
