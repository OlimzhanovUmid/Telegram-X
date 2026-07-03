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
 * File created on 25/08/2017
 */
package org.thunderdog.challegram.widget

import android.content.Context
import android.view.MotionEvent
import org.thunderdog.challegram.component.preview.FlingDetector
import org.thunderdog.challegram.navigation.ViewController
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.tool.UI

class BaseRootLayout (context: Context) : RootFrameLayout(context), FlingDetector.Callback {
  private val detector: FlingDetector = FlingDetector(context, this)

  override fun onInterceptTouchEvent (ev: MotionEvent): Boolean {
    lastTouchX = ev.x
    lastTouchY = ev.y
    if (mode == MODE_LISTENING) {
      if (ev.pointerCount > 1) {
        mode = MODE_NONE
      } else {
        when (ev.action) {
          MotionEvent.ACTION_MOVE -> {
            val diffY = touchStartY - lastTouchY
            val absDiffY = Math.abs(diffY)
            val threshold = Screen.getTouchSlopBig() * 2
            if ((diffY > 0 == dragDirectionDownUp) && absDiffY >= threshold && Math.abs(lastTouchX - touchStartX) < absDiffY) {
              if (UI.getContext(context).startCameraDrag(pendingCameraOptions, dragDirectionDownUp)) {
                mode = MODE_DRAGGING
                touchStartX = lastTouchX
                touchStartY = lastTouchY
                detector.onTouchEvent(MotionEvent.obtain(ev.downTime, ev.eventTime, MotionEvent.ACTION_DOWN, ev.x, ev.y, ev.metaState))
                return true
              }
              mode = MODE_NONE
            }
          }
          MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
            mode = MODE_NONE
        }
      }
    }
    return mode == MODE_DRAGGING || super.onInterceptTouchEvent(ev)
  }

  override fun onFling (velocityX: Float, velocityY: Float): Boolean {
    if (mode == MODE_DRAGGING) {
      if (Math.abs(velocityY) >= Screen.dp(10f)) {
        UI.getContext(context).dropCameraDrag(velocityY < 0, true)
        mode = MODE_NONE
        return true
      }
    }
    return false
  }

  override fun onTouchEvent (e: MotionEvent): Boolean {
    if (mode != MODE_DRAGGING) {
      return super.onTouchEvent(e)
    }
    detector.onTouchEvent(e)
    if (mode != MODE_DRAGGING) {
      return true
    }
    when (e.action) {
      MotionEvent.ACTION_MOVE -> {
        val touchY = e.y
        val factor = Math.max(0f, Math.min(1f, (if (dragDirectionDownUp) touchStartY - touchY else touchY - touchStartY) / measuredHeight.toFloat()))
        UI.getContext(context).setCameraDragFactor(if (dragDirectionDownUp) factor else 1f - factor)
      }
      MotionEvent.ACTION_UP -> {
        UI.getContext(context).dropCameraDrag()
        mode = MODE_NONE
      }
      MotionEvent.ACTION_CANCEL -> {
        UI.getContext(context).dropCameraDrag(false, false)
        mode = MODE_NONE
      }
    }
    return true
  }

  companion object {
    private const val MODE_NONE = 0
    private const val MODE_LISTENING = 1
    private const val MODE_DRAGGING = 2
  }

  private var mode = 0
  private var pendingCameraOptions: ViewController.CameraOpenOptions? = null
  private var lastTouchX = 0f
  private var lastTouchY = 0f
  private var touchStartX = 0f
  private var touchStartY = 0f
  private var dragDirectionDownUp = false // true if finger movement is bottom -> top

  fun prepareVerticalDrag (options: ViewController.CameraOpenOptions?, isOpen: Boolean) {
    if (mode == MODE_NONE) {
      this.mode = MODE_LISTENING
      if (isOpen) {
        this.pendingCameraOptions = options
      }
      this.dragDirectionDownUp = isOpen
      this.touchStartX = lastTouchX
      this.touchStartY = lastTouchY
    }
  }
}
