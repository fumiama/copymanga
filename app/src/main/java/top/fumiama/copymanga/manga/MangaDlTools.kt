package top.fumiama.copymanga.manga

import android.util.Log
import com.google.gson.Gson
import top.fumiama.copymanga.MainActivity.Companion.mainWeakReference
import top.fumiama.copymanga.json.Chapter2Return
import top.fumiama.copymanga.template.http.AutoDownloadThread
import top.fumiama.copymanga.tools.http.DownloadPool
import java.io.File

class MangaDlTools {
    private var pool: DownloadPool? = null
    private var grp: CharSequence = ""
    private var indexMap = hashMapOf<String, Int>()
    var exit: Boolean
        get() = pool?.exit?:false
        set(value) { pool?.exit = value }
    var wait
        get() = pool?.wait
        set(value) { if (value != null) { pool?.wait = value } }

    fun downloadChapterInVol(url: CharSequence, chapterName: CharSequence, group: CharSequence, index: Int){
        Log.d("MyMDT", "下载：$url, index：$index")
        AutoDownloadThread(url.toString()){
            Gson().fromJson(it?.decodeToString(), Chapter2Return::class.java)?.let {
                if(it.results.chapter.words.size != it.results.chapter.size) downloadChapterInVol(url, chapterName, group, index)
                else getChapterInfo(it, index, chapterName, group)
            }
        }.start()
    }

    @Synchronized private fun setPool(comicName: String, group: CharSequence) {
        if(pool == null || grp != group) {
            pool = DownloadPool(File(
                mainWeakReference?.get()?.getExternalFilesDir(""),
                "$comicName/$group"
            ).absolutePath)
            grp = group
        }
    }

    @Synchronized private fun setIndexMap(f : String, index: Int) {
        indexMap[f] = index
    }

    private fun getChapterInfo(chapter2Return: Chapter2Return, index: Int, chapterName: CharSequence, group: CharSequence) {
        if(index >= 0){
            val f = "$chapterName.zip"
            setPool(chapter2Return.results.comic.name, group)
            setIndexMap(f, index)
            pool?.plusAssign(DownloadPool.Quest(f, getMangaUrls(chapter2Return)))
            pool?.setOnDownloadListener { fileName: String, isSuccess: Boolean ->
                indexMap[fileName]?.let { onDownloadedListener?.handleMessage(it, isSuccess) }
            }
            pool?.setOnPageDownloadListener { fileName: String, downloaded: Int, total: Int, isSuccess: Boolean ->
                indexMap[fileName]?.let { onDownloadedListener?.handleMessage(it, downloaded, total, isSuccess) }
            }
        }
    }

    private fun getMangaUrls(chapter2Return: Chapter2Return): Array<String>{
        var re: Array<String> = arrayOf()
        val hm: HashMap<Int, String> = hashMapOf()
        val chapter = chapter2Return.results.chapter
        for(i in 0 until chapter.size) {
            hm[chapter.words[i]] = chapter.contents[i].url
        }
        for(i in 0 until chapter.size){
            re += hm[i]?:""
        }
        return re
    }

    var onDownloadedListener: OnDownloadedListener? = null
    interface OnDownloadedListener{
        fun handleMessage(index: Int, isSuccess: Boolean)
        fun handleMessage(index: Int, downloaded: Int, total: Int, isSuccess: Boolean)
    }
}
