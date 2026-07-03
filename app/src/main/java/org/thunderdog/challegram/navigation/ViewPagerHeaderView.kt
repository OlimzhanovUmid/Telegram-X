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
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout

import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.unsorted.Size

import me.vkryl.android.widget.FrameLayoutFix

class ViewPagerHeaderView (context: Context) : SimpleHeaderView(context), StretchyHeaderView, PagerHeaderView {
  private val topView: ViewPagerTopView

  init {
    val params: FrameLayout.LayoutParams = FrameLayoutFix.newParams(ViewGroup.LayoutParams.MATCH_PARENT, Size.getHeaderDrawerSize(), Gravity.TOP)
    params.topMargin = Size.getHeaderPortraitSize()

    topView = ViewPagerTopView(context)
    topView.setLayoutParams(params)
    topView.setSelectionColorId(ColorId.headerTabActive)
    topView.setTextFromToColorId(ColorId.headerTabInactiveText, ColorId.headerTabActiveText)
    addView(topView)
  }

  override fun checkRtl () {
    topView.checkRtl()
  }

  override fun getView (): View {
    return this
  }

  override fun getTopView (): ViewPagerTopView {
    return topView
  }

  override fun setScaleFactor (scaleFactor: Float, fromFactor: Float, toFactor: Float, byScroll: Boolean) {
    val totalScale = Size.getHeaderDrawerSize().toFloat() / Size.getHeaderSizeDifference(false).toFloat()
    val actualScaleFactor = scaleFactor / totalScale

    //noinspection Range
    topView.setAlpha(if (actualScaleFactor <= TOP_SCALE_LIMIT) 0f else (actualScaleFactor - TOP_SCALE_LIMIT) / TOP_SCALE_LIMIT)
    topView.setTranslationY((-Size.getHeaderDrawerSize()).toFloat() * (1f - actualScaleFactor))
  }

  companion object {
    private const val TOP_SCALE_LIMIT = .25f
  }
}
