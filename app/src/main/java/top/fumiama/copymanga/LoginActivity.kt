package top.fumiama.copymanga

import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.coroutines.launch
import top.fumiama.dmzj.copymanga.R
import kotlin.random.Random


class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        val pref = MainActivity.mainWeakReference?.get()?.getPreferences(MODE_PRIVATE) ?: return
        val isLogout = pref.getString("token", null) != null
        if (isLogout) {
            alblogin.setText(R.string.logout)
            altusrnm.setText(pref.getString("username", "N/A"))
        }
        alblogin.setOnClickListener {
            lifecycleScope.launch {
                val salt = Random.nextInt(10000)
                val username = altusrnm.text?.toString() ?: run {
                    Toast.makeText(
                        this@LoginActivity,
                        R.string.login_null_username,
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }
                val pwd = altpwd.text?.toString() ?: run {
                    Toast.makeText(this@LoginActivity, R.string.login_null_pwd, Toast.LENGTH_SHORT)
                        .show()
                    return@launch
                }
                if (isLogout) {
                    MainActivity.member?.logout()
                    MainActivity.mainWeakReference?.get()?.refreshUserInfo()
                    Toast.makeText(
                        this@LoginActivity,
                        R.string.login_restart_to_apply,
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                    return@launch
                }
                val l = MainActivity.member?.login(username, pwd, salt)
                Log.d("MyLA", "login return code: ${l?.code}")
                if (l?.code == 200) {
                    MainActivity.mainWeakReference?.get()?.refreshUserInfo()
                    finish()
                    return@launch
                }
                Toast.makeText(this@LoginActivity, l?.message, Toast.LENGTH_LONG).show()
            }
        }
        PreferenceManager.getDefaultSharedPreferences(this)?.apply {
            if (contains("settings_cat_general_sw_enable_transparent_systembar")) {
                if (getBoolean("settings_cat_general_sw_enable_transparent_systembar", false)) {
                    WindowCompat.setDecorFitsSystemWindows(window, false)
                    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
                    window.statusBarColor = 0
                    window.navigationBarColor = 0
                }
            }
        }
    }
}
