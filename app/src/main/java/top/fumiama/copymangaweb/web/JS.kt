package top.fumiama.copymangaweb.web

import android.util.Log
import android.webkit.JavascriptInterface
import top.fumiama.copymangaweb.R
import top.fumiama.copymangaweb.activity.MainActivity.Companion.wm

class JS {
    @JavascriptInterface
    fun loadComic(url: String) {
        val u = when {
            url.contains("/details/comic/") -> "${wm?.get()?.getString(R.string.web_comic_detail_pc)}${url.substringAfter("comic")}"
            url.contains("/comicContent/") -> "${wm?.get()?.getString(R.string.web_comic_detail_pc)}/${url.substringAfter("comicContent/").substringBefore("/")}/chapter/${url.substringAfterLast("/")}"
            else -> ""
        }
        Log.d("MyJS", "Load comic: $u")
        wm?.get()?.loadHiddenUrl(u)
    }
    @JavascriptInterface
    fun hideFab() {
        wm?.get()?.hideFab()
    }
    @JavascriptInterface
    fun enterProfile(){
        wm?.get()?.setFab2DlList()
    }
}