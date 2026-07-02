package org.thunderdog.challegram.charts.view_data

import android.animation.ValueAnimator
import android.graphics.Paint
import android.graphics.Path

import androidx.core.graphics.ColorUtils

import org.thunderdog.challegram.charts.BaseChartView
import org.thunderdog.challegram.charts.data.ChartData
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Screen

open class LineViewData (@JvmField val line: ChartData.Line) {

  @JvmField var bottomLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
  @JvmField var paint = Paint(Paint.ANTI_ALIAS_FLAG)
  @JvmField var selectionPaint = Paint(Paint.ANTI_ALIAS_FLAG)

  @JvmField var bottomLinePath = Path()
  @JvmField var chartPath = Path()
  @JvmField var chartPathPicker = Path()
  @JvmField var animatorIn: ValueAnimator? = null
  @JvmField var animatorOut: ValueAnimator? = null
  @JvmField var linesPathBottomSize: Int = 0

  @JvmField var linesPath: FloatArray? = null
  @JvmField var linesPathBottom: FloatArray? = null

  @JvmField var lineColor: Int = 0

  @JvmField var enabled = true

  @JvmField var alpha = 1f

  init {
    paint.strokeWidth = Screen.dpf(2f)
    paint.style = Paint.Style.STROKE
    if (!BaseChartView.USE_LINES) {
      paint.strokeJoin = Paint.Join.ROUND
    }
    paint.color = line.color

    bottomLinePaint.strokeWidth = Screen.dpf(1f)
    bottomLinePaint.style = Paint.Style.STROKE
    bottomLinePaint.color = line.color

    selectionPaint.strokeWidth = Screen.dpf(10f)
    selectionPaint.style = Paint.Style.STROKE
    selectionPaint.strokeCap = Paint.Cap.ROUND
    selectionPaint.color = line.color

    linesPath = FloatArray(line.y.size shl 2)
    linesPathBottom = FloatArray(line.y.size shl 2)
  }

  open fun updateColors () {
    /*TODO if (line.colorKey != null && Theme.hasThemeKey(line.colorKey)) {
        lineColor = Theme.getColor(line.colorKey);
    } else {*/
    val color = Theme.fillingColor() // key_windowBackgroundWhite
    val darkBackground = ColorUtils.calculateLuminance(color) < 0.5f
    lineColor = if (darkBackground) line.colorDark else line.color
    //}
    paint.color = lineColor
    bottomLinePaint.color = lineColor
    selectionPaint.color = lineColor
  }
}
