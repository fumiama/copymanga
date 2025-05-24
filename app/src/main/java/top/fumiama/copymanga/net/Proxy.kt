package top.fumiama.copymanga.net

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.fumiama.copymanga.MainActivity
import top.fumiama.copymanga.api.Config.proxy_key
import top.fumiama.copymanga.lib.Comancry
import java.net.URLEncoder
import java.nio.charset.Charset

class Proxy(id: Int, private val apiRegex: Regex) {
    private val code get() = proxy_key.value
    private val proxyApiUrl = MainActivity.mainWeakReference?.get()?.getString(id)
    private val sourceDir = MainActivity.mainWeakReference?.get()?.applicationInfo?.sourceDir?.substringBeforeLast("/")?:""
    private val enabled: Boolean get() = code.isNotEmpty() and sourceDir.isNotEmpty() and !proxyApiUrl.isNullOrEmpty()

    fun wrap(u: String): String {
        if(!apiRegex.containsMatchIn(u)) {
            Log.d("MyP", "[N] wrap: $u")
            return u
        }
        if(enabled) {
            val wu = proxyApiUrl?.format(code, URLEncoder.encode(u, Charset.defaultCharset().name()))
                ?:u
            Log.d("MyP", "[M] wrap: $wu")
            return wu
        }
        Log.d("MyP", "[C] wrap: $u")
        //return proxyApiUrl?.format(URLEncoder.encode(u, Charset.defaultCharset().name()))?:u
        return u
    }

    suspend fun comancry(u: String, use: suspend (String) -> ByteArray?): ByteArray? {
        if(!apiRegex.containsMatchIn(u)) {
            Log.d("MyP", "[N] comancry: $u")
            return use(u)
        }
        if(enabled) {
            val wu = proxyApiUrl?.format(code,
                withContext(Dispatchers.IO) {
                    URLEncoder.encode(u, Charset.defaultCharset().name())
                })
                ?:u
            Log.d("MyP", "[M] comancry: $wu, sd: $sourceDir")
            return use(wu)?.let { data ->
                Log.d("MyP", "[M] comancry: decrypt ${data.size} bytes data")
                Comancry.instance.decrypt(sourceDir, data)?.encodeToByteArray()
            }
        }
        Log.d("MyP", "[C] comancry: $u")
        return use(u)
    }
}
