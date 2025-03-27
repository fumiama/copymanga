package top.fumiama.copymanga.storage

import android.util.Log
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.core.content.edit
import top.fumiama.copymanga.MainActivity

data class UserPreferenceInt(private val key: String, private var default: Int) {
    var value: Int? = null
        get() {
            field?.let {
                Log.d("MyUPI", "get cached key $key value $it")
                return it
            }
            MainActivity.mainWeakReference?.get()?.let {
                it.getPreferences(MODE_PRIVATE).apply {
                    getInt(key, default).let { v ->
                        field = v
                        Log.d("MyUPI", "get new key $key value $v")
                        return v
                    }
                }
            }
            Log.d("MyUPI", "get default key $key value $default")
            return default
        }
        set(value) {
            if (value == null) {
                Log.d("MyUPI", "remove key $key")
                MainActivity.mainWeakReference?.get()?.let {
                    it.getPreferences(MODE_PRIVATE).apply {
                        edit(commit = true) { remove(key) }
                    }
                }
                field = default
                return
            }
            field = value
            Log.d("MyUPI", "set key $key value $value")
            MainActivity.mainWeakReference?.get()?.let {
                it.getPreferences(MODE_PRIVATE).apply {
                    edit(commit = true) { putInt(key, value) }
                }
            }
        }
}
