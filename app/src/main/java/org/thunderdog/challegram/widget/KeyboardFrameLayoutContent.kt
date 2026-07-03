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
 * File created on 12/06/2024
 */
package org.thunderdog.challegram.widget

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import org.thunderdog.challegram.tool.Keyboard
import me.vkryl.android.widget.FrameLayoutFix
import me.vkryl.core.hasFlag
import me.vkryl.core.setFlag

class KeyboardFrameLayoutContent (context: Context) : FrameLayout(context) {
  companion object {
    const val FLAG_CUSTOM_HEIGHT = 1
    const val FLAG_KEYBOARD_VISIBLE = 1 shl 1
  }

  private var flags = 0
  private var additionalHeight = 0

  private var keyboardView: KeyboardFrameLayout? = null

  @JvmField val emojiLayout: EmojiLayout
  @JvmField val textFormattingLayout: TextFormattingLayout

  init {
    emojiLayout = EmojiLayout(context)
    textFormattingLayout = TextFormattingLayout(context)
    textFormattingLayout.visibility = View.GONE

    addView(emojiLayout, FrameLayoutFix.newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    addView(textFormattingLayout, FrameLayoutFix.newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
  }

  fun getAdditionalHeight (): Int {
    return additionalHeight
  }

  fun setKeyboardView (keyboardView: KeyboardFrameLayout) {
    this.keyboardView = keyboardView
    this.textFormattingLayout.setKeyboardView(keyboardView)
  }

  fun setKeyboardVisible (keyboardVisible: Boolean) {
    this.flags = setFlag(flags, FLAG_KEYBOARD_VISIBLE, keyboardVisible)
  }

  fun setAllowCustomHeight (allowCustomHeight: Boolean) {
    this.flags = setFlag(flags, FLAG_CUSTOM_HEIGHT, allowCustomHeight)
  }

  fun setAdditionalHeight (additionalHeight: Int) {
    this.additionalHeight = additionalHeight
  }

  override fun onMeasure (widthMeasureSpec: Int, heightMeasureSpec: Int) {
    if (!hasFlag(flags, FLAG_CUSTOM_HEIGHT)) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec)
      return
    }

    var height = additionalHeight
    if (!hasFlag(flags, FLAG_KEYBOARD_VISIBLE)) {
      height += Keyboard.getSize()
    }

    super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY))
  }

  /* * */

  fun setTextFormattingLayoutVisible (visible: Boolean): Boolean {
    emojiLayout.optimizeForDisplayTextFormattingLayout(visible)    // todo - just hide view ?
    textFormattingLayout.visibility = if (visible) View.VISIBLE else View.GONE
    if (visible) {
      textFormattingLayout.checkButtonsActive(false)
    }

    return visible
  }
}
