package top.fumiama.copymanga.tools.file

import android.util.Log
import androidx.preference.PreferenceManager
import top.fumiama.copymanga.MainActivity

data class PreferenceString(private val key: String, private var default: String?, private val defaultID: Int) {
    constructor(key: Int, default: String?, defaultID: Int): this(
        MainActivity.mainWeakReference?.get()?.getString(key) ?:"", default, defaultID)
    constructor(key: String, default: Int): this(key, null, default)
    constructor(key: Int): this(key, "", 0)

    private val defaultField: String
        get() {
            if (default != null) return default!!
            MainActivity.mainWeakReference?.get()?.let {
                default = it.getString(defaultID)
            }
            return default?:""
        }
    val value: String
        get() {
            MainActivity.mainWeakReference?.get()?.let {
                PreferenceManager.getDefaultSharedPreferences(it).apply {
                    getString(key, null)?.let { v ->
                        if (v.isNotEmpty()) {
                            Log.d("MyPS", "get key $key value $v")
                            return v
                        }
                    }
                }
            }
            Log.d("MyPS", "get default key $key value $defaultField")
            return defaultField
        }
}
