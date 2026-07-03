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
 * File created on 20/08/2015 at 02:59
 */
package org.thunderdog.challegram.player

import android.content.Context
import android.graphics.Canvas
import android.graphics.Outline
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider

import org.thunderdog.challegram.U
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Paints
import org.thunderdog.challegram.tool.Screen

import me.vkryl.android.DECELERATE_INTERPOLATOR
import me.vkryl.android.animator.FactorAnimator
import me.vkryl.android.util.ClickHelper
import me.vkryl.android.widget.FrameLayoutFix
import me.vkryl.core.alphaColor

class RecordButton (context: Context) : View(context), FactorAnimator.Target, ClickHelper.Delegate {

  private val radiusAdd = Screen.dp(20f)
  private val radius = Screen.dp(41f)
  private val center = (radius + radiusAdd * MAX_FACTOR).toInt()

  private val helper = ClickHelper(this)

  init {
    val params = FrameLayoutFix.newParams(center * 2, center * 2, Gravity.TOP or Gravity.LEFT)

    setOutlineProvider(object : ViewOutlineProvider() {
      override fun getOutline (view: View, outline: Outline) {
        if (expand <= 0f) {
          outline.setEmpty()
        } else {
          val radius = (this@RecordButton.radius * expand).toInt()
          outline.setRoundRect(center - radius, center - radius, center + radius, center + radius, radius.toFloat())
        }
      }
    })
    setElevation(Screen.dp(1f).toFloat())
    setTranslationZ(Screen.dp(2f).toFloat())

    setLayoutParams(params)
  }

  private var onClickListener: View.OnClickListener? = null

  override fun setOnClickListener (l: View.OnClickListener?) {
    super.setOnClickListener(l)
    this.onClickListener = l
  }

  override fun onTouchEvent (e: MotionEvent): Boolean {
    return onClickListener != null && (e.action != MotionEvent.ACTION_DOWN || (expand == 1f && U.isInside(e.x, e.y, (measuredWidth / 2).toFloat(), (measuredHeight / 2).toFloat(), radius * expand))) && helper.onTouchEvent(this, e)
  }

  override fun needClickAt (view: View, x: Float, y: Float): Boolean {
    return U.isInside(x, y, (measuredWidth / 2).toFloat(), (measuredHeight / 2).toFloat(), radius * expand)
  }

  override fun onClickAt (view: View, x: Float, y: Float) {
    if (U.isInside(x, y, (measuredWidth / 2).toFloat(), (measuredHeight / 2).toFloat(), radius * expand)) {
      onClickListener?.onClick(this)
    }
  }

  fun getCenter (): Float {
    return center.toFloat()
  }

  fun getSize (): Int {
    return center * 2
  }

  // Animators

  var expand = 0f
    set (value) {
      if (field != value) {
        field = value
        invalidateOutline()
        invalidate()
      }
    }

  private var volume = 0f
  private var animator: FactorAnimator? = null

  private var scheduledToVolume = false
  private var currentToVolume = 0f

  fun setVolume (toVolume: Float, animated: Boolean) {
    val toVolume = Math.min(MAX_FACTOR, toVolume / 150f)
    if (animated) {
      if (Math.round(radiusAdd * currentToVolume) != Math.round(radiusAdd * toVolume)) {
        var animator = this.animator
        if (animator == null) {
          if (volume == toVolume) {
            return
          }
          animator = FactorAnimator(0, this, DECELERATE_INTERPOLATOR, 190L, volume)
          this.animator = animator
        }
        currentToVolume = toVolume
        if (toVolume >= currentToVolume || toVolume > 0 || !animator.isAnimating()) {
          scheduledToVolume = false
          animator.animateTo(toVolume)
        } else {
          scheduledToVolume = true
        }
      }
    } else {
      scheduledToVolume = false
      animator?.forceFactor(toVolume)
      setVolume(toVolume)
    }
  }

  private fun setVolume (volume: Float) {
    if (this.volume != volume) {
      this.volume = volume
      invalidate()
    }
  }

  override fun onFactorChanged (id: Int, factor: Float, fraction: Float, callee: FactorAnimator?) {
    setVolume(factor)
  }

  override fun onFactorChangeFinished (id: Int, finalFactor: Float, callee: FactorAnimator?) {
    if (scheduledToVolume) {
      animator?.animateTo(currentToVolume)
    }
  }

  public override fun onDraw (c: Canvas) {
    val color = Theme.getColor(ColorId.circleButtonRegular)
    c.drawCircle(center.toFloat(), center.toFloat(), (radius + radiusAdd * volume) * expand, Paints.fillingPaint(alphaColor(.3f, color)))
    c.drawCircle(center.toFloat(), center.toFloat(), radius * expand, Paints.fillingPaint(color))
  }

  companion object {
    private const val MAX_FACTOR = 3f
  }
}
