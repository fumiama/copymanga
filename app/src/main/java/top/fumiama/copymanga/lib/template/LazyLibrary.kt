package top.fumiama.copymanga.lib.template

import android.os.Build
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.sun.jna.Library
import com.sun.jna.Native
import kotlinx.android.synthetic.main.dialog_progress.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.fumiama.copymanga.MainActivity
import top.fumiama.copymanga.json.ComandyVersion
import top.fumiama.copymanga.storage.PreferenceBoolean
import top.fumiama.copymanga.storage.UserPreferenceInt
import top.fumiama.copymanga.net.DownloadTools
import top.fumiama.copymanga.net.Client
import top.fumiama.dmzj.copymanga.R
import java.io.ByteArrayInputStream
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.GZIPInputStream

open class LazyLibrary<T: Library>(
    private val clazz: Class<T>,
    val name: String,
    private val functionName: String,
    val isInUse: PreferenceBoolean,
    private val version: UserPreferenceInt
) {
    private val repoName = name.substring(3).substringBeforeLast(".")
    var isInInit = AtomicBoolean(false)
    private var mInstance: T? = null
    suspend fun getInstance(): T? {
        //Log.d("MyLazyLibrary", "get instance @$field")
        if (mInstance != null) return mInstance
        mInstance = libraryFile()?.absolutePath?.let { Native.load(it, clazz) }?:return null
        //Log.d("MyLazyLibrary", "init instance @$field")
        return mInstance
    }
    private var mLibraryFile: File? = null
    private suspend fun libraryFile(): File?  {
        if (isInInit.get()) return null
        mLibraryFile?.let { return it }
        isInInit.set(true)
        Log.d("MyLazyLibrary", "start to download/check $name")
        val prefix = when (Build.SUPPORTED_ABIS[0]) {
            "arm64-v8a" -> "aarch64"
            "armeabi-v7a" -> "armv7a"
            "x86_64" -> "x86_64"
            "x86" -> "i686"
            else -> null
        }?:return null
        Log.d("MyLazyLibrary", "$name arch: $prefix")
        MainActivity.mainWeakReference?.get()?.let { ma ->
            ma.lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    Log.d("MyLazyLibrary", "$name launched")
                    var f = File(ma.filesDir, "libs")
                    if (!f.exists()) f.mkdirs()
                    f = File(f, name)
                    var remoteVersion = 0
                    if (f.exists()) {
                        DownloadTools.getHttpContent(ma.getString(R.string.comandy_version_url).format(repoName), -1)?.let dataLet@{
                            try {
                                val body = Gson().fromJson(it.decodeToString(), ComandyVersion::class.java)?.body
                                if (body?.startsWith("Version: ") == true) {
                                    remoteVersion = body.substring(9).toInt()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            val myVersion = version.value?:0
                            if (myVersion >= remoteVersion) {
                                Log.d("MyLazyLibrary", "$name version $myVersion is latest")
                                isInInit.set(false)
                                mLibraryFile = f
                                return@withContext
                            }
                            Log.d("MyLazyLibrary", "$name version $myVersion <= latest $remoteVersion, update...")
                        }
                    }
                    withContext(Dispatchers.Main) {
                        val progressBar = ma.layoutInflater.inflate(R.layout.dialog_progress, null, false)
                        val progressHandler = object : Client.Progress{
                            override fun notify(progressPercentage: Int) {
                                Log.d("MyLazyLibrary", "Set dl $name progress: $progressPercentage")
                                progressBar.dpp.progress = progressPercentage
                            }
                        }
                        val info = ma.toolsBox.buildAlertWithView("加载${functionName}组件", progressBar, "隐藏")
                        withContext(Dispatchers.IO) {
                            DownloadTools.getHttpContent(ma.getString(R.string.comandy_download_url).format(repoName, prefix, name), -1, progressHandler)?.let {
                                if(f.exists()) f.delete()
                                try {
                                    GZIPInputStream(ByteArrayInputStream(it)).use { dataIn ->
                                        f.outputStream().use { dataOut ->
                                            dataIn.copyTo(dataOut)
                                        }
                                    }
                                    if (remoteVersion > 0) version.value = remoteVersion
                                    Log.d("MyLazyLibrary", "update success")
                                    isInInit.set(false)
                                    info.dismiss()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    if(f.exists()) f.delete()
                                }
                            }
                        }
                    }
                    mLibraryFile = if(f.exists()) f else null
                    return@withContext
                }
            }.join()
        }
        return mLibraryFile
    }
}
