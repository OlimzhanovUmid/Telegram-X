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
 * File created on 10/12/2016
 */
package org.thunderdog.challegram.mediaview

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.thunderdog.challegram.U
import org.thunderdog.challegram.loader.ImageFile
import java.util.ArrayList

class MediaOtherAdapter (private val context: Context, private val onClickListener: View.OnClickListener) : RecyclerView.Adapter<MediaOtherAdapter.Holder>() {
  private var images: ArrayList<ImageFile?>? = null

  fun setImages (images: ArrayList<ImageFile?>?) {
    val oldItemCount = itemCount
    this.images = images
    U.notifyItemsReplaced(this, oldItemCount)
  }

  override fun onCreateViewHolder (parent: ViewGroup, viewType: Int): Holder {
    val view = MediaOtherView(context)
    view.setOnDeleteClick(onClickListener)
    return Holder(view)
  }

  override fun onBindViewHolder (holder: Holder, position: Int) {
    (holder.itemView as MediaOtherView).setImage(images!!.get(position))
  }

  override fun onViewAttachedToWindow (holder: Holder) {
    (holder.itemView as MediaOtherView).attach()
  }

  override fun onViewDetachedFromWindow (holder: Holder) {
    (holder.itemView as MediaOtherView).detach()
  }

  override fun onViewRecycled (holder: Holder) {
    (holder.itemView as MediaOtherView).performDestroy()
  }

  fun removeImage (imageFile: ImageFile?) {
    var i = 0
    for (file in images!!) {
      if (file === imageFile) {
        images!!.removeAt(i)
        notifyItemRemoved(i)
        break
      }
      i++
    }
  }

  override fun getItemCount (): Int {
    return if (images != null) images!!.size else 0
  }

  class Holder (itemView: View) : RecyclerView.ViewHolder(itemView)
}
