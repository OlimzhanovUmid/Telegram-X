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
 * File created on 13/07/2018
 */
package org.thunderdog.challegram.mediaview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.view.View

import org.thunderdog.challegram.tool.DrawAlgorithms
import org.thunderdog.challegram.tool.Screen

import me.vkryl.android.animator.BoolAnimator
import me.vkryl.android.animator.FactorAnimator
import me.vkryl.android.DECELERATE_INTERPOLATOR

internal class PlayPauseButton (context: Context) : View(context), FactorAnimator.Target {
  private val playPausePath = Path()
  private var playPauseDrawFactor = -1f
  private val playPauseAnimator = BoolAnimator(0, this, DECELERATE_INTERPOLATOR, 160L)

  override fun onFactorChanged (id: Int, factor: Float, fraction: Float, callee: FactorAnimator?) {
    invalidate()
  }

  override fun onFactorChangeFinished (id: Int, finalFactor: Float, callee: FactorAnimator?) { }

  fun setIsPlaying (isPlaying: Boolean, animated: Boolean) {
    playPauseAnimator.setValue(isPlaying, animated)
  }

  override fun onDraw (c: Canvas) {
    DrawAlgorithms.drawPlayPause(c, measuredWidth / 2, measuredHeight / 2, Screen.dp(12f), playPausePath, playPauseDrawFactor, playPauseAnimator.getFloatValue().also { playPauseDrawFactor = it }, 1f, 0xffffffff.toInt())
  }
}
