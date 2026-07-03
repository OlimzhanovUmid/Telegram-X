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
 * File created on 08/03/2023
 */
package org.thunderdog.challegram.mediaview

import androidx.annotation.CallSuper
import java.util.concurrent.atomic.AtomicBoolean

abstract class MediaSpoilerSendDelegate : MediaSendDelegate {
  private val showCaptionAboveMedia = AtomicBoolean()
  private val hideMedia = AtomicBoolean()

  final override fun allowHideMedia (): Boolean {
    return true
  }

  final override fun isHideMediaEnabled (): Boolean {
    return hideMedia.get()
  }

  @CallSuper
  override fun onHideMediaStateChanged (hideMedia: Boolean) {
    this.hideMedia.set(hideMedia)
  }

  override fun allowShowCaptionAboveMedia (): Boolean {
    return true
  }

  override fun showCaptionAboveMedia (): Boolean {
    return showCaptionAboveMedia.get()
  }

  override fun onShowCaptionAboveMediaStateChanged (showCaptionAboveMedia: Boolean) {
    this.showCaptionAboveMedia.set(showCaptionAboveMedia)
  }
}
