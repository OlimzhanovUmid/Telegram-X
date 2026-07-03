/*
 * This file is a part of Telegram X
 * Copyright © 2014 (tgx-android@pm.me)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * File created on 15/08/2017
 */
package org.thunderdog.challegram.widget

import android.graphics.Canvas
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.thunderdog.challegram.navigation.ViewController
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Paints
import me.vkryl.core.alphaColor

open class FillingDecoration (view: RecyclerView, themeProvider: ViewController<*>?) : RecyclerView.ItemDecoration() {
  private val ranges = ArrayList<IntArray>()

  private @ColorId var fillingColorId: Int = ColorId.filling

  init {
    themeProvider?.addThemeInvalidateListener(view)
  }

  fun addRange (fromIndex: Int, toIndex: Int): FillingDecoration {
    ranges.add(intArrayOf(fromIndex, toIndex))
    return this
  }

  fun removeLastRange (): IntArray {
    return ranges.removeAt(ranges.size - 1)
  }

  fun clearRanges (): FillingDecoration {
    ranges.clear()
    return this
  }

  fun rangeAt (index: Int): IntArray {
    return ranges[index]
  }

  fun firstRange (): IntArray {
    return ranges[0]
  }

  fun lastRange (): IntArray {
    return ranges[ranges.size - 1]
  }

  fun rangesCount (): Int {
    return ranges.size
  }

  private var bottomId = View.NO_ID
  private var maxIndex = -1

  fun addBottom (bottomId: Int, maxIndex: Int): FillingDecoration {
    this.bottomId = bottomId
    this.maxIndex = maxIndex
    return this
  }

  final override fun onDraw (c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
    val manager = parent.layoutManager as LinearLayoutManager
    val first = manager.findFirstVisibleItemPosition()
    val last = manager.findLastVisibleItemPosition()

    if (first == RecyclerView.NO_POSITION || last == RecyclerView.NO_POSITION) {
      return
    }

    var fillingTop = -1
    var fillingBottom = -1

    var openFillingColor = 0

    val searchById = bottomId != View.NO_ID && maxIndex != -1
    var bottomFound = false
    var view: View? = null
    for (position in 0..last) {
      view = manager.findViewByPosition(position)
      val currentView = view

      var fillingColor = if (currentView != null) getFillingColor(position, currentView) else 0
      if (fillingColor == 0 && currentView == null && (searchById && position < maxIndex && !bottomFound)) {
        fillingColor = Theme.getColor(this.fillingColorId)
      }

      if (searchById && fillingColor != 0 && currentView != null && currentView.id == bottomId) {
        fillingColor = 0
        bottomFound = true
      }

      if (fillingColor != openFillingColor) {
        if (openFillingColor != 0) {
          drawFilling(c, parent, view, fillingTop, fillingBottom, openFillingColor)
        }
        if (fillingColor != 0) {
          fillingTop = if (currentView != null) (manager.getDecoratedTop(currentView) + currentView.translationY).toInt() else fillingBottom
        }
        openFillingColor = fillingColor
      }
      if (fillingColor != 0) {
        fillingBottom = if (currentView != null) (manager.getDecoratedBottom(currentView) + currentView.translationY).toInt() else parent.measuredHeight
      }
    }

    if (openFillingColor != 0) {
      drawFilling(c, parent, view, fillingTop, fillingBottom, openFillingColor)
    }
    if (needSeparateDecorations()) {
      for (i in 0 until parent.childCount) {
        view = parent.getChildAt(i)
        val currentView = view
        val adapterPosition = if (currentView != null) parent.getChildAdapterPosition(currentView) else RecyclerView.NO_POSITION
        if (currentView == null || adapterPosition != RecyclerView.NO_POSITION)
          continue
        val fillingColor = getFillingColor(adapterPosition, currentView)
        if (fillingColor != 0) {
          drawFilling(c, parent, currentView, manager.getDecoratedTop(currentView) + currentView.translationY.toInt(), manager.getDecoratedBottom(currentView) + currentView.translationY.toInt(), fillingColor)
        }
        drawDecorationForView(c, parent, state, currentView)
      }
      for (i in first..last) {
        view = manager.findViewByPosition(i)
        val loopView = view
        if (loopView != null) {
          drawDecorationForView(c, parent, state, loopView)
        }
      }
    }
  }

  private fun drawFilling (c: Canvas, parent: RecyclerView, view: View?, fillingTop: Int, fillingBottom: Int, fillingColor: Int) {
    var fillingBottom = fillingBottom
    if (view is ShadowView) {
      fillingBottom += view.shadowTop
    }
    c.drawRect(0f, maxOf(0, fillingTop).toFloat(), parent.measuredWidth.toFloat(), minOf(parent.measuredHeight, fillingBottom).toFloat(), Paints.fillingPaint(fillingColor))
  }

  protected open fun needSeparateDecorations (): Boolean {
    return false
  }

  protected open fun drawDecorationForView (c: Canvas, parent: RecyclerView, state: RecyclerView.State, view: View) {
    // Override
  }

  protected open fun getFillingColor (i: Int, view: View): Int {
    val alpha = view.alpha
    for (range in ranges) {
      if (i >= range[0] && i < range[1]) {
        return alphaColor(alpha, Theme.getColor(fillingColorId))
      }
    }
    if (bottomId != 0 && i < maxIndex) {
      return alphaColor(alpha, Theme.getColor(fillingColorId))
    }
    return 0
  }
}
