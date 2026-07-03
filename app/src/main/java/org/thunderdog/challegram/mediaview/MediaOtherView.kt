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
 * File created on 10/12/2016
 */
package org.thunderdog.challegram.mediaview

import android.content.Context
import android.graphics.Canvas
import android.view.Gravity
import android.view.View

import org.thunderdog.challegram.R
import org.thunderdog.challegram.loader.ImageFile
import org.thunderdog.challegram.loader.ImageReceiver
import org.thunderdog.challegram.tool.Paints
import org.thunderdog.challegram.tool.Screen

import me.vkryl.android.widget.FrameLayoutFix
import me.vkryl.core.lambda.Destroyable

class MediaOtherView (context: Context) : FrameLayoutFix(context), Destroyable {
  private val receiver: ImageReceiver = ImageReceiver(this, 0)
  private var imageFile: ImageFile? = null

  init {
    setWillNotDraw(false)

    val padding = Screen.dp(4f)
    val paddingBig = Screen.dp(16f)
    val params = FrameLayoutFix.newParams(Screen.dp(24f) + padding + paddingBig, Screen.dp(24f) + padding + paddingBig, Gravity.RIGHT or Gravity.TOP)

    val view = DeleteView(context)
    view.id = R.id.btn_removePhoto
    view.layoutParams = params
    view.setPadding(paddingBig, padding, padding, paddingBig)
    addView(view)
  }

  fun setOnDeleteClick (onDeleteClick: View.OnClickListener) {
    getChildAt(0).setOnClickListener(onDeleteClick)
  }

  fun attach () {
    receiver.attach()
  }

  fun detach () {
    receiver.detach()
  }

  override fun performDestroy () {
    receiver.requestFile(null)
  }

  override fun onMeasure (widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(MeasureSpec.makeMeasureSpec(Screen.dp(100f), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(Screen.dp(100f), MeasureSpec.EXACTLY))
    receiver.setBounds(0, 0, measuredWidth, measuredHeight)
  }

  fun setImage (image: ImageFile?) {
    this.imageFile = image
    receiver.requestFile(image)
  }

  fun getImage (): ImageFile? {
    return imageFile
  }

  override fun onDraw (c: Canvas) {
    if (receiver.needPlaceholder()) {
      c.drawRect(receiver.getLeft().toFloat(), receiver.getTop().toFloat(), receiver.getRight().toFloat(), receiver.getBottom().toFloat(), Paints.fillingPaint(0x22ffffff))
    }
    receiver.draw(c)
  }

  private class DeleteView (context: Context) : View(context) {
    override fun onDraw (c: Canvas) {
      val cx = paddingLeft + (measuredWidth - paddingLeft - paddingRight) / 2
      val cy = paddingTop + (measuredHeight - paddingLeft - paddingRight) / 2

      c.drawCircle(cx.toFloat(), cy.toFloat(), Screen.dp(12f).toFloat(), Paints.fillingPaint(0xffffffff.toInt()))
      c.drawCircle(cx.toFloat(), cy.toFloat(), Screen.dp(10f).toFloat(), Paints.fillingPaint(0xffe45356.toInt()))

      val width = Screen.dp(5f)
      val height = Screen.dp(3f)
      c.drawRect((cx - width).toFloat(), (cy - height / 2).toFloat(), (cx + width).toFloat(), (cy + height / 2 + height % 2).toFloat(), Paints.fillingPaint(0xffffffff.toInt()))
    }
  }
}
