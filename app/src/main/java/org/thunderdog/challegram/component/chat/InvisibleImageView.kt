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
 * File created on 14/03/2016 at 00:27
 */
package org.thunderdog.challegram.component.chat

import android.content.Context
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import org.thunderdog.challegram.navigation.TooltipOverlayView
import org.thunderdog.challegram.tool.Screen

open class InvisibleImageView (context: Context) : ImageView(context), TooltipOverlayView.LocationProvider {
  override fun onTouchEvent (e: MotionEvent): Boolean {
    return visibility == View.VISIBLE && super.onTouchEvent(e)
  }

  fun setVisible (visible: Boolean): Boolean {
    val isVisible = visibility == VISIBLE
    if (isVisible != visible) {
      visibility = if (visible) VISIBLE else INVISIBLE
      return true
    }
    return false
  }

  fun isVisible (): Boolean {
    return visibility == VISIBLE
  }

  override fun getTargetBounds (targetView: View?, outRect: Rect?) {
    outRect?.apply {
      top += Screen.dp(8f)
      bottom -= Screen.dp(8f)
    }
  }
}
