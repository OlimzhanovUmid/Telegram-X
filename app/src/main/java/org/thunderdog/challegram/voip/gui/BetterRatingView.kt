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
 * File created on 10/06/2017
 */
package org.thunderdog.challegram.voip.gui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.view.View
import org.thunderdog.challegram.R
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Drawables
import org.thunderdog.challegram.tool.Paints
import org.thunderdog.challegram.tool.Screen

class BetterRatingView(context: Context) : View(context) {
  private val paint = Paint()
  private val filledStar: Drawable
  private val hollowStar: Drawable
  private var numStars = 5
  private var selectedRating = 0
  private var listener: OnRatingChangeListener? = null

  init {
    filledStar = Drawables.get(R.drawable.baseline_star_24)
    hollowStar = Drawables.get(R.drawable.baseline_star_border_24)
  }

  override fun onMeasure (widthMeasureSpec: Int, heightMeasureSpec: Int) {
    setMeasuredDimension((numStars * Screen.dp(32f) + (numStars - 1) * Screen.dp(16f)).toInt(), Screen.dp(32f))
  }

  override fun onDraw (c: Canvas) {
    for (i in 0 until numStars) {
      paint.color = Theme.iconColor()
      Drawables.draw(c, if (i < selectedRating) filledStar else hollowStar, (i * Screen.dp(32f + 16f)).toFloat(), 0f, Paints.getIconGrayPorterDuffPaint())
    }
  }

  override fun onTouchEvent (event: MotionEvent): Boolean {
    var offset = Screen.dp(-8f).toFloat()
    for (i in 0 until numStars) {
      if (event.x > offset && event.x < offset + Screen.dp(32f + 16f).toFloat()) {
        if (selectedRating != i + 1) {
          selectedRating = i + 1
          listener?.onRatingChanged(selectedRating)
          invalidate()
          break
        }
      }
      offset += Screen.dp(32f + 16f).toFloat()
    }
    return true
  }

  fun getRating (): Int = selectedRating

  fun setOnRatingChangeListener (l: OnRatingChangeListener?) {
    listener = l
  }

  interface OnRatingChangeListener {
    fun onRatingChanged (newRating: Int)
  }
}
