package top.fumiama.copymanga.ui.download

import android.os.Handler
import android.os.Looper
import android.os.Message
import java.io.File
import java.lang.ref.WeakReference

class DlLHandler(looper: Looper, dl: DownloadFragment): Handler(looper) {
    private val dll = WeakReference(dl)
    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        when(msg.what){
            1 -> dll.get()?.checkDir(msg.obj as File)
            2 -> dll.get()?.rmrf(msg.obj as File)
            3 -> dll.get()?.scanFile(msg.obj as File)
        }
    }
}