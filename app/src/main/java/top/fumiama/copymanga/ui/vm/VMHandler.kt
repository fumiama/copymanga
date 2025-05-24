package top.fumiama.copymanga.ui.vm

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Message
import android.util.Log
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_viewmanga.*
import kotlinx.android.synthetic.main.dialog_unzipping.*
import kotlinx.android.synthetic.main.widget_infodrawer.*
import kotlinx.android.synthetic.main.widget_infodrawer.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import top.fumiama.copymanga.MainActivity
import top.fumiama.copymanga.json.Chapter2Return
import top.fumiama.copymanga.json.ChapterWithContent
import top.fumiama.copymanga.json.ComicStructure
import top.fumiama.copymanga.net.template.AutoDownloadHandler
import top.fumiama.copymanga.net.template.PausableDownloader
import top.fumiama.copymanga.view.ScaleImageView
import top.fumiama.dmzj.copymanga.R
import java.io.File
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*

class VMHandler(activity: ViewMangaActivity, private val chapterUrl: String, private val weeks: Array<String>) : AutoDownloadHandler(
    { chapterUrl }, Chapter2Return::class.java, activity
) {
    var manga: Chapter2Return? = null
    private val wv = WeakReference(activity)
    private val drawer = wv.get()?.infcard
    private var hasDrawerShown = false
    val dl = activity.let {
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
            val w = cal[Calendar.DAY_OF_WEEK]
            if (w > 7 || w <= 0) return ""
            return weeks[w-1]
        }
    private var remainingImageCount = 0

    @SuppressLint("SimpleDateFormat", "SetTextI18n")
    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        when (msg.what) {
            HIDE_INFO_CARD -> if (hasDrawerShown) {
                hideInfCard(); hasDrawerShown = false
            }
            SHOW_INFO_CARD -> if (!hasDrawerShown) {
                showInfCard(); hasDrawerShown = true
            }
            TRIGGER_INFO_CARD -> hasDrawerShown = if (hasDrawerShown) {
                hideInfCard(); false
            } else {
                showInfCard(); true
            }
            /*LOAD_IMG_ON -> {
                val scaleImageView = msg.obj as ScaleImageView
                // msg.arg2: isLast
                wv.get()?.apply {
                    lifecycleScope.launch {
                        loadImgOn(scaleImageView, msg.arg1)
                    }
                }
                //scaleImageView.setHeight2FitImgWidth()
                //if(msg.arg2 == 1) sendEmptyMessage(DELAYED_RESTORE_PAGE_NUMBER)
            }*/
            CLEAR_IMG_ON -> {
                (msg.obj as ScaleImageView).apply { post { visibility = View.GONE } }
                //sendEmptyMessage(DECREASE_IMAGE_COUNT_AND_RESTORE_PAGE_NUMBER_AT_ZERO)
            }
            PREPARE_LAST_PAGE -> wv.get()?.prepareLastPage(msg.arg1, msg.arg2)
            DIALOG_SHOW -> dl.show()

            LOAD_ITEM_SCROLL_MODE -> loadScrollMode(msg.arg1, msg.obj as? Runnable?)
            LOAD_SCROLL_MODE -> loadScrollMode()
            LOAD_ITEM_IMAGES_INTO_LINE -> wv.get()?.lifecycleScope?.launch { loadImagesIntoLine(msg.arg1, msg.obj as? Runnable?) }
            LOAD_IMAGES_INTO_LINE -> wv.get()?.lifecycleScope?.launch { loadImagesIntoLine() }
            RESTORE_PAGE_NUMBER -> {
                sendEmptyMessage(DIALOG_HIDE)
                wv.get()?.apply { lifecycleScope.launch { restorePN() } }
            }
            LOAD_PAGE_FROM_ITEM -> {
                val verticalMaxCount = wv.get()?.verticalLoadMaxCount?:20
                val item = ((wv.get()?.pn?:1) - 1) / verticalMaxCount * verticalMaxCount
                loadScrollMode(item)
                Log.d("MyVMH", "Load page from $item")
            }
            DIALOG_HIDE -> dl.hide()
            HIDE_INFO_CARD_FULL -> if (hasDrawerShown) {
                hideInfCardFull(); hasDrawerShown = false
            }
            SHOW_INFO_CARD_FULL -> if (!hasDrawerShown) {
                showInfCardFull(); hasDrawerShown = true
            }
            TRIGGER_INFO_CARD_FULL -> hasDrawerShown = if (hasDrawerShown) {
                hideInfCardFull(); false
            } else {
                showInfCardFull(); true
            }
            INIT_IMAGE_COUNT -> {
                remainingImageCount = msg.arg1
                Log.d("MyVMH", "init remainingImageCount = $remainingImageCount")
            }
            DECREASE_IMAGE_COUNT_AND_RESTORE_PAGE_NUMBER_AT_ZERO -> {
                if (--remainingImageCount == 0) {
                    Log.d("MyVMH", "last load page, restore pn...")
                    sendEmptyMessageDelayed(RESTORE_PAGE_NUMBER, 233)
                }
                Log.d("MyVMH", "remainingImageCount = $remainingImageCount")
            }
            DO_LAMBDA -> (msg.obj as? Runnable?)?.run()
            SET_NET_INFO -> wv.get()?.idtime?.text = SimpleDateFormat("HH:mm").format(Date()) + week + wv.get()?.toolsBox?.netInfo
            SET_DL_TEXT -> dl.tunz.text = msg.obj as String
        }
    }
    override fun getGsonItem() = manga
    override fun setGsonItem(gsonObj: Any): Boolean {
        super.setGsonItem(gsonObj)
        val m = gsonObj as Chapter2Return
        if (m.results.chapter.words != null) {
            if(m.results.chapter.words.size != m.results.chapter.contents.size) {
                return false
            }
            if(m.results.chapter.words.size != m.results.chapter.size) {
                m.results.chapter.size = m.results.chapter.words.size // 有时 size 不对
            }
        }
        manga = m
        return true
    }
    override suspend fun onError() {
        super.onError()
        if(exit) return
        withContext(Dispatchers.Main) {
            wv.get()?.toolsBox?.toastError(R.string.download_chapter_info_failed)
        }
    }

    override suspend fun doWhenFinishDownload() {
        super.doWhenFinishDownload()
        if(exit) return
        prepareManga()
    }

    suspend fun loadFromFile(file: File): Boolean {
        fakeLoad()
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
                    it.results.comic.name = file.parentFile?.parentFile?.name
                    it.results.chapter = ChapterWithContent()
                    it.results.chapter.name = file.nameWithoutExtension
                    wv.get()?.apply {
                        it.results.chapter.uuid = uuidArray[position]
                        countZipEntries { c ->
                            it.results.chapter.size = c
                            prepareManga()
                        }
                    }
                }
            }
            true
        } catch (e: Exception){
            e.printStackTrace()
            wv.get()?.toolsBox?.toastError(R.string.load_local_chapter_info_failed)
            false
        }
    }

    private fun fakeLoad() {
        if (MainActivity.member?.hasLogin == true) Thread {
            runBlocking { PausableDownloader(chapterUrl) { _ -> }.run() }
        }.start()
    }

    private suspend fun prepareManga() = withContext(Dispatchers.Main) {
        wv.get()?.apply {
            if(comicName == null) {
                comicName = manga?.results?.comic?.name
            }
            count = manga?.results?.chapter?.size?:0
            initManga()
            vprog?.visibility = View.GONE
        }
    }
    private suspend fun loadImagesIntoLine(item: Int = (wv.get()?.currentItem?:0), doAfter: Runnable? = null) {
        val maxCount: Int = (wv.get()?.verticalLoadMaxCount?:20)
        Log.d("MyVMH", "Fun: loadImagesIntoLine($item, $maxCount)")
        wv.get()?.realCount?.let { count ->
            if(count > 0){
                val notFull = item + maxCount > count
                val loadCount = (if(notFull) count - item else maxCount) - 1
                obtainMessage(INIT_IMAGE_COUNT, loadCount+1, 0).sendToTarget()
                Log.d("MyVMH", "count: $count, loadCount: $loadCount, notFull: $notFull")
                if(loadCount >= 0) withContext(Dispatchers.IO) {
                    for(i in 0..loadCount) {
                        wv.get()?.apply {
                            val p = item + i
                            scrollPositions[i] = p
                            launch { loadImgOn(scrollImages[i], scrollButtons[i], p, false) }
                        }
                        //obtainMessage(LOAD_IMG_ON,item + i, if(i == loadCount - 1) 1 else 0, wv.get()?.scrollImages?.get(i)).sendToTarget()
                    }
                }
                //else sendEmptyMessageDelayed(RESTORE_PAGE_NUMBER, 233)
                if(notFull) obtainMessage(PREPARE_LAST_PAGE, loadCount + 1, maxCount).sendToTarget()
                obtainMessage(DO_LAMBDA, Runnable{
                    doAfter?.run()
                    wv.get()?.apply { lifecycleScope.launch { updateSeekBar(0) } }
                }).sendToTarget()
            }
        }
    }

    private fun loadScrollMode() {
        sendEmptyMessage(DIALOG_SHOW)
        //sleep(233)
        sendEmptyMessage(LOAD_IMAGES_INTO_LINE)
    }

    private fun loadScrollMode(item: Int, doAfter: Runnable? = null) {
        sendEmptyMessage(DIALOG_SHOW)
        //sleep(233)
        Log.d("MyVMH", "loadImgsIntoLine($item)")
        obtainMessage(LOAD_ITEM_IMAGES_INTO_LINE, item, 0, doAfter).sendToTarget()
    }

    private fun showInfCard() {
        Log.d("MyVMH", "Read info drawer delta: $delta")
        ObjectAnimator.ofFloat(drawer?.idc, "alpha", 0.3F, 0.8F).setDuration(233).start()
        ObjectAnimator.ofFloat(drawer, "translationY", delta, 0F).setDuration(233).start()
    }

    private fun showInfCardFull() {
        Log.d("MyVMH", "Read info drawer delta: $delta")
        ObjectAnimator.ofFloat(drawer?.idc, "alpha", 0.0F, 0.8F).setDuration(233).start()
        ObjectAnimator.ofFloat(drawer, "translationY", delta, 0F).setDuration(233).start()
    }

    private fun hideInfCard() {
        ObjectAnimator.ofFloat(drawer?.idc, "alpha", 0.8F, 0.3F).setDuration(233).start()
        ObjectAnimator.ofFloat(drawer, "translationY", 0F, delta).setDuration(233).start()
    }
    private fun hideInfCardFull() {
        ObjectAnimator.ofFloat(drawer?.idc, "alpha", 0.8F, 0.0F).setDuration(233).start()
        ObjectAnimator.ofFloat(drawer, "translationY", 0F, delta).setDuration(233).start()
    }

    companion object {
        const val HIDE_INFO_CARD = 1
        const val SHOW_INFO_CARD = 2
        const val TRIGGER_INFO_CARD = 3
        //const val LOAD_IMG_ON = 4
        const val CLEAR_IMG_ON = 5
        const val PREPARE_LAST_PAGE = 6
        const val DIALOG_SHOW = 7
        //const val DELAYED_RESTORE_PAGE_NUMBER = 8
        const val LOAD_ITEM_SCROLL_MODE = 9
        const val LOAD_SCROLL_MODE = 10
        const val LOAD_ITEM_IMAGES_INTO_LINE = 11
        const val LOAD_IMAGES_INTO_LINE = 12
        const val RESTORE_PAGE_NUMBER = 13
        const val LOAD_PAGE_FROM_ITEM = 14
        const val DIALOG_HIDE = 15
        const val HIDE_INFO_CARD_FULL = 16
        const val SHOW_INFO_CARD_FULL = 17
        const val TRIGGER_INFO_CARD_FULL = 18
        const val INIT_IMAGE_COUNT = 19
        const val DECREASE_IMAGE_COUNT_AND_RESTORE_PAGE_NUMBER_AT_ZERO = 20
        const val DO_LAMBDA = 21
        const val SET_NET_INFO = 22
        const val SET_DL_TEXT = 23
    }
}