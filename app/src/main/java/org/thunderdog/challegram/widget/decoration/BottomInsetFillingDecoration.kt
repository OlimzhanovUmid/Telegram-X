package org.thunderdog.challegram.widget.decoration

import android.graphics.Canvas
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Paints

class BottomInsetFillingDecoration (@ColorId private val colorId: Int) : RecyclerView.ItemDecoration() {
  override fun onDraw (c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
    super.onDraw(c, parent, state)

    val manager = parent.layoutManager as LinearLayoutManager

    var maxBottom = -1
    var hasBottom = false
    for (i in 0 until manager.childCount) {
      val view = manager.getChildAt(i)
      if (view != null) {
        val bottom = manager.getDecoratedBottom(view)
        maxBottom = if (hasBottom) {
          maxOf(bottom, maxBottom)
        } else {
          bottom
        }
        hasBottom = true
      }
    }

    if (hasBottom) {
      val height = parent.measuredHeight
      if (height > maxBottom) {
        c.drawRect(0f, maxBottom.toFloat(), parent.measuredWidth.toFloat(), height.toFloat(), Paints.fillingPaint(Theme.getColor(colorId)))
      }
    }
  }
}
