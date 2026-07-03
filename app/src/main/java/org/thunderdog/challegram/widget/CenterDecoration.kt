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
 */
package org.thunderdog.challegram.widget

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.tool.Screen

abstract class CenterDecoration : RecyclerView.ItemDecoration() {
  abstract fun getItemCount (): Int

  override fun getItemOffsets (outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
    val itemCount = getItemCount()
    val adapterPosition = parent.getChildAdapterPosition(view)
    if (itemCount == 0 || adapterPosition == RecyclerView.NO_POSITION) {
      outRect.right = 0
      outRect.left = outRect.right
      return
    }
    val itemWidth = Screen.dp(72f)
    val totalWidth = itemWidth * itemCount
    val parentWidth = parent.measuredWidth
    val emptyWidth = parentWidth - totalWidth
    if (emptyWidth <= 0) {
      outRect.right = 0
      outRect.left = outRect.right
      return
    }
    var left = emptyWidth / (itemCount + 2)
    if (adapterPosition == 0) {
      left += left / 2
    }
    if (Lang.rtl()) {
      outRect.right = left
      outRect.left = 0
    } else {
      outRect.left = left
      outRect.right = 0
    }
  }
}
