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
 * File created on 01/09/2015 at 14:03
 */
package org.thunderdog.challegram.navigation

import android.content.Context

import androidx.recyclerview.widget.RecyclerView

import org.thunderdog.challegram.unsorted.Size
import org.thunderdog.challegram.v.CustomRecyclerView

import me.vkryl.core.clamp

open class ComplexRecyclerView (context: Context, private var target: ViewController<*>) : CustomRecyclerView(context), Runnable {
  private var headerView: StretchyHeaderView? = null
  private var floatingButton: FloatingButton? = null

  private var scrollFactor: Float
  private var factorLocked: Boolean
  private var totalY: Int = 0

  private var flags = 0

  init {
    scrollFactor = getMaxFactor()
    factorLocked = true
    setHasFixedSize(true)
    isVerticalScrollBarEnabled = false

    addOnScrollListener(object : RecyclerView.OnScrollListener() {
      override fun onScrolled (recyclerView: RecyclerView, dx: Int, dy: Int) {
        totalY += dy
        if (headerView != null && !factorLocked) {
          updateScrollFactor(true)
        }
      }
    })
  }

  fun setFloatingButton (floatingButton: FloatingButton?) {
    this.floatingButton = floatingButton
  }

  fun setFactorLocked (locked: Boolean) {
    this.factorLocked = locked
  }

  fun setHeaderView (headerView: StretchyHeaderView, target: ViewController<*>) {
    this.headerView = headerView
    this.target = target
  }

  private fun getMaxFactor (): Float {
    val maxHeight = Size.getHeaderBigPortraitSize(true)
    val targetMaxHeight = target.getMaximumHeaderHeight()
    return 1f - (maxHeight - targetMaxHeight).toFloat() / Size.getHeaderSizeDifference(true).toFloat()
  }

  fun getScrollFactor (): Float {
    if (childCount == 0) {
      return getMaxFactor()
    }
    if (headerView == null || factorLocked) {
      return scrollFactor
    }
    updateScrollFactor(false)
    return scrollFactor
  }

  override fun onLayout (changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    super.onLayout(changed, l, t, r, b)
    if (changed && !factorLocked && (flags and FLAG_REBUILD) != 0) {
      flags = flags and FLAG_REBUILD.inv()
      updateScrollFactor(true)
    }
  }

  fun rebuildTop () {
    flags = flags or FLAG_REBUILD
  }

  fun postUpdate () {
    post(this)
  }

  fun forceUpdate () {
    flags = flags or FLAG_FORCE
    updateScrollFactor(false)
    flags = flags and FLAG_FORCE.inv()
  }

  override fun run () {
    updateScrollFactor(true)
  }

  private fun updateScrollFactor (updateFilling: Boolean) {
    if (target.inCustomMode() && (flags and FLAG_FORCE) == 0) {
      return
    }
    val view = layoutManager!!.findViewByPosition(0)
    val diff = Size.getHeaderBigPortraitSize(true) - target.getMaximumHeaderHeight()
    val t = if (view == null) Size.getHeaderSizeDifference(true).toFloat() else (-view.top + diff).toFloat()
    val factor = 1f - t / Size.getHeaderSizeDifference(true).toFloat()
    scrollFactor = clamp(factor)
    if ((flags and FLAG_FORCE) == 0) {
      headerView!!.setScaleFactor(scrollFactor, scrollFactor, scrollFactor, true)
      val button = floatingButton
      if (button != null && target.getFloatingButtonId() != 0) {
        button.setHeightFactor(scrollFactor, 0f, true)
      }
      val targetHeaderView = target.headerView
      if (updateFilling && targetHeaderView != null) {
        if (scrollFactor == 1f) {
          targetHeaderView.setBackgroundHeight(Size.getHeaderBigPortraitSize(true))
        } else if (scrollFactor == 0f) {
          targetHeaderView.setBackgroundHeight(Size.getHeaderPortraitSize())
        } else {
          targetHeaderView.setBackgroundHeight(Size.getHeaderPortraitSize() + (Size.getHeaderSizeDifference(true) * scrollFactor).toInt())
        }
      }
    }
  }

  companion object {
    private const val FLAG_REBUILD = 0x01
    private const val FLAG_FORCE = 0x02
  }
}
