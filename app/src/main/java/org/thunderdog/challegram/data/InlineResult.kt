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
 * File created on 30/11/2016
 */
package org.thunderdog.challegram.data

import android.graphics.Canvas
import android.text.SpannableStringBuilder
import android.view.MotionEvent
import android.view.View
import me.vkryl.android.DECELERATE_INTERPOLATOR
import me.vkryl.android.animator.FactorAnimator
import me.vkryl.android.util.MultipleViewProvider
import me.vkryl.core.alphaColor
import me.vkryl.core.compositeColor
import me.vkryl.core.fromToArgb
import org.drinkless.tdlib.TdApi
import org.drinkless.tdlib.TdApi.*
import org.thunderdog.challegram.BaseActivity
import org.thunderdog.challegram.R
import org.thunderdog.challegram.component.chat.MediaPreview
import org.thunderdog.challegram.component.inline.CustomResultView
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.emoji.Emoji
import org.thunderdog.challegram.loader.ComplexReceiver
import org.thunderdog.challegram.mediaview.MediaViewThumbLocation
import org.thunderdog.challegram.mediaview.data.MediaItem
import org.thunderdog.challegram.player.TGPlayerController.PlayListBuilder
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Paints
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.tool.TGMimeType
import org.thunderdog.challegram.util.MessageSourceProvider
import org.thunderdog.challegram.widget.SimplestCheckBox
import kotlin.math.max

abstract class InlineResult<T> protected constructor(
  @JvmField protected val context: BaseActivity?,
  @JvmField protected val tdlib: Tdlib?,
  @JvmField val type: Int,
  @JvmField val id: String?,
  @JvmField protected val data: T?
) : MessageSourceProvider {
  @JvmField
  var queryId: Long = 0
  private var date = 0

  @JvmField
  protected var forceDarkMode: Boolean = false

  var targetStart: Int = -1
    protected set
  var targetEnd: Int = -1
    protected set

  @JvmField
  protected val currentViews: MultipleViewProvider = MultipleViewProvider()

  fun data(): T? {
    return data
  }

  fun tdlib(): Tdlib? {
    return tdlib
  }

  open fun setForceDarkMode(forceDarkMode: Boolean) {
    this.forceDarkMode = forceDarkMode
  }

  fun hasTarget(): Boolean {
    return targetStart != -1 && targetEnd != -1
  }

  fun replaceInTarget(targetText: CharSequence, within: CharSequence?): CharSequence {
    if (targetText is String && within is String) {
      val b = StringBuilder(within.length + targetText.length)
      if (targetStart > 0) {
        b.append(targetText, 0, targetStart)
      }
      b.append(within)
      if (targetEnd < targetText.length) {
        b.append(targetText, targetEnd, targetText.length)
      }
      return b.toString()
    } else {
      val b = SpannableStringBuilder()
      if (targetStart > 0) {
        b.append(targetText, 0, targetStart)
      }
      b.append(within)
      if (targetEnd < targetText.length) {
        b.append(targetText, targetEnd, targetText.length)
      }
      return b
    }
  }

  fun setTarget(start: Int, end: Int): InlineResult<T> {
    this.targetStart = start
    this.targetEnd = end
    return this
  }

  private var message: Message? = null

  open fun setMessage(message: Message?): InlineResult<T>? {
    this.message = message
    return this
  }

  override fun getMessage(): Message? {
    return message
  }

  fun setDate(date: Int) {
    this.date = date
  }

  // Message shit
  override fun getSourceDate(): Int {
    return date
  }

  override fun getSourceMessageId(): Long {
    return queryId
  }


  // List stuff
  private var boundList: ArrayList<InlineResult<*>?>? = null

  fun setBoundList(list: ArrayList<InlineResult<*>?>?) {
    this.boundList = list
  }

  private var forceSeparator = false

  fun setForceSeparator(forceSeparator: Boolean) {
    this.forceSeparator = forceSeparator
  }

  private fun needSeparator(): Boolean {
    if (isSeparatedType(type)) {
      if (boundList != null && !boundList!!.isEmpty()) {
        val result = boundList!![0]!!
        if (result == this) {
          return false
        }
        if (result.type == TYPE_BUTTON) {
          return boundList!!.size > 1 && boundList!![1] != this
        }
        return true
      }
      return forceSeparator
    }
    return false
  }

  open val cellWidth: Int
    // View and drawing stuff
    get() = 100

  open val cellHeight: Int
    get() = 100

  fun attachToView(view: View?) {
    if (currentViews.attachToView(view) && view != null) {
      if (view.measuredHeight != this.height) {
        view.requestLayout()
      }
      onResultAttachedToView(view, true)
    }
  }

  fun detachFromView(view: View?) {
    if (currentViews.detachFromView(view) && view != null) {
      onResultAttachedToView(view, false)
    }
  }

  protected open fun onResultAttachedToView(view: View, isAttached: Boolean) {}

  val height: Int
    get() = this.startY + this.contentHeight

  protected val startY: Int
    get() = if (needSeparator()) max(1, Screen.dp(.5f)) else 0

  protected open val contentHeight: Int
    get() = 0

  fun invalidate() {
    currentViews.invalidate()
  }

  fun hasAnyTargetToInvalidate(): Boolean {
    return currentViews.hasAnyTargetToInvalidate()
  }

  fun prepare(width: Int) {
    if (lastLayoutWidth == 0) {
      lastLayoutWidth = width
      layoutInternal(width)
    }
  }

  fun rebuildLayout() {
    layoutInternal(lastLayoutWidth)
  }

  private var lastLayoutWidth = 0

  fun layout(width: Int, receiver: ComplexReceiver?) {
    if (width > 0) {
      if (width != lastLayoutWidth) {
        lastLayoutWidth = width
        layoutInternal(width)
      }
    }
  }

  protected open fun layoutInternal(contentWidth: Int) {}

  fun requestFiles(receiver: ComplexReceiver) {
    requestContent(receiver, false)
  }

  open fun requestTextMedia(textMediaReceiver: ComplexReceiver) {
    textMediaReceiver.clear()
  }

  var mediaPreview: MediaPreview? = null
    protected set

  open fun requestContent(receiver: ComplexReceiver, isInvalidate: Boolean) {
    receiver.clear()
  }

  private var highlightAnimator: FactorAnimator? = null

  fun highlight() {
    if (highlightAnimator == null) {
      highlightAnimator = FactorAnimator(0, object : FactorAnimator.Target {
        override fun onFactorChanged(id: Int, factor: Float, fraction: Float, callee: FactorAnimator?) {
          invalidate()
        }

        override fun onFactorChangeFinished(id: Int, finalFactor: Float, callee: FactorAnimator?) {}
      }, DECELERATE_INTERPOLATOR, 400, 1f)
      highlightAnimator!!.setStartDelay(2000L)
    } else {
      highlightAnimator!!.forceFactor(1f)
    }
    invalidate()
  }

  fun draw(
    view: CustomResultView?,
    c: Canvas,
    receiver: ComplexReceiver?,
    viewWidth: Int,
    viewHeight: Int,
    anchorTouchX: Float,
    anchorTouchY: Float,
    selectFactor: Float,
    selectionIndex: Int,
    checkBox: SimplestCheckBox?
  ) {
    var startY = 0
    if (needSeparator()) {
      val width = Screen.dp(72f)
      val height = max(Screen.dp(.5f), 1)
      c.drawRect(
        0f,
        0f,
        width.toFloat(),
        height.toFloat(),
        Paints.fillingPaint(fromToArgb(Theme.fillingColor(), compositeColor(Theme.fillingColor(), Theme.chatSelectionColor()), selectFactor))
      )
      c.drawRect(width.toFloat(), 0f, viewWidth.toFloat(), height.toFloat(), Paints.fillingPaint(Theme.separatorColor()))
      startY += height
    }
    if (highlightAnimator != null) {
      val highlightFactor = highlightAnimator!!.getFactor()
      if (highlightFactor > 0f) {
        if (highlightFactor == 1f && !highlightAnimator!!.isAnimating()) {
          highlightAnimator!!.animateTo(0f)
        }
        c.drawRect(0f, startY.toFloat(), viewWidth.toFloat(), viewHeight.toFloat(), Paints.fillingPaint(alphaColor(highlightFactor, 0x28a0a0a0)))
      }
    }
    if (selectFactor != 0f) {
      c.drawRect(
        0f,
        startY.toFloat(),
        viewWidth.toFloat(),
        viewHeight.toFloat(),
        Paints.fillingPaint(alphaColor(selectFactor, Theme.chatSelectionColor()))
      )
    }
    drawInternal(view, c, receiver, viewWidth, viewHeight, startY)
    if (selectFactor != 0f) {
      val counter = if (selectionIndex != -1) (selectionIndex + 1).toString() else null
      onDrawSelectionOver(c, receiver, viewWidth, viewHeight, anchorTouchX, anchorTouchY, selectFactor, counter, checkBox)
    }
  }

  open fun onDrawSelectionOver(
    c: Canvas?,
    receiver: ComplexReceiver?,
    viewWidth: Int,
    viewHeight: Int,
    anchorTouchX: Float,
    anchorTouchY: Float,
    selectFactor: Float,
    counter: String?,
    checkBox: SimplestCheckBox?
  ) {
    // override
  }

  protected open fun drawInternal(view: CustomResultView?, c: Canvas?, receiver: ComplexReceiver?, viewWidth: Int, viewHeight: Int, startY: Int) {}

  open fun onTouchEvent(view: View?, e: MotionEvent?): Boolean {
    return false
  }

  open fun setThumbLocation(location: MediaViewThumbLocation?, view: View?, index: Int, mediaItem: MediaItem?): Boolean {
    return false
  }

  companion object {
    const val TYPE_ARTICLE: Int = 0
    const val TYPE_VIDEO: Int = 1
    const val TYPE_CONTACT: Int = 2
    const val TYPE_LOCATION: Int = 3
    const val TYPE_VENUE: Int = 4
    const val TYPE_GAME: Int = 5
    const val TYPE_GIF: Int = 6
    const val TYPE_AUDIO: Int = 7
    const val TYPE_VOICE: Int = 8
    const val TYPE_DOCUMENT: Int = 9
    const val TYPE_PHOTO: Int = 10
    const val TYPE_STICKER: Int = 11
    const val TYPE_MENTION: Int = 12
    const val TYPE_HASHTAG: Int = 13
    const val TYPE_COMMAND: Int = 14
    const val TYPE_BUTTON: Int = 15
    const val TYPE_EMOJI_SUGGESTION: Int = 16

    private fun isSeparatedType(type: Int): Boolean {
      when (type) {
        TYPE_VIDEO, TYPE_VENUE, TYPE_LOCATION, TYPE_CONTACT, TYPE_AUDIO, TYPE_VOICE, TYPE_DOCUMENT, TYPE_ARTICLE, TYPE_GAME -> {
          return true
        }
      }
      return false
    }

    @JvmStatic
    fun isGridType(type: Int): Boolean {
      return type == TYPE_STICKER
    }

    @JvmStatic
    fun isFlowType(type: Int): Boolean {
      return type == TYPE_PHOTO || type == TYPE_GIF
    }

    // Static stuff
    @JvmStatic
    fun valueOf(context: BaseActivity?, tdlib: Tdlib?, message: Message): InlineResult<*>? {
      return when (message.content.getConstructor()) {
        MessageAudio.CONSTRUCTOR -> {
          InlineResultCommon(context, tdlib, message, message.content as MessageAudio?, null).setMessage(message)
        }

        MessageDocument.CONSTRUCTOR -> {
          InlineResultCommon(context, tdlib, message, (message.content as MessageDocument).document).setMessage(message)
        }

        MessageVoiceNote.CONSTRUCTOR -> {
          InlineResultCommon(context, tdlib, message, (message.content as MessageVoiceNote).voiceNote).setMessage(message)
        }

        else -> null
      }
    }

    @JvmStatic
    fun valueOf(context: BaseActivity?, tdlib: Tdlib?, pageBlock: TdApi.PageBlock, builder: PlayListBuilder?): InlineResult<*>? {
      return when (pageBlock.getConstructor()) {
        PageBlockAudio.CONSTRUCTOR -> {
          InlineResultCommon(context, tdlib, pageBlock as PageBlockAudio, builder)
        }

        PageBlockVoiceNote.CONSTRUCTOR -> {
          InlineResultCommon(context, tdlib, pageBlock as PageBlockVoiceNote, Lang.getString(R.string.Audio))
        }
        else -> null
      }
    }

    @JvmStatic
    fun valueOf(context: BaseActivity?, tdlib: Tdlib?, query: String?, result: InlineQueryResult, builder: PlayListBuilder?): InlineResult<*>? {
      return when (result.getConstructor()) {
        InlineQueryResultPhoto.CONSTRUCTOR -> {
          InlineResultPhoto(context, tdlib, result as InlineQueryResultPhoto)
        }

        InlineQueryResultSticker.CONSTRUCTOR -> {
          InlineResultSticker(context, tdlib, if (Emoji.instance().isSingleEmoji(query)) query else null, result as InlineQueryResultSticker)
        }

        InlineQueryResultAnimation.CONSTRUCTOR -> {
          InlineResultGif(context, tdlib, result as InlineQueryResultAnimation)
        }

        InlineQueryResultVideo.CONSTRUCTOR -> {
          InlineResultCommon(context, tdlib, result as InlineQueryResultVideo)
        }

        InlineQueryResultVenue.CONSTRUCTOR -> {
          InlineResultCommon(context, tdlib, result as InlineQueryResultVenue)
        }

        InlineQueryResultLocation.CONSTRUCTOR -> {
          InlineResultCommon(context, tdlib, result as InlineQueryResultLocation)
        }

        InlineQueryResultContact.CONSTRUCTOR -> {
          InlineResultCommon(context, tdlib, result as InlineQueryResultContact)
        }

        InlineQueryResultAudio.CONSTRUCTOR -> {
          InlineResultCommon(context, tdlib, result as InlineQueryResultAudio, builder)
        }

        InlineQueryResultVoiceNote.CONSTRUCTOR -> {
          InlineResultCommon(context, tdlib, result as InlineQueryResultVoiceNote)
        }

        InlineQueryResultDocument.CONSTRUCTOR -> {
          val doc = result as InlineQueryResultDocument
          if (TGMimeType.isAudioMimeType(doc.document.mimeType)) {
            val audio = InlineQueryResultAudio(
              doc.id,
              Audio(
                0,
                doc.title,
                doc.description,
                doc.document.fileName,
                doc.document.mimeType,
                doc.document.minithumbnail,
                doc.document.thumbnail,
                null,
                doc.document.document
              )
            )
            InlineResultCommon(context, tdlib, audio, builder)
          } else {
            InlineResultCommon(context, tdlib, doc)
          }
        }

        InlineQueryResultArticle.CONSTRUCTOR -> {
          InlineResultMultiline(context, tdlib, result as InlineQueryResultArticle)
        }

        InlineQueryResultGame.CONSTRUCTOR -> {
          InlineResultMultiline(context, tdlib, result as InlineQueryResultGame)
        }

        else -> null
      }
    }
  }
}
