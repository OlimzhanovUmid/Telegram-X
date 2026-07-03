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
 * File created on 18/08/2023
 */
package org.thunderdog.challegram.widget

import android.content.Context
import android.text.TextUtils
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import org.thunderdog.challegram.R
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.data.TGStickerSetInfo
import org.thunderdog.challegram.navigation.ViewController
import org.thunderdog.challegram.support.ViewSupport
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Fonts
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.tool.Views
import org.thunderdog.challegram.util.text.Highlight

class TrendingPackHeaderView (context: Context) : RelativeLayout(context) {
  private val newView: TextView
  private val button: NonMaterialButton
  private val titleView: TextView
  private val subtitleView: TextView

  init {
    var params: RelativeLayout.LayoutParams
    params = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, Screen.dp(16f))
    params.addRule(Lang.alignParent())
    if (Lang.rtl()) {
      params.leftMargin = Screen.dp(6f)
    } else {
      params.rightMargin = Screen.dp(6f)
    }
    params.topMargin = Screen.dp(3f)
    newView = NoScrollTextView(context)
    newView.id = R.id.btn_new
    newView.setSingleLine(true)
    newView.setPadding(Screen.dp(4f), Screen.dp(1f), Screen.dp(4f), 0)
    newView.setTextColor(Theme.getColor(ColorId.promoContent))
    newView.typeface = Fonts.getRobotoBold()
    newView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10f)
    newView.text = Lang.getString(R.string.New).uppercase()
    newView.layoutParams = params

    params = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, Screen.dp(28f))
    if (Lang.rtl()) {
      params.rightMargin = Screen.dp(16f)
    } else {
      params.leftMargin = Screen.dp(16f)
    }
    params.topMargin = Screen.dp(5f)
    params.addRule(if (Lang.rtl()) RelativeLayout.ALIGN_PARENT_LEFT else RelativeLayout.ALIGN_PARENT_RIGHT)
    button = NonMaterialButton(context)
    button.id = R.id.btn_addStickerSet
    button.setText(R.string.Add)
    button.layoutParams = params

    params = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, Screen.dp(28f))
    if (Lang.rtl()) {
      params.rightMargin = Screen.dp(16f)
    } else {
      params.leftMargin = Screen.dp(16f)
    }
    params.topMargin = Screen.dp(5f)
    params.addRule(if (Lang.rtl()) RelativeLayout.ALIGN_PARENT_LEFT else RelativeLayout.ALIGN_PARENT_RIGHT)
    params.width = Screen.dp(16f)
    params.height = params.width

    params = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    if (Lang.rtl()) {
      params.leftMargin = Screen.dp(12f)
      params.addRule(RelativeLayout.LEFT_OF, R.id.btn_new)
      params.addRule(RelativeLayout.RIGHT_OF, R.id.btn_addStickerSet)
    } else {
      params.rightMargin = Screen.dp(12f)
      params.addRule(RelativeLayout.RIGHT_OF, R.id.btn_new)
      params.addRule(RelativeLayout.LEFT_OF, R.id.btn_addStickerSet)
    }
    titleView = NoScrollTextView(context)
    titleView.typeface = Fonts.getRobotoMedium()
    titleView.setTextColor(Theme.textAccentColor())
    titleView.gravity = Lang.gravity()
    titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
    titleView.setSingleLine(true)
    titleView.ellipsize = TextUtils.TruncateAt.END
    titleView.layoutParams = params

    params = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    if (Lang.rtl()) {
      params.leftMargin = Screen.dp(12f)
      params.addRule(RelativeLayout.LEFT_OF, R.id.btn_new)
      params.addRule(RelativeLayout.RIGHT_OF, R.id.btn_addStickerSet)
    } else {
      params.rightMargin = Screen.dp(12f)
      params.addRule(RelativeLayout.RIGHT_OF, R.id.btn_new)
      params.addRule(RelativeLayout.LEFT_OF, R.id.btn_addStickerSet)
    }

    params = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    params.addRule(Lang.alignParent())
    params.topMargin = Screen.dp(22f)
    subtitleView = NoScrollTextView(context)
    subtitleView.typeface = Fonts.getRobotoRegular()
    subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f)
    subtitleView.setTextColor(Theme.textDecentColor())
    subtitleView.setSingleLine(true)
    subtitleView.ellipsize = TextUtils.TruncateAt.END
    subtitleView.layoutParams = params

    addView(newView)
    addView(button)
    addView(titleView)
    addView(subtitleView)
  }

  fun setButtonOnClickListener (listener: View.OnClickListener?) {
    button.setOnClickListener(listener)
  }

  fun setThemeProvider (themeProvider: ViewController<*>?) {
    if (themeProvider != null) {
      themeProvider.addThemeTextColorListener(newView, ColorId.promoContent)
      themeProvider.addThemeInvalidateListener(newView)
      themeProvider.addThemeInvalidateListener(this)
      themeProvider.addThemeInvalidateListener(button)
      themeProvider.addThemeTextAccentColorListener(titleView)
      themeProvider.addThemeTextDecentColorListener(subtitleView)
      ViewSupport.setThemedBackground(newView, ColorId.promo, themeProvider).setCornerRadius(3f)
    }
  }

  fun setStickerSetInfo (stickerSet: TGStickerSetInfo?, highlight: String?, isInProgress: Boolean, isNew: Boolean) {
    tag = stickerSet

    newView.visibility = if (!isNew) View.GONE else View.VISIBLE
    button.setInProgress(stickerSet != null && !stickerSet.isRecent && isInProgress, false)
    button.setIsDone(stickerSet != null && stickerSet.isInstalled, false)
    button.tag = stickerSet

    Views.setMediumText(titleView, Highlight.toSpannable(if (stickerSet != null) stickerSet.title else "", highlight))
    subtitleView.text = if (stickerSet != null) Lang.plural(if (stickerSet.isEmoji) R.string.xEmoji else R.string.xStickers, stickerSet.fullSize.toLong()) else ""

    if (Views.setAlignParent(newView, Lang.rtl())) {
      val rightMargin = Screen.dp(6f)
      val topMargin = Screen.dp(3f)
      Views.setMargins(newView, if (Lang.rtl()) rightMargin else 0, topMargin, if (Lang.rtl()) 0 else rightMargin, 0)
      Views.updateLayoutParams(newView)
    }

    if (Views.setAlignParent(button, if (Lang.rtl()) RelativeLayout.ALIGN_PARENT_LEFT else RelativeLayout.ALIGN_PARENT_RIGHT)) {
      val leftMargin = Screen.dp(16f)
      val topMargin = Screen.dp(5f)
      Views.setMargins(button, if (Lang.rtl()) 0 else leftMargin, topMargin, if (Lang.rtl()) leftMargin else 0, 0)
      Views.updateLayoutParams(button)
    }
    val params = titleView.layoutParams as RelativeLayout.LayoutParams
    if (Lang.rtl()) {
      val leftMargin = Screen.dp(12f)
      if (params.leftMargin != leftMargin) {
        params.leftMargin = leftMargin
        params.rightMargin = 0
        params.addRule(RelativeLayout.LEFT_OF, R.id.btn_new)
        params.addRule(RelativeLayout.RIGHT_OF, R.id.btn_addStickerSet)
        Views.updateLayoutParams(titleView)
      }
    } else {
      val rightMargin = Screen.dp(12f)
      if (params.rightMargin != rightMargin) {
        params.rightMargin = rightMargin
        params.leftMargin = 0
        params.addRule(RelativeLayout.RIGHT_OF, R.id.btn_new)
        params.addRule(RelativeLayout.LEFT_OF, R.id.btn_addStickerSet)
        Views.updateLayoutParams(titleView)
      }
    }
    Views.setTextGravity(titleView, Lang.gravity())
    if (Views.setAlignParent(subtitleView, Lang.rtl())) {
      Views.updateLayoutParams(subtitleView)
    }
  }
}
