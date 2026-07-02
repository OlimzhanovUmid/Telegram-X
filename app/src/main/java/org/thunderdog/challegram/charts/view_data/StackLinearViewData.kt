package org.thunderdog.challegram.charts.view_data

import android.graphics.Paint

import org.thunderdog.challegram.charts.BaseChartView
import org.thunderdog.challegram.charts.data.ChartData

open class StackLinearViewData (line: ChartData.Line) : LineViewData(line) {

  init {
    paint.style = Paint.Style.FILL
    if (BaseChartView.USE_LINES) {
      paint.isAntiAlias = false
    }
  }
}
