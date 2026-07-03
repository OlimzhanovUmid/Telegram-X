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
 * File created on 22/10/2016
 */
package org.thunderdog.challegram.component.attach

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.view.View

import org.thunderdog.challegram.R
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Drawables
import org.thunderdog.challegram.tool.Paints
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.widget.ProgressComponent

import me.vkryl.android.animator.BoolAnimator
import me.vkryl.android.animator.FactorAnimator
import me.vkryl.android.DECELERATE_INTERPOLATOR

class MediaLocationPointView (context: Context) : View(context), FactorAnimator.Target {
  companion object {
    private const val ANIMATOR_GPS = 0
    private const val ANIMATOR_CUSTOM = 1
    private const val ANIMATOR_PLACE = 2
  }

  private var initializationTime = SystemClock.uptimeMillis()
  private val gpsLocated = BoolAnimator(ANIMATOR_GPS, this, DECELERATE_INTERPOLATOR, 180L)
  private val isCustom = BoolAnimator(ANIMATOR_CUSTOM, this, DECELERATE_INTERPOLATOR, 180L)
  private val isPlace = BoolAnimator(ANIMATOR_PLACE, this, DECELERATE_INTERPOLATOR, 180L)

  private val progressComponent = ProgressComponent.simpleInstance(this, 5f, Screen.dp(20f), Screen.dp(8f), Screen.dp(40f), Screen.dp(40f))

  private val locationIcon: Drawable = Drawables.get(resources, R.drawable.baseline_location_on_24)
  private val sendIcon: Drawable = Drawables.get(resources, R.drawable.deproko_baseline_send_24)

  init {
    progressComponent.setAlpha(1f)
  }

  private fun needAnimation (): Boolean {
    if (initializationTime == 0L) {
      return true
    }
    if (SystemClock.uptimeMillis() - initializationTime >= 100L) {
      initializationTime = 0
      return true
    }
    return false
  }

  fun setShowProgress (show: Boolean) {
    this.gpsLocated.setValue(!show, needAnimation())
  }

  fun setIsCustom (isCustom: Boolean) {
    this.isCustom.setValue(isCustom, needAnimation())
  }

  fun setIsPlace (isPlace: Boolean) {
    this.isPlace.setValue(isPlace, true)
  }

  override fun onFactorChanged (id: Int, factor: Float, fraction: Float, callee: FactorAnimator?) {
    progressComponent.setAlpha(1f - Math.max(gpsLocated.getFloatValue(), Math.max(isCustom.getFloatValue(), isPlace.getFloatValue())))
    invalidate()
  }

  override fun onFactorChangeFinished (id: Int, finalFactor: Float, callee: FactorAnimator?) { }

  override fun onDraw (c: Canvas) {
    val cx = (paddingLeft + (measuredWidth - paddingLeft - paddingRight) / 2).toFloat()
    val cy = (paddingTop + (measuredHeight - paddingTop - paddingBottom) / 2).toFloat()

    val gpsFactor = gpsLocated.getFloatValue()
    val customFactor = isCustom.getFloatValue()
    val placeFactor = isPlace.getFloatValue()
    val activeFactor = Math.max(gpsFactor, customFactor)

    val color = Theme.getColor(ColorId.file)

    c.drawCircle(cx, cy, Screen.dp(20f).toFloat(), Paints.fillingPaint(color))
    progressComponent.draw(c)
    if (activeFactor > 0f && placeFactor < 1f) {
      val paint = Paints.whitePorterDuffPaint()
      paint.setAlpha((255f * activeFactor * (1f - placeFactor)).toInt())
      Drawables.draw(c, locationIcon, cx - locationIcon.minimumWidth / 2, cy - locationIcon.minimumHeight / 2, paint)
      paint.setAlpha(255)
    }
    if (placeFactor > 0f) {
      val paint = Paints.whitePorterDuffPaint()
      paint.setAlpha((255f * placeFactor).toInt())
      c.save()
      c.scale(.7f, .7f, cx, cy)
      Drawables.draw(c, sendIcon, cx + Screen.dp(2f) - sendIcon.minimumWidth / 2, cy - sendIcon.minimumHeight / 2, paint)
      c.restore()
      paint.setAlpha(255)
    }
  }
}
