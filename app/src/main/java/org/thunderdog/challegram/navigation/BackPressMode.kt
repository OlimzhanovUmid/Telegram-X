package org.thunderdog.challegram.navigation

import androidx.annotation.IntDef
import kotlin.annotation.AnnotationRetention
import kotlin.annotation.Retention

@Retention(AnnotationRetention.SOURCE)
@IntDef(
  BackPressMode.SYSTEM_ACTION_REQUIRED,
  BackPressMode.CUSTOM_ACTION_PERFORMED,
  BackPressMode.NAVIGATE_BACK_IN_STACK,
  BackPressMode.CLOSE_NAVIGATION_DRAWER
)
annotation class BackPressMode {
  companion object {
    const val SYSTEM_ACTION_REQUIRED = 0
    const val CUSTOM_ACTION_PERFORMED = 1
    const val NAVIGATE_BACK_IN_STACK = 2
    const val CLOSE_NAVIGATION_DRAWER = 3
  }
}
