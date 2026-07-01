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
 * File created on 25/12/2016
 */
package org.thunderdog.challegram.ui

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.collection.SparseArrayCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.vkryl.android.animator.FactorAnimator
import me.vkryl.android.widget.FrameLayoutFix.Companion.newParams
import me.vkryl.core.asArray
import me.vkryl.core.ensureCapacity
import me.vkryl.core.equalsOrBothEmpty
import me.vkryl.core.isEmpty
import me.vkryl.core.lambda.CancellableRunnable
import me.vkryl.core.lambda.RunnableData
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import org.drinkless.tdlib.TdApi.*
import org.thunderdog.challegram.R
import org.thunderdog.challegram.component.MediaCollectorDelegate
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.data.InlineResult
import org.thunderdog.challegram.data.TD
import org.thunderdog.challegram.mediaview.MediaViewController
import org.thunderdog.challegram.mediaview.MediaViewDelegate
import org.thunderdog.challegram.mediaview.MediaViewThumbLocation
import org.thunderdog.challegram.mediaview.data.MediaItem
import org.thunderdog.challegram.mediaview.data.MediaStack
import org.thunderdog.challegram.navigation.HeaderView
import org.thunderdog.challegram.navigation.ViewController
import org.thunderdog.challegram.telegram.MessageListener
import org.thunderdog.challegram.telegram.TGLegacyManager
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.telegram.TdlibMessageViewer.Viewport
import org.thunderdog.challegram.telegram.TdlibUi.ChatOpenParameters
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.tool.UI
import org.thunderdog.challegram.tool.Views
import org.thunderdog.challegram.util.CancellableResultHandler
import org.thunderdog.challegram.util.MessageSourceProvider
import org.thunderdog.challegram.v.MediaRecyclerView
import org.thunderdog.challegram.widget.CheckBoxView
import org.thunderdog.challegram.widget.EmptySmartView
import org.thunderdog.challegram.widget.ListInfoView
import org.thunderdog.challegram.widget.SmallChatView
import tgx.td.MessageId
import tgx.td.data.MessageWithProperties
import tgx.td.isSecret
import tgx.td.matchesFilter
import tgx.td.textOrCaption
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.Any
import kotlin.Boolean
import kotlin.CharSequence
import kotlin.Comparator
import kotlin.Float
import kotlin.IllegalArgumentException
import kotlin.Int
import kotlin.LongArray
import kotlin.RuntimeException
import kotlin.String
import kotlin.UnsupportedOperationException
import kotlin.arrayOfNulls
import kotlin.checkNotNull

abstract class SharedBaseController<T : MessageSourceProvider>(context: Context, tdlib: Tdlib?) : ViewController<SharedBaseController.Args>(context, tdlib),
    View.OnClickListener, OnLongClickListener, FactorAnimator.Target, MessageListener, MediaCollectorDelegate, MediaViewDelegate {
    class Args(var chatId: Long, var topicId: MessageTopic?)

    override fun getChatId(): Long {
        return chatId
    }

    @JvmField
    protected var chatId: Long = 0
    @JvmField
    protected var topicId: MessageTopic? = null

    public override fun setArguments(args: Args) {
        super.setArguments(args)
        chatId = args.chatId
        topicId = args.topicId
    }

    var isPrepared: Boolean = false
        private set

    fun setPrepared() {
        isPrepared = true
    }

    override fun getId(): Int {
        return R.id.controller_media__new
    }

    @JvmField
    protected var parent: ProfileController? = null
    @JvmField
    protected var alternateParent: MessagesController? = null

    @JvmField
    protected var recyclerView: MediaRecyclerView? = null
    @JvmField
    protected var adapter: SettingsAdapter? = null

    fun setParent(parent: ProfileController) {
        this.parent = parent
    }

    fun setParent(parent: MessagesController) {
        this.alternateParent = parent
        tdlib!!.listeners().subscribeToMessageUpdates(chatId, this)
    }

    override fun supportsBottomInset(): Boolean {
        return true
    }

    override fun onBottomInsetChanged(extraBottomInset: Int, extraBottomInsetWithoutIme: Int, isImeInset: Boolean) {
        super.onBottomInsetChanged(extraBottomInset, extraBottomInsetWithoutIme, isImeInset)
        Views.applyBottomInset(recyclerView, extraBottomInset)
    }

    private var messageViewport: Viewport? = null

    @SuppressLint("InflateParams")
    override fun onCreateView(context: Context): View {
        checkNotNull(tdlib)
        messageViewport = tdlib.messageViewer().createViewport(MessageSourceSearch(), this)
        recyclerView = Views.inflate(context(), R.layout.recycler_sharedmedia, null) as MediaRecyclerView?
        recyclerView!!.setOverScrollMode(RecyclerView.OVER_SCROLL_NEVER)
        Views.applyBottomInset(recyclerView, extraBottomInset)
        addThemeInvalidateListener(recyclerView)
        if (alternateParent != null) {
            recyclerView!!.setBackgroundColor(Theme.backgroundColor())
            addThemeBackgroundColorListener(recyclerView, ColorId.background)
        }
        recyclerView!!.setHasFixedSize(true)
        recyclerView!!.setLayoutParams(newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        recyclerView!!.setItemAnimator(null) // new CustomItemAnimator(Anim.DECELERATE_INTERPOLATOR, 180l));
        tdlib.ui().attachViewportToRecyclerView(messageViewport!!, recyclerView!!)
        adapter = object : SettingsAdapter(this, if (needsDefaultOnClick()) this else null, this) {
            override fun setInfo(item: ListItem?, position: Int, infoView: ListInfoView) {
                if (isLoading || canLoadMore()) {
                    infoView.showProgress()
                } else {
                    infoView.showInfo(buildTotalCount(if (this@SharedBaseController.isSearching) searchData else data))
                }
            }

            override fun modifyChatView(item: ListItem?, chatView: SmallChatView?, checkBox: CheckBoxView?, isUpdate: Boolean) {
                modifyChatViewIfNeeded(item, chatView, checkBox, isUpdate)
            }
        }
        if (probablyHasEmoji()) {
            TGLegacyManager.instance().addEmojiListener(adapter)
        }
        if (needsDefaultLongPress()) {
            adapter!!.setOnLongClickListener(this)
        }
        if (parent != null) {
            val decoration: RecyclerView.ItemDecoration? = parent!!.newContentDecoration(this)
            if (decoration != null) {
                recyclerView!!.addItemDecoration(decoration)
            }
            parent!!.addOnScrollListener(recyclerView)
        }
        recyclerView!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                loadMoreIfNeeded()
                val items = adapter!!.items
                if (items.size == 1 && items.get(0).viewType == ListItem.TYPE_SMART_EMPTY) {
                    val view = recyclerView.layoutManager!!.findViewByPosition(0)
                    if (view != null) {
                        view.invalidate()
                    }
                }
            }
        })
        onCreateView(context, recyclerView!!, adapter)
        buildCells()
        recyclerView!!.setAdapter(adapter)
        loadInitialChunk()
        return recyclerView!!
    }

    fun getRecyclerView(): RecyclerView? {
        return recyclerView
    }

    fun stopScroll() {
        if (recyclerView != null) {
            recyclerView!!.stopScroll()
        }
    }

    fun onItemsHeightProbablyChanged() {
        if (recyclerView != null && parent != null) {
            parent!!.setIgnoreAnyPagerScrollEvents(true)
            recyclerView!!.invalidateItemDecorations()
            parent!!.checkContentScrollY(this)
            parent!!.setIgnoreAnyPagerScrollEvents(false)
        }
    }

    fun ensureMaxScrollY(scrollY: Int, maxScrollY: Int) {
        if (recyclerView != null) {
            val manager = recyclerView!!.layoutManager as LinearLayoutManager?

            if (scrollY < maxScrollY) {
                manager!!.scrollToPositionWithOffset(0, -scrollY)
                return
            }

            val firstVisiblePosition = manager!!.findFirstVisibleItemPosition()

            if (firstVisiblePosition == 0 || firstVisiblePosition == -1) {
                val view = manager.findViewByPosition(0)
                if (view != null) {
                    var top = view.top
                    if (parent != null) {
                        top -= parent!!.itemsBound
                    }
                    if (top > 0) {
                        manager.scrollToPositionWithOffset(0, -maxScrollY)
                    }
                } else {
                    manager.scrollToPositionWithOffset(0, -maxScrollY)
                }
            }
        }
    }

    protected open fun onCreateView(context: Context?, recyclerView: MediaRecyclerView, adapter: SettingsAdapter?) {
        recyclerView.setLayoutManager(LinearLayoutManager(context, RecyclerView.VERTICAL, false))
    }

    protected open val itemCellHeight: Int
        get() = Screen.dp(72f)

    protected open fun calculateInitialLoadCount(): Int { // override if otherwise needed
        return Screen.calculateLoadingItems(this.itemCellHeight, 10)
    }

    protected abstract fun buildTotalCount(data: ArrayList<T?>?): CharSequence?

    abstract override fun getName(): CharSequence?

    @get:DrawableRes
    abstract val icon: Int

    fun search(query: String?) {
        if (!currentQuery.equalsOrBothEmpty(query)) {
            setInMediaSelectMode(false)
            loadMessages(query, 0, calculateInitialLoadCount())
        }
    }

    fun onGlobalHeightChanged() {
        onItemsHeightProbablyChanged()
    }

    protected open fun calculateScrollY(position: Int): Int {
        if (position == 0) {
            return 0
        }

        var scrollY = 0
        var i = 0

        for (item in adapter!!.items) {
            when (item.viewType) {
                ListItem.TYPE_CUSTOM_INLINE -> {
                    val result = item.data as InlineResult<*>
                    val width = recyclerView!!.measuredWidth
                    if (width != 0) {
                        result.prepare(width)
                    }
                    scrollY += result.height
                }

                ListItem.TYPE_SMART_EMPTY, ListItem.TYPE_SMART_PROGRESS -> {
                    scrollY += measureBaseItemHeight(item.viewType)
                }

                else -> {
                    scrollY += SettingHolder.measureHeightForType(item.viewType)
                }
            }
            i++
            if (i == position) {
                break
            }
        }

        return scrollY
    }

    fun calculateItemsHeight(): Int {
        return calculateScrollY(adapter!!.items.size)
    }

    fun calculateScrollY(): Int {
        if (recyclerView == null) {
            return 0
        }

        val i = (recyclerView!!.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        if (i == -1) {
            return 0
        }

        var scrollY = calculateScrollY(i)
        val view = recyclerView!!.layoutManager!!.findViewByPosition(i)
        if (view != null) {
            scrollY -= view.top
        }

        return scrollY
    }

    protected val headerItemCount: Int
        get() {
            val i = adapter!!.indexOfViewById(R.id.shadowTop)
            return if (i != -1) i + 1 else 0
        }

    // Data load
    private fun loadInitialChunk() {
        loadMessages(null, 0, calculateInitialLoadCount())
    }

    protected open val loadColumnCount: Int
        get() = 0

    private fun loadMoreIfNeeded() {
        if (canLoadMore()) {
            val manager = recyclerView!!.layoutManager as LinearLayoutManager?
            val lastVisibleItemPosition = manager!!.findLastVisibleItemPosition()
            if (lastVisibleItemPosition != -1 && lastVisibleItemPosition + 6 >= adapter!!.items.size) {
                val offsetMessageId = getCurrentOffset(-1)
                if (offsetMessageId != -1L) {
                    var loadCount = 40
                    val columnCount = this.loadColumnCount
                    if (columnCount > 1) {
                        val remaining = loadCount % columnCount
                        if (remaining != 0) {
                            loadCount += columnCount - remaining - 1
                        }
                    }
                    loadMessages(currentQuery, offsetMessageId, loadCount)
                }
            }
        }
    }

    protected val isSearching: Boolean
        get() = (currentQuery != null && !currentQuery!!.isEmpty())

    private fun canLoadMore(): Boolean {
        return if (this.isSearching) canLoadMoreSearch else canLoadMoreData
    }

    private fun setCanLoadMore(canLoadMore: Boolean, notify: Boolean) {
        val prevCanLoadMore = canLoadMore()
        if (prevCanLoadMore != canLoadMore) {
            if (this.isSearching) {
                this.canLoadMoreSearch = canLoadMore
            } else {
                this.canLoadMoreData = canLoadMore
            }
            if (notify) {
                adapter!!.notifyItemChanged(adapter!!.items.size - 1)
            }
        }
    }

    private var isLoading = false
    private var canLoadMoreData = false
    private var canLoadMoreSearch = false
    var currentQuery: String? = null
        private set
    private var searchTask: CancellableRunnable? = null
    private var baseHandler: CancellableResultHandler? = null

    protected open fun buildRequest(chatId: Long, topicId: MessageTopic?, query: String?, offset: Long, secretOffset: String?, limit: Int): TdApi.Function<*>? {
      return if (isEmpty(query) || !isSecret(chatId)) {
        SearchChatMessages(chatId, topicId, query, null, offset, 0, limit, provideSearchFilter())
      } else {
        SearchSecretMessages(chatId, query, secretOffset, limit, provideSearchFilter())
      }
    }

    protected fun performRequest(chatId: Long, topicId: MessageTopic?, query: String?, offset: Long, secretOffset: String?, limit: Int) {
        val function = buildRequest(chatId, topicId, query, offset, secretOffset, limit)
        if (function == null) {
            return
        }
        tdlib!!.client().send(function, Client.ResultHandler { `object`: Object? -> processData(query, offset, `object`!!, limit) })
    }

    private fun loadMessages(query: String?, fromMessageId: Long, limit: Int) {
        if (!this.isLoading || !query.equalsOrBothEmpty(currentQuery)) {
            this.isLoading = true
            val processingSearch = !query.equalsOrBothEmpty(currentQuery)
            this.currentQuery = query

            if (searchTask != null) {
                searchTask!!.cancel()
                searchTask = null
            }

            if (baseHandler != null) {
                baseHandler!!.cancel()
                baseHandler = null
            }

            if (processingSearch) {
                this.searchData = null
                val scrollY = calculateScrollY()
                buildCells()
                recyclerView!!.invalidateItemDecorations()

                /*if (scrollY > 0) {
          // TODO
        }*/
                (recyclerView!!.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                    0,
                    if (parent != null) -parent!!.maxItemsScrollYOffset() else 0
                )
            }

            if (currentQuery == null || currentQuery!!.isEmpty()) {
                if (!processingSearch) {
                    performRequest(chatId, topicId, query, fromMessageId, nextSecretSearchOffset, limit)
                    if (fromMessageId == 0L) {
                        baseHandler = null
                    }
                } else {
                    isLoading = false
                }
            } else {
                if (fromMessageId == 0L) {
                    searchTask = object : CancellableRunnable() {
                        override fun act() {
                            performRequest(chatId, topicId, query, fromMessageId, nextSecretSearchOffset, limit)
                        }
                    }
                    searchTask!!.removeOnCancel(UI.getAppHandler())
                    UI.post(searchTask, 300L)
                } else {
                    performRequest(chatId, topicId, query, fromMessageId, nextSecretSearchOffset, limit)
                }
            }
        }
    }

    protected abstract fun parseObject(`object`: Object?): T?

    protected fun getCurrentOffset(emptyValue: Long): Long {
        return getCurrentOffset(if (this.isSearching) this.searchData else this.data, emptyValue)
    }

    protected open fun getCurrentOffset(data: ArrayList<T?>?, emptyValue: Long): Long {
        return getOffsetMessageId(data, emptyValue)
    }

    protected fun processData(query: String?, offset: Long, `object`: Object, limit: Int) {
        val items: ArrayList<T?>
        var nextSearchOffset: String? = null
        var nextOffset: Long = 0
        when (`object`.getConstructor()) {
            FoundMessages.CONSTRUCTOR -> {
                val messages = `object` as FoundMessages
                items = ArrayList<T?>(messages.messages.size)
                for (message in messages.messages) {
                    if (message == null) {
                        continue
                    }
                    nextOffset = message.id
                    val parsedItem = parseObject(message)
                    if (parsedItem != null) {
                        items.add(parsedItem)
                    }
                }
                nextSearchOffset = messages.nextOffset
                modifyResultIfNeeded(items, true)
            }

            FoundChatMessages.CONSTRUCTOR -> {
                val messages = `object` as FoundChatMessages
                nextOffset = messages.nextFromMessageId
                items = ArrayList<T?>(messages.messages.size)
                for (message in messages.messages) {
                    if (message == null) {
                        continue
                    }
                    val parsedItem = parseObject(message)
                    if (parsedItem != null) {
                        items.add(parsedItem)
                    }
                }
                modifyResultIfNeeded(items, true)
            }

            Messages.CONSTRUCTOR -> {
                val messages = `object` as Messages
                items = ArrayList<T?>(messages.messages.size)
                for (message in messages.messages) {
                    if (message == null) {
                        continue
                    }
                    nextOffset = message.id
                    val parsedItem = parseObject(message)
                    if (parsedItem != null) {
                        items.add(parsedItem)
                    }
                }
                modifyResultIfNeeded(items, true)
            }

            Chats.CONSTRUCTOR -> {
                val chatsIds = (`object` as Chats).chatIds
                val chats = tdlib!!.chats(chatsIds)
                items = ArrayList<T?>(chats.size)
                for (chat in chats) {
                    val parsedItem = parseObject(chat)
                    if (parsedItem != null) {
                        items.add(parsedItem)
                    }
                }
                modifyResultIfNeeded(items, true)
            }

            Users.CONSTRUCTOR -> {
                val userIds = (`object` as Users).userIds
                val users = tdlib!!.cache().users(userIds)
                items = ArrayList<T?>(users.size)
                for (user in users) {
                    val parsedItem = parseObject(user)
                    if (parsedItem != null) {
                        items.add(parsedItem)
                    }
                }
                modifyResultIfNeeded(items, true)
                nextOffset = userIds.size.toLong()
            }

            BasicGroupFullInfo.CONSTRUCTOR -> {
                val groupFull = `object` as BasicGroupFullInfo
                items = ArrayList<T?>(groupFull.members.size)
                for (member in groupFull.members) {
                    val parsedItem = parseObject(member)
                    if (parsedItem != null) {
                        items.add(parsedItem)
                    }
                }
                modifyResultIfNeeded(items, true)
            }

            ChatMembers.CONSTRUCTOR -> {
                val members = `object` as ChatMembers
                items = ArrayList<T?>(members.members.size)
                for (member in members.members) {
                    val parsedItem = parseObject(member)
                    if (parsedItem != null) {
                        items.add(parsedItem)
                    }
                }
                modifyResultIfNeeded(items, true)
                nextOffset = members.members.size.toLong()
            }

            Error.CONSTRUCTOR -> {
                UI.showError(`object`)
                items = ArrayList<T?>(0)
            }

            else -> {
                throw UnsupportedOperationException(`object`.toString())
            }
        }
        val nextOffsetFinal = nextOffset
        val nextSearchOffsetFinal = nextSearchOffset
        tdlib!!.uiExecute(Runnable {
            if (!isDestroyed()) {
                modifyResultIfNeeded(items, false)
                val currentOffset = getCurrentOffset(0)
                if (currentOffset == offset && query.equalsOrBothEmpty(currentQuery)) {
                    addItems(items, nextSearchOffsetFinal, offset == 0L)
                } else if (isEmpty(query) && this.isSearching) {
                    if (data == null || data!!.isEmpty()) {
                        data = items
                    } else {
                        data!!.addAll(items)
                    }
                    canLoadMoreData = !items.isEmpty() && supportsLoadingMore(!isEmpty(query))
                }
            }
        })
    }

    override fun needsTempUpdates(): Boolean {
        return parent != null && parent!!.needsTempUpdates()
    }

    // UI data
    @JvmField
    protected var data: ArrayList<T?>? = null
    @JvmField
    protected var searchData: ArrayList<T?>? = null
    protected var nextSecretSearchOffset: String? = null

    private fun setData(data: ArrayList<T?>?, nextSecretSearchOffset: String?) {
        if (this.isSearching) {
            this.searchData = data
            this.nextSecretSearchOffset = nextSecretSearchOffset
        } else {
            this.data = data
        }
    }

    private fun addItems(newData: ArrayList<T?>, nextSearchOffset: String?, reset: Boolean) {
        val target = if (this.isSearching) searchData else data

        this.isLoading = false

        if (target == null || target.isEmpty() || reset) {
            setCanLoadMore(!newData.isEmpty() && supportsLoadingMore(this.isSearching), false)
            if (target != null && target.isEmpty() && newData.isEmpty()) { // Nothing changed
                return
            }
            setData(newData, nextSearchOffset)
            buildCells()
        } else if (!newData.isEmpty()) {
            setCanLoadMore(supportsLoadingMore(this.isSearching), false)

            val comparator = provideItemComparator()
            if (comparator != null) {
                for (item in newData) {
                    var newIndex = Collections.binarySearch<T?>(target, item, comparator)
                    if (newIndex < 0) {
                        newIndex = newIndex * -1 - 1
                        target.add(newIndex, item)

                        val separatorItem = ListItem(ListItem.TYPE_SEPARATOR)
                        val contentItem = ListItem(provideViewType()).setData(item).setLongId(item!!.getSourceMessageId())

                        if (newIndex == 0) {
                            adapter!!.items.add(2, separatorItem)
                            adapter!!.items.add(2, contentItem)
                            adapter!!.notifyItemRangeInserted(2, 2)
                        } else {
                            val startIndex = 2 + newIndex * 2 - 1
                            adapter!!.items.add(startIndex, contentItem)
                            adapter!!.items.add(startIndex, separatorItem)
                            adapter!!.notifyItemRangeInserted(startIndex, 2)
                        }
                    }
                }
            } else {
                val startIndex = target.size
                target.addAll(newData)
                addItems<T?>(reuse, provideViewType(), target, startIndex, adapter!!.items, adapter, this, buildFlags())
            }

            if (!canLoadMore()) {
                adapter!!.notifyItemChanged(adapter!!.items.size)
            }
        } else {
            setCanLoadMore(false, true)
        }
    }

    private fun buildFlags(): Int {
        var flags = 0
        if (needDateSectionSplitting()) {
            flags = flags or FLAG_NEED_SPLITTING
        }
        if (alternateParent != null) {
            flags = flags or FLAG_USE_FIRST_HEADER_SPACING
        }
        return flags
    }

    private val reuse = ArrayList<ListItem>()

    protected open val emptySmartArgument: String?
        get() = null

    protected open val emptySmartMode: Int
        get() {
            val filter = provideSearchFilter()
            if (filter != null) {
                when (filter.getConstructor()) {
                    SearchMessagesFilterPhotoAndVideo.CONSTRUCTOR -> {
                        return EmptySmartView.MODE_EMPTY_MEDIA
                    }

                    SearchMessagesFilterPhoto.CONSTRUCTOR -> {
                        return EmptySmartView.MODE_EMPTY_PHOTO
                    }

                    SearchMessagesFilterVideo.CONSTRUCTOR -> {
                        return EmptySmartView.MODE_EMPTY_VIDEO
                    }

                    SearchMessagesFilterAnimation.CONSTRUCTOR -> {
                        return EmptySmartView.MODE_EMPTY_GIFS
                    }

                    SearchMessagesFilterVoiceNote.CONSTRUCTOR -> {
                        return EmptySmartView.MODE_EMPTY_VOICE
                    }

                    SearchMessagesFilterVideoNote.CONSTRUCTOR -> {
                        return EmptySmartView.MODE_EMPTY_VIDEO_MESSAGES
                    }

                    SearchMessagesFilterDocument.CONSTRUCTOR -> {
                        return EmptySmartView.MODE_EMPTY_FILES
                    }

                    SearchMessagesFilterAudio.CONSTRUCTOR -> {
                        return EmptySmartView.MODE_EMPTY_MUSIC
                    }

                    SearchMessagesFilterUrl.CONSTRUCTOR -> {
                        return EmptySmartView.MODE_EMPTY_LINKS
                    }
                }
            }
            return 0
        }

    protected open val isAlwaysEmpty: Boolean
        get() = false

    protected fun buildCells() {
        val items = ArrayList<ListItem>()
        val target = if (this.isSearching) searchData else data

        if (this.isAlwaysEmpty || (target != null && target.isEmpty())) {
            if (adapter!!.items.size == 1 && adapter!!.items.get(0).viewType == ListItem.TYPE_SMART_EMPTY) {
                return
            }
            recyclerView!!.setOverScrollMode(View.OVER_SCROLL_NEVER)
            items.add(
                ListItem(ListItem.TYPE_SMART_EMPTY).setIntValue(this.emptySmartMode).setStringValue(
                    this.emptySmartArgument
                ).setBoolValue(tdlib!!.isChannel(chatId))
            ) // FIXME TD.isFollowedChannel(chatId)
        } else if (target == null) {
            recyclerView!!.setOverScrollMode(View.OVER_SCROLL_NEVER)
            if (adapter!!.items.size == 1 && adapter!!.items.get(0).viewType == ListItem.TYPE_SMART_PROGRESS) {
                return
            }
            items.add(ListItem(ListItem.TYPE_SMART_PROGRESS))
        } else {
            // recyclerView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
            addItems<T?>(reuse, provideViewType(), target, 0, items, null, this, buildFlags())
        }
        adapter!!.replaceItems(items)
        onItemsHeightProbablyChanged()
    }

    protected fun measureBaseItemHeight(viewType: Int): Int {
        when (viewType) {
            ListItem.TYPE_SMART_EMPTY, ListItem.TYPE_SMART_PROGRESS -> {
                return recyclerView!!.measuredHeight
            }
        }
        return SettingHolder.measureHeightForType(viewType)
    }

    protected abstract fun provideViewType(): Int

    // Selection
    protected open fun onLongClick(v: View?, item: ListItem?): Boolean {
        return false
    }

    override fun onLongClick(v: View): Boolean {
        val holder = recyclerView!!.getChildViewHolder(v)
        if (holder != null && holder is SettingHolder) {
            val tag = v.tag
            if (tag != null && tag is ListItem) {
                val item = tag
                if (needsCustomLongClickListener()) {
                    return onLongClick(v, item)
                } else {
                    toggleSelected(item)
                    return true
                }
            }
        }
        return false
    }

    var isInMediaSelectMode: Boolean = false
        private set
    private var selectedMessages: MutableMap<String?, MessageWithProperties>? = null

    val selectedMediaCount: Int
        get() = if (selectedMessages != null) selectedMessages!!.size else 0

    @get:StringRes
    val selectedMediaSuffixRes: Int
        get() {
            if (this.selectedMediaCount == 0) return R.string.SelectedSuffix
            val map = TD.calculateCounters(selectedMessages)
            val size = map.size()
            if (size == 2 && map.indexOfKey(MessagePhoto.CONSTRUCTOR) >= 0 && map.indexOfKey(MessageVideo.CONSTRUCTOR) >= 0) {
                return R.string.AttachMediasSuffix
            } else if (size == 1) {
                @MessageContent.Constructors val constructor = map.keyAt(0)
                when (constructor) {
                    MessagePhoto.CONSTRUCTOR -> return R.string.SelectedPhotoSuffix
                    MessageVideo.CONSTRUCTOR -> return R.string.SelectedVideoSuffix
                    MessageAudio.CONSTRUCTOR -> return R.string.SelectedAudioSuffix
                    MessageDocument.CONSTRUCTOR -> return R.string.SelectedFileSuffix
                    MessageText.CONSTRUCTOR -> return R.string.SelectedLinkSuffix
                    MessageAnimation.CONSTRUCTOR -> return R.string.SelectedGifSuffix
                    MessageVoiceNote.CONSTRUCTOR -> return R.string.SelectedVoiceSuffix
                    MessageVideoNote.CONSTRUCTOR -> return R.string.SelectedRoundVideoSuffix
                }
            }
            return R.string.SelectedSuffix
        }

    fun setInMediaSelectMode(inSelectMode: Boolean): Boolean {
        if (this.isInMediaSelectMode != inSelectMode) {
            this.isInMediaSelectMode = inSelectMode
            if (parent != null) {
                parent!!.setInMediaSelectMode(inSelectMode, this.selectedMediaSuffixRes)
            } else if (alternateParent != null) {
                if (inSelectMode) {
                    alternateParent!!.openSelectMode(1)
                } else {
                    alternateParent!!.closeSelectMode()
                }
            }
            if (selectedMessages != null && selectedMessages!!.size > 0 && !inSelectMode) {
                adapter!!.clearSelectedItems()
                selectedMessages!!.clear()
            }
            adapter!!.setInSelectMode(inSelectMode, needSelectableAnimation(), this)
            return true
        }
        return false
    }

    override fun onFactorChanged(id: Int, factor: Float, fraction: Float, callee: FactorAnimator?) {
        if (parent != null) {
            parent!!.setSelectFactor(factor)
        }
    }

    override fun onFactorChangeFinished(id: Int, finalFactor: Float, callee: FactorAnimator?) {
    }

    protected fun toggleSelected(item: ListItem) {
        val messageId = item.longId

        val data = item.data as T?

        if (data == null || messageId == 0L || (item.viewType != ListItem.TYPE_SMALL_MEDIA && item.viewType != ListItem.TYPE_CUSTOM_INLINE)) {
            return
        }

        val message = data.getMessage()
        val isSelected: Boolean
        val key = message.chatId.toString() + "_" + message.id
        if (selectedMessages == null) {
            isSelected = false
            selectedMessages = HashMap<String?, MessageWithProperties>()
        } else {
            isSelected = selectedMessages!!.containsKey(key)
        }

        tdlib!!.getMessageProperties(message, RunnableData { properties: MessageProperties? ->
            runOnUiThreadOptional(Runnable {
                if (isSelected) {
                    selectedMessages!!.remove(key)
                } else {
                    selectedMessages!!.put(key, MessageWithProperties(message, properties!!))
                }
                if (setInMediaSelectMode(!selectedMessages!!.isEmpty())) {
                    if (this.isInMediaSelectMode) {
                        if (parent != null) {
                            parent!!.updateItemsAbility(
                                canCopyMessages(),
                                canDeleteMessages(),
                                canShareMessages(),
                                canClearMessages(),
                                selectedMessages!!.size == 1
                            )
                        } else if (alternateParent != null) {
                            alternateParent!!.updateSelectButtons()
                        }
                    }
                } else {
                    if (parent != null) {
                        parent!!.setSelectedMediaCount(this.selectedMediaCount, this.selectedMediaSuffixRes)
                        parent!!.updateItemsAbility(
                            canCopyMessages(),
                            canDeleteMessages(),
                            canShareMessages(),
                            canClearMessages(),
                            selectedMessages!!.size == 1
                        )
                    } else if (alternateParent != null) {
                        alternateParent!!.setSelectedCount(selectedMessages!!.size)
                        alternateParent!!.updateSelectButtons()
                    }
                }
                item.setSelected(!isSelected)

                val i = adapter!!.indexOfViewByLongId(messageId)
                if (i != -1) {
                    adapter!!.setIsSelected(i, !isSelected, -1)
                }
            })
        })
    }

    protected open fun supportsMessageClearing(message: MessageWithProperties?): Boolean {
        return true
    }

    fun canShareMessages(): Boolean {
        if (selectedMessages != null && !selectedMessages!!.isEmpty()) {
            for (message in selectedMessages!!.values) {
                if (!message.properties.canBeForwarded) return false
            }
            return true
        }
        return false
    }

    fun canDeleteMessages(): Boolean {
        if (selectedMessages != null && !selectedMessages!!.isEmpty()) {
            for (message in selectedMessages!!.values) {
                if (!message.properties.canBeDeletedOnlyForSelf && !message.properties.canBeDeletedForAllUsers) return false
            }
            return true
        }
        return false
    }

    fun deleteMessages() {
        if (canDeleteMessages()) {
            tdlib!!.ui().showDeleteOptions(this, selectedMessages!!.values.toTypedArray<MessageWithProperties>(), Runnable { setInMediaSelectMode(false) })
        }
    }

    fun canCopyMessages(): Boolean {
        return selectedMessages != null && selectedMessages!!.size == 1 && selectedMessages!!.values.iterator()
            .next().properties.canBeSaved && provideSearchFilter()!!.getConstructor() == SearchMessagesFilterUrl.CONSTRUCTOR
    }

    fun copyMessages() {
        if (selectedMessages != null && selectedMessages!!.size == 1) {
            for (message in selectedMessages!!.values) {
                val text = message.message.content.textOrCaption()
                val link = TD.findLink(text)
                if (!isEmpty(link)) {
                    UI.copyText(link, R.string.CopiedLink)
                    setInMediaSelectMode(false)
                }
            }
        }
    }

    fun canClearMessages(): Boolean {
        var hasSomethingToClear = false
        if (selectedMessages != null && !selectedMessages!!.isEmpty()) {
            for (message in selectedMessages!!.values) {
                if (supportsMessageClearing(message) && TD.canDeleteFiles(tdlib(), message.message)) {
                    hasSomethingToClear = true
                    break
                }
            }
        }
        return hasSomethingToClear
    }

    fun clearMessages() {
        if (canClearMessages()) {
            val files = SparseArrayCompat<File?>(selectedMessages!!.size)
            for (message in selectedMessages!!.values) {
                if (!supportsMessageClearing(message)) {
                    continue
                }
                val filesList = TD.getFiles(message.message)
                if (filesList != null) {
                    for (file in filesList) {
                        if (TD.canDeleteFile(message.message, file)) {
                            files.put(file.id, file)
                        }
                    }
                }
            }
            TD.deleteFiles(getParentOrSelf(), asArray<File?>(files, arrayOfNulls<File>(files.size())), Runnable { setInMediaSelectMode(false) })
        }
    }

    val singularMessageId: MessageId?
        get() {
            if (selectedMessages != null && selectedMessages!!.size == 1) {
                for (message in selectedMessages!!.values) {
                    return MessageId(message.message.chatId, message.message.id)
                }
            }
            return null
        }

    fun viewMessages() {
        val messageId = this.singularMessageId
        if (messageId != null) {
            tdlib!!.ui().openChat(this, getChatId(), ChatOpenParameters().passcodeUnlocked().highlightMessage(messageId).ensureHighlightAvailable())
        }
    }

    fun shareMessages() {
        val chat = tdlib!!.chat(chatId)
        if (chat != null && selectedMessages != null && !selectedMessages!!.isEmpty()) {
            val c = ShareController(context, tdlib)
            val messagesWithProperties = selectedMessages!!.values
            val messages = arrayOfNulls<Message>(messagesWithProperties.size)
            var index = 0
            for (message in messagesWithProperties) {
                messages[index] = message.message
                index++
            }
            Arrays.sort<Message?>(messages, Comparator { a: Message?, b: Message? -> java.lang.Long.compare(a!!.id, b!!.id) })
            c.setArguments(ShareController.Args(messages).setAfter(Runnable {
                if (parent != null) {
                    parent!!.clearSelectMode()
                }
            }).setAllowCopyLink(true))
            c.show()
        }
    }

    // Messages
    private fun findBestIndexForId(messageId: kotlin.Long): Int {
        if (data == null) {
            return -1
        }
        if (data!!.isEmpty()) {
            return 0
        }
        var i = 0
        for (item in data) {
            if (messageId > item!!.getSourceMessageId()) {
                return i
            }
            i++
        }
        return -1
    }

    open fun addMessage(message: Message) {
        if (!ProfileController.filterMediaMessage(message) || chatId != message.chatId || !supportsMessageContent()) {
            return
        }

        val filter = provideSearchFilter()

        if (filter == null || !message.matchesFilter(filter)) {
            return
        }

        if (data == null) {
            return
        }

        val alreadyFoundIndex = indexOfMessage(message.id)
        if (alreadyFoundIndex != -1) {
            return
        }

        val addedItem = parseObject(message)
        if (addedItem == null) {
            return
        }

        val bestIndex = findBestIndexForId(message.id)
        if (bestIndex == -1) {
            return
        }

        if (this.isSearching) {
            data!!.add(bestIndex, addedItem)
            return
        }

        val nextItem = if (bestIndex < data!!.size) data!!.get(bestIndex) else null
        val previousItem = if (bestIndex > 0) data!!.get(bestIndex - 1) else null

        val addedDate = addedItem.getSourceDate()
        val addedAnchorMode = TD.getAnchorMode(addedDate)

        val needAddGroup =
            (previousItem == null || TD.getAnchorMode(previousItem.getSourceDate()) != addedAnchorMode || TD.shouldSplitDatesByMonth(
                addedAnchorMode,
                previousItem.getSourceDate(),
                addedDate
            )) &&
                    (nextItem == null || TD.getAnchorMode(nextItem.getSourceDate()) != addedAnchorMode || TD.shouldSplitDatesByMonth(
                        addedAnchorMode,
                        addedDate,
                        nextItem.getSourceDate()
                    ))


        if (data!!.isEmpty()) {
            data!!.add(bestIndex, addedItem)
            buildCells()
            return
        }

        val items = adapter!!.items
        val itemToInsert = ListItem(provideViewType()).setData(addedItem).setLongId(addedItem.getSourceMessageId())
        if (previousItem != null) {
            var previousItemIndex = adapter!!.indexOfViewByLongId(previousItem.getSourceMessageId())
            if (previousItemIndex == -1) {
                return
            }

            data!!.add(bestIndex, addedItem)

            if (needAddGroup) {
                previousItemIndex++ // skipping shadow_bottom of the previous section

                items.add(previousItemIndex, ListItem(ListItem.TYPE_SHADOW_BOTTOM))
                items.add(previousItemIndex, itemToInsert!!)
                items.add(previousItemIndex, ListItem(ListItem.TYPE_SHADOW_TOP))
                items.add(previousItemIndex, ListItem(ListItem.TYPE_HEADER, 0, 0, Lang.getRelativeMonth(addedDate.toLong(), TimeUnit.SECONDS, true), false))

                adapter!!.notifyItemRangeInserted(previousItemIndex, 4)
            } else {
                previousItemIndex++ // skipping previous item

                items.add(previousItemIndex, itemToInsert!!)
                adapter!!.notifyItemInserted(previousItemIndex)
            }

            onItemsHeightProbablyChanged()
        } else if (nextItem != null) {
            var nextItemIndex = adapter!!.indexOfViewByLongId(nextItem.getSourceMessageId())
            if (nextItemIndex == -1) {
                return
            }

            if (needAddGroup) {
                nextItemIndex-- // shadow_top
                nextItemIndex-- // header

                if (nextItemIndex < 0) {
                    return
                }

                data!!.add(bestIndex, addedItem)

                items.add(nextItemIndex, ListItem(ListItem.TYPE_SHADOW_BOTTOM))
                items.add(nextItemIndex, itemToInsert!!)
                items.add(nextItemIndex, ListItem(ListItem.TYPE_SHADOW_TOP))
                items.add(nextItemIndex, ListItem(ListItem.TYPE_HEADER, 0, 0, Lang.getRelativeMonth(addedDate.toLong(), TimeUnit.SECONDS, true), false))

                adapter!!.notifyItemRangeInserted(nextItemIndex, 4)
            } else {
                data!!.add(bestIndex, addedItem)
                items.add(nextItemIndex, itemToInsert!!)
                adapter!!.notifyItemInserted(nextItemIndex)
            }

            onItemsHeightProbablyChanged()
        }
    }

    fun removeMessages(messageIds: LongArray) {
        if (supportsMessageContent()) {
            for (messageId in messageIds) {
                removeMessage(messageId)
            }
        }
    }

    protected fun indexOfMessage(messageId: kotlin.Long): Int {
        if (data == null || !supportsMessageContent()) {
            return -1
        }
        var foundDataIndex = -1
        var i = 0
        for (item in data) {
            if (item!!.getSourceMessageId() == messageId) {
                foundDataIndex = i
                break
            }
            i++
        }
        return foundDataIndex
    }


    private fun removeMessage(messageId: kotlin.Long) {
        if (data == null || !supportsMessageContent()) {
            return
        }

        val foundDataIndex = indexOfMessage(messageId)
        if (foundDataIndex == -1) {
            return
        }

        if (this.isSearching) {
            data!!.removeAt(foundDataIndex)
            return
        }

        val removingItem = data!!.get(foundDataIndex) // Will remove later
        val nextItem = if (foundDataIndex + 1 < data!!.size) data!!.get(foundDataIndex + 1) else null
        val previousItem = if (foundDataIndex > 0) data!!.get(foundDataIndex - 1) else null

        val removedDate = removingItem!!.getSourceDate()
        val removedAnchorMode = TD.getAnchorMode(removedDate)

        val needRemoveGroup =
            (previousItem == null || TD.getAnchorMode(previousItem.getSourceDate()) != removedAnchorMode || TD.shouldSplitDatesByMonth(
                removedAnchorMode,
                previousItem.getSourceDate(),
                removedDate
            )) &&
                    (nextItem == null || TD.getAnchorMode(nextItem.getSourceDate()) != removedAnchorMode || TD.shouldSplitDatesByMonth(
                        removedAnchorMode,
                        removedDate,
                        nextItem.getSourceDate()
                    ))

        val itemIndex = adapter!!.indexOfViewByLongId(messageId)
        if (itemIndex == -1) {
            return
        }

        data!!.removeAt(foundDataIndex)

        val items = adapter!!.items

        if (data!!.isEmpty()) {
            buildCells()
        } else if (needRemoveGroup) {
            items.removeAt(itemIndex + 1) // shadow_bottom
            items.removeAt(itemIndex) // item
            items.removeAt(itemIndex - 1) // shadow_top
            items.removeAt(itemIndex - 2) // header
            adapter!!.notifyItemRangeRemoved(itemIndex - 2, 4)
            adapter!!.updateValuedSettingById(R.id.search_counter)
            onItemsHeightProbablyChanged()
        } else {
            items.removeAt(itemIndex)
            adapter!!.notifyItemRemoved(itemIndex)
            adapter!!.updateValuedSettingById(R.id.search_counter)
            onItemsHeightProbablyChanged()
        }
    }

    fun editMessage(messageId: kotlin.Long, content: MessageContent?) {
        if (data == null || !supportsMessageContent()) {
            return
        }

        val index = indexOfMessage(messageId)
        if (index != -1) {
            data!!.get(index)!!.getMessage().content = content
        }
    }

    override fun destroy() {
        super.destroy()
        if (alternateParent != null) {
            tdlib!!.listeners().unsubscribeFromMessageUpdates(chatId, this)
        }
        if (messageViewport != null) {
            messageViewport!!.performDestroy()
        }
        TGLegacyManager.instance().removeEmojiListener(adapter)
        Views.destroyRecyclerView(recyclerView)
    }

    // API
    protected open fun needSelectableAnimation(): Boolean {
        return false
    }

    protected open fun canSearch(): Boolean {
        return true
    }

    protected open fun needsDefaultOnClick(): Boolean {
        return true
    }

    protected open fun needsDefaultLongPress(): Boolean {
        return true
    }

    protected open fun probablyHasEmoji(): Boolean {
        return false
    }

    protected open val explainedTitle: String?
        get() {
            throw RuntimeException("Stub!")
        }

    protected abstract fun supportsMessageContent(): Boolean

    protected open fun needDateSectionSplitting(): Boolean {
        return true
    }

    protected open fun supportsLoadingMore(isMore: Boolean): Boolean {
        return true
    }

    protected open fun modifyResultIfNeeded(data: ArrayList<T?>?, preparation: Boolean) {
        // override
    }

    protected open fun needsCustomLongClickListener(): Boolean {
        return false
    }

    protected open fun provideItemComparator(): Comparator<T?>? {
        return null
    }

    protected open fun modifyChatViewIfNeeded(item: ListItem?, chatView: SmallChatView?, checkBox: CheckBoxView?, isUpdate: Boolean) {
        // override
    }

    protected open fun provideSearchFilter(): SearchMessagesFilter? {
        throw RuntimeException("Stub!")
    }

    // Independent updates handler
    override fun onNewMessage(message: Message?) {
        if (ProfileController.filterMediaMessage(message!!)) {
            tdlib!!.ui().post(Runnable {
                if (!isDestroyed()) {
                    addMessage(message)
                }
            })
        }
    }

    /*@Override
  public final void __onNewMessages (final TdApi.Message[] messages) {
    boolean found = false;
    for (TdApi.Message message : messages) {
      if (filter(message)) {
        found = true;
        break;
      }
    }
    if (found) {
      tdlib.ui().post(() -> {
        if (!isDestroyed()) {
          for (TdApi.Message message : messages) {
            if (filter(message)) {
              addMessage(message);
            }
          }
        }
      });
    }
  }*/
    override fun onMessageSendSucceeded(message: Message?, oldMessageId: kotlin.Long) {
        tdlib!!.ui().post(Runnable {
            if (!isDestroyed()) {
                addMessage(message!!)
            }
        })
    }

    override fun onMessageContentChanged(chatId: kotlin.Long, messageId: kotlin.Long, newContent: MessageContent?) {
        tdlib!!.ui().post(Runnable {
            if (!isDestroyed() && this@SharedBaseController.chatId == chatId) {
                editMessage(messageId, newContent)
            }
        })
    }

    override fun onMessagesDeleted(chatId: kotlin.Long, messageIds: LongArray?) {
        tdlib!!.ui().post(Runnable {
            if (!isDestroyed() && this@SharedBaseController.chatId == chatId) {
                removeMessages(messageIds!!)
            }
        })
    }

    // Language
    override fun handleLanguagePackEvent(event: Int, arg1: Int) {
        super.handleLanguagePackEvent(event, arg1)
        if (adapter != null) {
            adapter!!.onLanguagePackEvent(event, arg1)
        }
    }

    // Media viewer
    protected open fun toMediaItem(index: Int, item: T?, filter: SearchMessagesFilter?): MediaItem? {
        return null
    }

    override fun collectMedias(fromMessageId: kotlin.Long, isSponsored: Boolean, filter: SearchMessagesFilter?): MediaStack? {
        if (data == null || data!!.isEmpty()) {
            return null
        }

        var items: ArrayList<MediaItem?>? = null

        var foundIndex = -1
        var index = 0
        for (item in data) {
            val copy = toMediaItem(index, item, filter)
            if (copy == null) {
                continue
            }
            if (items == null) {
                items = ArrayList<MediaItem?>()
            }
            if (foundIndex == -1 && copy.sourceMessageId == fromMessageId) {
                foundIndex = index
            }
            items.add(copy)
            index++
        }

        if (foundIndex == -1) {
            return null
        }

        val stack: MediaStack?

        stack = MediaStack(context, tdlib)
        stack.set(foundIndex, items)

        return stack
    }

    override fun modifyMediaArguments(cause: Any?, args: MediaViewController.Args) {
        args.delegate = this
    }

    private val location = MediaViewThumbLocation()

    protected open fun setThumbLocation(location: MediaViewThumbLocation?, view: View?, mediaItem: MediaItem?): Boolean {
        return false
    }

    override fun getTargetLocation(indexInStack: Int, item: MediaItem): MediaViewThumbLocation? {
        val i = adapter!!.indexOfViewByLongId(item.sourceMessageId)
        if (i == -1) {
            return null
        }
        val view = recyclerView!!.layoutManager!!.findViewByPosition(i)
        if (view != null) {
            var viewTop = view.top
            val viewBottom = view.bottom
            val top = viewTop + recyclerView!!.top + HeaderView.getSize(true)
            val bottom = top + view.measuredHeight
            val left = view.left
            val right = view.right

            if (alternateParent == null) {
                viewTop -= SettingHolder.measureHeightForType(ListItem.TYPE_FAKE_PAGER_TOPVIEW)
            }
            val clipTop = if (viewTop < 0) -viewTop else 0
            val clipBottom = if (viewBottom < 0) -viewBottom else 0

            location.set(left, top, right, bottom)
            location.setClip(0, clipTop, 0, clipBottom)

            if (!setThumbLocation(location, view, item)) {
                return null
            }

            return location
        }

        return null
    }

    override fun setMediaItemVisible(index: Int, item: MediaItem?, isVisible: Boolean) {}

    companion object {
        @JvmStatic
        fun isMediaController(c: SharedBaseController<*>?): Boolean {
            return c is SharedCommonController || c is SharedMediaController
        }

        @JvmStatic
        fun valueOf(context: Context?, tdlib: Tdlib?, filter: SearchMessagesFilter): SharedBaseController<*>? {
            when (filter.getConstructor()) {
                SearchMessagesFilterPhotoAndVideo.CONSTRUCTOR, SearchMessagesFilterPhoto.CONSTRUCTOR, SearchMessagesFilterVideo.CONSTRUCTOR, SearchMessagesFilterVideoNote.CONSTRUCTOR, SearchMessagesFilterAnimation.CONSTRUCTOR -> return SharedMediaController(
                    context,
                    tdlib
                ).setFilter(filter)

                SearchMessagesFilterUrl.CONSTRUCTOR, SearchMessagesFilterAudio.CONSTRUCTOR, SearchMessagesFilterDocument.CONSTRUCTOR, SearchMessagesFilterVoiceNote.CONSTRUCTOR -> return SharedCommonController(
                    context,
                    tdlib
                ).setFilter(filter)
            }
            throw IllegalArgumentException("unsupported filter: " + filter)
        }

        protected const val FLAG_NEED_SPLITTING: Int = 1
        protected val FLAG_USE_FIRST_HEADER_SPACING: Int = 1 shl 1

        protected fun <T : MessageSourceProvider?> addItems(
            reuse: MutableList<ListItem>,
            viewType: Int,
            dateItems: ArrayList<T?>,
            offset: Int,
            out: MutableList<ListItem>,
            adapter: SettingsAdapter?,
            controller: SharedBaseController<*>,
            flags: Int
        ): Boolean {
            if (dateItems.isEmpty()) {
                return false
            }

            val needSplitting = (flags and FLAG_NEED_SPLITTING) != 0
            val useFirstHeaderSpacing = (flags and FLAG_USE_FIRST_HEADER_SPACING) != 0

            val currentEndIndex = out.size

            reuse.clear()
            reuse.ensureCapacity(dateItems.size)

            var lastDate = if (offset == 0) -1 else dateItems.get(offset - 1)!!.getSourceDate()
            var lastAnchorMode: Int
            if (offset == 0) {
                lastAnchorMode = -1
            } else {
                lastAnchorMode = TD.getAnchorMode(lastDate)
            }

            val inlineResults = dateItems.get(0) is InlineResult<*>
            val size = dateItems.size
            for (i in offset..<size) {
                val item = dateItems.get(i)
                if (needSplitting) {
                    val sourceDate = item!!.getSourceDate()
                    val anchorMode = TD.getAnchorMode(sourceDate)
                    var forceSeparator = true
                    if (anchorMode != lastAnchorMode || lastDate == -1 || TD.shouldSplitDatesByMonth(anchorMode, lastDate, sourceDate)) {
                        if (i != offset || offset != 0) {
                            reuse.add(ListItem(ListItem.TYPE_SHADOW_BOTTOM))
                        }
                        reuse.add(
                            ListItem(
                                if (offset == 0 && useFirstHeaderSpacing) ListItem.TYPE_HEADER_PADDED else ListItem.TYPE_HEADER,
                                0,
                                0,
                                Lang.getRelativeMonth(sourceDate.toLong(), TimeUnit.SECONDS, true),
                                false
                            )
                        )
                        reuse.add(ListItem(ListItem.TYPE_SHADOW_TOP))

                        lastDate = sourceDate
                        lastAnchorMode = anchorMode
                        forceSeparator = false
                    }
                    if (inlineResults) {
                        (item as InlineResult<*>).setForceSeparator(forceSeparator)
                    }
                } else {
                    if (offset == 0 && i == 0) {
                        val explainedTitle = controller.explainedTitle
                        if (!isEmpty(explainedTitle)) {
                            reuse.add(ListItem(ListItem.TYPE_HEADER, 0, 0, explainedTitle, false))
                        }
                        reuse.add(ListItem(ListItem.TYPE_SHADOW_TOP, R.id.shadowTop))
                    } else {
                        reuse.add(ListItem(ListItem.TYPE_SEPARATOR))
                    }
                }
                reuse.add(ListItem(viewType).setData(item).setLongId(item!!.getSourceMessageId()))
            }

            if (offset == 0) {
                reuse.add(ListItem(ListItem.TYPE_SHADOW_BOTTOM))
                reuse.add(ListItem(ListItem.TYPE_LIST_INFO_VIEW, R.id.search_counter).setIntValue(-1))
                out.addAll(reuse)
                if (adapter != null) {
                    adapter.notifyItemRangeInserted(currentEndIndex, reuse.size)
                    controller.onItemsHeightProbablyChanged()
                }
            } else {
                out.addAll(currentEndIndex - 2, reuse)
                if (adapter != null) {
                    adapter.notifyItemRangeInserted(currentEndIndex - 2, reuse.size)
                    controller.onItemsHeightProbablyChanged()
                }
            }

            return !reuse.isEmpty()
        }

        protected fun getOffsetMessageId(data: ArrayList<out MessageSourceProvider?>?, emptyValue: kotlin.Long): kotlin.Long {
            return if (data == null || data.isEmpty()) emptyValue else data.get(data.size - 1).getSourceMessageId()
        }
    }
}
