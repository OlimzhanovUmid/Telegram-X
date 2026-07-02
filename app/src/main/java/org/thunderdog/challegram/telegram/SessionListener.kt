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

import org.drinkless.tdlib.TdApi

fun interface SessionListener {
  fun onSessionListChanged (tdlib: Tdlib, isWeakGuess: Boolean)
  fun onSessionTerminated (tdlib: Tdlib, session: TdApi.Session) {
    onSessionListChanged(tdlib, false)
  }
  fun onAllOtherSessionsTerminated (tdlib: Tdlib, currentSession: TdApi.Session) {
    onSessionListChanged(tdlib, false)
  }
  fun onSessionCreatedViaQrCode (tdlib: Tdlib, session: TdApi.Session) {
    onSessionListChanged(tdlib, false)
  }
  fun onInactiveSessionTtlChanged (tdlib: Tdlib, ttlDays: Int) {
    onSessionListChanged(tdlib, false)
  }
}
