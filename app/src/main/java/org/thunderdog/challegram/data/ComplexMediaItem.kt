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
 * File created on 06/09/2022, 00:24.
 */

package org.thunderdog.challegram.data

import android.graphics.Canvas
import android.graphics.Rect
import org.thunderdog.challegram.loader.ComplexReceiver

interface ComplexMediaItem {
  fun requestComplexMedia (receiver: ComplexReceiver, displayMediaKey: Long)
  fun getComplexMediaKey (): String
  fun requiresTopLayer (): Boolean
  fun draw (c: Canvas, rect: Rect, mediaReceiver: ComplexReceiver, displayMediaKey: Long, translate: Boolean)
}
