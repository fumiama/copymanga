package top.fumiama.copymanga.tool

import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import top.fumiama.copymanga.activity.MainActivity.Companion.wm
import top.fumiama.copymanga.activity.ViewMangaActivity
import java.lang.ref.WeakReference

class PagesManager(w: WeakReference<ViewMangaActivity>) {
    val v = w.get()
    private var isEndL = false
    private var isEndR = false
    @ExperimentalStdlibApi
    fun toPreviousPage(){
        toPage(v?.r2l==true)
    }
    @ExperimentalStdlibApi
    fun toNextPage(){
        toPage(v?.r2l!=true)
    }
    private fun judgePrevious() = v?.pageNum?:0 > 1
    private fun judgeNext() = v?.pageNum?:0 < v?.count?:0
    @ExperimentalStdlibApi
    private fun toPage(goNext:Boolean){
        val chapterUrl = if(goNext) ViewMangaActivity.nextChapterUrl else ViewMangaActivity.previousChapterUrl
        val hint = if(goNext) "下" else "上"
        if (v?.clicked == false) {
            if (if(goNext)judgeNext() else judgePrevious()) {
                if(goNext) {
                    v.scrollForward()
                    isEndR = false
                } else {
                    v.scrollBack()
                    isEndL = false
                }
            } else if (chapterUrl != null) {
                if (if(goNext)isEndR else isEndL) {
                    wm?.get()?.w?.loadUrl("javascript:invoke.clickClass(\"comicControlBottomTopClick\",${if(goNext)1 else 0});")
                    v.tt.canDo = false
                    v.finish()
                } else {
                    Toast.makeText(
                        v.applicationContext,
                        "再次按下加载${hint}一章",
                        Toast.LENGTH_SHORT
                    ).show()
                    if(goNext) isEndR = true
                    else isEndL = true
                }
            } else Toast.makeText(
                v.applicationContext,
                "已经到头了~",
                Toast.LENGTH_SHORT
            ).show()
        } else v?.hideObjs()
    }
    fun manageInfo(){
        if (v?.clicked == false) v.showObjs() else v?.hideObjs()
    }
}