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
 * File created on 12/03/2019
 */
package org.drinkmore

import androidx.annotation.IntDef
import androidx.annotation.Keep
import org.drinkless.tdlib.TdApi
import org.drinkmore.ClientException.DatabaseError
import org.drinkmore.ClientException.TdlibLaunchError
import org.drinkmore.ClientException.TdlibLostPromiseError
import org.thunderdog.challegram.Log
import org.thunderdog.challegram.N
import org.thunderdog.challegram.data.TD
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.telegram.TdlibAccount
import org.thunderdog.challegram.unsorted.Settings
import org.thunderdog.challegram.util.Crash
import java.util.Locale
import kotlin.system.exitProcess

@Suppress("unused")
object Tracer {
  @JvmStatic
  fun format(message: String): String {
    return String.format(Locale.US, "Client fatal error: %s", message)
  }

  private fun throwErrorOnAnotherThread(throwable: Throwable) {
    Thread {
      throwError(throwable)
      exitProcess(1)
    }.start()
    do {
      try {
        Thread.sleep(1000)
      } catch (ignored: InterruptedException) {
      }
    } while (true)
  }

  private fun throwError(throwable: Throwable) {
    Settings.instance().pmc().apply() // Release any locks

    if (throwable is ClientException) throw throwable
    if (throwable is RuntimeException) {
      throw throwable
    }
    val elements = throwable.stackTrace
    val newElements = arrayOfNulls<StackTraceElement>(elements.size + 1)
    System.arraycopy(elements, 0, newElements, 1, elements.size)
    newElements[0] = StackTraceElement("org.drinkmore.Tracer", "throwError", "Tracer.java", 50)
    throwable.setStackTrace(newElements)
    val exception = RuntimeException(format(throwable.javaClass.getSimpleName() + ": " + throwable.message), throwable.cause)
    exception.setStackTrace(throwable.stackTrace)
    throw exception
  }

  private fun onFatalError(error: Throwable, @Cause cause: Int) {
    when (cause) {
      Cause.FATAL_ERROR -> ClientException.throwAssertionError(error)
      Cause.DATABASE_ERROR -> throw DatabaseError(error.javaClass.getSimpleName() + ": " + error.message)
      Cause.TDLIB_LAUNCH_ERROR -> throw TdlibLaunchError(error.message)
      Cause.TDLIB_LOST_PROMISE_ERROR -> throw TdlibLostPromiseError(error.message)
      Cause.LAUNCH_ERROR, Cause.UI_ERROR, Cause.OTHER_ERROR, Cause.TEST_DIRECT -> throwError(error)
      Cause.TEST_INDIRECT -> ClientException.throwTestError(error)
      Cause.TDLIB_HANDLER_ERROR -> throwErrorOnAnotherThread(error)
    }
  }

  // Public API
  @JvmStatic
  fun onLaunchError(throwable: Throwable) {
    onFatalError(throwable, Cause.LAUNCH_ERROR)
  }

  @JvmStatic
  fun onOtherError(throwable: Throwable) {
    onFatalError(throwable, Cause.OTHER_ERROR)
  }

  @JvmStatic
  fun onDatabaseError(throwable: Throwable) {
    onFatalError(throwable, Cause.DATABASE_ERROR)
  }

  @JvmStatic
  fun canvasFailure(e: IllegalArgumentException, saveCount: Int) {
    Log.e("Restore count: %d", saveCount)
    val otherError = IllegalArgumentException(e.message + ", saveCount = " + saveCount)
    otherError.setStackTrace(e.stackTrace)
    onOtherError(otherError)
  }

  @JvmStatic
  fun onTdlibFatalError(tdlib: Tdlib?, function: Class<out TdApi.Function<*>?>?, error: TdApi.Error?, stackTrace: Array<StackTraceElement?>?) {
    val message = (if (function != null) function.getSimpleName() else "unknown") + ": " + TD.toErrorString(error)
    Settings.instance().storeCrash(
      Crash.Builder()
        .message(message)
        .accountId(tdlib?.accountId() ?: TdlibAccount.NO_ID)
        .flags(Crash.Flags.SOURCE_TDLIB_PARAMETERS)
    )
    if (stackTrace != null) {
      val t: Throwable = TdlibLaunchError(message)
      t.setStackTrace(stackTrace)
      onFatalError(t, Cause.TDLIB_HANDLER_ERROR)
    } else {
      onFatalError(TdlibLaunchError(message), Cause.TDLIB_HANDLER_ERROR)
    }
  }

  @JvmStatic
  fun onTdlibHandlerError(throwable: Throwable) {
    onFatalError(throwable, Cause.TDLIB_HANDLER_ERROR)
  }

  fun onTdlibLostPromiseError(message: String?) {
    onFatalError(AssertionError(message), Cause.TDLIB_LOST_PROMISE_ERROR)
  }

  @JvmStatic
  fun onUiError(throwable: Throwable) {
    onFatalError(throwable, Cause.UI_ERROR)
  }

  fun onDrawBitmapError(t: Throwable?) {
    Log.e("Bug: cannot draw bitmap", t)
  }

  @Keep
  fun onFatalError(message: String?, cause: Int) {
    onFatalError(AssertionError(message), cause)
  }

  // Experiments
  @JvmStatic
  fun test1(message: String?) {
    // Indirect throw via new exception
    onFatalError(AssertionError(message), Cause.TEST_INDIRECT)
  }

  @JvmStatic
  fun test2(message: String?) {
    // Direct throw
    onFatalError(AssertionError(message), Cause.TEST_DIRECT)
  }

  @JvmStatic
  fun test3(message: String?) {
    // Indirect throw via new exception & calling from NDK
    N.onFatalError(message, Cause.TEST_INDIRECT)
  }

  @JvmStatic
  fun test4(message: String?) {
    // Direct throw from NDK
    N.onFatalError(message, Cause.TEST_DIRECT)
  }

  @JvmStatic
  fun test5(message: String?) {
    // Just throws AssertionError from NDK
    N.throwDirect(message)
  }

  @Retention(AnnotationRetention.SOURCE)
  @IntDef(
    Cause.FATAL_ERROR,
    Cause.LAUNCH_ERROR,
    Cause.DATABASE_ERROR,
    Cause.TDLIB_HANDLER_ERROR,
    Cause.TDLIB_LAUNCH_ERROR,
    Cause.UI_ERROR,
    Cause.OTHER_ERROR,
    Cause.TDLIB_LOST_PROMISE_ERROR,
    Cause.TEST_INDIRECT,
    Cause.TEST_DIRECT
  )
  annotation class Cause {
    companion object {
      const val FATAL_ERROR: Int = 0
      const val LAUNCH_ERROR: Int = 1
      const val DATABASE_ERROR: Int = 2
      const val TDLIB_HANDLER_ERROR: Int = 3
      const val TDLIB_LAUNCH_ERROR: Int = 4
      const val UI_ERROR: Int = 6
      const val OTHER_ERROR: Int = 7
      const val TDLIB_LOST_PROMISE_ERROR: Int = 8

      const val TEST_INDIRECT: Int = 100
      const val TEST_DIRECT: Int = 101
    }
  }
}