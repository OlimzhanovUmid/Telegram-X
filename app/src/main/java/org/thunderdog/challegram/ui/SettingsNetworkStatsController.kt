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
import android.os.Bundle
import org.drinkless.tdlib.TdApi
import org.thunderdog.challegram.R
import org.thunderdog.challegram.component.base.SettingView
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.data.TGNetworkStats
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.tool.UI
import org.thunderdog.challegram.v.CustomRecyclerView
import java.util.ArrayList

class SettingsNetworkStatsController(context: Context, tdlib: Tdlib) : RecyclerViewController<SettingsNetworkStatsController.Args>(context, tdlib) {
  class Args(val type: Int, val stats: TGNetworkStats?)

  override fun needAsynchronousAnimation (): Boolean {
    return getArgumentsStrict().stats == null
  }

  override fun saveInstanceState (outState: Bundle, keyPrefix: String?): Boolean {
    super.saveInstanceState(outState, keyPrefix)
    outState.putInt(keyPrefix + "type", getArgumentsStrict().type)
    return true
  }

  override fun restoreInstanceState (`in`: Bundle, keyPrefix: String?): Boolean {
    super.restoreInstanceState(`in`, keyPrefix)
    setArguments(Args(`in`.getInt(keyPrefix + "type", TGNetworkStats.TYPE_MOBILE), null))
    return true
  }

  override fun getName (): CharSequence {
    return when (getArgumentsStrict().type) {
      TGNetworkStats.TYPE_ROAMING -> {
        Lang.getString(R.string.RoamingUsage)
      }
      TGNetworkStats.TYPE_WIFI -> {
        Lang.getString(R.string.WiFiUsage)
      }
      else -> {
        Lang.getString(R.string.MobileUsage)
      }
    }
  }

  override fun getId (): Int {
    return R.id.controller_networkStats
  }

  override fun onCreateView (context: Context, recyclerView: CustomRecyclerView) {
    val adapter = object : SettingsAdapter(this) {
      override fun setValuedSetting (item: ListItem, view: SettingView, isUpdate: Boolean) {
        view.setData(item.stringValue)
        view.setIgnoreEnabled(true)
        view.setEnabled(false)
      }
    }
    val items = ArrayList<ListItem>()
    val args = getArgumentsStrict()
    if (args.stats != null) {
      args.stats.buildEntries(items, args.type)
    } else {
      tdlib!!.client().send(TdApi.GetNetworkStatistics()) { result ->
        runOnUiThreadOptional(Runnable {
          when (result.constructor) {
            TdApi.NetworkStatistics.CONSTRUCTOR -> {
              val stats = TGNetworkStats(result as TdApi.NetworkStatistics)
              setArguments(Args(args.type, stats))
              stats.buildEntries(items, args.type)
              adapter.setItems(items, false)
              restorePersistentScrollPosition()
            }
            TdApi.Error.CONSTRUCTOR -> {
              UI.showError(result)
            }
          }
          executeScheduledAnimation()
        })
      }
    }
    adapter.setItems(items, false)
    recyclerView.setAdapter(adapter)
  }
}
