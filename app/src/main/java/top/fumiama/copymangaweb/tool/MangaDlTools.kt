package top.fumiama.copymangaweb.tool

import android.util.Log
import top.fumiama.copymangaweb.R
import top.fumiama.copymangaweb.activity.DlActivity
import java.io.File
import java.lang.Thread.sleep
import java.lang.ref.WeakReference
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.zip.CRC32
import java.util.zip.CheckedOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class MangaDlTools(activity: DlActivity) {
    var exit = false
    private val sem = Semaphore(1)
    private val da = WeakReference(activity)
    private val d get() = da.get()
    private val p = PropertiesTools(File("${d?.filesDir}/chapters.hash"))
    private var imgUrlsList: Array<Array<String>?>? = null
    private var chaptersCount = 0

    init {
        wmdlt = WeakReference(this)
    }

    fun getImgsCountByHash(hash: String): Int?{
        return p[hash].toIntOrNull()?.let { imgUrlsList?.getOrNull(it)?.size }
    }

    fun allocateChapterUrls(count: Int){
        imgUrlsList = arrayOfNulls(count)
        chaptersCount = 0
    }

    fun dlChapterUrl(url: String): Boolean {
        Log.d("CopymangaDL", "waiting chapter semaphore url=$url")
        if (!sem.tryAcquire(3, TimeUnit.MINUTES)) {
            Log.e("CopymangaDL", "timeout waiting previous chapter url collection")
            return false
        }
        var started = false
        da.get()?.apply {
            p[url.substringAfterLast("/")] = (chaptersCount++).toString()
            Log.d("CopymangaDL", "load chapter url=$url hash=${url.substringAfterLast("/")}")
            runOnUiThread { mBinding.dwh.apply { post { loadUrl(url) } } }
            started = true
        }
        if (!started) sem.release()
        return started
    }


    fun waitChapterUrlsReady(): Boolean {
        Log.d("CopymangaDL", "waiting final chapter url collection")
        return if (sem.tryAcquire(5, TimeUnit.MINUTES)) {
            sem.release()
            Log.d("CopymangaDL", "all chapter urls collected")
            true
        } else {
            Log.e("CopymangaDL", "timeout waiting final chapter url collection")
            false
        }
    }

    fun setChapterImages(hash: String, imgUrls: Array<String>){
        val index = p[hash].toIntOrNull()
        if (index == null) {
            Log.e("CopymangaDL", "missing chapter hash=$hash, images=${imgUrls.size}")
            sem.release()
            return
        }
        Log.d("CopymangaDL", "chapter images collected hash=$hash index=$index images=${imgUrls.size}")
        imgUrlsList?.set(index, imgUrls)
        sem.release()
    }

    fun dlChapterAndPackIntoZip(zipf: File, hash: String){
        val idx = p[hash].toIntOrNull()
        val images = idx?.let { imgUrlsList?.getOrNull(it) }
        if (images.isNullOrEmpty()) {
            Log.e("CopymangaDL", "no images to zip hash=$hash idx=$idx")
            onDownloadedListener?.handleMessage(false)
            return
        }

        val dl = DownloadTools()
        zipf.parentFile?.let { if (!it.exists()) it.mkdirs() }
        val tmpZip = File(zipf.absolutePath + ".tmp")
        if (tmpZip.exists()) tmpZip.delete()

        Log.d("CopymangaDL", "start zip file=${zipf.absolutePath} pages=${images.size}")
        var succeed = true
        try {
            ZipOutputStream(CheckedOutputStream(tmpZip.outputStream().buffered(), CRC32())).use { zip ->
                zip.setLevel(9)
                for (i in images.indices) {
                    if (i % 10 == 0) Log.d("CopymangaDL", "zipping page ${i + 1}/${images.size}")
                    var tryTimes = 3
                    var data: ByteArray? = null
                    while (data == null && tryTimes-- > 0) {
                        data = d?.toolsBox?.resolution?.wrap(images[i])?.let { u ->
                            dl.getHttpContent(
                                u,
                                d?.getString(R.string.web_home_www),
                                d?.getString(R.string.pc_ua)
                            )
                        }
                        if (data == null) {
                            Log.w("CopymangaDL", "retry page=${i + 1} left=$tryTimes url=${images[i]}")
                            onDownloadedListener?.handleMessage(i + 1)
                            sleep(2000)
                        }
                    }

                    if (data == null) {
                        succeed = false
                        Log.e("CopymangaDL", "download failed page=${i + 1} url=${images[i]}")
                        onDownloadedListener?.handleMessage(false, i + 1)
                    } else {
                        zip.putNextEntry(ZipEntry("%03d.webp".format(i)))
                        zip.write(data)
                        zip.closeEntry()
                        onDownloadedListener?.handleMessage(true, i + 1)
                    }
                    zip.flush()
                    if (exit) {
                        succeed = false
                        Log.w("CopymangaDL", "zip canceled")
                        break
                    }
                }
            }

            if (succeed) {
                if (zipf.exists()) zipf.delete()
                if (!tmpZip.renameTo(zipf)) {
                    tmpZip.copyTo(zipf, overwrite = true)
                    tmpZip.delete()
                }
            } else tmpZip.delete()
            Log.d("CopymangaDL", "zip finished succeed=$succeed file=${zipf.absolutePath}")
            onDownloadedListener?.handleMessage(succeed)
        } catch (e: Exception) {
            Log.e("CopymangaDL", "zip exception", e)
            tmpZip.delete()
            onDownloadedListener?.handleMessage(false)
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
    }
}