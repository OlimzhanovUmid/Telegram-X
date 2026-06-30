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
package org.thunderdog.challegram.ui

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.drinkless.tdlib.TdApi
import org.thunderdog.challegram.R
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.data.ContentPreview
import org.thunderdog.challegram.navigation.ViewController
import org.thunderdog.challegram.telegram.ForumTopicInfoListener
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.telegram.TdlibUi
import org.thunderdog.challegram.v.CustomRecyclerView

/**
 * Forum topic list for a single forum supergroup chat: browse, paginate, open a topic
 * (as a message thread), live-update rows, and manage topics (create/edit/close/pin/hide/delete).
 *
 * Rows are drawn by [TopicView].
 */
class TopicsController(context: Context, tdlib: Tdlib) :
  RecyclerViewController<TopicsController.Args>(context, tdlib),
  ForumTopicInfoListener {

  class Args(val chatId: Long)

  private var chatId: Long = 0

  private val topics = ArrayList<TdApi.ForumTopic>()
  private val subscribedTopicIds = HashSet<Int>()

  private var nextOffsetDate = 0
  private var nextOffsetMessageId = 0L
  private var nextOffsetForumTopicId = 0
  private var loading = false
  private var endReached = false

  private lateinit var adapter: TopicAdapter

  override fun getId (): Int = R.id.controller_topics

  override fun getName (): CharSequence = tdlib.chatTitle(chatId) ?: ""

  override fun getMenuId (): Int = R.id.menu_more

  override fun openMoreMenu () {
    val ids = ArrayList<Int>()
    val titles = ArrayList<String>()
    val icons = ArrayList<Int>()
    if (tdlib.canManageTopics(chatId)) {
      ids.add(R.id.btn_createTopic)
      titles.add(Lang.getString(R.string.NewTopic))
      icons.add(R.drawable.baseline_add_24)
    }
    ids.add(R.id.btn_viewAsMessages)
    titles.add(Lang.getString(R.string.ViewAsMessages))
    icons.add(R.drawable.baseline_forum_24)
    showOptions(null, ids.toIntArray(), titles.toTypedArray(), null, icons.toIntArray()) { _, id ->
      when (id) {
        R.id.btn_createTopic -> promptCreateTopic()
        R.id.btn_viewAsMessages -> tdlib.ui().openChat(this, chatId, TdlibUi.ChatOpenParameters().forceMessagesView())
      }
      true
    }
  }

  override fun setArguments (args: Args) {
    super.setArguments(args)
    this.chatId = args.chatId
  }

  override fun onCreateView (context: Context, recyclerView: CustomRecyclerView) {
    adapter = TopicAdapter()
    recyclerView.adapter = adapter
    recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
      override fun onScrolled (rv: RecyclerView, dx: Int, dy: Int) {
        if (dy <= 0 || loading || endReached) return
        if (findLastVisiblePosition() >= topics.size - LOAD_THRESHOLD) {
          loadTopics(false)
        }
      }
    })
    loadTopics(true)
  }

  // Data

  private fun loadTopics (reset: Boolean) {
    if (loading || (endReached && !reset)) {
      return
    }
    loading = true
    val offsetDate = if (reset) 0 else nextOffsetDate
    val offsetMessageId = if (reset) 0L else nextOffsetMessageId
    val offsetForumTopicId = if (reset) 0 else nextOffsetForumTopicId
    tdlib.getForumTopics(chatId, null, offsetDate, offsetMessageId, offsetForumTopicId, LOAD_LIMIT, { result ->
      runOnUiThreadOptional {
        loading = false
        if (result == null) return@runOnUiThreadOptional
        if (reset) {
          topics.clear()
          endReached = false
        }
        if (result.topics.isEmpty()) {
          endReached = true
        } else {
          val loaded = result.topics.asList()
          topics.addAll(loaded)
          nextOffsetDate = result.nextOffsetDate
          nextOffsetMessageId = result.nextOffsetMessageId
          nextOffsetForumTopicId = result.nextOffsetForumTopicId
          subscribe(loaded)
        }
        adapter.notifyDataSetChanged()
      }
    }, { runOnUiThreadOptional { loading = false } })
  }

  private fun subtitleOf (topic: TdApi.ForumTopic): CharSequence {
    val draft = topic.draftMessage
    if (draft != null) {
      val draftContent = draft.inputMessageText
      if (draftContent is TdApi.InputMessageText) {
        return Lang.getCharSequence(R.string.format_draft, draftContent.text.text)
      }
    }
    val message = topic.lastMessage
    return if (message != null) {
      ContentPreview.getChatListPreview(tdlib, chatId, message, true).buildText(false)
    } else {
      Lang.getString(R.string.NoMessages)
    }
  }

  private fun isMuted (topic: TdApi.ForumTopic): Boolean {
    val ns = topic.notificationSettings ?: return false
    return !ns.useDefaultMuteFor && ns.muteFor > 0
  }

  private fun indexOfTopic (forumTopicId: Int): Int {
    for (i in topics.indices) {
      if (topics[i].info.forumTopicId == forumTopicId) {
        return i
      }
    }
    return -1
  }

  // Navigation

  private fun onClickTopic (topic: TdApi.ForumTopic) {
    // Open via MessageTopicForum (not a message thread): GetMessageThread is rejected for the
    // General topic (#400 "Threads can't be used in General topic"), so all topics — General and
    // regular alike — are opened by topic id. The loader filters history with SearchChatMessages
    // over this MessageTopicForum.
    tdlib.ui().openChat(
      this, chatId,
      TdlibUi.ChatOpenParameters()
        .keepStack()
        .messageTopicId(TdApi.MessageTopicForum(topic.info.forumTopicId))
    )
  }

  // Management

  private fun showTopicOptions (topic: TdApi.ForumTopic): Boolean {
    if (!tdlib.canManageTopics(chatId)) {
      return false
    }
    val info = topic.info
    val ids = ArrayList<Int>()
    val titles = ArrayList<String>()
    val colors = ArrayList<Int>()
    val icons = ArrayList<Int>()

    ids.add(R.id.btn_editTopic)
    titles.add(Lang.getString(R.string.EditTopic))
    colors.add(ViewController.OptionColor.NORMAL)
    icons.add(R.drawable.baseline_edit_24)

    if (!info.isGeneral) {
      ids.add(R.id.btn_pinTopic)
      titles.add(Lang.getString(if (topic.isPinned) R.string.Unpin else R.string.Pin))
      colors.add(ViewController.OptionColor.NORMAL)
      icons.add(if (topic.isPinned) R.drawable.deproko_baseline_pin_undo_24 else R.drawable.deproko_baseline_pin_24)

      ids.add(R.id.btn_closeTopic)
      titles.add(Lang.getString(if (info.isClosed) R.string.ReopenTopic else R.string.CloseTopic))
      colors.add(ViewController.OptionColor.NORMAL)
      icons.add(if (info.isClosed) R.drawable.baseline_visibility_24 else R.drawable.baseline_lock_24)

      ids.add(R.id.btn_deleteTopic)
      titles.add(Lang.getString(R.string.DeleteTopic))
      colors.add(ViewController.OptionColor.RED)
      icons.add(R.drawable.baseline_delete_24)
    } else {
      ids.add(R.id.btn_hideTopic)
      titles.add(Lang.getString(if (info.isHidden) R.string.ShowTopic else R.string.HideTopic))
      colors.add(ViewController.OptionColor.NORMAL)
      icons.add(if (info.isHidden) R.drawable.baseline_visibility_24 else R.drawable.baseline_block_24)
    }

    showOptions(info.name, ids.toIntArray(), titles.toTypedArray(), colors.toIntArray(), icons.toIntArray()) { _, id ->
      when (id) {
        R.id.btn_editTopic -> promptEditTopic(topic)
        R.id.btn_pinTopic -> tdlib.toggleForumTopicIsPinned(chatId, info.forumTopicId, !topic.isPinned) { reload() }
        R.id.btn_closeTopic -> tdlib.toggleForumTopicIsClosed(chatId, info.forumTopicId, !info.isClosed) { reload() }
        R.id.btn_hideTopic -> tdlib.toggleGeneralForumTopicIsHidden(chatId, !info.isHidden) { reload() }
        R.id.btn_deleteTopic -> confirmDeleteTopic(topic)
      }
      true
    }
    return true
  }

  private fun promptCreateTopic () {
    val c = CreateTopicController(context, tdlib)
    c.setArguments(CreateTopicController.Args(chatId) { name, icon ->
      tdlib.createForumTopic(chatId, name, false, icon, { runOnUiThreadOptional { reload() } }, null)
    })
    navigateTo(c)
  }

  private fun promptEditTopic (topic: TdApi.ForumTopic) {
    openInputAlert(
      Lang.getString(R.string.EditTopic),
      Lang.getString(R.string.TopicNameHint),
      R.string.Done,
      R.string.Cancel,
      topic.info.name,
      { _, result ->
        val name = result?.trim() ?: ""
        if (name.isEmpty()) {
          return@openInputAlert false
        }
        tdlib.editForumTopic(chatId, topic.info.forumTopicId, name, false, 0L) { reload() }
        true
      },
      true
    )
  }

  private fun confirmDeleteTopic (topic: TdApi.ForumTopic) {
    showOptions(
      Lang.getString(R.string.DeleteTopicConfirm),
      intArrayOf(R.id.btn_deleteTopic, R.id.btn_cancel),
      arrayOf(Lang.getString(R.string.DeleteTopic), Lang.getString(R.string.Cancel)),
      intArrayOf(ViewController.OptionColor.RED, ViewController.OptionColor.NORMAL),
      intArrayOf(R.drawable.baseline_delete_24, R.drawable.baseline_cancel_24)
    ) { _, id ->
      if (id == R.id.btn_deleteTopic) {
        tdlib.deleteForumTopic(chatId, topic.info.forumTopicId) { reload() }
      }
      true
    }
  }

  private fun reload () {
    runOnUiThreadOptional { loadTopics(true) }
  }

  // Live updates

  private fun subscribe (newTopics: List<TdApi.ForumTopic>) {
    for (topic in newTopics) {
      val forumTopicId = topic.info.forumTopicId
      if (subscribedTopicIds.add(forumTopicId)) {
        tdlib.listeners().subscribeToForumTopicUpdates(chatId, forumTopicId.toLong(), this)
      }
    }
  }

  override fun onForumTopicInfoChanged (info: TdApi.ForumTopicInfo) {
    if (info.chatId != chatId) {
      return
    }
    runOnUiThreadOptional {
      val index = indexOfTopic(info.forumTopicId)
      if (index >= 0) {
        topics[index].info = info
        adapter.notifyItemChanged(index)
      }
    }
  }

  override fun onForumTopicUpdated (chatId: Long, messageThreadId: Long, isPinned: Boolean, lastReadInboxMessageId: Long, lastReadOutboxMessageId: Long, notificationSettings: TdApi.ChatNotificationSettings) {
    if (chatId != this.chatId) {
      return
    }
    runOnUiThreadOptional {
      val index = indexOfTopic(messageThreadId.toInt())
      if (index >= 0) {
        val topic = topics[index]
        topic.isPinned = isPinned
        topic.lastReadInboxMessageId = lastReadInboxMessageId
        topic.lastReadOutboxMessageId = lastReadOutboxMessageId
        topic.notificationSettings = notificationSettings
        adapter.notifyItemChanged(index)
      }
    }
  }

  override fun destroy () {
    super.destroy()
    for (forumTopicId in subscribedTopicIds) {
      tdlib.listeners().unsubscribeFromForumTopicUpdates(chatId, forumTopicId.toLong(), this)
    }
    subscribedTopicIds.clear()
  }

  private inner class TopicAdapter : RecyclerView.Adapter<TopicHolder>() {
    override fun onCreateViewHolder (parent: ViewGroup, viewType: Int): TopicHolder {
      val view = TopicView(parent.context, tdlib)
      addThemeInvalidateListener(view)
      val holder = TopicHolder(view)
      view.setOnClickListener {
        val position = holder.bindingAdapterPosition
        if (position != RecyclerView.NO_POSITION) {
          onClickTopic(topics[position])
        }
      }
      view.setOnLongClickListener {
        val position = holder.bindingAdapterPosition
        if (position != RecyclerView.NO_POSITION) showTopicOptions(topics[position]) else false
      }
      return holder
    }

    override fun onBindViewHolder (holder: TopicHolder, position: Int) {
      val topic = topics[position]
      (holder.itemView as TopicView).setTopic(topic, topic.info.name, subtitleOf(topic), isMuted(topic))
    }

    override fun onViewAttachedToWindow (holder: TopicHolder) {
      (holder.itemView as TopicView).attach()
    }

    override fun onViewDetachedFromWindow (holder: TopicHolder) {
      (holder.itemView as TopicView).detach()
    }

    override fun getItemCount (): Int = topics.size
  }

  private class TopicHolder(view: View) : RecyclerView.ViewHolder(view)

  companion object {
    private const val LOAD_LIMIT = 20
    private const val LOAD_THRESHOLD = 5
  }
}
