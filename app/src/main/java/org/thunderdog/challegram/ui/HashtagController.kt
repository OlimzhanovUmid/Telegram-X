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
 * File created on 27/07/2017
 */
package org.thunderdog.challegram.ui

import android.content.Context
import android.view.View
import me.vkryl.android.widget.FrameLayoutFix
import org.thunderdog.challegram.R
import org.thunderdog.challegram.component.dialogs.SearchManager
import org.thunderdog.challegram.navigation.BackHeaderButton
import org.thunderdog.challegram.navigation.TelegramViewController
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.theme.ColorId

open class HashtagController (context: Context, tdlib: Tdlib?) : TelegramViewController<String>(context, tdlib) {
  override fun getId (): Int {
    return R.id.controller_hashtag
  }

  override fun getName (): CharSequence {
    return getArgumentsStrict()
  }

  override fun getHeaderColorId (): Int {
    return ColorId.filling
  }

  override fun getHeaderIconColorId (): Int {
    return ColorId.headerLightIcon
  }

  override fun getHeaderTextColorId (): Int {
    return ColorId.text
  }

  override val chatSearchInitialQuery: String
    get() = getArgumentsStrict()

  override fun getBackButton (): Int {
    return BackHeaderButton.TYPE_BACK
  }

  override val chatSearchFlags: Int
    get() = SearchManager.FLAG_NEED_MESSAGES or SearchManager.FLAG_NO_CHATS

  override fun onCreateView (context: Context): View {
    val wrapView = FrameLayoutFix(context)
    wrapView.addView(generateChatSearchView(null))
    return wrapView
  }

  override fun getViewForApplyingOffsets (): View? {
    return getChatSearchView()
  }
}
