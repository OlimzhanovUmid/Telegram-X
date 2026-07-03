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
 * File created on 23/03/2018
 */
package org.thunderdog.challegram.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import android.os.Handler
import android.os.Message
import android.os.SystemClock
import android.util.TypedValue
import android.view.Gravity

import org.thunderdog.challegram.U
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Fonts
import org.thunderdog.challegram.tool.Paints
import org.thunderdog.challegram.tool.Screen

import me.vkryl.android.DECELERATE_INTERPOLATOR
import me.vkryl.android.animator.BoolAnimator
import me.vkryl.android.animator.FactorAnimator
import me.vkryl.core.alphaColor
import me.vkryl.core.clamp
import me.vkryl.core.modulo

class TimerView (context: Context) : NoScrollTextView(context), FactorAnimator.Target, Handler.Callback {
  private val handler = Handler(this)
  private val isVisible = BoolAnimator(0, this, DECELERATE_INTERPOLATOR, 180L)

  init {
    setTypeface(Fonts.getRobotoMedium())
    setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
    setGravity(Gravity.CENTER)
    setPadding(0, 0, 0, Screen.dp(1f))
    setAlpha(0f)
  }

  override fun handleMessage (msg: Message): Boolean {
    when (msg.what) {
      0 -> {
        if (invalidateScheduled) {
          invalidateScheduled = false
          invalidate()
        }
      }
      1 -> {
        if (textUpdateScheduled) {
          textUpdateScheduled = false
          updateText()
        }
      }
    }
    return true
  }

  override fun onFactorChanged (id: Int, factor: Float, fraction: Float, callee: FactorAnimator?) {
    setAlpha(factor)
  }

  override fun onFactorChangeFinished (id: Int, finalFactor: Float, callee: FactorAnimator?) { }

  private var livePeriod: Int = 0
  private var aliveExpiresAt: Long = 0

  fun interface ActiveStateChangeListener {
    fun onActiveStateChanged (v: TimerView, isActive: Boolean)
  }

  private var listener: ActiveStateChangeListener? = null

  fun setListener (listener: ActiveStateChangeListener?) {
    this.listener = listener
  }

  fun isTimerVisible (): Boolean {
    return isVisible.getValue()
  }

  private fun setIsActive (isActive: Boolean, animated: Boolean) {
    val nowIsVisible = this.isVisible.getValue()
    this.isVisible.setValue(isActive, animated)
    if (nowIsVisible != isActive) {
      if (listener != null) {
        listener!!.onActiveStateChanged(this, isActive)
      }
    }
  }

  fun setLivePeriod (livePeriod: Int, expiresAt: Long) {
    setIsActive(expiresAt > 0, false)
    this.livePeriod = livePeriod
    this.aliveExpiresAt = expiresAt

    if (invalidateScheduled || textUpdateScheduled) {
      invalidateScheduled = false
      textUpdateScheduled = false
      handler.removeCallbacksAndMessages(null)
    }

    updateText()
    invalidate()
  }

  private fun updateText () {
    if (aliveExpiresAt == 0L) {
      return
    }
    val now = SystemClock.uptimeMillis()
    val millisRemaining = aliveExpiresAt - now
    if (millisRemaining <= 0) {
      aliveExpiresAt = 0
      setIsActive(false, false)
      return
    }

    val timeTillNextTimerUpdate: Long
    val res: String
    if (millisRemaining <= 60000) {
      val seconds = (millisRemaining / 1000L).toInt()
      timeTillNextTimerUpdate = 1000 // - millisRemaining % 1000;
      res = seconds.toString()
    } else if (millisRemaining < 60000 * 60) {
      val minutes = ((millisRemaining / 1000L).toDouble() / 60.0).toInt()
      timeTillNextTimerUpdate = 60000 - millisRemaining % 60000
      res = minutes.toString()
    } else {
      val hours = Math.ceil((millisRemaining / 1000L / 60L).toDouble() / 60.0).toInt()
      timeTillNextTimerUpdate = (60000 * 60) - millisRemaining % (60000 * 60)
      res = "${hours}h"
    }

    text = res
    if (!textUpdateScheduled) {
      textUpdateScheduled = true
      handler.sendMessageDelayed(Message.obtain(handler, 1), timeTillNextTimerUpdate)
    }
  }

  private var invalidateScheduled = false
  private var textUpdateScheduled = false

  override fun onDraw (c: Canvas) {
    if (livePeriod == 0) {
      super.onDraw(c)
      return
    }

    val centerX = measuredWidth / 2
    val centerY = measuredHeight / 2

    val timerRadius = Screen.dp(12f)
    val color = Theme.progressColor()

    val millisRemaining = aliveExpiresAt - SystemClock.uptimeMillis()
    val doneFactor = clamp((millisRemaining.toDouble() / (livePeriod * 1000L).toDouble()).toFloat())
    val strokeSize = Screen.dp(1.5f)

    val degrees = (360f * doneFactor).toInt()
    if (degrees == 360) {
      c.drawCircle(centerX.toFloat(), centerY.toFloat(), timerRadius.toFloat(), Paints.getProgressPaint(color, strokeSize.toFloat()))
    } else {
      c.drawCircle(centerX.toFloat(), centerY.toFloat(), timerRadius.toFloat(), Paints.getProgressPaint(alphaColor(.25f, color), strokeSize.toFloat()))
      val rectF: RectF = Paints.getRectF()
      rectF.set((centerX - timerRadius).toFloat(), (centerY - timerRadius).toFloat(), (centerX + timerRadius).toFloat(), (centerY + timerRadius).toFloat())
      c.drawArc(rectF, modulo((-90 + (360 - degrees)), 360).toFloat(), degrees.toFloat(), false, Paints.getProgressPaint(color, strokeSize.toFloat()))
    }

    if (!invalidateScheduled) {
      val delay = U.calculateDelayForDiameter((timerRadius * 2).toLong(), livePeriod.toLong() * 1000L)
      invalidateScheduled = true
      handler.sendMessageDelayed(Message.obtain(handler, 0), delay)
    }

    super.onDraw(c)
  }
}
