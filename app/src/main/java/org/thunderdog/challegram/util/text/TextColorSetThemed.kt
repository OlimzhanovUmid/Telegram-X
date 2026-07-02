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
package org.thunderdog.challegram.util.text

import androidx.annotation.ColorInt

import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.theme.ThemeDelegate
import org.thunderdog.challegram.theme.ThemeManager

fun interface TextColorSetThemed : TextColorSet {
  fun forcedTheme (): ThemeDelegate? {
    return null
  }

  fun colorTheme (): ThemeDelegate {
    val forcedTheme = forcedTheme()
    return forcedTheme ?: ThemeManager.instance().currentTheme()
  }

  @ColorId
  fun defaultTextColorId (): Int
  @ColorId
  fun iconColorId (): Int {
    return defaultTextColorId()
  }
  @ColorId
  fun clickableTextColorId (isPressed: Boolean): Int {
    return defaultTextColorId()
  }
  @ColorId
  fun pressedBackgroundColorId (): Int {
    return 0
  }
  @ColorId
  fun staticBackgroundColorId (): Int {
    return 0
  }
  @ColorId
  override fun backgroundColorId (isPressed: Boolean): Int {
    return if (isPressed) pressedBackgroundColorId() else staticBackgroundColorId()
  }
  @ColorId
  override fun outlineColorId (isPressed: Boolean): Int {
    return 0
  }
  @ColorInt
  override fun defaultTextColor (): Int {
    val colorId = defaultTextColorId()
    return if (colorId != 0) colorTheme().getColor(colorId) else 0
  }
  @ColorInt
  override fun iconColor (): Int {
    val colorId = iconColorId()
    return if (colorId != 0) colorTheme().getColor(colorId) else 0
  }
  @ColorInt
  override fun clickableTextColor (isPressed: Boolean): Int {
    val colorId = clickableTextColorId(isPressed)
    return if (colorId != 0) colorTheme().getColor(colorId) else 0
  }
  @ColorInt
  override fun backgroundColor (isPressed: Boolean): Int {
    val colorId = backgroundColorId(isPressed)
    return if (colorId != 0) colorTheme().getColor(colorId) else 0
  }
  @ColorInt
  override fun outlineColor (isPressed: Boolean): Int {
    val colorId = outlineColorId(isPressed)
    return if (colorId != 0) colorTheme().getColor(colorId) else 0
  }
}
