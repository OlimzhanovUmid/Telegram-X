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
 * File created on 18/08/2022, 18:03.
 */

package org.thunderdog.challegram.telegram

import androidx.annotation.Nullable

fun interface GlobalTokenStateListener {
  fun onTokenStateChanged (newState: Int, @Nullable error: String?, @Nullable fullError: Throwable?)
}
