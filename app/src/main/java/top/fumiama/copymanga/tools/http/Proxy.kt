package top.fumiama.copymanga.tools.http

import android.util.Log
import top.fumiama.copymanga.MainActivity
import top.fumiama.copymanga.api.Config.proxy_key
import java.net.URLEncoder
import java.nio.charset.Charset

class Proxy(id: Int, private val apiRegex: Regex) {
    private val code get() = proxy_key.value
    private val proxyApiUrl = MainActivity.mainWeakReference?.get()?.getString(id)

    fun wrap(u: String): String {
        if(!apiRegex.containsMatchIn(u)) {
            Log.d("MyP", "[N] wrap: $u")
            return u
        }
        if(code.isNotEmpty() and !proxyApiUrl.isNullOrEmpty()) {
            val wu = proxyApiUrl?.format(code, URLEncoder.encode(u, Charset.defaultCharset().name()))
                ?:u
            Log.d("MyP", "[M] wrap: $wu")
            return wu
        }
        Log.d("MyP", "[C] wrap: $u")
        //return proxyApiUrl?.format(URLEncoder.encode(u, Charset.defaultCharset().name()))?:u
        return u
    }
}
