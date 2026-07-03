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
 * File created on 04/06/2018
 */
package org.thunderdog.challegram.widget

import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.collection.SparseArrayCompat
import org.thunderdog.challegram.util.DrawableProvider

open class SparseDrawableView @JvmOverloads constructor (context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr), DrawableProvider {
  // Sparse drawable function
  private var sparseDrawables: SparseArrayCompat<Drawable>? = null

  final override fun getSparseDrawableHolder (): SparseArrayCompat<Drawable> {
    return sparseDrawables ?: SparseArrayCompat<Drawable>().also { sparseDrawables = it }
  }

  final override fun getSparseDrawableResources (): Resources {
    return resources
  }
}
