package org.thunderdog.challegram.util

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.drawable.Drawable

import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.theme.PorterDuffColorId
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Drawables
import org.thunderdog.challegram.tool.Paints
import org.thunderdog.challegram.tool.UI
import org.thunderdog.challegram.tool.getPorterDuffPaint

import me.vkryl.android.DECELERATE_INTERPOLATOR
import me.vkryl.android.LINEAR_INTERPOLATOR
import me.vkryl.android.animator.BoolAnimator
import me.vkryl.android.animator.FactorAnimator
import me.vkryl.core.clamp
import me.vkryl.core.fromTo
import me.vkryl.core.fromToArgb

class TranslationCounterDrawable (private val drawable: Drawable) : Drawable(), FactorAnimator.Target {
  companion object {
    const val TRANSLATE_STATUS_DEFAULT = 0
    const val TRANSLATE_STATUS_LOADING = 1
    const val TRANSLATE_STATUS_SUCCESS = 2
    const val TRANSLATE_STATUS_ERROR = 3

    private const val ANIMATOR_OFFSET = 4
  }

  private val isSuccess = BoolAnimator(TRANSLATE_STATUS_SUCCESS, this, DECELERATE_INTERPOLATOR, 180L)
  private val isLoading = BoolAnimator(TRANSLATE_STATUS_LOADING, this, DECELERATE_INTERPOLATOR, 180L)
  private val isError = BoolAnimator(TRANSLATE_STATUS_ERROR, this, DECELERATE_INTERPOLATOR, 180L)
  private val offsetAnimator = BoolAnimator(ANIMATOR_OFFSET, this, LINEAR_INTERPOLATOR, 750L)

  private val drawableBg: Drawable = drawable.constantState!!.newDrawable().mutate()
  private val width: Int = drawable.minimumWidth
  private val height: Int = drawable.minimumHeight
  private var invalidateCallback: Runnable? = null

  private var defaultColorId: Int = ColorId.icon
  private @PorterDuffColorId var backgroundColorId: Int = ColorId.bubbleIn_time
  private var loadingColorId: Int = ColorId.bubbleIn_textLink

  private val checkLimiter = RateLimiter(this::checkStatus, 100L, null)

  init {
    UI.post(this::checkStatus)
  }

  fun setColors (defaultColorId: Int, @PorterDuffColorId backgroundColorId: Int, loadingColorId: Int) {
    this.defaultColorId = defaultColorId
    this.backgroundColorId = backgroundColorId
    this.loadingColorId = loadingColorId
  }

  fun setInvalidateCallback (invalidateCallback: Runnable?) {
    this.invalidateCallback = invalidateCallback
  }

  fun setStatus (status: Int, animated: Boolean) {
    setStatus(status == TRANSLATE_STATUS_SUCCESS, status == TRANSLATE_STATUS_LOADING, status == TRANSLATE_STATUS_ERROR, animated)
  }

  private fun setStatus (isSuccess: Boolean, isLoading: Boolean, isError: Boolean, animated: Boolean) {
    this.isSuccess.setValue(isSuccess, animated)
    this.isLoading.setValue(isLoading, animated)
    this.isError.setValue(isError, animated)
    if (animated) {
      checkStatus()
    }
  }

  private fun checkStatus () {
    if (isLoading.getValue() && !isSuccess.getValue() && !isError.getValue() && !offsetAnimator.isAnimating()) {
      offsetAnimator.setValue(!offsetAnimator.getValue(), true)
    }
  }

  fun getLoadingTextAlpha (): Float {
    val alpha = 1f - isLoading.getFloatValue() * 0.5f + ((offsetAnimator.getFloatValue() - 0.5f) * 2f * 0.1f) * isLoading.getFloatValue()
    return clamp(alpha)
  }

  override fun draw (canvas: Canvas) {
    val loadedProgress = 1f - isLoading.getFloatValue()
    val errorProgress = isError.getFloatValue()
    val successProgress = isSuccess.getFloatValue()
    val iconColor2 = fromToArgb(Theme.getColor(defaultColorId), Theme.getColor(ColorId.iconActive), successProgress)
    val iconColor1 = fromToArgb(Theme.getColor(loadingColorId), iconColor2, loadedProgress)
    val iconColor = fromToArgb(iconColor1, Theme.getColor(ColorId.iconNegative), errorProgress)

    if (loadedProgress == 1f) {
      Drawables.draw(canvas, drawable, 0f, 0f, Paints.getPorterDuffPaint(iconColor))
    } else {
      Drawables.draw(canvas, drawableBg, 0f, 0f, getPorterDuffPaint(backgroundColorId))

      val lineWidth = fromTo(0.571f, 1f, loadedProgress) * width
      val offset = fromTo(fromTo(-lineWidth - width * 0.5f, width * 1.5f, offsetAnimator.getFloatValue()), 0f, loadedProgress)

      canvas.save()
      canvas.rotate(45f, width / 2f, height / 2f)
      canvas.clipRect(offset, -height.toFloat(), offset + lineWidth, (height * 2).toFloat())
      canvas.rotate(-45f, width / 2f, height / 2f)
      Drawables.draw(canvas, drawable, 0f, 0f, Paints.getPorterDuffPaint(iconColor))
      canvas.restore()
    }
  }

  private fun invalidate () {
    invalidateCallback?.run()
  }

  override fun getMinimumWidth (): Int {
    return width
  }

  override fun getMinimumHeight (): Int {
    return height
  }

  override fun setAlpha (alpha: Int) {

  }

  override fun setColorFilter (colorFilter: ColorFilter?) {

  }

  @Suppress("deprecation")
  override fun getOpacity (): Int {
    return drawable.opacity
  }

  override fun onFactorChanged (id: Int, factor: Float, fraction: Float, callee: FactorAnimator?) {
    invalidate()
  }

  override fun onFactorChangeFinished (id: Int, finalFactor: Float, callee: FactorAnimator?) {
    if (id == ANIMATOR_OFFSET) {
      if (finalFactor == (if (offsetAnimator.getValue()) 1f else 0f)) {
        checkLimiter.run()
      }
    }
  }
}
