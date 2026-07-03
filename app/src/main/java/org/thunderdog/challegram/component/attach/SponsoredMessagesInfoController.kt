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
package org.thunderdog.challegram.component.attach

import android.content.Context
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.thunderdog.challegram.R
import org.thunderdog.challegram.component.base.SettingView
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.navigation.BackHeaderButton
import org.thunderdog.challegram.support.ViewSupport
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.tool.Intents
import org.thunderdog.challegram.ui.ListItem
import org.thunderdog.challegram.ui.SettingsAdapter

class SponsoredMessagesInfoController (context: MediaLayout, titleResource: Int) : MediaBottomBaseController<Void>(context, titleResource), View.OnClickListener {
  private var adapter: SettingsAdapter? = null

  override fun onCreateView (context: Context): View? {
    buildContentView(false)
    layoutManager = LinearLayoutManager(context(), RecyclerView.VERTICAL, false)

    val adapter = object : SettingsAdapter(this) {
      override fun setValuedSetting (item: ListItem, view: SettingView, isUpdate: Boolean) {
        if (item.getId() == R.id.btn_openLink) {
          view.setIconColorId(ColorId.textNeutral)
        } else {
          view.setIconColorId(ColorId.icon)
        }
      }
    }
    this.adapter = adapter
    setAdapter(adapter)

    ViewSupport.setThemedBackground(recyclerView, ColorId.background)

    adapter.setItems(arrayOf(
      ListItem(ListItem.TYPE_EMPTY_OFFSET_SMALL),
      ListItem(ListItem.TYPE_DESCRIPTION, 0, 0, R.string.SponsoredInfoText),
      ListItem(ListItem.TYPE_SHADOW_TOP),
      ListItem(ListItem.TYPE_SETTING, R.id.btn_openLink, R.drawable.baseline_language_24, Lang.getString(R.string.url_promote), false).setTextColorId(ColorId.textNeutral),
      ListItem(ListItem.TYPE_SHADOW_BOTTOM),
      ListItem(ListItem.TYPE_DESCRIPTION, 0, 0, R.string.SponsoredInfoText2)
    ), false)

    return contentView
  }

  override fun createCustomBottomBar (): ViewGroup? {
    val rv = RecyclerView(context)
    rv.setLayoutManager(LinearLayoutManager(context))
    val sa = object : SettingsAdapter(this) {
      override fun setValuedSetting (item: ListItem, view: SettingView, isUpdate: Boolean) {
        if (item.getId() == R.id.btn_close) {
          view.setIconColorId(ColorId.textNeutral)
        } else {
          view.setIconColorId(ColorId.icon)
        }
      }
    }
    rv.setAdapter(sa)
    sa.setItems(arrayOf(
      ListItem(ListItem.TYPE_SHADOW_TOP),
      ListItem(ListItem.TYPE_SETTING, R.id.btn_close, R.drawable.baseline_check_circle_24, R.string.Continue).setTextColorId(ColorId.textNeutral),
      ListItem(ListItem.TYPE_SETTING, R.id.btn_openLink, R.drawable.baseline_help_24, R.string.SponsoredInfoAction)
    ), false)
    return rv
  }

  override fun getId (): Int {
    return R.id.controller_sponsoredMessagesInfo
  }

  override fun onClick (v: View) {
    if (v.id == R.id.btn_openLink) {
      mediaLayout.hide(false)
      Intents.openUriInBrowser(Uri.parse(Lang.getString(R.string.url_promote)))
    }
  }

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
}
