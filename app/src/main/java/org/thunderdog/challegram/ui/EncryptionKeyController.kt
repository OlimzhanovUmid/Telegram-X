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
 * File created on 26/12/2016
 */
package org.thunderdog.challegram.ui

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import me.vkryl.android.widget.FrameLayoutFix
import me.vkryl.core.isEmpty
import org.thunderdog.challegram.R
import org.thunderdog.challegram.U
import org.thunderdog.challegram.component.chat.EncryptionKeyDrawable
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.emoji.Emoji
import org.thunderdog.challegram.navigation.BackHeaderButton
import org.thunderdog.challegram.navigation.ViewController
import org.thunderdog.challegram.support.ViewSupport
import org.thunderdog.challegram.telegram.TGLegacyManager
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Fonts
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.tool.Views
import org.thunderdog.challegram.util.CustomTypefaceSpan
import org.thunderdog.challegram.widget.NoScrollTextView
import org.thunderdog.challegram.widget.ShadowView

class EncryptionKeyController (context: Context, tdlib: Tdlib?) : ViewController<EncryptionKeyController.Args>(context, tdlib), TGLegacyManager.EmojiLoadListener {
  class Args (@JvmField val userId: Long, @JvmField val keyHash: ByteArray)

  private lateinit var keyHash: ByteArray

  override fun setArguments (args: Args) {
    super.setArguments(args)
    this.keyHash = args.keyHash
  }

  override fun getBackButton (): Int {
    return BackHeaderButton.TYPE_BACK
  }

  override fun getName (): CharSequence {
    return Lang.getString(R.string.EncryptionKey)
  }

  private var contentView: RelativeLayout? = null

  override fun supportsBottomInset (): Boolean {
    return true
  }

  override fun onBottomInsetChanged (extraBottomInset: Int, extraBottomInsetWithoutIme: Int, isImeInset: Boolean) {
    super.onBottomInsetChanged(extraBottomInset, extraBottomInsetWithoutIme, isImeInset)
    contentView?.let {
      Views.setPaddingBottom(it, extraBottomInset)
    }
  }

  override fun onCreateView (context: Context): View {
    val contentView = RelativeLayout(context)
    contentView.layoutParams = FrameLayoutFix.newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    Views.setPaddingBottom(contentView, extraBottomInset)
    ViewSupport.setThemedBackground(contentView, ColorId.background, this)
    this.contentView = contentView

    val keyView = object : FrameLayoutFix(context) {
      override fun onMeasure (widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)

        val spec = MeasureSpec.makeMeasureSpec(minOf(width, height), MeasureSpec.EXACTLY)
        super.onMeasure(spec, spec)
      }
    }
    keyView.id = R.id.btn_encryptionKey
    val padding = Screen.dp(12f)
    keyView.setPadding(padding, padding, padding, padding)
    ViewSupport.setThemedBackground(keyView, ColorId.filling, this)

    var params: RelativeLayout.LayoutParams

    params = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    params.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
    params.addRule(RelativeLayout.ALIGN_PARENT_TOP)
    keyView.layoutParams = params

    val keyImageView = ImageView(context)
    keyImageView.scaleType = ImageView.ScaleType.FIT_XY
    keyImageView.setImageDrawable(EncryptionKeyDrawable(getArgumentsStrict().keyHash))
    keyImageView.layoutParams = FrameLayoutFix.newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    keyView.addView(keyImageView)

    contentView.addView(keyView)

    // Shadows

    val bottomShadow = ShadowView(context)
    bottomShadow.setSimpleBottomTransparentShadow(true)
    params = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, bottomShadow.layoutParams.height)
    params.addRule(RelativeLayout.BELOW, R.id.btn_encryptionKey)
    bottomShadow.layoutParams = params
    contentView.addView(bottomShadow)

    val rightShadow = ShadowView(context)
    rightShadow.setSimpleRightShadow(true)
    params = RelativeLayout.LayoutParams(rightShadow.layoutParams.width, ViewGroup.LayoutParams.MATCH_PARENT)
    params.addRule(RelativeLayout.RIGHT_OF, R.id.btn_encryptionKey)
    rightShadow.layoutParams = params
    contentView.addView(rightShadow)

    val sequence = buildSequence()

    // Text

    params = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    params.addRule(RelativeLayout.BELOW, R.id.btn_encryptionKey)
    val bottomText = NoScrollTextView(context)
    bottomText.gravity = Gravity.CENTER
    bottomText.setPadding(padding, 0, padding, 0)
    bottomText.setTextColor(Theme.textDecent2Color())
    bottomText.text = sequence
    bottomText.layoutParams = params
    contentView.addView(bottomText)
    this.bottomText = bottomText

    params = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    params.addRule(RelativeLayout.RIGHT_OF, R.id.btn_encryptionKey)
    val rightText = NoScrollTextView(context)
    rightText.gravity = Gravity.CENTER
    rightText.setPadding(padding, padding, padding, padding)
    rightText.text = sequence
    rightText.setTextColor(Theme.textDecent2Color())
    rightText.layoutParams = params
    contentView.addView(rightText)
    this.rightText = rightText

    TGLegacyManager.instance().addEmojiListener(this)

    return contentView
  }

  private var bottomText: TextView? = null
  private var rightText: TextView? = null

  override fun destroy () {
    super.destroy()
    TGLegacyManager.instance().removeEmojiListener(this)
  }

  override fun onEmojiUpdated (isPackSwitch: Boolean) {
    bottomText?.invalidate()
    rightText?.invalidate()
  }

  private fun buildSequence (): CharSequence {
    val text = Emoji.instance().replaceEmoji(Lang.getStringBold(R.string.EncryptionKeyDescription, tdlib!!.cache().userFirstName(getArgumentsStrict().userId)))

    val b: SpannableStringBuilder = if (text is SpannableStringBuilder) text else SpannableStringBuilder(text)

    val hash = U.buildHex(keyHash)
    if (!isEmpty(hash)) {
      if (b.length > 0)
        b.insert(0, "\n")
      b.insert(0, hash)
      val span = CustomTypefaceSpan(Fonts.getRobotoMono(), ColorId.background_textLight)
      addThemeTextColorListener(span, ColorId.background_textLight)
      b.setSpan(span, 0, hash.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    return b
  }

  override fun getId (): Int {
    return R.id.controller_encryptionKey
  }
}
