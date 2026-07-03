package top.fumiama.copymangaweb.activity

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import top.fumiama.copymangaweb.R
import top.fumiama.copymangaweb.activity.MainActivity.Companion.wm
import top.fumiama.copymangaweb.activity.template.ToolsBoxActivity
import top.fumiama.copymangaweb.databinding.ActivityViewmangaBinding
import top.fumiama.copymangaweb.handler.TimeThread
import top.fumiama.copymangaweb.tool.PropertiesTools
import top.fumiama.copymangaweb.tool.ToolsBox
import top.fumiama.copymangaweb.view.ScaleImageView
import top.fumiama.copymangaweb.web.JSHidden
import top.fumiama.copymangaweb.web.WebChromeClient
import java.io.File
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.max
import kotlin.math.min
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

class ViewMangaActivity : ToolsBoxActivity() {
    lateinit var handler: Handler
    lateinit var tt: TimeThread
    lateinit var mBinding: ActivityViewmangaBinding

    var count = 0
    var clicked = false
    var r2l = true
    var infoDrawerDelta = 0f

    private var dialog: Dialog? = null
    private lateinit var p: PropertiesTools
    private var isInSeek = false
    private var currentItem = 0
    private var notUseVP = true
    private var mangaZip = zipFile
    val dlZip2View = mangaZip != null
    private var streamUrl = streamChapterUrl
    private var readerPrepared = false
    private var streamFinished = false
    private var streamDeclaredCount = 0
    private val streamSeenUrls = LinkedHashSet<String>()
    private var vpAdapter: RecyclerView.Adapter<ViewData>? = null
    private val volTurnPage get() = p["volturn"] == "true"
    var pageNum = 1
        get() {
            field = getPageNumber()
            return field
        }
        set(value) {
            setPageNumber(value)
            if (notUseVP) {
                //currentItem += delta
                try {
                    loadOneImg()
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                    toolsBox.toastError("页数${currentItem}不合法")
                }
            }// else vp.currentItem += delta
            field = getPageNumber()
        }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityViewmangaBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        va = WeakReference(this)
        p = PropertiesTools(File("$filesDir/settings.properties"))
        r2l = p["r2l"] == "true"
        notUseVP = p["noAnimation"] == "true"
        handler = MyHandler(toolsBox)
        tt = TimeThread(handler, 22)
        tt.canDo = true
        tt.start()
        dialog = Dialog(this)
        dialog?.apply {
            setContentView(R.layout.dialog_unzipping)
            show()
        }
        mBinding.oneinfo.inftitle.ttitle.apply { post { text = titleText } }
        Log.d("MyVM", "dlZip2View: $dlZip2View, mangaZip: $mangaZip, streamUrl: $streamUrl")
        if(dlZip2View && mangaZip?.exists() != true) toolsBox.toastError("已经到头了~")
        else if(!dlZip2View && !streamUrl.isNullOrBlank()) startStreamingCollector(streamUrl!!)
        else Thread {
            try {
                count = if (dlZip2View) countZipItems() else imgUrls.size
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread { toolsBox.toastError("分析图片url错误") }
            }
            runOnUiThread { prepareReaderIfNeeded() }
        }.start()
    }


    private fun startStreamingCollector(url: String) {
        Log.d("CopymangaDL", "reader collector load url=$url")
        mBinding.wcollector.apply { post {
            settings.userAgentString = getString(R.string.pc_ua)
            webChromeClient = WebChromeClient()
            setWebViewClient("h.js")
            loadJSInterface(
                JSHidden(
                    onLoadChapter = { onStreamingChapterFinished(it) },
                    onChapterMeta = { onStreamingMeta(it) },
                    onAppendImages = { appendStreamingImages(it) },
                    onFinishStreaming = { onStreamingFinished() },
                    onChapterCount = { onStreamingCount(it) },
                    enableLoadingDialog = false
                )
            )
            loadUrl(url)
        } }
    }

    private fun onStreamingMeta(headerString: String) {
        val lines = headerString.split('\n')
        if (lines.size < 3) return
        titleText = lines[0].substringBeforeLast(' ')
        nextChapterUrl = lines[1].let { if(it == "null") null else it }
        previousChapterUrl = lines[2].let { if(it == "null") null else it }
        runOnUiThread { mBinding.oneinfo.inftitle.ttitle.text = titleText }
    }
    private fun setEarlyTapLayerEnabled(enabled: Boolean) {
        mBinding.onec.apply { post {
            isClickable = enabled
            isFocusable = false
            setOnClickListener(if (enabled) View.OnClickListener {
                if (clicked) hideSettings() else showSettings()
            } else null)
        } }
    }

    private fun seekProgressToPage(progress: Int): Int {
        if (count <= 1) return 1
        return ((progress.coerceIn(0, 100) * (count - 1) + 50) / 100 + 1).coerceIn(1, count)
    }

    private fun onStreamingCount(total: Int) {
        if (total <= 0) return
        runOnUiThread {
            val oldCount = count
            streamDeclaredCount = max(streamDeclaredCount, total)
            count = max(count, streamDeclaredCount)
            if (!readerPrepared) {
                prepareReaderIfNeeded()
            } else {
                if (!notUseVP && count > oldCount) {
                    vpAdapter?.notifyItemRangeInserted(oldCount, count - oldCount)
                }
                syncSeekBarOnly()
            }
        }
    }

    private fun onStreamingChapterFinished(content: String) {
        val listChapter = content.split('\n')
        if (listChapter.size >= 3) onStreamingMeta(listChapter.take(3).joinToString("\n"))
        appendStreamingImages(listChapter.drop(3).filter { it.isNotBlank() }.toTypedArray())
        onStreamingFinished()
    }

    private fun onStreamingFinished() {
        streamFinished = true
        Log.d("CopymangaDL", "reader stream finished pages=$count")
        runOnUiThread {
            dialog?.dismiss()
            dialog = null
            syncSeekBarOnly()
        }
    }

    private fun appendStreamingImages(urls: Array<String>) {
        runOnUiThread {
            val oldCount = count
            val oldImageCount = imgUrls.size
            var added = 0
            for (url in urls) {
                if (streamSeenUrls.add(url)) {
                    imgUrls += url
                    added++
                }
            }
            if (added <= 0) return@runOnUiThread
            count = max(max(streamDeclaredCount, imgUrls.size), count)
            Log.d("CopymangaDL", "reader appended images added=$added total=$count images=${imgUrls.size}")
            if (!readerPrepared) prepareReaderIfNeeded()
            else {
                if (notUseVP) {
                    if (currentItem in oldImageCount until imgUrls.size) loadOneImg(hidePanel = false)
                    else syncSeekBarOnly()
                } else {
                    if (count > oldCount) vpAdapter?.notifyItemRangeInserted(oldCount, count - oldCount)
                    notifyVisiblePagesIfArrived(oldImageCount, imgUrls.size)
                    syncSeekBarOnly()
                }
                preloadAround(getLogicalCurrentItem())
            }
        }
    }

    private fun prepareReaderIfNeeded() {
        try {
            if (count <= 0) return
            prepareItems()
            if(pn > 0) {
                pageNum = pn
                pn = -1
            } else if(pn == -2){
                pageNum = count
                pn = -1
            }
        } catch (e: Exception) {
            e.printStackTrace()
            toolsBox.toastError("准备控件错误")
        } finally {
            if (streamUrl == null || count > 0) {
                dialog?.dismiss()
                dialog = null
            }
        }
    }

    private fun preloadAround(position: Int) {
        if (dlZip2View || position < 0 || imgUrls.isEmpty()) return
        val end = min(imgUrls.size - 1, position + 4)
        for (i in position + 1..end) {
            imgUrls.getOrNull(i)?.let { url ->
                Glide.with(this@ViewMangaActivity)
                    .load(toolsBox.resolution.wrap(url))
                    .preload()
            }
        }
    }

    private fun getLogicalCurrentItem(): Int {
        return if (notUseVP) currentItem
        else if (r2l) count - mBinding.vp.currentItem - 1
        else mBinding.vp.currentItem
    }

    private fun logicalToPagerPosition(logicalPosition: Int): Int {
        return if (r2l) count - logicalPosition - 1 else logicalPosition
    }

    private fun notifyVisiblePagesIfArrived(oldImageCount: Int, newImageCount: Int) {
        if (notUseVP || oldImageCount >= newImageCount || count <= 0) return
        val center = getLogicalCurrentItem().coerceIn(0, count - 1)
        val from = max(0, center - 1)
        val to = min(newImageCount - 1, center + 1)
        for (logicalPosition in from..to) {
            if (logicalPosition >= oldImageCount) {
                val pagerPosition = logicalToPagerPosition(logicalPosition)
                if (pagerPosition in 0 until count) vpAdapter?.notifyItemChanged(pagerPosition)
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) window.setDecorFitsSystemWindows(false)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        var flag = false
        if(volTurnPage) when(keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                scrollBack()
                flag = true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                scrollForward()
                flag = true
            }
        }
        return if(flag) true else super.onKeyDown(keyCode, event)
    }

    private fun getPageNumber(): Int {
        return if (r2l && !notUseVP) count - mBinding.vp.currentItem
        else (if (notUseVP) currentItem else mBinding.vp.currentItem) + 1
    }

    private fun setPageNumber(num: Int) {
        if (r2l && !notUseVP) mBinding.vp.apply { post { currentItem = count - num } }
        else if (notUseVP) currentItem = num - 1 else mBinding.vp.currentItem = num - 1
    }

    private fun getImgBitmap(position: Int): Bitmap? {
        if (position >= count || position < 0) return null
        val zipPath = mangaZip ?: return null
        return try {
            ZipFile(zipPath).use { zip ->
                // Older downloaded zips use 0.webp, 1.webp...
                // The large-chapter fix may use 000.webp, 001.webp... for stable sorting.
                // Support both formats and fall back to the sorted entry list.
                val entry = zip.getEntry("${position}.webp")
                    ?: zip.getEntry("%03d.webp".format(position))
                    ?: zip.entries().asSequence()
                        .filter { !it.isDirectory }
                        .sortedBy { it.name }
                        .elementAtOrNull(position)
                    ?: run {
                        Log.e("CopymangaDL", "zip entry missing position=$position file=$zipPath")
                        return null
                    }
                zip.getInputStream(entry).use { input ->
                    BitmapFactory.decodeStream(input)
                }
            }
        } catch (e: Exception) {
            Log.e("CopymangaDL", "decode zip image failed position=$position file=$zipPath", e)
            null
        }
    }

    private fun loadOneImg(hidePanel: Boolean = true) {
        if(dlZip2View) mBinding.vone.onei.apply { post { setImageBitmap(getImgBitmap(currentItem)) } }
        else {
            val url = imgUrls.getOrNull(currentItem)
            if (url.isNullOrBlank()) {
                mBinding.vone.onei.apply { post { setImageResource(R.drawable.ic_dl) } }
            } else {
                Glide.with(this@ViewMangaActivity)
                    .load(toolsBox.resolution.wrap(url))
                    .placeholder(R.drawable.ic_dl)
                    .dontAnimate()
                    .into(mBinding.vone.onei)
            }
        }
        updateSeekBar(hidePanel)
    }

    private fun setIdPosition(position: Int) {
        infoDrawerDelta = position.toFloat()
        mBinding.infcard.root.apply { post { translationY = infoDrawerDelta } }
    }

    @SuppressLint("SetTextI18n")
    private fun prepareItems() {
        if (count <= 0) return
        prepareVP()
        prepareInfoBar(count)
        if (notUseVP) loadOneImg() else prepareIdBtVH()
        toolsBox.dp2px(67)?.let { setIdPosition(it) }
        prepareIdBtVolTurn()
        prepareIdBtVP()
        prepareIdBtLR()
        readerPrepared = true
    }

    private fun prepareIdBtLR() {
        mBinding.infcard.idtblr.apply { post {
            isChecked = r2l
            setOnClickListener {
                if (mBinding.infcard.idtblr.isChecked) p["r2l"] = "true"
                else p["r2l"] = "false"
                Toast.makeText(this@ViewMangaActivity, "下次浏览生效", Toast.LENGTH_SHORT).show()
            }
        } }
    }

    private fun prepareIdBtVP() {
        mBinding.infcard.idtbvp.apply { post {
            isChecked = notUseVP
            setOnClickListener {
                if (mBinding.infcard.idtbvp.isChecked) p["noAnimation"] = "true"
                else p["noAnimation"] = "false"
                Toast.makeText(this@ViewMangaActivity, "下次浏览生效", Toast.LENGTH_SHORT).show()
            }
        } }
    }

    private fun prepareVP() {
        if (notUseVP) {
            mBinding.vp.apply { post { visibility = View.INVISIBLE } }
            mBinding.vone.root.apply { post { visibility = View.VISIBLE } }
        } else {
            mBinding.vp.apply { post {
                visibility = View.VISIBLE
                vpAdapter = ViewData(this).RecyclerViewAdapter()
                adapter = vpAdapter
                registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        val pos = if (r2l) count - position - 1 else position
                        preloadAround(pos)
                        updateSeekBar()
                        super.onPageSelected(position)
                    }
                })
                if (r2l) currentItem = count - 1
            } }
            mBinding.vone.root.apply { post { visibility = View.INVISIBLE } }
        }
    }

    private fun updateSeekBar(hidePanel: Boolean = true) {
        if (hidePanel && !isInSeek) hideSettings()
        syncSeekBarOnly()
    }

    private fun syncSeekBarOnly() {
        updateSeekText()
        updateSeekProgress()
    }

    @SuppressLint("SetTextI18n")
    private fun prepareInfoBar(size: Int) {
        mBinding.oneinfo.root.apply { post { alpha = 0F } }
        mBinding.oneinfo.infseek.apply { post {
            visibility = View.INVISIBLE
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(p0: SeekBar?, p1: Int, isHuman: Boolean) {
                    if (isHuman && count > 0) {
                        val targetPage = seekProgressToPage(p1)
                        if (targetPage != pageNum) pageNum = targetPage
                        updateSeekText()
                    }
                }

                override fun onStartTrackingTouch(p0: SeekBar?) {
                    isInSeek = true
                }

                override fun onStopTrackingTouch(p0: SeekBar?) {
                    p0?.let {
                        val targetPage = seekProgressToPage(it.progress)
                        if (targetPage != pageNum) pageNum = targetPage
                    }
                    isInSeek = false
                    updateSeekBar()
                }
            })
        } }
        mBinding.oneinfo.inftitle.isearch.apply { post {
            visibility = View.INVISIBLE
            setOnClickListener {
                this@ViewMangaActivity.handler.sendEmptyMessage(3)
            }
        } }
        mBinding.oneinfo.inftxtprogress.apply { post { text = "$pageNum/$size" } }
    }

    private fun prepareIdBtVH() {
        mBinding.infcard.idtbvh.apply { post {
            isChecked = p["vertical"] == "true"
            setOnClickListener {
                if (mBinding.infcard.idtbvh.isChecked) {
                    mBinding.vp.apply { post { orientation = ViewPager2.ORIENTATION_VERTICAL } }
                    p["vertical"] = "true"
                } else {
                    mBinding.vp.apply { post { orientation = ViewPager2.ORIENTATION_HORIZONTAL } }
                    p["vertical"] = "false"
                }
            }
            if (isChecked) mBinding.vp.apply { post {
                orientation = ViewPager2.ORIENTATION_VERTICAL
            } }
        } }
    }

    private fun prepareIdBtVolTurn() {
        mBinding.infcard.idtbvolturn.apply { post {
            isChecked = volTurnPage
            setOnClickListener {
                if (mBinding.infcard.idtbvolturn.isChecked) p["volturn"] = "true"
                else p["volturn"] = "false"
            }
        } }
    }

    private fun countZipItems(): Int {
        var c = 0
        try {
            val exist = mangaZip?.exists() == true
            if (!exist) return 0
            else {
                Log.d("Myvm", "zipf: $mangaZip")
                ZipFile(mangaZip).use { zip ->
                    c = zip.size()
                }
            }
        } catch (e: Exception) {
            runOnUiThread { toolsBox.toastError("读取zip错误!") }
        }
        return c
    }

    fun scrollBack() {
        if (pageNum > 1) pageNum--
    }

    fun scrollForward() {
        if (pageNum < count) pageNum++
        else if (!streamFinished && streamUrl != null) Toast.makeText(this, "后续页面仍在后台加载", Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("SetTextI18n")
    private fun updateSeekText() {
        mBinding.oneinfo.inftxtprogress.apply { post { text = "$pageNum/$count" } }
    }

    private fun updateSeekProgress() {
        if (isInSeek || count <= 0) return
        mBinding.oneinfo.infseek.apply { post {
            progress = if (count <= 1) 0 else ((pageNum - 1) * 100 / (count - 1)).coerceIn(0, 100)
        } }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        tt.canDo = false
        wm?.get()?.mBinding?.w?.goBack()
        super.onBackPressed()
    }

    override fun onDestroy() {
        tt.canDo = false
        handler.removeCallbacksAndMessages(null)
        if (streamUrl != null) streamChapterUrl = null
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
                holder.itemView.findViewById<ScaleImageView>(R.id.onei)?.let { oneImage ->
                    if(dlZip2View) getImgBitmap(pos)?.let {
                        //Glide.with(this@ViewMangaActivity).load(it).placeholder(R.drawable.bg_comment).into(holder.itemView.onei)
                        oneImage.setImageBitmap(it)
                    }
                    else imgUrls.getOrNull(pos)?.let { url ->
                        Glide.with(this@ViewMangaActivity)
                            .load(toolsBox.resolution.wrap(url)).placeholder(R.drawable.ic_dl)
                            .dontAnimate().timeout(10000)
                            .into(oneImage)
                        preloadAround(pos)
                    } ?: oneImage.setImageResource(R.drawable.ic_dl)
                }
            }

            override fun getItemCount(): Int {
                return count
            }
        }
    }

    fun showSettings() {
        mBinding.oneinfo.infseek.visibility = View.VISIBLE
        mBinding.oneinfo.inftitle.isearch.visibility = View.VISIBLE
        val v = mBinding.oneinfo.root
        ObjectAnimator.ofFloat(
            v,
            "alpha",
            v.alpha,
            1F
        ).setDuration(233).start()
        clicked = true
    }

    fun hideSettings() {
        val v = mBinding.oneinfo.root
        ObjectAnimator.ofFloat(
            v,
            "alpha",
            v.alpha,
            0F
        ).setDuration(233).start()
        clicked = false
        mBinding.oneinfo.infseek.postDelayed({
            mBinding.oneinfo.infseek.visibility = View.INVISIBLE
            mBinding.oneinfo.inftitle.isearch.visibility = View.INVISIBLE
        }, 300)
        handler.sendEmptyMessage(1)
    }

    class MyHandler(
        private val toolsBox: ToolsBox
    ) : Handler(Looper.myLooper()!!) {
        private var infoShown = false
        private var delta = -1f
            get() {
                if (field < 0) field = va?.get()?.infoDrawerDelta ?: 0f
                return field
            }

        @SuppressLint("SimpleDateFormat", "SetTextI18n")
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                1 -> if (infoShown) {
                    hideInfCard(); infoShown = false
                }
                2 -> if (!infoShown) {
                    showInfCard(); infoShown = true
                }
                3 -> infoShown = if (infoShown) {
                    hideInfCard(); false
                } else {
                    showInfCard(); true
                }
                22 -> (toolsBox.zis as? ViewMangaActivity)?.mBinding?.infcard?.idtime?.apply { post {
                    text = SimpleDateFormat("HH:mm")
                        .format(Date()) + toolsBox.week + toolsBox.netInfo
                } }
            }
        }

        private fun showInfCard() {
            Log.d("MyVM", "showInfCard delta $delta")
            va?.get()?.mBinding?.infcard?.apply {
                ObjectAnimator.ofFloat(idc, "alpha", 0.3F, 0.8F).setDuration(233).start()
                ObjectAnimator.ofFloat(root, "translationY", delta, 0F).setDuration(233).start()
            }
        }

        private fun hideInfCard() {
            Log.d("MyVM", "hideInfCard delta $delta")
            va?.get()?.mBinding?.infcard?.apply {
                ObjectAnimator.ofFloat(idc, "alpha", 0.8F, 0.3F).setDuration(233).start()
                ObjectAnimator.ofFloat(root, "translationY", 0F, delta).setDuration(233).start()
            }
        }
    }

    companion object {
        var va: WeakReference<ViewMangaActivity>? = null
        var imgUrls = arrayOf<String>()
        var zipFile: File? = null
        get() {
            val re = field
            if(field != null) field = null
            return re
        }
        var titleText = "Null"
        var nextChapterUrl: String? = null
        var previousChapterUrl: String? = null
        var zipPosition = 0
        var zipList: Array<String>? = null
        var cd: File? = null
        var pn = -1
        var streamChapterUrl: String? = null
    }
}