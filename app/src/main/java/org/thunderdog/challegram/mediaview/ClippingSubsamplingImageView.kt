package org.thunderdog.challegram.mediaview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF

import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView

import org.thunderdog.challegram.U
import org.thunderdog.challegram.support.ViewSupport
import org.thunderdog.challegram.tool.DrawAlgorithms
import org.thunderdog.challegram.tool.Paints
import org.thunderdog.challegram.tool.Views

open class ClippingSubsamplingImageView (context: Context) : SubsamplingScaleImageView(context) {
  private var imageWidth = 0f
  private var imageHeight = 0f
  private val clip = RectF()
  private var topLeftRadius = 0f
  private var topRightRadius = 0f
  private var bottomRightRadius = 0f
  private var bottomLeftRadius = 0f

  fun setClipping (
    imageWidth: Float, imageHeight: Float,
    clipLeft: Float, clipTop: Float, clipRight: Float, clipBottom: Float,
    topLeftRadius: Float, topRightRadius: Float,
    bottomRightRadius: Float, bottomLeftRadius: Float
  ) {
    if (this.imageWidth != imageWidth || this.imageHeight != imageHeight ||
      U.setRect(clip, clipLeft, clipTop, clipRight, clipBottom) ||
      this.topLeftRadius != topLeftRadius || this.topRightRadius != topRightRadius ||
      this.bottomRightRadius != bottomRightRadius || this.bottomLeftRadius != bottomLeftRadius) {
      this.imageWidth = imageWidth
      this.imageHeight = imageHeight
      this.topLeftRadius = topLeftRadius
      this.topRightRadius = topRightRadius
      this.bottomRightRadius = bottomRightRadius
      this.bottomLeftRadius = bottomLeftRadius
      invalidate()
    }
  }

  fun resetClipping () {
    setClipping(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
  }

  private val path = Path()

  override fun onDraw (canvas: Canvas) {
    if (imageWidth == 0f || imageHeight == 0f || (clip.left == 0f && clip.top == 0f && clip.right == 0f && clip.bottom == 0f && topLeftRadius == 0f && topRightRadius == 0f && bottomRightRadius == 0f && bottomLeftRadius == 0f)) {
      super.onDraw(canvas)
      return
    }
    val paddingLeft = getPaddingLeft()
    val paddingTop = getPaddingTop()
    val paddingRight = getPaddingRight()
    val paddingBottom = getPaddingBottom()
    val centerX = paddingLeft + (getMeasuredWidth() - paddingLeft - paddingRight) / 2f
    val centerY = paddingTop + (getMeasuredHeight() - paddingTop - paddingBottom) / 2f
    val saveCount = Views.save(canvas)
    val rectF = Paints.getRectF()
    rectF.set(
      centerX - imageWidth / 2f + clip.left,
      centerY - imageHeight / 2f + clip.top,
      centerX + imageWidth / 2f - clip.right,
      centerY + imageHeight / 2f - clip.bottom
    )
    canvas.clipRect(rectF)
    if (topLeftRadius != 0f || topRightRadius != 0f || bottomRightRadius != 0f || bottomLeftRadius != 0f) {
      var imageWidth = getSWidth()
      var imageHeight = getSHeight()
      if (U.isRotated(getOrientation())) {
        val temp = imageHeight
        imageHeight = imageWidth
        imageWidth = temp
      }
      val scale = Math.min(
        getMeasuredWidth().toFloat() / imageWidth.toFloat(),
        getMeasuredHeight().toFloat() / imageHeight.toFloat()
      )
      imageWidth = (imageWidth * scale).toInt()
      imageHeight = (imageHeight * scale).toInt()
      if (imageWidth > 0 && imageHeight > 0) {
        rectF.left = Math.max(rectF.left, centerX - imageWidth / 2f)
        rectF.top = Math.max(rectF.top, centerY - imageHeight / 2f)
        rectF.right = Math.min(rectF.right, centerX + imageWidth / 2f)
        rectF.bottom = Math.min(rectF.bottom, centerY + imageHeight / 2f)
      }

      path.reset()
      DrawAlgorithms.buildPath(path, rectF, topLeftRadius, topRightRadius, bottomRightRadius, bottomLeftRadius)
      ViewSupport.clipPath(canvas, path)
    }
    super.onDraw(canvas)

    Views.restore(canvas, saveCount)
  }
}
