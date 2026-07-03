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
import androidx.recyclerview.widget.RecyclerView
import org.drinkless.tdlib.TdApi
import org.thunderdog.challegram.R
import org.thunderdog.challegram.component.popups.MessageSeenController
import org.thunderdog.challegram.component.user.UserView
import org.thunderdog.challegram.data.TGMessage
import org.thunderdog.challegram.data.TGUser
import org.thunderdog.challegram.support.ViewSupport
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.telegram.TdlibUi
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.v.CustomRecyclerView
import org.thunderdog.challegram.widget.ListInfoView
import org.thunderdog.challegram.widget.PopupLayout
import java.util.ArrayList

class MessageOptionsSeenController (context: Context, tdlib: Tdlib, private val popupLayout: PopupLayout, msg: TGMessage) :
  BottomSheetViewController.BottomSheetBaseRecyclerViewController<Void>(context, tdlib), View.OnClickListener {
  private lateinit var adapter: SettingsAdapter
  private var message: TGMessage = msg
  private var viewers: TdApi.MessageViewers? = null

  override fun getId (): Int {
    return R.id.controller_messageOptionsSeen
  }

  override fun supportsBottomInset (): Boolean {
    return true
  }

  override fun onCreateView (context: Context, recyclerView: CustomRecyclerView) {
    adapter = object : SettingsAdapter(this) {
      override fun setUser (item: ListItem, position: Int, userView: UserView, isUpdate: Boolean) {
        val tdlib = tdlib!!
        val user = TGUser(tdlib, tdlib.chatUser(item.longId)!!)
        user.setActionDateStatus(item.intValue, message.message)
        userView.setUser(user)
      }

      override fun setInfo (item: ListItem, position: Int, infoView: ListInfoView) {
        val viewers = viewers
        if (viewers != null) {
          infoView.showInfo(MessageSeenController.getViewString(message, viewers.viewers.size))
        }
      }
    }
    recyclerView.adapter = adapter
    ViewSupport.setThemedBackground(recyclerView, ColorId.background)
    addThemeInvalidateListener(recyclerView)
    tdlib!!.client().send(TdApi.GetMessageViewers(message.chatId, message.id)) { obj ->
      if (obj.constructor != TdApi.MessageViewers.CONSTRUCTOR) return@send
      runOnUiThreadOptional(Runnable {
        setUsers(message, obj as TdApi.MessageViewers)
      })
    }
  }

  override fun needsTempUpdates (): Boolean {
    return true
  }

  fun setUsers (msg: TGMessage, viewers: TdApi.MessageViewers) {
    this.message = msg
    this.viewers = viewers

    var first = true
    val items = ArrayList<ListItem>()
    for (viewer in viewers.viewers) {
      if (first) {
        first = false
      } else {
        items.add(ListItem(ListItem.TYPE_SEPARATOR))
      }
      items.add(ListItem(ListItem.TYPE_USER_SMALL, R.id.user).setLongId(viewer.userId).setIntValue(viewer.viewDate))
    }
    items.add(ListItem(ListItem.TYPE_SHADOW_BOTTOM))
    //items.add(new ListItem(ListItem.TYPE_DESCRIPTION, R.id.description, 0, R.string.MessageSeenPrivacy));
    items.add(ListItem(ListItem.TYPE_LIST_INFO_VIEW))
    adapter.setItems(items, false)
  }

  override fun onClick (v: View) {
    if (v.id == R.id.user) {
      popupLayout.hideWindow(true)
      tdlib!!.ui().openPrivateProfile(this, (v.tag as ListItem).longId, TdlibUi.UrlOpenParameters().tooltip(context().tooltipManager().builder(v)))
    }
  }

  override fun getItemsHeight (recyclerView: RecyclerView?): Int {
    if (adapter.items.size == 0) {
      return 0
    }
    return adapter.measureHeight(-1)
  }
}
