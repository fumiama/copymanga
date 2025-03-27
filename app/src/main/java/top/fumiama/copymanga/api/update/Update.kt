package top.fumiama.copymanga.api.update

import android.app.Activity
import android.content.Context
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
import top.fumiama.copymanga.net.Client
import top.fumiama.copymanga.view.interaction.UITools
import top.fumiama.dmzj.copymanga.BuildConfig
import top.fumiama.dmzj.copymanga.R
import java.io.File
import java.security.MessageDigest

object Update {
    suspend fun checkUpdate(activity: AppCompatActivity, toolsBox: UITools, ignoreSkip: Boolean = false) = activity.apply{
        val client = Client("reilia.fumiama.top", 13212)
        val kanban = SimpleKanban(client, "fumiama")

        val progressBar = layoutInflater.inflate(R.layout.dialog_progress, null, false)
        val progressHandler = object : Client.Progress{
            override fun notify(progressPercentage: Int) {
                Log.d("MyUP", "Set progress: $progressPercentage")
                progressBar.dpp.progress = progressPercentage
            }
        }

        val msg = message(kanban)
        if (msg == "null") {
            if(ignoreSkip) withContext(Dispatchers.Main) {
                Toast.makeText(this@apply, "无更新", Toast.LENGTH_SHORT).show()
            }
            return@apply
        }

        val verNum = msg.substringBefore('\n').toIntOrNull()
        val skipNum = getPreferences(MODE_PRIVATE).getInt("skipVersion", 0)
        Log.d("MyUP", "Ver:$verNum, skip: $skipNum")
        if (verNum == null) return@apply

        if(!msg.contains("md5:")) {
            withContext(Dispatchers.Main) {
                toolsBox.buildInfo("看板", msg.substringAfter('\n'), "知道了")
            }
            return@apply
        }
        if(skipNum < verNum || ignoreSkip) {
            toolsBox.buildInfo("看板", msg.substringAfter('\n').substringBeforeLast('\n'), "下载新版", "跳过该版", "取消", {
                val info = toolsBox.buildAlertWithView("下载进度", progressBar, "隐藏")
                client.progress = progressHandler
                lifecycleScope.launch {
                    fetch(client, kanban, this@apply) {
                        lifecycleScope.launch {
                            val md5 = msg.substringAfterLast("md5:")
                            if (md5 == UITools.toHexStr(
                                    MessageDigest.getInstance("MD5").digest(it)
                                )
                            ) {
                                Toast.makeText(this@apply, "下载成功", Toast.LENGTH_SHORT).show()
                                info.dismiss()
                                install(it, this@apply)
                            } else runOnUiThread {
                                Toast.makeText(this@apply, "文件损坏", Toast.LENGTH_SHORT).show()
                                info.dismiss()
                            }
                            client.progress = null
                        }
                    }
                }
            }, {
                getPreferences(MODE_PRIVATE).edit {
                    putInt("skipVersion", verNum)
                    apply()
                }
            })
        }
    }

    private suspend fun message(kanban: SimpleKanban) = withContext(Dispatchers.IO) {
        return@withContext kanban[BuildConfig.VERSION_CODE]
    }

    private suspend fun fetch(client: Client, kanban: SimpleKanban, context: Context, doOnLoadSuccess: (data: ByteArray) -> Unit) = withContext(Dispatchers.IO) {
        return@withContext kanban.fetchRaw({ downloadFail(client, context) }, doOnLoadSuccess)
    }

    private suspend fun downloadFail(client: Client, context: Context) = withContext(Dispatchers.Main) {
        Toast.makeText(context, R.string.download_apk_fail, Toast.LENGTH_SHORT).show()
        client.progress = null
    }

    private suspend fun install(data: ByteArray, activity: Activity) = activity.apply{
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
