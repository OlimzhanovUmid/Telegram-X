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
 * File created on 18/08/2023
 */
package org.thunderdog.challegram.widget.emoji

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import org.thunderdog.challegram.R
import org.thunderdog.challegram.navigation.ViewController
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Paints
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.widget.emoji.section.EmojiSection
import org.thunderdog.challegram.widget.emoji.section.EmojiSectionView
import java.util.ArrayList
import me.vkryl.android.animator.BoolAnimator
import me.vkryl.android.animator.FactorAnimator
import me.vkryl.android.widget.FrameLayoutFix
import me.vkryl.android.DECELERATE_INTERPOLATOR
import me.vkryl.core.alphaColor
import me.vkryl.core.fromTo

@SuppressLint("ViewConstructor")
class EmojiHeaderCollapsibleSectionView (context: Context) : FrameLayout(context), FactorAnimator.Target {
  private val expandAnimator: BoolAnimator = BoolAnimator(0, this, DECELERATE_INTERPOLATOR, 220L)
  private val bgRect = RectF()
  private val emojiSectionsViews = ArrayList<EmojiSectionView>(6)
  private lateinit var emojiSections: ArrayList<EmojiSection>
  private var currentSelectedIndex = -1

  fun init (sections: ArrayList<EmojiSection>) {
    this.emojiSections = sections
    emojiSectionsViews.clear()
    for (emojiSection in sections) {
      val sectionView = EmojiSectionView(context)
      sectionView.id = R.id.btn_section
      sectionView.setSection(emojiSection)
      sectionView.layoutParams = FrameLayoutFix.newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
      sectionView.setForceWidth(Screen.dp(35f))
      addView(sectionView)
      emojiSectionsViews.add(sectionView)
    }
  }

  fun setOnButtonClickListener (listener: View.OnClickListener) {
    for (view in emojiSectionsViews) {
      view.setOnClickListener(listener)
    }
  }

  fun setThemeInvalidateListener (themeProvider: ViewController<*>?) {
    if (themeProvider != null) {
      for (view in emojiSectionsViews) {
        themeProvider.addThemeInvalidateListener(view)
      }
    }
  }

  fun setSelectedObject (section: EmojiSection?, animated: Boolean) {
    if (section != null) {
      for (i in emojiSections.indices) {
        if (emojiSections[i].index == section.index) {
          setSelectedIndex(i, animated)
          return
        }
      }
    }
    setSelectedIndex(-1, animated)
  }

  private fun setSelectedIndex (index: Int, animated: Boolean) {
    if (currentSelectedIndex >= 0) {
      emojiSections[currentSelectedIndex].setFactor(0f, animated)
    }
    currentSelectedIndex = index
    if (currentSelectedIndex >= 0) {
      emojiSections[currentSelectedIndex].setFactor(1f, animated)
    }

    expandAnimator.setValue(currentSelectedIndex >= 0, animated)
  }

  override fun onMeasure (widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val defaultWidth = Screen.dp(44f)
    val expandedWidth = Math.max(Screen.dp((35 * emojiSectionsViews.size + 9).toFloat()), defaultWidth)
    val width = fromTo(defaultWidth, expandedWidth, expandAnimator.getFloatValue())
    super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), heightMeasureSpec)
    updatePositions()
  }

  private fun updatePositions () {
    val factor = expandAnimator.getFloatValue()
    for (a in emojiSectionsViews.indices) {
      val view = emojiSectionsViews[a]
      view.setForceWidth(Screen.dp(35f))
      view.translationX = Screen.dp(4.5f + 35 * a).toFloat()
      if (a != 0) {
        view.alpha = factor
      }
    }
    bgRect.set(Screen.dp(2f).toFloat(), Screen.dp(4f).toFloat(), (measuredWidth - Screen.dp(2f)).toFloat(), (measuredHeight - Screen.dp(4f)).toFloat())
    invalidate()
  }

  override fun onFactorChanged (id: Int, factor: Float, fraction: Float, callee: FactorAnimator?) {
    requestLayout()
  }

  override fun dispatchDraw (canvas: Canvas) {
    canvas.drawRoundRect(bgRect, Screen.dp(20f).toFloat(), Screen.dp(20f).toFloat(),
      Paints.fillingPaint(alphaColor(expandAnimator.getFloatValue(), Theme.backgroundColor())))
    super.dispatchDraw(canvas)
  }
}
