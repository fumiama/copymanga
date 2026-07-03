package top.fumiama.copymangaweb.web

import android.util.Log
import android.webkit.JavascriptInterface
import top.fumiama.copymangaweb.activity.DlActivity
import top.fumiama.copymangaweb.activity.MainActivity.Companion.mh
import top.fumiama.copymangaweb.activity.MainActivity.Companion.wm
import top.fumiama.copymangaweb.handler.MainHandler

class JSHidden(
    private val onLoadChapter: ((String) -> Unit)? = null,
    private val onChapterMeta: ((String) -> Unit)? = null,
    private val onAppendImages: ((Array<String>) -> Unit)? = null,
    private val onFinishStreaming: (() -> Unit)? = null,
    private val onChapterCount: ((Int) -> Unit)? = null,
    private val enableLoadingDialog: Boolean = true,
) {
    @JavascriptInterface
    fun loadChapter(listString: String){
        Log.d("CopymangaDL", "JS loadChapter called, length=${listString.length}, lines=${listString.count { it == '\n' } + 1}")
        val callback = onLoadChapter
        if (callback != null) callback(listString)
        else {
            val main = wm?.get()
            if (main == null) Log.e("CopymangaDL", "MainActivity reference lost, dropped chapter result")
            else main.callViewManga(listString)
        }
    }

    @JavascriptInterface
    fun setChapterMeta(headerString: String) {
        Log.d("CopymangaDL", "JS setChapterMeta called, length=${headerString.length}")
        onChapterMeta?.invoke(headerString)
    }

    @JavascriptInterface
    fun appendChapterImages(urlsString: String) {
        val urls = urlsString
            .split('\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toTypedArray()
        Log.d("CopymangaDL", "JS appendChapterImages called, images=${urls.size}")
        if (urls.isNotEmpty()) onAppendImages?.invoke(urls)
    }

    @JavascriptInterface
    fun setChapterCount(countString: String) {
        val c = countString.toIntOrNull() ?: return
        if (c > 0) {
            Log.d("CopymangaDL", "JS setChapterCount called, count=$c")
            onChapterCount?.invoke(c)
        }
    }

    @JavascriptInterface
    fun finishStreamingChapter() {
        Log.d("CopymangaDL", "JS finishStreamingChapter called")
        onFinishStreaming?.invoke()
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
        if (enableLoadingDialog) mh?.sendEmptyMessage(if (display) MainHandler.SHOW_LOADING_DIALOG else MainHandler.HIDE_LOADING_DIALOG)
    }
    @JavascriptInterface
    fun setLoadingDialogProgress(index: String, count: String) {
        if (enableLoadingDialog) mh?.obtainMessage(MainHandler.SET_LOADING_DIALOG_TEXT, "$index/$count")?.sendToTarget()
    }
}