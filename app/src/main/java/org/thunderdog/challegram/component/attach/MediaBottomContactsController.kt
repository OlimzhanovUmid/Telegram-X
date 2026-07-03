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
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.vkryl.android.DECELERATE_INTERPOLATOR
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import org.drinkless.tdlib.TdApi.MessageSendOptions
import org.thunderdog.challegram.R
import org.thunderdog.challegram.component.user.SimpleUsersAdapter
import org.thunderdog.challegram.core.Background
import org.thunderdog.challegram.data.TD
import org.thunderdog.challegram.data.TGUser
import org.thunderdog.challegram.navigation.HeaderView
import org.thunderdog.challegram.navigation.Menu
import org.thunderdog.challegram.tool.Strings
import org.thunderdog.challegram.tool.UI
import org.thunderdog.challegram.ui.ContactsController
import java.util.ArrayList
import java.util.Locale

class MediaBottomContactsController (context: MediaLayout) : MediaBottomBaseController<Void>(context, R.string.AttachContact), Client.ResultHandler, SimpleUsersAdapter.Callback, Menu {
  override fun getId (): Int {
    return R.id.controller_media_contacts
  }

  override fun getMenuId (): Int {
    return R.id.menu_search
  }

  override val broadcastingAction: Int
    get() = TdApi.ChatActionChoosingContact.CONSTRUCTOR

  override fun getSearchMenuId (): Int {
    return R.id.menu_clear
  }

  override fun fillMenuItems (id: Int, header: HeaderView, menu: LinearLayout) {
    if (id == R.id.menu_search) {
      header.addSearchButton(menu, this)
    } else if (id == R.id.menu_clear) {
      header.addClearButton(menu, this)
    }
  }

  override fun onMenuItemPressed (id: Int, view: View?) {
    if (id == R.id.menu_btn_search) {
      if (users != null && !users!!.isEmpty()) {
        mediaLayout.getHeaderView().openSearchMode()
        headerView = mediaLayout.getHeaderView()
      }
    } else if (id == R.id.menu_btn_clear) {
      clearSearchInput()
    }
  }

  override fun onCreateView (context: Context): View? {
    buildContentView(true)
    layoutManager = LinearLayoutManager(context(), RecyclerView.VERTICAL, false)
    adapter = SimpleUsersAdapter(this, this, SimpleUsersAdapter.OPTION_CLICKABLE or SimpleUsersAdapter.OPTION_SELECTABLE, this)
    setAdapter(adapter)
    recyclerView!!.setItemAnimator(CustomItemAnimator(DECELERATE_INTERPOLATOR, HIDE_PROGRESS_DURATION))
    tdlib!!.searchContacts(null, ContactsController.DISPLAY_LIMIT, this)
    return contentView
  }

  private var adapter: SimpleUsersAdapter? = null
  private var users: ArrayList<TGUser>? = null

  protected fun displayContacts (users: ArrayList<TGUser>) {
    if (users.isEmpty()) {
      showError(if (this.users == null) R.string.NoContacts else R.string.NothingFound, 0, null, this.users == null)
      adapter!!.setUsers(null)
    } else if (this.users == null) {
      hideProgress()
      hideError()
      this.users = users
      adapter!!.setUsers(users)
      expandStartHeight(adapter!!)
    } else {
      adapter!!.setUsers(users)
      hideError()
    }
  }

  override fun onUserPicked (user: TGUser) {
    mediaLayout.sendContact(user)
  }

  override fun onUserSelected (selectedCount: Int, user: TGUser, isSelected: Boolean) {
    mediaLayout.setCounter(selectedCount)
  }

  override fun onMultiSendPress (view: View?, options: MessageSendOptions, disableMarkdown: Boolean) {
    mediaLayout.sendContacts(adapter!!.getSelectedUsers(), options)
  }

  override fun onCancelMultiSelection () {
    adapter!!.clearSelectedUsers(layoutManager as LinearLayoutManager?)
  }

  override fun onResult (`object`: TdApi.Object) {
    when (`object`.constructor) {
      TdApi.Error.CONSTRUCTOR -> {
        dispatchError(TD.toErrorString(`object`), null, null, true)
      }
      TdApi.Users.CONSTRUCTOR -> {
        val userIds = (`object` as TdApi.Users).userIds
        val contacts = tdlib!!.cache().users(userIds)
        val users = ArrayList<TGUser>(userIds.size)
        for (user in contacts) {
          if (TD.hasPhoneNumber(user)) {
            users.add(TGUser.createWithPhone(tdlib!!, user))
          }
        }
        runOnUiThread { displayContacts(users) }
      }
      else -> {
        dispatchError("Unknown constructor: " + `object`.constructor, null, null, true)
      }
    }
  }

  private var lastQuery = ""

  private fun searchUsers (q: String) {
    if (users == null) {
      return
    }
    recyclerView!!.setItemAnimator(null)
    if (lastQuery == q) {
      return
    }
    lastQuery = q
    if (q.isEmpty()) {
      displayContacts(users!!)
      return
    }
    Background.instance().post {
      val foundUsers = ArrayList<TGUser>()
      for (user in users!!) {
        val raw = user.getUser()
        if (raw == null) {
          continue
        }

        val firstName = Strings.clean(user.getFirstName().trim()).lowercase(Locale.getDefault())
        val lastName = Strings.clean(user.getLastName().trim()).lowercase(Locale.getDefault())
        val check = (firstName + " " + lastName).trim()

        if (!firstName.startsWith(q) && !lastName.startsWith(q) && !check.startsWith(q)) {
          continue
        }

        foundUsers.add(user)
      }
      UI.post {
        if (!isDestroyed() && lastQuery == q) {
          displayContacts(foundUsers)
        }
      }
    }
  }

  override fun onLeaveSearchMode () {
    searchUsers("")
  }

  override fun onSearchInputChanged (query: String) {
    searchUsers(Strings.clean(query.trim().lowercase(Locale.getDefault())))
  }
}
