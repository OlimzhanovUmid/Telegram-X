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
 * File created on 10/12/2016
 */
package org.thunderdog.challegram.mediaview

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.thunderdog.challegram.navigation.ViewController
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Fonts
import org.thunderdog.challegram.widget.NoScrollTextView
import me.vkryl.android.DECELERATE_INTERPOLATOR
import me.vkryl.android.animator.BoolAnimator
import me.vkryl.android.animator.FactorAnimator
import me.vkryl.android.widget.FrameLayoutFix

class MediaFilterNameView (context: Context) : FrameLayoutFix(context), FactorAnimator.Target {
  private val name: TextView
  private val value: TextView

  private class AutoFitTextView (context: Context) : NoScrollTextView(context) {
    private var scaleX = 1f
    private var scaleY = 1f
    private var baseScale = 1f

    override fun onMeasure (widthMeasureSpec: Int, heightMeasureSpec: Int) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec)
      checkScale()
    }

    override fun onTextChanged (text: CharSequence, start: Int, lengthBefore: Int, lengthAfter: Int) {
      super.onTextChanged(text, start, lengthBefore, lengthAfter)
      checkScale()
    }

    private fun checkScale () {
      val layout = layout
      val lineWidth = if (layout != null && layout.lineCount > 0) layout.getLineWidth(0) else 0f
      val viewWidth = measuredWidth.toFloat()
      if (viewWidth > 0 && lineWidth > viewWidth) {
        baseScale = Math.min(1f, viewWidth / lineWidth)
        setMeasuredDimension(lineWidth.toInt(), measuredHeight)
      } else {
        baseScale = 1f
      }
      super.setScaleX(this.scaleX * baseScale)
      super.setScaleY(this.scaleY * baseScale)
      pivotX = measuredWidth.toFloat()
      pivotY = measuredHeight / 2f
    }

    override fun setScaleX (scaleX: Float) {
      this.scaleX = scaleX
      super.setScaleX(scaleX * baseScale)
    }

    override fun setScaleY (scaleY: Float) {
      this.scaleY = scaleY
      super.setScaleY(scaleY * baseScale)
    }
  }

  init {
    name = AutoFitTextView(context)
    name.setTextColor(0xffffffff.toInt())
    name.typeface = Fonts.getRobotoRegular()
    name.gravity = Gravity.RIGHT
    name.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
    name.layoutParams = FrameLayoutFix.newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL or Gravity.RIGHT)
    name.setSingleLine(true)
    addView(name)

    value = NoScrollTextView(context)
    value.setTextColor(0xff64CEFD.toInt())
    value.typeface = Fonts.getRobotoRegular()
    value.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
    value.alpha = 0f
    value.gravity = Gravity.CENTER
    value.layoutParams = FrameLayoutFix.newParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL or Gravity.RIGHT)
    value.setSingleLine(true)
    value.text = "0"
    addView(value)
  }

  private @ColorId var nameColorId: Int = 0
  private @ColorId var valueColorId: Int = 0

  fun addThemeListeners (themeProvider: ViewController<*>?) {
    if (themeProvider != null) {
      if (nameColorId != 0) {
        themeProvider.addThemeTextColorListener(name, nameColorId)
      }
      if (valueColorId != 0) {
        themeProvider.addThemeTextColorListener(value, valueColorId)
      }
    }
  }

  fun setColors (@ColorId nameColorId: Int, @ColorId valueColorId: Int) {
    this.nameColorId = nameColorId
    name.setTextColor(Theme.getColor(nameColorId))
    this.valueColorId = valueColorId
    value.setTextColor(Theme.getColor(valueColorId))
  }

  fun setSizes (size: Float) {
    name.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
    value.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
  }

  private var isDragging = false
  private var isAlwaysDragging = false
  private val animator = BoolAnimator(0, this, DECELERATE_INTERPOLATOR, 180L)

  fun setAlwaysDragging (isAlwaysDragging: Boolean) {
    if (this.isAlwaysDragging != isAlwaysDragging) {
      this.isAlwaysDragging = isAlwaysDragging
      animator.setValue(isAlwaysDragging || isDragging, false)
    }
  }

  fun setIsDragging (isDragging: Boolean, animated: Boolean) {
    if (this.isDragging != isDragging) {
      this.isDragging = isDragging
      if (animated) {
        animator.setDuration(if (name.text.length == 0) 120L else 180L)
      }
      animator.setValue(isDragging || isAlwaysDragging, animated)
    }
  }

  fun setName (name: CharSequence) {
    this.name.text = name
  }

  fun setValue (value: String) {
    this.value.text = value
  }

  fun setValueMaxWidth (maxWidth: Float) {
    this.value.minimumWidth = Math.round(maxWidth)
  }

  override fun onFactorChanged (id: Int, factor: Float, fraction: Float, callee: FactorAnimator?) {
    if (name.text.length == 0) {
      setStyle(value, factor)
    } else {
      val factor1 = if (factor <= .5f) 1f - (factor / .5f) else 0f
      val factor2 = if (factor > .5f) (factor - .5f) / .5f else 0f
      setStyle(name, factor1)
      setStyle(value, factor2)
    }
  }

  override fun onFactorChangeFinished (id: Int, finalFactor: Float, callee: FactorAnimator?) { }

  private fun setStyle (view: View, factor: Float) {
    view.alpha = factor
    val scale = MIN_SCALE + (1f - MIN_SCALE) * factor
    view.scaleX = scale
    view.scaleY = scale
  }

  companion object {
    private const val MIN_SCALE = .8f
  }
}
