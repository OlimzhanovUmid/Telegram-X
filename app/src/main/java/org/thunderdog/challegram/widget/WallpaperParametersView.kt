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
package org.thunderdog.challegram.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.TextPaint
import android.view.MotionEvent
import android.view.View

import org.drinkless.tdlib.TdApi
import org.thunderdog.challegram.R
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.theme.ChatStyleChangeListener
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.theme.TGBackground
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.theme.ThemeManager
import org.thunderdog.challegram.tool.Fonts
import org.thunderdog.challegram.tool.Paints
import org.thunderdog.challegram.tool.Screen

import me.vkryl.android.animator.BoolAnimator
import me.vkryl.android.util.ClickHelper
import me.vkryl.core.lambda.Destroyable
import me.vkryl.android.DECELERATE_INTERPOLATOR

class WallpaperParametersView (context: Context) : View(context), ClickHelper.Delegate, Destroyable, ChatStyleChangeListener {
  private var listener: WallpaperParametersListener? = null
  private var isInitialBlur = false

  private val textPaint: Paint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
  private val blurRect = RectF()
  private val helper = ClickHelper(this)

  private val isBlurEnabled = BoolAnimator(0, { _, factor, _, _ ->
    listener?.onBlurValueAnimated(if (isInitialBlur) 1f - factor else factor)
    invalidate()
  }, DECELERATE_INTERPOLATOR, 180L)

  private val isViewShown = BoolAnimator(1, { _, factor, _, _ ->
    listener?.onParametersViewScaleChanged(factor)
  }, DECELERATE_INTERPOLATOR, 180L)

  init {
    textPaint.color = Theme.textAccentColor()
    textPaint.typeface = Fonts.getRobotoRegular()
    textPaint.textSize = Screen.sp(14f).toFloat()
    isViewShown.setValue(true, false)
    ThemeManager.instance().addChatStyleListener(this)
    setWillNotDraw(false)
  }

  fun initWith (background: TdApi.Background, listener: WallpaperParametersListener?) {
    isBlurEnabled.setValue((background.type as TdApi.BackgroundTypeWallpaper).isBlurred, false)
    isInitialBlur = isBlurEnabled.getValue()
    this.listener = listener
  }

  fun initWith (background: TGBackground?, listener: WallpaperParametersListener?) {
    isBlurEnabled.setValue(background != null && background.isBlurred(), false)
    isInitialBlur = isBlurEnabled.getValue()
    this.listener = listener
  }

  fun updateBackground (background: TGBackground) {
    isBlurEnabled.setValue(background.isBlurred(), true)
    isInitialBlur = isBlurEnabled.getValue()
  }

  fun setParametersAvailability (value: Boolean, animate: Boolean) {
    isViewShown.setValue(value, animate)
  }

  fun isBlurred (): Boolean {
    return isBlurEnabled.getValue()
  }

  override fun onDraw (c: Canvas) {
    drawButton(c, width / 2, height / 2, blurRect, Lang.getString(R.string.ChatBackgroundBlur), isBlurEnabled)
  }

  private fun drawButton (c: Canvas, centerX: Int, centerY: Int, buttonRect: RectF, text: String, selectAnimator: BoolAnimator) {
    val textWidth = textPaint.measureText(text)
    val checkboxScale = .75f
    val checkboxSize = SimplestCheckBox.size() * checkboxScale
    val offset = (textWidth / 2) - checkboxSize
    val checkboxX = centerX - checkboxSize.toInt() / 2 - Screen.dp(8f) + (Screen.dp(2f) * checkboxScale).toInt() - offset.toInt()
    val checkboxY = centerY - (Screen.dp(2f) * checkboxScale).toInt()

    buttonRect.top = checkboxY - checkboxSize
    buttonRect.bottom = checkboxY + checkboxSize
    buttonRect.left = checkboxX - checkboxSize
    buttonRect.right = centerX + textWidth + (checkboxSize / 1.5).toInt() - offset
    c.drawRoundRect(buttonRect, Screen.dp(16f).toFloat(), Screen.dp(16f).toFloat(), Paints.fillingPaint(Theme.getColor(ColorId.previewBackground)))

    c.drawText(text, centerX - offset, (centerY + Screen.sp(4f)).toFloat(), textPaint)

    c.save()
    c.scale(checkboxScale, checkboxScale, checkboxX.toFloat(), centerY.toFloat())
    c.drawCircle(checkboxX.toFloat(), checkboxY.toFloat(), checkboxSize / 2, Paints.getProgressPaint(Theme.getColor(ColorId.text), Screen.dp(2f).toFloat()))
    SimplestCheckBox.draw(c, checkboxX, checkboxY, selectAnimator.getFloatValue(), null)
    c.restore()
  }

  override fun onTouchEvent (event: MotionEvent): Boolean {
    return helper.onTouchEvent(this, event)
  }

  override fun needClickAt (view: View, x: Float, y: Float): Boolean {
    return blurRect.contains(x, y)
  }

  override fun onClickAt (view: View, x: Float, y: Float) {
    if (blurRect.contains(x, y)) {
      isBlurEnabled.toggleValue(true)
      listener?.onBlurValueChanged(isBlurred())
    }
  }

  override fun performDestroy () {
    ThemeManager.instance().removeChatStyleListener(this)
  }

  override fun onChatStyleChanged (tdlib: Tdlib, newChatStyle: Int) {

  }

  override fun onChatWallpaperChanged (tdlib: Tdlib, wallpaper: TGBackground?, usageIdentifier: Int) {
    setParametersAvailability(wallpaper != null && wallpaper.isWallpaper(), true)
  }

  interface WallpaperParametersListener {
    fun onBlurValueAnimated (factor: Float) { }
    fun onBlurValueChanged (newValue: Boolean) { }
    fun onParametersViewScaleChanged (factor: Float) { }
  }
}
