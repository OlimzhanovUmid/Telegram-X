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
package org.thunderdog.challegram.telegram

interface AnimatedEmojiListener {
  fun onAnimatedEmojiChanged (type: Int) { }

  companion object {
    const val TYPE_TGX = 0
    const val TYPE_EMOJI = 1
    const val TYPE_DICE = 2
  }
}
