package top.fumiama.copymanga.web

import android.util.Log
import android.webkit.JavascriptInterface
import top.fumiama.copymanga.activity.DlActivity
import top.fumiama.copymanga.activity.MainActivity.Companion.mh

class JSHidden {
    @JavascriptInterface
    fun loadChapter(listString: String){
        Thread{mh?.obtainMessage(2, listString)?.sendToTarget()}.start()
    }
    @JavascriptInterface
    fun setTitle(title:String){
        Log.d("MyJSH", "Set title: $title")
        DlActivity.comicName = title
    }
    @JavascriptInterface
    fun setFab(content: String){
        Thread{mh?.obtainMessage(4, content)?.sendToTarget()}.start()
    }
}