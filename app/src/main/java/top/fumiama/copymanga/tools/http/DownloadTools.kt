package top.fumiama.copymanga.tools.http

import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.fumiama.copymanga.MainActivity
import top.fumiama.copymanga.api.Config
import top.fumiama.copymanga.json.ComandyCapsule
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Callable
import java.util.concurrent.FutureTask
import java.util.concurrent.atomic.AtomicInteger

object DownloadTools {
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
                setRequestProperty("region", if(!Config.net_use_foreign.value) "1" else "0")
                setRequestProperty("version", Config.app_ver.value)
                Config.token.value?.let { tk ->
                    setRequestProperty("authorization", "Token $tk")
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
                capsule.headers["region"] = if(!Config.net_use_foreign.value) "1" else "0"
                capsule.headers["version"] = Config.app_ver.value
                Config.token.value?.let { tk ->
                    capsule.headers["authorization"] = "Token $tk"
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

    suspend fun getHttpContent(u: String, refer: String? = null, ua: String? = Config.pc_ua): ByteArray =
        withContext(Dispatchers.IO) {
            if (!u.startsWith("https://copymanga.azurewebsites.net") && Comandy.useComandy) {
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
        FutureTask(if (!u.startsWith("https://copymanga.azurewebsites.net") && Comandy.useComandy) Callable{
            try {
                Comandy.instance?.request(Gson().toJson(
                    getComandyNormalConnection(u, "GET", Config.pc_ua))
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
                val connection = getNormalConnection(u, "GET", Config.pc_ua)
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

    fun requestWithBody(url: String, method: String, body: ByteArray, refer: String? = Config.referer, ua: String? = Config.pc_ua, contentType: String? = "application/x-www-form-urlencoded;charset=utf-8"): ByteArray? {
        Log.d("MyDT", "$method Http: $url")
        var ret: ByteArray? = null
        val task = FutureTask(if(!url.startsWith("https://copymanga.azurewebsites.net") && Comandy.useComandy) Callable{
            try {
                val capsule = getComandyApiConnection(url, method, refer, ua)
                contentType?.let { capsule.headers["content-type"] = it }
                capsule.data = body.decodeToString()
                Comandy.instance?.request(Gson().toJson(capsule))?.let { result ->
                    Gson().fromJson(result, ComandyCapsule::class.java)?.let {
                        it.data?.let { d -> Base64.decode(d, Base64.DEFAULT) }?:"empty comandy data".encodeToByteArray()
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                ex.message?.encodeToByteArray()
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
