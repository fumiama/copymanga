package top.fumiama.copymanga.tools.http

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import top.fumiama.copymanga.MainActivity
import top.fumiama.copymanga.tools.ssl.AllTrustManager
import top.fumiama.copymanga.tools.ssl.IgnoreHostNameVerifier
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

    fun getConnection(url: String?, method: String = "GET", refer: String? = null, ua: String? = null) =
        url?.let {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = method
            connection.connectTimeout = 20000
            connection.readTimeout = 20000
            connection.apply {
                ua?.let { setRequestProperty("user-agent", it) }
                refer?.let { setRequestProperty("referer", it) }
                setRequestProperty("source", "copyApp")
                setRequestProperty("webp", "1")
                MainActivity.mainWeakReference?.get()?.let {
                    PreferenceManager.getDefaultSharedPreferences(it).apply {
                        setRequestProperty("region", if(!getBoolean("settings_cat_net_sw_use_foreign", false)) "1" else "0")
                    }
                    it.getPreferences(Context.MODE_PRIVATE).apply {
                        setRequestProperty("version", getString("app_ver", "1.4.4"))
                        getString("token", "")?.let {
                            if(it != "") setRequestProperty("authorization", "Token $it")
                            else setRequestProperty("authorization", "Token")
                        }
                    }
                }
                setRequestProperty("host", url.substringAfter("://").substringBefore("/"))
                setRequestProperty("platform", "3")
            }
        }

    fun getHttpContent(Url: String, refer: String? = null, ua: String? = null): ByteArray? {
        Log.d("Mydl", "getHttp: $Url")
        var ret: ByteArray? = null
        val task = FutureTask(Callable {
            try {
                getConnection(Url, "GET", refer, ua)?.apply {
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

    fun getHttpContent(Url: String, readSize: Int, refer: String? = "https://api.copymanga.com"): ByteArray? {
        Log.d("Mydl", "getHttp: $Url")
        var ret: ByteArray? = null
        val task = FutureTask(Callable {
            try {
                val connection = getConnection(Url, "GET", refer)?.apply {
                    ret = inputStream.readBytes()
                    disconnect()
                }

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
                    val connection = getConnection(it, "GET", refer)?.apply {
                        ret = inputStream.readBytes()
                        disconnect()
                    }

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
}