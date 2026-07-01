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
package org.thunderdog.challegram.navigation

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowManager
import androidx.annotation.CallSuper
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.vkryl.android.DECELERATE_INTERPOLATOR
import me.vkryl.android.startAnimator
import me.vkryl.android.widget.FrameLayoutFix.Companion.newParams
import me.vkryl.core.collection.IntList
import me.vkryl.core.ensureCapacity
import org.drinkless.tdlib.TdApi.*
import org.thunderdog.challegram.R
import org.thunderdog.challegram.component.attach.CustomItemAnimator
import org.thunderdog.challegram.component.dialogs.SearchManager
import org.thunderdog.challegram.component.user.RemoveHelper
import org.thunderdog.challegram.config.Config
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.data.TGFoundChat
import org.thunderdog.challegram.data.TGFoundMessage
import org.thunderdog.challegram.telegram.TGLegacyManager
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.telegram.TdlibMessageViewer.Viewport
import org.thunderdog.challegram.telegram.TdlibUi.ChatOpenParameters
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.UI
import org.thunderdog.challegram.tool.Views
import org.thunderdog.challegram.ui.ListItem
import org.thunderdog.challegram.ui.SettingHolder
import org.thunderdog.challegram.ui.SettingsAdapter
import org.thunderdog.challegram.util.StringList
import org.thunderdog.challegram.v.CustomRecyclerView
import org.thunderdog.challegram.widget.BaseView.ActionListProvider
import org.thunderdog.challegram.widget.BetterChatView
import org.thunderdog.challegram.widget.ForceTouchView
import org.thunderdog.challegram.widget.ForceTouchView.ForceTouchContext
import org.thunderdog.challegram.widget.ListInfoView
import org.thunderdog.challegram.widget.VerticalChatView
import kotlin.math.min

abstract class TelegramViewController<T>(context: Context, tdlib: Tdlib?) : ViewController<T>(context, tdlib) {
  // Search chats
  private var chatSearchView: CustomRecyclerView? = null
  private var chatSearchViewport: Viewport? = null
  private var chatSearchAdapter: SettingsAdapter? = null
  private var chatSearchManager: SearchManager? = null
  private var chatSearchDisallowScreenshots = false

  /**
   * @return true, if event is consumed. False, if chat should be handled with default action (open)
   */
  protected open fun onFoundChatClick(view: View?, chat: TGFoundChat?): Boolean {
    return false
  }

  protected open fun canSelectFoundChat(chat: TGFoundChat?): Boolean {
    return false
  }

  protected open fun canInteractWithFoundChat(chat: TGFoundChat?): Boolean {
    return true
  }

  protected open val chatMessagesSearchChatList: ChatList?
    get() = null

  protected open val chatSearchFlags: Int
    get() = SearchManager.FLAG_NEED_TOP_CHATS

  protected open fun filterChatSearchResult(chat: Chat?): Boolean {
    return true
  }

  protected open fun filterChatMessageSearchResult(chat: Chat?): Boolean {
    return true
  }

  protected open fun modifyFoundChat(chat: TGFoundChat?) {}

  protected open fun modifyFoundChatView(item: ListItem?, position: Int, chatView: BetterChatView?) {}

  protected fun needChatSearchManagerPreparation(): Boolean {
    // Disable if it's not needed
    return true
  }

  private fun removeSuggestedChat(chat: TGFoundChat) {
    showOptions(
      Lang.getStringBold(R.string.ChatHintsDelete, chat.title),
      intArrayOf(R.id.btn_delete, R.id.btn_cancel),
      arrayOf(Lang.getString(R.string.Delete), Lang.getString(R.string.Cancel)),
      intArrayOf(OptionColor.RED, OptionColor.NORMAL),
      intArrayOf(R.drawable.baseline_delete_sweep_24, R.drawable.baseline_cancel_24),
      { itemView: View?, id: Int ->
        if (id == R.id.btn_delete) {
          chatSearchManager!!.removeTopChat(chat.id)
        }
        true
      })
  }

  private fun removeRecentChat(chat: TGFoundChat) {
    showOptions(
      Lang.getStringBold(R.string.DeleteXFromRecents, chat.title),
      intArrayOf(R.id.btn_delete, R.id.btn_cancel),
      arrayOf(Lang.getString(R.string.Delete), Lang.getString(R.string.Cancel)),
      intArrayOf(OptionColor.RED, OptionColor.NORMAL),
      intArrayOf(R.drawable.baseline_delete_24, R.drawable.baseline_cancel_24),
      { itemView: View?, id: Int ->
        if (id == R.id.btn_delete) {
          chatSearchManager!!.removeRecentlyFoundChat(chat)
        }
        true
      })
  }

  override fun dispatchSystemInsets(
    parentView: View?,
    originalParams: MarginLayoutParams?,
    legacyInsets: Rect?,
    insets: Rect,
    insetsWithoutIme: Rect,
    systemInsets: Rect?,
    systemInsetsWithoutIme: Rect?,
    fitsSystemWindows: Boolean
  ) {
    super.dispatchSystemInsets(parentView, originalParams, legacyInsets, insets, insetsWithoutIme, systemInsets, systemInsetsWithoutIme, fitsSystemWindows)
    setBottomInset(insets.bottom, insetsWithoutIme.bottom)
  }

  override fun onBottomInsetChanged(extraBottomInset: Int, extraBottomInsetWithoutIme: Int, isImeInset: Boolean) {
    super.onBottomInsetChanged(extraBottomInset, extraBottomInsetWithoutIme, isImeInset)
    if (chatSearchView != null) {
      chatSearchView!!.setPadding(0, 0, 0, extraBottomInsetWithoutIme)
    }
  }

  protected fun generateChatSearchView(parent: ViewGroup?): CustomRecyclerView? {
    val noChatSearch = (this.chatSearchFlags and SearchManager.FLAG_NO_CHATS) != 0
    chatSearchViewport = tdlib!!.messageViewer().createViewport(MessageSourceSearch(), this)
    chatSearchViewport!!.addIgnoreLock { !this.isSearchContentVisible }
    chatSearchView = Views.inflate(context(), R.layout.recycler_custom, parent) as CustomRecyclerView?
    chatSearchView!!.setClipToPadding(false)
    chatSearchView!!.setPadding(0, 0, 0, systemInsets.bottom)
    Views.setScrollBarPosition(chatSearchView)
    tdlib.ui().attachViewportToRecyclerView(chatSearchViewport!!, chatSearchView!!)
    chatSearchView!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
      override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
          hideSoftwareKeyboard()
        }
      }

      private var firstVisiblePosition = -1
      private var lastVisiblePosition = -1

      override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        if (dy != 0) {
          /*
 * (((LinearLayoutManager) recyclerView.getLayoutManager()).findFirstVisibleItemPosition() > 0) || ((LinearLayoutManager) recyclerView.getLayoutManager()).findLastVisibleItemPosition() < recyclerView.getAdapter().getItemCount() - 1
 * */
          hideSoftwareKeyboard()
        }
        val first = (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        val last = (recyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
        if (this.firstVisiblePosition != first || this.lastVisiblePosition != last) {
          this.firstVisiblePosition = first
          this.lastVisiblePosition = last

          var shouldDisallowScreenshots = false
          for (i in first..last) {
            val item = chatSearchAdapter!!.getItem(i)
            if (item != null && item.data is TGFoundMessage) {
              val message = (item.data as TGFoundMessage).message
              if (!message.canBeSaved) {
                shouldDisallowScreenshots = true
                break
              }
            }
          }
          if (chatSearchDisallowScreenshots != shouldDisallowScreenshots) {
            chatSearchDisallowScreenshots = shouldDisallowScreenshots
            context().checkDisallowScreenshots()
          }
        }
        if (last + 5 >= chatSearchAdapter!!.items.size) {
          chatSearchManager!!.loadMoreMessages()
        }
      }
    })
    chatSearchView!!.setBackgroundColor(Theme.backgroundColor())
    addThemeBackgroundColorListener(chatSearchView, ColorId.background)
    chatSearchView!!.setLayoutManager(LinearLayoutManager(context(), RecyclerView.VERTICAL, false))
    if (parent != null) {
      chatSearchView!!.setAlpha(0f)
      chatSearchView!!.setScrollDisabled(true)
    } else {
      isSearchContentVisible = true
    }
    chatSearchView!!.setLayoutParams(newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

    val previewProvider =
      ActionListProvider { v: View?, context: ForceTouchContext?, ids: IntList?, icons: IntList?, strings: StringList?, target: ViewController<*>? ->
        val item = v!!.tag as ListItem
        val data = item.data
        val chat: TGFoundChat
        if (data is TGFoundMessage) {
          chat = data.chat
        } else {
          chat = data as TGFoundChat
        }
        val canInteractWithFoundChat = canInteractWithFoundChat(chat) && navigationController != null
        if (navigationController == null) {
          context!!.setExcludeHeader(true)
        }

        val itemId = item.id
        if (itemId == R.id.search_chat_local || itemId == R.id.search_chat_top) {
          if (Config.CALL_FROM_PREVIEW && canInteractWithFoundChat && tdlib.cache().userGeneral(chat.userId)) {
            ids!!.append(R.id.btn_phone_call)
            strings!!.append(R.string.Call)
            icons!!.append(R.drawable.baseline_call_24)
          }
          val chatAvailable = tdlib.chatAvailable(chat.chat)
          if (chatAvailable) {
            ids!!.append(R.id.btn_notifications)
            val hasNotifications = tdlib.chatNotificationsEnabled(chat.id)
            strings!!.append(if (hasNotifications) R.string.Mute else R.string.Unmute)
            icons!!.append(if (hasNotifications) R.drawable.baseline_notifications_off_24 else R.drawable.baseline_notifications_24)
            if (canInteractWithFoundChat) {
              if (chat.list != null) {
                val isPinned = tdlib.chatPinned(chat.list, chat.id)
                ids.append(R.id.btn_pinUnpinChat)
                strings.append(if (isPinned) R.string.Unpin else R.string.Pin)
                icons.append(if (isPinned) R.drawable.deproko_baseline_pin_undo_24 else R.drawable.deproko_baseline_pin_24)
                if (tdlib.canArchiveOrUnarchiveChat(chat.chat)) {
                  val isArchived = tdlib.chatArchived(chat.id)
                  ids.append(R.id.btn_archiveUnarchiveChat)
                  strings.append(if (isArchived) R.string.Unarchive else R.string.Archive)
                  icons.append(if (isArchived) R.drawable.baseline_unarchive_24 else R.drawable.baseline_archive_24)
                }
              }
              val canRead = tdlib.canMarkAsRead(chat.chat)
              ids.append(if (canRead) R.id.btn_markChatAsRead else R.id.btn_markChatAsUnread)
              strings.append(if (canRead) R.string.MarkAsRead else R.string.MarkAsUnread)
              icons.append(if (canRead) Config.ICON_MARK_AS_READ else Config.ICON_MARK_AS_UNREAD)
              if (chat.hasHighlight()) {
                ids.append(R.id.btn_removeChatFromListOrClearHistory)
                strings.append(R.string.Delete)
                icons.append(R.drawable.baseline_delete_24)
              }
            }
          }
          if (!chat.hasHighlight()) {
            ids!!.append(R.id.btn_delete)
            strings!!.append(R.string.Remove)
            icons!!.append(R.drawable.baseline_delete_sweep_24)
          }
        } else if (itemId == R.id.search_chat_global) { // Nothing?
        }

        if (canSelectFoundChat(chat)) {
          ids!!.append(R.id.btn_selectChat)
          strings!!.append(R.string.Select)
          icons!!.append(R.drawable.baseline_playlist_add_check_24)
        }
        object : ForceTouchView.ActionListener {
          override fun onForceTouchAction(context: ForceTouchContext?, actionId: Int, arg: Any?) {
            if (actionId == R.id.btn_phone_call) {
              tdlib.context().calls().makeCallDelayed(this@TelegramViewController, chat.userId, null, true)
            } else if (actionId == R.id.btn_pinUnpinChat || actionId == R.id.btn_archiveUnarchiveChat || actionId == R.id.btn_notifications || actionId == R.id.btn_markChatAsRead || actionId == R.id.btn_markChatAsUnread || actionId == R.id.btn_removeChatFromListOrClearHistory || actionId == R.id.btn_removePsaChatFromList) {
              tdlib.ui().processChatAction(
                this@TelegramViewController,
                chat.list,
                chat.chatId,
                chat.messageThread,
                MessageSourceSearch(),
                actionId,
                null
              )
            } else if (actionId == R.id.btn_selectChat) {
              onFoundChatClick(v, chat)
            } else if (actionId == R.id.btn_delete) {
              val itemId = item.id
              if (itemId == R.id.search_chat_top) {
                removeSuggestedChat(chat)
              } else if (itemId == R.id.search_chat_local) {
                removeRecentChat(chat)
              }
            }
          }

          override fun onAfterForceTouchAction(context: ForceTouchContext?, actionId: Int, arg: Any?) {
          }
        }
      }

    val topChatsAdapter: SettingsAdapter = object : SettingsAdapter(this, View.OnClickListener { v: View? ->
      val item = v!!.tag as ListItem
      val itemId = item.id
      if (itemId == R.id.search_chat_top) {
        val chat = item.data as TGFoundChat
        if (chat.id != 0L && !onFoundChatClick(v, chat)) {
          tdlib.ui().openChat(this@TelegramViewController, chat.id, null)
        }
      }
    }, this) {
      override fun setChatData(item: ListItem, chatView: VerticalChatView) {
        chatView.setPreviewActionListProvider(previewProvider)
        chatView.setChat(item.data as TGFoundChat?)
      }
    }
    topChatsAdapter.setOnLongClickListener { v: View? ->
      val item = v!!.tag as ListItem
      val itemId = item.id
      if (itemId == R.id.search_chat_top) {
        val chat = item.data as TGFoundChat
        removeSuggestedChat(chat)
        return@setOnLongClickListener true
      }
      false
    }

    chatSearchAdapter = object : SettingsAdapter(this, View.OnClickListener { v: View? ->
      val listItem = v!!.tag as ListItem
      val listItemId = listItem.id
      if (listItemId == R.id.search_section_local) {
        if (!chatSearchManager!!.areLocalChatsRecent()) {
          return@OnClickListener
        }
        showOptions(
          Lang.getString(R.string.ClearRecentsHint),
          intArrayOf(R.id.btn_delete, R.id.btn_cancel),
          arrayOf(Lang.getString(R.string.Clear), Lang.getString(R.string.Cancel)),
          intArrayOf(OptionColor.RED, OptionColor.NORMAL),
          intArrayOf(R.drawable.baseline_delete_24, R.drawable.baseline_cancel_24),
          { itemView: View?, id: Int ->
            if (id == R.id.btn_delete) {
              chatSearchManager!!.clearRecentlyFoundChats()
            }
            true
          })
      } else if (listItemId == R.id.search_chat_local || listItemId == R.id.search_chat_global) {
        val chat = listItem.data as TGFoundChat
        if (listItem.id == R.id.search_chat_global) {
          preventLeavingSearchMode()
        }
        if (chat.id != 0L) {
          chatSearchManager!!.addRecentlyFoundChat(chat)
          if (!onFoundChatClick(v, chat)) {
            tdlib.ui().openChat(this@TelegramViewController, chat.id, null)
          }
        } else if (chat.userId != 0L) {
          tdlib.ui().openPrivateChat(
            this@TelegramViewController,
            chat.userId,
            ChatOpenParameters().noOpen().after { createdChatId: Long ->
              chat.createdChatId = createdChatId
              chatSearchManager!!.addRecentlyFoundChat(chat)
              if (!onFoundChatClick(v, chat)) {
                tdlib.ui().openChat(this@TelegramViewController, createdChatId, null)
              }
            }
          )
        }
      } else if (listItemId == R.id.search_message) {
        val message = listItem.data as TGFoundMessage
        val rawMessage = message.message
        preventLeavingSearchMode()
        val query = lastSearchInput
        tdlib.ui().openChat(this@TelegramViewController, rawMessage.chatId, ChatOpenParameters().foundMessage(query, rawMessage).keepStack())
      }
    }, this) {
      override fun onEmojiUpdated(isPackSwitch: Boolean) {
        for (parentView in parentViews) {
          val manager = parentView.layoutManager as LinearLayoutManager?
          val first = manager!!.findFirstVisibleItemPosition()
          val last = manager.findLastVisibleItemPosition()
          for (i in first..last) {
            val view = manager.findViewByPosition(i)
            if (view is BetterChatView) {
              view.invalidate()
            }
          }
          if (first > 0) {
            notifyItemRangeChanged(0, first)
          }
          if (last < itemCount - 1) {
            notifyItemRangeChanged(last, itemCount - last)
          }
        }
      }

      override fun setRecyclerViewData(item: ListItem, recyclerView: RecyclerView, isInitialization: Boolean) {
        val itemId = item.id
        if (itemId == R.id.search_top) {
          if (recyclerView.adapter !== topChatsAdapter) {
            recyclerView.setItemAnimator(CustomItemAnimator(DECELERATE_INTERPOLATOR, 180L))
            recyclerView.setAdapter(topChatsAdapter)
          }
        }
      }

      override fun setChatData(item: ListItem, position: Int, chatView: BetterChatView) {
        chatView.setPreviewActionListProvider(previewProvider)
        val itemId = item.id
        if (itemId == R.id.search_chat_local || itemId == R.id.search_chat_global) {
          chatView.setChat(item.data as TGFoundChat?)
        } else if (itemId == R.id.search_message) {
          chatView.setMessage(item.data as TGFoundMessage?)
        }
        this@TelegramViewController.modifyFoundChatView(item, position, chatView)
      }

      override fun setInfo(item: ListItem?, position: Int, infoView: ListInfoView) {
        if (chatSearchManager!!.isEndReached) {
          infoView.showInfo(Lang.pluralBold(R.string.xMessages, chatSearchManager!!.foundMessagesCount.toLong()))
        } else {
          infoView.showProgress()
        }
      }
    }
    TGLegacyManager.instance().addEmojiListener(chatSearchAdapter)
    chatSearchAdapter!!.setOnLongClickListener { v: View? ->
      val chatViewId = v!!.id
      if (chatViewId == R.id.search_chat_local) {
        val item = v.tag as ListItem
        if (!item.boolValue) {
          return@setOnLongClickListener false
        }
        val chat = item.data as TGFoundChat
        showOptions(
          Lang.getStringBold(R.string.DeleteXFromRecents, chat.title),
          intArrayOf(R.id.btn_delete, R.id.btn_cancel),
          arrayOf(Lang.getString(R.string.Delete), Lang.getString(R.string.Cancel)),
          intArrayOf(OptionColor.RED, OptionColor.NORMAL),
          intArrayOf(R.drawable.baseline_delete_24, R.drawable.baseline_cancel_24),
          { itemView: View?, id: Int ->
            if (id == R.id.btn_delete) {
              chatSearchManager!!.removeRecentlyFoundChat(chat)
            }
            true
          })
        return@setOnLongClickListener true
      }
      false
    }
    if (!noChatSearch) {
      chatSearchAdapter!!.setItems(arrayOf(ListItem(ListItem.TYPE_EMPTY_OFFSET_SMALL)), false)
    }

    val chatSearchViewAnimator = chatSearchView!!.itemAnimator
    if (!DEBUG_CHATS_SEARCH_ADAPTER) {
      if (!noChatSearch) {
        chatSearchView!!.setItemAnimator(null)
      }
    }
    if (parent == null) {
      chatSearchView!!.setAdapter(chatSearchAdapter)
    }

    chatSearchManager = SearchManager(tdlib, object : SearchManager.Listener() {
      override fun onOpen() {
        setNeedPreventKeyboardLag(true)
        if (chatSearchView!!.adapter != null) {
          chatSearchAdapter!!.resetRecyclerScrollById(R.id.search_top)
          (chatSearchView!!.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(0, 0)
        }
      }

      override fun customFilter(chat: Chat?): Boolean {
        return filterChatSearchResult(chat)
      }

      override fun filterMessageSearchResultSource(chat: Chat?): Boolean {
        return this@TelegramViewController.filterChatMessageSearchResult(chat)
      }

      override fun modifyFoundChat(foundChat: TGFoundChat?) {
        this@TelegramViewController.modifyFoundChat(foundChat)
      }

      override fun onClose() {
        setNeedPreventKeyboardLag(false)
        if (chatSearchView!!.adapter != null) {
          Views.destroyRecyclerView(chatSearchView)
          chatSearchView!!.setAdapter(null)
        }
      }

      override fun onPerformNewSearch(isDefault: Boolean) {
        if (!DEBUG_CHATS_SEARCH_ADAPTER) {
          if (!noChatSearch) {
            chatSearchView!!.setItemAnimator(null)
          }
        }
      }

      fun updateTopChatsAdapter(topChats: ArrayList<TGFoundChat>) {
        val chatItems = ArrayList<ListItem?>(topChats.size)
        for (chat in topChats) {
          chatItems.add(ListItem(ListItem.TYPE_CHAT_VERTICAL, R.id.search_chat_top).setData(chat).setLongId(chat.id))
        }
        topChatsAdapter.replaceItems(chatItems)
      }

      override fun onAddTopChats(topChats: ArrayList<TGFoundChat>, updateData: Boolean, isSilent: Boolean): Boolean {
        if (updateData) {
          updateTopChatsAdapter(topChats)
        }
        if (!isSilent && chatSearchAdapter!!.indexOfViewById(R.id.search_section_top) == -1) {
          val startIndex = 1 // chatSearchAdapter.getItems().size();
          val addedCount = generateTopCells(chatSearchAdapter!!.items, startIndex)
          chatSearchAdapter!!.notifyItemRangeInserted(startIndex, addedCount)
          return true
        }
        return false
      }

      override fun onRemoveTopChats(updateData: Boolean, isSilent: Boolean) {
        if (updateData) {
          topChatsAdapter.replaceItems(null)
        }
        if (!isSilent) {
          val i = chatSearchAdapter!!.indexOfViewById(R.id.search_section_top)
          if (i != -1) {
            chatSearchAdapter!!.removeRange(i, 4)
          }
        }
      }

      override fun onRemoveTopChat(chatId: Long) {
        topChatsAdapter.removeItemByLongId(chatId)
      }

      override fun onUpdateTopChats(oldChatIds: LongArray?, newChatIds: LongArray?) {
        updateTopChatsAdapter(chatSearchManager!!.topChats)
      }

      override fun onAddLocalChats(localChats: ArrayList<TGFoundChat>?) {
        if (chatSearchAdapter!!.indexOfViewById(R.id.search_section_local) == -1) {
          var startIndex = chatSearchAdapter!!.indexOfViewById(R.id.search_section_messages)
          if (startIndex == -1) {
            startIndex = chatSearchAdapter!!.indexOfViewById(R.id.search_section_global)
            if (startIndex == -1) {
              startIndex = chatSearchAdapter!!.items.size
            }
          }
          val addedCount = generateChatsCells(
            startIndex,
            chatSearchAdapter!!.items,
            localChats,
            R.id.search_section_local,
            R.id.search_chat_local,
            if (chatSearchManager!!.areLocalChatsRecent()) R.string.Recent else R.string.ChatsAndContacts,
            chatSearchManager!!.areLocalChatsRecent()
          )
          chatSearchAdapter!!.notifyItemRangeInserted(startIndex, addedCount)
        }
      }

      override fun onAddMoreLocalChats(addedLocalChats: ArrayList<TGFoundChat>, oldChatCount: Int) {
        var startIndex = chatSearchAdapter!!.indexOfViewById(R.id.search_section_local)
        if (startIndex != -1) {
          startIndex += oldChatCount * 2 + 1
          var addedCount = 0
          val items = chatSearchAdapter!!.items
          items.ensureCapacity(items.size + addedLocalChats.size * 2)
          for (chat in addedLocalChats) {
            items.add(startIndex + addedCount++, ListItem(ListItem.TYPE_SEPARATOR))
            items.add(startIndex + addedCount++, searchValueOf(R.id.search_chat_local, chat, chatSearchManager!!.areLocalChatsRecent())!!)
          }
          chatSearchAdapter!!.notifyItemRangeInserted(startIndex, addedCount)
        }
      }

      override fun onRemoveLocalChats(oldChatCount: Int) {
        val i = chatSearchAdapter!!.indexOfViewById(R.id.search_section_local)
        if (i != -1) {
          chatSearchAdapter!!.removeRange(i, 3 + (oldChatCount - 1) * 2 + 1)
        }
      }

      override fun onUpdateLocalChats(oldChatCount: Int, localChats: ArrayList<TGFoundChat>) {
        var startIndex = chatSearchAdapter!!.indexOfViewById(R.id.search_section_local)
        if (startIndex == -1) {
          return
        }

        val headerItem = chatSearchAdapter!!.items[startIndex]
        val canClear = chatSearchManager!!.areLocalChatsRecent()
        if (canClear != (headerItem.viewType == ListItem.TYPE_HEADER_WITH_ACTION)) {
          headerItem.setViewType(if (canClear) ListItem.TYPE_HEADER_WITH_ACTION else ListItem.TYPE_HEADER)
          headerItem.setString(if (canClear) R.string.Recent else R.string.ChatsAndContacts)
          chatSearchAdapter!!.notifyItemChanged(startIndex)
        }

        startIndex += 2

        val newChatCount = localChats.size
        val changedChatCount = min(oldChatCount, newChatCount)
        for (i in 0..<changedChatCount) {
          val position = startIndex + i * 2
          val item = chatSearchAdapter!!.items[position]
          check(item.viewType == ListItem.TYPE_CHAT_BETTER) { "Bug, viewType: " + item.viewType }
          val chat = localChats[i]
          item.setData(chat).setLongId(chat.id).setBoolValue(canClear)
        }
        chatSearchAdapter!!.notifyItemRangeChanged(startIndex, 1 + (changedChatCount - 1) * 2)

        startIndex += 1 + (changedChatCount - 1) * 2

        if (newChatCount > oldChatCount) {
          var index = startIndex
          val addedChatCount = newChatCount - oldChatCount
          for (i in 0..<addedChatCount) {
            val chat = localChats[i + changedChatCount]
            chatSearchAdapter!!.items.add(index++, ListItem(ListItem.TYPE_SEPARATOR))
            chatSearchAdapter!!.items.add(index++, searchValueOf(R.id.search_chat_local, chat, canClear))
          }
          chatSearchAdapter!!.notifyItemRangeInserted(startIndex, index - startIndex)
        } else if (newChatCount < oldChatCount) {
          val removedChatCount = oldChatCount - newChatCount
          chatSearchAdapter!!.removeRange(startIndex, removedChatCount * 2)
        }
      }

      override fun onRemoveLocalChat(chatId: Long, position: Int, totalCount: Int) {
        var i = chatSearchAdapter!!.indexOfViewById(R.id.search_section_local)
        if (i != -1) {
          i += 2
          if (position == 0) {
            chatSearchAdapter!!.removeRange(i, 2)
          } else {
            chatSearchAdapter!!.removeRange(i + position * 2 - 1, 2)
          }
        }
      }

      override fun onAddLocalChat(chat: TGFoundChat) {
        var i = chatSearchAdapter!!.indexOfViewById(R.id.search_section_local)
        if (i != -1) {
          i += 2
          chatSearchAdapter!!.items.add(i, ListItem(ListItem.TYPE_SEPARATOR))
          chatSearchAdapter!!.items.add(i, searchValueOf(R.id.search_chat_local, chat, true))
          chatSearchAdapter!!.notifyItemRangeInserted(i, 2)
        }
      }

      override fun onMoveLocalChat(chat: TGFoundChat?, fromPosition: Int, totalCount: Int) {
        var i = chatSearchAdapter!!.indexOfViewById(R.id.search_section_local)
        if (i != -1) {
          i += 2
          val adapterPosition = i + fromPosition * 2
          val chatItem = chatSearchAdapter!!.items.removeAt(adapterPosition)
          val shadowItem: ListItem?
          if (fromPosition != totalCount - 1) {
            shadowItem = chatSearchAdapter!!.items.removeAt(adapterPosition)
            chatSearchAdapter!!.notifyItemRangeRemoved(adapterPosition, 2)
          } else {
            shadowItem = chatSearchAdapter!!.items.removeAt(adapterPosition - 1)
            chatSearchAdapter!!.notifyItemRangeRemoved(adapterPosition - 1, 2)
          }
          chatSearchAdapter!!.items.add(i, shadowItem)
          chatSearchAdapter!!.items.add(i, chatItem)
          chatSearchAdapter!!.notifyItemRangeInserted(i, 2)
        }
      }

      override fun onAddGlobalChats(globalChats: ArrayList<TGFoundChat>?) {
        if (chatSearchAdapter?.indexOfViewById(R.id.search_section_global) == -1) {
          var startIndex = chatSearchAdapter!!.indexOfViewById(R.id.search_section_messages)
          if (startIndex == -1) {
            startIndex = chatSearchAdapter!!.items.size
          }
          val addedCount = generateChatsCells(
            startIndex,
            chatSearchAdapter!!.items,
            globalChats,
            R.id.search_section_global,
            R.id.search_chat_global,
            R.string.GlobalSearch,
            false
          )
          chatSearchAdapter?.notifyItemRangeInserted(startIndex, addedCount)
        }
      }

      override fun onRemoveGlobalChats(oldChatCount: Int) {
        val startPosition = chatSearchAdapter?.indexOfViewById(R.id.search_section_global) ?: -1
        if (startPosition != -1) {
          chatSearchAdapter?.removeRange(startPosition, 3 + (oldChatCount - 1) * 2 + 1)
        }
      }

      override fun onUpdateGlobalChats(oldChatCount: Int, globalChats: ArrayList<TGFoundChat>) {
        var startIndex = chatSearchAdapter?.indexOfViewById(R.id.search_section_global) ?: -1
        if (startIndex == -1) {
          return
        }

        startIndex += 2

        val newChatCount = globalChats.size
        val changedChatCount = min(oldChatCount, newChatCount)
        for (i in 0..<changedChatCount) {
          val position = startIndex + i * 2
          val item = chatSearchAdapter!!.items[position]
          check(item.viewType == ListItem.TYPE_CHAT_BETTER) { "Bug, viewType: " + item.viewType }
          val chat = globalChats[i]
          item.setData(chat).setLongId(chat.id).setBoolValue(false)
        }
        chatSearchAdapter!!.notifyItemRangeChanged(startIndex, 1 + (changedChatCount - 1) * 2)

        startIndex += 1 + (changedChatCount - 1) * 2

        if (newChatCount > oldChatCount) {
          var index = startIndex
          val addedChatCount = newChatCount - oldChatCount
          for (i in 0..<addedChatCount) {
            val chat = globalChats[i + changedChatCount]
            chatSearchAdapter?.items?.add(index++, ListItem(ListItem.TYPE_SEPARATOR))
            chatSearchAdapter?.items
              ?.add(index++, ListItem(ListItem.TYPE_CHAT_BETTER, R.id.search_chat_global).setData(chat).setLongId(chat.id))
          }
          chatSearchAdapter!!.notifyItemRangeInserted(startIndex, index - startIndex)
        } else if (newChatCount < oldChatCount) {
          val removedChatCount = oldChatCount - newChatCount
          chatSearchAdapter!!.removeRange(startIndex, removedChatCount * 2)
        }
      }

      override fun onAddMessages(messages: ArrayList<TGFoundMessage?>) {
        if (chatSearchAdapter!!.indexOfViewById(R.id.search_section_messages) == -1) {
          generateMessagesCells(chatSearchAdapter!!.items, messages, 0, messages.size)
        }
      }

      override fun onUpdateMessages(oldMessageCount: Int, messages: ArrayList<TGFoundMessage>) {
        var startIndex = chatSearchAdapter!!.indexOfViewById(R.id.search_section_messages)
        if (startIndex == -1) {
          return
        }

        startIndex += 2

        val newMessageCount = messages.size
        val changedChatCount = min(oldMessageCount, newMessageCount)
        for (i in 0..<changedChatCount) {
          val position = startIndex + i * 2
          val item = chatSearchAdapter?.items[position]
          check(item?.viewType == ListItem.TYPE_CHAT_BETTER) { "Bug, viewType: " + item?.viewType }
          val message = messages[i]
          item.setData(message).setLongId(message.id).setBoolValue(false)
        }
        chatSearchAdapter!!.notifyItemRangeChanged(startIndex, 1 + (changedChatCount - 1) * 2)

        startIndex += 1 + (changedChatCount - 1) * 2

        if (newMessageCount > oldMessageCount) {
          var index = startIndex
          val addedChatCount = newMessageCount - oldMessageCount
          for (i in 0..<addedChatCount) {
            val message = messages[i + changedChatCount]
            chatSearchAdapter?.items?.add(index++, ListItem(ListItem.TYPE_SEPARATOR))
            chatSearchAdapter?.items?.add(index++, searchValueOf(message))
          }
          chatSearchAdapter?.notifyItemRangeInserted(startIndex, index - startIndex)
        } else if (newMessageCount < oldMessageCount) {
          val removedChatCount = oldMessageCount - newMessageCount
          chatSearchAdapter?.removeRange(startIndex, removedChatCount * 2)
        }
      }

      override fun onRemoveMessages(oldMessageCount: Int) {
        val startPosition = chatSearchAdapter?.indexOfViewById(R.id.search_section_messages) ?: -1
        if (startPosition != -1) {
          chatSearchAdapter?.removeRange(startPosition, 4 + (oldMessageCount - 1) * 2 + 1)
        }
      }

      override fun onAddMoreMessages(oldMessageCount: Int, messages: ArrayList<TGFoundMessage?>) {
        var i = chatSearchAdapter?.indexOfViewById(R.id.search_section_messages) ?: -1
        if (i != -1) {
          val startIndex = i + 2 + (oldMessageCount - 1) * 2 + 1
          i = startIndex
          for (j in oldMessageCount..<messages.size) {
            chatSearchAdapter?.items?.add(i++, ListItem(ListItem.TYPE_SEPARATOR))
            chatSearchAdapter?.items?.add(i++, searchValueOf(messages[j]!!))
          }
          chatSearchAdapter?.notifyItemRangeInserted(startIndex, i - startIndex)
        }
      }

      override fun getMessagesHeightOffset(): Int {
        var totalHeight = 0
        for (item in chatSearchAdapter!!.items) {
          if (item.id == R.id.search_section_messages) {
            break
          }
          totalHeight += SettingHolder.measureHeightForType(item.viewType)
        }
        return totalHeight
      }

      override fun onHeavyPartReached(currentContextId: Int) {
        if (!DEBUG_CHATS_SEARCH_ADAPTER) {
          if (!noChatSearch) {
            tdlib.ui().post { chatSearchView!!.setItemAnimator(chatSearchViewAnimator) }
          }
        }
      }

      override fun onHeavyPartFinished(currentContextId: Int) {
        /*TGDataManager.runOnUiThread(new Runnable() {
  @Override
  public void run () {
    if (currentContextId == chatSearchManager.getContextId()) {
      chatSearchView.setItemAnimator(null);
    }
  }
}, 360);*/
      }

      override fun onEndReached() {
        chatSearchAdapter?.updateValuedSettingById(R.id.search_counter)
        if (chatSearchAdapter?.items.isNullOrEmpty()) {
          chatSearchAdapter?.setItems(
            arrayOf(
              ListItem(ListItem.TYPE_EMPTY, 0, 0, R.string.NothingFound)
            ), false
          )
        }
      }
    })
    chatSearchManager!!.searchFlags = this.chatSearchFlags

    RemoveHelper.attach(chatSearchView, object : RemoveHelper.Callback {
      override fun canRemove(recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder, position: Int): Boolean {
        val item = viewHolder.itemView.tag as ListItem?
        return item != null && item.viewType == ListItem.TYPE_CHAT_BETTER && item.boolValue
      }

      override fun onRemove(viewHolder: RecyclerView.ViewHolder) {
        val item = viewHolder.itemView.tag as ListItem
        val chat = item.data as TGFoundChat
        removeRecentChat(chat)
      }
    })

    parent?.addView(chatSearchView)
    if (needChatSearchManagerPreparation()) {
      chatSearchManager!!.onPrepare(this.chatMessagesSearchChatList!!, this.chatSearchInitialQuery!!)
    }
    return chatSearchView
  }

  protected fun getChatSearchView(): RecyclerView? {
    return chatSearchView
  }

  protected open val chatSearchInitialQuery: String?
    get() = ""

  protected var isSearchAntagonistHidden: Boolean = false
    private set(isHidden) {
      if (field != isHidden) {
        field = isHidden
        getSearchAntagonistView()?.setVisibility(if (isHidden) View.INVISIBLE else View.VISIBLE)
      }
    }

  protected open fun getSearchAntagonistView(): View? {
    throw RuntimeException("Stub!")
  }

  private var isSearchContentVisible = false

  private fun setSearchContentVisible(isVisible: Boolean) {
    if (this.isSearchContentVisible != isVisible) {
      this.isSearchContentVisible = isVisible
      chatSearchView?.setScrollDisabled(!isVisible)
      chatSearchViewport?.notifyLockValueChanged()
      context().checkDisallowScreenshots()
    }
  }

  private fun generateTopCells(items: MutableList<ListItem>, index: Int): Int {
    var index = index
    val startCount = items.size
    val topChats = chatSearchManager!!.topChats
    if (topChats != null && topChats.isNotEmpty()) {
      items.ensureCapacity(items.size + 5)
      if (items.isEmpty()) {
        items.add(index++, ListItem(ListItem.TYPE_EMPTY_OFFSET_SMALL))
      }
      items.add(
        index++,
        ListItem(
          ListItem.TYPE_HEADER,
          R.id.search_section_top,
          0,
          if ((this.chatSearchFlags and SearchManager.FLAG_TOP_SEARCH_CATEGORY_GROUPS) != 0) R.string.Groups else R.string.People
        )
      )
      items.add(index++, ListItem(ListItem.TYPE_SHADOW_TOP))
      items.add(index++, ListItem(ListItem.TYPE_RECYCLER_HORIZONTAL, R.id.search_top))
      items.add(index++, ListItem(ListItem.TYPE_SHADOW_BOTTOM))
    }
    return items.size - startCount
  }

  private fun generateChatsCells(
    index: Int,
    items: MutableList<ListItem>,
    chats: ArrayList<TGFoundChat>?,
    @IdRes headerId: Int,
    @IdRes itemId: Int,
    @StringRes headerRes: Int,
    canClear: Boolean
  ): Int {
    var index = index
    val startSize = items.size
    if (!chats.isNullOrEmpty()) {
      items.ensureCapacity(items.size + (chats.size - 1) * 2 + 5)
      if (items.isEmpty()) {
        items.add(index++, ListItem(ListItem.TYPE_EMPTY_OFFSET_SMALL))
      }
      items.add(
        index++,
        ListItem(if (canClear) ListItem.TYPE_HEADER_WITH_ACTION else ListItem.TYPE_HEADER, headerId, R.drawable.baseline_clear_all_24, headerRes)
      )
      items.add(index++, ListItem(ListItem.TYPE_SHADOW_TOP))
      var isFirst = true
      for (chat in chats) {
        if (isFirst) {
          isFirst = false
        } else {
          items.add(index++, ListItem(ListItem.TYPE_SEPARATOR))
        }
        items.add(index++, searchValueOf(itemId, chat, canClear)!!)
      }
      items.add(index++, ListItem(ListItem.TYPE_SHADOW_BOTTOM))
    }
    return items.size - startSize
  }

  private fun generateMessagesCells(items: MutableList<ListItem>, messages: ArrayList<TGFoundMessage?>?, dataStartIndex: Int, dataCount: Int) {
    val isInitialChunk = dataStartIndex == 0
    val startIndex = if (isInitialChunk) items.size else items.size - 2
    var index = startIndex
    var addedInitialCell = false

    if (!messages.isNullOrEmpty()) {
      if (isInitialChunk) {
        items.ensureCapacity(items.size + (messages.size - 1) * 2 + 6)
        if (items.isEmpty()) {
          items.add(index++, ListItem(ListItem.TYPE_EMPTY_OFFSET_SMALL))
          addedInitialCell = true
        }
      } else {
        items.ensureCapacity(items.size + (messages.size - 1) + 1)
      }
      if (isInitialChunk) {
        items.add(
          index++, ListItem(
          ListItem.TYPE_HEADER, R.id.search_section_messages, 0,
          this.chatMessagesSearchTitle
        )
        )
        items.add(index++, ListItem(ListItem.TYPE_SHADOW_TOP))
      }
      var isFirst = true
      for (i in dataStartIndex..<dataStartIndex + dataCount) {
        val message = messages[i]
        if (isFirst) {
          isFirst = false
        } else {
          items.add(index++, ListItem(ListItem.TYPE_SEPARATOR))
        }
        items.add(index++, ListItem(ListItem.TYPE_CHAT_BETTER, R.id.search_message).setData(message))
      }
    }

    if (isInitialChunk) {
      items.add(index++, ListItem(ListItem.TYPE_SHADOW_BOTTOM))
      items.add(index++, ListItem(ListItem.TYPE_LIST_INFO_VIEW, R.id.search_counter))
    }

    val addedCount = index - startIndex

    if (addedInitialCell) {
      chatSearchAdapter?.notifyItemRemoved(0)
    }
    chatSearchAdapter?.notifyItemRangeInserted(startIndex, addedCount)
  }

  @get:StringRes
  protected open val chatMessagesSearchTitle: Int
    get() = R.string.general_Messages

  protected fun forceOpenChatSearch(query: String) {
    // enterSearchMode();
    if (chatSearchView?.adapter == null) chatSearchView?.setAdapter(chatSearchAdapter)
    setSearchTransformFactor(1f, true)
    chatSearchManager?.onOpen(this.chatMessagesSearchChatList)
    // onEnterSearchMode();
    // updateSearchMode(true);
    forceSearchChats(query)
  }

  protected val isChatSearchOpen: Boolean
    get() = chatSearchManager != null && chatSearchManager!!.isOpen

  protected fun forceCloseChatSearch() {
    // updateSearchMode(false);
    // onLeaveSearchMode();
    setSearchTransformFactor(0f, false)
    // leaveSearchMode();
    chatSearchManager?.onClose(this.chatMessagesSearchChatList)
    chatSearchView?.setAdapter(null)
  }

  protected fun forceSearchChats(query: String) {
    chatSearchManager?.onQueryChanged(this.chatMessagesSearchChatList!!, query)
  }

  private var needPreventKeyboardLag = false

  private fun setNeedPreventKeyboardLag(needPreventKeyboardLag: Boolean) {
    if (this.needPreventKeyboardLag != needPreventKeyboardLag) {
      this.needPreventKeyboardLag = needPreventKeyboardLag
      UI.setSoftInputMode(context, if (needPreventKeyboardLag) WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN else Config.DEFAULT_WINDOW_PARAMS)
    }
  }

  @CallSuper
  override fun handleLanguageDirectionChange() {
    super.handleLanguageDirectionChange()
    Views.setScrollBarPosition(chatSearchView)
  }

  @CallSuper
  override fun onLanguagePackEvent(event: Int, arg1: Int) {
    super.onLanguagePackEvent(event, arg1)
    chatSearchAdapter?.onLanguagePackEvent(event, arg1)
  }

  @CallSuper
  override fun onEnterSearchMode() {
    super.onEnterSearchMode()
    chatSearchManager?.onOpen(this.chatMessagesSearchChatList)
  }

  override fun startHeaderTransformAnimator(animator: ValueAnimator, mode: Int, open: Boolean) {
    chatSearchView?.also {
      if (it.adapter == null && mode == HeaderView.TRANSFORM_MODE_SEARCH && open) {
        animator.addListener(object : AnimatorListenerAdapter() {
          override fun onAnimationStart(animation: Animator) {
            onChatSearchOpenStarted()
          }
        })
        startAnimator(it, animator, true)
        chatSearchView?.setAdapter(chatSearchAdapter)
        return@also
      }
    }
    super.startHeaderTransformAnimator(animator, mode, open)
  }

  protected open fun onChatSearchOpenStarted() {}

  @CallSuper
  override fun applySearchTransformFactor(factor: Float, isOpening: Boolean) {
    super.applySearchTransformFactor(factor, isOpening)
    chatSearchManager?.let {
      chatSearchView?.alpha = factor
      isSearchAntagonistHidden = factor == 1f
      setSearchContentVisible(factor != 0f)
      // setNeedPreventKeyboardLag(factor != 0f && factor != 1f);
    }
  }

  override val rootColorId: Int
    get() = if (getSearchTransformFactor() != 0f && chatSearchManager != null) ColorId.background else super.rootColorId

  @CallSuper
  override fun shouldDisallowScreenshots(): Boolean {
    return (isSearchContentVisible && chatSearchDisallowScreenshots) || super.shouldDisallowScreenshots()
  }

  protected fun invalidateChatSearchResults() {
    chatSearchManager?.reloadSearchResults(this.chatMessagesSearchChatList)
  }

  protected fun inChatSearchMode(): Boolean {
    return chatSearchManager != null
  }

  override fun useGraySearchHeader(): Boolean {
    return chatSearchManager != null || super.useGraySearchHeader()
  }

  @CallSuper
  override fun onSearchInputChanged(query: String) {
    super.onSearchInputChanged(query)
    chatSearchManager?.onQueryChanged(this.chatMessagesSearchChatList!!, query)
  }

  @CallSuper
  override fun needPreventiveKeyboardHide(): Boolean {
    return inSearchMode() && inChatSearchMode()
  }

  override fun onAfterLeaveSearchMode() {
    super.onAfterLeaveSearchMode()
    chatSearchManager?.let {
      it.onClose(this.chatMessagesSearchChatList)
      clearSearchInput()
    }
  }

  override fun destroy() {
    super.destroy()
    if (chatSearchManager != null) {
      TGLegacyManager.instance().removeEmojiListener(chatSearchAdapter)
      Views.destroyRecyclerView(chatSearchView)
    }
    chatSearchViewport?.performDestroy()
  }

  companion object {
    @JvmStatic
    protected fun searchValueOf(@IdRes id: Int, chat: TGFoundChat, canClear: Boolean): ListItem? {
      return ListItem(ListItem.TYPE_CHAT_BETTER, id).setData(chat).setLongId(chat.id).setBoolValue(canClear)
    }

    private fun searchValueOf(message: TGFoundMessage): ListItem? {
      return ListItem(ListItem.TYPE_CHAT_BETTER, R.id.search_message).setData(message).setLongId(message.id)
    }

    private const val DEBUG_CHATS_SEARCH_ADAPTER = false
  }
}
