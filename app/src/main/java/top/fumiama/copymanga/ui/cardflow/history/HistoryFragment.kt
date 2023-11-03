package top.fumiama.copymanga.ui.cardflow.history

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.line_lazybooklines.*
import top.fumiama.copymanga.MainActivity
import top.fumiama.copymanga.template.ui.InfoCardLoader
import top.fumiama.copymanga.tools.api.CMApi
import top.fumiama.dmzj.copymanga.R

@OptIn(ExperimentalStdlibApi::class)
class HistoryFragment : InfoCardLoader(R.layout.fragment_history, R.id.action_nav_history_to_nav_book, isHistoryBook = true) {
    override fun getApiUrl() =
        getString(R.string.historyApiUrl).format(CMApi.myHostApiUrl, page * 21)

    override fun onCreate(savedInstanceState: Bundle?) {
        if (MainActivity.member?.hasLogin != true) findNavController().popBackStack()
        super.onCreate(savedInstanceState)
    }
}
