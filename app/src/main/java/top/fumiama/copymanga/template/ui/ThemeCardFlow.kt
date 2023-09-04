package top.fumiama.copymanga.template.ui

import android.os.Bundle
import kotlinx.android.synthetic.main.app_bar_main.*
import top.fumiama.copymanga.MainActivity.Companion.mainWeakReference
import top.fumiama.copymanga.tools.api.CMApi

@ExperimentalStdlibApi
open class ThemeCardFlow(private val api: Int, nav: Int) : StatusCardFlow(0, nav) {
    private var theme = ""
    override fun getApiUrl() =
        getString(api).format(
            CMApi.myHostApiUrl,
            page * 21,
            sortWay[sortValue],
            theme
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.apply {
            getString("path")?.apply { theme = this }
            getString("name")?.apply {
                mainWeakReference?.get()?.toolbar?.title = this
            }
        }
    }

    override fun onResume() {
        super.onResume()
        arguments?.getString("name")?.apply {
            mainWeakReference?.get()?.toolbar?.title = this
        }
    }
}