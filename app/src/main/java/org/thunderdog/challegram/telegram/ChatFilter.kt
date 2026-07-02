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
 * File created on 21/12/2019
 */
package org.thunderdog.challegram.telegram

import org.drinkless.tdlib.TdApi
import org.thunderdog.challegram.R

import me.vkryl.core.lambda.Filter

interface ChatFilter : Filter<TdApi.Chat> {
  fun getTotalStringRes (): Int = R.string.xChats
  fun getEmptyStringRes (): Int = R.string.NoChats
  fun canFilterMessages (): Boolean = false
  fun getMessagesStringRes (isArchive: Boolean): Int = R.string.general_Messages

  companion object {
    @JvmStatic
    fun gamesFilter (tdlib: Tdlib): ChatFilter {
      return object : ChatFilter {
        override fun accept (chat: TdApi.Chat): Boolean {
          return chat.type.constructor != TdApi.ChatTypeSecret.CONSTRUCTOR && !tdlib.isChannelChat(chat)
        }
      }
    }

    @JvmStatic
    fun groupsInviteFilter (tdlib: Tdlib): ChatFilter {
      return object : ChatFilter {
        override fun accept (chat: TdApi.Chat): Boolean {
          return tdlib.isMultiChat(chat) && tdlib.canInviteUsers(chat)
        }

        override fun getTotalStringRes (): Int {
          return R.string.xGroups
        }

        override fun getEmptyStringRes (): Int {
          return R.string.NoGroupsToShow
        }
      }
    }

    @JvmStatic
    fun groupsFilter (tdlib: Tdlib): ChatFilter {
      return object : ChatFilter {
        override fun accept (chat: TdApi.Chat): Boolean {
          return tdlib.isMultiChat(chat)
        }

        override fun getTotalStringRes (): Int {
          return R.string.xGroups
        }

        override fun getEmptyStringRes (): Int {
          return R.string.NoGroups
        }

        override fun getMessagesStringRes (isArchive: Boolean): Int {
          return if (isArchive) R.string.MessagesArchiveGroups else R.string.MessagesGroups
        }

        override fun canFilterMessages (): Boolean {
          return true
        }
      }
    }

    @JvmStatic
    fun channelsFilter (tdlib: Tdlib): ChatFilter {
      return object : ChatFilter {
        override fun accept (chat: TdApi.Chat): Boolean {
          return tdlib.isChannel(chat.id)
        }

        override fun getTotalStringRes (): Int {
          return R.string.xChannels
        }

        override fun getEmptyStringRes (): Int {
          return R.string.NoChannels
        }

        override fun getMessagesStringRes (isArchive: Boolean): Int {
          return if (isArchive) R.string.MessagesArchiveChannels else R.string.MessagesChannels
        }

        override fun canFilterMessages (): Boolean {
          return true
        }
      }
    }

    @JvmStatic
    fun privateFilter (tdlib: Tdlib): ChatFilter {
      return object : ChatFilter {
        override fun accept (chat: TdApi.Chat): Boolean {
          return tdlib.isUserChat(chat) && !tdlib.isBotChat(chat)
        }

        override fun getEmptyStringRes (): Int {
          return R.string.NoPrivateChats
        }

        override fun getMessagesStringRes (isArchive: Boolean): Int {
          return if (isArchive) R.string.MessagesArchivePrivate else R.string.MessagesPrivate
        }

        override fun canFilterMessages (): Boolean {
          return true
        }
      }
    }

    @JvmStatic
    fun botsFilter (tdlib: Tdlib): ChatFilter {
      return object : ChatFilter {
        override fun accept (chat: TdApi.Chat): Boolean {
          return tdlib.isBotChat(chat)
        }

        override fun getTotalStringRes (): Int {
          return R.string.xBots
        }

        override fun getEmptyStringRes (): Int {
          return R.string.NoBotsChats
        }

        override fun getMessagesStringRes (isArchive: Boolean): Int {
          return if (isArchive) R.string.MessagesArchiveBots else R.string.MessagesBots
        }

        override fun canFilterMessages (): Boolean {
          return true
        }
      }
    }

    @JvmStatic
    fun unreadFilter (tdlib: Tdlib): ChatFilter {
      return object : ChatFilter {
        override fun accept (chat: TdApi.Chat): Boolean {
          return chat.unreadCount > 0 || chat.isMarkedAsUnread
        }

        override fun getEmptyStringRes (): Int {
          return R.string.NoUnreadChats
        }
      }
    }
  }
}
