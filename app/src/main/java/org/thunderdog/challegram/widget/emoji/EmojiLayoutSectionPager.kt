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

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import me.vkryl.android.DECELERATE_INTERPOLATOR
import me.vkryl.android.animator.FactorAnimator
import org.thunderdog.challegram.core.Lang

abstract class EmojiLayoutSectionPager (context: Context) : FrameLayout(context), FactorAnimator.Target {
  companion object {
    private const val CHANGE_SECTION_ANIMATOR = 0
  }

  private lateinit var currentSectionView: View
  private var nextSection = -1
  private var nextSectionView: View? = null
  private var sectionIsLeft = false
  private var currentSection = 0

  private var sectionAnimator: FactorAnimator? = null
  private var sectionChangeFactor = 0f

  fun init (currentSection: Int) {
    this.currentSection = currentSection
    this.currentSectionView = getSectionView(currentSection)
    addView(currentSectionView)
  }

  fun getCurrentSection (): Int {
    return currentSection
  }

  fun getNextSection (): Int {
    return currentSection
  }

  fun canChangeSection (): Boolean {
    val animator = sectionAnimator
    return animator == null || (!animator.isAnimating() && animator.getFactor() == 0f && sectionChangeFactor == 0f)
  }

  fun isAnimationNotActive (): Boolean {
    val animator = sectionAnimator
    return animator == null || !animator.isAnimating()
  }

  fun isSectionStable (): Boolean {
    val animator = sectionAnimator
    return animator == null || animator.getFactor() == 0f
  }

  fun changeSection (sectionId: Int, fromLeft: Boolean, stickerSetSection: Int): Boolean {
    if (currentSection == sectionId || !canChangeSection()) {
      return false
    }

    val sectionView = getSectionView(sectionId)

    this.nextSection = sectionId
    this.nextSectionView = sectionView
    this.sectionIsLeft = fromLeft

    this.addView(sectionView)

    var animator = this.sectionAnimator
    if (animator == null) {
      animator = FactorAnimator(CHANGE_SECTION_ANIMATOR, this, DECELERATE_INTERPOLATOR, 180L)
      this.sectionAnimator = animator
    }

    animator.animateTo(1f)
    this.onSectionChangeStart(currentSection, nextSection, stickerSetSection)

    return true
  }

  private fun updatePositions () {
    if (sectionIsLeft != Lang.rtl()) {
      currentSectionView.translationX = currentSectionView.measuredWidth.toFloat() * sectionChangeFactor
      val next = nextSectionView
      if (next != null) {
        next.translationX = (-next.measuredWidth).toFloat() * (1f - sectionChangeFactor)
      }
    } else {
      currentSectionView.translationX = (-currentSectionView.measuredWidth).toFloat() * sectionChangeFactor
      val next = nextSectionView
      if (next != null) {
        next.translationX = next.measuredWidth.toFloat() * (1f - sectionChangeFactor)
      }
    }
  }

  private fun applySection () {
    removeView(currentSectionView)

    val oldSection = this.currentSection

    currentSection = nextSection
    nextSection = -1
    currentSectionView = nextSectionView!!
    nextSectionView = null
    sectionAnimator!!.forceFactor(0f)
    sectionChangeFactor = 0f

    this.onSectionChangeEnd(oldSection, currentSection)
  }

  override fun onFactorChanged (id: Int, factor: Float, fraction: Float, callee: FactorAnimator?) {
    if (id == CHANGE_SECTION_ANIMATOR) {
      this.sectionChangeFactor = factor
      updatePositions()
    }
  }

  override fun onFactorChangeFinished (id: Int, finalFactor: Float, callee: FactorAnimator?) {
    if (id == CHANGE_SECTION_ANIMATOR) {
      if (finalFactor == 1f) {
        applySection()
      }
    }
  }

  protected override fun onMeasure (widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    updatePositions()
  }

  protected abstract fun getSectionView (section: Int): View
  protected abstract fun onSectionChangeStart (prevSection: Int, nextSection: Int, stickerSetSection: Int)
  protected abstract fun onSectionChangeEnd (prevSection: Int, currentSection: Int)
}
