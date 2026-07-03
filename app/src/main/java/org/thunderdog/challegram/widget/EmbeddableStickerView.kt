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
 */
package org.thunderdog.challegram.widget

import android.content.Context
import android.text.method.LinkMovementMethod
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.widget.LinearLayout
import android.widget.TextView
import org.thunderdog.challegram.charts.LayoutHelper
import org.thunderdog.challegram.component.sticker.StickerSmallView
import org.thunderdog.challegram.component.sticker.TGStickerObj
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.theme.ThemeInvalidateListener
import me.vkryl.core.lambda.Destroyable

class EmbeddableStickerView @JvmOverloads constructor (context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : LinearLayout(context, attrs, defStyleAttr), ThemeInvalidateListener, Destroyable {
  private val stickerSmallView: StickerSmallView
  private val captionTextView: TextView

  init {
    setOrientation(LinearLayout.VERTICAL)

    stickerSmallView = object : StickerSmallView(context) {
      override fun dispatchTouchEvent (event: MotionEvent): Boolean {
        return false
      }
    }
    stickerSmallView.layoutParams = LayoutHelper.createLinear(128, 128, Gravity.CENTER_HORIZONTAL, 0, 8, 0, 0)
    addView(stickerSmallView)

    captionTextView = TextView(context)
    captionTextView.gravity = Gravity.CENTER_HORIZONTAL
    captionTextView.setTextSize(14f)
    captionTextView.movementMethod = LinkMovementMethod.getInstance()
    captionTextView.highlightColor = Theme.textLinkHighlightColor()
    addView(captionTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL.toFloat(), 16, 16, 8, 16, 8))

    recolor()
  }

  fun recolor () {
    captionTextView.setTextColor(Theme.textDecentColor())
    captionTextView.highlightColor = Theme.textLinkHighlightColor()
  }

  fun attach () {
    stickerSmallView.attach()
  }

  fun detach () {
    stickerSmallView.detach()
  }

  fun setCaptionText (text: CharSequence?) {
    captionTextView.text = text
  }

  fun init (tdlib: Tdlib) {
    stickerSmallView.init(tdlib)
  }

  fun setSticker (tgStickerObj: TGStickerObj?) {
    if (tgStickerObj != null && !tgStickerObj.isEmpty) {
      tgStickerObj.previewAnimation.setPlayOnce(false)
    }

    stickerSmallView.setSticker(tgStickerObj)
  }

  override fun performDestroy () {
    stickerSmallView.performDestroy()
  }

  override fun onThemeInvalidate (isTempUpdate: Boolean) {
    recolor()
    invalidate()
  }
}
