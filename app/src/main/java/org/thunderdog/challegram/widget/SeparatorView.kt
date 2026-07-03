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
 * File created on 17/06/2015 at 17:24
 */
package org.thunderdog.challegram.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup

import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Screen

class SeparatorView (context: Context) : View(context) {
  private val paint: Paint

  private var left = 0f
  private var right = 0f

  private var top = 0f

  private var height: Int
  private var forcedFillingColor = 0
  private var forcedColor = 0
  private var colorId = ColorId.separator
  private var noAlign = false

  private var useFilling = false
  private var alignBottom = false

  init {
    paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    paint.setColor(Theme.getColor(colorId))
    paint.setStyle(Paint.Style.FILL)

    height = Math.max(Screen.dp(.5f), 1)
  }

  fun setColorId (@ColorId colorId: Int) {
    if (this.colorId != colorId) {
      this.colorId = colorId
      this.paint.setColor(Theme.getColor(colorId))
    }
  }

  fun forceColor (color: Int) {
    forcedColor = color
    paint.setColor(forcedColor)
  }

  fun setOffsets (left: Float, right: Float) {
    this.left = left
    this.right = right
  }

  fun setUseFilling () {
    this.useFilling = true
  }

  fun forceFillingColor (filling: Int) {
    this.useFilling = true
    this.forcedFillingColor = filling
  }

  override fun onTouchEvent (event: MotionEvent): Boolean {
    super.onTouchEvent(event)
    return true
  }

  fun setNoAlign () {
    this.noAlign = true
  }

  fun setSeparatorHeight (height: Int) {
    this.height = height
  }

  fun setAlignBottom () {
    alignBottom = true
  }

  override fun onMeasure (widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    top = if (alignBottom) (measuredHeight - height).toFloat() else if (noAlign) 0f else measuredHeight / 2f
  }

  override fun onDraw (c: Canvas) {
    val width = measuredWidth
    if (useFilling) {
      c.drawColor(if (forcedFillingColor != 0) forcedFillingColor else Theme.fillingColor())
    }
    if (forcedColor == 0) {
      paint.setColor(Theme.getColor(colorId))
    } else {
      paint.setColor(forcedColor)
    }
    if (left != 0f || right != 0f) {
      if (Lang.rtl()) {
        c.drawRect(right, top, width - left, top + height, paint)
      } else {
        c.drawRect(left, top, width - right, top + height, paint)
      }
    } else {
      c.drawRect(0f, top, width.toFloat(), top + height, paint)
    }
  }

  companion object {
    @JvmStatic
    fun simpleSeparator (context: Context, params: ViewGroup.LayoutParams, needFilling: Boolean): SeparatorView {
      val view = SeparatorView(context)
      view.setSeparatorHeight(Math.max(1, Screen.dp(.5f)))
      if (needFilling) {
        view.setNoAlign()
        view.setUseFilling()
      }
      // view.setOffsets(Screen.dp(72f), 0f);
      params.width = ViewGroup.LayoutParams.MATCH_PARENT
      params.height = Screen.dp(1f)
      view.setLayoutParams(params)
      return view
    }
  }
}
