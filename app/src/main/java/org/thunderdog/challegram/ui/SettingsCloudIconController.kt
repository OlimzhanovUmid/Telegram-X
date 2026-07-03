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
 * File created on 20/10/2019
 */
package org.thunderdog.challegram.ui

import android.content.Context
import org.thunderdog.challegram.R
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.unsorted.Settings
import me.vkryl.core.lambda.RunnableData

class SettingsCloudIconController (context: Context, tdlib: Tdlib?) : SettingsCloudController<Settings.IconPack>(context, tdlib, 0L, 0, R.string.IconsCurrent, R.string.IconsBuiltIn, R.string.IconsLoaded, R.string.IconsUpdate, R.string.IconsInstalling) {
  override fun getId (): Int {
    return R.id.controller_iconSets
  }

  override fun getName (): CharSequence {
    return Lang.getString(R.string.Icons)
  }

  protected override fun getCurrentSetting (): Settings.IconPack {
    return Settings.instance().iconPack!!
  }

  protected override fun getSettings (callback: RunnableData<List<Settings.IconPack>>) {
    @Suppress("UNCHECKED_CAST")
    tdlib!!.getIconPacks(callback as RunnableData<MutableList<Settings.IconPack?>?>)
  }

  protected override fun applySetting (setting: Settings.IconPack) {
    // TODO Drawables.instance().changeIconPack(setting)
    if (getThemeController() != null) {
      getThemeController().updateSelectedIconPack()
    }
  }
}
