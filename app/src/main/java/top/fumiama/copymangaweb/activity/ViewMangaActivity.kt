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
import kotlinx.android.synthetic.main.activity_main.w
import kotlinx.android.synthetic.main.activity_viewmanga.infcard
import kotlinx.android.synthetic.main.activity_viewmanga.oneinfo
import kotlinx.android.synthetic.main.activity_viewmanga.vone
import kotlinx.android.synthetic.main.activity_viewmanga.vp
import kotlinx.android.synthetic.main.page_imgview.onei
import kotlinx.android.synthetic.main.page_imgview.view.onei
import kotlinx.android.synthetic.main.widget_infodrawer.idtblr
import kotlinx.android.synthetic.main.widget_infodrawer.idtbvh
import kotlinx.android.synthetic.main.widget_infodrawer.idtbvolturn
import kotlinx.android.synthetic.main.widget_infodrawer.idtbvp
import kotlinx.android.synthetic.main.widget_infodrawer.idtime
import kotlinx.android.synthetic.main.widget_infodrawer.view.idc
import kotlinx.android.synthetic.main.widget_titlebar.isearch
import kotlinx.android.synthetic.main.widget_titlebar.ttitle
import kotlinx.android.synthetic.main.widget_viewmangainfo.infseek
import kotlinx.android.synthetic.main.widget_viewmangainfo.inftxtprogress
import top.fumiama.copymangaweb.R
import top.fumiama.copymangaweb.activity.MainActivity.Companion.wm
import top.fumiama.copymangaweb.activity.template.ToolsBoxActivity
import top.fumiama.copymangaweb.databinding.ActivityViewmangaBinding
import top.fumiama.copymangaweb.handler.TimeThread
import top.fumiama.copymangaweb.tool.PropertiesTools
import top.fumiama.copymangaweb.tool.ToolsBox
import java.io.File
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

class ViewMangaActivity : ToolsBoxActivity() {
    lateinit var handler: Handler
    lateinit var tt: TimeThread

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
        val binding = ActivityViewmangaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        va = WeakReference(this)
        p = PropertiesTools(File("$filesDir/settings.properties"))
        r2l = p["r2l"] == "true"
        notUseVP = p["noAnimation"] == "true"
        handler = MyHandler(infcard, toolsBox)
        tt = TimeThread(handler, 22)
        tt.canDo = true
        tt.start()
        dialog = Dialog(this)
        dialog?.apply {
            setContentView(R.layout.dialog_unzipping)
            show()
        }
        ttitle.apply { post { text = titleText } }
        Log.d("MyVM", "dlZip2View: $dlZip2View, mangaZip: $mangaZip")
        if(dlZip2View && mangaZip?.exists() != true) toolsBox.toastError("已经到头了~")
        else Thread {
            try {
                count = if (dlZip2View) countZipItems() else imgUrls.size
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread { toolsBox.toastError("分析图片url错误") }
            }
            runOnUiThread {
                try {
                    prepareItems()
                    if(pn > 0) {
                        pageNum = pn
                        pn = -1
                    }else if(pn == -2){
                        pageNum = count
                        pn = -1
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    toolsBox.toastError("准备控件错误")
                } finally {
                    dialog?.hide()
                }
            }
        }.start()
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
        return if (r2l && !notUseVP) count - vp.currentItem
        else (if (notUseVP) currentItem else vp.currentItem) + 1
    }

    private fun setPageNumber(num: Int) {
        if (r2l && !notUseVP) vp.apply { post { currentItem = count - num } }
        else if (notUseVP) currentItem = num - 1 else vp.currentItem = num - 1
    }

    private fun getImgBitmap(position: Int): Bitmap? {
        if (position >= count || position < 0) return null
        else {
            val zip = ZipFile(mangaZip)
            return BitmapFactory.decodeStream(zip.getInputStream(zip.getEntry("${position}.webp")))
        }
    }

    private fun loadOneImg() {
        if(dlZip2View) onei.apply { post { setImageBitmap(getImgBitmap(currentItem)) } }
        else Glide.with(this@ViewMangaActivity)
            .load(imgUrls[currentItem])
            .placeholder(R.drawable.ic_dl)
            .dontAnimate()
            .into(onei)
        updateSeekBar()
    }

    private fun setIdPosition(position: Int) {
        infoDrawerDelta = position.toFloat()
        infcard.apply { post { translationY = infoDrawerDelta } }
    }

    @SuppressLint("SetTextI18n")
    private fun prepareItems() {
        prepareVP()
        prepareInfoBar(count)
        if (notUseVP) loadOneImg() else prepareIdBtVH()
        toolsBox.dp2px(67)?.let { setIdPosition(it) }
        prepareIdBtVolTurn()
        prepareIdBtVP()
        prepareIdBtLR()
    }

    private fun prepareIdBtLR() {
        idtblr.apply { post {
            isChecked = r2l
            setOnClickListener {
                if (idtblr.isChecked) p["r2l"] = "true"
                else p["r2l"] = "false"
                Toast.makeText(this@ViewMangaActivity, "下次浏览生效", Toast.LENGTH_SHORT).show()
            }
        } }
    }

    private fun prepareIdBtVP() {
        idtbvp.apply { post {
            isChecked = notUseVP
            setOnClickListener {
                if (idtbvp.isChecked) p["noAnimation"] = "true"
                else p["noAnimation"] = "false"
                Toast.makeText(this@ViewMangaActivity, "下次浏览生效", Toast.LENGTH_SHORT).show()
            }
        } }
    }

    private fun prepareVP() {
        if (notUseVP) {
            vp.apply { post { visibility = View.INVISIBLE } }
            vone.apply { post { visibility = View.VISIBLE } }
        } else {
            vp.apply { post {
                visibility = View.VISIBLE
                adapter = ViewData(this).RecyclerViewAdapter()
                registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        updateSeekBar()
                        super.onPageSelected(position)
                    }
                })
                if (r2l) currentItem = count - 1
            } }
            vone.apply { post { visibility = View.INVISIBLE } }
        }
    }

    private fun updateSeekBar() {
        if (!isInSeek) hideSettings()
        updateSeekText()
        updateSeekProgress()
    }

    @SuppressLint("SetTextI18n")
    private fun prepareInfoBar(size: Int) {
        oneinfo.apply { post { alpha = 0F } }
        infseek.apply { post {
            visibility = View.INVISIBLE
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
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
        } }
        isearch.apply { post {
            visibility = View.INVISIBLE
            setOnClickListener {
                handler.sendEmptyMessage(3)
            }
        } }
        inftxtprogress.apply { post { text = "$pageNum/$size" } }
    }

    private fun prepareIdBtVH() {
        idtbvh.apply { post {
            isChecked = p["vertical"] == "true"
            setOnClickListener {
                if (idtbvh.isChecked) {
                    vp.apply { post { orientation = ViewPager2.ORIENTATION_VERTICAL } }
                    p["vertical"] = "true"
                } else {
                    vp.apply { post { orientation = ViewPager2.ORIENTATION_HORIZONTAL } }
                    p["vertical"] = "false"
                }
            }
        } }
        if (idtbvh.isChecked) vp.apply { post { orientation = ViewPager2.ORIENTATION_VERTICAL } }
    }

    private fun prepareIdBtVolTurn() {
        idtbvolturn.apply { post {
            isChecked = volTurnPage
            setOnClickListener {
                if (idtbvolturn.isChecked) p["volturn"] = "true"
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
        pageNum--
    }

    fun scrollForward() {
        pageNum++
    }

    @SuppressLint("SetTextI18n")
    private fun updateSeekText() {
        inftxtprogress.apply { post { text = "$pageNum/$count" } }
    }

    private fun updateSeekProgress() {
        infseek.apply { post { progress = pageNum * 100 / count } }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        tt.canDo = false
        wm?.get()?.w?.goBack()
        super.onBackPressed()
    }

    override fun onDestroy() {
        tt.canDo = false
        handler.removeCallbacksAndMessages(null)
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
                if(dlZip2View) getImgBitmap(pos)?.let {
                    //Glide.with(this@ViewMangaActivity).load(it).placeholder(R.drawable.bg_comment).into(holder.itemView.onei)
                    holder.itemView.onei.setImageBitmap(it)
                }
                else Glide.with(this@ViewMangaActivity).load(imgUrls[pos]).placeholder(R.drawable.ic_dl).dontAnimate().timeout(10000).into(holder.itemView.onei)
            }

            override fun getItemCount(): Int {
                return count
            }
        }
    }

    fun showSettings() {
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

    fun hideSettings() {
        ObjectAnimator.ofFloat(
            oneinfo,
            "alpha",
            oneinfo.alpha,
            0F
        ).setDuration(233).start()
        clicked = false
        infseek.postDelayed({
            infseek.visibility = View.INVISIBLE
            isearch.visibility = View.INVISIBLE
        }, 300)
        handler.sendEmptyMessage(1)
    }

    class MyHandler(
        private val infoCard: View,
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
                22 -> toolsBox.zis?.idtime?.text =
                    SimpleDateFormat("HH:mm").format(Date()) + toolsBox.week + toolsBox.netInfo
            }
        }

        private fun showInfCard() {
            ObjectAnimator.ofFloat(infoCard.idc, "alpha", 0.3F, 0.8F).setDuration(233).start()
            ObjectAnimator.ofFloat(infoCard, "translationY", delta, 0F).setDuration(233).start()
        }

        private fun hideInfCard() {
            ObjectAnimator.ofFloat(infoCard.idc, "alpha", 0.8F, 0.3F).setDuration(233).start()
            ObjectAnimator.ofFloat(infoCard, "translationY", 0F, delta).setDuration(233).start()
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
    }
}