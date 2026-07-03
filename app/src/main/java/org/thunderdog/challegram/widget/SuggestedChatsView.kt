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
 * File created on 19/01/2024
 */
package org.thunderdog.challegram.widget

import android.content.Context
import android.graphics.Canvas
import android.view.Gravity
import android.view.View

import androidx.annotation.Dimension

import org.thunderdog.challegram.R
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.data.TD
import org.thunderdog.challegram.data.TGAvatars
import org.thunderdog.challegram.loader.ComplexReceiver
import org.thunderdog.challegram.support.RippleSupport
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Drawables
import org.thunderdog.challegram.tool.Paints
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.tool.Views
import org.thunderdog.challegram.util.text.Text
import org.thunderdog.challegram.util.text.TextEntity

import me.vkryl.core.lambda.Destroyable

class SuggestedChatsView (context: Context, private val tdlib: Tdlib) : View(context), TGAvatars.Callback, AttachDelegate, Destroyable {
  companion object {
    @Dimension(unit = Dimension.DP)
    const val DEFAULT_HEIGHT_DP = 36f
  }

  private val complexReceiver = ComplexReceiver(this)
  private val avatars = TGAvatars(tdlib, this, this)
  private val icon = Drawables.get(context.resources, R.drawable.baseline_info_16)
  private val text = Text.Builder("", Int.MAX_VALUE, Paints.robotoStyleProvider(15f), Theme::textAccent2Color)
    .singleLine()
    .build()

  init {
    setMinimumHeight(Screen.dp(DEFAULT_HEIGHT_DP))
    Views.setClickable(this)
    RippleSupport.setTransparentSelector(this)
  }

  fun setChatIds (chatIds: LongArray, animated: Boolean) {
    avatars.setChatIds(chatIds.copyOf(Math.min(3, chatIds.size)), animated)

    val chatCount = chatIds.size
    if (chatCount > 0) {
      val inText = Lang.plural(R.string.xNewChatsAvailableToJoin, chatCount.toLong())
      val formattedText = TD.toFormattedText(inText, false)
      if (TD.parseMarkdownWithEntities(formattedText) && formattedText.entities.size > 0) {
        val entities = TextEntity.valueOf(tdlib, formattedText, null)
        for (rawEntity in entities!!) {
          val entity = rawEntity!!
          if (entity.isBold) {
            entity.setCustomColorSet(Theme::textLinkColor)
          }
        }
        text.set(getTextMaxWidth(), formattedText.text, entities)
      } else {
        text.set(getTextMaxWidth(), inText, null)
      }
    }
  }

  override fun onDraw (canvas: Canvas) {
    val cy = height / 2
    avatars.draw(canvas, complexReceiver, Screen.dp(7f), cy, Gravity.LEFT, 1f)

    val textX = Math.round(avatars.getAnimatedWidth() + avatars.getAvatarsVisibility() * Screen.dp(7f)) + Screen.dp(7f)
    val textY = cy - text.getLineCenterY()
    text.draw(canvas, textX, textY)

    Drawables.drawCentered(canvas, icon, (width - Screen.dp(20f)).toFloat(), cy.toFloat(), Paints.getBackgroundIconPorterDuffPaint())
  }

  override fun onSizeChanged () {
    if (isLaidOut) {
      text.changeMaxWidth(getTextMaxWidth())
    }
    invalidate()
  }

  override fun onSizeChanged (width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
    super.onSizeChanged(width, height, oldWidth, oldHeight)
    if (width != oldWidth) {
      text.changeMaxWidth(getTextMaxWidth())
      invalidate()
    }
  }

  override fun onInvalidateMedia (avatars: TGAvatars) {
    avatars.requestFiles(complexReceiver, /* isUpdate */ true, /* neverClear */ false)
  }

  override fun attach () {
    complexReceiver.attach()
  }

  override fun detach () {
    complexReceiver.detach()
  }

  override fun performDestroy () {
    text.performDestroy()
    complexReceiver.performDestroy()
  }

  private fun getTextMaxWidth (): Int {
    if (!isLaidOut) {
      return Int.MAX_VALUE
    }
    return width - Math.round(avatars.getAnimatedWidth()) - Screen.dp(54f)
  }
}
