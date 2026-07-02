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
 * File created on 18/02/2018
 */
package org.thunderdog.challegram.telegram

import org.drinkless.tdlib.TdApi

interface GlobalMessageListener {
  fun onNewMessage (tdlib: Tdlib, message: TdApi.Message) { }
  fun onMessageContentChanged (tdlib: Tdlib, chatId: Long, messageId: Long, content: TdApi.MessageContent) { }

  fun onNewMessages (tdlib: Tdlib, messages: Array<TdApi.Message>) { }

  fun onMessageSendSucceeded (tdlib: Tdlib, message: TdApi.Message, oldMessageId: Long) { }

  fun onMessageSendFailed (tdlib: Tdlib, message: TdApi.Message, oldMessageId: Long, error: TdApi.Error) { }

  fun onMessagesDeleted (tdlib: Tdlib, chatId: Long, messageIds: LongArray) { }
}
