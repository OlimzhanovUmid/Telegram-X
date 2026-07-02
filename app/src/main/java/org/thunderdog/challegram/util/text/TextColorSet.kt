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
import org.thunderdog.challegram.theme.PorterDuffColorId
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Screen

import me.vkryl.core.mergeLong

fun interface TextColorSet {
  @ColorInt
  fun defaultTextColor (): Int

  @ColorInt
  fun iconColor (): Int {
    return defaultTextColor()
  }

  fun mediaTextComplexColor (): Long {
    return Theme.newComplexColor(false, defaultTextColor())
  }

  @ColorInt
  fun clickableTextColor (isPressed: Boolean): Int {
    return defaultTextColor()
  }
  @ColorInt
  fun backgroundColor (isPressed: Boolean): Int {
    return 0
  }
  @ColorInt
  fun outlineColor (isPressed: Boolean): Int {
    return 0
  }

  @ColorInt
  fun overlayColor (isPressed: Boolean): Int {
    return 0
  }
  @ColorInt
  fun overlayOutlineColor (isPressed: Boolean): Int {
    return 0
  }

  fun backgroundColorId (isPressed: Boolean): Int {
    return backgroundColor(isPressed)
  }
  fun outlineColorId (isPressed: Boolean): Int {
    return outlineColor(isPressed)
  }
  fun backgroundId (isPressed: Boolean): Long {
    return mergeLong(backgroundColorId(isPressed), outlineColorId(isPressed))
  }

  @PorterDuffColorId
  fun quoteTextColorId (): Int {
    return ColorId.blockQuoteText
  }

  @PorterDuffColorId
  fun quoteLineColorId (): Int {
    return ColorId.blockQuoteLine
  }

  fun overlayColorId (isPressed: Boolean): Int {
    return overlayColor(isPressed)
  }
  fun overlayOutlineColorId (isPressed: Boolean): Int {
    return overlayOutlineColor(isPressed)
  }
  fun overlayId (isPressed: Boolean): Long {
    return mergeLong(overlayColorId(isPressed), overlayOutlineColorId(isPressed))
  }

  fun backgroundPadding (): Int {
    return Screen.dp(3f)
  }
}
