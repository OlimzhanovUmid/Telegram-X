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
package org.thunderdog.challegram.theme

import androidx.annotation.ColorInt

abstract class ThemeObject protected constructor (@ThemeId protected val themeId: Int) : ThemeDelegate {
  override fun equals (obj: Any?): Boolean {
    return obj is ThemeDelegate && obj.getId() == getId()
  }

  final override fun getId (): Int = themeId

  final override fun getDefaultWallpaper (): String? {
    return TGBackground.getBackgroundForLegacyWallpaperId(TGBackground.getDefaultWallpaperId(themeId))
  }

  open override fun getProperty (@PropertyId propertyId: Int): Float {
    when (propertyId) {
      PropertyId.WALLPAPER_ID ->
        return TGBackground.getDefaultWallpaperId(themeId).toFloat()
      PropertyId.PARENT_THEME ->
        return ThemeId.NONE.toFloat()
    }
    throw Theme.newError(propertyId, "propertyId")
  }

  @ColorInt
  abstract override fun getColor (@ColorId colorId: Int): Int
}
