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
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import org.thunderdog.challegram.R
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.tool.Screen
import me.vkryl.android.widget.FrameLayoutFix

class BlurButtonsWrap (context: Context) : FrameLayoutFix(context), View.OnClickListener {
  private val nameView: MediaFilterNameView
  private val buttonsWrap: LinearLayout
  private var activeIndex = -1
  private var listener: Listener? = null

  init {
    var params = FrameLayoutFix.newParams(Screen.dp(86f), ViewGroup.LayoutParams.MATCH_PARENT)
    params.bottomMargin = Screen.dp(2.5f)

    nameView = MediaFilterNameView(context)
    nameView.layoutParams = params
    nameView.setName(Lang.getString(R.string.Blur))
    addView(nameView)

    params = FrameLayoutFix.newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    params.leftMargin = Screen.dp(64f) + Screen.dp(22f) + Screen.dp(18f) - Screen.dp(12f)
    params.rightMargin = Screen.dp(22f) - Screen.dp(12f)

    buttonsWrap = LinearLayout(context)
    buttonsWrap.orientation = LinearLayout.HORIZONTAL

    for (i in 0 until 3) {
      val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
      val button = BlurButton(context)
      button.setText(Lang.getString(if (i == 0) R.string.Off else if (i == 1) R.string.BlurRadial else R.string.BlurLinear).uppercase())
      button.setOnClickListener(this)
      if (i == 0) {
        button.setPadding(Screen.dp(20f), 0, 0, 0)
      } else if (i == 2) {
        button.setPadding(0, 0, Screen.dp(20f), 0)
      }
      button.layoutParams = lp
      buttonsWrap.addView(button)
    }

    buttonsWrap.layoutParams = params
    addView(buttonsWrap)

    layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Screen.dp(42f))
  }

  fun setData (selected: Int) {
    if (activeIndex != selected) {
      if (activeIndex != -1) {
        val button = buttonsWrap.getChildAt(activeIndex) as BlurButton
        button.setIsChecked(false, false)
      }
      activeIndex = selected
      if (selected != -1) {
        val button = buttonsWrap.getChildAt(selected) as BlurButton
        button.setIsChecked(true, false)
      }
    }
  }

  fun setListener (listener: Listener?) {
    this.listener = listener
  }

  override fun onClick (v: View) {
    if (listener != null && !listener!!.allowBlurChanges()) {
      return
    }
    val i = buttonsWrap.indexOfChild(v)
    if (i != -1 && activeIndex != i) {
      if (activeIndex != -1) {
        val button = buttonsWrap.getChildAt(activeIndex) as BlurButton
        button.setIsChecked(false, true)
      }
      activeIndex = i
      val button = buttonsWrap.getChildAt(i) as BlurButton
      button.setIsChecked(true, true)

      listener?.onBlurModeChanged(this, i)
    }
  }

  interface Listener {
    fun onBlurModeChanged (wrap: BlurButtonsWrap, newMode: Int)
    fun allowBlurChanges (): Boolean
  }
}
