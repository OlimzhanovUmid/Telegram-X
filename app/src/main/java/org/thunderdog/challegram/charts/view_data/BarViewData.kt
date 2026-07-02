package org.thunderdog.challegram.charts.view_data

import android.graphics.Paint

import androidx.core.graphics.ColorUtils

import org.thunderdog.challegram.Log
import org.thunderdog.challegram.charts.data.ChartData
import org.thunderdog.challegram.theme.Theme

class BarViewData (line: ChartData.Line) : LineViewData(line) {

  @JvmField val unselectedPaint = Paint()

  @JvmField var blendColor = 0

  init {
    paint.style = Paint.Style.STROKE
    unselectedPaint.style = Paint.Style.STROKE
    paint.isAntiAlias = false
  }

  override fun updateColors () {
    super.updateColors()
    blendColor = ColorUtils.blendARGB(Theme.fillingColor(), lineColor, 0.3f) // key_windowBackgroundWhite
  }

  fun canBlend (): Boolean {
    if (blendColor == 0) {
      updateColors()
      if (blendColor == 0)
        Log.e("blendColor is empty", Log.generateException())
    }
    return blendColor != 0
  }
}
