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
 * File created on 08/12/2016
 */
package org.thunderdog.challegram.filegen

import android.os.SystemClock
import org.thunderdog.challegram.U
import org.thunderdog.challegram.config.Config

abstract class GenerationInfo(@JvmField val generationId: Long, @JvmField val originalPath: String, @JvmField val destinationPath: String?, @JvmField val conversion: String?, hasThumb: Boolean) {
  val key: String
    get() {
      val b = StringBuilder(originalPath)
      if (conversion != null) {
        b.append('?')
        b.append(conversion)
      }
      return b.toString()
    }

  override fun toString(): String {
    return this.key
  }

  private var onCancel: Runnable? = null

  fun setOnCancel(onCancel: Runnable?) {
    synchronized(this) {
      this.onCancel = onCancel
    }
  }

  fun cancel() {
    synchronized(this) {
      if (onCancel != null) {
        onCancel!!.run()
        onCancel = null
      }
    }
  }

  companion object {
    const val TYPE_PHOTO: String = "photo"
    const val TYPE_PHOTO_THUMB: String = "pthumb"
    const val TYPE_VIDEO_THUMB: String = "vthumb"
    const val TYPE_MUSIC_THUMB: String = "mthumb"
    const val TYPE_VIDEO: String = "video"
    const val TYPE_AVATAR: String = "avatar"
    const val TYPE_LOTTIE_STICKER_PREVIEW: String = "asthumb"
    const val TYPE_VIDEO_STICKER_PREVIEW: String = "vsthumb"

    @JvmStatic
    fun randomStamp(): String {
      return SystemClock.uptimeMillis().toString() + "_" + System.currentTimeMillis() + "_" + Math.random()
    }

    @JvmStatic
    fun lastModified(path: String?): Long {
      return if (Config.DISABLE_SENDING_MEDIA_CACHE) System.currentTimeMillis() else if (Config.WORKAROUND_NEED_MODIFY) U.getLastModifiedTime(path) else 0
    }
  }
}
