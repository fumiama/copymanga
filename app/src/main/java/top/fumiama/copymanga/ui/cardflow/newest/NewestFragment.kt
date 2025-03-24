package top.fumiama.copymanga.ui.cardflow.newest

import top.fumiama.copymanga.template.ui.InfoCardLoader
import top.fumiama.copymanga.api.Config
import top.fumiama.dmzj.copymanga.R

@ExperimentalStdlibApi
class NewestFragment : InfoCardLoader(R.layout.fragment_newest, R.id.action_nav_newest_to_nav_book, true) {
    override fun getApiUrl() =
        getString(R.string.newestApiUrl).format(Config.myHostApiUrl.value, page * 21)
}
