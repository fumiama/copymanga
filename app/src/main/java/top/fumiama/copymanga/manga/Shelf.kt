package top.fumiama.copymanga.manga

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.fumiama.copymanga.json.BookQueryStructure
import top.fumiama.copymanga.json.ReturnBase
import top.fumiama.copymanga.tools.http.DownloadTools
import top.fumiama.dmzj.copymanga.R

class Shelf(private val token: String, getString: (Int) -> String) {
    private val hostUrl: String = getString(R.string.hostUrl)
    private val apiUrl: String = getString(R.string.shelfOperateApiUrl).format(hostUrl)
    private val queryApiUrl = getString(R.string.bookUserQueryApiUrl)
    private val referer: String = getString(R.string.referer)
    private val ua: String = getString(R.string.pc_ua)
    suspend fun add(comicId: String): String = withContext(Dispatchers.IO) {
        if (comicId.isEmpty()) {
            return@withContext "空漫画ID"
        }
        val body = buildString {
            append("comic_id=")
            append(comicId)
            append("&is_collect=1&authorization=Token+")
            append("")
            append(token)
        }
        val re = DownloadTools.requestWithBody(
            "$apiUrl?platform=3", "POST", body.encodeToByteArray(), referer, ua
        )?.decodeToString() ?: return@withContext "空回应"
        return@withContext Gson().fromJson(re, ReturnBase::class.java).message
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
            append(token)
        }
        val re = DownloadTools.requestWithBody(
            "${apiUrl}s?platform=3", "DELETE", body.encodeToByteArray(), referer, ua
        )?.decodeToString() ?: return@withContext "空回应"
        return@withContext Gson().fromJson(re, ReturnBase::class.java).message
    }

    suspend fun query(pathWord: String): BookQueryStructure? = withContext(Dispatchers.IO) {
        try {
            Gson().fromJson(DownloadTools.getHttpContent(
                queryApiUrl.format(hostUrl, pathWord), referer, ua
            ).decodeToString(), BookQueryStructure::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
