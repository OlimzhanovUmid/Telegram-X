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
package org.thunderdog.challegram.widget

import android.content.Context
import android.graphics.drawable.Animatable
import android.widget.ImageView

import androidx.annotation.DrawableRes

import org.thunderdog.challegram.R
import org.thunderdog.challegram.tool.Drawables
import org.thunderdog.challegram.tool.UI

import me.vkryl.core.lambda.CancellableRunnable

class SwirlView (context: Context) : ImageView(context) {
  // Keep in sync with attrs.
  enum class State {
    OFF,
    ON,
    ERROR,
  }

  private var state = State.OFF
  private var disabler: CancellableRunnable? = null

  fun getState (): State {
    return state
  }

  fun showDelayed (delay: Int) {
    if (state == State.OFF) {
      if (delay > 0) {
        UI.post({
          if (state == State.OFF) {
            setState(State.ON)
          }
        }, delay.toLong())
      } else {
        setState(State.ON)
      }
    }
  }

  fun showError (isFatal: Boolean) {
    if (state == State.ERROR) {
      /*if (disabler != null) {
        UI.removePendingRunnable(disabler);
        postDelayed(disabler, 1000);
      }*/
      return
    }
    if (disabler != null) {
      disabler!!.cancel()
      disabler = null
    }
    val savedState = state
    setState(State.ERROR)
    if (!isFatal) {
      val newDisabler = object : CancellableRunnable() {
        override fun act () {
          if (disabler === this) {
            setState(savedState)
          }
        }
      }
      disabler = newDisabler
      newDisabler.removeOnCancel(UI.getAppHandler())
      UI.post(newDisabler, 1000L)
    }
  }

  fun setState (state: State) {
    setState(state, true)
  }

  fun setState (state: State, animate: Boolean) {
    if (state == this.state) return

    disabler?.let {
      removeCallbacks(it)
    }

    @DrawableRes val resId = getDrawable(this.state, state, animate)
    if (resId == 0) {
      setImageDrawable(null)
    } else {
      val icon = Drawables.get(resources, resId)
      setImageDrawable(icon)
      if (icon is Animatable) {
        icon.start()
      }
    }

    this.state = state
  }

  companion object {
    @DrawableRes
    private fun getDrawable (currentState: State, newState: State, animate: Boolean): Int {
      return when (newState) {
        State.OFF -> {
          if (animate) {
            if (currentState == State.ON) {
              return R.drawable.swirl_draw_off_animation
            } else if (currentState == State.ERROR) {
              return R.drawable.swirl_error_off_animation
            }
          }

          0
        }
        State.ON -> {
          if (animate) {
            if (currentState == State.OFF) {
              return R.drawable.swirl_draw_on_animation
            } else if (currentState == State.ERROR) {
              return R.drawable.swirl_error_state_to_fp_animation
            }
          }

          R.drawable.swirl_fingerprint
        }
        State.ERROR -> {
          if (animate) {
            if (currentState == State.ON) {
              return R.drawable.swirl_fp_to_error_state_animation
            } else if (currentState == State.OFF) {
              return R.drawable.swirl_error_on_animation
            }
          }

          R.drawable.swirl_error
        }
      }
    }
  }
}
