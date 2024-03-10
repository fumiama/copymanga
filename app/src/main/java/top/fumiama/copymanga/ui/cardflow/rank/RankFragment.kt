package top.fumiama.copymanga.ui.cardflow.rank

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.fragment_rank.*
import kotlinx.android.synthetic.main.line_rank.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private var isLoading = false

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

    override fun onLoadFinish() {
        super.onLoadFinish()
        isLoading = false
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
                sortValue = tab?.position?:0
                if(!isLoading) delayedRefresh()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun delayedRefresh() {
        lifecycleScope.launch {
            isLoading = true
            withContext(Dispatchers.IO) {
                delay(400)
                withContext(Dispatchers.Main) {
                    reset()
                    addPage()
                }
            }
        }
    }

    fun showSexInfo(toolsBox: UITools) {
        if (ad?.exit != false) return
        toolsBox.buildInfo("切换类型", "选择一种想筛选的漫画类型",
            "男频", "全部", "女频", {
                if(!isLoading) {
                    audience = 1
                    delayedRefresh()
                }
            }, {
                if(!isLoading) {
                    audience = 0
                    delayedRefresh()
                }
            }, {
                if(!isLoading) {
                    audience = 2
                    delayedRefresh()
                }
            })
    }

    companion object {
        var wr: WeakReference<RankFragment>? = null
    }
}
