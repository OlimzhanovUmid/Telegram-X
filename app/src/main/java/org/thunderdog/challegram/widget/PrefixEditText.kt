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
 * File created on 07/02/2016 at 15:58
 */
package org.thunderdog.challegram.widget

import android.content.Context
import android.text.InputFilter
import android.text.InputType
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.annotation.StringRes
import org.thunderdog.challegram.R
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.UI
import org.thunderdog.challegram.util.CustomTypefaceSpan

class PrefixEditText (context: Context) : EmojiEditText(context), InputFilter, View.OnLongClickListener {
  private var prefix: String? = null
  private var minLength = 0
  private var forceEdit = false
  private var editable = false

  init {
    editable = true
    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
    filters = arrayOf(this)
    setCustomSelectionActionModeCallback(object : ActionMode.Callback {
      override fun onCreateActionMode (mode: ActionMode, menu: Menu): Boolean {
        return false
      }

      override fun onPrepareActionMode (mode: ActionMode, menu: Menu): Boolean {
        return false
      }

      override fun onActionItemClicked (mode: ActionMode, item: MenuItem): Boolean {
        return false
      }

      override fun onDestroyActionMode (mode: ActionMode) {

      }
    })
  }

  fun setPrefix (@StringRes resId: Int) {
    setPrefix(Lang.getString(resId))
  }

  fun setPrefix (prefix: String) {
    this.prefix = prefix
    val spannable: Spannable = SpannableString(prefix)
    if (prefix.length > 0) {
      spannable.setSpan(CustomTypefaceSpan(null, ColorId.textPlaceholder), 0, prefix.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    minLength = spannable.length
    forceText(spannable)
    setSelection(minLength)
  }

  /**
   * aka setText
   */
  var suffix: String
    get () {
      val str = text.toString()
      return if (str.length <= minLength) "" else str.substring(minLength)
    }
    set (value) {
      val spannable: Spannable = SpannableString(prefix + value)
      if (prefix!!.length > 0) {
        spannable.setSpan(ForegroundColorSpan(Theme.textPlaceholderColor()), 0, prefix!!.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
      }
      forceText(spannable)
      setSelection(spannable.length)
    }

  fun setEditable (editable: Boolean) {
    if (this.editable != editable) {
      this.editable = editable
      isClickable = editable
      isFocusable = editable
      isFocusableInTouchMode = editable
      setOnLongClickListener(if (editable) null else this)
    }
  }

  fun forceText (s: Spannable) {
    forceEdit = true
    setText(s, TextView.BufferType.SPANNABLE)
    forceEdit = false
  }

  override fun onLongClick (v: View): Boolean {
    UI.copyText(text.toString(), R.string.CopiedLink)
    return true
  }

  @Suppress("SpellCheckingInspection")
  override fun filter (source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int, dend: Int): CharSequence? {
    return if (forceEdit) null else if (!editable) (if (source.length < 1) dest.subSequence(dstart, dend) else "") else (if (prefix != null && dstart < minLength) dest.subSequence(dstart, dend) else null)
  }

  override fun onSelectionChanged (selStart: Int, selEnd: Int) {
    if (editable && selStart < minLength && text.length >= minLength) {
      setSelection(minLength)
    } else {
      super.onSelectionChanged(selStart, selEnd)
    }
  }
}
