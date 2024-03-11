package top.fumiama.copymanga.template.http

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import top.fumiama.copymanga.MainActivity.Companion.mainWeakReference
import top.fumiama.copymanga.tools.api.CMApi
import top.fumiama.copymanga.tools.http.DownloadTools
import top.fumiama.dmzj.copymanga.R
import java.lang.Thread.sleep
import kotlin.random.Random

class PausableDownloader(private val url: String, private val waitMilliseconds: Long = 0, private val isApi: Boolean = true, private val whenFinish: (suspend (result: ByteArray)->Unit)? = null) {
    var exit = false
    suspend fun run() = withContext(Dispatchers.IO) {
        var c = 0
        while (!exit && c++ < 3) {
            try {
                val data = (DownloadTools.getHttpContent(
                    (if(isApi) CMApi.apiProxy?.wrap(url) else null)?:url,
                    mainWeakReference?.get()?.getString(R.string.referer)!!,
                    mainWeakReference?.get()?.getString(R.string.pc_ua)!!
                ))
                whenFinish?.let { it(data) }
                break
            } catch (e: Exception) {
                e.printStackTrace()
                if (waitMilliseconds > 0) delay(200+Random.nextLong(waitMilliseconds))
            }
        }
        Log.d("MyPD", "found exit = $exit")
    }
}
