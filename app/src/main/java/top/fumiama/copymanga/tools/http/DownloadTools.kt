package top.fumiama.copymanga.tools.http

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.fumiama.copymanga.MainActivity
import top.fumiama.copymanga.json.ComandyCapsule
import top.fumiama.dmzj.copymanga.R
import java.lang.IllegalArgumentException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Callable
import java.util.concurrent.FutureTask
import java.util.concurrent.atomic.AtomicInteger

object DownloadTools {
    val app_ver = MainActivity.mainWeakReference?.get()?.let { main ->
        PreferenceManager.getDefaultSharedPreferences(main)
            ?.getString("settings_cat_general_et_app_version", main.getString(R.string.app_ver))
            ?:main.getString(R.string.app_ver)
    }!!
    val pc_ua = MainActivity.mainWeakReference?.get()!!.getString(R.string.pc_ua).format(app_ver)
    val referer = MainActivity.mainWeakReference?.get()!!.getString(R.string.referer).format(app_ver)
    val failTimes = AtomicInteger(0)
    fun getApiConnection(url: String, method: String = "GET", refer: String? = null, ua: String? = null, timeout: Int = 20000) =
        url.let {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = method
            connection.connectTimeout = timeout
            connection.readTimeout = timeout
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
                        setRequestProperty("version", app_ver)
                        getString("token", "")?.let { tk ->
                            setRequestProperty("authorization", "Token $tk")
                        }
                    }
                }
                setRequestProperty("platform", "3")
            }
            Log.d("MyDT", "getConnection: $url\n${connection.requestProperties.map { "${it.key}: ${it.value}" }.joinToString("\n")}")
            connection
        }

    fun getComandyApiConnection(url: String, method: String = "GET", refer: String? = null, ua: String? = null) =
        run {
            val capsule = ComandyCapsule()
            capsule.url = url
            capsule.method = method
            capsule.headers = hashMapOf()
            capsule.headers["host"] = url.substringAfter("://").substringBefore("/")
            ua?.let { capsule.headers["user-agent"] = it }
            refer?.let { capsule.headers["referer"] = it }
            capsule.headers["source"] = "copyApp"
            capsule.headers["webp"] = "1"
            MainActivity.mainWeakReference?.get()?.let {
                PreferenceManager.getDefaultSharedPreferences(it).apply {
                    capsule.headers["region"] = if(!getBoolean("settings_cat_net_sw_use_foreign", false)) "1" else "0"
                }
                it.getPreferences(Context.MODE_PRIVATE).apply {
                    capsule.headers["version"] = app_ver
                    getString("token", "")?.let { tk ->
                        capsule.headers["authorization"] = "Token $tk"
                    }
                }
            }
            capsule.headers["platform"] = "3"
            Log.d("MyDT", "getComandyConnection: $url\n${capsule.headers.map { "${it.key}: ${it.value}" }.joinToString("\n")}")
            capsule
        }

    private fun getNormalConnection(url: String, method: String = "GET", ua: String? = null, timeout: Int = 20000) =
        url.let {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = method
            connection.connectTimeout = timeout
            connection.readTimeout = timeout
            connection.apply {
                setRequestProperty("host", url.substringAfter("://").substringBefore("/"))
                ua?.let { setRequestProperty("user-agent", it) }
            }
        }

    private fun getComandyNormalConnection(url: String, method: String = "GET", ua: String? = null) =
        run {
            val capsule = ComandyCapsule()
            capsule.url = url
            capsule.method = method
            capsule.headers = hashMapOf()
            capsule.headers["host"] = url.substringAfter("://").substringBefore("/")
            ua?.let { capsule.headers["user-agent"] = it }
            capsule
        }

    suspend fun getHttpContent(u: String, refer: String? = null, ua: String? = null): ByteArray =
        withContext(Dispatchers.IO) {
            if (Comandy.useComandy) {
                getComandyApiConnection(u, "GET", refer, ua).let { capsule ->
                    val para = Gson().toJson(capsule)
                    //Log.d("MyDT", "comandy request: $para")
                    Comandy.instance?.request(para)?.let { result ->
                        //Log.d("MyDT", "comandy reply: $result")
                        Gson().fromJson(result, ComandyCapsule::class.java)!!.let {
                            if (it.code != 200) throw IllegalArgumentException("HTTP${it.code} ${
                                it.data?.let { d -> Base64.decode(d, Base64.DEFAULT).decodeToString() }
                            }")
                            Base64.decode(it.data, Base64.DEFAULT)
                        }
                    }
                }.let { if(it?.isNotEmpty() == true ) return@withContext it }
            }
            failTimes.incrementAndGet()
            getApiConnection(u, "GET", refer, ua).let {
                val ret = it.inputStream.readBytes()
                it.disconnect()
                Log.d("MyDT", "getHttpContent: ${ret.size} bytes")
                failTimes.decrementAndGet()
                ret
            }
        }

    fun getHttpContent(u: String, readSize: Int): ByteArray? {
        Log.d("MyDT", "getHttp: $u")
        val task = prepare(u, readSize)
        Thread(task).start()
        return try {
            task.get()
        } catch (ex: Exception) {
            ex.printStackTrace()
            if (Comandy.useComandy) failTimes.incrementAndGet()
            null
        }
    }

    fun prepare(u: String, readSize: Int = -1) = run {
        Log.d("MyDT", "prepareHttp: $u")
        FutureTask(if (Comandy.useComandy) Callable{
            try {
                Comandy.instance?.request(Gson().toJson(
                    getComandyNormalConnection(u, "GET"))
                )?.let { result ->
                    Gson().fromJson(result, ComandyCapsule::class.java)?.let {
                        if (it.code != 200) null
                        else it.data?.let { d -> Base64.decode(d, Base64.DEFAULT) }
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                null
            }
        } else Callable {
            var ret: ByteArray? = null
            try {
                val connection = getNormalConnection(u, "GET")
                val ci = connection.inputStream
                if(readSize > 0) {
                    ret = ByteArray(readSize)
                    ci?.read(ret, 0, readSize)
                } else ret = ci?.readBytes()
                ci?.close()
                connection.disconnect()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            ret
        })
    }

    /*private fun replaceChineseCharacters(string: String?) : String? {
        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.M) return string
        else return string?.replace(Regex("(?<=/)[\\w\\s\\d\\u4e00-\\u9fa5.-]+(?=/?)")) { match ->
            return@replace URLEncoder.encode(match.value, "UTF-8")
        }
    }*/

    fun requestWithBody(url: String, method: String, body: ByteArray, refer: String? = null, ua: String? = null): ByteArray? {
        Log.d("MyDT", "$method Http: $url")
        var ret: ByteArray? = null
        val task = FutureTask(if(Comandy.useComandy) Callable{
            try {
                val capsule = getComandyApiConnection(url, method, refer, ua)
                capsule.data = body.decodeToString()
                Comandy.instance?.request(Gson().toJson(capsule))?.let { result ->
                    Gson().fromJson(result, ComandyCapsule::class.java)?.let {
                        if (it.code != 200) null
                        else it.data?.let { d -> Base64.decode(d, Base64.DEFAULT) }
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                null
            }
        }
        else Callable {
            failTimes.incrementAndGet()
            try {
                getApiConnection(url, method, refer, ua).apply {
                    outputStream.write(body)
                    ret = inputStream.readBytes()
                    disconnect()
                    failTimes.decrementAndGet()
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
}
