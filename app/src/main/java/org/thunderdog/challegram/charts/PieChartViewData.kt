package org.thunderdog.challegram.charts

import android.animation.Animator
import org.thunderdog.challegram.charts.data.ChartData
import org.thunderdog.challegram.charts.view_data.StackLinearViewData

class PieChartViewData (line: ChartData.Line) : StackLinearViewData(line) {
  @JvmField var selectionA: Float = 0f
  @JvmField var drawingPart: Float = 0f
  @JvmField var animator: Animator? = null
}
