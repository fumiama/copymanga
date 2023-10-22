package top.fumiama.copymanga

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_login.*
import top.fumiama.copymanga.json.LoginInfoStructure
import top.fumiama.copymanga.tools.api.CMApi
import top.fumiama.copymanga.tools.http.DownloadTools
import top.fumiama.dmzj.copymanga.R
import kotlin.random.Random

class LoginActivity:Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        val pref = MainActivity.mainWeakReference?.get()?.getPreferences(MODE_PRIVATE) ?: return
        val isLogout = pref.getString("token", null) != null
        if (isLogout) {
            alblogin.setText(R.string.logout)
        }
        alblogin.setOnClickListener {
            val salt = Random.nextInt(10000)
                val username = altusrnm.text?.toString() ?: run {
                    Toast.makeText(this, R.string.login_null_username, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            val pwd = altpwd.text?.toString() ?: run {
                Toast.makeText(this, R.string.login_null_pwd, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Thread{
                if (isLogout) {
                    pref.edit()?.apply {
                        remove("token")
                        remove("user_id")
                        remove("username")
                        remove("nickname")
                        remove("avatar")
                        apply()
                        runOnUiThread {
                            MainActivity.mainWeakReference?.get()?.refreshUserInfo()
                            Toast.makeText(this@LoginActivity, R.string.login_restart_to_apply, Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                    return@Thread
                }
                try {
                    CMApi.getLoginConnection(username, pwd, salt)?.apply {
                        Gson().fromJson(inputStream.reader(), LoginInfoStructure::class.java)?.let { data ->
                            if(data.code == 200) {
                                pref.edit()?.apply {
                                    putString("token", data.results?.token)
                                    putString("user_id", data.results?.user_id)
                                    putString("username", data.results?.username)
                                    putString("nickname", data.results?.nickname)
                                    apply()
                                    DownloadTools.getHttpContent(getString(R.string.memberInfoApiUrl).format(CMApi.myHostApiUrl))?.decodeToString()?.let {
                                        val l = Gson().fromJson(it, LoginInfoStructure::class.java)
                                        if(l.code == 200) {
                                            putString("avatar", l.results.avatar)
                                            apply()
                                            runOnUiThread {
                                                MainActivity.mainWeakReference?.get()?.refreshUserInfo()
                                            }
                                        } else runOnUiThread { Toast.makeText(this@LoginActivity, l.message, Toast.LENGTH_SHORT).show() }
                                    }
                                    runOnUiThread { finish() }
                                }?:runOnUiThread { Toast.makeText(this@LoginActivity, R.string.login_get_conn_failed, Toast.LENGTH_SHORT).show() }
                            } else runOnUiThread { Toast.makeText(this@LoginActivity, data.message, Toast.LENGTH_SHORT).show() }
                        }
                        disconnect()
                    }?:runOnUiThread { Toast.makeText(this, R.string.login_get_conn_failed, Toast.LENGTH_SHORT).show() }
                } catch (e: Exception) {
                    runOnUiThread { Toast.makeText(this, e.localizedMessage, Toast.LENGTH_SHORT).show() }
                }
            }.start()
        }
    }
}
