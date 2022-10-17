package top.fumiama.copymanga.template.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.google.gson.Gson
import top.fumiama.copymanga.json.BookListStructure
import top.fumiama.copymanga.json.TypeBookListStructure
import top.fumiama.copymanga.template.general.MangaPagesFragmentTemplate
import top.fumiama.copymanga.template.http.AutoDownloadThread
import java.lang.ref.WeakReference

@ExperimentalStdlibApi
open class InfoCardLoader(inflateRes:Int, private val navId:Int, private val isTypeBook: Boolean = false): MangaPagesFragmentTemplate(inflateRes) {
    var offset = 0
    private val subUrl get() = getApiUrl()
    var ad: AutoDownloadThread? = null
    init {
        pageHandler = object : PageHandler {
            override fun addPage(){
                ad = AutoDownloadThread(subUrl){
                    if(isRefresh){
                        page = 0
                        isRefresh = false
                    }
                    if(isTypeBook) {
                        val bookList = Gson().fromJson(it?.decodeToString(), TypeBookListStructure::class.java)
                        bookList?.apply {
                            Log.d("MyICL", "offset:${results.offset}, total:${results.total}")
                            if(results.offset < results.total) {
                                if(code == 200) {
                                    results.list.forEach { book ->
                                        if(ad?.exit == true) return@AutoDownloadThread
                                        cardList.addCard(book.comic.name, null, book.comic.cover, book.comic.path_word, null, null, false)
                                    }
                                    offset += results.list.size
                                }
                            }
                            page++
                        }
                    } else {
                        val bookList = Gson().fromJson(it?.decodeToString(), BookListStructure::class.java)
                        bookList?.apply {
                            Log.d("MyICL", "offset:${results.offset}, total:${results.total}")
                            if(results.offset < results.total) {
                                if(code == 200) {
                                    results.list.forEach{ book ->
                                        if(ad?.exit == true) return@AutoDownloadThread
                                        cardList.addCard(book.name, null, book.cover, book.path_word, null, null, false)
                                    }
                                    offset += results.list.size
                                }
                            }
                            page++
                        }
                    }
                    onLoadFinish()
                }
                ad?.start()
            }
            override fun initCardList(weakReference: WeakReference<Fragment>) {
                cardList = CardList(weakReference, cardWidth, cardHeight, cardPerRow)
                cardList.initClickListeners = object : CardList.InitClickListeners {
                    override fun prepareListeners(v: View, name: String, path: String?, chapterUUID: String?, pn: Int?) {
                        v.setOnClickListener {
                            val bundle = Bundle()
                            bundle.putString("path", path)
                            rootView?.let { Navigation.findNavController(it).navigate(navId, bundle) }
                        }
                    }
                }
            }
            override fun setListeners() { this@InfoCardLoader.setListeners() }
        }
    }

    open fun getApiUrl(): String{
        return ""
    }

    open fun setListeners(){}

    open fun onLoadFinish(){}

    override fun onDestroy() {
        super.onDestroy()
        ad?.exit = true
    }
}