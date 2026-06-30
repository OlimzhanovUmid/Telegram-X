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
 */
package org.thunderdog.challegram.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import org.drinkless.tdlib.TdApi
import org.thunderdog.challegram.config.Config
import org.thunderdog.challegram.loader.ComplexReceiver
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Paints
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.util.text.FormattedText
import org.thunderdog.challegram.util.text.Text
import org.thunderdog.challegram.util.text.TextColorSets
import tgx.td.customEmojiId

/**
 * Horizontal picker for a forum topic icon: the 6 default topic colors followed by the default
 * custom-emoji icons from getForumTopicDefaultIcons. Reports the choice as a [TdApi.ForumTopicIcon].
 * Width is its full content width; wrap it in a HorizontalScrollView for overflow.
 */
class TopicIconPickerView(context: Context, private val tdlib: Tdlib) : View(context) {

  private val colors = intArrayOf(0x6FB9F0, 0xFFD67E, 0xCB86DB, 0x8EEE98, 0xFF93B2, 0xFB6F5F)

  private class IconItem(val customEmojiId: Long, var text: Text? = null)
  private val icons = ArrayList<IconItem>()

  private val emojiReceiver = ComplexReceiver(this, Config.MAX_ANIMATED_EMOJI_REFRESH_RATE)

  var selectedColor: Int = colors[0]
    private set
  var selectedCustomEmojiId: Long = 0L
    private set

  private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.STROKE
    strokeWidth = Screen.dp(2f).toFloat()
  }
  private val cell = RectF()

  fun load () {
    tdlib.getForumTopicDefaultIcons({ stickers -> if (stickers != null) post { setDefaultIcons(stickers) } }, null)
  }

  private fun setDefaultIcons (stickers: TdApi.Stickers) {
    emojiReceiver.clear()
    icons.clear()
    for (sticker in stickers.stickers) {
      val id = sticker.customEmojiId()
      if (id != 0L) {
        icons.add(IconItem(id))
      }
    }
    icons.forEachIndexed { index, item -> item.text = buildIcon(index, item.customEmojiId) }
    requestLayout()
    invalidate()
  }

  private fun buildIcon (index: Int, customEmojiId: Long): Text {
    val formatted = FormattedText.customEmoji(tdlib, "*", customEmojiId)
    return Text.Builder(
      formatted,
      Screen.dp(ITEM_SIZE),
      Paints.robotoStyleProvider(ICON_EMOJI_TEXT_SIZE),
      TextColorSets.Regular.NORMAL,
      Text.TextMediaListener { text, _ ->
        text.requestMedia(emojiReceiver, index.toLong(), 1)
        invalidate()
      }
    ).singleLine().build().also {
      it.requestMedia(emojiReceiver, index.toLong(), 1)
    }
  }

  // TDLib requires the raw palette RGB (one of the 6 allowed values) — NOT alpha-premultiplied.
  fun buildResult (): TdApi.ForumTopicIcon =
    TdApi.ForumTopicIcon(selectedColor, selectedCustomEmojiId)

  override fun onMeasure (widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val item = Screen.dp(ITEM_SIZE)
    val gap = Screen.dp(GAP)
    val count = colors.size + icons.size
    val width = gap + count * (item + gap)
    setMeasuredDimension(width, Screen.dp(ROW_HEIGHT))
  }

  override fun onDraw (c: Canvas) {
    val size = Screen.dp(ITEM_SIZE)
    val gap = Screen.dp(GAP)
    var x = gap
    val y = (measuredHeight - size) / 2
    val radius = Screen.dp(12f).toFloat()
    ringPaint.color = Theme.getColor(ColorId.iconActive)

    for (color in colors) {
      cell.set(x.toFloat(), y.toFloat(), (x + size).toFloat(), (y + size).toFloat())
      c.drawRoundRect(cell, radius, radius, Paints.fillingPaint(color or Color.BLACK))
      if (selectedCustomEmojiId == 0L && selectedColor == color) {
        drawRing(c, size, x, y)
      }
      x += size + gap
    }
    for (item in icons) {
      val text = item.text
      if (text != null) {
        val ex = x + (size - text.getWidth()) / 2
        val ey = y + (size - text.getHeight()) / 2
        text.draw(c, ex, ey, null, 1f, emojiReceiver)
        if (selectedCustomEmojiId == item.customEmojiId) {
          drawRing(c, size, x, y)
        }
      }
      x += size + gap
    }
  }

  private fun drawRing (c: Canvas, size: Int, x: Int, y: Int) {
    val inset = Screen.dp(2f).toFloat()
    c.drawRoundRect(
      x - inset, y - inset, x + size + inset, y + size + inset,
      Screen.dp(14f).toFloat(), Screen.dp(14f).toFloat(), ringPaint
    )
  }

  override fun onTouchEvent (e: MotionEvent): Boolean {
    if (e.action == MotionEvent.ACTION_UP) {
      val size = Screen.dp(ITEM_SIZE)
      val gap = Screen.dp(GAP)
      val idx = (e.x.toInt() / (size + gap)).coerceAtLeast(0)
      val total = colors.size + icons.size
      if (idx >= total) {
        return true
      }
      if (idx < colors.size) {
        selectedColor = colors[idx]
        selectedCustomEmojiId = 0L
      } else {
        icons.getOrNull(idx - colors.size)?.let { selectedCustomEmojiId = it.customEmojiId }
      }
      invalidate()
      return true
    }
    return true
  }

  override fun onAttachedToWindow () {
    super.onAttachedToWindow()
    emojiReceiver.attach()
  }

  override fun onDetachedFromWindow () {
    super.onDetachedFromWindow()
    emojiReceiver.detach()
  }

  companion object {
    private const val ITEM_SIZE = 48f
    private const val GAP = 10f
    private const val ROW_HEIGHT = 68f
    private const val ICON_EMOJI_TEXT_SIZE = 30f
  }
}
