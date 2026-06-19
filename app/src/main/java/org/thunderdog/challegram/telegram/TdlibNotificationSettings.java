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
 * File created on 25/12/2018
 */
package org.thunderdog.challegram.telegram;

import android.app.Notification;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class TdlibNotificationSettings {
  private final int vibrateMode, ledColor, priority;
  @Nullable
  private final String soundPath; // null means default, empty means none, otherwise custom

  public TdlibNotificationSettings (Tdlib tdlib, long settingsChatId, TdlibNotificationGroup group) {
    // Unused because of notification channels
    this.vibrateMode = this.ledColor = this.priority = 0;
    this.soundPath = null;
  }

  public static void applyStatic (NotificationCompat.Builder b, Tdlib tdlib, TdlibNotificationGroup group, boolean isSummary) {
    // Log.e("notification applyStatic isSummary:%b", isSummary);
    long settingsChatId = group.getChatId();
    if (group.isMention()) {
      long senderChatId = group.lastNotification().findSenderId();
      if (senderChatId != 0) {
        settingsChatId = senderChatId;
      }
    }
    int priority = tdlib.notifications().getEffectivePriorityOrImportance(settingsChatId);
    b.setPriority(priority);
  }

  public void apply (NotificationCompat.Builder b, boolean isSummary) {
    b.setPriority(priority);
    // Unused because of notification channels
  }
}
