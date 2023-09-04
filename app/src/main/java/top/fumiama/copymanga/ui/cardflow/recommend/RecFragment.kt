package top.fumiama.copymanga.ui.cardflow.recommend

import android.view.View
import kotlinx.android.synthetic.main.line_lazybooklines.*
import top.fumiama.copymanga.MainActivity.Companion.mainWeakReference
import top.fumiama.copymanga.template.ui.InfoCardLoader
import top.fumiama.copymanga.tools.api.CMApi
import top.fumiama.dmzj.copymanga.R

@ExperimentalStdlibApi
class RecFragment : InfoCardLoader(R.layout.fragment_recommend, R.id.action_nav_recommend_to_nav_book, true) {
    override fun getApiUrl() =
        getString(R.string.recommendApiUrl).format(CMApi.myHostApiUrl, page * 21)

    override fun onLoadFinish() {
        super.onLoadFinish()
        mainWeakReference?.get()?.runOnUiThread {
            if(ad?.exit == false) mypl.visibility = View.GONE
        }
    }
}