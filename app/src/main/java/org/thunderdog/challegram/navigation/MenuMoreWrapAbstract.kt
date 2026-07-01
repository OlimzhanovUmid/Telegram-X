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
 * File created on 12/06/2024
 */
package org.thunderdog.challegram.navigation

import android.animation.Animator
import android.content.Context
import android.widget.LinearLayout
import org.thunderdog.challegram.tool.Views
import me.vkryl.android.ACCELERATE_INTERPOLATOR
import me.vkryl.android.DECELERATE_INTERPOLATOR

abstract class MenuMoreWrapAbstract(context: Context) : LinearLayout(context) {
  abstract val itemsWidth: Int

  abstract val itemsHeight: Int

  open val anchorMode: Int
    get() = MenuMoreWrap.ANCHOR_MODE_RIGHT

  open fun shouldPivotBottom(): Boolean = false

  open val revealRadius: Float
    get() = Math.hypot(itemsWidth.toDouble(), itemsHeight.toDouble()).toFloat()

  open fun scaleIn(listener: Animator.AnimatorListener?) {
    Views.animate(this, 1f, 1f, 1f, 135L, 10L, DECELERATE_INTERPOLATOR, listener)
  }

  open fun scaleOut(listener: Animator.AnimatorListener?) {
    Views.animate(this, MenuMoreWrap.START_SCALE, MenuMoreWrap.START_SCALE, 0f, 120L, 0L, ACCELERATE_INTERPOLATOR, listener)
  }
}
