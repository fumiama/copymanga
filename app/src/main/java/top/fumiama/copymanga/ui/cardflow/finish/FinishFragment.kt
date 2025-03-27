package top.fumiama.copymanga.ui.cardflow.finish

import android.os.Bundle
import android.view.View
import top.fumiama.copymanga.view.template.StatusCardFlow
import top.fumiama.dmzj.copymanga.R
import kotlinx.android.synthetic.main.line_finish.*

@ExperimentalStdlibApi
class FinishFragment : StatusCardFlow(
    R.string.finishApiUrl, R.id.action_nav_finish_to_nav_book, R.layout.fragment_statuscardflow) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lineUpdate = line_finish_time
        lineHot = line_finish_pop
    }
}
