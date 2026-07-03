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
 * File created on 21/10/2016
 */
package org.thunderdog.challegram.component.attach

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Paints
import me.vkryl.core.color

class GridSpacingItemDecoration (
  private var spanCount: Int,
  private val spacing: Int,
  private val includeEdge: Boolean,
  private val needVertical: Boolean,
  private val enableRtl: Boolean
) : RecyclerView.ItemDecoration() {
  fun setSpanCount (spanCount: Int) {
    if (this.spanCount != spanCount) {
      this.spanCount = spanCount
    }
  }

  private var needDraw = false
  private var viewType = 0
  private var colorId = 0

  fun setNeedDraw (needDraw: Boolean, viewType: Int) {
    this.needDraw = needDraw
    this.viewType = viewType
  }

  fun setDrawColorId (@ColorId colorId: Int) {
    this.colorId = colorId
  }

  override fun onDraw (c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
    if (needDraw) {
      val manager = parent.layoutManager as LinearLayoutManager
      val firstVisiblePosition = manager.findFirstVisibleItemPosition()
      val lastVisiblePosition = manager.findLastVisibleItemPosition()

      if (firstVisiblePosition == -1 || lastVisiblePosition == -1) {
        return
      }

      val color = Theme.getColor(colorId)
      var maxAlpha = 0f
      val parentWidth = parent.measuredWidth
      var foundTop = false
      var currentTop = 0
      var lastView: View? = null
      for (i in firstVisiblePosition..lastVisiblePosition) {
        val view: View = manager.findViewByPosition(i)!!
        lastView = view
        val holder = parent.getChildViewHolder(view)
        if (holder == null) {
          continue
        }
        if (holder.itemViewType == viewType) {
          if (!foundTop) {
            currentTop = view.top - manager.getTopDecorationHeight(view)
            foundTop = true
          }
          maxAlpha = maxOf(view.alpha, maxAlpha)
        } else if (foundTop) {
          val viewTop = view.top
          if (currentTop != viewTop && maxAlpha > 0f) {
            val drawColor = color((Color.alpha(color) * maxAlpha).toInt(), color)
            c.drawRect(0f, currentTop.toFloat(), parentWidth.toFloat(), view.top.toFloat(), Paints.fillingPaint(drawColor))
          }
          currentTop = 0
          foundTop = false
          maxAlpha = 0f
        }
      }
      val viewBottom = if (lastView != null) lastView.top + lastView.measuredHeight + manager.getBottomDecorationHeight(lastView) else 0
      if (foundTop && currentTop != viewBottom && maxAlpha != 0f) {
        val drawColor = color((Color.alpha(color) * maxAlpha).toInt(), color)
        c.drawRect(0f, currentTop.toFloat(), parentWidth.toFloat(), viewBottom.toFloat(), Paints.fillingPaint(drawColor))
      }
    }
  }

  private var lookup: GridLayoutManager.SpanSizeLookup? = null

  fun setSpanSizeLookup (lookup: GridLayoutManager.SpanSizeLookup?) {
    this.lookup = lookup
  }

  override fun getItemOffsets (outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
    val adapterPosition = parent.getChildAdapterPosition(view) // item position
    var position = adapterPosition
    val itemCount = parent.adapter!!.itemCount
    var column: Int

    val lookup = this.lookup
    if (lookup != null) {
      val spanSize = lookup.getSpanSize(position)

      if (spanSize != 1) {
        outRect.setEmpty()
        return
      }

      column = lookup.getSpanIndex(position, spanCount)

      var i = position - 1
      while (i >= 0 && lookup.getSpanSize(i++) != 1) {
        position--
        if (position < 0 || i == itemCount) {
          outRect.setEmpty()
          return
        }
      }
    } else {
      column = position % spanCount // item column
    }

    if (enableRtl && Lang.rtl()) {
      column = spanCount - column - 1
    }

    if (includeEdge) {
      outRect.left = spacing - column * spacing / spanCount // spacing - column * ((1f / spanCount) * spacing)
      outRect.right = (column + 1) * spacing / spanCount // (column + 1) * ((1f / spanCount) * spacing)
      if (needVertical) {
        if (position < spanCount) { // top edge
          outRect.top = spacing
        }
        outRect.bottom = spacing // item bottom
      } else if (lookup != null) {
        if (true) {
          outRect.top = 0
        } else {
          if (adapterPosition > spanCount) {
            outRect.top = 0
          } else {
            var topEndIndex = 0
            while (topEndIndex < spanCount && lookup.getSpanSize(topEndIndex) == 1) {
              topEndIndex++
            }
            outRect.top = if (adapterPosition < topEndIndex) spacing else 0
          }
        }
        val adapterItemCount = parent.adapter!!.itemCount
        if (adapterPosition < adapterItemCount - spanCount) {
          outRect.bottom = 0
        } else {
          var bottomEndIndex = adapterItemCount - 1
          while (bottomEndIndex > adapterItemCount - spanCount && lookup.getSpanSize(bottomEndIndex) == 0) {
            bottomEndIndex--
          }
          outRect.bottom = if (adapterPosition >= bottomEndIndex) spacing else 0
        }
      }
    } else {
      outRect.left = column * spacing / spanCount // column * ((1f / spanCount) * spacing)
      outRect.right = spacing - (column + 1) * spacing / spanCount // spacing - (column + 1) * ((1f /    spanCount) * spacing)
      if (needVertical) {
        if (position >= spanCount || lookup != null) {
          outRect.top = spacing // item top
        }
        if (lookup != null) {
          if (lookup.getSpanSize(minOf(itemCount - 1, adapterPosition + (spanCount - column))) != 1) {
            outRect.bottom = spacing
          } else {
            outRect.bottom = 0
          }
        }
      } else {
        outRect.top = 0
        outRect.bottom = 0
      }
    }
  }
}
