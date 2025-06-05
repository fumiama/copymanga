package top.fumiama.copymanga.net.template

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import top.fumiama.copymanga.api.Config
import kotlin.random.Random

class PausableDownloader(private val url: String, private val waitMilliseconds: Long = 0, private val isApi: Boolean = true, private val whenFinish: (suspend (result: ByteArray)->Unit)? = null) {
    var exit = false
    suspend fun run(): Boolean = withContext(Dispatchers.IO) {
        var c = 0
        while (!exit && c++ < 3) {
            try {
                val data = Config.myHostApiUrl.get(url)
                whenFinish?.let { it(data.encodeToByteArray()) }
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
