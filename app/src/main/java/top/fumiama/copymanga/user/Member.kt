package top.fumiama.copymanga.user

import android.util.Base64
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.fumiama.copymanga.api.Config
import top.fumiama.copymanga.json.ComandyCapsule
import top.fumiama.copymanga.json.LoginInfoStructure
import top.fumiama.copymanga.tools.http.Comandy
import top.fumiama.copymanga.tools.http.DownloadTools
import top.fumiama.dmzj.copymanga.R
import java.net.URLEncoder
import java.nio.charset.Charset

class Member(private val getString: (Int) -> String) {
    val hasLogin: Boolean get() = Config.token.value?.isNotEmpty()?:false
    suspend fun login(username: String, pwd: String, salt: Int): LoginInfoStructure = withContext(Dispatchers.IO) {
        var err = ""
        if (!Config.net_use_api_proxy.value && Comandy.useComandy)
            getComandyLoginConnection(username, pwd, salt).let { capsule ->
            try {
                val para = Gson().toJson(capsule)
                Comandy.instance?.request(para)?.let { result ->
                    Gson().fromJson(result, ComandyCapsule::class.java)!!.let {
                        if (it.code != 200) {
                            val l = LoginInfoStructure()
                            l.code = it.code
                            l.message = it.data?.let { d -> Base64.decode(d, Base64.DEFAULT).decodeToString() }?:"HTTP ${it.code}"
                            return@withContext l
                        }
                        Base64.decode(it.data, Base64.DEFAULT)
                    }
                }
            } catch (e: Exception) {
                err = e.message.toString()
                null
            }
        }?.let {
            try {
                return@withContext saveInfo(it)
            } catch (e: Exception) {
                err = e.message.toString()
            }
        }
        else getLoginConnection(username, pwd, salt).apply {
            inputStream.use {
                it?.readBytes()?.let { data ->
                    try {
                        return@withContext saveInfo(data)
                    } catch (e: Exception) {
                        err = e.message.toString()
                    }
                }?: run { err = getString(R.string.login_get_conn_failed) }
            }
        }
        val l = LoginInfoStructure()
        l.code = 400
        l.message = err
        return@withContext l
    }


    /**
     * 获得登录信息并更新头像
     * @return 登录态
     * - **code**: 449: 未登录, 450: 有 Exception
     * - **message**: 可以 toast 的信息
     */
    suspend fun info() : LoginInfoStructure = withContext(Dispatchers.IO) {
        if (!hasLogin) {
            val l = LoginInfoStructure()
            l.code = 449
            l.message = getString(R.string.noLogin)
            return@withContext l
        }
        try {
            val data = DownloadTools.getHttpContent(
                getString(R.string.memberInfoApiUrl).format(Config.myHostApiUrl.value).let {
                    Config.apiProxy?.wrap(it)?:it
                }
            ).decodeToString()
            try {
                val l = Gson().fromJson(data, LoginInfoStructure::class.java)
                if(l.code == 200) Config.avatar.value = l.results.avatar
                l
            } catch (e : Exception) {
                val l = LoginInfoStructure()
                l.code = 450
                l.message = "${getString(R.string.login_get_avatar_failed)}: $data"
                l
            }
        } catch (e: Exception) {
            val l = LoginInfoStructure()
            l.code = 450
            l.message = "${getString(R.string.login_get_avatar_failed)}: $e"
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
                if(l.code == 200) {
                    Config.token.value = l.results?.token
                    Config.user_id.value = l.results?.user_id
                    Config.username.value = l.results?.username
                    Config.nickname.value = l.results.nickname
                    return@use info()
                }
                return@use l
            }?: throw Exception(getString(R.string.login_parse_json_error))
        } catch (e: Exception) {
            throw Exception(data.decodeToString(), e)
        }
    }

    private fun getLoginConnection(username: String, pwd: String, salt: Int) =
        getString(R.string.loginApiUrl).format(Config.myHostApiUrl.value).let {
            Config.apiProxy?.wrap(it)?:it
        }.let {
            DownloadTools.getApiConnection(it, "POST").apply {
                doOutput = true
                setRequestProperty("content-type", "application/x-www-form-urlencoded;charset=utf-8")
                setRequestProperty("platform", "3")
                setRequestProperty("accept", "application/json")
                val r = if(!Config.net_use_foreign.value) "1" else "0"
                val pwdEncoded = Base64.encode("$pwd-$salt".toByteArray(), Base64.DEFAULT).decodeToString()
                outputStream.write("username=${URLEncoder.encode(username, Charset.defaultCharset().name())}&password=$pwdEncoded&salt=$salt&platform=3&authorization=Token+&version=${Config.app_ver.value}&source=copyApp&region=$r&webp=1".toByteArray())
            }
        }

    private fun getComandyLoginConnection(username: String, pwd: String, salt: Int) =
        getString(R.string.loginApiUrl).format(Config.myHostApiUrl.value).let {
            Config.apiProxy?.wrap(it)?:it
        }.let {
            DownloadTools.getComandyApiConnection(it, "POST", null, Config.pc_ua).apply {
                headers["content-type"] = "application/x-www-form-urlencoded;charset=utf-8"
                headers["platform"] = "3"
                headers["accept"] = "application/json"
                val r = if(!Config.net_use_foreign.value) "1" else "0"
                val pwdEncoded = Base64.encode("$pwd-$salt".toByteArray(), Base64.DEFAULT).decodeToString()
                data = "username=${URLEncoder.encode(username, Charset.defaultCharset().name())}&password=$pwdEncoded&salt=$salt&platform=3&authorization=Token+&version=${Config.app_ver.value}&source=copyApp&region=$r&webp=1"
            }
        }
}
