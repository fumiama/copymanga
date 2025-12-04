package top.fumiama.copymanga.api.update

import android.app.Activity
import android.app.AlertDialog
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.dialog_progress.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.fumiama.copymanga.view.interaction.UITools
import top.fumiama.dmzj.copymanga.R
import top.fumiama.sdict.ApkUpdater
import top.fumiama.sdict.io.Client
import java.io.File
import java.lang.ref.WeakReference

class Updater(
    private val a: WeakReference<AppCompatActivity>, private val toolsBox: UITools,
    private val ignoreSkip: Boolean, private val skipNum: Int,
): ApkUpdater("reilia.fumiama.top", 13212, "fumiama") {
    private var mInfo: AlertDialog? = null
        set(value) {
            field?.dismiss()
            field = value
        }

    override suspend fun onCheckLatestVersion(version: Int) {
        super.onCheckLatestVersion(version)
        a.get()?.apply {
            if (ignoreSkip) withContext(Dispatchers.Main) {
                Toast.makeText(this@apply, "无更新", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override suspend fun onCheckNewVersion(version: Int, message: String, md5: String?) {
        super.onCheckNewVersion(version, message, md5)
        if (md5 == null) {
            withContext(Dispatchers.Main) {
                toolsBox.buildInfo("看板", message, "知道了")
            }
            return
        }
        if(skipNum < version || ignoreSkip) {
            val progressBar =
                a.get()?.layoutInflater?.inflate(R.layout.dialog_progress, null, false) ?: return
            val progressHandler = object : Client.Progress {
                override fun notify(progressPercentage: Int) {
                    Log.d("MyUP", "Set progress: $progressPercentage")
                    progressBar.dpp.progress = progressPercentage
                }
            }
            toolsBox.buildInfo("看板", message, "下载新版", "跳过该版", "取消", {
                mInfo = toolsBox.buildAlertWithView("下载进度", progressBar, "隐藏")
                a.get()?.lifecycleScope?.launch { download(md5, progressHandler) }
            }, {
                a.get()?.apply {
                    getPreferences(MODE_PRIVATE).edit {
                        putInt("skipVersion", version)
                        apply()
                    }
                }
            })
        }
    }

    override suspend fun onDownloadNewVersionFailed(cause: Int) {
        super.onDownloadNewVersionFailed(cause)
        withContext(Dispatchers.Main) {
            mInfo?.dismiss()
            mInfo = null
            when (cause) {
                UPDATE_FAIL_NETWORK -> a.get()?.apply {
                    Toast.makeText(this@apply, "网络错误", Toast.LENGTH_SHORT).show()
                }
                UPDATE_FAIL_FILE_CORRUPT -> a.get()?.apply {
                    Toast.makeText(this@apply, "文件损坏", Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }

    override suspend fun onDownloadNewVersionSuccess(data: ByteArray) {
        super.onDownloadNewVersionSuccess(data)
        withContext(Dispatchers.Main) {
            mInfo?.dismiss()
            mInfo = null
            a.get()?.apply {
                Toast.makeText(this@apply, "下载成功", Toast.LENGTH_SHORT).show()
                install(data, this)
            }
        }
    }

    private suspend fun install(data: ByteArray, activity: Activity) = activity.apply {
        withContext(Dispatchers.IO) {
            val f = File(externalCacheDir, "new.apk")
            f.writeBytes(data)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val contentUri: Uri = FileProvider.getUriForFile(this@apply, "$packageName.fileprovider", f)
                intent.setDataAndType(contentUri, "application/vnd.android.package-archive")
            } else intent.setDataAndType(Uri.fromFile(f), "application/vnd.android.package-archive")
            withContext(Dispatchers.Main) {
                startActivity(intent)
            }
        }
    }
}