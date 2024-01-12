package top.fumiama.copymanga.template.http

import android.util.Log
import top.fumiama.copymanga.MainActivity.Companion.mainWeakReference
import top.fumiama.copymanga.tools.http.DownloadTools
import top.fumiama.dmzj.copymanga.R
import kotlin.random.Random

class AutoDownloadThread(private val url: String, private val waitMilliseconds: Long = 0, private val whenFinish: (result: ByteArray?)->Unit): Thread() {
    var exit = false
    override fun run() {
        super.run()
        var re: ByteArray? = null
        var c = 0
        while (!exit && re == null && c++ < 3) {
            if (waitMilliseconds > 0) sleep(200+Random.nextLong(waitMilliseconds))
            re = DownloadTools.getHttpContent(url,
                mainWeakReference?.get()?.getString(R.string.referer)!!,
                mainWeakReference?.get()?.getString(R.string.pc_ua)!!
            )
        }
        if(!exit) whenFinish(re)
        Log.d("MyADT", "found exit = $exit")
    }
}