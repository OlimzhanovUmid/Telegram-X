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
 * File created on 08/06/2024
 */
package org.thunderdog.challegram.widget

import android.content.Context
import android.text.style.ClickableSpan
import android.view.ViewGroup
import android.widget.LinearLayout

import org.drinkless.tdlib.TdApi
import org.thunderdog.challegram.R
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.emoji.EmojiCodes
import org.thunderdog.challegram.navigation.ViewController
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.tool.UI
import org.thunderdog.challegram.util.text.FormattedText
import org.thunderdog.challegram.util.text.TextEntity

import tgx.td.toStickerSetInfo

@Suppress("ViewConstructor")
class EmojiStatusInfoView (context: Context, private val parent: ViewController<*>?, tdlib: Tdlib) : CustomTextView(context, tdlib) {
  private var lastInfo: TdApi.StickerSetInfo? = null
  private var key = 0
  private lateinit var displayName: String

  init {
    setTextColorId(ColorId.textLight)
    setLayoutParams(LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Screen.dp(40f)))
    setPadding(Screen.dp(16f), Screen.dp(14f), Screen.dp(16f), Screen.dp(6f))
  }

  fun update (firstEmojiId: Long, emojiPacksIds: Long, displayName: String, onClickListener: ClickableSpan, animated: Boolean) {
    this.displayName = displayName

    if (lastInfo == null || lastInfo!!.id != emojiPacksIds) {
      lastInfo = null
      key += 1

      val key = this.key
      parent!!.tdlib()!!.send(TdApi.GetStickerSet(emojiPacksIds)) { stickerSet, _ ->
        if (stickerSet == null) return@send
        UI.post {
          if (this.key != key) return@post
          this.lastInfo = stickerSet.toStickerSetInfo()
          updateImpl(firstEmojiId, onClickListener, lastInfo, false)
        }
      }
    }

    updateImpl(firstEmojiId, onClickListener, lastInfo, animated)
  }

  private fun updateImpl (firstEmojiId: Long, onClickListener: ClickableSpan, info: TdApi.StickerSetInfo?, animated: Boolean) {
    val tdlib = parent!!.tdlib()

    val formattedName: FormattedText
    run {
      val text = displayName
      val entities = TextEntity.valueOf(tdlib, text, arrayOf(
        TdApi.TextEntity(0, text.length, TdApi.TextEntityTypeBold())
      ), null)
      formattedName = FormattedText(text, entities)
    }

    val formattedPackName: FormattedText
    run {
      val text = if (info == null) Lang.getString(R.string.LoadingMessageEmojiPack) else info.title
      val entities = TextEntity.valueOf(tdlib, text, arrayOf(
        TdApi.TextEntity(0, text.length, TdApi.TextEntityTypeUrl()),
        TdApi.TextEntity(0, text.length, TdApi.TextEntityTypeBold())
      ), null)

      if (entities != null && entities.isNotEmpty()) {
        entities[0]?.setOnClickListener(onClickListener)
      }

      formattedPackName = FormattedText(text, entities)
    }

    val covers = info?.covers
    val cover: TdApi.Sticker? = if (covers != null && covers.isNotEmpty() &&
      covers[0].fullType.constructor == TdApi.StickerFullTypeCustomEmoji.CONSTRUCTOR)
      covers[0] else null

    val formattedEmoji: FormattedText? = if (info != null) FormattedText.customEmoji(tdlib,
      cover?.emoji ?: EmojiCodes.THUMBS_UP,
      cover?.id ?: firstEmojiId
    ) else null

    val formattedPackText = if (formattedEmoji != null)
      FormattedText.concat(" ", formattedEmoji, formattedPackName)
    else
      formattedPackName

    val formattedText = FormattedText.valueOf(tdlib, null, R.string.EmojiStatusUsed,
      formattedName,
      formattedPackText
    )

    setText(formattedText.text, formattedText.entities, animated)
  }
}
