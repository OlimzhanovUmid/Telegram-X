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
 * File created on 21/11/2016
 */
package org.thunderdog.challegram.widget

import android.content.Context
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.StringRes
import org.thunderdog.challegram.R
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.navigation.RtlCheckListener
import org.thunderdog.challegram.navigation.ViewController
import org.thunderdog.challegram.support.RippleSupport
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Fonts
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.tool.Views

class SettingStupidView (context: Context) : RelativeLayout(context), RtlCheckListener {
  private val titleView: TextView
  private val subtitleView: TextView

  private @ColorId var titleColorId: Int = 0
  private @ColorId var subtitleColorId: Int = 0

  init {
    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    minimumHeight = Screen.dp(76f)
    setPadding(Screen.dp(16f), Screen.dp(18f), Screen.dp(16f), Screen.dp(18f))

    var params: RelativeLayout.LayoutParams

    params = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    params.addRule(Lang.alignParent())

    titleView = NoScrollTextView(context)
    titleView.id = R.id.text_stupid
    titleColorId = ColorId.text
    titleView.setTextColor(Theme.getColor(titleColorId))
    titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
    titleView.typeface = Fonts.getRobotoRegular()
    titleView.layoutParams = params
    addView(titleView)

    params = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    params.addRule(RelativeLayout.BELOW, R.id.text_stupid)
    params.addRule(Lang.alignParent())
    params.topMargin = Screen.dp(2f)

    subtitleView = NoScrollTextView(context)
    subtitleColorId = ColorId.textLight
    subtitleView.setTextColor(Theme.getColor(subtitleColorId))
    subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
    subtitleView.typeface = Fonts.getRobotoRegular()
    subtitleView.layoutParams = params
    addView(subtitleView)

    Views.setClickable(this)
    RippleSupport.setSimpleWhiteBackground(this)
  }

  override fun checkRtl () {
    if (Views.setAlignParent(titleView, Lang.rtl()))
      Views.updateLayoutParams(titleView)
    if (Views.setAlignParent(subtitleView, Lang.rtl()))
      Views.updateLayoutParams(subtitleView)
  }

  fun setTitle (@StringRes titleResId: Int) {
    titleView.text = Lang.getString(titleResId)
  }

  fun setTitle (title: CharSequence?) {
    titleView.text = title
  }

  fun setSubtitle (@StringRes subtitleResId: Int) {
    subtitleView.text = Lang.getString(subtitleResId)
  }

  fun setSubtitle (subtitle: CharSequence?) {
    subtitleView.text = subtitle
  }

  fun setIsRed () {
    titleColorId = ColorId.textNegative
    titleView.setTextColor(Theme.getColor(titleColorId))
  }

  fun addThemeListeners (themeProvider: ViewController<*>?) {
    if (themeProvider != null) {
      themeProvider.addThemeTextColorListener(titleView, titleColorId)
      themeProvider.addThemeTextColorListener(subtitleView, subtitleColorId)
      themeProvider.addThemeInvalidateListener(this)
    }
  }
}
