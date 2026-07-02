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
 * File created on 28/03/2023
 */
package org.thunderdog.challegram.voip

class NetworkStats @JvmOverloads constructor (
  @JvmField var bytesSentWifi: Long = 0,
  @JvmField var bytesRecvdWifi: Long = 0,
  @JvmField var bytesSentMobile: Long = 0,
  @JvmField var bytesRecvdMobile: Long = 0
) {
  override fun toString (): String {
    return "Stats{" +
      "bytesRecvdMobile=" + bytesRecvdMobile +
      ", bytesSentWifi=" + bytesSentWifi +
      ", bytesRecvdWifi=" + bytesRecvdWifi +
      ", bytesSentMobile=" + bytesSentMobile +
      '}'
  }
}
