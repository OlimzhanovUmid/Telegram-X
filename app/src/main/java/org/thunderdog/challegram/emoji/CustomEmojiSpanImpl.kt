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
 * File created on 05/09/2022, 16:39.
 */

package org.thunderdog.challegram.emoji

import android.graphics.Canvas
import android.graphics.Rect
import android.text.Layout
import android.view.View

import androidx.annotation.UiThread

import org.thunderdog.challegram.data.ComplexMediaItem
import org.thunderdog.challegram.data.ComplexMediaItemCustomEmoji
import org.thunderdog.challegram.loader.ComplexReceiver
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.telegram.TdlibEmojiManager
import org.thunderdog.challegram.tool.Views
import org.thunderdog.challegram.util.text.TextMedia

import me.vkryl.core.lambda.Destroyable

class CustomEmojiSpanImpl (
  info: EmojiInfo?,
  private val surfaceProvider: CustomEmojiSurfaceProvider,
  private val tdlib: Tdlib,
  private val customEmojiIdValue: Long
) : EmojiSpanImpl(info), TdlibEmojiManager.Watcher, PreserveCustomEmojiFilter.RecoverableSpan, Destroyable {
  private var customEmoji: TdlibEmojiManager.Entry? = null
  private var emojiRequested = false
  private var isDestroyed = false

  private val drawRect = Rect()

  private var customEmojiSize = 0
  private var attachedToMediaKey = -1L
  private var mediaItem: ComplexMediaItem? = null

  init {
    if (customEmojiIdValue != 0L) {
      applyCustomEmoji(tdlib.emoji().findOrPostponeRequest(customEmojiIdValue, this))
    }
  }

  override fun belongsToSurface (customEmojiSurfaceProvider: CustomEmojiSurfaceProvider): Boolean {
    return this.surfaceProvider === customEmojiSurfaceProvider && !isDestroyed
  }

  override fun isCustomEmoji (): Boolean {
    val customEmoji = this.customEmoji
    return customEmoji == null || !customEmoji.isNotFound
  }

  override fun getCustomEmojiId (): Long {
    return customEmojiIdValue
  }

  override fun performDestroy () {
    if (isDestroyed)
      return
    isDestroyed = true
    tdlib.emoji().forgetWatcher(customEmojiIdValue, this)
    prepareCustomEmoji(0)
  }

  @UiThread
  private fun applyCustomEmoji (customEmoji: TdlibEmojiManager.Entry?) {
    if (this.customEmoji === customEmoji)
      return
    this.customEmoji = customEmoji
    if (size.isInitialized()) {
      prepareCustomEmoji(size.getSize())
      surfaceProvider.onInvalidateSpan(this, customEmoji != null && customEmoji.isNotFound)
    }
  }

  override fun onCustomEmojiLoaded (context: TdlibEmojiManager, entry: TdlibEmojiManager.Entry) {
    tdlib.ui().post {
      if (!isDestroyed) {
        applyCustomEmoji(entry)
      }
    }
  }

  override fun drawEmoji (canvas: Canvas, centerX: Float, centerY: Float, emojiSize: Int) {
    val left = (centerX - emojiSize / 2f).toInt()
    val top = (centerY - emojiSize / 2f).toInt()
    val right = left + emojiSize
    val bottom = top + emojiSize
    drawRect.set(left, top, right, bottom)
    prepareCustomEmoji(emojiSize)
    val customEmoji = this.customEmoji
    if (customEmoji != null && customEmoji.isNotFound) {
      super.drawEmoji(canvas, drawRect.centerX().toFloat(), drawRect.centerY().toFloat(), size.getSize())
    }
  }

  private fun drawCustomEmoji (c: Canvas) {
    val customEmoji = this.customEmoji
    if (customEmoji == null || customEmoji.isNotFound) {
      return
    }

    val scale = TextMedia.getScale(customEmoji.value, customEmojiSize)
    val needScale = scale != 1f

    val restoreToCount: Int
    if (needScale) {
      restoreToCount = Views.save(c)
      c.scale(scale, scale, drawRect.centerX().toFloat(), drawRect.centerY().toFloat())
    } else {
      restoreToCount = -1
    }
    val receiver = surfaceProvider.provideComplexReceiverForSpan(this)
    val mediaItem = this.mediaItem!!
    val haveDuplicateMedia = surfaceProvider.getDuplicateMediaItemCount(this, mediaItem) > 1
    mediaItem.draw(c,
      drawRect,
      receiver,
      attachedToMediaKey,
      haveDuplicateMedia
    )
    if (needScale) {
      Views.restore(c, restoreToCount)
    }
  }

  override fun onOverlayDraw (c: Canvas, view: View, layout: Layout) {
    val customEmoji = this.customEmoji
    if (customEmoji == null || customEmoji.isNotFound || isDestroyed) {
      return
    }
    if (drawRect.left == drawRect.right || drawRect.top == drawRect.bottom) {
      return // force invalidate()?
    }
    if (customEmojiSize != size.getSize() && size.getSize() > 0) {
      prepareCustomEmoji(size.getSize())
    }
    val paddingLeft = view.getPaddingLeft()
    val paddingTop = view.getPaddingTop()
    val translate = paddingLeft != 0 || paddingTop != 0
    if (translate) {
      drawRect.offset(paddingLeft, paddingTop)
    }
    drawCustomEmoji(c)
    if (translate) {
      drawRect.offset(-paddingLeft, -paddingTop)
    }
  }

  override fun requestCustomEmoji (receiver: ComplexReceiver, mediaKey: Int) {
    if (isDestroyed)
      throw IllegalStateException()
    if (this.attachedToMediaKey != mediaKey.toLong())
      throw IllegalArgumentException()
    val mediaItem = this.mediaItem
    if (mediaItem != null) {
      mediaItem.requestComplexMedia(receiver, mediaKey.toLong())
    } else {
      receiver.clearReceivers(mediaKey.toLong())
      requestCustomEmoji()
    }
  }

  private fun requestCustomEmoji () {
    if (customEmoji == null && !emojiRequested) {
      emojiRequested = true
      tdlib.emoji().performPostponedRequestsDelayed()
    }
  }

  private fun prepareCustomEmoji (size: Int) {
    if (customEmojiSize == size) {
      return
    }
    val currentMediaItem = mediaItem
    if (currentMediaItem != null) {
      surfaceProvider.detachFromReceivers(this, currentMediaItem, attachedToMediaKey)
      mediaItem = null
      attachedToMediaKey = -1L
      customEmojiSize = 0
    }
    if (!isDestroyed && size > 0) {
      val customEmoji = this.customEmoji
      if (customEmoji == null) {
        requestCustomEmoji()
      } else if (!customEmoji.isNotFound) {
        customEmojiSize = size
        val newMediaItem = ComplexMediaItemCustomEmoji(tdlib, customEmoji.value, size)
        mediaItem = newMediaItem
        attachedToMediaKey = surfaceProvider.attachToReceivers(this, newMediaItem)
      }
    }
  }
}
