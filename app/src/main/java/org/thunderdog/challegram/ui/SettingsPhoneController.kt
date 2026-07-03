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
 * File created on 21/12/2016
 */
package org.thunderdog.challegram.ui

import android.content.Context
import android.view.View
import org.drinkless.tdlib.TdApi
import org.thunderdog.challegram.R
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.tool.Strings
import org.thunderdog.challegram.util.OptionDelegate
import org.thunderdog.challegram.v.CustomRecyclerView

class SettingsPhoneController (context: Context, tdlib: Tdlib) : RecyclerViewController<Void>(context, tdlib), View.OnClickListener {
  override fun getId (): Int {
    return R.id.controller_editPhone
  }

  override fun getName (): CharSequence {
    val user: TdApi.User? = tdlib!!.myUser()
    return if (user != null) Strings.formatPhone(user.phoneNumber) else Lang.getString(R.string.PhoneNumberChange)
  }

  override fun onCreateView (context: Context, recyclerView: CustomRecyclerView) {
    val adapter = SettingsAdapter(this)
    adapter.setItems(arrayOf(
      ListItem(ListItem.TYPE_ICONIZED_EMPTY, R.id.changePhoneText, R.drawable.baseline_sim_card_96, Strings.replaceBoldTokens(Lang.getString(R.string.PhoneNumberHelp), ColorId.background_textLight), false),
      ListItem(ListItem.TYPE_SHADOW_TOP),
      ListItem(ListItem.TYPE_BUTTON, R.id.btn_changePhoneNumber, 0, R.string.PhoneNumberChange),
      ListItem(ListItem.TYPE_SHADOW_BOTTOM)
    ), false)
    recyclerView.adapter = adapter
  }

  override fun onClick (v: View) {
    if (v.id == R.id.btn_changePhoneNumber) {
      showOptions(Lang.getString(R.string.PhoneNumberAlert), intArrayOf(R.id.btn_edit, R.id.btn_cancel), arrayOf(Lang.getString(R.string.PhoneNumberChangeDone), Lang.getString(R.string.Cancel)), null, intArrayOf(R.drawable.baseline_check_circle_24, R.drawable.baseline_cancel_24), OptionDelegate { itemView, id ->
        if (id == R.id.btn_edit) {
          val c = PhoneController(context, tdlib!!)
          c.setMode(PhoneController.MODE_CHANGE_NUMBER)
          navigateTo(c)
        }
        true
      })
    }
  }
}
