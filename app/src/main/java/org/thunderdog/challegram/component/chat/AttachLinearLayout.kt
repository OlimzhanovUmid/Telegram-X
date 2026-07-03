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
 * File created on 14/03/2016 at 00:29
 */
package org.thunderdog.challegram.component.chat

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import org.thunderdog.challegram.tool.Views

open class AttachLinearLayout (context: Context) : LinearLayout(context) {
  /*private fun calculateWidth (): Int {
    var width = 0
    for (i in 0 until childCount) {
      val v = getChildAt(i)
      if (v.visibility == View.VISIBLE) {
        width += v.layoutParams.width
      }
    }
    return width
  }*/

  override fun onInterceptTouchEvent (ev: MotionEvent): Boolean {
    return (ev.action == MotionEvent.ACTION_DOWN && !Views.isValid(this)) || super.onInterceptTouchEvent(ev)
  }

  fun updatePivot () {
    var totalWidth = 0
    var width = 0

    //int itemWidth = 0;
    var itemHeight = 0

    for (i in 0 until childCount) {
      val v = getChildAt(i)
      val params = v.layoutParams
      val w = params.width
      if (itemHeight == 0) {
        //itemWidth = params.width;
        itemHeight = params.height
      }
      totalWidth += w
      if (v.visibility == View.VISIBLE) {
        width += w
      }
    }

    pivotX = (totalWidth - (width * .5f).toInt()).toFloat()
    pivotY = (itemHeight * .5f).toInt().toFloat()
  }

  fun getVisibleChildrenWidth (): Int {
    var totalWidth = 0
    val count = childCount
    for (i in 0 until count) {
      val v = getChildAt(i)
      if (v.visibility == View.VISIBLE) {
        val width = v.layoutParams.width
        totalWidth += width
      }
    }
    return totalWidth
  }
}
