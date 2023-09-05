package top.fumiama.copymanga.tools.http

import android.util.Log
import androidx.preference.PreferenceManager
import top.fumiama.copymanga.MainActivity
import java.net.URLEncoder
import java.nio.charset.Charset

class Proxy(id: Int, apiPrefixID: Int, keyID: Int? = null) {
    private val code = keyID?.let { k ->
        MainActivity.mainWeakReference?.get()?.let {
            PreferenceManager.getDefaultSharedPreferences(it).getString(it.getString(k), null)
        }
    }
    private val proxyApiUrl = MainActivity.mainWeakReference?.get()?.getString(id)
    private val apiPrefix = MainActivity.mainWeakReference?.get()?.getString(apiPrefixID)?:"<no prefix>"

    fun wrap(u: String): String {
        if(!u.startsWith(apiPrefix)) return u
        if(code != null) {
            return proxyApiUrl?.format(code, URLEncoder.encode(u, Charset.defaultCharset().name()))?:u
        }
        return proxyApiUrl?.format(URLEncoder.encode(u, Charset.defaultCharset().name()))?:u
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
        val useApiProxy: Boolean
            get() {
                MainActivity.mainWeakReference?.get()?.let {
                    PreferenceManager.getDefaultSharedPreferences(it).apply {
                        val b = getBoolean("settings_cat_net_sw_use_api_proxy", false)
                        Log.d("MyProxy", "use api proxy: $b")
                        return b
                    }
                }
                return false
            }
    }
}
