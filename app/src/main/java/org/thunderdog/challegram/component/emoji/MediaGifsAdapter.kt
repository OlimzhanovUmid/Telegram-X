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
 * File created on 27/02/2016 at 21:16
 */
package org.thunderdog.challegram.component.emoji

import android.content.Context
import android.view.View
import android.view.ViewGroup

import androidx.recyclerview.widget.RecyclerView

import org.drinkless.tdlib.TdApi
import org.thunderdog.challegram.data.TGGif
import org.thunderdog.challegram.tool.Views
import org.thunderdog.challegram.widget.EmojiLayout

import java.util.ArrayList

import me.vkryl.android.util.ClickHelper

class MediaGifsAdapter (private val context: Context, private val clickHelperDelegate: ClickHelper.Delegate?) : RecyclerView.Adapter<MediaGifsAdapter.GifHolder>(), View.OnClickListener, View.OnLongClickListener {
  private var gifs: ArrayList<TGGif>? = null
  private var callback: Callback? = null

  interface Callback {
    fun onGifPressed (view: View, animation: TdApi.Animation)
    fun onGifLongPressed (view: View, animation: TdApi.Animation)
  }

  private var needHeader = false

  fun setNeedHeader () {
    this.needHeader = true
  }

  fun setCallback (callback: Callback) {
    this.callback = callback
  }

  fun setGIFs (gifs: ArrayList<TGGif>?) {
    val oldGifsCount = if (this.gifs != null) this.gifs!!.size else 0
    val newGifsCount = gifs?.size ?: 0

    if (oldGifsCount == newGifsCount) {
      if (newGifsCount == 0) {
        return
      }
      var index = 0
      var hasDiff = false
      for (gif in gifs!!) {
        if (gif.id != this.gifs!!.get(index).id) {
          hasDiff = true
          break
        }
        index++
      }
      if (!hasDiff) {
        return
      }
    }

    if (this.gifs != null && this.gifs!!.size > 0) {
      val prevCount = this.gifs!!.size
      this.gifs = null
      // notifyItemRangeRemoved(needHeader ? 1 : 0, prevCount);
    }
    this.gifs = gifs
    val count = if (gifs != null && !gifs.isEmpty()) gifs.size else 0
    if (count > 0) {
      // notifyItemRangeInserted(needHeader ? 1 : 0, count);
    }
    notifyDataSetChanged() // FIXME
  }

  fun removeSavedGif (gifId: Int) {
    if (this.gifs == null || this.gifs!!.isEmpty()) {
      return
    }
    if (this.gifs!!.size == 1) {
      if (this.gifs!!.get(0).id == gifId) {
        clear()
      }
      return
    }
    var i = 0
    for (gif in gifs!!) {
      if (gif.id == gifId) {
        gifs!!.removeAt(i)
        // notifyItemRemoved(needHeader ? i + 1 : i);
        notifyDataSetChanged() // FIXME
        break
      }
      i++
    }
  }

  fun clear () {
    setGIFs(null)
  }

  fun getGif (i: Int): TGGif {
    return gifs!!.get(if (needHeader) i - 1 else i)
  }

  override fun onClick (v: View) {
    if (callback != null) {
      callback!!.onGifPressed(v, (v as GifView).gif!!.animation)
    }
  }

  override fun onLongClick (v: View): Boolean {
    if (callback != null) {
      callback!!.onGifLongPressed(v, (v as GifView).gif!!.animation)
      return true
    }
    return false
  }

  override fun onCreateViewHolder (parent: ViewGroup, viewType: Int): GifHolder {
    return GifHolder.create(context, viewType, this, this, clickHelperDelegate)
  }

  override fun onBindViewHolder (holder: GifHolder, position: Int) {
    (holder.itemView as GifView).gif = gifs!!.get(if (needHeader) position - 1 else position)
  }

  override fun getItemViewType (position: Int): Int {
    return if (position == 0 && needHeader) 1 else 0
  }

  override fun onViewAttachedToWindow (holder: GifHolder) {
    (holder.itemView as GifView).attach()
  }

  override fun onViewDetachedFromWindow (holder: GifHolder) {
    (holder.itemView as GifView).detach()
  }

  override fun getItemCount (): Int {
    return if (gifs == null) 0 else gifs!!.size
  }

  class GifHolder (itemView: View) : RecyclerView.ViewHolder(itemView) {
    companion object {
      @JvmStatic
      fun create (context: Context, viewType: Int, onClickListener: View.OnClickListener, onLongClickListener: View.OnLongClickListener, delegate: ClickHelper.Delegate?): GifHolder {
        when (viewType) {
          0 -> {
            val view = GifView(context)
            if (delegate != null) {
              view.initWithDelegate(delegate)
            } else {
              view.setOnClickListener(onClickListener)
              view.setOnLongClickListener(onLongClickListener)
            }
            Views.setClickable(view)
            view.setLayoutParams(RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            return GifHolder(view)
          }
          1 -> {
            val view = View(context)
            view.setLayoutParams(RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, EmojiLayout.getHeaderSize()))
            return GifHolder(view)
          }
        }
        throw RuntimeException()
      }
    }
  }
}
