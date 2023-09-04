package top.fumiama.copymanga.tools.api

import android.util.Base64
import androidx.preference.PreferenceManager
import com.bumptech.glide.load.model.LazyHeaders
import top.fumiama.dmzj.copymanga.R
import top.fumiama.copymanga.MainActivity
import top.fumiama.copymanga.tools.http.DownloadTools
import top.fumiama.copymanga.tools.http.Proxy
import java.io.File
import java.net.URLEncoder

object CMApi {
    var proxy = if(Proxy.useProxy) Proxy() else null
    var myGlideHeaders: LazyHeaders? = null
        get() {
            MainActivity.mainWeakReference?.get()?.let {
                PreferenceManager.getDefaultSharedPreferences(it).apply {
                    if(field === null)
                        field = LazyHeaders.Builder()
                            .addHeader("referer", MainActivity.mainWeakReference?.get()?.getString(R.string.referUrl)!!)
                            .addHeader("User-Agent", MainActivity.mainWeakReference?.get()?.getString(R.string.pc_ua)!!)
                            .addHeader("source", "copyApp")
                            .addHeader("webp", "1")
                            .addHeader("region", if(!getBoolean("settings_cat_net", false)) "1" else "0")
                            .addHeader("platform", "3")
                            .build()
                }
            }
            return field
        }
    var myHostApiUrl: String = ""
        get() {
            if(field != "") return field
            MainActivity.mainWeakReference?.get()?.let {
                PreferenceManager.getDefaultSharedPreferences(it).apply {
                    getString("settings_cat_net_et_api_url", "")?.let { host ->
                        if(host != "") {
                            field = host
                            return host
                        }
                    }
                }
                field = it.getString(R.string.hostUrl)
            }
            return field
        }

    fun getZipFile(exDir: File?, manga: String, caption: CharSequence, name: CharSequence) = File(exDir, "$manga/$caption/$name.zip")
    fun getChapterInfoApiUrl(arg1: String?, arg2: String?) =
        MainActivity.mainWeakReference?.get()?.getString(R.string.chapterInfoApiUrl)?.format(myHostApiUrl, arg1, arg2)
    fun getGroupInfoApiUrl(arg1: String?, arg2: String?, arg3: Int? = 0) =
        MainActivity.mainWeakReference?.get()?.getString(R.string.groupInfoApiUrl)?.format(myHostApiUrl, arg1, arg2, arg3)
    fun getLoginConnection(username: String, pwd: String, salt: Int) =
        MainActivity.mainWeakReference?.get()?.getString(R.string.loginApiUrl)?.format(myHostApiUrl)?.let {
            DownloadTools.getConnection(it, "POST")?.apply {
                MainActivity.mainWeakReference?.get()?.let {
                    PreferenceManager.getDefaultSharedPreferences(it).apply {
                        doOutput = true
                        setRequestProperty("content-type", "application/x-www-form-urlencoded;charset=utf-8")
                        setRequestProperty("platform", "3")
                        setRequestProperty("accept", "application/json")
                        val r = if(!getBoolean("settings_cat_net_sw_use_foreign", false)) "1" else "0"
                        val pwdb64 = Base64.encode("$pwd-$salt".toByteArray(), Base64.DEFAULT).decodeToString()
                        outputStream.write("username=${URLEncoder.encode(username)}&password=$pwdb64&salt=$salt&platform=3&authorization=Token+&version=1.4.4&source=copyApp&region=$r&webp=1".toByteArray())
                    }
                }
            }
        }
    }
