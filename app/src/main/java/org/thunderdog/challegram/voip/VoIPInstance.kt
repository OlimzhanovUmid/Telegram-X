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

import android.os.SystemClock

import androidx.annotation.Keep

import org.drinkless.tdlib.TdApi
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.voip.annotation.CallNetworkType
import org.thunderdog.challegram.voip.annotation.CallState

import me.vkryl.core.lambda.Destroyable

abstract class VoIPInstance (
  @JvmField protected val tdlib: Tdlib,
  @JvmField protected val call: TdApi.Call,
  @JvmField protected val configuration: CallConfiguration,
  @JvmField protected val options: CallOptions,
  @JvmField protected val connectionStateListener: ConnectionStateListener
) : Destroyable {

  abstract fun initializeAndConnect ()

  // Getters

  fun tdlib (): Tdlib {
    return tdlib
  }

  fun getCall (): TdApi.Call {
    return call
  }

  fun getConfiguration (): CallConfiguration {
    return configuration
  }

  fun getOptions (): CallOptions {
    return options
  }

  // Connection state

  private var callStartTime: Long = 0

  protected fun dispatchCallStateChanged (@CallState state: Int) {
    // this.callState = state;
    if (state == CallState.ESTABLISHED && callStartTime == 0L) {
      callStartTime = SystemClock.elapsedRealtime()
    }
    connectionStateListener.onConnectionStateChanged(this, state)
  }

  fun getCallDuration (): Long {
    return if (callStartTime != 0L) SystemClock.elapsedRealtime() - callStartTime else DURATION_UNKNOWN
  }

  // Setters

  fun setAudioOutputGainControlEnabled (isEnabled: Boolean) {
    options.audioGainControlEnabled = isEnabled
    handleAudioOutputGainControlEnabled(isEnabled)
  }
  protected abstract fun handleAudioOutputGainControlEnabled (isEnabled: Boolean)

  fun setEchoCancellationStrength (strength: Int) {
    options.echoCancellationStrength = strength
    handleEchoCancellationStrengthChange(strength)
  }
  protected abstract fun handleEchoCancellationStrengthChange (strength: Int)

  fun setMicDisabled (isDisabled: Boolean) {
    options.isMicDisabled = isDisabled
    handleMicDisabled(isDisabled)
  }
  protected abstract fun handleMicDisabled (isDisabled: Boolean)

  open fun setNetworkType (@CallNetworkType type: Int) {
    options.networkType = type
    handleNetworkTypeChange(type)
  }

  protected abstract fun handleNetworkTypeChange (@CallNetworkType type: Int)

  // Getters

  abstract fun collectDebugLog (): CharSequence?
  abstract fun getConnectionId (): Long
  abstract fun getNetworkStats (out: NetworkStats)

  abstract fun getLibraryName (): String
  abstract fun getLibraryVersion (): String

  // called from native code

  @Keep
  protected fun handleStateChange (@CallState state: Int) {
    dispatchCallStateChanged(state)
  }

  @Keep
  protected fun handleSignalBarsChange (count: Int) {
    connectionStateListener.onSignalBarCountChanged(count)
  }

  @Keep
  protected fun handleEmittedSignalingData (buffer: ByteArray) {
    connectionStateListener.onSignallingDataEmitted(buffer)
  }

  // called from TDLib

  abstract fun handleIncomingSignalingData (buffer: ByteArray)

  companion object {
    @JvmField var DURATION_UNKNOWN: Long = -1
  }
}
