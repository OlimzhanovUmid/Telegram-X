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
 * File created on 27/03/2023
 */
package org.thunderdog.challegram.voip.annotation

import androidx.annotation.IntDef
import kotlin.annotation.AnnotationRetention
import kotlin.annotation.Retention

@Retention(AnnotationRetention.SOURCE)
@IntDef(
  DataSavingOption.NEVER,
  DataSavingOption.MOBILE,
  DataSavingOption.ALWAYS,
  DataSavingOption.ROAMING
)
annotation class DataSavingOption {
  companion object {
    // enum from VoIPController.h:93
    const val NEVER = 0
    const val MOBILE = 1
    const val ALWAYS = 2
    const val ROAMING = 3 /*this field is not present in VoIPController.h and is converted to MOBILE or NEVER*/
  }
}
