package top.fumiama.copymanga.api

import com.bumptech.glide.load.model.LazyHeaders
import top.fumiama.copymanga.MainActivity
import top.fumiama.copymanga.storage.PreferenceBoolean
import top.fumiama.copymanga.storage.PreferenceInt
import top.fumiama.copymanga.storage.PreferenceString
import top.fumiama.copymanga.storage.UserPreferenceInt
import top.fumiama.copymanga.storage.UserPreferenceString
import top.fumiama.copymanga.net.Proxy
import top.fumiama.copymanga.net.Resolution
import top.fumiama.dmzj.copymanga.R
import java.io.File

object Config {
    var imageProxy: Proxy? = null
        get() {
            if (!net_use_img_proxy.value) return null
            if (field != null) return field
            field = Proxy(
                R.string.imgProxyApiUrl,
                Regex("^https://[0-9a-z-]+\\.mangafun[a-z]\\.(xyz|fun)/"),
            )
            return field
        }
    var apiProxy: Proxy? = null
        get() {
            if (!net_use_api_proxy.value) return null
            if (field != null) return field
            field = Proxy(
                R.string.apiProxyApiUrl,
                Regex("^https://(api|www)\\.(copymanga|mangacopy|copy-manga|copy20)\\.\\w+/api/"),
            )
            return field
        }
    val resolution = Resolution(Regex("c\\d+x\\."))

    var myGlideHeaders: LazyHeaders? = null
        get() {
            if (field === null)
                field = LazyHeaders.Builder()
                    .addHeader("referer", referer)
                    .addHeader("User-Agent", pc_ua)
                    .addHeader("source", "copyApp")
                    .addHeader("webp", "1")
                    .addHeader("version", app_ver.value)
                    .addHeader(
                        "region",
                        if (net_use_foreign.value) "1" else "0"
                    )
                    .addHeader("platform", "3")
                    .build()
            return field
        }

    val myHostApiUrl = PreferenceString("settings_cat_net_et_api_url", R.string.hostUrl)
    val navTextInfo = UserPreferenceString("navTextInfo", R.string.navTextInfo)
    val proxy_key = PreferenceString(R.string.imgProxyKeyID)
    val app_ver = PreferenceString("settings_cat_general_et_app_version", R.string.app_ver)
    val token = UserPreferenceString("token", "", null)
    val pc_ua get() = MainActivity.mainWeakReference?.get()?.getString(R.string.pc_ua)?.format(app_ver.value)?:""
    val referer get() = MainActivity.mainWeakReference?.get()?.getString(R.string.referer)?.format(app_ver.value)?:""
    val comandy_version = UserPreferenceInt("comandy_version", 0)
    val comancry_version = UserPreferenceInt("comancry_version", 0)
    val user_id = UserPreferenceString("user_id")
    val username = UserPreferenceString("username")
    val nickname = UserPreferenceString("nickname")
    val avatar = UserPreferenceString("avatar")

    val general_enable_transparent_system_bar = PreferenceBoolean("settings_cat_general_sw_enable_transparent_systembar", false)
    val general_disable_kanban_animation = PreferenceBoolean("settings_cat_general_sw_disable_kanban_animation", false)
    val general_card_per_row = PreferenceInt("settings_cat_general_sb_card_per_row", 0)

    val manga_dl_max_batch = PreferenceInt("settings_cat_md_sb_max_batch", 16)
    val manga_dl_show_0m_manga = PreferenceBoolean("settings_cat_md_sw_show_0m_manga", false)

    val net_use_comandy = PreferenceBoolean("settings_cat_net_sw_use_comandy", false)
    val net_use_foreign = PreferenceBoolean("settings_cat_net_sw_use_foreign", false)
    private val net_use_img_proxy = PreferenceBoolean("settings_cat_net_sw_use_img_proxy", false)
    val net_use_api_proxy = PreferenceBoolean("settings_cat_net_sw_use_api_proxy", false)
    val net_img_resolution = PreferenceString(R.string.imgResolutionKeyID)

    val view_manga_always_dark_bg = PreferenceBoolean("settings_cat_vm_sw_always_dark_bg", false)
    val view_manga_vertical_max = PreferenceInt("settings_cat_vm_sb_vertical_max", 20)
    val view_manga_quality = PreferenceInt("settings_cat_vm_sb_quality", 100)
    val view_manga_vol_turn = PreferenceBoolean("settings_cat_vm_sw_vol_turn", false)
    val view_manga_use_cellar = PreferenceBoolean("settings_cat_net_sw_use_cellar", false)
    val view_manga_hide_info = PreferenceBoolean("settings_cat_vm_sw_hide_info", false)

    fun getZipFile(exDir: File?, manga: String, caption: CharSequence, name: CharSequence) =
        File(File(File(exDir, manga), caption.toString()), "$name.zip")

    fun getChapterInfoApiUrl(path: String?, uuid: String?, version: Int) =
        MainActivity.mainWeakReference?.get()?.getString(R.string.chapterInfoApiUrl)
            ?.format(myHostApiUrl.value, path, if (version >= 2) "$version" else "" , uuid)
}
