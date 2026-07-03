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
package org.thunderdog.challegram.component.chat

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.Gravity

import org.drinkless.tdlib.TdApi
import org.thunderdog.challegram.R
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.data.TD
import org.thunderdog.challegram.loader.DoubleImageReceiver
import org.thunderdog.challegram.loader.ImageFile
import org.thunderdog.challegram.loader.ImageFileLocal
import org.thunderdog.challegram.navigation.HeaderView
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.telegram.TdlibAccentColor
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.tool.Drawables
import org.thunderdog.challegram.tool.Paints
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.unsorted.Size
import org.thunderdog.challegram.util.text.Text
import org.thunderdog.challegram.util.text.TextColorSets
import org.thunderdog.challegram.widget.SparseDrawableView

import me.vkryl.android.ScrimUtil
import me.vkryl.core.lambda.Destroyable
import me.vkryl.core.alphaColor

class DetachedChatHeaderView (context: Context) : SparseDrawableView(context), Destroyable {
  private val avatar = DoubleImageReceiver(this, 0)

  private lateinit var accentColor: TdlibAccentColor
  private var avatarPlaceholderDrawable: Drawable? = null

  private val topShadow: Drawable
  private val bottomShadow: Drawable

  private lateinit var title: Text
  private lateinit var subtitle: Text

  init {
    setWillNotDraw(false)
    topShadow = ScrimUtil.makeCubicGradientScrimDrawable(0x77000000, 2, Gravity.TOP, false)
    bottomShadow = ScrimUtil.makeCubicGradientScrimDrawable(0x99000000.toInt(), 2, Gravity.BOTTOM, false)
  }

  companion object {
    @JvmStatic
    fun getViewHeight (): Int {
      return (HeaderView.getBigSize(false) * 0.7f).toInt()
    }
  }

  override fun onMeasure (widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    avatar.setBounds(0, 0, measuredWidth, measuredHeight)
    title.changeMaxWidth(measuredWidth - Screen.dp(13f))
    subtitle.changeMaxWidth(measuredWidth - Screen.dp(13f))
    topShadow.setBounds(0, 0, measuredWidth, HeaderView.getSize(false) + Size.getHeaderPlayerSize())
    bottomShadow.setBounds(0, 0, measuredWidth, getBottomShadowSize())
  }

  private fun getBottomShadowSize (): Int {
    return (((Screen.dp(28f) + Screen.dp(5f) + ((title.getHeight() + subtitle.getHeight()) * 1.3f) + Screen.dp(8f)) + Screen.dp(14f)) * (1f / .9f)).toInt()
  }

  fun bindWith (tdlib: Tdlib, linkInfo: TdApi.ChatInviteLinkInfo) {
    val isChannel = TD.isChannel(linkInfo.type)

    title = Text.Builder(linkInfo.title, measuredWidth, Paints.robotoStyleProvider(18f), TextColorSets.WHITE)
      .singleLine()
      .clipTextArea()
      .allBold()
      .build()
    accentColor = tdlib.chatAccentColor(linkInfo.chatId)

    subtitle = Text.Builder(Lang.pluralMembers(linkInfo.memberCount, 0, isChannel).toString(), measuredWidth, Paints.robotoStyleProvider(14f), TextColorSets.WHITE).singleLine().build()

    val photo = linkInfo.photo
    if (photo != null) {
      val file: ImageFileLocal?

      val minithumbnail = photo.minithumbnail
      if (minithumbnail != null) {
        file = ImageFileLocal(minithumbnail)
        file.setSize(height)
        file.setDecodeSquare(true)
        file.setScaleType(ImageFile.CENTER_CROP)
      } else {
        file = null
      }

      val fileNetwork = ImageFile(tdlib, photo.big)
      fileNetwork.setSize(height)
      fileNetwork.setScaleType(ImageFile.CENTER_CROP)

      avatarPlaceholderDrawable = null
      avatar.requestFile(file, fileNetwork)
    } else {
      avatarPlaceholderDrawable = getSparseDrawable(if (isChannel) R.drawable.baseline_bullhorn_56 else R.drawable.baseline_group_56, ColorId.NONE)
      avatar.clear()
    }
  }

  fun attach () {
    avatar.attach()
  }

  fun detach () {
    avatar.detach()
  }

  override fun performDestroy () {
    avatar.destroy()
  }

  override fun onDraw (canvas: Canvas) {
    val placeholderDrawable = avatarPlaceholderDrawable
    if (placeholderDrawable != null) {
      val cx = avatar.centerX()
      val cy = avatar.centerY()

      canvas.drawColor(accentColor.getPrimaryColor())
      val placeholderScale = (avatar.getWidth() / 2f / (measuredWidth.toFloat() / 2f))
      canvas.save()
      canvas.scale(placeholderScale, placeholderScale, cx.toFloat(), cy.toFloat())
      Drawables.draw(canvas, placeholderDrawable, cx - placeholderDrawable.minimumWidth / 2f, cy - placeholderDrawable.minimumHeight / 2f, Paints.getPorterDuffPaint(alphaColor(.3f, Color.WHITE)))
      canvas.restore()
    } else {
      avatar.draw(canvas)

      topShadow.alpha = (255f * .8f).toInt()
      topShadow.draw(canvas)

      canvas.save()
      val shadowTop = measuredHeight - getBottomShadowSize()
      canvas.translate(0f, Math.max(shadowTop, topShadow.bounds.bottom - Screen.dp(28f)).toFloat())
      bottomShadow.alpha = (255f * .8f).toInt()
      bottomShadow.draw(canvas)
      canvas.restore()
    }

    val baseTextLeft = Screen.dp(13f).toFloat()
    val baseSubtitleTop = title.getHeight() + Screen.dp(4f)
    val baseTitleTop = (measuredHeight - (title.getHeight() + subtitle.getHeight() + Screen.dp(13f))).toFloat()

    canvas.save()
    canvas.translate(baseTextLeft, baseTitleTop)
    title.draw(canvas, 0, 0, null, 1f)
    subtitle.draw(canvas, 0, baseSubtitleTop, null, 1f)
    canvas.restore()
  }
}
