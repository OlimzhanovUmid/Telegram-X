package org.thunderdog.challegram.navigation

import androidx.activity.BackEventCompat

interface SystemBackEventListener {
  fun onSystemBackStarted (backEvent: BackEventCompat): Boolean
  fun onSystemBackProgressed (backEvent: BackEventCompat)
  fun onSystemBackCancelled ()
  fun onSystemBackPressed (): Boolean
}
