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
 * File created on 16/11/2016
 */
package org.thunderdog.challegram.ui

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import me.vkryl.android.DECELERATE_INTERPOLATOR
import me.vkryl.android.widget.FrameLayoutFix
import me.vkryl.android.widget.FrameLayoutFix.Companion.newParams
import org.thunderdog.challegram.Log
import org.thunderdog.challegram.R
import org.thunderdog.challegram.component.attach.CustomItemAnimator
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.navigation.BackHeaderButton
import org.thunderdog.challegram.navigation.HeaderView
import org.thunderdog.challegram.navigation.Menu
import org.thunderdog.challegram.navigation.RecyclerViewProvider
import org.thunderdog.challegram.navigation.TelegramViewController
import org.thunderdog.challegram.navigation.ViewPagerController.ScrollToTopDelegate
import org.thunderdog.challegram.support.ViewSupport
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.tool.Views
import org.thunderdog.challegram.v.CustomRecyclerView
import org.thunderdog.challegram.widget.DoneButton

abstract class RecyclerViewController<T>(context: Context, tdlib: Tdlib?) : TelegramViewController<T>(context, tdlib), RecyclerViewProvider,
    ScrollToTopDelegate, Menu {
    protected override fun getBackButton(): Int {
        return BackHeaderButton.TYPE_BACK
    }

    protected abstract fun onCreateView(context: Context, recyclerView: CustomRecyclerView)

    open var recyclerView: CustomRecyclerView? = null
        protected set

    @get:ColorId
    protected open val recyclerBackground: Int
        get() = ColorId.background

    private var disableSettling = false

    fun setDisableSettling(disableSettling: Boolean) {
        this.disableSettling = disableSettling
    }

    private var scrollState = 0
    private var prevScrollState = 0
    private var savedRecyclerPosition = RecyclerView.NO_POSITION
    private var savedRecyclerPositionOffset = 0

    protected fun saveRecyclerPosition() {
        val manager = (this.recyclerView!!.getLayoutManager() as LinearLayoutManager?)
        if (manager != null) {
            savedRecyclerPosition = manager.findFirstVisibleItemPosition()
            val view = manager.findViewByPosition(savedRecyclerPosition)
            savedRecyclerPositionOffset = if (view != null) view.getTop() else 0
        } else {
            savedRecyclerPosition = RecyclerView.NO_POSITION
            savedRecyclerPositionOffset = 0
        }
    }

    protected fun applySavedRecyclerPosition() {
        if (savedRecyclerPosition != RecyclerView.NO_POSITION) {
            val manager = (this.recyclerView!!.getLayoutManager() as LinearLayoutManager?)
            if (manager != null) {
                manager.scrollToPositionWithOffset(savedRecyclerPosition, savedRecyclerPositionOffset)
            }
            savedRecyclerPosition = RecyclerView.NO_POSITION
            savedRecyclerPositionOffset = 0
        }
    }

    public override fun supportsBottomInset(): Boolean {
        return true
    }

    protected open fun needRecyclerBottomInset(): Boolean {
        return true
    }

    override fun onBottomInsetChanged(extraBottomInset: Int, extraBottomInsetWithoutIme: Int, isImeInset: Boolean) {
        super.onBottomInsetChanged(extraBottomInset, extraBottomInsetWithoutIme, isImeInset)
        if (needRecyclerBottomInset()) {
            Views.applyBottomInset(recyclerView, extraBottomInset)
        }
    }

    protected open fun createFrameLayout(context: Context): FrameLayout {
        return FrameLayoutFix(context)
    }

    @SuppressLint("InflateParams")
    override fun onCreateView(context: Context): View? {
        val wrap = createFrameLayout(context)
        if (needContentBackground()) {
            ViewSupport.setThemedBackground(wrap, this.recyclerBackground, this)
        }
        wrap.setLayoutParams(newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        // Use the created instance directly, not the recyclerView property: subclasses may
        // override getRecyclerView() to return their own (still-null) field until onCreateView below.
        val recyclerView = onCreateRecyclerView()
        this.recyclerView = recyclerView
        if (needRecyclerBottomInset()) {
            Views.applyBottomInset(recyclerView, extraBottomInset)
        }
        Views.setScrollBarPosition(recyclerView)
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (scrollState != newState) {
                    prevScrollState = scrollState
                    scrollState = newState
                }
            }
        })
        onCreateView(context, recyclerView)
        wrap.addView(recyclerView)
        if (needPersistentScrollPosition()) {
            restorePersistentScrollPosition()
        }
        if (needSearch()) {
            generateChatSearchView(wrap)
        }
        return wrap
    }

    protected fun onCreateRecyclerView(): CustomRecyclerView {
        val recyclerView = Views.inflate(context(), R.layout.recycler_custom, null) as CustomRecyclerView
        recyclerView.setItemAnimator(CustomItemAnimator(DECELERATE_INTERPOLATOR, 180L))
        recyclerView.setLayoutManager(object : LinearLayoutManager(context, RecyclerView.VERTICAL, false) {
            override fun scrollVerticallyBy(dx: Int, recycler: Recycler?, state: RecyclerView.State?): Int {
                var nScroll = 0
                // Do not let auto scroll
                if (!disableSettling || recyclerView.getScrollState() != RecyclerView.SCROLL_STATE_SETTLING || prevScrollState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    nScroll = super.scrollVerticallyBy(dx, recycler, state)
                }
                return nScroll
            }
        })
        recyclerView.setLayoutParams(newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        return recyclerView
    }

    protected fun restorePersistentScrollPosition() {
        if (savedScrollPosition >= 0 && recyclerView != null) {
            val manager = recyclerView!!.getLayoutManager() as LinearLayoutManager?
            val adapter = recyclerView!!.getAdapter()
            if (manager != null && adapter != null && savedScrollPosition >= 0 && savedScrollPosition < adapter.getItemCount()) {
                manager.scrollToPositionWithOffset(savedScrollPosition, savedScrollOffset)
                savedScrollPosition = -1
                savedScrollOffset = 0
            }
        }
    }

    public override fun saveInstanceState(outState: Bundle, keyPrefix: String?): Boolean {
        val manager = if (recyclerView != null) recyclerView!!.getLayoutManager() as LinearLayoutManager? else null
        if (manager != null) {
            val position = manager.findFirstVisibleItemPosition()
            val view = manager.findViewByPosition(position)
            val scrollBy = if (view != null) manager.getDecoratedTop(view) else 0
            outState.putInt(keyPrefix + "base_scroll_position", position)
            outState.putInt(keyPrefix + "base_scroll_offset", scrollBy)
        }
        return super.saveInstanceState(outState, keyPrefix)
    }

    protected open fun needPersistentScrollPosition(): Boolean {
        return false
    }

    private var savedScrollPosition = -1
    private var savedScrollOffset = 0

    public override fun restoreInstanceState(`in`: Bundle, keyPrefix: String?): Boolean {
        savedScrollPosition = `in`.getInt(keyPrefix + "base_scroll_position", -1)
        savedScrollOffset = `in`.getInt(keyPrefix + "base_scroll_offset", 0)
        return super.restoreInstanceState(`in`, keyPrefix)
    }

    override fun handleLanguageDirectionChange() {
        super.handleLanguageDirectionChange()
        Views.setScrollBarPosition(recyclerView)
    }

    public override fun handleLanguagePackEvent(event: Int, arg1: Int) {
        val adapterListener = if (recyclerView != null && recyclerView!!.getAdapter() is Lang.Listener) recyclerView!!.getAdapter() as Lang.Listener? else null
        if (adapterListener != null) {
            adapterListener.onLanguagePackEvent(event, arg1)
        }
    }

    override fun onScrollToTopRequested() {
        if (recyclerView!!.getAdapter() != null) {
            try {
                val manager = this.recyclerView!!.getLayoutManager() as LinearLayoutManager?
                this.recyclerView!!.stopScroll()
                val firstVisiblePosition = manager!!.findFirstVisibleItemPosition()
                if (firstVisiblePosition == RecyclerView.NO_POSITION) {
                    return
                }
                var scrollTop = (recyclerView!!.getAdapter() as SettingsAdapter).measureScrollTop(firstVisiblePosition)
                val view = manager.findViewByPosition(firstVisiblePosition)
                if (view != null) {
                    scrollTop -= view.getTop()
                }
                this.recyclerView!!.smoothScrollBy(0, -scrollTop)
            } catch (t: Throwable) {
                Log.w("Cannot scroll to top", t)
            }
        }
    }

    protected fun needContentBackground(): Boolean {
        return true
    }

    override fun provideRecyclerView(): RecyclerView? {
        return recyclerView
    }

    public override fun getViewForApplyingOffsets(): View? {
        return recyclerView
    }

    protected fun findFirstVisiblePosition(): Int {
        return (recyclerView!!.getLayoutManager() as LinearLayoutManager).findFirstVisibleItemPosition()
    }

    protected fun findLastVisiblePosition(): Int {
        return (recyclerView!!.getLayoutManager() as LinearLayoutManager).findLastVisibleItemPosition()
    }

    protected fun getViewTop(position: Int): Int {
        val view = recyclerView!!.getLayoutManager()!!.findViewByPosition(position)
        return if (view != null) view.getTop() else 0
    }

    protected fun removeItemAnimatorDelayed() {
        if (recyclerView!!.getItemAnimator() != null) {
            recyclerView!!.postDelayed(Runnable {
                if (!isDestroyed()) {
                    recyclerView!!.setItemAnimator(null)
                }
            }, 300)
        }
    }

    override fun destroy() {
        super.destroy()
        Views.destroyRecyclerView(recyclerView)
    }

    // Search
    protected override fun getMenuId(): Int {
        return if (needSearch()) R.id.menu_search else 0
    }

    override fun fillMenuItems(id: Int, header: HeaderView, menu: LinearLayout) {
        if (id == R.id.menu_search) {
            header.addSearchButton(menu, this)
        } else if (id == R.id.menu_help) {
            header.addButton(menu, R.id.menu_btn_help, R.drawable.baseline_help_outline_24, getHeaderIconColorId(), this, Screen.dp(49f))
        } else if (id == R.id.menu_clear) {
            header.addClearButton(menu, this)
        } else if (id == R.id.menu_more) {
            header.addMoreButton(menu, this)
        }
    }

    override fun onMenuItemPressed(id: Int, view: View?) {
        if (id == R.id.menu_btn_search) {
            openSearchMode()
        } else if (id == R.id.menu_btn_clear) {
            clearSearchInput()
        } else if (id == R.id.menu_btn_more) {
            openMoreMenu()
        }
    }

    protected open fun openMoreMenu() {
        // override in children
    }

    private fun needSearch(): Boolean {
        return (flags and FLAG_NEED_SEARCH) != 0
    }

    private var flags = 0

    fun setNeedSearch(): RecyclerViewController<T> {
        flags = flags or FLAG_NEED_SEARCH
        return this
    }

    fun setNeedTutorial(): RecyclerViewController<T> {
        flags = flags or FLAG_NEED_TUTORIAL
        return this
    }

    override fun getSearchAntagonistView(): View? {
        return recyclerView
    }

    protected override fun getSearchMenuId(): Int {
        return if (needSearch()) R.id.menu_clear else super.getSearchMenuId()
    }

    // Done button
    private var doneButton: DoneButton? = null

    protected open fun onDoneClick() {
        // Override in children
    }

    protected fun getDoneButton(): DoneButton? {
        if (doneButton == null) {
            doneButton = DoneButton(context)
            val padding = Screen.dp(4f)
            val params: FrameLayout.LayoutParams?
            params = newParams(Screen.dp(56f) + padding * 2, Screen.dp(56f) + padding * 2, (if (Lang.rtl()) Gravity.LEFT else Gravity.RIGHT) or Gravity.BOTTOM)
            params.bottomMargin = Screen.dp(16f) - padding
            params.leftMargin = params.bottomMargin
            params.rightMargin = params.leftMargin

            doneButton = DoneButton(context)
            doneButton!!.setId(R.id.btn_done)
            addThemeInvalidateListener(doneButton)
            doneButton!!.setOnClickListener(View.OnClickListener { v: View? ->
                if (doneButton!!.getIsVisible()) {
                    onDoneClick()
                }
            })
            doneButton!!.setLayoutParams(params)
            (getValue() as ViewGroup).addView(doneButton)
        }
        return doneButton
    }

    protected fun setDoneVisible(isVisible: Boolean, animated: Boolean) {
        getDoneButton()!!.setIsVisible(isVisible, animated)
    }

    protected fun setDoneIcon(@DrawableRes doneRes: Int) {
        getDoneButton()!!.setIcon(doneRes)
    }

    protected fun setDoneProgress(inProgress: Boolean) {
        getDoneButton()!!.setInProgress(inProgress)
    }

    companion object {
        private const val FLAG_NEED_SEARCH = 1
        private val FLAG_NEED_TUTORIAL = 1 shl 1
    }
}
