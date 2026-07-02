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

import androidx.annotation.IntDef
import org.drinkless.tdlib.TdApi

interface ChatListListener {
  fun onChatChanged (chatList: TdlibChatList, chat: TdApi.Chat, index: Int, changeInfo: Tdlib.ChatChange) { }
  fun onChatAdded (chatList: TdlibChatList, chat: TdApi.Chat, atIndex: Int, changeInfo: Tdlib.ChatChange) { }
  fun onChatRemoved (chatList: TdlibChatList, chat: TdApi.Chat, fromIndex: Int, changeInfo: Tdlib.ChatChange) { }
  fun onChatMoved (chatList: TdlibChatList, chat: TdApi.Chat, fromIndex: Int, toIndex: Int, changeInfo: Tdlib.ChatChange) { }

  @Retention(AnnotationRetention.SOURCE)
  @IntDef(
    value = [
      ChangeFlags.ITEM_METADATA_CHANGED,
      ChangeFlags.ITEM_ADDED,
      ChangeFlags.ITEM_REMOVED,
      ChangeFlags.ITEM_MOVED
    ],
    flag = true
  )
  annotation class ChangeFlags {
    companion object {
      const val ITEM_METADATA_CHANGED: Int = 1
      const val ITEM_ADDED: Int = 1 shl 1
      const val ITEM_REMOVED: Int = 1 shl 2
      const val ITEM_MOVED: Int = 1 shl 3
    }
  }
  fun onChatListChanged (chatList: TdlibChatList, @ChangeFlags changeFlags: Int) { }

  fun onChatListStateChanged (
    chatList: TdlibChatList,
    @TdlibChatList.State newState: Int,
    @TdlibChatList.State oldState: Int
  ) { }

  @Retention(AnnotationRetention.SOURCE)
  @IntDef(
    ItemChangeType.TITLE,
    ItemChangeType.READ_INBOX,
    ItemChangeType.LAST_MESSAGE,
    ItemChangeType.DRAFT,
    ItemChangeType.UNREAD_AVAILABILITY_CHANGED,
    ItemChangeType.THEME
  )
  annotation class ItemChangeType {
    companion object {
      const val TITLE: Int = 0
      const val READ_INBOX: Int = 1
      const val LAST_MESSAGE: Int = 2
      const val DRAFT: Int = 3
      const val UNREAD_AVAILABILITY_CHANGED: Int = 4
      const val THEME: Int = 5
    }
  }
  fun onChatListItemChanged (chatList: TdlibChatList, chat: TdApi.Chat, @ItemChangeType changeType: Int) { }
}
