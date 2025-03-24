package top.fumiama.copymanga.tools.file

import android.util.Log
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.core.content.edit
import top.fumiama.copymanga.MainActivity

data class UserPreferenceString(private val key: String, private var default: String?, private val defaultID: Int?) {
    constructor(key: String): this(key, "", 0)
    constructor(key: String, default: Int): this(key, null, default)
    constructor(key: String, default: String): this(key, default, 0)
    private val defaultField: String?
        get() {
            default?.let { return it }
            defaultID?.let { id ->
                MainActivity.mainWeakReference?.get()?.let {
                    default = it.getString(id)
                }
            }
            return default
        }
    var value: String? = null
        get() {
            field?.let {
                Log.d("MyUPS", "get cached key $key value $it")
                return it
            }
            MainActivity.mainWeakReference?.get()?.let {
                it.getPreferences(MODE_PRIVATE).apply {
                    getString(key, null)?.let { v ->
                        if (v.isNotEmpty()) {
                            field = v
                            Log.d("MyUPS", "get new key $key value $v")
                            return v
                        }
                    }
                }
            }
            field = defaultField
            Log.d("MyUPS", "get default key $key value $field")
            return field
        }
        set(value) {
            if (value == null) {
                Log.d("MyUPS", "get remove key $key")
                MainActivity.mainWeakReference?.get()?.let {
                    it.getPreferences(MODE_PRIVATE).apply {
                        edit(commit = true) { remove(key) }
                    }
                }
                field = defaultField
                return
            }
            field = value
            Log.d("MyUPS", "set key $key value $value")
            MainActivity.mainWeakReference?.get()?.let {
                it.getPreferences(MODE_PRIVATE).apply {
                    edit(commit = true) { putString(key, value) }
                }
            }
        }
}
