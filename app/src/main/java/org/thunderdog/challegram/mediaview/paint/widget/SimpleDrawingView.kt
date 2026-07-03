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
 * File created on 11/06/2017
 */
package org.thunderdog.challegram.mediaview.paint.widget

import android.content.Context
import android.graphics.Canvas
import android.view.View

import org.thunderdog.challegram.mediaview.paint.PaintState

class SimpleDrawingView (context: Context) : View(context), PaintState.SimpleDrawingChangeListener {
  private var state: PaintState? = null

  fun setPaintState (state: PaintState?) {
    this.state?.removeSimpleDrawingChangeListener(this)
    this.state = state
    state?.addSimpleDrawingChangeListener(this)
    invalidate()
  }

  override fun onSimpleDrawingsChanged (state: PaintState) {
    if (this.state === state) {
      invalidate()
    }
  }

  override fun onDraw (c: Canvas) {
    state?.draw(c, 0, 0, measuredWidth, measuredHeight)
  }
}
