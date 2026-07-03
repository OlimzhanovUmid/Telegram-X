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
 * File created on 30/06/2019
 */
package org.thunderdog.challegram.widget

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView

import java.util.Locale

import org.thunderdog.challegram.R
import org.thunderdog.challegram.navigation.ViewController
import org.thunderdog.challegram.support.ViewSupport
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.tool.Views

import me.vkryl.android.DECELERATE_INTERPOLATOR
import me.vkryl.android.animator.BoolAnimator
import me.vkryl.android.animator.FactorAnimator
import me.vkryl.android.widget.FrameLayoutFix

class SnackBar (context: Context) : RelativeLayout(context) {
  interface Callback {
    fun onSnackBarTransition (v: SnackBar, factor: Float)
    fun onDestroySnackBar (v: SnackBar) { }
  }

  private val textView: TextView
  private val actionView: TextView

  private val isShowing: BoolAnimator

  init {
    var rp: RelativeLayout.LayoutParams

    rp = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    rp.addRule(RelativeLayout.LEFT_OF, R.id.text_title)
    rp.topMargin = Screen.dp(2f)
    rp.bottomMargin = Screen.dp(2f)

    textView = TextView(context)
    textView.setTextColor(Theme.getColor(ColorId.snackbarUpdateText))
    textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
    textView.setPadding(Screen.dp(12f), Screen.dp(12f), 0, Screen.dp(12f))
    textView.layoutParams = rp
    addView(textView)

    rp = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    rp.leftMargin = Screen.dp(2f)
    rp.rightMargin = Screen.dp(2f)
    rp.topMargin = Screen.dp(2f)
    rp.bottomMargin = Screen.dp(2f)
    rp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
    actionView = TextView(context)
    actionView.setPadding(Screen.dp(12f), Screen.dp(12f), Screen.dp(12f), Screen.dp(12f))
    actionView.setTextColor(Theme.getColor(ColorId.snackbarUpdateAction))
    actionView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
    actionView.layoutParams = rp
    Views.setClickable(actionView)
    addView(actionView)

    ViewSupport.setThemedBackground(this, ColorId.snackbarUpdate)

    isShowing = BoolAnimator(0, object : FactorAnimator.Target {
      override fun onFactorChanged (id: Int, factor: Float, fraction: Float, callee: FactorAnimator?) {
        updateTranslation()
      }

      override fun onFactorChangeFinished (id: Int, finalFactor: Float, callee: FactorAnimator?) {
        updateTranslation()
        if (finalFactor == 0f && !isShowing.getValue() && callback != null) {
          callback!!.onDestroySnackBar(this@SnackBar)
        }
      }
    }, DECELERATE_INTERPOLATOR, 180L)

    layoutParams = FrameLayoutFix.newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM)
    setOnTouchListener { _, _ -> true }
  }

  private var callback: Callback? = null

  fun setCallback (callback: Callback?): SnackBar {
    this.callback = callback
    return this
  }

  fun setText (text: String): SnackBar {
    textView.text = text
    return this
  }

  fun setAction (action: String, callback: Runnable, dismissAutomatically: Boolean): SnackBar {
    Views.setMediumText(actionView, action.uppercase(Locale.getDefault()))
    actionView.setOnClickListener {
      callback.run()
      if (dismissAutomatically) {
        dismissSnackBar(true)
      }
    }
    return this
  }

  fun showSnackBar (animated: Boolean): SnackBar {
    isShowing.setValue(true, animated)
    return this
  }

  fun dismissSnackBar (animated: Boolean): SnackBar {
    isShowing.setValue(false, animated)
    return this
  }

  fun addThemeListeners (themeProvider: ViewController<*>?): SnackBar {
    if (themeProvider != null) {
      themeProvider.addThemeTextColorListener(actionView, ColorId.snackbarUpdateAction)
      themeProvider.addThemeTextColorListener(textView, ColorId.snackbarUpdateText)
      themeProvider.addThemeInvalidateListener(this)
    }
    return this
  }

  fun removeThemeListeners (themeProvider: ViewController<*>?): SnackBar {
    if (themeProvider != null) {
      themeProvider.removeThemeListenerByTarget(textView)
      themeProvider.removeThemeListenerByTarget(actionView)
      themeProvider.removeThemeListenerByTarget(this)
    }
    return this
  }

  private fun updateTranslation () {
    val y = measuredHeight * (1f - isShowing.getFloatValue())
    if (translationY != y || y == 0f) {
      if (callback != null) {
        callback!!.onSnackBarTransition(this, isShowing.getFloatValue())
      }
      translationY = y
    }
  }

  fun getVisibilityFactor (): Float {
    return isShowing.getFloatValue()
  }

  override fun onMeasure (widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    updateTranslation()
  }
}
