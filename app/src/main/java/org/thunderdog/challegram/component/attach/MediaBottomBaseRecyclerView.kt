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
 * File created on 23/10/2016
 */
package org.thunderdog.challegram.component.attach

import android.content.Context
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView

class MediaBottomBaseRecyclerView (context: Context) : RecyclerView(context) {
  private var isDown = false

  override fun onInterceptTouchEvent (e: MotionEvent): Boolean {
    when (e.action) {
      MotionEvent.ACTION_DOWN -> {
        isDown = true
      }
      MotionEvent.ACTION_UP -> {
        isDown = false
      }
    }
    return super.onInterceptTouchEvent(e)
  }

  override fun onTouchEvent (e: MotionEvent): Boolean {
    when (e.action) {
      MotionEvent.ACTION_DOWN -> {
        isDown = true
      }
      MotionEvent.ACTION_UP -> {
        isDown = false
      }
    }
    return e.action != MotionEvent.ACTION_CANCEL && super.onTouchEvent(e)
  }

  fun processEvent (e: MotionEvent) {
    if (isDown) {
      dispatchTouchEvent(e)
    }
  }
}
