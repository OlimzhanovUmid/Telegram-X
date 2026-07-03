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
package org.thunderdog.challegram.component.popups

import org.drinkless.tdlib.TdApi
import org.thunderdog.challegram.component.attach.MediaBottomBaseController
import org.thunderdog.challegram.component.attach.MediaLayout
import org.thunderdog.challegram.data.TGMessage
import org.thunderdog.challegram.navigation.ViewController
import org.thunderdog.challegram.tool.UI

class ModernActionedLayout (context: ViewController<*>) : MediaLayout(context) {
  private var curController: MediaBottomBaseController<*>? = null

  fun setController (controller: MediaBottomBaseController<*>) {
    controller.getValue()
    curController = controller
  }

  override fun createControllerForIndex (index: Int): MediaBottomBaseController<*>? {
    return curController
  }

  // Helpers

  private fun interface MalDataProvider <VC : MediaBottomBaseController<*>> {
    fun provide (layout: ModernActionedLayout): VC
  }

  companion object {
    @JvmStatic
    fun showGiftCode (context: ViewController<*>, code: String, giftCodeContent: TdApi.MessagePremiumGiftCode?, giftCodeInfo: TdApi.PremiumGiftCodeInfo) {
      if (context.getKeyboardState()) {
        context.hideSoftwareKeyboard()
        UI.post({ showGiftCode(context, code, giftCodeContent, giftCodeInfo) }, 100)
        return
      }
      showMal(context) { mal -> GiftCodeController(mal, code, giftCodeContent, giftCodeInfo) }
    }

    @JvmStatic
    fun showMessageSeen (context: ViewController<*>, msg: TGMessage, viewers: TdApi.MessageViewers) {
      showMal(context) { mal -> MessageSeenController(mal, msg, viewers) }
    }

    @JvmStatic
    fun showJoinRequests (context: ViewController<*>, chatId: Long, requestsInfo: TdApi.ChatJoinRequestsInfo) {
      showDeferredMal(context) { mal -> JoinRequestsController(mal, chatId, requestsInfo) }
    }

    @JvmStatic
    fun showJoinDialog (context: ViewController<*>, inviteLinkInfo: TdApi.ChatInviteLinkInfo, onJoinClicked: Runnable) {
      showMal(context) { mal -> JoinDialogController(mal, inviteLinkInfo, onJoinClicked) }
    }

    private fun <VC : MediaBottomBaseController<*>> showMal (context: ViewController<*>, provider: MalDataProvider<VC>) {
      val mal = ModernActionedLayout(context)
      mal.setController(provider.provide(mal))
      mal.initCustom()
      mal.show()
    }

    private fun <VC : MediaBottomBaseController<*>> showDeferredMal (context: ViewController<*>, provider: MalDataProvider<VC>) {
      val mal = ModernActionedLayout(context)
      val controller = provider.provide(mal)
      controller.postOnAnimationExecute(Runnable { mal.show() })
      mal.setController(controller)
      mal.initCustom()
    }
  }
}
