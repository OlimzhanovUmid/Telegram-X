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
 * File created on 11/05/2015 at 20:06
 */
package org.thunderdog.challegram.navigation

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.view.MotionEvent
import android.view.View
import android.view.Window

import me.vkryl.core.color
import me.vkryl.core.compositeColor
import me.vkryl.core.util.ColorChanger

import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.UI
import org.thunderdog.challegram.util.Unlockable

class OverlayView (context: Context) : View(context) {
  private var unlockable: Unlockable? = null
  private var emptyUnlockable = false
  private var changeBarColor = false

  fun setEmptyUnlockable () {
    emptyUnlockable = true
  }

  fun setUnlockable (unlockable: Unlockable?) {
    this.unlockable = unlockable
  }

  private var color = 0
  // private var drawFilling = false
  private var window: Window? = null
  private var brightnessFactor = 0f
  private var barFactor = 0f
  private var statusChanger: ColorChanger? = null

  fun setData (backgroundColor: Int, mode: Int) {
    barFactor = if (mode == OVERLAY_MODE_DEFAULT) Theme.getPopupOverlayAlpha() else .6f
    color = backgroundColor
    val c = UI.getCurrentStackItem()
    changeBarColor = (c != null && !c.usePopupMode()) && color != 0x00000000 && color != 0xffffffff.toInt()
    if (changeBarColor) {
      window = UI.getWindow()
      brightnessFactor = 1f

      val fromColor = if (mode == OVERLAY_MODE_DRAWER) c!!.statusBarColor else window!!.statusBarColor
      val toColor = compositeColor(fromColor, color((barFactor * brightnessFactor * 255f).toInt(), color))

      if (statusChanger == null) {
        statusChanger = ColorChanger(fromColor, toColor)
      } else {
        statusChanger!!.setFromTo(fromColor, toColor)
      }
    }
  }

  fun getCurrentStatusBarColor (): Int {
    return if (statusChanger == null) 0 else statusChanger!!.getColor(alpha / barFactor * brightnessFactor)
  }

  override fun onTouchEvent (event: MotionEvent): Boolean {
    if (event.action == MotionEvent.ACTION_DOWN) {
      unlockable?.unlock()
    }
    return unlockable != null || emptyUnlockable
  }

  override fun onDraw (canvas: Canvas) {
    if (Color.alpha(color) > 0) {
      canvas.drawColor(color)
    }
  }

  companion object {
    const val OVERLAY_MODE_DEFAULT = 1
    const val OVERLAY_MODE_DRAWER = 2
    const val OVERLAY_MODE_PROGRESS = 3
  }
}
