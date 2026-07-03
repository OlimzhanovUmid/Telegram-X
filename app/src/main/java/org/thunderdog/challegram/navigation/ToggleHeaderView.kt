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
 * File created on 09/08/2015 at 16:28
 */
package org.thunderdog.challegram.navigation

import android.content.Context
import android.graphics.Canvas
import android.text.TextPaint
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup

import org.thunderdog.challegram.U
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Paints
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.util.text.Letters
import me.vkryl.core.isEmpty


class ToggleHeaderView (context: Context) : View(context) {
  // private TextPaint paint;

  private var text: Letters? = null
  private var textTrimmed: String? = null
  private var textTrimmedWidth: Float = 0f
  private var textWidth: Float = 0f
  private var textPadding: Int = 0
  private var textTop: Int = 0

  private val triangle: TriangleView = TriangleView()
  private var triangleTop: Float = 0f

  private var textColor: Int = Theme.headerTextColor()

  fun getTriangleCenterX (): Float {
    return triangle.centerX
  }

  private fun getPaint (needFakeBold: Boolean, willDraw: Boolean): TextPaint {
    return if (willDraw) {
      Paints.getMediumTextPaint(19f, textColor, needFakeBold)
    } else {
      Paints.getMediumTextPaint(19f, needFakeBold)
    }
  }

  fun setText (sequence: CharSequence?) {
    val text = if (!isEmpty(sequence)) Letters(sequence.toString()) else null
    this.text = text
    this.textWidth = if (text != null) U.measureText(text.text, getPaint(text.needFakeBold, false)) else 0f
    this.triangleTop = Screen.dp(12f).toFloat()
    this.textTop = Screen.dp(20f)
    this.textPadding = Screen.dp(10f)
    trimText()
    requestLayout()
    invalidate()
  }

  private fun trimText () {
    val avail = measuredWidth - textPadding - Screen.dp(12f)
    val text = this.text
    if (text == null || layoutParams.width == ViewGroup.LayoutParams.WRAP_CONTENT || textWidth <= avail) {
      textTrimmed = null
      textTrimmedWidth = 0f
    } else {
      val paint = getPaint(text.needFakeBold, false)
      textTrimmed = TextUtils.ellipsize(text.text, paint, avail.toFloat(), TextUtils.TruncateAt.END).toString()
      textTrimmedWidth = U.measureText(textTrimmed, paint)
    }
  }

  fun setTextColor (color: Int) {
    if (this.textColor != color) {
      this.textColor = color
      invalidate()
    }
  }

  fun setTriangleColor (color: Int) {
    triangle.setColor(color)
  }

  override fun onMeasure (widthMeasureSpec: Int, heightMeasureSpec: Int) {
    if (layoutParams.width == ViewGroup.LayoutParams.WRAP_CONTENT) {
      setMeasuredDimension((textWidth + triangle.width + textPadding).toInt(), getDefaultSize(suggestedMinimumHeight, heightMeasureSpec))
    } else {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec)
      trimText()
    }
  }

  override fun onDraw (c: Canvas) {
    val text = this.text
    val textTrimmed = this.textTrimmed
    val textWidth = if (textTrimmed != null) textTrimmedWidth else this.textWidth
    if (Lang.rtl()) {
      val viewWidth = measuredWidth
      if (text != null)
        c.drawText(textTrimmed ?: text.text, viewWidth - textWidth, textTop.toFloat(), getPaint(text.needFakeBold, true))
      c.save()
      c.translate(viewWidth - textWidth - textPadding - triangle.width, triangleTop)
      triangle.draw(c)
      c.restore()
    } else {
      if (text != null)
        c.drawText(textTrimmed ?: text.text, 0f, textTop.toFloat(), getPaint(text.needFakeBold, true))
      c.save()
      c.translate(textWidth + textPadding, triangleTop)
      triangle.draw(c)
      c.restore()
    }
  }
}
