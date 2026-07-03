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
 * File created on 01/09/2022, 04:22.
 */

package org.thunderdog.challegram.widget

import android.content.Context
import android.util.AttributeSet
import org.thunderdog.challegram.tool.UI
import org.thunderdog.challegram.tool.Views
import org.thunderdog.challegram.util.TextSelection

open class TextView @JvmOverloads constructor (context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : android.widget.TextView(context, attrs, defStyleAttr) {
  private var scrollDisabled = false
  private var selection: TextSelection? = null

  fun getTextSelection (): TextSelection? {
    if (!UI.inUiThread()) {
      throw IllegalStateException()
    }
    if (selection == null) {
      selection = TextSelection()
    }
    return if (Views.getSelection(this, selection)) {
      selection
    } else {
      null
    }
  }

  fun setScrollDisabled (scrollDisabled: Boolean) {
    this.scrollDisabled = scrollDisabled
  }

  override fun canScrollHorizontally (direction: Int): Boolean {
    return if (scrollDisabled) {
      false
    } else {
      super.canScrollHorizontally(direction)
    }
  }

  override fun canScrollVertically (direction: Int): Boolean {
    return if (scrollDisabled) {
      false
    } else {
      super.canScrollVertically(direction)
    }
  }
}
