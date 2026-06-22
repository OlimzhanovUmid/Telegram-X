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
package org.thunderdog.challegram.telegram

import org.drinkless.tdlib.TdApi

interface DownloadsListUpdateListener {
  fun updateFileAddedToDownloads (fileDownload: TdApi.FileDownload, counts: TdApi.DownloadedFileCounts) {}
  fun updateFileDownload (fileId: Int, completeDate: Int, isPaused: Boolean, counts: TdApi.DownloadedFileCounts) {}
  fun updateFileDownloads (totalSize: Long, totalCount: Int, downloadedSize: Long) {}
  fun updateFileRemovedFromDownloads (fileId: Int, counts: TdApi.DownloadedFileCounts) {}
}
