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
 * File created on 21/05/2015 at 19:54
 */
package org.thunderdog.challegram.util

import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi

abstract class CancellableResultHandler : Client.ResultHandler {
  @Volatile private var isCancelled = false

  fun cancel () {
    synchronized(this) {
      isCancelled = true
    }
  }

  final override fun onResult (`object`: TdApi.Object) {
    synchronized(this) {
      if (!isCancelled) {
        processResult(`object`)
      }
    }
  }

  fun isCancelled (): Boolean {
    synchronized(this) {
      return isCancelled
    }
  }

  abstract fun processResult (`object`: TdApi.Object)
}
