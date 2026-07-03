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
 * File created on 19/10/2016
 */
package org.thunderdog.challegram.component.attach

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import org.thunderdog.challegram.R
import org.thunderdog.challegram.component.user.SimpleUsersAdapter
import org.thunderdog.challegram.data.TD
import org.thunderdog.challegram.data.TGUser

class MediaBottomInlineBotsController (context: MediaLayout) : MediaBottomBaseController<Void>(context, R.string.InlineBot), Client.ResultHandler, SimpleUsersAdapter.Callback {
  override fun getId (): Int {
    return R.id.controller_media_inlineBots
  }

  override fun onCreateView (context: Context): View? {
    buildContentView(true)
    layoutManager = LinearLayoutManager(context(), RecyclerView.VERTICAL, false)
    adapter = SimpleUsersAdapter(this, this, SimpleUsersAdapter.OPTION_CLICKABLE, this)
    setAdapter(adapter)
    tdlib!!.client().send(TdApi.GetTopChats(TdApi.TopChatCategoryInlineBots(), 50), this)
    return contentView
  }

  private lateinit var adapter: SimpleUsersAdapter

  private fun displayBots (users: List<TGUser>) {
    if (users.isEmpty()) {
      showError(R.string.NothingFound, 0, null, true)
    } else {
      hideProgress {
        adapter.setUsers(users)
        expandStartHeight(adapter)
      }
    }
  }

  override fun onUserPicked (user: TGUser?) {
    mediaLayout.chooseInlineBot(user)
  }

  override fun onUserSelected (selectedCount: Int, user: TGUser?, isSelected: Boolean) { }

  private val defaultBots = ArrayList<TGUser>(DEFAULT_BOTS.size)

  private fun loadNextBot () {
    if (defaultBots.size > DEFAULT_BOTS.size) {
      return
    }
    if (defaultBots.size == DEFAULT_BOTS.size) {
      runOnUiThread { displayBots(defaultBots) }
    } else {
      tdlib!!.client().send(TdApi.SearchPublicChat(DEFAULT_BOTS[defaultBots.size]), this)
    }
  }

  private fun addDefaultBot (user: TdApi.User?) {
    defaultBots.add(TGUser.createWithUsername(tdlib!!, user))
    loadNextBot()
  }

  override fun onResult (`object`: TdApi.Object) {
    when (`object`.getConstructor()) {
      TdApi.Error.CONSTRUCTOR -> {
        dispatchError(TD.toErrorString(`object`), null, null, true)
      }
      TdApi.Chat.CONSTRUCTOR -> {
        addDefaultBot(tdlib!!.chatUser(`object` as TdApi.Chat))
      }
      TdApi.Chats.CONSTRUCTOR -> {
        val chatIds = (`object` as TdApi.Chats).chatIds
        if (chatIds.isEmpty()) {
          loadNextBot()
          return
        }

        val users = ArrayList<TGUser>(chatIds.size)
        val chats = tdlib!!.chats(chatIds)
        for (chat in chats) {
          val user = chat?.let { tdlib!!.chatUser(it) }
          if (user != null) {
            users.add(TGUser.createWithUsername(tdlib!!, user))
          }
        }

        if (users.isEmpty()) {
          tdlib!!.client().send(TdApi.GetRecentInlineBots(), this)
        } else {
          runOnUiThread { displayBots(users) }
        }
      }
      TdApi.Users.CONSTRUCTOR -> {
        val userIds = (`object` as TdApi.Users).userIds
        if (userIds.isEmpty()) {
          loadNextBot()
          return
        }
        val users = tdlib!!.cache().users(userIds)
        val parsedUsers = ArrayList<TGUser>(userIds.size)
        for (user in users) {
          parsedUsers.add(TGUser.createWithUsername(tdlib!!, user))
        }
        runOnUiThread { displayBots(parsedUsers) }
      }
    }
  }

  companion object {
    private val DEFAULT_BOTS = arrayOf("bing", "wiki", "gif", "nephobot", "vid")
  }
}
