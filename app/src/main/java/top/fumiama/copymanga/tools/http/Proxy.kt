package top.fumiama.copymanga.tools.http

import android.util.Log
import androidx.preference.PreferenceManager
import top.fumiama.copymanga.MainActivity
import top.fumiama.dmzj.copymanga.R
import java.net.URLEncoder
import java.nio.charset.Charset

class Proxy(private val code: String) {
    constructor(): this(
        MainActivity.mainWeakReference?.get()?.let {
            PreferenceManager.getDefaultSharedPreferences(it).let {sp ->
                sp.getString("settings_cat_net_et_img_proxy_code", "")
            }
        }?:""
    )

    fun wrap(u: String): String {
        return MainActivity.mainWeakReference?.get()?.getString(R.string.imgProxyApiUrl)
            ?.format(code, URLEncoder.encode(u, Charset.defaultCharset().name()))
            ?:u
    }

    companion object {
        val useProxy: Boolean
            get() {
                MainActivity.mainWeakReference?.get()?.let {
                    PreferenceManager.getDefaultSharedPreferences(it).apply {
                        val b = getBoolean("settings_cat_net_sw_use_img_proxy", false)
                        Log.d("MyProxy", "use proxy: $b")
                        return b
                    }
                }
                return false
            }
    }
}
