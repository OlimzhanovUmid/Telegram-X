package org.thunderdog.challegram.telegram

import org.drinkless.tdlib.TdApi

interface ForumTopicInfoListener {
  fun onForumTopicInfoChanged (info: TdApi.ForumTopicInfo) {}
  fun onForumTopicUpdated (chatId: Long, messageThreadId: Long, isPinned: Boolean, lastReadInboxMessageId: Long, lastReadOutboxMessageId: Long, notificationSettings: TdApi.ChatNotificationSettings) {}
}
