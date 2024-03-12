package top.fumiama.copymanga.ui.book

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.card_book.*
import kotlinx.android.synthetic.main.fragment_book.*
import kotlinx.android.synthetic.main.line_2chapters.view.*
import kotlinx.android.synthetic.main.line_bookinfo.*
import kotlinx.android.synthetic.main.line_bookinfo_text.*
import kotlinx.android.synthetic.main.line_caption.view.*
import kotlinx.android.synthetic.main.line_chapter.view.*
import kotlinx.android.synthetic.main.page_nested_list.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.fumiama.copymanga.MainActivity.Companion.mainWeakReference
import top.fumiama.copymanga.json.ThemeStructure
import top.fumiama.copymanga.manga.Reader
import top.fumiama.copymanga.tools.api.CMApi
import top.fumiama.copymanga.tools.ui.GlideBlurTransformation
import top.fumiama.copymanga.tools.ui.GlideHideLottieViewListener
import top.fumiama.copymanga.tools.ui.Navigate
import top.fumiama.copymanga.ui.vm.ViewMangaActivity
import top.fumiama.dmzj.copymanga.R
import java.lang.ref.WeakReference

class BookHandler(private val th: WeakReference<BookFragment>): Handler(Looper.myLooper()!!) {
    private val that get() = th.get()
    private var complete = false
    private val divider get() = that?.layoutInflater?.inflate(R.layout.div_h, that?.lbl, false)

    var chapterNames = arrayOf<String>()
    var collect: Int = -1
    var urlArray = arrayOf<String>()
    var exit = false

    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        when(msg.what){
            //0 -> setLayouts()
            1 -> setCover()
            2 -> setTexts()
            3 -> setAuthorsAndTags()
            6 -> if(complete) that?.navigate2dl()
            9 -> endSetLayouts()
            10 -> setVolumes()
        }
    }

    private fun endSetLayouts() {
        if (exit) return
        that?.fbloading?.apply {
            pauseAnimation()
            visibility = View.GONE
        }
        complete = true
        that?.setStartRead()
        that?.setAddToShelf()
        Log.d("MyBH", "Set complete: true")
    }

    private fun setCover() {
        if (exit) return
        that?.apply {
            val load = Glide.with(this).load(
                if (book?.cover != null)
                    GlideUrl(CMApi.imageProxy?.wrap(book?.cover!!)?:book?.cover!!, CMApi.myGlideHeaders)
                else book?.cachedCover
            ).addListener(GlideHideLottieViewListener(WeakReference(laic)))
            load.into(imic)
            context?.let { it1 -> GlideBlurTransformation(it1) }
                ?.let { it2 -> RequestOptions.bitmapTransform(it2) }
                ?.let { it3 -> load.apply(it3).into(lbibg) }
            //imf?.visibility = View.GONE
            //fbl?.addView(divider)
        }
    }

    private fun setTexts() {
        if (exit) return
        that?.apply {
            // tic?.text = book?.name
            // tic?.visibility = View.GONE
            mainWeakReference?.get()?.toolbar?.title = book?.name
            btauth?.text = that?.getString(R.string.text_format_region)?.format(book?.region?:"未知")
            bttag?.text = that?.getString(R.string.text_format_img_type)?.format(book?.imageType?:"未知")
            bthit?.text = that?.getString(R.string.text_format_hit)?.format(book?.popular?:-1)
            btsub?.text = that?.getString(R.string.text_format_stat)?.format(book?.status?:"未知")
            bttime?.text = book?.updateTime?:"未知"
            val v = layoutInflater.inflate(R.layout.line_text_info, lbl, false)
            (v as TextView).text = book?.brief
            lbl?.addView(v)
            lbl?.addView(divider)
        }
    }

    private fun setTheme(caption: String, themeStructure: Array<ThemeStructure>, nav: Int) {
        that?.apply {
            val t = layoutInflater.inflate(R.layout.line_caption, lbl, false)
            t.tcptn.text = caption
            lbl.addView(t)
            lbl.addView(layoutInflater.inflate(R.layout.div_h, lbl, false))
        }
        var line: View? = null
        val last = themeStructure.size - 1
        themeStructure.onEachIndexed { i, it ->
            if(line == null) {
                if(i == last) {
                    line = that?.layoutInflater?.inflate(R.layout.line_chapter, that!!.lbl, false)
                    line?.lcc?.apply {
                        lct.text = it.name
                        lci.setBackgroundResource(R.drawable.ic_list)
                        setOnClickListener { _ ->
                            loadVolume(it.name, it.path_word, nav)
                        }
                    }
                    that?.lbl?.addView(line)
                } else {
                    line = that?.layoutInflater?.inflate(R.layout.line_2chapters, that!!.lbl, false)
                    line?.l2cl?.apply {
                        lct.text = it.name
                        lci.setBackgroundResource(R.drawable.ic_list)
                        setOnClickListener { _ ->
                            loadVolume(it.name, it.path_word, nav)
                        }
                    }
                }
            } else line?.l2cr?.apply {
                lct.text = it.name
                lci.setBackgroundResource(R.drawable.ic_list)
                setOnClickListener { _ ->
                    loadVolume(it.name, it.path_word, nav)
                }
                that?.lbl?.addView(line)
                line = null
            }
        }
    }

    private fun setAuthorsAndTags() {
        if (exit) return
        that?.apply {
            book?.apply {
                author?.let {
                    setTheme(
                        getString(R.string.author),
                        it,
                        R.id.action_nav_book_to_nav_author
                    )
                }
                lbl.addView(layoutInflater.inflate(R.layout.div_h, lbl, false))
                theme?.let {
                    setTheme(
                        getString(R.string.caption),
                        it,
                        R.id.action_nav_book_to_nav_caption
                    )
                }
            }
        }
    }

    private suspend fun addVolumesView(l: LinearLayout, v: View) = withContext(Dispatchers.Main) {
        l.addView(v)
    }

    private suspend fun setVolume(fbl: LinearLayout, p: Int) = withContext(Dispatchers.IO) {
        if (exit) return@withContext
        that?.apply {
            book?.apply {
                var i = 0
                for (j in 0 until p) {
                    i += volumes[j].results?.list?.size?:0
                }
                var last = i-1
                val comicName = name?:return@withContext
                volumes[p].let { v ->
                    if(exit) return@withContext
                    var line: View? = null
                    last += v.results.list.size
                    v.results.list.forEach {
                        val f = CMApi.getZipFile(context?.getExternalFilesDir(""), comicName, keys[p], it.name)
                        //Log.d("MyBH", "i = $i, last=$last, add chapter ${it.name}, line is null: ${line == null}")
                        that?.isOnPause?.let { isOnPause ->
                            while (isOnPause && !exit) delay(500)
                            if (exit) return@withContext
                        }?:return@withContext
                        if(line == null) {
                            if(i == last) {
                                line = layoutInflater.inflate(R.layout.line_chapter, fbl, false)
                                line?.lcc?.apply {
                                    lct.text = it.name
                                    if (f.exists()) lci.setBackgroundResource(R.drawable.ic_success)
                                    Log.d("MyBH", "add last single chapter ${it.name}")
                                    val index = i
                                    setOnClickListener { Reader.start2viewManga(comicName, index, urlArray) }
                                }
                                line?.let { l -> addVolumesView(fbl, l) }
                            } else {
                                line = layoutInflater.inflate(R.layout.line_2chapters, fbl, false)
                                line?.l2cl?.apply {
                                    lct.text = it.name
                                    if (f.exists()) lci.setBackgroundResource(R.drawable.ic_success)
                                    val index = i
                                    setOnClickListener { Reader.start2viewManga(comicName, index, urlArray) }
                                }
                            }
                        } else line?.l2cr?.apply {
                            lct.text = it.name
                            if (f.exists()) lci.setBackgroundResource(R.drawable.ic_success)
                            val index = i
                            setOnClickListener { Reader.start2viewManga(comicName, index, urlArray) }
                            line?.let { l -> addVolumesView(fbl, l) }
                            line = null
                        }
                        i++
                    }
                }
            }
        }
    }

    private suspend fun setViewManga() = withContext(Dispatchers.IO) {
        if (exit) return@withContext
        that?.apply {
            book?.apply {
                val comicName = name?:return@withContext
                ViewMangaActivity.fileArray = arrayOf()
                urlArray = arrayOf()
                ViewMangaActivity.uuidArray = arrayOf()
                var i = 0
                var last = -1
                volumes.forEachIndexed { groupIndex, v ->
                    if(exit) return@withContext
                    last += v.results.list.size
                    v.results.list.forEach {
                        urlArray += CMApi.getChapterInfoApiUrl(
                            path,
                            it.uuid
                        )?:""
                        val f = CMApi.getZipFile(context?.getExternalFilesDir(""), comicName, keys[groupIndex], it.name)
                        ViewMangaActivity.fileArray += f
                        chapterNames += it.name
                        ViewMangaActivity.uuidArray += it.uuid
                        that?.isOnPause?.let { isOnPause ->
                            while (isOnPause && !exit) delay(500)
                            if (exit) return@withContext
                        }?:return@withContext
                        i++
                    }
                }
            }
        }
        sendEmptyMessage(9) // end set layout
    }

    private fun loadVolume(name: String, path: String, nav: Int){
        if(complete) {
            Log.d("MyBH", "start to load chapter")
            val bundle = Bundle()
            bundle.putString("name", name)
            bundle.putString("path", path)
            that?.apply { Navigate.safeNavigateTo(findNavController(), nav, bundle) }
        }
    }

    private fun setVolumes() {
        that?.apply {
            fbtab?.let { tab ->
                fbvp?.let { vp ->
                    vp.adapter = ViewData(vp).RecyclerViewAdapter()
                    TabLayoutMediator(tab, vp) { t, p ->
                        t.text = book?.keys?.get(p)
                    }.attach()
                }
            }
            lifecycleScope.launch { setViewManga() }
        }
    }

    inner class ViewData(itemView: View): RecyclerView.ViewHolder(itemView) {
        inner class RecyclerViewAdapter: RecyclerView.Adapter<ViewData>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewData {
                return ViewData(that?.layoutInflater?.inflate(R.layout.page_nested_list, parent, false) as NestedScrollView)
            }

            override fun onBindViewHolder(holder: ViewData, position: Int) {
                that?.lifecycleScope?.launch { setVolume(holder.itemView.fbl, position) }
            }

            override fun getItemCount(): Int = that?.book?.keys?.size?:0
        }
    }
}
