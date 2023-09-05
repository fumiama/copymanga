package top.fumiama.copymanga.tools.http

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import top.fumiama.copymanga.MainActivity
import top.fumiama.dmzj.copymanga.R
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Callable
import java.util.concurrent.FutureTask

object DownloadTools {
    fun getConnection(url: String?, method: String = "GET", refer: String? = null, ua: String? = null) =
        url?.let {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = method
            connection.connectTimeout = 20000
            connection.readTimeout = 20000
            connection.apply {
                setRequestProperty("host", url.substringAfter("://").substringBefore("/"))
                ua?.let { setRequestProperty("user-agent", it) }
                refer?.let { setRequestProperty("referer", it) }
                setRequestProperty("source", "copyApp")
                setRequestProperty("webp", "1")
                MainActivity.mainWeakReference?.get()?.let {
                    PreferenceManager.getDefaultSharedPreferences(it).apply {
                        setRequestProperty("region", if(!getBoolean("settings_cat_net_sw_use_foreign", false)) "1" else "0")
                    }
                    it.getPreferences(Context.MODE_PRIVATE).apply {
                        setRequestProperty("version", it.getString(R.string.app_ver))
                        getString("token", "")?.let { tk ->
                            setRequestProperty("authorization", "Token $tk")
                        }
                    }
                }
                setRequestProperty("platform", "3")
            }
            Log.d("Mydl", "getHttp: ${connection.requestProperties.map { "${it.key}: ${it.value}" }.joinToString("\n")}")
            connection
        }

    private fun getNormalConnection(url: String?, method: String = "GET", ua: String? = null) =
        url?.let {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = method
            connection.connectTimeout = 20000
            connection.readTimeout = 20000
            connection.apply {
                setRequestProperty("host", url.substringAfter("://").substringBefore("/"))
                ua?.let { setRequestProperty("user-agent", it) }
            }
        }

    fun getHttpContent(u: String, refer: String? = null, ua: String? = null): ByteArray? {
        Log.d("Mydl", "getHttp: $u")
        var ret: ByteArray? = null
        val task = FutureTask(Callable {
            try {
                getConnection(u, "GET", refer, ua)?.apply {
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

    fun getHttpContent(u: String, readSize: Int): ByteArray? {
        Log.d("Mydl", "getHttp: $u")
        var ret: ByteArray? = null
        val task = FutureTask(Callable {
            try {
                val connection = getNormalConnection(u, "GET")
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

    fun touch(url: String?): FutureTask<ByteArray?>? =
        url?.let {
            Log.d("Mydl", "touchHttp: $it")
            var ret: ByteArray? = null
            val task = FutureTask(Callable {
                try {
                    val connection = getNormalConnection(it, "GET")

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

    /*private fun replaceChineseCharacters(string: String?) : String? {
        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.M) return string
        else return string?.replace(Regex("(?<=/)[\\w\\s\\d\\u4e00-\\u9fa5.-]+(?=/?)")) { match ->
            return@replace URLEncoder.encode(match.value, "UTF-8")
        }
    }*/
}
