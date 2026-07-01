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
 * File created on 23/02/2017
 */
package org.thunderdog.challegram.util.text

import android.text.Spanned
import android.text.TextPaint
import android.text.style.CharacterStyle
import android.text.style.ClickableSpan
import android.view.View
import me.vkryl.core.isEmpty
import org.drinkless.tdlib.TdApi
import org.drinkless.tdlib.TdApi.RichTextIcon
import org.thunderdog.challegram.BuildConfig
import org.thunderdog.challegram.Log
import org.thunderdog.challegram.data.TD
import org.thunderdog.challegram.navigation.TooltipOverlayView.TooltipBuilder
import org.thunderdog.challegram.navigation.ViewController
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.telegram.TdlibUi.UrlOpenParameters
import org.thunderdog.challegram.tool.UI
import org.thunderdog.challegram.util.text.Text.ClickCallback

abstract class TextEntity(
  @JvmField val tdlib: Tdlib?,
  @JvmField var start: Int,
  @JvmField var end: Int,
  @JvmField protected var needFakeBold: Boolean,
  @JvmField protected val openParameters: UrlOpenParameters?
) {
    var tag: Any? = null
        private set

    @JvmField
    protected var customColorSet: TextColorSet? = null

    fun setStartEnd(start: Int, end: Int) {
        this.start = start
        this.end = end
    }

    fun offset(by: Int) {
        this.start += by
        this.end += by
    }

    fun getStart(): Int = start

    fun getEnd(): Int = end

    fun setTag(tag: Any?): TextEntity {
        this.tag = tag
        return this
    }

    fun setCustomColorSet(customColorSet: TextColorSet?): TextEntity {
        this.customColorSet = customColorSet
        return this
    }

    fun openParameters(view: View, text: Text?, part: TextPart, isFromLongPressMenu: Boolean): UrlOpenParameters {
        if (!isFromLongPressMenu && this.openParameters != null && this.openParameters.tooltip != null) return this.openParameters
        val b: TooltipBuilder?
        if (isFromLongPressMenu) {
            b = UI.getContext(view.getContext()).tooltipManager().builder(view)
        } else {
            b = part.newTooltipBuilder(view)
        }
        // TODO highlight the text part & modify color, if needed
        return UrlOpenParameters(this.openParameters).tooltip(b)
    }

    protected fun modifyUrlOpenParameters(parameters: UrlOpenParameters?, callback: ClickCallback?, url: String?): UrlOpenParameters? {
        var parameters = parameters
        if (callback == null) return parameters
        parameters = UrlOpenParameters(parameters)
        if (callback.forceInstantView(url)) {
            parameters.forceInstantView()
        }
        val webPage = callback.findLinkPreview(url)
        if (webPage != null) {
            parameters.sourceLinkPreview(webPage)
        }
        return parameters
    }

    abstract val type: Int

    abstract fun hasMedia(): Boolean

    abstract val isClickable: Boolean
    abstract val spoiler: TdApi.TextEntity?
    abstract val isBold: Boolean
    abstract val isIcon: Boolean
    open fun getIcon(): RichTextIcon? {
        return null
    }

    abstract val isItalic: Boolean
    abstract val isUnderline: Boolean
    abstract val isStrikethrough: Boolean
    abstract fun hasAnchor(anchor: String?): Boolean
    abstract val isFullWidth: Boolean
    abstract val isCustomEmoji: Boolean
    abstract val customEmojiId: Long
    abstract fun forceDisableAnimations(): Boolean
    abstract fun createCopy(): TextEntity?
    abstract val isQuote: Boolean
    abstract fun getQuote(): TdApi.TextEntity?
    abstract val quoteId: Int

    // TODO: TextEntityCustom & TextEntityMessage to make things simpler
    abstract fun setOnClickListener(onClickListener: ClickableSpan?): TextEntity?
    abstract val onClickListener: ClickableSpan?
    abstract fun makeBold(needFakeBold: Boolean): TextEntity?

    fun getTextPaint(textStyleProvider: TextStyleProvider, forceBold: Boolean): TextPaint {
        // different typefaces
        val isBold = forceBold || this.isBold
        val isItalic = this.isItalic
        val isFixed = this.isMonospace
        val isExtraBold = isBold && forceBold

        // different storages
        val isUnderline = this.isUnderline
        val isStrikeThrough = this.isStrikethrough
        val isSmall = this.isSmall

        var storage = textStyleProvider.getTextPaintStorage()

        /*if (isExtraBold) {
      storage = storage.getExtraBoldStorage();
    }*/
        if (isFixed) {
            storage = storage.getMonospaceStorage()
        }
        if (isUnderline) {
            storage = storage.getUnderlineStorage()
        }
        if (isStrikeThrough) {
            storage = storage.getStrikeThroughStorage()
        }
        if (isSmall) {
            storage = storage.getAlternativeSizeStorage()
        }

        val textPaint: TextPaint
        if (isBold && isItalic) {
            textPaint = storage.getBoldItalicPaint() // todo fake bolding?
        } else if (isItalic) {
            textPaint = storage.getItalicPaint()
        } else if (isBold) {
            textPaint =  /*isExtraBold || */if (needFakeBold) storage.getFakeBoldPaint() else storage.getBoldPaint()
        } else {
            textPaint = storage.getRegularPaint()
        }

        textStyleProvider.preparePaint(textPaint)
        if (isSmall) {
            textPaint.setTextSize(textPaint.getTextSize() * .75f)
        }

        return textPaint
    }

    abstract val isMonospace: Boolean
    abstract val isSmall: Boolean
    abstract fun performClick(view: View?, text: Text?, textPart: TextPart?, callback: ClickCallback?, isFromLongPressMenu: Boolean)
    abstract fun performLongPress(view: View?, text: Text?, textPart: TextPart?, allowShare: Boolean, callback: ClickCallback?): Boolean
    protected abstract fun equals(b: TextEntity?, compareMode: Int, originalText: String?): Boolean
    abstract val isEssential: Boolean
    abstract fun getSpecialColorSet(defaultColorSet: TextColorSet?): TextColorSet?

    protected fun findRoot(view: View?): ViewController<*>? {
        if (view == null) return null
        val c = ViewController.findRoot(view)
        if (c != null) return c
        return UI.getContext(view.getContext()).navigation().getCurrentStackItem()
    }

    open val baselineShift: Float
        get() = 0f

    companion object {
        const val TYPE_MESSAGE_ENTITY: Int = 0
        const val TYPE_CUSTOM: Int = 1

        @JvmStatic
        fun valueOf(tdlib: Tdlib?, text: TdApi.FormattedText, openParameters: UrlOpenParameters?): Array<TextEntity?>? {
            return valueOf(tdlib, text.text, text.entities, openParameters)
        }

        @JvmStatic
        fun valueOf(text: String?, highlightPart: Highlight.Part, highlightedColorSet: TextColorSet?): TextEntity {
            return TextEntityCustom(
                null, null,
                text, highlightPart.start, highlightPart.end,
                0, null
            ).setCustomColorSet(highlightedColorSet)
        }

        @JvmStatic
        fun valueOf(context: ViewController<*>?, tdlib: Tdlib?, text: CharSequence?, openParameters: UrlOpenParameters?): Array<TextEntity?>? {
            if (isEmpty(text)) {
                return null
            }
            if (text !is Spanned) {
                return null
            }
            val spans = text.getSpans<CharacterStyle?>(0, text.length, CharacterStyle::class.java)
            if (spans == null || spans.size == 0) {
                return null
            }
            val entities: MutableList<TextEntity?> = ArrayList<TextEntity?>()
            val str = text.toString()
            for (span in spans) {
                val startIndex = text.getSpanStart(span)
                val endIndex = text.getSpanEnd(span)
                if (span is ClickableSpan) {
                    entities.add(
                        TextEntityCustom(context, tdlib, str, startIndex, endIndex, 0, openParameters)
                            .setOnClickListener(span)
                    )
                } else {
                    val type = TD.toEntityType(span)
                    if (type != null && type.size > 0) {
                        val parentEntities: MutableList<TdApi.TextEntity?>?
                        if (type.size > 1) {
                            parentEntities = ArrayList<TdApi.TextEntity?>()
                            for (i in 0..<type.size - 1) {
                                parentEntities.add(TdApi.TextEntity(startIndex, endIndex - startIndex, type[i]))
                            }
                        } else {
                            parentEntities = null
                        }
                        entities.add(
                            TextEntityMessage(
                                tdlib,
                                str,
                                startIndex, endIndex,
                                TdApi.TextEntity(startIndex, endIndex - startIndex, type[type.size - 1]),
                                parentEntities,
                                openParameters
                            )
                        )
                    }
                }
            }
            return if (entities.isEmpty()) null else entities.toTypedArray<TextEntity?>()
        }

        private fun valueOf(
            tdlib: Tdlib?,
            `in`: String?,
            entities: Array<out TdApi.TextEntity?>,
            openParameters: UrlOpenParameters?,
            parents: MutableList<TdApi.TextEntity?>?,
            rootIndex: Int,
            out: MutableList<TextEntity>
        ): Int {
            var parents = parents
            val rootEntity = entities[rootIndex]!!
            var offset = rootEntity.offset
            var index = rootIndex

            var nextEntity: TdApi.TextEntity? = null
            while (index + 1 < entities.size && (entities[index + 1]!!.also { nextEntity = it }).offset < rootEntity.offset + rootEntity.length) {
                if (offset < nextEntity!!.offset) {
                    out.add(TextEntityMessage(tdlib, `in`, offset, nextEntity.offset, rootEntity, parents, openParameters))
                }

                if (parents == null) parents = ArrayList<TdApi.TextEntity?>()
                parents.add(rootEntity)
                index += valueOf(tdlib, `in`, entities, openParameters, parents, index + 1, out)
                parents.removeAt(parents.size - 1)

                offset = out.get(out.size - 1).end
            }
            if (offset < rootEntity.offset + rootEntity.length) {
                out.add(TextEntityMessage(tdlib, `in`, offset, rootEntity.offset + rootEntity.length, rootEntity, parents, openParameters))
            }
            return (index - rootIndex) + 1
        }

        @JvmStatic
        fun valueOf(tdlib: Tdlib?, `in`: String?, entities: Array<out TdApi.TextEntity?>?, openParameters: UrlOpenParameters?): Array<TextEntity?>? {
            if (entities == null || entities.size == 0) return null
            val result: MutableList<TextEntity> = ArrayList<TextEntity>()
            run {
                var i = 0
                while (i < entities.size) {
                    i += Companion.valueOf(tdlib, `in`, entities, openParameters, null, i, result)
                }
            }
            for (i in 1..<result.size) {
                val entity = result.get(i)
                val prev = result.get(i - 1)
                if (prev.end > entity.start) {
                    Log.e("Error processing entities, textLength: %d, entities: %s", (`in` ?: "").length, entities)
                    if (BuildConfig.DEBUG) throw RuntimeException()
                    return null
                }
            }
            return result.toTypedArray<TextEntity?>()
        }

        @JvmStatic
        fun valueOf(tdlib: Tdlib?, `in`: String?, entity: TdApi.TextEntity?, openParameters: UrlOpenParameters?): TextEntity {
            return TextEntityMessage(tdlib, `in`, entity, openParameters)
        }

        @JvmStatic
        fun toEntities(text: CharSequence?): Array<TextEntity?>? {
            if (text is Spanned) {
                val entities = text.getSpans<TextEntity?>(0, text.length, TextEntity::class.java)
                return if (entities != null && entities.size > 0) entities else null
            }
            return null
        }

        const val COMPARE_MODE_NORMAL: Int = 0
        const val COMPARE_MODE_CLICK_HIGHLIGHT: Int = 1
        const val COMPARE_MODE_SPOILER: Int = 2

        @JvmStatic
        fun equals(a: TextEntity?, b: TextEntity?, compareMode: Int, originalText: String?): Boolean {
            if (a == null && b == null) return true
            if (a == null || b == null) return false
            if (compareMode == COMPARE_MODE_CLICK_HIGHLIGHT) {
                // FIXME: merge TextEntityCustom & TextEntityMessage into one class instead for clarity purposes.
                val onClickListener = a.onClickListener
                if (onClickListener != null && onClickListener === b.onClickListener) {
                    return true
                }
            }
            if (a.type == b.type) {
                return a.equals(b, compareMode, originalText)
            }
            return false
        }
    }
}
