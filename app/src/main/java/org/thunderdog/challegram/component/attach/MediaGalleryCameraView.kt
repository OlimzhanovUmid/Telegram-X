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
 * File created on 21/10/2016
 */
package org.thunderdog.challegram.component.attach

import android.content.Context
import android.graphics.SurfaceTexture
import android.view.Gravity
import android.view.TextureView
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import org.thunderdog.challegram.R
import org.thunderdog.challegram.tool.Screen
import me.vkryl.android.widget.FrameLayoutFix
import me.vkryl.core.lambda.Destroyable

class MediaGalleryCameraView (context: Context) : FrameLayoutFix(context), Destroyable, TextureView.SurfaceTextureListener {
  private val textureView: TextureView

  init {
    setId(R.id.btn_camera)
    setLayoutParams(RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    setBackgroundColor(0xff000000.toInt())

    val params = newParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.RIGHT or Gravity.TOP)
    params.topMargin = Screen.dp(6f)
    params.rightMargin = Screen.dp(6f)

    textureView = TextureView(context)
    textureView.setLayoutParams(newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    textureView.setSurfaceTextureListener(this)
    addView(textureView)

    val imageView = ImageView(context)
    imageView.setScaleType(ImageView.ScaleType.CENTER)
    imageView.setImageResource(R.drawable.baseline_camera_alt_24)
    imageView.setColorFilter(0xffffffff.toInt())
    imageView.setLayoutParams(params)
    addView(imageView)
  }

  // Texture stuff

  private fun openCamera () {

  }

  override fun onSurfaceTextureAvailable (surface: SurfaceTexture, width: Int, height: Int) {
    openCamera()
  }

  override fun onSurfaceTextureSizeChanged (surface: SurfaceTexture, width: Int, height: Int) {

  }

  override fun onSurfaceTextureDestroyed (surface: SurfaceTexture): Boolean {
    return false
  }

  override fun onSurfaceTextureUpdated (surface: SurfaceTexture) {

  }


  // Etc

  override fun onMeasure (widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, widthMeasureSpec)
  }

  override fun performDestroy () {
    // TODO
  }

  fun attach () {

  }

  fun detach () {

  }
}
