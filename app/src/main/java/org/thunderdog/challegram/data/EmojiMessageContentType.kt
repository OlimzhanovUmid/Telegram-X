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
 * File created on 24/10/2023 at 02:14
 */
package org.thunderdog.challegram.data

import androidx.annotation.IntDef

@Retention(AnnotationRetention.SOURCE)
@IntDef(
  EmojiMessageContentType.NOT_EMOJI,
  EmojiMessageContentType.ANIMATED_EMOJI,
  EmojiMessageContentType.NON_BUBBLE_EMOJI
)
annotation class EmojiMessageContentType {
  companion object {
    const val NOT_EMOJI: Int = 0
    const val ANIMATED_EMOJI: Int = 1
    const val NON_BUBBLE_EMOJI: Int = 2
  }
}
