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
 * File created on 26/04/2015 at 14:53
 */
package org.thunderdog.challegram.component.dialogs

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.Px
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import org.thunderdog.challegram.R
import org.thunderdog.challegram.component.dialogs.ChatsAdapter.ViewType
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.data.TGChat
import org.thunderdog.challegram.navigation.ViewController
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Fonts
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.ui.ChatsController
import org.thunderdog.challegram.ui.ListItem
import org.thunderdog.challegram.ui.SettingHolder
import org.thunderdog.challegram.unsorted.Settings
import org.thunderdog.challegram.widget.BaseView
import org.thunderdog.challegram.widget.ListInfoView
import org.thunderdog.challegram.widget.NoScrollTextView
import org.thunderdog.challegram.widget.SuggestedChatsView

class ChatsViewHolder (itemView: View) : RecyclerView.ViewHolder(itemView) {
  fun setChat (chat: TGChat, needBackground: Boolean, noSeparator: Boolean, isSelected: Boolean) {
    (itemView as ChatView).setChat(chat)
    (itemView as ChatView).setNeedBackground(needBackground)
    (itemView as ChatView).setIsSelected(isSelected, false)
  }

  fun setInfo (info: CharSequence?) {
    if (info == null) {
      (itemView as ListInfoView).showProgress()
    } else {
      (itemView as ListInfoView).showInfo(info)
    }
  }

  fun setEmpty (empty: Int) {
    (itemView as ListInfoView).showEmpty(Lang.getString(empty))
  }

  fun setChatIds (chatIds: LongArray) {
    val hasContent = itemView.getTag(R.id.state) == true
    val animated = ViewCompat.isAttachedToWindow(itemView) && hasContent
    itemView.setTag(R.id.state, chatIds.isNotEmpty())
    (itemView as SuggestedChatsView).setChatIds(chatIds, animated)
  }

  companion object {
    @Px
    @JvmStatic
    fun measureHeightForType (@ViewType viewType: Int): Int {
      return when (viewType) {
        ChatsAdapter.VIEW_TYPE_CHAT ->
          ChatView.getViewHeight(Settings.instance().chatListMode)
        ChatsAdapter.VIEW_TYPE_INFO ->
          SettingHolder.measureHeightForType(ListItem.TYPE_LIST_INFO_VIEW)
        ChatsAdapter.VIEW_TYPE_SUGGESTED_CHATS ->
          Screen.dp(SuggestedChatsView.DEFAULT_HEIGHT_DP)
        else ->
          throw IllegalArgumentException("viewType = $viewType")
      }
    }

    @JvmStatic
    fun create (context: Context, tdlib: Tdlib, @ViewType viewType: Int, parentController: ChatsController?, themeProvider: ViewController<*>?, actionListProvider: BaseView.ActionListProvider): ChatsViewHolder {
      when (viewType) {
        ChatsAdapter.VIEW_TYPE_CHAT -> {
          val view = ChatView(context, tdlib)
          view.setPreviewActionListProvider(actionListProvider)
          view.setLongPressInterceptor(parentController)
          if (parentController != null) {
            view.setAnimationsDisabled(parentController.isLaunching)
            view.setOnClickListener(parentController)
            view.setOnLongClickListener(parentController)
          } else {
            view.isEnabled = false
            view.setOnClickListener(null)
            view.setOnLongClickListener(null)
          }
          if (themeProvider != null) {
            themeProvider.addThemeInvalidateListener(view)
          }
          return ChatsViewHolder(view)
        }
        ChatsAdapter.VIEW_TYPE_INFO -> {
          val view = ListInfoView(context)
          if (themeProvider != null) {
            view.addThemeListeners(themeProvider)
          }
          return ChatsViewHolder(view)
        }
        ChatsAdapter.VIEW_TYPE_EMPTY -> {
          val textView: TextView = NoScrollTextView(context)
          textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f)
          textView.typeface = Fonts.getRobotoRegular()
          textView.setPadding(Screen.dp(16f), Screen.dp(16f), Screen.dp(16f), Screen.dp(16f))
          textView.gravity = Gravity.CENTER
          textView.setTextColor(Theme.textDecentColor())
          if (themeProvider != null) {
            themeProvider.addThemeTextColorListener(textView, ColorId.textLight)
          }
          textView.layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
          return ChatsViewHolder(textView)
        }
        ChatsAdapter.VIEW_TYPE_SUGGESTED_CHATS -> {
          val view = SuggestedChatsView(context, tdlib)
          view.id = R.id.btn_chatsSuggestion
          view.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
          view.setOnClickListener(parentController)
          if (themeProvider != null) {
            themeProvider.addThemeInvalidateListener(view)
          }
          return ChatsViewHolder(view)
        }
        else -> throw IllegalArgumentException("viewType == $viewType")
      }
    }
  }
}
