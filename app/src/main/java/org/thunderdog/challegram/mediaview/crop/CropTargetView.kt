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
 * File created on 18/10/2017
 */
package org.thunderdog.challegram.mediaview.crop

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import org.thunderdog.challegram.U
import org.thunderdog.challegram.mediaview.paint.PaintState
import org.thunderdog.challegram.tool.DrawAlgorithms
import me.vkryl.core.modulo

class CropTargetView (context: Context) : View(context) {
  private var bitmap: Bitmap? = null
  private var rotation = 0
  private var paintState: PaintState? = null

  fun resetState (bitmap: Bitmap?, rotation: Int, degrees: Float, paintState: PaintState?) {
    if (this.bitmap != bitmap || this.rotation != rotation || this.degrees != degrees || (this.paintState == null && paintState != null) || (this.paintState != null && paintState == null) || (this.paintState != null && !this.paintState!!.compare(paintState))) {
      val needLayout = bitmap != null && (this.bitmap == null || this.bitmap!!.isRecycled || U.getWidth(this.bitmap, this.rotation) != U.getWidth(bitmap, rotation) || U.getHeight(this.bitmap, this.rotation) != U.getHeight(bitmap, rotation))
      this.bitmap = bitmap
      this.rotation = rotation
      this.degrees = degrees
      this.paintState = paintState
      if (needLayout) {
        requestLayout()
      }
      checkDegreesRotationScale()
      invalidate()
    }
  }

  fun rotateTargetBy (degrees: Int) {
    resetState(bitmap, modulo(rotation + degrees, 360), this.degrees, this.paintState)
  }

  private var degrees = 0f

  fun setDegreesAroundCenter (degrees: Float) {
    if (this.degrees != degrees) {
      this.degrees = degrees
      checkDegreesRotationScale()
    }
  }

  private var rotationScale = 0f

  private fun checkDegreesRotationScale () {
    val w = measuredWidth.toFloat()
    val h = measuredHeight.toFloat()

    if (w == 0f || h == 0f) {
      return
    }

    val rad = Math.toRadians(degrees.toDouble())
    val sin = Math.abs(Math.sin(rad)).toFloat()
    val cos = Math.abs(Math.cos(rad)).toFloat()

    // W = w·|cos φ| + h·|sin φ|
    // H = w·|sin φ| + h·|cos φ|

    val W = w * cos + h * sin
    val H = w * sin + h * cos

    rotationScale = Math.max(W / w, H / h)

    updateStyles(false)
  }

  private var baseScaleX = 1f
  private var baseScaleY = 1f
  private var baseRotation = 0f

  fun setBaseScale (baseScaleX: Float, baseScaleY: Float) {
    if (this.baseScaleX != baseScaleX || this.baseScaleY != baseScaleY) {
      this.baseScaleX = baseScaleX
      this.baseScaleY = baseScaleY
      updateStyles(false)
    }
  }

  fun setBaseRotation (baseRotation: Float) {
    if (this.baseRotation != baseRotation) {
      this.baseRotation = baseRotation
      updateStyles(false)
    }
  }

  private var rotateInternally = false

  fun setRotateInternally (rotateInternally: Boolean) {
    if (this.rotateInternally != rotateInternally) {
      this.rotateInternally = rotateInternally
      updateStyles(degrees != 0f)
    }
  }

  private fun updateStyles (forceInvalidate: Boolean) {
    val rotationScale: Float
    val degrees: Float

    if (rotateInternally) {
      rotationScale = 1f
      degrees = 0f
    } else {
      rotationScale = this.rotationScale
      degrees = this.degrees
    }

    scaleX = baseScaleX * rotationScale
    scaleY = baseScaleY * rotationScale

    setRotation(baseRotation + degrees)

    if (forceInvalidate || rotateInternally) {
      invalidate()
    }
  }

  fun getTargetWidth (): Int {
    return U.getWidth(bitmap, rotation)
  }

  fun getTargetHeight (): Int {
    return U.getHeight(bitmap, rotation)
  }

  override fun onMeasure (widthMeasureSpec: Int, heightMeasureSpec: Int) {
    if (bitmap != null) {
      val availWidth = getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
      val availHeight = getDefaultSize(suggestedMinimumHeight, heightMeasureSpec)
      var w = getTargetWidth()
      var h = getTargetHeight()
      val scale = Math.min(availWidth.toFloat() / w.toFloat(), availHeight.toFloat() / h.toFloat())
      w = (w * scale).toInt()
      h = (h * scale).toInt()
      setMeasuredDimension(w, h)
      translationY = availHeight / 2f - h / 2f
      checkDegreesRotationScale()
    } else {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
  }

  private var mirrorHorizontallyFactor = 0f
  private var mirrorVerticallyFactor = 0f

  fun setMirrorFactors (mirrorHorizontallyFactor: Float, mirrorVerticallyFactor: Float) {
    this.mirrorHorizontallyFactor = mirrorHorizontallyFactor
    this.mirrorVerticallyFactor = mirrorVerticallyFactor
    invalidate()
  }

  override fun onDraw (c: Canvas) {
    val cx = measuredWidth / 2f
    val cy = measuredHeight / 2f

    val saved = rotateInternally && (degrees != 0f || rotationScale != 1f)
    if (saved) {
      c.save()
      c.rotate(degrees, cx, cy)
      c.scale(rotationScale, rotationScale, cx, cy)
    }

    DrawAlgorithms.drawScaledBitmap(measuredWidth, measuredHeight, c, bitmap, rotation, mirrorHorizontallyFactor, mirrorVerticallyFactor, paintState)

    if (saved) {
      c.restore()
    }
  }
}
