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
 * File created on 19/01/2024
 */
package org.thunderdog.challegram.util.text

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.graphics.drawable.ScaleDrawable
import android.text.style.DynamicDrawableSpan
import android.view.Gravity

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.FloatRange

import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.theme.PorterDuffColorId
import org.thunderdog.challegram.tool.Drawables
import org.thunderdog.challegram.tool.getPorterDuffPaint

import me.vkryl.core.clamp

class IconSpan (@DrawableRes iconRes: Int, @PorterDuffColorId private val iconColorId: Int) : DynamicDrawableSpan(DynamicDrawableSpan.ALIGN_BOTTOM), TextReplacementSpan {

  private val drawable: ScaleDrawable = ScaleDrawable(Drawables.get(iconRes), Gravity.CENTER, 1.0f, 1.0f)
  private var overrideColor: Int = 0
  private var useOverrideColor: Boolean = false

  init {
    drawable.setLevel(MAX_LEVEL)
    drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
  }

  override fun getDrawable (): Drawable {
    return drawable
  }

  fun setAlpha (@FloatRange(from = 0.0, to = 1.0) alpha: Float) {
    Drawables.setAlpha(drawable, clamp((0xFF * alpha).toInt(), 0x00, 0xFF))
  }

  fun setScale (@FloatRange(from = 0.0, to = 1.0) scale: Float) {
    drawable.setLevel(clamp((scale * MAX_LEVEL).toInt(), 0, MAX_LEVEL))
  }

  fun setOverrideColor (@ColorInt color: Int) {
    useOverrideColor = true
    overrideColor = color
  }

  fun resetOverrideColor () {
    useOverrideColor = false
    overrideColor = Color.MAGENTA
  }

  override fun draw (canvas: Canvas, text: CharSequence, start: Int, end: Int, x: Float, top: Int, y: Int, bottom: Int, paint: Paint) {
    if (useOverrideColor) {
      drawable.setColorFilter(overrideColor, PorterDuff.Mode.SRC_IN)
    } else if (iconColorId != ColorId.NONE) {
      drawable.setColorFilter(getPorterDuffPaint(iconColorId).colorFilter)
    } else {
      drawable.setColorFilter(paint.color, PorterDuff.Mode.SRC_IN)
    }
    super.draw(canvas, text, start, end, x, top, y, bottom, paint)
  }

  override fun getSize (paint: Paint, text: CharSequence, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int {
    if (fm != null) {
      paint.getFontMetricsInt(fm)
    }
    return getRawSize(paint)
  }

  override fun getRawSize (paint: Paint): Int {
    return drawable.intrinsicWidth
  }

  companion object {
    private const val MAX_LEVEL = 10000
  }
}
