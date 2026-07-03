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
package org.thunderdog.challegram.ui

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import org.thunderdog.challegram.R
import org.thunderdog.challegram.component.popups.JoinRequestsComponent
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.navigation.HeaderView
import org.thunderdog.challegram.navigation.Menu
import org.thunderdog.challegram.navigation.ViewController
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.tool.Strings
import org.thunderdog.challegram.v.CustomRecyclerView

class ChatJoinRequestsController(context: Context, tdlib: Tdlib) :
  RecyclerViewController<ChatJoinRequestsController.Args>(context, tdlib),
  View.OnClickListener, Menu {

  private lateinit var component: JoinRequestsComponent

  override fun setArguments (args: Args) {
    super.setArguments(args)
    component = JoinRequestsComponent(this, args.chatId, args.inviteLink)
  }

  override fun onClick (v: View) {
    component.onClick(v)
  }

  override fun getId (): Int = R.id.controller_chatJoinRequests

  override fun getName (): CharSequence = Lang.getString(R.string.InviteLinkRequests)

  override fun onCreateView (context: Context, recyclerView: CustomRecyclerView) {
    component.onCreateView(context, recyclerView)
  }

  override fun destroy () {
    super.destroy()
    component.destroy()
  }

  override fun needAsynchronousAnimation (): Boolean {
    return component.needAsynchronousAnimation()
  }

  fun onRequestDecided () {
    val parent = getArgumentsStrict().parentController
    if (parent is ChatLinksController) {
      parent.onChatLinkPendingDecisionMade(getArgumentsStrict().inviteLink)
    }
  }

  class Args (val chatId: Long, val inviteLink: String?, val parentController: ViewController<*>)

  // Search

  override fun getMenuId (): Int = R.id.menu_search

  override fun getSearchMenuId (): Int = R.id.menu_clear

  override fun fillMenuItems (id: Int, header: HeaderView, menu: LinearLayout) {
    if (id == R.id.menu_search) {
      if (getArgumentsStrict().inviteLink.isNullOrEmpty()) {
        header.addSearchButton(menu, this)
      }
    } else if (id == R.id.menu_clear) {
      header.addClearButton(menu, this)
    }
  }

  override fun onMenuItemPressed (id: Int, view: View?) {
    if (id == R.id.menu_btn_search) {
      if (headerView != null) {
        headerView!!.openSearchMode()
      }
    } else if (id == R.id.menu_btn_clear) {
      clearSearchInput()
    }
  }

  override fun onLeaveSearchMode () {
    component.search(null)
  }

  override fun onSearchInputChanged (query: String) {
    super.onSearchInputChanged(query)
    component.search(Strings.clean(query.trim()))
  }

  override fun performOnBackPressed (fromTop: Boolean, commit: Boolean): Boolean {
    if (inSearchMode()) {
      if (commit) {
        closeSearchMode(null)
      }
      return true
    }
    return super.performOnBackPressed(fromTop, commit)
  }
}
