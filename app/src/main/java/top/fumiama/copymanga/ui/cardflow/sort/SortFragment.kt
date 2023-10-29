package top.fumiama.copymanga.ui.cardflow.sort

import android.animation.ObjectAnimator
import com.github.zawadz88.materialpopupmenu.popupMenu
import com.google.gson.Gson
import kotlinx.android.synthetic.main.anchor_popular.view.*
import kotlinx.android.synthetic.main.line_sort.*
import top.fumiama.copymanga.MainActivity.Companion.mainWeakReference
import top.fumiama.copymanga.json.FilterStructure
import top.fumiama.copymanga.template.http.AutoDownloadThread
import top.fumiama.copymanga.template.ui.InfoCardLoader
import top.fumiama.copymanga.tools.api.CMApi
import top.fumiama.dmzj.copymanga.R
import java.lang.Thread.sleep

@ExperimentalStdlibApi
class SortFragment : InfoCardLoader(R.layout.fragment_sort, R.id.action_nav_sort_to_nav_book) {
    private val sortWay = listOf("datetime_updated", "-datetime_updated", "-popular", "popular")
    private var theme = -1
    private var region = -1
    private var sortValue = 0
    private var filter: FilterStructure? = null

    override fun getApiUrl() =
        getString(R.string.sortApiUrl).format(
                CMApi.myHostApiUrl,
                page * 21,
                sortWay[sortValue],
                if(theme >= 0) (filter?.results?.theme?.get(theme)?.path_word ?: "") else "",
                if(region >= 0) (filter?.results?.top?.get(region)?.path_word ?: "") else "",
            )

    override fun setListeners() {
        super.setListeners()
        setUpdate()
        setHot()
        AutoDownloadThread(getString(R.string.filterApiUrl).format(CMApi.myHostApiUrl)) {
            if(ad?.exit == true) return@AutoDownloadThread
            it?.let {
                filter = Gson().fromJson(it.inputStream().reader(), FilterStructure::class.java)
                if(ad?.exit == true) return@AutoDownloadThread
                mainWeakReference?.get()?.runOnUiThread{
                    if(ad?.exit != true) setClasses()
                }
            }
        }.start()
    }

    private fun setUpdate(){
        if(ad?.exit == true) return
        line_sort_time.apt.setText(R.string.menu_update_time)
        line_sort_time.setOnClickListener {
            sortValue = if(it.apim.rotation == 0f) {
                ObjectAnimator.ofFloat(it.apim, "rotation", 0f, 180f).setDuration(233).start()
                1
            }else{
                ObjectAnimator.ofFloat(it.apim, "rotation", 180f, 0f).setDuration(233).start()
                0
            }
            Thread{
                sleep(400)
                mainWeakReference?.get()?.runOnUiThread {
                    reset()
                    addPage()
                }
            }.start()
        }
    }

    private fun setClasses(){
        filter?.results?.top?.let { items ->
            if(ad?.exit == true) return@let
            line_sort_region.apt.text = "全部"
            line_sort_region.setOnClickListener {
                val popupMenu = popupMenu {
                    style = R.style.Widget_MPM_Menu_Dark_CustomBackground
                    section {
                        item {
                            label = "全部"
                            labelColor = it.apt.currentTextColor
                            callback = {
                                region = -1
                                it.apt.text = "全部"
                                Thread{
                                    sleep(400)
                                    mainWeakReference?.get()?.runOnUiThread {
                                        reset()
                                        addPage()
                                    }
                                }.start()
                            }
                        }
                        for(i in items.indices) item {
                            label = items[i].name
                            labelColor = it.apt.currentTextColor
                            callback = { //optional
                                it.apt.text = label
                                region = i
                                Thread{
                                    sleep(400)
                                    mainWeakReference?.get()?.runOnUiThread {
                                        reset()
                                        addPage()
                                    }
                                }.start()
                            }
                        }
                    }
                }
                this.context?.let { it1 -> popupMenu.show(it1, it) }
            }
        }
        filter?.results?.theme?.let { items ->
            if(ad?.exit == true) return@let
            line_sort_class.apt.text = "全部"
            line_sort_class.setOnClickListener {
                val popupMenu = popupMenu {
                    style = R.style.Widget_MPM_Menu_Dark_CustomBackground
                    section {
                        item {
                            label = "全部"
                            labelColor = it.apt.currentTextColor
                            callback = {
                                theme = -1
                                it.apt.text = "全部"
                                Thread{
                                    sleep(400)
                                    mainWeakReference?.get()?.runOnUiThread {
                                        reset()
                                        addPage()
                                    }
                                }.start()
                            }
                        }
                        for(i in items.indices) item {
                            label = items[i].name
                            labelColor = it.apt.currentTextColor
                            callback = { //optional
                                it.apt.text = label
                                theme = i
                                Thread{
                                    sleep(400)
                                    mainWeakReference?.get()?.runOnUiThread {
                                        reset()
                                        addPage()
                                    }
                                }.start()
                            }
                        }
                    }
                }
                this.context?.let { it1 -> popupMenu.show(it1, it) }
            }
        }
    }

    private fun setHot() {
        if(ad?.exit == true) return
        line_sort_hot.apt.setText(R.string.menu_hot)
        line_sort_hot.setOnClickListener {
            sortValue = if (it.apim.rotation == 0f) {
                ObjectAnimator.ofFloat(it.apim, "rotation", 0f, 180f).setDuration(233).start()
                3
            } else {
                ObjectAnimator.ofFloat(it.apim, "rotation", 180f, 0f).setDuration(233).start()
                2
            }
            Thread {
                sleep(400)
                mainWeakReference?.get()?.runOnUiThread {
                    reset()
                    addPage()
                }
            }.start()
        }
    }
}