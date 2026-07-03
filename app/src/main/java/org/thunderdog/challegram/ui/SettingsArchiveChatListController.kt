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
 * File created on 29/09/2023
 */
package org.thunderdog.challegram.ui

import android.content.Context
import android.view.View
import org.drinkless.tdlib.TdApi
import org.thunderdog.challegram.R
import org.thunderdog.challegram.component.base.SettingView
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.data.TD
import org.thunderdog.challegram.telegram.NotificationSettingsListener
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.v.CustomRecyclerView
import tgx.td.CHAT_LIST_ARCHIVE
import java.util.ArrayList

class SettingsArchiveChatListController (context: Context, tdlib: Tdlib) : RecyclerViewController<Void>(context, tdlib), View.OnClickListener, NotificationSettingsListener {
  override fun getId (): Int {
    return R.id.controller_archiveSettings
  }

  override fun needAsynchronousAnimation (): Boolean {
    return true
  }

  override fun getName (): CharSequence {
    return Lang.getString(R.string.ArchiveSettings)
  }

  private var archiveChatListSettings: TdApi.ArchiveChatListSettings? = null
  private var error: TdApi.Error? = null

  private lateinit var adapter: SettingsAdapter

  override fun onCreateView (context: Context, recyclerView: CustomRecyclerView) {
    adapter = object : SettingsAdapter(this) {
      override fun setValuedSetting (item: ListItem, view: SettingView, isUpdate: Boolean) {
        val id = item.getId()
        if (id == R.id.btn_keepUnmutedChatsArchived) {
          view.getToggler().setRadioEnabled(archiveChatListSettings != null && archiveChatListSettings!!.keepUnmutedChatsArchived, isUpdate)
        } else if (id == R.id.btn_keepFolderChatsArchived) {
          view.getToggler().setRadioEnabled(archiveChatListSettings != null && archiveChatListSettings!!.keepChatsFromFoldersArchived, isUpdate)
        } else if (id == R.id.btn_archiveMuteNonContacts) {
          view.getToggler().setRadioEnabled(archiveChatListSettings != null && archiveChatListSettings!!.archiveAndMuteNewChatsFromUnknownUsers, isUpdate)
        } else if (id == R.id.btn_archiveAsFolder) {
          view.getToggler().setRadioEnabled(tdlib!!.settings().isChatListEnabled(CHAT_LIST_ARCHIVE), isUpdate)
        }
      }
    }
    tdlib!!.send(TdApi.GetArchiveChatListSettings()) { settings, error ->
      runOnUiThreadOptional(Runnable {
        this.archiveChatListSettings = settings
        this.error = error
        buildCells()
        executeScheduledAnimation()
      })
    }
    recyclerView.adapter = adapter
    tdlib!!.listeners().subscribeToSettingsUpdates(this)
  }

  private fun buildCells () {
    if (error != null) {
      adapter.setItems(arrayOf(
        ListItem(ListItem.TYPE_EMPTY, 0, 0, TD.toErrorString(error), false)
      ), false)
    } else {
      val items = ArrayList<ListItem>()

      items.add(ListItem(ListItem.TYPE_HEADER_PADDED, 0, 0, R.string.ArchiveSettingUnmutedChatsTitle))
      items.add(ListItem(ListItem.TYPE_SHADOW_TOP))
      items.add(ListItem(ListItem.TYPE_RADIO_SETTING, R.id.btn_keepUnmutedChatsArchived, 0, R.string.ArchiveSettingUnmutedChats))
      items.add(ListItem(ListItem.TYPE_SHADOW_BOTTOM))
      items.add(ListItem(ListItem.TYPE_DESCRIPTION, 0, 0, R.string.ArchiveSettingUnmutedChatsDesc))

      if (tdlib!!.hasFolders() || archiveChatListSettings!!.keepChatsFromFoldersArchived) {
        items.add(ListItem(ListItem.TYPE_HEADER, 0, 0, R.string.ArchiveSettingFolderChatsTitle))
        items.add(ListItem(ListItem.TYPE_SHADOW_TOP))
        items.add(ListItem(ListItem.TYPE_RADIO_SETTING, R.id.btn_keepFolderChatsArchived, 0, R.string.ArchiveSettingFolderChats))
        items.add(ListItem(ListItem.TYPE_SHADOW_BOTTOM))
        items.add(ListItem(ListItem.TYPE_DESCRIPTION, 0, 0, R.string.ArchiveSettingFolderChatsDesc))
      }

      if (tdlib!!.autoArchiveAvailable() || archiveChatListSettings!!.archiveAndMuteNewChatsFromUnknownUsers) {
        items.add(ListItem(ListItem.TYPE_HEADER, 0, 0, R.string.UnknownChats))
        items.add(ListItem(ListItem.TYPE_SHADOW_TOP))
        items.add(ListItem(ListItem.TYPE_RADIO_SETTING, R.id.btn_archiveMuteNonContacts, 0, R.string.ArchiveNonContacts))
        items.add(ListItem(ListItem.TYPE_SHADOW_BOTTOM))
        items.add(ListItem(ListItem.TYPE_DESCRIPTION, 0, 0, R.string.ArchiveNonContactsInfo))
      }

      if (tdlib!!.hasFolders()) {
        items.add(ListItem(ListItem.TYPE_HEADER, 0, 0, R.string.ArchiveAppearanceTitle))
        items.add(ListItem(ListItem.TYPE_SHADOW_TOP))
        items.add(ListItem(ListItem.TYPE_RADIO_SETTING, R.id.btn_archiveAsFolder, 0, R.string.ArchiveAppearanceToggle))
        items.add(ListItem(ListItem.TYPE_SHADOW_BOTTOM))
        items.add(ListItem(ListItem.TYPE_DESCRIPTION, 0, 0, Lang.getMarkdownString(this, R.string.ArchiveAppearanceHint)))
      }

      adapter.setItems(items, false)
    }
  }

  override fun onArchiveChatListSettingsChanged (settings: TdApi.ArchiveChatListSettings) {
    runOnUiThreadOptional(Runnable {
      if (archiveChatListSettings != null) {
        archiveChatListSettings = settings
        adapter.updateValuedSettingById(R.id.btn_keepFolderChatsArchived)
        adapter.updateValuedSettingById(R.id.btn_keepUnmutedChatsArchived)
        adapter.updateValuedSettingById(R.id.btn_archiveMuteNonContacts)
      }
    })
  }

  override fun onClick (v: View) {
    val id = v.id
    val settings = archiveChatListSettings ?: return
    if (id == R.id.btn_keepUnmutedChatsArchived ||
      id == R.id.btn_keepFolderChatsArchived ||
      id == R.id.btn_archiveMuteNonContacts) {
      val value = adapter.toggleView(v)
      if (id == R.id.btn_keepUnmutedChatsArchived) {
        settings.keepUnmutedChatsArchived = value
      } else if (id == R.id.btn_keepFolderChatsArchived) {
        settings.keepChatsFromFoldersArchived = value
      } else if (id == R.id.btn_archiveMuteNonContacts) {
        settings.archiveAndMuteNewChatsFromUnknownUsers = value
      }
      tdlib!!.send(TdApi.SetArchiveChatListSettings(settings)) { ok, _ ->
        if (ok != null) {
          tdlib!!.listeners().notifyArchiveChatListSettingsChanged(settings)
        }
      }
    } else if (id == R.id.btn_archiveAsFolder) {
      val value = adapter.toggleView(v)
      tdlib!!.settings().setChatListEnabled(CHAT_LIST_ARCHIVE, value)
    }
  }

  override fun destroy () {
    super.destroy()
    tdlib!!.listeners().unsubscribeFromSettingsUpdates(this)
  }
}
