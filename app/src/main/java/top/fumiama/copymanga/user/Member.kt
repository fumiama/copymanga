package top.fumiama.copymanga.user

import android.content.SharedPreferences
import android.widget.Toast
import com.google.gson.Gson
import top.fumiama.copymanga.MainActivity
import top.fumiama.copymanga.json.LoginInfoStructure
import top.fumiama.copymanga.tools.api.CMApi
import top.fumiama.copymanga.tools.http.DownloadTools
import top.fumiama.dmzj.copymanga.R

class Member(private val pref: SharedPreferences, private val getString: (Int) -> String) {
    val hasLogin: Boolean get() = pref.getString("token", "")?.isNotEmpty()?:false
    fun login(username: String, pwd: String, salt: Int): LoginInfoStructure {
        try {
            CMApi.getLoginConnection(username, pwd, salt)?.apply {
                Gson().fromJson(inputStream.reader(), LoginInfoStructure::class.java)?.let { data ->
                    disconnect()
                    if(data.code == 200) {
                        pref.edit()?.apply {
                            putString("token", data.results?.token)
                            putString("user_id", data.results?.user_id)
                            putString("username", data.results?.username)
                            putString("nickname", data.results?.nickname)
                            apply()
                            return refreshAvatar()
                        }
                    }
                    return data
                }
            }
            val l = LoginInfoStructure()
            l.code = 400
            l.message =  getString(R.string.login_get_conn_failed)
            return l
        } catch (e: Exception) {
            val l = LoginInfoStructure()
            l.code = 400
            l.message = e.localizedMessage
            return l
        }
    }

    fun refreshAvatar() : LoginInfoStructure {
        try {
            DownloadTools.getHttpContent(getString(R.string.memberInfoApiUrl).format(
                CMApi.myHostApiUrl))?.decodeToString()?.let {
                val l = Gson().fromJson(it, LoginInfoStructure::class.java)
                if(l.code == 200) pref.edit()?.apply {
                    putString("avatar", l.results.avatar)
                    apply()
                }
                return l
            }
        } catch (e: Exception) {
            val l = LoginInfoStructure()
            l.code = 400
            l.message = "${getString(R.string.login_get_avatar_failed)}: ${e.localizedMessage}"
            return l
        }
        val l = LoginInfoStructure()
        l.code = 400
        l.message =  getString(R.string.login_get_avatar_failed)
        return l
    }

    fun logout() {
        pref.edit()?.apply {
            remove("token")
            remove("user_id")
            remove("username")
            remove("nickname")
            remove("avatar")
            apply()
        }
    }
}
