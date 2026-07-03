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
 * File created on 26/04/2015 at 11:27
 */
package org.thunderdog.challegram.widget

import android.content.Context
import android.view.View
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.ImageView

import org.thunderdog.challegram.Log
import org.thunderdog.challegram.tool.Anim
import org.thunderdog.challegram.tool.Views
import me.vkryl.android.LINEAR_INTERPOLATOR


class SpinnerView (context: Context) : ImageView(context), Runnable {
  companion object {
    const val CYCLE_DURATION = 1000000L
  }

  @JvmField var started: Boolean = false

  //private Runnable starter;
  private var animation: RotateAnimation? = null

  init {
    scaleType = ImageView.ScaleType.CENTER_INSIDE
  }

  fun start () {
    if (!started && visibility != View.GONE) {
      started = true

      if (Anim.ANIMATORS) {
        startAnimator()
      } else {
        startSimpleAnimation()
      }
    }
  }

  private fun startAnimator () {
    try {
      Views.rotateBy(this, -360000f, CYCLE_DURATION, LINEAR_INTERPOLATOR, null)
    } catch (throwable: Throwable) {
      Log.e("Cannot animate SpinnerView, applying simple Animation", throwable)
      startSimpleAnimation()
    }
  }

  private fun startSimpleAnimation () {
    try {
      val animation = RotateAnimation(0f, -360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
      animation.duration = 1000
      animation.repeatCount = -1
      animation.interpolator = LINEAR_INTERPOLATOR
      animation.fillAfter = true
      this.animation = animation

      startAnimation(animation)
    } catch (t: Throwable) {
      Log.e("Cannot start simple animation on SpinnerView", t)
    }
  }

  fun stop () {
    if (started) {
      started = false
      if (Anim.ANIMATORS) {
        stopAnimator()
      } else {
        stopSimpleAnimation()
      }
    }
  }

  private fun stopAnimator () {
    try {
      Views.clearAnimations(this)
    } catch (throwable: Throwable) {
      Log.e("Cannot cancel pending animator on SpinnerView", throwable)
    }
  }

  private fun stopSimpleAnimation () {
    val animation = this.animation
    if (animation != null) {
      try {
        clearAnimation()
        animation.cancel()
        this.animation = null
      } catch (throwable: Throwable) {
        Log.e("Cannot cancel simple animation in SpinnerView", throwable)
      }
    }
  }

  override fun onAttachedToWindow () {
    super.onAttachedToWindow()
    postDelayed(this, 200L)
  }

  override fun onDetachedFromWindow () {
    super.onDetachedFromWindow()
    stop()
  }

  override fun setVisibility (visibility: Int) {
    super.setVisibility(visibility)
    if (visibility == View.GONE)
      stop()
    else if (visibility == View.VISIBLE)
      start()
  }

  override fun run () {
    start()
  }
}
