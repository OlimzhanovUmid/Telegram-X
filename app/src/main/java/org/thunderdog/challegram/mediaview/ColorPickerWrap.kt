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
import org.thunderdog.challegram.mediaview.data.FiltersState
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.tool.Screen
import me.vkryl.android.widget.FrameLayoutFix

class ColorPickerWrap (context: Context) : FrameLayoutFix(context), View.OnClickListener {
  private val nameView: MediaFilterNameView
  private val colorsWrap: LinearLayout
  private var activeIndex = -1
  private var listener: Listener? = null
  private lateinit var colorIds: IntArray

  init {
    var params = FrameLayoutFix.newParams(Screen.dp(86f), ViewGroup.LayoutParams.MATCH_PARENT)
    params.bottomMargin = Screen.dp(2.5f)

    nameView = MediaFilterNameView(context)
    nameView.layoutParams = params
    addView(nameView)

    params = FrameLayoutFix.newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    params.leftMargin = Screen.dp(64f) + Screen.dp(22f) + Screen.dp(18f) - Screen.dp(12f)
    params.rightMargin = Screen.dp(22f) - Screen.dp(12f)

    colorsWrap = LinearLayout(context)
    colorsWrap.orientation = LinearLayout.HORIZONTAL

    for (ignored in FiltersState.SHADOWS_TINT_COLOR_IDS) {
      val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
      val circle = CheckCircle(context)
      circle.layoutParams = lp
      circle.setOnClickListener(this)
      colorsWrap.addView(circle)
    }

    colorsWrap.layoutParams = params
    addView(colorsWrap)

    layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Screen.dp(42f))
  }

  fun setListener (listener: Listener?) {
    this.listener = listener
  }

  fun setData (name: String, @ColorId colorIds: IntArray, selectedColor: Int) {
    nameView.setName(name)
    this.colorIds = colorIds

    var foundActiveIndex = -1
    for (i in colorIds.indices) {
      val colorId = colorIds[i]
      val circle = colorsWrap.getChildAt(i) as CheckCircle
      circle.setColorId(if (colorId == 0) ColorId.white else colorId)
      val isChecked = colorId == selectedColor
      circle.setChecked(isChecked, false)
      if (isChecked) {
        foundActiveIndex = i
      }
    }
    activeIndex = foundActiveIndex
  }

  override fun onClick (v: View) {
    if (listener != null && !listener!!.allowColorChanges()) {
      return
    }
    val i = colorsWrap.indexOfChild(v)
    if (i != -1 && i != activeIndex) {
      if (activeIndex != -1) {
        val oldCircle = colorsWrap.getChildAt(activeIndex) as CheckCircle
        oldCircle.setChecked(false, true)
      }
      activeIndex = i
      (v as CheckCircle).setChecked(true, true)
      listener?.onColorChanged(this, colorIds[i])
    }
  }

  interface Listener {
    fun onColorChanged (wrap: ColorPickerWrap, @ColorId newColorId: Int)
    fun allowColorChanges (): Boolean
  }
}
