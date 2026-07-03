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
 * File created on 31/08/2015 at 13:54
 */
package org.thunderdog.challegram.component.base

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.view.View
import me.vkryl.android.DECELERATE_INTERPOLATOR
import me.vkryl.android.getFraction
import me.vkryl.android.simpleValueAnimator
import org.thunderdog.challegram.Log
import org.thunderdog.challegram.tool.Paints
import org.thunderdog.challegram.tool.UI

class SwitchDrawable (bottomResource: Int, topResource: Int) : Drawable() {
  companion object {
    private const val TRANSITION_DURATION = 180
  }

  private var factor = 0f
  private var isAnimating = false
  private var animator: ValueAnimator? = null

  private var boundView: View? = null
  private val bottomBitmap: Bitmap
  private val topBitmap: Bitmap

  init {
    val res = UI.getResources()

    bottomBitmap = BitmapFactory.decodeResource(res, bottomResource)
    topBitmap = BitmapFactory.decodeResource(res, topResource)

    if (bottomBitmap.width != topBitmap.width || bottomBitmap.height != topBitmap.height) {
      Log.w("SwitchDrawable: bitmap sizes are not equal: %dx%d vs %dx%d", bottomBitmap.width, bottomBitmap.height, topBitmap.width, topBitmap.height)
    }
  }

  fun setBoundView (boundView: View) {
    this.boundView = boundView
  }

  fun invalidate () {
    boundView?.invalidate()
  }

  fun forceSetFactor (factor: Float) {
    this.factor = factor
    invalidate()
  }

  fun animateFactor (toFactor: Float) {
    if (isAnimating && animator != null) {
      isAnimating = false
      animator?.cancel()
    }

    if (factor == toFactor) {
      isAnimating = false
      forceSetFactor(toFactor)
      return
    }

    isAnimating = true
    val startFactor = getFactor()
    val diffFactor = toFactor - startFactor
    val animator = simpleValueAnimator()
    this.animator = animator
    animator.addUpdateListener { animation: ValueAnimator? -> setFactor(startFactor + diffFactor * getFraction(animation!!)) }
    animator.interpolator = DECELERATE_INTERPOLATOR
    animator.setDuration(TRANSITION_DURATION.toLong())
    animator.addListener(object : AnimatorListenerAdapter() {
      override fun onAnimationEnd (animation: Animator) {
        isAnimating = false
      }
    })
    animator.start()
  }

  fun setFactor (factor: Float) {
    if (isAnimating && this.factor != factor) {
      this.factor = factor
      invalidate()
    }
  }

  fun getFactor (): Float {
    return factor
  }

  private var bitmapLeft = 0f
  private var bitmapTop = 0f

  override fun setBounds (left: Int, top: Int, right: Int, bottom: Int) {
    super.setBounds(left, top, right, bottom)
    bitmapLeft = left + (right - left) * .5f - bottomBitmap.width * .5f
    bitmapTop = top + (bottom - top) * .5f - bottomBitmap.height * .5f
  }

  override fun draw (canvas: Canvas) {
    val paint = Paints.getBitmapPaint()
    val restoreAlpha = paint.alpha
    if (factor != 1f) {
      paint.alpha = ((1f - factor) * 255f).toInt()
      canvas.drawBitmap(bottomBitmap, bitmapLeft, bitmapTop, paint)
      paint.alpha = restoreAlpha
    }

    if (factor != 0f) {
      paint.alpha = (factor * 255f).toInt()
      canvas.drawBitmap(topBitmap, bitmapLeft, bitmapTop, paint)
      paint.alpha = restoreAlpha
    }
  }

  override fun setAlpha (alpha: Int) {

  }

  override fun setColorFilter (colorFilter: ColorFilter?) {

  }

  @Suppress("deprecation")
  override fun getOpacity (): Int {
    return PixelFormat.UNKNOWN
  }
}
