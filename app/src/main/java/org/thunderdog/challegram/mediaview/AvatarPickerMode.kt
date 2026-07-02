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
 * File created on 19/11/2023 at 00:51
 */
package org.thunderdog.challegram.mediaview

import androidx.annotation.IntDef
import kotlin.annotation.AnnotationRetention
import kotlin.annotation.Retention

@Retention(AnnotationRetention.SOURCE)
@IntDef(
  AvatarPickerMode.NONE,
  AvatarPickerMode.PROFILE,
  AvatarPickerMode.GROUP,
  AvatarPickerMode.CHANNEL,
  AvatarPickerMode.BOT
)
annotation class AvatarPickerMode {
  companion object {
    const val NONE = 0
    const val PROFILE = 1
    const val GROUP = 2
    const val CHANNEL = 3
    const val BOT = 4
  }
}
