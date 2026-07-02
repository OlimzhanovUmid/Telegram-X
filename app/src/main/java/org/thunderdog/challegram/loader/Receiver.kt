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
 * File created on 24/12/2016
 */
package org.thunderdog.challegram.loader

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.view.View

import androidx.annotation.ColorInt

import org.thunderdog.challegram.config.Config
import org.thunderdog.challegram.navigation.TooltipOverlayView
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.theme.PorterDuffColorId
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Paints
import org.thunderdog.challegram.tool.Views

import me.vkryl.core.alphaColor
import tgx.td.data.StickerOutline

interface Receiver : TooltipOverlayView.LocationProvider {
  fun getTargetView (): View
  fun getTargetWidth (): Int
  fun getTargetHeight (): Int

  fun <T : Receiver> setUpdateListener (listener: ReceiverUpdateListener?): T

  fun getLeft (): Int
  fun getTop (): Int
  fun getRight (): Int
  fun getBottom (): Int

  fun setRadius (radius: Float)

  var tag: Any?

  fun getWidth (): Int {
    return getRight() - getLeft()
  }
  fun getHeight (): Int {
    return getBottom() - getTop()
  }
  fun centerX (): Int {
    return ((getLeft() + getRight()) * .5f).toInt()
  }
  fun centerY (): Int {
    return ((getTop() + getBottom()) * .5f).toInt()
  }

  fun attach ()
  fun detach ()
  fun clear ()
  fun destroy ()

  fun isEmpty (): Boolean

  fun toRect (rect: Rect) {
    rect.set(getLeft(), getTop(), getRight(), getBottom())
  }

  fun setPorterDuffColorFilter (colorOrColorId: Int, alpha: Float, colorIsId: Boolean) {
    throw UnsupportedOperationException()
  }
  fun setThemedPorterDuffColorId (@PorterDuffColorId colorId: Int) {
    setPorterDuffColorFilter(colorId, 1f, true)
  }
  fun setPorterDuffColorFilter (@ColorInt color: Int) {
    setPorterDuffColorFilter(color, 1f, false)
  }
  fun disablePorterDuffColorFilter () {
    setPorterDuffColorFilter(ColorId.NONE, 0f, true)
  }

  fun drawPlaceholderOutline (c: Canvas, outline: StickerOutline?) {
    if (outline != null && outline.hasPath()) {
      drawPlaceholderContour(c, outline.getPath())
    }
  }

  fun drawPlaceholderContour (c: Canvas, path: Path?) {
    drawPlaceholderContour(c, path, 1f)
  }

  fun drawPlaceholderContour (c: Canvas, path: Path?, alpha: Float) {
    if (path != null) {
      val size = minOf(getWidth(), getHeight())
      val left = centerX() - size / 2
      val top = centerY() - size / 2
      val translate = left != 0 || top != 0
      val restoreToCount: Int
      if (translate) {
        restoreToCount = Views.save(c)
        c.translate(left.toFloat(), top.toFloat())
      } else {
        restoreToCount = -1
      }
      if (Config.DEBUG_STICKER_OUTLINES) {
        c.drawPath(path, Paints.fillingPaint(alphaColor(alpha, 0x99ff0000.toInt())))
      } else {
        c.drawPath(path, if (alpha != 1f) Paints.fillingPaint(alphaColor(alpha, Theme.placeholderColor())) else Paints.getPlaceholderPaint())
      }
      if (translate) {
        Views.restore(c, restoreToCount)
      }
    }
  }
  fun drawPlaceholder (c: Canvas) {
    drawPlaceholderRounded(c, 0f)
  }
  fun drawPlaceholderRounded (c: Canvas, radius: Float) {
    drawPlaceholderRounded(c, radius, Theme.placeholderColor())
  }
  fun drawPlaceholderRounded (c: Canvas, radius: Float, color: Int) {
    drawPlaceholderRounded(c, radius, 0f, Paints.fillingPaint(color))
  }
  fun drawPlaceholderRounded (c: Canvas, radius: Float, color: Int, addSize: Float) {
    drawPlaceholderRounded(c, radius, addSize, Paints.fillingPaint(color))
  }
  fun drawPlaceholderRounded (c: Canvas, radius: Float, addSize: Float, paint: Paint) {
    if (radius > 0) {
      val rect: RectF = Paints.getRectF()
      rect.set(getLeft() - addSize, getTop() - addSize, getRight() + addSize, getBottom() + addSize)
      if (rect.width() == radius * 2 && rect.height() == radius * 2) {
        c.drawCircle(rect.centerX(), rect.centerY(), radius + addSize, paint)
      } else {
        c.drawRoundRect(rect, radius + addSize, radius + addSize, paint)
      }
    } else {
      c.drawRect(getLeft() - addSize, getTop() - addSize, getRight() + addSize, getBottom() + addSize, paint)
    }
  }

  fun needPlaceholder (): Boolean
  var alpha: Float
  fun setAnimationDisabled (disabled: Boolean)
  fun setBounds (left: Int, top: Int, right: Int, bottom: Int): Boolean
  fun setBoundsScaled (left: Int, top: Int, right: Int, bottom: Int, scale: Float): Boolean {
    if (scale == 1f) {
      return setBounds(left, top, right, bottom)
    } else {
      val width = right - left
      val height = bottom - top
      val centerX = left + width / 2
      val centerY = top + height / 2
      return setBounds(
        centerX - width / 2,
        centerY - height / 2,
        centerX + width / 2 + width % 2,
        centerY + height / 2 + height % 2
      )
    }
  }
  fun forceBoundsLayout ()

  fun isInsideContent (x: Float, y: Float, emptyWidth: Int, emptyHeight: Int): Boolean

  fun isInsideReceiver (x: Float, y: Float): Boolean {
    return x >= getLeft() && x <= getRight() && y >= getTop() && y <= getBottom()
  }

  fun draw (c: Canvas)

  fun drawScaled (c: Canvas, scale: Float) {
    // Note: make sure placeholder is scaled as well when using this method
    if (scale == 1f) {
      draw(c)
    } else {
      val saveCount = Views.save(c)
      c.scale(scale, scale, centerX().toFloat(), centerY().toFloat())
      draw(c)
      Views.restore(c, saveCount)
    }
  }

  fun invalidate ()

  override fun getTargetBounds (targetView: View?, outRect: Rect?) {
    outRect?.set(getLeft(), getTop(), getRight(), getBottom())
  }

  var paintAlpha: Float
  fun restorePaintAlpha ()
}
