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
 * File created on 05/06/2023
 */
package org.thunderdog.challegram.component.emoji

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.ColorInt
import org.thunderdog.challegram.R
import org.thunderdog.challegram.component.sticker.TGStickerObj
import org.thunderdog.challegram.config.Config
import org.thunderdog.challegram.loader.ImageReceiver
import org.thunderdog.challegram.loader.gif.GifReceiver
import org.thunderdog.challegram.theme.PorterDuffColorId
import org.thunderdog.challegram.tool.Drawables
import me.vkryl.core.lambda.Destroyable

class AnimatedEmojiDrawable (parentView: View) : Drawable(), Destroyable {

  private val imageReceiver = ImageReceiver(parentView, 0)
  private val gifReceiver = GifReceiver(parentView)
  private var drawable: Drawable? = null

  fun setSticker (sticker: TGStickerObj, isPlayOnce: Boolean) {
    if (sticker.isDefaultPremiumStar) {
      drawable = Drawables.get(R.drawable.baseline_premium_star_28).mutate()
      return
    }
    val imageFile = sticker.image
    val animation = sticker.previewAnimation
    if (animation != null) {
      if (isPlayOnce) {
        animation.setPlayOnce(true)
        animation.setLooped(false)
      }
      gifReceiver.requestFile(animation)
    }
    imageReceiver.requestFile(imageFile)
  }

  fun attach () {
    imageReceiver.attach()
    gifReceiver.attach()
  }

  fun detach () {
    imageReceiver.detach()
    gifReceiver.detach()
  }

  override fun draw (canvas: Canvas) {
    val drawable = this.drawable
    if (drawable != null) {
      drawable.draw(canvas)
      return
    }
    if (gifReceiver.needPlaceholder() || Config.DEBUG_REACTIONS_ANIMATIONS) {
      imageReceiver.draw(canvas)
    }
    gifReceiver.draw(canvas)
  }

  override fun setBounds (bounds: Rect) {
    gifReceiver.setBounds(bounds.left, bounds.top, bounds.right, bounds.bottom)
    imageReceiver.setBounds(bounds.left, bounds.top, bounds.right, bounds.bottom)
    drawable?.setBounds(bounds.left, bounds.top, bounds.right, bounds.bottom)
    super.setBounds(bounds)
  }

  override fun setBounds (left: Int, top: Int, right: Int, bottom: Int) {
    gifReceiver.setBounds(left, top, right, bottom)
    imageReceiver.setBounds(left, top, right, bottom)
    drawable?.setBounds(left, top, right, bottom)
    super.setBounds(left, top, right, bottom)
  }

  override fun setAlpha (i: Int) {
    gifReceiver.alpha = i / 255f
    imageReceiver.alpha = i / 255f
    drawable?.alpha = i
  }

  fun setThemedPorterDuffColorId (@PorterDuffColorId colorId: Int) {
    gifReceiver.setThemedPorterDuffColorId(colorId)
    imageReceiver.setThemedPorterDuffColorId(colorId)
    drawable?.colorFilter = imageReceiver.bitmapPaint.colorFilter
  }

  fun setPorterDuffColorFilter (@ColorInt color: Int) {
    gifReceiver.setPorterDuffColorFilter(color)
    imageReceiver.setPorterDuffColorFilter(color)
    drawable?.colorFilter = imageReceiver.bitmapPaint.colorFilter
  }

  fun disablePorterDuffColorFilter () {
    gifReceiver.disablePorterDuffColorFilter()
    imageReceiver.disablePorterDuffColorFilter()
    drawable?.colorFilter = null
  }

  override fun setColorFilter (colorFilter: ColorFilter?) {

  }

  @Suppress("deprecation")
  override fun getOpacity (): Int {
    return PixelFormat.UNKNOWN
  }

  override fun performDestroy () {
    gifReceiver.destroy()
    imageReceiver.destroy()
  }
}
