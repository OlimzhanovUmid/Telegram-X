package org.thunderdog.challegram.video

import android.media.MediaCodec
import android.media.MediaFormat
import org.thunderdog.challegram.video.old.Mp4Movie
import org.thunderdog.challegram.video.old.Mp4OutputImpl
import java.io.File
import java.nio.ByteBuffer

abstract class Mp4Output {
  @Throws(Exception::class) abstract fun addTrack (format: MediaFormat, isAudio: Boolean): Int
  @Throws(Exception::class) abstract fun writeSampleData (trackIndex: Int, byteBuf: ByteBuffer, bufferInfo: MediaCodec.BufferInfo, writeLength: Boolean): Boolean
  @Throws(Exception::class) abstract fun finishMovie ()

  companion object {
    @JvmStatic @Throws(Exception::class) fun valueOf (targetFile: File, width: Int, height: Int): Mp4Output {
      val movie = Mp4Movie()
      movie.setCacheFile(targetFile)
      movie.setSize(width, height)
      return Mp4OutputImpl().createMovie(movie)
    }
  }
}
