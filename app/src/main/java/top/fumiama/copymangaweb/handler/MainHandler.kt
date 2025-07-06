package top.fumiama.copymangaweb.handler

import android.app.Dialog
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.widget.TextView
import top.fumiama.copymangaweb.R
import top.fumiama.copymangaweb.activity.MainActivity.Companion.wm

class MainHandler(looper: Looper): Handler(looper) {
    private var dialog: Dialog? = null

    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        when(msg.what) {
            SHOW_LOADING_DIALOG -> {
                wm?.get()?.apply {
                    (dialog?:Dialog(this).also {
                        it.setContentView(R.layout.dialog_unzipping)
                        dialog = it
                    }).show()
                }
            }
            HIDE_LOADING_DIALOG -> {
                dialog?.dismiss()
                dialog = null
            }
            SET_LOADING_DIALOG_TEXT -> {
                val t = msg.obj as? String?:return
                dialog?.findViewById<TextView>(R.id.tunz)?.apply { post {
                    text = t
                } }
            }
        }
    }
    companion object {
        const val SHOW_LOADING_DIALOG = 7
        const val HIDE_LOADING_DIALOG = 8
        const val SET_LOADING_DIALOG_TEXT = 9
    }
}