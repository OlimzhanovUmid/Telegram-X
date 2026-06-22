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
 * File created on 31/03/2023
 */
package org.thunderdog.challegram.telegram

import org.drinkless.tdlib.TdApi

interface GroupCallListener {
  fun onGroupCallUpdated (groupCall: TdApi.GroupCall) {}
  fun onGroupCallParticipantUpdated (groupCallId: Int, participant: TdApi.GroupCallParticipant) {}
  fun onGroupCallParticipantsChanged (groupCallId: Int, participantUserIds: LongArray) {}
  fun onNewGroupCallMessage (groupCallId: Int, groupCallMessage: TdApi.GroupCallMessage) {}
  fun onNewGroupCallPaidReaction (groupCallId: Int, senderId: TdApi.MessageSender, starCount: Long) {}
  fun onGroupCallMessageSendFailed (groupCallId: Int, messageId: Int, error: TdApi.Error) {}
  fun onGroupCallMessagesDeleted (groupCallId: Int, messageIds: IntArray) {}
  fun onGroupCallVerificationStateChanged (groupCallId: Int, generation: Int, emojis: Array<String>) {}
}
