package org.thunderdog.challegram.telegram

import androidx.annotation.IntDef
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import org.drinkless.tdlib.TdApi
import me.vkryl.core.hasFlag
import me.vkryl.core.optional
import me.vkryl.core.parseInt
import me.vkryl.core.parseLong
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.util.Collection

@Suppress("EXPOSED_SUPER_CLASS", "EXPOSED_SUPER_INTERFACE")
internal class TdlibOutlineManager(tdlib: Tdlib) : TdlibDataManager<String, TdApi.Outline, TdlibOutlineManager.Entry>(tdlib) {
  @Retention(RetentionPolicy.SOURCE)
  @IntDef(
    value = [
      StickerOutlineOption.FOR_ANIMATED_EMOJI,
      StickerOutlineOption.FOR_CLICKED_ANIMATED_EMOJI_MESSAGE
    ],
    flag = true
  )
  annotation class StickerOutlineOption {
    companion object {
      const val FOR_ANIMATED_EMOJI: Int = 1
      const val FOR_CLICKED_ANIMATED_EMOJI_MESSAGE: Int = 1 shl 1
    }
  }

  class Entry(@NonNull key: String, @Nullable value: TdApi.Outline?, @Nullable error: TdApi.Error?) : AbstractEntry<String, TdApi.Outline>(key, value, error) {
    private val parsedKey = ParsedKey.parse(key)

    fun isStickerOutline (): Boolean = parsedKey.type == ParsedKey.TYPE_STICKER_OUTLINE
    fun isWebAppPlaceholder (): Boolean = parsedKey.type == ParsedKey.TYPE_WEB_APP_PLACEHOLDER
  }

  @Suppress("EXPOSED_SUPER_INTERFACE")
  fun interface Watcher : TdlibDataManager.Watcher<String, TdApi.Outline, TdlibOutlineManager.Entry> {
    fun onStickerOutlineLoaded (context: TdlibOutlineManager, entry: TdlibOutlineManager.Entry)

    @SuppressWarnings("ClassEscapesDefinedScope")
    override fun onEntryLoaded (context: TdlibDataManager<String, TdApi.Outline, TdlibOutlineManager.Entry>, entry: TdlibOutlineManager.Entry) {
      onStickerOutlineLoaded(context as TdlibOutlineManager, entry)
    }
  }

  fun requestStickerOutline (stickerFileId: Int, forAnimatedEmoji: Boolean, forClickedAnimatedEmojiMessage: Boolean, watcher: Watcher) {
    requestStickerOutline(stickerFileId, forAnimatedEmoji, forClickedAnimatedEmojiMessage, watcher, false)
  }

  fun requestStickerOutline (stickerFileId: Int, forAnimatedEmoji: Boolean, forClickedAnimatedEmojiMessage: Boolean, watcher: Watcher, strongReference: Boolean) {
    val option =
      optional(StickerOutlineOption.FOR_ANIMATED_EMOJI, forAnimatedEmoji) or
        optional(StickerOutlineOption.FOR_CLICKED_ANIMATED_EMOJI_MESSAGE, forClickedAnimatedEmojiMessage)
    val key = ParsedKey(ParsedKey.TYPE_STICKER_OUTLINE, stickerFileId.toLong(), option).serializeToKey()
    findOrPostponeRequest(key, watcher, strongReference)
  }

  fun requestWebAppPlaceholder (botUserId: Long, watcher: Watcher) {
    requestWebAppPlaceholder(botUserId, watcher, false)
  }

  fun requestWebAppPlaceholder (botUserId: Long, watcher: Watcher, strongReference: Boolean) {
    val key = ParsedKey(ParsedKey.TYPE_WEB_APP_PLACEHOLDER, botUserId, 0).serializeToKey()
    findOrPostponeRequest(key, watcher, strongReference)
    performPostponedRequestsDelayed()
  }

  override fun newEntry(@NonNull key: String, @Nullable value: TdApi.Outline?, @Nullable error: TdApi.Error?): Entry {
    return Entry(key, value, error)
  }

  private class ParsedKey(val type: Int, val arg1: Long, val options: Int) {
    fun serializeToKey (): String {
      return "${type}_${arg1}_${options}"
    }

    fun toRequest (): TdApi.Function<TdApi.Outline> {
      return when (type) {
        TYPE_STICKER_OUTLINE -> TdApi.GetStickerOutline(
          arg1.toInt(),
          hasFlag(options, StickerOutlineOption.FOR_ANIMATED_EMOJI),
          hasFlag(options, StickerOutlineOption.FOR_CLICKED_ANIMATED_EMOJI_MESSAGE)
        )
        TYPE_WEB_APP_PLACEHOLDER -> TdApi.GetWebAppPlaceholder(arg1)
        else -> throw IllegalStateException()
      }
    }

    companion object {
      const val TYPE_STICKER_OUTLINE = 1
      const val TYPE_WEB_APP_PLACEHOLDER = 2

      fun parse (key: String): ParsedKey {
        val data = key.split("_")
        val type = parseInt(data[0])
        val arg1 = parseLong(data[1])
        val options = parseInt(data[2])
        return ParsedKey(type, arg1, options)
      }
    }
  }

  override fun requestData(contextId: Int, keysToRequest: MutableCollection<String>) {
    for (key in keysToRequest) {
      val parsedKey = ParsedKey.parse(key)
      tdlib.send(parsedKey.toRequest()) { outline, error ->
        if (isCancelled(contextId)) {
          return@send
        }
        if (error != null) {
          processError(contextId, key, error)
        } else {
          processData(contextId, key, outline)
        }
      }
    }
  }
}
