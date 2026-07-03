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
package org.thunderdog.challegram.component.popups

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import kotlin.math.min
import me.vkryl.android.widget.FrameLayoutFix
import org.drinkless.tdlib.TdApi
import org.thunderdog.challegram.R
import org.thunderdog.challegram.component.attach.MediaBottomBaseController
import org.thunderdog.challegram.component.attach.MediaLayout
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.navigation.BackHeaderButton
import org.thunderdog.challegram.navigation.HeaderView
import org.thunderdog.challegram.navigation.Menu
import org.thunderdog.challegram.support.ViewSupport
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.tool.Strings

class JoinRequestsController (context: MediaLayout, chatId: Long, private val requestsInfo: TdApi.ChatJoinRequestsInfo) :
  MediaBottomBaseController<Void>(context, Lang.plural(R.string.xJoinRequests, requestsInfo.totalCount.toLong())), View.OnClickListener, Menu {
  private var allowExpand = false
  private val component = JoinRequestsComponent(this, chatId, null)
  private var reqCount = requestsInfo.totalCount

  override fun onClick (v: View) {
    component.onClick(v)
  }

  override fun onCreateView (context: Context): View? {
    buildContentView(false)

    component.onCreateView(context, recyclerView)
    ViewSupport.setThemedBackground(recyclerView, ColorId.background)

    initMetrics()
    allowExpand = initialContentHeight == super.initialContentHeight

    if (!allowExpand) {
      val params = recyclerView!!.getLayoutParams() as FrameLayout.LayoutParams
      params.height = initialContentHeight
      recyclerView!!.setLayoutParams(params)
    }

    return contentView
  }

  fun close () {
    mediaLayout.hide(false)
  }

  fun onRequestDecided () {
    reqCount--

    if (!mediaLayout.getHeaderView().inSearchMode()) {
      setName(Lang.plural(R.string.xJoinRequests, reqCount.toLong()))
    }

    if (reqCount == 0) {
      close()
    }
  }

  override val initialContentHeight: Int
    get() {
      if (requestsInfo.totalCount > 0) {
        return min(super.initialContentHeight, component.getHeight(requestsInfo.totalCount))
      }
      return super.initialContentHeight
    }

  override fun canExpandHeight (): Boolean {
    return allowExpand
  }

  override fun getId (): Int {
    return R.id.controller_chatJoinRequests
  }

  override fun createCustomBottomBar (): ViewGroup? {
    return FrameLayout(context)
  }

  override fun getBackButton (): Int {
    return BackHeaderButton.TYPE_CLOSE
  }

  override fun performOnBackPressed (fromTop: Boolean, commit: Boolean): Boolean {
    if (mediaLayout.getHeaderView().inSearchMode()) {
      if (commit) {
        mediaLayout.getHeaderView().closeSearchMode(true, null)
        headerView = mediaLayout.getHeaderView()
      }
      return true
    }

    if (!mediaLayout.isHidden()) {
      if (commit) {
        close()
      }
      return true
    }

    return super.performOnBackPressed(fromTop, commit)
  }

  override fun destroy () {
    super.destroy()
    component.destroy()
  }

  // Search

  override fun getMenuId (): Int {
    return R.id.menu_search
  }

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
      mediaLayout.getHeaderView().openSearchMode()
      headerView = mediaLayout.getHeaderView()
    } else if (id == R.id.menu_btn_clear) {
      clearSearchInput()
    }
  }

  override fun onLeaveSearchMode () {
    component.search(null)
  }

  override fun onAfterLeaveSearchMode () {
    runOnUiThread(Runnable { setName(Lang.plural(R.string.xJoinRequests, reqCount.toLong())) }, 100)
  }

  override fun onSearchInputChanged (query: String) {
    component.search(Strings.clean(query.trim()))
  }
}
