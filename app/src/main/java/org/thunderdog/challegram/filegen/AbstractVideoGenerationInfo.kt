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
 * File created on 10/06/2017
 */
package org.thunderdog.challegram.filegen

import androidx.annotation.Nullable
import org.thunderdog.challegram.mediaview.crop.CropState
import org.thunderdog.challegram.unsorted.Settings

interface AbstractVideoGenerationInfo {
  fun setVideoGenerationInfo(
    sourceFileId: Int,
    needMute: Boolean,
    videoLimit: Settings.VideoLimit,
    rotate: Int,
    startTime: Long,
    endTime: Long,
    noTranscoding: Boolean,
    @Nullable cropState: CropState?
  )

  companion object {
    const val PREFIX_ROTATE = "rotate"
    const val PREFIX_RANDOM = "random"
    const val PREFIX_LAST_MODIFIED = "modified"
    const val PREFIX_START = "start"
    const val PREFIX_END = "end"
    const val PREFIX_SOURCE_FILE_ID = "source"
    const val PREFIX_NO_TRANSCODING = "noconvert"
    const val PREFIX_QUALITY = "limit"
    const val PREFIX_FRAME_RATE = "fps"
    const val PREFIX_BITRATE = "bitrate"
    const val PREFIX_CROP = "crop"
  }
}
