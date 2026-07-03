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
import org.thunderdog.challegram.emoji.Emoji
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.unsorted.Settings
import me.vkryl.core.lambda.RunnableData

class SettingsCloudEmojiController (context: Context, tdlib: Tdlib?) : SettingsCloudController<Settings.EmojiPack>(context, tdlib, Settings.TUTORIAL_EMOJI_PACKS, R.string.EmojiInfo, R.string.EmojiCurrent, R.string.EmojiBuiltIn, R.string.EmojiLoaded, R.string.EmojiUpdate, R.string.EmojiInstalling) {
  override fun getId (): Int {
    return R.id.controller_emojiSets
  }

  override fun getName (): CharSequence {
    return Lang.getString(R.string.EmojiSets)
  }

  protected override fun getCurrentSetting (): Settings.EmojiPack {
    return Settings.instance().getEmojiPack()
  }

  protected override fun getSettings (callback: RunnableData<List<Settings.EmojiPack>>) {
    @Suppress("UNCHECKED_CAST")
    tdlib!!.getEmojiPacks(callback as RunnableData<MutableList<Settings.EmojiPack?>?>)
  }

  protected override fun applySetting (setting: Settings.EmojiPack) {
    Emoji.instance().changeEmojiPack(setting)
    if (getStickersAndEmojiController() != null) {
      getStickersAndEmojiController().updateSelectedEmoji()
    }
  }
}
