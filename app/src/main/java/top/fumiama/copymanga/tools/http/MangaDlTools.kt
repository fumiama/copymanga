package top.fumiama.copymanga.tools.http

import android.util.Log
import com.google.gson.Gson
import top.fumiama.dmzj.copymanga.R
import top.fumiama.copymanga.MainActivity.Companion.mainWeakReference
import top.fumiama.copymanga.json.Chapter2Return
import top.fumiama.copymanga.template.http.AutoDownloadThread
import top.fumiama.copymanga.tools.http.DownloadTools.getHttpContent
import java.io.File
import java.lang.Thread.sleep
import java.util.zip.CRC32
import java.util.zip.CheckedOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class MangaDlTools {
    var exit = false
    private var comicFileRelative: String? = null
    var size = 0
    var complete = false

    fun downloadChapterInVol(url: CharSequence, chapterName: CharSequence, group: CharSequence, index: Int){
        comicFileRelative = "$group/$chapterName.zip"
        complete = false
        getChapterInfo(url, index)
        while (!complete) sleep(1000)
    }

    private fun getChapterInfo(chapter2Return: Chapter2Return, index: Int) {
        if(index >= 0){
            comicFileRelative?.let {
                dlChapterAndPackIntoZip(
                    File(
                        mainWeakReference?.get()?.getExternalFilesDir(""),
                        "${chapter2Return.results.comic.name}/$it"
                    ),
                    getMangaUrls(chapter2Return)
                )
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
        size = re.size
        return re
    }

    private fun getChapterInfo(url: CharSequence, index: Int){
        Log.d("MyMDT", "下载：$url, index：$index")
        AutoDownloadThread(url.toString()){
            Gson().fromJson(it?.decodeToString(), Chapter2Return::class.java)?.let {
                getChapterInfo(it, index)
            }
        }.start()
    }

    private fun dlChapterAndPackIntoZip(zipf: File, urls: Array<String>) {
        zipf.parentFile?.let { if (!it.exists()) it.mkdirs() }
        if (zipf.exists()) zipf.delete()
        zipf.createNewFile()
        val zip = ZipOutputStream(CheckedOutputStream(zipf.outputStream(), CRC32()))
        zip.setLevel(9)
        var succeed = true
        for (i in urls.indices) {
            zip.putNextEntry(ZipEntry("$i.webp"))
            var tryTimes = 3
            var s = false
            while (!s && tryTimes-- > 0) {
                s = getHttpContent(
                    urls[i],
                    mainWeakReference?.get()?.getString(R.string.referUrl),
                    mainWeakReference?.get()?.getString(R.string.pc_ua)
                )?.let { zip.write(it); true } ?: false
                if (!s) {
                    onDownloadedListener?.handleMessage(i + 1)
                    sleep(2000)
                }
            }
            if (!s && tryTimes <= 0) succeed = false
            onDownloadedListener?.handleMessage(s, i + 1)
            zip.flush()
            if (exit) break
        }
        zip.close()
        onDownloadedListener?.handleMessage(succeed)
        complete = true
    }

    var onDownloadedListener: OnDownloadedListener? = null

    interface OnDownloadedListener {
        fun handleMessage(succeed: Boolean)
        fun handleMessage(succeed: Boolean, pageNow: Int)
        fun handleMessage(pageNow: Int)
    }
}
