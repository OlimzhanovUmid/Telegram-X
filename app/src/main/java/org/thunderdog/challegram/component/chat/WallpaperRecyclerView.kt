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
 * File created on 07/11/2018
 */
package org.thunderdog.challegram.component.chat

import android.content.Context
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView

class WallpaperRecyclerView (context: Context) : RecyclerView(context) {
  init {
    addOnScrollListener(object : OnScrollListener() {
      override fun onScrollStateChanged (recyclerView: RecyclerView, newState: Int) {
        when (newState) {
          SCROLL_STATE_DRAGGING, SCROLL_STATE_IDLE -> smoothRequested = false
        }
      }
    })
  }

  private var smoothRequested = false

  override fun smoothScrollBy (dx: Int, dy: Int) {
    smoothRequested = dx != 0
    super.smoothScrollBy(dx, dy)
  }

  override fun onInterceptTouchEvent (e: MotionEvent): Boolean {
    if (smoothRequested && e.action == MotionEvent.ACTION_DOWN) {
      stopScroll() // Hack
    }
    return super.onInterceptTouchEvent(e)
  }

  @JvmField var lastWidth: Int = 0

  override fun onMeasure (widthSpec: Int, heightSpec: Int) {
    super.onMeasure(widthSpec, heightSpec)
    if (lastWidth != measuredWidth) {
      lastWidth = measuredWidth
      (adapter as WallpaperAdapter).centerWallpapers(false)
    }
  }
}
