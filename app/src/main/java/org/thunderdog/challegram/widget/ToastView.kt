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
 * File created on 14/11/2016
 */
package org.thunderdog.challegram.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup

import org.thunderdog.challegram.tool.Fonts
import org.thunderdog.challegram.tool.Paints
import org.thunderdog.challegram.tool.Screen

class ToastView (context: Context) : NoScrollTextView(context) {
  init {
    setTypeface(Fonts.getRobotoRegular())
    setTextColor(0xffffffff.toInt())
    setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
    setPadding(Screen.dp(10f), Screen.dp(10f), Screen.dp(10f), Screen.dp(10f))
    gravity = Gravity.CENTER

    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
  }

  override fun onDraw (c: Canvas) {
    val rectF = Paints.getRectF()
    val width = measuredWidth
    val height = measuredHeight
    val radius = Screen.dp(10f)
    rectF.set(0f, 0f, width.toFloat(), height.toFloat())
    c.drawRoundRect(rectF, radius.toFloat(), radius.toFloat(), Paints.fillingPaint(0xa0000000.toInt()))

    super.onDraw(c)
  }
}
