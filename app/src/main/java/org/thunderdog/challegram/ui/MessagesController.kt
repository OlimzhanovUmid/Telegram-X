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
 * File created on 27/04/2015 at 15:32
 */
package org.thunderdog.challegram.ui

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Rect
import android.location.Location
import android.location.LocationManager
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.os.SystemClock
import android.text.InputType
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.ClickableSpan
import android.util.SparseIntArray
import android.util.TypedValue
import android.view.*
import android.view.View.OnLongClickListener
import android.widget.*
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.annotation.UiThread
import androidx.collection.LongSparseArray
import androidx.collection.SparseArrayCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResultCallback
import com.google.android.gms.location.*
import me.vkryl.android.DECELERATE_INTERPOLATOR
import me.vkryl.android.animator.BoolAnimator
import me.vkryl.android.animator.FactorAnimator
import me.vkryl.android.util.ClickHelper
import me.vkryl.android.widget.FrameLayoutFix
import me.vkryl.android.widget.FrameLayoutFix.Companion.newParams
import me.vkryl.core.*
import me.vkryl.core.collection.IntList
import me.vkryl.core.collection.LongList
import me.vkryl.core.collection.LongSet
import me.vkryl.core.lambda.*
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import org.drinkless.tdlib.TdApi.*
import org.thunderdog.challegram.*
import org.thunderdog.challegram.R
import org.thunderdog.challegram.component.MediaCollectorDelegate
import org.thunderdog.challegram.component.attach.CustomItemAnimator
import org.thunderdog.challegram.component.attach.MediaBottomFilesController.MusicEntry
import org.thunderdog.challegram.component.attach.MediaLayout
import org.thunderdog.challegram.component.attach.MediaToReplacePickerManager
import org.thunderdog.challegram.component.attach.MediaToReplacePickerManager.LocalPickedFile
import org.thunderdog.challegram.component.attach.SponsoredMessagesInfoController
import org.thunderdog.challegram.component.base.SettingView
import org.thunderdog.challegram.component.chat.*
import org.thunderdog.challegram.component.chat.InlineResultsWrap.PickListener
import org.thunderdog.challegram.component.chat.InputView.SpanChangeListener
import org.thunderdog.challegram.component.chat.MessagesManager.VideoScrollParameters
import org.thunderdog.challegram.component.chat.MessagesSearchManagerMiddleware.SearchMessagesFilterTextPolyfill
import org.thunderdog.challegram.component.chat.TdlibSingleUnreadReactionsManager.UnreadSingleReactionListener
import org.thunderdog.challegram.component.popups.MessageSeenController
import org.thunderdog.challegram.component.popups.ModernActionedLayout
import org.thunderdog.challegram.component.sticker.StickerSmallView
import org.thunderdog.challegram.component.sticker.StickerSmallView.StickerMovementCallback
import org.thunderdog.challegram.component.sticker.TGStickerObj
import org.thunderdog.challegram.config.Config
import org.thunderdog.challegram.core.Background
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.core.Media
import org.thunderdog.challegram.data.*
import org.thunderdog.challegram.data.TGMessage.SelectableDelegate
import org.thunderdog.challegram.filegen.PhotoGenerationInfo
import org.thunderdog.challegram.filegen.VideoGenerationInfo
import org.thunderdog.challegram.helper.BotHelper
import org.thunderdog.challegram.helper.FoundUrls
import org.thunderdog.challegram.helper.LinkPreview
import org.thunderdog.challegram.helper.LiveLocationHelper
import org.thunderdog.challegram.loader.ImageFile
import org.thunderdog.challegram.loader.ImageGalleryFile
import org.thunderdog.challegram.loader.ImageReader
import org.thunderdog.challegram.loader.ImageStrictCache
import org.thunderdog.challegram.mediaview.*
import org.thunderdog.challegram.mediaview.MediaViewController.Args.Companion.fromGallery
import org.thunderdog.challegram.mediaview.MediaViewController.Companion.openFromChat
import org.thunderdog.challegram.mediaview.data.MediaItem
import org.thunderdog.challegram.mediaview.data.MediaStack
import org.thunderdog.challegram.navigation.*
import org.thunderdog.challegram.navigation.Menu
import org.thunderdog.challegram.navigation.SettingsWrap.OnActionButtonClick
import org.thunderdog.challegram.navigation.SettingsWrapBuilder.CustomSettingProcessor
import org.thunderdog.challegram.navigation.TooltipOverlayView.TooltipBuilder
import org.thunderdog.challegram.navigation.TooltipOverlayView.TooltipInfo
import org.thunderdog.challegram.player.RecordAudioVideoController.RecordStateListeners
import org.thunderdog.challegram.support.RippleSupport
import org.thunderdog.challegram.support.ViewSupport
import org.thunderdog.challegram.telegram.*
import org.thunderdog.challegram.telegram.TGLegacyManager.EmojiLoadListener
import org.thunderdog.challegram.telegram.Tdlib.ChatMemberStatusChangeCallback
import org.thunderdog.challegram.telegram.TdlibCache.*
import org.thunderdog.challegram.telegram.TdlibSettingsManager.DismissRequestsListener
import org.thunderdog.challegram.telegram.TdlibUi.*
import org.thunderdog.challegram.theme.*
import org.thunderdog.challegram.tool.*
import org.thunderdog.challegram.ui.camera.CameraAccessImageView
import org.thunderdog.challegram.unsorted.Settings
import org.thunderdog.challegram.unsorted.Settings.VideoModePreferenceListener
import org.thunderdog.challegram.unsorted.Test
import org.thunderdog.challegram.util.*
import org.thunderdog.challegram.util.text.Text
import org.thunderdog.challegram.util.text.TextColorSets
import org.thunderdog.challegram.v.HeaderEditText
import org.thunderdog.challegram.v.MessagesLayoutManager
import org.thunderdog.challegram.v.MessagesRecyclerView
import org.thunderdog.challegram.voip.VoIPLogs
import org.thunderdog.challegram.widget.*
import org.thunderdog.challegram.widget.CollapseListView.TotalHeightChangeListener
import org.thunderdog.challegram.widget.CollapseListView.ViewItem
import org.thunderdog.challegram.widget.ForceTouchView.ForceTouchContext
import org.thunderdog.challegram.widget.ForceTouchView.PreviewDelegate
import org.thunderdog.challegram.widget.WallpaperParametersView.WallpaperParametersListener
import org.thunderdog.challegram.widget.rtl.RtlViewPager
import tgx.td.*
import tgx.td.data.MessageWithProperties
import tgx.td.ui.reportChatSponsoredMessage
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.contracts.ExperimentalContracts
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalContracts::class)
open class MessagesController(context: Context, tdlib: Tdlib?) : ViewController<MessagesController.Arguments?>(context, tdlib), Menu, Unlockable,
    View.OnClickListener, ActivityResultHandler, MoreDelegate, CommandKeyboardLayout.Callback, MediaCollectorDelegate, SelectDelegate, ReplyBarView.Callback,
    RaiseHelper.Listener, EmojiLoadListener, ChatHeaderView.Callback, ChatListener, NotificationSettingsListener, EmojiLayout.Listener, MessageThreadListener,
    UnreadSingleReactionListener, SupergroupDataChangeListener, BasicGroupDataChangeListener, SecretChatDataChangeListener, UserDataChangeListener,
    UserStatusChangeListener, FactorAnimator.Target, StickerSuggestionAdapter.Callback, MediaViewDelegate, PreviewDelegate, VideoModePreferenceListener,
    RecordStateListeners, OnPageChangeListener, ViewPagerTopView.OnItemClickListener, SelectableDelegate, GlobalAccountListener, EmojiToneHelper.Delegate,
    ComplexHeaderView.Callback, LiveLocationHelper.Callback, CreatePollController.Callback, HapticMenuHelper.Provider, HapticMenuHelper.OnItemClickListener,
    DismissRequestsListener, InputView.SelectionChangeListener {
    private var reuseEnabled = false
    private var destroyInstance = false

    fun setReuseEnabled(enabled: Boolean) {
        this.reuseEnabled = enabled
    }

    fun setDestroyInstance() {
        destroyInstance = true
    }

    private val flags = 0

    var chat: Chat? = null
        private set
    private var openedFromChatList: ChatList? = null

    private var headerCell: ChatHeaderView? = null
    private var headerDoubleCell: DoubleHeaderView? = null

    private var contentView: MessagesLayout? = null

    // Record
    var bottomWrap: LinearLayout? = null
        private set
    private var bottomSpace: FillingSpace? = null
    var messagesView: MessagesRecyclerView? = null
        private set

    @JvmField val manager: MessagesManager?
    private var botHelper: BotHelper? = null

    private var inputView: InputView? = null
    private val inputViewDisabledClickHelper = ClickHelper(object : ClickHelper.Delegate {
        override fun needClickAt(view: View, x: Float, y: Float): Boolean {
            return !hasSendBasicMessagePermission()
        }

        override fun onClickAt(view: View, x: Float, y: Float) {
            context().tooltipManager().builder(view).show(tdlib, R.string.MessageInputTextDisabledHint).hideDelayed()
        }
    })
    private var bottomShadowView: SeparatorView? = null
    private var enableOnResume = false

    var emojiKeyboardLayout: KeyboardFrameLayout? = null
        private set

    private var emojiLayout: EmojiLayout? = null
    private var textFormattingLayout: TextFormattingLayout? = null
    private var attachButtons: AttachLinearLayout? = null
    private var emojiButton: ImageView? = null
    private var recordButton: VoiceVideoButtonView? = null
    var sendButton: SendButton? = null
        private set
    private var sendMenu: HapticMenuHelper? = null
    private var mediaButton: CameraAccessImageView? = null
    private var cameraButton: InvisibleImageView? = null
    private var scheduleButton: InvisibleImageView? = null
    private var commandButton: InvisibleImageView? = null
    private var silentButton: SilentButton? = null

    private var sendAsMenu: HapticMenuHelper? = null
    private var messageSenderButton: MessageSenderButton? = null

    private var wallpaperViewBlurPreview: WallpaperView? = null
    private var backgroundParamsView: WallpaperParametersView? = null

    private var goToNextFoundMessageButtonBadge: CircleCounterBadgeView? = null
    private var goToPrevFoundMessageButtonBadge: CircleCounterBadgeView? = null
    private var scrollToBottomButtonWrap: FrameLayoutFix? = null
    private var mentionButtonWrap: FrameLayoutFix? = null
    private var reactionsButtonWrap: FrameLayoutFix? = null
    private var scrollToBottomButton: CircleButton? = null
    private var mentionButton: CircleButton? = null
    private var reactionsButton: CircleButton? = null
    private var unreadCountView: CounterBadgeView? = null
    private var mentionCountView: CounterBadgeView? = null
    private var reactionsCountView: CounterBadgeView? = null

    override fun onCreateHapticMenu(view: View?): MutableList<HapticMenuHelper.MenuItem?>? {
        var currentText: TdApi.FormattedText? = null
        val canSendWithoutMarkdown =
            inputView != null && !inputView!!.getOutputText(true).equalsTo(inputView!!.getOutputText(false).also { currentText = it }, true)
        var items = tdlib!!.ui().fillDefaultHapticMenu(
            getChatId(),
            this.isEditingMessage, canSendWithoutMarkdown, true
        )
        if (!canSendWithoutMarkdown && tdlib!!.shouldSendAsDice(currentText!!) && !this.isEditingMessage) {
            if (items == null) items = ArrayList<HapticMenuHelper.MenuItem?>()
            if (ContentPreview.EMOJI_DART.textRepresentation == currentText!!.text) {
                items.add(HapticMenuHelper.MenuItem(R.id.btn_sendNoMarkdown, Lang.getString(R.string.SendDiceAsEmoji), R.drawable.baseline_gps_fixed_24))
            } else if (ContentPreview.EMOJI_DICE.textRepresentation == currentText.text) {
                items.add(HapticMenuHelper.MenuItem(R.id.btn_sendNoMarkdown, Lang.getString(R.string.SendDiceAsEmoji), R.drawable.baseline_casino_24))
            } else {
                items.add(
                    HapticMenuHelper.MenuItem(
                        R.id.btn_sendNoMarkdown,
                        Lang.getString(R.string.SendDiceAsEmoji),
                        Drawables.emojiDrawable(currentText.text)
                    )
                )
            }
        }
        if (BuildConfig.DEBUG) {
            items!!.add(HapticMenuHelper.MenuItem(R.id.btn_sendToast, "Show toast", R.drawable.baseline_warning_24))
            items.add(HapticMenuHelper.MenuItem(R.id.btn_debugLtrEmoji, "Send LTR emoji", R.drawable.baseline_warning_24))
        }
        if (canSelectSender()) {
            items!!.add(0, createHapticSenderItem(R.id.btn_openSendersMenu, chat!!.messageSenderId, false, false))
        }
        hideCursorsForInputView()
        return items
    }

    override fun onHapticMenuItemClick(view: View, parentView: View?, item: HapticMenuHelper.MenuItem): Boolean {
        val viewId = view.getId()
        if (viewId == R.id.btn_setMsgSender) {
            if (item.isLocked) {
                context().tooltipManager().builder(messageSenderButton!!.getButtonView())
                    .show(tdlib, Lang.getString(R.string.error_PREMIUM_ACCOUNT_REQUIRED))
                    .hideDelayed(2000, TimeUnit.MILLISECONDS)
                return true
            }
            val sender = item.messageSenderId
            if (sender != null) {
                setNewMessageSender(sender)
            } else {
                openSetSenderPopup()
            }
        } else if (viewId == R.id.btn_openSendersMenu) {
            openSetSenderPopup()
        } else if (viewId == R.id.btn_sendOnceOnline) {
            val sendOptions = newSendOptions(MessageSchedulingStateSendWhenOnline())
            send(sendOptions, true)
        } else if (viewId == R.id.btn_sendScheduled) {
            tdlib!!.ui().pickSchedulingState(this, RunnableData { sendOptions: MessageSendOptions? ->
                send(sendOptions, true)
            }, getChatId(), false, false, null, null)
        } else if (viewId == R.id.btn_sendNoMarkdown) {
            if (this.isEditingMessage) {
                saveMessage(false)
            } else {
                pickDateOrProceed(
                    newSendOptions(),
                    SimpleSendCallback { sendOptions: MessageSendOptions?, disableMarkdown: Boolean -> send(sendOptions, false) })
            }
        } else if (viewId == R.id.btn_sendToast) {
            val newText = inputView!!.getOutputText(true)
            val text = TD.toCharSequence(newText)
            showCallbackToast(text)
        } else if (viewId == R.id.btn_sendNoSound) {
            val replyInfo = this.currentReplyId
            val sendOptions = newSendOptions(
                getInputSuggestedPostInfo(replyInfo),
                true
            )
            pickDateOrProceed(sendOptions, SimpleSendCallback { modifiedSendOptions: MessageSendOptions?, disableMarkdown: Boolean ->
                send(modifiedSendOptions, true)
            })
        } else if (viewId == R.id.btn_debugLtrEmoji) {
            pickDateOrProceed(newSendOptions(), SimpleSendCallback { sendOptions: MessageSendOptions?, disableMarkdown: Boolean ->
                send(
                    InputMessageText(
                        TdApi.FormattedText(
                            Text.bidiGenerateTestMessage(), arrayOfNulls<TdApi.TextEntity>(0)
                        ), null, false
                    ), false, sendOptions!!, null
                )
            })
        }
        return true
    }

    fun pickDateOrProceed(initialSendOptions: MessageSendOptions, sendCallback: SimpleSendCallback) {
        if (initialSendOptions.schedulingState == null && areScheduledOnly()) {
            tdlib()!!.ui()
                .showScheduleOptions(this, getChatId(), false, SimpleSendCallback { modifiedSendOptions: MessageSendOptions?, disableMarkdown: Boolean ->
                    if (!isDestroyed()) {
                        sendCallback.onSendRequested(modifiedSendOptions, disableMarkdown)
                    }
                }, null, null)
        } else {
            sendCallback.onSendRequested(initialSendOptions, false)
        }
    }

    override fun allowThemeChanges(): Boolean {
        return !inWallpaperMode()
    }

    override fun getViewForApplyingOffsets(): View? {
        return  /*pagerContentView != null ? null : */topBar
    }

    override fun shouldApplyPlayerMargin(): Boolean {
        return false // pagerContentView != null;
    }

    protected override fun applyPlayerOffset(factor: Float, top: Float): Boolean {
        if (super.applyPlayerOffset(factor, top)) {
            if (messagesView != null) {
                messagesView!!.invalidateDate()
            }
            return true
        }
        return false
    }

    private var wallpaperView: WallpaperView? = null
    private var wallpapersList: RecyclerView? = null

    fun wallpaper(): WallpaperView? {
        return wallpaperView
    }

    override fun performComplexPhotoOpen() {
        val headerChatId = this.headerChatId
        val chat = if (headerChatId == this.chat!!.id) this.chat else tdlib!!.chat(headerChatId)
        if (chat != null && chat.photo != null && !TD.isFileEmpty(chat.photo!!.small)) {
            openFromChat(this, chat, headerCell)
        }
    }

    fun needTabs(): Boolean {
        return this.isSelfChat && !isInForceTouchMode() && !inPreviewMode() && !areScheduledOnly()
    }

    val pagerScrollOffsetInPixels: Float
        get() = getValue()!!.measuredWidth * pagerScrollOffset

    fun onKnownMessageCountChanged(chatId: Long, knownMessageCount: Int) {
        if (getChatId() == chatId) {
            if (previewMode == PREVIEW_MODE_SEARCH && (previewSearchSender != null || previewSearchFilter != null)) {
                updateSearchSubtitle()
            }
            // TODO other cases?
        }
    }

    private fun updateSearchSubtitle() {
        val totalCount = if (manager != null) manager.getKnownTotalMessageCount() else -1

        if (previewSearchFilter != null) {
            if (previewSearchFilter!!.isPinnedFilter()) {
                if (totalCount > 0) {
                    headerCell!!.setForcedSubtitle(Lang.pluralBold(R.string.XPinnedMessages, totalCount.toLong()))
                } else {
                    headerCell!!.setForcedSubtitle(Lang.getString(R.string.PinnedMessages))
                }
            }
            return
        }

        if (totalCount > 0) {
            if (tdlib!!.isSelfSender(previewSearchSender)) {
                headerCell!!.setForcedSubtitle(Lang.pluralBold(R.string.XFoundMessagesFromSelf, totalCount.toLong()))
            } else {
                val sender = previewSearchSender!!
                headerCell!!.setForcedSubtitle(
                    Lang.pluralBold(
                        if (sender.getConstructor() == MessageSenderUser.CONSTRUCTOR) R.string.XFoundMessagesFromUser else R.string.XFoundMessagesFromChat,
                        totalCount.toLong(),
                        tdlib!!.senderName(sender, true)
                    )
                )
            }
        } else {
            if (tdlib!!.isSelfSender(previewSearchSender)) {
                headerCell!!.setForcedSubtitle(Lang.getString(R.string.FoundMessagesFromSelf))
            } else {
                val sender = previewSearchSender!!
                headerCell!!.setForcedSubtitle(
                    Lang.getStringBold(
                        if (sender.getConstructor() == MessageSenderUser.CONSTRUCTOR) R.string.FoundMessagesFromUser else R.string.FoundMessagesFromChat,
                        tdlib!!.senderName(sender, true)
                    )
                )
            }
        }
    }

    private fun updateMessageThreadSubtitle() {
        if (messageThread == null) return
        if (areScheduled) {
            headerCell!!.setForcedSubtitle(Lang.lowercase(Lang.getString(R.string.ScheduledMessages)))
        } else {
            headerCell!!.setForcedSubtitle(messageThread!!.chatHeaderSubtitle())
        }
    }

    private fun updateForcedSubtitle() {
        if (messageThread != null) {
            updateMessageThreadSubtitle()
        } else if (areScheduled) {
            headerCell!!.setForcedSubtitle(Lang.lowercase(Lang.getString(if (this.isSelfChat) R.string.Reminders else R.string.ScheduledMessages)))
        } else {
            headerCell!!.setForcedSubtitle(null)
        }
    }

    override fun supportsBottomInset(): Boolean {
        return !isInForceTouchMode()
    }

    override fun onBottomInsetChanged(extraBottomInset: Int, extraBottomInsetWithoutIme: Int, isImeInset: Boolean) {
        super.onBottomInsetChanged(extraBottomInset, extraBottomInsetWithoutIme, isImeInset)
        if (this.emojiKeyboardLayout != null) {
            emojiKeyboardLayout!!.setExtraBottomInset(extraBottomInset, extraBottomInsetWithoutIme)
        }
        updateBottomWrapOffset()
        iterateMediaTabs(RunnableData { c: SharedBaseController<*>? -> c!!.setBottomInset(extraBottomInset, extraBottomInsetWithoutIme) }
        )
        Views.setLayoutHeight(bottomBar, Screen.dp(48f) + extraBottomInsetWithoutIme)
        Views.setPaddingBottom(bottomBar, extraBottomInsetWithoutIme)
        checkScrollButtonOffsets()
        updateMessagesViewInset()
        onMessagesFrameChanged()
        if (searchControlsForChannel) {
            Views.setLayoutHeight(searchControlsLayout, Screen.dp(48f) + extraBottomInset)
        }
        Views.setPaddingBottom(searchControlsReveal, extraBottomInset)
        if (searchControlsLayout != null && needSearchControlsTranslate()) {
            searchControlsLayout!!.setTranslationY((Screen.dp(49f) + extraBottomInset) * (1f - searchControlsFactor))
        }
        updateSearchControlsInset()
        updateCommandKeyboardHeight()
    }

    private fun updateSearchControlsInset() {
        if (searchControlsLayout != null) {
            for (i in 0..<searchControlsLayout!!.getChildCount()) {
                val view = searchControlsLayout!!.getChildAt(i)
                if (view != null && view !== searchControlsReveal) {
                    Views.setBottomMargin(view, extraBottomInset / 2)
                }
            }
        }
    }

    private fun updateMessagesViewInset() {
        val inset = if (bottomWrap != null && bottomWrap!!.getVisibility() == View.VISIBLE) 0 else extraBottomInset
        val appliedInset = Views.getAppliedBottomInset(messagesView)
        if (inset != appliedInset) {
            manager!!.maintainScrollPositionAndOffset(FutureBool { Views.applyBottomInset(messagesView, inset) })
        }
    }

    override fun onCreateView(context: Context): View? {
        if (!isInForceTouchMode()) {
            UI.setSoftInputMode(UI.getContext(context), Config.DEFAULT_WINDOW_PARAMS)
        }

        contentView = MessagesLayout(context)
        contentView!!.setController(this)
        contentView!!.setLayoutParams(newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        var params: RelativeLayout.LayoutParams?

        headerCell = ChatHeaderView(context, tdlib, this)
        headerCell!!.setPhotoOpenCallback(this)
        when (previewMode) {
            PREVIEW_MODE_EVENT_LOG -> {
                headerCell!!.setForcedSubtitle(Lang.lowercase(Lang.getString(R.string.EventLogAllEvents)))
            }

            PREVIEW_MODE_SEARCH -> {
                if (previewSearchSender != null || previewSearchFilter != null) {
                    updateSearchSubtitle()
                } else {
                    headerCell!!.setForcedSubtitle(Lang.getString(R.string.FoundMessagesQuery, previewSearchQuery))
                    headerCell!!.setCallback(this)
                }
            }

            PREVIEW_MODE_WALLPAPER_OBJECT -> {
                headerDoubleCell = DoubleHeaderView(context)
                headerDoubleCell!!.setThemedTextColor(this)
                headerDoubleCell!!.initWithMargin(Screen.dp(12f), true)
                headerDoubleCell!!.setTitle(getName())
                if (getArguments()!!.wallpaperObject!!.document != null) {
                    headerDoubleCell!!.setSubtitle(Strings.buildSize(getArguments()!!.wallpaperObject!!.document!!.document.size))
                } else if (getArguments()!!.wallpaperObject!!.type.getConstructor() == TdApi.BackgroundTypePattern.CONSTRUCTOR) {
                    headerDoubleCell!!.setSubtitle(Lang.getString(R.string.ChatBackgroundTypePattern))
                } else if (getArguments()!!.wallpaperObject!!.type.getConstructor() == BackgroundTypeFill.CONSTRUCTOR) {
                    val filledWp = getArguments()!!.wallpaperObject!!.type as BackgroundTypeFill

                    when (filledWp.fill.getConstructor()) {
                        TdApi.BackgroundFillGradient.CONSTRUCTOR -> headerDoubleCell!!.setSubtitle(Lang.getString(R.string.ChatBackgroundTypeGradient))
                        TdApi.BackgroundFillFreeformGradient.CONSTRUCTOR -> headerDoubleCell!!.setSubtitle(Lang.getString(R.string.ChatBackgroundTypeMulticolor))
                        TdApi.BackgroundFillSolid.CONSTRUCTOR -> headerDoubleCell!!.setSubtitle(Lang.getString(R.string.ChatBackgroundTypeSolid))
                        else -> throw UnsupportedOperationException(filledWp.fill.toString())
                    }
                }
            }

            else -> {}
        }
        headerCell!!.initWithController(this, true)

        wallpaperView = WallpaperView(context, manager, tdlib)
        if (previewMode == PREVIEW_MODE_WALLPAPER_OBJECT) {
            wallpaperView!!.initWithCustomWallpaper(TGBackground(tdlib, getArguments()!!.wallpaperObject))
        } else {
            wallpaperView!!.initWithSetupMode(previewMode == PREVIEW_MODE_WALLPAPER)
        }
        wallpaperView!!.setLayoutParams(RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        addThemeInvalidateListener(wallpaperView)

        params = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        params.addRule(RelativeLayout.ABOVE, R.id.msg_bottom)

        val messagesManager: MessagesLayoutManager?

        messagesManager = MessagesLayoutManager(context, RecyclerView.VERTICAL, true)
        messagesManager.setManager(manager)

        messagesView = Views.inflate(context(), R.layout.recycler_messages, contentView) as MessagesRecyclerView?
        if (isInForceTouchMode()) {
            messagesView!!.setVerticalScrollBarEnabled(false)
        }
        addThemeInvalidateListener(messagesView)
        messagesView!!.setId(R.id.msg_list)
        messagesView!!.setManager(manager)
        messagesView!!.setController(this)
        messagesView!!.setHasFixedSize(true)
        messagesView!!.setLayoutManager(messagesManager)
        messagesView!!.setLayoutParams(params)
        Views.setScrollBarPosition(messagesView)
        if (Config.HARDWARE_MESSAGES_LIST) {
            Views.setLayerType(messagesView, View.LAYER_TYPE_HARDWARE)
        }

        manager!!.modifyRecycler(context, messagesView, messagesManager)

        params = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0)
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)

        bottomSpace = FillingSpace(context)
        bottomSpace!!.setLayoutParams(params)
        bottomSpace!!.setThemedBackground(ColorId.filling, this)

        params = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)

        bottomWrap = object : LinearLayout(context) {
            var lastHeight: Int = 0

            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec)
                updateButtonsY()
            }

            override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
                super.onLayout(changed, l, t, r, b)
                updateButtonsY()
            }
        }
        bottomWrap!!.setId(R.id.msg_bottom)
        bottomWrap!!.setOrientation(LinearLayout.VERTICAL)
        bottomWrap!!.setMinimumHeight(Screen.dp(49f))
        bottomWrap!!.setLayoutParams(params)

        if (previewMode == PREVIEW_MODE_NONE && !isInForceTouchMode()) {
            inputView = object : InputView(context, tdlib, this) {
                override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
                    updateButtonsY()
                }

                override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
                    super.onLayout(changed, left, top, right, bottom)
                    updateButtonsY()
                }

                override fun onTouchEvent(event: MotionEvent): Boolean {
                    val r = super.onTouchEvent(event)
                    inputViewDisabledClickHelper.onTouchEvent(this, event)
                    if (emojiKeyboardLayout != null) {
                        emojiKeyboardLayout!!.contentView.textFormattingLayout.onInputViewTouchEvent(event)
                    }
                    return r
                }
            }
            inputView!!.setNoPersonalizedLearning(Settings.instance().needsIncognitoMode(chat))
            inputView!!.setId(R.id.msg_input)
            inputView!!.setTextColor(Theme.textAccentColor())
            addThemePaintColorListener(inputView!!.getPlaceholderPaint(), ColorId.textPlaceholder)
            addThemeTextColorListener(inputView, ColorId.text)
            inputView!!.setHintTextColor(Theme.textPlaceholderColor())
            addThemeHintTextColorListener(inputView, ColorId.textPlaceholder)
            inputView!!.setLinkTextColor(Theme.textLinkColor())
            addThemeLinkTextColorListener(inputView, ColorId.textLink)
            ViewSupport.setThemedBackground(inputView, ColorId.filling, this)
            inputView!!.setHighlightColor(Theme.fillingTextSelectionColor())
            addThemeHighlightColorListener(inputView, ColorId.textSelectionHighlight)
            bindLocaleChanger(inputView!!.setController(this))
            if (inPreviewMode) {
                inputView!!.setEnabled(false)
                inputView!!.setInputPlaceholder(R.string.Message)
            }
            inputView!!.setSelectionChangeListener(this)
            inputView!!.setSpanChangeListener(SpanChangeListener { view: InputView? -> this.onInputSpansChanged(view) })
        }

        if (!inPreviewMode) {
            params = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Screen.dp(48f))
            params.addRule(RelativeLayout.ALIGN_TOP, R.id.msg_bottom)

            replyBarView = ReplyBarView(context(), tdlib)
            ViewSupport.setThemedBackground(replyBarView, ColorId.filling, this)
            replyBarView!!.setId(R.id.msg_bottomReply)
            replyBarView!!.setAnimationsDisabled(true)
            replyBarView!!.initWithCallback(this, this)
            replyBarView!!.setOnClickListener(this)
            replyBarView!!.setLayoutParams(params)
        }

        params = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP)
        params.addRule(RelativeLayout.ALIGN_TOP, R.id.msg_bottom)

        topBar = CollapseListView(context)
        topBar!!.setLayoutParams(params)
        topBar!!.setTotalHeightChangeListener(TotalHeightChangeListener { listView: CollapseListView? -> onMessagesFrameChanged() })

        val liveLocationHeight = Screen.dp(36f)
        liveLocationView = object : View(context) {
            override fun onTouchEvent(e: MotionEvent?): Boolean {
                return liveLocation != null && liveLocation!!.onTouchEvent(e)
            }

            override fun onDraw(c: Canvas) {
                c.drawRect(0f, 0f, getMeasuredWidth().toFloat(), Screen.dp(36f).toFloat(), Paints.fillingPaint(Theme.fillingColor()))
                if (liveLocation != null) {
                    liveLocation!!.draw(c, 0)
                }
            }
        }
        liveLocationView!!.setLayoutParams(newParams(ViewGroup.LayoutParams.MATCH_PARENT, liveLocationHeight))
        addThemeInvalidateListener(liveLocationView)

        val actionBarHeight = Screen.dp(36f)
        actionView = TopBarView(context)
        actionView!!.setDismissListener(TopBarView.DismissListener { barView: TopBarView? -> dismissActionBar() }
        )
        actionView!!.setLayoutParams(FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, actionBarHeight))
        actionView!!.addThemeListeners(this)

        val requestsViewHeight = Screen.dp(48f)
        requestsView = JoinRequestsView(context, tdlib)
        requestsView!!.setLayoutParams(FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, requestsViewHeight))
        requestsView!!.setOnClickListener(View.OnClickListener { v: View? -> ModernActionedLayout.showJoinRequests(this, chat!!.id, requestsView!!.getInfo()) })
        requestsView!!.setOnDismissRunnable(Runnable { tdlib!!.settings().dismissRequests(chat!!.id, requestsView!!.getInfo()) })
        tdlib!!.settings().addJoinRequestsDismissListener(this)
        RippleSupport.setSimpleWhiteBackground(requestsView!!, this)
        Views.setClickable(requestsView)

        toastAlertView = CustomTextView(context, tdlib)
        toastAlertItem = object : CollapseListView.Item {
            override fun getVisualHeight(): Int {
                return toastAlertView!!.getCurrentHeight(toastAlertView!!.getMeasuredWidth())
            }

            override fun allowCollapse(): Boolean {
                return false
            }

            override fun getValue(): View {
                return toastAlertView!!
            }
        }
        toastAlertView!!.setTextSize(15f)
        addThemeTextAccentColorListener(toastAlertView)
        ViewSupport.setThemedBackground(toastAlertView, ColorId.filling, this)
        toastAlertView!!.setTextColorId(ColorId.text)
        toastAlertView!!.setPadding(Screen.dp(16f), Screen.dp(8f), Screen.dp(16f), Screen.dp(8f))
        toastAlertView!!.setHeightChangeListener(CustomTextView.HeightChangeListener { v: CustomTextView?, newHeight: Int ->
            topBar!!.notifyItemHeightChanged(
                toastAlertItem
            )
        })

        pinnedMessagesBar = object : PinnedMessagesBar(context, true) {
            override fun onViewportChanged() {
                super.onViewportChanged()
                topBar!!.notifyItemHeightChanged(pinnedMessagesItem)
            }
        }
        pinnedMessagesBar!!.setAnimationsDisabled(true)
        pinnedMessagesBar!!.initialize(this)
        pinnedMessagesBar!!.setMessageListener(object : PinnedMessagesBar.MessageListener {
            override fun onMessageClick(view: PinnedMessagesBar?, message: TdApi.Message, quote: InputTextQuote?) {
                highlightMessage(MessageId(message.chatId, message.id))
            }

            override fun onDismissRequest(view: PinnedMessagesBar?) {
                dismissPinnedMessage()
            }

            override fun onShowAllRequest(view: PinnedMessagesBar?) {
                val c = MessagesController(context, tdlib)
                c.setArguments(Arguments(null, chat, null, null, SearchMessagesFilterPinned()))
                navigateTo(c)
            }
        })
        pinnedMessagesItem = object : CollapseListView.Item {
            override fun getVisualHeight(): Int {
                return pinnedMessagesBar!!.getTotalHeight()
            }

            override fun getValue(): View? {
                return pinnedMessagesBar
            }

            override fun onCompletelyHidden() {
                pinnedMessagesBar!!.collapse(false)
            }
        }

        topBar!!.initWithList(
            arrayOf<CollapseListView.Item?>( // TODO voice chat bar
                pinnedMessagesItem,
                ViewItem(requestsView, requestsViewHeight).also {
                    requestsItem = it
                },
                ViewItem(liveLocationView, liveLocationHeight).also {
                    liveLocationItem = it
                },
                ViewItem(actionView, actionBarHeight).also {
                    actionItem = it
                },
                toastAlertItem
            ), this
        )

        // Mention button
        params = RelativeLayout.LayoutParams(Screen.dp(118f), Screen.dp(74f))
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
        params.addRule(RelativeLayout.ABOVE, R.id.msg_bottom)

        mentionButtonWrap = FrameLayoutFix(context)
        mentionButtonWrap!!.setLayoutParams(params)
        setMentionButtonFactor(0f)

        val padding = Screen.dp(4f)
        var fparams = newParams(Screen.dp(24f) * 2 + padding * 2, Screen.dp(24f) * 2 + padding * 2, Gravity.RIGHT or Gravity.BOTTOM)
        params.bottomMargin = Screen.dp(16f) - padding
        params.rightMargin = params.bottomMargin

        mentionButton = CircleButton(context)
        mentionButton!!.setId(R.id.btn_mention)
        mentionButton!!.setOnClickListener(this)
        mentionButton!!.setOnLongClickListener(OnLongClickListener { v: View? ->
            val chatId = getChatId()
            if (chatId != 0L && !isDestroyed()) {
                tdlib!!.send<TdApi.Ok?>(ReadAllChatMentions(chatId), tdlib!!.typedOkHandler())
                return@OnLongClickListener true
            }
            false
        })
        addThemeInvalidateListener(mentionButton)
        mentionButton!!.init(R.drawable.baseline_alternate_email_24, 48f, 4f, ColorId.circleButtonChat, ColorId.circleButtonChatIcon)
        mentionButton!!.setLayoutParams(fparams)
        mentionButtonWrap!!.addView(mentionButton)

        val buttonPadding = Screen.dp(24f)
        fparams = newParams(buttonPadding + fparams.width, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.RIGHT or Gravity.BOTTOM)
        fparams.bottomMargin = Screen.dp(24f) * 2 - Screen.dp(28f) / 2

        mentionCountView = CounterBadgeView(context)
        mentionCountView!!.setLayoutParams(fparams)
        mentionCountView!!.setPadding(buttonPadding, 0, 0, 0)
        addThemeInvalidateListener(mentionCountView)
        mentionButtonWrap!!.addView(mentionCountView)
        mentionButton!!.setTag(mentionCountView)

        // Scroll button
        params = RelativeLayout.LayoutParams(Screen.dp(118f), Screen.dp(74f))
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
        params.addRule(RelativeLayout.ABOVE, R.id.msg_bottom)

        scrollToBottomButtonWrap = FrameLayoutFix(context)
        scrollToBottomButtonWrap!!.setLayoutParams(params)
        scrollToBottomButtonWrap!!.setAlpha(0f)
        if (isInForceTouchMode()) {
            scrollToBottomButtonWrap!!.setTranslationY((-Screen.dp(16f) + Screen.dp(4f)).toFloat())
        }

        fparams = newParams(Screen.dp(24f) * 2 + padding * 2, Screen.dp(24f) * 2 + padding * 2, Gravity.RIGHT or Gravity.BOTTOM)
        params.bottomMargin = Screen.dp(16f) - padding
        params.rightMargin = params.bottomMargin

        scrollToBottomButton = CircleButton(context())
        scrollToBottomButton!!.setId(R.id.btn_scroll)
        scrollToBottomButton!!.setOnClickListener(this)
        scrollToBottomButton!!.setOnLongClickListener(OnLongClickListener { v: View? ->
            manager.scrollToStart(true)
            true
        })
        addThemeInvalidateListener(scrollToBottomButton)
        scrollToBottomButton!!.init(R.drawable.baseline_arrow_downward_24, 48f, 4f, ColorId.circleButtonChat, ColorId.circleButtonChatIcon)
        scrollToBottomButton!!.setLayoutParams(fparams)
        scrollToBottomButtonWrap!!.addView(scrollToBottomButton)

        fparams = newParams(buttonPadding + fparams.width, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.RIGHT or Gravity.BOTTOM)
        fparams.bottomMargin = Screen.dp(24f) * 2 - Screen.dp(28f) / 2

        unreadCountView = CounterBadgeView(context)
        unreadCountView!!.setPadding(buttonPadding, 0, 0, 0)
        unreadCountView!!.setLayoutParams(fparams)
        addThemeInvalidateListener(unreadCountView)
        scrollToBottomButtonWrap!!.addView(unreadCountView)
        scrollToBottomButton!!.setTag(unreadCountView)

        // Reaction button
        params = RelativeLayout.LayoutParams(Screen.dp(118f), Screen.dp(74f))
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
        params.addRule(RelativeLayout.ABOVE, R.id.msg_bottom)

        reactionsButtonWrap = FrameLayoutFix(context)
        reactionsButtonWrap!!.setLayoutParams(params)

        fparams = newParams(Screen.dp(24f) * 2 + padding * 2, Screen.dp(24f) * 2 + padding * 2, Gravity.RIGHT or Gravity.BOTTOM)
        params.bottomMargin = Screen.dp(16f) - padding
        params.rightMargin = params.bottomMargin

        reactionsButton = CircleButton(context(), tdlib)
        reactionsButton!!.setId(R.id.btn_reaction)
        reactionsButton!!.setOnClickListener(this)
        addThemeInvalidateListener(reactionsButton)
        reactionsButton!!.init(R.drawable.baseline_favorite_20, 48f, 4f, ColorId.circleButtonChat, ColorId.circleButtonChatIcon)
        reactionsButton!!.setLayoutParams(fparams)
        reactionsButton!!.setOnClickListener(this)
        reactionsButton!!.setOnLongClickListener(OnLongClickListener { v: View? ->
            val chatId = getChatId()
            if (chatId != 0L && !isDestroyed()) {
                tdlib!!.send<TdApi.Ok?>(ReadAllChatReactions(chatId), tdlib!!.typedOkHandler())
                return@OnLongClickListener true
            }
            false
        })

        fparams = newParams(buttonPadding + fparams.width, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.RIGHT or Gravity.BOTTOM)
        fparams.bottomMargin = Screen.dp(24f) * 2 - Screen.dp(28f) / 2

        reactionsCountView = CounterBadgeView(context)
        reactionsCountView!!.setLayoutParams(fparams)
        reactionsCountView!!.setPadding(buttonPadding, 0, 0, 0)
        addThemeInvalidateListener(reactionsCountView)
        reactionsButton!!.setTag(reactionsCountView)
        reactionsButtonWrap!!.addView(reactionsButton)
        reactionsButtonWrap!!.addView(reactionsCountView)

        setReactionButtonFactor(0f)

        goToNextFoundMessageButtonBadge = CircleCounterBadgeView(this, R.id.btn_search_next, this, null)
        goToNextFoundMessageButtonBadge!!.init(R.drawable.baseline_keyboard_arrow_up_24, 48f, 4f, ColorId.circleButtonChat, ColorId.circleButtonChatIcon)
        goToNextFoundMessageButtonBadge!!.setTranslationX(CircleCounterBadgeView.BUTTON_WRAPPER_WIDTH.toFloat())
        goToNextFoundMessageButtonBadge!!.setTranslationY(Screen.dp((16 - 74).toFloat()).toFloat())
        goToNextFoundMessageButtonBadge!!.setEnabled(false, false)

        goToPrevFoundMessageButtonBadge = CircleCounterBadgeView(this, R.id.btn_search_prev, this, null)
        goToPrevFoundMessageButtonBadge!!.init(R.drawable.baseline_keyboard_arrow_down_24, 48f, 4f, ColorId.circleButtonChat, ColorId.circleButtonChatIcon)
        goToPrevFoundMessageButtonBadge!!.setTranslationX(CircleCounterBadgeView.BUTTON_WRAPPER_WIDTH.toFloat())
        goToPrevFoundMessageButtonBadge!!.setEnabled(false, false)
        searchNavigationButtonVisibleAnimator.setValue(false, false)

        searchByUserViewWrapper = ChatSearchMembersView(context, this)
        searchByUserViewWrapper!!.setVisibility(View.GONE)
        searchByUserViewWrapper!!.setDelegate(object : ChatSearchMembersView.Delegate {
            override fun onSetMessageSender(sender: MessageSender?) {
                onSetSearchMessagesSenderId(sender)
                showSearchByUserView(false, true)
                searchChatMessages(getLastMessageSearchQuery())
            }

            override fun onClose() {
                showSearchByUserView(false, true)
            }
        })

        // Shadow & bottom controls
        params = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Screen.dp(1f))
        params.addRule(RelativeLayout.ABOVE, R.id.msg_bottom)
        bottomShadowView = SeparatorView.simpleSeparator(context, params, false)
        bottomShadowView!!.setAlignBottom()
        addThemeInvalidateListener(bottomShadowView)
        bottomShadowView!!.setId(R.id.msg_bottomShadow)

        params = RelativeLayout.LayoutParams(Screen.dp(55f), Screen.dp(49f))
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP)
        if (Lang.rtl()) {
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
        } else {
            params.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
        }

        emojiButton = ImageView(context)
        emojiButton!!.setId(R.id.msg_emoji)
        emojiButton!!.setScaleType(ImageView.ScaleType.CENTER)
        emojiButton!!.setImageResource(EmojiLayout.getTargetIcon(true))
        emojiButton!!.setColorFilter(Theme.iconColor())
        addThemeFilterListener(emojiButton, ColorId.icon)
        emojiButton!!.setOnClickListener(this)
        emojiButton!!.setLayoutParams(params)

        params = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, Screen.dp(49f))
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP)
        if (Lang.rtl()) {
            params.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
        } else {
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
        }

        attachButtons = object : AttachLinearLayout(context) {
            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec)
                if (inputView != null && getMeasuredWidth() > 0) {
                    inputView!!.checkPlaceholderWidth()
                }
                if (messageSenderButton != null) {
                    messageSenderButton!!.checkPosition()
                }
            }
        }
        attachButtons!!.setOrientation(LinearLayout.HORIZONTAL)
        attachButtons!!.setLayoutParams(params)

        params = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Screen.dp(49f))
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP)

        messageSenderButton = MessageSenderButton(context, this)
        messageSenderButton!!.setLayoutParams(params)
        messageSenderButton!!.setDelegate(object : MessageSenderButton.Delegate {
            override fun onClick() {
                openSetSenderPopup()
            }
        })
        messageSenderButton!!.setOnLongClickListener(OnLongClickListener { v: View? ->
            if (canSelectSender()) {
                getChatAvailableMessagesSenders(Runnable { sendAsMenu!!.openMenu(messageSenderButton!!.getButtonView()) })
            }
            true
        })

        val ATTACH_BUTTONS_WIDTH = 47f

        var lp = LinearLayout.LayoutParams(Screen.dp(ATTACH_BUTTONS_WIDTH), Screen.dp(49f))

        mediaButton = CameraAccessImageView(context, this)
        mediaButton!!.setId(R.id.msg_attach)
        mediaButton!!.setScaleType(ImageView.ScaleType.CENTER)
        mediaButton!!.setImageResource(R.drawable.deproko_baseline_attach_26)
        mediaButton!!.setColorFilter(Theme.iconColor())
        addThemeFilterListener(mediaButton, ColorId.icon)
        mediaButton!!.setOnClickListener(this)
        mediaButton!!.setLayoutParams(lp)


        lp = LinearLayout.LayoutParams(Screen.dp(ATTACH_BUTTONS_WIDTH), Screen.dp(49f))
        cameraButton = CameraAccessImageView(context, this)
        cameraButton!!.setId(R.id.btn_camera)
        cameraButton!!.setScaleType(ImageView.ScaleType.CENTER)
        cameraButton!!.setImageResource(R.drawable.deproko_baseline_camera_26)
        cameraButton!!.setColorFilter(Theme.iconColor())
        addThemeFilterListener(cameraButton, ColorId.icon)
        cameraButton!!.setOnClickListener(this)
        cameraButton!!.setLayoutParams(lp)

        if (!areScheduled /*&& isSelfChat()*/) {
            lp = LinearLayout.LayoutParams(Screen.dp(ATTACH_BUTTONS_WIDTH), Screen.dp(49f))
            scheduleButton = CameraAccessImageView(context, this)
            scheduleButton!!.setId(R.id.btn_viewScheduled)
            scheduleButton!!.setScaleType(ImageView.ScaleType.CENTER)
            scheduleButton!!.setImageResource(R.drawable.baseline_date_range_24)
            scheduleButton!!.setColorFilter(Theme.iconColor())
            addThemeFilterListener(scheduleButton, ColorId.icon)
            scheduleButton!!.setOnClickListener(this)
            scheduleButton!!.setLayoutParams(lp)
        }

        lp = LinearLayout.LayoutParams(Screen.dp(ATTACH_BUTTONS_WIDTH), Screen.dp(49f))

        commandButton = InvisibleImageView(context)
        commandButton!!.setId(R.id.msg_command)
        commandButton!!.setColorFilter(Theme.iconColor())
        addThemeFilterListener(commandButton, ColorId.icon)
        commandButton!!.setScaleType(ImageView.ScaleType.CENTER)
        commandButton!!.setOnClickListener(this)
        commandButton!!.setVisibility(View.INVISIBLE)
        commandButton!!.setLayoutParams(lp)
        Views.setClickable(commandButton)
        updateCommandButton(0)

        if (Config.NEED_SILENT_BROADCAST) {
            lp = LinearLayout.LayoutParams(Screen.dp(ATTACH_BUTTONS_WIDTH), Screen.dp(49f))

            silentButton = SilentButton(context)
            silentButton!!.setId(R.id.btn_silent)
            silentButton!!.setOnClickListener(this)
            silentButton!!.setLayoutParams(lp)
            addThemeInvalidateListener(silentButton)
            updateSilentButton(false)
        }

        lp = LinearLayout.LayoutParams(Screen.dp(ATTACH_BUTTONS_WIDTH), Screen.dp(49f))
        lp.rightMargin = Screen.dp(2f)

        recordButton = VoiceVideoButtonView(context)
        recordButton!!.setPadding(0, 0, Screen.dp(2f), 0)
        recordButton!!.setHasTouchControls(true)
        addThemeInvalidateListener(recordButton)
        recordButton!!.setLayoutParams(lp)

        attachButtons!!.addView(commandButton)
        if (silentButton != null) {
            attachButtons!!.addView(silentButton)
        }
        if (scheduleButton != null) {
            attachButtons!!.addView(scheduleButton)
        }
        if (cameraButton != null) {
            attachButtons!!.addView(cameraButton)
        }
        attachButtons!!.addView(mediaButton)
        attachButtons!!.addView(recordButton)
        attachButtons!!.updatePivot()

        params = RelativeLayout.LayoutParams(Screen.dp(55f), Screen.dp(49f))
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP)
        if (Lang.rtl()) {
            params.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
        } else {
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
        }

        sendButton = SendButton(context, if (areScheduled) R.drawable.dotvhs_baseline_send_schedule_24 else R.drawable.deproko_baseline_send_24)
        sendButton!!.setIgnoreDrawMessageSender()
        sendButton!!.setOnClickListener(this)
        addThemeInvalidateListener(sendButton)
        sendButton!!.setId(R.id.msg_send)
        sendButton!!.setVisibility(View.INVISIBLE)
        sendButton!!.setAlpha(0f)
        sendButton!!.setLayoutParams(params)

        sendMenu = HapticMenuHelper(this, this, getThemeListeners(), null).attachToView(sendButton)
        sendAsMenu =
            HapticMenuHelper(hapticSendAsMenuProvider, this, getThemeListeners(), null).selectableMode(messageSenderButton).hapticListener(messageSenderButton)
        messageSenderButton!!.setHapticMenuHelper(sendAsMenu)

        if (inPreviewMode) {
            when (previewMode) {
                PREVIEW_MODE_WALLPAPER -> {
                    wallpapersList = WallpaperRecyclerView(context)
                    wallpapersList!!.setLayoutManager(LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, Lang.rtl()))
                    wallpapersList!!.setAdapter(WallpaperAdapter(this, ThemeManager.instance().currentTheme(false).getId()))
                    wallpapersList!!.addItemDecoration(object : RecyclerView.ItemDecoration() {
                        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                            val spacing = Screen.dp(3f)
                            outRect.bottom = spacing
                            outRect.top = outRect.bottom
                            val holder = parent.getChildViewHolder(view)
                            val position = if (holder != null) holder.getBindingAdapterPosition() else RecyclerView.NO_POSITION
                            if (holder == null || holder.getItemViewType() != 0 || position == RecyclerView.NO_POSITION) {
                                outRect.right = 0
                                outRect.left = outRect.right
                            } else {
                                if (Lang.rtl()) {
                                    outRect.right = spacing // position == 0 ? spacing : spacing / 2 + spacing % 2
                                    outRect.left = if (position == parent.getAdapter()!!.getItemCount() - 1) spacing else 0
                                } else {
                                    outRect.left = spacing
                                    outRect.right = if (position == parent.getAdapter()!!.getItemCount() - 1) spacing else 0
                                }
                            }
                        }
                    })
                    wallpapersList!!.setLayoutParams(LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Screen.dp(105f) + Screen.dp(3f) * 2))
                    ViewSupport.setThemedBackground(wallpapersList, ColorId.filling, this)
                    bottomWrap!!.addView(wallpapersList)
                }

                PREVIEW_MODE_WALLPAPER_OBJECT -> {
                    ViewSupport.setThemedBackground(wallpapersList, ColorId.filling, this)
                }

                PREVIEW_MODE_FONT_SIZE -> {
                    val textWrap = FrameLayoutFix(context)
                    textWrap.setLayoutParams(LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Screen.dp(49f)))
                    ViewSupport.setThemedBackground(textWrap, ColorId.filling, this)

                    val tv1: TextView = NoScrollTextView(context)
                    tv1.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f)
                    tv1.setText("T")
                    tv1.setTypeface(Fonts.getRobotoBold())
                    tv1.setTextColor(Theme.textDecentColor())
                    addThemeTextDecentColorListener(tv1)
                    tv1.setGravity(Gravity.CENTER)
                    tv1.setLayoutParams(newParams(Screen.dp(46f), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.LEFT))
                    textWrap.addView(tv1)

                    val tv2: TextView = NoScrollTextView(context)
                    tv2.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18f)
                    tv2.setText("T")
                    tv2.setGravity(Gravity.CENTER)
                    tv2.setPadding(0, 0, 0, Screen.dp(1f))
                    tv2.setTypeface(Fonts.getRobotoBold())
                    tv2.setTextColor(Theme.textDecentColor())
                    addThemeTextDecentColorListener(tv2)
                    tv2.setLayoutParams(newParams(Screen.dp(46f), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.RIGHT))
                    textWrap.addView(tv2)

                    fontSliderView = SliderView(context)
                    addThemeInvalidateListener(fontSliderView)
                    fontSliderView!!.setSlideEnabled(true, false)
                    fontSliderView!!.setValueCount(Settings.CHAT_FONT_SIZES.size)
                    Companion.updateFontSliderValue(fontSliderView!!)
                    fontSliderView!!.setListener(object : SliderView.Listener {
                        override fun onSetStateChanged(view: SliderView, isSetting: Boolean) {
                            if (!isSetting) {
                                val index = Math.round(view.getValue() * (Settings.CHAT_FONT_SIZES.size - 1).toFloat())
                                view.animateValue(index.toFloat() / (Settings.CHAT_FONT_SIZES.size - 1).toFloat())
                            }
                        }

                        override fun onValueChanged(view: SliderView?, factor: Float) {
                            val index = Math.round(factor * (Settings.CHAT_FONT_SIZES.size - 1).toFloat())
                            if (Settings.instance().setChatFontSize(Settings.CHAT_FONT_SIZES[index])) {
                                manager.onUpdateTextSize()
                                manager.rebuildLayouts()
                            }
                        }

                        override fun allowSliderChanges(view: SliderView?): Boolean {
                            return true
                        }
                    })
                    fontSliderView!!.setLayoutParams(newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                    fontSliderView!!.setForceBackgroundColorId(ColorId.sliderInactive)
                    fontSliderView!!.setPadding(tv2.getLayoutParams().width, 0, tv2.getLayoutParams().width, 0)
                    fontSliderView!!.setColorId(ColorId.sliderActive, false)
                    textWrap.addView(fontSliderView)

                    bottomWrap!!.addView(textWrap)
                }

                PREVIEW_MODE_NONE -> {
                    bottomWrap!!.addView(inputView)
                }
            }
        } else if (inputView != null) {
            bottomWrap!!.addView(inputView)
        }

        if (inWallpaperMode()) {
            val currentBackgroundObj = getArgumentsStrict()!!.wallpaperObject
            val currentBackground = tdlib!!.settings().getWallpaper(Theme.getWallpaperIdentifier())
            val shouldUseParams =
                !inWallpaperPreviewMode() || currentBackgroundObj != null && currentBackgroundObj.type.getConstructor() == BackgroundTypeWallpaper.CONSTRUCTOR

            if (shouldUseParams) {
                val height = Screen.dp(49f)
                backgroundParamsView = WallpaperParametersView(context)
                params = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, height)
                params.addRule(RelativeLayout.ABOVE, R.id.msg_bottom)
                backgroundParamsView!!.setLayoutParams(params)

                wallpaperViewBlurPreview = WallpaperView(context, manager, tdlib)
                wallpaperViewBlurPreview!!.setLayoutParams(
                    RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                wallpaperViewBlurPreview!!.setAlpha(0f)

                if (inWallpaperPreviewMode() && currentBackgroundObj != null) {
                    backgroundParamsView!!.initWith(currentBackgroundObj, object : WallpaperParametersListener {
                        override fun onBlurValueAnimated(factor: Float) {
                            wallpaperViewBlurPreview!!.setAlpha(clamp(factor))
                        }
                    })

                    wallpaperViewBlurPreview!!.initWithCustomWallpaper(TGBackground(tdlib, currentBackgroundObj, !backgroundParamsView!!.isBlurred()))
                    messagesView!!.setTranslationY(-height.toFloat())
                } else {
                    backgroundParamsView!!.initWith(currentBackground, object : WallpaperParametersListener {
                        override fun onParametersViewScaleChanged(factor: Float) {
                            val clamped = clamp(factor)
                            backgroundParamsView!!.setTranslationY(height * (1f - clamped))
                            messagesView!!.setTranslationY(-(height * clamped))
                        }

                        override fun onBlurValueAnimated(factor: Float) {
                            wallpaperViewBlurPreview!!.setAlpha(clamp(factor))
                        }
                    })

                    val previewBlurValue = !backgroundParamsView!!.isBlurred()
                    wallpaperViewBlurPreview!!.initWithCustomWallpaper(TGBackground.newBlurredWallpaper(tdlib, currentBackground, previewBlurValue))
                    wallpaperViewBlurPreview!!.setSelfBlur(previewBlurValue)
                    wallpaperView!!.setSelfBlur(!previewBlurValue)
                    backgroundParamsView!!.setParametersAvailability(currentBackground != null && currentBackground.isWallpaper(), false)
                    if (currentBackground != null && currentBackground.isWallpaper()) {
                        messagesView!!.setTranslationY(-height.toFloat())
                    }
                }
            }
        }

        // Bottom bar
        params = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Screen.dp(48f) + extraBottomInsetWithoutIme)
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)

        bottomBar = object : ChatBottomBarView(context, tdlib) {
            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec)
                updateBottomBarStyle()
            }
        }
        Views.setPaddingBottom(bottomBar, extraBottomInsetWithoutIme)
        bottomBar!!.setOnClickListener(this)
        bottomBar!!.setLayoutParams(params)
        addThemeInvalidateListener(bottomBar)
        checkScrollButtonOffsets()
        updateMessagesViewInset()

        if (previewMode == PREVIEW_MODE_WALLPAPER_OBJECT) {
            showBottomButton(BOTTOM_ACTION_APPLY_WALLPAPER, 0, false)
            bottomShadowView!!.setVisibility(View.GONE)
        }

        // Setup
        contentView!!.addView(wallpaperView)
        if (wallpaperViewBlurPreview != null) {
            contentView!!.addView(wallpaperViewBlurPreview)
        }
        if (!inPreviewMode) {
            contentView!!.addView(replyBarView)
        }
        contentView!!.addView(bottomSpace)
        contentView!!.addView(bottomWrap)
        contentView!!.addView(messagesView)
        contentView!!.addView(bottomShadowView)

        contentView!!.addView(topBar)
        contentView!!.addView(bottomBar)
        contentView!!.addView(reactionsButtonWrap)
        contentView!!.addView(mentionButtonWrap)
        contentView!!.addView(scrollToBottomButtonWrap)
        contentView!!.addView(goToNextFoundMessageButtonBadge)
        contentView!!.addView(goToPrevFoundMessageButtonBadge)

        if (previewMode == PREVIEW_MODE_NONE) {
            contentView!!.addView(emojiButton)
            contentView!!.addView(attachButtons)
            contentView!!.addView(sendButton)
            contentView!!.addView(messageSenderButton)

            initSearchControls()
            contentView!!.addView(searchControlsLayout)

            addStaticListeners()
        }

        if (backgroundParamsView != null) {
            contentView!!.addView(backgroundParamsView, 2)
        }

        contentView!!.addView(searchByUserViewWrapper)

        updateView()

        TGLegacyManager.instance().addEmojiListener(this)

        if (needTabs()) {
            pagerHeaderView = ViewPagerHeaderViewCompact(context)
            pagerHeaderView!!.getTopView().setShowLabelOnActiveOnly(!Settings.instance().needReduceMotion())
            addThemeInvalidateListener(pagerHeaderView!!.getTopView())
            fparams = pagerHeaderView!!.getRecyclerView().getLayoutParams() as FrameLayout.LayoutParams
            fparams.leftMargin = Screen.dp(56f)
            fparams.rightMargin = Screen.dp(56f)
            pagerHeaderView!!.getTopView().setOnItemClickListener(this)
            addThemeInvalidateListener(pagerHeaderView!!.getTopView())

            val mediaControllers: MutableList<SharedBaseController<*>> = ArrayList<SharedBaseController<*>>(8)
            ProfileController.fillMediaControllers(mediaControllers, context(), tdlib())
            val items: MutableList<ViewPagerTopView.Item?> = ArrayList<ViewPagerTopView.Item?>(mediaControllers.size + 1)
            items.add(
                ViewPagerTopView.Item(
                    Lang.getString(R.string.TabMessages).uppercase(Locale.getDefault()),
                    R.drawable.baseline_chat_bubble_24,
                    null
                )
            )
            for (c in mediaControllers) {
                items.add(
                    ViewPagerTopView.Item(
                        c.getName().toString().uppercase(Locale.getDefault()),
                        c.icon,
                        null
                    )
                )
            }
            pagerHeaderView!!.getTopView().setItems(items)

            pagerContentAdapter = MediaTabsAdapter(this, mediaControllers)

            pagerContentView = object : RtlViewPager(context) {
                var blocked: Boolean = false

                override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
                    if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                        blocked = false
                    }
                    if (!blocked) {
                        blocked = getCurrentItem() == 0 && (UI.getContext(this@MessagesController.context()).getRecordAudioVideoController()
                            .isOpen() || (inputView != null && inputView!!.getInlineSearchContext()
                            .isVisibleOrActive()) || (attachedFiles != null && attachedFiles!!.isDisplayingItems()))
                    }

                    return !blocked && super.onInterceptTouchEvent(ev)
                }
            }
            pagerContentView!!.setOffscreenPageLimit(1)
            pagerContentView!!.setOverScrollMode(if (Config.HAS_NICE_OVER_SCROLL_EFFECT) View.OVER_SCROLL_IF_CONTENT_SCROLLS else View.OVER_SCROLL_NEVER)
            pagerContentView!!.addOnPageChangeListener(this)
            pagerContentView!!.setAdapter(pagerContentAdapter)
            pagerContentView!!.setLayoutParams(newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

            val contentView = FrameLayoutFix(context)
            contentView.setLayoutParams(newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            contentView.addView(pagerContentView)

            return contentView
        }

        return contentView
    }

    private var fontSliderView: SliderView? = null
    private val blurSliderView: SliderView? = null

    // SELF-CHAT
    private class MediaTabsAdapter(private val context: MessagesController, private val mediaControllers: MutableList<SharedBaseController<*>>) :
        PagerAdapter() {
        var isLocked: Boolean = false

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            if (`object` is MessagesController) {
                container.removeView(`object`.contentView)
            } else {
                container.removeView((`object` as ViewController<*>).getValue())
            }
        }

        override fun getCount(): Int {
            return if (isLocked) 1 else 1 + mediaControllers.size
        }

        internal val cachedItems = SparseArrayCompat<SharedBaseController<*>?>()

        fun destroy() {
            val size = cachedItems.size()
            for (i in 0..<size) {
                if (!cachedItems.valueAt(i)!!.isDestroyed()) cachedItems.valueAt(i)!!.destroy()
            }
            cachedItems.clear()
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            if (position == 0) {
                container.addView(context.contentView)
                return context
            }
            var c = cachedItems.get(position)
            if (c == null) {
                c = mediaControllers.get(position - 1)
                c.setArguments(SharedBaseController.Args(context.getChatId(), context.getMessageTopicId()))
                c.setParent(context)
                cachedItems.put(position, c)
                c.bindThemeListeners(context)
                val input = context.lastMediaSearchQuery
                if (!isEmpty(input)) {
                    c.getValue()
                    c.search(input)
                }
            }
            container.addView(c.getValue())
            c.setBottomInset(context.extraBottomInset, context.extraBottomInsetWithoutIme)
            return c
        }

        override fun isViewFromObject(view: View, o: Any): Boolean {
            if (o is MessagesController) {
                return o.contentView === view
            } else {
                return o is ViewController<*> && o.getValue() === view
            }
        }

        companion object {
            internal const val POSITION_MESSAGES = 0
            internal const val POSITION_MEDIA = 1
        }
    }

    private var pagerContentAdapter: MediaTabsAdapter? = null
    private var pagerContentView: RtlViewPager? = null
    private var pagerHeaderView: ViewPagerHeaderViewCompact? = null

    var pagerScrollOffset: Float = 0f
        private set
    private var pagerScrollPosition = 0

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        var positionOffset = positionOffset
        positionOffset = ViewPager.clampPositionOffset(positionOffset)
        val offset = position.toFloat() + positionOffset
        pagerHeaderView!!.getTopView().setSelectionFactor(offset)
        if (this.pagerScrollOffset != offset) {
            this.pagerScrollOffset = offset
            if (hideKeyboardOnPageScroll) {
                hideKeyboardOnPageScroll = false
                hideSoftwareKeyboard()
            }
            checkPagerInputBlocked()
            checkRoundVideo()
            checkInlineResults()
            onMessagesFrameChanged()
        }
    }

    override fun onPageSelected(position: Int) {
        if (this.pagerScrollPosition != position) {
            this.pagerScrollPosition = position
            checkPagerInputBlocked()
        }
    }

    private var pagerInputBlocked = false

    private fun checkPagerInputBlocked() {
        val blocked = pagerScrollOffset > 0f || pagerScrollPosition != 0
        if (pagerInputBlocked != blocked) {
            pagerInputBlocked = blocked
            if (blocked) {
                Keyboard.hide(inputView)
            }
            setInputBlockFlag(FLAG_INPUT_OFFSCREEN, blocked)
        }
    }

    private var pagerScrollState = ViewPager.SCROLL_STATE_IDLE
    private var hideKeyboardOnPageScroll = false

    override fun onPageScrollStateChanged(scrollState: Int) {
        if (pagerScrollState != scrollState) {
            val wasScrolling = pagerScrollState != ViewPager.SCROLL_STATE_IDLE
            val nowScrolling = scrollState != ViewPager.SCROLL_STATE_IDLE
            this.pagerScrollState = scrollState
            if (wasScrolling != nowScrolling && nowScrolling) {
                hideKeyboardOnPageScroll = pagerScrollOffset % 1f == 0f
                if (!hideKeyboardOnPageScroll) {
                    hideSoftwareKeyboard()
                }
            }
            if (pagerHeaderView != null && scrollState != ViewPager.SCROLL_STATE_SETTLING) {
                pagerHeaderView!!.getTopView().resetFromTo()
            }
        }
    }

    override fun onPagerItemClick(position: Int) {
        if (pagerContentView!!.getCurrentItem() == 0 && UI.getContext(this@MessagesController.context()).getRecordAudioVideoController().isOpen()) {
            return
        }
        if (pagerContentView!!.getCurrentItem() != position) {
            pagerHeaderView!!.getTopView().setFromTo(pagerContentView!!.getCurrentItem(), position)
            pagerContentView!!.setCurrentItem(position, true)
        }
    }

    private fun showMessagesListIfNeeded() {
        if (pagerScrollPosition != 0 && pagerContentView != null) {
            pagerContentView!!.setCurrentItem(0, true)
        }
    }

    override fun onEnterSelectMode() {
        if (pagerContentView != null) {
            pagerContentView!!.setPagingEnabled(false)
        }
    }

    override fun onLeaveSelectMode() {
        if (pagerContentView != null) {
            pagerContentView!!.setPagingEnabled(true)
        }
        if (exitOnTransformFinish) tdlib!!.ui().post(Runnable { this.navigateBack() })
    }

    // SELF-CHAT END
    private fun addStaticListeners() {
        Settings.instance().addVideoPreferenceChangeListener(this)
        context().getRecordAudioVideoController().addRecordStateListener(this)
    }

    fun removeStaticListeners() {
        Settings.instance().removeVideoPreferenceChangeListener(this)
        context().getRecordAudioVideoController().removeRecordStateListener(this)
    }

    override fun onRecordStateChanged(isRecording: Boolean) {
        setInputBlockFlag(FLAG_INPUT_RECORDING, isRecording)
    }

    val activeChatId: Long
        get() = if (isFocused() && !isPaused() && manager!!.isFocused()) getChatId() else 0L

    override fun getChatId(): Long {
        return if (chat != null) chat!!.id else 0L
    }

    val headerChatId: Long
        get() = if (messageThread != null) messageThread!!.getContextChatId() else getChatId()

    fun getMessageTopicId(): MessageTopic? {
        return if (messageThread != null) messageThread!!.getMessageTopicId() else messageTopicId
    }

    fun getMessageTopicId(replyInfo: ReplyInfo?): MessageTopic? {
        if (replyInfo != null && replyInfo.inTopicId != null) {
            return replyInfo.inTopicId
        }
        return getMessageTopicId()
    }

    private fun matchesTopic(topicId: MessageTopic?): Boolean {
        return topicId.matchesTopic(messageTopicId)
    }

    fun getInputSuggestedPostInfo(replyInfo: ReplyInfo?): InputSuggestedPostInfo? {
        return null
    }

    val draftMessage: DraftMessage?
        get() = if (messageThread != null) messageThread!!.getDraft() else if (chat != null) chat!!.draftMessage else null

    val chatUserId: Long
        get() = TD.getUserId(chat)

    fun compareChat(chatId: Long, threadInfo: ThreadInfo?): Boolean {
        return getChatId() == chatId && ((this.messageThread == null && threadInfo == null) || (this.messageThread != null && this.messageThread == threadInfo))
    }

    fun compareChat(chatId: Long, topicId: MessageTopic?): Boolean {
        return getChatId() == chatId && matchesTopic(topicId)
    }

    fun compareChat(chatId: Long): Boolean {
        return getChatId() == chatId
    }

    fun compareChat(chatId: Long, threadInfo: ThreadInfo?, areScheduledOnly: Boolean): Boolean {
        return getChatId() == chatId && ((this.messageThread == null && threadInfo == null) || (this.messageThread != null && this.messageThread == threadInfo)) && areScheduledOnly == areScheduledOnly()
    }

    val isChannel: Boolean
        get() = tdlib!!.isChannel(getChatId())

    fun comparePrivateUserId(userId: Long): Boolean {
        return userId != 0L && toUserId(getChatId()) == userId
    }

    private val isInputLess: Boolean
        get() = tdlib!!.isChannel(getChatId()) && !tdlib!!.canSendBasicMessage(chat)

    override fun unlock() {
        if (enableOnResume) {
            enableOnResume = false
        }
        if (inputView != null && !inputView!!.isEnabled() && !this.isInputLess) {
            inputView!!.setEnabled(true)
            inputView!!.requestFocus()
        }
    }

    val inlineResultListener: PickListener?
        get() = if (canWriteMessages()) inputView else null

    fun removeInlineBot(userId: Long) {
        if (inputView != null) {
            inputView!!.getInlineSearchContext().removeInlineBot(userId)
        }
    }

    override fun onChatHeaderClick() {
        if (chat != null) {
            if (Test.NEED_CLICK) {
                if (Test.onChatClick(tdlib, chat)) {
                    return
                }
            }
            if (inPreviewSearchMode()) {
                val parent = getParentOrSelf()
                if (parent is ViewPagerController<*>) {
                    val index = if (parent.currentPagerItemPosition == 1) 0 else 1
                    parent.onPagerItemClick(index)
                }
            } else {
                if (messageThread != null && messageThread!!.getChatId() != messageThread!!.getContextChatId()) {
                    val contextChat = tdlib!!.chatSync(messageThread!!.getContextChatId())
                    if (contextChat != null) {
                        openChatProfile(contextChat)
                    } else {
                        openChatProfile(messageThread!!.getContextChatId())
                    }
                } else {
                    openChatProfile(chat)
                }
            }
        }
    }

    private fun openChatProfile(chat: Chat?) {
        val controller = ProfileController(context, tdlib)
        controller.setShareCustomHeaderView(true)
        controller.setArguments(ProfileController.Args(chat,  /* threadInfo */null, false))
        navigateTo(controller)
    }

    private fun openChatProfile(chatId: Long) {
        tdlib!!.ui().openChatProfile(this, chatId,  /* messageThread */null, null)
    }

    override fun needAsynchronousAnimation(): Boolean {
        return manager != null && !manager.isTotallyEmpty() && manager.getAdapter().getMessageCount() == 0
    }

    private fun openLinkedChat(directMessages: Boolean) {
        if (messageThread != null && messageThread!!.getChatId() != 0L && messageThread!!.getChatId() != messageThread!!.getContextChatId()) {
            tdlib!!.ui().openChat(this, messageThread!!.getChatId(), ChatOpenParameters().keepStack().removeDuplicates())
        } else {
            tdlib!!.ui().openLinkedChat(this, toSupergroupId(getChatId()), directMessages, ChatOpenParameters().keepStack().removeDuplicates())
        }
    }

    private fun manageGroup() {
        val controller = ProfileController(context, tdlib)
        controller.setArguments(ProfileController.Args(chat,  /*messageThread*/null, true))
        navigateTo(controller)
    }

    private fun deleteThread() {
        if (messageThread == null) return
        val message = messageThread!!.getOldestMessage()
        tdlib!!.getMessageProperties(message, RunnableData { properties: MessageProperties? ->
            if (properties!!.canGetMessageThread && properties.canBeDeletedForAllUsers) {
                tdlib!!.deleteMessages(messageThread!!.getChatId(), longArrayOf(message!!.id), true)
            }
        })
    }

    private fun joinChat() {
        if (referrer != null) {
            tdlib!!.send<Chat?>(JoinChatByInviteLink(referrer!!.inviteLink), Tdlib.ResultHandler { chat: Chat?, error: TdApi.Error? ->
                if (error != null) {
                    tdlib!!.send<FailedToAddMembers?>(AddChatMember(chat!!.id, tdlib!!.myUserId(), 0), tdlib!!.errorHandler<FailedToAddMembers?>())
                }
            })
        } else {
            tdlib!!.send<FailedToAddMembers?>(AddChatMember(chat!!.id, tdlib!!.myUserId(), 0), tdlib!!.errorHandler<FailedToAddMembers?>())
        }
    }

    private var preventHideKeyboard = false

    fun viewScheduledMessages(force: Boolean) {
        val c = MessagesController(context, tdlib)
        val keyboardVisible = getKeyboardState()
        c.setArguments(
            Arguments(openedFromChatList, chat,  /* messageThread */null, null, null, MessagesManager.HIGHLIGHT_MODE_NONE, null).setScheduled(true)
                .setOpenKeyboard(keyboardVisible)
        )
        if (force) {
            c.forceFastAnimationOnce()
        }
        if (keyboardVisible) {
            preventHideKeyboardOnBlur()
            preventHideKeyboard = true
        }
        navigateTo(c)
    }

    fun viewMessagesFromSender(sender: MessageSender?, animated: Boolean) {
        if (headerView != null) {
            headerView!!.openSearchMode(animated, false)
            onSetSearchMessagesSenderId(sender)
            onSetSearchFilteredShowMode(true)
            searchChatMessages(getLastMessageSearchQuery())
        }
    }

    @Suppress("deprecation")
    override fun onClick(v: View) {
        val viewId = v.getId()
        if (inPreviewMode) {
            if (viewId == R.id.btn_help) {
                context()
                    .tooltipManager()
                    .builder(v)
                    .icon(R.drawable.baseline_info_24)
                    .controller(this)
                    .needBlink(true)
                    .chatTextSize()
                    .show(tdlib, if (tdlib!!.isChannel(chat!!.id)) R.string.EventLogInfoDetailChannel else R.string.EventLogInfoDetail)
            } else if (viewId == R.id.btn_chatAction) {
                if (previewMode == PREVIEW_MODE_EVENT_LOG) {
                    processChatAction()
                }
            } else if (viewId == R.id.btn_scroll) {
                scrollToUnreadOrStartMessage()
            }
        }
        if (viewId == R.id.btn_camera) {
            if (!showPhotoVideoRestriction(v)) {
                openInAppCamera(CameraOpenOptions().anchor(v).noTrace(this.isSecretChat))
            }
        } else if (viewId == R.id.btn_viewScheduled) {
            viewScheduledMessages(false)
        } else if (viewId == R.id.msg_bottomReply) {
            if (reply != null) {
                highlightMessage(reply!!.toMessageId())
            }
        } else if (viewId == R.id.btn_mute) {
            if (chat != null) {
                tdlib!!.ui().toggleMute(this, chat!!.id, false, null)
            }
        } else if (viewId == R.id.btn_openLinkedChat) {
            openLinkedChat(false)
        } else if (viewId == R.id.btn_openDirectMessages) {
            openLinkedChat(true)
        } else if (viewId == R.id.btn_test) {
            bottomBar!!.setAction(R.id.btn_test_crash1, "try again", R.drawable.baseline_remove_circle_24, true)
        } else if (viewId == R.id.btn_test_crash1) {
            bottomBar!!.setAction(R.id.btn_test, "test", R.drawable.baseline_warning_24, true)
        } else if (viewId == R.id.btn_follow) {
            joinChat()
        } else if (viewId == R.id.btn_unpinAll) {
            dismissPinnedMessage()
        } else if (viewId == R.id.btn_applyWallpaper) {
            val newBackgroundType = getArgumentsStrict()!!.wallpaperObject!!.type

            if (newBackgroundType.getConstructor() == BackgroundTypeWallpaper.CONSTRUCTOR && backgroundParamsView != null) {
                (newBackgroundType as BackgroundTypeWallpaper).isBlurred = backgroundParamsView!!.isBlurred()
            }

            tdlib()!!.send<TdApi.Background?>(
                SetDefaultBackground(
                    InputBackgroundRemote(getArgumentsStrict()!!.wallpaperObject!!.id),
                    newBackgroundType,
                    Theme.isDark()
                ), Tdlib.ResultHandler { result: TdApi.Background?, error: TdApi.Error? ->
                    if (result != null) {
                        runOnUiThread(Runnable {
                            val bg = TGBackground(tdlib(), result)
                            tdlib!!.wallpaper().addBackground(bg, Theme.isDark())
                            tdlib!!.settings().setWallpaper(bg, true, Theme.getWallpaperIdentifier())
                            navigateBack()
                        })
                    }
                })
        } else if (viewId == R.id.btn_silent) {
            if (tdlib!!.isChannel(chat!!.id)) {
                val silent = silentButton!!.toggle()
                tdlib!!.send<TdApi.Ok?>(ToggleChatDefaultDisableNotification(chat!!.id, silent), tdlib!!.typedOkHandler())
                val pos = IntArray(2)
                Views.getPosition(bottomWrap, pos)
                UI.showCustomToast(
                    if (silentButton!!.getIsSilent()) R.string.ChannelNotifyMembersInfoOff else R.string.ChannelNotifyMembersInfoOn,
                    Toast.LENGTH_SHORT,
                    if (pos[1] <= Screen.currentHeight() / 2 + Screen.dp(60f)) -(contentView!!.getMeasuredHeight() - bottomWrap!!.getTop() + Screen.dp(14f)) else 0
                )
                if (inputView != null) {
                    updateInputHint()
                }
            }
        } else if (viewId == R.id.btn_chatAction) {
            processChatAction()
        } else if (viewId == R.id.msg_emoji) {
            toggleEmojiKeyboard()
        } else if (viewId == R.id.msg_attach) {
            hideBottomHint()
            openMediaView(false, false)
        } else if (viewId == R.id.msg_send) {
            if (!leaveInlineMode()) {
                if (inputView != null && !this.isSelfChat && !tdlib!!.hasPremium() && inputView!!.hasOnlyPremiumFeatures()) {
                    showBottomHint(Strings.buildMarkdown(this, Lang.getString(R.string.MessageContainsPremiumFeatures), null), false)
                } else if (this.isEditingMessage) {
                    saveMessage(true)
                } else if (areScheduled) {
                    tdlib!!.ui().showScheduleOptions(
                        this,
                        getChatId(),
                        false,
                        SimpleSendCallback { modifiedSendOptions: MessageSendOptions?, disableMarkdown: Boolean -> send(modifiedSendOptions) },
                        null,
                        null
                    )
                } else {
                    send(null)
                }
            }
        } else if (viewId == R.id.btn_scroll) {
            scrollToUnreadOrStartMessage()
        } else if (viewId == R.id.btn_mention) {
            manager!!.scrollToNextMention()
        } else if (viewId == R.id.btn_reaction) {
            manager!!.scrollToNextUnreadReaction()
        } else if (viewId == R.id.msg_command) {
            // FIXME: rely on some state, not on icon id
            if (lastCmdResource == R.drawable.deproko_baseline_bots_command_26) {
                onCommandClick()
            } else if (lastCmdResource == R.drawable.deproko_baseline_bots_keyboard_26 || lastCmdResource == R.drawable.baseline_direction_arrow_down_24 || lastCmdResource == R.drawable.baseline_keyboard_24) {
                toggleCommandsKeyboard()
            }
        } else if (viewId == R.id.btn_search_prev) {
            manager!!.moveToNextResult(false)
        } else if (viewId == R.id.btn_search_next) {
            manager!!.moveToNextResult(true)
        }
    }

    private fun leaveInlineMode(): Boolean {
        if (sendButton!!.inInlineMode()) {
            var currentUsername: CharSequence? = inputView!!.getInlineSearchContext().getInlineUsername()
            if (currentUsername != null) {
                currentUsername = "@" + currentUsername + " "
                if (inputView!!.getText().toString() == currentUsername.toString()) {
                    inputView!!.setInput("", false, true)
                } else {
                    inputView!!.setInput(currentUsername, true, true)
                }
            }
            return true
        }
        return false
    }

    private fun send(sendOptions: MessageSendOptions?, applyMarkdown: Boolean = true) {
        if (this.isEditingMessage) {
            saveMessage(applyMarkdown)
        } else if (hasAttachedFiles()) {
            if (isSendingText) {
                return
            }

            val caption = if (inputView != null) inputView!!.getOutputText(applyMarkdown) else null
            val selectedItems = attachedFiles!!.getCurrentItems()

            val musicEntries = ArrayList<MusicEntry>()
            val files = ArrayList<String?>()

            for (result in selectedItems) {
                when (result.type) {
                    InlineResult.TYPE_AUDIO -> {
                        musicEntries.add(((result as InlineResultCommon).getTag() as MusicEntry?)!!)
                    }

                    InlineResult.TYPE_DOCUMENT -> {
                        files.add(result.id)
                    }
                }
            }

            if (musicEntries.isEmpty() && files.isEmpty()) {
                return
            }

            val sentMessages: MutableList<TdApi.Message?> = ArrayList<TdApi.Message?>(selectedItems.size)
            val replyTo = this.currentReplyId
            val functions = ArrayList<TdApi.Function<*>?>()
            val isTimeout = BooleanArray(1)
            val chatId = getChatId()

            setIsSendingText(true)
            manager!!.setSentMessages(sentMessages)
            val clearInputRunnable = Runnable {
                if (getChatId() == chatId) {
                    clearInputAfterSend(true, true, replyTo, true)
                    UI.showToast(Lang.getString(R.string.SlowFileAccess), Toast.LENGTH_LONG)
                    isTimeout[0] = true
                }
            }

            UI.post(clearInputRunnable, clamp(50 * selectedItems.size, 200, 500).toLong())

            val musicFunctions = getSendMusicFunctions(sendButton, musicEntries, true, !musicEntries.isEmpty(), caption, sendOptions)
            sendFiles(
                sendButton,
                files,
                true,
                true,
                if (!musicEntries.isEmpty() && !files.isEmpty()) null else caption,
                sendOptions,
                RunnableData { filesFunctions: MutableList<TdApi.Function<*>?>? ->
                    if (filesFunctions != null) {
                        functions.addAll(filesFunctions)
                    }
                    if (musicFunctions != null) {
                        functions.addAll(musicFunctions)
                    }
                    executeSendMessageFunctions(
                        functions,
                        sentMessages,
                        sendOptions != null && sendOptions.schedulingState != null,
                        RunnableBool { success: Boolean ->
                            UI.post(
                                Runnable {
                                    if (!isTimeout[0] && getChatId() == chatId) {
                                        clearInputAfterSend(true, true, replyTo, true)
                                    }
                                    UI.cancel(clearInputRunnable)
                                })
                        })
                })
        } else {
            sendText(applyMarkdown, sendOptions)
        }
    }

    override fun onMoreItemPressed(id: Int) {
        if (tdlib!!.ui().processLeaveButton(this, null, getChatId(), id, null)) {
            return
        }
        if (id == R.id.btn_copyLink || id == R.id.btn_share) {
            tdlib!!.client().send(
                GetBackgroundUrl(
                    getArgumentsStrict()!!.wallpaperObject!!.name,
                    TGBackground.makeBlurredBackgroundType(
                        getArgumentsStrict()!!.wallpaperObject!!.type,
                        backgroundParamsView != null && backgroundParamsView!!.isBlurred()
                    )
                ), Client.ResultHandler { result: TdApi.Object? ->
                    if (result!!.getConstructor() == TdApi.HttpUrl.CONSTRUCTOR) {
                        val url = result as TdApi.HttpUrl
                        runOnUiThreadOptional(Runnable {
                            if (id == R.id.btn_copyLink) {
                                UI.copyText(url.url, R.string.CopiedLink)
                            } else {
                                val c = ShareController(context(), tdlib())
                                c.setArguments(ShareController.Args(url.url))
                                c.show()
                                hideCursorsForInputView()
                            }
                        })
                    }
                })
        } else if (id == R.id.btn_openLinkedChat) {
            openLinkedChat(false)
        } else if (id == R.id.btn_openDirectMessages) {
            openLinkedChat(true)
        } else if (id == R.id.btn_manageGroup) {
            manageGroup()
        } else if (id == R.id.btn_deleteThread) {
            deleteThread()
        } else if (id == R.id.btn_viewScheduled) {
            viewScheduledMessages(false)
        } else if (id == R.id.btn_sendScreenshotNotification) {
            UI.showToast("Sent screenshot notification", Toast.LENGTH_SHORT)
            tdlib!!.onScreenshotTaken((System.currentTimeMillis() / 1000L).toInt())
        } else if (id == R.id.btn_debugShowHideBottomBar) {
            if (bottomButtonAction == BOTTOM_ACTION_NONE) {
                showBottomButton(BOTTOM_ACTION_TEST, 0, true)
            } else {
                hideBottomBar(true)
            }
        } else if (id == R.id.btn_chatFontSizeScale) {
            Settings.instance().toggleChatFontSizeScaling()
            manager!!.rebuildLayouts()
        } else if (id == R.id.btn_chatFontSizeReset) {
            Settings.instance().resetChatFontSize()
            Companion.updateFontSliderValue(fontSliderView!!)
            manager!!.rebuildLayouts()
        } else if (id == R.id.btn_botHelp || id == R.id.btn_botSettings) {
            if (chat != null) {
                if (tdlib!!.chatFullyBlocked(chat!!.id)) {
                    tdlib!!.unblockSender(tdlib!!.sender(chat!!.id), tdlib!!.okHandler())
                }
                if (actionMode == ACTION_BOT_START) {
                    hideActionButton()
                }
                sendText(if (id == R.id.btn_botHelp) "/help" else "/settings", false, false, false, newSendOptions())
            }
        } else if (id == R.id.btn_showPinnedMessage) {
            manager!!.restorePinnedMessage()
        } else if (id == R.id.btn_shareMyContact) {
            val user = tdlib!!.myUser()
            if (user == null) {
                return
            }
            showOptions(
                TD.getUserName(user) + ", " + Strings.formatPhone(user.phoneNumber),
                intArrayOf(R.id.btn_shareMyContact, R.id.btn_cancel),
                arrayOf<String?>(
                    Lang.getString(R.string.ShareMyContactInfo), Lang.getString(R.string.Cancel)
                ),
                intArrayOf(OptionColor.BLUE, OptionColor.NORMAL),
                intArrayOf(R.drawable.baseline_contact_phone_24, R.drawable.baseline_cancel_24),
                OptionDelegate { itemView: View?, id1: Int ->
                    if (id1 == R.id.btn_shareMyContact) {
                        sendContact(tdlib!!.myUser()!!, true, newSendOptions())
                    }
                    true
                })
        } else if (id == R.id.btn_reportChat) {
            reportChat(null, null)
        } else if (id == R.id.btn_search) {
            if (manager!!.isReadyToSearch()) {
                openSearchMode()
            }
        } else if (id == R.id.btn_mute) {
            if (chat != null) {
                tdlib!!.ui().toggleMute(this, chat!!.id, false, null)
            }
        }
    }

    private fun reportChat(messages: Array<MessageWithProperties>?, after: Runnable?) {
        val array: Array<TdApi.Message>?
        if (messages != null) {
            array = Array(messages.size) { i -> messages[i].message }
        } else {
            array = null
        }
        TdlibUi.reportChat(this, getChatId(), array, null, after, true)
    }

    override fun saveInstanceState(outState: Bundle, keyPrefix: String?): Boolean {
        val args = getArguments()
        if (args != null) {
            super.saveInstanceState(outState, keyPrefix)
            outState.putInt(keyPrefix + "type", args.constructor)
            outState.putLong(keyPrefix + "chat_id", if (args.chat != null) args.chat.id else 0)
            outState.putString(keyPrefix + "chat_list", if (args.chatList != null) TD.makeChatListKey(args.chatList) else "")
            if (args.messageThread != null) {
                args.messageThread.saveTo(outState, keyPrefix + "thread")
            }
            outState.put(keyPrefix + "topicId", args.messageTopicId)
            outState.put(keyPrefix + "filter_", args.searchFilter)
            if (args.constructor == 1 || args.constructor == 4) {
                outState.putInt(keyPrefix + "mode", args.highlightMode)
                if (args.highlightMessageId != null) {
                    outState.putLong(keyPrefix + "message_id", args.highlightMessageId.messageId)
                    outState.putLong(keyPrefix + "message_chat_id", args.highlightMessageId.chatId)
                }
            }
            if (args.constructor == 2) {
                outState.putInt(keyPrefix + "mode", args.previewMode)
            }
            if (args.constructor == 3 || args.constructor == 4) {
                outState.putString(keyPrefix + "query", args.searchQuery)
                outState.put(keyPrefix + "sender_", args.searchSender)
            }
            outState.putBoolean(keyPrefix + "scheduled", args.areScheduled)
            return true
        }
        return false
    }

    override fun restoreInstanceState(`in`: Bundle, keyPrefix: String?): Boolean {
        val constructor = `in`.getInt(keyPrefix + "type", 0)
        val chatId = `in`.getLong(keyPrefix + "chat_id", 0)
        val chat = tdlib!!.chatSync(chatId)
        if (chatId != 0L && chat == null) return false
        val chatList = TD.chatListFromKey(`in`.getString(keyPrefix + "chat_list", null))
        val messageThread = ThreadInfo.restoreFrom(tdlib, `in`, keyPrefix + "thread")
        val topicId = `in`.restoreMessageTopic(keyPrefix + "topicId")
        if (messageThread === ThreadInfo.INVALID) return false
        val filter = `in`.restoreSearchMessagesFilter(keyPrefix + "filter_")
        var args: Arguments? = null
        when (constructor) {
            0 -> {
                args = Arguments(tdlib, chatList, chat, messageThread, topicId, filter)
            }

            1 -> {
                val highlightMode = `in`.getInt(keyPrefix + "mode", 0)
                val highlightMessageId = `in`.getLong(keyPrefix + "message_id", 0)
                val highlightMessageChatId = `in`.getLong(keyPrefix + "message_chat_id", 0)
                args = Arguments(
                    chatList,
                    chat,
                    messageThread,
                    topicId,
                    if (highlightMessageId != 0L) MessageId(highlightMessageChatId, highlightMessageId) else null,
                    highlightMode,
                    filter
                )
            }

            2 -> {
                val previewMode = `in`.getInt(keyPrefix + "mode", 0)
                args = Arguments(previewMode, chatList, chat)
            }

            3 -> {
                val query = `in`.getString(keyPrefix + "query", null)
                val sender = `in`.restoreMessageSender(keyPrefix + "sender_")
                args = Arguments(chatList, chat, query, sender, filter)
            }

            4 -> {
                val query = `in`.getString(keyPrefix + "query", null)
                val sender = `in`.restoreMessageSender(keyPrefix + "sender_")
                val highlightMode = `in`.getInt(keyPrefix + "mode", 0)
                val highlightMessageId = `in`.getLong(keyPrefix + "message_id", 0)
                val highlightMessageChatId = `in`.getLong(keyPrefix + "message_chat_id", 0)
                args = Arguments(
                    chatList,
                    chat,
                    query,
                    sender,
                    filter,
                    if (highlightMessageId != 0L) MessageId(highlightMessageChatId, highlightMessageId) else null,
                    highlightMode
                )
            }
        }
        if (args != null) {
            super.restoreInstanceState(`in`, keyPrefix)
            args.areScheduled = `in`.getBoolean(keyPrefix + "scheduled", false)
            setArguments(args)
            return true
        }
        return false
    }

    class Arguments {
        internal val constructor: Int

        val chat: Chat?
        val chatList: ChatList?
        val highlightMessageId: MessageId?
        val highlightMode: Int

        val inPreviewMode: Boolean
        val previewMode: Int

        var foundMessageId: MessageId? = null
        var searchQuery: String? = null
        var searchSender: MessageSender? = null
        var searchFilter: SearchMessagesFilter? = null

        var areScheduled: Boolean = false
        var openKeyboard: Boolean = false

        val messageThread: ThreadInfo?
        val messageTopicId: MessageTopic?

        var referrer: Referrer? = null
        var videoChatOrLiveStreamInvitation: InternalLinkTypeVideoChat? = null
        var fillDraft: TdApi.FormattedText? = null

        var eventLogUserId: Long = 0

        @JvmField var wallpaperObject: TdApi.Background? = null

        constructor(tdlib: Tdlib, chatList: ChatList?, chat: Chat?, messageThread: ThreadInfo?, messageTopicId: MessageTopic?, filter: SearchMessagesFilter?) {
            this.constructor = 0
            this.chatList = chatList
            this.chat = chat
            this.messageThread = messageThread
            this.messageTopicId = messageTopicId
            this.highlightMode = MessagesManager.getAnchorHighlightMode(tdlib!!.id(), chat, messageThread)
            this.highlightMessageId = MessagesManager.getAnchorMessageId(tdlib!!.id(), chat, messageThread, highlightMode)
            this.searchFilter = filter

            this.inPreviewMode = false
            this.previewMode = 0
        }

        constructor(
            chatList: ChatList?,
            chat: Chat?,
            messageThread: ThreadInfo?,
            messageTopicId: MessageTopic?,
            highlightMessageId: MessageId?,
            highlightMode: Int,
            filter: SearchMessagesFilter?
        ) {
            this.constructor = 1
            this.chatList = chatList
            this.chat = chat
            this.messageThread = messageThread
            this.messageTopicId = messageTopicId
            this.highlightMessageId = highlightMessageId
            this.highlightMode = highlightMode
            this.searchFilter = filter

            this.inPreviewMode = false
            this.previewMode = 0
        }

        constructor(previewMode: Int, chatList: ChatList?, chat: Chat?) {
            this.constructor = 2
            this.previewMode = previewMode
            this.chat = chat
            this.chatList = chatList
            this.messageThread = null
            this.messageTopicId = null

            this.inPreviewMode = true
            this.highlightMode = MessagesManager.HIGHLIGHT_MODE_NONE
            this.highlightMessageId = null
        }

        constructor(chatList: ChatList?, chat: Chat?, query: String?, sender: MessageSender?, filter: SearchMessagesFilter?) {
            this.constructor = 3
            this.chat = chat
            this.chatList = chatList
            this.messageThread = null
            this.messageTopicId = null
            this.searchQuery = query
            this.searchSender = sender
            this.searchFilter = filter

            this.inPreviewMode = true
            this.previewMode = PREVIEW_MODE_SEARCH
            this.highlightMode = 0
            this.highlightMessageId = null
        }

        constructor(
            chatList: ChatList?,
            chat: Chat?,
            query: String?,
            sender: MessageSender?,
            filter: SearchMessagesFilter?,
            highlightMessageId: MessageId?,
            highlightMode: Int
        ) {
            this.constructor = 4
            this.chat = chat
            this.chatList = chatList
            this.messageThread = null
            this.messageTopicId = null
            this.searchQuery = query
            this.searchSender = sender
            this.searchFilter = filter

            this.inPreviewMode = true
            this.previewMode = PREVIEW_MODE_SEARCH
            this.highlightMessageId = highlightMessageId
            this.highlightMode = highlightMode
        }

        constructor(
            chatList: ChatList?,
            chat: Chat?,
            messageThread: ThreadInfo?,
            messageTopicId: MessageTopic?,
            highlightMessageId: MessageId?,
            highlightMode: Int,
            filter: SearchMessagesFilter?,
            foundMessageId: MessageId?,
            globalSearchQuery: String?
        ) {
            this.constructor = 5
            this.chatList = chatList
            this.chat = chat
            this.messageThread = messageThread
            this.messageTopicId = messageTopicId
            this.highlightMessageId = highlightMessageId
            this.highlightMode = highlightMode
            this.searchFilter = filter

            this.foundMessageId = foundMessageId
            this.searchQuery = globalSearchQuery

            this.inPreviewMode = false
            this.previewMode = 0
        }

        constructor(chatList: ChatList?, chat: Chat?, sender: MessageSender?) {
            this.constructor = 6
            this.chatList = chatList
            this.chat = chat
            this.highlightMessageId = null
            this.highlightMode = 0
            this.searchSender = sender
            this.inPreviewMode = false
            this.previewMode = 0
            this.messageThread = null
            this.messageTopicId = null
        }

        fun referrer(referrer: Referrer?): Arguments {
            this.referrer = referrer
            return this
        }

        fun setScheduled(areScheduled: Boolean): Arguments {
            require(!(areScheduled && messageThread != null))
            this.areScheduled = areScheduled
            return this
        }

        fun fillDraft(fillDraft: TdApi.FormattedText?): Arguments {
            this.fillDraft = if (!fillDraft.isEmpty()) fillDraft else null
            return this
        }

        fun setOpenKeyboard(openKeyboard: Boolean): Arguments {
            this.openKeyboard = openKeyboard
            return this
        }

        fun setWallpaperObject(wallpaperObject: TdApi.Background?): Arguments {
            this.wallpaperObject = wallpaperObject
            return this
        }

        fun voiceChatInvitation(voiceChatInvitation: InternalLinkTypeVideoChat?): Arguments {
            this.videoChatOrLiveStreamInvitation = voiceChatInvitation
            return this
        }

        fun eventLogUserId(eventLogUserId: Long): Arguments {
            this.eventLogUserId = eventLogUserId
            return this
        }
    }

    class Referrer(val inviteLink: String?)

    fun areScheduledOnly(): Boolean {
        return areScheduled
    }

    private var inPreviewMode = false
    private var previewMode = 0
    private var foundMessageId: MessageId? = null
    private var searchFromUserMessageId: MessageId? = null
    private var previewSearchQuery: String? = null
    private var previewSearchSender: MessageSender? = null
    private var previewSearchFilter: SearchMessagesFilter? = null
    var messageThread: ThreadInfo? = null
        private set
    private var messageTopicId: MessageTopic? = null
    private var areScheduled = false
    private var referrer: Referrer? = null
    private var voiceChatInvitation: InternalLinkTypeVideoChat? = null
    private var fillDraft: TdApi.FormattedText? = null
    private var openKeyboard = false

    fun inWallpaperMode(): Boolean {
        return inPreviewMode && (previewMode == PREVIEW_MODE_WALLPAPER || previewMode == PREVIEW_MODE_WALLPAPER_OBJECT)
    }

    fun inWallpaperPreviewMode(): Boolean {
        return inPreviewMode && previewMode == PREVIEW_MODE_WALLPAPER_OBJECT
    }

    fun inPreviewMode(): Boolean {
        return inPreviewMode
    }

    fun inTextSizeMode(): Boolean {
        return inPreviewMode && previewMode == PREVIEW_MODE_FONT_SIZE
    }

    private var linkedChatId: Long = 0

    override fun setArguments(args: Arguments?) {
        super.setArguments(args)
        val args = args!!

        this.chat = args.chat
        this.customBotPlaceholder = null
        this.customCaptionPlaceholder = null
        this.messageThread = args.messageThread
        this.messageTopicId = args.messageTopicId
        this.openedFromChatList = args.chatList
        this.linkedChatId = 0
        this.areScheduled = args.areScheduled
        this.referrer = args.referrer
        this.voiceChatInvitation = args.videoChatOrLiveStreamInvitation
        this.previewSearchQuery = args.searchQuery
        this.previewSearchSender = args.searchSender
        this.previewSearchFilter = args.searchFilter
        this.manager!!.setHighlightMessageId(args.highlightMessageId, args.highlightMode)
        this.inPreviewMode = args.inPreviewMode
        this.previewMode = args.previewMode
        this.openKeyboard = args.openKeyboard
        this.foundMessageId = args.foundMessageId
        this.fillDraft = args.fillDraft

        if (contentView != null) {
            updateView()
        }
    }

    private val isEventLog: Boolean
        get() = inPreviewMode && previewMode == PREVIEW_MODE_EVENT_LOG

    private var ignoreDraftLoad = false
    private var itemToShare: Any? = null

    fun setShareItem(shareItem: Any?) {
        this.itemToShare = shareItem
        this.ignoreDraftLoad = shareItem != null && shouldIgnoreDraftLoad(shareItem)
    }

    private fun shareItem() {
        if (itemToShare != null) {
            shareItem(itemToShare)
            itemToShare = null
        }
    }

    fun shareItem(item: Any?) {
        if (item is InlineResultButton) {
            if (!hasSendBasicMessagePermission()) {
                return
            }
            processSwitchPm(item)
            return
        }

        if (item is TGSwitchInline) {
            if (!hasSendBasicMessagePermission()) {
                return
            }
            if (inputView != null) {
                inputView!!.setInput(item.toString(), true, false)
            }
            return
        }

        if (item is String) {
            sendText(item, false, false, false, newSendOptions())
            return
        }

        if (item is TdApi.User) {
            sendContact(item, true, newSendOptions())
            return
        }

        if (item is TGBotStart) {
            val start = item

            if (start.isGame()) {
                val replyInfo = this.currentReplyId
                val topicId = getMessageTopicId(replyInfo)
                val sendOptions = newSendOptions(
                    getInputSuggestedPostInfo(replyInfo),
                    obtainSilentMode()
                )
                tdlib!!.sendMessage(chat!!.id, topicId, null, sendOptions, InputMessageGame(start.getUserId(), start.getArgument()))
            } else if (start.useDeepLinking()) {
                if (!isUserChat(chat!!.id) || start.ignoreExplicitUserInteraction()) {
                    tdlib!!.sendBotStartMessage(start.getUserId(), chat!!.id, start.getArgument())
                } else {
                    showActionBotButton(start.getArgument())
                }
                return
            } else if (isPrivate(chat!!.id)) {
                return
            }

            tdlib!!.client().send(AddChatMember(chat!!.id, item.getUserId(), 0), Client.ResultHandler { `object`: TdApi.Object? ->
                if (`object`!!.getConstructor() != TdApi.Ok.CONSTRUCTOR) {
                    UI.showToast("Bot is already in chat", Toast.LENGTH_SHORT)
                }
            })

            return
        }

        if (item is TdApi.Message) {
            forwardMessage(item)
            return
        }

        if (item is TdApi.Audio) {
            sendAudio(item, false)
            return
        }
    }

    fun reloadData() {
        manager!!.loadFromStart()
    }

    private var adminsHandler: CancellableResultHandler? = null

    private fun updateView() {
        if (selectedMessageIds != null) {
            clearSelectedMessageIds()
        }

        if (sendButton != null) {
            sendButton!!.getSlowModeCounterController(tdlib).setCurrentChat(getChatId())
            sendButton!!.getSlowModeCounterController(tdlib)
                .setSlowModeCounterUpdateListener(RunnableInt { duration: Int -> this.onSlowModeCounterUpdate(duration) })
        }
        if (messageSenderButton != null) {
            messageSenderButton!!.setInSlowMode(tdlib!!.inSlowMode(getChatId()))
        }
        clearSwitchPmButton()
        clearReply()
        canSendMessageToUser = null

        resetEditState()
        forceHideToast()
        topBar!!.hideAll(false)

        resetSearchControls()
        updateSelectMessageSenderInterface(false)
        if (searchByUserViewWrapper != null) {
            searchByUserViewWrapper!!.dismiss()
        }

        tdlib!!.ui().updateTTLButton(R.id.menu_secretChat, headerView, chat, true)
        messagesView!!.setMessageAnimatorEnabled(false)

        // messagesView.showDateForcely();
        if (botHelper != null) {
            botHelper!!.destroy()
            botHelper = null
        }
        if (liveLocation != null) {
            liveLocation!!.destroy()
            liveLocation = null
        }

        if (previewMode == PREVIEW_MODE_NONE) {
            updateForcedSubtitle() // must be called before calling headerCell.setChat
            headerCell!!.setCallback(if (areScheduled) null else this)
        }
        val headerChat = if (messageThread != null) tdlib!!.chatSync(messageThread!!.getContextChatId()) else null
        headerCell!!.setChat(tdlib, if (headerChat != null) headerChat else chat, messageThread)
        if (previewMode == PREVIEW_MODE_NONE) {
            updateForumTopicHeader()
        }

        if (inPreviewMode) {
            when (previewMode) {
                PREVIEW_MODE_EVENT_LOG -> {
                    showActionButton(R.string.Settings, ACTION_EVENT_LOG_SETTINGS)
                    manager!!.openEventLog(chat)
                    messagesView!!.setItemAnimator(CustomItemAnimator(DECELERATE_INTERPOLATOR, 120L))
                    if (getArgumentsStrict()!!.eventLogUserId != 0L && headerCell != null) {
                        manager.applyEventLogFilters(
                            ChatEventLogFilters(
                                true,
                                true,
                                true,
                                true,
                                true,
                                true,
                                true,
                                true,
                                true,
                                true,
                                true,
                                true,
                                true,
                                true,
                                true
                            ), longArrayOf(getArgumentsStrict()!!.eventLogUserId)
                        )
                    }
                }

                PREVIEW_MODE_SEARCH -> {
                    manager!!.openSearch(chat, previewSearchQuery, previewSearchSender, previewSearchFilter)
                    updateBottomBar(false)
                }

                else -> manager!!.loadPreview()
            }
            return
        }

        setLockFocusView(if (chat != null && tdlib!!.canSendBasicMessage(chat)) inputView else null, false)

        if (chat == null) {
            return
        }
        if (chat!!.messageSenderId != null) {
            getChatAvailableMessagesSenders(null)
        }
        if (inputView != null) {
            // inputView.setIgnoreAnyChanges(true);
            val enabled = !this.isInputLess
            if (inputView!!.isEnabled() != enabled) {
                inputView!!.setEnabled(enabled)
            }
            if (enabled) {
                inputView!!.setNoPersonalizedLearning(Settings.instance().needsIncognitoMode(chat))
            }
        }

        var draftMessage: DraftMessage? = null
        if (tdlib!!.canSendBasicMessage(chat)) {
            draftMessage = this.draftMessage
            if (!fillDraft.isEmpty()) {
                if (draftMessage.isEmpty() || (draftMessage.inputMessageText as InputMessageText).text.isEmpty() /*allow dropping replyTo*/) {
                    draftMessage = DraftMessage(
                        null,
                        0,
                        InputMessageText(fillDraft, null, false),
                        0,
                        null
                    )
                } else if (!(draftMessage.inputMessageText as InputMessageText).text.equalsTo(fillDraft)) {
                    promptDraftPrefillOnFocus = true
                }
            }
            val replyTo = if (draftMessage != null) draftMessage.replyTo else null
            if (replyTo != null && replyTo.getConstructor() != TdApi.InputMessageReplyToStory.CONSTRUCTOR) {
                if (!ignoreDraftLoad) {
                    forceDraftReply(replyTo)
                }
            }
            updateSilentButton(tdlib!!.isChannel(chat!!.id))
        }
        if (!inPreviewMode && !isInForceTouchMode()) {
            checkActionBar()
            checkJoinRequests(chat!!.pendingJoinRequests)
            checkCanSendMessagesToUser(true)
        }
        if (inputView != null) {
            inputView!!.setChat(
                chat,
                messageThread,
                if (draftMessage != null) draftMessage.inputMessageText else null,
                this.customInputPlaceholder,
                silentButton != null && silentButton!!.getIsSilent()
            )
        }
        ignoreDraftLoad = false
        discardAttachedFiles(false)
        updateBottomBar(false)

        closeCommandsKeyboard(false)

        if (previewSearchSender == null) {
            manager!!.openChat(chat, messageThread, messageTopicId, previewSearchFilter, this, areScheduled, !inPreviewMode && !isInForceTouchMode())
        }

        updateShadowColor()
        if (scheduleButton != null && scheduleButton!!.setVisible(messageThread == null && tdlib!!.chatHasScheduled(chat!!.id))) {
            commandButton!!.setTranslationX((if (scheduleButton!!.isVisible()) 0 else scheduleButton!!.getLayoutParams().width).toFloat())
            attachButtons!!.updatePivot()
        }

        // Preloading data so profile will not jump when opening
        when (chat!!.type.getConstructor()) {
            TdApi.ChatTypePrivate.CONSTRUCTOR, TdApi.ChatTypeSecret.CONSTRUCTOR -> {
                tdlib!!.cache().userFull(TD.getUserId(chat))
            }

            ChatTypeBasicGroup.CONSTRUCTOR -> {
                tdlib!!.cache().basicGroupFull(toBasicGroupId(chat!!.id))
            }

            ChatTypeSupergroup.CONSTRUCTOR -> {
                tdlib!!.cache().supergroupFull(toSupergroupId(chat!!.id))
            }
        }

        // Loading bot information after the messages to display them faster
        val isMultiChat = tdlib!!.isMultiChat(chat!!.id)

        if (isMultiChat || tdlib!!.isBotChat(chat!!) /* || (TGUtils.isChannel(chat) && TGUtils.hasWritePermission(chat))*/) {
            updateCommandButton(R.drawable.deproko_baseline_bots_command_26)
            updateCommandButton(false)
            botHelper = BotHelper(this, chat!!)
        } else {
            updateCommandButton(0)
            botHelper = null
        }

        liveLocation = LiveLocationHelper(context, tdlib, chat!!.id, getMessageTopicId(), liveLocationView, false, this)
        liveLocation!!.init()

        if (inputView != null) {
            inputView!!.setCommandListProvider(botHelper)
            // inputView.setIgnoreAnyChanges(false);
        }

        val chatId = getChatId()
        tdlib!!.client().send(GetChatAdministrators(chatId), object : CancellableResultHandler() {
            override fun processResult(`object`: TdApi.Object) {
                if (`object`.getConstructor() == ChatAdministrators.CONSTRUCTOR) {
                    tdlib!!.ui().post(Runnable {
                        if (!isCancelled()) {
                            if (getChatId() == chatId) {
                                manager!!.setChatAdmins(`object` as ChatAdministrators)
                            }
                        }
                    })
                }
            }
        }.also { adminsHandler = it })

        if (emojiLayout != null) {
            emojiLayout!!.setAllowPremiumFeatures(this.isSelfChat)
        }

        updateCounters(false)
        checkRestriction()
        checkLinkedChat()
    }

    fun updateShadowColor() {
        if (bottomShadowView != null) {
            bottomShadowView!!.setColorId(if (manager!!.useBubbles()) ColorId.bubble_chatSeparator else ColorId.chatSeparator)
        }
    }

    private fun cancelAdminsRequest() {
        if (adminsHandler != null) {
            adminsHandler!!.cancel()
            adminsHandler = null
        }
    }

    private fun updateCounters(animated: Boolean) {
        if (chat != null) {
            if (messageThread != null) {
                val unreadCount: Int
                if (messageThread!!.hasUnreadMessages(chat) && !areScheduledOnly()) {
                    unreadCount =
                        if (messageThread!!.getUnreadMessageCount() != ThreadInfo.UNKNOWN_UNREAD_MESSAGE_COUNT) messageThread!!.getUnreadMessageCount() else Tdlib.CHAT_MARKED_AS_UNREAD
                } else {
                    unreadCount = 0
                }
                setUnreadCountBadge(unreadCount, animated)
                setMentionCountBadge(0)
                setReactionCountBadge(0)
            } else {
                setUnreadCountBadge(chat!!.unreadCount, true)
                setMentionCountBadge(chat!!.unreadMentionCount)
                setReactionCountBadge(chat!!.unreadReactionCount)
            }
            if (bottomButtonAction == BOTTOM_ACTION_TOGGLE_MUTE) {
                showBottomButton(bottomButtonAction, 0, animated)
            }
        }
    }

    private fun scrollToUnreadOrStartMessage() {
        val anchorMode = MessagesManager.getAnchorHighlightMode(tdlib!!.id(), chat, messageThread)
        if (!manager!!.hasReturnMessage()) {
            if (!inPreviewMode && !isInForceTouchMode() && anchorMode == MessagesManager.HIGHLIGHT_MODE_UNREAD) {
                val messageId = MessagesManager.getAnchorMessageId(tdlib!!.id(), chat, messageThread, anchorMode)
                manager.highlightMessage(messageId, MessagesManager.HIGHLIGHT_MODE_UNREAD_NEXT, null, true)
                return
            }
            if (chat != null && MessagesManager.canGoUnread(chat, messageThread)) {
                val messageId = MessagesManager.getAnchorMessageId(tdlib!!.id(), chat, messageThread, MessagesManager.HIGHLIGHT_MODE_UNREAD)
                val firstUnreadIndex = manager.indexOfFirstUnreadMessage()
                val bottom = manager.findBottomMessage()
                val bottomMessageId = if (bottom != null) bottom.toMessageId() else null
                val messageIndex = manager.getAdapter().indexOfMessageContainer(messageId)
                if (bottomMessageId == null || bottomMessageId.chatId != messageId!!.chatId) {
                    if (messageIndex != -1) {
                        manager.highlightMessage(messageId, MessagesManager.HIGHLIGHT_MODE_UNREAD_NEXT, null, true)
                    } else {
                        manager.resetByMessage(messageId, MessagesManager.HIGHLIGHT_MODE_UNREAD)
                    }
                    return
                } else if (bottomMessageId.messageId < messageId.messageId) {
                    val bottomMessageIndex = manager.getAdapter().indexOfMessageContainer(bottomMessageId)
                    if (firstUnreadIndex == -1 || bottomMessageIndex > firstUnreadIndex) {
                        if (messageIndex != -1 && (bottomMessageIndex - messageIndex > 1 || (firstUnreadIndex != -1 && bottomMessageIndex > firstUnreadIndex))) {
                            manager.highlightMessage(messageId, MessagesManager.HIGHLIGHT_MODE_UNREAD_NEXT, null, true)
                            return
                        }
                        if (messageIndex == -1) {
                            manager.resetByMessage(messageId, MessagesManager.HIGHLIGHT_MODE_UNREAD)
                            return
                        }
                    }
                }
            }
        }
        manager.scrollToStart(false)
    }

    fun centerMessage(chatId: Long, messageId: Long, delayed: Boolean, centered: Boolean): Boolean {
        if (getChatId() == chatId && chatId != 0L) {
            return manager!!.centerMessage(chatId, messageId, delayed, centered)
        }
        return false
    }

    fun calculateScrollDyForCenterVideoMessage(chatId: Long, messageId: Long, out: VideoScrollParameters?): Boolean {
        if (getChatId() == chatId && chatId != 0L) {
            return manager!!.calculateScrollDyForCenterVideoMessage(chatId, messageId, out)
        }
        return false
    }

    override fun onFocusStateChanged() {
        checkRoundVideo()
        checkInlineResults()
        checkBroadcastingSomeAction()
        if (pinnedMessagesBar != null) {
            pinnedMessagesBar!!.setAnimationsDisabled(!isFocused())
        }
    }

    private fun checkInlineResults() {
        context().setInlineResultsHidden(this, !isFocused() || pagerScrollOffset >= 1f)
        context().setEmojiSuggestionsVisible(isFocused() && !(pagerScrollOffset >= 1f) && canShowEmojiSuggestions)
    }

    @JvmOverloads
    fun checkRoundVideo(first: Int = -1, last: Int = -1, onScroll: Boolean = false) {
        var first = first
        var last = last
        if (isInForceTouchMode()) {
            /*NavigationController navigation = UI.getContext(getContext()).getNavigation();
      if (navigation != null) {
        ViewController c = navigation.getCurrentStackItem();
        if (c instanceof MessagesController && ((MessagesController) c).getChatId() == getChatId()) {
          return;
        }
        c = navigation.getPreviousStackItem();
        if (c instanceof MessagesController && ((MessagesController) c).getChatId() == getChatId()) {
          return;
        }
      }*/
            return
        }

        if (messagesView!!.isComputingLayout()) {
            UI.post(Runnable {
                if (!isDestroyed()) {
                    checkRoundVideo(-1, -1, onScroll)
                }
            })
            return
        }

        val roundVideoController = context().getRoundVideoController()
        var targetView: MessageViewGroup? = null
        var targetChat = false
        targetChat = getChatId() == roundVideoController.getPlayingChatId()
        if (targetChat) {
            val playingMessageId = roundVideoController.getPlayingMessageId()
            val manager = this.manager!!.getLayoutManager()
            if (first == -1) {
                first = manager.findFirstVisibleItemPosition()
            }
            if (last == -1) {
                last = manager.findLastVisibleItemPosition()
            }
            if (first != -1 && last != -1 && !messagesView!!.isComputingLayout()) {
                val adapter = this.manager.getAdapter()
                val messageCount = adapter.getMessageCount()
                if (messageCount > 0) {
                    for (i in first..last) {
                        if (i >= 0 && i < messageCount && MessagesHolder.isMessageType(adapter.getItemViewType(i)) && adapter.getItem(i)
                                .id == playingMessageId
                        ) {
                            val view = manager.findViewByPosition(i)
                            if (view is MessageViewGroup) {
                                val msg = view.getMessageView().getMessage()
                                if (msg != null && msg.id == playingMessageId) {
                                    targetView = view
                                    break
                                }
                            }
                        }
                    }
                }
            }
        }
        roundVideoController.setAttachedToView(targetView, if (isInForceTouchMode()) this else null)
        if (targetChat && onScroll) {
            roundVideoController.onMessagesScroll()
        }
    }

    override fun onChatDefaultMessageSenderIdChanged(chatId: Long, senderId: MessageSender?) {
        runOnUiThreadOptional(Runnable {
            if (getChatId() == chatId) {
                updateInputHint()
                updateSelectMessageSenderInterface(true)
            }
        })
    }

    fun updateInputHint() {
        if (inputView != null) {
            inputView!!.updateMessageHint(
                chat,
                messageThread,
                this.customInputPlaceholder,
                if (Config.NEED_SILENT_BROADCAST && silentButton != null) silentButton!!.getIsSilent() else tdlib!!.chatDefaultDisableNotifications(getChatId())
            )
        }
    }

    private val isReplyRequired: Boolean
        get() {
            if (reply != null) {
                return false
            }
            val supergroup = tdlib!!.chatToSupergroup(getChatId())
            if (supergroup != null && supergroup.isAdministeredDirectMessagesGroup) {
                return true
            }

            return false
        }

    private fun updateBottomBar(isUpdate: Boolean) {
        setInputBlockFlag(FLAG_INPUT_TEXT_DISABLED, !tdlib!!.canSendBasicMessage(chat))
        if (sendButton != null) {
            sendButton!!.getSlowModeCounterController(tdlib).updateSlowModeTimer(isUpdate)
        }
        if (messageSenderButton != null) {
            messageSenderButton!!.setInSlowMode(tdlib!!.inSlowMode(getChatId()))
        }
        if (isUpdate) {
            updateInputHint()
        }
        if (isInForceTouchMode()) {
            setInputVisible(false, false)
            return
        }
        if (inPreviewSearchMode()) {
            setInputVisible(false, false)
            if (arePinnedMessages()) {
                showBottomButton(BOTTOM_ACTION_UNPIN_ALL, 0, isUpdate)
            }
            return
        }
        val status = tdlib!!.chatStatus(chat!!.id)
        if (tdlib!!.isChannel(chat!!.id) && status != null && !TD.isAdmin(status)) {
            setInputVisible(false, false)
            if (TD.isLeft(status)) {
                showBottomButton(BOTTOM_ACTION_FOLLOW, 0, isUpdate)
            } else {
                val info = tdlib!!.cache().supergroupFull(toSupergroupId(chat!!.id))
                if (info != null && info.linkedChatId != 0L) {
                    showBottomButton(BOTTOM_ACTION_DISCUSS, info.linkedChatId, isUpdate)
                } else {
                    showBottomButton(BOTTOM_ACTION_TOGGLE_MUTE, 0, isUpdate)
                }
            }
        } else if (tdlib!!.isRepliesChat(chat!!.id)) {
            setInputVisible(false, false)
            showBottomButton(BOTTOM_ACTION_TOGGLE_MUTE, 0, isUpdate)
        } else {
            hideBottomBar(isUpdate)

            val secretChat = tdlib!!.chatToSecretChat(chat!!.id)
            val supergroup = tdlib!!.chatToSupergroup(chat!!.id)
            val joinSupergroupToSendMessages = messageThread == null || supergroup != null && supergroup.joinToSendMessages
            if (canSendMessageToUser != null && canSendMessageToUser!!.getConstructor() != TdApi.CanSendMessageToUserResultOk.CONSTRUCTOR) {
                when (canSendMessageToUser!!.getConstructor()) {
                    TdApi.CanSendMessageToUserResultOk.CONSTRUCTOR -> throw IllegalStateException() // unreachable

                    TdApi.CanSendMessageToUserResultUserIsDeleted.CONSTRUCTOR -> showActionDeleteChatButton()
                    TdApi.CanSendMessageToUserResultUserRestrictsNewChats.CONSTRUCTOR -> {
                        showActionButton(
                            Lang.getMarkdownString(this, R.string.UserNewChatRetricted, Lang.boldCreator(), tdlib!!.chatTitleShort(chat!!.id)),
                            ACTION_EMPTY,
                            false
                        )
                    }

                    CanSendMessageToUserResultUserHasPaidMessages.CONSTRUCTOR -> {
                        val paidMessages = canSendMessageToUser as CanSendMessageToUserResultUserHasPaidMessages
                        showActionButton(
                            Lang.getMarkdownPlural(
                                this,
                                R.string.UserMessagesPaid,
                                paidMessages.outgoingPaidMessageStarCount,
                                Lang.boldCreator(),
                                tdlib!!.chatTitleShort(chat!!.id)
                            ), ACTION_EMPTY, false
                        )
                    }

                    else -> {
                        assertCanSendMessageToUserResult_15fb1d0f()
                        throw unsupported(canSendMessageToUser!!)
                    }
                }
            } else if (secretChat != null && !TD.isSecretChatReady(secretChat)) {
                showSecretChatAction(secretChat)
            } else if (tdlib!!.chatFullyBlocked(chat!!.id) && tdlib!!.isUserChat(chat)) {
                showActionUnblockButton()
            } else if (tdlib!!.chatUserDeleted(chat!!) || (isBasicGroup(chat!!.id) && (!tdlib!!.chatBasicGroupActive(chat!!.id) || TD.isNotInChat(status))) || (tdlib!!.isSupergroupChat(
                    chat
                ) && TD.isNotInChat(status) && joinSupergroupToSendMessages)
            ) {
                if (tdlib!!.isSupergroupChat(chat) && status != null && TD.canReturnToChat(status)) {
                    showActionJoinChatButton()
                } else if (messageThread != null) {
                    val restrictionStatus = tdlib!!.getBasicMessageRestrictionText(chat!!)
                    if (restrictionStatus != null && !hasSendSomeMediaPermission()) {
                        showActionButton(restrictionStatus, ACTION_EMPTY, false)
                    } else {
                        hideActionButton()
                    }
                } else {
                    showActionDeleteChatButton()
                }
            } else if (tdlib!!.isBotChat(chat!!) && chat!!.lastMessage == null) {
                showActionBotButton()
            } else {
                val restrictionStatus = tdlib!!.getBasicMessageRestrictionText(chat!!)
                if (restrictionStatus != null && !hasSendSomeMediaPermission()) {
                    showActionButton(restrictionStatus, ACTION_EMPTY, false)
                } else if (this.isReplyRequired) {
                    showActionButton(Lang.getMarkdownString(this, R.string.ReplyRequiredHint), ACTION_AWAITING_REPLY, false)
                } else {
                    hideActionButton()
                }
            }
        }
    }

    // video/audio btn
    private var tooltipInfo: TooltipInfo? = null

    private fun showBottomHint(@StringRes res: Int) {
        showBottomHint(Lang.getString(res), false)
    }

    fun hideBottomHint() {
        if (tooltipInfo != null) {
            tooltipInfo!!.hide(true)
        }
    }

    private fun showBottomHint(text: CharSequence, isError: Boolean) {
        if (!isAttachedToNavigationController()) return
        if (tooltipInfo == null) {
            tooltipInfo = context().tooltipManager().builder(sendButton)
                .locate(TooltipOverlayView.LocationProvider { targetView: View?, rect: Rect? -> sendButton!!.getTargetBounds(targetView, rect) })
                .icon(if (isError) R.drawable.baseline_warning_24 else 0)
                .ignoreViewScale(true)
                .controller(this)
                .show(tdlib, text)
            tooltipInfo!!.addOnCloseListener(RunnableLong { duration: Long -> this.onTooltipInfoClose(duration) })
        } else {
            tooltipInfo!!.reset(context().tooltipManager().newContent(tdlib, text, 0), if (isError) R.drawable.baseline_warning_24 else 0)
            tooltipInfo!!.show()
        }
        isSlowModeRestrictionHintVisible = false
        tooltipInfo!!.hideDelayed(false)
    }

    private var isSlowModeRestrictionHintVisible = false

    private fun onTooltipInfoClose(duration: Long) {
        isSlowModeRestrictionHintVisible = false
    }

    private fun onSlowModeCounterUpdate(duration: Int) {
        if (sendButton != null && tooltipInfo != null && tooltipInfo!!.isVisible() && isSlowModeRestrictionHintVisible) {
            val restriction = tdlib()!!.getSlowModeRestrictionText(getChatId(), null)
            if (restriction != null) {
                tooltipInfo!!.reset(context().tooltipManager().newContent(tdlib, restriction, 0), R.drawable.baseline_warning_24)
            } else {
                tooltipInfo!!.hideNow()
            }
        }
    }

    override fun onPreferVideoModeChanged(preferVideoMode: Boolean) {
        if (!sendShown.getValue()) {
            showBottomHint(if (preferVideoMode) R.string.HoldToVideo else R.string.HoldToAudio)
        }
    }

    override fun onRecordAudioVideoError(preferVideoMode: Boolean) {
        if (!sendShown.getValue()) {
            showBottomHint(if (preferVideoMode) R.string.HoldToVideo else R.string.HoldToAudio)
        }
    }

    fun setInputVisible(visible: Boolean, notEmpty: Boolean) {
        val params1 = scrollToBottomButtonWrap!!.getLayoutParams() as RelativeLayout.LayoutParams
        val params2 = mentionButtonWrap!!.getLayoutParams() as RelativeLayout.LayoutParams
        val params3 = reactionsButtonWrap!!.getLayoutParams() as RelativeLayout.LayoutParams
        val params4 = goToNextFoundMessageButtonBadge!!.getLayoutParams() as RelativeLayout.LayoutParams
        val params5 = goToPrevFoundMessageButtonBadge!!.getLayoutParams() as RelativeLayout.LayoutParams
        if (visible) {
            params1.addRule(RelativeLayout.ABOVE, R.id.msg_bottom)
            params1.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0)
            params2.addRule(RelativeLayout.ABOVE, R.id.msg_bottom)
            params2.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0)
            params3.addRule(RelativeLayout.ABOVE, R.id.msg_bottom)
            params3.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0)
            params4.addRule(RelativeLayout.ABOVE, R.id.msg_bottom)
            params4.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0)
            params5.addRule(RelativeLayout.ABOVE, R.id.msg_bottom)
            params5.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0)
        } else {
            params1.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            params1.addRule(RelativeLayout.ABOVE, 0)
            params2.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            params2.addRule(RelativeLayout.ABOVE, 0)
            params3.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            params3.addRule(RelativeLayout.ABOVE, 0)
            params4.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            params4.addRule(RelativeLayout.ABOVE, 0)
            params5.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            params5.addRule(RelativeLayout.ABOVE, 0)
        }
        if (visible) {
            bottomWrap!!.setVisibility(View.VISIBLE)
            bottomSpace!!.setVisibility(View.VISIBLE)
            bottomShadowView!!.setVisibility(View.VISIBLE)
            if (replyBarView != null) {
                replyBarView!!.setVisibility(View.VISIBLE)
            }
            emojiButton!!.setVisibility(View.VISIBLE)
            if (notEmpty) {
                attachButtons!!.setVisibility(View.INVISIBLE)
                sendButton!!.setVisibility(View.VISIBLE)
                messageSenderButton!!.setVisibility(View.INVISIBLE)
            } else {
                attachButtons!!.setVisibility(View.VISIBLE)
                sendButton!!.setVisibility(View.INVISIBLE)
                if (canSelectSender()) {
                    messageSenderButton!!.setVisibility(View.VISIBLE)
                }
            }
        } else {
            hideActionButton()
            bottomWrap!!.setVisibility(View.GONE)
            bottomSpace!!.setVisibility(View.GONE)
            if (replyBarView != null) {
                replyBarView!!.setVisibility(View.GONE)
            }
            bottomShadowView!!.setVisibility(View.GONE)
            emojiButton!!.setVisibility(View.GONE)
            attachButtons!!.setVisibility(View.GONE)
            sendButton!!.setVisibility(View.GONE)
            messageSenderButton!!.setVisibility(View.GONE)
        }
        updateBottomBarStyle()
        updateMessagesViewInset()
    }

    override fun onEmojiUpdated(isPackSwitch: Boolean) {
        invalidateEmojiViews(false)
    }

    private var emojiScheduled = false

    fun invalidateEmojiViews(force: Boolean) {
        if (isNavigationAnimating() && !force) {
            emojiScheduled = true
            return
        }
        if (replyBarView != null) {
            replyBarView!!.invalidate()
        }
        if (messagesView != null) {
            val manager = messagesView!!.getLayoutManager() as LinearLayoutManager?
            for (i in 0..<manager!!.getChildCount()) {
                val v = manager.getChildAt(i)
                if (v != null) {
                    v.invalidate()
                }
            }
        }
        if (emojiLayout != null) {
            emojiLayout!!.invalidateAll()
        }
        if (keyboardLayout != null) {
            for (i in 0..<keyboardLayout!!.getChildCount()) {
                val v = keyboardLayout!!.getChildAt(i)
                if (v != null && v.getVisibility() == View.VISIBLE) {
                    v.invalidate()
                }
            }
        }
    }

    protected override fun getBackButton(): Int {
        return BackHeaderButton.TYPE_BACK
    }

    override fun getId(): Int {
        if (inPreviewMode) {
            when (previewMode) {
                PREVIEW_MODE_FONT_SIZE -> return R.id.controller_fontSize
                PREVIEW_MODE_WALLPAPER -> return R.id.controller_wallpaper
                PREVIEW_MODE_WALLPAPER_OBJECT -> return R.id.controller_wallpaper_preview
                PREVIEW_MODE_EVENT_LOG -> return R.id.controller_eventLog
                PREVIEW_MODE_SEARCH -> return R.id.controller_searchPreview
            }
            return 0
        }
        return R.id.controller_messages
    }

    override fun canSlideBackFrom(navigationController: NavigationController, x: Float, y: Float): Boolean {
        if (context().getRecordAudioVideoController().isOpen()) {
            return false
        }

        if (pagerScrollOffset > 0f) {
            return false
        }

        if (hasAttachedFiles()) {
            return false
        }

        if (hasEditedChanges()) {
            return false
        }

        if (getChatId() == 0L || inPreviewMode) {
            /*switch (previewMode) {
          case PREVIEW_MODE_WALLPAPER:
            View view = wallpapersList.getLayoutManager().findViewByPosition(0);
            return view != null && view.getLeft() >= 0;
        }*/
            return previewMode == PREVIEW_MODE_NONE || !(y > contentView!!.getMeasuredHeight() - bottomWrap!!.getMeasuredHeight() + HeaderView.getSize(true))
        }

        val baseY = Views.getLocationInWindow(navigationController.getValue())[1]

        /*if (areInlineResultsVisible()) {
      return false;
    }*/
        val bound: Int = (slideBackBound)

        if (emojiLayout != null && emojiLayout!!.getVisibility() == View.VISIBLE && y >= Views.getLocationInWindow(emojiLayout)[1] - baseY) {
            return emojiLayout!!.canSlideBack() && x <= bound
        }

        if (inputView != null && inputView!!.getVisibility() == View.VISIBLE) {
            val inputY = Views.getLocationInWindow(inputView)[1] - baseY
            if (y >= inputY && y < inputY + inputView!!.getMeasuredHeight()) {
                return x < emojiButton!!.getMeasuredWidth() || inputView!!.isEmpty()
            }
        }

        if (needTabs() && y < HeaderView.getSize(true)) return false

        if (Settings.instance().needChatQuickShare()) {
            if (Lang.rtl()) {
                val width = contentView!!.getMeasuredWidth()
                return width != 0 && x >= width - bound
            } else {
                return x <= bound
            }
        }

        return true
    }

    protected override fun getMenuId(): Int {
        if (areScheduled) return 0
        when (previewMode) {
            PREVIEW_MODE_EVENT_LOG -> return R.id.menu_search
            PREVIEW_MODE_SEARCH -> return 0
            PREVIEW_MODE_WALLPAPER_OBJECT, PREVIEW_MODE_FONT_SIZE -> return R.id.menu_more
            PREVIEW_MODE_WALLPAPER -> return R.id.menu_gallery
        }
        if (getChatId() != 0L) {
            if (this.isSelfChat) {
                return R.id.menu_search
            }
            if (this.isSecretChat) {
                return R.id.menu_secretChat
            }
            return R.id.menu_more
        }
        return 0
    }

    override fun getSelectMenuId(): Int {
        return R.id.menu_messageActions
    }

    override fun getName(): CharSequence? {
        when (previewMode) {
            PREVIEW_MODE_WALLPAPER -> return Lang.getString(R.string.Wallpaper)
            PREVIEW_MODE_WALLPAPER_OBJECT -> return Lang.getString(R.string.ChatBackgroundPreview)
            PREVIEW_MODE_FONT_SIZE -> return Lang.getString(R.string.TextSize)
            else -> return Lang.getString(if (this.isSelfChat) R.string.SavedMessages else R.string.ChatPreview)
        }
    }

    override fun getCustomHeaderCell(): View? {
        return if (inWallpaperPreviewMode()) headerDoubleCell else if (needTabs()) pagerHeaderView else if (getChatId() != 0L) headerCell else null
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)

        manager!!.rebuildLayouts()

        if (this.emojiKeyboardLayout != null) {
            emojiState = false
            if (emojiShown) {
                closeEmojiKeyboard()
            }
            emojiKeyboardLayout!!.rebuildLayout()
        }

        if (keyboardLayout != null) {
            commandsState = false
            if (commandsShown) {
                closeCommandsKeyboard(false)
            }
        }
    }

    fun checkEventLogSubtitle(inSearch: Boolean) {
        val res = if (inSearch) R.string.EventLogSelectedEvents else R.string.EventLogAllEvents
        val str = Lang.lowercase(Lang.getString(res))
        headerCell!!.setForcedSubtitle(str)
        headerCell!!.setSubtitle(str)
    }

    override fun fillMenuItems(id: Int, header: HeaderView, menu: LinearLayout) {
        if (id == R.id.menu_more) {
            header.addMoreButton(menu, this)
        } else if (id == R.id.menu_gallery) {
            header.addButton(menu, R.id.menu_btn_gallery, R.drawable.baseline_image_24, getHeaderIconColorId(), this, Screen.dp(52f))
        } else if (id == R.id.menu_share) {
            header.addButton(menu, R.id.menu_btn_share, R.drawable.baseline_share_arrow_24, getHeaderIconColorId(), this, Screen.dp(52f))
        } else if (id == R.id.menu_clear) {
            header.addClearButton(menu, this).setColorId(ColorId.headerLightIcon)
        } else if (id == R.id.menu_search) {
            header.addSearchButton(menu, this)
        } else if (id == R.id.menu_chat) {
            val btn = header.addButton(menu, R.id.menu_btn_viewScheduled, R.drawable.baseline_date_range_24, getHeaderIconColorId(), this, Screen.dp(52f))
            btn.setVisibility(if (tdlib!!.chatHasScheduled(getChatId())) View.VISIBLE else View.GONE)
            header.addMoreButton(menu, this)
        } else if (id == R.id.menu_secretChat) {
            val headerButton = header.addStopwatchButton(menu, this)
            headerButton.forceValue(tdlib!!.ui().getTTLShort(getChatId()), this.isSecretChat && tdlib!!.canChangeMessageAutoDeleteTime(chat!!.id))
            header.addMoreButton(menu, this)
        } else if (id == R.id.menu_messageActions) {
            val iconColorId = getSelectHeaderIconColorId()
            val selectInBetweenBtn = header.addButton(menu, R.id.menu_btn_selectInBetween, R.drawable.baseline_toc_24, iconColorId, this, Screen.dp(49f))
            selectInBetweenBtn.setThemeColorId(getSelectHeaderIconColorId())
            selectInBetweenBtn.setTag(Lang.getString(R.string.SelectMessagesInBetween))
            selectInBetweenBtn.setVisibility(View.GONE)
            var totalButtonsCount = 0
            var value: Boolean
            header.addButton(menu, R.id.menu_btn_send, R.drawable.baseline_send_24, iconColorId, this, Screen.dp(52f))
                .setVisibility(if (canSendSelectedMessages().also { value = it }) View.VISIBLE else View.GONE)
            if (value) totalButtonsCount++
            header.addViewButton(menu, this, iconColorId)
                .setVisibility(if (canViewSelectedMessages().also { value = it }) View.VISIBLE else View.GONE)
            if (value) totalButtonsCount++
            header.addReplyButton(menu, this, iconColorId)
                .setVisibility(if (canReplyToSelectedMessages().also { value = it }) View.VISIBLE else View.GONE)
            if (value) totalButtonsCount++
            header.addEditButton(menu, this, iconColorId)
                .setVisibility(if (canEditSelectedMessages().also { value = it }) View.VISIBLE else View.GONE)
            if (value) totalButtonsCount++
            header.addButton(menu, R.id.menu_btn_clearCache, R.drawable.templarian_baseline_broom_24, iconColorId, this, Screen.dp(52f))
                .setVisibility(if (canClearCacheSelectedMessages().also { value = it }) View.VISIBLE else View.GONE)
            if (value) totalButtonsCount++
            header.addButton(menu, R.id.menu_btn_unpinAll, R.drawable.deproko_baseline_pin_undo_24, iconColorId, this, Screen.dp(52f))
                .setVisibility(if (canUnpinSelectedMessages().also { value = it }) View.VISIBLE else View.GONE)
            if (value) totalButtonsCount++
            header.addRetryButton(menu, this, iconColorId)
                .setVisibility(if (canResendSelectedMessages().also { value = it }) View.VISIBLE else View.GONE)
            if (value) totalButtonsCount++
            header.addDeleteButton(menu, this, iconColorId)
                .setVisibility(if (canDeleteSelectedMessages().also { value = it }) View.VISIBLE else View.GONE)
            if (value) totalButtonsCount++

            val reportButton = header.addButton(menu, R.id.menu_btn_report, R.drawable.baseline_report_24, iconColorId, this, Screen.dp(52f))

            header.addCopyButton(menu, this, iconColorId)
                .setVisibility(if (canCopySelectedMessages().also { value = it }) View.VISIBLE else View.GONE)
            if (value) totalButtonsCount++
            header.addForwardButton(menu, this, iconColorId)
                .setVisibility(if (canShareSelectedMessages().also { value = it }) View.VISIBLE else View.GONE)
            if (value) totalButtonsCount++

            reportButton.setVisibility(if (canReportSelectedMessages(totalButtonsCount)) View.VISIBLE else View.GONE)
        }
    }

    private val singleSelectedMessage: MessageWithProperties?
        get() {
            if (selectedMessageIds != null && selectedMessageIds!!.size() == 1) {
                val messageId = selectedMessageIds!!.keyAt(0)
                val m = selectedMessageIds!!.valueAt(0)
                val message = m.getMessage(messageId)!!
                val properties = m.lastMessageProperties(messageId)
                return MessageWithProperties(message, properties)
            }
            return null
        }

    override fun onMenuItemPressed(id: Int, view: View?) {
        if (id == R.id.menu_btn_more) {
            if (inPreviewMode) {
                if (previewMode == PREVIEW_MODE_FONT_SIZE) {
                    val ids = IntList(2)
                    val strings = StringList(2)
                    ids.append(R.id.btn_chatFontSizeScale)
                    strings.append(if (Settings.instance().needChatFontSizeScaling()) R.string.TextSizeScaleDisable else R.string.TextSizeScaleEnable)
                    if (Settings.instance().canResetChatFontSize()) {
                        ids.append(R.id.btn_chatFontSizeReset)
                        strings.append(R.string.TextSizeReset)
                    }
                    showMore(ids.get(), strings.get(), 0)
                } else if (previewMode == PREVIEW_MODE_WALLPAPER_OBJECT) {
                    val ids = IntList(2)
                    val strings = StringList(2)
                    ids.append(R.id.btn_share)
                    strings.append(R.string.Share)
                    ids.append(R.id.btn_copyLink)
                    strings.append(R.string.CopyLink)
                    showMore(ids.get(), strings.get(), 0)
                }
            } else {
                if (inputView != null && inputView!!.canFormatText()) {
                    val count = 6
                    val ids = IntList(count)
                    val strings = StringList(count)
                    val icons = IntList(count)

                    if (inputView!!.canClearTextFormat()) {
                        ids.append(R.id.btn_plain)
                        strings.append(R.string.TextFormatClear)
                        icons.append(R.drawable.baseline_format_clear_24)
                    }

                    ids.append(R.id.btn_bold)
                    strings.append(R.string.TextFormatBold)
                    icons.append(R.drawable.baseline_format_bold_24)

                    ids.append(R.id.btn_italic)
                    strings.append(R.string.TextFormatItalic)
                    icons.append(R.drawable.baseline_format_italic_24)

                    ids.append(R.id.btn_underline)
                    strings.append(R.string.TextFormatUnderline)
                    icons.append(R.drawable.baseline_format_underlined_24)

                    ids.append(R.id.btn_strikethrough)
                    strings.append(R.string.TextFormatStrikethrough)
                    icons.append(R.drawable.baseline_strikethrough_s_24)

                    ids.append(R.id.btn_monospace)
                    strings.append(R.string.TextFormatMonospace)
                    icons.append(R.drawable.baseline_code_24)

                    ids.append(R.id.btn_spoiler)
                    strings.append(R.string.TextFormatSpoiler)
                    icons.append(R.drawable.baseline_eye_off_24)

                    ids.append(R.id.btn_link)
                    strings.append(R.string.TextFormatLink)
                    icons.append(R.drawable.baseline_link_24)

                    ids.append(R.id.btn_quote)
                    strings.append(R.string.TextFormatQuote)
                    icons.append(R.drawable.baseline_format_quote_close_24)

                    showOptions(null, ids.get(), strings.get(), null, icons.get(), OptionDelegate { itemView: View?, id1: Int -> inputView!!.setSpan(id1) })
                } else {
                    showMore()
                }
            }
        } else if (id == R.id.menu_btn_gallery) {
            Intents.openGallery(context, false)
        } else if (id == R.id.menu_btn_clear) {
            if (this.isEventLog) {
                clearSearchInput()
            } else {
                clearChatSearchInput()
            }
        } else if (id == R.id.menu_btn_search) {
            if (manager!!.isReadyToSearch()) {
                hideBottomHint()
                openSearchMode()
            }
        } else if (id == R.id.menu_btn_selectInBetween) {
            selectMessagesInBetween()
        } else if (id == R.id.menu_btn_stopwatch) {
            if (this.isSecretChat) {
                tdlib!!.ui().showTTLPicker(context(), chat!!)
            }
        } else if (id == R.id.menu_btn_viewScheduled) {
            viewScheduledMessages(false)
        } else if (id == R.id.menu_btn_copy) {
            if (selectedMessageIds != null) {
                copySelectedMessages()
                finishSelectMode(-1)
            }
        } else if (id == R.id.menu_btn_report) {
            if (selectedMessageIds != null) {
                val messages = selectedMessagesToArray()
                if (messages != null) {
                    reportChat(messages, Runnable { finishSelectMode(-1) })
                }
            }
        } else if (id == R.id.menu_btn_forward) {
            if (pagerScrollPosition != 0 && pagerContentAdapter != null) {
                val adapter = pagerContentAdapter!!
                val c = adapter.cachedItems.get(pagerScrollPosition)
                if (c != null) {
                    c.shareMessages()
                }
                return
            }
            if (selectedMessageIds != null) {
                val size = selectedMessageIds!!.size()
                if (size > 0) {
                    val messages = arrayOfNulls<TdApi.Message>(size)
                    for (i in 0..<size) {
                        val messageId = selectedMessageIds!!.keyAt(i)
                        val message = selectedMessageIds!!.valueAt(i).getMessage(messageId)
                        messages[i] = message
                    }
                    shareMessages(messages, true)
                }
            }
        } else if (id == R.id.menu_btn_reply) {
            val m = this.singleSelectedMessage
            if (m != null) {
                showReply(m, null, 0, "", true, true)
                finishSelectMode(-1)
                if (inputView != null && inputView!!.isEmpty()) {
                    Keyboard.show(inputView)
                }
            }
        } else if (id == R.id.menu_btn_edit) {
            val m = this.singleSelectedMessage
            if (m != null) {
                editMessage(m)
            }
        } else if (id == R.id.menu_btn_view) {
            if (pagerScrollPosition != 0 && pagerContentAdapter != null) {
                val adapter = pagerContentAdapter!!
                val c = adapter.cachedItems.get(pagerScrollPosition)
                if (c != null) {
                    val messageId = c.singularMessageId
                    if (messageId != null) {
                        highlightMessage(messageId)
                        c.setInMediaSelectMode(false)
                    }
                }
            }
        } else if (id == R.id.menu_btn_send) {
            if (selectedMessageIds != null && selectedMessageIds!!.size() > 0) {
                showOptions(
                    Lang.pluralBold(R.string.SendXMessagesNow, selectedMessageIds!!.size().toLong()),
                    intArrayOf(R.id.btn_send, R.id.btn_cancel),
                    arrayOf<String?>(
                        Lang.getString(R.string.SendNow), Lang.getString(R.string.Cancel)
                    ),
                    null,
                    intArrayOf(R.drawable.baseline_send_24, R.drawable.baseline_cancel_24),
                    OptionDelegate { v: View?, optionId: Int ->
                        if (optionId == R.id.btn_send && selectedMessageIds != null) {
                            for (i in selectedMessageIds!!.size() - 1 downTo 0) {
                                val messageId = selectedMessageIds!!.keyAt(i)
                                tdlib!!.send<TdApi.Ok?>(
                                    EditMessageSchedulingState(selectedMessageIds!!.valueAt(i).chatId, messageId, null),
                                    tdlib!!.typedOkHandler()
                                )
                            }
                            finishSelectMode(-1)
                        }
                        true
                    })
            }
        } else if (id == R.id.menu_btn_clearCache) {
            if (pagerScrollPosition != 0 && pagerContentAdapter != null) {
                val adapter = pagerContentAdapter!!
                val c = adapter.cachedItems.get(pagerScrollPosition)
                if (c != null) {
                    c.clearMessages()
                }
                return
            }
            if (selectedMessageIds != null && selectedMessageIds!!.size() > 0) {
                val size = selectedMessageIds!!.size()
                val files = SparseArrayCompat<TdApi.File?>(size)
                for (i in 0..<size) {
                    val message = selectedMessageIds!!.valueAt(i).getMessage(selectedMessageIds!!.keyAt(i))
                    val filesList = TD.getFiles(message)
                    if (filesList != null) {
                        for (file in filesList) {
                            if (TD.canDeleteFile(message, file)) {
                                files.put(file.id, file)
                            }
                        }
                    }
                }
                TD.deleteFiles(this, asArray<TdApi.File?>(files, arrayOfNulls<TdApi.File>(files.size())), Runnable { finishSelectMode(-1) })
            }
        } else if (id == R.id.menu_btn_unpinAll) {
            if (selectedMessageIds != null && selectedMessageIds!!.size() > 0) {
                showOptions(
                    Lang.pluralBold(R.string.UnpinXMessages, selectedMessageIds!!.size().toLong()),
                    intArrayOf(R.id.btn_unpinAll, R.id.btn_cancel),
                    arrayOf<String?>(
                        Lang.getString(R.string.Unpin), Lang.getString(R.string.Cancel)
                    ),
                    intArrayOf(OptionColor.RED, OptionColor.NORMAL),
                    intArrayOf(R.drawable.deproko_baseline_pin_undo_24, R.drawable.baseline_cancel_24),
                    OptionDelegate { itemView: View?, viewId: Int ->
                        if (viewId == R.id.btn_unpinAll) {
                            val size = selectedMessageIds!!.size()
                            for (i in 0..<size) {
                                tdlib!!.send<TdApi.Ok?>(UnpinChatMessage(chat!!.id, selectedMessageIds!!.keyAt(i)), tdlib!!.typedOkHandler())
                            }
                            exitOnTransformFinish = true
                            finishSelectMode(-1)
                        }
                        true
                    })
            }
        } else if (id == R.id.menu_btn_delete) {
            if (pagerScrollPosition != 0 && pagerContentAdapter != null) {
                val adapter = pagerContentAdapter!!
                val c = adapter.cachedItems.get(pagerScrollPosition)
                if (c != null) {
                    c.deleteMessages()
                }
                return
            }
            if (selectedMessageIds != null && selectedMessageIds!!.size() > 0) {
                val size = selectedMessageIds!!.size()
                @Suppress("UNCHECKED_CAST")
                val messages = arrayOfNulls<MessageWithProperties>(size) as Array<MessageWithProperties>
                for (i in 0..<size) {
                    val messageId = selectedMessageIds!!.keyAt(i)
                    val m = selectedMessageIds!!.valueAt(i)
                    val message = m.getMessage(messageId)!!
                    val properties = m.lastMessageProperties(messageId)
                    messages[i] = MessageWithProperties(message, properties)
                }
                tdlib!!.ui().showDeleteOptions(this, messages, Runnable { finishSelectMode(-1) })
            }
        } else if (id == R.id.menu_btn_retry) {
            if (selectedMessageIds != null && selectedMessageIds!!.size() > 0) {
                var count = 0
                var lastMediaGroupId: Long = 0
                var lastChatId: Long = 0
                for (i in 0..<selectedMessageIds!!.size()) {
                    val messageId = selectedMessageIds!!.keyAt(i)
                    val message = selectedMessageIds!!.valueAt(i).getMessage(messageId)!!
                    if (lastChatId != message.chatId || lastMediaGroupId != message.mediaAlbumId || lastMediaGroupId == 0L) {
                        lastChatId = message.chatId
                        lastMediaGroupId = message.mediaAlbumId
                        count++
                    }
                }
                if (count > 0) {
                    showOptions(
                        intArrayOf(R.id.btn_messageResend, R.id.btn_cancel),
                        arrayOf<String?>(Lang.plural(R.string.ResendXMessages, count.toLong()), Lang.getString(R.string.Cancel)),
                        intArrayOf(OptionColor.BLUE, OptionColor.NORMAL),
                        OptionDelegate { v: View?, optionId: Int ->
                            if (optionId == R.id.btn_messageResend) {
                                resendSelectedMessages()
                                finishSelectMode(-1)
                            }
                            true
                        })
                }
            }
        } else if (id == R.id.menu_btn_up) {
            moveSearchSelection(true)
        } else if (id == R.id.menu_btn_down) {
            moveSearchSelection(false)
        }
    }

    private fun resendSelectedMessages() {
        if (selectedMessageIds != null && selectedMessageIds!!.size() > 0) {
            val messageIds = LongArray(selectedMessageIds!!.size())
            for (i in messageIds.indices) {
                messageIds[i] = selectedMessageIds!!.keyAt(i)
            }
            tdlib!!.resendMessages(getChatId(), messageIds)
        }
    }

    val isChatFocused: Boolean
        get() = manager!!.isFocused()

    private var resetOnFocus = false

    fun setResetOnFocus() {
        if (!this.resetOnFocus) {
            this.resetOnFocus = true
            tdlib!!.context().global().addAccountListener(this)
        }
    }

    private fun clearResetOnFocus() {
        if (this.resetOnFocus) {
            this.resetOnFocus = false
            tdlib!!.context().global().removeAccountListener(this)
        }
    }

    override fun onAccountSwitched(newAccount: TdlibAccount, profile: TdApi.User?, reason: Int, oldAccount: TdlibAccount?) {
        if (tdlib!!.id() != newAccount.id) {
            clearResetOnFocus()
        }
    }

    private fun showMessageMenuTutorial() {
        if (sendShown.getValue() && !areScheduledOnly() && !this.isInputLess && canWriteMessages() && hasSendBasicMessagePermission() && !this.isEditingMessage && !this.isSecretChat && isFocused() && !sendButton!!.inInlineMode()) {
            val tutorialFlag: Long
            if (this.isSelfChat) {
                tutorialFlag = Settings.TUTORIAL_SET_REMINDER
            } else {
                tutorialFlag = Settings.TUTORIAL_SCHEDULE
            }
            if (Settings.instance().needTutorial(tutorialFlag)) {
                val userId = tdlib!!.chatUserId(getChatId())
                val canSendOnceOnline = !this.isSelfChat && tdlib!!.cache().userLastSeenAvailable(userId)
                val tutorialKey =
                    tutorialFlag.toString() + (if (this.isSelfChat) "_self" else if (canSendOnceOnline) "_online" else if (this.isChannel) "_channel" else "")
                if (shownTutorials == null || !shownTutorials!!.contains(tutorialKey)) {
                    if (shownTutorials == null) shownTutorials = HashSet<String?>()
                    shownTutorials!!.add(tutorialKey)
                    if (this.isSelfChat) {
                        showBottomHint(R.string.HoldToRemind)
                    } else if (canSendOnceOnline) {
                        showBottomHint(Lang.getStringBold(R.string.HoldToSchedule2, tdlib!!.cache().userFirstName(userId)), false)
                    } else if (this.isChannel) {
                        showBottomHint(R.string.HoldToSilentBroadcast)
                    } else {
                        showBottomHint(R.string.HoldToSchedule)
                    }
                }
            }
        }
    }

    fun openVoiceChatInvitation(invitation: InternalLinkTypeVideoChat?) {
        tdlib!!.ui().openVoiceChatInvitation(this, invitation)
    }

    private var promptDraftPrefillOnFocus = false

    fun fillDraft(fillDraft: TdApi.FormattedText?, checkCurrentDraft: Boolean): Boolean {
        if (fillDraft.isEmpty()) {
            return false
        }
        val act = Runnable {
            if (inputView != null) {
                inputView!!.setDraft(InputMessageText(fillDraft, null, false))
            }
        }
        val currentDraft = this.draftMessage
        if (checkCurrentDraft && !currentDraft.isEmpty()) {
            val currentText = (currentDraft.inputMessageText as InputMessageText).text
            if (!currentText.isEmpty() && !currentText.equalsTo(fillDraft)) {
                showWarning(Lang.getMarkdownString(this, R.string.DraftPreFillWarning), RunnableBool { isConfirmed: Boolean ->
                    if (isConfirmed) {
                        act.run()
                    }
                })
                return true
            }
        }
        act.run()
        return true
    }

    override fun onFocus() {
        super.onFocus()
        if (promptDraftPrefillOnFocus) {
            promptDraftPrefillOnFocus = false
            fillDraft(this.fillDraft, true)
        }
        if (chat != null && !isInForceTouchMode()) {
            val source = tdlib!!.chatSource(openedFromChatList, chat!!.id)
            if (source != null && Settings.instance().needTutorial(source)) {
                context().tooltipManager().builder(headerCell)
                    .controller(this)
                    .preventHideOnTouch(true)
                    .fillWidth(true)
                    .noPivot(true)
                    .needBlink(true)
                    .icon(R.drawable.baseline_info_24)
                    .chatTextSize()
                    .show(tdlib, Lang.getTutorial(this, source))
                    .addOnCloseListener(RunnableLong { duration: Long ->
                        if (duration >= TimeUnit.SECONDS.toMillis(5)) {
                            Settings.instance().markTutorialAsComplete(source)
                        }
                    })
                    .hideDelayed(10, TimeUnit.SECONDS)
            }
            if (voiceChatInvitation != null) {
                openVoiceChatInvitation(voiceChatInvitation)
                voiceChatInvitation = null
            }
        }
        showMessageMenuTutorial()
        if (!resetOnFocus && !inPreviewMode && !isInForceTouchMode()) {
            TdlibManager.instance().changePreferredAccountId(tdlib!!.id(), TdlibManager.SWITCH_REASON_CHAT_FOCUS, RunnableBool { success: Boolean ->
                if (success && isFocused()) {
                    resetOnFocus = true
                    resetOnFocus()
                }
            })
        }
        UI.setSoftInputMode(context, Config.DEFAULT_WINDOW_PARAMS)
        if (!inPreviewMode() && tdlib != null) {
            (context as MainActivity).destroyMessageControllers(tdlib!!.id())
        }
        if (pagerContentAdapter != null && pagerContentAdapter!!.isLocked) {
            pagerContentAdapter!!.isLocked = false
            pagerContentAdapter!!.notifyDataSetChanged()
        }
        messagesView!!.setMessageAnimatorEnabled(true)
        registerRaiseListener()
        manager!!.setParentFocused(true)
        if (scheduledKeyboardMessageId != 0L) {
            openCommandsKeyboard(scheduledKeyboardMessageId, scheduledKeyboard!!, false, false)
            scheduledKeyboardMessageId = 0
            scheduledKeyboard = null
        }
        if (InputView.USE_ANDROID_SELECTION_FIX) {
            if (bottomWrap!!.getVisibility() == View.VISIBLE && inputView != null && inputView!!.isEnabled() && !this.isInputLess) {
                inputView!!.setEnabled(false)
                inputView!!.setEnabled(true)
            }
        }
        if (inputView != null && inputView!!.isEnabled()) {
            inputView!!.requestFocus()
        }
        if (emojiScheduled) {
            invalidateEmojiViews(true)
            emojiScheduled = false
        }
        updateCounters(true)
        if (!inPreviewMode) {
            val stackSize = stackSize()
            if (stackSize == 4 && stackItemAt(2) is CreateGroupController) {
                destroyStackItemAt(2)
                destroyStackItemAt(1)
            } else if (stackSize == 3 && stackItemAt(1) is CreateGroupController) {
                destroyStackItemAt(1)
            }
            destroyStackItemById(R.id.controller_call)
            val c = previousStackItem()
            if (c is ChatsController && c.isPicker()) {
                destroyStackItemAt(stackSize() - 2)
            }
        }
        resetOnFocus()
        if (openKeyboard) {
            openKeyboard = false
            Keyboard.show(inputView)
        }
        // showEmojiSuggestionsIfTemporarilyHidden();
        // tdlib!!.context().changePreferredAccountId(tdlib!!.id(), TdlibManager.SWITCH_REASON_CHAT_FOCUS);
    }

    private fun resetOnFocus() {
        if (resetOnFocus && navigationController() != null) {
            clearResetOnFocus()
            val stack = navigationController()!!.getStack()
            stack.destroyAllButSaveLast(1)
            val c = MainController(context, tdlib!!)
            c.getValue()
            stack.insert(c, 0)
        }
    }

    override fun shouldDisallowScreenshots(): Boolean {
        return (chat != null && (this.isSecretChat || chat!!.hasProtectedContent || manager!!.hasVisibleProtectedContent())) || super.shouldDisallowScreenshots()
    }

    override fun onPrepareToShow() {
        super.onPrepareToShow()
        triggerOneShot = false
        if (headerCell != null) { // Fix for new profiles
            headerCell!!.setTranslationX(0f)
        }
        tdlib!!.ui().updateTTLButton(R.id.menu_secretChat, headerView, chat, true)
        shareItem()
        if (!isEmpty(previewSearchQuery) && headerView != null) {
            headerView!!.openSearchMode(false, false)
            setSearchInput(previewSearchQuery!!)
            lastMessageSearchQuery = previewSearchQuery
            previewSearchQuery = null
            return
        }
        if (previewSearchSender != null && headerView != null) {
            viewMessagesFromSender(previewSearchSender, false)
        }
    }

    fun canSaveDraft(): Boolean {
        return canWriteMessagesOrWaitingForReply() && getChatId() != 0L && !inPreviewMode() && !isInForceTouchMode()
    }

    private fun saveDraft() {
        if (canSaveDraft()) {
            if (this.isEditingMessage) {
                // TODO save local draft
            } else if (inputView != null && inputView!!.textChangedSinceChatOpened() && isFocused()) {
                val outputText = inputView!!.getOutputText(false)
                val replyTo = this.currentReplyId
                val date = tdlib!!.currentTime(TimeUnit.SECONDS)
                val inputMessageText = InputMessageText(
                    outputText,
                    findTargetContext().linkPreviewOptions,
                    false
                )
                val draftMessage = DraftMessage(
                    if (replyTo != null) replyTo.toInputMessageReply() else null,
                    date.toInt(),
                    inputMessageText,
                    0,
                    null
                )
                val outputChatId = if (messageThread != null) messageThread!!.getChatId() else getChatId()
                val topicId = if (messageThread != null) messageThread!!.getMessageTopicId() else getMessageTopicId()
                if (messageThread != null) {
                    messageThread!!.setDraft(draftMessage)
                }
                tdlib!!.send<TdApi.Ok?>(
                    SetChatDraftMessage(
                        outputChatId,
                        topicId,
                        if (!draftMessage.isEmpty()) draftMessage else null
                    ), tdlib!!.typedOkHandler()
                )
                if (hasAttachedFiles()) {
                    // TODO save local draft ?
                }
            }
        }
    }

    override fun onCleanAfterHide() {
        super.onCleanAfterHide()
        collapsePinnedMessagesBar(false)
    }

    fun collapsePinnedMessagesBar(animated: Boolean) {
        if (pinnedMessagesBar != null) {
            pinnedMessagesBar!!.collapse(animated)
        }
    }

    override fun onBlur() {
        saveDraft()

        super.onBlur()

        messagesView!!.stopScroll()

        if (preventHideKeyboard) {
            preventHideKeyboard = false
        } else {
            hideSoftwareKeyboard()
        }
        unregisterRaiseListener()
        manager!!.setParentFocused(false)

        if (translationPopup != null) {
            translationPopup!!.hidePopupWindow(true)
        }
        cancelSheduledKeyboardOpeningAndHideAllKeyboards()
        // hideEmojiSuggestionsTemporarily();
        // closeEmojiKeyboard();
        // Media.instance().stopVoice();
    }

    private var messagesHidden = false

    private fun setHideMessages(hide: Boolean) {
        if (this.messagesHidden != hide) {
            this.messagesHidden = hide
            manager!!.setParentHidden(hide)
            // messagesView.setVisibility(hide ? View.GONE : View.VISIBLE);
            // TODO checkPinnedMessage();
            if (hide) {
                setScrollToBottomVisible(false, isFocused())
            }
        }
    }

    private fun checkLinkedChat() {
        val supergroupId = toSupergroupId(getChatId())
        val info = if (supergroupId != 0L) tdlib!!.cache().supergroupFull(supergroupId) else null
        val linkedChatId = if (info != null) info.linkedChatId else 0
        if (this.linkedChatId != linkedChatId) {
            this.linkedChatId = linkedChatId
            updateBottomBar(true)
        }
    }

    private fun checkRestriction() {
        if (!inPreviewMode() && !this.isEventLog) {
            setHideMessages(tdlib!!.chatRestricted(chat))
        }
    }

    override fun onActivityPause() {
        super.onActivityPause()
        checkBroadcastingSomeAction()
        unregisterRaiseListener()
        manager!!.setParentPaused(true)
        saveDraft()
        if (!getKeyboardState() && !this.isInputLess) {
            enableOnResume = true
            if (inputView != null) {
                inputView!!.setEnabled(false)
            }
        }
        /* FIXME I hope it works fine

    emojiState = false;
    closeEmojiKeyboard();*/
    }

    override fun onActivityResume() {
        super.onActivityResume()
        checkBroadcastingSomeAction()
        registerRaiseListener()
        manager!!.setParentPaused(false)
        if (enableOnResume) {
            UI.unlock(this, 200L)
        }
    }

    override fun destroy() {
        resetSelectableControl()

        discardAttachedFiles(false)
        setScrollToBottomVisible(false, false)
        applyPlayerOffset(0f, 0f)
        hideActionButton()
        setBroadcastAction(ChatActionCancel.CONSTRUCTOR)

        cancelJumpToDate()
        cancelAdminsRequest()
        resetSearchControls()
        triggerOneShot = true

        if (searchByAvatarView != null) {
            searchByAvatarView!!.performDestroy()
        }
        if (searchByUserViewWrapper != null) {
            searchByUserViewWrapper!!.dismiss()
        }
        canSendMessageToUser = null

        if (manager != null) {
            manager.destroy(this)
        }
        if (attachedFiles != null) {
            context.removeFromRoot(attachedFiles)
        }

        if (tooltipInfo != null) {
            tooltipInfo!!.destroy()
            tooltipInfo = null
        }

        if (emojiLayout != null) {
            emojiLayout!!.reset()
        }

        if (wallpaperViewBlurPreview != null) {
            wallpaperViewBlurPreview!!.performDestroy()
        }

        if (backgroundParamsView != null) {
            backgroundParamsView!!.performDestroy()

            if (inWallpaperMode() && !inWallpaperPreviewMode()) {
                val background = tdlib!!.settings().getWallpaper(Theme.getWallpaperIdentifier())
                if (background != null && background.isWallpaper()) {
                    tdlib!!.settings().setWallpaper(
                        TGBackground.newBlurredWallpaper(tdlib, background, backgroundParamsView!!.isBlurred()),
                        true,
                        Theme.getWallpaperIdentifier()
                    )
                }
            }
        }

        botStartArgument = null

        // switch pm state
        clearSwitchPmButton()

        // edit message state
        resetEditState()

        setInputBlockFlags(0)

        if (inputView != null) {
            val inputText = inputView!!.getInput()
            if (inputText.isEmpty()) {
                updateSendButton("", false)
            } else {
                inputView!!.setText("")
            }
        }

        if (pagerContentAdapter != null) {
            pagerContentAdapter!!.destroy()
        }

        // toast shit
        forceHideToast()

        pinnedMessagesBar!!.performDestroy()

        if (requestsView != null) {
            requestsView!!.performDestroy()
        }

        if (reactionsButton != null) {
            reactionsButton!!.performDestroy()
        }

        if (sendButton != null) {
            sendButton!!.destroySlowModeCounterController()
        }

        // messagesView.clear();
        closeEmojiKeyboard()
        clearScheduledKeyboard()

        setSearchTransformFactor(0f, false)

        if (headerCell != null) {
            headerCell!!.pause()
        }

        Views.destroyRecyclerView(messagesView)

        // TODO chat = null;
        if (destroyInstance || !reuseEnabled) {
            super.destroy()
            if (liveLocation != null) {
                liveLocation!!.destroy()
                liveLocation = null
            }
            if (pinnedMessagesBar != null) pinnedMessagesBar!!.completeDestroy()
            if (replyBarView != null) replyBarView!!.completeDestroy()
            if (topBar != null) topBar!!.performDestroy()
            if (replyBarView != null) replyBarView!!.performDestroy()
            recordButton!!.performDestroy()
            if (inputView != null) inputView!!.performDestroy()
            if (wallpaperView != null) wallpaperView!!.performDestroy()
            if (googleClient != null) {
                closeGoogleClient()
            }
            if (botHelper != null) {
                botHelper!!.destroy()
            }
            removeStaticListeners()
            Views.destroyRecyclerView(wallpapersList)
            if (wallpapersList != null) {
                (wallpapersList!!.getAdapter() as WallpaperAdapter).destroy()
            }
            tdlib!!.settings().removeJoinRequestsDismissListener(this)
            TGLegacyManager.instance().removeEmojiListener(this)
            if (emojiLayout != null) {
                emojiLayout!!.destroy()
            }
            manager!!.release()
        }

        checkBroadcastingSomeAction()

        U.gc()
    }

    // Show more
    fun showMore() {
        if (chat == null) {
            return
        }

        val ids = IntList(4)
        val strings = StringList(4)

        val status = tdlib!!.chatStatus(chat!!.id)

        if (messageThread != null) {
            if (!manager!!.isTotallyEmpty() && !messagesHidden) {
                ids.append(R.id.btn_search)
                strings.append(R.string.Search)
            }
            if (status != null && TD.isAdmin(status) || tdlib!!.canChangeInfo(chat)) {
                ids.append(R.id.btn_manageGroup)
                strings.append(R.string.ManageGroup)
            }
            if (tdlib!!.canDeleteMessages(messageThread!!.getChatId())) {
                val message = messageThread!!.getOldestMessage()
                val properties = tdlib!!.getMessagePropertiesSync(message!!)
                if (properties.canGetMessageThread && properties.canBeDeletedForAllUsers) {
                    ids.append(R.id.btn_deleteThread)
                    strings.append(R.string.DeleteThread)
                }
            }
            if (messageThread!!.getChatId() != 0L && messageThread!!.getChatId() != messageThread!!.getContextChatId()) {
                ids.append(R.id.btn_openLinkedChat)
                strings.append(R.string.LinkedGroup)
            }
            if (tdlib!!.chatHasScheduled(messageThread!!.getChatId())) {
                ids.append(R.id.btn_viewScheduled)
                strings.append(R.string.ScheduledMessages)
            }
            showMore(ids.get(), strings.get())
            return
        }

        if (!manager!!.isTotallyEmpty() && (Config.USE_SECRET_SEARCH || !this.isSecretChat) && !messagesHidden) {
            ids.append(R.id.btn_search)
            strings.append(R.string.Search)
        }

        if ((!tdlib!!.isChannel(chat!!.id) || (status != null && !TD.isLeft(status))) && !tdlib!!.isSelfChat(chat!!.id)) {
            ids.append(R.id.btn_mute)
            strings.append(if (tdlib!!.chatNotificationsEnabled(chat!!.id)) R.string.Mute else R.string.Unmute)
        }

        if (tdlib!!.canReportChatSpam(chat!!.id)) {
            ids.append(R.id.btn_reportChat)
            strings.append(R.string.Report)
        }

        if (tdlib!!.canSetPasscode(chat)) {
            ids.append(R.id.btn_setPasscode)
            strings.append(R.string.PasscodeTitle)
        }
        tdlib!!.ui().addDeleteChatOptions(getChatId(), ids, strings, !tdlib!!.isChannel(chat!!.id), false)

        if (!messagesHidden) {
            if (isUserChat(chat!!.id)) {
                val user = tdlib!!.chatUser(chat!!)
                if (TD.suggestSharingContact(user)) {
                    ids.append(R.id.btn_shareMyContact)
                    strings.append(R.string.ShareMyContactInfo)
                }
            }
            if (manager.canRestorePinnedMessage()) {
                ids.append(R.id.btn_showPinnedMessage)
                strings.append(R.string.PinnedMessage)
            }

            if (tdlib!!.isBotChat(chat!!) && botHelper != null) {
                if (botHelper!!.findHelpCommand() != null) {
                    ids.append(R.id.btn_botHelp)
                    strings.append(R.string.BotHelp)
                }
                if (botHelper!!.findSettingsCommand() != null) {
                    ids.append(R.id.btn_botSettings)
                    strings.append(R.string.BotSettings)
                }
            }
        }

        if (linkedChatId != 0L && bottomButtonAction != BOTTOM_ACTION_DISCUSS) {
            ids.append(R.id.btn_openLinkedChat)
            strings.append(if (tdlib!!.isChannel(getChatId())) R.string.LinkedGroup else R.string.LinkedChannel)
        }
        if (tdlib!!.hasDirectMessagesChat(getChatId())) {
            ids.append(R.id.btn_openDirectMessages)
            strings.append(R.string.DirectMessages)
        }

        if (BuildConfig.DEBUG) {
            if (TD.isSecretChat(chat!!.type)) {
                ids.append(R.id.btn_sendScreenshotNotification)
                strings.append("Send screenshot notification")
            }
            if (!hasSendBasicMessagePermission()) {
                ids.append(R.id.btn_debugShowHideBottomBar)
                strings.append("Show/hide bottom bar")
            }
        }

        showMore(ids.get(), strings.get(), 0)
    }

    // Clear history
    fun deleteAndLeave() {
        if (!isFocused() || chat == null) {
            return
        }
        tdlib!!.deleteChat(getChatId(), false, null)
        tdlib!!.ui().exitToChatScreen(this, getChatId())
    }

    // Message options
    class MessageContext @JvmOverloads constructor(
        val message: TGMessage,
        val tag: Any? = null,
        val messageSender: ChatMember? = null,
        val disableMetadata: Boolean = false
    )

    fun showMessageOptions(
        msg: TGMessage,
        ids: IntArray,
        options: Array<String?>?,
        icons: IntArray?,
        selectedMessageTag: Any?,
        selectedMessageSender: ChatMember?,
        disableMessageMetadata: Boolean
    ) {
        showMessageOptions(MessageContext(msg, selectedMessageTag, selectedMessageSender, disableMessageMetadata), ids, options, icons)
    }

    fun showMessageOptions(messageContext: MessageContext, ids: IntArray, options: Array<String?>?, icons: IntArray?) {
        val msg = messageContext.message
        val b = SpannableStringBuilder()
        if (chat != null) {
            val isChannel = tdlib!!.isChannel(chat!!.id)
            if (!isChannel && msg.message.content != null) {
                when (msg.message.content.getConstructor()) {
                    MessageSticker.CONSTRUCTOR -> {
                        val from = tdlib!!.messageAuthor(msg.message, true, true)
                        if (!isEmpty(from)) {
                            b.append(from)
                            b.append(": ")
                        }
                        val contentPreview = ContentPreview.getChatListPreview(tdlib, msg.chatId, msg.message, true)
                        b.append(contentPreview.buildText(false))
                    }

                    MessageDice.CONSTRUCTOR -> {
                        val emoji = (msg.message.content as MessageDice).emoji
                        b.append(
                            Lang.getString(
                                if (ContentPreview.EMOJI_DART.textRepresentation == emoji) R.string.SendDartHint else if (ContentPreview.EMOJI_DICE.textRepresentation == emoji) R.string.SendDiceHint else R.string.SendUnknownDiceHint,
                                emoji
                            )
                        )
                    }
                }
            }
            if (isChannel && !msg.isScheduled) {
                if (msg.viewCount > 0) {
                    if (b.length > 0) {
                        b.append(", ")
                    }
                    b.append(Lang.pluralBold(R.string.xViews, msg.viewCount.toLong()))
                }
                if (msg.forwardCount > 0) {
                    if (b.length > 0) {
                        b.append(", ")
                    }
                    b.append(Lang.pluralBold(R.string.StatsXShared, msg.forwardCount.toLong()))
                }
                if (msg.messageReactions!!.getTotalCount() > 0) {
                    if (b.length > 0) {
                        b.append(", ")
                    }
                    b.append(Lang.pluralBold(R.string.xReacted, msg.messageReactions!!.getTotalCount().toLong()))
                }
            }
        }
        if (msg.isFailed) {
            val errors = msg.failureMessages
            if (errors != null) {
                if (b.length > 0) {
                    // b.append("\n");
                    b.append(". ")
                }
                b.append(Lang.getString(R.string.SendFailureInfo, Strings.join(", ", *errors as Array<Any?>)))
            }
        }
        if (msg.isSponsoredMessage()) {
            val additionalInfo = msg.sponsoredMessage!!.additionalInfo
            if (!isEmpty(additionalInfo)) {
                if (b.length > 0) {
                    b.append('\n')
                }
                b.append(additionalInfo)
            }
        }
        if (!msg.canBeSaved()) {
            if (b.length > 0) {
                // b.append("\n\n");
                b.append(". ")
            }
            val senderId = msg.message.senderId
            val resId: Int
            if (tdlib!!.cache().senderBot(senderId)) {
                resId = R.string.RestrictSavingBotInfo
            } else if (tdlib!!.isChannel(senderId)) {
                resId = R.string.RestrictSavingChannelInfo
            } else if (tdlib!!.isUser(senderId)) {
                resId = R.string.RestrictSavingUserInfo
            } else {
                resId = R.string.RestrictSavingGroupInfo
            }
            b.append(Lang.getString(resId))
        }

        val text = trim(b)

        msg.loadAllMessageProperties(Runnable {
            msg.loadAvailableReactions(Runnable {
                val shown = AtomicBoolean(false)
                val inject = AtomicReference<RunnableData<MessageReadDate?>?>()
                msg.checkReadDate(RunnableBool { expectAgain: Boolean ->
                    val readDate = msg.readDate
                    if (shown.getAndSet(true)) {
                        if (!expectAgain && readDate != null && readDate.getConstructor() == MessageReadDateRead.CONSTRUCTOR) {
                            val act = inject.get()
                            if (act != null) {
                                act.runWithData(readDate)
                            }
                        }
                        return@RunnableBool
                    }

                    val messageHandler = newMessageOptionDelegate(messageContext)
                    val messageOptionsBuilder = Options.Builder()
                        .info(if (isEmpty(text)) null else text)
                        .items(ids, options!!, null, icons)
                    if (readDate != null) {
                        when (readDate.getConstructor()) {
                            MessageReadDateRead.CONSTRUCTOR -> {
                                val date = (readDate as MessageReadDateRead).readDate
                                messageOptionsBuilder.subtitle(readItem(tdlib!!, date))
                            }

                            TdApi.MessageReadDateUnread.CONSTRUCTOR, TdApi.MessageReadDateTooOld.CONSTRUCTOR, TdApi.MessageReadDateUserPrivacyRestricted.CONSTRUCTOR, TdApi.MessageReadDateMyPrivacyRestricted.CONSTRUCTOR -> {}
                            else -> {
                                assertMessageReadDate_5a6e5fbf()
                                throw unsupported(readDate)
                            }
                        }
                    }
                    val messageOptions = messageOptionsBuilder.build()
                    if (!messageContext.disableMetadata && msg.canBeReacted()) {
                        val popupLayout = showMessageOptions(messageOptions, messageContext.message, null, messageHandler)
                        if (expectAgain && popupLayout != null) {
                            inject.set(RunnableData { loadedReadDate: MessageReadDate? ->
                                if (!popupLayout.isDestroyed() && loadedReadDate!!.getConstructor() == MessageReadDateRead.CONSTRUCTOR) {
                                    val item: OptionItem = readItem(tdlib!!, (loadedReadDate as MessageReadDateRead).readDate)
                                    messageOptionsBuilder.subtitle(item)
                                    val c = (popupLayout.getBoundController() as MessageOptionsPagerController).findOptionsController()
                                    if (c != null) {
                                        c.updateSubtitle(messageOptionsBuilder.build())
                                    }
                                }
                            })
                        }
                    } else {
                        val popupLayout = showOptions(messageOptions, messageHandler)
                        patchReadReceiptsOptions(popupLayout!!, messageContext)
                        patchUsedEmojiPacks(popupLayout, messageContext)
                        if (expectAgain && popupLayout != null) {
                            inject.set(RunnableData { loadedReadDate: MessageReadDate? ->
                                if (!popupLayout.isDestroyed() && loadedReadDate!!.getConstructor() == MessageReadDateRead.CONSTRUCTOR) {
                                    val item: OptionItem = readItem(tdlib!!, (loadedReadDate as MessageReadDateRead).readDate)
                                    (popupLayout.getBoundView() as OptionsLayout).setSubtitle(item)
                                }
                            })
                        }
                    }
                }, 200L)
            })
        })
    }

    fun showMessageAddedReactions(message: TGMessage?, reactionType: ReactionType?) {
        showMessageOptions(null, message, reactionType, newMessageOptionDelegate(message, null, null))
    }

    private var isMessageOptionsVisible = false

    private fun showMessageOptions(options: Options?, message: TGMessage?, reactionType: ReactionType?, optionsDelegate: OptionDelegate?): PopupLayout? {
        if (isMessageOptionsVisible) {
            return null
        }
        isMessageOptionsVisible = true

        val r: MessageOptionsPagerController = object : MessageOptionsPagerController(context, tdlib, options, message, reactionType, optionsDelegate) {
            override fun onCustomShowComplete() {
                super.onCustomShowComplete()
                optimizeEmojiLayoutForOptionsWindow(true)
            }
        }
        val result = r.show()
        r.setDismissListener(object : PopupLayout.DismissListener {
            override fun onPopupDismiss(popup: PopupLayout?) {
                optimizeEmojiLayoutForOptionsWindow(false)
                isMessageOptionsVisible = false
            }

            override fun onPopupDismissPrepare(popup: PopupLayout?) {
                onHideMessageOptions()
            }
        })
        prepareToShowMessageOptions()
        hideCursorsForInputView()
        return result
    }

    private var needShowKeyboardAfterHideMessageOptions = false
    private var needShowEmojiKeyboardAfterHideMessageOptions = false

    private fun prepareToShowMessageOptions() {
        needShowKeyboardAfterHideMessageOptions = getKeyboardState()
        needShowEmojiKeyboardAfterHideMessageOptions = emojiShown
        if (needShowKeyboardAfterHideMessageOptions) {    // показываем emoji-клавиатуру, чтобы скрыть системную
            openEmojiKeyboard() // делаем emojiLayout невидимым для оптимизации
            emojiLayout!!.optimizeForDisplayMessageOptionsWindow(true)
        } // todo: если меню сообщения ниже EmojiLayout, то не скрывать?
    }

    private fun optimizeEmojiLayoutForOptionsWindow(needOptimize: Boolean) {
        if (needShowKeyboardAfterHideMessageOptions || needShowEmojiKeyboardAfterHideMessageOptions) {
            emojiLayout!!.optimizeForDisplayMessageOptionsWindow(needOptimize)
        }
    }

    private fun onHideMessageOptions() {
        if (needShowEmojiKeyboardAfterHideMessageOptions) {
            openEmojiKeyboard()
            emojiLayout!!.optimizeForDisplayMessageOptionsWindow(false)
        } else if (needShowKeyboardAfterHideMessageOptions) {
            showKeyboard()
        }
    }


    private fun patchUsedEmojiPacks(layout: PopupLayout, messageContext: MessageContext) {
        val message = messageContext.message
        val emojiPackIds = message.uniqueEmojiPackIdList
        if (emojiPackIds.size == 0) {
            return
        }

        val optionsLayout = layout.getChildAt(1) as OptionsLayout
        val emojiPacksInfoView = EmojiPacksInfoView(layout.getContext(), this, tdlib)
        emojiPacksInfoView.update(message.firstEmojiId, emojiPackIds, object : ClickableSpan() {
            override fun onClick(widget: View) {
                cancelSheduledKeyboardOpeningAndHideAllKeyboards()
                tdlib!!.ui().showStickerSets(this@MessagesController, emojiPackIds, true, null)
                layout.hideWindow(true)
            }
        }, false)
        optionsLayout.addView(emojiPacksInfoView, 1)
    }

    private fun patchReadReceiptsOptions(layout: PopupLayout, messageContext: MessageContext) {
        val message = messageContext.message
        if (!message.canGetViewers() || messageContext.disableMetadata || (message.isUnread && !message.noUnread()) || (layout.getChildAt(1) !is OptionsLayout)) {
            return
        }

        val optionsLayout = layout.getChildAt(1) as OptionsLayout

        val receiptWrap = LinearLayout(layout.getContext())
        receiptWrap.setOrientation(LinearLayout.HORIZONTAL)

        val frameLayout: FrameLayout = FrameLayoutFix(layout.getContext())
        frameLayout.setLayoutParams(LinearLayout.LayoutParams(0, Screen.dp(54f), 1f))
        val receiptText = OptionsLayout.genOptionView(
            layout.getContext(),
            R.id.more_btn_openReadReceipts,
            Lang.getString(R.string.LoadingMessageSeen),
            OptionColor.NORMAL,
            0,
            OptionColor.NORMAL,
            null,
            getThemeListeners(),
            null
        )

        val tav = TripleAvatarView(layout.getContext())

        receiptText.setLayoutParams(
            newParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_VERTICAL,
                Screen.dp(18f) + Screen.dp(24f), 0, 0, 0
            )
        )
        val iconView = ImageView(context)
        iconView.setScaleType(ImageView.ScaleType.CENTER)
        iconView.setLayoutParams(
            newParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_VERTICAL,
                Screen.dp(17f), 0, 0, 0
            )
        )
        iconView.setImageResource(R.drawable.baseline_visibility_24)
        iconView.setColorFilter(Theme.iconColor())
        addThemeFilterListener(iconView, ColorId.icon)
        frameLayout.addView(iconView)
        receiptText.setClickable(false)
        frameLayout.addView(receiptText)
        tav.setLayoutParams(LinearLayout.LayoutParams(Screen.dp((TripleAvatarView.AVATAR_SIZE * 3 + Screen.dp(6f)).toFloat()), Screen.dp(54f)))
        receiptWrap.addView(frameLayout)
        receiptWrap.addView(tav)

        Views.setClickable(receiptWrap)
        RippleSupport.setSimpleWhiteBackground(receiptWrap)

        optionsLayout.addView(receiptWrap, 2)

        val subtitleView = OptionsLayout.genOptionView(layout.getContext(), 0, null, OptionColor.NORMAL, 0, OptionColor.NORMAL, null, getThemeListeners(), null)

        val isSubtitleVisible = BoolAnimator(0, FactorAnimator.Target { id: Int, factor: Float, fraction: Float, callee: FactorAnimator? ->
            receiptText.setTranslationY(-Screen.dp(10f) * factor)
            subtitleView.setTranslationY(Screen.dp(10f) + Screen.dp(10f) * (1f - factor))
            subtitleView.setAlpha(factor)
        }, DECELERATE_INTERPOLATOR, 180L)

        val viewSubtitle = RunnableData { subtitleText: CharSequence? ->
            subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
            subtitleView.setTextColor(Theme.textDecentColor())
            subtitleView.setLayoutParams(
                newParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER_VERTICAL,
                    Screen.dp(18f) + Screen.dp(24f), 0, 0, 0
                )
            )
            subtitleView.setClickable(false)
            subtitleView.setText(subtitleText)
            subtitleView.setAlpha(0f)
            frameLayout.addView(subtitleView)
            isSubtitleVisible.setValue(true, true)
        }

        tdlib!!.client().send(GetMessageViewers(message.chatId, message.id), Client.ResultHandler { obj: TdApi.Object? ->
            if (obj!!.getConstructor() != MessageViewers.CONSTRUCTOR) return@ResultHandler
            runOnUiThreadOptional(Runnable {
                val viewers = obj as MessageViewers
                if (viewers.viewers.size > 1) {
                    receiptText.setText(MessageSeenController.getViewString(message, viewers.viewers.size).toString())
                } else if (viewers.viewers.size == 1) {
                    val viewer = viewers.viewers[0]
                    receiptText.setText(tdlib!!.senderName(MessageSenderUser(viewer.userId)))
                    if (viewer.viewDate != 0) {
                        val viewedText = TGUser.getActionDateStatus(tdlib, viewer.viewDate, message.message)
                        viewSubtitle.runWithData(viewedText)
                    }
                } else {
                    receiptText.setText(MessageSeenController.getNobodyString(message))
                }

                tav.setUsers(tdlib, viewers)
                receiptWrap.setOnClickListener(View.OnClickListener { v: View? ->
                    layout.hideWindow(true)
                    if (viewers.viewers.size > 1) {
                        ModernActionedLayout.showMessageSeen(this, message, viewers)
                    } else if (viewers.viewers.size == 1) {
                        tdlib!!.ui().openPrivateProfile(this, viewers.viewers[0].userId, UrlOpenParameters().tooltip(context().tooltipManager().builder(v)))
                    }
                })
            })
        })
    }

    fun onCommandLongPressed(command: InlineResultCommand): Boolean {
        if (!canWriteMessages() || actionShowing) {
            return false
        }

        val str: String?
        if (isPrivate(getChatId())) {
            str = command.getCommand() + " "
        } else {
            str = command.getCommand() + "@" + command.getUsername() + " "
        }
        inputView!!.setInput(str, true, true)
        Keyboard.show(inputView)

        return true
    }

    fun onCommandLongPressed(msg: TGMessage, command: String?): Boolean {
        if (!canWriteMessages()) {
            return false
        }
        val str: String?
        if (tdlib!!.isMultiChat(getChatId()) && msg.sender.isBot()) {
            str = command + '@' + msg.sender.getUsername() + ' '
        } else {
            str = command + ' '
        }
        inputView!!.setInput(str, true, true)
        Keyboard.show(inputView)
        return true
    }

    fun setInputInlineBot(userId: Long, username: String?) {
        if (canWriteMessages()) {
            // pressed via @NephoBot message
            inputView!!.setInput(username + " ", true, true)
            Keyboard.show(inputView)
        } else {
            tdlib!!.ui().openPrivateChat(this, userId, ChatOpenParameters().keepStack())
        }
    }

    // Message selection
    private var selectedMessageIds: LongSparseArray<TGMessage>? = null

    private fun selectedMessagesToArray(): Array<MessageWithProperties>? {
        if (selectedMessageIds == null) {
            return null
        }
        val size = selectedMessageIds!!.size()
        if (size == 0) {
            return null
        }
        @Suppress("UNCHECKED_CAST")
        val messages = arrayOfNulls<MessageWithProperties>(size) as Array<MessageWithProperties>
        for (i in 0..<size) {
            val messageId = selectedMessageIds!!.keyAt(i)
            val msg = selectedMessageIds!!.valueAt(i)
            val message = msg.getMessage(messageId)!!
            val properties = msg.lastMessageProperties(messageId)
            messages[i] = MessageWithProperties(message, properties)
        }
        return messages
    }

    private fun canSelectInBetween(): Boolean {
        if (selectedMessageIds == null || selectedMessageIds!!.size() != 2) {
            return false
        }

        val firstMessageId = selectedMessageIds!!.keyAt(0)
        val secondMessageId = selectedMessageIds!!.keyAt(1)

        val firstContainerIndex = manager!!.getAdapter().indexOfMessageContainer(firstMessageId)
        val secondContainerIndex = manager.getAdapter().indexOfMessageContainer(secondMessageId)
        if (firstContainerIndex == -1 || secondContainerIndex == -1) {
            return false
        }

        if (firstContainerIndex - secondContainerIndex > 1) {
            return true
        }

        val firstContainer = selectedMessageIds!!.valueAt(0)
        val secondContainer = selectedMessageIds!!.valueAt(1)

        return firstContainer.getMessageCountBetween(firstMessageId, secondMessageId) + secondContainer.getMessageCountBetween(
            firstMessageId,
            secondMessageId
        ) > 0
    }

    private fun selectMessagesInBetween() {
        if (selectedMessageIds == null || selectedMessageIds!!.size() != 2) {
            return
        }

        val firstMessageId = selectedMessageIds!!.keyAt(0)
        val secondMessageId = selectedMessageIds!!.keyAt(1)

        val firstContainerIndex = manager!!.getAdapter().indexOfMessageContainer(firstMessageId)
        val secondContainerIndex = manager.getAdapter().indexOfMessageContainer(secondMessageId)
        if (firstContainerIndex == -1 || secondContainerIndex == -1) {
            return
        }

        var selectedCount = 0
        val ids: LongSet = LongSet(MAX_MESSAGE_GROUP_SIZE)
        for (i in secondContainerIndex..firstContainerIndex) {
            val container = manager.getAdapter().getMessage(i)
            if (!container.canBeSelected()) {
                continue
            }
            container.getIds(ids, firstMessageId, secondMessageId)
            for (id in ids) {
                container.setSelected(id, true, true, -1f, -1f, null)
                putSelectedMessageId(id, container)
            }
            selectedCount += ids.size()
            ids.clear()
        }

        if (selectedCount > 0) {
            updateSelectButtons()
            setSelectedCount(selectedMessageIds!!.size())
        }
    }

    fun updateSelectButtons() {
        if (headerView != null) {
            var totalButtonsCount = 0
            var value: Boolean
            headerView!!.updateButton(
                R.id.menu_messageActions,
                R.id.menu_btn_selectInBetween,
                if (canSelectInBetween().also { value = it }) View.VISIBLE else View.GONE,
                0
            )
            if (value) totalButtonsCount++
            headerView!!.updateButton(
                R.id.menu_messageActions,
                R.id.menu_btn_reply,
                if (canReplyToSelectedMessages().also { value = it }) View.VISIBLE else View.GONE,
                0
            )
            if (value) totalButtonsCount++
            headerView!!.updateButton(
                R.id.menu_messageActions,
                R.id.menu_btn_forward,
                if (canShareSelectedMessages().also { value = it }) View.VISIBLE else View.GONE,
                0
            )
            if (value) totalButtonsCount++
            headerView!!.updateButton(
                R.id.menu_messageActions,
                R.id.menu_btn_edit,
                if (canEditSelectedMessages().also { value = it }) View.VISIBLE else View.GONE,
                0
            )
            if (value) totalButtonsCount++
            headerView!!.updateButton(
                R.id.menu_messageActions,
                R.id.menu_btn_copy,
                if (canCopySelectedMessages().also { value = it }) View.VISIBLE else View.GONE,
                0
            )
            if (value) totalButtonsCount++
            headerView!!.updateButton(
                R.id.menu_messageActions,
                R.id.menu_btn_retry,
                if (canResendSelectedMessages().also { value = it }) View.VISIBLE else View.GONE,
                0
            )
            if (value) totalButtonsCount++
            headerView!!.updateButton(
                R.id.menu_messageActions,
                R.id.menu_btn_delete,
                if (canDeleteSelectedMessages().also { value = it }) View.VISIBLE else View.GONE,
                0
            )
            if (value) totalButtonsCount++
            headerView!!.updateButton(
                R.id.menu_messageActions,
                R.id.menu_btn_view,
                if (canViewSelectedMessages().also { value = it }) View.VISIBLE else View.GONE,
                0
            )
            if (value) totalButtonsCount++
            headerView!!.updateButton(
                R.id.menu_messageActions,
                R.id.menu_btn_send,
                if (canSendSelectedMessages().also { value = it }) View.VISIBLE else View.GONE,
                0
            )
            if (value) totalButtonsCount++
            headerView!!.updateButton(
                R.id.menu_messageActions,
                R.id.menu_btn_clearCache,
                if (canClearCacheSelectedMessages().also { value = it }) View.VISIBLE else View.GONE,
                0
            )
            if (value) totalButtonsCount++
            headerView!!.updateButton(
                R.id.menu_messageActions,
                R.id.btn_unpinAll,
                if (canUnpinSelectedMessages().also { value = it }) View.VISIBLE else View.GONE,
                0
            )
            if (value) totalButtonsCount++
            headerView!!.updateButton(
                R.id.menu_messageActions,
                R.id.menu_btn_report,
                if (canReportSelectedMessages(totalButtonsCount)) View.VISIBLE else View.GONE,
                0
            )
        }
    }

    private fun copySelectedMessages() {
        if (selectedMessageIds == null || selectedMessageIds!!.size() == 0) {
            return
        }
        if (selectedMessageIds!!.size() == 1) {
            val message = this.singleSelectedMessage
            val formattedText = if (message != null) message.message.content.textOrCaption() else null
            if (formattedText != null) {
                UI.copyText(TD.toCharSequence(formattedText), R.string.CopiedText)
            }
            return
        }

        val b = SpannableStringBuilder()
        var first = true
        val size = selectedMessageIds!!.size()
        for (i in 0..<size) {
            val messageId = selectedMessageIds!!.keyAt(i)
            val m = selectedMessageIds!!.valueAt(i)
            val msg = m.getMessage(messageId)

            if (msg == null) {
                continue
            }

            if (first) {
                first = false
            } else {
                b.append("\n\n")
            }

            val author = tdlib!!.messageAuthor(msg)
            if (msg.viaBotUserId != 0L) {
                b.append(Lang.getString(R.string.message_nameViaBot, author, "@" + tdlib!!.cache().userUsername(msg.viaBotUserId)))
            } else {
                b.append(author)
            }
            b.append(", [")
            b.append(Lang.getTimestamp(msg.date.toLong(), TimeUnit.SECONDS))
            b.append("]")
            if (msg.isChannelPost) {
                if (!isEmpty(msg.authorSignature)) {
                    b.append("\n[")
                    b.append(Lang.getString(R.string.PostedBy, msg.authorSignature))
                    b.append("]")
                }
            }
            if (msg.replyTo != null) {
                val inReply = m.inReplyTo
                if (!isEmpty(inReply)) {
                    b.append("\n[")
                    b.append(Lang.getString(R.string.InReplyToX, inReply))
                    b.append("]")
                }
            }
            if (msg.forwardInfo != null) {
                b.append("\n[ ")
                b.append(Lang.getString(R.string.ForwardedFromX, m.sourceName))
                b.append(" ]")
            }
            val text = msg.content.textOrCaption()
            if (msg.content.getConstructor() != MessageText.CONSTRUCTOR && msg.content.getConstructor() != MessageAnimatedEmoji.CONSTRUCTOR) {
                b.append("\n[")
                val preview = ContentPreview.getChatListPreview(tdlib, msg.chatId, msg, true)
                b.append(preview.buildText(false))
                b.append("]")
            }
            if (!text.isEmpty()) {
                b.append('\n')
                b.append(TD.toCharSequence(text))
            }
        }

        UI.copyText(SpannableString.valueOf(b), R.string.CopiedMessages)
    }

    private fun canCopySelectedMessages(): Boolean {
        if (pagerScrollPosition != 0 && pagerContentAdapter != null) {
            val adapter = pagerContentAdapter!!
            val c = adapter.cachedItems.get(pagerScrollPosition)
            return c != null && c.canCopyMessages()
        }
        if (selectedMessageIds == null || selectedMessageIds!!.size() == 0) {
            return false
        }
        if (selectedMessageIds!!.size() == 1) {
            val m = this.singleSelectedMessage
            return m!!.message.canBeSaved && TD.canCopyText(m.message)
        }
        for (i in 0..<selectedMessageIds!!.size()) {
            val message = selectedMessageIds!!.valueAt(i).getMessage(selectedMessageIds!!.keyAt(i))!!
            if (!message.canBeSaved) {
                return false
            }
        }
        return !this.isSecretChat
    }

    private fun canDeleteSelectedMessages(): Boolean {
        if (pagerScrollPosition != 0 && pagerContentAdapter != null) {
            val adapter = pagerContentAdapter!!
            val c = adapter.cachedItems.get(pagerScrollPosition)
            return c != null && c.canDeleteMessages()
        }
        if (selectedMessageIds != null) {
            val size = selectedMessageIds!!.size()
            for (i in 0..<size) {
                val messageId = selectedMessageIds!!.keyAt(i)
                val m = selectedMessageIds!!.valueAt(i)
                val msg = m.getMessage(messageId)
                val properties = m.lastMessageProperties(messageId)
                if (msg == null || (!properties.canBeDeletedForAllUsers && !properties.canBeDeletedOnlyForSelf)) {
                    return false
                }
            }
            return size > 0
        }
        return false
    }

    private fun canResendSelectedMessages(): Boolean {
        if (chat != null && chat!!.canBeReported && selectedMessageIds != null && !this.isEventLog) {
            val size = selectedMessageIds!!.size()
            if (size > 1) {
                for (i in 0..<size) {
                    if (!selectedMessageIds!!.valueAt(i).canResend()) {
                        return false
                    }
                }
                return true
            }
        }
        return false
    }

    private fun canReportSelectedMessages(otherButtonsCount: Int): Boolean {
        if (chat != null && chat!!.canBeReported && selectedMessageIds != null && otherButtonsCount <= 3 && !this.isEventLog) {
            val size = selectedMessageIds!!.size()
            if (size > 1) {
                for (i in 0..<size) {
                    if (!selectedMessageIds!!.valueAt(i).canBeReported()) {
                        return false
                    }
                }
                return true
            }
        }
        return false
    }

    private fun canViewSelectedMessages(): Boolean {
        if (pagerScrollPosition != 0 && pagerContentAdapter != null) {
            val adapter = pagerContentAdapter!!
            val c = adapter.cachedItems.get(pagerScrollPosition)
            return c != null && c.selectedMediaCount == 1
        }
        return false
    }

    private fun canSendSelectedMessages(): Boolean {
        if (pagerScrollPosition == 0 && selectedMessageIds != null && selectedMessageIds!!.size() > 0) {
            for (i in 0..<selectedMessageIds!!.size()) {
                val msg = selectedMessageIds!!.valueAt(i)
                val message = msg.getMessage(selectedMessageIds!!.keyAt(i))
                if (message == null || message.schedulingState == null) return false
            }
            return true
        }
        return false
    }

    private fun canClearCacheSelectedMessages(): Boolean {
        if (pagerScrollPosition != 0 && pagerContentAdapter != null) {
            val adapter = pagerContentAdapter!!
            val c = adapter.cachedItems.get(pagerScrollPosition)
            return c != null && c.canClearMessages()
        }
        if (selectedMessageIds != null) {
            var hasMergedMessages = false
            var canClearCache = 0
            for (i in 0..<selectedMessageIds!!.size()) {
                val msg = selectedMessageIds!!.valueAt(i)
                val message = msg.getMessage(selectedMessageIds!!.keyAt(i))
                if (TD.canDeleteFiles(tdlib(), message)) {
                    canClearCache++
                    hasMergedMessages = hasMergedMessages || msg.combinedMessageCount > 0
                }
            }
            return canClearCache > 1 || hasMergedMessages
        }
        return false
    }

    private fun canUnpinSelectedMessages(): Boolean {
        return arePinnedMessages() && canPinAnyMessage(false)
    }

    private fun canReplyToSelectedMessages(): Boolean {
        val msg = this.singleSelectedMessage
        return pagerScrollPosition == 0 && msg != null && TD.canReplyTo(msg.message) && canWriteMessages()
    }

    private fun canEditSelectedMessages(): Boolean {
        val msg = this.singleSelectedMessage
        return !arePinnedMessages() && pagerScrollPosition == 0 && msg != null && msg.properties.canBeEdited && TD.canEditText(msg.message.content)
    }

    private fun canShareSelectedMessages(): Boolean {
        if (pagerScrollPosition != 0 && pagerContentAdapter != null) {
            val c = pagerContentAdapter!!.cachedItems.get(pagerScrollPosition)
            return c != null && c.canShareMessages()
        }
        if (selectedMessageIds != null) {
            val size = selectedMessageIds!!.size()
            for (i in 0..<size) {
                val messageId = selectedMessageIds!!.keyAt(i)
                val m = selectedMessageIds!!.valueAt(i)
                val msg = m.getMessage(messageId)
                if (msg == null || !m.lastMessageProperties(messageId).canBeForwarded) {
                    return false
                }
            }
            return size > 0
        }
        return false
    }

    fun unselectMessage(messageId: Long, msg: TGMessage?): Boolean {
        if (selectedMessageIds != null && msg != null && selectedMessageIds!!.get(messageId) != null) {
            selectMessage(messageId, msg, -1f, -1f, false, 0)
            updateSelectButtons()
            return true
        }
        return false
    }

    fun selectAllMessages(msg: TGMessage, pivotX: Float, pivotY: Float) {
        val ids = LongSet(msg.messageCount)
        msg.getIds(ids)
        val size = ids.size()
        val unselect = msg.isCompletelySelected
        var counterSet = false
        for (messageId in ids) {
            var ok = false
            if (unselect) {
                ok = selectedMessageIds != null && selectedMessageIds!!.get(messageId) != null
            } else {
                ok = selectedMessageIds == null || selectedMessageIds!!.get(messageId) == null
            }
            if (ok && selectMessage(messageId, msg, pivotX, pivotY, false, size)) {
                counterSet = true
            }
        }
        if (!counterSet) {
            val messageCount = this.selectedMessageCount
            if (messageCount == 0) {
                closeSelectMode()
                return
            }
            setSelectedCount(messageCount)
        }
        updateSelectButtons()
    }

    fun selectMessage(messageId: Long, msg: TGMessage, pivotX: Float, pivotY: Float) {
        selectMessage(messageId, msg, pivotX, pivotY, true, -1)
    }

    val selectedMessageCount: Int
        get() = if (selectedMessageIds != null) selectedMessageIds!!.size() else 0

    private fun selectMessage(messageId: Long, msg: TGMessage, pivotX: Float, pivotY: Float, needCounter: Boolean, openCount: Int): Boolean {
        if (inTransformMode() && !inSelectMode()) {
            if (inSearchMode()) {
                closeSearchMode(Runnable { selectMessage(messageId, msg, pivotX, pivotY, needCounter, openCount) })
            }
            return false
        }
        synchronized(this) {
            if (selectedMessageIds == null) {
                selectedMessageIds = LongSparseArray<TGMessage>(50)
            }
        }
        var counterSet = false
        if (!inSelectMode()) {
            msg.setSelected(messageId, true, true, pivotX, pivotY, this)
            putSelectedMessageId(messageId, msg)
            openSelectMode(if (needCounter) 1 else openCount)
            counterSet = true
        } else {
            val selected = selectedMessageIds!!.get(messageId)
            if (selected != null) {
                deleteSelectedMessageId(messageId)
                val finished = selectedMessageIds!!.size() == 0
                msg.setSelected(messageId, false, true, pivotX, pivotY, if (finished) this else null)
                if (finished) {
                    closeSelectMode()
                    return false
                }
            } else {
                if (selectedMessageIds!!.size() < tdlib!!.forwardMaxCount()) {
                    msg.setSelected(messageId, true, true, pivotX, pivotY, null)
                    putSelectedMessageId(messageId, msg)
                } else {
                    UI.showToast(Lang.plural(R.string.YouCantForwardMoreMessages, tdlib!!.forwardMaxCount().toLong()), Toast.LENGTH_SHORT)
                }
            }
            if (needCounter) {
                setSelectedCount(selectedMessageIds!!.size())
                counterSet = true
            }
        }
        if (needCounter) {
            updateSelectButtons()
        }
        return counterSet
    }

    override fun finishSelectMode(position: Int) {
        if ((position == -2 || position == -1)) {
            if (selectedMessageIds != null) {
                val size = selectedMessageIds!!.size()
                for (i in 0..<size) {
                    val messageId = selectedMessageIds!!.keyAt(i)
                    selectedMessageIds!!.valueAt(i).setSelected(messageId, false, position == -1, -1f, -1f, if (i == size - 1) this else null)
                }
                clearSelectedMessageIds()
            }
            if (pagerContentAdapter != null) {
                iterateMediaTabs(RunnableData { c: SharedBaseController<*>? -> c!!.setInMediaSelectMode(false) }
                )
            }
            if (position == -1) {
                closeSelectMode()
            }
        }
        if (position == -2) {
            leaveTransformMode()
        }
    }

    private var selectableAnimator: FactorAnimator? = null
    var selectableFactor: Float = 0f
        private set

    private fun resetSelectableControl() {
        selectableAnimator = null
        selectableFactor = 0f
    }

    override fun onSelectableModeChanged(isSelectable: Boolean, animator: FactorAnimator?) {
        selectableAnimator = animator
    }

    private fun putSelectedMessageId(messageId: Long, container: TGMessage?) {
        synchronized(this) {
            if (selectedMessageIds == null) {
                selectedMessageIds = LongSparseArray<TGMessage>()
            }
            selectedMessageIds!!.put(messageId, container!!)
        }
    }

    private fun deleteSelectedMessageId(messageId: Long) {
        synchronized(this) {
            if (selectedMessageIds != null) {
                selectedMessageIds!!.remove(messageId)
            }
        }
    }

    private fun clearSelectedMessageIds() {
        synchronized(this) {
            if (selectedMessageIds != null) {
                selectedMessageIds!!.clear()
            }
        }
    }

    fun isMessageSelected(chatId: Long, messageId: Long, container: TGMessage?): Boolean {
        synchronized(this) {
            if (selectedMessageIds != null && selectedMessageIds!!.size() > 0) {
                val i = selectedMessageIds!!.indexOfKey(messageId)
                if (i >= 0) {
                    val msg = selectedMessageIds!!.valueAt(i)
                    if (msg != null && msg.chatId == chatId) {
                        selectedMessageIds!!.setValueAt(i, container!!)
                        return true
                    }
                }
            }
        }
        return false
    }

    override fun onSelectableFactorChanged(factor: Float, callee: FactorAnimator?) {
        if (selectableAnimator == callee && selectableFactor != factor) {
            selectableFactor = factor
            val adapter = manager!!.getAdapter()
            val messageCount = adapter.getMessageCount()
            for (i in 0..<messageCount) {
                val msg = adapter.getMessage(i)
                if (msg != null) {
                    msg.onSelectableFactorChanged()
                }
            }
        }
    }

    fun canWriteMessages(): Boolean {
        return inputView != null && bottomWrap!!.getVisibility() == View.VISIBLE && inputView!!.getVisibility() == View.VISIBLE
    }

    fun canWriteMessagesOrWaitingForReply(): Boolean {
        return canWriteMessages() || (actionShowing && actionMode == ACTION_AWAITING_REPLY)
    }

    fun canPinAnyMessage(checkUi: Boolean): Boolean {
        return (if (checkUi) canWriteMessages() else hasWritePermission()) && chat != null && tdlib!!.canPinMessages(chat) && !areScheduled
    }

    val isSecretChat: Boolean
        get() = chat != null && isSecret(chat!!.id)

    fun inPreviewSearchMode(): Boolean {
        return inPreviewMode && previewMode == PREVIEW_MODE_SEARCH
    }

    val isSelfChat: Boolean
        get() = chat != null && tdlib!!.isSelfChat(chat!!.id)

    @Deprecated("")
    private fun hasWritePermission(): Boolean {
        // FIXME: this check is outdated and no longer correct
        return chat != null && tdlib!!.canSendBasicMessage(chat) && !this.isEventLog
    }

    fun canSendPhotosAndVideos(): Boolean { // FIXME separate photos and videos
        return tdlib!!.canSendMessage(chat, RightId.SEND_PHOTOS) &&
                tdlib!!.canSendMessage(chat, RightId.SEND_VIDEOS)
    }

    fun hasSendMessagePermission(@RightId rightId: Int): Boolean {
        return chat != null && tdlib!!.canSendMessage(chat, rightId) && !this.isEventLog
    }

    fun hasSendBasicMessagePermission(): Boolean {
        return chat != null && tdlib!!.canSendBasicMessage(chat) && !this.isEventLog
    }

    fun hasSendSomeMediaPermission(): Boolean {
        return chat != null && tdlib!!.canSendSendSomeMedia(chat) && !this.isEventLog
    }

    // test
    private fun newMessageOptionDelegate(context: MessageContext): OptionDelegate {
        return newMessageOptionDelegate(context.message, context.messageSender, context.tag)
    }

    private fun cancelSheduledKeyboardOpeningAndHideAllKeyboards() {
        if (needShowKeyboardAfterHideMessageOptions || needShowEmojiKeyboardAfterHideMessageOptions) {
            needShowEmojiKeyboardAfterHideMessageOptions = false
            needShowKeyboardAfterHideMessageOptions = false
            hideAllKeyboards()
        }
    }

    private fun newMessageOptionDelegate(selectedMessage: TGMessage?, selectedMessageSender: ChatMember?, selectedMessageTag: Any?): OptionDelegate {
        return OptionDelegate { itemView: View?, id: Int ->
            if (id == R.id.btn_cancel) {
                return@OptionDelegate true
            }
            if (selectedMessage == null) {
                return@OptionDelegate false
            }
            if (id == R.id.btn_emojiPackInfoButton) {
                cancelSheduledKeyboardOpeningAndHideAllKeyboards()
                tdlib!!.ui().showStickerSets(this@MessagesController, (itemView as EmojiPacksInfoView).getEmojiPacksIds(), true, null)
                return@OptionDelegate true
            } else if (id == R.id.btn_messageApplyLocalization) {
                if (selectedMessage.message.content.getConstructor() == MessageDocument.CONSTRUCTOR) {
                    val document = (selectedMessage.message.content as MessageDocument).document
                    tdlib!!.ui().readCustomLanguage(
                        this,
                        document,
                        RunnableData { langPack: CustomLangPackResult -> tdlib!!.ui().showLanguageInstallPrompt(this, langPack, selectedMessage.message) },
                        null
                    )
                }
                return@OptionDelegate true
            } else if (id == R.id.btn_messageInstallTheme) {
                if (selectedMessage.message.content.getConstructor() == MessageDocument.CONSTRUCTOR) {
                    val document = (selectedMessage.message.content as MessageDocument).document
                    tdlib!!.ui().readCustomTheme(this, document, null, null)
                }
                return@OptionDelegate true
            } else if (id == R.id.btn_messageSponsorInfo) {
                cancelSheduledKeyboardOpeningAndHideAllKeyboards()
                val mal = ModernActionedLayout(this)
                mal.setController(SponsoredMessagesInfoController(mal, R.string.SponsoredInfoMenu))
                mal.initCustom()
                mal.show()
                return@OptionDelegate true
            } else if (id == R.id.btn_messageCopyLink) {
                tdlib!!.getMessageLink(
                    selectedMessage.newestMessage,
                    selectedMessage.messageCount > 1,
                    messageThread != null,
                    RunnableData { link: Tdlib.MessageLink? ->
                        UI.copyText(
                            link!!.url,
                            if (link.isPublic) R.string.CopiedLink else R.string.CopiedLinkPrivate
                        )
                    })
                return@OptionDelegate true
            } else if (id == R.id.btn_messageRetractVote) {
                val message = selectedMessage.message
                tdlib!!.send<TdApi.Ok?>(SetPollAnswer(message.chatId, message.id, null), tdlib!!.typedOkHandler())
                return@OptionDelegate true
            } else if (id == R.id.btn_messagePollStop) {
                val message = selectedMessage.message
                val poll = (message.content as MessagePoll).poll
                val isQuiz = poll.type.getConstructor() == TdApi.PollTypeQuiz.CONSTRUCTOR
                showOptions(
                    Lang.getStringBold(if (isQuiz) R.string.StopQuizWarn else R.string.StopPollWarn, poll.question),
                    intArrayOf(R.id.btn_done, R.id.btn_cancel),
                    arrayOf<String?>(
                        Lang.getString(if (isQuiz) R.string.StopQuiz else R.string.StopPoll), Lang.getString(R.string.Cancel)
                    ),
                    intArrayOf(OptionColor.RED, OptionColor.NORMAL),
                    intArrayOf(R.drawable.baseline_poll_24, R.drawable.baseline_cancel_24),
                    OptionDelegate { optionItemView: View?, optionId: Int ->
                        if (optionId == R.id.btn_done) {
                            tdlib!!.send<TdApi.Ok?>(StopPoll(message.chatId, message.id, message.replyMarkup), tdlib!!.typedOkHandler())
                        }
                        true
                    })
                return@OptionDelegate true
            } else if (id == R.id.btn_messageLiveStop) {
                (selectedMessage as TGMessageLocation).stopLiveLocation()
                return@OptionDelegate true
            } else if (id == R.id.btn_messageAddContact) {
                val contact = (selectedMessage.message.content as MessageContact).contact
                tdlib!!.ui().addContact(this, contact)
                return@OptionDelegate true
            } else if (id == R.id.btn_messageCallContact) {
                cancelSheduledKeyboardOpeningAndHideAllKeyboards()
                val contact = (selectedMessage.message.content as MessageContact).contact
                showCallOptions(contact.phoneNumber, contact.userId)
                return@OptionDelegate true
            } else if (id == R.id.btn_messageResend) {
                tdlib!!.resendMessages(selectedMessage.chatId, selectedMessage.ids)
                return@OptionDelegate true
            } else if (id == R.id.btn_messageSendNow) {
                cancelSheduledKeyboardOpeningAndHideAllKeyboards()
                if (!showRestriction(null, tdlib!!.getSlowModeRestrictionText(getChatId()))) {
                    tdlib!!.send<TdApi.Ok?>(EditMessageSchedulingState(getChatId(), selectedMessage.id, null), tdlib!!.typedOkHandler())
                }
                return@OptionDelegate true
            } else if (id == R.id.btn_messageReschedule) {
                cancelSheduledKeyboardOpeningAndHideAllKeyboards()
                tdlib!!.ui().showScheduleOptions(this, getChatId(), false, SimpleSendCallback { sendOptions: MessageSendOptions?, disableMarkdown: Boolean ->
                    if (sendOptions!!.schedulingState != null) {
                        tdlib!!.send<TdApi.Ok?>(
                            EditMessageSchedulingState(getChatId(), selectedMessage.id, sendOptions.schedulingState),
                            tdlib!!.typedOkHandler()
                        )
                    }
                }, null, null)
                return@OptionDelegate true
            } else if (id == R.id.btn_messageShowSource) {
                selectedMessage.openSourceMessage()
                return@OptionDelegate true
            } else if (id == R.id.btn_messageShowInChat) {
                val chatId = selectedMessage.chatId
                val messageId = selectedMessage.smallestId
                val otherMessageIds = selectedMessage.getOtherMessageIds(messageId)
                tdlib!!.ui().openMessage(this, chatId, MessageId(chatId, messageId, otherMessageIds), selectedMessage.openParameters())
                return@OptionDelegate true
            } else if (id == R.id.btn_messageShowInChatSearch) {
                cancelSheduledKeyboardOpeningAndHideAllKeyboards()
                if (inOnlyFoundMode()) {
                    val chatId = selectedMessage.chatId
                    val messageId = selectedMessage.smallestId
                    val otherMessageIds = selectedMessage.getOtherMessageIds(messageId)
                    manager!!.setHighlightMessageId(MessageId(chatId, messageId), MessagesManager.HIGHLIGHT_MODE_NORMAL)
                    onSetSearchFilteredShowMode(false)
                }
                return@OptionDelegate true
            } else if (id == R.id.btn_messageDirections) {
                cancelSheduledKeyboardOpeningAndHideAllKeyboards()
                val content = selectedMessage.message.content
                when (content.getConstructor()) {
                    MessageVenue.CONSTRUCTOR -> {
                        val venue = (content as MessageVenue).venue
                        Intents.openDirections(venue.location.latitude, venue.location.longitude, venue.title, venue.address)
                    }

                    MessageLocation.CONSTRUCTOR -> {
                        val location = (content as MessageLocation).location
                        Intents.openDirections(location.latitude, location.longitude, null, null)
                    }
                }
                return@OptionDelegate true
            } else if (id == R.id.btn_messageFoursquare) {
                cancelSheduledKeyboardOpeningAndHideAllKeyboards()
                val venueId = (selectedMessage.message.content as MessageVenue).venue.id
                tdlib!!.ui().openUrl(this, "https://foursquare.com/v/" + venueId, selectedMessage.openParameters())
                return@OptionDelegate true
            } else if (id == R.id.btn_messageCall) {
                cancelSheduledKeyboardOpeningAndHideAllKeyboards()
                tdlib!!.context().calls().makeCall(this, tdlib!!.calleeUserId(selectedMessage.message), null)
                return@OptionDelegate true
            } else if (id == R.id.btn_messageShareCallLogs) {
                val logFiles = selectedMessageTag as VoIPLogs.Pair?
                tdlib!!.ui().shareCallLogs(this, logFiles, true)
            } else if (id == R.id.btn_messageDelete) {
                cancelSheduledKeyboardOpeningAndHideAllKeyboards()
                tdlib!!.ui().showDeleteOptions(this, selectedMessage.allMessagesAndProperties, null)
                return@OptionDelegate true
            } else if (id == R.id.btn_messageReport) {
                cancelSheduledKeyboardOpeningAndHideAllKeyboards()
                if (selectedMessage.isSponsoredMessage()) {
                    this.reportChatSponsoredMessage<Arguments?>(tdlib!!, getChatId(), selectedMessage.sponsoredMessage!!)
                } else {
                    reportChat(selectedMessage.allMessagesAndProperties, null)
                }
                return@OptionDelegate true
            } else if (id == R.id.btn_messageSelect) {
                selectAllMessages(selectedMessage, -1f, -1f)
                return@OptionDelegate true
            } else if (id == R.id.btn_messageViewList) { //FIXME?
                cancelSheduledKeyboardOpeningAndHideAllKeyboards()
                foundMessageId = MessageId(selectedMessage.message.chatId, selectedMessage.message.id)
                searchFromUserMessageId = foundMessageId
                manager!!.setHighlightMessageId(foundMessageId, MessagesManager.HIGHLIGHT_MODE_NORMAL)
                viewMessagesFromSender(selectedMessage.message.senderId, true)
                return@OptionDelegate true
            } else if (id == R.id.btn_messageRestrictMember) {
                cancelSheduledKeyboardOpeningAndHideAllKeyboards()
                val c = EditRightsController(context, tdlib)
                c.setArguments(
                    EditRightsController.Args(
                        selectedMessage.chatId,
                        selectedMessage.message.senderId,
                        true,
                        tdlib!!.chatStatus(selectedMessage.chatId)!!,
                        selectedMessageSender
                    )
                )
                navigateTo(c)
                return@OptionDelegate true
            } else if (id == R.id.btn_messageBlockUser) {
                if (selectedMessageSender != null) {
                    tdlib!!.ui().kickMember(this, selectedMessage.chatId, selectedMessage.message.senderId, selectedMessageSender.status)
                }
            } else if (id == R.id.btn_messageUnblockMember) {
                if (selectedMessageSender != null) {
                    tdlib!!.ui().unblockMember(this, selectedMessage.chatId, selectedMessage.message.senderId, selectedMessageSender.status)
                }
                return@OptionDelegate true
            } else if (id == R.id.btn_messageMore) {
                cancelSheduledKeyboardOpeningAndHideAllKeyboards()
                val ids = IntList(3)
                val icons = IntList(3)
                val strings = StringList(3)
                val chatId = selectedMessage.chatId
                if (isMultiChat(chatId) && !tdlib!!.isChannel(chatId) && TD.isAdmin(tdlib!!.chatStatus(chatId)) && selectedMessage.message.senderId.getSenderId() != chatId) {
                    val senderId = selectedMessage.message.senderId
                    tdlib!!.send<ChatMember?>(GetChatMember(chatId, senderId), Tdlib.ResultHandler { otherMember: ChatMember?, error: TdApi.Error? ->
                        runOnUiThreadOptional(
                            Runnable {
                                if (!selectedMessage.isDestroyed) {
                                    val tag = MessageView.fillMessageOptions(this, selectedMessage, otherMember, ids, icons, strings, true)
                                    if (!ids.isEmpty()) {
                                        showMessageOptions(selectedMessage, ids.get(), strings.get(), icons.get(), tag, otherMember, true)
                                    }
                                }
                            })
                    })
                } else {
                    val tag = MessageView.fillMessageOptions(this, selectedMessage, selectedMessageSender, ids, icons, strings, true)
                    if (!ids.isEmpty()) {
                        showMessageOptions(selectedMessage, ids.get(), strings.get(), icons.get(), tag, selectedMessageSender, true)
                    }
                }
                return@OptionDelegate true
            } else if (id == R.id.btn_messageStickerSet) {
                cancelSheduledKeyboardOpeningAndHideAllKeyboards()
                (selectedMessage as TGMessageSticker).openStickerSet()
            } else if (id == R.id.btn_messageFavoriteContent || id == R.id.btn_messageUnfavoriteContent) {
                val isFavorite = id == R.id.btn_messageFavoriteContent
                val fileId = (selectedMessage.message.content as MessageSticker).sticker.sticker.id
                val inputFile: TdApi.InputFile = InputFileId(fileId)
                tdlib!!.send<TdApi.Ok?>(if (isFavorite) AddFavoriteSticker(inputFile) else RemoveFavoriteSticker(inputFile), tdlib!!.typedOkHandler())
            } else if (id == R.id.btn_messageUnpin || id == R.id.btn_messagePin) {
                pinUnpinMessage(selectedMessage, id == R.id.btn_messagePin)
                return@OptionDelegate true
            } else if (id == R.id.btn_messageReply) {
                val message = selectedMessage.newestMessage
                selectedMessage.getMessageProperties(message.id, RunnableData { properties: MessageProperties? ->
                    runOnUiThreadOptional(Runnable {
                        if (properties != null) {
                            showReply(MessageWithProperties(message, properties), null, 0, "", true, true)
                            if (inputView!!.isEmpty()) {
                                Keyboard.show(inputView)
                            }
                        }
                    })
                })
                return@OptionDelegate true
            } else if (id == R.id.btn_messageReplies) {
                cancelSheduledKeyboardOpeningAndHideAllKeyboards()
                selectedMessage.openMessageThread()
                return@OptionDelegate true
            } else if (id == R.id.btn_messageReplyWithDice) {
                sendDice(itemView, (selectedMessage.message.content as MessageDice).emoji)
                return@OptionDelegate true
            } else if (id == R.id.btn_copyTranslation || id == R.id.btn_messageCopy) {
                if (!selectedMessage.canBeSaved()) {
                    context().tooltipManager().builder(itemView).show(tdlib, R.string.ChannelNoCopy).hideDelayed()
                    return@OptionDelegate false
                }
                var message: TdApi.Message? = null
                if (selectedMessage is TGMessageMedia) {
                    val messageId = selectedMessage.getCaptionMessageId()
                    message = selectedMessage.getMessage(messageId)
                }
                if (message == null) {
                    message = selectedMessage.newestMessage
                }
                val text = if (id == R.id.btn_copyTranslation) selectedMessage.translatedText else message!!.content.textOrCaption()
                if (text != null) UI.copyText(TD.toCopyText(text), R.string.CopiedText)
                return@OptionDelegate true
            } else if (id == R.id.btn_messageEdit) {
                var message: TdApi.Message? = null
                if (selectedMessage is TGMessageMedia) {
                    val messageId = selectedMessage.getCaptionMessageId()
                    message = selectedMessage.getMessage(messageId)
                }
                if (message == null) {
                    message = selectedMessage.newestMessage
                }
                val editingMessage = message
                val properties = selectedMessage.lastMessageProperties(editingMessage!!.id)
                editMessage(MessageWithProperties(editingMessage, properties))
                return@OptionDelegate true
            } else if (id == R.id.btn_messageShare) {
                cancelSheduledKeyboardOpeningAndHideAllKeyboards()
                if (selectedMessage.canBeForwarded()) {
                    shareMessages(selectedMessage.allMessages, false)
                }
                return@OptionDelegate true
            } else if (id == R.id.btn_chatTranslate) {
                cancelSheduledKeyboardOpeningAndHideAllKeyboards()
                startTranslateMessages(selectedMessage)
                return@OptionDelegate true
            } else if (id == R.id.btn_chatTranslateOff) {
                stopTranslateMessages(selectedMessage)
                return@OptionDelegate true
            } else if (id == R.id.btn_saveGif) {
                if (selectedMessageTag != null) {
                    if (!selectedMessage.canBeSaved()) {
                        context().tooltipManager().builder(itemView).show(tdlib, R.string.ChannelNoSave).hideDelayed()
                        return@OptionDelegate false
                    }
                    @Suppress("UNCHECKED_CAST")
                    tdlib!!.ui().saveGifs(selectedMessageTag as MutableList<TD.DownloadedFile>)
                }
                return@OptionDelegate true
            } else if (id == R.id.btn_saveFile) {
                if (selectedMessageTag != null) {
                    if (!selectedMessage.canBeSaved()) {
                        context().tooltipManager().builder(itemView).show(tdlib, R.string.ChannelNoSave).hideDelayed()
                        return@OptionDelegate false
                    }
                    TD.saveFiles(context, selectedMessageTag as MutableList<TD.DownloadedFile?>)
                }
                return@OptionDelegate true
            } else if (id == R.id.btn_openIn) {
                if (selectedMessageTag != null) {
                    val document = (selectedMessage.message.content as MessageDocument).document
                    U.openFile(this, U.getFileName(document.document.local.path), File(document.document.local.path), document.mimeType, 0)
                }
                return@OptionDelegate true
            } else if (id == R.id.btn_addToPlaylist) {
                TdlibManager.instance().player().addToPlayList(selectedMessage.message)
                return@OptionDelegate true
            } else if (id == R.id.btn_downloadFile) {
                if (!selectedMessage.canBeSaved()) {
                    context().tooltipManager().builder(itemView).show(tdlib, R.string.ChannelNoSave).hideDelayed()
                    return@OptionDelegate false
                }
                val file = TD.getFile(selectedMessage)
                if (file != null && !file.local.isDownloadingActive && !file.local.isDownloadingCompleted) {
                    tdlib!!.files().downloadFile(file)
                }
                return@OptionDelegate true
            } else if (id == R.id.btn_pauseFile) {
                val file = TD.getFile(selectedMessage)
                if (file != null && file.local.isDownloadingActive && !tdlib!!.context().player().isPlayingFileId(file.id)) {
                    tdlib!!.files().cancelDownloadOrUploadFile(file.id, false, true)
                }
                return@OptionDelegate true
            } else if (id == R.id.btn_viewStatistics) {
                cancelSheduledKeyboardOpeningAndHideAllKeyboards()
                val messages = selectedMessage.allMessages
                val statsController = MessageStatisticsController(context, tdlib)
                if (messages.size == 1) {
                    statsController.setArguments(MessageStatisticsController.Args(messages[0]!!.chatId, messages[0]))
                } else {
                    statsController.setArguments(MessageStatisticsController.Args(messages[0]!!.chatId, Arrays.asList<TdApi.Message?>(*messages)))
                }
                navigateTo(statsController)

                return@OptionDelegate true
            } else if (id == R.id.btn_deleteFile) {
                if (selectedMessageTag != null) {
                    TD.deleteFiles(this, selectedMessageTag as MutableList<TD.DownloadedFile?>, null)
                } else {
                    val messages = selectedMessage.allMessages
                    val files = SparseArrayCompat<TdApi.File?>(messages.size)
                    for (message in messages) {
                        val filesList = TD.getFiles(message)
                        if (filesList != null) {
                            for (file in filesList) {
                                if (TD.canDeleteFile(message, file)) {
                                    files.put(file.id, file)
                                }
                            }
                        }
                    }
                    if (!files.isEmpty()) {
                        TD.deleteFiles(this, asArray<TdApi.File?>(files, arrayOfNulls<TdApi.File>(files.size())), null)
                    }
                }
                return@OptionDelegate true
            } else if (id == R.id.btn_stickerSetInfo) {
                cancelSheduledKeyboardOpeningAndHideAllKeyboards()
                val content = selectedMessage.message.content
                if (content.getConstructor() == MessageSticker.CONSTRUCTOR) {
                    val sticker = content as MessageSticker
                    tdlib!!.ui().showStickerSet(this, sticker.sticker.setId, null)
                }
                return@OptionDelegate true
            }
            true
        }
    }

    // Action button
    private var bottomBar: ChatBottomBarView? = null

    private var bottomButtonAction = 0
    private var needBigPadding = false

    private fun checkExtraPadding() {
        val needBigPadding = bottomBarVisible.getValue() //  && !scrollToBottomVisible;
        if (this.needBigPadding != needBigPadding) {
            this.needBigPadding = needBigPadding
            manager!!.rebuildLastItem()
        }
    }

    fun needExtraBigPadding(): Boolean {
        return needBigPadding && !inWallpaperPreviewMode()
    }

    private fun showBottomButton(bottomButtonAction: Int, bottomButtonData: Long, animated: Boolean) {
        this.bottomButtonAction = bottomButtonAction
        val animateButtonContent = animated && bottomBarVisible.getFloatValue() > 0f
        when (bottomButtonAction) {
            BOTTOM_ACTION_FOLLOW -> {
                bottomBar!!.setAction(R.id.btn_follow, Lang.getString(R.string.Follow), R.drawable.baseline_group_add_24, animateButtonContent)
                bottomBar!!.clearPreviewChat()
            }

            BOTTOM_ACTION_DISCUSS -> {
                bottomBar!!.setAction(R.id.btn_openLinkedChat, Lang.getString(R.string.Discuss), R.drawable.baseline_chat_bubble_24, animateButtonContent)
                bottomBar!!.setPreviewChatId(null, bottomButtonData, null)
            }

            BOTTOM_ACTION_TOGGLE_MUTE -> {
                val notificationsEnabled = tdlib!!.chatNotificationsEnabled(getChatId())
                bottomBar!!.setAction(
                    R.id.btn_mute,
                    Lang.getString(if (notificationsEnabled) R.string.Mute else R.string.Unmute),
                    if (notificationsEnabled) R.drawable.baseline_notifications_off_24 else R.drawable.baseline_notifications_active_24,
                    animateButtonContent
                )
                bottomBar!!.clearPreviewChat()
            }

            BOTTOM_ACTION_UNPIN_ALL -> {
                bottomBar!!.setAction(
                    R.id.btn_unpinAll,
                    Lang.getString(if (canPinAnyMessage(false)) R.string.UnpinAll else R.string.DismissAllPinned),
                    R.drawable.deproko_baseline_pin_undo_24,
                    animated
                )
                bottomBar!!.clearPreviewChat()
            }

            BOTTOM_ACTION_TEST -> {
                bottomBar!!.setAction(R.id.btn_test, "test", R.drawable.baseline_warning_24, animateButtonContent)
                bottomBar!!.clearPreviewChat()
            }

            BOTTOM_ACTION_APPLY_WALLPAPER -> {
                bottomBar!!.setAction(
                    R.id.btn_applyWallpaper,
                    Lang.getString(R.string.ChatBackgroundApply),
                    R.drawable.baseline_warning_24,
                    animateButtonContent
                )
                bottomBar!!.clearPreviewChat()
            }
        }
        bottomBarVisible.setValue(true, animated)
        checkExtraPadding()
    }

    private fun hideBottomBar(animated: Boolean) {
        bottomBarVisible.setValue(false, animated)
        checkExtraPadding()
    }

    private fun updateBottomBarStyle() {
        if (bottomBar == null) return
        val bottomButtonFactor = bottomBarVisible.getFloatValue()
        val detachFactor = scrollToBottomVisible.getFloatValue()
        val barHeight = Screen.dp(48f) + extraBottomInset
        val baseY = if (needSearchControlsTranslate()) (barHeight.toFloat() * clamp(searchControlsFactor)).toInt() else 0
        val fromY = (if (bottomButtonFactor == 1f) baseY else baseY + (barHeight.toFloat() * (1f - bottomButtonFactor)).toInt()).toFloat()
        val alpha = (1f - 1f * detachFactor * (1f - bottomButtonFactor)) * (1f - searchControlsFactor)
        val moveBy = Screen.dp(74f) - Screen.dp(16f)
        val toY =
            -this.buttonsOffset - Screen.dp(16f) - moveBy - moveBy * mentionButtonFactor + (if (bottomWrap!!.getVisibility() == View.VISIBLE) 0 else extraBottomInsetWithoutIme) //  -getReplyOffset() - (Screen.dp(74f) - Screen.dp(48f)) / 2f;
        bottomBar!!.setCollapseFactor(detachFactor)
        bottomBar!!.setAlpha(alpha)
        val dx = ((bottomBar!!.getMeasuredWidth() / 2f - Screen.dp(16f) - Screen.dp(48f) / 2f) * detachFactor).toInt()
        bottomBar!!.setTranslationY(if (bottomButtonFactor == 1f && detachFactor == 0f) fromY else fromY + (toY - fromY) * detachFactor)
        bottomBar!!.setTranslationX(dx.toFloat())
        val desiredVisibility = if (bottomButtonFactor > 0f && searchControlsFactor != 1f) View.VISIBLE else View.GONE
        if (bottomBar!!.getVisibility() != desiredVisibility) {
            bottomBar!!.setVisibility(desiredVisibility)
        }
    }

    private val bottomBarVisible = BoolAnimator(ANIMATOR_BOTTOM_BUTTON, this, DECELERATE_INTERPOLATOR, 180L)

    val bottomOffset: Int
        get() = (this.replyOffset + this.attachedFilesOffset).toInt()

    private var actionButtonWrap: FrameLayoutFix? = null
    private var actionButton: TextView? = null
    private var actionShowing = false
    private var actionMode = 0

    fun showActionButton(resource: Int, mode: Int) {
        showActionButton(Lang.getString(resource).uppercase(Locale.getDefault()), mode, true)
    }

    fun showActionButton(string: String?, mode: Int) {
        showActionButton(string, mode, true)
    }

    private var botStartArgument: String? = null

    @JvmOverloads
    fun showActionBotButton(argument: String? = botStartArgument): Boolean {
        if (tdlib!!.isBotChat(getChatId())) {
            this.botStartArgument = argument
            showActionButton(R.string.BotStart, ACTION_BOT_START)
            return true
        }
        return false
    }

    fun showActionDeleteChatButton() {
        if (isBasicGroup(getChatId()) && chat!!.lastMessage != null && chat!!.lastMessage!!.content.getConstructor() == MessageChatUpgradeTo.CONSTRUCTOR) {
            showActionButton(Lang.getString(R.string.OpenSupergroup).uppercase(Locale.getDefault()), ACTION_OPEN_SUPERGROUP)
        } else {
            showActionButton(R.string.DeleteChat, ACTION_DELETE_CHAT)
        }
    }

    fun showActionJoinChatButton() {
        val supergroup = tdlib!!.chatToSupergroup(getChatId())
        if (supergroup != null && supergroup.joinByRequest && !TD.isAdmin(supergroup.status)) {
            showActionButton(if (supergroup.isChannel) R.string.RequestJoinChannel else R.string.RequestJoinGroup, ACTION_JOIN_CHAT)
        } else {
            showActionButton(R.string.JoinChat, ACTION_JOIN_CHAT)
        }
    }

    fun showActionUnblockButton() {
        val userId = tdlib!!.chatUserId(getChatId())
        if (!tdlib!!.isRepliesChat(chat!!.id) && tdlib!!.isBotChat(chat!!)) {
            showActionButton(R.string.RestartBot, ACTION_BOT_START)
        } else {
            showActionButton(R.string.Unblock, ACTION_UNBAN_USER)
        }
    }

    fun showSecretChatAction(secretChat: SecretChat) {
        when (secretChat.state.getConstructor()) {
            TdApi.SecretChatStatePending.CONSTRUCTOR -> {
                showActionButton(Lang.getString(R.string.AwaitingEncryption, tdlib!!.cache().userFirstName(secretChat.userId)), ACTION_EMPTY, false)
            }

            TdApi.SecretChatStateClosed.CONSTRUCTOR -> {
                showActionButton(Lang.getString(R.string.SecretChatCancelled), ACTION_EMPTY, false)
            }
        }
    }

    private fun updateActionButton(text: CharSequence?, mode: Int, isActive: Boolean) {
        actionMode = mode
        actionButton!!.setEnabled(isActive)
        actionButton!!.setTextColor(if (isActive) Theme.textLinkColor() else Theme.textAccentColor())
        removeThemeListenerByTarget(actionButton)
        addThemeTextColorListener(actionButton, if (isActive) ColorId.textLink else ColorId.text)
        Views.setMediumText(actionButton, text)
    }

    fun showActionButton(text: CharSequence?, mode: Int, isActive: Boolean) {
        if (actionShowing) {
            hideSoftwareKeyboard()
            updateActionButton(text, mode, isActive)
            return
        }

        val initialized: Boolean

        if (actionButtonWrap == null) {
            actionButtonWrap = FrameLayoutFix(context())
            actionButtonWrap!!.setLayoutParams(LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Screen.dp(49f)))

            actionButton = NoScrollTextView(context())
            actionButton!!.setId(R.id.btn_chatAction)
            actionButton!!.setOnClickListener(this)
            actionButton!!.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
            actionButton!!.setTypeface(Fonts.getRobotoMedium())
            actionButton!!.setPadding(Screen.dp(12f), 0, Screen.dp(12f), 0)
            // actionButton.setSingleLine(false);
            RippleSupport.setSimpleWhiteBackground(actionButton!!, this)
            actionButton!!.setEllipsize(TextUtils.TruncateAt.END)
            actionButton!!.setGravity(Gravity.CENTER)
            actionButton!!.setLayoutParams(newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

            actionButtonWrap!!.addView(actionButton)

            if (mode == ACTION_EVENT_LOG_SETTINGS) {
                val imageView = ImageView(context())
                imageView.setOnClickListener(this)
                imageView.setScaleType(ImageView.ScaleType.CENTER)
                imageView.setImageResource(R.drawable.baseline_help_outline_24)
                imageView.setColorFilter(Theme.getColor(ColorId.textNeutral))
                addThemeFilterListener(imageView, ColorId.textNeutral)
                imageView.setLayoutParams(newParams(Screen.dp(49f), Screen.dp(49f), Gravity.RIGHT or Gravity.CENTER_VERTICAL))
                imageView.setId(R.id.btn_help)
                actionButtonWrap!!.addView(imageView)
            }

            initialized = true
        } else {
            initialized = false
        }

        actionShowing = true

        updateActionButton(text, mode, isActive)

        closeCommandsKeyboard(false)
        closeEmojiKeyboard()

        if (inputView != null) {
            inputView!!.setVisibility(View.GONE)
            inputView!!.setInput("", false, false)
        }

        hideAttachButtons()
        hideSendButton()
        hideEmojiButton()

        if (initialized) {
            bottomWrap!!.addView(actionButtonWrap, min(bottomWrap!!.getChildCount(), 1))
        } else {
            actionButtonWrap!!.setVisibility(View.VISIBLE)
        }

        hideSoftwareKeyboard()
    }

    fun hideActionButton() {
        if (!actionShowing) {
            return
        }

        actionButtonWrap!!.setVisibility(View.GONE)
        if (inputView != null) {
            inputView!!.setVisibility(View.VISIBLE)
            checkSendButton(false)
        }

        displayEmojiButton()

        actionShowing = false
    }

    private fun processChatAction() {
        when (actionMode) {
            ACTION_OPEN_SUPERGROUP -> {
                if (chat != null && chat!!.lastMessage != null && chat!!.lastMessage!!.content.getConstructor() == MessageChatUpgradeTo.CONSTRUCTOR) {
                    val to = chat!!.lastMessage!!.content as MessageChatUpgradeTo
                    tdlib!!.ui().openSupergroupChat(this, to.supergroupId, null)
                }
            }

            ACTION_DELETE_CHAT -> {
                hideSoftwareKeyboard()
                tdlib!!.ui().showDeleteChatConfirm(this, getChatId())
            }

            ACTION_BOT_START -> {
                val chatId = getChatId()
                val after = Runnable {
                    if (!isDestroyed() && getChatId() == chatId) {
                        tdlib!!.sendBotStartMessage(tdlib!!.chatUserId(chatId), chat!!.id, botStartArgument)
                        hideActionButton()
                    }
                }
                if (tdlib!!.chatFullyBlocked(chat!!.id)) {
                    tdlib!!.unblockSender(tdlib!!.sender(chat!!.id), Client.ResultHandler { result: TdApi.Object? ->
                        if (TD.isOk(result)) {
                            tdlib!!.ui().postDelayed(after, 200)
                        } else {
                            tdlib!!.okHandler().onResult(result)
                        }
                    })
                } else {
                    after.run()
                }
            }

            ACTION_JOIN_CHAT -> {
                tdlib!!.client().send(AddChatMember(chat!!.id, tdlib!!.myUserId(), 0), Client.ResultHandler { result: TdApi.Object? ->
                    if (result!!.getConstructor() == TdApi.Error.CONSTRUCTOR) {
                        runOnUiThreadOptional(Runnable {
                            if (isFocused()) {
                                context
                                    .tooltipManager()
                                    .builder(actionButton)
                                    .show(this, tdlib, R.drawable.baseline_error_24, TD.toErrorString(result))
                            }
                        })
                    }
                })
            }

            ACTION_UNBAN_USER -> {
                tdlib!!.unblockSender(tdlib!!.sender(chat!!.id), tdlib!!.okHandler())
            }

            ACTION_EMPTY, ACTION_AWAITING_REPLY -> {}
            ACTION_EVENT_LOG_SETTINGS -> {
                openEventLogSettings()
            }
        }
    }

    // Scroll button
    fun setScrollToBottomVisible(isVisible: Boolean, isReverse: Boolean) {
        setScrollToBottomVisible(isVisible, isReverse, isFocused() || getParentOrSelf().isFocused())
    }

    private val scrollToBottomVisible = BoolAnimator(ANIMATOR_SCROLL_TO_BOTTOM, this, DECELERATE_INTERPOLATOR, 180L)
    private val scrollToBottomReverse = BoolAnimator(0, object : FactorAnimator.Target {
        override fun onFactorChanged(id: Int, factor: Float, fraction: Float, callee: FactorAnimator?) {
            scrollToBottomButton!!.setRotation(180f * factor)
        }
    }, DECELERATE_INTERPOLATOR, 120L)

    fun setScrollToBottomVisible(isVisible: Boolean, isReverse: Boolean, animated: Boolean) {
        var isVisible = isVisible
        if (messagesHidden) {
            isVisible = false
        }
        scrollToBottomVisible.setValue(isVisible, animated)
        scrollToBottomReverse.setValue(isReverse, animated && scrollToBottomVisible.getFloatValue() > 0f)
        checkExtraPadding()
    }

    fun onFirstChatScroll() {
    }

    private fun setUnreadCountBadge(unreadCount: Int, isUpdate: Boolean) {
        if (unreadCount != 0 && (inPreviewMode || isInForceTouchMode())) {
            return
        }
        val animated = isUpdate && scrollToBottomVisible.getFloatValue() > 0f && isFocused()
        val isMuted = messageThread == null && !tdlib!!.chatNotificationsEnabled(getChatId())
        unreadCountView!!.setCounter(unreadCount, isMuted, animated)
    }

    private var mentionButtonAnimator: FactorAnimator? = null
    private var mentionButtonVisible = false
    private var mentionButtonFactor = 1f
    private var mentionCountBadge = 0

    private fun setMentionButtonVisible(visible: Boolean, animated: Boolean) {
        if (this.mentionButtonVisible != visible) {
            this.mentionButtonVisible = visible
            val toFactor = if (visible) 1f else 0f
            if (animated) {
                if (mentionButtonAnimator == null) {
                    mentionButtonAnimator = FactorAnimator(ANIMATOR_MENTION_BUTTON, this, DECELERATE_INTERPOLATOR, 180L, this.mentionButtonFactor)
                }
                mentionButtonAnimator!!.animateTo(toFactor)
            } else {
                if (mentionButtonAnimator != null) {
                    mentionButtonAnimator!!.forceFactor(toFactor)
                }
                setMentionButtonFactor(toFactor)
            }
        }
    }

    private fun setMentionCountBadge(mentionCount: Int) {
        if (mentionCount > 0 && (inPreviewMode || isInForceTouchMode() || areScheduledOnly())) {
            return
        }
        if (mentionCountBadge != mentionCount) {
            mentionCountBadge = mentionCount
            val visible = mentionCount > 0
            val animate = isFocused()
            mentionCountView!!.setCounter(mentionCount, false, animate && mentionButtonFactor > 0f)
            setMentionButtonVisible(visible, animate)
        }
    }

    private fun setMentionButtonFactor(factor: Float) {
        if (this.mentionButtonFactor != factor) {
            this.mentionButtonFactor = factor
            val range = clamp(factor)
            mentionButtonWrap!!.setAlpha(range)
            updateBottomBarStyle()
        }
    }


    private var reactionButtonAnimator: FactorAnimator? = null
    private var reactionButtonVisible = false
    private var reactionButtonFactor = 1f
    private var reactionCountBadge = 0

    private fun setReactionButtonVisible(visible: Boolean, animated: Boolean) {
        if (this.reactionButtonVisible != visible) {
            this.reactionButtonVisible = visible
            val toFactor = if (visible) 1f else 0f
            if (animated) {
                if (reactionButtonAnimator == null) {
                    reactionButtonAnimator = FactorAnimator(ANIMATOR_REACTION_BUTTON, this, DECELERATE_INTERPOLATOR, 180L, this.reactionButtonFactor)
                }
                reactionButtonAnimator!!.animateTo(toFactor)
            } else {
                if (reactionButtonAnimator != null) {
                    reactionButtonAnimator!!.forceFactor(toFactor)
                }
                setReactionButtonFactor(toFactor)
            }
        }
    }

    private fun setReactionCountBadge(reactionCount: Int) {
        if (reactionCount > 0 && (inPreviewMode || isInForceTouchMode() || areScheduledOnly())) {
            return
        }
        if (reactionCountBadge != reactionCount) {
            reactionCountBadge = reactionCount
            val visible = reactionCount > 0
            val animate = isFocused()
            reactionsCountView!!.setCounter(reactionCount, true, animate && reactionButtonFactor > 0f)
            setReactionButtonVisible(visible, animate)
        }
        if (reactionCount > 0) {
            reactionsButton!!.setUnreadReaction(tdlib!!.getSingleUnreadReaction(getChatId()))
        }
    }

    private fun setReactionButtonFactor(factor: Float) {
        if (this.reactionButtonFactor != factor) {
            this.reactionButtonFactor = factor
            val range = clamp(factor)
            reactionsButtonWrap!!.setAlpha(range)
            updateBottomBarStyle()
        }
    }


    // Etc
    fun startSwipe(view: View?) {
        messagesView!!.startSwipe(view)
    }

    // Reply utils
    fun highlightMessage(messageId: MessageId?, isScheduled: Boolean): Boolean {
        if (areScheduledOnly() == isScheduled) {
            highlightMessage(messageId)
            return true
        }
        return false
    }

    fun highlightMessage(messageId: MessageId, parameters: UrlOpenParameters?) {
        val paramMsgId = parameters?.messageId
        if (paramMsgId != null && paramMsgId.chatId == messageId.chatId) {
            highlightMessage(messageId, paramMsgId)
        } else {
            highlightMessage(messageId)
        }
    }

    fun highlightMessage(messageId: MessageId?) {
        if (inPreviewSearchMode()) {
            tdlib!!.ui().openMessage(this, getChatId(), messageId, null)
            return
        }
        manager!!.highlightMessage(messageId, MessagesManager.HIGHLIGHT_MODE_NORMAL, null, pagerScrollPosition == 0)
        showMessagesListIfNeeded()
    }

    fun highlightMessage(messageId: MessageId?, fromMessageId: MessageId?) {
        if (inOnlyFoundMode()) {
            manager!!.setHighlightMessageId(messageId, MessagesManager.HIGHLIGHT_MODE_NORMAL)
            onSetSearchFilteredShowMode(false)
            return
        }
        val returnToMessageIds = manager!!.extendReturnToMessageIdStack(fromMessageId)
        highlightMessage(messageId, returnToMessageIds)
    }

    fun highlightMessage(messageId: MessageId?, returnToMessageIds: LongArray?) {
        if (inPreviewSearchMode()) {
            tdlib!!.ui().openMessage(this, getChatId(), messageId, null)
            return
        }
        manager!!.highlightMessage(messageId, MessagesManager.HIGHLIGHT_MODE_NORMAL, returnToMessageIds, pagerScrollPosition == 0)
        showMessagesListIfNeeded()
    }

    class ReplyInfo @JvmOverloads constructor(
        val tdlib: Tdlib?,
        val message: MessageWithProperties,
        val quote: InputTextQuote?,
        val checklistTaskId: Int,
        val pollOptionId: String?,
        val inChatId: Long = 0,
        val inTopicId: MessageTopic? = null
    ) {
        fun toMessageId(): MessageId {
            return MessageId(message.message)
        }

        fun withTopic(inChatId: Long, inTopicId: MessageTopic?): ReplyInfo {
            var inTopicId = inTopicId
            if (inTopicId == null && inChatId == message.message.chatId) {
                inTopicId = message.message.topicId
            }
            if (inTopicId != null && inTopicId.getConstructor() == TdApi.MessageTopicSavedMessages.CONSTRUCTOR) {
                inTopicId = null
            }
            return ReplyInfo(tdlib, message, quote, checklistTaskId, pollOptionId, inChatId, inTopicId)
        }

        override fun equals(obj: Any?): Boolean {
            if (this === obj) return true
            if (obj is ReplyInfo) {
                val other = obj
                return toInputMessageReply().equalsTo(other.toInputMessageReply())
            }
            return false
        }

        val directMessagesTopicId: Long
            get() {
                if (inTopicId != null && inTopicId.getConstructor() == MessageTopicDirectMessages.CONSTRUCTOR) {
                    return (inTopicId as MessageTopicDirectMessages).directMessagesChatTopicId
                }
                return 0L
            }

        fun toInputMessageReply(): InputMessageReplyTo {
            if (inChatId != message.message.chatId || (inTopicId != null && !inTopicId.equalsTo(message.message.topicId))) {
                return InputMessageReplyToExternalMessage(
                    message.message.chatId,
                    message.message.id,
                    quote,
                    checklistTaskId,
                    pollOptionId
                )
            } else {
                return InputMessageReplyToMessage(
                    message.message.id,
                    quote,
                    checklistTaskId,
                    pollOptionId
                )
            }
        }
    }

    private var reply: ReplyInfo? = null
    private var replyBarView: ReplyBarView? = null

    private var topBar: CollapseListView? = null
    private var actionView: TopBarView? = null
    private var actionItem: CollapseListView.Item? = null

    private var toastAlertView: CustomTextView? = null
    private var toastAlertItem: CollapseListView.Item? = null

    private var pinnedMessagesBar: PinnedMessagesBar? = null
    private var pinnedMessagesItem: CollapseListView.Item? = null

    private var requestsView: JoinRequestsView? = null
    private var requestsItem: CollapseListView.Item? = null

    val currentReplyId: ReplyInfo?
        get() {
            if (reply != null) {
                return reply!!.withTopic(this.messageThreadChatId, getMessageTopicId())
            }
            return null
        }

    val messageThreadChatId: Long
        get() = if (messageThread != null) messageThread!!.getChatId() else getChatId()

    fun obtainReplyTo(): ReplyInfo? {
        if (reply != null) {
            val replyTo = this.currentReplyId
            closeReply(true, false)
            return replyTo
        }
        return null
    }

    fun removeReply(chatId: Long, messageIds: LongArray) {
        if (reply != null) {
            for (msgId in messageIds) {
                if (reply!!.message.message.chatId == chatId && msgId == reply!!.message.message.id) {
                    setReplyInfo(null, true)
                    break
                }
            }
        }
    }

    fun showReply(msg: MessageWithProperties?, quote: InputTextQuote?, checklistTaskId: Int, pollOptionId: String?, byUser: Boolean, showKeyboard: Boolean) {
        if (inPreviewMode || isInForceTouchMode()) {
            return
        }
        if (msg == null || msg.message.id == 0L) {
            setReplyInfo(null, true)
            return
        }
        if (inSearchMode()) {
            // TODO prevent keyboard close
            closeSearchMode(null)
        } else if (inSelectMode()) {
            finishSelectMode(-1)
        }
        collapsePinnedMessagesBar(true)
        // TODO show keyboard properly
        if (reply == null || reply!!.message.message.id != msg.message.id || reply!!.message.message.chatId != msg.message.chatId || !reply!!.quote.equalsTo(
                quote
            ) || reply!!.checklistTaskId != checklistTaskId || !reply!!.pollOptionId.equalsOrBothEmpty(pollOptionId)
        ) {
            setReplyInfo(ReplyInfo(tdlib, msg, quote, checklistTaskId, pollOptionId), true)

            if (byUser) {
                inputView!!.setTextChangedSinceChatOpened(true)
                saveDraft()
            }
        }
        if (showKeyboard) {
            Keyboard.show(inputView)
        }
    }

    private fun updateReplyBarVisibility(animated: Boolean) {
        if (replyBarView == null) {
            return
        }
        var shouldBeVisible = true
        if (showingLinkPreview()) {
            replyBarView!!.showWebPage(findTargetContext(), findTargetContext().findSelectedUrlIndex())
        } else if (this.isEditingMessage) {
            val ctx = editContext!!
            replyBarView!!.setEditingMessage(
                MessageWithProperties(ctx.existingMessage!!, ctx.messageProperties!!),
                ctx.localPickedFile
            )
        } else if (reply != null) {
            replyBarView!!.setReplyTo(reply!!.message, reply!!.quote)
        } else {
            shouldBeVisible = false
        }
        val toFactor = if (shouldBeVisible) 1f else 0f
        if (animated && replyBarVisible.getFloatValue() != toFactor) {
            setForceHw(true) // Resets back in onFactorChangeFinished
        }
        replyBarVisible.setValue(shouldBeVisible, animated)
    }

    override fun onDismissReplyBar(view: ReplyBarView?) {
        if (showingLinkPreview()) {
            closeLinkPreview()
        } else if (this.isEditingMessage) {
            closeEdit(false)
        } else {
            closeReply(true, true)
        }
    }

    private var mediaPickerManager: MediaToReplacePickerManager? = null

    override fun onMessageMediaReplaceRequested(view: ReplyBarView?, message: TdApi.Message) {
        if (mediaPickerManager == null) {
            mediaPickerManager = MediaToReplacePickerManager(this)
        }
        mediaPickerManager!!.openMediaView(
            RunnableData { file: LocalPickedFile? -> this.setMessageMediaEdited(file!!) },
            getChatId(),
            null,
            false,
            this,
            message
        )
    }

    override fun onMessageMediaEditRequested(view: ReplyBarView?, message: TdApi.Message?) {
        val ctx = editContext
        if (ctx != null && ctx.localPickedFile != null && ctx.localPickedFile!!.imageGalleryFile != null) {
            onMessageMediaEditRequestedImpl(ctx.localPickedFile!!.imageGalleryFile)
        } else {
            U.toGalleryFile(message, RunnableData { imageGalleryFile: ImageGalleryFile? -> this.onMessageMediaEditRequestedImpl(imageGalleryFile) })
        }
    }

    private fun onMessageMediaEditRequestedImpl(imageGalleryFile: ImageGalleryFile?) {
        if (imageGalleryFile == null || isDestroyed()) {
            return
        }
        if (inputView != null) {
            imageGalleryFile.setCaption(inputView!!.getOutputText(false))
        }

        val stack = MediaStack(context, tdlib)
        stack.set(MediaItem(context, tdlib, imageGalleryFile))

        val controller = MediaViewController(context, tdlib)
        controller.setArguments(
            fromGallery(
                this, null, object : MediaSelectDelegate {
                    override fun isMediaItemSelected(index: Int, item: MediaItem?): Boolean {
                        return false
                    }

                    override fun setMediaItemSelected(index: Int, item: MediaItem?, isSelected: Boolean) {
                    }

                    override fun getSelectedMediaCount(): Int {
                        return 0
                    }

                    override fun getOutputChatId(): Long {
                        return this@MessagesController.getOutputChatId()
                    }

                    override fun canDisableMarkdown(): Boolean {
                        return false
                    }

                    override fun getSelectedMediaItems(copy: Boolean): ArrayList<ImageFile?>? {
                        return null
                    }
                },
                object : MediaSpoilerSendDelegate() {
                    override fun sendSelectedItems(
                        view: View?,
                        images: ArrayList<ImageFile?>?,
                        options: MessageSendOptions?,
                        disableMarkdown: Boolean,
                        asFiles: Boolean,
                        showCationAboveMedia: Boolean,
                        hasSpoiler: Boolean
                    ): Boolean {
                        if (isDestroyed()) {
                            return false
                        }
                        // FIXME: hasSpoiler, showCationAboveMedia
                        setMessageMediaEdited(LocalPickedFile(imageGalleryFile))
                        return true
                    }
                },
                stack, areScheduledOnly()
            )
                .setFlag(MediaViewController.Args.FLAG_DISALLOW_MULTI_SELECTION_MEDIA)
                .setFlag(MediaViewController.Args.FLAG_DISALLOW_SET_DESTRUCTION_TIMER)
                .setFlag(MediaViewController.Args.FLAG_DISALLOW_SEND_BUTTON_HAPTIC_MENU)
                .setSendButtonIconRes(R.drawable.baseline_check_circle_24)
                .setReceiverChatIdValue(getChatId())
        )
        controller.open()
    }

    private fun setMessageMediaEdited(file: LocalPickedFile) {
        editContext!!.localPickedFile = file
        if (inputView != null && file.imageGalleryFile != null) {
            val formattedText = file.imageGalleryFile.getCaption(true, false)
            if (!formattedText.isEmpty()) {
                val text = TD.toCharSequence(formattedText)
                inputView!!.setText(text)
                inputView!!.setSelection(text.length)
            }
        }

        updateReplyBarVisibility(true)
    }

    private var anotherChatHint: TooltipInfo? = null

    override fun onMessageHighlightRequested(view: ReplyBarView?, message: TdApi.Message, quote: InputTextQuote?) {
        if (message.chatId == getChatId()) {
            highlightMessage(MessageId(message.chatId, message.id))
        } else {
            if (anotherChatHint != null && anotherChatHint!!.isVisible()) {
                tdlib!!.ui().openMessage(this, message.chatId, MessageId(message.chatId, message.id), UrlOpenParameters().controller(this))
                return
            }
            anotherChatHint = context()
                .tooltipManager()
                .builder(view)
                .show(this, tdlib, R.drawable.baseline_info_24, Lang.getString(R.string.AnotherChatReplyHint))
                .hideDelayed()
        }
    }

    private fun forceDraftReply(replyTo: InputMessageReplyTo) {
        val currentChatId = chat!!.id
        val replyToChatId: Long
        val replyToMessageId: Long
        val replyToQuote: InputTextQuote?
        val replyToChecklistTaskId: Int
        val replyToPollOptionId: String?
        when (replyTo.getConstructor()) {
            InputMessageReplyToMessage.CONSTRUCTOR -> {
                val replyToMessage = replyTo as InputMessageReplyToMessage
                replyToChatId = currentChatId
                replyToMessageId = replyToMessage.messageId
                replyToQuote = replyToMessage.quote
                replyToChecklistTaskId = replyToMessage.checklistTaskId
                replyToPollOptionId = replyToMessage.pollOptionId
            }

            InputMessageReplyToExternalMessage.CONSTRUCTOR -> {
                val replyToExternalMessage = replyTo as InputMessageReplyToExternalMessage
                replyToChatId = replyToExternalMessage.chatId
                replyToMessageId = replyToExternalMessage.messageId
                replyToQuote = replyToExternalMessage.quote
                replyToChecklistTaskId = replyToExternalMessage.checklistTaskId
                replyToPollOptionId = replyToExternalMessage.pollOptionId
            }

            TdApi.InputMessageReplyToStory.CONSTRUCTOR -> return
            else -> {
                assertInputMessageReplyTo_acef6f3a()
                throw unsupported(replyTo)
            }
        }
        if (replyToChatId == currentChatId) {
            val foundMessage = manager!!.getAdapter().findMessageById(replyToMessageId)
            if (foundMessage != null) {
                foundMessage.getMessageWithProperties(RunnableData { msg: MessageWithProperties? ->
                    runOnUiThreadOptional(Runnable {
                        forceReply(msg, replyToQuote, replyToChecklistTaskId, replyToPollOptionId)
                    })
                })
                return
            }
        }
        tdlib!!.send<TdApi.Message?>(GetMessage(replyToChatId, replyToMessageId), Tdlib.ResultHandler { foundReplyMessage: TdApi.Message?, error: TdApi.Error? ->
            if (foundReplyMessage != null) {
                runOnUiThreadOptional(Runnable {
                    if (chat != null && chat!!.id == currentChatId) {
                        val draftMessage = this.draftMessage
                        val currentReplyTo = if (draftMessage != null) draftMessage.replyTo else null
                        if (replyTo.equalsTo(currentReplyTo)) {
                            tdlib!!.getMessageProperties(foundReplyMessage, RunnableData { properties: MessageProperties? ->
                                runOnUiThreadOptional(Runnable {
                                    if (properties != null) {
                                        forceReply(
                                            MessageWithProperties(foundReplyMessage, properties),
                                            replyToQuote,
                                            replyToChecklistTaskId,
                                            replyToPollOptionId
                                        )
                                    }
                                })
                            })
                        }
                    }
                })
            }
        })
    }

    fun forceReply(message: MessageWithProperties?, quote: InputTextQuote?, checklistTaskId: Int, pollOptionId: String?) {
        if (message == null || chat == null || inPreviewMode || isInForceTouchMode()) {
            clearReply()
            return
        }

        setReplyInfo(ReplyInfo(tdlib, message, quote, checklistTaskId, pollOptionId), false)
    }

    private fun setReplyInfo(replyInfo: ReplyInfo?, animated: Boolean) {
        val replyRequired = this.isReplyRequired
        this.reply = replyInfo
        updateReplyBarVisibility(animated)
        if (this.isReplyRequired != replyRequired) {
            updateBottomBar(true)
        }
    }

    private fun clearReply() {
        draftContext.reset()
        setReplyInfo(null, false)
        updateReplyBarVisibility(false)
    }

    fun closeReply(byUser: Boolean, animated: Boolean) {
        tdlib!!.uiExecute(Runnable {
            if (reply != null) {
                setReplyInfo(null, animated)
                if (byUser) {
                    inputView!!.setTextChangedSinceChatOpened(true)
                    saveDraft()
                }
            }
        })
    }

    private var forceHw = false
    private var originalLayerType1 = 0
    private var originalLayerType2 = 0
    private var originalLayerType3 = 0

    private fun setForceHw(forceHw: Boolean) {
        if (!Views.HARDWARE_LAYER_ENABLED) return
        if (this.forceHw != forceHw) {
            this.forceHw = forceHw
            if (forceHw) {
                originalLayerType1 = messagesView!!.getLayerType()
                if (originalLayerType1 != View.LAYER_TYPE_HARDWARE) {
                    Views.setLayerType(messagesView, View.LAYER_TYPE_HARDWARE)
                }
                if (replyBarView != null) {
                    originalLayerType2 = replyBarView!!.getLayerType()
                    if (originalLayerType2 != View.LAYER_TYPE_HARDWARE) {
                        Views.setLayerType(replyBarView, View.LAYER_TYPE_HARDWARE)
                    }
                }
                originalLayerType3 = bottomShadowView!!.getLayerType()
                if (originalLayerType3 != View.LAYER_TYPE_HARDWARE) {
                    Views.setLayerType(bottomShadowView, View.LAYER_TYPE_HARDWARE)
                }
            } else {
                if (originalLayerType1 != View.LAYER_TYPE_HARDWARE) {
                    Views.setLayerType(messagesView, originalLayerType1)
                }
                if (originalLayerType2 != View.LAYER_TYPE_HARDWARE) {
                    Views.setLayerType(replyBarView, originalLayerType2)
                }
                if (originalLayerType3 != View.LAYER_TYPE_HARDWARE) {
                    Views.setLayerType(bottomShadowView, originalLayerType3)
                }
            }
        }
    }

    private val replyBarVisible = BoolAnimator(0, object : FactorAnimator.Target {
        override fun onFactorChanged(id: Int, factor: Float, fraction: Float, callee: FactorAnimator?) {
            if (ANIMATE_REPLY_BAR && replyBarView != null) {
                replyBarView!!.setAnimationsDisabled(factor == 0f)
            }
            updateReplyView()
        }

        override fun onFactorChangeFinished(id: Int, finalFactor: Float, callee: FactorAnimator?) {
            if (finalFactor == 1f || finalFactor == 0f) {
                setForceHw(false)
            }
            if (finalFactor == 0f) {
                replyBarView!!.reset()
            }
        }
    }, DECELERATE_INTERPOLATOR, 200L)

    private val replyOffset: Float
        get() {
            if (replyBarView == null) {
                return 0f
            }
            return replyBarVisible.getFloatValue() * (1f - getSearchTransformFactor()) * (replyBarView!!.getLayoutParams().height).toFloat()
        }

    private val buttonsOffset: Float
        get() = this.replyOffset + this.attachedFilesOffset + this.searchControlsOffset + this.keyboardOffset + (if (bottomWrap!!.getVisibility() == View.VISIBLE) 0 else extraBottomInsetWithoutIme)

    private val mentionButtonY: Float
        get() {
            val moveY = Screen.dp(74f) - Screen.dp(16f)
            var y = -this.buttonsOffset - moveY * scrollToBottomVisible.getFloatValue()
            if (isInForceTouchMode()) {
                y += (-Screen.dp(16f) + Screen.dp(4f)).toFloat()
            }
            return y
        }

    private val reactionButtonY: Float
        get() {
            val scrollToBottomOffset = Screen.dp(74f) - Screen.dp(16f)
            val mentionButtonOffset = Screen.dp(74f) - Screen.dp(16f)

            var y = -this.buttonsOffset
            y -= scrollToBottomOffset * scrollToBottomVisible.getFloatValue()
            y -= mentionButtonOffset * mentionButtonFactor

            if (isInForceTouchMode()) {
                y += (-Screen.dp(16f) + Screen.dp(4f)).toFloat()
            }

            return y
        }

    private fun checkScrollButtonOffsets() {
        if (isInForceTouchMode()) {
            return
        }
        val offsetY = -this.buttonsOffset
        if (scrollToBottomButtonWrap != null) {
            scrollToBottomButtonWrap!!.setTranslationY(offsetY)
        }
        if (mentionButtonWrap != null) {
            mentionButtonWrap!!.setTranslationY(this.mentionButtonY)
        }

        if (reactionsButtonWrap != null) {
            reactionsButtonWrap!!.setTranslationY(this.reactionButtonY)
        }
        if (goToPrevFoundMessageButtonBadge != null) {
            goToPrevFoundMessageButtonBadge!!.setTranslationY(offsetY)
        }
        if (goToNextFoundMessageButtonBadge != null) {
            goToNextFoundMessageButtonBadge!!.setTranslationY(offsetY - Screen.dp((74 - 16).toFloat()))
        }
        updateBottomBarStyle()
    }

    private fun updateReplyView() {
        val y = -this.replyOffset
        val offset = -this.attachedFilesOffset
        val keyboardOffset = -this.keyboardOffset
        messagesView!!.setTranslationY(y + offset + keyboardOffset)
        bottomShadowView!!.setTranslationY(y + offset + keyboardOffset)
        if (replyBarView != null) {
            replyBarView!!.setTranslationY(y + keyboardOffset)
        }
        checkScrollButtonOffsets()
        onMessagesFrameChanged()
    }

    val isEditingMessage: Boolean
        // Edit utils
        get() = editContext != null

    fun hasEditedChanges(): Boolean {
        if (this.isEditingMessage) {
            val newText = if (inputView != null) inputView!!.getOutputText(true) else null
            val newOptions = if (inputView != null) editContext!!.takeOutputLinkPreviewOptions(false) else null

            when (editContext!!.existingMessage!!.content.getConstructor()) {
                MessageAnimatedEmoji.CONSTRUCTOR -> {
                    val oldText = editContext!!.existingMessage!!.content.textOrCaption()
                    if (!oldText.equalsTo(newText) || !null.equalsTo(newOptions)) {
                        return true
                    }
                }

                MessageText.CONSTRUCTOR -> {
                    val oldMessageText = editContext!!.existingMessage!!.content as MessageText
                    val oldLinkPreviewOptions = oldMessageText.linkPreviewOptions
                    if (!oldMessageText.text.equalsTo(newText) || !oldLinkPreviewOptions.equalsTo(newOptions)) {
                        return true
                    }
                }

                TdApi.MessagePhoto.CONSTRUCTOR, TdApi.MessageVideo.CONSTRUCTOR, TdApi.MessageAudio.CONSTRUCTOR, TdApi.MessageVoiceNote.CONSTRUCTOR, MessageDocument.CONSTRUCTOR, TdApi.MessageAnimation.CONSTRUCTOR -> {
                    val oldText = editContext!!.existingMessage!!.content.textOrCaption()
                    return !oldText.equalsTo(newText)
                }

                else -> {
                    assertMessageContent_baa076bf()
                }
            }
        }

        return false
    }

    val isEditingCaption: Boolean
        get() = editContext != null && editContext!!.isMediaCaption

    class MessageInputContext internal constructor(
        private val context: MessagesController,
        private val tdlib: Tdlib?,
        val existingMessage: TdApi.Message?,
        internal val messageProperties: MessageProperties?
    ) {
        var foundUrls: FoundUrls
            private set
        internal var localPickedFile: LocalPickedFile? = null

        private var dismissedFoundUrls: FoundUrls? = null

        internal val linkPreviewOptions: LinkPreviewOptions

        val isMediaCaption: Boolean
            get() = this.existingMessage != null && !existingMessage.content.isText()

        fun setLinkPreviewUrl(url: String): Boolean {
            if (url == this.linkPreviewOptions.url) {
                return false
            }
            if (isEmpty(this.linkPreviewOptions.url) && !foundUrls.isEmpty() && url == foundUrls.urls[0]) {
                return false
            }
            this.linkPreviewOptions.url = url
            return true
        }

        private val linkPreviews: MutableMap<String?, LinkPreview?> = HashMap<String?, LinkPreview?>()

        init {
            this.foundUrls = FoundUrls.emptyResult()
            if (existingMessage != null && existingMessage.content.isText()) {
                val messageText = existingMessage.content as MessageText
                linkPreviewOptions = messageText.linkPreviewOptions.copyOf()
                if (linkPreviewOptions.isDisabled) {
                    foundUrls = FoundUrls(messageText)
                    dismissedFoundUrls = foundUrls
                }
            } else {
                linkPreviewOptions = LinkPreviewOptions()
            }
        }

        val selectedLinkPreview: LinkPreview?
            get() {
                if (linkPreviewOptions.isDisabled) {
                    return null
                }
                val index = findSelectedUrlIndex()
                if (index == -1) return null
                val url = foundUrls.urls[index]
                return getLinkPreview(url)
            }

        fun getLinkPreview(url: String?): LinkPreview {
            var linkPreview = linkPreviews.get(url)
            if (linkPreview == null) {
                linkPreview = LinkPreview(tdlib, url, this.existingMessage)
                linkPreview.addLoadCallback(RunnableData { loadedLinkPreview: LinkPreview? ->
                    if (loadedLinkPreview!!.isNotFound()) {
                        context.updateReplyBarVisibility(true)
                    }
                })
                linkPreviews.put(url, linkPreview)
            }
            return linkPreview
        }

        fun findSelectedUrlIndex(): Int {
            if (foundUrls.isEmpty()) {
                return -1
            } else if (isEmpty(linkPreviewOptions.url)) {
                return 0
            } else {
                val index = foundUrls.indexOfUrl(linkPreviewOptions.url)
                if (index != -1) {
                    return index
                }
                return 0
            }
        }

        fun takeOutputLinkPreviewOptions(copy: Boolean): LinkPreviewOptions {
            val linkPreview = this.selectedLinkPreview
            if (linkPreview != null) {
                val forceLargeMedia = linkPreview.forceLargeMedia()
                val forceSmallMedia = linkPreview.forceSmallMedia()
                if (linkPreviewOptions.forceLargeMedia != forceLargeMedia || linkPreviewOptions.forceSmallMedia != forceSmallMedia) {
                    linkPreviewOptions.forceLargeMedia = forceLargeMedia
                    linkPreviewOptions.forceSmallMedia = forceSmallMedia
                }
                if ((forceLargeMedia || forceSmallMedia) && isEmpty(linkPreviewOptions.url)) {
                    linkPreviewOptions.url = linkPreview.url
                }
            }
            return if (copy) linkPreviewOptions.copyOf() else linkPreviewOptions
        }

        fun takeOutputShowCaptionAboveMedia(): Boolean {
            // TODO
            return existingMessage!!.content.showCaptionAboveMedia()
        }

        fun takePreloadedOutputLinkPreview(): TdApi.LinkPreview? {
            val preview = this.selectedLinkPreview
            var linkPreview = if (preview != null) preview.linkPreview else null
            if (linkPreview != null && linkPreview.hasLargeMedia) {
                val showLargeMedia = preview!!.getOutputShowLargeMedia()
                if (linkPreview.showLargeMedia != showLargeMedia) {
                    linkPreview = linkPreview.copyOf()
                    linkPreview!!.showLargeMedia = showLargeMedia
                }
            }
            return linkPreview
        }

        fun checkMessage(chatId: Long, messageId: Long): Boolean {
            return this.existingMessage != null && existingMessage.chatId == chatId && existingMessage.id == messageId
        }

        fun setFoundUrls(foundUrls: FoundUrls?): Boolean {
            var foundUrls = foundUrls
            if (foundUrls == null) {
                foundUrls = FoundUrls.emptyResult()
            }

            val previouslyFoundUrls: FoundUrls? = this.foundUrls
            this.foundUrls = foundUrls

            val wasEmpty = previouslyFoundUrls!!.isEmpty()
            val nowEmpty = foundUrls.isEmpty()

            var hasChanges = wasEmpty != nowEmpty || (!nowEmpty && previouslyFoundUrls != foundUrls)

            if (dismissedFoundUrls != null && foundUrls != dismissedFoundUrls) {
                linkPreviewOptions.isDisabled = false
                dismissedFoundUrls = null
                hasChanges = true
            }
            if (!isEmpty(linkPreviewOptions.url) && !foundUrls.hasUrl(linkPreviewOptions.url)) {
                linkPreviewOptions.url = null
                hasChanges = true
            }
            if (hasChanges && !foundUrls.isEmpty()) {
                linkPreviewOptions.isDisabled = false
            }
            if (hasChanges && foundUrls.size() > 1 && Settings.instance().needTutorial(Settings.TUTORIAL_MULTIPLE_LINK_PREVIEWS)) {
                Settings.instance().markTutorialAsShown(Settings.TUTORIAL_MULTIPLE_LINK_PREVIEWS)
                context.context()
                    .tooltipManager()
                    .builder(context.replyBarView)
                    .icon(R.drawable.baseline_info_24)
                    .show(tdlib, R.string.SwipeToSwapLinkPreview)
            }

            return hasChanges
        }

        fun dismiss() {
            dismissedFoundUrls = foundUrls
            linkPreviewOptions.reset()
            linkPreviewOptions.isDisabled = true
        }

        fun reset() {
            dismissedFoundUrls = null
            foundUrls = FoundUrls.emptyResult()
            linkPreviewOptions.reset()
        }

        val isVisible: Boolean
            get() {
                if (!linkPreviewOptions.isDisabled && !foundUrls.isEmpty()) {
                    for (url in foundUrls.urls) {
                        val linkPreview = linkPreviews.get(url)
                        if (linkPreview == null || !linkPreview.isNotFound()) {
                            return true
                        }
                    }
                }
                return false
            }
    }

    private fun setInEditMode(inEditMode: Boolean, futureText: String?) {
        sendButton!!.setInEditMode(inEditMode)
        messageSenderButton!!.setAnimateVisible(!inEditMode)
        if (inputView != null) {
            inputView!!.setIsInEditMessageMode(inEditMode, futureText)
        }
    }

    fun inSimpleSendMode(): Boolean {
        return sendButton!!.inSimpleSendMode()
    }

    private var inputBlockFlags = 0

    private fun setInputBlockFlags(flags: Int): Boolean {
        if (this.inputBlockFlags != flags) {
            val prevIsBlocked = this.inputBlockFlags != 0
            this.inputBlockFlags = flags
            val nowIsBlocked = flags != 0
            if (prevIsBlocked != nowIsBlocked && inputView != null) {
                inputView!!.setInputBlocked(nowIsBlocked)
            }
            return true
        }
        return false
    }

    private fun setInputBlockFlag(flag: Int, active: Boolean) {
        if (setInputBlockFlags(setFlag(inputBlockFlags, flag, active))) {
            if ((flag == FLAG_INPUT_OFFSCREEN || flag == FLAG_INPUT_TEXT_DISABLED) && inputView != null) {
                inputView!!.setEnabled(
                    !hasFlag(inputBlockFlags, FLAG_INPUT_OFFSCREEN) &&
                            !hasFlag(inputBlockFlags, FLAG_INPUT_TEXT_DISABLED)
                )
            }
        }
    }

    fun arePinnedMessages(): Boolean {
        return previewSearchFilter != null && previewSearchFilter!!.isPinnedFilter()
    }

    fun openPreviewMessage(msg: TGMessage) {
        if (arePinnedMessages()) {
            val c = previousStackItem()
            if (c is MessagesController && c.getChatId() == getChatId()) {
                c.highlightMessage(msg.toMessageId())
                navigateBack()
                return
            }
        }
        tdlib()!!.ui().openMessage(this, msg.chatId, msg.toMessageId(), null)
    }

    private fun resetEditState() {
        editContext = null
        if (inputView != null) {
            inputView!!.resetState()
            setInputBlockFlag(FLAG_INPUT_EDITING, false)
        }
        sendButton!!.forceState(false, false)
    }

    private fun editMessage(m: MessageWithProperties) {
        if (this.isEditingMessage) {
            return
        }

        val msg = m.message
        val properties = m.properties

        needShowEmojiKeyboardAfterHideMessageOptions = false
        saveDraft()

        if (inSelectMode()) {
            finishSelectMode(-1)
        } else if (inSearchMode()) {
            closeSearchMode(null)
        }

        this.editContext = MessageInputContext(this, tdlib, msg, properties)
        var text = msg.content.textOrCaption()
        setInEditMode(true, text!!.text)
        checkAttachedFiles(true)
        sendButton!!.setIsActive(!isEmpty(text.text) || this.isEditingCaption)
        updateReplyBarVisibility(true)
        if (inputView != null) {
            val pendingText = tdlib!!.getPendingFormattedText(msg.chatId, msg.id)
            if (pendingText != null) {
                text = pendingText
            }
            inputView!!.setInput(
                if (Settings.instance().getNewSetting(Settings.SETTING_FLAG_EDIT_MARKDOWN)) TD.toMarkdown(text) else TD.toCharSequence(text),
                true,
                false
            )
        }
        Keyboard.show(inputView)
    }

    private fun closeEdit(isSaved: Boolean) {
        if (!this.isEditingMessage) {
            return
        }

        if (inputView != null) {
            val draftMessage = this.draftMessage
            inputView!!.setDraft(if (draftMessage != null) draftMessage.inputMessageText else null)
            setInputBlockFlag(FLAG_INPUT_EDITING, false)
        }
        setInEditMode(false, "")
        editContext = null
        checkAttachedFiles(true)
        if (inputView != null) {
            updateSendButton(inputView!!.getInput(), true)
        }

        // Intentionally doesn't match the send logic (where there's no animation after send).
        // For the exact match, !isSaved could be passed here instead.
        updateReplyBarVisibility(true)
    }

    private fun saveMessage(applyMarkdown: Boolean) {
        if (!this.isEditingMessage || inputView == null) {
            return
        }

        val newText = inputView!!.getOutputText(applyMarkdown)
        val newOptions = editContext!!.takeOutputLinkPreviewOptions(true)
        val newShowCaptionAboveMedia = editContext!!.takeOutputShowCaptionAboveMedia()

        when (editContext!!.existingMessage!!.content.getConstructor()) {
            MessageText.CONSTRUCTOR, MessageAnimatedEmoji.CONSTRUCTOR -> {
                if (newText.isEmpty()) {
                    return
                }

                val oldMessageText: MessageText
                if (editContext!!.existingMessage!!.content.getConstructor() == MessageAnimatedEmoji.CONSTRUCTOR) {
                    oldMessageText = MessageText(editContext!!.existingMessage!!.content.textOrCaption(), null, null)
                } else {
                    oldMessageText = editContext!!.existingMessage!!.content as MessageText
                }
                val oldLinkPreviewOptions = oldMessageText.linkPreviewOptions

                val newInputMessageText = InputMessageText(newText, newOptions, false)
                if (!newInputMessageText.text.equalsTo(oldMessageText.text) || !newInputMessageText.linkPreviewOptions.equalsTo(oldLinkPreviewOptions)) {
                    val maxLength = tdlib!!.maxMessageTextLength()
                    val newTextLength = if (newText != null) newText.text.codePointCount(0, newText.text.length) else 0
                    if (newTextLength > maxLength) {
                        showBottomHint(Lang.pluralBold(R.string.EditMessageTextTooLong, (newTextLength - maxLength).toLong()), true)
                        return
                    }
                    val linkPreview = editContext!!.takePreloadedOutputLinkPreview()
                    tdlib!!.editMessageText(editContext!!.existingMessage!!.chatId, editContext!!.existingMessage!!.id, newInputMessageText, linkPreview)
                }
            }

            TdApi.MessagePhoto.CONSTRUCTOR, TdApi.MessageVideo.CONSTRUCTOR, TdApi.MessageAudio.CONSTRUCTOR, TdApi.MessageVoiceNote.CONSTRUCTOR, MessageDocument.CONSTRUCTOR, TdApi.MessageAnimation.CONSTRUCTOR -> {
                val editContext = this.editContext
                val oldText = editContext!!.existingMessage!!.content.textOrCaption()
                val oldShowCaptionAboveMedia = editContext.existingMessage.content.showCaptionAboveMedia()
                if (!oldText.equalsTo(newText) || oldShowCaptionAboveMedia != newShowCaptionAboveMedia || editContext.localPickedFile != null) {
                    val newString = newText.text.trim { it <= ' ' }
                    val maxLength = tdlib!!.maxCaptionLength()
                    val newCaptionLength = newString.codePointCount(0, newString.length)
                    if (newCaptionLength > maxLength) {
                        showBottomHint(Lang.pluralBold(R.string.EditMessageCaptionTooLong, (newCaptionLength - maxLength).toLong()), true)
                        return
                    }
                    if (editContext.localPickedFile != null && editContext.localPickedFile!!.inlineResult != null) {
                        val allowAudio = !tdlib!!.hasReplaceMediaRestriction(editContext.existingMessage, RightId.SEND_AUDIO) && tdlib!!.getRestrictionStatus(
                            chat,
                            RightId.SEND_AUDIO
                        ) == null
                        val allowDocs = !tdlib!!.hasReplaceMediaRestriction(editContext.existingMessage, RightId.SEND_DOCS) && tdlib!!.getRestrictionStatus(
                            chat,
                            RightId.SEND_DOCS
                        ) == null
                        val allowVideos = !tdlib!!.hasReplaceMediaRestriction(editContext.existingMessage, RightId.SEND_VIDEOS) && tdlib!!.getRestrictionStatus(
                            chat,
                            RightId.SEND_VIDEOS
                        ) == null
                        val allowGifs =
                            !tdlib!!.hasReplaceMediaRestriction(editContext.existingMessage, RightId.SEND_OTHER_MESSAGES) && tdlib!!.getRestrictionStatus(
                                chat,
                                RightId.SEND_OTHER_MESSAGES
                            ) == null

                        Media.instance().post(Runnable {
                            // FIXME: hasSpoiler
                            var content = TD.toInputMessageContent(
                                newText,
                                editContext.localPickedFile!!.inlineResult,
                                newShowCaptionAboveMedia,
                                false,
                                allowDocs,
                                allowAudio,
                                allowVideos,
                                allowGifs
                            )
                            if (content != null) {
                                content = tdlib!!.filegen().createThumbnail<InputMessageContent?>(content, this.isSecretChat)
                                val finalContent = content
                                UI.post(Runnable {
                                    tdlib!!.editMessageMedia(
                                        editContext.existingMessage.chatId,
                                        editContext.existingMessage.id,
                                        finalContent,
                                        editContext.localPickedFile!!
                                    )
                                })
                            } else {
                                UI.post(Runnable { showRestriction(sendButton, Lang.getString(R.string.EditMediaRestricted)) })
                            }
                        })
                    } else if (editContext.localPickedFile != null && editContext.localPickedFile!!.imageGalleryFile != null) {
                        editContext.localPickedFile!!.imageGalleryFile.setCaption(newText)
                        Media.instance().post(Runnable {
                            // FIXME: hasSpoiler
                            val content = TD.toContent(
                                tdlib, editContext.localPickedFile!!.imageGalleryFile, false, false, newShowCaptionAboveMedia, false,
                                this.isSecretChat
                            )
                            UI.post(Runnable {
                                tdlib!!.editMessageMedia(
                                    editContext.existingMessage.chatId,
                                    editContext.existingMessage.id,
                                    content,
                                    editContext.localPickedFile!!
                                )
                            })
                        })
                    } else {
                        tdlib!!.editMessageCaption(editContext.existingMessage.chatId, editContext.existingMessage.id, newText, newShowCaptionAboveMedia)
                    }
                }
            }

            else -> {
                assertMessageContent_baa076bf()
                throw unsupported(editContext!!.existingMessage!!.content)
            }
        }

        closeEdit(true)
    }

    // Share utils
    fun shareMessages(messages: Array<TdApi.Message?>?, isExplicitSelection: Boolean) {
        if (messages == null || messages.size == 0) {
            return
        }
        hideAllKeyboards()
        val c = ShareController(context, tdlib)
        c.setArguments(ShareController.Args(messages).setDisallowReply(isExplicitSelection).setAfter(Runnable { finishSelectMode(-1) }))
        c.show()
        hideCursorsForInputView()
    }

    // Markup utils
    private var lastCmdResource = 0

    private fun setCameraVisible(isVisible: Boolean): Boolean {
        val visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
        // TODO
        return false
    }

    fun updateCommandButton(isVisible: Boolean) {
        val ok = setCameraVisible(!isVisible || !isUserChat(getChatId()))
        if (commandButton!!.setVisible(isVisible) || ok) {
            attachButtons!!.updatePivot()
        }
    }

    private fun updateCommandButton(resource: Int) {
        if (resource == 0) {
            if (commandButton!!.setVisible(false)) {
                attachButtons!!.updatePivot()
            }
            return
        }
        if (commandButton!!.setVisible(true)) {
            attachButtons!!.updatePivot()
        }
        if (lastCmdResource != resource) {
            lastCmdResource = resource
            commandButton!!.setImageResource(resource)
        }
    }

    private fun onCommandClick() {
        inputView!!.setInput("/", true, true)
        Keyboard.show(inputView)
    }

    fun showKeyboard() {
        if (isFocused()) {
            Keyboard.show(inputView)
        }
    }

    fun hideAllKeyboards() {
        hideSoftwareKeyboard()
        closeCommandsKeyboard(false)
        closeEmojiKeyboard(true)
    }

    fun hideKeyboard(personal: Boolean) {
        if (personal && commandsShown) {
            commandsState = false
        }
        closeCommandsKeyboardImpl(true, false)
    }

    private var commandsShown = false

    // Commands
    var commandsState: Boolean = false
        private set
    private var commandsMessageId: Long = 0
    private var commandsKeyboard: ReplyMarkupShowKeyboard? = null
    private var keyboardWrapper: ScrollView? = null
    var keyboardLayout: CommandKeyboardLayout? = null
        private set

    private fun setCommandsShown(commandsShown: Boolean) {
        if (this.commandsShown != commandsShown) {
            this.commandsShown = commandsShown
            updateBottomWrapOffset()
        }
    }

    private fun toggleCommandsKeyboard() {
        if (commandsShown) {
            closeCommandsKeyboard(true)
        } else {
            openCommandsKeyboard(true)
        }
    }

    private fun closeCommandsKeyboard(byUserEvent: Boolean) {
        closeCommandsKeyboardImpl(false, byUserEvent)
    }

    private fun closeCommandsKeyboardImpl(destroy: Boolean, byUserEvent: Boolean) {
        if (commandsShown) {
            if (keyboardWrapper != null) {
                keyboardWrapper!!.setVisibility(View.GONE)
            }

            // updateButtonsY();
            if (commandsState && isFocused()) {
                keyboardLayout!!.showKeyboard(inputView)
            }

            setCommandsShown(false)
            if (destroy) {
                updateCommandButton(R.drawable.deproko_baseline_bots_command_26)
            } else {
                updateCommandButton(R.drawable.deproko_baseline_bots_keyboard_26)
                if (byUserEvent) {
                    Settings.instance().onRequestKeyboardClose(tdlib!!.id(), chat!!.id, chat!!.replyMarkupMessageId, true)
                }
            }
        } else if (destroy) {
            updateCommandButton(R.drawable.deproko_baseline_bots_command_26)
        }
    }

    private fun forceCloseCommandsKeyboard() {
        if (commandsShown) {
            if (keyboardWrapper != null) {
                keyboardWrapper!!.setVisibility(View.GONE)
            }
            setCommandsShown(false)
            updateCommandButton(R.drawable.deproko_baseline_bots_keyboard_26)
        }
    }

    private fun openCommandsKeyboard(byUserEvent: Boolean) {
        openCommandsKeyboard(commandsMessageId, commandsKeyboard!!, true, byUserEvent)
    }

    private fun updateCommandKeyboardHeight() {
        if (keyboardWrapper != null && keyboardLayout != null) {
            Views.setLayoutHeight(keyboardWrapper, min(Keyboard.getSize(), keyboardLayout!!.getSize()) + extraBottomInsetWithoutIme)
            Views.applyBottomInset(keyboardWrapper, extraBottomInsetWithoutIme)
        }
    }

    private fun openCommandsKeyboard(messageId: Long, keyboard: ReplyMarkupShowKeyboard, force: Boolean, byUserEvent: Boolean) {
        if (keyboardLayout == null) {
            keyboardWrapper = ScrollView(context())
            ViewSupport.setThemedBackground(keyboardWrapper, ColorId.chatKeyboard, this)

            keyboardLayout = CommandKeyboardLayout(context())
            keyboardLayout!!.setThemeProvider(this)
            keyboardLayout!!.setCallback(this)

            keyboardWrapper!!.addView(keyboardLayout)
            keyboardWrapper!!.setLayoutParams(
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    min(Keyboard.getSize(), keyboardLayout!!.getSize()) + extraBottomInsetWithoutIme
                )
            )
            Views.applyBottomInset(keyboardWrapper, extraBottomInsetWithoutIme)

            bottomWrap!!.addView(keyboardWrapper)
            contentView!!.getViewTreeObserver().addOnPreDrawListener(keyboardLayout)
            keyboardWrapper!!.setVisibility(View.GONE)
        }

        if (commandsMessageId != messageId) {
            commandsMessageId = messageId
            commandsKeyboard = keyboard
            keyboardLayout!!.setKeyboard(keyboard)
            updateCommandKeyboardHeight()
        }

        if (byUserEvent) {
            Settings.instance().onRequestKeyboardClose(tdlib!!.id(), getChatId(), messageId, false)
        }

        if (!force && (!keyboard.isPersonal || Settings.instance().shouldKeepKeyboardClosed(tdlib!!.id(), getChatId(), messageId))) {
            keyboardWrapper!!.setVisibility(View.GONE)
            updateCommandButton(R.drawable.deproko_baseline_bots_keyboard_26)
            // updateButtonsY();
            return
        }

        if (emojiShown) {
            commandsState = emojiState
            forceCloseEmojiKeyboard()
        } else {
            commandsState = getKeyboardState()
        }

        keyboardWrapper!!.setVisibility(View.VISIBLE)
        // updateButtonsY();
        setCommandsShown(true)

        if (commandsState) {
            updateCommandButton(R.drawable.baseline_keyboard_24)
            keyboardLayout!!.hideKeyboard(inputView)
        } else {
            updateCommandButton(R.drawable.baseline_direction_arrow_down_24)
        }
    }

    private var prevButtonsY = 0f

    private fun updateButtonsY() {
        val y = bottomWrap!!.getTop() + (if (inputView != null) inputView!!.getBottom() else Screen.dp(49f)) - Screen.dp(49f) - this.keyboardOffset

        sendButton!!.setTranslationY(y)
        emojiButton!!.setTranslationY(y)
        attachButtons!!.setTranslationY(y)
        messageSenderButton!!.setTranslationY(y)

        if (prevButtonsY != y) {
            prevButtonsY = y
            onMessagesFrameChanged()
            updateRecordLayout()
        }
    }

    private var scheduledKeyboardMessageId: Long = 0
    private var scheduledKeyboard: ReplyMarkupShowKeyboard? = null

    private fun clearScheduledKeyboard() {
        scheduledKeyboardMessageId = 0
        scheduledKeyboard = null
    }

    fun showKeyboard(messageId: Long, markup: ReplyMarkupShowKeyboard) {
        if (false && !isFocused() && commandsMessageId != messageId) { // Смотрится как говно
            scheduledKeyboardMessageId = messageId
            scheduledKeyboard = markup
        } else {
            openCommandsKeyboard(messageId, markup, false, false)
        }
    }

    override fun onCommandPressed(command: String?) {
        pickDateOrProceed(
            newSendOptions(),
            SimpleSendCallback { sendOptions: MessageSendOptions?, disableMarkdown: Boolean -> sendText(command, false, true, false, sendOptions) }
        )
    }

    override fun onRequestPoll(oneTime: Boolean, forceQuiz: Boolean, forceRegular: Boolean) {
        if (chat != null && tdlib!!.canSendPolls(chat!!.id)) {
            val c = CreatePollController(context, tdlib)
            c.setArguments(
                CreatePollController.Args(
                    chat!!.id,
                    getMessageTopicId(this.currentReplyId),
                    getInputSuggestedPostInfo(null),
                    this,
                    forceQuiz,
                    forceRegular
                )
            )
            navigateTo(c)
        }
    }

    override fun onSendPoll(
        context: CreatePollController?,
        chatId: Long,
        topicId: MessageTopic?,
        poll: InputMessagePoll?,
        sendOptions: MessageSendOptions,
        after: RunnableData<TdApi.Message?>?
    ): Boolean {
        if (getChatId() == chatId && matchesTopic(topicId)) {
            send(poll, true, sendOptions, after)
            return true
        }
        return false
    }

    override fun areScheduledOnly(context: CreatePollController?): Boolean {
        return areScheduledOnly()
    }

    override fun provideChatList(context: CreatePollController?): ChatList? {
        return openedFromChatList
    }

    fun chatList(): ChatList? {
        return openedFromChatList
    }

    override fun onRequestContact(oneTime: Boolean) {
        if (chat != null && isPrivate(getChatId())) {
            val builder = AlertDialog.Builder(context(), Theme.dialogTheme())
            builder.setTitle(Lang.getString(R.string.ShareYourPhoneNumberTitle))
            builder.setMessage(Lang.getString(R.string.ShareYourPhoneNumberDesc, tdlib!!.chatTitle(getChatId())))
            builder.setPositiveButton(Lang.getOK(), DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                shareMyContact(true)
                if (oneTime) {
                    onDestroyCommandKeyboard()
                }
            })
            builder.setNegativeButton(
                Lang.getString(R.string.Cancel),
                DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int -> dialog!!.dismiss() })
            showAlert(builder)
        }
    }

    override fun onRequestLocation(oneTime: Boolean) {
        if (chat != null && isPrivate(chat!!.id)) {
            val builder = AlertDialog.Builder(context(), Theme.dialogTheme())
            builder.setTitle(Lang.getString(R.string.ShareYourLocation))
            builder.setMessage(Lang.getString(R.string.ShareYouLocationInfo))
            builder.setPositiveButton(Lang.getOK(), DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int -> shareCurrentLocation(oneTime) })
            builder.setNegativeButton(
                Lang.getString(R.string.Cancel),
                DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int -> dialog!!.dismiss() })
            showAlert(builder)
        }
    }

    private var googleClient: GoogleApiClient? = null

    private fun closeGoogleClient() {
        if (googleClient != null) {
            try {
                googleClient!!.disconnect()
            } catch (t: Throwable) {
                Log.w("GoogleApiClient throws", t)
            }
            googleClient = null
        }
    }

    private var currentShareLocationDestroyKeyboard = false
    private var currentShareLocationChatId: Long = 0

    @Suppress("deprecation")
    private fun shareCurrentLocation(destroyKeyboard: Boolean) {
        currentShareLocationDestroyKeyboard = destroyKeyboard
        currentShareLocationChatId = getChatId()

        if (context().checkLocationPermissions(false) != PackageManager.PERMISSION_GRANTED) {
            context().requestLocationPermission(false, false, null)
            return
        }

        try {
            if (googleClient == null) {
                val b = GoogleApiClient.Builder(context())
                b.addApi(LocationServices.API)
                googleClient = b.build()
                googleClient!!.connect()
            }

            val builder = LocationSettingsRequest.Builder()
                .addLocationRequest(LocationRequest.create())
                .setAlwaysShow(true)
            val request = builder.build()
            val result =
                LocationServices.SettingsApi.checkLocationSettings(googleClient!!, request)

            result.setResultCallback(ResultCallback { result1: LocationSettingsResult? ->
                val status = result1!!.getStatus()
                //final LocationSettingsStates state = result.getLocationSettingsStates();
                when (status.getStatusCode()) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                        try {
                            status.startResolutionForResult(context(), Intents.ACTIVITY_RESULT_RESOLUTION)
                        } catch (t: Throwable) {
                            getCustomCurrentLocation(destroyKeyboard, true, false)
                        }
                    }

                    LocationSettingsStatusCodes.SUCCESS -> {
                        getCurrentLocation(destroyKeyboard, googleClient)
                    }

                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                        getCurrentLocation(destroyKeyboard, googleClient)
                    }

                    else -> {
                        getCurrentLocation(destroyKeyboard, googleClient)
                    }
                }
            })
        } catch (t: Throwable) {
            shareCurrentLocation(destroyKeyboard, null)
        }
    }

    fun sendPickedLocation(location: TdApi.Location?, heading: Int, sendOptions: MessageSendOptions) {
        if (getChatId() == currentShareLocationChatId) {
            send(InputMessageLocation(location, 0, heading, 0), true, sendOptions, null)
            if (currentShareLocationDestroyKeyboard) {
                onDestroyCommandKeyboard()
            }
        }
    }

    private fun shareCurrentLocation(destroyKeyboard: Boolean, location: Location?) {
        if (location == null || getChatId() != currentShareLocationChatId) {
            return
        }
        send(
            InputMessageLocation(
                TdApi.Location(location.getLatitude(), location.getLongitude(), location.getAccuracy().toDouble()),
                0,
                U.getHeading(location),
                0
            ), true, newSendOptions(), null
        )
        if (destroyKeyboard) {
            onDestroyCommandKeyboard()
        }
    }

    private fun getCustomCurrentLocation(destroyKeyboard: Boolean, byError: Boolean, byTimeout: Boolean) {
        if (getChatId() == currentShareLocationChatId) {
            val builder = AlertDialog.Builder(context(), Theme.dialogTheme())
            builder.setTitle(Lang.getString(R.string.AppName))
            builder.setMessage(Lang.getString(R.string.DetectLocationError))
            builder.setPositiveButton(Lang.getOK(), DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int -> dialog!!.dismiss() })
            builder.setNegativeButton(
                Lang.getString(R.string.ShareYouLocationUnableManually),
                DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int -> pickCustomCurrentLocation(destroyKeyboard) })
            showAlert(builder)
        }
    }

    private fun pickCustomCurrentLocation(destroyKeyboard: Boolean) {
        val mediaLayout = MediaLayout(this)
        mediaLayout.init(MediaLayout.MODE_LOCATION, this)
        mediaLayout.show()
        // TODO run MediaLayout with only map available
    }

    private fun getCurrentLocation(destroyKeyboard: Boolean, client: GoogleApiClient?) {
        if (client != null && USE_GOOGLE_LOCATION) {
            getCurrentLocationViaGoogleApiClient(destroyKeyboard, client)
        } else {
            shareCurrentLocationViaManager(destroyKeyboard)
        }
    }

    @Suppress("deprecation")
    private fun getCurrentLocationViaGoogleApiClient(destroyKeyboard: Boolean, client: GoogleApiClient) {
        val timeout = arrayOfNulls<CancellableRunnable>(1)
        val sent = BooleanArray(1)
        val listener = LocationListener { location: Location? ->
            timeout[0]!!.cancel()
            if (!sent[0]) {
                sent[0] = true
                shareCurrentLocation(destroyKeyboard, location!!)
            }
        }
        timeout[0] = object : CancellableRunnable() {
            public override fun act() {
                if (!sent[0]) {
                    sent[0] = true
                    try {
                        LocationServices.FusedLocationApi.removeLocationUpdates(googleClient!!, listener)
                    } catch (t: Throwable) {
                        Log.w("Error removeLocationUpdates", t)
                    }
                    var location: Location? = null
                    try {
                        location = LocationServices.FusedLocationApi.getLastLocation(client)
                    } catch (ignored: SecurityException) {
                    } catch (t: Throwable) {
                        Log.w("getLastLocation error", t)
                    }
                    if (location == null && USE_LAST_KNOWN_LOCATION) {
                        location = U.getLastKnownLocation(context(), false)
                    }
                    if (location != null) {
                        shareCurrentLocation(destroyKeyboard, location)
                    } else {
                        getCustomCurrentLocation(destroyKeyboard, false, true)
                    }
                }
            }
        }
        UI.post(timeout[0], LOCATION_MAX_WAIT_TIME)
        try {
            val request =
                LocationRequest.create().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setExpirationDuration(LOCATION_MAX_WAIT_TIME).setNumUpdates(1)
                    .setMaxWaitTime(5000L)
            LocationServices.FusedLocationApi.requestLocationUpdates(googleClient!!, request, listener)
        } catch (ignored: SecurityException) {
            sent[0] = true
            shareCurrentLocationViaManager(destroyKeyboard)
        } catch (t: Throwable) {
            Log.w("requestLocationUpdates error", t)
            sent[0] = true
            shareCurrentLocationViaManager(destroyKeyboard)
        }
    }

    private fun shareCurrentLocationViaManager(destroyKeyboard: Boolean) {
        try {
            val manager = context().getSystemService(Context.LOCATION_SERVICE) as LocationManager

            val timeout = arrayOfNulls<CancellableRunnable>(1)
            val sent = BooleanArray(1)
            val listener: android.location.LocationListener = object : android.location.LocationListener {
                override fun onLocationChanged(location: Location) {
                    timeout[0]!!.cancel()
                    try {
                        manager.removeUpdates(this)
                    } catch (ignored: SecurityException) {
                    } catch (t: Throwable) {
                        Log.w("removeUpdates failed. Probable resource leak", t)
                    }
                    if (!sent[0]) {
                        sent[0] = true
                        shareCurrentLocation(destroyKeyboard, location)
                    }
                }

                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

                override fun onProviderEnabled(provider: String) {}

                override fun onProviderDisabled(provider: String) {}
            }
            timeout[0] = object : CancellableRunnable() {
                public override fun act() {
                    if (!sent[0]) {
                        sent[0] = true
                        val location = if (USE_LAST_KNOWN_LOCATION) U.getLastKnownLocation(context(), true) else null
                        if (location != null) {
                            shareCurrentLocation(destroyKeyboard, location)
                        } else {
                            getCustomCurrentLocation(destroyKeyboard, false, true)
                        }
                    }
                }
            }
            UI.post(timeout[0], LOCATION_MAX_WAIT_TIME)
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, 0f, listener)
            manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1, 0f, listener)
        } catch (ignored: SecurityException) {
            getCustomCurrentLocation(destroyKeyboard, true, false)
        } catch (t: Throwable) {
            Log.w("Error occured", t)
            getCustomCurrentLocation(destroyKeyboard, true, false)
        }
    }

    override fun onDestroyCommandKeyboard() {
        clearScheduledKeyboard()
        closeCommandsKeyboardImpl(true, false)
        updateCommandButton(R.drawable.deproko_baseline_bots_command_26)
        if (commandsMessageId != 0L) {
            botHelper!!.onDestroyKeyboard(commandsMessageId)
        }
        setCustomBotPlaceholder(null)
    }

    private var customBotPlaceholder: String? = null

    fun setCustomBotPlaceholder(customPlaceholder: String?) {
        if (!this.customBotPlaceholder.equalsOrBothEmpty(customPlaceholder)) {
            this.customBotPlaceholder = customPlaceholder
            updateInputHint()
        }
    }

    private var customCaptionPlaceholder: String? = null

    fun setCustomCaptionPlaceholder(customPlaceholder: String?) {
        if (!this.customCaptionPlaceholder.equalsOrBothEmpty(customPlaceholder)) {
            this.customCaptionPlaceholder = customPlaceholder
            updateInputHint()
        }
    }

    private val customInputPlaceholder: String?
        get() {
            if (!isEmpty(customBotPlaceholder)) {
                return customBotPlaceholder
            } else if (!isEmpty(customCaptionPlaceholder)) {
                return customCaptionPlaceholder
            }
            return null
        }

    protected override fun onTranslationChanged(newTranslationX: Float) {
        context().reactionsOverlayManager().setControllerTranslationX(newTranslationX.toInt())
    }

    override fun onResizeCommandKeyboard(size: Int) {
        // updateButtonsY();
    }

    val horizontalInputPadding: Int
        // Silent mode
        get() = attachButtons!!.getVisibleChildrenWidth() + (if (canSelectSender()) Screen.dp(47f) else 0)

    private fun updateSilentButton(visible: Boolean) {
        if (Config.NEED_SILENT_BROADCAST) {
            commandButton!!.setTranslationX((if (visible) 0 else silentButton!!.getLayoutParams().width).toFloat())
            if (silentButton!!.setVisible(visible)) {
                attachButtons!!.updatePivot()
            }
            if (visible) {
                silentButton!!.forceState(tdlib!!.chatDefaultDisableNotifications(getChatId()))
            }
        }
    }

    fun obtainSilentMode(): Boolean {
        return tdlib!!.chatDefaultDisableNotifications(getChatId())
    }

    // pinned messages
    private fun pinUnpinMessage(m: TGMessage, pin: Boolean) {
        val chatId = m.message.chatId
        if (chatId == 0L) {
            return
        }
        if (!pin) {
            m.iterate(RunnableData { message: TdApi.Message? ->
                if (message!!.isPinned != pin) {
                    val function = if (pin) PinChatMessage(getChatId(), message.id, false, false) else UnpinChatMessage(getChatId(), message.id)
                    tdlib!!.send<TdApi.Ok?>(function, tdlib!!.typedOkHandler())
                }
            }, false)
            return
        }
        if (tdlib!!.isSelfChat(getChatId())) {
            tdlib!!.send<TdApi.Ok?>(PinChatMessage(chatId, m.smallestId, false, false), tdlib!!.typedOkHandler())
            return
        }
        val hintRes: Int
        val item: ListItem?
        when (getType(chatId)) {
            TdApi.ChatTypePrivate.CONSTRUCTOR, TdApi.ChatTypeSecret.CONSTRUCTOR -> {
                hintRes = R.string.PinMessageInChat
                item = ListItem(
                    ListItem.TYPE_CHECKBOX_OPTION,
                    R.id.btn_notifyMembers,
                    0,
                    Lang.getStringBold(R.string.PinMessageOther, tdlib!!.chatTitle(chatId, false, true)),
                    true
                )
            }

            ChatTypeBasicGroup.CONSTRUCTOR, ChatTypeSupergroup.CONSTRUCTOR -> {
                val isChannel = tdlib!!.isChannel(getChatId())
                hintRes = if (isChannel) R.string.PinMessageInThisChannel else R.string.PinMessageInThisGroup
                item = ListItem(ListItem.TYPE_CHECKBOX_OPTION, R.id.btn_notifyMembers, 0, if (isChannel) R.string.PinNotify2 else R.string.PinNotify, true)
            }

            else -> throw RuntimeException()
        }
        showSettings(
            SettingsWrapBuilder(R.id.btn_messagePin)
                .addHeaderItem(
                    ListItem(ListItem.TYPE_INFO, 0, 0, hintRes)
                ).setIntDelegate(SettingsIntDelegate { id: Int, result: SparseIntArray? ->
                    val checkbox = result!!.get(R.id.btn_notifyMembers) != 0
                    val isUserChat = isUserChat(chatId)
                    val disableNotification = !isUserChat && !checkbox
                    val onlyForSelf = isUserChat && !checkbox
                    tdlib!!.send<TdApi.Ok?>(PinChatMessage(chatId, m.smallestId, disableNotification, onlyForSelf), tdlib!!.typedOkHandler())
                })
                .setRawItems(arrayOf<ListItem>(item)).setSaveStr(R.string.Pin)
        )
    }

    fun showHidePinnedMessage(show: Boolean, message: TdApi.Message?) {
        if (show) {
            pinnedMessagesBar!!.setCollapseButtonVisible(false)
            pinnedMessagesBar!!.setContextChatId(if (getChatId() != this.headerChatId) this.headerChatId else 0)
            pinnedMessagesBar!!.setMessage(tdlib, message)
        }
        topBar!!.setItemVisible(pinnedMessagesItem, show, isFocused())
    }

    fun showHidePinnedMessages(show: Boolean, messageList: MessageListManager?) {
        if (arePinnedMessages()) {
            if (!show) navigateBack()
            return
        }
        if (show) {
            pinnedMessagesBar!!.setCollapseButtonVisible(true)
            pinnedMessagesBar!!.setContextChatId(if (getChatId() != this.headerChatId) this.headerChatId else 0)
            pinnedMessagesBar!!.setMessageList(messageList)
        }
        topBar!!.setItemVisible(pinnedMessagesItem, show, isFocused())
    }

    private fun dismissPinnedMessage() {
        if (!canPinAnyMessage(false)) { // Just hide locally.
            manager!!.dismissPinnedMessage()
            return
        }
        val pinnedCount = manager!!.getPinnedMessageCount()
        if (pinnedCount == ListManager.COUNT_UNKNOWN) {
            return
        }
        showOptions(
            Options.Builder()
                .info(if (pinnedCount > 1) Lang.pluralBold(R.string.UnpinXMessages, pinnedCount.toLong()) else null)
                .item(
                    if (canPinAnyMessage(false)) OptionItem.Builder()
                        .id(R.id.btn_unpinMessage)
                        .name(Lang.getString(if (pinnedCount == 1) R.string.UnpinMessage else R.string.UnpinMessagesConfirm))
                        .color(OptionColor.RED)
                        .icon(R.drawable.deproko_baseline_pin_undo_24)
                        .build() else null
                )
                .item(
                    if (!this.isSelfChat) OptionItem.Builder()
                        .id(R.id.btn_dismissForSelf)
                        .name(R.string.HideForYourself)
                        .icon(R.drawable.baseline_close_24)
                        .build() else null
                )
                .cancelItem()
                .build(),
            OptionDelegate { itemView: View?, id: Int ->
                if (id == R.id.btn_unpinMessage) {
                    tdlib!!.send<TdApi.Ok?>(UnpinAllChatMessages(getChatId()), tdlib!!.typedOkHandler())
                } else if (id == R.id.btn_dismissForSelf) {
                    manager.dismissPinnedMessage()
                }
                true
            }
        )
    }

    fun onMessageChanged(chatId: Long, messageId: Long, content: MessageContent) {
        if (this.isEditingMessage && editContext!!.checkMessage(chatId, messageId)) {
            if (!editContext!!.messageProperties!!.canBeEdited) {
                closeEdit(false)
            } else {
                val oldContent = editContext!!.existingMessage!!.content
                editContext!!.existingMessage!!.content = content
                updateReplyBarVisibility(true)
                // This is required because we work with a reference from TGMessage, not a copy.
                editContext!!.existingMessage!!.content = oldContent
            }
        }
    }

    fun onInlineTranslationChanged(chatId: Long, messageId: Long, text: TdApi.FormattedText?) {
    }

    fun onMessagesDeleted(chatId: Long, messageIds: LongArray?) {
        if (this.isEditingMessage && editContext!!.existingMessage!!.chatId == chatId && indexOf(messageIds, editContext!!.existingMessage!!.id) != -1) {
            closeEdit(false)
        }
    }

    override fun onMessageThreadReplyCountChanged(chatId: Long, topicId: MessageTopic?, replyCount: Int) {
        if (messageThread != null && messageThread!!.belongsTo(chatId, topicId)) {
            updateMessageThreadSubtitle()
        }
    }

    override fun onMessageThreadReadInbox(chatId: Long, topicId: MessageTopic?, lastReadInboxMessageId: Long, remainingUnreadCount: Int) {
        if (messageThread != null && messageThread!!.belongsTo(chatId, topicId)) {
            updateCounters(true)
        }
    }

    override fun onMessageThreadDeleted(chatId: Long, topicId: MessageTopic?) {
        if (messageThread != null && messageThread!!.belongsTo(chatId, topicId)) {
            forceFastAnimationOnce()
            navigateBack()
        }
    }

    // Live location
    private var liveLocationView: View? = null
    private var liveLocationItem: CollapseListView.Item? = null
    private var liveLocation: LiveLocationHelper? = null

    override fun onBeforeVisibilityStateChanged(helper: LiveLocationHelper?, isVisible: Boolean, willAnimate: Boolean): Boolean {
        return isFocused()
    }

    override fun onAfterVisibilityStateChanged(helper: LiveLocationHelper?, isVisible: Boolean, willAnimate: Boolean) {
    }

    override fun onApplyVisibility(helper: LiveLocationHelper?, isVisible: Boolean, visibilityFactor: Float, finished: Boolean) {
        topBar!!.forceItemVisibility(liveLocationItem, isVisible, visibilityFactor)
    }

    // Action bar
    private fun dismissActionBar() {
        tdlib!!.send<TdApi.Ok?>(RemoveChatActionBar(getChatId()), tdlib!!.typedOkHandler())
    }

    private fun needActionBar(): Boolean {
        return !(inPreviewMode || isInForceTouchMode() || areScheduledOnly() || messageThread != null)
    }

    private fun newAddContactItem(chatId: Long): TopBarView.Item {
        return TopBarView.Item(R.id.btn_addContact, R.string.AddContact, View.OnClickListener { v: View? ->
            tdlib!!.ui().addContact(this, tdlib!!.chatUser(chatId))
        })
    }

    private fun newUnarchiveItem(chatId: Long): TopBarView.Item {
        return TopBarView.Item(R.id.btn_unarchiveChat, R.string.UnarchiveUnmute, View.OnClickListener { v: View? ->
            tdlib!!.send<TdApi.Ok?>(AddChatToList(chatId, ChatListMain()), tdlib!!.typedOkHandler())
            val settings = tdlib!!.chatSettings(chatId)
            if (settings != null) {
                val newSettings = ChatNotificationSettings(
                    true, 0,
                    settings.useDefaultSound, settings.soundId,
                    settings.useDefaultShowPreview, settings.showPreview,
                    settings.useDefaultMuteStories, settings.muteStories,
                    settings.useDefaultStorySound, settings.storySoundId,
                    settings.useDefaultShowStoryPoster, settings.showStoryPoster,
                    settings.useDefaultDisablePinnedMessageNotifications, settings.disablePinnedMessageNotifications,
                    settings.useDefaultDisableMentionNotifications, settings.disableMentionNotifications
                )
                tdlib!!.send<TdApi.Ok?>(SetChatNotificationSettings(chatId, newSettings), tdlib!!.typedOkHandler())
            }
        })
    }

    private fun newReportItem(chatId: Long, isBlock: Boolean): TopBarView.Item? {
        return TopBarView.Item(R.id.btn_reportChat, if (isBlock) R.string.BlockContact else R.string.ReportSpam, View.OnClickListener { v: View? ->
            showSettings(
                SettingsWrapBuilder(R.id.btn_reportSpam)
                    .addHeaderItem(ListItem(ListItem.TYPE_INFO, 0, 0, Lang.getStringBold(R.string.ReportChatSpam, chat!!.title), false))
                    .setRawItems(
                        if (this.chatUserId != 0L) arrayOf<ListItem>(
                            ListItem(ListItem.TYPE_CHECKBOX_OPTION, R.id.btn_reportSpam, 0, R.string.ReportSpam, true),
                            ListItem(ListItem.TYPE_CHECKBOX_OPTION, R.id.btn_removeChatFromList, 0, R.string.DeleteChat, true),
                            ListItem(ListItem.TYPE_CHECKBOX_OPTION, R.id.btn_blockSender, 0, R.string.BlockUser, true),
                        ) else arrayOf<ListItem>(
                            ListItem(ListItem.TYPE_CHECKBOX_OPTION, R.id.btn_reportSpam, 0, R.string.ReportSpam, true),
                            ListItem(ListItem.TYPE_CHECKBOX_OPTION, R.id.btn_removeChatFromList, 0, R.string.DeleteChat, true)
                        )
                    )
                    .setIntDelegate(SettingsIntDelegate { id: Int, result: SparseIntArray? ->
                        if (id != R.id.btn_reportSpam || chatId != getChatId() || !isFocused()) {
                            return@SettingsIntDelegate
                        }
                        val reportSpam = result!!.get(R.id.btn_reportSpam) != 0
                        val deleteChat = result.get(R.id.btn_removeChatFromList) != 0
                        val blockSender = result.get(R.id.btn_blockSender) != 0

                        if (!reportSpam && !deleteChat && !blockSender) {
                            return@SettingsIntDelegate
                        }

                        if (blockSender) {
                            tdlib!!.blockSender(tdlib!!.sender(chat!!.id), BlockListMain(), tdlib!!.okHandler())
                        }

                        if (reportSpam) {
                            tdlib!!.send<ReportChatResult?>(ReportChat(getChatId(), null, null, null), tdlib!!.errorHandler<ReportChatResult?>())
                        }
                        if (deleteChat) {
                            deleteAndLeave()
                        }
                    })
                    .setSaveStr(R.string.Done)
                    .setSaveColorId(ColorId.textNegative)
            )
        }).setIsNegative()
    }

    private fun checkJoinRequests(info: ChatJoinRequestsInfo?) {
        if (!needActionBar()) return

        if (info == null || info.totalCount == 0 || (chat != null && tdlib!!.settings().isRequestsDismissed(chat!!.id, info))) {
            topBar!!.setItemVisible(requestsItem, false, isFocused())
        } else {
            if (info.totalCount > 0) {
                tdlib!!.settings().restoreRequests(chat!!.id, true)
            }

            requestsView!!.setInfo(info, isFocused())
            topBar!!.setItemVisible(requestsItem, true, isFocused())
        }
    }

    private fun checkActionBar() {
        if (!needActionBar()) return

        val items: MutableList<TopBarView.Item?> = ArrayList<TopBarView.Item?>()

        val chatId = getChatId()
        val chat = tdlib!!.chat(chatId)
        val actionBar = if (chat != null) chat.actionBar else null
        if (actionBar != null) {
            when (actionBar.getConstructor()) {
                TdApi.ChatActionBarAddContact.CONSTRUCTOR -> {
                    items.add(newAddContactItem(chatId))
                }

                ChatActionBarReportSpam.CONSTRUCTOR -> {
                    val reportSpam = actionBar as ChatActionBarReportSpam
                    items.add(newReportItem(chatId, false))
                    if (reportSpam.canUnarchive) {
                        items.add(newUnarchiveItem(chatId))
                    }
                }

                ChatActionBarReportAddBlock.CONSTRUCTOR -> {
                    val reportAddBlock = actionBar as ChatActionBarReportAddBlock
                    items.add(newReportItem(chatId, true))
                    items.add(newAddContactItem(chatId))
                    if (reportAddBlock.canUnarchive) {
                        items.add(newUnarchiveItem(chatId))
                    }
                }

                TdApi.ChatActionBarInviteMembers.CONSTRUCTOR -> {
                    items.add(TopBarView.Item(R.id.btn_invite, R.string.AddMember, View.OnClickListener { v: View? ->
                        val c = ContactsController(context, tdlib)
                        c.initWithMode(ContactsController.MODE_ADD_MEMBER)
                        c.setAllowBots(true)
                        c.setArguments(ContactsController.Args(object : SenderPickerDelegate {
                            override fun onSenderPick(context: ContactsController, view: View?, senderId: MessageSender?): Boolean {
                                if (tdlib!!.isSelfSender(senderId)) {
                                    return false
                                }
                                val sender = senderId ?: return false

                                tdlib!!.setChatMemberStatus(
                                    chat!!.id,
                                    sender,
                                    ChatMemberStatusMember(),
                                    null,
                                    ChatMemberStatusChangeCallback { ok: Boolean, error: TdApi.Error?, failedToAddMember: FailedToAddMember? ->
                                        runOnUiThreadOptional(
                                            Runnable {
                                                if (!ok && error != null) {
                                                    context.context()
                                                        .tooltipManager()
                                                        .builder(view)
                                                        .show(context, tdlib, R.drawable.baseline_error_24, TD.toErrorString(error))
                                                } else {
                                                    context.navigateBack()
                                                }
                                            })
                                    })

                                return true
                            }
                        }))
                        c.setChatTitle(R.string.AddMember, chat!!.title)
                        navigateTo(c)
                    }))
                }

                TdApi.ChatActionBarSharePhoneNumber.CONSTRUCTOR -> {
                    items.add(TopBarView.Item(R.id.btn_shareMyContact, R.string.SharePhoneNumber, View.OnClickListener { v: View? ->
                        val user = tdlib!!.myUser()
                        if (user != null) {
                            showOptions(
                                TD.getUserName(user) + ", " + Strings.formatPhone(user.phoneNumber),
                                intArrayOf(R.id.btn_shareMyContact, R.id.btn_cancel),
                                arrayOf<String?>(
                                    Lang.getString(R.string.SharePhoneNumberAction), Lang.getString(R.string.Cancel)
                                ),
                                intArrayOf(OptionColor.BLUE, OptionColor.NORMAL),
                                intArrayOf(R.drawable.baseline_contact_phone_24, R.drawable.baseline_cancel_24),
                                OptionDelegate { itemView: View?, id1: Int ->
                                    if (id1 == R.id.btn_shareMyContact) {
                                        tdlib!!.send<TdApi.Ok?>(SharePhoneNumber(tdlib!!.chatUserId(chatId)), tdlib!!.typedOkHandler())
                                    }
                                    true
                                })
                        }
                    }))
                }

                ChatActionBarJoinRequest.CONSTRUCTOR -> {
                    val joinRequest = actionBar as ChatActionBarJoinRequest
                }

                else -> {
                    assertChatActionBar_eedc82ed()
                    throw unsupported(actionBar)
                }
            }
        }
        if (isSecret(chatId)) {
            val secretChat = tdlib!!.chatToSecretChat(chatId)
            if (secretChat != null && secretChat.state.getConstructor() == TdApi.SecretChatStateClosed.CONSTRUCTOR) {
                items.add(TopBarView.Item(R.id.btn_delete, R.string.DeleteAndLeave, View.OnClickListener { v: View? ->
                    if (manager!!.isTotallyEmpty()) {
                        deleteAndLeave()
                    } else {
                        tdlib!!.ui().showDeleteChatConfirm(this, getChatId())
                    }
                }).setNoDismiss().setIsNegative())
            }
        }
        if (!items.isEmpty()) {
            actionView!!.setItems(*items.toTypedArray<TopBarView.Item?>())
        }
        topBar!!.setItemVisible(actionItem, !items.isEmpty(), isFocused())
    }

    // Callback shit
    private var currentSwitchPmButton: InlineResultButton? = null

    private fun processSwitchPm(button: InlineResultButton) {
        currentSwitchPmButton = button
        tdlib!!.sendBotStartMessage(button.getUserId(), getChatId(), button.botStartParameter())
    }

    fun switchBackToSourcePmIfNeeded(button: InlineKeyboardButtonTypeSwitchInline) {
        if (currentSwitchPmButton != null) {
            val chat = tdlib!!.chat(currentSwitchPmButton!!.getSourceChatId())
            val user = tdlib!!.cache().user(currentSwitchPmButton!!.getUserId())
            if (chat != null && user != null) {
                tdlib!!.ui().openChat(this, chat, ChatOpenParameters().shareItem(TGSwitchInline(user.primaryUsername(), button.query)))
            }
        }
    }

    private fun clearSwitchPmButton() {
        currentSwitchPmButton = null
    }

    fun checkSwitchPm(msg: TdApi.Message) {
        if (currentSwitchPmButton != null && msg.replyMarkup != null && msg.replyMarkup!!.getConstructor() == ReplyMarkupInlineKeyboard.CONSTRUCTOR) {
            val keyboard = msg.replyMarkup as ReplyMarkupInlineKeyboard
            for (row in keyboard.rows) {
                for (button in row) {
                    if (button.type.getConstructor() == InlineKeyboardButtonTypeSwitchInline.CONSTRUCTOR) {
                        switchBackToSourcePmIfNeeded((button.type as InlineKeyboardButtonTypeSwitchInline?)!!)
                        break
                    }
                }
            }
        }
    }

    fun onSwitchPm(button: InlineResultButton) {
        tdlib!!.sendBotStartMessage(button.getUserId(), getChatId(), button.botStartParameter())
        inputView!!.setText("")
    }

    fun openGame(ownerUserId: Long, game: Game?, url: String?, message: TdApi.Message?) {
        val user = tdlib!!.cache().user(ownerUserId)
        val controller = GameController(context, tdlib)
        controller.setArguments(
            GameController.Args(
                if (user != null) user.id else 0,
                game,
                if (user != null) "@" + user.primaryUsername() else "Game",
                url,
                message,
                this
            )
        )
        /*PopupLayout popupLayout = new PopupLayout(getContext());
    popupLayout.init(true);
    popupLayout.showSimplePopupView(controller.getWrap(), Screen.currentHeight());*/
        navigateTo(controller)
    }

    fun switchInline(viaBotUserId: Long, switchInline: InlineKeyboardButtonTypeSwitchInline) {
        val chat = this.chat ?: return
        val user = if (viaBotUserId != 0L) tdlib!!.cache().user(viaBotUserId) else tdlib!!.chatUser(chat)
        if (user == null || !user.hasUsername()) {
            return
        }

        val username = user.primaryUsername()

        if (switchInline.targetChat.getConstructor() == TdApi.TargetChatCurrent.CONSTRUCTOR && canWriteMessages() && hasSendMessagePermission(RightId.SEND_OTHER_MESSAGES)) {
            if (inputView != null) {
                inputView!!.setInput("@" + username + " " + switchInline.query, true, true)
            }
            return
        }

        // TODO support TargetChatInternalLink
        tdlib!!.ui().switchInline(this, username, switchInline.query, false)
    }

    // Toast, Report Spam, Share my contact info
    fun showCallbackToast(text: CharSequence?) {
        toastAlertView!!.setText(text, null, topBar!!.isVisible(toastAlertItem))
        cancelScheduledToastHide()
        scheduledToastHide = object : CancellableRunnable() {
            public override fun act() {
                topBar!!.setItemVisible(toastAlertItem, false, true)
            }
        }
        topBar!!.setItemVisible(toastAlertItem, true, true)
        runOnUiThread(scheduledToastHide!!, 3000L)
    }

    override fun onFactorChanged(id: Int, factor: Float, fraction: Float, callee: FactorAnimator?) {
        when (id) {
            ANIMATOR_SCROLL_TO_BOTTOM -> {
                scrollToBottomButtonWrap!!.setAlpha(clamp(factor))
                checkScrollButtonOffsets()
            }

            ANIMATOR_MENTION_BUTTON -> {
                setMentionButtonFactor(factor)
                checkScrollButtonOffsets()
            }

            ANIMATOR_SEND -> {
                setSendFactor(factor)
            }

            ANIMATOR_SEARCH_PROGRESS -> {
                setSearchInProgress(factor)
            }

            ANIMATOR_BOTTOM_BUTTON -> {
                updateBottomBarStyle()
            }

            ANIMATOR_REACTION_BUTTON -> {
                setReactionButtonFactor(factor)
                checkScrollButtonOffsets()
            }

            ANIMATOR_SEARCH_BY_USER -> {
                setSearchByUserTransformFactor(factor)
                applySearchByUserTransformFactor(factor)
            }

            ANIMATOR_SEARCH_NAVIGATION -> {
                setBadgesButtonsTranslationX(searchControlsFactor, factor)
            }
        }
    }

    private var scheduledToastHide: CancellableRunnable? = null

    private fun cancelScheduledToastHide() {
        if (scheduledToastHide != null) {
            scheduledToastHide!!.cancel()
            removeCallbacks(scheduledToastHide!!)
            scheduledToastHide = null
        }
    }

    private fun forceHideToast() {
        cancelScheduledToastHide()
        topBar!!.setItemVisible(toastAlertItem, false, false)
    }

    override fun onFactorChangeFinished(id: Int, finalFactor: Float, callee: FactorAnimator?) {
        when (id) {
            ANIMATOR_BOTTOM_BUTTON -> {
                if (finalFactor == 0f) {
                    bottomButtonAction = 0
                }
            }
        }
    }

    // Emoji tones
    override fun displayBaseViewWithAnchor(
        context: EmojiToneHelper?,
        anchorView: View,
        viewToDisplay: View,
        viewWidth: Int,
        viewHeight: Int,
        horizontalMargin: Int,
        horizontalOffset: Int,
        verticalOffset: Int
    ): IntArray? {
        return EmojiToneHelper.defaultDisplay(
            context, anchorView, viewToDisplay, viewWidth, viewHeight, horizontalMargin, horizontalOffset, verticalOffset, contentView, bottomWrap,
            this.emojiKeyboardLayout
        )
    }

    override fun removeView(context: EmojiToneHelper?, displayedView: View?) {
        contentView!!.removeView(displayedView)
    }

    fun onUsernamePick(username: String?) {
        inputView!!.setInput("@" + username + " ", true, true)
    }

    // Link preview
    private fun closeLinkPreview() {
        findTargetContext().dismiss()
        updateReplyBarVisibility(!this.isEditingMessage)
        inputView!!.setTextChangedSinceChatOpened(true)
    }

    override fun onSelectLinkPreviewUrl(view: ReplyBarView?, messageContext: MessageInputContext, url: String) {
        if (messageContext.setLinkPreviewUrl(url)) {
            Settings.instance().markTutorialAsComplete(Settings.TUTORIAL_MULTIPLE_LINK_PREVIEWS)
            inputView!!.setTextChangedSinceChatOpened(true)
        }
    }

    override fun onRequestToggleLargeMedia(view: ReplyBarView?, buttonView: View?, messageContext: MessageInputContext, linkPreview: LinkPreview): Boolean {
        val options = messageContext.takeOutputLinkPreviewOptions(false)
        if (options.isDisabled) {
            return false
        }
        if (linkPreview.toggleLargeMedia()) {
            options.forceSmallMedia = linkPreview.forceSmallMedia()
            options.forceLargeMedia = linkPreview.forceLargeMedia()
            if (isEmpty(options.url)) {
                options.url = linkPreview.url
            }
            inputView!!.setTextChangedSinceChatOpened(true)
            showLinkPreviewHint(Lang.getString(if (linkPreview.getOutputShowLargeMedia()) R.string.LinkPreviewEnlarged else R.string.LinkPreviewMinimized))
            return true
        }
        return false
    }

    override fun onRequestToggleShowAbove(view: ReplyBarView?, buttonView: View?, messageContext: MessageInputContext): Boolean {
        val options = messageContext.takeOutputLinkPreviewOptions(false)
        if (!options.isDisabled) {
            options.showAboveText = !options.showAboveText
            showLinkPreviewHint(Lang.getString(if (options.showAboveText) R.string.LinkPreviewShowAbove else R.string.LinkPreviewShowBelow))
            return true
        }
        return false
    }

    private var linkPreviewHint: TooltipInfo? = null

    private fun showLinkPreviewHint(text: CharSequence) {
        if (linkPreviewHint == null) {
            linkPreviewHint = context().tooltipManager().builder(replyBarView!!.getLinkPreviewToggleView())
                .locate(TooltipOverlayView.LocationProvider { targetView: View?, rect: Rect? ->
                    replyBarView!!.getLinkPreviewToggleView().getTargetBounds(targetView, rect)
                })
                .icon(R.drawable.baseline_info_24)
                .ignoreViewScale(true)
                .controller(this)
                .show(tdlib, text)
        } else {
            linkPreviewHint!!.reset(context().tooltipManager().newContent(tdlib, text, 0), R.drawable.baseline_info_24)
            linkPreviewHint!!.show()
        }
        linkPreviewHint!!.hideDelayed(false)
    }

    private fun obtainLinkPreviewOptions(close: Boolean): LinkPreviewOptions {
        val linkPreviewOptions = findTargetContext().takeOutputLinkPreviewOptions(true)
        if (close) {
            findTargetContext().reset()
            updateReplyBarVisibility(false)
        }
        return linkPreviewOptions
    }

    private val draftContext = MessageInputContext(this, tdlib, null, null)
    private var editContext: MessageInputContext? = null

    private fun findTargetContext(): MessageInputContext {
        return (if (this.isEditingMessage) editContext else draftContext)!!
    }

    fun showLinkPreview(foundUrls: FoundUrls?) {
        if (inPreviewMode || isInForceTouchMode()) {
            return
        }
        val targetContext = findTargetContext()
        if (targetContext.setFoundUrls(foundUrls)) {
            updateReplyBarVisibility(true)
        }
    }

    private fun showingLinkPreview(): Boolean {
        return findTargetContext().isVisible
    }

    // Guess about the future RecyclerView height
    fun makeGuessAboutWidth(): Int {
        if (isInForceTouchMode()) {
            return Screen.currentWidth() - ForceTouchView.getMatchParentHorizontalMargin() * 2
        } else {
            return Screen.currentWidth()
        }
    }

    fun makeGuessAboutHeight(): Int {
        if (isInForceTouchMode()) {
            return makeGuessAboutForcePreviewHeight()
        } else {
            var height: Int
            if (Settings.instance().useEdgeToEdge()) {
                height = context().getVisibleContentHeight() - context().getRootView().getTopInset() - HeaderView.getSize(false)
            } else {
                height = Screen.currentHeight() - HeaderView.getSize(true)
            }

            if (canWriteMessages() || actionShowing) {
                height -= Screen.dp(49f)
            }
            /*if (bottomWrap == null || bottomWrap.getVisibility() == View.VISIBLE) {
        height -= Screen.dp(49f);
      }*/
            return height
        }
    }

    protected open fun makeGuessAboutForcePreviewHeight(): Int {
        return getForcePreviewHeight( /* hasHeader */true,  /* hasFooter */true)
    }

    override fun hideSoftwareKeyboard() {
        super.hideSoftwareKeyboard()
        Keyboard.hide(inputView)
    }

    override fun onKeyboardStateChanged(visible: Boolean): Boolean {
        if (this.isEventLog) {
            bottomWrap!!.setVisibility(if (visible) View.INVISIBLE else View.VISIBLE)
            bottomSpace!!.setVisibility(if (visible) View.INVISIBLE else View.VISIBLE)
            bottomShadowView!!.setVisibility(if (visible) View.INVISIBLE else View.VISIBLE)
            var params = (messagesView!!.getLayoutParams() as RelativeLayout.LayoutParams)
            params.addRule(RelativeLayout.ABOVE, if (visible) 0 else R.id.msg_bottom)
            messagesView!!.setLayoutParams(params)

            params = (scrollToBottomButtonWrap!!.getLayoutParams() as RelativeLayout.LayoutParams)
            params.addRule(RelativeLayout.ABOVE, if (visible) 0 else R.id.msg_bottom)
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, if (visible) RelativeLayout.TRUE else 0)
            scrollToBottomButtonWrap!!.setLayoutParams(params)

            updateBottomBarStyle()
            updateMessagesViewInset()
        }

        if (isFocused()) {
            //boolean emojiShown = this.emojiShown;
            // boolean commandsShown = this.commandsShown;
            if (visible && !getKeyboardState()) {
                if (emojiShown) {
                    closeEmojiKeyboard(true)
                }
                if (commandsShown) {
                    closeCommandsKeyboard(true)
                }
            }
            val result = super.onKeyboardStateChanged(visible)
            if (emojiShown && this.emojiKeyboardLayout != null) {
                emojiKeyboardLayout!!.onKeyboardStateChanged(visible)
            }
            if (commandsShown && keyboardLayout != null) {
                keyboardLayout!!.onKeyboardStateChanged(visible)
            }
            return result
        } else {
            return super.onKeyboardStateChanged(visible)
        }
    }

    override fun performOnBackPressed(fromTop: Boolean, commit: Boolean): Boolean {
        val context = context()
        if (context.getRecordAudioVideoController().isOpen()) {
            if (commit) {
                context.getRecordAudioVideoController().finishRecording(true)
            }
            return true
        }
        if (hasEditedChanges()) {
            if (commit) {
                if (this.isEditingCaption) {
                    showUnsavedChangesPromptBeforeLeaving(Lang.getString(R.string.DiscardEditCaptionHint), Lang.getString(R.string.DiscardEditCaption), null)
                } else {
                    showUnsavedChangesPromptBeforeLeaving(Lang.getString(R.string.DiscardEditMsgHint), Lang.getString(R.string.DiscardEditMsg), null)
                }
            }
            return true
        }

        if (hasAttachedFiles()) {
            if (commit) {
                showUnsavedChangesPromptBeforeLeaving(Lang.getString(R.string.DiscardCaptionHint), Lang.getString(R.string.DiscardEditCaption), null)
            }
            return true
        }

        if (!fromTop) {
            if (emojiShown) {
                if (commit) {
                    emojiState = false
                    closeEmojiKeyboard()
                }
                return true
            }
            if (commandsShown) {
                if (commit) {
                    commandsState = false
                    closeCommandsKeyboard(true)
                }
                return true
            }
        }

        return super.performOnBackPressed(fromTop, commit)
    }

    private var emojiShown = false

    // Emoji
    var emojiState: Boolean = false
        private set

    private fun toggleEmojiKeyboard() {
        if (emojiShown) {
            closeEmojiKeyboard()
        } else {
            openEmojiKeyboard()
        }
    }

    private val keyboardOffset: Float
        get() = if (this.emojiKeyboardLayout != null) emojiKeyboardLayout!!.getLayoutTranslationOffset() else 0f

    private fun onKeyboardLayoutTranslation(translationY: Float) {
        updateButtonsY()
        updateReplyView()
        if (bottomWrap != null) {
            bottomWrap!!.setTranslationY(translationY)
        }
    }

    private fun updateBottomWrapOffset() {
        if (bottomWrap != null) {
            val height = if (emojiShown || commandsShown) 0 else extraBottomInset
            Views.setPaddingBottom(bottomWrap, height)
            if (bottomSpace!!.setLayoutHeight(height, false)) {
                onMessagesFrameChanged()
            }
        }
    }

    private fun openEmojiKeyboard() {
        if (!emojiShown) {
            if (this.emojiKeyboardLayout == null) {
                this.emojiKeyboardLayout = KeyboardFrameLayout(context())
                emojiKeyboardLayout!!.setLayoutParams(newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                emojiKeyboardLayout!!.setParentView(bottomWrap, contentView, contentView)
                emojiKeyboardLayout!!.setUpdateTranslationListener(RunnableFloat { translationY: Float -> this.onKeyboardLayoutTranslation(translationY) })
                emojiKeyboardLayout!!.setExtraBottomInset(extraBottomInset, extraBottomInsetWithoutIme)

                textFormattingLayout = emojiKeyboardLayout!!.contentView.textFormattingLayout
                textFormattingLayout!!.init(this, inputView, object : TextFormattingLayout.Delegate {
                    override fun onWantsCloseTextFormattingKeyboard() {
                        closeTextFormattingKeyboard()
                    }

                    override fun onWantsOpenTextFormattingKeyboard() {
                        openEmojiKeyboard()
                    }
                })

                emojiLayout = emojiKeyboardLayout!!.contentView.emojiLayout
                emojiLayout!!.initWithMediasEnabled(this, true, this, this, false)
                emojiLayout!!.setAllowPremiumFeatures(this.isSelfChat)
                emojiLayout!!.setAllowMedia(!hasAttachedFiles())
                emojiLayout!!.setLayoutParams(newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                bottomWrap!!.addView(this.emojiKeyboardLayout)
            }
            emojiKeyboardLayout!!.setVisible(true)

            // updateButtonsY();
            if (commandsShown) {
                emojiState = commandsState
                forceCloseCommandsKeyboard()
            } else {
                emojiState = getKeyboardState()
            }

            setEmojiShown(true, true)
            if (emojiState) {
                emojiButton!!.setImageResource(R.drawable.baseline_keyboard_24)
                emojiKeyboardLayout!!.hideKeyboard(inputView)
            } else {
                emojiButton!!.setImageResource(R.drawable.baseline_direction_arrow_down_24)
            }
        }
    }

    override fun onSectionSwitched(layout: EmojiLayout?, section: Int, prevSection: Int) {
        if (emojiShown) {
            if (section != prevSection) {
                notifyChoosingEmoji(prevSection, false)
            }
            updateEmojiStatus()
        }
    }

    private fun updateEmojiStatus() {
        @EmojiMediaType val type = if (emojiLayout != null) emojiLayout!!.getCurrentEmojiSection() else EmojiLayout.getTargetSection()
        notifyChoosingEmoji(type, emojiShown)
    }

    private fun setEmojiShown(emojiShown: Boolean, animated: Boolean) {
        if (this.emojiShown != emojiShown) {
            this.emojiShown = emojiShown
            if (emojiShown) {
                hideSoftwareKeyboard()
            }
            updateEmojiStatus()
            setTextFormattingLayoutVisible(textInputHasSelection)
            updateBottomWrapOffset()

            if (inputView != null) {
                inputView!!.setActionModeVisibility(!textInputHasSelection || !emojiShown)
            }
        }
    }

    private fun forceCloseEmojiKeyboard() {
        if (emojiShown) {
            if (this.emojiKeyboardLayout != null) {
                emojiKeyboardLayout!!.setVisible(false)
            }
            setEmojiShown(false, false)
            emojiButton!!.setImageResource(getTargetIcon(true))
        }
    }

    private fun closeEmojiKeyboard(byKeyboardOpen: Boolean = false) {
        if (emojiShown) {
            if (this.emojiKeyboardLayout != null) {
                emojiKeyboardLayout!!.setVisible(false)
            }
            if (emojiState && isFocused() && !byKeyboardOpen) {
                emojiKeyboardLayout!!.showKeyboard(inputView)
            }
            setEmojiShown(false, true)
            emojiButton!!.setImageResource(getTargetIcon(true))
        }
    }

    private fun updateRecordLayout() {
        context().getRecordAudioVideoController().updatePositions()
    }

    /*@Override
  public boolean onBackspace () {
    if (inputView.length() > 0) {
      inputView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
      return true;
    }
    return false;
  }

  @Override
  public void onEmojiSelected (String emoji) {
    inputView.onEmojiSelected(emoji);
  }*/
    override fun onEnterEmoji(emoji: String?) {
        inputView!!.onEmojiSelected(emoji)
    }

    override fun onEnterCustomEmoji(sticker: TGStickerObj?) {
        inputView!!.onCustomEmojiSelected(sticker)
    }

    override fun getStickerSuggestionsChatId(): Long {
        return getChatId()
    }

    override fun getOutputChatId(): Long {
        return getChatId()
    }

    override fun onSendSticker(view: View?, sticker: TGStickerObj, sendOptions: MessageSendOptions): Boolean {
        if (lastJunkTime == 0L || SystemClock.uptimeMillis() - lastJunkTime >= JUNK_MINIMUM_DELAY) {
            if (sendSticker(view, sticker.getSticker(), sticker.getFoundByEmoji(), true, sendOptions)) {
                lastJunkTime = SystemClock.uptimeMillis()
                return true
            }
        }
        return false
    }

    override fun onSendGIF(view: View?, animation: TdApi.Animation): Boolean {
        if (lastJunkTime == 0L || SystemClock.uptimeMillis() - lastJunkTime >= JUNK_MINIMUM_DELAY) {
            if (sendAnimation(view, animation, true)) {
                lastJunkTime = SystemClock.uptimeMillis()
                return true
            }
        }
        return false
    }

    override fun onDeleteEmoji() {
        if (inputView!!.length() > 0) {
            inputView!!.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
        }
    }

    override fun onSearchRequested(layout: EmojiLayout?, areStickers: Boolean) {
        setInputInlineBot(0, "@" + tdlib!!.animationSearchBotUsername)
    }

    fun onInputTextChange(charSequence: CharSequence, byUserAction: Boolean) {
        if (sendMenu != null) {
            sendMenu!!.hideMenu()
        }
        if (emojiLayout != null) {
            emojiLayout!!.onTextChanged(charSequence)
        }
        updateSendButton(charSequence, true)
        if (byUserAction) {
            setTyping(charSequence.length > 0)
            inputView!!.setTextChangedSinceChatOpened(true)
        }
    }

    override fun isEmojiInputEmpty(): Boolean {
        return inputView == null || inputView!!.getText().length == 0
    }

    val topOffset: Int
        get() {
            var total = topBar!!.getTotalVisualHeight()
            total = (total * (1f - getSearchTransformFactor())).toInt()
            return total
        }

    fun onMessagesFrameChanged() {
        context().updateHackyOverlaysPositions()
        manager!!.onViewportMeasure()
        if (attachedFiles != null && attachedFiles!!.getParent() != null) {
            attachedFiles!!.updatePosition(true)
        }
        if (messagesView != null) {
            messagesView!!.invalidate()
        }
    }

    private val cursorCoordinates = IntArray(2)
    private val symbolUnderCursorPosition = IntArray(2)

    val inputCursorOffset: IntArray
        get() {
            if (inputView == null) {
                cursorCoordinates[1] = 0
                cursorCoordinates[0] = cursorCoordinates[1]
                return cursorCoordinates
            }
            inputView!!.getSymbolUnderCursorPosition(symbolUnderCursorPosition)
            cursorCoordinates[0] = symbolUnderCursorPosition[0] + inputView!!.getLeft() + inputView!!.getPaddingLeft()
            val y = context.getRootView().getMeasuredHeight() - getInputOffset(true) - extraBottomInset
            // cursorCoordinates[1] = symbolUnderCursorPosition[1] - inputView.getLineHeight() + context.getRootView().getMeasuredHeight() - extraBottomInset - getInputOffset(true) - Screen.dp(40);
            cursorCoordinates[1] = y
            return cursorCoordinates
        }

    fun getInputOffset(excludeTranslation: Boolean): Int {
        if (bottomWrap!!.getVisibility() == View.GONE) {
            return 0
        }
        var bottom = bottomWrap!!.getMeasuredHeight().toFloat()
        if (!excludeTranslation) {
            bottom += this.replyOffset
        }
        bottom += this.keyboardOffset
        bottom -= extraBottomInset.toFloat()
        return bottom.toInt()
    }

    private var lastJunkTime: Long = 0

    // Send sticker
    private fun sendContent(
        view: View?,
        rightId: Int,
        defaultRes: Int,
        specificRes: Int,
        specificUntilRes: Int,
        canReply: Boolean,
        sendOptions: MessageSendOptions,
        content: Future<InputMessageContent?>
    ): Boolean {
        return sendContent(
            view,
            rightId,
            defaultRes,
            specificRes,
            specificUntilRes,
            if (canReply) Future { this.obtainReplyTo() } else null, sendOptions, content)
    }

    private fun showGifRestriction(view: View?): Boolean {
        return showSlowModeRestriction(view, null) || showRestriction(
            view,
            RightId.SEND_OTHER_MESSAGES,
            R.string.ChatDisabledStickers,
            R.string.ChatRestrictedStickers,
            R.string.ChatRestrictedStickersUntil
        )
    }

    @JvmOverloads
    fun showPhotoVideoRestriction(view: View?, checkPhotos: Boolean = true, checkVideos: Boolean = true): Boolean { // TODO separate photos & videos
        val photosStatus = if (checkPhotos) tdlib!!.getRestrictionStatus(chat, RightId.SEND_PHOTOS) else null
        val videosStatus = if (checkVideos) tdlib!!.getRestrictionStatus(chat, RightId.SEND_VIDEOS) else null
        if (photosStatus == null && videosStatus == null) {
            return false
        }

        if (showSlowModeRestriction(view, null)) {
            return true
        }

        if (videosStatus == null || (videosStatus.isGlobal && photosStatus != null && !photosStatus.isGlobal)) {
            // photo
            return showRestriction(view, RightId.SEND_PHOTOS, R.string.ChatDisabledPhoto, R.string.ChatRestrictedPhoto, R.string.ChatRestrictedPhotoUntil)
        }
        if (photosStatus == null || (photosStatus.isGlobal && videosStatus != null && !videosStatus.isGlobal)) {
            // video
            return showRestriction(view, RightId.SEND_PHOTOS, R.string.ChatDisabledVideo, R.string.ChatRestrictedVideo, R.string.ChatRestrictedVideoUntil)
        }
        return showRestriction(view, RightId.SEND_PHOTOS, R.string.ChatDisabledMedia, R.string.ChatRestrictedMedia, R.string.ChatRestrictedMediaUntil)
    }

    fun showRestriction(view: View?, @RightId rightId: Int): Boolean {
        val text = tdlib!!.getDefaultRestrictionText(chat!!, rightId)
        return showRestriction(view, text)
    }

    fun showSlowModeRestriction(v: View?, sendOptions: MessageSendOptions?): Boolean {
        val restriction = tdlib()!!.getSlowModeRestrictionText(getChatId(), if (sendOptions != null) sendOptions.schedulingState else null)
        if (restriction != null) {
            if (v === sendButton || v === recordButton) {
                showBottomHint(restriction, true)
                isSlowModeRestrictionHintVisible = true
                return true
            }
            showRestriction(v, restriction)
            return true
        }

        return false
    }

    fun showRestriction(view: View?, restrictionText: CharSequence?): Boolean {
        if (restrictionText != null) {
            if (view === sendButton || view === recordButton) {
                showBottomHint(restrictionText, true)
            } else if (view == null) {
                UI.showToast(restrictionText, Toast.LENGTH_SHORT)
            } else {
                context().tooltipManager().builder(view).icon(R.drawable.baseline_warning_24).controller(this).show(tdlib, restrictionText).hideDelayed()
            }
            return true
        }
        return false
    }

    fun showRestriction(view: View?, @RightId rightId: Int, defaultRes: Int, specificRes: Int, specificUntilRes: Int): Boolean {
        val restrictionText = tdlib!!.buildRestrictionText(chat!!, rightId, defaultRes, specificRes, specificUntilRes)
        return showRestriction(view, restrictionText)
    }

    private fun sendContent(
        view: View?,
        @RightId rightId: Int,
        defaultRes: Int,
        specificRes: Int,
        specificUntilRes: Int,
        replyToFuture: Future<ReplyInfo?>?,
        initialSendOptions: MessageSendOptions,
        content: Future<InputMessageContent?>
    ): Boolean {
        if (showSlowModeRestriction(view, initialSendOptions) || showRestriction(view, rightId, defaultRes, specificRes, specificUntilRes)) return false
        pickDateOrProceed(initialSendOptions, SimpleSendCallback { modifiedSendOptions: MessageSendOptions?, disableMarkdown: Boolean ->
            val replyInfo = if (replyToFuture != null) replyToFuture.getValue() else null
            val replyTo = if (replyInfo != null) replyInfo.toInputMessageReply() else null
            val topicId = getMessageTopicId(replyInfo)
            val sendOptions = newSendOptions(
                modifiedSendOptions,
                getInputSuggestedPostInfo(replyInfo),
                obtainSilentMode()
            )

            val inputMessageContent = content.getValue()
            tdlib!!.sendMessage(chat!!.id, topicId, replyTo, sendOptions, inputMessageContent, null)
        })
        return true
    }

    private fun sendSticker(view: View?, sticker: Sticker?, emoji: String?, allowReply: Boolean, initialSendOptions: MessageSendOptions): Boolean {
        if (sticker == null) {
            return false
        }
        if (sticker.isPremium() && tdlib!!.ui().showPremiumAlert(this, view, TdlibUi.PremiumFeature.STICKER)) {
            return false
        }
        if (sticker.customEmojiId() != 0L && canWriteMessages() && inputView != null) {
            inputView!!.onCustomEmojiSelected(sticker)
            return false
        }
        if (sticker.customEmojiId() != 0L && canWriteMessages() && inputView != null) {
            inputView!!.onCustomEmojiSelected(sticker)
            return false
        }
        return sendContent(
            view,
            RightId.SEND_OTHER_MESSAGES,
            R.string.ChatDisabledStickers,
            R.string.ChatRestrictedStickers,
            R.string.ChatRestrictedStickersUntil,
            allowReply,
            initialSendOptions,
            me.vkryl.core.lambda.Future { InputMessageSticker(InputFileId(sticker.sticker.id), null, 0, 0, emoji) })
    }

    private fun sendSticker(path: String?, allowReply: Boolean, initialSendOptions: MessageSendOptions) {
        sendContent(
            null,
            RightId.SEND_OTHER_MESSAGES,
            R.string.ChatDisabledStickers,
            R.string.ChatRestrictedStickers,
            R.string.ChatRestrictedStickersUntil,
            allowReply,
            initialSendOptions,
            me.vkryl.core.lambda.Future {
                InputMessageSticker(
                    TD.createInputFile(path), null, 0, 0, null
                )
            })
    }

    private fun sendAnimation(view: View?, animation: TdApi.Animation, allowReply: Boolean): Boolean {
        return sendContent(
            view,
            RightId.SEND_OTHER_MESSAGES,
            R.string.ChatDisabledGifs,
            R.string.ChatRestrictedGifs,
            R.string.ChatRestrictedGifsUntil,
            allowReply,
            newSendOptions(),
            me.vkryl.core.lambda.Future { TD.toInputMessageContent(animation) })
    }

    private fun sendDice(view: View?, emoji: String?) {
        val disabledRes: Int
        val restrictedRes: Int
        val restrictedUntilRes: Int
        if (ContentPreview.EMOJI_DART.textRepresentation == emoji) {
            disabledRes = R.string.ChatDisabledDart
            restrictedRes = R.string.ChatRestrictedDart
            restrictedUntilRes = R.string.ChatRestrictedDartUntil
        } else if (ContentPreview.EMOJI_DICE.textRepresentation == emoji) {
            disabledRes = R.string.ChatDisabledDice
            restrictedRes = R.string.ChatRestrictedDice
            restrictedUntilRes = R.string.ChatRestrictedDiceUntil
        } else {
            disabledRes = R.string.ChatDisabledStickers
            restrictedRes = R.string.ChatRestrictedStickers
            restrictedUntilRes = R.string.ChatRestrictedStickersUntil
        }
        sendContent(
            view,
            RightId.SEND_OTHER_MESSAGES,
            disabledRes,
            restrictedRes,
            restrictedUntilRes,
            null,
            newSendOptions(),
            me.vkryl.core.lambda.Future { InputMessageDice(emoji, false) })
    }

    // Event log
    private var chatAdmins: ChatAdministrators? = null
    private var pendingChatAdminsCallback: RunnableData<ChatAdministrators?>? = null

    private fun loadChannelAdmins(after: RunnableData<ChatAdministrators?>) {
        if (chatAdmins != null) {
            after.runWithData(chatAdmins)
            return
        }
        if (pendingChatAdminsCallback != null) {
            pendingChatAdminsCallback = after
            return
        }
        pendingChatAdminsCallback = after

        // TdApi.Function function = new TdApi.GetSupergroupMembers(TD.getChatSupergroupId(chat), new TdApi.ChannelMembersFilterAdministrators(), 0, 200);
        val chatId = chat!!.id
        val handler = Client.ResultHandler { `object`: TdApi.Object? ->
            when (`object`!!.getConstructor()) {
                ChatAdministrators.CONSTRUCTOR -> {
                    tdlib!!.ui().post(Runnable {
                        if (!isDestroyed()) {
                            val members = `object` as ChatAdministrators
                            chatAdmins = members
                            pendingChatAdminsCallback!!.runWithData(members)
                        }
                    })
                }

                TdApi.Error.CONSTRUCTOR -> {
                    UI.showError(`object`)
                }
            }
        }
        if (tdlib!!.isSupergroupChat(chat) && tdlib!!.telegramAntiSpamUserId() != 0L) {
            tdlib!!.client().send(GetUser(tdlib!!.telegramAntiSpamUserId()), Client.ResultHandler { ignored: TdApi.Object? ->
                tdlib!!.client().send(GetChatAdministrators(chatId), handler)
            })
        } else {
            tdlib!!.client().send(GetChatAdministrators(chatId), handler)
        }
    }

    private fun openEventLogSettings() {
        loadChannelAdmins(RunnableData { result: ChatAdministrators? ->
            if (!isFocused()) {
                return@RunnableData
            }
            val filters = manager!!.getEventLogFilters()
            val userIds = manager.getEventLogUserIds()

            val items = ArrayList<ListItem?>()

            val ids: IntArray
            val strings: Array<String?>

            val isChannel = tdlib!!.isChannel(chat!!.id)

            if (isChannel) {
                ids = intArrayOf(
                    R.id.btn_filterAll,
                    R.id.btn_filterAdmins,
                    R.id.btn_filterMembers,
                    R.id.btn_filterInviteLinks,
                    R.id.btn_filterInfo,
                    R.id.btn_filterSettings,
                    R.id.btn_filterDeletedMessages,
                    R.id.btn_filterEditedMessages,
                    R.id.btn_filterPinnedMessages,
                    R.id.btn_filterLeavingMembers,
                    R.id.btn_filterVideoChats
                )
                strings = arrayOf<String?>(
                    Lang.getString(R.string.EventLogFilterAll),
                    Lang.getString(R.string.EventLogFilterNewAdmins),
                    Lang.getString(R.string.EventLogFilterNewMembers),
                    Lang.getString(R.string.EventLogFilterInviteLinks),
                    Lang.getString(R.string.EventLogFilterChannelInfo),
                    Lang.getString(R.string.EventLogFilterChannelSettings),
                    Lang.getString(R.string.EventLogFilterDeletedMessages),
                    Lang.getString(R.string.EventLogFilterEditedMessages),
                    Lang.getString(R.string.EventLogFilterPinnedMessages),
                    Lang.getString(R.string.EventLogFilterLeavingMembers),
                    Lang.getString(R.string.EventLogFilterLiveStreams)
                )
            } else {
                ids = intArrayOf(
                    R.id.btn_filterAll,
                    R.id.btn_filterRestrictions,
                    R.id.btn_filterAdmins,
                    R.id.btn_filterMembers,
                    R.id.btn_filterInviteLinks,
                    R.id.btn_filterInfo,
                    R.id.btn_filterSettings,
                    R.id.btn_filterDeletedMessages,
                    R.id.btn_filterEditedMessages,
                    R.id.btn_filterPinnedMessages,
                    R.id.btn_filterLeavingMembers,
                    R.id.btn_filterVideoChats
                )
                strings = arrayOf<String?>(
                    Lang.getString(R.string.EventLogFilterAll),
                    Lang.getString(R.string.EventLogFilterNewRestrictions),
                    Lang.getString(R.string.EventLogFilterNewAdmins),
                    Lang.getString(R.string.EventLogFilterNewMembers),
                    Lang.getString(R.string.EventLogFilterInviteLinks),
                    Lang.getString(R.string.EventLogFilterGroupInfo),
                    Lang.getString(R.string.EventLogFilterGroupSettings),
                    Lang.getString(R.string.EventLogFilterDeletedMessages),
                    Lang.getString(R.string.EventLogFilterEditedMessages),
                    Lang.getString(R.string.EventLogFilterPinnedMessages),
                    Lang.getString(R.string.EventLogFilterLeavingMembers),
                    Lang.getString(R.string.EventLogFilterVoiceChats)
                )
            }

            var isFirst = true
            var i = 0

            for (id in ids) {
                if (isFirst) {
                    isFirst = false
                } else {
                    items.add(ListItem(ListItem.TYPE_SEPARATOR_FULL))
                }
                items.add(
                    ListItem(
                        ListItem.TYPE_CHECKBOX_OPTION,
                        if (id == R.id.btn_filterAll) id else R.id.btn_filter,
                        0,
                        strings[i],
                        id,
                        checkFilter(id, filters)
                    ).setData(filters)
                )
                i++
            }
            items.add(ListItem(ListItem.TYPE_SHADOW_BOTTOM).setTextColorId(ColorId.background))

            items.add(ListItem(ListItem.TYPE_SHADOW_TOP).setTextColorId(ColorId.background))

            items.add(ListItem(ListItem.TYPE_CHECKBOX_OPTION, R.id.btn_members, 0, R.string.EventLogAllAdmins, userIds == null))

            if (tdlib!!.isSupergroupChat(chat)) {
                val fullInfo = tdlib!!.cache().supergroupFull(toSupergroupId(chat!!.id))
                if (fullInfo != null && fullInfo.hasAggressiveAntiSpamEnabled) {
                    val userId = tdlib!!.telegramAntiSpamUserId()
                    if (userId != 0L) {
                        items.add(ListItem(ListItem.TYPE_SEPARATOR_FULL))
                        items.add(
                            ListItem(
                                ListItem.TYPE_CHECKBOX_OPTION_WITH_AVATAR,
                                R.id.user,
                                0,
                                tdlib!!.cache().userName(userId),
                                userIds == null || indexOf(userIds, userId) != -1
                            ).setLongId(userId).setLongValue(userId)
                        )
                    }
                }
            }

            for (admin in chatAdmins!!.administrators) {
                items.add(ListItem(ListItem.TYPE_SEPARATOR_FULL))
                items.add(
                    ListItem(
                        ListItem.TYPE_CHECKBOX_OPTION_WITH_AVATAR,
                        R.id.user,
                        0,
                        tdlib!!.cache().userName(admin.userId),
                        userIds == null || indexOf(userIds, admin.userId) != -1
                    ).setLongId(admin.userId).setLongValue(admin.userId)
                )
            }


            val array = arrayOfNulls<ListItem>(items.size)
            items.toArray<ListItem?>(array)

            val b = SettingsWrapBuilder(R.id.btn_filter)
            showSettings(
                b
                    .setNeedSeparators(false)
                    .setNeedRootInsets(true)
                    .setRawItems(array)
                    .setSaveStr(R.string.Apply)
                    .setAllowResize(true)
                    .setDisableToggles(true)
                    .setOnActionButtonClick(OnActionButtonClick { wrap: SettingsWrap?, view: View?, isCancel: Boolean ->
                        if (isCancel) {
                            return@OnActionButtonClick false
                        }
                        val filter = ChatEventLogFilters(
                            true,
                            true,
                            true,
                            true,
                            true,
                            true,
                            true,
                            true,
                            true,
                            true,
                            true,
                            true,
                            true,
                            true,
                            true
                        )
                        val userIds1: LongList?


                        var i12 = wrap!!.adapter.indexOfViewById(R.id.btn_members)
                        if (i12 != -1 && wrap.adapter.getItems().get(i12).isSelected()) {
                            userIds1 = null
                        } else {
                            userIds1 = LongList(if (chatAdmins != null) chatAdmins!!.administrators.size else 10)
                        }

                        var filterCount = 0

                        val listItems = wrap.adapter.getItems()
                        val totalCount = listItems.size
                        i12 = 0
                        while (i12 < totalCount) {
                            val item = listItems.get(i12)
                            val itemId = item.getId()

                            if (itemId == R.id.btn_filter) {
                                val isSelected = item.isSelected()
                                if (isSelected) {
                                    filterCount++
                                }
                                val checkId = item.getCheckId()
                                if (checkId == R.id.btn_filterRestrictions) {
                                    filter.memberRestrictions = isSelected
                                } else if (checkId == R.id.btn_filterAdmins) {
                                    filter.memberPromotions = isSelected
                                } else if (checkId == R.id.btn_filterMembers) {
                                    filter.memberInvites = isSelected
                                    filter.memberJoins = filter.memberInvites
                                } else if (checkId == R.id.btn_filterInviteLinks) {
                                    filter.inviteLinkChanges = isSelected
                                } else if (checkId == R.id.btn_filterInfo) {
                                    filter.infoChanges = isSelected
                                } else if (checkId == R.id.btn_filterDeletedMessages) {
                                    filter.messageDeletions = isSelected
                                } else if (checkId == R.id.btn_filterSettings) {
                                    filter.settingChanges = isSelected
                                } else if (checkId == R.id.btn_filterEditedMessages) {
                                    filter.messageEdits = isSelected
                                } else if (checkId == R.id.btn_filterPinnedMessages) {
                                    filter.messagePins = isSelected
                                } else if (checkId == R.id.btn_filterLeavingMembers) {
                                    filter.memberLeaves = isSelected
                                } else if (checkId == R.id.btn_filterVideoChats) {
                                    filter.videoChatChanges = isSelected
                                }
                            } else if (itemId == R.id.user) {
                                if (item.isSelected() && userIds1 != null) {
                                    userIds1.append(item.getLongValue())
                                }
                            }
                            i12++
                        }

                        if (filterCount == 0 || (userIds1 != null && userIds1.size() == 0)) {
                            context.tooltipManager().builder(view)
                                .show(null, tdlib, R.drawable.baseline_warning_24, Lang.getString(R.string.EventLogEmptyFilter))
                            return@OnActionButtonClick true
                        }

                        manager.applyEventLogFilters(filter, if (userIds1 != null) userIds1.get() else null)
                        false
                    })
                    .setSettingProcessor(CustomSettingProcessor { item: ListItem?, view: SettingView?, isUpdate: Boolean ->
                        when (item!!.getViewType()) {
                            ListItem.TYPE_CHECKBOX_OPTION, ListItem.TYPE_CHECKBOX_OPTION_WITH_AVATAR -> {
                                val userId = item.getLongValue()
                                view!!.setEmojiStatus(if (userId != 0L) tdlib!!.cache().user(userId) else null)
                                (view.getChildAt(0) as CheckBoxView).setChecked(item.isSelected(), isUpdate)
                            }
                        }
                    })
                    .setOnSettingItemClick(object : ViewController.OnSettingItemClick { override fun onSettingItemClick(view: View?, settingsId: Int, item: ListItem?, doneButton: TextView?, settingsAdapter: SettingsAdapter?, window: PopupLayout?) {
                        when (item!!.getViewType()) {
                            ListItem.TYPE_CHECKBOX_OPTION, ListItem.TYPE_CHECKBOX_OPTION_WITH_AVATAR -> {}
                            else -> return
                        }
                        val isSelect = ((view as SettingView).getChildAt(0) as CheckBoxView).toggle()
                        item.setSelected(isSelect)

                        val allItems = settingsAdapter!!.getItems()
                        val size = allItems.size

                        val itemId = item.getId()
                        if (itemId == R.id.btn_members) {
                            // Select/unselect all admins
                            for (i1 in 0..<size) {
                                val userItem = allItems.get(i1)
                                if (userItem.getId() == R.id.user && userItem.isSelected() != isSelect) {
                                    userItem.setSelected(isSelect)
                                    settingsAdapter.updateValuedSettingByPosition(i1)
                                }
                            }
                        } else if (itemId == R.id.user) {
                            // Select/unselect user, unselect "All admins"
                            val i1 = settingsAdapter.indexOfViewById(R.id.btn_members)
                            if (i1 != -1) {
                                val allItem = allItems.get(i1)
                                if (allItem.isSelected()) {
                                    allItem.setSelected(false)
                                    settingsAdapter.updateValuedSettingByPosition(i1)
                                }
                            }
                        } else if (itemId == R.id.btn_filterAll) {
                            // Select/Unselect all filters
                            for (i1 in 0..<size) {
                                val filterItem = allItems.get(i1)
                                if (filterItem.getId() == R.id.btn_filter && filterItem.isSelected() != isSelect) {
                                    filterItem.setSelected(isSelect)
                                    settingsAdapter.updateValuedSettingByPosition(i1)
                                }
                            }
                        } else if (itemId == R.id.btn_filter) {
                            var selectedFilters = 0
                            for (i1 in 0..<size) {
                                val filterItem = allItems.get(i1)
                                if (filterItem.getId() == R.id.btn_filter && filterItem.isSelected()) {
                                    selectedFilters++
                                }
                            }

                            val allSelected = selectedFilters == ids.size - 1

                            val i1 = settingsAdapter.indexOfViewById(R.id.btn_filterAll)
                            if (i1 != -1) {
                                val allItem = allItems.get(i1)
                                if (allItem.isSelected() != allSelected) {
                                    allItem.setSelected(allSelected)
                                    settingsAdapter.updateValuedSettingByPosition(i1)
                                }
                            }
                        }
                    }})
            )
        })
    }

    // Send text
    fun updateSendButton(message: CharSequence, animated: Boolean) {
        sendButton!!.setIsActive(message.length > 0 || this.isEditingCaption)
        checkSendButton(animated)
    }

    private fun checkSendButton(animated: Boolean) {
        setSendVisible(
            inputView!!.getText().length > 0 || this.isEditingMessage || hasAttachedFiles(),
            animated && getParentOrSelf().isAttachedToNavigationController()
        )
    }

    private fun displaySendButton() {
        sendButton!!.setVisibility(View.VISIBLE)
    }

    private fun hideSendButton() {
        sendButton!!.setVisibility(View.INVISIBLE)
    }

    private fun displayAttachButtons() {
        attachButtons!!.setVisibility(View.VISIBLE)
        if (canSelectSender()) {
            messageSenderButton!!.setVisibility(View.VISIBLE)
        }
    }

    private fun hideAttachButtons() {
        attachButtons!!.setVisibility(View.INVISIBLE)
        messageSenderButton!!.setVisibility(View.INVISIBLE)
    }

    private fun displayEmojiButton() {
        emojiButton!!.setVisibility(View.VISIBLE)
        emojiButton!!.setOnClickListener(this)
    }

    private fun hideEmojiButton() {
        emojiButton!!.setVisibility(View.INVISIBLE)
        emojiButton!!.setOnClickListener(null)
    }

    val attachButton: View
        get() = mediaButton!!

    private var sendFactor = 0f

    private fun setSendFactor(factor: Float) {
        if (this.sendFactor != factor) {
            this.sendFactor = factor

            val scale = .5f * factor

            val scale1 = .5f + scale
            sendButton!!.setAlpha(factor)
            sendButton!!.setScaleX(scale1)
            sendButton!!.setScaleY(scale1)

            val scale2 = 1f - scale
            attachButtons!!.setAlpha(1f - factor)
            attachButtons!!.setScaleX(scale2)
            attachButtons!!.setScaleY(scale2)
            messageSenderButton!!.setSendFactor(sendFactor)

            if (tooltipInfo != null && tooltipInfo!!.isVisible()) {
                tooltipInfo!!.reposition()
            }
        }
    }

    private val sendShown = BoolAnimator(ANIMATOR_SEND, this, DECELERATE_INTERPOLATOR, 150L)

    private fun setSendVisible(isVisible: Boolean, animated: Boolean) {
        if (isVisible && sendButton != null && sendButton!!.getVisibility() != View.VISIBLE) { // fix
            displaySendButton()
        }
        if (this.sendShown.getValue() != isVisible || !animated) {
            hideBottomHint()
            if (!sendShown.isAnimating()) {
                if (isVisible) {
                    displaySendButton()
                } else {
                    displayAttachButtons()
                }
            }
            sendShown.setValue(isVisible, animated && isFocused())
            showMessageMenuTutorial()
        }
    }

    fun sendText(applyMarkdown: Boolean, sendOptions: MessageSendOptions?) {
        if (inputView != null) {
            sendText(inputView!!.getOutputText(applyMarkdown), true, applyMarkdown, true, true, sendOptions)
        }
    }

    fun sendCommand(command: InlineResultCommand) {
        pickDateOrProceed(newSendOptions(), SimpleSendCallback { sendOptions: MessageSendOptions?, disableMarkdown: Boolean ->
            if (isUserChat(getChatId()) || command.getUsername() == null) {
                sendText(command.getCommand(), true, true, false, sendOptions)
            } else {
                sendText(command.getCommand() + '@' + command.getUsername(), true, true, false, sendOptions)
            }
        })
    }

    fun sendCommand(command: String, username: String?) {
        pickDateOrProceed(newSendOptions(), SimpleSendCallback { sendOptions: MessageSendOptions?, disableMarkdown: Boolean ->
            if (isPrivate(getChatId()) || username == null || command.contains("@")) {
                if (actionShowing && actionMode == ACTION_BOT_START) {
                    hideActionButton()
                }
                sendText(command, false, false, false, sendOptions)
            } else {
                sendText(command + '@' + username, false, false, false, sendOptions)
            }
        })
    }

    private fun sendText(msg: String?, clearInput: Boolean, allowReply: Boolean, allowLinkPreview: Boolean, sendOptions: MessageSendOptions?) {
        sendText(TdApi.FormattedText(msg, null), clearInput, true, allowReply, allowLinkPreview, sendOptions)
    }

    private var isSendingText = false

    private fun setIsSendingText(isSendingText: Boolean) {
        if (this.isSendingText != isSendingText) {
            this.isSendingText = isSendingText
            setStackLocked(isSendingText)
            inputView!!.setInputBlocked(isSendingText)
        }
    }

    private fun sendText(
        msg: TdApi.FormattedText,
        clearInput: Boolean,
        allowDice: Boolean,
        allowReply: Boolean,
        allowLinkPreview: Boolean,
        initialSendOptions: MessageSendOptions?
    ) {
        if ((msg.isEmpty() && !(clearInput && inputView != null && inputView!!.getText().length > 0)) || (isSendingText && clearInput)) {
            return
        }
        if (!hasSendBasicMessagePermission()) {
            context().tooltipManager().builder(if (sendButton != null) sendButton else inputView).show(tdlib, R.string.MessageInputTextDisabledHint)
                .hideDelayed()
            return
        }

        val chatId = getChatId()
        val replyInfo = if (allowReply) (if (clearInput) this.currentReplyId else obtainReplyTo()) else null
        val replyTo = if (replyInfo != null) replyInfo.toInputMessageReply() else null
        val linkPreviewOptions = if (allowLinkPreview) obtainLinkPreviewOptions(false) else LinkPreviewOptions(
            true, "", false, false, false
        )
        val topicId = getMessageTopicId(replyInfo)

        val content: InputMessageContent?
        if (allowDice && tdlib!!.shouldSendAsDice(msg)) {
            val disabledRes: Int
            val restrictedRes: Int
            val restrictedUntilRes: Int
            if (ContentPreview.EMOJI_DART.textRepresentation == msg.text) {
                disabledRes = R.string.ChatDisabledDart
                restrictedRes = R.string.ChatRestrictedDart
                restrictedUntilRes = R.string.ChatRestrictedDartUntil
            } else if (ContentPreview.EMOJI_DICE.textRepresentation == msg.text) {
                disabledRes = R.string.ChatDisabledDice
                restrictedRes = R.string.ChatRestrictedDice
                restrictedUntilRes = R.string.ChatRestrictedDiceUntil
            } else {
                disabledRes = R.string.ChatDisabledStickers
                restrictedRes = R.string.ChatRestrictedStickers
                restrictedUntilRes = R.string.ChatRestrictedStickersUntil
            }
            if (showRestriction(sendButton, RightId.SEND_OTHER_MESSAGES, disabledRes, restrictedRes, restrictedUntilRes)) {
                return
            }
            content = InputMessageDice(msg.text.trim { it <= ' ' }, clearInput)
        } else {
            content = InputMessageText(msg, linkPreviewOptions, clearInput)
        }

        val forceUpdateOrderOfInstalledStickerSets = Settings.instance().getNewSetting(Settings.SETTING_FLAG_DYNAMIC_ORDER_EMOJI_PACKS)
        val finalSendOptions = newSendOptions(
            initialSendOptions,
            getInputSuggestedPostInfo(replyInfo),
            obtainSilentMode(),
            forceUpdateOrderOfInstalledStickerSets
        )
        val functions = TD.sendMessageText(
            chatId,
            topicId,
            replyTo,
            finalSendOptions,
            content,
            tdlib!!.maxMessageTextLength()
        ) as MutableList<*> as MutableList<TdApi.Function<*>?>

        if (showSlowModeRestriction(if (sendButton != null) sendButton else inputView, finalSendOptions)) {
            return
        }

        if (clearInput) {
            val sentMessages: MutableList<TdApi.Message?> = ArrayList<TdApi.Message?>(functions.size)
            setIsSendingText(true)
            manager!!.setSentMessages(sentMessages)
            executeSendMessageFunctions(functions, sentMessages, finalSendOptions.schedulingState != null, RunnableBool { success: Boolean ->
                clearInputAfterSend(success, allowReply, replyInfo, allowLinkPreview)
            })
        } else {
            for (function in functions) {
                tdlib!!.client().send(function, tdlib!!.messageHandler())
            }
        }
    }

    private fun clearInputAfterSend(success: Boolean, allowReply: Boolean, replyTo: ReplyInfo?, allowLinkPreview: Boolean) {
        if (!isDestroyed()) {
            manager!!.setSentMessages(null)
            if (success) {
                if (allowReply && replyTo != null && replyTo == this.currentReplyId) {
                    obtainReplyTo()
                }
                if (allowLinkPreview) {
                    obtainLinkPreviewOptions(true)
                }
                discardAttachedFiles(true)
                inputView!!.setInput("", false, true)
            }
            setIsSendingText(false)
            /*if (success) {
        inputView.setInput("", false);
      }*/
        }
    }

    private fun executeSendMessageFunctions(
        functions: MutableList<TdApi.Function<*>?>,
        sentMessages: MutableList<TdApi.Message?>,
        isSchedule: Boolean,
        onDone: RunnableBool
    ) {
        val expectedCount = functions.size
        val sentFunctionsCount = IntArray(1)

        val handler: Client.ResultHandler = object : Client.ResultHandler {
            override fun onResult(result: TdApi.Object) {
                var done = false
                when (result.getConstructor()) {
                    TdApi.Message.CONSTRUCTOR -> {
                        val message = result as TdApi.Message
                        sentMessages.add(message)
                        sentFunctionsCount[0] += 1
                        val sentCount = sentFunctionsCount[0]
                        if (sentCount < expectedCount) {
                            tdlib!!.listeners().subscribeToUpdates(message)
                            tdlib!!.client().send(functions.get(sentCount), this)
                        } else {
                            done = true
                        }
                        tdlib!!.messageHandler().onResult(result)
                    }

                    TdApi.Messages.CONSTRUCTOR -> {
                        val messages = result as TdApi.Messages
                        for (message in messages.messages) {
                            if (message == null) continue
                            sentMessages.add(message)
                        }
                        sentFunctionsCount[0] += 1
                        val sentCount = sentFunctionsCount[0]
                        if (sentCount < expectedCount) {
                            for (message in messages.messages) {
                                if (message == null) continue
                                tdlib!!.listeners().subscribeToUpdates(message)
                            }
                            tdlib!!.client().send(functions.get(sentCount), this)
                        } else {
                            done = true
                        }
                        tdlib!!.messageHandler().onResult(result)
                    }

                    TdApi.Error.CONSTRUCTOR -> {
                        tdlib!!.ui().post(Runnable {
                            if (isFocused()) {
                                showBottomHint(TD.toErrorString(result), true)
                            } else {
                                UI.showError(result)
                            }
                        })
                        done = true
                    }

                    else -> {
                        throw UnsupportedOperationException(result.toString())
                    }
                }
                if (done) {
                    val sentMessagesCount = sentMessages.size
                    if (sentMessagesCount > 0) {
                        for (i in sentMessagesCount - 1 downTo 0) {
                            tdlib!!.listeners().unsubscribeFromUpdates(sentMessages.get(i))
                        }
                        val parsedMessages = manager!!.parseMessages(sentMessages)
                        tdlib!!.ui().post(Runnable {
                            if (isSchedule == areScheduled) {
                                manager.addSentMessages(parsedMessages)
                            }
                            onDone.runWithBool(sentFunctionsCount[0] == expectedCount)
                            if (!areScheduled && isSchedule && isFocused()) {
                                viewScheduledMessages(true)
                            }
                        })
                    } else {
                        tdlib!!.ui().post(Runnable { onDone.runWithBool(false) })
                    }
                }
            }
        }
        tdlib!!.client().send(functions.get(0), handler)
    }

    fun sendContact(user: TdApi.User, allowReply: Boolean, initialSendOptions: MessageSendOptions) {
        if (hasSendMessagePermission(RightId.SEND_BASIC_MESSAGES)) {
            pickDateOrProceed(initialSendOptions, SimpleSendCallback { modifiedSendOptions: MessageSendOptions?, disableMarkdown: Boolean ->
                val replyInfo = if (allowReply) obtainReplyTo() else null
                val replyTo = if (replyInfo != null) replyInfo.toInputMessageReply() else null
                val topicId = getMessageTopicId(replyInfo)
                val sendOptions = newSendOptions(
                    modifiedSendOptions,
                    getInputSuggestedPostInfo(replyInfo),
                    obtainSilentMode()
                )

                val inputMessageContact = InputMessageContact(Contact(user.phoneNumber, user.firstName, user.lastName, null, user.id))
                tdlib!!.sendMessage(chat!!.id, topicId, replyTo, sendOptions, inputMessageContact, null)
            })
        }
    }

    fun shareMyContact(allowReply: Boolean) {
        shareMyContact(if (allowReply) obtainReplyTo() else null)
    }

    fun shareMyContact(forceReplyTo: ReplyInfo?) {
        if (hasSendMessagePermission(RightId.SEND_BASIC_MESSAGES)) {
            val user = tdlib!!.myUser()
            if (user != null) {
                pickDateOrProceed(newSendOptions(), SimpleSendCallback { modifiedSendOptions: MessageSendOptions?, disableMarkdown: Boolean ->
                    val replyTo = if (forceReplyTo != null) forceReplyTo.toInputMessageReply() else null
                    val topicId = getMessageTopicId(forceReplyTo)
                    val sendOptions = newSendOptions(
                        modifiedSendOptions,
                        getInputSuggestedPostInfo(forceReplyTo),
                        obtainSilentMode()
                    )

                    val inputMessageContact = InputMessageContact(Contact(user.phoneNumber, user.firstName, user.lastName, null, user.id))
                    tdlib!!.sendMessage(chat!!.id, topicId, replyTo, sendOptions, inputMessageContact, null)
                })
            }
        }
    }

    fun send(content: InputMessageContent?, allowReply: Boolean, initialSendOptions: MessageSendOptions, after: RunnableData<TdApi.Message?>?) {
        if (tdlib()!!.getRestrictionText(chat!!, content) == null) {
            pickDateOrProceed(initialSendOptions, SimpleSendCallback { modifiedSendOptions: MessageSendOptions?, disableMarkdown: Boolean ->
                val replyInfo = if (allowReply) obtainReplyTo() else null
                val replyTo = if (replyInfo != null) replyInfo.toInputMessageReply() else null
                val topicId = getMessageTopicId(replyInfo)
                val sendOptions = newSendOptions(
                    modifiedSendOptions,
                    getInputSuggestedPostInfo(replyInfo),
                    obtainSilentMode()
                )
                tdlib!!.sendMessage(chat!!.id, topicId, replyTo, sendOptions, content, after)
            })
        }
    }

    fun sendInlineQueryResult(inlineQueryId: Long, id: String?, allowReply: Boolean, clearInput: Boolean, initialSendOptions: MessageSendOptions) {
        if (hasSendMessagePermission(RightId.SEND_OTHER_MESSAGES)) {
            pickDateOrProceed(initialSendOptions, SimpleSendCallback { modifiedSendOptions: MessageSendOptions?, disableMarkdown: Boolean ->
                val replyInfo = if (allowReply) obtainReplyTo() else null
                val replyTo = if (replyInfo != null) replyInfo.toInputMessageReply() else null
                val topicId = getMessageTopicId(replyInfo)
                val sendOptions = newSendOptions(
                    modifiedSendOptions,
                    getInputSuggestedPostInfo(replyInfo),
                    obtainSilentMode()
                )
                tdlib!!.sendInlineQueryResult(chat!!.id, topicId, replyTo, sendOptions, inlineQueryId, id)
                if (clearInput) {
                    inputView!!.setInput("", false, true)
                    inputView!!.getInlineSearchContext().resetInlineBotsCache()
                }
            })
        }
    }

    fun sendAudio(audio: TdApi.Audio, allowReply: Boolean) {
        if (hasSendMessagePermission(RightId.SEND_AUDIO)) {
            pickDateOrProceed(newSendOptions(), SimpleSendCallback { modifiedSendOptions: MessageSendOptions?, disableMarkdown: Boolean ->
                val replyInfo = if (allowReply) obtainReplyTo() else null
                val replyTo = if (replyInfo != null) replyInfo.toInputMessageReply() else null
                val topicId = getMessageTopicId(replyInfo)
                val sendOptions = newSendOptions(
                    modifiedSendOptions,
                    getInputSuggestedPostInfo(replyInfo),
                    obtainSilentMode()
                )
                tdlib!!.sendMessage(chat!!.id, topicId, replyTo, sendOptions, TD.toInputMessageContent(audio), null)
            })
        }
    }

    fun sendMusic(view: View?, musicFiles: MutableList<MusicEntry>, needGroupMedia: Boolean, allowReply: Boolean, initialSendOptions: MessageSendOptions?) {
        sendMusic(view, musicFiles, needGroupMedia, allowReply, null, initialSendOptions)
    }

    fun sendMusic(
        view: View?,
        musicFiles: MutableList<MusicEntry>,
        needGroupMedia: Boolean,
        allowReply: Boolean,
        lastFileCaption: TdApi.FormattedText?,
        initialSendOptions: MessageSendOptions?
    ) {
        val functions = getSendMusicFunctions(view, musicFiles, needGroupMedia, allowReply, lastFileCaption, initialSendOptions)
        if (functions == null) {
            return
        }
        for (function in functions) {
            tdlib!!.client().send(function, tdlib!!.messageHandler())
        }
    }

    private fun getSendMusicFunctions(
        view: View?,
        musicFiles: MutableList<MusicEntry>,
        needGroupMedia: Boolean,
        allowReply: Boolean,
        lastFileCaption: TdApi.FormattedText?,
        initialSendOptions: MessageSendOptions?
    ): MutableList<TdApi.Function<*>?>? {
        if (!showSlowModeRestriction(view, initialSendOptions) && !showRestriction(view, RightId.SEND_AUDIO)) {
            val content = arrayOfNulls<InputMessageContent>(musicFiles.size)
            for (i in content.indices) {
                val caption = if (i == content.size - 1) lastFileCaption else null
                val musicFile = musicFiles.get(i)
                content[i] = tdlib!!.filegen().createThumbnail<InputMessageAudio?>(
                    InputMessageAudio(
                        TD.createInputFile(musicFile.getPath(), musicFile.getMimeType()),
                        null,
                        (musicFile.getDuration() / 1000L).toInt(),
                        musicFile.getTitle(),
                        musicFile.getArtist(),
                        caption
                    ),
                    this.isSecretChat
                )
            }
            val replyInfo = if (allowReply) obtainReplyTo() else null
            val replyTo = if (replyInfo != null) replyInfo.toInputMessageReply() else null
            val topicId = getMessageTopicId(replyInfo)
            val finalSendOptions = newSendOptions(
                initialSendOptions,
                getInputSuggestedPostInfo(replyInfo),
                obtainSilentMode()
            )
            return TD.toFunctions(chat!!.id, topicId, replyTo, finalSendOptions, content, needGroupMedia)
        }
        return null
    }

    fun forwardMessage(message: TdApi.Message) { // TODO remove all related to Forward stuff to replace with ShareLayout
        if (tdlib!!.getRestrictionText(chat!!, message) == null) {
            val replyInfo = this.currentReplyId
            val topicId = getMessageTopicId(replyInfo)
            val sendOptions = newSendOptions(
                getInputSuggestedPostInfo(replyInfo),
                obtainSilentMode()
            )
            tdlib!!.forwardMessage(chat!!.id, topicId, message.chatId, message.id, sendOptions)
        }
    }

    // Attach
    private var openingMediaLayout = false

    private fun openMediaView(ignorePermissionRequest: Boolean, noMedia: Boolean) {
        if (openingMediaLayout) {
            return
        }

        if (!ignorePermissionRequest && context().permissions()
                .requestReadExternalStorage(Permissions.ReadType.IMAGES_AND_VIDEOS, RunnableInt { grantType: Int ->
                    openMediaView(true, grantType == Permissions.GrantResult.NONE)
                })
        ) {
            return
        }

        val mediaLayout: MediaLayout

        mediaLayout = MediaLayout(this)
        mediaLayout.initDefault(this)
        if (noMedia) {
            mediaLayout.setNoMediaAccess()
        }

        openingMediaLayout = true
        mediaLayout.preload(Runnable {
            if (isFocused() && !isDestroyed()) {
                mediaLayout.show()
            }
            openingMediaLayout = false
        }, 300L)

        // mediaLayout.show();
    }

    /*@Override
  public void onSendPaths (String[] paths, boolean areFiles) {
    if (areFiles) {
      for (String path : paths) {
        sendFile(path, true);
      }
    } else {
      for (String path : paths) {
        sendPhotoCompressed(path, 0, true);
      }
    }
  }*/
    override fun onRequestPermissionResult(requestCode: Int, success: Boolean) {
        when (requestCode) {
            BaseActivity.REQUEST_FINE_LOCATION -> {
                if (success) {
                    // TODO
                } else {
                    openMissingLocationPermissionAlert(false)
                }
            }
        }
    }

    /*@Override
  public void onActivityResultCancel (int requestCode, int resultCode) {
    switch (requestCode) {
      case Intents.ACTIVITY_RESULT_RESOLUTION: {
        // getCustomCurrentLocation(currentShareLocationDestroyKeyboard, false, false);
        break;
      }
    }
  }*/
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK) {
            return
        }
        when (requestCode) {
            Intents.ACTIVITY_RESULT_RESOLUTION -> {
                getCurrentLocation(currentShareLocationDestroyKeyboard, googleClient)
            }

            Intents.ACTIVITY_RESULT_IMAGE_CAPTURE, Intents.ACTIVITY_RESULT_VIDEO_CAPTURE -> {
                val file = Intents.takeLastOutputMedia()
                val isVideo = requestCode == Intents.ACTIVITY_RESULT_VIDEO_CAPTURE
                if (showSlowModeRestriction(mediaButton, null) || showRestriction(mediaButton, if (isVideo) RightId.SEND_VIDEOS else RightId.SEND_PHOTOS)) {
                    return
                }
                if (file != null) {
                    if (!this.isSecretChat) {
                        U.addToGallery(file)
                    }
                    U.toGalleryFile(file, isVideo, RunnableData { galleryFile: ImageGalleryFile? ->
                        if (isDestroyed()) {
                            ImageStrictCache.instance().forget(galleryFile)
                            return@RunnableData
                        }
                        if (galleryFile == null) {
                            UI.showToast(
                                if (requestCode == Intents.ACTIVITY_RESULT_VIDEO_CAPTURE) R.string.TakeVideoError else R.string.TakePhotoError,
                                Toast.LENGTH_SHORT
                            )
                            return@RunnableData
                        }
                        val stack = MediaStack(context, tdlib)
                        stack.set(MediaItem(context, tdlib, galleryFile))
                        val controller = MediaViewController(context, tdlib)
                        val hideMedia = AtomicBoolean(false)
                        controller.setArguments(
                            fromGallery(
                                this, null, null,
                                object : MediaSpoilerSendDelegate() {
                                    override fun sendSelectedItems(
                                        view: View?,
                                        images: ArrayList<ImageFile?>?,
                                        options: MessageSendOptions?,
                                        disableMarkdown: Boolean,
                                        asFiles: Boolean,
                                        showCaptionAboveMedia: Boolean,
                                        hasSpoiler: Boolean
                                    ): Boolean {
                                        sendPhotosAndVideosCompressed(
                                            arrayOf<ImageGalleryFile>(galleryFile),
                                            false,
                                            options,
                                            disableMarkdown,
                                            asFiles,
                                            showCaptionAboveMedia,
                                            hasSpoiler
                                        )
                                        return true
                                    }
                                },
                                stack, areScheduledOnly()
                            ).setReceiverChatIdValue(getChatId())
                                .setFlag(
                                    MediaViewController.Args.FLAG_DELETE_FILE_ON_EXIT,
                                    this.isSecretChat || !Settings.instance().getNewSetting(Settings.SETTING_FLAG_CAMERA_KEEP_DISCARDED_MEDIA)
                                )
                        )
                        controller.open()
                    })
                }
            }

            Intents.ACTIVITY_RESULT_GALLERY, Intents.ACTIVITY_RESULT_GALLERY_FILE -> {
                val path = if (data != null) data.getData() else null
                if (path == null) return
                val imagePath = U.tryResolveFilePath(path)

                if (inPreviewMode) {
                    if (previewMode == PREVIEW_MODE_WALLPAPER) {
                        tdlib!!.settings().setWallpaper(TGBackground(tdlib, imagePath), true, Theme.getWallpaperIdentifier())
                        return
                    }
                    return
                }

                if (imagePath != null && imagePath.endsWith(".webp") && tdlib!!.getRestrictionStatus(chat, RightId.SEND_OTHER_MESSAGES) == null) {
                    sendSticker(imagePath, true, newSendOptions())
                } else if (requestCode == Intents.ACTIVITY_RESULT_GALLERY_FILE) {
                    sendFiles(mediaButton, mutableListOf<String?>(imagePath), false, true, newSendOptions())
                } else {
                    sendPhotoCompressed(imagePath, null, true)
                }
            }

            Intents.ACTIVITY_RESULT_AUDIO -> {
                val path = data!!.getData()
                if (path == null) return
                if (showSlowModeRestriction(mediaButton, null) || showRestriction(mediaButton, RightId.SEND_AUDIO)) {
                    return
                }
                val audioPath = U.tryResolveFilePath(path)
                if (audioPath != null) {
                    val chatId = chat!!.id
                    val disableNotification = obtainSilentMode()
                    val replyInfo = obtainReplyTo()
                    val replyTo = if (replyInfo != null) replyInfo.toInputMessageReply() else null
                    val topicId = getMessageTopicId(replyInfo)
                    val sendOptions = newSendOptions(
                        getInputSuggestedPostInfo(replyInfo),
                        disableNotification
                    )
                    Background.instance().post(Runnable {
                        val file: AudioFile?
                        file = AudioFile(audioPath)
                        file.loadId3Tags()

                        val inputMessageAudio =
                            InputMessageAudio(TD.createInputFile(audioPath), null, file.getDuration(), file.getTitle(), file.getPerformer(), null)
                        tdlib!!.sendMessage(chatId, topicId, replyTo, sendOptions, inputMessageAudio)
                    })
                }
            }
        }
    }

    fun sendFiles(view: View?, paths: MutableList<String?>, needGroupMedia: Boolean, allowReply: Boolean, initialSendOptions: MessageSendOptions?) {
        sendFiles(view, paths, needGroupMedia, allowReply, null, initialSendOptions)
    }

    fun sendFiles(
        view: View?,
        paths: MutableList<String?>,
        needGroupMedia: Boolean,
        allowReply: Boolean,
        lastFileCaption: TdApi.FormattedText?,
        initialSendOptions: MessageSendOptions?
    ) {
        sendFiles(view, paths, needGroupMedia, allowReply, lastFileCaption, initialSendOptions, RunnableData { functions: MutableList<TdApi.Function<*>?>? ->
            if (functions == null) {
                return@RunnableData
            }
            for (function in functions) {
                tdlib!!.client().send(function, tdlib!!.messageHandler())
            }
        })
    }

    private fun sendFiles(
        view: View?,
        paths: MutableList<String?>,
        needGroupMedia: Boolean,
        allowReply: Boolean,
        lastFileCaption: TdApi.FormattedText?,
        initialSendOptions: MessageSendOptions?,
        onReadyToSend: RunnableData<MutableList<TdApi.Function<*>?>?>
    ) {
        if (showSlowModeRestriction(view, initialSendOptions)) {
            onReadyToSend.runWithData(null)
            return
        }

        val chatId = chat!!.id
        val isSecretChat = this.isSecretChat
        val replyInfo = if (allowReply) obtainReplyTo() else null
        val replyTo = if (replyInfo != null) replyInfo.toInputMessageReply() else null
        val topicId = getMessageTopicId(replyInfo)
        val finalSendOptions = newSendOptions(
            initialSendOptions,
            getInputSuggestedPostInfo(replyInfo),
            obtainSilentMode()
        )
        val allowAudio = tdlib!!.getRestrictionStatus(chat, RightId.SEND_AUDIO) == null
        val allowDocs = tdlib!!.getRestrictionStatus(chat, RightId.SEND_DOCS) == null
        val allowVideos = tdlib!!.getRestrictionStatus(chat, RightId.SEND_VIDEOS) == null
        val allowGifs = tdlib!!.getRestrictionStatus(chat, RightId.SEND_OTHER_MESSAGES) == null
        Media.instance().post(Runnable {
            var restrictionFailed = false
            val content: MutableList<InputMessageContent> = ArrayList<InputMessageContent>()
            for (a in paths.indices) {
                val path = paths.get(a)
                val isLast = a == paths.size - 1
                val caption = if (isLast) lastFileCaption else null
                val showCaptionAboveMedia = false // FIXME: showCaptionAboveMedia
                val info = TD.FileInfo()
                val inputFile = TD.createInputFile(path, null, info)
                val inputMessageContent =
                    TD.toInputMessageContent(path, inputFile, info, caption, showCaptionAboveMedia, allowAudio, allowGifs, allowVideos, allowDocs, false)
                if (inputMessageContent == null) {
                    restrictionFailed = true
                    break
                }
                content.add(inputMessageContent)
            }
            if (restrictionFailed) {
                runOnUiThreadOptional(Runnable {
                    showRestriction(view, RightId.SEND_DOCS)
                    onReadyToSend.runWithData(null)
                })
                return@Runnable
            }
            for (i in content.indices) {
                val inputMessageContent = content.get(i)
                content.set(i, tdlib!!.filegen().createThumbnail<InputMessageContent?>(inputMessageContent, isSecretChat))
            }

            val functions = TD.toFunctions(chatId, topicId, replyTo, finalSendOptions, content.toTypedArray<InputMessageContent?>(), needGroupMedia)
            UI.post(Runnable { onReadyToSend.runWithData(functions) })
        })
    }

    fun sendPhotoCompressed(path: String?, selfDestructType: MessageSelfDestructType?, allowReply: Boolean) {
        if (showSlowModeRestriction(mediaButton, null) || showRestriction(mediaButton, RightId.SEND_PHOTOS)) {
            return
        }
        if (isEmpty(path)) {
            return
        }
        if (path != null) {
            val chatId = chat!!.id
            val replyInfo = if (allowReply) obtainReplyTo() else null
            val replyTo = if (replyInfo != null) replyInfo.toInputMessageReply() else null
            val topicId = getMessageTopicId(replyInfo)
            val sendOptions = newSendOptions(
                getInputSuggestedPostInfo(replyInfo),
                obtainSilentMode()
            )
            val isSecret = this.isSecretChat

            Media.instance().post(Runnable {
                val opts = ImageReader.getImageSize(path)
                val orientation = U.getExifOrientation(path)
                val inSampleSize = ImageReader.calculateInSampleSize(opts, 1280, 1280)
                val sampledWidth = opts.outWidth / inSampleSize
                val sampledHeight = opts.outHeight / inSampleSize
                val width: Int
                val height: Int
                if (U.isExifRotated(orientation)) {
                    width = sampledHeight
                    height = sampledWidth
                } else {
                    width = sampledWidth
                    height = sampledHeight
                }
                val inputFile = PhotoGenerationInfo.newFile(path, U.getRotationForExifOrientation(orientation))
                val photo = tdlib!!.filegen().createThumbnail<InputMessagePhoto>(
                    InputMessagePhoto(inputFile, null, null, null, width, height, null, false, selfDestructType, false),
                    isSecret
                )
                tdlib!!.sendMessage(chatId, topicId, replyTo, sendOptions, photo)
            })
        }
    }

    fun sendPhotosAndVideosCompressed(
        files: Array<ImageGalleryFile>?,
        needGroupMedia: Boolean,
        modifiedSendOptions: MessageSendOptions?,
        disableMarkdown: Boolean,
        asFiles: Boolean,
        showCaptionAboveMedia: Boolean,
        hasSpoiler: Boolean
    ): Boolean {
        if (files == null || files.size == 0) {
            return false
        }

        // TODO check RightId.SEND_PHOTOS / RightId.SEND_VIDEOS
        val chatId = chat!!.id
        val replyInfo = obtainReplyTo()
        val replyTo = if (replyInfo != null) replyInfo.toInputMessageReply() else null
        val topicId = getMessageTopicId(replyInfo)
        val finalSendOptions = newSendOptions(
            modifiedSendOptions,
            getInputSuggestedPostInfo(replyInfo),
            obtainSilentMode()
        )

        val isSecretChat = this.isSecretChat

        Media.instance().post(Runnable {
            val inputContent = arrayOfNulls<InputMessageContent>(files.size)
            var i = 0
            for (file in files) {
                require(!(file.getSelfDestructType() != null && asFiles))
                val content: InputMessageContent?
                if (file.isVideo()) {
                    var sendAsAnimation = file.shouldMuteVideo()
                    var retriever: MediaMetadataRetriever? = null
                    try {
                        retriever = U.openRetriever(file.getFilePath())
                        if (!sendAsAnimation) {
                            val hasAudioStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)
                            if (isEmpty(hasAudioStr) || !hasAudioStr!!.lowercase(Locale.getDefault()).equalsOrBothEmpty("yes")) {
                                sendAsAnimation = true
                            }
                        }
                        val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                        if (isNumeric(rotation)) {
                            file.setRotation(parseInt(rotation))
                        }
                    } catch (ignored: Throwable) {
                        // Doing nothing
                    }
                    U.closeRetriever(retriever)

                    val size = IntArray(2)
                    file.getOutputSize(size)

                    val width = size[0]
                    val height = size[1]

                    val fileInfo = TD.FileInfo()
                    val forceVideo = Config.USE_VIDEO_COMPRESSION && !(asFiles && VideoGenerationInfo.isEmpty(file))
                    val inputVideo = if (forceVideo) VideoGenerationInfo.newFile(file.getFilePath(), file, asFiles) else TD.createInputFile(
                        file.getFilePath(),
                        null,
                        fileInfo
                    )
                    val caption = file.getCaption(true, !disableMarkdown)
                    if (asFiles && !forceVideo) {
                        content = tdlib!!.filegen().createThumbnail<InputMessageContent?>(
                            TD.toInputMessageContent(
                                file.getFilePath(),
                                inputVideo,
                                fileInfo,
                                caption,
                                showCaptionAboveMedia,
                                hasSpoiler
                            ), isSecretChat
                        )
                    } else if (sendAsAnimation && file.getSelfDestructType() == null && (files.size == 1 || !needGroupMedia)) {
                        content = tdlib!!.filegen().createThumbnail<InputMessageAnimation?>(
                            InputMessageAnimation(
                                inputVideo,
                                null,
                                null,
                                file.getVideoDuration(true),
                                width,
                                height,
                                caption,
                                showCaptionAboveMedia,
                                hasSpoiler
                            ), isSecretChat
                        )
                    } else {
                        content = tdlib!!.filegen().createThumbnail<InputMessageVideo?>(
                            InputMessageVideo(
                                inputVideo,
                                null,
                                null,
                                0,
                                null,
                                file.getVideoDuration(true),
                                width,
                                height,
                                U.canStreamVideo(inputVideo),
                                caption,
                                showCaptionAboveMedia,
                                file.getSelfDestructType(),
                                hasSpoiler
                            ), isSecretChat
                        )
                    }
                } else {
                    val size = IntArray(2)
                    file.getOutputSize(size)

                    val width = size[0]
                    val height = size[1]

                    val inputFile: TdApi.InputFile
                    if (asFiles && PhotoGenerationInfo.isEmpty(file)) {
                        inputFile = TD.createInputFile(file.getFilePath())
                    } else {
                        inputFile = PhotoGenerationInfo.newFile(file)
                    }

                    val caption = file.getCaption(true, !disableMarkdown)

                    if (asFiles) {
                        content = tdlib!!.filegen().createThumbnail<InputMessageDocument?>(InputMessageDocument(inputFile, null, false, caption), isSecretChat)
                    } else {
                        content = tdlib!!.filegen().createThumbnail<InputMessagePhoto?>(
                            InputMessagePhoto(
                                inputFile,
                                null,
                                null,
                                null,
                                width,
                                height,
                                caption,
                                showCaptionAboveMedia,
                                file.getSelfDestructType(),
                                hasSpoiler
                            ), isSecretChat
                        )
                    }
                }
                inputContent[i] = content
                i++
            }
            val functions = TD.toFunctions(chatId, topicId, replyTo, finalSendOptions, inputContent, needGroupMedia)
            for (function in functions) {
                tdlib!!.client().send(function, tdlib!!.messageHandler())
            }
        })

        return true
    }

    /*private void uploadFile (boolean isSecretChat, TdApi.InputMessageContent content) {
    if (true) {
      // FIXME TDLib uploadFile
      return;
    }
    TdApi.FileType fileType;
    TdApi.InputFile inputFile;
    switch (content.getConstructor()) {
      case TdApi.InputMessagePhoto.CONSTRUCTOR:
        fileType = new TdApi.FileTypePhoto();
        inputFile = isSecretChat ? null : ((TdApi.InputMessagePhoto) content).photo;
        break;
      case TdApi.InputMessageAnimation.CONSTRUCTOR:
        fileType = new TdApi.FileTypeAnimation();
        inputFile = isSecretChat ? null : ((TdApi.InputMessageAnimation) content).animation;
        break;
      case TdApi.InputMessageVideo.CONSTRUCTOR:
        fileType = new TdApi.FileTypeVideo();
        inputFile = isSecretChat ? null : ((TdApi.InputMessageVideo) content).video;
        break;
      default:
        return;
    }
    if (isSecretChat) {
      fileType = new TdApi.FileTypeSecret();
    }
    tdlib!!.client().send(new TdApi.UploadFile(inputFile, fileType, 2), tdlib!!.silentHandler());
  }*/
    // Typing utils
    private var broadcastingSomeAction = false
    private var broadcastActor: CancellableRunnable? = null

    private fun checkBroadcastingSomeAction() {
        val needBroadcast = broadcastingAction != 0 && context.getActivityState() == UI.State.RESUMED && !isDestroyed()
        if (this.broadcastingSomeAction != needBroadcast) {
            if (needBroadcast) {
                broadcastActor = object : CancellableRunnable() {
                    public override fun act() {
                        if (broadcastActor === this && broadcastingAction != 0) {
                            setChatAction(broadcastingAction, true, false)
                            tdlib!!.ui().postDelayed(this, 4500L)
                        }
                    }
                }
                tdlib!!.ui().postDelayed(broadcastActor!!, 4500L)
            } else {
                broadcastActor!!.cancel()
                broadcastActor = null
            }
            this.broadcastingSomeAction = needBroadcast
        }
    }

    private var broadcastingAction = 0

    fun setBroadcastAction(@ChatAction.Constructors action: Int) {
        var action = action
        if (action == ChatActionCancel.CONSTRUCTOR) {
            action = 0
        }
        val hadAction = this.broadcastingAction != 0
        val hasAction = action != 0
        if (hadAction != hasAction) {
            if (hasAction) {
                setChatAction(action, true, true)
            } else {
                setChatAction(broadcastingAction, false, false)
            }
            this.broadcastingAction = action
            checkBroadcastingSomeAction()
        } else if (this.broadcastingAction != action) {
            if (actions != null) {
                actions!!.delete(broadcastingAction)
            }
            setChatAction(action, true, true)
            this.broadcastingAction = action
        }
    }

    private var actions: SparseIntArray? = null
    private var lastActionCancelled = false

    fun setChatAction(@ChatAction.Constructors action: Int, set: Boolean, force: Boolean) {
        if (chat == null) {
            return
        }
        if (actions == null) {
            actions = SparseIntArray(5)
        }
        val handler = Tdlib.ResultHandler { ok: TdApi.Ok?, error: TdApi.Error? ->
            if (error != null && (error.code != 400 || "Have no rights to send a message" != error.message)) {
                tdlib!!.okHandler().onResult(error)
            }
        }
        var topicId: MessageTopic?
        if (this.isEditingMessage) {
            topicId = editContext!!.existingMessage!!.topicId
        } else {
            topicId = getMessageTopicId(reply)
            if (topicId == null && reply != null) {
                topicId =
                    if (reply!!.message.message.topicId != null) reply!!.message.message.topicId else if (tdlib!!.hasMessageThreads(reply!!.message.message.chatId)) MessageTopicThread(
                        reply!!.message.message.id
                    ) else null
            }
        }
        if (set) {
            val time = (SystemClock.uptimeMillis() / 1000L).toInt()
            if (time - actions!!.get(action) >= 4 || force || lastActionCancelled) {
                actions!!.put(action, time)
                tdlib!!.send<TdApi.Ok?>(SendChatAction(chat!!.id, topicId, null, constructChatAction(action)), handler)
                lastActionCancelled = false
            }
        } else {
            if (actions!!.get(action, 0) != 0) {
                actions!!.delete(action)
                tdlib!!.send<TdApi.Ok?>(SendChatAction(chat!!.id, topicId, null, ChatActionCancel()), handler)
                lastActionCancelled = true
            }
        }
    }

    fun setTyping(isTyping: Boolean) {
        if (chat != null) {
            setChatAction(TdApi.ChatActionTyping.CONSTRUCTOR, isTyping, false)
        }
    }

    override fun onSectionInteracted(layout: EmojiLayout?, emojiType: Int, interactionFinished: Boolean) {
        notifyChoosingEmoji(emojiType, !interactionFinished)
    }

    fun notifyChoosingEmoji(emojiType: Int, isChoosingEmoji: Boolean) {
        if (chat != null) {
            @ChatAction.Constructors val action: Int
            when (emojiType) {
                EmojiMediaType.STICKER -> {
                    action = TdApi.ChatActionChoosingSticker.CONSTRUCTOR
                    if (suggestionYDiff > Screen.dp(50f) || stickerPreviewIsVisible) {
                        hideEmojiSuggestionsTemporarily()
                    } else if (suggestionYDiff <= 0) {
                        showEmojiSuggestionsIfTemporarilyHidden()
                    }
                }

                EmojiMediaType.EMOJI -> {
                    if (suggestionXDiff > Screen.dp(50f)) {
                        hideStickersSuggestionsTemporarily()
                    } else if (suggestionXDiff <= 0) {
                        showStickersSuggestionsIfTemporarilyHidden()
                    }
                    return
                }

                EmojiMediaType.GIF -> return
                else -> return
            }
            setChatAction(action, isChoosingEmoji, false)
        }
    }

    // Audio utils
    override fun collectMedias(fromMessageId: Long, isSponsored: Boolean, filter: SearchMessagesFilter?): MediaStack? {
        if (!needTabs() || pagerScrollPosition == MediaTabsAdapter.Companion.POSITION_MESSAGES) {
            return manager!!.collectMedias(fromMessageId, isSponsored, filter)
        } else {
            val c = pagerContentAdapter?.cachedItems?.get(pagerScrollPosition)
            if (c != null) {
                return c.collectMedias(fromMessageId, isSponsored, filter)
            }
        }
        return null
    }

    override fun modifyMediaArguments(cause: Any?, args: MediaViewController.Args) {
        if (!needTabs() || pagerScrollPosition == MediaTabsAdapter.Companion.POSITION_MESSAGES) {
            args.delegate = this
        } else {
            val c = pagerContentAdapter?.cachedItems?.get(pagerScrollPosition)
            if (c is MediaCollectorDelegate) {
                (c as MediaCollectorDelegate).modifyMediaArguments(cause, args)
            }
        }
    }

    override fun getTargetLocation(indexInStack: Int, item: MediaItem): MediaViewThumbLocation? {
        if (!needTabs() || pagerScrollPosition == MediaTabsAdapter.Companion.POSITION_MESSAGES) {
            val i = manager!!.getAdapter().findMessageByMediaItem(item)
            if (i != -1) {
                val view = manager.getLayoutManager().findViewByPosition(i)
                if (view != null) {
                    val holder = messagesView!!.getChildViewHolder(view)
                    if (holder is MessagesHolder && ((MessagesHolder.isMessageType(holder.getItemViewType())))) {
                        val msg = manager.getAdapter().getMessage(i)
                        // FIXME state with FOLLOW
                        val offset = messagesView!!.getTranslationY().toInt()
                        return msg.getMediaThumbLocation(
                            item.getSourceMessageId(),
                            view,
                            view.getTop() - this.topOffset,
                            messagesView!!.getBottom() - view.getBottom(),
                            view.getTop() + HeaderView.getSize(true) + offset
                        )
                    }
                }
            }
        } else {
            val c = pagerContentAdapter?.cachedItems?.get(pagerScrollPosition)
            if (c is MediaViewDelegate) {
                return (c as MediaViewDelegate).getTargetLocation(indexInStack, item)
            }
        }
        return null
    }

    override fun setMediaItemVisible(index: Int, item: MediaItem?, isVisible: Boolean) {
        if (!needTabs() || pagerScrollPosition == MediaTabsAdapter.Companion.POSITION_MESSAGES) {
            val i = manager!!.getAdapter().findMessageByMediaItem(item)
            if (i != -1) {
                val msg = manager.getAdapter().getMessage(i)
                msg.setMediaVisible(item, isVisible)
            }
        }
    }

    // Updates (new)
    fun subscribeToUpdates(chatId: Long) {
        tdlib!!.listeners().subscribeToChatUpdates(chatId, this)
        tdlib!!.singleUnreadReactionsManager().subscribeToUnreadSingleReactionUpdates(chatId, this)
        if (chatId != this.headerChatId) {
            tdlib!!.listeners().subscribeToChatUpdates(this.headerChatId, this)
        }
        tdlib!!.listeners().subscribeToSettingsUpdates(this)
        if (messageThread != null) {
            messageThread!!.addListener(this)
        }
        if (getChatId() == chatId) {
            when (chat!!.type.getConstructor()) {
                TdApi.ChatTypePrivate.CONSTRUCTOR -> {
                    tdlib!!.cache().subscribeToUserUpdates<MessagesController?>(TD.getUserId(chat), this)
                }

                TdApi.ChatTypeSecret.CONSTRUCTOR -> {
                    tdlib!!.cache().subscribeToUserUpdates<MessagesController?>(TD.getUserId(chat), this)
                    tdlib!!.cache().subscribeToSecretChatUpdates(TD.getSecretChatId(chat), this)
                }

                ChatTypeSupergroup.CONSTRUCTOR -> {
                    tdlib!!.cache().subscribeToSupergroupUpdates((chat!!.type as ChatTypeSupergroup).supergroupId, this)
                }

                ChatTypeBasicGroup.CONSTRUCTOR -> {
                    tdlib!!.cache().subscribeToGroupUpdates((chat!!.type as ChatTypeBasicGroup).basicGroupId, this)
                }
            }
        }
    }

    fun unsubscribeFromUpdates(chatId: Long) {
        tdlib!!.listeners().unsubscribeFromChatUpdates(chatId, this)
        tdlib!!.singleUnreadReactionsManager().unsubscribeFromUnreadSingleReactionUpdates(chatId, this)
        if (chatId != this.headerChatId) {
            tdlib!!.listeners().unsubscribeFromChatUpdates(this.headerChatId, this)
        }
        tdlib!!.listeners().unsubscribeFromSettingsUpdates(this)
        // tdlib!!.status().unsubscribeFromChatUpdates(chatId, this);
        if (messageThread != null) {
            messageThread!!.removeListener(this)
        }
        if (getChatId() == chatId) {
            when (chat!!.type.getConstructor()) {
                TdApi.ChatTypePrivate.CONSTRUCTOR -> {
                    tdlib!!.cache().unsubscribeFromUserUpdates<MessagesController?>(TD.getUserId(chat), this)
                }

                TdApi.ChatTypeSecret.CONSTRUCTOR -> {
                    tdlib!!.cache().unsubscribeFromUserUpdates<MessagesController?>(TD.getUserId(chat), this)
                    tdlib!!.cache().unsubscribeFromSecretChatUpdates(TD.getSecretChatId(chat), this)
                }

                ChatTypeSupergroup.CONSTRUCTOR -> {
                    tdlib!!.cache().unsubscribeFromSupergroupUpdates((chat!!.type as ChatTypeSupergroup).supergroupId, this)
                }

                ChatTypeBasicGroup.CONSTRUCTOR -> {
                    tdlib!!.cache().unsubscribeFromGroupUpdates((chat!!.type as ChatTypeBasicGroup).basicGroupId, this)
                }
            }
        }
    }

    override fun onJoinRequestsDismissed(chatId: Long) {
        runOnUiThreadOptional(Runnable {
            if (getChatId() == chatId && chat != null) {
                checkJoinRequests(chat!!.pendingJoinRequests)
            }
        })
    }

    override fun onJoinRequestsRestore(chatId: Long) {
        runOnUiThreadOptional(Runnable {
            if (getChatId() == chatId && chat != null) {
                checkJoinRequests(chat!!.pendingJoinRequests)
            }
        })
    }

    override fun onChatPendingJoinRequestsChanged(chatId: Long, pendingJoinRequests: ChatJoinRequestsInfo?) {
        runOnUiThreadOptional(Runnable {
            if (getChatId() == chatId) {
                checkJoinRequests(pendingJoinRequests)
            }
        })
    }

    override fun onChatActionBarChanged(chatId: Long, actionBar: ChatActionBar?) {
        runOnUiThreadOptional(Runnable {
            if (getChatId() == chatId) {
                checkActionBar()
            }
        })
    }

    override fun onChatTopMessageChanged(chatId: Long, topMessage: TdApi.Message?) {
        runOnUiThreadOptional(Runnable {
            if (getChatId() == chatId) {
                if (topMessage == null) {
                    manager!!.onMissedMessagesHintReceived()
                }
            }
        })
    }

    override fun onChatTitleChanged(chatId: Long, title: String?) {
        runOnUiThreadOptional(Runnable {
            if (this.headerChatId == chatId) {
                headerCell!!.setTitle(title)
            }
        })
    }

    private var exitOnTransformFinish = false

    override fun onChatHasScheduledMessagesChanged(chatId: Long, hasScheduledMessages: Boolean) {
        tdlib!!.ui().post(Runnable {
            if (getChatId() == chatId) {
                if (scheduleButton != null && scheduleButton!!.setVisible(messageThread == null && tdlib!!.chatHasScheduled(chatId))) {
                    commandButton!!.setTranslationX((if (scheduleButton!!.isVisible()) 0 else scheduleButton!!.getLayoutParams().width).toFloat())
                    attachButtons!!.updatePivot()
                }
                if (areScheduled && !hasScheduledMessages) {
                    forceFastAnimationOnce()
                    if (inTransformMode()) {
                        exitOnTransformFinish = true
                    } else {
                        navigateBack()
                    }
                }
            }
        })
    }

    override fun onChatPermissionsChanged(chatId: Long, permissions: ChatPermissions?) {
        tdlib!!.ui().post(Runnable {
            if (getChatId() == chatId) {
                updateBottomBar(true)
            }
        })
    }

    override fun onChatReadInbox(chatId: Long, lastReadInboxMessageId: Long, unreadCount: Int, availabilityChanged: Boolean) {
        tdlib!!.ui().post(Runnable {
            if (getChatId() == chatId) {
                updateCounters(true)
            }
        })
    }

    override fun onChatUnreadMentionCount(chatId: Long, unreadMentionCount: Int, availabilityChanged: Boolean) {
        tdlib!!.ui().post(Runnable {
            if (getChatId() == chatId) {
                updateCounters(true)
            }
        })
    }

    override fun onChatUnreadReactionCount(chatId: Long, unreadReactionCount: Int, availabilityChanged: Boolean) {
        tdlib!!.ui().post(Runnable {
            if (getChatId() == chatId) {
                updateCounters(true)
            }
        })
    }

    override fun onUnreadSingleReactionUpdate(chatId: Long, unreadReaction: UnreadReaction?) {
        UI.execute(Runnable {
            if (getChatId() == chatId) {
                updateCounters(true)
            }
        })
    }

    override fun onChatReadOutbox(chatId: Long, lastReadOutboxMessageId: Long) {
        tdlib!!.ui().post(Runnable {
            if (getChatId() == chatId && messageThread == null) {
                manager!!.updateChatReadOutbox(lastReadOutboxMessageId)
            }
        })
    }

    override fun onChatReplyMarkupChanged(chatId: Long, replyMarkupMessage: TdApi.Message?) {
        tdlib!!.ui().post(Runnable {
            if (getChatId() == chatId && botHelper != null) {
                botHelper!!.updateReplyMarkup(chatId, replyMarkupMessage)
            }
        })
    }

    override fun onChatDefaultDisableNotifications(chatId: Long, defaultDisableNotifications: Boolean) {
        tdlib!!.ui().post(Runnable {
            if (Config.NEED_SILENT_BROADCAST && silentButton != null && getChatId() == chatId) {
                silentButton!!.setValue(defaultDisableNotifications)
                updateInputHint()
            }
        })
    }

    override fun onChatDraftMessageChanged(chatId: Long, draftMessage: DraftMessage?) {
        runOnUiThreadOptional(Runnable {
            if (getChatId() == chatId && inputView != null && !inputView!!.textChangedSinceChatOpened() && !this.isSecretChat && messageThread == null) {
                // Applying server chat draft changes only if text wasn't changed while chat was open
                updateDraftMessage(chatId, draftMessage)
            }
        })
    }

    private fun updateDraftMessage(chatId: Long, draftMessage: DraftMessage?) {
        if (this.isEditingMessage) {
            return
        }
        val replyTo = if (draftMessage != null) draftMessage.replyTo else null
        if (replyTo == null || replyTo.getConstructor() == TdApi.InputMessageReplyToStory.CONSTRUCTOR) {
            closeReply(false, true)
        } else {
            val replyChatId: Long
            val replyMessageId: Long
            val replyQuote: InputTextQuote?
            val replyChecklistTaskId: Int
            val replyPollOptionId: String?
            when (replyTo.getConstructor()) {
                InputMessageReplyToMessage.CONSTRUCTOR -> {
                    val replyToMessage = replyTo as InputMessageReplyToMessage
                    replyChatId = getChatId()
                    replyMessageId = replyToMessage.messageId
                    replyQuote = replyToMessage.quote
                    replyChecklistTaskId = replyToMessage.checklistTaskId
                    replyPollOptionId = replyToMessage.pollOptionId
                }

                InputMessageReplyToExternalMessage.CONSTRUCTOR -> {
                    val replyToExternalMessage = replyTo as InputMessageReplyToExternalMessage
                    replyChatId = replyToExternalMessage.chatId
                    replyMessageId = replyToExternalMessage.messageId
                    replyQuote = replyToExternalMessage.quote
                    replyChecklistTaskId = replyToExternalMessage.checklistTaskId
                    replyPollOptionId = replyToExternalMessage.pollOptionId
                }

                TdApi.InputMessageReplyToStory.CONSTRUCTOR -> {
                    assertInputMessageReplyTo_acef6f3a()
                    throw unsupported(replyTo)
                }

                else -> {
                    assertInputMessageReplyTo_acef6f3a()
                    throw unsupported(replyTo)
                }
            }
            tdlib!!.send<TdApi.Message?>(GetMessage(replyChatId, replyMessageId), Tdlib.ResultHandler { remoteMessage: TdApi.Message?, error: TdApi.Error? ->
                tdlib!!.send<MessageProperties?>(
                    GetMessageProperties(replyChatId, replyMessageId),
                    Tdlib.ResultHandler { properties: MessageProperties?, error1: TdApi.Error? ->
                        runOnUiThreadOptional(
                            Runnable {
                                if (getChatId() == chatId && this.draftMessage.equalsTo(draftMessage) && remoteMessage != null && properties != null) {
                                    showReply(
                                        MessageWithProperties(remoteMessage, properties),
                                        replyQuote,
                                        replyChecklistTaskId,
                                        replyPollOptionId,
                                        false,
                                        false
                                    )
                                } else {
                                    closeReply(false, true)
                                }
                            })
                    })
            })
        }
        inputView!!.setDraft(if (draftMessage != null) draftMessage.inputMessageText else null)
    }

    private var canSendMessageToUser: CanSendMessageToUserResult? = null

    @UiThread
    private fun checkCanSendMessagesToUser(allowRemote: Boolean) {
        val chat = this.chat
        if (chat != null && TD.isPrivateChat(chat.type) && !inPreviewMode && !isInForceTouchMode()) {
            val user = tdlib!!.chatUser(chat)
            if (user != null && user.restrictsNewChats) {
                val userId = user.id
                tdlib!!.send<CanSendMessageToUserResult?>(
                    CanSendMessageToUser(userId, true),
                    Tdlib.ResultHandler { localCanSendMessageToUser: CanSendMessageToUserResult?, error: TdApi.Error? ->
                        if (localCanSendMessageToUser != null) {
                            processCanSendMessagesToUser(userId, localCanSendMessageToUser)
                        }
                        if (allowRemote || error != null) {
                            tdlib!!.send<CanSendMessageToUserResult?>(
                                CanSendMessageToUser(userId, false),
                                Tdlib.ResultHandler { remoteCanSendMessageToUser: CanSendMessageToUserResult?, error1: TdApi.Error? ->
                                    if (remoteCanSendMessageToUser != null) {
                                        processCanSendMessagesToUser(userId, remoteCanSendMessageToUser)
                                    }
                                })
                        }
                    })
                return
            }
        }
        processCanSendMessagesToUser(tdlib!!.chatUserId(getChatId()), null)
    }

    private fun processCanSendMessagesToUser(userId: Long, result: CanSendMessageToUserResult?) {
        executeOnUiThreadOptional(Runnable {
            if (getChatId() == fromUserId(userId) && !this.canSendMessageToUser.equalsTo(result)) {
                this.canSendMessageToUser = result
                updateBottomBar(true)
            }
        })
    }

    override fun onUserUpdated(user: TdApi.User?) {
        if (chat != null && user != null && headerCell != null && TD.getUserId(chat) == user.id) {
            runOnUiThreadOptional(Runnable {
                headerCell!!.setEmojiStatus(user)
                checkCanSendMessagesToUser(false)
            })
        }
    }

    override fun onUserFullUpdated(userId: Long, userFull: UserFullInfo?) {
        tdlib!!.ui().post(Runnable {
            if (chat != null && TD.getUserId(chat) == userId) {
                updateBottomBar(true)
            }
        })
    }

    override fun needUserStatusUiUpdates(): Boolean {
        return true
    }

    @UiThread
    override fun onUserStatusChanged(userId: Long, status: UserStatus?, uiOnly: Boolean) {
        if (chat != null && headerCell != null && TD.getUserId(chat) == userId) {
            headerCell!!.updateUserStatus(chat)
        }
    }

    /*@UiThread
  @Override
  public void onChatActionsChanged (long chatId, @Nullable ArrayList<int[]> actions) {
    if (chat != null && chat.id == chatId && headerCell != null) {
      headerCell.updateChatAction(chat, actions);
    }
  }*/
    override fun onSupergroupUpdated(supergroup: Supergroup) {
        tdlib!!.ui().post(Runnable {
            if (toSupergroupId(getChatId()) == supergroup.id) {
                updateBottomBar(true)
            }
        })
    }

    override fun onSupergroupFullUpdated(supergroupId: Long, newSupergroupFull: SupergroupFullInfo?) {
        tdlib!!.ui().post(Runnable {
            if (toSupergroupId(this.headerChatId) == supergroupId) {
                headerCell!!.updateUserStatus(chat)
            }
            if (toSupergroupId(getChatId()) == supergroupId) {
                checkLinkedChat()
                if (messageSenderButton != null) {
                    messageSenderButton!!.setInSlowMode(tdlib!!.inSlowMode(getChatId()))
                }
            }
        })
    }

    override fun onBasicGroupUpdated(basicGroup: BasicGroup, migratedToSupergroup: Boolean) {
        tdlib!!.ui().post(Runnable {
            if (toBasicGroupId(getChatId()) == basicGroup.id) {
                if (migratedToSupergroup) {
                    val newChatId = fromSupergroupId(basicGroup.upgradedToSupergroupId)
                    tdlib!!.chat(newChatId, RunnableData { ignored: Chat? ->
                        tdlib!!.uiExecute(Runnable {
                            if (manager != null) {
                                manager.destroy(this)
                            }
                            setArguments(Arguments(tdlib, openedFromChatList, tdlib!!.chatStrict(newChatId), null, null, null))
                        })
                    })
                } else {
                    updateBottomBar(true)
                }
            }
        })
    }

    override fun onBasicGroupFullUpdated(basicGroupId: Long, basicGroupFull: BasicGroupFullInfo?) {
        tdlib!!.ui().post(Runnable {
            if (chat != null && toBasicGroupId(this.headerChatId) == basicGroupId) {
                headerCell!!.updateUserStatus(chat)
            }
        })
    }

    override fun onChatOnlineMemberCountChanged(chatId: Long, onlineMemberCount: Int) {
        tdlib!!.ui().post(Runnable {
            if (chat != null && this.headerChatId == chatId) {
                headerCell!!.updateUserStatus(chat)
            }
        })
    }

    override fun onChatMessageTtlSettingChanged(chatId: Long, messageAutoDeleteTime: Int) {
        tdlib!!.ui().post(Runnable {
            if (chat != null && chat!!.id == chatId) {
                tdlib!!.ui().updateTTLButton(R.id.menu_secretChat, headerView, chat, false)
            }
        })
    }

    override fun onSecretChatUpdated(secretChat: SecretChat) {
        tdlib!!.ui().post(Runnable {
            if (chat != null && TD.getSecretChatId(chat) == secretChat.id) {
                updateBottomBar(true)
                if (secretChat.state.getConstructor() == TdApi.SecretChatStateClosed.CONSTRUCTOR) {
                    checkActionBar()
                }
            }
        })
    }

    override fun onNotificationSettingsChanged(scope: NotificationSettingsScope, settings: ScopeNotificationSettings?) {
        tdlib!!.ui().post(Runnable {
            if (tdlib!!.chatType(getChatId())!!.matchesScope(scope)) {
                updateCounters(true)
            }
        })
    }

    override fun onNotificationSettingsChanged(chatId: Long, settings: ChatNotificationSettings?) {
        tdlib!!.ui().post(Runnable {
            if (this.headerChatId == chatId) {
                headerCell!!.setShowMute(TD.needMuteIcon(settings, tdlib!!.scopeNotificationSettings(chatId)))
            }
            if (getChatId() == chatId) {
                updateCounters(true)
            }
        })
    }

    // Raise to speak / listen utils
    private fun registerRaiseListener() {
        if (canWriteMessages()) {
            // FIXME RaiseHelper.instance().register(this);
        }
    }

    private fun unregisterRaiseListener() {
        // FIXME RaiseHelper.instance().unregister(this);
    }

    override fun enterRaiseMode(): Boolean {
        val context = context()
        if (context.isPasscodeShowing()) {
            return false
        }
        /*if (Player.instance().currentIsVoice()) {
      Player.instance().playIfRequested();
      return true;
    }*/
        val audio: ArrayList<TGAudio?>? = null // collectAudios(true, true);
        if (audio != null && audio.size != 0) {
            // Player.instance().playPause(audio.get(0), true);
            return true
        }
        return context.getRecordAudioVideoController().enterRaiseRecordMode()
    }

    override fun leaveRaiseMode(): Boolean {
        /*if (Player.instance().currentIsVoice()) {
      Player.instance().pauseIfPlaying();
    }*/
        context().getRecordAudioVideoController().leaveRaiseRecordMode()
        return true
    }

    // Messages search
    private var searchControlsLayout: FrameLayoutFix? = null
    private var searchControlsReveal: RippleRevealView? = null
    private var searchByButton: ImageView? = null
    private var searchJumpToDateButton: ImageView? = null
    private var searchCounterView: TextView? = null
    private var searchProgressView: ProgressComponentView? = null
    private var searchSetTypeFilterButton: ImageView? = null
    private var searchShowOnlyFoundButton: ImageView? = null
    private var searchByAvatarView: AvatarView? = null

    private fun canSetSearchFilteredMode(): Boolean {
        return !this.isEventLog && chat != null && (searchMessagesSender != null || searchMessagesFilterIndex != 0 || !isEmpty(getLastMessageSearchQuery()))
    }

    private fun canSearchByUserId(): Boolean {
        return !this.isEventLog && chat != null && !tdlib!!.isDirectMessagesChat(chat!!.id) && (tdlib!!.isMultiChat(chat!!.id) || tdlib!!.isUserChat(chat!!.id))
    }

    private fun checkSearchByVisible() {
        setSearchByVisible(canSearchByUserId())
    }

    private fun checkSearchFilteredModeButton(animated: Boolean) {
        val isVisible = canSetSearchFilteredMode()
        if (searchShowOnlyFoundButton != null && searchCounterView != null && searchProgressView != null) {
            searchShowOnlyFoundButton!!.setVisibility(if (isVisible) View.VISIBLE else View.GONE)
            searchCounterView!!.setTranslationX((if (isVisible) 0 else Screen.dp(42.5f)).toFloat())
            searchProgressView!!.setTranslationX((if (isVisible) 0 else Screen.dp(42.5f)).toFloat())
        }
        if (!isVisible) {
            onSetSearchFilteredShowMode(false)
        }
    }

    private fun setSearchByVisible(isVisible: Boolean) {
        searchByButton!!.setVisibility(if (isVisible) View.VISIBLE else View.GONE)
        searchSetTypeFilterButton!!.setTranslationX((if (isVisible) 0 else Screen.dp(-42.5f)).toFloat())
    }

    private var jumpToDateRequest: TdApi.Function<*>? = null

    private fun cancelJumpToDate() {
        jumpToDateRequest = null
    }

    private fun jumpToDate() {
        val msg = manager!!.findBottomMessage()

        val currentTime = if (msg != null) msg.getDate() * 1000L else System.currentTimeMillis()
        val c = Calendar.getInstance()

        c.set(Calendar.YEAR, 2013)
        c.set(Calendar.MONTH, Calendar.AUGUST)
        c.set(Calendar.DAY_OF_MONTH, 14)
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        val sourceMinDate = c.getTimeInMillis()

        var minDate = manager.getMinimumDate().toLong()
        val maxDate = System.currentTimeMillis() // manager.getMaximumDate();

        if (minDate == -1L) {
            minDate = sourceMinDate
        } else {
            minDate *= 1000L
        }


        /*if (maxDate == -1) {
      maxDate = System.currentTimeMillis();
    } else {
      maxDate *= 1000l;
    }*/
        c.setTimeInMillis(max(minDate, min(maxDate, currentTime)))

        val year = c.get(Calendar.YEAR)
        val monthOfYear = c.get(Calendar.MONTH)
        val dayOfMonth = c.get(Calendar.DAY_OF_MONTH)
        val datePickerDialog =
            DatePickerDialog(context(), Theme.dialogTheme(), OnDateSetListener { view: DatePicker?, year1: Int, month: Int, dayOfMonth1: Int ->
                val c1 = Calendar.getInstance()
                c1.set(Calendar.YEAR, year1)
                c1.set(Calendar.MONTH, month)
                c1.set(Calendar.DAY_OF_MONTH, dayOfMonth1)
                c1.set(Calendar.HOUR_OF_DAY, 0)
                c1.set(Calendar.MINUTE, 0)
                c1.set(Calendar.SECOND, 0)
                c1.set(Calendar.MILLISECOND, 0)
                jumpToDate((c1.getTimeInMillis() / 1000L).toInt())
            }, year, monthOfYear, dayOfMonth)
        datePickerDialog.setButton(
            DialogInterface.BUTTON_NEUTRAL,
            Lang.getString(R.string.Beginning),
            DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int -> jumpToDate(0) })

        datePickerDialog.getDatePicker().setMaxDate(maxDate)
        try {
            datePickerDialog.getDatePicker().setMinDate(minDate)
        } catch (ignored: Throwable) {
            if (minDate != sourceMinDate) {
                try {
                    datePickerDialog.getDatePicker().setMinDate(sourceMinDate)
                } catch (ignored1: Throwable) {
                }
            }
        }

        ViewSupport.showDatePicker(datePickerDialog)
    }

    override fun handleLanguageDirectionChange() {
        super.handleLanguageDirectionChange()
        if (needTabs()) {
            if (pagerContentView != null) {
                pagerContentView!!.checkRtl()
            }
        }
        if (inputView != null) {
            inputView!!.checkRtl()
        }
        Views.setScrollBarPosition(messagesView)
        if (wallpapersList != null) {
            (wallpapersList!!.getLayoutManager() as LinearLayoutManager).setReverseLayout(Lang.rtl())
            wallpapersList!!.invalidateItemDecorations()
            (wallpapersList!!.getAdapter() as WallpaperAdapter).centerWallpapers(false)
        }
    }

    public override fun handleLanguagePackEvent(event: Int, arg1: Int) {
        if (emojiLayout != null) {
            emojiLayout!!.onLanguagePackEvent(event, arg1)
        }
        if (isFocused() && !inPreviewMode()) {
            manager!!.rebuildLayouts()
            if (headerCell != null) {
                headerCell!!.setChat(tdlib, chat, messageThread)
                if (messageThread != null) {
                    updateMessageThreadSubtitle()
                } else {
                    updateForumTopicHeader()
                }
            }
        }
    }

    // Forum topic header: when a forum topic is opened (messageTopicId = MessageTopicForum,
    // no messageThread), show the topic name as the header title instead of the chat name.
    private fun updateForumTopicHeader() {
        if (messageThread != null || headerCell == null) {
            return
        }
        val topic = getMessageTopicId()
        if (topic !is MessageTopicForum) {
            return
        }
        val forumTopicId = topic.forumTopicId
        val chatId = getChatId()
        val info = tdlib!!.forumTopicInfo(chatId, forumTopicId.toLong())
        if (info != null) {
            applyForumTopicHeader(info)
        } else {
            tdlib!!.getForumTopic(chatId, forumTopicId, RunnableData { forumTopic: ForumTopic? ->
                if (forumTopic != null && forumTopic.info != null) {
                    runOnUiThreadOptional(Runnable { applyForumTopicHeader(forumTopic.info) })
                }
            }, null)
        }
    }

    private fun applyForumTopicHeader(info: ForumTopicInfo) {
        if (headerCell == null) {
            return
        }
        headerCell!!.setTitle(info.name)
        headerCell!!.setForcedSubtitle(tdlib!!.chatTitle(getChatId()))
    }

    fun jumpToBeginningOfTheDay(date: Int) {
        val c = Calendar.getInstance()
        c.setTimeInMillis(date.toLong() * 1000L)
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        jumpToDate((c.getTimeInMillis() / 1000L).toInt())
    }

    fun jumpToDate(date: Int) {
        val inSearchMode = canSetSearchFilteredMode()
        if (date == 0) {
            val messageId = MessageId(getChatId(), MessageId.MIN_VALID_ID)
            jumpToDateRequest = null
            setSearchInProgress(false, true)
            if (inSearchMode) {
                manager!!.searchMoveToMessage(messageId)
                return
            }
            manager!!.highlightMessage(messageId, MessagesManager.HIGHLIGHT_MODE_NORMAL, null, true)
            return
        }
        val request = GetChatMessageByDate(getChatId(), 0)
        jumpToDateRequest = request
        request.date = date
        setSearchInProgress(true, true)
        tdlib!!.client().send(request, Client.ResultHandler { `object`: TdApi.Object? ->
            val messageId: MessageId
            val message: TdApi.Message?
            if (`object`!!.getConstructor() == TdApi.Message.CONSTRUCTOR) {
                message = `object` as TdApi.Message
                messageId = MessageId(message.chatId, message.id)
            } else {
                message = null
                messageId = MessageId(getChatId(), MessageId.MIN_VALID_ID)
            }
            tdlib!!.ui().post(Runnable {
                if (jumpToDateRequest === request) {
                    jumpToDateRequest = null
                    setSearchInProgress(false, true)
                    if (inSearchMode) {
                        manager!!.searchMoveToMessage(messageId)
                        return@Runnable
                    }
                    manager!!.highlightMessage(
                        messageId,
                        if (messageId.isHistoryStart()) MessagesManager.HIGHLIGHT_MODE_NORMAL else MessagesManager.HIGHLIGHT_MODE_NORMAL_NEXT,
                        null,
                        true
                    )
                }
            })
        })
    }

    private fun initSearchControls() {
        if (searchControlsLayout != null) {
            return
        }

        val onClickListener = View.OnClickListener { v: View? ->
            val viewId = v!!.getId()
            if (viewId == R.id.btn_search_setTypeFilter) {
                showSearchTypeOptions()
            } else if (viewId == R.id.btn_search_by) {
                showSearchByUserView(true, true)
            } else if (viewId == R.id.btn_search_counter) {
                // TODO open search by text
            } else if (viewId == R.id.btn_search_jump) {
                jumpToDate()
            } else if (viewId == R.id.btn_search_onlyResult) {
                toggleSearchFilteredShowMode()
            }
        }

        val context: Context = context()

        val rp: RelativeLayout.LayoutParams?
        var fp: FrameLayout.LayoutParams?

        rp = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        rp.addRule(RelativeLayout.ALIGN_TOP, R.id.msg_bottom)
        rp.addRule(RelativeLayout.ALIGN_BOTTOM, R.id.msg_bottom)

        searchControlsLayout = object : FrameLayoutFix(context) {
            override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
                return getSearchTransformFactor() == 0f || super.onInterceptTouchEvent(ev)
            }

            override fun onTouchEvent(event: MotionEvent?): Boolean {
                return getSearchTransformFactor() > 0f
            } /*@Override
      protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(heightMeasureSpec), MeasureSpec.EXACTLY));
      }*/
        }
        searchControlsLayout!!.setMinimumHeight(Screen.dp(49f))
        searchControlsLayout!!.setLayoutParams(rp)

        searchControlsReveal = RippleRevealView(context)
        searchControlsReveal!!.setLayoutParams(newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        searchControlsLayout!!.addView(searchControlsReveal)
        Views.setPaddingBottom(searchControlsReveal, extraBottomInset)
        addThemeInvalidateListener(searchControlsReveal)

        fp = newParams(Screen.dp(52f), Screen.dp(49f), Gravity.LEFT or Gravity.CENTER_VERTICAL)
        fp.leftMargin = Screen.dp(42.5f)
        searchByButton = Views.newImageButton(context, R.drawable.baseline_person_24, ColorId.icon, this)
        searchByButton!!.setId(R.id.btn_search_by)
        searchByButton!!.setOnClickListener(onClickListener)
        searchByButton!!.setLayoutParams(fp)
        searchControlsLayout!!.addView(searchByButton)

        searchByAvatarView = AvatarView(context)
        searchByAvatarView!!.setId(R.id.btn_search_by)
        searchByAvatarView!!.setOnClickListener(onClickListener)
        searchByAvatarView!!.setLayoutParams(fp)
        searchByAvatarView!!.setPadding(Screen.dp(16f), Screen.dp(14.5f), Screen.dp(16f), Screen.dp(14.5f))
        searchControlsLayout!!.addView(searchByAvatarView)

        fp = newParams(Screen.dp(52f), Screen.dp(49f), Gravity.LEFT or Gravity.CENTER_VERTICAL)
        searchJumpToDateButton = Views.newImageButton(context, R.drawable.baseline_date_range_24, ColorId.icon, this)
        searchJumpToDateButton!!.setId(R.id.btn_search_jump)
        searchJumpToDateButton!!.setOnClickListener(onClickListener)
        searchJumpToDateButton!!.setLayoutParams(fp)
        searchControlsLayout!!.addView(searchJumpToDateButton)

        fp = newParams(Screen.dp(52f), Screen.dp(49f), Gravity.LEFT or Gravity.CENTER_VERTICAL)
        fp.leftMargin = Screen.dp(42.5f * 2)
        searchSetTypeFilterButton = Views.newImageButton(context, R.drawable.baseline_filter_variant_remove_24, ColorId.icon, this)
        searchSetTypeFilterButton!!.setId(R.id.btn_search_setTypeFilter)
        searchSetTypeFilterButton!!.setOnClickListener(onClickListener)
        searchSetTypeFilterButton!!.setLayoutParams(fp)
        searchControlsLayout!!.addView(searchSetTypeFilterButton)

        fp = newParams(Screen.dp(52f), Screen.dp(49f), Gravity.RIGHT or Gravity.CENTER_VERTICAL)
        searchShowOnlyFoundButton = Views.newImageButton(context, R.drawable.baseline_text_search_variant_24, ColorId.icon, this)
        searchShowOnlyFoundButton!!.setId(R.id.btn_search_onlyResult)
        searchShowOnlyFoundButton!!.setOnClickListener(onClickListener)
        searchShowOnlyFoundButton!!.setPadding(0, 0, Screen.dp(12f), 0)
        searchShowOnlyFoundButton!!.setLayoutParams(fp)
        searchControlsLayout!!.addView(searchShowOnlyFoundButton)

        val padding = Screen.dp(22f)
        fp = newParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.RIGHT or Gravity.CENTER_VERTICAL)
        fp.rightMargin = Screen.dp(60f)
        fp.leftMargin = Screen.dp(5f) + padding
        searchCounterView = object : NoScrollTextView(context) {
            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec)
                setPivotX((getMeasuredWidth() - padding).toFloat())
                setPivotY((getMeasuredHeight() / 2).toFloat())
            }
        }
        searchCounterView!!.setId(R.id.btn_search_counter)
        // TODO searchCounterView.setOnClickListener(onClickListener);
        searchCounterView!!.setSingleLine(true)
        searchCounterView!!.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f)
        searchCounterView!!.setEllipsize(TextUtils.TruncateAt.MIDDLE)
        searchCounterView!!.setTypeface(Fonts.getRobotoMedium())
        searchCounterView!!.setTextColor(Theme.textAccentColor())
        addThemeTextAccentColorListener(searchCounterView)
        searchCounterView!!.setLayoutParams(fp)
        searchControlsLayout!!.addView(searchCounterView)

        fp = newParams(Screen.dp(49f), Screen.dp(49f), Gravity.RIGHT or Gravity.CENTER_VERTICAL)
        fp.rightMargin = Screen.dp(60f)
        searchProgressView = ProgressComponentView(context)
        searchProgressView!!.initCustom(4.5f, 0f, 10f)
        searchProgressView!!.setLayoutParams(fp)
        searchProgressView!!.forceFactor(1f)
        searchControlsLayout!!.addView(searchProgressView)
        addThemeInvalidateListener(searchProgressView)

        setSearchInProgress(0f)
        setSearchControlsFactor(0f)
        updateSearchControlsInset()
    }

    private var searchControlsForChannel = false

    private fun resetSearchControls(animated: Boolean = false) {
        lastMessageSearchQuery = ""
        lastMembersSearchQuery = ""
        onSetSearchTypeFilter(0)
        onSetSearchMessagesSenderId(null)
        onSetSearchFilteredShowMode(false)
        showSearchByUserView(false, animated)
        if (searchControlsLayout != null) {
            searchCounterView!!.setText("")
            setSearchInProgress(false, animated)
            checkSearchByVisible()
            checkSearchFilteredModeButton(animated)
            setSearchControlsFactor(0f)
            updateSearchNavigation()

            val isChannel = tdlib!!.isChannelChat(chat)
            if (searchControlsForChannel != isChannel) {
                searchControlsForChannel = isChannel

                val rp = searchControlsLayout!!.getLayoutParams() as RelativeLayout.LayoutParams
                if (isChannel) {
                    rp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                    rp.addRule(RelativeLayout.ALIGN_BOTTOM, 0)
                    rp.addRule(RelativeLayout.ALIGN_TOP, 0)
                    rp.height = Screen.dp(48f) + extraBottomInset
                } else {
                    rp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0)
                    rp.addRule(RelativeLayout.ALIGN_BOTTOM, R.id.msg_bottom)
                    rp.addRule(RelativeLayout.ALIGN_TOP, R.id.msg_bottom)
                    rp.height = ViewGroup.LayoutParams.MATCH_PARENT
                }

                searchControlsLayout!!.setLayoutParams(rp)
            }
        }
        if (goToPrevFoundMessageButtonBadge != null && goToNextFoundMessageButtonBadge != null) {
            goToNextFoundMessageButtonBadge!!.setEnabled(false, animated)
            goToPrevFoundMessageButtonBadge!!.setEnabled(false, animated)
            goToNextFoundMessageButtonBadge!!.setInProgress(false)
            goToPrevFoundMessageButtonBadge!!.setInProgress(false)
        }
        searchNavigationButtonVisibleAnimator.setValue(false, animated)
    }

    private var searchInProgress = false
    private var searchProgressAnimator: FactorAnimator? = null
    private fun setSearchInProgress(inProgress: Boolean, animated: Boolean): Boolean {
        if (this.searchInProgress != inProgress || !animated) {
            val oldValue = this.searchInProgress
            this.searchInProgress = inProgress
            val toFactor = if (inProgress) 1f else 0f
            if (animated) {
                if (searchProgressAnimator == null) {
                    searchProgressAnimator = FactorAnimator(ANIMATOR_SEARCH_PROGRESS, this, DECELERATE_INTERPOLATOR, 90L, this.searchInProgressFactor)
                }
                searchProgressAnimator!!.animateTo(toFactor)
            } else {
                if (searchProgressAnimator != null) {
                    searchProgressAnimator!!.forceFactor(toFactor)
                }
                setSearchInProgress(toFactor)
            }
            return oldValue != inProgress
        }
        return false
    }

    private var searchInProgressFactor = -1f

    private fun setSearchInProgress(factor: Float) {
        if (this.searchInProgressFactor != factor) {
            this.searchInProgressFactor = factor
            updateSearchNavigation()
        }
    }

    private fun updateSearchNavigation() {
        if (searchControlsLayout != null) {
            val maxAlpha = searchControlsFactor

            val counterAlpha = maxAlpha * (1f - searchInProgressFactor)
            val counterScale = .6f + .4f * (1f - searchInProgressFactor)
            val progressAlpha = maxAlpha * searchInProgressFactor
            val progressScale = .6f + .4f * searchInProgressFactor

            searchCounterView!!.setAlpha(counterAlpha)
            searchCounterView!!.setScaleX(counterScale)
            searchCounterView!!.setScaleY(counterScale)

            searchProgressView!!.setAlpha(progressAlpha)
            searchProgressView!!.setScaleX(progressScale)
            searchProgressView!!.setScaleY(progressScale)
        }
    }

    private var searchControlsFactor = -1f

    private fun setSearchControlsFactor(factor: Float) {
        setBadgesButtonsTranslationX(factor, searchNavigationButtonVisibleAnimator.getFloatValue())
        updateBottomBarStyle()
        if (this.searchControlsLayout != null) {
            if (searchControlsFactor != factor) {
                searchControlsFactor = factor
                searchJumpToDateButton!!.setAlpha(factor)
                searchByButton!!.setAlpha(factor)
                searchShowOnlyFoundButton!!.setAlpha(factor)
                searchByAvatarView!!.setAlpha(factor)
                searchSetTypeFilterButton!!.setAlpha(factor)
                updateSearchNavigation()
            }
            val translate = needSearchControlsTranslate()
            searchControlsReveal!!.setRevealFactor(if (translate) 1f else factor)
            searchControlsLayout!!.setTranslationY(if (translate) (Screen.dp(49f) + extraBottomInset) * (1f - factor) else 0f)
            if (translate) {
                checkScrollButtonOffsets()
            }
        }
    }

    private fun needSearchControlsTranslate(): Boolean {
        return tdlib!!.isChannelChat(chat) && !canWriteMessages()
    }

    private val searchControlsOffset: Float
        get() = if (needSearchControlsTranslate() && searchControlsFactor != -1f) (Screen.dp(49f) + extraBottomInset) * (searchControlsFactor) else 0f

    protected override fun getSearchMenuId(): Int {
        return R.id.menu_clear
    }

    override fun useGraySearchHeader(): Boolean {
        return true
    }

    override fun applySearchTransformFactor(factor: Float, isOpening: Boolean) {
        super.applySearchTransformFactor(factor, isOpening)
        topBar!!.setGlobalVisibility(1f - factor, false)
        updateReplyView()
        setSearchControlsFactor(factor)
        // checkBottomOffsetFactor();
    }

    protected override fun onEnterSearchMode() {
        super.onEnterSearchMode()
        checkAttachedFiles(true)
        manager!!.onPrepareToSearch()
        if (searchByUserViewWrapper != null) {
            searchByUserViewWrapper!!.prepare()
        }
    }

    protected override fun onLeaveSearchMode() {
        if (searchFromUserMessageId != null) {
            manager!!.setHighlightMessageId(searchFromUserMessageId, MessagesManager.HIGHLIGHT_MODE_WITHOUT_HIGHLIGHT)
        }
        onSetSearchFilteredShowMode(false)
        manager!!.onDestroySearch()
        manager.getAdapter().checkAllMessages()
        searchMedia(null)
    }

    protected override fun getHeaderColorId(): Int {
        if (previewSearchSender != null) {
            return ColorId.filling
        }
        return super.getHeaderColorId()
    }

    override fun onAfterLeaveSearchMode() {
        super.onAfterLeaveSearchMode()
        checkAttachedFiles(true)
        resetSearchControls(true)
        if (searchByUserViewWrapper != null) {
            searchByUserViewWrapper!!.dismiss()
        }
    }

    private fun moveSearchSelection(next: Boolean) {
        manager!!.moveToNextResult(next)
    }

    // Search counter
    private val showingSearchIndex = false
    private var lastSearchIndex = -1
    private var lastSearchTotalCount = -1

    fun onChatSearchStarted() {
        if (setSearchInProgress(true, true)) {
            lastSearchTotalCount = -1
            lastSearchIndex = lastSearchTotalCount
        }
    }

    fun onChatSearchAwaitNext(next: Boolean) {
        goToNextFoundMessageButtonBadge!!.setInProgress(next)
        goToPrevFoundMessageButtonBadge!!.setInProgress(!next)
    }

    fun onChatSearchFinished(counter: String?, index: Int, totalCount: Int) {
        lastSearchIndex = index
        lastSearchTotalCount = totalCount
        searchCounterView!!.setText(counter)
        setSearchInProgress(false, true)
        updateSearchNavigation()
        goToNextFoundMessageButtonBadge!!.setEnabled(manager!!.canSearchNext(), true)
        goToPrevFoundMessageButtonBadge!!.setEnabled(manager.canSearchPrev(), true)
        goToNextFoundMessageButtonBadge!!.setInProgress(false)
        goToPrevFoundMessageButtonBadge!!.setInProgress(false)
        searchNavigationButtonVisibleAnimator.setValue((index < totalCount) || (index > 0), true)
        manager.getAdapter().checkAllMessages()
    }

    private var lastMediaSearchQuery: String? = null

    private fun iterateMediaTabs(callback: RunnableData<SharedBaseController<*>?>) {
        if (pagerContentAdapter != null) {
            val adapter = pagerContentAdapter!!
            val size = adapter.cachedItems.size()
            for (i in 0..<size) {
                val c = adapter.cachedItems.valueAt(i)
                if (c != null) {
                    callback.runWithData(c)
                }
            }
        }
    }

    private fun searchMedia(query: String?) {
        if (pagerContentAdapter != null && !lastMediaSearchQuery.equalsOrBothEmpty(query)) {
            lastMediaSearchQuery = query
            iterateMediaTabs(RunnableData { c: SharedBaseController<*>? -> c!!.search(query) }
            )
        }
    }

    private fun clearChatSearchInput() {
        clearSearchInput()
        // TODO clear input on first tap, clear "From: " on second tap
    }

    private var triggerOneShot = false

    override fun allowLeavingSearchMode(): Boolean {
        return triggerOneShot
    }

    override fun modifySearchHeaderView(headerEditText: HeaderEditText) {
        headerEditText.setInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)
        val padding = Screen.dp(112f)
        if (Lang.rtl()) {
            headerEditText.setPadding(padding, headerEditText.getPaddingTop(), headerEditText.getPaddingRight(), headerEditText.getPaddingBottom())
        } else {
            headerEditText.setPadding(headerEditText.getPaddingLeft(), headerEditText.getPaddingTop(), padding, headerEditText.getPaddingBottom())
        }
    }


    // Locale change
    /*TODO LANG
  @Override
  public void onLocaleChange () {
    super.onLocaleChange();
    if (chat != null) {
      if (headerCell != null) {
        headerCell.setChat(tdlib, chat);
      }
    }
    if (manager != null) {
      MessagesAdapter adapter = manager.getAdapter();
      if (adapter != null && adapter.updateLocale()) {
        messagesView.invalidateAll();
      }
    }
  }*/
    // Force preview
    override fun onPrepareForceTouchContext(context: ForceTouchContext) {
        val messageThread = this.messageThread
        val boundChatId = if (messageThread != null) messageThread.getContextChatId() else getChatId()
        context.setIsMatchParent(true)
        context.setBoundChatId(boundChatId, messageThread)
        context.setAllowFullscreen(true)
        context.setAnimationType(ForceTouchContext.ANIMATION_TYPE_EXPAND_VERTICALLY)
    }

    val chatMessageSender: MessageSender?
        get() = chat!!.messageSenderId

    fun canSelectSender(): Boolean {
        return chat != null && chat!!.messageSenderId != null
    }

    val isCameraButtonVisibleOnAttachPanel: Boolean
        get() = !canSelectSender()

    fun createHapticSenderItem(id: Int, sender: MessageSender?, useUsername: Boolean, isLocked: Boolean): HapticMenuHelper.MenuItem {
        val title = if (useUsername) tdlib!!.senderName(sender!!) else Lang.getString(R.string.SendAs)
        if (tdlib!!.isSelfSender(sender)) {
            return HapticMenuHelper.MenuItem(id, title, Lang.getString(R.string.YourAccount), R.drawable.dot_baseline_acc_personal_24, tdlib, sender, false)
        } else if (!tdlib!!.isChannel(sender)) {
            return HapticMenuHelper.MenuItem(id, title, Lang.getString(R.string.AnonymousAdmin), R.drawable.dot_baseline_acc_anon_24, tdlib, sender, false)
        } else {
            val username = tdlib!!.chatUsername(sender.getSenderId())
            val subtitle = if (useUsername && !isEmpty(username)) ("@" + username) else tdlib!!.getMessageSenderTitle(sender)
            return HapticMenuHelper.MenuItem(id, title, subtitle, 0, tdlib, sender, isLocked)
        }
    }

    private fun setNewMessageSender(sender: ChatMessageSender) {
        tdlib!!.send<TdApi.Ok?>(SetChatMessageSender(getChatId(), sender.sender), tdlib!!.typedOkHandler())
    }

    private fun setNewMessageSender(sender: MessageSender?) {
        tdlib!!.send<TdApi.Ok?>(SetChatMessageSender(getChatId(), sender), tdlib!!.typedOkHandler())
    }

    private fun getChatAvailableMessagesSenders(after: Runnable?) {
        getChatAvailableMessagesSenders(tdlib!!, getChatId(), RunnableData { result: ChatMessageSenders? ->
            onUpdateChatAvailableMessagesSenders(result!!.senders)
            if (after != null) {
                tdlib!!.ui().post(after)
            }
        })
    }


    // Interface
    private fun updateSelectMessageSenderInterface(animated: Boolean) {
        val cameraVisible = this.isCameraButtonVisibleOnAttachPanel
        val canSetSender = canSelectSender()
        if (cameraButton != null) {
            cameraButton!!.setVisibility(if (cameraVisible) View.VISIBLE else View.GONE) //.setVisible(cameraVisible);
        }

        messageSenderButton!!.setVisibility(if (canSetSender && attachButtons!!.getVisibility() == View.VISIBLE) View.VISIBLE else View.GONE)
        if (canSetSender) {
            messageSenderButton!!.update(this.chatMessageSender, animated)
        }
    }


    private var clearCurrentNonAnonymousActionHash: CancellableRunnable? = null
    private var currentNonAnonymousActionHash: Long = 0


    fun callNonAnonymousProtection(hash: Long, message: TGMessage, locationProvider: TooltipOverlayView.LocationProvider): Boolean {
        return callNonAnonymousProtection(hash, message.findCurrentView(), locationProvider)
    }

    fun callNonAnonymousProtection(hash: Long, view: View?, locationProvider: TooltipOverlayView.LocationProvider): Boolean {
        return callNonAnonymousProtection(hash, context.tooltipManager().builder(view).locate(locationProvider))
    }

    fun callNonAnonymousProtection(hash: Long, tooltipBuilder: TooltipBuilder?): Boolean {
        if (chat == null || tdlib!!.isSelfSender(chat!!.messageSenderId) || (chat!!.messageSenderId == null && !tdlib!!.isAnonymousAdmin(chat!!.id))) {
            return true
        }

        if (currentNonAnonymousActionHash == hash) {
            currentNonAnonymousActionHash = 0
            if (clearCurrentNonAnonymousActionHash != null) {
                clearCurrentNonAnonymousActionHash!!.cancel()
                clearCurrentNonAnonymousActionHash = null
            }
            return true
        }

        if (clearCurrentNonAnonymousActionHash != null) {
            clearCurrentNonAnonymousActionHash!!.cancel()
            clearCurrentNonAnonymousActionHash = null
        }

        currentNonAnonymousActionHash = hash
        clearCurrentNonAnonymousActionHash = object : CancellableRunnable() {
            public override fun act() {
                currentNonAnonymousActionHash = 0
                clearCurrentNonAnonymousActionHash = null
            }
        }

        tdlib!!.ui().postDelayed(clearCurrentNonAnonymousActionHash!!, 10000)

        if (tooltipBuilder != null) {
            tooltipBuilder
                .show(tdlib, Lang.getString(R.string.AnonWarning))
                .hideDelayed(2000, TimeUnit.MILLISECONDS)
        } else {
            UI.showToast(Lang.getString(R.string.AnonWarning), Toast.LENGTH_LONG)
        }

        return false
    }


    // Updates
    private var chatAvailableSenders: Array<ChatMessageSender?>? = null
    private fun onUpdateChatAvailableMessagesSenders(senders: Array<ChatMessageSender?>?) {
        tdlib!!.ui().post(Runnable {
            this.chatAvailableSenders = senders
        })
    }


    //
    private fun openSetSenderPopup() {
        getChatAvailableMessagesSenders(Runnable {
            val c = SetSenderController(context, tdlib)
            c.setArguments(SetSenderController.Args(chat, chatAvailableSenders, chat!!.messageSenderId))
            c.setDelegate(SetSenderControllerPage.Delegate { sender: ChatMessageSender? -> this.setNewMessageSender(sender!!) })
            c.show()
            hideCursorsForInputView()
        })
    }


    //
    private val hapticSendAsMenuProvider: HapticMenuHelper.Provider = object : HapticMenuHelper.Provider {
        override fun onCreateHapticMenu(view: View?): MutableList<HapticMenuHelper.MenuItem?> {
            if (chatAvailableSenders == null || chatAvailableSenders!!.size == 0) return ArrayList<HapticMenuHelper.MenuItem?>()

            val maxCount = 5
            val needMoreButton = chatAvailableSenders!!.size > maxCount
            val senderButtonsCount = if (needMoreButton) maxCount - 1 else chatAvailableSenders!!.size
            val items: MutableList<HapticMenuHelper.MenuItem?> = ArrayList<HapticMenuHelper.MenuItem?>(min(chatAvailableSenders!!.size, maxCount))

            for (i in 0..<senderButtonsCount) {
                items.add(
                    0,
                    createHapticSenderItem(
                        R.id.btn_setMsgSender,
                        chatAvailableSenders!![i]!!.sender,
                        true,
                        chatAvailableSenders!![i]!!.needsPremium && !tdlib!!.hasPremium()
                    )
                )
            }

            if (needMoreButton) {
                items.add(
                    0,
                    HapticMenuHelper.MenuItem(R.id.btn_openSendersMenu, Lang.getString(R.string.MoreMessageSenders), R.drawable.baseline_more_horiz_24)
                )
            }

            hideCursorsForInputView()
            return items
        }

        override fun getAnchorMode(view: View?): Int {
            return MenuMoreWrap.ANCHOR_MODE_CENTER
        }
    }

    fun isMessageFound(message: TdApi.Message): Boolean {
        if (foundMessageId != null && foundMessageId!!.messageId == message.id) {
            return true
        }
        return manager!!.isMessageFound(message)
    }

    // Search Input Callbacks
    private var lastMessageSearchQuery: String? = ""
    var lastMembersSearchQuery: String = ""
        private set

    fun getLastMessageSearchQuery(): String? {
        return if (!isEmpty(previewSearchQuery)) previewSearchQuery else lastMessageSearchQuery
    }

    override fun onSearchInputChanged(query: String) {
        if (searchMode == SEARCH_MODE_MESSAGES) {
            if (query != lastMessageSearchQuery) {
                searchChatMessages(query)
            }
            lastMessageSearchQuery = query
            checkSearchFilteredModeButton(false)
        } else if (searchMode == SEARCH_MODE_USERS) {
            if (query != lastMembersSearchQuery) {
                searchChatMembers(query)
            }
            lastMembersSearchQuery = query
        }
    }

    private fun searchChatMessages(query: String?) {
        if (chat == null) return
        if (searchMessagesFilterMode) {
            applyQueryForManagerInFilteredShowMode(query)
        }

        manager!!.search(
            chat!!.id,
            messageThread,
            messageTopicId,
            searchMessagesSender,
            searchFiltersTdApi[searchMessagesFilterIndex],
            chat!!.type.getConstructor() == TdApi.ChatTypeSecret.CONSTRUCTOR,
            query,
            foundMessageId
        )
        foundMessageId = null
        searchMedia(query)
        manager.getAdapter().checkAllMessages()
    }

    private fun searchChatMembers(query: String?) {
        searchByUserViewWrapper!!.search(query)
    }


    private var searchMode = 0

    private var searchByUserViewWrapper: ChatSearchMembersView? = null
    private var isSearchByUserContentVisible = false
    private var searchByUserTransformFactor = 0f
    private val searchByUserTransformAnimator = BoolAnimator(ANIMATOR_SEARCH_BY_USER, this, DECELERATE_INTERPOLATOR, 200L)
    private val searchNavigationButtonVisibleAnimator = BoolAnimator(ANIMATOR_SEARCH_NAVIGATION, this, DECELERATE_INTERPOLATOR, 200L)

    private fun showSearchByUserView(show: Boolean, animated: Boolean) {
        searchByUserTransformAnimator.setValue(show, animated)
        searchMode = if (show) SEARCH_MODE_USERS else SEARCH_MODE_MESSAGES
        if (show) {
            setSearchInput(this.lastMembersSearchQuery)
        } else {
            setSearchInput(getLastMessageSearchQuery()!!)
        }
    }

    private fun setSearchByUserTransformFactor(factor: Float) {
        if (this.searchByUserTransformFactor != factor) {
            this.searchByUserTransformFactor = factor
            applySearchByUserTransformFactor(factor)
        }
    }

    private fun applySearchByUserTransformFactor(factor: Float) {
        if (searchByUserViewWrapper != null) {
            searchByUserViewWrapper!!.setAlpha(factor)
            setSearchContentVisible(factor != 0f)
        }
    }

    private fun setSearchContentVisible(isVisible: Boolean) {
        if (this.isSearchByUserContentVisible != isVisible) {
            this.isSearchByUserContentVisible = isVisible
            if (searchByUserViewWrapper != null) {
                searchByUserViewWrapper!!.setVisibility(if (isVisible) View.VISIBLE else View.GONE)
            }
        }
    }

    private fun showSearchTypeOptions() {
        showOptions(null, searchFilterIds, searchFilterTexts, null, searchFilterIcons, OptionDelegate { v: View?, id: Int ->
            val index = indexOf(searchFilterIds, id)
            if (index >= 0) {
                onSetSearchTypeFilter(index)
                searchChatMessages(getLastMessageSearchQuery())
            }
            true
        })
    }

    private fun setBadgesButtonsTranslationX(searchFactor: Float, navVisible: Float) {
        val translationX = CircleCounterBadgeView.BUTTON_WRAPPER_WIDTH * searchFactor
        val translationX2 = CircleCounterBadgeView.BUTTON_WRAPPER_WIDTH * (1f - min(searchFactor, navVisible))

        if (scrollToBottomButtonWrap != null) {
            scrollToBottomButtonWrap!!.setTranslationX(translationX)
        }
        if (mentionButtonWrap != null) {
            mentionButtonWrap!!.setTranslationX(translationX)
        }
        if (reactionsButtonWrap != null) {
            reactionsButtonWrap!!.setTranslationX(translationX)
        }
        if (goToNextFoundMessageButtonBadge != null) {
            goToNextFoundMessageButtonBadge!!.setTranslationX(translationX2)
        }
        if (goToPrevFoundMessageButtonBadge != null) {
            goToPrevFoundMessageButtonBadge!!.setTranslationX(translationX2)
        }
    }


    // Search Callbacks
    private var searchMessagesSender: MessageSender? = null
    private var searchMessagesFilterIndex = 0
    private var searchMessagesFilterMode = false

    private fun onSetSearchMessagesSenderId(sender: MessageSender?) {
        searchMessagesSender = sender
        if (searchByUserViewWrapper != null) {
            searchByUserViewWrapper!!.setMessageSender(sender)
        }
        if (searchByAvatarView != null && searchByButton != null) {
            searchByAvatarView!!.setMessageSender(tdlib, sender)
            searchByAvatarView!!.setVisibility(if (sender != null && canSearchByUserId()) View.VISIBLE else View.GONE)
            searchByButton!!.setVisibility(if (sender == null && canSearchByUserId()) View.VISIBLE else View.GONE)
        }

        checkSearchFilteredModeButton(false)
    }

    private fun onSetSearchTypeFilter(index: Int) {
        searchMessagesFilterIndex = index

        if (searchSetTypeFilterButton != null && index < searchFilterIcons.size && index >= 0) {
            searchSetTypeFilterButton!!.setImageResource(searchFilterIcons[index])
            searchSetTypeFilterButton!!.setColorFilter(Theme.getColor(if (index == 0) ColorId.icon else ColorId.iconActive))
        }
        checkSearchFilteredModeButton(false)
    }

    private fun toggleSearchFilteredShowMode() {
        onSetSearchFilteredShowMode(!searchMessagesFilterMode)
    }

    fun inOnlyFoundMode(): Boolean {
        return searchMessagesFilterMode
    }

    private fun onSetSearchFilteredShowMode(inSearchMode: Boolean) {
        // Reopen chat if needed
        if (!inSearchMode && searchMessagesFilterMode) {
            manager!!.openChat(chat, messageThread, messageTopicId, previewSearchFilter, this, areScheduled, !inPreviewMode && !isInForceTouchMode())
        } else if (inSearchMode && !searchMessagesFilterMode) {
            applyQueryForManagerInFilteredShowMode(getLastMessageSearchQuery())
        }

        searchMessagesFilterMode = inSearchMode
        if (searchShowOnlyFoundButton != null) {
            searchShowOnlyFoundButton!!.setColorFilter(Theme.getColor(if (inSearchMode) ColorId.iconActive else ColorId.icon))
        }
    }

    fun inSearchFilteredShowMode(): Boolean {
        return searchMessagesFilterMode
    }

    private fun applyQueryForManagerInFilteredShowMode(query: String?) {
        manager!!.openSearch(chat, query, searchMessagesSender, searchFiltersTdApi[searchMessagesFilterIndex])
    }


    // Controller Callbacks
    override fun onBeforeLeaveSearchMode(): Boolean {
        if (searchMode == SEARCH_MODE_USERS) {
            showSearchByUserView(false, true)
            return false
        }
        if (previewSearchSender != null) {
            navigateBack()
            return false
        }
        return true
    }

    protected override fun needHideKeyboardOnTouchBackButton(): Boolean {
        return searchMode != SEARCH_MODE_USERS
    }


    // Translate
    var translationPopup: TranslationControllerV2.Wrapper? = null

    @JvmOverloads
    fun startTranslateMessages(message: TGMessage, forcePopup: Boolean = false) {
        if (message.translationStyleMode() == Settings.TRANSLATE_MODE_INLINE && (message !is TGMessageBotInfo) && !forcePopup) {
            message.startTranslated()
        } else {
            translationPopup = TranslationControllerV2.Wrapper(context, tdlib, this)
            translationPopup!!.setArguments(TranslationControllerV2.Args(message))
            translationPopup!!.setClickCallback(message.clickCallback())
            translationPopup!!.setTextColorSet(TextColorSets.Regular.NORMAL)
            translationPopup!!.show()
            translationPopup!!.setDismissListener(PopupLayout.DismissListener { popup: PopupLayout? -> translationPopup = null })
            hideCursorsForInputView()
        }
    }

    fun stopTranslateMessages(message: TGMessage) {
        message.stopTranslated()
    }

    /**/
    private var textInputHasSelection = false
    private var textFormattingVisible = false

    override fun onInputSelectionChanged(v: InputView?, start: Int, end: Int) {
        if (textFormattingLayout != null) {
            textFormattingLayout!!.onInputViewSelectionChanged(start, end)
        }
    }

    override fun onInputSelectionExistChanged(v: InputView?, hasSelection: Boolean) {
        textInputHasSelection = hasSelection
        if (!emojiShown) {
            emojiButton!!.setImageResource(getTargetIcon(true))
        }
    }

    fun onInputSpansChanged(view: InputView?) {
        if (textFormattingLayout != null) {
            textFormattingLayout!!.onInputViewSpansChanged()
        }
    }

    private fun setTextFormattingLayoutVisible(visible: Boolean) {
        textFormattingVisible = this.emojiKeyboardLayout != null && emojiKeyboardLayout!!.contentView.setTextFormattingLayoutVisible(visible)
    }

    private fun closeTextFormattingKeyboard() {
        if (textFormattingVisible && emojiShown) {
            closeEmojiKeyboard()
        }
    }

    @DrawableRes
    fun getTargetIcon(isMessage: Boolean): Int {
        return if (textInputHasSelection || (textFormattingVisible && emojiShown)) R.drawable.baseline_format_text_24 else EmojiLayout.getTargetIcon(isMessage)
    }

    override fun onCreatePopupLayout(popupLayout: PopupLayout?) {
        hideCursorsForInputView()
    }

    fun hideCursorsForInputView() {
        if (inputView != null) {
            inputView!!.hideSelectionCursors()
        }
    }


    /* * */
    private var stickerSuggestionItems: ArrayList<InlineResult<*>?>? = null
    private var canShowEmojiSuggestions = false
    private var isStickerSuggestionsTemporarilyHidden = false

    fun showStickerSuggestions(stickers: ArrayList<TGStickerObj>?, isMore: Boolean) {
        val items: ArrayList<InlineResult<*>?>?
        if (stickers != null) {
            items = ArrayList<InlineResult<*>?>(stickers.size)
            for (sticker in stickers) {
                items.add(InlineResultSticker(context, tdlib, "x", InlineQueryResultSticker("x", sticker.getSticker())))
            }
        } else {
            items = null
        }

        if (!isMore) {
            stickerSuggestionItems = items
            context.showInlineResults(this, tdlib, items, true, null, this.inlineResultsStickerScrollListener, this.inlineResultsStickerMovementsCallback)
        } else {
            if (items != null && stickerSuggestionItems != null) {
                stickerSuggestionItems!!.addAll(items)
            }
            context.addInlineResults(this, items, null, this.inlineResultsStickerScrollListener, this.inlineResultsStickerMovementsCallback)
        }
    }

    private var lastFoundByEmoji: String? = null
    private var needSkipMoreEmojiSuggestions = false

    fun showEmojiSuggestions(stickers: ArrayList<TGStickerObj?>?, foundByEmoji: String?, isMore: Boolean) {
        if (lastFoundByEmoji.equalsOrBothEmpty(foundByEmoji)) {
            if (!isMore || needSkipMoreEmojiSuggestions) {
                if (context.hasEmojiSuggestions()) {
                    if (isFocused() && pagerScrollOffset < 1f) {
                        context.setEmojiSuggestionsVisible(true)
                    }
                    canShowEmojiSuggestions = true
                }
                return
            }
            needSkipMoreEmojiSuggestions = true
        } else {
            lastFoundByEmoji = foundByEmoji
            needSkipMoreEmojiSuggestions = false
        }

        if (stickers == null || stickers.isEmpty()) {
            if (!isMore) {
                context.setEmojiSuggestions(
                    this,
                    null,
                    this.inlineEmojiStickerScrollListener,
                    StickersSuggestionsLayout.Delegate { emojiType: Int, isChoosingEmoji: Boolean -> this.notifyChoosingEmoji(emojiType, isChoosingEmoji) })
                context().setEmojiSuggestionsVisible(false)
                canShowEmojiSuggestions = false
            }
            return
        }

        if (isMore && context.hasEmojiSuggestions() && context.isEmojiSuggestionsVisible()) {
            context.addEmojiSuggestions(this, stickers)
        } else {
            context.setEmojiSuggestions(
                this,
                stickers,
                this.inlineEmojiStickerScrollListener,
                StickersSuggestionsLayout.Delegate { emojiType: Int, isChoosingEmoji: Boolean -> this.notifyChoosingEmoji(emojiType, isChoosingEmoji) })
        }
        if (isFocused() && pagerScrollOffset < 1f) {
            context.setEmojiSuggestionsVisible(true)
        }

        canShowEmojiSuggestions = true
    }

    private fun showStickersSuggestionsIfTemporarilyHidden() {
        if (isStickerSuggestionsTemporarilyHidden && stickerSuggestionItems != null) {
            isStickerSuggestionsTemporarilyHidden = false
            context.showInlineResults(
                this, tdlib, stickerSuggestionItems, true, null,
                this.inlineResultsStickerScrollListener,
                this.inlineResultsStickerMovementsCallback
            )
        }
    }

    private fun hideStickersSuggestionsTemporarily() {
        isStickerSuggestionsTemporarilyHidden = true
        context.showInlineResults(this, tdlib, null, true, null)
    }

    private fun showEmojiSuggestionsIfTemporarilyHidden() {
        if (canShowEmojiSuggestions) {
            context().setEmojiSuggestionsVisible(true)
        }
    }

    private fun hideEmojiSuggestionsTemporarily() {
        context().setEmojiSuggestionsVisible(false)
    }

    private fun hideEmojiAndStickerSuggestionsFinally() {
        onHideEmojiAndStickerSuggestionsFinally()
        context.showInlineResults(this, tdlib, null, false, null)
    }

    fun onHideEmojiAndStickerSuggestionsFinally() {
        canShowEmojiSuggestions = false
        stickerSuggestionItems = null
        hideEmojiSuggestionsTemporarily()
    }

    private val inlineResultsStickerMovementsCallback: StickerMovementCallback
        get() = object : StickerMovementCallback {
            override fun onStickerClick(
                view: StickerSmallView?,
                clickView: View?,
                sticker: TGStickerObj,
                isMenuClick: Boolean,
                sendOptions: MessageSendOptions
            ): Boolean {
                if (onSendStickerSuggestion(clickView, sticker, sendOptions)) {
                    hideEmojiAndStickerSuggestionsFinally()
                    return true
                }

                return false
            }

            override fun getStickerOutputChatId(): Long {
                return getOutputChatId()
            }

            override fun setStickerPressed(view: StickerSmallView?, sticker: TGStickerObj?, isPressed: Boolean) {
                val inlineResultsWrap = context.getInlineResultsView()
                if (inlineResultsWrap != null) {
                    inlineResultsWrap.setStickerPressed(view, sticker, isPressed)
                }
            }

            override fun canFindChildViewUnder(view: StickerSmallView?, recyclerX: Int, recyclerY: Int): Boolean {
                return true
            }

            override fun needsLongDelay(view: StickerSmallView?): Boolean {
                return true
            }

            override fun getStickersListTop(): Int {
                return -getInputOffset(false)
            }

            override fun getViewportHeight(): Int {
                return getStickerSuggestionPreviewViewportHeight()
            }

            override fun onStickerPreviewOpened(view: StickerSmallView?, sticker: TGStickerObj?) {
                notifyChoosingEmoji(EmojiMediaType.STICKER, true.also { stickerPreviewIsVisible = it })
            }

            override fun onStickerPreviewChanged(view: StickerSmallView?, otherOrThisSticker: TGStickerObj?) {
                notifyChoosingEmoji(EmojiMediaType.STICKER, true.also { stickerPreviewIsVisible = it })
            }

            override fun onStickerPreviewClosed(view: StickerSmallView?, thisSticker: TGStickerObj?) {
                notifyChoosingEmoji(EmojiMediaType.STICKER, false.also { stickerPreviewIsVisible = it })
            }
        }

    private var suggestionXDiff = 0
    private var suggestionYDiff = 0
    private var stickerPreviewIsVisible = false

    private val inlineResultsStickerScrollListener: RecyclerView.OnScrollListener
        get() {
            suggestionYDiff = 0
            return object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy == 0) return
                    suggestionYDiff = recyclerView.computeVerticalScrollOffset()
                    notifyChoosingEmoji(EmojiMediaType.STICKER, true)
                }
            }
        }

    private val inlineEmojiStickerScrollListener: RecyclerView.OnScrollListener
        get() {
            suggestionYDiff = 0
            return object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dx == 0) return
                    suggestionXDiff = recyclerView.computeHorizontalScrollOffset()
                    notifyChoosingEmoji(EmojiMediaType.EMOJI, true)
                }
            }
        }

    override fun onSendStickerSuggestion(view: View?, sticker: TGStickerObj, initialSendOptions: MessageSendOptions): Boolean {
        if (lastJunkTime == 0L || SystemClock.uptimeMillis() - lastJunkTime >= JUNK_MINIMUM_DELAY) {
            if (sticker.isCustomEmoji()) {
                inputView!!.onCustomEmojiSelected(sticker, true)
                return true
            }
            if (showGifRestriction(view)) return false
            pickDateOrProceed(initialSendOptions, SimpleSendCallback { modifiedSendOptions: MessageSendOptions?, disableMarkdown: Boolean ->
                if (sendSticker(view, sticker.getSticker(), sticker.getFoundByEmoji(), true, modifiedSendOptions!!)) {
                    lastJunkTime = SystemClock.uptimeMillis()
                    inputView!!.setInput("", false, true)
                }
            })
            return true
        }
        return false
    }

    override fun getStickerSuggestionsTop(isEmoji: Boolean): Int {
        val v: View? = context().getEmojiSuggestionsView()
        return if (v != null) Views.getLocationInWindow(v)[1] else 0
    }

    override fun getStickerSuggestionPreviewViewportHeight(): Int {
        return HeaderView.getSize(true) + messagesView!!.getMeasuredHeight()
    }

    override fun getCurrentChatId(): Long {
        return getChatId()
    }

    private fun checkAttachedFiles(animated: Boolean) {
        val hasAttachedFiles = hasAttachedFiles()

        if (attachedFiles != null) {
            attachedFiles!!.setHidden(!hasAttachedFiles, animated)
        }

        setCustomCaptionPlaceholder(if (hasAttachedFiles) Lang.getString(R.string.Caption) else null)
        if (inputView != null) {
            inputView!!.getInlineSearchContext().setIsCaption(hasAttachedFiles)
            checkSendButton(animated)
        }
        if (emojiLayout != null) {
            emojiLayout!!.setAllowMedia(!hasAttachedFiles)
        }
    }

    private val attachedFilesOffset: Float
        get() = attachedFilesLastHeight * (1f - getSearchTransformFactor()) * (if (attachedFiles != null) attachedFiles!!.getVisibleFactor() else 0f)

    fun hasAttachedFiles(): Boolean {
        return !needHideAttachedFiles() && attachedFiles != null && attachedFiles!!.isDisplayingItems()
    }

    private fun discardAttachedFiles(animated: Boolean) {
        if (attachedFiles != null) {
            attachedFiles!!.showItems(this, null, false, null, null, null, !isFocused())
            checkAttachedFiles(animated)
        }
    }

    private fun needHideAttachedFiles(): Boolean {
        return inSearchMode() || this.isEditingMessage
    }

    override fun onThemeColorsChanged(areTemp: Boolean, state: ColorState?) {
        super.onThemeColorsChanged(areTemp, state)
        if (attachedFiles != null) {
            attachedFiles!!.getThemeProvider().onThemeColorsChanged(areTemp)
        }
    }

    private var attachedFiles: InlineResultsWrap? = null
    private var attachedFilesAnimator: CustomItemAnimator? = null
    private var attachedFilesClickHelperDelegate: ClickHelper.Delegate? = null
        get() {
            if (field == null) {
                field = object : ClickHelper.Delegate {
                    override fun needClickAt(view: View, x: Float, y: Float): Boolean {
                        return true
                    }

                    override fun onClickAt(view: View, x: Float, y: Float) {
                        if (x < view.getMeasuredWidth() - Screen.dp(36f)) {
                            return
                        }

                        val tag = view.getTag()
                        if (tag is InlineResult<*>) {
                            if (attachedFiles!!.getRecyclerView().getItemAnimator() == null) {
                                attachedFiles!!.getRecyclerView().setItemAnimator(attachedFilesAnimator)
                            }

                            attachedFiles!!.removeItem(tag)
                            scheduleRemoveAttachedFilesAnimator()
                            checkAttachedFiles(true)
                        }
                    }
                }
            }
            return field
        }
    private var attachedFilesLastHeight = 0

    private var attachedFilesAnimatorRemoveRunnable: Runnable? = null

    init {
        this.manager = MessagesManager(this)
    }

    private fun scheduleRemoveAttachedFilesAnimator() {
        if (attachedFilesAnimatorRemoveRunnable != null) {
            UI.cancel(attachedFilesAnimatorRemoveRunnable)
        }
        attachedFilesAnimatorRemoveRunnable = Runnable { this.removeAttachedFilesAnimator() }
        UI.post(attachedFilesAnimatorRemoveRunnable, 500)
    }

    private fun removeAttachedFilesAnimator() {
        attachedFilesAnimatorRemoveRunnable = null
        val recyclerView = attachedFiles!!.getRecyclerView()
        if (recyclerView.getItemAnimator() != null && recyclerView.getItemAnimator()!!.isRunning()) {
            scheduleRemoveAttachedFilesAnimator()
            return
        }
        recyclerView.setItemAnimator(null)
    }

    fun setFilesToAttach(results: ArrayList<InlineResult<*>?>?, needShowKeyboard: Boolean) {
        if (results == null || results.isEmpty()) {
            discardAttachedFiles(true)
            return
        }

        for (result in results) {
            if (result is InlineResultCommon) {
                result.setNeedCloseButton(true)
                result.setClickHelper(ClickHelper(this.attachedFilesClickHelperDelegate!!))
                result.rebuildLayout()
            }
        }

        if (attachedFiles == null) {
            attachedFiles = object : InlineResultsWrap(context) {
                fun checkTopEdge(top: Int): Int {
                    val height = min(getHeightLimit(), max(getMinItemsHeight(), getRecyclerView().getMeasuredHeight() - top))
                    if (attachedFilesLastHeight != height) {
                        attachedFilesLastHeight = height
                        UI.post(Runnable { this@MessagesController.updateReplyView() })
                    }
                    return top
                }

                override fun detectRecyclerTopEdge(): Int {
                    val recyclerView = getRecyclerView()
                    val manager = recyclerView.getLayoutManager() as LinearLayoutManager?
                    val i = manager!!.findFirstVisibleItemPosition()
                    if (i != 0) {
                        return checkTopEdge(0)
                    }

                    var topA = 0
                    if (i == 0) {
                        val view = manager.findViewByPosition(0)
                        if (view != null) {
                            topA = view.getMeasuredHeight()
                            topA += view.getTop()
                        }
                    }

                    var top = recyclerView.getMeasuredHeight()
                    for (a in 0..<recyclerView.getChildCount()) {
                        val view = recyclerView.getChildAt(a)
                        if (view is ShadowView) {
                            continue
                        }
                        top = min(top, (view.getTop() + view.getTranslationY() + (view.getMeasuredHeight() * (1f - view.getAlpha()))).toInt())
                    }
                    return checkTopEdge(min(top, topA))
                }

                override fun onFactorChanged(id: Int, factor: Float, fraction: Float, callee: FactorAnimator?) {
                    super.onFactorChanged(id, factor, fraction, callee)
                    updateReplyView()
                    if (factor == 0f) {
                        if (getParent() != null) {
                            context().removeFromRoot(this)
                        }
                    } else {
                        if (getParent() == null) {
                            context().addToRoot(this, false)
                        }
                    }
                }
            }
            attachedFilesAnimator = CustomItemAnimator(DECELERATE_INTERPOLATOR, 150L)
            attachedFiles!!.getRecyclerView().setItemAnimator(null)
            attachedFiles!!.setListener(object : PickListener {})
        }

        if (attachedFiles!!.getParent() == null) {
            context().addToRoot(attachedFiles, false)
        }

        attachedFiles!!.showItems(this, results, false, null, null, null, needHideAttachedFiles())
        checkAttachedFiles(true)
        if (inputView != null && needShowKeyboard) {
            showKeyboard()
        }
    }

    companion object {
        private fun updateFontSliderValue(sliderView: SliderView) {
            var found = false
            var index = 0
            for (dp in Settings.CHAT_FONT_SIZES) {
                if (dp == Settings.instance().chatFontSize) {
                    found = true
                    break
                }
                index++
            }
            if (found) {
                sliderView.setValue(index.toFloat() / (Settings.CHAT_FONT_SIZES.size - 1).toFloat())
            }
        }

        private fun updateWpBlurSliderValue(sliderView: SliderView) {
            var found = false
            var index = 0
            for (dp in Settings.CHAT_FONT_SIZES) {
                if (dp == Settings.instance().chatFontSize) {
                    found = true
                    break
                }
                index++
            }
            if (found) {
                sliderView.setValue(index.toFloat() / (Settings.CHAT_FONT_SIZES.size - 1).toFloat())
            }
        }

        const val PREVIEW_MODE_NONE: Int = 0
        const val PREVIEW_MODE_WALLPAPER: Int = 1
        const val PREVIEW_MODE_FONT_SIZE: Int = 2
        const val PREVIEW_MODE_EVENT_LOG: Int = 3
        const val PREVIEW_MODE_SEARCH: Int = 4
        const val PREVIEW_MODE_WALLPAPER_OBJECT: Int = 5

        private fun shouldIgnoreDraftLoad(`object`: Any?): Boolean {
            return `object` is TGSwitchInline
        }

        @JvmStatic val slideBackBound: Int
            get() = TGMessage.contentLeft

        private var shownTutorials: HashSet<String?>? = null

        private fun readItem(tdlib: Tdlib, readDate: Int): OptionItem {
            return OptionItem.Builder()
                .name(
                    Lang.getRelativeDate(
                        readDate.toLong(),
                        TimeUnit.SECONDS,
                        tdlib!!.currentTimeMillis(),
                        TimeUnit.MILLISECONDS,
                        false,
                        0,
                        R.string.ReadDate,
                        false
                    )
                )
                .icon(R.drawable.deproko_baseline_check_double_24)
                .color(OptionColor.LIGHT, OptionColor.BLUE)
                .build()
        }

        private const val BOTTOM_ACTION_NONE = 0
        private const val BOTTOM_ACTION_FOLLOW = 1
        private const val BOTTOM_ACTION_TOGGLE_MUTE = 2
        private const val BOTTOM_ACTION_UNPIN_ALL = 3
        private const val BOTTOM_ACTION_DISCUSS = 4
        private const val BOTTOM_ACTION_APPLY_WALLPAPER = 5
        private const val BOTTOM_ACTION_TEST = 100

        private const val ANIMATOR_BOTTOM_BUTTON = 12
        private const val ACTION_DELETE_CHAT = 1
        private const val ACTION_BOT_START = 2
        private const val ACTION_JOIN_CHAT = 3
        private const val ACTION_OPEN_SUPERGROUP = 4
        private const val ACTION_UNBAN_USER = 5
        private const val ACTION_EMPTY = 6
        private const val ACTION_EVENT_LOG_SETTINGS = 7
        private const val ACTION_AWAITING_REPLY = 8

        private const val ANIMATOR_SCROLL_TO_BOTTOM = 6
        private const val ANIMATOR_MENTION_BUTTON = 7
        private const val ANIMATOR_REACTION_BUTTON = 15
        private const val ANIMATE_REPLY_BAR = false

        private const val FLAG_INPUT_EDITING = 1
        private val FLAG_INPUT_OFFSCREEN = 1 shl 1
        private val FLAG_INPUT_RECORDING = 1 shl 2
        private val FLAG_INPUT_TEXT_DISABLED = 1 shl 3

        private const val LOCATION_MAX_WAIT_TIME = 3000L
        private const val USE_GOOGLE_LOCATION = true
        private const val USE_LAST_KNOWN_LOCATION = false

        @JvmStatic fun getForcePreviewHeight(hasHeader: Boolean, hasFooter: Boolean): Int {
            var forcePreviewHeight = (Screen.currentHeight()
                    - ForceTouchView.getMatchParentTopMargin()
                    - ForceTouchView.getMatchParentBottomMargin())
            if (hasHeader) {
                forcePreviewHeight -= Screen.dp(ForceTouchView.HEADER_HEIGHT)
            }
            if (hasFooter) {
                forcePreviewHeight -= Screen.dp(ForceTouchView.FOOTER_HEIGHT)
            }
            return forcePreviewHeight
        }

        // Stickers
        private const val JUNK_MINIMUM_DELAY = 200L
        private fun checkFilter(@IdRes filterId: Int, filters: ChatEventLogFilters?): Boolean {
            if (filters == null) {
                return true
            }
            if (filterId == R.id.btn_filterAll) {
                return TD.isAll(filters)
            } else if (filterId == R.id.btn_filterRestrictions) {
                return filters.memberRestrictions
            } else if (filterId == R.id.btn_filterAdmins) {
                return filters.memberPromotions
            } else if (filterId == R.id.btn_filterMembers) {
                return filters.memberJoins || filters.memberInvites
            } else if (filterId == R.id.btn_filterInviteLinks) {
                return filters.inviteLinkChanges
            } else if (filterId == R.id.btn_filterInfo) {
                return filters.infoChanges
            } else if (filterId == R.id.btn_filterSettings) {
                return filters.settingChanges
            } else if (filterId == R.id.btn_filterDeletedMessages) {
                return filters.messageDeletions
            } else if (filterId == R.id.btn_filterEditedMessages) {
                return filters.messageEdits
            } else if (filterId == R.id.btn_filterPinnedMessages) {
                return filters.messagePins
            } else if (filterId == R.id.btn_filterLeavingMembers) {
                return filters.memberLeaves
            } else if (filterId == R.id.btn_filterVideoChats) {
                return filters.videoChatChanges
            }
            return false
        }

        private const val ANIMATOR_SEND = 9

        private const val ANIMATOR_SEARCH_PROGRESS = 11

        private const val DISABLED_BUTTON_ALPHA = .6f

        @JvmStatic fun maximizeFrom(
            tdlib: Tdlib?,
            context: Context,
            target: FactorAnimator,
            animateToWhenReady: Float,
            controller: MessagesController,
            modifier: RunnableData<MessagesController?>?
        ): Boolean {
            val c = MessagesController(context, tdlib)
            c.setArguments(controller.getArgumentsStrict())
            c.forceFastAnimationOnce()
            if (modifier != null) {
                modifier.runWithData(c)
            }
            c.postOnAnimationReady(Runnable { target.animateTo(animateToWhenReady) })
            UI.getContext(context).navigation().navigateTo(c)
            return true
        }


        // Call methods
        @JvmStatic fun getChatAvailableMessagesSenders(tdlib: Tdlib, chatId: Long, callback: RunnableData<ChatMessageSenders?>) {
            tdlib!!.send<ChatMessageSenders?>(GetChatAvailableMessageSenders(chatId), Tdlib.ResultHandler { result: ChatMessageSenders?, error: TdApi.Error? ->
                UI.post(
                    Runnable {
                        if (error != null) {
                            UI.showError(error)
                        } else {
                            callback.runWithData(result)
                        }
                    })
            })
        }

        @JvmStatic fun setNewMessageSender(tdlib: Tdlib, chatId: Long, sender: ChatMessageSender, after: Runnable?) {
            val handler = tdlib!!.typedOkHandler(after)
            tdlib!!.send<TdApi.Ok?>(SetChatMessageSender(chatId, sender.sender), handler)
        }

        private const val SEARCH_MODE_MESSAGES = 0
        private const val SEARCH_MODE_USERS = 1
        private const val ANIMATOR_SEARCH_BY_USER = 21
        private const val ANIMATOR_SEARCH_NAVIGATION = 22

        // Filter Type Utils
        val MESSAGE_TYPE_UNKNOWN: Int = -1
        val MESSAGE_TYPE_TEXT: Int = SearchMessagesFilterTextPolyfill.CONSTRUCTOR
        val MESSAGE_TYPE_PHOTO: Int = SearchMessagesFilterPhoto.CONSTRUCTOR
        val MESSAGE_TYPE_VIDEO: Int = SearchMessagesFilterVideo.CONSTRUCTOR
        val MESSAGE_TYPE_VOICE: Int = SearchMessagesFilterVoiceNote.CONSTRUCTOR
        val MESSAGE_TYPE_ROUND: Int = SearchMessagesFilterVideoNote.CONSTRUCTOR
        val MESSAGE_TYPE_FILE: Int = SearchMessagesFilterDocument.CONSTRUCTOR
        val MESSAGE_TYPE_MUSIC: Int = SearchMessagesFilterAudio.CONSTRUCTOR
        val MESSAGE_TYPE_GIF: Int = SearchMessagesFilterAnimation.CONSTRUCTOR

        @JvmStatic fun getMessageType(tdlib: Tdlib, msg: TdApi.Message, content: MessageContent?): Int {
            try {
                if (content == null) {
                    return MESSAGE_TYPE_TEXT
                }
                if (msg.restrictionInfo.hasRestriction(Settings.instance().needRestrictContent())) {
                    return MESSAGE_TYPE_TEXT
                }
                when (content.getConstructor()) {
                    MessageAnimatedEmoji.CONSTRUCTOR -> {
                        val emoji = content as MessageAnimatedEmoji
                        val pendingContent = tdlib!!.getPendingMessageText(msg.chatId, msg.id)
                        if (pendingContent != null) {
                            if (pendingContent.getConstructor() == MessageAnimatedEmoji.CONSTRUCTOR && !Settings.instance()
                                    .getNewSetting(Settings.SETTING_FLAG_NO_ANIMATED_EMOJI)
                            ) {
                                return MESSAGE_TYPE_UNKNOWN
                            } else {
                                return MESSAGE_TYPE_TEXT
                            }
                        }
                        if (Settings.instance().getNewSetting(Settings.SETTING_FLAG_NO_ANIMATED_EMOJI)) {
                            return MESSAGE_TYPE_TEXT
                        } else {
                            return MESSAGE_TYPE_UNKNOWN
                        }
                    }

                    MessageText.CONSTRUCTOR -> {
                        val pendingContent = tdlib!!.getPendingMessageText(msg.chatId, msg.id)
                        if (pendingContent != null && pendingContent.getConstructor() == MessageAnimatedEmoji.CONSTRUCTOR) {
                            return MESSAGE_TYPE_UNKNOWN
                        }
                        return MESSAGE_TYPE_TEXT
                    }

                    TdApi.MessagePhoto.CONSTRUCTOR -> {
                        return MESSAGE_TYPE_PHOTO
                    }

                    TdApi.MessageVideo.CONSTRUCTOR -> {
                        return MESSAGE_TYPE_VIDEO
                    }

                    TdApi.MessageAnimation.CONSTRUCTOR -> {
                        return MESSAGE_TYPE_GIF
                    }

                    TdApi.MessageVideoNote.CONSTRUCTOR -> {
                        return MESSAGE_TYPE_ROUND
                    }

                    TdApi.MessageVoiceNote.CONSTRUCTOR -> {
                        return MESSAGE_TYPE_VOICE
                    }

                    TdApi.MessageAudio.CONSTRUCTOR -> {
                        return MESSAGE_TYPE_MUSIC
                    }

                    MessageDocument.CONSTRUCTOR -> {
                        return MESSAGE_TYPE_FILE
                    }

                    else -> {
                        return MESSAGE_TYPE_UNKNOWN
                    }
                }
            } catch (t: Throwable) {
                return MESSAGE_TYPE_UNKNOWN
            }
        }

        private val searchFilterIds = intArrayOf(
            R.id.btn_messageSearchFilterAll,
            R.id.btn_messageSearchFilterText,
            R.id.btn_messageSearchFilterPhoto,
            R.id.btn_messageSearchFilterVideo,
            R.id.btn_messageSearchFilterVoice,
            R.id.btn_messageSearchFilterRound,
            R.id.btn_messageSearchFilterFiles,
            R.id.btn_messageSearchFilterMusic,
            R.id.btn_messageSearchFilterGif
        )

        private val searchFilterIcons = intArrayOf(
            R.drawable.baseline_filter_variant_remove_24,
            R.drawable.baseline_format_text_24,
            R.drawable.baseline_image_24,
            R.drawable.baseline_videocam_24,
            R.drawable.baseline_mic_24,
            R.drawable.deproko_baseline_msg_video_24,
            R.drawable.baseline_insert_drive_file_24,
            R.drawable.baseline_music_note_24,
            R.drawable.deproko_baseline_gif_filled_24
        )

        private val searchFilterTexts = arrayOf<String?>(
            Lang.getString(R.string.MessageSearchFilterAll),
            Lang.getString(R.string.MessageSearchFilterText),
            Lang.getString(R.string.MessageSearchFilterPhoto),
            Lang.getString(R.string.MessageSearchFilterVideo),
            Lang.getString(R.string.MessageSearchFilterVoice),
            Lang.getString(R.string.MessageSearchFilterRound),
            Lang.getString(R.string.MessageSearchFilterFiles),
            Lang.getString(R.string.MessageSearchFilterMusic),
            Lang.getString(R.string.MessageSearchFilterGif)
        )

        private val searchFiltersTdApi = arrayOf<SearchMessagesFilter?>(
            null,
            SearchMessagesFilterTextPolyfill(),
            SearchMessagesFilterPhoto(),
            SearchMessagesFilterVideo(),
            SearchMessagesFilterVoiceNote(),
            SearchMessagesFilterVideoNote(),
            SearchMessagesFilterDocument(),
            SearchMessagesFilterAudio(),
            SearchMessagesFilterAnimation()
        )
    }
}
