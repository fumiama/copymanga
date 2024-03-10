package top.fumiama.copymanga.user

import android.content.SharedPreferences
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.fumiama.copymanga.json.LoginInfoStructure
import top.fumiama.copymanga.tools.api.CMApi
import top.fumiama.copymanga.tools.http.DownloadTools
import top.fumiama.dmzj.copymanga.R
import java.net.URLEncoder
import java.nio.charset.Charset

class Member(private val pref: SharedPreferences, private val getString: (Int) -> String) {
    val hasLogin: Boolean get() = pref.getString("token", "")?.isNotEmpty()?:false
    suspend fun login(username: String, pwd: String, salt: Int): LoginInfoStructure = withContext(Dispatchers.IO) {
        try {
            getLoginConnection(username, pwd, salt).apply {
                Gson().fromJson<LoginInfoStructure>(
                    JsonReader(inputStream.reader()), LoginInfoStructure::class.java
                )?.let { data ->
                    disconnect()
                    if(data.code == 200) {
                        pref.edit()?.apply {
                            putString("token", data.results?.token)
                            putString("user_id", data.results?.user_id)
                            putString("username", data.results?.username)
                            putString("nickname", data.results?.nickname)
                            apply()
                            return@withContext info()
                        }
                    }
                    return@withContext data
                }
            }
            val l = LoginInfoStructure()
            l.code = 400
            l.message =  getString(R.string.login_get_conn_failed)
            return@withContext l
        } catch (e: Exception) {
            val l = LoginInfoStructure()
            l.code = 400
            l.message = e.localizedMessage
            return@withContext l
        }
    }


    /**
     * 获得登录信息并更新头像
     * @return 登录态
     * - **code**: 449: 未登录, 450: 有 Exception
     * - **message**: 可以 toast 的信息
     */
    suspend fun info() : LoginInfoStructure = withContext(Dispatchers.IO) {
        if (!pref.contains("token")) {
            val l = LoginInfoStructure()
            l.code = 449
            l.message = getString(R.string.noLogin)
            return@withContext l
        }
        return@withContext try {
            val l = Gson().fromJson(DownloadTools.getHttpContent(
                getString(R.string.memberInfoApiUrl).format(CMApi.myHostApiUrl)).decodeToString(),
                LoginInfoStructure::class.java)
            if(l.code == 200) pref.edit()?.apply {
                putString("avatar", l.results.avatar)
                apply()
            }
            l
        } catch (e: Exception) {
            val l = LoginInfoStructure()
            l.code = 450
            l.message = "${getString(R.string.login_get_avatar_failed)}: $e"
            l
        }
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        pref.edit()?.apply {
            remove("token")
            remove("user_id")
            remove("username")
            remove("nickname")
            remove("avatar")
            apply()
        }
    }

    private fun getLoginConnection(username: String, pwd: String, salt: Int) =
        getString(R.string.loginApiUrl).format(CMApi.myHostApiUrl).let {
            DownloadTools.getApiConnection(it, "POST").apply {
                pref.apply {
                    doOutput = true
                    setRequestProperty("content-type", "application/x-www-form-urlencoded;charset=utf-8")
                    setRequestProperty("platform", "3")
                    setRequestProperty("accept", "application/json")
                    val r = if(!getBoolean("settings_cat_net_sw_use_foreign", false)) "1" else "0"
                    val pwdEncoded = Base64.encode("$pwd-$salt".toByteArray(), Base64.DEFAULT).decodeToString()
                    outputStream.write("username=${URLEncoder.encode(username, Charset.defaultCharset().name())}&password=$pwdEncoded&salt=$salt&platform=3&authorization=Token+&version=1.4.4&source=copyApp&region=$r&webp=1".toByteArray())
                }
            }
        }
}
