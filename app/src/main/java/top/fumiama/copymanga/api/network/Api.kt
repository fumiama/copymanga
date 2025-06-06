package top.fumiama.copymanga.api.network

import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import top.fumiama.copymanga.MainActivity
import top.fumiama.copymanga.api.Config.apiProxy
import top.fumiama.copymanga.api.Config.networkApiUrl
import top.fumiama.copymanga.api.Config.platform
import top.fumiama.copymanga.api.Config.proxyUrl
import top.fumiama.copymanga.api.Config.reverseProxyUrl
import top.fumiama.copymanga.json.NetworkStructure
import top.fumiama.copymanga.json.ReturnBase
import top.fumiama.copymanga.net.DownloadTools
import top.fumiama.dmzj.copymanga.R

class Api {
    private var mHostApiUrls = mutableListOf<String>()
    private var mu = Mutex()

    fun getApis(): Array<String> {
        return mHostApiUrls.toTypedArray()
    }

    suspend fun init() {
        if (mHostApiUrls.isNotEmpty()) return
        if (reverseProxyUrl.value.isNotEmpty() && reverseProxyUrl.value != proxyUrl) {
            mu.withLock { mHostApiUrls = mutableListOf(reverseProxyUrl.value) }
            Log.d("MyApi", "myHostApiUrl set reverse proxy to ${reverseProxyUrl.value}")
            return
        }
        MainActivity.mainWeakReference?.get()?.apply {
            mu.withLock {
                if (mHostApiUrls.isNotEmpty()) return
                try {
                    val d = get(getString(R.string.networkApiUrl).format(platform.value), networkApiUrl.value)
                    val r = Gson().fromJson(d, NetworkStructure::class.java)
                    if (r != null) {
                        Log.d("MyApi", "myHostApiUrl get code ${r.code} msg ${r.message}")
                        if (r.results != null) {
                            r.results.api.forEach { it.forEach { api -> if (!api.isNullOrEmpty() && api !in mHostApiUrls) mHostApiUrls += api } }
                            r.results.share.forEach { api -> if (!api.isNullOrEmpty() && api !in mHostApiUrls) mHostApiUrls += api }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    mHostApiUrls = mutableListOf(networkApiUrl.value)
                    withContext(Dispatchers.Main) {
                        runOnUiThread {
                            Toast.makeText(this@apply, "${e::class.simpleName} ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                if (mHostApiUrls.isEmpty()) {
                    mHostApiUrls = mutableListOf(networkApiUrl.value)
                    Log.d("MyApi", "myHostApiUrl set default ${mHostApiUrls[0]}")
                    withContext(Dispatchers.Main) {
                        runOnUiThread {
                            Toast.makeText(this@apply, "无法获取API列表", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
        Log.d("MyApi", "myHostApiUrl get hosts ${mHostApiUrls.joinToString(", ")}")
    }
    // get throw error on non-json or non-200 or empty apis, path: /api/v3/xxx, return json string
    suspend fun get(path: String, forceApi: String? = null): String {
        val apis = if (forceApi == null) mu.withLock { mHostApiUrls.toTypedArray() } else arrayOf(forceApi)
        if (apis.isEmpty()) {
            throw NoSuchElementException("API列表为空")
        }
        var r: ReturnBase? = null
        apis.forEachIndexed { i, api ->
            val u = "https://$api$path"
            try {
                val ret = (apiProxy?.comancry(u) {
                    DownloadTools.getApiContent(it)
                }?: DownloadTools.getApiContent(u)).decodeToString()
                r = Gson().fromJson(ret, ReturnBase::class.java)
                if (r!!.code != 200) {
                    withContext(Dispatchers.Main) {
                        MainActivity.mainWeakReference?.get()?.apply {
                            runOnUiThread {
                                Toast.makeText(this, "错误码${r?.code?:-1}, 信息: ${r?.message?:"空"}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    return ret
                }
            } catch (e: Exception) {
                mu.withLock  {
                    if (mHostApiUrls.size <= 1) return@withLock
                    mHostApiUrls.remove(api)
                }
                if (i >= apis.size-1) { // throw last exception
                    throw e
                }
            }
        }
        throw IllegalStateException("错误码${r?.code?:-1}, 信息: ${r?.message?:"空"}")
    }
    // request throw error on non-json or non-200 or empty apis, path: /api/v3/xxx, return json string
    suspend fun request(path: String, body: ByteArray,  method: String, contentType: String, forceApi: String? = null): String {
        val apis = if (forceApi == null) mu.withLock  { mHostApiUrls } else mutableListOf(forceApi)
        if (apis.isEmpty()) {
            throw NoSuchElementException("API列表为空")
        }
        var r: ReturnBase? = null
        apis.forEachIndexed { i, api ->
            val u = "https://$api$path"
            try {
                val ret = (apiProxy?.comancry(u) {
                    DownloadTools.requestApiWithBody(u, method, body, contentType)
                }?: DownloadTools.requestApiWithBody(u, method, body, contentType)).decodeToString()
                r = Gson().fromJson(ret, ReturnBase::class.java)
                if (r!!.code != 200) {
                    withContext(Dispatchers.Main) {
                        MainActivity.mainWeakReference?.get()?.apply {
                            runOnUiThread {
                                Toast.makeText(this, "错误码${r?.code?:-1}, 信息: ${r?.message?:"空"}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    return ret
                }
            } catch (e: Exception) {
                e.printStackTrace()
                mu.withLock  {
                    if (mHostApiUrls.size <= 1) return@withLock
                    mHostApiUrls.remove(api)
                }
                if (i >= apis.size-1) { // throw last exception
                    throw e
                }
            }
        }
        throw IllegalStateException("错误码${r?.code?:-1}, 信息: ${r?.message?:"空"}")
    }
}
