package top.fumiama.copymanga.tools.http

import android.os.Build
import android.util.Log
import com.google.gson.Gson
import com.sun.jna.Library
import com.sun.jna.Native
import top.fumiama.copymanga.MainActivity
import top.fumiama.copymanga.api.Config
import top.fumiama.copymanga.json.ComandyVersion
import top.fumiama.dmzj.copymanga.R
import java.io.ByteArrayInputStream
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.GZIPInputStream

interface Comandy : Library {
    // fun add_dns(para: String?, is_ipv6: Int): String?

    fun request(para: String): String?

    companion object {
        private var isInInit = AtomicBoolean(false)
        var instance: Comandy? = null
            get() {
                //Log.d("MyComandy", "get instance @$field")
                if (field != null) return field
                field = libraryFile?.absolutePath?.let { Native.load(it, Comandy::class.java) }?:return null
                //Log.d("MyComandy", "init instance @$field")
                return field
            }
        private var mUseComandy: Boolean? = null
        val useComandy: Boolean
            get() {
                if (isInInit.get()) {
                    Log.d("MyComandy", "block useComandy for isInInit")
                    return false
                }
                if (mUseComandy != true && DownloadTools.failTimes.get() >= 2) {
                    mUseComandy = true
                    return true
                }
                if (mUseComandy != null) return mUseComandy!!
                val v = Config.net_use_comandy.value
                mUseComandy = v
                return v
            }
        private val libraryFile: File?
            get() {
                if (isInInit.get()) return null
                isInInit.set(true)
                Log.d("MyComandy", "start to download/check lib")
                val prefix = when (Build.SUPPORTED_ABIS[0]) {
                    "arm64-v8a" -> "aarch64"
                    "armeabi-v7a" -> "armv7a"
                    "x86_64" -> "x86_64"
                    "x86" -> "i686"
                    else -> null
                }?:return null
                Log.d("MyComandy", "arch: $prefix")
                return MainActivity.mainWeakReference?.get()?.let { ma ->
                    var f = File(ma.filesDir, "libs")
                    if (!f.exists()) f.mkdirs()
                    f = File(f, "libcomandy.so")
                    var remoteVersion = 0
                    if (f.exists()) {
                        DownloadTools.getHttpContent(ma.getString(R.string.comandy_version_url), -1)?.let dataLet@{
                            try {
                                val body = Gson().fromJson(it.decodeToString(), ComandyVersion::class.java)?.body
                                if (body?.startsWith("Version: ") == true) {
                                    remoteVersion = body.substring(9).toInt()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            val myVersion = Config.comandy_version.value?:0
                            if (myVersion >= remoteVersion) {
                                Log.d("MyComandy", "lib version $myVersion is latest")
                                isInInit.set(false)
                                return@let f
                            }
                            Log.d("MyComandy", "lib version $myVersion <= latest $remoteVersion, update...")
                        }
                    }
                    DownloadTools.getHttpContent(ma.getString(R.string.comandy_download_url).format(prefix), -1)?.let {
                        if(f.exists()) f.delete()
                        try {
                            GZIPInputStream(ByteArrayInputStream(it)).use { dataIn ->
                                f.outputStream().use { dataOut ->
                                    dataIn.copyTo(dataOut)
                                }
                            }
                            if (remoteVersion > 0) Config.comandy_version.value = remoteVersion
                            Log.d("MyComandy", "update success")
                            isInInit.set(false)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            if(f.exists()) f.delete()
                        }
                    }
                    return@let if(f.exists()) f else null
                }
            }
    }
}
