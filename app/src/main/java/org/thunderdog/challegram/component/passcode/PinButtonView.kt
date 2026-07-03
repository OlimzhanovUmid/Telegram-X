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
 * File created on 08/08/2015 at 16:42
 */
package org.thunderdog.challegram.component.passcode

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.view.View
import org.thunderdog.challegram.R
import org.thunderdog.challegram.U
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Drawables
import org.thunderdog.challegram.tool.Paints
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.tool.Views
import org.thunderdog.challegram.tool.getPorterDuffPaint

class PinButtonView (context: Context) : View(context) {
  // private static Paint bigPaint;
  // private static Paint smallPaint;

  private var number: Int = 0
  private var icon: Drawable? = null

  companion object {
    private const val TEXT_SIZE_BIG = 34f
    private const val TEXT_SIZE_SMALL = 11f
  }

  init {
    Views.setClickable(this)
  }

  fun setHasFeedback (has: Boolean) {
    setBackgroundResource(if (has) R.drawable.bg_btn_pin else R.drawable.transparent)
  }

  fun setNumber (number: Int) {
    this.number = number
  }

  fun getNumber (): String {
    return number.toString()
  }

  @Suppress("SpellCheckingInspection")
  fun getCodes (): String? {
    return when (number) {
      0 -> "+"
      1 -> ""
      2 -> "ABC"
      3 -> "DEF"
      4 -> "GHI"
      5 -> "JKL"
      6 -> "MNO"
      7 -> "PQRS"
      8 -> "TUV"
      9 -> "WXYZ"
      else -> null
    }
  }

  override fun onLayout (changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
    super.onLayout(changed, left, top, right, bottom)
    if (changed) {
      buildLayout()
    }
  }

  private var smallLeft = 0f
  private var smallTop = 0f
  private var bigLeft = 0f
  private var bigTop = 0f

  private fun buildLayout () {
    val totalWidth = measuredWidth.toFloat()
    val totalHeight = measuredHeight.toFloat()

    val centerX = totalWidth * .5f
    val centerY = totalHeight * .5f

    if (number == -1) {
      var icon = icon
      if (icon == null) {
        icon = Drawables.get(resources, R.drawable.baseline_backspace_24)
        this.icon = icon
      }

      bigLeft = centerX - icon!!.minimumWidth * .5f
      bigTop = centerY - icon.minimumHeight * .5f - Screen.dp(10f)

      return
    }

    smallLeft = centerX - U.measureText(getCodes(), Paints.getRegularTextPaint(TEXT_SIZE_SMALL)) * .5f
    bigLeft = centerX - U.measureText(getNumber(), Paints.getRegularTextPaint(TEXT_SIZE_BIG)) * .5f

    val offset = 22f

    bigTop = centerY - Screen.dp(20f - offset)
    smallTop = centerY - Screen.dp(-offset)
  }

  override fun onDraw (c: Canvas) {
    if (number == -1) {
      Drawables.draw(c, icon, bigLeft, bigTop, getPorterDuffPaint(ColorId.passcodeText))
    } else {
      c.drawText(getNumber(), bigLeft, bigTop, Paints.getRegularTextPaint(TEXT_SIZE_BIG, Theme.getColor(ColorId.passcodeText)))
      c.drawText(getCodes()!!, smallLeft, smallTop, Paints.getRegularTextPaint(TEXT_SIZE_SMALL, Theme.passcodeSubtitleColor()))
    }
  }
}
