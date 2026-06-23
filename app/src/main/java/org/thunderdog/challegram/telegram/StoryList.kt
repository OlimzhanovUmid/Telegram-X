package org.thunderdog.challegram.telegram

import androidx.annotation.NonNull
import org.drinkless.tdlib.TdApi
import me.vkryl.core.lambda.RunnableBool

class StoryList (
  tdlib: Tdlib,
  private val list: TdApi.StoryList
) : SortedList<TdApi.ChatActiveStories>(tdlib) {
  override fun compare (a: TdApi.ChatActiveStories, b: TdApi.ChatActiveStories): Int {
    return tdlib.storiesComparator().compare(a, b)
  }

  override fun loadMoreItems (desiredCount: Int, @NonNull after: RunnableBool) {
    tdlib.client().send(TdApi.LoadActiveStories(list)) { result ->
      val endReached = result.constructor == TdApi.Error.CONSTRUCTOR
      after.runWithBool(endReached)
    }
  }

  override fun approximateTotalItemCount (): Int {
    return tdlib.getStoryListChatCount(list)
  }
}
