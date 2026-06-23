package org.thunderdog.challegram.util

data class LongPair (
  @JvmField val value1: Long,
  @JvmField val value2: Long
) {
  companion object {
    @JvmStatic fun of (value1: Long, value2: Long): LongPair = LongPair(value1, value2)
  }
}
