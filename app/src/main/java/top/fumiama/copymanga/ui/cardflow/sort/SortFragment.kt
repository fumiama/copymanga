package top.fumiama.copymanga.ui.cardflow.sort

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.github.zawadz88.materialpopupmenu.popupMenu
import com.google.gson.Gson
import kotlinx.android.synthetic.main.anchor_popular.view.*
import kotlinx.android.synthetic.main.line_sort.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.fumiama.copymanga.json.FilterStructure
import top.fumiama.copymanga.json.ThemeStructure
import top.fumiama.copymanga.net.template.PausableDownloader
import top.fumiama.copymanga.view.template.cardflow.StatusCardFlow
import top.fumiama.copymanga.api.Config
import top.fumiama.dmzj.copymanga.R

@ExperimentalStdlibApi
class SortFragment : StatusCardFlow(0, R.id.action_nav_sort_to_nav_book, R.layout.fragment_sort) {
    private var theme = -1
    private var region = -1
    private var filter: FilterStructure? = null

    override fun getApiUrl() =
        getString(R.string.sortApiUrl).format(
            page * 21,
            sortWay[sortValue],
            if(theme >= 0 && theme < (filter?.results?.theme?.size ?: 0)) (filter?.results?.theme?.get(theme)?.path_word ?: "") else "",
            if(region >= 0 && region < (filter?.results?.top?.size ?: 0)) (filter?.results?.top?.get(region)?.path_word ?: "") else "",
            Config.platform.value,
        )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lineUpdate = line_sort_time
        lineHot = line_sort_hot
    }

    override fun setListeners() {
        super.setListeners()
        lifecycleScope.launch {
            setProgress(5)
            PausableDownloader(getString(R.string.filterApiUrl).format(Config.platform.value)) {
                if(ad?.exit == true) return@PausableDownloader
                it.let {
                    it.inputStream().use { i ->
                        filter = Gson().fromJson(i.reader(), FilterStructure::class.java)
                    }
                    if(ad?.exit == true) return@PausableDownloader
                    withContext(Dispatchers.Main) {
                        if(ad?.exit != true) setClasses()
                    }
                }
            }.run()
        }
    }

    private fun setClasses() {
        setProgress(10)
        filter?.results?.top?.let { items ->
            setMenu(items, line_sort_region) {
                region = it
            }
        }
        setProgress(15)
        filter?.results?.theme?.let { items ->
            setMenu(items, line_sort_class) {
                theme = it
            }
        }
    }

    private fun setMenu(items: Array<out ThemeStructure>, line: View, setIndex: (Int) -> Unit) {
        if(ad?.exit == true) return
        line.apt.text = "全部"
        line.setOnClickListener {
            val popupMenu = popupMenu {
                style = R.style.Widget_MPM_Menu_Dark_CustomBackground
                section {
                    item {
                        label = "全部"
                        labelColor = it.apt.currentTextColor
                        callback = {
                            setIndex(-1)
                            it.apt.text = "全部"
                            delayedRefresh(400)
                        }
                    }
                    for(i in items.indices) item {
                        label = items[i].name
                        labelColor = it.apt.currentTextColor
                        callback = { //optional
                            it.apt.text = label
                            setIndex(i)
                            delayedRefresh(400)
                        }
                    }
                }
            }
            context?.let { c -> popupMenu.show(c, it) }
        }
    }
}
