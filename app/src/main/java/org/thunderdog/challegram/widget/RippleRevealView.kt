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
 * File created on 28/10/2017
 */
package org.thunderdog.challegram.widget

import android.content.Context
import android.graphics.Canvas
import android.view.View
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Paints
import me.vkryl.core.alphaColor

class RippleRevealView (context: Context) : View(context) {
  private @ColorId var colorId: Int = ColorId.filling
  private var revealFactor: Float = 0f

  fun setRevealFactor (factor: Float) {
    if (this.revealFactor != factor) {
      this.revealFactor = factor
      invalidate()
    }
  }

  override fun onDraw (c: Canvas) {
    if (revealFactor > 0f) {
      val color = Theme.getColor(colorId)
      c.drawColor(alphaColor(revealFactor, color))

      val width = measuredWidth.toFloat()
      val height = (measuredHeight - paddingBottom - paddingTop).toFloat()
      val radius = Math.sqrt((width * width + height * height).toDouble()).toFloat() * .5f
      c.drawCircle(width / 2f, paddingTop + height / 2f, radius * revealFactor, Paints.fillingPaint(color))
    }
  }
}
