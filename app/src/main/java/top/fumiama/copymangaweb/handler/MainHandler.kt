package top.fumiama.copymangaweb.handler

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_unzipping.*
import top.fumiama.copymangaweb.activity.DlActivity.Companion.json
import top.fumiama.copymangaweb.activity.MainActivity.Companion.wm
import top.fumiama.copymangaweb.activity.ViewMangaActivity
import top.fumiama.copymangaweb.tool.MangaDlTools.Companion.wmdlt

class MainHandler(looper: Looper):Handler(looper) {
    var saveUrlsOnly = false
    var showDlList = false

    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        when(msg.what) {
            LOAD_URL_IN_HIDDEN_WEB_VIEW -> loadUrlInHiddenWebView(msg.obj as String)
            CALL_VIEW_MANGA -> callViewManga(msg.obj as String)
            UPDATE_LOAD_PROGRESS -> updateLoadProgress(msg.arg1)
            SET_FAB -> setFab(msg.obj as String)
            HIDE_FAB -> hideFab()
            SET_FAB_TO_DOWNLOAD_LIST -> setFab2DlList()
            SHOW_LOADING_DIALOG -> wm?.get()?.dialog?.show()
            HIDE_LOADING_DIALOG -> wm?.get()?.dialog?.hide()
            SET_LOADING_DIALOG_TEXT -> wm?.get()?.dialog?.tunz?.text = msg.obj as String
        }
    }
    private fun loadUrlInHiddenWebView(url: String) { wm?.get()?.wh?.apply { post { loadUrl(url) } } }
    private fun callViewManga(content: String) = Thread{
        val listChapter = content.split('\n')
        if(!saveUrlsOnly) {
            ViewMangaActivity.titleText = listChapter[0].substringBeforeLast(' ')
            ViewMangaActivity.nextChapterUrl = listChapter[1].let { if(it == "null") null else it }
            ViewMangaActivity.previousChapterUrl = listChapter[2].let { if(it == "null") null else it }
            ViewMangaActivity.imgUrls = arrayOf()
            for(i in 3 until listChapter.size) ViewMangaActivity.imgUrls += listChapter[i]
            wm?.get()?.apply { runOnUiThread { startActivity(Intent(this, ViewMangaActivity::class.java)) } }
        } else {
            var imgs = arrayOf<String>()
            for(i in 3 until listChapter.size) imgs += listChapter[i]
            wmdlt?.get()?.setChapterImgs(listChapter[0].substringAfterLast(' '), imgs)
        }
    }.start()
    private fun updateLoadProgress(p: Int) {
        wm?.get()?.pw?.apply { post {
            if(progress == 100 && p < 100) {
                progress = 0
                visibility = View.VISIBLE
            }
            ObjectAnimator.ofInt(this, "progress", progress, p).setDuration(233).start()
            if(p == 100) postDelayed({ visibility = View.GONE }, 500)
        } }
    }
    private fun showFab() { wm?.get()?.fab?.apply { post { visibility = View.VISIBLE } } }
    private fun hideFab() { wm?.get()?.fab?.apply { post { visibility = View.GONE } } }
    private fun setFab(content: String) {
        //Log.d("MyMH", "Get chapter json: $content")
        showDlList = false
        json = content
        showFab()
    }
    private fun setFab2DlList(){
        showDlList = true
        showFab()
    }

    companion object {
        const val LOAD_URL_IN_HIDDEN_WEB_VIEW = 1
        const val CALL_VIEW_MANGA = 2
        const val UPDATE_LOAD_PROGRESS = 3
        const val SET_FAB = 4
        const val HIDE_FAB = 5
        const val SET_FAB_TO_DOWNLOAD_LIST = 6
        const val SHOW_LOADING_DIALOG = 7
        const val HIDE_LOADING_DIALOG = 8
        const val SET_LOADING_DIALOG_TEXT = 9
    }
}