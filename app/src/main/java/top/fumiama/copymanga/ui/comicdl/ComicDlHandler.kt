package top.fumiama.copymanga.ui.comicdl

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import kotlinx.android.synthetic.main.button_tbutton.*
import kotlinx.android.synthetic.main.button_tbutton.view.*
import kotlinx.android.synthetic.main.fragment_book.*
import kotlinx.android.synthetic.main.fragment_dlcomic.*
import kotlinx.android.synthetic.main.line_caption.view.*
import kotlinx.android.synthetic.main.line_chapter.view.*
import kotlinx.android.synthetic.main.line_horizonal_empty.view.*
import kotlinx.android.synthetic.main.widget_downloadbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.fumiama.copymanga.json.ChapterStructure
import top.fumiama.copymanga.json.ComicStructureOld
import top.fumiama.copymanga.json.VolumeStructure
import top.fumiama.copymanga.manga.MangaDlTools
import top.fumiama.copymanga.manga.Reader
import top.fumiama.copymanga.tools.api.CMApi
import top.fumiama.copymanga.tools.ui.UITools
import top.fumiama.copymanga.ui.comicdl.ComicDlFragment.Companion.exit
import top.fumiama.copymanga.ui.comicdl.ComicDlFragment.Companion.json
import top.fumiama.copymanga.ui.vm.ViewMangaActivity
import top.fumiama.copymanga.views.ChapterToggleButton
import top.fumiama.copymanga.views.LazyScrollView
import top.fumiama.dmzj.copymanga.R
import java.io.File
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicInteger

class ComicDlHandler(
    looper: Looper, private val th: WeakReference<ComicDlFragment>,
    private val vols: Array<VolumeStructure>, private val comicName: String,
    private val groupNames: Array<String>?, private val version: Int,
):Handler(looper) {
    constructor(looper: Looper, th: WeakReference<ComicDlFragment>, comicName: String)
            : this(looper, th, arrayOf(), comicName, null, 2) {
        isOld = true
    }
    private var isOld = false
    private var complete = false
    private val that get() = th.get()
    private val toolsBox = UITools(th.get()?.context)
    private var btnNumPerRow = 4
    private var btnw = 0
    private var cdwnWidth = 0
    var dl: Dialog? = null
    private var haveSElectAll = false
    private var checkedChapter = 0
    private val dldChapter: Int get() = finishMap.count { p -> return@count p == true }
    private var tbtnlist: Array<ChapterToggleButton> = arrayOf()
    private var tbtncnt = 0
    private var isNewTitle = false
    val mangaDlTools = MangaDlTools()
    private var multiSelect = false
    private var finishMap = arrayOf<Boolean?>()
    var downloading = false
    private var urlArray = arrayOf<String>()
    private var uuidArray = arrayOf<String>()
    private val maxBatch = that?.activity?.let { PreferenceManager.getDefaultSharedPreferences(it) }?.getInt("settings_cat_md_sb_max_batch", 16)?:16

    @SuppressLint("SetTextI18n")
    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        when(msg.what){
            0 -> dl?.hide()
            //2 -> scanHiddenChapters()
            //3 ->
            4 -> {
                that?.pdwn?.progress = 0
                if (haveSElectAll) {
                    for (i in tbtnlist) {
                        if (!isChapterExists(i.chapterName, i.caption ?: "null")) {
                            i.setBackgroundResource(R.drawable.toggle_button)
                            i.isChecked = false
                        } else if(multiSelect) {
                            i.setBackgroundResource(R.drawable.rndbg_checked)
                            i.isChecked = false
                        }
                    }
                    haveSElectAll = false
                    checkedChapter = 0
                } else {
                    for (i in tbtnlist) {
                        if (multiSelect || !i.isChecked && !isChapterExists(i.chapterName, i.caption ?: "null")) {
                            i.setBackgroundResource(R.drawable.toggle_button)
                            i.isChecked = true
                            checkedChapter++
                        }
                    }
                    haveSElectAll = true
                }
                that?.tdwn?.text = "${dldChapter}/${checkedChapter}"
            }
            6 -> that?.tdwn?.text = "${dldChapter}/${checkedChapter}"
            7 -> that?.lifecycleScope?.launch { deleteChapters(msg.obj as File, msg.arg1) }
            9 -> that?.cdwn?.setCardBackgroundColor(that!!.resources.getColor(R.color.colorGreen))
            //10 -> addButton(msg.obj as Array<String>)
            //11 -> addCaption(msg.obj as String)
            //12 -> addDiv()
            13 -> if(complete) showMultiSelectInfo()
        }
    }

    suspend fun startLoad() = withContext(Dispatchers.IO) {
        setComponents()
        if(isOld) analyzeOldStructure()
        else {
            urlArray = arrayOf()
            Reader.fileArray = arrayOf()
            uuidArray = arrayOf()
            vols.forEachIndexed { i, vol ->
                val caption = groupNames?.get(i)?:vol.results.list[0].group_path_word
                Log.d("MyCDH", "caption: $caption, group name: ${groupNames?.get(i)}")
                withContext(Dispatchers.Main) {
                    addCaption(caption) {
                        addButtons(vol.results.list, caption, version)
                    }
                }
            }
            complete = true
        }
        that?.hideKanban()
    }
    private suspend fun addDiv() = withContext(Dispatchers.Main) {
        that?.ldwn?.addView(
            that!!.layoutInflater.inflate(R.layout.div_h, that!!.ldwn, false),
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
    }
    private suspend fun addCaption(title: String, body: suspend() -> Unit) = withContext(Dispatchers.Main) {
        val tc = that?.layoutInflater?.inflate(R.layout.line_caption, that!!.ldwn, false)
        tc?.tcptn?.text = title
        that?.ldwn?.addView(
            tc,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        addDiv()
        isNewTitle = true
        body()
    }
    private suspend fun deleteChapter(f: File, v: ChapterToggleButton) = withContext(Dispatchers.IO) {
        f.delete()
        withContext(Dispatchers.Main) {
            v.setBackgroundResource(R.drawable.toggle_button)
            v.isChecked = false
        }
    }
    private suspend fun deleteChapters(zipf: File, index: Int) = withContext(Dispatchers.IO) {
        if (multiSelect) {
            for (i in tbtnlist) {
                if (i.isChecked) {
                    val f = CMApi.getZipFile(that?.context?.getExternalFilesDir(""), comicName, i.caption?:"null", i.chapterName)
                    if (f.exists()) {
                        deleteChapter(f, i)
                        checkedChapter--
                    }
                }
            }
            multiSelect = false
            sendEmptyMessage(6)
        } else deleteChapter(zipf, tbtnlist[index])
    }
    private fun isChapterExists(chapter: CharSequence, caption: CharSequence) =
        File(that?.context?.getExternalFilesDir(""),"$comicName/$caption/$chapter.zip").exists()
    @SuppressLint("SetTextI18n")
    private fun updateProgressBar() {
        that?.tdwn?.text = "$dldChapter/$checkedChapter"
        setProgress2(dldChapter * 100 / (if(checkedChapter > 0) checkedChapter else 1), 233)
    }
    private fun setProgress2(end: Int, duration: Long) {
        ObjectAnimator.ofInt(
            that?.pdwn,
            "progress",
            that?.pdwn?.progress?:0,
            end
        ).setDuration(duration).start()
    }
    private suspend fun setComponents() = withContext(Dispatchers.Main) {
        val widthData = toolsBox.calcWidthFromDpRoot(8, 64)
        btnNumPerRow = widthData[0]
        btnw = widthData[1]
        dl = that?.activity?.let { Dialog(it) }
        dl?.setContentView(R.layout.dialog_unzipping)
        that?.dlsdwn?.viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener{
            override fun onGlobalLayout() {
                that!!.apply {
                    cdwnWidth = dlsdwn.width
                    Log.d("MyDl", "Get dlsdwn width: $cdwnWidth")
                    ldwn?.setPadding(0, 0, 0, dlsdwn.height+navBarHeight)
                }
                that!!.dlsdwn.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
        that?.dllazys?.onScrollListener = object : LazyScrollView.OnScrollListener{
            override fun onBottom() { //if(dlsdwn.translationX > 0f) showDlCard()
            }
            override fun onScroll() { if(that?.dlsdwn?.translationX == 0f) hideDlCard() }
            override fun onTop() { //if(dlsdwn.translationX > 0f) showDlCard()
            }
        }
        that?.cdwn?.setOnClickListener {
            if(that!!.dlsdwn.translationX != 0f) showDlCard()
            else if(checkedChapter == 0) hideDlCard()
            else{
                that!!.pdwn.progress = 0
                if (downloading || checkedChapter == 0) {
                   mangaDlTools.wait = !mangaDlTools.wait!!
                } else that?.lifecycleScope?.launch {
                    downloading = true
                    sendEmptyMessage(9)
                    finishMap = arrayOfNulls(tbtnlist.size)
                    downloadChapterPages()
                }
            }
        }
        that?.cdwn?.setOnLongClickListener {
            sendEmptyMessage(4)
            return@setOnLongClickListener true
        }
        mangaDlTools.onDownloadedListener = object :MangaDlTools.OnDownloadedListener {
            override fun handleMessage(index: Int, isSuccess: Boolean, message: String) {
                that?.lifecycleScope?.launch {
                    if(isSuccess) onZipDownloadFinish(index)
                    else onZipDownloadFailure(index, message)
                }
            }

            @SuppressLint("SetTextI18n")
            override fun handleMessage(
                index: Int,
                downloaded: Int,
                total: Int,
                isSuccess: Boolean,
                message: String
            ) {
                that?.lifecycleScope?.launch {
                    if(isSuccess) {
                        tbtnlist[index].text = if(downloaded == 0 && total == 0) tbtnlist[index].chapterName else "$downloaded/$total"
                    } else {
                        tbtnlist[index].text = "$downloaded/$total"
                        Toast.makeText(that?.context, "下载${tbtnlist[index].chapterName}的第${downloaded}页失败: $message", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun showMultiSelectInfo() {
        if(multiSelect) {
            toolsBox.buildInfo("退出多选模式？", "退出后只能对单个漫画进行长按删除",
                "确定", null, "取消", { multiSelect = false })
            return
        }
        toolsBox.buildInfo("进入多选模式？", "之后可以对已下载漫画进行批量删除/重新下载",
            "确定", null, "取消", { multiSelect = true })
    }
    
    private suspend fun downloadChapterPages() = withContext(Dispatchers.IO) {
        val totalInDownload = AtomicInteger(0)
        tbtnlist.forEach { i ->
            if(i.isChecked) {
                withContext(Dispatchers.Main) {
                    i.isEnabled = false
                }
                i.url?.let {
                    while (totalInDownload.get() >= maxBatch) delay(1000)
                    totalInDownload.incrementAndGet()
                    launch {
                        mangaDlTools.downloadChapterInVol(
                            it,
                            i.chapterName,
                            i.caption?:"null",
                            i.index
                        )
                    }.invokeOnCompletion { totalInDownload.decrementAndGet() }
                }
            }
        }
    }

    private suspend fun onZipDownloadFinish(index: Int) = withContext(Dispatchers.Main) {
        if(index >= 0 && index < tbtnlist.size) {
            tbtnlist[index].setBackgroundResource(R.drawable.rndbg_checked)
            tbtnlist[index].isChecked = false
            tbtnlist[index].isEnabled = true
            finishMap[index] = true
            updateProgressBar()
            that?.apply {
                cdwn.postDelayed({
                    if (mangaDlTools.exit) return@postDelayed
                    if (dldChapter == checkedChapter) {
                        checkedChapter = 0
                        setProgress2(0, 233)
                        tdwn.text = "0/0"
                        cdwn.setCardBackgroundColor(resources.getColor(R.color.colorBlue))
                        finishMap = arrayOf()
                        downloading = false
                    }
                }, 400)
            }
        }
    }

    private suspend fun onZipDownloadFailure(index: Int, message: String) = withContext(Dispatchers.Main) {
        if (exit) return@withContext
        tbtnlist[index].setBackgroundResource(R.drawable.rndbg_error)
        tbtnlist[index].isEnabled = true
        Toast.makeText(that?.context, "下载${tbtnlist[index].chapterName}失败: $message", Toast.LENGTH_SHORT).show()
        updateProgressBar()
    }
    
    private fun showDlCard(){
        //ObjectAnimator.ofFloat(dlsdwn, "alpha", 0.3f, 0.9f).setDuration(233).start()
        ObjectAnimator.ofFloat(that?.dlsdwn, "translationX", cdwnWidth.toFloat() * 0.9f, 0f).setDuration(233).start()
    }

    private fun hideDlCard(){
        //ObjectAnimator.ofFloat(dlsdwn, "alpha", 0.9f, 0.3f).setDuration(233).start()
        ObjectAnimator.ofFloat(that?.dlsdwn, "translationX", 0f, cdwnWidth.toFloat() * 0.9f).setDuration(233).start()
    }
    private suspend fun addButtons(chapters: Array<ChapterStructure>, caption: String, version: Int) = withContext(Dispatchers.IO) {
        chapters.forEach { chapter ->
            val u = CMApi.getChapterInfoApiUrl(chapter.comic_path_word, chapter.uuid, version)?:""
            addButton(chapter.name, chapter.uuid, caption, u)
            urlArray += u
        }
        addDiv()
    }
    @SuppressLint("SetTextI18n")
    private suspend fun addButton(title: String, uuid: String, caption: String, url: String) = withContext(Dispatchers.Main) {
        if ((tbtncnt % btnNumPerRow == 0) || isNewTitle) {
            that?.ltbtn = that?.layoutInflater?.inflate(R.layout.line_horizonal_empty, that!!.ldwn, false)
            that?.ldwn?.addView(that!!.ltbtn)
            tbtncnt = 0
            isNewTitle = false
        }
        that?.layoutInflater?.inflate(R.layout.button_tbutton, that!!.ltbtn?.ltbtn, false)?.let { tbv ->
            tbv.tbtn.index = tbtnlist.size
            tbtnlist += tbv.tbtn
            tbtncnt++

            tbv.tbtn.uuid = uuid
            uuidArray += uuid
            tbv.tbtn.chapterName = title
            tbv.tbtn.url = url
            //tbv.tbtn.hint = caption
            tbv.tbtn.caption = caption
            tbv.tbtn.layoutParams.width = btnw

            that?.ltbtn?.ltbtn?.addView(tbv)
            that?.ltbtn?.invalidate()

            withContext(Dispatchers.IO) {
                val zipf = CMApi.getZipFile(that!!.context?.getExternalFilesDir(""), comicName, caption, title)
                Log.d("MyCD", "Get zipf: $zipf")
                Reader.fileArray += zipf
                if (zipf.exists()) withContext(Dispatchers.Main) {
                    tbv.tbtn.setBackgroundResource(R.drawable.rndbg_checked)
                    tbv.tbtn.isChecked = false
                }
                tbv.tbtn.setOnClickListener {
                    if (zipf.exists() && !multiSelect) {
                        it.tbtn.setBackgroundResource(R.drawable.rndbg_checked)
                        it.tbtn.isChecked = false
                        ViewMangaActivity.dlHandler = this@ComicDlHandler
                        dl?.show()
                        Reader.viewMangaZipFile(it.tbtn.index, urlArray, uuidArray, zipf)
                    } else {
                        it.tbtn.setBackgroundResource(R.drawable.toggle_button)
                        if (it.tbtn.isChecked) that?.tdwn?.text = "$dldChapter/${++checkedChapter}"
                        else that?.tdwn?.text = "$dldChapter/${--checkedChapter}"
                    }
                }
                tbv.tbtn.setOnLongClickListener {
                    if (zipf.exists()) {
                        toolsBox.buildInfo("确认删除${if (multiSelect) "这些" else "本"}章节?",
                            "该操作将不可撤销",
                            "确定",
                            null,
                            "取消",
                            {
                                obtainMessage(7, it.tbtn.index, 0, zipf).sendToTarget()
                            })
                    } else {
                        toolsBox.buildInfo("直接观看", "不下载而进行观看", "确定",
                            null, "取消", {
                                ViewMangaActivity.dlHandler = this@ComicDlHandler
                                dl?.show()
                                Reader.start2viewManga(null, it.tbtn.index, urlArray, uuidArray)
                            }, null, null
                        )
                    }
                    true
                }
            }
        }
    }

    private suspend fun analyzeOldStructure() = withContext(Dispatchers.IO) {
        Gson().fromJson(json?.reader(), Array<ComicStructureOld>::class.java)?.let {
            for (group in it) {
                that?.layoutInflater?.inflate(R.layout.line_caption, that!!.ldwn, false)?.let { tc ->
                    tc.tcptn.text = group.name
                    withContext(Dispatchers.Main) {
                        that!!.ldwn.addView(
                            tc,
                            ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                        )
                        that!!.ldwn.addView(
                            that!!.layoutInflater.inflate(R.layout.div_h, that!!.ldwn, false),
                            ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                        )
                    }
                    isNewTitle = true
                    for (chapter in group.chapters) {
                        val newUrl = CMApi.getChapterInfoApiUrl(
                            chapter.url.substringAfter("/comic/").substringBefore('/'),
                            chapter.url.substringAfterLast('/'), version,
                        )?:""
                        Log.d("MyCD", "Generate new url: $newUrl")
                        addButton(chapter.name, "", group.name, newUrl)
                        urlArray += newUrl
                    }
                }
            }
        }
    }
}
