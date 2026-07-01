package org.thunderdog.challegram.ui

import android.animation.Animator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import android.view.View.OnLayoutChangeListener
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.vkryl.android.widget.FrameLayoutFix
import me.vkryl.android.widget.FrameLayoutFix.Companion.newParams
import org.thunderdog.challegram.navigation.HeaderView
import org.thunderdog.challegram.navigation.TooltipOverlayView
import org.thunderdog.challegram.navigation.ViewController
import org.thunderdog.challegram.navigation.ViewPagerController
import org.thunderdog.challegram.support.ViewSupport
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Paints
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.tool.UI
import org.thunderdog.challegram.tool.Views
import org.thunderdog.challegram.unsorted.Settings.Companion.instance
import org.thunderdog.challegram.widget.LickView
import org.thunderdog.challegram.widget.PopupLayout
import org.thunderdog.challegram.widget.PopupLayout.PopupHeightProvider
import org.thunderdog.challegram.widget.PopupLayout.TouchSectionProvider
import kotlin.math.max

abstract class BottomSheetViewController<T>(context: Context, tdlib: Tdlib?) : ViewPagerController<T>(context, tdlib), TouchSectionProvider,
  PopupHeightProvider {
  @JvmField
  protected var wrapView: FrameLayoutFix? = null

  @JvmField
  protected var contentView: RelativeLayout? = null

  @JvmField
  protected var fixView: View? = null
  protected var pagerInFrameLayoutFix: View? = null

  abstract override fun getHeaderHeight(): Int

  protected abstract val contentOffset: Int
  protected abstract fun onCreateHeaderView(): HeaderView?
  protected open fun onBeforeCreateView() {}
  protected open fun onAfterCreateView() {}

  protected open val hideByScrollBorder: Int
    get() = Screen.dp(150f)

  protected fun getHeaderHeight(withOffset: Boolean): Int {
    return getHeaderHeight() + (if (withOffset) HeaderView.getTopOffset() else 0)
  }

  protected val contentMinHeight: Int
    get() = (this.targetHeight - getHeaderHeight(true) - this.contentOffset)

  protected val contentVisibleHeight: Int
    get() = this.targetHeight - (this.topEdge + getHeaderHeight(true))


  override fun onCreateView(context: Context): View? {
    onBeforeCreateView()

    headerView = onCreateHeaderView()

    contentView = object : RelativeLayout(context) {
      override fun onDraw(canvas: Canvas) {
        canvas.drawRect(
          0f, headerTranslationY, measuredWidth.toFloat(), measuredHeight.toFloat(), Paints.fillingPaint(
          Theme.getColor(this@BottomSheetViewController.backgroundColorId)
        )
        )
        super.onDraw(canvas)
      }

      override fun drawChild(canvas: Canvas, child: View?, drawingTime: Long): Boolean {
        if (child === pagerInFrameLayoutFix) {
          canvas.save()
          canvas.clipRect(
            0f,
            headerTranslationY + (if (headerView != null) HeaderView.getTopOffset() else 0),
            measuredWidth.toFloat(),
            measuredHeight.toFloat()
          )
          val result = super.drawChild(canvas, child, drawingTime)
          canvas.restore()
          return result
        } else {
          return super.drawChild(canvas, child, drawingTime)
        }
      }
    }
    contentView!!.setWillNotDraw(false)
    contentView!!.setLayoutParams(newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    addThemeInvalidateListener(contentView)

    val fp = newParams(ViewGroup.LayoutParams.MATCH_PARENT, Screen.dp(6f))
    fp.topMargin = getHeaderHeight()
    fixView = View(context)
    ViewSupport.setThemedBackground(fixView, ColorId.background, this)
    fixView!!.setLayoutParams(fp)

    wrapView = object : FrameLayoutFix(context) {
      override fun onInterceptTouchEvent(e: MotionEvent): Boolean {
        val b = (e.action == MotionEvent.ACTION_DOWN && e.y < (this@BottomSheetViewController.topEdge + HeaderView.getTopOffset()))
        return b || super.onInterceptTouchEvent(e)
      }

      override fun onTouchEvent(e: MotionEvent): Boolean {
        val b = (e.action == MotionEvent.ACTION_DOWN && e.y < (this@BottomSheetViewController.topEdge + HeaderView.getTopOffset()))
        return b && super.onTouchEvent(e)
      }

      private var oldHeight = -1

      override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        post {
          val height: Int = this@BottomSheetViewController.targetHeight
          if (height != oldHeight) {
            invalidateAllItemDecorations()
            val disallowKeyboardHide = isDisallowKeyboardHideOnPageScrolled
            isDisallowKeyboardHideOnPageScrolled = true
            onPageScrolled(currentMediaPosition, currentPositionOffset, 0)
            isDisallowKeyboardHideOnPageScrolled = disallowKeyboardHide
            oldHeight = height
          }
        }
      } /*
      @Override
      protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        int headerHeight = getHeaderHeight(true);
        int DP = Screen.dp(1);

        canvas.drawRect(0, 0, getMeasuredWidth(), getTargetHeight(), Paints.strokeBigPaint(Color.RED));

        canvas.drawRect(DP, headerHeight, getMeasuredWidth() - DP, headerHeight + getContentOffset(), Paints.strokeBigPaint(Color.GREEN));
        canvas.drawRect(DP, headerHeight + getContentOffset(), getMeasuredWidth() - DP, headerHeight + getContentOffset() + getContentMinHeight(), Paints.strokeBigPaint(Color.GREEN));

        canvas.drawRect(DP * 2, getMeasuredHeight() - getHideByScrollBorder(), getMeasuredWidth() - DP * 2, getMeasuredHeight(), Paints.strokeBigPaint(Color.BLUE));
      }
      */
    }

    val params = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    params.topMargin = getHeaderHeight() + HeaderView.getTopOffset()
    pagerInFrameLayoutFix = super.onCreateView(context)
    pagerInFrameLayoutFix!!.setLayoutParams(params)
    contentView!!.addView(pagerInFrameLayoutFix)

    wrapView!!.addView(fixView)
    wrapView!!.addView(contentView)
    if (headerView != null) {
      wrapView!!.addView(headerView)
    }
    wrapView!!.setWillNotDraw(false)
    addThemeInvalidateListener(wrapView)
    if (HeaderView.getTopOffset() > 0) {
      lickView = LickView(context)
      addThemeInvalidateListener(lickView)
      lickView!!.setLayoutParams(newParams(ViewGroup.LayoutParams.MATCH_PARENT, HeaderView.getTopOffset()))
      wrapView!!.addView(lickView)
    }

    onAfterCreateView()

    return wrapView
  }

  protected fun findCurrentCachedController(): ViewController<*>? {
    return findCachedControllerByPosition(viewPager!!.currentItem)
  }

  protected fun findCachedControllerByPosition(position: Int): ViewController<*>? {
    if (viewPager!!.adapter is ViewPagerAdapter) {
      val adapter = (viewPager!!.adapter) as ViewPagerAdapter?
      return adapter!!.getCachedItemByPosition(position)
    }
    return null
  }


  protected open val backgroundColorId: Int
    get() = ColorId.background

  @JvmField
  protected var ignoreAnyPagerScrollEventsBecauseOfMovements: Boolean = false

  protected open fun canHideByScroll(): Boolean {
    return false
  }

  @JvmField
  protected var currentMediaPosition: Int = 0

  @JvmField
  protected var currentPositionOffset: Float = 0f

  @JvmField
  protected var checkedPosition: Int = -1

  @JvmField
  protected var checkedBasePosition: Int = -1

  protected fun invalidateCachedPosition() {
    checkedPosition = -1
    checkedBasePosition = -1
  }

  @JvmField
  protected var lastHeaderPosition: Float = 0f

  protected fun checkHeaderPosition(recyclerView: RecyclerView?) {
    lastHeaderPosition = (max(Views.getRecyclerFirstElementTop(recyclerView), 0) + HeaderView.getTopOffset()).toFloat()
    setHeaderPosition(lastHeaderPosition)
    /*
View view = null;
if (recyclerView != null) {
  view = recyclerView.getLayoutManager().findViewByPosition(0);
}
int top = HeaderView.getTopOffset();
if (view != null) {
  top = Math.max(view.getTop() + (recyclerView != null ? recyclerView.getTop() : 0) + HeaderView.getTopOffset(), HeaderView.getTopOffset());
}

if (headerView != null) {
  setHeaderPosition(lastHeaderPosition = top);
}
*/
  }

  val targetHeight: Int
    get() = context().getRootView().measuredHeight

  abstract override fun supportsBottomInset(): Boolean

  override fun onBottomInsetChanged(extraBottomInset: Int, extraBottomInsetWithoutIme: Int, isImeInset: Boolean) {
    super.onBottomInsetChanged(extraBottomInset, extraBottomInsetWithoutIme, isImeInset)
    invalidateAllItemDecorations()
  }

  protected fun invalidateAllItemDecorations() {
    for (i in 0..<pagerItemCount) {
      val c = findCachedControllerByPosition(i)
      if (c is BottomSheetBaseControllerPage) {
        val customRecyclerView = (c as BottomSheetBaseControllerPage).recyclerView
        customRecyclerView?.invalidateItemDecorations()
      }
    }
  }

  @JvmField
  protected var headerBackgroundFactor: Float = 0f

  @JvmField
  protected var headerTranslationY: Float = 0f

  protected open fun setHeaderPosition(y: Float) {
    var y = y
    y = max(y, HeaderView.getTopOffset().toFloat())
    headerTranslationY = y
    val realHeaderOffset = y
    if (headerView != null) {
      headerView!!.translationY = realHeaderOffset
    }
    fixView!!.translationY = realHeaderOffset
    contentView!!.invalidate()
    fixView!!.invalidate()
    if (lickView != null) {
      val topOffset = HeaderView.getTopOffset()
      val top = y - topOffset
      lickView!!.translationY = realHeaderOffset - topOffset
      val factor = if (top > topOffset) 0f else 1f - (top / topOffset.toFloat())
      lickView!!.factor = factor
      onUpdateLickViewFactor(factor)
      // headerView.getFilling().setShadowAlpha(factor);
      setHeaderBackgroundFactor(factor)
    }
  }

  protected open fun setHeaderBackgroundFactor(headerBackgroundFactor: Float) {
    this.headerBackgroundFactor = headerBackgroundFactor
  }

  protected open val topEdge: Int
    get() = max(0, (headerTranslationY - HeaderView.getTopOffset()).toInt())

  override fun shouldTouchOutside(x: Float, y: Float): Boolean {
    return y < headerTranslationY - (if (headerView != null) getHeaderHeight(true) else 0)
  }

  override fun getCurrentPopupHeight(): Int {
    return (this.targetHeight - this.topEdge - (HeaderView.getTopOffset().toFloat()).toInt()) + max(wrapView!!.measuredHeight - this.targetHeight, 0)
  }

  fun maxItemsScrollYOffset(): Int {
    return maxItemsScrollY()
  }

  fun maxItemsScrollY(): Int {
    return this.contentOffset
  }

  fun checkContentScrollY(c: BottomSheetBaseControllerPage?) {
    val maxScrollY = maxItemsScrollYOffset()
    val scrollY = (this.contentOffset - headerTranslationY + HeaderView.getTopOffset()).toInt() //();
    c?.ensureMaxScrollY(scrollY, maxScrollY)
  }

  override fun launchCustomHeaderTransformAnimator(open: Boolean, transformMode: Int, listener: Animator.AnimatorListener?): Boolean {
    return PREVENT_HEADER_ANIMATOR && open && this.topEdge > 0
  }

  protected fun calculateTotalHeight(): Int {
    return this.targetHeight - (this.contentOffset + HeaderView.getTopOffset())
  }

  protected fun checkContentScrollY(position: Int) {
    if (viewPager!!.adapter is ViewPagerAdapter) {
      val adapter = (viewPager!!.adapter) as ViewPagerAdapter?
      val controller = adapter!!.getCachedItemByPosition(position)
      if (controller is BottomSheetBaseControllerPage) {
        checkContentScrollY(controller as BottomSheetBaseControllerPage)
      }
    }
  }

  override fun setCurrentPagerPosition(position: Int, animated: Boolean) {
    if (headerCell != null && animated) {
      headerCell!!.getTopView().setFromTo(viewPager!!.currentItem, position)
    }
    super.setCurrentPagerPosition(position, animated)
  }

  override val pagerSections: Array<String?>?
    get() = null

  override fun getCustomHeaderCell(): View? {
    return null
  }

  override fun destroy() {
    super.destroy()
    context().removeFullScreenView(this, false)
  }


  // PopupLayout
  private var popupLayout: PopupLayout? = null
  private var openLaunched = false

  @JvmField
  protected var isFirstCreation: Boolean = true

  fun show(): PopupLayout? {
    if (tdlib == null) {
      return null
    }
    popupLayout = object : PopupLayout(context()) {
      override fun onCustomShowComplete() {
        super.onCustomShowComplete()
        isFirstCreation = false
        if (!isDestroyed) {
          this@BottomSheetViewController.onCustomShowComplete()
          val c = currentPagerItem
          if (c is BottomSheetBaseControllerPage) {
            val r = (c as BottomSheetBaseControllerPage).recyclerView
            if (r != null) {
              r.invalidateItemDecorations()
              checkHeaderPosition(r)
            }
          }
        }
      }
    }

    setupPopupLayout(popupLayout!!)
    getValue()
    context().addFullScreenView(this, false)
    return popupLayout
  }

  protected open fun onCustomShowComplete() {
  }

  protected open fun setupPopupLayout(popupLayout: PopupLayout) {
    popupLayout.boundController = this
    popupLayout.setPopupHeightProvider(this)
    popupLayout.init(true)
    popupLayout.setNeedFullScreen(true)
    popupLayout.setTouchProvider(this)
  }

  fun launchOpenAnimation() {
    if (!openLaunched) {
      openLaunched = true
      popupLayout!!.showSimplePopupView(getValue(), calculateTotalHeight())
    }
  }

  fun hidePopupWindow(animated: Boolean) {
    popupLayout!!.hideWindow(animated)
  }

  fun setDismissListener(l: PopupLayout.DismissListener?) {
    popupLayout!!.setDismissListener(l)
  }

  fun getPopupLayout(): PopupLayout {
    return popupLayout!!
  }


  // LickView
  private var lickView: LickView? = null

  val lickViewFactor: Float
    get() = if (lickView != null) lickView!!.factor else 0f

  protected open fun onUpdateLickViewFactor(factor: Float) {
  }

  protected fun setLickViewColor(@ColorInt color: Int) {
    if (lickView != null) {
      lickView!!.setHeaderBackground(color)
    }
  }


  // Custom tooltip manager, used in case the popup window is opened on top of all content.
  // Might be worth moving this to the ViewController class
  private var tooltipOverlayView: TooltipOverlayView? = null

  fun tooltipManager(): TooltipOverlayView? {
    if (tooltipOverlayView == null) {
      tooltipOverlayView = TooltipOverlayView(context())
      tooltipOverlayView!!.setLayoutParams(FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
      tooltipOverlayView!!.setAvailabilityListener { overlayView: TooltipOverlayView?, hasChildren: Boolean ->
        if (hasChildren) {
          if (tooltipOverlayView!!.parent != null) return@setAvailabilityListener
          addToViewController(tooltipOverlayView)
        } else {
          removeFromViewController(tooltipOverlayView)
        }
      }
    }
    return tooltipOverlayView
  }

  fun addToViewController(view: View?) {
    wrapView!!.addView(view)
  }

  fun removeFromViewController(view: View?) {
    wrapView!!.removeView(view)
  }


  // Default listeners ind decorators
  protected open fun setDefaultListenersAndDecorators(controller: BottomSheetBaseControllerPage) {
    val recyclerView = controller.recyclerView
    recyclerView!!.isVerticalScrollBarEnabled = false
    recyclerView.addOnScrollListener(ScrollListener(this))
    recyclerView.addItemDecoration(ContentDecoration(this, controller))
    recyclerView.addOnLayoutChangeListener(LayoutChangeListener(this))
    addThemeInvalidateListener(recyclerView)
    checkContentScrollY(controller)
    if (canHideByScroll()) {
      UI.post { recyclerView.scrollBy(0, this.contentMinHeight + getHeaderHeight()) }
    }
  }

  class ContentDecoration(private val controller: BottomSheetViewController<*>, private val page: BottomSheetBaseControllerPage) :
    RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
      val position = parent.getChildAdapterPosition(view)
      val itemCount = parent.adapter!!.itemCount
      val isUnknown = position == RecyclerView.NO_POSITION
      var top = 0
      var bottom = 0

      if (position == 0 || isUnknown) {
        top =
          if (controller.canHideByScroll()) (controller.targetHeight - HeaderView.getTopOffset() - (if (instance().useEdgeToEdge()) controller.context()
            .getRootView().systemInsetsWithoutIme.bottom else 0)) else (controller.contentOffset)
      }
      if (position == itemCount - 1 || isUnknown) {
        val itemsHeight = if (isUnknown) view.measuredHeight else page.getItemsHeight(parent)
        val parentHeight = parent.measuredHeight - parent.paddingBottom
        bottom = parentHeight - itemsHeight
      }

      outRect.set(
        0, if (page.needTopDecorationOffsets(parent)) max(top, 0) else 0,
        0, if (page.needBottomDecorationOffsets(parent)) max(0, bottom) else 0
      )
    }
  }

  class ScrollListener(private val controller: BottomSheetViewController<*>) : RecyclerView.OnScrollListener() {
    private var ignoreScrollChangeState = false

    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
      super.onScrollStateChanged(recyclerView, newState)
      if (newState == RecyclerView.SCROLL_STATE_IDLE) {
        if (ignoreScrollChangeState) {
          ignoreScrollChangeState = false
          return
        }

        val c = controller.findCurrentCachedController()
        val canHideByScroll = controller.canHideByScroll()
        val contentOffset = controller.contentOffset
        val topEdge = controller.topEdge

        if (c is BottomSheetBaseControllerPage) {
          val ci = c as BottomSheetBaseControllerPage
          if (ci.recyclerView === recyclerView && !controller.ignoreAnyPagerScrollEventsBecauseOfMovements) {
            if (controller.lickViewFactor != 0f && controller.lickViewFactor != 1f) {
              ci.onScrollToTopRequested()
            } else if (canHideByScroll && (topEdge > contentOffset)) {
              if (controller.contentVisibleHeight > controller.hideByScrollBorder) {
                ci.recyclerView!!.smoothScrollBy(0, topEdge - contentOffset)
                ignoreScrollChangeState = true
              } else {
                controller.hidePopupWindow(true)
              }
            }
          }
        }
      }
    }

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
      val c = controller.findCurrentCachedController()
      val canHideByScroll = controller.canHideByScroll() && recyclerView.scrollState == RecyclerView.SCROLL_STATE_SETTLING
      val contentOffset = controller.contentOffset
      val topEdge = controller.topEdge

      if (c is BottomSheetBaseControllerPage) {
        val ci = c as BottomSheetBaseControllerPage
        if (ci.recyclerView === recyclerView && (!controller.ignoreAnyPagerScrollEventsBecauseOfMovements)) {
          controller.checkHeaderPosition(recyclerView)
          if (canHideByScroll && (topEdge > contentOffset)) {
            if (controller.contentVisibleHeight <= controller.hideByScrollBorder) {
              controller.hidePopupWindow(true)
            }
          }
        }
      }
    }
  }

  class LayoutChangeListener(private val controller: BottomSheetViewController<*>) : OnLayoutChangeListener {
    override fun onLayoutChange(view: View?, i: Int, i1: Int, i2: Int, i3: Int, i4: Int, i5: Int, i6: Int, i7: Int) {
      val c = controller.findCurrentCachedController()
      if (c is BottomSheetBaseControllerPage) {
        val ci = c as BottomSheetBaseControllerPage
        if (ci.recyclerView === view && controller.currentPositionOffset == 0f) {
          controller.checkHeaderPosition(view as RecyclerView?)
        }
      }
    }
  }


  //
  interface BottomSheetBaseControllerPage {
    fun onScrollToTopRequested()
    fun onScrollToBottomRequested()
    fun ensureMaxScrollY(scrollY: Int, maxScrollY: Int)

    val recyclerView: RecyclerView?

    fun getItemsHeight(parent: RecyclerView?): Int
    fun needTopDecorationOffsets(parent: RecyclerView?): Boolean
    fun needBottomDecorationOffsets(parent: RecyclerView?): Boolean
  }


  //
  abstract class BottomSheetBaseRecyclerViewController<T>(context: Context, tdlib: Tdlib?) : RecyclerViewController<T>(context, tdlib),
    BottomSheetBaseControllerPage {
    abstract override fun getItemsHeight(parent: RecyclerView?): Int

    override fun needTopDecorationOffsets(parent: RecyclerView?): Boolean {
      return true
    }

    override fun needBottomDecorationOffsets(parent: RecyclerView?): Boolean {
      return true
    }

    abstract override fun supportsBottomInset(): Boolean

    override fun ensureMaxScrollY(scrollY: Int, maxScrollY: Int) {
      val recyclerView = recyclerView
      if (recyclerView != null) {
        val manager = recyclerView.layoutManager as LinearLayoutManager?
        if (scrollY < maxScrollY) {
          manager!!.scrollToPositionWithOffset(0, -scrollY)
          return
        }

        val firstVisiblePosition = manager!!.findFirstVisibleItemPosition()
        if (firstVisiblePosition == 0 || firstVisiblePosition == -1) {
          val view = manager.findViewByPosition(0)
          if (view != null) {
            val top = view.top
            if (top > 0) {
              manager.scrollToPositionWithOffset(0, -maxScrollY)
            }
          } else {
            manager.scrollToPositionWithOffset(0, -maxScrollY)
          }
        }
      }
    }

    override fun onScrollToBottomRequested() {
      val recyclerView = recyclerView ?: return

      recyclerView.stopScroll()
      recyclerView.smoothScrollToPosition(0)
    }
  }

  companion object {
    private const val PREVENT_HEADER_ANIMATOR = false // TODO
  }
}
