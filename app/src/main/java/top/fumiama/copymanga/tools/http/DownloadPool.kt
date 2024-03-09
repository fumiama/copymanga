package top.fumiama.copymanga.tools.http

import android.util.Log
import top.fumiama.copymanga.tools.api.CMApi
import java.io.File
import java.io.FileOutputStream
import java.lang.Thread.sleep
import java.util.zip.CRC32
import java.util.zip.CheckedOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.random.Random

class DownloadPool(folder: String) {
    class Quest(val fileName: String, val imgUrl: Array<String>, val refer: String? = null)
    var exit = false
        set(value) {
            if(value) {
                mOnDownloadListener = null
                mOnPageDownloadListener = null
            }
            field = value
        }
    var wait = false
    private val saveFolder = File(folder)
    //fileName: String, isSuccess: Boolean
    private var mOnDownloadListener: ((String, Boolean, String) -> Unit)? = null
    //fileName: String, downloaded: Int, total: Int, isSuccess: Boolean
    private var mOnPageDownloadListener: ((String, Int, Int, Boolean, String) -> Unit)? = null
    init {
        if(!saveFolder.exists()) saveFolder.mkdirs()
    }

    operator fun plusAssign(quest: Quest) {
        packZipFile(quest.fileName, quest.imgUrl)
    }

    fun setOnDownloadListener(onDownloadListener: (String, Boolean, String) -> Unit) {
        mOnDownloadListener = onDownloadListener
    }

    fun setOnPageDownloadListener(onPageDownloadListener: (String, Int, Int, Boolean, String) -> Unit) {
        mOnPageDownloadListener = onPageDownloadListener
    }

    private fun packZipFile(fileName: String, imgUrls: Array<String>) {
        Thread {
            File(saveFolder, "$fileName.tmp").let { f ->
                f.parentFile?.let { if(!it.exists()) it.mkdirs() }
                var start = 0
                Log.d("MyDP", "Zip file: ${f.absolutePath}")
                if(f.exists()) {
                    try {
                        val zipFile = ZipFile(f)
                        start = zipFile.size()
                        zipFile.close()
                        Log.d("MyDP", "next download index: $start")
                        if (start <= 0 || start >= imgUrls.size) { // error or re-download
                            f.delete()
                            f.createNewFile()
                            start = 0
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        f.delete()
                        f.createNewFile()
                    }
                } else f.createNewFile()
                val zip: ZipOutputStream
                if (start > 0) {
                    val fromZip = ZipInputStream(f.readBytes().inputStream())
                    zip = ZipOutputStream(CheckedOutputStream(FileOutputStream(f), CRC32()))
                    zip.setLevel(9)
                    fromZip.use { z ->
                        var e = z.nextEntry
                        while (e != null) {
                            zip.putNextEntry(e)
                            z.copyTo(zip)
                            zip.closeEntry()
                            z.closeEntry()
                            e = z.nextEntry
                        }
                    }
                } else {
                    zip = ZipOutputStream(CheckedOutputStream(FileOutputStream(f), CRC32()))
                    zip.setLevel(9)
                }
                var succeed = true
                var lastIndex = -8
                try {
                    for(index in start until imgUrls.size) {
                        while (wait && !exit) sleep(100+Random.nextLong(1000))
                        if(exit) break
                        var tryTimes = 3
                        var s = false
                        while (!s && tryTimes-- > 0) {
                            val u = imgUrls[index]
                            s = (DownloadTools.getHttpContent(CMApi.resolution.wrap(CMApi.proxy?.wrap(u)?:u), -1))?.let {
                                zip.putNextEntry(ZipEntry("$index.${if(imgUrls[index].contains(".webp")) "webp" else "jpg"}"))
                                zip.write(it)
                                zip.closeEntry()
                                true
                            }?:false
                            if(exit) break
                            if (!s) sleep(2000)
                            if(exit) break
                        }
                        if(!s && tryTimes <= 0) {
                            succeed = false
                            mOnPageDownloadListener?.let { it(fileName, index + 1, imgUrls.size, false, "超过最大重试次数") }
                            break
                        } else mOnPageDownloadListener?.let { it(fileName, index + 1, imgUrls.size, true, "") }
                        lastIndex = index
                    }
                    zip.close()
                    if (succeed && lastIndex+1 >= imgUrls.size) f.renameTo(File(saveFolder, fileName))
                    mOnPageDownloadListener?.let { it(fileName, 0, 0, true, "") }
                    mOnDownloadListener?.let { it(fileName, succeed, "") }
                } catch (e: Exception) {
                    e.printStackTrace()
                    mOnDownloadListener?.let { it(fileName, false, e.localizedMessage?:"packZipFile") }
                }
            }
        }.start()
    }
}