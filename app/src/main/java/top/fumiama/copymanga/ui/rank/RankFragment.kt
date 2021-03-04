package top.fumiama.copymanga.ui.rank

import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.zawadz88.materialpopupmenu.popupMenu
import com.google.gson.Gson
import kotlinx.android.synthetic.main.anchor_popular.view.*
import kotlinx.android.synthetic.main.line_rank.*
import top.fumiama.dmzj.copymanga.R
import top.fumiama.copymanga.json.FilterStructure
import top.fumiama.copymanga.template.InfoCardLoader
import java.lang.Thread.sleep

@ExperimentalStdlibApi
class RankFragment: Fragment()/*: InfoCardLoader(R.layout.fragment_rank, R.id.action_nav_rank_to_nav_book, "name", "cover", "id") {
    private var type = 0
    private var pop_sub = 0
    private var filter: Array<FilterStructure>? = null
        get() {
            if (field == null) {
                context?.assets?.open(getString(R.string.assets_filter))?.let {
                    field = Gson().fromJson(it.reader(), Array<FilterStructure>::class.java)
                    it.close()
                }
            }
            return field
        }

    override fun getApiUrl() =
        getString(R.string.rankApiUrl).let { String.format(it, pop_sub, type, page) }

    override fun setListeners() {
        super.setListeners()
        setPop()
        setClasses()
    }

    private fun setPop(){
        line_rank_pop.apt.setText(if(pop_sub == 1) R.string.menu_pop_sub else R.string.menu_pop_pop)
        line_rank_pop.setOnClickListener {
            val popupMenu = popupMenu {
                style = R.style.Widget_MPM_Menu_Dark_CustomBackground
                section {
                    item {
                        labelRes = if(pop_sub == 0) R.string.menu_pop_sub else R.string.menu_pop_pop
                        labelColor = it.apt.currentTextColor
                        iconDrawable =
                            this@RankFragment.context?.let { it1 -> ContextCompat.getDrawable(it1, R.drawable.ic_refresh) } //optional
                        iconColor = it.apt.currentTextColor
                        callback = { //optional
                            if(pop_sub == 0){
                                pop_sub = 1
                                it.apt.setText(R.string.menu_pop_sub)
                            }else{
                                pop_sub = 0
                                it.apt.setText(R.string.menu_pop_pop)
                            }
                            Thread{
                                sleep(400)
                                mh?.sendEmptyMessage(4)
                            }.start()
                        }
                    }
                }
            }
            this.context?.let { it1 -> popupMenu.show(it1, it) }
        }
    }

    private fun setClasses(){
        val items = filter?.get(0)?.items
        line_rank_class.apt.text = items?.get(0)?.tag_name?:getString(R.string.text_null)
        line_rank_class.setOnClickListener {
            val popupMenu = popupMenu {
                style = R.style.Widget_MPM_Menu_Dark_CustomBackground
                if(items != null) section {
                    for(i in items.indices) item {
                        label = items[i]?.tag_name
                        labelColor = it.apt.currentTextColor
                        callback = { //optional
                            it.apt.text = label
                            type = items[i]?.tag_id?:0
                            Thread{
                                sleep(400)
                                mh?.sendEmptyMessage(4)
                            }.start()
                        }
                    }
                }
            }
            this.context?.let { it1 -> popupMenu.show(it1, it) }
        }
    }
}*/