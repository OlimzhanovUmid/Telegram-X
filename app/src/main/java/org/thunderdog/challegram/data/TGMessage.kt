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
 * File created on 03/05/2015 at 10:39
 */
package org.thunderdog.challegram.data

import android.graphics.*
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.os.*
import android.os.Message
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.TextUtils
import android.util.Log
import android.util.SparseIntArray
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.*
import androidx.collection.LongSparseArray
import me.vkryl.android.DECELERATE_INTERPOLATOR
import me.vkryl.android.animator.BoolAnimator
import me.vkryl.android.animator.FactorAnimator
import me.vkryl.android.getPrettyName
import me.vkryl.android.util.ClickHelper
import me.vkryl.android.util.InvalidateContentProvider
import me.vkryl.android.util.MultipleViewProvider
import me.vkryl.core.*
import me.vkryl.core.collection.LongList
import me.vkryl.core.collection.LongSet
import me.vkryl.core.lambda.*
import me.vkryl.core.reference.ReferenceList
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import org.drinkless.tdlib.TdApi.*
import org.drinkmore.Tracer.onLaunchError
import org.drinkmore.Tracer.onTdlibHandlerError
import org.thunderdog.challegram.BaseActivity
import org.thunderdog.challegram.BuildConfig
import org.thunderdog.challegram.R
import org.thunderdog.challegram.U
import org.thunderdog.challegram.component.chat.*
import org.thunderdog.challegram.component.sticker.TGStickerObj
import org.thunderdog.challegram.config.Config
import org.thunderdog.challegram.config.Device
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.core.Lang.SpanCreator
import org.thunderdog.challegram.data.TGReaction.ReactionDrawable
import org.thunderdog.challegram.data.TGReactions.MessageReactionEntry
import org.thunderdog.challegram.data.TGReactions.MessageReactionsDelegate
import org.thunderdog.challegram.data.TdApiExt.MessageChatEvent
import org.thunderdog.challegram.data.TranslationsManager.*
import org.thunderdog.challegram.loader.*
import org.thunderdog.challegram.loader.gif.GifReceiver
import org.thunderdog.challegram.mediaview.MediaViewThumbLocation
import org.thunderdog.challegram.mediaview.data.MediaItem
import org.thunderdog.challegram.navigation.HeaderView
import org.thunderdog.challegram.navigation.ReactionsOverlayView.*
import org.thunderdog.challegram.navigation.TooltipOverlayView
import org.thunderdog.challegram.navigation.TooltipOverlayView.*
import org.thunderdog.challegram.navigation.ViewController
import org.thunderdog.challegram.telegram.*
import org.thunderdog.challegram.telegram.TdlibUi.ChatOpenParameters
import org.thunderdog.challegram.telegram.TdlibUi.UrlOpenParameters
import org.thunderdog.challegram.theme.*
import org.thunderdog.challegram.tool.*
import org.thunderdog.challegram.ui.FeatureToggles
import org.thunderdog.challegram.ui.MessagesController
import org.thunderdog.challegram.ui.TranslationControllerV2.LanguageSelectorPopup
import org.thunderdog.challegram.ui.TranslationControllerV2.LanguageSelectorPopup.OnLanguageSelectListener
import org.thunderdog.challegram.unsorted.Settings
import org.thunderdog.challegram.util.*
import org.thunderdog.challegram.util.EmojiStatusHelper.EmojiStatusDrawable
import org.thunderdog.challegram.util.text.*
import org.thunderdog.challegram.util.text.Text
import org.thunderdog.challegram.util.text.Text.ClickCallback
import org.thunderdog.challegram.util.text.TextColorSets.BubbleIn
import org.thunderdog.challegram.util.text.TextColorSets.BubbleOut
import org.thunderdog.challegram.util.text.TextEntity
import org.thunderdog.challegram.v.MessagesRecyclerView
import org.thunderdog.challegram.widget.ShadowView
import org.thunderdog.challegram.widget.SimplestCheckBox
import tgx.td.*
import tgx.td.data.MessageWithProperties
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.contracts.ExperimentalContracts
import kotlin.math.*

@OptIn(ExperimentalContracts::class)
abstract class TGMessage private constructor(manager: MessagesManager, msg: TdApi.Message, sponsoredMessage: SponsoredMessage?, isBelowAllMessages: Boolean) :
    InvalidateContentProvider, TdlibDelegate, FactorAnimator.Target, Comparable<TGMessage?>, Counter.Callback, TGAvatars.Callback, Translatable {
    @JvmField
    var msg: TdApi.Message?
    val sponsoredMessage: SponsoredMessage?
    private var flags: Int = 0

    @JvmField protected var mergeTime: Int = 0
    @JvmField protected var mergeIndex: Int = 0

    @JvmField protected var width: Int = 0
    @JvmField protected var height: Int = 0

    open fun getHeight(): Int = height

    protected var time: String?

    // Data getters
    @JvmField
    val sender: TdlibSender

    protected val viaBotUsername: String?
    var forwardInfo: TGSource? = null
        protected set
    @JvmField
    var replyData: ReplyComponent? = null
    @JvmField
    protected var inlineKeyboard: TGInlineKeyboard? = null
    @JvmField
    val messageReactions: TGReactions?
    @JvmField
    protected val commentButton: TGCommentButton?
    var swipeHelper: MessageQuickActionSwipeHelper
        protected set

    // header values
    var drawDateText: String? = null
        private set
    private var hAuthorAccentColor: TdlibAccentColor? = null
    private var hAuthorNameT: Text? = null
    private var hPsaTextT: Text? = null
    private var hAuthorChatMark: Text? = null
    private var hAdminNameT: Text? = null
    private var uBadge: Letters? = null
    private var hAuthorEmojiStatus: EmojiStatusDrawable? = null

    // counters
    private val viewCounter: Counter?
    private val replyCounter: Counter?
    private val shareCounter: Counter?
    private val isPinned: Counter?
    private val isEdited: Counter?
    private val isRestricted: Counter?
    private val isUnsupported: Counter?
    private var shrinkedReactionsCounter: Counter?
    private var reactionsCounter: Counter?
    private val reactionsCounterDrawable: ReactionsCounterDrawable
    private val isChannelHeaderCounter: Counter

    private var translatedCounterForceShow = false
    private val isTranslatedCounter: Counter
    private val isTranslatedCounterDrawable: TranslationCounterDrawable

    // counter last-draw positions
    private val isChannelHeaderCounterLastDrawRect = RectF()
    private val isTranslatedCounterLastDrawRect = RectF()
    private val isRestrictedCounterLastDrawRect = RectF()
    private val isEditedCounterLastDrawRect = RectF()


    // forward values
    private var fTime: String? = null
    private var fAuthorNameAccentColor: TdlibAccentColor? = null
    private var fAuthorNameT: Text? = null
    private var fPsaTextT: Text? = null
    private var fTimeWidth = 0f

    // positions and sizes
    private var pBadgeX = 0
    private var pBadgeIconX = 0

    var realContentX: Int = 0
        private set
    var realContentMaxWidth: Int = 0
        private set
    private var leftContentEdge = 0
    var topContentEdge: Int = 0
        private set
    var bottomContentEdge: Int = 0
        private set
    private var rightContentEdge = 0
    private var lastMergeRadius = 0f
    private var lastDefaultRadius = 0f
    open var contentX: Int = 0
        protected set
    var contentY: Int = 0
        private set
    protected var contentMaxWidth: Int = 0
        private set
    private var timeAddedHeight = 0
    private val timeExpandValue = FactorAnimator(0, { id, factor, fraction, callee ->
      if (hasFlag(flags, FLAG_LAYOUT_BUILT)) {
        if (useBubbles() && !useReactionBubbles()) {
          val height: Int = this@TGMessage.getHeight()
          buildBubble(false)
          if (this@TGMessage.getHeight() != height) {
            requestLayout()
          }
        }
      }
      invalidate()
    }, DECELERATE_INTERPOLATOR, 200L)

    private var pTimeLeft = 0
    private var pTimeWidth = 0
    private var pClockLeft = 0
    private var pClockTop = 0
    private var pTicksLeft = 0
    private var pTicksTop = 0
    var dateWidth: Int = 0
        private set
    private var lastDrawReactionsX = 0
    private var lastDrawReactionsY = 0

    private val bubblePath: Path
    @JvmField
    val bubbleClipPath: Path
    var bubbleTopRightRadius: Float = 0f
        private set
    var bubbleTopLeftRadius: Float = 0f
        private set
    var bubbleBottomLeftRadius: Float = 0f
        private set
    var bubbleBottomRightRadius: Float = 0f
        private set
    @JvmField
    protected val bubblePathRect: RectF
    protected val bubbleClipPathRect: RectF

    private var needSponsorSmallPadding = false

    @JvmField
    val manager: MessagesManager
    @JvmField
    val tdlib: Tdlib
    @JvmField
    val currentViews: MultipleViewProvider
    @JvmField
    protected val overlayViews: MultipleViewProvider?

    private var messageAvailableReactions: AvailableReactions? = null

    private val mTranslationsManager: TranslationsManager

    protected constructor(manager: MessagesManager, msg: TdApi.Message) : this(manager, msg, null, false)

    protected constructor(manager: MessagesManager, sponsoredMessage: SponsoredMessage, inChatId: Long, isBelowAllMessages: Boolean) : this(
        manager,
        toFakeMessage(manager, inChatId, sponsoredMessage),
        sponsoredMessage,
        isBelowAllMessages
    )

    override fun context(): BaseActivity {
        return manager.controller().context()
    }

    override fun tdlib(): Tdlib {
        return tdlib
    }

    fun manager(): MessagesManager {
        return manager
    }

    fun messagesController(): MessagesController {
        return manager.controller()
    }

    fun controller(): ViewController<*>? {
        return messagesController().getParentOrSelf()
    }

    fun navigateTo(c: ViewController<*>) {
        if (!controller()!!.navigateTo(c)) c.destroy()
    }

    // Value Generators
    private fun genTime(): String? {
        if (this.isEventLog) {
            return Lang.getRelativeTimestampShort(msg!!.date.toLong(), TimeUnit.SECONDS)
        } else if (isSponsoredMessage()) {
            return Lang.getString(if (sponsoredMessage!!.isRecommended) R.string.RecommendedSign else R.string.SponsoredSign)
        }
        val b = StringBuilder()
        val signature: String?
        if (this.isChannel && !isEmpty(msg!!.authorSignature)) {
            signature = msg!!.authorSignature
        } else if (forceForwardOrImportInfo() && msg!!.forwardInfo != null) {
            when (msg!!.forwardInfo!!.origin.getConstructor()) {
                MessageOriginChannel.CONSTRUCTOR -> signature = (msg!!.forwardInfo!!.origin as MessageOriginChannel).authorSignature
                MessageOriginChat.CONSTRUCTOR -> signature = (msg!!.forwardInfo!!.origin as MessageOriginChat).authorSignature
                else -> signature = null
            }
        } else {
            signature = null
        }
        if (!isEmpty(signature)) {
            if (Fonts.isLtrCharSupported()) {
                b.append(Strings.LTR_CHAR)
            }
            b.append(Strings.limit(signature, 18))
            if (Fonts.isLtrCharSupported()) {
                b.append(Strings.LTR_CHAR)
            }
            b.append(", ")
        }
        if (!useBubbles() && needAdminSign()) {
            b.append(this.administratorSign).append(" ")
        }
        if (this.isImported) {
            b.append(Lang.getString(R.string.ImportedSign)).append(" ")
        }
        if (TD.isFailed(msg)) {
            b.append(Lang.getString(R.string.failed))
        } else if (this.isScheduled) {
            val date =
                if (msg!!.schedulingState!!.getConstructor() == MessageSchedulingStateSendAtDate.CONSTRUCTOR) (msg!!.schedulingState as MessageSchedulingStateSendAtDate).sendDate else 0
            if (date != 0) {
                b.append(Lang.time(date.toLong(), TimeUnit.SECONDS))
            }
        } else if ((flags and FLAG_SELF_CHAT) != 0 && !this.isOutgoing && msg!!.forwardInfo != null) {
            var date = if (replaceTimeWithEditTime()) msg!!.editDate else msg!!.date
            val forwardOrImportDate = this.forwardOrImportDate
            if (forwardOrImportDate != 0) date = forwardOrImportDate
            if (date != 0) {
                b.append(Lang.getRelativeTimestampShort(date.toLong(), TimeUnit.SECONDS))
            }
        } else if (forceForwardOrImportInfo() && this.forwardOrImportDate != 0) {
            val date = this.forwardOrImportDate
            b.append(
                if (isSameDay(date, msg!!.date)) Lang.time(date.toLong(), TimeUnit.SECONDS) else Lang.getRelativeTimestampShort(
                    date.toLong(),
                    TimeUnit.SECONDS
                )
            )
        } else {
            val date = if (replaceTimeWithEditTime()) msg!!.editDate else msg!!.date
            if (date != 0) {
                b.append(Lang.time(date.toLong(), TimeUnit.SECONDS))
            }
        }

        return b.toString()
    }

    private fun genForwardTime(): String? {
        if (!useForward()) {
            return null
        }
        if (msg!!.forwardInfo == null) {
            return null
        }
        return Lang.getRelativeTimestampShort(msg!!.forwardInfo!!.date.toLong(), TimeUnit.SECONDS)
    }

    val forwardTimeStamp: Int
        get() {
            if (msg!!.forwardInfo == null) {
                return -1
            }

            return msg!!.forwardInfo!!.date
        }

    // Other
    private fun genDate(): String? {
        if (this.isDemoChat && (flags and MESSAGE_FLAG_HAS_OLDER_MESSAGE) == 0) {
            return Lang.getString(R.string.ChatPreview)
        }
        if (this.isScheduled) {
            when (msg!!.schedulingState!!.getConstructor()) {
                TdApi.MessageSchedulingStateSendWhenOnline.CONSTRUCTOR -> return Lang.getString(R.string.ScheduledUntilOnline)
                MessageSchedulingStateSendAtDate.CONSTRUCTOR -> {
                    val date = (msg!!.schedulingState as MessageSchedulingStateSendAtDate).sendDate
                    if (isToday(date.toLong(), TimeUnit.SECONDS)) {
                        return Lang.getString(R.string.ScheduledToday)
                    } else if (isTomorrow(date.toLong(), TimeUnit.SECONDS)) {
                        return Lang.getString(R.string.ScheduledTomorrow)
                    } else {
                        return Lang.getString(R.string.ScheduledDate, Lang.getDate(date.toLong(), TimeUnit.SECONDS))
                    }
                }
            }
        }
        if (this.isEventLog) {
            return Lang.getRelativeTimestamp(this.comparingDate.toLong(), TimeUnit.SECONDS)
        }
        return Lang.getDate(this.comparingDate.toLong(), TimeUnit.SECONDS)
    }

    fun forceAvatarWhenMerging(value: Boolean) {
        flags = setFlag(flags, MESSAGE_FLAG_FORCE_AVATAR, value)
    }

    fun mergeWith(top: TGMessage?, isBottom: Boolean): Boolean {
        if (top != null) {
            top.setNeedExtraPadding(false)
            top.setNeedExtraPresponsoredPadding(isSponsoredMessage() && hasFlag(flags, FLAG_BELOW_ALL_MESSAGES))
            flags = flags or MESSAGE_FLAG_HAS_OLDER_MESSAGE
        } else {
            flags = flags and MESSAGE_FLAG_HAS_OLDER_MESSAGE.inv()
        }
        if (isBottom) {
            setNeedExtraPadding(true)
        }

        val isBelowHeader = top != null && top.isThreadHeader
        flags = setFlag(flags, MESSAGE_FLAG_BELOW_HEADER, isBelowHeader)
        updateShowBadge()
        updateBadgeText()

        setIsBottom(true)

        if (top == null || top.isThreadHeader != this.isThreadHeader || !(if (this.isEventLog) needHideEventDate() || (isSameHour(
                top.comparingDate,
                this.comparingDate
            ) /*|| !(msg.content instanceof TdApiExt.MessageChatEvent)*/) else isSameDay(top.comparingDate, this.comparingDate))
        ) {
            if (top != null) {
                top.setIsBottom(true)
            }
            setHeaderEnabled(!headerDisabled())
            if ((top != null || getDate() != 0 || this.isScheduled) && !isSponsoredMessage() && (!isBelowHeader || messagesController().areScheduledOnly())) {
                flags = flags or FLAG_SHOW_DATE
                setDate(genDate())
            } else {
                flags = flags and FLAG_SHOW_DATE.inv()
            }
            return false
        }

        flags = flags and FLAG_SHOW_DATE.inv()

        val useBubbles = useBubbles()
        val isChannel = this.isChannel

        val topMessage = top.message
        if (top.headerDisabled() || top.isSponsoredMessage() != isSponsoredMessage() || (flags and FLAG_SHOW_BADGE) != 0 || !tdlib.isSameSender(
                topMessage,
                msg
            ) || !TD.isSameSource(
                topMessage,
                msg,
                forceForwardOrImportInfo()
            ) || topMessage.viaBotUserId != msg!!.viaBotUserId || !topMessage.authorSignature.equalsOrBothEmpty(msg!!.authorSignature) || mergeDisabled() || (if (useBubbles) top.isOutgoingBubble != this.isOutgoingBubble else top.message.mediaAlbumId != msg!!.mediaAlbumId || msg!!.mediaAlbumId != 0L)
        ) {
            setHeaderEnabled(!headerDisabled())
            top.setIsBottom(true)
            return false
        }

        val maxTimeDiff: Int
        val maxIndex: Int

        if (this.isEventLog) {
            maxTimeDiff = MAXIMUM_COMMON_MERGE_TIME_DIFF
            maxIndex = Int.MAX_VALUE
        } else if (useBubbles()) {
            maxTimeDiff =  /*isChannel() ? MAXIMUM_CHANNEL_MERGE_TIME_DIFF :*/MAXIMUM_COMMON_MERGE_TIME_DIFF
            maxIndex = Int.MAX_VALUE
        } else if (isChannel) {
            maxTimeDiff = MAXIMUM_CHANNEL_MERGE_TIME_DIFF
            maxIndex = MAXIMUM_CHANNEL_MERGE_COUNT
        } else {
            maxTimeDiff = MAXIMUM_COMMON_MERGE_TIME_DIFF
            maxIndex = MAXIMUM_COMMON_MERGE_COUNT
        }

        if (!(useBubbles && isChannel && msg!!.forwardInfo != null && msg!!.forwardInfo!!.origin.getConstructor() == MessageOriginUser.CONSTRUCTOR) && msg!!.date - top.getMergeTime() < maxTimeDiff && top.getMergeIndex() < maxIndex) {
            flags = flags and FLAG_HEADER_ENABLED.inv()
            mergeTime = top.getMergeTime()
            mergeIndex = top.getMergeIndex() + 1
            if (top.isForward && this.isForward) {
                flags = flags or FLAG_MERGE_FORWARD
                top.setMergeBottom(true)
            } else {
                flags = flags and FLAG_MERGE_FORWARD.inv()
                top.setMergeBottom(false)
            }
            if (this.isOutgoing) {
                if (!headerDisabled() && !isChannel && ((this.isSending || this.isUnread) != (top.isUnread || top.isSending))) {
                    flags = flags or FLAG_SHOW_TICKS
                } else {
                    flags = flags and FLAG_SHOW_TICKS.inv()
                }
            }

            top.setIsBottom(false)
            return true
        }

        setHeaderEnabled(!headerDisabled())
        top.setIsBottom(true)

        return false
    }

    protected open fun headerDisabled(): Boolean {
        return false
    }

    protected open fun disableBubble(): Boolean {
        return false
    }

    fun useBubble(): Boolean {
        return useBubbles() && (!disableBubble() || useForward())
    }

    protected open fun separateReplyFromBubble(): Boolean {
        return false
    }

    protected fun mergeDisabled(): Boolean {
        return false
    }

    fun hasHeader(): Boolean {
        return !headerDisabled() && (flags and FLAG_HEADER_ENABLED) != 0
    }

    protected val smallestMaxContentWidth: Int
        get() = min(
            this.realContentMaxWidth,
            Screen.smallestSide() - xPaddingRight - this.realContentX
        )

    val smallestMaxContentHeight: Int
        get() = (this.smallestMaxContentWidth.toFloat() * 1.24f).toInt()

    // Layout
    private fun computeBubbleLeft(): Int {
        val x: Int
        if (needAvatar() && !this.isOutgoing) {
            x = xBubbleLeft1 + Screen.dp(40f)
        } else {
            x = if (Device.NEED_BIGGER_BUBBLE_OFFSETS) xBubbleLeft2 else xBubbleLeft1
        }
        return x
    }

    private fun computeBubbleTop(): Int {
        return this.headerPadding + this.bubbleViewPaddingTop
    }

    private val authorWidth: Int
        get() = if (hAuthorNameT != null) hAuthorNameT!!.getWidth() + (if (hAuthorEmojiStatus != null) hAuthorEmojiStatus!!.getWidth(
            Screen.dp(
                3f
            )
        ) else 0) + (if (hAuthorChatMark != null) hAuthorChatMark!!.getWidth() + Screen.dp(16f) else 0) else if (needName(true)) -Screen.dp(
            3f
        ) else 0

    private fun computeBubbleWidth(): Int {
        val contentWidth = getContentWidth()
        var width = contentWidth
        if (allowMessageHorizontalExtend()) {
            if (replyData != null) {
                width = max(width, replyData!!.width(!useBubble()))
            }
            if (needName(true) && (flags and FLAG_HEADER_ENABLED) != 0) {
                var nameWidth = this.authorWidth
                if (needAdminSign() && hAdminNameT != null) {
                    nameWidth += hAdminNameT!!.getWidth()
                }
                if (needDrawChannelIconInHeader() && hAuthorNameT != null) {
                    nameWidth = (nameWidth + isChannelHeaderCounter.getScaledWidth(Screen.dp(5f))).toInt()
                }
                width = max(width, nameWidth)
            }
            if (hasFooter()) {
                width = max(width, this.footerWidth)
            }
        }

        if (useForward()) {
            if (allowMessageHorizontalExtend()) {
                val isPsa = this.isPsa && !forceForwardOrImportInfo()
                val forwardWidth = max(
                    (if (isPsa && fPsaTextT != null) fAuthorNameT!!.getWidth() else if (fAuthorNameT != null) fAuthorNameT!!.getWidth() else 0)
                            + fTimeWidth + Screen.dp(6f)
                            + (if (this.viewCountMode == VIEW_COUNT_FORWARD) viewCounter!!.getScaledWidth(Screen.dp(COUNTER_ICON_MARGIN + COUNTER_ADD_MARGIN)) + shareCounter!!.getScaledWidth(
                        Screen.dp(COUNTER_ICON_MARGIN + COUNTER_ADD_MARGIN)
                    ) else 0f),
                    (if (isPsa && fPsaTextT != null && fAuthorNameT != null) fAuthorNameT!!.getWidth() else 0).toFloat()
                ) + (if (replyData != null) xTextPadding else 0)
                width = max(width, max(contentWidth + Screen.dp(11f), (forwardWidth + Screen.dp(11f)).toInt()))
            } else {
                width = max(width, contentWidth + Screen.dp(11f))
            }
        }

        if (commentButton!!.isVisible() && commentButton.isInline()) {
            if (allowMessageHorizontalExtend()) {
                val commentButtonWidth = commentButton.getAnimatedWidth(0, 1f).toFloat()
                if (commentButtonWidth > width) {
                    width = Math.round(fromTo(width.toFloat(), commentButtonWidth, commentButton.getVisibility()))
                }
            }
        }

        return width //  + getBubblePaddingLeft() + getBubblePaddingRight();
    }

    protected fun useForward(): Boolean {
        // && !((flags & FLAG_SELF_CHAT) == 0 && msg.forwardInfo.origin.getConstructor() != TdApi.MessageOriginChannel.CONSTRUCTOR && msg.content != null && msg.content.getConstructor() == TdApi.MessageAudio.CONSTRUCTOR)
        return msg!!.forwardInfo != null && (!useBubbles() || !separateReplyFromBubble()) && !forceForwardOrImportInfo()
    }


    private val isHiddenByFilter: BoolAnimator

    fun setIsHiddenByMessagesFilter(hidden: Boolean, animated: Boolean) {
        isHiddenByFilter.setValue(
            hidden && !isSponsoredMessage(),
            hasFlag(
                flags,
                FLAG_LAYOUT_BUILT
            ) && currentViews.hasAnyTargetToInvalidate() && UI.inUiThread() && controller() != null && controller()!!.isFocused() && animated
        )
    }

    val isHiddenByMessagesFilter: Boolean
        get() = isHiddenByFilter.getValue()


    private val viewCountMode: Int
        get() {
            if (viewCounter != null) {
                if (useForward() && !msg!!.isChannelPost && msg!!.forwardInfo != null && msg!!.forwardInfo!!.origin.getConstructor() == MessageOriginChannel.CONSTRUCTOR) {
                    return VIEW_COUNT_FORWARD
                }
                if (useBubbles() || hasFlag(flags, FLAG_HEADER_ENABLED)) {
                    return VIEW_COUNT_MAIN
                }
            }
            return VIEW_COUNT_HIDDEN
        }

    private val openingComments = BoolAnimator(0, object : FactorAnimator.Target {
        override fun onFactorChanged(id: Int, factor: Float, fraction: Float, callee: FactorAnimator?) {
            invalidate()
        }
    }, DECELERATE_INTERPOLATOR, 200L)

    protected val commentButtonViewMode: Int
        get() {
            if (!needCommentButton()) {
                return TGCommentButton.VIEW_MODE_HIDDEN
            }
            if (useBubbles() && (!useBubble() || useCircleBubble())) {
                return TGCommentButton.VIEW_MODE_BUBBLE
            }
            return TGCommentButton.VIEW_MODE_INLINE
        }

    fun findMessageWithReplyInfo(): TdApi.Message? {
        val message = this.anchorMessageThreadMessage
        val interactionInfo = message.interactionInfo
        if (interactionInfo != null && interactionInfo.replyInfo != null) {
            return message
        }
        return null
    }

    protected fun needCommentButton(): Boolean {
        if (this.isScheduled || isSponsoredMessage() || !allowInteraction()) {
            return false
        }
        if (this.isChannel) {
            return findMessageWithReplyInfo() != null
        }
        if (this.isRepliesChat) {
            return FeatureToggles.SHOW_VIEW_IN_CHAT_BUTTON_IN_REPLIES && msg!!.forwardInfo != null && msg!!.forwardInfo.hasMessageSource() && msg!!.forwardInfo!!.source!!.chatId != msg!!.chatId
        }
        return false
    }

    protected fun needCommentButtonSeparator(): Boolean {
        return !drawBubbleTimeOverContent() || useForward()
    }

    fun openMessageThread() {
        val messageWithReplyInfo = findMessageWithReplyInfo()
        val targetMessage = if (messageWithReplyInfo != null) messageWithReplyInfo else this.newestMessage
        getMessageProperties(targetMessage.id, RunnableData { properties: MessageProperties? ->
            if (!properties!!.canGetMessageThread) {
                val messageThreadId = targetMessage.topicId.messageThreadId()
                if (messageThreadId != 0L) {
                    val highlightMessageId = toMessageId()
                    openMessageThread(GetMessageThread(targetMessage.chatId, targetMessage.id), highlightMessageId)
                }
                return@RunnableData
            }
            val highlightMessageId: MessageId?
            if (this.isChannel || this.isChannelAutoForward) {
                // View X Comments
                highlightMessageId = null
            } else if (this.isMessageThreadRoot) {
                // View X Replies
                highlightMessageId = MessageId(targetMessage.chatId, MessageId.MIN_VALID_ID)
            } else {
                // View Thread
                highlightMessageId = toMessageId()
            }
            openMessageThread(GetMessageThread(targetMessage.chatId, targetMessage.id), highlightMessageId)
        })
    }

    @JvmOverloads
    fun openMessageThread(highlightMessageId: MessageId, fallbackHighlightMessageId: MessageId? = null) {
        val query = GetMessageThread(highlightMessageId.chatId, highlightMessageId.messageId)
        val fallbackQuery: GetMessageThread?
        if (fallbackHighlightMessageId != null) {
            fallbackQuery = GetMessageThread(fallbackHighlightMessageId.chatId, fallbackHighlightMessageId.messageId)
        } else {
            fallbackQuery = null
        }
        openMessageThread(query, highlightMessageId, fallbackQuery, fallbackHighlightMessageId)
    }

    @JvmOverloads
    fun openMessageThread(
        query: GetMessageThread,
        highlightMessageId: MessageId? = null,
        fallbackQuery: GetMessageThread? = null,
        fallbackHighlightMessageId: MessageId? = null
    ) {
        if (openingComments.getValue()) return
        openingComments.setValue(true, needAnimateChanges())
        tdlib.client().send(query, Client.ResultHandler { result: TdApi.Object? ->
            runOnUiThreadOptional(Runnable {
                when (result!!.getConstructor()) {
                    MessageThreadInfo.CONSTRUCTOR -> {
                        val messageThread = result as MessageThreadInfo
                        val threadInfo = ThreadInfo.openedFromChat(
                            tdlib, messageThread,
                            this.chatId
                        )
                        if (Config.SHOW_CHANNEL_POST_REPLY_INFO_IN_COMMENTS && this.isChannel && msg!!.replyTo != null && msg!!.chatId == query.chatId && isDescendantOrSelf(
                                query.messageId
                            )
                        ) {
                            val message = threadInfo.getOldestMessage()
                            if (message != null && message.replyTo == null && tdlib.isChannelAutoForward(message)) {
                                message.replyTo = msg!!.replyTo
                            }
                        }
                        val params = ChatOpenParameters().keepStack().messageThread(threadInfo).after(RunnableLong { chatId: Long ->
                            openingComments.setValue(false, needAnimateChanges())
                        })
                        if (highlightMessageId != null) {
                            val finalHighlightMessageId: MessageId?
                            if (highlightMessageId.chatId != messageThread.chatId) {
                                finalHighlightMessageId = MessageId(messageThread.chatId, highlightMessageId.messageId, highlightMessageId.otherMessageIds)
                            } else {
                                finalHighlightMessageId = highlightMessageId
                            }
                            if (finalHighlightMessageId.isHistoryStart()) {
                                params.highlightMessage(MessagesManager.HIGHLIGHT_MODE_UNREAD, finalHighlightMessageId)
                            } else {
                                params.highlightMessage(finalHighlightMessageId)
                                params.ensureHighlightAvailable()
                            }
                        }
                        tdlib.ui().openChat(this, messageThread.chatId, params)
                    }

                    TdApi.Error.CONSTRUCTOR -> {
                        if ("MSG_ID_INVALID" == TD.errorText(result)) {
                            val needAnimateChanges = needAnimateChanges()
                            openingComments.setValue(false, needAnimateChanges)
                            if (this.isChannel) {
                                showCommentButtonError(Lang.getString(R.string.ChannelPostDeleted))
                            } else {
                                showCommentButtonError(TD.toErrorString(result))
                            }
                            return@Runnable
                        }
                        if (fallbackQuery != null) {
                            openingComments.setValue(false, false)
                            openMessageThread(fallbackQuery, fallbackHighlightMessageId)
                            return@Runnable
                        }
                        openingComments.setValue(false, needAnimateChanges())
                        showCommentButtonError(TD.toErrorString(result))
                    }
                }
            })
        })
    }

    private fun showCommentButtonError(text: String) {
        val view = findCurrentView()
        if (!needCommentButton() || view == null) {
            UI.showToast(text, Toast.LENGTH_SHORT)
            return
        }
        buildContentHint(view, TooltipOverlayView.LocationProvider { targetView: View?, outRect: Rect? -> commentButton!!.getRect(outRect) }, false)!!
            .show(tdlib, text).hideDelayed()
    }

    private fun computeBubbleHeight(): Int {
        var height = this.contentHeight
        if (replyData != null && !alignReplyHorizontally()) {
            height += this.bubbleReplyOffset
        }
        if (needName(true) && (flags and FLAG_HEADER_ENABLED) != 0) {
            height += bubbleNameHeight
            if (this.isPsa) {
                height += psaTitleHeight
            }
        }
        if (useForward()) {
            height += bubbleForwardOffset
        }
        if (hasFooter()) {
            height += this.footerHeight + this.footerPaddingTop + this.footerPaddingBottom
        }
        if (!useMediaBubbleReactions() && !useStickerBubbleReactions() && useReactionBubbles()) {
            height =
                (height + (messageReactions!!.getAnimatedHeight() + (xReactionBubblePaddingBottom - Screen.dp(2f)) * messageReactions.getVisibility())).toInt()
        }
        if (commentButton!!.isVisible() && commentButton.isInline()) {
            height += commentButton.getAnimatedHeight(0, commentButton.getVisibility())
        }

        return height
    }

    private val bubbleReplyOffset: Int
        get() = ReplyComponent.height() + Screen.dp(if (useBubble()) 3f else 6f) - (if (useForward()) Screen.dp(
            9f
        ) else 0)

    fun rebuildLayout() {
        val width = this.width
        if (width != 0) {
            this.width = 0
            buildLayout(width)
        }
    }

    fun onUpdateTextSize() {
        messageReactions!!.onUpdateTextSize()
    }

    fun prepareLayout() {
        if (this.width != 0) {
            rebuildLayout()
        } else {
            buildLayout(manager.getRecyclerWidth())
        }
    }

    fun buildLayout(width: Int) {
        if (width == 0 || this.width == width) {
            return
        }

        this.width = width

        if (useBubbles()) {
            this.realContentX = computeBubbleLeft()
            this.realContentMaxWidth =
                width - (if (Device.NEED_BIGGER_BUBBLE_OFFSETS) xBubbleLeft2 else xBubbleLeft1) - computeBubbleLeft() - (Screen.dp(if (this.isThreadHeader) 8f else 56f))

            if (useForward()) {
                this.realContentX += Screen.dp(11f)
                this.realContentMaxWidth -= Screen.dp(11f)
            }

            val bubblePaddingLeft = this.bubblePaddingLeft
            val bubblePaddingRight = this.bubblePaddingRight

            this.realContentX += bubblePaddingLeft
            this.realContentMaxWidth -= bubblePaddingLeft + bubblePaddingRight
        } else {
            if (!useForward()) {
                this.realContentX = contentLeft
                this.contentY = (if ((flags and FLAG_HEADER_ENABLED) != 0) xContentTop else xPaddingTop) + this.headerPadding
            } else {
                this.realContentX = xfContentLeft
                this.contentY = this.forwardTop + forwardHeaderHeight
            }
            this.realContentMaxWidth = width - xPaddingRight - this.realContentX
            if (this.isPsa) {
                this.contentY += psaTitleHeight
            }
        }

        updateContentPositions(true)
        if (allowMessageHorizontalExtend()) {
            if (replyData != null && !alignReplyHorizontally()) {
                this.contentY += ReplyComponent.height()
            }
            buildHeader()
            buildForward()
            if (alignReplyHorizontally()) {
                buildContent(this.contentMaxWidth)
                buildReply()
            } else {
                buildReply()
                buildContent(this.contentMaxWidth)
            }
        } else {
            if (replyData != null) {
                this.contentY += ReplyComponent.height()
            }
            buildContent(this.contentMaxWidth)
            buildHeader()
            buildForward()
            buildReply()
        }

        buildFooter()
        buildReactions(false)
        buildBubble(true)

        if (useBubbles()) {
            this.contentY = topContentEdge + this.bubblePaddingTop
            if (replyData != null && !alignReplyHorizontally()) {
                this.contentY += this.bubbleReplyOffset
            }
            if (needName(true) && (flags and FLAG_HEADER_ENABLED) != 0) {
                this.contentY += bubbleNameHeight
                if (this.isPsa) {
                    this.contentY += psaTitleHeight
                }
            }
            if (useForward()) {
                this.contentY += bubbleForwardOffset
            }
        }

        buildMarkup()

        height = computeHeight()

        flags = flags or FLAG_LAYOUT_BUILT
    }

    @get:ColorInt
    val contentReplaceColor: Int
        get() {
            if (useBubbles()) {
                return Theme.getColor(if (this.isOutgoingBubble) ColorId.bubbleOut_background else ColorId.bubbleIn_background)
            } else {
                val color = Theme.getColor(ColorId.chatBackground)
                if (selectionFactor > 0f) {
                    return compositeColor(color, alphaColor(selectionFactor, Theme.chatSelectionColor()))
                } else {
                    return color
                }
            }
        }

    val isOutgoingBubble: Boolean
        get() = useBubbles() && this.isOutgoing && !this.isChannel && !this.isEventLog

    protected fun alignBubbleRight(): Boolean {
        return useBubbles() && (this.isOutgoingBubble != Lang.rtl())
    }

    private fun updateContentPositions(maxOnly: Boolean) {
        if (useFullWidth()) {
            this.contentX = 0
            this.contentMaxWidth = width
        } else if (alignBubbleRight()) {
            this.contentX = width - rightContentEdge - leftContentEdge + this.realContentX
            this.contentMaxWidth = this.realContentMaxWidth
        } else {
            this.contentX = this.realContentX
            this.contentMaxWidth = this.realContentMaxWidth
        }
    }

    private fun measureKeyboardLeft(): Int {
        return if (useBubbles()) if (alignBubbleRight()) this.actualRightContentEdge - inlineKeyboard!!.getWidth() else this.actualLeftContentEdge else this.realContentX
    }

    private fun measureKeyboardTop(): Int {
        return if (useBubbles()) bottomContentEdge + TGInlineKeyboard.getButtonSpacing() else this.contentY + this.contentHeight + this.paddingBottom + (if (hasFooter()) this.footerHeight + this.footerPaddingTop + this.footerPaddingBottom else 0)
    }

    protected fun rebuildContentDimensions(): Boolean {
        if ((flags and FLAG_LAYOUT_BUILT) != 0) {
            updateContentPositions(true)
            buildBubble(false)
            val oldHeight = height
            height = computeHeight()
            return oldHeight != height
        } else {
            return rebuildContent()
        }
    }

    fun rebuildContent(): Boolean {
        if ((flags and FLAG_LAYOUT_BUILT) != 0) {
            updateContentPositions(true)
            buildContent(this.contentMaxWidth)
            buildBubble(false)
            val oldHeight = height
            height = computeHeight()
            if (oldHeight != height) {
                manager.onMessageHeightChanged(this.chatId, this.id, oldHeight, height)
            }
            return oldHeight != height
        } else {
            rebuildLayout()
            return false
        }
    }

    protected fun rebuildAndUpdateContent() {
        val isBackground = Looper.myLooper() != Looper.getMainLooper()
        if (isBackground) {
            if (rebuildContent()) {
                requestLayout()
            } else {
                postInvalidate(true)
            }
        } else {
            if (rebuildContent()) {
                requestLayout()
            } else {
                invalidate(true)
            }
        }
    }

    private val bubbleViewPaddingTop: Int
        get() = if (useBubble()) (if ((flags and FLAG_HEADER_ENABLED) != 0) xBubbleViewPadding else xBubbleViewPaddingSmall) else xBubbleViewPadding

    private val bubbleViewPaddingBottom: Int
        get() = if (useBubble()) (if (this.isBottomMessage || (inlineKeyboard != null && !inlineKeyboard!!.isEmpty())) xBubbleViewPadding else xBubbleViewPaddingSmall) else xBubbleViewPadding

    protected val paddingBottom: Int
        get() = if (useBubbles()) this.bubbleViewPaddingBottom else xPaddingBottom

    val extraPadding: Int
        get() {
            if (needSponsorSmallPadding) {
                return Screen.dp(7f)
            }

            return if ((flags and FLAG_EXTRA_PADDING) != 0) Screen.dp(7f) + (if (messagesController().needExtraBigPadding()) Screen.dp(
                48f
            ) else 0) else 0
        }

    fun computeHeight(): Int {
        val headerPadding = this.headerPadding
        val extraPadding = this.extraPadding
        if (useBubbles()) {
            var height = bottomContentEdge + this.paddingBottom + extraPadding
            if (inlineKeyboard != null && !inlineKeyboard!!.isEmpty()) {
                height += inlineKeyboard!!.getHeight() + TGInlineKeyboard.getButtonSpacing()
            }
            if (useReactionBubbles()) {
                if (useMediaBubbleReactions()) {
                    height = (height + (messageReactions!!.getAnimatedHeight() + xReactionBubblePaddingTop * messageReactions.getVisibility())).toInt()
                } else if (useStickerBubbleReactions()) {
                    height = (height + (messageReactions!!.getAnimatedHeight() + xReactionBubblePaddingTop * messageReactions.getVisibility())).toInt()
                }
            }
            if (commentButton!!.isBubble()) {
                height += commentButton.getAnimatedHeight(Screen.dp(5f), commentButton.getVisibility())
            }
            return fromTo(height, Screen.dp(HIDDEN_BY_MESSAGE_FILTER_HEIGHT.toFloat()) + extraPadding + headerPadding, isHiddenByFilter.getFloatValue())
        } else {
            var height = this.contentY + this.contentHeight + this.paddingBottom + extraPadding
            if (inlineKeyboard != null && !inlineKeyboard!!.isEmpty()) {
                height += inlineKeyboard!!.getHeight() + xPaddingBottom
            }
            val useReactionBubbles = useReactionBubbles()
            if (useReactionBubbles) {
                height = (height + (messageReactions!!.getAnimatedHeight() + xReactionBubblePaddingTop * messageReactions.getVisibility())).toInt()
            }
            if (hasFooter()) {
                height += this.footerHeight + this.footerPaddingTop + this.footerPaddingBottom
            }
            if (commentButton!!.isVisible() && commentButton.isInline()) {
                height += commentButton.getAnimatedHeight(if (useReactionBubbles) -Screen.dp(2f) else 0, commentButton.getVisibility())
            }
            return fromTo(height, Screen.dp(HIDDEN_BY_MESSAGE_FILTER_HEIGHT.toFloat()) + extraPadding + headerPadding, isHiddenByFilter.getFloatValue())
        }
    }

    protected open fun useLargeHeight(): Boolean {
        return false
    }

    fun useFullWidth(): Boolean {
        return !useForward() && !useBubbles() && preferFullWidth() && !this.isEventLog
    }

    protected open fun preferFullWidth(): Boolean {
        return false
    }

    protected open fun buildContent(maxWidth: Int) {}

    protected open fun getContentWidth(): Int = realContentMaxWidth

    protected open val contentHeight: Int
        get() = 10

    protected open fun centerBubble(): Boolean {
        return false
    }

    // Drawing
    private fun needHeader(): Boolean {
        return (flags and FLAG_HEADER_ENABLED) != 0
    }

    private fun shouldShowTicks(): Boolean {
        return !headerDisabled() && !this.isChannel && !TD.isFailed(msg) && ((needHeader() && this.isOutgoing) || (flags and FLAG_SHOW_TICKS) != 0) && !(!useBubbles() && noUnread() && !needHeader()) && !this.isFailed
    }

    private fun shouldShowEdited(): Boolean {
        return !headerDisabled() && (isEdited() || this.isBeingEdited) && msg!!.viaBotUserId == 0L && !sender.isBot() && !sender.isServiceAccount() && (if (useBubbles()) useBubbleTime() else (!this.isOutgoing || hasHeader() || !shouldShowTicks())) && (this.viewCount > 0 || !this.isEventLog)
    }

    private fun shouldShowMessageRestrictedWarning(): Boolean {
        return hasFlag(flags, FLAG_UNSUPPORTED) || this.isRestrictedByTelegram
    }

    private fun needAvatar(): Boolean {
        if (!useBubbles()) {
            return true
        }
        if (isSponsoredMessage()) {
            return sponsoredMessage!!.sponsor.photo != null
        }
        if (this.isThreadHeader && messagesController().messageThread!!.areComments()) {
            return false
        }
        if (chat != null) {
            when (chat!!.type.getConstructor()) {
                TdApi.ChatTypeBasicGroup.CONSTRUCTOR -> {
                    return !this.isOutgoing
                }

                ChatTypeSupergroup.CONSTRUCTOR -> {
                    return this.isEventLog || (tdlib.isSupergroupChat(chat) && !this.isOutgoing)
                }

                ChatTypePrivate.CONSTRUCTOR -> {
                    return ((flags and FLAG_SELF_CHAT) != 0 && !this.isOutgoing) || this.isRepliesChat
                }
            }
        }
        return !this.isOutgoing && this.isDemoGroupChat
    }

    protected val isDemoChat: Boolean
        get() = msg!!.chatId == 0L

    protected val isDemoGroupChat: Boolean
        get() = this.isDemoChat && manager.isDemoGroupChat()

    protected fun userForId(userId: Long): TdApi.User? {
        if (!msg!!.isOutgoing && this.isDemoChat) return manager.demoParticipant(userId)
        else return tdlib.cache().user(userId)
    }

    protected fun needName(): Boolean {
        return needName(true)
    }

    private fun needName(allowVia: Boolean): Boolean {
        if (!useBubbles() ||
            (useBubble() && ((msg!!.viaBotUserId != 0L && !useForward() && allowVia) ||
                    ((flags and FLAG_SELF_CHAT) != 0 && !this.isOutgoing)))
        ) {
            return true
        }
        if (!useBubble() || separateReplyFromBubble()) {
            return false
        }
        if (isSponsoredMessage() && useBubbles()) return true
        if (this.isPsa && forceForwardOrImportInfo()) return true
        if (this.isOutgoing && (sender.isAnonymousGroupAdmin() || sender.isChannel())) return true
        if (chat != null) {
            when (chat!!.type.getConstructor()) {
                TdApi.ChatTypeBasicGroup.CONSTRUCTOR -> {
                    return !this.isOutgoing
                }

                ChatTypeSupergroup.CONSTRUCTOR -> {
                    return !this.isOutgoing && !(chat!!.type as ChatTypeSupergroup).isChannel
                }
            }
        }
        return !this.isOutgoing && this.isDemoGroupChat
    }

    private fun useBubbleTime(): Boolean {
        return !headerDisabled() && (!useForward() || (this.isOutgoing || (flags and FLAG_HEADER_ENABLED) != 0)) && (this !is TGMessageCall)
    }

    protected fun centerReactions(): Boolean {
        return this is TGMessageService
    }

    protected open fun needBubbleCornerFix(): Boolean {
        return false
    }

    private fun drawBubbleShadow(c: Canvas, factor: Float) {
        val alpha = (255f * factor).toInt()
        if (alpha <= 0) {
            return
        }

        val paint: Paint = shadowPaint!!
        paint.setAlpha(alpha)

        if (useCircleBubble()) {
            // c.drawBitmap(videoShadow, bubblePathRect.centerX() - videoShadow.getWidth() / 2, bubblePathRect.centerY() - videoShadow.getHeight() / 2, paint);
            return
        }

        var offset = Screen.dp(2f)

        val left = bubblePathRect.left - offset
        val top = bubblePathRect.top - offset
        val right = bubblePathRect.right + offset
        val bottom = bubblePathRect.bottom + offset

        val topLeft: Bitmap =
            (if (org.thunderdog.challegram.theme.Theme.isBubbleRadiusBig(this.bubbleTopLeftRadius)) TGMessage.Companion.cornerTopLeftBig else TGMessage.Companion.cornerTopLeftSmall)!!
        c.drawBitmap(topLeft, left, top, paint)

        val bottomLeft: Bitmap =
            (if (org.thunderdog.challegram.theme.Theme.isBubbleRadiusBig(this.bubbleBottomLeftRadius)) TGMessage.Companion.cornerBottomLeftBig else TGMessage.Companion.cornerBottomLeftSmall)!!
        c.drawBitmap(bottomLeft, left, bottom - bottomLeft.getHeight(), paint)

        val topRight: Bitmap =
            (if (org.thunderdog.challegram.theme.Theme.isBubbleRadiusBig(this.bubbleTopRightRadius)) TGMessage.Companion.cornerTopRightBig else TGMessage.Companion.cornerTopRightSmall)!!
        c.drawBitmap(topRight, right - topRight.getWidth(), top, paint)

        val bottomRight: Bitmap =
            (if (org.thunderdog.challegram.theme.Theme.isBubbleRadiusBig(this.bubbleBottomRightRadius)) TGMessage.Companion.cornerBottomRightBig else TGMessage.Companion.cornerBottomRightSmall)!!
        c.drawBitmap(bottomRight, right - bottomRight.getWidth(), bottom - bottomRight.getHeight(), paint)

        paint.setAlpha(255)

        val restoreToCount = Views.save(c)

        offset = Screen.dp(18f)
        var shadowLeft: Float
        var shadowTop: Float
        var shadowRight: Float
        var shadowBottom: Float

        var cx = 0f
        var cy = 0f
        var tx: Float
        var ty: Float

        shadowLeft = left + topLeft.getWidth()
        shadowRight = right - topRight.getWidth()
        if (shadowRight - shadowLeft > 0) {
            topShadowPaint!!.setAlpha(alpha)
            tx = shadowLeft
            ty = bubblePathRect.top - topShadow!!.getHeight() + offset
            c.translate(tx - cx, ty - cy)
            c.drawRect(0f, 0f, shadowRight - shadowLeft, topShadow!!.getHeight().toFloat(), topShadowPaint!!)
            cx = tx
            cy = ty
        }

        shadowLeft = left + bottomLeft.getWidth()
        shadowRight = right - bottomRight.getWidth()
        if (shadowRight - shadowLeft > 0) {
            bottomShadowPaint!!.setAlpha(alpha)
            tx = shadowLeft
            ty = bubblePathRect.bottom - offset
            c.translate(tx - cx, ty - cy)
            c.drawRect(0f, 0f, shadowRight - shadowLeft, bottomShadow!!.getHeight().toFloat(), bottomShadowPaint!!)
            cx = tx
            cy = ty
        }

        shadowTop = top + topLeft.getHeight()
        shadowBottom = bottom - bottomLeft.getHeight()
        if (shadowBottom - shadowTop > 0) {
            leftShadowPaint!!.setAlpha(alpha)
            tx = bubblePathRect.left - leftShadow!!.getWidth() + offset
            ty = shadowTop
            c.translate(tx - cx, ty - cy)
            c.drawRect(0f, 0f, leftShadow!!.getWidth().toFloat(), shadowBottom - shadowTop, leftShadowPaint!!)
            cx = tx
            cy = ty
        }

        shadowTop = top + topRight.getHeight()
        shadowBottom = bottom - bottomRight.getHeight()
        if (shadowBottom - shadowTop > 0) {
            rightShadowPaint!!.setAlpha(alpha)
            tx = bubblePathRect.right - offset
            ty = shadowTop
            c.translate(tx - cx, ty - cy)
            c.drawRect(0f, 0f, rightShadow!!.getWidth().toFloat(), shadowBottom - shadowTop, rightShadowPaint!!)
            cx = tx
            cy = ty
        }

        Views.restore(c, restoreToCount)
    }

    protected open fun drawBubble(c: Canvas, paint: Paint, stroke: Boolean, padding: Int) {
        if (paint.getAlpha() == 0) {
            return
        }

        val left = bubblePathRect.left - padding
        val top = bubblePathRect.top - padding
        val right = bubblePathRect.right + padding
        val bottom = bubblePathRect.bottom + padding

        val rectF = Paints.getRectF()
        if (this.bubbleTopLeftRadius != 0f) {
            rectF.set(left, top, left + this.bubbleTopLeftRadius * 2, top + this.bubbleTopLeftRadius * 2)
            c.drawArc(rectF, 180f, 90f, !stroke, paint)
            if (this.bubbleTopLeftRadius < this.bubbleTopRightRadius && !stroke) {
                c.drawRect(left, top + this.bubbleTopLeftRadius, left + this.bubbleTopLeftRadius, top + this.bubbleTopRightRadius, paint)
            }
        }
        if (this.bubbleTopRightRadius != 0f) {
            rectF.set(right - this.bubbleTopRightRadius * 2, top, right, top + this.bubbleTopRightRadius * 2)
            c.drawArc(rectF, 270f, 90f, !stroke, paint)
            if (this.bubbleTopRightRadius < this.bubbleTopLeftRadius && !stroke) {
                c.drawRect(right - this.bubbleTopRightRadius, top + this.bubbleTopRightRadius, right, top + this.bubbleTopLeftRadius, paint)
            }
        }
        if ((this.bubbleTopLeftRadius != 0f || this.bubbleTopRightRadius != 0f) && !stroke) {
            c.drawRect(
                left + this.bubbleTopLeftRadius, top, right - this.bubbleTopRightRadius, top + max(
                    this.bubbleTopLeftRadius,
                    this.bubbleTopRightRadius
                ), paint
            )
        }
        if (this.bubbleBottomLeftRadius != 0f) {
            rectF.set(left, bottom - this.bubbleBottomLeftRadius * 2, left + this.bubbleBottomLeftRadius * 2, bottom)
            c.drawArc(rectF, 90f, 90f, !stroke, paint)
            if (this.bubbleBottomLeftRadius < this.bubbleBottomRightRadius && !stroke) {
                c.drawRect(left, bottom - this.bubbleBottomRightRadius, left + this.bubbleBottomLeftRadius, bottom - this.bubbleBottomLeftRadius, paint)
            }
        }
        if (this.bubbleBottomRightRadius != 0f) {
            rectF.set(right - this.bubbleBottomRightRadius * 2, bottom - this.bubbleBottomRightRadius * 2, right, bottom)
            c.drawArc(rectF, 0f, 90f, !stroke, paint)
            if (this.bubbleBottomRightRadius < this.bubbleBottomLeftRadius && !stroke) {
                c.drawRect(right - this.bubbleBottomRightRadius, bottom - this.bubbleBottomLeftRadius, right, bottom - this.bubbleBottomRightRadius, paint)
            }
        }
        if ((this.bubbleBottomLeftRadius != 0f || this.bubbleBottomRightRadius != 0f) && !stroke) {
            c.drawRect(
                left + this.bubbleBottomLeftRadius, bottom - max(
                    this.bubbleBottomLeftRadius,
                    this.bubbleBottomRightRadius
                ), right - this.bubbleBottomRightRadius, bottom, paint
            )
        }

        if (stroke) {
            c.drawLine(left + this.bubbleTopLeftRadius, top, right - this.bubbleTopRightRadius, top, paint)
            c.drawLine(left + this.bubbleBottomLeftRadius, bottom, right - this.bubbleBottomRightRadius, bottom, paint)
            c.drawLine(left, top + this.bubbleTopLeftRadius, left, bottom - this.bubbleBottomLeftRadius, paint)
            c.drawLine(right, top + this.bubbleTopRightRadius, right, bottom - this.bubbleBottomRightRadius, paint)
        } else {
            val bubbleTop = top + max(this.bubbleTopLeftRadius, this.bubbleTopRightRadius)
            val bubbleBottom = bottom - max(this.bubbleBottomLeftRadius, this.bubbleBottomRightRadius)
            if (bubbleBottom > bubbleTop) c.drawRect(left, bubbleTop, right, bubbleBottom, paint)
        }
    }

    fun drawBackground(view: MessageView, c: Canvas) {
        val moveFactor = swipeHelper.getMoveFactor()

        if (moveFactor != 0f && !useBubbles()) {
            c.drawRect(
                0f,
                findTopEdge().toFloat(),
                view.getMeasuredWidth().toFloat(),
                findBottomEdge().toFloat(),
                Paints.fillingPaint(getSelectionColor(moveFactor))
            )
        }
        if (selectionFactor != 0f) {
            drawSelection(view, c)
        }
        if (highlightFactor != 0f) {
            drawHighlight(view, c)
        }
    }

    fun hasDate(): Boolean {
        return (flags and FLAG_SHOW_DATE) != 0
    }

    @JvmField
    var tag: Any? = null

    val datePadding: Int
        get() = Screen.dp(if (useBubbles()) 8f else 10f)

    val bubbleDateBackgroundColor: Int
        get() = manager.getOverlayColor(ColorId.NONE, ColorId.bubble_date, ColorId.bubble_date_noWallpaper, PropertyId.WALLPAPER_OVERRIDE_DATE)

    val bubbleDateTextColor: Int
        get() = manager.getColor(ColorId.NONE, ColorId.bubble_dateText, ColorId.bubble_dateText_noWallpaper, PropertyId.WALLPAPER_OVERRIDE_DATE)

    protected val unreadSeparatorBackgroundColor: Int
        get() = manager.getOverlayColor(ColorId.unread, ColorId.bubble_unread, ColorId.bubble_unread_noWallpaper, PropertyId.WALLPAPER_OVERRIDE_UNREAD)

    protected val unreadSeparatorContentColor: Int
        get() = manager.getColor(ColorId.unreadText, ColorId.bubble_unreadText, ColorId.bubble_unreadText_noWallpaper, PropertyId.WALLPAPER_OVERRIDE_UNREAD)

    val bubbleMediaReplyBackgroundColor: Int
        get() = manager.getOverlayColor(
            ColorId.NONE,
            ColorId.bubble_mediaReply,
            ColorId.bubble_mediaReply_noWallpaper,
            PropertyId.WALLPAPER_OVERRIDE_MEDIA_REPLY
        )

    val bubbleMediaReplyTextColor: Int
        get() = manager.getColor(
            ColorId.NONE,
            ColorId.bubble_mediaReplyText,
            ColorId.bubble_mediaReplyText_noWallpaper,
            PropertyId.WALLPAPER_OVERRIDE_MEDIA_REPLY
        )

    protected val bubbleTimeColor: Int
        get() = manager.getOverlayColor(ColorId.NONE, ColorId.bubble_mediaTime, ColorId.bubble_mediaTime_noWallpaper, PropertyId.WALLPAPER_OVERRIDE_TIME)

    protected val bubbleTimeTextColor: Int
        get() = manager.getColor(ColorId.NONE, ColorId.bubble_mediaTimeText, ColorId.bubble_mediaTimeText_noWallpaper, PropertyId.WALLPAPER_OVERRIDE_TIME)

    val bubbleButtonBackgroundColor: Int
        get() = manager.getOverlayColor(ColorId.NONE, ColorId.bubble_button, ColorId.bubble_button_noWallpaper, PropertyId.WALLPAPER_OVERRIDE_BUTTON)

    val bubbleButtonRippleColor: Int
        get() = manager.getOverlayColor(
            ColorId.NONE,
            ColorId.bubble_buttonRipple,
            ColorId.bubble_buttonRipple_noWallpaper,
            PropertyId.WALLPAPER_OVERRIDE_BUTTON
        )

    val bubbleButtonTextColor: Int
        get() = manager.getColor(ColorId.NONE, ColorId.bubble_buttonText, ColorId.bubble_buttonText_noWallpaper, PropertyId.WALLPAPER_OVERRIDE_BUTTON)

    fun drawDate(c: Canvas, centerX: Int, startY: Int, detachFactor: Float, alpha: Float): Boolean {
        if (!hasDate()) {
            return false
        }
        if (alpha == 0f) {
            return true
        }
        val rectF = Paints.getRectF()
        val textX: Int
        var textY: Int
        val textPaint: TextPaint
        if (useBubbles()) {
            val color = this.bubbleDateBackgroundColor
            val textColor = this.bubbleDateTextColor

            val padding = Screen.dp(8f)
            rectF.set(
                (centerX - this.dateWidth / 2 - padding).toFloat(),
                (startY + Screen.dp(5f)).toFloat(),
                (centerX + this.dateWidth / 2 + padding).toFloat(),
                (startY + Screen.dp(5f) + Screen.dp(26f)).toFloat()
            )
            val radius = Screen.dp(Theme.getBubbleDateRadius())
            c.drawRoundRect(rectF, radius.toFloat(), radius.toFloat(), Paints.fillingPaint(alphaColor(alpha, color)))
            textX = centerX - this.dateWidth / 2
            textY = startY + xDateTop - Screen.dp(3f)
            textPaint = Paints.getBoldPaint13((flags and FLAG_DATE_FAKE_BOLD) != 0, textColor)
        } else {
            if (detachFactor > 0f) {
                val padding = Screen.dp(10f)
                rectF.set(
                    (centerX - this.dateWidth / 2 - padding).toFloat(),
                    (startY + Screen.dp(8f)).toFloat(),
                    (centerX + this.dateWidth / 2 + padding).toFloat(),
                    (startY + Screen.dp(8f) + Screen.dp(26f)).toFloat()
                )
                val radius = Screen.dp(Theme.getDateRadius())
                c.drawRoundRect(
                    rectF,
                    radius.toFloat(),
                    radius.toFloat(),
                    Paints.fillingPaint(alphaColor(alpha * detachFactor, Theme.getColor(ColorId.chatBackground)))
                )
                c.drawRoundRect(
                    rectF,
                    radius.toFloat(),
                    radius.toFloat(),
                    Paints.getProgressPaint(alphaColor(alpha * detachFactor, Theme.separatorColor()), max(1, Screen.dp(.5f)).toFloat())
                )
            }
            textX = centerX - this.dateWidth / 2
            textY = startY + xDateTop
            if ((flags and FLAG_SHOW_BADGE) != 0) {
                textY += Screen.dp(1f)
            }
            textPaint = Paints.getTitlePaint((flags and FLAG_DATE_FAKE_BOLD) != 0)
        }
        val savedAlpha = if (alpha != 1f) textPaint.getAlpha() else 255
        if (alpha != 1f) {
            textPaint.setAlpha((savedAlpha.toFloat() * alpha).toInt())
        }
        c.drawText(this.drawDateText!!, textX.toFloat(), textY.toFloat(), textPaint)
        if (alpha != 1f) {
            textPaint.setAlpha(savedAlpha)
        }
        return true
    }

    val drawDateY: Int
        get() = if ((flags and FLAG_SHOW_BADGE) != 0) xBadgeHeight + (if (useBubbles()) Screen.dp(
            3.5f
        ) else 0) else 0

    fun separateReplyFromContent(): Boolean {
        return useBubbles() && (!useBubble() || separateReplyFromBubble())
    }

    fun draw(
        view: MessageView,
        c: Canvas,
        avatarReceiver: AvatarReceiver,
        replyReceiver: Receiver?,
        replyTextMediaReceiver: ComplexReceiver?,
        previewReceiver: DoubleImageReceiver?,
        contentReceiver: ImageReceiver?,
        gifReceiver: GifReceiver?,
        complexReceiver: ComplexReceiver?
    ) {
        val viewWidth = view.getMeasuredWidth()
        val viewHeight = view.getMeasuredHeight()

        val useBubbles = useBubbles()
        val useReactionBubbles = useReactionBubbles()
        val reactionsDrawMode = this.reactionsDrawMode

        val selectableFactor = manager.getSelectableFactor()

        checkEdges()

        val isHiddenFactor = isHiddenByFilter.getFloatValue()
        if (isHiddenFactor == 1f) {
            drawHiddenMessage(view, c, isHiddenFactor)
            return
        }

        // "Unread messages" / "Discussion started" badge
        if ((flags and FLAG_SHOW_BADGE) != 0) {
            var top = 0
            if (useBubbles) {
                top += if ((flags and FLAG_SHOW_DATE) != 0) Screen.dp(3.5f) else Screen.dp(2f)
            }
            val bottom: Int = xBadgeHeight + top
            val shadow = Theme.getBubbleUnreadShadow()
            if (shadow > 0f) {
                ShadowView.drawTopShadow(c, 0, width, top, shadow)
                ShadowView.drawBottomShadow(c, 0, width, bottom, shadow)
            }
            c.drawRect(0f, top.toFloat(), width.toFloat(), bottom.toFloat(), Paints.fillingPaint(this.unreadSeparatorBackgroundColor))
            val mBadge = getBadgePaint(uBadge!!.needFakeBold)
            val color = this.unreadSeparatorContentColor
            mBadge.setColor(color)
            c.drawText(uBadge!!.text, pBadgeX.toFloat(), (xBadgeTop + top).toFloat(), mBadge)
            if (this.isFirstUnread) {
                val iconTop = Screen.dp(4.5f)
                Drawables.draw(c, iBadge, pBadgeIconX.toFloat(), (iconTop + top).toFloat(), Paints.getUnreadSeparationPaint(color))
            }
        }

        if (useBubbles && !needViewGroup()) {
            drawBackground(view, c)
        }

        val savedTranslation = translation != 0f
        if (savedTranslation) {
            c.save()
            c.translate(translation, 0f)
        }

        // selection and highlight background
        if (!useBubbles && !needViewGroup()) {
            drawBackground(view, c)
        }

        val contentOffset = getSelectableContentOffset(selectableFactor)
        if (contentOffset != 0) {
            c.save()
            c.translate((contentOffset * (if (Lang.rtl()) -1 else 1)).toFloat(), 0f)
        }

        val hasBubble = useBubbles && useBubble()
        var lineFactor = 0f
        if (hasBubble) {
            val bubbleColor = Theme.getColor(if (this.isOutgoingBubble && !useCircleBubble()) ColorId.bubbleOut_background else ColorId.bubbleIn_background)
            lineFactor = Theme.getBubbleOutlineFactor()
            /*if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP && lineFactor < 1f) {
        c.save();
        int offset = Screen.dp(1.5f);
        c.clipRect(getActualLeftContentEdge(), bottomContentEdge - (xBubblePadding + xBubblePaddingSmall) * 2, getActualRightContentEdge(), bottomContentEdge + offset * 2);
        c.translate(0, offset);
        drawBubble(c, Paints.fillingPaint(Utils.alphaColor((1f - lineFactor) * .2f, 0xff000000)), false);
        c.restore();
      }*/
            if (lineFactor != 1f) {
                drawBubbleShadow(c, 1f - lineFactor)
            }
            if (lineFactor != 0f) {
                val outlineColor = this.outlineColor
                val outlineWidthDp = Theme.getBubbleOutlineSize()
                if (outlineWidthDp > 0f) {
                    val outlineWidth = max(1, Screen.dp(outlineWidthDp))
                    drawBubble(c, Paints.getProgressPaint(alphaColor(lineFactor, outlineColor), outlineWidth.toFloat()), true, 0)
                }
            }
            drawBubble(c, Paints.fillingPaint(bubbleColor), false, 0)
        }

        // Content universal
        if (needComplexReceiver()) {
            drawContent(view, c, this.contentX, this.contentY, this.contentMaxWidth, complexReceiver)
        } else if (needGifReceiver()) {
            drawContent(view, c, this.contentX, this.contentY, this.contentMaxWidth, previewReceiver, gifReceiver)
        } else if (needImageReceiver()) {
            drawContent(view, c, this.contentX, this.contentY, this.contentMaxWidth, previewReceiver, contentReceiver)
        } else {
            drawContent(view, c, this.contentX, this.contentY, this.contentMaxWidth)
        }

        if (hasBubble) {
            if (needBubbleCornerFix()) {
                val padding = getBubbleContentPadding()
                drawCornerFixes(
                    c, this, 1f,
                    bubblePathRect.left + padding, bubblePathRect.top + padding, bubblePathRect.right - padding, bubblePathRect.bottom - padding,
                    this.bubbleTopLeftRadius, this.bubbleTopRightRadius, this.bubbleBottomRightRadius, this.bubbleBottomLeftRadius
                )
            }
            if (commentButton!!.isVisible() && commentButton.isInline()) {
                val left = leftContentEdge
                val right = rightContentEdge
                val bottom = bottomContentEdge
                val top = bottom - commentButton.getAnimatedHeight(0, commentButton.getVisibility())
                if (needCommentButtonSeparator()) {
                    val separatorColor = alphaColor(0.15f * commentButton.getVisibility(), this.decentColor)
                    val separatorPaint = Paints.strokeSmallPaint(separatorColor)
                    c.drawLine((left + Screen.dp(7f)).toFloat(), top.toFloat(), (right - Screen.dp(7f)).toFloat(), top.toFloat(), separatorPaint)
                }
                commentButton.draw(view, c, view, left, top, right, bottom)
            }
        }

        if (hasFooter()) {
            drawFooter(view, c)
        }

        // Header
        if ((flags and FLAG_HEADER_ENABLED) != 0) {
            // Avatar
            if (needAvatar()) {
                val cx = avatarReceiver.centerX().toFloat()
                val cy = avatarReceiver.centerY().toFloat()
                if (useFullWidth()) {
                    c.drawCircle(cx, cy, (xAvatarRadius + Screen.dp(2.5f)).toFloat(), Paints.fillingPaint(Theme.getColor(ColorId.chatBackground)))
                }
                if (avatarReceiver.needPlaceholder()) avatarReceiver.drawPlaceholder(c)
                avatarReceiver.draw(c)
            }

            // Author
            if (needName(true)) {
                val left: Int = if (useBubbles) this.actualLeftContentEdge + xBubblePadding + xBubblePaddingSmall else contentLeft
                val top: Int = if (useBubbles) topContentEdge + xBubbleNameTop else xNameTop + this.headerPadding
                val isPsa = this.isPsa && forceForwardOrImportInfo()
                if (hAuthorNameT != null) {
                    var newTop = if (useBubbles) topContentEdge + Screen.dp(9f) else this.headerPadding + Screen.dp(1f)
                    if (isPsa && hPsaTextT != null) {
                        hPsaTextT!!.draw(c, left, left + hPsaTextT!!.getWidth(), 0, newTop)
                        newTop += psaTitleHeight
                    }
                    hAuthorNameT!!.draw(c, left, left + hAuthorNameT!!.getWidth(), 0, newTop)
                    if (hAuthorEmojiStatus != null) {
                        hAuthorEmojiStatus!!.draw(c, left + hAuthorNameT!!.getWidth() + Screen.dp(3f), newTop, 1f, view.getEmojiStatusReceiver())
                    }
                    if (sender.hasChatMark() && hAuthorChatMark != null) {
                        var cmLeft = (left + hAuthorNameT!!.getWidth() + Screen.dp(3f)
                                + (if (hAuthorEmojiStatus != null) hAuthorEmojiStatus!!.getWidth(Screen.dp(3f)) else 0))
                        val rct = Paints.getRectF()
                        rct.set(
                            cmLeft.toFloat(),
                            newTop.toFloat(),
                            (cmLeft + hAuthorChatMark!!.getWidth() + Screen.dp(8f)).toFloat(),
                            (newTop + hAuthorNameT!!.getLineHeight(false)).toFloat()
                        )
                        c.drawRoundRect(
                            rct,
                            Screen.dp(2f).toFloat(),
                            Screen.dp(2f).toFloat(),
                            Paints.getProgressPaint(Theme.getColor(ColorId.textNegative), Screen.dp(1.5f).toFloat())
                        )
                        cmLeft += Screen.dp(4f)
                        hAuthorChatMark!!.draw(
                            c,
                            cmLeft,
                            cmLeft + hAuthorChatMark!!.getWidth(),
                            0,
                            newTop + ((hAuthorNameT!!.getLineHeight(false) - hAuthorChatMark!!.getLineHeight(false)) / 2)
                        )
                    }
                }
                var right: Int = this.actualRightContentEdge - xBubblePadding - xBubblePaddingSmall
                if (useBubbles && needAdminSign() && hAdminNameT != null) {
                    right -= hAdminNameT!!.getWidth()
                    val y = top - Screen.dp(1.5f)
                    hAdminNameT!!.draw(c, right, top - Screen.dp(12f))
                }
                if (useBubbles && needDrawChannelIconInHeader() && hAuthorNameT != null) {
                    isChannelHeaderCounter.draw(
                        c,
                        (right - Screen.dp(6f)).toFloat(),
                        (top - Screen.dp(5f)).toFloat(),
                        Gravity.RIGHT or Gravity.BOTTOM,
                        1f,
                        view,
                        if (this.isOutgoing) ColorId.bubbleOut_time else ColorId.bubbleIn_time,
                        isChannelHeaderCounterLastDrawRect
                    )
                }
            }
        }

        if (!useBubbles) {
            // Plain mode time part

            val needMetadata = hasFlag(flags, FLAG_HEADER_ENABLED)
            val top: Int = this.headerPadding + xViewsOffset + Screen.dp(7f)

            // Time
            if (needMetadata) {
                c.drawText(time!!, pTimeLeft.toFloat(), (xTimeTop + this.headerPadding).toFloat(), mTime(true))
            }

            var clockX = pClockLeft - Icons.getClockIconWidth() - Screen.dp(Icons.CLOCK_SHIFT_X)
            var viewsX =
                pTicksLeft - Icons.getSingleTickWidth() + (if ((flags and FLAG_HEADER_ENABLED) != 0) 0 else Screen.dp(1f)) - Screen.dp(Icons.TICKS_SHIFT_X)

            if (needDrawChannelIconInHeader() && hAuthorNameT != null) {
                isChannelHeaderCounter.draw(
                    c,
                    ((if (this.isSending) clockX else viewsX) + Screen.dp(7f)).toFloat(),
                    (pTicksTop + Screen.dp(5f)).toFloat(),
                    Gravity.LEFT,
                    1f,
                    view,
                    ColorId.iconLight,
                    isChannelHeaderCounterLastDrawRect
                )
                clockX = (clockX - isChannelHeaderCounter.getScaledWidth(Screen.dp(1f))).toInt()
                viewsX = (viewsX - isChannelHeaderCounter.getScaledWidth(Screen.dp(1f))).toInt()
            }

            // Clock, tick and views
            if (this.isSending) {
                Drawables.draw(
                    c,
                    Icons.getClockIcon(ColorId.iconLight),
                    clockX.toFloat(),
                    (pClockTop - Screen.dp(Icons.CLOCK_SHIFT_Y)).toFloat(),
                    Paints.getIconLightPorterDuffPaint()
                )
            } else if (this.isFailed) {
                // TODO failure icon
            } else if (shouldShowTicks() && this.viewCountMode != VIEW_COUNT_MAIN) {
                val unread = this.isUnread && !noUnread()
                Drawables.draw(
                    c,
                    if (unread) Icons.getSingleTick(ColorId.ticks) else Icons.getDoubleTick(ColorId.ticksRead),
                    viewsX.toFloat(),
                    (pTicksTop - Screen.dp(Icons.TICKS_SHIFT_Y)).toFloat(),
                    if (unread) Paints.getTicksPaint() else Paints.getTicksReadPaint()
                )
            }

            var right =
                pTicksLeft - (if (shouldShowTicks()) Icons.getSingleTickWidth() + Screen.dp(2.5f) else 0) //needMetadata ? pTimeLeft - Screen.dp(4f) : pTicksLeft;
            if (needDrawChannelIconInHeader() && hAuthorNameT != null) {
                right = (right - isChannelHeaderCounter.getScaledWidth(Screen.dp(1f))).toInt()
            }

            // Edited
            if (shouldShowEdited()) {
                if (this.isBeingEdited) {
                    right -= Icons.getEditedIconWidth()
                    right -= Screen.dp(COUNTER_ADD_MARGIN)
                    Drawables.draw(
                        c,
                        Icons.getClockIcon(ColorId.iconLight),
                        (pTicksLeft - (if (shouldShowTicks()) Icons.getSingleTickWidth() + Screen.dp(2.5f) else 0) - Icons.getEditedIconWidth() - Screen.dp(6f)).toFloat(),
                        (pTicksTop - Screen.dp(5f)).toFloat(),
                        Paints.getIconLightPorterDuffPaint()
                    )
                } else {
                    isEdited!!.draw(c, right.toFloat(), top.toFloat(), Gravity.RIGHT, 1f, view, ColorId.iconLight, isEditedCounterLastDrawRect)
                    right = (right - isEdited.getScaledWidth(Screen.dp(COUNTER_ICON_MARGIN))).toInt()
                }
            }

            if (shouldShowMessageRestrictedWarning()) {
                if (this.isRestrictedByTelegram) {
                    isRestricted!!.draw(c, right.toFloat(), top.toFloat(), Gravity.RIGHT, 1f, view, ColorId.NONE, isRestrictedCounterLastDrawRect)
                    right = (right - isRestricted.getScaledWidth(Screen.dp(COUNTER_ICON_MARGIN))).toInt()
                } else {
                    isUnsupported!!.draw(c, right.toFloat(), top.toFloat(), Gravity.RIGHT, 1f, view, ColorId.iconLight, isRestrictedCounterLastDrawRect)
                    right = (right - isUnsupported.getScaledWidth(Screen.dp(COUNTER_ICON_MARGIN))).toInt()
                }
            }

            isPinned!!.draw(c, right.toFloat(), top.toFloat(), Gravity.RIGHT, 1f, view, this.timePartIconColorId)
            right = (right - isPinned.getScaledWidth(Screen.dp(COUNTER_ICON_MARGIN))).toInt()
            if (needMetadata) {
                right -= Screen.dp(COUNTER_ADD_MARGIN)
                if (replyCounter!!.getVisibility() > 0f) {
                    replyCounter.draw(c, right.toFloat(), top.toFloat(), Gravity.RIGHT, 1f, view, this.timePartIconColorId)
                    right = (right - replyCounter.getScaledWidth(Screen.dp(COUNTER_ICON_MARGIN + COUNTER_ADD_MARGIN))).toInt()
                }
                if (this.viewCountMode == VIEW_COUNT_MAIN) {
                    shareCounter!!.draw(c, right.toFloat(), top.toFloat(), Gravity.RIGHT, 1f, view, this.timePartIconColorId)
                    right = (right - shareCounter.getScaledWidth(Screen.dp(COUNTER_ICON_MARGIN + COUNTER_ADD_MARGIN))).toInt()
                    viewCounter!!.draw(c, right.toFloat(), top.toFloat(), Gravity.RIGHT, 1f, view, this.timePartIconColorId)
                    right = (right - viewCounter.getScaledWidth(Screen.dp(COUNTER_ICON_MARGIN + COUNTER_ADD_MARGIN))).toInt()
                }
            }

            if (translationStyleMode() == Settings.TRANSLATE_MODE_INLINE) {
                isTranslatedCounter.draw(c, right.toFloat(), top.toFloat(), Gravity.RIGHT, 1f, isTranslatedCounterLastDrawRect)
                right = (right - isTranslatedCounter.getScaledWidth(Screen.dp(COUNTER_ICON_MARGIN + COUNTER_ADD_MARGIN))).toInt()
            }
            if (reactionsDrawMode == REACTIONS_DRAW_MODE_FLAT) {
                reactionsCounter!!.draw(c, right.toFloat(), top.toFloat(), Gravity.RIGHT, 1f, view, this.timePartIconColorId)
                right = (right - reactionsCounter!!.getScaledWidth(Screen.dp(COUNTER_ICON_MARGIN))).toInt()
                right -= reactionsCounterDrawable.getMinimumWidth()
                drawReactionsWithoutBubbles(c, right, top)
                right = (right - Screen.dp(5f) * reactionsCounter!!.getVisibility()).toInt()
            }
            if (reactionsDrawMode == REACTIONS_DRAW_MODE_ONLY_ICON) {
                shrinkedReactionsCounter!!.draw(c, right.toFloat(), top.toFloat(), Gravity.RIGHT, 1f, view, ColorId.NONE)
                setLastDrawReactionsPosition(right, top)
                right = (right - (shrinkedReactionsCounter!!.getScaledWidth(Screen.dp(COUNTER_ICON_MARGIN)) + Screen.dp(COUNTER_ADD_MARGIN))).toInt()
            }
        }

        // Check box
        if (useBubbles) {
            if (selectableFactor > 0 && canBeSelected()) {
                val centerY = (findTopEdge() / 2 + findBottomEdge() / 2)
                var centerX = Screen.dp(18f) - contentOffset
                if (contentOffset != 0) {
                    centerX = (centerX - Screen.dp(28f) * (1f - selectableFactor)).toInt()
                }
                if (Lang.rtl()) {
                    centerX = viewWidth - centerX
                }

                // float darkFactor = Theme.getDarkFactor();
                val transparency = manager.controller().wallpaper()!!.getBackgroundTransparency()
                c.drawCircle(
                    centerX.toFloat(), centerY.toFloat(), (Screen.dp(9f) + (Screen.dp(1f) * (1f - transparency)).toInt()).toFloat(), Paints.strokeBigPaint(
                        alphaColor(
                            selectableFactor, fromToArgb(
                                Theme.getColor(ColorId.bubble_messageCheckOutline), Theme.getColor(ColorId.bubble_messageCheckOutlineNoWallpaper), transparency
                            )
                        )
                    )
                )
                SimplestCheckBox.draw(c, centerX, centerY, selectionFactor, null)
            }
        } else if (selectionFactor > 0f) {
            val radians = Math.toRadians(45.0)

            val x = (xAvatarCenterX + ((xAvatarRadius).toDouble() * sin(radians)).toFloat()).toInt()
            val y: Int

            if ((flags and FLAG_HEADER_ENABLED) != 0) {
                y = (xAvatarRadius + this.headerPadding + ((xAvatarRadius).toDouble() * cos(radians)).toFloat()).toInt()
            } else {
                y = this.contentY + Screen.dp(8f)
            }

            val rectF = Paints.getRectF()
            val radius = Screen.dp(11f)
            rectF.set((x - radius).toFloat(), (y - radius).toFloat(), (x + radius).toFloat(), (y + radius).toFloat())

            if ((flags and FLAG_HEADER_ENABLED) != 0) {
                if (useFullWidth()) {
                    val color = Theme.getColor(ColorId.chatBackground)
                    c.drawArc(rectF, 135f, 170f * selectionFactor, false, Paints.getOuterCheckPaint(color))
                    c.drawArc(rectF, 305f, 195f * selectionFactor, false, Paints.getOuterCheckPaint(color))
                } else {
                    c.drawArc(rectF, 135f, 170f * selectionFactor, false, Paints.getOuterCheckPaint(getSelectionColor(1f)))
                }
            } else if (useFullWidth()) {
                c.drawArc(rectF, 305f, 290f * selectionFactor, false, Paints.getOuterCheckPaint(getSelectionColor(1f)))
            }

            SimplestCheckBox.draw(c, x, y, selectionFactor, null)
        }

        // Inline keyboard
        if (inlineKeyboard != null && !inlineKeyboard!!.isEmpty()) {
            inlineKeyboard!!.draw(view, c, measureKeyboardLeft(), measureKeyboardTop())
        }

        // Reaction bubbles
        if (useReactionBubbles) {
            var top = (findBottomEdge() - messageReactions!!.getAnimatedHeight()).toInt()
            if (!useBubbles) {
                if (commentButton!!.isVisible() && commentButton.isInline()) {
                    top -= commentButton.getAnimatedHeight(-Screen.dp(2f), commentButton.getVisibility())
                }
                val left = if (centerReactions()) (view.getMeasuredWidth() / 2f - messageReactions.getAnimatedWidth() / 2f).toInt() else contentLeft
                drawReactionsWithBubbles(c, view, left, top - Screen.dp(9f))
            } else {
                if (useMediaBubbleReactions()) {
                    drawReactionsWithBubbles(c, view, bubblePathRect.left.toInt(), top - Screen.dp(6f))
                } else if (useStickerBubbleReactions()) {
                    var left: Int
                    if (centerReactions()) {
                        left = (view.getMeasuredWidth() / 2f - messageReactions.getAnimatedWidth() / 2f).toInt()
                    } else {
                        left =
                            if (this.isOutgoingBubble) (if (useBubble()) this.contentX else this.actualRightContentEdge - getContentWidth()) else (if (useBubble() && useCircleBubble()) bubblePathRect.left.toInt() else this.contentX)
                        if (this.isOutgoingBubble && messageReactions.getAnimatedWidth() > getContentWidth()) {
                            left = (this.actualRightContentEdge - messageReactions.getAnimatedWidth()).toInt()
                        }
                    }
                    if (commentButton!!.isVisible() && commentButton.isBubble()) {
                        top -= commentButton.getAnimatedHeight(Screen.dp(5f), commentButton.getVisibility())
                    }
                    drawReactionsWithBubbles(c, view, left, top - Screen.dp(6f))
                } else {
                    val x: Int = bubblePathRect.left.toInt() + xReactionBubblePadding
                    var y: Int = bottomContentEdge - messageReactions.getAnimatedHeight().toInt() - timeAddedHeight - xReactionBubblePaddingBottom
                    if (commentButton!!.isVisible() && commentButton.isInline()) {
                        y -= commentButton.getAnimatedHeight(0, commentButton.getVisibility())
                    }
                    drawReactionsWithBubbles(c, view, x, y)
                }
            }
        }

        if (commentButton!!.isVisible()) {
            if (useBubbles) {
                if (commentButton.isBubble()) {
                    val left: Int
                    if (useBubble()) {
                        left = bubblePathRect.left.toInt()
                    } else {
                        left = this.contentX
                    }
                    val bottom = findBottomEdge() - Math.round((Screen.dp(5f) * commentButton.getVisibility()))
                    val right = left + commentButton.getAnimatedWidth(0, 1f)
                    val top = bottom - commentButton.getAnimatedHeight(0, commentButton.getVisibility())
                    val inset = Math.round((right - left) * 0.2f * (1f - commentButton.getVisibility()))
                    commentButton.draw(view, c, view, left + inset, top, right - inset, bottom)
                }
            } else {
                if (commentButton.isInline()) {
                    val scale = commentButton.getVisibility()
                    val bottom = findBottomEdge() - (if (useReactionBubbles) Math.round(Screen.dp(2f) * scale) else 0)
                    val top = bottom - commentButton.getAnimatedHeight(0, scale)
                    commentButton.draw(view, c, view, 0, top, width, bottom)
                }
            }
        }

        if (useBubbles) {
            if (!needViewGroup() && useBubbleTime()) {
                drawBubbleTimePart(c, view)
            }
        }

        // forward
        if (useForward()) {
            val forwardY = this.forwardTop
            var forwardTextTop = if (useBubbles) this.contentY - bubbleForwardOffset + Screen.dp(15f) else forwardY + Screen.dp(16f)

            // forward author and time
            val isPsa = this.isPsa && !forceForwardOrImportInfo()
            val nameColor = if (isPsa) this.chatAuthorPsaColor else this.chatAuthorColor
            val forwardTextLeft = this.forwardAuthorNameLeft

            var forwardX =
                forwardTextLeft + (if (isPsa) (if (fPsaTextT != null) fPsaTextT!!.getWidth() else 0) else (if (fAuthorNameT != null) fAuthorNameT!!.getWidth() else 0)) + Screen.dp(
                    6f
                )
            val mTimePaint = if (useBubbles) Paints.colorPaint<TextPaint>(mTimeBubble(), this.decentColor) else mTime(true)
            if (fTime != null) {
                c.drawText(fTime!!, forwardX.toFloat(), forwardTextTop.toFloat(), mTimePaint)
            }
            if (this.viewCountMode == VIEW_COUNT_FORWARD) {
                forwardX = (forwardX + (Screen.dp(2f) + fTimeWidth + Screen.dp(COUNTER_ADD_MARGIN))).toInt()
                val iconTop = forwardTextTop - Screen.dp(3f)
                viewCounter!!.draw(c, forwardX.toFloat(), iconTop.toFloat(), Gravity.LEFT, 1f, view, this.decentIconColorId)
                forwardX = (forwardX + viewCounter.getScaledWidth(Screen.dp(COUNTER_ICON_MARGIN + COUNTER_ADD_MARGIN))).toInt()
                shareCounter!!.draw(c, forwardX.toFloat(), iconTop.toFloat(), Gravity.LEFT, 1f, view, this.decentIconColorId)
                forwardX = (forwardX + shareCounter.getScaledWidth(Screen.dp(COUNTER_ICON_MARGIN + COUNTER_ADD_MARGIN))).toInt()
            }
            if (isPsa && fPsaTextT != null) {
                fPsaTextT!!.draw(c, forwardTextLeft, forwardTextLeft + fPsaTextT!!.getWidth(), 0, forwardTextTop - Screen.dp(13f))
                forwardTextTop += psaTitleHeight
            }
            if (fAuthorNameT != null) {
                fAuthorNameT!!.draw(c, forwardTextLeft, forwardTextLeft + fAuthorNameT!!.getWidth(), 0, forwardTextTop - Screen.dp(13f))
            }

            val lineTop: Int
            var lineBottom: Int
            val mergeTop: Boolean
            val mergeBottom: Boolean

            if (useBubbles) {
                lineTop =
                    if (replyData != null) (topContentEdge + (if (useBubble()) xBubblePadding + xBubblePaddingSmall else Screen.dp(8f)) + (if (useBubbleName()) bubbleNameHeight else 0)) else forwardY
                lineBottom = bottomContentEdge - xBubblePadding - xBubblePaddingSmall - (if (useBubbleTime()) bubbleTimePartHeight else 0)
                if (useReactionBubbles) {
                    lineBottom =
                        (lineBottom - (messageReactions!!.getAnimatedHeight() + xReactionBubblePaddingTop * messageReactions.getVisibility()) + (bubbleTimePartHeight * (1f - messageReactions.getTimeHeightExpand()) * messageReactions.getVisibility())).toInt() // - xReactionBubblePaddingBottom * messageReactions.getVisibility());
                }
                if (commentButton.isVisible() && commentButton.isInline()) {
                    lineBottom -= commentButton.getAnimatedHeight(0, commentButton.getVisibility())
                }
                mergeTop = false
                mergeBottom = mergeTop
            } else {
                if ((flags and FLAG_MERGE_FORWARD) != 0 && (flags and FLAG_HEADER_ENABLED) == 0) {
                    lineTop = 0
                    mergeTop = true
                } else {
                    lineTop = forwardY
                    mergeTop = false
                }

                if ((flags and FLAG_MERGE_BOTTOM) != 0) {
                    lineBottom = height
                    mergeBottom = true
                } else {
                    lineBottom = forwardY + this.forwardHeight + (if (replyData != null) ReplyComponent.height() else 0)
                    mergeBottom = false
                }
            }

            val lineWidth = Screen.dp(3f)
            val lineLeft = this.forwardLeft
            val lineRight = lineLeft + lineWidth

            val rectF = Paints.getRectF()
            rectF.set(lineLeft.toFloat(), lineTop.toFloat(), lineRight.toFloat(), lineBottom.toFloat())
            val lineColor = this.forwardLineColor
            c.drawRoundRect(rectF, lineWidth / 2f, lineWidth / 2f, Paints.fillingPaint(lineColor))

            if (mergeTop) {
                c.drawRect(lineLeft.toFloat(), lineTop.toFloat(), lineRight.toFloat(), (lineTop + lineWidth).toFloat(), Paints.fillingPaint(lineColor))
            }
            if (mergeBottom) {
                c.drawRect(lineLeft.toFloat(), (lineBottom - lineWidth).toFloat(), lineRight.toFloat(), lineBottom.toFloat(), Paints.fillingPaint(lineColor))
            }
            // c.drawRect(getForwardLeft(), lineTop, getForwardLeft() + Screen.dp(3f), lineBottom, Paints.fillingPaint(LINE_COLOR));
        }

        // reply
        if (replyData != null) {
            var startX: Int
            var top: Int
            val endX: Int

            val width = replyData!!.width(useBubbles && !useBubble())

            if (useBubbles) {
                top =
                    topContentEdge + (if (useBubble()) xBubblePadding + xBubblePaddingSmall else Screen.dp(8f)) + (if (useBubbleName()) bubbleNameHeight else 0)
                if (this.isOutgoing && (!useBubble() || separateReplyFromBubble())) {
                    startX = this.actualRightContentEdge - width
                    endX = this.internalBubbleEndX
                    if (alignReplyHorizontally()) {
                        startX -= getContentWidth()
                        top = topContentEdge + (bottomContentEdge - topContentEdge) / 2 - ReplyComponent.height() / 2
                    }
                } else {
                    startX = this.internalBubbleStartX
                    endX = this.internalBubbleEndX
                    if (alignReplyHorizontally()) {
                        startX += getContentWidth()
                        top = topContentEdge + (bottomContentEdge - topContentEdge) / 2 - ReplyComponent.height() / 2
                    }
                }
            } else {
                top = this.contentY - ReplyComponent.height()
                startX = if (useFullWidth()) this.realContentX else this.contentX
                endX = viewWidth - startX
            }

            if (useBubbles && this.isForward && !forceForwardOrImportInfo()) {
                startX += xTextPadding
            }

            replyData!!.draw(c, startX, top, endX, width, replyReceiver, replyTextMediaReceiver, Lang.rtl())
        }

        if (contentOffset != 0) {
            c.restore()
        }

        if (savedTranslation) {
            c.restore()
            if (!needViewGroup()) {
                drawTranslate(view, c)
            }
        } else if (dismissFactor != 0f && !needViewGroup()) {
            drawTranslate(view, c)
        }

        startSetReactionAnimationIfReady()
        highlightUnreadReactionsIfNeeded()
        if (isHiddenFactor > 0f) {
            drawHiddenMessage(view, c, isHiddenFactor)
        }
    }

    fun drawHiddenMessage(view: MessageView, c: Canvas, isHiddenFactor: Float) {
        val viewWidth = view.getMeasuredWidth()
        val viewHeight = view.getMeasuredHeight()
        val y = this.headerPadding

        c.drawRect(0f, y.toFloat(), viewWidth.toFloat(), viewHeight.toFloat(), Paints.fillingPaint(alphaColor(isHiddenFactor, Theme.getColor(ColorId.filling))))
        // c.drawRect(0, y, viewWidth, y + 1, Paints.fillingPaint(ColorUtils.alphaColor(isHiddenFactor, Theme.getColor(ColorId.separator))));
        c.drawRect(
            0f,
            (viewHeight - 1).toFloat(),
            viewWidth.toFloat(),
            viewHeight.toFloat(),
            Paints.fillingPaint(alphaColor(isHiddenFactor, Theme.getColor(ColorId.separator)))
        )
    }

    protected fun needColoredNames(): Boolean {
        return !this.isOutgoingBubble
    }

    private val internalBubbleStartX: Int
        get() = this.actualLeftContentEdge + xBubblePadding + xBubblePaddingSmall

    private val internalBubbleEndX: Int
        get() = this.actualRightContentEdge - xBubblePadding - xBubblePaddingSmall

    fun getSelectableContentOffset(selectableFactor: Float): Int {
        return if (useBubbles() && !this.isOutgoingBubble && !headerDisabled()) (Screen.dp(28f) * selectableFactor).toInt() else 0
    }

    fun drawOverlay(view: MessageView, c: Canvas) {
        val contentOffset = getSelectableContentOffset(manager.getSelectableFactor())
        val parentViewGroup = view.getParentMessageViewGroup()
        if (parentViewGroup != null) {
            parentViewGroup.setSelectableTranslation(contentOffset.toFloat())
        }
        val savedContentTranslation = contentOffset != 0
        var globalRestoreToCount = -1
        if (savedContentTranslation) {
            globalRestoreToCount = Views.save(c)
            c.translate(contentOffset.toFloat(), 0f)
        }
        val savedTranslation = translation != 0f
        var restoreToCount = -1
        if (savedTranslation) {
            restoreToCount = Views.save(c)
            c.translate(translation, 0f)
        }
        drawOverlay(view, c, this.contentX, this.contentY, this.contentMaxWidth)
        if (savedTranslation) {
            Views.restore(c, restoreToCount)
            drawTranslate(view, c)
        } else if (dismissFactor != 0f) {
            drawTranslate(view, c)
        }
        if (useBubbles()) {
            if (useBubbleTime()) {
                drawBubbleTimePart(c, view)
            }
        }
        if (savedContentTranslation) {
            Views.restore(c, globalRestoreToCount)
        }
    }

    protected open fun drawOverlay(view: MessageView?, c: Canvas?, startX: Int, startY: Int, maxWidth: Int) {
        // Override
    }

    protected open fun drawContent(view: MessageView?, c: Canvas, startX: Int, startY: Int, maxWidth: Int) {
        // These two dots should never appear in MessagesListView

        c.drawCircle(startX + 5f, startY + 5f, 5f, Paints.fillingPaint(Theme.radioFillingColor()))
        c.drawCircle(startX + maxWidth - 5f, startY + 5f, 5f, Paints.fillingPaint(Theme.radioFillingColor()))
    }

    protected open fun drawContent(view: MessageView?, c: Canvas, startX: Int, startY: Int, maxWidth: Int, receiver: ComplexReceiver?) {
        // These two dots should never appear in MessagesListView

        c.drawCircle(startX + 5f, startY + 5f, 5f, Paints.fillingPaint(Theme.radioFillingColor()))
        c.drawCircle(startX + maxWidth - 5f, startY + 5f, 5f, Paints.fillingPaint(Theme.radioFillingColor()))
    }

    protected open fun drawContent(view: MessageView?, c: Canvas, startX: Int, startY: Int, maxWidth: Int, preview: Receiver?, receiver: Receiver?) {
        // These two dots should never appear in MessagesListView

        c.drawCircle(startX + 5f, startY + 5f, 5f, Paints.fillingPaint(Theme.radioFillingColor()))
        c.drawCircle(startX + maxWidth - 5f, startY + 5f, 5f, Paints.fillingPaint(Theme.radioFillingColor()))
    }

    open fun needImageReceiver(): Boolean {
        return false
    }

    open fun needViewGroup(): Boolean {
        return false
    }

    val childrenWidth: Int
        get() = getContentWidth()

    val childrenHeight: Int
        get() = this.contentHeight

    open fun needGifReceiver(): Boolean {
        return false
    }

    open fun needComplexReceiver(): Boolean {
        return false
    }

    // Touch
    private fun setViewAttached(isAttached: Boolean) {
        val nowIsAttached = (flags and FLAG_ATTACHED) != 0
        if (isAttached != nowIsAttached) {
            flags = setFlag(flags, FLAG_ATTACHED, isAttached)
            onMessageAttachStateChange(isAttached)
            if (isAttached) {
                manager.viewMessages(false)
            }
        }
    }

    val isAttachedToView: Boolean
        get() = hasFlag(flags, FLAG_ATTACHED)

    protected open fun onMessageAttachStateChange(isAttached: Boolean) {
        // override
    }

    open fun handleUiMessage(what: Int, arg1: Int, arg2: Int) {
        // override
    }

    private fun hasAttachedToAnything(): Boolean {
        return currentViews.hasAnyTargetToInvalidate() || (overlayViews != null && overlayViews.hasAnyTargetToInvalidate())
    }

    fun onAttachedToView(view: MessageView?) {
        if (hAuthorEmojiStatus != null) {
            hAuthorEmojiStatus!!.onAppear()
        }

        setViewAttached(view != null || hasAttachedToAnything())
        if (currentViews.attachToView(view) && view != null) {
            onMessageAttachedToView(view, true)
        }
    }

    fun onDetachedFromView(view: MessageView?) {
        if (currentViews.detachFromView(view) && view != null) {
            onMessageAttachedToView(view, false)
        }
        setViewAttached(hasAttachedToAnything())
    }

    protected open fun onMessageAttachedToView(view: MessageView, attached: Boolean) {}

    fun onAttachedToOverlayView(view: View?) {
        setViewAttached(view != null || hasAttachedToAnything())
        if (view != null && overlayViews != null) {
            if (overlayViews.attachToView(view)) {
                onMessageAttachedToOverlayView(view, true)
            }
        }
    }

    fun onDetachedFromOverlayView(view: View?) {
        if (overlayViews != null && overlayViews.detachFromView(view) && view != null) {
            onMessageAttachedToOverlayView(view, false)
        }
        setViewAttached(hasAttachedToAnything())
    }

    protected open fun onMessageAttachedToOverlayView(view: View, attached: Boolean) {}

    // Invalidate attached views
    fun requestLayout() {
        currentViews.requestLayout()
    }

    fun hasAnyTargetToInvalidate(): Boolean {
        return currentViews.hasAnyTargetToInvalidate()
    }

    /**
     * Synonym to [.hasAnyTargetToInvalidate]
     * @return false, when layout must be updated immediately
     */
    fun needAnimateChanges(): Boolean {
        return hasAnyTargetToInvalidate() && controller()!!.getParentOrSelf().isAttachedToNavigationController() && hasFlag(
            flags,
            FLAG_LAYOUT_BUILT
        ) && UI.inUiThread()
    }

    val isLayoutBuilt: Boolean
        get() = hasFlag(flags, FLAG_LAYOUT_BUILT)

    fun invalidate() {
        currentViews.invalidate()
    }

    fun invalidateParentOrSelf(invalidateOverlay: Boolean) {
        invalidate()
        if (needViewGroup()) {
            invalidateParent()
        }
        if (invalidateOverlay) {
            invalidateOverlay()
        }
    }

    fun invalidateParentOrSelf(left: Int, top: Int, right: Int, bottom: Int, invalidateOverlay: Boolean) {
        invalidate(left, top, right, bottom)
        if (needViewGroup()) {
            invalidateParent(left, top, right, bottom)
        }
        if (invalidateOverlay) {
            invalidateOverlay()
        }
    }

    fun invalidateParent() {
        currentViews.invalidateParent()
    }

    fun invalidateParent(left: Int, top: Int, right: Int, bottom: Int) {
        currentViews.invalidateParent(left, top, right, bottom)
    }

    fun invalidate(withOverlay: Boolean) {
        currentViews.invalidate()
        if (withOverlay && overlayViews != null) {
            overlayViews.invalidate()
        }
    }

    fun invalidateOverlay() {
        if (overlayViews != null) {
            overlayViews.invalidate()
        }
    }

    fun invalidateOverlay(left: Int, top: Int, right: Int, bottom: Int) {
        if (overlayViews != null) {
            overlayViews.invalidate(left, top, right, bottom)
        }
    }

    /*public final void invalidateOutline (boolean withInvalidate) {
    currentViews.invalidateOutline(withInvalidate);
  }*/
    fun invalidate(left: Int, top: Int, right: Int, bottom: Int) {
        currentViews.invalidate(left, top, right, bottom)
    }

    fun postInvalidate() {
        currentViews.postInvalidate()
    }

    fun postInvalidate(withOverlay: Boolean) {
        currentViews.postInvalidate()
        if (withOverlay && overlayViews != null) {
            overlayViews.postInvalidate()
        }
    }

    fun performClickSoundFeedback() {
        currentViews.performClickSoundFeedback()
    }

    fun performShakeAnimation(isPositive: Boolean) {
        performWithViews(RunnableData { view: MessageView? -> view!!.shake(isPositive) })
    }

    fun performConfettiAnimation(pivotX: Int, pivotY: Int) {
        performWithViews(RunnableData { view: MessageView? -> context().performConfetti(view, pivotX, pivotY) })
    }

    private fun performWithViews(act: RunnableData<MessageView?>) {
        for (view in currentViews) {
            act.runWithData(view as MessageView?)
        }
    }

    fun findCurrentView(): View? {
        return currentViews.findAnyTarget()
    }

    override fun invalidateContent(cause: Any?): Boolean {
        if (cause === replyData) {
            invalidateReplyReceiver()
        } else {
            invalidateContentReceiver()
        }
        return true
    }

    fun invalidateContentReceiver(messageId: Long, arg: Int) {
        performWithViews(RunnableData { view: MessageView? -> view!!.invalidateContentReceiver(msg!!.chatId, messageId, arg) })
    }

    fun invalidatePreviewReceiver() {
        performWithViews(RunnableData { view: MessageView? -> view!!.invalidatePreviewReceiver(msg!!.chatId, msg!!.id) })
    }

    fun invalidateContentReceiver() {
        performWithViews(RunnableData { view: MessageView? -> view!!.invalidateContentReceiver(msg!!.chatId, msg!!.id, -1) })
    }

    fun invalidateContentReceiver(arg: Int) {
        performWithViews(RunnableData { view: MessageView? -> view!!.invalidateContentReceiver(msg!!.chatId, msg!!.id, arg) })
    }

    fun invalidateReplyReceiver() {
        performWithViews(RunnableData { view: MessageView? -> view!!.invalidateReplyReceiver(msg!!.chatId, msg!!.id) })
    }

    fun invalidateTextMediaReceiver() {
        performWithViews(RunnableData { view: MessageView? -> requestTextMedia(view!!.getTextMediaReceiver()) })
    }

    fun invalidateEmojiStatusReceiver() {
        performWithViews(RunnableData { view: MessageView? -> requestAuthorTextMedia(view!!.getEmojiStatusReceiver()) })
    }

    fun invalidateAvatarsReceiver() {
        performWithViews(RunnableData { view: MessageView? -> requestCommentsResources(view!!.getAvatarsReceiver(), true) })
    }

    fun invalidateGiveawayReceiver() {
        performWithViews(RunnableData { view: MessageView? -> requestGiveawayAvatars(view!!.getGiveawayAvatarsReceiver(), true) })
    }

    fun invalidateReactionFilesReceiver() {
        performWithViews(RunnableData { view: MessageView? -> requestReactions(view!!.getReactionsComplexReceiver()) })
    }

    fun invalidateTextMediaReceiver(text: Text, textMedia: TextMedia?) {
        performWithViews(RunnableData { view: MessageView? -> view!!.invalidateTextMediaReceiver(this, text, textMedia) })
    }

    fun invalidateReplyTextMediaReceiver(text: Text, textMedia: TextMedia?) {
        performWithViews(RunnableData { view: MessageView? -> view!!.invalidateReplyTextMediaReceiver(this, text, textMedia) })
    }

    // Touch
    open fun allowLongPress(x: Float, y: Float): Boolean {
        if (messagesController().inSelectMode()) {
            return true
        }
        if (commentButton!!.isVisible() && commentButton.contains(x, y)) {
            // long press is handled in commentButton
            return false
        }
        return true
    }

    @CallSuper
    open fun performLongPress(view: View?, x: Float, y: Float): Boolean {
        var result = false
        if (inlineKeyboard != null) {
            result = inlineKeyboard!!.performLongPress(view)
        }
        if (messageReactions!!.getTotalCount() > 0 && useReactionBubbles()) {
            result = messageReactions.performLongPress(view) || result
        }
        if (hasFooter()) {
            result = footerText!!.performLongPress(view) || result
        }
        clickHelper.cancel(view, x, y)
        if (hAuthorNameT != null) {
            hAuthorNameT!!.cancelTouch()
        }
        if (fAuthorNameT != null) {
            fAuthorNameT!!.cancelTouch()
        }
        return result
    }

    fun shouldIgnoreTap(e: MotionEvent): Boolean {
        return e.getY() < findTopEdge()
    }

    private fun getClickType(view: MessageView, x: Float, y: Float): Int {
        if (this.isTranslated) {
            if (checkClickOnRect(isTranslatedCounterLastDrawRect, x, y, Screen.dp(4f).toFloat())) {
                return CLICK_TYPE_TRANSLATE_MESSAGE_ICON
            }
        }

        if (needDrawChannelIconInHeader() && hAuthorNameT != null) {
            if (checkClickOnRect(isChannelHeaderCounterLastDrawRect, x, y, Screen.dp(4f).toFloat())) {
                return CLICK_TYPE_CHANNEL_MESSAGE_ICON
            }
        }

        if (shouldShowMessageRestrictedWarning()) {
            if (checkClickOnRect(isRestrictedCounterLastDrawRect, x, y, Screen.dp(4f).toFloat())) {
                return CLICK_TYPE_MESSAGE_RESTRICTED_ICON
            }
        }

        if (shouldShowEdited()) {
            if (checkClickOnRect(isEditedCounterLastDrawRect, x, y, Screen.dp(4f).toFloat())) {
                return CLICK_TYPE_MESSAGE_EDITED_ICON
            }
        }

        if (replyData != null && replyData!!.isInside(x, y, useBubbles() && !useBubble())) {
            return CLICK_TYPE_REPLY
        }
        if (hasHeader() && needAvatar() && view.getAvatarReceiver().isInsideReceiver(x, y)) {
            return CLICK_TYPE_AVATAR
        }
        return CLICK_TYPE_NONE
    }

    private val clickHelper = ClickHelper(object : ClickHelper.Delegate {
        override fun needClickAt(view: View, x: Float, y: Float): Boolean {
            clickType = getClickType(view as MessageView, x, y)
            return clickType != CLICK_TYPE_NONE
        }

        override fun onClickAt(view: View, x: Float, y: Float) {
            when (clickType) {
                CLICK_TYPE_TRANSLATE_MESSAGE_ICON -> {
                    openLanguageSelectorInlineMode()
                }

                CLICK_TYPE_CHANNEL_MESSAGE_ICON -> {
                    openMessageFromChannel()
                }

                CLICK_TYPE_MESSAGE_RESTRICTED_ICON -> {
                    showMessageTooltip(TooltipOverlayView.LocationProvider { targetView: View?, outRect: Rect? ->
                        isRestrictedCounterLastDrawRect.round(outRect!!)
                        outRect.top -= Screen.dp(6f)
                    }, Lang.getString(if (this@TGMessage.isRestrictedByTelegram) R.string.MessageRestrictedByTelegram else R.string.MessageUnsupportedHint), 2500)
                }

                CLICK_TYPE_MESSAGE_EDITED_ICON -> {
                    showMessageTooltip(
                        TooltipOverlayView.LocationProvider { targetView: View?, outRect: Rect? ->
                            isEditedCounterLastDrawRect.round(outRect!!)
                            outRect.top -= Screen.dp(6f)
                        },
                        Lang.getRelativeDate(
                            this@TGMessage.editDate.toLong(), TimeUnit.SECONDS,
                            tdlib.currentTimeMillis(), TimeUnit.MILLISECONDS,
                            true, 60, R.string.message_edited, false
                        ),
                        2500
                    )
                }

                CLICK_TYPE_REPLY -> {
                    if (msg.replyTo != null && msg.replyTo!!.getConstructor() == MessageReplyToMessage.CONSTRUCTOR) {
                        val replyToMessage = msg.replyTo as MessageReplyToMessage
                        if (replyData != null && replyData!!.getError() != null) {
                            buildContentHint(view, this@TGMessage.replyLocationProvider, false)!!.show(tdlib, replyData!!.toErrorText())
                        } else {
                            if (replyToMessage.chatId != msg.chatId) {
                                if (replyToMessage.chatId == 0L || replyToMessage.messageId == 0L) {
                                    buildContentHint(view, this@TGMessage.replyLocationProvider, false)!!.show(tdlib, Lang.getString(R.string.MessageReplyPrivate))
                                } else {
                                    tdlib.ui().openMessage(controller(), replyToMessage.chatId, MessageId(replyToMessage), openParameters())
                                }
                            } else if (this@TGMessage.isScheduled) {
                                tdlib.ui().openMessage(controller(), replyToMessage.chatId, MessageId(replyToMessage), openParameters())
                            } else {
                                highlightOtherMessage(MessageId(replyToMessage))
                            }
                        }
                    }
                }

                CLICK_TYPE_AVATAR -> {
                    onAvatarClick(view)
                }
            }
            clickType = CLICK_TYPE_NONE
        }
    })

    protected fun highlightOtherMessage(messageId: MessageId?) {
        manager.controller().highlightMessage(messageId, toMessageId())
    }

    protected fun highlightOtherMessage(otherMessageId: Long) {
        highlightOtherMessage(MessageId(msg!!.chatId, otherMessageId))
    }

    private var mInitialTouchX = 0f
    private var mInitialTouchY = 0f

    open fun onTouchEvent(view: MessageView, e: MotionEvent): Boolean {
        /*if ((flags & FLAG_HEADER_ENABLED) == 0 && (msg.forwardInfo == null || forwardInfo == null) && replyData == null && (inlineKeyboard == null || inlineKeyboard.isEmpty())) {
      return false;
    }*/
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            mInitialTouchX = e.getX()
            mInitialTouchY = e.getY()
        }

        if (hasHeader() && needName(true)) {
            if (hAuthorNameT != null && hAuthorNameT!!.onTouchEvent(view, e)) return true
            if (hPsaTextT != null && hPsaTextT!!.onTouchEvent(view, e)) return true
        }
        if (useForward()) {
            if (fPsaTextT != null && fPsaTextT!!.onTouchEvent(view, e)) return true
            if (fAuthorNameT != null && fAuthorNameT!!.onTouchEvent(view, e)) return true
        }
        if (hasFooter()) {
            if (footerText!!.onTouchEvent(view, e)) return true
        }
        if (!this.isEventLog && inlineKeyboard != null && !inlineKeyboard!!.isEmpty() && inlineKeyboard!!.onTouchEvent(view, e)) {
            return true
        }
        if (useReactionBubbles()) {
            if (messageReactions!!.onTouchEvent(view, e)) {
                return true
            }
        }
        if (commentButton!!.onTouchEvent(view, e)) {
            return true
        }
        return clickHelper.onTouchEvent(view, e)
    }

    private var clickType: Int = CLICK_TYPE_NONE

    // Header
    fun updateDate() {
        setDate(genDate())
        if (useBubbles() || (flags and FLAG_HEADER_ENABLED) != 0) {
            layoutInfo()
        }
    }

    private fun setDate(date: String?) {
        this.drawDateText = date
        val needFakeBold = Text.needFakeBold(date)
        this.flags = setFlag(flags, FLAG_DATE_FAKE_BOLD, needFakeBold)
        // TextPaint paint = useBubbles() ? Paints.getDatePaint() : Paints.getTitlePaint(false);
        val paint = if (useBubbles()) Paints.getBoldPaint13(needFakeBold) else Paints.getTitlePaint(needFakeBold)
        this.dateWidth = if (isEmpty(date)) 0 else U.measureText(date, paint).toInt()
    }

    protected fun alignReplyHorizontally(): Boolean {
        return false
    }

    private fun buildHeader() {
        val currentWidth = this.width

        if ((flags and FLAG_HEADER_ENABLED) != 0 || useBubbles()) {
            layoutAvatar()
            layoutInfo()
        }

        if ((flags and FLAG_SHOW_BADGE) != 0) {
            val center = currentWidth.toFloat() / 2f
            val badgeWidth = if (uBadge != null) U.measureText(uBadge!!.text, getBadgePaint(uBadge!!.needFakeBold)) else 0f

            if (this.isFirstUnread) {
                // badge has icon
                pBadgeX = (center - (badgeWidth + Screen.dp(7f) + iBadge!!.getMinimumWidth()) / 2f).toInt()
            } else {
                // badge has no icon
                pBadgeX = (center - badgeWidth / 2f).toInt()
            }
            pBadgeIconX = pBadgeX + badgeWidth.toInt() + Screen.dp(2f)
        }

        pClockTop = this.headerPadding + Screen.dp(3.5f)
        pTicksTop = this.headerPadding + Screen.dp(3f)

        if ((flags and FLAG_HEADER_ENABLED) != 0) {
            pClockLeft = pTimeLeft - Screen.dp(6f)
            pTicksLeft = pTimeLeft - Screen.dp(3f)
        } else {
            pClockLeft = currentWidth - Screen.dp(17f)
            pTicksLeft = currentWidth - Screen.dp(15f)

            if (replyData != null && !alignReplyHorizontally()) {
                pClockTop += ReplyComponent.height()
                pTicksTop += ReplyComponent.height()
            }
        }
    }

    fun findReplyMarkupMessage(): TdApi.Message? {
        synchronized(this) {
            if (this.combinedMessagesUnsafely != null && !combinedMessagesUnsafely!!.isEmpty()) {
                var markup: TdApi.Message? = null
                for (msg in this.combinedMessagesUnsafely) {
                    if (msg.replyMarkup != null) {
                        if (markup != null) {
                            return null
                        }
                        markup = msg
                    }
                }
                return markup
            }
        }
        return msg
    }

    protected open fun buildReactions(animated: Boolean) {
        if (useReactionBubbles()) {
            if (useBubbles()) {
                if (useMediaBubbleReactions()) {
                    messageReactions!!.measureReactionBubbles(computeBubbleWidth() + this.bubblePaddingLeft + this.bubblePaddingRight)
                } else if (useStickerBubbleReactions()) {
                    messageReactions!!.measureReactionBubbles(max(getContentWidth(), (estimatedContentMaxWidth * 0.85f).toInt()))
                } else {
                    messageReactions!!.measureReactionBubbles(
                        (computeBubbleWidth() + this.bubblePaddingLeft + this.bubblePaddingRight - xReactionBubblePadding * 2),
                        computeBubbleTimePartWidth(true, true)
                    )
                }
            } else {
                messageReactions!!.measureReactionBubbles(estimatedContentMaxWidth, 0)
            }
        }
        messageReactions!!.resetReactionsAnimator(animated)
    }

    private fun buildMarkup() {
        val replyMarkupMessage = findReplyMarkupMessage()
        if (replyMarkupMessage != null && replyMarkupMessage.replyMarkup != null && replyMarkupMessage.replyMarkup!!.getConstructor() == ReplyMarkupInlineKeyboard.CONSTRUCTOR) {
            if (inlineKeyboard == null) {
                inlineKeyboard = TGInlineKeyboard(this, true)
                inlineKeyboard!!.setViewProvider(currentViews)
            }
            if (useBubbles() && useBubble()) {
                val width = this.actualRightContentEdge - this.actualLeftContentEdge
                val maxWidth = this.realContentMaxWidth + this.bubblePaddingLeft + this.bubblePaddingRight
                inlineKeyboard!!.set(replyMarkupMessage.id, (replyMarkupMessage.replyMarkup as ReplyMarkupInlineKeyboard?)!!, width, maxWidth)
            } else {
                inlineKeyboard!!.set(
                    replyMarkupMessage.id, (replyMarkupMessage.replyMarkup as ReplyMarkupInlineKeyboard?)!!,
                    getContentWidth(),
                    this.realContentMaxWidth
                )
            }
        } else if (inlineKeyboard != null) {
            inlineKeyboard!!.clear()
        }
    }

    val forwardOrImportDate: Int
        get() = if (msg!!.forwardInfo != null && msg!!.forwardInfo!!.date != 0) msg!!.forwardInfo!!.date else if (msg!!.importInfo != null && msg!!.importInfo!!.date != 0) msg!!.importInfo!!.date else 0

    fun forceForwardOrImportInfo(): Boolean {
        if (msg!!.importInfo != null) {
            return true
        }
        val forwardInfo = msg!!.forwardInfo
        return forwardInfo != null && !this.isOutgoing && (hasFlag(flags, FLAG_SELF_CHAT) ||
                (this.isChannelAutoForward && forwardInfo.origin.getConstructor() == MessageOriginChannel.CONSTRUCTOR && forwardInfo.source != null && forwardInfo.source!!.chatId == (forwardInfo.origin as MessageOriginChannel).chatId) ||
                (this.isPsa && !sender.isUser() && useBubbles()) ||
                this.isRepliesChat)
    }

    val isImported: Boolean
        get() = msg!!.importInfo != null

    val isChannelAutoForward: Boolean
        get() = tdlib.isChannelAutoForward(msg)

    val isRepliesChat: Boolean
        get() = tdlib.isRepliesChat(msg!!.chatId)

    val isMessageThread: Boolean
        get() = messagesController().messageThread != null

    private var hasAvatar = false

    private fun layoutAvatar() {
        if (useBubbles() && !needAvatar()) {
            hasAvatar = false
            return
        }
        if (this.chat == null) {
            this.chat = tdlib.chat(msg!!.chatId)
        }
        hasAvatar = true
        // FIXME: better logic behind this method
    }

    private fun onAvatarClick(view: View): Boolean {
        return openProfile(view, null, null, null, (view as MessageView).getAvatarReceiver())
    }

    private fun onNameClick(view: View?, text: Text?, part: TextPart, openParameters: UrlOpenParameters?): Boolean {
        if (part.getEntity() != null && part.getEntity()!!.getTag() is Long) {
            manager.controller().setInputInlineBot(msg!!.viaBotUserId, viaBotUsername)
            return true
        } else if (needDrawChannelIconInHeader()) {
            return openMessageFromChannel()
        } else {
            return openProfile(view, text, part, openParameters, null)
        }
    }

    private fun openProfile(view: View?, text: Text?, part: TextPart?, openParameters: UrlOpenParameters?, receiver: Receiver?): Boolean {
        if (forceForwardOrImportInfo()) {
            forwardInfo!!.open(view, text, part, openParameters, receiver)
        } else if (isSponsoredMessage()) {
            openSponsoredMessage()
        } else if (sender.isUser()) {
            tdlib.ui().openPrivateProfile(controller(), sender.getUserId(), openParameters)
        } else if (sender.isChat()) {
            tdlib.ui().openChatProfile(controller(), sender.getChatId(), null, openParameters)
        } else {
            return false
        }
        return true
    }

    private fun onForwardClick(view: View?, text: Text, part: TextPart, openParameters: UrlOpenParameters?): Boolean {
        if (part.getEntity() == null && text.getEntityCount() == 1) return false
        if (part.getEntity() != null && part.getEntity()!!.getTag() is Long) {
            manager.controller().setInputInlineBot(msg!!.viaBotUserId, viaBotUsername)
        } else {
            forwardInfo!!.open(view, text, part, openParameters, null)
        }
        return true
    }

    fun openParameters(): UrlOpenParameters? {
        return UrlOpenParameters().sourceMessage(this)
    }

    private fun makeChatMark(maxWidth: Int): Text? {
        return Text.Builder(
            Lang.getString(if (sender.isFake()) R.string.FakeMark else R.string.ScamMark),
            maxWidth,
            Paints.robotoStyleProvider(10f),
            TextColorSets.Regular.NEGATIVE
        )
            .singleLine()
            .allBold()
            .clipTextArea()
            .build()
    }

    private fun makeName(
        authorName: String,
        accentColor: TdlibAccentColor?,
        available: Boolean,
        isPsa: Boolean,
        hideName: Boolean,
        viaBotUserId: Long,
        maxWidth: Int,
        isForward: Boolean
    ): Text? {
        var maxWidth = maxWidth
        if (maxWidth <= 0) return null
        val hasBot = viaBotUserId != 0L
        val viaBot = if (viaBotUserId != 0L) tdlib.cache().user(viaBotUserId) else null
        var viaBotUsername: String? = ""
        if (hasBot) {
            if (viaBot.hasUsername()) {
                viaBotUsername = "@" + viaBot.primaryUsername()
            } else if (TD.isUserDeleted(viaBot)) {
                viaBotUsername = Lang.getString(R.string.DeactivatedBot)
            } else {
                viaBotUsername = TD.getUserName(viaBot)
            }
        }
        val textRes =
            if (isPsa) (if (hasBot) R.string.PsaFromXViaBot else R.string.PsaFromX) else (if (hasBot) if (hideName) R.string.message_viaBot else R.string.message_nameViaBot else 0)
        val text: CharSequence
        var allActive = textRes == R.string.PsaFromXViaBot || textRes == R.string.PsaFromX
        var allBold = false
        if (textRes == 0) {
            if (hideName) {
                return null
            }
            val b = SpannableStringBuilder(authorName)
            b.setSpan(
                TextEntityCustom(controller(), tdlib, authorName, 0, authorName.length, TextEntityCustom.FLAG_CLICKABLE, openParameters()),
                0,
                b.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            text = b
            allActive = true
            allBold = available
        } else if (textRes == R.string.PsaFromXViaBot || textRes == R.string.message_nameViaBot) { // author via bot
            text = Lang.getString(
                textRes,
                SpanCreator { target: CharSequence?, argStart: Int, argEnd: Int, argIndex: Int, needFakeBold: Boolean ->
                    TextEntityCustom(
                        controller(),
                        tdlib,
                        target.toString(),
                        argStart,
                        argEnd,
                        TextEntityCustom.FLAG_CLICKABLE or (if (argIndex == 1 || available) TextEntityCustom.FLAG_BOLD else 0),
                        openParameters()
                    ).setTag(if (argIndex == 1) viaBotUserId else null)
                },
                authorName,
                viaBotUsername
            )
        } else if (textRes == R.string.PsaFromX) { // author
            text = Lang.getString(
                textRes,
                SpanCreator { target: CharSequence?, argStart: Int, argEnd: Int, argIndex: Int, needFakeBold: Boolean -> Lang.newBoldSpan(needFakeBold) },
                authorName
            )
            allActive = true
        } else { // via bot
            text = Lang.getString(
                textRes,
                SpanCreator { target: CharSequence?, argStart: Int, argEnd: Int, argIndex: Int, needFakeBold: Boolean ->
                    TextEntityCustom(
                        controller(),
                        tdlib,
                        target.toString(),
                        argStart,
                        argEnd,
                        TextEntityCustom.FLAG_CLICKABLE or TextEntityCustom.FLAG_BOLD,
                        openParameters()
                    ).setTag(viaBotUserId)
                },
                viaBotUsername
            )
        }
        val colorTheme: TextColorSet
        if (isPsa) {
            colorTheme = this.chatAuthorPsaColorSet
        } else if ((APPLY_ACCENT_TO_FORWARDS || !isForward) && needColoredNames() && accentColor != null) {
            colorTheme = object : TextColorSetOverride(this.chatAuthorColorSet) {
                override fun clickableTextColor(isPressed: Boolean): Int {
                    return accentColor.getNameColor()
                }

                override fun mediaTextComplexColor(): Long {
                    return accentColor.getNameComplexColor()
                }

                override fun backgroundColor(isPressed: Boolean): Int {
                    return if (isPressed) alphaColor(.2f, accentColor.getNameColor()) else 0
                }

                override fun backgroundColorId(isPressed: Boolean): Int {
                    return if (isPressed) Theme.extractColorValue(accentColor.getNameComplexColor()) else 0
                }
            }
        } else {
            colorTheme = this.chatAuthorColorSet
        }

        if (!(tdlib.isSelfChat(chat) && forwardInfo != null) && !hasBot && !isForward && sender.isUser()) {
            hAuthorEmojiStatus = EmojiStatusHelper.makeDrawable(
                null,
                tdlib,
                tdlib.cache().user(sender.getUserId()),
                colorTheme,
                Text.TextMediaListener { text1: Text?, specificMedia: TextMedia? -> invalidateEmojiStatusReceiver() })
            hAuthorEmojiStatus!!.invalidateTextMedia()
            maxWidth -= hAuthorEmojiStatus!!.getWidth(Screen.dp(3f))
        }

        return Text.Builder(tdlib, text, openParameters(), maxWidth, nameStyleProvider, colorTheme, null)
            .singleLine()
            .clipTextArea()
            .allBold(allBold)
            .allClickable(allActive)
            .viewProvider(currentViews)
            .onClick(if (isForward) Text.ClickListener { view: View?, text: Text?, part: TextPart?, openParameters: UrlOpenParameters? ->
                this.onForwardClick(
                    view,
                    text!!,
                    part!!,
                    openParameters
                )
            } else Text.ClickListener { view: View?, text: Text?, part: TextPart?, openParameters: UrlOpenParameters? ->
                this.onNameClick(
                    view,
                    text,
                    part!!,
                    openParameters
                )
            })
            .build()
    }

    private fun layoutInfo() {
        val reactionsDrawMode = this.reactionsDrawMode
        val isPsa = this.isPsa && forceForwardOrImportInfo()

        if (useBubbles()) {
            // time part
            pTimeWidth = U.measureText(time, mTimeBubble()).toInt()

            // header part
            var maxWidth: Int =
                (if (allowMessageHorizontalExtend()) this.realContentMaxWidth else getContentWidth()) + this.bubblePaddingRight + this.bubblePaddingRight - (xBubblePadding + xBubblePaddingSmall) * 2

            if (needAdminSign()) {
                hAdminNameT = Text.Builder(
                    this.administratorSign, maxWidth,
                    timeTextStyleProvider!!, this.decentColorSet
                ).singleLine().build()
                maxWidth -= hAdminNameT!!.getWidth()
            } else {
                hAdminNameT = null
            }

            val authorName = this.displayAuthor
            if (needName(true) && maxWidth > 0) {
                if (!forceForwardOrImportInfo() && sender.hasChatMark()) {
                    hAuthorChatMark = makeChatMark(maxWidth)
                    maxWidth -= hAuthorChatMark!!.getWidth()
                }
                isChannelHeaderCounter.showHide(needDrawChannelIconInHeader(), false)
                if (needDrawChannelIconInHeader()) {
                    maxWidth = (maxWidth - isChannelHeaderCounter.getScaledWidth(Screen.dp(5f))).toInt()
                }
                hAuthorAccentColor = if (forceForwardOrImportInfo()) forwardInfo!!.getAuthorAccentColor() else sender.getAccentColor()
                hAuthorNameT = makeName(
                    authorName,
                    hAuthorAccentColor,
                    !(forceForwardOrImportInfo() && forwardInfo is TGSourceHidden),
                    isPsa,
                    !needName(false),
                    if (msg!!.forwardInfo == null || forceForwardOrImportInfo()) msg!!.viaBotUserId else 0,
                    maxWidth,
                    false
                )
            } else {
                hAuthorAccentColor = null
                hAuthorNameT = null
                hAuthorChatMark = null
                isChannelHeaderCounter.showHide(false, false)
            }
            if (isPsa) {
                val text = Lang.getPsaNotificationType(controller(), msg!!.forwardInfo!!.publicServiceAnnouncementType)
                hPsaTextT = Text.Builder(
                    tdlib, text, openParameters(), maxWidth,
                    nameStyleProvider, this.chatAuthorPsaColorSet, null
                )
                    .allClickable()
                    .singleLine()
                    .viewProvider(currentViews)
                    .onClick(Text.ClickListener { view: View?, text: Text?, part: TextPart?, openParameters: UrlOpenParameters? ->
                        this.onNameClick(
                            view,
                            text,
                            part!!,
                            openParameters
                        )
                    })
                    .build()
            } else {
                hPsaTextT = null
            }

            if ((flags and FLAG_LAYOUT_BUILT) != 0) {
                buildBubble(false)
            }

            return
        }

        val currentWidth = this.width

        pTimeWidth = U.measureText(time, mTime(false)).toInt()
        val timePaddingRight = Screen.dp(16f)
        pTimeLeft = currentWidth - pTimeWidth - timePaddingRight

        val totalMaxWidth: Int = currentWidth - contentLeft - Screen.dp(4f)
        var max = totalMaxWidth - timePaddingRight - pTimeWidth

        if (shouldShowTicks()) {
            max -= Screen.dp(3f) + Icons.getSingleTickWidth() + Screen.dp(3f)
        }

        if (shouldShowEdited()) {
            if (this.isBeingEdited) {
                max -= Screen.dp(6f) + Icons.getEditedIconWidth()
            } else {
                max = (max - (isEdited!!.getScaledWidth(Screen.dp(COUNTER_ICON_MARGIN)) + Screen.dp(COUNTER_ADD_MARGIN))).toInt()
            }
        }

        val authorName: String
        if (forceForwardOrImportInfo()) {
            authorName = forwardInfo!!.getAuthorName()
        } else {
            authorName = sender.getName()
        }

        max = (max - (isPinned!!.getScaledWidth(Screen.dp(COUNTER_ICON_MARGIN)) + Screen.dp(COUNTER_ADD_MARGIN))).toInt()
        if (shouldShowMessageRestrictedWarning()) {
            if (this.isRestrictedByTelegram) {
                max = (max - (isRestricted!!.getScaledWidth(Screen.dp(COUNTER_ICON_MARGIN)) + Screen.dp(COUNTER_ADD_MARGIN))).toInt()
            } else {
                max = (max - (isUnsupported!!.getScaledWidth(Screen.dp(COUNTER_ICON_MARGIN)) + Screen.dp(COUNTER_ADD_MARGIN))).toInt()
            }
        }
        if (replyCounter!!.getVisibility() > 0f) {
            max = (max - replyCounter.getScaledWidth(Screen.dp(COUNTER_ICON_MARGIN + COUNTER_ADD_MARGIN))).toInt()
        }
        if (this.viewCountMode == VIEW_COUNT_MAIN) {
            max = (max - shareCounter!!.getScaledWidth(Screen.dp(COUNTER_ICON_MARGIN + COUNTER_ADD_MARGIN))).toInt()
            max = (max - viewCounter!!.getScaledWidth(Screen.dp(COUNTER_ICON_MARGIN + COUNTER_ADD_MARGIN))).toInt()
        }
        if (reactionsDrawMode == REACTIONS_DRAW_MODE_FLAT) {
            max -= reactionsCounterDrawable.getMinimumWidth()
            max = (max - reactionsCounter!!.getScaledWidth(Screen.dp(8f))).toInt()
        }
        if (reactionsDrawMode == REACTIONS_DRAW_MODE_ONLY_ICON) {
            max = (max - shrinkedReactionsCounter!!.getScaledWidth(Screen.dp(COUNTER_ICON_MARGIN + COUNTER_ADD_MARGIN))).toInt()
        }
        if (translationStyleMode() == Settings.TRANSLATE_MODE_INLINE) {
            max = (max - isTranslatedCounter.getScaledWidth(Screen.dp(COUNTER_ICON_MARGIN + COUNTER_ADD_MARGIN))).toInt()
        }
        var nameMaxWidth: Int
        if (isPsa) {
            nameMaxWidth = totalMaxWidth
            val text = Lang.getPsaNotificationType(controller(), msg!!.forwardInfo!!.publicServiceAnnouncementType)
            hPsaTextT = if (max <= 0) null else Text.Builder(
                tdlib, text, openParameters(), max,
                nameStyleProvider, this.chatAuthorPsaColorSet, null
            )
                .allClickable()
                .singleLine()
                .viewProvider(currentViews)
                .onClick(Text.ClickListener { view: View?, text: Text?, part: TextPart?, openParameters: UrlOpenParameters? ->
                    this.onNameClick(
                        view,
                        text,
                        part!!,
                        openParameters
                    )
                })
                .build()
        } else {
            hPsaTextT = null
            nameMaxWidth = max
        }
        if (nameMaxWidth > 0) {
            if (!forceForwardOrImportInfo() && sender.hasChatMark()) {
                hAuthorChatMark = makeChatMark(totalMaxWidth)
                nameMaxWidth -= hAuthorChatMark!!.getWidth() + Screen.dp(8f)
            }
            isChannelHeaderCounter.showHide(needDrawChannelIconInHeader(), false)
            if (needDrawChannelIconInHeader()) {
                nameMaxWidth = (nameMaxWidth - isChannelHeaderCounter.getScaledWidth(Screen.dp(1f))).toInt()
            }
            hAuthorAccentColor = if (forceForwardOrImportInfo()) forwardInfo!!.getAuthorAccentColor() else sender.getAccentColor()
            hAuthorNameT = makeName(
                authorName,
                hAuthorAccentColor,
                !(forceForwardOrImportInfo() && forwardInfo is TGSourceHidden),
                isPsa,
                !needName(false),
                if (msg!!.forwardInfo == null || forceForwardOrImportInfo()) msg!!.viaBotUserId else 0,
                nameMaxWidth,
                false
            )
        } else {
            hAuthorNameT = null
            hAuthorAccentColor = null
            hAuthorChatMark = null
            isChannelHeaderCounter.showHide(false, false)
        }
    }

    private val displayAuthor: String
        get() {
            if (forceForwardOrImportInfo()) {
                return forwardInfo!!.getAuthorName()
            } else {
                return sender.getName()
            }
        }

    private fun loadForward() {
        if (msg!!.forwardInfo != null) {
            when (msg!!.forwardInfo!!.origin.getConstructor()) {
                MessageOriginUser.CONSTRUCTOR -> {
                    forwardInfo = TGSourceUser(this, msg!!.forwardInfo!!.origin as MessageOriginUser?)
                }

                MessageOriginChat.CONSTRUCTOR -> {
                    forwardInfo = TGSourceChat(this, msg!!.forwardInfo!!.origin as MessageOriginChat?)
                }

                MessageOriginChannel.CONSTRUCTOR -> {
                    forwardInfo = TGSourceChat(this, msg!!.forwardInfo!!.origin as MessageOriginChannel?)
                }

                MessageOriginHiddenUser.CONSTRUCTOR -> {
                    forwardInfo = TGSourceHidden(this, msg!!.forwardInfo!!.origin as MessageOriginHiddenUser?)
                }

                else -> {
                    assertMessageOrigin_f2224a59()
                    throw unsupported(msg!!.forwardInfo!!.origin)
                }
            }
        } else if (msg!!.importInfo != null) {
            forwardInfo = TGSourceHidden(this, msg!!.importInfo)
        } else {
            return
        }
        buildForwardTime()
        forwardInfo!!.load()
    }

    fun rebuildForward() {
        if ((flags and FLAG_LAYOUT_BUILT) != 0) {
            if (useBubbles() && allowMessageHorizontalExtend()) {
                rebuildLayout()
            } else {
                buildForward()
            }
        }
    }

    private fun buildForward() {
        if (!useForward() || forwardInfo == null) {
            return
        }

        fTimeWidth = U.measureText(fTime, if (useBubbles()) mTimeBubble() else mTime(false))

        val totalMax: Float
        if (useBubbles()) {
            totalMax =
                (if (allowMessageHorizontalExtend()) this.realContentMaxWidth else getContentWidth() + (this.bubblePaddingRight + this.bubblePaddingLeft - (xBubblePaddingSmall + xBubblePadding) * 2)).toFloat()
        } else {
            totalMax = (this.width - xfContentLeft - xPaddingRight).toFloat()
        }

        var max: Float = totalMax - fTimeWidth - xTimePadding
        if (this.viewCountMode == VIEW_COUNT_FORWARD) {
            max -= viewCounter!!.getScaledWidth(Screen.dp(COUNTER_ICON_MARGIN + COUNTER_ADD_MARGIN)) +
                    shareCounter!!.getScaledWidth(Screen.dp(COUNTER_ICON_MARGIN + COUNTER_ADD_MARGIN))
        }

        val isPsa = this.isPsa && !forceForwardOrImportInfo()
        fAuthorNameAccentColor = forwardInfo!!.getAuthorAccentColor()
        fAuthorNameT = makeName(
            forwardInfo!!.getAuthorName(),
            fAuthorNameAccentColor,
            forwardInfo !is TGSourceHidden,
            isPsa,
            false,
            msg!!.viaBotUserId,
            (if (isPsa) totalMax else max).toInt(),
            true
        )
        if (isPsa) {
            val text = Lang.getPsaNotificationType(controller(), msg!!.forwardInfo!!.publicServiceAnnouncementType)
            fPsaTextT = Text.Builder(
                tdlib, text, openParameters(), max.toInt(),
                nameStyleProvider, this.chatAuthorPsaColorSet, null
            )
                .allClickable()
                .singleLine()
                .onClick(Text.ClickListener { view: View?, text: Text?, part: TextPart?, openParameters: UrlOpenParameters? ->
                    this.onForwardClick(
                        view,
                        text!!,
                        part!!,
                        openParameters
                    )
                })
                .build()
        } else {
            fPsaTextT = null
        }
    }

    open val forwardLineColor: Int
        get() = if (APPLY_ACCENT_TO_FORWARDS && !this.isOutgoingBubble && fAuthorNameAccentColor != null) fAuthorNameAccentColor!!.getVerticalLineColor() else this.verticalLineColor

    open val forwardAuthorNameLeft: Int
        get() = if (useBubbles()) this.internalBubbleStartX + Screen.dp(11f) else xfContentLeft

    private fun loadReply() {
        replyData = ReplyComponent(this)
        replyData!!.setUseColorize(!this.isOutgoingBubble)
        replyData!!.setViewProvider(currentViews)
        replyData!!.load()
    }

    fun onReplyLoaded() {
    }

    @MessageChangeType
    private fun performContentfulUpdate(act: FutureBool): Int {
        val height = getHeight()
        val width = this.width
        val contentWidth = getContentWidth()
        val updated = act.getBoolValue()
        if (updated) {
            if (width != this.width || contentWidth != getContentWidth()) {
                buildMarkup()
            }
            return if (height == getHeight()) MESSAGE_INVALIDATED else MESSAGE_CHANGED
        }
        return MESSAGE_NOT_CHANGED
    }

    @MessageChangeType
    fun replaceMessagePreview(chatId: Long, messageId: Long, newContent: MessageContent?): Int {
        return performContentfulUpdate(FutureBool {
            val replyUpdated = msg!!.replyTo.equalsTo(chatId, messageId) && replyData != null
            if (replyUpdated) {
                replyData!!.replaceMessageContent(messageId, newContent)
            }
            handleMessagePreviewChange(chatId, messageId, newContent) || replyUpdated
        })
    }

    protected open fun handleMessagePreviewChange(chatId: Long, messageId: Long, newContent: MessageContent?): Boolean {
        return false
    }

    @MessageChangeType
    fun removeMessagePreview(chatId: Long, messageId: Long): Int {
        return performContentfulUpdate(FutureBool {
            val replyUpdated = msg!!.replyTo.equalsTo(chatId, messageId) && replyData != null
            if (replyUpdated) {
                replyData!!.deleteMessageContent(messageId)
            }
            handleMessagePreviewDelete(chatId, messageId) || replyUpdated
        })
    }

    protected open fun handleMessagePreviewDelete(chatId: Long, messageId: Long): Boolean {
        return false
    }

    fun replaceReplyTranslation(chatId: Long, messageId: Long, translation: TdApi.FormattedText?) {
        if (msg!!.replyTo.equalsTo(chatId, messageId) && replyData != null) {
            replyData!!.replaceMessageTranslation(messageId, translation)
        }
    }

    private fun buildReply() {
        if (replyData != null) {
            var maxWidth = if (allowMessageHorizontalExtend()) this.realContentMaxWidth else getContentWidth()
            if (alignReplyHorizontally()) {
                maxWidth = this.width - getContentWidth() - Screen.dp(18f) - (if (Device.NEED_BIGGER_BUBBLE_OFFSETS) xBubbleLeft2 else xBubbleLeft1) * 2
            }
            replyData!!.layout(maxWidth)
        }
    }

    // Bubble stuff
    private fun checkEdges() {
        if (useBubbles()) {
            buildBubble(false)
        }

        if (!useBubbles() || useMediaBubbleReactions() || useStickerBubbleReactions()) {
            notifyBubbleChanged() // не костыль ?
        }
    }

    protected open fun useCircleBubble(): Boolean {
        return false
    }

    private fun allowMessageHorizontalExtend(): Boolean {
        return  /*msg.forwardInfo != null ||*/!useBubbles() || allowBubbleHorizontalExtend()
    }

    protected open fun allowBubbleHorizontalExtend(): Boolean {
        return true
    }

    private var bubbleTimePartWidth = 0

    protected fun getBubbleTimePartWidth(): Int {
        return if (useBubbles() && useBubbleTime()) bubbleTimePartWidth else 0
    }

    protected fun moveBubbleTimePartToLeft(): Boolean {
        return Lang.rtl()
    }

    protected fun needExpandBubble(bottomLineContentWidth: Int, bubbleTimePartWidth: Int, maxLineWidth: Int): Boolean {
        when (bottomLineContentWidth) {
            BOTTOM_LINE_KEEP_WIDTH -> return false
            BOTTOM_LINE_EXPAND_HEIGHT -> return true
            BOTTOM_LINE_DEFINE_BY_FACTOR -> throw UnsupportedOperationException()
        }
        return bottomLineContentWidth > 0 && (bottomLineContentWidth + bubbleTimePartWidth > maxLineWidth)
    }

    protected open val intermediateBubbleExpandFactor: Float
        get() {
            throw RuntimeException()
        }

    protected open fun getAnimatedBottomLineWidth(bubbleTimePartWidth: Int): Int {
        throw RuntimeException()
    }

    fun buildBubble(force: Boolean) {
        if (useBubbles()) {
            val needBubble = useBubble()

            val defaultBubbleWidth = computeBubbleWidth()
            val defaultBubbleHeight = computeBubbleHeight()
            var bubbleWidth = defaultBubbleWidth
            var bubbleHeight = defaultBubbleHeight
            var needAnimateTimeExpand = true
            timeAddedHeight = 0

            if (!headerDisabled() && !(drawBubbleTimeOverContent() && !useForward()) && useBubbleTime()) {
                val bubbleTimePartWidthWithoutPadding = computeBubbleTimePartWidth(false)
                val bubbleTimePartWidth = computeBubbleTimePartWidth(true)
                val bottomLineContentWidth =
                    if (useForward()) BOTTOM_LINE_EXPAND_HEIGHT else if (hasFooter()) footerText!!.getLastLineWidth() + Screen.dp(10f) else getBottomLineContentWidth()
                val allowHorizontalExtend = allowBubbleHorizontalExtend()

                val expandedBubbleWidth = if (allowHorizontalExtend) max(bubbleWidth, bubbleTimePartWidthWithoutPadding) else bubbleWidth
                val expandedBubbleHeight: Int = bubbleHeight + bubbleTimePartHeight

                when (bottomLineContentWidth) {
                    BOTTOM_LINE_KEEP_WIDTH -> {}
                    BOTTOM_LINE_DEFINE_BY_FACTOR -> {
                        val extendedWidth = getAnimatedBottomLineWidth(bubbleTimePartWidth)
                        val fitBubbleWidth = max(bubbleWidth, if (extendedWidth != -1) extendedWidth + bubbleTimePartWidth else bubbleWidth)

                        val factor = this.intermediateBubbleExpandFactor
                        if (factor > 0f) {
                            bubbleWidth = fromTo(fitBubbleWidth, expandedBubbleWidth, factor)
                            val newBubbleHeight = fromTo(bubbleHeight, expandedBubbleHeight, factor)
                            timeAddedHeight = newBubbleHeight - bubbleHeight
                            bubbleHeight = newBubbleHeight
                        } else {
                            bubbleWidth = fitBubbleWidth
                        }
                        needAnimateTimeExpand = false
                    }

                    else -> {
                        val maxLineWidth = if (allowHorizontalExtend) this.realContentMaxWidth else min(this.realContentMaxWidth, bubbleWidth)
                        if (needExpandBubble(bottomLineContentWidth, bubbleTimePartWidth, maxLineWidth)) {
                            bubbleWidth = expandedBubbleWidth
                            timeAddedHeight = expandedBubbleHeight - bubbleHeight
                            bubbleHeight = expandedBubbleHeight
                        } else {
                            val extendedWidth = bottomLineContentWidth + bubbleTimePartWidth
                            val fitBubbleWidth = max(bubbleWidth, extendedWidth)
                            bubbleWidth = fitBubbleWidth
                        }
                    }
                }

                if (messageReactions!!.getVisibility() > 0f && useReactionBubbles()) {
                    val reactionsWidth: Int =
                        messageReactions.getAnimatedWidth().toInt() + xReactionBubblePadding * 2 - this.bubblePaddingLeft - this.bubblePaddingRight
                    val reactionsFitBubbleWidth = max(max(defaultBubbleWidth, reactionsWidth), bubbleTimePartWidthWithoutPadding)

                    val reactionsFinalCorrectedWidth: Int
                    val reactionsFinalCorrectedHeight: Int

                    reactionsFinalCorrectedWidth = fromTo(
                        reactionsFitBubbleWidth,
                        max(expandedBubbleWidth, reactionsWidth),
                        messageReactions.getTimeHeightExpand()
                    )
                    reactionsFinalCorrectedHeight = fromTo(defaultBubbleHeight, expandedBubbleHeight, messageReactions.getTimeHeightExpand())
                    timeAddedHeight = fromTo(
                        timeAddedHeight.toFloat(),
                        bubbleTimePartHeight * messageReactions.getTimeHeightExpand(),
                        messageReactions.getVisibility()
                    ).toInt()
                    needAnimateTimeExpand = false

                    bubbleWidth = fromTo(bubbleWidth, reactionsFinalCorrectedWidth, messageReactions.getVisibility())
                    bubbleHeight = fromTo(bubbleHeight, reactionsFinalCorrectedHeight, messageReactions.getVisibility())
                }

                this.bubbleTimePartWidth = bubbleTimePartWidth
            } else {
                this.bubbleTimePartWidth = 0
            }

            if (timeExpandValue.getToFactor() != timeAddedHeight.toFloat()) {
                if (needAnimateChanges() && needAnimateTimeExpand) {
                    timeExpandValue.animateTo(timeAddedHeight.toFloat())
                } else {
                    timeExpandValue.forceFactor(timeAddedHeight.toFloat())
                }
            }

            bubbleHeight = (bubbleHeight - timeAddedHeight + timeExpandValue.getFactor()).toInt()

            val bubblePaddingLeft = this.bubblePaddingLeft
            val bubblePaddingTop = this.bubblePaddingTop
            val bubblePaddingRight = this.bubblePaddingRight
            val bubblePaddingBottom = this.bubblePaddingBottom

            bubbleWidth += bubblePaddingLeft + bubblePaddingRight
            bubbleHeight += bubblePaddingTop + bubblePaddingBottom

            var leftContentEdge = if (centerBubble()) width / 2 - bubbleWidth / 2 else computeBubbleLeft()
            var rightContentEdge = leftContentEdge + bubbleWidth

            var topContentEdge = computeBubbleTop()
            val bottomContentEdge = topContentEdge + bubbleHeight

            val bubbleDefaultRadius = Screen.dp(Theme.getBubbleDefaultRadius())
            val bubbleMergeRadius = Screen.dp(Theme.getBubbleMergeRadius())

            if (this.leftContentEdge == leftContentEdge && this.topContentEdge == topContentEdge && this.rightContentEdge == rightContentEdge && this.bottomContentEdge == bottomContentEdge && this.lastMergeRadius == bubbleMergeRadius.toFloat() && this.lastDefaultRadius == bubbleDefaultRadius.toFloat() && !force) {
                return
            }

            this.leftContentEdge = leftContentEdge
            this.topContentEdge = topContentEdge
            this.rightContentEdge = rightContentEdge
            this.bottomContentEdge = bottomContentEdge

            this.lastMergeRadius = bubbleMergeRadius.toFloat()
            this.lastDefaultRadius = bubbleDefaultRadius.toFloat()

            if (needBubble) {
                bubblePath.reset()
                bubbleClipPath.reset()
            }

            if (alignBubbleRight()) {
                val translateBy = width - rightContentEdge - leftContentEdge
                leftContentEdge += translateBy
                rightContentEdge += translateBy
            }

            if (needBubble) {
                val circleBubble = useCircleBubble()
                var dr: Int
                var mr: Int
                if (circleBubble) {
                    mr = bubbleWidth / 2
                    dr = mr
                    topContentEdge = bottomContentEdge - bubbleWidth
                } else {
                    dr = bubbleDefaultRadius
                    mr = bubbleMergeRadius
                }
                val mergeTop = !headerDisabled() /* && !drawBubbleTimeOverContent()*/ // !hasHeader() && !headerDisabled();
                val mergeBottom = (flags and MESSAGE_FLAG_IS_BOTTOM) == 0 || (msg!!.content.getConstructor() == MessageGame.CONSTRUCTOR)

                synchronized(bubblePath) {
                    val alignContentRight = alignBubbleRight()
                    if (circleBubble) {
                        this.bubbleTopRightRadius = dr.toFloat()
                        this.bubbleTopLeftRadius = this.bubbleTopRightRadius
                        this.bubbleBottomLeftRadius = this.bubbleTopLeftRadius
                        this.bubbleBottomRightRadius = this.bubbleBottomLeftRadius
                        val centerX = (leftContentEdge + rightContentEdge) * .5f
                        val centerY = (bottomContentEdge - dr).toFloat()
                        bubblePath.addCircle(centerX, centerY, dr.toFloat(), Path.Direction.CW)
                        bubbleClipPath.addCircle(centerX, centerY, dr.toFloat(), Path.Direction.CW)
                        bubblePathRect.set(
                            leftContentEdge.toFloat(),
                            (bottomContentEdge - bubbleWidth).toFloat(),
                            rightContentEdge.toFloat(),
                            bottomContentEdge.toFloat()
                        )
                        bubbleClipPathRect.set(bubblePathRect)
                    } else {
                        bubblePathRect.set(leftContentEdge.toFloat(), topContentEdge.toFloat(), rightContentEdge.toFloat(), bottomContentEdge.toFloat())
                        DrawAlgorithms.buildPath(
                            bubblePath,
                            bubblePathRect,
                            (if (mergeTop && !alignContentRight) mr else dr).toFloat().also { this.bubbleTopLeftRadius = it },
                            (if (mergeTop && alignContentRight) mr else dr).toFloat().also { this.bubbleTopRightRadius = it },
                            (if (mergeBottom && alignContentRight) mr else dr).toFloat().also { this.bubbleBottomRightRadius = it },
                            (if (mergeBottom && !alignContentRight) mr else dr).toFloat().also { this.bubbleBottomLeftRadius = it })
                        bubbleClipPathRect.set(
                            (leftContentEdge + bubblePaddingLeft).toFloat(),
                            (topContentEdge + bubblePaddingTop - this.bubbleSpecialPaddingTop).toFloat(),
                            (rightContentEdge - bubblePaddingRight).toFloat(),
                            (bottomContentEdge - bubblePaddingBottom).toFloat()
                        )
                        dr = (dr / 1.5).toInt()
                        mr = (mr / 1.5).toInt()
                        DrawAlgorithms.buildPath(
                            bubbleClipPath,
                            bubbleClipPathRect,
                            (if (mergeTop && !alignContentRight) mr else dr).toFloat(),
                            (if (mergeTop && alignContentRight) mr else dr).toFloat(),
                            (if (mergeBottom && alignContentRight) mr else dr).toFloat(),
                            (if (mergeBottom && !alignContentRight) mr else dr).toFloat()
                        )
                    }
                }
            }

            updateContentPositions(false)
            notifyBubbleChanged()
            // invalidateOutline(true);
        }
    }

    open val forceTimeExpandHeightByReactions: Boolean
        get() = false

    private fun notifyBubbleChanged() {
        val oldHeight = height
        height = computeHeight()
        onBubbleHasChanged()
        if (height != oldHeight) {
            // FIXME?
            if (hasAnyTargetToInvalidate()) {
                manager.onMessageHeightChanged(this.chatId, this.id, oldHeight, height)
            }
            requestLayout()
        }
    }

    protected open fun onBubbleHasChanged() {
        // override
    }

    fun getBubblePath(): Path? {
        return if (disableBubble()) null else bubblePath
    }

    protected open fun getAbsolutelyRealRightContentEdge(view: View?, timePartWidth: Int): Int {
        return this.actualRightContentEdge - timePartWidth
    }

    protected open val bubbleTimePartOffsetY: Int
        get() = Screen.dp(8f)

    protected val timePartTextColor: Int
        get() {
            if (!useBubbles()) {
                return this.decentColor
            }
            val isTransparent = !useBubble() || useCircleBubble()
            val isWhite = isTransparent || (drawBubbleTimeOverContent() && !useForward())
            if (!isWhite) { // Inside bubble
                return this.decentColor
            } else if (isTransparent) {
                return this.bubbleTimeTextColor
            } else {
                return Theme.getColor(ColorId.bubble_mediaOverlayText)
            }
        }

    protected val timePartIconColorId: Int
        get() {
            if (!useBubbles()) {
                return this.decentIconColorId
            }

            val isTransparent = !useBubble() || useCircleBubble()
            val isWhite = isTransparent || (drawBubbleTimeOverContent() && !useForward())

            if (!isWhite) { // Inside bubble
                return this.decentIconColorId
            } else if (isTransparent) { // Partially on the content
                return ColorId.bubble_mediaTime
            } else {
                return ColorId.bubble_mediaOverlayText
            }
        }

    protected open fun drawBubbleTimePart(c: Canvas, view: MessageView?) {
        val isTransparent = !useBubble() || useCircleBubble()
        val isWhite = isTransparent || (drawBubbleTimeOverContent() && !useForward())
        val reactionsDrawMode = this.reactionsDrawMode

        val iconColorId: Int
        val backgroundColor: Int
        val textColor: Int
        val iconPaint: Paint?
        val ticksPaint: Paint?
        val ticksReadPaint: Paint?

        if (!isWhite) { // Inside bubble
            textColor = this.decentColor
            iconColorId = this.decentIconColorId
            backgroundColor = 0
            iconPaint = this.decentIconPaint
            ticksPaint = Paints.getBubbleTicksPaint()
            ticksReadPaint = Paints.getBubbleTicksReadPaint()
        } else if (isTransparent) { // Partially on the content
            textColor = this.bubbleTimeTextColor
            iconColorId = ColorId.bubble_mediaTimeText
            backgroundColor = this.bubbleTimeColor
            ticksReadPaint = Paints.getBubbleTimePaint(textColor)
            ticksPaint = ticksReadPaint
            iconPaint = ticksPaint
        } else { // Media
            iconColorId = ColorId.bubble_mediaOverlayText
            textColor = Theme.getColor(ColorId.bubble_mediaOverlayText)
            backgroundColor = Theme.getColor(ColorId.bubble_mediaOverlay)
            ticksReadPaint = Paints.getBubbleOverlayTimePaint(textColor)
            ticksPaint = ticksReadPaint
            iconPaint = ticksPaint
        }

        val innerWidth = computeBubbleTimePartWidth(false)
        var startX: Int

        val isSending = this.isSending
        val isFailed = this.isFailed

        val reverseOrder: Boolean

        if (((Config.MOVE_BUBBLE_TIME_RTL_TO_LEFT && moveBubbleTimePartToLeft()).also { reverseOrder = it })) {
            startX = this.actualLeftContentEdge + Screen.dp(10f)
        } else {
            startX = getAbsolutelyRealRightContentEdge(view, innerWidth + Screen.dp(11f))
        }
        var startY: Int = bottomContentEdge - bubbleTimePartHeight - this.bubbleTimePartOffsetY
        if (commentButton!!.isVisible() && commentButton.isInline()) {
            startY -= commentButton.getAnimatedHeight(0, commentButton.getVisibility())
        }

        if (backgroundColor != 0) {
            startY -= Screen.dp(4f)
            val rectF = Paints.getRectF()
            val padding = Screen.dp(6f)
            rectF.set((startX - padding).toFloat(), startY.toFloat(), (startX + innerWidth + padding).toFloat(), (startY + Screen.dp(21f)).toFloat())
            c.drawRoundRect(rectF, Screen.dp(12f).toFloat(), Screen.dp(12f).toFloat(), Paints.fillingPaint(backgroundColor))
            startY -= Screen.dp(1f)
        }

        val counterY = startY + Screen.dp(11.5f)

        if (reactionsDrawMode == REACTIONS_DRAW_MODE_FLAT) {
            drawReactionsWithoutBubbles(c, startX, counterY)
            startX = (startX + (reactionsCounterDrawable.getMinimumWidth() + Screen.dp(COUNTER_ADD_MARGIN) * messageReactions!!.getVisibility())).toInt()
            reactionsCounter!!.draw(c, startX.toFloat(), counterY.toFloat(), Gravity.LEFT, 1f, view, iconColorId)
            startX = (startX + reactionsCounter!!.getScaledWidth(Screen.dp(5f))).toInt()
        }

        if (this.viewCountMode == VIEW_COUNT_MAIN) {
            if (isSending) {
                val viewsWidth = viewCounter!!.getScaledWidth(Screen.dp(COUNTER_ICON_MARGIN + COUNTER_ADD_MARGIN))
                val clockWidth = Icons.getClockIconWidth() + Screen.dp(3f)
                if ((useBubble() && !drawBubbleTimeOverContent()) && viewsWidth > clockWidth) {
                    startX = (startX + (viewsWidth - clockWidth)).toInt()
                }
                Drawables.draw(
                    c,
                    Icons.getClockIcon(iconColorId),
                    (startX - Screen.dp(Icons.CLOCK_SHIFT_X)).toFloat(),
                    (startY + Screen.dp(5f) - Screen.dp(Icons.CLOCK_SHIFT_Y)).toFloat(),
                    iconPaint
                )
                startX += clockWidth
            } else {
                viewCounter!!.draw(c, startX.toFloat(), counterY.toFloat(), Gravity.LEFT, 1f, view, iconColorId)
                startX = (startX + viewCounter.getScaledWidth(Screen.dp(COUNTER_ICON_MARGIN + COUNTER_ADD_MARGIN))).toInt()
            }
            shareCounter!!.draw(c, startX.toFloat(), counterY.toFloat(), Gravity.LEFT, 1f, view, iconColorId)
            startX = (startX + shareCounter.getScaledWidth(Screen.dp(COUNTER_ICON_MARGIN + COUNTER_ADD_MARGIN))).toInt()
        }
        if (replyCounter!!.getVisibility() > 0f) {
            replyCounter.draw(c, startX.toFloat(), counterY.toFloat(), Gravity.LEFT, 1f, view, iconColorId)
            startX = (startX + replyCounter.getScaledWidth(Screen.dp(COUNTER_ICON_MARGIN + COUNTER_ADD_MARGIN))).toInt()
        }
        isPinned!!.draw(c, startX.toFloat(), counterY.toFloat(), Gravity.LEFT, 1f, view, iconColorId)
        startX = (startX + isPinned.getScaledWidth(Screen.dp(COUNTER_ICON_MARGIN))).toInt()

        if (shouldShowMessageRestrictedWarning()) {
            if (this.isRestrictedByTelegram) {
                isRestricted!!.draw(c, startX.toFloat(), counterY.toFloat(), Gravity.LEFT, 1f, view, ColorId.NONE, isRestrictedCounterLastDrawRect)
                startX = (startX + isRestricted.getScaledWidth(Screen.dp(COUNTER_ICON_MARGIN))).toInt()
            } else {
                isUnsupported!!.draw(c, startX.toFloat(), counterY.toFloat(), Gravity.LEFT, 1f, view, iconColorId, isRestrictedCounterLastDrawRect)
                startX = (startX + isUnsupported.getScaledWidth(Screen.dp(COUNTER_ICON_MARGIN))).toInt()
            }
        }

        if (shouldShowEdited()) {
            if (this.isBeingEdited) {
                Drawables.draw(
                    c,
                    Icons.getClockIcon(iconColorId),
                    (startX - Screen.dp(6f)).toFloat(),
                    (startY + Screen.dp(4.5f) - Screen.dp(5f)).toFloat(),
                    iconPaint
                )
                startX += Icons.getEditedIconWidth() + Screen.dp(3f)
            } else {
                isEdited!!.draw(c, startX.toFloat(), counterY.toFloat(), Gravity.LEFT, 1f, view, iconColorId, isEditedCounterLastDrawRect)
                startX = (startX + isEdited.getScaledWidth(Screen.dp(COUNTER_ICON_MARGIN))).toInt()
            }
        }

        if (translationStyleMode() == Settings.TRANSLATE_MODE_INLINE) {
            isTranslatedCounter.draw(c, startX.toFloat(), counterY.toFloat(), Gravity.LEFT, 1f, isTranslatedCounterLastDrawRect)
            startX = (startX + isTranslatedCounter.getScaledWidth(Screen.dp(COUNTER_ICON_MARGIN + COUNTER_ADD_MARGIN))).toInt()
        }

        if (time != null) {
            c.drawText(time!!, startX.toFloat(), (startY + Screen.dp(15.5f)).toFloat(), Paints.colorPaint<TextPaint?>(mTimeBubble(), textColor))
            startX += pTimeWidth
        }

        if (this.isOutgoingBubble || (isSending && this.viewCountMode != VIEW_COUNT_MAIN)) {
            val top: Int

            startX += Screen.dp(3.5f)

            if (isSending) {
                top = startY + Screen.dp(5f)
                startX += Screen.dp(1f)
            } else {
                top = startY + Screen.dp(4.5f)
            }

            if (isFailed) {
                // TODO failure icon
            } else if (isSending) {
                Drawables.draw(
                    c,
                    Icons.getClockIcon(iconColorId),
                    (startX - Screen.dp(Icons.CLOCK_SHIFT_X)).toFloat(),
                    (top - Screen.dp(Icons.CLOCK_SHIFT_Y)).toFloat(),
                    iconPaint
                )
            } else {
                val unread = this.isUnread && !noUnread()
                Drawables.draw(
                    c,
                    if (unread) Icons.getSingleTick(iconColorId) else Icons.getDoubleTick(iconColorId),
                    (startX - Screen.dp(Icons.TICKS_SHIFT_X)).toFloat(),
                    (top - Screen.dp(Icons.TICKS_SHIFT_Y)).toFloat(),
                    if (unread) ticksPaint else ticksReadPaint
                )
            }
            startX += Icons.getSingleTickWidth()
        }
    }

    @JvmOverloads
    protected fun computeBubbleTimePartWidth(includePadding: Boolean, isTarget: Boolean = false): Int {
        val reactionsDrawMode = this.reactionsDrawMode
        var width = 0
        /*if (shouldShowTicks()) {
      width += Screen.dp(3f) + Icons.getSingleTick().getWidth() + Screen.dp(3f);
    }
    if (shouldShowEdited()) {
      width += Screen.dp(5f) + Icons.getEditedIcon().getWidth();
    }*/
        width += pTimeWidth
        if (width == 0 && !isEmpty(time)) { // TODO do it in a proper place
            width = U.measureText(time, mTimeBubble()).toInt()
        }
        if (shouldShowEdited()) {
            if (this.isBeingEdited) {
                width += Icons.getEditedIconWidth() + Screen.dp(2f)
            } else {
                width = (width + isEdited!!.getScaledOrTargetWidth(Screen.dp(COUNTER_ICON_MARGIN), isTarget)).toInt()
            }
        }
        if (translationStyleMode() == Settings.TRANSLATE_MODE_INLINE) {
            width = (width + isTranslatedCounter.getScaledOrTargetWidth(Screen.dp(COUNTER_ICON_MARGIN + COUNTER_ADD_MARGIN), isTarget)).toInt()
        }
        val isSending = this.isSending
        if (this.viewCountMode == VIEW_COUNT_MAIN) {
            val viewsWidth = viewCounter!!.getScaledOrTargetWidth(Screen.dp(COUNTER_ICON_MARGIN + COUNTER_ADD_MARGIN), isTarget)
            val clockWidth = Icons.getClockIconWidth() + Screen.dp(3f)
            if (isSending && (drawBubbleTimeOverContent() || !useBubble())) {
                width += clockWidth
            } else {
                width = (width + viewsWidth).toInt()
            }
            width = (width + shareCounter!!.getScaledOrTargetWidth(Screen.dp(COUNTER_ICON_MARGIN + COUNTER_ADD_MARGIN), isTarget)).toInt()
        }
        if (replyCounter!!.getVisibility() > 0f) {
            width = (width + replyCounter.getScaledOrTargetWidth(Screen.dp(COUNTER_ICON_MARGIN + COUNTER_ADD_MARGIN), isTarget)).toInt()
        }
        width = (width + isPinned!!.getScaledOrTargetWidth(Screen.dp(COUNTER_ICON_MARGIN), isTarget)).toInt()
        if (shouldShowMessageRestrictedWarning()) {
            if (this.isRestrictedByTelegram) {
                width = (width + isRestricted!!.getScaledOrTargetWidth(Screen.dp(COUNTER_ICON_MARGIN), isTarget)).toInt()
            } else {
                width = (width + isUnsupported!!.getScaledOrTargetWidth(Screen.dp(COUNTER_ICON_MARGIN), isTarget)).toInt()
            }
        }
        if (reactionsDrawMode == REACTIONS_DRAW_MODE_FLAT) {
            width = (width + (reactionsCounterDrawable.getMinimumWidth() + messageReactions!!.getVisibility() * Screen.dp(3f))).toInt()
            width = (width + reactionsCounter!!.getScaledOrTargetWidth(Screen.dp(COUNTER_ICON_MARGIN + COUNTER_ADD_MARGIN), isTarget)).toInt()
        }
        if (!this.isFailed && (this.isOutgoingBubble || (isSending && this.viewCountMode != VIEW_COUNT_MAIN))) {
            width +=  /*Screen.dp(3.5f) +*/Icons.getSingleTickWidth() /*- Screen.dp(3.5f)*/ // singleTick bitmap contains padding
        }
        if (includePadding) {
            width += Screen.dp(8f)
        }
        return width
    }

    protected open fun drawBubbleTimeOverContent(): Boolean {
        return false
    }

    protected fun needTopContentRounding(): Boolean {
        return useBubbleName() || useForward() || replyData != null
    }

    protected fun needBottomContentRounding(): Boolean {
        if (useForward() || hasFooter()) {
            return true
        }
        if (drawBubbleTimeOverContent()) {
            return !commentButton!!.isVisible() || !commentButton.isInline()
        }
        return false
    }

    // Image receivers
    fun layoutAvatar(view: MessageView, receiver: AvatarReceiver) {
        var left: Int
        val top: Int
        val size: Int
        if (useBubbles()) {
            left = xBubbleAvatarLeft
            top = computeBubbleTop()
            size = xBubbleAvatarRadius * 2
        } else {
            top = this.headerPadding
            left = xAvatarLeft
            size = xAvatarRadius * 2
        }
        if (Lang.rtl() && useBubbles()) {
            left = view.getMeasuredWidth() - left - size
        }
        receiver.setBounds(left, top, left + size, top + size)
    }

    fun requestReply(receiver: DoubleImageReceiver, textMediaReceiver: ComplexReceiver?) {
        if (replyData != null) {
            replyData!!.requestPreview(receiver, textMediaReceiver)
        } else {
            receiver.clear()
        }
    }

    @JvmOverloads
    fun requestAvatar(receiver: AvatarReceiver, force: Boolean = false) {
        if (hasAvatar || force) {
            if (isSponsoredMessage()) {
                if (sponsoredMessage!!.sponsor.photo != null) {
                    receiver.requestSpecific(tdlib, TD.toChatPhotoInfo(sponsoredMessage.sponsor.photo), AvatarReceiver.Options.NONE)
                } else if (needAvatar()) {
                    receiver.requestPlaceholder(tdlib, sender.getPlaceholderMetadata(), AvatarReceiver.Options.NONE)
                } else {
                    receiver.clear()
                }
            } else if (forceForwardOrImportInfo()) {
                forwardInfo!!.requestAvatar(receiver)
            } else if (sender.isDemo()) {
                receiver.requestPlaceholder(tdlib, sender.getPlaceholderMetadata(), AvatarReceiver.Options.NONE)
            } else {
                receiver.requestMessageSender(tdlib, sender.toSender(), AvatarReceiver.Options.NONE)
            }
        } else {
            receiver.clear()
        }
    }

    var currentComplexReceiver: ComplexReceiver? = null

    fun requestReactions(complexReceiver: ComplexReceiver?) {
        currentComplexReceiver = complexReceiver
        messageReactions!!.requestReactionFiles(complexReceiver)
        computeQuickButtons()
    }

    fun requestCommentsResources(complexReceiver: ComplexReceiver?, isUpdate: Boolean) {
        if (commentButton != null) {
            commentButton.requestResources(complexReceiver, isUpdate)
        }
    }

    open fun requestGiveawayAvatars(complexReceiver: ComplexReceiver?, isUpdate: Boolean) {
    }

    fun requestReactionsResources(complexReceiver: ComplexReceiver?, isUpdate: Boolean) {
        if (messageReactions != null) {
            messageReactions.requestAvatarFiles(complexReceiver, isUpdate)
        }
    }


    fun requestAllTextMedia(view: MessageView) {
        requestTextMedia(view.getTextMediaReceiver())
        requestAuthorTextMedia(view.getEmojiStatusReceiver())

        if (footerText != null) {
            footerText!!.requestMedia(view.getFooterTextMediaReceiver(true))
        } else {
            val receiver = view.getFooterTextMediaReceiver(false)
            if (receiver != null) {
                receiver.clear()
            }
        }
    }

    fun requestAuthorTextMedia(textMediaReceiver: ComplexReceiver) {
        if (hAuthorEmojiStatus != null) {
            hAuthorEmojiStatus!!.requestMedia(textMediaReceiver)
        } else {
            textMediaReceiver.clear()
        }
    }

    open fun requestTextMedia(textMediaReceiver: ComplexReceiver) {
        // override in children
        textMediaReceiver.clear()
    }

    open fun getImageContentRadius(isPreview: Boolean): Int {
        return 0
    }

    open fun requestImage(receiver: ImageReceiver) {
        receiver.requestFile(null)
    }

    open fun requestGif(receiver: GifReceiver) {
        receiver.requestFile(null)
    }

    open fun requestPreview(receiver: DoubleImageReceiver) {
        receiver.clear()
    }

    open fun requestMediaContent(receiver: ComplexReceiver, invalidate: Boolean, invalidateArg: Int) {
        receiver.clear()
    }

    fun invalidateMediaContent(receiver: ComplexReceiver, messageId: Long) {
        receiver.clear()
    }

    // Getters
    fun onMessageClick(v: MessageView?, c: MessagesController?): Boolean {
        // TODO
        return  /* isEventLog() */false
    }

    val actualRightContentEdge: Int
        get() = if (alignBubbleRight()) width - leftContentEdge else rightContentEdge

    val actualLeftContentEdge: Int
        get() = if (alignBubbleRight()) width - rightContentEdge else leftContentEdge

    fun centerX(): Int {
        return if (useBubbles()) this.actualLeftContentEdge + (this.actualRightContentEdge - this.actualLeftContentEdge) / 2 else this.contentX + getContentWidth() / 2
    }

    fun centerY(): Int {
        return if (useBubbles()) bubblePathRect.centerY().toInt() else this.contentY + this.contentHeight / 2
    }

    fun isInsideBubble(x: Float, y: Float): Boolean {
        return x >= this.actualLeftContentEdge && x < this.actualRightContentEdge && y >= topContentEdge && y < bottomContentEdge
    }

    private val forwardLeft: Int
        get() = if (useBubbles()) this.internalBubbleStartX else contentLeft

    private val forwardTop: Int
        get() = if (useBubbles()) this.contentY - bubbleForwardOffset else (if ((flags and FLAG_HEADER_ENABLED) != 0) xContentTop - Screen.dp(
            3f
        ) else xPaddingTop - Screen.dp(3f)) + this.headerPadding

    private val forwardHeight: Int
        get() = this.contentHeight + forwardHeaderHeight + (if (this.isPsa && !forceForwardOrImportInfo()) psaTitleHeight else 0)

    val headerPadding: Int
        get() {
            val result: Int
            if ((flags and FLAG_SHOW_BADGE) != 0) {
                if ((flags and FLAG_SHOW_DATE) != 0) {
                    result =
                        xBadgeHeight + xBadgePadding + (if (useBubbles()) xDatePadding - Screen.dp(
                            3f
                        ) * 2 else xDatePadding)
                } else {
                    result = xBadgeHeight + xBadgePadding
                }
            } else {
                if ((flags and FLAG_SHOW_DATE) != 0) {
                    result =
                        (if (useBubbles()) xDatePadding - Screen.dp(3f) * 2 else xDatePadding)
                } else {
                    result = 0
                }
            }
            return if ((flags and FLAG_HEADER_ENABLED) != 0 && !useBubbles()) xHeaderPadding + result else result
        }

    val message: TdApi.Message
        get() = msg!!

    fun getMessageWithProperties(act: RunnableData<MessageWithProperties?>) {
        getMessageWithProperties(this.message, act)
    }

    fun getMessageWithProperties(message: TdApi.Message, act: RunnableData<MessageWithProperties?>) {
        getMessageProperties(message.id, RunnableData { properties: MessageProperties? ->
            if (properties != null) {
                act.runWithData(MessageWithProperties(message, properties))
            }
        })
    }

    fun getEvent(): ChatEvent? {
        return if (event != null) event!!.event else null
    }

    fun getMessage(messageId: Long): TdApi.Message? {
        synchronized(this) {
            if (this.combinedMessagesUnsafely != null) {
                for (message in this.combinedMessagesUnsafely) {
                    if (message.id == messageId) {
                        return message
                    }
                }
            }
        }
        return if (msg!!.id == messageId) msg else null
    }

    fun getMessageCountBetween(afterMessageId: Long, beforeMessageId: Long): Int {
        synchronized(this) {
            if (this.combinedMessagesUnsafely != null && !combinedMessagesUnsafely!!.isEmpty()) {
                var count = 0
                for (msg in this.combinedMessagesUnsafely) {
                    if (msg.id <= afterMessageId) {
                        continue
                    }
                    if (msg.id >= beforeMessageId) {
                        break
                    }
                    count++
                }
                return count
            }
        }
        return 0
    }

    val messageCount: Int
        get() {
            synchronized(this) {
                if (this.combinedMessagesUnsafely != null && !combinedMessagesUnsafely!!.isEmpty()) {
                    return combinedMessagesUnsafely!!.size
                }
            }
            return 1
        }

    val messageAlbumType: Int
        get() {
            val counters = SparseIntArray()
            iterate(RunnableData { message: TdApi.Message? ->
                var key: Int = MESSAGE_ALBUM_TYPE_MIXED
                when (message!!.content.getConstructor()) {
                    MessagePhoto.CONSTRUCTOR, MessageVideo.CONSTRUCTOR, MessageAnimation.CONSTRUCTOR -> key =
                        MESSAGE_ALBUM_TYPE_MEDIA

                    TdApi.MessageAudio.CONSTRUCTOR -> key = MESSAGE_ALBUM_TYPE_PLAYLIST
                    TdApi.MessageDocument.CONSTRUCTOR -> key = MESSAGE_ALBUM_TYPE_FILES
                }
                counters.increment(key)
            }, false)
            if (counters.size() > 1) {
                return MESSAGE_ALBUM_TYPE_MIXED
            }
            return if (counters.valueAt(0) > 1) counters.keyAt(0) else MESSAGE_ALBUM_TYPE_NONE
        }

    val allMessages: Array<TdApi.Message?>
        get() {
            synchronized(this) {
                if (this.combinedMessagesUnsafely != null && !combinedMessagesUnsafely!!.isEmpty()) {
                    val result = arrayOfNulls<TdApi.Message>(combinedMessagesUnsafely!!.size)
                    combinedMessagesUnsafely!!.toArray<TdApi.Message?>(result)
                    return result
                }
            }
            return arrayOf<TdApi.Message?>(msg)
        }

    val allMessagesAndProperties: Array<MessageWithProperties?>
        get() {
            synchronized(this) {
                if (this.combinedMessagesUnsafely != null && !combinedMessagesUnsafely!!.isEmpty()) {
                    val result = arrayOfNulls<MessageWithProperties>(combinedMessagesUnsafely!!.size)
                    for (i in result.indices) {
                        val message = combinedMessagesUnsafely!!.get(i)
                        val properties = lastMessageProperties(message.id)
                        result[i] = MessageWithProperties(message, properties)
                    }
                    return result
                }
            }
            return arrayOf<MessageWithProperties?>(
                MessageWithProperties(msg!!, lastMessageProperties(msg!!.id))
            )
        }

    fun iterate(callback: RunnableData<TdApi.Message?>, reverse: Boolean) {
        synchronized(this) {
            if (this.combinedMessagesUnsafely != null && !combinedMessagesUnsafely!!.isEmpty()) {
                if (reverse) {
                    val size = combinedMessagesUnsafely!!.size
                    for (i in size - 1 downTo 0) {
                        callback.runWithData(combinedMessagesUnsafely!!.get(i))
                    }
                } else {
                    for (message in this.combinedMessagesUnsafely) {
                        callback.runWithData(message)
                    }
                }
            } else {
                callback.runWithData(msg)
            }
        }
    }

    val sourceName: String?
        get() {
            if (forwardInfo != null) {
                if (forwardInfo!!.isReady()) {
                    return forwardInfo!!.getAuthorName()
                }
                if (forwardInfo is TGSourceChat) {
                    return null
                }
                if (forwardInfo is TGSourceUser) {
                    val user = tdlib.cache().user((forwardInfo as TGSourceUser).getSenderUserId())
                    return if (user != null) user.firstName else null
                }
                return null
            }
            return sender.getNameShort()
        }

    val inReplyTo: String?
        get() = if (replyData != null) replyData!!.getAuthor() else null

    val inReplyToSender: MessageSender?
        get() = if (replyData != null) replyData!!.getSender() else null

    val viewCount: Int
        get() {
            if (this.isSending || this.isFailed) {
                return 0
            }
            return TD.getViewCount(msg!!.interactionInfo)
        }

    val replyCount: Int
        get() = TD.getReplyCount(this.oldestMessage.interactionInfo)

    val replyInfo: MessageReplyInfo?
        get() = TD.getReplyInfo(this.oldestMessage.interactionInfo)

    val forwardCount: Int
        get() {
            val info = msg!!.interactionInfo
            return if (info != null) info.forwardCount else 0
        }

    val id: Long
        get() = msg!!.id

    fun toMessageId(): MessageId {
        return MessageId(msg!!.chatId, msg!!.id, getOtherMessageIds(msg!!.id))
    }

    val isRealMessage: Boolean
        get() = msg != null && !this.isFakeMessage && !isSponsoredMessage()

    @get:Suppress("WrongConstant")
    open val isFakeMessage: Boolean
        get() {
            if (msg!!.content.getConstructor() == MessageChatEvent.CONSTRUCTOR) {
                return true
            }
            return this.isDemoChat
        }

    fun getIds(ids: LongSet, afterMessageId: Long, beforeMessageId: Long) {
        if (this.isFakeMessage) {
            return
        }
        synchronized(this) {
            if (this.combinedMessagesUnsafely != null && !combinedMessagesUnsafely!!.isEmpty()) {
                ids.ensureCapacity(ids.size() + combinedMessagesUnsafely!!.size)
                for (msg in this.combinedMessagesUnsafely) {
                    if (msg.id != 0L && ((afterMessageId == 0L && beforeMessageId == 0L) || (msg.id > afterMessageId && msg.id < beforeMessageId))) {
                        ids.add(msg.id)
                    }
                }
                return
            }
        }
        if (msg!!.id != 0L && ((afterMessageId == 0L && beforeMessageId == 0L) || (msg!!.id > afterMessageId && msg!!.id < beforeMessageId))) {
            ids.add(msg!!.id)
        }
    }

    fun getIds(ids: LongSet) {
        getIds(ids, 0, 0)
    }

    val ids: LongArray
        get() {
            val ids = LongSet(this.messageCount)
            getIds(ids, 0, 0)
            val result = ids.toArray()
            Arrays.sort(result)
            return result
        }

    val isMessageThreadRoot: Boolean
        get() = canGetMessageThread() && (this.isChannel ||
                (this.isMessageThread && this.isThreadHeader) ||
                (msg!!.topicId.messageThreadId() == msg!!.id || msg!!.topicId == null /*FIXME TDLib*/)
                )

    val messageTopicId: MessageTopic?
        get() = this.oldestMessage.topicId

    fun getOtherMessageIds(exceptMessageId: Long): LongArray? {
        synchronized(this) {
            if (this.combinedMessagesUnsafely != null && combinedMessagesUnsafely!!.size > 1) {
                val result = LongArray(combinedMessagesUnsafely!!.size - 1)
                var i = 0
                for (msg in this.combinedMessagesUnsafely) {
                    if (msg.id != exceptMessageId) {
                        result[i] = msg.id
                        i++
                    }
                }
                Arrays.sort(result)
                return result
            }
        }
        return null
    }

    val smallestId: Long
        get() {
            synchronized(this) {
                if (this.combinedMessagesUnsafely != null && !combinedMessagesUnsafely!!.isEmpty()) {
                    return combinedMessagesUnsafely!!.get(0).id
                }
            }
            return msg!!.id
        }

    val biggestId: Long
        get() {
            synchronized(this) {
                if (this.combinedMessagesUnsafely != null && !combinedMessagesUnsafely!!.isEmpty()) {
                    return combinedMessagesUnsafely!!.get(combinedMessagesUnsafely!!.size - 1).id
                }
            }
            return msg!!.id
        }

    val oldestMessage: TdApi.Message
        get() {
            synchronized(this) {
                if (this.combinedMessagesUnsafely != null && !combinedMessagesUnsafely!!.isEmpty()) {
                    return combinedMessagesUnsafely!!.get(0)
                }
            }
            return msg!!
        }

    val newestMessage: TdApi.Message
        get() {
            synchronized(this) {
                if (this.combinedMessagesUnsafely != null && !combinedMessagesUnsafely!!.isEmpty()) {
                    return combinedMessagesUnsafely!!.get(combinedMessagesUnsafely!!.size - 1)
                }
            }
            return msg!!
        }

    fun containsUnreadReactions(): Boolean {
        synchronized(this) {
            if (this.combinedMessagesUnsafely != null) {
                for (i in combinedMessagesUnsafely!!.indices.reversed()) {
                    if (combinedMessagesUnsafely!!.get(i).unreadReactions != null && combinedMessagesUnsafely!!.get(i).unreadReactions.size > 0) {
                        return true
                    }
                }
            }
        }
        return msg!!.unreadReactions != null && msg!!.unreadReactions.size > 0
    }

    fun containsUnreadMention(): Boolean {
        synchronized(this) {
            if (this.combinedMessagesUnsafely != null) {
                for (i in combinedMessagesUnsafely!!.indices.reversed()) {
                    if (combinedMessagesUnsafely!!.get(i).containsUnreadMention) {
                        return true
                    }
                }
            }
        }
        return msg!!.containsUnreadMention
    }

    fun readMention(messageId: Long) {
        synchronized(this) {
            if (this.combinedMessagesUnsafely != null) {
                for (message in this.combinedMessagesUnsafely) {
                    if (message.id == messageId) {
                        message.containsUnreadMention = false
                        return
                    }
                }
            }
        }
        if (msg!!.id == messageId) {
            msg!!.containsUnreadMention = false
        }
    }

    val mediaGroupId: Long
        get() = msg!!.mediaAlbumId

    protected var combinedMessagesUnsafely: ArrayList<TdApi.Message>? = null
        private set

    @AnyThread
    protected open fun onMessageCombinedWithOtherMessage(otherMessage: TdApi.Message?, atBottom: Boolean, local: Boolean) {
        // override
    }

    @UiThread
    protected open fun onMessageCombinationRemoved(message: TdApi.Message?, index: Int) {
        // override
    }

    @AnyThread
    fun wouldCombineWith(message: TdApi.Message): Boolean {
        if (msg!!.mediaAlbumId == 0L || msg!!.mediaAlbumId != message.mediaAlbumId || !msg!!.selfDestructType.equalsTo(message.selfDestructType) ||
            ((msg!!.forwardInfo == null) != (message.forwardInfo == null)) ||
            ((msg!!.forwardInfo != null && message.forwardInfo != null && !msg!!.forwardInfo!!.origin.equalsTo(message.forwardInfo!!.origin, false))) ||
            this.isHot || this.isEventLog || isSponsoredMessage()
        ) {
            return false
        }
        val combineMode = TD.getCombineMode(msg)
        return combineMode != TD.COMBINE_MODE_NONE && TD.getCombineMode(message) == combineMode
    }

    @AnyThread
    fun combineWith(message: TdApi.Message, atBottom: Boolean): Boolean {
        if (!wouldCombineWith(message)) return false
        val local = (flags and FLAG_LAYOUT_BUILT) == 0
        synchronized(this) {
            if (this.combinedMessagesUnsafely == null) {
                this.combinedMessagesUnsafely = ArrayList<TdApi.Message>()
                combinedMessagesUnsafely!!.add(msg!!)
            }
            if (atBottom) {
                combinedMessagesUnsafely!!.add(message)
                msg = message
            } else {
                combinedMessagesUnsafely!!.add(0, message)
            }
            onMessageCombinedWithOtherMessage(message, atBottom, local)
            forceCancelGlobalAnimation(true)
        }
        updateInteractionInfo(false)
        if (!local) {
            if (atBottom) {
                layoutInfo()
            }
            rebuildAndUpdateContent()
        }
        computeQuickButtons()
        return true
    }

    val combinedMessageCount: Int
        get() {
            synchronized(this) {
                return if (this.combinedMessagesUnsafely != null) combinedMessagesUnsafely!!.size else 0
            }
        }

    private fun indexOfMessageInternal(messageId: Long): Int {
        if (this.combinedMessagesUnsafely != null) {
            var index = 0
            for (msg in this.combinedMessagesUnsafely) {
                if (msg.id == messageId) {
                    return index
                }
                index++
            }
        }
        if (msg!!.id == messageId) {
            return MESSAGE_INDEX_SELF
        }
        return MESSAGE_INDEX_NOT_FOUND
    }

    @UiThread
    fun removeMessage(messageId: Long): Int {
        synchronized(this) {
            val index = indexOfMessageInternal(messageId)
            if (index >= 0) {
                val message = combinedMessagesUnsafely!!.removeAt(index)
                if (index == combinedMessagesUnsafely!!.size && index > 0) {
                    msg = combinedMessagesUnsafely!!.get(index - 1)
                    computeQuickButtons()
                }
                if (combinedMessagesUnsafely!!.isEmpty()) {
                    return REMOVE_COMPLETELY
                }
                onMessageCombinationRemoved(message, index)
                forceRemoveAnimation(messageId)
                updateInteractionInfo(true)
                return REMOVE_COMBINATION
            }
            if (index == MESSAGE_INDEX_SELF) {
                return if (this.combinedMessagesUnsafely != null && !combinedMessagesUnsafely!!.isEmpty()) REMOVE_NOTHING else REMOVE_COMPLETELY
            }
            return REMOVE_NOTHING
        }
    }

    val chatId: Long
        get() = msg!!.chatId

    var administrator: ChatAdministrator? = null
        private set

    private val administratorSign: String?
        get() {
            var result: String? = null
            if (isSponsoredMessage()) {
                return null
            } else if (administrator != null) {
                if (!isEmpty(administrator!!.customTitle)) result = administrator!!.customTitle
                else if (administrator!!.isOwner) result = Lang.getString(R.string.message_ownerSign)
                else result = Lang.getString(R.string.message_adminSignPlain)
            } else if (sender.isAnonymousGroupAdmin()) {
                result =
                    if (!isEmpty(msg!!.authorSignature)) msg!!.authorSignature else Lang.getString(R.string.message_adminSignPlain)
            } else if (isEmpty(msg!!.authorSignature) && msg!!.chatId != 0L && tdlib.isMultiChat(msg!!.chatId)) {
                val chatId = sender.getChatId()
                if (tdlib.isChannel(chatId)) {
                    result = null //Lang.getString(R.string.message_channelSign);
                } else if (isMultiChat(chatId)) {
                    result = Lang.getString(R.string.message_groupSign)
                }
            }
            if (result != null) {
                if (useBubbles()) return "  " + result.trim { it <= ' ' }
                else return result
            }
            return null
        }

    private fun needAdminSign(): Boolean {
        return this.administratorSign != null
    }

    fun setAdministratorSign(administrator: ChatAdministrator?) {
        val isAdmin = (this.administrator != null || sender.isAnonymousGroupAdmin()) && !this.isOutgoing
        val nowIsAdmin = administrator != null
        if (isAdmin != nowIsAdmin || isAdmin) {
            this.administrator = administrator
            if ((flags and FLAG_LAYOUT_BUILT) != 0) {
                if (useBubbles()) {
                    buildHeader()
                    buildBubble(false)
                } else {
                    buildTime()
                    buildHeader()
                }
                invalidate()
            } else {
                buildTime()
            }
        }
    }

    fun getDate(): Int {
        return msg!!.date
    }

    val comparingDate: Int
        get() = if (event != null && event!!.event.date != 0) event!!.event.date else if (msg!!.schedulingState != null) (if (msg!!.schedulingState!!.getConstructor() == MessageSchedulingStateSendAtDate.CONSTRUCTOR) (msg!!.schedulingState as MessageSchedulingStateSendAtDate).sendDate else 0) else msg!!.date

    val isSending: Boolean
        get() = msg!!.sendingState != null && msg!!.sendingState!!.getConstructor() == TdApi.MessageSendingStatePending.CONSTRUCTOR && !tdlib.qack()
            .isMessageAcknowledged(msg!!.chatId, msg!!.id)

    val isPsa: Boolean
        get() = msg!!.forwardInfo != null && !isEmpty(msg!!.forwardInfo!!.publicServiceAnnouncementType) && !sender.isUser()

    fun useMediaBubbleReactions(): Boolean {
        val isTransparent = !useBubble() || useCircleBubble()
        val isWhite = isTransparent || (drawBubbleTimeOverContent() && !useForward())

        return (isWhite && !isTransparent)
    }

    fun useStickerBubbleReactions(): Boolean {
        val isTransparent = !useBubble() || useCircleBubble()
        val isWhite = isTransparent || (drawBubbleTimeOverContent() && !useForward())
        return (isWhite && isTransparent)
    }

    val pinnedMessageCount: Int
        get() {
            if (this.isThreadHeader) {
                return 0
            }
            synchronized(this) {
                if (this.combinedMessagesUnsafely != null && !combinedMessagesUnsafely!!.isEmpty()) {
                    var messageCount = 0
                    for (message in this.combinedMessagesUnsafely) {
                        if (message.isPinned) {
                            messageCount++
                        }
                    }
                    return messageCount
                }
            }
            return if (msg!!.isPinned) 1 else 0
        }

    fun isPinned(): Boolean {
        if (this.isThreadHeader) return false
        if (msg!!.isPinned) return true
        synchronized(this) {
            if (this.combinedMessagesUnsafely != null && !combinedMessagesUnsafely!!.isEmpty()) {
                for (message in this.combinedMessagesUnsafely) {
                    if (message.isPinned) {
                        return true
                    }
                }
            }
        }
        return false
    }

    val isOld: Boolean
        get() = tdlib().timeElapsedSinceDate(getDate().toLong(), TimeUnit.SECONDS) >= TimeUnit.MINUTES.toMillis(5)

    val isChatMember: Boolean
        get() {
            val chat = tdlib.chat(this.chatId)
            if (chat != null) {
                when (chat.type.getConstructor()) {
                    ChatTypePrivate.CONSTRUCTOR, TdApi.ChatTypeSecret.CONSTRUCTOR, TdApi.ChatTypeBasicGroup.CONSTRUCTOR -> return true
                    ChatTypeSupergroup.CONSTRUCTOR -> {
                        val status = tdlib.chatStatus(chat.id)
                        return status != null && TD.isMember(status, false)
                    }
                }
            }
            return false
        }

    protected open val isBeingEdited: Boolean
        get() = false // override in children

    val isFailed: Boolean
        get() = msg!!.sendingState != null && msg!!.sendingState!!.getConstructor() == MessageSendingStateFailed.CONSTRUCTOR

    fun canResend(): Boolean {
        if (msg!!.sendingState !is MessageSendingStateFailed || !(msg!!.sendingState as MessageSendingStateFailed).canRetry) {
            return false
        }
        synchronized(this) {
            if (this.combinedMessagesUnsafely != null) {
                for (msg in this.combinedMessagesUnsafely) {
                    if (msg.sendingState !is MessageSendingStateFailed || !(msg.sendingState as MessageSendingStateFailed).canRetry) {
                        return false
                    }
                }
            }
        }
        return true
    }

    val failureMessages: Array<String?>?
        get() {
            val errors: MutableSet<String?> = HashSet<String?>()
            synchronized(this) {
                if (this.combinedMessagesUnsafely != null && !combinedMessagesUnsafely!!.isEmpty()) {
                    for (msg in this.combinedMessagesUnsafely) {
                        if (msg.sendingState is MessageSendingStateFailed) {
                            val failed = msg.sendingState as MessageSendingStateFailed
                            errors.add(TD.toErrorString(failed.error))
                        }
                    }
                } else {
                    if (msg!!.sendingState is MessageSendingStateFailed) {
                        val failed = msg!!.sendingState as MessageSendingStateFailed
                        errors.add(TD.toErrorString(failed.error))
                    }
                }
            }
            return if (errors.isEmpty()) null else errors.toTypedArray<String?>()
        }

    val isNotSent: Boolean
        get() = msg!!.sendingState != null

    val isAnimating: Boolean
        get() = false // FIXME return correct animation state

    fun canBeDeletedForSomebody(): Boolean {
        val properties = lastMessageProperties()
        return (properties.canBeDeletedOnlyForSelf || properties.canBeDeletedForAllUsers) && allowInteraction()
    }

    fun canBeReported(): Boolean {
        if (isSponsoredMessage()) {
            return sponsoredMessage!!.canBeReported
        } else {
            return !this.isSelfChat && msg!!.sendingState == null && !msg!!.isOutgoing && tdlib.canReportChatSpam(msg!!.chatId) && !this.isEventLog
        }
    }

    fun canViewStatistics(): Boolean {
        val properties = lastMessageProperties()
        return properties.canGetStatistics
    }

    fun canGetViewers(): Boolean {
        val properties = lastMessageProperties()
        return properties.canGetViewers
    }

    val anchorMessageThreadMessage: TdApi.Message
        get() = this.oldestMessage

    fun canGetMessageThread(): Boolean {
        val properties = lastMessageProperties(this.anchorMessageThreadMessage.id)
        return properties.canGetMessageThread
    }

    fun canGetAddedReactions(): Boolean {
        synchronized(this) {
            if (this.combinedMessagesUnsafely != null) {
                for (message in this.combinedMessagesUnsafely) {
                    if (message.canGetAddedReactions()) {
                        return true
                    }
                }
            }
        }

        return msg.canGetAddedReactions()

        //return !isChannel() && messageReactions.getTotalCount() > 0 && (msg.forwardInfo == null || msg.forwardInfo.origin.getConstructor() != TdApi.MessageOriginChannel.CONSTRUCTOR);
    }

    open fun canBeSelected(): Boolean {
        return (!this.isNotSent || canResend()) && (flags and FLAG_UNSUPPORTED) == 0 && allowInteraction() && !isSponsoredMessage() && !messagesController().inSearchMode()
    }

    open fun canBePinned(): Boolean {
        return !this.isNotSent && allowInteraction() && !isSponsoredMessage()
    }

    fun canEditText(): Boolean {
        val properties = lastMessageProperties()
        return properties.canBeEdited && TD.canEditText(msg!!.content) && allowInteraction() && messagesController().canWriteMessages()
    }

    fun canBeForwarded(): Boolean {
        val properties = lastMessageProperties()
        return properties.canBeForwarded && (msg!!.content.getConstructor() != MessageLocation.CONSTRUCTOR || (msg!!.content as MessageLocation).expiresIn == 0) && !this.isEventLog
    }

    fun canBeReacted(): Boolean {
        return !isSponsoredMessage() && !this.isEventLog && !messageAvailableReactions.isEmpty() && (tdlib.hasPremium() || messageAvailableReactions!!.hasNonPremiumReactions())
    }

    open fun canBeSaved(): Boolean {
        return msg!!.canBeSaved
    }

    val isUnread: Boolean
        get() = (flags and MESSAGE_FLAG_READ) == 0 || (msg!!.sendingState != null)

    fun checkIsUnread(needMention: Boolean): Boolean {
        if (needMention && msg!!.containsUnreadMention) return true
        var chat = this.chat
        val messageThread = messagesController().messageThread
        if (chat == null) {
            chat = tdlib.chat(msg!!.chatId)
            setChatData(chat!!, messageThread)
        }
        val lastReadMessageId: Long
        if (messageThread != null) {
            lastReadMessageId = if (msg!!.isOutgoing) messageThread.getLastReadOutboxMessageId() else messageThread.getLastReadInboxMessageId()
        } else if (chat != null) {
            lastReadMessageId = if (msg!!.isOutgoing) chat.lastReadOutboxMessageId else chat.lastReadInboxMessageId
        } else {
            return false
        }
        return lastReadMessageId < this.biggestId
    }

    val isChannel: Boolean
        get() = msg!!.isChannelPost

    val isSecretChat: Boolean
        get() = isSecret(msg!!.chatId)

    val isGame: Boolean
        get() = msg!!.content.getConstructor() != MessageGame.CONSTRUCTOR

    val isOutgoing: Boolean
        get() = msg!!.isOutgoing && !this.isEventLog

    @CallSuper
    open fun markAsBeingAdded(isBeingAdded: Boolean) {
        this.flags = setFlag(flags, FLAG_BEING_ADDED, isBeingAdded)
    }

    val isBeingAdded: Boolean
        get() = hasFlag(flags, FLAG_BEING_ADDED)

    open fun canMarkAsViewed(): Boolean {
        return msg!!.id != 0L && msg!!.chatId != 0L && (flags and FLAG_VIEWED) == 0
    }

    fun markAsViewed(): Boolean {
        var result = false

        if (canMarkAsViewed()) {
            flags = flags or FLAG_VIEWED
            if (msg!!.containsUnreadMention) {
                highlight(true)
            }
            result = true
        }
        if (containsUnreadReactions() && !hasFlag(flags, FLAG_IGNORE_REACTIONS_VIEW)) {
            flags = flags or FLAG_IGNORE_REACTIONS_VIEW

            highlightUnreadReactions()
            highlight(true)
            tdlib.ui().postDelayed(Runnable {
                flags = setFlag(flags, FLAG_IGNORE_REACTIONS_VIEW, false)
            }, 500L)

            result = true
        }
        return result
    }

    open fun needRefreshViewCount(): Boolean {
        return !isSponsoredMessage() && viewCounter != null && !this.isSending
    }

    fun markAsUnread() {
        flags = flags and FLAG_VIEWED.inv()
    }

    val editDate: Int
        get() {
            synchronized(this) {
                if (this.combinedMessagesUnsafely != null && !combinedMessagesUnsafely!!.isEmpty()) {
                    var result = 0
                    for (message in this.combinedMessagesUnsafely) {
                        result = max(result, message.editDate)
                    }
                    return result
                }
            }

            return msg!!.editDate
        }

    open fun isEdited(): Boolean {
        if (msg!!.editDate > 0) {
            return true
        }
        synchronized(this) {
            if (this.combinedMessagesUnsafely != null && !combinedMessagesUnsafely!!.isEmpty()) {
                for (message in this.combinedMessagesUnsafely) {
                    if (message.editDate > 0) {
                        return true
                    }
                }
            }
        }
        return false
    }

    val isRestrictedByTelegram: Boolean
        get() = isRestrictedByTelegram(Settings.instance().needRestrictContent())

    fun isRestrictedByTelegram(restrictSensitiveContent: Boolean): Boolean {
        synchronized(this) {
            if (this.combinedMessagesUnsafely != null) {
                for (message in this.combinedMessagesUnsafely) {
                    if (message.restrictionInfo.hasRestriction(restrictSensitiveContent)) {
                        return true
                    }
                }
            }
        }

        return msg!!.restrictionInfo.hasRestriction(restrictSensitiveContent)
    }

    protected fun replaceTimeWithEditTime(): Boolean {
        return false
    }

    fun getMergeIndex(): Int {
        if ((flags and FLAG_HEADER_ENABLED) != 0) {
            return 0
        } else {
            return mergeIndex
        }
    }

    fun getMergeTime(): Int {
        if ((flags and FLAG_HEADER_ENABLED) != 0 || mergeTime == 0) {
            return msg!!.date
        } else {
            return mergeTime
        }
    }

    val isForward: Boolean
        get() = msg!!.forwardInfo != null

    // Setters
    var chat: Chat? = null
        private set

    val isUserChat: Boolean
        get() = isUserChat(this.chatId)

    val channelId: Long
        get() = toSupergroupId(msg!!.chatId)

    private fun setChatData(chat: Chat, messageThread: ThreadInfo?) {
        this.chat = chat
        var flags = this.flags
        flags = setFlag(flags, FLAG_NO_UNREAD, needNoUnread())
        flags = setFlag(flags, FLAG_SELF_CHAT, this.isSelfChat)
        this.flags = flags

        if (this.isOutgoing && !this.isSending) {
            setUnread(if (messageThread != null) messageThread.getLastReadOutboxMessageId() else chat.lastReadOutboxMessageId)
        }

        /*if (replyData != null && TD.isMultiChat(chat)) {
      replyData.setUseColorize(!useBubbles());
    }*/
        if (tdlib.isChannelChat(chat)) {
            if (replyData != null) {
                replyData!!.setChannelTitle(chat.title)
            }
            buildTime()
        }
    }

    private fun needNoUnread(): Boolean {
        if (chat != null) {
            when (chat!!.type.getConstructor()) {
                ChatTypePrivate.CONSTRUCTOR -> {
                    val userId = (chat!!.type as ChatTypePrivate).userId
                    return tdlib.isSelfUserId(userId) || tdlib.cache().userBot(userId)
                }
            }
        }
        return false
    }

    private val isSelfChat: Boolean
        get() = chat != null && chat!!.type.getConstructor() == ChatTypePrivate.CONSTRUCTOR && tdlib.isSelfUserId((chat!!.type as ChatTypePrivate).userId)

    fun needMessageButton(): Boolean {
        return ((flags and FLAG_SELF_CHAT) != 0 || this.isChannelAutoForward || this.isRepliesChat) && msg!!.forwardInfo.hasMessageSource() && msg!!.forwardInfo!!.source!!.chatId != msg!!.chatId
    }

    fun openSourceMessage() {
        if (msg!!.forwardInfo.hasMessageSource()) {
            if (this.isRepliesChat) {
                val replyMessageId = MessageId(msg!!.forwardInfo!!.source!!)
                val replyToMessageId = msg!!.replyTo.toMessageId()
                openMessageThread(replyMessageId, replyToMessageId)
            } else {
                tdlib.ui().openMessage(controller(), msg!!.forwardInfo!!.source!!.chatId, MessageId(msg!!.forwardInfo!!.source!!), openParameters())
            }
        }
    }

    fun noUnread(): Boolean {
        return (flags and FLAG_NO_UNREAD) != 0 || this.isDemoChat
    }

    @JvmOverloads
    fun findDescendantOrSelf(messageId: Long, otherMessageIds: LongArray? = null): TdApi.Message? {
        if (msg!!.id == messageId || (otherMessageIds != null && contains(otherMessageIds, messageId))) {
            return msg
        }
        synchronized(this) {
            if (this.combinedMessagesUnsafely != null) {
                for (msg in this.combinedMessagesUnsafely) {
                    if (msg.id == messageId || (otherMessageIds != null && contains(otherMessageIds, msg.id))) {
                        return msg
                    }
                }
            }
        }
        return null
    }

    fun isDescendantOrSelf(messageId: Long, otherMessageIds: LongArray?): Boolean {
        return findDescendantOrSelf(messageId, otherMessageIds) != null
    }

    fun isDescendantOrSelf(messageId: Long): Boolean {
        return findDescendantOrSelf(messageId) != null
    }

    protected open fun isSupportedMessageContent(message: TdApi.Message, messageContent: MessageContent): Boolean {
        return message.content.getConstructor() == messageContent.getConstructor()
    }

    protected open fun isSupportedMessagePendingContent(pending: MessageEditMediaPending): Boolean {
        return false
    }

    @MessageChangeType
    fun replaceMessageContent(chatId: Long, messageId: Long, newContent: MessageContent): Int {
        if (msg!!.chatId != chatId || !isDescendantOrSelf(messageId)) {
            return replaceMessagePreview(chatId, messageId, newContent)
        }
        val message: TdApi.Message?
        val isBottomMessage: Boolean
        synchronized(this) {
            val i = indexOfMessageInternal(messageId)
            if (i >= 0) {
                message = combinedMessagesUnsafely!!.get(i)
                isBottomMessage = i == combinedMessagesUnsafely!!.size - 1
            } else if (i == MESSAGE_INDEX_SELF) {
                message = msg
                isBottomMessage = true
            } else {
                message = null
                isBottomMessage = false
            }
        }
        if (message == null) {
            return MESSAGE_NOT_CHANGED
        }
        if ((flags and FLAG_UNSUPPORTED) != 0) {
            if (message.content.getConstructor() == TdApi.MessageUnsupported.CONSTRUCTOR && newContent.getConstructor() != TdApi.MessageUnsupported.CONSTRUCTOR) {
                message.content = newContent
                return MESSAGE_REPLACE_REQUIRED
            }
            return MESSAGE_NOT_CHANGED
        }
        if (isSupportedMessageContent(message, newContent)) {
            val height = getHeight()
            val width = this.width
            val contentWidth = getContentWidth()
            updateMessageContent(message, newContent, isBottomMessage)
            message.content = newContent
            if (width != this.width || contentWidth != getContentWidth()) {
                buildMarkup()
            }
            updateReactionAvatars(UI.inUiThread())
            return if (height == getHeight()) MESSAGE_INVALIDATED else MESSAGE_CHANGED
        }
        return MESSAGE_REPLACE_REQUIRED
    }

    protected open fun updateMessageContent(message: TdApi.Message?, newContent: MessageContent?, isBottomMessage: Boolean): Boolean {
        return false
    }

    open fun autoDownloadContent(type: ChatType?) {
        // Override in children
    }

    protected fun shouldHideMedia(): Boolean {
        return hasFlag(flags, FLAG_HIDE_MEDIA)
    }

    fun setMediaVisible(media: MediaItem?, isVisible: Boolean) {
        this.flags = setFlag(flags, FLAG_HIDE_MEDIA, !isVisible)
        invalidate()
    }

    open fun getMediaThumbLocation(messageId: Long, view: View?, viewTop: Int, viewBottom: Int, top: Int): MediaViewThumbLocation? {
        // override
        return null
    }

    fun updateUnread(lastReadOutboxId: Long, topMessage: TGMessage?): Boolean {
        if (setUnread(lastReadOutboxId)) {
            if (this.isOutgoing) {
                if ((flags and FLAG_HEADER_ENABLED) == 0) {
                    if (topMessage == null || ((this.isUnread || this.isSending) != (topMessage.isUnread || topMessage.isSending))) {
                        flags = flags or FLAG_SHOW_TICKS
                    } else {
                        flags = flags and FLAG_SHOW_TICKS.inv()
                    }
                }
            }
            return true
        }
        return false
    }

    fun setUnread(lastReadMessageId: Long): Boolean {
        val wasRead = (flags and MESSAGE_FLAG_READ) != 0
        if (msg!!.id > lastReadMessageId) {
            flags = flags and MESSAGE_FLAG_READ.inv()
            return wasRead && !noUnread()
        } else {
            flags = flags or MESSAGE_FLAG_READ
            return !wasRead && !noUnread()
        }
    }

    private fun buildForwardTime() {
        fTime = genForwardTime()
        if ((flags and FLAG_LAYOUT_BUILT) != 0) {
            buildForward()
        }
    }

    private fun buildTime() {
        this.time = genTime()
        if ((flags and FLAG_LAYOUT_BUILT) != 0 && (useBubbles() || (flags and FLAG_HEADER_ENABLED) != 0)) {
            layoutInfo()
        }
    }

    val isHot: Boolean
        // Hot stuff
        get() = isUserChat(msg!!.chatId) && msg!!.content.isSecret()
    // return msg.ttl > 0 && ((chat != null && chat.type.getConstructor() == TdApi.ChatTypePrivate.CONSTRUCTOR) || msg.ttl <= 60) && (flags & FLAG_EVENT_LOG) == 0 && !isEventLog();

    val isViewOnce: Boolean
        get() = msg!!.selfDestructType != null && msg!!.selfDestructType!!.getConstructor() == TdApi.MessageSelfDestructTypeImmediately.CONSTRUCTOR

    val isHotDone: Boolean
        get() = this.isOutgoing && this.isHotOpened

    val isHotOpened: Boolean
        get() {
            if (msg!!.selfDestructType != null && msg!!.selfDestructType!!.getConstructor() == MessageSelfDestructTypeTimer.CONSTRUCTOR) {
                val timer = msg!!.selfDestructType as MessageSelfDestructTypeTimer
                return msg!!.selfDestructIn != 0.0 && msg!!.selfDestructIn < timer.selfDestructTime
            }
            return false
        }

    protected open fun needHotTimer(): Boolean {
        return false // override
    }

    protected open fun onHotInvalidate(secondsChanged: Boolean) {
        // override
    }

    private var hotTimerStart: Long = 0

    fun readContent() {
        if (!this.isEventLog) {
            tdlib.send<TdApi.Ok?>(OpenMessageContent(msg!!.chatId, msg!!.id), tdlib.typedOkHandler())
            if (!this.isOutgoing) {
                startHotTimer(true)
            }
        }
    }

    val isContentRead: Boolean
        get() = msg!!.content.isListenedOrViewed()

    private fun startHotTimer(byEvent: Boolean) {
        if (this.isHot && needHotTimer() && hotTimerStart == 0L) {
            val hotHandler: HotHandler = hotHandler
            hotTimerStart = SystemClock.uptimeMillis()
            hotHandler.sendMessageDelayed(Message.obtain(hotHandler, HotHandler.MSG_HOT_CHECK, this), HOT_CHECK_DELAY.toLong())
            onHotTimerStarted(byEvent)
        }
    }

    protected open fun onHotTimerStarted(byEvent: Boolean) {
        // override
    }

    val isHotTimerStarted: Boolean
        get() = hotTimerStart != 0L

    private fun stopHotTimer() {
        if (hotTimerStart != 0L) {
            hotTimerStart = 0
            hotHandler.removeMessages(HotHandler.MSG_HOT_CHECK, this)
        }
    }

    private fun checkHotTimer() {
        if (msg!!.selfDestructType == null || msg!!.selfDestructType!!.getConstructor() != MessageSelfDestructTypeTimer.CONSTRUCTOR) {
            return
        }
        val timer = msg!!.selfDestructType as MessageSelfDestructTypeTimer
        val preSelfDestructIn = if (msg!!.selfDestructIn != 0.0) msg!!.selfDestructIn else timer.selfDestructTime.toDouble()
        val now = SystemClock.uptimeMillis()
        val elapsedMs = now - hotTimerStart
        hotTimerStart = now
        msg!!.selfDestructIn = max(0.0, preSelfDestructIn - elapsedMs.toDouble() / 1000.0)
        val secondsChanged = Math.round(preSelfDestructIn) != Math.round(msg!!.selfDestructIn)
        onHotInvalidate(secondsChanged)
        if (hotListener != null) {
            hotListener!!.onHotInvalidate(secondsChanged)
        }
        if (needHotTimer() && hotTimerStart != 0L && msg!!.selfDestructIn > 0) {
            val hotHandler: HotHandler = hotHandler
            hotHandler.sendMessageDelayed(Message.obtain(hotHandler, HotHandler.MSG_HOT_CHECK, this), HOT_CHECK_DELAY.toLong())
        }
    }

    val hotExpiresFactor: Float
        get() {
            if (msg!!.selfDestructType != null && msg!!.selfDestructType!!.getConstructor() == MessageSelfDestructTypeTimer.CONSTRUCTOR) {
                val timer = msg!!.selfDestructType as MessageSelfDestructTypeTimer
                return if (msg!!.selfDestructIn == 0.0) 1f else (msg!!.selfDestructIn / timer.selfDestructTime).toFloat()
            }
            return 0f
        }

    val hotTimerText: String?
        get() {
            var selfDestructIn = msg!!.selfDestructIn
            if (msg!!.selfDestructType != null) {
                when (msg!!.selfDestructType!!.getConstructor()) {
                    TdApi.MessageSelfDestructTypeImmediately.CONSTRUCTOR -> return Lang.getString(R.string.ViewOnce)
                    MessageSelfDestructTypeTimer.CONSTRUCTOR -> {
                        val timer = msg!!.selfDestructType as MessageSelfDestructTypeTimer
                        if (selfDestructIn == 0.0) {
                            selfDestructIn = timer.selfDestructTime.toDouble()
                        }
                    }

                    else -> {
                        assertMessageSelfDestructType_58882d8c()
                        throw unsupported(msg!!.selfDestructType!!)
                    }
                }
            }
            return TdlibUi.getDuration(Math.round(selfDestructIn), TimeUnit.SECONDS, false)
        }

    interface HotListener {
        fun onHotInvalidate(secondsChanged: Boolean)
    }

    private var hotListener: HotListener? = null

    fun setHotListener(listener: HotListener?) {
        this.hotListener = listener
    }

    // updates
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(MESSAGE_NOT_CHANGED, MESSAGE_INVALIDATED, MESSAGE_CHANGED, MESSAGE_REPLACE_REQUIRED)
    annotation class MessageChangeType

    interface MessageIdChangeListener {
        fun onMessageIdChanged(msg: TGMessage?, oldMessageId: Long, newMessageId: Long, success: Boolean)
    }

    private var messageIdChangeListeners: ReferenceList<MessageIdChangeListener?>? = null

    private fun updateMessageId(oldMessageId: Long, newMessageId: Long, success: Boolean) {
        onMessageIdChanged(oldMessageId, newMessageId, success)
        if (messageIdChangeListeners != null) {
            for (listener in messageIdChangeListeners) {
                listener!!.onMessageIdChanged(this, oldMessageId, newMessageId, success)
            }
        }
    }

    fun getMessageIdChangeListeners(): ReferenceList<MessageIdChangeListener?> {
        return (if (messageIdChangeListeners != null) messageIdChangeListeners else (me.vkryl.core.reference.ReferenceList<MessageIdChangeListener?>()
            .also { messageIdChangeListeners = it }))!!
    }

    protected open fun onMessageIdChanged(oldMessageId: Long, newMessageId: Long, success: Boolean) {}

    fun onMessageSendAcknowledged(messageId: Long): Boolean {
        updateMessageId(messageId, messageId, true)
        return msg!!.id == messageId
    }

    fun allowInteraction(): Boolean {
        return !this.isFakeMessage && !this.isEventLog && !this.isThreadHeader
    }

    fun canReplyTo(): Boolean {
        return TD.canReplyTo(msg) && allowInteraction()
    }

    val isScheduled: Boolean
        get() = msg!!.schedulingState != null

    protected open fun onMessageContentChanged(
        message: TdApi.Message?,
        oldContent: MessageContent?,
        newContent: MessageContent?,
        isBottomMessage: Boolean
    ): Boolean {
        // override
        return false
    }

    private var isMediaPending = false

    protected fun setIsMediaPending(): TGMessage {
        isMediaPending = true
        return this
    }

    fun setMessagePendingContentChanged(chatId: Long, messageId: Long): Int {
        val pending = tdlib.getPendingMessageMedia(chatId, messageId)
        if (pending != null && pending.getFile() != null && !isSupportedMessagePendingContent(pending)) {
            return MESSAGE_REPLACE_REQUIRED
        }

        if (isMediaPending && pending == null) {
            isMediaPending = false
            val message = getMessage(messageId)
            if (!isSupportedMessageContent(message!!, message.content)) {
                return MESSAGE_REPLACE_REQUIRED
            }
        }

        val oldHeight = getHeight()
        return onMessagePendingContentChanged(chatId, messageId, oldHeight)
    }

    protected open fun onMessagePendingContentChanged(chatId: Long, messageId: Long, oldHeight: Int): Int {
        return MESSAGE_NOT_CHANGED
    }

    fun setSendSucceeded(message: TdApi.Message, oldMessageId: Long): Int {
        var msg: TdApi.Message? = null
        var isBottomMessage = false
        synchronized(this) {
            if (this.combinedMessagesUnsafely != null && !combinedMessagesUnsafely!!.isEmpty()) {
                for (combinedMessage in this.combinedMessagesUnsafely) {
                    if (combinedMessage.id == oldMessageId) {
                        msg = combinedMessage
                        isBottomMessage = combinedMessage.id == combinedMessagesUnsafely!!.get(combinedMessagesUnsafely!!.size - 1).id
                        break
                    }
                }
            }
            if (msg == null && this.msg!!.id == oldMessageId) {
                msg = this.msg
                isBottomMessage = true
            }
        }
        if (msg == null || msg.id != oldMessageId) {
            return MESSAGE_NOT_CHANGED
        }

        val replaceRequired = !isSupportedMessageContent(msg, message.content) || (msg.forwardInfo == null) == (message.forwardInfo != null) ||
                (msg.forwardInfo != null && msg.forwardInfo!!.origin.getConstructor() != message.forwardInfo!!.origin.getConstructor()) || msg.isOutgoing != message.isOutgoing

        copyFlags(message, msg)
        if (inlineKeyboard != null) {
            inlineKeyboard!!.updateMessageId(oldMessageId, message.id)
        }

        if (isBottomMessage) {
            buildTime()
        }
        updateInteractionInfo(true)

        updateMessageId(oldMessageId, message.id, true)

        val oldHeight = getHeight()

        if (!replaceRequired && (flags and FLAG_UNSUPPORTED) == 0) {
            if (onMessageContentChanged(msg, msg.content, message.content, isBottomMessage)) {
                msg.content = message.content
            }
        }

        if (!msg.replyMarkup.equalsTo(message.replyMarkup)) {
            msg.replyMarkup = message.replyMarkup
            // if (isBottomMessage) {
            buildMarkup()
            height = computeHeight()
            // }
        }

        computeQuickButtons()
        return if (replaceRequired) MESSAGE_REPLACE_REQUIRED else if (getHeight() == oldHeight) MESSAGE_INVALIDATED else MESSAGE_CHANGED
    }

    private fun updateInteractionInfo(allowAnimation: Boolean) {
        val interactionInfo = msg!!.interactionInfo
        val animated = allowAnimation && needAnimateChanges()
        if (viewCounter != null) {
            viewCounter.setCount(TD.getViewCount(interactionInfo), animated && this.viewCountMode != VIEW_COUNT_HIDDEN)
        }
        val commentButtonViewMode = this.commentButtonViewMode
        commentButton!!.setViewMode(commentButtonViewMode, animated)
        if (commentButtonViewMode != TGCommentButton.VIEW_MODE_HIDDEN) {
            if (this.isRepliesChat) {
                commentButton.showAsViewInChat(animated)
            } else {
                commentButton.setReplyInfo(this.replyInfo, animated)
            }
            replyCounter!!.hide(animated)
        } else {
            replyCounter!!.setCount(if (this.isThreadHeader || this.isChannel) 0 else this.replyCount, animated)
        }
        shareCounter!!.setCount(if (interactionInfo != null) interactionInfo.forwardCount else 0, animated)
        isPinned!!.showHide(isPinned(), animated)

        if (this.combinedMessagesUnsafely != null) {
            messageReactions!!.setReactions(this.combinedMessagesUnsafely)
        } else {
            messageReactions!!.setReactions(if (interactionInfo != null) interactionInfo.reactions else null)
        }
        messageReactions.updateCounterAnimators(animated)
        if (allowAnimation) {
            buildReactions(animated)
        }
        if (reactionsCounter != null) {
            var count = messageReactions.getTotalCount()
            if (tdlib.isUserChat(msg!!.chatId) && messageReactions.getReactions() != null && (count == 1 || messageReactions.getReactions()
                    .reactionTypesCount() > 1)
            ) {
                count = 0
            }
            reactionsCounter!!.setCount(count.toLong(), !messageReactions.hasChosen(), animated)
        }

        if (shrinkedReactionsCounter != null) {
            shrinkedReactionsCounter!!.showHide(messageReactions.getTotalCount() > 0, animated)
            shrinkedReactionsCounter!!.setMuted(!messageReactions.hasChosen(), animated)
        }

        if (animated) {
            startReactionAnimationIfNeeded()
        }
        if (allowAnimation) {
            notifyBubbleChanged()
            layoutInfo()
        }
    }

    fun setSendFailed(message: TdApi.Message, oldMessageId: Long): Boolean {
        val msg = getMessage(oldMessageId)
        if (msg != null && msg.id == oldMessageId) {
            copyFlags(message, msg)
            if (inlineKeyboard != null) {
                inlineKeyboard!!.updateMessageId(oldMessageId, message.id)
            }
            buildTime()
            updateMessageId(oldMessageId, message.id, false)
            updateInteractionInfo(true)
            return true
        }
        return false
    }

    protected open fun onMessageEdited(messageId: Long, editDate: Int): Boolean {
        return false
    }

    @MessageChangeType
    fun setMessageEdited(messageId: Long, editDate: Int, replyMarkup: ReplyMarkup?): Int {
        var affectsGroup = false
        var markupChanged = false
        var wasEdited = false
        synchronized(this) {
            val i = indexOfMessageInternal(messageId)
            if (i >= 0) {
                affectsGroup = i == combinedMessagesUnsafely!!.size - 1
                val msg = combinedMessagesUnsafely!!.get(i)
                wasEdited = msg.editDate != 0
                msg.editDate = editDate
                markupChanged = !msg.replyMarkup.equalsTo(replyMarkup)
                msg.replyMarkup = replyMarkup
            } else if (i == MESSAGE_INDEX_SELF) {
                affectsGroup = true
                wasEdited = msg!!.editDate != 0
                msg!!.editDate = editDate
                markupChanged = !msg!!.replyMarkup.equalsTo(replyMarkup)
                msg!!.replyMarkup = replyMarkup
            }
        }
        if (affectsGroup || markupChanged) {
            if (affectsGroup && !wasEdited && (useBubbles() || (flags and FLAG_HEADER_ENABLED) != 0)) {
                layoutInfo()
            }
            if (markupChanged) {
                buildMarkup()
                val oldHeight = getHeight()
                height = computeHeight()
                return if (getHeight() == oldHeight) MESSAGE_INVALIDATED else MESSAGE_CHANGED
            }
            if (onMessageEdited(messageId, editDate) || !wasEdited) {
                return MESSAGE_INVALIDATED
            }
        } else if (onMessageEdited(messageId, editDate)) {
            return MESSAGE_INVALIDATED
        }
        return MESSAGE_NOT_CHANGED
    }

    fun setMessageOpened(messageId: Long) {
        synchronized(this) {
            val i = indexOfMessageInternal(messageId)
            if (i >= 0) {
                TD.setMessageOpened(combinedMessagesUnsafely!!.get(i))
            } else if (i == MESSAGE_INDEX_SELF) {
                TD.setMessageOpened(msg)
            } else {
                return
            }
        }
        startHotTimer(true)
        onMessageContentOpened(messageId)
    }

    protected open fun onMessageContentOpened(messageId: Long) {
        // override
    }

    fun setMessageUnreadReactions(messageId: Long, unreadReactions: Array<UnreadReaction?>?): Boolean {
        val changed: Boolean
        synchronized(this) {
            val i = indexOfMessageInternal(messageId)
            if (i >= 0) {
                combinedMessagesUnsafely!!.get(i).unreadReactions = unreadReactions
                changed = true // i == combinedMessages.size() - 1 || i == 0;
            } else if (i == MESSAGE_INDEX_SELF) {
                msg!!.unreadReactions = unreadReactions
                changed = true
            } else {
                return false
            }
        }
        return changed
    }

    fun setMessageInteractionInfo(messageId: Long, interactionInfo: MessageInteractionInfo?): Boolean {
        val changed: Boolean
        synchronized(this) {
            val i = indexOfMessageInternal(messageId)
            if (i >= 0) {
                combinedMessagesUnsafely!!.get(i).interactionInfo = interactionInfo
                changed = true //i == combinedMessages.size() - 1;
            } else if (i == MESSAGE_INDEX_SELF) {
                msg!!.interactionInfo = interactionInfo
                changed = true
            } else {
                return false
            }
            if (changed) {
                updateInteractionInfo(true)
            }
        }
        return changed
    }

    fun setIsPinned(messageId: Long, isPinned: Boolean): Boolean {
        var changed: Boolean
        synchronized(this) {
            val i = indexOfMessageInternal(messageId)
            if (i >= 0) {
                if ((combinedMessagesUnsafely!!.get(i).isPinned != isPinned).also { changed = it }) {
                    combinedMessagesUnsafely!!.get(i).isPinned = isPinned
                    changed = true
                }
                if (changed && !isPinned) {
                    for (message in this.combinedMessagesUnsafely!!) {
                        if (message.isPinned) {
                            changed = false
                            break
                        }
                    }
                }
            } else if (i == MESSAGE_INDEX_SELF) {
                changed = msg!!.isPinned != isPinned
                msg!!.isPinned = isPinned
            } else {
                return false
            }
        }
        if (changed) {
            this.isPinned!!.showHide(isPinned, needAnimateChanges())
            this.buildReactions(needAnimateChanges())
            return true
        }
        return false
    }

    fun setMergeBottom(mergeBottom: Boolean) {
        if (mergeBottom) {
            flags = flags or FLAG_MERGE_BOTTOM
        } else {
            flags = flags and FLAG_MERGE_BOTTOM.inv()
        }
    }

    private val badgeText: String
        get() {
            if (this.isBelowHeader) {
                return Lang.getString(R.string.DiscussionStart)
            }
            if (this.isFirstUnread) {
                return Lang.getString(R.string.NewMessages)
            }
            return ""
        }

    private fun updateShowBadge() {
        val showBadge = this.isFirstUnread || this.isBelowHeader && !messagesController().areScheduledOnly()
        flags = setFlag(flags, FLAG_SHOW_BADGE, showBadge)
    }

    private fun updateBadgeText(): Boolean {
        val badgeText = this.badgeText
        if (uBadge == null || uBadge!!.text != badgeText) {
            uBadge = Letters(badgeText)
            return true
        }
        return false
    }

    fun setShowUnreadBadge(show: Boolean) {
        flags = setFlag(flags, MESSAGE_FLAG_FIRST_UNREAD, show)
        updateShowBadge()
        updateBadgeText()
        if (hasFlag(flags, FLAG_LAYOUT_BUILT)) {
            rebuildLayout()
            requestLayout()
            invalidate()
        }
    }

    fun hasBadge(): Boolean {
        return hasFlag(flags, FLAG_SHOW_BADGE)
    }

    fun hasUnreadBadge(): Boolean {
        return this.isFirstUnread && hasBadge()
    }

    val isFirstUnread: Boolean
        get() = hasFlag(flags, MESSAGE_FLAG_FIRST_UNREAD)

    val isBelowHeader: Boolean
        get() = hasFlag(flags, MESSAGE_FLAG_BELOW_HEADER)

    private val isBottomMessage: Boolean
        get() = (flags and MESSAGE_FLAG_IS_BOTTOM) != 0

    fun setIsBottom(isBottom: Boolean) {
        if (isBottom != this.isBottomMessage) {
            if (isBottom) {
                flags = flags or MESSAGE_FLAG_IS_BOTTOM
            } else {
                flags = flags and MESSAGE_FLAG_IS_BOTTOM.inv()
            }
            if ((flags and FLAG_LAYOUT_BUILT) != 0) {
                buildBubble(true)
            }
        }
    }

    protected open fun getBubbleContentPadding(): Int = xBubblePadding

    protected open fun getBottomLineContentWidth(): Int = BOTTOM_LINE_EXPAND_HEIGHT

    private val bubblePaddingLeft: Int
        get() = if (useForward()) xBubblePadding + xBubblePaddingSmall else getBubbleContentPadding()

    open fun getChildrenLeft(): Int = contentX
    open fun getChildrenTop(): Int = contentY

    fun useBubbleName(): Boolean {
        return (flags and FLAG_HEADER_ENABLED) != 0 && needName(true)
    }

    private val bubbleSpecialPaddingTop: Int
        get() {
            /*if (disableBubble()) {
              return msg.replyToMessageId != 0 && !alignReplyHorizontally() ? Screen.dp(3f) : 0;
            }*/
            var padding = 0
            if (hasHeader() && needName(true) && (flags and FLAG_HEADER_ENABLED) != 0) {
                padding = max(xBubblePadding, getBubbleContentPadding())
            } else if (replyData != null && !alignReplyHorizontally()) {
                if (disableBubble()) {
                    padding = Screen.dp(3f)
                } else {
                    padding = max(xBubblePadding, getBubbleContentPadding())
                }
            }
            if (padding == 0 && useForward()) {
                padding = max(xBubblePadding + xBubblePaddingSmall, getBubbleContentPadding())
            }
            return padding
        }

    private val bubblePaddingTop: Int
        get() {
            val specialPadding = this.bubbleSpecialPaddingTop
            return if (specialPadding != 0) specialPadding else getBubbleContentPadding()
        }

    protected val bubblePaddingRight: Int
        get() = if (useForward()) xBubblePadding + xBubblePaddingSmall else getBubbleContentPadding()

    private val bubblePaddingBottom: Int
        get() {
            if (useForward()) {
                return xBubblePadding + xBubblePaddingSmall
            }
            if (drawBubbleTimeOverContent() && commentButton!!.isVisible() && commentButton.isInline()) {
                return Math.round(getBubbleContentPadding() * (1f - commentButton.getVisibility())).toInt()
            }
            return getBubbleContentPadding()
        }

    fun setNeedExtraPadding(needPadding: Boolean): Boolean {
        val oldHeight = height
        if (needPadding) {
            if ((flags and FLAG_EXTRA_PADDING) == 0) {
                flags = flags or FLAG_EXTRA_PADDING
                if ((flags and FLAG_LAYOUT_BUILT) != 0) {
                    height = computeHeight()
                }
            }
        } else {
            if ((flags and FLAG_EXTRA_PADDING) != 0) {
                flags = flags and FLAG_EXTRA_PADDING.inv()
                if ((flags and FLAG_LAYOUT_BUILT) != 0) {
                    height = computeHeight()
                }
            }
        }
        return oldHeight != height
    }

    fun setNeedExtraPresponsoredPadding(needPadding: Boolean): Boolean {
        val oldHeight = height
        if (needPadding) {
            if (!needSponsorSmallPadding) {
                needSponsorSmallPadding = true
                if ((flags and FLAG_LAYOUT_BUILT) != 0) {
                    height = computeHeight()
                }
            }
        } else {
            if (needSponsorSmallPadding) {
                needSponsorSmallPadding = false
                if ((flags and FLAG_LAYOUT_BUILT) != 0) {
                    height = computeHeight()
                }
            }
        }
        return oldHeight != height
    }

    private fun setHeaderEnabled(enabled: Boolean) {
        val isEnabledNow = (flags and FLAG_HEADER_ENABLED) != 0
        if (enabled != isEnabledNow) {
            if (enabled) {
                flags = flags or FLAG_HEADER_ENABLED
            } else {
                flags = flags and FLAG_HEADER_ENABLED.inv()
            }
            if ((flags and FLAG_LAYOUT_BUILT) != 0) {
                buildBubble(true)
            }
        }
    }

    // Destructor
    var isDestroyed: Boolean = false
        private set

    fun onDestroy() {
        isDestroyed = true
        stopHotTimer()
        if (forwardInfo != null) forwardInfo!!.destroy()
        if (replyData != null) replyData!!.performDestroy()
        messageReactions!!.performDestroy()
        setViewAttached(false)
        onMessageContainerDestroyed()
    }

    protected open fun onMessageContainerDestroyed() {
        // implement in children
    }

    // Selection
    private var highlightFactor = 0f
    private var selectionFactor = 0f
    private var selection: SelectionInfo? = null

    fun getSelectionColor(factor: Float): Int {
        val useBubbles = useBubbles()
        if (useBubbles) {
            return alphaColor(
                factor,
                fromToArgb(
                    Theme.getColor(ColorId.bubble_messageSelection),
                    Theme.getColor(ColorId.bubble_messageSelectionNoWallpaper),
                    manager.controller().wallpaper()!!.getBackgroundTransparency()
                )
            )
        } else {
            // manager.controller().wallpaper().getOverlayColor(ColorId.chatTransparentColor)
            val color = Theme.chatSelectionColor()
            return alphaColor(factor, compositeColor(Theme.getColor(ColorId.chatBackground), color))
        }
        /*final int color = useBubbles ? Theme.getColor(ColorId.messageBubbleSelection) : Theme.chatSelectionColor();
    return U.alphaColor(factor, useBubbles ? color : U.compositeColor(Theme.getColor(ColorId.chatPlainBackground), color));*/
    }

    private fun findBottomEdge(): Int {
        return height - this.extraPadding
    }

    private fun drawSelection(view: View, c: Canvas) {
        if (selectionFactor == 1f) {
            c.drawRect(0f, findTopEdge().toFloat(), view.getMeasuredWidth().toFloat(), findBottomEdge().toFloat(), Paints.fillingPaint(getSelectionColor(1f)))
            return
        }
        if (selection != null) {
            c.save()
            c.clipRect(0, selection!!.startY, view.getMeasuredWidth(), findBottomEdge())
            c.drawCircle(
                selection!!.pivotX + selection!!.diffX * selectionFactor,
                selection!!.pivotY + selection!!.diffY * selectionFactor,
                selection!!.expectedRadius * selectionFactor,
                Paints.fillingPaint(getSelectionColor(1f))
            )
            c.restore()
        }
    }

    fun onSelectableFactorChanged() {
        invalidate(true)
    }

    private fun setSelectionFactor(factor: Float) {
        if (this.selectionFactor != factor) {
            this.selectionFactor = factor
            invalidateParentOrSelf(false)
        }
    }

    fun findTopEdge(): Int {
        return this.headerPadding - (if ((flags and FLAG_HEADER_ENABLED) != 0 && !useBubbles()) xHeaderPadding else 0)
    }

    private var selectedMessageIds: LongList? = null
    private var selectionAnimators: LongSparseArray<FactorAnimator>? = null

    val isCompletelySelected: Boolean
        get() {
            synchronized(this) {
                val selectedCount = if (selectedMessageIds != null) selectedMessageIds!!.size() else 0
                if (selectedCount == 0) {
                    return false
                }
                val combinedCount = if (this.combinedMessagesUnsafely != null) combinedMessagesUnsafely!!.size else 1
                return combinedCount > 0 && selectedCount >= combinedCount
            }
        }

    fun hasSelectedMessages(): Boolean {
        synchronized(this) {
            return selectedMessageIds != null && selectedMessageIds!!.size() > 0
        }
    }

    fun shouldApplySelectionFully(): Boolean {
        synchronized(this) {
            return this.combinedMessagesUnsafely == null || combinedMessagesUnsafely!!.size <= 1
        }
    }

    protected fun findSelectionAnimator(messageId: Long): FactorAnimator? {
        synchronized(this) {
            if (selectionAnimators != null) {
                return selectionAnimators!!.get(messageId)
            }
        }
        return null
    }

    protected open fun onAnimatorAttachedToMessage(messageId: Long, animator: FactorAnimator?) {
        // override
    }

    protected open fun onMessageSelectionChanged(messageId: Long, selectionFactor: Float, needInvalidate: Boolean) {}

    fun getSelectionFactor(childAnimator: FactorAnimator?): Float {
        return if (childAnimator == null || (childAnimator.getIntValue() and ANIMATION_FLAG_IGNORE_CHILD).toFloat() != 0f || shouldApplySelectionFully()) 0f else childAnimator.getFactor()
    }

    fun findMessageIdUnder(x: Float, y: Float): Long {
        var x = x
        x -= getSelectableContentOffset(manager.getSelectableFactor()).toFloat()
        return findChildMessageIdUnder(x, y)
    }

    protected open fun findChildMessageIdUnder(x: Float, y: Float): Long {
        return 0
    }

    private fun forceCancelGlobalAnimation(reset: Boolean) {
        if (selectedMessageIds != null) {
            val size = selectionAnimators!!.size()
            for (i in 0..<size) {
                val animator = selectionAnimators!!.valueAt(i)
                if (animator != null && animator.getIntFlag(ANIMATION_FLAG_AFFECTS_CONTAINER)) {
                    animator.setIntFlag(ANIMATION_FLAG_AFFECTS_CONTAINER, false)
                }
            }
            if (reset) {
                setSelectionFactor(0f)
            }
        }
    }

    private fun forceRemoveAnimation(messageId: Long) {
        if (selectedMessageIds != null) {
            selectedMessageIds!!.remove(messageId)

            forceCancelGlobalAnimation(false)

            val totalCount = if (this.combinedMessagesUnsafely != null) combinedMessagesUnsafely!!.size else 1
            val needGlobalSelection = totalCount == selectedMessageIds!!.size()
            val animate = needGlobalSelection && currentViews.hasAnyTargetToInvalidate()

            val i = selectionAnimators!!.indexOfKey(messageId)
            if (i >= 0) {
                val animator = selectionAnimators!!.valueAt(i)
                if (animate) {
                    animator.setIntFlag(ANIMATION_FLAG_IGNORE_CHILD, true)
                    animator.forceFactor(selectionFactor)
                    animator.animateTo(1f)
                } else if (animator.getIntFlag(ANIMATION_FLAG_AFFECTS_SELECTABLE)) {
                    selectionAnimators!!.removeAt(i)
                } else {
                    animator.cancel()
                    selectionAnimators!!.removeAt(i)
                }
            }

            if (needGlobalSelection && !animate) {
                setSelectionFactor(1f)
            }
        }
    }

    private fun prepareSelectionAnimation(pivotX: Float, pivotY: Float): Boolean {
        var pivotX = pivotX
        var pivotY = pivotY
        val viewWidth = currentViews.getMeasuredWidth()
        val view = findCurrentView()

        if (viewWidth == 0 || view == null || !currentViews.hasAnyTargetToInvalidate() || view.getParent() == null) {
            return false
        }

        val viewHeight = view.getMeasuredHeight()
        if (viewHeight > (view.getParent() as View).getMeasuredHeight() * 2.5f) {
            return false
        }

        val startY = findTopEdge()
        val endX = viewWidth
        val endY = findBottomEdge()

        val targetX = (endX.toFloat() * .5f).toInt().toFloat()
        val targetY = (startY + ((endY - startY).toFloat() * .5f).toInt()).toFloat()

        if (pivotX == -1f && pivotY == -1f) {
            pivotX = targetX
            pivotY = targetY
        }

        val diffX = targetX - pivotX
        val diffY = targetY - pivotY

        val circleHeight = (endY - startY).toFloat()
        val radius = sqrt((endX * endX + circleHeight * circleHeight).toDouble()).toFloat() * .5f

        if (selection == null) {
            selection = SelectionInfo(startY, pivotX, pivotY, diffX, diffY, radius)
        } else {
            selection!!.startY = startY
            selection!!.pivotX = pivotX
            selection!!.pivotY = pivotY
            selection!!.diffX = diffX
            selection!!.diffY = diffY
            selection!!.expectedRadius = radius
        }

        return true
    }

    interface SelectableDelegate {
        fun onSelectableModeChanged(isSelectable: Boolean, animator: FactorAnimator?)
        fun onSelectableFactorChanged(factor: Float, callee: FactorAnimator?)
    }

    /*public interface CommonDelegate {
    boolean isMessageSelected (long chatId, long messageId, TGMessage container);
    float getSelectableFactor ();
  }*/
    // private @Nullable CommonDelegate commonDelegate;
    private fun initSelectedMessages() {
        var ids: LongList? = null
        synchronized(this) {
            if (this.combinedMessagesUnsafely != null && !combinedMessagesUnsafely!!.isEmpty()) {
                var skippedCount = 0
                for (message in this.combinedMessagesUnsafely) {
                    if (manager.isMessageSelected(message.chatId, message.id, this)) {
                        if (ids == null) {
                            ids = LongList(combinedMessagesUnsafely!!.size - skippedCount)
                        }
                        ids.append(message.id)
                    } else {
                        skippedCount++
                    }
                }
            }
        }
        if (ids != null) {
            val size = ids.size()
            for (i in 0..<size) {
                val messageId = ids.get(i)
                setSelected(messageId, true, false, -1f, -1f, null)
            }
        } else if (manager.isMessageSelected(msg!!.chatId, msg!!.id, this)) {
            setSelected(msg!!.id, true, false, -1f, -1f, null)
        }
    }

    fun setSelected(messageId: Long, isSelected: Boolean, animated: Boolean, pivotX: Float, pivotY: Float, selectable: SelectableDelegate?) {
        var animated = animated
        if (isSelected && !canBeSelected()) {
            return
        }

        val affectsSelectable = selectable != null && useBubbles()
        var targetAnimator: FactorAnimator?
        val hasGlobalEffect: Boolean

        synchronized(this) {
            val totalCount = if (this.combinedMessagesUnsafely != null && !combinedMessagesUnsafely!!.isEmpty()) combinedMessagesUnsafely!!.size else 1
            if (isSelected) {
                if (selectedMessageIds == null) {
                    selectedMessageIds = LongList(totalCount)
                } else if (selectedMessageIds!!.contains(messageId)) {
                    return
                }
                selectedMessageIds!!.append(messageId)
                hasGlobalEffect = totalCount == selectedMessageIds!!.size()
            } else {
                if (selectedMessageIds == null || !selectedMessageIds!!.remove(messageId)) {
                    return
                }
                hasGlobalEffect = totalCount - 1 == selectedMessageIds!!.size()
            }

            if (selectionAnimators != null) {
                targetAnimator = selectionAnimators!!.get(messageId)
            } else {
                targetAnimator = null
                selectionAnimators = LongSparseArray<FactorAnimator>()
            }
            if (targetAnimator == null) {
                targetAnimator = FactorAnimator(ANIMATOR_SELECT, this, DECELERATE_INTERPOLATOR, 200L)
                targetAnimator.setLongValue(messageId)
                selectionAnimators!!.put(messageId, targetAnimator)
                onAnimatorAttachedToMessage(messageId, targetAnimator)
            }
            targetAnimator.setIntFlag(ANIMATION_FLAG_AFFECTS_CONTAINER, hasGlobalEffect)
            targetAnimator.setIntFlag(ANIMATION_FLAG_AFFECTS_SELECTABLE, affectsSelectable)
            targetAnimator.setObjValue(if (affectsSelectable) selectable else null)
            if (affectsSelectable) {
                selectable.onSelectableModeChanged(isSelected, targetAnimator)
            }
        }

        if (hasGlobalEffect) {
            animated = animated && prepareSelectionAnimation(pivotX, pivotY)
        }
        if (animated) {
            animated = currentViews.hasAnyTargetToInvalidate()
        }

        targetAnimator!!.setIntFlag(ANIMATION_FLAG_IGNORE_SELF, !animated && affectsSelectable)

        val toFactor = if (isSelected) 1f else 0f

        if (animated || affectsSelectable) {
            targetAnimator.animateTo(toFactor)
        } else {
            targetAnimator.forceFactor(toFactor)
        }
        if (!animated && hasGlobalEffect) {
            setSelectionFactor(toFactor)
        }
    }

    // Animations
    override fun needAnimateChanges(counter: Counter?): Boolean {
        return needAnimateChanges()
    }

    override fun onCounterAppearanceChanged(counter: Counter?, sizeChanged: Boolean) {
        if ((sizeChanged || (counter === reactionsCounter && !useBubbles())) && hasFlag(flags, FLAG_LAYOUT_BUILT)) {
            if (counter === viewCounter) {
                when (this.viewCountMode) {
                    VIEW_COUNT_FORWARD -> buildForward()
                    VIEW_COUNT_MAIN -> if (useBubbles() || (flags and FLAG_HEADER_ENABLED) != 0) {
                        layoutInfo()
                    }
                }
            } else if (counter === replyCounter || counter === shareCounter || counter === shrinkedReactionsCounter || counter === isPinned || counter === isTranslatedCounter) {
                if (useBubbles() || (flags and FLAG_HEADER_ENABLED) != 0) {
                    layoutInfo()
                }
            } else if (counter === reactionsCounter) {
                layoutInfo()
            }
        }
        if (counter === isTranslatedCounter && isTranslatedCounter.getVisibility() == 1f) {
            val force = !useBubbles() && Settings.instance().needTutorial(Settings.TUTORIAL_SELECT_LANGUAGE_INLINE_MODE)
            if (force) {
                Settings.instance().markTutorialAsShown(Settings.TUTORIAL_SELECT_LANGUAGE_INLINE_MODE)
            }
            checkSelectLanguageWarning(force)
        }
        if (UI.inUiThread()) { // FIXME remove this after reworking combineWith method
            invalidate()
        } else {
            postInvalidate()
        }
    }

    override fun onSizeChanged() {
        if (UI.inUiThread()) { // FIXME remove this after reworking combineWith method
            invalidate()
        } else {
            postInvalidate()
        }
    }

    override fun onInvalidateMedia(avatars: TGAvatars?) {
        performWithViews(RunnableData { view: MessageView? -> requestReactionsResources(view!!.getReactionAvatarsReceiver(), true) })
    }

    override fun onFactorChanged(id: Int, factor: Float, fraction: Float, callee: FactorAnimator?) {
        if (id >= 0) {
            onChildFactorChanged(id, factor, fraction)
        } else {
            when (id) {
                ANIMATOR_REVOKE -> {
                    setHighlight(1f - factor)
                }

                ANIMATOR_DISMISS -> {
                    this.dismiss = factor
                }

                ANIMATOR_READY_LEFT -> {
                    if (this.quickLeftReadyFactor != factor) {
                        this.quickLeftReadyFactor = factor
                        invalidate()
                    }
                }

                ANIMATOR_READY_RIGHT -> {
                    if (this.quickRightReadyFactor != factor) {
                        this.quickRightReadyFactor = factor
                        invalidate()
                    }
                }

                ANIMATOR_QUICK_VERTICAL_LEFT -> {
                    if (this.leftActionVerticalFactor != factor) {
                        this.leftActionVerticalFactor = factor
                        invalidate()
                    }
                }

                ANIMATOR_QUICK_VERTICAL_RIGHT -> {
                    if (this.rightActionVerticalFactor != factor) {
                        this.rightActionVerticalFactor = factor
                        invalidate()
                    }
                }

                ANIMATOR_SELECT -> {
                    val messageId = callee!!.getLongValue()
                    val intValue = callee.getIntValue()
                    val hasGlobalEffect = (intValue and ANIMATION_FLAG_AFFECTS_CONTAINER) != 0
                    val ignoreSelf = (intValue and ANIMATION_FLAG_IGNORE_SELF) != 0
                    if (!ignoreSelf) {
                        if ((intValue and ANIMATION_FLAG_IGNORE_CHILD) == 0) {
                            onMessageSelectionChanged(messageId, factor, !hasGlobalEffect || this.selectionFactor == factor)
                        }
                        if (hasGlobalEffect) {
                            setSelectionFactor(factor)
                        }
                    }
                    if ((intValue and ANIMATION_FLAG_AFFECTS_SELECTABLE) != 0) {
                        val delegate = callee.getObjValue() as SelectableDelegate?
                        if (delegate != null) {
                            delegate.onSelectableFactorChanged(factor, callee)
                        }
                    }
                }
            }
        }
    }

    override fun onFactorChangeFinished(id: Int, finalFactor: Float, callee: FactorAnimator?) {
        if (id >= 0) {
            onChildFactorChangeFinished(id, finalFactor, callee)
        }
    }

    protected open fun onChildFactorChanged(id: Int, factor: Float, fraction: Float) {
        // override
    }

    protected open fun onChildFactorChangeFinished(id: Int, finalFactor: Float, callee: FactorAnimator?) {
        // override
    }

    // Highlight
    fun drawHighlight(view: View, c: Canvas) {
        if (highlightFactor != 0f) {
            c.drawRect(
                0f,
                findTopEdge().toFloat(),
                view.getMeasuredWidth().toFloat(),
                findBottomEdge().toFloat(),
                Paints.fillingPaint(getSelectionColor(highlightFactor))
            )
        }
    }

    fun highlight(revoke: Boolean) {
        cancelHighlightRevoke()
        setHighlight(1f)
        if (revoke) {
            revokeHighlight()
        }
    }

    private fun setHighlight(highlight: Float) {
        if (this.highlightFactor != highlight) {
            this.highlightFactor = highlight
            invalidateParentOrSelf(false)
        }
    }

    private var highlightedText: String? = null
    private val searchResultsHighlightPool = Highlight.Pool()

    private fun setHighlightedText(text: String?) {
        if (!text.equalsOrBothEmpty(highlightedText)) {
            searchResultsHighlightPool.clear()
            this.highlightedText = text
            onUpdateHighlightedText()
            invalidate()
        }
    }

    fun getHighlightedText(key: Int, `in`: String?): Highlight? {
        val highlight = Highlight.valueOf(`in`, highlightedText)
        if (highlight == null) return null

        highlight.setCustomColorSet(this.searchHighlightColorSet)
        val mostRelevantKey = searchResultsHighlightPool.getMostRelevantHighlightKey()
        searchResultsHighlightPool.add(key, highlight)
        if (mostRelevantKey != Highlight.Pool.KEY_NONE && mostRelevantKey != searchResultsHighlightPool.getMostRelevantHighlightKey()) {
            UI.post(Runnable { this.onUpdateHighlightedText() })
            Log.i("HIGHLIGHT", "UPDATE")
        }

        return if (searchResultsHighlightPool.isMostRelevant(key)) highlight else null
    }

    fun checkHighlightedText() {
        if (messagesController().isMessageFound(getMessage(this.smallestId)!!)) {
            setHighlightedText(manager.controller().getLastMessageSearchQuery())
        } else {
            setHighlightedText(null)
        }
    }

    // Override
    protected open fun onUpdateHighlightedText() {
    }

    private var revokeAnimator: FactorAnimator? = null
    private fun cancelHighlightRevoke() {
        if (revokeAnimator != null) {
            revokeAnimator!!.cancel()
            revokeAnimator = null
        }
    }

    fun revokeHighlight() {
        if (revokeAnimator != null && revokeAnimator!!.isAnimating()) {
            return
        }
        if (revokeAnimator == null) {
            revokeAnimator = FactorAnimator(ANIMATOR_REVOKE, this, DECELERATE_INTERPOLATOR, 400L, 0f)
            revokeAnimator!!.setStartDelay(2000L)
        } else {
            revokeAnimator!!.forceFactor(0f)
        }
        revokeAnimator!!.animateTo(1f)
    }

    // Translation
    private var translationOption = 0f
    var translation: Float = 0f
        private set
    private var dismissFactor = 0f

    fun resetTransformState() {
        if (highlightFactor == 1f) {
            revokeHighlight()
        }
    }

    open fun canSwipe(): Boolean {
        return ((flags and FLAG_IGNORE_SWIPE) == 0)
    }

    fun normalizeTranslation(view: View?, after: Runnable?, needDelay: Boolean) {
        if (translation == 0f) {
            return
        }

        if (after != null && ((flags and FLAG_READY_QUICK_RIGHT) == 0) && (flags and FLAG_READY_QUICK_LEFT) == 0) {
            vibrate()
        }

        if (!needDelay) {
            U.run(after)
        }
        if (quickLeftReadyAnimator != null) {
            quickLeftReadyAnimator!!.cancel()
        }
        if (quickRightReadyAnimator != null) {
            quickRightReadyAnimator!!.cancel()
        }
        if (leftActionVerticalAnimator != null) {
            leftActionVerticalAnimator!!.cancel()
        }
        if (rightActionVerticalAnimator != null) {
            rightActionVerticalAnimator!!.cancel()
        }

        val status = if (needDelay && after != null) BooleanArray(1) else null
        val fromTranslation = translation
        val fromTranslationOption = translationOption
        val fromReplyReadyFactor = quickRightReadyFactor
        val fromShareReadyFactor = quickLeftReadyFactor
        flags = flags or FLAG_IGNORE_SWIPE
        val animator = FactorAnimator(0, object : FactorAnimator.Target {
            override fun onFactorChanged(id: Int, factor: Float, fraction: Float, callee: FactorAnimator?) {
                if (needDelay && fraction >= .85f && after != null && !status!![0]) {
                    status[0] = true
                    U.run(after)
                }
                setReadyFactor(false, fromReplyReadyFactor * (1f - factor), false, true)
                setReadyFactor(true, fromShareReadyFactor * (1f - factor), false, true)
                translate(fromTranslation * (1f - factor), fromTranslationOption, false)
                if (view is MessageViewGroup) {
                    view.setSwipeTranslation(translation)
                }
            }

            override fun onFactorChangeFinished(id: Int, finalFactor: Float, callee: FactorAnimator?) {
                setQuickActionVerticalFactor(false, 0f, false, true)
                setQuickActionVerticalFactor(true, 0f, false, true)
                translate(0f, 0f, false)
                flags = flags and FLAG_IGNORE_SWIPE.inv()
                /*if (needDelay) {
          U.run(after);
        }*/
            }
        }, DECELERATE_INTERPOLATOR, MessagesRecyclerView.ITEM_ANIMATOR_DURATION + 50L)
        animator.animateTo(1f)
    }

    fun vibrate() {
        UI.hapticVibrate(findCurrentView(), true)
    }

    private var quickRightReadyFactor = 0f
    private var quickLeftReadyFactor = 0f
    private var quickRightReadyAnimator: FactorAnimator? = null
    private var quickLeftReadyAnimator: FactorAnimator? = null

    private var leftActionVerticalFactor = 0f
    private var rightActionVerticalFactor = 0f
    private var leftActionVerticalAnimator: FactorAnimator? = null
    private var rightActionVerticalAnimator: FactorAnimator? = null

    private fun setQuickActionVerticalFactor(isLeft: Boolean, factor: Float, animated: Boolean, invalidate: Boolean) {
        var verticalAnimator = if (isLeft) leftActionVerticalAnimator else rightActionVerticalAnimator
        val verticalFactor = if (isLeft) leftActionVerticalFactor else rightActionVerticalFactor
        if (animated) {
            if (verticalAnimator == null) {
                if (verticalFactor == factor) {
                    return
                }
                verticalAnimator = FactorAnimator(
                    if (isLeft) ANIMATOR_QUICK_VERTICAL_LEFT else ANIMATOR_QUICK_VERTICAL_RIGHT,
                    this,
                    DECELERATE_INTERPOLATOR,
                    if (useBubbles()) 110L else 220L,
                    verticalFactor
                )
                if (isLeft) {
                    leftActionVerticalAnimator = verticalAnimator
                } else {
                    rightActionVerticalAnimator = verticalAnimator
                }
            }
            if (verticalAnimator.getToFactor() == factor) {
                return
            }
            verticalAnimator.animateTo(factor)
            vibrate()
        } else {
            if (verticalAnimator != null) {
                verticalAnimator.forceFactor(factor)
            }
            if (verticalFactor != factor) {
                if (isLeft) {
                    leftActionVerticalFactor = factor
                } else {
                    rightActionVerticalFactor = factor
                }
                if (invalidate) {
                    invalidate()
                }
            }
        }
    }

    private fun setReadyFactor(isLeft: Boolean, factor: Float, animated: Boolean, invalidate: Boolean) {
        var readyAnimator = if (isLeft) quickLeftReadyAnimator else quickRightReadyAnimator
        val readyFactor = if (isLeft) quickLeftReadyFactor else quickRightReadyFactor
        if (animated) {
            if (readyAnimator == null) {
                if (readyFactor == factor) {
                    return
                }
                readyAnimator = FactorAnimator(if (isLeft) ANIMATOR_READY_LEFT else ANIMATOR_READY_RIGHT, this, DECELERATE_INTERPOLATOR, 110L, readyFactor)
                if (isLeft) {
                    quickLeftReadyAnimator = readyAnimator
                } else {
                    quickRightReadyAnimator = readyAnimator
                }
            }
            readyAnimator.animateTo(factor)
        } else {
            if (readyAnimator != null) {
                readyAnimator.forceFactor(factor)
            }
            if (readyFactor != factor) {
                if (isLeft) {
                    this.quickLeftReadyFactor = factor
                } else {
                    this.quickRightReadyFactor = factor
                }
                if (invalidate) {
                    invalidate()
                }
            }
        }
    }

    fun translate(dx: Float, verticalPosition: Float, bySwipe: Boolean) {
        var dx = dx
        if (bySwipe && ((flags and FLAG_IGNORE_SWIPE) != 0)) {
            return
        }

        val useBubbles = useBubbles()

        if (useBubbles) {
            val bound = Screen.dp(BUBBLE_MOVE_MAX)
            dx = max(-bound.toFloat(), min(bound.toFloat(), dx))
        }

        if (translation == dx && translationOption == verticalPosition) {
            return
        }

        translation = dx
        context().reactionsOverlayManager().invalidate()
        translationOption = verticalPosition
        if (translation == 0f) {
            translationOption = 0f
            setQuickActionVerticalFactor(true, 0f, false, true)
            setQuickActionVerticalFactor(false, 0f, false, true)
        } else {
            setQuickActionVerticalFactor(dx >= 0, Math.round(translationOption).toFloat(), true, true)
        }

        if (useBubbles) {
            if (dx >= 0) {
                flags = flags and FLAG_READY_QUICK_RIGHT.inv()
                setReadyFactor(false, 0f, false, false)
            }
            if (dx <= 0) {
                flags = flags and FLAG_READY_QUICK_LEFT.inv()
                setReadyFactor(true, 0f, false, false)
            }
            val threshold = Screen.dp(BUBBLE_MOVE_THRESHOLD)
            if (dx >= threshold && (flags and FLAG_READY_QUICK_LEFT) == 0) {
                flags = flags or FLAG_READY_QUICK_LEFT
                vibrate()
                setReadyFactor(true, 1f, true, true)
            } else if (dx <= -threshold && (flags and FLAG_READY_QUICK_RIGHT) == 0) {
                flags = flags or FLAG_READY_QUICK_RIGHT
                vibrate()
                setReadyFactor(false, 1f, true, true)
            }
        }

        invalidate(true)
    }

    fun getQuickActionVerticalFactor(isLeft: Boolean): Float {
        return (if (isLeft) leftActionVerticalFactor else rightActionVerticalFactor) + getQuickDefaultPosition(isLeft)
    }

    fun drawTranslate(view: View, c: Canvas) {
        val x: Float
        val quickColor: Int
        if (translation == 0f) {
            val alpha = (255f * abs(dismissFactor)).toInt()
            x = sign(dismissFactor) * view.getMeasuredWidth()
            quickColor = color(alpha, Theme.chatQuickActionColor())
            mQuickText!!.setAlpha(alpha)
        } else {
            x = this.translation
            quickColor = Theme.chatQuickActionColor()
            mQuickText!!.setAlpha(255)
        }

        val isLeft = x > 0
        val actions = if (isLeft) this.leftQuickReactions else this.rightQuickReactions
        val verticalFactor = getQuickActionVerticalFactor(isLeft)
        val readyFactor = if (isLeft) quickLeftReadyFactor else quickRightReadyFactor

        if (useBubbles()) {
            if (translation == 0f) {
                return
            }

            val height = bottomContentEdge - topContentEdge
            val shrinkFactor = 0.5f // 1f - MathUtils.clamp( ((float)(height - Screen.dp(40))) / Screen.dp(75));
            val shrinkSize = (Screen.dp(8f) * shrinkFactor).toInt()
            val offset = Screen.dp(32f) - shrinkSize
            val positionOffset = -(verticalFactor * offset).toInt()

            val startY = topContentEdge + (bottomContentEdge - topContentEdge) / 2 + positionOffset
            val cx = if (translation > 0f) translation / 2 else view.getMeasuredWidth() + translation / 2
            for (a in actions.indices) {
                val action = actions.get(a)
                val positionFactor = getTranslatePositionFactor(isLeft, a)
                val cy = (startY + offset * a).toFloat()
                val closenessToBorder = 1f - clamp(min(cy - topContentEdge, bottomContentEdge - cy) / Screen.dp(12f))
                val maxAlpha = max(positionFactor, 1f - closenessToBorder)
                val ncx = cx + shrinkSize * (1f - positionFactor)
                val icon = if (!action.isQuickReaction || nextSetReactionAnimation == null) action.icon else null
                drawTranslateRound(c, ncx, cy, readyFactor, maxAlpha, positionFactor, icon)
            }
            return
        }

        val startY: Int
        val endY: Int
        if (useBubbles()) {
            startY = topContentEdge
            endY = bottomContentEdge
        } else {
            startY = this.headerPadding - xHeaderPadding
            endY = findBottomEdge()
        }
        val height = endY - startY
        val rtl = Lang.rtl()
        val textWidth: Float
        if (rtl != (x > 0)) {
            textWidth = xQuickShareWidth.toFloat()
        } else {
            textWidth = xQuickReplyWidth.toFloat()
        }

        val positionOffset = -(verticalFactor * height).toInt()
        val restoreToCount = Views.save(c)
        c.clipRect(0, startY, view.getMeasuredWidth(), endY)
        for (a in actions.indices) {
            val action = actions.get(a)
            val positionFactor = getTranslatePositionFactor(x > 0, a)
            val icon = if (!action.isQuickReaction || nextSetReactionAnimation == null) action.icon else null
            drawTranslateRect(
                c,
                x, (startY + positionOffset + height * a).toFloat(),
                view.getMeasuredWidth(), height, positionFactor,
                quickColor, icon, action.text, textWidth.toInt(),
                if (height > Screen.dp(256f)) (mInitialTouchY - this.headerPadding + xHeaderPadding).toInt() else (height / 2)
            )
        }
        Views.restore(c, restoreToCount)
    }

    private fun getTranslatePositionFactor(isLeft: Boolean, position: Int): Float {
        return (1f - (clamp(abs(getQuickActionVerticalFactor(isLeft) - position))))
    }

    private fun drawTranslateRound(c: Canvas, cx: Float, cy: Float, readyFactor: Float, maxAlpha: Float, positionFactor: Float, icon: Drawable?) {
        val threshold = Screen.dp(BUBBLE_MOVE_THRESHOLD).toFloat()

        val alpha = min(clamp(min(abs(translation / threshold), .6f + 4f * positionFactor)) * readyFactor, maxAlpha)
        val scale = (.6f + .4f * min(readyFactor, positionFactor))
        if (alpha == 0f) {
            return
        }

        val darkFactor = Theme.getDarkFactor() * (1f - manager.controller().wallpaper()!!.getBackgroundTransparency())
        val radius = Screen.dp(16f) * scale
        val radius2 = Screen.dp(15.5f) * scale

        if (darkFactor > 0f) {
            c.drawCircle(cx, cy, radius, Paints.getProgressPaint(alphaColor(alpha * darkFactor, Theme.headerColor()), Screen.dp(1f).toFloat()))
        }
        c.drawCircle(cx, cy, radius, Paints.strokeSmallPaint(alphaColor(alpha * 0.05f, this.bubbleButtonTextColor)))
        c.drawCircle(cx, cy, radius2, Paints.fillingPaint(alphaColor(alpha, this.bubbleButtonBackgroundColor)))

        if (icon != null) {
            val restoreToCount = Views.save(c)
            c.scale((if (Lang.rtl()) -.8f else .8f) * scale, .8f * scale, cx, cy)
            icon.setAlpha((alpha * 255).toInt())
            val paint = Paints.getInlineBubbleIconPaint(alphaColor(alpha, this.bubbleButtonTextColor))
            Drawables.draw(c, icon, cx - icon.getMinimumWidth() / 2f, cy - icon.getMinimumHeight() / 2f, paint)
            Views.restore(c, restoreToCount)
        }
    }

    private fun drawTranslateRect(
        c: Canvas,
        x: Float,
        y: Float,
        width: Int,
        height: Int,
        positionFactor: Float,
        quickColor: Int,
        icon: Drawable?,
        text: String,
        textWidth: Int,
        textY: Int
    ) {
        val iconPaint = Paints.getInlineBubbleIconPaint(
            alphaColor(
                mQuickText!!.getAlpha().toFloat() / 255f,
                Theme.getColor(ColorId.messageSwipeContent)
            )
        )

        val iconWidth = if (icon != null) icon.getMinimumWidth() else 0
        val iconHeight = if (icon != null) icon.getMinimumHeight() else 0

        val check = Screen.dp(12f) + iconHeight
        val iconX: Float
        val textX: Float
        val cy = (y + (height.toFloat() * .5f).toInt()).toInt()
        val rtl = Lang.rtl()

        if (x > 0) {
            c.drawRect(0f, y, x, y + height, Paints.fillingPaint(alphaColor(positionFactor, quickColor)))
            iconX = x - xQuickPadding - iconWidth
            textX = iconX - xQuickTextPadding - textWidth
        } else {
            val endX = width
            c.drawRect(endX + x, y, endX.toFloat(), y + height, Paints.fillingPaint(alphaColor(positionFactor, quickColor)))
            iconX = endX + x + xQuickPadding
            textX = iconX + iconWidth + textWidth
        }
        if (check > height) {
            c.save()
            val scale = height.toFloat() / check.toFloat()
            c.scale(scale, scale, iconX, cy.toFloat())
        }
        if (rtl) {
            c.save()
            c.scale(-1f, 1f, iconX + iconWidth / 2f, cy.toFloat())
        }
        if (icon != null) {
            icon.setAlpha(255)
            Drawables.draw(c, icon, iconX, y + textY - (iconHeight.toFloat() * .5f).toInt() + Screen.dp(if (x > 0) 0f else 0.5f), iconPaint)
        }
        if (rtl) {
            c.restore()
        }
        c.drawText(text, textX, y + textY + xQuickTextOffset, mQuickText!!)
        if (check > height) {
            c.restore()
        }
    }

    private var dismissAnimator: FactorAnimator? = null

    private fun animateDismiss(startFactor: Float, toFactor: Float) {
        if (dismissAnimator != null) {
            dismissAnimator!!.forceFactor(startFactor)
        }
        this.dismiss = startFactor
        if (dismissAnimator == null) {
            if (dismissFactor == toFactor) {
                return
            }
            dismissAnimator =
                FactorAnimator(ANIMATOR_DISMISS, this, DECELERATE_INTERPOLATOR, MessagesRecyclerView.ITEM_ANIMATOR_DURATION + 60L, this.dismissFactor)
        } else if (!dismissAnimator!!.isAnimating() && dismissFactor == toFactor) {
            return
        }
        dismissAnimator!!.animateTo(toFactor)
    }

    var dismiss: Float
        get() = dismissFactor
        set(dismiss) {
            if (this.dismissFactor != dismiss) {
                this.dismissFactor = dismiss
                if (dismiss == 0f) {
                    translationOption = 0f
                    setQuickActionVerticalFactor(true, 0f, false, true)
                    setQuickActionVerticalFactor(false, 0f, false, true)
                }
                invalidateParentOrSelf(true)
            }
        }

    fun currentActualHeight(): Int {
        return height - this.headerPadding + (if (useBubbles()) 0 else xHeaderPadding)
    }

    fun completeTranslation() {
        val startDismiss = sign(translation)
        translation = 0f
        swipeHelper.reset()

        animateDismiss(startDismiss, 0f)
    }

    private class SelectionInfo(var startY: Int, var pivotX: Float, var pivotY: Float, var diffX: Float, var diffY: Float, var expectedRadius: Float)

    // Chat events
    private var event: MessageChatEvent? = null
    private var eventMessageId: Long = 0

    fun setIsEventLog(event: MessageChatEvent?, messageId: Long): TGMessage {
        flags = flags or FLAG_EVENT_LOG
        this.event = event
        this.time = genTime()
        setDate(genDate())
        return this
    }

    @Suppress("WrongConstant")
    private fun needHideEventDate(): Boolean {
        return ((event != null) and event!!.hideDate) || (msg!!.content.getConstructor() == MessageChatEvent.CONSTRUCTOR && (msg!!.content as MessageChatEvent).hideDate)
    }

    @get:Suppress("WrongConstant")
    val isEventLog: Boolean
        get() = (flags and FLAG_EVENT_LOG) != 0 || msg!!.content.getConstructor() == MessageChatEvent.CONSTRUCTOR

    @set:JvmName("setIsThreadHeader") var isThreadHeader: Boolean
        get() = hasFlag(flags, MESSAGE_FLAG_IS_THREAD_HEADER)
        set(isThreadHeader) {
            this.flags = setFlag(flags, MESSAGE_FLAG_IS_THREAD_HEADER, isThreadHeader)
            updateInteractionInfo(false)
        }

    protected var footerTitle: String? = null
    protected var footerText: TextWrapper? = null

    protected fun hasFooter(): Boolean {
        return footerTitle != null && footerText != null
    }

    fun setFooter(title: String?, text: String?, entities: Array<TdApi.TextEntity?>?) {
        this.footerTitle = title

        val footerWrapper = TextWrapper(text, smallerTextStyleProvider, this.textColorSet)
            .setEntities(
                TextEntity.valueOf(tdlib, text, entities, openParameters()),
                TextWrapper.TextMediaListener { wrapper: TextWrapper?, text1: Text?, specificMedia: TextMedia? ->
                    if (footerText === wrapper) {
                        performWithViews(RunnableData { view: MessageView? ->
                            view!!.invalidateFooterTextMediaReceiver(this, text1!!, specificMedia)
                        })
                    }
                })
            .setClickCallback(clickCallback())
        if (useBubbles()) {
            footerWrapper.addTextFlags(Text.FLAG_ADJUST_TO_CURRENT_WIDTH)
        }
        if (Config.USE_NONSTRICT_TEXT_ALWAYS || !useBubbles()) {
            footerWrapper.addTextFlags(Text.FLAG_BOUNDS_NOT_STRICT)
        }
        footerWrapper.setViewProvider(currentViews)

        this.footerText = footerWrapper
    }

    protected val footerTop: Int
        get() = this.contentY + this.contentHeight + this.footerPaddingTop

    protected val footerHeight: Int
        get() = Screen.dp(22f) + footerText!!.getHeight() + Screen.dp(2f)

    protected val footerWidth: Int
        get() = max(trimmedFooterTitleWidth, footerText!!.getWidth()) + Screen.dp(10f)

    protected open val footerPaddingTop: Int
        get() = Screen.dp(4f)

    protected open val footerPaddingBottom: Int
        get() = Screen.dp(2f)

    private var trimmedFooterTitle: String? = null
    private var trimmedFooterTitleWidth = 0

    private fun buildFooter() {
        if (!hasFooter()) {
            return
        }

        val paddingLeft = Screen.dp(10f)
        var maxWidth = if (allowMessageHorizontalExtend()) this.realContentMaxWidth else getContentWidth()
        maxWidth -= paddingLeft
        if (useBubbles()) {
            maxWidth -= xBubblePadding + xBubblePaddingSmall
        } else {
            maxWidth -= Screen.dp(8f)
        }

        footerText!!.prepare(maxWidth)

        val textPaint: TextPaint = smallerTextStyleProvider.getBoldPaint()
        trimmedFooterTitle = TextUtils.ellipsize(footerTitle, textPaint, maxWidth.toFloat(), TextUtils.TruncateAt.END).toString()
        trimmedFooterTitleWidth = U.measureText(footerTitle, textPaint).toInt()
    }

    // Color Sets
    private fun pick(regular: TextColorSets.Regular, bubbleOut: BubbleOut?, bubbleIn: BubbleIn?): TextColorSet {
        return (if (useBubbles()) (if (this.isOutgoingBubble) bubbleOut else bubbleIn) else regular)!!
    }

    val decentColorSet: TextColorSet
        get() = pick(TextColorSets.Regular.LIGHT, BubbleOut.LIGHT, BubbleIn.LIGHT)

    val contentAccentColor: TdlibAccentColor?
        get() {
            if (true) {
                return null
            }
            if (needColoredNames()) {
                if (fAuthorNameAccentColor != null) {
                    return fAuthorNameAccentColor
                } else if (forwardInfo != null) {
                    return forwardInfo!!.getAuthorAccentColor()
                }
                if (hAuthorAccentColor != null) {
                    return hAuthorAccentColor
                } else {
                    return if (forceForwardOrImportInfo()) forwardInfo!!.getAuthorAccentColor() else sender.getAccentColor()
                }
            }
            return null
        }

    fun overrideWithAccent(colorSet: TextColorSet, onlyClickable: Boolean): TextColorSet {
        val accentColor = this.contentAccentColor
        if (accentColor != null) {
            return object : TextColorSetOverride(colorSet) {
                override fun defaultTextColor(): Int {
                    return if (onlyClickable) super.defaultTextColor() else accentColor.getNameColor()
                }

                override fun clickableTextColor(isPressed: Boolean): Int {
                    return accentColor.getNameColor()
                }

                override fun backgroundColor(isPressed: Boolean): Int {
                    return if (isPressed) alphaColor(.2f, accentColor.getNameColor()) else super.backgroundColor(false)
                }

                override fun backgroundColorId(isPressed: Boolean): Int {
                    val complexColor = accentColor.getNameComplexColor()
                    return Theme.extractColorValue(complexColor)
                }
            }
        }
        return colorSet
    }

    val linkColorSet: TextColorSet
        get() = overrideWithAccent(
            pick(TextColorSets.Regular.LINK, BubbleOut.LINK, BubbleIn.LINK),
            false
        )

    val textColorSet: TextColorSet
        get() = overrideWithAccent(
            pick(TextColorSets.Regular.NORMAL, BubbleOut.NORMAL, BubbleIn.NORMAL),
            true
        )

    val chatAuthorColorSet: TextColorSet
        get() = overrideWithAccent(
            pick(TextColorSets.Regular.MESSAGE_AUTHOR, BubbleOut.MESSAGE_AUTHOR, BubbleIn.MESSAGE_AUTHOR),
            false
        )

    val chatAuthorPsaColorSet: TextColorSet
        get() = pick(TextColorSets.Regular.MESSAGE_AUTHOR_PSA, BubbleOut.MESSAGE_AUTHOR_PSA, BubbleIn.MESSAGE_AUTHOR_PSA)

    val searchHighlightColorSet: TextColorSet
        get() = pick(
            TextColorSets.Regular.MESSAGE_SEARCH_HIGHLIGHT,
            BubbleOut.MESSAGE_SEARCH_HIGHLIGHT,
            BubbleIn.MESSAGE_SEARCH_HIGHLIGHT
        )

    @get:ColorInt
    val contentBackgroundColor: Int
        // Colors
        get() {
            if (useBubbles()) {
                return Theme.getColor(if (this.isOutgoingBubble) ColorId.bubbleOut_background else ColorId.bubbleIn_background)
            } else {
                return compositeColor(Theme.getColor(ColorId.chatBackground), getSelectionColor(selectionFactor))
            }
        }

    @PorterDuffColorId
    fun getDecentColorId(@ColorId defaultColorId: Int): Int {
        return if (useBubbles()) (if (this.isOutgoingBubble) ColorId.bubbleOut_time else ColorId.bubbleIn_time) else defaultColorId
    }

    @get:ColorId
    val progressColorId: Int
        get() = if (useBubbles()) (if (this.isOutgoingBubble) ColorId.bubbleOut_progress else ColorId.bubbleIn_progress) else ColorId.progress

    val progressColor: Int
        get() = Theme.getColor(this.progressColorId)

    @get:ColorId
    val decentColorId: Int
        get() = getDecentColorId(ColorId.textLight)

    @get:ColorId
    val separatorColorId: Int
        get() = if (useBubbles()) (if (this.isOutgoingBubble) ColorId.bubbleOut_separator else ColorId.bubbleIn_separator) else ColorId.separator

    @get:ColorId
    val pressColorId: Int
        get() = if (useBubbles()) (if (this.isOutgoingBubble) ColorId.bubbleOut_pressed else ColorId.bubbleIn_pressed) else ColorId.messageSelection

    @get:PorterDuffColorId
    val decentIconColorId: Int
        get() = getDecentColorId(ColorId.iconLight)

    @get:ColorInt
    val decentColor: Int
        get() = Theme.getColor(this.decentColorId)

    @get:ColorInt
    val separatorColor: Int
        get() = Theme.getColor(this.separatorColorId)

    @get:ColorInt
    val decentIconColor: Int
        get() = Theme.getColor(this.decentIconColorId)

    val decentIconPaint: Paint
        get() = if (useBubbles()) (if (this.isOutgoingBubble) Paints.getBubbleOutTimePaint() else Paints.getBubbleInTimePaint()) else Paints.getIconLightPorterDuffPaint()

    val textColor: Int
        get() = Theme.getColor(if (useBubbles()) (if (this.isOutgoingBubble) ColorId.bubbleOut_text else ColorId.bubbleIn_text) else ColorId.text)

    val outlineColor: Int
        get() = Theme.getColor(if (useBubbles()) (if (this.isOutgoingBubble) ColorId.bubbleOut_outline else ColorId.bubbleIn_outline) else ColorId.separator)

    val textLinkColor: Int
        get() = Theme.getColor(if (useBubbles()) (if (this.isOutgoingBubble) ColorId.bubbleOut_textLink else ColorId.bubbleIn_textLink) else ColorId.textLink)

    val textLinkHighlightColor: Int
        get() = Theme.getColor(if (useBubbles()) (if (this.isOutgoingBubble) ColorId.bubbleOut_textLinkPressHighlight else ColorId.bubbleIn_textLinkPressHighlight) else ColorId.textLinkPressHighlight)

    protected val textTopOffset: Int
        get() = if ((!needHeader() || !needName()) && (!useForward() || ((flags and FLAG_SELF_CHAT) != 0 && !this.isOutgoing)) && replyData == null) 0 else -Screen.dp(
            if (useBubbles()) 4f else 2f
        )

    val verticalLineColor: Int
        get() {
            if (this.isOutgoingBubble) {
                return Theme.getColor(ColorId.bubbleOut_chatVerticalLine)
            }
            val accentColor = this.contentAccentColor
            if (accentColor != null) {
                return accentColor.getVerticalLineColor()
            }
            return Theme.getColor(ColorId.messageVerticalLine)
        }

    protected val verticalLineContentColor: Int
        get() = Theme.getColor(if (this.isOutgoingBubble) ColorId.bubbleOut_chatNeutralFillingContent else ColorId.messageNeutralFillingContent)

    protected fun getCorrectLineColor(isPersonal: Boolean): Int {
        return Theme.getColor(
            if (isPersonal) if (this.isOutgoingBubble) ColorId.bubbleOut_chatCorrectChosenFilling else ColorId.messageCorrectChosenFilling else if (this.isOutgoingBubble) ColorId.bubbleOut_chatCorrectFilling else ColorId.messageCorrectFilling
        )
    }

    protected fun getCorrectLineContentColor(isPersonal: Boolean): Int {
        return Theme.getColor(
            if (isPersonal) if (this.isOutgoingBubble) ColorId.bubbleOut_chatCorrectChosenFillingContent else ColorId.messageCorrectChosenFillingContent else if (this.isOutgoingBubble) ColorId.bubbleOut_chatCorrectFillingContent else ColorId.messageCorrectFillingContent
        )
    }

    protected val negativeLineColor: Int
        get() = Theme.getColor(if (this.isOutgoingBubble) ColorId.bubbleOut_chatNegativeFilling else ColorId.messageNegativeLine)

    protected val negativeLineContentColor: Int
        get() = Theme.getColor(if (this.isOutgoingBubble) ColorId.bubbleOut_chatNegativeFillingContent else ColorId.messageNegativeLineContent)

    protected val chatAuthorColor: Int
        get() = Theme.getColor(this.chatAuthorColorId)

    protected val chatAuthorColorId: Int
        get() = if (this.isOutgoingBubble) ColorId.bubbleOut_messageAuthor else ColorId.messageAuthor

    protected val chatAuthorPsaColor: Int
        get() = Theme.getColor(if (this.isOutgoingBubble) ColorId.bubbleOut_messageAuthorPsa else ColorId.messageAuthorPsa)

    private fun drawFooter(view: MessageView, c: Canvas) {
        var contentX: Int
        val contentY = this.footerTop
        if (useBubbles()) {
            // int bubblePadding = getBubbleContentPadding();
            contentX = this.actualLeftContentEdge + xBubblePadding + xBubblePaddingSmall
        } else {
            contentX = this.contentX
        }

        val rectF = Paints.getRectF()
        rectF.set(contentX.toFloat(), contentY.toFloat(), (contentX + Screen.dp(3f)).toFloat(), (contentY + this.footerHeight).toFloat())
        c.drawRoundRect(
            rectF, Screen.dp(1.5f).toFloat(), Screen.dp(1.5f).toFloat(), Paints.fillingPaint(
                this.verticalLineColor
            )
        )

        contentX += Screen.dp(10f)
        // int textY = contentY; // + Screen.dp(14f); //  + getFooterFontSizeOffset() / 2;
        val provider: TextStyleProvider = smallerTextStyleProvider
        val paint = provider.getBoldPaint()
        paint.setColor(Theme.getColor(ColorId.textNeutral))
        c.drawText((if (trimmedFooterTitle != null) trimmedFooterTitle else footerTitle)!!, contentX.toFloat(), (contentY + Screen.dp(15f)).toFloat(), paint)

        footerText!!.draw(c, contentX, contentY + Screen.dp(22f), null, 1f, view.getFooterTextMediaReceiver(true))
    }

    // Locale change
    fun updateLocale(): Boolean {
        var updated = false
        val time = genTime()
        if (this.time == null || this.time != time) {
            this.time = time
            updated = true
        }
        val fTime = genForwardTime()
        if (this.fTime == null || this.fTime != fTime) {
            this.fTime = fTime
            updated = true
        }
        if (hasBadge() && updateBadgeText()) {
            updated = true
        }
        if ((flags and FLAG_SHOW_DATE) != 0) {
            val date = genDate()
            if (this.drawDateText == null || this.drawDateText != date) {
                setDate(date)
                updated = true
            }
        }
        updateInteractionInfo(true)
        if (onLocaleChange()) {
            updated = true
        }
        if (updated) {
            buildHeader()
            return true
        }
        return false
    }

    protected open fun onLocaleChange(): Boolean {
        return false
    }

    override fun compareTo(o: TGMessage?): Int {
        return java.lang.Long.compare(o?.id ?: 0L, this.id)
    }

    fun showContentHint(view: View?, locationProvider: TooltipOverlayView.LocationProvider?, @StringRes stringRes: Int): TooltipInfo? {
        return showContentHint(view, locationProvider, TdApi.FormattedText(Lang.getString(stringRes), null))
    }

    fun showContentHint(view: View?, locationProvider: TooltipOverlayView.LocationProvider?, text: TdApi.FormattedText?): TooltipInfo? {
        return buildContentHint(view, locationProvider, true)!!.show(tdlib, text)
    }

    fun buildContentHint(view: View?, locationProvider: TooltipOverlayView.LocationProvider?, applyContentOffset: Boolean): TooltipBuilder? {
        return context().tooltipManager().builder(view, currentViews)
            .locate(TooltipOverlayView.LocationProvider { v: View?, outRect: Rect? ->
                if (locationProvider != null) {
                    locationProvider.getTargetBounds(v, outRect)
                    if (applyContentOffset) {
                        outRect!!.offset(this.contentX, this.contentY)
                    }
                } else {
                    if (applyContentOffset) {
                        outRect!!.left = this.contentX
                        outRect.top = this.contentY
                    } else {
                        outRect!!.top = 0
                        outRect.left = outRect.top
                    }
                    outRect.right = outRect.left + getContentWidth()
                    outRect.bottom = outRect.top + this.contentHeight
                }
            })
            .chatTextSize(-2f)
            .click(clickCallback())
            .controller(controller())
            .source(openParameters())
    }

    private var clickCallback: ClickCallback? = null

    protected open fun findLinkPreview(link: String?): TdApi.LinkPreview? {
        return null
    }

    protected open fun hasInstantView(link: String?): Boolean {
        return false
    }

    fun clickCallback(): ClickCallback {
        if (clickCallback != null) return clickCallback!!
        return object : ClickCallback {
            override fun onCommandClick(view: View?, text: Text?, part: TextPart?, command: String, isLongPress: Boolean): Boolean {
                if (isLongPress) {
                    messagesController().onCommandLongPressed(this@TGMessage, command)
                } else {
                    val m: TdApi.Message = this@TGMessage.message
                    val user: TdApi.User?
                    if (m.forwardInfo != null) {
                        user =
                            if (m.forwardInfo!!.origin.getConstructor() == MessageOriginUser.CONSTRUCTOR) (this@TGMessage.forwardInfo as TGSourceUser).getUser() else null
                    } else {
                        user = if (sender.isUser()) tdlib.cache().user(sender.getUserId()) else null
                    }
                    messagesController().sendCommand(
                        command,
                        if (user != null && user.type.getConstructor() == TdApi.UserTypeBot.CONSTRUCTOR) user.primaryUsername() else null
                    )
                }
                return true
            }

            override fun findLinkPreview(link: String?): TdApi.LinkPreview? {
                return this@TGMessage.findLinkPreview(link)
            }

            override fun forceInstantView(link: String?): Boolean {
                return hasInstantView(link)
            }

            override fun onUsernameClick(username: String): Boolean {
                if (isSponsoredMessage()) {
                    val usernames = sender.getUsernames()
                    if (usernames != null && usernames.findUsername(username.substring(1), true)) {
                        openSponsoredMessage()
                        return true
                    }
                }
                trackSponsoredMessageClicked()
                return false
            }

            override fun onUserClick(userId: Long): Boolean {
                if (isSponsoredMessage() && userId != 0L && sender.getUserId() == userId) {
                    openSponsoredMessage()
                    return true
                }
                trackSponsoredMessageClicked()
                return false
            }

            override fun onEmailClick(email: String?): Boolean {
                trackSponsoredMessageClicked()
                return false
            }

            override fun onPhoneNumberClick(phoneNumber: String?): Boolean {
                trackSponsoredMessageClicked()
                return false
            }

            override fun onUrlClick(view: View?, link: String?, promptUser: Boolean, openParameters: UrlOpenParameters): Boolean {
                trackSponsoredMessageClicked()
                return false
            }
        }.also { clickCallback = it }
    }

    protected fun newMessageColorProvider(): ColorProvider? {
        return if (useBubbles() && !useBubble()) null else object : ColorProvider {
            override fun tooltipColor(): Int {
                return this@TGMessage.contentReplaceColor
            }

            override fun defaultTextColor(): Int {
                return this@TGMessage.textColor
            }

            override fun clickableTextColor(isPressed: Boolean): Int {
                return this@TGMessage.textLinkColor
            }

            override fun backgroundColor(isPressed: Boolean): Int {
                return if (isPressed) this@TGMessage.textLinkHighlightColor else 0
            }

            override fun outlineColor(isPressed: Boolean): Int {
                return this@TGMessage.outlineColor
            }
        }
    }

    private fun getBadgePaint(needFakeBold: Boolean): TextPaint {
        return Paints.getMediumTextPaint(14f, needFakeBold)
    }

    protected fun addMessageFlags(flags: Int) {
        this.flags = this.flags or flags
    }

    protected val isErrorMessage: Boolean
        get() = (flags and FLAG_ERROR) != 0

    // Hot stuff
    private class HotHandler : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_HOT_CHECK -> {
                    (msg.obj as TGMessage).checkHotTimer()
                }
            }
        }

        companion object {
            const val MSG_HOT_CHECK: Int = 0
        }
    }

    // Bubbles
    fun useBubbles(): Boolean {
        return manager().useBubbles()
    }

    fun useReactionBubbles(): Boolean {
        return manager().useReactionBubbles() || forceReactionBubbles()
    }

    protected fun forceReactionBubbles(): Boolean {
        return (this is TGMessageService) || (this is TGMessageCall)
    }

    //
    var readDate: MessageReadDate? = null
        private set

    fun checkReadDate(after: RunnableBool, timeoutMs: Long) {
        if (readDate != null || (this.isUnread && !noUnread())) {
            after.runWithBool(false)
            return
        }
        getMessageProperties(msg!!.id, RunnableData { properties: MessageProperties? ->
            if (properties!!.canGetReadDate) {
                loadReadDate(after, timeoutMs)
            } else {
                runOnUiThreadOptional(Runnable { after.runWithBool(false) }
                )
            }
        })
    }

    private fun loadReadDate(after: RunnableBool, timeoutMs: Long) {
        val callbackInvoked = AtomicBoolean()
        if (timeoutMs > 0) {
            runOnUiThreadOptional(Runnable {
                if (!callbackInvoked.getAndSet(true)) {
                    after.runWithBool(true)
                }
            }, timeoutMs)
        }
        tdlib.send<MessageReadDate?>(GetMessageReadDate(msg!!.chatId, msg!!.id), Tdlib.ResultHandler { readDate: MessageReadDate?, error: TdApi.Error? ->
            if (readDate != null) {
                this.readDate = readDate
            }
            runOnUiThreadOptional(Runnable {
                callbackInvoked.set(true)
                after.runWithBool(false)
            })
        })
    }

    fun loadAvailableReactions(after: Runnable) {
        tdlib.send<AvailableReactions?>(
            GetMessageAvailableReactions(
                msg!!.chatId,
                this.smallestId, 25
            ), Tdlib.ResultHandler { availableReactions: AvailableReactions?, error: TdApi.Error? ->
                if (error != null) {
                    runOnUiThreadOptional(after)
                } else {
                    tdlib.ensureReactionsAvailable(availableReactions!!, RunnableBool { reactionsUpdated: Boolean ->
                        messageAvailableReactions = availableReactions
                        computeQuickButtons()
                        runOnUiThreadOptional(after)
                    })
                }
            })
    }

    @AnyThread
    fun lastMessageProperties(messageId: Long): MessageProperties {
        var properties = if (cachedProperties != null) cachedProperties!!.get(messageId) else null
        if (properties == null) {
            if (tdlib.inTdlibThread()) {
                onTdlibHandlerError(AssertionError("Can't access message properties here"))
                throw IllegalStateException()
            }
            properties = tdlib.getMessagePropertiesSync(this.chatId, messageId)
            cacheMessageProperties(messageId, properties)
        }
        return properties
    }

    private var cachedProperties: LongSparseArray<MessageProperties?>? = null

    @TdlibThread
    private fun cacheMessageProperties(messageId: Long, properties: MessageProperties?) {
        if (cachedProperties == null) {
            cachedProperties = LongSparseArray<MessageProperties?>()
        }
        cachedProperties!!.put(messageId, properties)
    }

    fun loadAllMessageProperties(after: Runnable?) {
        val messageIds = this.ids
        val remaining = AtomicInteger(messageIds.size)
        for (messageId in messageIds) {
            getMessageProperties(messageId, RunnableData { properties: MessageProperties? ->
                if (remaining.decrementAndGet() <= 0 && after != null) {
                    val set = LongSet(this.ids)
                    set.removeAll(*messageIds)
                    if (set.isEmpty()) {
                        runOnUiThreadOptional(after)
                    } else {
                        // Covering the edge-case when album messages were added during the MessageOptions fetch
                        loadAllMessageProperties(after)
                    }
                }
            })
        }
    }

    @AnyThread
    fun lastMessageProperties(): MessageProperties {
        return lastMessageProperties(msg!!.id)
    }

    fun getMessageProperties(messageId: Long, callback: RunnableData<MessageProperties?>?) {
        val msg = getMessage(messageId)
        if (!this.isRealMessage) {
            if (callback != null) {
                callback.runWithData(MessageProperties())
            }
            return
        }
        tdlib().send<MessageProperties?>(
            GetMessageProperties(msg!!.chatId, msg.id),
            Tdlib.ResultHandler { properties: MessageProperties?, error: TdApi.Error? ->
                if (properties != null) {
                    cacheMessageProperties(msg.id, properties)
                    runOnUiThread(Runnable {
                        if (callback != null) {
                            callback.runWithData(properties)
                        }
                    })
                }
            })
    }

    val isCustomEmojiReactionsAvailable: Boolean
        get() {
            if (messageAvailableReactions == null) return false

            return messageAvailableReactions!!.allowCustomEmoji
        }

    fun getMessageAvailableReactions(): Array<AvailableReaction?>? {
        val hasPremium = tdlib.hasPremium()
        val availableReactions: AvailableReactions? = messageAvailableReactions
        if (availableReactions == null) return null
        val addedReactions: MutableSet<String?> = HashSet<String?>()
        val reactions: MutableList<AvailableReaction?> = ArrayList<AvailableReaction?>()
        for (reaction in availableReactions.popularReactions) {
            if (reaction.type.isUnsupported()) continue
            if ((!reaction.needsPremium || hasPremium) && addedReactions.add(TD.makeReactionKey(reaction.type))) {
                reactions.add(reaction)
            }
        }
        for (reaction in availableReactions.topReactions) {
            if (reaction.type.isUnsupported()) continue
            if ((!reaction.needsPremium || hasPremium) && addedReactions.add(TD.makeReactionKey(reaction.type))) {
                reactions.add(reaction)
            }
        }
        for (reaction in availableReactions.recentReactions) {
            if (reaction.type.isUnsupported()) continue
            if ((!reaction.needsPremium || hasPremium) && addedReactions.add(TD.makeReactionKey(reaction.type))) {
                reactions.add(reaction)
            }
        }
        if (reactions.isEmpty()) {
            return null
        }
        val sortedReactions: MutableList<AvailableReaction?> = ArrayList<AvailableReaction?>(reactions)
        val activeEmojiReactions = tdlib.getActiveEmojiReactions()
        if (activeEmojiReactions != null && !activeEmojiReactions.isEmpty()) {
            Collections.sort<AvailableReaction?>(sortedReactions, Comparator { a: AvailableReaction?, b: AvailableReaction? ->
                val aPriority: Int = getPriority(a!!.type)
                val bPriority: Int = getPriority(b!!.type)
                if (aPriority != bPriority) {
                    return@Comparator if (aPriority < bPriority) -1 else 1
                }
                if (a.type.getConstructor() == ReactionTypeEmoji.CONSTRUCTOR) {
                    val aEmoji = (a.type as ReactionTypeEmoji).emoji
                    val bEmoji = (b.type as ReactionTypeEmoji).emoji
                    val aIndex = indexOf<String?>(activeEmojiReactions, aEmoji)
                    val bIndex = indexOf<String?>(activeEmojiReactions, bEmoji)
                    val aAvailable = aIndex != -1
                    val bAvailable = bIndex != -1
                    if (aAvailable != bAvailable) {
                        return@Comparator if (aAvailable) -1 else 1
                    }
                    if (aIndex != bIndex) {
                        return@Comparator if (aIndex < bIndex) -1 else 1
                    }
                }
                0
            })
        }
        @Suppress("UNCHECKED_CAST")
        return Companion.prioritizeElements(sortedReactions.toTypedArray<AvailableReaction?>() as Array<AvailableReaction>, messageReactions!!.getChosen())
    }

    fun needShowReactionPopupPicker(): Boolean {
        return messageAvailableReactions != null && (messageAvailableReactions!!.allowCustomEmoji ||
                (messageAvailableReactions!!.popularReactions.size + messageAvailableReactions!!.recentReactions.size + messageAvailableReactions!!.topReactions.size > 25)
                )
    }

    // Utils
    fun runOnUiThread(act: Runnable) {
        tdlib.ui().post(act)
    }

    fun runOnUiThread(act: Runnable, delayMillis: Long) {
        if (delayMillis > 0) {
            tdlib.ui().postDelayed(act, delayMillis)
        } else {
            tdlib.ui().post(act)
        }
    }

    @JvmOverloads
    fun runOnUiThreadOptional(act: Runnable, delayMs: Long = 0) {
        runOnUiThread(Runnable {
            if (!this.isDestroyed) {
                act.run()
            }
        }, delayMs)
    }

    fun executeOnUiThreadOptional(act: Runnable) {
        if (UI.inUiThread()) {
            if (!this.isDestroyed) {
                act.run()
            }
        } else {
            runOnUiThreadOptional(act)
        }
    }

    // quick action
    val leftQuickReactions: ArrayList<SwipeQuickAction> = ArrayList<SwipeQuickAction>()
    val rightQuickReactions: ArrayList<SwipeQuickAction?> = ArrayList<SwipeQuickAction?>()
    private val leftQuickDefaultPosition = 0
    private var rightQuickDefaultPosition = 0

    class SwipeQuickAction internal constructor(
      var text: String,
      var icon: Drawable?,
      var handler: Runnable,
      @JvmField var needDelay: Boolean,
      var isQuickReaction: Boolean
    ) {
        fun onSwipe() {
            handler.run()
        }
    }

    private fun canSendReaction(reactionType: ReactionType): Boolean {
        return canBeReacted() && !tdlib.isSelfChat(msg!!.chatId) && messageAvailableReactions!!.isAvailable(reactionType)
    }

    private fun computeQuickButtons() {
        if (!UI.inUiThread()) {
            tdlib.ui().post(Runnable { this.computeQuickButtons() })
            return
        }

        val canReply = Settings.instance()
            .needChatQuickReply() && messagesController().canWriteMessagesOrWaitingForReply() && !messagesController().needTabs() && canReplyTo()
        val canShare = Settings.instance().needChatQuickShare() && !messagesController().isSecretChat && canBeForwarded()

        leftQuickReactions.clear()
        rightQuickReactions.clear()
        rightQuickDefaultPosition = 0

        var replyButton: SwipeQuickAction? = null
        if (canReply) {
            replyButton = SwipeQuickAction(replyText!!, iQuickReply, Runnable {
                val message = this.newestMessage
                getMessageProperties(message.id, RunnableData { properties: MessageProperties? ->
                    runOnUiThreadOptional(Runnable {
                        messagesController().showReply(MessageWithProperties(message, properties!!), null, 0, "", true, true)
                    })
                })
            }, true, false)
            rightQuickReactions.add(replyButton)
        }

        if (Settings.instance().needUseQuickTranslation()) {
            if (this.isTranslated) {
                rightQuickReactions.add(SwipeQuickAction(translateStopText!!, iQuickStopTranslate, Runnable {
                    stopTranslated()
                }, true, false))
            } else if (this.isTranslatable && translationStyleMode() != Settings.TRANSLATE_MODE_NONE) {
                rightQuickReactions.add(SwipeQuickAction(translateText!!, iQuickTranslate, Runnable {
                    messagesController().startTranslateMessages(this)
                }, true, false))
            }
        }

        val quickReactions = Settings.instance().getQuickReactions(tdlib)
        for (a in quickReactions.indices) {
            val reactionString = quickReactions[a]
            val reactionType = TD.toReactionType(reactionString)
            val canReact = canSendReaction(reactionType)
            val reactionObj = tdlib.getReaction(reactionType)
            if (reactionObj != null && canReact) {
                val reactionDrawable = ReactionDrawable(reactionObj, Screen.dp(48f), Screen.dp(48f))
                reactionDrawable.setComplexReceiver(currentComplexReceiver)

                val isOdd = a % 2 == 1
                val quickReaction = SwipeQuickAction(reactionObj.getTitle(), reactionDrawable, Runnable {
                    val hasReaction = messageReactions!!.hasReaction(reactionType)
                    if (Config.DISABLE_ANONYMOUS_NON_OWNER_REACTIONS && !hasReaction && tdlib.isAnonymousAdminNonCreator(msg!!.chatId)) {
                        showContentHint(findCurrentView(), null, R.string.error_ANONYMOUS_REACTIONS_DISABLED)
                    } else if (!Config.PROTECT_ANONYMOUS_REACTIONS || hasReaction || !canGetAddedReactions() || messagesController().callNonAnonymousProtection(
                            this.id + reactionObj.hashCode(), null
                        )
                    ) {
                        if (messageReactions.toggleReaction(reactionType, false, false, handler(findCurrentView(), null, Runnable {}))) {
                            scheduleSetReactionAnimation(NextReactionAnimation(reactionObj, NextReactionAnimation.TYPE_QUICK))
                        }
                    }
                }, false, true)

                if (isOdd) {
                    rightQuickDefaultPosition += 1
                    rightQuickReactions.add(0, quickReaction)
                } else {
                    rightQuickReactions.add(quickReaction)
                }
            }
        }



        if (canShare) {
            leftQuickReactions.add(SwipeQuickAction(shareText!!, iQuickShare, Runnable {
                messagesController().shareMessages(
                    this.allMessages, false
                )
            }, true, false))
        }
    }

    fun getQuickAction(isLeft: Boolean, index: Int): SwipeQuickAction? {
        val swipeQuickActions: List<SwipeQuickAction?> = (if (isLeft) this.leftQuickReactions else this.rightQuickReactions)
        return if (index >= 0 && index < swipeQuickActions.size) swipeQuickActions.get(index) else null
    }

    fun getQuickDefaultPosition(isLeft: Boolean): Int {
        return if (isLeft) leftQuickDefaultPosition else rightQuickDefaultPosition
    }

    fun getQuickActionsCount(isLeft: Boolean): Int {
        return if (isLeft) this.leftQuickReactions.size else this.rightQuickReactions.size
    }


    // Reaction positions and draw
    private fun drawReactionsWithBubbles(c: Canvas?, v: MessageView?, x: Int, y: Int) {
        messageReactions!!.drawReactionBubbles(c, v, x, y)
        setLastDrawReactionsPosition(x, y)
    }

    private fun drawReactionsWithoutBubbles(c: Canvas, x: Int, y: Int) {
        setLastDrawReactionsPosition(x, y)
        reactionsCounterDrawable.draw(c, x, y)
    }

    private fun setLastDrawReactionsPosition(lastDrawReactionsX: Int, lastDrawReactionsY: Int) {
        this.lastDrawReactionsX = lastDrawReactionsX
        this.lastDrawReactionsY = lastDrawReactionsY
    }

    private fun getReactionPosition(reaction: TGReaction): Point {
        val reactionsDrawMode = this.reactionsDrawMode

        if (reactionsDrawMode == REACTIONS_DRAW_MODE_BUBBLE) {
            val x = lastDrawReactionsX + messageReactions!!.getReactionBubbleX(reaction) + TGReactions.getReactionImageSize() / 2 - Screen.dp(1f)
            val y = lastDrawReactionsY + messageReactions.getReactionBubbleY(reaction) + TGReactions.getReactionBubbleHeight() / 2
            return Point(x, y)
        } else if (reactionsDrawMode == REACTIONS_DRAW_MODE_ONLY_ICON) {
            val x = lastDrawReactionsX - Screen.dp(7f)
            val y = lastDrawReactionsY
            return Point(x, y)
        } else if (reactionsDrawMode == REACTIONS_DRAW_MODE_FLAT) {
            val x = (lastDrawReactionsX + Screen.dp(6f) + Screen.dp(15f) * clamp(messageReactions!!.getReactionPositionInList(reaction.type), 0f, 2f)).toInt()
            val y = lastDrawReactionsY
            return Point(x, y)
        }

        return Point(Screen.currentWidth(), 0)
    }

    private val reactionsDrawMode: Int
        get() {
            if (useReactionBubbles()) {
                return REACTIONS_DRAW_MODE_BUBBLE
            }

            val headerEnabled = hasFlag(flags, FLAG_HEADER_ENABLED)
            val isUserChat = tdlib.isUserChat(this.chatId)

            if (!useBubbles() && (this.isChannel || (!headerEnabled && !isUserChat))) {
                return REACTIONS_DRAW_MODE_ONLY_ICON
            }

            return REACTIONS_DRAW_MODE_FLAT
        }

    private val reactionsOnlyIconCounter: Counter?
        get() {
            if (shrinkedReactionsCounter != null) {
                return shrinkedReactionsCounter
            }

            shrinkedReactionsCounter = Counter.Builder()
                .noBackground()
                .allBold(false)
                .callback(this)
                .colorSet(TextColorSet {
                    if (messageReactions!!.hasChosen()) Theme.getColor(ColorId.badge) else Theme.getColor(
                        ColorId.iconLight
                    )
                })
                .drawable(R.drawable.baseline_favorite_14, 14f, 0f, Gravity.CENTER_HORIZONTAL)
                .build()

            shrinkedReactionsCounter!!.showHide(messageReactions!!.getTotalCount() > 0, false)
            shrinkedReactionsCounter!!.setMuted(!messageReactions.hasChosen(), false)

            return shrinkedReactionsCounter
        }

    private fun getReactionsCounter(): Counter? {
        if (reactionsCounter != null) {
            return reactionsCounter
        }

        reactionsCounter = Counter.Builder()
            .noBackground()
            .allBold(false)
            .callback(this)
            .textSize(if (useBubbles()) 11f else 12f)
            .colorSet(TextColorSet { if (messageReactions!!.hasChosen()) Theme.getColor(ColorId.badge) else this.timePartTextColor })
            .build()

        var count = messageReactions!!.getTotalCount()
        if (tdlib.isUserChat(msg!!.chatId) && messageReactions.getReactions() != null && (count == 1 || messageReactions.getReactions()
                .reactionTypesCount() > 1)
        ) {
            count = 0
        }
        reactionsCounter!!.setCount(count.toLong(), !messageReactions.hasChosen(), false)

        return reactionsCounter
    }

    // Set Reaction Animations
    private var nextSetReactionAnimation: NextReactionAnimation? = null
    private var savedUnreadReactions: Array<UnreadReaction>?
    private var setReactionAnimationReadyToPlay = false
    private var setReactionAnimationNowPlaying = false
    private var needHighlightUnreadReactions = false

    fun scheduleSetReactionAnimationFromBottomSheet(reaction: TGReaction, startPos: Point?) {
        val animation = NextReactionAnimation(reaction, NextReactionAnimation.TYPE_BOTTOM_SHEET)
        animation.setStartPosition(startPos)
        scheduleSetReactionAnimation(animation)
    }

    fun scheduleSetReactionAnimationFullscreenFromBottomSheet(reaction: TGReaction, startPos: Point?) {
        val animation = NextReactionAnimation(reaction, NextReactionAnimation.TYPE_BOTTOM_SHEET_FULLSCREEN)
        animation.setStartPosition(startPos)
        scheduleSetReactionAnimation(animation)
    }

    private fun scheduleSetReactionAnimation(animation: NextReactionAnimation?) {
        if (setReactionAnimationNowPlaying) {
            return
        }

        nextSetReactionAnimation = animation
        invalidate(true)
    }

    fun cancelScheduledSetReactionAnimation() {
        clearSetReactionAnimation()
    }

    private fun clearSetReactionAnimation() {
        setReactionAnimationNowPlaying = false
        setReactionAnimationReadyToPlay = false
        nextSetReactionAnimation = null
    }

    private fun startReactionAnimationIfNeeded() {
        if (nextSetReactionAnimation == null || setReactionAnimationNowPlaying) {
            return
        }

        setReactionAnimationReadyToPlay = true
        invalidate()
    }

    private fun startSetReactionAnimationIfReady() {
        if (!setReactionAnimationReadyToPlay || setReactionAnimationNowPlaying) {
            return
        }

        if (nextSetReactionAnimation == null) {
            clearSetReactionAnimation()
            return
        }

        val view = findCurrentView()
        if (view == null) {
            clearSetReactionAnimation()
            return
        }

        setReactionAnimationNowPlaying = true

        val reaction = nextSetReactionAnimation!!.reaction
        val reactionPosition = getReactionPosition(reaction)

        val positionCords = IntArray(2)
        view.getLocationOnScreen(positionCords)

        val finishX = positionCords[0] + reactionPosition.x
        val finishY = positionCords[1] + reactionPosition.y

        val entry = messageReactions!!.getMessageReactionEntry(reaction.key)
        if (entry != null) {
            val messageReaction = entry.getMessageReaction()
            if (messageReaction.totalCount == 1 && messageReaction.isChosen) {
                entry.setHidden(true)
                entry.prepareAnimation()
            }
        }

        if (nextSetReactionAnimation!!.type == NextReactionAnimation.TYPE_CLICK) {
            onQuickReactionAnimationFinish()
        } else if (nextSetReactionAnimation!!.type == NextReactionAnimation.TYPE_QUICK) {
            var startX = positionCords[0]
            var startY = positionCords[1] + view.getMeasuredHeight() / 2
            if (useBubbles()) {
                val top = topContentEdge
                val bottom = bottomContentEdge
                val height = bottom - top
                if (!Lang.rtl()) {
                    startX += view.getMeasuredWidth() - Screen.dp(BUBBLE_MOVE_MAX) / 2
                } else {
                    startX += Screen.dp(BUBBLE_MOVE_MAX) / 2
                }
                startX = (startX - translation).toInt()
                startY = positionCords[1] + top + height / 2
            } else {
                if (!Lang.rtl()) {
                    startX += Screen.dp(BUBBLE_MOVE_MAX) / 2
                } else {
                    startX += view.getMeasuredWidth() - Screen.dp(BUBBLE_MOVE_MAX) / 2
                }

                // FIXME: rely on dp, not on px
                if (height > 256) {
                    startY = (positionCords[1] + (mInitialTouchY - this.headerPadding + xHeaderPadding)).toInt()
                }
            }

            context().reactionsOverlayManager().addOverlay(
                ReactionInfo(context().reactionsOverlayManager())
                    .setSticker(nextSetReactionAnimation!!.reaction.staticCenterAnimationSicker(), false)
                    .setAnimationEndListener(OnFinishAnimationListener { this.onQuickReactionAnimationFinish() })
                    .setRepaintingColorIds(ColorId.text, ColorId.text)
                    .setAnimatedPosition(
                        Point(startX, startY),
                        Point(finishX, finishY),
                        Screen.dp(40f),
                        Screen.dp((if (useReactionBubbles()) 32 else 24).toFloat()),
                        QuickReactionAnimatedPositionProvider(),
                        MessagesRecyclerView.ITEM_ANIMATOR_DURATION + 210
                    )
                    .setAnimatedPositionOffsetProvider(QuickReactionAnimatedPositionOffsetProvider())
            )
        } else if (nextSetReactionAnimation!!.type == NextReactionAnimation.TYPE_BOTTOM_SHEET && nextSetReactionAnimation!!.startPosition != null) {
            val startX = nextSetReactionAnimation!!.startPosition!!.x
            val startY = nextSetReactionAnimation!!.startPosition!!.y
            context().reactionsOverlayManager().addOverlay(
                ReactionInfo(context().reactionsOverlayManager())
                    .setSticker(nextSetReactionAnimation!!.reaction.staticCenterAnimationSicker(), false)
                    .setAnimationEndListener(OnFinishAnimationListener { this.onQuickReactionAnimationFinish() })
                    .setRepaintingColorIds(ColorId.text, ColorId.text)
                    .setAnimatedPosition(
                        Point(startX, startY),
                        Point(finishX, finishY),
                        Screen.dp(40f),
                        Screen.dp((if (useReactionBubbles()) 32 else 24).toFloat()),
                        QuickReactionAnimatedPositionProvider(Screen.dp(80f)),
                        MessagesRecyclerView.ITEM_ANIMATOR_DURATION + 210
                    )
                    .setAnimatedPositionOffsetProvider(QuickReactionAnimatedPositionOffsetProvider())
            )
        } else if (nextSetReactionAnimation!!.type == NextReactionAnimation.TYPE_BOTTOM_SHEET_FULLSCREEN && nextSetReactionAnimation!!.startPosition != null) {
            val startX = nextSetReactionAnimation!!.startPosition!!.x
            val startY = nextSetReactionAnimation!!.startPosition!!.y

            reaction.withEffectAnimation(RunnableData { effectAnimation: TGStickerObj? ->
                val offsetProvider = QuickReactionAnimatedPositionOffsetProvider()
                val finishRunnable = Runnable {
                    val p = Point()
                    offsetProvider.getOffset(p)
                    context().replaceReactionPreviewCords(finishX + p.x, finishY + p.y)
                    context().closeStickerPreview()
                    onQuickReactionAnimationFinish(view.isAttachedToWindow())
                }
                val finishAnimation: CancellableRunnable = object : CancellableRunnable() {
                    public override fun act() {
                        finishRunnable.run()
                    }
                }

                if (effectAnimation != null && effectAnimation.getFullAnimation() != null) {
                    effectAnimation.getFullAnimation().setPlayOnce(true)
                    effectAnimation.getFullAnimation().setLooped(false)
                    effectAnimation.getFullAnimation().addLoopListener(Runnable {
                        if (nextSetReactionAnimation != null) {
                            nextSetReactionAnimation!!.fullscreenEffectFinished = true
                            if (nextSetReactionAnimation!!.fullscreenEmojiFinished) {
                                finishAnimation.cancel()
                                tdlib().ui().postDelayed(finishRunnable, 180L)
                            }
                        }
                    })
                } else {
                    nextSetReactionAnimation!!.fullscreenEffectFinished = true
                }

                val activateAnimation = nextSetReactionAnimation!!.reaction.activateAnimationSicker()
                val activateFullAnimation = activateAnimation.getFullAnimation()
                if (activateAnimation.getFullAnimation() != null) {
                    if (!activateAnimation.isCustomReaction()) {
                        activateAnimation.getFullAnimation().setPlayOnce(true)
                        activateAnimation.getFullAnimation().setLooped(false)
                    }
                    activateAnimation.getFullAnimation().addLoopListener(Runnable {
                        if (nextSetReactionAnimation != null) {
                            nextSetReactionAnimation!!.fullscreenEmojiFinished = true
                            if (nextSetReactionAnimation!!.fullscreenEffectFinished) {
                                finishAnimation.cancel()
                                tdlib().ui().postDelayed(finishRunnable, 180L)
                            }
                        }
                    })
                    activateFullAnimation.setOnTotalFrameCountLoadListener(Runnable {
                        if (nextSetReactionAnimation != null && !activateFullAnimation.hasFrame(1)) {
                            nextSetReactionAnimation!!.fullscreenEmojiFinished = true
                            if (nextSetReactionAnimation!!.fullscreenEffectFinished) {
                                finishAnimation.cancel()
                                tdlib().ui().postDelayed(finishRunnable, 180L)
                            }
                        }
                    })
                } else {
                    nextSetReactionAnimation!!.fullscreenEmojiFinished = true
                }

                context().openReactionPreview(tdlib, null, reaction, effectAnimation, startX, startY, Screen.dp(30f), -1, true)
                tdlib().ui().postDelayed(finishAnimation, 7500L)
            })
        }
    }

    @JvmOverloads
    fun onQuickReactionAnimationFinish(needPlayEffectAnimation: Boolean = true) {
        if (nextSetReactionAnimation == null) {
            clearSetReactionAnimation()
            return
        }

        if (needPlayEffectAnimation) {
            vibrate()
            startReactionBubbleAnimation(nextSetReactionAnimation!!.reaction.type)
        }
        clearSetReactionAnimation()
    }

    fun startReactionBubbleAnimation(reactionType: ReactionType?) {
        val view = findCurrentView()
        val tgReaction = tdlib.getReaction(reactionType)
        if (tgReaction == null || view == null) {
            return
        }

        val reactionPosition = getReactionPosition(tgReaction)

        val positionCords = IntArray(2)
        view.getLocationOnScreen(positionCords)
        val bubbleX = positionCords[0] + reactionPosition.x
        val bubbleY = positionCords[1] + reactionPosition.y

        messageReactions!!.startAnimation(tgReaction.key)
        val act = RunnableData { overlaySticker: TGStickerObj? ->
            context().reactionsOverlayManager().addOverlay(
                ReactionInfo(context().reactionsOverlayManager())
                    .setSticker(overlaySticker, true)
                    .setRepaintingColorIds(ColorId.text, ColorId.text)
                    .setUseDefaultSprayAnimation(tgReaction.isCustom())
                    .setEmojiStatusEffect(if (tgReaction.isCustom()) tgReaction.newCenterAnimationSicker() else null)
                    .setPosition(Point(bubbleX, bubbleY), Screen.dp(90f))
                    .setAnimatedPositionOffsetProvider(QuickReactionAnimatedPositionOffsetProvider())
            )
        }
        val overlaySticker = tgReaction.newAroundAnimationSicker()
        if (overlaySticker != null && !Config.TEST_GENERIC_REACTION_EFFECTS) {
            act.runWithData(overlaySticker)
        } else {
            tdlib.pickRandomGenericOverlaySticker(RunnableData { sticker: Sticker? ->
                if (sticker != null) {
                    val genericOverlaySticker = TGStickerObj(tdlib, sticker, null, sticker.fullType)
                        .setReactionType(tgReaction.type)
                    executeOnUiThreadOptional(Runnable { act.runWithData(genericOverlaySticker) }
                    )
                }
            })
        }
    }


    private var readyHighlightUnreadReactions = false
    private fun highlightUnreadReactionsIfNeeded() {
        if (readyHighlightUnreadReactions) {
            highlightUnreadReactionsImpl()
            readyHighlightUnreadReactions = false
        } else if (needHighlightUnreadReactions) {
            readyHighlightUnreadReactions = true
            invalidate()
        }
    }

    private fun highlightUnreadReactions() {
        needHighlightUnreadReactions = true
        if (this.combinedMessagesUnsafely != null) {
            for (message in this.combinedMessagesUnsafely) {
                if (message.unreadReactions != null && message.unreadReactions.size > 0) {
                    savedUnreadReactions = message.unreadReactions
                    break
                }
            }
        } else {
            savedUnreadReactions = msg!!.unreadReactions
        }
        invalidate()
    }

    private fun highlightUnreadReactionsImpl() {
        if (savedUnreadReactions == null || savedUnreadReactions!!.size == 0) {
            return
        }

        val reactionKeys: MutableSet<String?> = HashSet<String?>()
        val reactions = ArrayList<UnreadReaction>()
        for (unreadReaction in savedUnreadReactions) {
            if (reactionKeys.add(TD.makeReactionKey(unreadReaction.type))) {
                reactions.add(unreadReaction)
            }
        }

        for (reaction in reactions) {
            // TODO support reaction.isBig
            startReactionBubbleAnimation(reaction.type)
        }

        needHighlightUnreadReactions = false
        savedUnreadReactions = null
    }

    private class NextReactionAnimation(val reaction: TGReaction, val type: Int) {
        var startPosition: Point? = null
        var fullscreenEffectFinished: Boolean = false
        var fullscreenEmojiFinished: Boolean = false

        fun setStartPosition(startPosition: Point?): NextReactionAnimation {
            this.startPosition = startPosition
            return this
        }

        companion object {
            const val TYPE_QUICK: Int = 0
            const val TYPE_CLICK: Int = 3
            const val TYPE_BOTTOM_SHEET: Int = 1
            const val TYPE_BOTTOM_SHEET_FULLSCREEN: Int = 2
        }
    }

    class QuickReactionAnimatedPositionProvider : AnimatedPositionProvider {
        private val jumpHeight: Int

        constructor() {
            this.jumpHeight = Screen.dp(20f)
        }

        constructor(jumpHeight: Int) {
            this.jumpHeight = jumpHeight
        }

        override fun getPosition(reactionInfo: ReactionInfo, factor: Float, outPosition: Rect) {
            val startPosition = reactionInfo.getStartPosition()
            val finishPosition = reactionInfo.getFinishPosition()
            if (startPosition == null || finishPosition == null) {
                return
            }

            val positionFactor = factor // MathUtils.clamp(factor / 0.9f);
            val scaleFactor = 1f // - MathUtils.clamp((factor - .9f) * 10f);

            val jumpFactor = -4f * (positionFactor - 0.5f) * (positionFactor - 0.5f) + 1f
            val yAdd = (-jumpHeight * jumpFactor).toInt()

            val width = ((startPosition.width() + (finishPosition.width() - startPosition.width()) * positionFactor) * scaleFactor).toInt()
            val height = ((startPosition.height() + (finishPosition.height() - startPosition.height()) * positionFactor) * scaleFactor).toInt()
            val x = (startPosition.centerX() + (finishPosition.centerX() - startPosition.centerX()) * positionFactor).toInt()
            val y = (startPosition.centerY() + (finishPosition.centerY() - startPosition.centerY()) * positionFactor).toInt() + yAdd
            outPosition.set(x - width / 2, y - height / 2, x + width / 2, y + height / 2)
        }
    }

    private inner class QuickReactionAnimatedPositionOffsetProvider : AnimatedPositionOffsetProvider {
        private val startX: Int
        private val startY: Int
        private val startH: Int

        init {
            this.startX = lastDrawReactionsX
            this.startY = lastDrawReactionsY
            this.startH = height
        }

        override fun getOffset(p: Point) {
            p.x = lastDrawReactionsX - startX + translation.toInt()
            p.y = lastDrawReactionsY - startY + (startH - height)
        }
    }

    // Set reaction handlers
    private fun handler(v: View?, entry: MessageReactionEntry?, onSuccess: Runnable): Client.ResultHandler {
        return Client.ResultHandler { `object`: TdApi.Object? ->
            when (`object`!!.getConstructor()) {
                TdApi.Ok.CONSTRUCTOR -> tdlib.ui().post(onSuccess)
                TdApi.Error.CONSTRUCTOR -> {
                    tdlib.ui().post(Runnable { onSendError(v, entry, `object` as TdApi.Error) })
                    cancelScheduledSetReactionAnimation()
                }
            }
        }
    }

    private fun onSendError(v: View?, entry: MessageReactionEntry?, error: TdApi.Error?) {
        showReactionBubbleTooltip(v, entry, TD.toErrorString(error))
    }

    private fun showReactionBubbleTooltip(v: View?, entry: MessageReactionEntry?, text: String) {
        context().tooltipManager().builder(v)
            .locate(getReactionBubbleLocationProvider(entry))
            .show(tdlib, text).hideDelayed(3500, TimeUnit.MILLISECONDS)
    }

    private fun getReactionBubbleLocationProvider(entry: MessageReactionEntry?): TooltipOverlayView.LocationProvider {
        return TooltipOverlayView.LocationProvider { targetView: View?, outRect: Rect? ->
            if (entry == null) {
                return@LocationProvider
            }
            outRect!!.set(entry.getX(), entry.getY(), entry.getX() + entry.getBubbleWidth(), entry.getY() + entry.getBubbleHeight())
            outRect.offset(lastDrawReactionsX, lastDrawReactionsY)
        }
    }

    private val replyLocationProvider: TooltipOverlayView.LocationProvider
        get() = TooltipOverlayView.LocationProvider { targetView: View?, outRect: Rect? ->
            if (replyData == null) {
                return@LocationProvider
            }
            outRect!!.left = replyData!!.getLastX()
            outRect.top = replyData!!.getLastY()
            outRect.right = outRect.left + replyData!!.width(!useBubble())
            outRect.bottom = outRect.top + ReplyComponent.height()
        }

    private fun openMessageFromChannel(): Boolean {
        val tooltipBuilder = context().tooltipManager().builder(findCurrentView())
            .locate(TooltipOverlayView.LocationProvider { targetView: View?, outRect: Rect? -> isChannelHeaderCounterLastDrawRect.round(outRect!!) })

        tdlib().ui().openChat(
            this, sender.getChatId(), ChatOpenParameters()
                .urlOpenParameters(UrlOpenParameters().tooltip(tooltipBuilder)).keepStack()
        )
        return true
    }

    private fun needDrawChannelIconInHeader(): Boolean {
        return sender.isChannel() && !messagesController().isChannel
    }

    private var languageSelectorTooltip: TooltipInfo? = null

    private fun openLanguageSelectorInlineMode() {
        val tooltipBuilder = context().tooltipManager().builder(findCurrentView())
            .locate(TooltipOverlayView.LocationProvider { targetView: View?, outRect: Rect? -> isTranslatedCounterLastDrawRect.round(outRect!!) })

        languageSelectorTooltip = tooltipBuilder.show(
            this,
            View.OnClickListener { v: View? -> this.showLanguageSelectorInlineMode(v!!) }) //.hideDelayed(3500, TimeUnit.MILLISECONDS);
    }

    private fun showTranslateErrorMessageBubbleMode(message: String) {
        val tooltipBuilder = context().tooltipManager().builder(findCurrentView())
            .locate(TooltipOverlayView.LocationProvider { targetView: View?, outRect: Rect? -> isTranslatedCounterLastDrawRect.round(outRect!!) })
        languageSelectorTooltip = tooltipBuilder.show(tdlib, message).hideDelayed(3500, TimeUnit.MILLISECONDS)
    }

    private fun showMessageTooltip(locate: TooltipOverlayView.LocationProvider?, message: String, msDuration: Long): TooltipInfo? {
        val tooltipBuilder = context().tooltipManager().builder(findCurrentView()).locate(locate)
        return tooltipBuilder.show(tdlib, message).hideDelayed(msDuration, TimeUnit.MILLISECONDS)
    }

    private fun showLanguageSelectorInlineMode(v: View) {
        if (languageSelectorTooltip == null) return

        val x = max(languageSelectorTooltip!!.getContentRight() - Screen.dp((178 + 16).toFloat()), 0f)
        var y: Float
        var pivotY: Float
        if (useBubbles()) {
            y = languageSelectorTooltip!!.getContentBottom() - Screen.dp((280 + 16).toFloat())
            pivotY = Screen.dp(288f).toFloat()
            if (y < HeaderView.getTopOffset()) {
                pivotY = Screen.dp(288f) - (HeaderView.getTopOffset() - y)
                y = HeaderView.getTopOffset().toFloat()
            }
        } else {
            pivotY = Screen.dp(8f).toFloat()
            y = languageSelectorTooltip!!.getContentTop()
            if (y > Screen.currentHeight() - Screen.dp((280 + 16).toFloat())) {
                pivotY = Screen.dp(24f) + y - (Screen.currentHeight() - Screen.dp((280 + 16).toFloat()))
                y = (Screen.currentHeight() - Screen.dp((280 + 16).toFloat())).toFloat()
            }
        }

        val languagePopupLayout = LanguageSelectorPopup(
            v.getContext(),
            null,
            OnLanguageSelectListener { language: String? -> this.onLanguageChanged(language) },
            mTranslationsManager.getCurrentTranslatedLanguage(),
            getOriginalMessageLanguage()
        )
        // languagePopupLayout.languageRecyclerWrap.setAnchorMode(MenuMoreWrap.ANCHOR_MODE_RIGHT);
        languagePopupLayout.languageRecyclerWrap.setTranslationX(x)
        languagePopupLayout.languageRecyclerWrap.setTranslationY(y)

        val params = languagePopupLayout.languageRecyclerWrap.getLayoutParams() as FrameLayout.LayoutParams
        params.gravity = Gravity.TOP or Gravity.LEFT

        languagePopupLayout.show()
        languagePopupLayout.languageRecyclerWrap.setPivotX(Screen.dp((178 + 8).toFloat()).toFloat())
        languagePopupLayout.languageRecyclerWrap.setPivotY(pivotY)
    }

    private fun onLanguageChanged(language: String?) {
        if (languageSelectorTooltip != null) {
            languageSelectorTooltip!!.hide(true)
        }
        mTranslationsManager.requestTranslation(language)
    }

    protected val translationLoadingAlphaValue: Float
        get() = isTranslatedCounterDrawable.getLoadingTextAlpha()

    fun startTranslated() {
        translatedCounterForceShow = true
        mTranslationsManager.requestTranslation(Lang.getDefaultLanguageToTranslateV2(textToTranslateOriginalLanguage))
    }

    fun stopTranslated() {
        translatedCounterForceShow = false
        mTranslationsManager.stopTranslation()
    }

    fun translationStyleMode(): Int {
        return manager().getUsedTranslateStyleMode()
    }

    private var textToTranslate: TdApi.FormattedText? = null
    private var textToTranslateOriginalLanguage: String? = null

    init {
        if (!initialized) {
            synchronized(TGMessage::class.java) {
                if (!initialized) {
                    init()
                }
            }
        }
        initPaints()

        this.manager = manager
        this.tdlib = manager.controller().tdlib()

        this.mTranslationsManager = TranslationsManager(
            tdlib,
            this,
            OnChangeTranslatedStatus { status: Int, animated: Boolean -> this.setTranslatedStatus(status, animated) },
            OnChangeTranslatedResult { text: TdApi.FormattedText? -> this.setTranslationResult(text) },
            OnNewTranslatedError { message: String? -> this.showTranslateErrorMessageBubbleMode(message!!) })

        this.bubblePath = Path()
        this.bubblePathRect = RectF()
        this.bubbleClipPath = Path()
        this.bubbleClipPathRect = RectF()

        this.swipeHelper = MessageQuickActionSwipeHelper(this)
        this.currentViews = MultipleViewProvider()
        this.currentViews.setContentProvider(this)
        this.msg = msg
        this.sponsoredMessage = sponsoredMessage
        this.flags = this.flags or optional(FLAG_BELOW_ALL_MESSAGES, isBelowAllMessages)
        this.messageReactions =
            TGReactions(this, tdlib, if (msg.interactionInfo != null) msg.interactionInfo!!.reactions else null, object : MessageReactionsDelegate {
                override fun onClick(v: View?, entry: MessageReactionEntry) {
                    val hasReaction = messageReactions!!.hasReaction(entry.getReactionType())
                    if (Config.DISABLE_ANONYMOUS_NON_OWNER_REACTIONS && !hasReaction && tdlib.isAnonymousAdminNonCreator(msg.chatId)) {
                        showReactionBubbleTooltip(v, entry, Lang.getString(R.string.error_ANONYMOUS_REACTIONS_DISABLED))
                    } else if (!Config.PROTECT_ANONYMOUS_REACTIONS || hasReaction || messagesController().callNonAnonymousProtection(
                            this@TGMessage.id + entry.hashCode(), this@TGMessage, getReactionBubbleLocationProvider(entry)
                        )
                    ) {
                        val needAnimation = messageReactions.toggleReaction(entry.getReactionType(), false, false, handler(v, entry, Runnable {}))
                        if (needAnimation) {
                            scheduleSetReactionAnimation(NextReactionAnimation(entry.getTGReaction(), NextReactionAnimation.TYPE_CLICK))
                        }
                    }
                }

                override fun onLongClick(v: View?, entry: MessageReactionEntry) {
                    loadAvailableReactions(Runnable {
                        loadAllMessageProperties(Runnable {
                            if (canGetAddedReactions()) {
                                val m = messagesController()
                                m.showMessageAddedReactions(this@TGMessage, entry.getReactionType())
                            } else {
                                showReactionBubbleTooltip(v, entry, Lang.getString(R.string.ChannelReactionsAnonymous))
                            }
                        })
                    })
                }

                override fun onRebuildRequested() {
                    runOnUiThreadOptional(Runnable {
                        if (this@TGMessage.isLayoutBuilt) {
                            updateInteractionInfo(true)
                        }
                    })
                }

                override fun onInvalidateReceiversRequested() {
                    runOnUiThreadOptional(Runnable {
                        invalidateReactionFilesReceiver()
                    })
                }
            })
        this.commentButton = TGCommentButton(this)

        if (isSponsoredMessage()) {
            this.sender = TdlibSender(tdlib, msg.chatId, sponsoredMessage)
        } else {
            var sender = msg.senderId
            requireNotNull(sender)
            if (tdlib.isSelfChat(msg.chatId)) {
                flags = flags or FLAG_SELF_CHAT
                if (msg.forwardInfo != null) {
                    when (msg.forwardInfo!!.origin.getConstructor()) {
                        MessageOriginUser.CONSTRUCTOR -> sender = MessageSenderUser((msg.forwardInfo!!.origin as MessageOriginUser).senderUserId)
                        MessageOriginChat.CONSTRUCTOR -> sender = MessageSenderChat((msg.forwardInfo!!.origin as MessageOriginChat).senderChatId)
                        MessageOriginChannel.CONSTRUCTOR -> {
                            val info = msg.forwardInfo!!.origin as MessageOriginChannel
                            /* FIXME?
                               if (!Td.hasMessageSource(msg.forwardInfo)) {
                                 msg.forwardInfo.source = TdApi.ForwardSource(info.chatId, info.messageId, null, "", 0, false)
                               }
                            */
                        }
                        MessageOriginHiddenUser.CONSTRUCTOR -> {}
                    }
                }
            }
            this.sender = TdlibSender(tdlib, msg.chatId, sender, manager, !msg.isOutgoing && this.isDemoChat)
        }

        this.isPinned = Counter.Builder()
            .noBackground()
            .allBold(false)
            .callback(this)
            .drawable(R.drawable.deproko_baseline_pin_14, 14f, 0f, Gravity.CENTER_HORIZONTAL)
            .build()
        this.isEdited = Counter.Builder()
            .noBackground()
            .allBold(false)
            .callback(this)
            .drawable(R.drawable.baseline_edit_12, 12f, 0f, Gravity.CENTER_HORIZONTAL)
            .build()
        this.isEdited.showHide(true, false)
        this.isRestricted = Counter.Builder()
            .noBackground()
            .allBold(false)
            .callback(this)
            .drawable(R.drawable.baseline_warning_14, 14f, 0f, Gravity.CENTER_HORIZONTAL)
            .colorSet(TextColorSet { Theme.getColor(ColorId.messageNegativeLine) })
            .build()
        this.isRestricted.showHide(true, false)
        this.isUnsupported = Counter.Builder()
            .noBackground()
            .allBold(false)
            .callback(this)
            .drawable(R.drawable.baseline_info_14, 14f, 0f, Gravity.CENTER_HORIZONTAL)
            .build()
        this.isUnsupported.showHide(true, false)
        this.isChannelHeaderCounter = Counter.Builder()
            .noBackground()
            .allBold(false)
            .callback(this)
            .drawable(R.drawable.baseline_bullhorn_16, 16f, 0f, Gravity.CENTER_HORIZONTAL)
            .build()
        if (msg.isChannelPost || (msg.forwardInfo != null && (msg.forwardInfo!!.origin.getConstructor() == MessageOriginChannel.CONSTRUCTOR || TD.getViewCount(
                msg.interactionInfo
            ) > 1 ||
                    tdlib.isChannel(msg.forwardInfo!!.source) ||
                    this.sender.isChannel()
                    ))
        ) {
            this.viewCounter = Counter.Builder()
                .noBackground()
                .allBold(false)
                .textSize(if (useBubbles()) 11f else 12f)
                .callback(this)
                .colorSet(TextColorSet { this.timePartTextColor })
                .drawable(R.drawable.baseline_visibility_14, 14f, 3f, Gravity.LEFT)
                .build()
        } else {
            this.viewCounter = null
        }
        this.replyCounter = Counter.Builder()
            .noBackground()
            .allBold(false)
            .textSize(if (useBubbles()) 11f else 12f)
            .callback(this)
            .colorSet(TextColorSet { this.timePartTextColor })
            .drawable(R.drawable.baseline_reply_14, 14f, 3f, Gravity.LEFT)
            .build()
        this.shareCounter = Counter.Builder()
            .noBackground()
            .allBold(false)
            .textSize(if (useBubbles()) 11f else 12f)
            .callback(this)
            .colorSet(TextColorSet { this.timePartTextColor })
            .drawable(R.drawable.baseline_share_arrow_14, 14f, 3f, Gravity.LEFT)
            .build()
        this.reactionsCounterDrawable = ReactionsCounterDrawable(messageReactions.getReactionsAnimator())
        this.reactionsCounter = Counter.Builder()
            .noBackground()
            .allBold(false)
            .callback(this)
            .textSize(if (useBubbles()) 11f else 12f)
            .colorSet(TextColorSet { if (messageReactions.hasChosen()) Theme.getColor(ColorId.badge) else this.timePartTextColor })
            .build()
        this.shrinkedReactionsCounter = Counter.Builder()
            .noBackground()
            .allBold(false)
            .callback(this)
            .colorSet(TextColorSet { if (messageReactions.hasChosen()) Theme.getColor(ColorId.badge) else Theme.getColor(ColorId.iconLight) })
            .drawable(R.drawable.baseline_favorite_14, 14f, 0f, Gravity.CENTER_HORIZONTAL)
            .build()

        this.isTranslatedCounterDrawable = TranslationCounterDrawable(Drawables.get(R.drawable.baseline_translate_14))
        this.isTranslatedCounterDrawable.setColors(
            if (msg.isOutgoing) ColorId.bubbleOut_time else ColorId.bubbleIn_time,
            if (msg.isOutgoing) ColorId.bubbleOut_time else ColorId.bubbleIn_time,
            if (msg.isOutgoing) ColorId.bubbleOut_textLink else ColorId.bubbleIn_textLink
        )
        this.isTranslatedCounter = Counter.Builder()
            .noBackground()
            .allBold(false)
            .callback(this)
            .drawable(isTranslatedCounterDrawable, 3f, Gravity.LEFT)
            .build()

        updateInteractionInfo(false)

        this.time = genTime()

        if (msg.viaBotUserId != 0L) {
            val viaBot = tdlib.cache().user(msg.viaBotUserId)
            if (viaBot != null && viaBot.hasUsername()) {
                this.viaBotUsername = "@" + viaBot.primaryUsername()
            } else {
                this.viaBotUsername = null
            }
        } else {
            this.viaBotUsername = null
        }

        if (needViewGroup()) {
            overlayViews = MultipleViewProvider()
            overlayViews.setContentProvider(this)
        } else {
            overlayViews = null
        }

        if (useForward() || forceForwardOrImportInfo()) {
            loadForward()
        }

        val messageThread = messagesController().messageThread
        if (msg.replyTo != null && (messageThread == null || !messageThread.isRootMessage(msg.replyTo)) && !(msg.content != null && msg.content.getConstructor() == MessageGiveawayWinners.CONSTRUCTOR)) {
            if (msg.replyTo!!.getConstructor() == MessageReplyToMessage.CONSTRUCTOR) { // TODO: support replies to stories
                loadReply()
            }
        }

        if (this.isHot && needHotTimer() && this.isHotOpened) {
            startHotTimer(false)
        }

        computeQuickButtons()
        checkHighlightedText()

        UI.post(Runnable { updateReactionAvatars(false) })

        this.isHiddenByFilter = BoolAnimator(IS_HIDDEN_BY_MESSAGE_FILTER_ANIMATOR_ID, FactorAnimator.Target { a: Int, b: Float, c: Float, d: FactorAnimator? ->
            if (hasFlag(flags, FLAG_LAYOUT_BUILT)) {
                notifyBubbleChanged()
                invalidate()
            }
        }, DECELERATE_INTERPOLATOR, 320L)
    }

    val currentTranslatedLanguage: String?
        get() = mTranslationsManager.getCurrentTranslatedLanguage()

    val translatedText: TdApi.FormattedText?
        get() {
            if (textToTranslate == null) return null
            return mTranslationsManager.getCachedTextTranslation(textToTranslate!!.text, this.currentTranslatedLanguage)
        }

    override fun getOriginalMessageLanguage(): String? {
        return textToTranslateOriginalLanguage
    }

    val isTranslated: Boolean
        get() = mTranslationsManager.getCurrentTranslatedLanguage() != null || translatedCounterForceShow

    fun canCopyText(): Boolean {
        if (this is TGMessageMedia) {
            val messageId = this.getCaptionMessageId()
            val message = if (messageId != 0L) getMessage(messageId) else null
            return TD.canCopyText(message)
        }
        return TD.canCopyText(this.newestMessage)
    }

    val isTranslatable: Boolean
        get() = !textToTranslate.isEmpty() && (flags and FLAG_UNSUPPORTED) == 0 && !Settings.instance()
            .isNotTranslatableLanguage(textToTranslateOriginalLanguage)

    override fun getTextToTranslate(): TdApi.FormattedText? {
        return textToTranslate
    }

    fun checkTranslatableText(after: Runnable) {
        val textToTranslate = this.textToTranslateImpl
        this.textToTranslate = textToTranslate
        textToTranslateOriginalLanguage = if (textToTranslate != null) mTranslationsManager.getCachedTextLanguage(textToTranslate.text) else null
        if (textToTranslate != null && textToTranslateOriginalLanguage == null && translationStyleMode() != Settings.TRANSLATE_MODE_NONE) {
            LanguageDetector.detectLanguage(context(), textToTranslate.text, RunnableData { lang: String? ->
                mTranslationsManager.saveCachedTextLanguage(textToTranslate.text, lang.also { textToTranslateOriginalLanguage = it })
                after.run()
            }, RunnableData { err: Throwable? ->
                textToTranslateOriginalLanguage = null
                after.run()
            })
        } else {
            after.run()
        }
    }

    protected open val textToTranslateImpl: TdApi.FormattedText?
        get() = null // override

    private fun setTranslatedStatus(status: Int, animated: Boolean) {
        val show = status != TranslationCounterDrawable.TRANSLATE_STATUS_DEFAULT || translatedCounterForceShow
        isTranslatedCounterDrawable.setInvalidateCallback(if (show) Runnable { this.invalidate() } else null)
        isTranslatedCounterDrawable.setStatus(status, animated)
        if (show) {
            isTranslatedCounter.show(animated)
        } else {
            isTranslatedCounter.hide(animated)
        }
        this.buildReactions(animated)
    }

    private fun checkSelectLanguageWarning(force: Boolean) {
        val current = mTranslationsManager.getCurrentTranslatedLanguage()
        if (current == null || current.equalsOrBothEmpty(getOriginalMessageLanguage()) || force) {
            context().tooltipManager().builder(findCurrentView())
                .locate(TooltipOverlayView.LocationProvider { targetView: View?, outRect: Rect? -> isTranslatedCounterLastDrawRect.round(outRect!!) })
                .show(tdlib, Lang.getString(R.string.TapToSelectLanguage)).hideDelayed(3500, TimeUnit.MILLISECONDS)
        }
    }

    protected open fun setTranslationResult(text: TdApi.FormattedText?) {
        manager.updateMessageTranslation(this.chatId, this.smallestId, text)
    } /*  */

    open val firstEmojiId: Long
        get() {
            val text = getTextToTranslate()
            if (text == null || text.text == null || text.entities == null || text.entities.size == 0) return -1

            for (entity in text.entities) {
                if (entity.type.isCustomEmoji()) {
                    return (entity.type as TextEntityTypeCustomEmoji).customEmojiId
                }
            }

            return -1
        }

    open val uniqueEmojiPackIdList: LongArray
        get() {
            val emojiIds = TD.getUniqueEmojiIdList(getTextToTranslate())

            val emojiSets = LongSet()
            for (emojiId in emojiIds) {
                val entry = tdlib().emoji().find(emojiId)
                if (entry == null || entry.value == null) continue
                emojiSets.add(entry.value.setId)
            }

            return emojiSets.toArray()
        }

    /* Reaction Avatars */ // todo: update when supergroup updated
    fun updateReactionAvatars(animated: Boolean) {
        messageReactions!!.updateCounterAnimators(animated)
        if (hasFlag(flags, FLAG_LAYOUT_BUILT)) {
            buildReactions(animated)
        }
    }

    fun matchesReactionSenderAvatarFilter(messageText: TdApi.FormattedText?, reaction: MessageReaction, sender: MessageSender?): Boolean {
        val currentChatId = this.chatId

        if (reaction.usedSenderId.equalsTo(sender)) {
            return true
        }
        if (tdlib.isSelfSender(sender)
            || this.chatId == sender.getSenderId() || sender.equalsTo(this.inReplyToSender)
        ) {
            return true
        }

        val userId = sender.getSenderUserId()
        val user = if (userId != 0L) tdlib.cache().user(userId) else null
        if (user != null && (user.isContact || user.isCloseFriend || TD.containsMention(messageText, user))) {
            return true
        }

        val supergroup = tdlib.chatToSupergroup(currentChatId)
        if (tdlib.chatMemberCount(currentChatId) < 50 && (supergroup == null || (!supergroup.hasLocation && !supergroup.hasLinkedChat && supergroup.usernames.isEmpty()))) {
            return true
        }

        return false
    }

    // Sponsored-related tools
    fun isSponsoredMessage(): Boolean {
        return sponsoredMessage != null
    }

    fun trackSponsoredMessageClicked() {
        if (isSponsoredMessage()) {
            tdlib.client().send(ClickChatSponsoredMessage(msg!!.chatId, sponsoredMessage!!.messageId, false, false), tdlib.silentHandler())
        }
    }

    @JvmOverloads
    fun openSponsoredMessage(onFinishProgress: Runnable? = null) {
        if (!isSponsoredMessage()) {
            return
        }
        val after = RunnableBool { ok: Boolean ->
            if (onFinishProgress != null) {
                onFinishProgress.run()
            }
            if (ok) {
                trackSponsoredMessageClicked()
            }
        }
        val openParameters = openParameters()!!
            .requireOpenPrompt()
        if (onFinishProgress != null) {
            openParameters.openPromptCancellationCallback = onFinishProgress
        }
        tdlib.ui().openUrl(this, sponsoredMessage!!.sponsor.url, openParameters, after)
    }


    val sponsoredMessageUrl: String?
        get() {
            if (!isSponsoredMessage() || !Config.ALLOW_SPONSORED_MESSAGE_LINK_COPY) {
                return null
            }
            return sponsoredMessage!!.sponsor.url
        }

    val messageText: TdApi.FormattedText?
        /* * */
        get() {
            synchronized(this) {
                if (this.combinedMessagesUnsafely != null && !combinedMessagesUnsafely!!.isEmpty()) {
                    val sep = TdApi.FormattedText(" ", arrayOfNulls<TdApi.TextEntity>(0))
                    var result = TdApi.FormattedText("", arrayOfNulls<TdApi.TextEntity>(0))
                    for (msg in this.combinedMessagesUnsafely) {
                        val textPart = if (msg.content != null) msg.content.textOrCaption() else null
                        if (!textPart.isEmpty()) {
                            if (!result.isEmpty()) {
                                result = result.concat(sep)
                            }
                            result = result.concat(textPart)
                        }
                    }
                    return if (!result.isEmpty()) result else null
                }
            }
            return if (msg!!.content != null) msg!!.content.textOrCaption() else null
        }


    companion object {
        private const val MAXIMUM_CHANNEL_MERGE_TIME_DIFF = 150
        private const val MAXIMUM_COMMON_MERGE_TIME_DIFF = 900

        protected const val TEXT_CROSS_FADE_DURATION_MS: Long = 200L

        private const val MAXIMUM_CHANNEL_MERGE_COUNT = 19
        private const val MAXIMUM_COMMON_MERGE_COUNT = 14

        private const val MESSAGE_FLAG_READ = 1
        private val MESSAGE_FLAG_IS_BOTTOM = 1 shl 1
        private val MESSAGE_FLAG_HAS_OLDER_MESSAGE = 1 shl 2
        private val MESSAGE_FLAG_IS_THREAD_HEADER = 1 shl 3
        private val MESSAGE_FLAG_BELOW_HEADER = 1 shl 4
        private val MESSAGE_FLAG_FORCE_AVATAR = 1 shl 5 // FIXME conflicts with FLAG_LAYOUT_BUILT
        private val MESSAGE_FLAG_FIRST_UNREAD = 1 shl 14

        private val FLAG_LAYOUT_BUILT = 1 shl 5
        private val FLAG_MERGE_FORWARD = 1 shl 6
        private val FLAG_MERGE_BOTTOM = 1 shl 7
        private val FLAG_HEADER_ENABLED = 1 shl 8
        private val FLAG_SHOW_TICKS = 1 shl 9
        private val FLAG_SHOW_BADGE = 1 shl 10
        private val FLAG_SHOW_DATE = 1 shl 11
        private val FLAG_EXTRA_PADDING = 1 shl 12
        private val FLAG_IGNORE_REACTIONS_VIEW = 1 shl 13
        private val FLAG_HIDE_MEDIA = 1 shl 17
        private val FLAG_VIEWED = 1 shl 18
        private val FLAG_DATE_FAKE_BOLD = 1 shl 19
        private val FLAG_NO_UNREAD = 1 shl 20
        private val FLAG_ATTACHED = 1 shl 21
        private val FLAG_EVENT_LOG = 1 shl 22
        private val FLAG_BELOW_ALL_MESSAGES = 1 shl 24
        private val FLAG_SELF_CHAT = 1 shl 25
        private val FLAG_IGNORE_SWIPE = 1 shl 26
        private val FLAG_READY_QUICK_LEFT = 1 shl 27
        private val FLAG_READY_QUICK_RIGHT = 1 shl 28
        private val FLAG_UNSUPPORTED = 1 shl 29
        private val FLAG_ERROR = 1 shl 30
        private val FLAG_BEING_ADDED = 1 shl 31

        // Reaction draw mode
        const val REACTIONS_DRAW_MODE_BUBBLE: Int = 0
        const val REACTIONS_DRAW_MODE_FLAT: Int = 1
        const val REACTIONS_DRAW_MODE_ONLY_ICON: Int = 2

        private fun <T> nonNull(value: T?): T {
            requireNotNull(value) { "TDLib bug" }
            return value
        }

        private fun showUnreadAlways(msg: TGMessage?): Boolean {
            return msg is TGMessageMedia || msg is TGMessageSticker
        }

        @JvmStatic
        val estimatedContentMaxWidth: Int
            get() = Screen.smallestSide() - xPaddingRight - contentLeft

        //
        private const val IS_HIDDEN_BY_MESSAGE_FILTER_ANIMATOR_ID = 2

        private const val HIDDEN_BY_MESSAGE_FILTER_HEIGHT = 35

        private const val VIEW_COUNT_HIDDEN = 0
        private const val VIEW_COUNT_MAIN = 1
        private const val VIEW_COUNT_FORWARD = 2

        private val bubbleForwardOffset: Int
            get() = bubbleNameHeight

        private var cornerTopLeftBig: Bitmap? = null
        private var cornerTopLeftSmall: Bitmap? = null
        private var cornerTopRightBig: Bitmap? = null
        private var cornerTopRightSmall: Bitmap? = null
        private var cornerBottomLeftBig: Bitmap? = null
        private var cornerBottomLeftSmall: Bitmap? = null
        private var cornerBottomRightBig: Bitmap? = null
        private var cornerBottomRightSmall: Bitmap? = null

        private var topShadowPaint: Paint? = null
        private var bottomShadowPaint: Paint? = null
        private var leftShadowPaint: Paint? = null
        private var rightShadowPaint: Paint? = null
        private var leftShadow: Bitmap? = null
        private var topShadow: Bitmap? = null
        private var rightShadow: Bitmap? = null
        private var bottomShadow: Bitmap? = null
        private var shadowPaint: Paint? = null

        private fun initBubbleResources() {
            val res = UI.getResources()
            cornerTopLeftSmall = BitmapFactory.decodeResource(res, R.drawable.corner_small_up_left_w)
            cornerTopLeftBig = BitmapFactory.decodeResource(res, R.drawable.corner_big_up_left_w)
            cornerTopRightSmall = BitmapFactory.decodeResource(res, R.drawable.corner_small_up_right_w)
            cornerTopRightBig = BitmapFactory.decodeResource(res, R.drawable.corner_big_up_right_w)

            cornerBottomLeftSmall = BitmapFactory.decodeResource(res, R.drawable.corner_small_down_left_w)
            cornerBottomLeftBig = BitmapFactory.decodeResource(res, R.drawable.corner_big_down_left_w)
            cornerBottomRightSmall = BitmapFactory.decodeResource(res, R.drawable.corner_small_down_right_w)
            cornerBottomRightBig = BitmapFactory.decodeResource(res, R.drawable.corner_big_down_right_w)

            topShadow = BitmapFactory.decodeResource(res, R.drawable.msg_top_w)
            topShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
            topShadowPaint!!.setShader(BitmapShader(topShadow!!, Shader.TileMode.REPEAT, Shader.TileMode.CLAMP))

            bottomShadow = BitmapFactory.decodeResource(res, R.drawable.msg_down_w)
            bottomShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
            bottomShadowPaint!!.setShader(BitmapShader(bottomShadow!!, Shader.TileMode.REPEAT, Shader.TileMode.CLAMP))

            leftShadow = BitmapFactory.decodeResource(res, R.drawable.msg_left_w)
            leftShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
            leftShadowPaint!!.setShader(BitmapShader(leftShadow!!, Shader.TileMode.CLAMP, Shader.TileMode.REPEAT))

            rightShadow = BitmapFactory.decodeResource(res, R.drawable.msg_right_w)
            rightShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
            rightShadowPaint!!.setShader(BitmapShader(rightShadow!!, Shader.TileMode.CLAMP, Shader.TileMode.REPEAT))

            shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
        }

        @JvmStatic
        fun drawCornerFixes(
            c: Canvas,
            source: TGMessage,
            factor: Float,
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
            topLeftRadius: Float,
            topRightRadius: Float,
            bottomRightRadius: Float,
            bottomLeftRadius: Float
        ) {
            var left = left
            var top = top
            var right = right
            var bottom = bottom
            if (factor == 0f) return
            val paint = Paints.strokeBigPaint(alphaColor(factor, source.contentReplaceColor))

            // Paint paint = Paints.strokeBigPaint(0xffff0000);
            val offset = -paint.getStrokeWidth() / 2f
            val addRadius = 0f // paint.getStrokeWidth();

            left += offset
            top += offset
            right -= offset
            bottom -= offset

            val rectF = Paints.getRectF()

            if (topLeftRadius > 0) {
                val cx = left + topLeftRadius
                val cy = top + topLeftRadius
                val radius = topLeftRadius + addRadius
                rectF.set(cx - radius, cy - radius, cx + radius, cy + radius)
                c.drawArc(rectF, 180f, 90f, false, paint)
            }
            if (topRightRadius > 0) {
                val cx = right - topRightRadius
                val cy = top + topRightRadius
                val radius = topRightRadius + addRadius
                rectF.set(cx - radius, cy - radius, cx + radius, cy + radius)
                c.drawArc(rectF, 270f, 90f, false, paint)
            }
            if (bottomLeftRadius > 0) {
                val cx = left + bottomLeftRadius
                val cy = bottom - bottomLeftRadius
                val radius = bottomLeftRadius + addRadius
                rectF.set(cx - radius, cy - radius, cx + radius, cy + radius)
                c.drawArc(rectF, 90f, 90f, false, paint)
            }
            if (bottomRightRadius > 0) {
                val cx = right - bottomRightRadius
                val cy = bottom - bottomRightRadius
                val radius = bottomRightRadius + addRadius
                rectF.set(cx - radius, cy - radius, cx + radius, cy + radius)
                c.drawArc(rectF, 0f, 90f, false, paint)
            }
        }

        @JvmStatic
        fun getDateHeight(useBubbles: Boolean): Int {
            return (if (useBubbles) xDatePadding - Screen.dp(3f) * 2 else xDatePadding)
        }

        @JvmStatic
        fun getBubbleTransparentColor(manager: MessagesManager): Int {
            return manager.getOverlayColor(ColorId.NONE, ColorId.bubble_overlay, ColorId.bubble_overlay_noWallpaper, PropertyId.WALLPAPER_OVERRIDE_OVERLAY)
        }

        @JvmStatic
        fun getBubbleTransparentTextColor(manager: MessagesManager): Int {
            return manager.getColor(ColorId.NONE, ColorId.bubble_overlayText, ColorId.bubble_overlayText_noWallpaper, PropertyId.WALLPAPER_OVERRIDE_OVERLAY)
        }

        private fun checkClickOnRect(rectF: RectF, x: Float, y: Float, accuracy: Float): Boolean {
            val rect = Paints.getRectF()
            rect.set(rectF)
            rect.inset(-accuracy, -accuracy)
            return rect.contains(x, y)
        }

        private const val CLICK_TYPE_NONE = 0
        private const val CLICK_TYPE_REPLY = 1
        private const val CLICK_TYPE_AVATAR = 2
        private const val CLICK_TYPE_CHANNEL_MESSAGE_ICON = 3
        private const val CLICK_TYPE_TRANSLATE_MESSAGE_ICON = 4
        private const val CLICK_TYPE_MESSAGE_RESTRICTED_ICON = 5
        private const val CLICK_TYPE_MESSAGE_EDITED_ICON = 6
        private const val CLICK_TYPE_CHANNEL_MESSAGE_SENDER_ICON = 7

        protected const val LETTERS_SIZE: Float = 16f
        protected const val LETTERS_SIZE_SMALL: Float = 15f

        private const val APPLY_ACCENT_TO_FORWARDS = true

        @JvmStatic
        protected val bubbleTimePartHeight: Int
            get() = Screen.dp(16f)

        private const val COUNTER_ICON_MARGIN = 3f
        private const val COUNTER_ADD_MARGIN = 3f

        protected const val BOTTOM_LINE_EXPAND_HEIGHT: Int = -1
        @JvmField
        val BOTTOM_LINE_KEEP_WIDTH: Int = -2
        @JvmField
        protected val BOTTOM_LINE_DEFINE_BY_FACTOR: Int = -3
        @JvmField
        protected val bottomLineContentWidth: Int = BOTTOM_LINE_EXPAND_HEIGHT

        private val forwardHeaderHeight: Int
            get() = Screen.dp(26f)

        const val MESSAGE_ALBUM_TYPE_NONE: Int = 0
        const val MESSAGE_ALBUM_TYPE_MIXED: Int = 1
        const val MESSAGE_ALBUM_TYPE_MEDIA: Int = 2
        const val MESSAGE_ALBUM_TYPE_PLAYLIST: Int = 3
        const val MESSAGE_ALBUM_TYPE_FILES: Int = 4

        private val MESSAGE_INDEX_NOT_FOUND = -1
        private val MESSAGE_INDEX_SELF = -2

        const val REMOVE_NOTHING: Int = 0
        const val REMOVE_COMPLETELY: Int = 1
        const val REMOVE_COMBINATION: Int = 2

        private const val HOT_CHECK_DELAY = 18
        private var __hotHandler: HotHandler? = null

        private val hotHandler: HotHandler
            get() {
                if (__hotHandler == null) {
                    synchronized(HotHandler::class.java) {
                        if (__hotHandler == null) {
                            __hotHandler = HotHandler()
                        }
                    }
                }
                return __hotHandler!!
            }

        const val MESSAGE_NOT_CHANGED: Int = 0
        const val MESSAGE_INVALIDATED: Int = 1
        const val MESSAGE_CHANGED: Int = 2
        const val MESSAGE_REPLACE_REQUIRED: Int = 3

        private fun copyFlags(src: TdApi.Message, dst: TdApi.Message) {
            dst.id = src.id
            dst.date = src.date
            dst.sendingState = src.sendingState
            dst.schedulingState = src.schedulingState

            dst.canBeSaved = src.canBeSaved
            dst.hasTimestampedMedia = src.hasTimestampedMedia
            dst.restrictionInfo = src.restrictionInfo

            dst.editDate = src.editDate
            dst.isChannelPost = src.isChannelPost
            dst.interactionInfo = src.interactionInfo
        }

        private val bubbleNameHeight: Int
            get() = Screen.dp(24f)

        private val psaTitleHeight: Int
            get() = Screen.dp(20f)

        private const val ANIMATION_FLAG_AFFECTS_CONTAINER = 1
        private val ANIMATION_FLAG_IGNORE_CHILD = 1 shl 1
        private val ANIMATION_FLAG_AFFECTS_SELECTABLE = 1 shl 2
        private val ANIMATION_FLAG_IGNORE_SELF = 1 shl 3

        private val ANIMATOR_SELECT = -3

        private val ANIMATOR_REVOKE = -1

        private const val BUBBLE_MOVE_MAX = 64f
        const val BUBBLE_MOVE_THRESHOLD: Float = 42f
        const val QUICK_ACTION_VERTICAL_SWIPE_SIZE: Float = 80f

        private val ANIMATOR_READY_LEFT = -4
        private val ANIMATOR_READY_RIGHT = -5
        private val ANIMATOR_QUICK_VERTICAL_LEFT = -6
        private val ANIMATOR_QUICK_VERTICAL_RIGHT = -7

        private val ANIMATOR_DISMISS = -2

        // Paints
        protected var mQuickText: Paint? = null

        @JvmStatic
        protected fun mHotPaint(): TextPaint {
            return Paints.getRegularTextPaint(12f)
        }

        @JvmStatic
        protected fun mTimeBubble(): TextPaint {
            return Paints.getRegularTextPaint(11f)
        }

        @JvmStatic
        protected fun mTime(willDraw: Boolean): TextPaint {
            val paint = Paints.getRegularTextPaint(12f)
            if (willDraw) paint.setColor(Theme.getColor(ColorId.textLight))

            return paint
        }

        private fun initPaints() {
            if (mQuickText == null) {
                mQuickText = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG or Paint.FILTER_BITMAP_FLAG)
                mQuickText!!.setColor(Theme.chatQuickActionTextColor())
                ThemeManager.addThemeListener(mQuickText, ColorId.messageSwipeContent)
                mQuickText!!.setTypeface(Fonts.getRobotoRegular())
                mQuickText!!.setTextSize(Screen.dp(16f).toFloat())
            }
        }

        private var styleProvider: TextStyleProvider? = null
        private var simpleStyleProvider: TextStyleProvider? = null
        private var biggerStyleProvider: TextStyleProvider? = null
        private var smallerStyleProvider: TextStyleProvider? = null
        private var nameProvider: TextStyleProvider? = null
        private var timeProvider: TextStyleProvider? = null
        private var reactionBubbleProvider: TextStyleProvider? = null
        private var bubbleServiceProvider: TextStyleProvider? = null

        @JvmStatic
        fun reactionsTextStyleProvider(): TextStyleProvider? {
            if (reactionBubbleProvider == null) {
                reactionBubbleProvider =
                    TextStyleProvider(Fonts.newRobotoStorage()).setTextSizeDiff(-4f).setTextSize(Settings.instance().getChatFontSize()).setAllowSp(true)
                Settings.instance().addChatFontSizeChangeListener(reactionBubbleProvider)
            }
            return reactionBubbleProvider
        }

        @JvmStatic
        fun simpleTextStyleProvider(): TextStyleProvider? {
            if (simpleStyleProvider == null) {
                simpleStyleProvider = TextStyleProvider(Fonts.newRobotoStorage()).setTextSize(Settings.CHAT_FONT_SIZE_DEFAULT)
            }
            return simpleStyleProvider
        }

        @JvmStatic
        val nameStyleProvider: TextStyleProvider?
            get() {
                if (nameProvider == null) {
                    nameProvider = TextStyleProvider(Fonts.newRobotoStorage()).setTextSize(15f)
                }
                return nameProvider
            }

        @JvmStatic
        val textStyleProvider: TextStyleProvider?
            get() {
                if (styleProvider == null) {
                    styleProvider =
                        TextStyleProvider(Fonts.newRobotoStorage()).setTextSize(Settings.instance().getChatFontSize())
                            .setAllowSp(true)
                    Settings.instance().addChatFontSizeChangeListener(styleProvider)
                }
                return styleProvider
            }

        @JvmStatic
        fun getServiceTextStyleProvider(bubbleMode: Boolean): TextStyleProvider? {
            if (bubbleMode) {
                return bubbleServiceTextStyleProvider()
            } else {
                return textStyleProvider
            }
        }

        fun bubbleServiceTextStyleProvider(): TextStyleProvider? {
            if (bubbleServiceProvider == null) {
                bubbleServiceProvider =
                    TextStyleProvider(Fonts.newRobotoStorage()).setTextSizeDiff(-2f).setTextSize(Settings.instance().getChatFontSize()).setAllowSp(true)
                Settings.instance().addChatFontSizeChangeListener(bubbleServiceProvider)
            }
            return bubbleServiceProvider
        }

        val smallerTextStyleProvider: TextStyleProvider
            get() {
                if (smallerStyleProvider == null) {
                    smallerStyleProvider = TextStyleProvider(Fonts.newRobotoStorage()).setTextSizeDiff(-1f)
                        .setTextSize(Settings.instance().getChatFontSize()).setAllowSp(true)
                    Settings.instance().addChatFontSizeChangeListener(smallerStyleProvider)
                }
                return smallerStyleProvider!!
            }

        @JvmStatic
        val biggerTextStyleProvider: TextStyleProvider?
            get() {
                if (biggerStyleProvider == null) {
                    biggerStyleProvider = TextStyleProvider(Fonts.newRobotoStorage()).setTextSizeDiff(1f)
                        .setTextSize(Settings.instance().getChatFontSize()).setAllowSp(true)
                    Settings.instance().addChatFontSizeChangeListener(biggerStyleProvider)
                }
                return biggerStyleProvider
            }

        val timeTextStyleProvider: TextStyleProvider?
            get() {
                if (timeProvider == null) {
                    timeProvider = TextStyleProvider(Fonts.newRobotoStorage()).setTextSize(11f)
                }
                return timeProvider
            }

        // Sizes
        protected var xTextLine: Int = 0
        protected var xTextSize: Int = 0

        protected var xBubbleLeft1: Int = 0
        protected var xBubbleLeft2: Int = 0
        protected var xBubbleViewPadding: Int = 0
        protected var xBubbleViewPaddingSmall: Int = 0
        protected var xBubbleNameTop: Int = 0
        protected var xBubblePadding: Int = 0
        protected var xReactionBubblePadding: Int = 0
        protected var xReactionBubblePaddingTop: Int = 0
        protected var xReactionBubblePaddingBottom: Int = 0
        @JvmField
        protected var xBubblePaddingSmall: Int = 0
        @JvmField
        protected var bubbleContentPadding: Int = 0
        protected var xBubbleAvatarRadius: Int = 0
        protected var xBubbleAvatarLeft: Int = 0

        @JvmField var contentLeft: Int = 0

        @JvmStatic fun getContentLeft(): Int = contentLeft
        protected var xContentTop: Int = 0
        @JvmField
        protected var xContentOffset: Int = 0
        protected var xfContentLeft: Int = 0
        @JvmField
        protected var xPaddingTop: Int = 0
        @JvmField
        protected var xPaddingBottom: Int = 0
        @JvmField
        protected var xPaddingRight: Int = 0
        protected var xTextPadding: Int = 0
        @JvmField
        protected var xHeaderPadding: Int = 0
        protected var xDatePadding: Int = 0
        protected var xDateTop: Int = 0
        protected var xBadgeHeight: Int = 0
        protected var xBadgeTop: Int = 0
        protected var xBadgePadding: Int = 0

        @JvmField
        protected var xAvatarRadius: Int = 0
        protected var xAvatarLeft: Int = 0
        @JvmField
        protected var xAvatarCenterX: Int = 0
        protected var xLettersTop: Int = 0

        protected var xNameTop: Int = 0
        protected var xTimeTop: Int = 0
        protected var xTimePadding: Int = 0

        protected var xViewsPaddingRight: Int = 0
        protected var xViewsOffset: Int = 0
        protected var xViewsPaddingLeft: Int = 0

        protected var xQuickPadding: Int = 0
        protected var xQuickTextPadding: Int = 0
        protected var xQuickTextOffset: Int = 0
        protected var xQuickShareWidth: Int = 0
        protected var xQuickReplyWidth: Int = 0
        protected var xQuickTranslateWidth: Int = 0
        protected var xQuickTranslateStopWidth: Int = 0

        // protected static int xCaptionTouchOffset, xCaptionAddition;
        @JvmStatic
        fun reset() {
            initialized = false
            /*initSizes();
    if (mTimeBubble != null)
      mTimeBubble.setTextSize(Screen.dp(11f));
    if (mTime != null)
      mTime.setTextSize(Screen.dp(12f));
    if (mTextDeprecated != null)
      mTextDeprecated.setTextSize(Screen.dp(15f));
    if (mTextItalicDeprecated != null)
      mTextItalicDeprecated.setTextSize(Screen.dp(15f));
    if (mQuickText != null)
      mQuickText.setTextSize(Screen.dp(16f));
    if (mHotPaint != null)
      mHotPaint.setTextSize(Screen.dp(12f));
    initResources();
    initTexts();*/
        }

        private const val BUBBLE_AVATAR_RADIUS = 18f
        private const val AVATAR_RADIUS = 20.5f

        private fun initSizes() {
            xTextSize = Screen.dp(15f)
            xHeaderPadding = Screen.dp(4.5f)
            xDatePadding = Screen.dp(40f)
            xDateTop = Screen.dp(26f)

            xBubblePadding = Screen.dp(8f)
            bubbleContentPadding = xBubblePadding
            xReactionBubblePadding = Screen.dp(10f)
            xReactionBubblePaddingTop = Screen.dp(9f)
            xReactionBubblePaddingBottom = Screen.dp(10f)
            xBubblePaddingSmall = Screen.dp(2f)
            xBubbleAvatarRadius = Screen.dp(BUBBLE_AVATAR_RADIUS)
            xBubbleLeft1 = Screen.dp(8f)
            xBubbleLeft2 = Screen.dp(9f)
            xBubbleAvatarLeft = Screen.dp(6f)

            xBubbleViewPadding = Screen.dp(3f)
            xBubbleViewPaddingSmall = Screen.dp(1.5f)
            xBubbleNameTop = Screen.dp(22f)

            xAvatarLeft = Screen.dp(9f)
            xAvatarRadius = Screen.dp(AVATAR_RADIUS)
            xAvatarCenterX = xAvatarLeft + xAvatarRadius
            xLettersTop = Screen.dp(27f)

            xNameTop = Screen.dp(14f)
            xTimeTop = Screen.dp(14f)
            xContentTop = Screen.dp(25f)

            xTimePadding = Screen.dp(8f)

            xViewsOffset = Screen.dp(2.5f)
            xViewsPaddingRight = Screen.dp(3f)
            xViewsPaddingLeft = Screen.dp(5f)

            xTextLine = Screen.dp(20f)

            contentLeft = xAvatarLeft + xAvatarRadius * 2 + Screen.dp(10f)
            xContentOffset = Screen.dp(12f)
            xPaddingTop = Screen.dp(7.5f)
            xPaddingBottom = Screen.dp(7.5f)
            xPaddingRight = Screen.dp(35f)
            xTextPadding = Screen.dp(10f)

            xfContentLeft = contentLeft + Screen.dp(11f)

            xBadgeTop = Screen.dp(18f)
            // xBadgeIconTop = Screen.dp(11f);
            xBadgeHeight = Screen.dp(26f)
            xBadgePadding = Screen.dp(4f)

            xQuickPadding = Screen.dp(19f)
            xQuickTextPadding = Screen.dp(22f)
            xQuickTextOffset = Screen.dp(5.5f)

            // xCaptionTouchOffset = Screen.dp(22f);
            // xCaptionOffset = Screen.dp(22f);
            // xCaptionAddition = Screen.dp(14f);
        }

        // Icons
        private var iQuickTranslate: Drawable? = null
        private var iQuickStopTranslate: Drawable? = null
        private var iQuickReply: Drawable? = null
        private var iQuickShare: Drawable? = null
        private var iBadge: Drawable? = null
        private var shareText: String? = null
        private var replyText: String? = null
        private var translateText: String? = null
        private var translateStopText: String? = null
        private var initialized = false

        private fun initResources() {
            val res = UI.getResources()
            iBadge = Drawables.get(res, R.drawable.baseline_keyboard_arrow_down_20)
            iQuickReply = Drawables.get(res, R.drawable.baseline_reply_24)
            iQuickShare = Drawables.get(res, R.drawable.baseline_forward_24)
            iQuickTranslate = Drawables.get(res, R.drawable.baseline_translate_24)
            iQuickStopTranslate = Drawables.get(res, R.drawable.baseline_translate_off_24)
            initBubbleResources()
        }

        private fun initTexts() {
            if (mQuickText != null) {
                shareText = Lang.getString(R.string.SwipeShare)
                replyText = Lang.getString(R.string.SwipeReply)
                translateText = Lang.getString(R.string.Translate)
                translateStopText = Lang.getString(R.string.TranslateOff)
                xQuickReplyWidth = U.measureText(replyText, mQuickText!!).toInt()
                xQuickShareWidth = U.measureText(shareText, mQuickText!!).toInt()
                xQuickTranslateWidth = U.measureText(translateText, mQuickText!!).toInt()
                xQuickTranslateStopWidth = U.measureText(translateStopText, mQuickText!!).toInt()
            }
        }

        private fun isStaticText(res: Int): Boolean {
            return res == R.string.SwipeShare || res == R.string.SwipeReply
        }

        @JvmStatic
        fun processLanguageEvent(@Lang.EventType eventType: Int, arg1: Int) {
            if (eventType == Lang.EVENT_PACK_CHANGED || (eventType == Lang.EVENT_STRING_CHANGED && (arg1 == 0 || isStaticText(arg1)))) {
                initTexts()
            }
        }

        private fun init() {
            try {
                initResources()
                initPaints()
                initSizes()
                initTexts()
                initialized = true
            } catch (t: Throwable) {
                onLaunchError(t)
            }
        }

        // Other
        fun toFakeMessage(manager: MessagesManager, inChatId: Long, sponsoredMessage: SponsoredMessage): TdApi.Message {
            val tdlib = manager.controller().tdlib()
            val fakeMessage = TdApi.Message()
            fakeMessage.chatId = inChatId
            fakeMessage.id = sponsoredMessage.messageId
            fakeMessage.canBeSaved = true
            fakeMessage.content = sponsoredMessage.content
            fakeMessage.authorSignature = Lang.getString(if (sponsoredMessage.isRecommended) R.string.RecommendedSign else R.string.SponsoredSign)
            fakeMessage.isChannelPost = tdlib.isChannel(inChatId)
            val type: InlineKeyboardButtonType?
            if (tdlib.isTmeUrl(sponsoredMessage.sponsor.url)) {
                type = InlineKeyboardButtonTypeCallback()
            } else {
                type = InlineKeyboardButtonTypeUrl()
            }
            fakeMessage.replyMarkup = ReplyMarkupInlineKeyboard(
                arrayOf(
                    arrayOf<InlineKeyboardButton>(
                        InlineKeyboardButton(sponsoredMessage.buttonText, 0, ButtonStyleDefault(), type)
                    )
                )
            )
            return fakeMessage
        }

        @JvmStatic
        fun valueOf(manager: MessagesManager?, inChatId: Long, sponsoredMessage: SponsoredMessage, isBelowAllMessages: Boolean): TGMessage {
            when (sponsoredMessage.content.getConstructor()) {
                MessageText.CONSTRUCTOR -> return TGMessageText(manager, sponsoredMessage, inChatId, isBelowAllMessages)
                MessageAnimation.CONSTRUCTOR, MessagePhoto.CONSTRUCTOR, MessageVideo.CONSTRUCTOR -> return TGMessageMedia(
                    manager,
                    sponsoredMessage,
                    inChatId,
                    isBelowAllMessages
                )
            }
            throw UnsupportedOperationException(sponsoredMessage.content.toString())
        }

        @JvmStatic
        fun valueOf(
            context: MessagesManager,
            msg: TdApi.Message,
            chat: Chat?,
            messageThread: ThreadInfo?,
            chatAdmins: LongSparseArray<ChatAdministrator?>?
        ): TGMessage {
            return valueOf(
                context,
                msg,
                chat,
                messageThread,
                if (msg.senderId.getConstructor() == MessageSenderUser.CONSTRUCTOR && chatAdmins != null) chatAdmins.get((msg.senderId as MessageSenderUser).userId) else null
            )
        }

        @JvmStatic
        fun valueOf(context: MessagesManager, msg: TdApi.Message, chat: Chat?, messageThread: ThreadInfo?, admin: ChatAdministrator?): TGMessage {
            val parsedMessage = valueOf(context, msg)
            if (chat != null) {
                parsedMessage.setChatData(chat, messageThread)
            }
            if (admin != null) {
                parsedMessage.setAdministratorSign(admin)
            }
            parsedMessage.initSelectedMessages()
            return parsedMessage
        }

        private fun checkPendingContent(
            context: MessagesManager?,
            msg: TdApi.Message?,
            oldContent: MessageContent,
            pendingContent: MessageContent?,
            allowAnimatedEmoji: Boolean,
            allowNonBubbleEmoji: Boolean
        ): TGMessage? {
            if (pendingContent == null || oldContent.getConstructor() != MessageAnimatedEmoji.CONSTRUCTOR && oldContent.getConstructor() != MessageText.CONSTRUCTOR) {
                return null
            }

            @EmojiMessageContentType val emojiPendingContentType: Int = getEmojiMessageContentType(pendingContent, allowAnimatedEmoji, allowNonBubbleEmoji)
            if (emojiPendingContentType == EmojiMessageContentType.NOT_EMOJI) {
                val oldMessageText: MessageText
                if (oldContent.getConstructor() == MessageAnimatedEmoji.CONSTRUCTOR) {
                    val oldEmoji: MessageAnimatedEmoji = nonNull<MessageAnimatedEmoji>(oldContent as MessageAnimatedEmoji)
                    oldMessageText = MessageText(oldEmoji.textOrCaption(), null, null)
                } else if (oldContent.getConstructor() == MessageText.CONSTRUCTOR) {
                    oldMessageText = nonNull<MessageText>(oldContent as MessageText)
                } else {
                    throw IllegalArgumentException("Wrong content type")
                }

                val newMessageText: MessageText
                if (pendingContent.getConstructor() == MessageAnimatedEmoji.CONSTRUCTOR) {
                    val newEmoji: MessageAnimatedEmoji = nonNull<MessageAnimatedEmoji>(pendingContent as MessageAnimatedEmoji)
                    newMessageText = MessageText(newEmoji.textOrCaption(), null, null)
                } else if (pendingContent.getConstructor() == MessageText.CONSTRUCTOR) {
                    newMessageText = pendingContent as MessageText
                } else {
                    throw IllegalArgumentException("Wrong content type")
                }

                return TGMessageText(context, msg, oldMessageText, newMessageText)
            } else {
                return TGMessageSticker(context, msg, oldContent, pendingContent)
            }
        }

        @JvmStatic
        @JvmOverloads
        @Suppress("WrongConstant")
        fun valueOf(context: MessagesManager, msg: TdApi.Message, content: MessageContent? = msg.content): TGMessage {
            val tdlib = context.controller().tdlib()
            var unsupportedStringRes = R.string.UnsupportedMessage
            try {
                if (content == null) {
                    return TGMessageText(context, msg, TdApi.FormattedText(Lang.getString(R.string.DeletedMessage), null))
                }
                if (msg.restrictionInfo.hasRestriction(Settings.instance().needRestrictContent())) {
                    val restrictionText = Lang.getRestrictionText(msg.restrictionInfo)
                    val text = TGMessageText(
                        context, msg, TdApi.FormattedText(
                            restrictionText, arrayOf<TdApi.TextEntity>(
                                TdApi.TextEntity(0, restrictionText.length, TextEntityTypeItalic())
                            )
                        )
                    )
                    text.addMessageFlags(FLAG_UNSUPPORTED)
                    return text
                }

                if (content.getConstructor() == MessageChatEvent.CONSTRUCTOR) {
                    return ChatEventUtil.newMessage(context, msg, content as MessageChatEvent)
                }

                val allowAnimatedEmoji = !Settings.instance().getNewSetting(Settings.SETTING_FLAG_NO_ANIMATED_EMOJI)
                val allowNonBubbleEmoji = Settings.instance().useBigEmoji()
                val pendingMedia = tdlib.getPendingMessageMedia(msg.chatId, msg.id)
                val pendingContent = tdlib.getPendingMessageText(msg.chatId, msg.id)

                if (pendingMedia != null && pendingMedia.getFile() != null) {
                    if (pendingMedia.isPhoto()) {
                        val messagePhoto = pendingMedia.getMessagePhoto()
                        return TGMessageMedia(context, msg, messagePhoto, messagePhoto.caption).setIsMediaPending()
                    } else if (pendingMedia.isVideo()) {
                        val messageVideo = pendingMedia.getMessageVideo()
                        return TGMessageMedia(context, msg, messageVideo, messageVideo.caption).setIsMediaPending()
                    } else if (pendingMedia.isAnimation()) {
                        val messageAnimation = pendingMedia.getMessageAnimation()
                        return TGMessageMedia(context, msg, messageAnimation, messageAnimation.caption).setIsMediaPending()
                    } else if (pendingMedia.isDocument()) {
                        return TGMessageFile(context, msg, pendingMedia.getMessageDocument()).setIsMediaPending()
                    } else if (pendingMedia.isAudio()) {
                        return TGMessageFile(context, msg, pendingMedia.getMessageAudio()).setIsMediaPending()
                    }
                }

                val message: TGMessage? = checkPendingContent(context, msg, content, pendingContent, allowAnimatedEmoji, allowNonBubbleEmoji)
                if (message != null) {
                    return message
                }

                when (content.getConstructor()) {
                    MessageAnimatedEmoji.CONSTRUCTOR -> {
                        val emoji: MessageAnimatedEmoji = nonNull<MessageAnimatedEmoji>(content as MessageAnimatedEmoji)
                        if (getEmojiMessageContentType(content, allowAnimatedEmoji, allowNonBubbleEmoji) == EmojiMessageContentType.NOT_EMOJI) {
                            return TGMessageText(context, msg, MessageText(emoji.textOrCaption(), null, null), null)
                        } else {
                            return TGMessageSticker(context, msg, emoji, null)
                        }
                    }

                    MessageText.CONSTRUCTOR -> {
                        val messageText: MessageText = nonNull<MessageText>(content as MessageText)
                        if (getEmojiMessageContentType(content, allowAnimatedEmoji, allowNonBubbleEmoji) != EmojiMessageContentType.NOT_EMOJI) {
                            return TGMessageSticker(context, msg, messageText, null)
                        }
                        return TGMessageText(context, msg, nonNull<MessageText?>(content), null)
                    }

                    MessageCall.CONSTRUCTOR -> {
                        return TGMessageCall(context, msg, nonNull<MessageCall?>((content as MessageCall)))
                    }

                    MessagePhoto.CONSTRUCTOR -> {
                        val messagePhoto = content as MessagePhoto
                        return TGMessageMedia(context, msg, messagePhoto, messagePhoto.caption)
                    }

                    MessageVideo.CONSTRUCTOR -> {
                        val messageVideo = content as MessageVideo
                        return TGMessageMedia(context, msg, messageVideo, messageVideo.caption)
                    }

                    MessageAnimation.CONSTRUCTOR -> {
                        val messageAnimation = content as MessageAnimation
                        return TGMessageMedia(context, msg, messageAnimation, messageAnimation.caption)
                    }

                    MessageVideoNote.CONSTRUCTOR -> {
                        return TGMessageVideo(context, msg, nonNull<VideoNote?>((content as MessageVideoNote).videoNote), content.isViewed)
                    }

                    MessageDice.CONSTRUCTOR -> {
                        return TGMessageSticker(context, msg, nonNull<MessageDice?>((content as MessageDice)))
                    }

                    MessageSticker.CONSTRUCTOR -> {
                        return TGMessageSticker(context, msg, nonNull<Sticker?>((content as MessageSticker).sticker), false, 0)
                    }

                    TdApi.MessageVoiceNote.CONSTRUCTOR, TdApi.MessageAudio.CONSTRUCTOR, TdApi.MessageDocument.CONSTRUCTOR -> {
                        return TGMessageFile(context, msg)
                    }

                    MessagePoll.CONSTRUCTOR -> {
                        return TGMessagePoll(context, msg, nonNull<MessagePoll?>(content as MessagePoll)!!.poll)
                    }

                    MessageLocation.CONSTRUCTOR -> {
                        val location = content as MessageLocation
                        return TGMessageLocation(context, msg, nonNull<TdApi.Location?>(location.location), location.livePeriod, location.expiresIn)
                    }

                    MessageVenue.CONSTRUCTOR -> {
                        return TGMessageLocation(context, msg, (content as MessageVenue).venue)
                    }

                    MessageContact.CONSTRUCTOR -> {
                        return TGMessageContact(context, msg, content as MessageContact)
                    }

                    MessageGame.CONSTRUCTOR -> {
                        return TGMessageGame(context, msg, (content as MessageGame).game)
                    }

                    MessageExpiredPhoto.CONSTRUCTOR -> {
                        return TGMessageService(context, msg, content as MessageExpiredPhoto)
                    }

                    MessageExpiredVideo.CONSTRUCTOR -> {
                        return TGMessageService(context, msg, content as MessageExpiredVideo)
                    }

                    MessageExpiredVoiceNote.CONSTRUCTOR -> {
                        return TGMessageService(context, msg, content as MessageExpiredVoiceNote)
                    }

                    MessageExpiredVideoNote.CONSTRUCTOR -> {
                        return TGMessageService(context, msg, content as MessageExpiredVideoNote)
                    }

                    MessagePinMessage.CONSTRUCTOR -> {
                        return TGMessageService(context, msg, content as MessagePinMessage)
                    }

                    MessageScreenshotTaken.CONSTRUCTOR -> {
                        return TGMessageService(context, msg, content as MessageScreenshotTaken)
                    }

                    MessageGiftedPremium.CONSTRUCTOR -> {
                        return TGMessageService(context, msg, content as MessageGiftedPremium)
                    }

                    MessageGiftedStars.CONSTRUCTOR -> {
                        return TGMessageService(context, msg, content as MessageGiftedStars)
                    }

                    MessageChatSetTheme.CONSTRUCTOR -> {
                        return TGMessageService(context, msg, content as MessageChatSetTheme)
                    }

                    MessageChatSetMessageAutoDeleteTime.CONSTRUCTOR -> {
                        return TGMessageService(context, msg, content as MessageChatSetMessageAutoDeleteTime)
                    }

                    MessageGameScore.CONSTRUCTOR -> {
                        return TGMessageService(context, msg, content as MessageGameScore)
                    }

                    MessageContactRegistered.CONSTRUCTOR -> {
                        return TGMessageService(context, msg, content as MessageContactRegistered)
                    }

                    MessageChatChangePhoto.CONSTRUCTOR -> {
                        return TGMessageService(context, msg, content as MessageChatChangePhoto)
                    }

                    MessageCustomServiceAction.CONSTRUCTOR -> {
                        return TGMessageService(context, msg, content as MessageCustomServiceAction)
                    }

                    MessagePaymentSuccessful.CONSTRUCTOR -> {
                        return TGMessageService(context, msg, content as MessagePaymentSuccessful)
                    }

                    MessagePaymentRefunded.CONSTRUCTOR -> {
                        return TGMessageService(context, msg, content as MessagePaymentRefunded)
                    }

                    MessageWebAppDataSent.CONSTRUCTOR -> {
                        return TGMessageService(context, msg, content as MessageWebAppDataSent)
                    }

                    MessageChatDeletePhoto.CONSTRUCTOR -> {
                        return TGMessageService(context, msg, content as MessageChatDeletePhoto)
                    }

                    MessageChatAddMembers.CONSTRUCTOR -> {
                        return TGMessageService(context, msg, content as MessageChatAddMembers)
                    }

                    MessageBasicGroupChatCreate.CONSTRUCTOR -> {
                        return TGMessageService(context, msg, content as MessageBasicGroupChatCreate)
                    }

                    MessageChatChangeTitle.CONSTRUCTOR -> {
                        return TGMessageService(context, msg, content as MessageChatChangeTitle)
                    }

                    MessageChatDeleteMember.CONSTRUCTOR -> {
                        return TGMessageService(context, msg, content as MessageChatDeleteMember)
                    }

                    MessageChatJoinByLink.CONSTRUCTOR -> {
                        return TGMessageService(context, msg, content as MessageChatJoinByLink)
                    }

                    MessageChatJoinByRequest.CONSTRUCTOR -> {
                        return TGMessageService(context, msg, content as MessageChatJoinByRequest)
                    }

                    MessageProximityAlertTriggered.CONSTRUCTOR -> {
                        return TGMessageService(context, msg, content as MessageProximityAlertTriggered)
                    }

                    MessageInviteVideoChatParticipants.CONSTRUCTOR -> {
                        return TGMessageService(context, msg, content as MessageInviteVideoChatParticipants)
                    }

                    MessageVideoChatStarted.CONSTRUCTOR -> {
                        return TGMessageService(context, msg, content as MessageVideoChatStarted)
                    }

                    MessageVideoChatScheduled.CONSTRUCTOR -> {
                        return TGMessageService(context, msg, content as MessageVideoChatScheduled)
                    }

                    MessageVideoChatEnded.CONSTRUCTOR -> {
                        return TGMessageService(context, msg, content as MessageVideoChatEnded)
                    }

                    MessageSupergroupChatCreate.CONSTRUCTOR -> {
                        return TGMessageService(context, msg, content as MessageSupergroupChatCreate)
                    }

                    MessageDirectMessagePriceChanged.CONSTRUCTOR -> {
                        return TGMessageService(context, msg, content as MessageDirectMessagePriceChanged)
                    }

                    MessageBotWriteAccessAllowed.CONSTRUCTOR -> {
                        return TGMessageService(context, msg, content as MessageBotWriteAccessAllowed)
                    }

                    MessageChatUpgradeTo.CONSTRUCTOR -> {
                        return TGMessageService(context, msg, content as MessageChatUpgradeTo)
                    }

                    MessageChatUpgradeFrom.CONSTRUCTOR -> {
                        return TGMessageService(context, msg, content as MessageChatUpgradeFrom)
                    }

                    MessageForumTopicCreated.CONSTRUCTOR -> {
                        return TGMessageService(context, msg, content as MessageForumTopicCreated)
                    }

                    MessageForumTopicEdited.CONSTRUCTOR -> {
                        return TGMessageService(context, msg, content as MessageForumTopicEdited)
                    }

                    MessageForumTopicIsClosedToggled.CONSTRUCTOR -> {
                        return TGMessageService(context, msg, content as MessageForumTopicIsClosedToggled)
                    }

                    MessageForumTopicIsHiddenToggled.CONSTRUCTOR -> {
                        return TGMessageService(context, msg, content as MessageForumTopicIsHiddenToggled)
                    }

                    MessageGiveawayCreated.CONSTRUCTOR -> {
                        return TGMessageService(context, msg, content as MessageGiveawayCreated)
                    }

                    MessageGiveawayCompleted.CONSTRUCTOR -> {
                        return TGMessageService(context, msg, content as MessageGiveawayCompleted)
                    }

                    MessageChatBoost.CONSTRUCTOR -> {
                        return TGMessageService(context, msg, content as MessageChatBoost)
                    }

                    MessagePremiumGiftCode.CONSTRUCTOR -> {
                        return TGMessageGift(context, msg, content as MessagePremiumGiftCode)
                    }

                    MessageGiveawayWinners.CONSTRUCTOR -> {
                        return TGMessageGiveawayWinners(context, msg, content as MessageGiveawayWinners)
                    }

                    MessageGiveaway.CONSTRUCTOR -> {
                        return TGMessageGiveaway(context, msg, content as MessageGiveaway)
                    }

                    TdApi.MessageInvoice.CONSTRUCTOR, TdApi.MessagePassportDataSent.CONSTRUCTOR, TdApi.MessageStory.CONSTRUCTOR, TdApi.MessageChatSetBackground.CONSTRUCTOR, TdApi.MessageSuggestProfilePhoto.CONSTRUCTOR, TdApi.MessageSuggestBirthdate.CONSTRUCTOR, TdApi.MessageUsersShared.CONSTRUCTOR, TdApi.MessageChatShared.CONSTRUCTOR, TdApi.MessagePaidMedia.CONSTRUCTOR, TdApi.MessageGiveawayPrizeStars.CONSTRUCTOR, TdApi.MessageGift.CONSTRUCTOR, TdApi.MessageUpgradedGift.CONSTRUCTOR, TdApi.MessageUpgradedGiftPurchaseOffer.CONSTRUCTOR, TdApi.MessageUpgradedGiftPurchaseOfferRejected.CONSTRUCTOR, TdApi.MessageRefundedUpgradedGift.CONSTRUCTOR, TdApi.MessageStakeDice.CONSTRUCTOR, TdApi.MessageGroupCall.CONSTRUCTOR, TdApi.MessagePaidMessagesRefunded.CONSTRUCTOR, TdApi.MessagePaidMessagePriceChanged.CONSTRUCTOR, TdApi.MessageChecklist.CONSTRUCTOR, TdApi.MessageChecklistTasksAdded.CONSTRUCTOR, TdApi.MessageChecklistTasksDone.CONSTRUCTOR, TdApi.MessageSuggestedPostApprovalFailed.CONSTRUCTOR, TdApi.MessageSuggestedPostApproved.CONSTRUCTOR, TdApi.MessageSuggestedPostDeclined.CONSTRUCTOR, TdApi.MessageSuggestedPostPaid.CONSTRUCTOR, TdApi.MessageSuggestedPostRefunded.CONSTRUCTOR, TdApi.MessageGiftedTon.CONSTRUCTOR, TdApi.MessagePaymentSuccessfulBot.CONSTRUCTOR, TdApi.MessageChatHasProtectedContentDisableRequested.CONSTRUCTOR, TdApi.MessageChatHasProtectedContentToggled.CONSTRUCTOR, TdApi.MessageChatOwnerChanged.CONSTRUCTOR, TdApi.MessageChatOwnerLeft.CONSTRUCTOR, TdApi.MessageManagedBotCreated.CONSTRUCTOR, TdApi.MessagePollOptionAdded.CONSTRUCTOR, TdApi.MessagePollOptionDeleted.CONSTRUCTOR -> {
                    }

                    TdApi.MessageUnsupported.CONSTRUCTOR -> unsupportedStringRes = R.string.UnsupportedMessageType
                    TdApi.MessagePassportDataReceived.CONSTRUCTOR, TdApi.MessageWebAppDataReceived.CONSTRUCTOR -> {
                        org.thunderdog.challegram.Log.e("Received bot message for a regular user:\n%s", msg)
                    }

                    else -> {
                        assertMessageContent_baa076bf()
                        throw unsupported(msg.content)
                    }
                }
            } catch (e: UnsupportedOperationException) {
                org.thunderdog.challegram.Log.v("Unsupported message (app-level)", e)
            } catch (t: Throwable) {
                org.thunderdog.challegram.Log.e("Cannot parse message", t)
                return valueOfError(context, msg, t)
            }
            val unsupportedText = Lang.getString(unsupportedStringRes)
            val text = TGMessageText(
                context, msg, TdApi.FormattedText(
                    unsupportedText, arrayOf<TdApi.TextEntity>(
                        TdApi.TextEntity(0, unsupportedText.length, TextEntityTypeItalic())
                    )
                )
            )
            text.addMessageFlags(FLAG_UNSUPPORTED)
            return text
        }

        fun valueOfError(context: MessagesManager, msg: TdApi.Message, error: Throwable?): TGMessage {
            val text = Lang.getString(R.string.FailureMessageText)

            var entities: Array<TdApi.TextEntity?>? = null
            try {
                val result = Client.execute<TextEntities>(GetTextEntities(text))
                entities = result.entities
            } catch (ignored: Client.ExecutionException) {
            }

            val logEntity = TdApi.TextEntity(-1, -1, TextEntityTypePreCode())

            if (entities != null && entities.size > 0) {
                val newEntities = arrayOfNulls<TdApi.TextEntity>(entities.size + 1)
                System.arraycopy(entities, 0, newEntities, 0, entities.size)
                newEntities[entities.size] = logEntity
                entities = newEntities
            } else {
                entities = arrayOf<TdApi.TextEntity?>(logEntity)
            }


            val b = StringBuilder(text)
            b.append("\n\n")

            logEntity.offset = b.length

            b.append(Lang.getString(R.string.AppName)).append(' ').append(BuildConfig.VERSION_NAME).append("\n")
            b.append("Type: ").append(if (msg.content != null) msg.content.javaClass.getSimpleName() else "NULL").append("\n")
            b.append("Android: ").append(getPrettyName()).append(" (").append(Build.VERSION.SDK_INT).append(")\n")
            b.append("Screen: ").append(Screen.widestSide()).append("x").append(Screen.smallestSide()).append("x").append(Screen.density()).append("\n")

            val tdlib = context.controller().tdlib()
            val chat = tdlib.chat(msg.chatId)
            if (chat != null) {
                if (chat.type.getConstructor() == ChatTypeSupergroup.CONSTRUCTOR) {
                    val supergroup = tdlib.chatToSupergroup(msg.chatId)
                    if (supergroup != null && supergroup.hasUsername()) {
                        b.append("Public post: t.me/").append(supergroup.primaryUsername()).append('/').append(msg.id shr 20).append("\n")
                    }
                }
            } else {
                b.append("FROM UNKNOWN CHAT #").append(msg.chatId).append("\n")
            }
            b.append("\n")

            org.thunderdog.challegram.Log.toStringBuilder(error, 2, true, b)

            logEntity.length = b.length - logEntity.offset

            val result = TGMessageText(context, msg, TdApi.FormattedText(b.toString(), entities))
            result.addMessageFlags(FLAG_UNSUPPORTED or FLAG_ERROR)
            return result
        }

        private fun getPriority(type: ReactionType): Int {
            when (type.getConstructor()) {
                TdApi.ReactionTypePaid.CONSTRUCTOR -> return 0
                ReactionTypeEmoji.CONSTRUCTOR -> return 1
                TdApi.ReactionTypeCustomEmoji.CONSTRUCTOR -> return 2
                else -> {
                    assertReactionType_43844388()
                    throw unsupported(type)
                }
            }
        }

        private fun prioritizeElements(inputArray: Array<AvailableReaction>?, set: MutableSet<String?>): Array<AvailableReaction?>? {
            if (inputArray == null) {
                return null
            }

            val resultList: MutableList<AvailableReaction?> = ArrayList<AvailableReaction?>()

            for (element in inputArray) {
                if (set.contains(TD.makeReactionKey(element.type))) {
                    resultList.add(element)
                }
            }

            for (element in inputArray) {
                if (!set.contains(TD.makeReactionKey(element.type))) {
                    resultList.add(element)
                }
            }

            val resultArray = resultList.toTypedArray<AvailableReaction?>()

            return resultArray
        }

        /* * */
        @JvmStatic
        @EmojiMessageContentType
        fun getEmojiMessageContentType(content: MessageContent?): Int {
            val allowAnimatedEmoji = !Settings.instance().getNewSetting(Settings.SETTING_FLAG_NO_ANIMATED_EMOJI)
            val allowNonBubbleEmoji = Settings.instance().useBigEmoji()
            return getEmojiMessageContentType(content, allowAnimatedEmoji, allowNonBubbleEmoji)
        }

        @EmojiMessageContentType
        fun getEmojiMessageContentType(content: MessageContent?, allowAnimatedEmoji: Boolean, allowNonBubbleEmoji: Boolean): Int {
            if (content == null) {
                return EmojiMessageContentType.NOT_EMOJI
            }

            if (content.getConstructor() == MessageAnimatedEmoji.CONSTRUCTOR) {
                if (allowAnimatedEmoji && TD.isStickerFromAnimatedEmojiPack(content)) {
                    return EmojiMessageContentType.ANIMATED_EMOJI
                } else if (allowNonBubbleEmoji) {
                    return EmojiMessageContentType.NON_BUBBLE_EMOJI
                }
            } else if (content.getConstructor() == MessageText.CONSTRUCTOR) {
                if (allowNonBubbleEmoji && NonBubbleEmojiLayout.isValidEmojiText((content as MessageText).text)) {
                    return EmojiMessageContentType.NON_BUBBLE_EMOJI
                }
            }
            return EmojiMessageContentType.NOT_EMOJI
        }
    }
}
