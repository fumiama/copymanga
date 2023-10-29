package top.fumiama.copymanga.template.http

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import com.google.gson.Gson
import top.fumiama.dmzj.copymanga.R
import top.fumiama.copymanga.MainActivity.Companion.mainWeakReference
import top.fumiama.copymanga.json.ReturnBase
import top.fumiama.copymanga.tools.http.DownloadTools
import top.fumiama.copymanga.tools.thread.TimeThread
import java.io.File
import java.lang.Thread.sleep
import java.security.MessageDigest

open class AutoDownloadHandler(private val url: String, private val jsonClass: Class<*>, looper: Looper, private val callCheckMsg: Int = -1, private val loadFromCache: Boolean = false, private val customCacheFile: File? = null): Handler(looper) {
    var exit = false
    private var timeThread: TimeThread? = null
    private var checkTimes = 0
    private var cnt = 0
    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        when(msg.what){
            callCheckMsg -> check()
            0 -> setLayouts()
        }
    }
    open fun setGsonItem(gsonObj: Any): Boolean = true
    open fun getGsonItem(): ReturnBase? = null
    open fun onError() {}
    open fun doWhenFinishDownload() {}
    fun startLoad() {
        sendEmptyMessage(0)
    }
    fun destroy() {
        exit = true
    }
    private fun download() {
        Thread{ dlThread() }.start()
        checkTimes = 0
        timeThread = TimeThread(this, callCheckMsg)
        timeThread?.canDo = true
        timeThread?.start()
    }
    private fun toHexStr(byteArray: ByteArray) =
        with(StringBuilder()) {
            byteArray.forEach {
                val hex = it.toInt() and (0xFF)
                val hexStr = Integer.toHexString(hex)
                if (hexStr.length == 1) append("0").append(hexStr)
                else append(hexStr)
            }
            toString()
        }
    private fun dlThread() {
        val cacheName = toHexStr(MessageDigest.getInstance("MD5").digest(url.encodeToByteArray()))
        val cacheFile = customCacheFile?:(mainWeakReference?.get()?.externalCacheDir?.let { File(it, cacheName) })
        if(loadFromCache) {
            cacheFile?.let {
                if (it.exists()) {
                    var pass = true
                    val fi = it.inputStream()
                    try {
                        pass = setGsonItem(Gson().fromJson(fi.reader(), jsonClass))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    fi.close()
                    if (pass) return
                }
            }
        }
        DownloadTools.getHttpContent(url, null, mainWeakReference?.get()?.getString(R.string.pc_ua)!!).let {
            if(exit) return
            if(it == null) {
                if (cnt++>3) return
                sleep(1000)
                dlThread()
                return
            }
            val fi = it.inputStream()
            var pass = true
            try {
                pass = setGsonItem(Gson().fromJson(fi.reader(), jsonClass))
                if (pass && loadFromCache) {
                    cacheFile?.writeBytes(it)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            fi.close()
            if(!pass) {
                dlThread()
            }
        }
    }
    private fun check() {
        val g = getGsonItem()
        if(g != null) {
            timeThread?.canDo = false
            if(g.code == 200) sendEmptyMessage(0)
            else onError()
            Log.d("MyADH", "[${g.code}]${g.message}")
        } else if(checkTimes++ > 10) timeThread?.canDo = false
    }
    private fun setLayouts() {
        if(getGsonItem() == null) download()
        else doWhenFinishDownload()
    }
}
