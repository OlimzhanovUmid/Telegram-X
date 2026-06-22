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
 * File created on 04/01/2019
 */
package org.thunderdog.challegram.telegram

import android.content.Context
import org.drinkless.tdlib.TdApi

interface TdlibNotificationStyleDelegate {
  fun displayNotificationGroup (context: Context, helper: TdlibNotificationHelper, badgeCount: Int, allowPreview: Boolean, group: TdlibNotificationGroup, settings: TdlibNotificationSettings?)
  fun rebuildNotificationsSilently (context: Context, helper: TdlibNotificationHelper, badgeCount: Int, allowPreview: Boolean, scope: TdApi.NotificationSettingsScope?, specificChatId: Long, specificGroupId: Int)
  fun hideNotificationGroup (context: Context, helper: TdlibNotificationHelper, badgeCount: Int, allowPreview: Boolean, group: TdlibNotificationGroup)
  fun hideAllNotifications (context: Context, helper: TdlibNotificationHelper, badgeCount: Int)
  fun cancelPendingMediaPreviewDownloads (context: Context, helper: TdlibNotificationHelper)
}
