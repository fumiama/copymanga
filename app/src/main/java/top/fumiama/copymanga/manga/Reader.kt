package top.fumiama.copymanga.manga

import android.content.Context
import android.content.Intent
import androidx.core.content.edit
import top.fumiama.copymanga.MainActivity.Companion.mainWeakReference
import top.fumiama.copymanga.ui.vm.ViewMangaActivity

object Reader {
    fun viewMangaAt(name: String, pos: Int, from_first_page: Boolean = false) {
        mainWeakReference?.get()?.apply {
            getPreferences(Context.MODE_PRIVATE)?.edit {
                putInt(name, pos)
                apply()
            }
            ViewMangaActivity.dlhandler = null
            ViewMangaActivity.position = pos
            ViewMangaActivity.comicName = name
            val zipf = ViewMangaActivity.fileArray[pos]
            val intent = Intent(this, ViewMangaActivity::class.java)
            if(!from_first_page) {
                intent.putExtra("function", "log")
                ViewMangaActivity.pn = -2
            }
            if (zipf.exists()) {
                ViewMangaActivity.zipFile = zipf
                intent.putExtra("callFrom", "zipFirst")
                startActivity(intent)
            } else {
                ViewMangaActivity.zipFile = null
                startActivity(intent)
            }
        }
    }
}