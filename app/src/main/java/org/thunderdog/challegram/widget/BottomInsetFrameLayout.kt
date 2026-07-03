package org.thunderdog.challegram.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.view.View
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Paints
import org.thunderdog.challegram.tool.Views
import me.vkryl.android.widget.FrameLayoutFix

class BottomInsetFrameLayout (context: Context) : FrameLayoutFix(context), RootFrameLayout.MarginModifier {
  override fun onApplyMarginInsets (child: View?, params: LayoutParams, legacyInsets: Rect?, insets: Rect, insetsWithoutIme: Rect) {
    setBottomInset(insetsWithoutIme.bottom)
  }

  private var bottomInset = 0

  fun setBottomInset (bottomInset: Int) {
    if (this.bottomInset != bottomInset) {
      this.bottomInset = bottomInset
      Views.setPaddingBottom(this, bottomInset)
      setWillNotDraw(bottomInset == 0)
    }
  }

  override fun onDraw (c: Canvas) {
    if (paddingBottom > 0) {
      c.drawRect(0f, (measuredHeight - paddingBottom).toFloat(), measuredWidth.toFloat(), measuredHeight.toFloat(), Paints.fillingPaint(Theme.fillingColor()))
    }
  }
}
