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
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import org.thunderdog.challegram.Log
import org.thunderdog.challegram.R
import org.thunderdog.challegram.U
import org.thunderdog.challegram.data.TD
import org.thunderdog.challegram.data.TGMessage
import org.thunderdog.challegram.navigation.TooltipOverlayView
import org.thunderdog.challegram.navigation.ViewController
import org.thunderdog.challegram.player.TGPlayerController
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.telegram.TdlibManager
import org.thunderdog.challegram.tool.UI
import org.thunderdog.challegram.widget.PopupLayout
import java.io.File
import java.io.FileNotFoundException
import java.io.RandomAccessFile
import me.vkryl.android.DECELERATE_INTERPOLATOR
import me.vkryl.android.animator.FactorAnimator
import me.vkryl.android.widget.FrameLayoutFix
import me.vkryl.core.clamp
import tgx.td.isVideoNote
import tgx.td.isVoiceNote

abstract class DisposableMediaViewController(context: Context, tdlib: Tdlib?) :
  ViewController<DisposableMediaViewController.Args>(context, tdlib),
  PopupLayout.AnimatedPopupProvider, FactorAnimator.Target, Player.Listener, PopupLayout.TouchSectionProvider {

  private lateinit var popupView: PopupLayout
  private var exoPlayer: ExoPlayer? = null
  protected lateinit var contentView: FrameLayoutFix

  final override fun onCreateView(context: Context): View? {
    popupView = PopupLayout(context)
    popupView.setOverlayStatusBar(true)
    popupView.setTouchProvider(this)
    popupView.setNeedRootInsets()
    popupView.init(true)
    popupView.setIgnoreAllInsets(true)
    popupView.setBoundController(this)
    popupView.setDisableCancelOnTouchDown(true)

    contentView = onCreateContentView(context)

    val exoPlayer = U.newExoPlayer(context, true)
    this.exoPlayer = exoPlayer
    TdlibManager.instance().player().proximityManager().modifyExoPlayer(exoPlayer, C.AUDIO_CONTENT_TYPE_MOVIE)
    exoPlayer.addListener(this)
    exoPlayer.setVolume(1f)
    exoPlayer.setPlayWhenReady(false)
    exoPlayer.setMediaSource(U.newMediaSource(contentFile))
    onExoPlayerCreated(exoPlayer)
    exoPlayer.prepare()

    return contentView
  }

  override fun shouldTouchOutside(x: Float, y: Float): Boolean {
    return false
  }

  override fun onPlaybackStateChanged(playbackState: Int) {
    if (playbackState == Player.STATE_ENDED) {
      hide()
    }
  }

  protected abstract fun onCreateContentView(context: Context): FrameLayoutFix

  protected abstract fun onExoPlayerCreated(exoPlayer: ExoPlayer)

  fun open() {
    getValue()
    popupView.showAnimatedPopupView(contentView, this)
  }

  fun hide() {
    popupView.hideWindow(true)
  }

  fun getExoPlayerDuration(): Long {
    return exoPlayer?.duration ?: C.TIME_UNSET
  }

  fun getExoPlayerCurrentPosition(): Long {
    return exoPlayer?.currentPosition ?: C.TIME_UNSET
  }

  override fun prepareShowAnimation() {
    revealAnimator = FactorAnimator(ANIMATOR_REVEAL, this, DECELERATE_INTERPOLATOR, REVEAL_ANIMATION_DURATION)
  }

  override fun launchShowAnimation(popup: PopupLayout?) {
    revealAnimator.animateTo(1f)
  }

  override fun launchHideAnimation(popup: PopupLayout?, originalAnimator: FactorAnimator?): Boolean {
    revealAnimator.animateTo(0f)
    return true
  }

  private lateinit var revealAnimator: FactorAnimator

  protected var revealFactor: Float = 0f
    private set(value) {
      if (field != value) {
        field = value
        onChangeRevealFactor(value)
      }
    }

  protected abstract fun onChangeRevealFactor(factor: Float)

  override fun onFactorChanged(id: Int, factor: Float, fraction: Float, callee: FactorAnimator?) {
    if (id == ANIMATOR_REVEAL) {
      revealFactor = factor
    }
  }

  override fun onFactorChangeFinished(id: Int, finalFactor: Float, callee: FactorAnimator?) {
    if (id == ANIMATOR_REVEAL) {
      if (finalFactor == 0f) {
        popupView.onCustomHideAnimationComplete()
      }
      if (finalFactor == 1f) {
        tgMessage.readContent()
        popupView.onCustomShowComplete()
        exoPlayer!!.setPlayWhenReady(true)
        checkProgressTimer(true)
      }
    }
  }


  /* Args */

  protected lateinit var tgMessage: TGMessage
  @JvmField
  protected var anchorView: View? = null
  private var contentFile: RandomAccessFile? = null

  class Args(view: View?, message: TGMessage, file: RandomAccessFile) {
    val message: TGMessage = message
    val anchorView: View? = view
    val contentFile: RandomAccessFile = file
  }

  override fun setArguments(args: Args) {
    super.setArguments(args)
    this.tgMessage = args.message
    this.anchorView = args.anchorView
    this.contentFile = args.contentFile
  }

  override fun destroy() {
    super.destroy()

    checkProgressTimer(false)

    exoPlayer!!.release()
    exoPlayer = null

    U.closeFile(contentFile)
  }


  /* Visual Progress */

  private val handler = VideoHandler(this)

  private class VideoHandler(private val controller: DisposableMediaViewController) : Handler(Looper.getMainLooper()) {
    override fun handleMessage(msg: Message) {
      controller.onProgressTick()
    }
  }

  private var progressTimerStarted = false

  private fun checkProgressTimer(isPlayed: Boolean) {
    if (this.progressTimerStarted != isPlayed) {
      this.progressTimerStarted = isPlayed
      Log.i(Log.TAG_VIDEO, "progressTimerStarted -> %b", isPlayed)
      handler.removeMessages(ACTION_PROGRESS_TICK)
      onProgressTick()
    }
  }

  private var progress = 0f
  private var playPosition: Long = -1
  private var playDuration: Long = -1

  private fun setPlayProgress(progress: Float, playPosition: Long, playDuration: Long) {
    if (this.progress != progress || this.playPosition != playPosition || this.playDuration != playDuration) {
      // boolean reset = this.remainingSeconds != remainingSeconds || this.totalSeconds != totalSeconds;

      this.progress = progress
      this.playDuration = playDuration
      if (this.playPosition != playPosition) {
        this.playPosition = playPosition
      }

      visualProgress = clamp(progress)
    }
  }

  var visualProgress: Float = 0f
    private set(value) {
      if (field != value) {
        field = value
        onChangeVisualProgress(value)
      }
    }

  private fun onProgressTick() {
    val duration = getExoPlayerDuration()
    val position = getExoPlayerCurrentPosition()
    if (duration != C.TIME_UNSET && position != C.TIME_UNSET) {
      val progress = if (duration != 0L) clamp(position.toFloat() / duration.toFloat()) else 0f
      setPlayProgress(progress, position, duration)
    }

    if (progressTimerStarted) {
      val delay = calculateProgressTickDelay(playDuration)
      handler.sendMessageDelayed(Message.obtain(handler, ACTION_PROGRESS_TICK), delay)
    }
  }

  protected abstract fun onChangeVisualProgress(progress: Float)

  protected abstract fun calculateProgressTickDelay(playDuration: Long): Long

  companion object {
    private const val REVEAL_ANIMATION_DURATION: Long = 280
    private const val ANIMATOR_REVEAL = -1

    private const val ACTION_PROGRESS_TICK = 1

    @JvmStatic
    fun openMediaOrShowTooltip(view: View, message: TGMessage, locationProvider: TooltipOverlayView.LocationProvider): Boolean {
      val context = message.context()
      val tdlib = message.tdlib()
      val msg = message.message
      val file = TD.getFile(msg)

      if (!TD.isFileLoaded(file)) {
        UI.hapticVibrate(view, true)
        context.tooltipManager().builder(view).locate(locationProvider).show(tdlib, R.string.MediaOnceWaitFilуDownload).hideDelayed()
        return false
      }

      if (U.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) {
        U.adjustStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI)
        UI.hapticVibrate(view, true)
        context.tooltipManager().builder(view).locate(locationProvider).show(tdlib, R.string.MediaOnceTurnOnSound).hideDelayed()
        return false
      }

      var player: DisposableMediaViewController? = null
      if (msg.content.isVideoNote()) {
        player = DisposableMediaViewControllerVideo(context, tdlib)
      } else if (msg.content.isVoiceNote()) {
        player = DisposableMediaViewControllerAudio(context, tdlib)
      }

      if (player != null) {
        val randomAccessFile: RandomAccessFile
        try {
          randomAccessFile = RandomAccessFile(File(file!!.local.path), "r")
        } catch (e: FileNotFoundException) {
          UI.hapticVibrate(view, true)
          Log.e(e)
          return false
        }

        TdlibManager.instance().player().pauseWithReason(TGPlayerController.PAUSE_REASON_OPEN_ONCE_MEDIA)

        player.setArguments(Args(view, message, randomAccessFile))
        player.open()
        return true
      }

      return false
    }
  }
}
