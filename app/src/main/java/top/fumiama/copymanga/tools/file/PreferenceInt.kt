package top.fumiama.copymanga.tools.file

import android.util.Log
import androidx.preference.PreferenceManager
import top.fumiama.copymanga.MainActivity

data class PreferenceInt(private val key: String, private var default: Int) {
    val value: Int
        get() {
            MainActivity.mainWeakReference?.get()?.let {
                PreferenceManager.getDefaultSharedPreferences(it).apply {
                    getInt(key, default).let { v ->
                        Log.d("MyPI", "get key $key value $v")
                        return v
                    }
                }
            }
            return default
        }
}
