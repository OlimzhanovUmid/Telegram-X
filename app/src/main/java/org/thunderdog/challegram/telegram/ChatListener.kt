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

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import org.drinkless.tdlib.TdApi

interface ChatListener : ForumTopicInfoListener {
  fun onChatTopMessageChanged (chatId: Long, @Nullable topMessage: TdApi.Message?) { }
  fun onChatPositionChanged (chatId: Long, position: TdApi.ChatPosition, orderChanged: Boolean, sourceChanged: Boolean, pinStateChanged: Boolean) { }
  fun onChatAddedToList (chatId: Long, chatList: TdApi.ChatList) { }
  fun onChatRemovedFromList (chatId: Long, chatList: TdApi.ChatList) { }
  fun onChatPermissionsChanged (chatId: Long, permissions: TdApi.ChatPermissions) { }
  fun onChatTitleChanged (chatId: Long, title: String) { }
  fun onChatThemeChanged (chatId: Long, @Nullable theme: TdApi.ChatTheme?) { }

  fun onChatBackgroundChanged (chatId: Long, @Nullable background: TdApi.ChatBackground?) { }
  fun onChatAccentColorsChanged (
    chatId: Long,
    accentColorId: Int, backgroundCustomEmojiId: Long,
    profileAccentColorId: Int, profileBackgroundCustomEmojiId: Long
  ) { }
  fun onChatEmojiStatusChanged (chatId: Long, @Nullable emojiStatus: TdApi.EmojiStatus?) { }

  fun onChatBackgroundCustomEmojiChanged (chatId: Long, customEmojiId: Long) { }
  fun onChatActionBarChanged (chatId: Long, @Nullable actionBar: TdApi.ChatActionBar?) { }
  fun onChatBusinessBotManageBarChanged (chatId: Long, @Nullable botManageBar: TdApi.BusinessBotManageBar?) { }
  fun onChatPhotoChanged (chatId: Long, @Nullable photo: TdApi.ChatPhotoInfo?) { }
  fun onChatReadInbox (chatId: Long, lastReadInboxMessageId: Long, unreadCount: Int, availabilityChanged: Boolean) { }
  fun onChatHasScheduledMessagesChanged (chatId: Long, hasScheduledMessages: Boolean) { }
  fun onChatHasProtectedContentChanged (chatId: Long, hasProtectedContent: Boolean) { }
  fun onChatReadOutbox (chatId: Long, lastReadOutboxMessageId: Long) { }
  fun onChatMarkedAsUnread (chatId: Long, isMarkedAsUnread: Boolean) { }
  fun onChatIsTranslatableChanged (chatId: Long, isTranslatable: Boolean) { }
  fun onChatBlockListChanged (chatId: Long, @Nullable blockList: TdApi.BlockList?) { }
  fun onChatOnlineMemberCountChanged (chatId: Long, onlineMemberCount: Int) { }
  fun onChatMessageTtlSettingChanged (chatId: Long, messageAutoDeleteTime: Int) { }
  fun onChatActiveStoriesChanged (@NonNull activeStories: TdApi.ChatActiveStories) { }
  fun onChatVideoChatChanged (chatId: Long, videoChat: TdApi.VideoChat) { }
  fun onChatViewAsTopics (chatId: Long, viewAsTopics: Boolean) { }
  fun onChatPendingJoinRequestsChanged (chatId: Long, @Nullable pendingJoinRequests: TdApi.ChatJoinRequestsInfo?) { }
  fun onChatReplyMarkupChanged (chatId: Long, @Nullable replyMarkupMessage: TdApi.Message?) { }
  fun onChatDraftMessageChanged (chatId: Long, @Nullable draftMessage: TdApi.DraftMessage?) { }
  fun onChatUnreadMentionCount (chatId: Long, unreadMentionCount: Int, availabilityChanged: Boolean) { }
  fun onChatUnreadPollVoteCount (chatId: Long, unreadMentionCount: Int, availabilityChanged: Boolean) { }
  fun onChatUnreadReactionCount (chatId: Long, unreadReactionCount: Int, availabilityChanged: Boolean) { }
  fun onChatDefaultDisableNotifications (chatId: Long, defaultDisableNotifications: Boolean) { }
  fun onChatDefaultMessageSenderIdChanged (chatId: Long, @Nullable senderId: TdApi.MessageSender?) { }
  fun onChatClientDataChanged (chatId: Long, @Nullable clientData: String?) { }
  fun onChatAvailableReactionsUpdated (chatId: Long, availableReactions: TdApi.ChatAvailableReactions) { }
}
