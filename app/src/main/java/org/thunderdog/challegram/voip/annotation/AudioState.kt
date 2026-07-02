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
 * File created on 01/04/2023
 */
package org.thunderdog.challegram.voip.annotation

import androidx.annotation.IntDef
import kotlin.annotation.AnnotationRetention
import kotlin.annotation.Retention

@Retention(AnnotationRetention.SOURCE)
@IntDef(
  AudioState.MUTED,
  AudioState.ACTIVE
)
annotation class AudioState {
  companion object {
    const val MUTED = 0
    const val ACTIVE = 1
  }
}
