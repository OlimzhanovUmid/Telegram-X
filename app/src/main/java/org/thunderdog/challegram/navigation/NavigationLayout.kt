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
 * File created on 23/04/2015 at 19:07
 */
package org.thunderdog.challegram.navigation

import android.content.Context
import android.graphics.Rect

import org.thunderdog.challegram.tool.Views
import org.thunderdog.challegram.widget.RootFrameLayout

import me.vkryl.android.widget.FrameLayoutFix
import me.vkryl.core.lambda.Destroyable

class NavigationLayout (context: Context) : FrameLayoutFix(context), Destroyable, RootFrameLayout.InsetsChangeListener {
  private var rootView: RootFrameLayout? = null

  protected override fun onAttachedToWindow () {
    super.onAttachedToWindow()
    rootView = Views.findAncestor(this, RootFrameLayout::class.java, true)
    rootView?.let {
      it.addInsetsChangeListener(this)
      applyTopInset(it.topInset)
    }
  }

  protected override fun onDetachedFromWindow () {
    super.onDetachedFromWindow()
    rootView?.let {
      it.removeInsetsChangeListener(this)
      rootView = null
    }
  }

  override fun onInsetsChanged (viewGroup: RootFrameLayout, effectiveInsets: Rect, effectiveInsetsWithoutIme: Rect, systemInsets: Rect, systemInsetsWithoutIme: Rect, isUpdate: Boolean) {
    applyTopInset(effectiveInsets.top)
  }

  private fun applyTopInset (topInset: Int) {
    val newSize = HeaderView.getSize(false) + topInset
    if (newSize != paddingTop) {
      setPadding(0, newSize, 0, 0)
    }
  }

  override fun performDestroy () {
    rootView?.let {
      it.removeInsetsChangeListener(this)
      rootView = null
    }
  }

  /* optimization */

  private var layoutPrevented = false
  private var layoutRequestedFlag = false

  fun preventLayout () {
    layoutPrevented = true
  }

  fun layoutIfRequested () {
    layoutPrevented = false
    if (layoutRequestedFlag) {
      layoutRequestedFlag = false
      requestLayout()
    }
  }

  fun cancelLayout () {
    layoutPrevented = false
    layoutRequestedFlag = false
  }

  override fun isLayoutRequested (): Boolean {
    return layoutRequestedFlag
  }

  override fun requestLayout () {
    if (!layoutPrevented) {
      if (layoutLimit == -1) {
        super.requestLayout()
      } else if (layoutComplete < layoutLimit) {
        layoutComplete++
        super.requestLayout()
      }
    } else {
      layoutRequestedFlag = true
    }
  }

  private var layoutLimit = -1
  private var layoutComplete = 0

  fun preventNextLayouts (limit: Int) {
    layoutLimit = limit
    layoutComplete = 0
  }

  fun completeNextLayout () {
    layoutLimit = -1
    layoutComplete = 0
  }

  /* optimization end */

  fun setController (controller: NavigationController) {
    // NavigationController controller1 = controller;
  }
}
