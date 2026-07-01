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
 * File created on 19/10/2016
 */
package org.thunderdog.challegram.component.attach

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.Rect
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.CallSuper
import androidx.annotation.StringRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.vkryl.android.DECELERATE_INTERPOLATOR
import me.vkryl.android.getFraction
import me.vkryl.android.simpleValueAnimator
import me.vkryl.android.widget.FrameLayoutFix.Companion.newParams
import org.drinkless.tdlib.TdApi
import org.drinkless.tdlib.TdApi.ChatAction
import org.drinkless.tdlib.TdApi.MessageSendOptions
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.navigation.BackHeaderButton
import org.thunderdog.challegram.navigation.HeaderView
import org.thunderdog.challegram.navigation.ViewController
import org.thunderdog.challegram.support.ViewSupport
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.tool.Views
import org.thunderdog.challegram.unsorted.Settings.Companion.instance
import org.thunderdog.challegram.util.HapticMenuHelper
import org.thunderdog.challegram.widget.EmptyTextView
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

abstract class MediaBottomBaseController<T> : ViewController<T> {
  @JvmField
  protected val mediaLayout: MediaLayout
  private val titleRes: Int
  private var titleString: String?

  protected constructor(context: MediaLayout, titleResource: Int) : super(context.context, context.tdlib()) {
    this.titleRes = titleResource
    this.titleString = ""
    this.mediaLayout = context
    initMetrics()
  }

  protected constructor(context: MediaLayout, titleString: String?) : super(context.context, context.tdlib()) {
    this.titleRes = 0
    this.titleString = titleString
    this.mediaLayout = context
    initMetrics()
  }

  @get:ChatAction.Constructors
  open val broadcastingAction: Int
    get() = TdApi.ChatActionCancel.CONSTRUCTOR

  override fun getName(): CharSequence? {
    return if (titleRes != 0) Lang.getString(titleRes) else titleString
  }

  fun setName(name: String?) {
    titleString = name
    mediaLayout.getHeaderView().setTitle(this)
  }

  // Settings
  open fun allowSpoiler(): Boolean {
    return false
  }

  override fun getHeaderTextColorId(): Int {
    return ColorId.text
  }

  override fun getHeaderColorId(): Int {
    return ColorId.filling
  }

  override fun getBackButton(): Int {
    return BackHeaderButton.TYPE_BACK
  }

  override fun getHeaderIconColorId(): Int {
    return ColorId.headerLightIcon
  }

  override fun useGraySearchHeader(): Boolean {
    return true
  }

  override fun performOnBackPressed(fromTop: Boolean, commit: Boolean): Boolean {
    if (inSearchMode()) {
      if (commit) {
        mediaLayout.getHeaderView().closeSearchMode(true, null)
      }
      return true
    }
    return super.performOnBackPressed(fromTop, commit)
  }

  open fun canMoveRecycler(): Boolean {
    return !inTransformMode() // aka isAnimating
  }

  open fun supportsMediaGrouping(): Boolean {
    return false
  }

  open fun ignoreStartHeightLimits(): Boolean {
    return false
  }

  // Metrics
  var contentHeight: Int = 0
    private set
  var startHeight: Int = 0
    private set
  var currentHeight: Int = 0
    private set

  private var lastHeightIncreaseCheck = 0
  private var isHeightIncreasing = false // true if it increased

  protected fun initMetrics() { // FIXME make private again and move calling from constructor to some proper place
    contentHeight = this.initialContentHeight
    resetStartHeights(true)
  }

  private val barHeightIfAvailable: Int
    get() = if (mediaLayout.inSpecificMode() && mediaLayout.mode != MediaLayout.MODE_CUSTOM_ADAPTER) 0 else MediaBottomBar.getBarHeight() + (if (instance()
        .useEdgeToEdge()
    ) context.getRootView().systemInsetsWithoutIme.bottom else 0)

  private fun resetStartHeights(initial: Boolean) {
    startHeight = min(
      contentHeight + this.barHeightIfAvailable + HeaderView.getSize(false), min(
      this.maxStartHeight,
      this.maxHeight
    )
    )
    setCurrentHeight(if (this.recyclerScrollY > 0 || lastIsExpanded) this.maxHeight else startHeight, !initial)
  }

  val currentWidth: Int
    get() = mediaLayout.currentContentWidth

  protected open val initialContentHeight: Int
    // Metrics (internal)
    get() = this.maxInitialContentHeight

  protected val maxInitialContentHeight: Int
    get() = Screen.smallestSide() - HeaderView.getSize(false)

  private val maxStartHeight: Int
    get() {
      if (ignoreStartHeightLimits()) return Int.MAX_VALUE
      return min(
        this.contentHeight + this.barHeightIfAvailable + HeaderView.getSize(false),
        min(this.currentWidth + this.barHeightIfAvailable, this.maxStartHeightLimit)
      )
    }

  protected val maxStartHeightLimit: Int
    get() {
      if (ignoreStartHeightLimits()) return Int.MAX_VALUE
      return this.maxHeight - MediaBottomBar.getBarHeight() / 4
    }

  protected open val recyclerHeaderOffset: Int
    get() = 0

  protected open fun canExpandHeight(): Boolean {
    return true
  }

  protected open fun canMinimizeHeight(): Boolean {
    return true
  }

  open fun showExitWarning(isExitingSelection: Boolean, commit: Boolean): Boolean {
    // override
    return false
  }

  private fun findYForStaticView(viewHeight: Int): Int {
    return recyclerView!!.translationY.toInt() + this.recyclerHeaderOffset + (max(
      startHeight,
      this.currentHeight
    ) - this.recyclerHeaderOffset) / 2 - this.barHeightIfAvailable - viewHeight / 2
  }

  // Recycler position
  open fun onViewportChanged(width: Int, height: Int) {
    if (recyclerView != null) {
      resetStartHeights(false)
      updateRecyclerTop(height)
    }
  }

  private fun updateRecyclerTop() {
    if (contentView != null) {
      val currentHeight = contentView!!.measuredHeight
      updateRecyclerTop(if (currentHeight == 0) this.targetHeight else currentHeight)
    }
  }

  private val targetHeight: Int
    get() = context.getRootView().measuredHeight - HeaderView.getTopOffset()

  private fun updateRecyclerTop(height: Int) {
    if (recyclerView != null) {
      val top = (height - currentHeight).toFloat()
      recyclerView!!.translationY = top
      onRecyclerTopUpdate(top)
    }
  }

  // General pattern
  @JvmField
  protected var contentView: MediaContentView? = null
  private var progressView: View? = null

  @JvmField
  protected var recyclerView: MediaBottomBaseRecyclerView? = null
  private var emptyView: EmptyTextView? = null

  override fun supportsBottomInset(): Boolean {
    return true
  }

  override fun onBottomInsetChanged(extraBottomInset: Int, extraBottomInsetWithoutIme: Int, isImeInset: Boolean) {
    super.onBottomInsetChanged(extraBottomInset, extraBottomInsetWithoutIme, isImeInset)
    Views.applyBottomInset(recyclerView, extraBottomInsetWithoutIme)
  }

  protected fun buildContentView(needProgress: Boolean): MediaContentView? {
    contentView = MediaContentView(context())
    contentView!!.setLayoutParams(newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    contentView!!.setBoundController(this)

    var params: FrameLayout.LayoutParams?
    params = newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    params.topMargin = HeaderView.getSize(false)
    params.bottomMargin = HeaderView.getTopOffset()

    recyclerView = MediaBottomBaseRecyclerView(context())
    Views.applyBottomInset(recyclerView, extraBottomInsetWithoutIme)
    recyclerView!!.addItemDecoration(object : RecyclerView.ItemDecoration() {
      override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        if (position != RecyclerView.NO_POSITION && recyclerView!!.adapter != null && position == recyclerView!!.adapter!!
            .itemCount - 1 && mediaLayout.counterFactor == 1f
        ) {
          outRect.set(0, 0, 0, MediaBottomBar.getBarHeight())
        } else {
          outRect.setEmpty()
        }
      }
    })
    recyclerView!!.setOverScrollMode(View.OVER_SCROLL_NEVER)
    ViewSupport.setThemedBackground(recyclerView, this.recyclerBackgroundColorId)
    addThemeInvalidateListener(recyclerView)
    recyclerView!!.setItemAnimator(CustomItemAnimator(DECELERATE_INTERPOLATOR, 150L))
    recyclerView!!.setLayoutParams(params)
    recyclerView!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
      override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        if (newState != RecyclerView.SCROLL_STATE_IDLE) {
          hideSoftwareKeyboard()
        }
      }
    })
    contentView!!.addView(recyclerView)

    if (needProgress) {
      params = newParams(Screen.dp(48f), Screen.dp(48f), Gravity.TOP or Gravity.CENTER_HORIZONTAL)
      params.topMargin = HeaderView.getSize(false)
      progressView = Views.simpleProgressView(context(), params)
      progressView!!.translationY = findYForStaticView(Screen.dp(48f)).toFloat()
      contentView!!.addView(progressView)
    }

    updateRecyclerTop()

    return contentView
  }

  open fun onRecyclerFirstMovement() {
  }

  @get:ColorId
  protected open val recyclerBackgroundColorId: Int
    get() = ColorId.filling

  fun dispatchRecyclerTouchEvent(e: MotionEvent?) {
    recyclerView!!.processEvent(e)
  }

  val recyclerScrollY: Int
    get() {
      if (recyclerView == null) {
        return 0
      }
      val manager = this.layoutManager
      if (manager !is LinearLayoutManager) {
        return 0
      }
      val adapter = recyclerView!!.adapter
      if (adapter !is MeasuredAdapterDelegate) {
        return 0
      }
      val firstPosition = manager.findFirstVisibleItemPosition()
      if (firstPosition == RecyclerView.NO_POSITION) {
        return 0
      }
      var scrollTop = (adapter as MeasuredAdapterDelegate).measureScrollTop(firstPosition)
      val view = manager.findViewByPosition(firstPosition)
      if (view != null) {
        scrollTop -= view.top
      }
      return scrollTop
    }

  protected var layoutManager: RecyclerView.LayoutManager?
    get() = recyclerView!!.layoutManager
    set(manager) {
      recyclerView!!.setLayoutManager(manager)
    }

  protected fun addItemDecoration(decoration: RecyclerView.ItemDecoration) {
    recyclerView!!.addItemDecoration(decoration)
  }

  protected fun setAdapter(adapter: RecyclerView.Adapter<*>?) {
    recyclerView!!.setAdapter(adapter)
  }

  protected fun dispatchError(error: String?, resolveErrorButtonText: String?, onResolveButtonClick: View.OnClickListener?, animated: Boolean) {
    runOnUiThread { showError(error, resolveErrorButtonText, onResolveButtonClick, animated) }
  }

  fun updateExtraSpacingDecoration() {
    recyclerView!!.invalidateItemDecorations()
  }

  protected fun showError(@StringRes errorRes: Int, @StringRes resolveErrorButtonRes: Int, onResolveButtonClick: View.OnClickListener?, animated: Boolean) {
    showError(Lang.getString(errorRes), if (resolveErrorButtonRes != 0) Lang.getString(resolveErrorButtonRes) else null, onResolveButtonClick, animated)
  }

  protected fun hideError() {
    if (emptyView != null) {
      emptyView!!.setAlpha(0f)
    }
  }

  // Called when removed from view tree
  fun resetState() {
    // force scroll to top
  }

  protected fun showError(error: String?, resolveButtonText: String?, onResolveButtonClick: View.OnClickListener?, animated: Boolean) {
    var animated = animated
    if (emptyView == null) {
      val params = newParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.CENTER_HORIZONTAL)
      params.topMargin = HeaderView.getSize(false)

      emptyView = EmptyTextView(context())
      emptyView!!.setLayoutParams(params)
      emptyView!!.translationY = findYForStaticView(Screen.dp(18f)).toFloat()
      contentView!!.addView(emptyView)
    } else {
      animated = false
    }
    emptyView!!.text = error
    if (!animated || progressView == null) {
      emptyView!!.setAlpha(1f)
      return
    }
    emptyView!!.setAlpha(0f)
    val animator = simpleValueAnimator()
    animator.addUpdateListener { animation: ValueAnimator? ->
      var factor = getFraction(animation!!)
      if (factor <= .5f) {
        factor = DECELERATE_INTERPOLATOR.getInterpolation(factor / .5f)
        progressView!!.setAlpha(1f - factor)
      } else {
        if (progressView!!.alpha != 0f) {
          progressView!!.setAlpha(0f)
        }
        factor = DECELERATE_INTERPOLATOR.getInterpolation((factor - .5f) / .5f)
        emptyView!!.setAlpha(factor)
      }
    }
    animator.addListener(object : AnimatorListenerAdapter() {
      override fun onAnimationEnd(animation: Animator) {
        contentView!!.removeView(progressView)
        progressView = null
      }
    })
    animator.setDuration(300L)
    animator.start()
  }

  @JvmOverloads
  protected fun hideProgress(after: Runnable? = null) {
    if (progressView == null) {
      return
    }
    val animator = simpleValueAnimator()
    animator.addUpdateListener { animation: ValueAnimator? -> progressView!!.setAlpha(1f - getFraction(animation!!)) }
    animator.addListener(object : AnimatorListenerAdapter() {
      override fun onAnimationEnd(animation: Animator) {
        contentView!!.removeView(progressView)
        progressView = null
        after?.run()
      }
    })
    animator.interpolator = DECELERATE_INTERPOLATOR
    animator.setDuration(HIDE_PROGRESS_DURATION)
    animator.start()
  }

  private fun applyStartHeight(newStartHeight: Int) {
    if (this.startHeight != newStartHeight) {
      this.currentHeight = newStartHeight
      this.startHeight = this.currentHeight
      updateRecyclerTop()
      mediaLayout.onContentHeightChanged()
    }
  }

  protected fun expandStartHeight(adapter: MeasuredAdapterDelegate) {
    this.contentHeight = adapter.measureHeight(-1)
    val newStartHeight = this.maxStartHeight

    if (newStartHeight <= startHeight) {
      return
    }

    if (isMoving || animatingHeight || currentHeight > startHeight) {
      startHeight = newStartHeight
      return
    }

    val fromHeight = startHeight.toFloat()
    val heightDiff = (newStartHeight - startHeight).toFloat()

    val animator = simpleValueAnimator()
    animator.addUpdateListener { animation: ValueAnimator? ->
      applyStartHeight(
        (fromHeight + heightDiff * getFraction(
          animation!!
        )).roundToInt()
      )
    }
    animator.interpolator = DECELERATE_INTERPOLATOR
    animator.setDuration(150L)
    animator.start()
  }

  private var lastIsExpanded = false

  private fun setCurrentHeight(height: Int, updateMediaLayout: Boolean) {
    if (this.currentHeight != height) {
      this.currentHeight = height

      val maxHeight = this.maxHeight

      lastIsExpanded = height == maxHeight

      if (abs(height - lastHeightIncreaseCheck) >= Screen.getTouchSlop()) {
        isHeightIncreasing = height > lastHeightIncreaseCheck
        lastHeightIncreaseCheck = height
      }

      updateRecyclerTop()
      mediaLayout.setContentVisible(currentHeight < maxHeight)

      if (updateMediaLayout) {
        if (currentHeight == startHeight) {
          mediaLayout.setBottomBarFactor(1f)
          mediaLayout.setHeaderFactor(0f)
        } else if (currentHeight < startHeight) {
          mediaLayout.setBottomBarFactor(currentHeight.toFloat() / startHeight.toFloat())
          mediaLayout.setHeaderFactor(0f)
        } else {
          val barFactor = (this.maxHeight - currentHeight).toFloat() / (maxHeight - startHeight).toFloat()
          mediaLayout.setBottomBarFactor(barFactor)
          mediaLayout.setHeaderFactor(1f - barFactor)
        }
      }
    }
  }

  protected open fun onUpdateBottomBarFactor(bottomBarFactor: Float, counterFactor: Float, y: Float) {
  }

  private var animatingHeight = false
  private var heightAnimator: ValueAnimator? = null

  protected fun animateCurrentHeight(toHeight: Int, fast: Boolean) {
    if (animatingHeight) {
      animatingHeight = false
      if (heightAnimator != null) {
        heightAnimator!!.cancel()
        heightAnimator = null
      }
    }

    if (currentHeight == toHeight) {
      return
    }

    animatingHeight = true

    val fromHeight = this.currentHeight.toFloat()
    val heightDiff = toHeight - fromHeight

    heightAnimator = simpleValueAnimator()
    heightAnimator!!.addUpdateListener { animation: ValueAnimator? ->
      if (animatingHeight) {
        val factor = getFraction(animation!!)
        setCurrentHeight((fromHeight + heightDiff * factor).roundToInt(), true)
      }
    }
    heightAnimator!!.addListener(object : AnimatorListenerAdapter() {
      override fun onAnimationEnd(animation: Animator) {
        animatingHeight = false
      }
    })
    heightAnimator!!.interpolator = DECELERATE_INTERPOLATOR
    heightAnimator!!.setDuration(if (fast) 150L else MediaLayout.REVEAL_DURATION)
    heightAnimator!!.start()
  }

  val isAnimating: Boolean
    // Height expanding
    get() = animatingHeight || useBottomBarChange

  fun isInsideRecyclerView(x: Float, y: Float): Boolean {
    return y >= recyclerView!!.translationY && y <= recyclerView!!.translationY + recyclerView!!.measuredHeight
  }

  val maxHeight: Int
    get() = this.targetHeight //  - HeaderView.getSize();

  private var moveStartHeight = 0
  private var isMoving = false

  private var useBottomBarChange = false
  private var barChangeFromHeight = 0
  private var barChangeHeightDiff = 0

  fun onBottomBarAnimationStart(beforeExit: Boolean) {
    if (beforeExit) {
      useBottomBarChange = true
      barChangeFromHeight = currentHeight
      barChangeHeightDiff = -currentHeight
    }
  }

  fun onBottomBarAnimationFraction(factor: Float) {
    if (useBottomBarChange) {
      setCurrentHeight(barChangeFromHeight + (barChangeHeightDiff.toFloat() * factor).toInt(), false)
    }
  }

  fun onBottomBarAnimationComplete() {
    useBottomBarChange = false
  }

  fun onRecyclerMovementStarted() {
    moveStartHeight = currentHeight
    isMoving = true
  }

  fun handleFling(toUp: Boolean): Boolean {
    if (isMoving) {
      isMoving = false
      useBottomBarChange = false

      val expandFactor =
        if (currentHeight < startHeight) 0f else 1f - (this.maxHeight - currentHeight).toFloat() / (this.maxHeight - startHeight).toFloat()

      mediaLayout.prepareHeader()
      if (toUp) {
        if (canExpandHeight()) {
          animateCurrentHeight(this.maxHeight, true)
        } else {
          animateCurrentHeight(startHeight, true)
        }
      } else if (expandFactor >= .2f || !canMinimizeHeight()) {
        animateCurrentHeight(startHeight, false)
      } else {
        useBottomBarChange = true
        barChangeFromHeight = currentHeight
        barChangeHeightDiff = -currentHeight
        mediaLayout.hideBottomBarAndDismiss()
      }

      return true
    }
    return false
  }

  fun onRecyclerMovementFinished() {
    if (isMoving) {
      isMoving = false
      useBottomBarChange = false

      val openFactor = if (currentHeight >= startHeight) 1f else currentHeight.toFloat() / startHeight.toFloat()
      val expandFactor =
        if (currentHeight < startHeight) 0f else 1f - (this.maxHeight - currentHeight).toFloat() / (this.maxHeight - startHeight).toFloat()

      mediaLayout.prepareHeader()

      if (openFactor <= .45f && !isHeightIncreasing) {
        useBottomBarChange = true
        barChangeFromHeight = currentHeight
        barChangeHeightDiff = -currentHeight
        mediaLayout.hideBottomBarAndDismiss()
      } else if (expandFactor >= .35f && isHeightIncreasing) {
        animateCurrentHeight(this.maxHeight, true)
      } else {
        animateCurrentHeight(startHeight, false)
      }
    }
  }

  private var recyclerEverMoved = false

  fun moveRecyclerView(diffY: Float): Boolean {
    val maxHeight = if (canExpandHeight()) this.maxHeight else startHeight
    val newHeight = min(maxHeight, moveStartHeight - diffY.toInt())

    if (newHeight < startHeight && !canMinimizeHeight()) {
      return false
    }

    if (currentHeight == newHeight) {
      return newHeight == maxHeight
    }

    if (!recyclerEverMoved && newHeight > startHeight) {
      recyclerEverMoved = true
      onRecyclerFirstMovement()
    }

    if (currentHeight > startHeight) {
      mediaLayout.prepareHeader()
    }
    setCurrentHeight(newHeight, true)

    return newHeight == maxHeight
  }

  // Override
  protected open fun preload(after: Runnable?, timeout: Long) {
    // do heavy load
    after?.run()
  }

  @CallSuper
  protected open fun onRecyclerTopUpdate(top: Float) {
    if (progressView != null) {
      progressView!!.translationY = findYForStaticView(progressView!!.layoutParams.height).toFloat()
    }
    if (emptyView != null) {
      emptyView!!.translationY = findYForStaticView(Screen.dp(18f)).toFloat()
    }
    // Use to align overlay views
  }

  fun expandFully() {
    mediaLayout.prepareHeader()
    animateCurrentHeight(this.maxHeight, false)
  }

  val isExpanded: Boolean
    get() = currentHeight == this.maxHeight

  fun collapseToStart() {
    val scrollY = this.recyclerScrollY
    if (scrollY != 0) {
      recyclerView!!.smoothScrollBy(0, -scrollY)
    }
    animateCurrentHeight(startHeight, false)
  }

  fun forceScrollRecyclerToTop() {
    val manager = this.layoutManager
    if (manager != null && manager is LinearLayoutManager) {
      manager.scrollToPositionWithOffset(0, 0)
    }
  }

  protected open fun onCompleteShow(isPopup: Boolean) {
    // Do all heavy work like layout or etc., no animation will lag
  }

  protected open fun onMultiSendPress(view: View?, options: MessageSendOptions, disableMarkdown: Boolean) {
    // Send all selected shit
  }

  protected open fun addCustomItems(view: View?, hapticItems: MutableList<HapticMenuHelper.MenuItem?>) {
    // Add specific items
  }

  fun canRemoveMarkdown(): Boolean {
    return false
  }

  protected open fun onCancelMultiSelection() {
    // unselect all selected shit
  }

  protected open fun createCustomBottomBar(): ViewGroup? {
    return null
  }

  override fun destroy() {
    super.destroy()
    if (recyclerView != null) {
      Views.destroyRecyclerView(recyclerView)
    }
  }

  companion object {
    protected const val HIDE_PROGRESS_DURATION: Long = 140L
  }
}