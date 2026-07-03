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
 * File created on 17/08/2015 at 23:11
 */
package org.thunderdog.challegram.navigation

import android.content.Context

import me.vkryl.android.widget.FrameLayoutFix

class RootLayout (context: Context) : FrameLayoutFix(context) {
  /* optimization */

  private var preventLayout = false
  private var layoutRequested = false

  fun preventLayout () {
    preventLayout = true
  }

  fun layoutIfRequested () {
    preventLayout = false
    if (layoutRequested) {
      layoutRequested = false
      requestLayout()
    }
  }

  fun cancelLayout () {
    preventLayout = false
    layoutRequested = false
  }

  override fun isLayoutRequested (): Boolean {
    return layoutRequested
  }

  override fun requestLayout () {
    if (!preventLayout) {
      if (layoutLimit == -1) {
        super.requestLayout()
      } else if (layoutComplete < layoutLimit) {
        layoutComplete++
        super.requestLayout()
      }
    } else {
      layoutRequested = true
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
}
