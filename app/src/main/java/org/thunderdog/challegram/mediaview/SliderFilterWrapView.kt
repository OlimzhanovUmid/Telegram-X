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
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.tool.Screen
import me.vkryl.android.widget.FrameLayoutFix

class SliderFilterWrapView (context: Context) : FrameLayoutFix(context), SliderView.Listener {
  private val nameView: MediaFilterNameView
  private val sliderView: SliderView
  private var callback: Callback? = null

  init {
    var params = FrameLayoutFix.newParams(Screen.dp(86f), ViewGroup.LayoutParams.MATCH_PARENT)
    params.bottomMargin = Screen.dp(2.5f)

    nameView = MediaFilterNameView(context)
    nameView.layoutParams = params
    addView(nameView)

    params = FrameLayoutFix.newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    params.leftMargin = Screen.dp(64f) + Screen.dp(22f) + Screen.dp(18f) - Screen.dp(12f)
    params.rightMargin = Screen.dp(22f) - Screen.dp(12f)

    sliderView = SliderView(context)
    sliderView.setPadding(Screen.dp(12f), Screen.dp(1f), Screen.dp(12f), 0)
    sliderView.setListener(this)
    sliderView.layoutParams = params
    addView(sliderView)

    layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Screen.dp(42f))
  }

  fun setData (name: String, value: Int, floatValue: Float, anchorMode: Int, @ColorId colorId: Int, isEnabled: Boolean) {
    nameView.setName(name)
    nameView.setValue(if (value == 0) "0" else if (value > 0) "+$value" else value.toString())
    sliderView.setColorId(colorId, false)
    sliderView.setValue(floatValue)
    sliderView.setAnchorMode(anchorMode)
    sliderView.setSlideEnabled(isEnabled, false)
  }

  fun setSlideEnabled (isEnabled: Boolean) {
    sliderView.setSlideEnabled(isEnabled, true)
  }

  fun setColorId (@ColorId colorId: Int) {
    sliderView.setColorId(colorId, true)
  }

  fun setCallback (callback: Callback?) {
    this.callback = callback
  }

  override fun onSetStateChanged (view: SliderView, isSetting: Boolean) {
    nameView.setIsDragging(isSetting, true)
    callback?.let {
      if (isSetting) {
        it.onChangeStarted(this)
      } else {
        it.onChangeEnded(this)
      }
    }
  }

  override fun allowSliderChanges (view: SliderView): Boolean {
    return callback == null || callback!!.canApplySliderChanges()
  }

  override fun onValueChanged (view: SliderView, factor: Float) {
    val origValue = factor * 100f
    val value = Math.round(origValue)
    nameView.setValue(if (value == 0) "0" else if (value > 0) "+$value" else value.toString())
    callback?.onValueChanged(this, value)
  }

  interface Callback {
    fun canApplySliderChanges (): Boolean
    fun onChangeStarted (wrapView: SliderFilterWrapView)
    fun onChangeEnded (wrapView: SliderFilterWrapView)
    fun onValueChanged (wrapView: SliderFilterWrapView, value: Int)
  }
}
