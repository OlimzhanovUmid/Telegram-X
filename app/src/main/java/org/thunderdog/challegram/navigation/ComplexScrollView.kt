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
 * File created on 10/08/2015 at 11:14
 */
package org.thunderdog.challegram.navigation

import android.content.Context
import android.widget.ScrollView

import org.thunderdog.challegram.tool.UI
import org.thunderdog.challegram.unsorted.Size

@Deprecated("")
open class ComplexScrollView (context: Context) : ScrollView(context) {
  private var headerView: ComplexHeaderView? = null
  private var floatingButton: FloatingButton? = null

  var scrollFactor: Float = 1f
    private set
  private var factorLocked = true

  init {
    isVerticalScrollBarEnabled = false
  }

  fun setFactorLocked (locked: Boolean) {
    this.factorLocked = locked
  }

  fun setFloatingButton (floatingButton: FloatingButton?) {
    this.floatingButton = floatingButton
  }

  fun setHeaderView (headerView: ComplexHeaderView?) {
    this.headerView = headerView
  }

  override fun onScrollChanged (left: Int, top: Int, oldLeft: Int, oldTop: Int) {
    super.onScrollChanged(left, top, oldLeft, oldTop)
    val headerView = this.headerView
    if (headerView != null && !factorLocked) {
      val factor = 1f - top.toFloat() / Size.getMaximumHeaderSizeDifference().toFloat()
      val navigationHeaderView = UI.getHeaderView()
      if (factor >= 1f) {
        scrollFactor = 1f
        headerView.setScaleFactor(1f, 1f, 1f, true)
        navigationHeaderView?.setBackgroundHeight(Size.getMaximumHeaderSizeDifference())
      } else if (factor <= 0f) {
        scrollFactor = 0f
        headerView.setScaleFactor(0f, 0f, 0f, true)
        navigationHeaderView?.setBackgroundHeight(Size.getHeaderPortraitSize())
      } else {
        scrollFactor = factor
        headerView.setScaleFactor(factor, factor, factor, true)
        navigationHeaderView?.setBackgroundHeight(Size.getHeaderPortraitSize() + (Size.getMaximumHeaderSizeDifference() * factor).toInt())
      }

      floatingButton?.setHeightFactor(scrollFactor, 0f, true)
    }
  }
}
