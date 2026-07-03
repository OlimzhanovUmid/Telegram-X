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
 * File created on 24/08/2023
 */
package org.thunderdog.challegram.widget.emoji.header

import android.content.Context
import android.view.View
import android.view.ViewGroup
import org.thunderdog.challegram.R
import org.thunderdog.challegram.navigation.ViewController
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.widget.emoji.EmojiLayoutRecyclerController
import org.thunderdog.challegram.widget.emoji.section.EmojiSection
import org.thunderdog.challegram.widget.emoji.section.EmojiSectionView
import java.util.ArrayList
import me.vkryl.android.widget.FrameLayoutFix

class EmojiHeaderViewNonPremium (context: Context) : FrameLayoutFix(context) {
  private val emojiSections = ArrayList<EmojiSection>(9)
  private val emojiSectionViews = ArrayList<EmojiSectionView>(9)
  private var currentSelectedIndex = -1
  private var allowMedia = false

  fun init (emojiLayout: EmojiLayoutRecyclerController.Callback, themeProvider: ViewController<*>?, allowMedia: Boolean) {
    this.allowMedia = allowMedia

    emojiSections.add(EmojiSection(emojiLayout, EmojiSection.SECTION_EMOJI_RECENT, R.drawable.baseline_access_time_24, R.drawable.baseline_watch_later_24)/*.setFactor(1f, false)*/.setMakeFirstTransparent().setOffsetHalf(false))
    emojiSections.add(EmojiSection(emojiLayout, EmojiSection.SECTION_EMOJI_SMILEYS, R.drawable.baseline_emoticon_outline_24, R.drawable.baseline_emoticon_24).setMakeFirstTransparent())
    emojiSections.add(EmojiSection(emojiLayout, EmojiSection.SECTION_EMOJI_ANIMALS, R.drawable.deproko_baseline_animals_outline_24, R.drawable.deproko_baseline_animals_24)) /*.setIsPanda(!useDarkMode)*/
    emojiSections.add(EmojiSection(emojiLayout, EmojiSection.SECTION_EMOJI_FOOD, R.drawable.baseline_restaurant_menu_24, R.drawable.baseline_restaurant_menu_24))
    emojiSections.add(EmojiSection(emojiLayout, EmojiSection.SECTION_EMOJI_TRAVEL, R.drawable.baseline_directions_car_24, R.drawable.baseline_directions_car_24))
    emojiSections.add(EmojiSection(emojiLayout, EmojiSection.SECTION_EMOJI_SYMBOLS, R.drawable.deproko_baseline_lamp_24, R.drawable.deproko_baseline_lamp_filled_24))
    emojiSections.add(EmojiSection(emojiLayout, EmojiSection.SECTION_EMOJI_FLAGS, R.drawable.deproko_baseline_flag_outline_24, R.drawable.deproko_baseline_flag_filled_24).setMakeFirstTransparent())
    if (allowMedia) {
      emojiSections.add(EmojiSection(emojiLayout, EmojiSection.SECTION_SWITCH_TO_MEDIA, R.drawable.deproko_baseline_stickers_24, 0).setActiveDisabled())
    }
    emojiSectionViews.clear()
    for (a in emojiSections.indices) {
      val emojiSectionView = EmojiSectionView(context)
      emojiSectionView.setSection(emojiSections[a])
      emojiSectionView.id = R.id.btn_section
      themeProvider?.addThemeInvalidateListener(emojiSectionView)
      addView(emojiSectionView, FrameLayoutFix.newParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT))
      emojiSectionViews.add(emojiSectionView)
    }
  }

  fun setMediaSection (isGif: Boolean) {
    if (allowMedia) {
      emojiSections[emojiSections.size - 1].changeIcon(if (isGif) R.drawable.deproko_baseline_gif_24 else R.drawable.deproko_baseline_stickers_24)
    }
  }

  fun setSelectedIndex (index: Int, animated: Boolean) {
    if (index == currentSelectedIndex) {
      return
    }
    if (currentSelectedIndex != -1) {
      emojiSections[currentSelectedIndex].setFactor(0f, animated)
    }
    if (index >= 0 && index < emojiSections.size) {
      this.currentSelectedIndex = index
      emojiSections[currentSelectedIndex].setFactor(1f, animated)
    }
  }

  override fun setOnClickListener (onClickListener: View.OnClickListener?) {
    for (view in emojiSectionViews) {
      view.setOnClickListener(onClickListener)
    }
  }

  override fun setOnLongClickListener (onLongClickListener: View.OnLongClickListener?) {
    for (view in emojiSectionViews) {
      view.setOnLongClickListener(onLongClickListener)
    }
  }

  protected override fun onMeasure (widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    updatePositions()
  }

  private fun updatePositions () {
    val itemCount = emojiSections.size
    val itemWidth = (measuredWidth - Screen.dp(EmojiHeaderView.DEFAULT_PADDING * 2f)).toFloat() / itemCount
    val itemOffset = (itemWidth - Screen.dp(44f)) / 2f
    val itemPadding = (measuredWidth - Screen.dp((EmojiHeaderView.DEFAULT_PADDING * 2).toFloat()) - itemWidth * itemCount) / (itemCount - 1)

    for (a in 0 until itemCount) {
      val v = emojiSectionViews[a]
      v.translationX = (Screen.dp(EmojiHeaderView.DEFAULT_PADDING.toFloat()) + itemOffset + (itemWidth + itemPadding) * a).toInt().toFloat()
    }
  }
}
