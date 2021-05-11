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
import top.fumiama.copymanga.tools.file.PropertiesTools
import top.fumiama.copymanga.ui.vm.ViewMangaActivity.Companion.comicName
import top.fumiama.copymanga.ui.vm.ViewMangaActivity.Companion.pn
import top.fumiama.copymanga.views.ScaleImageView
import java.io.File
import java.lang.Exception
import java.lang.Thread.sleep
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipInputStream

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
    var progressLog: PropertiesTools? = null

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
                wv.get()?.loadImgOn(simg, msg.arg1)
                simg.setHeight2FitImgWidth()
                if(msg.arg2 == 1) sendEmptyMessage(8)
            }
            5 -> wv.get()?.clearImgOn(msg.obj as ScaleImageView)
            6 -> wv.get()?.prepareLastPage(msg.arg1, msg.arg2)
            7 -> dl?.show()
            8 -> Thread{
                    sleep(233)
                    sendEmptyMessage(13)
                }.start()
            9 -> loadThread(msg.arg1)
            10 -> loadThread()
            11 -> loadImgsIntoLine(msg.arg1)
            12 -> loadImgsIntoLine()
            13 -> {
                dl?.hide()
                wv.get()?.restorePN()
            }
            14 -> {
                val item = (pn - 1) / (wv.get()?.verticalLoadMaxCount?:40) * (wv.get()?.verticalLoadMaxCount?:40)
                loadThread(item)
                Log.d("MyVMH", "Load page from $item")
            }
            22 -> wv.get()?.idtime?.text = SimpleDateFormat("HH:mm").format(Date()) + week + wv.get()?.toolsBox?.netinfo
        }
    }

    override fun getGsonItem() = manga
    override fun setGsonItem(gsonObj: Any) {
        super.setGsonItem(gsonObj)
        manga = gsonObj as Chapter2Return
    }
    override fun onError() {
        super.onError()
        if(exit) return
        wv.get()?.toolsBox?.toastError("下载章节信息失败")
    }

    @ExperimentalStdlibApi
    override fun doWhenFinishDownload() {
        super.doWhenFinishDownload()
        if(exit) return
        prepareManga()
    }

    @ExperimentalStdlibApi
    fun loadFromFile(file: File): Boolean {
        return try {
            val jsonFile = File(file.parentFile, "${file.nameWithoutExtension}.json")
            if(jsonFile.exists()) manga = Gson().fromJson(jsonFile.reader(), Chapter2Return::class.java)
            else{
                manga = Chapter2Return()
                manga?.let {
                    it.results = Chapter2Return.Results()
                    it.results.comic = ComicStructure()
                    it.results.comic.name = file.parentFile?.name
                    it.results.chapter = ChapterWithContent()
                    it.results.chapter.name = file.nameWithoutExtension
                    it.results.chapter.size = countZipEntries(file)
                }
            }
            prepareManga()
            true
        }catch (e: Exception){
            e.printStackTrace()
            //wv.get()?.toolsBox?.toastError("读取本地章节信息失败")
            false
        }
    }

    private fun countZipEntries(file: File): Int{
        var count = 0
        try {
            val zip = ZipInputStream(file.inputStream().buffered())
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) count++
                entry = zip.nextEntry
            }
            zip.closeEntry()
            zip.close()
        } catch (e: Exception) {
            wv.get()?.toolsBox?.toastError("统计zip图片数错误!")
        }
        return count
    }

    @ExperimentalStdlibApi
    private fun prepareManga(){
        comicName = manga?.results?.comic?.name
        progressLog = PropertiesTools(File("${wv.get()?.filesDir}/progress/${manga?.results?.comic?.name}"))
        wv.get()?.count = manga?.results?.chapter?.size?:0
        wv.get()?.initManga()
        wv.get()?.vprog?.visibility = View.GONE
    }
    private fun loadImgsIntoLine(item: Int = (wv.get()?.currentItem?:0), maxCount: Int = (wv.get()?.verticalLoadMaxCount?:40)){
        Log.d("MyVMH", "Fun: loadImgsIntoLine($item)")
        val count = wv.get()?.count?.minus(1)?:0
        val notFull = item + maxCount > count
        val loadCount = (if(notFull) count - item else maxCount) - 1
        Log.d("MyVMH", "loadCount: $loadCount")
        if(loadCount >= 0) for(i in 0..loadCount) obtainMessage(4,item + i, if(i == loadCount - 1)1 else 0, wv.get()?.scrollImages?.get(i)).sendToTarget()
        else sendEmptyMessage(8)
        if(notFull) obtainMessage(6, loadCount + 1, maxCount).sendToTarget()
    }

    private fun loadThread() = Thread{
        sendEmptyMessage(7)
        //sleep(233)
        sendEmptyMessage(12)
    }.start()

    private fun loadThread(item: Int) = Thread{
        sendEmptyMessage(7)
        //sleep(233)
        Log.d("MyVMH", "loadImgsIntoLine($item)")
        obtainMessage(11, item, 0).sendToTarget()
    }.start()

    private fun showInfCard() {
        Log.d("MyVMH", "Read info drawer delta: $delta")
        ObjectAnimator.ofFloat(infcard?.idc, "alpha", 0.3F, 0.8F).setDuration(233).start()
        ObjectAnimator.ofFloat(infcard, "translationY", delta, 0F).setDuration(233).start()
    }

    private fun hideInfCard() {
        ObjectAnimator.ofFloat(infcard?.idc, "alpha", 0.8F, 0.3F).setDuration(233).start()
        ObjectAnimator.ofFloat(infcard, "translationY", 0F, delta).setDuration(233).start()
    }
}