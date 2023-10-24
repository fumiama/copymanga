package top.fumiama.copymanga.ui.vm

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.View
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_viewmanga.*
import kotlinx.android.synthetic.main.widget_infodrawer.*
import kotlinx.android.synthetic.main.widget_infodrawer.view.*
import top.fumiama.dmzj.copymanga.R
import top.fumiama.copymanga.json.Chapter2Return
import top.fumiama.copymanga.json.ChapterWithContent
import top.fumiama.copymanga.json.ComicStructure
import top.fumiama.copymanga.template.http.AutoDownloadHandler
import top.fumiama.copymanga.ui.vm.ViewMangaActivity.Companion.comicName
import top.fumiama.copymanga.ui.vm.ViewMangaActivity.Companion.pn
import top.fumiama.copymanga.views.ScaleImageView
import java.io.File
import java.lang.Exception
import java.lang.Thread.sleep
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*

class VMHandler(activity: ViewMangaActivity, url: String) : AutoDownloadHandler(
    url, Chapter2Return::class.java, Looper.myLooper()!!
) {
    var manga: Chapter2Return? = null
    private val wv = WeakReference(activity)
    private val infcard = wv.get()?.infcard
    private var infcShowed = false
    val dl = wv.get()?.let {
        val re = Dialog(it)
        re.setContentView(R.layout.dialog_unzipping)
        re
    }
    private var delta = -1f
        get() {
            if (field < 0) field = wv.get()?.infoDrawerDelta ?: 0f
            return field
        }
    private val week: String
        get() {
            val cal = Calendar.getInstance()
            return when (cal[Calendar.DAY_OF_WEEK]) {
                1 -> "周日"
                2 -> "周一"
                3 -> "周二"
                4 -> "周三"
                5 -> "周四"
                6 -> "周五"
                7 -> "周六"
                else -> ""
            }
        }

    @SuppressLint("SimpleDateFormat", "SetTextI18n")
    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        when (msg.what) {
            1 -> if (infcShowed) {
                hideInfCard(); infcShowed = false
            }
            2 -> if (!infcShowed) {
                showInfCard(); infcShowed = true
            }
            3 -> infcShowed = if (infcShowed) {
                hideInfCard(); false
            } else {
                showInfCard(); true
            }
            4 -> {
                val simg = msg.obj as ScaleImageView
                wv.get()?.loadImgOn(simg, msg.arg1, msg.arg2)
                //simg.setHeight2FitImgWidth()
                //if(msg.arg2 == 1) sendEmptyMessage(8)
            }
            5 -> wv.get()?.clearImgOn(msg.obj as ScaleImageView)
            6 -> wv.get()?.prepareLastPage(msg.arg1, msg.arg2)
            7 -> dl?.show()
            8 -> Thread{
                sleep(233)
                sendEmptyMessage(13)
            }.start()
            9 -> loadScrollMode(msg.arg1)
            10 -> loadScrollMode()
            11 -> loadImagesIntoLine(msg.arg1)
            12 -> loadImagesIntoLine()
            13 -> {
                dl?.hide()
                wv.get()?.restorePN()
            }
            14 -> {
                val item = (pn - 1) / (wv.get()?.verticalLoadMaxCount?:20) * (wv.get()?.verticalLoadMaxCount?:20)
                loadScrollMode(item)
                Log.d("MyVMH", "Load page from $item")
            }
            15 -> dl?.hide()
            16 -> if (infcShowed) {
                hideInfCardFull(); infcShowed = false
            }
            17 -> if (!infcShowed) {
                showInfCardFull(); infcShowed = true
            }
            18 -> infcShowed = if (infcShowed) {
                hideInfCardFull(); false
            } else {
                showInfCardFull(); true
            }
            22 -> wv.get()?.idtime?.text = SimpleDateFormat("HH:mm").format(Date()) + week + wv.get()?.toolsBox?.netinfo
        }
    }
    override fun getGsonItem() = manga
    override fun setGsonItem(gsonObj: Any): Boolean {
        super.setGsonItem(gsonObj)
        val m = gsonObj as Chapter2Return
        if(m.results.chapter.words.size != m.results.chapter.size) {
            return false
        }
        manga = m
        return true
    }
    override fun onError() {
        super.onError()
        if(exit) return
        wv.get()?.toolsBox?.toastError("下载章节信息失败")
    }

    override fun doWhenFinishDownload() {
        super.doWhenFinishDownload()
        if(exit) return
        prepareManga()
    }

    fun loadFromFile(file: File): Boolean {
        return try {
            val jsonFile = File(file.parentFile, "${file.nameWithoutExtension}.json")
            if(jsonFile.exists()) {
                manga = Gson().fromJson(jsonFile.reader(), Chapter2Return::class.java)
                prepareManga()
            }
            else{
                manga = Chapter2Return()
                manga?.let {
                    it.results = Chapter2Return.Results()
                    it.results.comic = ComicStructure()
                    it.results.comic.name = file.parentFile?.name
                    it.results.chapter = ChapterWithContent()
                    it.results.chapter.name = file.nameWithoutExtension
                    wv.get()?.countZipEntries { c ->
                        it.results.chapter.size = c
                        prepareManga()
                    }
                }
            }
            true
        }catch (e: Exception){
            e.printStackTrace()
            //wv.get()?.toolsBox?.toastError("读取本地章节信息失败")
            false
        }
    }

    private fun prepareManga(){
        if(comicName == null) {
            comicName = manga?.results?.comic?.name
        }
        wv.get()?.count = manga?.results?.chapter?.size?:0
        wv.get()?.initManga()
        wv.get()?.vprog?.visibility = View.GONE
    }
    private fun loadImagesIntoLine(item: Int = (wv.get()?.currentItem?:0), maxCount: Int = (wv.get()?.verticalLoadMaxCount?:20)) /*= Thread*/{
        Log.d("MyVMH", "Fun: loadImagesIntoLine($item, $maxCount)")
        wv.get()?.realCount?.let { count ->
            if(count > 0){
                val notFull = item + maxCount > count
                val loadCount = (if(notFull) count - item else maxCount) - 1
                Log.d("MyVMH", "count: $count, loadCount: $loadCount, notFull: $notFull")
                if(loadCount >= 0) for(i in 0..loadCount) obtainMessage(4,item + i, if(i == loadCount - 1) 1 else 0, wv.get()?.scrollImages?.get(i)).sendToTarget()
                else sendEmptyMessage(8)
                if(notFull) obtainMessage(6, loadCount + 1, maxCount).sendToTarget()
                wv.get()?.updateSeekBar()
            }
        }
    }//.start()

    private fun loadScrollMode() {
        sendEmptyMessage(7)
        //sleep(233)
        sendEmptyMessage(12)
    }

    private fun loadScrollMode(item: Int) {
        sendEmptyMessage(7)
        //sleep(233)
        Log.d("MyVMH", "loadImgsIntoLine($item)")
        obtainMessage(11, item, 0).sendToTarget()
    }

    private fun showInfCard() {
        Log.d("MyVMH", "Read info drawer delta: $delta")
        ObjectAnimator.ofFloat(infcard?.idc, "alpha", 0.3F, 0.8F).setDuration(233).start()
        ObjectAnimator.ofFloat(infcard, "translationY", delta, 0F).setDuration(233).start()
    }

    private fun showInfCardFull() {
        Log.d("MyVMH", "Read info drawer delta: $delta")
        ObjectAnimator.ofFloat(infcard?.idc, "alpha", 0.0F, 0.8F).setDuration(233).start()
        ObjectAnimator.ofFloat(infcard, "translationY", delta, 0F).setDuration(233).start()
    }

    private fun hideInfCard() {
        ObjectAnimator.ofFloat(infcard?.idc, "alpha", 0.8F, 0.3F).setDuration(233).start()
        ObjectAnimator.ofFloat(infcard, "translationY", 0F, delta).setDuration(233).start()
    }
    private fun hideInfCardFull() {
        ObjectAnimator.ofFloat(infcard?.idc, "alpha", 0.8F, 0.0F).setDuration(233).start()
        ObjectAnimator.ofFloat(infcard, "translationY", 0F, delta).setDuration(233).start()
    }
}