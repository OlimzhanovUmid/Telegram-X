package org.thunderdog.challegram.emoji

import android.graphics.Paint
import kotlin.math.abs

class EmojiSize {
  private val tmpFontMetrics = Paint.FontMetricsInt()
  private var size = -1

  fun isInitialized (): Boolean = size != -1

  fun initialize (paint: Paint, fm: Paint.FontMetricsInt?, optional: Boolean) {
    if (optional && isInitialized()) {
      return
    }

    paint.getFontMetricsInt(tmpFontMetrics)
    size = abs(tmpFontMetrics.descent - tmpFontMetrics.ascent)

    if (fm != null) {
      fm.ascent = tmpFontMetrics.ascent
      fm.descent = tmpFontMetrics.descent
      fm.top = tmpFontMetrics.top
      fm.bottom = tmpFontMetrics.bottom
    }
  }

  fun getSize (): Int = size
}
