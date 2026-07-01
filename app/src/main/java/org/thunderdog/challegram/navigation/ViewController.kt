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
 * File created on 23/04/2015 at 16:15
 */
@file:OptIn(kotlin.contracts.ExperimentalContracts::class)

package org.thunderdog.challegram.navigation

import android.animation.Animator
import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.res.Configuration
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.util.SparseIntArray
import android.util.TypedValue
import android.view.*
import android.view.ViewGroup.MarginLayoutParams
import android.view.animation.Interpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.*
import androidx.collection.SparseArrayCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.vkryl.android.DECELERATE_INTERPOLATOR
import me.vkryl.android.setBackground
import me.vkryl.android.widget.FrameLayoutFix
import me.vkryl.android.widget.FrameLayoutFix.Companion.newParams
import me.vkryl.core.*
import me.vkryl.core.collection.IntList
import me.vkryl.core.lambda.*
import me.vkryl.core.reference.ReferenceList
import org.drinkless.tdlib.TdApi.DeepLinkInfo
import org.thunderdog.challegram.BaseActivity
import org.thunderdog.challegram.BaseActivity.ActivityListener
import org.thunderdog.challegram.BaseActivity.KeyEventListener
import org.thunderdog.challegram.Log
import org.thunderdog.challegram.R
import org.thunderdog.challegram.component.base.SettingView
import org.thunderdog.challegram.config.Config
import org.thunderdog.challegram.core.Background
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.data.TD
import org.thunderdog.challegram.mediaview.AvatarPickerMode
import org.thunderdog.challegram.mediaview.MediaSelectDelegate
import org.thunderdog.challegram.mediaview.MediaSendDelegate
import org.thunderdog.challegram.mediaview.MediaViewDelegate
import org.thunderdog.challegram.navigation.TooltipOverlayView.TooltipInfo
import org.thunderdog.challegram.support.RippleSupport
import org.thunderdog.challegram.support.ViewSupport
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.telegram.TdlibAccount
import org.thunderdog.challegram.telegram.TdlibContext
import org.thunderdog.challegram.telegram.TdlibDelegate
import org.thunderdog.challegram.telegram.TdlibUi.UrlOpenParameters
import org.thunderdog.challegram.theme.*
import org.thunderdog.challegram.tool.*
import org.thunderdog.challegram.tool.Keyboard.OnStateChangeListener
import org.thunderdog.challegram.ui.ListItem
import org.thunderdog.challegram.ui.SettingHolder
import org.thunderdog.challegram.ui.SettingsAdapter
import org.thunderdog.challegram.ui.SettingsBugController
import org.thunderdog.challegram.ui.camera.CameraController.QrCodeListener
import org.thunderdog.challegram.ui.camera.CameraController.ReadyListener
import org.thunderdog.challegram.unsorted.Passcode
import org.thunderdog.challegram.unsorted.Settings
import org.thunderdog.challegram.unsorted.Settings.Companion.instance
import org.thunderdog.challegram.unsorted.Size
import org.thunderdog.challegram.util.*
import org.thunderdog.challegram.util.ColumnDataPicker.Column.StylingOptions
import org.thunderdog.challegram.util.text.Text
import org.thunderdog.challegram.util.text.TextEntity
import org.thunderdog.challegram.v.HeaderEditText
import org.thunderdog.challegram.widget.*
import org.thunderdog.challegram.widget.ForceTouchView.ForceTouchContext
import org.thunderdog.challegram.widget.InfiniteRecyclerView.ItemChangeListener
import org.thunderdog.challegram.widget.InfiniteRecyclerView.MinMaxProvider
import org.thunderdog.challegram.widget.PopupLayout.PopupHeightProvider
import org.thunderdog.challegram.widget.RootFrameLayout.MarginModifier
import org.thunderdog.challegram.widget.decoration.BottomInsetFillingDecoration
import tgx.td.isEmpty
import tgx.td.isUserChat
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max
import kotlin.math.min

// TODO separate Telegram-related stuff to TelegramViewController<T>. This will allow reusing navigation logic in other projects
abstract class ViewController<T>(context: Context, @JvmField val tdlib: Tdlib?) : Future<View?>, ThemeChangeListener, Lang.Listener, ForceTouchView.StateListener,
  ActivityListener, KeyEventListener, TdlibDelegate, Destroyable {
  @JvmField
  val context: BaseActivity = UI.getContext(context)

  private var flags = 0
  private var name: CharSequence? = null

  private var args: T? = null
  var wrapUnchecked: View? = null
    private set

  private var lockFocusView: View? = null

  private var parentWrapper: ViewController<*>? = null

  @JvmField
  protected var headerView: HeaderView? = null

  @JvmField
  protected var floatingButton: FloatingButton? = null

  @JvmField
  protected var navigationController: NavigationController? = null

  fun onInteractedWithContent() {
    setFlags(flags or FLAG_CONTENT_INTERACTED)
  }

  fun hasInteractedWithContent(): Boolean {
    return (flags and FLAG_CONTENT_INTERACTED) != 0
  }

  fun getParentOrSelf(): ViewController<*> {
    return parentWrapper ?: this
  }

  open fun setParentWrapper(parentWrapper: ViewController<*>?) {
    this.parentWrapper = parentWrapper
  }

  // Theme stuff
  private fun subscribeToNeededUpdates() {
    ThemeManager.instance().addThemeListener(this)
    Lang.addLanguageListener(this)
    context.addActivityListener(this)
  }

  private fun unsubscribeFromControllerUpdates() {
    ThemeManager.instance().removeThemeListener(this)
    Lang.removeLanguageListener(this)
    context.removeActivityListener(this)
  }

  open fun allowThemeChanges(): Boolean {
    return true
  }

  private var themeListeners: ThemeListenerList? = null

  fun getThemeListeners(): ThemeListenerList {
    if (themeListeners == null) themeListeners = ThemeListenerList()
    return themeListeners!!
  }

  private fun addThemeListener(listenerEntry: ThemeListenerEntry?): ThemeListenerEntry? {
    if (listenerEntry != null && !listenerEntry.isEmpty) {
      getThemeListeners().add(listenerEntry)
    }
    return listenerEntry
  }

  fun attachToThemeListeners(themeListeners: ThemeListenerList) {
    if (this.themeListeners != null) themeListeners.addAll(this.themeListeners)
    this.themeListeners = themeListeners
  }

  fun bindThemeListeners(c: ViewController<*>?) {
    if (c != null) {
      if (c.themeListeners == null) c.themeListeners = ThemeListenerList()
      if (this.themeListeners != null) c.themeListeners!!.list.addAll(this.themeListeners!!.list)
      this.themeListeners = c.themeListeners
    }
  }

  fun addThemePaintColorListener(paint: Paint?, @ColorId color: Int) {
    addThemeListener(ThemeListenerEntry(ThemeListenerEntry.MODE_PAINT_COLOR, color, paint))
  }

  fun addThemeBackgroundColorListener(view: View?, @ColorId color: Int) {
    addThemeListener(ThemeListenerEntry(ThemeListenerEntry.MODE_BACKGROUND, color, view))
  }

  fun addThemeFillingColorListener(view: View?) {
    addThemeListener(ThemeListenerEntry(ThemeListenerEntry.MODE_BACKGROUND, ColorId.filling, view))
  }

  fun addThemeTextColorListener(view: Any?, @ColorId colorId: Int): ThemeListenerEntry {
    val entry: ThemeListenerEntry
    addThemeListener(ThemeListenerEntry(ThemeListenerEntry.MODE_TEXT_COLOR, colorId, view).also { entry = it })
    return entry
  }

  fun addThemeCompoundDrawableColorListener(view: TextView?, @ColorId colorId: Int) {
    addThemeListener(ThemeListenerEntry(ThemeListenerEntry.MODE_COMPOUND_DRAWABLE_COLOR, colorId, view))
  }

  fun addOrUpdateThemeTextColorListener(view: Any?, @ColorId colorId: Int): ThemeListenerEntry {
    val entry = getThemeListeners().findThemeListenerByTarget(view, ThemeListenerEntry.MODE_TEXT_COLOR)
    if (entry != null) {
      entry.setTargetColorId(colorId)
      return entry
    }
    return addThemeTextColorListener(view, colorId)
  }

  fun addThemeHintTextColorListener(view: Any?, @ColorId color: Int): ThemeListenerEntry? {
    return addThemeListener(ThemeListenerEntry(ThemeListenerEntry.MODE_HINT_TEXT_COLOR, color, view))
  }

  fun addThemeLinkTextColorListener(view: Any?, @ColorId color: Int) {
    addThemeListener(ThemeListenerEntry(ThemeListenerEntry.MODE_LINK_TEXT_COLOR, color, view))
  }

  fun addThemeHighlightColorListener(view: Any?, @ColorId color: Int) {
    addThemeListener(ThemeListenerEntry(ThemeListenerEntry.MODE_HIGHLIGHT_COLOR, color, view))
  }

  fun addThemeTextAccentColorListener(view: Any?) {
    addThemeListener(ThemeListenerEntry(ThemeListenerEntry.MODE_TEXT_COLOR, ColorId.text, view))
  }

  fun addThemeTextDecentColorListener(view: Any?) {
    addThemeListener(ThemeListenerEntry(ThemeListenerEntry.MODE_TEXT_COLOR, ColorId.textLight, view))
  }

  fun addThemeInvalidateListener(view: View?) {
    addThemeListener(ThemeListenerEntry(ThemeListenerEntry.MODE_INVALIDATE, ColorId.NONE, view))
  }

  fun addThemeFilterListener(target: Any?, @ColorId color: Int): ThemeListenerEntry {
    val entry: ThemeListenerEntry
    addThemeListener(ThemeListenerEntry(ThemeListenerEntry.MODE_FILTER, color, target).also { entry = it })
    return entry
  }

  fun addThemeSpecialFilterListener(target: Any?, @ColorId colorId: Int) {
    addThemeListener(ThemeListenerEntry(ThemeListenerEntry.MODE_SPECIAL_FILTER, colorId, target))
  }

  fun removeThemeListenerByTarget(target: Any?) {
    if (themeListeners != null) {
      themeListeners!!.removeThemeListenerByTarget(target)
    }
  }

  override fun needsTempUpdates(): Boolean {
    return this.isAttachedToNavigationController() || this.isInForceTouchMode()
  }

  @CallSuper
  override fun onThemeColorsChanged(areTemp: Boolean, state: ColorState?) {
    if (themeListeners != null) {
      themeListeners!!.onThemeColorsChanged(areTemp)
    }
  }

  override fun onThemeChanged(fromTheme: ThemeDelegate?, toTheme: ThemeDelegate?) {}

  override fun onThemeAutoNightModeChanged(autoNightMode: Int) {}

  @CallSuper
  protected open fun handleLanguageDirectionChange() {
    searchHeaderView?.let { HeaderView.updateEditTextDirection(it.editView(), Screen.dp(68f), Screen.dp(49f)) }
    counterHeaderView?.let { HeaderView.updateLayoutMargins(it, Screen.dp(68f), 0) }
    val headerCell = this.getCustomHeaderCell()
    if (headerCell is RtlCheckListener) {
      (headerCell as RtlCheckListener).checkRtl()
    }
    // override
  }

  protected open fun handleLanguagePackEvent(@Lang.EventType event: Int, arg1: Int) {
    // override
  }

  @CallSuper
  override fun onLanguagePackEvent(@Lang.EventType event: Int, arg1: Int) {
    val directionChanged = Lang.hasDirectionChanged(event, arg1)

    if (directionChanged) {
      handleLanguageDirectionChange()
    }

    when (event) {
      Lang.EVENT_PACK_CHANGED, Lang.EVENT_DIRECTION_CHANGED, Lang.EVENT_DATE_FORMAT_CHANGED -> {
        if (localeChangers != null) {
          for (changer in localeChangers) {
            changer.onLocaleChange()
          }
        }
        if (event == Lang.EVENT_PACK_CHANGED) {
          setName(this.name)
        }
      }

      Lang.EVENT_STRING_CHANGED -> {
        if (localeChangers != null) {
          for (changer in localeChangers) {
            if (changer.getResource() == arg1) changer.onLocaleChange()
          }
        }
        setName(this.name)
      }
    }

    handleLanguagePackEvent(event, arg1)
  }

  // Navigation stuff
  fun navigationController(): NavigationController? {
    return navigationController
  }

  protected open fun attachNavigationController(navigationController: NavigationController) {
    setFlags(flags or FLAG_ATTACHED_TO_NAVIGATION)
    this.navigationController = navigationController
    this.headerView = navigationController.headerView
    this.floatingButton = navigationController.floatingButton
    navigationController.applyBottomInset(this)
  }

  open fun attachHeaderViewWithoutNavigation(headerView: HeaderView?) {
    setFlags(this.flags and (FLAG_ATTACHED_TO_NAVIGATION.inv())) // since it's false state
    this.headerView = headerView
    this.navigationController = null
    this.floatingButton = null
  }

  protected open fun detachNavigationController() {
    setFlags(this.flags and (FLAG_ATTACHED_TO_NAVIGATION.inv()))
    this.navigationController = null
    this.headerView = null
    this.floatingButton = null
    setBottomInset(0, 0)
  }

  protected fun navigationStack(): NavigationStack? {
    return navigationController?.stack
  }

  fun stackSize(): Int {
    return navigationController?.stackSize ?: 0
  }

  protected fun previousStackItem(): ViewController<*>? {
    return navigationController?.stack?.previous
  }

  fun stackItemAt(index: Int): ViewController<*>? {
    return navigationController?.stack?.get(index)
  }

  fun removeStackItemAt(index: Int): ViewController<*>? {
    return navigationController?.stack?.remove(index)
  }

  fun destroyStackItemAt(index: Int): ViewController<*>? {
    return navigationController?.stack?.destroy(index)
  }

  fun destroyPreviousStackItem(): ViewController<*>? {
    return navigationController?.let { navigationController ->
      val stack = navigationController.stack
      val currentIndex = stack.size() - 1
      currentIndex.takeIf { it > 0 }?.let { stack.destroy(it - 1) }
    }
  }

  fun removeStackItemById(id: Int): ViewController<*>? {
    return navigationController?.stack?.removeById(id)
  }

  fun destroyStackItemById(id: Int): ViewController<*>? {
    return navigationController?.stack?.destroyById(id)
  }

  fun destroyAllStackItemsById(id: Int) {
    if (navigationController != null) {
      navigationController!!.stack.destroyAllById(id)
    }
  }

  fun destroyStackItemByIdExcludingLast(id: Int): ViewController<*>? {
    return navigationController?.stack?.destroyByIdExcludingLast(id)
  }

  fun findLastStackItemById(id: Int): ViewController<*>? {
    return navigationController?.stack?.findLastById(id)
  }

  fun isStackLocked(): Boolean {
    return navigationController != null && navigationController!!.stack.isLocked
  }

  protected fun setStackLocked(isLocked: Boolean) {
    if (navigationController != null) {
      navigationController!!.stack.setIsLocked(isLocked)
    }
  }

  protected fun isNavigationAnimating(): Boolean {
    return navigationController != null && navigationController!!.isAnimating
  }

  protected fun postNavigateBack() {
    // TODO proper way
    UI.post { this.navigateBack() }
  }

  // Methods which have implementation in NavigationController
  fun navigateTo(c: ViewController<*>?): Boolean {
    return !this.isStackLocked() && navigationController != null && navigationController!!.navigateTo(c)
  }

  fun isAttachedToNavigationController(): Boolean {
    return (flags and FLAG_ATTACHED_TO_NAVIGATION) != 0
  }

  fun navigateBack(): Boolean {
    return navigationController != null && navigationController!!.navigateBack()
  }

  protected fun setController(controller: ViewController<*>?) {
    if (navigationController != null) {
      navigationController!!.setController(controller)
    }
  }

  protected fun setControllerAnimated(controller: ViewController<*>?, asForward: Boolean, saveFirst: Boolean) {
    if (navigationController != null) {
      navigationController!!.setControllerAnimated(controller, asForward, saveFirst)
    }
  }

  @JvmOverloads
  fun runOnUiThreadOptional(runnable: Runnable?, condition: FutureBool? = null, delay: Long = 0) {
    if (runnable == null) return
    val act = Runnable {
      if (!this.isDestroyed() && (condition == null || condition.getBoolValue())) {
        runnable.run()
      }
    }
    if (delay > 0) {
      runOnUiThread(act, delay)
    } else {
      runOnUiThread(act)
    }
  }

  protected fun runOnUiThread(runnable: Runnable) {
    UI.post(runnable)
  }

  protected fun removeCallbacks(runnable: Runnable) {
    UI.removePendingRunnable(runnable)
  }

  protected fun executeOnUiThread(runnable: Runnable) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
      runnable.run()
    } else {
      runOnUiThread(runnable)
    }
  }

  protected fun executeOnUiThreadOptional(runnable: Runnable) {
    if (UI.inUiThread()) {
      if (!this.isDestroyed()) {
        runnable.run()
      }
    } else {
      runOnUiThreadOptional(runnable)
    }
  }

  protected fun runOnUiThread(runnable: Runnable, delay: Long) {
    UI.post(runnable, delay)
  }

  protected fun runOnBackgroundThread(runnable: Runnable) {
    Background.instance().post(runnable)
  }

  /*protected void setController (Class<? extends ViewController> rawController) {
    if ((flags & FLAG_ATTACHED_TO_NAVIGATION) != 0) {
      navigationController.setController(rawController);
    }
  }

  protected void setControllerAnimated (Class<? extends ViewController> rawController, boolean asForward, boolean saveFirst) {
    if ((flags & FLAG_ATTACHED_TO_NAVIGATION) != 0) {
      navigationController.setControllerAnimated(rawController, asForward, saveFirst);
    }
  }

  protected void setControllerAnimated (Class<? extends ViewController> rawController, Object args, boolean asForward, boolean saveFirst) {
    final NavigationController navigation = this.navigationController;
    if (navigation != null) { // Because we use this method only from background thread
      navigation.setControllerAnimated(rawController, args, asForward, saveFirst);
    }
  }*/
  fun openSelectMode(initialCount: Int) {
    if (headerView != null) {
      headerView!!.openSelectMode(initialCount, true)
    }
  }

  fun closeSelectMode() {
    if (headerView != null) {
      headerView!!.closeSelectMode()
    }
  }

  // NB: public (not protected) — same-package siblings call this cross-instance
  // (e.g. ViewPagerController on its current pager item); Kotlin protected is subclass-only.
  open fun onEnterSelectMode() {
  }

  open fun onLeaveSelectMode() {
  }

  private var cachedLockFocusView: View? = null

  @CallSuper
  protected open fun onEnterSearchMode() {
    val text = this.searchStartQuery
    clearSearchInput(text.orEmpty(), true)
  }

  protected open fun onLeaveSearchMode() {
  }

  @CallSuper
  fun updateSearchMode(inSearch: Boolean, needUpdateKeyboard: Boolean) {
    if (inSearch) {
      cachedLockFocusView = lockFocusView
      lockFocusView = searchHeaderView!!.editView()
      if (needUpdateKeyboard) {
        Keyboard.show(searchHeaderView!!.editView())
      }
    } else {
      lockFocusView = cachedLockFocusView
      if (needUpdateKeyboard) {
        Keyboard.hide(searchHeaderView!!.editView())
      }
      cachedLockFocusView = null
    }
  }

  @CallSuper
  @Deprecated("")
  protected open fun updateSearchMode(inSearch: Boolean) {
    updateSearchMode(inSearch, true)
  }

  protected val customFocusView: View?
    get() = null

  fun updateCustomMode(inCustom: Boolean) {
    if (inCustom) {
      cachedLockFocusView = lockFocusView
      lockFocusView = this.customFocusView
      if (lockFocusView == null) {
        Keyboard.hide(cachedLockFocusView)
      } else {
        Keyboard.show(lockFocusView)
      }
    } else {
      val oldFocusView = lockFocusView
      lockFocusView = cachedLockFocusView
      cachedLockFocusView = null
      if (oldFocusView != null) {
        Keyboard.hide(oldFocusView)
      }
    }
  }

  protected fun openCustomMode() {
    if (headerView != null) {
      headerView!!.openCustomMode()
    }
  }

  protected fun closeCustomMode() {
    if (headerView != null) {
      headerView!!.closeCustomMode()
    }
  }

  protected fun openSearchMode() {
    if (headerView != null) {
      headerView!!.openSearchMode()
    }
  }

  protected open fun launchCustomHeaderTransformAnimator(open: Boolean, transformMode: Int, listener: Animator.AnimatorListener?): Boolean {
    return false
  }

  protected open fun startHeaderTransformAnimator(animator: ValueAnimator, mode: Int, open: Boolean) {
    animator.start()
  }

  protected open val searchStartQuery: String?
    get() = null

  private var searchTransformFactor: Float = 0f

  protected fun getSearchTransformFactor(): Float {
    return searchTransformFactor
  }

  fun setSearchTransformFactor(factor: Float, isOpening: Boolean) {
    if (this.searchTransformFactor != factor) {
      this.searchTransformFactor = factor
      applySearchTransformFactor(factor, isOpening)
    }
  }

  protected open fun getSearchTransformInterpolator(): Interpolator? {
    return DECELERATE_INTERPOLATOR
  }

  protected open fun getSearchTransformDuration(): Long {
    return 200L
  }

  @CallSuper
  protected open fun applySearchTransformFactor(factor: Float, isOpening: Boolean) {
    // override
  }

  @get:ColorId
  open val rootColorId: Int
    get() = ColorId.filling

  protected fun closeSearchMode(after: Runnable?) {
    if (headerView != null) {
      headerView!!.closeSearchMode(true, after)
    }
  }

  protected fun getCustomModeHeaderView(headerView: HeaderView?): View? {
    return null
  }

  fun getTransformHeaderView(headerView: HeaderView): View? {
    if ((flags and FLAG_IN_SELECT_MODE) != 0) {
      return getCounterHeaderView(headerView)
    }
    if ((flags and FLAG_IN_SEARCH_MODE) != 0) {
      return getSearchHeaderView(headerView)!!.view()
    }
    if ((flags and FLAG_IN_CUSTOM_MODE) != 0) {
      return getCustomModeHeaderView(headerView)
    }
    return null
  }

  fun disableHeaderTransformation(): Boolean {
    return false
  }

  // Async controller opening
  var scheduledAnimation: Runnable? = null
    private set
  private var animationReadyListeners: ArrayList<Runnable>? = null

  open fun needAsynchronousAnimation(): Boolean {
    return false
  }

  open fun getAsynchronousAnimationTimeout(fastAnimation: Boolean): Long {
    return if (fastAnimation) 2000L else 500L
  }

  fun scheduleAnimation(scheduledAnimation: Runnable, timeout: Long) {
    this.scheduledAnimation = scheduledAnimation
    if (timeout >= 0) {
      tdlib!!.ui().postDelayed(scheduledAnimation, timeout)
    }
  }

  fun resetScheduledAnimation() {
    this.scheduledAnimation = null
  }

  fun postOnAnimationReady(runnable: Runnable?) {
    if (animationReadyListeners == null) {
      animationReadyListeners = ArrayList<Runnable>()
    }
    animationReadyListeners!!.add(runnable!!)
  }

  fun executeAnimationReadyListeners() {
    if (animationReadyListeners != null) {
      for (runnable in animationReadyListeners) {
        runnable.run()
      }
      animationReadyListeners!!.clear()
    }
  }

  private var animationExecuteListeners: ArrayList<Runnable>? = null

  fun postOnAnimationExecute(runnable: Runnable?) {
    if (animationExecuteListeners == null) {
      animationExecuteListeners = ArrayList<Runnable>()
    }
    animationExecuteListeners!!.add(runnable!!)
  }

  fun executeScheduledAnimation() {
    if (scheduledAnimation != null) {
      scheduledAnimation!!.run()
      scheduledAnimation = null
    }
    if (animationExecuteListeners != null) {
      for (runnable in animationExecuteListeners) {
        runnable.run()
      }
      animationExecuteListeners!!.clear()
    }
  }

  // Other
  fun setLockFocusView(view: View?) {
    setLockFocusView(view, true)
  }

  fun getLockFocusView(): View? {
    return lockFocusView
  }

  fun needAlwaysShowKeyboardOnFocusView(): Boolean {
    return hasFlag(flags, FLAG_LOCK_ALWAYS)
  }

  open fun setLockFocusView(view: View?, showAlways: Boolean) {
    if ((flags and FLAG_IN_SEARCH_MODE) != 0 || (flags and FLAG_IN_CUSTOM_MODE) != 0) {
      this.cachedLockFocusView = view
    } else {
      this.lockFocusView = view
    }
    setFlags(setFlag(flags, FLAG_LOCK_ALWAYS, showAlways))
  }

  @CallSuper
  open fun setArguments(args: T) {
    this.args = args
  }

  fun getArguments(): T? = args

  fun getArgumentsStrict(): T {
    return args
      ?: throw NullPointerException(toString() + " (" + javaClass.getSimpleName() + ") arguments are null")
  }

  protected open fun getBackButton(): Int {
    return BackHeaderButton.TYPE_NONE
  }

  /*protected final int getBackButtonColor () {
         return Theme.headerBackColor();
       }

       protected final @ColorId int getBackButtonColorId () {
         return getHeaderIconColorId(); // ColorId.headerIcon;
       }*/
  @DrawableRes
  protected open fun getBackButtonResource(): Int {
    return ThemeDeprecated.headerSelector()
  }

  @IdRes
  protected open fun getMenuId(): Int {
    return 0
  }

  protected open fun allowMenuReuse(): Boolean {
    return true
  }

  // NB: public (not protected) — same-package siblings call this cross-instance; see onEnterSelectMode.
  @IdRes
  open fun getSelectMenuId(): Int {
    return 0
  }

  @IdRes
  protected open fun getSearchMenuId(): Int {
    return 0
  }

  protected fun useLightSearchHeader(): Boolean {
    return false
  }

  val searchBackButton: Int
    get() = BackHeaderButton.TYPE_BACK

  protected open val searchHeaderColorId: Int
    get() = if (useGraySearchHeader()) ColorId.filling else this.getHeaderColorId()

  @get:ColorId
  protected open val searchHeaderIconColorId: Int
    get() = if (useGraySearchHeader()) ColorId.icon else this.getHeaderIconColorId()

  protected val searchTextColorId: Int
    get() = if (useGraySearchHeader()) ColorId.text else this.getHeaderTextColorId()

  val searchBackButtonResource: Int
    get() = this.getBackButtonResource()

  @get:StringRes
  protected open val searchHint: Int
    get() = R.string.Search

  protected val selectHeaderColorId: Int
    get() = ColorId.headerLightBackground

  @get:ColorId
  val selectTextColorId: Int
    get() = ColorId.headerLightText

  fun updateCustomButtonColorFactor(view: View?, menuId: Int, colorFactor: Float) {
    // override
  }

  protected open fun updateCustomMenu(menuId: Int, menu: LinearLayout?) {
    // override
  }

  val selectHeaderIconColor: Int
    // DEPRECATED
    get() = Theme.getColor(this.getSelectHeaderIconColorId())

  protected open fun getHeaderIconColor(): Int {
    return Theme.getColor(this.getHeaderIconColorId())
  }

  val searchHeaderIconColor: Int
    get() = Theme.getColor(this.searchHeaderIconColorId)

  val searchTextColor: Int
    get() = Theme.getColor(this.searchTextColorId)

  val selectHeaderColor: Int
    get() = Theme.getColor(this.selectHeaderColorId)

  val searchHeaderColor: Int
    get() = Theme.getColor(this.searchHeaderColorId)

  val headerColor: Int
    get() = Theme.getColor(this.getHeaderColorId())

  val headerTextColor: Int
    get() = Theme.getColor(this.getHeaderTextColorId())

  @ColorId
  protected open fun getHeaderIconColorId(): Int {
    // FUTURE
    return ColorId.headerIcon
  }

  @ColorId
  protected fun getSelectHeaderIconColorId(): Int {
    return ColorId.headerLightIcon
  }

  val selectBackButtonResource: Int
    /*protected int getSelectStatusBarColor () {
             return HeaderView.computeStatusBarColor(Theme.getColor(getSelectHeaderColorId()));
           }*/
    get() = this.getBackButtonResource()

  protected open fun useHeaderTranslation(): Boolean {
    return true
  }

  protected open fun forceFadeMode(): Boolean {
    return false
  }

  protected open fun forceFastAnimation(): Boolean {
    if (forceFadeModeOnce) {
      forceFadeModeOnce = false
      return true
    }
    return false
  }

  open fun getCustomHeaderCell(): View? {
    return null
  }

  // Select mode header
  private var counterHeaderView: CounterHeaderView? = null

  protected fun getCounterHeaderView(view: HeaderView?): CounterHeaderView? {
    if (counterHeaderView == null) {
      counterHeaderView = HeaderView.genCounterHeader(context(), this.selectTextColorId)
      addThemeInvalidateListener(counterHeaderView)
    }
    return counterHeaderView
  }

  fun initSelectedCount(count: Int) {
    if (counterHeaderView != null) {
      counterHeaderView!!.initCounter(count, false)
    }
  }

  protected fun onSelectedCountChanged(count: Int) {
    // children stuff
  }

  protected fun getSelectedCount(): Int {
    return counterHeaderView?.counter ?: 0
  }

  fun setSelectedCount(count: Int) {
    if (counterHeaderView != null && counterHeaderView!!.setCounter(count)) {
      onSelectedCountChanged(count)
    }
  }

  // Search mode header
  private var searchHeaderView: SearchEditTextDelegate? = null

  protected open fun modifySearchHeaderView(headerEditText: HeaderEditText) {
    // called only once
  }

  protected open fun useGraySearchHeader(): Boolean {
    return false
  }

  protected open fun genSearchHeader(headerView: HeaderView): SearchEditTextDelegate? {
    val view = if (useGraySearchHeader()) headerView.genGreySearchHeader(this) else headerView.genSearchHeader(useLightSearchHeader(), this)
    return object : SearchEditTextDelegate {
      override fun view(): View {
        return view
      }

      override fun editView(): HeaderEditText {
        return view
      }
    }
  }

  protected fun getSearchHeaderView(headerView: HeaderView): SearchEditTextDelegate? {
    if (searchHeaderView == null) {

      val params = newParams(ViewGroup.LayoutParams.MATCH_PARENT, Size.getHeaderPortraitSize())

      if (Lang.rtl()) {
        params.rightMargin = Screen.dp(68f)
        params.leftMargin = Screen.dp(49f)
      } else {
        params.leftMargin = Screen.dp(68f)
        params.rightMargin = Screen.dp(49f)
      }

      searchHeaderView = genSearchHeader(headerView)
      searchHeaderView!!.editView().addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
          if (inSearchMode()) {
            val input = s.toString()
            updateClearSearchButton(input.isNotEmpty(), true)
            if (lastSearchInput != input) {
              lastSearchInput = input
              onSearchInputChanged(input)
            }
          }
        }

        override fun afterTextChanged(s: Editable?) {
        }
      })
      searchHeaderView!!.editView().setHint(Lang.getString(bindLocaleChanger(this.searchHint, searchHeaderView!!.editView(), true, false)))
      searchHeaderView!!.view().setLayoutParams(params)

      modifySearchHeaderView(searchHeaderView!!.editView())
    }
    return searchHeaderView
  }

  protected fun setSearchInput(text: String) {
    clearSearchInput(text, false)
  }

  var lastSearchInput: String = ""
    private set

  fun clearSearchInput() {
    clearSearchInput("", false)
  }

  private fun clearSearchInput(text: String, reset: Boolean) {
    if (searchHeaderView != null) {
      if (reset) {
        lastSearchInput = text
      }
      searchHeaderView!!.editView().setText(text)
      if (!text.isEmpty()) {
        searchHeaderView!!.editView().setSelection(text.length)
      }
      updateClearSearchButton(!text.isEmpty(), false)
    }
  }

  protected fun updateClearSearchButton(visible: Boolean, animated: Boolean) {
    if (this.getSearchMenuId() == R.id.menu_clear && headerView != null) {
      headerView!!.updateMenuClear(R.id.menu_clear, R.id.menu_btn_clear, visible, animated)
    }
  }

  protected fun setClearButtonSearchInProgress(inProgress: Boolean) {
    if (this.getSearchMenuId() == R.id.menu_clear && headerView != null) {
      headerView!!.updateMenuInProgress(R.id.menu_clear, R.id.menu_btn_clear, inProgress)
    }
  }

  protected open fun onSearchInputChanged(query: String) {
    // override in children
  }

  private var lastPlayerFactor = 0f

  @JvmField
  protected var extraBottomInset: Int = 0

  @JvmField
  protected var extraBottomInsetWithoutIme: Int = 0

  fun setBottomInset(extraBottomInset: Int, extraBottomInsetWithoutIme: Int) {
    if (this.extraBottomInset != extraBottomInset || this.extraBottomInsetWithoutIme != extraBottomInsetWithoutIme) {
      this.extraBottomInset = extraBottomInset
      this.extraBottomInsetWithoutIme = extraBottomInsetWithoutIme
      onBottomInsetChanged(extraBottomInset, extraBottomInsetWithoutIme, extraBottomInset == extraBottomInsetWithoutIme)
    }
  }

  open fun supportsBottomInset(): Boolean {
    return false
  }

  protected open fun onBottomInsetChanged(extraBottomInset: Int, extraBottomInsetWithoutIme: Int, isImeInset: Boolean) {
    // override in children
  }

  @JvmField
  protected val systemInsets: Rect = Rect()
  protected val systemInsetsWithoutIme: Rect = Rect()

  @CallSuper
  open fun dispatchSystemInsets(
    parentView: View?,
    originalParams: MarginLayoutParams?,
    legacyInsets: Rect?,
    insets: Rect,
    insetsWithoutIme: Rect,
    systemInsets: Rect?,
    systemInsetsWithoutIme: Rect?,
    fitsSystemWindows: Boolean
  ) {
    this.systemInsets.set(insets)
    this.systemInsetsWithoutIme.set(insetsWithoutIme)
    // override in children
  }

  open fun getViewForApplyingOffsets(): View? {
    return null
  }

  protected open fun shouldApplyPlayerMargin(): Boolean {
    return true
  }

  protected open fun applyPlayerOffset(factor: Float, top: Float): Boolean {
    if (lastPlayerFactor == factor) {
      return false
    }

    val view = this.getViewForApplyingOffsets()

    if (view == null) {
      lastPlayerFactor = factor
      return false
    }

    view.translationY = top

    if (shouldApplyPlayerMargin()) {
      if (factor == 1f) {
        val params = view.layoutParams as MarginLayoutParams
        if (params.bottomMargin != top.toInt()) {
          params.bottomMargin = top.toInt()
          view.setLayoutParams(params)
        }
      } else if (lastPlayerFactor == 1f) {
        val params = view.layoutParams as MarginLayoutParams
        if (params.bottomMargin != 0) {
          params.bottomMargin = 0
          view.setLayoutParams(params)
        }
      }
    }

    lastPlayerFactor = factor

    return true
  }

  fun drawTransform(c: Canvas?, width: Int, height: Int) {
    if (!this.isTransformed && transformFactor > 0f) {
      drawTransform(c, transformFactor, width, height)
    }
  }

  protected open fun drawTransform(c: Canvas?, transformFactor: Float, width: Int, height: Int) {
    // override
  }

  protected open fun applyTransformChanges() {}
  protected open fun clearTransformChanges() {}
  protected open fun applyStaticTransform(factor: Float) {}
  protected open fun applyHeaderMenuTransform(menu: LinearLayout?, factor: Float) {}

  private var transformFactor = 0f
  protected var isTransformed: Boolean = false
    private set

  val isTransforming: Boolean
    get() = transformFactor != 0f && !this.isTransformed

  protected fun getTransformFactor(): Float {
    return transformFactor
  }

  protected fun setTransformFactor(factor: Float) {
    if (this.transformFactor != factor) {
      this.transformFactor = factor
      applyStaticTransform(factor)
      setTransformFullyApplied(factor == 1f)
      if (headerView != null) {
        headerView!!.invalidate()
      }
    }
  }

  private fun setTransformFullyApplied(isApplied: Boolean) {
    if (this.isTransformed != isApplied) {
      this.isTransformed = isApplied
      if (isApplied) {
        applyTransformChanges()
      } else {
        clearTransformChanges()
      }
    }
  }

  fun updateSetting(setting: Int, value: Int) {
    // Must be implemented in ViewControllers
  }

  fun setShareCustomHeaderView(share: Boolean) {
    setFlags(setFlag(flags, FLAG_SHARE_CUSTOM_HEADER, share))
  }

  fun shareCustomHeaderView(): Boolean {
    return (flags and FLAG_SHARE_CUSTOM_HEADER) != 0 && !inTransformMode() && (transformFactor == 0f || allowTransformedHeaderSharing())
  }

  protected fun allowTransformedHeaderSharing(): Boolean {
    return false
  }

  protected open fun getHeaderHeight(): Int {
    return Size.getHeaderPortraitSize()
  }

  protected open fun getMaximumHeaderHeight(): Int {
    return this.getHeaderHeight()
  }

  val customHeaderHeight: Int
    get() = Size.getHeaderPortraitSize()

  protected open fun getTransformHeaderHeight(): Int {
    return Size.getHeaderPortraitSize()
  }

  val customFloatingButtonId: Int
    get() = this.getFloatingButtonId()

  @ColorId
  protected open fun getHeaderColorId(): Int {
    return ColorId.headerBackground
  }

  @get:Deprecated("")
  val statusBarColor: Int
    get() = HeaderView.defaultStatusColor()

  protected open fun getHeaderTextColorId(): Int {
    return ColorId.headerText
  }

  val newStatusBarColor: Int
    get() = HeaderView.DEFAULT_STATUS_COLOR

  protected open fun getFloatingButtonId(): Int {
    return 0
  }

  protected open fun onFloatingButtonPressed() {
  }

  protected open fun useDropShadow(): Boolean {
    return true
  }

  protected open fun useDropPlayer(): Boolean {
    return true
  }

  protected open fun usePopupMode(): Boolean {
    return false
  }

  fun useBigHeaderButtons(): Boolean {
    return usePopupMode()
  }

  protected open fun getPopupRestoreColor(): Int {
    return 0
  }

  protected open fun getStartDelay(forward: Boolean): Long {
    return 0L
  }

  protected open fun applyCustomHeaderAnimations(factor: Float) {
  }

  fun setSwipeNavigationEnabled(enabled: Boolean) {
    setFlags(setFlag(flags, FLAG_SWIPE_DISABLED, !enabled))
  }

  protected open fun swipeNavigationEnabled(): Boolean {
    return (flags and FLAG_SWIPE_DISABLED) == 0
  }

  fun inTransformMode(): Boolean {
    return (flags and FLAG_IN_SEARCH_MODE) != 0 || (flags and FLAG_IN_SELECT_MODE) != 0 || (flags and FLAG_IN_CUSTOM_MODE) != 0
  }

  fun inCustomMode(): Boolean {
    return (flags and FLAG_IN_CUSTOM_MODE) != 0
  }

  fun enterCustomMode() {
    setFlags(flags or FLAG_IN_CUSTOM_MODE)
  }

  fun leaveCustomMode() {
    setFlags(flags and (FLAG_IN_CUSTOM_MODE.inv()))
  }

  fun inSearchMode(): Boolean {
    return (flags and FLAG_IN_SEARCH_MODE) != 0
  }

  open fun onBeforeLeaveSearchMode(): Boolean {
    return true
  }

  protected open fun needHideKeyboardOnTouchBackButton(): Boolean {
    return true
  }

  fun enterSearchMode() {
    setFlags(flags or FLAG_IN_SEARCH_MODE)
  }

  protected open fun onAfterLeaveSearchMode() {}

  fun leaveSearchMode() {
    setFlags(flags and (FLAG_IN_SEARCH_MODE.inv()))
    onAfterLeaveSearchMode()
    setSearchTransformFactor(0f, false)
  }

  fun preventLeavingSearchMode() {
    setFlags(flags or FLAG_PREVENT_LEAVING_SEARCH_MODE)
  }

  fun inSelectMode(): Boolean {
    return hasFlag(flags, FLAG_IN_SELECT_MODE)
  }

  private fun setFlags(flags: Int): Boolean {
    if (this.flags != flags) {
      val oldFlags = this.flags
      this.flags = flags
      val changedFlags = oldFlags xor flags
      if (hasFlag(
          changedFlags,
          FLAG_IN_SELECT_MODE or
            FLAG_IN_SEARCH_MODE or
            FLAG_IN_CUSTOM_MODE
        )
      ) {
        context.notifyBackPressAvailabilityChanged()
      }
      return true
    }
    return false
  }

  fun enterSelectMode() {
    setFlags(flags or FLAG_IN_SELECT_MODE)
  }

  fun leaveSelectMode() {
    setFlags(flags and (FLAG_IN_SELECT_MODE.inv()))
  }

  fun leaveTransformMode() {
    var flags = this.flags
    flags = flags and FLAG_IN_SELECT_MODE.inv()
    flags = flags and FLAG_IN_SEARCH_MODE.inv()
    flags = flags and FLAG_IN_CUSTOM_MODE.inv()
    setFlags(flags)
  }

  protected open fun useDrawer(): Boolean {
    return false
  }

  fun showMore(ids: IntArray, titles: Array<String?>?, buttonIndex: Int) {
    showMore(ids, titles, null, buttonIndex, false)
  }

  fun showMore(ids: IntArray, titles: Array<String?>?, buttonIndex: Int, isLayered: Boolean) {
    showMore(ids, titles, null, buttonIndex, isLayered)
  }

  // Header utils
  @JvmOverloads
  fun showMore(ids: IntArray, titles: Array<String?>?, icons: IntArray? = null, buttonIndex: Int = 0, isLayered: Boolean = false) {
    if (this.isStackLocked() || context.isNavigationBusy) {
      return
    }
    if (headerView != null) {
      headerView!!.showMore(ids, titles, icons, buttonIndex, isLayered, this)
    }
  }

  // Permissions alerts
  @UiThread
  fun processDeepLinkInfo(info: DeepLinkInfo) {
    if (info.needUpdateApplication) {
      openUpdateAlert(if (info.text.isEmpty()) null else TD.toDisplayCharSequence(info.text))
    } else {
      openAlert(R.string.AppName, TD.toDisplayCharSequence(info.text))
    }
  }

  @UiThread
  fun openUpdateAlert(text: CharSequence?) {
    openAlert(
      R.string.AppUpdateRequiredTitle,
      text,
      Lang.getString(R.string.AppUpdateOk),
      { dialog: DialogInterface?, which: Int -> Intents.openSelfGooglePlay() },
      0
    )
  }

  fun showAlert(b: AlertDialog.Builder?): AlertDialog? {
    return modifyAlert(context.showAlert(b), 0)
  }

  fun openFeatureUnavailable(stringRes: Int) {
    val atomicDialog = AtomicReference<AlertDialog?>()
    atomicDialog.set(
      openAlert(
        R.string.FeatureUnavailableSorry,
        Strings.buildMarkdown(
          this,
          Lang.getString(stringRes)
        ) { view: View?, span: CustomTypefaceSpan?, clickedText: String? ->
          val finalDialog = atomicDialog.get()
          if (finalDialog != null) {
            try {
              finalDialog.dismiss()
            } catch (ignored: Throwable) {
            }
          }
          false
        }
      )
    )
  }

  fun openAlert(@StringRes title: Int, message: CharSequence?): AlertDialog? {
    val b = AlertDialog.Builder(context, Theme.dialogTheme())
    b.setTitle(Lang.getString(title))
    b.setMessage(message)
    b.setPositiveButton(Lang.getOK()) { dialog: DialogInterface?, which: Int -> dialog!!.dismiss() }
    return showAlert(b)
  }

  @JvmOverloads
  fun openAlert(@StringRes title: Int, message: CharSequence?, okListener: DialogInterface.OnClickListener?, needCancel: Boolean = true) {
    val b = AlertDialog.Builder(context, Theme.dialogTheme())
    b.setTitle(Lang.getString(title))
    b.setMessage(message)
    b.setPositiveButton(Lang.getOK(), okListener)
    if (needCancel) {
      b.setNegativeButton(Lang.getString(R.string.Cancel)) { dialog: DialogInterface?, which: Int -> dialog!!.dismiss() }
    } else {
      b.setCancelable(false)
    }
    showAlert(b)
  }

  fun openAlert(@StringRes title: Int, @StringRes message: Int) {
    val b = AlertDialog.Builder(context, Theme.dialogTheme())
    b.setTitle(Lang.getString(title))
    b.setMessage(Lang.getString(message))
    b.setPositiveButton(Lang.getOK()) { dialog: DialogInterface?, which: Int -> dialog!!.dismiss() }
    showAlert(b)
  }

  fun openAlert(@StringRes title: Int, @StringRes message: Int, okListener: DialogInterface.OnClickListener?) {
    openAlert(title, message, Lang.getOK(), okListener)
  }

  fun openAlert(@StringRes title: Int, @StringRes message: Int, positiveButton: CharSequence?, okListener: DialogInterface.OnClickListener?) {
    openAlert(title, Lang.getString(message), positiveButton, okListener, 0)
  }

  fun openAlert(
    @StringRes title: Int,
    message: CharSequence?,
    positiveButton: CharSequence?,
    okListener: DialogInterface.OnClickListener?,
    flags: Int
  ): AlertDialog? {
    return openAlert(
      title,
      message,
      positiveButton,
      okListener,
      { dialog: DialogInterface?, which: Int -> dialog!!.dismiss() },
      flags
    )
  }

  interface InputAlertCallback {
    fun onAcceptInput(inputView: MaterialEditTextGroup?, result: String?): Boolean
  }

  fun openInputAlert(
    title: CharSequence?,
    placeholder: CharSequence?,
    @StringRes doneRes: Int,
    @StringRes cancelRes: Int,
    value: CharSequence?,
    callback: InputAlertCallback,
    hideKeyboard: Boolean
  ): MaterialEditTextGroup {
    return openInputAlert(title, placeholder, doneRes, cancelRes, value, null, callback, hideKeyboard, null, null)
  }

  fun openInputAlert(
    title: CharSequence?,
    placeholder: CharSequence?,
    @StringRes doneRes: Int,
    @StringRes cancelRes: Int,
    value: CharSequence?,
    defaultValue: String?,
    callback: InputAlertCallback,
    hideKeyboard: Boolean,
    layoutOverride: RunnableData<ViewGroup?>?,
    forcedTheme: ThemeDelegate?
  ): MaterialEditTextGroup {
    val inputView = MaterialEditTextGroup(context, tdlib)
    inputView.setHint(placeholder)
    inputView.editText.setInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS)
    if (!isEmpty(value)) {
      inputView.text = value
      inputView.editText.setSelection(0, value!!.length)
    }
    inputView.editText.addTextChangedListener(object : TextWatcher {
      override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
      }

      override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        inputView.setInErrorState(false)
      }

      override fun afterTextChanged(s: Editable?) {
      }
    })

    val ll: LinearLayout = object : LinearLayout(context) {
      private var first = false
      override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        if (first && measuredWidth > 0 && measuredHeight > 0) {
          first = false
          Keyboard.show(inputView.editText)
        }
      }
    }
    ll.orientation = LinearLayout.VERTICAL
    ll.gravity = Gravity.CENTER_HORIZONTAL
    val pad = Screen.dp(16f)
    ll.setPadding(pad, pad, pad, pad)

    val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL.toFloat())
    ll.addView(inputView, params)
    layoutOverride?.runWithData(ll)

    val alert = AlertDialog.Builder(context, Theme.dialogTheme())
      .setTitle(title)
      .setView(ll)
      .setPositiveButton(Lang.getString(doneRes)) { dialog: DialogInterface?, which: Int ->
        val result = inputView.text.toString()
        if (callback.onAcceptInput(inputView, result)) {
          if (hideKeyboard) {
            Keyboard.hide(inputView.editText)
          }
        } else {
          inputView.setInErrorState(true)
        }
      }
      .setNegativeButton(Lang.getString(cancelRes)) { dialog: DialogInterface?, which: Int ->
        if (hideKeyboard) {
          Keyboard.hide(inputView.editText)
        }
        dialog!!.dismiss()
      }
    val needReset = !isEmpty(value) && !isEmpty(defaultValue) && (value != defaultValue)
    if (needReset) {
      alert.setNeutralButton(Lang.getString(R.string.ValueReset)) { dialog: DialogInterface?, which: Int ->
        if (callback.onAcceptInput(inputView, defaultValue)) {
          if (hideKeyboard) {
            Keyboard.hide(inputView.editText)
          }
        } else {
          inputView.setInErrorState(true)
        }
      }
    }
    alert.setCancelable(false)
    val dialog = showAlert(alert)
    if (dialog?.window != null) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED)
      } else {
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
      }
    }
    var button = dialog?.getButton(DialogInterface.BUTTON_POSITIVE)
    button?.setOnClickListener { v: View? ->
      val result = inputView.text.toString()
      if (callback.onAcceptInput(inputView, result)) {
        if (hideKeyboard) {
          Keyboard.hide(inputView.editText)
        }
        dialog?.dismiss()
      } else {
        inputView.setInErrorState(true)
      }
    }
    if (needReset) {
      button = dialog?.getButton(DialogInterface.BUTTON_NEUTRAL)
      button?.setOnClickListener { v: View? ->
        if (callback.onAcceptInput(inputView, defaultValue)) {
          if (hideKeyboard) {
            Keyboard.hide(inputView.editText)
          }
          dialog?.dismiss()
        } else {
          inputView.setInErrorState(true)
        }
      }
    }
    return inputView
  }

  protected fun modifyAlert(dialog: AlertDialog?, flags: Int): AlertDialog? {
    if (dialog == null) return null
    if ((flags and ALERT_HAS_LINKS) != 0) {
      val message = dialog.findViewById<View?>(android.R.id.message)
      if (message is TextView) {
        message.movementMethod = LinkMovementMethod.getInstance()
      }
    }
    return dialog
  }

  fun openAlert(
    @StringRes title: Int,
    message: CharSequence?,
    positiveButton: CharSequence?,
    okListener: DialogInterface.OnClickListener?,
    cancelListener: DialogInterface.OnClickListener?,
    flags: Int
  ): AlertDialog? {
    val b = AlertDialog.Builder(context, Theme.dialogTheme())
    b.setTitle(Lang.getString(title))
    b.setMessage(message)
    b.setPositiveButton(positiveButton, okListener)
    if ((flags and ALERT_NO_CANCEL) == 0) {
      b.setNegativeButton(Lang.getString(R.string.Cancel), cancelListener)
    }
    if ((flags and ALERT_NO_CANCELABLE) != 0) {
      b.setCancelable(false)
    }
    return modifyAlert(showAlert(b), flags)
  }

  fun openMissingPermissionAlert(@StringRes message: Int) {
    val b = AlertDialog.Builder(context, Theme.dialogTheme())
    b.setTitle(Lang.getString(R.string.AppName))
    b.setMessage(Lang.getString(message))
    b.setPositiveButton(Lang.getOK()) { dialog: DialogInterface?, which: Int -> dialog!!.dismiss() }
    b.setNegativeButton(
      Lang.getString(R.string.Settings)
    ) { dialog: DialogInterface?, which: Int -> Intents.openPermissionSettings() }
    showAlert(b)
  }

  protected fun openLiveLocationAlert(chatId: Long, callback: RunnableInt) {
    val b = SettingsWrapBuilder(R.id.btn_shareLiveLocation)
    b.setRawItems(
      arrayOf(
        ListItem(ListItem.TYPE_RADIO_OPTION, R.id.btn_messageLive15Minutes, 0, Lang.plural(R.string.xMinutes, 15), R.id.btn_shareLiveLocation, true),
        ListItem(ListItem.TYPE_RADIO_OPTION, R.id.btn_messageLive1Hour, 0, Lang.plural(R.string.xHours, 1), R.id.btn_shareLiveLocation, false),
        ListItem(ListItem.TYPE_RADIO_OPTION, R.id.btn_messageLive8Hours, 0, Lang.plural(R.string.xHours, 8), R.id.btn_shareLiveLocation, false)
      )
    )
    val text: String?
    if (isUserChat(chatId)) {
      text = Lang.getString(R.string.LiveLocationAlertPrivate, tdlib!!.cache().userName(tdlib.chatUserId(chatId)))
    } else {
      text = Lang.getString(R.string.LiveLocationAlertGroup)
    }
    b.addHeaderItem(ListItem(ListItem.TYPE_LIVE_LOCATION_PROMO))
    b.setAllowResize(false)
    b.addHeaderItem(text)
    b.setSaveStr(R.string.Share)
    b.setIntDelegate(SettingsIntDelegate { id: Int, result: SparseIntArray? ->
      val resId = result!!.get(id)
      val time =      when (resId) {
        R.id.btn_messageLiveTemp -> 60
        R.id.btn_messageLive15Minutes -> 60 * 15
        R.id.btn_messageLive1Hour -> 60 * 60
        R.id.btn_messageLive8Hours -> 60 * 60 * 8
        else -> null
      } ?: return@SettingsIntDelegate
      callback.runWithInt(time)
    })
    showSettings(b)
  }

  fun openMissingLocationPermissionAlert(needBackground: Boolean) {
    openMissingPermissionAlert(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && Config.REQUEST_BACKGROUND_LOCATION && needBackground) R.string.NoLocationAccessBackground else R.string.NoLocationAccess)
  }

  fun openMissingMicrophonePermissionAlert() {
    openMissingPermissionAlert(R.string.NoMicrophoneAccess)
  }

  fun openMissingStoragePermissionAlert() {
    openMissingPermissionAlert(R.string.NoStorageAccess)
  }

  fun openMissingCameraPermissionAlert() {
    openMissingPermissionAlert(R.string.NoCameraAccess)
  }

  fun openMissingGoogleMapsAlert() {
    val b = AlertDialog.Builder(context, Theme.dialogTheme())
    b.setTitle(Lang.getString(R.string.AppName))
    b.setMessage(Lang.getString(R.string.NoGoogleMaps))
    b.setPositiveButton(
      Lang.getString(R.string.Install)
    ) { dialog: DialogInterface?, which: Int -> Intents.openGooglePlay("com.google.android.apps.maps") }
    b.setNegativeButton(Lang.getString(R.string.Cancel)) { dialog: DialogInterface?, which: Int -> dialog!!.dismiss() }
    showAlert(b)
  }

  private var linkWarningDialog: AlertDialog? = null
  private var linkWarningCallbacks: MutableList<RunnableBool>? = null

  fun openSecretLinkPreviewAlert(onAcceptWarning: RunnableBool?) {
    if (onAcceptWarning != null) {
      if (this.linkWarningCallbacks == null) {
        this.linkWarningCallbacks = ArrayList<RunnableBool>()
      }
      this.linkWarningCallbacks!!.add(onAcceptWarning)
    }
    if (linkWarningDialog != null && linkWarningDialog!!.isShowing) {
      return
    }
    val after = RunnableBool { isAccepted: Boolean ->
      linkWarningDialog = null
      instance().markTutorialAsComplete(Settings.TUTORIAL_SECRET_LINK_PREVIEWS)
      instance().setUseSecretLinkPreviews(isAccepted)
      val callbacks = linkWarningCallbacks
      this.linkWarningCallbacks = null
      if (callbacks != null) {
        for (callback in callbacks) {
          callback.runWithBool(isAccepted)
        }
      }
    }
    val b = AlertDialog.Builder(context(), Theme.dialogTheme())
    b.setTitle(Lang.getString(R.string.AppName))
    b.setMessage(Lang.getString(R.string.SecretLinkPreviewAlert))
    b.setPositiveButton(
      Lang.getString(R.string.SecretLinkPreviewEnable)
    ) { dialog: DialogInterface?, which: Int -> after.runWithBool(true) }
    b.setNegativeButton(
      Lang.getString(R.string.SecretLinkPreviewDisable)
    ) { dialog: DialogInterface?, which: Int -> after.runWithBool(false) }
    b.setCancelable(false)
    linkWarningDialog = showAlert(b)
  }

  fun openLinkAlert(url: String, options: UrlOpenParameters?) {
    tdlib!!.ui().openUrl(this, url, options?.requireOpenPrompt()
      ?: UrlOpenParameters().requireOpenPrompt())
  }

  fun openOkAlert(title: String?, message: CharSequence?) {
    val b = AlertDialog.Builder(context, Theme.dialogTheme())
    if (!title.isNullOrEmpty()) {
      b.setTitle(title)
    }
    b.setMessage(message)
    b.setPositiveButton(Lang.getOK()) { dialog: DialogInterface?, which: Int -> dialog!!.dismiss() }
    showAlert(b)
  }

  // Settings delegate
  fun interface SettingsIntDelegate {
    fun onApplySettings(@IdRes id: Int, result: SparseIntArray?)
  }

  interface SettingsStringDelegate {
    fun onApplySettings(@IdRes id: Int, result: SparseArrayCompat<String?>?)
  }

  interface OnSettingItemClick {
    fun onSettingItemClick(
      view: View?,
      @IdRes settingsId: Int,
      item: ListItem?,
      doneButton: TextView?,
      settingsAdapter: SettingsAdapter?,
      window: PopupLayout?
    )
  }

  fun showSettings(@IdRes id: Int, rawItems: Array<ListItem?>?, delegate: SettingsIntDelegate?) {
    showSettings(SettingsWrapBuilder(id).setRawItems(rawItems).setIntDelegate(delegate))
  }

  fun showSettings(@IdRes id: Int, rawItems: Array<ListItem?>?, delegate: SettingsIntDelegate?, allowResize: Boolean) {
    showSettings(SettingsWrapBuilder(id).setRawItems(rawItems).setIntDelegate(delegate).setAllowResize(allowResize))
  }

  private class SettingsWrapLayout(context: Context) : FrameLayoutFix(context), MarginModifier {
    override fun onApplyMarginInsets(child: View?, params: LayoutParams, legacyInsets: Rect?, insets: Rect, insetsWithoutIme: Rect) {
      Views.setMargins(params, insets.left, 0, insets.right, 0)
      setBottomInset(insetsWithoutIme.bottom)
    }

    private var bottomInset = 0

    fun setBottomInset(extraBottomInsetWithoutIme: Int) {
      if (this.bottomInset != extraBottomInsetWithoutIme) {
        this.bottomInset = extraBottomInsetWithoutIme
        if (footerView != null) {
          footerView!!.setLayoutParams(newParams(LayoutParams.MATCH_PARENT, Screen.dp(56f) + extraBottomInsetWithoutIme, Gravity.BOTTOM))
          Views.setPaddingBottom(footerView, extraBottomInsetWithoutIme)
        }
        Views.applyBottomInset(recyclerView, if (footerView == null) extraBottomInsetWithoutIme else 0)
        Views.setBottomMargin(shadowView, Screen.dp(56f) + extraBottomInsetWithoutIme)
      }
    }

    private var recyclerView: RecyclerView? = null
    private var footerView: FrameLayout? = null
    private var shadowView: SeparatorView? = null

    fun setBottomInsetTargets(recyclerView: RecyclerView?, footerView: FrameLayout?, shadowView: SeparatorView?, inset: Int) {
      this.recyclerView = recyclerView
      this.footerView = footerView
      this.shadowView = shadowView
      this.bottomInset = inset
    }
  }

  @Suppress("deprecation")
  fun showSettings(b: SettingsWrapBuilder): SettingsWrap? {
    if (this.isStackLocked()) {
      Log.i("Ignoring showSettings because stack is locked")
      return null
    }

    val items = ArrayList<ListItem?>(b.rawItems.size * 2 + 1 + (if (b.headerItems != null && !b.headerItems!!.isEmpty()) b.headerItems!!.size + 1 else 0))
    var first = true

    items.add(ListItem(ListItem.TYPE_SHADOW_TOP))
    if (b.headerItems != null && !b.headerItems!!.isEmpty()) {
      items.addAll(b.headerItems!!)
      items.add(ListItem(ListItem.TYPE_SEPARATOR_FULL))
    }
    if (b.sizeValues != null) {
      items.add(ListItem(ListItem.TYPE_SLIDER, b.sizeOptionId, 0, b.sizeStringRes, false).setSliderInfo(b.sizeValues, b.sizeValue))
      items.add(ListItem(ListItem.TYPE_SEPARATOR_FULL))
    }
    if (b.needSeparators) {
      for (item in b.rawItems) {
        if (first) {
          first = false
        } else {
          items.add(ListItem(ListItem.TYPE_SEPARATOR_FULL))
        }
        items.add(item)
      }
    } else {
      items.ensureCapacity(items.size + b.rawItems.size)
      Collections.addAll(items, *b.rawItems)
    }

    val settingsLayout = SettingsWrapLayout(context)
    settingsLayout.setLayoutParams(newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

    val settings = SettingsWrap()

    val recyclerView: RecyclerView = object : RecyclerView(context) {
      override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_DOWN && settings.adapter != null && settings.adapter.itemCount > 0) {
          val i = (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
          if (i == 0) {
            val view = layoutManager!!.findViewByPosition(i)
            if (view != null && e.y < view.top) {
              return false
            }
          }
        }
        return super.onTouchEvent(e)
      }

      private var lastHeight = 0

      override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        super.onMeasure(widthSpec, heightSpec)

        val height = measuredHeight
        if (lastHeight != 0 && lastHeight != height) {
          lastHeight = height
          post { this.invalidateItemDecorations() }
        } else {
          lastHeight = height
        }
      }
    }
    recyclerView.addItemDecoration(BottomInsetFillingDecoration(ColorId.filling))
    settings.recyclerView = recyclerView
    if (b.allowResize) {
      recyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
          val position = parent.getChildAdapterPosition(view)

          if (position == 0) {
            val contentHeight = settings.adapter.measureHeight(-1)
            val controlsHeight = context.getRootView().systemInsetsWithoutIme.bottom + (if (b.disableFooter) 0 else Screen.dp(56f))
            outRect.top = max((Screen.currentHeight() - controlsHeight) / 2, Screen.currentHeight() - contentHeight - controlsHeight)
          } else {
            outRect.top = 0
          }
        }
      })
    }
    recyclerView.setOverScrollMode(if (Config.HAS_NICE_OVER_SCROLL_EFFECT) View.OVER_SCROLL_IF_CONTENT_SCROLLS else View.OVER_SCROLL_NEVER)
    recyclerView.setItemAnimator(null)
    recyclerView.setLayoutManager(LinearLayoutManager(context(), RecyclerView.VERTICAL, false))

    settings.window = PopupLayout(context)
    val popupLayout = settings.window
    popupLayout.setPopupHeightProvider(PopupHeightProvider {
      val manager = recyclerView.layoutManager as LinearLayoutManager?
      val firstPosition = manager!!.findFirstVisibleItemPosition()
      if (firstPosition == 0) {
        val view = manager.findViewByPosition(0)
        if (view != null) {
          return@PopupHeightProvider min(
            Screen.currentHeight(),
            min(
              settingsLayout.measuredHeight - view.top,
              settings.adapter.measureHeight(-1)
            ) + Screen.dp(56f) + extraBottomInsetWithoutIme
          )
        }
      }
      Screen.currentHeight()
    })
    popupLayout.init(true)
    if (b.needRootInsets) {
      popupLayout.setNeedRootInsets()
    }
    // popupLayout.addStatusBar();
    popupLayout.setDismissListener(b.dismissListener)
    popupLayout.setNeedFullScreen(true)

    val onClickListener = View.OnClickListener { v: View? ->
      val viewId = v!!.id
      if (viewId == R.id.btn_cancel) {
        if (b.onActionButtonClick == null || !b.onActionButtonClick!!.onActionButtonClick(settings, v, true)) {
          popupLayout.hideWindow(true)
        }
      } else if (viewId == R.id.btn_save) {
        if (b.onActionButtonClick != null && b.onActionButtonClick!!.onActionButtonClick(settings, v, false)) {
          return@OnClickListener
        }
        val type = settings.adapter.checkResultType

        when (type) {
          SettingsAdapter.SETTINGS_RESULT_INTS, SettingsAdapter.SETTINGS_RESULT_UNKNOWN -> {
            if (b.intDelegate != null) {
              b.intDelegate!!.onApplySettings(b.id, settings.adapter.getCheckIntResults())
            }
          }

          SettingsAdapter.SETTINGS_RESULT_STRING -> {
            if (b.stringDelegate != null) {
              b.stringDelegate!!.onApplySettings(b.id, settings.adapter.getCheckStringResults())
            }
          }
        }

        popupLayout.hideWindow(true)
      } else {
        val tag = v.tag
        if (!b.disableToggles) {
          settings.adapter.processToggle(v)
        }
        if (tag != null && tag is ListItem && b.onSettingItemClick != null) {
          b.onSettingItemClick!!.onSettingItemClick(v, b.id, tag, settings.doneButton, settings.adapter, settings.window)
        }
      }
    }
    settings.adapter = object : SettingsAdapter(b.tdlibDelegate ?: this, onClickListener, this) {
      protected override fun setValuedSetting(item: ListItem, view: SettingView, isUpdate: Boolean) {
        when (item.viewType) {
          ListItem.TYPE_CHECKBOX_OPTION_DOUBLE_LINE -> {
            view.setData(item.charSequenceValue)
          }

          ListItem.TYPE_CHECKBOX_OPTION, ListItem.TYPE_CHECKBOX_OPTION_WITH_AVATAR, ListItem.TYPE_CHECKBOX_OPTION_MULTILINE, ListItem.TYPE_CHECKBOX_OPTION_REVERSE -> {
            view.findCheckBox().setChecked(item.isSelected, isUpdate)
          }
        }
        if (b.settingProcessor != null) {
          b.settingProcessor!!.setValuedSetting(item, view, isUpdate)
        }
      }

      override fun setDrawerItem(item: ListItem?, view: DrawerItemView?, timerView: TimerView?, isUpdate: Boolean) {
        if (b.drawerProcessor != null) {
          b.drawerProcessor.setDrawerItem(item, view, timerView, isUpdate)
        }
      }
    }
    val checkedIndex = settings.adapter.setItems(items, true)

    var footerView: FrameLayoutFix? = null
    if (!b.disableFooter) {
      footerView = object : FrameLayoutFix(context) {
        override fun onTouchEvent(event: MotionEvent?): Boolean {
          super.onTouchEvent(event)
          return true
        }
      }
      ViewSupport.setThemedBackground(footerView, ColorId.filling, this)
      footerView.setLayoutParams(newParams(ViewGroup.LayoutParams.MATCH_PARENT, Screen.dp(56f) + extraBottomInsetWithoutIme, Gravity.BOTTOM))
      Views.setPaddingBottom(footerView, extraBottomInsetWithoutIme)

      for (i in 0..1) {
        val button: TextView = NoScrollTextView(context)

        val colorId = if (i == 1) b.saveColorId else b.cancelColorId
        button.setTextColor(Theme.getColor(colorId))
        addThemeTextColorListener(button, colorId)
        button.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
        button.setOnClickListener(onClickListener)
        button.setBackgroundResource(R.drawable.bg_btn_header)
        button.setGravity(Gravity.CENTER)
        button.setPadding(Screen.dp(16f), 0, Screen.dp(16f), 0)

        val text: CharSequence?
        if (i == 0) {
          button.setId(R.id.btn_cancel)
          button.text = b.cancelStr.uppercase(Locale.getDefault()).also { text = it }
          button.setLayoutParams(
            newParams(
              ViewGroup.LayoutParams.WRAP_CONTENT,
              Screen.dp(55f),
              Gravity.END or Gravity.BOTTOM
            )
          )
          settings.cancelButton = button
        } else {
          button.setId(R.id.btn_save)
          button.text = b.saveStr.uppercase(Locale.getDefault()).also { text = it }
          button.setLayoutParams(
            newParams(
              ViewGroup.LayoutParams.WRAP_CONTENT,
              Screen.dp(55f),
              Gravity.START or Gravity.BOTTOM
            )
          )
          settings.doneButton = button
        }
        Views.updateMediumTypeface(button, text)

        Views.setClickable(button)
        footerView.addView(button)
      }
    }

    var params = newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM)
    params.bottomMargin = if (footerView != null) Screen.dp(56f) + extraBottomInsetWithoutIme else 0

    Views.applyBottomInset(recyclerView, if (footerView == null) extraBottomInsetWithoutIme else 0)
    recyclerView.setAdapter(settings.adapter)
    recyclerView.setLayoutParams(params)
    addThemeInvalidateListener(recyclerView)

    var shadowView: SeparatorView? = null

    settingsLayout.addView(recyclerView)
    if (footerView != null) {
      settingsLayout.addView(footerView)
    }

    if (footerView != null) {
      params = newParams(ViewGroup.LayoutParams.MATCH_PARENT, Screen.dp(1f), Gravity.BOTTOM)
      params.bottomMargin = Screen.dp(56f) + extraBottomInsetWithoutIme
      shadowView = SeparatorView.simpleSeparator(context, params, true)
      shadowView.setAlignBottom()
      addThemeInvalidateListener(shadowView)
      settingsLayout.addView(shadowView)
    }

    settingsLayout.setBottomInsetTargets(recyclerView, footerView, shadowView, extraBottomInset)

    val height = settings.adapter.measureHeight(-1)
    val desiredHeight = height + (if (footerView != null) Screen.dp(56f) else 0) + extraBottomInsetWithoutIme
    val popupHeight = min(Screen.currentHeight(), desiredHeight)

    if (desiredHeight > Screen.currentActualHeight() && checkedIndex != -1) {
      val viewHeight = SettingHolder.measureHeightForType(items[checkedIndex]!!.viewType)
      (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
        checkedIndex,
        (Screen.currentActualHeight() - Screen.dp(56f)) / 2 - viewHeight / 2
      )
    }
    popupLayout.addThemeListeners(this)
    popupLayout.showSimplePopupView(settingsLayout, min(Screen.currentHeight() / 2 + Screen.dp(56f), popupHeight))

    onCreatePopupLayout(popupLayout)
    return settings
  }

  // Copy text
  fun showCopyUrlOptions(url: String, options: UrlOpenParameters?, openCallback: FutureBool?) {
    val ids = IntList(3)
    val strings = StringList(3)
    val icons = IntList(3)

    ids.append(R.id.btn_openLink)
    strings.append(R.string.Open)
    icons.append(R.drawable.baseline_open_in_browser_24)

    ids.append(R.id.btn_copyLink)
    strings.append(R.string.CopyLink)
    icons.append(R.drawable.baseline_link_24)

    ids.append(R.id.btn_shareLink)
    strings.append(R.string.Share)
    icons.append(R.drawable.baseline_forward_24)

    val shareState = intArrayOf(0)

    showOptions(url, ids.get(), strings.get(), null, icons.get(), { itemView: View?, id: Int ->
      if (id == R.id.btn_copyLink) {
        UI.copyText(url, R.string.CopiedLink)
      } else if (id == R.id.btn_shareLink) {
        if (shareState[0] == 0) {
          shareState[0] = 1
          TD.shareLink(TdlibContext(context, tdlib), url)
        }
      } else if (id == R.id.btn_openLink) {
        if (openCallback == null || !openCallback.getBoolValue()) {
          tdlib!!.ui().openUrl(this@ViewController, url, options)
        }
      }
      true
    })
  }

  // Options delegate
  @Retention(AnnotationRetention.SOURCE)
  @IntDef(OptionColor.NORMAL, OptionColor.RED, OptionColor.BLUE, OptionColor.GREEN, OptionColor.INACTIVE, OptionColor.LIGHT)
  annotation class OptionColor {
    companion object {
      const val NORMAL: Int = 1
      const val RED: Int = 2
      const val BLUE: Int = 3
      const val GREEN: Int = 4
      const val INACTIVE: Int = 5
      const val LIGHT: Int = 6
    }
  }

  fun showCallOptions(phoneNumber: String?, userId: Long) {
    if (userId == 0L) {
      Intents.openNumber(phoneNumber)
      return
    }
    showOptions(
      intArrayOf(R.id.btn_phone_call, R.id.btn_telegram_call),
      arrayOf(Lang.getString(R.string.PhoneCall), Lang.getString(R.string.VoipInCallBranding))
    ) { itemView: View?, id: Int ->
      if (id == R.id.btn_phone_call) {
        Intents.openNumber(TD.getPhoneNumber(phoneNumber))
      } else if (id == R.id.btn_telegram_call) {
        tdlib!!.context().calls().makeCall(this@ViewController, userId, null)
      }
      true
    }
  }

  fun showOptions(info: CharSequence?, ids: IntArray, titles: Array<String?>, delegate: OptionDelegate?): PopupLayout? {
    return showOptions(info, ids, titles, null, null, delegate)
  }

  fun showOptions(ids: IntArray, titles: Array<String?>, colors: IntArray?): PopupLayout? {
    return showOptions(null, ids, titles, colors, null, null)
  }

  fun showOptions(ids: IntArray, titles: Array<String?>): PopupLayout? {
    return showOptions(null, ids, titles, null, null, null)
  }

  fun showConfirm(info: CharSequence?, okString: String?, onConfirm: Runnable): PopupLayout? {
    return showConfirm(info, okString, R.drawable.baseline_check_circle_24, OptionColor.NORMAL, onConfirm)
  }

  fun showConfirm(info: CharSequence?, okString: String?, okIcon: Int, okColor: Int, onConfirm: Runnable): PopupLayout? {
    return showOptions(
      info,
      intArrayOf(R.id.btn_done, R.id.btn_cancel),
      arrayOf(okString ?: Lang.getString(R.string.OK), Lang.getString(R.string.Cancel)),
      intArrayOf(okColor, OptionColor.NORMAL),
      intArrayOf(okIcon, R.drawable.baseline_cancel_24),
      { itemView: View?, id: Int ->
        if (id == R.id.btn_done) {
          onConfirm.run()
        }
        true
      })
  }

  fun showOptions(ids: IntArray, titles: Array<String?>, delegate: OptionDelegate?): PopupLayout? {
    return showOptions(null, ids, titles, null, null, delegate)
  }

  fun showOptions(ids: IntArray, titles: Array<String?>, colors: IntArray?, delegate: OptionDelegate?): PopupLayout? {
    return showOptions(null, ids, titles, colors, null, delegate)
  }

  fun showUnsavedChangesPromptBeforeLeaving(onConfirm: Runnable?) {
    showUnsavedChangesPromptBeforeLeaving(null, Lang.getString(R.string.DiscardChanges), onConfirm)
  }

  fun showUnsavedChangesPromptBeforeLeaving(info: CharSequence?, discardText: String, onConfirm: Runnable?) {
    showOptions(
      info,
      intArrayOf(R.id.btn_done, R.id.btn_cancel),
      arrayOf(discardText, Lang.getString(R.string.Cancel)),
      intArrayOf(OptionColor.RED, OptionColor.NORMAL),
      intArrayOf(R.drawable.baseline_delete_24, R.drawable.baseline_cancel_24),
      { itemView: View?, id: Int ->
        if (id == R.id.btn_done) {
          onConfirm?.run()
          navigateBack()
        }
        true
      })
  }

  fun showWarning(info: CharSequence?, callback: RunnableBool): PopupLayout? {
    return showOptions(
      info,
      intArrayOf(R.id.btn_done, R.id.btn_cancel),
      arrayOf(Lang.getString(R.string.TdlibLogsWarningConfirm), Lang.getString(R.string.Cancel)),
      intArrayOf(
        OptionColor.RED, OptionColor.NORMAL
      ),
      intArrayOf(R.drawable.baseline_warning_24, R.drawable.baseline_cancel_24),
      { itemView: View?, id: Int ->
        callback.runWithBool(id == R.id.btn_done)
        true
      })
  }

  class OptionItem(
    @JvmField val id: Int,
    @JvmField val name: CharSequence?,
    @JvmField @field:OptionColor @param:OptionColor val textColor: Int,
    @JvmField val icon: Int,
    @JvmField @field:OptionColor @param:OptionColor val iconColor: Int
  ) {
    constructor(id: Int, name: CharSequence?, @OptionColor color: Int, icon: Int) : this(id, name, color, icon, color)

    class Builder {
      private var id = 0
      private var name: CharSequence? = null
      private var textColor = OptionColor.NORMAL
      private var iconColor = OptionColor.NORMAL
      private var icon = 0

      fun id(id: Int): Builder {
        this.id = id
        return this
      }

      fun name(name: CharSequence?): Builder {
        this.name = name
        return this
      }

      fun name(resId: Int): Builder {
        return name(Lang.getString(resId))
      }

      fun color(@OptionColor color: Int): Builder {
        this.textColor = color
        this.iconColor = color
        return this
      }

      fun color(@OptionColor textColor: Int, @OptionColor iconColor: Int): Builder {
        this.textColor = textColor
        this.iconColor = iconColor
        return this
      }

      fun icon(icon: Int): Builder {
        this.icon = icon
        return this
      }

      fun build(): OptionItem {
        return OptionItem(id, name, textColor, icon, iconColor)
      }
    }

    companion object {
      @JvmField
      val SEPARATOR: OptionItem = OptionItem(0, null, OptionColor.NORMAL, 0, 0)
    }
  }

  class Options(@JvmField val info: CharSequence?, val title: CharSequence?, @JvmField val subtitle: OptionItem?, @JvmField val items: Array<OptionItem>, val maxLineCount: Int) {
    var ignoreOtherPopUps: Boolean = false

    class Builder {
      private var info: CharSequence? = null
      private var title: CharSequence? = null
      private var subtitle: OptionItem? = null
      private val items: MutableList<OptionItem?> = ArrayList<OptionItem?>()
      private var maxLineCount = Text.LINE_COUNT_UNLIMITED

      fun title(header: CharSequence?): Builder {
        this.title = header
        return this
      }

      fun subtitle(item: OptionItem?): Builder {
        this.subtitle = item
        return this
      }

      fun info(info: CharSequence?): Builder {
        this.info = info
        return this
      }

      fun item(item: OptionItem?): Builder {
        if (item != null) {
          items.add(item)
        }
        return this
      }

      fun items(ids: IntArray, titles: Array<String?>, colors: IntArray?, icons: IntArray?): Builder {
        for (i in ids.indices) {
          val item = OptionItem.Builder()
            .id(ids[i])
            .name(titles[i])
            .color(colors?.get(i) ?: OptionColor.NORMAL)
            .icon(icons?.get(i) ?: 0)
            .build()
          items.add(item)
        }
        return this
      }

      fun cancelItem(): Builder {
        return item(
          OptionItem.Builder()
            .id(R.id.btn_cancel)
            .name(R.string.Cancel)
            .icon(R.drawable.baseline_cancel_24)
            .build()
        )
      }

      fun maxLineCount(maxLineCount: Int): Builder {
        this.maxLineCount = maxLineCount
        return this
      }

      fun itemCount(): Int {
        return items.size
      }

      fun build(): Options {
        return Options(info, title, subtitle, items.filterNotNull().toTypedArray(), maxLineCount)
      }
    }
  }

  @JvmOverloads
  fun showOptions(
    info: CharSequence?,
    ids: IntArray,
    titles: Array<String?>,
    colors: IntArray? = null,
    icons: IntArray? = null,
    delegate: OptionDelegate? = null,
    forcedTheme: ThemeDelegate? = null
  ): PopupLayout? {
    return showOptions(info, ids, titles, colors, icons, Text.LINE_COUNT_UNLIMITED, delegate, forcedTheme)
  }

  fun showOptions(
    info: CharSequence?,
    ids: IntArray,
    titles: Array<String?>,
    colors: IntArray?,
    icons: IntArray?,
    maxLineCount: Int,
    delegate: OptionDelegate?,
    forcedTheme: ThemeDelegate?
  ): PopupLayout? {
    return showOptions(getOptions(info, ids, titles, colors, icons, maxLineCount), delegate, forcedTheme)
  }

  fun getOptions(info: CharSequence?, ids: IntArray, titles: Array<String?>, colors: IntArray?, icons: IntArray?, maxLineCount: Int): Options {
    val items: Array<OptionItem> = Array(ids.size) { i ->
      OptionItem.Builder()
        .id(ids[i])
        .name(titles[i])
        .color(colors?.get(i) ?: OptionColor.NORMAL)
        .icon(icons?.get(i) ?: 0)
        .build()
    }
    return Options(info, null, null, items, maxLineCount)
  }

  @JvmOverloads
  fun showOptions(options: Options, delegate: OptionDelegate?, forcedTheme: ThemeDelegate? = null): PopupLayout? {
    if (this.isStackLocked()) {
      Log.i("Ignoring options show because stack is locked")
      return null
    }

    val popupLayout = PopupLayout(context).apply {
      tag = this@ViewController
      init(true)
      setDismissOtherPopUps(!options.ignoreOtherPopUps)
      setNeedFullScreen(true)
      if (delegate != null) {
        setDisableCancelOnTouchDown(delegate.disableCancelOnTouchdown())
      }
    }

    val optionsWrap = OptionsLayout(context(), this, forcedTheme).apply {
      setHeader(options.title)
      options.subtitle?.let { setSubtitle(options.subtitle) }

      setInfo(this@ViewController, tdlib(), options.info, false, options.maxLineCount)
      setLayoutParams(newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))
    }

    val shadowView = ShadowView(context)
    shadowView.setSimpleTopShadow(true)
    optionsWrap.addView(shadowView, 0)
    addThemeInvalidateListener(shadowView)

    // Item generation
    val onClickListener = if (delegate != null) {
      View.OnClickListener { v: View? ->
        if (delegate.onOptionItemPressed(v, v!!.id)) {
          popupLayout.hideWindow(true)
        }
      }
    } else {
      View.OnClickListener { v: View? ->
        val c = context.navigation().currentStackItem
        if (c is OptionDelegate && (c as OptionDelegate).onOptionItemPressed(v, v!!.id)) {
          popupLayout.hideWindow(true)
        }
      }
    }
    var totalHeight = shadowView.layoutParams.height + optionsWrap.textHeight + extraBottomInsetWithoutIme
    var index = 0
    for (item in options.items) {
      if (item === OptionItem.SEPARATOR) {
        val shadowViewBottom = ShadowView(context)
        shadowViewBottom.setSimpleBottomTransparentShadow(false)
        ViewSupport.setThemedBackground(shadowViewBottom, ColorId.background, this)
        addThemeInvalidateListener(shadowViewBottom)
        optionsWrap.addView(shadowViewBottom, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Screen.dp(6f)))

        val shadowViewTop = ShadowView(context)
        shadowViewTop.setSimpleTopShadow(true, this)
        addThemeInvalidateListener(shadowViewTop)
        optionsWrap.addView(shadowViewTop, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Screen.dp(6f)))
        index++
        totalHeight += shadowViewBottom.layoutParams.height + shadowViewTop.layoutParams.height
        continue
      }
      val text = OptionsLayout.genOptionView(
        context,
        item.id,
        item.name,
        item.textColor,
        item.icon,
        item.iconColor,
        onClickListener,
        getThemeListeners(),
        forcedTheme
      )
      RippleSupport.setTransparentSelector(text)
      forcedTheme?.let { Theme.forceTheme(text, it) }
      text.setLayoutParams(LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Screen.dp(54f)))
      delegate?.let {
        text.tag = delegate.getTagForItem(index)
      }
      optionsWrap.addView(text)
      index++
      totalHeight += text.layoutParams.height
    }

    // Window
    popupLayout.showSimplePopupView(optionsWrap, totalHeight)
    onCreatePopupLayout(popupLayout)

    return popupLayout
  }

  fun interface PopUpBuilder {
    fun onBuildPopUp(popupLayout: PopupLayout?, optionsLayout: OptionsLayout?): Int
  }

  fun showPopup(title: CharSequence?, isTitle: Boolean, maxLineCount: Int, popUpBuilder: PopUpBuilder, forcedTheme: ThemeDelegate?): PopupLayout {
    val popupLayout = PopupLayout(context)
    popupLayout.tag = this
    popupLayout.init(true)
    popupLayout.setNeedFullScreen(true)

    var totalHeight = 0

    val optionsWrap = OptionsLayout(context(), this, forcedTheme)
    optionsWrap.setInfo(this, tdlib(), title, isTitle, maxLineCount)
    optionsWrap.setLayoutParams(newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))
    totalHeight += optionsWrap.textHeight

    val shadowView = ShadowView(context)
    shadowView.setSimpleTopShadow(true)
    optionsWrap.addView(shadowView, 0)
    addThemeInvalidateListener(shadowView)
    totalHeight += shadowView.layoutParams.height

    totalHeight += popUpBuilder.onBuildPopUp(popupLayout, optionsWrap)
    totalHeight += extraBottomInsetWithoutIme

    popupLayout.showSimplePopupView(optionsWrap, totalHeight)
    onCreatePopupLayout(popupLayout)
    return popupLayout
  }

  fun showText(title: CharSequence?, text: CharSequence?, entities: Array<TextEntity?>?, forcedTheme: ThemeDelegate?): PopupLayout {
    return showPopup(title, true, Text.LINE_COUNT_UNLIMITED, { popupLayout: PopupLayout?, optionsLayout: OptionsLayout? ->
      val textView = CustomTextView(context, tdlib)
      textView.setPadding(Screen.dp(16f), Screen.dp(12f), Screen.dp(16f), Screen.dp(16f))
      textView.setTextColorId(ColorId.text)
      textView.setText(text, entities, false)
      textView.setLayoutParams(LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
      optionsLayout!!.addView(textView)
      textView.getCurrentHeight(context.getControllerWidth(textView))
    }, forcedTheme)
  }

  interface CalendarDatePickerListener {
    fun onCommitDate(picker: ColumnDataPicker?, commitButtonView: View?, newDay: Int, newMonth: Int, newYear: Int): Boolean
    fun onPendingCommitDateChanged(picker: ColumnDataPicker?, newDay: Int, newMonth: Int, newYear: Int) {}
  }

  fun showCalendarDatePicker(
    title: CharSequence?,
    commitButtonText: CharSequence?,
    day: Int,
    month: Int,
    year: Int,
    yearOptional: Boolean,
    listener: CalendarDatePickerListener?
  ): ColumnDataPicker {
    val dataPicker = ColumnDataPicker()

    val c = Calendar.getInstance()

    val minYear = 1875
    val minDayOfMonth = 1
    val minMonth = Calendar.JANUARY
    val maxYear = c.get(Calendar.YEAR)
    val maxMonth = c.get(Calendar.MONTH)
    val maxDayOfMonth = c.get(Calendar.DAY_OF_MONTH)

    c.set(Calendar.DAY_OF_MONTH, 1)
    c.set(Calendar.MONTH, month)
    c.set(Calendar.YEAR, if (year != 0) year else 2024 /*leap year*/)

    val daysCount = c.getActualMaximum(Calendar.DAY_OF_MONTH)
    val dayItems: MutableList<SimpleStringItem?> = ArrayList<SimpleStringItem?>()
    for (currentDay in 1..daysCount) {
      val str = if (currentDay < 10) "0$currentDay" else currentDay.toString()
      dayItems.add(
        SimpleStringItem(0, str)
          .setArg1(currentDay.toLong())
      )
    }

    val monthItemsRaw = Lang.getMonths(Lang.locale())
    val monthItems: MutableList<SimpleStringItem?> = ArrayList<SimpleStringItem?>()
    for ((currentMonth, monthItem) in monthItemsRaw.withIndex()) {
      monthItems.add(
        SimpleStringItem(0, monthItem)
          .setArg1(currentMonth.toLong())
      )
    }

    val yearItems: MutableList<SimpleStringItem?> = ArrayList<SimpleStringItem?>()
    for (currentYear in minYear..maxYear) {
      val str = currentYear.toString()
      yearItems.add(
        SimpleStringItem(0, str)
          .setArg1(currentYear.toLong())
      )
    }
    if (yearOptional) {
      yearItems.add(SimpleStringItem(0, "—"))
    }
    val yearIndex = if (yearOptional && year == 0) yearItems.size - 1 else year - minYear

    val dayColumn = ColumnDataPicker.Column(dayItems, StylingOptions(1f).setNoPadding(true), day - 1)
    val monthColumn = ColumnDataPicker.Column(monthItems, StylingOptions(2.5f), month)
    val yearColumn = ColumnDataPicker.Column(yearItems, StylingOptions(1f).setNoPadding(true), yearIndex)

    val columns = listOf(
      dayColumn,
      monthColumn,
      yearColumn
    )

    val getDay: FutureInt = object : FutureInt {
      override fun getIntValue(): Int = dayColumn.index + 1
    }
    val getMonth: FutureInt = object : FutureInt {
      override fun getIntValue(): Int = monthColumn.index
    }
    val getYear: FutureInt = object : FutureInt {
      override fun getIntValue(): Int = if (yearOptional && yearColumn.index == yearColumn.rows.size - 1) 0 else minYear + yearColumn.index
    }

    val onValueChanged = RunnableBool { checkMaxDay: Boolean ->
      if (checkMaxDay) {
        val maxDayCount = c.getActualMaximum(Calendar.DAY_OF_MONTH)
        val prevMaxDayCount = dayColumn.rows.size
        while (dayColumn.rows.size != maxDayCount) {
          if (dayColumn.rows.size > maxDayCount) {
            dayColumn.rows.removeAt(dayColumn.rows.size - 1)
          } else {
            val addedDay = dayColumn.rows.size + 1
            val str = if (addedDay < 10) "0$addedDay" else addedDay.toString()
            dayColumn.rows.add(
              SimpleStringItem(0, str)
                .setArg1(addedDay.toLong())
            )
          }
        }
        if (dayColumn.index >= maxDayCount) {
          dayColumn.index = maxDayCount - 1
          // c.set(Calendar.DAY_OF_MONTH, maxDayCount);
        }
        if (maxDayCount > prevMaxDayCount) {
          dayColumn.view.addRange(prevMaxDayCount, dayColumn.rows.subList(prevMaxDayCount, maxDayCount))
        } else if (prevMaxDayCount > maxDayCount) {
          dayColumn.view.removeRange(maxDayCount, prevMaxDayCount - maxDayCount)
        }
      }
      listener?.let {
        val newDay = getDay.getIntValue()
        val newMonth = getMonth.getIntValue()
        val newYear = getYear.getIntValue()
        listener.onPendingCommitDateChanged(dataPicker, newDay, newMonth, newYear)
      }
    }

    dayColumn.setMinMaxProvider { v: InfiniteRecyclerView<SimpleStringItem?>?, index: Int ->
      var index = index
      val newMonth = getMonth.getIntValue()
      val newYear = getYear.getIntValue()
      if (newYear == maxYear && newMonth == maxMonth) {
        index = min(index, maxDayOfMonth - 1)
      }
      if (newYear == minYear && newMonth == minMonth) {
        index = max(index, minDayOfMonth - 1)
      }
      index
    }
    dayColumn.setItemChangeListener { v: InfiniteRecyclerView<SimpleStringItem?>?, index: Int ->
      if (dayColumn.index != index) {
        dayColumn.index = index
        // c.set(Calendar.DAY_OF_MONTH, index + 1);
        onValueChanged.runWithBool(false)
      }
    }
    monthColumn.setMinMaxProvider { v: InfiniteRecyclerView<SimpleStringItem?>?, index: Int ->
      var index = index
      val newYear = getYear.getIntValue()
      if (newYear == maxYear) {
        index = min(index, maxMonth)
      }
      if (newYear == minYear) {
        index = max(index, minMonth)
      }
      index
    }
    monthColumn.setItemChangeListener(object : ItemChangeListener<SimpleStringItem?> {
      override fun onCurrentIndexChanged(v: InfiniteRecyclerView<SimpleStringItem?>?, newMonth: Int) {
        if (monthColumn.index != newMonth) {
          monthColumn.index = newMonth
          c.set(Calendar.MONTH, newMonth)
          onValueChanged.runWithBool(true)
        }
      }

      override fun onCurrentIndexFinalized(v: InfiniteRecyclerView<SimpleStringItem?>?, index: Int) {
        dayColumn.view.checkFitsMinMax()
      }
    })
    yearColumn.setItemChangeListener(object : ItemChangeListener<SimpleStringItem?> {
      override fun onCurrentIndexChanged(v: InfiniteRecyclerView<SimpleStringItem?>?, newYearIndex: Int) {
        if (yearColumn.index != newYearIndex) {
          yearColumn.index = newYearIndex
          val newYear = if (yearOptional && newYearIndex == yearItems.size - 1) 2024 else minYear + newYearIndex
          c.set(Calendar.YEAR, newYear)
          onValueChanged.runWithBool(true)
        }
      }

      override fun onCurrentIndexFinalized(v: InfiniteRecyclerView<SimpleStringItem?>?, newYearIndex: Int) {
        monthColumn.view.checkFitsMinMax()
        dayColumn.view.checkFitsMinMax()
      }
    })

    dataPicker.setColumns(columns)
    dataPicker.setSpacing(.5f, .5f)
    dataPicker.showPopup(this, title, commitButtonText, null) { picker: ColumnDataPicker?, commitButtonView: View? ->
      val newDay = getDay.getIntValue()
      val newMonth = getMonth.getIntValue()
      val newYear = getYear.getIntValue()
      listener != null && listener.onCommitDate(picker, commitButtonView, newDay, newMonth, newYear)
    }
    return dataPicker
  }

  fun showDateTimePicker(
    title: CharSequence?,
    @StringRes todayRes: Int,
    @StringRes tomorrowRes: Int,
    @StringRes futureRes: Int,
    callback: RunnableLong,
    forcedTheme: ThemeDelegate?
  ): PopupLayout {
    return showDateTimePicker(tdlib!!, title, todayRes, tomorrowRes, futureRes, callback, forcedTheme)
  }

  fun showDateTimePicker(
    tdlib: Tdlib,
    title: CharSequence?,
    @StringRes todayRes: Int,
    @StringRes tomorrowRes: Int,
    @StringRes futureRes: Int,
    callback: RunnableLong,
    forcedTheme: ThemeDelegate?
  ): PopupLayout {
    return showPopup(title, true, Text.LINE_COUNT_UNLIMITED, { popupLayout: PopupLayout?, optionsWrap: OptionsLayout? ->
      var contentHeight = 0
      val pickerHeight = InfiniteRecyclerView.getItemHeight() * 5

      val datePickerWrap = LinearLayout(context)
      datePickerWrap.orientation = LinearLayout.HORIZONTAL
      datePickerWrap.setLayoutParams(ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, pickerHeight))
      setBackground(datePickerWrap, object : Drawable() {
        override fun draw(c: Canvas) {
          val bounds = getBounds()
          val viewWidth = bounds.width()
          val viewHeight = bounds.height()

          val cy = viewHeight / 2
          val h2 = InfiniteRecyclerView.getItemHeight() / 2

          c.drawLine(
            0f,
            (cy - h2).toFloat(),
            viewWidth.toFloat(),
            (cy - h2).toFloat(),
            Paints.strokeSeparatorPaint(forcedTheme?.getColor(ColorId.separator)
              ?: Theme.separatorColor())
          )
          c.drawLine(
            0f,
            (cy + h2).toFloat(),
            viewWidth.toFloat(),
            (cy + h2).toFloat(),
            Paints.strokeSeparatorPaint(forcedTheme?.getColor(ColorId.separator)
              ?: Theme.separatorColor())
          )
        }

        override fun setAlpha(alpha: Int) {}

        override fun setColorFilter(colorFilter: ColorFilter?) {}

        @Suppress("deprecation")
        override fun getOpacity(): Int {
          return PixelFormat.UNKNOWN
        }
      })
      if (forcedTheme == null) addThemeInvalidateListener(datePickerWrap)

      val nowMs = tdlib.currentTimeMillis()
      val c = calendarInstance(nowMs)

      val startAddMinutes = 2
      val minAddMinutes = 1

      val currentDay = c.get(Calendar.DAY_OF_YEAR)
      c.set(Calendar.SECOND, 0)
      c.set(Calendar.MILLISECOND, 0)
      c.add(Calendar.MINUTE, startAddMinutes)
      val startDay = if (c.get(Calendar.DAY_OF_YEAR) != currentDay) 1 else 0
      val startHour = c.get(Calendar.HOUR_OF_DAY)
      val startMinute = c.get(Calendar.MINUTE)

      val sendView = TextView(context)
      val updateSendButton = RunnableLong { targetMillis: Long ->
        val text: String?
        if (isToday(targetMillis, TimeUnit.MILLISECONDS)) {
          text = Lang.getString(todayRes, Lang.time(targetMillis, TimeUnit.MILLISECONDS))
        } else if (isTomorrow(targetMillis, TimeUnit.MILLISECONDS)) {
          text = Lang.getString(tomorrowRes, Lang.time(targetMillis, TimeUnit.MILLISECONDS))
        } else {
          text = Lang.getString(futureRes, Lang.getDate(targetMillis, TimeUnit.MILLISECONDS), Lang.time(targetMillis, TimeUnit.MILLISECONDS))
        }
        Views.setMediumText(sendView, text.uppercase(Locale.getDefault()))
      }

      val dayCount = 366
      val days = ArrayList<SimpleStringItem?>(dayCount)
      for (day in startDay..<dayCount) {
        c.setTimeInMillis(nowMs)
        c.add(Calendar.DAY_OF_MONTH, day)
        val startMillis = getStartOfDay(c)
        days.add(
          SimpleStringItem(
            0,
            when (day) {
              0 -> Lang.getString(R.string.Today)
              1 -> Lang.getString(R.string.Tomorrow)
              else -> Lang.getDate(
                startMillis,
                TimeUnit.MILLISECONDS
              )
            }
          )
            .setArgs(c.get(Calendar.YEAR).toLong(), c.get(Calendar.DAY_OF_YEAR).toLong())
        )
      }
      val dayPicker = InfiniteRecyclerView<SimpleStringItem?>(context(), false)
      val hourPickerFinal = AtomicReference<InfiniteRecyclerView<SimpleStringItem?>?>()
      val minutePickerFinal = AtomicReference<InfiniteRecyclerView<SimpleStringItem?>?>()
      val calculateDate: FutureLong = object : FutureLong {
        override fun getLongValue(): Long {
          val dayItem = dayPicker.getCurrentItem()
          val hourItem = hourPickerFinal.get()!!.getCurrentItem()
          val minuteItem = minutePickerFinal.get()!!.getCurrentItem()
          if (dayItem == null || hourItem == null || minuteItem == null) return 0L
          val year = dayItem.arg1.toInt()
          val dayOfYear = dayItem.arg2.toInt()
          val hour = hourItem.arg1.toInt()
          val minute = minuteItem.arg1.toInt()
          c.set(Calendar.YEAR, year)
          c.set(Calendar.DAY_OF_YEAR, dayOfYear)
          c.set(Calendar.HOUR_OF_DAY, hour)
          c.set(Calendar.MINUTE, minute)
          c.set(Calendar.SECOND, 0)
          c.set(Calendar.MILLISECOND, 0)
          return c.getTimeInMillis()
        }
      }
      val listener = ItemChangeListener { v: InfiniteRecyclerView<SimpleStringItem?>?, index: Int ->
        updateSendButton.runWithLong(calculateDate.getLongValue())
      }
      dayPicker.setNeedSeparators(false)
      dayPicker.minMaxProvider = MinMaxProvider { v: InfiniteRecyclerView<SimpleStringItem?>?, index: Int ->
        c.setTimeInMillis(tdlib.currentTimeMillis())
        c.add(Calendar.MINUTE, minAddMinutes)
        val year = c.get(Calendar.YEAR)
        val day = c.get(Calendar.DAY_OF_YEAR)
        for (minIndex in days.indices) {
          if (days[minIndex]!!.arg1 == year.toLong() && days[minIndex]!!.arg2 == day.toLong()) {
            return@MinMaxProvider max(minIndex, index)
          }
        }
        index
      }
      dayPicker.setItemChangeListener { v: InfiniteRecyclerView<SimpleStringItem?>?, index: Int ->
        val millis = calculateDate.getLongValue()
        if (millis < tdlib.currentTimeMillis()) {
          c.setTimeInMillis(tdlib.currentTimeMillis())
          c.add(Calendar.MINUTE, minAddMinutes)
          val hour = c.get(Calendar.HOUR_OF_DAY)
          val minute = c.get(Calendar.MINUTE)
          hourPickerFinal.get()?.setCurrentItem(hour)
          minutePickerFinal.get()?.setCurrentItem(minute)
        }
        listener.onCurrentIndexChanged(v, index)
      }
      dayPicker.setForcedTheme(forcedTheme)
      dayPicker.addThemeListeners(this)
      dayPicker.initWithItems(days, 0)
      dayPicker.setLayoutParams(LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 2.5f))
      datePickerWrap.addView(dayPicker)

      val hours = ArrayList<SimpleStringItem?>()
      for (hour in 0..23) {
        c.set(Calendar.HOUR_OF_DAY, hour)
        hours.add(SimpleStringItem(0, Lang.hour(c.getTimeInMillis(), TimeUnit.MILLISECONDS)).setArg1(hour.toLong()))
      }
      val hourPicker = InfiniteRecyclerView<SimpleStringItem?>(context(), false)
      hourPickerFinal.set(hourPicker)
      hourPicker.setTrimItems(false)
      hourPicker.minMaxProvider = MinMaxProvider { v: InfiniteRecyclerView<SimpleStringItem?>?, index: Int ->
        val minDayIndex = dayPicker.getMinMaxProvider().getMinMax(v, 0)
        if (dayPicker.currentIndex == minDayIndex) {
          c.setTimeInMillis(tdlib.currentTimeMillis())
          c.add(Calendar.MINUTE, minAddMinutes)
          return@MinMaxProvider max(index, c.get(Calendar.HOUR_OF_DAY))
        }
        index
      }
      hourPicker.setNeedSeparators(false)
      hourPicker.setItemChangeListener(listener)
      hourPicker.setForcedTheme(forcedTheme)
      hourPicker.addThemeListeners(this)
      hourPicker.initWithItems(hours, startHour)
      hourPicker.setLayoutParams(LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
      datePickerWrap.addView(hourPicker)

      val minutes = ArrayList<SimpleStringItem?>()
      for (minute in 0..59) {
        minutes.add(
          SimpleStringItem(0, if (minute < 10) "0$minute" else minute.toString())
            .setArg1(minute.toLong())
        )
      }
      val minutePicker = InfiniteRecyclerView<SimpleStringItem?>(context(), false)
      minutePickerFinal.set(minutePicker)
      minutePicker.setNeedSeparators(false)
      minutePicker.setItemChangeListener(listener)
      minutePicker.minMaxProvider = MinMaxProvider { v: InfiniteRecyclerView<SimpleStringItem?>?, index: Int ->
        val minDayIndex = dayPicker.getMinMaxProvider().getMinMax(v, 0)
        val minHourIndex = hourPicker.getMinMaxProvider().getMinMax(v, 0)
        if (dayPicker.currentIndex == minDayIndex && hourPicker.currentIndex == minHourIndex) {
          c.setTimeInMillis(tdlib.currentTimeMillis())
          c.add(Calendar.MINUTE, minAddMinutes)
          return@MinMaxProvider max(index, c.get(Calendar.MINUTE))
        }
        index
      }
      minutePicker.setForcedTheme(forcedTheme)
      minutePicker.setTrimItems(false)
      minutePicker.addThemeListeners(this)
      minutePicker.initWithItems(minutes, startMinute)
      minutePicker.setLayoutParams(LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
      datePickerWrap.addView(minutePicker)

      val emptyView = View(context)
      emptyView.setLayoutParams(LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, .5f))
      datePickerWrap.addView(emptyView)

      optionsWrap!!.addView(datePickerWrap)
      contentHeight += pickerHeight

      Views.setClickable(sendView)
      sendView.setGravity(Gravity.CENTER)
      sendView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
      sendView.setLayoutParams(LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Screen.dp(56f)))
      val drawable = ViewSupport.setThemedBackground(sendView, ColorId.fillingPositive, if (forcedTheme != null) null else this)
      drawable.setForcedTheme(forcedTheme)
      if (forcedTheme != null) {
        sendView.setTextColor(forcedTheme.getColor(ColorId.fillingPositiveContent))
      } else {
        sendView.setTextColor(Theme.getColor(ColorId.fillingPositiveContent))
        addThemeTextColorListener(sendView, ColorId.fillingPositiveContent)
      }
      contentHeight += Screen.dp(56f)
      sendView.setOnClickListener { v: View? ->
        val millis = calculateDate.getLongValue()
        if (tdlib.currentTimeMillis() < millis) {
          callback.runWithLong(millis)
          popupLayout!!.hideWindow(true)
        }
      }
      listener.onCurrentIndexChanged(null, -1)
      optionsWrap.addView(sendView)
      contentHeight
    }, forcedTheme)
  }

  // Other
  override fun context(): BaseActivity {
    return context
  }

  override fun tdlib(): Tdlib? {
    return tdlib
  }

  fun tdlibId(): Int {
    return tdlib?.id() ?: TdlibAccount.NO_ID
  }

  fun showErrorTooltip(view: View?, text: CharSequence?): TooltipInfo? {
    return showTooltip(view, R.drawable.baseline_error_24, text)
  }

  fun showWarningTooltip(view: View?, text: CharSequence?): TooltipInfo? {
    return showTooltip(view, R.drawable.baseline_warning_24, text)
  }

  fun showInfoTooltip(view: View?, text: CharSequence?): TooltipInfo? {
    return showTooltip(view, R.drawable.baseline_info_24, text)
  }

  fun showTooltip(view: View?, @DrawableRes icon: Int, text: CharSequence?): TooltipInfo? {
    return context.tooltipManager()
      .builder(view)
      .show(this, tdlib, icon, text)
  }

  fun openTdlibLogs(testerLevel: Int, crashInfo: Crash?) {
    showWarning(Lang.getMarkdownString(this, R.string.TdlibLogsWarning)) { proceed: Boolean ->
      if (proceed) {
        val c = SettingsBugController(context, tdlib)
        c.setArguments(SettingsBugController.Args(SettingsBugController.Section.TDLIB, crashInfo).setTesterLevel(testerLevel))
        navigateTo(c)
      }
    }
  }

  fun openExperimentalSettings(testerLevel: Int) {
    showWarning(Lang.getMarkdownStringSecure(this, R.string.ExperimentalSettingsWarning)) { proceed: Boolean ->
      if (proceed) {
        val c = SettingsBugController(context, tdlib)
        c.setArguments(SettingsBugController.Args(SettingsBugController.Section.EXPERIMENTS).setTesterLevel(testerLevel))
        navigateTo(c)
      }
    }
  }

  private var pendingActivityRestart: CancellableRunnable? = null

  fun cancelPendingActivityRestart() {
    if (pendingActivityRestart != null) {
      pendingActivityRestart!!.cancel()
      pendingActivityRestart = null
    }
  }

  fun scheduleActivityRestart() {
    cancelPendingActivityRestart()
    pendingActivityRestart = object : CancellableRunnable() {
      override fun act() {
        pendingActivityRestart = null
        context.recreate()
      }
    }
    pendingActivityRestart!!.removeOnCancel(UI.getAppHandler())
    UI.getAppHandler().postDelayed(pendingActivityRestart!!, 300L)
  }

  fun isSameTdlib(tdlib: Tdlib): Boolean {
    return tdlibId() == tdlib.id() // && this.tdlib.isDebug() == tdlib.isDebug();
  }

  fun isSameAccount(account: TdlibAccount): Boolean {
    return tdlibId() == account.id // && tdlib.isDebug() == account.debug;
  }

  open fun isFocused(): Boolean {
    return (flags and FLAG_FOCUSED) != 0
  }

  open fun isPaused(): Boolean {
    return (flags and FLAG_PAUSED) != 0
  }

  open fun isDestroyed(): Boolean {
    return (flags and FLAG_DESTROYED) != 0
  }

  open val isIntercepted: Boolean
    get() = false

  open fun canSlideBackFrom(navigationController: NavigationController, x: Float, y: Float): Boolean {
    return true
  }

  open fun dismissIntercept() {}

  fun getKeyboardState(): Boolean {
    return (flags and FLAG_KEYBOARD_STATE) != 0
  }

  protected fun preventHideKeyboardOnBlur() {
    setFlags(this.flags or FLAG_PREVENT_KEYBOARD_HIDE)
  }

  override fun getValue(): View? {
    if (this.wrapUnchecked == null) {
      this.wrapUnchecked = onCreateView(context())
      wrapUnchecked!!.tag = this
      tdlib?.incrementUiReferenceCount()
      subscribeToNeededUpdates()
    }
    return this.wrapUnchecked
  }

  protected abstract fun onCreateView(context: Context): View?

  @CallSuper
  open fun saveInstanceState(outState: Bundle, keyPrefix: String?): Boolean {
    return false
  }

  @CallSuper
  open fun restoreInstanceState(`in`: Bundle, keyPrefix: String?): Boolean {
    return false
  }

  @CallSuper
  override fun onActivityPause() {
    var flags = this.flags
    flags = flags and FLAG_KEYBOARD_SHOWN.inv()
    flags = flags or FLAG_PAUSED
    setFlags(flags)
  }

  @CallSuper
  override fun onActivityResume() {
    if (lockFocusView != null && lockFocusView!!.isEnabled && this.isPaused() && (flags and FLAG_KEYBOARD_SHOWN) == 0 && navigationController != null && !navigationController!!.isAnimating) {
      if (((flags and FLAG_LOCK_ALWAYS) != 0 || (flags and FLAG_KEYBOARD_STATE) != 0) && !context.isPasscodeShowing && !context.isWindowPopupShowing) {
        setFlags(flags or FLAG_KEYBOARD_SHOWN)
        UI.showKeyboardDelayed(lockFocusView)
      } else {
        Keyboard.hide(lockFocusView)
      }
    }
    setFlags(flags and (FLAG_PAUSED.inv()))
  }

  @CallSuper
  override fun onActivityPermissionResult(code: Int, granted: Boolean) {
  }

  @CallSuper
  override fun onActivityDestroy() {
    if (!this.isDestroyed()) {
      destroy()
    }
  }

  @CallSuper
  open fun onConfigurationChanged(newConfig: Configuration?) {
    /*View customHeader = getCustomHeaderCell();
    if (customHeader != null && customHeader instanceof ComplexHeaderView) {
      ((ComplexHeaderView) customHeader).rebuildLayout();
    }*/
  }

  fun inMultiWindowMode(): Boolean {
    return context.isInMultiWindowMode
  }

  @CallSuper
  open fun onMultiWindowModeChanged(inMultiWindowMode: Boolean) {
  }

  @CallSuper
  open fun onKeyboardStateChanged(visible: Boolean): Boolean {
    var currentPopup: View? = context.currentPopupWindow
    if (currentPopup != null && currentPopup !is OnStateChangeListener) {
      currentPopup = null
    }

    if (visible) {
      if ((flags and FLAG_KEYBOARD_STATE) != 0) {
        return false
      }
      if (currentPopup != null) {
        (currentPopup as OnStateChangeListener).closeAdditionalKeyboards()
      }
      setFlags(flags or FLAG_KEYBOARD_STATE)
    } else {
      if ((flags and FLAG_KEYBOARD_STATE) == 0) {
        return false
      }
      setFlags(flags and (FLAG_KEYBOARD_STATE.inv()))
    }
    if (currentPopup != null) {
      (currentPopup as OnStateChangeListener).onKeyboardStateChanged(visible)
    }
    if (!visible && lockFocusView != null && lockFocusView is MaterialEditText /* && ((MaterialEditText) lockFocusView).getText().length() == 0*/) {
      val prevFocusable = lockFocusView!!.isFocusable
      val prevFocusableInTouchMode = lockFocusView!!.isFocusableInTouchMode
      lockFocusView!!.setFocusable(false)
      lockFocusView!!.setFocusableInTouchMode(false)
      lockFocusView!!.clearFocus()

      context.requestBlankFocus()

      lockFocusView!!.setFocusable(prevFocusable)
      lockFocusView!!.setFocusableInTouchMode(prevFocusableInTouchMode)
    }
    return true
  }

  fun onPrepareToDismissPopup() {
    hideSoftwareKeyboard()
    // Called from PopupLayout
  }

  protected open fun needPreventiveKeyboardHide(): Boolean {
    return false
  }

  @CallSuper
  open fun onPrepareToShow() {
    if (inSearchMode()) {
      if ((flags and FLAG_PREVENT_LEAVING_SEARCH_MODE) == 0 && allowLeavingSearchMode()) {
        // TODO move this hacky-driven state reset to onCleanAfterHide?
        if (headerView != null && navigationController != null && navigationController!!.stack
            .current === this && headerView!!.getCurrentTransformMode() == HeaderView.TRANSFORM_MODE_SEARCH
        ) {
          // headerView.closeSearchMode(false, null);
          // Do nothing.
          // Reset state only if we are exiting the next screen to this one.
        } else {
          setSearchTransformFactor(0f, false)
          onLeaveSearchMode()
          leaveSearchMode()
        }
      }
    }
  }

  @CallSuper
  open fun onCleanAfterHide() {
  }

  fun interface AttachListener {
    fun onAttachStateChanged(context: ViewController<*>?, navigation: NavigationController?, isAttached: Boolean)
  }

  private var attachListeners: ReferenceList<AttachListener?>? = null

  val attachState: Boolean
    get() = (flags and FLAG_ATTACH_STATE) != 0

  fun addAttachStateListener(listener: AttachListener?) {
    if (attachListeners == null) {
      attachListeners = ReferenceList()
    }
    attachListeners!!.add(listener)
  }

  fun removeAttachStateListener(listener: AttachListener?) {
    if (attachListeners != null) {
      attachListeners!!.remove(listener)
    }
  }

  fun onAttachStateChanged(navigation: NavigationController?, isAttached: Boolean) {
    val nowIsAttached = (this.flags and FLAG_ATTACH_STATE) != 0
    if (nowIsAttached != isAttached) {
      setFlags(setFlag(this.flags, FLAG_ATTACH_STATE, isAttached))
      if (attachListeners != null) {
        for (listener in attachListeners) {
          listener!!.onAttachStateChanged(this, navigation, isAttached)
        }
      }
    }
  }

  protected open fun allowLeavingSearchMode(): Boolean {
    return true
  }

  fun onAfterShow() {}

  open fun onRequestPermissionResult(requestCode: Int, success: Boolean) {}

  private var localeChangers: MutableList<LocaleChanger>? = null

  protected fun bindLocaleChanger(localeChanger: LocaleChanger): Int {
    if (localeChangers == null) {
      localeChangers = ArrayList<LocaleChanger>()
    }
    localeChangers!!.add(localeChanger)
    return localeChanger.getResource()
  }

  @StringRes
  fun bindLocaleChanger(@StringRes resource: Int, item: TextView?, isHint: Boolean, isMedium: Boolean): Int {
    return bindLocaleChanger(LocaleChanger(resource, item, isHint, isMedium))
  }

  /*@Deprecated
  @CallSuper
  public void onLocaleChange () {
    if (localeChangers != null) {
      for (LocaleChanger changer : localeChangers) {
        changer.onLocaleChange();
      }
    }
  }*/
  protected fun updateSettingView(v: SettingView, item: ListItem, isUpdate: Boolean) {
    var value = instance().getNewSetting(item.longId)
    if (item.boolValue) value = !value
    v.toggler.setRadioEnabled(value, isUpdate)
  }

  protected fun handleSettingClick(v: View, adapter: SettingsAdapter) {
    val item = v.tag as ListItem
    var value = adapter.toggleView(v)
    if (item.boolValue) value = !value
    instance().setNewSetting(item.longId, value)
    if (value && item.longId == Settings.SETTING_FLAG_DOWNLOAD_BETAS) {
      context().appUpdater().checkForUpdates()
    }
  }

  protected open fun onFocusStateChanged() {}

  fun interface FocusStateListener {
    fun onFocusStateChanged(c: ViewController<*>, isFocused: Boolean)
  }

  private var focusStateListeners: MutableList<FocusStateListener?>? = null

  fun addOneShotFocusListener(onFocus: Runnable) {
    if (this.isFocused()) {
      onFocus.run()
      return
    }
    addFocusListener(object : FocusStateListener {
      override fun onFocusStateChanged(c: ViewController<*>, isFocused: Boolean) {
        if (isFocused) {
          onFocus.run()
          removeFocusListener(this)
        }
      }
    })
  }

  fun addFocusListener(listener: FocusStateListener?) {
    if (focusStateListeners == null) {
      focusStateListeners = ArrayList<FocusStateListener?>()
    }
    if (!focusStateListeners!!.contains(listener)) {
      focusStateListeners!!.add(listener)
    }
  }

  fun removeFocusListener(listener: FocusStateListener?) {
    if (focusStateListeners != null) {
      focusStateListeners!!.remove(listener)
    }
  }

  private fun notifyFocusChanged(isFocused: Boolean) {
    if (focusStateListeners != null) {
      val size = focusStateListeners!!.size
      for (i in size - 1 downTo 0) {
        focusStateListeners!![i]!!.onFocusStateChanged(this, isFocused)
      }
    }
  }

  @CallSuper
  open fun onFocus() {
    var flags = this.flags
    flags = flags or FLAG_FOCUSED
    flags = flags and FLAG_PREVENT_LEAVING_SEARCH_MODE.inv()
    setFlags(flags)
    if (lockFocusView != null && lockFocusView!!.isEnabled && (flags and FLAG_KEYBOARD_SHOWN) == 0) {
      if ((flags and FLAG_LOCK_ALWAYS) != 0) {
        setFlags(this.flags or FLAG_KEYBOARD_SHOWN)
        Keyboard.show(lockFocusView)
        UI.showKeyboardDelayed(lockFocusView)
      }
    } else {
      getValue()!!.requestFocus()
    }
    trackUserActivity()
    onFocusStateChanged()
    notifyFocusChanged(true)
    context.addKeyEventListener(this)
  }

  protected fun trackUserActivity() {
    Passcode.instance().trackUserActivity(false)
  }

  @CallSuper
  open fun onBlur() {
    var flags = this.flags
    flags = flags and FLAG_FOCUSED.inv()
    if (lockFocusView != null && lockFocusView!!.isEnabled && ((flags and FLAG_IN_SEARCH_MODE) != 0 || (flags and FLAG_KEYBOARD_SHOWN) != 0 || (flags and FLAG_KEYBOARD_STATE) != 0)) {
      flags = flags and FLAG_KEYBOARD_SHOWN.inv()
      if ((flags and FLAG_PREVENT_KEYBOARD_HIDE) != 0) {
        flags = flags and FLAG_PREVENT_KEYBOARD_HIDE.inv()
      } else {
        Keyboard.hide(lockFocusView)
      }
    }
    setFlags(flags)
    onFocusStateChanged()
    notifyFocusChanged(false)
    context.removeKeyEventListener(this)
  }

  @CallSuper
  open fun hideSoftwareKeyboard() {
    if (inSearchMode()) {
      Keyboard.hide(searchHeaderView!!.editView())
    }
    if (lockFocusView != null) {
      Keyboard.hide(lockFocusView)
    }
    // children
  }

  override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
    // override in children
    return false
  }

  override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
    // override in children
    return false
  }

  open fun allowPopupInterruption(): Boolean {
    return false
  }

  open fun closeSearchModeByBackPress(fromTop: Boolean, commit: Boolean): Boolean {
    return false
  }

  @CallSuper
  open fun performOnBackPressed(fromTop: Boolean, commit: Boolean): Boolean {
    return false
  }

  open fun needPassBackPressToActivity(fromTop: Boolean): Boolean {
    return false
  }

  open fun allowLayerTypeChanges(): Boolean {
    return true
  }

  open fun preventRootInteractions(): Boolean {
    return false
  }

  abstract fun getId(): Int

  open fun getName(): CharSequence? = name

  fun setName(string: Int) {
    setName(Lang.getString(string))
  }

  open fun passNameToHeader(): Boolean {
    return false
  }

  fun setName(name: CharSequence?) {
    this.name = name
    if (headerView != null && passNameToHeader()) {
      headerView!!.updateTextTitle(this.getId(), name)
    }
  }

  // int lastPaddingTop = -1;
  /*public void setPaddingTop (int top) {
    */
  /*if (top != lastPaddingTop) {
      lastPaddingTop = top;
      applyOffset(lastPaddingTop);
    }*/
  /*
  }

  public void setClipToPadding (boolean clipToPadding) {
    applyClipOffset(clipToPadding);
  }

  public void applyOffset (int paddingTop) {
    getWrap().setPadding(0, paddingTop, 0, 0);
  }

  public void applyClipOffset (boolean clipOffset) {
    if (getWrap() instanceof ViewGroup) {
      ((ViewGroup) getWrap()).setClipToPadding(clipOffset);
    }
  }*/
  private var destroyListeners: ArrayList<Destroyable>? = null

  fun addDestroyListener(delegate: Destroyable?) {
    if (delegate != null) {
      if (destroyListeners == null) {
        destroyListeners = ArrayList<Destroyable>()
      }
      destroyListeners!!.add(delegate)
    }
  }

  fun removeDestroyListener(delegate: Destroyable?) {
    if (delegate != null && destroyListeners != null) {
      destroyListeners!!.remove(delegate)
    }
  }

  override fun performDestroy() {
    destroy()
  }

  @CallSuper
  open fun destroy() {
    if ((flags and FLAG_DESTROYED) == 0) {
      setFlags(flags or FLAG_DESTROYED)
      if (localeChangers != null) {
        localeChangers!!.clear()
      }
      if (this.wrapUnchecked != null && tdlib != null) {
        tdlib.decrementUiReferenceCount()
      }
      unsubscribeFromControllerUpdates()
      val view = this.getCustomHeaderCell()
      if (view is Destroyable) {
        (view as Destroyable).performDestroy()
      }
      resetScheduledAnimation()
      if (destroyListeners != null) {
        for (destroyable in destroyListeners) {
          destroyable.performDestroy()
        }
      }
    } else {
      Log.bug("Controller is already destroyed: name: %s, class: %s", name, this.javaClass.getName())
    }
  }

  // Chat context
  open val isUnauthorized: Boolean
    /**
     * @return true if current screen should be shown only when there is no authorization
     */
    get() = false

  /**
   * Describes the chatId the current ViewController instance belongs to
   */
  open fun getChatId(): Long {
    return 0
  }

  // Custom camera utils
  class CameraOpenOptions {
    var anchorView: View? = null
    var noTrace: Boolean = false
    var allowSystem: Boolean = true

    @JvmField
    var optionalMicrophone: Boolean = false

    @JvmField
    var mode: Int = 0
    var ignoreAnchor: Boolean = false

    @JvmField
    var readyListener: ReadyListener? = null

    @JvmField
    var qrCodeListener: QrCodeListener? = null

    @JvmField
    @StringRes
    var qrModeSubtitle: Int = 0

    @JvmField
    var qrModeDebug: Boolean = false

    @JvmField
    @AvatarPickerMode
    var avatarPickerMode: Int = 0

    @JvmField
    var delegate: MediaViewDelegate? = null

    @JvmField
    var selectDelegate: MediaSelectDelegate? = null

    @JvmField
    var sendDelegate: MediaSendDelegate? = null

    fun anchor(anchorView: View?): CameraOpenOptions {
      this.anchorView = anchorView
      return this
    }

    fun readyListener(readyListener: ReadyListener?): CameraOpenOptions {
      this.readyListener = readyListener
      return this
    }

    fun qrCodeListener(qrCodeListener: QrCodeListener?): CameraOpenOptions {
      this.qrCodeListener = qrCodeListener
      return this
    }

    fun ignoreAnchor(ignoreAnchor: Boolean): CameraOpenOptions {
      this.ignoreAnchor = ignoreAnchor
      return this
    }

    fun noTrace(noTrace: Boolean): CameraOpenOptions {
      this.noTrace = noTrace
      return this
    }

    fun mode(mode: Int): CameraOpenOptions {
      this.mode = mode
      return this
    }

    fun qrModeSubtitle(@StringRes qrModeSubtitle: Int): CameraOpenOptions {
      this.qrModeSubtitle = qrModeSubtitle
      return this
    }

    fun allowSystem(allowSystem: Boolean): CameraOpenOptions {
      this.allowSystem = allowSystem
      return this
    }

    fun setAvatarPickerMode(@AvatarPickerMode avatarPickerMode: Int): CameraOpenOptions {
      this.avatarPickerMode = avatarPickerMode
      return this
    }

    fun setMediaEditorDelegates(delegate: MediaViewDelegate?, selectDelegate: MediaSelectDelegate?, sendDelegate: MediaSendDelegate?): CameraOpenOptions {
      this.delegate = delegate
      this.selectDelegate = selectDelegate
      this.sendDelegate = sendDelegate
      return this
    }

    fun optionalMicrophone(optionalMicrophone: Boolean): CameraOpenOptions {
      this.optionalMicrophone = optionalMicrophone
      return this
    }

    fun qrModeDebug(qrModeDebug: Boolean): CameraOpenOptions {
      this.qrModeDebug = qrModeDebug
      return this
    }
  }

  protected fun openInAppCamera() {
    openInAppCamera(CameraOpenOptions())
  }

  fun openInAppCamera(options: CameraOpenOptions) {
    if (options.allowSystem && instance().cameraType == Settings.CAMERA_TYPE_SYSTEM) {
      showOptions(
        null,
        intArrayOf(R.id.btn_takePhoto, R.id.btn_takeVideo),
        arrayOf(Lang.getString(R.string.TakePhoto), Lang.getString(R.string.TakeVideo)),
        null,
        intArrayOf(R.drawable.baseline_camera_alt_24, R.drawable.baseline_videocam_24),
        { itemView: View?, id: Int ->
          if (id == R.id.btn_takePhoto) {
            Intents.openCamera(context, options.noTrace, false)
          } else if (id == R.id.btn_takeVideo) {
            Intents.openCamera(context, options.noTrace, true)
          }
          true
        })
    } else {
      openCustomCamera(options.ignoreAnchor(this.getKeyboardState()))
    }
  }

  private fun openCustomCamera(options: CameraOpenOptions): Boolean {
    return context.openCameraByTap(options)
  }

  fun isInForceTouchMode(): Boolean {
    return (flags and FLAG_IN_FORCE_TOUCH_MODE) != 0
  }

  // Force touch
  fun setInForceTouchMode(inForceTouchMode: Boolean) {
    if (isInForceTouchMode() != inForceTouchMode) {
      setFlags(setFlag(this.flags, FLAG_IN_FORCE_TOUCH_MODE, inForceTouchMode))
      onForceTouchModeChanged(inForceTouchMode)
    }
  }

  fun wouldMaximizeFromPreview(): Boolean {
    return (flags and FLAG_MAXIMIZING) != 0
  }

  fun maximizeFromPreviewIfNeeded(y: Float, startY: Float) {
    if ((flags and FLAG_MAXIMIZING) != 0 || boundForceTouchView == null || boundForceTouchView!!.isAnimatingReveal) {
      return
    }
    val startDistanceToList = boundForceTouchView!!.getDistanceToButtonsList(startY)
    val d = (y - startY + startDistanceToList)
    val maximizeFactor =  /*hasInteractedWithContent() ||*/if (d >= 0) 0f else clamp(-d / Screen.dp(64f).toFloat())
    if (maximizeFactor == 1f) {
      maximizeFromPreview()
    } else if (boundForceTouchView != null) {
      boundForceTouchView!!.setBeforeMaximizeFactor(maximizeFactor)
    }
  }

  fun maximizeFromPreview() {
    if (this.isInForceTouchMode() && (flags and FLAG_MAXIMIZING) == 0) {
      setFlags(flags or FLAG_MAXIMIZING)
      UI.forceVibrate(getValue(), false)
      context.closeForceTouch()
    }
  }

  open fun wouldHideKeyboardInForceTouchMode(): Boolean {
    return true
  }

  protected fun onForceTouchModeChanged(inForceTouchMode: Boolean) {
    // Override
  }

  protected open fun onTranslationChanged(newTranslationX: Float) {
    // Override
  }

  protected open fun onCreatePopupLayout(popupLayout: PopupLayout?) {
    // Override
  }

  override fun onPrepareToExitForceTouch(context: ForceTouchContext?) {
    onBlur()
  }

  override fun onPrepareToEnterForceTouch(context: ForceTouchContext?) {
    onPrepareToShow()
  }

  override fun onCompletelyShownForceTouch(context: ForceTouchContext?) {
    onFocus()
  }

  override fun onDestroyForceTouch(context: ForceTouchContext?) {
    onCleanAfterHide()
    destroy()
  }

  private var boundForceTouchView: ForceTouchView? = null

  fun forceTouchView(): ForceTouchView? {
    return boundForceTouchView
  }

  fun setBoundForceTouchView(forceTouchView: ForceTouchView) {
    this.boundForceTouchView = forceTouchView
  }

  // Disabling screenshot
  private var disallowScreenshotReasons: MutableList<FutureBool>? = null

  fun addDisallowScreenshotReason(reason: FutureBool?) {
    if (disallowScreenshotReasons == null) {
      disallowScreenshotReasons = ArrayList<FutureBool>()
    }
    disallowScreenshotReasons!!.add(reason!!)
  }

  open fun shouldDisallowScreenshots(): Boolean {
    if (disallowScreenshotReasons != null) {
      for (reason in disallowScreenshotReasons) {
        if (reason.getBoolValue()) {
          return true
        }
      }
    }
    return false
  }

  // Drag-n-Drop
  private var thumbnailContentView: View? = null

  open fun onCreateThumbnailView(context: Context?): View? {
    throw RuntimeException("Stub!")
  }

  val thumbnailWrap: View?
    get() {
      if (thumbnailContentView == null) {
        thumbnailContentView = onCreateThumbnailView(context())
      }
      return thumbnailContentView
    }

  private var forceFadeModeOnce = false

  init {
    requireNotNull(this.context)
  }

  fun forceFastAnimationOnce() {
    forceFadeModeOnce = true
  }


  interface SearchEditTextDelegate {
    fun view(): View
    fun editView(): HeaderEditText
  }

  companion object {
    // hint: stop at 0x40000000
    private val FLAG_PAUSED = 1 shl 12
    private val FLAG_FOCUSED = 1 shl 13
    private val FLAG_DESTROYED = 1 shl 14
    private val FLAG_LOCK_ALWAYS = 1 shl 15
    private val FLAG_KEYBOARD_SHOWN = 1 shl 16
    private val FLAG_KEYBOARD_STATE = 1 shl 17
    private val FLAG_SWIPE_DISABLED = 1 shl 18
    private val FLAG_IN_SELECT_MODE = 1 shl 19
    private val FLAG_IN_SEARCH_MODE = 1 shl 20
    private val FLAG_IN_CUSTOM_MODE = 1 shl 21
    private val FLAG_ATTACHED_TO_NAVIGATION = 1 shl 22
    private val FLAG_PREVENT_LEAVING_SEARCH_MODE = 1 shl 23
    private val FLAG_SHARE_CUSTOM_HEADER = 1 shl 24
    private val FLAG_IN_FORCE_TOUCH_MODE = 1 shl 25
    private val FLAG_ATTACH_STATE = 1 shl 26
    private val FLAG_CONTENT_INTERACTED = 1 shl 27
    private val FLAG_MAXIMIZING = 1 shl 28
    private val FLAG_PREVENT_KEYBOARD_HIDE = 1 shl 29

    const val ALERT_NO_CANCEL: Int = 1

    @JvmField
    val ALERT_NO_CANCELABLE: Int = 1 shl 1

    @JvmField
    val ALERT_HAS_LINKS: Int = 1 shl 2

    @JvmStatic
    fun findRoot(view: View?): ViewController<*>? {
      var view = view
      var result: ViewController<*>? = null
      while (view != null) {
        val tag = view.tag
        if (tag is ViewController<*>) {
          result = tag
        }
        val parent = view.parent
        if (parent is View) {
          view = parent as View
        } else {
          break
        }
      }
      return result
    }

    @JvmStatic
    fun findAncestor(view: View?): ViewController<*>? {
      var view = view
      while (view != null) {
        val tag = view.tag
        if (tag is ViewController<*>) {
          return tag
        }
        val parent = view.parent
        if (parent is View) {
          view = parent as View
        } else {
          break
        }
      }
      return null
    }
  }
}
