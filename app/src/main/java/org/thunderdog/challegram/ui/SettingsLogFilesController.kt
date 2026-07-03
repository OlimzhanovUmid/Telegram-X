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
 * File created on 07/11/2017
 */
package org.thunderdog.challegram.ui

import android.content.Context
import android.view.View
import android.widget.Toast
import java.io.File
import java.util.ArrayList
import java.util.concurrent.TimeUnit
import org.thunderdog.challegram.Log
import org.thunderdog.challegram.R
import org.thunderdog.challegram.component.base.SettingView
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.navigation.ViewController
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.tool.Strings
import org.thunderdog.challegram.tool.UI
import org.thunderdog.challegram.util.OptionDelegate
import org.thunderdog.challegram.v.CustomRecyclerView

class SettingsLogFilesController(context: Context, tdlib: Tdlib) :
  RecyclerViewController<SettingsLogFilesController.Arguments>(context, tdlib),
  View.OnClickListener, Log.OutputListener {

  class Arguments(val currentFiles: Log.LogFiles?)

  override fun getId (): Int = R.id.controller_logs

  override fun getName (): CharSequence = "Application Logs"

  private lateinit var adapter: SettingsAdapter

  override fun onCreateView (context: Context, recyclerView: CustomRecyclerView) {
    adapter = object : SettingsAdapter(this) {
      override fun setValuedSetting (item: ListItem, view: SettingView, isUpdate: Boolean) {
        if (item.getId() == R.id.btn_file) {
          val file = item.getData() as File
          view.setData(Lang.getFileTimestamp(file.lastModified(), TimeUnit.MILLISECONDS, file.length()))
        }
      }
    }
    val args = getArguments()
    if (args != null && args.currentFiles != null && !args.currentFiles.isEmpty()) {
      setFiles(args.currentFiles)
    } else {
      buildCells()
      getFiles()
    }
    recyclerView.adapter = adapter
    Log.addOutputListener(this)
  }

  override fun destroy () {
    super.destroy()
    Log.removeOutputListener(this)
  }

  private fun getFiles () {
    Log.getLogFiles { result ->
      if (!isDestroyed()) {
        UI.post {
          if (!isDestroyed()) {
            setFiles(result)
          }
        }
      }
    }
  }

  override fun onLogOutput (tag: Int, level: Int, message: String, t: Throwable?) {
    UI.post {
      if (!isDestroyed()) {
        if (adapter.getItems() != null) {
          var position = 0
          for (item in adapter.getItems()) {
            if (item.getId() == R.id.btn_file) {
              adapter.updateValuedSettingByPosition(position)
            }
            position++
          }
        }
        if (files == null || files!!.isEmpty()) {
          getFiles()
        }
      }
    }
  }

  override fun onLogFilesAltered () { }

  override fun onClick (v: View) {
    val viewId = v.id
    if (viewId == R.id.btn_file) {
      val item = v.tag as ListItem
      val file = item.getData() as File
      showOptions(file.getName() + " (" + Strings.buildSize(file.length()) + ")", intArrayOf(R.id.btn_open, R.id.btn_share, R.id.btn_delete), arrayOf("View", "Share", "Delete"), intArrayOf(ViewController.OptionColor.NORMAL, ViewController.OptionColor.NORMAL, ViewController.OptionColor.RED), intArrayOf(R.drawable.baseline_visibility_24, R.drawable.baseline_forward_24, R.drawable.baseline_delete_24), OptionDelegate { _, id ->
        if (id == R.id.btn_open) {
          val c = TextController(context, tdlib)
          c.setArguments(TextController.Arguments.fromFile(file.getName(), file.getPath(), "text/plain"))
          navigateTo(c)
        } else if (id == R.id.btn_share) {
          val c = ShareController(context, tdlib)
          c.setArguments(ShareController.Args(file, "text/plain"))
          c.show()
        } else if (id == R.id.btn_delete) {
          val size = file.length()
          val isCrash = file.getName().startsWith(Log.CRASH_PREFIX)
          if (Log.deleteFile(file)) {
            UI.showToast("OK. Freed " + Strings.buildSize(size), Toast.LENGTH_SHORT)
            removeFile(file, size, isCrash)
          } else {
            UI.showToast("Failed", Toast.LENGTH_SHORT)
          }
        }
        true
      })
    }
  }

  private fun removeFile (file: File, size: Long, isCrash: Boolean) {
    if (files == null) {
      return
    }
    val i = files!!.files.indexOf(file)
    if (i != -1) {
      files!!.totalSize -= size
      if (isCrash) {
        files!!.crashesCount--
      } else {
        files!!.logsCount--
      }
      removeFileByPosition(i)
    }
  }

  private fun removeFileByPosition (position: Int) {
    files!!.files.removeAt(position)
    if (files!!.files.isEmpty()) {
      buildCells()
    } else if (position == 0) {
      adapter.getItems().removeAt(0)
      adapter.getItems().removeAt(0)
      adapter.notifyItemRangeRemoved(0, 2)
    } else if (position == files!!.files.size) { // TODO check removal of the last log in the list
      val count = adapter.getItems().size
      adapter.getItems().removeAt(count - 2)
      adapter.getItems().removeAt(count - 3)
      adapter.notifyItemRangeRemoved(count - 3, 2)
    } else {
      adapter.getItems().removeAt(position * 2 + 1)
      adapter.getItems().removeAt(position * 2)
      adapter.notifyItemRangeRemoved(position * 2, 2)
    }
  }

  private var files: Log.LogFiles? = null
  private var filesReceived = false

  private fun setFiles (files: Log.LogFiles?) {
    this.files = files
    this.filesReceived = true
    buildCells()
  }

  private fun buildCells () {
    val items = ArrayList<ListItem>()
    if (filesReceived) {
      if (files == null || files!!.isEmpty()) {
        items.add(ListItem(ListItem.TYPE_EMPTY, 0, 0, "Application Logs are empty", false))
      } else {
        var first = true
        for (file in files!!.files) {
          if (first) {
            first = false
          } else {
            items.add(ListItem(ListItem.TYPE_SEPARATOR_FULL))
          }
          items.add(ListItem(ListItem.TYPE_VALUED_SETTING, R.id.btn_file, 0, file.getName(), false).setData(file))
        }
        items.add(ListItem(ListItem.TYPE_SHADOW_BOTTOM))
      }
    }
    adapter.setItems(items, false)
  }
}
