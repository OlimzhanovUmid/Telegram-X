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
 * File created on 21/10/2016
 */
package org.thunderdog.challegram.component.attach

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.annotation.Nullable
import org.drinkless.tdlib.TdApi
import org.thunderdog.challegram.Log
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.tool.UI
import org.thunderdog.challegram.util.CancellableResultHandler
import tgx.td.toUserId

class MediaLocationFinder private constructor() {
  private val manager: LocationManager?

  init {
    var locationManager: LocationManager? = null
    try {
      locationManager = UI.getAppContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager?
    } catch (t: Throwable) {
      Log.e("LocationService is unavailable", t)
    }
    manager = locationManager
  }

  fun getLastKnownLocation (): Location? {
    val locationManager = manager ?: return null
    if (UI.getAppContext().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
      return null
    }
    val providers = locationManager.getProviders(true)
    for (i in providers.size - 1 downTo 0) {
      val location = locationManager.getLastKnownLocation(providers[i])
      if (location != null) {
        return Location(location)
      }
    }
    return null
  }

  interface Callback {
    fun onNearbyPlacesLoaded (call: CancellableResultHandler, location: Location, queryId: Long, result: MutableList<MediaLocationData>, @Nullable nextOffset: String?)
    fun onNearbyPlacesErrorLoading (call: CancellableResultHandler, location: Location, error: TdApi.Error)
  }

  companion object {
    private var instance: MediaLocationFinder? = null

    @JvmStatic
    fun instance (): MediaLocationFinder {
      if (instance == null) {
        instance = MediaLocationFinder()
      }
      return instance!!
    }

    @JvmStatic
    fun findNearbyPlaces (tdlib: Tdlib, chatId: Long, location: Location, @Nullable q: String?, callback: Callback): CancellableResultHandler {
      val userLocation = TdApi.Location(location.latitude, location.longitude, location.accuracy.toDouble())

      val handler = object : CancellableResultHandler() {
        override fun processResult (object_: TdApi.Object) {
          when (object_.constructor) {
            TdApi.Chat.CONSTRUCTOR -> {
              val chat = object_ as TdApi.Chat
              tdlib.client().send(TdApi.GetInlineQueryResults(toUserId(chat.id), chatId, userLocation, q, null), this)
            }
            TdApi.InlineQueryResults.CONSTRUCTOR -> {
              val results = object_ as TdApi.InlineQueryResults
              val list = ArrayList<MediaLocationData>(results.results.size)
              for (result in results.results) {
                if (result.constructor == TdApi.InlineQueryResultVenue.CONSTRUCTOR) {
                  list.add(MediaLocationData(tdlib, result as TdApi.InlineQueryResultVenue, userLocation))
                }
              }
              tdlib.ui().post { callback.onNearbyPlacesLoaded(this, location, results.inlineQueryId, list, results.nextOffset) }
            }
            TdApi.Error.CONSTRUCTOR -> {
              tdlib.ui().post { callback.onNearbyPlacesErrorLoading(this, location, object_ as TdApi.Error) }
            }
          }
        }
      }

      tdlib.client().send(TdApi.SearchPublicChat(tdlib.venueSearchBotUsername), handler)
      return handler
    }
  }
}
