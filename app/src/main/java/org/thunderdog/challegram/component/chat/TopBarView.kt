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
package org.thunderdog.challegram.component.chat

import android.content.Context
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout

import org.thunderdog.challegram.R
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.navigation.ViewController
import org.thunderdog.challegram.support.ViewSupport
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.tool.Views

import me.vkryl.android.widget.FrameLayoutFix

class TopBarView (context: Context) : FrameLayoutFix(context) {
  private val topDismissButton: ImageView
  private val actionsList: LinearLayout

  private var canDismiss = false

  fun interface DismissListener {
    fun onDismissRequest (barView: TopBarView?)
  }

  class Item (val id: Int, val stringRes: Int, val onClickListener: View.OnClickListener) {
    var isNegative = false
    var noDismiss = false

    fun setIsNegative (): Item {
      this.isNegative = true
      return this
    }

    fun setNoDismiss (): Item {
      this.noDismiss = true
      return this
    }
  }

  private var dismissListener: DismissListener? = null

  init {
    layoutParams = FrameLayoutFix.newParams(ViewGroup.LayoutParams.MATCH_PARENT, Screen.dp(36f))
    ViewSupport.setThemedBackground(this, ColorId.filling, null)

    actionsList = LinearLayout(context)
    actionsList.orientation = LinearLayout.HORIZONTAL
    actionsList.layoutParams = FrameLayoutFix.newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Lang.gravity() or Gravity.TOP)
    addView(actionsList)

    topDismissButton = object : ImageView(context) {
      override fun onTouchEvent (event: MotionEvent): Boolean {
        return Views.isValid(this) && super.onTouchEvent(event)
      }
    }
    topDismissButton.setOnClickListener {
      dismissListener?.onDismissRequest(this)
    }
    topDismissButton.scaleType = ImageView.ScaleType.CENTER
    topDismissButton.setColorFilter(Theme.iconColor())
    topDismissButton.setImageResource(R.drawable.baseline_close_18)
    topDismissButton.layoutParams = FrameLayoutFix.newParams(Screen.dp(40f), ViewGroup.LayoutParams.MATCH_PARENT, Lang.gravity() or Gravity.TOP)
    topDismissButton.setBackgroundResource(R.drawable.bg_btn_header)
    Views.setClickable(topDismissButton)
    topDismissButton.visibility = View.INVISIBLE
    addView(topDismissButton)
  }

  fun setDismissListener (dismissListener: DismissListener?) {
    this.dismissListener = dismissListener
  }

  private var themeProvider: ViewController<*>? = null

  fun addThemeListeners (themeProvider: ViewController<*>?) {
    this.themeProvider = themeProvider
    if (themeProvider != null) {
      themeProvider.addThemeFilterListener(topDismissButton, ColorId.icon)
      themeProvider.addThemeInvalidateListener(this)
    }
  }

  fun setCanDismiss (canDismiss: Boolean) {
    if (this.canDismiss != canDismiss) {
      this.canDismiss = canDismiss
      topDismissButton.visibility = if (canDismiss) View.VISIBLE else View.GONE
    }
  }

  fun setItems (vararg items: Item?) {
    for (i in 0 until actionsList.childCount) {
      val view = actionsList.getChildAt(i)
      if (view != null && themeProvider != null) {
        themeProvider!!.removeThemeListenerByTarget(view)
      }
    }
    actionsList.removeAllViews()
    if (items.size > 1) {
      val offsetView = View(context)
      offsetView.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, .75f)
      actionsList.addView(offsetView)
    }
    var canDismiss = false
    for (rawItem in items) {
      val item = rawItem!!
      if (!item.noDismiss) {
        canDismiss = true
      }
      val textColorId = if (item.isNegative) ColorId.textNegative else ColorId.textNeutral
      val button = Views.newTextView(context, 15f, Theme.getColor(textColorId), Gravity.CENTER, Views.TEXT_FLAG_BOLD or Views.TEXT_FLAG_HORIZONTAL_PADDING)
      button.id = item.id
      if (themeProvider != null) {
        themeProvider!!.addThemeTextColorListener(button, textColorId)
      }
      button.ellipsize = TextUtils.TruncateAt.END
      button.setSingleLine(true)
      button.setBackgroundResource(R.drawable.bg_btn_header)
      button.setOnClickListener(item.onClickListener)
      Views.setMediumText(button, Lang.getString(item.stringRes).uppercase())
      Views.setClickable(button)
      button.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT, 2f)
      actionsList.addView(button)
    }
    if (items.size > 1) {
      val offsetView = View(context)
      offsetView.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, .75f)
      actionsList.addView(offsetView)
    }
    setCanDismiss(canDismiss)
  }
}
