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

import androidx.annotation.Keep
import org.drinkless.tdlib.TdApi

@Keep
class Socks5Proxy (
  @JvmField val host: String?,
  @JvmField val port: Int,
  @JvmField val username: String?,
  @JvmField val password: String?
) {
  constructor (proxy: TdApi.Proxy) : this(proxy, asSocks5(proxy))

  private constructor (proxy: TdApi.Proxy, socks5: TdApi.ProxyTypeSocks5) : this(
    proxy.server,
    proxy.port,
    socks5.username,
    socks5.password
  )

  companion object {
    private fun asSocks5 (proxy: TdApi.Proxy): TdApi.ProxyTypeSocks5 {
      require(proxy.type.getConstructor() == TdApi.ProxyTypeSocks5.CONSTRUCTOR) { proxy.type.toString() }
      return proxy.type as TdApi.ProxyTypeSocks5
    }
  }
}
