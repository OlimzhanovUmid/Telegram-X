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
 * File created on 18/10/2022, 01:17.
 */
package org.thunderdog.challegram.telegram

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import org.drinkless.tdlib.TdApi

import java.util.Collection

@Suppress("EXPOSED_SUPER_CLASS", "EXPOSED_SUPER_INTERFACE")
internal class TdlibEmojiReactionsManager(tdlib: Tdlib) : TdlibDataManager<String, TdApi.EmojiReaction, TdlibEmojiReactionsManager.Entry>(tdlib) {
  class Entry(@NonNull key: String, @Nullable value: TdApi.EmojiReaction?, @Nullable error: TdApi.Error?) : AbstractEntry<String, TdApi.EmojiReaction>(key, value, error)

  interface Watcher : TdlibDataManager.Watcher<String, TdApi.EmojiReaction, TdlibEmojiReactionsManager.Entry>

  override fun newEntry(@NonNull key: String, @Nullable value: TdApi.EmojiReaction?, @Nullable error: TdApi.Error?): Entry {
    return Entry(key, value, error)
  }

  override fun requestData(contextId: Int, keysToRequest: MutableCollection<String>) {
    for (emoji in keysToRequest) {
      tdlib.client().send(TdApi.GetEmojiReaction(emoji)) { result ->
        when (result.constructor) {
          TdApi.EmojiReaction.CONSTRUCTOR -> {
            processData(contextId, emoji, result as TdApi.EmojiReaction)
          }
          TdApi.Error.CONSTRUCTOR -> {
            processError(contextId, emoji, result as TdApi.Error)
          }
        }
      }
    }
  }
}
