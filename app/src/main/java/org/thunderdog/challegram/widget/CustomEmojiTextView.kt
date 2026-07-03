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
 * File created on 08/05/2024
 */
package org.thunderdog.challegram.widget

import android.content.Context
import android.graphics.Canvas

import org.thunderdog.challegram.config.Config
import org.thunderdog.challegram.data.ComplexMediaHolder
import org.thunderdog.challegram.data.ComplexMediaItem
import org.thunderdog.challegram.emoji.CustomEmojiSurfaceProvider
import org.thunderdog.challegram.emoji.Emoji
import org.thunderdog.challegram.emoji.EmojiInfo
import org.thunderdog.challegram.emoji.EmojiSpan
import org.thunderdog.challegram.emoji.EmojiUpdater
import org.thunderdog.challegram.loader.ComplexReceiver
import org.thunderdog.challegram.receiver.RefreshRateLimiter
import org.thunderdog.challegram.telegram.Tdlib

@Suppress("ViewConstructor")
class CustomEmojiTextView (context: Context, private val tdlib: Tdlib?) : EmojiTextView(context), CustomEmojiSurfaceProvider {
  private val refreshRateLimiter = RefreshRateLimiter(this, Config.MAX_ANIMATED_EMOJI_REFRESH_RATE)
  private val mediaHolder = ComplexMediaHolder<EmojiSpan>(this)

  init {
    mediaHolder.setUpdateListener { _, _ ->
      refreshRateLimiter.invalidate()
    }
  }

  override fun onCreateNewSpan (emojiCode: CharSequence, info: EmojiInfo, customEmojiId: Long): EmojiSpan? {
    return if (tdlib != null) {
      Emoji.instance().newCustomSpan(emojiCode, info, this, tdlib, customEmojiId)
    } else {
      null
    }
  }

  override fun onInvalidateSpan (span: EmojiSpan, requiresLayoutUpdate: Boolean) {
    if (requiresLayoutUpdate) {
      EmojiUpdater.invalidateEmojiSpan(this, span)
    }
    invalidate()
  }

  override fun provideComplexReceiverForSpan (span: EmojiSpan): ComplexReceiver {
    return mediaHolder.receiver
  }

  override fun getDuplicateMediaItemCount (span: EmojiSpan, mediaItem: ComplexMediaItem): Int {
    return mediaHolder.getMediaUsageCount(mediaItem)
  }

  override fun attachToReceivers (span: EmojiSpan, complexMediaItem: ComplexMediaItem): Long {
    return mediaHolder.attachMediaUsage(complexMediaItem, span)
  }

  override fun detachFromReceivers (span: EmojiSpan, complexMediaItem: ComplexMediaItem, mediaKey: Long) {
    mediaHolder.detachMediaUsage(complexMediaItem, span, mediaKey)
  }

  override fun performDestroy () {
    super.performDestroy()
    mediaHolder.performDestroy()
  }

  private fun drawEmojiOverlay (c: Canvas) {
    val layout = layout
    for (span in mediaHolder.defaultLayerUsages()) {
      span.onOverlayDraw(c, this, layout)
    }
    for (span in mediaHolder.topLayerUsages()) {
      span.onOverlayDraw(c, this, layout)
    }
  }

  override fun onDraw (c: Canvas) {
    super.onDraw(c)
    drawEmojiOverlay(c)
  }
}
