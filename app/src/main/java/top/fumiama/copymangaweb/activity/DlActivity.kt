package top.fumiama.copymangaweb.activity

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import com.google.gson.Gson
import top.fumiama.copymangaweb.R
import top.fumiama.copymangaweb.activity.MainActivity.Companion.mh
import top.fumiama.copymangaweb.activity.template.ToolsBoxActivity
import top.fumiama.copymangaweb.data.ComicStructure
import top.fumiama.copymangaweb.databinding.ActivityDlBinding
import top.fumiama.copymangaweb.handler.DlHandler
import top.fumiama.copymangaweb.tool.MangaDlTools
import top.fumiama.copymangaweb.tool.MangaDlTools.Companion.wmdlt
import top.fumiama.copymangaweb.view.ChapterToggleButton
import top.fumiama.copymangaweb.view.LazyScrollView
import top.fumiama.copymangaweb.web.JSHidden
import top.fumiama.copymangaweb.web.WebChromeClient
import java.io.File
import java.lang.Thread.sleep

class DlActivity : ToolsBoxActivity() {
    lateinit var mBinding: ActivityDlBinding
    private var tbtncnt = 0
    private var isNewTitle = false
    var haveSElectAll = false
    var checkedChapter = 0
    var dldChapter = 0
    var haveDlStarted = false
    private var btnNumPerRow = 4
    private lateinit var toggleButtonLine: View
    var tbtnlist: Array<ChapterToggleButton> = arrayOf()
    private val handler = DlHandler(this, Looper.myLooper()!!)
    private var btnw = 0
    private var cdwnWidth = 0
    private var canDl = false
    private lateinit var mangaDlTools: MangaDlTools
    var multiSelect = false

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityDlBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        mh?.saveUrlsOnly = true
        mangaDlTools = MangaDlTools(this)
        mBinding.dwh.apply { post {
            settings.userAgentString = getString(R.string.pc_ua)
            webChromeClient = WebChromeClient()
            setWebViewClient("h.js")
            loadJSInterface(JSHidden())
        } }
        handler.sendEmptyMessage(-2)        //setLayouts
    }

    override fun onDestroy() {
        mh?.saveUrlsOnly = false
        wmdlt?.get()?.exit = true
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun showDlCard() {
        ObjectAnimator.ofFloat(
            mBinding.dldlbar.csdwn,
            "translationX",
            cdwnWidth.toFloat() * 0.9f,
            0f
        ).setDuration(
            233
        ).start()
    }

    private fun hideDlCard() {
        ObjectAnimator.ofFloat(
            mBinding.dldlbar.csdwn,
            "translationX",
            0f,
            cdwnWidth.toFloat() * 0.9f
        ).setDuration(
            233
        ).start()
    }

    private fun fillChapters() {
        mangaDlTools.allocateChapterUrls(checkedChapter)
        for (i in tbtnlist) {
            if (i.isChecked) mangaDlTools.dlChapterUrl(i.url.toString())
        }
    }

    private fun dlThread(dlMethod: (i: ChapterToggleButton) -> Unit) {
        sleep(10000)
        for (i in tbtnlist) {
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

    @SuppressLint("SetTextI18n")
    fun setLayouts() {
        mBinding.dtitle.ttitle.apply { post { text = comicName } }
        val widthData = toolsBox.calcWidthFromDp(8, 64)
        btnNumPerRow = widthData[0]
        btnw = widthData[1]
        mBinding.dldlbar.csdwn.apply { post { viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                cdwnWidth = width
                viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        }) } }
        mBinding.dllazys.onScrollListener = object : LazyScrollView.OnScrollListener {
            override fun onBottom() {}
            override fun onScroll() { if (mBinding.dldlbar.csdwn.translationX == 0f) hideDlCard() }
            override fun onTop() {}
        }
        mBinding.dldlbar.cdwn.let { it.post {
            it.setOnClickListener {
                if (mBinding.dldlbar.csdwn.translationX != 0f) showDlCard()
                else if (checkedChapter == 0) hideDlCard()
                else {
                    mBinding.dldlbar.pdwn.progress = 0
                    if (canDl || checkedChapter == 0) canDl = false
                    else {
                        haveDlStarted = true
                        canDl = true
                        handler.sendEmptyMessage(9)     //set dl card color to red
                        Toast.makeText(this@DlActivity, "请耐心等待加载...", Toast.LENGTH_SHORT).show()
                        Thread {
                            fillChapters()
                            dlThread { downloadChapterPages(it) }
                        }.start()
                    }
                }
            }
            it.setOnLongClickListener {
                handler.sendEmptyMessage(4)
                return@setOnLongClickListener true
            }
        } }
        mBinding.dtitle.isearch.apply { post {
            setOnClickListener { showMultiSelectInfo() }
        } }
        Thread{ analyzeStructure() }.start()
    }

    private fun showMultiSelectInfo() {
        toolsBox.buildInfo("进入多选模式？", "确定后，长按下载条可选中全部漫画，而不仅限于未下载者；点击已下载漫画可进行选择。",
            "确定", null, "取消", { multiSelect = true })
    }

    private fun analyzeStructure() {
        ViewMangaActivity.zipList = arrayOf()
        Gson().fromJson(json?.reader(), Array<ComicStructure>::class.java)?.let {
            for (group in it) {
                val tc = layoutInflater.inflate(R.layout.line_caption, mBinding.ldwn, false)
                tc.findViewById<TextView>(R.id.tcptn).text = group.name
                mBinding.ldwn.apply { post {
                    addView(
                        tc,
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    )
                    addView(
                        layoutInflater.inflate(R.layout.div_h, mBinding.ldwn, false),
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    )
                } }
                isNewTitle = true
                for (chapter in group.chapters) addToggleButton(chapter.name, chapter.url, group.name)
            }
        }
        val mangaHome = File("${getExternalFilesDir("")}/$comicName")
        val jsonFile = File(mangaHome, "info.bin")
        if(!mangaHome.exists()) mangaHome.mkdirs()
        if(!(jsonFile.exists() && intent.getBooleanExtra("callFromDlList", false))) json?.let { jsonFile.writeText(it) }
    }

    private fun downloadChapterPages(i: ChapterToggleButton) {
        mangaDlTools.onDownloadedListener =
            object : MangaDlTools.OnDownloadedListener {
                override fun handleMessage(succeed: Boolean) {
                    handler.obtainMessage(if (succeed) 1 else -1, i.index, 0)
                        .sendToTarget()
                }
                override fun handleMessage(succeed: Boolean, pageNow: Int) {
                    handler.obtainMessage(
                        5,
                        i.index,
                        pageNow,
                        succeed
                    ).sendToTarget()
                }
                override fun handleMessage(pageNow: Int){
                    handler.obtainMessage(
                        10,
                        i.index,
                        pageNow
                    ).sendToTarget()
                }
            }
        i.hash?.let {
            mangaDlTools.dlChapterAndPackIntoZip(
                File("${getExternalFilesDir("")}/$comicName/${i.hint}/${i.textOn}.zip"),
                it
            )
        }
    }

    @SuppressLint("SetTextI18n")
    fun addToggleButton(title: String, url: String, caption: String) {
        if ((tbtncnt % btnNumPerRow == 0) || isNewTitle) {
            toggleButtonLine = layoutInflater.inflate(R.layout.line_horizonal, mBinding.ldwn, false)
            mBinding.ldwn.apply {
                val t = toggleButtonLine
                post { addView(t) }
            }
            tbtncnt = 0
            isNewTitle = false
        }
        val tbv = layoutInflater.inflate(R.layout.button_tbutton, toggleButtonLine.findViewById(R.id.ltbtn), false)
        val tbvTbtn = tbv.findViewById<ChapterToggleButton>(R.id.tbtn)?:return
        tbvTbtn.index = tbtnlist.size
        tbtnlist += tbvTbtn
        tbvTbtn.url = url
        tbtncnt++
        val zipPosition = ViewMangaActivity.zipList?.size
        ViewMangaActivity.zipList = ViewMangaActivity.zipList?.plus("$title.zip")
        tbvTbtn.textOff = title
        tbvTbtn.textOn = title
        tbvTbtn.text = title
        tbvTbtn.hint = caption
        tbvTbtn.layoutParams.width = btnw
        val zipFile = File("${getExternalFilesDir("")}/$comicName/$caption/$title.zip")
        if (zipFile.exists()) {
            tbvTbtn.setBackgroundResource(R.drawable.rndbg_checked)
            tbvTbtn.isChecked = false
            tbvTbtn.freezesText = true
        }
        toggleButtonLine.apply { post {
            findViewById<LinearLayout>(R.id.ltbtn)?.addView(tbv)
            invalidate()
        } }
        tbvTbtn.setOnClickListener { v ->
            val normalAct = (multiSelect && zipFile.exists()) || !zipFile.exists()
            val tbtn = v.findViewById<ChapterToggleButton>(R.id.tbtn)?:return@setOnClickListener
            if (zipFile.exists() && !tbtn.isChecked) tbtn.apply { post { setBackgroundResource(R.drawable.rndbg_checked) } }
            else if(normalAct) tbtn.apply { post { setBackgroundResource(R.drawable.toggle_button) } }
            if (normalAct) {
                mBinding.dldlbar.tdwn.apply {
                    if (tbtn.isChecked) post {
                        text = "$dldChapter/${++checkedChapter}"
                    } else post {
                        text = "$dldChapter/${--checkedChapter}"
                    }
                }
            } else if(tbtn.isChecked) {
                tbtn.apply { post {
                    isChecked = false
                    zipPosition?.let { Thread {
                        callVM(title, zipFile, it)
                    }.start() }
                } }
            }
        }
        tbvTbtn.setOnLongClickListener {
            if (zipFile.exists()) {
                toolsBox.buildInfo("确认删除这些章节?",
                    "该操作将不可撤销",
                    "确定",
                    null,
                    "取消",
                    {
                        if (checkedChapter == 0) {
                            tbvTbtn.apply { post { isChecked = true } }
                            mBinding.dldlbar.tdwn.apply { post {
                                text = "$dldChapter/${++checkedChapter}"
                            } }
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

    private fun callVM(titleText: String, zipFile: File, zipPosition:Int) {
        ViewMangaActivity.titleText = titleText
        ViewMangaActivity.zipFile = zipFile
        //ViewMangaActivity.zipList = zipArrayList
        ViewMangaActivity.zipPosition = zipPosition
        ViewMangaActivity.cd = zipFile.parentFile
        startActivity(Intent(this@DlActivity, ViewMangaActivity::class.java))
    }

    private fun deleteChapter(f: File, v: ToggleButton) {
        f.delete()
        v.apply { post{
            setBackgroundResource(R.drawable.toggle_button)
            isChecked = false
        } }
    }

    @SuppressLint("SetTextI18n")
    fun updateProgressBar() {
        mBinding.dldlbar.tdwn.apply { post {
            text = "${++dldChapter}/$checkedChapter"
        } }
        setProgress2(dldChapter * 100 / checkedChapter, 233)
    }

    fun updateProgressBar(pageNow: Int, size: Int) {
        val delta = 100 / checkedChapter
        val start = dldChapter * delta
        val now = pageNow * delta / size
        setProgress2(start + now, 64)
    }

    fun setProgress2(end: Int, duration: Long) {
        val pdwn = mBinding.dldlbar.pdwn
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