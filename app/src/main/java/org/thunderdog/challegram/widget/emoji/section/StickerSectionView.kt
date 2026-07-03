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
 * File created on 19/08/2023
 */
package org.thunderdog.challegram.widget.emoji.section

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import android.view.View

import org.thunderdog.challegram.data.TGStickerSetInfo
import org.thunderdog.challegram.loader.ImageReceiver
import org.thunderdog.challegram.loader.gif.GifReceiver
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.theme.PorterDuffColorId
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.DrawAlgorithms
import org.thunderdog.challegram.tool.Paints
import org.thunderdog.challegram.tool.Screen

import me.vkryl.android.animator.FactorAnimator
import me.vkryl.core.lambda.Destroyable
import me.vkryl.android.DECELERATE_INTERPOLATOR
import me.vkryl.core.color
import me.vkryl.core.fromToArgb

class StickerSectionView (context: Context) : View(context), Destroyable, FactorAnimator.Target {
  companion object {
    private const val WIDTH = 44
    private const val PADDING = 10
  }

  private val receiver: ImageReceiver = ImageReceiver(this, 0)
  private val gifReceiver: GifReceiver = GifReceiver(this)

  private var selectionFactor = 0f

  fun attach () {
    receiver.attach()
    gifReceiver.attach()
  }

  fun detach () {
    receiver.detach()
    gifReceiver.detach()
  }

  override fun performDestroy () {
    receiver.destroy()
    gifReceiver.destroy()
  }

  private var info: TGStickerSetInfo? = null
  private var contour: Path? = null

  fun setStickerSet (info: TGStickerSetInfo) {
    this.info = info
    this.contour = info.getPreviewContour(Math.min(receiver.getWidth(), receiver.getHeight()))
    receiver.requestFile(info.previewImage)
    gifReceiver.requestFile(info.previewAnimation)
  }

  private var animator: FactorAnimator? = null

  fun setSelectionFactor (factor: Float, animated: Boolean) {
    if (animated && this.selectionFactor != factor) {
      if (animator == null) {
        animator = FactorAnimator(0, this, DECELERATE_INTERPOLATOR, 180L, selectionFactor)
      }
      animator?.animateTo(factor)
    } else {
      animator?.forceFactor(factor)
      setSelectionFactor(factor)
    }
  }

  override fun onFactorChanged (id: Int, factor: Float, fraction: Float, callee: FactorAnimator?) {
    if (id == 0) {
      setSelectionFactor(factor)
    }
  }

  override fun onFactorChangeFinished (id: Int, finalFactor: Float, callee: FactorAnimator?) { }

  private fun setSelectionFactor (factor: Float) {
    if (this.selectionFactor != factor) {
      this.selectionFactor = factor
      invalidate()
    }
  }

  fun getStickerSet (): TGStickerSetInfo? {
    return info
  }

  override fun onMeasure (widthMeasureSpec: Int, heightMeasureSpec: Int) {
    setMeasuredDimension(MeasureSpec.makeMeasureSpec(Screen.dp(WIDTH.toFloat()), MeasureSpec.EXACTLY), getDefaultSize(suggestedMinimumHeight, heightMeasureSpec))
    setBounds()
  }

  private fun setBounds () {
    val padding = Screen.dp(PADDING.toFloat())
    val width = receiver.getWidth()
    val height = receiver.getHeight()
    receiver.setBounds(padding, padding, measuredWidth - padding, measuredHeight - padding)
    gifReceiver.setBounds(padding, padding, measuredWidth - padding, measuredHeight - padding)
    val info = this.info
    if (info != null && (width != receiver.getWidth() || height != receiver.getHeight())) {
      this.contour = info.getPreviewContour(Math.min(receiver.getWidth(), receiver.getHeight()))
    }
  }

  override fun onDraw (c: Canvas) {
    val cx = measuredWidth / 2
    val cy = measuredHeight / 2

    val saved = selectionFactor != 0f
    if (saved) {
      val selectionColor = Theme.chatSelectionColor()
      val selectionAlpha = Color.alpha(selectionColor)
      val color = color((selectionAlpha.toFloat() * selectionFactor).toInt(), selectionColor)
      val radius = Screen.dp(18f) - (Screen.dp(4f).toFloat() * (1f - selectionFactor)).toInt()

      c.drawCircle(cx.toFloat(), cy.toFloat(), radius.toFloat(), Paints.fillingPaint(color))
      c.save()
      val scale = .85f + .15f * (1f - selectionFactor)
      c.scale(scale, scale, cx.toFloat(), cy.toFloat())
    }

    if (info!!.needThemedColorFilter()) {
      if (selectionFactor == 0f || selectionFactor == 1f) {
        @PorterDuffColorId val colorId: Int = if (selectionFactor == 0f) ColorId.icon else ColorId.iconActive
        receiver.setThemedPorterDuffColorId(colorId)
        gifReceiver.setThemedPorterDuffColorId(colorId)
      } else {
        val color = fromToArgb(Theme.getColor(ColorId.icon), Theme.getColor(ColorId.iconActive), selectionFactor)
        receiver.setPorterDuffColorFilter(color)
        gifReceiver.setPorterDuffColorFilter(color)
      }
    } else {
      receiver.disablePorterDuffColorFilter()
      gifReceiver.disablePorterDuffColorFilter()
    }
    DrawAlgorithms.drawSticker(c, info, gifReceiver, receiver, contour)

    if (saved) {
      c.restore()
    }
  }
}
