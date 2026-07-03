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
 * File created on 06/07/2018
 */
package org.thunderdog.challegram.ui

import android.content.Context
import org.thunderdog.challegram.R
import org.thunderdog.challegram.navigation.BackHeaderButton
import org.thunderdog.challegram.navigation.ViewController
import org.thunderdog.challegram.navigation.ViewPagerController
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.widget.ViewPager
import java.util.Locale
import me.vkryl.android.widget.FrameLayoutFix

class SimpleViewPagerController (context: Context, tdlib: Tdlib, private val controllers: Array<ViewController<*>>, private val sections: Array<String>?, private val isWhite: Boolean) : ViewPagerController<Any>(context, tdlib) {
  init {
    if (sections != null && sections.size != controllers.size) {
      throw IllegalArgumentException(sections.size.toString() + " != " + controllers.size)
    }
  }

  override fun supportsBottomInset (): Boolean = true

  override fun getBackButton (): Int = BackHeaderButton.TYPE_BACK

  override fun useCenteredTitle (): Boolean = true

  override fun getId (): Int = R.id.controller_simplePager

  override val pagerItemCount: Int
    get() = controllers.size

  override fun onCreateView (context: Context?, contentView: FrameLayoutFix?, pager: ViewPager?) {
    if (isWhite && headerCell != null) {
      headerCell!!.getTopView().setTextFromToColorId(ColorId.NONE, ColorId.text)
    }
    prepareControllerForPosition(0) { executeScheduledAnimation() }
  }

  override fun needAsynchronousAnimation (): Boolean {
    val first = getCachedControllerForPosition(0)
    return first?.needAsynchronousAnimation() ?: super.needAsynchronousAnimation()
  }

  override fun getAsynchronousAnimationTimeout (fastAnimation: Boolean): Long {
    val first = getCachedControllerForPosition(0)
    return first?.getAsynchronousAnimationTimeout(fastAnimation) ?: super.getAsynchronousAnimationTimeout(fastAnimation)
  }

  override fun onCreatePagerItemForPosition (context: Context?, position: Int): ViewController<*>? = controllers[position]

  override val pagerSections: Array<String>?
    get() {
      if (sections != null) {
        return sections
      }
      return Array(controllers.size) { i -> controllers[i].getName().toString().uppercase(Locale.getDefault()) }
    }

  override fun getHeaderColorId (): Int = if (isWhite) ColorId.filling else super.getHeaderColorId()

  override fun getHeaderIconColorId (): Int = if (isWhite) ColorId.headerLightIcon else super.getHeaderIconColorId()

  override fun getHeaderTextColorId (): Int = if (isWhite) ColorId.text else super.getHeaderTextColorId()
}
