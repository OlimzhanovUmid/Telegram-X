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
 * File created on 27/09/2023
 */
package org.thunderdog.challegram.widget.decoration

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.thunderdog.challegram.tool.UI
import org.thunderdog.challegram.util.ScrollJumpCompensator

class ItemDecorationFirstViewTop private constructor (
  private val recyclerView: RecyclerView,
  private val linearLayoutManager: LinearLayoutManager,
  private val callback: Callback
) : RecyclerView.ItemDecoration() {
  private val scrollListener: RecyclerView.OnScrollListener = object : RecyclerView.OnScrollListener() {
    override fun onScrollStateChanged (recyclerView: RecyclerView, newState: Int) {
      if (newState == RecyclerView.SCROLL_STATE_IDLE) {
        UI.post { checkCanDisableDecorationOffset() }
      }
    }
  }
  private var isScheduledDecorationOffsetDisable = false
  private var isDecorationOffsetDisabled = false
  private var lastTopDecorationOffset = 0

  interface Callback {
    fun getTopDecorationOffset (): Int
  }

  fun scheduleDisableDecorationOffset () {
    this.isScheduledDecorationOffsetDisable = true
    checkCanDisableDecorationOffset()
  }

  fun enableDecorationOffset () {
    setDecorationOffsetDisabled(false)
  }

  /* * */

  private fun checkCanDisableDecorationOffset () {
    if (isScheduledDecorationOffsetDisable && !isDecorationOffsetDisabled) {
      val v = linearLayoutManager.findViewByPosition(0)
      if (v == null || v.top <= 0) {
        setDecorationOffsetDisabled(true)
      } else {
        recyclerView.smoothScrollBy(0, v.top)
      }
    }
  }

  private fun setDecorationOffsetDisabled (disabled: Boolean) {
    if (isDecorationOffsetDisabled == disabled) return
    isDecorationOffsetDisabled = disabled
    isScheduledDecorationOffsetDisable = isScheduledDecorationOffsetDisable && disabled

    val firstVisibleItemPosition = linearLayoutManager.findFirstVisibleItemPosition()
    val offset = lastTopDecorationOffset * (if (disabled) -1 else 1)

    // Changing the height of the first view can be animated, this leads to unwanted behavior.
    // Temporarily disable animation.

    val itemAnimator = recyclerView.itemAnimator
    recyclerView.itemAnimator = null

    recyclerView.invalidateItemDecorations()
    if (firstVisibleItemPosition == 0 && offset != 0) {
      ScrollJumpCompensator.compensate(recyclerView, offset)
    }

    if (itemAnimator != null) {
      UI.post {
        if (recyclerView.itemAnimator == null) {
          recyclerView.itemAnimator = itemAnimator
        }
      }
    }
  }

  override fun getItemOffsets (outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
    val position = parent.getChildAdapterPosition(view)
    val isUnknown = position == RecyclerView.NO_POSITION
    var top = 0
    if (position == 0 || isUnknown) {
      top = callback.getTopDecorationOffset()
      lastTopDecorationOffset = top
    }

    outRect.set(0, if (isDecorationOffsetDisabled) 0 else top, 0, 0)
  }

  companion object {
    @JvmStatic
    fun attach (recyclerView: RecyclerView, callback: Callback): ItemDecorationFirstViewTop {
      val decoration = ItemDecorationFirstViewTop(recyclerView, recyclerView.layoutManager as LinearLayoutManager, callback)
      recyclerView.addItemDecoration(decoration)
      recyclerView.addOnScrollListener(decoration.scrollListener)
      return decoration
    }
  }
}
