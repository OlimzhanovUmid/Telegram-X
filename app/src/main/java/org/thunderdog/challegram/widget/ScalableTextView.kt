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
 * File created on 15/12/2017
 */
package org.thunderdog.challegram.widget

import android.content.Context

import org.thunderdog.challegram.tool.Views

import me.vkryl.android.animator.FactorAnimator
import me.vkryl.android.DECELERATE_INTERPOLATOR

open class ScalableTextView (context: Context) : NoScrollTextView(context), FactorAnimator.Target {
  // Change

  private var changeAnimator: FactorAnimator? = null

  private var futureReplacement: CharSequence? = null

  fun replaceText (text: CharSequence?) {
    var changeAnimator = this.changeAnimator
    if (changeAnimator == null) {
      changeAnimator = FactorAnimator(0, this, DECELERATE_INTERPOLATOR, 180L)
      this.changeAnimator = changeAnimator
    } else {
      if (factor > .5f && factor != 1f) {
        if (futureReplacement != null && futureReplacement == text) {
          return
        }
        this.factor = 1f - factor
        changeAnimator.forceFactor(factor)
      } else {
        changeAnimator.forceFactor(0f)
      }
    }
    futureReplacement = text
    changeAnimator.animateTo(1f)
  }

  private var factor: Float = 0f

  private fun setFactor (factor: Float) {
    if (this.factor != factor) {
      this.factor = factor
      if (factor >= .5f && futureReplacement != null) {
        onReplaceText(futureReplacement!!)
        futureReplacement = null
      }
      val alpha = if (factor <= .5f) 1f - factor / .5f else (factor - .5f) / .5f
      val scale = .6f + .4f * alpha
      this.scaleX = scale
      this.scaleY = scale
      this.alpha = alpha
    }
  }

  override fun onFactorChanged (id: Int, factor: Float, fraction: Float, callee: FactorAnimator?) {
    setFactor(factor)
  }

  override fun onFactorChangeFinished (id: Int, finalFactor: Float, callee: FactorAnimator?) {

  }

  protected open fun onReplaceText (replacement: CharSequence) {
    Views.setMediumText(this, replacement)
  }
}
