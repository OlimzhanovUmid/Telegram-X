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
 * File created on 01/03/2016 at 19:45
 */
package org.thunderdog.challegram.loader.gif

import android.os.Message
import org.thunderdog.challegram.core.BaseThread

class GifThread(index: Int) : BaseThread("GifThread#$index") {
  override fun process (msg: Message) {
    when (msg.what) {
      START_DECODING -> {
        val obj = msg.obj as Array<Any?>
        (obj[0] as GifActor).startDecoding(obj[1] as String)
        obj[0] = null
        obj[1] = null
      }
      PREPARE_NEXT_FRAME -> (msg.obj as GifActor).prepareNextFrame()
      PREPARE_START_FRAME -> (msg.obj as GifActor).prepareStartFrame()
      DESTROY -> (msg.obj as GifActor).onDestroy()
    }
  }

  fun startDecoding (actor: GifActor, path: String) {
    sendMessage(Message.obtain(handler, START_DECODING, arrayOf(actor, path)), 0)
  }

  fun prepareStartFrame (actor: GifActor) {
    sendMessage(Message.obtain(handler, PREPARE_START_FRAME, actor), 0)
  }

  fun prepareNextFrame (actor: GifActor) {
    sendMessage(Message.obtain(handler, PREPARE_NEXT_FRAME, actor), 0)
  }

  fun onDestroy (actor: GifActor) {
    sendMessage(Message.obtain(handler, DESTROY, actor), 0)
  }

  companion object {
    private const val START_DECODING = 0
    private const val PREPARE_NEXT_FRAME = 1
    private const val PREPARE_START_FRAME = 2
    private const val DESTROY = 3
  }
}
