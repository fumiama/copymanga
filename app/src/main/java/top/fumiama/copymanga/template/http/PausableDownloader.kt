package top.fumiama.copymanga.template.http

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.fumiama.copymanga.MainActivity.Companion.mainWeakReference
import top.fumiama.copymanga.tools.http.DownloadTools
import top.fumiama.dmzj.copymanga.R
import java.lang.Thread.sleep
import kotlin.random.Random

class PausableDownloader(private val url: String, private val waitMilliseconds: Long = 0, private val whenFinish: suspend (result: ByteArray)->Unit) {
    var exit = false
    suspend fun run() = withContext(Dispatchers.IO) {
        var c = 0
        while (!exit && c++ < 3) {
            try {
                whenFinish(DownloadTools.getHttpContent(url,
                    mainWeakReference?.get()?.getString(R.string.referer)!!,
                    mainWeakReference?.get()?.getString(R.string.pc_ua)!!
                ))
                break
            } catch (e: Exception) {
                e.printStackTrace()
                if (waitMilliseconds > 0) sleep(200+Random.nextLong(waitMilliseconds))
            }
        }
        Log.d("MyPD", "found exit = $exit")
    }
}
