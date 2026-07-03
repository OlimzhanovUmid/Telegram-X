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
 * File created on 02/09/2023
 */
package org.thunderdog.challegram.util

import me.vkryl.core.collection.LongSparseIntArray
import org.drinkless.tdlib.TdApi
import org.thunderdog.challegram.component.sticker.TGStickerObj
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.tool.UI

abstract class StickerSetsDataProvider (private val tdlib: Tdlib) : TGStickerObj.DataProvider {
  companion object {
    const val FLAG_TRENDING = 0x01
    const val FLAG_REGULAR = 0x02
  }

  private val loadingStickerSets = LongSparseIntArray()

  override fun requestStickerData (sticker: TGStickerObj, stickerSetId: Long) {
    if (needIgnoreRequests(stickerSetId, sticker)) {
      return
    }

    val currentFlags = loadingStickerSets.get(stickerSetId, 0)
    val loadingFlags = getLoadingFlags(stickerSetId, sticker)
    val needRequestData = ((currentFlags xor loadingFlags) and loadingFlags) != 0

    loadingStickerSets.put(stickerSetId, currentFlags or loadingFlags)

    if (needRequestData) {
      tdlib.send(TdApi.GetStickerSet(stickerSetId), singleStickerSetHandler())
    }
  }

  fun clear () {
    loadingStickerSets.clear()
  }

  private fun singleStickerSetHandler (): Tdlib.ResultHandler<TdApi.StickerSet?> {
    return Tdlib.ResultHandler { stickerSet, error ->
      if (stickerSet != null) {
        UI.post {
          val flags = loadingStickerSets.get(stickerSet.id)
          loadingStickerSets.delete(stickerSet.id)
          applyStickerSet(stickerSet, flags)
        }
      } else {
        UI.showError(error)
      }
    }
  }

  protected abstract fun needIgnoreRequests (stickerSetId: Long, stickerObj: TGStickerObj): Boolean
  protected abstract fun getLoadingFlags (stickerSetId: Long, stickerObj: TGStickerObj): Int
  protected abstract fun applyStickerSet (stickerSet: TdApi.StickerSet, flags: Int)
}
