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
 * File created on 15/05/2015 at 19:25
 */
package org.thunderdog.challegram.component.base

import android.content.Context
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView

import org.thunderdog.challegram.support.ViewSupport
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.theme.ThemeListenerList
import org.thunderdog.challegram.tool.Fonts
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.widget.NoScrollTextView

import me.vkryl.android.widget.FrameLayoutFix

class ProgressWrap (context: Context) : FrameLayoutFix(context) {
  private val textView: TextView
  private val progressBar: ProgressBar

  fun addThemeListeners (themeProvider: ThemeListenerList?) {
    if (themeProvider != null) {
      themeProvider.addThemeInvalidateListener(this)
      themeProvider.addThemeTextAccentColorListener(textView)
    }
  }

  init {
    val width = minOf(Screen.smallestSide() - Screen.dp(56f), Screen.dp(300f))
    val height = Screen.dp(94f)

    ViewSupport.setThemedBackground(this, ColorId.filling)

    var params = FrameLayoutFix.newParams(Screen.dp(36f), Screen.dp(36f), Gravity.LEFT or Gravity.CENTER_VERTICAL)
    params.setMargins(Screen.dp(12f), 0, 0, 0)

    progressBar = ProgressBar(getContext())
    progressBar.isIndeterminate = true
    progressBar.layoutParams = params
    addView(progressBar)

    params = FrameLayoutFix.newParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.LEFT or Gravity.CENTER_VERTICAL)
    params.setMargins(Screen.dp(60f), Screen.dp(1f), 0, 0)

    textView = NoScrollTextView(context)
    textView.setTextColor(Theme.textAccentColor())
    textView.gravity = Gravity.LEFT
    textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
    textView.typeface = Fonts.getRobotoRegular()
    textView.ellipsize = TextUtils.TruncateAt.END
    textView.maxWidth = width - Screen.dp(64f)
    textView.maxLines = 2
    textView.layoutParams = params

    addView(textView)
    layoutParams = FrameLayoutFix.newParams(width, height, Gravity.CENTER)
  }

  fun setMessage (message: String) {
    textView.text = message
  }

  fun getProgress (): ProgressBar? {
    return progressBar
  }
}
