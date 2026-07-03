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
 * File created on 04/04/2017
 */
package org.thunderdog.challegram.component.chat

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import org.thunderdog.challegram.tool.Paints
import org.thunderdog.challegram.tool.Screen

class EncryptionKeyDrawable (private var data: ByteArray?) : Drawable() {
  companion object {
    private val colors = intArrayOf(
      0xffffffff.toInt(),
      0xffd5e6f3.toInt(),
      0xff2d5775.toInt(),
      0xff2f99c9.toInt()
    )
  }

  private fun getBits (bitOffset: Int): Int {
    return (data!![bitOffset / 8].toInt() shr (bitOffset % 8)) and 0x3
  }

  fun setData (data: ByteArray?) {
    this.data = data
    invalidateSelf()
  }

  /*fun setColors (value: IntArray) {
    if (colors.size != 4) {
      throw IllegalArgumentException("colors must have length of 4")
    }
    colors = value
    invalidateSelf()
  }*/

  override fun draw (c: Canvas) {
    if (data == null) {
      return
    }

    if (data!!.size == 16) {
      var bitPointer = 0
      val rectSize = Math.floor((Math.min(bounds.width(), bounds.height()) / 8.0f).toDouble()).toFloat()
      val xOffset = Math.max(0f, (bounds.width() - rectSize * 8) / 2)
      val yOffset = Math.max(0f, (bounds.height() - rectSize * 8) / 2)
      for (iy in 0 until 8) {
        for (ix in 0 until 8) {
          val byteValue = getBits(bitPointer)
          bitPointer += 2
          val colorIndex = Math.abs(byteValue) % colors.size
          c.drawRect(xOffset + ix * rectSize, iy * rectSize + yOffset, xOffset + ix * rectSize + rectSize, iy * rectSize + rectSize + yOffset, Paints.fillingPaint(colors[colorIndex]))
        }
      }
    } else {
      var bitPointer = 0
      val rectSize = Math.floor((Math.min(bounds.width(), bounds.height()) / 12.0f).toDouble()).toFloat()
      val xOffset = Math.max(0f, (bounds.width() - rectSize * 12) / 2)
      val yOffset = Math.max(0f, (bounds.height() - rectSize * 12) / 2)
      for (iy in 0 until 12) {
        for (ix in 0 until 12) {
          val byteValue = getBits(bitPointer)
          val colorIndex = Math.abs(byteValue) % colors.size
          c.drawRect(xOffset + ix * rectSize, iy * rectSize + yOffset, xOffset + ix * rectSize + rectSize, iy * rectSize + rectSize + yOffset, Paints.fillingPaint(colors[colorIndex]))
          bitPointer += 2
        }
      }
    }
  }

  override fun setAlpha (alpha: Int) { }

  override fun setColorFilter (cf: ColorFilter?) { }

  @Suppress("deprecation")
  override fun getOpacity (): Int {
    return PixelFormat.UNKNOWN
  }

  override fun getIntrinsicWidth (): Int {
    return Screen.dp(32f)
  }

  override fun getIntrinsicHeight (): Int {
    return Screen.dp(32f)
  }
}
