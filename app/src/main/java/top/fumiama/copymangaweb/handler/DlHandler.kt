package top.fumiama.copymangaweb.handler

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.widget.Toast
import android.widget.ToggleButton
import top.fumiama.copymangaweb.R
import top.fumiama.copymangaweb.activity.DlActivity
import top.fumiama.copymangaweb.tool.MangaDlTools.Companion.wmdlt
import java.lang.ref.WeakReference

class DlHandler(activity: DlActivity, looper: Looper) : Handler(looper) {
    private val da = WeakReference(activity)
    private val d get() = da.get()
    private var size = 0
    private var refreshSize = true

    @SuppressLint("SetTextI18n")
    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        when (msg.what) {
            -2 -> d?.setLayouts()
            1 -> {
                d?.tbtnlist?.get(msg.arg1)?.apply { post {
                    setBackgroundResource(R.drawable.rndbg_checked)
                    isChecked = false
                    d?.updateProgressBar()
                    if (d?.haveDlStarted == false) {
                        d?.dldChapter = 0
                        d?.checkedChapter = 0
                        postDelayed({
                            d?.setProgress2(0, 233)
                            d?.mBinding?.dldlbar?.tdwn?.apply { post {
                                text = d?.getString(R.string.zero_per_zero)
                            } }
                        }, 400)
                    }
                } }
            }
            -1 -> {
                d?.tbtnlist?.get(msg.arg1)?.apply { post {
                    setBackgroundResource(R.drawable.rndbg_error)
                    d!!.dldChapter--
                    Toast.makeText(d, "下载${d?.tbtnlist?.get(msg.arg1)?.textOn}失败", Toast.LENGTH_SHORT).show()
                    d?.updateProgressBar()
                } }
            }
            4 -> {
                d?.mBinding?.dldlbar?.pdwn?.apply { post { progress = 0 } }
                val selectDownloaded = d?.multiSelect?:false
                if (d?.haveSElectAll == true) {
                    d?.tbtnlist?.forEach { i ->
                        i.apply { post {
                            if(freezesText) setBackgroundResource(R.drawable.rndbg_checked)
                            else setBackgroundResource(R.drawable.toggle_button)
                            isChecked = false
                        } }
                    }
                    d?.haveSElectAll = false
                    d?.checkedChapter = 0
                    d?.dldChapter = 0
                } else {
                    d?.let {
                        val checkBtn = { i: ToggleButton, a: DlActivity ->
                            i.apply { post {
                                setBackgroundResource(R.drawable.toggle_button)
                                isChecked = true
                                a.checkedChapter++
                            } }
                        }
                        for (i in it.tbtnlist) {
                            if(selectDownloaded) checkBtn(i, it)
                            else if(!i.freezesText) checkBtn(i, it)
                        }
                    }
                    d?.haveSElectAll = true
                }
                d?.mBinding?.dldlbar?.tdwn?.apply { post {
                    text = "${d?.dldChapter}/${d?.checkedChapter}"
                } }
            }
            5 -> {
                setSize(msg.arg2, msg.arg1)
                d?.updateProgressBar(msg.arg2, size)
                if (!(msg.obj as Boolean)) {
                    Toast.makeText(d, "下载${d?.tbtnlist?.get(msg.arg1)?.textOn}的第${msg.arg2}页失败", Toast.LENGTH_SHORT).show()
                }else{
                    val progressTxt = d?.mBinding?.dldlbar?.tdwn?.text.toString()
                    d?.mBinding?.dldlbar?.tdwn?.apply { post {
                        text = "${progressTxt.substringBefore(' ')} 的 ${msg.arg2}/${size} 页"
                    } }
                }
            }
            6 -> d?.mBinding?.dldlbar?.tdwn?.apply { post { text = "${d?.dldChapter}/${d?.checkedChapter}" } }
            7 -> d?.deleteChapters()
            8 -> d?.resources?.getColor(R.color.colorBlue)?.let { d?.mBinding?.dldlbar?.cdwn?.apply { post {
                setCardBackgroundColor(it)
            } } }
            9 -> d?.resources?.getColor(R.color.colorRed)?.let { d?.mBinding?.dldlbar?.cdwn?.apply { post {
                setCardBackgroundColor(it)
            } } }
            10 -> Toast.makeText(d, "下载${d?.tbtnlist?.get(msg.arg1)?.textOn}的第${msg.arg2}页失败，尝试重新下载...", Toast.LENGTH_SHORT).show()
        }
    }
    private fun setSize(pageNow: Int, tbtnNo: Int){
        if(refreshSize || size == 0) {
            size = d?.tbtnlist?.get(tbtnNo)?.hash?.let { wmdlt?.get()?.getImgsCountByHash(it) }?:0
            refreshSize = false
        }else if(pageNow == size) refreshSize = true
    }
}