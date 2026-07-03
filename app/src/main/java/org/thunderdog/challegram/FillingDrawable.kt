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
 * File created on 22/01/2017
 */
package org.thunderdog.challegram

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.view.View
import me.vkryl.core.alphaColor
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.theme.ThemeDelegate
import org.thunderdog.challegram.tool.Paints
import org.thunderdog.challegram.tool.Screen

open class FillingDrawable @JvmOverloads constructor (@field:ColorId private var colorId: Int, private var cornerRadius: Float = 0f) : Drawable() {
  private var forcedTheme: ThemeDelegate? = null

  private var alpha = 1f

  fun setForcedTheme (forcedTheme: ThemeDelegate?) {
    if (this.forcedTheme != forcedTheme) {
      this.forcedTheme = forcedTheme
      invalidateSelf()
    }
  }

  open fun setCornerRadius (radius: Float) {
    if (this.cornerRadius != radius) {
      this.cornerRadius = radius
      invalidateSelf()
    }
  }

  @ColorId
  fun getColorId (): Int {
    return colorId
  }

  fun setColorId (@ColorId colorId: Int) {
    if (this.colorId != colorId) {
      this.colorId = colorId
      invalidateSelf()
    }
  }

  open fun setAlphaFactor (alpha: Float) {
    if (this.alpha != alpha) {
      this.alpha = alpha
      invalidateSelf()
    }
  }

  protected open fun getFillingColor (): Int {
    return alphaColor(alpha, forcedTheme?.getColor(colorId) ?: Theme.getColor(colorId))
  }

  final override fun draw (c: Canvas) {
    if (colorId != 0) {
      if (cornerRadius != 0f) {
        val rectF = Paints.getRectF()
        rectF.set(bounds)
        val radius = Screen.dp(cornerRadius).toFloat()
        c.drawRoundRect(rectF, radius, radius, Paints.fillingPaint(getFillingColor()))
      } else {
        c.drawRect(bounds, Paints.fillingPaint(getFillingColor()))
      }
    }
  }

  final override fun setAlpha (alpha: Int) { }

  final override fun setColorFilter (colorFilter: ColorFilter?) { }

  @Suppress("deprecation")
  final override fun getOpacity (): Int {
    return PixelFormat.UNKNOWN
  }

  companion object {
    @JvmStatic
    fun changeColor (view: View?, @ColorId newColorId: Int) {
      if (view != null) {
        val drawable = view.background
        if (drawable is FillingDrawable) {
          if (drawable.colorId != newColorId) {
            drawable.colorId = newColorId
            view.invalidate()
          }
        }
      }
    }
  }
}
