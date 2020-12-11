package top.fumiama.copymanga.handler

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.View
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import top.fumiama.copymanga.activity.DlActivity
import top.fumiama.copymanga.activity.MainActivity.Companion.wm
import top.fumiama.copymanga.activity.ViewMangaActivity
import top.fumiama.copymanga.data.ComicStructure
import top.fumiama.copymanga.tool.MangaDlTools
import top.fumiama.copymanga.tool.MangaDlTools.Companion.comicStructure
import top.fumiama.copymanga.tool.MangaDlTools.Companion.wmdlt

class MainHandler(looper: Looper):Handler(looper) {
    var saveUrlsOnly = false
    var showDlList = false
    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        when(msg.what){
            1 -> loadUrlInHiddenWebView(msg.obj as String)
            2 -> callViewManga(msg.obj as String)
            3 -> updateLoadProgress(msg.arg1)
            4 -> setFab(msg.obj as String)
            5 -> hideFab()
            6 -> setFab2DlList()
        }
    }
    private fun loadUrlInHiddenWebView(url: String){wm?.get()?.wh?.loadUrl(url)}
    private fun callViewManga(content: String){
        val listChapter = content.split("\n")
        if(!saveUrlsOnly) {
            ViewMangaActivity.titleText = listChapter[0].substringBeforeLast(" ")
            ViewMangaActivity.nextChapterUrl = listChapter[1].let { if(it == "null") null else it }
            ViewMangaActivity.previousChapterUrl = listChapter[2].let { if(it == "null") null else it }
            ViewMangaActivity.imgUrls = arrayOf()
            for(i in 3 until listChapter.size) ViewMangaActivity.imgUrls += listChapter[i]
            wm?.get()?.let { it.startActivity(Intent(it, ViewMangaActivity::class.java)) }
        } else{
            var imgs = arrayOf<String>()
            for(i in 3 until listChapter.size) imgs += listChapter[i]
            wmdlt?.get()?.setChapterImgs(listChapter[0].substringAfterLast(" "), imgs)
        }
    }
    private fun updateLoadProgress(progress: Int){
        wm?.get()?.let{
            if(it.pw.progress == 100 && progress < 100) {
                it.pw.progress = 0
                it.pw.visibility = View.VISIBLE
            }
            ObjectAnimator.ofInt(it.pw, "progress", it.pw.progress, progress).setDuration(233).start()
            if(progress == 100) it.pw.postDelayed({it.pw.visibility = View.GONE}, 500)
        }
    }
    private fun showFab() {wm?.get()?.fab?.visibility = View.VISIBLE}
    private fun hideFab() {wm?.get()?.fab?.visibility = View.GONE}
    private fun setFab(content: String){
        //Log.d("MyMH", "Get chapter json: $content")
        showDlList = false
        comicStructure = Gson().fromJson(content.reader(), Array<ComicStructure>::class.java)
        showFab()
    }
    private fun setFab2DlList(){
        showDlList = true
        showFab()
    }
}