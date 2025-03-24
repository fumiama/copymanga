package top.fumiama.copymanga.tools.file

import android.util.Log
import androidx.preference.PreferenceManager
import top.fumiama.copymanga.MainActivity

data class PreferenceBoolean(private val key: String, private var default: Boolean) {
    val value: Boolean
        get() {
            MainActivity.mainWeakReference?.get()?.let {
                PreferenceManager.getDefaultSharedPreferences(it).apply {
                    getBoolean(key, default).let { v ->
                        Log.d("MyPB", "get key $key value $v")
                        return v
                    }
                }
            }
            return default
        }
}
