package top.fumiama.copymanga.tools.http

import android.util.Log
import top.fumiama.copymanga.tools.ssl.AllTrustManager
import top.fumiama.copymanga.tools.ssl.IgnoreHostNameVerifier
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom
import java.util.concurrent.Callable
import java.util.concurrent.FutureTask
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext

object DownloadTools {
    private val trustManager = AllTrustManager()
    private val sslContext: SSLContext = SSLContext.getInstance("SSL").let {
        it.init(null, arrayOf(trustManager), SecureRandom())
        it
    }
    private val ignoreHostNameVerifier = IgnoreHostNameVerifier()

    init {
        HttpsURLConnection.setDefaultHostnameVerifier(ignoreHostNameVerifier)
        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)
    }

    fun getHttpContent(Url: String, refer: String? = null, ua: String? = null): ByteArray? {
        Log.d("Mydl", "getHttp: $Url")
        var ret: ByteArray? = null
        val task = FutureTask(Callable {
            try {
                val connection = URL(Url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 20000
                connection.readTimeout = 20000
                refer?.let { connection.setRequestProperty("referer", it) }
                connection.setRequestProperty("source", "copyApp")
                connection.setRequestProperty("webp", "1")
                connection.setRequestProperty("region", "1")
                connection.setRequestProperty("platform", "3")
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
    fun downloadUsingUrlRet(Url: String?, f: File): Boolean {
        Log.d("Mydl", "Ret Get Url: $Url, File: $f")
        val task = FutureTask(Callable {
            try {
                val connection = URL(Url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 20000
                connection.readTimeout = 20000
                connection.setRequestProperty("source", "copyApp")
                connection.setRequestProperty("webp", "1")
                connection.setRequestProperty("region", "1")
                connection.setRequestProperty("platform", "3")

                if (f.exists()) f.delete()
                else f.parentFile?.mkdirs()
                f.parentFile?.let {
                    if (!it.canRead()) it.setReadable(true)
                    if (!it.canWrite()) it.setWritable(true)
                }
                connection.inputStream.buffered().copyTo(f.outputStream())
                connection.disconnect()
                return@Callable true
            } catch (ex: Exception) {
                ex.printStackTrace()
                return@Callable false
            }
        })
        Thread(task).start()
        return try {
            task.get()
        } catch (ex: Exception) {
            ex.printStackTrace()
            false
        }
    }
    fun downloadUsingUrl(Url: String?, f: File, refer: String? = null) {
        Log.d("Mydl", "Get Url: $Url, File: $f")
        Thread(Runnable {
            try {
                val connection = URL(Url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 20000
                connection.readTimeout = 20000
                refer?.let { connection.setRequestProperty("referer", it) }

                if (f.exists()) f.delete()
                else f.parentFile?.mkdirs()
                f.parentFile?.let {
                    if (!it.canRead()) it.setReadable(true)
                    if (!it.canWrite()) it.setWritable(true)
                }
                connection.inputStream.buffered().copyTo(f.outputStream())
                connection.disconnect()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }).start()
    }

    fun getHttpContent(Url: String, refer: String? = null): ByteArray? {
        Log.d("Mydl", "getHttp: $Url")
        var ret: ByteArray? = null
        val task = FutureTask(Callable {
            try {
                val connection = URL(Url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 20000
                connection.readTimeout = 20000
                refer?.let { connection.setRequestProperty("referer", it) }

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