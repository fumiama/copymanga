package top.fumiama.copymanga.activity

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Toast
import android.widget.ToggleButton
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_dl.*
import kotlinx.android.synthetic.main.button_tbutton.view.*
import kotlinx.android.synthetic.main.line_caption.view.*
import kotlinx.android.synthetic.main.line_horizonal.view.*
import kotlinx.android.synthetic.main.widget_downloadbar.*
import kotlinx.android.synthetic.main.widget_titlebar.*
import top.fumiama.copymanga.R
import top.fumiama.copymanga.activity.MainActivity.Companion.mh
import top.fumiama.copymanga.data.ComicStructure
import top.fumiama.copymanga.handler.DlHandler
import top.fumiama.copymanga.tool.MangaDlTools
import top.fumiama.copymanga.tool.MangaDlTools.Companion.wmdlt
import top.fumiama.copymanga.tool.ToolsBox
import top.fumiama.copymanga.view.LazyScrollView
import java.io.File
import java.lang.Thread.sleep
import java.lang.ref.WeakReference
import java.util.zip.ZipFile


class DlActivity : Activity() {
    private var tbtncnt = 0
    private var isNewTitle = false
    var haveSElectAll = false
    var checkedChapter = 0
    var dldChapter = 0
    var haveDlStarted = false
    private var btnNumPerRow = 4
    private lateinit var ltbtn: View
    var tbtnlist: List<ToggleButton> = arrayListOf()
    var tbtnUrlList = arrayListOf<String>()
    private val handler = DlHandler(this, Looper.myLooper()!!)
    private var btnw = 0
    private var cdwnWidth = 0
    private var canDl = false
    private lateinit var toolsBox: ToolsBox
    lateinit var mangaDlTools: MangaDlTools
    var multiSelect = false
    private var zipArrayList: Array<String> = arrayOf()


    @ExperimentalStdlibApi
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dl)
        mh?.saveUrlsOnly = true
        mangaDlTools = MangaDlTools(this)
        handler.sendEmptyMessage(-2)        //setLayouts
    }

    override fun onDestroy() {
        mh?.saveUrlsOnly = false
        wmdlt?.get()?.exit = true
        super.onDestroy()
    }

    private fun showDlCard(){
        //ObjectAnimator.ofFloat(csdwn, "alpha", 0.3f, 0.9f).setDuration(233).start()
        ObjectAnimator.ofFloat(csdwn, "translationX", cdwnWidth.toFloat() * 0.9f, 0f).setDuration(
            233
        ).start()
    }

    private fun hideDlCard(){
        //ObjectAnimator.ofFloat(csdwn, "alpha", 0.9f, 0.3f).setDuration(233).start()
        ObjectAnimator.ofFloat(csdwn, "translationX", 0f, cdwnWidth.toFloat() * 0.9f).setDuration(
            233
        ).start()
    }

    private fun fillChapters() {
        mangaDlTools.allocateChapterUrls(checkedChapter)
        for (i in tbtnlist.indices) {
            if (tbtnlist[i].isChecked) mangaDlTools.dlChapterUrl(tbtnUrlList[i])
        }
    }

    private fun dlThead(dlMethod: (i: ToggleButton) -> Unit) {
        sleep(10000)
        for (i in tbtnlist.listIterator()) {
            if (i.isChecked) dlMethod(i)
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
        handler.sendEmptyMessage(8)     //set dl card color to blue
    }

    @ExperimentalStdlibApi
    @SuppressLint("SetTextI18n")
    fun setLayouts() {
        ttitle.text = comicName
        toolsBox = ToolsBox(WeakReference(this))
        val widthData = toolsBox.calcWidthFromDp(8, 64)
        btnNumPerRow = widthData[0]
        btnw = widthData[1]
        csdwn.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                cdwnWidth = csdwn.width
                csdwn.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
        dllazys.onScrollListener = object : LazyScrollView.OnScrollListener {
            override fun onBottom() {}
            override fun onScroll() { if (csdwn.translationX == 0f) hideDlCard() }
            override fun onTop() {}
        }
        cdwn.setOnClickListener {
            if (csdwn.translationX != 0f) showDlCard()
            else if (checkedChapter == 0) hideDlCard()
            else {
                pdwn.progress = 0
                if (canDl || checkedChapter == 0) canDl = false
                else {
                    haveDlStarted = true
                    canDl = true
                    handler.sendEmptyMessage(9)     //set dl card color to red
                    Toast.makeText(this, "十秒后开始下载...", Toast.LENGTH_SHORT).show()
                    fillChapters()
                    Thread { dlThead { downloadChapterPages(it) } }.start()
                }
            }
        }
        cdwn.setOnLongClickListener {
            handler.sendEmptyMessage(4)
            return@setOnLongClickListener true
        }
        isearch.setOnClickListener { showMultiSelectInfo() }
        analyzeStructure()
    }

    private fun showMultiSelectInfo() {
        toolsBox.buildInfo("进入多选模式？", "确定后，长按下载条可选中全部漫画，而不仅限于未下载者；点击已下载漫画可进行选择。",
            "确定", null, "取消", { multiSelect = true })
    }

    private fun analyzeStructure() {
        Gson().fromJson(json?.reader(), Array<ComicStructure>::class.java)?.let {
            for (group in it) {
                val tc = layoutInflater.inflate(R.layout.line_caption, ldwn, false)
                tc.tcptn.text = group.name
                ldwn.addView(
                    tc,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                )
                ldwn.addView(
                    layoutInflater.inflate(R.layout.div_h, ldwn, false),
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                )
                isNewTitle = true
                for (chapter in group.chapters) addTbtn(chapter.name, chapter.url, group.name)
            }
        }
        val mangaHome = File("${getExternalFilesDir("")}/$comicName")
        val jsonFile = File(mangaHome, "info.bin")
        if(!mangaHome.exists()) mangaHome.mkdirs()
        json?.let { jsonFile.writeText(it) }
    }

    @ExperimentalStdlibApi
    private fun downloadChapterPages(i: ToggleButton) {
        mangaDlTools.onDownloadedListener =
            object : MangaDlTools.OnDownloadedListener {
                override fun handleMessage(succeed: Boolean) {
                    handler.obtainMessage(if (succeed) 1 else -1, tbtnlist.indexOf(i), 0)
                        .sendToTarget()
                }
                override fun handleMessage(succeed: Boolean, pageNow: Int) {
                    handler.obtainMessage(
                        5,
                        tbtnlist.indexOf(i),
                        pageNow,
                        succeed
                    ).sendToTarget()
                }
                override fun handleMessage(pageNow: Int){
                    handler.obtainMessage(
                        10,
                        tbtnlist.indexOf(i),
                        pageNow
                    ).sendToTarget()
                }
            }
        mangaDlTools.dlChapterAndPackIntoZip(
            File("${getExternalFilesDir("")}/$comicName/${i.hint}/${i.textOn}.zip"),
            tbtnUrlList[tbtnlist.indexOf(i)].substringAfterLast("/")
        )
    }

    @SuppressLint("SetTextI18n")
    fun addTbtn(title: String, url: String, caption: String) {
        if ((tbtncnt % btnNumPerRow == 0) || isNewTitle) {
            ltbtn = layoutInflater.inflate(R.layout.line_horizonal, ldwn, false)
            ldwn.addView(ltbtn)
            tbtncnt = 0
            isNewTitle = false
        }
        val tbv = layoutInflater.inflate(R.layout.button_tbutton, ltbtn.ltbtn, false)
        tbtnlist += tbv.tbtn
        tbtncnt++
        tbtnUrlList.add(url)
        val zipPosition = zipArrayList.size
        zipArrayList += "$title.zip"
        tbv.tbtn.textOff = title
        tbv.tbtn.textOn = title
        tbv.tbtn.text = title
        tbv.tbtn.hint = caption
        tbv.tbtn.layoutParams.width = btnw
        val zipf = File("${getExternalFilesDir("")}/$comicName/$caption/$title.zip")
        if (zipf.exists()) {
            tbv.tbtn.setBackgroundResource(R.drawable.rndbg_checked)
            tbv.tbtn.isChecked = false
            tbv.tbtn.freezesText = true
        }
        ltbtn.ltbtn.addView(tbv)
        ltbtn.invalidate()
        tbv.tbtn.setOnClickListener {
            val normalAct = (multiSelect && zipf.exists()) || !zipf.exists()
            if (zipf.exists() && !it.tbtn.isChecked) it.tbtn.setBackgroundResource(R.drawable.rndbg_checked)
            else if(normalAct) it.tbtn.setBackgroundResource(R.drawable.toggle_button)
            if(normalAct){
                if (it.tbtn.isChecked) tdwn.text = "$dldChapter/${++checkedChapter}"
                else tdwn.text = "$dldChapter/${--checkedChapter}"
            }else if(it.tbtn.isChecked){
                it.tbtn.isChecked = false
                callVM(title, zipf, zipPosition)
            }
        }
        tbv.tbtn.setOnLongClickListener {
            if (zipf.exists()) {
                toolsBox.buildInfo("确认删除这些章节?",
                    "该操作将不可撤销",
                    "确定",
                    null,
                    "取消",
                    {
                        if (checkedChapter == 0) {
                            it.tbtn.isChecked = true
                            tdwn.text = "$dldChapter/${++checkedChapter}"
                        }
                        handler.sendEmptyMessage(7)
                    })
            }
            true
        }
    }

    fun deleteChapters() {
        for (i in tbtnlist) {
            if (i.isChecked) {
                val f = File("${getExternalFilesDir("")}/$comicName/${i.hint}/${i.textOn}.zip")
                if (f.exists()) {
                    deleteChapter(f, i)
                    checkedChapter--
                }
            }
        }
        handler.sendEmptyMessage(6)
    }

    private fun callVM(titleText: String, zipFile: File, zipPosition:Int){
        ViewMangaActivity.titleText = titleText
        ViewMangaActivity.zipFile = zipFile
        ViewMangaActivity.zipList = zipArrayList
        ViewMangaActivity.zipPosition = zipPosition
        ViewMangaActivity.cd = zipFile.parentFile
        startActivity(Intent(this, ViewMangaActivity::class.java))
    }

    private fun deleteChapter(f: File, v: ToggleButton) {
        f.delete()
        v.setBackgroundResource(R.drawable.toggle_button)
        v.isChecked = false
    }

    @SuppressLint("SetTextI18n")
    fun updateProgressBar() {
        tdwn.text = "${++dldChapter}/$checkedChapter"
        setProgress2(dldChapter * 100 / checkedChapter, 233)
    }

    fun updateProgressBar(pageNow: Int, size: Int) {
        val delta = 100 / checkedChapter
        val start = dldChapter * delta
        val now = pageNow * delta / size
        setProgress2(start + now, 64)
    }

    fun setProgress2(end: Int, duration: Long) {
        ObjectAnimator.ofInt(
            pdwn,
            "progress",
            pdwn.progress,
            end
        ).setDuration(duration).start()
    }

    companion object {
        var comicName = "Null"
        var json: String? = null
    }
}