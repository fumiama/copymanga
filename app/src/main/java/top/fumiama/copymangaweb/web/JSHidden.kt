package top.fumiama.copymangaweb.web

import android.util.Log
import android.webkit.JavascriptInterface
import top.fumiama.copymangaweb.activity.DlActivity
import top.fumiama.copymangaweb.activity.MainActivity.Companion.mh
import top.fumiama.copymangaweb.activity.MainActivity.Companion.wm
import top.fumiama.copymangaweb.handler.MainHandler

class JSHidden {
    @JavascriptInterface
    fun loadChapter(listString: String){
        wm?.get()?.callViewManga(listString)
    }
    @JavascriptInterface
    fun setTitle(title:String){
        Log.d("MyJSH", "Set title: $title")
        DlActivity.comicName = title
    }
    @JavascriptInterface
    fun setFab(content: String){
        wm?.get()?.setFab(content)
    }
    @JavascriptInterface
    fun setLoadingDialog(display: Boolean) {
        mh?.sendEmptyMessage(if (display) MainHandler.SHOW_LOADING_DIALOG else MainHandler.HIDE_LOADING_DIALOG)
    }
    @JavascriptInterface
    fun setLoadingDialogProgress(index: String, count: String) {
        mh?.obtainMessage(MainHandler.SET_LOADING_DIALOG_TEXT, "$index/$count")?.sendToTarget()
    }
}