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
 */
package org.thunderdog.challegram.data

import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import org.drinkless.tdlib.TdApi
import org.thunderdog.challegram.loader.DoubleImageReceiver
import org.thunderdog.challegram.loader.ImageReceiver
import org.thunderdog.challegram.loader.Receiver
import org.thunderdog.challegram.util.DrawableProvider
import org.thunderdog.challegram.widget.FileProgressComponent
import me.vkryl.android.util.ViewProvider
import me.vkryl.core.lambda.Destroyable

abstract class BaseComponent : Destroyable {
  @JvmField
  protected var viewProvider: ViewProvider? = null

  open fun setViewProvider(provider: ViewProvider?) {
    this.viewProvider = provider
  }

  abstract fun <T> draw(
    view: T, c: Canvas, startX: Int, startY: Int,
    preview: Receiver, receiver: Receiver,
    @ColorInt backgroundColor: Int, contentReplaceColor: Int,
    alpha: Float, checkFactor: Float
  ) where T : View, T : DrawableProvider

  abstract fun buildLayout(maxWidth: Int)

  abstract fun requestPreview(receiver: DoubleImageReceiver)

  abstract fun requestContent(receiver: ImageReceiver)

  abstract val height: Int

  abstract val width: Int

  abstract fun getContentRadius(defaultValue: Int): Int

  abstract fun onTouchEvent(view: View, event: MotionEvent): Boolean

  open val file: TdApi.File?
    get() = null

  open val fileProgress: FileProgressComponent?
    get() = null
}
