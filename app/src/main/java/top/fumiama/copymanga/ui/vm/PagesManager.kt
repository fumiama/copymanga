package top.fumiama.copymanga.ui.vm

import android.widget.Toast
import top.fumiama.copymanga.manga.Reader
import top.fumiama.dmzj.copymanga.R
import java.lang.ref.WeakReference

class PagesManager(private val w: WeakReference<ViewMangaActivity>) {
    val v get() = w.get()
    private var isEndL = false
    private var isEndR = false
    private val canGoPrevious get() = (v?.pageNum ?: 0) > 1
    private val canGoNext get() = (v?.pageNum ?: 0) < (v?.realCount ?: 0)
    @ExperimentalStdlibApi
    fun toPreviousPage(){
        toPage(v?.r2l==true)
    }
    @ExperimentalStdlibApi
    fun toNextPage(){
        toPage(v?.r2l!=true)
    }
    @ExperimentalStdlibApi
    fun toPage(goNext:Boolean) {
        v?.let { v ->
            if (v.clicked == 1) {
                v.hideDrawer()
                return
            }
            if (if(goNext) canGoNext else canGoPrevious) {
                if(goNext) {
                    v.scrollForward()
                    isEndR = false
                } else {
                    v.scrollBack()
                    isEndL = false
                }
                return
            }
            val chapterPosition = v.position + if(goNext) 1 else -1
            if (v.urlArray.isEmpty()) return
            if(chapterPosition < 0 || chapterPosition >= v.urlArray.size) {
                Toast.makeText(v.applicationContext, R.string.end_of_chapter, Toast.LENGTH_SHORT).show()
                return
            }
            if (if(goNext) isEndR else isEndL) {
                //if(v.zipFirst) intent.putExtra("callFrom", "zipFirst")
                v.tt.canDo = false
                //ViewMangaActivity.dlhandler = null
                v.comicName?.let { Reader.start2viewManga(it, chapterPosition, v.urlArray, v.uuidArray, goNext) }
                v.finish()
                return
            }
            val hint = if(goNext) R.string.press_again_to_load_next_chapter else R.string.press_again_to_load_previous_chapter
            Toast.makeText(v.applicationContext, hint, Toast.LENGTH_SHORT).show()
            if(goNext) isEndR = true
            else isEndL = true
        }
    }
    fun toggleDrawer() {
        v?.apply {
            when(clicked) {
                0 -> showDrawer()
                1 -> hideDrawer()
            }
        }
    }
}
