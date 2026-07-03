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
 * File created on 15/11/2016
 */
package org.thunderdog.challegram.ui

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import me.vkryl.android.widget.FrameLayoutFix
import org.thunderdog.challegram.R
import org.thunderdog.challegram.navigation.BackHeaderButton
import org.thunderdog.challegram.navigation.DoubleHeaderView
import org.thunderdog.challegram.navigation.ViewController
import org.thunderdog.challegram.support.ViewSupport
import org.thunderdog.challegram.telegram.Tdlib
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.tool.Screen

open class WebkitController<T>(context: Context, tdlib: Tdlib?) : ViewController<T>(context, tdlib) {
  private var webView: WebView? = null
  private var headerCell: DoubleHeaderView? = null

  override fun getId (): Int {
    return R.id.controller_webkit
  }

  @SuppressLint("SetJavaScriptEnabled")
  final override fun onCreateView (context: Context): View {
    val headerCell = DoubleHeaderView(this.context())
    headerCell.setThemedTextColor(this)
    headerCell.initWithMargin(Screen.dp(49f), true)
    this.headerCell = headerCell

    val contentView = object : FrameLayoutFix(context) {
      override fun onTouchEvent (event: MotionEvent): Boolean {
        return true
      }
    }
    ViewSupport.setThemedBackground(contentView, ColorId.filling, this)
    contentView.layoutParams = FrameLayoutFix.newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

    // FIXME android.webkit.WebViewFactory$MissingWebViewPackageException
    val webView = WebView(context)
    this.webView = webView
    webView.settings.javaScriptEnabled = true
    webView.settings.domStorageEnabled = true
    webView.layoutParams = FrameLayoutFix.newParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

    // FIXME maybe better to remove?
    webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
    CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

    if (hasSpecialProcessing()) {
      webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished (view: WebView, url: String) {
          val uri: Uri? = try {
            Uri.parse(url)
          } catch (t: Throwable) {
            null
          }
          if (uri == null || !processSpecial(uri))
            super.onPageFinished(view, url)
        }

        override fun shouldOverrideUrlLoading (view: WebView, request: WebResourceRequest): Boolean {
          return processSpecial(request.url) || super.shouldOverrideUrlLoading(view, request)
        }
      }
    } else {
      webView.webViewClient = WebViewClient()
    }
    webView.webChromeClient = object : WebChromeClient() {
      override fun onProgressChanged (view: WebView, newProgress: Int) {
        onPageProgress(newProgress.toFloat() / 100f)
      }
    }
    onCreateWebView(headerCell, webView)

    contentView.addView(webView)

    return contentView
  }

  override fun getViewForApplyingOffsets (): View? {
    return webView
  }

  protected open fun onCreateWebView (headerCell: DoubleHeaderView, webView: WebView) {
    val arguments = getArguments()
    if (arguments is String) {
      headerCell.setSubtitle(arguments)
      loadUrl(arguments)
    }
  }

  protected fun loadUrl (url: String) {
    webView!!.loadUrl(url)
  }

  protected open fun onPageProgress (progress: Float) {
    headerCell?.animateProgress(progress)
  }

  override fun destroy () {
    super.destroy()
    webView!!.destroy()
  }

  override fun getBackButton (): Int {
    return BackHeaderButton.TYPE_BACK
  }

  override fun getCustomHeaderCell (): View? {
    return headerCell
  }

  protected open fun hasSpecialProcessing (): Boolean {
    return false
  }

  protected open fun processSpecial (url: Uri): Boolean {
    return false // override
  }
}
