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
 */
package org.thunderdog.challegram.component.chat

import android.content.Context
import android.graphics.Canvas
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.tool.Views
import org.thunderdog.challegram.util.text.Counter

class CounterBadgeView (context: Context) : View(context) {
  init {
    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    minimumHeight = Screen.dp(28f)
  }

  // Count

  private val counter = Counter.Builder().callback(object : Counter.Callback {
    override fun onCounterAppearanceChanged (counter: Counter, sizeChanged: Boolean) {
      if (sizeChanged) {
        invalidateOutline()
      }
      invalidate()
    }

    override fun needAnimateChanges (counter: Counter): Boolean {
      return Views.isValid(this@CounterBadgeView)
    }
  }).build()

  override fun onMeasure (widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    pivotX = (paddingLeft + (measuredWidth - paddingLeft - paddingRight)).toFloat()
    pivotY = (paddingTop + (measuredHeight - paddingTop - paddingBottom)).toFloat()
  }

  fun setCounter (count: Int, isMuted: Boolean, animated: Boolean) {
    counter.setCount(count.toLong(), isMuted, animated)
  }

  // Drawing

  override fun onDraw (c: Canvas) {
    val paddingLeft = paddingLeft
    val paddingRight = paddingRight
    val x = paddingLeft + (measuredWidth - paddingLeft - paddingRight) / 2
    val y = measuredHeight / 2
    counter.draw(c, x.toFloat(), y.toFloat(), Gravity.CENTER_HORIZONTAL, 1f)
  }
}
