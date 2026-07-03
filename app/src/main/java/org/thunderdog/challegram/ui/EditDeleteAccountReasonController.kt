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
 * File created on 25/12/2023 at 00:17
 */
package org.thunderdog.challegram.ui

import android.content.Context
import me.vkryl.core.isEmptyOrBlank
import org.drinkless.tdlib.TdApi
import org.thunderdog.challegram.R
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.data.TD
import org.thunderdog.challegram.navigation.ViewController
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.tool.Strings
import org.thunderdog.challegram.util.OptionDelegate
import org.thunderdog.challegram.widget.DoneButton

class EditDeleteAccountReasonController (context: Context, tdlib: Tdlib) : EditTextController<String>(context, tdlib) {
  init {
    setDelegate(object : Delegate {
      override fun getId (): Int {
        return R.id.controller_deleteAccount
      }

      override fun getDoneIcon (): Int {
        return R.drawable.baseline_delete_alert_24
      }

      override fun getName (): CharSequence {
        return Lang.getString(R.string.DeleteAccount)
      }

      override fun getHint (): CharSequence {
        return Lang.getString(R.string.DeleteAccountReason)
      }

      override fun getDescription (): CharSequence {
        return Lang.getMarkdownString(this@EditDeleteAccountReasonController, R.string.DeleteAccountDescription)
      }

      override fun allowEmptyValue (): Boolean {
        return false
      }

      override fun needFocusInput (): Boolean {
        return false
      }

      override fun onDonePressed (controller: EditTextController<*>, button: DoneButton, value: CharSequence): Boolean {
        if (isInProgress)
          return false
        if (!isEmptyOrBlank(value)) {
          showOptions(
            Lang.getMarkdownString(controller, R.string.DeleteAccountConfirmFinal),
            intArrayOf(R.id.btn_deleteAccount, R.id.btn_cancel),
            arrayOf(Lang.getString(R.string.DeleteAccountConfirmFinalBtn), Lang.getString(R.string.Cancel)),
            intArrayOf(ViewController.OptionColor.RED, ViewController.OptionColor.NORMAL),
            intArrayOf(R.drawable.baseline_delete_alert_24, R.drawable.baseline_cancel_24),
            OptionDelegate { _, id ->
              if (id == R.id.btn_deleteAccount) {
                val account = tdlib.account()!!
                val name = account.getName()
                val phoneNumber = account.getPhoneNumber()
                isDoneInProgress = true
                setStackLocked(true)
                tdlib.send(TdApi.DeleteAccount(value.toString(), getArguments())) { _, error ->
                  runOnUiThreadOptional(Runnable {
                    setStackLocked(false)
                    isDoneInProgress = false
                    if (error != null) {
                      context().tooltipManager()
                        .builder(getDoneButton()!!)
                        .icon(R.drawable.baseline_warning_24)
                        .show(tdlib, TD.toErrorString(error))
                    } else {
                      tdlib.switchToNextAuthorizedAccount()
                      openAlert(R.string.AccountDeleted, Lang.getMarkdownString(controller, R.string.AccountDeletedText, name, Strings.formatPhone(phoneNumber)))
                    }
                  })
                }
              }
              true
            }
          )
          return true
        }
        return false
      }
    })
    addOneShotFocusListener {
      val c: ViewController<*>? = navigationController()!!.getStack().getPrevious()
      if (c is PasswordController) { // Password confirmation
        destroyPreviousStackItem()
      }
    }
  }
}
