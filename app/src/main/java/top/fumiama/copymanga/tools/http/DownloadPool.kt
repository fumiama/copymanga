package top.fumiama.copymanga.tools.http

import android.util.Log
import java.io.File
import java.lang.Thread.sleep
import java.util.zip.CRC32
import java.util.zip.CheckedOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

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
    private var mOnDownloadListener: ((String, Boolean) -> Unit)? = null
    //fileName: String, downloaded: Int, total: Int, isSuccess: Boolean
    private var mOnPageDownloadListener: ((String, Int, Int, Boolean) -> Unit)? = null
    init {
        if(!saveFolder.exists()) saveFolder.mkdirs()
    }

    operator fun plusAssign(quest: Quest) {
        packZipFile(quest.fileName, quest.imgUrl, quest.refer?:"")
    }

    operator fun plusAssign(quests: Array<Quest>) {
        Thread{
            quests.forEach { quest ->
                packZipFile(quest.fileName, quest.imgUrl, quest.refer?:"")
                sleep(1000)
            }
        }.start()
    }

    fun setOnDownloadListener(onDownloadListener: (String, Boolean) -> Unit) {
        mOnDownloadListener = onDownloadListener
    }

    fun setOnPageDownloadListener(onPageDownloadListener: (String, Int, Int, Boolean) -> Unit) {
        mOnPageDownloadListener = onPageDownloadListener
    }

    private fun packZipFile(fileName: String, imgUrls: Array<String>, refer: String) {
        Thread{
            File(saveFolder, fileName).let { f ->
                f.parentFile?.let { if(!it.exists()) it.mkdirs() }
                if(f.exists()) f.delete()
                f.createNewFile()
                Log.d("MyDP", "Zip file: ${f.absolutePath}")
                val zip = ZipOutputStream(CheckedOutputStream(f.outputStream(), CRC32()))
                zip.setLevel(9)
                var succeed = true
                for(index in imgUrls.indices) {
                    while (wait && !exit) sleep(1000)
                    if(exit) break
                    zip.putNextEntry(ZipEntry("$index.jpg"))
                    var tryTimes = 3
                    var s = false
                    while (!s && tryTimes-- > 0){
                        s = (DownloadTools.getHttpContent(imgUrls[index], -1, refer)) ?.let { zip.write(it); true }?:false
                        if (!s) sleep(2000)
                    }
                    if(!s && tryTimes <= 0) {
                        succeed = false
                        mOnPageDownloadListener?.let { it(fileName, index + 1, imgUrls.size, false) }
                        break
                    } else mOnPageDownloadListener?.let { it(fileName, index + 1, imgUrls.size, true) }
                    //zip.flush()
                }
                zip.close()
                mOnPageDownloadListener?.let { it(fileName, 0, 0, true) }
                mOnDownloadListener?.let { it(fileName, succeed) }
            }
        }.start()
    }
}