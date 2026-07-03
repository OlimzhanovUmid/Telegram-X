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
 * File created on 30/06/2024
 */
package org.thunderdog.challegram.player

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable

import org.thunderdog.challegram.R
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Drawables
import org.thunderdog.challegram.tool.Paints
import org.thunderdog.challegram.tool.getPorterDuffPaint

import me.vkryl.core.fromToArgb

class RecordDisposableSwitchButton (context: Context) : RecordControllerButton (context) {
  private val drawable: Drawable? = Drawables.get(context.resources, R.drawable.baseline_hot_once_24)

  protected override fun dispatchDraw (canvas: Canvas) {
    super.dispatchDraw(canvas)

    val cx = measuredWidth / 2f
    val cy = measuredHeight / 2f
    val active = getActiveFactor()

    if (active == 0f) {
      Drawables.drawCentered(canvas, drawable, cx, cy, getPorterDuffPaint(ColorId.icon))
    } else if (active == 1f) {
      Drawables.drawCentered(canvas, drawable, cx, cy, getPorterDuffPaint(ColorId.fillingPositiveContent))
    } else {
      Drawables.drawCentered(canvas, drawable, cx, cy, Paints.getPorterDuffPaint(
        fromToArgb(Theme.getColor(ColorId.icon), Theme.getColor(ColorId.fillingPositiveContent), active)
      ))
    }
  }
}
