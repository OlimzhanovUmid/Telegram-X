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
package org.thunderdog.challegram.player

import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import org.drinkless.tdlib.TdApi
import org.thunderdog.challegram.Log
import org.thunderdog.challegram.data.TD
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.telegram.TdlibManager

abstract class BasePlaybackController : TGPlayerController.TrackChangeListener, Player.Listener {
  private var isDestroyed = false

  final override fun onTrackChanged (tdlib: Tdlib, newTrack: TdApi.Message?, fileId: Int, state: Int, progress: Float, byUserRequest: Boolean) {
    val isPlaying = state == TGPlayerController.STATE_PLAYING
    val newMessage: TdApi.Message? = if (newTrack != null && state != TGPlayerController.STATE_NONE && isSupported(newTrack) && !isDestroyed) {
      newTrack
    } else {
      null
    }
    setPlaybackObject(tdlib, newMessage, isPlaying, byUserRequest)
  }

  @JvmField
  protected var tdlib: Tdlib? = null
  @JvmField
  protected var `object`: TdApi.Message? = null

  fun comparePlayingObject (tdlib: Tdlib, `object`: TdApi.Message): Boolean {
    val thisObject = this.`object`
    return this.tdlib != null && this.tdlib === tdlib && thisObject != null && thisObject.chatId == `object`.chatId && thisObject.id == `object`.id && TD.getFileId(thisObject) == TD.getFileId(`object`)
  }

  protected fun destroy () {
    isDestroyed = true
    if (`object` != null) {
      TdlibManager.instance().player().stopPlayback(false)
    }
  }

  protected fun stopPlayback () {
    if (`object` != null) {
      val oldObject = this.`object`
      this.`object` = null
      finishPlayback(tdlib, oldObject, false)
    }
  }

  fun getPlayingChatId (): Long {
    return if (`object` != null) `object`!!.chatId else 0
  }

  fun getPlayingMessageId (): Long {
    return if (`object` != null) `object`!!.id else 0
  }

  private fun setPlaybackObject (tdlib: Tdlib, `object`: TdApi.Message?, isPlaying: Boolean, byUserRequest: Boolean) {
    if (this.`object` == null && `object` == null) { // Nothing to do.
      return
    }

    if (`object` == null) { // We need to stop any playback & release resources
      val oldTdlib = this.tdlib
      val oldObject = this.`object`
      this.tdlib = tdlib
      this.`object` = null
      finishPlayback(oldTdlib, oldObject, byUserRequest)
      return
    }

    val thisObject = this.`object`
    val hadObject = this.tdlib === tdlib && thisObject != null && thisObject.chatId == `object`.id
    val previousFileId = if (thisObject != null) TD.getFileId(thisObject) else -1
    val previousTdlib = this.tdlib
    val equals = comparePlayingObject(tdlib, `object`)

    this.tdlib = tdlib
    this.`object` = `object`

    if (equals) { // Playback object not changed, simply play/pause.
      playPause(isPlaying)
    } else { // So, we actually need to initialize a new playback
      startPlayback(tdlib, `object`, byUserRequest, hadObject, previousTdlib, previousFileId)
    }
  }

  protected abstract fun isSupported (message: TdApi.Message): Boolean
  protected abstract fun finishPlayback (tdlib: Tdlib?, oldMessage: TdApi.Message?, byUserRequest: Boolean)
  protected abstract fun playPause (isPlaying: Boolean)
  protected abstract fun startPlayback (tdlib: Tdlib, message: TdApi.Message, byUserRequest: Boolean, hadObject: Boolean, previousTdlib: Tdlib?, previousFileId: Int)
  protected abstract fun displayPlaybackError (e: PlaybackException)

  override fun onPlayerError (e: PlaybackException) {
    Log.e(Log.TAG_PLAYER, "onPlayerError", e)
    if (`object` != null) {
      displayPlaybackError(e)
    }
  }
}
