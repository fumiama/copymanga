package top.fumiama.copymangaweb.activity

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import top.fumiama.copymangaweb.R
import top.fumiama.copymangaweb.activity.reader.ContinuousMangaAdapter
import top.fumiama.copymangaweb.activity.reader.PagedMangaAdapter
import top.fumiama.copymangaweb.activity.reader.ReaderOverlayController
import top.fumiama.copymangaweb.activity.template.ToolsBoxActivity
import top.fumiama.copymangaweb.databinding.ActivityViewmangaBinding
import top.fumiama.copymangaweb.tool.PropertiesTools
import top.fumiama.copymangaweb.tool.PagesManager
import top.fumiama.copymangaweb.view.ScaleImageView
import top.fumiama.copymangaweb.web.JSHidden
import top.fumiama.copymangaweb.web.WebChromeClient
import java.io.File
import java.lang.ref.WeakReference
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min
import java.util.zip.ZipFile

class ViewMangaActivity : ToolsBoxActivity() {
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
    private var verticalReading = false
    private var mangaZip = zipFile
    val dlZip2View = mangaZip != null
    private var streamUrl = streamChapterUrl
    private var readerPrepared = false
    private var streamFinished = false
    private var streamDeclaredCount = 0
    private var backInvokedCallback: OnBackInvokedCallback? = null
    private val streamSeenUrls = LinkedHashSet<String>()
    private var pagedAdapter: PagedMangaAdapter? = null
    private var continuousAdapter: ContinuousMangaAdapter? = null
    private val imageExecutor = Executors.newFixedThreadPool(2)
    private lateinit var overlayController: ReaderOverlayController
    private val pagesManager by lazy { PagesManager(WeakReference(this)) }
    private val volTurnPage get() = p["volturn"] == "true"
    private val readerMode: ReaderMode
        get() = when {
            verticalReading -> ReaderMode.CONTINUOUS
            notUseVP -> ReaderMode.SINGLE_PAGE
            else -> ReaderMode.PAGED
        }
    var pageNum: Int
        get() = getPageNumber()
        set(value) {
            setPageNumber(value, smoothScroll = true)
            if (readerMode == ReaderMode.SINGLE_PAGE) {
                try {
                    loadOneImg()
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                    toolsBox.toastError("页数${currentItem}不合法")
                }
            }
        }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityViewmangaBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        registerBackCallback()
        va = WeakReference(this)
        p = PropertiesTools(File("$filesDir/settings.properties"))
        r2l = p["r2l"] == "true"
        notUseVP = p["noAnimation"] == "true"
        verticalReading = p["vertical"] == "true"
        overlayController = ReaderOverlayController(
            activity = this,
            binding = mBinding,
            toolsBox = toolsBox,
            drawerOffset = { infoDrawerDelta }
        ).also { it.start() }
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
            val initialCount = try {
                if (dlZip2View) countZipItems() else imgUrls.size
            } catch (e: Exception) {
                e.printStackTrace()
                0
            }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                count = initialCount
                if (initialCount == 0) toolsBox.toastError("分析图片url错误")
                prepareReaderIfNeeded()
            }
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
            if (isFinishing || isDestroyed) return@runOnUiThread
            val oldCount = count
            streamDeclaredCount = max(streamDeclaredCount, total)
            count = max(count, streamDeclaredCount)
            if (!readerPrepared) {
                prepareReaderIfNeeded()
            } else {
                if (readerMode != ReaderMode.SINGLE_PAGE && count > oldCount) {
                    notifyPagesInserted(oldCount, count - oldCount)
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
        runOnUiThread {
            if (isFinishing || isDestroyed) return@runOnUiThread
            streamFinished = true
            Log.d(
                "CopymangaDL",
                "reader stream finished declared=$streamDeclaredCount received=${imgUrls.size}"
            )
            prepareReaderIfNeeded()
            if (readerPrepared) {
                dialog?.dismiss()
                dialog = null
                syncSeekBarOnly()
            }
        }
    }

    private fun appendStreamingImages(urls: Array<String>) {
        runOnUiThread {
            if (isFinishing || isDestroyed) return@runOnUiThread
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
                if (readerMode == ReaderMode.SINGLE_PAGE) {
                    if (currentItem in oldImageCount until imgUrls.size) loadOneImg(hidePanel = false)
                    else syncSeekBarOnly()
                } else {
                    if (count > oldCount) notifyPagesInserted(oldCount, count - oldCount)
                    notifyVisiblePagesIfArrived(oldImageCount, imgUrls.size)
                    continuousAdapter?.notifyItemRangeChanged(
                        oldImageCount,
                        imgUrls.size - oldImageCount
                    )
                    syncSeekBarOnly()
                }
                preloadAround(getLogicalCurrentItem())
            }
        }
    }

    private fun prepareReaderIfNeeded() {
        if (
            readerPrepared ||
            count <= 0 ||
            isFinishing ||
            isDestroyed ||
            shouldWaitForLastStreamingPage()
        ) return
        readerPrepared = true
        try {
            prepareItems()
            setPageNumber(consumeRequestedPage(), smoothScroll = false)
            if (readerMode == ReaderMode.SINGLE_PAGE) loadOneImg()
            else syncSeekBarOnly()
        } catch (e: Exception) {
            readerPrepared = false
            e.printStackTrace()
            toolsBox.toastError("准备控件错误")
        } finally {
            if (streamUrl == null || count > 0) {
                dialog?.dismiss()
                dialog = null
            }
        }
    }

    private fun shouldWaitForLastStreamingPage(): Boolean {
        if (streamUrl == null || pn != LAST_PAGE) return false
        val allDeclaredPagesReceived =
            streamDeclaredCount <= 0 || imgUrls.size >= streamDeclaredCount
        return !streamFinished || !allDeclaredPagesReceived
    }

    private fun consumeRequestedPage(): Int {
        val requestedPage = when {
            pn == LAST_PAGE -> count
            pn > 0 -> pn.coerceAtMost(count)
            else -> 1
        }
        pn = FIRST_PAGE
        return requestedPage
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
        return when (readerMode) {
            ReaderMode.SINGLE_PAGE, ReaderMode.CONTINUOUS -> currentItem
            ReaderMode.PAGED -> mBinding.vp.currentItem
        }
    }

    private fun notifyVisiblePagesIfArrived(oldImageCount: Int, newImageCount: Int) {
        if (readerMode != ReaderMode.PAGED || oldImageCount >= newImageCount || count <= 0) return
        val center = getLogicalCurrentItem().coerceIn(0, count - 1)
        val from = max(0, center - 1)
        val to = min(newImageCount - 1, center + 1)
        for (logicalPosition in from..to) {
            if (logicalPosition >= oldImageCount) {
                pagedAdapter?.notifyItemChanged(logicalPosition)
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
        if (volTurnPage) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> pagesManager.goBackward()
                KeyEvent.KEYCODE_VOLUME_DOWN -> pagesManager.goForward()
                else -> return super.onKeyDown(keyCode, event)
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        exitByUserRequest()
    }

    private fun registerBackCallback() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        backInvokedCallback = OnBackInvokedCallback(::exitByUserRequest).also { callback ->
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                callback
            )
        }
    }

    private fun exitByUserRequest() {
        finishAfterTransition()
    }

    private fun getPageNumber(): Int {
        return when (readerMode) {
            ReaderMode.SINGLE_PAGE, ReaderMode.CONTINUOUS -> currentItem + 1
            ReaderMode.PAGED -> mBinding.vp.currentItem + 1
        }
    }

    private fun setPageNumber(num: Int, smoothScroll: Boolean) {
        val target = (num - 1).coerceIn(0, (count - 1).coerceAtLeast(0))
        when (readerMode) {
            ReaderMode.SINGLE_PAGE -> currentItem = target
            ReaderMode.CONTINUOUS -> {
                currentItem = target
                mBinding.continuousPages.scrollToPosition(target)
            }
            ReaderMode.PAGED -> mBinding.vp.setCurrentItem(
                target,
                smoothScroll
            )
        }
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
        mBinding.vone.onei.resetImageTransform()
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
        mBinding.vone.onei.setOnTapRegionListener(::onImageTapped)
        prepareVP()
        prepareInfoBar(count)
        toolsBox.dp2px(67)?.let { setIdPosition(it) }
        prepareIdBtVolTurn()
        prepareIdBtVH()
        prepareIdBtVP()
        prepareIdBtLR()
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
            isEnabled = !verticalReading
            setOnClickListener {
                if (mBinding.infcard.idtbvp.isChecked) p["noAnimation"] = "true"
                else p["noAnimation"] = "false"
                Toast.makeText(this@ViewMangaActivity, "下次浏览生效", Toast.LENGTH_SHORT).show()
            }
        } }
    }

    private fun prepareVP() {
        mBinding.vone.root.visibility = if (readerMode == ReaderMode.SINGLE_PAGE) View.VISIBLE else View.GONE
        mBinding.vp.visibility = if (readerMode == ReaderMode.PAGED) View.VISIBLE else View.GONE
        mBinding.continuousPages.visibility = if (readerMode == ReaderMode.CONTINUOUS) View.VISIBLE else View.GONE

        when (readerMode) {
            ReaderMode.SINGLE_PAGE -> Unit
            ReaderMode.PAGED -> mBinding.vp.apply {
                layoutDirection = if (r2l) {
                    View.LAYOUT_DIRECTION_RTL
                } else {
                    View.LAYOUT_DIRECTION_LTR
                }
                pagedAdapter = PagedMangaAdapter(
                    itemCountProvider = { count },
                    bindImage = ::bindPageImage
                )
                adapter = pagedAdapter
                registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        preloadAround(position)
                        updateSeekBar()
                        super.onPageSelected(position)
                    }
                })
            }
            ReaderMode.CONTINUOUS -> prepareContinuousReader()
        }
    }

    private fun prepareContinuousReader() {
        continuousAdapter = ContinuousMangaAdapter(
            itemCountProvider = { count },
            bindImage = ::bindPageImage
        )
        mBinding.continuousPages.apply {
            adapter = continuousAdapter
            itemAnimator = null
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    val manager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                    val firstVisible = manager.findFirstVisibleItemPosition()
                    if (firstVisible != RecyclerView.NO_POSITION) {
                        currentItem = firstVisible
                        syncSeekBarOnly()
                        preloadAround(firstVisible)
                    }
                }
            })
        }
    }

    private fun notifyPagesInserted(positionStart: Int, itemCount: Int) {
        if (itemCount <= 0) return
        pagedAdapter?.notifyItemRangeInserted(positionStart, itemCount)
        continuousAdapter?.notifyItemRangeInserted(positionStart, itemCount)
    }

    private fun bindPageImage(imageView: ScaleImageView, position: Int) {
        imageView.tag = position
        imageView.setOnTapRegionListener(::onImageTapped)
        if (dlZip2View) loadZipImage(imageView, position)
        else loadNetworkImage(imageView, position)
    }

    private fun onImageTapped(region: ScaleImageView.TapRegion) {
        when (region) {
            ScaleImageView.TapRegion.PREVIOUS -> {
                if (readerMode == ReaderMode.CONTINUOUS) pagesManager.goBackward()
                else pagesManager.toPreviousPage()
            }
            ScaleImageView.TapRegion.CENTER -> pagesManager.manageInfo()
            ScaleImageView.TapRegion.NEXT -> {
                if (readerMode == ReaderMode.CONTINUOUS) pagesManager.goForward()
                else pagesManager.toNextPage()
            }
        }
    }

    private fun loadNetworkImage(imageView: ScaleImageView, position: Int) {
        val url = imgUrls.getOrNull(position)
        if (url.isNullOrBlank()) {
            imageView.setImageResource(R.drawable.ic_dl)
            return
        }
        Glide.with(this)
            .load(toolsBox.resolution.wrap(url))
            .placeholder(R.drawable.ic_dl)
            .dontAnimate()
            .into(imageView)
        preloadAround(position)
    }

    private fun loadZipImage(imageView: ScaleImageView, position: Int) {
        imageView.setImageResource(R.drawable.ic_dl)
        imageExecutor.execute {
            val bitmap = getImgBitmap(position)
            imageView.post {
                if (!isFinishing && !isDestroyed && imageView.tag == position) {
                    imageView.setImageBitmap(bitmap)
                }
            }
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
            setOnClickListener { overlayController.toggleDrawer() }
        } }
        mBinding.oneinfo.inftxtprogress.apply { post { text = "$pageNum/$size" } }
    }

    private fun prepareIdBtVH() {
        mBinding.infcard.idtbvh.apply { post {
            isChecked = verticalReading
            setOnClickListener {
                p["vertical"] = isChecked.toString()
                Toast.makeText(this@ViewMangaActivity, "下次浏览生效", Toast.LENGTH_SHORT).show()
            }
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

    override fun onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            backInvokedCallback?.let(onBackInvokedDispatcher::unregisterOnBackInvokedCallback)
        }
        backInvokedCallback = null
        overlayController.close()
        imageExecutor.shutdownNow()
        pagedAdapter = null
        continuousAdapter = null
        mBinding.vp.adapter = null
        mBinding.continuousPages.adapter = null
        dialog?.dismiss()
        dialog = null
        mBinding.wcollector.destroy()
        if (streamUrl != null) streamChapterUrl = null
        if (va?.get() === this) va = null
        super.onDestroy()
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
            if (isFinishing || isDestroyed) return@postDelayed
            mBinding.oneinfo.infseek.visibility = View.INVISIBLE
            mBinding.oneinfo.inftitle.isearch.visibility = View.INVISIBLE
        }, 300)
        overlayController.hideDrawer()
    }

    companion object {
        const val FIRST_PAGE = -1
        const val LAST_PAGE = -2

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
        var zipList: Array<File>? = null
        var cd: File? = null
        var pn = FIRST_PAGE
        var streamChapterUrl: String? = null
    }

    private enum class ReaderMode {
        SINGLE_PAGE,
        PAGED,
        CONTINUOUS,
    }
}
