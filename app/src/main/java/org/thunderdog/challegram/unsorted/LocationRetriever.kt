package org.thunderdog.challegram.unsorted

import android.location.Location
import org.thunderdog.challegram.BaseActivity
import me.vkryl.core.lambda.Destroyable
import me.vkryl.core.lambda.RunnableBool
import java.util.Queue
import java.util.concurrent.LinkedBlockingDeque

abstract class LocationRetriever(@JvmField protected val activity: BaseActivity) : Destroyable {
  interface LocationCallback {
    fun onSuccess (location: Location)
    fun onFailure ()
  }

  private val pendingCallbacks: Queue<LocationCallback> = LinkedBlockingDeque()

  fun requestLocation (callback: LocationCallback) {
    pendingCallbacks.offer(callback)
    retrieveLocation()
  }

  abstract fun checkPermissions (runnable: RunnableBool)
  protected abstract fun retrieveLocation ()

  protected fun onLocationRetrieved (location: Location) {
    var callback: LocationCallback?
    while (pendingCallbacks.poll().also { callback = it } != null) {
      callback!!.onSuccess(location)
    }
  }

  protected fun onLocationFetchFailed () {
    var callback: LocationCallback?
    while (pendingCallbacks.poll().also { callback = it } != null) {
      callback!!.onFailure()
    }
  }
}
