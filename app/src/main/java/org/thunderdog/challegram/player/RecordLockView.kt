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
 * File created on 10/12/2017
 */
package org.thunderdog.challegram.player

import android.content.Context
import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider

import org.thunderdog.challegram.R
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.DrawAlgorithms
import org.thunderdog.challegram.tool.Drawables
import org.thunderdog.challegram.tool.Paints
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.tool.Views
import org.thunderdog.challegram.tool.getPorterDuffPaint
import me.vkryl.core.alphaColor
import me.vkryl.core.fromTo
import me.vkryl.core.fromToArgb


class RecordLockView (context: Context) : View(context) {
  companion object {
    const val BUTTON_SIZE = RecordControllerButton.BUTTON_SIZE
    const val BUTTON_EXPANDED = 33

    const val MODE_DEFAULT = 0
    const val MODE_AUDIO = 1
    const val MODE_VIDEO = 2

    private val tmpRect = RectF()
  }

  private val drawableVoice: Drawable? = Drawables.get(context.resources, R.drawable.baseline_mic_24)
  private val drawableRound: Drawable? = Drawables.get(context.resources, R.drawable.deproko_baseline_msg_video_24)

  init {
    outlineProvider = object : ViewOutlineProvider() {
      override fun getOutline (view: View, outline: Outline) {
        outline.setRoundRect(0, 0, view.measuredWidth, (view.measuredHeight - Screen.dp(BUTTON_EXPANDED.toFloat()) * collapseFactor).toInt(), Screen.dp(BUTTON_SIZE / 2f).toFloat())
      }
    }
    layoutParams = ViewGroup.LayoutParams(Screen.dp(BUTTON_SIZE.toFloat()), Screen.dp((BUTTON_SIZE + BUTTON_EXPANDED).toFloat()))
  }

  override fun onTouchEvent (e: MotionEvent): Boolean {
    if (e.action == MotionEvent.ACTION_DOWN) {
      val bottom = measuredHeight - Screen.dp(BUTTON_EXPANDED.toFloat()) * collapseFactor
      if (e.y > bottom) {
        return false
      }
    }

    return Views.onTouchEvent(this, e) && super.onTouchEvent(e)
  }

  override fun onMeasure (widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    pivotX = (measuredWidth / 2).toFloat()
    pivotY = getCenterY()
  }

  private fun getCenterY (): Float {
    return ((measuredHeight - Screen.dp(BUTTON_EXPANDED.toFloat()) * collapseFactor).toInt() - Screen.dp(BUTTON_SIZE / 2f)).toFloat()
  }

  private var collapseFactor = 0f

  fun setCollapseFactor (factor: Float) {
    if (this.collapseFactor != factor) {
      this.collapseFactor = factor
      pivotY = getCenterY()
      invalidate()
      invalidateOutline()
    }
  }

  private var sendFactor = 0f

  fun setSendFactor (sendFactor: Float) {
    if (this.sendFactor != sendFactor) {
      this.sendFactor = sendFactor
      invalidate()
    }
  }

  private var editFactor = 0f

  fun setEditFactor (editFactor: Float) {
    if (this.editFactor != editFactor) {
      this.editFactor = editFactor
      invalidate()
    }
  }

  private var mode = MODE_DEFAULT

  fun setMode (mode: Int) {
    this.mode = mode
  }

  override fun onDraw (c: Canvas) {
    val fillingColor = Theme.fillingColor()

    val rectF = Paints.getRectF()
    val viewWidth = measuredWidth
    val viewHeight = measuredHeight
    rectF.set(0f, 0f, viewWidth.toFloat(), viewHeight - Screen.dp(BUTTON_EXPANDED.toFloat()) * collapseFactor)
    val radius = Screen.dp(BUTTON_SIZE.toFloat()) / 2
    c.drawRoundRect(rectF, radius.toFloat(), radius.toFloat(), Paints.fillingPaint(fillingColor))

    val bottomCy = rectF.bottom.toInt() - radius

    val cx = viewWidth / 2
    val cy = Screen.dp(BUTTON_SIZE.toFloat()) / 2

    val grayColor = Theme.iconColor()
    val redColor = Theme.getColor(ColorId.iconNegative)

    val totalDy = (Screen.dp(2f) * collapseFactor * (1f - sendFactor)).toInt()

    val width = (Screen.dp(6f) + Screen.dp(2f) * (1f - sendFactor)).toInt()
    val height = (Screen.dp(6f) + Screen.dp(1f) * (if (mode == MODE_DEFAULT) (1f - sendFactor) else 1f)).toInt()
    var dy = (Screen.dp(BUTTON_SIZE.toFloat()) / 3f * (1f - collapseFactor)).toInt()
    rectF.set((cx - width).toFloat(), (cy - height + dy + totalDy).toFloat(), (cx + width).toFloat(), (cy + height + dy + totalDy).toFloat())

    val r = Screen.dp(2f) * (1f - sendFactor)
    if (mode == MODE_DEFAULT) {
      c.drawRoundRect(rectF, Screen.dp(2f).toFloat(), Screen.dp(2f).toFloat(), Paints.fillingPaint(fromToArgb(grayColor, redColor, sendFactor)))
    } else {
      val pauseAlpha = 1f - editFactor
      val alpha = editFactor

      if (editFactor < 1f) {
        c.drawRoundRect(rectF, r, r, Paints.fillingPaint(alphaColor(pauseAlpha, grayColor)))
        val w = Screen.dp(2f).toFloat()
        val h = fromTo(Screen.dp(2f).toFloat(), rectF.height() / 2f + 1, sendFactor)
        tmpRect.set(cx - w, rectF.centerY() - h, cx + w, rectF.centerY() + h)
        c.drawRoundRect(tmpRect, r, r, Paints.fillingPaint(alphaColor(pauseAlpha, fillingColor)))
      }

      if (editFactor > 0f) {
        Drawables.drawCentered(c, if (mode == MODE_VIDEO) drawableRound else drawableVoice, rectF.centerX(), rectF.centerY(), getPorterDuffPaint(ColorId.icon, alpha))
      }
    }

    if (sendFactor < 1f) {
      if (mode == MODE_DEFAULT) {
        c.drawCircle(cx.toFloat(), rectF.centerY(), Screen.dp(2f).toFloat(), Paints.fillingPaint(alphaColor(1f - sendFactor, fillingColor)))
      }
      dy /= 2
      rectF.offset(0f, -dy.toFloat())
      val paint = Paints.strokeBigPaint(alphaColor(1f - sendFactor, grayColor))
      rectF.set((cx - Screen.dp(5f)).toFloat(), rectF.top - Screen.dp(5f), (cx + Screen.dp(5f)).toFloat(), rectF.top + Screen.dp(5f))
      c.drawArc(rectF, 180f, 180f, false, paint)
      if (dy > 0) {
        val x = rectF.left.toInt()
        val y = rectF.centerY().toInt()
        c.drawLine(x.toFloat(), y.toFloat(), x.toFloat(), (y + dy).toFloat(), paint)
        c.drawLine(rectF.right, y.toFloat(), rectF.right, (y + Math.min(Screen.dp(2f), dy)).toFloat(), paint)
      }
    }

    if (collapseFactor < 1f) {
      DrawAlgorithms.drawDirection(c, cx.toFloat(), bottomCy.toFloat(), alphaColor(1f - (if (collapseFactor >= .5f) 1f else collapseFactor / .5f), grayColor), Gravity.TOP)
    }
  }
}
