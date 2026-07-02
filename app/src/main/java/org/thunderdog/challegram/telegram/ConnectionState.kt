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
 * File created on 21/02/2018
 */
package org.thunderdog.challegram.telegram

import androidx.annotation.IntDef
import kotlin.annotation.Retention
import kotlin.annotation.AnnotationRetention

@Retention(AnnotationRetention.SOURCE)
@IntDef(
  ConnectionState.UNKNOWN,
  ConnectionState.CONNECTED,
  ConnectionState.CONNECTING_TO_PROXY,
  ConnectionState.CONNECTING,
  ConnectionState.UPDATING,
  ConnectionState.WAITING_FOR_NETWORK
)
annotation class ConnectionState {
  companion object {
    const val UNKNOWN = -1
    const val CONNECTED = 0
    const val CONNECTING_TO_PROXY = 1
    const val CONNECTING = 2
    const val UPDATING = 3
    const val WAITING_FOR_NETWORK = 4
  }
}
