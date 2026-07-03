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
 */
package org.thunderdog.challegram.widget

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView

import org.thunderdog.challegram.navigation.ViewController
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.tool.Views

import me.vkryl.android.setBackground
import me.vkryl.android.widget.FrameLayoutFix

class DoubleTextViewWithIcon (context: Context) : FrameLayoutFix(context) {
  private val iconView: ImageView
  private val textView: DoubleTextView

  init {
    val iconWidth = Screen.dp(56f)
    iconView = ImageView(context)
    textView = DoubleTextView(context)
    textView.ignoreStartOffset(true)
    textView.layoutParams = FrameLayoutFix.newParams(ViewGroup.LayoutParams.MATCH_PARENT, Screen.dp(72f), 0, iconWidth, 0, 0, 0)
    iconView.layoutParams = FrameLayoutFix.newParams(iconWidth, Screen.dp(72f))
    iconView.scaleType = ImageView.ScaleType.CENTER
    iconView.setColorFilter(Theme.getColor(ColorId.icon))
    addView(iconView)
    addView(textView)
    setBackgroundColor(Theme.fillingColor())
  }

  fun icon (): ImageView {
    return iconView
  }

  fun text (): DoubleTextView {
    return textView
  }

  fun attach () {
    textView.attach()
  }

  fun detach () {
    textView.detach()
  }

  fun setIconClickListener (listener: View.OnClickListener?) {
    Views.setClickable(iconView)
    setBackground(iconView, Theme.transparentSelector())
    iconView.setOnClickListener(listener)
  }

  fun setTextClickListener (listener: View.OnClickListener?) {
    Views.setClickable(textView)
    setBackground(textView, Theme.transparentSelector())
    textView.setOnClickListener(listener)
  }

  fun addThemeListeners (themeProvider: ViewController<*>?) {
    if (themeProvider != null) {
      textView.addThemeListeners(themeProvider)
      themeProvider.addThemeBackgroundColorListener(this, ColorId.filling)
      themeProvider.addThemeFilterListener(iconView, ColorId.icon)
    }
  }

  fun checkRtl () {
    textView.checkRtl()
  }
}
