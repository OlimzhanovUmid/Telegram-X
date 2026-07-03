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
 * File created on 31/08/2022, 22:07.
 */

package org.thunderdog.challegram.widget

import android.content.Context
import android.text.InputFilter
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection

import androidx.annotation.CallSuper

import org.thunderdog.challegram.emoji.CustomEmojiSurfaceProvider
import org.thunderdog.challegram.emoji.EmojiFilter
import org.thunderdog.challegram.emoji.EmojiInputConnection
import org.thunderdog.challegram.emoji.EmojiUpdater
import org.thunderdog.challegram.util.DestroySpansWatcher

import java.util.Collections

import me.vkryl.core.lambda.Destroyable

open class EmojiTextView @JvmOverloads constructor (context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : TextView(context, attrs, defStyleAttr), Destroyable {
  private var emojiUpdater: EmojiUpdater? = null

  init {
    addTextChangedListener(DestroySpansWatcher())
    setFilters(arrayOf<InputFilter>())
  }

  final override fun setFilters (filters: Array<InputFilter>) {
    if (emojiUpdater == null)
      emojiUpdater = EmojiUpdater(this)
    super.setFilters(newFilters(this, filters, emojiUpdater!!))
  }

  @CallSuper
  override fun performDestroy () {
    emojiUpdater!!.performDestroy()
  }

  final override fun onCreateInputConnection (editorInfo: EditorInfo): InputConnection? {
    val ic = createInputConnection(editorInfo)
    return if (ic != null && ic !is EmojiInputConnection) {
      EmojiInputConnection(this, ic)
    } else {
      ic
    }
  }

  protected open fun createInputConnection (editorInfo: EditorInfo): InputConnection? {
    return super.onCreateInputConnection(editorInfo)
  }

  companion object {
    @JvmStatic
    fun newFilters (textView: android.widget.TextView, filters: Array<InputFilter>, emojiUpdater: InputFilter): Array<InputFilter> {
      var emojiFilterIndex = -1
      var emojiUpdaterIndex = -1
      for (i in filters.indices) {
        val filter = filters[i]
        if (filter is EmojiFilter) {
          emojiFilterIndex = i
        } else if (filter === emojiUpdater) {
          emojiUpdaterIndex = i
        } else {
          continue
        }
        if (emojiFilterIndex != -1 && emojiUpdaterIndex != -1) {
          return filters
        }
      }
      val filtersList: MutableList<InputFilter> = ArrayList(
        (if (emojiFilterIndex == -1) 1 else 0) +
          (if (emojiUpdaterIndex == -1) 1 else 0) +
          filters.size
      )
      if (emojiFilterIndex == -1) {
        val customEmojiSurfaceProvider = textView as? CustomEmojiSurfaceProvider
        filtersList.add(EmojiFilter(customEmojiSurfaceProvider))
        if (emojiUpdaterIndex == -1) {
          filtersList.add(emojiUpdater)
        }
        Collections.addAll(filtersList, *filters)
      } else {
        Collections.addAll(filtersList, *filters)
        filtersList.add(emojiFilterIndex + 1, emojiUpdater)
      }
      return filtersList.toTypedArray()
    }
  }
}
