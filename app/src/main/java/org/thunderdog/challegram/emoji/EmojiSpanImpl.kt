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
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.text.Spannable
import android.text.style.BackgroundColorSpan
import android.text.style.ReplacementSpan

import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.tool.Paints

import me.vkryl.core.compositeColor

open class EmojiSpanImpl protected constructor (protected val info: EmojiInfo?) : ReplacementSpan(), EmojiSpan {
  companion object {
    @JvmStatic
    fun newSpan (info: EmojiInfo?): EmojiSpan {
      return EmojiSpanImpl(info)
    }

    @JvmStatic
    fun newCustomEmojiSpan (
      info: EmojiInfo?,
      surfaceProvider: CustomEmojiSurfaceProvider,
      tdlib: Tdlib, customEmojiId: Long
    ): EmojiSpan {
      return CustomEmojiSpanImpl(info, surfaceProvider, tdlib, customEmojiId)
    }
  }

  @JvmField
  protected val size = EmojiSize()

  override fun getBuiltInEmojiInfo (): EmojiInfo? {
    return info
  }

  override fun toBuiltInEmojiSpan (): EmojiSpan {
    // Note: the EmojiSpan interface declares this method as non-null, but historically
    // (and still, for a null info) this returns null. Preserved via a forced non-null
    // assertion; this method has no live callers today (verified via repo-wide grep).
    val span: EmojiSpan? = if (info != null) newSpan(info) else null
    return span!!
  }

  open override fun isCustomEmoji (): Boolean {
    return false
  }

  override fun getRawSize (paint: Paint): Int {
    size.initialize(paint, null, true)
    return size.getSize()
  }

  override fun getSize (paint: Paint, text: CharSequence, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int {
    size.initialize(paint, fm, false)
    return size.getSize()
  }

  protected fun getEmojiSize (paint: Paint): Int {
    size.initialize(paint, null, true)
    return size.getSize()
  }

  protected var needInvalidate = false

  override fun needRefresh (): Boolean {
    return needInvalidate
  }

  override fun draw (canvas: Canvas, text: CharSequence, start: Int, end: Int, x: Float, top: Int, y: Int, bottom: Int, paint: Paint) {
    val emojiSize = getEmojiSize(paint)

    val centerX = x + emojiSize / 2f
    val height = (bottom - top).toFloat()
    val centerY = top + height / 2f

    if (text is Spannable) {
      val backgroundColorSpans = text.getSpans(start, end, BackgroundColorSpan::class.java)
      if (backgroundColorSpans != null && backgroundColorSpans.isNotEmpty()) {
        var blendedColor = 0
        for (backgroundColorSpan in backgroundColorSpans) {
          val backgroundColor = backgroundColorSpan.backgroundColor
          blendedColor = if (blendedColor == 0) {
            backgroundColor
          } else {
            compositeColor(blendedColor, backgroundColor)
          }
        }
        if (Color.alpha(blendedColor) != 0) {
          canvas.drawRect(x, top.toFloat(), x + emojiSize, bottom.toFloat(), Paints.fillingPaint(blendedColor))
        }
      }
    }

    drawEmoji(canvas, centerX, centerY, emojiSize)
  }

  protected open fun drawEmoji (canvas: Canvas, centerX: Float, centerY: Float, emojiSize: Int) {
    val rect = Paints.getRect()

    val reduce = Emoji.instance().getReduceSize()

    rect.left = (centerX - emojiSize / 2f).toInt() + reduce / 2
    rect.top = (centerY - emojiSize / 2f).toInt() + reduce / 2
    rect.right = rect.left + emojiSize - reduce / 2 - reduce % 2
    rect.bottom = rect.top + emojiSize - reduce / 2 - reduce % 2

    needInvalidate = !Emoji.instance().draw(canvas, info, rect)
  }
}
