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
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.view.View

import org.drinkless.tdlib.TdApi
import org.thunderdog.challegram.component.dialogs.ChatView
import org.thunderdog.challegram.data.AvatarPlaceholder
import org.thunderdog.challegram.data.TD
import org.thunderdog.challegram.loader.ImageFile
import org.thunderdog.challegram.loader.ImageReceiver
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Paints
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.tool.Views

import me.vkryl.core.lambda.Destroyable

// TODO complete rework, similar to avatars in TGMessagePoll
class TripleAvatarView (context: Context) : View(context), Destroyable {
  companion object {
    const val AVATAR_SIZE = 24
    const val AVATAR_PADDING = 2
  }

  private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG)

  private val receivers = Array(3) { createReceiver() }
  private val measureRect = Rect()

  private val placeholders = arrayOfNulls<AvatarPlaceholder>(3)
  private var ignoranceFlags = 0

  init {
    setWillNotDraw(false)
    clearPaint.color = 0
    clearPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
  }

  private fun createReceiver (): ImageReceiver {
    val receiver = ImageReceiver(this, 1)
    receiver.setRadius(0f)
    receiver.attach()
    return receiver
  }

  fun setUsers (tdlib: Tdlib, viewers: TdApi.MessageViewers) {
    val ids = LongArray(viewers.viewers.size)
    for (i in viewers.viewers.indices) {
      val viewer = viewers.viewers[i]
      ids[i] = viewer.userId
    }

    for (i in receivers.indices) {
      requestUserFile(ids, i, tdlib, receivers[i])
    }

    syncReceivers(ids)
  }

  private fun syncReceivers (users: LongArray) {
    if (users.size == 1) {
      placeholders[2] = placeholders[0]
      receivers[2].requestFile(receivers[0].getCurrentFile())
      ignoranceFlags = 1
    } else if (users.size == 2) {
      placeholders[2] = placeholders[1]
      placeholders[1] = placeholders[0]
      receivers[2].requestFile(receivers[1].getCurrentFile())
      receivers[1].requestFile(receivers[0].getCurrentFile())
      ignoranceFlags = 2
    } else if (users.isEmpty()) {
      ignoranceFlags = 3
    } else {
      ignoranceFlags = 0
    }

    invalidate()
  }

  private fun requestUserFile (users: LongArray, index: Int, tdlib: Tdlib, receiver: ImageReceiver) {
    if (users.size > index) {
      val user = tdlib.chatUser(users[index])

      if (user == null || TD.isPhotoEmpty(user.profilePhoto)) {
        placeholders[index] = AvatarPlaceholder(AVATAR_SIZE / 2f, AvatarPlaceholder.Metadata(tdlib.cache().userAccentColor(user), TD.getLetters(user)), null)
        receiver.requestFile(null)
      } else {
        placeholders[index] = null
        val chatAvatar = ImageFile(tdlib, user.profilePhoto!!.small)
        chatAvatar.setSize(ChatView.getDefaultAvatarCacheSize())
        receiver.requestFile(chatAvatar)
      }
    } else {
      placeholders[index] = null
      receiver.requestFile(null)
    }
  }

  override fun onMeasure (widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)

    val left = paddingLeft
    var top = paddingTop
    val right = measuredWidth - paddingRight
    var bottom = measuredHeight - paddingBottom
    val sizeDp = Screen.dp(AVATAR_SIZE.toFloat())
    val halfSizeDp = sizeDp / 2f
    val evenHalfSizeDp = sizeDp / 4f

    measureRect.set(left, top, right, bottom)
    top = (measureRect.centerY() - halfSizeDp).toInt()
    bottom = (measureRect.centerY() + halfSizeDp).toInt()
    setReceiverBounds(receivers[0], (measureRect.centerX() - (halfSizeDp * 3) + evenHalfSizeDp).toInt(), (measureRect.centerX() - (halfSizeDp) + evenHalfSizeDp).toInt(), top, bottom)
    setReceiverBounds(receivers[1], (measureRect.centerX() - (halfSizeDp)).toInt(), (measureRect.centerX() + (halfSizeDp)).toInt(), top, bottom)
    setReceiverBounds(receivers[2], (measureRect.centerX() + (halfSizeDp) - evenHalfSizeDp).toInt(), (measureRect.centerX() + (halfSizeDp * 3) - evenHalfSizeDp).toInt(), top, bottom)
  }

  override fun onDraw (canvas: Canvas) {
    Views.saveLayerAlpha(canvas, 0f, 0f, measuredWidth.toFloat(), measuredHeight.toFloat(), 255, Canvas.ALL_SAVE_FLAG)
    for (i in (receivers.size - 1) downTo 0) {
      drawReceiver(canvas, i, receivers[i])
    }
    canvas.restore()
  }

  private fun setReceiverBounds (receiver: ImageReceiver, left: Int, right: Int, top: Int, bottom: Int) {
    receiver.setBounds(left, top, right, bottom)
    receiver.setRadius(Math.min(receiver.getWidth(), receiver.getHeight()) / 2f)
  }

  private fun drawReceiver (c: Canvas, index: Int, receiver: ImageReceiver) {
    if ((index == 0 && ignoranceFlags == 2) || (index != 2 && ignoranceFlags == 1) || ignoranceFlags == 3)
      return
    drawPlaceholder(c, receiver)
    placeholders[index]?.draw(c, receiver.centerX().toFloat(), receiver.centerY().toFloat())
    receiver.draw(c)
  }

  private fun drawPlaceholder (c: Canvas, receiver: ImageReceiver) {
    c.drawCircle(receiver.centerX().toFloat(), receiver.centerY().toFloat(), receiver.getRadius() + Screen.dp(AVATAR_PADDING.toFloat()), clearPaint)
    c.drawCircle(receiver.centerX().toFloat(), receiver.centerY().toFloat(), receiver.getRadius(), Paints.fillingPaint(Theme.placeholderColor()))
  }

  override fun performDestroy () {
    for (receiver in receivers) {
      receiver.destroy()
    }
  }
}
