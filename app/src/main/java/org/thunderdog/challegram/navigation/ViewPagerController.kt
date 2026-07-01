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
 * File created on 21/11/2016
 */
package org.thunderdog.challegram.navigation

import android.content.Context
import android.graphics.Canvas
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.annotation.IdRes
import androidx.annotation.IntDef
import androidx.collection.SparseArrayCompat
import androidx.core.util.ObjectsCompat
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import me.vkryl.android.widget.FrameLayoutFix
import me.vkryl.android.widget.FrameLayoutFix.Companion.newParams
import me.vkryl.core.collection.LongSparseIntArray
import org.thunderdog.challegram.R
import org.thunderdog.challegram.config.Config
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Paints
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.tool.Views
import org.thunderdog.challegram.unsorted.Size
import org.thunderdog.challegram.util.OptionDelegate
import org.thunderdog.challegram.widget.ViewPager
import org.thunderdog.challegram.widget.rtl.RtlViewPager
import kotlin.math.abs
import kotlin.math.sign

abstract class ViewPagerController<T>(context: Context, tdlib: Tdlib?) : TelegramViewController<T>(context, tdlib), OnPageChangeListener,
    ViewPagerTopView.OnItemClickListener, OptionDelegate, SelectDelegate, Menu, MoreDelegate {
    interface ScrollToTopDelegate {
        fun onScrollToTopRequested()
    }

    private var pager: RtlViewPager? = null
    private var adapter: ViewPagerAdapter? = null
    @JvmField
    protected var headerCell: PagerHeaderView? = null

    override fun getTransformHeaderHeight(): Int {
        return getHeaderHeight()
    }

    protected open val menuButtonsWidth: Int
        get() = 0 // override for performance

    /*@Override
 public boolean onBackPressed (boolean fromTop) {
   int currentPosition = getCurrentPagerItemPosition();
   ViewController<?> currentController = getCachedControllerForPosition(currentPosition);
   if (currentController != null && currentController.onBackPressed(fromTop)) {
     return true;
   }
   SparseArrayCompat<ViewController<?>> controllers = getAllCachedControllers();
   if (controllers != null) {
     for (int i = 0; i < controllers.size(); i++) {
       int position = controllers.keyAt(i);
       if (position != currentPosition) {
         ViewController<?> controller = controllers.valueAt(position);
         if (controller != null && controller.onBackPressed(fromTop)) {
           return true;
         }
       }
     }
   }
   return super.onBackPressed(fromTop);
 }*/
    override fun handleLanguageDirectionChange() {
        super.handleLanguageDirectionChange()
        if (pager != null) {
            pager!!.checkRtl()
        }
        if (this.titleStyle == TITLE_STYLE_COMPACT_BIG && headerCell != null) {
            val textView = headerCell!!.getView().findViewById<TextView?>(R.id.text_title)
            if (Views.setGravity(textView, Gravity.TOP or (if (Lang.rtl()) Gravity.RIGHT else Gravity.LEFT))) {
                val params = (textView!!.getLayoutParams() as FrameLayout.LayoutParams)
                if (Lang.rtl()) {
                    params.rightMargin = Screen.dp(68f)
                    params.leftMargin = 0
                } else {
                    params.leftMargin = Screen.dp(68f)
                    params.rightMargin = 0
                }
                Views.updateLayoutParams(textView)
            }
        }
    }

    public override fun handleLanguagePackEvent(event: Int, arg1: Int) {
        when (event) {
            Lang.EVENT_PACK_CHANGED -> updateHeader()
            Lang.EVENT_STRING_CHANGED ->         // TODO update specific strings?
                updateHeader()

            Lang.EVENT_DIRECTION_CHANGED, Lang.EVENT_DATE_FORMAT_CHANGED -> {}
        }
    }

    private fun updateHeader() {
        if (headerCell != null) {
            val sections = this.pagerSectionItems
            if (sections != null) {
                require(sections.size == this.pagerItemCount) { "sections.size() != " + this.pagerItemCount }
                headerCell!!.getTopView().setItems(sections)
            }
        }
    }

    @get:ColorId
    protected open val drawerReplacementColorId: Int
        get() = ColorId.filling

    abstract override fun supportsBottomInset(): Boolean

    @CallSuper
    override fun onBottomInsetChanged(extraBottomInset: Int, extraBottomInsetWithoutIme: Int, isImeInset: Boolean) {
        super.onBottomInsetChanged(extraBottomInset, extraBottomInsetWithoutIme, isImeInset)
        if (adapter != null) {
            for (c in adapter!!.attachedControllers) {
                c.setBottomInset(extraBottomInset, extraBottomInsetWithoutIme)
            }
        }
    }

    override fun onCreateView(context: Context): View? {
        val contentView: FrameLayoutFix = object : FrameLayoutFix(context) {
            override fun onDraw(c: Canvas) {
                c.drawRect(
                    0f, 0f, getMeasuredWidth().toFloat(), Size.getHeaderDrawerSize().toFloat(), Paints.fillingPaint(
                        Theme.getColor(
                            this@ViewPagerController.drawerReplacementColorId
                        )
                    )
                )
            }
        }
        contentView.setWillNotDraw(false)

        val sections = this.pagerSectionItems
        if (sections != null) {
            require(sections.size == this.pagerItemCount) { "sections.size() != " + this.pagerItemCount }
            when (this.titleStyle) {
                TITLE_STYLE_BIG -> {
                    headerCell = ViewPagerHeaderView(context)
                    (headerCell as ViewPagerHeaderView).initWithController(this)
                }

                TITLE_STYLE_COMPACT, TITLE_STYLE_COMPACT_BIG -> {
                    headerCell = ViewPagerHeaderViewCompact(context)
                    val params = (headerCell as ViewPagerHeaderViewCompact).getRecyclerView().getLayoutParams() as FrameLayout.LayoutParams
                    if (getBackButton() != BackHeaderButton.TYPE_NONE && this.menuButtonsWidth != 0) {
                        if (Lang.rtl()) {
                            params.rightMargin = Screen.dp(56f)
                            params.leftMargin = this.menuButtonsWidth
                        } else {
                            params.leftMargin = Screen.dp(56f)
                            params.rightMargin = this.menuButtonsWidth
                        }
                    }
                    if (useCenteredTitle()) {
                        params.width = ViewGroup.LayoutParams.WRAP_CONTENT
                        params.gravity = Gravity.CENTER_HORIZONTAL
                    }
                    if (this.titleStyle == TITLE_STYLE_COMPACT_BIG) {
                        headerCell!!.getTopView().setItemPadding(Screen.dp(12f))
                        val title = SimpleHeaderView.newTitle(context)
                        title.setTextColor(Theme.headerTextColor())
                        addThemeTextColorListener(title, ColorId.headerText)
                        title.setId(R.id.text_title)
                        Views.setMediumText(title, getName())
                        (headerCell as ViewPagerHeaderViewCompact).addView(title)
                    }
                }
            }
            headerCell!!.getTopView().setOnItemClickListener(this)
            headerCell!!.getTopView().setItems(sections)
            addThemeInvalidateListener(headerCell!!.getTopView())
        }

        val params = newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        when (this.titleStyle) {
            TITLE_STYLE_BIG, TITLE_STYLE_COMPACT_BIG -> params.topMargin = Size.getHeaderDrawerSize()
            TITLE_STYLE_COMPACT -> params.topMargin = 0
        }

        adapter = ViewPagerAdapter(context, this)
        addAttachStateListener(attachListener)
        addFocusListener(focusListener)
        pager = RtlViewPager(context)
        pager!!.setLayoutParams(params)
        pager!!.setOverScrollMode(if (Config.HAS_NICE_OVER_SCROLL_EFFECT) View.OVER_SCROLL_IF_CONTENT_SCROLLS else View.OVER_SCROLL_NEVER)
        pager!!.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                var positionOffset = positionOffset
                positionOffset = ViewPager.clampPositionOffset(positionOffset)
                val needUpdateAttachState = currentPosition != position || (currentPositionOffset == 0f) != (positionOffset == 0f)
                currentPosition = position
                currentPositionOffset = positionOffset
                if (needUpdateAttachState) {
                    updateAttachedControllers()
                }
                context().checkDisallowScreenshots()
            }

            override fun onPageSelected(position: Int) {}

            override fun onPageScrollStateChanged(state: Int) {}
        })
        pager!!.addOnPageChangeListener(this)
        pager!!.setAdapter(adapter)
        if (!overridePagerParent()) {
            contentView.addView(pager)
        }

        onCreateView(context, contentView, pager)

        val startPosition = adapter!!.reversePosition(0)
        if (startPosition != 0) {
            pager!!.setCurrentItem(startPosition)
        }

        return contentView
    }

    internal fun updateControllerState(c: ViewController<*>, position: Int) {
        val attachState = c.attachState
        var desiredAttachState = this.attachState && adapter!!.attachedControllers.contains(c)

        if (desiredAttachState) {
            if (currentPositionOffset == 0f || abs(currentPositionOffset) == 1f) {
                desiredAttachState = (currentPosition + currentPositionOffset).toInt() == position
            } else {
                desiredAttachState = position == currentPosition || position.toFloat() == currentPosition + sign(currentPositionOffset)
            }
        }

        val isFocused = c.isFocused()
        var desiredFocusState = desiredAttachState && isFocused()
        if (desiredFocusState) {
            desiredFocusState = currentPositionOffset == 0f && position == currentPosition
        }

        if (isFocused != desiredFocusState && !desiredFocusState) {
            c.onBlur()
        }
        if (attachState != desiredAttachState) {
            c.onAttachStateChanged(navigationController, desiredAttachState)
        }
        if (isFocused != desiredFocusState && desiredFocusState) {
            c.onFocus()
        }
    }

    private val attachListener: AttachListener = AttachListener { context: ViewController<*>?, navigation: NavigationController?, isAttached: Boolean ->
        updateAttachedControllers()
    }

    private val focusListener: FocusStateListener = FocusStateListener { c: ViewController<*>?, isFocused: Boolean ->
        updateAttachedControllers()
    }

    private fun updateAttachedControllers() {
        for (c in adapter!!.attachedControllers) {
            updateControllerState(c, adapter!!.getControllerPosition(c))
        }
    }

    protected open fun overridePagerParent(): Boolean {
        return false
    }

    override fun getViewForApplyingOffsets(): View? {
        when (this.titleStyle) {
            TITLE_STYLE_BIG, TITLE_STYLE_COMPACT_BIG -> return null
            TITLE_STYLE_COMPACT -> return pager
        }
        return null
    }

    fun changeName(newName: CharSequence?) {
        if (this.titleStyle == TITLE_STYLE_COMPACT_BIG && headerCell != null) {
            val view = headerCell!!.getView().findViewById<TextView?>(R.id.text_title)
            if (view != null) {
                Views.setMediumText(view, newName)
            }
        }
    }

    val viewPager: ViewPager?
        get() = pager

    override fun onEnterSelectMode() {
        super.onEnterSelectMode()
        this.viewPager!!.setPagingEnabled(false)
        this.currentPagerItem!!.onEnterSelectMode()
    }

    override fun onLeaveSelectMode() {
        super.onLeaveSelectMode()
        this.viewPager!!.setPagingEnabled(true)
        this.currentPagerItem!!.onLeaveSelectMode()
    }

    override fun getSelectMenuId(): Int {
        val c = this.currentPagerItem
        return if (c != null) c.getSelectMenuId() else super.getSelectMenuId()
    }

    override fun finishSelectMode(position: Int) {
        val c = this.currentPagerItem
        if (c is SelectDelegate) {
            (c as SelectDelegate).finishSelectMode(position)
        }
    }

    override fun fillMenuItems(id: Int, header: HeaderView?, menu: LinearLayout?) {
        val c = this.currentPagerItem
        if (c is Menu) {
            (c as Menu).fillMenuItems(id, header, menu)
        }
    }

    override fun onMoreItemPressed(id: Int) {
        val c = this.currentPagerItem
        if (c is MoreDelegate) {
            (c as MoreDelegate).onMoreItemPressed(id)
        }
    }

    override fun onMenuItemPressed(id: Int, view: View?) {
        val c = this.currentPagerItem
        if (c is Menu) {
            (c as Menu).onMenuItemPressed(id, view)
        }
    }

    override fun getCustomHeaderCell(): View? {
        return if (headerCell != null) headerCell!!.getView() else null
    }

    override fun getHeaderHeight(): Int {
        when (this.titleStyle) {
            TITLE_STYLE_BIG, TITLE_STYLE_COMPACT_BIG -> return Size.getHeaderPortraitSize() + Size.getHeaderDrawerSize()
            TITLE_STYLE_COMPACT -> return Size.getHeaderPortraitSize()
        }
        return super.getHeaderHeight()
    }

    private var scrollState = 0

    @IntDef(TITLE_STYLE_COMPACT, TITLE_STYLE_BIG, TITLE_STYLE_COMPACT_BIG)
    @Retention(AnnotationRetention.SOURCE)
    annotation class TitleStyle

    @get:TitleStyle
    protected open val titleStyle: Int
        get() = TITLE_STYLE_COMPACT

    /*protected final boolean useBigTitle () {
    return false;
  }*/
    protected open fun useCenteredTitle(): Boolean {
        return false
    }

    override fun onPageScrollStateChanged(state: Int) {
        scrollState = state
        if (state != ViewPager.SCROLL_STATE_SETTLING && headerCell != null) {
            headerCell!!.getTopView().resetFromTo()
        }
    }

    /**
     * @param position position from logic perspective
     * @param actualPosition position from viewPager perspective
     */
    open fun onPageSelected(position: Int, actualPosition: Int) {
        // override
    }

    override fun onPageSelected(position: Int) {
        onPageSelected(adapter!!.reversePosition(position), position)
        context.notifyBackPressAvailabilityChanged()
    }

    var isDisallowKeyboardHideOnPageScrolled: Boolean = false

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        var positionOffset = positionOffset
        positionOffset = ViewPager.clampPositionOffset(positionOffset)
        if (headerCell != null) {
            headerCell!!.getTopView().setSelectionFactor(position.toFloat() + positionOffset)
        }
        onPageScrolled(adapter!!.reversePosition(position), position, positionOffset, positionOffsetPixels)
        if (getKeyboardState() && !this.isDisallowKeyboardHideOnPageScrolled) {
            hideSoftwareKeyboard()
        }
    }

    open fun onPageScrolled(position: Int, actualPosition: Int, actualPositionOffset: Float, actualPositionOffsetPixels: Int) {
        // override
    }

    val isAtFirstPosition: Boolean
        get() = adapter!!.reversePosition(pager!!.getCurrentItem()) == 0

    @JvmOverloads
    protected fun replaceController(itemId: Long, newController: ViewController<*>, notifyAdapter: Boolean = true) {
        val position = getPagerItemPosition(itemId)
        if (position != NO_POSITION) {
            val currentController = adapter!!.getCachedItemByPosition(position)
            if (currentController != null) {
                currentController.destroy()
            }
            newController.setParentWrapper(this)
            newController.bindThemeListeners(this)
            adapter!!.cachedItems.put(position, newController)
            adapter!!.cachedPositions.put(itemId, position)
            if (notifyAdapter) {
                adapter!!.notifyDataSetChanged()
            }
        } else {
            newController.destroy()
        }
    }

    protected fun notifyPagerItemsChanged() {
        adapter!!.notifyDataSetChanged()
    }

    fun scrollToFirstPosition(): Boolean {
        if (!this.isAtFirstPosition) {
            setCurrentPagerPosition(adapter!!.reversePosition(0),  /* animated */true)
            return true
        }
        return false
    }

    val currentPagerItemPosition: Int
        get() = adapter!!.reversePosition(pager!!.getCurrentItem())

    val currentPagerItemId: Long
        get() = getPagerItemId(this.currentPagerItemPosition)

    fun notifyPagerItemPositionsChanged() {
        if (headerCell != null) {
            updateHeader()
        }
        if (adapter != null) {
            val itemCount = this.pagerItemCount
            val cachedItems = SparseArrayCompat<ViewController<*>?>(itemCount)
            val cachedPositions = LongSparseIntArray(itemCount)
            for (position in 0..<itemCount) {
                val itemId = getPagerItemId(position)
                val oldPosition = adapter!!.getCachedItemPosition(itemId)
                if (oldPosition != NO_POSITION) {
                    val cachedItem = adapter!!.getCachedItemByPosition(oldPosition)
                    cachedItems.put(position, cachedItem)
                    cachedPositions.put(itemId, position)
                    adapter!!.cachedItems.remove(oldPosition)
                    adapter!!.cachedPositions.delete(itemId)
                }
            }
            if (inTransformMode() && headerView != null) {
                val currentPosition = adapter!!.reversePosition(pager!!.getCurrentItem())
                for (index in 0..<adapter!!.cachedItems.size()) {
                    val position = adapter!!.cachedItems.keyAt(index)
                    if (currentPosition == position) {
                        if (headerView!!.inSelectMode()) {
                            headerView!!.finishSelectMode()
                        } else if (headerView!!.inSearchMode()) {
                            headerView!!.closeSearchMode(true,  /* after */null)
                        } else if (headerView!!.inCustomMode()) {
                            headerView!!.closeCustomMode()
                        }
                        break
                    }
                }
            }
            adapter!!.destroyCachedItems()
            adapter!!.cachedItems = cachedItems
            adapter!!.cachedPositions = cachedPositions
            adapter!!.notifyDataSetChanged()
            if (headerCell != null) {
                headerCell!!.getTopView().setSelectionFactor(pager!!.getCurrentItem().toFloat())
            }
        }
    }

    val currentPagerItem: ViewController<*>?
        get() = getCachedControllerForPosition(this.currentPagerItemPosition)

    private var currentPosition = 0
    private var currentPositionOffset = 0f

    override fun shouldDisallowScreenshots(): Boolean {
        if (super.shouldDisallowScreenshots()) {
            return true
        }
        if (isSearchAntagonistHidden) {
            return false
        }
        val controller = this.currentPagerItem
        if (controller != null && controller.shouldDisallowScreenshots()) {
            return true
        }
        if (currentPositionOffset != 0f) {
            val targetPosition = currentPosition + (if (currentPositionOffset > 0f) 1 else -1)
            val position = adapter!!.reversePosition(targetPosition)
            val otherController = getCachedControllerForPosition(position)
            if (otherController != null && otherController.shouldDisallowScreenshots()) {
                return true
            }
        }
        return super.shouldDisallowScreenshots()
    }

    protected open fun setCurrentPagerPosition(position: Int, animated: Boolean) {
        if (headerCell != null && animated) {
            headerCell!!.getTopView().setFromTo(pager!!.getCurrentItem(), position)
        }
        pager!!.setCurrentItem(position, animated)
    }

    override fun onOptionItemPressed(optionItemView: View?, id: Int): Boolean {
        val c = adapter!!.getCachedItemByPosition(pager!!.getCurrentItem())
        return c is OptionDelegate && (c as OptionDelegate).onOptionItemPressed(optionItemView, id)
    }

    fun getPagerItemPosition(itemId: Long): Int {
        if (adapter != null) {
            val cachedItemPosition = adapter!!.getCachedItemPosition(itemId)
            if (cachedItemPosition != NO_POSITION) {
                return cachedItemPosition
            }
        }
        val pagerItemCount = this.pagerItemCount
        for (position in 0..<pagerItemCount) {
            if (getPagerItemId(position) == itemId) {
                return position
            }
        }
        return NO_POSITION
    }

    fun getCachedControllerForId(@IdRes id: Int): ViewController<*>? {
        return if (adapter != null) adapter!!.getCachedItemByControllerId(id) else null
    }

    fun getCachedControllerForItemId(itemId: Long): ViewController<*>? {
        return if (adapter != null) adapter!!.getCachedItemByItemId(itemId) else null
    }

    fun getCachedControllerForPosition(position: Int): ViewController<*>? {
        return if (adapter != null) adapter!!.getCachedItemByPosition(position) else null
    }

    val allCachedControllers: SparseArrayCompat<ViewController<*>?>?
        get() = if (adapter != null) adapter!!.cachedItems else null

    fun getPreparedControllerForPosition(position: Int): ViewController<*> {
        if (adapter == null) getValue()
        val c = adapter!!.prepareViewController(position)
        c.getValue()
        return c
    }

    fun prepareControllerForPosition(position: Int, after: Runnable?) {
        var position = position
        if (adapter != null && adapter!!.cachedItems.get(adapter!!.reversePosition(position).also { position = it }) == null) {
            val c = adapter!!.prepareViewController(position)
            if (c != null) {
                if (after != null) {
                    c.postOnAnimationExecute(after)
                }
                c.getValue()
                return
            }
        }
        if (after != null) {
            after.run()
        }
    }

    private var cachedPagerSectionItems: MutableList<ViewPagerTopView.Item?>? = null

    protected abstract val pagerItemCount: Int
    protected open fun getPagerItemId(position: Int): Long {
        return position.toLong()
    }

    protected abstract fun onCreateView(context: Context?, contentView: FrameLayoutFix?, pager: ViewPager?)
    protected abstract fun onCreatePagerItemForPosition(context: Context?, position: Int): ViewController<*>?
    protected abstract val pagerSections: Array<out CharSequence?>?
    protected open val pagerSectionItems: MutableList<ViewPagerTopView.Item?>?
        get() {
            val pagerSections = this.pagerSections
            if (pagerSections == null) {
                return null.also { cachedPagerSectionItems = it }
            }
            if (pagerSections.size == 0) {
                return mutableListOf<ViewPagerTopView.Item?>().also { cachedPagerSectionItems = it }
            }
            if (cachedPagerSectionItems != null && cachedPagerSectionItems!!.size == pagerSections.size) {
                var hasChanges = false
                for (i in pagerSections.indices) {
                    if (!ObjectsCompat.equals(pagerSections[i], cachedPagerSectionItems!!.get(i)!!.string)) {
                        hasChanges = true
                        break
                    }
                }
                if (!hasChanges) {
                    return cachedPagerSectionItems
                }
            }
            val pagerSectionItems: MutableList<ViewPagerTopView.Item?> = ArrayList<ViewPagerTopView.Item?>(pagerSections.size)
            for (pagerSection in pagerSections) {
                pagerSectionItems.add(ViewPagerTopView.Item(pagerSection))
            }
            cachedPagerSectionItems = pagerSectionItems
            return pagerSectionItems
        }

    override fun onPagerItemClick(index: Int) {
        if (this.currentPagerItemPosition == index) {
            val c = adapter!!.getCachedItemByPosition(pager!!.getCurrentItem())
            if (c is ScrollToTopDelegate) {
                (c as ScrollToTopDelegate).onScrollToTopRequested()
            }
        } else if (pager!!.isPagingEnabled()) {
            setCurrentPagerPosition(index, true)
        }
    }

    override fun destroy() {
        super.destroy()
        if (adapter != null) {
            adapter!!.destroyCachedItems()
        }
    }

    override fun canSlideBackFrom(navigationController: NavigationController, x: Float, y: Float): Boolean {
        return y <= HeaderView.getTopOffset() + getHeaderHeight() - Size.getHeaderDrawerSize() || (pager != null && (this.isAtFirstPosition /*|| !pager.isPagingEnabled()*/) && scrollState == ViewPager.SCROLL_STATE_IDLE)
    }

    class ViewPagerAdapter(private val context: Context?, private val parent: ViewPagerController<*>) : PagerAdapter() {
        /*final*/ var cachedItems: SparseArrayCompat<ViewController<*>?>
        /*final*/ var cachedPositions: LongSparseIntArray

        fun getCachedItemPosition(itemId: Long): Int {
            return cachedPositions.get(itemId, NO_POSITION)
        }

        fun getCachedItemByPosition(position: Int): ViewController<*>? {
            return if (position != NO_POSITION) cachedItems.get(position) else null
        }

        fun getCachedItemByItemId(itemId: Long): ViewController<*>? {
            return getCachedItemByPosition(getCachedItemPosition(itemId))
        }

        fun getCachedItemByControllerId(@IdRes id: Int): ViewController<*>? {
            val size = cachedItems.size()
            for (i in 0..<size) {
                val c = cachedItems.valueAt(i)!!
                if (c.getId() == id) {
                    return c
                }
            }
            return null
        }

        override fun getCount(): Int {
            return parent.pagerItemCount
        }

        val attachedControllers: MutableSet<ViewController<*>> = HashSet<ViewController<*>>()

        init {
            val itemCount = parent.pagerItemCount
            this.cachedItems = SparseArrayCompat<ViewController<*>?>(itemCount)
            this.cachedPositions = LongSparseIntArray(itemCount)
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            val c = `object` as ViewController<*>
            container.removeView(c.getValue())
            attachedControllers.remove(c)
            parent.updateControllerState(c, position)
        }

        fun destroyCachedItems() {
            val count = cachedItems.size()
            for (i in 0..<count) {
                val c = cachedItems.valueAt(i)!!
                if (!c.isDestroyed()) {
                    c.destroy()
                }
            }
            cachedItems.clear()
            cachedPositions.clear()
        }

        fun reversePosition(position: Int): Int {
            return position // FIXME RTL Lang.rtl() ? getCount() - position - 1 : position;
        }

        override fun getItemPosition(`object`: Any): Int {
            if (`object` is ViewController<*>) {
                return getControllerPosition(`object`)
            } else {
                return POSITION_NONE
            }
        }

        fun getControllerPosition(controller: ViewController<*>?): Int {
            val count = cachedItems.size()
            for (i in 0..<count) {
                if (cachedItems.valueAt(i) === controller) {
                    return reversePosition(cachedItems.keyAt(i))
                }
            }
            return POSITION_NONE
        }

        fun prepareViewController(position: Int): ViewController<*> {
            var c = cachedItems.get(position)
            if (c == null) {
                c = parent.onCreatePagerItemForPosition(context, position)
                c!!.setParentWrapper(parent)
                c.bindThemeListeners(parent)
                cachedItems.put(position, c)
                cachedPositions.put(parent.getPagerItemId(position), position)
            }
            return c
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val c = prepareViewController(reversePosition(position))
            container.addView(c.getValue())
            c.setBottomInset(parent.extraBottomInset, parent.extraBottomInsetWithoutIme)
            attachedControllers.add(c)
            parent.updateControllerState(c, position)
            if ((position == parent.currentPosition || (parent.currentPositionOffset != 0f && position == parent.currentPosition + (if (parent.currentPositionOffset > 0f) 1 else -1))) && c.shouldDisallowScreenshots()) {
                parent.context().checkDisallowScreenshots()
            }
            return c
        }

        override fun isViewFromObject(view: View, `object`: Any): Boolean {
            return `object` is ViewController<*> && `object`.wrapUnchecked === view
        }
    }

    companion object {
        @JvmField
        protected val NO_POSITION: Int = -1

        protected const val TITLE_STYLE_COMPACT: Int = 1
        protected const val TITLE_STYLE_BIG: Int = 2
        protected const val TITLE_STYLE_COMPACT_BIG: Int = 3
    }
}
