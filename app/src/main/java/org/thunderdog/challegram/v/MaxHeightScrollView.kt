package org.thunderdog.challegram.v

import android.content.Context
import android.view.MotionEvent
import android.widget.ScrollView
import org.thunderdog.challegram.tool.Views

class MaxHeightScrollView (context: Context) : ScrollView(context) {
  private var maxHeight = 0

  fun setMaxHeight (maxHeight: Int) {
    if (this.maxHeight != maxHeight) {
      this.maxHeight = maxHeight
      requestLayout()
    }
  }

  private var isScrollable = false

  override fun onMeasure (widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    if (maxHeight != 0 && measuredHeight > maxHeight) {
      super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.EXACTLY))
      isScrollable = true
    } else {
      isScrollable = false
    }
  }

  override fun onTouchEvent (ev: MotionEvent): Boolean {
    val res = super.onTouchEvent(ev) && (ev.action != MotionEvent.ACTION_DOWN || (isScrollable && Views.isValid(this)))
    when (ev.action) {
      MotionEvent.ACTION_DOWN -> {
        if (isScrollable && Views.isValid(this)) {
          parent?.requestDisallowInterceptTouchEvent(true)
        }
      }
      MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
        parent?.requestDisallowInterceptTouchEvent(false)
      }
    }
    return res
  }

  override fun onInterceptTouchEvent (ev: MotionEvent): Boolean {
    return (super.onInterceptTouchEvent(ev) && (ev.action != MotionEvent.ACTION_DOWN || Views.isValid(this))) || !Views.isValid(this)
  }
}
