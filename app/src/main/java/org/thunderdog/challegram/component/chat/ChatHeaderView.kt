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
 * File created on 10/08/2015 at 19:56
 */
package org.thunderdog.challegram.component.chat

import android.content.Context
import android.view.MotionEvent
import org.drinkless.tdlib.TdApi
import org.thunderdog.challegram.data.ThreadInfo
import org.thunderdog.challegram.loader.AvatarReceiver
import org.thunderdog.challegram.navigation.ComplexHeaderView
import org.thunderdog.challegram.navigation.HeaderView
import org.thunderdog.challegram.navigation.ViewController
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.theme.ThemeDeprecated
import org.thunderdog.challegram.tool.Screen
import me.vkryl.core.equalsOrBothEmpty
import me.vkryl.core.isEmpty
import tgx.td.isSecret

class ChatHeaderView (context: Context, tdlib: Tdlib, parent: ViewController<*>?) : ComplexHeaderView(context, tdlib, parent) {
  interface Callback {
    fun onChatHeaderClick ()
  }

  private var callback: Callback? = null

  init {
    setPhotoOpenDisabled(true)
    setOnClickListener {
      callback?.onChatHeaderClick()
    }
    setUseDefaultClickListener(true)
    setBackgroundResource(ThemeDeprecated.headerSelector())
    setInnerMargins(Screen.dp(56f), Screen.dp(49f))
  }

  private var forcedSubtitle: CharSequence? = null

  fun setForcedSubtitle (subtitle: CharSequence?) {
    if (!forcedSubtitle.equalsOrBothEmpty(subtitle)) {
      this.forcedSubtitle = subtitle
      setNoStatus(!isEmpty(subtitle))
      if (hasSubtitle()) {
        setSubtitle(subtitle)
      }
    }
  }

  fun setCallback (callback: Callback?) {
    this.callback = callback
  }

  override fun onMeasure (widthMeasureSpec: Int, heightMeasureSpec: Int) {
    setMeasuredDimension(widthMeasureSpec, HeaderView.getSize(scaleFactor != 0f, true))
  }

  override fun setScaleFactor (scaleFactor: Float, fromFactor: Float, toScaleFactor: Float, byScroll: Boolean) {
    if (this.scaleFactor != scaleFactor) {
      val layout = this.scaleFactor == 0f || scaleFactor == 0f
      super.setScaleFactor(scaleFactor, fromFactor, toScaleFactor, byScroll)
      if (layout) {
        setEnabled(scaleFactor == 0f)
        requestLayout()
      }
    }
  }

  override fun onTouchEvent (e: MotionEvent): Boolean {
    return callback != null && super.onTouchEvent(e)
  }

  fun setChat (tdlib: Tdlib, chat: TdApi.Chat?, messageThread: ThreadInfo?) {
    this.chatTdlib = tdlib

    if (chat == null) {
      setText("Debug controller", "nobody should find this view")
      return
    }

    getAvatarReceiver().requestChat(tdlib, chat.id, AvatarReceiver.Options.FULL_SIZE)
    setShowVerify(tdlib.chatVerified(chat))
    setShowScam(tdlib.chatScam(chat))
    setShowFake(tdlib.chatFake(chat))
    setShowMute(tdlib.chatNeedsMuteIcon(chat))
    setShowLock(isSecret(chat.id))
    if (messageThread != null) {
      setEmojiStatus(null)
      setText(messageThread.chatHeaderTitle(), if (!isEmpty(forcedSubtitle)) forcedSubtitle else messageThread.chatHeaderSubtitle())
      setExpandedSubtitle(null)
      setUseRedHighlight(false)
      attachChatStatus(messageThread.getChatId(), messageThread.getMessageTopicId())
    } else {
      setEmojiStatus(if (tdlib.isSelfChat(chat)) null else tdlib.chatUser(chat))
      setText(tdlib.chatTitle(chat), if (!isEmpty(forcedSubtitle)) forcedSubtitle else tdlib.status().chatStatus(chat))
      setExpandedSubtitle(tdlib.status().chatStatusExpanded(chat))
      setUseRedHighlight(tdlib.isRedTeam(chat.id))
      attachChatStatus(chat.id, null)
    }
  }

  // Updates (new)

  private var chatTdlib: Tdlib? = null

  fun updateUserStatus (chat: TdApi.Chat) {
    if (isEmpty(forcedSubtitle)) {
      setSubtitle(chatTdlib!!.status().chatStatus(chat))
      setExpandedSubtitle(chatTdlib!!.status().chatStatusExpanded(chat))
    }
  }
}
