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
 * File created on 24/10/2016
 */
package org.thunderdog.challegram.component.attach

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.thunderdog.challegram.R
import org.thunderdog.challegram.core.Media
import org.thunderdog.challegram.tool.Screen

class MediaBottomGalleryBucketAdapter(
  private val context: Context,
  private val callback: Callback?,
  private val gallery: Media.Gallery
) : RecyclerView.Adapter<MediaBottomGalleryBucketAdapter.BucketViewHolder>(), View.OnClickListener, MeasuredAdapterDelegate {
  interface Callback {
    fun onBucketSelected (bucket: Media.GalleryBucket)
  }

  override fun onCreateViewHolder (parent: ViewGroup, viewType: Int): BucketViewHolder {
    return BucketViewHolder.create(context, this)
  }

  override fun onClick (v: View) {
    if (v.id == R.id.bucket) {
      val bucket = (v as MediaBottomGalleryBucketView).bucket
      callback?.onBucketSelected(bucket)
    }
  }

  override fun onBindViewHolder (holder: BucketViewHolder, position: Int) {
    holder.setBucket(gallery.getBucketForIndex(position))
  }

  override fun measureHeight (maxHeight: Int): Int {
    val height = itemCount * Screen.dp(48f)
    return if (maxHeight == -1 || maxHeight >= height) height else maxHeight
  }

  override fun onViewRecycled (holder: BucketViewHolder) {
    holder.detach()
  }

  override fun measureScrollTop (position: Int): Int = 0

  override fun onViewAttachedToWindow (holder: BucketViewHolder) {
    holder.attach()
  }

  override fun onViewDetachedFromWindow (holder: BucketViewHolder) {
    holder.detach()
  }

  override fun getItemCount (): Int = gallery.getBucketCount()

  class BucketViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun attach () {
      (itemView as MediaBottomGalleryBucketView).attach()
    }

    fun detach () {
      (itemView as MediaBottomGalleryBucketView).detach()
    }

    fun setBucket (bucket: Media.GalleryBucket) {
      (itemView as MediaBottomGalleryBucketView).setBucket(bucket)
    }

    companion object {
      @JvmStatic
      fun create (context: Context, onClickListener: View.OnClickListener): BucketViewHolder {
        val view = MediaBottomGalleryBucketView(context)
        view.id = R.id.bucket
        view.setOnClickListener(onClickListener)
        return BucketViewHolder(view)
      }
    }
  }
}
