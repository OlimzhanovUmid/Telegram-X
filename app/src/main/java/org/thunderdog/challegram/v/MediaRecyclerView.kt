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
 * File created on 27/12/2016
 */
package org.thunderdog.challegram.v

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MediaRecyclerView @JvmOverloads constructor (context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : RecyclerView(context, attrs, defStyle) {
  fun interface MeasureCallback {
    fun onRecyclerMeasure (recyclerView: MediaRecyclerView, width: Int, height: Int)
  }

  private var measureCallback: MeasureCallback? = null

  fun setMeasureCallback (callback: MeasureCallback?) {
    this.measureCallback = callback
  }

  init {
    init()
  }

  override fun onMeasure (widthSpec: Int, heightSpec: Int) {
    super.onMeasure(widthSpec, heightSpec)
    val callback = measureCallback
    if (callback != null && Math.max(measuredWidth, measuredHeight) > 0) {
      callback.onRecyclerMeasure(this, measuredWidth, measuredHeight)
    }
  }

  override fun getVerticalScrollbarPosition (): Int {
    val manager = layoutManager as LinearLayoutManager
    if (manager.findFirstCompletelyVisibleItemPosition() == 0) {
      val view: View? = manager.findViewByPosition(0)
      if (view != null) {
        return Math.max(super.getVerticalScrollbarPosition(), view.top)
      }
    }
    return super.getVerticalScrollbarPosition()
  }

  private fun init () {
    isVerticalScrollBarEnabled = false
    addOnScrollListener(object : OnScrollListener() {
      override fun onScrolled (recyclerView: RecyclerView, dx: Int, dy: Int) {
        val manager = layoutManager as LinearLayoutManager
        setScrollbarsVisible(manager.findFirstVisibleItemPosition() != 0)
      }
    })
  }

  private var scrollbarsVisible = false

  private fun setScrollbarsVisible (visible: Boolean) {
    if (this.scrollbarsVisible != visible) {
      this.scrollbarsVisible = visible
      isVerticalScrollBarEnabled = visible
      if (visible) {
        awakenScrollBars()
      }
    }
  }
}
