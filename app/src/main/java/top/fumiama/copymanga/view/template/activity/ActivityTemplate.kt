package top.fumiama.copymanga.view.template.activity

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import top.fumiama.copymanga.view.interaction.UITools

open class ActivityTemplate: AppCompatActivity() {
    lateinit var toolsBox: UITools
    val pb = BoolPref()
    private val allFullScreen
        get() = getPreferences(MODE_PRIVATE).getBoolean("allFullScreen", false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        toolsBox = UITools(this)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if(allFullScreen) window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }

    inner class BoolPref {
        operator fun get(key: String) = getPreferences(MODE_PRIVATE).getBoolean(key, false)
        operator fun set(key: String, value: Boolean) = getPreferences(MODE_PRIVATE).edit {
            putBoolean(key, value)
            apply()
        }
    }
}