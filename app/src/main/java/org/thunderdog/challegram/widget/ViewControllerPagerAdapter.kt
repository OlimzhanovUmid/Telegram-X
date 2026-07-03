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
 * File created on 25/12/2016
 */
package org.thunderdog.challegram.widget

import android.view.View
import android.view.ViewGroup
import androidx.collection.SparseArrayCompat
import androidx.viewpager.widget.PagerAdapter
import org.thunderdog.challegram.navigation.NavigationController
import org.thunderdog.challegram.navigation.ViewController
import me.vkryl.core.lambda.Destroyable

class ViewControllerPagerAdapter (private val provider: ControllerProvider) : PagerAdapter(), Destroyable, ViewController.AttachListener {
  interface ControllerProvider {
    fun getControllerCount (): Int
    fun createControllerForPosition (position: Int): ViewController<*>
    fun onPrepareToShow (position: Int, controller: ViewController<*>)
    fun onAfterHide (position: Int, controller: ViewController<*>)
    fun getParentOrSelf (): ViewController<*>
  }

  private val controllers = SparseArrayCompat<ViewController<*>>()
  private val visibleControllers = HashSet<ViewController<*>>()

  init {
    provider.getParentOrSelf().addAttachStateListener(this)
  }

  override fun onAttachStateChanged (context: ViewController<*>?, navigation: NavigationController?, isAttached: Boolean) {
    for (c in visibleControllers) {
      c.onAttachStateChanged(navigation, isAttached)
    }
  }

  fun notifyItemInserted (index: Int) {
    val size = controllers.size()
    for (i in size - 1 downTo 0) {
      val key = controllers.keyAt(i)
      if (key < index)
        break
      val item = controllers.valueAt(i)
      controllers.removeAt(i)
      controllers.put(key + 1, item)
    }
  }

  fun notifyItemRemoved (index: Int) {
    var i = controllers.indexOfKey(index)
    if (i < 0) {
      return
    }
    val c = controllers.valueAt(i)
    controllers.removeAt(i)
    c.destroy()
    val count = controllers.size()
    while (i < count) {
      val key = controllers.keyAt(i)
      val item = controllers.valueAt(i)
      controllers.removeAt(i)
      controllers.put(key - 1, item)
      i++
    }
  }

  override fun performDestroy () {
    val size = controllers.size()
    for (i in 0 until size) {
      val c = controllers.valueAt(i)
      if (!c.isDestroyed())
        c.destroy()
    }
    controllers.clear()
  }

  override fun getCount (): Int {
    return provider.getControllerCount()
  }

  fun findViewControllerById (id: Int): ViewController<*>? {
    val size = controllers.size()
    for (i in 0 until size) {
      val c = controllers.valueAt(i)
      if (c.getId() == id) {
        return c
      }
    }
    return null
  }

  fun findCachedControllerByPosition (position: Int): ViewController<*>? {
    return controllers.get(position)
  }

  override fun getItemPosition (`object`: Any): Int {
    for (i in 0 until controllers.size()) {
      val c = controllers.valueAt(i)
      if (c === `object`) {
        return controllers.keyAt(i)
      }
    }
    return POSITION_NONE
  }

  override fun instantiateItem (container: ViewGroup, position: Int): Any {
    var c = controllers.get(position)
    if (c == null) {
      c = provider.createControllerForPosition(position)
      controllers.put(position, c)
    }
    val view = c.getValue()!!
    if (view.parent != null) {
      (view.parent as ViewGroup).removeView(view)
    }
    provider.onPrepareToShow(position, c)
    c.onPrepareToShow()
    container.addView(view)
    visibleControllers.add(c)
    if (!c.attachState) {
      c.onAttachStateChanged(provider.getParentOrSelf().navigationController(), true)
    }
    return c
  }

  override fun destroyItem (container: ViewGroup, position: Int, `object`: Any) {
    val c = `object` as ViewController<*>
    container.removeView(c.getValue())
    visibleControllers.remove(c)
    if (c.attachState) {
      c.onAttachStateChanged(provider.getParentOrSelf().navigationController(), false)
    }
    provider.onAfterHide(position, c)
    c.onCleanAfterHide()
  }

  override fun isViewFromObject (view: View, `object`: Any): Boolean {
    return `object` is ViewController<*> && `object`.wrapUnchecked == view
  }
}
