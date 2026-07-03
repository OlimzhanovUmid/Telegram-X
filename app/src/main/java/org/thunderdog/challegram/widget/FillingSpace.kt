package org.thunderdog.challegram.widget

import android.content.Context
import android.view.View
import org.thunderdog.challegram.navigation.ViewController
import org.thunderdog.challegram.support.ViewSupport
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.tool.Views
import me.vkryl.core.lambda.Destroyable

class FillingSpace (context: Context) : View(context), Destroyable {
  init {
    visibility = View.GONE
  }

  fun setLayoutHeight (height: Int, updateVisibility: Boolean): Boolean {
    val updated = Views.setLayoutHeight(this, height)
    if (updateVisibility) {
      visibility = if (height > 0) View.VISIBLE else View.GONE
    }
    return updated
  }

  private var themeProvider: ViewController<*>? = null

  fun setThemedBackground (@ColorId colorId: Int, themeProvider: ViewController<*>?) {
    this.themeProvider = themeProvider
    ViewSupport.setThemedBackground(this, colorId, themeProvider)
  }

  override fun performDestroy () {
    themeProvider?.removeThemeListenerByTarget(this)
    themeProvider = null
  }
}
