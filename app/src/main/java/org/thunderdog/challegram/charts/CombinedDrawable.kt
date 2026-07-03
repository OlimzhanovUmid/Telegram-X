package org.thunderdog.challegram.charts

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.drawable.Drawable
import android.graphics.drawable.Drawable.ConstantState

import androidx.annotation.NonNull

class CombinedDrawable : Drawable, Drawable.Callback {

  private val background: Drawable
  private val icon: Drawable?
  private val left: Int
  private val top: Int
  private var iconWidth = 0
  private var iconHeight = 0
  private var backWidth = 0
  private var backHeight = 0
  private var offsetX = 0
  private var offsetY = 0
  private var fullSize = false

  constructor (backgroundDrawable: Drawable, iconDrawable: Drawable?, leftOffset: Int, topOffset: Int) : super() {
    background = backgroundDrawable
    icon = iconDrawable
    left = leftOffset
    top = topOffset
    if (iconDrawable != null) {
      iconDrawable.setCallback(this)
    }
  }

  constructor (backgroundDrawable: Drawable, iconDrawable: Drawable?) : super() {
    background = backgroundDrawable
    icon = iconDrawable
    left = 0
    top = 0
    if (iconDrawable != null) {
      iconDrawable.setCallback(this)
    }
  }

  fun setIconSize (width: Int, height: Int) {
    iconWidth = width
    iconHeight = height
  }

  fun setCustomSize (width: Int, height: Int) {
    backWidth = width
    backHeight = height
  }

  fun setIconOffset (x: Int, y: Int) {
    offsetX = x
    offsetY = y
  }

  fun getIcon (): Drawable? {
    return icon
  }

  fun getBackground (): Drawable {
    return background
  }

  fun setFullsize (value: Boolean) {
    fullSize = value
  }

  override fun setColorFilter (colorFilter: ColorFilter?) {
    icon!!.setColorFilter(colorFilter)
  }

  override fun isStateful (): Boolean {
    return icon!!.isStateful()
  }

  override fun setState (stateSet: IntArray): Boolean {
    icon!!.setState(stateSet)
    return true
  }

  override fun getState (): IntArray {
    return icon!!.getState()
  }

  protected override fun onStateChange (state: IntArray): Boolean {
    return true
  }

  override fun jumpToCurrentState () {
    icon!!.jumpToCurrentState()
  }

  override fun getConstantState (): ConstantState? {
    return icon!!.constantState
  }

  override fun draw (canvas: Canvas) {
    background.setBounds(getBounds())
    background.draw(canvas)
    if (icon != null) {
      if (fullSize) {
        val bounds = getBounds()
        if (left != 0) {
          icon.setBounds(bounds.left + left, bounds.top + top, bounds.right - left, bounds.bottom - top)
        } else {
          icon.setBounds(bounds)
        }
      } else {
        val x: Int
        val y: Int
        if (iconWidth != 0) {
          x = getBounds().centerX() - iconWidth / 2 + left + offsetX
          y = getBounds().centerY() - iconHeight / 2 + top + offsetY
          icon.setBounds(x, y, x + iconWidth, y + iconHeight)
        } else {
          x = getBounds().centerX() - icon.intrinsicWidth / 2 + left
          y = getBounds().centerY() - icon.intrinsicHeight / 2 + top
          icon.setBounds(x, y, x + icon.intrinsicWidth, y + icon.intrinsicHeight)
        }
      }
      icon.draw(canvas)
    }
  }

  override fun setAlpha (alpha: Int) {
    icon!!.setAlpha(alpha)
    background.setAlpha(alpha)
  }

  override fun getIntrinsicWidth (): Int {
    return if (backWidth != 0) backWidth else background.intrinsicWidth
  }

  override fun getIntrinsicHeight (): Int {
    return if (backHeight != 0) backHeight else background.intrinsicHeight
  }

  override fun getMinimumWidth (): Int {
    return if (backWidth != 0) backWidth else background.minimumWidth
  }

  override fun getMinimumHeight (): Int {
    return if (backHeight != 0) backHeight else background.minimumHeight
  }

  @Suppress("deprecation")
  override fun getOpacity (): Int {
    return icon!!.getOpacity()
  }

  override fun invalidateDrawable (@NonNull who: Drawable) {
    invalidateSelf()
  }

  override fun scheduleDrawable (@NonNull who: Drawable, @NonNull what: Runnable, `when`: Long) {
    scheduleSelf(what, `when`)
  }

  override fun unscheduleDrawable (@NonNull who: Drawable, @NonNull what: Runnable) {
    unscheduleSelf(what)
  }
}
