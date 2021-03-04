package top.fumiama.copymanga.ui.vm

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import android.widget.SeekBar
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.liaoinstan.springview.widget.SpringView
import kotlinx.android.synthetic.main.activity_viewmanga.*
import kotlinx.android.synthetic.main.line_header.view.*
import kotlinx.android.synthetic.main.page_imgview.*
import kotlinx.android.synthetic.main.page_imgview.view.*
import kotlinx.android.synthetic.main.page_scrollimgview.*
import kotlinx.android.synthetic.main.page_scrollimgview.view.*
import kotlinx.android.synthetic.main.widget_infodrawer.*
import kotlinx.android.synthetic.main.widget_infodrawer.view.*
import kotlinx.android.synthetic.main.widget_titlebar.*
import kotlinx.android.synthetic.main.widget_titlebar.view.*
import kotlinx.android.synthetic.main.widget_viewmangainfo.*
import top.fumiama.dmzj.copymanga.R
import top.fumiama.copymanga.template.TitleActivityTemplate
import top.fumiama.copymanga.tools.CMApi
import top.fumiama.copymanga.tools.DownloadTools
import top.fumiama.copymanga.tools.TimeThread
import top.fumiama.copymanga.views.ScaleImageView
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.ref.WeakReference
import java.util.*
import java.util.zip.ZipFile

class ViewMangaActivity : TitleActivityTemplate() {
    var count = 0
    private lateinit var handler: VMHandler
    lateinit var tt: TimeThread
    var clicked = false
    private var isInSeek = false
    private var isInScroll = true
    //private var progressLog: PropertiesTools? = null
    var scrollImages = arrayOf<ScaleImageView>()
    //var zipFirst = false
    private var useFullScreen = false
    var r2l = true
    var currentItem = 0
    var verticalLoadMaxCount = 40
    private var notUseVP = true
    private var isVertical = false
    private var q = 90
    private val size get() = if(count / verticalLoadMaxCount > currentItem / verticalLoadMaxCount) verticalLoadMaxCount else count % verticalLoadMaxCount
    var infoDrawerDelta = 0f
    var pageNum: Int
        get() = getPageNumber()
        set(value) = setPageNumber(value)
    //var pn = 0
    private val isPnValid: Boolean get(){
        if(pn == -2) pn = count
        return intent.getStringExtra("function") == "log" && pn > 0
    }

    @ExperimentalStdlibApi
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_viewmanga)
        super.onCreate(savedInstanceState)
        va = WeakReference(this)
        //progressLog = PropertiesTools(File("$filesDir/progress/${chapter2Return?.results?.chapter?.comic_id}"))
        //dlZip2View = intent.getStringExtra("callFrom") == "Dl" || p["dlZip2View"] == "true"
        //zipFirst = intent.getStringExtra("callFrom") == "zipFirst"
        useFullScreen = p["useFullScreen"] != "true"
        r2l = p["r2l"] == "true"
        isVertical = p["vertical"] == "true"
        notUseVP = p["noVP"] == "true" || isVertical
        //url = intent.getStringExtra("url")
        handler = VMHandler(this, if(urlArray.isNotEmpty()) urlArray[position] else "")
        if (p["quality"] != "null") q = p["quality"].toInt()
        if (p["verticalMax"] != "null") verticalLoadMaxCount = p["verticalMax"].toInt()
        tt = TimeThread(handler, 22)
        tt.canDo = true
        tt.start()

        Log.d("MyVM", "Now ZipFile is $zipFile")
        try {
            if (zipFile != null && zipFile?.exists() == true) {
                if (!handler.loadFromFile(zipFile!!)) prepareImgFromWeb()
            } else prepareImgFromWeb()
        } catch (e: Exception) {
            e.printStackTrace()
            toolsBox.toastError("加载漫画错误")
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if(useFullScreen) {
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R)
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            else {
                window.setDecorFitsSystemWindows(false)
                window.insetsController?.hide(WindowInsets.Type.statusBars())
                //window.insetsController?.hide(WindowInsets.Type.navigationBars())
            }
        }
    }

    fun restorePN(){
        if (isPnValid) {
            isInScroll = false
            pageNum = pn
            pn = -1
        }
        sendProgress()
    }

    @ExperimentalStdlibApi
    fun initManga(){
        prepareItems(count)
        if (!isVertical) restorePN()
    }

    @ExperimentalStdlibApi
    private fun prepareImgFromWeb() {
        handler.startLoad()
    }

    private fun getPageNumber(): Int {
        return if (r2l && !notUseVP) count - vp.currentItem
        else (if (notUseVP) currentItem else vp.currentItem) + 1
    }

    private fun setPageNumber(num: Int) {
        if (r2l && !notUseVP) vp.currentItem = count - num
        else if (notUseVP) {
            if(isVertical){
                currentItem = num - 1
                val delta = currentItem % verticalLoadMaxCount
                Log.d("MyVM", "Height: ${psivl.height}, scrollY: ${psivs.scrollY}")
                if (!isInScroll || isInSeek) psivs.scrollY = psivl.height / size * delta
                updateSeekBar()
            }
            else {
                currentItem = num - 1
                try {
                    loadOneImg()
                } catch (e: Exception) {
                    e.printStackTrace()
                    toolsBox.toastError("页数${currentItem}不合法")
                }
            }
        } else vp.currentItem = num - 1
    }

    fun clearImgOn(imgView: ScaleImageView){
        imgView.visibility = View.GONE
    }

    private fun getTempFile(position: Int) = File(cacheDir, "$position")

    private fun getImgUrl(position: Int) = handler.manga?.results?.chapter?.let {
        it.contents[it.words.indexOf(position)].url
    }

    fun loadImgOn(imgView: ScaleImageView, position: Int){
        if (zipFile?.exists() == true) imgView.setImageBitmap(getImgBitmap(position))
        else if(isVertical) {
            val f = getTempFile(position)
            if(DownloadTools.downloadUsingUrlRet(getImgUrl(position), f))
                imgView.setImageBitmap(BitmapFactory.decodeFile(f.path))
            else Toast.makeText(this, "下载第${position}页失败", Toast.LENGTH_SHORT).show()
        }
        else Glide.with(this)
            .load(GlideUrl(getImgUrl(position), CMApi.myGlideHeaders))
            .timeout(10000)
            .into(imgView)
        imgView.visibility = View.VISIBLE
    }

    private fun loadOneImg() {
        loadImgOn(onei, currentItem)
        updateSeekBar()
    }

    private fun initImgList(){
        for (i in 0..39) {
            val newImg = ScaleImageView(this)
            scrollImages += newImg
            psivl.addView(newImg)
        }
    }

    fun prepareLastPage(loadCount: Int, maxCount: Int){
        for (i in loadCount until maxCount) handler.obtainMessage(5, scrollImages[i]).sendToTarget()
        handler.dl?.hide()
    }

    private fun getImgBitmap(position: Int): Bitmap? {
        Log.d("MyVM", "Get bitmap @$position, count is $count")
        if (position >= count || position < 0) return null
        else {
            val zip = ZipFile(zipFile)
            if (q == 100) return BitmapFactory.decodeStream(zip.getInputStream(zip.getEntry("${position}.jpg")))
            else {
                val out = ByteArrayOutputStream()
                try {
                    BitmapFactory.decodeStream(zip.getInputStream(zip.getEntry("${position}.webp")))
                } catch (e: Exception) {
                    e.printStackTrace()
                    return null
                }?.compress(Bitmap.CompressFormat.JPEG, q, out)
                return BitmapFactory.decodeStream(ByteArrayInputStream(out.toByteArray()))
            }
        }
    }

    private fun setIdPosition(position: Int) {
        infoDrawerDelta = position.toFloat()
        infcard.translationY = infoDrawerDelta
        Log.d("MyVM", "Set info drawer delta to $infoDrawerDelta")
    }

    @ExperimentalStdlibApi
    @SuppressLint("SetTextI18n")
    private fun prepareItems(size: Int) {
        ttitle.text = handler.manga?.results?.chapter?.name
        prepareVP()
        prepareInfoBar(size)
        if (notUseVP && !isVertical) loadOneImg()
        prepareIdBtVH()
        toolsBox.dp2px(67)?.let { setIdPosition(it) }
        prepareIdBtFullScreen()
        prepareIdBtVP()
        prepareIdBtLR()
        handler.progressLog?.let {
            //it["uuid"] = handler.manga?.results?.comic?.uuid
            it["name"] = inftitle.ttitle.text
        }
    }

    private fun sendProgress() {
        handler.progressLog?.let {
            //it["chapterId"] = hm.chapterId.toString()
            it["page"] = pageNum.toString()
            //it["name"] = inftitle.ttitle.text
        }
    }

    private fun prepareIdBtLR() {
        idtblr.isChecked = r2l
        idtblr.setOnClickListener {
            if (idtblr.isChecked) p["r2l"] = "true"
            else p["r2l"] = "false"
            Toast.makeText(this, "下次浏览生效", Toast.LENGTH_SHORT).show()
        }
    }

    private fun prepareIdBtVP() {
        idtbvp.isChecked = notUseVP
        idtbvp.setOnClickListener {
            if (idtbvp.isChecked) p["noVP"] = "true"
            else p["noVP"] = "false"
            Toast.makeText(this, "下次浏览生效", Toast.LENGTH_SHORT).show()
        }
    }

    private fun prepareVP() {
        if (notUseVP) {
            vp.visibility = View.GONE
            if(!isVertical) vone.visibility = View.VISIBLE
        } else {
            vp.visibility = View.VISIBLE
            vone.visibility = View.GONE
            vp.adapter = ViewData(vp).RecyclerViewAdapter()
            vp.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    updateSeekBar()
                    super.onPageSelected(position)
                }
            })
            if (r2l) vp.currentItem = count - 1
        }
    }

    private fun updateSeekBar() {
        if (!isInSeek) hideObjs()
        updateSeekText()
        updateSeekProgress()
        sendProgress()
    }

    @SuppressLint("SetTextI18n")
    private fun prepareInfoBar(size: Int) {
        oneinfo.alpha = 0F
        infseek.visibility = View.GONE
        isearch.visibility = View.GONE
        inftitle.ttitle.text = handler.manga?.results?.chapter?.name
        inftxtprogress.text = "$pageNum/$size"
        infseek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, isHuman: Boolean) {
                if (isHuman) {
                    if (p1 >= (pageNum + 1) * 100 / size) scrollForward()
                    else if (p1 < (pageNum - 1) * 100 / size) scrollBack()
                }
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {
                isInSeek = true
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                isInSeek = false
            }
        })
        isearch.setOnClickListener {
            handler.sendEmptyMessage(3)
        }
    }

    @ExperimentalStdlibApi
    private fun prepareIdBtVH() {
        idtbvh.isChecked = isVertical
        if (isVertical) {
            val vsps = vsp as SpringView
            vsps.footerView.lht.text = "更多"
            vsps.headerView.lht.text = "更多"
            val pm = PagesManager(WeakReference(this))
            vsps.setListener(object :SpringView.OnFreshListener{
                override fun onLoadmore() {
                    //scrollForward()
                    pm.toPage(true)
                    vsps.onFinishFreshAndLoad()
                }
                override fun onRefresh() {
                    //scrollBack()
                    pm.toPage(false)
                    vsps.onFinishFreshAndLoad()
                }
            })
            vp.visibility = View.GONE
            vsp.visibility = View.VISIBLE
            initImgList()
            handler.sendEmptyMessage(if(isPnValid)14 else 10)
            psivs.setOnScrollChangeListener { _, _, scrollY, _, _ ->
                isInScroll = true
                if(!isInSeek){
                    val newCurrent = (scrollY.toFloat() * size.toFloat() / psivl.height.toFloat() + 0.5).toInt()
                    pageNum += newCurrent - currentItem % verticalLoadMaxCount
                }
            }
        }
        idtbvh.setOnClickListener {
            p["vertical"] = if (idtbvh.isChecked) "true" else "false"
            Toast.makeText(this, "下次浏览生效", Toast.LENGTH_SHORT).show()
        }
    }

    private fun prepareIdBtFullScreen() {
        idtbfullscreen.isChecked = !useFullScreen
        idtbfullscreen.setOnClickListener {
            p["useFullScreen"] = if (idtbfullscreen.isChecked) "true" else "false"
            Toast.makeText(this, "下次浏览生效", Toast.LENGTH_SHORT).show()
        }
    }

    fun scrollBack() {
        isInScroll = false
        if(isVertical && (pageNum-1) % verticalLoadMaxCount == 0){
            Log.d("MyVM", "Do scroll back, isVertical: $isVertical, pageNum: $pageNum")
            handler.obtainMessage(9, currentItem - verticalLoadMaxCount, 0).sendToTarget()    //loadImgsIntoLine(currentItem - verticalLoadMaxCount)
            psivl.postDelayed({ pageNum-- }, 233)
        }else pageNum--
    }

    fun scrollForward() {
        isInScroll = false
        pageNum++
        if(isVertical && (pageNum-1) % verticalLoadMaxCount == 0) handler.sendEmptyMessage(10)
    }



    @SuppressLint("SetTextI18n")
    private fun updateSeekText() {
        inftxtprogress.text = "$pageNum/$count"
    }

    private fun updateSeekProgress() {
        infseek.progress = pageNum * 100 / count
    }

    override fun onDestroy() {
        dlhandler?.sendEmptyMessage(0)
        tt.canDo = false
        dlhandler = null
        super.onDestroy()
    }

    inner class ViewData(itemView: View) : RecyclerView.ViewHolder(itemView) {
        inner class RecyclerViewAdapter :
            RecyclerView.Adapter<ViewData>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewData {
                return ViewData(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.page_imgview, parent, false)
                )
            }

            @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
            override fun onBindViewHolder(holder: ViewData, position: Int) {
                val pos = if (r2l) count - position - 1 else position
                if (zipFile?.exists() == true) getImgBitmap(pos)?.let {
                    Glide.with(this@ViewMangaActivity).load(it)
                        //.thumbnail(Glide.with(this@ViewMangaActivity).load(R.drawable.load))
                        .into(holder.itemView.onei)
                    //holder.itemView.onei.setImageBitmap(it)
                }
                else Glide.with(this@ViewMangaActivity).load(
                    GlideUrl(getImgUrl(pos), CMApi.myGlideHeaders))
                    .timeout(10000)
                    //.thumbnail(Glide.with(this@ViewMangaActivity).load(R.drawable.load))
                    .into(holder.itemView.onei)
            }

            override fun getItemCount(): Int {
                return count
            }
        }
    }

    fun showObjs() {
        infseek.visibility = View.VISIBLE
        isearch.visibility = View.VISIBLE
        ObjectAnimator.ofFloat(
            oneinfo,
            "alpha",
            oneinfo.alpha,
            1F
        ).setDuration(233).start()
        clicked = true
    }

    fun hideObjs() {
        ObjectAnimator.ofFloat(
            oneinfo,
            "alpha",
            oneinfo.alpha,
            0F
        ).setDuration(233).start()
        clicked = false
        infseek.postDelayed({
            infseek.visibility = View.GONE
            isearch.visibility = View.GONE
        }, 300)
        handler.sendEmptyMessage(1)
    }

    companion object {
        var comicName: String? = null
        var urlArray = arrayOf<String>()
        var fileArray = arrayOf<File>()
        var position = 0
        var zipFile: File? = null
        var dlhandler: Handler? = null
        var va: WeakReference<ViewMangaActivity>? = null
        var pn = 0
    }
}