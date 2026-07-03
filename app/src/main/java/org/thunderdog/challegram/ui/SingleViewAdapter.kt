package org.thunderdog.challegram.ui

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class SingleViewAdapter<V : View> (val singleView: V) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

  fun getView (): V {
    return singleView
  }

  override fun onCreateViewHolder (parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
    return object : RecyclerView.ViewHolder(singleView) {}
  }

  override fun onBindViewHolder (holder: RecyclerView.ViewHolder, position: Int) {
  }

  override fun getItemCount (): Int {
    return 1
  }
}
