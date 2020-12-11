package top.fumiama.copymanga.web

import android.util.Log
import android.webkit.JavascriptInterface
import top.fumiama.copymanga.R
import top.fumiama.copymanga.activity.MainActivity.Companion.mh
import top.fumiama.copymanga.activity.MainActivity.Companion.wm
import top.fumiama.copymanga.activity.ViewMangaActivity

class JS {
    @JavascriptInterface
    fun loadComic(url: String){
        val u = when {
            url.contains("/details/comic/") -> "${wm?.get()?.getString(R.string.web_comic_detail_pc)}${url.substringAfter("comic")}"
            url.contains("/comicContent/") -> "${wm?.get()?.getString(R.string.web_comic_detail_pc)}/${url.substringAfter("comicContent/").substringBefore("/")}/chapter/${url.substringAfterLast("/")}"
            else -> ""
        }
        Log.d("MyJS", "Load comic: $u")
        Thread{mh?.obtainMessage(1, u)?.sendToTarget()}.start()
    }
    @JavascriptInterface
    fun hideFab(){
        Thread{mh?.sendEmptyMessage(5)}.start()
    }
    @JavascriptInterface
    fun enterProfile(){
        Thread{mh?.sendEmptyMessage(6)}.start()
    }
}