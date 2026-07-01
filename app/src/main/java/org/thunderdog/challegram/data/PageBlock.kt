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
@file:OptIn(kotlin.contracts.ExperimentalContracts::class)

package org.thunderdog.challegram.data

import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import me.vkryl.android.util.MultipleViewProvider
import me.vkryl.android.util.ViewProvider
import org.drinkless.tdlib.TdApi
import org.drinkless.tdlib.TdApi.*
import org.thunderdog.challegram.loader.ComplexReceiver
import org.thunderdog.challegram.loader.DoubleImageReceiver
import org.thunderdog.challegram.loader.ImageReceiver
import org.thunderdog.challegram.loader.Receiver
import org.thunderdog.challegram.loader.gif.GifReceiver
import org.thunderdog.challegram.navigation.ViewController
import org.thunderdog.challegram.player.TGPlayerController.PlayListBuilder
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.telegram.TdlibUi.UrlOpenParameters
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Paints
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.tool.Views
import org.thunderdog.challegram.ui.ListItem
import org.thunderdog.challegram.util.DrawableProvider
import org.thunderdog.challegram.util.text.Text
import org.thunderdog.challegram.util.text.TextColorSets
import org.thunderdog.challegram.util.text.TextStyleProvider
import tgx.td.isEmpty
import java.util.*
import kotlin.math.max

abstract class PageBlock(@JvmField protected val context: ViewController<*>?, val originalBlock: TdApi.PageBlock?) {
    @JvmField
    protected var currentViews: MultipleViewProvider

    @JvmField
    protected var mergeBottom: Boolean = false
    @JvmField
    protected var mergeTop: Boolean = false

    fun parent(): ViewController<*>? {
        return context
    }

    private var chatLinkBlock: PageBlock? = null

    var anchor: String? = null
        private set
    var isAnchorOnBottom: Boolean = false
        private set

    fun setAnchor(anchor: String?, isBottom: Boolean) {
        this.anchor = anchor
        this.isAnchorOnBottom = isBottom
    }

    fun belongsToBlock(pageBlock: PageBlock?): Boolean {
        return pageBlock === this || (pageBlock != null && chatLinkBlock === pageBlock)
    }

    open fun requestIcons(receiver: ComplexReceiver) {
        if (chatLinkBlock != null) {
            chatLinkBlock!!.requestIcons(receiver)
        } else {
            receiver.clear()
        }
    }

    @JvmField
    protected var isPost: Boolean = false

    fun isPost(): Boolean {
        return true
    }

    fun setIsPost() {
        this.isPost = true
    }

    open val isClickable: Boolean
        get() = false

    open fun onClick(view: View?, isLongPress: Boolean): Boolean {
        return false
    }

    var listItem: Array<ListItemInfo?>? = null

    @JvmField
    protected var details: PageBlock? = null

    fun setDetails(details: PageBlock?) {
        this.details = details
    }

    protected var detailsHeaderItemCount: Int = 0
    protected var detailsFooterItemCount: Int = 0

    fun setDetailsDecorations(headerItemCount: Int, footerItemCount: Int) {
    }

    fun isChildOf(pageBlock: PageBlock): Boolean {
        var details = this.details
        while (details != null) {
            if (details === pageBlock) return true
            details = details.details
        }
        return false
    }

    val isIndependent: Boolean
        get() = this.listItem == null && details == null && !isPost

    open fun mergeWith(topBlock: PageBlock) {
        mergeTop = true
        topBlock.mergeBottom = true
    }

    val viewProvider: ViewProvider
        get() = currentViews

    fun attachToView(view: View?) {
        currentViews.attachToView(view)
    }

    fun detachFromView(view: View?) {
        currentViews.detachFromView(view)
    }

    abstract val relatedViewType: Int

    protected var maxWidth: Int = 0
        private set
    protected var computedHeight: Int = 0
        private set

    init {
        this.currentViews = MultipleViewProvider()
    }

    fun invalidateHeight(view: View?) {
        val lastWidth = this.maxWidth
        if (lastWidth != 0) {
            this.maxWidth = 0
            val lastHeight = computedHeight
            computedHeight = computeHeight(view, lastWidth)
            if (chatLinkBlock != null) {
                computedHeight = max(chatLinkBlock!!.getHeight(view, lastWidth), computedHeight)
            }
            if (lastHeight != computedHeight) {
                currentViews.requestLayout()
            }
        }
    }

    open fun allowScrolling(): Boolean {
        return false
    }

    fun getHeight(view: View?, width: Int): Int {
        if (width != maxWidth && width != 0) {
            computedHeight = computeHeight(view, width)
            if (chatLinkBlock != null) {
                computedHeight = max(chatLinkBlock!!.getHeight(view, width), computedHeight)
            }
            maxWidth = width
        }
        return computedHeight
    }

    fun initializeLayout(view: View?, parent: View?) {
        // override
    }

    open fun applyLayoutMargins(view: View?, params: FrameLayout.LayoutParams?, viewWidth: Int, viewHeight: Int) {
        // override
    }

    protected abstract fun computeHeight(view: View?, width: Int): Int

    open fun requestPreview(receiver: DoubleImageReceiver?) {}
    open fun requestImage(receiver: ImageReceiver?) {}
    open fun requestGif(receiver: GifReceiver?) {}
    open fun requestFiles(receiver: ComplexReceiver?, invalidate: Boolean) {}
    open val imageContentRadius: Int
        get() = 0

    open fun autoDownloadContent() {}

    fun onTouchEvent(view: View?, e: MotionEvent): Boolean {
        if (chatLinkBlock != null) {
            val deltaY = this.contentTop + this.contentHeight - chatLinkBlock!!.computedHeight
            e.offsetLocation(0f, -deltaY.toFloat())
            if (chatLinkBlock!!.onTouchEvent(view, e)) return true
            e.offsetLocation(0f, deltaY.toFloat())
        }
        return handleTouchEvent(view, e)
    }

    open val customWidth: Int
        get() = -1

    protected abstract fun handleTouchEvent(view: View?, e: MotionEvent?): Boolean

    protected fun getMinimumContentPadding(leftEdge: Boolean): Int {
        return if (isPost) Screen.dp(PageBlockRichText.TEXT_HORIZONTAL_OFFSET) + (if (leftEdge) Screen.dp(12f) else getDefaultContentPadding(leftEdge)) else getDefaultContentPadding(
            leftEdge
        )
    }

    protected open fun getDefaultContentPadding(leftEdge: Boolean): Int {
        return if (this.listItem != null) (if (isPost || !leftEdge) Screen.dp(PageBlockRichText.TEXT_HORIZONTAL_OFFSET) else Screen.dp(10f)) else 0
    }

    protected abstract val contentTop: Int
    protected abstract val contentHeight: Int
    val bulletTop: Int
        get() = this.contentTop

    open fun hasChildAnchor(anchor: String): Boolean {
        return false
    }

    open fun getChildAnchorTop(anchor: String, viewWidth: Int): Int {
        return 0
    }

    open val backgroundColorId: Int
        get() = ColorId.filling

    protected val totalContentPadding: Int
        get() = getMinimumContentPadding(true) - getMinimumContentPadding(false)

    fun <T> draw(view: T?, c: Canvas, preview: Receiver?, receiver: Receiver?, iconReceiver: ComplexReceiver?) where T : View?, T : DrawableProvider? {
        if (isPost) {
            val lineColor = Theme.getColor(ColorId.iv_blockQuoteLine)
            val rectF = Paints.getRectF()
            val lineWidth = Screen.dp(3f)
            val linePadding = Screen.dp(8f) / 2
            val contentLeft = Screen.dp(PageBlockRichText.TEXT_HORIZONTAL_OFFSET)
            val contentTop = this.contentTop
            val contentHeight = this.contentHeight

            // int viewWidth = view.getMeasuredWidth();
            rectF.top = (contentTop - linePadding).toFloat()
            rectF.bottom = (contentTop + linePadding + contentHeight + Screen.dp(if (!mergeBottom) 1.5f else 0f)).toFloat()
            rectF.left = contentLeft.toFloat()
            rectF.right = (contentLeft + lineWidth).toFloat()
            /*if (rtl) {
        rectF.left = viewWidth - contentLeft - lineWidth;
        rectF.right = viewWidth - contentLeft;
      }*/
            c.drawRoundRect(rectF, lineWidth.toFloat() / 2, lineWidth.toFloat() / 2, Paints.fillingPaint(lineColor))

            if (mergeTop) {
                c.drawRect(rectF.left, 0f, rectF.right, rectF.top + lineWidth, Paints.fillingPaint(lineColor))
            }
            if (mergeBottom) {
                c.drawRect(rectF.left, rectF.bottom - lineWidth, rectF.right, view!!.getMeasuredHeight().toFloat(), Paints.fillingPaint(lineColor))
            }
            drawInternal<T?>(view, c, preview, receiver, iconReceiver)
        } else {
            drawInternal<T?>(view, c, preview, receiver, iconReceiver)
        }
        if (chatLinkBlock != null) {
            val saveCount = Views.save(c)
            c.translate(0f, (this.contentTop + this.contentHeight - chatLinkBlock!!.computedHeight).toFloat())
            chatLinkBlock!!.draw<T?>(view, c, null, null, iconReceiver)
            Views.restore(c, saveCount)
        }
    }

    protected abstract fun <T> drawInternal(
        view: T?,
        c: Canvas?,
        preview: Receiver?,
        receiver: Receiver?,
        iconReceiver: ComplexReceiver?
    ) where T : View?, T : DrawableProvider?

    class ListInfo(val list: PageBlockList?) {
        @JvmField
        var maxLabelWidth: Float = 0f
    }

    class ListItemInfo(@JvmField val list: ListInfo?, val itemIndex: Int, label: String?, provider: TextStyleProvider) {
        @JvmField
        val label: Text = Text.Builder(label, Screen.dp(100f), provider, TextColorSets.InstantView.NORMAL).build()

      @JvmField
        var firstBlock: PageBlock? = null
    }

    class ParseContext(val url: String?, instantView: WebPageInstantView, val playListBuilder: PlayListBuilder?) {
        val isRtl: Boolean = instantView.isRtl

      internal var lastBlock: PageBlock? = null
        private var nextAnchor: String? = null
        internal var isCover = false
        internal var isPost = false
        private var isClosed = false
        internal var coverBlock: PageBlock? = null
        internal val viewCount: Int

        internal var openedList: Array<ListItemInfo?>? = null
        internal var detailsBlock: PageBlock? = null

        internal fun processCaption(
            parent: ViewController<*>?,
            mediaBlock: TdApi.PageBlock,
            caption: PageBlockCaption,
            openParameters: UrlOpenParameters?,
            out: ArrayList<PageBlock?>
        ) {
            var captionBlock: PageBlockRichText? = null
            val needMerge = (lastBlock != null && lastBlock!!.originalBlock === mediaBlock) || mediaBlock.getConstructor() == PageBlockEmbeddedPost.CONSTRUCTOR
            if (!caption.text.isEmpty()) {
                captionBlock = PageBlockRichText(parent, mediaBlock, caption, false, isCover, openParameters)
                if (needMerge) {
                    captionBlock.mergeWith(lastBlock!!)
                }
                process(captionBlock, out)
            }
            if (!caption.credit.isEmpty()) {
                val credit = PageBlockRichText(parent, mediaBlock, caption, true, isCover, openParameters)
                if (captionBlock != null || needMerge) {
                    credit.mergeWith((if (captionBlock != null) captionBlock else lastBlock)!!)
                }
                process(credit, out)
            }
        }

        internal var hasKicker = false

        init {
          this.viewCount = instantView.viewCount
        }

        internal fun setClosed(isClosed: Boolean, context: ViewController<*>?, out: ArrayList<PageBlock?>, needOffset: Boolean) {
            if (this.isClosed != isClosed) {
                this.isClosed = isClosed
                if (needOffset && isClosed && !((lastBlock != null && lastBlock!!.originalBlock != null) && (lastBlock!!.originalBlock!!.getConstructor() == PageBlockDetails.CONSTRUCTOR || lastBlock!!.originalBlock!!.getConstructor() == PageBlockChatLink.CONSTRUCTOR))) {
                    processImpl(PageBlockSimple(context, ListItem.TYPE_EMPTY_OFFSET_NO_HEAD, ColorId.filling), out)
                }
                processImpl(PageBlockSimple(context, if (isClosed) ListItem.TYPE_SHADOW_BOTTOM else ListItem.TYPE_SHADOW_TOP, 0), out)
                if (needOffset && !isClosed) {
                    processImpl(PageBlockSimple(context, ListItem.TYPE_EMPTY_OFFSET_NO_HEAD, ColorId.filling), out)
                }
            }
        }

        internal fun process(block: PageBlock, out: ArrayList<PageBlock?>) {
            val pageBlock = block.originalBlock
            if (pageBlock != null) {
                setClosed(
                    !isPost && openedList == null && detailsBlock == null && (pageBlock.getConstructor() == PageBlockFooter.CONSTRUCTOR),
                    block.context,
                    out,
                    pageBlock.getConstructor() != PageBlockChatLink.CONSTRUCTOR
                )
            }
            if (lastBlock != null && (lastBlock !== detailsBlock && ((lastBlock!!.originalBlock != null && lastBlock!!.originalBlock!!.getConstructor() == PageBlockDetails.CONSTRUCTOR) || (lastBlock!!.details != null && lastBlock!!.details !== detailsBlock)))) {
                processImpl(PageBlockSimple(block.context, ListItem.TYPE_EMPTY_OFFSET_NO_HEAD, ColorId.filling), out)
            }
            processImpl(block, out)
        }

        private fun processImpl(block: PageBlock, out: ArrayList<PageBlock?>) {
            if (nextAnchor != null) {
                block.setAnchor(nextAnchor, false)
                nextAnchor = null
            }
            if (isCover && block is PageBlockMedia) {
                block.setIsCover()
            }

            if (isPost) {
                block.setIsPost()
                if (lastBlock != null && lastBlock!!.isPost) {
                    block.mergeWith(lastBlock!!)
                }
            }

            if (openedList != null && block !is PageBlockSimple) {
                if (openedList!![openedList!!.size - 1]!!.firstBlock == null) {
                    openedList!![openedList!!.size - 1]!!.firstBlock = block
                }
                block.listItem = openedList
            }
            if (detailsBlock != null) {
                block.setDetails(detailsBlock)
            }

            out.add(block.also { lastBlock = it })
        }

        internal fun setAnchor(anchor: String?) {
            if (lastBlock != null) {
                lastBlock!!.setAnchor(anchor, true)
            }
            nextAnchor = anchor
        }
    }

    class UnsupportedPageBlockException : Exception()

    companion object {
        @JvmStatic
        @Throws(UnsupportedPageBlockException::class)
        fun parse(
            parent: ViewController<*>,
            url: String?,
            instantView: WebPageInstantView,
            detailsBlock: PageBlock?,
            playListBuilder: PlayListBuilder?,
            urlOpenParameters: UrlOpenParameters?
        ): ArrayList<PageBlock?> {
            val context = ParseContext(url, instantView, playListBuilder)
            context.lastBlock = detailsBlock
            context.detailsBlock = context.lastBlock
            val pageBlocks = if (detailsBlock != null) (detailsBlock.originalBlock as PageBlockDetails).pageBlocks else instantView.pageBlocks
            val needPadding = (detailsBlock != null && pageBlocks.size > 0)
            val out = ArrayList<PageBlock?>(pageBlocks.size)
            if (needPadding) {
                context.process(PageBlockSimple(parent, ListItem.TYPE_EMPTY_OFFSET_NO_HEAD, ColorId.filling), out)
            }
            for (rawPageBlock in pageBlocks) {
                parse(parent, out, context, rawPageBlock, urlOpenParameters)
            }
            if (needPadding) {
                context.process(PageBlockSimple(parent, ListItem.TYPE_EMPTY_OFFSET_NO_HEAD, ColorId.filling), out)
            }
            if (detailsBlock == null) {
                context.setClosed(true, parent, out, true)
                val needReportButton = !Objects.requireNonNull<Tdlib?>(parent.tdlib()).isKnownHost(url, true)
                if (instantView.viewCount > 0 || needReportButton) {
                    // TODO view counter + "Wrong layout?"
                }
            }
            return out
        }

        @Throws(UnsupportedPageBlockException::class)
        private fun parse(
            parent: ViewController<*>?,
            out: ArrayList<PageBlock?>,
            context: ParseContext,
            block: TdApi.PageBlock,
            openParameters: UrlOpenParameters?
        ) {
            when (block.getConstructor()) {
                PageBlockCover.CONSTRUCTOR -> {
                    val cover = block as PageBlockCover
                    context.isCover = true
                    val index = out.size
                    parse(parent, out, context, cover.cover, openParameters)
                    context.isCover = false
                    var i = index
                    while (i < out.size) {
                        if (out.get(i)!!.originalBlock === cover.cover) {
                            context.coverBlock = out.get(i)
                            break
                        }
                        i++
                    }
                }

                PageBlockTitle.CONSTRUCTOR -> {
                    val text = PageBlockRichText(parent, block as PageBlockTitle, out.isEmpty(), context.hasKicker, openParameters)
                    context.process(text, out)
                }

                PageBlockSubtitle.CONSTRUCTOR -> {
                    val text = PageBlockRichText(parent, block as PageBlockSubtitle, openParameters)
                    context.process(text, out)
                }

                PageBlockAuthorDate.CONSTRUCTOR -> {
                    val text = PageBlockRichText(parent, block as PageBlockAuthorDate, context.viewCount, openParameters)
                    context.process(text, out)
                }

                PageBlockKicker.CONSTRUCTOR -> {
                    val text = PageBlockRichText(parent, block as PageBlockKicker, openParameters)
                    context.process(text, out)
                    context.hasKicker = true
                }

                PageBlockHeader.CONSTRUCTOR -> {
                    val text = PageBlockRichText(parent, block as PageBlockHeader, openParameters)
                    context.process(text, out)
                }

                PageBlockSubheader.CONSTRUCTOR -> {
                    val text = PageBlockRichText(parent, block as PageBlockSubheader, openParameters)
                    context.process(text, out)
                }

                PageBlockParagraph.CONSTRUCTOR -> {
                    val text = PageBlockRichText(parent, block as PageBlockParagraph, openParameters)
                    context.process(text, out)
                }

                PageBlockPreformatted.CONSTRUCTOR -> {
                    val text = PageBlockRichText(parent, block as PageBlockPreformatted, openParameters)
                    context.process(text, out)
                }

                PageBlockChatLink.CONSTRUCTOR -> {
                    var added = false
                    if (context.coverBlock != null) {
                        when (context.coverBlock!!.relatedViewType) {
                            ListItem.TYPE_PAGE_BLOCK_MEDIA, ListItem.TYPE_PAGE_BLOCK_GIF -> {
                                val text = PageBlockRichText(parent, block as PageBlockChatLink, true, context.viewCount, openParameters)
                                text.currentViews = context.coverBlock!!.currentViews
                                context.coverBlock!!.chatLinkBlock = text
                                added = true
                            }
                        }
                    }
                    if (!added) {
                        context.process(PageBlockRichText(parent, block as PageBlockChatLink, false, context.viewCount, openParameters), out)
                    }
                }

                PageBlockFooter.CONSTRUCTOR -> {
                    val text = PageBlockRichText(parent, block as PageBlockFooter, context.isPost, openParameters)
                    context.process(text, out)
                }

                PageBlockBlockQuote.CONSTRUCTOR -> {
                    val quoteRaw = block as PageBlockBlockQuote
                    val quote = PageBlockRichText(parent, quoteRaw, false, openParameters)
                    context.process(quote, out)
                    if (!quoteRaw.credit.isEmpty()) {
                        val credit = PageBlockRichText(parent, quoteRaw, true, openParameters)
                        credit.mergeWith(quote)
                        context.process(credit, out)
                    }
                }

                PageBlockPullQuote.CONSTRUCTOR -> {
                    val quoteRaw = block as PageBlockPullQuote
                    val quote = PageBlockRichText(parent, quoteRaw, false, openParameters)
                    context.process(quote, out)
                    if (!quoteRaw.credit.isEmpty()) {
                        val credit = PageBlockRichText(parent, quoteRaw, true, openParameters)
                        credit.mergeWith(quote)
                        context.process(credit, out)
                    }
                }

                PageBlockList.CONSTRUCTOR -> {
                    val listRaw = block as PageBlockList
                    var itemIndex = 0
                    val listInfo = ListInfo(listRaw)
                    for (item in listRaw.items) {
                        val itemInfo = ListItemInfo(listInfo, itemIndex, item.label, PageBlockRichText.getListTextProvider())
                        listInfo.maxLabelWidth = max(listInfo.maxLabelWidth, itemInfo.label.getWidth().toFloat())
                        val lastListItemInfo = context.openedList
                        if (lastListItemInfo == null) {
                            context.openedList = arrayOf<ListItemInfo?>(itemInfo)
                        } else {
                            context.openedList = arrayOfNulls<ListItemInfo>(lastListItemInfo.size + 1)
                            System.arraycopy(lastListItemInfo, 0, context.openedList, 0, lastListItemInfo.size)
                            context.openedList!![lastListItemInfo.size] = itemInfo
                        }
                        val lastSize = out.size
                        for (pageBlock in item.pageBlocks) {
                            parse(parent, out, context, pageBlock, openParameters)
                        }
                        if (out.size - lastSize == 0) { // Empty list item
                            context.process(PageBlockRichText(parent, listRaw, openParameters), out)
                        }
                        context.openedList = lastListItemInfo
                        itemIndex++
                    }
                }

                TdApi.PageBlockTable.CONSTRUCTOR -> {
                    val tableRaw = block as TdApi.PageBlockTable
                    if (!tableRaw.caption.isEmpty()) {
                        context.process(PageBlockRichText(parent, tableRaw, openParameters), out)
                    }
                    context.process(PageBlockTable(parent, tableRaw, openParameters), out)
                }

                PageBlockDetails.CONSTRUCTOR -> {
                    val detailsRaw = block as PageBlockDetails
                    val details = PageBlockRichText(parent, detailsRaw, openParameters)
                    if (detailsRaw.isOpen) {
                        context.process(details, out)
                        val prevDetails = context.detailsBlock
                        context.detailsBlock = details
                        for (pageBlock in detailsRaw.pageBlocks) {
                            parse(parent, out, context, pageBlock, openParameters)
                        }
                        context.detailsBlock = prevDetails
                    } else {
                        context.process(details, out)
                    }
                }

                TdApi.PageBlockDivider.CONSTRUCTOR -> {
                    context.process(PageBlockDivider(parent, block), out)
                }

                PageBlockAnimation.CONSTRUCTOR -> {
                    val mediaRaw = block as PageBlockAnimation
                    val media = PageBlockMedia(parent, mediaRaw)
                    context.process(media, out)
                    context.processCaption(parent, mediaRaw, mediaRaw.caption, openParameters, out)
                }

                PageBlockPhoto.CONSTRUCTOR -> {
                    val mediaRaw = block as PageBlockPhoto
                    val media = PageBlockMedia(parent, mediaRaw, null, openParameters)
                    context.process(media, out)
                    context.processCaption(parent, mediaRaw, mediaRaw.caption, openParameters, out)
                }

                PageBlockMap.CONSTRUCTOR -> {
                    val mapRaw = block as PageBlockMap
                    val map = PageBlockMedia(parent, mapRaw)
                    context.process(map, out)
                    context.processCaption(parent, mapRaw, mapRaw.caption, openParameters, out)
                }

                PageBlockVideo.CONSTRUCTOR -> {
                    val mediaRaw = block as PageBlockVideo
                    val media = PageBlockMedia(parent, mediaRaw)
                    context.process(media, out)
                    context.processCaption(parent, mediaRaw, mediaRaw.caption, openParameters, out)
                }

                PageBlockRelatedArticles.CONSTRUCTOR -> {
                    val relatedRaw = block as PageBlockRelatedArticles
                    context.setClosed(true, parent, out, true)
                    if (!relatedRaw.header.isEmpty()) {
                        context.setClosed(false, parent, out, true)
                        val header = PageBlockRichText(parent, relatedRaw, openParameters)
                        context.process(header, out)
                    } else {
                        context.setClosed(false, parent, out, false)
                    }
                    var index = 0
                    for (related in relatedRaw.articles) {
                        if (index > 0) {
                            context.process(PageBlockSimple(parent, ListItem.TYPE_SEPARATOR_FULL, ColorId.filling), out)
                        }
                        context.process(PageBlockRelatedArticle(parent, relatedRaw, related, openParameters), out)
                        index++
                    }
                    context.setClosed(true, parent, out, false)
                }

                PageBlockCollage.CONSTRUCTOR -> {
                    val collageRaw = block as PageBlockCollage
                    if (collageRaw.pageBlocks.size == 0) {
                        return
                    }
                    var isOk = true
                    for (pageBlock in collageRaw.pageBlocks) {
                        when (pageBlock.getConstructor()) {
                            PageBlockPhoto.CONSTRUCTOR, PageBlockVideo.CONSTRUCTOR, PageBlockAnimation.CONSTRUCTOR -> {
                                continue
                            }
                        }
                        isOk = false
                        break
                    }
                    if (isOk) {
                        val collage = PageBlockMedia(parent, collageRaw)
                        context.process(collage, out)
                        context.processCaption(parent, collageRaw, collageRaw.caption, openParameters, out)
                    }
                }

                PageBlockSlideshow.CONSTRUCTOR -> {
                    val slideshowRaw = block as PageBlockSlideshow

                    if (slideshowRaw.pageBlocks.size == 0) {
                        return
                    }

                    var isOk = true
                    for (pageBlock in slideshowRaw.pageBlocks) {
                        when (pageBlock.getConstructor()) {
                            PageBlockPhoto.CONSTRUCTOR, PageBlockVideo.CONSTRUCTOR, PageBlockAnimation.CONSTRUCTOR -> {
                                continue
                            }
                        }
                        isOk = false
                        break
                    }
                    if (isOk) {
                        val slideshow = PageBlockMedia(parent, slideshowRaw)
                        context.process(slideshow, out)
                        context.processCaption(parent, slideshowRaw, slideshowRaw.caption, openParameters, out)
                    }
                }

                PageBlockAudio.CONSTRUCTOR -> {
                    val audioRaw = block as PageBlockAudio
                    if (audioRaw.audio != null) {
                        val audio = PageBlockFile(parent, audioRaw, context.url, context.playListBuilder)
                        context.process(audio, out)
                    }
                    context.processCaption(parent, audioRaw, audioRaw.caption, openParameters, out)
                }

                PageBlockVoiceNote.CONSTRUCTOR -> {
                    val voiceNoteRaw = block as PageBlockVoiceNote
                    if (voiceNoteRaw.voiceNote != null) {
                        val voiceNote = PageBlockFile(parent, voiceNoteRaw, context.url, context.playListBuilder)
                        context.process(voiceNote, out)
                    }
                    context.processCaption(parent, voiceNoteRaw, voiceNoteRaw.caption, openParameters, out)
                }

                PageBlockAnchor.CONSTRUCTOR -> {
                    val anchor = block as PageBlockAnchor
                    context.setAnchor(anchor.name)
                }

                PageBlockEmbedded.CONSTRUCTOR -> {
                    val embeddedRaw = block as PageBlockEmbedded
                    var embedded: PageBlockMedia? = null
                    if (embeddedRaw.posterPhoto != null) {
                        val service = EmbeddedService.parse(embeddedRaw)
                        if (service != null) {
                            val fakePhoto = PageBlockPhoto(embeddedRaw.posterPhoto, embeddedRaw.caption, null)
                            embedded = PageBlockMedia(parent, fakePhoto, service, null)
                        }
                    }
                    if (embedded == null) {
                        embedded = PageBlockMedia(parent, embeddedRaw)
                    }
                    context.process(embedded, out)
                    context.processCaption(parent, embeddedRaw, embeddedRaw.caption, openParameters, out)
                }

                PageBlockEmbeddedPost.CONSTRUCTOR -> {
                    val postRaw = block as PageBlockEmbeddedPost

                    context.isPost = true
                    context.lastBlock = null

                    val author = PageBlockRichText(parent, postRaw, openParameters)
                    context.process(author, out)
                    for (pageBlock in postRaw.pageBlocks) {
                        parse(parent, out, context, pageBlock, openParameters)
                    }
                    context.processCaption(parent, postRaw, postRaw.caption, openParameters, out)

                    context.isPost = false
                }

                else -> {
                    throw UnsupportedOperationException(block.toString())
                }
            }
        }
    }
}
