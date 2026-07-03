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
 * File created on 13/03/2016 at 16:47
 */
package org.thunderdog.challegram.component.chat

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.view.View

import org.thunderdog.challegram.R
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Drawables
import org.thunderdog.challegram.tool.Paints
import org.thunderdog.challegram.tool.Screen

import me.vkryl.android.DECELERATE_INTERPOLATOR
import me.vkryl.android.getFraction
import me.vkryl.android.simpleValueAnimator
import me.vkryl.core.color

class SilentButton (context: Context) : View(context) {
  private val lineWidth: Int
  private val silentWidth: Int
  private val lineHeight: Float

  private var isSilent = false

  private val icon: Drawable? = Drawables.get(resources, R.drawable.outline_notifications_24)

  init {
    lineWidth = Screen.dp(2f)
    lineHeight = Screen.dp(23f).toFloat()
    silentWidth = Screen.dp(1.5f)
  }

  fun setVisible (visible: Boolean): Boolean {
    val isVisible = visibility == VISIBLE
    if (isVisible != visible) {
      visibility = if (visible) VISIBLE else INVISIBLE
      return true
    }
    return false
  }

  fun forceState (isSilent: Boolean) {
    lastAnimator?.cancel()
    lastAnimator = null
    this.isSilent = isSilent
    factor = if (isSilent) 1f else 0f
  }

  fun getIsSilent (): Boolean {
    return isSilent
  }

  private var lastAnimator: ValueAnimator? = null

  fun toggle (): Boolean {
    setValue(!isSilent)
    return isSilent
  }

  fun setValue (value: Boolean) {
    if (this.isSilent == value)
      return
    this.isSilent = value
    val startFactor = factor
    val obj = simpleValueAnimator()
    if (isSilent) {
      val diffFactor = 1f - startFactor
      obj.addUpdateListener { animation -> factor = startFactor + diffFactor * getFraction(animation) }
    } else {
      obj.addUpdateListener { animation -> factor = startFactor - startFactor * getFraction(animation) }
    }
    obj.setDuration(150L)
    obj.interpolator = DECELERATE_INTERPOLATOR
    lastAnimator?.cancel()
    lastAnimator = obj

    obj.start()
  }

  override fun onTouchEvent (e: MotionEvent): Boolean {
    return visibility == View.VISIBLE && super.onTouchEvent(e)
  }

  var factor = 0f
    set (value) {
      if (field != value) {
        field = value
        invalidate()
      }
    }

  override fun onDraw (c: Canvas) {
    if (visibility != View.VISIBLE) {
      return
    }

    val width = measuredWidth
    val cx = width * .5f
    val cy = measuredHeight * .5f

    val icon = icon
    if (icon != null) {
      Drawables.draw(c, icon, cx - icon.minimumWidth / 2, cy - icon.minimumHeight / 2, Paints.getIconGrayPorterDuffPaint())
    }
    if (factor == 0f) {
      return
    }

    c.save()
    c.rotate(-45f, cx, cy)

    val padding = Screen.dp(1f)
    val lineTop = (cy - lineHeight * .5f).toInt() + padding
    val x = (cx - padding).toInt()

    c.clipRect(x.toFloat(), lineTop.toFloat(), (x + lineWidth + silentWidth).toFloat(), lineTop + lineHeight * factor)
    val rectF: RectF = Paints.getRectF()
    rectF.set(x.toFloat(), lineTop.toFloat(), (x + lineWidth).toFloat(), lineTop + lineHeight)

    val alpha = (255f * Math.min(1f, (lineHeight * factor) / Screen.dpf(8f))).toInt()
    c.drawRoundRect(rectF, (lineWidth / 2).toFloat(), (lineWidth / 2).toFloat(), Paints.fillingPaint(if (alpha == 255) Theme.iconColor() else color(alpha, Theme.iconColor())))
    c.drawRect((x + lineWidth).toFloat(), lineTop.toFloat(), (x + lineWidth + silentWidth).toFloat(), lineTop + lineHeight, Paints.fillingPaint(/*alpha == 255 ? Theme.fillingColor() : */color((255f * factor).toInt(), Theme.fillingColor())))

    c.restore()
  }
}
