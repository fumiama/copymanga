package top.fumiama.copymanga.ui.vm

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Service
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
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
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
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
import top.fumiama.copymanga.template.general.TitleActivityTemplate
import top.fumiama.copymanga.template.http.AutoDownloadThread
import top.fumiama.copymanga.tools.api.CMApi
import top.fumiama.copymanga.tools.http.DownloadTools
import top.fumiama.copymanga.tools.thread.TimeThread
import top.fumiama.copymanga.views.ScaleImageView
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.lang.Thread.sleep
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.FutureTask
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
    //private var useFullScreen = false
    var r2l = true
    var currentItem = 0
    var verticalLoadMaxCount = 20
    private var notUseVP = true
    private var isVertical = false
    private var q = 100
    private val size get() = if(realCount / verticalLoadMaxCount > currentItem / verticalLoadMaxCount) verticalLoadMaxCount else realCount % verticalLoadMaxCount
    var infoDrawerDelta = 0f
    var pageNum: Int
        get() = getPageNumber()
        set(value) = setPageNumber(value)
    //var pn = 0
    private val isPnValid: Boolean get(){
        if(pn == -2) pn = realCount
        return intent.getStringExtra("function") == "log" && pn > 0
    }
    private var tasks: Array<FutureTask<ByteArray?>?>? = null
    private var destroy = false
    private var cut = false
    private var isCut = booleanArrayOf()
    private var indexMap = intArrayOf()
    private var volTurnPage = false
    private var am: AudioManager? = null
    private var pm: PagesManager? = null
    val realCount get() = if(cut) indexMap.size else count

    @ExperimentalStdlibApi
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_viewmanga)
        super.onCreate(savedInstanceState)
        va = WeakReference(this)
        //progressLog = PropertiesTools(File("$filesDir/progress/${chapter2Return?.results?.chapter?.comic_id}"))
        //dlZip2View = intent.getStringExtra("callFrom") == "Dl" || p["dlZip2View"] == "true"
        //zipFirst = intent.getStringExtra("callFrom") == "zipFirst"
        cut = p["useCut"] == "true"
        r2l = p["r2l"] == "true"
        verticalLoadMaxCount = if (p["verticalMax"] != "null") p["verticalMax"].toInt() else 20
        isVertical = p["vertical"] == "true"
        notUseVP = p["noVP"] == "true" || isVertical
        //url = intent.getStringExtra("url")
        handler = VMHandler(this, if(urlArray.isNotEmpty()) urlArray[position] else "")
        if (p["quality"] != "null") q = p["quality"].toInt()
        tt = TimeThread(handler, 22)
        tt.canDo = true
        tt.start()
        volTurnPage = p["volturn"] == "true"
        am = getSystemService(Service.AUDIO_SERVICE) as AudioManager

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
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R)
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        else {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.hide(WindowInsets.Type.statusBars())
            //window.insetsController?.hide(WindowInsets.Type.navigationBars())
        }
    }

    @ExperimentalStdlibApi
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        var flag = false
        if(volTurnPage) when(keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                pm?.toPage(false)
                flag = true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                pm?.toPage(true)
                flag = true
            }
        }
        return if(flag) true else super.onKeyDown(keyCode, event)
    }

    private fun alertCellar() {
        toolsBox.buildInfo("注意", "要使用使用流量观看吗？", "确定", "不再提醒", "取消", {handler.startLoad()}, { noCellarAlert = true; handler.startLoad()}, {finish()})
    }

    fun restorePN(){
        if (isPnValid) {
            isInScroll = false
            pageNum = pn
            pn = -1
        }
        sendProgress()
    }

    private fun preDownloadChapterPages() {
        getImgUrlArray()?.let {
            val mid = (if(pn in 1 until realCount) (if(cut) Math.abs(indexMap[pn]) else pn) else if(pn == -2 || pn >= realCount) it.size else 1) - 1
            val left = if(isVertical && mid > verticalLoadMaxCount) (mid / verticalLoadMaxCount) * verticalLoadMaxCount else (mid-1)
            val right = if(isVertical) (mid / verticalLoadMaxCount + 1) * verticalLoadMaxCount else mid
            tasks = arrayOfNulls(it.size)
            Thread{
                for (i in right until it.size) {
                    if(destroy) break
                    tasks?.let { tasks ->
                        tasks[i] = DownloadTools.touch(it[i])
                        Thread.sleep(1000)
                    }
                }
            }.start()
            Thread.sleep(500)
            Thread{
                for (i in left downTo 0) {
                    if(destroy) break
                    tasks?.let { tasks ->
                        tasks[i] = DownloadTools.touch(it[i])
                        Thread.sleep(1000)
                    }
                }
            }.start()
        }
    }

    @ExperimentalStdlibApi
    private fun doPrepareWebImg() {
        getImgUrlArray()?.apply {
            if(cut) {
                handler.sendEmptyMessage(7)     //showDl
                isCut = BooleanArray(size)
                val analyzedCnt = BooleanArray(size)
                forEachIndexed{ i, it ->
                    if(it != null) {
                        Thread{
                            DownloadTools.getHttpContent(it, 1024)?.inputStream()?.let {
                                isCut[i] = canCut(it)
                                analyzedCnt[i] = true
                            }
                        }.start()
                        Thread.sleep(22)
                    }
                }
                while (analyzedCnt.count { it } != size) Thread.sleep(233)
                isCut.forEachIndexed { index, b ->
                    Log.d("MyVM", "[$index] cut: $b")
                    indexMap += index+1
                    if(b) indexMap += -(index+1)
                }
                handler.sendEmptyMessage(15)     //hideDl
            }
            count = size
            runOnUiThread { prepareItems() }
            preDownloadChapterPages()
        }
    }

    @ExperimentalStdlibApi
    fun initManga(){
        if (zipFile?.exists() != true) doPrepareWebImg()
        else prepareItems()
        if (!isVertical) restorePN()
    }

    @ExperimentalStdlibApi
    private fun prepareImgFromWeb() {
        if(toolsBox.netinfo == "移动数据") alertCellar()
        else handler.startLoad()
    }

    private fun canCut(inputStream: InputStream): Boolean{
        val op = BitmapFactory.Options()
        op.inJustDecodeBounds = true
        BitmapFactory.decodeStream(inputStream, null, op)
        Log.d("MyVM", "w: ${op.outWidth}, h: ${op.outHeight}")
        return op.outWidth.toFloat() / op.outHeight.toFloat() > 1
    }

    @ExperimentalStdlibApi
    fun countZipEntries(doWhenFinish : (count: Int) -> Unit) = Thread{
        if (zipFile != null) try {
            Log.d("Myvm", "zip: $zipFile")
            val zip = ZipFile(zipFile)
            count = zip.size()
            if(cut) zip.entries().toList().sortedBy{it.name.substringBefore('.').toInt()}.forEachIndexed { i, it ->
                val useCut = canCut(zip.getInputStream(it))
                isCut += useCut
                indexMap += i + 1
                if (useCut) indexMap += -(i + 1)
                Log.d("Myvm", "[$i] 分析: ${it.name}, cut: $useCut")
            }
        } catch (e: Exception) {
            runOnUiThread { toolsBox.toastError("统计zip图片数错误!") }
        }
        runOnUiThread {
            Log.d("Myvm", "开始加载控件")
            doWhenFinish(count)
        }
    }.start()

    private fun getPageNumber(): Int {
        return if (r2l && !notUseVP) realCount - vp.currentItem
        else (if (notUseVP) currentItem else vp.currentItem) + 1
    }

    private fun setPageNumber(num: Int) {
        if (r2l && !notUseVP) vp.currentItem = realCount - num
        else if (notUseVP) {
            if(isVertical){
                currentItem = num - 1
                val offset = currentItem % verticalLoadMaxCount
                Log.d("MyVM", "Current: $currentItem, Height: ${psivl.height}, scrollY: ${psivs.scrollY}")
                if (!isInScroll || isInSeek) psivs.scrollY = psivl.height * offset / size
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
        } else {
            Log.d("MyVM", "Set vp current: ${num-1}")
            var delta = num - 1 - vp.currentItem
            if(delta >= 1) Thread{
                while (delta-- > 0){
                    Thread.sleep(23)
                    runOnUiThread {
                        vp.currentItem++
                    }
                }
            }.start()
            else if(delta <= -1) Thread{
                while (delta++ < 0){
                    Thread.sleep(23)
                    runOnUiThread {
                        vp.currentItem--
                    }
                }
            }.start()
        }
    }

    fun clearImgOn(imgView: ScaleImageView){
        imgView.visibility = View.GONE
    }

    //private fun getTempFile(position: Int) = File(cacheDir, "$position")

    private fun getImgUrl(position: Int) = handler.manga?.results?.chapter?.let {
        it.contents[it.words.indexOf(position)].url
    }

    private fun getImgUrlArray() = handler.manga?.results?.chapter?.let{
            val re = arrayOfNulls<String>(it.contents.size)
            for(i in it.contents.indices) {
                re[i] = getImgUrl(i)
            }
            re
        }

    private fun cutBitmap(bitmap: Bitmap, isEnd: Boolean) = Bitmap.createBitmap(bitmap, if(!isEnd) 0 else (bitmap.width/2), 0, bitmap.width/2, bitmap.height)

    private fun loadImg(imgView: ScaleImageView, bitmap: Bitmap, isLast: Int = 0, useCut: Boolean, isLeft: Boolean){
        val bitmap2load = if(useCut) cutBitmap(bitmap, isLeft) else bitmap
        runOnUiThread {
            imgView.setImageBitmap(bitmap2load)
            if(isVertical){
                imgView.setHeight2FitImgWidth()
                if (isLast == 1) handler.sendEmptyMessage(8)
            }
        }
    }

    private fun loadImgUrlInto(imgView: ScaleImageView, url: String, isLast: Int = 0, useCut: Boolean, isLeft: Boolean){
        Log.d("MyVM", "Load from adt: $url")
        AutoDownloadThread(url) {
            it?.let { loadImg(imgView, BitmapFactory.decodeByteArray(it, 0, it.size), isLast, useCut, isLeft) }
        }.start()
    }

    fun loadImgOn(imgView: ScaleImageView, position: Int, isLast: Int = 0){
        Log.d("MyVM", "Load img: $position")
        val index2load = if(cut) Math.abs(indexMap[position]) -1 else position
        val useCut = cut && isCut[index2load]
        val isLeft = cut && indexMap[position] > 0
        if (zipFile?.exists() == true) getImgBitmap(index2load)?.let {
            loadImg(imgView, it, isLast, useCut, isLeft)
        }
        else {
            val re = tasks?.get(index2load)
            if (re != null) Thread{
                val data = re.get()
                if(data != null) {
                    loadImg(imgView, BitmapFactory.decodeByteArray(data, 0, data.size), isLast, useCut, isLeft)
                    Log.d("MyVM", "Load from task")
                }
                else getImgUrl(index2load)?.let { loadImgUrlInto(imgView, it, isLast, useCut, isLeft) }
            }.start()
            else getImgUrl(index2load)?.let { loadImgUrlInto(imgView, it, isLast, useCut, isLeft) }
        }
        imgView.visibility = View.VISIBLE
    }

    private fun loadOneImg() {
        loadImgOn(onei, currentItem)
        updateSeekBar()
    }

    private fun initImgList(){
        for (i in 0 until verticalLoadMaxCount) {
            val newImg = ScaleImageView(this)
            scrollImages += newImg
            psivl.addView(newImg)
        }
    }

    fun prepareLastPage(loadCount: Int, maxCount: Int){
        for (i in loadCount until maxCount) handler.obtainMessage(5, scrollImages[i]).sendToTarget()
        handler.dl?.hide()
    }

    private fun getImgBitmap(position: Int): Bitmap? =
        if (position >= count || position < 0) null
        else try {
            val zip = ZipFile(zipFile)
            if (q == 100) BitmapFactory.decodeStream(zip.getInputStream(zip.getEntry("${position}.webp")))
            else {
                val out = ByteArrayOutputStream()
                BitmapFactory.decodeStream(zip.getInputStream(zip.getEntry("${position}.webp")))?.compress(Bitmap.CompressFormat.JPEG, q, out)
                BitmapFactory.decodeStream(ByteArrayInputStream(out.toByteArray()))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "加载zip的${position}.webp错误", Toast.LENGTH_SHORT).show()
            null
        }

    private fun setIdPosition(position: Int) {
        infoDrawerDelta = position.toFloat()
        infcard.translationY = infoDrawerDelta
        Log.d("MyVM", "Set info drawer delta to $infoDrawerDelta")
    }

    @ExperimentalStdlibApi
    @SuppressLint("SetTextI18n")
    private fun prepareItems() {
        try {
            prepareVP()
            //if (!isVertical) restorePN()
            prepareInfoBar()
            if (notUseVP && !isVertical && !isPnValid) loadOneImg()
            prepareIdBtVH()
            toolsBox.dp2px(67)?.let { setIdPosition(it) }
            prepareIdBtCut()
            prepareIdBtVP()
            prepareIdBtLR()
            /*progressLog?.let {
                it["chapterId"] = hm.chapterId.toString()
                it["name"] = inftitle.ttitle.text
            }*/
        }catch (e: Exception) {
            e.printStackTrace()
            toolsBox.toastError("准备控件错误")
        }
    }

    private fun sendProgress() {
        handler.progressLog?.let {
            //it["chapterId"] = hm.chapterId.toString()
            it["page"] = pageNum.toString()
            //it["name"] = inftitle.ttitle.text
        }
    }

    private fun prepareIdBtCut() {
        idtbcut.isChecked = cut
        idtbcut.setOnClickListener {
            p["useCut"] = if (idtbcut.isChecked) "true" else "false"
            Toast.makeText(this, "下次浏览生效", Toast.LENGTH_SHORT).show()
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
            if (r2l && !isPnValid) vp.currentItem = realCount - 1
        }
    }

    fun updateSeekBar() {
        if (!isInSeek) hideObjs()
        updateSeekText()
        updateSeekProgress()
        sendProgress()
    }

    @SuppressLint("SetTextI18n")
    private fun prepareInfoBar() {
        oneinfo.alpha = 0F
        infseek.visibility = View.GONE
        isearch.visibility = View.GONE
        inftitle.ttitle.text = handler.manga?.results?.chapter?.name
        inftxtprogress.text = "$pageNum/$realCount"
        infseek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, isHuman: Boolean) {
                Log.d("MyVM", "seek to ${p1 * realCount / 100}")
                if (isHuman) {
                    if (p1 >= (pageNum + 1) * 100 / realCount) scrollForward()
                    else if (p1 < (pageNum - 1) * 100 / realCount) scrollBack()
                }
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {
                isInSeek = true
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                isInSeek = false
            }
        })
        isearch.setImageResource(R.drawable.ic_author)
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
            pm = PagesManager(WeakReference(this))
            vsps.setListener(object :SpringView.OnFreshListener{
                override fun onLoadmore() {
                    //scrollForward()
                    pm?.toPage(true)
                    vsps.onFinishFreshAndLoad()
                }
                override fun onRefresh() {
                    //scrollBack()
                    pm?.toPage(false)
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
                    val delta = (scrollY.toFloat() * size.toFloat() / psivl.height.toFloat() + 0.5).toInt() - currentItem % verticalLoadMaxCount
                    if(delta != 0 && !(delta > 0 && pageNum == size)) {
                        pageNum += delta
                        Log.d("MyVM", "Scroll to offset $delta")
                    }
                }
            }
        }
        idtbvh.setOnClickListener {
            p["vertical"] = if (idtbvh.isChecked) "true" else "false"
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
        inftxtprogress.text = "$pageNum/$realCount"
    }

    private fun updateSeekProgress() {
        infseek.progress = pageNum * 100 / realCount
    }

    override fun onDestroy() {
        dlhandler?.sendEmptyMessage(0)
        tt.canDo = false
        destroy = true
        dlhandler = null
        handler.destroy()
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
                val pos = if (r2l) realCount - position - 1 else position
                val index2load = if(cut) Math.abs(indexMap[pos]) -1 else pos
                val useCut = cut && isCut[index2load]
                val isLeft = cut && indexMap[pos] > 0
                if (zipFile?.exists() == true) getImgBitmap(index2load)?.let {
                    //Glide.with(this@ViewMangaActivity).load(if(useCut) cutBitmap(it, isLeft) else it).into(holder.itemView.onei)
                    holder.itemView.onei.setImageBitmap(if(useCut) cutBitmap(it, isLeft) else it)
                }
                else getImgUrl(index2load)?.let{
                    if(useCut){
                        val thisOneI = holder.itemView.onei
                        Glide.with(this@ViewMangaActivity)
                            .asBitmap()
                            .load(GlideUrl(it, CMApi.myGlideHeaders)
                            ).into(object : SimpleTarget<Bitmap>() {
                                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                    thisOneI.setImageBitmap(cutBitmap(resource, isLeft))
                                } })
                    } else Glide.with(this@ViewMangaActivity).load(GlideUrl(it, CMApi.myGlideHeaders)).into(holder.itemView.onei)
                }
            }

            override fun getItemCount(): Int {
                return realCount
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
        var noCellarAlert = false
    }
}