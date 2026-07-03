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
 * File created on 05/12/2016
 */
package org.thunderdog.challegram.ui

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.webkit.WebView
import android.widget.LinearLayout
import org.drinkless.tdlib.TdApi
import org.thunderdog.challegram.R
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.navigation.DoubleHeaderView
import org.thunderdog.challegram.navigation.HeaderView
import org.thunderdog.challegram.navigation.Menu
import org.thunderdog.challegram.navigation.MoreDelegate
import org.thunderdog.challegram.navigation.NavigationController
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.tool.Intents
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.tool.UI
import org.thunderdog.challegram.unsorted.Size
import org.thunderdog.challegram.v.WebViewProxy

class GameController (context: Context, tdlib: Tdlib?) : WebkitController<GameController.Args>(context, tdlib), Menu, MoreDelegate {
  class Args (
    @JvmField val userId: Long,
    @JvmField val game: TdApi.Game?,
    @JvmField val username: String,
    @JvmField val gameUrl: String?,
    @JvmField val message: TdApi.Message?,
    @JvmField val ownerController: MessagesController?
  )

  /*protected override fun useDarkMode (): Boolean {
    return true
  }

  protected override fun getHeaderColor (): Int {
    return 0xff000000.toInt()
  }*/

  protected override fun getMenuId (): Int = R.id.menu_game

  override fun fillMenuItems (id: Int, header: HeaderView, menu: LinearLayout) {
    if (id == R.id.menu_game) {
      header.addForwardButton(menu, this)
      header.addMoreButton(menu, this)
    }
  }

  override fun onFocusStateChanged () {
    super.onFocusStateChanged()
    checkPlaying()
  }

  override fun onActivityPause () {
    super.onActivityPause()
    checkPlaying()
  }

  override fun onActivityResume () {
    super.onActivityResume()
    checkPlaying()
  }

  private fun checkPlaying () {
    getArgumentsStrict().ownerController?.setBroadcastAction(if (isFocused() && !isDestroyed() && context.getActivityState() == UI.State.RESUMED) TdApi.ChatActionStartPlayingGame.CONSTRUCTOR else TdApi.ChatActionCancel.CONSTRUCTOR)
  }

  override fun onMenuItemPressed (id: Int, view: View?) {
    if (id == R.id.menu_btn_more) {
      showMore(intArrayOf(R.id.btn_openLink), arrayOf(Lang.getString(R.string.OpenInExternalApp)), 0)
    } else if (id == R.id.menu_btn_forward) {
      if (getArguments() != null) {
        val args = getArgumentsStrict()
        val c = ShareController(context, tdlib)
        c.setArguments(ShareController.Args(args.game, args.userId, args.message, false))
        c.show()
      }
    }
  }

  override fun onMoreItemPressed (id: Int) {
    if (id == R.id.btn_openLink) {
      val args = getArguments()
      if (args != null) {
        Intents.openUri(args.gameUrl)
      }
    }
  }

  @SuppressLint("AddJavascriptInterface")
  protected override fun onCreateWebView (headerCell: DoubleHeaderView, webView: WebView) {
    val args = getArguments()
    if (args != null) {
      headerCell.setTitle(args.game!!.title)
      headerCell.setSubtitle(args.username)
    }
    webView.addJavascriptInterface(WebViewProxy(this), "TelegramWebviewProxy")
    if (args != null && args.gameUrl != null) {
      webView.loadUrl(args.gameUrl)
    }
  }

  override fun canSlideBackFrom (navigationController: NavigationController, x: Float, y: Float): Boolean {
    return y < Size.getHeaderPortraitSize() || x <= Screen.dp(15f)
  }
}
