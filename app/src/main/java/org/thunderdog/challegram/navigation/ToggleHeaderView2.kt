package org.thunderdog.challegram.navigation

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.text.TextPaint
import android.text.TextUtils
import android.view.View

import androidx.annotation.ColorInt
import androidx.annotation.Dimension

import org.thunderdog.challegram.R
import org.thunderdog.challegram.U
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.theme.PorterDuffColorId
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Drawables
import org.thunderdog.challegram.tool.Paints
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.tool.getPorterDuffPaint

import me.vkryl.android.DECELERATE_INTERPOLATOR
import me.vkryl.android.animator.ReplaceAnimator

class ToggleHeaderView2 (context: Context) : View(context) {
  private val titleR = ReplaceAnimator<TrimmedText>({ invalidate() }, DECELERATE_INTERPOLATOR, 180L)
  private val subtitleR = ReplaceAnimator<TrimmedText>({ invalidate() }, DECELERATE_INTERPOLATOR, 180L)
  private val arrowDrawable: Drawable? = Drawables.get(R.drawable.baseline_keyboard_arrow_down_20)

  @Dimension(unit = Dimension.DP)
  private var textPaddingDp = 10f
  @Dimension(unit = Dimension.DP)
  private var textTopDp = 8.5f
  @Dimension(unit = Dimension.DP)
  private var triangleTopDp = 8.5f

  private var colorSet: ColorSet = ColorSet.DEFAULT

  interface ColorSet {
    @ColorInt
    fun titleColor (): Int {
      return Theme.getColor(ColorId.text)
    }

    @ColorInt
    fun subtitleColor (): Int {
      return Theme.getColor(ColorId.textLight)
    }

    @PorterDuffColorId
    fun iconColorId (): Int {
      return ColorId.icon
    }

    companion object {
      val DEFAULT: ColorSet = object : ColorSet {}
    }
  }

  fun setTitle (title: String, animated: Boolean) {
    titleR.replace(TrimmedText(title), animated)
    trimTexts()
    invalidate()
  }

  fun setSubtitle (subtitle: String, animated: Boolean) {
    subtitleR.replace(TrimmedText(subtitle), animated)
    trimTexts()
    invalidate()
  }

  fun setTextTop (@Dimension(unit = Dimension.DP) textTop: Float) {
    textTopDp = textTop
  }

  fun setTriangleTop (@Dimension(unit = Dimension.DP) triangleTop: Float) {
    triangleTopDp = triangleTop
  }

  fun setTextPadding (@Dimension(unit = Dimension.DP) textPadding: Float) {
    textPaddingDp = textPadding
  }

  fun setColorSet (colorSet: ColorSet) {
    this.colorSet = colorSet
  }

  private fun trimTexts () {
    val avail = measuredWidth - Screen.dp(textPaddingDp) - Screen.dp(12f)
    for (entry in titleR) {
      entry.item.measure(avail, Paints.getMediumTextPaint(18f, colorSet.titleColor(), false))
    }
    for (entry in subtitleR) {
      entry.item.measure(avail, Paints.getRegularTextPaint(14f, colorSet.subtitleColor()))
    }
  }

  override fun onMeasure (widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    trimTexts()
  }

  fun getTitleWidth (): Float {
    var width = 0f
    for (entry in titleR) {
      width += entry.item.getWidth() * entry.getVisibility()
    }

    return width
  }

  override fun onDraw (c: Canvas) {
    val textTop = Screen.dp(textTopDp) + Screen.dp(17f)
    val triangleTop = Screen.dp(triangleTopDp) + Screen.dp(3f)
    for (entry in titleR) {
      val offset2 = (if (!entry.isAffectingList())
        (entry.getVisibility() - 1f) * Screen.dp(18f)
      else
        (1f - entry.getVisibility()) * Screen.dp(18f)).toInt()
      entry.item.draw(c, paddingLeft, paddingTop + textTop + offset2, entry.getVisibility(), Paints.getMediumTextPaint(18f, colorSet.titleColor(), false))
    }
    for (entry in subtitleR) {
      val offset2 = (if (!entry.isAffectingList())
        (entry.getVisibility() - 1f) * Screen.dp(14f)
      else
        (1f - entry.getVisibility()) * Screen.dp(14f)).toInt()
      entry.item.draw(c, paddingLeft, paddingTop + textTop + Screen.dp(19f) + offset2, entry.getVisibility(), Paints.getRegularTextPaint(14f, colorSet.subtitleColor()))
    }

    Drawables.draw(c, arrowDrawable, getTitleWidth() + Screen.dp(2f), (paddingTop + triangleTop).toFloat(), getPorterDuffPaint(colorSet.iconColorId()))
  }


  private class TrimmedText (private val text: String) {
    private var textTrimmed: String? = null
    private var textTrimmedWidth = 0f
    private var textWidth = 0f

    fun measure (width: Int, paint: TextPaint) {
      textWidth = U.measureText(text, paint)
      if (textWidth <= width) {
        textTrimmed = null
        textTrimmedWidth = 0f
      } else {
        textTrimmed = TextUtils.ellipsize(text, paint, width.toFloat(), TextUtils.TruncateAt.END).toString()
        textTrimmedWidth = U.measureText(textTrimmed, paint)
      }
    }

    fun getWidth (): Float {
      return if (textTrimmed != null) textTrimmedWidth else textWidth
    }

    fun draw (canvas: Canvas, x: Int, y: Int, alpha: Float, paint: TextPaint) {
      paint.alpha = (alpha * 255).toInt()
      canvas.drawText(textTrimmed ?: text, x.toFloat(), y.toFloat(), paint)
    }
  }
}
