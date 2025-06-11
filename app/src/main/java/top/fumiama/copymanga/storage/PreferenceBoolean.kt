package top.fumiama.copymanga.storage

import android.util.Log
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import top.fumiama.copymanga.MainActivity

data class PreferenceBoolean(private val key: String, private var default: Boolean) {
    var value: Boolean = default
        get() {
            MainActivity.mainWeakReference?.get()?.let {
                PreferenceManager.getDefaultSharedPreferences(it).apply {
                    getBoolean(key, field).let { v ->
                        Log.d("MyPB", "get key $key value $v")
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
                    edit(commit = true) { putBoolean(key, value) }
                }
            }
        }
}
