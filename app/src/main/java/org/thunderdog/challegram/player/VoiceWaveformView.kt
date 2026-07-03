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
 * File created on 01/07/2024
 */
package org.thunderdog.challegram.player

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import android.view.animation.AnticipateOvershootInterpolator

import org.thunderdog.challegram.N
import org.thunderdog.challegram.component.chat.Waveform
import org.thunderdog.challegram.core.Background
import org.thunderdog.challegram.data.TGRecord
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Fonts
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.tool.Strings
import org.thunderdog.challegram.tool.UI

import me.vkryl.android.animator.FactorAnimator

class VoiceWaveformView (context: Context) : View(context), FactorAnimator.Target {
  private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
    typeface = Fonts.getRobotoRegular()
    textSize = Screen.dp(15f).toFloat()
  }
  private val textOffset = Screen.dp(5f)
  private val textRight = Screen.dp(39f)
  private val waveLeft = Screen.dp(10f)

  private val waveform = Waveform(null, Waveform.MODE_RECT, false)

  fun clearData () {
    waveform.setData(null)
    invalidate()
  }

  private var record: TGRecord? = null
  private var seekStr: String? = null

  fun processRecord (record: TGRecord) {
    this.record = record
    this.seekStr = Strings.buildDuration(record.getDuration().toLong())

    Background.instance().post {
      val waveform = if (record.getWaveform() != null) record.getWaveform() else N.getWaveform(record.getPath())
      if (waveform != null) {
        UI.post { setWaveform(record, waveform) }
      }
    }
  }

  private val waveformAnimator = FactorAnimator(0, this, overshoot, OPEN_DURATION)

  private fun setWaveform (record: TGRecord, waveform: ByteArray) {
    if (this.record == null || this.record != record) {
      return
    }
    record.setWaveform(waveform)
    this.waveform.setData(waveform)
    waveformAnimator.forceFactor(0f)
    waveformAnimator.setStartDelay(80L)
    waveformAnimator.animateTo(1f)
    invalidate()
  }

  override fun onFactorChanged (id: Int, factor: Float, fraction: Float, callee: FactorAnimator?) {
    waveform.setExpand(factor)
    invalidate()
  }

  private fun calculateWaveformWidth (): Int {
    return measuredWidth - waveLeft - Screen.dp(110f) + Screen.dp(55f)
  }

  override fun onMeasure (widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    if (measuredWidth != 0) {
      waveform.layout(calculateWaveformWidth())
    }
  }

  override fun onDraw (c: Canvas) {
    val width = measuredWidth
    val height = measuredHeight
    val centerY = (height.toFloat() * .5f).toInt()

    if (seekStr != null) {
      textPaint.color = Theme.textAccentColor()
      c.drawText(seekStr!!, (width - textRight).toFloat(), (centerY + textOffset).toFloat(), textPaint)
    }

    waveform.draw(c, 1f, waveLeft, centerY)
  }

  companion object {
    private val overshoot = AnticipateOvershootInterpolator(3.0f)
    private const val OPEN_DURATION = 350L
  }
}
