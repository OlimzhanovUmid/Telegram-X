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
import android.graphics.Canvas
import android.graphics.Path
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView
import org.thunderdog.challegram.navigation.HeaderView
import org.thunderdog.challegram.support.ViewSupport
import org.thunderdog.challegram.tool.Paints
import org.thunderdog.challegram.tool.Screen

class MediaOtherRecyclerView (context: Context) : RecyclerView(context) {
  private val path: Path? = null//Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ? new Path() : null;
  private var factor = 0f

  fun setFactor (factor: Float) {
    if (path != null) {
      if (this.factor != factor) {
        this.factor = factor
        layoutPath()
        invalidate()
      }
    } else {
      alpha = factor
    }
  }

  override fun onTouchEvent (e: MotionEvent): Boolean {
    if (alpha > 0f) {
      super.onTouchEvent(e)
      return true
    }
    return false
  }

  private fun layoutPath () {
    if (path != null) {
      val viewWidth = measuredWidth
      val fromCx = viewWidth - Screen.dp(78f) + Screen.dp(15f)
      val fromCy = Screen.dp(26f) + HeaderView.getTopOffset() + Screen.dp(15f)
      val fromRadius = Screen.dp(15f)

      val cx = fromCx + ((viewWidth - fromCx).toFloat() * factor).toInt()
      val cy = fromCy - (fromCy.toFloat() * factor).toInt()

      val width = fromRadius + ((viewWidth - fromRadius).toFloat() * factor).toInt()
      val height = fromRadius + ((measuredHeight - fromRadius).toFloat() * factor).toInt()
      val radius = fromRadius * (1f - factor)

      val rectF = Paints.getRectF()
      rectF.set((cx - width / 2).toFloat(), (cy - height / 2).toFloat(), (cx + width / 2).toFloat(), (cy + height / 2).toFloat())
      path.reset()
      path.addRoundRect(rectF, radius, radius, Path.Direction.CCW)
    }
  }

  override fun onMeasure (widthSpec: Int, heightSpec: Int) {
    super.onMeasure(widthSpec, heightSpec)
    if (path != null) {
      layoutPath()
    }
  }

  override fun draw (c: Canvas) {
    val needClip = factor < 1f
    val saveCount = if (needClip) ViewSupport.clipPath(c, path) else Int.MIN_VALUE

    super.draw(c)

    if (needClip) {
      ViewSupport.restoreClipPath(c, saveCount)
    }
  }
}
