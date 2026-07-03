package org.thunderdog.challegram.util

import androidx.recyclerview.widget.DiffUtil
import org.thunderdog.challegram.ui.ListItem

abstract class ListItemDiffUtilCallback (private val oldList: List<ListItem>, private val newList: List<ListItem>) : DiffUtil.Callback() {
  final override fun getOldListSize (): Int {
    return oldList.size
  }

  final override fun getNewListSize (): Int {
    return newList.size
  }

  final override fun areItemsTheSame (oldItemPosition: Int, newItemPosition: Int): Boolean {
    val oldItem = oldList[oldItemPosition]
    val newItem = newList[newItemPosition]
    return areItemsTheSame(oldItem, newItem)
  }

  final override fun areContentsTheSame (oldItemPosition: Int, newItemPosition: Int): Boolean {
    val oldItem = oldList[oldItemPosition]
    val newItem = newList[newItemPosition]
    return areContentsTheSame(oldItem, newItem)
  }

  abstract fun areItemsTheSame (oldItem: ListItem, newItem: ListItem): Boolean
  abstract fun areContentsTheSame (oldItem: ListItem, newItem: ListItem): Boolean
}
