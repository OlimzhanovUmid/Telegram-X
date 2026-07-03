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
import android.view.WindowManager
import androidx.annotation.IntDef
import me.vkryl.android.widget.FrameLayoutFix
import me.vkryl.core.requireNonNull
import org.drinkless.tdlib.TdApi
import org.thunderdog.challegram.R
import org.thunderdog.challegram.navigation.HeaderView
import org.thunderdog.challegram.navigation.ViewController
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.theme.ColorState
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.widget.PopupLayout
import org.thunderdog.challegram.widget.ViewPager

class ChatFolderInviteLinkController (context: Context, tdlib: Tdlib) : BottomSheetViewController<ChatFolderInviteLinkController.Arguments>(context, tdlib) {

  @IntDef(MODE_INVITE_LINK, MODE_NEW_CHATS, MODE_DELETE_FOLDER)
  @Retention(AnnotationRetention.SOURCE)
  annotation class Mode

  class Arguments {
    @JvmField @Mode val mode: Int
    @JvmField val chatFolderId: Int
    @JvmField val chatFolderName: TdApi.ChatFolderName
    @JvmField val selectableChatIds: LongArray
    @JvmField val inviteLinkUrl: String?
    @JvmField val inviteLinkInfo: TdApi.ChatFolderInviteLinkInfo?

    constructor (inviteLink: String, inviteLinkInfo: TdApi.ChatFolderInviteLinkInfo) {
      this.mode = ChatFolderInviteLinkController.MODE_INVITE_LINK
      this.chatFolderId = inviteLinkInfo.chatFolderInfo.id
      this.chatFolderName = inviteLinkInfo.chatFolderInfo.name
      this.selectableChatIds = inviteLinkInfo.missingChatIds
      this.inviteLinkUrl = requireNonNull(inviteLink)
      this.inviteLinkInfo = requireNonNull(inviteLinkInfo)
    }

    private constructor (@Mode mode: Int, chatFolderId: Int, chatFolderName: TdApi.ChatFolderName, chatIds: LongArray) {
      this.mode = mode
      this.chatFolderId = chatFolderId
      this.chatFolderName = chatFolderName
      this.selectableChatIds = chatIds
      this.inviteLinkUrl = null
      this.inviteLinkInfo = null
    }

    companion object {
      @JvmStatic
      fun newChats (chatFolderInfo: TdApi.ChatFolderInfo, chatIds: LongArray): Arguments {
        return newChats(chatFolderInfo.id, chatFolderInfo.name, chatIds)
      }

      @JvmStatic
      fun newChats (chatFolderId: Int, chatFolderName: TdApi.ChatFolderName, chatIds: LongArray): Arguments {
        return Arguments(ChatFolderInviteLinkController.MODE_NEW_CHATS, chatFolderId, chatFolderName, chatIds)
      }

      @JvmStatic
      fun deleteFolder (chatFolderInfo: TdApi.ChatFolderInfo, chatIds: LongArray): Arguments {
        return deleteFolder(chatFolderInfo.id, chatFolderInfo.name, chatIds)
      }

      @JvmStatic
      fun deleteFolder (chatFolderId: Int, chatFolderName: TdApi.ChatFolderName, chatIds: LongArray): Arguments {
        return Arguments(ChatFolderInviteLinkController.MODE_DELETE_FOLDER, chatFolderId, chatFolderName, chatIds)
      }
    }
  }

  private val singlePage: ChatFolderInviteLinkControllerPage = ChatFolderInviteLinkControllerPage(this)

  override fun getId (): Int {
    return R.id.controller_chatFolderInviteLink
  }

  protected override val pagerItemCount: Int
    get() = 1

  protected override fun onBeforeCreateView () {
    singlePage.getValue()
  }

  protected override fun onAfterCreateView () {
    setLickViewColor(Theme.getColor(ColorId.headerLightBackground))
  }

  override fun onThemeColorsChanged (areTemp: Boolean, state: ColorState?) {
    super.onThemeColorsChanged(areTemp, state)
    setLickViewColor(Theme.getColor(ColorId.headerLightBackground))
  }

  override fun setArguments (args: Arguments) {
    super.setArguments(args)
    singlePage.setArguments(args)
  }

  protected override fun onCreateView (context: Context?, contentView: FrameLayoutFix?, pager: ViewPager?) {
    pager?.setOffscreenPageLimit(1)
    tdlib!!.ui().post(this::launchOpenAnimation)
  }

  override fun supportsBottomInset (): Boolean {
    return true
  }

  protected override fun onCreatePagerItemForPosition (context: Context?, position: Int): ViewController<*>? {
    if (position != 0) return null
    setHeaderPosition((contentOffset + HeaderView.getTopOffset()).toFloat())
    setDefaultListenersAndDecorators(singlePage)
    return singlePage
  }

  protected override fun getHeaderHeight (): Int {
    return Screen.dp(56f)
  }

  protected override val contentOffset: Int
    get() = (targetHeight - getHeaderHeight(true)) / 2

  protected override fun canHideByScroll (): Boolean {
    return true
  }

  protected override fun onCreateHeaderView (): HeaderView? {
    return singlePage.getHeaderView()
  }

  override fun needsTempUpdates (): Boolean {
    return true
  }

  protected override fun setupPopupLayout (popupLayout: PopupLayout) {
    popupLayout.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    popupLayout.boundController = singlePage
    popupLayout.setPopupHeightProvider(this)
    popupLayout.init(true)
    popupLayout.setHideKeyboard()
    popupLayout.setNeedRootInsets()
    popupLayout.setTouchProvider(this)
    popupLayout.setIgnoreHorizontal()
  }

  companion object {
    const val MODE_INVITE_LINK: Int = 0
    const val MODE_NEW_CHATS: Int = 1
    const val MODE_DELETE_FOLDER: Int = 2
  }
}
