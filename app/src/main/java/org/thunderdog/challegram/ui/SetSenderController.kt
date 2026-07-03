package org.thunderdog.challegram.ui

import android.content.Context
import android.view.WindowManager
import androidx.recyclerview.widget.RecyclerView
import me.vkryl.android.widget.FrameLayoutFix
import org.drinkless.tdlib.TdApi
import org.thunderdog.challegram.R
import org.thunderdog.challegram.navigation.HeaderView
import org.thunderdog.challegram.navigation.ViewController
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.theme.ColorState
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.ui.BottomSheetViewController.BottomSheetBaseControllerPage
import org.thunderdog.challegram.widget.PopupLayout
import org.thunderdog.challegram.widget.ViewPager

class SetSenderController (context: Context, tdlib: Tdlib) : BottomSheetViewController<SetSenderController.Args>(context, tdlib) {
  private val setSenderControllerPage: SetSenderControllerPage = SetSenderControllerPage(context, tdlib, this)

  fun setDelegate (delegate: SetSenderControllerPage.Delegate) {
    setSenderControllerPage.setDelegate(delegate)
  }

  protected override fun onBeforeCreateView () {
    setSenderControllerPage.setArguments(getArguments())
    setSenderControllerPage.getValue()
  }

  protected override fun onCreateHeaderView (): HeaderView? {
    return setSenderControllerPage.getHeaderView()
  }

  override fun supportsBottomInset (): Boolean {
    return true
  }

  protected override fun onCreateView (context: Context?, contentView: FrameLayoutFix?, pager: ViewPager?) {
    pager?.setOffscreenPageLimit(1)
    tdlib!!.ui().post(this::launchOpenAnimation)
  }

  protected override fun onAfterCreateView () {
    setLickViewColor(Theme.getColor(ColorId.headerLightBackground))
  }

  override fun onThemeColorsChanged (areTemp: Boolean, state: ColorState?) {
    super.onThemeColorsChanged(areTemp, state)
    setLickViewColor(Theme.getColor(ColorId.headerLightBackground))
  }

  private var showOverEverything = false

  fun setShowOverEverything (showOverEverything: Boolean) {
    this.showOverEverything = showOverEverything
  }

  protected override fun setupPopupLayout (popupLayout: PopupLayout) {
    if (showOverEverything) {
      popupLayout.boundController = setSenderControllerPage
      popupLayout.setPopupHeightProvider(this)
      popupLayout.setOverlayStatusBar(true)
      popupLayout.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
      popupLayout.setTouchProvider(this)
      popupLayout.setNeedRootInsets()
      popupLayout.setActivityListener(this)
      popupLayout.setHideKeyboard()
      popupLayout.init(false)
      return
    }
    popupLayout.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    popupLayout.boundController = setSenderControllerPage
    popupLayout.setPopupHeightProvider(this)
    popupLayout.init(true)
    popupLayout.setHideKeyboard()
    popupLayout.setNeedRootInsets()
    popupLayout.setTouchProvider(this)
    popupLayout.setIgnoreHorizontal()
  }

  protected override fun setDefaultListenersAndDecorators (controller: BottomSheetBaseControllerPage) {
    controller.recyclerView!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
      override fun onScrollStateChanged (recyclerView: RecyclerView, newState: Int) {
        super.onScrollStateChanged(recyclerView, newState)
        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
          if (setSenderControllerPage.inSearchMode()) {
            if (lickViewFactor == 1f) {
              invalidateAllItemDecorations()
            } else {
              setSenderControllerPage.onScrollToTopRequested()
            }
          }
        }
      }
    })
    super.setDefaultListenersAndDecorators(controller)
  }

  protected override fun onUpdateLickViewFactor (factor: Float) {
    if (headerView == null) return

    headerView!!.getFilling().setShadowAlpha(factor)
  }

  protected override fun getHeaderHeight (): Int {
    return Screen.dp(56f)
  }

  protected override val pagerItemCount: Int
    get() = 1

  protected override fun onCreatePagerItemForPosition (context: Context?, position: Int): ViewController<*>? {
    if (position != 0) return null
    setHeaderPosition((contentOffset + HeaderView.getTopOffset()).toFloat())
    setDefaultListenersAndDecorators(setSenderControllerPage)
    return setSenderControllerPage
  }

  protected override val contentOffset: Int
    get() = (targetHeight - getHeaderHeight(true)) / 2

  protected override fun canHideByScroll (): Boolean {
    return true
  }

  override fun getId (): Int {
    return R.id.controller_sender
  }

  class Args (@JvmField val chat: TdApi.Chat, @JvmField val chatAvailableSenders: Array<TdApi.ChatMessageSender>, @JvmField val currentSender: TdApi.MessageSender?)
}
