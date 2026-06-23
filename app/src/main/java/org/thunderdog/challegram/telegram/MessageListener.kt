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
import org.drinkless.tdlib.TdApi.MessageContent
import org.drinkless.tdlib.TdApi.MessageInteractionInfo
import org.drinkless.tdlib.TdApi.ReplyMarkup
import org.drinkless.tdlib.TdApi.Sticker
import org.drinkless.tdlib.TdApi.UnreadReaction

interface MessageListener {
    fun onNewMessage(message: TdApi.Message?) {}
    fun onMessageSendAcknowledged(chatId: Long, messageId: Long) {}
    fun onMessageSendSucceeded(message: TdApi.Message?, oldMessageId: Long) {}
    fun onMessageSendFailed(message: TdApi.Message?, oldMessageId: Long, error: TdApi.Error?) {}
    fun onMessageContentChanged(chatId: Long, messageId: Long, newContent: MessageContent?) {}
    fun onMessageEdited(chatId: Long, messageId: Long, editDate: Int, replyMarkup: ReplyMarkup?) {}
    fun onMessagePinned(chatId: Long, messageId: Long, isPinned: Boolean) {}
    fun onMessageOpened(chatId: Long, messageId: Long) {}
    fun onAnimatedEmojiMessageClicked(chatId: Long, messageId: Long, sticker: Sticker?) {}
    fun onMessageMentionRead(chatId: Long, messageId: Long) {}
    fun onMessageInteractionInfoChanged(chatId: Long, messageId: Long, interactionInfo: MessageInteractionInfo?) {}
    fun onMessageUnreadPollVotesChanged(chatId: Long, messageId: Long, hasUnreadPollVote: Boolean, unreadPollVoteCount: Int) {}
    fun onMessageUnreadReactionsChanged(chatId: Long, messageId: Long, unreadReactions: Array<UnreadReaction?>?, unreadReactionCount: Int) {}
    fun onMessagesDeleted(chatId: Long, messageIds: LongArray?) {}
    fun onMessageLiveLocationViewed(chatId: Long, messageId: Long) {}
}
