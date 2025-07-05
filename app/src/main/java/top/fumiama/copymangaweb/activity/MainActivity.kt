package top.fumiama.copymangaweb.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.fumiama.copymangaweb.BuildConfig
import top.fumiama.copymangaweb.R
import top.fumiama.copymangaweb.activity.template.ToolsBoxActivity
import top.fumiama.copymangaweb.databinding.ActivityMainBinding
import top.fumiama.copymangaweb.handler.MainHandler
import top.fumiama.copymangaweb.tool.SetDraggable
import top.fumiama.copymangaweb.tool.Updater
import top.fumiama.copymangaweb.web.JS
import top.fumiama.copymangaweb.web.JSHidden
import top.fumiama.copymangaweb.web.WebChromeClient
import java.lang.ref.WeakReference

class MainActivity: ToolsBoxActivity() {
    var uploadMessageAboveL: ValueCallback<Array<Uri>>? = null
    lateinit var mBinding: ActivityMainBinding

    @SuppressLint("JavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        wm = WeakReference(this)
        mh = MainHandler(Looper.myLooper()!!)
        toolsBox.netInfo.let {
            if(it == "无网络" || it == "错误") {
                mh?.sendEmptyMessage(MainHandler.SET_FAB_TO_DOWNLOAD_LIST)
                return@let
            }

            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    goCheckUpdate(false)
                }
            }

            WebView.setWebContentsDebuggingEnabled(true)
            mBinding.w.apply { post {
                setWebViewClient("i.js")
                webChromeClient = WebChromeClient()
                loadJSInterface(JS())
                loadUrl(getString(R.string.web_home))
            } }

            mBinding.wh.apply { post {
                settings.userAgentString = getString(R.string.pc_ua)
                webChromeClient = WebChromeClient()
                setWebViewClient("h.js")
                loadJSInterface(JSHidden())
            } }
        }
        SetDraggable().with(this).onto(mBinding.fab)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if(mBinding.w.canGoBack()) mBinding.w.goBack()
        else super.onBackPressed()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_CHOOSER_RESULT_CODE) {  //处理返回的图片，并进行上传
            if (uploadMessageAboveL == null || resultCode != RESULT_OK) return
            data?.let {
                onActivityResultAboveL(requestCode, resultCode, it)
            }
        }
    }

    private fun onActivityResultAboveL(requestCode: Int, resultCode: Int, intent: Intent) {
        if (requestCode != FILE_CHOOSER_RESULT_CODE ||
            uploadMessageAboveL == null ||
            resultCode != RESULT_OK
        ) return
        intent.clipData?.let { clipData ->
            var results = arrayOf<Uri>()
            for (i in 0..clipData.itemCount) {
                val item = clipData.getItemAt(i)
                results += item.uri
            }
            if (intent.dataString != null) {
                uploadMessageAboveL?.onReceiveValue(results)
                uploadMessageAboveL = null
            }
        }
    }

    private suspend fun goCheckUpdate(ignoreSkip: Boolean) {
        Updater(
            WeakReference(this),
            toolsBox,
            ignoreSkip,
            getPreferences(MODE_PRIVATE).getInt("skipVersion", 0)
        ).check(BuildConfig.VERSION_CODE)
    }

    fun onFabClicked(v: View){
        DlListActivity.currentDir = getExternalFilesDir("")
        startActivity(
            Intent(this, (if(mh?.showDlList == true) DlListActivity::class else DlActivity::class).java)
                .putExtra("title", "我的下载")
        )
    }

    fun openImageChooserActivity() {
        // 调用自己的图库
        startActivityForResult(
            Intent.createChooser(
                Intent(Intent.ACTION_GET_CONTENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("image/*"), "Image Chooser"
            ), FILE_CHOOSER_RESULT_CODE
        )
    }

    companion object {
        const val FILE_CHOOSER_RESULT_CODE = 1
        var wm: WeakReference<MainActivity>? = null
        var mh: MainHandler? = null
    }
}