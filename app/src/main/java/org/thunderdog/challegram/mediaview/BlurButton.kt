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
import android.util.TypedValue
import android.view.Gravity

import org.thunderdog.challegram.tool.Fonts
import org.thunderdog.challegram.tool.Views
import org.thunderdog.challegram.widget.NoScrollTextView

import me.vkryl.android.animator.FactorAnimator
import me.vkryl.core.util.ColorChanger
import me.vkryl.android.DECELERATE_INTERPOLATOR

class BlurButton (context: Context) : NoScrollTextView(context), FactorAnimator.Target {
  companion object {
    private val changer = ColorChanger(0x77ffffff, 0xffffffff.toInt())
  }

  init {
    Views.setClickable(this)
    setGravity(Gravity.CENTER)
    setSingleLine(true)
    setTypeface(Fonts.getRobotoMedium())
    setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
    setTextColor(changer.getColor(0f))
  }

  private var isChecked = false

  fun setIsChecked (isChecked: Boolean, animated: Boolean) {
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

  private fun animateFactor (factor: Float) {
    if (animator == null) {
      if (this.factor == factor) {
        return
      }
      animator = FactorAnimator(0, this, DECELERATE_INTERPOLATOR, 180L, this.factor)
    }
    animator?.animateTo(factor)
  }

  private fun forceFactor (factor: Float) {
    animator?.forceFactor(factor)
    setFactor(factor)
  }

  private fun setFactor (factor: Float) {
    if (this.factor != factor) {
      this.factor = factor
      setTextColor(changer.getColor(factor))
    }
  }

  override fun onFactorChanged (id: Int, factor: Float, fraction: Float, callee: FactorAnimator?) {
    setFactor(factor)
  }

  override fun onFactorChangeFinished (id: Int, finalFactor: Float, callee: FactorAnimator?) {

  }
}
