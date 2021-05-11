package top.fumiama.copymanga.update

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.android.synthetic.main.dialog_progress.view.*
import top.fumiama.copymanga.tools.file.PropertiesTools
import top.fumiama.copymanga.tools.api.UITools
import top.fumiama.dmzj.copymanga.R
import java.io.File
import java.security.MessageDigest

object Update {
    fun checkUpdate(activity: Activity, p: PropertiesTools, toolsBox: UITools, ignoreSkip: Boolean = false) = activity.apply{
        val client = Client("copymanga.v6.army", 12315)
        val progressBar = layoutInflater.inflate(R.layout.dialog_progress, null, false)
        val progressHandler = object : Client.Progress{
            override fun notify(progressPercentage: Int) {
                Log.d("MyUP", "Set progress: $progressPercentage")
                progressBar.dpp.progress = progressPercentage
            }
        }
        val kanban = SimpleKanban(client, "fumiama")
        val msg = kanban[packageManager.getPackageInfo(packageName, 0).versionCode]
        if(msg != "null") {
            val verNum = msg.substringBefore('\n').toIntOrNull()
            val skipNum = p["skipVersion"].let { if(it != "null") it.toInt() else 0 }

            Log.d("MyUP", "Ver:$verNum, skip: $skipNum")
            if(verNum != null) {
                if(msg.contains("md5:")) {
                    if(skipNum < verNum || ignoreSkip) runOnUiThread {
                        toolsBox.buildInfo("看板", msg.substringAfter('\n').substringBeforeLast('\n'), "下载新版", "跳过该版", "取消", {
                            val info = toolsBox.buildAlertWithView("下载进度", progressBar, "隐藏")
                            client.progress = progressHandler
                            Thread {
                                kanban.fetchRaw({
                                    runOnUiThread {
                                        Toast.makeText(this, "下载失败", Toast.LENGTH_SHORT).show()
                                        client.progress = null
                                    }
                                }) {
                                    val md5 = msg.substringAfterLast("md5:")
                                    if (md5 == toolsBox.toHexStr(
                                            MessageDigest.getInstance("MD5").digest(it)
                                        )
                                    ) {
                                        runOnUiThread {
                                            Toast.makeText(this, "下载成功", Toast.LENGTH_SHORT).show()
                                            info.dismiss()
                                        }
                                        val f = File(externalCacheDir, "new.apk")
                                        f.writeBytes(it)
                                        install(f, activity)
                                    } else runOnUiThread {
                                        Toast.makeText(this, "文件损坏", Toast.LENGTH_SHORT).show()
                                        info.dismiss()
                                    }
                                    client.progress = null
                                }
                            }.start()
                        }, { p["skipVersion"] = verNum.toString() })
                    }
                } else runOnUiThread {
                    toolsBox.buildInfo("看板", msg.substringAfter('\n'), "知道了")
                }
            }
        } else if(ignoreSkip) runOnUiThread {
            Toast.makeText(this, "无更新", Toast.LENGTH_SHORT).show()
        }
    }

    private fun install(apkFile: File, activity: Activity) = activity.apply{
        val intent = Intent(Intent.ACTION_VIEW)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val contentUri: Uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", apkFile)
            intent.setDataAndType(contentUri, "application/vnd.android.package-archive")
        } else intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive")
        startActivity(intent)
    }
}