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
 * File created on 18/08/2023
 */
package org.thunderdog.challegram.widget.emoji.section

import android.content.Context
import android.graphics.Canvas
import android.view.View
import org.thunderdog.challegram.tool.Screen

class EmojiSectionView (context: Context) : View(context) {
  private var forceWidth = -1

  private var section: EmojiSection? = null

  fun setSection (section: EmojiSection?) {
    this.section?.setCurrentView(null)
    this.section = section
    section?.setCurrentView(this)
  }

  fun getSection (): EmojiSection? {
    return section
  }

  fun setForceWidth (width: Int) {
    forceWidth = width
  }

  override fun onMeasure (widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val itemWidth = if (forceWidth > 0) forceWidth else Screen.dp(44f)
    setMeasuredDimension(MeasureSpec.makeMeasureSpec(itemWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(heightMeasureSpec), MeasureSpec.EXACTLY))
  }

  override fun onDraw (c: Canvas) {
    section?.draw(c, measuredWidth / 2, measuredHeight / 2)
  }
}
