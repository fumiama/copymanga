package top.fumiama.copymanga.template

import top.fumiama.dmzj.copymanga.R
import top.fumiama.copymanga.MainActivity.Companion.mainWeakReference
import top.fumiama.copymanga.tools.DownloadTools

class AutoDownloadThread(private val url: String, private val whenFinish: (result: ByteArray?)->Unit): Thread() {
    override fun run() {
        super.run()
        var re: ByteArray? = null
        var c = 0
        while (re == null && c++ < 3){
            re = DownloadTools.getHttpContent(url,
                mainWeakReference?.get()?.getString(R.string.referUrl)!!,
                mainWeakReference?.get()?.getString(R.string.pc_ua)!!
            )
        }
        whenFinish(re)
    }
}