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
 * File created on 21/02/2018
 */
package org.thunderdog.challegram.telegram

import org.drinkless.tdlib.TdApi

interface GlobalAccountListener {
  fun onAccountProfileChanged (account: TdlibAccount, profile: TdApi.User, isCurrent: Boolean, isLoaded: Boolean) { }
  fun onAccountProfilePhotoChanged (account: TdlibAccount, big: Boolean, isCurrent: Boolean) { }
  fun onAccountProfileEmojiStatusChanged (account: TdlibAccount, isCurrent: Boolean) { }
  fun onAccountSwitched (newAccount: TdlibAccount, profile: TdApi.User?, reason: Int, oldAccount: TdlibAccount?) { }
  fun onAuthorizationStateChanged (account: TdlibAccount, authorizationState: TdApi.AuthorizationState, status: Int) { }
  fun onTdlibOptimizing (tdlib: Tdlib, isOptimizing: Boolean) { }
  fun onActiveAccountRemoved (account: TdlibAccount, position: Int) { }
  fun onActiveAccountAdded (account: TdlibAccount, position: Int) { }
  fun onActiveAccountMoved (account: TdlibAccount, fromPosition: Int, toPosition: Int) { }
}
