package top.fumiama.copymanga.template.http

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import top.fumiama.copymanga.api.Config
import top.fumiama.copymanga.tools.http.DownloadTools
import kotlin.random.Random

class PausableDownloader(private val url: String, private val waitMilliseconds: Long = 0, private val isApi: Boolean = true, private val whenFinish: (suspend (result: ByteArray)->Unit)? = null) {
    var exit = false
    suspend fun run(): Boolean = withContext(Dispatchers.IO) {
        var c = 0
        while (!exit && c++ < 3) {
            try {
                val data = (DownloadTools.getHttpContent(
                    (if(isApi) Config.apiProxy?.wrap(url) else null)?:url,
                    Config.referer
                ))
                whenFinish?.let { it(data) }
                return@withContext true
            } catch (e: Exception) {
                e.printStackTrace()
                if (waitMilliseconds > 0) delay(200+Random.nextLong(waitMilliseconds))
            }
        }
        Log.d("MyPD", "found exit = $exit")
        return@withContext false
    }
}
