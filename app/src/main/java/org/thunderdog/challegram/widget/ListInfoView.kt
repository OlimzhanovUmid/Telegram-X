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
 * File created on 27/04/2015 at 15:36
 */
package org.thunderdog.challegram.widget

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView

import androidx.recyclerview.widget.RecyclerView

import org.thunderdog.challegram.navigation.ViewController
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Fonts
import org.thunderdog.challegram.tool.Screen

import me.vkryl.android.widget.FrameLayoutFix

class ListInfoView (context: Context) : FrameLayoutFix(context) {
  private val textView: TextView
  private val progress: ProgressBar
  private var spinnerView: SpinnerView? = null

  private var isEmptyView = false

  init {
    layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

    textView = NoScrollTextView(context)
    textView.typeface = Fonts.getRobotoRegular()
    textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f)
    textView.setTextColor(Theme.textDecent2Color())
    textView.visibility = View.GONE

    progress = ProgressBar(getContext())
    progress.isIndeterminate = true
    addView(progress, LayoutParams(Screen.dp(32f), Screen.dp(32f), Gravity.CENTER))

    addView(textView, LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
  }

  fun addThemeListeners (themeProvider: ViewController<*>?) {
    if (themeProvider != null) {
      themeProvider.addThemeTextColorListener(textView, ColorId.background_textLight)
    }
  }

  override fun onMeasure (widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(getCurrentHeight(), MeasureSpec.EXACTLY))
  }

  private fun getCurrentHeight (): Int {
    return if (isEmptyView) Math.max(if (parent == null) 0 else (parent as ViewGroup).measuredHeight, Screen.dp(42f)) else Screen.dp(42f)
  }

  fun showProgress () {
    isEmptyView = false
    progress.visibility = View.VISIBLE
    textView.visibility = View.GONE
    val height = measuredHeight
    if (height != 0 && height != getCurrentHeight()) {
      requestLayout()
    }
  }

  fun showInfo (message: CharSequence) {
    if (isEmptyView) {
      textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f)
      isEmptyView = false
    }
    setText(message)
  }

  fun showEmpty (empty: CharSequence) {
    if (!isEmptyView) {
      textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
      isEmptyView = true
    }
    setText(empty)
  }

  private fun setText (text: CharSequence) {
    progress.visibility = View.GONE
    textView.text = text
    textView.visibility = View.VISIBLE
    val height = measuredHeight
    if (height != 0 && height != getCurrentHeight()) {
      requestLayout()
    }
  }

  fun getText (): CharSequence {
    return textView.text
  }
}
