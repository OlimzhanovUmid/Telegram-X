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
 * File created on 15/02/2018
 */
package org.thunderdog.challegram.telegram

import org.drinkless.tdlib.TdApi

interface StickersListener {
  fun onInstalledStickerSetsUpdated (stickerSetIds: LongArray, stickerType: TdApi.StickerType) { }
  fun onRecentStickersUpdated (stickerIds: IntArray, isAttached: Boolean) { }
  fun onFavoriteStickersUpdated (stickerIds: IntArray) { }
  fun onTrendingStickersUpdated (stickerType: TdApi.StickerType, stickerSets: TdApi.TrendingStickerSets, unreadCount: Int) { }
  fun onStickerSetUpdated (stickerSet: TdApi.StickerSet) { }
  fun onStickerSetArchived (stickerSet: TdApi.StickerSetInfo) { }
  fun onStickerSetRemoved (stickerSet: TdApi.StickerSetInfo) { }
  fun onStickerSetInstalled (stickerSet: TdApi.StickerSetInfo) { }
}
