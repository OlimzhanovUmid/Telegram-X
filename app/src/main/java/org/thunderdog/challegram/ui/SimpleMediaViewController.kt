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
 * File created on 10/03/2018
 */
package org.thunderdog.challegram.ui

import android.content.Context
import android.view.View
import org.drinkless.tdlib.TdApi
import org.thunderdog.challegram.R
import org.thunderdog.challegram.data.TD
import org.thunderdog.challegram.loader.ImageFile
import org.thunderdog.challegram.mediaview.MediaCellView
import org.thunderdog.challegram.mediaview.data.MediaItem
import org.thunderdog.challegram.navigation.ViewController
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.widget.ForceTouchView

class SimpleMediaViewController (context: Context, tdlib: Tdlib?) : ViewController<SimpleMediaViewController.Args>(context, tdlib), ForceTouchView.PreviewDelegate {
  class Args {
    @JvmField val mode: Int

    @JvmField var photo: TdApi.Photo? = null
    @JvmField var animation: TdApi.Animation? = null
    @JvmField var previewFile: ImageFile? = null
    @JvmField var targetFile: ImageFile? = null

    @JvmField var profilePhoto: TdApi.ProfilePhoto? = null
    @JvmField var chatPhotoInfo: TdApi.ChatPhotoInfo? = null
    @JvmField var chatPhoto: TdApi.ChatPhoto? = null
    @JvmField var dataId: Long = 0

    constructor (photo: TdApi.Photo, previewFile: ImageFile?, targetFile: ImageFile?) {
      this.mode = SimpleMediaViewController.MODE_PHOTO
      this.photo = photo
      this.previewFile = previewFile
      this.targetFile = targetFile
    }

    constructor (animation: TdApi.Animation, previewFile: ImageFile?) {
      this.mode = SimpleMediaViewController.MODE_GIF
      this.animation = animation
      this.previewFile = previewFile
    }

    constructor (profilePhoto: TdApi.ProfilePhoto, userId: Long) {
      this.mode = SimpleMediaViewController.MODE_PROFILE_PHOTO
      this.profilePhoto = profilePhoto
      this.dataId = userId
    }

    constructor (chatPhotoInfo: TdApi.ChatPhotoInfo, chatId: Long) {
      this.mode = SimpleMediaViewController.MODE_CHAT_PHOTO
      this.chatPhotoInfo = chatPhotoInfo
      this.dataId = chatId
    }

    constructor (chatPhoto: TdApi.ChatPhoto) {
      this.mode = SimpleMediaViewController.MODE_CHAT_PHOTO_FULL
      this.chatPhoto = chatPhoto
    }
  }

  override fun getId (): Int {
    return R.id.controller_media_simple
  }

  private var cellView: MediaCellView? = null

  override fun onCreateView (context: Context): View {
    val cellView = MediaCellView(context())
    cellView.setBoundForceTouchContext(null)
    this.cellView = cellView

    var mediaItem: MediaItem? = null

    val args = getArgumentsStrict()
    when (args.mode) {
      MODE_PHOTO -> {
        mediaItem = MediaItem.valueOf(context(), tdlib, args.photo, null)
        if (mediaItem.isLoaded()) {
          mediaItem.setPreviewImageFile(args.targetFile)
        } else {
          mediaItem.setPreviewImageFile(args.previewFile)
        }
      }
      MODE_GIF -> {
        if (TD.isFileLoaded(args.animation!!.animation)) {
          cellView.setEnableEarlyLoad()
        }
        mediaItem = MediaItem.valueOf(context(), tdlib, args.animation, null)
      }
      MODE_PROFILE_PHOTO -> {
        mediaItem = MediaItem(context(), tdlib, args.dataId, args.profilePhoto)
      }
      MODE_CHAT_PHOTO -> {
        mediaItem = MediaItem(context(), tdlib, args.dataId, args.chatPhotoInfo)
      }
      MODE_CHAT_PHOTO_FULL -> {
        mediaItem = MediaItem(context(), tdlib, args.dataId, 0, args.chatPhoto)
      }
    }
    if (mediaItem == null) {
      throw IllegalArgumentException()
    }
    mediaItem.download(true)
    cellView.setMedia(mediaItem)
    executeScheduledAnimation()
    return cellView
  }

  override fun onPrepareForceTouchContext (context: ForceTouchView.ForceTouchContext) {
    cellView!!.setBoundForceTouchContext(context)
    context.setAllowFullscreen(true)
    context.setStateListener(cellView)
    context.setBackgroundColor(0x70000000)
  }

  override fun wouldHideKeyboardInForceTouchMode (): Boolean {
    return false
  }

  override fun destroy () {
    super.destroy()
    cellView!!.destroy()
  }

  companion object {
    const val MODE_PHOTO: Int = 0
    const val MODE_GIF: Int = 1
    const val MODE_PROFILE_PHOTO: Int = 2
    const val MODE_CHAT_PHOTO: Int = 3
    const val MODE_CHAT_PHOTO_FULL: Int = 4
  }
}
