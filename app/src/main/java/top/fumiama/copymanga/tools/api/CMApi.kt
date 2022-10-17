package top.fumiama.copymanga.tools.api

import com.bumptech.glide.load.model.LazyHeaders
import top.fumiama.dmzj.copymanga.R
import top.fumiama.copymanga.MainActivity
import top.fumiama.copymanga.ui.settings.SettingsFragment.Companion.settingsPref
import java.io.File

object CMApi {
    var myGlideHeaders: LazyHeaders? = null
        get() {
            if(field === null)
                field = LazyHeaders.Builder()
                    .addHeader("referer", MainActivity.mainWeakReference?.get()?.getString(R.string.referUrl)!!)
                    .addHeader("User-Agent", MainActivity.mainWeakReference?.get()?.getString(R.string.pc_ua)!!)
                    .addHeader("source", "copyApp")
                    .addHeader("webp", "1")
                    .addHeader("region", if(settingsPref?.getBoolean("", false) == false) "1" else "0")
                    .addHeader("platform", "3")
                    .build()
            return field
        }
    fun getZipFile(exDir: File?, manga: String, caption: CharSequence, name: CharSequence) = File(exDir, "$manga/$caption/$name.zip")
    fun getApiUrl(id: Int, arg1: String?, arg2: String?) = MainActivity.mainWeakReference?.get()?.getString(id)?.let { String.format(it, arg1, arg2) }
    fun getApiUrl(id: Int, arg1: String?, arg2: String?, arg3: Int? = 0) = MainActivity.mainWeakReference?.get()?.getString(id)?.let { String.format(it, arg1, arg2, arg3) }
}