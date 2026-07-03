package org.thunderdog.challegram.charts.view_data

import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Screen
import java.util.ArrayList

class PieLegendView (context: Context) : LegendSignatureView(context) {

  var signature: TextView? = null
  var value: TextView? = null

  init {
    val root = LinearLayout(getContext())
    root.setPadding(Screen.dp(4f), Screen.dp(2f), Screen.dp(4f), Screen.dp(2f))
    val signatureView = TextView(getContext())
    signature = signatureView
    root.addView(signatureView)
    signatureView.layoutParams.width = Screen.dp(96f)
    val valueView = TextView(getContext())
    value = valueView
    root.addView(valueView)
    addView(root)
    valueView.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)

    setPadding(Screen.dp(12f), Screen.dp(12f), Screen.dp(12f), Screen.dp(12f))
    chevron.visibility = View.GONE
    zoomEnabled = false
  }

  override fun recolor () {
    if (signature == null) {
      return
    }
    super.recolor()
    signature!!.setTextColor(Theme.textAccentColor()) // key_dialogTextBlack
  }

  fun setData (name: String?, value: Int, color: Int) {
    signature!!.text = name
    this.value!!.text = value.toString()
    this.value!!.setTextColor(color)
  }

  override fun setSize (n: Int) {
  }

  fun setData (index: Int, date: Long, lines: ArrayList<LineViewData?>?) {
  }
}
