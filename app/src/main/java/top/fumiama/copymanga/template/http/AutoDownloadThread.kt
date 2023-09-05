package top.fumiama.copymanga.template.http

import top.fumiama.dmzj.copymanga.R
import top.fumiama.copymanga.MainActivity.Companion.mainWeakReference
import top.fumiama.copymanga.tools.api.CMApi
import top.fumiama.copymanga.tools.http.DownloadTools

class AutoDownloadThread(private val url: String, private val whenFinish: (result: ByteArray?)->Unit): Thread() {
    var exit = false
    override fun run() {
        super.run()
        var re: ByteArray? = null
        var c = 0
        while (!exit && re == null && c++ < 3){
            re = DownloadTools.getHttpContent(url,
                mainWeakReference?.get()?.getString(R.string.referer)!!,
                mainWeakReference?.get()?.getString(R.string.pc_ua)!!
            )
        }
        if(!exit) whenFinish(re)
    }
}