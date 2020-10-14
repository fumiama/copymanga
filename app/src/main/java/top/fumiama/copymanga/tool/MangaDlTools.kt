package top.fumiama.copymanga.tool

import top.fumiama.copymanga.R
import top.fumiama.copymanga.activity.MainActivity.Companion.wm
import top.fumiama.copymanga.data.ComicStructure
import top.fumiama.copymanga.view.JSWebView
import top.fumiama.copymanga.web.JSHidden
import java.io.File
import java.lang.ref.WeakReference
import java.util.zip.CRC32
import java.util.zip.CheckedOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class MangaDlTools {
    var exit = false
    private val p = PropertiesTools(File("${wm?.get()?.filesDir}/chapters.hash"))
    private var imgUrlsList: Array<Array<String>?>? = null
    private var chaptersCount = 0
    private val newWebViewHidden: JSWebView?
        get() {
            val re = wm?.get()?.let { JSWebView(it, it.getString(R.string.pc_ua)) }
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
                val s = dl.getHttpContent(it[i])?.let { zip.write(it); true } ?: false
                if (!s) succeed = s
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
    }

    companion object {
        var wmdlt: WeakReference<MangaDlTools>? = null
        var comicStructure: Array<ComicStructure>? = null
    }
}