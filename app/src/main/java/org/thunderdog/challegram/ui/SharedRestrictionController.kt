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
 * File created on 19/09/2019
 */
package org.thunderdog.challegram.ui

import android.content.Context
import android.view.View
import org.drinkless.tdlib.TdApi
import org.thunderdog.challegram.R
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.data.InlineResult
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.widget.EmptySmartView

class SharedRestrictionController (context: Context, tdlib: Tdlib) : SharedBaseController<InlineResult<*>>(context, tdlib) {
  private var restrictionReason: String? = null

  fun setRestrictionReason (restrictionReason: String?): SharedRestrictionController {
    this.restrictionReason = restrictionReason
    return this
  }

  protected override fun supportsMessageContent (): Boolean {
    return false
  }

  protected override val emptySmartMode: Int
    get() = EmptySmartView.MODE_EMPTY_RESTRICTED

  protected override val emptySmartArgument: String?
    get() = restrictionReason

  protected override val isAlwaysEmpty: Boolean
    get() = true

  protected override fun buildTotalCount (data: ArrayList<InlineResult<*>?>?): CharSequence? {
    return null
  }

  override fun getName (): CharSequence = Lang.getString(R.string.TabMedia)

  override val icon: Int
    get() = R.drawable.baseline_image_24

  protected override fun parseObject (`object`: TdApi.Object?): InlineResult<*>? {
    return null
  }

  protected override fun buildRequest (chatId: Long, topicId: TdApi.MessageTopic?, query: String?, offset: Long, secretOffset: String?, limit: Int): TdApi.Function<*>? {
    return null
  }

  protected override fun provideViewType (): Int {
    return 0
  }

  override fun onClick (v: View) {

  }
}
