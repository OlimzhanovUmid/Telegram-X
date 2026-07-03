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
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import me.vkryl.core.collection.LongSet
import me.vkryl.core.lambda.RunnableInt

abstract class UserListManager (
  tdlib: Tdlib,
  initialLoadCount: Int,
  loadCount: Int,
  listener: ChangeListener?
) : ListManager<Long>(tdlib, initialLoadCount, loadCount, false, listener), TdlibCache.UserDataChangeListener, TdlibCache.UserStatusChangeListener {
  interface ChangeListener : ListManager.ListChangeListener<Long>

  private val userIdsCheck = LongSet()

  abstract override fun nextLoadFunction (reverse: Boolean, itemCount: Int, loadCount: Int): TdApi.Function<TdApi.Users>

  override fun subscribeToUpdates () {
    tdlib.cache().addGlobalUsersListener(this)
  }

  override fun unsubscribeFromUpdates () {
    tdlib.cache().removeGlobalUsersListener(this)
  }

  override fun processResponse (response: TdApi.Object, retryHandler: Client.ResultHandler, retryLoadCount: Int, reverse: Boolean): Response<Long> {
    val users = response as TdApi.Users
    val rawUserIds = users.userIds
    val userIds = ArrayList<Long>(rawUserIds.size)
    for (userId in rawUserIds) {
      if (userIdsCheck.add(userId)) {
        userIds.add(userId)
      }
    }
    return Response(userIds, users.totalCount)
  }

  // Updates

  private fun runWithUser (userId: Long, act: RunnableInt) {
    if (userIdsCheck.has(userId)) {
      runOnUiThreadIfReady {
        val index = indexOfItem(userId)
        if (index != -1) {
          // This check is needed, because
          // userIdsCheck is modified on TDLib thread,
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
  )
  annotation class UpdateReason {
    companion object {
      const val USER_CHANGED = 1
      const val USER_FULL_CHANGED = 2
      const val USER_STATUS_CHANGED = 3
    }
  }

  private fun notifyUserChanged (userId: Long, @UpdateReason reason: Int) {
    runWithUser(userId) { index ->
      notifyItemChanged(index, reason)
    }
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
}
