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
 * File created on 26/10/2016
 */
package org.thunderdog.challegram.component.attach

import org.drinkless.tdlib.TdApi
import org.thunderdog.challegram.loader.ImageFile
import org.thunderdog.challegram.telegram.TdlibProvider

class MediaImageFile (
  tdlib: TdlibProvider,
  file: TdApi.File,
  private val queryId: Long,
  private val resultId: String
) : ImageFile(tdlib, file) {
  fun getQueryId (): Long = queryId

  fun getResultId (): String = resultId
}
