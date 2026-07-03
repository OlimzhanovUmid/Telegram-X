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
import android.view.View
import org.drinkless.tdlib.TdApi
import org.thunderdog.challegram.R
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.tool.UI
import org.thunderdog.challegram.v.CustomRecyclerView

class WaitForPremiumController(context: Context, tdlib: Tdlib) : RecyclerViewController<TdApi.AuthorizationStateWaitPremiumPurchase>(context, tdlib), View.OnClickListener {
  override fun getId (): Int {
    return R.id.controller_waitForPremium
  }

  override fun getName (): CharSequence {
    return Lang.getString(R.string.login_PremiumRequiredTitle)
  }

  private var oneShot = false

  override fun onFocus () {
    super.onFocus()
    if (!oneShot) {
      oneShot = true
      destroyStackItemById(R.id.controller_code)
      destroyStackItemById(R.id.controller_name)
      destroyStackItemById(R.id.controller_password)
    }
  }

  override fun onCreateView (context: Context, recyclerView: CustomRecyclerView) {
    val adapter = SettingsAdapter(this)
    adapter.setItems(arrayOf(
      ListItem(ListItem.TYPE_ICONIZED_EMPTY, 0, R.drawable.baseline_premium_star_96, Lang.getMarkdownString(this, R.string.login_PremiumRequired)),
      ListItem(ListItem.TYPE_SHADOW_TOP),
      ListItem(ListItem.TYPE_BUTTON, R.id.btn_buyPremium, 0, R.string.login_PremiumRequiredBtn),
      ListItem(ListItem.TYPE_SHADOW_BOTTOM)
    ), false)
    recyclerView.adapter = adapter
  }

  override fun onClick (v: View) {
    if (v.id == R.id.btn_buyPremium) {
      UI.openUrl("https://telegram.org/")
    }
  }
}
