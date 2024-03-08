package top.fumiama.copymanga.tools.http

import androidx.preference.PreferenceManager
import top.fumiama.copymanga.MainActivity
import top.fumiama.dmzj.copymanga.R

class Resolution(private val original: Regex) {
    private val imageResolution: Int
        get() {
            MainActivity.mainWeakReference?.get()?.apply {
                PreferenceManager.getDefaultSharedPreferences(this).apply {
                    val b = getString(getString(R.string.imgResolutionKeyID), null)
                    //Log.d("MyResolution", "use image resolution: $b")
                    return b?.toInt()?:1500
                }
            }
            return 1500
        }
    fun wrap(u: String) : String = u.replace(original, "c${imageResolution}x.")
}
