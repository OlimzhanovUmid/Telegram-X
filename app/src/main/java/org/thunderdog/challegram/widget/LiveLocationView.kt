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
 * File created on 20/03/2018
 */
package org.thunderdog.challegram.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup

import org.thunderdog.challegram.R
import org.thunderdog.challegram.tool.DrawAlgorithms
import org.thunderdog.challegram.tool.Drawables
import org.thunderdog.challegram.tool.Paints
import org.thunderdog.challegram.tool.Screen

import me.vkryl.core.lambda.Destroyable

class LiveLocationView (context: Context) : View(context), Destroyable {
  private var liveLocationBmp: Drawable? = Drawables.get(resources, R.drawable.baseline_location_on_48)

  init {
    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Screen.dp(132f))
  }

  override fun performDestroy () {
    if (liveLocationBmp != null) {
      liveLocationBmp = null
    }
  }

  private var nextScheduleTime: Long = 0

  override fun onDraw (c: Canvas) {
    val liveLocationBmp = liveLocationBmp ?: return
    val cx = measuredWidth / 2
    val cy = measuredHeight / 2
    Drawables.draw(c, liveLocationBmp, (cx - liveLocationBmp.minimumWidth / 2).toFloat(), (cy - liveLocationBmp.minimumHeight / 2).toFloat(), Paints.whitePorterDuffPaint())
    val delay = DrawAlgorithms.drawWaves(c, cx.toFloat(), (cy - Screen.dp(4f)).toFloat(), 0xffffffff.toInt(), true, nextScheduleTime)
    if (delay != -1L) {
      nextScheduleTime = SystemClock.uptimeMillis() + delay
      postInvalidateDelayed(delay)
    }
  }
}
