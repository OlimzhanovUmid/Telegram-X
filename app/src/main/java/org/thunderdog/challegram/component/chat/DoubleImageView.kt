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
 * File created on 05/12/2016
 */
package org.thunderdog.challegram.component.chat

import android.content.Context
import android.graphics.Canvas
import android.view.View.MeasureSpec
import org.thunderdog.challegram.loader.DoubleImageReceiver
import org.thunderdog.challegram.loader.ImageFile
import org.thunderdog.challegram.loader.ImageReceiver
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.widget.BaseView

class DoubleImageView (context: Context) : BaseView(context, null) {
  private val preview: DoubleImageReceiver = DoubleImageReceiver(this, 0)
  private val receiver: ImageReceiver = ImageReceiver(this, 0)

  fun setImage (miniThumbnail: ImageFile?, previewFile: ImageFile?, targetFile: ImageFile?) {
    preview.requestFile(miniThumbnail, previewFile)
    receiver.requestFile(targetFile)
  }

  fun attach () {
    preview.attach()
    receiver.attach()
  }

  fun detach () {
    preview.detach()
    receiver.detach()
  }

  override fun onMeasure (widthMeasureSpec: Int, heightMeasureSpec: Int) {
    setMeasuredDimension(getDefaultSize(suggestedMinimumWidth, widthMeasureSpec),
      MeasureSpec.makeMeasureSpec(Screen.dp(118f), MeasureSpec.EXACTLY))
    preview.setBounds(0, 0, measuredWidth, measuredHeight)
    receiver.setBounds(0, 0, measuredWidth, measuredHeight)
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
