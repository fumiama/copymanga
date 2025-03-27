package top.fumiama.copymanga.view.interaction

import android.os.Handler

class TimeThread(private val handler: Handler, private val msg: Int, private val interval: Long = 3000) : Thread() {
    var canDo = false
    override fun run() {
        while (canDo) {
            try {
                handler.sendEmptyMessage(msg)
                sleep(interval)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }
}