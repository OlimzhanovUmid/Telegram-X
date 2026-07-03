package org.thunderdog.challegram.component.chat

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RelativeLayout

import androidx.annotation.DrawableRes

import org.thunderdog.challegram.R
import org.thunderdog.challegram.navigation.ViewController
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.widget.CircleButton

import me.vkryl.android.DECELERATE_INTERPOLATOR
import me.vkryl.android.animator.BoolAnimator
import me.vkryl.android.animator.FactorAnimator
import me.vkryl.android.widget.FrameLayoutFix

class CircleCounterBadgeView (controller: ViewController<*>, id: Int, onClickListener: View.OnClickListener?, onLongClickListener: View.OnLongClickListener?) : FrameLayout(controller.context()), FactorAnimator.Target {
  companion object {
    private const val DISABLED_BUTTON_ALPHA = .4f

    @JvmField
    val BUTTON_WRAPPER_WIDTH = Screen.dp(118f)
    @JvmField
    val BUTTON_PADDING = Screen.dp(24f)
    @JvmField
    val PADDING = Screen.dp(4f)

    const val ANIMATOR_ENABLED = 0
  }

  private val circleButton: CircleButton
  private val counterBadgeView: CounterBadgeView
  private val animatorEnabled = BoolAnimator(ANIMATOR_ENABLED, this, DECELERATE_INTERPOLATOR, 200L)

  init {
    val context: Context = controller.context()

    val params = RelativeLayout.LayoutParams(Screen.dp(BUTTON_WRAPPER_WIDTH.toFloat()), Screen.dp(74f))
    params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
    params.addRule(RelativeLayout.ABOVE, R.id.msg_bottom)
    params.rightMargin = Screen.dp(16f) - PADDING
    params.bottomMargin = params.rightMargin

    val fParams = FrameLayoutFix.newParams(Screen.dp(24f) * 2 + PADDING * 2, Screen.dp(24f) * 2 + PADDING * 2, Gravity.RIGHT or Gravity.BOTTOM)
    val fParams2 = FrameLayoutFix.newParams(BUTTON_PADDING + fParams.width, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.RIGHT or Gravity.BOTTOM)
    fParams2.bottomMargin = Screen.dp(24f) * 2 - Screen.dp(28f) / 2

    layoutParams = params

    counterBadgeView = CounterBadgeView(context)
    counterBadgeView.layoutParams = fParams2
    counterBadgeView.setPadding(BUTTON_PADDING, 0, 0, 0)

    circleButton = CircleButton(context)
    circleButton.id = id
    if (onClickListener != null) {
      circleButton.setOnClickListener(onClickListener)
    }
    if (onLongClickListener != null) {
      circleButton.setOnLongClickListener(onLongClickListener)
    }
    circleButton.tag = counterBadgeView
    circleButton.layoutParams = fParams

    controller.addThemeInvalidateListener(counterBadgeView)
    controller.addThemeInvalidateListener(circleButton)

    addView(circleButton)
    addView(counterBadgeView)

    setEnabled(true, false)
  }

  fun init (@DrawableRes icon: Int, size: Float, padding: Float, @ColorId backgroundColorId: Int, @ColorId iconColorId: Int) {
    circleButton.init(icon, size, padding, backgroundColorId, iconColorId)
  }

  fun setInProgress (inProgress: Boolean) {
    circleButton.setInProgress(inProgress)
  }

  fun setEnabled (enabled: Boolean, animated: Boolean) {
    animatorEnabled.setValue(enabled, animated)
  }

  fun getEnabled (): Boolean {
    return animatorEnabled.getValue()
  }

  override fun onFactorChanged (id: Int, factor: Float, fraction: Float, callee: FactorAnimator?) {
    when (id) {
      ANIMATOR_ENABLED -> {
        circleButton.setIconAlpha(DISABLED_BUTTON_ALPHA + (1 - DISABLED_BUTTON_ALPHA) * factor)
      }
    }
  }
}
