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
 * File created on 09/02/2018
 */
package org.thunderdog.challegram.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.view.View

import org.thunderdog.challegram.R
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Drawables
import org.thunderdog.challegram.tool.Paints
import org.thunderdog.challegram.tool.Screen

import me.vkryl.core.lambda.Destroyable

class WebsiteEmptyImageView (context: Context) : View(context), Destroyable {
  private var starIcon: Drawable? = null
  private var loveIcon: Drawable? = null
  private var walletIcon: Drawable? = null
  private var locationIcon: Drawable? = null

  init {
    prepare()
  }

  override fun performDestroy () {
    if (starIcon != null) {
      starIcon = null
      walletIcon = null
      locationIcon = null
      loveIcon = null
    }
  }

  private fun prepare () {
    if (starIcon == null) {
      starIcon = Drawables.get(resources, R.drawable.baseline_star_24)
      walletIcon = Drawables.get(resources, R.drawable.baseline_account_balance_wallet_24)
      locationIcon = Drawables.get(resources, R.drawable.baseline_location_on_24)
      loveIcon = Drawables.get(resources, R.drawable.baseline_favorite_20)
    }
  }

  override fun onDraw (c: Canvas) {
    prepare()

    val starIcon = starIcon ?: return
    val loveIcon = loveIcon ?: return
    val walletIcon = walletIcon ?: return
    val locationIcon = locationIcon ?: return

    val cx = measuredWidth / 2
    val cy = measuredHeight / 2

    val size = Screen.dp(36f)
    val spacing = Screen.dp(2f)

    val radius = Screen.dp(3f).toFloat()
    val color = Theme.backgroundIconColor()

    val strokeSize = Screen.dp(2f)

    val iconPaint = Paints.getBackgroundIconPorterDuffPaint()

    val rectF = Paints.getRectF()
    rectF.left = (cx - spacing - size + strokeSize / 2).toFloat()
    rectF.right = (cx - spacing - strokeSize / 2).toFloat()
    rectF.top = (cy - spacing - size + strokeSize / 2).toFloat()
    rectF.bottom = (cy - spacing - strokeSize / 2).toFloat()
    c.drawRoundRect(rectF, radius, radius, Paints.getProgressPaint(color, strokeSize.toFloat()))
    drawIcon(c, rectF, starIcon, iconPaint)

    rectF.top = (cy + spacing + strokeSize / 2).toFloat()
    rectF.bottom = (cy + spacing + size - strokeSize / 2).toFloat()
    c.drawRoundRect(rectF, radius, radius, Paints.getProgressPaint(color, strokeSize.toFloat()))
    drawIcon(c, rectF, walletIcon, iconPaint)

    rectF.left = (cx + spacing + strokeSize / 2).toFloat()
    rectF.right = (cx + spacing + size - strokeSize / 2).toFloat()
    c.drawRoundRect(rectF, radius, radius, Paints.getProgressPaint(color, strokeSize.toFloat()))
    drawIcon(c, rectF, locationIcon, iconPaint)

    rectF.top = (cy - spacing - size + strokeSize / 2).toFloat()
    rectF.bottom = (cy - spacing - strokeSize / 2).toFloat()
    c.drawRoundRect(rectF, radius, radius, Paints.getProgressPaint(color, strokeSize.toFloat()))
    drawIcon(c, rectF, loveIcon, iconPaint)
  }

  companion object {
    private fun drawIcon (c: Canvas, rectF: RectF, icon: Drawable, paint: Paint) {
      Drawables.draw(c, icon, rectF.centerX() - icon.minimumWidth / 2, rectF.centerY() - icon.minimumHeight / 2, paint)
    }
  }
}
