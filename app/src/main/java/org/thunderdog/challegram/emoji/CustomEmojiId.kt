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
 * File created on 05/09/2022, 17:31.
 */

package org.thunderdog.challegram.emoji

import android.graphics.Paint
import android.os.Parcel
import android.os.Parcelable
import android.text.TextPaint
import android.text.style.CharacterStyle

class CustomEmojiId : CharacterStyle, EmojiSpan, Parcelable {
  @JvmField
  val customEmojiId: Long
  @JvmField
  val disableAnimations: Boolean

  constructor (customEmojiId: Long, disableAnimations: Boolean) {
    this.customEmojiId = customEmojiId
    this.disableAnimations = disableAnimations
  }

  constructor (source: Parcel) : this(source.readLong(), source.readByte().toInt() == 1)

  // EmojiSpan

  override fun getCustomEmojiId (): Long {
    return customEmojiId
  }

  override fun forceDisableAnimations (): Boolean {
    return disableAnimations
  }

  override fun isCustomEmoji (): Boolean {
    return true
  }

  override fun toBuiltInEmojiSpan (): EmojiSpan {
    throw UnsupportedOperationException()
  }

  private val size = EmojiSize()

  override fun getRawSize (paint: Paint): Int {
    size.initialize(paint, null, true)
    return size.getSize()
  }

  // CharacterStyle

  override fun updateDrawState (tp: TextPaint) {
    // Do nothing.
  }

  // Parcelable

  override fun writeToParcel (dest: Parcel, flags: Int) {
    dest.writeLong(customEmojiId)
  }

  override fun describeContents (): Int {
    return 0
  }

  companion object CREATOR : Parcelable.Creator<CustomEmojiId> {
    override fun createFromParcel (source: Parcel): CustomEmojiId {
      return CustomEmojiId(source)
    }

    override fun newArray (size: Int): Array<CustomEmojiId?> {
      return arrayOfNulls(0)
    }
  }
}
