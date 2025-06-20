package top.fumiama.copymangaweb.web

import android.util.Log
import android.webkit.JavascriptInterface
import top.fumiama.copymangaweb.R
import top.fumiama.copymangaweb.activity.MainActivity.Companion.mh
import top.fumiama.copymangaweb.activity.MainActivity.Companion.wm
import top.fumiama.copymangaweb.handler.MainHandler

class JS {
    @JavascriptInterface
    fun loadComic(url: String) {
        val u = when {
            url.contains("/details/comic/") -> "${wm?.get()?.getString(R.string.web_comic_detail_pc)}${url.substringAfter("comic")}"
            url.contains("/comicContent/") -> "${wm?.get()?.getString(R.string.web_comic_detail_pc)}/${url.substringAfter("comicContent/").substringBefore("/")}/chapter/${url.substringAfterLast("/")}"
            else -> ""
        }
        Log.d("MyJS", "Load comic: $u")
        mh?.obtainMessage(MainHandler.LOAD_URL_IN_HIDDEN_WEB_VIEW, u)?.sendToTarget()
    }
    @JavascriptInterface
    fun hideFab() {
        mh?.sendEmptyMessage(MainHandler.HIDE_FAB)
    }
    @JavascriptInterface
    fun enterProfile(){
        mh?.sendEmptyMessage(MainHandler.SET_FAB_TO_DOWNLOAD_LIST)
    }
}