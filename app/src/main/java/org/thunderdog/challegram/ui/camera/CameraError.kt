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
 * File created on 21/09/2017
 */
package org.thunderdog.challegram.ui.camera

import androidx.annotation.IntDef

class CameraError {
  @Retention(AnnotationRetention.SOURCE)
  @IntDef(NOT_ENOUGH_SPACE)
  annotation class Code

  companion object {
    /**
     * Not enough storage space. Offer user to free some space.
     */
    const val NOT_ENOUGH_SPACE = -1
  }
}
