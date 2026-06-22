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
import android.graphics.RectF
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import org.drinkless.tdlib.TdApi
import org.thunderdog.challegram.R
import org.thunderdog.challegram.config.Config
import org.thunderdog.challegram.loader.ComplexReceiver
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Drawables
import org.thunderdog.challegram.tool.Paints
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.util.text.Counter
import org.thunderdog.challegram.util.text.FormattedText
import org.thunderdog.challegram.util.text.Text
import org.thunderdog.challegram.util.text.TextColorSets

/**
 * Custom-drawn forum topic row: topic icon (custom emoji when set, else a colored monogram),
 * title, last-message/draft preview, unread-count badge, muted/closed/pinned markers.
 */
class TopicView(context: Context, private val tdlib: Tdlib) : View(context) {

  private var topic: TdApi.ForumTopic? = null
  private var title: CharSequence = ""
  private var subtitle: CharSequence = ""
  private var trimmedTitle: CharSequence = ""
  private var trimmedSubtitle: CharSequence = ""
  private var monogram: String = ""
  private var iconColor: Int = 0
  private var isClosed = false
  private var isPinned = false
  private var lastWidth = 0

  private val iconRect = RectF()
  private val unreadCounter = Counter.Builder().callback(this).build()

  private val emojiReceiver = ComplexReceiver(this, Config.MAX_ANIMATED_EMOJI_REFRESH_RATE)
  private var iconEmoji: Text? = null

  init {
    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Screen.dp(ROW_HEIGHT))
  }

  fun setTopic (topic: TdApi.ForumTopic, title: CharSequence, subtitle: CharSequence, muted: Boolean) {
    this.topic = topic
    val info = topic.info
    this.title = title
    this.subtitle = subtitle
    this.monogram = if (info.name.isNotEmpty()) info.name.substring(0, 1).uppercase() else "#"
    this.iconColor = info.icon.color or Color.BLACK // ensure opaque
    this.isClosed = info.isClosed
    this.isPinned = topic.isPinned && !info.isGeneral
    unreadCounter.setCount(topic.unreadCount.toLong(), muted, false)
    buildIconEmoji(info.icon.customEmojiId)
    if (lastWidth > 0) {
      trim()
    }
    invalidate()
  }

  private fun buildIconEmoji (customEmojiId: Long) {
    if (customEmojiId != 0L) {
      val formatted = FormattedText.customEmoji(tdlib, "*", customEmojiId)
      iconEmoji = Text.Builder(
        formatted,
        Screen.dp(ICON_SIZE),
        Paints.robotoStyleProvider(ICON_EMOJI_TEXT_SIZE),
        TextColorSets.Regular.NORMAL,
        Text.TextMediaListener { text, _ ->
          text.requestMedia(emojiReceiver, 0, 1)
          invalidate()
        }
      ).singleLine().build().also {
        it.requestMedia(emojiReceiver, 0, 1)
      }
    } else {
      iconEmoji = null
      emojiReceiver.clear()
    }
  }

  override fun onMeasure (widthMeasureSpec: Int, heightMeasureSpec: Int) {
    setMeasuredDimension(
      MeasureSpec.getSize(widthMeasureSpec),
      Screen.dp(ROW_HEIGHT)
    )
    val width = measuredWidth
    if (width != lastWidth) {
      lastWidth = width
      trim()
    }
  }

  private fun textLeft (): Int = Screen.dp(72f)

  private fun trim () {
    val avail = (lastWidth - textLeft() - Screen.dp(56f)).coerceAtLeast(0)
    trimmedTitle = TextUtils.ellipsize(title, Paints.getTitlePaint(false), avail.toFloat(), TextUtils.TruncateAt.END)
    trimmedSubtitle = TextUtils.ellipsize(subtitle, Paints.getRegularTextPaint(SUBTITLE_TEXT_SIZE), avail.toFloat(), TextUtils.TruncateAt.END)
  }

  override fun onDraw (c: Canvas) {
    val height = measuredHeight
    val iconSize = Screen.dp(ICON_SIZE)
    val iconLeft = Screen.dp(12f)
    val iconTop = (height - iconSize) / 2

    val emoji = iconEmoji
    if (emoji != null) {
      val x = iconLeft + (iconSize - emoji.getWidth()) / 2
      val y = iconTop + (iconSize - emoji.getHeight()) / 2
      emoji.draw(c, x, y, null, 1f, emojiReceiver)
    } else {
      iconRect.set(iconLeft.toFloat(), iconTop.toFloat(), (iconLeft + iconSize).toFloat(), (iconTop + iconSize).toFloat())
      val radius = Screen.dp(12f).toFloat()
      c.drawRoundRect(iconRect, radius, radius, Paints.fillingPaint(iconColor))
      val letterPaint = Paints.getBoldPaint15(false, Color.WHITE)
      val ty = iconRect.centerY() - (letterPaint.descent() + letterPaint.ascent()) / 2f
      c.drawText(monogram, iconRect.centerX() - letterPaint.measureText(monogram) / 2f, ty, letterPaint)
    }

    var titleLeft = textLeft()
    val titleTop = Screen.dp(28f)
    if (isClosed) {
      val lock = Drawables.get(resources, R.drawable.baseline_lock_16)
      if (lock != null) {
        Drawables.draw(c, lock, titleLeft.toFloat(), (titleTop - lock.minimumHeight).toFloat(), Paints.getPorterDuffPaint(Theme.getColor(ColorId.textLight)))
        titleLeft += lock.minimumWidth + Screen.dp(2f)
      }
    }
    c.drawText(trimmedTitle, 0, trimmedTitle.length, titleLeft.toFloat(), titleTop.toFloat(), Paints.getTitlePaint(false).also { it.color = Theme.getColor(ColorId.text) })

    val subtitleTop = Screen.dp(50f)
    c.drawText(trimmedSubtitle, 0, trimmedSubtitle.length, textLeft().toFloat(), subtitleTop.toFloat(), Paints.getRegularTextPaint(SUBTITLE_TEXT_SIZE, Theme.getColor(ColorId.textLight)))

    // Pin marker / unread counter on the right
    val rightX = (lastWidth - Screen.dp(16f)).toFloat()
    if (isPinned && topic?.unreadCount == 0) {
      val pin = Drawables.get(resources, R.drawable.deproko_baseline_pin_16)
      if (pin != null) {
        Drawables.draw(c, pin, rightX - pin.minimumWidth, height / 2f - pin.minimumHeight / 2f, Paints.getPorterDuffPaint(Theme.getColor(ColorId.textLight)))
      }
    }
    unreadCounter.draw(c, rightX, height / 2f, Gravity.RIGHT, 1f)
  }

  fun attach () = emojiReceiver.attach()

  fun detach () = emojiReceiver.detach()

  fun performDestroy () {
    emojiReceiver.performDestroy()
    iconEmoji = null
  }

  companion object {
    private const val ROW_HEIGHT = 72f
    private const val ICON_SIZE = 48f
    private const val ICON_EMOJI_TEXT_SIZE = 34f
    private const val SUBTITLE_TEXT_SIZE = 14f
  }
}
