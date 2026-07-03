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
 * File created on 23/04/2015 at 17:44
 */
package org.thunderdog.challegram.navigation

import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent

import org.thunderdog.challegram.BaseActivity
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.UI

import me.vkryl.android.animator.FactorAnimator
import me.vkryl.android.widget.FrameLayoutFix

class InterceptLayout (context: Context) : FrameLayoutFix(context), FactorAnimator.StartHelper {
  init {
    setOnTouchListener(context as BaseActivity)
  }

  fun isBlocked (): Boolean {
    val context = UI.getUiContext()
    return context != null && context.gestureController != null && context.gestureController.isDispatching
  }

  override fun onTouchEvent (event: MotionEvent): Boolean {
    return super.onTouchEvent(event) || event.action == MotionEvent.ACTION_DOWN
  }

  override fun draw (c: Canvas) {
    if ((context as BaseActivity).isPasscodeShowing) {
      c.drawColor(Theme.headerColor())
    } else {
      super.draw(c)
    }
  }

  private var disallowIntercept = false

  /*override fun requestDisallowInterceptTouchEvent (disallowIntercept: Boolean) {
    this.disallowIntercept = disallowIntercept
    super.requestDisallowInterceptTouchEvent(disallowIntercept)
  }*/

  public override fun onInterceptTouchEvent (event: MotionEvent): Boolean {
    if (disallowIntercept) {
      return false
    }

    val context = context as BaseActivity

    if (isBlocked() || context.isAnimating(true)) {
      return true
    }

    val drawer = context.drawer
    if (drawer != null && drawer.isVisible && event.action == MotionEvent.ACTION_DOWN) {
      if (Lang.rtl()) {
        if (event.x < measuredWidth - drawer.width + drawer.shadowWidth) {
          context.processTouchEvent(event)
          return true
        }
      } else {
        if (event.x >= drawer.width) {
          context.processTouchEvent(event)
          return true
        }
      }
    }

    return context.processTouchEvent(event) && event.action != MotionEvent.ACTION_DOWN
  }

  private var scheduledAnimator: FactorAnimator? = null
  private var scheduledAnimationFactor = 0f

  override fun startAnimatorOnLayout (animator: FactorAnimator, toFactor: Float) {
    scheduledAnimator = animator
    scheduledAnimationFactor = toFactor
  }

  override fun onLayout (changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
    super.onLayout(changed, left, top, right, bottom)
    val animator = scheduledAnimator
    if (animator != null) {
      animator.animateTo(scheduledAnimationFactor)
      scheduledAnimator = null
    }
  }

  override fun onMeasure (widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    UI.getContext(context).applyContentTranslation(this)
  }
}
