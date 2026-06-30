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
 * File created on 18/10/2022, 01:17.
 */
package org.thunderdog.challegram.telegram

import androidx.annotation.UiThread
import me.vkryl.core.lambda.RunnableData
import me.vkryl.core.reference.ReferenceMap
import org.drinkless.tdlib.TdApi
import org.thunderdog.challegram.util.BatchOperationHandler

internal abstract class TdlibDataManager<Key : Any, Value : TdApi.Object, Result : TdlibDataManager.AbstractEntry<Key, Value>> protected constructor(@JvmField protected val tdlib: Tdlib) :
    CleanupStartupDelegate {
    internal abstract class AbstractEntry<K : Any, V : TdApi.Object>(val key: K, @JvmField val value: V?, val error: TdApi.Error?) {

      init {
        check(!(error == null && value == null))
        }

        val isNotFound: Boolean
            get() = error != null || value == null
    }

    internal fun interface Watcher<Key : Any, Value : TdApi.Object, Result : AbstractEntry<Key, Value>> {
        fun onEntryLoaded(context: TdlibDataManager<Key, Value, Result>, entry: Result)
    }

    protected abstract fun newEntry(key: Key, value: Value?, error: TdApi.Error?): Result?

  private val dataLock = Any()
    private val entries: MutableMap<Key, Result?> = HashMap<Key, Result?>()
    private val postponedKeys: MutableSet<Key> = HashSet<Key>()
    private val loadingKeys: MutableSet<Key> = HashSet<Key>()
    private val watcherReferences: ReferenceMap<Key, Watcher<Key, Value, Result>?> = ReferenceMap<Key, Watcher<Key, Value, Result>?>(true)
    private val watchers: MutableMap<Key, MutableList<Watcher<Key, Value, Result>>?> = LinkedHashMap<Key, MutableList<Watcher<Key, Value, Result>>?>()

    private var contextId = 0

    // Listeners
    override fun onPerformRestart() {
        synchronized(dataLock) {
            contextId++
            entries.clear()
        }
    }

    // Impl
    @UiThread
    fun find(key: Key): Result? {
        synchronized(dataLock) {
            return entries.get(key)
        }
    }

    @UiThread
    fun findOrRequest(key: Key, callback: RunnableData<Result?>?): Result? {
        val watcher: Watcher<Key, Value, Result>?
        if (callback != null) {
            watcher = TdlibDataManager.Watcher { context: TdlibDataManager<Key, Value, Result>?, entry: Result? -> callback.runWithData(entry) }
        } else {
            watcher = null
        }
        val entry = findOrPostponeRequest(key, watcher, true)
        if (entry != null) {
          callback?.runWithData(entry)
        } else {
            performPostponedRequest(key)
        }
        return entry
    }

    @JvmOverloads
    fun findOrPostponeRequest(key: Key, watcher: Watcher<Key, Value, Result>?, strongReference: Boolean = false): Result? {
        synchronized(dataLock) {
            val entry = entries.get(key)
            if (entry != null) {
                return entry
            }
            if (!loadingKeys.contains(key)) {
                postponedKeys.add(key)
            }
            if (watcher != null) {
                if (strongReference) {
                    addWatcherImpl(key, watcher)
                } else {
                    watcherReferences.add(key, watcher)
                }
            }
            return null
        }
    }

    private fun addWatcherImpl(key: Key, watcher: Watcher<Key, Value, Result>) {
        var list = watchers[key]
        if (list != null && !list.contains(watcher)) {
            list.add(watcher)
        } else {
            list = ArrayList<Watcher<Key, Value, Result>>()
            list.add(watcher)
          watchers[key] = list
        }
    }

    fun forgetWatcher(key: Key, watcher: Watcher<Key, Value, Result>?) {
        watcherReferences.remove(key, watcher)
    }

    private val delayedHandler = BatchOperationHandler(Runnable { this.performPostponedRequests() }, 10)

    init {
      tdlib.listeners().addCleanupListener(this)
    }

    @UiThread
    fun performPostponedRequest(key: Key) {
        val contextId: Int
        synchronized(dataLock) {
            if (postponedKeys.isEmpty() || !postponedKeys.remove(key)) {
                return
            }
            loadingKeys.add(key)
            contextId = this.contextId
        }
        requestData(contextId, mutableSetOf<Key>(key))
    }

    @UiThread
    fun performPostponedRequestsDelayed() {
        delayedHandler.performOperation()
    }

    @UiThread
    fun performPostponedRequests() {
        val keysToRequest: MutableSet<Key>
        val contextId: Int
        synchronized(dataLock) {
            if (postponedKeys.isEmpty()) {
                return
            }
            loadingKeys.addAll(postponedKeys)
            keysToRequest = HashSet<Key>(postponedKeys)
            postponedKeys.clear()
            contextId = this.contextId
        }
        requestData(contextId, keysToRequest)
    }

    protected fun isCancelled(contextId: Int): Boolean {
        synchronized(dataLock) {
            return this.contextId != contextId
        }
    }

    @UiThread
    protected abstract fun requestData(contextId: Int, keysToRequest: MutableCollection<Key>)

    @TdlibThread
    protected fun processData(contextId: Int, key: Key, value: Value?) {
        if (value != null) {
            processEntry(contextId, newEntry(key, value, null))
        } else {
            processError(contextId, key, TdApi.Error(404, "Not Found"))
        }
    }

    @TdlibThread
    protected fun processError(contextId: Int, key: Key, error: TdApi.Error) {
        processEntry(contextId, newEntry(key, null, error))
    }

    @TdlibThread
    private fun processEntry(contextId: Int, entry: Result?) {
        val watcherList: MutableList<Watcher<Key, Value, Result>>?
        synchronized(dataLock) {
            if (this.contextId != contextId) return
            entries.put(entry!!.key, entry)
            watcherList = watchers.remove(entry.key)
        }
        val referenceList = watcherReferences.removeAll(entry!!.key)
        if (referenceList != null) {
            for (watcher in referenceList) {
                watcher!!.onEntryLoaded(this, entry)
            }
            referenceList.clear()
        }
        if (watcherList != null) {
            for (watcher in watcherList) {
                watcher.onEntryLoaded(this, entry)
            }
        }
        synchronized(dataLock) {
            if (this.contextId != contextId) return
            loadingKeys.remove(entry.key)
        }
    }
}
