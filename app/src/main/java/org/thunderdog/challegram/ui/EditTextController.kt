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
 * File created on 24/12/2023 at 22:32
 */
package org.thunderdog.challegram.ui

import android.content.Context
import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.recyclerview.widget.RecyclerView
import me.vkryl.android.text.CodePointCountFilter
import me.vkryl.android.widget.FrameLayoutFix
import org.thunderdog.challegram.R
import org.thunderdog.challegram.emoji.EmojiFilter
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.tool.Views
import org.thunderdog.challegram.util.CharacterStyleFilter
import org.thunderdog.challegram.v.EditText
import org.thunderdog.challegram.widget.DoneButton
import org.thunderdog.challegram.widget.MaterialEditTextGroup
import java.util.ArrayList
import java.util.Collections

abstract class EditTextController<T>(context: Context, tdlib: Tdlib) : EditBaseController<T>(context, tdlib) {
  interface Delegate {
    @IdRes fun getId (): Int
    @DrawableRes fun getDoneIcon (): Int = 0
    fun getName (): CharSequence
    fun getHint (): CharSequence
    fun getDescription (): CharSequence
    fun getCurrentValue (): String? = null
    fun onValueChanged (controller: EditTextController<*>, value: CharSequence) {
      controller.setDoneVisible(allowEmptyValue() || !value.isBlank())
    }
    fun onDonePressed (controller: EditTextController<*>, button: DoneButton, value: CharSequence): Boolean
    fun getMaxLength (): Int = 0
    fun allowEmptyValue (): Boolean = true
    fun needFocusInput (): Boolean = true
  }

  private var delegate: Delegate? = null
  private var adapter: SettingsAdapter? = null
  private var currentValue: String? = null

  fun setDelegate (delegate: Delegate) {
    this.delegate = delegate
  }

  override fun getId (): Int = delegate!!.getId()

  override fun getName (): CharSequence = delegate!!.getName()

  override fun onCreateView (context: Context, contentView: FrameLayoutFix, recyclerView: RecyclerView) {
    val localDelegate = delegate!!
    val maxLength = localDelegate.getMaxLength()
    adapter = object : SettingsAdapter(this) {
      override fun modifyEditText (item: ListItem, parent: ViewGroup, editText: MaterialEditTextGroup) {
        editText.editText.setInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
        Views.setSingleLine(editText.editText, false)
        if (maxLength > 0) {
          editText.setMaxLength(maxLength)
        }
      }
    }

    currentValue = localDelegate.getCurrentValue()
    val hint = localDelegate.getHint()
    val item = ListItem(if (maxLength > 0) ListItem.TYPE_EDITTEXT_COUNTERED else ListItem.TYPE_EDITTEXT, R.id.input, 0, hint, false)
    if (!currentValue.isNullOrEmpty()) {
      item.setStringValue(currentValue)
    }

    val inputFilters = ArrayList<InputFilter>()
    if (maxLength > 0) {
      inputFilters.add(CodePointCountFilter(maxLength))
    }
    Collections.addAll(inputFilters, EmojiFilter(), CharacterStyleFilter())
    item.setInputFilters(inputFilters.toTypedArray())

    val items = ArrayList<ListItem>()
    items.add(item)

    val description = localDelegate.getDescription()
    if (!description.isNullOrEmpty()) {
      items.add(ListItem(ListItem.TYPE_DESCRIPTION, R.id.description, 0, description, false).setTextColorId(ColorId.textLight))
    }

    adapter!!.setTextChangeListener { _, _, v ->
      currentValue = EditText.nonModifiableCopy(v.text).toString()
      localDelegate.onValueChanged(this, currentValue!!)
    }
    adapter!!.setLockFocusOn(this, localDelegate.needFocusInput())
    adapter!!.setItems(items, false)

    recyclerView.adapter = adapter
    recyclerView.overScrollMode = View.OVER_SCROLL_NEVER

    setDoneVisible(localDelegate.allowEmptyValue())
    val iconRes = localDelegate.getDoneIcon()
    if (iconRes != 0) {
      setDoneIcon(iconRes)
    }
  }

  override fun onProgressStateChanged (inProgress: Boolean) {
    adapter!!.updateLockEditTextById(R.id.input, if (inProgress) currentValue else null)
  }

  override fun onDoneClick (): Boolean {
    return delegate!!.onDonePressed(this, getDoneButton()!!, currentValue!!)
  }
}
