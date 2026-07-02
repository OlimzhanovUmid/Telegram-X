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
 * File created on 01/09/2015 at 06:27
 */
package org.thunderdog.challegram.component

import org.drinkless.tdlib.TdApi
import org.thunderdog.challegram.mediaview.MediaViewController
import org.thunderdog.challegram.mediaview.data.MediaStack

fun interface MediaCollectorDelegate {
  fun collectMedias (fromMessageId: Long, isSponsored: Boolean, filter: TdApi.SearchMessagesFilter?): MediaStack? {
    return null
  }
  fun modifyMediaArguments (cause: Any?, args: MediaViewController.Args)
}
