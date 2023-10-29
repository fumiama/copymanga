package top.fumiama.copymanga

import android.app.Activity
import android.os.Bundle
import android.util.Log
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
            Thread {
                if (isLogout) {
                    MainActivity.member?.logout()
                    runOnUiThread {
                        MainActivity.mainWeakReference?.get()?.refreshUserInfo()
                        Toast.makeText(this@LoginActivity, R.string.login_restart_to_apply, Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    return@Thread
                }
                val l = MainActivity.member?.login(username, pwd, salt)
                Log.d("MyLA", "login return code: ${l?.code}")
                if (l?.code == 200) {
                    runOnUiThread {
                        MainActivity.mainWeakReference?.get()?.refreshUserInfo()
                        finish()
                    }
                    return@Thread
                }
                runOnUiThread { Toast.makeText(this@LoginActivity, l?.message, Toast.LENGTH_SHORT).show() }
            }.start()
        }
    }
}
