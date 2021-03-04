package top.fumiama.copymanga.ui.comicdl

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Toast
import com.google.gson.Gson
import kotlinx.android.synthetic.main.fragment_book.*
import kotlinx.android.synthetic.main.fragment_chapters.*
import kotlinx.android.synthetic.main.line_chapter.view.*
import kotlinx.android.synthetic.main.widget_downloadbar.*
import kotlinx.android.synthetic.main.fragment_dlcomic.*
import kotlinx.android.synthetic.main.line_horizonal_empty.view.*
import kotlinx.android.synthetic.main.button_tbutton.*
import kotlinx.android.synthetic.main.button_tbutton.view.*
import kotlinx.android.synthetic.main.line_caption.view.*
import top.fumiama.dmzj.copymanga.R
import top.fumiama.copymanga.MainActivity.Companion.mainWeakReference
import top.fumiama.copymanga.json.ComicStructureOld
import top.fumiama.copymanga.json.VolumeStructure
import top.fumiama.copymanga.tools.CMApi
import top.fumiama.copymanga.tools.MangaDlTools
import top.fumiama.copymanga.tools.PropertiesTools
import top.fumiama.copymanga.tools.UITools
import top.fumiama.copymanga.ui.comicdl.ComicDlFragment.Companion.json
import top.fumiama.copymanga.ui.vm.ViewMangaActivity
import top.fumiama.copymanga.views.ChapterToggleButton
import top.fumiama.copymanga.views.LazyScrollView
import java.io.File
import java.lang.ref.WeakReference

class ComicDlHandler(looper: Looper, that: WeakReference<ComicDlFragment>, private val vols: Array<VolumeStructure>, private val comicName: String, private val groupNames: Array<String>?):Handler(looper) {
    constructor(looper: Looper, that: WeakReference<ComicDlFragment>, comicName: String) : this(looper, that, arrayOf(), comicName, null) {
        isOld = true
    }
    private var isOld = false
    var complete = false
    private val that = that.get()
    private val toolsBox = UITools(that.get()?.context)
    private val p = PropertiesTools(File("${that.get()?.context?.filesDir}/settings.properties"))
    private var btnNumPerRow = 4
    private var btnw = 0
    private var cdwnWidth = 0
    private var dl: Dialog? = null
    private var hasToastedError = false
        get(){
            val re = field
            field = true
            return re
        }
    private var haveSElectAll = false
    private var checkedChapter = 0
    private var dldChapter = 0
    private var haveDlStarted = false
    private var canDl = false
    private var tbtnlist: Array<ChapterToggleButton> = arrayOf()
    private var tbtncnt = 0
    private var isNewTitle = false
    private val mangaDlTools = MangaDlTools()
    private var multiSelect = false
    private var size = 0
    private var refreshSize = true
    private var ltbtn: View? = null


    @SuppressLint("SetTextI18n")
    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        when(msg.what){
            0 -> dl?.hide()
            1 -> {
                tbtnlist[msg.arg1].setBackgroundResource(R.drawable.rndbg_checked)
                tbtnlist[msg.arg1].isChecked = false
                updateProgressBar()
            }
            -1 -> {
                tbtnlist.get(msg.arg1).setBackgroundResource(R.drawable.rndbg_error)
                dldChapter--
                Toast.makeText(
                    that?.context,
                    "下载${tbtnlist[msg.arg1].chapterName}失败",
                    Toast.LENGTH_SHORT
                ).show()
                updateProgressBar()
            }
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
                    dldChapter = 0
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
            5 -> {
                setSize(msg.arg2)
                updateProgressBar(msg.arg2, size)
                if (!(msg.obj as Boolean)) {
                    Toast.makeText(that?.context, "下载${tbtnlist.get(msg.arg1).chapterName}的第${msg.arg2}页失败", Toast.LENGTH_SHORT).show()
                }else{
                    val progressTxt = that?.tdwn?.text.toString()
                    that?.tdwn?.text = "${progressTxt.substringBefore(' ')} 的 ${msg.arg2}/${size} 页"
                }
            }
            6 -> that?.tdwn?.text = "${dldChapter}/${checkedChapter}"
            7 -> deleteChapters(msg.obj as File, msg.arg1)
            8 -> that?.cdwn?.setCardBackgroundColor(that.resources.getColor(R.color.colorBlue))
            9 -> that?.cdwn?.setCardBackgroundColor(that.resources.getColor(R.color.colorGreen))
            10 -> addTbtn(msg.obj as Array<String>)
            11 -> addCaption(msg.obj as String)
            12 -> addDiv()
            13 -> that?.let { Toast.makeText(it.context, "下载${tbtnlist[msg.arg1].textOn}的第${msg.arg2}页失败，尝试重新下载...", Toast.LENGTH_SHORT).show() }
        }
    }

    fun startLoad(){
        setComponents()
        if(isOld) analyzeOldStructure()
        else Thread{
            ViewMangaActivity.urlArray = arrayOf()
            ViewMangaActivity.fileArray = arrayOf()
            vols.forEachIndexed { i, vol ->
                val caption = groupNames?.get(i)?:vol.results.list[0].group_path_word
                Log.d("MyCDH", "caption: $caption, group name: ${groupNames?.get(i)}")
                obtainMessage(11, caption).sendToTarget()       //addCaption
                vol.results.list.forEach { chapter ->
                    var data = arrayOf<String>()
                    data += chapter.name
                    data += chapter.uuid
                    data += caption
                    data += CMApi.getApiUrl(R.string.chapterInfoApiUrl, chapter.comic_path_word, chapter.uuid)?:""
                    obtainMessage(10, data).sendToTarget()
                }
                sendEmptyMessage(12)                            //addDiv
            }
            complete = true
        }.start()

    }
    private fun addDiv(){
        that?.ldwn?.addView(
            that.layoutInflater.inflate(R.layout.div_h, that.ldwn, false),
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
    }
    private fun addCaption(title: String){
        val tc = that?.layoutInflater?.inflate(R.layout.line_caption, that.ldwn, false)
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
    }
    private fun deleteChapter(f: File, v: ChapterToggleButton) {
        f.delete()
        v.setBackgroundResource(R.drawable.toggle_button)
        v.isChecked = false
    }
    private fun deleteChapters(zipf: File, index: Int) {
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
        that?.tdwn?.text = "${++dldChapter}/$checkedChapter"
        setProgress2(dldChapter * 100 / checkedChapter, 233)
    }
    private fun updateProgressBar(pageNow: Int, size: Int) {
        val delta = 100 / checkedChapter
        val start = dldChapter * delta
        val now = pageNow * delta / size
        setProgress2(start + now, 64)
    }
    private fun setProgress2(end: Int, duration: Long) {
        ObjectAnimator.ofInt(
            that?.pdwn,
            "progress",
            that?.pdwn?.progress?:0,
            end
        ).setDuration(duration).start()
    }
    private fun setSize(pageNow: Int){
        if(refreshSize || size == 0) {
            size = mangaDlTools.size
            refreshSize = false
        }else if(pageNow == size) refreshSize = true
    }
    private fun setComponents() {
        val widthData = toolsBox.calcWidthFromDpRoot(8, 64)
        btnNumPerRow = widthData[0]
        btnw = widthData[1]
        dl = mainWeakReference?.get()?.let { Dialog(it) }
        dl?.setContentView(R.layout.dialog_unzipping)
        that?.dlsdwn?.viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener{
            override fun onGlobalLayout() {
                cdwnWidth = that.dlsdwn.width
                Log.d("MyDl", "Get dlsdwn height: $cdwnWidth")
                that.dlsdwn.viewTreeObserver.removeOnGlobalLayoutListener(this)
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
            if(that.dlsdwn.translationX != 0f) showDlCard()
            else if(checkedChapter == 0) hideDlCard()
            else{
                that.pdwn.progress = 0
                if (canDl || checkedChapter == 0) canDl = false
                else {
                    haveDlStarted = true
                    canDl = true
                    Thread{
                        sendEmptyMessage(9)     //set dl card color to green
                        downloadMangas()
                        sendEmptyMessage(8)     //set dl card color to blue
                        if (!haveDlStarted) {
                            dldChapter = 0
                            checkedChapter = 0
                            this.postDelayed({
                                setProgress2(0, 233)
                                that.tdwn?.text = "0/0"
                            }, 400)
                        }
                    }.start()
                }
            }
        }
        that?.cdwn?.setOnLongClickListener {
            Thread { sendEmptyMessage(4) }.start()
            return@setOnLongClickListener true
        }
    }
    fun showMultiSelectInfo() {
        toolsBox.buildInfo("进入多选模式？", "之后可以对已下载漫画进行批量删除/重新下载",
            "确定", null, "取消", { multiSelect = true })
    }
    private fun downloadMangas(){
        for (i in tbtnlist) {
            if (i.isChecked) downloadChapterPages(i)
            if (!canDl) {
                checkedChapter -= dldChapter
                dldChapter = 0
                break
            }
        }
        if (canDl) {
            haveDlStarted = false
            canDl = false
        }
    }
    
    private fun downloadChapterPages(i: ChapterToggleButton) {
        mangaDlTools.onDownloadedListener =
            object : MangaDlTools.OnDownloadedListener {
                override fun handleMessage(succeed: Boolean) {
                    this@ComicDlHandler.obtainMessage(if (succeed) 1 else -1, i.index, 0)
                        .sendToTarget()
                }
                override fun handleMessage(succeed: Boolean, pageNow: Int) {
                    this@ComicDlHandler.obtainMessage(
                        5,
                        i.index,
                        pageNow,
                        succeed
                    ).sendToTarget()
                }
                override fun handleMessage(pageNow: Int){
                    this@ComicDlHandler.obtainMessage(13, i.index, pageNow).sendToTarget()
                }
            }
        i.url?.let {
            mangaDlTools.downloadChapterInVol(
                it,
                i.chapterName,
                i.caption?:"null",
                i.index
            )
        }
    }
    private fun showDlCard(){
        //ObjectAnimator.ofFloat(dlsdwn, "alpha", 0.3f, 0.9f).setDuration(233).start()
        ObjectAnimator.ofFloat(that?.dlsdwn, "translationX", cdwnWidth.toFloat() * 0.9f, 0f).setDuration(233).start()
    }

    private fun hideDlCard(){
        //ObjectAnimator.ofFloat(dlsdwn, "alpha", 0.9f, 0.3f).setDuration(233).start()
        ObjectAnimator.ofFloat(that?.dlsdwn, "translationX", 0f, cdwnWidth.toFloat() * 0.9f).setDuration(233).start()
    }
    private fun addTbtn(data: Array<String>){
        addTbtn(data[0], data[1], data[2], data[3])
        ViewMangaActivity.urlArray += data[3]
    }
    @SuppressLint("SetTextI18n")
    private fun addTbtn(title: String, uuid: String, caption: String, url: String) {
        if ((tbtncnt % btnNumPerRow == 0) || isNewTitle) {
            ltbtn = that?.layoutInflater?.inflate(R.layout.line_horizonal_empty, that.ldwn, false)
            that?.ldwn?.addView(ltbtn)
            tbtncnt = 0
            isNewTitle = false
        }
        that?.layoutInflater?.inflate(R.layout.button_tbutton, ltbtn?.ltbtn, false)?.let { tbv ->
            tbv.tbtn.index = tbtnlist.size
            tbtnlist += tbv.tbtn
            tbtncnt++

            tbv.tbtn.uuid = uuid
            tbv.tbtn.chapterName = title
            tbv.tbtn.url = url
            //tbv.tbtn.hint = caption
            tbv.tbtn.caption = caption
            tbv.tbtn.layoutParams.width = btnw
            val zipf = CMApi.getZipFile(that.context?.getExternalFilesDir(""), comicName, caption, title)
            Log.d("MyCD", "Get zipf: $zipf")
            ViewMangaActivity.fileArray += zipf
            if (zipf.exists()) {
                tbv.tbtn.setBackgroundResource(R.drawable.rndbg_checked)
                tbv.tbtn.isChecked = false
            }
            ltbtn?.ltbtn?.addView(tbv)
            ltbtn?.invalidate()
            tbv.tbtn.setOnClickListener {
                if (zipf.exists() && !multiSelect) {
                    it.tbtn.setBackgroundResource(R.drawable.rndbg_checked)
                    it.tbtn.isChecked = false
                    ViewMangaActivity.zipFile = zipf
                    ViewMangaActivity.dlhandler = this
                    ViewMangaActivity.position = it.tbtn.index
                    dl?.show()

                    that.startActivity(Intent(that.context, ViewMangaActivity::class.java)
                        .putExtra("callFrom", "zipFirst")
                    )
                } else {
                    it.tbtn.setBackgroundResource(R.drawable.toggle_button)
                    if (it.tbtn.isChecked) that.tdwn?.text = "$dldChapter/${++checkedChapter}"
                    else that.tdwn.text = "$dldChapter/${--checkedChapter}"
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
                            Thread {
                                obtainMessage(7, it.tbtn.index, 0, zipf).sendToTarget()
                            }.start()
                        })
                }else{
                    toolsBox.buildInfo("直接观看", "不下载而进行观看", "确定",
                        null, "取消", {
                            ViewMangaActivity.zipFile = null
                            ViewMangaActivity.dlhandler = this
                            ViewMangaActivity.position = it.tbtn.index
                            dl?.show()

                            that.startActivity(Intent(that.context, ViewMangaActivity::class.java))
                        }, null, null
                    )
                }
                true
            }
        }
    }

    private fun analyzeOldStructure() = Thread{
        Gson().fromJson(json?.reader(), Array<ComicStructureOld>::class.java)?.let {
            for (group in it) {
                that?.layoutInflater?.inflate(R.layout.line_caption, that.ldwn, false)?.let { tc ->
                    tc.tcptn.text = group.name
                    that.ldwn.addView(
                        tc,
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    )
                    that.ldwn.addView(
                        that.layoutInflater.inflate(R.layout.div_h, that.ldwn, false),
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    )
                    isNewTitle = true
                    for (chapter in group.chapters) {
                        val newUrl = CMApi.getApiUrl(R.string.chapterInfoApiUrl, chapter.url.substringAfter("/comic/").substringBefore('/'), chapter.url.substringAfterLast('/'))?:""
                        Log.d("MyCD", "Generate new url: $newUrl")
                        obtainMessage(10, arrayOf(chapter.name, "", group.name, newUrl)).sendToTarget()
                    }
                }
            }
        }
    }.start()
}