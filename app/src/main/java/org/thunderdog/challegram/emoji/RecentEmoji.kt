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

import org.thunderdog.challegram.tool.Strings

class RecentEmoji (
  @JvmField val emoji: String,
  @JvmField val info: RecentInfo
) {
  val isCustomEmoji: Boolean =
    emoji.startsWith(Emoji.CUSTOM_EMOJI_CACHE) || emoji.startsWith(Emoji.CUSTOM_EMOJI_CACHE_OLD)

  @JvmField val customEmojiId: Long = if (isCustomEmoji) {
    try {
      Strings.getNumber(emoji).toLong()
    } catch (ignored: Throwable) {
      0L
    }
  } else {
    0L
  }
}
