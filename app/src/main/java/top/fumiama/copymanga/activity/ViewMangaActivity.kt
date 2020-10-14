package top.fumiama.copymanga.activity

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_viewmanga.*
import kotlinx.android.synthetic.main.page_imgview.*
import kotlinx.android.synthetic.main.page_imgview.view.*
import kotlinx.android.synthetic.main.widget_infodrawer.*
import kotlinx.android.synthetic.main.widget_infodrawer.view.*
import kotlinx.android.synthetic.main.widget_titlebar.*
import kotlinx.android.synthetic.main.widget_viewmangainfo.*
import top.fumiama.copymanga.R
import top.fumiama.copymanga.activity.MainActivity.Companion.wm
import top.fumiama.copymanga.handler.TimeThread
import top.fumiama.copymanga.tool.PropertiesTools
import top.fumiama.copymanga.tool.ToolsBox
import java.io.File
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*


class ViewMangaActivity : Activity() {
    var count = 0
    lateinit var handler: Handler
    lateinit var tt: TimeThread
    var clicked = false
    private var isInSeek = false
    private var useFullScreen = false
    var r2l = true
    private var currentItem = 0
    private var notUseVP = true
    private var q = 90
    var infoDrawerDelta = 0f
    lateinit var toolsBox: ToolsBox
    private lateinit var p: PropertiesTools
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

    @ExperimentalStdlibApi
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_viewmanga)
        toolsBox = ToolsBox(WeakReference(this))
        va = WeakReference(this)
        p = PropertiesTools(File("$filesDir/settings.properties"))
        useFullScreen = p["useFullScreen"] != "true"
        r2l = p["r2l"] == "true"
        //toolsBox = ToolsBox(WeakReference(this))
        notUseVP = p["noAnimation"] == "true"
        handler = MyHandler(infcard, toolsBox)
        if (p["quality"] == "null") p["quality"] = "90"
        else q = p["quality"].toInt()
        tt = TimeThread(handler, 22)
        tt.canDo = true
        tt.start()
        ttitle.text = titleText
        try {
            count = imgUrls.size
        } catch (e: Exception) {
            e.printStackTrace()
            toolsBox.toastError("分析图片url错误")
        }
        try {
            prepareItems(count)
        } catch (e: Exception) {
            e.printStackTrace()
            toolsBox.toastError("准备控件错误")
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (useFullScreen) window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }

    private fun getPageNumber(): Int {
        return if (r2l && !notUseVP) count - vp.currentItem
        else (if (notUseVP) currentItem else vp.currentItem) + 1
    }

    private fun setPageNumber(num: Int) {
        if (r2l && !notUseVP) vp.currentItem = count - num
        else if (notUseVP) currentItem = num - 1 else vp.currentItem = num - 1
    }

    private fun loadOneImg() {
        Glide.with(this@ViewMangaActivity.applicationContext).load(
            imgUrls[currentItem]
        ).thumbnail(
            Glide.with(this@ViewMangaActivity.applicationContext).load(R.drawable.bg_comment)
        ).into(onei)
        updateSeekBar()
    }

    private fun setIdPosition(position: Int) {
        infoDrawerDelta = position.toFloat()
        infcard.translationY = infoDrawerDelta
    }

    @SuppressLint("SetTextI18n")
    private fun prepareItems(size: Int) {
        prepareVP()
        prepareInfoBar(size)
        if (notUseVP) loadOneImg() else prepareIdBtVH()
        toolsBox.dp2px(67)?.let { setIdPosition(it) }
        prepareIdBtFullScreen()
        prepareIdBtVP()
        prepareIdBtLR()
    }

    private fun prepareIdBtLR() {
        idtblr.isChecked = r2l
        idtblr.setOnClickListener {
            if (idtblr.isChecked) p["r2l"] = "true"
            else p["r2l"] = "false"
            Toast.makeText(this.applicationContext, "下次浏览生效", Toast.LENGTH_SHORT).show()
        }
    }

    private fun prepareIdBtVP() {
        idtbvp.setOnClickListener {
            if (idtbvp.isChecked) p["noAnimation"] = "true"
            else p["noAnimation"] = "false"
            Toast.makeText(this.applicationContext, "下次浏览生效", Toast.LENGTH_SHORT).show()
        }
    }

    private fun prepareVP() {
        if (notUseVP) {
            vp.visibility = View.INVISIBLE
            vone.visibility = View.VISIBLE
        } else {
            vp.visibility = View.VISIBLE
            vone.visibility = View.INVISIBLE
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
        infseek.visibility = View.INVISIBLE
        isearch.visibility = View.INVISIBLE
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

    private fun prepareIdBtVH() {
        idtbvh.isChecked =
            p["vertical"] == "true"
        if (idtbvh.isChecked) vp.orientation = ViewPager2.ORIENTATION_VERTICAL
        idtbvh.setOnClickListener {
            if (idtbvh.isChecked) {
                vp.orientation = ViewPager2.ORIENTATION_VERTICAL
                p["vertical"] = "true"
            } else {
                vp.orientation = ViewPager2.ORIENTATION_HORIZONTAL
                p["vertical"] = "false"
            }
        }
    }

    private fun prepareIdBtFullScreen() {
        idtbfullscreen.isChecked = !useFullScreen
        idtbfullscreen.setOnClickListener {
            if (idtbfullscreen.isChecked) p["useFullScreen"] =
                "true"
            else p["useFullScreen"] = "false"
            Toast.makeText(this.applicationContext, "下次浏览生效", Toast.LENGTH_SHORT).show()
        }
    }

    fun scrollBack() {
        pageNum--
    }

    fun scrollForward() {
        pageNum++
    }

    private fun sendProgress() {

    }

    @SuppressLint("SetTextI18n")
    private fun updateSeekText() {
        inftxtprogress.text = "$pageNum/$count"
    }

    private fun updateSeekProgress() {
        infseek.progress = pageNum * 100 / count
    }

    override fun onBackPressed() {
        tt.canDo = false
        wm?.get()?.w?.goBack()
        super.onBackPressed()
    }

    override fun onDestroy() {
        tt.canDo = false
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
                Glide.with(this@ViewMangaActivity.applicationContext).load(
                    imgUrls[pos]
                ).thumbnail(
                    Glide.with(this@ViewMangaActivity.applicationContext)
                        .load(R.drawable.bg_comment)
                ).into(holder.itemView.onei)
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
            infseek.visibility = View.INVISIBLE
            isearch.visibility = View.INVISIBLE
        }, 300)
        handler.sendEmptyMessage(1)
    }

    class MyHandler(
        private val infcard: View,
        private val toolsBox: ToolsBox
    ) : Handler() {
        private var infcShowed = false
        private var delta = -1f
            get() {
                if (field < 0) field = va?.get()?.infoDrawerDelta ?: 0f
                return field
            }

        @SuppressLint("SimpleDateFormat", "SetTextI18n")
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                1 -> if (infcShowed) {
                    hideInfCard(); infcShowed = false
                }
                2 -> if (!infcShowed) {
                    showInfCard(); infcShowed = true
                }
                3 -> infcShowed = if (infcShowed) {
                    hideInfCard(); false
                } else {
                    showInfCard(); true
                }
                22 -> toolsBox.zis?.idtime?.text =
                    SimpleDateFormat("HH:mm").format(Date()) + toolsBox.week + toolsBox.netinfo
            }
        }

        private fun showInfCard() {
            ObjectAnimator.ofFloat(infcard.idc, "alpha", 0.3F, 0.8F).setDuration(233).start()
            ObjectAnimator.ofFloat(infcard, "translationY", delta, 0F).setDuration(233).start()
        }

        private fun hideInfCard() {
            ObjectAnimator.ofFloat(infcard.idc, "alpha", 0.8F, 0.3F).setDuration(233).start()
            ObjectAnimator.ofFloat(infcard, "translationY", 0F, delta).setDuration(233).start()
        }
    }

    companion object {
        var va: WeakReference<ViewMangaActivity>? = null
        var imgUrls = arrayOf<String>()
        var titleText = "Null"
        var nextChapterUrl: String? = null
        var previousChapterUrl: String? = null
    }
}