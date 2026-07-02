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
 * File created on 09/12/2016
 */
package org.thunderdog.challegram.mediaview

import org.thunderdog.challegram.mediaview.data.MediaItem

interface MediaViewDelegate {
  fun getTargetLocation (indexInStack: Int, item: MediaItem): MediaViewThumbLocation? // null if item is not presented on screen
  fun setMediaItemVisible (index: Int, item: MediaItem?, isVisible: Boolean) // called when opening and closing photo viewer
}
