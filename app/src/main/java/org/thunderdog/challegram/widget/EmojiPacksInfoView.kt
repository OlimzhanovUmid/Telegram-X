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
import android.text.style.ClickableSpan
import android.view.ViewGroup
import android.widget.LinearLayout

import org.drinkless.tdlib.TdApi
import org.thunderdog.challegram.Log
import org.thunderdog.challegram.R
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.navigation.ViewController
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.tool.UI
import org.thunderdog.challegram.util.text.FormattedText

import tgx.td.toStickerSetInfo

@Suppress("ViewConstructor")
class EmojiPacksInfoView (context: Context, private val parent: ViewController<*>, tdlib: Tdlib?) : CustomTextView(context, tdlib) {
  private var lastInfo: TdApi.StickerSetInfo? = null
  private var key = 0
  private lateinit var emojiPacksIds: LongArray

  init {
    setTextColorId(ColorId.textLight)
    setLayoutParams(LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Screen.dp(40f)))
    setPadding(Screen.dp(16f), Screen.dp(14f), Screen.dp(16f), Screen.dp(6f))
  }

  fun update (firstEmojiId: Long, emojiPacksIds: LongArray, onClickListener: ClickableSpan, animated: Boolean) {
    this.emojiPacksIds = emojiPacksIds
    val isSingle = emojiPacksIds.size == 1

    if (isSingle && (lastInfo == null || lastInfo!!.id != emojiPacksIds[0])) {
      lastInfo = null
      key += 1

      val key = this.key
      parent.tdlib()!!.send(TdApi.GetStickerSet(emojiPacksIds[0])) { stickerSet, _ ->
        if (stickerSet == null) return@send
        UI.post {
          if (this.key != key) return@post
          this.lastInfo = stickerSet.toStickerSetInfo()
          updateImpl(firstEmojiId, emojiPacksIds.size, onClickListener, lastInfo, false)
        }
      }
    }

    updateImpl(firstEmojiId, emojiPacksIds.size, onClickListener, lastInfo, animated)
  }

  fun getEmojiPacksIds (): LongArray {
    return emojiPacksIds
  }

  private fun updateImpl (firstEmojiId: Long, emojiPacksCount: Int, onClickListener: ClickableSpan, info: TdApi.StickerSetInfo?, animated: Boolean) {
    val isSingle = emojiPacksCount == 1

    val link: String = if (isSingle) {
      Lang.getString(R.string.xEmojiPacksEmojiSingle, if (info != null) info.title else Lang.getString(R.string.LoadingMessageEmojiPack))
    } else {
      Lang.plural(R.string.xEmojiPacks, emojiPacksCount.toLong())
    }
    val text = Lang.getString(if (isSingle) R.string.EmojiUsedFromSingle else R.string.EmojiUsedFromX, link)

    // FIXME: do not rely on cloud strings here
    val linkStart = text.indexOf(link)
    val emojiStart = text.indexOf("*")

    try {
      val formattedTextRaw: TdApi.FormattedText = if (!isSingle || emojiStart == -1) {
        TdApi.FormattedText(text, arrayOf(
          TdApi.TextEntity(linkStart, link.length, TdApi.TextEntityTypeUrl())
        ))
      } else if (emojiStart >= linkStart) {
        TdApi.FormattedText(text, arrayOf(
          TdApi.TextEntity(linkStart, link.length, TdApi.TextEntityTypeUrl()),
          TdApi.TextEntity(emojiStart, 1, TdApi.TextEntityTypeCustomEmoji(firstEmojiId))
        ))
      } else {
        TdApi.FormattedText(text, arrayOf(
          TdApi.TextEntity(emojiStart, 1, TdApi.TextEntityTypeCustomEmoji(firstEmojiId)),
          TdApi.TextEntity(linkStart, link.length, TdApi.TextEntityTypeUrl())
        ))
      }

      val formattedText = FormattedText.valueOf(parent, formattedTextRaw, null)
      val entities = formattedText.entities
      if (entities != null) {
        for (entity in entities) {
          entity.setOnClickListener(onClickListener)
          if (!entity.isCustomEmoji) {
            entity.makeBold(true)
          }
        }
      }

      setText(text, entities, animated)
    } catch (t: Throwable) {
      Log.e("Cannot get string", t)
    }
  }
}
