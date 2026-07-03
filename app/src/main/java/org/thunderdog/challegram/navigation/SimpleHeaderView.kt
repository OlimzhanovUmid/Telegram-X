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
package org.thunderdog.challegram.navigation

import android.content.Context
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView

import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.tool.Fonts
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.widget.NoScrollTextView

import me.vkryl.android.widget.FrameLayoutFix

open class SimpleHeaderView (context: Context) : FrameLayoutFix(context), ColorSwitchPreparator, TextChangeDelegate {
  private val title: TextView = newTitle(context)

  init {
    title.tag = this
    addView(title)
  }

  fun initWithController (c: ViewController<*>) {
    title.text = c.getName()
  }

  override fun setTextColor (color: Int) {
    title.setTextColor(color)
  }

  final override fun prepareColorChangers (fromColor: Int, toColor: Int) {
    // reserved for children
  }

  companion object {
    @JvmStatic
    fun newTitle (context: Context): TextView {
      val params = FrameLayoutFix.newParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP or (if (Lang.rtl()) Gravity.RIGHT else Gravity.LEFT))
      params.setMargins(0, Screen.dp(15f), 0, 0)
      if (Lang.rtl()) {
        params.rightMargin = Screen.dp(68f)
      } else {
        params.leftMargin = Screen.dp(68f)
      }

      val title = NoScrollTextView(context)
      title.typeface = Fonts.getRobotoMedium()
      title.setSingleLine()
      title.gravity = Gravity.LEFT
      title.ellipsize = TextUtils.TruncateAt.END
      title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 19f)
      title.setTextColor(0xffffffff.toInt())
      title.layoutParams = params

      return title
    }
  }
}
