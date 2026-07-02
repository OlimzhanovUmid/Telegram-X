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
 * File created on 14/12/2017
 */
package org.thunderdog.challegram.data

import org.thunderdog.challegram.loader.ImageFile
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.telegram.TdlibAccentColor
import org.thunderdog.challegram.tool.Paints
import org.thunderdog.challegram.util.text.Letters

class AvatarInfo(
  @JvmField val tdlib: Tdlib,
  @JvmField val userId: Long
) {
  @JvmField var imageFile: ImageFile? = null

  @JvmField var accentColor: TdlibAccentColor? = null
  @JvmField var letters: Letters? = null

  @JvmField var lettersWidth15dp: Float = 0f

  init {
    updateUser()
  }

  fun updateUser() {
    val user = tdlib.cache().user(userId)
    letters = TD.getLetters(user)
    accentColor = tdlib.cache().userAccentColor(user)
    imageFile = TD.getAvatar(tdlib, user)
    lettersWidth15dp = Paints.measureLetters(letters, 15f).toFloat()
  }
}
