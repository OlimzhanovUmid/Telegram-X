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
 * File created on 16/08/2015 at 16:17
 */
package org.thunderdog.challegram.util

import android.view.View
import org.drinkless.tdlib.TdApi
import org.thunderdog.challegram.ui.ContactsController

fun interface SenderPickerDelegate {
  fun onSenderPick (context: ContactsController, view: View?, senderId: TdApi.MessageSender?): Boolean // true if no confirm required
  fun onSenderConfirm (context: ContactsController, senderId: TdApi.MessageSender, option: Int) { } // called if onSenderPick returned false
  fun allowGlobalSearch (): Boolean = false
  fun getUserPickTitle (): String? = null
}
