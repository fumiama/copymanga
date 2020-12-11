package top.fumiama.copymanga.tool

import android.os.Looper
import android.widget.Toast
import top.fumiama.copymanga.R
import top.fumiama.copymanga.activity.DlActivity
import top.fumiama.copymanga.data.ComicStructure
import top.fumiama.copymanga.view.JSWebView
import top.fumiama.copymanga.web.JSHidden
import java.io.File
import java.lang.Thread.sleep
import java.lang.ref.WeakReference
import java.util.zip.CRC32
import java.util.zip.CheckedOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class MangaDlTools(activity: DlActivity) {
    var exit = false
    private val da = WeakReference(activity)
    private val d = da.get()
    private val p = PropertiesTools(File("${d?.filesDir}/chapters.hash"))
    private var imgUrlsList: Array<Array<String>?>? = null
    private var chaptersCount = 0
    private val newWebViewHidden: JSWebView?
        get() {
            val re = d?.let { JSWebView(it, it.getString(R.string.pc_ua)) }
            re?.setWebViewClient("h.js")
            re?.loadJSInterface(JSHidden())
            return re
        }

    init {
        wmdlt = WeakReference(this)
    }

    fun getImgsCountByHash(hash: String): Int?{
        return imgUrlsList?.get(p[hash].toInt())?.size
    }

    fun allocateChapterUrls(count: Int){
        imgUrlsList = arrayOfNulls(count)
        chaptersCount = 0
    }

    fun dlChapterUrl(url: String){
        p[url.substringAfterLast("/")] = (chaptersCount++).toString()
        newWebViewHidden?.loadUrl(url)
    }

    fun setChapterImgs(hash: String, imgUrls: Array<String>){
        imgUrlsList?.set(p[hash].toInt(), imgUrls)
    }

    fun dlChapterAndPackIntoZip(zipf: File, hash: String){
        imgUrlsList?.get(p[hash].toInt())?.let {
            val dl = DownloadTools()
            zipf.parentFile?.let { if (!it.exists()) it.mkdirs() }
            if (zipf.exists()) zipf.delete()
            zipf.createNewFile()
            val zip = ZipOutputStream(CheckedOutputStream(zipf.outputStream(), CRC32()))
            zip.setLevel(9)
            var succeed = true
            for (i in it.indices) {
                zip.putNextEntry(ZipEntry("$i.webp"))
                var tryTimes = 3
                var s = false
                while (!s && tryTimes-- > 0){
                    s = dl.getHttpContent(it[i], d?.getString(R.string.web_home_www), d?.getString(R.string.pc_ua))?.let { zip.write(it); true } ?: false
                    if (!s) {
                        onDownloadedListener?.handleMessage(i + 1)
                        sleep(2000)
                    }
                }
                if(tryTimes == 0) succeed = false
                onDownloadedListener?.handleMessage(s, i + 1)
                zip.flush()
                if (exit) break
            }
            zip.close()
            onDownloadedListener?.handleMessage(succeed)
        }
    }

    var onDownloadedListener: OnDownloadedListener? = null

    interface OnDownloadedListener {
        fun handleMessage(succeed: Boolean)
        fun handleMessage(succeed: Boolean, pageNow: Int)
        fun handleMessage(pageNow: Int)
    }

    companion object {
        var wmdlt: WeakReference<MangaDlTools>? = null
        var comicStructure: Array<ComicStructure>? = null
    }
}