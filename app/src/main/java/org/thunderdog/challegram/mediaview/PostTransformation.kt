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
 * File created on 10/05/2017
 */
package org.thunderdog.challegram.mediaview

import me.vkryl.core.modulo

class PostTransformation {
  private var rotationAroundCenter = 0f

  fun isEmpty (): Boolean {
    return rotationAroundCenter == 0f
  }

  fun rotateAroundCenter (): Float {
    rotationAroundCenter = modulo(rotationAroundCenter - 90f, 360f)
    return rotationAroundCenter
  }

  fun getRotationAroundCenter (): Float {
    return rotationAroundCenter
  }
}
