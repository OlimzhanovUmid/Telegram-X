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
package org.thunderdog.challegram.voip

import androidx.annotation.Nullable

import org.thunderdog.challegram.voip.annotation.AudioState
import org.thunderdog.challegram.voip.annotation.CallState
import org.thunderdog.challegram.voip.annotation.VideoState

interface ConnectionStateListener {
  fun onConnectionStateChanged (context: VoIPInstance, @CallState newState: Int) { }

  fun onSignalBarCountChanged (newCount: Int) { }

  fun onStopped (releasedContext: VoIPInstance, finalStats: NetworkStats, @Nullable debugLog: String?) { }

  fun onRemoteMediaStateChanged (context: VoIPInstance, @AudioState audioState: Int, @VideoState videoState: Int) { }

  fun onSignallingDataEmitted (data: ByteArray) { }

  fun onGroupCallKeyReceived (key: ByteArray) { }

  fun onGroupCallKeySent () { }

  fun onCallUpgradeRequestReceived () { }
}
