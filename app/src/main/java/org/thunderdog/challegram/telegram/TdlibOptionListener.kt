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
 * File created on 04/07/2018
 */
package org.thunderdog.challegram.telegram

import org.drinkless.tdlib.TdApi

interface TdlibOptionListener {
  fun onTopChatsDisabled (areDisabled: Boolean) { }
  fun onSentScheduledMessageNotificationsDisabled (areDisabled: Boolean) { }
  fun onSuggestedLanguagePackChanged (suggestedLanguagePackId: String, suggestedLanguagePack: TdApi.LanguagePackInfo) { }
  fun onContactRegisteredNotificationsDisabled (areDisabled: Boolean) { }
  fun onSuggestedActionsChanged (addedActions: Array<TdApi.SuggestedAction>, removedActions: Array<TdApi.SuggestedAction>) { }
  fun onChatRevenueUpdated (chatId: Long, revenueAmount: TdApi.ChatRevenueAmount) { }
  fun onStarRevenueStatusUpdated (ownerId: TdApi.MessageSender, status: TdApi.StarRevenueStatus) { }
  fun onTonRevenueStatusUpdated (status: TdApi.TonRevenueStatus) { }
  fun onSpeedLimitNotification (isUpload: Boolean) { }
  fun onContactCloseBirthdayUsersChanged (birthdayUsers: Array<TdApi.CloseBirthdayUser>) { }
  fun onArchiveAndMuteChatsFromUnknownUsersEnabled (enabled: Boolean) { }
  fun onAccentColorsChanged (colors: Array<TdApi.AccentColor>, availableAccentColorIds: IntArray) { }
  fun onProfileAccentColorsChanged (listChanged: Boolean) { }
  fun onTrustedMiniAppBotsUpdated (botUserIds: LongArray) { }
  fun onGroupCallMessageLevelsUpdated (levels: Array<TdApi.GroupCallMessageLevel>) { }
}
