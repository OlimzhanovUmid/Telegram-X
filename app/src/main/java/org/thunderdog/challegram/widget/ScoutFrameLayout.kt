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
 * File created on 19/08/2017
 */
package org.thunderdog.challegram.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import me.vkryl.android.widget.FrameLayoutFix
import me.vkryl.core.lambda.Destroyable

class ScoutFrameLayout @JvmOverloads constructor (context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayoutFix(context, attrs, defStyleAttr), AttachDelegate, Destroyable {
  override fun attach () {
    val childCount = childCount
    for (i in 0 until childCount) {
      attachChild(getChildAt(i), true)
    }
  }

  override fun detach () {
    val childCount = childCount
    for (i in 0 until childCount) {
      attachChild(getChildAt(i), false)
    }
  }

  override fun performDestroy () {
    val childCount = childCount
    for (i in 0 until childCount) {
      destroyChild(getChildAt(i))
    }
  }

  companion object {
    private fun destroyChild (view: View?) {
      if (view != null) {
        if (view is Destroyable) {
          view.performDestroy()
        }
        if (view is ViewGroup) {
          val childCount = view.childCount
          for (i in 0 until childCount) {
            destroyChild(view.getChildAt(i))
          }
        }
      }
    }

    private fun attachChild (view: View?, attach: Boolean) {
      if (view != null) {
        if (view is AttachDelegate) {
          if (attach) {
            view.attach()
          } else {
            view.detach()
          }
        }
        if (view is ViewGroup) {
          val childCount = view.childCount
          for (i in 0 until childCount) {
            attachChild(view.getChildAt(i), attach)
          }
        }
      }
    }
  }
}
