package top.fumiama.copymangaweb.web

import android.net.Uri
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import top.fumiama.copymangaweb.activity.MainActivity.Companion.wm

class WebChromeClient:WebChromeClient() {
    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        //Log.d("MyWCC", "W progress: $newProgress")
        wm?.get()?.updateLoadProgress(newProgress)
    }

    override fun onJsAlert(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        result?.confirm()
        return true
    }

    override fun onJsPrompt(
        view: WebView?,
        url: String?,
        message: String?,
        defaultValue: String?,
        result: JsPromptResult?
    ): Boolean {
        result?.confirm()
        return true
    }

    override fun onJsConfirm(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        result?.confirm()
        return true
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        wm?.get()?.apply {
            uploadMessageAboveL = filePathCallback
            openImageChooserActivity()
        }
        return true
    }
}