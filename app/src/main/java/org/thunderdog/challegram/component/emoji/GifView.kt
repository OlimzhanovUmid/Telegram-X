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
 * File created on 27/02/2016 at 21:59
 */
package org.thunderdog.challegram.component.emoji

import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View.MeasureSpec

import org.thunderdog.challegram.data.TGGif
import org.thunderdog.challegram.loader.ImageReceiver
import org.thunderdog.challegram.loader.gif.GifReceiver
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.widget.BaseView

import me.vkryl.android.util.ClickHelper

class GifView (context: Context) : BaseView(context, null) {
  private val receiver: GifReceiver = GifReceiver(this)
  private val preview: ImageReceiver = ImageReceiver(this, 0)

  var gif: TGGif? = null
    set (value) {
      if (field == null || value == null || field!!.id != value.id) {
        field = value
        preview.requestFile(value?.image)
        receiver.requestFile(value?.gif)
      }
      if (getDesiredHeight() != measuredHeight) {
        requestLayout()
      }
    }

  private var helper: ClickHelper? = null

  fun initWithDelegate (delegate: ClickHelper.Delegate) {
    helper = ClickHelper(delegate)
  }

  override fun onTouchEvent (event: MotionEvent): Boolean {
    return helper?.onTouchEvent(this, event) ?: super.onTouchEvent(event)
  }

  private fun getDesiredHeight (): Int {
    return Screen.dp(118f)
  }

  override fun onMeasure (widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(getDesiredHeight(), MeasureSpec.EXACTLY))
    preview.setBounds(0, 0, measuredWidth, measuredHeight)
    receiver.setBounds(0, 0, measuredWidth, measuredHeight)
  }

  fun attach () {
    preview.attach()
    receiver.attach()
  }

  fun detach () {
    preview.detach()
    receiver.detach()
  }

  fun destroy () {
    preview.requestFile(null)
    receiver.requestFile(null)
  }

  override fun onDraw (c: Canvas) {
    if (receiver.needPlaceholder()) {
      if (preview.needPlaceholder()) {
        preview.drawPlaceholder(c)
      }
      preview.draw(c)
    }
    receiver.draw(c)
  }
}
