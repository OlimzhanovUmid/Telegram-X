package org.thunderdog.challegram.charts

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint

import androidx.core.graphics.ColorUtils

import org.thunderdog.challegram.charts.data.ChartData
import org.thunderdog.challegram.charts.view_data.BarViewData

class BarChartView (context: Context) : BaseChartView<ChartData, BarViewData>(context) {

  init {
    superDraw = true
    useAlphaSignature = true
  }

  override fun drawChart (canvas: Canvas?) {
    if (chartData != null) {
      val fullWidth = (chartWidth / (pickerDelegate.pickerEnd - pickerDelegate.pickerStart))
      val offset = fullWidth * (pickerDelegate.pickerStart) - horizontalPadding

      var start = startXIndex - 1
      if (start < 0) start = 0
      var end = endXIndex + 1
      if (end > chartData!!.lines[0].y.size - 1)
        end = chartData!!.lines[0].y.size - 1

      canvas!!.save()
      canvas.clipRect(chartStart, 0, chartEnd, measuredHeight - chartBottom)


      var transitionAlpha = 1f
      canvas.save()
      if (transitionMode == TRANSITION_MODE_PARENT) {
        postTransition = true
        selectionA = 0f
        transitionAlpha = 1f - transitionParams!!.progress

        canvas.scale(
          1 + 2 * transitionParams!!.progress, 1f,
          transitionParams!!.pX, transitionParams!!.pY
        )

      } else if (transitionMode == TRANSITION_MODE_CHILD) {

        transitionAlpha = transitionParams!!.progress

        canvas.scale(
          transitionParams!!.progress, 1f,
          transitionParams!!.pX, transitionParams!!.pY
        )
      }


      for (k in 0 until lines!!.size) {
        val line = lines!![k]!!
        if (!line.enabled && line.alpha == 0f) continue

        val p: Float = if (chartData!!.xPercentage.size < 2) {
          1f
        } else {
          chartData!!.xPercentage[1] * fullWidth
        }
        val y = line.line.y
        var j = 0

        var selectedX = 0f
        var selectedY = 0f
        var selected = false
        val a = line.alpha
        for (i in start..end) {
          val xPoint = p / 2 + chartData!!.xPercentage[i] * fullWidth - offset
          val yPercentage = y[i] / currentMaxHeight * a

          val yPoint = measuredHeight - chartBottom - (yPercentage) * (measuredHeight - chartBottom - signatureTextHeight)

          if (i == selectedIndex && legendShowing) {
            selected = true
            selectedX = xPoint
            selectedY = yPoint
            continue
          }

          line.linesPath!![j++] = xPoint
          line.linesPath!![j++] = yPoint

          line.linesPath!![j++] = xPoint
          line.linesPath!![j++] = (measuredHeight - chartBottom).toFloat()
        }

        val paint: Paint = if (selected || postTransition) line.unselectedPaint else line.paint
        paint.strokeWidth = p


        if (line.canBlend()) {
          if (selected) line.unselectedPaint.color = ColorUtils.blendARGB(
            line.lineColor, line.blendColor, selectionA
          )

          if (postTransition) {
            line.unselectedPaint.color = ColorUtils.blendARGB(
              line.lineColor, line.blendColor, 0f
            )
          }
        }

        paint.alpha = (transitionAlpha * 255).toInt()
        canvas.drawLines(line.linesPath!!, 0, j, paint)

        if (selected) {
          line.paint.strokeWidth = p
          line.paint.alpha = (transitionAlpha * 255).toInt()
          canvas.drawLine(
            selectedX, selectedY,
            selectedX, (measuredHeight - chartBottom).toFloat(),
            line.paint
          )
          line.paint.alpha = 255
        }


      }

      canvas.restore()
      canvas.restore()
    }
  }

  override fun drawPickerChart (canvas: Canvas?) {
    val bottom = measuredHeight - pickerPadding
    val top = measuredHeight - pikerHeight - pickerPadding

    val nl = lines!!.size
    if (chartData != null) {
      for (k in 0 until nl) {
        val line = lines!![k]!!
        if (!line.enabled && line.alpha == 0f) continue

        line.bottomLinePath.reset()

        val n = chartData!!.xPercentage.size
        var j = 0

        val p: Float = if (chartData!!.xPercentage.size < 2) {
          1f
        } else {
          chartData!!.xPercentage[1] * pickerWidth
        }
        val y = line.line.y

        val a = line.alpha

        for (i in 0 until n) {
          if (y[i] < 0) continue
          val xPoint = chartData!!.xPercentage[i] * pickerWidth
          val h = if (ANIMATE_PICKER_SIZES) pickerMaxHeight else chartData!!.maxValue.toFloat()
          val yPercentage = y[i] / h * a
          val yPoint = (1f - yPercentage) * (bottom - top)

          line.linesPath!![j++] = xPoint
          line.linesPath!![j++] = yPoint

          line.linesPath!![j++] = xPoint
          line.linesPath!![j++] = (measuredHeight - chartBottom).toFloat()
        }

        line.paint.strokeWidth = p + 2
        canvas!!.drawLines(line.linesPath!!, 0, j, line.paint)
      }
    }
  }

  override fun drawSelection (canvas: Canvas) {

  }

  override fun createLineViewData (line: ChartData.Line?): BarViewData? {
    return BarViewData(line!!)
  }

  override fun onDraw (canvas: Canvas) {
    tick()
    drawChart(canvas)
    drawBottomLine(canvas)
    tmpN = horizontalLines.size
    tmpI = 0
    while (tmpI < tmpN) {
      drawHorizontalLines(canvas, horizontalLines[tmpI])
      drawSignaturesToHorizontalLines(canvas, horizontalLines[tmpI])
      tmpI++
    }
    drawBottomSignature(canvas)
    drawPicker(canvas)
    drawSelection(canvas)

    super.onDraw(canvas)
  }

  override val minDistance: Float
    get() = 0.1f
}
