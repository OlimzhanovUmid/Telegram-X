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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.drinkless.tdlib.TdApi
import org.thunderdog.challegram.R
import org.thunderdog.challegram.component.attach.MediaBottomBaseController
import org.thunderdog.challegram.component.attach.MediaLayout
import org.thunderdog.challegram.component.base.SettingView
import org.thunderdog.challegram.component.chat.DetachedChatHeaderView
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.data.TD
import org.thunderdog.challegram.data.TGFoundChat
import org.thunderdog.challegram.navigation.BackHeaderButton
import org.thunderdog.challegram.navigation.HeaderView
import org.thunderdog.challegram.support.ViewSupport
import org.thunderdog.challegram.telegram.TdlibUi
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.ui.ListItem
import org.thunderdog.challegram.ui.SettingsAdapter
import org.thunderdog.challegram.util.text.TextEntity
import org.thunderdog.challegram.widget.CustomTextView
import org.thunderdog.challegram.widget.VerticalChatView
import java.util.ArrayList
import me.vkryl.android.widget.FrameLayoutFix
import tgx.td.findEntities

class JoinDialogController (context: MediaLayout, private val inviteLinkInfo: TdApi.ChatInviteLinkInfo, private val onJoinClicked: Runnable) : MediaBottomBaseController<Void>(context, ""), View.OnClickListener {
  private val DESCRIPTION_PADDING = Screen.dp(16f)

  private var adapter: SettingsAdapter? = null
  private var adapterMembers: SettingsAdapter? = null

  private var measuredRecyclerHeight = 0

  override fun getBackButton (): Int {
    return BackHeaderButton.TYPE_CLOSE
  }

  override fun performOnBackPressed (fromTop: Boolean, commit: Boolean): Boolean {
    if (commit) {
      mediaLayout.hide(false)
    }
    return true
  }

  override fun onCreateView (context: Context): View? {
    buildContentView(false)
    layoutManager = LinearLayoutManager(context(), RecyclerView.VERTICAL, false)

    val adapterMembers = object : SettingsAdapter(this) {
      override fun setChatData (item: ListItem, chatView: VerticalChatView) {
        chatView.setChat(item.getData() as TGFoundChat)
        chatView.clearPreviewChat()
      }
    }
    this.adapterMembers = adapterMembers

    val adapter = object : SettingsAdapter(this) {
      override fun setValuedSetting (item: ListItem, view: SettingView, isUpdate: Boolean) {
        if (item.getId() == R.id.btn_join) {
          view.setIconColorId(ColorId.textNeutral)
        } else {
          view.setIconColorId(ColorId.icon)
        }
      }

      override fun setText (item: ListItem, view: CustomTextView, isUpdate: Boolean) {
        if (item.getId() == R.id.description) {
          view.setPadding(DESCRIPTION_PADDING, DESCRIPTION_PADDING, DESCRIPTION_PADDING, DESCRIPTION_PADDING / 2)
          view.setText(item.getString(), TextEntity.valueOf(tdlib, item.getString().toString(), item.getString().toString().findEntities(), TdlibUi.UrlOpenParameters().tooltip(context().tooltipManager().builder(view))), false)
          view.setTextSize(15f)
        } else {
          view.setPadding(0, 0, 0, 0)
          super.setText(item, view, isUpdate)
        }
      }

      override fun setChatHeader (item: ListItem, position: Int, headerView: DetachedChatHeaderView) {
        headerView.bindWith(tdlib!!, inviteLinkInfo)
      }

      override fun setRecyclerViewData (item: ListItem, recyclerView: RecyclerView, isInitialization: Boolean) {
        recyclerView.setAdapter(adapterMembers)
      }
    }
    this.adapter = adapter

    ViewSupport.setThemedBackground(recyclerView, ColorId.filling)

    val isChannel = TD.isChannel(inviteLinkInfo.type)

    val items = ArrayList<ListItem>()

    items.add(ListItem(ListItem.TYPE_CHAT_HEADER_LARGE))

    if (inviteLinkInfo.description != null && inviteLinkInfo.description.length > 0) {
      items.add(ListItem(ListItem.TYPE_TEXT_VIEW, R.id.description, 0, inviteLinkInfo.description, false))
    }

    if (inviteLinkInfo.memberUserIds != null && inviteLinkInfo.memberUserIds.size > 0) {
      items.add(ListItem(ListItem.TYPE_RECYCLER_HORIZONTAL))

      val itemsMembers = ArrayList<ListItem>()
      for (i in inviteLinkInfo.memberUserIds.indices) {
        val user = tdlib!!.cache().user(inviteLinkInfo.memberUserIds[i])
        if (user != null) {
          itemsMembers.add(ListItem(ListItem.TYPE_CHAT_VERTICAL, R.id.user).setLongId(inviteLinkInfo.memberUserIds[i]).setData(TGFoundChat(tdlib, user, null, false).setNoUnread()))
        }
      }
      adapterMembers.setItems(itemsMembers.toTypedArray(), false)
    }

    if (inviteLinkInfo.createsJoinRequest) {
      items.add(ListItem(ListItem.TYPE_INFO, R.id.message, 0, Lang.getString(if (isChannel) R.string.RequestToJoinChannelInfo else R.string.RequestToJoinGroupInfo), false))
    }

    items.add(ListItem(ListItem.TYPE_SETTING, R.id.btn_join, R.drawable.baseline_person_add_24, if (inviteLinkInfo.createsJoinRequest) Lang.getString(if (isChannel) R.string.RequestJoinChannelBtn else R.string.RequestJoinGroupBtn) else Lang.getString(if (isChannel) R.string.JoinChannel else R.string.JoinChat), false).setTextColorId(ColorId.textNeutral))
    items.add(ListItem(ListItem.TYPE_SETTING, R.id.btn_cancel, R.drawable.baseline_cancel_24, R.string.Cancel))

    val params = FrameLayoutFix.newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    params.topMargin = HeaderView.getSize(false)
    params.bottomMargin = HeaderView.getTopOffset()
    recyclerView!!.setLayoutParams(params)

    adapter.setItems(items.toTypedArray(), false)
    initMetrics()

    recyclerView!!.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
      override fun onLayoutChange (v: View, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int) {
        recyclerView!!.removeOnLayoutChangeListener(this)
        measuredRecyclerHeight = recyclerView!!.measuredHeight
        initMetrics()
      }
    })

    setAdapter(adapter)

    return contentView
  }

  override val initialContentHeight: Int
    get() {
      if (measuredRecyclerHeight != 0) {
        return measuredRecyclerHeight
      }
      return super.initialContentHeight
    }

  override fun ignoreStartHeightLimits (): Boolean {
    return true
  }

  override fun canExpandHeight (): Boolean {
    return false
  }

  override fun createCustomBottomBar (): ViewGroup {
    return FrameLayout(context)
  }

  override fun getId (): Int {
    return R.id.controller_joinDialog
  }

  override fun onClick (v: View) {
    if (v.id == R.id.btn_join) {
      mediaLayout.hide(false)
      onJoinClicked.run()
    } else if (v.id == R.id.btn_cancel) {
      mediaLayout.hide(false)
    } else if (v.id == R.id.user) {
      mediaLayout.hide(false)
      tdlib!!.ui().openPrivateProfile(this, (v.tag as ListItem).getLongId(), TdlibUi.UrlOpenParameters().tooltip(context().tooltipManager().builder(v)))
    }
  }
}
