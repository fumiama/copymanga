package top.fumiama.copymanga.tools.http

import android.util.Log
import top.fumiama.copymanga.tools.ssl.AllTrustManager
import top.fumiama.copymanga.tools.ssl.IgnoreHostNameVerifier
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
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

    private fun getConnection(url: String?, method: String = "GET") =
        url?.let {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = method
            connection.connectTimeout = 20000
            connection.readTimeout = 20000
            connection
        }

    fun getHttpContent(Url: String, refer: String? = null, ua: String? = null): ByteArray? {
        Log.d("Mydl", "getHttp: $Url")
        var ret: ByteArray? = null
        val task = FutureTask(Callable {
            try {
                getConnection(Url)?.apply {
                    refer?.let { setRequestProperty("referer", it) }
                    setRequestProperty("source", "copyApp")
                    setRequestProperty("webp", "1")
                    setRequestProperty("region", "0")
                    setRequestProperty("authorization", "Token")
                    setRequestProperty("platform", "3")
                    ua?.let { setRequestProperty("User-agent", it) }

                    ret = inputStream.readBytes()
                    disconnect()
                }
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

    fun getHttpContent(Url: String, refer: String? = null): ByteArray? {
        Log.d("Mydl", "getHttp: $Url")
        var ret: ByteArray? = null
        val task = FutureTask(Callable {
            try {
                val connection = getConnection(Url)
                refer?.let { connection?.setRequestProperty("referer", it) }

                ret = connection?.inputStream?.readBytes()
                connection?.disconnect()
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

    fun getHttpContent(Url: String, readSize: Int, refer: String? = "https://api.copymanga.com"): ByteArray? {
        Log.d("Mydl", "getHttp: $Url")
        var ret: ByteArray? = null
        val task = FutureTask(Callable {
            try {
                val connection = getConnection(Url)
                refer?.let { connection?.setRequestProperty("referer", it) }

                val ci = connection?.inputStream
                if(readSize > 0) {
                    ret = ByteArray(readSize)
                    ci?.read(ret, 0, readSize)
                } else ret = ci?.readBytes()
                ci?.close()
                connection?.disconnect()
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

    fun touch(url: String?, refer: String? = "https://api.copymanga.com"): FutureTask<ByteArray?>? =
        url?.let {
            Log.d("Mydl", "touchHttp: $it")
            var ret: ByteArray? = null
            val task = FutureTask(Callable {
                try {
                    val connection = getConnection(it)
                    refer?.let { connection?.setRequestProperty("referer", it) }

                    val ci = connection?.inputStream
                    ret = ci?.readBytes()
                    ci?.close()
                    connection?.disconnect()
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
                return@Callable ret
            })
            Thread(task).start()
            task
        }

    private fun replaceChineseCharacters(string: String?) : String? {
        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.M) return string
        else return string?.replace(Regex("(?<=/)[\\w\\s\\d\\u4e00-\\u9fa5.-]+(?=/?)")) { match ->
            return@replace URLEncoder.encode(match.value, "UTF-8")
        }
    }

    fun downloadUsingUrlRet(url: String?, f: File, refer: String?): Boolean {
        Log.d("Mydl", "Ret Get url: $url, File: $f")
        val task = FutureTask(Callable {
            val connection = getConnection(replaceChineseCharacters(url))
            if(refer != null) connection?.setRequestProperty("referer", refer)

            if (f.exists()) f.delete()
            else f.parentFile?.mkdirs()
            f.parentFile?.let {
                if (!it.canRead()) it.setReadable(true)
                if (!it.canWrite()) it.setWritable(true)
            }
            val ci = connection?.inputStream
            val fo = f.outputStream()
            ci?.buffered()?.copyTo(fo)
            fo.close()
            ci?.close()
            connection?.disconnect()
            return@Callable true
        })
        Thread(task).start()
        return try {
            task.get()
        } catch (ex: Exception) {
            ex.printStackTrace()
            false
        }
    }
}