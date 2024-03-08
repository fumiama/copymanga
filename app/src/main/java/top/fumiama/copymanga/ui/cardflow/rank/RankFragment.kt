package top.fumiama.copymanga.ui.cardflow.rank

import android.os.Bundle
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.fragment_rank.*
import kotlinx.android.synthetic.main.line_rank.view.*
import top.fumiama.copymanga.template.ui.InfoCardLoader
import top.fumiama.copymanga.tools.api.CMApi
import top.fumiama.copymanga.tools.ui.UITools
import top.fumiama.dmzj.copymanga.R
import java.lang.Thread.sleep
import java.lang.ref.WeakReference

@ExperimentalStdlibApi
class RankFragment : InfoCardLoader(R.layout.fragment_rank, R.id.action_nav_rank_to_nav_book, isTypeBook = true) {
    private val sortWay = listOf("day", "week", "month", "total")
    private var sortValue = 0
    private val audienceWay = listOf("", "male", "female")
    private var audience = 0 // 0 all 1 male 2 female

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wr = WeakReference(this)
    }

    override fun onPause() {
        super.onPause()
        ad?.exit = true
    }

    override fun onResume() {
        super.onResume()
        ad?.exit = true
    }

    override fun onDestroy() {
        super.onDestroy()
        wr = null
        ad?.exit = true
    }

    override fun getApiUrl() =
        getString(R.string.rankApiUrl).format(
                CMApi.myHostApiUrl,
                page * 21,
                sortWay[sortValue],
                audienceWay[audience]
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

    private fun setSortValue(value: Int) {
        sortValue = value
        Thread{
            sleep(400)
            if(ad?.exit != true) activity?.runOnUiThread {
                reset()
                addPage()
            }
        }.start()
    }

    fun showSexInfo(toolsBox: UITools) {
        if (ad?.exit != false) return
        toolsBox.buildInfo("切换类型", "选择一种想筛选的漫画类型",
            "男频", "全部", "女频", {
                audience = 1
                reset()
                Thread {
                    sleep(600)
                    addPage()
                }.start()
            }, {
                audience = 0
                reset()
                Thread {
                    sleep(600)
                    addPage()
                }.start()
            }, {
                audience = 2
                reset()
                Thread {
                    sleep(600)
                    addPage()
                }.start()
            })
    }

    companion object {
        var wr: WeakReference<RankFragment>? = null
    }
}
