package top.fumiama.copymanga.handler

import android.os.Handler
import android.os.Looper
import android.os.Message
import top.fumiama.copymanga.activity.DlListActivity
import java.io.File
import java.lang.ref.WeakReference


class DlLHandler(looper: Looper, activity: DlListActivity): Handler(looper) {
    private val dll = WeakReference(activity)
    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        when(msg.what){
            1 -> dll.get()?.checkDir(msg.obj as File)
            2 -> dll.get()?.rmrf(msg.obj as File)
            3 -> dll.get()?.scanFile(msg.obj as File)
        }
    }
}