package top.fumiama.copymangaweb.activity.template

import android.app.Activity
import android.os.Bundle
import top.fumiama.copymangaweb.tool.ToolsBox
import java.lang.ref.WeakReference

open class ToolsBoxActivity: Activity() {
    lateinit var toolsBox: ToolsBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        toolsBox = ToolsBox(WeakReference(this))
    }
}
