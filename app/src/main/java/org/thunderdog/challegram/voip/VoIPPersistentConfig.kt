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
 * File created on 19/03/2023
 */
package org.thunderdog.challegram.voip

import org.thunderdog.challegram.telegram.TdlibManager
import java.io.File

object VoIPPersistentConfig {
  @JvmStatic
  fun getVoipConfigFile (): File {
    return File(TdlibManager.getTgvoipDirectory(), "voip_persistent_state.json")
  }

  @JvmStatic
  fun getVoipConfigFileSize (): Long {
    val file = getVoipConfigFile()
    return if (file.exists()) file.length() else 0
  }
}
