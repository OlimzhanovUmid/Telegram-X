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
 * File created on 30/06/2024
 */
package org.thunderdog.challegram.mediaview.disposable

import android.content.Context
import android.graphics.Color
import android.view.MotionEvent
import androidx.media3.exoplayer.ExoPlayer
import org.drinkless.tdlib.TdApi
import org.thunderdog.challegram.U
import org.thunderdog.challegram.component.chat.MessageView
import org.thunderdog.challegram.data.TD
import org.thunderdog.challegram.data.TGMessage
import org.thunderdog.challegram.data.TGMessageFile
import org.thunderdog.challegram.player.TGPlayerController
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.widget.FileProgressComponent
import org.thunderdog.challegram.widget.PopupLayout
import me.vkryl.android.animator.FactorAnimator
import me.vkryl.android.widget.FrameLayoutFix
import me.vkryl.core.alphaColor
import me.vkryl.core.fromTo
import tgx.td.copyOf

internal class DisposableMediaViewControllerAudio (context: Context, tdlib: Tdlib?) : DisposableMediaViewController(context, tdlib) {
  private lateinit var messageView: MessageView

  override fun onCreateContentView (context: Context): FrameLayoutFix {
    contentView = ContentView(context)

    messageView = object : MessageView(context) {
      override fun onTouchEvent (e: MotionEvent): Boolean {
        return false
      }

      override fun onAttachedToWindow () {
        super.onAttachedToWindow()
        onAttachedToRecyclerView()
      }

      override fun onDetachedFromWindow () {
        super.onDetachedFromWindow()
        onDetachedFromRecyclerView()
      }
    }
    messageView.setParentOnMeasureDisabled(true)
    messageView.setUseComplexReceiver()
    messageView.setManager(copy.manager())
    messageView.setMessage(copy)
    contentView.addView(messageView)

    return contentView
  }

  override fun onExoPlayerCreated (exoPlayer: ExoPlayer) {

  }

  private var revealStartPosition = 0f
  private var revealEndPosition = 0f

  override fun prepareShowAnimation () {
    val anchor = anchorView
    if (anchor != null) {
      anchor.getLocationOnScreen(tmpCords)
      revealStartPosition = tmpCords[1].toFloat()
    } else {
      revealStartPosition = 0f
    }

    onChangeRevealFactor(revealFactor)
    super.prepareShowAnimation()
  }

  override fun launchHideAnimation (popup: PopupLayout?, originalAnimator: FactorAnimator?): Boolean {
    revealStartPosition = revealEndPosition
    return super.launchHideAnimation(popup, originalAnimator)
  }

  override fun onChangeRevealFactor (factor: Float) {
    val viewWidth = contentView.measuredWidth
    val viewHeight = contentView.measuredHeight
    val messageHeight = messageView.measuredHeight
    if (viewWidth == 0 || viewHeight == 0 || messageHeight == 0) {
      return
    }

    revealEndPosition = (viewHeight - messageHeight) / 2f
    if (anchorView == null) {
      revealStartPosition = revealEndPosition
    }

    messageView.setAlpha(factor)
    messageView.setTranslationY(Math.round(fromTo(revealStartPosition, revealEndPosition, factor)).toFloat())

    context.setPhotoRevealFactor(factor)
    contentView.setBackgroundColor(alphaColor(factor * 0.9f, Color.BLACK))
    contentView.invalidate()
  }

  override fun onChangeVisualProgress (progress: Float) {
    fileProgressComponent?.let {
      // send fake progress event
      it.onTrackPlayProgress(tdlib!!, msgCopy.chatId, msgCopy.id, TD.getFileId(msgCopy), progress, getExoPlayerCurrentPosition(), getExoPlayerDuration(), false)
    }
    contentView.invalidate()
  }

  override fun calculateProgressTickDelay (playDuration: Long): Long {
    return U.calculateDelayForDiameter(getVideoSize().toLong(), playDuration)
  }

  override fun getId (): Int {
    return 0
  }

  override fun destroy () {
    messageView.performDestroy()
    super.destroy()
  }


  /* Content */

  inner class ContentView (context: Context) : FrameLayoutFix(context) {
    override fun onMeasure (widthMeasureSpec: Int, heightMeasureSpec: Int) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec)
      onChangeRevealFactor(revealFactor)
    }
  }

  private lateinit var copy: TGMessageFile
  protected lateinit var msgCopy: TdApi.Message
  private var fileProgressComponent: FileProgressComponent? = null

  override fun setArguments (args: Args) {
    super.setArguments(args)

    msgCopy = tgMessage.message.copyOf()!!
    msgCopy.chatId = -1  // set fake chat id and mark as listened
    if (msgCopy.content.constructor == TdApi.MessageVoiceNote.CONSTRUCTOR) {
      (msgCopy.content as TdApi.MessageVoiceNote).isListened = true
    }

    copy = TGMessage.valueOf(tgMessage.manager(), msgCopy) as TGMessageFile

    val file = copy.findFileComponent(msgCopy.id)
    fileProgressComponent = file?.fileProgress
    fileProgressComponent?.let {
      // send fake play event
      it.onTrackStateChanged(tdlib!!, msgCopy.chatId, msgCopy.id, TD.getFileId(msgCopy), TGPlayerController.STATE_PLAYING)
      tdlib!!.files().unsubscribe(TD.getFileId(msgCopy), it)
    }
  }

  companion object {
    private val tmpCords = IntArray(2)

    private fun getVideoSize (): Int {
      return Math.min(Screen.smallestSide() - Screen.dp(80f), Screen.dp(640f))
    }
  }
}
