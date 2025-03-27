package top.fumiama.copymanga.net

import top.fumiama.copymanga.api.Config

class Resolution(private val original: Regex) {
    private val imageResolution: Int
        get() = Config.net_img_resolution.value.let {
            if (it.isNotEmpty()) it.toInt() else 1500
        }
    fun wrap(u: String) : String = u.replace(original, "c${imageResolution}x.")
}
