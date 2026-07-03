package org.thunderdog.challegram.util

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import org.thunderdog.challegram.tool.Screen

class ReactionsCounterDrawable (private val topReactions: ReactionsListAnimator) : Drawable() {
  override fun setAlpha (i: Int) {

  }

  override fun setColorFilter (colorFilter: ColorFilter?) {

  }

  @Suppress("deprecation")
  override fun getOpacity (): Int {
    return PixelFormat.UNKNOWN
  }

  private fun getVisibility (item: ReactionsListAnimator.Entry): Float {
    val position = item.getPosition()
    val visibility = item.getVisibility()
    if (position > 3f) {
      return 0f
    } else if (position > 2f) {
      return Math.min(visibility, 3f - position)
    }

    return visibility
  }

  private fun getTargetVisibility (item: ReactionsListAnimator.Entry): Float {
    val position = item.getPosition()
    val visibility = if (item.isAffectingList()) 1f else 0f
    if (position > 3f) {
      return 0f
    } else if (position > 2f) {
      return Math.min(visibility, 3f - position)
    }

    return visibility
  }

  override fun getMinimumWidth (): Int {
    var width = 0f
    for (a in 0 until topReactions.size()) {
      val item = topReactions.getEntry(a)
      width += Screen.dp(15f) * getVisibility(item)
    }
    return Math.max(width.toInt() - Screen.dp(3f), 0)
  }

  fun getTargetWidth (): Int {
    var width = 0f
    for (a in 0 until topReactions.size()) {
      val item = topReactions.getEntry(a)
      width += Screen.dp(15f) * getTargetVisibility(item)
    }
    return Math.max(width.toInt() - Screen.dp(3f), 0)
  }

  override fun draw (c: Canvas) {  // Never use
    draw(c, 0, 0)
  }

  fun draw (c: Canvas, x: Int, y: Int) {
    // c.drawRect(x, y - Screen.dp(6), x + getMinimumWidth(), y + Screen.dp(6), Paints.strokeSmallPaint(Color.RED));

    for (a in 0 until topReactions.size()) {
      val item = topReactions.getEntry(a)
      item.item.drawReactionNonBubble(c, x + item.getPosition() * Screen.dp(15f) + Screen.dp(6f), y.toFloat(), 12f, getVisibility(item))
    }
  }
}
