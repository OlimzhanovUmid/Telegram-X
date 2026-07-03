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
 * File created on 14/06/2024
 */
package org.thunderdog.challegram.util.text.counter

import android.graphics.Canvas
import android.graphics.drawable.Drawable

import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.tool.Drawables
import org.thunderdog.challegram.tool.Paints
import org.thunderdog.challegram.tool.getPorterDuffPaint
import org.thunderdog.challegram.util.text.Counter
import org.thunderdog.challegram.util.text.TextColorSet

import java.util.HashMap

import me.vkryl.core.alphaColor

abstract class CounterDrawableSet : Counter.CustomTextPartBuilder {
  private val set = HashMap<String, CharDrawable>()

  abstract fun onCreateTextDrawableImpl (text: String): CharDrawable

  override fun onCreateTextDrawable (text: String): CounterTextPart {
    var part = set[text]

    if (part == null) {
      part = onCreateTextDrawableImpl(text)
      set[text] = part
    }

    return part
  }

  class CharDrawable (val drawable: Drawable, private val text: String) : CounterTextPart {
    private var gap: Int = 0

    fun setGap (gap: Int): CharDrawable {
      this.gap = gap
      return this
    }

    override fun draw (c: Canvas, startX: Int, endX: Int, endXBottomPadding: Int, startY: Int, defaultTheme: TextColorSet?, alpha: Float) {
      Drawables.draw(c, drawable, (startX + gap).toFloat(), startY.toFloat(), if (defaultTheme != null)
        Paints.getPorterDuffPaint(alphaColor(alpha, defaultTheme.defaultTextColor()))
      else
        getPorterDuffPaint(ColorId.text, alpha)
      )
    }

    override fun getWidth (): Int {
      return drawable.minimumWidth + gap
    }

    override fun getHeight (): Int {
      return drawable.minimumHeight
    }

    override fun getText (): String {
      return text
    }
  }
}
