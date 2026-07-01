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
 * File created on 21/12/2019
 */
package org.thunderdog.challegram.telegram

import androidx.annotation.UiThread
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import org.thunderdog.challegram.tool.UI
import me.vkryl.core.lambda.Destroyable

abstract class ListManager<T>(
  @JvmField protected val tdlib: Tdlib,
  @JvmField protected val initialLoadCount: Int,
  @JvmField protected val loadCount: Int,
  canLoadInReverseDirection: Boolean,
  listener: ListChangeListener<T>?
) : Destroyable, Iterable<T>, TdlibProvider {

  @JvmField
  protected val items: MutableList<T> = ArrayList()

  protected class Response<T>(@JvmField val items: List<T>, @JvmField val totalCount: Int)

  private var state = STATE_INITIALIZING
  private var isLoading = false
  private var endReached = false
  private var reverseEndReached = !canLoadInReverseDirection
  private var totalCountValue = COUNT_UNKNOWN
  private var listAvailable = false

  private val changeListeners: MutableList<ListChangeListener<T>> = ArrayList()

  init {
    if (listener != null) {
      addChangeListener(listener)
    }
    subscribeToUpdates()
  }

  protected abstract fun subscribeToUpdates()
  protected abstract fun unsubscribeFromUpdates()

  protected abstract fun nextLoadFunction(reverse: Boolean, itemCount: Int, loadCount: Int): TdApi.Function<*>

  override fun tdlib(): Tdlib {
    return tdlib
  }

  protected open fun loadTotalCount(after: Runnable?) {
    // Implement in the child if the count may desync from the list size
  }

  @UiThread
  fun loadInitialChunk(after: Runnable?) {
    if (items.isEmpty()) {
      loadItems(false, after)
    } else {
      after?.run()
    }
  }

  @UiThread
  fun loadItems(reverse: Boolean, after: Runnable?) {
    loadItems(if (items.isEmpty()) initialLoadCount else loadCount, reverse, after)
  }

  @UiThread
  fun loadAll() {
    loadItems(if (items.isEmpty()) initialLoadCount else loadCount, false, object : Runnable {
      override fun run() {
        if (!isEndReached()) {
          loadItems(loadCount, false, this)
        }
      }
    })
  }

  @UiThread
  private fun loadItems(count: Int, reverse: Boolean, after: Runnable?) {
    if (isLoading || state == STATE_FULL || state == STATE_DESTROYED || (if (reverse) reverseEndReached else endReached)) {
      return
    }
    isLoading = true
    tdlib.client().send(nextLoadFunction(reverse, items.size, count), object : Client.ResultHandler {
      override fun onResult(result: TdApi.Object) {
        if (result.getConstructor() == TdApi.Error.CONSTRUCTOR) {
          UI.showError(result)
          return
        }
        val data = processResponse(result, this, count, reverse)
        if (data != null) {
          runOnUiThread {
            processData(data, reverse)
            after?.run()
          }
        }
      }
    })
  }

  protected fun indexOfItem(item: T): Int {
    var index = 0
    for (existingItem in items) {
      if (existingItem == item) {
        return index
      }
      index++
    }
    return -1
  }

  protected fun isReady(): Boolean {
    return state == STATE_INITIALIZED || state == STATE_FULL
  }

  protected fun isEndReached(): Boolean {
    return state == STATE_FULL
  }

  protected fun isDestroyed(): Boolean {
    return state == STATE_DESTROYED
  }

  override fun iterator(): Iterator<T> {
    return items.iterator()
  }

  fun getItem(index: Int): T {
    return items[index]
  }

  fun getCount(): Int {
    return items.size
  }

  fun getTotalCount(): Int {
    if (totalCountValue == COUNT_UNKNOWN) {
      return COUNT_UNKNOWN
    }
    if (reverseEndReached && endReached) {
      return items.size
    }
    return Math.max(totalCountValue, items.size)
  }

  fun hasReceivedInitialChunk(): Boolean {
    return state != STATE_INITIALIZING
  }

  fun isAvailable(): Boolean {
    return isListAvailable()
  }

  // Return null means retryHandler will be called again
  protected abstract fun processResponse(response: TdApi.Object, retryHandler: Client.ResultHandler, retryLoadCount: Int, reverse: Boolean): Response<T>?

  @UiThread
  private fun processData(data: Response<T>, reverse: Boolean) {
    if (isDestroyed()) {
      return
    }
    isLoading = false
    val wasInitialized = state != STATE_INITIALIZING
    if (data.items.isEmpty()) {
      if (reverse) {
        reverseEndReached = true
      } else {
        endReached = true
      }
      if ((endReached && reverseEndReached) || items.size == data.totalCount) {
        state = STATE_FULL
      }
      onEndReached(reverse)
    } else {
      state = if (items.size + data.items.size == totalCountValue) STATE_FULL else STATE_INITIALIZED
      val startIndex = items.size
      if (startIndex == 0 || !reverse) {
        items.addAll(data.items)
        onItemsAdded(data.items, startIndex, wasInitialized)
      } else {
        items.addAll(0, data.items)
        onItemsAdded(data.items, 0, wasInitialized)
      }
    }
    if (data.totalCount != COUNT_UNKNOWN) {
      setTotalCount(data.totalCount)
    }
    if (!wasInitialized) {
      notifyInitialChunkReceived()
    }
  }

  @UiThread
  override fun performDestroy() {
    if (state != STATE_DESTROYED) {
      unsubscribeFromUpdates()
      state = STATE_DESTROYED
    }
  }

  protected fun runOnUiThreadIfReady(act: Runnable) {
    tdlib.ui().post {
      if (isReady()) {
        act.run()
      }
    }
  }

  protected fun runOnUiThread(act: Runnable) {
    tdlib.ui().post {
      if (!isDestroyed()) {
        act.run()
      }
    }
  }

  // List availability check

  protected fun checkListAvailability(): Boolean {
    val isAvailable = items.isNotEmpty() || totalCountValue > 0
    val availabilityChanged = listAvailable != isAvailable
    if (availabilityChanged) {
      listAvailable = isAvailable
      notifyAvailabilityChanged(isAvailable)
    }
    if (items.size < initialLoadCount) {
      loadItems(false, null)
    }
    return availabilityChanged
  }

  protected fun isListAvailable(): Boolean {
    return listAvailable
  }

  protected fun setTotalCount(count: Int): Boolean {
    if (totalCountValue != count) {
      totalCountValue = count
      if (count > items.size && state == STATE_FULL) {
        state = STATE_INITIALIZED
      }
      notifyTotalCountChanged(count)
      return checkListAvailability()
    }
    return false
  }

  protected fun changeTotalCount(delta: Int): Boolean {
    if (totalCountValue != COUNT_UNKNOWN && delta != 0) {
      if (totalCountValue + delta < 0) {
        val changed = setTotalCount(items.size)
        loadTotalCount(null)
        return changed
      }
      return setTotalCount(totalCountValue + delta)
    }
    return false
  }

  // List state notifications

  @JvmSuppressWildcards
  interface ListChangeListener<T> {
    fun onItemsAdded(list: ListManager<T>, items: List<T>, startIndex: Int, isInitialChunk: Boolean) {
      onListChanged(list)
    }
    fun onItemAdded(list: ListManager<T>, item: T, toIndex: Int) {
      onListChanged(list)
    }
    fun onItemMoved(list: ListManager<T>, item: T, fromIndex: Int, toIndex: Int) {
      onListChanged(list)
    }
    fun onItemRemoved(list: ListManager<T>, removedItem: T, fromIndex: Int) {
      onListChanged(list)
    }
    fun onItemChanged(list: ListManager<T>, item: T, index: Int, cause: Int) {
      onListChanged(list)
    }
    fun onListMetadataChanged(list: ListManager<T>) {
      onListChanged(list)
    }

    fun onAvailabilityChanged(list: ListManager<T>, isAvailable: Boolean) {}
    fun onTotalCountChanged(list: ListManager<T>, totalCount: Int) {}
    fun onInitialChunkLoaded(list: ListManager<T>) {}
    fun onListChanged(list: ListManager<T>) {}
  }

  fun addChangeListener(listener: ListChangeListener<T>) {
    changeListeners.add(listener)
  }

  fun removeChangeListener(listener: ListChangeListener<T>) {
    changeListeners.remove(listener)
  }

  protected fun notifyItemChanged(index: Int, cause: Int) {
    val item = items[index]
    for (i in changeListeners.size - 1 downTo 0) {
      changeListeners[i].onItemChanged(this, item, index, cause)
    }
  }

  protected fun notifyMetadataChanged() {
    for (i in changeListeners.size - 1 downTo 0) {
      changeListeners[i].onListMetadataChanged(this)
    }
  }

  private fun notifyAvailabilityChanged(isAvailable: Boolean) {
    for (i in changeListeners.size - 1 downTo 0) {
      changeListeners[i].onAvailabilityChanged(this, isAvailable)
    }
  }

  private fun notifyInitialChunkReceived() {
    for (i in changeListeners.size - 1 downTo 0) {
      changeListeners[i].onInitialChunkLoaded(this)
    }
  }

  private fun notifyTotalCountChanged(totalCount: Int) {
    for (i in changeListeners.size - 1 downTo 0) {
      changeListeners[i].onTotalCountChanged(this, totalCount)
    }
  }

  @UiThread
  protected fun onItemsAdded(items: List<T>, startIndex: Int, isInitialChunk: Boolean) {
    for (i in changeListeners.size - 1 downTo 0) {
      changeListeners[i].onItemsAdded(this, items, startIndex, isInitialChunk)
    }
    checkListAvailability()
  }

  @UiThread
  protected fun onItemAdded(item: T, toIndex: Int) {
    for (i in changeListeners.size - 1 downTo 0) {
      changeListeners[i].onItemAdded(this, item, toIndex)
    }
    checkListAvailability()
  }

  @UiThread
  protected fun onItemMoved(item: T, fromIndex: Int, toIndex: Int) {
    for (i in changeListeners.size - 1 downTo 0) {
      changeListeners[i].onItemMoved(this, item, fromIndex, toIndex)
    }
  }

  @UiThread
  protected fun onItemRemoved(removedItem: T, fromIndex: Int) {
    checkListAvailability()
    for (i in changeListeners.size - 1 downTo 0) {
      changeListeners[i].onItemRemoved(this, removedItem, fromIndex)
    }
  }

  @UiThread
  protected fun onEndReached(reverse: Boolean) {
    // TODO notify?
  }

  companion object {
    private const val STATE_INITIALIZING = 0
    private const val STATE_INITIALIZED = 1
    private const val STATE_FULL = 2
    private const val STATE_DESTROYED = 3

    const val COUNT_UNKNOWN = -1
  }
}
