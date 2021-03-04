package top.fumiama.copymanga.template

import android.app.Activity
import android.os.Bundle
import android.view.View
import top.fumiama.copymanga.tools.PropertiesTools
import top.fumiama.copymanga.tools.UITools
import java.io.File
import java.lang.ref.WeakReference

open class ActivityTemplate:Activity() {
    lateinit var p: PropertiesTools
    lateinit var toolsBox: UITools
    private val allFullScreen
        get() = p["allFullScreen"] == "true"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        p = PropertiesTools(File("$filesDir/settings.properties"))
        toolsBox = UITools(WeakReference(this))
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if(allFullScreen) window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }
}