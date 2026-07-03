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
 * File created on 13/02/2016 at 12:24
 */
package org.thunderdog.challegram.navigation

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import android.widget.Toast

import org.thunderdog.challegram.R
import org.thunderdog.challegram.theme.ThemeDeprecated
import org.thunderdog.challegram.tool.Drawables
import org.thunderdog.challegram.tool.Paints
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.tool.UI
import org.thunderdog.challegram.ui.PasscodeController
import org.thunderdog.challegram.unsorted.Passcode

import me.vkryl.android.animator.BoolAnimator
import me.vkryl.android.animator.FactorAnimator

class LockHeaderButton (context: Context) : HeaderButton(context), View.OnClickListener, View.OnLongClickListener, FactorAnimator.Target {
  private val animator = BoolAnimator(0, this, OVERSHOOT_INTERPOLATOR, 130L)

  private val lock: Drawable = Drawables.get(resources, R.drawable.baseline_lock_top_24)
  private val base: Drawable = Drawables.get(resources, R.drawable.baseline_lock_base_24)

  init {
    setId(R.id.menu_btn_lock)
    setButtonBackground(ThemeDeprecated.headerSelector())
    setVisibility(if (Passcode.instance().isEnabled) View.VISIBLE else View.GONE)
    setOnClickListener(this)
    setOnLongClickListener(this)
    setLayoutParams(LinearLayout.LayoutParams(Screen.dp(49f), ViewGroup.LayoutParams.MATCH_PARENT))
  }

  override fun onMeasure (widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    animator.setValue(!isVisuallyLocked(), false)
  }

  override fun onFactorChanged (id: Int, factor: Float, fraction: Float, callee: FactorAnimator?) {
    invalidate()
  }

  override fun onFactorChangeFinished (id: Int, finalFactor: Float, callee: FactorAnimator?) {
    invalidate()
  }

  override fun onClick (v: View) {
    if (Passcode.instance().autolockMode == Passcode.AUTOLOCK_MODE_INSTANT) {
      UI.showToast(R.string.AutoLockInstantWarn, Toast.LENGTH_SHORT)
      return
    }
    val open = !Passcode.instance().toggleLock()
    animator.setValue(open, true)
    UI.getContext(context).checkPasscode(true)
  }

  override fun onLongClick (v: View): Boolean {
    val navigation = UI.getContext(context).navigation()
    val current = navigation?.currentStackItem
    if (current != null) {
      val passcode = PasscodeController(UI.getContext(context), current.tdlib())
      passcode.setPasscodeMode(PasscodeController.MODE_UNLOCK_SETUP)
      navigation.navigateTo(passcode)
      return true
    }
    return false
  }

  fun update () {
    val visibility = if (Passcode.instance().isEnabled) View.VISIBLE else View.GONE
    val open = !isVisuallyLocked()
    animator.setValue(open, false)
    if (visibility != getVisibility()) {
      setVisibility(visibility)
    }
  }

  override fun onDraw (c: Canvas) {
    val cx = measuredWidth / 2
    val cy = measuredHeight / 2

    val paint = Paints.getHeaderIconPaint()
    Drawables.draw(c, lock, (cx - lock.minimumWidth / 2 + (Screen.dp(8f).toFloat() * animator.getFloatValue()).toInt()).toFloat(), (cy - lock.minimumHeight / 2).toFloat(), paint)
    Drawables.draw(c, base, (cx - base.minimumWidth / 2).toFloat(), (cy - base.minimumHeight / 2).toFloat(), paint)
  }

  companion object {
    private val OVERSHOOT_INTERPOLATOR = OvershootInterpolator(3f)

    private fun isVisuallyLocked (): Boolean {
      return Passcode.instance().isLocked || Passcode.instance().autolockMode == Passcode.AUTOLOCK_MODE_INSTANT
    }
  }
}
