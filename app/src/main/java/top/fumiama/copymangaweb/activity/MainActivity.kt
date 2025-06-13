package top.fumiama.copymangaweb.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebView
import kotlinx.android.synthetic.main.activity_main.*
import top.fumiama.copymangaweb.R
import top.fumiama.copymangaweb.databinding.ActivityMainBinding
import top.fumiama.copymangaweb.handler.MainHandler
import top.fumiama.copymangaweb.tool.SetDraggable
import top.fumiama.copymangaweb.tool.ToolsBox
import top.fumiama.copymangaweb.web.JS
import top.fumiama.copymangaweb.web.JSHidden
import top.fumiama.copymangaweb.web.WebChromeClient
import java.lang.ref.WeakReference

class MainActivity: Activity() {
    var uploadMessageAboveL: ValueCallback<Array<Uri>>? = null
    private var toolsBox: ToolsBox? = null
    @SuppressLint("JavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        wm = WeakReference(this)
        mh = MainHandler(Looper.myLooper()!!)
        toolsBox = ToolsBox(wm as WeakReference<Any>)
        toolsBox?.netinfo?.let {
            if(it == "无网络" || it == "错误") {
                Thread{mh?.sendEmptyMessage(6)}.start()
            }else{
                WebView.setWebContentsDebuggingEnabled(true)
                w.setWebViewClient("i.js")
                w.webChromeClient = WebChromeClient()
                w.loadJSInterface(JS())
                w.loadUrl(getString(R.string.web_home))

                wh.settings.userAgentString = getString(R.string.pc_ua)
                wh.webChromeClient = WebChromeClient()
                wh.setWebViewClient("h.js")
                wh.loadJSInterface(JSHidden())
            }
        }
        SetDraggable().with(this).onto(fab)
    }

    override fun onBackPressed() {
        if(w.canGoBack()) w.goBack()
        else super.onBackPressed()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_CHOOSER_RESULT_CODE) {  //处理返回的图片，并进行上传
            if (uploadMessageAboveL == null) return
            else {
                if(resultCode == RESULT_OK) {
                    data?.apply {
                        if(uploadMessageAboveL != null) {
                            onActivityResultAboveL(requestCode, resultCode, this)
                        }
                    }
                }
            }
        }
    }

    private fun onActivityResultAboveL(requestCode: Int, resultCode: Int, intent: Intent) {
        if (requestCode != FILE_CHOOSER_RESULT_CODE || uploadMessageAboveL == null) return
        else {
            if (resultCode == RESULT_OK) {
                intent.clipData?.apply {
                    var results = arrayOf<Uri>()
                    for(i in 0..itemCount) {
                        val item = getItemAt(i)
                        results += item.uri
                    }
                    intent.dataString?.apply {
                        uploadMessageAboveL?.onReceiveValue(results)
                        uploadMessageAboveL = null
                    }
                }
            }
        }
    }

    fun onFabClicked(v: View){
        DlListActivity.currentDir = getExternalFilesDir("")
        startActivity(
            Intent(this, (if(mh?.showDlList == true) DlListActivity::class else DlActivity::class).java)
                .putExtra("title", "我的下载")
        )
    }

    fun openImageChooserActivity() {
        //调用自己的图库
        val i = Intent(Intent.ACTION_GET_CONTENT)
        i.addCategory(Intent.CATEGORY_OPENABLE)
        i.type = "image/*"
        startActivityForResult(Intent.createChooser(i, "Image Chooser"), FILE_CHOOSER_RESULT_CODE)
    }

    companion object{
        const val FILE_CHOOSER_RESULT_CODE = 1
        var wm: WeakReference<MainActivity>? = null
        var mh: MainHandler? = null
    }
}