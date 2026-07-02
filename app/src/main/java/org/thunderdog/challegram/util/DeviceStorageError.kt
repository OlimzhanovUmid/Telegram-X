package org.thunderdog.challegram.util

class DeviceStorageError : IllegalStateException {
  constructor () : super()

  constructor (s: String?) : super(s)

  constructor (cause: Throwable?) : super(cause)
}
