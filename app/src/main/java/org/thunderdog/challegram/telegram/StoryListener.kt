package org.thunderdog.challegram.telegram

import org.drinkless.tdlib.TdApi

interface StoryListener {
  fun onStoryUpdated (story: TdApi.Story)
  fun onStoryDeleted (storySenderChatId: Long, storyId: Int)
  fun onStorySendSucceeded (story: TdApi.Story, oldStoryId: Int) {}
  fun onStorySendFailed (story: TdApi.Story, error: TdApi.Error, errorType: TdApi.CanPostStoryResult?) {}
  fun onStoryStealthModeUpdated (activeUntilDate: Int, cooldownUntilDate: Int) {}
}
