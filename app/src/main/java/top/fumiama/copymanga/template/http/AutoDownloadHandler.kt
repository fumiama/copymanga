package top.fumiama.copymanga.template.http

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import com.google.gson.Gson
import top.fumiama.dmzj.copymanga.R
import top.fumiama.copymanga.MainActivity.Companion.mainWeakReference
import top.fumiama.copymanga.json.ReturnBase
import top.fumiama.copymanga.tools.http.DownloadTools
import top.fumiama.copymanga.tools.thread.TimeThread

open class AutoDownloadHandler(private val url: String, private val jsonClass: Class<*>, looper: Looper, private val callCheckMsg: Int = -1): Handler(looper) {
    var exit = false
    private var timeThread: TimeThread? = null
    private var checkTimes = 0
    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        when(msg.what){
            callCheckMsg -> check()
            0 -> setLayouts()
        }
    }
    open fun setGsonItem(gsonObj: Any) {}
    open fun getGsonItem(): ReturnBase? = null
    open fun onError() {}
    open fun doWhenFinishDownload() {}
    fun startLoad() {
        sendEmptyMessage(0)
    }
    fun destroy() {
        exit = true
    }
    private fun download(){
        Thread{
            DownloadTools.getHttpContent(url,
                mainWeakReference?.get()?.getString(R.string.referUrl)!!,
                mainWeakReference?.get()?.getString(R.string.pc_ua)!!
            )?.let {
                if(exit) return@Thread
                val fi = it.inputStream()
                setGsonItem(Gson().fromJson(fi.reader(), jsonClass))
                fi.close()
            }
        }.start()
        checkTimes = 0
        timeThread = TimeThread(this, callCheckMsg)
        timeThread?.canDo = true
        timeThread?.start()
    }
    private fun check(){
        val g = getGsonItem()
        if(g != null) {
            timeThread?.canDo = false
            if(g.code == 200) sendEmptyMessage(0)
            else onError()
            Log.d("MyADH", "[${g.code}]${g.message}")
        } else if(checkTimes++ > 10) timeThread?.canDo = false
    }
    private fun setLayouts() {
        if(getGsonItem() == null) download()
        else doWhenFinishDownload()
    }
}