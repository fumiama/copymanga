package top.fumiama.copymanga.template.http

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.fumiama.copymanga.MainActivity.Companion.mainWeakReference
import top.fumiama.copymanga.json.ReturnBase
import top.fumiama.copymanga.tools.http.DownloadTools
import top.fumiama.copymanga.tools.thread.TimeThread
import top.fumiama.dmzj.copymanga.R
import java.io.File
import java.lang.Thread.sleep
import java.security.MessageDigest

open class AutoDownloadHandler(
    private val url: String, private val jsonClass: Class<*>,
    private val context: LifecycleOwner?,
    private val callCheckMsg: Int = -1,
    private val loadFromCache: Boolean = false,
    private val customCacheFile: File? = null): Handler(Looper.myLooper()!!) {
    private var timeThread: TimeThread? = null
    private var checkTimes = 0
    var exit = false
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
    open suspend fun doWhenFinishDownload() {}
    fun startLoad() {
        sendEmptyMessage(0)
    }
    fun destroy() {
        exit = true
    }
    private suspend fun download() = withContext(Dispatchers.IO) {
        checkTimes = 0
        TimeThread(this@AutoDownloadHandler, callCheckMsg, 100).let {
            timeThread = it
            it.canDo = true
            it.start()
        }
        downloadCoroutine()
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
    private suspend fun downloadCoroutine() = withContext(Dispatchers.IO) {
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
                    if (pass) return@withContext
                }
            }
        }
        var cnt = 0
        while (cnt++ <= 3) {
            try {
                val data = DownloadTools.getHttpContent(url, null, mainWeakReference?.get()?.getString(R.string.pc_ua)!!)
                if(exit) return@withContext
                val fi = data.inputStream()
                val pass = setGsonItem(Gson().fromJson(fi.reader(), jsonClass))
                if (pass && loadFromCache) {
                    cacheFile?.writeBytes(data)
                }
                fi.close()
                if(!pass) {
                    sleep(2000)
                    continue
                }
                break
            } catch (e: Exception) {
                e.printStackTrace()
                sleep(2000)
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
        } else if(checkTimes++ > 1000) timeThread?.canDo = false
    }
    private fun setLayouts() = context?.lifecycleScope?.launch {
        if(getGsonItem() == null) download()
        else doWhenFinishDownload()
    }
}
