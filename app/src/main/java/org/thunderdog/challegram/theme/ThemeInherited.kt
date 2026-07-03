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
 * File created on 06/11/2018
 */
package org.thunderdog.challegram.theme

abstract class ThemeInherited (themeIdValue: Int, @ThemeId parentThemeId: Int) : ThemeDelegate {
  protected val themeId: Int = themeIdValue
  protected val parentTheme: ThemeDelegate

  init {
    val parentTheme = ThemeSet.getBuiltinTheme(parentThemeId)
    if (parentTheme == null)
      throw IllegalArgumentException("parentThemeId == $themeIdValue")
    this.parentTheme = parentTheme
  }

  override fun getId (): Int {
    return themeId
  }

  override fun getProperty (propertyId: Int): Float {
    if (propertyId == PropertyId.PARENT_THEME)
      return parentTheme.getId().toFloat()
    return parentTheme.getProperty(propertyId)
  }

  override fun getColor (colorId: Int): Int {
    return parentTheme.getColor(colorId)
  }

  override fun getDefaultWallpaper (): String? {
    return parentTheme.getDefaultWallpaper()
  }
}
