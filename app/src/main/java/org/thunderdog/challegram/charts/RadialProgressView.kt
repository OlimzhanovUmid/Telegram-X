package org.thunderdog.challegram.charts

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Screen

class RadialProgressView (context: Context) : View(context) {

  private var lastUpdateTime: Long = 0
  private var radOffset = 0f
  private var currentCircleLength = 0f
  private var risingCircleLength = false
  private var currentProgressTime = 0f
  private val cicleRect = RectF()
  private var useSelfAlpha = false

  private var progressColor: Int

  private val decelerateInterpolator: DecelerateInterpolator
  private val accelerateInterpolator: AccelerateInterpolator
  private val progressPaint: Paint
  private var size: Int

  init {
    size = Screen.dp(40f)

    progressColor = Theme.progressColor()
    decelerateInterpolator = DecelerateInterpolator()
    accelerateInterpolator = AccelerateInterpolator()
    progressPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    progressPaint.style = Paint.Style.STROKE
    progressPaint.strokeCap = Paint.Cap.ROUND
    progressPaint.strokeWidth = Screen.dp(3f).toFloat()
    progressPaint.color = progressColor
  }

  fun setUseSelfAlpha (value: Boolean) {
    useSelfAlpha = value
  }

  override fun setAlpha (alpha: Float) {
    super.setAlpha(alpha)
    if (useSelfAlpha) {
      val background = getBackground()
      val a = (alpha * 255).toInt()
      if (background != null) {
        background.setAlpha(a)
      }
      progressPaint.alpha = a
    }
  }

  private fun updateAnimation () {
    val newTime = System.currentTimeMillis()
    var dt = newTime - lastUpdateTime
    if (dt > 17) {
      dt = 17
    }
    lastUpdateTime = newTime

    radOffset += 360 * dt / rotationTime
    val count = (radOffset / 360).toInt()
    radOffset -= count * 360

    currentProgressTime += dt
    if (currentProgressTime >= risingTime) {
      currentProgressTime = risingTime
    }
    currentCircleLength = if (risingCircleLength) {
      4 + 266 * accelerateInterpolator.getInterpolation(currentProgressTime / risingTime)
    } else {
      4 - 270 * (1.0f - decelerateInterpolator.getInterpolation(currentProgressTime / risingTime))
    }
    if (currentProgressTime == risingTime) {
      if (risingCircleLength) {
        radOffset += 270
        currentCircleLength = -266f
      }
      risingCircleLength = !risingCircleLength
      currentProgressTime = 0f
    }
    invalidate()
  }

  fun setSize (value: Int) {
    size = value
    invalidate()
  }

  fun setStrokeWidth (value: Float) {
    progressPaint.strokeWidth = Screen.dp(value).toFloat()
  }

  fun setProgressColor (color: Int) {
    progressColor = color
    progressPaint.color = progressColor
  }

  override fun onDraw (canvas: Canvas) {
    val x = (measuredWidth - size) / 2
    val y = (measuredHeight - size) / 2
    cicleRect.set(x.toFloat(), y.toFloat(), (x + size).toFloat(), (y + size).toFloat())
    canvas.drawArc(cicleRect, radOffset, currentCircleLength, false, progressPaint)
    updateAnimation()
  }

  companion object {
    private const val rotationTime = 2000f
    private const val risingTime = 500f
  }
}
