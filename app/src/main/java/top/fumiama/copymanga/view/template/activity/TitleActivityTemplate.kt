package top.fumiama.copymanga.view.template.activity

import android.os.Bundle
import kotlinx.android.synthetic.main.widget_titlebar.*

open class TitleActivityTemplate: ActivityTemplate() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ilogo.setOnClickListener {
            onBackPressed()
        }
    }
}