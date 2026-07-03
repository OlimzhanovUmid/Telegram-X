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
 * File created on 25/01/2019
 */
package org.thunderdog.challegram.sync

import android.accounts.Account
import android.accounts.AccountManager
import android.content.AbstractThreadedSyncAdapter
import android.content.ContentProviderClient
import android.content.ContentResolver
import android.content.Context
import android.content.SyncResult
import android.os.Bundle
import org.thunderdog.challegram.BuildConfig
import org.thunderdog.challegram.Log
import org.thunderdog.challegram.config.Config
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.telegram.TdlibAccount
import org.thunderdog.challegram.telegram.TdlibManager
import org.thunderdog.challegram.unsorted.Settings

internal class SyncAdapter (context: Context, autoInitialize: Boolean) : AbstractThreadedSyncAdapter(context, autoInitialize) {
  override fun onPerformSync (account: Account, extras: Bundle?, authority: String, provider: ContentProviderClient, syncResult: SyncResult) {
    if (Config.NEED_SYSTEM_SYNC) {
      val accountId = extras?.getInt(EXTRA_ACCOUNT_ID, TdlibAccount.NO_ID) ?: TdlibAccount.NO_ID
      try {
        TdlibManager.makeSync(getContext(), accountId, TdlibManager.SYNC_CAUSE_SYSTEM_SYNC, 0, !TdlibManager.inUiThread(), 0)
      } catch (t: Throwable) {
        Log.e("Failed to perform sync", t)
      }
    }
  }

  companion object {
    private val AUTHORITY = BuildConfig.APPLICATION_ID + ".sync.provider"
    private val ACCOUNT_TYPE = BuildConfig.APPLICATION_ID + ".sync.account"
    private const val ACCOUNT_NAME = "Telegram"

    private const val EXTRA_ACCOUNT_ID = "account_id"

    private fun getAccount (context: Context): Account {
      val newAccount = Account(ACCOUNT_NAME, ACCOUNT_TYPE)
      val accountManager = context.getSystemService(Context.ACCOUNT_SERVICE) as AccountManager
      accountManager.addAccountExplicitly(newAccount, null, null)
      return newAccount
    }

    @JvmStatic
    fun register (context: Context) {
      try {
        val account = getAccount(context)
        ContentResolver.setIsSyncable(account, AUTHORITY, 1)
        ContentResolver.setSyncAutomatically(account, AUTHORITY, true)
        ContentResolver.addPeriodicSync(account, AUTHORITY, Bundle.EMPTY, Settings.instance().periodicSyncFrequencySeconds)
      } catch (t: SecurityException) {
        Log.e("Cannot register stub account", t)
      }
    }

    @JvmStatic
    fun isSyncEnabledGlobally (): Boolean {
      return try {
        ContentResolver.getMasterSyncAutomatically()
      } catch (e: SecurityException) {
        Log.e(e)
        true
      }
    }

    @JvmStatic
    fun isSyncEnabled (context: Context): Boolean {
      return try {
        ContentResolver.getSyncAutomatically(getAccount(context), AUTHORITY)
      } catch (e: SecurityException) {
        Log.e(e)
        true
      }
    }

    @JvmStatic
    fun turnOnSync (context: Context, tdlib: Tdlib, needGlobal: Boolean) {
      try {
        if (needGlobal)
          ContentResolver.setMasterSyncAutomatically(true)
        register(context)
        tdlib.listeners().updateNotificationGlobalSettings()
      } catch (t: Throwable) {
        Log.e(t)
      }
    }

    @JvmStatic
    fun performSync (context: Context, accountId: Int) {
      try {
        ContentResolver.requestSync(getAccount(context), AUTHORITY, makeBundle(accountId))
      } catch (e: SecurityException) {
        Log.e(e)
      }
    }

    private fun makeBundle (accountId: Int): Bundle {
      val extras: Bundle
      if (accountId != TdlibAccount.NO_ID) {
        extras = Bundle()
        extras.putInt(EXTRA_ACCOUNT_ID, accountId)
      } else {
        extras = Bundle.EMPTY
      }
      return extras
    }
  }
}
