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
 * File created on 21/12/2016
 */
package org.thunderdog.challegram.texture

import android.content.Context
import android.opengl.GLES20
import android.view.TextureView

@Suppress("NewApi")
open class CustomTextureView(context: Context) : TextureView(context) {
  interface Listener {
    fun onTextureCreated (width: Int, height: Int)
    fun onTextureSizeChanged (width: Int, height: Int)
    fun onDrawFrame ()
  }

  private val queue = TextureViewQueue(object : TextureViewQueue.Listener {
    override fun onSurfaceCreated (width: Int, height: Int) {
      listener?.onTextureCreated(width, height)
    }

    override fun onSurfaceSizeChanged (width: Int, height: Int) {
      listener?.onTextureSizeChanged(width, height)
    }

    override fun onSurfaceDraw () {
      listener?.onDrawFrame() ?: run {
        GLES20.glClearColor(1f, 0f, 0f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
      }
    }
  })

  private var listener: Listener? = null

  fun setListener (listener: Listener) {
    // preserve existing behavior: setter intentionally inert
  }

  fun onPause () {
    queue.onPause()
  }

  fun onResume () {
    queue.onResume()
  }

  fun onDestroy () {
    queue.onDestroy()
  }

  fun requestRender () {
    queue.requestRender()
  }
}
