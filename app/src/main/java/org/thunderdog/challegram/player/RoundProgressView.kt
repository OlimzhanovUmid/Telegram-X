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
 * File created on 16/10/2017
 */
package org.thunderdog.challegram.player

import android.content.Context
import android.graphics.Canvas
import android.view.View

import org.thunderdog.challegram.tool.Paints
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.tool.UI
import org.thunderdog.challegram.widget.ProgressComponent

import me.vkryl.android.animator.FactorAnimator
import me.vkryl.android.DECELERATE_INTERPOLATOR
import me.vkryl.core.alphaColor

class RoundProgressView (context: Context) : View(context), FactorAnimator.Target {
  private var controller: RoundVideoController? = null

  private var progress: ProgressComponent? = null

  fun addProgressIfNeeded () {
    if (progress == null) {
      progress = ProgressComponent(UI.getContext(context), Screen.dp(6f))
      progress!!.setUseLargerPaint(Screen.dp(2f).toFloat())
      progress!!.setIsPrecise()
      progress!!.forceColor(0xffffffff.toInt())
      progress!!.setAlpha(0f)
      progress!!.attachToView(this)
      progress!!.setBounds(0, 0, measuredWidth, measuredHeight)
    }
  }

  override fun onFactorChanged (id: Int, factor: Float, fraction: Float, callee: FactorAnimator?) {
    setProgressAlpha(factor)
  }

  override fun onFactorChangeFinished (id: Int, finalFactor: Float, callee: FactorAnimator?) {
  }

  private var progressVisible = false
  private var progressVisibilityAnimator: FactorAnimator? = null
  private var progressVisibleFactor = 0f

  fun setLoadProgress (progress: Float) {
    this.progress?.setProgress(progress, true)
  }

  fun setProgressVisible (isVisible: Boolean) {
    if (this.progressVisible != isVisible) {
      this.progressVisible = isVisible

      addProgressIfNeeded()

      if (isVisible) {
        progress!!.setProgress(0f, true)
      }

      val toFactor = if (isVisible) 1f else 0f
      val animated = alpha != 0f && parent != null && (parent as View).alpha != 0f

      if (animated) {
        if (progressVisibilityAnimator == null) {
          if (progressVisibleFactor == toFactor) {
            return
          }
          progressVisibilityAnimator = FactorAnimator(0, this, DECELERATE_INTERPOLATOR, 180L, this.progressVisibleFactor)
        }
        progressVisibilityAnimator!!.animateTo(toFactor)
      } else {
        if (progressVisibilityAnimator != null) {
          progressVisibilityAnimator!!.forceFactor(toFactor)
        }
        setProgressAlpha(toFactor)
      }
    }
  }

  private fun setProgressAlpha (alpha: Float) {
    if (this.progressVisibleFactor != alpha) {
      this.progressVisibleFactor = alpha
      progress!!.setAlpha(alpha)
      invalidate()
    }
  }

  fun setController (controller: RoundVideoController?) {
    this.controller = controller
  }

  private var removeDegrees = 0f
  private var totalDistance = 0f

  override fun onMeasure (widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)

    val removeDistance = Paints.videoStrokePaint().strokeWidth.toDouble()
    val totalDistance = (2.0 * Math.PI * (measuredWidth / 2).toDouble()).toInt().toDouble()
    this.totalDistance = (totalDistance - removeDistance).toFloat()
    this.removeDegrees = (removeDistance / totalDistance).toFloat() * 360f

    if (progress != null) {
      progress!!.setBounds(0, 0, measuredWidth, measuredHeight)
    }
  }

  private var visualProgress = 0f

  fun setVisualProgress (progress: Float) {
    if (this.visualProgress != progress) {
      this.visualProgress = progress
      if ((totalDistance * lastDrawProgress).toInt() != (totalDistance * progress).toInt()) {
        invalidate()
      }
    }
  }

  private var lastDrawProgress = 0f

  override fun onDraw (c: Canvas) {
    val progress = if (controller != null) controller!!.visualProgress else this.visualProgress
    val viewWidth = measuredWidth
    val viewHeight = measuredHeight
    if (progress != 0f) {
      val rectF = Paints.getRectF()
      val padding = Screen.dp(1.5f)

      rectF.set(padding.toFloat(), padding.toFloat(), (viewWidth - padding).toFloat(), (viewHeight - padding).toFloat())
      c.drawArc(rectF, -90f, (360f - removeDegrees) * progress, false, Paints.videoStrokePaint())
    }
    if (this.progress != null) {
      c.drawCircle((viewWidth / 2).toFloat(), (viewHeight / 2).toFloat(), Screen.dp(12f).toFloat(), Paints.fillingPaint(alphaColor(this.progress!!.alpha, 0x44000000)))
      this.progress!!.draw(c)
    }
    this.lastDrawProgress = progress
  }
}
