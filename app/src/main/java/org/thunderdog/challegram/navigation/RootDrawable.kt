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
 * File created on 24/03/2016 at 12:01
 */
package org.thunderdog.challegram.navigation

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.graphics.drawable.Drawable.ConstantState
import org.thunderdog.challegram.BaseActivity
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Paints
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.tool.UI
import org.thunderdog.challegram.unsorted.Settings

class RootDrawable (context: BaseActivity) : Drawable() {
  private val state: RootState = RootState(context)

  override fun getConstantState (): ConstantState? {
    return state
  }

  fun setDisabled (isDisabled: Boolean): RootDrawable {
    state.isDisabled = isDisabled
    return this
  }

  override fun draw (c: Canvas) {
    if (!state.isDisabled) {
      c.drawColor(Theme.getColor(state.getColorId()))
    }
    if (Settings.instance().useEdgeToEdge()) {
      val width = state.context.getRootView().measuredWidth
      val height = state.context.getRootView().measuredHeight
      val rect = state.context.getRootView().systemInsetsWithoutIme
      val paint = Paints.fillingPaint(Color.BLACK)
      // top part
      if (rect.top != 0) {
        c.drawRect(0f, 0f, width.toFloat(), rect.top.toFloat(), paint)
      }
      // bottom part
      if (rect.bottom != 0) {
        c.drawRect(0f, (height - rect.bottom).toFloat(), width.toFloat(), height.toFloat(), paint)
      }
      // left part
      if (rect.left != 0) {
        c.drawRect(0f, 0f, rect.left.toFloat(), height.toFloat(), paint)
      }
      // right part
      if (rect.right != 0) {
        c.drawRect((width - rect.right).toFloat(), 0f, width.toFloat(), height.toFloat(), paint)
      }
    } else {
      val height = Screen.getNavigationBarHeight()
      if (height > 0 && state.context.hadSoftwareKeysOnActivityLaunch()) {
        val rotation = state.context.getWindowRotationDegrees()
        val bounds = bounds
        when (rotation) {
          0, 180 ->
            c.drawRect(bounds.left.toFloat(), (bounds.bottom - height).toFloat(), bounds.right.toFloat(), bounds.bottom.toFloat(), Paints.fillingPaint(UI.NAVIGATION_BAR_COLOR))
          90 ->
            c.drawRect((bounds.right - height).toFloat(), bounds.top.toFloat(), bounds.right.toFloat(), bounds.bottom.toFloat(), Paints.fillingPaint(UI.NAVIGATION_BAR_COLOR))
          270 ->
            c.drawRect(bounds.left.toFloat(), bounds.top.toFloat(), (bounds.left + height).toFloat(), bounds.bottom.toFloat(), Paints.fillingPaint(UI.NAVIGATION_BAR_COLOR))
        }
      }
    }
  }

  override fun setAlpha (alpha: Int) {

  }

  override fun setColorFilter (colorFilter: ColorFilter?) {

  }

  @Suppress("deprecation")
  override fun getOpacity (): Int {
    return PixelFormat.UNKNOWN
  }

  private class RootState (internal val context: BaseActivity) : ConstantState() {
    internal var isDisabled: Boolean = false

    internal fun getColorId (): Int {
      if (context.isPasscodeShowing()) {
        return ColorId.headerBackground
      }
      val navigation = context.navigation()
      var colorId = ColorId.filling
      val popup = context.getCurrentlyOpenWindowedViewController()
      if (popup != null) {
        colorId = popup.rootColorId
      } else if (navigation != null) {
        val v = navigation.getCurrentStackItem()
        if (v != null && !v.usePopupMode()) {
          colorId = v.rootColorId
        }
      }
      return colorId
    }

    override fun newDrawable (): Drawable {
      val rootDrawable = RootDrawable(context)
      rootDrawable.setDisabled(isDisabled)
      return rootDrawable
    }

    override fun getChangingConfigurations (): Int {
      return 0
    }
  }
}
