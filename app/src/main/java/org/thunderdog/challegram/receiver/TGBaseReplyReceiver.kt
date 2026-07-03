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
 * File created on 08/04/2017
 */
package org.thunderdog.challegram.receiver

import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.thunderdog.challegram.telegram.TdlibManager
import org.thunderdog.challegram.telegram.TdlibNotificationExtras

abstract class TGBaseReplyReceiver : BroadcastReceiver() {
  companion object {
    const val EXTRA_VOICE_REPLY = "extra_voice_reply"
  }

  override fun onReceive (context: Context, intent: Intent) {
    val remoteInput = RemoteInput.getResultsFromIntent(intent) ?: return
    val text = remoteInput.getCharSequence(EXTRA_VOICE_REPLY)
    val extras = TdlibNotificationExtras.parse(intent.extras)
    TdlibManager.performExternalReply(context, text, extras)
  }
}
