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
package org.thunderdog.challegram.ui

import android.content.Context
import android.text.InputFilter
import android.text.InputType
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import androidx.recyclerview.widget.RecyclerView
import org.drinkless.tdlib.TdApi
import org.thunderdog.challegram.R
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.emoji.EmojiFilter
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.util.CharacterStyleFilter
import org.thunderdog.challegram.widget.MaterialEditTextGroup
import me.vkryl.android.text.CodePointCountFilter
import me.vkryl.android.widget.FrameLayoutFix

/**
 * "New Topic" screen: a name field plus an icon picker (default colors + default custom-emoji icons).
 * Reports the chosen name + icon through [Args.callback].
 */
class CreateTopicController(context: Context, tdlib: Tdlib) :
  EditBaseController<CreateTopicController.Args>(context, tdlib),
  SettingsAdapter.TextChangeListener {

  fun interface Callback {
    fun onCreateTopic(name: String, icon: TdApi.ForumTopicIcon)
  }

  class Args(val chatId: Long, val callback: Callback)

  private lateinit var adapter: SettingsAdapter
  private lateinit var nameItem: ListItem
  private var pickerView: TopicIconPickerView? = null

  override fun getId (): Int = R.id.controller_createTopic

  override fun getName (): CharSequence = Lang.getString(R.string.NewTopic)

  override fun onCreateView (context: Context, contentView: FrameLayoutFix, recyclerView: RecyclerView) {
    adapter = object : SettingsAdapter(this) {
      override fun modifyEditText (item: ListItem, parent: ViewGroup, editText: MaterialEditTextGroup) {
        editText.editText.setInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
      }

      override fun initCustom (parent: ViewGroup): SettingHolder {
        val picker = TopicIconPickerView(context, tdlib)
        pickerView = picker
        picker.load()
        val scroll = HorizontalScrollView(context)
        scroll.isHorizontalScrollBarEnabled = false
        scroll.layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        scroll.addView(picker, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        return SettingHolder(scroll)
      }
    }
    adapter.setLockFocusOn(this, true)
    adapter.setTextChangeListener(this)

    nameItem = ListItem(ListItem.TYPE_EDITTEXT, R.id.edit_topicName, 0, R.string.TopicNameHint)
      .setStringValue("")
      .setInputFilters(arrayOf<InputFilter>(
        CodePointCountFilter(MAX_TOPIC_NAME_LENGTH),
        EmojiFilter(),
        CharacterStyleFilter()
      ))
      .setOnEditorActionListener(SimpleEditorActionListener(EditorInfo.IME_ACTION_DONE, this))

    val items = ArrayList<ListItem>()
    items.add(ListItem(ListItem.TYPE_SHADOW_TOP))
    items.add(nameItem)
    items.add(ListItem(ListItem.TYPE_SHADOW_BOTTOM))
    items.add(ListItem(ListItem.TYPE_SHADOW_TOP))
    items.add(ListItem(ListItem.TYPE_CUSTOM_SINGLE, R.id.btn_color))
    items.add(ListItem(ListItem.TYPE_SHADOW_BOTTOM))
    adapter.setItems(items, false)
    recyclerView.adapter = adapter

    setDoneIcon(R.drawable.baseline_check_24)
    setDoneVisible(false)
  }

  override fun onTextChanged (id: Int, item: ListItem, v: MaterialEditTextGroup) {
    if (id == R.id.edit_topicName) {
      nameItem.setStringValue(v.text.toString())
      setDoneVisible(nameItem.stringValue.trim().isNotEmpty())
    }
  }

  override fun onDoneClick (): Boolean {
    val name = nameItem.stringValue.trim()
    if (name.isEmpty()) {
      return true
    }
    val icon = pickerView?.buildResult() ?: TdApi.ForumTopicIcon(DEFAULT_TOPIC_COLOR, 0L)
    getArgumentsStrict().callback.onCreateTopic(name, icon)
    onSaveCompleted()
    return true
  }

  companion object {
    private const val MAX_TOPIC_NAME_LENGTH = 128
    private const val DEFAULT_TOPIC_COLOR = 0x6FB9F0
  }
}
