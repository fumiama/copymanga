package top.fumiama.copymanga.api.manga

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.fumiama.copymanga.json.BookQueryStructure
import top.fumiama.copymanga.json.ReturnBase
import top.fumiama.copymanga.api.Config
import top.fumiama.copymanga.net.DownloadTools
import top.fumiama.dmzj.copymanga.R

class Shelf(private val getString: (Int) -> String) {
    private val apiUrl: String get() = getString(R.string.shelfOperateApiUrl).format(Config.myHostApiUrl.random())
    private val queryApiUrlTemplate = getString(R.string.bookUserQueryApiUrl)
    private val addApiUrl get() = "$apiUrl?platform=${Config.platform.value}"
    private val delApiUrl get() = "${apiUrl}s?platform=${Config.platform.value}"
    suspend fun add(comicId: String): String = withContext(Dispatchers.IO) {
        if (comicId.isEmpty()) {
            return@withContext "空漫画ID"
        }
        val body = buildString {
            append("comic_id=")
            append(comicId)
            append("&is_collect=1&authorization=Token+")
            append("")
            append(Config.token.value)
        }
        val re = (Config.apiProxy?.comancry(addApiUrl) { url ->
            DownloadTools.requestWithBody(
                url, "POST", body.encodeToByteArray()
            )
        }?:DownloadTools.requestWithBody(
            addApiUrl, "POST", body.encodeToByteArray()
        ))?.decodeToString() ?: return@withContext "空回应"
        return@withContext try {
            Gson().fromJson(re, ReturnBase::class.java).message
        } catch (e: Exception) {
            "$re ${e.message}"
        }
    }

    suspend fun del(vararg bookIds: Int): String = withContext(Dispatchers.IO) {
        if (bookIds.isEmpty()) {
            return@withContext "空ID列表"
        }
        val body = buildString {
            bookIds.forEach {
                append("ids=")
                append(it)
                append("&")
            }
            append("authorization=Token+")
            append(Config.token.value)
        }
        val re = (Config.apiProxy?.comancry(delApiUrl) { url ->
            DownloadTools.requestWithBody(
                url, "DELETE", body.encodeToByteArray()
            )
        }?:DownloadTools.requestWithBody(
            delApiUrl, "DELETE", body.encodeToByteArray()
        ))?.decodeToString() ?: return@withContext "空回应"
        return@withContext try {
            Gson().fromJson(re, ReturnBase::class.java).message
        } catch (e: Exception) {
            "$re ${e.message}"
        }
    }

    suspend fun query(pathWord: String): BookQueryStructure? = withContext(Dispatchers.IO) {
        try {
            val queryUrl = queryApiUrlTemplate.format(Config.myHostApiUrl.random(), pathWord, Config.platform.value)
            (Config.apiProxy?.comancry(queryUrl) { url ->
                DownloadTools.getHttpContent(url, Config.referer)
            }?:DownloadTools.getHttpContent(queryUrl, Config.referer)).let {
                Gson().fromJson(it.decodeToString(), BookQueryStructure::class.java)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
