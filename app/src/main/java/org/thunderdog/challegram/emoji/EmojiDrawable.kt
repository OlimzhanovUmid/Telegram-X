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
 * File created on 04/05/2019
 */
package org.thunderdog.challegram.emoji

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import org.thunderdog.challegram.tool.Screen

class EmojiDrawable (private val info: EmojiInfo) : Drawable() {
  override fun setAlpha (alpha: Int) { }

  override fun setColorFilter (colorFilter: ColorFilter?) { }

  @Suppress("deprecation")
  override fun getOpacity (): Int {
    return PixelFormat.TRANSPARENT
  }

  override fun draw (c: Canvas) {
    Emoji.instance().draw(c, info, bounds)
  }

  override fun getIntrinsicWidth (): Int {
    return Screen.dp(24f)
  }

  override fun getIntrinsicHeight (): Int {
    return Screen.dp(24f)
  }
}
