package top.fumiama.copymanga.tools.http

import android.util.Log
import androidx.preference.PreferenceManager
import top.fumiama.copymanga.MainActivity
import java.net.URLEncoder
import java.nio.charset.Charset

class Proxy(id: Int, apiRegexID: Int, keyID: Int? = null) {
    private val code = keyID?.let { k ->
        MainActivity.mainWeakReference?.get()?.let {
            PreferenceManager.getDefaultSharedPreferences(it).getString(it.getString(k), null)
        }
    }
    private val proxyApiUrl = MainActivity.mainWeakReference?.get()?.getString(id)
    private val apiRegex = Regex(MainActivity.mainWeakReference?.get()?.getString(apiRegexID)?:"<no prefix>")

    fun wrap(u: String): String {
        if(!apiRegex.matches(u)) {
            Log.d("MyP", "[N] wrap: $u")
            return u
        }
        if(!code.isNullOrEmpty()) {
            val wu = proxyApiUrl?.format(code, URLEncoder.encode(u, Charset.defaultCharset().name()))?:u
            Log.d("MyP", "[M] wrap: $wu")
            return wu
        }
        Log.d("MyP", "[C] wrap: $u")
        //return proxyApiUrl?.format(URLEncoder.encode(u, Charset.defaultCharset().name()))?:u
        return u
    }

    companion object {
        val useImageProxy: Boolean
            get() {
                MainActivity.mainWeakReference?.get()?.let {
                    PreferenceManager.getDefaultSharedPreferences(it).apply {
                        val b = getBoolean("settings_cat_net_sw_use_img_proxy", false)
                        Log.d("MyProxy", "use image proxy: $b")
                        return b
                    }
                }
                return false
            }
        /*val useApiProxy: Boolean
            get() {
                MainActivity.mainWeakReference?.get()?.let {
                    PreferenceManager.getDefaultSharedPreferences(it).apply {
                        val b = getBoolean("settings_cat_net_sw_use_api_proxy", false)
                        Log.d("MyProxy", "use api proxy: $b")
                        return b
                    }
                }
                return false
            }*/
    }
}
