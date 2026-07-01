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
 * File created on 09/02/2024 at 18:34
 */
package org.thunderdog.challegram.component.attach

import org.thunderdog.challegram.loader.ImageGalleryFile
import org.thunderdog.challegram.navigation.ViewController
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.util.Permissions
import me.vkryl.core.lambda.RunnableBool
import me.vkryl.core.lambda.RunnableData

abstract class MediaLayoutManager(@JvmField protected val context: ViewController<*>) {
  @JvmField protected val tdlib: Tdlib = context.tdlib()!!
  @JvmField protected var currentMediaLayout: MediaLayout? = null
  @JvmField protected var openingMediaLayout: Boolean = false

  protected fun waitPermissionsForOpen(onOpen: RunnableBool) {
    waitPermissionsForOpen(false, false, onOpen)
  }

  private fun waitPermissionsForOpen(ignorePermissionRequest: Boolean, noMedia: Boolean, onOpen: RunnableBool) {
    if (openingMediaLayout || currentMediaLayout?.isVisible == true) {
      return
    }

    if (!ignorePermissionRequest && context.context().permissions()
        .requestReadExternalStorage(Permissions.ReadType.IMAGES_AND_VIDEOS) { grantType ->
          waitPermissionsForOpen(true, grantType == Permissions.GrantResult.NONE, onOpen)
        }
    ) {
      return
    }

    onOpen.runWithBool(!noMedia)
  }

  companion object {
    @JvmStatic
    fun singleMediaCallback(callback: RunnableData<ImageGalleryFile>): MediaLayout.MediaGalleryCallback {
      return object : MediaLayout.MediaGalleryCallback {
        override fun onSendVideo(file: ImageGalleryFile, isFirst: Boolean) {
          if (!isFirst) return
          callback.runWithData(file)
        }

        override fun onSendPhoto(file: ImageGalleryFile, isFirst: Boolean) {
          if (!isFirst) return
          callback.runWithData(file)
        }
      }
    }
  }
}
