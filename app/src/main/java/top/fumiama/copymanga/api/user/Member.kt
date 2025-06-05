package top.fumiama.copymanga.api.user

import android.util.Base64
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.fumiama.copymanga.api.Config
import top.fumiama.copymanga.json.LoginInfoStructure
import top.fumiama.dmzj.copymanga.R
import java.net.URLEncoder
import java.nio.charset.Charset

class Member(private val getString: (Int) -> String) {
    val hasLogin: Boolean get() = Config.token.value?.isNotEmpty() ?: false
    suspend fun login(username: String, pwd: String, salt: Int): LoginInfoStructure =
        withContext(Dispatchers.IO) {
            return@withContext try {
                saveInfo(postLogin(username, pwd, salt))
            } catch (e: Exception) {
                val l = LoginInfoStructure()
                l.code = 400
                l.message =  e.message.toString()
                l
            }
        }

    /**
     * 获得登录信息并更新头像
     * @return 登录态
     * - **code**: 449: 未登录, 450: 有 Exception
     * - **message**: 可以 toast 的信息
     */
    suspend fun info(): LoginInfoStructure = withContext(Dispatchers.IO) {
        if (!hasLogin) {
            val l = LoginInfoStructure()
            l.code = 449
            l.message = getString(R.string.noLogin)
            return@withContext l
        }
        try {
            val u = getString(R.string.memberInfoApiUrl)
                .format(Config.platform.value)
            try {
                val l = Gson().fromJson(Config.myHostApiUrl.get(u), LoginInfoStructure::class.java)
                if (l.code == 200) Config.avatar.value = l.results.avatar
                l
            } catch (e: Exception) {
                val l = LoginInfoStructure()
                l.code = 450
                l.message = "${getString(R.string.login_get_avatar_failed)}: ${e.message}"
                l
            }
        } catch (e: Exception) {
            val l = LoginInfoStructure()
            l.code = 450
            l.message = "${getString(R.string.login_get_avatar_failed)}: ${e.message}"
            l
        }
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        Config.token.value = ""
        Config.user_id.value = null
        Config.username.value = null
        Config.nickname.value = null
        Config.avatar.value = null
    }

    private suspend fun saveInfo(data: ByteArray) = data.inputStream().use { dataIn ->
        try {
            Gson().fromJson(dataIn.reader(), LoginInfoStructure::class.java)?.let { l ->
                if (l.code == 200) {
                    Config.token.value = l.results?.token
                    Config.user_id.value = l.results?.user_id
                    Config.username.value = l.results?.username
                    Config.nickname.value = l.results.nickname
                    return@use info()
                }
                return@use l
            } ?: throw Exception(getString(R.string.login_parse_json_error))
        } catch (e: Exception) {
            throw Exception(data.decodeToString(), e)
        }
    }

    private suspend fun postLogin(username: String, pwd: String, salt: Int): ByteArray =
        getString(R.string.loginApiUrl).format(Config.platform.value).let { u ->
            val r = if (!Config.net_use_foreign.value) "1" else "0"
            val pwdEncoded =
                Base64.encode("$pwd-$salt".toByteArray(), Base64.DEFAULT).decodeToString()
            Config.myHostApiUrl.request(u, "username=${
                URLEncoder.encode(
                    username,
                    Charset.defaultCharset().name()
                )
            }&password=$pwdEncoded&salt=$salt&platform=${Config.platform.value}&authorization=Token+&version=${Config.app_ver.value}&source=copyApp&region=$r&webp=1".encodeToByteArray(),
                "POST", "application/x-www-form-urlencoded;charset=utf-8")
        }.encodeToByteArray()
}
