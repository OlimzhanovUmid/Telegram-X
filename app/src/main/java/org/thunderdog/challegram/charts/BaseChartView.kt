package org.thunderdog.challegram.charts

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.text.TextPaint
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withClip
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import me.vkryl.core.alphaColor
import org.thunderdog.challegram.U
import org.thunderdog.challegram.charts.data.ChartData
import org.thunderdog.challegram.charts.view_data.*
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.theme.ThemeInvalidateListener
import org.thunderdog.challegram.tool.Screen
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

abstract class BaseChartView<T : ChartData?, L : LineViewData?>(context: Context) : View(context), ChartPickerDelegate.Listener, ThemeInvalidateListener {
  @JvmField
  var sharedUiComponents: SharedUiComponents? = null

  @JvmField
  var horizontalLines: ArrayList<ChartHorizontalLinesData> = ArrayList<ChartHorizontalLinesData>(10)
  var bottomSignatureDate: ArrayList<ChartBottomSignatureData> = ArrayList<ChartBottomSignatureData>(25)

  @JvmField
  var lines: ArrayList<L?>? = ArrayList<L?>()

  private val ANIM_DURATION = 400
  private val SELECTED_LINE_WIDTH = Screen.dpf(1.5f)
  private val SIGNATURE_TEXT_SIZE = Screen.dpf(12f)
  private val BOTTOM_SIGNATURE_TEXT_HEIGHT = Screen.dp(14f)
  private val PICKER_CAPTURE_WIDTH = Screen.dp(24f)
  private val BOTTOM_SIGNATURE_OFFSET = Screen.dp(10f)

  private val DP_12 = Screen.dp(12f)
  private val DP_6 = Screen.dp(6f)
  private val DP_5 = Screen.dp(5f)
  private val DP_2 = Screen.dp(2f)
  private val DP_1 = Screen.dp(1f)

  @JvmField
  protected var drawPointOnSelection: Boolean = true

  @JvmField
  var signaturePaintAlpha: Float = 0f
  var bottomSignaturePaintAlpha: Float = 0f
  var hintLinePaintAlpha: Int = 0

  @JvmField
  var chartActiveLineAlpha: Int = 0

  @JvmField
  var chartBottom: Int = 0

  @JvmField
  var currentMaxHeight: Float = 250f

  @JvmField
  var currentMinHeight: Float = 0f

  var animateToMaxHeight: Float = 0f
  var animateToMinHeight: Float = 0f


  var thresholdMaxHeight: Float = 0f

  @JvmField
  var startXIndex: Int = 0

  @JvmField
  var endXIndex: Int = 0

  @JvmField
  var invalidatePickerChart: Boolean = true

  @JvmField
  var enabled: Boolean = true


  var emptyPaint: Paint = Paint()

  @JvmField
  var linePaint: Paint = Paint()

  @JvmField
  var selectedLinePaint: Paint = Paint()

  @JvmField
  var signaturePaint: Paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG)

  @JvmField
  var signaturePaint2: Paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG)
  var bottomSignaturePaint: Paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG)
  var pickerSelectorPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
  var unactiveBottomChartPaint: Paint = Paint()

  @JvmField
  var selectionBackgroundPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
  var ripplePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
  var whiteLinePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

  var pickerRect: Rect = Rect()
  var pathTmp: Path = Path()

  var maxValueAnimator: Animator? = null

  var alphaAnimator: ValueAnimator? = null
  var alphaBottomAnimator: ValueAnimator? = null

  @JvmField
  var pickerAnimator: Animator? = null
  var selectionAnimator: ValueAnimator? = null

  @JvmField
  var postTransition: Boolean = false

  @JvmField
  var pickerDelegate: ChartPickerDelegate = ChartPickerDelegate(this)

  @JvmField
  var chartData: T? = null

  var currentBottomSignatures: ChartBottomSignatureData? = null

  @JvmField
  protected var pickerMaxHeight: Float = 0f

  @JvmField
  protected var pickerMinHeight: Float = 0f

  @JvmField
  protected var animatedToPickerMaxHeight: Float = 0f
  protected var animatedToPickerMinHeight: Float = 0f

  @JvmField
  protected var tmpN: Int = 0

  @JvmField
  protected var tmpI: Int = 0
  protected var bottomSignatureOffset: Int = 0

  private var bottomChartBitmap: Bitmap? = null
  private var bottomChartCanvas: Canvas? = null

  protected var chartCaptured: Boolean = false

  @JvmField
  protected var selectedIndex: Int = -1

  @JvmField
  protected var selectedCoordinate: Float = -1f

  @JvmField
  var legendSignatureView: LegendSignatureView? = null

  @JvmField
  var legendShowing: Boolean = false

  @JvmField
  var selectionA: Float = 0f

  @JvmField
  var superDraw: Boolean = false

  @JvmField
  var useAlphaSignature: Boolean = false

  @JvmField
  var transitionMode: Int = TRANSITION_MODE_NONE

  @JvmField
  var transitionParams: TransitionParams? = null

  private val touchSlop: Int

  @JvmField
  var pikerHeight: Int = Screen.dp(46f)

  @JvmField
  var pickerWidth: Int = 0

  @JvmField
  var chartStart: Int = 0

  @JvmField
  var chartEnd: Int = 0

  @JvmField
  var chartWidth: Int = 0

  @JvmField
  var chartFullWidth: Float = 0f

  @JvmField
  var chartArea: Rect = Rect()

  private val pickerHeightUpdateListener: AnimatorUpdateListener = AnimatorUpdateListener { animation ->
    pickerMaxHeight = animation.getAnimatedValue() as Float
    invalidatePickerChart = true
    invalidate()
  }

  private val pickerMinHeightUpdateListener: AnimatorUpdateListener = AnimatorUpdateListener { animation ->
    pickerMinHeight = animation.getAnimatedValue() as Float
    invalidatePickerChart = true
    invalidate()
  }

  private val heightUpdateListener = AnimatorUpdateListener { animation: ValueAnimator? ->
    currentMaxHeight = (animation!!.getAnimatedValue() as Float)
    invalidate()
  }

  private val minHeightUpdateListener = AnimatorUpdateListener { animation: ValueAnimator? ->
    currentMinHeight = (animation!!.getAnimatedValue() as Float)
    invalidate()
  }
  private val selectionAnimatorListener: AnimatorUpdateListener = AnimatorUpdateListener { animation ->
    selectionA = animation.getAnimatedValue() as Float
    legendSignatureView!!.setAlpha(selectionA)
    invalidate()
  }
  private val selectorAnimatorEndListener: Animator.AnimatorListener = object : AnimatorListenerAdapter() {
    override fun onAnimationEnd(animation: Animator) {
      super.onAnimationEnd(animation)
      if (!animateLegentTo) {
        legendShowing = false
        legendSignatureView!!.visibility = GONE
        invalidate()
      }

      postTransition = false
    }
  }

  @JvmField
  protected var useMinHeight: Boolean = false

  @JvmField
  protected var dateSelectionListener: DateSelectionListener? = null
  private var startFromMax = 0f
  private var startFromMin = 0f
  private var startFromMaxH = 0f
  private var startFromMinH = 0f
  private var minMaxUpdateStep = 0f

  protected open fun init() {
    linePaint.strokeWidth = LINE_WIDTH
    selectedLinePaint.strokeWidth = SELECTED_LINE_WIDTH

    signaturePaint.textSize = SIGNATURE_TEXT_SIZE
    signaturePaint2.textSize = SIGNATURE_TEXT_SIZE
    signaturePaint2.textAlign = Paint.Align.RIGHT
    bottomSignaturePaint.textSize = SIGNATURE_TEXT_SIZE
    bottomSignaturePaint.textAlign = Paint.Align.CENTER

    selectionBackgroundPaint.strokeWidth = Screen.dpf(6f)
    selectionBackgroundPaint.strokeCap = Paint.Cap.ROUND

    setLayerType(LAYER_TYPE_HARDWARE, null)
    setWillNotDraw(false)

    legendSignatureView = createLegendView()


    legendSignatureView!!.visibility = GONE

    whiteLinePaint.setColor(Color.WHITE)
    whiteLinePaint.strokeWidth = Screen.dpf(3f)
    whiteLinePaint.strokeCap = Paint.Cap.ROUND

    updateColors()
  }

  protected open fun createLegendView(): LegendSignatureView {
    return LegendSignatureView(context)
  }

  override fun onThemeInvalidate(isTempUpdate: Boolean) {
    updateColors()
    invalidate()
  }

  fun updateColors() {
    if (useAlphaSignature) {
      signaturePaint.setColor(Theme.textDecentColor()) // TODO key_statisticChartSignatureAlpha
    } else {
      signaturePaint.setColor(Theme.textDecentColor()) // Theme.key_statisticChartSignature
    }

    if (sharedUiComponents != null) {
      sharedUiComponents!!.invalidate()
    }

    bottomSignaturePaint.setColor(Theme.textDecentColor()) // Theme.key_statisticChartSignature
    linePaint.setColor(Theme.separatorColor()) // Theme.key_statisticChartHintLine
    selectedLinePaint.setColor(Theme.separatorColor()) // TODO key_statisticChartActiveLine
    pickerSelectorPaint.setColor(Theme.getColor(ColorId.fillingPositive)) // TODO key_statisticChartActivePickerChart
    unactiveBottomChartPaint.setColor(
      alphaColor(
        .5f,
        ColorUtils.blendARGB(Theme.getColor(ColorId.fillingPositive), Theme.fillingColor(), .6f)
      )
    ) // TODO key_statisticChartInactivePickerChart
    selectionBackgroundPaint.setColor(Theme.fillingColor()) // Theme.key_windowBackgroundWhite
    ripplePaint.setColor(alphaColor(.2f, Theme.getColor(ColorId.fillingPositive))) // Theme.key_statisticChartRipple
    legendSignatureView!!.recolor()

    hintLinePaintAlpha = linePaint.alpha
    chartActiveLineAlpha = selectedLinePaint.alpha
    signaturePaintAlpha = signaturePaint.alpha / 255f
    bottomSignaturePaintAlpha = bottomSignaturePaint.alpha / 255f


    for (l in lines!!) {
      l!!.updateColors()
    }

    if (legendShowing && selectedIndex < chartData!!.x.size) {
      legendSignatureView!!.setData(selectedIndex, chartData!!.x[selectedIndex], lines as ArrayList<LineViewData?>?, false)
    }

    invalidatePickerChart = true
  }

  var lastW: Int = 0
  var lastH: Int = 0

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    setMeasuredDimension(
      MeasureSpec.getSize(widthMeasureSpec),
      MeasureSpec.getSize(heightMeasureSpec) // FIXME? AndroidUtilities.displaySize.y - Screen.dp(56)
    )


    if (measuredWidth != lastW || measuredHeight != lastH) {
      lastW = measuredWidth
      lastH = measuredHeight
      bottomChartBitmap = createBitmap(measuredWidth - (horizontalPadding shl 1), pikerHeight, Bitmap.Config.ARGB_4444)
      bottomChartCanvas = Canvas(bottomChartBitmap!!)

      sharedUiComponents!!.getPickerMaskBitmap(pikerHeight, measuredWidth - horizontalPadding * 2)
      measureSizes()

      if (legendShowing) moveLegend(chartFullWidth * (pickerDelegate.pickerStart) - horizontalPadding)

      onPickerDataChanged(animated = false, force = true, useAniamtor = false)
    }
  }


  private fun measureSizes() {
    if (measuredHeight <= 0 || measuredWidth <= 0) {
      return
    }
    pickerWidth = measuredWidth - (horizontalPadding * 2)
    chartStart = horizontalPadding
    chartEnd = measuredWidth - horizontalPadding
    chartWidth = chartEnd - chartStart
    chartFullWidth = (chartWidth / (pickerDelegate.pickerEnd - pickerDelegate.pickerStart))

    updateLineSignature()
    chartBottom = Screen.dp(100f)
    chartArea.set(chartStart - horizontalPadding, 0, chartEnd + horizontalPadding, measuredHeight - chartBottom)

    if (chartData != null) {
      bottomSignatureOffset = (Screen.dp(20f) / (pickerWidth.toFloat() / chartData!!.x.size)).toInt()
    }
    measureHeightThreshold()
  }

  private fun measureHeightThreshold() {
    val chartHeight = measuredHeight - chartBottom
    if (animateToMaxHeight == 0f || chartHeight == 0) return
    thresholdMaxHeight = (animateToMaxHeight / chartHeight) * SIGNATURE_TEXT_SIZE
  }


  protected open fun drawPickerChart(canvas: Canvas?) {
  }


  override fun onDraw(canvas: Canvas) {
    if (superDraw) {
      super.onDraw(canvas)
      return
    }
    tick()
    canvas.withClip(0, chartArea.top, measuredWidth, chartArea.bottom) {
      drawBottomLine(canvas)
      tmpN = horizontalLines.size
      tmpI = 0
      while (tmpI < tmpN) {
        drawHorizontalLines(canvas, horizontalLines[tmpI])
        tmpI++
      }

      drawChart(canvas)

      tmpI = 0
      while (tmpI < tmpN) {
        drawSignaturesToHorizontalLines(canvas, horizontalLines[tmpI])
        tmpI++
      }

    }
    drawBottomSignature(canvas)

    drawPicker(canvas)
    drawSelection(canvas)

    super.onDraw(canvas)
  }

  protected fun tick() {
    if (minMaxUpdateStep == 0f) {
      return
    }
    if (currentMaxHeight != animateToMaxHeight) {
      startFromMax += minMaxUpdateStep
      if (startFromMax > 1) {
        startFromMax = 1f
        currentMaxHeight = animateToMaxHeight
      } else {
        currentMaxHeight = startFromMaxH + (animateToMaxHeight - startFromMaxH) * CubicBezierInterpolator.EASE_OUT.getInterpolation(startFromMax)
      }
      invalidate()
    }
    if (useMinHeight) {
      if (currentMinHeight != animateToMinHeight) {
        startFromMin += minMaxUpdateStep
        if (startFromMin > 1) {
          startFromMin = 1f
          currentMinHeight = animateToMinHeight
        } else {
          currentMinHeight = startFromMinH + (animateToMinHeight - startFromMinH) * CubicBezierInterpolator.EASE_OUT.getInterpolation(startFromMin)
        }
        invalidate()
      }
    }
  }


  open fun drawBottomSignature(canvas: Canvas) {
    if (chartData == null) return

    tmpN = bottomSignatureDate.size

    var transitionAlpha = 1f
    when (transitionMode) {
        TRANSITION_MODE_PARENT -> {
          transitionAlpha = 1f - transitionParams!!.progress
        }
        TRANSITION_MODE_CHILD -> {
          transitionAlpha = transitionParams!!.progress
        }
        TRANSITION_MODE_ALPHA_ENTER -> {
          transitionAlpha = transitionParams!!.progress
        }
    }

    tmpI = 0
    while (tmpI < tmpN) {
      val resultAlpha = bottomSignatureDate[tmpI].alpha
      var step = bottomSignatureDate[tmpI].step
      if (step == 0) step = 1

      var start = startXIndex - bottomSignatureOffset
      while (start % step != 0) {
        start--
      }

      var end = endXIndex - bottomSignatureOffset
      while (end % step != 0 || end < chartData!!.x.size - 1) {
        end++
      }

      start += bottomSignatureOffset
      end += bottomSignatureOffset


      val offset: Float = chartFullWidth * (pickerDelegate.pickerStart) - horizontalPadding

      var i = start
      while (i < end) {
        if (i < 0 || i >= chartData!!.x.size - 1) {
          i += step
          continue
        }
        val xPercentage = (chartData!!.x[i] - chartData!!.x[0]).toFloat() / ((chartData!!.x[chartData!!.x.size - 1] - chartData!!.x[0])).toFloat()
        val xPoint = xPercentage * chartFullWidth - offset
        val xPointOffset = xPoint - BOTTOM_SIGNATURE_OFFSET
        if (xPointOffset > 0 &&
          xPointOffset <= chartWidth + horizontalPadding
        ) {
          if (xPointOffset < bottomSignatureStartAlpha) {
            val a: Float = 1f - (bottomSignatureStartAlpha - xPointOffset) / bottomSignatureStartAlpha
            bottomSignaturePaint.setAlpha((resultAlpha * a * bottomSignaturePaintAlpha * transitionAlpha).toInt())
          } else if (xPointOffset > chartWidth) {
            val a: Float = 1f - (xPointOffset - chartWidth) / horizontalPadding
            bottomSignaturePaint.setAlpha((resultAlpha * a * bottomSignaturePaintAlpha * transitionAlpha).toInt())
          } else {
            bottomSignaturePaint.setAlpha((resultAlpha * bottomSignaturePaintAlpha * transitionAlpha).toInt())
          }
          canvas.drawText(
            chartData!!.getDayString(i),
            xPoint,
            (measuredHeight - chartBottom + BOTTOM_SIGNATURE_TEXT_HEIGHT + Screen.dp(3f)).toFloat(),
            bottomSignaturePaint
          )
        }
        i += step
      }
      tmpI++
    }
  }

  protected open fun drawBottomLine(canvas: Canvas) {
    if (chartData == null) {
      return
    }
    var transitionAlpha = 1f
    when (transitionMode) {
        TRANSITION_MODE_PARENT -> {
          transitionAlpha = 1f - transitionParams!!.progress
        }
        TRANSITION_MODE_CHILD -> {
          transitionAlpha = transitionParams!!.progress
        }
        TRANSITION_MODE_ALPHA_ENTER -> {
          transitionAlpha = transitionParams!!.progress
        }
    }

    linePaint.setAlpha((hintLinePaintAlpha * transitionAlpha).toInt())
    signaturePaint.setAlpha((255 * signaturePaintAlpha * transitionAlpha).toInt())
    val textOffset = (signatureTextHeight - signaturePaint.textSize).toInt()
    val y = (measuredHeight - chartBottom - 1)
    canvas.drawLine(
      chartStart.toFloat(),
      y.toFloat(),
      chartEnd.toFloat(),
      y.toFloat(),
      linePaint
    )
    if (useMinHeight) return

    canvas.drawText("0", horizontalPadding.toFloat(), (y - textOffset).toFloat(), signaturePaint)
  }

  protected open fun drawSelection(canvas: Canvas) {
    if (selectedIndex < 0 || !legendShowing || chartData == null) return

    val alpha = (chartActiveLineAlpha * selectionA).toInt()


    val fullWidth = (chartWidth / (pickerDelegate.pickerEnd - pickerDelegate.pickerStart))
    val offset: Float = fullWidth * (pickerDelegate.pickerStart) - horizontalPadding

    val xPoint: Float
    if (selectedIndex < chartData!!.xPercentage.size) {
      xPoint = chartData!!.xPercentage[selectedIndex] * fullWidth - offset
    } else {
      return
    }

    selectedLinePaint.setAlpha(alpha)
    canvas.drawLine(xPoint, 0f, xPoint, chartArea.bottom.toFloat(), selectedLinePaint)

    if (drawPointOnSelection) {
      tmpN = lines!!.size
      tmpI = 0
      while (tmpI < tmpN) {
        val line: LineViewData = lines!![tmpI]!!
        if (!line.enabled && line.alpha == 0f) {
          tmpI++
          continue
        }
        val yPercentage = (line.line.y[selectedIndex] - currentMinHeight) / (currentMaxHeight - currentMinHeight)
        val yPoint: Float = measuredHeight - chartBottom - (yPercentage) * (measuredHeight - chartBottom - signatureTextHeight)

        line.selectionPaint.setAlpha((255 * line.alpha * selectionA).toInt())
        selectionBackgroundPaint.setAlpha((255 * line.alpha * selectionA).toInt())

        canvas.drawPoint(xPoint, yPoint, line.selectionPaint)
        canvas.drawPoint(xPoint, yPoint, selectionBackgroundPaint)
        tmpI++
      }
    }
  }

  protected open fun drawChart(canvas: Canvas?) {
  }

  protected open fun drawHorizontalLines(canvas: Canvas, a: ChartHorizontalLinesData) {
    val n = a.values.size

    var additionalOutAlpha = 1f
    if (n > 2) {
      val v = (a.values[1] - a.values[0]) / (currentMaxHeight - currentMinHeight)
      if (v < 0.1) {
        additionalOutAlpha = v / 0.1f
      }
    }

    var transitionAlpha = 1f
    when (transitionMode) {
        TRANSITION_MODE_PARENT -> {
          transitionAlpha = 1f - transitionParams!!.progress
        }
        TRANSITION_MODE_CHILD -> {
          transitionAlpha = transitionParams!!.progress
        }
        TRANSITION_MODE_ALPHA_ENTER -> {
          transitionAlpha = transitionParams!!.progress
        }
    }
    linePaint.setAlpha((a.alpha * (hintLinePaintAlpha / 255f) * transitionAlpha * additionalOutAlpha).toInt())
    signaturePaint.setAlpha((a.alpha * signaturePaintAlpha * transitionAlpha * additionalOutAlpha).toInt())
    val chartHeight: Int = measuredHeight - chartBottom - signatureTextHeight
    for (i in (if (useMinHeight) 0 else 1) until n) {
      val y = ((measuredHeight - chartBottom) - chartHeight * ((a.values[i] - currentMinHeight) / (currentMaxHeight - currentMinHeight))).toInt()
      canvas.drawRect(chartStart.toFloat(), y.toFloat(), chartEnd.toFloat(), (y + 1).toFloat(), linePaint)
    }
  }

  protected open fun drawSignaturesToHorizontalLines(canvas: Canvas, a: ChartHorizontalLinesData) {
    val n = a.values.size

    var additionalOutAlpha = 1f
    if (n > 2) {
      val v = (a.values[1] - a.values[0]) / (currentMaxHeight - currentMinHeight)
      if (v < 0.1) {
        additionalOutAlpha = v / 0.1f
      }
    }

    var transitionAlpha = 1f
    when (transitionMode) {
        TRANSITION_MODE_PARENT -> {
          transitionAlpha = 1f - transitionParams!!.progress
        }
        TRANSITION_MODE_CHILD -> {
          transitionAlpha = transitionParams!!.progress
        }
        TRANSITION_MODE_ALPHA_ENTER -> {
          transitionAlpha = transitionParams!!.progress
        }
    }
    linePaint.setAlpha((a.alpha * (hintLinePaintAlpha / 255f) * transitionAlpha * additionalOutAlpha).toInt())
    signaturePaint.setAlpha((a.alpha * signaturePaintAlpha * transitionAlpha * additionalOutAlpha).toInt())
    val chartHeight: Int = measuredHeight - chartBottom - signatureTextHeight

    val textOffset = (signatureTextHeight - signaturePaint.textSize).toInt()
    for (i in (if (useMinHeight) 0 else 1) until n) {
      val y = ((measuredHeight - chartBottom) - chartHeight * ((a.values[i] - currentMinHeight) / (currentMaxHeight - currentMinHeight))).toInt()
      canvas.drawText(a.valuesStr[i], horizontalPadding.toFloat(), (y - textOffset).toFloat(), signaturePaint)
    }
  }

  fun drawPicker(canvas: Canvas) {
    if (chartData == null) {
      return
    }
    pickerDelegate.pickerWidth = pickerWidth
    val bottom: Int = measuredHeight - pickerPadding
    val top: Int = measuredHeight - pikerHeight - pickerPadding

    var start = (horizontalPadding + pickerWidth * pickerDelegate.pickerStart).toInt()
    var end = (horizontalPadding + pickerWidth * pickerDelegate.pickerEnd).toInt()

    var transitionAlpha = 1f
    if (transitionMode == TRANSITION_MODE_CHILD) {
      val startParent = (horizontalPadding + pickerWidth * transitionParams!!.pickerStartOut).toInt()
      val endParent = (horizontalPadding + pickerWidth * transitionParams!!.pickerEndOut).toInt()

      start = (start + (startParent - start) * (1f - transitionParams!!.progress)).toInt()
      end = (end + (endParent - end) * (1f - transitionParams!!.progress)).toInt()
    } else if (transitionMode == TRANSITION_MODE_ALPHA_ENTER) {
      transitionAlpha = transitionParams!!.progress
    }

    if (chartData != null) {
      var instantDraw = false
      if (transitionMode == TRANSITION_MODE_NONE) {
        for (i in lines!!.indices) {
          val l = lines!![i]
          if ((l!!.animatorIn?.isRunning == true) || (l.animatorOut?.isRunning == true)) {
            instantDraw = true
            break
          }
        }
      }
      if (instantDraw) {
        canvas.withClip(
          horizontalPadding, measuredHeight - pickerPadding - pikerHeight,
          measuredWidth - horizontalPadding, measuredHeight - pickerPadding
        ) {
          translate(horizontalPadding.toFloat(), (measuredHeight - pickerPadding - pikerHeight).toFloat())
          drawPickerChart(this)
        }
      } else if (invalidatePickerChart) {
        bottomChartBitmap!!.eraseColor(0)
        drawPickerChart(bottomChartCanvas)
        invalidatePickerChart = false
      }
      if (!instantDraw) {
        when (transitionMode) {
            TRANSITION_MODE_PARENT -> {
              val pY = (top + (bottom - top) shr 1).toFloat()
              val pX: Float = horizontalPadding + pickerWidth * transitionParams!!.xPercentage

              emptyPaint.setAlpha(((1f - transitionParams!!.progress) * 255).toInt())

              canvas.withClip(horizontalPadding, top, measuredWidth - horizontalPadding, bottom) {
                scale(1 + 2 * transitionParams!!.progress, 1f, pX, pY)
                drawBitmap(
                  bottomChartBitmap!!,
                  horizontalPadding.toFloat(),
                  (measuredHeight - pickerPadding - pikerHeight).toFloat(),
                  emptyPaint
                )
              }
            }
            TRANSITION_MODE_CHILD -> {
              val pY = (top + (bottom - top) shr 1).toFloat()
              val pX: Float = horizontalPadding + pickerWidth * transitionParams!!.xPercentage

              val dX =
                (if (transitionParams!!.xPercentage > 0.5f) pickerWidth * transitionParams!!.xPercentage else pickerWidth * (1f - transitionParams!!.xPercentage)) * transitionParams!!.progress

              canvas.withClip(pX - dX, top.toFloat(), pX + dX, bottom.toFloat()) {
                emptyPaint.setAlpha((transitionParams!!.progress * 255).toInt())
                scale(transitionParams!!.progress, 1f, pX, pY)
                drawBitmap(
                  bottomChartBitmap!!,
                  horizontalPadding.toFloat(),
                  (measuredHeight - pickerPadding - pikerHeight).toFloat(),
                  emptyPaint
                )
              }
            }
            else -> {
              emptyPaint.setAlpha(((transitionAlpha) * 255).toInt())
              canvas.drawBitmap(
                bottomChartBitmap!!,
                horizontalPadding.toFloat(),
                (measuredHeight - pickerPadding - pikerHeight).toFloat(),
                emptyPaint
              )
            }
        }
      }


      if (transitionMode == TRANSITION_MODE_PARENT) {
        return
      }

      canvas.drawRect(
        horizontalPadding.toFloat(),
        top.toFloat(),
        (start + DP_12).toFloat(),
        bottom.toFloat(), unactiveBottomChartPaint
      )

      canvas.drawRect(
        (end - DP_12).toFloat(),
        top.toFloat(),
        (measuredWidth - horizontalPadding).toFloat(),
        bottom.toFloat(), unactiveBottomChartPaint
      )
    } else {
      canvas.drawRect(
        horizontalPadding.toFloat(),
        top.toFloat(),
        (measuredWidth - horizontalPadding).toFloat(),
        bottom.toFloat(), unactiveBottomChartPaint
      )
    }

    canvas.drawBitmap(
      sharedUiComponents!!.getPickerMaskBitmap(pikerHeight, measuredWidth - horizontalPadding * 2),
      horizontalPadding.toFloat(), (measuredHeight - pickerPadding - pikerHeight).toFloat(), emptyPaint
    )

    if (chartData != null) {
      pickerRect.set(
        start,
        top,
        end,
        bottom
      )


      pickerDelegate.middlePickerArea.set(pickerRect)


      canvas.drawPath(
        RoundedRect(
          pathTmp, pickerRect.left.toFloat(),
          (pickerRect.top - DP_1).toFloat(),
          (pickerRect.left + DP_12).toFloat(),
          (pickerRect.bottom + DP_1).toFloat(), DP_6.toFloat(), DP_6.toFloat(),
          tl = true, tr = false, br = false, bl = true
        ), pickerSelectorPaint
      )


      canvas.drawPath(
        RoundedRect(
          pathTmp, (pickerRect.right - DP_12).toFloat(),
          (pickerRect.top - DP_1).toFloat(), pickerRect.right.toFloat(),
          (pickerRect.bottom + DP_1).toFloat(), DP_6.toFloat(), DP_6.toFloat(),
          tl = false, tr = true, br = true, bl = false
        ), pickerSelectorPaint
      )

      canvas.drawRect(
        (pickerRect.left + DP_12).toFloat(),
        pickerRect.bottom.toFloat(), (pickerRect.right - DP_12).toFloat(),
        (pickerRect.bottom + DP_1).toFloat(), pickerSelectorPaint
      )

      canvas.drawRect(
        (pickerRect.left + DP_12).toFloat(),
        (pickerRect.top - DP_1).toFloat(), (pickerRect.right - DP_12).toFloat(),
        pickerRect.top.toFloat(), pickerSelectorPaint
      )


      canvas.drawLine(
        (pickerRect.left + DP_6).toFloat(), (pickerRect.centerY() - DP_6).toFloat(),
        (pickerRect.left + DP_6).toFloat(), (pickerRect.centerY() + DP_6).toFloat(), whiteLinePaint
      )

      canvas.drawLine(
        (pickerRect.right - DP_6).toFloat(), (pickerRect.centerY() - DP_6).toFloat(),
        (pickerRect.right - DP_6).toFloat(), (pickerRect.centerY() + DP_6).toFloat(), whiteLinePaint
      )


      val middleCap = pickerDelegate.getMiddleCaptured()

      val r = ((pickerRect.bottom - pickerRect.top) shr 1)
      val cY = pickerRect.top + r

      if (middleCap != null) {
        // canvas.drawCircle(pickerRect.left + ((pickerRect.right - pickerRect.left) >> 1), cY, r * middleCap.aValue + HORIZONTAL_PADDING, ripplePaint);
      } else {
        val lCap = pickerDelegate.getLeftCaptured()
        val rCap = pickerDelegate.getRightCaptured()

        if (lCap != null) canvas.drawCircle((pickerRect.left + DP_5).toFloat(), cY.toFloat(), r * lCap.aValue - DP_2, ripplePaint)
        if (rCap != null) canvas.drawCircle((pickerRect.right - DP_5).toFloat(), cY.toFloat(), r * rCap.aValue - DP_2, ripplePaint)
      }

      var cX = start
      pickerDelegate.leftPickerArea.set(
        cX - PICKER_CAPTURE_WIDTH,
        top,
        cX + (PICKER_CAPTURE_WIDTH shr 1),
        bottom
      )

      cX = end
      pickerDelegate.rightPickerArea.set(
        cX - (PICKER_CAPTURE_WIDTH shr 1),
        top,
        cX + PICKER_CAPTURE_WIDTH,
        bottom
      )
    }
  }


  var lastTime: Long = 0

  private fun setMaxMinValue(newMaxHeight: Int, newMinHeight: Int, animated: Boolean) {
    setMaxMinValue(newMaxHeight, newMinHeight, animated, force = false, useAnimator = false)
  }

  protected fun setMaxMinValue(newMaxHeight: Int, newMinHeight: Int, animated: Boolean, force: Boolean, useAnimator: Boolean) {
    var newMaxHeight = newMaxHeight
    var newMinHeight = newMinHeight
    var heightChanged = true
    if ((abs(ChartHorizontalLinesData.lookupHeight(newMaxHeight) - animateToMaxHeight) < thresholdMaxHeight) || newMaxHeight == 0) {
      heightChanged = false
    }

    if (!heightChanged && newMaxHeight.toFloat() == animateToMinHeight) return
    val newData = createHorizontalLinesData(newMaxHeight, newMinHeight)
    newMaxHeight = newData.values[newData.values.size - 1]
    newMinHeight = newData.values[0]


    if (!useAnimator) {
      var k = (currentMaxHeight - currentMinHeight) / (newMaxHeight - newMinHeight)
      if (k > 1f) {
        k = (newMaxHeight - newMinHeight) / (currentMaxHeight - currentMinHeight)
      }
      var s = 0.045f
      if (k > 0.7) {
        s = 0.1f
      } else if (k < 0.1) {
        s = 0.03f
      }

      var update = false
      if (newMaxHeight.toFloat() != animateToMaxHeight) {
        update = true
      }
      if (useMinHeight && newMinHeight.toFloat() != animateToMinHeight) {
        update = true
      }
      if (update) {
        if (maxValueAnimator != null) {
          maxValueAnimator!!.removeAllListeners()
          maxValueAnimator!!.cancel()
        }
        startFromMaxH = currentMaxHeight
        startFromMinH = currentMinHeight
        startFromMax = 0f
        startFromMin = 0f
        minMaxUpdateStep = s
      }
    }

    animateToMaxHeight = newMaxHeight.toFloat()
    animateToMinHeight = newMinHeight.toFloat()
    measureHeightThreshold()

    val t = System.currentTimeMillis()
    //  debounce
    if (t - lastTime < 320 && !force) {
      return
    }
    lastTime = t

    if (alphaAnimator != null) {
      alphaAnimator!!.removeAllListeners()
      alphaAnimator!!.cancel()
    }

    if (!animated) {
      currentMaxHeight = newMaxHeight.toFloat()
      currentMinHeight = newMinHeight.toFloat()
      horizontalLines.clear()
      horizontalLines.add(newData)
      newData.alpha = 255
      return
    }


    horizontalLines.add(newData)

    if (useAnimator) {
      if (maxValueAnimator != null) {
        maxValueAnimator!!.removeAllListeners()
        maxValueAnimator!!.cancel()
      }
      minMaxUpdateStep = 0f

      val animatorSet = AnimatorSet()
      animatorSet.playTogether(createAnimator(currentMaxHeight, newMaxHeight.toFloat(), heightUpdateListener))

      if (useMinHeight) {
        animatorSet.playTogether(createAnimator(currentMinHeight, newMinHeight.toFloat(), minHeightUpdateListener))
      }

      maxValueAnimator = animatorSet
      maxValueAnimator!!.start()
    }

    val n = horizontalLines.size
    for (i in 0..<n) {
      val a = horizontalLines[i]
      if (a !== newData) a.fixedAlpha = a.alpha
    }

    alphaAnimator = createAnimator(0f, 255f) { animation: ValueAnimator? ->
      newData.alpha = (animation!!.getAnimatedValue() as Float).toInt()
      for (a in horizontalLines) {
        if (a !== newData) a.alpha = ((a.fixedAlpha / 255f) * (255 - newData.alpha)).toInt()
      }
      invalidate()
    }
    alphaAnimator!!.addListener(object : AnimatorListenerAdapter() {
      override fun onAnimationEnd(animation: Animator) {
        horizontalLines.clear()
        horizontalLines.add(newData)
      }
    })

    alphaAnimator!!.start()
  }

  protected open fun createHorizontalLinesData(newMaxHeight: Int, newMinHeight: Int): ChartHorizontalLinesData {
    return ChartHorizontalLinesData(newMaxHeight, newMinHeight, useMinHeight)
  }

  fun createAnimator(f1: Float, f2: Float, l: AnimatorUpdateListener?): ValueAnimator {
    val a = ValueAnimator.ofFloat(f1, f2)
    a.setDuration(ANIM_DURATION.toLong())
    a.interpolator = INTERPOLATOR
    a.addUpdateListener(l)
    return a
  }

  var lastX: Int = 0
  var lastY: Int = 0
  var capturedX: Int = 0
  var capturedY: Int = 0
  var capturedTime: Long = 0

  @JvmField
  protected var canCaptureChartSelection: Boolean = false

  override fun onTouchEvent(event: MotionEvent): Boolean {
    if (chartData == null) {
      return false
    }
    if (!enabled) {
      pickerDelegate.uncapture(event, event.actionIndex)
      parent.requestDisallowInterceptTouchEvent(false)
      chartCaptured = false
      return false
    }


    var x = event.getX(event.actionIndex).toInt()
    var y = event.getY(event.actionIndex).toInt()

    when (event.actionMasked) {
      MotionEvent.ACTION_DOWN -> {
        capturedTime = System.currentTimeMillis()
        parent.requestDisallowInterceptTouchEvent(true)
        val captured = pickerDelegate.capture(x, y, event.actionIndex)
        if (captured) {
          return true
        }

        run {
          lastX = x
          capturedX = lastX
        }
        run {
          lastY = y
          capturedY = lastY
        }
        if (chartArea.contains(x, y)) {
          if (selectedIndex < 0 || !animateLegentTo) {
            chartCaptured = true
            selectXOnChart(x, y)
          }
          return true
        }
        return false
      }

      MotionEvent.ACTION_POINTER_DOWN -> return pickerDelegate.capture(x, y, event.actionIndex)
      MotionEvent.ACTION_MOVE -> {
        val dx = x - lastX
        val dy = y - lastY

        if (pickerDelegate.captured()) {
          val rez = pickerDelegate.move(x, y, event.actionIndex)
          if (event.pointerCount > 1) {
            x = event.getX(1).toInt()
            y = event.getY(1).toInt()
            pickerDelegate.move(x, y, 1)
          }

          parent.requestDisallowInterceptTouchEvent(rez)

          return true
        }

        if (chartCaptured) {
          val disable: Boolean = if (canCaptureChartSelection && System.currentTimeMillis() - capturedTime > 200) {
            true
          } else {
            abs(dx) > abs(dy) || abs(dy) < touchSlop
          }
          lastX = x
          lastY = y

          parent.requestDisallowInterceptTouchEvent(disable)
          selectXOnChart(x, y)
        } else if (chartArea.contains(capturedX, capturedY)) {
          val dxCaptured = capturedX - x
          val dyCaptured = capturedY - y
          if (sqrt((dxCaptured * dxCaptured + dyCaptured * dyCaptured).toDouble()) > touchSlop || System.currentTimeMillis() - capturedTime > 200) {
            chartCaptured = true
            selectXOnChart(x, y)
          }
        }
        return true
      }

      MotionEvent.ACTION_POINTER_UP -> {
        pickerDelegate.uncapture(event, event.actionIndex)
        return true
      }

      MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
        if (pickerDelegate.uncapture(event, event.actionIndex)) {
          return true
        }
        if (chartArea.contains(capturedX, capturedY) && !chartCaptured) {
          animateLegend(false)
        }
        pickerDelegate.uncapture()
        updateLineSignature()
        parent.requestDisallowInterceptTouchEvent(false)
        chartCaptured = false
        onActionUp()
        invalidate()
        var min = 0
        if (useMinHeight) min = findMinValue(startXIndex, endXIndex)
        setMaxMinValue(findMaxValue(startXIndex, endXIndex), min, animated = true, force = true, useAnimator = false)
        return true
      }
    }

    return false
  }

  protected open fun onActionUp() {
  }

  protected open fun selectXOnChart(x: Int, y: Int) {
    val oldSelectedX = selectedIndex
    if (chartData == null) return
    val offset: Float = chartFullWidth * (pickerDelegate.pickerStart) - horizontalPadding
    val xP = (offset + x) / chartFullWidth
    selectedCoordinate = xP
    if (xP < 0) {
      selectedIndex = 0
      selectedCoordinate = 0f
    } else if (xP > 1) {
      selectedIndex = chartData!!.x.size - 1
      selectedCoordinate = 1f
    } else {
      selectedIndex = chartData!!.findIndex(startXIndex, endXIndex, xP)
      if (selectedIndex + 1 < chartData!!.xPercentage.size) {
        val dx = abs(chartData!!.xPercentage[selectedIndex] - xP)
        val dx2 = abs(chartData!!.xPercentage[selectedIndex + 1] - xP)
        if (dx2 < dx) {
          selectedIndex++
        }
      }
    }

    if (selectedIndex > endXIndex) selectedIndex = endXIndex
    if (selectedIndex < startXIndex) selectedIndex = startXIndex

    legendShowing = true
    animateLegend(true)
    moveLegend(offset)
    if (dateSelectionListener != null) {
      dateSelectionListener!!.onDateSelected(this.selectedDate)
    }
    invalidate()
  }

  var animateLegentTo: Boolean = false

  fun animateLegend(show: Boolean) {
    moveLegend()
    if (animateLegentTo == show) return
    animateLegentTo = show
    if (selectionAnimator != null) {
      selectionAnimator!!.removeAllListeners()
      selectionAnimator!!.cancel()
    }
    selectionAnimator = createAnimator(selectionA, if (show) 1f else 0f, selectionAnimatorListener)
      .setDuration(200)

    selectionAnimator!!.addListener(selectorAnimatorEndListener)


    selectionAnimator!!.start()
  }

  @JvmOverloads
  fun moveLegend(offset: Float = chartFullWidth * (pickerDelegate.pickerStart) - horizontalPadding) {
    if (chartData == null || selectedIndex == -1 || !legendShowing) return
    legendSignatureView!!.setData(selectedIndex, chartData!!.x[selectedIndex], lines as ArrayList<LineViewData?>?, false)
    legendSignatureView!!.visibility = VISIBLE
    legendSignatureView!!.measure(
      MeasureSpec.makeMeasureSpec(measuredWidth, MeasureSpec.AT_MOST),
      MeasureSpec.makeMeasureSpec(measuredHeight, MeasureSpec.AT_MOST)
    )
    var lXPoint = chartData!!.xPercentage[selectedIndex] * chartFullWidth - offset
    if (lXPoint > (chartStart + chartWidth) shr 1) {
      lXPoint -= (legendSignatureView!!.width + DP_5).toFloat()
    } else {
      lXPoint += DP_5.toFloat()
    }
    if (lXPoint < 0) {
      lXPoint = 0f
    } else if (lXPoint + legendSignatureView!!.measuredWidth > measuredWidth) {
      lXPoint = (measuredWidth - legendSignatureView!!.measuredWidth).toFloat()
    }
    legendSignatureView!!.translationX = lXPoint
  }

  open fun findMaxValue(startXIndex: Int, endXIndex: Int): Int {
    val linesSize = lines!!.size
    var maxValue = 0
    for (j in 0..<linesSize) {
      if (!lines!![j]!!.enabled) continue
      val lineMax = lines!![j]!!.line.segmentTree.rMaxQ(startXIndex, endXIndex)
      if (lineMax > maxValue) maxValue = lineMax
    }
    return maxValue
  }


  open fun findMinValue(startXIndex: Int, endXIndex: Int): Int {
    val linesSize = lines!!.size
    var minValue = Int.MAX_VALUE
    for (j in 0..<linesSize) {
      if (!lines!![j]!!.enabled) continue
      val lineMin = lines!![j]!!.line.segmentTree.rMinQ(startXIndex, endXIndex)
      if (lineMin < minValue) minValue = lineMin
    }
    return minValue
  }

  open fun setData(chartData: T?) {
    if (this.chartData !== chartData) {
      invalidate()
      lines!!.clear()
      if (chartData != null && chartData.lines != null) {
        for (i in chartData.lines.indices) {
          lines!!.add(createLineViewData(chartData.lines[i]))
        }
      }
      clearSelection()
      this.chartData = chartData
      if (chartData != null) {
        if (chartData.x[0] == 0L) {
          pickerDelegate.pickerStart = 0f
          pickerDelegate.pickerEnd = 1f
        } else {
          pickerDelegate.minDistance = this.minDistance
          if (pickerDelegate.pickerEnd - pickerDelegate.pickerStart < pickerDelegate.minDistance) {
            pickerDelegate.pickerStart = pickerDelegate.pickerEnd - pickerDelegate.minDistance
            if (pickerDelegate.pickerStart < 0) {
              pickerDelegate.pickerStart = 0f
              pickerDelegate.pickerEnd = 1f
            }
          }
        }
      }
    }
    measureSizes()

    if (chartData != null) {
      updateIndexes()
      val min = if (useMinHeight) findMinValue(startXIndex, endXIndex) else 0
      setMaxMinValue(findMaxValue(startXIndex, endXIndex), min, false)
      pickerMaxHeight = 0f
      pickerMinHeight = Int.MAX_VALUE.toFloat()
      initPickerMaxHeight()
      legendSignatureView!!.setSize(lines!!.size)

      invalidatePickerChart = true
      updateLineSignature()
    } else {
      pickerDelegate.pickerStart = 0.7f
      pickerDelegate.pickerEnd = 1f

      pickerMinHeight = 0f
      pickerMaxHeight = pickerMinHeight
      horizontalLines.clear()

      if (maxValueAnimator != null) {
        maxValueAnimator!!.cancel()
      }

      if (alphaAnimator != null) {
        alphaAnimator!!.removeAllListeners()
        alphaAnimator!!.cancel()
      }
    }
  }

  protected open val minDistance: Float
    get() {
      if (chartData == null) {
        return 0.1f
      }

      val n = chartData!!.x.size
      if (n < 5) {
        return 1f
      }
      val r = 5f / n
      if (r < 0.1f) {
        return 0.1f
      }
      return r
    }

  protected open fun initPickerMaxHeight() {
    for (l in lines!!) {
      if (l!!.enabled && l.line.maxValue > pickerMaxHeight) pickerMaxHeight = l.line.maxValue.toFloat()
      if (l.enabled && l.line.minValue < pickerMinHeight) pickerMinHeight = l.line.minValue.toFloat()
      if (pickerMaxHeight == pickerMinHeight) {
        pickerMaxHeight++
        pickerMinHeight--
      }
    }
  }

  abstract fun createLineViewData(line: ChartData.Line?): L?

  override fun onPickerDataChanged() {
    onPickerDataChanged(animated = true, force = false, useAniamtor = false)
  }

  open fun onPickerDataChanged(animated: Boolean, force: Boolean, useAniamtor: Boolean) {
    if (chartData == null) return
    chartFullWidth = (chartWidth / (pickerDelegate.pickerEnd - pickerDelegate.pickerStart))

    updateIndexes()
    val min = if (useMinHeight) findMinValue(startXIndex, endXIndex) else 0
    setMaxMinValue(findMaxValue(startXIndex, endXIndex), min, animated, force, useAniamtor)

    if (legendShowing && !force) {
      animateLegend(false)
      moveLegend(chartFullWidth * (pickerDelegate.pickerStart) - horizontalPadding)
    }
    invalidate()
  }

  override fun onPickerJumpTo(start: Float, end: Float, force: Boolean) {
    if (chartData == null) return
    if (force) {
      val startXIndex = chartData!!.findStartIndex(
        max(
          start, 0f
        )
      )
      val endXIndex = chartData!!.findEndIndex(
        startXIndex, min(
        end, 1f
      )
      )
      setMaxMinValue(findMaxValue(startXIndex, endXIndex), findMinValue(startXIndex, endXIndex), animated = true, force = true, useAnimator = false)
      animateLegend(false)
    } else {
      updateIndexes()
      invalidate()
    }
  }

  protected fun updateIndexes() {
    if (chartData == null) return
    startXIndex = chartData!!.findStartIndex(
      max(
        pickerDelegate.pickerStart, 0f
      )
    )
    endXIndex = chartData!!.findEndIndex(
      startXIndex, min(
      pickerDelegate.pickerEnd, 1f
    )
    )
    if (chartListener != null) {
      chartListener!!.onDateChanged(this, chartData!!.x[startXIndex], chartData!!.x[endXIndex])
    }
    updateLineSignature()
  }

  private fun updateLineSignature() {
    if (chartData == null || chartWidth == 0) return
    val d = chartFullWidth * chartData!!.oneDayPercentage

    val k = chartWidth / d
    val step = (k / BOTTOM_SIGNATURE_COUNT).toInt()
    updateDates(step)
  }


  private fun updateDates(step: Int) {
    var step = step
    if (currentBottomSignatures == null || step >= currentBottomSignatures!!.stepMax || step <= currentBottomSignatures!!.stepMin) {
      step = Integer.highestOneBit(step) shl 1
      if (currentBottomSignatures != null && currentBottomSignatures!!.step == step) {
        return
      }

      if (alphaBottomAnimator != null) {
        alphaBottomAnimator!!.removeAllListeners()
        alphaBottomAnimator!!.cancel()
      }

      val stepMax = (step + step * 0.2).toInt()
      val stepMin = (step - step * 0.2).toInt()


      val data = ChartBottomSignatureData(step, stepMax, stepMin)
      data.alpha = 255

      if (currentBottomSignatures == null) {
        currentBottomSignatures = data
        data.alpha = 255
        bottomSignatureDate.add(data)
        return
      }

      currentBottomSignatures = data


      tmpN = bottomSignatureDate.size
      for (i in 0..<tmpN) {
        val a = bottomSignatureDate[i]
        a.fixedAlpha = a.alpha
      }

      bottomSignatureDate.add(data)
      if (bottomSignatureDate.size > 2) {
        bottomSignatureDate.removeAt(0)
      }

      alphaBottomAnimator = createAnimator(0f, 1f) { animation: ValueAnimator? ->
        val alpha = animation!!.getAnimatedValue() as Float
        for (a in bottomSignatureDate) {
          if (a === data) {
            data.alpha = (255 * alpha).toInt()
          } else {
            a.alpha = ((1f - alpha) * (a.fixedAlpha)).toInt()
          }
        }
        invalidate()
      }.setDuration(200)
      alphaBottomAnimator!!.addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
          super.onAnimationEnd(animation)
          bottomSignatureDate.clear()
          bottomSignatureDate.add(data)
        }
      })

      alphaBottomAnimator!!.start()
    }
  }

  open fun onCheckChanged() {
    onPickerDataChanged(animated = true, force = true, useAniamtor = true)
    tmpN = lines!!.size
    tmpI = 0
    while (tmpI < tmpN) {
      val lineViewData: LineViewData = lines!![tmpI]!!

      if (lineViewData.enabled) {
        lineViewData.animatorOut?.cancel()
      }

      if (!lineViewData.enabled) {
        lineViewData.animatorIn?.cancel()
      }

      if (lineViewData.enabled && lineViewData.alpha != 1f) {
        if (lineViewData.animatorIn?.isRunning == true) {
          tmpI++
          continue
        }
        lineViewData.animatorIn = createAnimator(lineViewData.alpha, 1f) { animation: ValueAnimator? ->
          lineViewData.alpha = (animation!!.getAnimatedValue() as Float)
          invalidatePickerChart = true
          invalidate()
        }
        lineViewData.animatorIn!!.start()
      }

      if (!lineViewData.enabled && lineViewData.alpha != 0f) {
        if (lineViewData.animatorOut?.isRunning == true) {
          tmpI++
          continue
        }
        lineViewData.animatorOut = createAnimator(lineViewData.alpha, 0f) { animation: ValueAnimator? ->
          lineViewData.alpha = (animation!!.getAnimatedValue() as Float)
          invalidatePickerChart = true
          invalidate()
        }
        lineViewData.animatorOut!!.start()
      }
      tmpI++
    }

    updatePickerMinMaxHeight()
    if (legendShowing) legendSignatureView!!.setData(selectedIndex, chartData!!.x[selectedIndex], lines as ArrayList<LineViewData?>?, true)
  }

  protected open fun updatePickerMinMaxHeight() {
    var max = 0
    var min = Int.MAX_VALUE
    for (l in lines!!) {
      if (l!!.enabled && l.line.maxValue > max) max = l.line.maxValue
      if (l.enabled && l.line.minValue < min) min = l.line.minValue
    }

    if ((min != Int.MAX_VALUE && min.toFloat() != animatedToPickerMinHeight) || (max > 0 && max.toFloat() != animatedToPickerMaxHeight)) {
      animatedToPickerMaxHeight = max.toFloat()
      if (pickerAnimator != null) pickerAnimator!!.cancel()
      val animatorSet = AnimatorSet()
      animatorSet.playTogether(
        createAnimator(pickerMaxHeight, animatedToPickerMaxHeight, pickerHeightUpdateListener),
        createAnimator(pickerMinHeight, animatedToPickerMinHeight, pickerMinHeightUpdateListener)
      )
      pickerAnimator = animatorSet
      pickerAnimator!!.start()
    }
  }

  fun saveState(outState: Bundle?) {
    if (outState == null) return

    outState.putFloat("chart_start", pickerDelegate.pickerStart)
    outState.putFloat("chart_end", pickerDelegate.pickerEnd)


    if (lines != null) {
      val n = lines!!.size
      val bArray = BooleanArray(n)
      for (i in 0..<n) {
        bArray[i] = lines!![i]!!.enabled
      }
      outState.putBooleanArray("chart_line_enabled", bArray)
    }
  }

  interface DateChangeListener {
    fun onDateChanged(chartView: BaseChartView<*, *>?, startTimeMs: Long, endTimeMs: Long)
  }

  private var chartListener: DateChangeListener? = null

  init {
    init()
    touchSlop = ViewConfiguration.get(context).scaledTouchSlop
  }

  fun setListener(listener: DateChangeListener?) {
    this.chartListener = listener
  }

  val selectedDate: Long
    get() {
      if (selectedIndex < 0) {
        return -1
      }
      return chartData!!.x[selectedIndex]
    }

  fun clearSelection() {
    selectedIndex = -1
    legendShowing = false
    animateLegentTo = false
    legendSignatureView!!.visibility = GONE
    selectionA = 0f
  }

  fun selectDate(activeZoom: Long) {
    selectedIndex = Arrays.binarySearch(chartData!!.x, activeZoom)
    legendShowing = true
    legendSignatureView!!.visibility = VISIBLE
    selectionA = 1f
    moveLegend(chartFullWidth * (pickerDelegate.pickerStart) - horizontalPadding)
  }

  val startDate: Long
    get() = chartData!!.x[startXIndex]

  val endDate: Long
    get() = chartData!!.x[endXIndex]

  open fun updatePicker(chartData: ChartData, d: Long) {
    val n = chartData.x.size
    val startOfDay = d - d % 86400000L
    val endOfDay = startOfDay + 86400000L - 1
    var startIndex = 0
    var endIndex = 0

    for (i in 0..<n) {
      if (startOfDay > chartData.x[i]) startIndex = i
      if (endOfDay > chartData.x[i]) endIndex = i
    }
    pickerDelegate.pickerStart = chartData.xPercentage[startIndex]
    pickerDelegate.pickerEnd = chartData.xPercentage[endIndex]
  }

  fun setDateSelectionListener(dateSelectionListener: DateSelectionListener?) {
    this.dateSelectionListener = dateSelectionListener
  }

  interface DateSelectionListener {
    fun onDateSelected(date: Long)
  }

  class SharedUiComponents {
    private var pickerRoundBitmap: Bitmap? = null
    private var canvas: Canvas? = null


    private val rectF = RectF()
    private val xRefP = Paint(Paint.ANTI_ALIAS_FLAG)

    var k: Int = 0
    private var invalidate = true

    init {
      xRefP.setColor(0)
      xRefP.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    fun getPickerMaskBitmap(h: Int, w: Int): Bitmap {
      if (h + w shl 10 != k || invalidate) {
        invalidate = false
        k = h + w shl 10
        if (!U.isValidBitmap(pickerRoundBitmap) || pickerRoundBitmap!!.getWidth() != w || pickerRoundBitmap!!.getHeight() != h) {
          pickerRoundBitmap = createBitmap(w, h)
          canvas = Canvas(pickerRoundBitmap!!)
          rectF.set(0f, 0f, w.toFloat(), h.toFloat())
        }

        canvas!!.drawColor(Theme.fillingColor()) // Theme.key_windowBackgroundWhite
        canvas!!.drawRoundRect(rectF, Screen.dp(4f).toFloat(), Screen.dp(4f).toFloat(), xRefP)
      }


      return pickerRoundBitmap!!
    }

    fun invalidate() {
      invalidate = true
    }
  }

  companion object {
    private const val LINE_WIDTH = 1f

    @JvmField
    val USE_LINES: Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.P
    protected const val ANIMATE_PICKER_SIZES: Boolean = true

    @JvmField
    var INTERPOLATOR: FastOutSlowInInterpolator = FastOutSlowInInterpolator()

    const val TRANSITION_MODE_CHILD: Int = 1
    const val TRANSITION_MODE_PARENT: Int = 2
    const val TRANSITION_MODE_ALPHA_ENTER: Int = 3
    const val TRANSITION_MODE_NONE: Int = 0

    @JvmStatic
    val horizontalPadding: Int
      get() = Screen.dp(16f)

    @JvmStatic
    val signatureTextHeight: Int
      get() = Screen.dp(18f)

    val bottomSignatureStartAlpha: Int
      get() = Screen.dp(10f)

    @JvmStatic
    val pickerPadding: Int
      get() = Screen.dp(16f)

    private const val BOTTOM_SIGNATURE_COUNT = 6

    fun RoundedRect(
      path: Path,
      left: Float, top: Float, right: Float, bottom: Float, rx: Float, ry: Float,
      tl: Boolean, tr: Boolean, br: Boolean, bl: Boolean
    ): Path {
      var rx = rx
      var ry = ry
      path.reset()
      if (rx < 0) rx = 0f
      if (ry < 0) ry = 0f
      val width = right - left
      val height = bottom - top
      if (rx > width / 2) rx = width / 2
      if (ry > height / 2) ry = height / 2
      val widthMinusCorners = (width - (2 * rx))
      val heightMinusCorners = (height - (2 * ry))

      path.moveTo(right, top + ry)
      if (tr) path.rQuadTo(0f, -ry, -rx, -ry)
      else {
        path.rLineTo(0f, -ry)
        path.rLineTo(-rx, 0f)
      }
      path.rLineTo(-widthMinusCorners, 0f)
      if (tl) path.rQuadTo(-rx, 0f, -rx, ry)
      else {
        path.rLineTo(-rx, 0f)
        path.rLineTo(0f, ry)
      }
      path.rLineTo(0f, heightMinusCorners)

      if (bl) path.rQuadTo(0f, ry, rx, ry)
      else {
        path.rLineTo(0f, ry)
        path.rLineTo(rx, 0f)
      }

      path.rLineTo(widthMinusCorners, 0f)
      if (br) path.rQuadTo(rx, 0f, rx, -ry)
      else {
        path.rLineTo(rx, 0f)
        path.rLineTo(0f, -ry)
      }

      path.rLineTo(0f, -heightMinusCorners)

      path.close()
      return path
    }
  }
}
