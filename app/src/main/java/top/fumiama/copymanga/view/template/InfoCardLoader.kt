package top.fumiama.copymanga.view.template

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import kotlinx.android.synthetic.main.line_header.view.*
import kotlinx.android.synthetic.main.line_lazybooklines.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.fumiama.copymanga.json.BookListStructure
import top.fumiama.copymanga.json.HistoryBookListStructure
import top.fumiama.copymanga.json.ShelfStructure
import top.fumiama.copymanga.json.TypeBookListStructure
import top.fumiama.copymanga.net.template.PausableDownloader
import top.fumiama.copymanga.strings.Chinese
import top.fumiama.copymanga.view.interaction.Navigate
import top.fumiama.dmzj.copymanga.R
import java.lang.ref.WeakReference

@ExperimentalStdlibApi
open class InfoCardLoader(inflateRes:Int, private val navId:Int, private val isTypeBook: Boolean = false, private val isHistoryBook: Boolean = false, private val isShelfBook: Boolean = false): MangaPagesFragmentTemplate(inflateRes) {
    var offset = 0
    private val subUrl get() = getApiUrl()
    var ad: PausableDownloader? = null

    @SuppressLint("SetTextI18n")
    override suspend fun addPage(): Unit = withContext(Dispatchers.IO) {
        super.addPage()
        setProgress(20)
        ad = PausableDownloader(subUrl) { data ->
            if(isRefresh) {
                page = 0
                isRefresh = false
            }
            if(isTypeBook) {
                val bookList = Gson().fromJson(data.decodeToString(), TypeBookListStructure::class.java)
                bookList?.apply {
                    Log.d("MyICL", "offset:${results.offset}, total:${results.total}")
                    withContext(Dispatchers.Main) {
                        mysp?.footerView?.lht?.text = "${results.offset}+"
                        activity?.findViewById<Toolbar>(R.id.toolbar)?.let { appbar ->
                            appbar.title.let {
                                if (!it.endsWith(")")) {
                                    appbar.title = "$it (${results.total})"
                                }
                            }
                        }
                    }
                    if(results.offset < results.total) {
                        if(code == 200) {
                            val size = results?.list?.size?:0
                            results.list.forEachIndexed { i, book ->
                                Log.d("MyICL", "load @ $i")
                                if(ad?.exit == true) return@PausableDownloader
                                cardList?.addCard(
                                    book?.comic?.name?:"null", null, book?.comic?.cover,
                                    book?.comic?.path_word, null, null,
                                    isFinish = false, isNew = false
                                )
                                setProgress(20+80*(i+1)/size)
                            }
                            offset += size
                        }
                    }
                    page++
                }
            } else if(isHistoryBook) {
                val bookList = Gson().fromJson(data.decodeToString(), HistoryBookListStructure::class.java)
                bookList?.apply {
                    Log.d("MyICL", "offset:${results?.offset}, total:${results?.total}")
                    withContext(Dispatchers.Main) {
                        mysp?.footerView?.lht?.text = "${results.offset}+"
                        activity?.findViewById<Toolbar>(R.id.toolbar)?.let { appbar ->
                            appbar.title.let {
                                if (!it.endsWith(")")) {
                                    appbar.title = "$it (${results.total})"
                                }
                            }
                        }
                    }
                    if(results.offset < results.total) {
                        if(code == 200) {
                            val size = results?.list?.size?:0
                            results?.list?.forEachIndexed { i, book ->
                                Log.d("MyICL", "load @ $i")
                                if(ad?.exit == true) return@PausableDownloader
                                cardList?.addCard(
                                    book?.comic?.name?:"null", "\n云读至${book?.last_chapter_name?.let { Chinese.fixEncodingIfNeeded(it) }}", book?.comic?.cover,
                                    book?.comic?.path_word, null, null,
                                    book?.comic?.status==1
                                )
                                setProgress(20+80*(i+1)/size)
                            }
                            offset += size
                        }
                    }
                    page++
                }
            } else if (isShelfBook) {
                val bookList = Gson().fromJson(data.decodeToString(), ShelfStructure::class.java)
                bookList?.apply {
                    Log.d("MyICL", "offset:${results?.offset}, total:${results?.total}")
                    withContext(Dispatchers.Main) {
                        mysp?.footerView?.lht?.text = "${results.offset}+"
                        activity?.findViewById<Toolbar>(R.id.toolbar)?.let { appbar ->
                            appbar.title.let {
                                if (!it.endsWith(")")) {
                                    appbar.title = "$it (${results.total})"
                                }
                            }
                        }
                    }
                    if(results.offset < results.total) {
                        if(code == 200) {
                            val size = results?.list?.size?:0
                            results?.list?.forEachIndexed { i, book ->
                                Log.d("MyICL", "load @ $i")
                                if(ad?.exit == true) return@PausableDownloader
                                cardList?.addCard(
                                    book?.comic?.name?:"null", "\n${book?.last_browse?.last_browse_name?.let { "读到${Chinese.fixEncodingIfNeeded(it)}" }?:"未读"}", book?.comic?.cover,
                                    book?.comic?.path_word, null, null,
                                    book?.comic?.status==1,
                                    book.comic?.browse?.chapter_uuid != book.comic?.last_chapter_id
                                )
                                setProgress(20+80*(i+1)/size)
                            }
                            offset += size
                        }
                    }
                    page++
                }
            } else {
                val bookList = Gson().fromJson(data.decodeToString(), BookListStructure::class.java)
                bookList?.apply {
                    Log.d("MyICL", "offset:${results?.offset}, total:${results?.total}")
                    withContext(Dispatchers.Main) {
                        mysp?.footerView?.lht?.text = "${results.offset}+"
                        activity?.findViewById<Toolbar>(R.id.toolbar)?.let { appbar ->
                            appbar.title.let {
                                if (!it.endsWith(")")) {
                                    appbar.title = "$it (${results.total})"
                                }
                            }
                        }
                    }
                    if(results.offset < results.total) {
                        if(code == 200) {
                            val size = results?.list?.size?:0
                            results?.list?.forEachIndexed { i, book ->
                                Log.d("MyICL", "load @ $i")
                                if(ad?.exit == true) return@PausableDownloader
                                cardList?.addCard(book?.name?:"null", null, book?.cover, book?.path_word, null, null, false)
                                setProgress(20+80*(i+1)/size)
                            }
                            offset += size
                        }
                    }
                    page++
                }
            }
            onLoadFinish()
        }
        try {
            ad?.run()
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                withContext(Dispatchers.Main) {
                    findNavController().popBackStack()
                }
            } catch (_: Exception) {}
        }
    }

    override fun initCardList(weakReference: WeakReference<Fragment>) {
        super.initCardList(weakReference)
        cardList = CardList(weakReference, cardWidth, cardHeight, cardPerRow)
        cardList?.initClickListeners = object : CardList.InitClickListeners {
            override fun prepareListeners(v: View, name: String, path: String?, chapterUUID: String?, pn: Int?) {
                v.setOnClickListener {
                    val bundle = Bundle()
                    bundle.putString("path", path)
                    Navigate.safeNavigateTo(findNavController(), navId, bundle)
                }
            }
        }
    }

    open fun getApiUrl(): String{
        return ""
    }

    override suspend fun onLoadFinish() {
        if(ad?.exit != true) super.onLoadFinish()
    }

    override suspend fun reset() {
        super.reset()
        offset = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ad?.exit = false
    }

    override fun onResume() {
        super.onResume()
        ad?.exit = false
    }

    override fun onDestroy() {
        super.onDestroy()
        ad?.exit = true
    }

    fun delayedRefresh(timeMillis: Long) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                delay(timeMillis)
                showKanban()
                reset()
                addPage()
                hideKanban()
            }
        }
    }
}