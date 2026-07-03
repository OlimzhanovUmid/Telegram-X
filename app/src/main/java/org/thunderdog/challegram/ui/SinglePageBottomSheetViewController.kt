package org.thunderdog.challegram.ui

import android.content.Context
import android.view.WindowManager
import androidx.annotation.CallSuper
import me.vkryl.android.widget.FrameLayoutFix
import org.thunderdog.challegram.navigation.HeaderView
import org.thunderdog.challegram.navigation.ViewController
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.ui.BottomSheetViewController.BottomSheetBaseControllerPage
import org.thunderdog.challegram.widget.PopupLayout
import org.thunderdog.challegram.widget.ViewPager

abstract class SinglePageBottomSheetViewController<V, A> (context: Context, tdlib: Tdlib?) : BottomSheetViewController<A>(context, tdlib)
  where V : ViewController<A>, V : BottomSheetBaseControllerPage {

  @JvmField
  protected val singlePage: V = onCreateSinglePage()

  protected final override val pagerItemCount: Int
    get() = 1

  override fun getId (): Int {
    return singlePage.getId()
  }

  @CallSuper
  override fun onBeforeCreateView () {
    singlePage.getValue()
  }

  override fun setArguments (args: A) {
    super.setArguments(args)
    singlePage.setArguments(args)
  }

  protected abstract fun onCreateSinglePage (): V

  override fun onCreateView (context: Context?, contentView: FrameLayoutFix?, pager: ViewPager?) {
    pager?.setOffscreenPageLimit(1)
  }

  protected final override fun onCreatePagerItemForPosition (context: Context?, position: Int): ViewController<*>? {
    if (position != 0) return null
    setHeaderPosition((contentOffset + HeaderView.getTopOffset()).toFloat())
    setDefaultListenersAndDecorators(singlePage)
    return singlePage
  }

  override fun setupPopupLayout (popupLayout: PopupLayout) {
    popupLayout.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    popupLayout.boundController = singlePage
    popupLayout.setPopupHeightProvider(this)
    popupLayout.init(true)
    popupLayout.setNeedRootInsets()
    popupLayout.setTouchProvider(this)
    popupLayout.setIgnoreHorizontal()
  }
}
