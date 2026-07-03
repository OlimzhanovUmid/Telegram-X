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
 * File created on 27/02/2024
 */
package org.thunderdog.challegram.mediaview.crop

import android.content.Context
import android.view.ViewGroup
import org.thunderdog.challegram.Log
import org.thunderdog.challegram.U

class CroppedLayout (context: Context) : ViewGroup(context) {
  private var sourceWidth = 0
  private var sourceHeight = 0
  private var unappliedRotationDegrees = 0
  private var cropState: CropState? = null

  init {
    clipChildren = true
  }

  fun setSourceDimensions (width: Int, height: Int, unappliedRotationDegrees: Int) {
    if (this.sourceWidth != width || this.sourceHeight != height || this.unappliedRotationDegrees != unappliedRotationDegrees) {
      this.sourceWidth = width
      this.sourceHeight = height
      this.unappliedRotationDegrees = unappliedRotationDegrees
      requestLayout()
      invalidate()
    }
  }

  fun setCropState (cropState: CropState?) {
    var cropState = cropState
    if (cropState != null && cropState.isEmpty) {
      cropState = null
    }
    if ((this.cropState == null && cropState != null) || (this.cropState != null && this.cropState != cropState)) {
      this.cropState = if (cropState != null) CropState(cropState) else null
      requestLayout()
      invalidate()
    }
  }

  private var renderWidth = 0
  private var renderHeight = 0

  fun onPreMeasure (
    canvasWidth: Int, canvasHeight: Int,
    viewportWidth: Int, viewportHeight: Int,
    renderWidth: Int, renderHeight: Int
  ) {
    this.renderWidth = renderWidth
    this.renderHeight = renderHeight
  }

  override fun onMeasure (widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    val parentWidth = measuredWidth
    val parentHeight = measuredHeight
    val viewCount = childCount
    for (i in 0 until viewCount) {
      val view = getChildAt(i)
      val layoutParams = view.layoutParams
      if (layoutParams != null && (layoutParams.width == LayoutParams.WRAP_CONTENT && layoutParams.height == LayoutParams.WRAP_CONTENT)) {
        Log.v("renderView: %dx%d, parent: %dx%d", renderWidth, renderHeight, parentWidth, parentHeight)
        view.measure(
          MeasureSpec.makeMeasureSpec(renderWidth, MeasureSpec.EXACTLY),
          MeasureSpec.makeMeasureSpec(renderHeight, MeasureSpec.EXACTLY)
        )
      } else {
        measureChild(view, widthMeasureSpec, heightMeasureSpec)
      }
    }
  }

  override fun onLayout (changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    val childCount = childCount
    val parentWidth = measuredWidth
    val parentHeight = measuredHeight
    for (i in 0 until childCount) {
      val view = getChildAt(i)
      val width = view.measuredWidth
      val height = view.measuredHeight
      val pivotX: Int
      val pivotY: Int
      val croppedWidth: Int
      val croppedHeight: Int
      val rotateBy = (if (cropState != null) cropState!!.rotateBy else 0) + unappliedRotationDegrees
      val isRotated = U.isRotated(rotateBy)
      val mirrorHorizontally = cropState != null && cropState!!.needMirrorHorizontally()
      val mirrorVertically = cropState != null && cropState!!.needMirrorVertically()
      if (cropState != null && !cropState!!.isRegionEmpty) {
        var left: Double
        var top: Double
        var right: Double
        var bottom: Double
        when (cropState!!.rotateBy) {
          0 -> {
            left = cropState!!.left
            top = cropState!!.top
            right = cropState!!.right
            bottom = cropState!!.bottom
          }
          90 -> {
            left = cropState!!.top
            top = 1.0 - cropState!!.right
            right = cropState!!.bottom
            bottom = 1.0 - cropState!!.left
          }
          180 -> {
            left = 1.0 - cropState!!.right
            top = 1.0 - cropState!!.bottom
            right = 1.0 - cropState!!.left
            bottom = 1.0 - cropState!!.top
          }
          270 -> {
            left = 1.0 - cropState!!.bottom
            top = cropState!!.left
            right = 1.0 - cropState!!.top
            bottom = cropState!!.right
          }
          else -> throw IllegalStateException()
        }

        if (mirrorHorizontally) {
          val temp = left
          left = 1.0 - right
          right = 1.0 - temp
        }
        if (mirrorVertically) {
          val temp = top
          top = 1.0 - bottom
          bottom = 1.0 - temp
        }

        pivotX = (width * (left + (right - left) / 2.0)).toInt()
        pivotY = (height * (top + (bottom - top) / 2.0)).toInt()

        croppedWidth = ((if (isRotated) height else width) * cropState!!.regionWidth).toInt()
        croppedHeight = ((if (isRotated) width else height) * cropState!!.regionHeight).toInt()
      } else {
        if (isRotated) {
          croppedWidth = height
          croppedHeight = width
        } else {
          croppedWidth = width
          croppedHeight = height
        }
        pivotX = width / 2
        pivotY = height / 2
      }
      val left = parentWidth / 2 - pivotX
      val top = parentHeight / 2 - pivotY
      view.layout(left, top, left + width, top + height)
      val scale = Math.max(
        parentWidth.toFloat() / croppedWidth.toFloat(),
        parentHeight.toFloat() / croppedHeight.toFloat()
      )
      view.pivotX = pivotX.toFloat()
      view.pivotY = pivotY.toFloat()
      view.scaleX = scale * (if (mirrorHorizontally) -1.0f else 1.0f)
      view.scaleY = scale * (if (mirrorVertically) -1.0f else 1.0f)
      view.rotation = rotateBy.toFloat()
    }
  }
}
