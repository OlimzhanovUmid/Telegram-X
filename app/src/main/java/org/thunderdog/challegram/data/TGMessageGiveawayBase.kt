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
 * File created on 04/01/2024
 */
package org.thunderdog.challegram.data

import android.graphics.Canvas
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.text.SpannableStringBuilder
import android.view.MotionEvent
import android.view.View
import androidx.annotation.CallSuper
import androidx.annotation.DrawableRes
import org.drinkless.tdlib.TdApi
import org.thunderdog.challegram.R
import org.thunderdog.challegram.component.chat.MessageView
import org.thunderdog.challegram.component.chat.MessagesManager
import org.thunderdog.challegram.component.user.BubbleWrapView2
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.loader.ComplexReceiver
import org.thunderdog.challegram.navigation.ViewController
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.tool.Drawables
import org.thunderdog.challegram.tool.Fonts
import org.thunderdog.challegram.tool.getPorterDuffPaint
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.tool.UI
import org.thunderdog.challegram.tool.Views
import org.thunderdog.challegram.unsorted.Settings
import org.thunderdog.challegram.util.GiftParticlesDrawable
import org.thunderdog.challegram.util.text.Text
import org.thunderdog.challegram.util.text.TextColorSet
import org.thunderdog.challegram.util.text.TextStyleProvider
import org.thunderdog.challegram.util.text.TextWrapper
import java.util.concurrent.TimeUnit
import me.vkryl.android.util.ViewProvider
import me.vkryl.core.isEmpty
import me.vkryl.core.lambda.RunnableData
import tgx.td.assertGiveawayInfo_cee9b325
import tgx.td.assertGiveawayParticipantStatus_18551e87
import tgx.td.assertGiveawayPrize_28ff2954
import tgx.td.unsupported

abstract class TGMessageGiveawayBase protected constructor(manager: MessagesManager, msg: TdApi.Message) :
  TGMessage(manager, msg), TGInlineKeyboard.ClickListener, GiftParticlesDrawable.ParticleValidator {

  private var particlesDrawable: GiftParticlesDrawable? = null
  private var contentHeightValue = 0

  @JvmField
  protected var content: Content? = null

  @JvmField
  protected var rippleButton: TGInlineKeyboard? = null
  private var rippleButtonY = 0
  private var useFullWidthParticles = false

  final override fun buildContent(maxWidth: Int) {
    useFullWidthParticles = !useBubbles() && !useForward()

    if (rippleButton == null) {
      rippleButton = TGInlineKeyboard(this, false)
      rippleButton!!.setViewProvider(currentViews)
    }
    onBuildButton(maxWidth)

    contentHeightValue = onBuildContent(maxWidth)

    contentHeightValue += Screen.dp(BLOCK_MARGIN.toFloat())

    rippleButtonY = contentHeightValue

    contentHeightValue += TGInlineKeyboard.getButtonHeight()
    contentHeightValue += Screen.dp(BLOCK_MARGIN.toFloat()) / 2

    if (particlesDrawable == null) {
      particlesDrawable = GiftParticlesDrawable(this)
    }
    particlesDrawable!!.setBounds(0, 0, if (useFullWidthParticles) width else maxWidth, contentHeightValue)

    invalidateGiveawayReceiver()
  }

  override fun isValidPosition(x: Float, y: Float): Boolean {
    return y < content!!.getHeight() && content!!.isValidPosition(x - (if (useFullWidthParticles) contentX else 0) - Screen.dp(CONTENT_PADDING_DP.toFloat()), y)
  }

  protected open fun onBuildButton(maxWidth: Int) {
    rippleButton!!.setCustom(0, getButtonText(), maxWidth, false, this)
  }

  protected abstract fun onBuildContent(maxWidth: Int): Int

  protected open fun getButtonText(): String? {
    return null
  }

  override fun drawContent(view: MessageView?, c: Canvas, startX: Int, startY: Int, maxWidth: Int) {
    if (particlesDrawable != null) {
      val saveCount = Views.save(c)
      c.translate((if (useFullWidthParticles) 0 else startX).toFloat(), startY.toFloat())
      particlesDrawable!!.draw(c)
      Views.restore(c, saveCount)
    }

    content!!.draw(c, view, startX + Screen.dp(CONTENT_PADDING_DP.toFloat()), startY)

    if (rippleButton != null) {
      rippleButton!!.draw(view, c, startX, startY + rippleButtonY)
    }
  }

  override val contentHeight: Int
    get() = contentHeightValue

  override fun performLongPress(view: View?, x: Float, y: Float): Boolean {
    val res = super.performLongPress(view, x, y)

    val a = rippleButton?.performLongPress(view) == true
    val b = content?.performLongPress(view, x, y) == true

    return a || b || res
  }

  override fun onTouchEvent(view: MessageView, e: MotionEvent): Boolean {
    if (rippleButton?.onTouchEvent(view, e) == true) {
      return true
    }
    if (content?.onTouchEvent(view, e) == true) {
      return true
    }
    return super.onTouchEvent(view, e)
  }

  override fun requestGiveawayAvatars(complexReceiver: ComplexReceiver?, isUpdate: Boolean) {
    content?.requestFiles(complexReceiver)
  }


  /* * */

  class Content(private val maxWidth: Int) : GiftParticlesDrawable.ParticleValidator {
    private val parts = ArrayList<ContentPart>()
    private var y = 0

    fun add(text: CharSequence, colorSet: TextColorSet, viewProvider: ViewProvider?) {
      add(ContentText(genTextWrapper(text, colorSet, viewProvider)))
    }

    fun add(textWrapper: TextWrapper) {
      add(ContentText(textWrapper))
    }

    fun add(p: ContentPart) {
      parts.add(p)
      p.y = y
      p.build(maxWidth)
      y += p.getHeight()
    }

    fun padding(padding: Int) {
      y += padding
    }

    fun getHeight(): Int {
      return y
    }

    fun draw(c: Canvas, v: MessageView?, x: Int, y: Int) {
      for (p in parts) {
        p.draw(c, v, x, p.y + y)
      }
    }

    fun requestFiles(complexReceiver: ComplexReceiver?) {
      for (p in parts) {
        p.requestFiles(complexReceiver)
      }
    }

    fun onTouchEvent(view: View, e: MotionEvent): Boolean {
      for (p in parts) {
        if (p.onTouchEvent(view, e)) {
          return true
        }
      }
      return false
    }

    fun performLongPress(view: View?, x: Float, y: Float): Boolean {
      for (p in parts) {
        if (p.performLongPress(view, x, y)) {
          return true
        }
      }
      return false
    }

    private val tmpRectF = RectF()

    override fun isValidPosition(x: Float, y: Float): Boolean {
      for (p in parts) {
        val w = p.getWidth()
        val h = p.getHeight()

        tmpRectF.set((maxWidth - w) / 2f, p.y.toFloat(), (maxWidth + w) / 2f, (p.y + h).toFloat())
        tmpRectF.inset((-Screen.dp(10f)).toFloat(), (-Screen.dp(10f)).toFloat())

        if (tmpRectF.contains(x, y)) {
          return false
        }
      }

      return true
    }
  }

  abstract class ContentPart {
    var y = 0

    abstract fun build(width: Int)

    abstract fun getHeight(): Int

    abstract fun getWidth(): Int

    abstract fun draw(c: Canvas, v: MessageView?, x: Int, y: Int)

    open fun requestFiles(r: ComplexReceiver?) {
    }

    open fun performLongPress(view: View?, x: Float, y: Float): Boolean {
      return false
    }

    open fun onTouchEvent(view: View, e: MotionEvent): Boolean {
      return false
    }
  }

  class ContentText : ContentPart {
    private val textWrapper: TextWrapper?
    private val text: Text?
    private var maxWidth = 0

    constructor(wrapper: TextWrapper) : super() {
      this.textWrapper = wrapper
      this.text = null
    }

    constructor(text: Text) : super() {
      this.textWrapper = null
      this.text = text
    }

    override fun build(width: Int) {
      this.maxWidth = width
      textWrapper?.prepare(width)
    }

    override fun getWidth(): Int {
      return textWrapper?.getWidth() ?: (text?.getWidth() ?: 0)
    }

    override fun getHeight(): Int {
      return textWrapper?.getHeight() ?: (text?.getHeight() ?: 0)
    }

    override fun draw(c: Canvas, v: MessageView?, x: Int, y: Int) {
      if (textWrapper != null) {
        textWrapper.draw(c, x, y)
      } else if (text != null) {
        text.draw(c, x, x + maxWidth, 0, y, null)
      }
    }

    override fun onTouchEvent(view: View, e: MotionEvent): Boolean {
      return if (text != null) {
        text.onTouchEvent(view, e)
      } else if (textWrapper != null) {
        textWrapper.onTouchEvent(view, e)
      } else {
        false
      }
    }

    override fun performLongPress(view: View?, x: Float, y: Float): Boolean {
      return text != null && text.performLongPress(view) || textWrapper != null && textWrapper.performLongPress(view)
    }
  }

  class ContentBubbles(msg: TGMessage, private val maxTextWidth: Int) : ContentPart() {
    private val layout: BubbleWrapView2 = BubbleWrapView2(msg.tdlib, msg.currentViews)
    private val tdlib: Tdlib = msg.tdlib

    fun addChatId(chatId: Long): ContentBubbles {
      layout.addBubble(tdlib.sender(chatId), maxTextWidth)
      return this
    }

    fun addChatIds(chatIds: LongArray): ContentBubbles {
      for (chatId in chatIds) {
        addChatId(chatId)
      }
      return this
    }

    fun setOnClickListener(onClickListener: RunnableData<TdApi.MessageSender>): ContentBubbles {
      layout.setOnClickListener(onClickListener)
      return this
    }

    override fun build(width: Int) {
      layout.buildLayout(width)
    }

    override fun getWidth(): Int {
      return maxTextWidth
    }

    override fun getHeight(): Int {
      return layout.getCurrentHeight()
    }

    override fun draw(c: Canvas, v: MessageView?, x: Int, y: Int) {
      layout.draw(c, v!!.getGiveawayAvatarsReceiver(), x, y)
    }

    override fun requestFiles(r: ComplexReceiver?) {
      layout.requestFiles(r)
    }

    override fun onTouchEvent(view: View, e: MotionEvent): Boolean {
      return layout.onTouchEvent(view, e)
    }

    override fun performLongPress(view: View?, x: Float, y: Float): Boolean {
      return layout.performLongPress(view)
    }
  }

  class ContentDrawable(@DrawableRes drawableRes: Int) : ContentPart() {
    private val drawable: Drawable = Drawables.get(drawableRes)
    private var width = 0

    override fun build(width: Int) {
      this.width = width
    }

    override fun getWidth(): Int {
      return drawable.getMinimumWidth()
    }

    override fun getHeight(): Int {
      return drawable.getMinimumHeight()
    }

    override fun draw(c: Canvas, v: MessageView?, x: Int, y: Int) {
      Drawables.draw(c, drawable, x + (width - drawable.getMinimumWidth()) / 2f, y.toFloat(), getPorterDuffPaint(ColorId.icon))
    }
  }


  /* * */

  @JvmField
  protected var giveawayInfo: TdApi.GiveawayInfo? = null
  @JvmField
  protected var giveawayInfoLoaded = false

  protected fun loadGiveawayInfo() {
    if (!giveawayInfoLoaded) {
      tdlib.send(TdApi.GetGiveawayInfo(msg!!.chatId, msg!!.id)) { a, b -> UI.post { onGiveawayInfoLoaded(a, b) } }
      rippleButton!!.firstButton().showProgressDelayed()
      giveawayInfoLoaded = true
    }
  }

  @CallSuper
  protected open fun onGiveawayInfoLoaded(result: TdApi.GiveawayInfo?, error: TdApi.Error?) {
    this.giveawayInfo = result
    rippleButton!!.firstButton().hideProgress()
    giveawayInfoLoaded = false
  }

  protected fun showGiveawayInfoPopup(winnerCount: Int, prize: TdApi.GiveawayPrize, boostedChatId: Long, additionalChatsCount: Int, additionalChatIds: LongArray?, winnersSelectionDate: Int, prizeDescription: String?) {
    val giveawayInfo = this.giveawayInfo ?: return

    val b = ViewController.Options.Builder()

    var channelsSb = StringBuilder()
    channelsSb.append(tdlib.chatTitle(boostedChatId))
    if (additionalChatIds != null) {
      for (chatId in additionalChatIds) {
        channelsSb.append(Lang.getConcatSeparator())
        channelsSb.append(tdlib.chatTitle(chatId))
      }
    } else if (additionalChatsCount > 0) {
      channelsSb = StringBuilder()
      channelsSb.append(Lang.plural(R.string.GiveawaySponsors, additionalChatsCount.toLong(), tdlib.chatTitle(boostedChatId)))
    }
    val sponsors = channelsSb.toString()

    when (giveawayInfo.getConstructor()) {
      TdApi.GiveawayInfoCompleted.CONSTRUCTOR -> {
        val infoCompleted = giveawayInfo as TdApi.GiveawayInfoCompleted
        val hasGiftCode = !isEmpty(infoCompleted.giftCode)
        val wasRefunded = infoCompleted.wasRefunded

        b.title(Lang.getString(R.string.GiveawayEnded))

        val infoSsb = SpannableStringBuilder()
        when (prize.getConstructor()) {
          TdApi.GiveawayPrizePremium.CONSTRUCTOR -> {
            val premium = prize as TdApi.GiveawayPrizePremium
            infoSsb.append(Lang.pluralBold(R.string.GiveawayInfoEndedPart1, infoCompleted.winnerCount.toLong(), tdlib.chatTitle(boostedChatId), premium.monthCount))
          }
          TdApi.GiveawayPrizeStars.CONSTRUCTOR -> {
            val stars = prize as TdApi.GiveawayPrizeStars
            infoSsb.append(Lang.pluralBold(R.string.GiveawayInfoEndedPart1Stars, stars.starCount, tdlib.chatTitle(boostedChatId)))
          }
          else -> {
            assertGiveawayInfo_cee9b325()
            throw unsupported(prize)
          }
        }
        if (!isEmpty(prizeDescription)) {
          infoSsb.append("\n\n")
          infoSsb.append(Lang.pluralBold(R.string.GiveawayInfoPartExtended, infoCompleted.winnerCount.toLong(), tdlib.chatTitle(boostedChatId), prizeDescription))
        }
        infoSsb.append("\n\n")
        infoSsb.append(Lang.pluralBold(R.string.GiveawayInfoEndedPart2, infoCompleted.winnerCount.toLong(), getDateTime(infoCompleted.actualWinnersSelectionDate), sponsors))
        infoSsb.append(" ")
        infoSsb.append(Lang.pluralBold(R.string.GiveawayInfoEndedPart3, infoCompleted.activationCount.toLong()))

        if (hasGiftCode) {
          b.item(ViewController.OptionItem(R.id.btn_openLink, Lang.getString(R.string.GiveawayViewMyPrize), ViewController.OptionColor.BLUE, R.drawable.baseline_gift_outline_24))
        }

        if (wasRefunded) {
          b.subtitle(ViewController.OptionItem(0, Lang.getString(R.string.GiveawayEndedRefunded), ViewController.OptionColor.RED, R.drawable.baseline_info_24))
        } else if (hasGiftCode) {
          b.subtitle(ViewController.OptionItem(0, Lang.getString(R.string.GiveawayEndedYouWon), ViewController.OptionColor.GREEN, R.drawable.baseline_party_popper_24))
        } else {
          b.subtitle(ViewController.OptionItem(0, Lang.getString(R.string.GiveawayEndedYouLose), ViewController.OptionColor.BLUE, R.drawable.baseline_info_24))
        }

        b.info(infoSsb)
      }
      TdApi.GiveawayInfoOngoing.CONSTRUCTOR -> {
        val infoOngoing = giveawayInfo as TdApi.GiveawayInfoOngoing
        val status = infoOngoing.status
        b.title(Lang.getString(R.string.GiveawayOngoing))

        val infoSsb = SpannableStringBuilder()
        when (prize.getConstructor()) {
          TdApi.GiveawayPrizePremium.CONSTRUCTOR -> {
            val premium = prize as TdApi.GiveawayPrizePremium
            infoSsb.append(Lang.pluralBold(R.string.GiveawayInfoOngoingPart1, winnerCount.toLong(), tdlib.chatTitle(boostedChatId), premium.monthCount))
          }
          TdApi.GiveawayPrizeStars.CONSTRUCTOR -> {
            val stars = prize as TdApi.GiveawayPrizeStars
            infoSsb.append(Lang.pluralBold(R.string.GiveawayInfoOngoingPart1Stars, stars.starCount, tdlib.chatTitle(boostedChatId)))
          }
          else -> {
            assertGiveawayPrize_28ff2954()
            throw unsupported(prize)
          }
        }
        if (!isEmpty(prizeDescription)) {
          infoSsb.append("\n\n")
          infoSsb.append(Lang.pluralBold(R.string.GiveawayInfoPartExtended, winnerCount.toLong(), tdlib.chatTitle(boostedChatId), prizeDescription))
        }
        infoSsb.append("\n\n")
        infoSsb.append(Lang.pluralBold(R.string.GiveawayInfoOngoingPart2, winnerCount.toLong(), getDateTime(winnersSelectionDate), sponsors))

        when (status.getConstructor()) {
          TdApi.GiveawayParticipantStatusParticipating.CONSTRUCTOR -> {
            b.subtitle(ViewController.OptionItem(0, Lang.getString(R.string.GiveawayOngoingYouParticipating), ViewController.OptionColor.GREEN, R.drawable.baseline_info_24))
          }
          TdApi.GiveawayParticipantStatusEligible.CONSTRUCTOR -> {
            b.subtitle(ViewController.OptionItem(0, Lang.getString(R.string.GiveawayOngoingYouEligible), ViewController.OptionColor.BLUE, R.drawable.baseline_info_24))
            infoSsb.append("\n\n")
            infoSsb.append(Lang.getStringBold(R.string.GiveawayInfoOngoingPartEligible, sponsors, getDateTime(winnersSelectionDate)))
          }
          TdApi.GiveawayParticipantStatusAdministrator.CONSTRUCTOR -> {
            val s = status as TdApi.GiveawayParticipantStatusAdministrator
            infoSsb.append("\n\n")
            infoSsb.append(Lang.getStringBold(R.string.GiveawayInfoOngoingPartRestrictedAdmin, tdlib.chatTitle(s.chatId)))
            b.subtitle(ViewController.OptionItem(0, Lang.getString(R.string.GiveawayOngoingYouRestricted), ViewController.OptionColor.RED, R.drawable.baseline_info_24))
          }
          TdApi.GiveawayParticipantStatusAlreadyWasMember.CONSTRUCTOR -> {
            infoSsb.append("\n\n")
            infoSsb.append(Lang.getStringBold(R.string.GiveawayInfoOngoingPartRestrictedMember, tdlib.chatTitle(boostedChatId)))
            b.subtitle(ViewController.OptionItem(0, Lang.getString(R.string.GiveawayOngoingYouRestricted), ViewController.OptionColor.RED, R.drawable.baseline_info_24))
          }
          TdApi.GiveawayParticipantStatusDisallowedCountry.CONSTRUCTOR -> {
            val s = status as TdApi.GiveawayParticipantStatusDisallowedCountry
            infoSsb.append("\n\n")
            infoSsb.append(Lang.getStringBold(R.string.GiveawayInfoOngoingPartRestrictedCountry, s.userCountryCode))
            b.subtitle(ViewController.OptionItem(0, Lang.getString(R.string.GiveawayOngoingYouRestricted), ViewController.OptionColor.RED, R.drawable.baseline_info_24))
          }
          else -> {
            assertGiveawayParticipantStatus_18551e87()
            throw unsupported(status)
          }
        }

        b.info(infoSsb)
      }
      else -> {
        assertGiveawayInfo_cee9b325()
        throw unsupported(giveawayInfo)
      }
    }

    b.item(ViewController.OptionItem(R.id.btn_cancel, Lang.getString(R.string.GiveawayInfoClose), ViewController.OptionColor.NORMAL, R.drawable.baseline_cancel_24))
    controller()!!.showOptions(b.build(), this::onGiveawayInfoOptionItemPressed)
  }

  protected fun onGiveawayInfoOptionItemPressed(optionItemView: View?, id: Int): Boolean {
    if (id == R.id.btn_openLink) {
      val giveawayInfo = this.giveawayInfo
      if (giveawayInfo != null && giveawayInfo.getConstructor() == TdApi.GiveawayInfoCompleted.CONSTRUCTOR) {
        val infoCompleted = giveawayInfo as TdApi.GiveawayInfoCompleted
        tdlib.ui().openInternalLinkType(this, tdlib.tMeGiftCodeUrl(infoCompleted.giftCode), TdApi.InternalLinkTypePremiumGiftCode(infoCompleted.giftCode), null, null)
      }
    }
    return true
  }

  companion object {
    const val BLOCK_MARGIN = 18
    const val CONTENT_PADDING_DP = 16

    private var giveawayStyleProvider: TextStyleProvider? = null

    @JvmStatic
    fun getGiveawayTextStyleProvider(): TextStyleProvider {
      if (giveawayStyleProvider == null) {
        val provider = TextStyleProvider(Fonts.newRobotoStorage()).setTextSize(15f).setAllowSp(true)
        giveawayStyleProvider = provider
        Settings.instance().addChatFontSizeChangeListener(provider)
      }
      return giveawayStyleProvider!!
    }

    @JvmStatic
    fun getDateTime(date: Int): CharSequence {
      return Lang.getString(R.string.format_GiveawayDateTime, Lang.dateYearFull(date.toLong(), TimeUnit.SECONDS), Lang.time(date.toLong(), TimeUnit.SECONDS))
    }
  }
}

private fun genTextWrapper(text: CharSequence, textColorSet: TextColorSet, viewProvider: ViewProvider?): TextWrapper {
  return TextWrapper(null, TD.toFormattedText(text, false), TGMessageGiveawayBase.getGiveawayTextStyleProvider(), textColorSet, null, null)
    .setTextFlagEnabled(Text.FLAG_ALIGN_CENTER, true).setViewProvider(viewProvider)
}
