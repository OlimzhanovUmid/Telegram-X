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
 */
package org.thunderdog.challegram.ui.camera

import android.content.Context
import android.graphics.SurfaceTexture
import android.view.Gravity
import android.view.TextureView
import android.view.ViewGroup

import me.vkryl.android.widget.FrameLayoutFix

abstract class CameraManagerTexture (context: Context, delegate: CameraDelegate) : CameraManager<CameraTextureView>(context, delegate), TextureView.SurfaceTextureListener {
  protected var surfaceTexture: SurfaceTexture? = null
    private set
  protected var textureWidth: Int = 0
    private set
  protected var textureHeight: Int = 0
    private set

  abstract fun setPreviewSize (viewWidth: Int, viewHeight: Int)

  protected abstract fun onTextureAvailable (surfaceTexture: SurfaceTexture, width: Int, height: Int)
  protected abstract fun onTextureSizeChanged (surfaceTexture: SurfaceTexture, width: Int, height: Int)
  protected abstract fun onTextureDestroyed (surfaceTexture: SurfaceTexture)

  final override fun onCreateView (): CameraTextureView {
    val cameraView = CameraTextureView(context)
    cameraView.setLayoutParams(FrameLayoutFix.newParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
    cameraView.setManager(this)
    cameraView.setSurfaceTextureListener(this)
    return cameraView
  }

  final override fun onSurfaceTextureAvailable (surface: SurfaceTexture, width: Int, height: Int) {
    this.surfaceTexture = surface
    this.textureWidth = width
    this.textureHeight = height
    onTextureAvailable(surface, width, height)
  }

  final override fun onSurfaceTextureSizeChanged (surface: SurfaceTexture, width: Int, height: Int) {
    this.textureWidth = width
    this.textureHeight = height
    onTextureSizeChanged(surface, width, height)
  }

  final override fun onSurfaceTextureDestroyed (surface: SurfaceTexture): Boolean {
    onTextureDestroyed(surface)
    this.surfaceTexture = null
    return true
  }

  final override fun onSurfaceTextureUpdated (surface: SurfaceTexture) { }
}
