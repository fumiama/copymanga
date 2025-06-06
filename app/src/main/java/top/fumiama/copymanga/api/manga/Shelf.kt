package top.fumiama.copymanga.api.manga

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.fumiama.copymanga.api.Config
import top.fumiama.copymanga.json.BookQueryStructure
import top.fumiama.copymanga.json.ReturnBase
import top.fumiama.dmzj.copymanga.R

class Shelf(private val getString: (Int) -> String) {
    private val apiUrl: String get() = getString(R.string.shelfOperateApiUrl)
    private val queryApiUrlTemplate = getString(R.string.bookUserQueryApiUrl)
    private val addApiUrl get() = "$apiUrl?platform=${Config.platform.value}"
    private val delApiUrl get() = "${apiUrl}s?platform=${Config.platform.value}"
    suspend fun add(comicId: String): String = withContext(Dispatchers.IO) {
        if (comicId.isEmpty()) {
            throw IllegalArgumentException("空漫画ID")
        }
        val body = buildString {
            append("comic_id=")
            append(comicId)
            append("&is_collect=1&authorization=Token+")
            append("")
            append(Config.token.value)
        }
        val re = Config.myHostApiUrl.request(
            addApiUrl, body.encodeToByteArray(), "POST",
            "application/x-www-form-urlencoded;charset=utf-8")
        Gson().fromJson(re, ReturnBase::class.java).message
    }

    suspend fun del(vararg bookIds: Int): String = withContext(Dispatchers.IO) {
        if (bookIds.isEmpty()) {
            throw IllegalArgumentException("空ID列表")
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
        val re = Config.myHostApiUrl.request(
            delApiUrl, body.encodeToByteArray(),
            "DELETE", "application/x-www-form-urlencoded;charset=utf-8")
        Gson().fromJson(re, ReturnBase::class.java).message
    }

    suspend fun query(pathWord: String): BookQueryStructure? = withContext(Dispatchers.IO) {
        val queryUrl = queryApiUrlTemplate.format(pathWord, Config.platform.value)
        Config.myHostApiUrl.get(queryUrl).let {
            Gson().fromJson(it, BookQueryStructure::class.java)
        }
    }
}
