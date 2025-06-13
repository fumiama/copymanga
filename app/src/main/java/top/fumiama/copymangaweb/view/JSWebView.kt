package top.fumiama.copymangaweb.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.webkit.WebView
import top.fumiama.copymangaweb.web.WebViewClient

@SuppressLint("JavascriptInterface")
class JSWebView : WebView {
    constructor(context: Context): super(context)
    constructor(context: Context, attributeSet: AttributeSet): super(context, attributeSet)
    constructor(context: Context, attributeSet: AttributeSet, defSA: Int): super(context, attributeSet, defSA)
    constructor(context: Context, UA: String) : super(context) { settings.userAgentString = UA }
    init {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        Log.d("MyJSW", "UA is: ${settings.userAgentString}")
    }
    fun setWebViewClient(jsFileName: String){webViewClient = WebViewClient(context, jsFileName)}
    fun loadJSInterface(obj: Any){addJavascriptInterface(obj, "GM")}
}