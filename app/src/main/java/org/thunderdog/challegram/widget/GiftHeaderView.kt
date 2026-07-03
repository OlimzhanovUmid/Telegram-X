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
 * File created on 05/01/2023
 */
package org.thunderdog.challegram.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.text.TextPaint
import android.view.View

import androidx.annotation.StringRes

import org.thunderdog.challegram.R
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.data.TD
import org.thunderdog.challegram.data.TGMessageGiveawayBase
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Fonts
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.unsorted.Settings
import org.thunderdog.challegram.util.GiftParticlesDrawable
import org.thunderdog.challegram.util.text.Text
import org.thunderdog.challegram.util.text.TextStyleProvider
import org.thunderdog.challegram.util.text.TextWrapper

class GiftHeaderView (context: Context) : View(context), GiftParticlesDrawable.ParticleValidator {
  private val particlesDrawable: GiftParticlesDrawable
  private var content: TGMessageGiveawayBase.Content? = null
  private var contentY = 0

  private @StringRes var headerRes = R.string.GiftLink
  private @StringRes var descriptionRes = R.string.GiftLinkDesc

  init {
    particlesDrawable = GiftParticlesDrawable()
    particlesDrawable.setParticleValidator(this)
  }

  fun setTexts (@StringRes headerRes: Int, @StringRes descriptionRes: Int) {
    if (this.headerRes != headerRes || this.descriptionRes != descriptionRes) {
      this.headerRes = headerRes
      this.descriptionRes = descriptionRes
      if (measuredWidth > 0) {
        buildContent()
      }
    }
  }

  override fun onMeasure (widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    buildContent()
    particlesDrawable.setBounds(0, 0, measuredWidth, measuredHeight)
  }

  private fun buildContent () {
    content = TGMessageGiveawayBase.Content(measuredWidth - Screen.dp(120f))
    content!!.padding(Screen.dp(10f))
    content!!.add(TGMessageGiveawayBase.ContentDrawable(R.drawable.baseline_gift_72))
    content!!.padding(Screen.dp(22f))
    content!!.add(TextWrapper(Lang.getString(headerRes), getHeaderStyleProvider()) { Theme.getColor(ColorId.text) }.setTextFlagEnabled(Text.FLAG_ALIGN_CENTER, true))
    content!!.padding(Screen.dp(8f))
    content!!.add(TextWrapper(null, TD.toFormattedText(Lang.getMarkdownString(null, descriptionRes), false), getTextStyleProvider(), { Theme.getColor(ColorId.text) }, null, null).setTextFlagEnabled(Text.FLAG_ALIGN_CENTER, true))
    contentY = (measuredHeight - content!!.getHeight()) / 2
  }

  override fun isValidPosition (x: Float, y: Float): Boolean {
    return content == null || content!!.isValidPosition(x - Screen.dp(60f), y - contentY)
  }

  override fun onDraw (canvas: Canvas) {
    super.onDraw(canvas)
    particlesDrawable.draw(canvas)

    content!!.draw(canvas, null, Screen.dp(60f), contentY)
  }

  companion object {
    @JvmStatic
    fun getDefaultHeight (): Int {
      return Screen.dp(231f)
    }

    private var headerStyleProvider: TextStyleProvider? = null

    private fun getHeaderStyleProvider (): TextStyleProvider {
      if (headerStyleProvider == null) {
        val tp = TextPaint(Paint.ANTI_ALIAS_FLAG)
        tp.typeface = Fonts.getRobotoMedium()
        headerStyleProvider = TextStyleProvider(tp).setTextSize(20f).setAllowSp(true)
        Settings.instance().addChatFontSizeChangeListener(headerStyleProvider)
      }
      return headerStyleProvider!!
    }

    private var textStyleProvider: TextStyleProvider? = null

    private fun getTextStyleProvider (): TextStyleProvider {
      if (textStyleProvider == null) {
        textStyleProvider = TextStyleProvider(Fonts.newRobotoStorage()).setTextSize(15f).setAllowSp(true)
        Settings.instance().addChatFontSizeChangeListener(textStyleProvider)
      }
      return textStyleProvider!!
    }
  }
}
