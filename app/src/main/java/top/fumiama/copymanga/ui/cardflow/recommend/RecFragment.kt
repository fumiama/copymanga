package top.fumiama.copymanga.ui.cardflow.recommend

import top.fumiama.copymanga.view.template.InfoCardLoader
import top.fumiama.copymanga.api.Config
import top.fumiama.dmzj.copymanga.R

@ExperimentalStdlibApi
class RecFragment : InfoCardLoader(R.layout.fragment_recommend, R.id.action_nav_recommend_to_nav_book, true) {
    override fun getApiUrl() =
        getString(R.string.recommendApiUrl).format(Config.myHostApiUrl.random(), page * 21)
}