package top.fumiama.copymanga.manga

import android.content.SharedPreferences
import com.google.gson.Gson
import top.fumiama.copymanga.json.ReturnBase
import top.fumiama.copymanga.tools.http.DownloadTools

class Shelf(private val token: String, private val apiUrl: String, private val referer: String, private val ua: String) {
    fun add(comicId: String): String {
        if (comicId.isEmpty()) {
            return "空漫画ID"
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
        )?.decodeToString() ?: return "空回应"
        return Gson().fromJson(re, ReturnBase::class.java).message
    }

    fun del(vararg bookIds: Int): String {
        if (bookIds.isEmpty()) {
            return "空ID列表"
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
        )?.decodeToString() ?: return "空回应"
        return Gson().fromJson(re, ReturnBase::class.java).message
    }
}
