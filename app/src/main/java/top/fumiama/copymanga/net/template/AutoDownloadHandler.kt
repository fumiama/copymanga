package top.fumiama.copymanga.net.template

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.fumiama.copymanga.MainActivity.Companion.mainWeakReference
import top.fumiama.copymanga.json.ReturnBase
import top.fumiama.copymanga.api.Config
import top.fumiama.copymanga.net.DownloadTools
import java.io.File
import java.security.MessageDigest

open class AutoDownloadHandler(
    private val url: () -> String, private val jsonClass: Class<*>,
    private val context: LifecycleOwner?,
    private val loadFromCache: Boolean = false,
    private val customCacheFile: File? = null): Handler(Looper.myLooper()!!) {
    private var checkTimes = 0
    var exit = false
    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        when(msg.what){
            MSG_START_LOAD -> setLayouts()
        }
    }
    open fun setGsonItem(gsonObj: Any): Boolean = true
    open fun getGsonItem(): ReturnBase? = null
    open suspend fun onError() {}
    open suspend fun doWhenFinishDownload() {}
    fun startLoad() {
        sendEmptyMessage(MSG_START_LOAD)
    }
    fun destroy() {
        exit = true
    }
    private suspend fun download() = withContext(Dispatchers.IO) {
        checkTimes = 0
        downloadCoroutine()
        check()
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
        val cacheName = toHexStr(MessageDigest.getInstance("MD5").digest(url().encodeToByteArray()))
        val cacheFile = customCacheFile?:(mainWeakReference?.get()?.externalCacheDir?.let { File(it, cacheName) })
        if(loadFromCache) {
            cacheFile?.let {
                if (it.exists()) {
                    var pass = true
                    it.inputStream().use { fi->
                        try {
                            pass = setGsonItem(Gson().fromJson(fi.reader(), jsonClass))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    if (pass) return@withContext
                }
            }
        }
        var cnt = 0
        while (cnt++ <= 3) {
            try {
                val data = Config.apiProxy?.comancry(url()) {
                    DownloadTools.getHttpContent(it)
                }?:DownloadTools.getHttpContent(url())
                if(exit) return@withContext
                val fi = data.inputStream()
                val pass = setGsonItem(Gson().fromJson(fi.reader(), jsonClass))
                if (pass && loadFromCache) {
                    cacheFile?.writeBytes(data)
                }
                fi.close()
                if(!pass) {
                    delay(2000)
                    continue
                }
                break
            } catch (e: Exception) {
                e.printStackTrace()
                delay(2000)
            }
        }
    }
    private suspend fun check() {
        getGsonItem()?.let {
            Log.d("MyADH", "[${it.code}]${it.message}")
            if (it.code == 200) startLoad() else null
        }?:onError()
    }
    private fun setLayouts() = context?.lifecycleScope?.launch {
        if(getGsonItem() == null) download()
        else doWhenFinishDownload()
    }

    companion object {
        const val MSG_START_LOAD = 0
    }
}
