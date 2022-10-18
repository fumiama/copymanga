package top.fumiama.copymanga

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_login.*
import top.fumiama.copymanga.json.BookListStructure
import top.fumiama.copymanga.json.LoginInfoStructure
import top.fumiama.copymanga.tools.api.CMApi
import top.fumiama.copymanga.tools.http.DownloadTools
import top.fumiama.dmzj.copymanga.R
import kotlin.random.Random
import kotlin.random.nextUInt

class LoginActivity:Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        alblogin.setOnClickListener {
            val salt = Random.nextInt(10000)
            altusrnm.text?.toString()?.let { username ->
                altpwd.text?.toString()?.let { pwd ->
                    Thread{
                        try {
                            CMApi.getLoginConnection(username, pwd, salt)?.apply {
                                Gson().fromJson(inputStream.reader(), LoginInfoStructure::class.java)?.let {
                                    if(it.code == 200) {
                                        MainActivity.mainWeakReference?.get()?.getPreferences(MODE_PRIVATE)?.edit()?.apply {
                                            putString("token", it.results?.token)
                                            putString("user_id", it.results?.user_id)
                                            putString("username", it.results?.username)
                                            putString("nickname", it.results?.nickname)
                                            apply()
                                            DownloadTools.getHttpContent(getString(R.string.memberInfoApiUrl))?.decodeToString()?.let {
                                                val l = Gson().fromJson(it, LoginInfoStructure::class.java)
                                                if(l.code == 200) {
                                                    putString("avatar", l.results.avatar)
                                                    apply()
                                                } else runOnUiThread { Toast.makeText(this@LoginActivity, l.message, Toast.LENGTH_SHORT).show() }
                                            }
                                            runOnUiThread { finish() }
                                        }?:runOnUiThread { Toast.makeText(this@LoginActivity, R.string.login_get_conn_failed, Toast.LENGTH_SHORT).show() }
                                    } else runOnUiThread { Toast.makeText(this@LoginActivity, it.message, Toast.LENGTH_SHORT).show() }
                                }
                                disconnect()
                            }?:runOnUiThread { Toast.makeText(this, R.string.login_get_conn_failed, Toast.LENGTH_SHORT).show() }
                        }catch (e: Exception) {
                            runOnUiThread { Toast.makeText(this, e.localizedMessage, Toast.LENGTH_SHORT).show() }
                        }
                    }.start()
                }?:Toast.makeText(this, R.string.login_null_pwd, Toast.LENGTH_SHORT).show()
            }?:Toast.makeText(this, R.string.login_null_username, Toast.LENGTH_SHORT).show()
        }
    }
}
