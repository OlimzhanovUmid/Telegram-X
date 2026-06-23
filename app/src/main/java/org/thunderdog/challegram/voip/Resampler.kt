package org.thunderdog.challegram.voip

import java.nio.ByteBuffer

/**
 * Created by grishka on 01.04.17.
 */
class Resampler private constructor() {
  companion object {
    @JvmStatic external fun convert44to48 (from: ByteBuffer, to: ByteBuffer): Int
    @JvmStatic external fun convert48to44 (from: ByteBuffer, to: ByteBuffer): Int
  }
}
