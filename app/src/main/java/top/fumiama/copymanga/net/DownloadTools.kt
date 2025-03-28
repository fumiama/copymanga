package top.fumiama.copymanga.net

import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import top.fumiama.copymanga.MainActivity
import top.fumiama.copymanga.api.Config
import top.fumiama.copymanga.json.ComandyCapsule
import top.fumiama.copymanga.lib.Comandy
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.lang.Thread.sleep
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

    private fun InputStream.copyToWithProgress(out: OutputStream, sz: Int, p: Client.Progress, bufferSize: Int = DEFAULT_BUFFER_SIZE): Long {
        var bytesCopied: Long = 0
        val buffer = ByteArray(bufferSize)
        var bytes = read(buffer)
        var prevP = 0
        p.notify(0)
        while (bytes >= 0) {
            val progress = (100*bytesCopied/sz).toInt()
            if (prevP != progress) {
                p.notify(progress)
                prevP = progress
            }
            out.write(buffer, 0, bytes)
            bytesCopied += bytes
            bytes = read(buffer)
        }
        p.notify(100)
        return bytesCopied
    }

    private fun InputStream.readBytesWithProgress(sz: Int, p: Client.Progress): ByteArray {
        val buffer = ByteArrayOutputStream(maxOf(DEFAULT_BUFFER_SIZE, this.available()))
        copyToWithProgress(buffer, sz, p)
        return buffer.toByteArray()
    }

    suspend fun getHttpContent(u: String, refer: String? = null, ua: String? = Config.pc_ua, p: Client.Progress? = null): ByteArray =
        withContext(Dispatchers.IO) {
            if (!u.startsWith("https://copymanga.azurewebsites.net") && Comandy.instance.enabled) {
                getComandyApiConnection(u, "GET", refer, ua).let { capsule ->
                    val para = Gson().toJson(capsule)
                    //Log.d("MyDT", "comandy request: $para")
                    Comandy.instance.getInstance()?.let { ins ->
                        var completed = false
                        p?.let {
                            Thread {
                                Log.d("MyDT", "launch comandy get progress, completed: $completed for url $u")
                                var prev = 0
                                while (!completed) {
                                    sleep(50)
                                    val progress = ins.progress(para)
                                    Log.d("MyDT", "comandy get progress $progress for url $u")
                                    if (progress > prev) {
                                        it.notify(progress)
                                        prev = progress
                                        if (progress >= 100) break
                                    }
                                }
                                Log.d("MyDT", "quit comandy get progress, completed: $completed for url $u")
                            }.start()
                        }
                        val r = ins.request(para)?.let { result ->
                            completed = true
                            p?.notify(100)
                            //Log.d("MyDT", "comandy reply: $result")
                            Gson().fromJson(result, ComandyCapsule::class.java)!!.let {
                                if (it.code != 200) throw IllegalArgumentException("HTTP${it.code} ${
                                    it.data?.let { d -> Base64.decode(d, Base64.DEFAULT).decodeToString() }
                                }")
                                Base64.decode(it.data, Base64.DEFAULT)
                            }
                        }
                        completed = true
                        p?.notify(100)
                        r
                    }
                }.let { if(it?.isNotEmpty() == true ) return@withContext it }
                failTimes.incrementAndGet()
            }
            getApiConnection(u, "GET", refer, ua).let {
                val sz = it.getHeaderFieldInt("Content-Length", 0)
                val ret = if (sz > 0 && p != null) {
                    it.inputStream.readBytesWithProgress(sz, p)
                } else {
                    it.inputStream.readBytes()
                }
                it.disconnect()
                Log.d("MyDT", "getHttpContent: ${ret.size} bytes")
                ret
            }
        }

    fun getHttpContent(u: String, readSize: Int, p: Client.Progress? = null): ByteArray? {
        Log.d("MyDT", "getHttp: $u")
        val task = prepare(u, readSize, p)
        Thread(task).start()
        return try {
            task.get()
        } catch (ex: Exception) {
            ex.printStackTrace()
            null
        }
    }

    // prepare p only take effect when readSize is -1
    fun prepare(u: String, readSize: Int = -1, p: Client.Progress? = null) = run {
        Log.d("MyDT", "prepareHttp: $u")
        FutureTask(if (!u.startsWith("https://copymanga.azurewebsites.net") && Comandy.instance.enabled) Callable{
            try {
                runBlocking { Comandy.instance.getInstance() }?.let { ins ->
                    runBlocking {
                        val para = Gson().toJson(getComandyNormalConnection(u, "GET", Config.pc_ua))
                        var completed = false
                        p?.let {
                            Thread {
                                Log.d("MyDT", "launch comandy get progress, completed: $completed for url $u")
                                var prev = 0
                                while (!completed) {
                                    sleep(50)
                                    val progress = ins.progress(para)
                                    Log.d("MyDT", "comandy get progress $progress for url $u")
                                    if (progress > prev) {
                                        it.notify(progress)
                                        prev = progress
                                        if (progress >= 100) break
                                    }
                                }
                                Log.d("MyDT", "quit comandy get progress, completed: $completed for url $u")
                            }.start()
                        }
                        val r = ins.request(para)?.let { result ->
                            completed = true
                            p?.notify(100)
                            Gson().fromJson(result, ComandyCapsule::class.java)?.let {
                                if (it.code != 200) null
                                else it.data?.let { d -> Base64.decode(d, Base64.DEFAULT) }
                            }
                        }
                        completed = true
                        p?.notify(100)
                        r
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
                val sz = connection.getHeaderFieldInt("Content-Length", 0)
                if(readSize > 0) {
                    ret = ByteArray(readSize)
                    ci?.read(ret, 0, readSize)
                } else ret = if (sz > 0 && p != null) {
                    ci.readBytesWithProgress(sz, p)
                } else {
                    ci.readBytes()
                }
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
        val task = FutureTask(if(!url.startsWith("https://copymanga.azurewebsites.net") && Comandy.instance.enabled) Callable{
            try {
                val capsule = getComandyApiConnection(url, method, refer, ua)
                contentType?.let { capsule.headers["content-type"] = it }
                capsule.data = body.decodeToString()
                runBlocking { Comandy.instance.getInstance() }?.request(Gson().toJson(capsule))?.let { result ->
                    Gson().fromJson(result, ComandyCapsule::class.java)?.let {
                        it.data?.let { d -> Base64.decode(d, Base64.DEFAULT) }?:"empty comandy data".encodeToByteArray()
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                failTimes.incrementAndGet()
                ex.message?.encodeToByteArray()
            }
        }
        else Callable {
            try {
                getApiConnection(url, method, refer, ua).apply {
                    outputStream.write(body)
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
}
