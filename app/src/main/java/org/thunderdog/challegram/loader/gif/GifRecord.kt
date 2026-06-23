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
 * File created on 01/03/2016 at 12:14
 */
package org.thunderdog.challegram.loader.gif

class GifRecord(
  private var file: GifFile,
  private val actor: GifActor,
  reference: GifWatcherReference
) {
  private val watchers = ArrayList<GifWatcherReference>(2)

  init {
    watchers.add(reference)
  }

  fun getFile (): GifFile = file

  fun getActor (): GifActor = actor

  fun getWatchers (): ArrayList<GifWatcherReference> = watchers

  fun setFile (file: GifFile) {
    this.file = file
  }

  fun addWatcher (reference: GifWatcherReference): Boolean {
    if (watchers.contains(reference)) {
      return false
    }

    actor.watcherJoined(reference)
    watchers.add(reference)
    return true
  }

  fun removeWatcher (reference: GifWatcherReference): Boolean {
    if (!watchers.contains(reference)) {
      return false
    }

    watchers.remove(reference)
    return true
  }

  fun hasWatchers (): Boolean = watchers.isNotEmpty()
}
