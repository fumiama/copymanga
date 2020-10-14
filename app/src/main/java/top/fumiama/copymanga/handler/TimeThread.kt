package top.fumiama.copymanga.handler

import android.os.Handler

class TimeThread(private val handler: Handler, private val msg: Int) : Thread() {
    var canDo = false
    override fun run() {
        while (canDo) {
            try {
                handler.sendEmptyMessage(msg)
                sleep(3000)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }
}