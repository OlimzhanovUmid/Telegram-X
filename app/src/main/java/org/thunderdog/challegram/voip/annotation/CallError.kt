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
 * File created on 28/03/2023
 */
package org.thunderdog.challegram.voip.annotation

import androidx.annotation.IntDef
import kotlin.annotation.AnnotationRetention
import kotlin.annotation.Retention

@Retention(AnnotationRetention.SOURCE)
@IntDef(
  CallError.UNKNOWN,
  CallError.INCOMPATIBLE,
  CallError.TIMEOUT,
  CallError.AUDIO_IO,
  CallError.PROXY,

  CallError.PEER_OUTDATED,
  CallError.PRIVACY,
  CallError.LOCALIZED,
  CallError.INSECURE_UPGRADE,
  CallError.CONNECTION_SERVICE
)
annotation class CallError {
  companion object {
    // VoIPController.h:70
    const val UNKNOWN = 0
    const val INCOMPATIBLE = 1
    const val TIMEOUT = 2
    const val AUDIO_IO = 3
    const val PROXY = 4

    // local error codes (unused)

    const val PEER_OUTDATED = -1
    const val PRIVACY = -2
    const val LOCALIZED = -3
    const val INSECURE_UPGRADE = -4
    const val CONNECTION_SERVICE = -5
  }
}
