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
 * File created on 30/08/2015 at 18:12
 */
package org.thunderdog.challegram.data

import java.io.File

class CrashLog(private val crashId: Long, private var file: File?) {
  val id: Long
    get() = crashId

  fun deleteFile() {
    if (file?.delete() == true) {
      file = null
    }
  }

  fun getFile(): String {
    return file!!.path
  }
}
