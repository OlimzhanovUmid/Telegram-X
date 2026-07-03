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
 * File created on 10/12/2017
 */
package org.thunderdog.challegram.player

import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View
import org.thunderdog.challegram.tool.Views

class RecordBackgroundView (context: Context) : View(context) {
  private var pivotX = -1
  private var pivotY = -1

  private var factor = -1f

  fun setFactor (factor: Float) {
    if (this.factor != factor) {
      this.factor = factor
      // invalidate();
      alpha = factor
    }
  }

  override fun onTouchEvent (event: MotionEvent): Boolean {
    return Views.isValid(this) && (onClickListener == null || super.onTouchEvent(event))
  }

  private var onClickListener: View.OnClickListener? = null

  override fun setOnClickListener (l: View.OnClickListener?) {
    super.setOnClickListener(l)
    this.onClickListener = l
  }

  fun setPivot (x: Int, y: Int) {
    if (this.pivotX != x || this.pivotY != y) {
      this.pivotX = x
      this.pivotY = y

      if (factor != 0f && factor != 1f) {
        // invalidate();
      }
    }
  }

  override fun onDraw (c: Canvas) {
    /*if (factor > 0f) {
      int color = Utils.alphaColor(factor, Theme.getColor(ColorId.overlay));
      int halfColor = Utils.alphaColor(.5f, color);
      c.drawColor(halfColor);
      if (factor == 1f) {
        c.drawColor(halfColor);
      } else {
        int viewWidth = getMeasuredWidth();
        int viewHeight = getMeasuredHeight();

        float radius = (float) Math.sqrt(viewWidth * viewWidth + viewHeight * viewHeight) * .5f; // * Math.max((float) pivotX / (float) viewWidth, (float) pivotY / (float) viewHeight);

        int toX = viewWidth / 2;
        int toY = viewHeight / 2;

        float x = (pivotX + (toX - pivotX) * factor);
        float y = (pivotY + (toY - pivotY) * factor);

        c.drawCircle(x, y, radius * factor, Paints.fillingPaint(halfColor));

        // c.drawCircle(pivotX, pivotY, radius * factor, Paints.fillingPaint(halfColor));
      }
    }*/
  }
}
