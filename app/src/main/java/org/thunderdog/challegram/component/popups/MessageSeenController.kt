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
import org.thunderdog.challegram.component.user.UserView
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.data.TGMessage
import org.thunderdog.challegram.data.TGUser
import org.thunderdog.challegram.navigation.BackHeaderButton
import org.thunderdog.challegram.support.ViewSupport
import org.thunderdog.challegram.telegram.TdlibUi
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.ui.ListItem
import org.thunderdog.challegram.ui.SettingHolder
import org.thunderdog.challegram.ui.SettingsAdapter
import org.thunderdog.challegram.widget.ListInfoView
import java.util.ArrayList
import kotlin.math.min

class MessageSeenController (context: MediaLayout, private val msg: TGMessage, private val viewers: TdApi.MessageViewers) :
  MediaBottomBaseController<Void>(context, getViewString(msg, viewers.viewers.size).toString()), View.OnClickListener {
  private lateinit var adapter: SettingsAdapter
  private var allowExpand = false

  override fun getBackButton (): Int {
    return BackHeaderButton.TYPE_CLOSE
  }

  override fun performOnBackPressed (fromTop: Boolean, commit: Boolean): Boolean {
    if (!mediaLayout.isHidden()) {
      if (commit) {
        mediaLayout.hide(false)
      }
      return true
    }
    return super.performOnBackPressed(fromTop, commit)
  }

  override fun onCreateView (context: Context): View? {
    buildContentView(false)
    layoutManager = LinearLayoutManager(context(), RecyclerView.VERTICAL, false)

    adapter = object : SettingsAdapter(this) {
      override fun setValuedSetting (item: ListItem, view: SettingView, isUpdate: Boolean) {
        if (item.id == R.id.btn_openLink) {
          view.setIconColorId(ColorId.textNeutral)
        } else {
          view.setIconColorId(ColorId.icon)
        }
      }

      override fun setInfo (item: ListItem, position: Int, infoView: ListInfoView) {
        infoView.showInfo(getViewString(msg, viewers.viewers.size))
      }

      override fun setUser (item: ListItem, position: Int, userView: UserView, isUpdate: Boolean) {
        val tdlib = tdlib!!
        val user = TGUser(tdlib, tdlib.chatUser(item.longId)!!)
        user.setActionDateStatus(item.intValue, msg.message)
        userView.setUser(user)
      }
    }

    ViewSupport.setThemedBackground(recyclerView, ColorId.background)

    val items = ArrayList<ListItem>()

    for (viewer in viewers.viewers) {
      items.add(ListItem(ListItem.TYPE_USER, R.id.user).setLongId(viewer.userId).setIntValue(viewer.viewDate))
    }
    items.add(ListItem(ListItem.TYPE_SHADOW_BOTTOM))
    items.add(ListItem(ListItem.TYPE_DESCRIPTION, R.id.description, 0, R.string.MessageSeenPrivacy))
    items.add(ListItem(ListItem.TYPE_LIST_INFO_VIEW))

    adapter.setItems(items.toTypedArray(), false)
    initMetrics()

    allowExpand = initialContentHeight == super.initialContentHeight
    if (allowExpand) {
      adapter.removeItem(adapter.itemCount - 1)
    }

    setAdapter(adapter)

    return contentView
  }

  override val initialContentHeight: Int
    get() {
      if (viewers != null) {
        var computedHeight = SettingHolder.measureHeightForType(ListItem.TYPE_USER) * viewers.viewers.size
        for (i in viewers.viewers.size until adapter.itemCount) {
          val item = adapter.items[i]
          computedHeight += if (item.viewType == ListItem.TYPE_DESCRIPTION) Screen.dp(24f) else SettingHolder.measureHeightForType(item.viewType)
        }
        return min(super.initialContentHeight, computedHeight)
      }
      return super.initialContentHeight
    }

  override fun canExpandHeight (): Boolean {
    return allowExpand
  }

  override fun createCustomBottomBar (): ViewGroup {
    return FrameLayout(context)
  }

  override fun getId (): Int {
    return R.id.controller_messageSeen
  }

  override fun onClick (v: View) {
    if (v.id == R.id.user) {
      mediaLayout.hide(false)
      tdlib!!.ui().openPrivateProfile(this, (v.tag as ListItem).longId, TdlibUi.UrlOpenParameters().tooltip(context().tooltipManager().builder(v)))
    }
  }

  companion object {
    @JvmStatic
    fun getViewString (msg: TGMessage, count: Int): CharSequence {
      return when (msg.message.content.constructor) {
        TdApi.MessageVoiceNote.CONSTRUCTOR -> Lang.pluralBold(R.string.MessageSeenXListened, count.toLong())
        TdApi.MessageVideoNote.CONSTRUCTOR -> Lang.pluralBold(R.string.MessageSeenXPlayed, count.toLong())
        else -> Lang.pluralBold(R.string.xViews, count.toLong())
      }
    }

    @JvmStatic
    fun getNobodyString (msg: TGMessage): String {
      return when (msg.message.content.constructor) {
        TdApi.MessageVoiceNote.CONSTRUCTOR -> Lang.getString(R.string.MessageSeenNobodyListened)
        TdApi.MessageVideoNote.CONSTRUCTOR -> Lang.getString(R.string.MessageSeenNobodyPlayed)
        else -> Lang.getString(R.string.MessageSeenNobody)
      }
    }
  }
}
