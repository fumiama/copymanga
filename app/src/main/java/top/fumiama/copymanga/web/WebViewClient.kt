package top.fumiama.copymanga.web

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import top.fumiama.copymanga.R

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
        super.onPageFinished(view, url)
        url?.let {
            view?.postDelayed({view.loadUrl(js)}, 1000)
        }
    }
}