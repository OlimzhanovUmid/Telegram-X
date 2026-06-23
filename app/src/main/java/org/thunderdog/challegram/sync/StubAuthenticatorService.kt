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
 * File created on 25/01/2019
 */
package org.thunderdog.challegram.sync

import android.app.Service
import android.content.Intent
import android.os.IBinder

class StubAuthenticatorService : Service() {
  private lateinit var authenticator: StubAuthenticator

  override fun onCreate () {
    super.onCreate()
    authenticator = StubAuthenticator(this)
  }

  override fun onBind (intent: Intent): IBinder? {
    return authenticator.iBinder
  }
}
