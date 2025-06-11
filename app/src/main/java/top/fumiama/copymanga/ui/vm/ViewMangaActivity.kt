package top.fumiama.copymanga.ui.vm

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Service
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.util.TypedValue
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.Button
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.liaoinstan.springview.widget.SpringView
import kotlinx.android.synthetic.main.activity_viewmanga.*
import kotlinx.android.synthetic.main.line_header.view.*
import kotlinx.android.synthetic.main.page_imgview.*
import kotlinx.android.synthetic.main.page_imgview.view.*
import kotlinx.android.synthetic.main.page_scrollimgview.*
import kotlinx.android.synthetic.main.widget_infodrawer.*
import kotlinx.android.synthetic.main.widget_titlebar.*
import kotlinx.android.synthetic.main.widget_titlebar.view.*
import kotlinx.android.synthetic.main.widget_viewmangainfo.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.fumiama.copymanga.api.Config
import top.fumiama.copymanga.view.template.TitleActivityTemplate
import top.fumiama.copymanga.net.template.PausableDownloader
import top.fumiama.copymanga.net.DownloadTools
import top.fumiama.copymanga.view.interaction.TimeThread
import top.fumiama.copymanga.view.Font
import top.fumiama.copymanga.view.ScaleImageView
import top.fumiama.dmzj.copymanga.R
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.lang.ref.WeakReference
import java.util.concurrent.FutureTask
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipFile
import kotlin.math.abs

class ViewMangaActivity : TitleActivityTemplate() {
    var count = 0
    private lateinit var mHandler: VMHandler
    lateinit var tt: TimeThread
    var clicked = 0
    private var isInSeek = false
    private var isInScroll = true
    var scrollImages = arrayOf<ScaleImageView>()
    var scrollButtons = arrayOf<Button>()
    var scrollPositions = arrayOf<Int>()
    //var zipFirst = false
    //private var useFullScreen = false
    var r2l = true
    var currentItem = 0
    var verticalLoadMaxCount = 20
    private var notUseVP = true
    private var isVertical = false
    private var q = 100
    private var tryWebpFirst = true
    private val size get() = if(realCount / verticalLoadMaxCount > currentItem / verticalLoadMaxCount) verticalLoadMaxCount else realCount % verticalLoadMaxCount
    var infoDrawerDelta = 0f
    var pageNum: Int
        get() = getPageNumber()
        set(value) = setPageNumber(value)
    //var pn = 0
    private val isPnValid: Boolean get() {
        val re = forceLetPNValid || if(pn == -2) {
            pn = 0
            true
        } else {
            intent.getStringExtra("function") == "log" && pn > 0
        }
        Log.d("MyVM", "isPnValid: $re")
        return re && pn <= realCount
    }
    private var forceLetPNValid: Boolean = false
        get() {
            if(!field) return false
            field = false
            return true
        }
    private var tasks: Array<FutureTask<ByteArray?>?>? = null
    private var tasksRunStatus: Array<Boolean>? = null
    private var destroy = false
    private var cut = false
    private var isCut = booleanArrayOf()
    private var indexMap = intArrayOf()
    private var volTurnPage = false
    private var am: AudioManager? = null
    var pm: PagesManager? = null
    private var fullyHideInfo = false
    val realCount get() = if(cut) indexMap.size else count

    var urlArray = arrayOf<String>()
    var uuidArray = arrayOf<String>()
    var position = 0
    var comicName: String? = null
    private var zipFile: File? = null
    var pn = 0

    private val loadImgOnWait = AtomicInteger()

    private var colorOnSurface: Int = 0
        get() {
            if (field != 0) return field
            val tv = TypedValue()
            field = if (theme.resolveAttribute(R.attr.colorOnSurface, tv, true)) {
                Log.d("MyVM", "resolve R.attr.colorOnSurface: ${tv.data}")
                tv.data
            } else {
                ContextCompat.getColor(applicationContext, R.color.material_on_surface_stroke)
            }
            return field
        }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        if (Config.view_manga_always_dark_bg.value) {
            Log.d("MyVM", "force dark")
            delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_YES
        } else {
            delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        postponeEnterTransition()
        setContentView(R.layout.activity_viewmanga)
        super.onCreate(null)
        va = WeakReference(this@ViewMangaActivity)
        //dlZip2View = intent.getStringExtra("callFrom") == "Dl" || p["dlZip2View"] == "true"
        //zipFirst = intent.getStringExtra("callFrom") == "zipFirst"
        intent.getStringArrayExtra("urlArray")?.let { urlArray = it }
        intent.getStringArrayExtra("uuidArray")?.let { uuidArray = it }
        position = intent.getIntExtra("position", 0)
        comicName = intent.getStringExtra("comicName")
        zipFile = intent.getStringExtra("zipFile")?.let { File(it) }
        pn = intent.getIntExtra("pn", 0)
        cut = pb["useCut"]
        r2l = pb["r2l"]
        verticalLoadMaxCount = Config.view_manga_vertical_max.value.let { if(it > 0) it else 20 }
        isVertical = pb["vertical"]
        notUseVP = pb["noVP"] || isVertical
        //url = intent.getStringExtra("url")
        mHandler = VMHandler(this@ViewMangaActivity, if(urlArray.isNotEmpty()) urlArray[position] else "", resources.getStringArray(R.array.weeks))
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                Config.view_manga_quality.value.let { q = if (it > 0) it else 100 }
                tt = TimeThread(mHandler, VMHandler.SET_NET_INFO, 10000)
                tt.canDo = true
                tt.start()
                volTurnPage = Config.view_manga_vol_turn.value
                am = getSystemService(Service.AUDIO_SERVICE) as AudioManager
                if (!noCellarAlert) noCellarAlert = Config.view_manga_use_cellar.value
                fullyHideInfo = Config.view_manga_hide_info.value

                Log.d("MyVM", "Now ZipFile is $zipFile")
                try {
                    if (zipFile != null && zipFile?.exists() == true) {
                        if (!mHandler.loadFromFile(zipFile!!)) prepareImgFromWeb()
                    } else prepareImgFromWeb()
                } catch (e: Exception) {
                    e.printStackTrace()
                    toolsBox.toastErrorAndFinish(R.string.load_manga_error)
                }
                withContext(Dispatchers.Main) {
                    startPostponedEnterTransition()
                    ObjectAnimator.ofFloat(vcp, "alpha", 0.1f, 1f).setDuration(1000).start()
                }
            }
        }
        if (Config.general_enable_transparent_system_bar.value) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R)
                window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
    }

    @Suppress("DEPRECATION")
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R)
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        else {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.apply {
                hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
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

    private suspend fun alertCellar() = withContext(Dispatchers.Main) {
        toolsBox.buildInfo(
            "注意", "要使用使用流量观看吗？", "确定", "本次阅读不再提醒", "取消",
            { mHandler.startLoad() }, { noCellarAlert = true; mHandler.startLoad() }, { finish() }
        )
    }

    suspend fun restorePN() = withContext(Dispatchers.Main) {
        if (isPnValid) {
            isInScroll = false
            pageNum = pn
            Log.d("MyVM", "restore pageNum to $pn")
            pn = -1
        }
        setProgress()
    }

    private fun prepareDownloadTasks() {
        getImgUrlArray()?.let {
            tasks = Array(it.size) { i ->
                val u = it[i]?:return@Array null
                return@Array DownloadTools.prepare(Config.resolution.wrap(Config.imageProxy?.wrap(u)?:u))
            }
            tasksRunStatus = Array(it.size) { return@Array false }
        }
    }

    @ExperimentalStdlibApi
    private suspend fun doPrepareWebImg() = withContext(Dispatchers.IO) {
        getImgUrlArray()?.apply {
            if(cut) {
                Log.d("MyVM", "is cut, load all pages...")
                mHandler.sendEmptyMessage(VMHandler.DIALOG_SHOW)     // showDl
                isCut = BooleanArray(size)
                forEachIndexed { i, it ->
                    mHandler.obtainMessage(VMHandler.SET_DL_TEXT, "$i/$size").sendToTarget()
                    if(it != null) try {
                        DownloadTools.getHttpContent(Config.resolution.wrap(Config.imageProxy?.wrap(it)?:it), 1024)?.inputStream()?.let {
                            isCut[i] = canCut(it)
                        }?:run {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@ViewMangaActivity, R.string.touch_img_error, Toast.LENGTH_SHORT)
                                    .show()
                                finish()
                            }
                            return@withContext
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@ViewMangaActivity, R.string.analyze_img_size_error, Toast.LENGTH_SHORT)
                                .show()
                            finish()
                        }
                        return@withContext
                    }
                }
                isCut.forEachIndexed { index, b ->
                    Log.d("MyVM", "[$index] cut: $b")
                    indexMap += index+1
                    if(b) indexMap += -(index+1)
                }
                mHandler.sendEmptyMessage(15)     // hideDl
                Log.d("MyVM", "load all pages finished")
            }
            count = size
            prepareItems()
            if (notUseVP) prepareDownloadTasks()
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    suspend fun initManga() = withContext(Dispatchers.IO) {
        val uuid = mHandler.manga?.results?.chapter?.uuid
        Log.d("MyVM", "initManga, chapter uuid: $uuid")
        if (uuid != null && uuid != "") {
            pn = getPreferences(MODE_PRIVATE).getInt(uuid, -4)
            Log.d("MyVM", "load pn from uuid: $pn")
        } else {
            pn = -4
        }
        if (zipFile?.exists() != true) doPrepareWebImg()
        else prepareItems()
        if (!isVertical) restorePN()
    }

    private suspend fun prepareImgFromWeb() {
        if(!noCellarAlert && toolsBox.netInfo == getString(R.string.TRANSPORT_CELLULAR)) alertCellar()
        else mHandler.startLoad()
    }

    private fun canCut(inputStream: InputStream): Boolean{
        val op = BitmapFactory.Options()
        op.inJustDecodeBounds = true
        inputStream.use {
            BitmapFactory.decodeStream(it, null, op)
        }
        Log.d("MyVM", "w: ${op.outWidth}, h: ${op.outHeight}")
        return op.outWidth.toFloat() / op.outHeight.toFloat() > 1
    }

    suspend fun countZipEntries(doWhenFinish : suspend (count: Int) -> Unit) = withContext(Dispatchers.IO) {
        if (zipFile != null) try {
            Log.d("MyVM", "zip: $zipFile")
            val zip = ZipFile(zipFile)
            count = zip.size()
            if(cut) zip.entries().toList().sortedBy{ it.name.substringBefore('.').toInt()}.forEachIndexed { i, it ->
                val useCut = canCut(zip.getInputStream(it))
                isCut += useCut
                indexMap += i + 1
                if (useCut) indexMap += -(i + 1)
                Log.d("MyVM", "[$i] 分析: ${it.name}, cut: $useCut")
            }
        } catch (e: Exception) {
            toolsBox.toastErrorAndFinish(R.string.count_zip_entries_error)
        }
        Log.d("MyVM", "开始加载控件")
        doWhenFinish(count)
    }

    private fun getPageNumber(): Int {
        return if (r2l && !notUseVP) realCount - vp.currentItem
        else (if (notUseVP) currentItem else vp.currentItem) + 1
    }

    private fun setPageNumber(num: Int) { lifecycleScope.launch {
        Log.d("MyVM", "setPageNumber($num)")
        if (r2l && !notUseVP) vp.currentItem = realCount - num
        else if (notUseVP) {
            if(isVertical) {
                currentItem = num - 1
                val offset = currentItem % verticalLoadMaxCount
                Log.d("MyVM", "Current: $currentItem, Height: ${psivl.height}, scrollY: ${psivs.scrollY}")
                if (!isInScroll || isInSeek) psivs.scrollY = psivl.height * offset / size
                updateSeekBar()
            } else {
                currentItem = num - 1
                try {
                    loadOneImg()
                } catch (e: Exception) {
                    e.printStackTrace()
                    toolsBox.toastError(getString(R.string.load_page_number_error).format(currentItem))
                    finish()
                }
            }
        } else {
            Log.d("MyVM", "Set vp current: ${num-1}")
            vp.currentItem = num - 1
        }
    } }

    /*fun clearImgOn(imgView: ScaleImageView){
        imgView.visibility = View.GONE
        mHandler.sendEmptyMessage(VMHandler.DECREASE_IMAGE_COUNT_AND_RESTORE_PAGE_NUMBER_AT_ZERO)
    }*/

    //private fun getTempFile(position: Int) = File(cacheDir, "$position")

    private fun getImgUrl(position: Int) = mHandler.manga?.results?.chapter?.let {
        it.contents[it.words?.indexOf(position)?:position].url
    }

    private fun getImgUrlArray() = mHandler.manga?.results?.chapter?.let{
            val re = arrayOfNulls<String>(it.contents.size)
            for(i in it.contents.indices) {
                re[i] = getImgUrl(i)
            }
            re
        }

    private fun cutBitmap(bitmap: Bitmap, isEnd: Boolean) = Bitmap.createBitmap(bitmap, if(!isEnd) 0 else (bitmap.width/2), 0, bitmap.width/2, bitmap.height)

    private suspend fun loadImg(imgView: ScaleImageView, bitmap: Bitmap, useCut: Boolean, isLeft: Boolean, isPlaceholder: Boolean = true) = withContext(Dispatchers.IO) {
        val bitmap2load = if(!isPlaceholder && useCut) cutBitmap(bitmap, isLeft) else bitmap
        imgView.apply { post {
            setImageBitmap(bitmap2load)
            if(!isPlaceholder && isVertical) {
                setHeight2FitImgWidth()
                Log.d("MyVM", "dec remainingImageCount")
                mHandler.sendEmptyMessage(VMHandler.DECREASE_IMAGE_COUNT_AND_RESTORE_PAGE_NUMBER_AT_ZERO)
            }
        } }
    }

    private suspend fun loadImgUrlInto(imgView: ScaleImageView, button: Button, url: String, useCut: Boolean, isLeft: Boolean, check: (() -> Boolean)? = null): Boolean {
        Log.d("MyVM", "Load from adt: $url")
        val success = PausableDownloader(Config.resolution.wrap(Config.imageProxy?.wrap(url)?:url), 1000, false) { data ->
            check?.let { it() }?.let { if(it) loadImg(imgView, BitmapFactory.decodeByteArray(data, 0, data.size), useCut, isLeft, false) }
        }.run()
        if (!success) button.apply { post {
            visibility = View.VISIBLE
        } }
        return success
    }

    private fun getLoadingBitmap(position: Int): Bitmap {
        val loading = Bitmap.createBitmap(1024, 256, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(loading)
        val paint = Paint()
        paint.color = colorOnSurface
        paint.textSize = 100.0f
        paint.typeface = Font.nisiTypeFace!!
        val text = "${position+1}"
        val x = (canvas.width - paint.measureText(text)) / 2
        val y = (canvas.height + paint.descent() - paint.ascent()) / 2
        canvas.drawText(text, x, y, paint)
        return loading
    }

    suspend fun loadImgOn(imgView: ScaleImageView, reloadButton: Button, position: Int, isSingle: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        Log.d("MyVM", "Load img: $position")
        if (isSingle && position != currentItem) return@withContext true
        if (position < 0 || position > realCount) return@withContext false
        val index2load = if(cut) abs(indexMap[position]) -1 else position
        val useCut = cut && isCut[index2load]
        val isLeft = cut && indexMap[position] > 0
        val success: Boolean = if (zipFile?.exists() == true) getImgBitmap(index2load)?.let {
            loadImg(imgView, it, useCut, isLeft, false)
            true
        }?:false
        else {
            val sleepTime = loadImgOnWait.getAndIncrement().toLong()*200
            Log.d("MyVM", "loadImgOn sleep: $sleepTime ms")
            val re = tasks?.get(index2load)
            if (sleepTime > 0 && re?.isDone != true) {
                loadImg(imgView, getLoadingBitmap(position), useCut, isLeft, true)
                delay(sleepTime)
                if (isSingle && position != currentItem) return@withContext true
            }
            val s: Boolean = if (re != null) {
                if(!re.isDone) {
                    loadImg(imgView, getLoadingBitmap(position), useCut, isLeft, true)
                    re.run()
                }
                val data = re.get()
                if (isSingle && position != currentItem) return@withContext true
                if(data != null && data.isNotEmpty()) {
                    BitmapFactory.decodeByteArray(data, 0, data.size)?.let {
                        loadImg(imgView, it, useCut, isLeft, false)
                        Log.d("MyVM", "Load position $position from task")
                    }?:Log.d("MyVM", "null bitmap at $position")
                    true
                }
                else getImgUrl(index2load)?.let {
                    loadImg(imgView, getLoadingBitmap(position), useCut, isLeft, true)
                    loadImgUrlInto(imgView, reloadButton, it, useCut, isLeft) {
                        return@loadImgUrlInto !(isSingle && position != currentItem)
                    }
                }?:false
            }
            else getImgUrl(index2load)?.let {
                    loadImg(imgView, getLoadingBitmap(position), useCut, isLeft, true)
                    loadImgUrlInto(imgView, reloadButton, it, useCut, isLeft) {
                        return@loadImgUrlInto !(isSingle && position != currentItem)
                    }
                }?:false
            loadImgOnWait.decrementAndGet()
            tasks?.apply {
                if (index2load >= size) return@apply
                val p = if (index2load == size-1) index2load-1 else index2load+1
                var delta = 1
                var isMinus = false
                var pos = p
                var maxCount = size
                while (pos in indices && get(pos)?.isDone != false && tasksRunStatus?.get(pos) != false && maxCount-- > 0) {
                    Log.d("MyVM", "search $pos")
                    pos = p + if (isMinus) -delta else delta
                    if (pos !in indices) {
                        isMinus = !isMinus
                        if (!isMinus) delta++
                        pos = p + if (isMinus) -delta else delta
                        if (pos !in indices) return@apply
                    }
                    isMinus = !isMinus
                    if (!isMinus) delta++
                }
                if (pos !in indices || tasksRunStatus?.get(pos) != false) return@apply
                Log.d("MyVM", "Preload position $pos from task")
                get(pos)?.apply {
                    if(!isDone) {
                        tasksRunStatus?.set(pos, true)
                        Thread(this).start()
                    }
                }
            }
            s
        }
        withContext(Dispatchers.Main) {
            if(imgView.visibility != View.VISIBLE) imgView.visibility = View.VISIBLE
        }
        return@withContext success
    }

    private suspend fun loadOneImg() {
        val img = onei
        oneb.apply { post {
            if (!hasOnClickListeners()) setOnClickListener {
                lifecycleScope.launch {
                    if (loadImgOn(img, this@apply, currentItem, true)) {
                        post { visibility = View.GONE }
                    }
                }
            }
        } }
        loadImgOn(onei, oneb, currentItem, true)
        updateSeekBar()
    }

    private fun initImgList() {
        for (i in 0 until verticalLoadMaxCount) {
            val newOneImage = layoutInflater.inflate(R.layout.page_imgview, psivl, false)
            val img = newOneImage.onei
            val b = newOneImage.oneb
            val p = scrollPositions.size
            b.apply { post {
                setOnClickListener {
                    lifecycleScope.launch {
                        if (loadImgOn(img, this@apply, scrollPositions[p])) {
                            post { visibility = View.GONE }
                        }
                    }
                }
            } }
            scrollImages += img
            scrollButtons += b
            scrollPositions += -1
            psivl.addView(newOneImage)
        }
    }

    fun prepareLastPage(loadCount: Int, maxCount: Int){
        for (i in loadCount until maxCount) {
            mHandler.obtainMessage(VMHandler.CLEAR_IMG_ON, scrollImages[i]).sendToTarget()
            scrollButtons[i].apply { post { visibility = View.GONE } }
        }
        // mHandler.dl?.hide()
    }

    private suspend fun getImgBitmap(position: Int): Bitmap? = withContext(Dispatchers.IO) {
        if (position >= count || position < 0) null
        else {
            val zip = ZipFile(zipFile)
            var bitmap: Bitmap? = null
            for (i in 0..1) {
                val ext = if((i == 0 && tryWebpFirst) || (i == 1 && !tryWebpFirst)) "webp" else "jpg"
                bitmap = try {
                    zip.getInputStream(zip.getEntry("${position}.$ext"))?.use { zipInputStream ->
                        if (q == 100) BitmapFactory.decodeStream(zipInputStream)
                        else {
                            ByteArrayOutputStream().use { out ->
                                BitmapFactory.decodeStream(zipInputStream)?.compress(Bitmap.CompressFormat.JPEG, q, out)
                                ByteArrayInputStream(out.toByteArray()).use { i ->
                                    BitmapFactory.decodeStream(i)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (i == 1) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@ViewMangaActivity, "加载zip的第${position}项错误", Toast.LENGTH_SHORT).show()
                        }
                    }
                    null
                }
                if (bitmap != null) {
                    tryWebpFirst = ext == "webp"
                    break
                }
            }
            bitmap
        }
    }

    private fun setIdPosition(position: Int) {
        infoDrawerDelta = position.toFloat()
        infcard.translationY = infoDrawerDelta
        Log.d("MyVM", "Set info drawer delta to $infoDrawerDelta")
        mHandler.sendEmptyMessage(if (fullyHideInfo) 16 else VMHandler.HIDE_INFO_CARD)
    }

    @ExperimentalStdlibApi
    @SuppressLint("SetTextI18n")
    private suspend fun prepareItems(): Unit = withContext(Dispatchers.Main) {
        try {
            prepareVP()
            prepareInfoBar()
            prepareIdBtVH()
            toolsBox.dp2px(if(fullyHideInfo) 100 else 67)?.let { setIdPosition(it) }
            prepareIdBtCut()
            prepareIdBtVP()
            prepareIdBtLR()
            if (notUseVP && !isVertical && !isPnValid) loadOneImg()
            /*progressLog?.let {
                it["chapterId"] = hm.chapterId.toString()
                it["name"] = inftitle.ttitle.text
            }*/
        } catch (e: Exception) {
            e.printStackTrace()
            toolsBox.toastErrorAndFinish(R.string.load_chapter_error)
            finish()
        }
    }

    private suspend fun setProgress() = withContext(Dispatchers.IO) {
        mHandler.manga?.results?.chapter?.uuid?.let {
            getPreferences(MODE_PRIVATE).edit {
                //it["chapterId"] = hm.chapterId.toString()
                putInt(it, pageNum)
                //it["name"] = inftitle.ttitle.text
                apply()
            }
        }
    }

    private fun fadeRecreate() {
        val oa = ObjectAnimator.ofFloat(vcp, "alpha", 1f, 0.1f).setDuration(1000)
        oa.doOnEnd {
            onecons?.removeAllViews()
            psivl?.removeAllViews()
            recreate()
        }
        oa.start()
    }

    private fun prepareIdBtCut() {
        idtbcut.isChecked = cut
        idtbcut.setOnClickListener {
            pb["useCut"] = idtbcut.isChecked
            fadeRecreate()
        }
    }

    private fun prepareIdBtLR() {
        idtblr.isChecked = r2l
        idtblr.setOnClickListener {
            if (isVertical) {
                Toast.makeText(this, R.string.unsupported_mode_switching, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            pb["r2l"] = idtblr.isChecked
            fadeRecreate()
        }
    }

    private fun prepareIdBtVP() {
        idtbvp.isChecked = notUseVP
        idtbvp.setOnClickListener {
            if (isVertical) {
                Toast.makeText(this, R.string.unsupported_mode_switching, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            pb["noVP"] = idtbvp.isChecked
            fadeRecreate()
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
                    super.onPageSelected(position)
                    lifecycleScope.launch { updateSeekBar() }
                }
            })
            if (r2l && !isPnValid) vp.currentItem = realCount - 1
        }
    }

    suspend fun updateSeekBar(p: Int = 0) = withContext(Dispatchers.Main) {
        if (p > 0) {
            updateSeekText(p)
            return@withContext
        }
        if (!isInSeek) hideDrawer()
        updateSeekText()
        updateSeekProgress()
        setProgress()
    }

    @SuppressLint("SetTextI18n")
    private fun prepareInfoBar() {
        oneinfo.alpha = 0F
        infseek.visibility = View.GONE
        isearch.visibility = View.GONE
        inftitle.ttitle.text = "$comicName ${mHandler.manga?.results?.chapter?.name}"
        inftxtprogress.text = "$pageNum/$realCount"
        infseek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            var p = 0
            var manualCount = 0
            var startP = 0
            override fun onProgressChanged(p0: SeekBar?, p1: Int, isHuman: Boolean) {
                if (isHuman) {
                    var np = p1 * realCount / 100
                    if (np <= 0) np = 1
                    else if (np > realCount) np = realCount
                    Log.d("MyVM", "seek to $np")
                    if (p1 >= (pageNum + 1) * 100 / realCount) {
                        if(manualCount < 3) scrollForward() else p = np
                        after()
                    }
                    else if (p1 < (pageNum - 1) * 100 / realCount) {
                        if(manualCount < 3) scrollBack() else p = np
                        after()
                    }
                }
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {
                isInSeek = true
                p = pageNum
                startP = p
                manualCount = 0
            }
            override fun onStopTrackingTouch(p0: SeekBar?) {
                if(manualCount >= 3) {
                    val pS = p
                    Log.d("MyVM", "stop seek at $pS")
                    if (isVertical && startP/verticalLoadMaxCount != p/verticalLoadMaxCount) {
                        mHandler.obtainMessage(
                            VMHandler.LOAD_ITEM_SCROLL_MODE,
                            p / verticalLoadMaxCount * verticalLoadMaxCount,
                            0,
                            Runnable {
                                isInScroll = false
                                forceLetPNValid = true
                                pn = pS
                                Log.d("MyVM", "set stopped seek to $pS = $pageNum")
                                isInSeek = false
                            }
                        ).sendToTarget()
                    } else pageNum = pS
                } else isInSeek = false
            }
            private fun after() {
                if(manualCount++ < 3) p = pageNum else lifecycleScope.launch { updateSeekBar(p) }
            }
        })
        isearch.setImageResource(R.drawable.ic_author)
        isearch.setOnClickListener {
            mHandler.sendEmptyMessage(if (fullyHideInfo) VMHandler.TRIGGER_INFO_CARD_FULL else VMHandler.TRIGGER_INFO_CARD) // trigger info card
        }
    }

    @ExperimentalStdlibApi
    private fun prepareIdBtVH() {
        idtbvh.isChecked = isVertical
        pm = PagesManager(WeakReference(this))
        if (isVertical) {
            (vsp as SpringView).apply {
                footerView.lht.setText(R.string.button_more)
                headerView.lht.setText(R.string.button_more)
                setListener(object :SpringView.OnFreshListener{
                    override fun onLoadmore() {
                        //scrollForward()
                        pm?.toPage(true)
                        onFinishFreshAndLoad()
                    }
                    override fun onRefresh() {
                        //scrollBack()
                        pm?.toPage(false)
                        onFinishFreshAndLoad()
                    }
                })
            }
            vp.visibility = View.GONE
            vsp.visibility = View.VISIBLE
            initImgList()
            mHandler.sendEmptyMessage(if(isPnValid) VMHandler.LOAD_PAGE_FROM_ITEM else VMHandler.LOAD_SCROLL_MODE)
            psivs.setOnScrollChangeListener { _, _, scrollY, _, _ ->
                isInScroll = true
                if(!isInSeek) {
                    val delta = (scrollY.toFloat() * size.toFloat() / psivl.height.toFloat() + 0.5).toInt() - currentItem % verticalLoadMaxCount
                    if(delta != 0 && !(delta > 0 && pageNum == size)) {
                        val fin = pageNum + delta
                        pageNum = when {
                            fin <= 0 -> 1
                            fin%verticalLoadMaxCount == 0 -> fin/verticalLoadMaxCount*verticalLoadMaxCount
                            else -> fin
                        }
                        Log.d("MyVM", "Scroll to offset $delta, page $pageNum")
                    }
                }
            }
        }
        idtbvh.setOnClickListener {
            pb["vertical"] = idtbvh.isChecked
            fadeRecreate()
        }
    }

    fun scrollBack() {
        isInScroll = false
        if(isVertical && (pageNum-1) % verticalLoadMaxCount == 0) {
            Log.d("MyVM", "Do scroll back, isVertical: $isVertical, pageNum: $pageNum")
            if (isInSeek) {
                (pageNum-1).let { lifecycleScope.launch { updateSeekBar(it) } }
                return
            }
            mHandler.obtainMessage(
                VMHandler.LOAD_ITEM_SCROLL_MODE,
                currentItem - verticalLoadMaxCount, 0,
                Runnable{
                    forceLetPNValid = true
                    pn = pageNum-1
                }
            ).sendToTarget()    //loadImgsIntoLine(currentItem - verticalLoadMaxCount)
        } else pageNum--
    }

    fun scrollForward() {
        isInScroll = false
        pageNum++
        if(isVertical && (pageNum-1) % verticalLoadMaxCount == 0) {
            if (isInSeek) {
                (pageNum+1).let { lifecycleScope.launch { updateSeekBar(it) } }
                return
            }
            mHandler.sendEmptyMessage(VMHandler.LOAD_SCROLL_MODE)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateSeekText(p: Int = 0) {
        inftxtprogress.text = "${if(p == 0) pageNum else p}/$realCount"
    }

    private fun updateSeekProgress() {
        infseek.progress = pageNum * 100 / realCount
    }

    override fun onDestroy() {
        dlHandler?.sendEmptyMessage(0)
        tt.canDo = false
        destroy = true
        dlHandler = null
        mHandler.dl.dismiss()
        mHandler.destroy()
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
                val index2load = if(cut) abs(indexMap[pos]) -1 else pos
                val useCut = cut && isCut[index2load]
                val isLeft = cut && indexMap[pos] > 0
                if (zipFile?.exists() == true) lifecycleScope.launch {
                    getImgBitmap(index2load)?.let {
                        //Glide.with(this@ViewMangaActivity).load(if(useCut) cutBitmap(it, isLeft) else it).into(holder.itemView.onei)
                        holder.itemView.onei.setImageBitmap(if(useCut) cutBitmap(it, isLeft) else it)
                        holder.itemView.oneb.visibility = View.GONE
                    }
                }
                else getImgUrl(index2load)?.let{
                    if(useCut) {
                        val thisOneI = holder.itemView.onei
                        val thisOneB = holder.itemView.oneb
                        Glide.with(this@ViewMangaActivity.applicationContext)
                            .asBitmap()
                            .load(GlideUrl(Config.resolution.wrap(Config.imageProxy?.wrap(it)?:it), Config.myGlideHeaders))
                            .placeholder(BitmapDrawable(resources, getLoadingBitmap(pos)))
                            .timeout(60000)
                            .addListener(OneButtonRequestListener(thisOneB))
                            .into(object : CustomTarget<Bitmap>() {
                                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                    thisOneI.setImageBitmap(cutBitmap(resource, isLeft))
                                }
                                override fun onLoadCleared(placeholder: Drawable?) { }
                            })
                    } else Glide.with(this@ViewMangaActivity.applicationContext)
                        .load(GlideUrl(Config.resolution.wrap(Config.imageProxy?.wrap(it)?:it), Config.myGlideHeaders))
                        .timeout(60000)
                        .placeholder(BitmapDrawable(resources, getLoadingBitmap(pos)))
                        .addListener(OneButtonRequestListener(holder.itemView.oneb))
                        .into(holder.itemView.onei)
                }
            }

            override fun getItemCount(): Int {
                return realCount
            }

            private inner class OneButtonRequestListener<T>(private val thisOneB: Button) : RequestListener<T> {
                var mTarget: Target<T>? = null
                init {
                    thisOneB.apply { post {
                        setOnClickListener { mTarget?.request?.apply {
                            clear()
                            begin()
                        } }
                    } }
                }
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<T>,
                    isFirstResource: Boolean
                ): Boolean {
                    thisOneB.visibility = View.VISIBLE
                    mTarget = target
                    return false
                }
                override fun onResourceReady(
                    resource: T & Any,
                    model: Any,
                    target: Target<T>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    thisOneB.visibility = View.GONE
                    return false
                }
            }
        }
    }

    fun showDrawer() {
        clicked = 2 // loading
        infseek.post {
            infseek.visibility = View.VISIBLE
            isearch.post {
                isearch.visibility = View.VISIBLE
                infseek.invalidate()
                isearch.invalidate()
                ObjectAnimator.ofFloat(
                    oneinfo,
                    "alpha",
                    oneinfo.alpha,
                    1F
                ).setDuration(300).start()
                clicked = 1 // true
            }
        }
    }

    fun hideDrawer() {
        clicked = 2 // loading
        ObjectAnimator.ofFloat(
            oneinfo,
            "alpha",
            oneinfo.alpha,
            0F
        ).setDuration(300).start()
        infseek.postDelayed({
            infseek.visibility = View.GONE
            isearch.visibility = View.GONE
            infseek.invalidate()
            isearch.invalidate()
            clicked = 0 // false
        }, 300)
        mHandler.sendEmptyMessage(if (fullyHideInfo) VMHandler.HIDE_INFO_CARD_FULL else VMHandler.HIDE_INFO_CARD)
    }

    companion object {
        var dlHandler: Handler? = null
        var va: WeakReference<ViewMangaActivity>? = null
        var noCellarAlert = false
    }
}