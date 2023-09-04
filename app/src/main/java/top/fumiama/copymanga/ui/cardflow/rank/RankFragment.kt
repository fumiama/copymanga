package top.fumiama.copymanga.ui.cardflow.rank

import android.view.View
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.fragment_rank.*
import kotlinx.android.synthetic.main.line_lazybooklines.*
import kotlinx.android.synthetic.main.line_rank.view.*
import top.fumiama.copymanga.MainActivity.Companion.mainWeakReference
import top.fumiama.copymanga.template.ui.InfoCardLoader
import top.fumiama.copymanga.tools.api.CMApi
import top.fumiama.dmzj.copymanga.R
import java.lang.Thread.sleep

@ExperimentalStdlibApi
class RankFragment : InfoCardLoader(R.layout.fragment_rank, R.id.action_nav_rank_to_nav_book, true) {
    private val sortWay = listOf("day", "week", "month", "total")
    private var sortValue = 0

    override fun getApiUrl() =
        getString(R.string.rankApiUrl).format(
                CMApi.myHostApiUrl,
                page * 21,
                sortWay[sortValue]
            )

    override fun setListeners() {
        super.setListeners()
        frlai.lrt.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {}

            override fun onTabSelected(tab: TabLayout.Tab?) {
                setSortValue(tab?.position?:0)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
        })
    }

    override fun onLoadFinish() {
        super.onLoadFinish()
        mainWeakReference?.get()?.runOnUiThread {
            if(ad?.exit == false) mypl.visibility = View.GONE
        }
    }

    private fun setSortValue(value: Int) {
        sortValue = value
        Thread{
            sleep(400)
            if(ad?.exit == false) mh?.sendEmptyMessage(4)
        }.start()
    }
}
