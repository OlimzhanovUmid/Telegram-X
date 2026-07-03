/*
 * Copyright 2015 Diego Gómez Olvera
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thunderdog.challegram.widget.rtl

import android.database.DataSetObserver
import android.os.Parcelable
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter

/**
 * PagerAdapter decorator.
 */
internal open class PagerAdapterWrapper protected constructor (private val adapter: PagerAdapter) : PagerAdapter() {

  fun getInnerAdapter (): PagerAdapter {
    return adapter
  }

  override fun getCount (): Int {
    return adapter.getCount()
  }

  override fun isViewFromObject (view: View, `object`: Any): Boolean {
    return adapter.isViewFromObject(view, `object`)
  }

  override fun getPageTitle (position: Int): CharSequence? {
    return adapter.getPageTitle(position)
  }

  override fun getPageWidth (position: Int): Float {
    return adapter.getPageWidth(position)
  }

  override fun getItemPosition (`object`: Any): Int {
    return adapter.getItemPosition(`object`)
  }

  override fun instantiateItem (container: ViewGroup, position: Int): Any {
    return adapter.instantiateItem(container, position)
  }

  override fun destroyItem (container: ViewGroup, position: Int, `object`: Any) {
    adapter.destroyItem(container, position, `object`)
  }

  override fun setPrimaryItem (container: ViewGroup, position: Int, `object`: Any) {
    adapter.setPrimaryItem(container, position, `object`)
  }

  fun superNotifyDataSetChanged () {
    super.notifyDataSetChanged()
  }

  override fun notifyDataSetChanged () {
    adapter.notifyDataSetChanged()
  }

  override fun registerDataSetObserver (observer: DataSetObserver) {
    adapter.registerDataSetObserver(observer)
  }

  override fun unregisterDataSetObserver (observer: DataSetObserver) {
    adapter.unregisterDataSetObserver(observer)
  }

  override fun saveState (): Parcelable? {
    return adapter.saveState()
  }

  override fun restoreState (state: Parcelable?, loader: ClassLoader?) {
    adapter.restoreState(state, loader)
  }

  override fun startUpdate (container: ViewGroup) {
    adapter.startUpdate(container)
  }

  override fun finishUpdate (container: ViewGroup) {
    adapter.finishUpdate(container)
  }
}
