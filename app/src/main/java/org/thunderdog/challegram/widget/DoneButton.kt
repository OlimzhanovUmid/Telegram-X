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
 * File created on 25/08/2017
 */
package org.thunderdog.challegram.widget

import android.content.Context
import android.graphics.Rect
import android.view.Gravity

import org.thunderdog.challegram.R
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.tool.Views
import org.thunderdog.challegram.unsorted.Settings

import me.vkryl.android.animator.FactorAnimator
import me.vkryl.android.widget.FrameLayoutFix
import me.vkryl.android.DECELERATE_INTERPOLATOR
import me.vkryl.android.OVERSHOOT_INTERPOLATOR
import me.vkryl.core.clamp

class DoneButton (context: Context) : CircleButton(context), RootFrameLayout.InsetsChangeListener {
  companion object {
    private const val ANIMATOR_VISIBILITY = 0
  }

  private var maximumAlpha = 1f

  private val target: FactorAnimator.Target = object : FactorAnimator.Target {
    override fun onFactorChanged (id: Int, factor: Float, fraction: Float, callee: FactorAnimator?) {
      when (id) {
        ANIMATOR_VISIBILITY -> setVisibilityFactor(factor)
      }
    }

    override fun onFactorChangeFinished (id: Int, finalFactor: Float, callee: FactorAnimator?) {

    }
  }

  init {
    this.init(R.drawable.baseline_check_24, 56f, 4f, ColorId.circleButtonRegular, ColorId.circleButtonRegularIcon)

    val padding = Screen.dp(4f)
    val params = FrameLayoutFix.newParams(Screen.dp(56f) + padding * 2, Screen.dp(56f) + padding * 2, (if (Lang.rtl()) Gravity.LEFT else Gravity.RIGHT) or Gravity.BOTTOM)
    params.rightMargin = Screen.dp(16f) - padding
    params.bottomMargin = Screen.dp(16f) - padding
    setLayoutParams(params)

    setAlpha(0f)
    setScaleX(.6f)
    setScaleY(.6f)
  }

  private var rootFrameLayout: RootFrameLayout? = null

  override fun onAttachedToWindow () {
    super.onAttachedToWindow()
    if (Settings.instance().useEdgeToEdge()) {
      val rootFrameLayout = Views.findAncestor(this, RootFrameLayout::class.java, true)
      this.rootFrameLayout = rootFrameLayout
      if (rootFrameLayout != null) {
        rootFrameLayout.addInsetsChangeListener(this)
        Views.setBottomMargin(this, Screen.dp(16f) - Screen.dp(4f) + rootFrameLayout.systemInsets.bottom)
      }
    }
  }

  override fun onDetachedFromWindow () {
    super.onDetachedFromWindow()
    rootFrameLayout?.removeInsetsChangeListener(this)
    rootFrameLayout = null
  }

  override fun onInsetsChanged (viewGroup: RootFrameLayout, effectiveInsets: Rect, effectiveInsetsWithoutIme: Rect, systemInsets: Rect, systemInsetsWithoutIme: Rect, isUpdate: Boolean) {
    Views.setBottomMargin(this, Screen.dp(16f) - Screen.dp(4f) + systemInsets.bottom)
  }

  private var isVisible = false
  private var visibilityFactor = 0f
  private var visibilityAnimator: FactorAnimator? = null

  fun getIsVisible (): Boolean {
    return isVisible
  }

  fun setIsVisible (isVisible: Boolean, animated: Boolean) {
    if (this.isVisible != isVisible) {
      this.isVisible = isVisible

      val toFactor = if (isVisible) 1f else 0f

      if (animated && maximumAlpha > 0f) {
        var visibilityAnimator = this.visibilityAnimator
        if (visibilityAnimator == null) {
          visibilityAnimator = FactorAnimator(ANIMATOR_VISIBILITY, target, OVERSHOOT_INTERPOLATOR, 210L, visibilityFactor)
          this.visibilityAnimator = visibilityAnimator
        }
        if (toFactor == 1f && visibilityFactor == 0f) {
          visibilityAnimator.setInterpolator(OVERSHOOT_INTERPOLATOR)
          visibilityAnimator.setDuration(210L)
        } else {
          visibilityAnimator.setInterpolator(DECELERATE_INTERPOLATOR)
          visibilityAnimator.setDuration(100L)
        }
        visibilityAnimator.animateTo(toFactor)
      } else {
        visibilityAnimator?.forceFactor(toFactor)
        setVisibilityFactor(toFactor)
      }
    }
  }

  protected fun setVisibilityFactor (factor: Float) {
    if (this.visibilityFactor != factor) {
      this.visibilityFactor = factor
      val scale = .6f + .4f * factor
      setScaleX(scale)
      setScaleY(scale)
      updateAlpha()
    }
  }

  fun setMaximumAlpha (alpha: Float) {
    if (this.maximumAlpha != alpha) {
      this.maximumAlpha = alpha
      updateAlpha()
    }
  }

  private fun updateAlpha () {
    setAlpha(clamp(visibilityFactor) * clamp(maximumAlpha))
  }
}
