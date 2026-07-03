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
 * File created on 21/02/2016 at 21:28
 */
package org.thunderdog.challegram.component.chat

import android.content.Context
import android.view.View
import android.widget.RelativeLayout
import org.thunderdog.challegram.ui.MessagesController
import me.vkryl.android.animator.Animated

class MessagesLayout (context: Context) : RelativeLayout(context), Animated {
  private lateinit var controller: MessagesController

  fun setController (controller: MessagesController) {
    this.controller = controller
  }

  private var changedMin: Boolean = false
  private var lastMeasuredWidth: Int = 0

  override fun onMeasure (widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val emojiState = controller.emojiState
    val emojiLayout = controller.emojiKeyboardLayout

    val commandsState = controller.commandsState
    val keyboardLayout = controller.keyboardLayout

    val height = measuredHeight
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    changedMin = height > measuredHeight && ((emojiState && emojiLayout != null) || (commandsState && keyboardLayout != null)) && measuredWidth == lastMeasuredWidth
    lastMeasuredWidth = measuredWidth
  }

  private var pendingAction: Runnable? = null

  override fun runOnceViewBecomesReady (view: View, action: Runnable) {
    pendingAction = action
  }

  override fun onLayout (changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    super.onLayout(changed, l, t, r, b)
    if (changedMin) {
      val emojiState = controller.emojiState
      val emojiLayout = controller.emojiKeyboardLayout

      val commandsState = controller.commandsState
      val keyboardLayout = controller.keyboardLayout

      if (emojiState && emojiLayout != null) {
        emojiLayout.onKeyboardStateChanged(true)
      }
      if (commandsState && keyboardLayout != null) {
        keyboardLayout.onKeyboardStateChanged(true)
      }
    }
    if (pendingAction != null) {
      pendingAction!!.run()
      pendingAction = null
    }
  }
}
