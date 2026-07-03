package org.thunderdog.challegram.ui

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import org.thunderdog.challegram.R
import org.thunderdog.challegram.component.user.UserView
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.data.TD
import org.thunderdog.challegram.data.TGMessage
import org.thunderdog.challegram.data.TGUser
import org.thunderdog.challegram.support.ViewSupport
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.telegram.TdlibUi
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.util.ReactionModifier
import org.thunderdog.challegram.util.StringList
import org.thunderdog.challegram.v.CustomRecyclerView
import org.thunderdog.challegram.widget.ListInfoView
import org.thunderdog.challegram.widget.PopupLayout
import tgx.td.equalsTo

class MessageOptionsReactedController (context: Context, tdlib: Tdlib?, private val popupLayout: PopupLayout, private val message: TGMessage, private val reactionType: TdApi.ReactionType?) : BottomSheetViewController.BottomSheetBaseRecyclerViewController<Void?>(context, tdlib), View.OnClickListener {
  private lateinit var adapter: SettingsAdapter
  private var offset: String = ""

  private var canLoadMore = true
  private var isLoadingMore = false
  private var totalCount = 0

  override fun supportsBottomInset (): Boolean {
    return true
  }

  override fun onCreateView (context: Context, recyclerView: CustomRecyclerView) {
    adapter = object : SettingsAdapter(this) {
      override fun setUser (item: ListItem, position: Int, userView: UserView, isUpdate: Boolean) {
        val senderId = item.getData() as TdApi.MessageSender
        val user: TGUser = if (senderId.getConstructor() == TdApi.MessageSenderUser.CONSTRUCTOR) {
          TGUser(tdlib!!, tdlib!!.cache().userStrict((senderId as TdApi.MessageSenderUser).userId))
        } else {
          TGUser(tdlib!!, tdlib!!.chatStrict((senderId as TdApi.MessageSenderChat).chatId))
        }
        user.setActionDateStatus(item.getIntValue(), R.string.reacted)
        userView.setUser(user)
        val sliderValues = item.getSliderValues()
        if (sliderValues != null && sliderValues.isNotEmpty() && reactionType == null) {
          userView.setDrawModifier(ReactionModifier(tdlib!!, sliderValues).setMode(ReactionModifier.MODE_INLINE).setOffset(8).requestFiles(userView.getComplexReceiver()))
        } else {
          userView.setDrawModifier(null)
        }
      }

      override fun setInfo (item: ListItem, position: Int, infoView: ListInfoView) {
        infoView.showInfo(Lang.pluralBold(R.string.xReacted, totalCount.toLong()))
      }
    }
    recyclerView.setAdapter(adapter)
    recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
      override fun onScrolled (recyclerView: RecyclerView, dx: Int, dy: Int) {
        if (canLoadMore && !isLoadingMore /*&& senders != null && !senders.isEmpty() && loadOffset != 0*/) {
          val lastVisiblePosition = (recyclerView.getLayoutManager() as LinearLayoutManager).findLastVisibleItemPosition()
          if (lastVisiblePosition + 10 >= adapter.getItemCount()) {
            loadMore()
          }
        }
      }
    })
    ViewSupport.setThemedBackground(recyclerView, ColorId.background)
    addThemeInvalidateListener(recyclerView)
    loadMore()
  }

  private fun loadMore () {
    if (isLoadingMore || !canLoadMore) {
      return
    }
    isLoadingMore = true
    tdlib!!.client().send(TdApi.GetMessageAddedReactions(message.chatId, message.smallestId, reactionType, offset, 50), Client.ResultHandler { obj: TdApi.Object? ->
      if (obj!!.getConstructor() != TdApi.AddedReactions.CONSTRUCTOR) return@ResultHandler
      runOnUiThreadOptional(Runnable {
        val reactions = obj as TdApi.AddedReactions
        offset = reactions.nextOffset
        isLoadingMore = false
        canLoadMore = offset.isNotEmpty()
        totalCount = reactions.totalCount
        processNewAddedReactions(reactions)
      })
    })
  }

  private fun processNewAddedReactions (addedReactions: TdApi.AddedReactions) {
    val reactions = addedReactions.reactions
    val items = adapter.getItems()
    val itemsCount = items.size

    val reactionKeys = StringList(3)
    var lastReaction: TdApi.AddedReaction? = null
    for (reaction in reactions) {
      if (lastReaction != null && !reaction.senderId.equalsTo(lastReaction.senderId)) {
        if (!items.isEmpty()) {
          items.add(ListItem(ListItem.TYPE_SEPARATOR))
        }
        val item = ListItem(ListItem.TYPE_USER_SMALL, R.id.sender)
          .setData(lastReaction.senderId)
          .setIntValue(lastReaction.date)
          .setSliderInfo(reactionKeys.get(), 0)
        items.add(item)
        reactionKeys.clear()
      }
      lastReaction = reaction
      reactionKeys.append(TD.makeReactionKey(reaction.type))
    }
    if (lastReaction != null) {
      if (!items.isEmpty()) {
        items.add(ListItem(ListItem.TYPE_SEPARATOR))
      }
      val item = ListItem(ListItem.TYPE_USER_SMALL, R.id.sender)
        .setData(lastReaction.senderId)
        .setIntValue(lastReaction.date)
        .setSliderInfo(reactionKeys.get(), 0)
      items.add(item)
    }

    if (addedReactions.nextOffset.isEmpty()) {
      items.add(ListItem(ListItem.TYPE_SHADOW_BOTTOM))
      items.add(ListItem(ListItem.TYPE_LIST_INFO_VIEW))
    }

    if (itemsCount > 0) {
      adapter.notifyItemRangeChanged(itemsCount - 1, items.size - itemsCount + 1)
    } else {
      adapter.notifyItemRangeChanged(itemsCount, items.size - itemsCount)
    }
  }

  override fun onClick (v: View) {
    if (v.getId() == R.id.sender) {
      popupLayout.hideWindow(true)
      val item = v.getTag() as ListItem
      val senderId = item.getData() as TdApi.MessageSender
      tdlib!!.ui().openSenderProfile(this, senderId, TdlibUi.UrlOpenParameters().tooltip(context().tooltipManager().builder(v)))
    }
  }

  override fun getId (): Int {
    return R.id.controller_messageOptionsReacted
  }

  override fun getItemsHeight (parent: RecyclerView?): Int {
    if (adapter.getItems().size == 0) {
      return 0
    }
    return adapter.measureHeight(-1)
  }
}
