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

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.os.CancellationSignal
import androidx.core.view.ViewCompat
import androidx.core.view.isGone
import me.vkryl.android.DECELERATE_INTERPOLATOR
import me.vkryl.android.OVERSHOOT_INTERPOLATOR
import me.vkryl.android.animator.BoolAnimator
import me.vkryl.android.animator.FactorAnimator
import me.vkryl.android.util.SingleViewProvider
import me.vkryl.android.util.ViewProvider
import me.vkryl.core.alphaColor
import me.vkryl.core.clamp
import me.vkryl.core.hasFlag
import me.vkryl.core.lambda.CancellableRunnable
import me.vkryl.core.lambda.Destroyable
import me.vkryl.core.lambda.RunnableData
import me.vkryl.core.lambda.RunnableLong
import me.vkryl.core.reference.ReferenceList
import me.vkryl.core.setFlag
import org.drinkless.tdlib.TdApi
import org.thunderdog.challegram.R
import org.thunderdog.challegram.U
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.data.TD
import org.thunderdog.challegram.data.TGMessage
import org.thunderdog.challegram.loader.ComplexReceiver
import org.thunderdog.challegram.loader.DoubleImageReceiver
import org.thunderdog.challegram.loader.ImageFile
import org.thunderdog.challegram.loader.gif.GifFile
import org.thunderdog.challegram.loader.gif.GifReceiver
import org.thunderdog.challegram.navigation.ViewController.FocusStateListener
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.telegram.TdlibUi.UrlOpenParameters
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.theme.ThemeDelegate
import org.thunderdog.challegram.tool.*
import org.thunderdog.challegram.unsorted.Settings.Companion.instance
import org.thunderdog.challegram.util.text.Text
import org.thunderdog.challegram.util.text.Text.ClickCallback
import org.thunderdog.challegram.util.text.TextColorSetThemed
import org.thunderdog.challegram.util.text.TextMedia
import org.thunderdog.challegram.util.text.TextWrapper
import org.thunderdog.challegram.widget.BaseView
import org.thunderdog.challegram.widget.BaseView.TranslationChangeListener
import org.thunderdog.challegram.widget.BubbleLayout
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

class TooltipOverlayView(context: Context?) : ViewGroup(context) {
  fun interface LocationProvider {
    fun getTargetBounds(targetView: View?, outRect: Rect?)
  }

  fun newContent(tdlib: Tdlib?, text: CharSequence, textFlags: Int): TooltipContentView {
    val formattedText = TdApi.FormattedText(text.toString(), TD.toEntities(text, false))
    return TooltipContentViewText(this, tdlib, formattedText, textFlags, null)
  }

  abstract class TooltipContentView protected constructor(protected val parentView: TooltipOverlayView?) {
    abstract fun layout(info: TooltipInfo, parentWidth: Int, parentHeight: Int, maxWidth: Int): Boolean
    abstract val width: Int
    abstract val height: Int
    abstract fun onTouchEvent(info: TooltipInfo, view: View?, e: MotionEvent?): Boolean
    abstract fun requestIcons(iconReceiver: ComplexReceiver)
    abstract fun draw(c: Canvas, colorProvider: ColorProvider?, left: Int, top: Int, right: Int, bottom: Int, alpha: Float, iconReceiver: ComplexReceiver?)
  }

  interface ColorProvider : TextColorSetThemed {
    fun tooltipColor(): Int {
      return colorTheme().getColor(tooltipColorId())
    }

    fun tooltipColorId(): Int {
      return ColorId.tooltip
    }

    fun tooltipOutlineColor(): Int {
      return colorTheme().getColor(tooltipOutlineColorId())
    }

    fun tooltipOutlineColorId(): Int {
      return ColorId.tooltip_outline
    }

    override fun defaultTextColorId(): Int {
      return ColorId.tooltip_text
    }

    override fun clickableTextColorId(isPressed: Boolean): Int {
      return ColorId.tooltip_textLink
    }

    override fun pressedBackgroundColorId(): Int {
      return ColorId.tooltip_textLinkPressHighlight
    }
  }

  private class TooltipContentViewTextWrapper(parentView: TooltipOverlayView?, private val text: TextWrapper) : TooltipContentView(parentView) {
    init {
      this.text.setViewProvider(SingleViewProvider(parentView))
    }

    override fun layout(info: TooltipInfo, parentWidth: Int, parentHeight: Int, maxWidth: Int): Boolean {
      text.setTextMediaListener { wrapper: TextWrapper?, text: Text?, specificMedia: TextMedia? ->
        if (!text!!.invalidateMediaContent(info.iconReceiver, specificMedia)) {
          text.requestMedia(info.iconReceiver)
        }
      }
      text.prepare(maxWidth)
      return true
    }

    override val width: Int
      get() = text.getWidth()

    override val height: Int
      get() = text.getHeight()

    override fun onTouchEvent(info: TooltipInfo, view: View?, e: MotionEvent?): Boolean {
      return text.onTouchEvent(view, e, info.clickCallback)
    }

    override fun requestIcons(iconReceiver: ComplexReceiver) {
      text.requestMedia(iconReceiver)
    }

    override fun draw(
      c: Canvas,
      colorProvider: ColorProvider?,
      left: Int,
      top: Int,
      right: Int,
      bottom: Int,
      alpha: Float,
      iconReceiver: ComplexReceiver?
    ) {
      text.draw(c, left, right, 0, top, null, alpha, iconReceiver)
    }
  }

  private class TooltipContentViewText(
    parentView: TooltipOverlayView?,
    private val tdlib: Tdlib?,
    val formattedText: TdApi.FormattedText,
    private val textFlags: Int,
    private val urlOpenParameters: UrlOpenParameters?
  ) : TooltipContentView(parentView) {
    private var text: Text? = null

    override val width: Int
      get() = if (text != null) text!!.getWidth() else 0

    override val height: Int
      get() = if (text != null) text!!.getHeight() else 0

    override fun requestIcons(iconReceiver: ComplexReceiver) {
      if (text != null) {
        text!!.requestMedia(iconReceiver)
      } else {
        iconReceiver.clear()
      }
    }

    override fun layout(info: TooltipInfo, parentWidth: Int, parentHeight: Int, maxWidth: Int): Boolean {
      if (text == null || text!!.maxWidth != maxWidth) {
        text = Text.Builder(
          tdlib,
          formattedText,
          urlOpenParameters,
          maxWidth,
          Paints.robotoStyleProvider(info.textSize).setAllowSp(info.allowSp),
          info.colorProvider
        ) { text: Text?, specificMedia: TextMedia? ->
          if (this.text === text) {
            if (!text!!.invalidateMediaContent(info.iconReceiver, specificMedia)) {
              text.requestMedia(info.iconReceiver)
            }
          }
        }
          .textFlags(Text.FLAG_CUSTOM_LONG_PRESS or textFlags)
          .viewProvider(SingleViewProvider(parentView))
          .build()
        return true
      }
      return false
    }

    override fun onTouchEvent(info: TooltipInfo, view: View?, e: MotionEvent?): Boolean {
      return text!!.onTouchEvent(view, e, info.clickCallback)
    }

    override fun draw(
      c: Canvas,
      colorProvider: ColorProvider?,
      left: Int,
      top: Int,
      right: Int,
      bottom: Int,
      alpha: Float,
      iconReceiver: ComplexReceiver?
    ) {
      if (text != null) {
        text!!.draw(c, left, right, 0, top, null, alpha, iconReceiver)
      }
    }
  }

  class TooltipLanguageSelectorView(parentView: TooltipOverlayView?, message: TGMessage, private val listener: OnClickListener) :
    TooltipContentView(parentView) {
    private val arrow: Drawable?
    private val originalLanguage: String
    private val translatedLanguage: String
    private val arrowX: Int
    override val width: Int

    init {
      val currentLang = message.currentTranslatedLanguage
      originalLanguage = Lang.getLanguageName(message.originalMessageLanguage, Lang.getString(R.string.TranslateLangUnknown))
      translatedLanguage = Lang.getLanguageName(message.currentTranslatedLanguage, currentLang
        ?: originalLanguage)
      arrowX = U.measureText(originalLanguage, Paints.getRegularTextPaint(14f)).toInt()
      width = (arrowX + U.measureText(translatedLanguage, Paints.getRegularTextPaint(14f)) + Screen.dp(18f)).toInt()
      arrow = Drawables.get(R.drawable.round_keyboard_arrow_right_16)
    }

    override fun layout(info: TooltipInfo, parentWidth: Int, parentHeight: Int, maxWidth: Int): Boolean {
      return false
    }

    override val height: Int
      get() = Screen.dp(16f)

    override fun onTouchEvent(info: TooltipInfo, view: View?, e: MotionEvent?): Boolean {
      val isInside = info.isInside(e!!.x, e.y)
      if (!isInside) {
        info.hide(true)
        return true
      }
      if (e.action == MotionEvent.ACTION_UP) {
        listener.onClick(view)
      }
      return true
    }

    override fun requestIcons(iconReceiver: ComplexReceiver) {
    }

    override fun draw(
      c: Canvas,
      colorProvider: ColorProvider?,
      left: Int,
      top: Int,
      right: Int,
      bottom: Int,
      alpha: Float,
      iconReceiver: ComplexReceiver?
    ) {
      c.drawText(
        originalLanguage,
        left.toFloat(),
        (top + Screen.dp(14f)).toFloat(),
        Paints.getRegularTextPaint(14f, Theme.getColor(ColorId.tooltip_text))
      )
      c.drawText(
        translatedLanguage,
        (left + arrowX + Screen.dp(18f)).toFloat(),
        (top + Screen.dp(14f)).toFloat(),
        Paints.getRegularTextPaint(14f, Theme.getColor(ColorId.tooltip_textLink))
      )
      Drawables.draw(c, arrow, (left + arrowX + Screen.dp(1f)).toFloat(), top.toFloat(), getPorterDuffPaint(ColorId.tooltip_text))
    }
  }

  interface VisibilityListener {
    fun onVisibilityChanged(tooltipInfo: TooltipInfo?, visibilityFactor: Float)
    fun onVisibilityChangeFinished(tooltipInfo: TooltipInfo?, isVisible: Boolean)
  }

  interface OffsetProvider {
    fun onProvideOffset(rect: RectF?)
  }

  class TooltipInfo internal constructor(
    private val parentView: TooltipOverlayView,
    private val originalView: View?, private val viewProvider: ViewProvider?,
    private val locationProvider: LocationProvider?, colorProvider: ColorProvider?, private val offsetProvider: OffsetProvider?,
    private val controller: ViewController<*>?,
    internal val clickCallback: ClickCallback?, internal val textSize: Float, internal val allowSp: Boolean,
    @DrawableRes iconRes: Int, private val previewFile: ImageFile?, private val imageFile: ImageFile?, private val gifFile: GifFile?,
    maxWidthDp: Float, flags: Int,
    view: TooltipContentView
  ) {
    internal val colorProvider: ColorProvider
    private var icon: Drawable?
    private val maxWidthDp: Float
    internal val flags: Int
    private var popupView: TooltipContentView

    internal val iconReceiver: ComplexReceiver
    private val imageReceiver: DoubleImageReceiver?
    private val gifReceiver: GifReceiver?

    private val position = IntArray(2)
    private val innerRect = Rect()

    private var visibilityListeners: ReferenceList<VisibilityListener?>? = null

    fun isVisible(): Boolean {
      return isAttached && isVisible.getFloatValue() > 0f
    }

    private val isVisible = BoolAnimator(0, object : FactorAnimator.Target {
      override fun onFactorChanged(id: Int, factor: Float, fraction: Float, callee: FactorAnimator?) {
        if (visibilityListeners != null) {
          for (listener in visibilityListeners) {
            listener!!.onVisibilityChanged(this@TooltipInfo, factor)
          }
        }
        if (isAttached) {
          parentView.invalidate()
        }
      }

      override fun onFactorChangeFinished(id: Int, finalFactor: Float, callee: FactorAnimator?) {
        if (visibilityListeners != null) {
          for (listener in visibilityListeners) {
            listener!!.onVisibilityChangeFinished(this@TooltipInfo, finalFactor > 0f)
          }
        }
        if (finalFactor == 0f) {
          detach()
        }
      }
    }, OVERSHOOT_INTERPOLATOR, BubbleLayout.REVEAL_DURATION)

    val contentText: String?
      get() {
        if (popupView is TooltipContentViewText) {
          return (popupView as TooltipContentViewText).formattedText.text
        }
        return null
      }

    fun reset(popupView: TooltipContentView, @DrawableRes iconRes: Int) {
      this.popupView = popupView
      this.icon = Drawables.get(iconRes)
      layout(parentView.measuredWidth, parentView.measuredHeight)
      parentView.invalidate()
    }

    fun reset(tdlib: Tdlib?, text: CharSequence, @DrawableRes iconRes: Int) {
      reset(parentView.newContent(tdlib, text, 0), iconRes)
    }

    fun addListener(listener: VisibilityListener?): TooltipInfo {
      if (visibilityListeners == null) {
        visibilityListeners = ReferenceList<VisibilityListener?>()
      }
      visibilityListeners!!.add(listener)
      return this
    }

    fun removeListener(listener: VisibilityListener?): TooltipInfo {
      if (visibilityListeners != null) {
        visibilityListeners!!.remove(listener)
      }
      return this
    }

    private fun findView(): View? {
      if (viewProvider != null) {
        if (originalView != null && viewProvider.belongsToProvider(originalView)) return originalView
        val view = viewProvider.findAnyTarget()
        if (view != null) return view
      }
      return originalView
    }

    internal var delayedHide: CancellableRunnable? = null

    fun wontHideDelayed(): Boolean {
      return isVisible.getValue() && (delayedHide == null || !delayedHide!!.isPending)
    }

    private fun cancelDelayedHide() {
      shallBeHidden = false
      if (delayedHide != null) {
        delayedHide!!.cancel()
        delayedHide = null
      }
    }

    internal var shallBeHidden = false

    fun isInside(x: Float, y: Float): Boolean {
      return x >= contentRect.left && x < contentRect.right && y >= contentRect.top && y < contentRect.bottom
    }

    val contentRight: Float
      get() = contentRect.right

    val contentBottom: Float
      get() = contentRect.bottom

    val contentTop: Float
      get() = contentRect.top

    fun hideNow() {
      hide(true)
    }

    fun hide(force: Boolean) {
      cancelDelayedShow()
      if (force || delayedHide == null) {
        cancelDelayedHide()
        setIsVisible(isVisible = false, animated = true)
      } else {
        shallBeHidden = true
      }
    }

    private var delayedShowCanceled: CancellationSignal? = null

    private fun cancelDelayedShow() {
      if (delayedShowCanceled != null) {
        delayedShowCanceled!!.cancel()
        delayedShowCanceled = null
      }
    }

    fun show() {
      cancelDelayedHide()
      cancelDelayedShow()
      if (controller == null || controller.isFocused() || isVisible.getFloatValue() > 0f) {
        setIsVisible(isVisible = true, animated = true)
      } else {
        val signal = CancellationSignal()
        this.delayedShowCanceled = signal
        controller.addOneShotFocusListener {
          if (!signal.isCanceled) {
            setIsVisible(isVisible = true, animated = true)
            if (delayedShowCanceled == signal) delayedShowCanceled = null
          }
        }
      }
    }

    fun hideDelayed(duration: Long, unit: TimeUnit): TooltipInfo {
      return hideDelayed(true, duration, unit)
    }

    @JvmOverloads
    fun hideDelayed(shallBeHidden: Boolean = true, duration: Long = 2500, unit: TimeUnit = TimeUnit.MILLISECONDS): TooltipInfo {
      cancelDelayedHide()
      this.shallBeHidden = shallBeHidden
      delayedHide = object : CancellableRunnable() {
        override fun act() {
          hide(true)
        }
      }
      delayedHide!!.removeOnCancel(UI.getAppHandler())
      UI.post(delayedHide, unit.toMillis(duration))
      return this
    }

    private var isAttached = false

    internal fun attach() {
      if (!isAttached) {
        isAttached = true
        parentView.addHint(this)
        popupView.requestIcons(iconReceiver)
        iconReceiver.attach()
        if (imageReceiver != null) {
          imageReceiver.requestFile(previewFile, imageFile)
          imageReceiver.attach()
        }
        if (gifReceiver != null) {
          gifReceiver.requestFile(gifFile)
          gifReceiver.attach()
        }
      }
    }

    fun destroy() {
      cancelDelayedHide()
      detach()
      setIsVisible(isVisible = false, animated = false)
      iconReceiver.performDestroy()
      imageReceiver?.destroy()
      gifReceiver?.destroy()
    }

    private fun detach() {
      if (isAttached) {
        isAttached = false
        attachListeners(null)
        parentView.removeHint(this)
        iconReceiver.detach()
        imageReceiver?.detach()
        gifReceiver?.detach()
      }
    }

    private val destroyable: Destroyable = object : Destroyable {
      override fun performDestroy() {
        hideNow()
      }
    }
    private val focusStateListener = FocusStateListener { c: ViewController<*>?, isFocused: Boolean ->
      if (!isFocused) hideNow()
    }

    private var lastVisibleTime: Long = 0

    private var closeCallbacks: MutableList<RunnableLong>? = null

    fun addOnCloseListener(listener: RunnableLong?): TooltipInfo {
      if (closeCallbacks == null) closeCallbacks = ArrayList<RunnableLong>()
      closeCallbacks!!.add(listener!!)
      return this
    }

    private fun setIsVisible(isVisible: Boolean, animated: Boolean) {
      if (this.isVisible.getValue() != isVisible) {
        if (isVisible && this.isVisible.getFloatValue() == 0f && !hasFlag(flags, FLAG_NO_PIVOT)) {
          this.isVisible.setInterpolator(OVERSHOOT_INTERPOLATOR)
          this.isVisible.setDuration(BubbleLayout.REVEAL_DURATION)
        } else {
          this.isVisible.setInterpolator(DECELERATE_INTERPOLATOR)
          this.isVisible.setDuration(BubbleLayout.DISMISS_DURATION)
        }
        if (controller != null) {
          if (isVisible) {
            controller.addDestroyListener(destroyable)
            controller.addFocusListener(focusStateListener)
          } else {
            controller.removeDestroyListener(destroyable)
            controller.removeFocusListener(focusStateListener)
          }
        }
        if (isVisible) {
          lastVisibleTime = SystemClock.uptimeMillis()
        } else {
          val duration = SystemClock.uptimeMillis() - lastVisibleTime
          if (closeCallbacks != null) {
            for (callback in closeCallbacks) {
              callback.runWithLong(duration)
            }
          }
        }
      }
      if (isVisible) {
        attach()
      }
      this.isVisible.setValue(isVisible, animated)
    }

    fun onTouchEvent(v: View?, e: MotionEvent?): Boolean {
      val hasIcon = hasIcon()
      val innerPaddingLeft = Screen.dp(if (hasIcon) INNER_PADDING_LEFT_WITH_ICON else INNER_PADDING_LEFT)
      val innerPaddingVertical = Screen.dp(if (hasIcon) INNER_PADDING_VERTICAL_WITH_ICON else INNER_PADDING_VERTICAL)
      val iconOffset = if (hasIcon) Screen.dp(ICON_SIZE) + Screen.dp(ICON_HORIZONTAL_MARGIN) else 0
      return popupView.onTouchEvent(this, v, e)
    }

    private var alignBottom = false
    private var pivotX = 0
    private var pivotY = 0
    private val contentRect = RectF()
    private val backgroundPath = Path()

    private fun buildPath(backgroundPath: Path, contentRect: RectF) {
      val pivotWidth = Screen.dp(PIVOT_WIDTH)
      val backgroundRadius = Screen.dp(BACKGROUND_RADIUS)
      val pivotHeight = Screen.dp(PIVOT_HEIGHT)

      backgroundPath.reset()
      backgroundPath.fillType = Path.FillType.EVEN_ODD
      val rectF = Paints.getRectF()
      if (hasFlag(flags, FLAG_NO_PIVOT)) {
        backgroundPath.addRoundRect(contentRect, backgroundRadius.toFloat(), backgroundRadius.toFloat(), Path.Direction.CW)
      } else if (alignBottom) {
        backgroundPath.moveTo(contentRect.left, contentRect.top + backgroundRadius)

        rectF.set(contentRect.left, contentRect.top, contentRect.left + backgroundRadius * 2, contentRect.top + backgroundRadius * 2)
        backgroundPath.arcTo(rectF, -180f, 90f)

        backgroundPath.lineTo(pivotX - pivotWidth / 2f, contentRect.top)
        backgroundPath.rLineTo(pivotWidth / 2f, -pivotHeight.toFloat())
        backgroundPath.rLineTo(pivotWidth / 2f, pivotHeight.toFloat())
        backgroundPath.lineTo(contentRect.right - backgroundRadius, contentRect.top)

        rectF.set(contentRect.right - backgroundRadius * 2, contentRect.top, contentRect.right, contentRect.top + backgroundRadius * 2f)
        backgroundPath.arcTo(rectF, -90f, 90f)
        backgroundPath.lineTo(contentRect.right, contentRect.bottom - backgroundRadius)

        rectF.set(contentRect.right - backgroundRadius * 2, contentRect.bottom - backgroundRadius * 2, contentRect.right, contentRect.bottom)
        backgroundPath.arcTo(rectF, 0f, 90f)
        backgroundPath.lineTo(contentRect.left + backgroundRadius * 2, contentRect.bottom)

        rectF.set(contentRect.left, contentRect.bottom - backgroundRadius * 2, contentRect.left + backgroundRadius * 2, contentRect.bottom)
        backgroundPath.arcTo(rectF, 90f, 90f)
        backgroundPath.lineTo(contentRect.left, contentRect.top + backgroundRadius)
      } else {
        backgroundPath.moveTo(contentRect.left + backgroundRadius, contentRect.bottom)

        // Start from bottom-left corner
        backgroundPath.lineTo(pivotX - pivotWidth / 2f, contentRect.bottom)
        backgroundPath.rLineTo(pivotWidth / 2f, pivotHeight.toFloat())
        backgroundPath.rLineTo(pivotWidth / 2f, -pivotHeight.toFloat())
        backgroundPath.lineTo(contentRect.right - backgroundRadius, contentRect.bottom)

        // Move from bottom to right side
        rectF.set(contentRect.right - backgroundRadius * 2, contentRect.bottom - backgroundRadius * 2, contentRect.right, contentRect.bottom)
        backgroundPath.arcTo(rectF, 90f, -90f)
        backgroundPath.lineTo(contentRect.right, contentRect.top + backgroundRadius)

        // Move from right to top
        rectF.set(contentRect.right - backgroundRadius * 2, contentRect.top, contentRect.right, contentRect.top + backgroundRadius * 2)
        backgroundPath.arcTo(rectF, 0f, -90f)
        backgroundPath.lineTo(contentRect.left + backgroundRadius, contentRect.top)

        // Move from top to left
        rectF.set(contentRect.left, contentRect.top, contentRect.left + backgroundRadius * 2, contentRect.top + backgroundRadius * 2)
        backgroundPath.arcTo(rectF, -90f, -90f)
        backgroundPath.lineTo(contentRect.left, contentRect.bottom - backgroundRadius)

        // Move from left to bottom. The end
        rectF.set(contentRect.left, contentRect.bottom - backgroundRadius * 2, contentRect.left + backgroundRadius * 2, contentRect.bottom)
        backgroundPath.arcTo(rectF, -180f, -90f)
      }
      backgroundPath.close()
    }

    fun reposition() {
      if (layout(parentView.measuredWidth, parentView.measuredHeight) && isVisible.getFloatValue() > 0f) {
        parentView.invalidate()
      }
    }

    private var attachedToView: View? = null
    private var viewAttachedToWindow = false

    private val onLayoutChangeListener =
      OnLayoutChangeListener { v: View?, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int ->
        if (v!!.isGone) {
          hideNow()
        } else {
          reposition()
        }
      }
    private val onTranslationChangeListener = TranslationChangeListener { view: BaseView?, x: Float, y: Float -> reposition() }
    private val onAttachStateChangeListener: OnAttachStateChangeListener = object : OnAttachStateChangeListener {
      override fun onViewAttachedToWindow(v: View) {
        onViewAttachDetach(v, true)
      }

      override fun onViewDetachedFromWindow(v: View) {
        onViewAttachDetach(v, false)
      }
    }

    init {
      this.colorProvider = colorProvider ?: parentView.defaultProvider
      this.icon = Drawables.get(iconRes)
      this.imageReceiver = if (previewFile != null || imageFile != null) DoubleImageReceiver(parentView, 0) else null
      this.gifReceiver = if (gifFile != null) GifReceiver(parentView) else null
      this.iconReceiver = ComplexReceiver(parentView)
      this.maxWidthDp = maxWidthDp
      this.flags = flags
      this.popupView = view
    }

    private fun onViewAttachDetach(v: View?, isAttached: Boolean) {
      if (this.attachedToView === v && this.viewAttachedToWindow != isAttached) {
        this.viewAttachedToWindow = isAttached
        if (isAttached) {
          hideNow()
        }
      }
    }

    private fun attachListeners(view: View?) {
      if (this.attachedToView !== view) {
        if (this.attachedToView != null) {
          this.attachedToView!!.removeOnLayoutChangeListener(onLayoutChangeListener)
          this.attachedToView!!.removeOnAttachStateChangeListener(onAttachStateChangeListener)
          if (attachedToView is BaseView) {
            (attachedToView as BaseView).removeOnTranslationChangeListener(onTranslationChangeListener)
          }
        }
        this.attachedToView = view
        if (view != null) {
          this.viewAttachedToWindow = view.isAttachedToWindow
          view.addOnLayoutChangeListener(onLayoutChangeListener)
          view.addOnAttachStateChangeListener(onAttachStateChangeListener)
          if (view is BaseView) {
            view.addOnTranslationChangeListener(onTranslationChangeListener)
          }
        }
      }
    }

    private fun hasIcon(): Boolean {
      return this.icon != null || this.previewFile != null || this.imageFile != null || this.gifFile != null
    }

    fun layout(parentWidth: Int, parentHeight: Int): Boolean {
      val view = findView()
      val prevPivotX = this.pivotX
      val prevPivotY = this.pivotY
      val prevAlignBottom = this.alignBottom
      if (view != null) {
        view.getLocationOnScreen(position)
        position[0] -= parentView.position[0]
        position[1] -= parentView.position[1]
      } else {
        position[1] = 0
        position[0] = position[1]
      }
      val prevInnerRect = Paints.getRect()
      prevInnerRect.set(innerRect)
      val prevContentRect = Paints.getRectF()
      prevContentRect.set(contentRect)
      if (view != null) {
        val scaleX = view.scaleX
        val scaleY = view.scaleY
        val pivotX = view.pivotX
        val pivotY = view.pivotY
        val width = view.measuredWidth
        val height = view.measuredHeight
        innerRect.set(0, 0, width, height)
        if (scaleX != 1f || scaleY != 1f) {
          val scaledWidth = (width.toFloat() * scaleX).toInt()
          val scaledHeight = (height.toFloat() * scaleY).toInt()
          val diffX = ((width - scaledWidth).toFloat() * (pivotX / width.toFloat())).toInt()
          val diffY = ((height - scaledHeight).toFloat() * (pivotY / height.toFloat())).toInt()
          if (!hasFlag(flags, FLAG_IGNORE_VIEW_SCALE)) {
            innerRect.set(0, 0, scaledWidth, scaledHeight)
            innerRect.offset(diffX, diffY)
          }
          position[0] -= diffX
          position[1] -= diffY
        }
      } else {
        innerRect.set(0, 0, 0, 0)
      }
      if (locationProvider != null) {
        locationProvider.getTargetBounds(view, innerRect)
      } else if (view is LocationProvider) {
        (view as LocationProvider).getTargetBounds(view, innerRect)
      } else {
        val tag = view!!.getTag(R.id.tag_tooltip_location_provider)
        if (tag is LocationProvider) {
          tag.getTargetBounds(view, innerRect)
        }
      }

      val hasIcon = hasIcon()
      val innerPaddingLeft = Screen.dp(if (hasIcon) INNER_PADDING_LEFT_WITH_ICON else INNER_PADDING_LEFT)
      val innerPaddingRight = Screen.dp(if (hasIcon) INNER_PADDING_RIGHT_WITH_ICON else INNER_PADDING_RIGHT)
      val innerPaddingVertical = Screen.dp(if (hasIcon) INNER_PADDING_VERTICAL_WITH_ICON else INNER_PADDING_VERTICAL)
      val verticalOffset = 0
      val horizontalMargin = Screen.dp(8f)
      val iconOffset = if (hasIcon) Screen.dp(ICON_SIZE) + Screen.dp(ICON_HORIZONTAL_MARGIN) else 0
      val fillWidth = hasFlag(flags, FLAG_FILL_WIDTH)

      var maxWidth = min(parentWidth - horizontalMargin * 2, parentHeight - horizontalMargin * 2)
      if (!fillWidth && this.maxWidthDp > 0) {
        maxWidth = min(Screen.dp(this.maxWidthDp), maxWidth)
      }
      val textChanged = popupView.layout(this, parentWidth, parentHeight, maxWidth - innerPaddingLeft - innerPaddingRight - iconOffset)

      val triangleHeight = Screen.dp(12f)
      val totalHeight = popupView.height + innerPaddingVertical * 2 + triangleHeight

      this.alignBottom =
        position[1] + innerRect.top - totalHeight < HeaderView.getTopOffset() && position[1] + innerRect.bottom + totalHeight < parentHeight
      this.pivotX = position[0] + innerRect.centerX()
      this.pivotY = if (alignBottom) position[1] + innerRect.bottom else position[1] + innerRect.top

      val pivotHeight = if (hasFlag(flags, FLAG_NO_PIVOT)) horizontalMargin else Screen.dp(PIVOT_HEIGHT)

      var contentWidth = innerPaddingLeft + innerPaddingRight + popupView.width + iconOffset
      var contentHeight = innerPaddingVertical * 2 + popupView.height

      if (hasIcon) {
        contentWidth = max(contentWidth, innerPaddingLeft * 2 + Screen.dp(ICON_SIZE))
        contentHeight = max(contentHeight, innerPaddingVertical * 2 + Screen.dp(ICON_SIZE))
      }

      if (fillWidth) {
        contentRect.set(pivotX - maxWidth / 2f, 0f, pivotX + maxWidth / 2f, contentHeight.toFloat())
      } else {
        contentRect.set(
          pivotX - contentWidth / 2f,
          0f,
          pivotX + contentWidth / 2f,
          contentHeight.toFloat()
        )
      }
      if (alignBottom) {
        contentRect.offset(0f, (pivotY + pivotHeight + verticalOffset).toFloat())
      } else {
        contentRect.offset(0f, (pivotY - pivotHeight - contentHeight - verticalOffset).toFloat())
      }
      contentRect.offset(max(0f, horizontalMargin - contentRect.left), 0f)
      contentRect.offset(min(0f, (parentWidth - horizontalMargin) - contentRect.right), 0f)
      val checkBound = Screen.dp(PIVOT_WIDTH) / 2 + Screen.dp(BACKGROUND_RADIUS)
      if (pivotX - checkBound < contentRect.left) {
        contentRect.offset((pivotX - checkBound) - contentRect.left, 0f)
      } else if (pivotX + checkBound > contentRect.right) {
        contentRect.offset((pivotX + checkBound) - contentRect.right, 0f)
      }

      offsetProvider?.onProvideOffset(contentRect)

      attachListeners(view)

      if (textChanged || this.pivotX != prevPivotX || this.pivotY != prevPivotY || this.alignBottom != prevAlignBottom || (this.contentRect != prevContentRect)) {
        buildPath(backgroundPath, contentRect)
        return true
      }

      return false
    }

    @Suppress("deprecation")
    fun draw(c: Canvas) {
      val factor: Float = this.isVisible.getFloatValue()
      val alpha = clamp(factor)
      val scale = .8f + .2f * factor

      val rect = Paints.getRect()
      rect.left = pivotX - popupView.width / 2
      rect.right = rect.left + popupView.width

      val needSave = scale != 1f
      val saveCount: Int
      if (needSave) {
        saveCount = c.save()
        if (hasFlag(flags, FLAG_NO_PIVOT)) {
          c.scale(scale, scale, contentRect.centerX(), contentRect.centerY())
        } else {
          c.scale(scale, scale, pivotX.toFloat(), pivotY.toFloat())
        }
      } else {
        saveCount = -1
      }
      val outlineColor = alphaColor(alpha, colorProvider.tooltipOutlineColor())
      if (Color.alpha(outlineColor) > 0) {
        parentView.paint.style = Paint.Style.STROKE
        parentView.paint.strokeWidth = Screen.dp(2f).toFloat()
        parentView.paint.setColor(outlineColor)
        c.drawPath(backgroundPath, parentView.paint)
        parentView.paint.style = Paint.Style.FILL
      }
      parentView.paint.setColor(alphaColor(alpha, colorProvider.tooltipColor()))
      c.drawPath(backgroundPath, parentView.paint)

      val hasIcon = hasIcon()

      val innerPaddingLeft: Int
      val innerPaddingRight: Int
      val innerPaddingVertical: Int
      val iconOffset: Int
      val verticalOffset: Int

      if (hasIcon) {
        innerPaddingLeft = Screen.dp(INNER_PADDING_LEFT_WITH_ICON)
        innerPaddingRight = Screen.dp(INNER_PADDING_RIGHT_WITH_ICON)
        innerPaddingVertical = Screen.dp(INNER_PADDING_VERTICAL_WITH_ICON)
        iconOffset = Screen.dp(ICON_SIZE) + Screen.dp(ICON_HORIZONTAL_MARGIN)

        verticalOffset = max(0, Screen.dp(ICON_SIZE) / 2 - popupView.height / 2)

        val iconSize = Screen.dp(ICON_SIZE)
        val iconLeft = (contentRect.left + innerPaddingLeft).toInt()
        val iconTop = (contentRect.top + innerPaddingVertical).toInt()
        val iconCenterX = iconLeft + iconSize / 2
        val iconCenterY = iconTop + iconSize / 2
        val imageSize = iconSize + min(innerPaddingLeft, Screen.dp(ICON_HORIZONTAL_MARGIN)) / 2

        if (imageReceiver != null && (gifReceiver == null || gifReceiver.needPlaceholder())) {
          imageReceiver.setBounds(iconCenterX - imageSize / 2, iconCenterY - imageSize / 2, iconCenterX + imageSize / 2, iconCenterY + imageSize / 2)
          imageReceiver.paintAlpha = alpha
          imageReceiver.draw(c)
          imageReceiver.restorePaintAlpha()
        }
        if (gifReceiver != null) {
          gifReceiver.setBounds(iconCenterX - imageSize / 2, iconCenterY - imageSize / 2, iconCenterX + imageSize / 2, iconCenterY + imageSize / 2)
          gifReceiver.alpha = alpha
          gifReceiver.draw(c)
        }
        if (icon != null) {
          Drawables.draw(
            c,
            icon,
            iconLeft.toFloat(),
            iconTop.toFloat(),
            Paints.getPorterDuffPaint(alphaColor(alpha, colorProvider.defaultTextColor()))
          )
        }
        if (hasFlag(flags, FLAG_NEED_BLINK)) {
          // TODO
        }
      } else {
        innerPaddingLeft = Screen.dp(INNER_PADDING_LEFT)
        innerPaddingRight = Screen.dp(INNER_PADDING_RIGHT)
        innerPaddingVertical = Screen.dp(INNER_PADDING_VERTICAL)
        verticalOffset = 0
        iconOffset = verticalOffset
      }
      popupView.draw(
        c,
        colorProvider,
        (contentRect.left + innerPaddingLeft + iconOffset).toInt(),
        (contentRect.top + innerPaddingVertical + verticalOffset).toInt(),
        (contentRect.right - innerPaddingRight).toInt(),
        (contentRect.bottom - innerPaddingVertical).toInt(),
        alpha,
        iconReceiver
      )
      if (needSave) {
        c.restoreToCount(saveCount)
      }
    }
  }

  fun interface AvailabilityListener {
    fun onAvailabilityChanged(view: TooltipOverlayView?, hasChildren: Boolean)
  }

  private var availabilityListener: AvailabilityListener? = null
  private val position = IntArray(2)
  private val defaultProvider: ColorProvider = object : ColorProvider {}

  private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)

  fun overrideColorProvider(forcedTheme: ThemeDelegate?): ColorProvider? {
    if (forcedTheme == null) return null
    return object : ColorProvider {
      override fun forcedTheme(): ThemeDelegate {
        return forcedTheme
      }
    }
  }

  fun newFillingColorProvider(forcedTheme: ThemeDelegate?): ColorProvider? {
    if (forcedTheme == null) return null
    return object : ColorProvider {
      override fun forcedTheme(): ThemeDelegate {
        return forcedTheme
      }

      override fun tooltipColorId(): Int {
        return ColorId.filling
      }

      override fun tooltipOutlineColorId(): Int {
        return ColorId.separator
      }

      override fun defaultTextColorId(): Int {
        return ColorId.text
      }

      override fun clickableTextColorId(isPressed: Boolean): Int {
        return ColorId.textLink
      }

      override fun pressedBackgroundColorId(): Int {
        return ColorId.textLinkPressHighlight
      }
    }
  }

  fun setAvailabilityListener(availabilityListener: AvailabilityListener?) {
    this.availabilityListener = availabilityListener
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    getLocationOnScreen(position)
  }

  class TooltipBuilder : Cloneable {
    private val parentView: TooltipOverlayView

    private var originalView: View? = null
    private var viewProvider: ViewProvider? = null
    private var locationProvider: LocationProvider? = null
    private var colorProvider: ColorProvider? = null
    private var offsetProvider: OffsetProvider? = null
    private var clickCallback: ClickCallback? = null
    private var controller: ViewController<*>? = null
    private var textSize = 13f
    private var allowSp = true
    private var flags = 0
    private var iconRes = 0
    private var previewFile: ImageFile? = null
    private var imageFile: ImageFile? = null
    private var gifFile: GifFile? = null
    private var maxWidthDp = 320f
    private var urlOpenParameters: UrlOpenParameters? = null
    private var onBuild: MutableList<RunnableData<TooltipInfo?>>? = null

    internal constructor(parentView: TooltipOverlayView) {
      this.parentView = parentView
    }

    constructor(clone: TooltipBuilder) {
      this.parentView = clone.parentView
      this.originalView = clone.originalView
      this.viewProvider = clone.viewProvider
      this.locationProvider = clone.locationProvider
      this.colorProvider = clone.colorProvider
      this.offsetProvider = clone.offsetProvider
      this.clickCallback = clone.clickCallback
      this.controller = clone.controller
      this.textSize = clone.textSize
      this.allowSp = clone.allowSp
      this.flags = clone.flags
      this.iconRes = clone.iconRes
      this.previewFile = clone.previewFile
      this.imageFile = clone.imageFile
      this.gifFile = clone.gifFile
      this.maxWidthDp = clone.maxWidthDp
      this.urlOpenParameters = clone.urlOpenParameters
      this.onBuild = if (clone.onBuild != null) ArrayList<RunnableData<TooltipInfo?>>(clone.onBuild) else null
    }

    fun anchor(originalView: View?): TooltipBuilder {
      this.originalView = originalView
      return this
    }

    fun anchor(provider: ViewProvider?): TooltipBuilder {
      this.viewProvider = provider
      return this
    }

    fun anchor(originalView: View?, viewProvider: ViewProvider?): TooltipBuilder {
      this.originalView = originalView
      this.viewProvider = viewProvider
      return this
    }

    fun onBuild(act: RunnableData<TooltipInfo?>?): TooltipBuilder {
      if (onBuild == null) onBuild = ArrayList<RunnableData<TooltipInfo?>>()
      onBuild!!.add(act!!)
      return this
    }

    fun colorProvider(): ColorProvider {
      return (if (colorProvider != null) colorProvider else object : ColorProvider {})!!
    }

    fun locate(locationProvider: LocationProvider?): TooltipBuilder {
      this.locationProvider = locationProvider
      return this
    }

    fun maxWidth(dp: Float): TooltipBuilder {
      this.maxWidthDp = dp
      return this
    }

    fun noMaxWidth(): TooltipBuilder {
      return maxWidth(0f)
    }

    fun color(colorProvider: ColorProvider?): TooltipBuilder {
      this.colorProvider = colorProvider
      return this
    }

    fun click(clickCallback: ClickCallback?): TooltipBuilder {
      this.clickCallback = clickCallback
      return this
    }

    fun controller(controller: ViewController<*>?): TooltipBuilder {
      this.controller = controller
      return this
    }

    fun hasController(): Boolean {
      return this.controller != null
    }

    fun hasVisibleTarget(): Boolean {
      if (viewProvider != null) {
        val view = viewProvider!!.findAnyTarget()
        return view != null && ViewCompat.isAttachedToWindow(view)
      }
      if (controller != null) {
        return controller!!.isFocused()
      }
      if (originalView != null) {
        return ViewCompat.isAttachedToWindow(originalView!!)
      }
      return false
    }

    fun textSize(textSize: Float, allowSp: Boolean): TooltipBuilder {
      this.textSize = max(13f, textSize)
      this.allowSp = allowSp
      return this
    }

    @JvmOverloads
    fun chatTextSize(diff: Float = -1f): TooltipBuilder {
      return textSize(instance().chatFontSize + diff, instance().needChatFontSizeScaling())
    }

    fun icon(@DrawableRes iconRes: Int): TooltipBuilder {
      this.iconRes = iconRes
      return this
    }

    fun gif(gif: GifFile?, preview: ImageFile?): TooltipBuilder {
      this.gifFile = gif
      this.previewFile = preview
      return this
    }

    fun image(image: ImageFile?, preview: ImageFile?): TooltipBuilder {
      this.imageFile = image
      this.previewFile = preview
      return this
    }

    fun offset(offsetProvider: OffsetProvider?): TooltipBuilder {
      this.offsetProvider = offsetProvider
      return this
    }

    fun interceptTouchEvents(intercept: Boolean): TooltipBuilder {
      return flag(FLAG_INTERCEPT_TOUCH_EVENTS, intercept)
    }

    fun preventHideOnTouch(preventHideOnTouch: Boolean): TooltipBuilder {
      return flag(FLAG_PREVENT_HIDE_ON_TOUCH, preventHideOnTouch)
    }

    fun fillWidth(fillWidth: Boolean): TooltipBuilder {
      return flag(FLAG_FILL_WIDTH, fillWidth)
    }

    fun noPivot(noPivot: Boolean): TooltipBuilder {
      return flag(FLAG_NO_PIVOT, noPivot)
    }

    fun needBlink(needBlink: Boolean): TooltipBuilder {
      return flag(FLAG_NEED_BLINK, needBlink)
    }

    fun handleBackPress(handleBackPress: Boolean): TooltipBuilder {
      return flag(FLAG_HANDLE_BACK_PRESS, handleBackPress)
    }

    fun ignoreViewScale(ignoreViewScale: Boolean): TooltipBuilder {
      return flag(FLAG_IGNORE_VIEW_SCALE, ignoreViewScale)
    }

    fun flag(flag: Int, enabled: Boolean): TooltipBuilder {
      this.flags = setFlag(flags, flag, enabled)
      return this
    }

    fun source(openParameters: UrlOpenParameters?): TooltipBuilder {
      this.urlOpenParameters = openParameters
      return this
    }

    fun show(controller: ViewController<*>?, tdlib: Tdlib?, iconRes: Int, text: CharSequence): TooltipInfo? {
      if (originalView == null && viewProvider == null && locationProvider == null) {
        UI.showToast(text, Toast.LENGTH_SHORT)
        return null
      } else {
        return icon(iconRes).needBlink(iconRes == R.drawable.baseline_info_24 || iconRes == R.drawable.baseline_error_24)
          .controller(controller?.getParentOrSelf()).show(tdlib, text).hideDelayed(3500, TimeUnit.MILLISECONDS)
      }
    }

    fun show(tdlib: Tdlib?, text: TdApi.FormattedText): TooltipInfo {
      return show(TooltipContentViewText(parentView, tdlib, text, 0, urlOpenParameters))
    }

    fun show(tdlib: Tdlib?, @StringRes stringRes: Int): TooltipInfo {
      return show(tdlib, TdApi.FormattedText(Lang.getString(stringRes), null))
    }

    fun show(tdlib: Tdlib?, text: CharSequence): TooltipInfo {
      return show(tdlib, TdApi.FormattedText(text.toString(), TD.toEntities(text, false)))
    }

    fun show(textWrapper: TextWrapper): TooltipInfo {
      return show(TooltipContentViewTextWrapper(parentView, textWrapper))
    }

    fun show(message: TGMessage, listener: OnClickListener): TooltipInfo {
      return show(TooltipLanguageSelectorView(parentView, message, listener))
    }

    fun show(view: TooltipContentView): TooltipInfo {
      val info = TooltipInfo(
        parentView,
        originalView,
        viewProvider,
        locationProvider,
        colorProvider,
        offsetProvider,
        controller,
        clickCallback,
        textSize,
        allowSp,
        iconRes,
        previewFile,
        imageFile,
        gifFile,
        maxWidthDp,
        flags,
        view
      )
      if (onBuild != null) {
        for (act in onBuild) {
          act.runWithData(info)
        }
      }
      info.attach()
      return info
    }
  }

  fun builder(view: View?): TooltipBuilder {
    return TooltipBuilder(this).anchor(view)
  }

  fun builder(viewProvider: ViewProvider?): TooltipBuilder {
    return TooltipBuilder(this).anchor(viewProvider)
  }

  fun builder(view: View?, viewProvider: ViewProvider?): TooltipBuilder {
    return TooltipBuilder(this).anchor(view, viewProvider)
  }

  fun hideAll(force: Boolean) {
    for (i in activePopups.indices.reversed()) {
      activePopups[i].hide(force)
    }
  }

  fun handleOnBackPress(commit: Boolean): Boolean {
    for (i in activePopups.indices.reversed()) {
      val info = activePopups[i]
      val handled = info.wontHideDelayed()
      if (commit) {
        info.hide(true)
      }
      if (handled || hasFlag(info.flags, FLAG_HANDLE_BACK_PRESS)) {
        return true
      }
    }
    return false
  }

  fun reposition() {
    val viewWidth = measuredWidth
    val viewHeight = measuredHeight
    if (viewWidth > 0 && viewHeight > 0) {
      var invalidate = false
      for (popup in activePopups) {
        invalidate = popup.layout(viewWidth, viewHeight) || invalidate
      }
      if (invalidate) {
        invalidate()
      }
    }
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    reposition()
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    reposition()
  }

  private val activePopups: MutableList<TooltipInfo> = ArrayList<TooltipInfo>()

  private fun removeHint(tooltipInfo: TooltipInfo?) {
    if (activePopups.remove(tooltipInfo)) {
      UI.getContext(context).notifyBackPressAvailabilityChanged()
      if (activePopups.isEmpty()) {
        setWillNotDraw(true)
        if (availabilityListener != null) {
          availabilityListener!!.onAvailabilityChanged(this, false)
        }
      }
    }
  }

  private fun addHint(info: TooltipInfo) {
    for (i in activePopups.indices.reversed()) {
      val tooltipInfo = activePopups[i]
      if (tooltipInfo.delayedHide != null /*&& tooltipInfo.shallBeHidden*/) {
        tooltipInfo.hideNow()
      }
    }
    getLocationOnScreen(position)
    if (measuredWidth > 0 && measuredHeight > 0) {
      info.layout(measuredWidth, measuredHeight)
    }
    this.activePopups.add(info)
    UI.getContext(context).notifyBackPressAvailabilityChanged()
    if (this.activePopups.size == 1) {
      setWillNotDraw(false)
      addOnAttachStateChangeListener(object : OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
          info.show()
          removeOnAttachStateChangeListener(this)
        }

        override fun onViewDetachedFromWindow(v: View) {}
      })
      if (availabilityListener != null) {
        availabilityListener!!.onAvailabilityChanged(this, true)
      }
    } else {
      info.show()
    }
  }

  private var touchingInfo: TooltipInfo? = null
  private var waitUp = false

  init {
    paint.style = Paint.Style.FILL
    setWillNotDraw(true)
  }

  override fun onTouchEvent(e: MotionEvent): Boolean {
    when (e.action) {
      MotionEvent.ACTION_DOWN -> {
        touchingInfo = null
        waitUp = false
        var i = activePopups.size - 1
        while (i >= 0) {
          val info = activePopups[i]
          if (touchingInfo == null && info.onTouchEvent(this, e)) {
            touchingInfo = activePopups[i]
          } else if (hasFlag(info.flags, FLAG_INTERCEPT_TOUCH_EVENTS) && info.isInside(e.x, e.y)) {
            waitUp = true
          } else if (!hasFlag(info.flags, FLAG_PREVENT_HIDE_ON_TOUCH)) {
            info.hide(info.shallBeHidden)
          }
          i--
        }
        return touchingInfo != null || waitUp
      }

      MotionEvent.ACTION_CANCEL -> {
        val value = touchingInfo != null && touchingInfo!!.onTouchEvent(this, e)
        hideAll(true)
        return value
      }
    }
    return (touchingInfo != null && touchingInfo!!.onTouchEvent(this, e)) || waitUp
  }

  override fun onDraw(c: Canvas) {
    for (info in activePopups) {
      info.draw(c)
    }
  }

  companion object {
    private const val INNER_PADDING_LEFT = 8f
    private const val INNER_PADDING_RIGHT = 8f
    private const val INNER_PADDING_VERTICAL = 8f

    private const val ICON_SIZE = 24f
    private const val ICON_HORIZONTAL_MARGIN = 8f
    private const val INNER_PADDING_LEFT_WITH_ICON = 8f
    private const val INNER_PADDING_RIGHT_WITH_ICON = 10f
    private const val INNER_PADDING_VERTICAL_WITH_ICON = 11f
    private const val PIVOT_HEIGHT = 5f
    private const val PIVOT_WIDTH = 10f
    private const val BACKGROUND_RADIUS = 6f

    const val FLAG_INTERCEPT_TOUCH_EVENTS: Int = 1
    const val FLAG_PREVENT_HIDE_ON_TOUCH: Int = 1 shl 1
    const val FLAG_FILL_WIDTH: Int = 1 shl 2
    const val FLAG_NO_PIVOT: Int = 1 shl 3
    const val FLAG_NEED_BLINK: Int = 1 shl 4
    const val FLAG_HANDLE_BACK_PRESS: Int = 1 shl 5
    const val FLAG_IGNORE_VIEW_SCALE: Int = 1 shl 6
  }
}
