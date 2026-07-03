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
 * File created on 24/09/2023
 */
package org.thunderdog.challegram.telegram

import androidx.annotation.IntDef
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import me.vkryl.core.collection.LongSet
import me.vkryl.core.lambda.RunnableInt
import tgx.td.fromSupergroupId
import tgx.td.fromUserId
import tgx.td.getSenderId

abstract class PollVoterListManager (tdlib: Tdlib, initialLoadCount: Int, loadCount: Int, listener: ChangeListener?) :
  ListManager<TdApi.PollVoter>(tdlib, initialLoadCount, loadCount, false, listener),
  TdlibCache.UserDataChangeListener, TdlibCache.UserStatusChangeListener, TdlibCache.SupergroupDataChangeListener, ChatListener {
  interface ChangeListener : ListManager.ListChangeListener<TdApi.PollVoter>

  private val voterIdsCheck = LongSet()

  abstract override fun nextLoadFunction (reverse: Boolean, itemCount: Int, loadCount: Int): TdApi.Function<TdApi.PollVoters>

  override fun subscribeToUpdates () {
    tdlib.cache().subscribeForGlobalUpdates(this)
    tdlib.listeners().subscribeForGlobalUpdates(this)
  }

  override fun unsubscribeFromUpdates () {
    tdlib.cache().unsubscribeFromGlobalUpdates(this)
    tdlib.listeners().unsubscribeFromGlobalUpdates(this)
  }

  override fun processResponse (response: TdApi.Object, retryHandler: Client.ResultHandler, retryLoadCount: Int, reverse: Boolean): Response<TdApi.PollVoter> {
    val voters = response as TdApi.PollVoters
    val sendersList = ArrayList<TdApi.PollVoter>(voters.voters.size)
    for (voter in voters.voters) {
      val senderId = voter.voterId.getSenderId()
      if (voterIdsCheck.add(senderId)) {
        sendersList.add(voter)
      }
    }
    return Response(sendersList, voters.totalCount)
  }

  // Updates

  private fun indexOfItem (senderId: Long): Int {
    var index = 0
    for (voter in items) {
      if (voter.voterId.getSenderId() == senderId) {
        return index
      }
      index++
    }
    return -1
  }

  private fun runWithChat (chatId: Long, act: RunnableInt) {
    if (voterIdsCheck.has(chatId)) {
      runOnUiThreadIfReady {
        val index = indexOfItem(chatId)
        if (index != -1) {
          // This check is needed, because
          // senderIdsCheck is modified on TDLib thread,
          // but items list is modified on main thread
          act.runWithInt(index)
        }
      }
    }
  }

  @Retention(AnnotationRetention.SOURCE)
  @IntDef(
    UpdateReason.USER_CHANGED,
    UpdateReason.USER_FULL_CHANGED,
    UpdateReason.USER_STATUS_CHANGED,
    UpdateReason.CHAT_TITLE_CHANGED,
    UpdateReason.CHAT_PHOTO_CHANGED,
    UpdateReason.SUPERGROUP_UPDATED,
    UpdateReason.SUPERGROUP_FULL_UPDATED,
  )
  annotation class UpdateReason {
    companion object {
      const val USER_CHANGED = 1
      const val USER_FULL_CHANGED = 2
      const val USER_STATUS_CHANGED = 3
      const val CHAT_TITLE_CHANGED = 4
      const val CHAT_PHOTO_CHANGED = 5
      const val SUPERGROUP_UPDATED = 6
      const val SUPERGROUP_FULL_UPDATED = 7
    }
  }

  private fun notifyChatChanged (chatId: Long, @UpdateReason reason: Int) {
    runWithChat(chatId) { index ->
      notifyItemChanged(index, reason)
    }
  }

  private fun notifyUserChanged (userId: Long, @UpdateReason reason: Int) {
    notifyChatChanged(fromUserId(userId), reason)
  }

  private fun notifySupergroupChanged (supergroupId: Long, @UpdateReason reason: Int) {
    notifyChatChanged(fromSupergroupId(supergroupId), reason)
  }

  override fun onUserUpdated (user: TdApi.User) {
    notifyUserChanged(user.id, UpdateReason.USER_CHANGED)
  }

  override fun onUserFullUpdated (userId: Long, userFull: TdApi.UserFullInfo) {
    notifyUserChanged(userId, UpdateReason.USER_FULL_CHANGED)
  }

  override fun onUserStatusChanged (userId: Long, status: TdApi.UserStatus, uiOnly: Boolean) {
    notifyUserChanged(userId, UpdateReason.USER_STATUS_CHANGED)
  }

  override fun onChatTitleChanged (chatId: Long, title: String) {
    notifyChatChanged(chatId, UpdateReason.CHAT_TITLE_CHANGED)
  }

  override fun onChatPhotoChanged (chatId: Long, photo: TdApi.ChatPhotoInfo?) {
    notifyChatChanged(chatId, UpdateReason.CHAT_PHOTO_CHANGED)
  }

  override fun onSupergroupUpdated (supergroup: TdApi.Supergroup) {
    notifySupergroupChanged(supergroup.id, UpdateReason.SUPERGROUP_UPDATED)
  }

  override fun onSupergroupFullUpdated (supergroupId: Long, newSupergroupFull: TdApi.SupergroupFullInfo) {
    notifySupergroupChanged(supergroupId, UpdateReason.SUPERGROUP_FULL_UPDATED)
  }
}
