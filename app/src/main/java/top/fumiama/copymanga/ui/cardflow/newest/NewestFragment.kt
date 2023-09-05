package top.fumiama.copymanga.ui.cardflow.newest

import android.view.View
import kotlinx.android.synthetic.main.line_lazybooklines.*
import top.fumiama.copymanga.MainActivity.Companion.mainWeakReference
import top.fumiama.copymanga.template.ui.InfoCardLoader
import top.fumiama.copymanga.tools.api.CMApi
import top.fumiama.dmzj.copymanga.R

@ExperimentalStdlibApi
class NewestFragment : InfoCardLoader(R.layout.fragment_newest, R.id.action_nav_newest_to_nav_book, true) {
    override fun getApiUrl() =
        getString(R.string.newestApiUrl).format(CMApi.myHostApiUrl, page * 21)
}
