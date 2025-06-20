package top.fumiama.copymangaweb.web

import android.util.Log
import android.webkit.JavascriptInterface
import top.fumiama.copymangaweb.activity.DlActivity
import top.fumiama.copymangaweb.activity.MainActivity.Companion.mh
import top.fumiama.copymangaweb.handler.MainHandler

class JSHidden {
    @JavascriptInterface
    fun loadChapter(listString: String){
        mh?.obtainMessage(MainHandler.CALL_VIEW_MANGA, listString)?.sendToTarget()
    }
    @JavascriptInterface
    fun setTitle(title:String){
        Log.d("MyJSH", "Set title: $title")
        DlActivity.comicName = title
    }
    @JavascriptInterface
    fun setFab(content: String){
        mh?.obtainMessage(MainHandler.SET_FAB, content)?.sendToTarget()
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