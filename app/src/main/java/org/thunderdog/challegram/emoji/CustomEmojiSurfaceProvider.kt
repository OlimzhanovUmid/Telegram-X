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
 * File created on 05/09/2022, 18:16.
 */

package org.thunderdog.challegram.emoji

import org.thunderdog.challegram.data.ComplexMediaItem
import org.thunderdog.challegram.loader.ComplexReceiver

interface CustomEmojiSurfaceProvider {
  fun onCreateNewSpan (emojiCode: CharSequence, info: EmojiInfo, customEmojiId: Long): EmojiSpan
  fun onInvalidateSpan (span: EmojiSpan, requiresLayoutUpdate: Boolean)
  fun provideComplexReceiverForSpan (span: EmojiSpan): ComplexReceiver
  fun getDuplicateMediaItemCount (span: EmojiSpan, mediaItem: ComplexMediaItem): Int
  fun attachToReceivers (span: EmojiSpan, complexMediaItem: ComplexMediaItem): Long
  fun detachFromReceivers (span: EmojiSpan, complexMediaItem: ComplexMediaItem, mediaKey: Long)
}
