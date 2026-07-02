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
 * File created on 14/06/2024
 */
package org.thunderdog.challegram.util.text.counter

import android.graphics.Canvas
import androidx.annotation.FloatRange
import org.thunderdog.challegram.util.text.TextColorSet
import me.vkryl.android.animator.CounterAnimator

interface CounterTextPart : CounterAnimator.TextDrawable {
  fun draw (c: Canvas, startX: Int, endX: Int, endXBottomPadding: Int, startY: Int, defaultTheme: TextColorSet?, @FloatRange(from = 0.0, to = 1.0) alpha: Float)
}
