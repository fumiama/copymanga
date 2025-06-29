package top.fumiama.copymanga.net

import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import top.fumiama.sdict.io.Client
import top.fumiama.copymanga.api.Config
import top.fumiama.copymanga.api.Config.proxyUrl
import top.fumiama.copymanga.json.ComandyCapsule
import top.fumiama.copymanga.lib.Comandy
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.lang.Thread.sleep
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.FutureTask
import java.util.zip.GZIPInputStream

object DownloadTools {
    private fun getApiConnection(url: String, method: String = "GET", timeout: Int = 20000): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = timeout
        connection.readTimeout = timeout
        connection.apply {
            Config.net_ua.value.let {
                if (it.isEmpty()) return@let
                setRequestProperty("user-agent", if (it == Config.default_ua) Config.pc_ua else it)
            }
            Config.net_source.value.let { if(it.isNotEmpty()) setRequestProperty("source", it) }
            // deviceinfo
            if (!Config.net_no_webp.value) setRequestProperty("webp", "1")
            setRequestProperty("dt", SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Calendar.getInstance().time))
            if (Config.net_use_gzip.value) setRequestProperty("accept-encoding", "gzip")
            setRequestProperty("authorization", "Token${Config.token.value?.let { tk ->
                if (tk.isNotEmpty()) " $tk" else ""
            }?:""}")
            if (Config.net_platform.value) setRequestProperty("platform", Config.platform.value)
            if (Config.net_referer.value) setRequestProperty("referer", Config.referer)
            if (Config.net_use_json.value) setRequestProperty("accept", "application/json")
            if (Config.net_version.value) setRequestProperty("version", Config.app_ver.value)
            if (Config.net_region.value) setRequestProperty("region", if(!Config.net_use_foreign.value) "1" else "0")
            // device
            // host
            Config.net_umstring.value.let { if (it.isNotEmpty()) setRequestProperty("umstring", it) }
            setRequestProperty("connection", "close")
        }
        Log.d("MyDT", "getConnection: $url\n${connection.requestProperties.map { "${it.key}: ${it.value}" }.joinToString("\n")}")
        return connection
    }

    private fun getComandyApiConnection(url: String, method: String = "GET") =
        run {
            val capsule = ComandyCapsule()
            capsule.url = url
            capsule.method = method
            capsule.headers = hashMapOf()
            Config.net_ua.value.let {
                if (it.isEmpty()) return@let
                capsule.headers["user-agent"] = if (it == Config.default_ua) Config.pc_ua else it
            }
            Config.net_source.value.let { if(it.isNotEmpty()) capsule.headers["source"] = it }
            // deviceinfo
            if (!Config.net_no_webp.value) capsule.headers["webp"] = "1"
            capsule.headers["dt"] = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Calendar.getInstance().time)
            if (Config.net_use_gzip.value) capsule.headers["accept-encoding"] = "gzip"
            capsule.headers["authorization"] = "Token${Config.token.value?.let { tk ->
                if (tk.isNotEmpty()) " $tk" else ""
            }?:""}"
            if (Config.net_platform.value) capsule.headers["platform"] = Config.platform.value
            if (Config.net_referer.value) capsule.headers["referer"] = Config.referer
            if (Config.net_use_json.value) capsule.headers["accept"] = "application/json"
            if (Config.net_version.value) capsule.headers["version"] = Config.app_ver.value
            if (Config.net_region.value) capsule.headers["region"] = if(!Config.net_use_foreign.value) "1" else "0"
            // device
            // host
            Config.net_umstring.value.let { if (it.isNotEmpty()) capsule.headers["umstring"] = it }
            capsule.headers["connection"] = "close"

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

    private fun decodeBody(ret: ByteArray, coding: String) : ByteArray {
        return if (coding == "gzip") ByteArrayInputStream(ret).use { byteIn ->
            GZIPInputStream(byteIn).use useGzip@ { gzipIn ->
                ByteArrayOutputStream().use { byteOut ->
                    val buffer = ByteArray(4096)
                    var len: Int
                    while (gzipIn.read(buffer).also { len = it } != -1) {
                        byteOut.write(buffer, 0, len)
                    }
                    return@useGzip byteOut.toByteArray()
                }
            }
        } else ret
    }

    private fun parseErrorResponse(body: String): String {
        return try {
            val json = JSONObject(body)
            if (json.has("detail")) {
                json.getString("detail")
            } else {
                body // 原始 JSON 返回
            }
        } catch (e: Exception) {
            "非JSON返回: $body"
        }
    }

    private fun InputStream.readBytesWithProgress(sz: Int, p: Client.Progress): ByteArray {
        val buffer = ByteArrayOutputStream(maxOf(DEFAULT_BUFFER_SIZE, this.available()))
        copyToWithProgress(buffer, sz, p)
        return buffer.toByteArray()
    }

    suspend fun getApiContent(u: String, p: Client.Progress? = null): ByteArray =
        withContext(Dispatchers.IO) {
            if (!u.startsWith("https://$proxyUrl") && Comandy.instance.enabled) {
                getComandyApiConnection(u, "GET").let { capsule ->
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
                        var coding: String? = null
                        val r = ins.request(para)?.let { result ->
                            completed = true
                            p?.notify(100)
                            //Log.d("MyDT", "comandy reply: $result")
                            Gson().fromJson(result, ComandyCapsule::class.java)!!.let {
                                if (it.code != 200) throw IllegalArgumentException("HTTP${it.code} ${
                                    it.data?.let { d -> parseErrorResponse(Base64.decode(d, Base64.DEFAULT).decodeToString()) }
                                }")
                                coding = it.headers["Content-Encoding"] as String?
                                Base64.decode(it.data, Base64.DEFAULT)
                            }
                        }
                        completed = true
                        p?.notify(100)
                        r?.let { ret -> coding?.let { decodeBody(ret, it) } }
                    }
                }.let { if(it?.isNotEmpty() == true ) return@withContext it }
            }
            getApiConnection(u, "GET").let { conn ->
                val sz = conn.getHeaderFieldInt("Content-Length", 0)
                if (conn.responseCode >= 400)
                    throw IllegalArgumentException("HTTP${conn.responseCode} " +
                        parseErrorResponse(conn.errorStream.bufferedReader().use(BufferedReader::readText))
                )
                val ret = if (sz > 0 && p != null) {
                    conn.inputStream.use { it.readBytesWithProgress(sz, p) }
                } else {
                    conn.inputStream.use(InputStream::readBytes)
                }
                conn.disconnect()
                Log.d("MyDT", "getHttpContent: ${ret.size} bytes")
                if (!u.startsWith("https://$proxyUrl") &&
                    conn.getHeaderField("Content-type") != "application/json") {
                    throw IllegalStateException("非JSON返回: ${ret.decodeToString()}")
                }
                decodeBody(ret, conn.getHeaderField("Content-Encoding")?:"")
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
        FutureTask(if (!u.startsWith("https://$proxyUrl") && Comandy.instance.enabled) Callable{
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
                                if (it.code != 200) throw IllegalArgumentException("HTTP${it.code} ${
                                    it.data?.let { d -> Base64.decode(d, Base64.DEFAULT).decodeToString() }
                                }")
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
                val sz = connection.getHeaderFieldInt("Content-Length", 0)
                connection.inputStream.use { ci ->
                    if(readSize > 0) {
                        ret = ByteArray(readSize)
                        ci.read(ret, 0, readSize)
                    } else ret = if (sz > 0 && p != null) {
                        ci.readBytesWithProgress(sz, p)
                    } else {
                        ci.readBytes()
                    }
                }
                connection.disconnect()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            ret
        })
    }

    fun requestApiWithBody(url: String, method: String, body: ByteArray, contentType: String): ByteArray {
        Log.d("MyDT", "$method Http: $url")
        return if(!url.startsWith("https://$proxyUrl") && Comandy.instance.enabled) {
            val capsule = getComandyApiConnection(url, method)
            capsule.headers["content-type"] = contentType
            capsule.data = body.decodeToString()
            runBlocking { Comandy.instance.getInstance() }?.request(Gson().toJson(capsule))?.let { result ->
                Gson().fromJson(result, ComandyCapsule::class.java)?.let { c ->
                    c.data?.let { d ->
                        Base64.decode(d, Base64.DEFAULT).let {
                            (c.headers["Content-Encoding"] as String?)?.let { coding ->
                                decodeBody(it, coding)
                            }?:it
                        }
                    }?: throw IllegalStateException("empty comandy data")
                }
            }?: throw IllegalStateException("no comandy")
        } else {
            var ret: ByteArray
            var coding = ""
            getApiConnection(url, method).apply {
                outputStream.write(body)
                if (responseCode >= 400)
                    throw IllegalArgumentException("HTTP${responseCode} " +
                            parseErrorResponse(errorStream.bufferedReader().use(BufferedReader::readText))
                    )
                ret = inputStream.use(InputStream::readBytes)
                disconnect()
                coding = getHeaderField("Content-Encoding")?:""
            }
            decodeBody(ret, coding)
        }
    }
}
