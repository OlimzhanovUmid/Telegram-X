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
 * File created on 12/08/2017
 */
package org.thunderdog.challegram.ui

import android.content.Context
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.thunderdog.challegram.R
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.tool.Views
import org.thunderdog.challegram.widget.MaterialEditTextGroup
import me.vkryl.android.widget.FrameLayoutFix
import me.vkryl.core.lambda.RunnableBool

class RequestController(context: Context, tdlib: Tdlib) :
  EditBaseController<RequestController.Delegate>(context, tdlib),
  SettingsAdapter.TextChangeListener {
  interface Delegate {
    fun getName (): CharSequence?
    fun getPlaceholder (): Int
    fun performRequest (input: String?, callback: RunnableBool)
  }

  override fun getId (): Int {
    return R.id.controller_request
  }

  override fun getName (): CharSequence? {
    return getArgumentsStrict().getName()
  }

  private lateinit var adapter: SettingsAdapter

  override fun onCreateView (context: Context, contentView: FrameLayoutFix, recyclerView: RecyclerView) {
    adapter = object : SettingsAdapter(this) {
      override fun modifyEditText (item: ListItem, parent: ViewGroup, editText: MaterialEditTextGroup) {
        editText.editText.setInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
        Views.setSingleLine(editText.editText, false)
      }
    }
    adapter.setTextChangeListener(this)
    adapter.setLockFocusOn(this, true)
    adapter.setItems(arrayOf(
      ListItem(ListItem.TYPE_EDITTEXT, R.id.input, 0, getArgumentsStrict().getPlaceholder())
    ), false)

    recyclerView.adapter = adapter
    recyclerView.overScrollMode = View.OVER_SCROLL_NEVER

    setDoneVisible(true)
  }

  private var currentInput: String? = null

  override fun onTextChanged (id: Int, item: ListItem, v: MaterialEditTextGroup) {
    currentInput = v.text.toString()
  }

  override fun onProgressStateChanged (inProgress: Boolean) {
    adapter.updateLockEditTextById(R.id.input, if (inProgress) currentInput else null)
  }

  final override fun onDoneClick (): Boolean {
    if (!isInProgress) {
      isInProgress = true
      getArgumentsStrict().performRequest(currentInput) { result ->
        tdlib!!.ui().post {
          if (!isDestroyed()) {
            isInProgress = false
            if (result) {
              onSaveCompleted()
            }
          }
        }
      }
    }
    return true
  }
}
