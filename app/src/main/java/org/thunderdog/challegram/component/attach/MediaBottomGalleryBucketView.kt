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
 * File created on 24/10/2016
 */
package org.thunderdog.challegram.component.attach

import android.content.Context
import android.graphics.Canvas
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView

import androidx.recyclerview.widget.RecyclerView

import org.thunderdog.challegram.core.Media
import org.thunderdog.challegram.loader.ImageReceiver
import org.thunderdog.challegram.support.RippleSupport
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Fonts
import org.thunderdog.challegram.tool.Paints
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.tool.Views
import org.thunderdog.challegram.widget.NoScrollTextView

import me.vkryl.android.widget.FrameLayoutFix
import me.vkryl.core.lambda.Destroyable

class MediaBottomGalleryBucketView (context: Context) : FrameLayoutFix(context), Destroyable {
  private val receiver: ImageReceiver
  private val textView: TextView

  var bucket: Media.GalleryBucket? = null
    set (value) {
      if (field == null || field?.id != value?.id) {
        field = value
        textView.text = value?.name
        receiver.requestFile(value?.previewImage)
      }
    }

  init {
    val paddingTop = Screen.dp(9f)
    val paddingLeft = Screen.dp(8f)
    val imageSize = Screen.dp(30f)

    receiver = ImageReceiver(this, 0)
    receiver.setBounds(paddingLeft, paddingTop, paddingLeft + imageSize, paddingTop + imageSize)

    val params = newParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.LEFT or Gravity.CENTER_VERTICAL)
    params.leftMargin = paddingLeft + imageSize + Screen.dp(17f)
    params.rightMargin = paddingLeft
    textView = NoScrollTextView(context)
    textView.setTextColor(Theme.textAccentColor())
    textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
    textView.setTypeface(Fonts.getRobotoRegular())
    textView.setSingleLine(true)
    textView.setEllipsize(TextUtils.TruncateAt.END)
    textView.setLayoutParams(params)
    addView(textView)

    setWillNotDraw(false)
    Views.setClickable(this)
    RippleSupport.setSimpleWhiteBackground(this)

    setLayoutParams(RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, paddingTop + imageSize + paddingTop))
  }

  fun attach () {
    receiver.attach()
  }

  fun detach () {
    receiver.detach()
  }

  override fun onDraw (c: Canvas) {
    if (receiver.needPlaceholder()) {
      c.drawRect(receiver.getLeft().toFloat(), receiver.getTop().toFloat(), receiver.getRight().toFloat(), receiver.getBottom().toFloat(), Paints.getPlaceholderPaint())
    }
    receiver.draw(c)
  }

  override fun performDestroy () {
    receiver.requestFile(null)
  }
}
