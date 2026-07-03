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
 * File created on 26/01/2017
 */
package org.thunderdog.challegram.support

import android.graphics.Canvas
import org.thunderdog.challegram.tool.Paints
import org.thunderdog.challegram.tool.Screen

class CircleDrawable (colorId: Int, size: Float, isPressed: Boolean) : SimpleShapeDrawable(colorId, size, isPressed) {
  override fun draw (c: Canvas) {
    val rect = bounds
    val centerX = rect.centerX()
    val centerY = rect.centerY()
    val radius = Screen.dp(size) / 2

    val color = getDrawColor()
    if (color != 0) {
      c.drawCircle(centerX.toFloat(), centerY.toFloat(), radius.toFloat(), Paints.fillingPaint(color))
    }
  }
}
