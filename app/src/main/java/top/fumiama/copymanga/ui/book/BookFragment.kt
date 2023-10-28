package top.fumiama.copymanga.ui.book

import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import kotlinx.android.synthetic.main.line_booktandb.*
import top.fumiama.copymanga.MainActivity
import top.fumiama.copymanga.MainActivity.Companion.mainWeakReference
import top.fumiama.copymanga.json.VolumeStructure
import top.fumiama.copymanga.manga.Reader
import top.fumiama.copymanga.template.general.NoBackRefreshFragment
import top.fumiama.copymanga.tools.api.Navigate
import top.fumiama.copymanga.ui.comicdl.ComicDlFragment
import top.fumiama.dmzj.copymanga.R
import java.io.File
import java.lang.Thread.sleep
import java.lang.ref.WeakReference

class BookFragment: NoBackRefreshFragment(R.layout.fragment_book) {
    var fbibinfo: View? = null
    var fbtinfo: View? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ComicDlFragment.exit = false

        if(isFirstInflate) {
            var path = ""
            arguments?.apply {
                if (getBoolean("loadJson")) {
                    getString("name")?.let { name ->
                        mainWeakReference?.get()?.getExternalFilesDir("")?.let {
                            Gson().fromJson(File(File(it, name), "info.json").readText(), Array<VolumeStructure>::class.java)
                        }?.apply {
                            if (isEmpty() || get(0).results.list.isEmpty()) {
                                findNavController().popBackStack()
                                return
                            }
                            else {
                                path = get(0).results.list[0].comic_path_word
                            }
                        }
                    }
                } else getString("path").let {
                    if (it != null) path = it
                    else {
                        findNavController().popBackStack()
                        return
                    }
                }
            }
            bookHandler = BookHandler(WeakReference(this), path)
            Log.d("MyBF", "read path: $path")
            Thread {
                sleep(600)
                bookHandler?.startLoad()
            }.start()
        }
    }

    /*override fun onResume() {
        super.onResume()
        mainWeakReference?.get()?.apply {
            toolbar.title = bookHandler?.book?.results?.comic?.name
        }
        setStartRead()
        fbibinfo?.layoutParams?.height = ((fbibinfo?.width?:0) * 4.0 / 9.0 + 0.5).toInt()
    }*/

    override fun onDestroy() {
        super.onDestroy()
        bookHandler?.destroy()
        bookHandler?.ads?.forEach {
            it.exit = true
        }
        bookHandler = null
    }

    fun setStartRead() {
        if(bookHandler?.chapterNames?.isNotEmpty() == true) mainWeakReference?.get()?.apply {
            bookHandler?.book?.results?.comic?.let { comic ->
                getPreferences(MODE_PRIVATE).getInt(comic.name, -1).let { p ->
                    this@BookFragment.lbbstart.apply {
                        var i = 0
                        if(p >= 0) {
                            text = bookHandler!!.chapterNames[p]
                            i = p
                        }
                        setOnClickListener {
                            Reader.viewMangaAt(comic.name, i)
                        }
                    }
                }
            }
        }
    }

    fun setAddToShelf() {
        if(bookHandler?.chapterNames?.isNotEmpty() == true)
            bookHandler?.book?.results?.comic?.let { comic ->
                this@BookFragment.lbbsub.setOnClickListener {
                    Thread{
                        val re = MainActivity.shelf?.add(comic.uuid)
                        mainWeakReference?.get()?.runOnUiThread {
                            Toast.makeText(context, re, Toast.LENGTH_SHORT).show()
                        }
                    }.start()
                }
            }
    }

    fun navigate2dl(){
        val bundle = Bundle()
        bundle.putString("path", arguments?.getString("path")?:"null")
        bundle.putString("name", bookHandler!!.book?.results?.comic?.name)
        if(bookHandler!!.vols != null) {
            bundle.putBoolean("loadJson", true)
        }
        bundle.putStringArray("group", bookHandler!!.gpws)
        bundle.putStringArray("groupNames", bookHandler!!.keys)
        bundle.putIntArray("count", bookHandler!!.cnts)
        findNavController().let {
            Navigate.safeNavigateTo(it, R.id.action_nav_book_to_nav_group, bundle)
        }
    }

    companion object {
        var bookHandler: BookHandler? = null
    }
}