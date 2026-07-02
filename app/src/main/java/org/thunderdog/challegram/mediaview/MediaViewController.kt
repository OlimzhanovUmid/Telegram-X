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
 * File created on 01/09/2015 at 00:51
 */
package org.thunderdog.challegram.mediaview

import android.content.Context
import android.graphics.*
import android.graphics.Outline
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.SystemClock
import android.text.*
import android.util.TypedValue
import android.view.*
import android.view.View.OnLongClickListener
import android.view.ViewGroup.MarginLayoutParams
import android.view.animation.Interpolator
import android.view.animation.OvershootInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.*
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.media3.common.PlaybackException
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemAnimator
import me.vkryl.android.ScrimUtil.makeCubicGradientScrimDrawable
import me.vkryl.android.animator.BoolAnimator
import me.vkryl.android.animator.FactorAnimator
import me.vkryl.android.onClick
import me.vkryl.android.setBackground
import me.vkryl.android.util.ClickHelper
import me.vkryl.android.util.InvalidateContentProvider
import me.vkryl.android.widget.FrameLayoutFix
import me.vkryl.android.widget.FrameLayoutFix.Companion.newParams
import me.vkryl.core.*
import me.vkryl.core.collection.IntList
import me.vkryl.core.lambda.*
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import org.drinkless.tdlib.TdApi.*
import org.thunderdog.challegram.BaseActivity.ActivityListener
import org.thunderdog.challegram.Log
import org.thunderdog.challegram.R
import org.thunderdog.challegram.U
import org.thunderdog.challegram.component.MediaCollectorDelegate
import org.thunderdog.challegram.component.attach.CustomItemAnimator
import org.thunderdog.challegram.component.attach.MediaLayout
import org.thunderdog.challegram.component.attach.MediaToReplacePickerManager
import org.thunderdog.challegram.component.attach.MediaToReplacePickerManager.LocalPickedFile
import org.thunderdog.challegram.component.chat.EmojiToneHelper
import org.thunderdog.challegram.component.chat.InlineResultsWrap
import org.thunderdog.challegram.component.chat.InlineResultsWrap.PickListener
import org.thunderdog.challegram.component.chat.InputView
import org.thunderdog.challegram.component.chat.InputView.SpanChangeListener
import org.thunderdog.challegram.component.preview.FlingDetector
import org.thunderdog.challegram.component.sticker.TGStickerObj
import org.thunderdog.challegram.config.Config
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.core.Media
import org.thunderdog.challegram.data.*
import org.thunderdog.challegram.data.TGMessage.HotListener
import org.thunderdog.challegram.loader.*
import org.thunderdog.challegram.mediaview.crop.CropAreaView
import org.thunderdog.challegram.mediaview.crop.CropAreaView.*
import org.thunderdog.challegram.mediaview.crop.CropLayout
import org.thunderdog.challegram.mediaview.crop.CropState
import org.thunderdog.challegram.mediaview.crop.CropTargetView
import org.thunderdog.challegram.mediaview.data.FiltersState
import org.thunderdog.challegram.mediaview.data.MediaItem
import org.thunderdog.challegram.mediaview.data.MediaItem.ThumbExpandChangeListener
import org.thunderdog.challegram.mediaview.data.MediaStack
import org.thunderdog.challegram.mediaview.gl.BitmapCallback
import org.thunderdog.challegram.mediaview.gl.EGLEditorView
import org.thunderdog.challegram.mediaview.gl.EGLEditorView.ContentLayout.DrawingListener
import org.thunderdog.challegram.mediaview.paint.PaintMode
import org.thunderdog.challegram.mediaview.paint.PaintState
import org.thunderdog.challegram.mediaview.paint.PaintState.UndoStateListener
import org.thunderdog.challegram.mediaview.paint.SimpleDrawing
import org.thunderdog.challegram.mediaview.paint.widget.ColorDirectionView
import org.thunderdog.challegram.mediaview.paint.widget.ColorPickerView
import org.thunderdog.challegram.mediaview.paint.widget.ColorPickerView.ToneEventListener
import org.thunderdog.challegram.mediaview.paint.widget.ColorPreviewView
import org.thunderdog.challegram.mediaview.paint.widget.ColorPreviewView.*
import org.thunderdog.challegram.mediaview.paint.widget.ColorToneView
import org.thunderdog.challegram.navigation.*
import org.thunderdog.challegram.navigation.Menu
import org.thunderdog.challegram.navigation.TooltipOverlayView.TooltipInfo
import org.thunderdog.challegram.support.ViewSupport
import org.thunderdog.challegram.telegram.*
import org.thunderdog.challegram.telegram.CallManager.CurrentCallListener
import org.thunderdog.challegram.telegram.TGLegacyManager.EmojiLoadListener
import org.thunderdog.challegram.telegram.TdlibUi.SimpleSendCallback
import org.thunderdog.challegram.telegram.TdlibUi.TTLOption
import org.thunderdog.challegram.theme.*
import org.thunderdog.challegram.tool.*
import org.thunderdog.challegram.ui.*
import org.thunderdog.challegram.unsorted.Settings
import org.thunderdog.challegram.unsorted.Settings.VideoLimit
import org.thunderdog.challegram.unsorted.Size
import org.thunderdog.challegram.util.HapticMenuHelper
import org.thunderdog.challegram.util.OptionDelegate
import org.thunderdog.challegram.util.StringList
import org.thunderdog.challegram.util.text.Text
import org.thunderdog.challegram.util.text.TextEntity
import org.thunderdog.challegram.v.MaxHeightScrollView
import org.thunderdog.challegram.widget.*
import org.thunderdog.challegram.widget.PopupLayout.*
import org.thunderdog.challegram.widget.RootFrameLayout.InsetsChangeListener
import org.thunderdog.challegram.widget.VideoTimelineView.TimelineDelegate
import tgx.td.MAX_MESSAGE_GROUP_SIZE
import tgx.td.MessageId
import tgx.td.assertMessageSelfDestructType_58882d8c
import tgx.td.findBiggest
import tgx.td.getSenderId
import tgx.td.getSenderUserId
import tgx.td.getType
import tgx.td.isDocumentFilter
import tgx.td.isEmpty
import tgx.td.isSecret
import tgx.td.isText
import tgx.td.isUserChat
import tgx.td.messageThreadId
import tgx.td.newSendOptions
import tgx.td.toBasicGroupId
import tgx.td.toSupergroupId
import tgx.td.unsupported
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.Any
import kotlin.Array
import kotlin.Boolean
import kotlin.CharSequence
import kotlin.Double
import kotlin.Float
import kotlin.IllegalStateException
import kotlin.Int
import kotlin.IntArray
import kotlin.Long
import kotlin.LongArray
import kotlin.OptIn
import kotlin.String
import kotlin.also
import kotlin.arrayOf
import kotlin.check
import kotlin.contracts.ExperimentalContracts
import kotlin.intArrayOf
import kotlin.math.*
import kotlin.require

@OptIn(ExperimentalContracts::class)
class MediaViewController(context: Context, tdlib: Tdlib?) : ViewController<MediaViewController.Args>(context, tdlib), AnimatedPopupProvider,
    FactorAnimator.Target, View.OnClickListener, MediaStackCallback, MediaFiltersAdapter.Callback, Watcher, RotationControlView.Callback,
    MediaView.ClickListener, EmojiLayout.Listener, InputView.InputListener, InlineResultsWrap.OffsetProvider, MediaCellView.Callback, SliderView.Listener,
    EmojiLoadListener, Menu, MoreDelegate, TouchSectionProvider, FlingDetector.Callback, CurrentCallListener, BrushChangeListener, UndoStateListener,
    MediaView.FactorChangeListener, EmojiToneHelper.Delegate, MessageListener, InputView.SelectionChangeListener, ShowListener, InsetsChangeListener {
    private fun useEdgeToEdge(): Boolean {
        return Settings.instance().useEdgeToEdge()
    }

    private var reverseMode = false

    class Args(
      val parentController: ViewController<*>?,
      @JvmField val mode: Int,
      @JvmField var delegate: MediaViewDelegate?,
      val selectDelegate: MediaSelectDelegate?,
      val sendDelegate: MediaSendDelegate?,
      val stack: MediaStack?
    ) {
        var caption: String? = null
        var noLoadMore = false
        var customSubtitle: String? = null

        var forceThumbs = false
        var forceOpenIn = false

        var copyLink: String? = null

        var reverseMode = false

        var receiverChatId: Long = 0
        var topicId: MessageTopic? = null

        var areOnlyScheduled = false

        constructor(parentController: ViewController<*>?, mode: Int, stack: MediaStack?) : this(parentController, mode, null, null, null, stack)

        fun setOnlyScheduled(onlyScheduled: Boolean): Args {
            this.areOnlyScheduled = onlyScheduled
            return this
        }

        @AvatarPickerMode
        var avatarPickerMode = 0

        fun setCustomSubtitle(subtitle: String?): Args {
            this.customSubtitle = subtitle
            return this
        }

        fun setAvatarPickerModeValue(@AvatarPickerMode avatarPickerMode: Int): Args {
            check(mode == MODE_GALLERY)
            this.avatarPickerMode = avatarPickerMode
            setSendButtonIconRes(
                getResId(
                    avatarPickerMode, 0,
                    R.drawable.dot_baseline_profile_accept_24,
                    R.drawable.dot_baseline_group_accept_24,
                    R.drawable.dot_baseline_channel_accept_24,
                    R.drawable.dot_baseline_bot_accept_24
                )
            )

            val textRes: Int = getResId(
                avatarPickerMode, 0,
                R.string.ProfilePhoto, R.string.GroupPhoto, R.string.ChannelPhoto, R.string.BotPhoto
            )
            setReceiverRow(
                getResId(
                    avatarPickerMode, 0,
                    R.drawable.dot_baseline_account_circle_18,
                    R.drawable.dot_baseline_group_circle_18,
                    R.drawable.dot_baseline_channel_circle_18,
                    R.drawable.dot_baseline_bot_circle_18
                ),
                if (textRes != 0) Lang.getString(textRes) else null
            )

            return this
        }

        fun setForceThumbs(forceThumbs: Boolean): Args {
            this.forceThumbs = forceThumbs
            return this
        }

        fun setForceOpenIn(forceOpenIn: Boolean): Args {
            this.forceOpenIn = forceOpenIn
            return this
        }

        fun setReceiverChatIdValue(chatId: Long): Args {
            this.receiverChatId = chatId
            return this
        }

        fun setTopicId(topicId: MessageTopic?): Args {
            this.topicId = topicId
            return this
        }

        var filter: SearchMessagesFilter? = null

        fun setSearchFilter(filter: SearchMessagesFilter?) {
            this.filter = filter
        }

        /* * */
        @DrawableRes
        var sendButtonIcon = 0

        fun setSendButtonIconRes(@DrawableRes icon: Int): Args {
            this.sendButtonIcon = icon
            return this
        }

        @DrawableRes
        var receiverRowIcon = 0
        var receiverRowText: CharSequence? = null

        fun setReceiverRow(@DrawableRes receiverRowIcon: Int, receiverRowText: CharSequence?): Args {
            this.receiverRowIcon = receiverRowIcon
            this.receiverRowText = receiverRowText
            return this
        }

        /* * */
        private var flags = 0

        fun setFlag(flag: Int): Args {
            this.flags = setFlag(flags, flag, true)
            return this
        }

        fun setFlag(flag: Int, value: Boolean): Args {
            this.flags = setFlag(flags, flag, value)
            return this
        }

        fun hasFlag(flag: Int): Boolean {
            return hasFlag(flags, flag)
        }

        companion object {
            @JvmStatic
            fun fromGallery(
                context: ViewController<*>?,
                delegate: MediaViewDelegate?,
                selectDelegate: MediaSelectDelegate?,
                sendDelegate: MediaSendDelegate?,
                galleryStack: MediaStack?,
                areOnlyScheduled: Boolean
            ): Args {
                return Args(context, MODE_GALLERY, delegate, selectDelegate, sendDelegate, galleryStack).setOnlyScheduled(areOnlyScheduled)
            }

            const val FLAG_DISALLOW_MULTI_SELECTION_MEDIA: Int = 1
            @JvmField
            val FLAG_DISALLOW_SET_DESTRUCTION_TIMER: Int = 1 shl 1
            @JvmField
            val FLAG_DELETE_FILE_ON_EXIT: Int = 1 shl 2
            @JvmField
            val FLAG_DISALLOW_SEND_BUTTON_HAPTIC_MENU: Int = 1 shl 3
        }
    }

    // Controller-related
    override fun getId(): Int {
        return R.id.controller_mediaView
    }

    // Stack-related stuff
    var mode: Int = 0
        private set
    private var delegate: MediaViewDelegate? = null
    private var selectDelegate: MediaSelectDelegate? = null
    private var sendDelegate: MediaSendDelegate? = null
    private var stack: MediaStack? = null
    private var filter: SearchMessagesFilter? = null
    private var topicId: MessageTopic? = null

    override fun setArguments(args: Args) {
        super.setArguments(args)
        this.mode = args.mode
        this.delegate = args.delegate
        this.selectDelegate = args.selectDelegate
        this.sendDelegate = args.sendDelegate
        this.stack = args.stack
        this.reverseMode = args.reverseMode
        this.filter = args.filter
        this.topicId = args.topicId
    }

    val currentTargetLocation: MediaViewThumbLocation?
        get() = if (delegate != null && !UI.isNavigationAnimating()) delegate!!.getTargetLocation(
            stack!!.getCurrentIndex(),
            stack!!.getCurrent()
        ) else null

    private var revealAnimator: FactorAnimator? = null
    private var revealAnimationType = 0
    private var currentThumb: MediaViewThumbLocation? = null

    /*MediaItem item = stack.getCurrent();
    return item != null && item.getSourceGalleryFile() != null && item.getSourceGalleryFile().isFromCamera();*/
    private var isCurrentCamera = false

    private var isInstant = false

    fun isFromCamera(): Boolean {
        return isCurrentCamera
    }

    fun forceCameraAnimationType(isInstant: Boolean) {
        this.isInstant = isInstant
        this.forceAnimationType = ANIMATION_TYPE_CAMERA
        this.isCurrentCamera = true
    }

    override fun prepareShowAnimation() {
        revealAnimator = FactorAnimator(ANIMATOR_REVEAL, this, DECELERATE_INTERPOLATOR, REVEAL_ANIMATION_DURATION)
        setLowProfile(true)
        onAppear()

        val location = this.currentTargetLocation

        if (location != null && !isInstant) {
            currentThumb = location
            setAnimatorType(ANIMATION_TYPE_REVEAL, true)
            mediaView!!.setTarget(location, 0f)
            hideCurrentCell()

            when (mode) {
                MODE_GALLERY -> {
                    setEditComponentsAlpha(0f)
                }
            }
        } else {
            currentThumb = null
            if (this.isCurrentCamera || isInstant) {
                setAnimatorType(ANIMATION_TYPE_CAMERA, true)
            } else {
                setAnimatorType(ANIMATION_TYPE_FADE, true)
                mediaView!!.setAlpha(0f)
            }
        }

        initHeaderStyle()
    }

    private fun setEditComponentsAlpha(alpha: Float) {
        editWrap!!.setAlpha(alpha)
        overlayView!!.setAlpha(alpha * otherFactor)
        if (othersView != null) {
            othersView!!.setAlpha(alpha * otherFactor)
        }
        when (currentSection) {
            SECTION_CAPTION -> {
                setBottomAlpha(alpha)
                setCheckAlpha(alpha * this.allowedCheckAlpha)
                setCounterAlpha(alpha * this.allowedCounterAlpha)
            }

            SECTION_FILTERS -> {
                filtersView!!.setAlpha(alpha)
            }

            SECTION_QUALITY -> {
                qualityControlWrap!!.setAlpha(alpha)
            }
        }
        updateBottomSpaceAlpha()
    }

    private fun hasCaption(): Boolean {
        if (captionView != null) {
            if (captionView is TextView) {
                return (captionView as TextView).getText().length > 0
            }
            if (captionView is CustomTextView) {
                return !isEmpty((captionView as CustomTextView).getText())
            }
        }
        return false
    }

    private fun setBottomAlpha(alpha: Float) {
        val visibility = alpha * headerVisible.getFloatValue() * (1f - pipFactor)
        if (hasCaption() || mode == MODE_GALLERY) {
            captionWrapView!!.setAlpha(visibility)
        }
        if (videoSliderView != null) {
            videoSliderView!!.setAlpha(visibility)
        }
        if (thumbsRecyclerView != null) {
            thumbsRecyclerView!!.setAlpha(visibility)
        }
        updateBottomSpaceAlpha()
    }

    private fun updateMainItemsAlpha() {
        setCheckAlpha(this.allowedCheckAlpha)
        setCounterAlpha(this.allowedCounterAlpha)
    }

    // Caption
    override fun provideInlineSearchChatId(v: InputView?): Long {
        return getArgumentsStrict().receiverChatId
    }

    override fun provideInlineSearchChat(v: InputView?): Chat? {
        val chatId = getArgumentsStrict().receiverChatId
        if (chatId != 0L) {
            return tdlib!!.chat(chatId)
        }
        return null
    }

    override fun provideInlineSearchChatUserId(v: InputView?): Long {
        val chat = tdlib!!.chat(provideInlineSearchChatId(v))
        return if (chat != null) TD.getUserId(chat) else 0
    }

    private var inlineResultsView: InlineResultsWrap? = null

    override fun provideOffset(v: InlineResultsWrap?): Int {
        val offset = if (keyboardFrameLayout != null && this.isEmojiVisible) max(0, keyboardFrameLayout!!.getMeasuredHeight() - bottomInnerMargin) else 0
        return (captionWrapView!!.getMeasuredHeight()) + offset
    }

    override fun showInlineResults(v: InputView?, results: ArrayList<InlineResult<*>>?, isContent: Boolean) {
        if (inlineResultsView == null) {
            if (results == null || results.isEmpty()) {
                return
            }

            inlineResultsView = InlineResultsWrap(context())
            inlineResultsView!!.setListener(captionView as PickListener?)
            inlineResultsView!!.setAlpha(inCaptionFactor)
            inlineResultsView!!.setOffsetProvider(this)
            inlineResultsView!!.setUseDarkMode(true)
        }

        if (results != null && !results.isEmpty()) {
            for (result in results) {
                result.setForceDarkMode(true)
            }
            if (inlineResultsView!!.getParent() == null) {
                popupView!!.addView(inlineResultsView)
                // FIXME inlineResultsView.addLick(popupView);
            }
        }

        inlineResultsView!!.showItems(this, results, isContent, (captionView as InputView).getInlineSearchContext(), false)
    }

    override fun addInlineResults(v: InputView?, items: ArrayList<InlineResult<*>?>?) {
        inlineResultsView!!.addItems(this, items, null)
    }

    override fun canSearchInline(v: InputView?): Boolean {
        return true
    }

    override fun onInputChanged(v: InputView?, input: String?) {
        if (emojiLayout != null) {
            emojiLayout!!.onTextChanged(input)
        }
    }

    private var inCaptionFactor = 0f

    private fun setInCaptionFactor(factor: Float) {
        if (this.inCaptionFactor != factor) {
            this.inCaptionFactor = factor
            updateMainItemsAlpha()
            if (inlineResultsView != null) {
                inlineResultsView!!.setAlpha(factor)
            }
            captionEmojiButton!!.setTranslationX(-captionEmojiButton!!.getMeasuredWidth() * (1f - factor))
            captionEmojiButton!!.setAlpha(factor)
            captionDoneButton!!.setAlpha(factor)
            captionView!!.setTranslationX(-(Screen.dp(55f) - Screen.dp(14f)) * (1f - factor))
        }
    }

    private fun onCaptionDone() {
        closeCaption()
    }

    private var inCaption = false
    private var inCaptionAnimator: FactorAnimator? = null
    fun isInCaption(): Boolean {
        return inCaption
    }

    override fun onClick(mediaView: MediaView?, x: Float, y: Float) {
        closeCaption()
    }

    private fun closeCaption() {
        if (inCaption) {
            if (this.isEmojiVisible) {
                forceCloseEmojiKeyboard()
            } else {
                Keyboard.hide(captionView)
            }
        }
    }

    private var keyboardFrameLayout: KeyboardFrameLayout? = null
    private var emojiLayout: EmojiLayout? = null
    private var textFormattingLayout: TextFormattingLayout? = null

    var isEmojiVisible: Boolean = false
        private set
    private var emojiState = false

    private fun processEmojiClick() {
        if (this.isEmojiVisible) {
            setInCaption(emojiState || getKeyboardState())
            closeEmojiKeyboard()
        } else {
            openEmojiKeyboard()
            setInCaption()
        }
    }

    override fun onEnterEmoji(emoji: String?) {
        (captionView as InputView).onEmojiSelected(emoji)
    }

    override fun onEnterCustomEmoji(sticker: TGStickerObj?) {
        (captionView as InputView).onCustomEmojiSelected(sticker)
    }

    override fun getOutputChatId(): Long {
        return if (selectDelegate != null) selectDelegate!!.getOutputChatId() else 0
    }

    override fun isEmojiInputEmpty(): Boolean {
        return isEmpty((captionView as TextView).getText())
    }

    override fun onDeleteEmoji() {
        if ((captionView as TextView).getText().length > 0) {
            captionView!!.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
        }
    }

    override fun onSearchRequested(layout: EmojiLayout?, areStickers: Boolean) {}


    private val keyboardOffset: Float
        get() = if (keyboardFrameLayout != null) keyboardFrameLayout!!.getLayoutTranslationOffset() else 0f

    private fun onKeyboardLayoutTranslation(translationY: Float) {
        checkBottomWrapY()
    }


    private fun openEmojiKeyboard() {
        if (!this.isEmojiVisible) {
            if (keyboardFrameLayout == null) {
                keyboardFrameLayout = object : KeyboardFrameLayout(context()) {
                    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
                        if (inlineResultsView != null) {
                            inlineResultsView!!.updatePosition(true)
                        }
                        checkCaptionButtonsY()
                    }
                }
                keyboardFrameLayout!!.setLayoutParams(newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))
                keyboardFrameLayout!!.setParentView(bottomWrap, contentView, popupView)
                keyboardFrameLayout!!.setUpdateTranslationListener(RunnableFloat { translationY: Float -> this.onKeyboardLayoutTranslation(translationY) })
                keyboardFrameLayout!!.useHideByDetachView()
                keyboardFrameLayout!!.setExtraBottomInset(systemInsets.bottom, systemInsets.bottom)

                textFormattingLayout = keyboardFrameLayout!!.contentView.textFormattingLayout
                textFormattingLayout!!.init(this, inputView, object : TextFormattingLayout.Delegate {
                    override fun onWantsCloseTextFormattingKeyboard() {
                        closeTextFormattingKeyboard()
                    }

                    override fun onWantsOpenTextFormattingKeyboard() {
                        openEmojiKeyboard()
                    }
                })

                emojiLayout = keyboardFrameLayout!!.contentView.emojiLayout
                emojiLayout!!.initWithMediasEnabled(this, false, this, this, false) // FIXME shall we use dark mode?
                emojiLayout!!.setAllowPremiumFeatures(tdlib!!.isSelfChat(getOutputChatId()))
                emojiLayout!!.setLayoutParams(newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                bottomWrap!!.addView(keyboardFrameLayout)
            }
            keyboardFrameLayout!!.setVisible(true)
            emojiState = getKeyboardState()

            setEmojiShown(true)
            if (emojiState) {
                captionEmojiButton!!.setImageResource(R.drawable.baseline_keyboard_24)
                keyboardFrameLayout!!.hideKeyboard(captionView as EditText?)
            } else {
                captionEmojiButton!!.setImageResource(R.drawable.baseline_direction_arrow_down_24)
            }
        }
    }

    private fun removeEmojiView() {
        if (keyboardFrameLayout != null) {
            keyboardFrameLayout!!.setVisible(false)
        }
    }

    private fun forceCloseEmojiKeyboard() {
        if (this.isEmojiVisible) {
            removeEmojiView()
            setEmojiShown(false)
            captionEmojiButton!!.setImageResource(this.targetIcon)
            setInCaption()
        }
    }

    private fun closeEmojiKeyboard(byKeyboardOpen: Boolean = false) {
        if (this.isEmojiVisible) {
            if (keyboardFrameLayout != null) {
                removeEmojiView()
                if (emojiState && !byKeyboardOpen) {
                    keyboardFrameLayout!!.showKeyboard(captionView as EditText?)
                }
            }
            setEmojiShown(false)
            captionEmojiButton!!.setImageResource(this.targetIcon)
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
            context,
            anchorView,
            viewToDisplay,
            viewWidth,
            viewHeight,
            horizontalMargin,
            horizontalOffset,
            verticalOffset,
            contentView,
            bottomWrap,
            keyboardFrameLayout
        )
    }

    override fun removeView(context: EmojiToneHelper?, displayedView: View?) {
        contentView!!.removeView(displayedView)
    }

    // Other
    private fun checkCaptionButtonsY() {
        val offset =
            (if (keyboardFrameLayout != null && keyboardFrameLayout!!.getVisibility() == View.VISIBLE && keyboardFrameLayout!!.getParent() != null) keyboardFrameLayout!!.getMeasuredHeight() else 0).toFloat()
        captionEmojiButton!!.setTranslationY(-offset)
        captionDoneButton!!.setTranslationY(-offset)
        captionWrapView!!.setTranslationY(-offset)
        if (inlineResultsView != null) {
            inlineResultsView!!.updatePosition(true)
        }
        /*FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) captionWrapView.getLayoutParams();
    if (params.bottomMargin != offset) {
      params.bottomMargin = offset;
      captionWrapView.setLayoutParams(params);
    }*/
    }

    private fun setInCaption() {
        setInCaption(getKeyboardState() || this.isEmojiVisible)
    }

    private val bottomWrapMargin: Int
        get() = (if (inCaption || mode != MODE_GALLERY) 0 else Screen.dp(56f)) + (if (this.isEmojiVisible) 0 else controlsMargin)

    private fun setInCaption(inCaption: Boolean) {
        var inCaption = inCaption
        inCaption = inCaption && currentSection == SECTION_CAPTION
        if (this.inCaption != inCaption) {
            this.inCaption = inCaption

            if (!inCaption && !isEmptyOrBlank((captionView as TextView).getText())) {
                selectMediaIfItsNot()
            }

            updateCaptionLayout()
            captionEmojiButton!!.setEnabled(inCaption)
            captionDoneButton!!.setEnabled(inCaption)
            mediaView!!.setDisableTouch(inCaption)
            mediaView!!.setButStillNeedClick(if (inCaption) this else null)

            updateBottomWrapMargin()
            editWrap!!.setVisibility(if (inCaption) View.GONE else View.VISIBLE)
            updateSliderAlpha()
            updateBottomSpaceAlpha()
            if (this.inCaptionAnimator == null) {
                this.inCaptionAnimator = FactorAnimator(ANIMATOR_ID_CAPTION, this, DECELERATE_INTERPOLATOR, 180L, this.inCaptionFactor)
            }
            inCaptionAnimator!!.animateTo(if (inCaption) 1f else 0f)

            if (!inCaption) {
                UI.post(Runnable {
                    if (!isDestroyed() && !this@MediaViewController.inCaption) {
                        setLowProfile(true)
                    }
                }, 100)
            }
        }
    }

    private fun applyInCaption(inCaption: Boolean) {
        // TODO apply margins, reset translations
    }

    private val allowedCheckAlpha: Float
        get() = (1f - otherFactor) * (1f - mainSectionDisappearFactor) * (1f - inCaptionFactor) * (1f - this.cameraFactor) * (if (selectDelegate != null) 1f else 0f)

    private val cameraFactor: Float
        get() = if (this.isCurrentCamera) 1f else 0f

    private val allowedCounterAlpha: Float
        get() = max(
            0f,
            min(1f, counterFactor)
        ) * (1f - otherFactor) * (1f - mainSectionDisappearFactor) * (1f - inCaptionFactor) * (1f - this.cameraFactor)

    private fun showCurrentCell() {
        if (hiddenCell != null && delegate != null) {
            delegate!!.setMediaItemVisible(hiddenCellIndex, hiddenCell, true)
        }
        hiddenCell = null
    }

    private var hiddenCellIndex = 0
    private var hiddenCell: MediaItem? = null

    private fun hideCurrentCell() {
        val currentItem = stack!!.getCurrent()
        if (hiddenCell == null || hiddenCell !== currentItem) {
            if (hiddenCell != null && delegate != null) {
                delegate!!.setMediaItemVisible(hiddenCellIndex, hiddenCell, true)
            }
            hiddenCell = currentItem
            hiddenCellIndex = stack!!.getCurrentIndex()
            if (delegate != null) {
                delegate!!.setMediaItemVisible(hiddenCellIndex, hiddenCell, false)
            }
        }
    }

    private fun setAnimatorType(type: Int, isOpen: Boolean) {
        mediaView!!.setDisableAnimations(isOpen)
        this.revealAnimationType = type
        when (revealAnimationType) {
            ANIMATION_TYPE_FADE -> {
                revealAnimator!!.setInterpolator(DECELERATE_INTERPOLATOR)
                revealAnimator!!.setDuration(if (mode != MODE_SECRET && !animationAlreadyDone) 180L else 0L)
            }

            ANIMATION_TYPE_CAMERA -> {
                revealAnimator!!.setInterpolator(LINEAR_INTERPOLATOR)
                revealAnimator!!.setDuration(0)
            }

            ANIMATION_TYPE_SECRET_CLOSE -> {
                revealAnimator!!.setInterpolator(DECELERATE_INTERPOLATOR)
                revealAnimator!!.setDuration(220L)
            }

            ANIMATION_TYPE_PIP_CLOSE -> {
                revealAnimator!!.setInterpolator(DECELERATE_INTERPOLATOR)
                revealAnimator!!.setDuration(190L)
            }

            ANIMATION_TYPE_REVEAL -> {
                if ((mode == MODE_GALLERY || isOpen) && !this.isCurrentCamera) {
                    //revealAnimator.setInterpolator(isOpen ? OVERSHOOT_INTERPOLATOR : OVERSHOOT_INTERPOLATOR_2);
                    //revealAnimator.setDuration(280l);
                    revealAnimator!!.setInterpolator(DECELERATE_INTERPOLATOR)
                    revealAnimator!!.setDuration(REVEAL_OPEN_ANIMATION_DURATION)
                } else {
                    revealAnimator!!.setInterpolator(DECELERATE_INTERPOLATOR)
                    revealAnimator!!.setDuration(REVEAL_ANIMATION_DURATION)
                }
            }

            ANIMATION_TYPE_SLIDEOFF -> {}
        }
    }

    override fun launchShowAnimation(popup: PopupLayout?) {
        if (revealAnimationType == ANIMATION_TYPE_REVEAL) {
            mediaView!!.setPendingOpenAnimator(revealAnimator)
        } else {
            revealAnimator!!.animateTo(1f)
        }
    }

    private fun setLowProfile(isLowProfile: Boolean) {
        if (isLowProfile) {
            if (mode == MODE_SECRET && Config.CUTOUT_ENABLED) {
                context().setWindowDecorSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LOW_PROFILE,
                    true
                )
            } else {
                context().setWindowDecorSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE, true)
            }
        } else {
            context().setWindowDecorSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE, true)
        }
    }

    private fun setFullScreen(isFullscreen: Boolean) {
        if (mode != MODE_GALLERY && Config.CUTOUT_ENABLED) {
            if (isFullscreen && (mode == MODE_MESSAGES || mode == MODE_SIMPLE)) {
                val c = context.navigation().getCurrentStackItem()
                if (c != null) {
                    c.hideSoftwareKeyboard()
                }
            }
            if (isFullscreen) {
                context().addFullScreenView(this, true)
            } else {
                context().removeFullScreenView(this, true)
            }
        }
    }

    override fun onKeyboardStateChanged(visible: Boolean): Boolean {
        if (mode == MODE_GALLERY) {
            if (visible && !getKeyboardState()) {
                closeEmojiKeyboard(true)
            }
            val res = super.onKeyboardStateChanged(visible)
            if (keyboardFrameLayout != null) {
                keyboardFrameLayout!!.onKeyboardStateChanged(visible)
            }
            setInCaption(visible || this.isEmojiVisible)
            mediaView!!.layoutCells()
            return res
        }
        return super.onKeyboardStateChanged(visible)
    }

    private var forceAnimationType = -1

    override fun launchHideAnimation(popup: PopupLayout?, ignored: FactorAnimator?): Boolean {
        val location: MediaViewThumbLocation?

        if (forceAnimationType != -1) {
            location = null
        } else {
            location = this.currentTargetLocation
        }

        if (mode != MODE_SECRET || forceAnimationType == -1) {
            setLowProfile(false)
        }

        revealAnimator!!.cancel()
        currentThumb = null

        /*if (revealAnimator.getFactor() == 0f) {
      popup.onCustomHideAnimationComplete();
      return true;
    }*/
        currentThumb = location
        val animationType = if (forceAnimationType != -1) forceAnimationType else if (location != null) ANIMATION_TYPE_REVEAL else ANIMATION_TYPE_FADE

        if (animationType != ANIMATION_TYPE_PIP_CLOSE) {
            mediaView!!.setTarget(location, 1f)
        }

        if (revealAnimationType != ANIMATION_TYPE_PIP_CLOSE && !Config.DISABLE_VIEWER_ELEVATION) {
            mediaView!!.setTranslationZ(0f)
            mediaView!!.invalidateOutline()
        }

        when (animationType) {
            ANIMATION_TYPE_REVEAL -> {
                mediaView!!.getBaseCell().getDetector().preparePositionReset()
                hideCurrentCell()
                setAnimatorType(ANIMATION_TYPE_REVEAL, false)
            }

            ANIMATION_TYPE_FADE, ANIMATION_TYPE_SECRET_CLOSE -> {
                setAnimatorType(animationType, false)
                mediaView!!.setAlpha(1f)
            }

            ANIMATION_TYPE_PIP_CLOSE -> {
                setAnimatorType(ANIMATION_TYPE_PIP_CLOSE, false)
            }
        }

        if (animationType == ANIMATION_TYPE_SECRET_CLOSE) {
            revealAnimator!!.setStartDelay(if (revealAnimator!!.getFactor() == 0f) 0L else 70L)
        } else if (revealAnimationType != ANIMATION_TYPE_PIP_CLOSE) {
            revealAnimator!!.setStartDelay(100)
        }
        revealAnimator!!.animateTo(0f)

        return true
    }

    private var fadeFactor = 0f

    private fun setFadeFactor(factor: Float) {
        if (this.fadeFactor != factor) {
            this.fadeFactor = factor
            mediaView!!.setAlpha(max(0f, min(1f, factor)))
        }
    }

    private val path: Path? = Path()

    private fun layoutPath() {
        if (path != null && currentThumb != null && commonFactor > 0f && commonFactor < 1f) {
            // v1

            val factor = max(0f, min(1f, commonFactor))

            val fromX = currentThumb!!.centerX()

            // int fromY = currentThumb.centerY();
            val width = contentView!!.getMeasuredWidth()
            val height = contentView!!.getMeasuredHeight()

            /*int targetX = width / 2;
      int targetY = height / 2;*/
            val receiver = mediaView!!.getBaseReceiver()

            /*int imageWidth = receiver.getRight() - receiver.getLeft();
      int imageHeight = receiver.getBottom() - receiver.getTop();*/
            val startRadius = (min(
                currentThumb!!.width(),
                currentThumb!!.height()
            ) / 2).toFloat() //(float) Math.sqrt(imageWidth * imageWidth + imageHeight * imageHeight) * .5f;
            val radius = sqrt((width * width + height * height).toDouble()).toFloat() * .5f

            /*float centerX = fromX + (float) (targetX - fromX) * factor;
      float centerY = fromY + (float) (targetY - fromY) * factor;*/
            val targetRadius = startRadius + (radius - startRadius) * factor


            val rectF = Paints.getRectF()
            rectF.set(
                receiver.centerX() - targetRadius,
                receiver.centerY() - targetRadius,
                receiver.centerX() + targetRadius,
                receiver.centerY() + targetRadius
            )

            path.reset()
            path.addRoundRect(rectF, targetRadius, targetRadius, Path.Direction.CCW)
        }
    }

    private var commonFactor = 0f

    private fun updatePhotoRevealFactor() {
        val factor = commonFactor * (1f - pipFactor)
        context.setPhotoRevealFactor(factor)
    }

    private fun setCommonFactor(factor: Float) {
        if (this.commonFactor != factor) {
            require(!java.lang.Float.isNaN(factor))
            this.commonFactor = factor
            updatePhotoRevealFactor()
            if (path != null && currentThumb != null && commonFactor > 0f && commonFactor < 1f) {
                layoutPath()
            }
            contentView!!.setWillNotDraw(factor == 0f)
            contentView!!.invalidate()
            checkThumbsItemAnimator()
        }
    }

    fun onMediaZoomStart() {
        if (mode == MODE_GALLERY && currentSection == SECTION_PAINT && paintView != null) {
            paintView!!.getContentWrap().cancelDrawingByZoom()
        }
    }

    override fun onFactorChanged(id: Int, factor: Float, fraction: Float, callee: FactorAnimator?) {
        when (id) {
            ANIMATOR_REVEAL -> {
                when (revealAnimationType) {
                    ANIMATION_TYPE_FADE -> {
                        setFadeFactor(factor)
                    }

                    ANIMATION_TYPE_SECRET_CLOSE -> {
                        if (secretView != null) {
                            secretView!!.setAlpha(factor)
                        }
                        // mediaView.forceZoom(.75f + .25f * factor);
                        mediaView!!.setAlpha(factor)
                    }

                    ANIMATION_TYPE_REVEAL -> {
                        mediaView!!.getBaseCell().getDetector().setPositionFactor(fraction)
                        mediaView!!.setRevealFactor(factor)
                    }

                    ANIMATION_TYPE_PIP_CLOSE -> {
                        setPipDismissFactor(factor)
                    }

                    ANIMATION_TYPE_SLIDEOFF -> {}
                }
                setCommonFactor(factor)
                setHeaderAlpha(clamp(factor))
                when (mode) {
                    MODE_GALLERY -> {
                        val alpha = max(0f, min(1f, factor))
                        setEditComponentsAlpha(alpha)
                    }

                    MODE_MESSAGES, MODE_SIMPLE -> {
                        setBottomAlpha(clamp(factor))
                    }
                }
            }

            ANIMATOR_EDIT_MODE_VISIBILITY -> {
                setHeaderAlpha(if (revealAnimator != null) revealAnimator!!.getFactor() else 1f)
            }

            ANIMATOR_COUNTER -> {
                setCounterFactor(factor)
            }

            ANIMATOR_OTHER -> {
                setOtherFactor(factor)
            }

            ANIMATOR_SECTION -> {
                setSectionChangeFactor(factor, fraction)
            }

            ANIMATOR_VIDEO -> {
                setVideoFactor(factor)
            }

            ANIMATOR_CAPTION -> {
                setCaptionFactor(factor)
            }

            ANIMATOR_HEADER -> {
                setHeaderVisibilityFactor(factor)
            }

            ANIMATOR_PIP -> {
                setPipFactor(factor, fraction)
            }

            ANIMATOR_PIP_UP -> {
                setPipUpFactor(factor)
            }

            ANIMATOR_PIP_CONTROLS -> {
                setPipControlsFactor(factor)
            }

            ANIMATOR_PIP_POSITION -> {
                setPipPositionFactor(factor)
            }

            ANIMATOR_PIP_HIDE -> {
                setPipHideFactor(factor)
            }

            ANIMATOR_PRIVACY -> {
                mediaView!!.getBaseReceiver().setAlpha(1f - factor)
            }

            ANIMATOR_SLIDE -> {
                setSlideFactor(factor)
            }

            ANIMATOR_ID_CAPTION -> {
                setInCaptionFactor(factor)
            }

            ANIMATOR_CROP -> {
                setCropFactor(factor)
            }

            ANIMATOR_IMAGE_ROTATE -> {
                setImageRotateFactor(factor)
            }

            ANIMATOR_IMAGE_FLIP_HORIZONTALLY, ANIMATOR_IMAGE_FLIP_VERTICALLY -> {
                setImageMirrorFactors()
            }

            ANIMATOR_PAINT_HIDE -> {
                setHidePaint(factor)
            }

            ANIMATOR_THUMBS -> {
                setThumbsFactor(factor)
            }

            ANIMATOR_THUMBS_SCROLL -> {
                setThumbsScrollFactor(factor)
            }

            ANIMATOR_THUMBS_AUTO_SCROLLER -> {
                setAutoThumbScrollFactor(factor)
            }
        }
    }

    override fun onPopupCompletelyShown(popup: PopupLayout?) {
        if (inProfilePhotoEditMode()) {
            UI.post(Runnable { openCrop(true) })
        }
    }

    private fun onAppear() {
        // Nothing to do?
    }

    private fun onHide() {
        // Nothing to do anymore?
    }

    override fun onFactorChangeFinished(id: Int, finalFactor: Float, callee: FactorAnimator?) {
        when (id) {
            ANIMATOR_REVEAL -> {
                if (finalFactor == 0f) {
                    showCurrentCell()
                    popupView!!.onCustomHideAnimationComplete()
                    setFullScreen(false)
                    if (mode == MODE_SECRET && revealAnimationType == ANIMATION_TYPE_SECRET_CLOSE) {
                        setLowProfile(false)
                    }
                    onHide()
                } else if (finalFactor == 1f) {
                    context().getRootView().forceHideKeyboard()
                    popupView!!.onCustomShowComplete()
                    mediaView!!.setDisableAnimations(false)
                    if (!SET_FULLSCREEN_ON_OPEN) {
                        setFullScreen(true)
                    }
                    mediaView!!.autoplayIfNeeded(false)
                    val openedItem = stack!!.getCurrent()
                    UI.post(Runnable { openedItem.viewContent(false) }, 20)
                    if (canSendAsFile() != SEND_MODE_NONE && Settings.instance().needTutorial(Settings.TUTORIAL_SEND_AS_FILE)) {
                        Settings.instance().markTutorialAsShown(Settings.TUTORIAL_SEND_AS_FILE)
                        context().tooltipManager().builder(sendButton).color(context().tooltipManager().overrideColorProvider(this@MediaViewController.forcedTheme))
                            .locate(TooltipOverlayView.LocationProvider { targetView: View?, outRect: Rect? ->
                                outRect!!.top += Screen.dp(8f)
                                outRect.bottom -= Screen.dp(8f)
                                outRect.left -= Screen.dp(4f)
                                outRect.right -= Screen.dp(4f)
                            }).controller(this).show(tdlib, R.string.HoldToSendAsFile).hideDelayed()
                    }
                }
            }

            ANIMATOR_OTHER -> {
                if (finalFactor == 0f) {
                    otherAdapter!!.setImages(null)
                }
            }

            ANIMATOR_SECTION -> {
                if (finalFactor == 1f) {
                    applySection()
                }
            }

            ANIMATOR_CAPTION -> {
                if (finalFactor == 0f) {
                    setCaption("", null)
                }
            }

            ANIMATOR_PIP -> {
                applyPipMode()
            }

            ANIMATOR_PIP_POSITION -> {
                applyPipPosition()
            }

            ANIMATOR_PRIVACY -> {}
            ANIMATOR_SLIDE -> {
                if (finalFactor == 1f && (toSlideY != 0f || toSlideX != 0f)) {
                    applySlide()
                }
            }

            ANIMATOR_ID_CAPTION -> {
                applyInCaption(finalFactor == 1f)
            }

            ANIMATOR_CROP -> {
                applyCropMode(finalFactor == 1f)
            }

            ANIMATOR_IMAGE_ROTATE -> {
                if (finalFactor == 1f) {
                    applyImageRotation()
                }
            }

            ANIMATOR_IMAGE_FLIP_HORIZONTALLY, ANIMATOR_IMAGE_FLIP_VERTICALLY -> {
                applyImageMirror()
            }

            ANIMATOR_THUMBS -> {
                if (finalFactor == 0f) {
                    clearThumbsView()
                }
            }
        }
    }

    private fun setCaption(text: String?, entities: Array<TextEntity?>?) {
        if (captionView is TextView) {
            (captionView as TextView).setText(text)
        } else if (captionView is CustomTextView) {
            (captionView as CustomTextView).setText(text, entities, false)
        }
    }

    private fun updateCaption(animated: Boolean) {
        val item = stack!!.getCurrent()
        when (mode) {
            MODE_GALLERY -> {
                ignoreCaptionUpdate = true
                val caption = item.getCaption()
                if (caption != null) {
                    (captionView as InputView).setInput(TD.toCharSequence(caption), true, false)
                } else {
                    (captionView as InputView).setInput("", true, false)
                }
                ignoreCaptionUpdate = false
            }

            MODE_MESSAGES, MODE_SIMPLE -> {
                val isVisible = item.getCaption() != null
                if (isVisible) {
                    (captionView as CustomTextView).setText(item.getCaption().text, item.getCaptionEntities(), false)
                    if (!animated && !this.isCaptionVisible) {
                        this.isCaptionVisible = true
                        this.captionFactor = 1f
                    }
                }
                setCaptionVisible(isVisible, animated)
            }
        }
    }

    private var isCaptionVisible = false
    private var captionFactor = 0f
    private var captionAnimator: FactorAnimator? = null
    private fun setCaptionVisible(isVisible: Boolean, animated: Boolean) {
        if (this.isCaptionVisible != isVisible) {
            this.isCaptionVisible = isVisible
            if (animated) {
                animateCaptionFactor(if (isVisible) 1f else 0f)
            } else {
                forceCaptionFactor(if (isVisible) 1f else 0f)
            }
        }
    }

    private fun animateCaptionFactor(toFactor: Float) {
        if (captionAnimator == null) {
            captionAnimator = FactorAnimator(ANIMATOR_CAPTION, this, DECELERATE_INTERPOLATOR, BOTTOM_ANIMATION_DURATION, captionFactor)
        }
        captionAnimator!!.animateTo(toFactor)
    }

    private fun forceCaptionFactor(factor: Float) {
        if (captionAnimator != null) {
            captionAnimator!!.forceFactor(factor)
        }
        setCaptionFactor(factor)
    }

    private fun setCaptionFactor(factor: Float) {
        if (this.captionFactor != factor) {
            this.captionFactor = factor
            captionWrapView!!.setAlpha(factor * headerVisible.getFloatValue())
            updateBottomSpaceAlpha()
        }
    }

    private fun toggleMute() {
        if (Config.MUTE_VIDEO_AVAILABLE) {
            val isMuted = stack!!.getCurrent().toggleMute()
            paintOrMuteButton!!.setActive(isMuted, true)
            mediaView!!.getBaseCell().updateMute()
            updateQualityInfo()
        } else {
            UI.showApiLevelWarning(Build.VERSION_CODES.JELLY_BEAN_MR2)
        }
    }

    private fun updateVideoState(animated: Boolean) {
        val item = stack!!.getCurrent()
        setVideoVisible(
            item.isVideo(),
            Config.VIDEO_CLOUD_PLAYBACK_AVAILABLE || item.isLoaded(),
            item.getVideoDuration(true, TimeUnit.MILLISECONDS),
            TimeUnit.MILLISECONDS,
            animated
        )
        if (mode == MODE_GALLERY) {
            val isVideo = item.isVideo()
            if (isVideo) {
                adjustOrTextButton!!.setIcon(R.drawable.baseline_settings_24, animated, false)
                if (!Config.MODERN_VIDEO_TRANSCODING_ENABLED) {
                    cropOrStickerButton!!.setIcon(R.drawable.baseline_rotate_90_degrees_ccw_24, animated, false)
                }
                paintOrMuteButton!!.setIcon(R.drawable.baseline_volume_up_24, animated, item.needMute())
            } else {
                adjustOrTextButton!!.setIcon(R.drawable.baseline_tune_24, animated, false)
                if (!Config.MODERN_VIDEO_TRANSCODING_ENABLED) {
                    cropOrStickerButton!!.setIcon(R.drawable.baseline_crop_rotate_24, animated, false)
                }
                paintOrMuteButton!!.setIcon(R.drawable.baseline_brush_24, animated, false)
            }
            // paintButton.setVisibility(isVideo ? View.GONE : View.VISIBLE);
            // setHidePaint(isVideo, animated);
        }
    }

    private var hidePaint = false
    private var hideAnimator: BoolAnimator? = null

    private fun setHidePaint(hidePaint: Boolean, animated: Boolean) {
        if (this.hidePaint != hidePaint) {
            this.hidePaint = hidePaint
            if (hideAnimator == null) {
                hideAnimator = BoolAnimator(ANIMATOR_PAINT_HIDE, this, DECELERATE_INTERPOLATOR, 290L)
            } else {
                hideAnimator!!.setDuration((if (hidePaint) 290 else 380).toLong())
            }
            hideAnimator!!.setValue(hidePaint, animated)
        }
    }

    private fun setHidePaint(factor: Float) {
        paintOrMuteButton!!.setAlpha(1f - factor)
        val scale = .6f + .4f * (1f - factor)
        paintOrMuteButton!!.setScaleX(scale)
        paintOrMuteButton!!.setScaleY(scale)
        val width = Views.getParamsWidth(paintOrMuteButton)
        var found = false
        for (i in 0..<editButtons!!.getChildCount()) {
            val view = editButtons!!.getChildAt(i)
            if (view === paintOrMuteButton) {
                found = true
            } else if (found) {
                view.setTranslationX(-width / 2 * factor)
            } else {
                view.setTranslationX(width / 2 * factor)
            }
        }
    }

    // Current photo changed
    override fun onMediaChanged(index: Int, estimatedTotalSize: Int, currentItem: MediaItem?, itemCountChanged: Boolean) {
        when (mode) {
            MODE_GALLERY -> {
                checkView!!.setChecked(selectDelegate != null && selectDelegate!!.isMediaItemSelected(index, currentItem))
                updateIconStates(true)
                updateVideoState(true)
                updateCaption(true)
            }

            MODE_MESSAGES, MODE_SIMPLE -> {
                if (!itemCountChanged) {
                    updateVideoState(true)
                }
                updateCaption(true)

                updateHeaderButtons()
                onMediaStackChanged(itemCountChanged)

                loadMoreIfNeeded()
            }

            MODE_PROFILE, MODE_CHAT_PROFILE -> {
                onMediaStackChanged(itemCountChanged)
                loadMoreIfNeeded()
            }
        }
    }

    // Controller-related stuff
    protected override fun getHeaderColorId(): Int {
        return ColorId.transparentEditor
    }

    protected override fun getHeaderTextColorId(): Int {
        return ColorId.white
    }

    protected override fun getHeaderIconColorId(): Int {
        return ColorId.white
    }

    protected override fun getMenuId(): Int {
        val current = stack!!.getCurrent()
        if (current != null && current.isViewOnce()) {
            return 0
        }
        return R.id.menu_photo
    }

    protected override fun getBackButtonResource(): Int {
        return R.drawable.bg_btn_header_light
    }

    protected override fun getBackButton(): Int {
        return BackHeaderButton.TYPE_BACK
    }

    private fun canGoPip(): Boolean {
        val current = stack!!.getCurrent()
        return current != null && current.isVideo() && !current.isGifType()
    }

    private fun canEdit(): Boolean {
        val current = stack!!.getCurrent()
        return (mode == MODE_MESSAGES || mode == MODE_SIMPLE) && current != null && !current.isVideo() && !current.isGifType() && (current.canBeShared() && current.canBeSaved() && !tdlib!!.hasRestriction(
            current.getSourceChatId(),
            RightId.SEND_PHOTOS
        ))
    }

    private fun canShare(): Boolean {
        return false
    }

    private fun canViewMasks(): Boolean {
        return false
    }

    override fun fillMenuItems(id: Int, header: HeaderView, menu: LinearLayout) {
        if (Config.MASKS_TEXTS_AVAILABLE) {
            val masksButton = header.genButton(R.id.menu_btn_masks, R.drawable.deproko_baseline_masks_24, ColorId.white, null, Screen.dp(49f), header)
            masksButton.setBackgroundResource(R.drawable.bg_btn_header_light)
            masksButton.setVisibility(if (canViewMasks()) View.VISIBLE else View.GONE)
            menu.addView(masksButton)
        }

        val pipButton = header.genButton(R.id.menu_btn_pictureInPicture, R.drawable.deproko_baseline_outinline_24, ColorId.white, null, Screen.dp(49f), header)
        pipButton.setBackgroundResource(R.drawable.bg_btn_header_light)
        pipButton.setVisibility(if (canGoPip()) View.VISIBLE else View.GONE)
        menu.addView(pipButton)

        val shareButton = header.addForwardButton(menu, null, ColorId.white)
        shareButton.setBackgroundResource(R.drawable.bg_btn_header_light)
        shareButton.setVisibility(if (canShare()) View.VISIBLE else View.GONE)

        val editButton = header.addEditButton(menu, null, ColorId.white)
        editButton.setImageResource(R.drawable.baseline_brush_24)
        editButton.setBackgroundResource(R.drawable.bg_btn_header_light)
        editButton.setVisibility(if (canEdit()) View.VISIBLE else View.GONE)

        val moreButton = header.addMoreButton(menu, null, ColorId.white)
        moreButton.setBackgroundResource(R.drawable.bg_btn_header_light)
    }

    private fun openMoreMenu() {
        val ids = IntList(4)
        val strings = StringList(4)

        val item = stack!!.getCurrent()

        val chat = tdlib!!.chat(item.getSourceChatId())
        val message = item.getMessage()

        if (message != null && tdlib!!.canEditMedia(message, tdlib!!.getMessagePropertiesSync(message), true)) {
            ids.append(R.id.btn_replace)
            strings.append(if (item.isVideo()) R.string.ReplaceVideo else R.string.ReplaceImage)
        }

        if (item.isLoaded() && item.canBeSaved()) {
            if ((item.isVideo() && !item.isGifType()) || (getArgumentsStrict().forceOpenIn)) {
                ids.append(R.id.btn_open)
                strings.append(R.string.OpenInExternalApp)
            }
            ids.append(R.id.btn_saveToGallery)
            strings.append(R.string.SaveToGallery)
        }

        if (mode != MODE_SECRET && mode != MODE_GALLERY && item.canBeSaved() && item.canBeShared()) {
            ids.append(R.id.btn_share)
            strings.append(R.string.Share)
        }

        if (item.isGifType() && item.canBeSaved()) {
            ids.append(R.id.btn_saveGif)
            strings.append(R.string.SaveGif)
        }

        if (!isEmpty(getArgumentsStrict().copyLink) || (chat != null && tdlib!!.canCopyPostLink(item.getMessage()))) {
            ids.append(R.id.btn_copyLink)
            strings.append(R.string.CopyLink)
        }

        if (item.getSourceChatId() != 0L && item.getSourceMessageId() != 0L && mode == MODE_MESSAGES) {
            ids.append(R.id.btn_showInChat)
            strings.append(R.string.ShowInChat)
        }

        if (item.canBeReported() && (item.getMessage() != null || stack!!.getCurrentIndex() == 0)) {
            ids.append(R.id.btn_messageReport)
            strings.append(R.string.Report)
        }

        val isSelfProfile = mode == MODE_PROFILE && tdlib!!.isSelfSender(item.getSourceSender())
        var canDelete = isSelfProfile
        if (!canDelete && mode == MODE_CHAT_PROFILE) {
            canDelete = chat != null && tdlib!!.canChangeInfo(chat)
        }
        if (isSelfProfile && stack!!.getCurrentIndex() != 0) {
            ids.append(R.id.btn_setProfilePhoto)
            strings.append(R.string.SetAsCurrent)
        }
        if (canDelete) {
            ids.append(R.id.btn_deleteProfilePhoto)
            strings.append(R.string.Delete)
        }

        if (!ids.isEmpty()) {
            showMore(ids.get(), strings.get(), 0, canRunFullscreen())
        }
    }

    override fun onMenuItemPressed(id: Int, view: View?) {
        if (id == R.id.menu_btn_pictureInPicture) {
            enterPictureInPicture()
        } else if (id == R.id.menu_btn_forward) {
            // ...
        } else if (id == R.id.menu_btn_edit) {
            openForceEditMode()
        } else if (id == R.id.menu_btn_more) {
            openMoreMenu()
        } else if (id == R.id.menu_btn_masks) {
        }
    }

    private val forcedTheme: ThemeDelegate
        get() =// TODO actually move this to ViewController?
            ThemeSet.getBuiltinTheme(ThemeId.NIGHT_BLACK)

    override fun shouldDisallowScreenshots(): Boolean {
        return mode == MODE_SECRET || !stack!!.getCurrent().canBeSaved() || super.shouldDisallowScreenshots()
    }

    override fun onMoreItemPressed(id: Int) {
        val item = stack!!.getCurrent()
        if (id == R.id.btn_saveToGallery) {
            val file = item.getTargetFile()
            tdlib!!.files().isFileLoadedAndExists(file, RunnableBool { isLoadedAndExists: Boolean ->
                if (isLoadedAndExists) {
                    runOnUiThreadOptional(Runnable {
                        U.copyToGallery(
                            context,
                            file.local.path,
                            if (item.isAnimatedAvatar() || item.isGifType()) U.TYPE_GIF else if (item.isVideo()) U.TYPE_VIDEO else U.TYPE_PHOTO
                        )
                    })
                }
            })
        } else if (id == R.id.btn_saveGif) {
            val file = item.getTargetFile()
            if (file != null) {
                tdlib!!.ui().saveGif(file.id)
            }
        } else if (id == R.id.btn_messageReport) {
            val message = item.getMessage()
            if (message != null) {
                TdlibUi.reportChat(this, item.getSourceChatId(), arrayOf<TdApi.Message>(message), this@MediaViewController.forcedTheme, null, true)
            } else {
                val chatId = item.getSourceSender().getSenderId()
                val act = RunnableData { photoSize: PhotoSize? ->
                    if (photoSize != null) {
                        tdlib!!.ui().post(Runnable { TdlibUi.reportChatPhoto(this, chatId, photoSize.photo.id, null, this@MediaViewController.forcedTheme) }
                        )
                    }
                }
                when (getType(chatId)) {
                    TdApi.ChatTypeBasicGroup.CONSTRUCTOR -> {
                        tdlib!!.cache().basicGroupFull(toBasicGroupId(chatId), RunnableData { groupFull: BasicGroupFullInfo? ->
                            if (groupFull != null && groupFull.photo != null) {
                                act.runWithData(groupFull.photo!!.sizes.findBiggest())
                            }
                        })
                    }

                    TdApi.ChatTypePrivate.CONSTRUCTOR, TdApi.ChatTypeSecret.CONSTRUCTOR -> {
                        val userId = tdlib!!.chatUserId(chatId)
                        tdlib!!.cache().userFull(userId, RunnableData { userFull: UserFullInfo? ->
                            if (userFull != null && userFull.photo != null) {
                                act.runWithData(userFull.photo!!.sizes.findBiggest())
                            }
                        })
                    }

                    TdApi.ChatTypeSupergroup.CONSTRUCTOR -> {
                        tdlib!!.cache().supergroupFull(toSupergroupId(chatId), RunnableData { supergroupFull: SupergroupFullInfo? ->
                            if (supergroupFull != null && supergroupFull.photo != null) {
                                act.runWithData(supergroupFull.photo!!.sizes.findBiggest())
                            }
                        })
                    }
                }
            }
        } else if (id == R.id.btn_copyLink) {
            if (!isEmpty(getArgumentsStrict().copyLink)) {
                UI.copyText(getArgumentsStrict().copyLink, R.string.CopiedLink)
            } else if (item.getSourceChatId() != 0L) {
                if (tdlib!!.canCopyPostLink(item.getMessage())) {
                    tdlib!!.getMessageLink(
                        item.getMessage(),
                        false,
                        topicId.messageThreadId() != 0L,
                        RunnableData { link: Tdlib.MessageLink? ->
                            UI.copyText(
                                link!!.url,
                                if (link.isPublic) R.string.CopiedLink else R.string.CopiedLinkPrivate
                            )
                        })
                }
            }
        } else if (id == R.id.btn_open) {
            if (item.getSourceVideo() != null) {
                val video = item.getSourceVideo()
                U.openFile(this, video)
            } else if (item.getSourceDocument() != null) {
                val document = item.getSourceDocument()
                U.openFile(this, document.fileName, File(document.document.local.path), document.mimeType, 0)
            }
        } else if (id == R.id.btn_replace) {
            if (mediaPickerManager == null) {
                mediaPickerManager = MediaToReplacePickerManager(this)
            }

            stopFullScreenTemporarily(true)
            mediaPickerManager!!.openMediaView(
                RunnableData { file: LocalPickedFile? ->
                    Media.instance().post(Runnable {
                        if (!file!!.imageGalleryFile.hasCaption()) {
                            file.imageGalleryFile.setCaption(item.getCaption())
                        }
                        val content = TD.toContent(
                            tdlib,
                            file.imageGalleryFile,
                            false,
                            false,
                            item.showCaptionAboveMedia(),
                            item.hasSpoiler(),
                            isSecret(item.getSourceChatId())
                        )
                        UI.post(Runnable {
                            tdlib!!.editMessageMedia(item.getSourceChatId(), item.getSourceMessageId(), content, LocalPickedFile(file.imageGalleryFile, null))
                            forceClose()

                            val c = context.navigation().getCurrentStackItem()
                            if (c is MessagesController) {
                                c.highlightMessage(MessageId(item.getSourceChatId(), item.getSourceMessageId()))
                            }
                        })
                    })
                },
                item.getSourceChatId(),
                Runnable { stopFullScreenTemporarily(false) },
                true,
                null,
                (1 shl RightId.SEND_PHOTOS) or (1 shl RightId.SEND_VIDEOS),
                false
            )
        } else if (id == R.id.btn_share) {
            val c: ShareController?
            if (item.getMessage() != null) {
                c = ShareController(context, tdlib)
                if (item.getMessage().content.isText()) {
                    val linkPreview = (item.getMessage().content as MessageText).linkPreview
                    c.setArguments(ShareController.Args(item, linkPreview!!.displayUrl, linkPreview.displayUrl))
                } else {
                    c.setArguments(ShareController.Args(item.getMessage()))
                }
            } else if (item.getShareFile() != null) {
                c = ShareController(context, tdlib)
                var caption: CharSequence? = null
                var exportCaption: CharSequence? = null
                when (mode) {
                    MODE_PROFILE -> {
                        val userId = stack!!.getCurrent().getSourceSender().getSenderUserId()
                        val userName = tdlib!!.cache().userName(userId)
                        if (!isEmpty(userName)) {
                            exportCaption = Lang.getString(R.string.ShareTextProfile, userName)
                        }
                        val username = tdlib!!.cache().userUsername(userId)
                        if (!isEmpty(username)) {
                            exportCaption = Lang.getString(R.string.format_ShareTextSignature, exportCaption, tdlib!!.tMeUrl(username))
                        }
                    }

                    MODE_CHAT_PROFILE -> {
                        val chatId = stack!!.getCurrent().getSourceChatId()
                        val chatTitle = tdlib!!.chatTitle(chatId)
                        if (!isEmpty(chatTitle)) {
                            if (tdlib!!.isChannel(chatId)) {
                                exportCaption = Lang.getString(R.string.ShareTextChannel, chatTitle)
                            } else {
                                exportCaption = Lang.getString(R.string.ShareTextChat, chatTitle)
                            }
                            val username = tdlib!!.chatUsername(chatId)
                            if (!isEmpty(username)) {
                                exportCaption = Lang.getString(R.string.format_ShareTextSignature, exportCaption, tdlib!!.tMeUrl(username))
                            }
                        }
                    }

                    MODE_SIMPLE -> {
                        exportCaption = if (item.getCaption().isEmpty()) null else TD.toCharSequence(item.getCaption())
                        caption = exportCaption
                    }
                }
                c.setArguments(ShareController.Args(item, caption, exportCaption))
            } else {
                return
            }

            c.show()

            forceAnimationType = ANIMATION_TYPE_FADE
            close()
        } else if (id == R.id.btn_showInChat) {
            forceAnimationType = ANIMATION_TYPE_FADE

            val c = context.navigation().getCurrentStackItem()
            if (c is MessagesController && c.compareChat(item.getSourceChatId(), topicId)) {
                c.highlightMessage(MessageId(item.getSourceChatId(), item.getSourceMessageId()))
            } else {
                tdlib!!.ui().openMessage(this, item.getSourceChatId(), MessageId(item.getSourceChatId(), item.getSourceMessageId()), null)
            }

            close()
        } else if (id == R.id.btn_setProfilePhoto) {
            val photoId = item.getPhotoId()
            tdlib!!.send<TdApi.Ok?>(SetProfilePhoto(InputChatPhotoPrevious(photoId), false), tdlib!!.typedOkHandler())
            close()
        } else if (id == R.id.btn_deleteProfilePhoto) {
            if (mode == MODE_PROFILE) {
                tdlib!!.send<TdApi.Ok?>(DeleteProfilePhoto(item.getPhotoId()), tdlib!!.typedOkHandler())
            } else if (mode == MODE_CHAT_PROFILE) {
                tdlib!!.send<TdApi.Ok?>(SetChatPhoto(item.getSourceChatId(), null), tdlib!!.typedOkHandler())
            }
            forceAnimationType = ANIMATION_TYPE_FADE
            close()
        }
    }

    private var mediaPickerManager: MediaToReplacePickerManager? = null

    override fun getCustomHeaderCell(): View? {
        return headerCell
    }

    private fun measureButtonsPadding(): Int {
        var width = Screen.dp(49f)
        if (canShare()) {
            width += Screen.dp(49f)
        }
        if (canEdit()) {
            width += Screen.dp(49f)
        }
        if (canGoPip()) {
            width += Screen.dp(49f)
        }
        if (canViewMasks()) {
            width += Screen.dp(49f)
        }
        return width
    }

    private fun updateHeaderButtons() {
        if (headerView != null) {
            headerView!!.updateButton(R.id.menu_photo, R.id.menu_btn_masks, if (canViewMasks()) View.VISIBLE else View.GONE, 0)
            headerView!!.updateButton(R.id.menu_photo, R.id.menu_btn_pictureInPicture, if (canGoPip()) View.VISIBLE else View.GONE, 0)
            headerView!!.updateButton(R.id.menu_photo, R.id.menu_btn_forward, if (canShare()) View.VISIBLE else View.GONE, 0)
            headerView!!.updateButton(R.id.menu_photo, R.id.menu_btn_edit, if (canEdit()) View.VISIBLE else View.GONE, 0)
            updateTitleMargins()
        }
    }

    private fun updateTitleMargins() {
        if (headerCell != null) {
            val rightMargin = measureButtonsPadding()
            val leftMargin = Screen.dp(68f)
            if (Views.setMargins(
                    headerCell!!.getLayoutParams() as FrameLayout.LayoutParams?,
                    if (Lang.rtl()) rightMargin else leftMargin,
                    headerView!!.getEffectiveTopOffset(),
                    if (Lang.rtl()) leftMargin else rightMargin,
                    0
                )
            ) {
                Views.updateLayoutParams(headerCell)
            }
        }
    }

    private fun toggleHeaderVisibility(): Boolean {
        if (headerView != null && needHeader()) {
            val isVisible = headerVisible.toggleValue(true)
            if (isVisible) {
                context().removeHideNavigationView(this)
            } else {
                context().addHideNavigationView(this)
            }
            return true
        }
        return false
    }

    private val headerVisible = BoolAnimator(ANIMATOR_HEADER, this, DECELERATE_INTERPOLATOR, BOTTOM_ANIMATION_DURATION, true)

    private fun updateCaptionAlpha() {
        if (captionWrapView != null) {
            val alpha = captionFactor * headerVisible.getFloatValue() * (1f - pipFactor)
            captionWrapView!!.setAlpha(alpha)
        }
    }

    private fun updateBottomWrapMargin() {
        val newMargin = this.bottomWrapMargin
        Views.setBottomMargin(bottomWrap, newMargin)
    }

    private fun updateBottomSpaceAlpha() {
        if (bottomSpace != null) {
            val alpha: Float
            if (mode == MODE_GALLERY) {
                alpha = if (editWrap != null) editWrap!!.getAlpha() else 0f
            } else {
                val captionWrapAlpha = if (Views.isValid(captionWrapView)) captionWrapView!!.getAlpha() else 0f
                val sliderAlpha = if (Views.isValid(videoSliderView)) videoSliderView!!.getAlpha() * videoSliderView!!.getInnerAlpha() else 0f
                val thumbsAlpha = if (Views.isValid(thumbsRecyclerView)) thumbsRecyclerView!!.getAlpha() else 0f
                alpha = max(captionWrapAlpha, max(thumbsAlpha, sliderAlpha))
            }
            bottomSpace!!.setAlpha(alpha)
        }
    }

    private fun updateSliderAlpha() {
        if (videoSliderView != null) {
            val alpha = headerVisible.getFloatValue() * (1f - pipFactor) * (if (inCaption) 0f else 1f)
            videoSliderView!!.setAlpha(alpha)
        }
    }

    private var headerAlpha = 0f

    private fun setHeaderAlphaImpl(alpha: Float) {
        if (this.headerAlpha != alpha) {
            this.headerAlpha = alpha
            updateThumbsAlpha()
            updateBottomSpaceAlpha()
        }
    }

    private fun updateThumbsAlpha() {
        if (thumbsRecyclerView != null) {
            thumbsRecyclerView!!.setAlpha(headerAlpha * headerVisible.getFloatValue() * (1f - pipFactor))
            updateBottomSpaceAlpha()
        }
    }

    private fun updateHeaderAlpha() {
        val alpha = clamp(headerVisible.getFloatValue() * (1f - pipFactor))
        if (headerView != null) {
            headerView!!.setAlpha(alpha)
        }
        /*if (bottomPaddingView != null) {
      bottomPaddingView.setAlpha(alpha);
    }*/
    }

    private fun setHeaderVisibilityFactor(factor: Float) {
        updateHeaderAlpha()
        updateCaptionAlpha()
        updateSliderAlpha()
        updateThumbsAlpha()
        updateBottomSpaceAlpha()
    }

    private fun onMediaStackChanged(itemCountChanged: Boolean) {
        when (mode) {
            MODE_MESSAGES, MODE_SIMPLE -> {
                val currentIndex = Strings.buildCounter((stack!!.getEstimatedIndex() + 1).toLong())
                val totalIndex = Strings.buildCounter(stack!!.getEstimatedSize().toLong())
                if (getArgumentsStrict().noLoadMore && stack!!.getEstimatedSize() == 1) {
                    val sponsoredMessage = stack!!.getCurrent().getSourceSponsoredMessage()
                    if (sponsoredMessage != null) {
                        headerCell!!.setTitle(if (sponsoredMessage.isRecommended) R.string.MediaRecommendation else R.string.MediaAd)
                    } else if (stack!!.getCurrent().isVideo()) {
                        headerCell!!.setTitle(R.string.Video)
                    } else if (stack!!.getCurrent().isGif()) {
                        headerCell!!.setTitle(R.string.Gif)
                    } else {
                        headerCell!!.setTitle(R.string.Photo)
                    }
                } else {
                    headerCell!!.setTitle(Lang.getString(R.string.XofY, currentIndex, totalIndex))
                }
                headerCell!!.setSubtitle(genSubtitle())
            }

            MODE_PROFILE, MODE_CHAT_PROFILE -> {
                headerCell!!.setSubtitle(genSubtitle())
            }
        }
        if (!itemCountChanged) {
            checkNeedThumbs()
        }
    }

    private val xofY: String
        get() {
            val currentIndex = Strings.buildCounter((stack!!.getEstimatedIndex() + 1).toLong())
            val totalIndex = Strings.buildCounter(stack!!.getEstimatedSize().toLong())
            return Lang.getString(R.string.XofY, currentIndex, totalIndex)
        }

    private fun initHeaderStyle() {
        if (headerView != null) {
            if (revealAnimationType == ANIMATION_TYPE_REVEAL) {
                headerView!!.setTranslationY(-headerView!!.getSize().toFloat())
            } else {
                headerView!!.setAlpha(0f)
            }
            setHeaderAlphaImpl(0f)
        }
    }

    private fun setHeaderAlpha(alpha: Float) {
        setHeaderAlphaImpl(alpha)
        when (revealAnimationType) {
            ANIMATION_TYPE_REVEAL -> {
                if (headerView != null) {
                    headerView!!.setTranslationY(-headerView!!.getSize() * max(1f - alpha, this.forceEditModeVisibility))
                }
            }

            ANIMATION_TYPE_FADE -> {
                if (headerView != null) {
                    headerView!!.setAlpha(min(alpha, this.forceEditModeVisibility))
                }
            }
        }
    }

    private fun getAuthorText(item: MediaItem): String? {
        if (item.getSourceSender() != null) {
            return tdlib!!.senderName(item.getSourceSender())
        } else if (item.getSourceChatId() != 0L) {
            val chat = tdlib!!.chat(item.getSourceChatId())
            if (chat != null) {
                return chat.title
            }
        }
        return null
    }

    private fun genSubtitle(): CharSequence? {
        val customSubtitle = getArgumentsStrict().customSubtitle
        if (!isEmpty(customSubtitle)) {
            return customSubtitle
        }
        val item = stack!!.getCurrent()
        val time = if (item.getSourceDate() != 0) Lang.getMessageTimestamp(item.getSourceDate().toLong(), TimeUnit.SECONDS) else null
        when (mode) {
            MODE_MESSAGES -> {
                val message = item.getMessage()
                if (message != null && message.content.isText()) {
                    val messageText = message.content as MessageText
                    if (messageText.linkPreview != null) {
                        val author = messageText.linkPreview!!.author
                        if (!isEmpty(author)) {
                            return author
                        }
                        return messageText.linkPreview!!.displayUrl
                    }
                }
                val authorText = getAuthorText(item)
                if (authorText != null) {
                    val b = SpannableStringBuilder(authorText)
                    b.setSpan(Lang.newBoldSpan(Text.needFakeBold(authorText)), 0, authorText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    if (time != null) {
                        b.append(' ')
                        b.append(time)
                    }
                    return b
                } else {
                    return if (time != null) time else ""
                }
            }

            MODE_SIMPLE -> {
                return trim(getArgumentsStrict().caption)
            }

            MODE_PROFILE, MODE_CHAT_PROFILE -> {
                if (time != null) {
                    return if (stack!!.getEstimatedSize() != 1) Lang.getString(R.string.format_mediaIndexAndTime, this.xofY, time) else time
                }
                val resId: Int
                if (getArgumentsStrict().avatarPickerMode != AvatarPickerMode.NONE) {
                    resId = getResId(
                        getArgumentsStrict().avatarPickerMode,
                        R.string.ProfilePhoto,
                        R.string.ProfilePhoto,
                        R.string.GroupPhoto,
                        R.string.ChannelPhoto,
                        R.string.BotPhoto
                    )
                } else if (mode == MODE_CHAT_PROFILE) {
                    resId = (if (tdlib!!.isChannel(stack!!.getCurrent().getSourceChatId())) R.string.ChannelPhoto else R.string.GroupPhoto)
                } else {
                    resId = R.string.ProfilePhoto
                }
                val content = Lang.getString(resId)
                if (stack!!.getEstimatedSize() != 1) {
                    return this.xofY
                } else {
                    return content
                }
            }
        }
        return ""
    }

    // Load more stuff
    private var loadedInitialChunk = false
    private var isLoading = false

    private fun processRequestedEndReached() {
        // TODO load newer messages when possible
    }

    private fun searchFilter(): SearchMessagesFilter {
        return (if (this.filter != null) this.filter else if (mode == MediaViewController.Companion.MODE_CHAT_PROFILE) SearchMessagesFilterChatPhoto() else SearchMessagesFilterPhotoAndVideo())!!
    }

    private fun foundChatMessagesHandler(chatId: Long, topicId: MessageTopic?, fromMessageId: Long, loadCount: Int): Client.ResultHandler {
        return Client.ResultHandler { result: TdApi.Object? ->
            when (result!!.getConstructor()) {
                FoundChatMessages.CONSTRUCTOR -> {
                    val foundChatMessages = result as FoundChatMessages
                    runOnUiThreadOptional(Runnable { addItems(chatId, topicId, fromMessageId, loadCount, foundChatMessages) }
                    )
                }

                TdApi.Error.CONSTRUCTOR -> {
                    UI.showError(result)
                }
            }
        }
    }

    private fun loadMoreIfNeeded(edgeReached: Boolean = false, isEnd: Boolean = false) {
        if (isLoading || getArgumentsStrict().noLoadMore) {
            return
        }
        when (mode) {
            MODE_MESSAGES -> {
                val chatId = stack!!.getCurrent().getSourceChatId()
                if (chatId == 0L || getArgumentsStrict().areOnlyScheduled) {
                    return
                }
                if (!loadedInitialChunk || (if (reverseMode) (edgeReached && isEnd) || stack!!.getCurrentIndex() >= stack!!.getCurrentSize() - LOAD_THRESHOLD else (edgeReached && !isEnd) || stack!!.getCurrentIndex() <= LOAD_THRESHOLD)) {
                    isLoading = true
                    val item = if (reverseMode) stack!!.lastAvalable() else stack!!.firstAvailable()
                    val initialFromMessageId = item.getSourceMessageId()
                    val searchFunction = SearchChatMessages(
                        chatId, topicId, null, null,
                        initialFromMessageId, 0,
                        LOAD_COUNT, searchFilter()
                    )
                    tdlib!!.client().send(searchFunction, foundChatMessagesHandler(chatId, topicId, initialFromMessageId, LOAD_COUNT))
                }
            }

            MODE_CHAT_PROFILE -> {
                val chatId = stack!!.getCurrent().getSourceChatId()
                if (!loadedInitialChunk || (edgeReached && isEnd) || stack!!.getCurrentIndex() <= stack!!.getCurrentSize() - LOAD_THRESHOLD) {
                    isLoading = true
                    val item = stack!!.lastAvalable()
                    val initialFromMessageId = item.getSourceMessageId()
                    val searchFunction = SearchChatMessages(
                        chatId, topicId, null, null,
                        initialFromMessageId, 0,
                        LOAD_COUNT_PROFILE, searchFilter()
                    )
                    tdlib!!.client().send(searchFunction, foundChatMessagesHandler(chatId, topicId, initialFromMessageId, LOAD_COUNT_PROFILE))
                }
            }

            MODE_PROFILE -> {
                val userId = stack!!.getCurrent().getSourceSender().getSenderUserId()
                if (!loadedInitialChunk || (edgeReached && isEnd) || stack!!.getCurrentIndex() <= stack!!.getCurrentSize() - LOAD_THRESHOLD) {
                    isLoading = true
                    val searchFunction = GetUserProfilePhotos(
                        userId,
                        if (loadedInitialChunk) stack!!.getCurrentSize() else 0,
                        LOAD_COUNT_PROFILE
                    )
                    tdlib!!.client().send(searchFunction, Client.ResultHandler { result: TdApi.Object? ->
                        when (result!!.getConstructor()) {
                            ChatPhotos.CONSTRUCTOR -> {
                                runOnUiThreadOptional(Runnable { addItems(result as ChatPhotos) }
                                )
                            }

                            TdApi.Error.CONSTRUCTOR -> {
                                UI.showError(result)
                            }
                        }
                    })
                }
            }
        }
    }

    private fun addItems(photos: ChatPhotos) {
        if (photos.photos.size == 0) {
            return
        }

        var skipCount = 0

        if (!loadedInitialChunk) {
            stack!!.setEstimatedSize(0, photos.totalCount - stack!!.getCurrentSize())
            loadedInitialChunk = true
            if (photos.photos.size > 0 && photos.photos[0].id == stack!!.get(0).getPhotoId()) {
                stack!!.get(0).setChatPhoto(photos.photos[0])
                skipCount = 1
            }
        }

        val sourceUserId = stack!!.getCurrent().getSourceSender().getSenderUserId()
        val items = ArrayList<MediaItem>(photos.photos.size)
        for (photo in photos.photos) {
            if (skipCount > 0) {
                skipCount--
                continue
            }
            items.add(MediaItem(context(), tdlib, sourceUserId, 0, photo))
        }

        if (!items.isEmpty()) {
            stack!!.insertItems(items, false)
            addMoreThumbItems(items, false)
        } else {
            headerCell!!.setSubtitle(genSubtitle())
        }
        isLoading = false
    }

    private var subscribedToChatId: Long = 0

    private fun subscribeToChatId(chatId: Long) {
        if (this.subscribedToChatId != chatId) {
            if (this.subscribedToChatId != 0L) {
                tdlib!!.listeners().unsubscribeFromMessageUpdates(this.subscribedToChatId, this)
            }
            this.subscribedToChatId = chatId
            if (chatId != 0L) {
                tdlib!!.listeners().subscribeToMessageUpdates(chatId, this)
            }
        }
    }

    private fun addItems(chatId: Long, topicId: MessageTopic?, fromMessageId: Long, loadCount: Int, messages: FoundChatMessages) {
        var messagesChatId = TD.getChatId(messages.messages)
        if (messagesChatId == 0L) {
            messagesChatId = chatId
        }
        val list: MutableList<TdApi.Message> = ArrayList<TdApi.Message>(messages.messages.size)
        for (message in messages.messages) {
            if (!message.content.isSecret()) {
                list.add(message)
            }
        }
        val addedCount = addItemsImpl(list, messages.totalCount)
        if (messagesChatId != 0L) {
            subscribeToChatId(messagesChatId)
        }
        if (messages.nextFromMessageId == 0L || (addedCount == 0 && messages.nextFromMessageId == fromMessageId)) {
            stack!!.onEndReached(reverseMode)
            getArgumentsStrict().noLoadMore = true
        } else if (addedCount == 0) {
            val retryFunction = SearchChatMessages(
                chatId, topicId, null, null,
                messages.nextFromMessageId, 0,
                loadCount, searchFilter()
            )
            tdlib!!.client().send(retryFunction, foundChatMessagesHandler(chatId, topicId, messages.nextFromMessageId, loadCount))
            return
        }
        isLoading = false
    }

    private fun addItemsImpl(messages: MutableList<TdApi.Message>, totalCount: Int): Int {
        if (messages.isEmpty()) {
            processRequestedEndReached()
            return 0
        }

        var skipCount = 0
        if (!loadedInitialChunk) {
            loadedInitialChunk = true

            when (mode) {
                MODE_MESSAGES -> {
                    if (reverseMode) {
                        stack!!.setEstimatedSize(0, totalCount - stack!!.getCurrentSize())
                    } else {
                        stack!!.setEstimatedSize(totalCount - stack!!.getCurrentSize(), 0)
                    }
                }

                MODE_CHAT_PROFILE, MODE_PROFILE -> {
                    stack!!.setEstimatedSize(0, totalCount - stack!!.getCurrentSize())
                    if (stack!!.firstAvailable().getSourceMessageId() == 0L) {
                        skipCount = 1
                        stack!!.firstAvailable().setSourceMessageId(messages.get(0).chatId, messages.get(0).id)
                        stack!!.firstAvailable().setSourceDate(messages.get(0).date)
                    }
                }
            }
        }

        val items = ArrayList<MediaItem>(messages.size)
        for (msg in messages) {
            if (skipCount != 0) {
                skipCount--
                continue
            }
            if (msg.content.isSecret())  // skip self-destructing images
                continue

            val item = MediaItem.valueOf(context(), tdlib, msg)
            if (item == null) {
                continue
            }

            if (reverseMode) {
                if (mode == MODE_MESSAGES) {
                    items.add(item)
                } else {
                    items.add(0, item)
                }
            } else {
                if (mode == MODE_MESSAGES) {
                    items.add(0, item)
                } else {
                    items.add(item)
                }
            }
        }
        val onTop = mode == MODE_MESSAGES && !reverseMode
        stack!!.insertItems(items, onTop)
        addMoreThumbItems(items, onTop)

        return items.size
    }

    private fun deleteMedia(index: Int, deletedMedia: MediaItem?) {
        mediaView!!.replaceMedia(deletedMedia, null)
        stack!!.deleteItemAt(index)
        if (thumbsAnimator != null && thumbsAnimator!!.getValue()) {
            if (!needThumbPreviews()) {
                checkNeedThumbs()
            } else {
                thumbsAdapter!!.items!!.deleteItem(index, deletedMedia)
                onFactorChanged(mediaView, slideFactor)
            }
        }
    }

    private fun replaceMedia(index: Int, oldMedia: MediaItem?, newMedia: MediaItem?) {
        mediaView!!.replaceMedia(oldMedia, newMedia)
        stack!!.setItemAt(index, newMedia)
        if (thumbsAnimator != null && thumbsAnimator!!.getValue()) {
            thumbsAdapter!!.items!!.replaceItem(index, oldMedia, newMedia)
            onFactorChanged(mediaView, slideFactor)
        }
    }

    override fun onMessageContentChanged(chatId: Long, messageId: Long, newContent: MessageContent?) {
        runOnUiThreadOptional(Runnable {
            val index = stack!!.indexOfMessage(chatId, messageId)
            if (index != -1) {
                val oldItem = stack!!.get(index)
                val message = oldItem.getMessage()
                message.content = newContent
                val newItem = MediaItem.valueOf(context(), tdlib, message)
                if (newItem != null) {
                    replaceMedia(index, oldItem, newItem)
                    if (headerCell != null) {
                        headerCell!!.setSubtitle(genSubtitle())
                    }
                } else if (stack!!.getCurrentIndex() == index) {
                    // if (message.selfDestructType == null || message.selfDestructType.getConstructor() != TdApi.MessageSelfDestructTypeImmediately.CONSTRUCTOR) {
                    forceClose()
                    // }
                } else {
                    deleteMedia(index, oldItem)
                }
            }
        })
    }

    override fun onMessagesDeleted(chatId: Long, messageIds: LongArray?) {
        val deletedIds = messageIds ?: return
        runOnUiThreadOptional(Runnable {
            val items = stack!!.getAll()
            var remainingCount = deletedIds.size
            var index = items.size - 1
            while (index >= 0 && remainingCount > 0) {
                val item = items.get(index)
                if (item.getSourceChatId() == chatId && item.getSourceMessageId() != 0L && indexOf(deletedIds, item.getSourceMessageId()) != -1) {
                    remainingCount--
                    if (stack!!.getCurrentIndex() == index) {
                        forceClose()
                    } else {
                        deleteMedia(index, item)
                    }
                }
                index--
            }
        })
    }

    // Picture-in-picture stuff
    private fun enterPictureInPicture() {
        if (mediaView!!.isStill()) {
            setInPictureInPicture(true)
        }
    }

    override fun hideSoftwareKeyboard() {
        super.hideSoftwareKeyboard()
        if (mode == MODE_GALLERY) {
            closeCaption()
        }
    }

    private fun backFromPictureInPicture() {
        val c = context.navigation().getCurrentStackItem()
        if (c != null) {
            c.hideSoftwareKeyboard()
        }
        if (mediaView!!.isStill()) {
            setInPictureInPicture(false)
        }
    }

    private var pipStartScale = 0f

    private fun closePictureInPicture() {
        pipStartScale = mediaView!!.getScaleX()
        forceAnimationType = ANIMATION_TYPE_PIP_CLOSE
        popupView!!.hideWindow(true)
    }

    private fun setPipDismissFactor(x: Float) {
        val origScale = (.6f + .4f * x)
        val scale = pipStartScale * origScale
        mediaView!!.setScaleX(scale)
        mediaView!!.setScaleY(scale)
        mediaView!!.setAlpha(x)
        pipControlsWrap!!.setScaleX(origScale)
        pipControlsWrap!!.setScaleY(origScale)
        pipControlsWrap!!.setAlpha(x)
    }

    private var inPictureInPicture = false
    private var pipAnimator: FactorAnimator? = null
    private var pipFactor = 0f

    private var pipItem: MediaItem? = null

    private var pipXFactor = 0f
    private var pipYFactor = 0f

    private fun setInPictureInPicture(`in`: Boolean) {
        if (this.inPictureInPicture != `in`) {
            this.inPictureInPicture = `in`

            /*if (in && pipFactor == 0f) {
        pipXFactor = 1f;
        pipYFactor = -1f;
      }*/
            mediaView!!.getBaseCell().getDetector().preparePositionReset()
            pipItem = stack!!.getCurrent()

            if (`in`) {
                // remember in onFactorChangeFinished
                context().pretendYouDontKnowThisWindow(popupView)
                if (!Config.DISABLE_VIEWER_ELEVATION) {
                    mediaView!!.invalidateOutline()
                }
            } else {
                setDifferentStuffVisible(true)
            }

            if (pipAnimator == null) {
                pipAnimator = FactorAnimator(ANIMATOR_PIP, this, DECELERATE_INTERPOLATOR, 198, pipFactor)
            }

            pipAnimator!!.animateTo(if (`in`) 1f else 0f)
        }
    }

    private fun getPipSize(out: IntArray): Boolean {
        val viewWidth = mediaView!!.getMeasuredWidth()
        val viewHeight = mediaView!!.getMeasuredHeight()

        if (viewHeight == 0 || viewWidth == 0 || pipItem == null) {
            return false
        }

        var mediaWidth = pipItem!!.getWidth()
        var mediaHeight = pipItem!!.getHeight()

        val viewRatio = min(viewWidth.toFloat() / mediaWidth.toFloat(), viewHeight.toFloat() / mediaHeight.toFloat())
        mediaWidth = (mediaWidth * viewRatio).toInt()
        mediaHeight = (mediaHeight * viewRatio).toInt()

        out[0] = mediaWidth
        out[1] = mediaHeight

        return true
    }

    private var isPlayingVideo = false

    private fun updatePipState(isPlaying: Boolean) {
        if (pipPlayPauseButton != null) {
            pipPlayPauseButton!!.setIsPlaying(isPlaying, pipFactor > 0f && pipPlayPauseButton!!.getAlpha() > 0f)
        }
    }

    private var pipWidth = 0
    private var pipHeight = 0
    private var pipX = 0f
    private var pipY = 0f

    private fun updatePipLayout(viewWidth: Int, viewHeight: Int, byLayout: Boolean, allowControlsHide: Boolean) {
        /*int viewWidth = mediaView.getMeasuredWidth();
    int viewHeight = mediaView.getMeasuredHeight();*/

        if (viewHeight == 0 || viewWidth == 0 || pipItem == null) {
            return
        }

        var mediaWidth = pipItem!!.getWidth()
        var mediaHeight = pipItem!!.getHeight()

        val viewRatio = min(viewWidth.toFloat() / mediaWidth.toFloat(), viewHeight.toFloat() / mediaHeight.toFloat())
        mediaWidth = (mediaWidth * viewRatio).toInt()
        mediaHeight = (mediaHeight * viewRatio).toInt()

        // mediaView styles
        val pipSize = Screen.dp(220f)
        val pipScale = min(pipSize.toFloat() / mediaWidth.toFloat(), pipSize.toFloat() / mediaHeight.toFloat())

        // scale
        val scale = pipScale + (1f - pipScale) * (1f - pipFactor)
        mediaView!!.setScaleX(scale)
        mediaView!!.setScaleY(scale)

        /*pipControlsWrap.setScaleX(scale);
    pipControlsWrap.setScaleY(scale);*/

        // translate
        val currentWidth = (mediaWidth.toFloat() * pipScale).toInt()
        val currentHeight = (mediaHeight.toFloat() * pipScale).toInt()

        pipWidth = currentWidth
        pipHeight = currentHeight

        val offsetX = Screen.dp(10f) / 2
        val offsetTopY = (HeaderView.getSize(true) + Screen.dp(11f)) / 2
        val offsetBottomY = (Screen.dp(49f) + Screen.dp(11f)) - offsetTopY

        val fullX = viewWidth / 2 - currentWidth / 2 - offsetX
        val fullY = viewHeight / 2 - currentHeight / 2 - offsetTopY

        val pipXFactor = if (this.pipXFactor < -1f) -1f else if (this.pipXFactor > 1f) 1f else this.pipXFactor
        var x = ((offsetX * -pipXFactor) + (fullX * pipXFactor) + pipAddX) * pipFactor
        val y = (((if (pipYFactor < 0f) offsetTopY else offsetBottomY) * -pipYFactor) + (fullY * pipYFactor) + pipAddY) * pipFactor

        if (abs(this.pipXFactor) == 2f) {
            x = if (this.pipXFactor == -2f) x - currentWidth - offsetX + Screen.dp(24f) else x + currentWidth + offsetX - Screen.dp(24f)
        }

        if (allowControlsHide) {
            val bound = (currentWidth / 6).toFloat()
            val leftX: Float = calculatePipPosition(offsetX, fullX, pipFactor, -1f)
            val rightX: Float = calculatePipPosition(offsetX, fullX, pipFactor, 1f)

            val hiddenLeft = x + currentWidth / 2 < leftX + bound
            setPipHidden(pipFactor == 1f && (hiddenLeft || x + currentWidth / 2 > rightX + currentWidth - bound), hiddenLeft)
        }

        pipX = x
        pipY = y

        mediaView!!.setTranslationX(x)
        mediaView!!.setTranslationY(y)

        val addY = ((mediaHeight * scale) - currentHeight) / 2

        pipControlsWrap!!.setTranslationX(viewWidth / 2 + x - currentWidth / 2)
        pipControlsWrap!!.setTranslationY(viewHeight / 2 + y - currentHeight / 2 + addY)

        mediaView!!.invalidateOutline()

        // inline controls check
        pipControlsWrap!!.setAlpha(pipFactor)
        val visibility = if (pipFactor == 0f) View.GONE else View.VISIBLE
        if (pipControlsWrap!!.getVisibility() != visibility) {
            pipControlsWrap!!.setVisibility(visibility)
        }

        val params = pipControlsWrap!!.getLayoutParams() as FrameLayout.LayoutParams
        if (params.width != currentWidth || params.height != currentHeight) {
            params.width = currentWidth
            params.height = currentHeight
            if (!byLayout) {
                pipControlsWrap!!.setLayoutParams(params)
            }
        }
    }

    private fun setPipFactor(factor: Float, fraction: Float) {
        if (this.pipFactor != factor) {
            require(!java.lang.Float.isNaN(factor))

            this.pipFactor = factor

            updatePipLayout(mediaView!!.getMeasuredWidth(), mediaView!!.getMeasuredHeight(), false, false)

            if (!Config.DISABLE_VIEWER_ELEVATION) {
                mediaView!!.setTranslationZ(factor * Screen.dp(1f).toFloat())
            }

            pipItem!!.setComponentsAlpha(1f - factor)

            if (mediaView!!.getBaseCell().getDetector().setPositionFactor(fraction)) {
                mediaView!!.invalidateOutline()
            }
            mediaView!!.setDisableTouch(factor != 0f)

            contentView!!.invalidate()
            updateHeaderAlpha()
            updateCaptionAlpha()
            updateSliderAlpha()
            updateThumbsAlpha()
            updateBottomSpaceAlpha()

            updatePhotoRevealFactor()
        }
    }

    private fun setDifferentStuffVisible(isVisible: Boolean) {
        if (headerView != null) {
            headerView!!.setVisibility(if (isVisible) View.VISIBLE else View.GONE)
        }
        if (bottomWrap != null) {
            bottomWrap!!.setVisibility(if (isVisible) View.VISIBLE else View.GONE)
        }
    }

    private fun applyPipMode() {
        if (pipFactor == 0f) {
            context().letsRememberAboutThisWindow(popupView)
            setFullScreen(true)
            setLowProfile(true)
            pipItem = null
        } else if (pipFactor == 1f) {
            setFullScreen(false)
            setLowProfile(false)
            setDifferentStuffVisible(false)
        }
    }

    // PiP touch stuff
    private var listenCatchPip = false
    private var isPipUp = false

    private fun setPipUp(isUp: Boolean, velocityX: Float, velocityY: Float) {
        if (isPipUp != isUp) {
            this.isPipUp = isUp
            if (isUp) {
                preparePipMovement()
            } else {
                normalizePipPosition(velocityX, velocityY, true)
            }
            animatePipUpFactor(isUp)
        }
    }

    private var pipUpAnimator: FactorAnimator? = null

    private fun animatePipUpFactor(isUp: Boolean) {
        if (pipUpAnimator == null) {
            pipUpAnimator = FactorAnimator(ANIMATOR_PIP_UP, this, DECELERATE_INTERPOLATOR, 180L)
        }
        pipUpAnimator!!.animateTo(if (isUp) 1f else 0f)
    }

    private fun setPipUpFactor(factor: Float) {
        if (!Config.DISABLE_VIEWER_ELEVATION) {
            mediaView!!.setTranslationZ(Screen.dp(1f) + Screen.dp(2f).toFloat() * factor)
        }
    }

    private var pipHidden = false
    private var pipHideAnimator: FactorAnimator? = null
    private fun setPipHidden(isHidden: Boolean, isLeft: Boolean) {
        if (this.pipHidden != isHidden) {
            this.pipHidden = isHidden
            if (pipHideAnimator == null) {
                pipHideAnimator = FactorAnimator(ANIMATOR_PIP_HIDE, this, DECELERATE_INTERPOLATOR, 180L)
            }
            if (isHidden) {
                pipOverlayView!!.setIsLeft(isLeft)
            }
            pipHideAnimator!!.animateTo(if (isHidden) 1f else 0f)
        }
    }

    private var pipHideFactor = 0f

    private fun setPipHideFactor(factor: Float) {
        if (this.pipHideFactor != factor) {
            this.pipHideFactor = factor
            updatePipControlsAlpha()
            pipOverlayView!!.setAlpha(factor)
        }
    }

    private var pipStartX = 0f
    private var pipStartY = 0f

    private fun preparePipMovement() {
        pipStartX = pipAddX
        pipStartY = pipAddY
    }

    private var pipAddX = 0f
    private var pipAddY = 0f

    private var pipPositionAnimator: FactorAnimator? = null
    private var fromPipAddX = 0f
    private var fromPipAddY = 0f
    private var toPipAddX = 0f
    private var toPipAddY = 0f
    private var toPipXFactor = 0f
    private var toPipYFactor = 0f

    private fun dropPip(velocityX: Float, velocityY: Float) {
        setPipUp(false, velocityX, velocityY)
    }

    private fun normalizePipPosition(velocityX: Float, velocityY: Float, allowHide: Boolean) {
        val offsetX = Screen.dp(10f) / 2
        val offsetTopY = (HeaderView.getSize(true) + Screen.dp(11f)) / 2
        val offsetBottomY = (Screen.dp(49f) + Screen.dp(11f)) - offsetTopY

        val viewWidth = mediaView!!.getMeasuredWidth()
        val viewHeight = mediaView!!.getMeasuredHeight()

        val fullX = viewWidth / 2 - pipWidth / 2 - offsetX
        val fullY = viewHeight / 2 - pipHeight / 2 - offsetTopY

        val leftX: Float = calculatePipPosition(offsetX, fullX, pipFactor, -1f)
        val rightX: Float = calculatePipPosition(offsetX, fullX, pipFactor, 1f)

        val topY: Float = calculatePipPosition(offsetTopY, fullY, pipFactor, -1f)
        val bottomY: Float = calculatePipPosition(offsetBottomY, fullY, pipFactor, 1f)

        val currentX = pipX + pipWidth / 2
        val currentY = pipY + pipHeight / 2

        val targetX: Float
        val targetY: Float
        val targetXFactor: Float
        val targetYFactor: Float
        val bound = (pipWidth / 6).toFloat()

        val shouldBeHidden = allowHide && (currentX < leftX + bound || currentX > rightX + pipWidth - bound)
        val hideLeft = shouldBeHidden && currentX < leftX + bound
        val hiddenLeftX = leftX - pipWidth - offsetX + Screen.dp(24f)
        val hiddenRightX = rightX + pipWidth + offsetX - Screen.dp(24f)

        val degrees = if (velocityX != 0f || velocityY != 0f) Math.toDegrees(atan2(velocityY.toDouble(), velocityX.toDouble())) else 0.0
        val absDegrees = abs(degrees)
        var allowVelocityY = true

        if (abs(velocityX) > Screen.getTouchSlopBig() && (absDegrees < 65 || absDegrees > 115)) {
            if (absDegrees > 115 && pipX <= leftX) {
                targetX = hiddenLeftX
                targetXFactor = -2f
                allowVelocityY = false
            } else if (absDegrees < 65 && pipX >= rightX) {
                targetX = hiddenRightX
                targetXFactor = 2f
                allowVelocityY = false
            } else if (shouldBeHidden && absDegrees > 115 && !hideLeft) {
                targetX = rightX
                targetXFactor = 1f
                allowVelocityY = false
            } else if (shouldBeHidden && absDegrees < 65 && hideLeft) {
                targetX = leftX
                targetXFactor = -1f
                allowVelocityY = false
            } else {
                targetX = if (absDegrees > 115) leftX else rightX
                targetXFactor = if (targetX == leftX) -1f else 1f
            }
        } else {
            targetX =
                if (shouldBeHidden) (if (hideLeft) hiddenLeftX else hiddenRightX) else if (abs(leftX + pipWidth / 2 - currentX) < abs(rightX + pipWidth / 2 - currentX)) leftX else rightX
            targetXFactor = if (shouldBeHidden) (if (hideLeft) -2f else 2f) else if (targetX == leftX) -1f else 1f
        }

        setPipHidden(abs(targetXFactor) == 2f, targetXFactor == -2f)

        if (allowVelocityY && abs(velocityY) > Screen.dp(80f) && (absDegrees >= 45 && absDegrees <= 135)) {
            targetY = if (degrees > 0) bottomY else topY
        } else {
            targetY = if (abs(topY + pipHeight / 2 - currentY) < abs(bottomY + pipHeight / 2 - currentY)) topY else bottomY
        }

        targetYFactor = if (targetY == topY) -1f else 1f

        if (pipPositionAnimator == null) {
            pipPositionAnimator = FactorAnimator(ANIMATOR_PIP_POSITION, this, DECELERATE_INTERPOLATOR, 180)
        } else {
            pipPositionAnimator!!.forceFactor(0f)
        }

        fromPipAddX = pipAddX
        fromPipAddY = pipAddY

        toPipAddX = targetX - (pipX - pipAddX)
        toPipAddY = targetY - (pipY - pipAddY)

        toPipXFactor = targetXFactor
        toPipYFactor = targetYFactor

        pipPositionAnimator!!.animateTo(1f)
    }

    private fun applyPipPosition() {
        pipXFactor = toPipXFactor
        pipYFactor = toPipYFactor
        pipAddY = 0f
        pipAddX = pipAddY
        savePipFactors()
    }

    private fun restorePipFactors() {
        val position = Settings.instance().pipPosition
        pipXFactor = splitLongToFirstInt(position).toFloat()
        pipYFactor = splitLongToSecondInt(position).toFloat()
    }

    private fun savePipFactors() {
        Settings.instance().setPipPosition(pipXFactor, pipYFactor)
    }

    private fun setPipPositionFactor(factor: Float) {
        pipAddX = fromPipAddX + (toPipAddX - fromPipAddX) * factor
        pipAddY = fromPipAddY + (toPipAddY - fromPipAddY) * factor
        updatePipLayout(mediaView!!.getMeasuredWidth(), mediaView!!.getMeasuredHeight(), false, false)
    }

    private fun movePip(dx: Float, dy: Float, viewWidth: Int, viewHeight: Int) {
        pipAddX = pipStartX + dx
        pipAddY = pipStartY + dy
        updatePipLayout(viewWidth, viewHeight, false, true)
    }

    private var pipControlsVisible = true
    private var pipControlsFactor = 1f

    private fun togglePipControlsVisibility() {
        if (abs(pipXFactor) == 2f) {
            normalizePipPosition(0f, 0f, false)
        } else {
            setPipControlsVisible(!pipControlsVisible, true)
        }
    }

    private fun setPipControlsVisible(visible: Boolean, animated: Boolean) {
        if (this.pipControlsVisible != visible) {
            this.pipControlsVisible = visible
            if (animated) {
                animatePipControlsFactor(if (visible) 1f else 0f)
            } else {
                forcePipControlsFactor(if (visible) 1f else 0f)
            }
        }
    }

    private var pipControlsAnimator: FactorAnimator? = null

    private fun animatePipControlsFactor(toFactor: Float) {
        if (pipControlsAnimator == null) {
            pipControlsAnimator = FactorAnimator(ANIMATOR_PIP_CONTROLS, this, DECELERATE_INTERPOLATOR, 180L, pipControlsFactor)
        }
        pipControlsAnimator!!.animateTo(toFactor)
    }

    private fun forcePipControlsFactor(toFactor: Float) {
        if (pipControlsAnimator != null) {
            pipControlsAnimator!!.forceFactor(toFactor)
        }
        setPipControlsFactor(toFactor)
    }

    private fun setPipControlsFactor(factor: Float) {
        if (this.pipControlsFactor != factor) {
            this.pipControlsFactor = factor
            updatePipControlsAlpha()
        }
    }

    private fun updatePipControlsAlpha() {
        val alpha = pipControlsFactor * (1f - pipHideFactor)
        pipOpenButton!!.setAlpha(alpha)
        pipCloseButton!!.setAlpha(alpha)
        pipPlayPauseButton!!.setAlpha(alpha)
        pipBackgroundView!!.setAlpha(alpha * .7f)

        pipOpenButton!!.setEnabled(alpha == 1f)
        pipCloseButton!!.setEnabled(alpha == 1f)
        pipPlayPauseButton!!.setEnabled(alpha == 1f)
    }

    // View-related stuff
    private class SecretTimerView(context: Context?) : View(context), HotListener {
        private var secretPhoto: TGMessageMedia? = null
        private var text: String? = null
        private var textWidth = 0

        private val timerPaint: Paint

        init {
            timerPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
            timerPaint.setTypeface(Fonts.getRobotoMedium())
            timerPaint.setTextSize(Screen.dp(19f).toFloat())
            timerPaint.setColor(-0x1)
        }

        fun setSecretPhoto(photo: TGMessageMedia) {
            if (!photo.isOutgoing) {
                secretPhoto = photo
                setText(photo.hotTimerText)
                photo.setHotListener(this)
            }
        }

        override fun onHotInvalidate(secondsChanged: Boolean) {
            if (secretPhoto != null) {
                if (secondsChanged) {
                    setText(secretPhoto!!.hotTimerText)
                }
                invalidate()
            }
        }

        fun setText(text: String?) {
            if (this.text == null || this.text != text) {
                this.text = text
                this.textWidth = U.measureText(text, timerPaint).toInt()
            }
        }

        fun destroy() {
            if (secretPhoto != null) {
                secretPhoto!!.setHotListener(null)
            }
        }

        override fun onDraw(c: Canvas) {
            if (secretPhoto == null) {
                return
            }
            val offset = Screen.dp(18f)
            val radius = Screen.dp(10f)
            val centerX = offset + radius
            val centerY = offset + radius

            val rectF = Paints.getRectF()

            val offset2 = Screen.dp(16f)
            val padding = Screen.dp(4f)

            /*rectF.set(offset - padding, offset - padding, offset + radius + radius + offset2 + textWidth + padding, offset + radius + radius + padding);
      int radius2 = (int) (rectF.height() / 2);
      c.drawRoundRect(rectF, radius2, radius2, Paints.fillingPaint(0x4c000000));*/
            rectF.set((centerX - radius).toFloat(), (centerY - radius).toFloat(), (centerX + radius).toFloat(), (centerY + radius).toFloat())
            c.drawArc(rectF, -90f, -360f * secretPhoto!!.hotExpiresFactor, true, Paints.fillingPaint(-0x1))
            c.drawText(text!!, (offset + radius + radius + offset2).toFloat(), Screen.dp(35.5f).toFloat(), timerPaint)
        }
    }

    private class PipOverlayView(context: Context?) : View(context) {
        private val backIcon: Drawable?

        private var isLeft = false

        init {
            backIcon = Drawables.get(getResources(), R.drawable.baseline_keyboard_arrow_left_24)
        }

        fun setIsLeft(isLeft: Boolean) {
            if (this.isLeft != isLeft) {
                this.isLeft = isLeft
                invalidate()
            }
        }

        override fun onDraw(c: Canvas) {
            val y = getMeasuredHeight() / 2 - backIcon!!.getMinimumHeight() / 2
            if (isLeft) {
                c.save()
                c.rotate(180f, (getMeasuredWidth() / 2).toFloat(), (getMeasuredHeight() / 2).toFloat())
                Drawables.draw(c, backIcon, 0f, y.toFloat(), Paints.whitePorterDuffPaint())
                c.restore()
            } else {
                Drawables.draw(c, backIcon, 0f, y.toFloat(), Paints.whitePorterDuffPaint())
            }
        }
    }

    private var contentView: FrameLayoutFix? = null
    private var mediaView: MediaView? = null
    private var bottomSpace: FillingSpace? = null
    private var pipControlsWrap: FrameLayoutFix? = null
    private var pipOpenButton: EditButton? = null
    private var pipCloseButton: EditButton? = null
    private var pipBackgroundView: View? = null
    private var pipPlayPauseButton: PlayPauseButton? = null
    private var pipOverlayView: PipOverlayView? = null

    private var backButton: EditButton? = null
    private var sendButton: EditButton? = null
    private var editWrap: FrameLayoutFix? = null
    private var editButtons: LinearLayout? = null
    private var cropOrStickerButton: EditButton? = null
    private var paintOrMuteButton: EditButton? = null
    private var mirrorButton: EditButton? = null
    private var adjustOrTextButton: EditButton? = null
    private var stopwatchButton: StopwatchHeaderButton? = null

    private var bottomWrap: FrameLayoutFix? = null
    private var captionWrapView: ViewGroup? = null
    private var captionView: View? = null
    private var inputView: InputView? = null
    private var captionEmojiButton: ImageView? = null
    private var captionDoneButton: ImageView? = null
    private var videoSliderView: VideoControlView? = null

    private var secretView: SecretTimerView? = null

    private var checkView: CheckView? = null
    private var counterView: CounterView? = null
    private var overlayView: View? = null

    private var receiverView: LinearLayout? = null

    private var headerCell: DoubleHeaderView? = null

    private var filtersView: RecyclerView? = null
    private var filtersAdapter: MediaFiltersAdapter? = null
    private var qualityControlWrap: FrameLayoutFix? = null
    private var qualitySlider: SliderView? = null
    private var qualityInfo: TextView? = null

    private var cropControlsWrap: FrameLayoutFix? = null
    private var proportionButton: EditButton? = null
    private var rotateButton: EditButton? = null
    private var rotationControlView: RotationControlView? = null

    private var paintControlsWrap: FrameLayoutFix? = null
    private var colorPickerView: ColorPickerView? = null
    private var colorToneView: ColorToneView? = null
    private var colorToneShadow: ShadowView? = null
    private var undoButton: ImageView? = null
    private var paintTypeButton: EditButton? = null
    private var colorPreviewView: ColorPreviewView? = null

    private var counterFactor = 0f

    override fun performOnBackPressed(fromTop: Boolean, commit: Boolean): Boolean {
        if (inSlideMode || (slideAnimator != null && slideAnimator!!.isAnimating())) {
            return true
        }
        if (showOtherMedias) {
            if (commit) {
                setShowOtherMedias(false)
            }
            return true
        }
        if (currentSection != SECTION_CAPTION) {
            if (commit) {
                goBackToCaption(true)
            }
            return true
        }
        if (this.isEmojiVisible) {
            if (commit) {
                forceCloseEmojiKeyboard()
            }
            return true
        }
        if (mediaView!!.isZoomed()) {
            if (commit) {
                mediaView!!.normalizeZoom()
            }
            return true
        }
        if (inForceEditMode()) {
            if (commit) {
                closeForceEditMode()
            }
            return true
        }
        return super.performOnBackPressed(fromTop, commit)
    }

    private fun setCounterFactor(counterFactor: Float) {
        if (this.counterFactor != counterFactor) {
            this.counterFactor = counterFactor
            setCounterAlpha(this.allowedCounterAlpha)
            val scale: Float = COUNTER_SCALE + (1f - COUNTER_SCALE) * counterFactor
            counterView!!.setScaleX(scale)
            counterView!!.setScaleY(scale)
        }
    }

    private var counterAnimator: FactorAnimator? = null

    private fun animateCounterFactor(factor: Float) {
        if (this.counterAnimator == null) {
            counterAnimator = FactorAnimator(ANIMATOR_COUNTER, this, OvershootInterpolator(3.8f), 260L, counterFactor)
        }
        counterAnimator!!.animateTo(factor)
    }

    private fun forceCounterFactor(factor: Float) {
        if (counterAnimator != null) {
            counterAnimator!!.forceFactor(factor)
        }
        this.counterFactor = -1f
        setCounterFactor(factor)
    }

    private var videoVisible = false

    private fun needTrim(): Boolean {
        return mode == MODE_GALLERY
    }

    private fun setVideoVisible(isVisible: Boolean, isLoaded: Boolean, duration: Long, unit: TimeUnit, animated: Boolean) {
        if (videoSliderView != null) {
            val cellView = if (mediaView != null) mediaView!!.findCellForItem(stack!!.getCurrent()) else null
            if (this.videoVisible != isVisible) {
                this.videoVisible = isVisible
                if (isVisible) {
                    val timeNow: Long
                    val timeTotal: Long
                    if (isLoaded && cellView != null) {
                        timeNow = cellView.getTimeNow()
                        timeTotal = cellView.getTimeTotal()
                    } else {
                        timeTotal = -1
                        timeNow = timeTotal
                    }
                    if ((timeNow == -1L) or (timeTotal == -1L)) {
                        videoSliderView!!.resetDuration(unit.toMillis(duration), 0, isLoaded, animated && videoFactor != 0f)
                    } else {
                        videoSliderView!!.resetDuration(timeTotal, timeNow, isLoaded, animated && videoFactor != 0f)
                    }
                    videoSliderView!!.setFile(stack!!.getCurrent().getSourceGalleryFile())
                    val item = stack!!.getCurrent()
                    videoSliderView!!.setShowPlayPause(item.isVideoOrGif(), animated && videoFactor != 0f)
                    videoSliderView!!.setIsPlaying(false, true)
                    videoSliderView!!.setSlideEnabled(item.canSeekVideo())
                    if (item.isVideoOrGif() && commonFactor < 1f) {
                        videoSliderView!!.setIsPlaying(true, animated && videoFactor != 0f)
                        updatePipState(true)
                    }
                }
                if (animated) {
                    animateVideoFactor(if (isVisible) 1f else 0f)
                } else {
                    forceVideoFactor(if (isVisible) 1f else 0f)
                }
            } else if (isVisible) {
                val timeNow: Long
                val timeTotal: Long
                if (isLoaded && cellView != null) {
                    timeNow = cellView.getTimeNow()
                    timeTotal = cellView.getTimeTotal()
                } else {
                    timeTotal = -1
                    timeNow = timeTotal
                }
                if (timeNow == -1L || timeTotal == -1L) {
                    videoSliderView!!.resetDuration(unit.toMillis(duration), 0, isLoaded, animated)
                } else {
                    videoSliderView!!.resetDuration(timeTotal, timeNow, isLoaded, animated)
                }
                val item = stack!!.getCurrent()
                videoSliderView!!.updateSecondarySeek(TD.getFileOffsetProgress(item.getTargetFile()), TD.getFilePrefixProgress(item.getTargetFile()))
                videoSliderView!!.setFile(item.getSourceGalleryFile())
                videoSliderView!!.setShowPlayPause(item.isVideoOrGif(), animated)
                videoSliderView!!.setIsPlaying(false, true)
                videoSliderView!!.setSlideEnabled(item.canSeekVideo())
                if (item.isVideoOrGif() && (commonFactor < 1f || item.isAutoplay())) {
                    videoSliderView!!.setIsPlaying(true, animated)
                    updatePipState(true)
                }
            }
        }
    }

    override fun onPlayStarted(item: MediaItem?, isPlaying: Boolean) {
        if (videoSliderView != null && stack!!.getCurrent() === item) {
            videoSliderView!!.setIsPlaying(isPlaying, true)
            updatePipState(isPlaying)
            videoSliderView!!.setShowPlayPause(true, true)
        }
    }

    private var videoFactor = 0f
    private var videoAnimator: FactorAnimator? = null
    private var needVideoMargin = false

    private fun updateCaptionLayout() {
        val needMargin = videoFactor == 1f && !inCaption
        if (this.needVideoMargin != needMargin) {
            this.needVideoMargin = needMargin
            val y = (if (needMargin) 0f else -Screen.dp(56f).toFloat() * videoFactor * (if (inCaption) 0f else 1f)) + (thumbsFactor * Screen.dp(THUMBS_PADDING))
            captionWrapView!!.setTranslationY(y)
            Views.setBottomMargin(captionWrapView, (if (needVideoMargin) Screen.dp(56f) else 0))
        }
    }

    private fun checkBottomComponentOffsets() {
        val thumbOffset = thumbsFactor * Screen.dp(THUMBS_PADDING)
        if (captionWrapView != null) {
            updateCaptionLayout()
            if (!needVideoMargin) {
                val y = -Screen.dp(56f).toFloat() * videoFactor + thumbOffset
                captionWrapView!!.setTranslationY(y)
            }
        }
        if (videoSliderView != null) {
            videoSliderView!!.setTranslationY(Screen.dp(56f).toFloat() * (1f - videoFactor) + thumbOffset)
        }
    }

    private fun setVideoFactor(factor: Float) {
        if (this.videoFactor != factor) {
            this.videoFactor = factor
            checkBottomComponentOffsets()
            if (videoSliderView != null) {
                videoSliderView!!.setInnerAlpha(factor)
                updateBottomSpaceAlpha()
            }
        }
    }

    private fun forceVideoFactor(factor: Float) {
        if (videoAnimator != null) {
            videoAnimator!!.forceFactor(factor)
        }
        setVideoFactor(factor)
    }

    override fun onDisplayError(error: PlaybackException, item: MediaItem?): Boolean {
        val isGif = item != null && item.isGifType()
        val info =
            Lang.getString(if (U.isUnsupportedFormat(error)) (if (isGif) R.string.GifPlaybackUnsupported else R.string.VideoPlaybackUnsupported) else (if (isGif) R.string.GifPlaybackError else R.string.VideoPlaybackError))
        showOptions(
            info,
            intArrayOf(R.id.btn_view, R.id.btn_cancel),
            arrayOf<String?>(Lang.getString(R.string.ViewVideoError), Lang.getString(R.string.Cancel)),
            intArrayOf(OptionColor.RED, OptionColor.NORMAL),
            intArrayOf(R.drawable.baseline_bug_report_24, R.drawable.baseline_cancel_24),
            OptionDelegate { itemView: View?, id: Int ->
                if (id == R.id.btn_view) {
                    forceClose()
                    val c = TextController(context, tdlib)

                    val log =
                        Log.toPreviewString(error) +
                                "\n\n" +
                                U.getUsefulMetadata(tdlib) +
                                "\n\n" +
                                Log.toString(error, true)

                    c.setArguments(TextController.Arguments.fromRawText(Lang.getString(R.string.VideoErrorLog), log, "text/plain"))
                    context.navigation().navigateTo(c)
                }
                true
            },
            this@MediaViewController.forcedTheme
        )
        return true
    }

    override fun onCanSeekChanged(item: MediaItem?, canSeek: Boolean) {
        if (stack!!.getCurrent() === item && videoSliderView != null) {
            videoSliderView!!.setSlideEnabled(canSeek)
        }
    }

    override fun onSeekProgress(item: MediaItem?, nowMs: Long, durationMs: Long, progress: Float) {
        if (stack!!.getCurrent() === item && videoSliderView != null) {
            videoSliderView!!.updateSeek(nowMs, durationMs, progress)
        }
    }

    override fun onSeekSecondaryProgress(item: MediaItem?, offset: Float, progress: Float) {
        if (stack!!.getCurrent() === item && videoSliderView != null) {
            videoSliderView!!.updateSecondarySeek(offset, progress)
        }
    }

    override fun onPlayPause(item: MediaItem?, isPlaying: Boolean) {
        if (stack!!.getCurrent() !== item) {
            return
        }
        if (videoSliderView != null) {
            videoSliderView!!.setIsPlaying(isPlaying, videoFactor > 0f)
        }
        isPlayingVideo = isPlaying
        when (mode) {
            MODE_MESSAGES, MODE_SIMPLE -> {
                updatePipState(isPlaying)
                if (!isPlaying) {
                    setPipControlsVisible(true, true)
                }
            }
        }
    }

    fun pauseVideoIfPlaying() {
        if (isPlayingVideo) {
            stack!!.getCurrent().performClick(pipPlayPauseButton)
        }
    }

    private fun animateVideoFactor(toFactor: Float) {
        if (videoAnimator == null) {
            videoAnimator = FactorAnimator(ANIMATOR_VIDEO, this, DECELERATE_INTERPOLATOR, BOTTOM_ANIMATION_DURATION, videoFactor)
        }
        videoAnimator!!.animateTo(toFactor)
    }

    private var popupView: PopupLayout? = null
    private var ignoreCaptionUpdate = false

    private fun canRunFullscreen(): Boolean {
        if (useEdgeToEdge()) {
            return true
        }
        if (Config.CUTOUT_ENABLED && mode == MODE_MESSAGES) {
            return true
        }
        return false // mode != MODE_GALLERY && mode != MODE_MESSAGES; // mode == MODE_PROFILE || mode == MODE_CHAT_PROFILE;
    }

    override fun onEmojiUpdated(isPackSwitch: Boolean) {
        if (captionView != null) {
            captionView!!.invalidate()
        }
    }

    private fun needHeader(): Boolean {
        return mode == MODE_MESSAGES || mode == MODE_SIMPLE || mode == MODE_CHAT_PROFILE || mode == MODE_PROFILE
    }

    private var flingDetector: FlingDetector? = null

    override fun onFling(velocityX: Float, velocityY: Float): Boolean {
        if (isPipUp) {
            dropPip(velocityX, velocityY)
            return true
        }
        if (inSlideMode) {
            dropSlideMode(velocityX, velocityY, max(abs(velocityX), abs(velocityY)) > Screen.dp(50f))
        }
        return false
    }

    private fun canCloseBySlide(): Boolean {
        return mode != MODE_SECRET && (mode != MODE_GALLERY || currentSection == SECTION_CAPTION) && !mediaView!!.isZoomed() && !inCaption
    }

    private var listenCloseBySlide = false
    private var inSlideMode = false

    private var slideItem: MediaItem? = null

    private fun setInSlideMode(x: Float, y: Float) {
        slideItem = stack!!.getCurrent()
        inSlideMode = true
    }

    private fun measureBottomWrapHeight(): Int {
        var height = (Screen.dp(56f).toFloat() * videoFactor).toInt()
        if (captionFactor == 1f && captionWrapView != null) {
            height += captionWrapView!!.getMeasuredHeight()
        }
        if (useEdgeToEdge()) {
            height += bottomInnerMargin
        }
        return height
    }

    private var lastSlideX = 0f
    private var lastSlideY = 0f
    private var lastSlideSourceX = 0f
    private var dismissFactor = 0f

    private fun setSlideDismissFactor(factor: Float) {
        if (this.dismissFactor != factor) {
            this.dismissFactor = factor
            setCommonFactor(1f - dismissFactor)

            if (headerView != null) {
                headerView!!.setTranslationY(-headerView!!.getSize() * max(dismissFactor, this.forceEditModeVisibility))
            }

            if (mode == MODE_GALLERY) {
                setEditComponentsAlpha(1f - dismissFactor)
            } else {
                checkBottomWrapY()
            }
        }
    }

    private fun checkBottomWrapY() {
        val thumbsDistance = (Screen.dp(THUMBS_PADDING) * 2 + Screen.dp(THUMBS_HEIGHT)) * (if (inForceEditMode()) 0 else 1)
        val offsetDistance = measureBottomWrapHeight().toFloat() * dismissFactor - this.keyboardOffset
        var maxY = 0f
        if (bottomWrap != null) {
            val y = offsetDistance - (thumbsFactor * thumbsDistance.toFloat()) * (1f - dismissFactor)
            bottomWrap!!.setTranslationY(y)
            maxY = max(0f, y)
        }
        if (thumbsRecyclerView != null) {
            val dy = (thumbsDistance.toFloat() * max((1f - thumbsFactor), dismissFactor))
            val y = offsetDistance + dy
            thumbsRecyclerView!!.setTranslationY(y)
            maxY = max(0f, y)
        }
        if (bottomSpace != null) {
            bottomSpace!!.setTranslationY(maxY)
        }
    }

    private fun setSlide(x: Float, y: Float, sourceX: Float, noRotation: Boolean, byTouch: Boolean) {
        lastSlideX = x
        lastSlideY = y
        lastSlideSourceX = sourceX

        var dismissFactor = abs(min(1f, y / Screen.dp(125f).toFloat()))
        if (java.lang.Float.isNaN(dismissFactor))  // TODO: find out why it could become NaN
            dismissFactor = 0f
        if (noRotation || dismissFactor > this.dismissFactor || byTouch) {
            setSlideDismissFactor(dismissFactor)
        }

        mediaView!!.setTranslationX(x)
        mediaView!!.setTranslationY(y)

        if (noRotation) {
            return
        }

        if (sourceX == 0f || y == 0f) {
            mediaView!!.setRotation(0f)
        } else {
            mediaView!!.setRotation(calculateRotationForXY(x, y, sourceX))
        }
    }

    private fun calculateRotationForXY(x: Float, y: Float, sourceX: Float): Float {
        var sourceX = sourceX
        val viewWidth = mediaView!!.getMeasuredWidth()
        val viewHeight = mediaView!!.getMeasuredHeight()

        var mediaWidth = slideItem!!.getWidth()
        var mediaHeight = slideItem!!.getHeight()

        val viewRatio = min(viewWidth.toFloat() / mediaWidth.toFloat(), viewHeight.toFloat() / mediaHeight.toFloat())
        mediaWidth = (mediaWidth * viewRatio).toInt()
        mediaHeight = (mediaHeight * viewRatio).toInt()

        sourceX -= (viewWidth / 2).toFloat()
        sourceX *= -1f

        val rotation = -35f * (y / (viewHeight * .5f)) * (min(1f, max(-1f, x / (mediaWidth * .2f))) * sign(sourceX))

        return (sourceX / (viewWidth.toFloat() * .5f)) * rotation
    }

    private var fromSlideX = 0f
    private var fromSlideY = 0f
    private var fromSlideRotation = 0f
    private var fromSlideSourceX = 0f
    private var toSlideX = 0f
    private var toSlideY = 0f
    private var toSlideRotation = 0f
    private var slideAnimator: FactorAnimator? = null
    private fun dropSlideMode(velocityX: Float, velocityY: Float, apply: Boolean) {
        inSlideMode = false

        fromSlideX = lastSlideX
        fromSlideY = lastSlideY
        fromSlideRotation = mediaView!!.getRotation()
        fromSlideSourceX = lastSlideSourceX

        if (fromSlideX == 0f && fromSlideY == 0f) {
            return
        }

        if (slideAnimator == null) {
            slideAnimator = FactorAnimator(ANIMATOR_SLIDE, this, DECELERATE_INTERPOLATOR, 280L)
        } else {
            slideAnimator!!.forceFactor(0f)
        }

        if (apply) {
            val direction = atan2(velocityY.toDouble(), velocityX.toDouble())

            val viewWidth = mediaView!!.getMeasuredWidth().toFloat()
            val viewHeight = mediaView!!.getMeasuredHeight().toFloat()

            var mediaWidth = slideItem!!.getWidth()
            var mediaHeight = slideItem!!.getHeight()

            val viewRatio = min(viewWidth / mediaWidth.toFloat(), viewHeight / mediaHeight.toFloat())
            mediaWidth = (mediaWidth * viewRatio).toInt()
            mediaHeight = (mediaHeight * viewRatio).toInt()

            val cos = cos(direction)
            val sin = sin(direction)

            toSlideX = (viewWidth * cos).toFloat()
            toSlideY = (viewHeight * sin).toFloat()
            toSlideRotation = fromSlideRotation * 1.5f // calculateRotationForXY(toSlideX, toSlideY, fromSlideSourceX);

            val neededWidth = (abs(viewWidth * sin(toSlideRotation.toDouble())) + abs(viewHeight * cos(toSlideRotation.toDouble()))).toFloat()
            val neededHeight = (abs(viewWidth * cos(toSlideRotation.toDouble())) + abs(viewHeight * sin(toSlideRotation.toDouble()))).toFloat()

            toSlideX += abs(viewWidth - neededWidth) * sign(toSlideX)
            toSlideY += abs(viewHeight - neededHeight) * sign(toSlideY)
        } else {
            toSlideRotation = 0f
            toSlideY = toSlideRotation
            toSlideX = toSlideY
        }

        slideAnimator!!.animateTo(1f)
    }

    private fun setSlideFactor(factor: Float) {
        val x = fromSlideX + (toSlideX - fromSlideX) * factor
        val y = fromSlideY + (toSlideY - fromSlideY) * factor
        val noRotation = toSlideRotation != -1f
        setSlide(x, y, fromSlideSourceX, noRotation, false)

        if (noRotation) {
            mediaView!!.setRotation(fromSlideRotation + (toSlideRotation - fromSlideRotation) * factor)
        }
    }

    private var animationAlreadyDone = false

    private fun applySlide() {
        forceAnimationType = ANIMATION_TYPE_FADE
        animationAlreadyDone = true
        close()
    }

    // Thumb previews
    private var thumbsFactor = 0f
    private var thumbsAnimator: BoolAnimator? = null
    private var thumbsRecyclerView: ThumbRecyclerView? = null
    private var thumbItemAnimator: CustomItemAnimator? = null
    private var thumbsAdapter: ThumbAdapter? = null
    private var thumbsLayoutManager: LinearLayoutManager? = null

    // PADDING: 10dp
    // HEIGHT: 36dp
    // SPACING: 1dp
    // SPACING_ADD: 5dp
    // __
    // DECORATION_BETWEEN: 1dp
    // DECORATION_SIDE = recyclerWidth / 2 - cellWidth / 2
    private fun setThumbsFactor(factor: Float) {
        if (this.thumbsFactor != factor) {
            this.thumbsFactor = factor
            checkBottomComponentOffsets()
            checkBottomWrapY()
            checkThumbsItemAnimator()
            updateThumbsAlpha()
            updateBottomSpaceAlpha()
        }
    }

    private fun checkNeedThumbs() {
        setNeedThumbs(needThumbPreviews())
    }

    private fun setNeedThumbs(needThumbs: Boolean) {
        val prevNeedThumbs = thumbsAnimator != null && thumbsAnimator!!.getValue()
        if (prevNeedThumbs != needThumbs) {
            if (thumbsAnimator == null) {
                thumbsAnimator = BoolAnimator(ANIMATOR_THUMBS, this, DECELERATE_INTERPOLATOR, BOTTOM_ANIMATION_DURATION)
            }
            if (needThumbs) {
                initThumbsRecyclerView()
                fillThumbItems()
            }
            thumbsAnimator!!.setValue(needThumbs, commonFactor > 0f)
        } else if (needThumbs && mode == MODE_MESSAGES && !getArgumentsStrict().forceThumbs && thumbsAdapter!!.items!!.mediaGroupId != stack!!.getCurrent()
                .getMessage().mediaAlbumId
        ) {
            fillThumbItems()
        }
    }

    private fun addMoreThumbItems(items: ArrayList<MediaItem>?, onLeftSide: Boolean) {
        if (items == null || items.isEmpty()) {
            return
        }
        if (thumbsAnimator == null || !thumbsAnimator!!.getValue()) {
            checkNeedThumbs()
        } else {
            thumbsAdapter!!.items!!.addItems(items, onLeftSide, mode == MODE_MESSAGES && !getArgumentsStrict().forceThumbs)
        }
    }

    private fun needThumbPreviews(): Boolean {
        if (stack!!.getCurrentSize() <= 1) {
            return false
        }
        if (mode == MODE_PROFILE || mode == MODE_CHAT_PROFILE || getArgumentsStrict().forceThumbs) {
            return true
        }
        if (mode == MODE_MESSAGES) {
            val current = stack!!.getCurrent()
            val prev = stack!!.getPrevious()
            val next = stack!!.getNext()

            val currentMessage = current.getMessage()
            val prevMessage = if (prev != null) prev.getMessage() else null
            val nextMessage = if (next != null) next.getMessage() else null

            return currentMessage != null && currentMessage.mediaAlbumId != 0L && ((prevMessage != null && prevMessage.mediaAlbumId == currentMessage.mediaAlbumId) || (nextMessage != null && nextMessage.mediaAlbumId == currentMessage.mediaAlbumId))
        }
        return false
    }

    private fun fillThumbItems() {
        check(stack!!.getCurrentSize() > 1)
        var focusItemIndex = -1
        val focusItem = stack!!.getCurrent()
        var indexInStack = -1

        var items: ArrayList<MediaItem?>? = null

        if (mode == MODE_PROFILE || mode == MODE_CHAT_PROFILE || getArgumentsStrict().forceThumbs) {
            items = ArrayList<MediaItem?>(stack!!.getCurrentSize())
            items.addAll(stack!!.getAll())
            focusItemIndex = stack!!.getCurrentIndex()
            indexInStack = 0
        } else if (mode == MODE_MESSAGES) {
            items = ArrayList<MediaItem?>(MAX_MESSAGE_GROUP_SIZE)
            val currentItem = stack!!.getCurrent()
            val mediaGroupId = currentItem.getMessage().mediaAlbumId
            check(mediaGroupId != 0L)
            var index = stack!!.getCurrentIndex()
            while (index - 1 >= 0) {
                val item = stack!!.get(index - 1)
                if (item.getMessage().mediaAlbumId != mediaGroupId) {
                    break
                }
                index--
            }
            indexInStack = index
            val stackSize = stack!!.getCurrentSize()
            while (index < stackSize) {
                val item = stack!!.get(index)
                if (item.getMessage().mediaAlbumId != mediaGroupId) {
                    break
                }
                if (item === focusItem) {
                    focusItemIndex = items!!.size
                }
                items!!.add(item)
                index++
            }
        } else {
            throw IllegalStateException()
        }

        require(indexInStack != -1)

        require(!(items == null || items.isEmpty()))

        require(focusItemIndex != -1)

        val thumbItems = ThumbItems(thumbsAdapter!!, items, indexInStack)
        thumbItems.setFocusItem(focusItem, focusItemIndex, 1f)
        thumbsAdapter!!.submitItems(thumbItems)
        ensureThumbsPosition(false, false)
    }

    private fun clearThumbsView() {
        if (thumbsAdapter != null) {
            thumbsAdapter!!.submitItems(null)
            thumbsRecyclerView!!.setItemAnimator(null)
        }
    }

    private fun checkThumbsItemAnimator() {
        if (thumbsRecyclerView != null) {
            val desiredItemAnimator: ItemAnimator? = if (commonFactor == 1f && thumbsFactor > 0f) thumbItemAnimator else null
            if (thumbsRecyclerView!!.getItemAnimator() !== desiredItemAnimator) {
                thumbsRecyclerView!!.setItemAnimator(desiredItemAnimator)
            }
        }
    }

    private class ThumbItemDecoration(private val context: ThumbAdapter) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            val thumbView = ThumbViewHolder.Companion.getThumbView(view)
            val item = thumbView.item
            val items = thumbView.items
            val parentWidth = parent.getMeasuredWidth()
            val itemWidth = context.controller.calculateThumbWidth()
            val distance = parentWidth / 2 - itemWidth / 2
            if (items == null || item == null) {
                val adapterPosition = parent.getChildAdapterPosition(view)
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    val itemCount = parent.getAdapter()!!.getItemCount()
                    if (Lang.rtl()) {
                        outRect.left = if (adapterPosition == itemCount - 1) distance else 0
                        outRect.right = if (adapterPosition == 0) distance else 0
                    } else {
                        outRect.left = if (adapterPosition == 0) distance else 0
                        outRect.right = if (adapterPosition == itemCount - 1) distance else 0
                    }
                }
                return
            }
            if (Lang.rtl()) {
                outRect.left = if (items.last === item) distance else 0
                outRect.right = if (items.first === item) distance else 0
            } else {
                outRect.left = if (items.first === item) distance else 0
                outRect.right = if (items.last === item) distance else 0
            }
        }

        override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            val childCount = parent.getChildCount()
            for (i in 0..<childCount) {
                val view = parent.getChildAt(i)
                if (view == null) {
                    continue
                }
                val thumbView = ThumbViewHolder.Companion.getThumbView(view)
                val thumbItems = thumbView.items
                val thumbItem = thumbView.item
                val expandAllowance = 1f - context.controller.thumbsScrollFactor
                if (thumbItems != null && thumbItem != null) {
                    var x = thumbView.getLeft() + thumbView.getMeasuredWidth() / 2f
                    var position = parent.getChildAdapterPosition(view)
                    if (position == RecyclerView.NO_POSITION) {
                        val tag = view.getTag() as Int?
                        if (tag != null) {
                            position = tag
                        } else {
                            position = thumbItems.indexOf(thumbItem)
                            view.setTag(position)
                        }
                    } else {
                        view.setTag(null)
                    }
                    if (position != RecyclerView.NO_POSITION) {
                        val dx = thumbItems.getTranslationX(thumbItem, position, expandAllowance)
                        if (Lang.rtl()) {
                            x -= dx
                        } else {
                            x += dx
                        }
                    }
                    thumbView.drawImage(c, x.toInt(), Screen.dp(THUMBS_PADDING), view.getAlpha(), expandAllowance)
                }
            }
        }
    }

    private var thumbsScrollAnimator: BoolAnimator? = null
    private var applyingThumbScrollFix = false
    private var thumbScrollFixStartValue = 0f
    private var thumbScrollFixDistance = 0
    private var thumbScrollFixDone = 0

    private fun areThumbsScrolling(): Boolean {
        return thumbsScrollAnimator != null && thumbsScrollAnimator!!.getValue()
    }

    private val isAutoScrolling: Boolean
        get() = autoThumbScroller != null && autoThumbScroller!!.isAnimating()

    private fun setThumbsScrolling(isScrolling: Boolean) {
        var isScrolling = isScrolling
        val prevIsScrolling = thumbsScrollAnimator != null && thumbsScrollAnimator!!.getValue()
        isScrolling = isScrolling && !this.isAutoScrolling
        if (prevIsScrolling != isScrolling) {
            if (thumbsScrollAnimator == null) {
                thumbsScrollAnimator = BoolAnimator(ANIMATOR_THUMBS_SCROLL, this, DECELERATE_INTERPOLATOR, 140L)
            }
            applyingThumbScrollFix = false
            if (!isScrolling) {
                thumbRequestTime = 0
                val startValue = thumbsScrollAnimator!!.getFloatValue()
                ensureThumbsPosition(true, true)
                if (startValue == 0f || desiredThumbScrollByX == 0) {
                    ensureThumbsPosition(true, false)
                } else if (desiredThumbScrollByX != 0) {
                    applyingThumbScrollFix = true
                    thumbScrollFixStartValue = startValue
                    thumbScrollFixDistance = desiredThumbScrollByX
                    thumbScrollFixDone = 0
                }
            } else {
                cancelAutoThumbScroller()
                thumbsRecyclerView!!.cancelClick()
            }
            thumbsScrollAnimator!!.setValue(isScrolling, true)
        }
    }

    private fun dropAutoThumbScroll(): Boolean {
        return false
        /*float factor = autoThumbScroller.getFactor();
    if (factor <= .5f) {
      factor
    }

    if (isAutoScrolling()) {

      controller.cancelAutoThumbScroller();
      controller.setThumbsScrolling(true);
    }*/
    }

    private var thumbRequestTime: Long = 0
    private val thumbStrength: Float
        get() {
            if (thumbRequestTime == 0L) {
                return 1f
            }
            val now = SystemClock.uptimeMillis()
            val ms = now - thumbRequestTime
            if (ms > 100L) {
                return 1f
            }
            return max(0f, ms.toFloat() / 200f)
        }

    private fun detectThumbPosition() {
        ensureThumbsPosition(true, true)
        if (totalThumbScrollX == -1 || thumbsAdapter!!.items == null) {
            return
        }
        val thumbsWidth = calculateThumbWidth()
        val index = Math.round((totalThumbScrollX).toFloat() / thumbsWidth.toFloat())
        if (fastShowMediaItem(thumbsAdapter!!.items!!.get(index), thumbsAdapter!!.items, index, false)) {
            thumbRequestTime = SystemClock.uptimeMillis()
        }
    }

    private var thumbsScrolled = false

    private var thumbsScrollFactor = 0f

    private fun setThumbsScrollFactor(factor: Float) {
        if (this.thumbsScrollFactor != factor) {
            this.thumbsScrollFactor = factor
            if (applyingThumbScrollFix) {
                val fixFactor = (thumbScrollFixStartValue - factor) / thumbScrollFixStartValue
                val distance = (fixFactor * thumbScrollFixDistance.toFloat()).toInt()
                val distanceDiff = distance - thumbScrollFixDone
                if (distanceDiff != 0) {
                    thumbScrollFixDone = distance
                    thumbsRecyclerView!!.scrollBy(distanceDiff, 0)
                }
            }
            thumbsRecyclerView!!.invalidate()
        }
    }

    private fun initThumbsRecyclerView() {
        if (thumbsRecyclerView != null) {
            return
        }

        thumbItemAnimator = CustomItemAnimator(DECELERATE_INTERPOLATOR, 140L)

        thumbsAdapter = ThumbAdapter(context(), this)

        thumbsLayoutManager = LinearLayoutManager(context(), LinearLayoutManager.HORIZONTAL, Lang.rtl())

        thumbsRecyclerView = ThumbRecyclerView(context())
        thumbsRecyclerView!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            private var thumbsScrolling = false

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                val isScrolling = newState != RecyclerView.SCROLL_STATE_IDLE
                if (!isScrolling) {
                    thumbsScrolled = false
                    if (thumbsScrolling) {
                        contentView!!.requestDisallowInterceptTouchEvent(false)
                    }
                }
                thumbsScrolling = isScrolling
                setThumbsScrolling(thumbsScrolling && thumbsScrolled)
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (!thumbsScrolled) {
                    thumbsScrolled = abs(dx) > 1 && thumbsScrolling
                    setThumbsScrolling(thumbsScrolling && thumbsScrolled)
                }
                if (dx != 0 && thumbsScrolling) {
                    detectThumbPosition()
                }
            }
        })
        thumbsRecyclerView!!.setOverScrollMode(View.OVER_SCROLL_NEVER)
        thumbsRecyclerView!!.setController(this)
        thumbsRecyclerView!!.addItemDecoration(ThumbItemDecoration(thumbsAdapter!!))
        thumbsRecyclerView!!.setItemAnimator(null)
        thumbsRecyclerView!!.setBackgroundColor(Theme.getColor(ColorId.transparentEditor))
        thumbsRecyclerView!!.setLayoutManager(thumbsLayoutManager)
        thumbsRecyclerView!!.setAdapter(thumbsAdapter)

        thumbsRecyclerView!!.setAlpha(0f)
        thumbsRecyclerView!!.setLayoutParams(newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))
        Views.setBottomMargin(thumbsRecyclerView, controlsMargin)
        if (!Config.DISABLE_VIEWER_ELEVATION) {
            thumbsRecyclerView!!.setElevation(Screen.dp(3f).toFloat())
        }

        checkBottomWrapY()
        contentView!!.addView(thumbsRecyclerView)
    }

    private var savedThumbsPosition = -1
    private var savedThumbsOffset = 0

    private fun saveThumbsPosition() {
        savedThumbsPosition = thumbsLayoutManager!!.findFirstVisibleItemPosition()
        val savedThumbView = thumbsLayoutManager!!.findViewByPosition(savedThumbsPosition)
        savedThumbsOffset = if (savedThumbView != null) thumbsLayoutManager!!.getDecoratedLeft(savedThumbView) else 0
    }

    private fun restoreThumbsPosition(shift: Int) {
        if (savedThumbsPosition != -1) {
            savedThumbsPosition += shift
            if (thumbsRecyclerView!!.getMeasuredWidth() > 0) {
                if (areThumbsScrolling()) {
                    thumbsLayoutManager!!.scrollToPositionWithOffset(savedThumbsPosition, savedThumbsOffset)
                } else {
                    ensureThumbsPosition(false, false)
                }
            }
        }
    }

    private fun calculateThumbWidth(): Int {
        return Screen.dp(if (mode == MODE_PROFILE || mode == MODE_CHAT_PROFILE) THUMBS_WIDTH_BIG else THUMBS_WIDTH_SMALL) + Screen.dp(THUMBS_SPACING_BETWEEN)
    }

    // Called when recyclerView width has been changed
    private fun resetThumbsPositionByLayout() {
        if (thumbsRecyclerView == null || thumbsAnimator == null || !thumbsAnimator!!.getValue()) {
            return
        }
        cancelAutoThumbScroller()
        thumbsRecyclerView!!.invalidateItemDecorations()
        ensureThumbsPosition(false, false)
    }

    private var autoThumbScroller: FactorAnimator? = null
    private var autoScrollDistance = 0
    private var autoScrollDistanceLast = 0
    private var autoScrollItems: ThumbItems? = null
    private var autoScrollStartValue = 0f
    private fun cancelAutoThumbScroller() {
        if (autoThumbScroller != null) {
            autoThumbScroller!!.forceFactor(0f)
            autoScrollItems = null
        }
    }

    private fun setAutoThumbScrollFactor(factor: Float) {
        val distance = (autoScrollDistance.toFloat() * factor).toInt()
        val distanceDiff = distance - autoScrollDistanceLast
        if (distance != 0) {
            autoScrollDistanceLast = distance
            thumbsRecyclerView!!.scrollBy(distanceDiff * (if (Lang.rtl()) -1 else 1), 0)
        }
        val expandFactor = autoScrollStartValue + (1f - autoScrollStartValue) * factor
        if (autoScrollItems!!.expandFactor != expandFactor) {
            autoScrollItems!!.expandFactor = expandFactor
            thumbsRecyclerView!!.invalidate()
        }
    }

    // Called when thumbnail was clicked
    private fun fastShowMediaItem(item: MediaItem?, items: ThumbItems?, index: Int, animatePosition: Boolean): Boolean {
        if (item == null || items == null) {
            return false
        }
        if (index == -1) {
            return false
        }
        if (mediaView == null || mediaView!!.isMovingItem()) {
            return false
        }
        /*TODO?
    if (animatePosition && items.expandFactor != 1f && items.expandFactor != 0f) {
      return false;
    }*/
        val changed = items.focusItemPosition != index
        if (changed && animatePosition) {
            items.swapFocusWithSecondary()
            items.setFocusItem(item, index, items.expandFactor)
        } else {
            if (items.setExpandingItems(index, -1, items.expandFactor)) {
                thumbsRecyclerView!!.invalidate()
            }
        }
        if (changed) {
            mediaView!!.getBaseCell()
                .setMedia(item, true, Screen.dp(THUMBS_HEIGHT) + Screen.dp(THUMBS_PADDING) * 2, if (animatePosition) 1.0f else this.thumbStrength)
            stack!!.forceIndex(items.indexInStack + index)
        }
        if (animatePosition) {
            val expandFactor = items.expandFactor
            items.expandFactor = 1f
            ensureThumbsPosition(true, true)
            items.expandFactor = expandFactor
            cancelAutoThumbScroller()
            thumbsRecyclerView!!.stopScroll()
            if (desiredThumbScrollByX == 0) {
                ensureThumbsPosition(true, false)
                thumbsRecyclerView!!.invalidate()
            } else {
                autoScrollItems = items
                autoScrollStartValue = items.expandFactor
                autoScrollDistance = desiredThumbScrollByX
                autoScrollDistanceLast = 0
                if (autoThumbScroller == null) {
                    autoThumbScroller = FactorAnimator(ANIMATOR_THUMBS_AUTO_SCROLLER, this, DECELERATE_INTERPOLATOR, 180L)
                }
                autoThumbScroller!!.animateTo(1f)
            }
        }
        return changed
    }

    private var totalThumbScrollX = 0
    private var desiredThumbScrollX = 0
    private var desiredThumbScrollByX = 0

    private fun ensureThumbsPosition(scrollBy: Boolean, onlyMeasure: Boolean) {
        if (thumbsRecyclerView!!.getMeasuredWidth() > 0) {
            ensureThumbsPosition(scrollBy, onlyMeasure, thumbsAdapter!!.items!!.focusItemPosition)
        }
    }

    private fun ensureThumbsPosition(scrollBy: Boolean, onlyMeasure: Boolean, desiredPosition: Int) {
        val firstVisiblePosition = thumbsLayoutManager!!.findFirstVisibleItemPosition()
        val firstVisibleView = if (firstVisiblePosition != RecyclerView.NO_POSITION) thumbsLayoutManager!!.findViewByPosition(firstVisiblePosition) else null
        val firstVisibleOffset =
            if (firstVisibleView != null) if (Lang.rtl()) thumbsRecyclerView!!.getMeasuredWidth() - thumbsLayoutManager!!.getDecoratedRight(firstVisibleView) else thumbsLayoutManager!!.getDecoratedLeft(
                firstVisibleView
            ) else 0

        val thumbWidth = calculateThumbWidth()
        val halfParentWidth = thumbsRecyclerView!!.getMeasuredWidth() / 2
        val offsetX = halfParentWidth - thumbWidth / 2

        var totalScrollX: Int
        if (firstVisiblePosition != RecyclerView.NO_POSITION && firstVisibleView != null) {
            totalScrollX = -firstVisibleOffset + thumbWidth * firstVisiblePosition
            if (firstVisiblePosition > 0) {
                totalScrollX += offsetX
            }
            if (totalScrollX < 0) {
                totalScrollX = 0
            }
        } else {
            totalScrollX = -1
        }
        var desiredScrollX = -1

        if (desiredPosition != -1) {
            var desiredOffset = if (desiredPosition != 0) offsetX else 0
            val secondaryPosition = thumbsAdapter!!.items!!.secondaryItemPosition
            if (secondaryPosition != -1) {
                val expandFactor = thumbsAdapter!!.items!!.expandFactor
                if (secondaryPosition < desiredPosition) {
                    desiredOffset += ((thumbWidth * (desiredPosition - secondaryPosition)).toFloat() * (1f - expandFactor)).toInt()
                } else if (secondaryPosition > desiredPosition) {
                    desiredOffset -= ((thumbWidth * (secondaryPosition - desiredPosition)).toFloat() * (1f - expandFactor)).toInt()
                }
                // TODO skip items, if (desiredOffset / thumbsWidth) != 0
            }
            if (scrollBy && totalScrollX != -1) {
                desiredScrollX = desiredPosition * thumbWidth - desiredOffset
                if (desiredPosition != 0) {
                    desiredScrollX += offsetX
                }
                if (desiredScrollX != totalScrollX && !onlyMeasure) {
                    thumbsRecyclerView!!.scrollBy((desiredScrollX - totalScrollX) * (if (Lang.rtl()) -1 else 1), 0)
                } else {
                    desiredThumbScrollByX = desiredScrollX - totalScrollX
                }
            } else {
                if (!onlyMeasure) {
                    thumbsLayoutManager!!.scrollToPositionWithOffset(desiredPosition, desiredOffset)
                } else {
                    desiredThumbScrollByX = 0
                }
            }
        }

        if (onlyMeasure) {
            totalThumbScrollX = totalScrollX
            desiredThumbScrollX = desiredScrollX
        }
    }

    private var slideFactor = 0f

    override fun onFactorChanged(view: MediaView?, factor: Float) {
        if (thumbsAnimator == null || !thumbsAnimator!!.getValue() || thumbsAdapter!!.items == null) {
            return
        }

        this.slideFactor = factor

        var focusItemIndex = stack!!.getCurrentIndex()

        var secondaryItemIndex = -1
        var expandFactor = 1f
        if (factor < 0f && focusItemIndex > 0) {
            secondaryItemIndex = thumbsAdapter!!.items!!.stackIndexToLocalIndex(focusItemIndex - 1)
            expandFactor = 1f + factor
        } else if (factor > 0f && focusItemIndex + 1 < stack!!.getCurrentSize()) {
            secondaryItemIndex = thumbsAdapter!!.items!!.stackIndexToLocalIndex(focusItemIndex + 1)
            expandFactor = 1f - factor
        }
        if (secondaryItemIndex == -1) {
            expandFactor = 1f
        }
        focusItemIndex = thumbsAdapter!!.items!!.stackIndexToLocalIndex(focusItemIndex)
        if (focusItemIndex == -1) {
            return
        }

        if (thumbsAdapter!!.items!!.setExpandingItems(focusItemIndex, secondaryItemIndex, expandFactor)) {
            ensureThumbsPosition(true, false)
            thumbsRecyclerView!!.invalidate()
        }
    }

    private class ThumbRecyclerView(context: Context) : RecyclerView(context), Runnable, ClickHelper.Delegate {
        private val clickHelper: ClickHelper

        private var cancelClick = false

        private var controller: MediaViewController? = null

        fun setController(controller: MediaViewController?) {
            this.controller = controller
        }

        private var lastWidth = 0

        init {
            this.clickHelper = ClickHelper(this)
            this.clickHelper.setNoSound(true)
        }

        override fun onMeasure(widthSpec: Int, heightSpec: Int) {
            super.onMeasure(widthSpec, heightSpec)
            val newWidth = getMeasuredWidth()
            if (lastWidth != newWidth) {
                lastWidth = newWidth
                post(this)
            }
        }

        fun cancelClick() {
            cancelClick = true
        }

        override fun run() {
            controller!!.resetThumbsPositionByLayout()
        }

        fun allowTouch(): Boolean {
            return controller != null && (!controller!!.mediaView!!.isMovingItem() && controller!!.thumbsFactor == 1f) && Views.isValid(this)
        }

        override fun onTouchEvent(e: MotionEvent): Boolean {
            if (e.getAction() != MotionEvent.ACTION_DOWN || allowTouch()) {
                val down = e.getAction() == MotionEvent.ACTION_DOWN
                if (down && controller!!.isAutoScrolling) {
                    if (!controller!!.dropAutoThumbScroll()) {
                        return false
                    }
                }
                var res = super.onTouchEvent(e)
                if (down || !((cancelClick || controller!!.areThumbsScrolling()).also { cancelClick = it })) {
                    cancelClick = false
                    res = clickHelper.onTouchEvent(this, e) || res
                }
                return res
            }
            return false
        }

        override fun onInterceptTouchEvent(e: MotionEvent): Boolean {
            return super.onInterceptTouchEvent(e) || (e.getAction() == MotionEvent.ACTION_DOWN && !allowTouch())
        }

        // Click
        override fun needClickAt(view: View, x: Float, y: Float): Boolean {
            return true
        }

        override fun onClickAt(view: View, x: Float, y: Float) {
            if (controller!!.areThumbsScrolling() || y < 0 || y > getMeasuredHeight() || controller!!.thumbsScrolled || controller!!.isAutoScrolling) {
                return
            }
            val childCount = getChildCount()
            for (i in 0..<childCount) {
                val child = getChildAt(i)
                if (child == null) {
                    continue
                }
                val thumbView = ThumbViewHolder.Companion.getThumbView(child)
                val item = thumbView.item
                val items = thumbView.items
                if (items != null && thumbView.previewReceiver.isInsideReceiver(x, y)) {
                    if (controller!!.fastShowMediaItem(item, items, items.indexOf(item), true)) {
                        onClick(this)
                        return
                    }
                }
            }
        }
    }

    private class ThumbView(context: Context?, drawTarget: RecyclerView?) : View(context), AttachDelegate, ThumbExpandChangeListener, Destroyable,
        InvalidateContentProvider {
        private val preview: DoubleImageReceiver
        private val avatarReceiver: AvatarReceiver

        var items: ThumbItems? = null
            private set
        var item: MediaItem? = null
            private set

        val previewReceiver: Receiver
            get() = if (item != null && item!!.isAvatar()) avatarReceiver else this.preview

        override fun performDestroy() {
            preview.destroy()
            avatarReceiver.destroy()
        }

        override fun onThumbExpandFactorChanged(item: MediaItem?) {
            if (getParent() != null) {
                (getParent() as ViewGroup).invalidate()
            }
        }

        private var isAttached = true

        override fun attach() {
            preview.attach()
            avatarReceiver.attach()
            isAttached = true
            if (item != null) {
                item!!.attachToThumbView(this)
            }
        }

        override fun detach() {
            preview.detach()
            avatarReceiver.detach()
            isAttached = false
            if (item != null) {
                item!!.detachFromThumbView(this)
            }
        }

        override fun invalidateContent(cause: Any?): Boolean {
            if (this.item != null && this.item!!.getPreviewImageFile() == null && (Config.VIDEO_CLOUD_PLAYBACK_AVAILABLE || this.item!!.isLoaded())) {
                if (this.item!!.isAvatar()) {
                    this.item!!.requestAvatar(this.avatarReceiver, false)
                    preview.clear()
                } else {
                    this.preview.getImageReceiver().requestFile(item!!.getThumbImageFile(Screen.dp(THUMBS_HEIGHT) + Screen.dp(THUMBS_PADDING) * 2, false))
                    avatarReceiver.clear()
                }
                return true
            }
            return false
        }

        fun setItem(item: MediaItem?, items: ThumbItems?) {
            if (this.item !== item) {
                if (this.item != null && isAttached) {
                    this.item!!.detachFromThumbView(this)
                }
                this.item = item
                this.items = items
                if (item == null) {
                    preview.clear()
                    avatarReceiver.clear()
                } else if (item.isAvatar()) {
                    item.requestAvatar(avatarReceiver, false)
                    preview.clear()
                } else {
                    preview.requestFile(item.getThumbImageMiniThumb(), item.getThumbImageFile(Screen.dp(THUMBS_HEIGHT) + Screen.dp(THUMBS_PADDING) * 2, false))
                    avatarReceiver.clear()
                }
                // preview.requestFile(item != null ? item.getThumbImageFile(Screen.dp(THUMBS_HEIGHT) + Screen.dp(THUMBS_PADDING) * 2, false) : null);
                layoutImage()
                if (isAttached) {
                    item!!.attachToThumbView(this)
                }
                invalidate()
            } else if (this.items !== items) {
                this.items = items
                invalidate()
            }
        }

        private var thumbStartWidth = 0
        private var thumbEndWidth = 0
        private var thumbHeight = 0

        init {
            preview = DoubleImageReceiver(drawTarget, 0)
            avatarReceiver = AvatarReceiver(drawTarget)
            avatarReceiver.setFullScreen(true, false)
            avatarReceiver.setScaleMode(AvatarReceiver.ScaleMode.CENTER_CROP)
        }

        fun layoutImage(): Boolean {
            val totalPaddingHorizontal = Screen.dp(THUMBS_SPACING_BETWEEN)
            val paddingVertical = Screen.dp(THUMBS_PADDING)
            val width = getMeasuredWidth()
            val height = getMeasuredHeight()

            if (width == 0 || height == 0) {
                return false
            }

            val thumbHeight = (height - paddingVertical * 2)

            val startWidth = (width - totalPaddingHorizontal)
            val endWidth = ThumbItems.getEndWidth(item, startWidth, thumbHeight)

            if (this.thumbStartWidth != startWidth || this.thumbEndWidth != endWidth || this.thumbHeight != thumbHeight) {
                this.thumbStartWidth = startWidth
                this.thumbEndWidth = endWidth
                this.thumbHeight = thumbHeight
                return true
            }
            return false
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            layoutImage()
        }

        fun drawImage(c: Canvas?, centerX: Int, startY: Int, alpha: Float, expandAllowance: Float) {
            layoutImage()

            if (thumbStartWidth == 0 || thumbHeight == 0) {
                return
            }

            val expandFactor = if (items != null) items!!.getExpandFactor(item) * expandAllowance else 0f
            val thumbWidth = thumbStartWidth + ((thumbEndWidth - thumbStartWidth).toFloat() * expandFactor).toInt()

            val preview = this.previewReceiver
            if (alpha != 1f) {
                preview.setPaintAlpha(alpha)
            }
            val startX = centerX - thumbWidth / 2
            preview.setBounds(startX, startY, startX + thumbWidth, startY + thumbHeight)
            if (preview.needPlaceholder()) {
                preview.drawPlaceholderRounded(c, 0f, 0x10ffffff)
            }
            preview.draw(c)

            if (alpha != 1f) {
                preview.restorePaintAlpha()
            }
        }
    }

    private class ThumbViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        companion object {
            fun create(context: Context?, controller: MediaViewController): ThumbViewHolder {
                val thumbView = ThumbView(context, controller.thumbsRecyclerView)
                thumbView.setLayoutParams(RecyclerView.LayoutParams(controller.calculateThumbWidth(), Screen.dp(THUMBS_PADDING) * 2 + Screen.dp(THUMBS_HEIGHT)))
                return ThumbViewHolder(thumbView)
                /*thumbView.setLayoutParams(FrameLayout.newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
      FrameLayout wrapView = new FrameLayout(context);
      wrapView.setLayoutParams(new RecyclerView.LayoutParams(controller.calculateThumbWidth(), ViewGroup.LayoutParams.MATCH_PARENT));
      wrapView.addView(thumbView);
      return new ThumbViewHolder(wrapView);*/
            }

            fun getThumbView(v: View): ThumbView {
                return v as ThumbView // v instanceof ThumbView ? (ThumbView) v : (ThumbView) ((ViewGroup) v).getChildAt(0);
            }
        }
    }

    private class ThumbItems(private val adapter: ThumbAdapter, private val items: ArrayList<MediaItem?>?, var indexInStack: Int) {
        fun stackIndexToLocalIndex(stackIndex: Int): Int {
            return if (items == null || stackIndex < indexInStack || stackIndex >= indexInStack + items.size) -1 else stackIndex - indexInStack
        }

        val mediaGroupId: Long
            get() = items!!.get(0)!!.getMessage().mediaAlbumId

        fun indexOf(item: MediaItem?): Int {
            return items!!.indexOf(item)
        }

        fun size(): Int {
            return if (items != null) items.size else 0
        }

        fun get(i: Int): MediaItem? {
            return if (items != null && i >= 0 && i < items.size) items.get(i) else null
        }

        val first: MediaItem?
            get() = get(0)

        val last: MediaItem?
            get() = if (items != null && !items.isEmpty()) items.get(items.size - 1) else null

        fun deleteItem(indexInStack: Int, item: MediaItem?) {
            val index = indexOf(item)
            if (index != -1) {
                items!!.removeAt(index)
                if (this.indexInStack > indexInStack) {
                    this.indexInStack--
                }
                adapter.notifyItemRemoved(index)
                adapter.controller.thumbsRecyclerView!!.invalidateItemDecorations()
            }
        }

        fun replaceItem(indexInStack: Int, oldItem: MediaItem?, newItem: MediaItem?) {
            if (secondaryItem === oldItem) {
                secondaryItem = newItem
            }
            if (focusItem === oldItem) {
                focusItem = newItem
            }
            val index = indexOf(oldItem)
            if (index != -1) {
                items!!.set(index, newItem)
                adapter.notifyItemChanged(index)
                adapter.controller.thumbsRecyclerView!!.invalidateItemDecorations()
            }
        }

        fun addItems(items: ArrayList<MediaItem>, onTop: Boolean, checkMediaGroupId: Boolean) {
            var items = items
            val totalAddedCount = items.size
            if (checkMediaGroupId) {
                var addItems: ArrayList<MediaItem>? = null

                val mediaGroupId = this.items!!.get(0)!!.getMessage().mediaAlbumId
                val estimatedSize = max(1, MAX_MESSAGE_GROUP_SIZE - this.items.size)
                if (onTop) {
                    val size = items.size
                    for (i in size - 1 downTo 0) {
                        val item = items.get(i)
                        if (item.getMessage().mediaAlbumId != mediaGroupId) {
                            break
                        }
                        if (addItems == null) {
                            addItems = ArrayList<MediaItem>(estimatedSize)
                        }
                        addItems.add(item)
                    }
                } else {
                    for (item in items) {
                        if (item.getMessage().mediaAlbumId != mediaGroupId) {
                            break
                        }
                        if (addItems == null) {
                            addItems = ArrayList<MediaItem>(estimatedSize)
                        }
                        addItems.add(item)
                    }
                }

                if (addItems == null) {
                    if (onTop) {
                        indexInStack += totalAddedCount
                    }
                    return
                }

                items = addItems
            }

            val addedItemCount = items.size
            if (onTop) {
                indexInStack += (totalAddedCount - addedItemCount)
            }

            val insertIndex: Int
            val changedIndex: Int
            if (onTop) {
                insertIndex = 0
                changedIndex = items.size
                this.items!!.addAll(0, items)
            } else {
                insertIndex = this.items!!.size
                changedIndex = insertIndex - 1
                this.items.addAll(items)
            }

            if (adapter.items === this) {
                if (onTop) {
                    shiftPositions(addedItemCount)
                }
                adapter.controller.saveThumbsPosition()
                adapter.notifyItemRangeInserted(insertIndex, addedItemCount)
                adapter.notifyItemChanged(insertIndex)
                adapter.controller.thumbsRecyclerView!!.invalidateItemDecorations()
                adapter.controller.restoreThumbsPosition(if (onTop) addedItemCount else 0)
            }
        }

        private var focusItem: MediaItem? = null
        var focusItemPosition: Int = -1
            private set

        private var secondaryItem: MediaItem? = null
        var secondaryItemPosition: Int = -1
            private set

        fun shiftPositions(shift: Int) {
            if (focusItemPosition != -1) {
                focusItemPosition += shift
            }
            if (secondaryItemPosition != -1) {
                secondaryItemPosition += shift
            }
        }

        fun setExpandingItems(focusItemPosition: Int, secondaryItemPosition: Int, expandFactor: Float): Boolean {
            if (this.focusItemPosition != focusItemPosition || this.secondaryItemPosition != secondaryItemPosition || this.expandFactor != expandFactor) {
                this.focusItemPosition = focusItemPosition
                this.focusItem = get(focusItemPosition)
                this.secondaryItemPosition = secondaryItemPosition
                this.secondaryItem = get(secondaryItemPosition)
                this.expandFactor = expandFactor
                return true
            }
            return false
        }

        fun setFocusItem(item: MediaItem?, position: Int, expandFactor: Float) {
            this.focusItem = item
            this.focusItemPosition = position
            this.expandFactor = expandFactor
        }

        fun setSecondaryItem(item: MediaItem?, position: Int) {
            this.secondaryItem = item
            this.secondaryItemPosition = position
        }

        var expandFactor: Float = 0f

        fun swapFocusWithSecondary() {
            val tempPosition = secondaryItemPosition
            val tempItem = secondaryItem

            secondaryItemPosition = focusItemPosition
            secondaryItem = focusItem

            focusItemPosition = tempPosition
            focusItem = tempItem

            expandFactor = 1f - expandFactor
        }

        fun getExpandFactor(item: MediaItem?): Float {
            return if (item == null) 0f else if (item === focusItem) expandFactor else if (item === secondaryItem) 1f - expandFactor else 0f
        }

        fun getTranslationX(item: MediaItem?, position: Int, expandAllowance: Float): Float {
            var x = 0f
            val add = Screen.dp(THUMBS_SPACING_ADD).toFloat()
            val expandFactor = this.expandFactor * expandAllowance
            val thumbHeight = Screen.dp(THUMBS_HEIGHT)
            val startWidth = adapter.controller.calculateThumbWidth()
            if (focusItem != null && focusItem !== item && expandFactor > 0f) {
                val endWidth = getEndWidth(focusItem, startWidth, thumbHeight)
                x += (add + (endWidth - startWidth) / 2) * expandFactor * (if (position < focusItemPosition) -1f else 1f)
            }
            if (secondaryItem != null && secondaryItem !== item && expandFactor < 1f) {
                val endWidth = getEndWidth(secondaryItem, startWidth, thumbHeight)
                x += (add + (endWidth - startWidth) / 2) * (1f - expandFactor) * (if (position < secondaryItemPosition) -1f else 1f)
            }
            return x
        }

        companion object {
            fun getEndWidth(item: MediaItem?, startWidth: Int, thumbHeight: Int): Int {
                var endWidth = 0
                if (item != null) {
                    val sourceWidth = item.getWidth()
                    val sourceHeight = item.getHeight()
                    val scale = if (sourceHeight != 0) thumbHeight.toFloat() / sourceHeight.toFloat() else 1f
                    endWidth = (sourceWidth.toFloat() * scale).toInt()
                }
                return min(max(startWidth, endWidth), Screen.dp(THUMBS_WIDTH_MAX))
            }
        }
    }

    private class ThumbAdapter(val context: Context?, val controller: MediaViewController) : RecyclerView.Adapter<ThumbViewHolder>() {
        var items: ThumbItems? = null

        fun submitItems(items: ThumbItems?) {
            if (this.items == null) {
                this.items = items
                if (items != null) {
                    notifyItemRangeInserted(0, items.size())
                }
            } else if (items == null) {
                val oldItemCount = this.items!!.size()
                this.items = null
                if (oldItemCount > 0) {
                    notifyItemRangeRemoved(0, oldItemCount)
                }
            } else if (this.items!!.size() == items.size()) {
                this.items = items
                notifyItemRangeChanged(0, items.size())
            } else {
                val oldItemCount = this.items!!.size()
                this.items = null
                notifyItemRangeRemoved(0, oldItemCount)
                this.items = items
                notifyItemRangeInserted(0, items.size())
            }
        }

        override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ThumbViewHolder {
            return ThumbViewHolder.create(context, controller)
        }

        override fun getItemCount(): Int {
            return if (items != null) items!!.size() else 0
        }

        override fun onBindViewHolder(holder: ThumbViewHolder, index: Int) {
            val thumbView = ThumbViewHolder.Companion.getThumbView(holder.itemView)
            if (items != null) {
                val item = items!!.get(index)
                thumbView.setItem(item!!, items)
                val threshold = max(LOAD_THRESHOLD, controller.thumbsRecyclerView!!.getChildCount())
                if (index >= items!!.size() - threshold) {
                    controller.loadMoreIfNeeded(true, true)
                } else if (index - threshold <= 0) {
                    controller.loadMoreIfNeeded(true, false)
                }
            } else {
                thumbView.setItem(null, null)
            }
        }

        override fun onViewAttachedToWindow(holder: ThumbViewHolder) {
            Views.attach(ThumbViewHolder.Companion.getThumbView(holder.itemView))
        }

        override fun onViewDetachedFromWindow(holder: ThumbViewHolder) {
            Views.detach(ThumbViewHolder.Companion.getThumbView(holder.itemView))
        }
    }

    // CreateView
    override fun onCreateView(context: Context): View {
        context().closeOtherPips()

        if (SET_FULLSCREEN_ON_OPEN) {
            setFullScreen(true)
        }

        restorePipFactors()

        popupView = PopupLayout(context)
        popupView!!.setOverlayStatusBar(true)
        popupView!!.setShowListener(this)
        if (mode == MODE_SECRET) {
            popupView!!.setIgnoreHorizontal()
        }
        if (mode == MODE_GALLERY) {
            popupView!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        if (this.isCurrentCamera) {
            popupView!!.setIgnoreSystemNavigationBar(true)
        }
        popupView!!.setTouchProvider(this)
        popupView!!.setActivityListener(object : ActivityListener {
            override fun onActivityPause() {
                mediaView!!.onMediaActivityPause()
                val item = stack!!.getCurrent()
                if (item != null && item.isViewOnce()) {
                    item.viewContent(true)
                }
            }

            override fun onActivityResume() {
                mediaView!!.onMediaActivityResume()
            }

            override fun onActivityDestroy() {}

            override fun onActivityPermissionResult(code: Int, granted: Boolean) {}
        })
        if (!canRunFullscreen()) {
            popupView!!.setNeedRootInsets()
            popupView!!.setHideKeyboard()
            popupView!!.init(false)
        } else {
            popupView!!.setNeedRootInsets()
            popupView!!.init(true)
            popupView!!.setIgnoreAllInsets(true)
        }
        popupView!!.setBoundController(this)

        flingDetector = FlingDetector(context, this)
        contentView = object : FrameLayoutFix(context) {
            private var lastWidth = 0
            private var lastHeight = 0

            override fun onAttachedToWindow() {
                super.onAttachedToWindow()
                setRootView(Views.findAncestor<RootFrameLayout?>(this, RootFrameLayout::class.java, false))
            }

            override fun onDetachedFromWindow() {
                super.onDetachedFromWindow()
                setRootView(null)
            }

            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                updatePipLayout(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec), true, true)
                super.onMeasure(widthMeasureSpec, heightMeasureSpec)
                if (mode == MODE_GALLERY) {
                    val newWidth = getMeasuredWidth()
                    val newHeight = getMeasuredHeight()
                    if (lastWidth == 0 || lastHeight == 0) {
                        lastWidth = newWidth
                        lastHeight = newHeight
                    } else if (lastWidth != newWidth || lastHeight != newHeight) {
                        lastWidth = newWidth
                        lastHeight = newHeight
                        resetMediaPaddings(currentSection)
                    }
                }
            }

            private var startX = 0f
            private var startY = 0f
            private var diffX = 0f
            private var diffY = 0f
            private var slideStartY = 0f
            private var slideStartX = 0f

            private var disallowIntercept = false

            override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
                super.requestDisallowInterceptTouchEvent(disallowIntercept)
                this.disallowIntercept = disallowIntercept
            }

            override fun onInterceptTouchEvent(e: MotionEvent): Boolean {
                if (slideAnimator != null && slideAnimator!!.isAnimating()) {
                    return true
                }
                if (mode == MODE_SECRET || inCaption || (disallowIntercept && e.getAction() != MotionEvent.ACTION_DOWN)) {
                    return super.onInterceptTouchEvent(e)
                }
                when (e.getAction()) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = e.getX()
                        startY = e.getY()
                        listenCloseBySlide = canCloseBySlide() && pipFactor == 0f && !mediaView!!.isZoomed() && mediaView!!.isBaseVisible()
                    }

                    MotionEvent.ACTION_MOVE -> {
                        if (listenCloseBySlide) {
                            val x = e.getX()
                            val y = e.getY()
                            if (abs(startY - y) >= Screen.getTouchSlopBig() && abs(startX - x) < Screen.getTouchSlop() * 1.65f) {
                                mediaView!!.dropPreview(MediaView.DIRECTION_AUTO, 0f)
                                listenCloseBySlide = false
                                slideStartX = x
                                slideStartY = y
                                setInSlideMode(x, y)
                                return true
                            } else if (abs(startX - x) >= Screen.getTouchSlopY()) {
                                listenCloseBySlide = false
                            }
                        } else if (inSlideMode) {
                            return true
                        } else if (listenCatchPip) {
                            val x = e.getX()
                            val y = e.getY()
                            if (max(abs(startX - x), abs(startY - y)) > Screen.getTouchSlopBig()) {
                                listenCatchPip = false
                                diffX = startX - x
                                diffY = startY - y
                                startX = x
                                startY = y
                                setPipUp(true, 0f, 0f)
                                return true
                            }
                        } else if (isPipUp) {
                            return true
                        }
                    }
                }
                flingDetector!!.onTouchEvent(e)
                return super.onInterceptTouchEvent(e)
            }

            override fun onTouchEvent(e: MotionEvent): Boolean {
                if (slideAnimator != null && slideAnimator!!.isAnimating()) {
                    return true
                }
                if (mode == MODE_SECRET) {
                    return true
                }
                flingDetector!!.onTouchEvent(e)
                when (e.getAction()) {
                    MotionEvent.ACTION_MOVE -> {
                        if (isPipUp) {
                            val x = e.getX()
                            val y = e.getY()
                            movePip(x - startX, y - startY, mediaView!!.getMeasuredWidth(), mediaView!!.getMeasuredHeight())
                            return true
                        }
                        if (inSlideMode) {
                            val x = e.getX()
                            val y = e.getY()
                            setSlide(x - slideStartX, y - slideStartY, slideStartX, false, true)
                            return true
                        }
                    }

                    MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                        if (isPipUp) {
                            setPipUp(false, 0f, 0f)
                            return true
                        }
                        if (inSlideMode) {
                            dropSlideMode(0f, 0f, false)
                        }
                    }
                }
                return super.onTouchEvent(e)
            }

            override fun onDraw(c: Canvas) {
                if (commonFactor == 0f) {
                    return
                }
                var alpha = max(0f, min(1f, commonFactor))

                val saved =
                    path != null && !inSlideMode && lastSlideX == 0f && lastSlideY == 0f && currentThumb != null && commonFactor > 0f && commonFactor < 1f && !currentThumb!!.noBounce()
                val saveCount = if (saved) ViewSupport.clipPath(c, path) else Int.MIN_VALUE

                if (currentThumb != null && commonFactor < 1f && !inSlideMode && lastSlideX == 0f && lastSlideY == 0f) {
                    currentThumb!!.drawPlaceholder(c)
                }

                alpha *= (1f - pipFactor)

                if (alpha > 0f) {
                    val color = (255f * alpha).toInt() shl 24
                    c.drawColor(color)
                }

                if (saved) {
                    ViewSupport.restoreClipPath(c, saveCount)
                }
            }
        }
        if (Config.HARDWARE_MEDIA_VIEWER) {
            Views.setLayerType(contentView, View.LAYER_TYPE_HARDWARE)
        }
        contentView!!.setWillNotDraw(false)
        contentView!!.setLayoutParams(newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        mediaView = MediaView(context)
        mediaView!!.setFactorChangeListener(this)
        mediaView!!.prepare(mode != MODE_SECRET)
        updateMediaView()
        mediaView!!.setCellCallback(this)
        mediaView!!.setBoundController(this)
        mediaView!!.initWithStack(stack)
        stack!!.setCallback(this)
        mediaView!!.setLayoutParams(newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        contentView!!.addView(mediaView)

        if (needHeader()) {
            headerCell = DoubleHeaderView(context)
            headerCell!!.setThemedTextColor(this)
            headerCell!!.initWithMargin(measureButtonsPadding(), true)
            if (mode == MODE_PROFILE) {
                val item = stack!!.getCurrent()
                headerCell!!.setTitle(tdlib!!.senderName(item.getSourceSender()))
            } else if (mode == MODE_CHAT_PROFILE) {
                val item = stack!!.getCurrent()
                val chat = tdlib!!.chat(item.getSourceChatId())
                headerCell!!.setTitle(if (chat != null) chat.title else "Chat#" + item.getSourceChatId())
            }
            onMediaStackChanged(false)

            headerView = HeaderView(context)
            if ((mode == MODE_MESSAGES || mode == MODE_SIMPLE) && !Config.DISABLE_VIEWER_ELEVATION) {
                headerView!!.setElevation(Screen.dp(3f).toFloat())
            }
            attachHeaderViewWithoutNavigation(headerView)
            headerView!!.initWithSingleController(this, useEdgeToEdge() || ((SET_FULLSCREEN_ON_OPEN || canRunFullscreen()) && !Config.CUTOUT_ENABLED))
            headerView!!.getFilling().setShadowAlpha(0f)
            val leftMargin = Screen.dp(68f)
            val rightMargin = measureButtonsPadding()
            Views.setMargins(
                headerCell!!.getLayoutParams() as FrameLayout.LayoutParams?,
                if (Lang.rtl()) rightMargin else leftMargin,
                headerView!!.getEffectiveTopOffset(),
                if (Lang.rtl()) leftMargin else rightMargin,
                0
            )
            contentView!!.addView(headerView)
        }

        when (mode) {
            MODE_GALLERY -> {
                createViewGalleryMode(context, false)
            }

            MODE_SIMPLE, MODE_MESSAGES -> {
                var fp: FrameLayout.LayoutParams?

                pipControlsWrap = object : FrameLayoutFix(context) {
                    private var startX = 0f
                    private var startY = 0f

                    override fun onTouchEvent(e: MotionEvent): Boolean {
                        when (e.getAction()) {
                            MotionEvent.ACTION_DOWN -> {
                                if (pipFactor == 1f && (pipPositionAnimator == null || !pipPositionAnimator!!.isAnimating())) {
                                    listenCatchPip = true
                                    startX = e.getX()
                                    startY = e.getY()
                                    return true
                                }
                            }

                            MotionEvent.ACTION_CANCEL -> {
                                if (listenCatchPip) {
                                    listenCatchPip = false
                                    return true
                                }
                            }

                            MotionEvent.ACTION_UP -> {
                                if (listenCatchPip) {
                                    if (max(abs(startX - e.getX()), abs(startY - e.getY())) < Screen.getTouchSlop()) {
                                        togglePipControlsVisibility()
                                    }
                                    listenCatchPip = false
                                    return true
                                }
                            }
                        }
                        return super.onTouchEvent(e)
                    }
                }
                if (!Config.DISABLE_VIEWER_ELEVATION) {
                    pipControlsWrap!!.setTranslationZ(Screen.dp(10f).toFloat())
                }
                pipControlsWrap!!.setAlpha(0f)
                pipControlsWrap!!.setVisibility(View.GONE)
                pipControlsWrap!!.setLayoutParams(newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

                pipBackgroundView = View(context)
                pipBackgroundView!!.setLayoutParams(newParams(ViewGroup.LayoutParams.MATCH_PARENT, (Screen.dp(45f) * 1.2f).toInt(), Gravity.BOTTOM))
                pipBackgroundView!!.setAlpha(.7f)
                setBackground(pipBackgroundView, makeCubicGradientScrimDrawable(-0x1000000, 2, Gravity.BOTTOM, false))
                pipControlsWrap!!.addView(pipBackgroundView)

                fp = newParams(Screen.dp(45f), Screen.dp(45f), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
                fp.rightMargin = Screen.dp(45f)
                pipOpenButton = EditButton(context)
                pipOpenButton!!.setOnClickListener(this)
                pipOpenButton!!.setId(R.id.btn_inlineOpen)
                pipOpenButton!!.setIcon(R.drawable.deproko_baseline_outinline_24, false, false)
                pipOpenButton!!.setLayoutParams(fp)
                pipControlsWrap!!.addView(pipOpenButton)

                fp = newParams(Screen.dp(45f), Screen.dp(45f), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
                pipPlayPauseButton = PlayPauseButton(context)
                pipPlayPauseButton!!.setIsPlaying(isPlayingVideo, false)
                pipPlayPauseButton!!.setOnClickListener(this)
                pipPlayPauseButton!!.setId(R.id.btn_inlinePlayPause)
                pipPlayPauseButton!!.setLayoutParams(fp)
                pipControlsWrap!!.addView(pipPlayPauseButton)

                fp = newParams(Screen.dp(45f), Screen.dp(45f), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
                fp.leftMargin = Screen.dp(45f)
                pipCloseButton = EditButton(context)
                pipCloseButton!!.setOnClickListener(this)
                pipCloseButton!!.setId(R.id.btn_inlineClose)
                pipCloseButton!!.setIcon(R.drawable.baseline_close_24, false, false)
                pipCloseButton!!.setLayoutParams(fp)
                pipControlsWrap!!.addView(pipCloseButton)

                pipOverlayView = PipOverlayView(context)
                pipOverlayView!!.setAlpha(0f)
                pipOverlayView!!.setBackgroundColor(0x77000000)
                pipOverlayView!!.setLayoutParams(newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                pipControlsWrap!!.addView(pipOverlayView)

                contentView!!.addView(pipControlsWrap)

                fp = newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.BOTTOM)
                fp.bottomMargin = this.bottomWrapMargin

                bottomWrap = FrameLayoutFix(context)
                bottomWrap!!.setLayoutParams(fp)
                checkBottomWrapY()
                if (!Config.DISABLE_VIEWER_ELEVATION) {
                    bottomWrap!!.setElevation(Screen.dp(3f).toFloat())
                }

                val captionView: CustomTextView = object : CustomTextView(context, tdlib) {
                    override fun onTouchEvent(event: MotionEvent): Boolean {
                        return Views.onTouchEvent(this, event) && super.onTouchEvent(event)
                    }
                }
                captionView.setPadding(Screen.dp(14f), Screen.dp(14f), Screen.dp(14f), Screen.dp(14f))
                captionView.setTextColorId(ColorId.white, true)
                captionView.setTextSize(Screen.dp(16f).toFloat())
                captionView.setTextStyleProvider(TGMessage.textStyleProvider)
                captionView.setLinkColorId(ColorId.caption_textLink, ColorId.caption_textLinkPressHighlight)
                captionView.setForcedTheme(this@MediaViewController.forcedTheme)
                captionView.setId(R.id.input)
                captionView.setLayoutParams(RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                this.captionView = captionView


                val captionWrap = LinearLayout(context)
                captionWrap.setOrientation(LinearLayout.VERTICAL)
                captionWrap.addView(captionView)
                captionWrap.setLayoutParams(ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

                val scrollView = MaxHeightScrollView(context)
                scrollView.setMaxHeight(Text.getLineHeight(TGMessage.textStyleProvider, true) * 10 + Screen.dp(14f))
                scrollView.addView(captionWrap)
                scrollView.setAlpha(0f)
                scrollView.setBackgroundColor(Theme.getColor(ColorId.transparentEditor))
                scrollView.setLayoutParams(newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))
                this.captionWrapView = scrollView

                bottomWrap!!.addView(this.captionWrapView)

                videoSliderView = VideoControlView(context)
                videoSliderView!!.setOnPlayPauseClick(View.OnClickListener { v: View? ->
                    val c = stack!!.getCurrent().getFileProgress()
                    if (c != null) {
                        c.performClick(v)
                    }
                })
                videoSliderView!!.setSliderListener(this)
                videoSliderView!!.setInnerAlpha(0f)
                videoSliderView!!.setAlpha(0f)
                videoSliderView!!.setTranslationY(Screen.dp(56f).toFloat())
                videoSliderView!!.setLayoutParams(newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                videoSliderView!!.setSlideEnabled(stack!!.getCurrent().canSeekVideo())
                bottomWrap!!.addView(videoSliderView)

                contentView!!.addView(bottomWrap)
            }

            MODE_SECRET -> {
                if (!stack!!.getCurrent().isSecretOutgoing()) {
                    secretView = SecretTimerView(context)
                    secretView!!.setSecretPhoto(stack!!.getCurrent().getSecretPhoto())
                    secretView!!.setLayoutParams(newParams(ViewGroup.LayoutParams.MATCH_PARENT, Size.getHeaderPortraitSize(), Gravity.TOP))
                    contentView!!.addView(secretView)
                }
            }
        }

        bottomSpace = FillingSpace(context)
        bottomSpace!!.setThemedBackground(ColorId.transparentEditor, this)
        bottomSpace!!.setLayoutParams(FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, Gravity.BOTTOM))
        bottomSpace!!.setAlpha(0f)
        bottomSpace!!.setLayoutHeight(bottomInnerMargin, false)
        bottomSpace!!.setVisibility(if (bottomInnerMargin > 0) View.VISIBLE else View.GONE)
        if (!Config.DISABLE_VIEWER_ELEVATION) {
            bottomSpace!!.setElevation(Screen.dp(3f).toFloat())
        }
        contentView!!.addView(bottomSpace)

        updateVideoState(false)
        updateCaption(false)

        loadMoreIfNeeded()

        TGLegacyManager.instance().addEmojiListener(this)
        tdlib!!.context().calls().addCurrentCallListener(this)
        if (stack!!.getCurrent().getSourceChatId() != 0L) {
            subscribeToChatId(stack!!.getCurrent().getSourceChatId())
        }

        return contentView!!
    }

    private fun inProfilePhotoEditMode(): Boolean {
        return getArgumentsStrict().avatarPickerMode != AvatarPickerMode.NONE
    }

    private fun inSingleMediaMode(): Boolean {
        return hasFlag(Args.Companion.FLAG_DISALLOW_MULTI_SELECTION_MEDIA) || inProfilePhotoEditMode()
    }

    private fun hasFlag(flag: Int): Boolean {
        return getArgumentsStrict().hasFlag(flag)
    }

    private var controlsMargin = 0

    private fun setControlsMargin(margin: Int) {
        if (this.controlsMargin != margin) {
            this.controlsMargin = margin
            Views.setBottomMargin(thumbsRecyclerView, margin)
            Views.setBottomMargin(editWrap, margin)
            updateBottomWrapMargin()
            bottomSpace!!.setLayoutHeight(margin, false)
            bottomSpace!!.setVisibility(if (margin > 0 && !this.isEmojiVisible) View.VISIBLE else View.GONE)
        }
    }

    var bottomInnerMargin: Int = 0
        private set

    private var topOffset = 0

    private fun setTopOffset(topOffset: Int) {
        if (this.topOffset != topOffset) {
            this.topOffset = topOffset
            Views.setPaddingTop(othersView, topOffset)
            if (receiverView != null) {
                val fp = receiverView!!.getLayoutParams() as FrameLayout.LayoutParams
                if (topOffset > 0) {
                    fp.leftMargin = Screen.dp(8f)
                    fp.topMargin = topOffset + Screen.dp(4f)
                } else {
                    fp.leftMargin = Screen.dp(12f)
                    fp.topMargin = Screen.dp(4f)
                }
                receiverView!!.setLayoutParams(fp)
            }

            Views.setTopMargin(checkView, Screen.dp(20f) - Screen.dp(9f) + topOffset / 2)
            Views.setTopMargin(counterView, Screen.dp(26f) + topOffset / 2)
        }
    }

    private var rootView: RootFrameLayout? = null

    private fun setRootView(rootView: RootFrameLayout?) {
        if (this.rootView !== rootView) {
            if (this.rootView != null) {
                this.rootView!!.removeInsetsChangeListener(this)
            }
            this.rootView = rootView
            if (rootView != null) {
                rootView.addInsetsChangeListener(this)
                setTopOffset(rootView.getSystemInsets().top)
            }
        }
    }

    override fun onInsetsChanged(
        viewGroup: RootFrameLayout?,
        effectiveInsets: Rect?,
        effectiveInsetsWithoutIme: Rect?,
        systemInsets: Rect,
        systemInsetsWithoutIme: Rect?,
        isUpdate: Boolean
    ) {
        setTopOffset(systemInsets.top)
    }

    override fun dispatchSystemInsets(
        parentView: View?,
        originalParams: MarginLayoutParams?,
        legacyInsets: Rect?,
        insets: Rect,
        insetsWithoutIme: Rect,
        systemInsets: Rect?,
        systemInsetsWithoutIme: Rect?,
        fitsSystemWindows: Boolean
    ) {
        super.dispatchSystemInsets(parentView, originalParams, legacyInsets, insets, insetsWithoutIme, systemInsets, systemInsetsWithoutIme, fitsSystemWindows)
        val bottomInset = insets.bottom
        val changed = this.bottomInnerMargin != bottomInset
        this.bottomInnerMargin = bottomInset
        if (useEdgeToEdge() || (mode == MODE_GALLERY && this.isCurrentCamera)) {
            val controlsMargin = if (useEdgeToEdge() || bottomInset <= Screen.getNavigationBarHeight()) bottomInset else 0
            setControlsMargin(controlsMargin)
            val bottomOffset = getSectionBottomOffset(SECTION_CROP)
            Views.setBottomMargin(cropTargetView, bottomOffset)
            if (cropAreaView != null) {
                cropAreaView!!.setOffsetBottom(bottomOffset)
            }
            checkBottomWrapY()
            if (keyboardFrameLayout != null) {
                keyboardFrameLayout!!.setExtraBottomInset(insets.bottom, insetsWithoutIme.bottom)
            }
        }
        if (mediaView != null) {
            if (changed) {
                val offsetBottom = getSectionBottomOffset(currentSection)
                mediaView!!.setNavigationalOffsets(0, 0, offsetBottom)
            }
            mediaView!!.layoutCells()
        }
    }

    override fun destroy() {
        super.destroy()
        val current = stack!!.getCurrent()
        if (current != null && current.isViewOnce()) {
            current.viewContent(true)
        }
        if (!isMediaSent && getArguments() != null && hasFlag(Args.Companion.FLAG_DELETE_FILE_ON_EXIT) && stack != null) {
            for (i in 0..<stack!!.getCurrentSize()) {
                val item = stack!!.get(i)
                if (item.getSourceGalleryFile().isFromCamera()) {
                    item.deleteFiles()
                }
            }
        }
        if (thumbsRecyclerView != null) {
            Views.destroyRecyclerView(thumbsRecyclerView)
        }
        TGLegacyManager.instance().removeEmojiListener(this)
        if (mediaView != null) {
            mediaView!!.destroy()
        }
        if (secretView != null) {
            secretView!!.destroy()
        }
        tdlib!!.context().calls().removeCurrentCallListener(this)
        context.removeFullScreenView(this, true)
        context.removeHideNavigationView(this)
        if (captionView is Destroyable) {
            (captionView as Destroyable).performDestroy()
        }
        if (forceEditModeOld_captionView is Destroyable) {
            (forceEditModeOld_captionView as Destroyable).performDestroy()
        }
        if (sendButton != null) {
            sendButton!!.destroySlowModeCounterController()
        }
        subscribeToChatId(0)
    }

    override fun onCurrentCallChanged(tdlib: Tdlib?, call: TdApi.Call?) {
        if (call != null && !call.isOutgoing && !TD.isActive(call) && !TD.isFinished(call)) {
            minimizeOrClose()
        }
    }

    override fun onSetStateChanged(view: SliderView, isSetting: Boolean) {
        if (isSetting) {
            mediaView!!.pauseIfPlaying()
        } else {
            mediaView!!.resumeIfNeeded(view.getValue())
        }
    }

    override fun onValueChanged(view: SliderView?, factor: Float) {
        // TODO? mediaView.setSeekProgress(factor);
    }

    override fun allowSliderChanges(view: SliderView?): Boolean {
        return stack!!.getCurrent().isVideo() && (Config.VIDEO_CLOUD_PLAYBACK_AVAILABLE || stack!!.getCurrent().isLoaded())
    }

    private val selectedMediaCount: Int
        get() = if (selectDelegate != null) selectDelegate!!.getSelectedMediaCount() else 0

    private val isCurrentItemSelected: Boolean
        get() = selectDelegate != null && selectDelegate!!.isMediaItemSelected(stack!!.getCurrentIndex(), stack!!.getCurrent())

    val currentFile: TdApi.File
        get() = stack!!.getCurrent().getTargetFile()

    // Clicks
    private var showOtherMedias = false

    private var otherAnimator: FactorAnimator? = null
    private var otherFactor = 0f
    private fun animateOtherFactor(toFactor: Float) {
        if (otherAnimator == null) {
            otherAnimator = FactorAnimator(ANIMATOR_OTHER, this, DECELERATE_INTERPOLATOR, 180L, otherFactor)
        }
        otherAnimator!!.animateTo(toFactor)
    }

    private var othersView: MediaOtherRecyclerView? = null
    private var otherAdapter: MediaOtherAdapter? = null

    private fun setOtherFactor(factor: Float) {
        if (this.otherFactor != factor) {
            this.otherFactor = factor
            val alpha = max(0f, min(1f, factor))
            overlayView!!.setAlpha(alpha)
            othersView!!.setAlpha(alpha)
            setCheckAlpha(1f - alpha)
            setCounterAlpha((1f - alpha) * max(0f, min(1f, counterFactor)))
        }
    }

    private fun setCounterAlpha(alpha: Float) {
        counterView!!.setAlpha(alpha)
        counterView!!.setEnabled(alpha == 1f)
    }

    private fun setCheckAlpha(alpha: Float) {
        checkView!!.setAlpha(alpha)
        checkView!!.setEnabled(alpha == 1f)
        if (receiverView != null) {
            receiverView!!.setAlpha(alpha)
        }
    }

    val isFullyShown: Boolean
        get() = commonFactor == 1f

    private fun setSelectedImages(images: ArrayList<ImageFile?>?) {
        if (othersView == null) {
            otherAdapter = MediaOtherAdapter(context(), this)
            otherAdapter!!.setImages(images)

            val params: FrameLayout.LayoutParams?

            params = newParams(ViewGroup.LayoutParams.MATCH_PARENT, Screen.dp(112f) + topOffset, Gravity.TOP)

            othersView = MediaOtherRecyclerView(context())
            othersView!!.setPadding(Screen.dp(2f), topOffset, Screen.dp(2f), 0)
            othersView!!.setLayoutManager(LinearLayoutManager(context(), LinearLayoutManager.HORIZONTAL, true))
            othersView!!.setHasFixedSize(true)
            othersView!!.setItemAnimator(CustomItemAnimator(DECELERATE_INTERPOLATOR, 180L))
            othersView!!.setClipToPadding(false)
            othersView!!.setBackgroundColor(Theme.getColor(ColorId.transparentEditor))
            othersView!!.setOverScrollMode(View.OVER_SCROLL_NEVER)
            othersView!!.addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                    val position = parent.getChildAdapterPosition(view)
                    outRect.set(if (position == otherAdapter!!.getItemCount() - 1) Screen.dp(4f) else 0, Screen.dp(6f), Screen.dp(4f), Screen.dp(6f))
                }
            })
            othersView!!.setAlpha(0f)
            othersView!!.setAdapter(otherAdapter)
            othersView!!.setLayoutParams(params)
            contentView!!.addView(othersView)
        } else {
            otherAdapter!!.setImages(images)
        }
    }

    private fun setShowOtherMedias(showOtherMedias: Boolean) {
        if (this.showOtherMedias != showOtherMedias) {
            this.showOtherMedias = showOtherMedias
            if (showOtherMedias && selectDelegate != null) {
                val selectedFiles = selectDelegate!!.getSelectedMediaItems(true)
                setSelectedImages(selectedFiles)
                if (selectedFiles == null || selectedFiles.isEmpty()) {
                    return
                }
            }
            animateOtherFactor(if (showOtherMedias) 1f else 0f)
        }
    }

    private fun clearFilters(item: MediaItem?, index: Int) {
        if (item != null && item.getFiltersState() != null && !item.getFiltersState().isEmpty()) {
            val state = item.getFiltersState()
            item.setFiltersState(null)
            tdlib!!.filegen().removeFilteredBitmap(ImageFilteredFile.getPath(state))
        }
    }

    private fun unselectImage(imageFile: ImageFile?) {
        val i = stack!!.indexOfImageFile(imageFile)
        if (i != -1) {
            if (i == stack!!.getCurrentIndex()) {
                checkView!!.setChecked(false)
            }
            if (selectDelegate != null) {
                val item = stack!!.get(i)
                clearFilters(item, i)
                selectDelegate!!.setMediaItemSelected(i, item, false)
                val selectedCount = selectDelegate!!.getSelectedMediaCount()

                if (selectedCount == 0) {
                    forceCounterFactor(0f)
                } else {
                    counterView!!.setCounter(selectedCount)
                }
            }

            otherAdapter!!.removeImage(imageFile)
            if (otherAdapter!!.getItemCount() == 0) {
                setShowOtherMedias(false)
            }
        }
    }

    private var currentSection = 0
    private var fromSection = 0

    private fun getHorizontalOffsets(section: Int): Int {
        var futureOffset = 0

        when (section) {
            SECTION_CROP -> {
                futureOffset = Screen.dp(CROP_PADDING_HORIZONTAL)
            }
        }

        val file = stack!!.getCurrent().getSourceGalleryFile()
        val origWidth = file.getWidthCropRotated()
        val origHeight = file.getHeightCropRotated()

        val actualWidth = mediaView!!.getActualImageWidth()
        val actualHeight = mediaView!!.getActualImageHeight()

        var scale = min(actualWidth.toFloat() / origWidth.toFloat(), actualHeight.toFloat() / origHeight.toFloat())
        val nowWidth = (origWidth.toFloat() * scale).toInt()

        // int nowHeight = (int) ((float) origHeight * scale);
        val futureActualWidth = mediaView!!.getMeasuredWidth() - futureOffset - futureOffset
        val futureActualHeight: Int = mediaView!!.getMeasuredHeight() - getSectionBottomOffset(section) - getSectionTopOffset(section)

        scale = min(futureActualWidth.toFloat() / origWidth.toFloat(), futureActualHeight.toFloat() / origHeight.toFloat())
        val futureWidth = (origWidth.toFloat() * scale).toInt()

        // int futureHeight = (int) ((float) origHeight * scale);
        return futureOffset + max(0, ((nowWidth - futureWidth) / 2) - futureOffset)
    }

    private fun getSectionBottomOffset(section: Int): Int {
        if (mode != MODE_GALLERY) {
            return 0
        }
        val add = if (useEdgeToEdge() || this.isCurrentCamera) this.bottomInnerMargin else 0
        when (section) {
            SECTION_CAPTION -> {
                return 0 // Screen.dp(56f);
            }

            SECTION_QUALITY -> {
                return getSectionHeight(section) + Screen.dp(12f) + add
            }

            SECTION_FILTERS -> {
                return Screen.dp(220f) + add
            }

            SECTION_PAINT -> {
                return Screen.dp(136f) + add
            }

            SECTION_CROP -> {
                return Screen.dp(160f) + add
            }
        }
        return 0
    }

    private var fromTopOffset = 0
    private var toTopOffset = 0
    private var fromBottomOffset = 0
    private var toBottomOffset = 0
    private var fromHorOffset = 0
    private var toHorOffset = 0
    private var sectionChangeAnimator: FactorAnimator? = null
    private var editorView: EGLEditorView? = null

    private var currentSourceFile: ImageGalleryFile? = null
    private var sourceBitmap: Bitmap? = null

    private fun clearSourceBitmapReference() {
        if (currentSourceFile != null) {
            if (sourceBitmap != null) {
                ImageCache.instance().removeReference(currentTargetImageFile, sourceBitmap)
            }
            sourceBitmap = null
            currentSourceFile = null
        }
    }

    private fun setSourceBitmap(targetFile: ImageFile, file: ImageGalleryFile?, bitmap: Bitmap?) {
        clearSourceBitmapReference()
        this.currentSourceFile = file
        this.currentTargetImageFile = targetFile
        this.sourceBitmap = bitmap
        if (!TEST_LOAD_ORIGINAL && file != null && bitmap != null) {
            ImageCache.instance().addReference(targetFile, bitmap)
        }
    }

    private var currentTargetImageFile: ImageFile? = null
    private var currentFiltersState: FiltersState? = null
    private var currentTargetReceiver: ImageReceiver? = null
    private var currentBitmapWidth = 0
    private var currentBitmapHeight = 0

    private fun prepareBitmapForEditing(): Bitmap? {
        if (mediaView!!.getBaseReceiver() !is ImageReceiver) {
            return null
        }

        currentTargetReceiver = mediaView!!.getBaseReceiver() as ImageReceiver
        val bitmap = currentTargetReceiver!!.getCurrentBitmap()
        if (bitmap == null || bitmap.isRecycled()) {
            return null
        }

        currentBitmapWidth = bitmap.getWidth()
        currentBitmapHeight = bitmap.getHeight()

        return bitmap
    }

    private fun prepareFilters(): Boolean {
        val bitmap = prepareBitmapForEditing()
        if (bitmap == null) {
            return false
        }

        currentFiltersState = getFilterState(true)
        oldFiltersState = FiltersState(currentFiltersState)

        val galleryFile = stack!!.getCurrent().getSourceGalleryFile()

        if (currentSourceFile !== galleryFile) {
            cancelSourceLoad()

            val targetFile = stack!!.getCurrent().getTargetImageFile(false)
            setSourceBitmap(targetFile, galleryFile, if (!TEST_LOAD_ORIGINAL && currentTargetReceiver!!.getCurrentFile() === targetFile) bitmap else null)
            // image will load when filters will show completely in applyFiltersView() if sourceBitmap is not available now
        }

        return true
    }

    private var reference: WatcherReference? = null

    private fun cancelSourceLoad() {
        ImageLoader.instance().removeWatcher(reference)
    }

    private fun loadSourceAsync(sourceFile: ImageFile) {
        if (TEST_LOAD_ORIGINAL) {
            ImageReader.instance().post(Runnable {
                val path = sourceFile.getFilePath()
                // BitmapFactory.Options opts = new BitmapFactory.Options();
                // opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
                val bitmap = BitmapFactory.decodeFile(path) // ImageReader.decodeFile(path, opts);
                setEditBitmap(sourceFile, bitmap)
            })
        } else {
            if (reference == null) {
                reference = WatcherReference(this)
            }
            ImageLoader.instance().requestFile(sourceFile, reference)
        }
    }

    private fun applyFilteredBitmap(targetFile: ImageFile?, bitmap: Bitmap?) {
        currentTargetReceiver!!.setBundle(targetFile, bitmap, true)
    }

    private fun setEditBitmap(file: ImageFile?, bitmap: Bitmap?) {
        UI.post(Runnable {
            if (currentTargetImageFile === file) {
                setSourceBitmap(currentTargetImageFile!!, currentSourceFile, bitmap)
                editorView!!.reset(
                    currentSourceFile,
                    sourceBitmap!!.getWidth(),
                    sourceBitmap!!.getHeight(),
                    sourceBitmap,
                    currentFiltersState,
                    currentSourceFile!!.getPaintState()
                )
                editorView!!.setEditorVisible(true)
            }
        })
    }

    override fun imageLoaded(file: ImageFile?, successful: Boolean, bitmap: Bitmap?) {
        if (successful) {
            setEditBitmap(file, bitmap)
        }
    }

    override fun imageProgress(file: ImageFile?, progress: Float) {}

    private fun initSection(section: Int): Boolean {
        when (section) {
            SECTION_FILTERS -> {
                return this.isCurrentReady && prepareFilters()
            }

            SECTION_CROP -> {
                return this.isCurrentReady && prepareCrop()
            }

            SECTION_PAINT -> {
                return this.isCurrentReady && preparePaint()
            }

            SECTION_QUALITY -> {
                return this.isCurrentReady && prepareQuality()
            }
        }
        return true
    }

    private fun indexAfterMediaView(): Int {
        return contentView!!.indexOfChild(mediaView) + 1
    }

    private fun showEditorView() {
        if (editorView == null) {
            editorView = EGLEditorView(context())
            editorView!!.setLayoutParams(newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER))
        }

        if (sourceBitmap != null) {
            editorView!!.reset(
                currentSourceFile,
                sourceBitmap!!.getWidth(),
                sourceBitmap!!.getHeight(),
                sourceBitmap,
                currentFiltersState,
                currentSourceFile!!.getPaintState()
            )
        } else {
            editorView!!.setSizes(
                currentBitmapWidth,
                currentBitmapHeight,
                currentSourceFile!!.getVisualRotationWithCropRotation(),
                currentSourceFile!!.getCropState()
            )
        }

        mediaView!!.getBaseCell().addView(editorView)
        if (sourceBitmap != null) {
            editorView!!.setEditorVisible(true)
        } else {
            loadSourceAsync(currentTargetImageFile!!)
        }
    }

    private fun hideEditorView() {
        cancelSourceLoad()
        editorView!!.setEditorVisible(false)
        editorView!!.pause()
        editorView!!.setScaleX(1f)
        editorView!!.setScaleY(1f)
    }

    private fun hideQualityView() {
    }

    override fun shouldTouchOutside(x: Float, y: Float): Boolean {
        return pipFactor != 0f
    }

    fun onMediaItemClick(mediaView: MediaView?, x: Float, y: Float) {
        if (inPictureInPicture) {
            setInPictureInPicture(false)
            onClick(contentView)
            return
        }
        if (mode == MODE_GALLERY && currentSection != SECTION_CAPTION) {
            return
        }
        var clicked = stack!!.getCurrent().onClick(mediaView, x, y) || toggleHeaderVisibility()
        if (!clicked && mode == MODE_GALLERY) {
            clicked = toggleCheck()
        }
        if (clicked) {
            onClick(mediaView)
        }
    }

    private var oldFiltersState: FiltersState? = null
    private var hasEditedFilters = false

    private fun resetEditorState() {
        oldFiltersState = null
    }

    private fun hasEditorChanges(): Boolean {
        return hasEditedFilters && !oldFiltersState!!.compare(currentFiltersState)
    }

    override fun canApplyChanges(): Boolean {
        return !isUIBlocked
    }

    override fun onRequestRender(changedKey: Int) {
        hasEditedFilters = true
        if (editorView != null) {
            editorView!!.requestRenderByChange(changedKey == FiltersState.KEY_BLUR_TYPE)
        }
    }

    private fun prepareSectionToShow(section: Int) {
        when (section) {
            SECTION_FILTERS -> {
                if (filtersView == null) {
                    val manager = LinearLayoutManager(context(), RecyclerView.VERTICAL, false)
                    filtersAdapter = MediaFiltersAdapter(context(), manager)
                    filtersAdapter!!.setFilterState(currentFiltersState)
                    filtersAdapter!!.setCallback(this)

                    filtersView = MediaFiltersRecyclerView(context())
                    filtersView!!.setItemAnimator(null)
                    filtersView!!.setOverScrollMode(View.OVER_SCROLL_NEVER)
                    filtersView!!.addItemDecoration(object : RecyclerView.ItemDecoration() {
                        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                            val position = parent.getChildAdapterPosition(view)
                            outRect.set(
                                0,
                                if (position == 0) Screen.dp(16f) else 0,
                                0,
                                0
                            ) // position == filtersAdapter.getItemCount() - 1 ? Screen.dp(16f) : 0);
                        }
                    })
                    filtersView!!.setLayoutManager(manager)
                    filtersView!!.setAdapter(filtersAdapter)
                    filtersView!!.setBackgroundColor(Theme.getColor(ColorId.transparentEditor))
                    filtersView!!.setLayoutParams(newParams(ViewGroup.LayoutParams.MATCH_PARENT, getSectionHeight(SECTION_FILTERS), Gravity.BOTTOM))
                    filtersView!!.setTranslationY(getSectionHeight(SECTION_FILTERS).toFloat())
                    filtersView!!.setAlpha(0f)
                } else {
                    (filtersView!!.getLayoutManager() as LinearLayoutManager).scrollToPositionWithOffset(0, 0)
                    filtersAdapter!!.setFilterState(currentFiltersState)
                }
                bottomWrap!!.addView(filtersView)
            }

            SECTION_QUALITY -> {
                if (qualityControlWrap == null) {
                    val totalHeight: Int = getSectionHeight(SECTION_QUALITY)
                    val sliderHeight = Screen.dp(56f)
                    val hintHeight = Screen.dp(16f)
                    val infoHeight = Screen.dp(18f)

                    qualityControlWrap = FrameLayoutFix(context())
                    qualityControlWrap!!.setBackgroundColor(Theme.getColor(ColorId.transparentEditor))
                    qualityControlWrap!!.setAlpha(0f)
                    qualityControlWrap!!.setLayoutParams(newParams(ViewGroup.LayoutParams.MATCH_PARENT, totalHeight, Gravity.BOTTOM))

                    qualitySlider = SliderView(context())
                    qualitySlider!!.setListener(object : SliderView.Listener {
                        var targetIndex: Int = -1

                        override fun onSetStateChanged(view: SliderView?, isSetting: Boolean) {
                            if (!isSetting) {
                                val value = targetIndex.toFloat() * (1f / (videoLimits!!.size - 1).toFloat())
                                qualitySlider!!.animateValue(value)
                            }
                        }

                        override fun onValueChanged(view: SliderView?, factor: Float) {
                            val newIndex = Math.round(factor * (videoLimits!!.size - 1))
                            if (targetIndex != newIndex) {
                                targetIndex = newIndex
                                currentVideoLimit = videoLimits!!.get(targetIndex)
                                if (targetIndex == videoLimits!!.size - 1 && (prevVideoLimit!!.size.isUnlimited || (!currentVideoLimit!!.size.isUnlimited && currentVideoLimit!!.size.majorSize < prevVideoLimit!!.size.majorSize))) {
                                    currentVideoLimit = VideoLimit(prevVideoLimit!!)
                                }
                                updateQualityInfo()
                            }
                        }

                        override fun allowSliderChanges(view: SliderView?): Boolean {
                            return true
                        }
                    })
                    qualitySlider!!.setLayoutParams(newParams(ViewGroup.LayoutParams.MATCH_PARENT, sliderHeight, Gravity.BOTTOM, 0, 0, 0, infoHeight))
                    qualitySlider!!.setAnchorMode(SliderView.ANCHOR_MODE_START)
                    qualitySlider!!.setAddPaddingLeft(Screen.dp(18f))
                    qualitySlider!!.setAddPaddingRight(Screen.dp(18f))
                    qualitySlider!!.setColorId(ColorId.white, false)
                    qualityControlWrap!!.addView(qualitySlider)

                    var textView = Views.newTextView(context(), 14f, Theme.getColor(ColorId.white), Gravity.LEFT, Views.TEXT_FLAG_SINGLE_LINE)
                    textView.setLayoutParams(
                        newParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            Gravity.LEFT or Gravity.TOP,
                            Screen.dp(15f),
                            Screen.dp(10f),
                            Screen.dp(15f),
                            0
                        )
                    )
                    textView.setText(Lang.getString(R.string.QualityWorse))
                    qualityControlWrap!!.addView(textView)
                    textView = Views.newTextView(context(), 14f, Theme.getColor(ColorId.white), Gravity.RIGHT, Views.TEXT_FLAG_SINGLE_LINE)
                    textView.setLayoutParams(
                        newParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            Gravity.RIGHT or Gravity.TOP,
                            Screen.dp(15f),
                            Screen.dp(10f),
                            Screen.dp(15f),
                            0
                        )
                    )
                    textView.setText(Lang.getString(R.string.QualityBetter))
                    qualityControlWrap!!.addView(textView)

                    qualityInfo = Views.newTextView(context(), 15f, Theme.getColor(ColorId.white), Gravity.CENTER, Views.TEXT_FLAG_SINGLE_LINE)
                    qualityInfo!!.setLayoutParams(
                        newParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM,
                            0,
                            0,
                            0,
                            Screen.dp(8f)
                        )
                    )
                    qualityControlWrap!!.addView(qualityInfo)
                }
                bottomWrap!!.addView(qualityControlWrap)
                updateQualitySlider()
            }

            SECTION_CROP -> {
                if (cropControlsWrap == null) {
                    val inProfilePhotoEditMode = inProfilePhotoEditMode()
                    var params: FrameLayout.LayoutParams?

                    params = newParams(ViewGroup.LayoutParams.MATCH_PARENT, getSectionHeight(SECTION_CROP), Gravity.BOTTOM)

                    cropControlsWrap = FrameLayoutFix(context())
                    cropControlsWrap!!.setPadding(0, Screen.dp(CROP_PADDING_TOP), 0, 0)
                    cropControlsWrap!!.setBackgroundColor(Theme.getColor(ColorId.transparentEditor))
                    cropControlsWrap!!.setLayoutParams(params)
                    cropControlsWrap!!.setAlpha(0f)

                    rotateButton = EditButton(context())
                    rotateButton!!.setId(R.id.btn_rotate)
                    rotateButton!!.setOnClickListener(this)
                    rotateButton!!.setIcon(R.drawable.baseline_rotate_90_degrees_ccw_24, false, false)
                    rotateButton!!.setLayoutParams(newParams(Screen.dp(56f), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.RIGHT))
                    cropControlsWrap!!.addView(rotateButton)

                    proportionButton = EditButton(context())
                    proportionButton!!.setId(R.id.btn_proportion)
                    proportionButton!!.setOnClickListener(this)
                    proportionButton!!.setIcon(R.drawable.baseline_image_aspect_ratio_24, false, false)
                    proportionButton!!.setLayoutParams(newParams(Screen.dp(56f), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.LEFT))
                    if (!inProfilePhotoEditMode) {
                        cropControlsWrap!!.addView(proportionButton)
                    }

                    mirrorButton = EditButton(context())
                    mirrorButton!!.setId(R.id.btn_mirrorHorizontal)
                    mirrorButton!!.setOnClickListener(this)
                    mirrorButton!!.setIcon(R.drawable.dot_baseline_flip_horizontal_24, false, false)
                    mirrorButton!!.setLayoutParams(
                        newParams(
                            Screen.dp(56f),
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            Gravity.LEFT,
                            if (inProfilePhotoEditMode) 0 else Screen.dp(56f),
                            0,
                            0,
                            0
                        )
                    )
                    cropControlsWrap!!.addView(mirrorButton)

                    params = newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    params.leftMargin = Screen.dp(if (inProfilePhotoEditMode) 56f else (56f * 2))
                    params.rightMargin = Screen.dp(56f)

                    rotationControlView = RotationControlView(context())
                    rotationControlView!!.setCallback(this)
                    rotationControlView!!.setLayoutParams(params)
                    cropControlsWrap!!.addView(rotationControlView)

                    prepareCropLayout()
                }

                cropLayout!!.setAlpha(0f)
                prepareCropState()

                bottomWrap!!.addView(cropControlsWrap)
                contentView!!.addView(cropLayout, indexAfterMediaView())
            }

            SECTION_PAINT -> {
                if (paintControlsWrap == null) {
                    var params: FrameLayout.LayoutParams?

                    // FIXME use less height
                    params = newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.BOTTOM)

                    paintControlsWrap = FrameLayoutFix(context())
                    paintControlsWrap!!.setAlpha(0f)
                    paintControlsWrap!!.setLayoutParams(params)

                    val margin = Screen.dp(56f)
                    val padding = Screen.dp(18f)

                    params = newParams(ViewGroup.LayoutParams.MATCH_PARENT, Screen.dp(12f) + padding * 2, Gravity.BOTTOM)
                    val backgroundView = View(context())
                    backgroundView.setBackgroundColor(Theme.getColor(ColorId.transparentEditor))
                    backgroundView.setLayoutParams(params)
                    paintControlsWrap!!.addView(backgroundView)

                    params = newParams(Screen.dp(56f), Screen.dp(48f), Gravity.LEFT or Gravity.BOTTOM)
                    undoButton = ImageView(context())
                    undoButton!!.setId(R.id.paint_undo)
                    undoButton!!.setOnClickListener(this)
                    undoButton!!.setOnLongClickListener(OnLongClickListener { v: View? ->
                        if (paintView != null && !paintView!!.getContentWrap().isBusy()) {
                            showOptions(
                                null,
                                intArrayOf(R.id.paint_clear, R.id.btn_cancel),
                                arrayOf<String?>(Lang.getString(R.string.ClearDrawing), Lang.getString(R.string.Cancel)),
                                intArrayOf(OptionColor.RED, OptionColor.NORMAL),
                                intArrayOf(R.drawable.baseline_delete_24, R.drawable.baseline_cancel_24),
                                OptionDelegate { itemView: View?, id: Int ->
                                    if (id == R.id.paint_clear) {
                                        undoAllPaintActions()
                                    }
                                    true
                                },
                                this@MediaViewController.forcedTheme
                            )
                            true
                        } else {
                            false
                        }
                    })
                    undoButton!!.setScaleType(ImageView.ScaleType.CENTER)
                    undoButton!!.setImageResource(R.drawable.baseline_undo_24)
                    undoButton!!.setColorFilter(-0x1)
                    Views.setClickable(undoButton)
                    undoButton!!.setBackgroundResource(R.drawable.bg_btn_header_light)
                    undoButton!!.setLayoutParams(params)
                    paintControlsWrap!!.addView(undoButton)

                    params = newParams(Screen.dp(56f), Screen.dp(48f), Gravity.RIGHT or Gravity.BOTTOM)
                    paintTypeButton = EditButton(context())
                    paintTypeButton!!.setId(R.id.btn_paintType)
                    paintTypeButton!!.setUseFastAnimations()
                    paintTypeButton!!.setOnClickListener(this)
                    paintTypeButton!!.setIcon(getIconForPaintType(PaintMode.PATH), false, false)
                    paintTypeButton!!.setLayoutParams(params)
                    paintControlsWrap!!.addView(paintTypeButton)

                    params = newParams(ViewGroup.LayoutParams.MATCH_PARENT, Screen.dp(12f) + padding * 2, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
                    params.rightMargin = margin - padding
                    params.leftMargin = params.rightMargin
                    colorPickerView = ColorPickerView(context())
                    colorPickerView!!.setToneEventListener(object : ToneEventListener {
                        override fun onLongTapStateChanged(v: ColorPickerView?, inLongTap: Boolean) {
                            showDefaultBrushHint()
                        }

                        override fun onTonePicking(v: ColorPickerView?, pickingTone: Boolean) {
                            if (pickingTone) {
                                Settings.instance().markTutorialAsComplete(Settings.TUTORIAL_BRUSH_COLOR_TONE)
                            }
                        }
                    })
                    colorPickerView!!.setPadding(padding, padding, padding, padding)
                    colorPickerView!!.setLayoutParams(params)
                    paintControlsWrap!!.addView(colorPickerView)

                    params = newParams(ViewGroup.LayoutParams.MATCH_PARENT, Screen.dp(220f), Gravity.BOTTOM)
                    params.bottomMargin = Screen.dp(12f) + padding * 2

                    colorToneView = ColorToneView(context())
                    colorToneView!!.setAlpha(0f)
                    colorToneView!!.setLayoutParams(params)
                    paintControlsWrap!!.addView(colorToneView)

                    colorToneShadow = ShadowView(context())
                    colorToneShadow!!.setSimpleTopShadow(true)
                    colorToneShadow!!.setAlpha(0f)
                    params = newParams(ViewGroup.LayoutParams.MATCH_PARENT, colorToneShadow!!.getLayoutParams().height, Gravity.BOTTOM)
                    params.bottomMargin = Screen.dp(220f) + Screen.dp(12f) + padding * 2
                    colorToneShadow!!.setLayoutParams(params)
                    paintControlsWrap!!.addView(colorToneShadow)

                    val addY = Screen.dp(78f)
                    params = newParams(ViewGroup.LayoutParams.MATCH_PARENT, Screen.dp(220f) + padding + addY, Gravity.BOTTOM)
                    params.bottomMargin = Screen.dp(12f) + padding

                    val colorDirectionView = ColorDirectionView(context())
                    colorDirectionView.setPadding(0, addY, 0, 0)
                    colorDirectionView.setLayoutParams(params)
                    paintControlsWrap!!.addView(colorDirectionView)

                    params = newParams(Screen.dp(48f), Screen.dp(48f), Gravity.LEFT or Gravity.BOTTOM)
                    params.leftMargin = margin - Screen.dp(48f) / 2
                    params.bottomMargin = (Screen.dp(12f) + padding * 2) / 2 - Screen.dp(48f) / 2
                    colorPreviewView = ColorPreviewView(context())
                    colorPreviewView!!.setColorChangeListener(ColorChangeListener { v: ColorPreviewView?, color: Int ->
                        showDefaultBrushHint()
                    })
                    colorPreviewView!!.setPositionChangeListener(PositionChangeListener {
                        if (this.brushToneHint != null) {
                            this.brushToneHint!!.reposition()
                        }
                    })
                    colorPreviewView!!.setBrushChangeListener(this)
                    colorPreviewView!!.setTone(colorToneView)
                    colorPreviewView!!.setDirection(colorDirectionView)
                    colorPreviewView!!.setLayoutParams(params)
                    paintControlsWrap!!.addView(colorPreviewView)

                    colorToneView!!.setPreview(colorPreviewView)
                    colorPickerView!!.setPreview(colorPreviewView)
                    colorPickerView!!.setTone(colorToneView, colorToneShadow)
                    colorPickerView!!.setDirection(colorDirectionView)
                }

                setPaintType(PaintMode.restore(), false)
                checkCanUndo()
                colorPreviewView!!.reset(true)

                bottomWrap!!.addView(paintControlsWrap)
            }

            SECTION_CAPTION -> {}
        }
        if (section != SECTION_CAPTION) {
            val item = stack!!.getCurrent()
            if (item != null && item.isVideo()) {
                val cellView = mediaView!!.findCellForItem(item)
                if (cellView != null) {
                    cellView.stopPlaying()
                }
            }
        }
    }

    private fun getFilterState(createIfEmpty: Boolean): FiltersState? {
        val item = stack!!.getCurrent()
        var state = item.getFiltersState()
        if (state == null && createIfEmpty) {
            state = FiltersState()
        }
        return state
    }

    // Crop
    private var inCrop = false

    private var cropLayout: CropLayout? = null
    private var cropAreaView: CropAreaView? = null
    private var cropTargetView: CropTargetView? = null

    private val cropAnimator = BoolAnimator(ANIMATOR_CROP, this, DECELERATE_INTERPOLATOR, 140L)

    private var sectionAfterCrop = -1
    private var sectionAfterCropReady = false

    private fun onCloseCrop() {
        if (sectionAfterCrop != -1) {
            if (resettingCrop) {
                onCropResetComplete()
            }
            sectionAfterCropReady = true
            changeSectionImpl(sectionAfterCrop)
            sectionAfterCrop = -1
        }
    }

    private fun closeCropAsync(futureSection: Int): Boolean {
        if (sectionAfterCropReady) {
            sectionAfterCropReady = false
            return false
        }
        if (sectionAfterCrop != -1) {
            return true
        }
        sectionAfterCrop = futureSection
        if (!resettingCrop) {
            setInCrop(false)
        }
        return true
    }

    private fun setInCrop(inCrop: Boolean) {
        if (this.inCrop != inCrop) {
            this.inCrop = inCrop
            if (!inCrop) {
                prepareSectionToHide(SECTION_CROP)
                mediaView!!.setVisibility(View.VISIBLE)
            } else if (inProfilePhotoEditMode()) {
                setCropProportion(1, 1, false)
            }
            cropAnimator.setDuration(if (inCrop) (if (currentCropState!!.isEmpty()) CROP_OUT_DURATION else CROP_IN_DURATION) else 120L)
            cropAnimator.setValue(inCrop, true)
        }
    }

    private fun setCropFactor(factor: Float) {
        cropLayout!!.setAlpha(factor)
    }

    private fun applyCropMode(inCrop: Boolean) {
        if (inCrop) {
            mediaView!!.setVisibility(View.INVISIBLE)
        } else {
            onCloseCrop()
        }
    }

    override fun allowPreciseRotation(view: RotationControlView?): Boolean {
        if ((sectionChangeAnimator == null || !sectionChangeAnimator!!.isAnimating()) && inCrop && (cropAreaView!!.canRotate())) {
            val item = stack!!.getCurrent()
            if (item.isVideoOrGif()) {
                context().tooltipManager().builder(view).icon(R.drawable.baseline_warning_24)
                    .show(tdlib, R.string.MediaFeatureUnavailable)
                    .hideDelayed()
                return false
            }
            return true
        }
        return false
    }

    private var imageRotateFactor = 0f
    private var imageRotateAnimator: FactorAnimator? = null

    private fun setImageRotateFactor(factor: Float) {
        if (this.imageRotateFactor != factor) {
            this.imageRotateFactor = factor
            val rotation = rotatingByDegrees.toFloat() * factor

            val scaleX: Float
            val scaleY: Float

            if (U.isRotated(rotatingByDegrees)) {
                val fromWidth = cropTargetView!!.getMeasuredWidth()
                val fromHeight = cropTargetView!!.getMeasuredHeight()

                val params = cropTargetView!!.getLayoutParams() as FrameLayout.LayoutParams

                val targetWidth = cropTargetView!!.getTargetHeight()
                val targetHeight = cropTargetView!!.getTargetWidth()

                val availWidth = cropLayout!!.getMeasuredWidth() - params.leftMargin - params.rightMargin
                val availHeight = cropLayout!!.getMeasuredHeight() - params.topMargin - params.bottomMargin

                val ratio = min(availWidth.toFloat() / targetWidth.toFloat(), availHeight.toFloat() / targetHeight.toFloat())

                val toWidth = (targetWidth.toFloat() * ratio).toInt()
                val toHeight = (targetHeight.toFloat() * ratio).toInt()

                val width = fromWidth + ((toHeight - fromWidth).toFloat() * factor).toInt()
                val height = fromHeight + ((toWidth - fromHeight).toFloat() * factor).toInt()

                scaleX = width.toFloat() / fromWidth.toFloat()
                scaleY = height.toFloat() / fromHeight.toFloat()
            } else {
                scaleX = 1f
                scaleY = 1f
            }

            cropTargetView!!.setBaseRotation(rotation)
            cropTargetView!!.setBaseScale(scaleX, scaleY)

            cropAreaView!!.setRotation(rotation)
            cropAreaView!!.setScaleX(scaleX)
            cropAreaView!!.setScaleY(scaleY)
        }
    }

    private var rotatingByDegrees = 0

    private fun applyImageRotation() {
        if (rotatingByDegrees == 0) {
            return
        }

        cropTargetView!!.setBaseRotation(0f)
        cropTargetView!!.setBaseScale(1f, 1f)
        cropTargetView!!.rotateTargetBy(rotatingByDegrees)

        cropAreaView!!.setRotation(0f)
        cropAreaView!!.setScaleX(1f)
        cropAreaView!!.setScaleY(1f)
        cropAreaView!!.rotateValues(rotatingByDegrees)

        resetMediaPaddings(SECTION_CROP)

        rotatingByDegrees = 0
    }

    private fun cancelImageRotation() {
        if (imageRotateAnimator != null) {
            imageRotateAnimator!!.cancel()
        }
        imageRotateFactor = 0f
    }

    private fun cropRotateByDegrees(degrees: Int) {
        if (imageRotateAnimator == null) {
            imageRotateAnimator = FactorAnimator(ANIMATOR_IMAGE_ROTATE, this, DECELERATE_INTERPOLATOR, 180L)
        } else if (!imageRotateAnimator!!.isAnimating()) {
            imageRotateAnimator!!.forceFactor(0f)
            imageRotateFactor = 0f
        } else {
            return
        }
        if (degrees != 0) {
            val newPostRotate = currentCropState!!.rotateBy(degrees)
            this.rotatingByDegrees = degrees
            imageRotateAnimator!!.animateTo(1f)
        }
    }

    override fun onPreciseActiveStateChanged(isActive: Boolean) {
        cropAreaView!!.setMode(if (isActive) CropAreaView.MODE_PRECISE else CropAreaView.MODE_NONE, false)
    }

    override fun onPreciseActiveFactorChanged(activeFactor: Float) {
        cropAreaView!!.forceActiveFactor(activeFactor)
    }

    override fun onPreciseRotationChanged(newValue: Float) {
        currentCropState!!.setDegreesAroundCenter(newValue)
        cropTargetView!!.setDegreesAroundCenter(newValue)
    }

    private val imageFlipAnimatorHorizontally = BoolAnimator(ANIMATOR_IMAGE_FLIP_HORIZONTALLY, this, DECELERATE_INTERPOLATOR, 250L)
    private val imageFlipAnimatorVertically = BoolAnimator(ANIMATOR_IMAGE_FLIP_VERTICALLY, this, DECELERATE_INTERPOLATOR, 250L)

    private fun imageMirrorAnimate(mirrorFlag: Int, needMirror: Boolean, animated: Boolean): Boolean {
        if (mirrorFlag == 0) {
            return false
        }
        val oldFlags = currentCropState!!.getFlags()
        val newFlags = setFlag(oldFlags, mirrorFlag, needMirror)
        if (oldFlags == newFlags) {
            return false
        }

        val animator = if (mirrorFlag == CropState.Flags.MIRROR_HORIZONTALLY) imageFlipAnimatorHorizontally else imageFlipAnimatorVertically

        if (!animator.isAnimating()) {
            animator.setValue(!needMirror, false)
        } else {
            return false
        }
        currentCropState!!.setFlags(newFlags)
        animator.setValue(needMirror, animated)
        return true
    }

    private fun setImageMirrorFactors() {
        cropTargetView!!.setMirrorFactors(imageFlipAnimatorHorizontally.getFloatValue(), imageFlipAnimatorVertically.getFloatValue())
    }

    private fun applyImageMirror() {
        cropTargetView!!.setMirrorFactors(
            (if (currentCropState!!.hasFlag(CropState.Flags.MIRROR_HORIZONTALLY)) 1 else 0).toFloat(),
            (if (currentCropState!!.hasFlag(CropState.Flags.MIRROR_VERTICALLY)) 1 else 0).toFloat()
        )
    }

    private fun cancelImageMirrorAnimations() {
        imageFlipAnimatorHorizontally.cancel()
        imageFlipAnimatorVertically.cancel()
    }

    /**/
    private var currentCropState: CropState? = null
    private var oldCropState: CropState? = null

    private var cropBitmap: Bitmap? = null
    private var cropRotation = 0

    private fun obtainCropState(createIfEmpty: Boolean): CropState? {
        val item = stack!!.getCurrent()
        var cropState = item.getCropState()
        if (cropState == null && createIfEmpty) {
            cropState = CropState()
        }
        return cropState
    }

    private fun prepareCropLayout() {
        if (cropLayout == null) {
            cropLayout = CropLayout(context())
            cropLayout!!.setLayoutParams(newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

            val params = newParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL or Gravity.TOP)
            params.topMargin = getSectionTopOffset(SECTION_CROP)
            params.bottomMargin = getSectionBottomOffset(SECTION_CROP)
            params.rightMargin = Screen.dp(CROP_PADDING_HORIZONTAL)
            params.leftMargin = params.rightMargin

            cropTargetView = CropTargetView(context())
            cropTargetView!!.setLayoutParams(params)
            cropLayout!!.addView(cropTargetView)

            cropAreaView = CropAreaView(context())
            cropAreaView!!.setProfilePhotoMode(inProfilePhotoEditMode())
            cropAreaView!!.setRectChangeListener(RectChangeListener { left: Double, top: Double, right: Double, bottom: Double ->
                if (inCrop) {
                    currentCropState!!.setRect(left, top, right, bottom)
                }
            })
            cropAreaView!!.setNormalizeListener(object : NormalizeListener {
                override fun onCropNormalization(factor: Float) {
                    setCropResetFactor(factor)
                }

                override fun onCropNormalizationComplete() {
                    onCropResetComplete()
                }
            })
            cropAreaView!!.setRotateModeChangeListener(RotateModeChangeListener { rotateInternally: Boolean ->
                cropTargetView!!.setRotateInternally(
                    rotateInternally
                )
            })
            cropAreaView!!.setOffsets(params.leftMargin, params.topMargin, params.rightMargin, params.bottomMargin)
            cropAreaView!!.setLayoutParams(newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            cropLayout!!.addView(cropAreaView)
        }
    }

    private fun prepareCropState() {
        proportionButton!!.setActive(false, false)
        val cropRotation = modulo(this.cropRotation + (if (oldCropState != null) oldCropState!!.getRotateBy() else 0), 360)
        cropTargetView!!.resetState(cropBitmap, cropRotation, currentCropState!!.getDegreesAroundCenter(), currentPaintState)
        cropTargetView!!.setMirrorFactors(
            if (currentCropState!!.hasFlag(CropState.Flags.MIRROR_HORIZONTALLY)) 1f else 0f,
            if (currentCropState!!.hasFlag(CropState.Flags.MIRROR_VERTICALLY)) 1f else 0f
        )
        mirrorButton!!.setActive(currentCropState!!.needMirror(), false)
        rotationControlView!!.reset(currentCropState!!.getDegreesAroundCenter(), false)
        cropAreaView!!.resetProportion()
        cropAreaView!!.resetState(
            U.getWidth(cropBitmap, cropRotation),
            U.getHeight(cropBitmap, cropRotation),
            currentCropState!!.getLeft(),
            currentCropState!!.getTop(),
            currentCropState!!.getRight(),
            currentCropState!!.getBottom(),
            false
        )
    }

    private fun prepareCrop(): Boolean {
        if (!Config.CROP_ENABLED) {
            return false
        }
        val item = stack!!.getCurrent()
        val receiver = mediaView!!.getBaseCell().getImageReceiver()
        if ((!Config.MODERN_VIDEO_TRANSCODING_ENABLED && item.isVideoOrGif()) || receiver == null) {
            UI.showToast(R.string.MediaFeatureUnavailable, Toast.LENGTH_SHORT)
            return false
        }

        cropBitmap = receiver.getCurrentBitmap()
        if (cropBitmap == null || cropBitmap!!.isRecycled()) {
            return false
        }
        if (item.isVideoOrGif()) {
            cropRotation = 0
        } else {
            cropRotation = item.getSourceGalleryFile().getRotation()
        }
        currentCropState = obtainCropState(true)
        currentPaintState = obtainPaintState(false)
        oldCropState = CropState(currentCropState)
        return true
    }

    private fun hasCropChanges(): Boolean {
        return !oldCropState!!.equalsTo(currentCropState)
    }

    private fun resetCropState() {
        oldCropState = null
    }

    private fun setCropProportion(big: Int, small: Int, animated: Boolean) {
        cropAreaView!!.setFixedProportion(big, small, animated)
        proportionButton!!.setActive(big != 0 && small != 0, animated)
    }

    val mirrorHorizontallyFlag: Int
        get() {
            val item = if (stack != null) stack!!.getCurrent() else null
            if (item == null) {
                return 0
            }
            if (item.isVideoOrGif()) {
                return if (U.isRotated(item.getCropRotateBy())) CropState.Flags.MIRROR_VERTICALLY else CropState.Flags.MIRROR_HORIZONTALLY
            } else {
                return if (item.isRotated()) CropState.Flags.MIRROR_VERTICALLY else CropState.Flags.MIRROR_HORIZONTALLY
            }
        }

    private fun setMirrorHorizontally(newValue: Boolean) {
        if (imageMirrorAnimate(this.mirrorHorizontallyFlag, newValue, true)) {
            mirrorButton!!.setActive(newValue, true)
        }
    }

    private var cropStartDegrees = 0f
    private var cropEndDegrees = 0f
    private var resetCropDegrees = false

    private var resettingCrop = false
    private var closeCropAfterReset = false

    private fun resetCrop(zero: Boolean) {
        if (resettingCrop) { // Awaiting previous animation to complete
            return
        }

        cancelImageRotation()
        cancelImageMirrorAnimations()

        cropStartDegrees = currentCropState!!.getDegreesAroundCenter()
        cropEndDegrees = if (zero || oldCropState == null) 0f else oldCropState!!.getDegreesAroundCenter()
        resetCropDegrees = (cropEndDegrees - cropStartDegrees) != 0f

        rotatingByDegrees = (if (zero || oldCropState == null) 0 else oldCropState!!.getRotateBy()) - currentCropState!!.getRotateBy()
        if (rotatingByDegrees < -180) {
            rotatingByDegrees = 360 + rotatingByDegrees
        }
        currentCropState!!.rotateBy(rotatingByDegrees)
        proportionButton!!.setActive(false, true)
        resettingCrop = resetCropDegrees || rotatingByDegrees != 0
        closeCropAfterReset = !zero
        setMirrorHorizontally(!zero && oldCropState != null && oldCropState!!.hasFlag(this.mirrorHorizontallyFlag))
        if (zero || oldCropState == null || oldCropState!!.isEmpty()) {
            if (cropAreaView!!.resetArea(resettingCrop, !zero)) {
                resettingCrop = true
            }
        } else {
            if (cropAreaView!!.animateArea(
                    oldCropState!!.getLeft(),
                    oldCropState!!.getTop(),
                    oldCropState!!.getRight(),
                    oldCropState!!.getBottom(),
                    true,
                    true
                )
            ) {
                resettingCrop = true
            }
        }
        closeCropAfterReset = closeCropAfterReset && resettingCrop
    }

    private fun setCropResetFactor(factor: Float) {
        if (resetCropDegrees) {
            rotationControlView!!.reset(cropStartDegrees + (cropEndDegrees - cropStartDegrees) * factor, true)
        }
        if (rotatingByDegrees != 0) {
            setImageRotateFactor(factor)
        }
    }

    private fun onCropResetComplete() {
        resetCropDegrees = false
        resettingCrop = false
        applyImageRotation()
        if (closeCropAfterReset) {
            closeCropAfterReset = false
            setInCrop(false)
        }
    }

    // Video rotation
    private var rotateAnimator: FactorAnimator? = null
    private fun rotateBy90Degrees() {
        if (!mediaView!!.isBaseVisible() || !mediaView!!.isStill()) {
            return
        }

        val item = stack!!.getCurrent()
        if (item == null) {
            return
        }

        if (Config.MODERN_VIDEO_TRANSCODING_ENABLED || !item.isVideoOrGif()) {
            cropRotateByDegrees(-90)
            return
        }

        if (!Config.USE_VIDEO_COMPRESSION) {
            UI.showApiLevelWarning(Build.VERSION_CODES.JELLY_BEAN_MR2)
            return
        }

        if (rotateAnimator == null) {
            rotateAnimator = FactorAnimator(ANIMATOR_ROTATION, this, DECELERATE_INTERPOLATOR, 180L)
        } else if (rotateAnimator!!.isAnimating()) {
            // Wait until previous video rotation will be applied
            return
        }

        val cellView = mediaView!!.getBaseCell()
        item.postRotateBy90Degrees()
        cellView.checkPostRotation(true)
        if (currentSection == SECTION_QUALITY) {
            updateQualityInfo()
        }
    }

    // Paint
    private fun hasPaintChanges(): Boolean {
        return !oldPaintState!!.compare(currentPaintState)
    }

    private var paintingItem: MediaItem? = null
    private var paintingWidth = 0
    private var paintingHeight = 0

    private fun preparePaint(): Boolean {
        val item = stack!!.getCurrent()
        val receiver = mediaView!!.getBaseCell().getImageReceiver()
        if (item.isVideoOrGif() || receiver == null) {
            UI.showToast(R.string.MediaFeatureUnavailable, Toast.LENGTH_SHORT)
            return false
        }

        val bitmap = prepareBitmapForEditing()
        if (bitmap == null) {
            return false
        }

        currentPaintState = obtainPaintState(true)
        currentPaintState!!.addUndoStateListener(this)
        oldPaintState = PaintState(currentPaintState)

        currentCropState = obtainCropState(false)

        paintingItem = stack!!.getCurrent()
        paintingWidth = bitmap.getWidth()
        paintingHeight = bitmap.getHeight()

        return true
    }

    private var currentPaintState: PaintState? = null
    private var oldPaintState: PaintState? = null

    private fun obtainPaintState(createIfEmpty: Boolean): PaintState? {
        val item = stack!!.getCurrent()
        var paintState = item.getPaintState()
        if (paintState == null && createIfEmpty) {
            paintState = PaintState()
        }
        return paintState
    }

    private var paintSectionFactor = 0f

    private fun setPaintSectionFactor(factor: Float) {
        if (Config.MASKS_TEXTS_AVAILABLE && this.paintSectionFactor != factor) {
            this.paintSectionFactor = factor
            cropOrStickerButton!!.setSecondFactor(factor)
            adjustOrTextButton!!.setSecondFactor(factor)
            if (stopwatchButton != null) {
                val alpha = 1f - factor
                stopwatchButton!!.setAlpha(alpha)
                val scale = .6f + .4f * alpha
                stopwatchButton!!.setScaleX(scale)
                stopwatchButton!!.setScaleY(scale)
                editButtons!!.setTranslationX(Views.getParamsWidth(stopwatchButton) / 2 * factor)
            }
        }
    }

    private var paintView: EGLEditorView? = null

    private fun showPaintEditor() {
        if (paintView == null) {
            paintView = EGLEditorView(context())
            paintView!!.setLayoutParams(newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER))
            applyPaintingMode(paintType)
        }

        paintView!!.getContentWrap().setBrushParameters(colorPreviewView!!.getBrushColor(), colorPreviewView!!.getBrushRadius(this.brushScale))

        paintView!!.reset(paintingItem!!.getSourceGalleryFile(), paintingWidth, paintingHeight, null, null, currentPaintState)
        val changed = paintingItem!!.setPaintState(null, false)

        mediaView!!.getBaseCell().addView(paintView)
        paintView!!.setEditorVisible(true)

        paintView!!.getContentWrap().setDrawingListener(DrawingListener { v: View?, mode: Int ->
            showDefaultBrushHint()
        })

        if (changed) {
            mediaView!!.getBaseReceiver().invalidate()
        }

        showDefaultBrushHint()
    }

    fun showDefaultBrushHint() {
        if (paintView != null && paintView!!.isEditorVisible() && !paintView!!.getContentWrap().hasEffectiveDrawing()) {
            val text: CharSequence?
            var textFlags = 0
            if (colorPickerView!!.isInLongTap()) {
                val hue = colorPreviewView!!.getHue()
                val hsv = colorPreviewView!!.getHsv()
                val color = colorPreviewView!!.getBrushColor()
                text =
                    Strings.getColorRepresentation(Settings.COLOR_FORMAT_HEX, hue, hsv[1], hsv[2], color, true) + "\n" +
                            Strings.getColorRepresentation(Settings.COLOR_FORMAT_RGB, hue, hsv[1], hsv[2], color, true) + "\n" +
                            Strings.getColorRepresentation(Settings.COLOR_FORMAT_HSL, hue, hsv[1], hsv[2], color, true) + "\n" +
                            Lang.getStringBold(R.string.BrushSize, U.formatFloat(colorPreviewView!!.getBrushRadius(1f), false))
                textFlags = Text.FLAG_ALIGN_CENTER
            } else if (Settings.instance().needTutorial(Settings.TUTORIAL_BRUSH_COLOR_TONE)) {
                text = Lang.getString(R.string.HoldToTone)
            } else {
                text = null
            }
            if (!isEmpty(text)) {
                if (brushToneHint != null) {
                    if (text != brushToneHint!!.contentText) {
                        brushToneHint!!.reset(context().tooltipManager().newContent(tdlib, text!!, textFlags), 0)
                    }
                    brushToneHint!!.show()
                } else {
                    brushToneHint = context().tooltipManager().builder(colorPreviewView).preventHideOnTouch(true).color(
                        context().tooltipManager().overrideColorProvider(
                            this@MediaViewController.forcedTheme
                        )
                    ).show(context().tooltipManager().newContent(tdlib, text!!, textFlags))
                }
                return
            }
        }
        if (brushToneHint != null) {
            brushToneHint!!.hideNow()
        }
    }

    private var brushToneHint: TooltipInfo? = null

    private fun hidePaintEditor() {
        paintView!!.setEditorVisible(false)
        if (paintingItem!!.setPaintState(paintingItem!!.getPaintState(), false)) {
            mediaView!!.getBaseReceiver().invalidate()
        }
        paintView!!.pause()
        paintView!!.setScaleX(1f)
        paintView!!.setScaleY(1f)
        showDefaultBrushHint()
    }

    private var paintType = 0

    private fun setPaintType(mode: Int, changeByUser: Boolean) {
        if (this.paintType != mode) {
            this.paintType = mode
            if (changeByUser) {
                PaintMode.save(mode)
            }
            paintTypeButton!!.setIcon(getIconForPaintType(mode), changeByUser, false)
            applyPaintingMode(mode)
        }
    }

    private fun showPaintTypes() {
        // TODO design

        val canErase = currentPaintState != null && !currentPaintState!!.isEmpty()

        val ids = IntList(5)
        val icons = IntList(5)
        val strings = StringList(5)

        ids.append(R.id.paint_mode_path)
        icons.append(R.drawable.baseline_adjust_24)
        strings.append(R.string.PaintModeDoodle)

        ids.append(R.id.paint_mode_arrow)
        icons.append(R.drawable.baseline_arrow_upward_24)
        strings.append(R.string.PaintModeArrow)

        ids.append(R.id.paint_mode_rect)
        icons.append(R.drawable.baseline_crop_3_2_24)
        strings.append(R.string.PaintModeRect)

        if (currentPaintState != null && currentPaintState!!.isEmpty()) {
            ids.append(R.id.paint_mode_fill)
            icons.append(R.drawable.baseline_format_color_fill_24)
            strings.append(R.string.PaintModeFill)
        }

        /*ids.append(R.id.paint_mode_zoom);
    icons.append(R.drawable.ic_zoom_out_map_black_24dp);
    strings.append(R.string.PaintModeZoomArea);*/

        /*if (canErase) {
      ids.append(R.id.paint_clear);
      icons.append(R.drawable.ic_delete_white);
      strings.append("Clear all");
    }*/
        showOptions(null, ids.get(), strings.get(), null, icons.get(), OptionDelegate { itemView: View?, id: Int ->
            val paintMode = when (id) {
                R.id.paint_mode_path -> PaintMode.PATH
                R.id.paint_mode_arrow -> PaintMode.ARROW
                R.id.paint_mode_rect -> PaintMode.RECTANGLE
                R.id.paint_mode_zoom -> PaintMode.FREE_MOVEMENT
                R.id.paint_mode_fill -> {
                    val color = colorPickerView!!.getPreview().getBrushColor()
                    val drawing = SimpleDrawing(SimpleDrawing.TYPE_FILLING)
                    drawing.setBrushParameters(color, 0f)
                    currentPaintState!!.addSimpleDrawing(drawing)
                    currentPaintState!!.trackSimpleDrawingAction(drawing)
                    null
                }
                else -> null
            }
            if (paintMode != null) {
                setPaintType(paintMode, true)
            }
            true
        }, this@MediaViewController.forcedTheme)
    }

    private fun checkCanUndo() {
        setCanUndo(currentPaintState != null && !currentPaintState!!.isEmpty())
    }

    private var canUndo = true

    private fun setCanUndo(canUndo: Boolean) {
        if (this.canUndo != canUndo) {
            this.canUndo = canUndo
            undoButton!!.setAlpha(if (canUndo) 1f else .4f)
            undoButton!!.setEnabled(canUndo)
        }
    }

    fun allowMediaViewGestures(isTouchDown: Boolean): Boolean {
        if (thumbsScrollAnimator == null || !thumbsScrollAnimator!!.getValue()) {
            return true
        }
        if (isTouchDown) {
            thumbsRecyclerView!!.stopScroll()
            setThumbsScrolling(false)
            return false
        }
        return false
    }

    fun allowMovingZoomedView(): Boolean {
        return mode != MODE_GALLERY || currentSection != SECTION_PAINT || paintType == PaintMode.FREE_MOVEMENT
    }

    private val brushScale: Float
        // Painting process
        get() = 1f // currentCropState != null ? (float) Math.min(currentCropState.getRegionWidth(), currentCropState.getRegionHeight()) : 1f;

    override fun onBrushChanged(v: ColorPreviewView) {
        if (paintView != null) {
            paintView!!.getContentWrap().setBrushParameters(v.getBrushColor(), v.getBrushRadius(this.brushScale))
        }
    }

    private fun undoAllPaintActions(): Boolean {
        if (paintView != null && !paintView!!.getContentWrap().isBusy()) {
            currentPaintState!!.undoAllActions()
            return true
        }
        return false
    }

    private fun undoLastPaintAction() {
        if (paintView != null && !paintView!!.getContentWrap().isBusy()) {
            currentPaintState!!.undoLastAction()
        }
    }

    private fun applyPaintingMode(mode: Int) {
        if (paintView != null) {
            paintView!!.setPaintingMode(mode)
        }
    }

    override fun onUndoAvailableStateChanged(state: PaintState?, isAvailable: Boolean, totalActionsCount: Int) {
        setCanUndo(isAvailable)
    }

    // etc
    private fun hideSection(section: Int) {
        when (section) {
            SECTION_CAPTION -> {}
            SECTION_FILTERS -> {
                resetEditorState()
                bottomWrap!!.removeViewInLayout(filtersView)
                mediaView!!.getBaseCell().removeView(editorView)
            }

            SECTION_QUALITY -> {
                resetVideoLimit()
                bottomWrap!!.removeViewInLayout(qualityControlWrap)
            }

            SECTION_CROP -> {
                resetCropState()
                bottomWrap!!.removeView(cropControlsWrap)
                contentView!!.removeView(cropLayout)
            }

            SECTION_PAINT -> {
                if (currentPaintState != null) {
                    currentPaintState!!.removeUndoStateListener(this)
                }
                bottomWrap!!.removeView(paintControlsWrap)
                mediaView!!.getBaseCell().removeView(paintView)
            }
        }
    }

    private fun applySection() {
        setLowProfile(true)
        hideSection(fromSection)

        when (currentSection) {
            SECTION_CAPTION -> {
                mediaView!!.setDisallowMove(false)
            }

            SECTION_FILTERS -> {
                showEditorView()
            }

            SECTION_CROP -> {
                setInCrop(true)
            }

            SECTION_PAINT -> {
                showPaintEditor()
            }
        }
    }

    private fun scheduleSectionChange(fromSection: Int, toSection: Int): Boolean {
        when (fromSection) {
            SECTION_CROP -> {
                return closeCropAsync(toSection)
            }
        }
        return false
    }

    private fun prepareSectionToHide(section: Int) {
        when (section) {
            SECTION_CAPTION -> {
                mediaView!!.setDisallowMove(true)
            }

            SECTION_FILTERS -> {
                hideEditorView()
            }

            SECTION_QUALITY -> {
                hideQualityView()
            }

            SECTION_CROP -> {}
            SECTION_PAINT -> {
                hidePaintEditor()
            }
        }
    }

    private fun resetMediaPaddings(section: Int) {
        mediaView!!.setOffsets(getHorizontalOffsets(section), 0, getSectionTopOffset(section), 0, getSectionBottomOffset(section))
    }

    private fun showYesNo() {
        showOptions(
            Lang.getString(R.string.DiscardCurrentChanges),
            intArrayOf(R.id.btn_discard, R.id.btn_cancel),
            arrayOf<String?>(Lang.getString(R.string.Discard), Lang.getString(R.string.Cancel)),
            intArrayOf(OptionColor.RED, OptionColor.NORMAL),
            intArrayOf(R.drawable.baseline_delete_24, R.drawable.baseline_cancel_24),
            OptionDelegate { itemView: View?, id: Int ->
                if (id == R.id.btn_discard) {
                    changeSection(SECTION_CAPTION, MODE_CANCEL)
                }
                true
            },
            this@MediaViewController.forcedTheme
        )
    }

    private fun hasChanges(): Boolean {
        when (currentSection) {
            SECTION_FILTERS -> {
                return hasEditorChanges()
            }

            SECTION_CROP -> {
                return hasCropChanges()
            }

            SECTION_PAINT -> {
                return hasPaintChanges()
            }

            SECTION_QUALITY -> {
                return hasQualityChanges()
            }
        }
        return false
    }

    private var isUIBlocked = false

    fun setUIBlocked(blocked: Boolean) {
        this.isUIBlocked = blocked
    }

    private fun saveSectionData(section: Int): Boolean {
        when (currentSection) {
            SECTION_FILTERS -> {
                if (currentFiltersState!!.isEmpty()) {
                    val file = stack!!.getCurrent().getFilteredFile()
                    if (file != null) {
                        tdlib!!.filegen().removeFilteredBitmap(file.getFilePath())
                    }
                    stack!!.getCurrent().setFiltersState(null)
                    applyFilteredBitmap(currentTargetImageFile, sourceBitmap)
                } else {
                    selectMediaIfItsNot()
                    setUIBlocked(true)
                    applyFiltersAsync(section)
                    return true
                }
            }

            SECTION_CROP -> {
                if (!currentCropState!!.isEmpty()) {
                    selectMediaIfItsNot()
                }
                val item = stack!!.getCurrent()
                item.setCropState(currentCropState)

                val cellView = if (mediaView != null) mediaView!!.findCellForItem(item) else null
                if (cellView != null) {
                    cellView.checkCrop()
                    mediaView!!.requestLayout()
                }
            }

            SECTION_PAINT -> {
                if (!currentPaintState!!.isEmpty()) {
                    selectMediaIfItsNot()
                }
                if (stack!!.getCurrent().setPaintState(currentPaintState, true)) {
                    mediaView!!.getBaseReceiver().invalidate()
                }
            }

            SECTION_QUALITY -> {
                Settings.instance().preferredVideoLimit = currentVideoLimit
            }
        }
        return false
    }

    private fun resetChangedData() {
        when (currentSection) {
            SECTION_FILTERS -> {
                if (oldFiltersState != null && hasEditedFilters) {
                    currentFiltersState!!.reset(oldFiltersState)
                }
            }

            SECTION_QUALITY -> {
                currentVideoLimit = VideoLimit(prevVideoLimit!!)
            }

            SECTION_CROP -> {
                if (oldCropState != null && !oldCropState!!.isEmpty()) {
                    if (!oldCropState!!.equalsTo(currentCropState)) {
                        resetCrop(false)
                    }
                    stack!!.getCurrent().setCropState(oldCropState)
                } else {
                    if (currentCropState!!.hasRotations()) {
                        resetCrop(false)
                    }
                    stack!!.getCurrent().setCropState(null)
                }
            }

            SECTION_PAINT -> {
                val changed: Boolean
                if (oldPaintState != null && !oldPaintState!!.isEmpty()) {
                    changed = stack!!.getCurrent().setPaintState(oldPaintState, true)
                } else {
                    changed = stack!!.getCurrent().setPaintState(null, true)
                }
                if (changed) {
                    mediaView!!.getBaseReceiver().invalidate()
                }
            }
        }
    }

    private val isCurrentReady: Boolean
        get() {
            val item = stack!!.getCurrent()
            return item != null && (item.getSourceGalleryFile() == null || item.getSourceGalleryFile().isReady())
        }

    private fun allowDataChanges(): Boolean {
        return (sectionChangeAnimator == null || !sectionChangeAnimator!!.isAnimating()) && !isUIBlocked && this.isCurrentReady
    }

    private fun changeSection(section: Int, mode: Int, useFastAnimation: Boolean = false) {
        if (currentSection == section || !allowDataChanges()) {
            return
        }

        val hasChanges = hasChanges()
        val applyChanges: Boolean

        when (mode) {
            MODE_BACK_PRESS -> {
                if (hasChanges) {
                    showYesNo()
                    return
                }
                applyChanges = false
            }

            MODE_OK -> {
                applyChanges = true
            }

            MODE_CANCEL -> {
                applyChanges = false
            }

            else -> {
                return
            }
        }

        if (hasChanges) {
            if (applyChanges) {
                if (saveSectionData(section)) {
                    return
                }
            } else {
                resetChangedData()
            }
        }

        changeSectionImpl(section, useFastAnimation)
    }

    private fun applyFiltersAsync(futureSection: Int) {
        editorView!!.getBitmapAsync(BitmapCallback { bitmap: Bitmap? ->
            setUIBlocked(false)
            if (bitmap != null) {
                val filteredFile = stack!!.getCurrent().setFiltersState(currentFiltersState)
                tdlib!!.filegen().saveFilteredBitmap(filteredFile, bitmap)
                applyFilteredBitmap(filteredFile, bitmap)
                changeSectionImpl(futureSection)
            } else {
                UI.showToast("Error while saving changes, sorry", Toast.LENGTH_SHORT)
            }
        })
    }

    private fun changeSectionImpl(section: Int, useFastAnimation: Boolean = false) {
        changeSectionImpl(section, if (useFastAnimation) 220L else 380L)
    }

    private fun changeSectionImpl(section: Int, duration: Long) {
        if (scheduleSectionChange(currentSection, section)) {
            return
        }
        if (!initSection(section)) {
            return
        }

        fromSection = currentSection
        fromTopOffset = getSectionTopOffset(fromSection)
        fromBottomOffset = getSectionBottomOffset(fromSection)
        fromHorOffset =  /*fromSection == SECTION_CROP ? getHorizontalOffsets(fromSection) :*/
            mediaView!!.getPaddingHorizontal() // getHorizontalOffsets(fromSection);

        mediaView!!.getBaseCell().getDetector().preparePositionReset()

        prepareSectionToHide(fromSection)
        prepareSectionToShow(section)

        fillIcons(section)

        currentSection = section
        toTopOffset = getSectionTopOffset(currentSection)
        toBottomOffset = getSectionBottomOffset(currentSection)
        toHorOffset = getHorizontalOffsets(currentSection)

        updateIconStates(true)

        if (sectionChangeAnimator == null) {
            sectionChangeAnimator = FactorAnimator(ANIMATOR_SECTION, this, LINEAR_INTERPOLATOR, duration)
        } else {
            sectionChangeAnimator!!.forceFactor(0f)
            sectionChangeAnimator!!.setDuration(duration)
        }

        if (duration > 0) {
            sectionChangeAnimator!!.animateTo(1f)
        } else {
            sectionChangeAnimator!!.forceFactor(1f)
        }
    }

    private var prevActiveButtonId = 0

    private fun fillIcons(section: Int, animated: Boolean = true) {
        var activeButtonId = 0
        var isSticker = false
        when (section) {
            SECTION_FILTERS, SECTION_QUALITY -> {
                activeButtonId = R.id.btn_adjust
            }

            SECTION_CROP -> {
                activeButtonId = R.id.btn_crop
            }

            SECTION_PAINT -> {
                activeButtonId = R.id.btn_paint
                isSticker = true
            }

            SECTION_CAPTION -> {}
        }

        if (isSticker) {
            // adjustOrTextButton.setIcon(R.drawable.ic_addtext, true, false);
            // cropOrStickerButton.setIcon(R.drawable.ic_addsticker, true, false);
        } else {
            if (stack!!.getCurrent().isVideo()) {
                adjustOrTextButton!!.setIcon(R.drawable.baseline_settings_24, animated, section == SECTION_QUALITY)
                if (!Config.MODERN_VIDEO_TRANSCODING_ENABLED) {
                    cropOrStickerButton!!.setIcon(R.drawable.baseline_rotate_90_degrees_ccw_24, animated, false)
                }
            } else {
                adjustOrTextButton!!.setIcon(R.drawable.baseline_tune_24, animated, section == SECTION_FILTERS)
                if (!Config.MODERN_VIDEO_TRANSCODING_ENABLED) {
                    cropOrStickerButton!!.setIcon(R.drawable.baseline_crop_rotate_24, animated, section == SECTION_CROP)
                }
            }
        }

        if (prevActiveButtonId != 0 && activeButtonId != prevActiveButtonId) {
            setButtonActive(prevActiveButtonId, false, animated)
        }

        prevActiveButtonId = activeButtonId

        if (activeButtonId != 0) {
            backButton!!.setIcon(R.drawable.baseline_close_24, animated, false)
            sendButton!!.setIcon(R.drawable.baseline_check_24, animated, false)
            sendButton!!.setSlowModeVisibility(false, animated)
        } else {
            backButton!!.setIcon(R.drawable.baseline_arrow_back_24, animated, false)
            setDefaultSendButtonIcon(animated)
        }
    }

    private fun setDefaultSendButtonIcon(animated: Boolean) {
        val forced = getArgumentsStrict().sendButtonIcon
        sendButton!!.setIcon(if (forced != 0) forced else R.drawable.deproko_baseline_send_24, animated, false)
        sendButton!!.setSlowModeVisibility(true, animated)
    }

    private fun hasAppliedFilters(): Boolean {
        val state = stack!!.getCurrent().getFiltersState()
        return state != null && !state.isEmpty()
    }

    private fun hasAppliedCrop(): Boolean {
        val cropState = stack!!.getCurrent().getCropState()
        return cropState != null && !cropState.isEmpty()
    }

    private fun hasAppliedPaints(): Boolean {
        val paintState = stack!!.getCurrent().getPaintState()
        return paintState != null && !paintState.isEmpty()
    }

    private fun updateIconStates(animated: Boolean) {
        val allowEditState = !Config.MASKS_TEXTS_AVAILABLE || currentSection != SECTION_PAINT
        adjustOrTextButton!!.setEdited(allowEditState && hasAppliedFilters(), animated)
        cropOrStickerButton!!.setEdited(allowEditState && hasAppliedCrop(), animated)
        paintOrMuteButton!!.setEdited(hasAppliedPaints(), animated)
        if (stopwatchButton != null) {
            val selfDestructType = stack!!.getCurrent().getSelfDestructType()
            val value: String?
            if (selfDestructType != null) {
                when (selfDestructType.getConstructor()) {
                    MessageSelfDestructTypeImmediately.CONSTRUCTOR -> value = TdlibUi.getDuration(0, TimeUnit.SECONDS, false) // FIXME
                    MessageSelfDestructTypeTimer.CONSTRUCTOR -> value =
                        TdlibUi.getDuration((selfDestructType as MessageSelfDestructTypeTimer).selfDestructTime.toLong(), TimeUnit.SECONDS, false)

                    else -> {
                        assertMessageSelfDestructType_58882d8c()
                        throw unsupported(selfDestructType)
                    }
                }
            } else {
                value = null
            }
            if (animated) {
                stopwatchButton!!.setValue(value, false)
            } else {
                stopwatchButton!!.forceValue(value, true)
            }
        }
    }

    private fun setButtonActive(id: Int, isActive: Boolean) {
        setButtonActive(id, isActive, true)
    }

    private fun setButtonActive(id: Int, isActive: Boolean, animated: Boolean) {
        if (id == R.id.btn_crop) {
            cropOrStickerButton!!.setActive(isActive, animated)
        } else if (id == R.id.btn_paint) {
            paintOrMuteButton!!.setActive(isActive, animated)
        } else if (id == R.id.btn_adjust) {
            adjustOrTextButton!!.setActive(isActive, animated)
        }
    }

    private var currentActiveButton = 0

    private fun setSectionChangeFactor(factor: Float, fraction: Float) {
        val sectionFromFactor = if (factor >= .5f) 0f else 1f - DECELERATE_INTERPOLATOR.getInterpolation(factor / .5f)
        val sectionToFactor = if (factor < .5f) 0f else DECELERATE_INTERPOLATOR.getInterpolation((factor - .5f) / .5f)

        if (factor >= .5f && currentActiveButton != prevActiveButtonId) {
            if (prevActiveButtonId != 0) {
                setButtonActive(prevActiveButtonId, true)
            }
            currentActiveButton = prevActiveButtonId
        }

        setSectionFactor(fromSection, sectionFromFactor)
        setSectionFactor(currentSection, sectionToFactor)

        if (fromSection == SECTION_CAPTION || currentSection == SECTION_CAPTION) {
            if (fromSection == SECTION_PAINT) {
                setPaintSectionFactor(sectionFromFactor)
            } else if (currentSection == SECTION_PAINT) {
                setPaintSectionFactor(sectionToFactor)
            }
        } else {
            val rangeFactor = if (factor <= .25f) 0f else (factor - .25f) / .75f
            if (fromSection == SECTION_PAINT) {
                setPaintSectionFactor(1f - DECELERATE_INTERPOLATOR.getInterpolation(rangeFactor))
            } else if (currentSection == SECTION_PAINT) {
                setPaintSectionFactor(DECELERATE_INTERPOLATOR.getInterpolation(rangeFactor))
            }
        }

        mediaView!!.getBaseCell().getDetector().setPositionFactor(fraction)

        val currentHorOffset: Int
        if (toHorOffset > fromHorOffset) {
            currentHorOffset = fromHorOffset + ((toHorOffset - fromHorOffset).toFloat() * sectionToFactor).toInt()
        } else if (toHorOffset < fromHorOffset) {
            currentHorOffset = fromHorOffset + ((toHorOffset - fromHorOffset).toFloat() * (1f - sectionFromFactor)).toInt()
        } else {
            currentHorOffset = fromHorOffset
        }

        val currentBottomOffset: Int
        if (toBottomOffset > fromBottomOffset) {
            currentBottomOffset = fromBottomOffset + ((toBottomOffset - fromBottomOffset).toFloat() * sectionToFactor).toInt()
        } else if (toBottomOffset < fromBottomOffset) {
            currentBottomOffset = fromBottomOffset + ((toBottomOffset - fromBottomOffset).toFloat() * (1f - sectionFromFactor)).toInt()
        } else {
            currentBottomOffset = fromBottomOffset
        }

        val currentTopOffset: Int
        if (toTopOffset > fromTopOffset) {
            currentTopOffset = fromTopOffset + ((toTopOffset - fromTopOffset).toFloat() * sectionToFactor).toInt()
        } else if (toTopOffset < fromTopOffset) {
            currentTopOffset = fromTopOffset + ((toTopOffset - fromTopOffset).toFloat() * (1f - sectionFromFactor)).toInt()
        } else {
            currentTopOffset = fromTopOffset
        }

        mediaView!!.setOffsets(currentHorOffset, 0, currentTopOffset, 0, currentBottomOffset)
    }

    private var mainSectionDisappearFactor = 0f

    private fun setSectionFactor(section: Int, factor: Float) {
        when (section) {
            SECTION_CAPTION -> {
                mainSectionDisappearFactor = 1f - factor
                updateMainItemsAlpha()

                captionWrapView!!.setAlpha(factor)
                if (videoSliderView != null) {
                    videoSliderView!!.setAlpha(factor)
                }
                updateBottomSpaceAlpha()
            }

            SECTION_FILTERS -> {
                filtersView!!.setAlpha(clamp(factor))
                filtersView!!.setTranslationY(getSectionHeight(SECTION_FILTERS) * (1f - factor))
            }

            SECTION_QUALITY -> {
                qualityControlWrap!!.setAlpha(clamp(factor))
                qualityControlWrap!!.setTranslationY(getSectionHeight(SECTION_QUALITY) * (1f - factor))
            }

            SECTION_CROP -> {
                cropControlsWrap!!.setAlpha(factor)
                mediaView!!.getBaseCell().getImageReceiver().setCropApplyFactor(1f - factor)
            }

            SECTION_PAINT -> {
                paintControlsWrap!!.setAlpha(factor)
            }
        }
    }

    // Quality stuff
    private var prevVideoLimit: VideoLimit? = null
    private var currentVideoLimit: VideoLimit? = null
    private var videoLimits: MutableList<VideoLimit>? = null

    private fun resetVideoLimit() {
        prevVideoLimit = null
    }

    private fun hasQualityChanges(): Boolean {
        return currentVideoLimit != prevVideoLimit
    }

    private fun updateQualityInfo() {
        if (qualityInfo == null) return

        val file = stack!!.getCurrent().getSourceGalleryFile()
        var width = file.getWidth()
        var height = file.getHeight()
        val inputFrameRate = file.getVideoFrameRate()
        val inputBitrate = file.getVideoBitrate()
        val accurateWidth = file.getVideoWidth()
        val accurateHeight = file.getVideoHeight()
        val videoDurationMs = file.getVideoDuration(true, TimeUnit.MILLISECONDS)
        val duration = videoDurationMs.toDouble() / 1000.0
        if (accurateWidth != 0.0 && accurateHeight != 0.0) {
            if (width > height) {
                width = max(accurateWidth, accurateHeight).toInt()
                height = min(accurateWidth, accurateHeight).toInt()
            } else {
                width = min(accurateWidth, accurateHeight).toInt()
                height = max(accurateWidth, accurateHeight).toInt()
            }
        }

        val outputFrameRate = currentVideoLimit!!.getOutputFrameRate(inputFrameRate)

        // File size = bitrate * number of minutes * .0075.
        // Bitrate = file size / (number of minutes * .0075).
        // Number of minutes = file size / (bitrate * .0075).
        var scaled = currentVideoLimit!!.getOutputSize(width, height)
        if (scaled == null) {
            scaled = Settings.VideoSize(max(width, height), min(width, height))
        }

        val outputBitrate = currentVideoLimit!!.getOutputBitrate(scaled, outputFrameRate, inputBitrate)
        var outputFileSize: Long = 0
        outputFileSize = (outputFileSize + (outputBitrate * duration) / 8).toLong()
        if (!file.shouldMuteVideo()) {
            outputFileSize = (outputFileSize + (62000 * duration) / 8).toLong()
        }

        val b = StringBuilder()
        b.append(Strings.buildSize(outputFileSize, true))
        b.append(" • ")
        b.append(if (width > height) scaled.majorSize.toString() + "x" + scaled.minorSize else scaled.minorSize.toString() + "x" + scaled.majorSize)
        b.append(" • ")
        b.append(outputFrameRate).append(" FPS")
        b.append(" • ").append((outputBitrate + (if (file.shouldMuteVideo()) 0 else 6200)) / 1000).append(" kbps")
        qualityInfo!!.setText(b.toString())
    }

    private fun updateQualitySlider() {
        if (qualitySlider == null) return

        if (this.videoLimits != null) {
            this.videoLimits!!.clear()
        } else {
            this.videoLimits = ArrayList<VideoLimit>()
        }

        val file = stack!!.getCurrent().getSourceGalleryFile()
        var width = file.getWidth()
        var height = file.getHeight()
        val accurateWidth = file.getVideoWidth()
        val accurateHeight = file.getVideoHeight()
        if (accurateWidth != 0.0 && accurateHeight != 0.0) {
            if (width > height) {
                width = max(accurateWidth, accurateHeight).toInt()
                height = min(accurateWidth, accurateHeight).toInt()
            } else {
                width = min(accurateWidth, accurateHeight).toInt()
                height = max(accurateWidth, accurateHeight).toInt()
            }
        }

        var currentValue = -1
        var minDiff = -1
        for (videoLimit in Settings.instance().videoLimits()) {
            val scaled = videoLimit.getOutputSize(width, height)
            if (scaled == null) {
                break
            }
            val diff = abs(currentVideoLimit!!.size.majorSize - scaled.majorSize)
            if (currentValue == -1 || diff < minDiff) {
                currentValue = videoLimits!!.size
                minDiff = diff
            }
            videoLimits!!.add(videoLimit)
        }
        if (currentVideoLimit!!.size.isUnlimited) {
            currentValue = videoLimits!!.size - 1
        }
        qualitySlider!!.setValueCount(videoLimits!!.size)
        qualitySlider!!.setValue(currentValue.toFloat() / (videoLimits!!.size - 1).toFloat())
        qualitySlider!!.setSlideEnabled(videoLimits!!.size > 1, false)

        updateQualityInfo()
    }

    private fun openQuality() {
        if (currentSection != SECTION_QUALITY) {
            if (!Config.USE_VIDEO_COMPRESSION) {
                UI.showApiLevelWarning(Build.VERSION_CODES.JELLY_BEAN_MR2)
                return
            }
            changeSection(SECTION_QUALITY, MODE_OK)
        }
    }

    private fun prepareQuality(): Boolean {
        this.prevVideoLimit = Settings.instance().preferredVideoLimit!!
        this.currentVideoLimit = VideoLimit(prevVideoLimit!!)
        return true
    }

    // Filters stuff
    private fun openFilters() {
        if (currentSection != SECTION_FILTERS) {
            changeSection(SECTION_FILTERS, MODE_OK)
        }
    }

    private fun openPaintCanvas() {
        if (stack!!.getCurrent().isVideo()) {
            return
        }
        if (currentSection != SECTION_PAINT) {
            changeSection(SECTION_PAINT, MODE_OK)
        }
    }

    private fun openCrop(useFastAnimation: Boolean = false) {
        if (Config.CROP_ENABLED) {
            if (currentSection != SECTION_CROP) {
                changeSection(SECTION_CROP, MODE_OK, useFastAnimation)
            }
        } else {
            // UI.showToast(R.string.FeatureDisabled, Toast.LENGTH_SHORT);
        }
    }

    private fun goBackToCaption(byBackPress: Boolean) {
        if (currentSection != SECTION_CAPTION) {
            changeSection(SECTION_CAPTION, if (byBackPress || currentSection == SECTION_PAINT) MODE_BACK_PRESS else MODE_CANCEL)
        }
    }

    // etc
    private fun selectMediaIfItsNot() {
        if (selectDelegate == null) {
            return
        }
        if (!checkView!!.isChecked()) {
            setMediaSelected(checkView!!.toggleChecked(), false)
        }
    }

    private fun setMediaSelected(isSelected: Boolean, animated: Boolean) {
        if (selectDelegate == null) {
            return
        }
        val index = stack!!.getCurrentIndex()
        val item = stack!!.getCurrent()
        if (!isSelected) {
            clearFilters(item, index)
        }
        selectDelegate!!.setMediaItemSelected(index, item, isSelected)
        val selectedCount = selectDelegate!!.getSelectedMediaCount()

        if (isSelected && selectedCount == 1) {
            if (animated) {
                animateCounterFactor(1f)
            } else {
                forceCounterFactor(1f)
            }
        } else if (selectedCount == 0 && !isSelected) {
            if (animated) {
                animateCounterFactor(0f)
            } else {
                forceCounterFactor(0f)
            }
        } else {
            counterView!!.setCounter(selectedCount)
        }
    }

    private fun openMasks() {
        // TODO
    }

    private fun addText() {
        // TODO
    }

    private fun toggleCheck(): Boolean {
        if (selectDelegate != null) {
            setMediaSelected(checkView!!.toggleChecked(), true)
            return true
        }
        return false
    }

    override fun onClick(v: View) {
        if (isUIBlocked) {
            return
        }

        val viewId = v.getId()
        if (viewId == R.id.menu_btn_stopwatch) {
            showTTLOptions()
        } else if (viewId == R.id.btn_inlineOpen) {
            backFromPictureInPicture()
        } else if (viewId == R.id.btn_caption_done) {
            onCaptionDone()
        } else if (viewId == R.id.btn_caption_emoji) {
            processEmojiClick()
        } else if (viewId == R.id.btn_inlineClose) {
            closePictureInPicture()
        } else if (viewId == R.id.btn_inlinePlayPause) {
            stack!!.getCurrent().performClick(v)
        } else if (viewId == R.id.btn_check) {
            toggleCheck()
        } else if (viewId == R.id.btn_counter) {
            if (selectDelegate != null && (!selectDelegate!!.isMediaItemSelected(
                    stack!!.getCurrentIndex(),
                    stack!!.getCurrent()
                ) || selectDelegate!!.getSelectedMediaCount() > 1)
            ) {
                setShowOtherMedias(true)
            }
        } else if (viewId == R.id.btn_removePhoto) {
            val imageFile = (v.getParent() as MediaOtherView).getImage()
            unselectImage(imageFile)
        } else if (viewId == R.id.btn_back) {
            if (currentSection != SECTION_CAPTION) {
                goBackToCaption(false)
            } else if (inForceEditMode()) {
                closeForceEditMode()
            } else {
                close()
            }
        } else if (viewId == R.id.btn_send) {
            if (currentSection != SECTION_CAPTION) {
                changeSection(SECTION_CAPTION, MODE_OK)
            } else if (inputView != null && !tdlib!!.isSelfChat(getOutputChatId()) && !tdlib!!.hasPremium() && inputView!!.hasOnlyPremiumFeatures()) {
                context().tooltipManager().builder(sendButton)
                    .show(tdlib, Strings.buildMarkdown(this, Lang.getString(R.string.MessageContainsPremiumFeatures), null)).hideDelayed()
            } else if (needShowCropSectionInsteadSend()) {
                changeSection(SECTION_CROP, MODE_OK)
            } else {
                send(v, newSendOptions(), false, false)
            }
        } else if (viewId == R.id.btn_crop) {
            if (!Config.MASKS_TEXTS_AVAILABLE || currentSection != SECTION_PAINT) {
                if (Config.MODERN_VIDEO_TRANSCODING_ENABLED || !stack!!.getCurrent().isVideoOrGif()) {
                    openCrop()
                } else {
                    rotateBy90Degrees()
                }
            } else {
                openMasks()
            }
        } else if (viewId == R.id.btn_rotate) {
            rotateBy90Degrees()
        } else if (viewId == R.id.btn_mirrorHorizontal) {
            setMirrorHorizontally(!currentCropState!!.hasFlag(this.mirrorHorizontallyFlag))
        } else if (viewId == R.id.btn_proportion) {
            if (allowDataChanges() && currentSection == SECTION_CROP && !inProfilePhotoEditMode()) {
                val ids: IntList = IntList(PROPORTION_MODES.size + 2)
                val strings: StringList = StringList(PROPORTION_MODES.size + 2)
                val icons: IntList = IntList(PROPORTION_MODES.size + 2)
                val colors: IntList = IntList(PROPORTION_MODES.size + 2)

                val item = stack!!.getCurrent()

                val width = item.getWidth()
                val height = item.getHeight()

                // final boolean flipSides = Utils.isVertical(width, height, currentCropState.getRotateBy());
                var proportion = 0f

                if (proportionButton!!.isActive()) {
                    proportion = cropAreaView!!.getFixedProportion()

                    icons.append(R.drawable.baseline_crop_free_24)
                    ids.append(R.id.btn_proportion_free)
                    strings.append(R.string.CropFree)
                    colors.append(OptionColor.NORMAL)
                }

                val originalProportion = cropAreaView!!.getOriginalProportion()
                var proportionExists: IntArray? = null
                for (proportionMode in PROPORTION_MODES) {
                    if (proportionMode[0].toFloat() / proportionMode[1].toFloat() == originalProportion) {
                        proportionExists = proportionMode
                        break
                    }
                }
                if (originalProportion != 0f) {
                    icons.append(R.drawable.baseline_crop_original_24)
                    ids.append(R.id.btn_proportion_original)
                    if (proportionExists != null) {
                        if (proportionExists[2] == R.id.btn_proportion_square) {
                            strings.append(Lang.getString(R.string.CropOriginal) + " (" + Lang.getString(R.string.CropSquare) + ")")
                        } else {
                            strings.append(Lang.getString(R.string.CropOriginal) + " (" + proportionExists[0] + ":" + proportionExists[1] + ")")
                        }
                    } else {
                        strings.append(R.string.CropOriginal)
                    }
                    colors.append(if (originalProportion == proportion) OptionColor.BLUE else OptionColor.NORMAL)
                }

                if (width.toFloat() / height.toFloat() != 1f) {
                    // TODO ids.append(R.id.btn_proportion_flipSides);
                }

                for (proportionMode in PROPORTION_MODES) {
                    val id = proportionMode[2]
                    val verb1 = proportionMode[0]
                    val verb2 = proportionMode[1]
                    val verb3 = proportionMode[3]
                    if (proportionExists != null && verb1.toFloat() / verb2.toFloat() == originalProportion) {
                        continue
                    }
                    ids.append(id)
                    if (id == R.id.btn_proportion_square) {
                        strings.append(R.string.CropSquare)
                    } else {
                        strings.append(verb1.toString() + ":" + verb2)
                    }
                    icons.append(verb3)
                    colors.append(if (verb1.toFloat() / verb2.toFloat() == proportion) OptionColor.BLUE else OptionColor.NORMAL)
                }

                if (!currentCropState!!.isEmpty()) {
                    colors.append(OptionColor.RED)
                    ids.append(R.id.btn_crop_reset)
                    strings.append(R.string.Reset)
                    icons.append(R.drawable.baseline_cancel_24)
                }

                showOptions(null, ids.get(), strings.get(), colors.get(), icons.get(), OptionDelegate { itemView: View?, id: Int ->
                    if (id == R.id.btn_crop_reset) {
                        resetCrop(true)
                    } else if (id == R.id.btn_proportion_free) {
                        setCropProportion(0, 0, true)
                    } else if (id == R.id.btn_proportion_original) {
                        val targetWidth = cropAreaView!!.getTargetWidth()
                        val targetHeight = cropAreaView!!.getTargetHeight()
                        setCropProportion(max(targetWidth, targetHeight), min(targetWidth, targetHeight), true)
                    } else {
                        var mode: IntArray? = null
                        for (proportionMode in PROPORTION_MODES) {
                            if (proportionMode[2] == id) {
                                mode = proportionMode
                                break
                            }
                        }
                        if (mode != null) {
                            setCropProportion(mode[0], mode[1], true)
                        }
                    }
                    true
                }, this@MediaViewController.forcedTheme)
            }
        } else if (viewId == R.id.btn_adjust) {
            if (!Config.MASKS_TEXTS_AVAILABLE || currentSection != SECTION_PAINT) {
                val item = stack!!.getCurrent()
                when (item.getType()) {
                    MediaItem.TYPE_GALLERY_PHOTO -> {
                        openFilters()
                    }

                    MediaItem.TYPE_GALLERY_VIDEO -> {
                        openQuality()
                    }

                    MediaItem.TYPE_GALLERY_GIF -> {}
                }
            } else {
                addText()
            }
        } else if (viewId == R.id.btn_paint) {
            when (stack!!.getCurrent().getType()) {
                MediaItem.TYPE_GALLERY_PHOTO -> {
                    openPaintCanvas()
                }

                MediaItem.TYPE_GALLERY_VIDEO -> {
                    toggleMute()
                }
            }
        } else if (viewId == R.id.btn_paintType) {
            showPaintTypes()
        } else if (viewId == R.id.paint_undo) {
            undoLastPaintAction()
        }
    }

    private fun needShowCropSectionInsteadSend(): Boolean {
        if (!inProfilePhotoEditMode()) {
            return false
        }

        if (cropAreaView == null) {
            return true
        }

        val cropState = obtainCropState(true)!!

        val targetWidth = (cropAreaView!!.getTargetWidth() * (cropState.getRight() - cropState.getLeft()))
        val targetHeight = (cropAreaView!!.getTargetHeight() * (cropState.getBottom() - cropState.getTop()))
        val proportion = max(targetWidth, targetHeight) / min(targetWidth, targetHeight)

        return abs(proportion - 1.0) > 0.02
    }

    // TTL
    private fun showTTLOptions() {
        val item = stack!!.getCurrent()
        tdlib!!.ui().showTTLPicker(
            context(),
            item.getSelfDestructType(),
            !isSecret(item.getSourceChatId()),
            true,
            true,
            if (item.isVideo()) R.string.MessageLifetimeVideo else R.string.MessageLifetimePhoto,
            RunnableData { result: TTLOption? ->
                if (stack!!.getCurrent() === item) {
                    val selfDestructType: MessageSelfDestructType?
                    val textRepresentation: String?
                    if (result!!.isOff) {
                        selfDestructType = null
                        textRepresentation = null
                    } else if (result.isImmediate) {
                        selfDestructType = MessageSelfDestructTypeImmediately()
                        textRepresentation = TdlibUi.getDuration(0, TimeUnit.SECONDS, false) // FIXME
                    } else {
                        val newTTL = result.ttlTime
                        textRepresentation = TdlibUi.getDuration(newTTL.toLong(), TimeUnit.SECONDS, false)
                        selfDestructType = MessageSelfDestructTypeTimer(newTTL)
                    }
                    item.setSelfDestructType(selfDestructType)
                    stopwatchButton!!.setValue(textRepresentation)
                    if (selfDestructType != null) {
                        selectMediaIfItsNot()
                    }
                }
            })
    }

    // Etc
    fun open() {
        getValue()
        popupView!!.showAnimatedPopupView(contentView, this)
    }

    fun minimizeOrClose() {
        if (inPictureInPicture) {
            return
        }
        pauseVideoIfPlaying()
        if (canGoPip()) {
            enterPictureInPicture()
            return
        }
        close()
    }

    fun close() {
        if (inPictureInPicture) {
            closePictureInPicture()
        } else if (forceAnimationType != -1 || !performOnBackPressed(false, true)) {
            popupView!!.hideWindow(true)
        }
    }

    fun forceClose() {
        if (forceAnimationType == -1) {
            forceAnimationType = ANIMATION_TYPE_FADE
        }
        if (inPictureInPicture) {
            closePictureInPicture()
        } else {
            popupView!!.hideWindow(true)
        }
    }

    fun closeByDelete() {
        val animator = FactorAnimator(ANIMATOR_PRIVACY, this, DECELERATE_INTERPOLATOR, 180L)
        animator.animateTo(1f)
        forceAnimationType = ANIMATION_TYPE_SECRET_CLOSE
        popupView!!.hideWindow(true)
    }

    var isMediaSent: Boolean = false
        private set

    private fun canDisableMarkdown(): Boolean {
        if (selectDelegate == null) return false
        val imageFiles = selectDelegate!!.getSelectedMediaItems(false)
        if (imageFiles != null) {
            for (file in imageFiles) {
                if (file is ImageGalleryFile && file.canDisableMarkdown()) return true
            }
            return false
        }
        val file = stack!!.getCurrent().getSourceGalleryFile()
        if (file.canDisableMarkdown()) return true
        return selectDelegate!!.canDisableMarkdown()
    }

    private fun canSendAsFile(): Int {
        if (selectDelegate == null) return SEND_MODE_NONE
        val imageFiles = selectDelegate!!.getSelectedMediaItems(false)
        if (imageFiles != null) {
            for (file in imageFiles) {
                if (file !is ImageGalleryFile || !file.canSendAsFile()) return SEND_MODE_NONE
            }
            if (!imageFiles.isEmpty()) {
                for (file in imageFiles) {
                    if (!(file is ImageGalleryFile && file.isVideo())) return SEND_MODE_FILES
                }
                return SEND_MODE_VIDEOS
            }
        } else {
            val file = stack!!.getCurrent().getSourceGalleryFile()
            if (file.canSendAsFile()) {
                return if (file.isVideo()) SEND_MODE_VIDEOS else SEND_MODE_FILES
            }
        }
        return SEND_MODE_NONE
    }

    fun send(view: View?, initialSendOptions: MessageSendOptions, disableMarkdown: Boolean, asFiles: Boolean) {
        if (sendDelegate == null) {
            return
        }

        if (initialSendOptions.schedulingState == null && showSlowModeRestriction(sendButton)) {
            return
        }

        if (initialSendOptions.schedulingState == null && getArgumentsStrict().areOnlyScheduled) {
            tdlib!!.ui()
                .showScheduleOptions(this, getOutputChatId(), false, SimpleSendCallback { modifiedSendOptions: MessageSendOptions?, disableMarkdown1: Boolean ->
                    send(view, modifiedSendOptions!!, disableMarkdown, asFiles)
                }, initialSendOptions, this@MediaViewController.forcedTheme)
            return
        }

        var imageFiles = if (selectDelegate != null) selectDelegate!!.getSelectedMediaItems(true) else null

        if (imageFiles == null) {
            imageFiles = ArrayList<ImageFile?>()
            imageFiles.add(stack!!.getCurrent().getSourceGalleryFile())
        } else if (imageFiles.isEmpty()) {
            imageFiles.add(stack!!.getCurrent().getSourceGalleryFile())
        }

        if (sendDelegate!!.sendSelectedItems(
                view,
                imageFiles,
                initialSendOptions,
                disableMarkdown,
                asFiles,
                sendDelegate!!.showCaptionAboveMedia(),
                sendDelegate!!.isHideMediaEnabled()
            )
        ) {
            forceAnimationType = ANIMATION_TYPE_FADE
            isMediaSent = true
            setUIBlocked(true)
            popupView!!.hideWindow(true)
        }
    }

    private val isProfileStack: Boolean
        get() = mode == MODE_PROFILE || mode == MODE_CHAT_PROFILE

    override fun handleLanguageDirectionChange() {
        super.handleLanguageDirectionChange()
        updateTitleMargins()
        if (captionView is RtlCheckListener) {
            (captionView as RtlCheckListener).checkRtl()
        }
        if (thumbsLayoutManager != null) thumbsLayoutManager!!.setReverseLayout(Lang.rtl())
        if (thumbsRecyclerView != null) {
            thumbsRecyclerView!!.invalidateItemDecorations()
            ensureThumbsPosition(false, false)
        }
    }

    private fun openSetSenderPopup(chat: Chat?) {
        if (chat == null) return

        tdlib()!!.send<ChatMessageSenders?>(GetChatAvailableMessageSenders(chat.id), Tdlib.ResultHandler { result: ChatMessageSenders?, error: TdApi.Error? ->
            UI.post(
                Runnable {
                    if (result != null) {
                        val c = SetSenderController(context, tdlib()!!)
                        c.setArguments(SetSenderController.Args(chat, result.senders, chat.messageSenderId))
                        c.setShowOverEverything(true)
                        c.setDelegate(SetSenderControllerPage.Delegate { s: ChatMessageSender? -> setNewMessageSender(chat, s!!) })
                        c.show()
                    }
                })
        })
    }

    private fun setNewMessageSender(chat: Chat, sender: ChatMessageSender) {
        tdlib()!!.send<TdApi.Ok?>(SetChatMessageSender(chat.id, sender.sender), tdlib!!.typedOkHandler())
    }

    private fun setEmojiShown(emojiShown: Boolean) {
        if (this.isEmojiVisible != emojiShown) {
            this.isEmojiVisible = emojiShown
            setTextFormattingLayoutVisible(textInputHasSelection)
            if (inputView != null) {
                inputView!!.setActionModeVisibility(!textInputHasSelection || !emojiShown)
            }
            if (inlineResultsView != null) {
                inlineResultsView!!.updatePosition(false)
            }
            updateBottomWrapMargin()
            if (bottomSpace != null) {
                bottomSpace!!.setVisibility(if (bottomInnerMargin > 0 && !emojiShown) View.VISIBLE else View.GONE)
            }
        }
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
        if (!this.isEmojiVisible) {
            captionEmojiButton!!.setImageResource(this.targetIcon)
        }
    }

    private fun setTextFormattingLayoutVisible(visible: Boolean) {
        textFormattingVisible = keyboardFrameLayout != null && keyboardFrameLayout!!.contentView.setTextFormattingLayoutVisible(visible)
    }

    private fun closeTextFormattingKeyboard() {
        if (textFormattingVisible && this.isEmojiVisible) {
            closeEmojiKeyboard()
        }
    }

    @get:DrawableRes
    val targetIcon: Int
        get() = if (textInputHasSelection || (textFormattingVisible && this.isEmojiVisible)) R.drawable.baseline_format_text_24 else R.drawable.deproko_baseline_insert_emoticon_26

    fun showSlowModeRestriction(v: View?): Boolean {
        if (selectDelegate == null) {
            return false
        }

        val restriction = tdlib()!!.getSlowModeRestrictionText(selectDelegate!!.getOutputChatId())
        if (restriction != null) {
            context().tooltipManager().builder(v).show(tdlib, restriction).hideDelayed()
            return true
        }

        return false
    }

    private var forceEditModeOld_arguments: Args? = null
    private var forceEditModeOld_bottomWrap: FrameLayoutFix? = null
    private var forceEditModeOld_captionView: View? = null
    private var forceEditModeOld_captionWrapView: ViewGroup? = null
    private var forceEditMode_views: MutableList<View?>? = null

    private fun inForceEditMode(): Boolean {
        return forceEditModeOld_arguments != null
    }

    private fun openForceEditMode() {
        val imageFile = stack!!.getCurrent().getTargetImage()
        U.toGalleryFile(File(imageFile.getFilePath()), false, RunnableData { imageGalleryFile: ImageGalleryFile? ->
            if (imageGalleryFile != null) {
                val mediaItem = MediaItem(context, tdlib, imageGalleryFile)
                val mediaStack = MediaStack(context, tdlib)
                mediaStack.set(mediaItem)
                openForceEditModeImpl(mediaStack)
            }
        })
    }

    private fun stopFullScreenTemporarily(stop: Boolean) {
        if (useEdgeToEdge()) {
            return
        }
        if (stop) {
            popupView!!.setIgnoreBottom(false)
            popupView!!.setIgnoreAllInsets(false)
            // setLowProfile(false);
            setFullScreen(false)
        } else {
            val b = canRunFullscreen()
            popupView!!.setIgnoreBottom(b)
            popupView!!.setIgnoreAllInsets(b)
            // setLowProfile(true);
            setFullScreen(true)
        }
    }

    private fun openForceEditModeImpl(mediaStack: MediaStack) {
        forceEditModeOld_arguments = getArgumentsStrict()
        forceEditModeOld_bottomWrap = bottomWrap
        forceEditModeOld_captionView = captionView
        forceEditModeOld_captionWrapView = captionWrapView

        stopFullScreenTemporarily(true)

        val chatId = context.navigation().getCurrentStackItem()!!.getChatId()
        val hasRestriction = tdlib!!.hasRestriction(chatId, RightId.SEND_PHOTOS)

        replaceArguments(
            Args.Companion.fromGallery(
                this, null,
                object : MediaSelectDelegate {
                    override fun isMediaItemSelected(index: Int, item: MediaItem?): Boolean {
                        return false
                    }

                    override fun setMediaItemSelected(index: Int, item: MediaItem?, isSelected: Boolean) {
                    }

                    override fun getSelectedMediaCount(): Int {
                        return 0
                    }

                    override fun getOutputChatId(): Long {
                        val m = findOutputController()
                        return if (m != null) m.getOutputChatId() else 0
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
                        images: ArrayList<ImageFile?>,
                        options: MessageSendOptions?,
                        disableMarkdown: Boolean,
                        asFiles: Boolean,
                        showCaptionAboveMedia: Boolean,
                        hasSpoiler: Boolean
                    ): Boolean {
                        val galleryFile = images.get(0) as ImageGalleryFile
                        return onSendMedia(galleryFile, options, disableMarkdown, asFiles, showCaptionAboveMedia, hasSpoiler)
                    }

                    override fun onHideMediaStateChanged(hideMedia: Boolean) {
                        super.onHideMediaStateChanged(hideMedia)
                        mediaStack.getCurrent().setHasSpoiler(hideMedia)
                    }

                    fun onSendMedia(
                        file: ImageGalleryFile,
                        options: MessageSendOptions?,
                        disableMarkdown: Boolean,
                        asFiles: Boolean,
                        showCaptionAboveMedia: Boolean,
                        hasSpoiler: Boolean
                    ): Boolean {
                        val m = findOutputController()
                        if (m != null) {
                            val oldItem =
                                if (forceEditModeOld_arguments != null && forceEditModeOld_arguments!!.stack != null) forceEditModeOld_arguments!!.stack!!.getCurrent() else null
                            val canShare = oldItem != null && oldItem.canBeSaved() && oldItem.canBeShared()

                            if (hasRestriction) {
                                if (canShare) {
                                    openShareControllerForItem(MediaItem(context, tdlib, file))
                                } else {
                                    m.showRestriction(
                                        sendButton,
                                        Lang.getString(if (tdlib!!.isChannel(chatId)) R.string.RestrictSavingChannelInfo else R.string.RestrictSavingGroupInfo)
                                    )
                                }
                                return false
                            }

                            @RightId val rightId = if (file.isVideo()) RightId.SEND_VIDEOS else RightId.SEND_PHOTOS
                            if (m.showSlowModeRestriction(sendButton, options)) {
                                return false
                            }

                            context.forceCloseCamera()
                            val restriction = tdlib!!.getDefaultRestrictionText(m.chat!!, rightId)
                            if (restriction != null) {
                                if (canShare) {
                                    openShareControllerForItem(MediaItem(context, tdlib, file))
                                    UI.showToast(restriction, Toast.LENGTH_LONG)
                                } else {
                                    m.showRestriction(sendButton, restriction)
                                }
                                return false
                            }
                            return m.sendPhotosAndVideosCompressed(
                                arrayOf<ImageGalleryFile>(file),
                                false,
                                options,
                                disableMarkdown,
                                asFiles,
                                showCaptionAboveMedia,
                                hasSpoiler
                            )
                        }
                        return false
                    }
                }, mediaStack, false
            ).setReceiverChatIdValue(chatId).setSendButtonIconRes(if (hasRestriction) R.drawable.baseline_forward_24 else 0)
        )

        if (thumbsRecyclerView != null) {
            thumbsRecyclerView!!.setVisibility(View.GONE)
        }
        bottomWrap!!.setVisibility(View.GONE)

        updateMediaView()
        forceEditMode_views = createViewGalleryMode(context, true)

        stack!!.notifyMediaChanged(true)
        setForceEditModeVisibility(true)

        val captionWrapViewFinal: View? = captionWrapView

        fillIcons(SECTION_PAINT, false)
        captionWrapViewFinal!!.setVisibility(View.INVISIBLE)
        UI.post(Runnable { captionWrapViewFinal.setVisibility(View.VISIBLE) }, 300)
        changeSectionImpl(SECTION_PAINT, false)
    }

    private fun closeForceEditMode() {
        for (v in forceEditMode_views!!) {
            contentView!!.removeView(v)
        }
        if (captionView is Destroyable) {
            (captionView as Destroyable).performDestroy()
        }
        if (sendButton != null) {
            sendButton!!.destroySlowModeCounterController()
        }

        replaceArguments(forceEditModeOld_arguments!!)
        captionView = forceEditModeOld_captionView
        captionWrapView = forceEditModeOld_captionWrapView
        bottomWrap = forceEditModeOld_bottomWrap
        bottomWrap!!.setVisibility(View.VISIBLE)

        if (thumbsRecyclerView != null) {
            thumbsRecyclerView!!.setVisibility(View.VISIBLE)
        }

        updateMediaView()

        stack!!.notifyMediaChanged(true)
        setForceEditModeVisibility(false)

        forceEditModeOld_captionView = null
        forceEditModeOld_captionWrapView = null
        forceEditModeOld_bottomWrap = null
        forceEditModeOld_arguments = null
        forceEditMode_views = null

        stopFullScreenTemporarily(false)
    }

    private fun replaceArguments(args: Args) {
        stack!!.setCallback(null)
        setArguments(args)
        mediaView!!.initWithStack(stack)
        stack!!.setCallback(this)
    }

    private var animatorEditModeVisibility: BoolAnimator? = null

    private fun setForceEditModeVisibility(visible: Boolean) {
        if (animatorEditModeVisibility == null) {
            animatorEditModeVisibility = BoolAnimator(ANIMATOR_EDIT_MODE_VISIBILITY, this, DECELERATE_INTERPOLATOR, 180L, !visible)
        }
        animatorEditModeVisibility!!.setValue(visible, true)
    }

    private val forceEditModeVisibility: Float
        get() = if (animatorEditModeVisibility != null) animatorEditModeVisibility!!.getFloatValue() else 0f

    /* * */
    private fun updateMediaView() {
        if (!Config.DISABLE_VIEWER_ELEVATION) {
            if (mode == MODE_MESSAGES || mode == MODE_SIMPLE) {
                mediaView!!.setElevation(Screen.dp(2f).toFloat())
                mediaView!!.setOutlineProvider(object : ViewOutlineProvider() {
                    private val size = IntArray(2)

                    override fun getOutline(view: View, outline: Outline) {
                        if (getPipSize(size)) {
                            // size[0] /= view.getScaleX();
                            // size[1] /= view.getScaleY();

                            val width = view.getMeasuredWidth()
                            val height = view.getMeasuredHeight()

                            val left = width / 2 - size[0] / 2
                            val right = width / 2 + size[0] / 2
                            val top = height / 2 - size[1] / 2
                            val bottom = height / 2 + size[1] / 2
                            outline.setRect(left, top, right, bottom)
                        } else {
                            outline.setEmpty()
                        }
                    }
                })
            } else {
                mediaView!!.setElevation(0f)
                mediaView!!.setOutlineProvider(null)
            }
        }

        mediaView!!.setDisableDoubleTapZoom(mode == MODE_GALLERY)
    }

    private fun createViewGalleryMode(context: Context, hideCheckViews: Boolean): MutableList<View?> {
        val inProfilePhotoEditMode = inProfilePhotoEditMode()
        val attachedViews = ArrayList<View?>(7)
        val args = getArgumentsStrict()

        val chat = if (args.receiverChatId != 0L) tdlib!!.chat(args.receiverChatId) else null

        mediaView!!.setOffsets(0, 0, 0, 0, 0) // Screen.dp(56f)
        editWrap = FrameLayoutFix(context)
        editWrap!!.setBackgroundColor(Theme.getColor(ColorId.transparentEditor))
        editWrap!!.setLayoutParams(newParams(ViewGroup.LayoutParams.MATCH_PARENT, Screen.dp(56f), Gravity.BOTTOM))
        Views.setBottomMargin(editWrap, controlsMargin)

        backButton = EditButton(context)
        backButton!!.setId(R.id.btn_back)
        backButton!!.setIcon(R.drawable.baseline_arrow_back_24, false, false)
        backButton!!.setOnClickListener(this)
        backButton!!.setLayoutParams(newParams(Screen.dp(56f), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.LEFT))
        editWrap!!.addView(backButton)

        sendButton = EditButton(context)
        sendButton!!.setId(R.id.btn_send)
        setDefaultSendButtonIcon(false)
        sendButton!!.setOnClickListener(this)
        sendButton!!.setLayoutParams(newParams(Screen.dp(56f), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.RIGHT))
        sendButton!!.setBackgroundResource(R.drawable.bg_btn_header_light)
        if (selectDelegate != null) {
            sendButton!!.getSlowModeCounterController(tdlib).setCurrentChat(selectDelegate!!.getOutputChatId())
        }
        editWrap!!.addView(sendButton)

        if (chat != null && !hasFlag(Args.Companion.FLAG_DISALLOW_SEND_BUTTON_HAPTIC_MENU)) {
            tdlib!!.ui().createSimpleHapticMenu(
                this,
                chat.id,
                object : me.vkryl.core.lambda.FutureBool {
                    override fun getBoolValue(): Boolean = currentActiveButton == 0
                },
                object : me.vkryl.core.lambda.FutureBool {
                    override fun getBoolValue(): Boolean = this@MediaViewController.canDisableMarkdown()
                },
                object : me.vkryl.core.lambda.FutureBool {
                    override fun getBoolValue(): Boolean = true
                },
                RunnableData { hapticItems: MutableList<HapticMenuHelper.MenuItem?>? ->
                    if (sendDelegate != null && sendDelegate!!.allowHideMedia()) {
                        hapticItems!!.add(
                            0,
                            HapticMenuHelper.MenuItem(R.id.btn_spoiler, Lang.getString(R.string.HideMedia), R.drawable.deproko_baseline_whatshot_24)
                                .setIsCheckbox(true, sendDelegate!!.isHideMediaEnabled())
                                .setOnClickListener(object : HapticMenuHelper.OnItemClickListener {
                                    private var hotTooltipInfo: TooltipInfo? = null

                                    override fun onHapticMenuItemClick(view: View, parentView: View?, item: HapticMenuHelper.MenuItem): Boolean {
                                        if (view.getId() == R.id.btn_spoiler) {
                                            if (item.isCheckboxSelected) {
                                                hotTooltipInfo = context().tooltipManager().builder(view)
                                                    .icon(R.drawable.baseline_whatshot_24)
                                                    .color(context().tooltipManager().overrideColorProvider(this@MediaViewController.forcedTheme))
                                                    .locate(TooltipOverlayView.LocationProvider { targetView: View?, outRect: Rect? ->
                                                        val centerX = outRect!!.left + Screen.dp(29f)
                                                        val centerY = outRect.centerY()
                                                        val radius = Screen.dp(11f)
                                                        outRect.left = centerX - radius
                                                        outRect.right = centerX + radius
                                                        outRect.top = centerY - radius
                                                        outRect.bottom = centerY + radius
                                                    })
                                                    .show(tdlib, R.string.MediaSpoilerHint)
                                                    .hideDelayed()
                                            } else {
                                                if (hotTooltipInfo != null) {
                                                    hotTooltipInfo!!.hideNow()
                                                    hotTooltipInfo = null
                                                }
                                            }
                                            sendDelegate!!.onHideMediaStateChanged(item.isCheckboxSelected)
                                            return true
                                        }
                                        return false
                                    }
                                })
                        )
                    }
                    if (inForceEditMode()) {
                        val oldItem =
                            if (forceEditModeOld_arguments != null && forceEditModeOld_arguments!!.stack != null) forceEditModeOld_arguments!!.stack!!.getCurrent() else null
                        val canShare = oldItem != null && oldItem.canBeSaved() && oldItem.canBeShared()
                        if (canShare) {
                            hapticItems!!.add(
                                1,
                                HapticMenuHelper.MenuItem(R.id.btn_share, Lang.getString(R.string.MediaHapticForward), R.drawable.baseline_forward_24)
                                    .setOnClickListener(HapticMenuHelper.OnItemClickListener { view: View?, parentView: View?, item: HapticMenuHelper.MenuItem? ->
                                        if (view!!.getId() == R.id.btn_share) {
                                            openShareControllerForCurrentItem()
                                        }
                                        true
                                    })
                            )
                        }
                    }
                    val sendAsFile = canSendAsFile()
                    if (sendAsFile != SEND_MODE_NONE) {
                        val onlyVideos = sendAsFile == SEND_MODE_VIDEOS
                        val count = if (selectDelegate != null) selectDelegate!!.getSelectedMediaCount() else 1
                        hapticItems!!.add(
                            HapticMenuHelper.MenuItem(
                                R.id.btn_sendAsFile,
                                if (count <= 1) Lang.getString(if (onlyVideos) R.string.SendOriginal else R.string.SendAsFile) else Lang.plural(
                                    if (onlyVideos) R.string.SendXOriginals else R.string.SendAsXFiles,
                                    count.toLong()
                                ),
                                R.drawable.baseline_insert_drive_file_24
                            ).setOnClickListener(HapticMenuHelper.OnItemClickListener { view: View?, parentView: View?, item: HapticMenuHelper.MenuItem? ->
                                if (view!!.getId() == R.id.btn_sendAsFile) {
                                    send(sendButton, newSendOptions(), false, true)
                                }
                                true
                            }).bindTutorialFlag(Settings.TUTORIAL_SEND_AS_FILE)
                        )
                    }
                    if (chat != null && chat.messageSenderId != null) {
                        hapticItems!!.add(
                            0,
                            MediaLayout.createHapticSenderItem(tdlib, chat)
                                .setOnClickListener(HapticMenuHelper.OnItemClickListener { view: View?, parentView: View?, item: HapticMenuHelper.MenuItem? ->
                                    openSetSenderPopup(chat)
                                    true
                                })
                        )
                    }
                },
                SimpleSendCallback { sendOptions: MessageSendOptions?, disableMarkdown: Boolean ->
                    send(sendButton, sendOptions!!, disableMarkdown, false)
                },
                this@MediaViewController.forcedTheme
            ).attachToView(sendButton)
        }

        editButtons = LinearLayout(context)
        editButtons!!.setOrientation(LinearLayout.HORIZONTAL)
        editButtons!!.setLayoutParams(newParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER_HORIZONTAL))

        cropOrStickerButton = EditButton(context)
        cropOrStickerButton!!.setOnClickListener(this)
        cropOrStickerButton!!.setId(R.id.btn_crop)
        // cropOrStickerButton.setSecondIcon(R.drawable.deproko_baseline_insert_sticker_24);
        cropOrStickerButton!!.setIcon(R.drawable.baseline_crop_rotate_24, false, false)
        cropOrStickerButton!!.setLayoutParams(LinearLayout.LayoutParams(Screen.dp(56f), ViewGroup.LayoutParams.MATCH_PARENT))
        editButtons!!.addView(cropOrStickerButton)

        val params = LinearLayout.LayoutParams(Screen.dp(56f), ViewGroup.LayoutParams.MATCH_PARENT)
        params.leftMargin = Screen.dp(8f)
        params.rightMargin = Screen.dp(8f)

        paintOrMuteButton = EditButton(context)
        paintOrMuteButton!!.setOnClickListener(this)
        paintOrMuteButton!!.setId(R.id.btn_paint)
        paintOrMuteButton!!.setIcon(R.drawable.baseline_brush_24, false, false)
        paintOrMuteButton!!.setLayoutParams(params)
        editButtons!!.addView(paintOrMuteButton)

        adjustOrTextButton = EditButton(context)
        adjustOrTextButton!!.setId(R.id.btn_adjust)
        adjustOrTextButton!!.setOnClickListener(this)
        adjustOrTextButton!!.setSecondIcon(R.drawable.deproko_baseline_text_add_24)
        adjustOrTextButton!!.setIcon(R.drawable.baseline_tune_24, false, false)
        adjustOrTextButton!!.setLayoutParams(LinearLayout.LayoutParams(Screen.dp(56f), ViewGroup.LayoutParams.MATCH_PARENT))
        editButtons!!.addView(adjustOrTextButton)

        if (chat != null && chat.type.getConstructor() == TdApi.ChatTypePrivate.CONSTRUCTOR && !tdlib!!.isBotChat(chat) && !hasFlag(Args.Companion.FLAG_DISALLOW_SET_DESTRUCTION_TIMER)) {
            stopwatchButton = StopwatchHeaderButton(context)
            stopwatchButton!!.setBackgroundResource(R.drawable.bg_btn_header_light)
            stopwatchButton!!.forceValue(null, true)
            stopwatchButton!!.setId(R.id.menu_btn_stopwatch)
            stopwatchButton!!.setOnClickListener(this)
            stopwatchButton!!.setLayoutParams(LinearLayout.LayoutParams(Screen.dp(56f), ViewGroup.LayoutParams.MATCH_PARENT))
            editButtons!!.addView(stopwatchButton)
        }

        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      editWrap.setOutlineProvider(new android.view.ViewOutlineProvider() {
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void getOutline (View view, android.graphics.Outline outline) {
          int centerX = view.getMeasuredWidth() / 2;
          int centerY = view.getMeasuredHeight() / 2;
          int radius = Screen.dp(16f);
          outline.setRoundRect(centerX - radius, centerY - radius, centerX + radius, centerY + radius, radius);
        }
      });
    }*/
        editWrap!!.addView(editButtons)

        updateIconStates(false)

        contentView!!.addView(editWrap)
        attachedViews.add(editWrap)

        // Bottom wrap
        var fp = newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.BOTTOM)
        fp.bottomMargin = this.bottomWrapMargin

        bottomWrap = FrameLayoutFix(context)
        bottomWrap!!.setLayoutParams(fp)
        checkBottomWrapY()

        val captionView: InputView = object : InputView(context, tdlib, this) {
            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec)
                if (inlineResultsView != null) {
                    inlineResultsView!!.updatePosition(true)
                }
            }

            private var isDown = false

            override fun onTouchEvent(event: MotionEvent): Boolean {
                val res = Views.onTouchEvent(this, event) && super.onTouchEvent(event)
                when (event.getAction()) {
                    MotionEvent.ACTION_DOWN -> isDown = true
                    MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> isDown = false
                }
                if (textFormattingLayout != null) {
                    textFormattingLayout!!.onInputViewTouchEvent(event)
                }
                return res
            }

            override fun onScrollChanged(horiz: Int, vert: Int, oldHoriz: Int, oldVert: Int) {
                super.onScrollChanged(horiz, vert, oldHoriz, oldVert)
                contentView!!.requestDisallowInterceptTouchEvent(isDown)
            }
        }
        if (chat != null) {
            captionView.setNoPersonalizedLearning(Settings.instance().needsIncognitoMode(chat))
        }
        captionView.setHighlightColor(alphaColor(0.2f, Theme.fillingTextSelectionColor()))
        captionView.setHighlightColor(this@MediaViewController.forcedTheme.getColor(ColorId.textSelectionHighlight))
        // addThemeHighlightColorListener(captionView, ColorId.textSelectionHighlight);
        captionView.setMaxCodePointCount(tdlib!!.maxCaptionLength())
        captionView.setIgnoreCustomStuff(false)
        captionView.getInlineSearchContext().setIsCaption(true)
        captionView.setInputListener(this)
        captionView.setBackgroundColor(0)
        captionView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (ignoreCaptionUpdate) {
                    ignoreCaptionUpdate = false
                } else {
                    stack!!.getCurrent().setCaption(captionView.getOutputText(false))
                }
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })
        captionView.setSpanChangeListener(SpanChangeListener { v: InputView? ->
            if (!ignoreCaptionUpdate) {
                stack!!.getCurrent().setCaption(v!!.getOutputText(false))
            }
            if (textFormattingLayout != null) {
                textFormattingLayout!!.onInputViewSpansChanged()
            }
        })
        captionView.setHint(Lang.getString(R.string.AddCaption))
        captionView.setMaxLines(4)
        captionView.setId(R.id.input)
        captionView.setPadding(Screen.dp(55f), Screen.dp(15f), Screen.dp(55f), Screen.dp(14f))
        captionView.setTranslationX(-(Screen.dp(55f) - Screen.dp(14f)).toFloat())
        captionView.setHintTextColor(-0x45000001)
        captionView.setTextColor(-0x1)
        captionView.setTypeface(Fonts.getRobotoRegular())
        captionView.setLayoutParams(LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        captionView.setInputType(captionView.getInputType() or EditorInfo.TYPE_TEXT_FLAG_CAP_SENTENCES or EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE)
        captionView.setSelectionChangeListener(this)
        this.captionView = captionView
        this.inputView = captionView

        captionDoneButton = ImageView(context)
        captionDoneButton!!.setId(R.id.btn_caption_done)
        captionDoneButton!!.setOnClickListener(this)
        captionDoneButton!!.setScaleType(ImageView.ScaleType.CENTER)
        captionDoneButton!!.setImageResource(R.drawable.baseline_check_24)
        captionDoneButton!!.setColorFilter(-0x1)
        captionDoneButton!!.setAlpha(0f)
        captionDoneButton!!.setEnabled(false)
        captionDoneButton!!.setLayoutParams(newParams(Screen.dp(55f), Screen.dp(52f), Gravity.RIGHT or Gravity.BOTTOM))

        captionEmojiButton = ImageView(context())
        captionEmojiButton!!.setId(R.id.btn_caption_emoji)
        captionEmojiButton!!.setOnClickListener(this)
        captionEmojiButton!!.setScaleType(ImageView.ScaleType.CENTER)
        captionEmojiButton!!.setImageResource(R.drawable.deproko_baseline_insert_emoticon_26)
        captionEmojiButton!!.setColorFilter(-0x1)
        captionEmojiButton!!.setAlpha(0f)
        captionEmojiButton!!.setEnabled(false)
        captionEmojiButton!!.setLayoutParams(newParams(Screen.dp(55f), Screen.dp(52f), Gravity.LEFT or Gravity.BOTTOM))

        val captionWrapView: LinearLayout = object : LinearLayout(context) {
            override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
                return getVisibility() != VISIBLE || getAlpha() != 1f
            }

            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec)
                checkCaptionButtonsY()
            }

            override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
                super.onLayout(changed, l, t, r, b)
                checkCaptionButtonsY()
            }

            override fun onTouchEvent(event: MotionEvent?): Boolean {
                return getVisibility() == VISIBLE && getAlpha() == 1f && super.onTouchEvent(event)
            }
        }
        captionWrapView.setOrientation(LinearLayout.VERTICAL)
        captionWrapView.setBackgroundColor(Theme.getColor(ColorId.transparentEditor))
        captionWrapView.addView(captionView)
        captionWrapView.setLayoutParams(newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))
        this.captionWrapView = captionWrapView

        if (!inProfilePhotoEditMode) {
            bottomWrap!!.addView(captionWrapView)
            bottomWrap!!.addView(captionDoneButton)
            bottomWrap!!.addView(captionEmojiButton)
        }

        videoSliderView = VideoControlView(context)
        videoSliderView!!.setSliderListener(this)
        videoSliderView!!.setOnPlayPauseClick(View.OnClickListener { v: View? ->
            val c = stack!!.getCurrent().getFileProgress()
            if (c != null) {
                c.performClick(v)
            }
        })
        videoSliderView!!.setInnerAlpha(0f)
        videoSliderView!!.setTranslationY(Screen.dp(56f).toFloat())
        videoSliderView!!.setLayoutParams(newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        videoSliderView!!.setSlideEnabled(stack!!.getCurrent().canSeekVideo())
        bottomWrap!!.addView(videoSliderView)
        if (needTrim()) {
            videoSliderView!!.addTrim(object : TimelineDelegate {
                override fun canTrimTimeline(v: VideoTimelineView?): Boolean {
                    return true // TODO check if video is rendered
                }

                override fun onVideoLoaded(v: VideoTimelineView?, totalDuration: Double, width: Double, height: Double, frameRate: Int, bitrate: Long) {
                    stack!!.getCurrent().getSourceGalleryFile().setVideoInformation((totalDuration * 1000000.0).toLong(), width, height, frameRate, bitrate)
                }

                private var needResume = false

                override fun onTrimStartEnd(v: VideoTimelineView?, isStarted: Boolean) {
                    if (isStarted) {
                        if (isPlayingVideo.also { needResume = it }) {
                            pauseVideoIfPlaying()
                        }
                    } else if (needResume) {
                        stack!!.getCurrent().performClick(pipPlayPauseButton)
                    }
                }

                override fun onSeekTo(v: VideoTimelineView?, progress: Float) {
                    mediaView!!.setSeekProgress(progress)
                }

                override fun onTimelineTrimChanged(v: VideoTimelineView?, totalDuration: Double, startTimeSeconds: Double, endTimeSeconds: Double) {
                    val item = stack!!.getCurrent()
                    val changed: Boolean
                    if (startTimeSeconds == 0.0 && endTimeSeconds == totalDuration) {
                        changed = item.getSourceGalleryFile().setTrim(-1, -1, (totalDuration * 1000000.0).toLong())
                    } else {
                        changed = item.getSourceGalleryFile().setTrim(
                            (startTimeSeconds * 1000000.0).toLong(),
                            if (endTimeSeconds >= totalDuration) -1 else (endTimeSeconds * 1000000.0).toLong(),
                            (totalDuration * 1000000.0).toLong()
                        )
                    }
                    if (changed) {
                        val needFrameUpdate = item.checkTrim()
                        val cellView = if (mediaView != null) mediaView!!.findCellForItem(stack!!.getCurrent()) else null
                        if (cellView != null) {
                            cellView.checkTrim(needFrameUpdate)
                            var timeNow = cellView.getTimeNow()
                            var timeTotal = cellView.getTimeTotal()
                            if (timeNow == -1L || timeTotal == -1L) {
                                timeNow = 0
                                timeTotal = ((endTimeSeconds - startTimeSeconds) * 1000.0).toLong()
                            }
                            videoSliderView!!.resetDuration(timeTotal, timeNow, true, true)
                        } else {
                            item.invalidateContent(item)
                        }
                    }
                }
            }, this@MediaViewController.forcedTheme)
        }

        contentView!!.addView(bottomWrap)
        attachedViews.add(bottomWrap)

        // Image overlay
        overlayView = object : View(context) {
            override fun onTouchEvent(event: MotionEvent): Boolean {
                if (event.getAction() == MotionEvent.ACTION_DOWN && showOtherMedias) {
                    setShowOtherMedias(false)
                    return true
                }
                return false
            }
        }
        overlayView!!.setLayoutParams(newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        overlayView!!.setBackgroundColor(-0x60000000)
        overlayView!!.setAlpha(0f)
        contentView!!.addView(overlayView)
        attachedViews.add(overlayView)

        // Checks
        val innerPadding = Screen.dp(9f)
        val size = Screen.dp(20f) * 2 + Screen.dp(1f) * 2 + innerPadding * 2
        fp = newParams(size, size, Gravity.TOP or Gravity.RIGHT)
        fp.rightMargin = Screen.dp(20f) - innerPadding
        fp.topMargin = Screen.dp(20f) - innerPadding + topOffset / 2

        checkView = CheckView(context)
        checkView!!.setId(R.id.btn_check)
        checkView!!.initWithMode(CheckView.MODE_GALLERY)
        checkView!!.setPadding(innerPadding, innerPadding, 0, 0)
        checkView!!.setLayoutParams(fp)
        checkView!!.setOnClickListener(this)
        checkView!!.forceSetChecked(this.isCurrentItemSelected)

        fp = newParams(ViewGroup.LayoutParams.WRAP_CONTENT, Screen.dp(30f), Gravity.RIGHT)
        fp.rightMargin = Screen.dp(78f)
        fp.topMargin = Screen.dp(26f) + topOffset / 2

        counterView = CounterView(context)
        counterView!!.setId(R.id.btn_counter)
        counterView!!.setOnClickListener(this)
        counterView!!.setLayoutParams(fp)

        val count = this.selectedMediaCount
        counterView!!.initCounter(max(count, 1), false)
        forceCounterFactor(if (count == 0) 0f else 1f)

        if (!inProfilePhotoEditMode && !hideCheckViews && !inSingleMediaMode()) {
            contentView!!.addView(checkView)
            contentView!!.addView(counterView)
            attachedViews.add(checkView)
            attachedViews.add(counterView)
        }

        receiverView = buildReceiverRowView(chat)
        if (receiverView != null) {
            contentView!!.addView(receiverView)
            attachedViews.add(receiverView)
        }
        return attachedViews
    }

    private fun openShareControllerForCurrentItem() {
        openShareControllerForItem(stack!!.getCurrent())
    }

    private fun openShareControllerForItem(item2: MediaItem) {
        val c = ShareController(context, tdlib)
        val caption = if (item2.getCaption().isEmpty()) null else TD.toCharSequence(item2.getCaption())
        c.setArguments(ShareController.Args(item2, caption, caption).setAfter(Runnable { this.forceClose() }))
        c.show(true)
    }

    private fun buildReceiverRowView(chat: Chat?): LinearLayout? {
        val args = getArgumentsStrict()

        val needReceiverRow = (chat != null || args.receiverRowIcon != 0 || !isEmpty(args.receiverRowText))
        if (!needReceiverRow) {
            return null
        }

        @DrawableRes val icon = if (args.receiverRowIcon != 0) args.receiverRowIcon else R.drawable.baseline_arrow_upward_18
        val text = if (isEmpty(args.receiverRowText)) (if (chat != null) tdlib!!.chatTitle(chat) else null) else args.receiverRowText

        val fp = newParams(ViewGroup.LayoutParams.WRAP_CONTENT, Screen.getStatusBarHeight())
        if (topOffset > 0) {
            fp.leftMargin = Screen.dp(8f)
            fp.topMargin = topOffset + Screen.dp(4f)
        } else {
            fp.leftMargin = Screen.dp(12f)
            fp.topMargin = Screen.dp(4f)
        }

        val receiverView = LinearLayout(context)
        receiverView.setOrientation(LinearLayout.HORIZONTAL)
        receiverView.setAlpha(0f)
        receiverView.setLayoutParams(fp)

        var lp: LinearLayout.LayoutParams?

        lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, Screen.dp(17f))

        val imageView = ImageView(context)
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE)
        imageView.setImageResource(icon)
        imageView.setColorFilter(-0x1)
        imageView.setAlpha(0xaa / 0xff.toFloat())
        imageView.setLayoutParams(lp)
        receiverView.addView(imageView)

        lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.leftMargin = Screen.dp(6f)

        if (!isEmpty(text)) {
            val textView: TextView = NoScrollTextView(context)
            textView.setTextColor(-0x55000001)
            textView.setSingleLine(true)
            textView.setEllipsize(TextUtils.TruncateAt.END)
            textView.setTypeface(Fonts.getRobotoMedium())
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
            textView.setText(text)
            textView.setLayoutParams(lp)
            receiverView.addView(textView)
        }

        return receiverView
    }

    /* * */
    private fun findOutputController(): MessagesController? {
        val nstack = context.navigation().getStack()
        val s = nstack.size()

        for (a in s - 1 downTo 0) {
            val c = nstack.get(a)
            if (c is MessagesController) {
                return c
            }
        }

        return null
    }

    companion object {
        private const val REVEAL_ANIMATION_DURATION: Long =  /*BuildConfig.DEBUG ? 1800l :*/180
        private const val REVEAL_OPEN_ANIMATION_DURATION =  /*BuildConfig.DEBUG ? 1800l :*/180L
        private val DECELERATE_INTERPOLATOR = me.vkryl.android.DECELERATE_INTERPOLATOR
        private val LINEAR_INTERPOLATOR = me.vkryl.android.LINEAR_INTERPOLATOR

        const val MODE_MESSAGES: Int = 0 // opened from chat
        const val MODE_PROFILE: Int = 1 // opened from profile or chat (in case of groups and channels)
        const val MODE_CHAT_PROFILE: Int = 2
        const val MODE_GALLERY: Int = 3 // opened from gallery, need photo editor and stuff
        const val MODE_SECRET: Int = 4 // just single photo, no animations and etc
        const val MODE_SIMPLE: Int = 5

        // Appear animation
        private const val ANIMATION_TYPE_FADE = 0
        private const val ANIMATION_TYPE_REVEAL = 1
        private const val ANIMATION_TYPE_SLIDEOFF = 2
        private const val ANIMATION_TYPE_PIP_CLOSE = 3
        private const val ANIMATION_TYPE_SECRET_CLOSE = 4
        private const val ANIMATION_TYPE_CAMERA = 5

        private const val ANIMATOR_REVEAL = 0
        private val OVERSHOOT_INTERPOLATOR: Interpolator = OvershootInterpolator(.97f)
        private val OVERSHOOT_INTERPOLATOR_2: Interpolator = OvershootInterpolator(.82f)

        private const val ANIMATOR_ID_CAPTION = 17

        private const val SET_FULLSCREEN_ON_OPEN = true

        private const val ANIMATOR_CAPTION = 6

        private const val BOTTOM_ANIMATION_DURATION = 150L

        private const val ANIMATOR_PAINT_HIDE = 20
        private const val ANIMATOR_HEADER = 7
        private const val LOAD_THRESHOLD = 4
        private const val LOAD_COUNT = 40
        private const val LOAD_COUNT_PROFILE = 100

        private const val ANIMATOR_PIP = 8

        private const val ANIMATOR_PIP_UP = 9
        private const val ANIMATOR_PIP_HIDE = 12

        private fun calculatePipPosition(offset: Int, full: Int, pipFactor: Float, position: Float): Float {
            return ((offset * -position) + (full * position)) * pipFactor
        }

        private const val ANIMATOR_PIP_POSITION = 11

        private const val ANIMATOR_PIP_CONTROLS = 10
        private const val COUNTER_SCALE = .7f

        private const val ANIMATOR_COUNTER = 1

        private const val ANIMATOR_VIDEO = 5

        private const val ANIMATOR_SLIDE = 15

        private const val ANIMATOR_THUMBS = 21
        private const val THUMBS_PADDING = 9f
        private const val THUMBS_HEIGHT = 43f
        private const val THUMBS_SPACING_BETWEEN = 1f
        private const val THUMBS_SPACING_ADD = 5f
        private const val THUMBS_WIDTH_SMALL = 22f
        private const val THUMBS_WIDTH_BIG = 26f
        private val THUMBS_WIDTH_MAX: Float = THUMBS_HEIGHT * 2f

        private const val ANIMATOR_THUMBS_SCROLL = 22
        private const val ANIMATOR_THUMBS_AUTO_SCROLLER = 23

        private fun getResId(
            @AvatarPickerMode mode: Int,
            defaultResId: Int,
            profileResId: Int, groupResId: Int, channelResId: Int, botResId: Int
        ): Int {
            when (mode) {
                AvatarPickerMode.PROFILE -> return profileResId
                AvatarPickerMode.CHANNEL -> return channelResId
                AvatarPickerMode.GROUP -> return groupResId
                AvatarPickerMode.BOT -> return botResId
            }
            return defaultResId
        }

        private const val ANIMATOR_OTHER = 2

        // Section change
        const val SECTION_CAPTION: Int = 0 // default
        const val SECTION_FILTERS: Int = 1
        const val SECTION_CROP: Int = 2
        const val SECTION_PAINT: Int = 3
        const val SECTION_QUALITY: Int = 4

        private const val CROP_PADDING_HORIZONTAL = 22f

        private fun getSectionTopOffset(section: Int): Int {
            when (section) {
                SECTION_CROP -> {
                    return Screen.getStatusBarHeight() * 2
                }
            }
            return 0
        }

        fun getSectionHeight(section: Int): Int {
            when (section) {
                SECTION_FILTERS -> {
                    return Screen.dp(164f)
                }

                SECTION_CROP -> {
                    return Screen.dp(56f + CROP_PADDING_TOP)
                }

                SECTION_PAINT -> {
                    return Screen.dp(64f) // TODO check
                }

                SECTION_QUALITY -> {
                    return Screen.dp(72f) + Screen.dp(24f)
                }
            }
            return 0
        }

        private const val ANIMATOR_SECTION = 4

        private const val TEST_LOAD_ORIGINAL = false // BuildConfig.DEBUG;

        private const val CROP_PADDING_TOP = 16f

        private const val ANIMATOR_CROP = 18

        private const val CROP_IN_DURATION: Long = 198
        private const val CROP_OUT_DURATION = 160L

        private const val ANIMATOR_IMAGE_ROTATE = 19

        /**/
        private const val ANIMATOR_IMAGE_FLIP_HORIZONTALLY = 192
        private const val ANIMATOR_IMAGE_FLIP_VERTICALLY = 193
        private const val ANIMATOR_ROTATION = 18

        private fun getIconForPaintType(type: Int): Int {
            when (type) {
                PaintMode.PATH -> return R.drawable.baseline_adjust_24
                PaintMode.FREE_MOVEMENT -> return R.drawable.baseline_zoom_out_map_24
                PaintMode.ARROW -> return R.drawable.baseline_arrow_upward_24
                PaintMode.RECTANGLE -> return R.drawable.baseline_crop_3_2_24
            }
            return R.drawable.baseline_bubble_chart_24
        }

        private const val MODE_BACK_PRESS = 0
        private const val MODE_OK = 1
        private const val MODE_CANCEL = 2

        private val PROPORTION_MODES = arrayOf<IntArray>(
            intArrayOf(1, 1, R.id.btn_proportion_square, R.drawable.baseline_crop_square_24),  // Square
            intArrayOf(3, 2, R.id.btn_proportion_3_2, R.drawable.baseline_crop_5_4_24),  // 3:2
            intArrayOf(4, 3, R.id.btn_proportion_4_3, R.drawable.baseline_crop_3_2_24),  // 4:3
            // {7, 5, R.id.btn_proportion_16_10, R.drawable.ic_crop_7_5_black_24dp}, // 16:10
            intArrayOf(16, 9, R.id.btn_proportion_16_9, R.drawable.baseline_crop_16_9_24),  // 16:9
            // 5.0f / 3.0f, // 5:3
            // 5.0f / 4.0f, // 5:4
            // 7.0f / 5.0f, // 7:5

        )

        private const val ANIMATOR_PRIVACY = 14

        private const val SEND_MODE_NONE = 0
        private const val SEND_MODE_FILES = 1
        private const val SEND_MODE_VIDEOS = 2

        // Opening and collecting photos
        @JvmStatic
        fun openFromMedia(context: ViewController<*>, item: MediaItem, filter: SearchMessagesFilter?, forceOpenIn: Boolean) {
            var filter = filter
            var stack: MediaStack? = null

            if (context.isStackLocked()) {
                return
            }
            if (filter == null && item.isGifType()) {
                filter = SearchMessagesFilterAnimation()
            }
            if (context is MediaCollectorDelegate) {
                stack = (context as MediaCollectorDelegate).collectMedias(item.getSourceMessageId(), false, filter)
            }

            if (stack == null) {
                stack = MediaStack(context.context(), context.tdlib())
                stack.set(MediaItem.copyOf(item))
            }

            val args = Args(context, MODE_MESSAGES, stack)
            args.reverseMode = stack.getReverseModeHint(true)
            args.forceThumbs = stack.getForceThumbsHint(true)
            args.forceOpenIn = forceOpenIn || (filter != null && filter.isDocumentFilter())
            args.filter = filter
            if (context is MediaCollectorDelegate) {
                (context as MediaCollectorDelegate).modifyMediaArguments(item, args)
            }

            openWithArgs(context, args)
        }

        private fun openWithArgs(context: ViewController<*>, args: Args): MediaViewController {
            val controller = MediaViewController(context.context(), context.tdlib())
            controller.setArguments(args)
            controller.open()
            return controller
        }

        @JvmStatic
        fun openFromProfile(context: ViewController<*>, user: TdApi.User, delegate: MediaCollectorDelegate?) {
            if (context.isStackLocked()) {
                return
            }

            if (user.profilePhoto == null) {
                return
            }

            val stack: MediaStack?

            stack = MediaStack(context.context(), context.tdlib())
            stack.set(MediaItem(context.context(), context.tdlib(), user.id, user.profilePhoto))

            val args = Args(context, MODE_PROFILE, stack)
            if (delegate != null) {
                delegate.modifyMediaArguments(user, args)
            }

            openWithArgs(context, args)
        }

        @JvmStatic
        fun openFromChat(context: ViewController<*>, chat: Chat, delegate: MediaCollectorDelegate?) {
            if (isUserChat(chat.id)) {
                Companion.openFromProfile(context, context.tdlib()!!.chatUser(chat.id)!!, delegate)
                return
            }

            if (context.isStackLocked()) {
                return
            }

            val item: MediaItem?
            val chatPhotoFull = context.tdlib()!!.chatPhoto(chat.id)
            if (chatPhotoFull != null) {
                item = MediaItem(context.context(), context.tdlib()!!, chat.id, 0, chatPhotoFull)
            } else {
                val chatPhotoInfo = chat.photo
                if (chatPhotoInfo != null) {
                    item = MediaItem(context.context(), context.tdlib()!!, chat.id, chatPhotoInfo)
                } else {
                    return
                }
            }

            val stack = MediaStack(context.context(), context.tdlib()!!)
            stack.set(item)

            val args = Args(context, MODE_CHAT_PROFILE, stack)
            if (delegate != null) {
                delegate.modifyMediaArguments(chat, args)
            }

            openWithArgs(context, args)
        }

        @JvmStatic
        fun openWithStack(context: ViewController<*>, stack: MediaStack?, caption: String?, delegate: MediaCollectorDelegate?, forceThumbs: Boolean) {
            if (context.isStackLocked()) {
                return
            }

            val args = Args(context, MODE_SIMPLE, stack)
            args.caption = caption
            args.forceThumbs = forceThumbs
            if (delegate != null) {
                delegate.modifyMediaArguments(stack, args)
            }

            openWithArgs(context, args)
        }

        @JvmStatic
        fun openFromMessage(message: TGMessage, item: MediaItem) {
            val context = message.controller()!!
            if (context.isStackLocked()) {
                return
            }

            item.setSourceMessage(message)

            val stack: MediaStack?

            stack = MediaStack(context.context(), context.tdlib())
            stack.set(item)

            val args = Args(context, MODE_CHAT_PROFILE, stack)
            args.reverseMode = true
            if (context is MediaCollectorDelegate) {
                (context as MediaCollectorDelegate).modifyMediaArguments(message, args)
            }
            args.noLoadMore = message.isEventLog || message.isSponsoredMessage()
            args.areOnlyScheduled = message.isScheduled

            openWithArgs(context, args)
        }

        @JvmStatic
        fun openFromMessage(msg: TGMessageText) {
            val context = msg.controller()!!
            if (context.isStackLocked()) {
                return
            }

            val parsedWebPage = msg.getParsedLinkPreview()
            if (parsedWebPage == null) {
                return
            }
            val linkPreview = parsedWebPage.getLinkPreview()
            val stack: MediaStack?

            stack = MediaStack(context.context(), context.tdlib())

            val items = parsedWebPage.getInstantItems()
            if (items != null) {
                val files: MutableList<TdApi.File?> = ArrayList<TdApi.File?>()
                for (item in items) {
                    files.add(item.getTargetFile())
                }
                msg.tdlib().files().syncFiles(files, 500L)
                stack.set(parsedWebPage.getInstantPosition(), items)
            } else {
                val item = MediaItem.valueOf(context.context(), context.tdlib(), msg.message)
                if (item == null) {
                    return
                }
                stack.set(item)
            }

            val args = Args(context, MODE_MESSAGES, stack)
            args.noLoadMore = true
            args.copyLink = linkPreview.url
            args.forceThumbs = true
            args.areOnlyScheduled = msg.isScheduled
            if (context is MediaCollectorDelegate) {
                (context as MediaCollectorDelegate).modifyMediaArguments(msg, args)
            }
            args.setTopicId(msg.messagesController().getMessageTopicId())

            openWithArgs(context, args)
        }

        @JvmStatic
        fun openSecret(photo: TGMessageMedia): MediaViewController? {
            val context = photo.controller()!!
            val item = MediaItem.valueOf(context.context(), context.tdlib(), photo.message)
            if (item == null) {
                return null
            }
            val stack = MediaStack(context.context(), context.tdlib())
            stack.set(item)
            item.setSecretPhoto(photo)
            val args = Args(context, MODE_SECRET, stack)
            return openWithArgs(context, args)
        }

        @JvmStatic
        fun openFromMessage(messageContainer: TGMessageMedia, messageId: Long) {
            val context = messageContainer.controller()!!
            val msg = messageContainer.getMessage(messageId)!!
            val item = MediaItem.valueOf(messageContainer, messageId)
            if (item == null) {
                return
            }

            val allowLoadMore = !messageContainer.isSponsoredMessage() && !item.isSecret() && !item.isViewOnce()
            var filter: SearchMessagesFilter? = null
            if (allowLoadMore) {
                when (msg.content.getConstructor()) {
                    TdApi.MessagePhoto.CONSTRUCTOR -> {
                        filter = SearchMessagesFilterPhotoAndVideo()
                    }

                    TdApi.MessageChatChangePhoto.CONSTRUCTOR -> {
                        filter = SearchMessagesFilterChatPhoto()
                    }

                    TdApi.MessageVideo.CONSTRUCTOR -> {
                        filter = SearchMessagesFilterPhotoAndVideo()
                    }

                    TdApi.MessageAnimation.CONSTRUCTOR -> {
                        filter = SearchMessagesFilterAnimation()
                    }

                    MessageText.CONSTRUCTOR -> {
                        filter = SearchMessagesFilterUrl()
                    }

                    TdApi.MessageDocument.CONSTRUCTOR -> {
                        filter = SearchMessagesFilterDocument()
                    }
                }
            }

            var stack: MediaStack? = null

            if (context.isStackLocked()) {
                return
            }
            if (allowLoadMore && context is MediaCollectorDelegate) {
                stack = (context as MediaCollectorDelegate).collectMedias(msg.id, messageContainer.isSponsoredMessage(), filter)
            }

            if (stack == null) {
                stack = MediaStack(context.context(), context.tdlib())
                stack.set(item)
            }

            val args = Args(context, MODE_MESSAGES, stack)
            args.noLoadMore = !allowLoadMore || messageContainer.isEventLog || messageContainer.isSponsoredMessage()
            if (context is MediaCollectorDelegate) {
                (context as MediaCollectorDelegate).modifyMediaArguments(msg, args)
            }
            args.setSearchFilter(filter)
            args.setTopicId(messageContainer.messagesController().getMessageTopicId())
            args.areOnlyScheduled = TD.isScheduled(msg)

            if (messageContainer.isSponsoredMessage()) {
                args.setCustomSubtitle((messageContainer as TGMessage).sponsoredMessage!!.title)
            }

            openWithArgs(context, args)
        }

        /* * */
        private const val ANIMATOR_EDIT_MODE_VISIBILITY = 242
    }
}
