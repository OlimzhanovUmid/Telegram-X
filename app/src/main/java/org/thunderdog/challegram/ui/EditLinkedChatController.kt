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
 * File created on 16/12/2019
 */
package org.thunderdog.challegram.ui

import android.content.Context
import android.view.View
import org.thunderdog.challegram.R
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.v.CustomRecyclerView

class EditLinkedChatController (context: Context, tdlib: Tdlib?) : RecyclerViewController<EditLinkedChatController.Args>(context, tdlib), View.OnClickListener {
  class Args (val chatId: Long)

  override fun getId (): Int {
    return R.id.controller_linkChat
  }

  private lateinit var adapter: SettingsAdapter

  override fun onCreateView (context: Context, recyclerView: CustomRecyclerView) {
    adapter = SettingsAdapter(this, this, this)
    val items: MutableList<ListItem> = ArrayList()

    recyclerView.setAdapter(adapter)
  }

  override fun onClick (v: View) {
    if (v.id == R.id.chat) {
      // ...
    }
  }
}
