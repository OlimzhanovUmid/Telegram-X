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
 * File created on 19/02/2019
 */
package org.thunderdog.challegram.ui

import android.content.Context
import android.view.View
import androidx.annotation.IdRes
import androidx.annotation.IntDef
import org.thunderdog.challegram.R
import org.thunderdog.challegram.component.base.SettingView
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.data.TGFoundChat
import org.thunderdog.challegram.navigation.DoubleHeaderView
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.tool.Strings
import org.thunderdog.challegram.unsorted.Passcode
import org.thunderdog.challegram.v.CustomRecyclerView
import org.thunderdog.challegram.widget.BetterChatView
import java.util.ArrayList

class SettingsLogOutController (context: Context, tdlib: Tdlib) : RecyclerViewController<Int>(context, tdlib), View.OnClickListener {
  @Retention(AnnotationRetention.SOURCE)
  @IntDef(Type.LOG_OUT, Type.DELETE_ACCOUNT)
  annotation class Type {
    companion object {
      const val LOG_OUT = 0
      const val DELETE_ACCOUNT = 1
    }
  }

  @Type
  private fun getType (): Int {
    return if (getArguments() == null) Type.LOG_OUT else getArgumentsStrict()
  }

  override fun getId (): Int {
    return R.id.controller_logOut
  }

  override fun getName (): CharSequence {
    return when (getType()) {
      Type.LOG_OUT -> Lang.getString(R.string.LogOut)
      Type.DELETE_ACCOUNT -> Lang.getString(R.string.DeleteAccount)
      else -> throw UnsupportedOperationException()
    }
  }

  private lateinit var adapter: SettingsAdapter
  private var headerCell: DoubleHeaderView? = null

  override fun getCustomHeaderCell (): View? {
    return headerCell
  }

  override fun needPersistentScrollPosition (): Boolean {
    return true
  }

  override fun onCreateView (context: Context, recyclerView: CustomRecyclerView) {
    val headerCell = DoubleHeaderView(context)
    headerCell.setThemedTextColor(this)
    headerCell.initWithMargin(0, true)
    headerCell.setTitle(getName())
    headerCell.setSubtitle(Lang.getString(R.string.SignOutAlt))
    this.headerCell = headerCell

    adapter = object : SettingsAdapter(this) {
      override fun setValuedSetting (item: ListItem, view: SettingView, isUpdate: Boolean) {
        @IdRes val itemId = item.getId()
        view.setIconColorId(if (itemId == R.id.btn_logout || itemId == R.id.btn_deleteAccount) ColorId.iconNegative else ColorId.NONE)
      }

      override fun setChatData (item: ListItem, position: Int, chatView: BetterChatView) {
        chatView.setEnabled(false)
        chatView.setChat(item.getData() as TGFoundChat)
      }
    }

    val items = ArrayList<ListItem>()

    @Type val type = getType()

    val chat = TGFoundChat(tdlib!!, tdlib!!.mySender(), true)
    chat.setForcedSubtitle(Strings.formatPhone(tdlib!!.account()!!.getPhoneNumber()))
    items.add(ListItem(ListItem.TYPE_CHAT_BETTER).setData(chat))
    items.add(ListItem(ListItem.TYPE_SEPARATOR))
    items.add(ListItem(ListItem.TYPE_SETTING, R.id.btn_addAccount, R.drawable.baseline_person_add_24, R.string.SignOutAltAddAccount))
    items.add(ListItem(ListItem.TYPE_SHADOW_BOTTOM))
    items.add(ListItem(ListItem.TYPE_DESCRIPTION, 0, 0, R.string.SignOutAltAddAccountHint))

    if (!Passcode.instance().isEnabled()) {
      items.add(ListItem(ListItem.TYPE_SHADOW_TOP))
      items.add(ListItem(ListItem.TYPE_SETTING, R.id.btn_passcode, R.drawable.baseline_lock_24, R.string.SignOutAltPasscode))
      items.add(ListItem(ListItem.TYPE_SHADOW_BOTTOM))
      items.add(ListItem(ListItem.TYPE_DESCRIPTION, 0, 0, R.string.SignOutAltPasscodeHint))
    }

    items.add(ListItem(ListItem.TYPE_SHADOW_TOP))
    items.add(ListItem(ListItem.TYPE_SETTING, R.id.btn_storageUsage, R.drawable.templarian_baseline_broom_24, R.string.SignOutAltClearCache))
    items.add(ListItem(ListItem.TYPE_SHADOW_BOTTOM))
    items.add(ListItem(ListItem.TYPE_DESCRIPTION, 0, 0, R.string.SignOutAltClearCacheHint))

    items.add(ListItem(ListItem.TYPE_SHADOW_TOP))
    items.add(ListItem(ListItem.TYPE_SETTING, R.id.btn_changePhoneNumber, R.drawable.baseline_sim_card_24, R.string.SignOutAltChangeNumber))
    items.add(ListItem(ListItem.TYPE_SHADOW_BOTTOM))
    items.add(ListItem(ListItem.TYPE_DESCRIPTION, 0, 0, R.string.SignOutAltChangeNumberHint))

    items.add(ListItem(ListItem.TYPE_SHADOW_TOP))
    items.add(ListItem(ListItem.TYPE_SETTING, R.id.btn_help, R.drawable.baseline_help_24, R.string.SignOutAltHelp))
    items.add(ListItem(ListItem.TYPE_SHADOW_BOTTOM))
    items.add(ListItem(ListItem.TYPE_DESCRIPTION, 0, 0, if (type == Type.DELETE_ACCOUNT) R.string.DeleteAccountHelpHint else R.string.SignOutAltHelpHint))

    items.add(ListItem(ListItem.TYPE_SHADOW_TOP))
    items.add(ListItem(ListItem.TYPE_SETTING, R.id.btn_logout, R.drawable.baseline_logout_24, R.string.LogOut).setTextColorId(ColorId.textNegative))
    items.add(ListItem(ListItem.TYPE_SHADOW_BOTTOM))
    items.add(ListItem(ListItem.TYPE_DESCRIPTION, 0, 0, if (type == Type.DELETE_ACCOUNT) R.string.DeleteAccountSignOutAltHint2 else R.string.SignOutAltHint2))

    if (type == Type.DELETE_ACCOUNT) {
      items.add(ListItem(ListItem.TYPE_SHADOW_TOP))
      items.add(ListItem(ListItem.TYPE_SETTING, R.id.btn_deleteAccount, R.drawable.baseline_delete_alert_24, R.string.DeleteAccountBtn).setTextColorId(ColorId.textNegative))
      items.add(ListItem(ListItem.TYPE_SHADOW_BOTTOM))
      items.add(ListItem(ListItem.TYPE_DESCRIPTION, 0, 0, R.string.DeleteAccountInfo))
    }

    adapter.setItems(items, false)

    recyclerView.setAdapter(adapter)
  }

  override fun onClick (v: View) {
    val viewId = v.id
    if (viewId == R.id.btn_addAccount) {
      tdlib!!.ui().addAccount(context, true, false)
    } else if (viewId == R.id.btn_passcode) {
      if (!Passcode.instance().isEnabled()) {
        navigateTo(PasscodeSetupController(context, tdlib!!))
      }
    } else if (viewId == R.id.btn_storageUsage) {
      navigateTo(SettingsCacheController(context, tdlib!!))
    } else if (viewId == R.id.btn_changePhoneNumber) {
      navigateTo(SettingsPhoneController(context, tdlib!!))
    } else if (viewId == R.id.btn_help) {
      tdlib!!.ui().openSupport(this)
    } else if (viewId == R.id.btn_logout) {
      tdlib!!.ui().logOut(this, false)
    } else if (viewId == R.id.btn_deleteAccount) {
      tdlib!!.ui().permanentlyDeleteAccount(this, false)
    }
  }
}
