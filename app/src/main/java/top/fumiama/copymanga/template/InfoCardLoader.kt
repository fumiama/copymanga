package top.fumiama.copymanga.template

import android.os.Bundle
import android.util.JsonReader
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.google.gson.Gson
import top.fumiama.dmzj.copymanga.R
import top.fumiama.copymanga.json.BookListStructure
import top.fumiama.copymanga.json.TypeBookListStructure
import top.fumiama.copymanga.tools.DownloadTools
import java.io.File
import java.lang.ref.WeakReference

@ExperimentalStdlibApi
open class InfoCardLoader(inflateRes:Int, private val navId:Int, private val isTypeBook: Boolean = false): MangaPagesFragmentTemplate(inflateRes) {
    private val subUrl get() = getApiUrl()

    init {
        pageHandler = object : PageHandler {
            override fun addPage(){
                AutoDownloadThread(subUrl){
                    if(isRefresh){
                        page = 0
                        isRefresh = false
                    }
                    if(isTypeBook) {
                        val bookList = Gson().fromJson(it?.decodeToString(), TypeBookListStructure::class.java)
                        bookList?.apply {
                            if(results.offset < results.total) {
                                if(code == 200) results.list.forEach { book ->
                                    cardList.addCard(book.comic.name, null, book.comic.cover, book.comic.path_word, null, null, false)
                                }
                            }
                            page++
                        }
                    } else {
                        val bookList = Gson().fromJson(it?.decodeToString(), BookListStructure::class.java)
                        bookList?.apply {
                            if(results.offset < results.total) {
                                if(code == 200) results.list.forEach{ book ->
                                    cardList.addCard(book.name, null, book.cover, book.path_word, null, null, false)
                                }
                            }
                            page++
                        }
                    }
                    onLoadFinish()
                }.start()
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
}