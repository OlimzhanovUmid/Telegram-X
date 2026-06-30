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
 * File created on 26/01/2017
 */
package org.thunderdog.challegram.support

import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import me.vkryl.core.compositeColor
import me.vkryl.core.fromToArgb
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.util.ColorChangeAcceptorDelegate

abstract class SimpleShapeDrawable(@field:ColorId private var colorId: Int, @JvmField protected val size: Float, private val isPressed: Boolean) : Drawable(),
    ColorChangeAcceptorDelegate {
    private var changeFactor = 0f

    @ColorId
    private var toColorId = 0

    override fun applyColor(@ColorId fromColorId: Int, @ColorId toColorId: Int, factor: Float) {
        if (colorId != fromColorId || changeFactor != factor || (this.toColorId != toColorId && factor > 0f)) {
            this.colorId = fromColorId
            this.changeFactor = factor
            this.toColorId = toColorId
        }
    }

    override fun getDrawColor(): Int {
        val color: Int
        if (changeFactor == 0f) {
            color = if (colorId != 0) Theme.getColor(colorId) else 0
        } else if (changeFactor == 1f) {
            color = if (toColorId != 0) Theme.getColor(toColorId) else 0
        } else {
            color = fromToArgb(if (colorId != 0) Theme.getColor(colorId) else 0, if (toColorId != 0) Theme.getColor(toColorId) else 0, changeFactor)
        }
        return if (isPressed) compositeColor(color, 0x40a0a0a0) else color
    }

    override fun setAlpha(alpha: Int) {}

    override fun setColorFilter(colorFilter: ColorFilter?) {}

    @Suppress("deprecation")
    override fun getOpacity(): Int {
        return PixelFormat.UNKNOWN
    }

    companion object {
        const val USE_SOFTWARE_SHADOW: Boolean = false
    }
}
