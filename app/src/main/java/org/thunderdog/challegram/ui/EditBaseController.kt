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
 * File created on 21/12/2016
 */
package org.thunderdog.challegram.ui

import android.content.Context
import android.view.*
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemAnimator
import me.vkryl.android.DECELERATE_INTERPOLATOR
import me.vkryl.android.animator.FactorAnimator
import me.vkryl.android.widget.FrameLayoutFix
import me.vkryl.android.widget.FrameLayoutFix.Companion.newParams
import org.thunderdog.challegram.R
import org.thunderdog.challegram.component.attach.CustomItemAnimator
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.navigation.BackHeaderButton
import org.thunderdog.challegram.navigation.ViewController
import org.thunderdog.challegram.support.ViewSupport
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.tool.Keyboard
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.tool.Views
import org.thunderdog.challegram.util.DoneListener
import org.thunderdog.challegram.widget.DoneButton
import org.thunderdog.challegram.widget.MaterialEditText
import org.thunderdog.challegram.widget.MaterialEditText.EnterKeyListener

abstract class EditBaseController<T>(context: Context, tdlib: Tdlib?) : ViewController<T>(context, tdlib),
  FactorAnimator.Target,
  EnterKeyListener,
  DoneListener {
  override fun getBackButton(): Int {
    return BackHeaderButton.TYPE_BACK
  }

  protected abstract fun onCreateView(context: Context, contentView: FrameLayoutFix, recyclerView: RecyclerView)

  private var contentView: FrameLayoutFix? = null
  @JvmField
  protected var recyclerView: RecyclerView? = null

  open fun getRecyclerView(): RecyclerView? = recyclerView

  private var doneButton: DoneButton? = null

  @JvmField
  protected var itemAnimator: ItemAnimator? = null

  @get:ColorId
  protected open val recyclerBackgroundColorId: Int
    get() = ColorId.filling

  protected fun getDoneButton(): DoneButton? {
    return if (isDoneVisible()) doneButton else null
  }

  override fun supportsBottomInset(): Boolean {
    return true
  }

  override fun onBottomInsetChanged(extraBottomInset: Int, extraBottomInsetWithoutIme: Int, isImeInset: Boolean) {
    super.onBottomInsetChanged(extraBottomInset, extraBottomInsetWithoutIme, isImeInset)
    Views.applyBottomInset(recyclerView, extraBottomInset)
    Views.setBottomMargin(doneButton, Screen.dp(16f) - Screen.dp(4f) + extraBottomInset)
  }

  override fun onCreateView(context: Context): View {
    contentView = FrameLayoutFix(context)
    ViewSupport.setThemedBackground(contentView, this.recyclerBackgroundColorId, this)
    contentView?.setLayoutParams(newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

    recyclerView = onCreateRecyclerView()
    Views.applyBottomInset(recyclerView, extraBottomInset)
    contentView?.addView(recyclerView)

    val padding = Screen.dp(4f)
    val params = newParams(
      width = Screen.dp(56f) + padding * 2,
      height = Screen.dp(56f) + padding * 2,
      gravity = (if (Lang.rtl()) Gravity.LEFT else Gravity.RIGHT) or Gravity.BOTTOM
    ).apply {
      leftMargin = Screen.dp(16f) - padding
      rightMargin = leftMargin
      bottomMargin = Screen.dp(16f) - padding + extraBottomInset
    }

    val doneButton = DoneButton(context)
    this.doneButton = doneButton
    doneButton.setId(R.id.btn_done)
    addThemeInvalidateListener(doneButton)
    doneButton.setOnClickListener { v: View? ->
      if (doneVisible) {
        onDoneClick(null)
      }
    }
    doneButton.setLayoutParams(params)
    doneButton.setMaximumAlpha(0f)
    contentView?.addView(doneButton)

    onCreateView(context, contentView!!, recyclerView!!)

    return FrameLayoutFix(context).apply {
      setLayoutParams(newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
      addView(contentView)
    }
  }

  protected open fun onCreateRecyclerView(): RecyclerView = (Views.inflate(context(), R.layout.recycler, contentView) as RecyclerView).apply {
    setItemAnimator(CustomItemAnimator(DECELERATE_INTERPOLATOR, 180L).also { this@EditBaseController.itemAnimator = it })
    setHasFixedSize(true)
    setLayoutManager(LinearLayoutManager(this.context, RecyclerView.VERTICAL, false))
    setLayoutParams(newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
  }

  override val rootColorId: Int
    get() = this.recyclerBackgroundColorId

  override fun onDoneClick(v: View?): Boolean {
    return onDoneClick()
  }

  override fun onEnterPressed(v: MaterialEditText?): Boolean {
    return onDoneClick(v)
  }

  override fun getViewForApplyingOffsets(): View {
    return contentView!!
  }

  class SimpleEditorActionListener(@JvmField val imeAction: Int, private val doneListener: DoneListener?) : OnEditorActionListener {
    fun hasContext(): Boolean {
      return doneListener != null
    }

    override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
      return (doneListener != null) &&
        (actionId == imeAction)
        && ((event == null) ||
        (event.action == MotionEvent.ACTION_DOWN))
        && doneListener.onDoneClick(v)
    }
  }

  private var doneVisible = false
  private var doneVisibilityFactor = 0f

  private var needDoneFadeIn = false

  protected open fun needShowAnimationDelay(): Boolean {
    return true
  }

  protected fun isDoneVisible(): Boolean {
    return doneVisible
  }

  protected open fun onDoneVisibleChanged(isVisible: Boolean) {
    // override
  }

  protected open fun setDoneVisible(isVisible: Boolean) {
    setDoneVisible(isVisible, true)
  }

  protected fun setDoneVisible(isVisible: Boolean, allowAnimation: Boolean) {
    if (this.doneVisible != isVisible) {
      this.doneVisible = isVisible
      if (contentView?.parent != null && doneButton?.measuredWidth != 0 && isFocused()) {
        this.doneVisibilityFactor = 1f
        doneButton?.setMaximumAlpha(1f)
        doneButton?.setIsVisible(isVisible, allowAnimation)
      } else {
        if (isVisible) {
          if (needShowAnimationDelay()) {
            this.doneVisibilityFactor = 0f
            doneButton?.setMaximumAlpha(0f)
            this.needDoneFadeIn = true
          } else {
            this.doneVisibilityFactor = 1f
            doneButton?.setMaximumAlpha(1f)
          }
        }
        doneButton?.setIsVisible(isVisible, false)
      }
      onDoneVisibleChanged(isVisible)
    }
  }

  protected fun setInstantDoneVisible(isVisible: Boolean) {
    if (this.doneVisible != isVisible) {
      this.doneVisible = isVisible
      this.doneVisibilityFactor = 1f
      doneButton?.setMaximumAlpha(1f)
      doneButton?.setIsVisible(isVisible, false)
      onDoneVisibleChanged(isVisible)
    }
  }

  protected var isInProgress: Boolean = false
    set(inProgress) {
      if (field != inProgress) {
        field = inProgress
        this.isDoneInProgress = inProgress
        onProgressStateChanged(inProgress)
      }
    }

  protected open fun onProgressStateChanged(inProgress: Boolean) {
    // override
  }

  protected var isDoneInProgress: Boolean
    get() = doneButton?.isInProgress == true
    set(inProgress) {
      doneButton?.setInProgress(inProgress)
    }

  override fun onFactorChanged(id: Int, factor: Float, fraction: Float, callee: FactorAnimator?) {
    if (id == DONE_FADE_ANIMATOR) {
      setDoneVisibilityFactor(factor)
    } else {
      onChildFactorChanged(id, factor, fraction)
    }
  }

  override fun onFactorChangeFinished(id: Int, finalFactor: Float, callee: FactorAnimator?) {
    if (id != DONE_ANIMATOR) {
      onChildFactorChangeFinished(id, finalFactor)
    }
  }

  protected fun onChildFactorChanged(id: Int, factor: Float, fraction: Float) {
    // override
  }

  protected fun onChildFactorChangeFinished(id: Int, finalFator: Float) {
    // override
  }

  private fun setDoneVisibilityFactor(factor: Float) {
    if (this.doneVisibilityFactor != factor) {
      this.doneVisibilityFactor = factor
      doneButton!!.setMaximumAlpha(factor)
    }
  }

  protected fun setDoneIcon(@DrawableRes icon: Int) {
    setDoneIcon(icon, 0)
  }

  protected fun setDoneIcon(@DrawableRes icon: Int, offsetLeft: Int) {
    if (doneButton?.alpha != 0f) {
      doneButton?.replaceIcon(icon, offsetLeft)
    } else {
      doneButton?.setIcon(icon, offsetLeft)
    }
  }

  protected open fun onDoneClick(): Boolean {
    // override
    return false
  }

  protected fun onSaveCompleted() {
    if (getKeyboardState() && getLockFocusView() != null) {
      Keyboard.hide(getLockFocusView())
      tdlib()?.ui()?.postDelayed({ this.navigateBack() }, 120)
    } else {
      navigateBack()
    }
  }

  override fun onFocus() {
    super.onFocus()
    if (needDoneFadeIn) {
      needDoneFadeIn = false
      val animator = FactorAnimator(DONE_FADE_ANIMATOR, this, DECELERATE_INTERPOLATOR, 180L)
      animator.setStartDelay(120L)
      animator.animateTo(1f)
    }
  }

  override fun handleLanguageDirectionChange() {
    super.handleLanguageDirectionChange()
    if (Views.setGravity(doneButton, (if (Lang.rtl()) Gravity.LEFT else Gravity.RIGHT) or Gravity.BOTTOM)) {
      Views.updateLayoutParams(doneButton)
    }
  }

  public override fun handleLanguagePackEvent(event: Int, arg1: Int) {
    (recyclerView?.adapter as? SettingsAdapter)?.let { adapter ->
      when (event) {
        Lang.EVENT_PACK_CHANGED, Lang.EVENT_DIRECTION_CHANGED -> adapter.notifyAllStringsChanged()
        Lang.EVENT_STRING_CHANGED -> adapter.notifyStringChanged(arg1)
        Lang.EVENT_DATE_FORMAT_CHANGED -> {}
      }
    }
  }

  companion object {
    private const val DONE_ANIMATOR = 0
    private const val DONE_FADE_ANIMATOR = 1
  }
}
