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
 * File created on 11/01/2018
 */
package org.thunderdog.challegram.widget

import android.content.Context
import android.graphics.Canvas
import android.view.View
import org.thunderdog.challegram.tool.Paints
import org.thunderdog.challegram.tool.Screen
import me.vkryl.core.color

class ViewPagerPositionView (context: Context) : View(context) {
  private var itemCount: Int = 0

  fun reset (count: Int, factor: Float) {
    if (this.itemCount != count || this.positionFactor != factor) {
      this.itemCount = count
      this.positionFactor = factor
      invalidate()
    }
  }

  private var positionFactor: Float = 0f

  fun setPositionFactor (factor: Float) {
    if (this.positionFactor != factor) {
      this.positionFactor = factor
      invalidate()
    }
  }

  override fun onDraw (c: Canvas) {
    val viewWidth = measuredWidth
    val viewHeight = measuredHeight

    val spacing = Screen.dp(12f)

    val cy = viewHeight / 2
    var cx = viewWidth / 2 - (spacing * (itemCount / 2))

    for (i in 0 until itemCount) {
      var factor = 1f - Math.abs(positionFactor - i)
      if (factor > 1f || factor < 0f) {
        factor = 0f
      }
      c.drawCircle(cx.toFloat(), cy.toFloat(), Screen.dp(2f).toFloat(), Paints.fillingPaint(color((255f * (.6f + .4f * factor)).toInt(), 0xffffff)))
      cx += spacing
    }
  }
}
