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
 * File created on 27/03/2023
 */
package org.thunderdog.challegram.voip.annotation

import androidx.annotation.IntDef
import kotlin.annotation.AnnotationRetention
import kotlin.annotation.Retention

@Retention(AnnotationRetention.SOURCE)
@IntDef(
  CallState.WAIT_INIT,
  CallState.WAIT_INIT_ACK,
  CallState.ESTABLISHED,
  CallState.FAILED,
  CallState.RECONNECTING
)
annotation class CallState {
  companion object {
    // enum from VoIPController.h:62
    const val WAIT_INIT = 1
    const val WAIT_INIT_ACK = 2
    const val ESTABLISHED = 3
    const val FAILED = 4
    const val RECONNECTING = 5
  }
}
