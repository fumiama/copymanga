package top.fumiama.copymanga.tool

import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Callable
import java.util.concurrent.FutureTask

class DownloadTools {
    fun getHttpContent(Url: String, refer: String? = null, ua: String? = null): ByteArray? {
        Log.d("Mydl", "getHttp: $Url")
        var ret: ByteArray? = null
        val task = FutureTask(Callable {
            try {
                val connection = URL(Url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                refer?.let { connection.setRequestProperty("referer", it) }
                ua?.let { connection.setRequestProperty("User-agent", it) }

                ret = connection.inputStream.readBytes()
                connection.disconnect()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            return@Callable ret
        })
        Thread(task).start()
        return try {
            task.get()
        } catch (ex: Exception) {
            ex.printStackTrace()
            null
        }
    }
}