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
 * File created on 30/06/2024
 */
package org.thunderdog.challegram.player

import android.content.Context
import android.graphics.Canvas

import org.thunderdog.challegram.navigation.ViewController
import org.thunderdog.challegram.support.RippleSupport
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Paints
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.tool.Views

import me.vkryl.android.DECELERATE_INTERPOLATOR
import me.vkryl.android.animator.BoolAnimator
import me.vkryl.android.animator.FactorAnimator
import me.vkryl.android.widget.FrameLayoutFix
import me.vkryl.core.fromToArgb

open class RecordControllerButton (context: Context) : FrameLayoutFix(context) {
  private var themeProvider: ViewController<*>? = null

  init {
    Views.setClickable(this)
  }

  open fun init (themeProvider: ViewController<*>?) {
    this.themeProvider = themeProvider
    RippleSupport.setCircleBackground(this, BUTTON_SIZE.toFloat(), PADDING.toFloat(), ColorId.filling, true, themeProvider)
  }

  protected open override fun onMeasure (widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val sizeMeasureSpec = MeasureSpec.makeMeasureSpec(Screen.dp((BUTTON_SIZE + PADDING * 2).toFloat()), MeasureSpec.EXACTLY)
    super.onMeasure(sizeMeasureSpec, sizeMeasureSpec)
  }

  protected open override fun dispatchDraw (canvas: Canvas) {
    val active = isActiveAnimator.getFloatValue()
    if (active > 0f) {
      val color = fromToArgb(Theme.getColor(ColorId.filling), Theme.getColor(ColorId.fillingPositive), active)
      canvas.drawCircle(measuredWidth / 2f, measuredHeight / 2f, Screen.dp(BUTTON_SIZE / 2f).toFloat(), Paints.fillingPaint(color))
    }

    super.dispatchDraw(canvas)
  }

  private val isActiveTarget = object : FactorAnimator.Target {
    override fun onFactorChanged (id: Int, factor: Float, fraction: Float, callee: FactorAnimator?) {
      invalidate()
      onActiveFactorChanged(factor)
    }

    override fun onFactorChangeFinished (id: Int, finalFactor: Float, callee: FactorAnimator?) {
      onActiveFactorChangeFinished(finalFactor)
    }
  }
  private val isActiveAnimator = BoolAnimator(0, isActiveTarget, DECELERATE_INTERPOLATOR, 220L)

  fun setActive (active: Boolean, animated: Boolean) {
    isActiveAnimator.setValue(active, animated)
  }

  fun toggleActive (animated: Boolean) {
    isActiveAnimator.setValue(!isActiveAnimator.getValue(), animated)
  }

  fun isActive (): Boolean {
    return isActiveAnimator.getValue()
  }

  fun getActiveFactor (): Float {
    return isActiveAnimator.getFloatValue()
  }

  protected open fun onActiveFactorChanged (factor: Float) {

  }

  protected open fun onActiveFactorChangeFinished (factor: Float) {

  }

  companion object {
    const val BUTTON_SIZE = 40
    const val PADDING = 5
  }
}
