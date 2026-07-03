package org.thunderdog.challegram.widget

import android.content.Context
import android.graphics.Canvas
import android.view.View
import androidx.core.graphics.ColorUtils
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Paints

class LickView (context: Context) : View(context) {
  var factor: Float = 0f
    set (factor) {
      if (field != factor) {
        field = factor
        invalidate()
      }
    }
  private var headerBackground: Int = 0

  fun setHeaderBackground (headerBackground: Int) {
    this.headerBackground = headerBackground
    invalidate()
  }

  override fun onDraw (c: Canvas) {
    if (factor > 0f) {
      val bottom = measuredHeight
      val top = bottom - (bottom.toFloat() * factor).toInt()
      c.drawRect(0f, top.toFloat(), measuredWidth.toFloat(), bottom.toFloat(), Paints.fillingPaint(
        ColorUtils.compositeColors(Theme.getColor(ColorId.statusBar), headerBackground)
      ))
    }
  }
}
