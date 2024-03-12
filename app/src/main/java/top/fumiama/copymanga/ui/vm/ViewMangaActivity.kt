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
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.request.target.CustomTarget
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.fumiama.copymanga.MainActivity
import top.fumiama.copymanga.template.general.TitleActivityTemplate
import top.fumiama.copymanga.template.http.PausableDownloader
import top.fumiama.copymanga.tools.api.CMApi
import top.fumiama.copymanga.tools.http.DownloadTools
import top.fumiama.copymanga.tools.thread.TimeThread
import top.fumiama.copymanga.tools.ui.Font
import top.fumiama.copymanga.views.ScaleImageView
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
    private lateinit var handler: VMHandler
    lateinit var tt: TimeThread
    var clicked = 0
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
        postponeEnterTransition()
        setContentView(R.layout.activity_viewmanga)
        super.onCreate(null)
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val settingsPref = MainActivity.mainWeakReference?.get()?.let { PreferenceManager.getDefaultSharedPreferences(it) }
                settingsPref?.getBoolean("settings_cat_vm_sw_always_dark_bg", false)?.let {
                    if (it) {
                        Log.d("MyVM", "force dark")
                        delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_YES
                    } else {
                        delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    }
                }
                va = WeakReference(this@ViewMangaActivity)
                //dlZip2View = intent.getStringExtra("callFrom") == "Dl" || p["dlZip2View"] == "true"
                //zipFirst = intent.getStringExtra("callFrom") == "zipFirst"
                intent.getStringArrayExtra("urlArray")?.let { urlArray = it }
                cut = pb["useCut"]
                r2l = pb["r2l"]
                verticalLoadMaxCount = settingsPref?.getInt("settings_cat_vm_sb_vertical_max", 20)?.let { if(it > 0) it else 20 }?:20
                isVertical = pb["vertical"]
                notUseVP = pb["noVP"] || isVertical
                //url = intent.getStringExtra("url")
                withContext(Dispatchers.Main) {
                    handler = VMHandler(this@ViewMangaActivity, if(urlArray.isNotEmpty()) urlArray[position] else "", resources.getStringArray(R.array.weeks))
                    withContext(Dispatchers.IO) {
                        settingsPref?.getInt("settings_cat_vm_sb_quality", 100)?.let { q = if (it > 0) it else 100 }
                        tt = TimeThread(handler, VMHandler.SET_NET_INFO, 10000)
                        tt.canDo = true
                        tt.start()
                        volTurnPage = settingsPref?.getBoolean("settings_cat_vm_sw_vol_turn", false)?:false
                        am = getSystemService(Service.AUDIO_SERVICE) as AudioManager
                        if (!noCellarAlert) noCellarAlert = settingsPref?.getBoolean("settings_cat_net_sw_use_cellar", false) == true
                        fullyHideInfo = settingsPref?.getBoolean("settings_cat_vm_sw_hide_info", false) == true

                        Log.d("MyVM", "Now ZipFile is $zipFile")
                        try {
                            if (zipFile != null && zipFile?.exists() == true) {
                                if (!handler.loadFromFile(zipFile!!)) prepareImgFromWeb()
                            } else prepareImgFromWeb()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            toolsBox.toastError(R.string.load_manga_error)
                        }
                        withContext(Dispatchers.Main) {
                            startPostponedEnterTransition()
                            ObjectAnimator.ofFloat(vcp, "alpha", 0.1f, 1f).setDuration(1000).start()
                        }
                    }
                }
            }
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
            { handler.startLoad() }, { noCellarAlert = true; handler.startLoad() }, { finish() }
        )
    }

    fun restorePN() {
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
                return@Array DownloadTools.prepare(CMApi.resolution.wrap(CMApi.imageProxy?.wrap(u)?:u))
            }
            tasksRunStatus = Array(it.size) { return@Array false }
        }
    }

    @ExperimentalStdlibApi
    private fun doPrepareWebImg() = Thread {
        getImgUrlArray()?.apply {
            if(cut) {
                Log.d("MyVM", "is cut, load all pages...")
                handler.sendEmptyMessage(VMHandler.DIALOG_SHOW)     //showDl
                isCut = BooleanArray(size)
                val analyzedCnt = BooleanArray(size)
                forEachIndexed { i, it ->
                    if(it != null) {
                        Thread{
                            DownloadTools.getHttpContent(CMApi.resolution.wrap(CMApi.imageProxy?.wrap(it)?:it), 1024)?.inputStream()?.let {
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
                Log.d("MyVM", "load all pages finished")
            }
            count = size
            runOnUiThread { prepareItems() }
            if (notUseVP) prepareDownloadTasks()
        }
    }.start()

    @OptIn(ExperimentalStdlibApi::class)
    fun initManga() {
        val uuid = handler.manga?.results?.chapter?.uuid
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
        else handler.startLoad()
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
            withContext(Dispatchers.Main) { toolsBox.toastError(R.string.count_zip_entries_error) }
        }
        Log.d("MyVM", "开始加载控件")
        doWhenFinish(count)
    }

    private fun getPageNumber(): Int {
        return if (r2l && !notUseVP) realCount - vp.currentItem
        else (if (notUseVP) currentItem else vp.currentItem) + 1
    }

    private fun setPageNumber(num: Int) {
        Log.d("MyVM", "setPageNumber($num)")
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
                    lifecycleScope.launch {
                        toolsBox.toastError(getString(R.string.load_page_number_error).format(currentItem))
                    }
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

    /*fun clearImgOn(imgView: ScaleImageView){
        imgView.visibility = View.GONE
        handler.sendEmptyMessage(VMHandler.DECREASE_IMAGE_COUNT_AND_RESTORE_PAGE_NUMBER_AT_ZERO)
    }*/

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

    private suspend fun loadImg(imgView: ScaleImageView, bitmap: Bitmap, useCut: Boolean, isLeft: Boolean, isPlaceholder: Boolean = true) = withContext(Dispatchers.IO) {
        val bitmap2load = if(!isPlaceholder && useCut) cutBitmap(bitmap, isLeft) else bitmap
        withContext(Dispatchers.Main) {
            imgView.setImageBitmap(bitmap2load)
            if(!isPlaceholder && isVertical) {
                imgView.setHeight2FitImgWidth()
                handler.sendEmptyMessage(VMHandler.DECREASE_IMAGE_COUNT_AND_RESTORE_PAGE_NUMBER_AT_ZERO)
            }
        }
    }

    private suspend fun loadImgUrlInto(imgView: ScaleImageView, url: String, useCut: Boolean, isLeft: Boolean){
        Log.d("MyVM", "Load from adt: $url")
        PausableDownloader(CMApi.resolution.wrap(CMApi.imageProxy?.wrap(url)?:url), 1000, false) {
            it.let { loadImg(imgView, BitmapFactory.decodeByteArray(it, 0, it.size), useCut, isLeft, false) }
        }.run()
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

    suspend fun loadImgOn(imgView: ScaleImageView, position: Int) = withContext(Dispatchers.IO) {
        Log.d("MyVM", "Load img: $position")
        if (position < 0 || position > realCount) return@withContext
        val index2load = if(cut) abs(indexMap[position]) -1 else position
        val useCut = cut && isCut[index2load]
        val isLeft = cut && indexMap[position] > 0
        if (zipFile?.exists() == true) getImgBitmap(index2load)?.let {
            loadImg(imgView, it, useCut, isLeft, false)
        }
        else {
            val sleepTime = loadImgOnWait.getAndIncrement().toLong()*200
            Log.d("MyVM", "loadImgOn sleep: $sleepTime ms")
            val re = tasks?.get(index2load)
            if (sleepTime > 0 && re?.isDone != true) {
                loadImg(imgView, getLoadingBitmap(position), useCut, isLeft, true)
                Thread.sleep(sleepTime)
            }
            if (re != null) {
                if(!re.isDone) {
                    loadImg(imgView, getLoadingBitmap(position), useCut, isLeft, true)
                    re.run()
                }
                val data = re.get()
                if(data != null && data.isNotEmpty()) {
                    BitmapFactory.decodeByteArray(data, 0, data.size)?.let {
                        loadImg(imgView, it, useCut, isLeft, false)
                        Log.d("MyVM", "Load position $position from task")
                    }?:Log.d("MyVM", "null bitmap at $position")
                }
                else getImgUrl(index2load)?.let {
                    loadImg(imgView, getLoadingBitmap(position), useCut, isLeft, true)
                    loadImgUrlInto(imgView, it, useCut, isLeft)
                }
            }
            else getImgUrl(index2load)?.let {
                loadImg(imgView, getLoadingBitmap(position), useCut, isLeft, true)
                loadImgUrlInto(imgView, it, useCut, isLeft)
            }
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
                        run()
                    }
                }
            }
        }
        withContext(Dispatchers.Main) {
            if(imgView.visibility != View.VISIBLE) imgView.visibility = View.VISIBLE
        }
    }

    private fun loadOneImg() {
        lifecycleScope.launch {
            loadImgOn(onei, currentItem)
            updateSeekBar()
        }
    }

    private fun initImgList(){
        for (i in 0 until verticalLoadMaxCount) {
            val newImg = ScaleImageView(this)
            scrollImages += newImg
            psivl.addView(newImg)
        }
    }

    fun prepareLastPage(loadCount: Int, maxCount: Int){
        for (i in loadCount until maxCount) handler.obtainMessage(VMHandler.CLEAR_IMG_ON, scrollImages[i]).sendToTarget()
        // handler.dl?.hide()
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
        handler.sendEmptyMessage(if (fullyHideInfo) 16 else VMHandler.HIDE_INFO_CARD)
    }

    @ExperimentalStdlibApi
    @SuppressLint("SetTextI18n")
    private fun prepareItems() {
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
            lifecycleScope.launch {
                toolsBox.toastError(R.string.load_chapter_error)
                finish()
            }
        }
    }

    private fun setProgress() {
        handler.manga?.results?.chapter?.uuid?.let {
            getPreferences(MODE_PRIVATE).edit {
                //it["chapterId"] = hm.chapterId.toString()
                putInt(it, pageNum)
                //it["name"] = inftitle.ttitle.text
                apply()
            }
        }
    }

    private fun prepareIdBtCut() {
        idtbcut.isChecked = cut
        idtbcut.setOnClickListener {
            pb["useCut"] = idtbcut.isChecked
            val oa = ObjectAnimator.ofFloat(vcp, "alpha", 1f, 0.1f).setDuration(1000)
            oa.doOnEnd {
                recreate()
            }
            oa.start()
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
            val oa = ObjectAnimator.ofFloat(vcp, "alpha", 1f, 0.1f).setDuration(1000)
            oa.doOnEnd {
                recreate()
            }
            oa.start()
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
            val oa = ObjectAnimator.ofFloat(vcp, "alpha", 1f, 0.1f).setDuration(1000)
            oa.doOnEnd {
                recreate()
            }
            oa.start()
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

    fun updateSeekBar(p: Int = 0) {
        if (p > 0) {
            updateSeekText(p)
            return
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
        inftitle.ttitle.text = handler.manga?.results?.chapter?.name
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
                        handler.obtainMessage(
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
                if(manualCount++ < 3) p = pageNum else updateSeekBar(p)
            }
        })
        isearch.setImageResource(R.drawable.ic_author)
        isearch.setOnClickListener {
            handler.sendEmptyMessage(if (fullyHideInfo) VMHandler.TRIGGER_INFO_CARD_FULL else VMHandler.TRIGGER_INFO_CARD) // trigger info card
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
            handler.sendEmptyMessage(if(isPnValid) VMHandler.LOAD_PAGE_FROM_ITEM else VMHandler.LOAD_SCROLL_MODE)
            psivs.setOnScrollChangeListener { _, _, scrollY, _, _ ->
                isInScroll = true
                if(!isInSeek) {
                    val delta = (scrollY.toFloat() * size.toFloat() / psivl.height.toFloat() + 0.5).toInt() - currentItem % verticalLoadMaxCount
                    if(delta != 0 && !(delta > 0 && pageNum == size)) {
                        pageNum += delta
                        Log.d("MyVM", "Scroll to offset $delta")
                    }
                }
            }
        }
        idtbvh.setOnClickListener {
            pb["vertical"] = idtbvh.isChecked
            val oa = ObjectAnimator.ofFloat(vcp, "alpha", 1f, 0.1f).setDuration(1000)
            oa.doOnEnd {
                recreate()
            }
            oa.start()
        }
    }

    fun scrollBack() {
        isInScroll = false
        if(isVertical && (pageNum-1) % verticalLoadMaxCount == 0) {
            Log.d("MyVM", "Do scroll back, isVertical: $isVertical, pageNum: $pageNum")
            if (isInSeek) {
                updateSeekBar(pageNum-1)
                return
            }
            handler.obtainMessage(
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
                updateSeekBar(pageNum+1)
                return
            }
            handler.sendEmptyMessage(VMHandler.LOAD_SCROLL_MODE)
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
        handler.dl.dismiss()
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
                val index2load = if(cut) abs(indexMap[pos]) -1 else pos
                val useCut = cut && isCut[index2load]
                val isLeft = cut && indexMap[pos] > 0
                if (zipFile?.exists() == true) lifecycleScope.launch {
                    getImgBitmap(index2load)?.let {
                        //Glide.with(this@ViewMangaActivity).load(if(useCut) cutBitmap(it, isLeft) else it).into(holder.itemView.onei)
                        holder.itemView.onei.setImageBitmap(if(useCut) cutBitmap(it, isLeft) else it)
                    }
                }
                else getImgUrl(index2load)?.let{
                    if(useCut){
                        val thisOneI = holder.itemView.onei
                        Glide.with(this@ViewMangaActivity.applicationContext)
                            .asBitmap()
                            .load(GlideUrl(CMApi.resolution.wrap(CMApi.imageProxy?.wrap(it)?:it), CMApi.myGlideHeaders))
                            .placeholder(BitmapDrawable(resources, getLoadingBitmap(pos)))
                            .into(object : CustomTarget<Bitmap>() {
                                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                    thisOneI.setImageBitmap(cutBitmap(resource, isLeft))
                                }
                                override fun onLoadCleared(placeholder: Drawable?) { }
                            })
                    } else Glide.with(this@ViewMangaActivity.applicationContext)
                        .load(GlideUrl(CMApi.resolution.wrap(CMApi.imageProxy?.wrap(it)?:it), CMApi.myGlideHeaders))
                        .placeholder(BitmapDrawable(resources, getLoadingBitmap(pos)))
                        .into(holder.itemView.onei)
                }
            }

            override fun getItemCount(): Int {
                return realCount
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
        handler.sendEmptyMessage(if (fullyHideInfo) VMHandler.HIDE_INFO_CARD_FULL else VMHandler.HIDE_INFO_CARD)
    }

    companion object {
        var comicName: String? = null
        var uuidArray = arrayOf<String>()
        var fileArray = arrayOf<File>()
        var position = 0
        var zipFile: File? = null
        var dlHandler: Handler? = null
        var va: WeakReference<ViewMangaActivity>? = null
        var pn = 0
        var noCellarAlert = false
    }
}