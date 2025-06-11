package top.fumiama.copymanga.storage

import android.util.Log
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import top.fumiama.copymanga.MainActivity

data class PreferenceInt(private val key: String, private var default: Int) {
    var value: Int = default
        get() {
            MainActivity.mainWeakReference?.get()?.let {
                PreferenceManager.getDefaultSharedPreferences(it).apply {
                    getInt(key, field).let { v ->
                        Log.d("MyPI", "get key $key value $v")
                        return v
                    }
                }
            }
            return field
        }
        set(value) {
            field = value
            Log.d("MyUPI", "set key $key value $value")
            MainActivity.mainWeakReference?.get()?.let {
                PreferenceManager.getDefaultSharedPreferences(it).apply {
                    edit(commit = true) { putInt(key, value) }
                }
            }
        }
}
