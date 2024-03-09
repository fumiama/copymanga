package top.fumiama.copymanga.template.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import kotlinx.android.synthetic.main.line_lazybooklines.*
import kotlinx.coroutines.launch
import top.fumiama.copymanga.json.BookListStructure
import top.fumiama.copymanga.json.HistoryBookListStructure
import top.fumiama.copymanga.json.ShelfStructure
import top.fumiama.copymanga.json.TypeBookListStructure
import top.fumiama.copymanga.template.general.MangaPagesFragmentTemplate
import top.fumiama.copymanga.template.http.PausableDownloader
import top.fumiama.copymanga.tools.ui.Navigate
import java.lang.ref.WeakReference

@ExperimentalStdlibApi
open class InfoCardLoader(inflateRes:Int, private val navId:Int, private val isTypeBook: Boolean = false, private val isHistoryBook: Boolean = false, private val isShelfBook: Boolean = false): MangaPagesFragmentTemplate(inflateRes) {
    var offset = 0
    private val subUrl get() = getApiUrl()
    var ad: PausableDownloader? = null

    override fun addPage() {
        super.addPage()
        ad = PausableDownloader(subUrl) { data ->
            if(isRefresh) {
                page = 0
                isRefresh = false
            }
            if(isTypeBook) {
                val bookList = Gson().fromJson(data.decodeToString(), TypeBookListStructure::class.java)
                bookList?.apply {
                    Log.d("MyICL", "offset:${results.offset}, total:${results.total}")
                    if(results.offset < results.total) {
                        if(code == 200) {
                            results.list.forEach { book ->
                                if(ad?.exit == true) return@PausableDownloader
                                cardList?.addCard(
                                    book?.comic?.name?:"null", null, book?.comic?.cover,
                                    book?.comic?.path_word, null, null,
                                    isFinish = false, isNew = false
                                )
                            }
                            offset += results.list.size
                        }
                    }
                    page++
                }
            } else if(isHistoryBook) {
                val bookList = Gson().fromJson(data.decodeToString(), HistoryBookListStructure::class.java)
                bookList?.apply {
                    Log.d("MyICL", "offset:${results?.offset}, total:${results?.total}")
                    if(results.offset < results.total) {
                        if(code == 200) {
                            results?.list?.forEach{ book ->
                                if(ad?.exit == true) return@PausableDownloader
                                cardList?.addCard(
                                    book?.comic?.name?:"null", "\n云读至${book?.last_chapter_name}", book?.comic?.cover,
                                    book?.comic?.path_word, null, null,
                                    book?.comic?.status==1
                                )
                            }
                            offset += results.list.size
                        }
                    }
                    page++
                }
            } else if (isShelfBook) {
                val bookList = Gson().fromJson(data.decodeToString(), ShelfStructure::class.java)
                bookList?.apply {
                    Log.d("MyICL", "offset:${results?.offset}, total:${results?.total}")
                    if(results.offset < results.total) {
                        if(code == 200) {
                            results?.list?.forEach{ book ->
                                if(ad?.exit == true) return@PausableDownloader
                                cardList?.addCard(
                                    book?.comic?.name?:"null", "\n${book?.last_browse?.last_browse_name?.let { "读到$it" }?:"未读"}", book?.comic?.cover,
                                    book?.comic?.path_word, null, null,
                                    book?.comic?.status==1,
                                    book.comic?.browse?.chapter_uuid != book.comic?.last_chapter_id
                                )
                            }
                            offset += results.list.size
                        }
                    }
                    page++
                }
            } else {
                val bookList = Gson().fromJson(data.decodeToString(), BookListStructure::class.java)
                bookList?.apply {
                    Log.d("MyICL", "offset:${results?.offset}, total:${results?.total}")
                    if(results.offset < results.total) {
                        if(code == 200) {
                            results?.list?.forEach{ book ->
                                if(ad?.exit == true) return@PausableDownloader
                                cardList?.addCard(book?.name?:"null", null, book?.cover, book?.path_word, null, null, false)
                            }
                            offset += results.list.size
                        }
                    }
                    page++
                }
            }
            onLoadFinish()
        }
        lifecycleScope.launch {
            try {
                ad?.run()
            } catch (e: Exception) {
                e.printStackTrace()
                try {
                    findNavController().popBackStack()
                } catch (_: Exception) {}
            }
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

    override fun onLoadFinish() {
        super.onLoadFinish()
        activity?.runOnUiThread {
            if(ad?.exit != true) mypl.visibility = View.GONE
        }
    }

    override fun reset() {
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
}