package org.thunderdog.challegram.telegram

import org.drinkless.tdlib.TdApi

interface MessageThreadListener {
  fun onMessageThreadLastMessageChanged (chatId: Long, topicId: TdApi.MessageTopic?, lastMessageId: Long) { }
  fun onMessageThreadReplyCountChanged (chatId: Long, topicId: TdApi.MessageTopic?, replyCount: Int) { }
  fun onMessageThreadReadInbox (chatId: Long, topicId: TdApi.MessageTopic?, lastReadInboxMessageId: Long, remainingUnreadCount: Int) { }
  fun onMessageThreadReadOutbox (chatId: Long, topicId: TdApi.MessageTopic?, lastReadOutboxMessageId: Long) { }
  fun onMessageThreadDeleted (chatId: Long, topicId: TdApi.MessageTopic?) { }
}
