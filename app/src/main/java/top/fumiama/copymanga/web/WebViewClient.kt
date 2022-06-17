package top.fumiama.copymanga.web

import android.content.Context
import android.graphics.Bitmap
import android.net.http.SslError
import android.util.Log
import android.webkit.*
import android.webkit.WebViewClient
import android.widget.Toast
import top.fumiama.copymanga.R
import top.fumiama.copymanga.activity.MainActivity.Companion.mh
import top.fumiama.copymanga.activity.MainActivity.Companion.wm

class WebViewClient(private val context: Context, jsFileName: String):WebViewClient() {
    private val js = context.assets.open(jsFileName).readBytes().decodeToString()
    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        Log.d("MyWC", "Load URL: $url")
        url?.let {
            if(!it.startsWith(context.getString(R.string.web_home)) && !it.startsWith(context.getString(R.string.web_home_www))){
                view?.goBack()
                Toast.makeText(context, R.string.blocked_ad, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        Thread {
            Thread.sleep(500)
            wm?.get()?.runOnUiThread {
                view?.loadUrl(js)
                Log.d("MyWC", "Inject JS into: $url")
                super.onPageFinished(view, url)
            }
        }.start()
    }

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        handler?.proceed() // ignore ssl errors
    }

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        request?.requestHeaders?.set("Access-Control-Allow-Origin", "*")
        return super.shouldInterceptRequest(view, request)
    }
}