package top.fumiama.copymanga.ui.book

import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import androidx.navigation.Navigation
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.line_booktandb.*
import top.fumiama.dmzj.copymanga.R
import top.fumiama.copymanga.MainActivity.Companion.mainWeakReference
import top.fumiama.copymanga.template.general.NoBackRefreshFragment
import java.lang.Thread.sleep
import java.lang.ref.WeakReference

class BookFragment: NoBackRefreshFragment(R.layout.fragment_book) {
    private lateinit var bookHandler: BookHandler
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if(isFirstInflate) {
            bookHandler = BookHandler(WeakReference(this), arguments?.getString("path")?:"null")
            Thread{
                sleep(600)
                bookHandler.startLoad()
            }.start()
        }
        else bookHandler.fbibinfo?.layoutParams?.height = (bookHandler.fbibinfo?.width?:0 * 4.0 / 9.0 + 0.5).toInt()
    }

    override fun onResume() {
        super.onResume()
        mainWeakReference?.get()?.apply {
            menuMain?.let { setMenuVisible(it) }
            toolbar.title = bookHandler.book?.results?.comic?.name
        }
        setStartRead()
    }

    override fun onDestroy() {
        super.onDestroy()
        mainWeakReference?.get()?.menuMain?.let { setMenuInvisible(it) }
        bookHandler.destroy()
        bookHandler.ads.forEach {
            it.exit = true
        }
    }

    override fun onPause() {
        super.onPause()
        mainWeakReference?.get()?.menuMain?.let { setMenuInvisible(it) }
    }

    fun setStartRead() {
        mainWeakReference?.get()?.apply {
            bookHandler.book?.results?.comic?.name?.let {
                getPreferences(MODE_PRIVATE).getInt(it, -1).let { p ->
                    this@BookFragment.lbbstart.apply {
                        var i = 0
                        if(p >= 0) {
                            text = bookHandler.chapterNames[p]
                            i = p
                        }
                        setOnClickListener {
                            bookHandler.callViewManga(i)
                        }
                    }
                }
            }
        }
    }

    private fun setMenuInvisible(menu: Menu){
        menu.findItem(R.id.action_download)?.isVisible = false
    }

    private fun setMenuVisible(menu: Menu) {
        Log.d("MyBF", "显示下载按钮")
        val dl = menu.findItem(R.id.action_download)
        dl?.isVisible = true
        dl?.setIcon(R.drawable.ic_menu_download)
        dl?.setOnMenuItemClickListener {
            if(bookHandler.complete && it.itemId == R.id.action_download){
                navigate2dl()
                true
            }
            else it.itemId == R.id.action_download
        }
    }

    private fun navigate2dl(){
        val bundle = Bundle()
        bundle.putString("path", arguments?.getString("path")?:"null")
        bundle.putString("name", bookHandler.book?.results?.comic?.name)
        if(bookHandler.vols != null) {
            bundle.putBoolean("loadJson", true)
        }
        bundle.putStringArray("group", bookHandler.gpws)
        bundle.putStringArray("groupNames", bookHandler.keys)
        bundle.putIntArray("count", bookHandler.cnts)
        rootView?.let { Navigation.findNavController(it).navigate(R.id.action_nav_book_to_nav_group, bundle) }
    }
}