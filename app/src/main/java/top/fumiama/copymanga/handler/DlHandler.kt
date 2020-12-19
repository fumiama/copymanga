package top.fumiama.copymanga.handler

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Message
import android.widget.Toast
import android.widget.ToggleButton
import kotlinx.android.synthetic.main.widget_downloadbar.*
import top.fumiama.copymanga.R
import top.fumiama.copymanga.activity.DlActivity
import top.fumiama.copymanga.tool.MangaDlTools.Companion.wmdlt
import java.lang.ref.WeakReference

class DlHandler(activity: DlActivity) : Handler() {
    private val da = WeakReference(activity)
    private val d = da.get()

    @ExperimentalStdlibApi
    @SuppressLint("SetTextI18n")
    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        when (msg.what) {
            -2 -> d?.setLayouts()
            1 -> {
                d?.tbtnlist?.get(msg.arg1)?.setBackgroundResource(R.drawable.rndbg_checked)
                d?.tbtnlist?.get(msg.arg1)?.isChecked = false
                d?.updateProgressBar()
                if (d?.haveDlStarted == false) {
                    d.dldChapter = 0
                    d.checkedChapter = 0
                    this.postDelayed({
                        d.setProgress2(0, 233)
                        d.tdwn?.text = "0/0"
                    }, 400)
                }
            }
            -1 -> {
                d?.tbtnlist?.get(msg.arg1)?.setBackgroundResource(R.drawable.rndbg_error)
                d!!.dldChapter--
                Toast.makeText(d, "下载${d.tbtnlist[msg.arg1].textOn}失败", Toast.LENGTH_SHORT).show()
                d.updateProgressBar()
            }
            4 -> {
                d?.pdwn?.progress = 0
                val selectDownloaded = d?.multiSelect?:false
                if (d?.haveSElectAll == true) {
                    for (i in d.tbtnlist.listIterator()) {
                        if(i.freezesText) i.setBackgroundResource(R.drawable.rndbg_checked) else i.setBackgroundResource(R.drawable.toggle_button)
                        i.isChecked = false
                    }
                    d.haveSElectAll = false
                    d.checkedChapter = 0
                    d.dldChapter = 0
                } else {
                    d?.let {
                        val checkBtn = { i: ToggleButton, it: DlActivity ->
                            i.setBackgroundResource(R.drawable.toggle_button)
                            i.isChecked = true
                            it.checkedChapter++
                        }
                        for (i in it.tbtnlist.listIterator()) {
                            if(selectDownloaded) checkBtn(i, it)
                            else if(!i.freezesText) checkBtn(i, it)
                        }
                    }
                    d?.haveSElectAll = true
                }
                d?.tdwn?.text = "${d?.dldChapter}/${d?.checkedChapter}"
            }
            5 -> {
                val size = d?.tbtnUrlList?.get(msg.arg1)?.let { wmdlt?.get()?.getImgsCountByHash(it.substringAfterLast("/")) }?:0
                d?.updateProgressBar(msg.arg2, size)
                if (!(msg.obj as Boolean)) {
                    Toast.makeText(d, "下载${d?.tbtnlist?.get(msg.arg1)?.textOn}的第${msg.arg2}页失败", Toast.LENGTH_SHORT).show()
                }else{
                    val progressTxt = d?.tdwn?.text.toString()
                    d?.tdwn?.text = "${progressTxt.substringBefore(" ")} 的${msg.arg2}/${size}页"
                }
            }
            6 -> d?.tdwn?.text = "${d?.dldChapter}/${d?.checkedChapter}"
            7 -> d?.deleteChapters()
            8 -> d?.cdwn?.setCardBackgroundColor(d.resources.getColor(R.color.colorBlue))
            9 -> d?.cdwn?.setCardBackgroundColor(d.resources.getColor(R.color.colorRed))
            10 -> Toast.makeText(d, "下载${d?.tbtnlist?.get(msg.arg1)?.textOn}的第${msg.arg2}页失败，尝试重新下载...", Toast.LENGTH_SHORT).show()
        }
    }
}