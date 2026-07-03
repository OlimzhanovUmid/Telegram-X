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
 * File created on 08/08/2015 at 16:55
 */
package org.thunderdog.challegram.component.passcode

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import org.thunderdog.challegram.navigation.HeaderView
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.tool.UI
import me.vkryl.android.widget.FrameLayoutFix

class PinInputLayout (context: Context) : FrameLayoutFix(context), View.OnClickListener, View.OnLongClickListener {
  companion object {
    const val BUTTON_WIDTH = 106
    private const val BUTTON_HEIGHT = 82
  }

  private var callback: Callback? = null
  private var hasFeedback = false

  fun initWithFeedback (hasFeedback: Boolean) {
    this.hasFeedback = hasFeedback
    fillButtons()
    setLayoutParams(ViewGroup.LayoutParams(Screen.dp((BUTTON_WIDTH * 3).toFloat()), ViewGroup.LayoutParams.MATCH_PARENT))
  }

  fun setCallback (callback: Callback?) {
    this.callback = callback
  }

  fun setHasFeedback (hasFeedback: Boolean) {
    if (this.hasFeedback != hasFeedback) {
      this.hasFeedback = hasFeedback
      for (i in 0 until childCount) {
        val v = getChildAt(i)
        if (v != null && v is PinButtonView) {
          v.setHasFeedback(hasFeedback)
        }
      }
    }
  }

  private var buttonHeight = 0

  private fun fillButtons () {
    var view: PinButtonView
    var params: FrameLayout.LayoutParams

    var cx = 0
    var cy = 0

    val buttonWidth = Screen.dp(BUTTON_WIDTH.toFloat())
    buttonHeight = getButtonHeight()

    var i = 0
    var number = 1
    while (i < 9) {
      params = FrameLayoutFix.newParams(buttonWidth, buttonHeight)
      params.setMargins(cx, cy, 0, 0)

      view = PinButtonView(getContext())
      view.setHasFeedback(hasFeedback)
      view.setId(number)
      view.setNumber(number)
      view.setOnClickListener(this)
      view.setLayoutParams(params)

      addView(view)

      i++
      number++

      if (i % 3 == 0) {
        cx = 0
        cy += buttonHeight
      } else {
        cx += buttonWidth
      }
    }

    cx += buttonWidth

    params = FrameLayoutFix.newParams(buttonWidth, buttonHeight)
    params.setMargins(cx, cy, 0, 0)

    view = PinButtonView(getContext())
    view.setHasFeedback(hasFeedback)
    view.setId(0)
    view.setNumber(0)
    view.setOnClickListener(this)
    view.setLayoutParams(params)

    addView(view)

    cx += buttonWidth

    params = FrameLayoutFix.newParams(buttonWidth, buttonHeight)
    params.setMargins(cx, cy, 0, 0)

    view = PinButtonView(getContext())
    view.setHasFeedback(hasFeedback)
    view.setId(-1)
    view.setNumber(-1)
    view.setOnClickListener(this)
    view.setOnLongClickListener(this)
    view.setLayoutParams(params)

    addView(view)
  }

  fun updateHeights () {
    buttonHeight = getButtonHeight()
    var cy = 0
    var i = 0
    while (i < childCount) {
      val view = getChildAt(i)

      val params = view.layoutParams as FrameLayout.LayoutParams
      params.height = buttonHeight
      params.topMargin = cy

      i++

      if (i % 3 == 0) {
        cy += buttonHeight
      }
    }
  }

  private fun getButtonHeight (): Int {
    val availableHeight = Screen.currentActualHeight() - HeaderView.getSize(false) - (if (UI.isLandscape()) 0 else Screen.dp(156f) + Screen.dp(32f))
    val height = Screen.dpf(BUTTON_HEIGHT.toFloat())
    val factor = availableHeight / (height * 4f)
    return (height * Math.min(factor, 1.2f)).toInt()
  }

  override fun onClick (v: View) {
    val callback = callback
    if (callback != null) {
      if (v.id == -1) {
        callback.onPinRemove()
      } else {
        callback.onPinAppend(v.id)
      }
      UI.hapticVibrate(v, false)
    }
  }

  /*override fun onMeasure (widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val p = getLayoutParams()
    setMeasuredDimension(MeasureSpec.makeMeasureSpec(p.width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(p.height, MeasureSpec.EXACTLY))
  }

  override fun onLayout (changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    for (i in 0 until childCount) {
      val v = getChildAt(i)
      val mp = v.layoutParams as MarginLayoutParams
      v.measure(MeasureSpec.makeMeasureSpec(mp.width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(mp.height, MeasureSpec.EXACTLY))
      v.layout(mp.leftMargin, mp.topMargin, mp.leftMargin + mp.width, mp.topMargin + mp.height)
    }
  }*/

  override fun onLongClick (v: View): Boolean {
    return callback != null && callback!!.onPinRemoveAll()
  }

  interface Callback {
    fun onPinRemove ()
    fun onPinRemoveAll (): Boolean
    fun onPinAppend (number: Int)
  }
}
