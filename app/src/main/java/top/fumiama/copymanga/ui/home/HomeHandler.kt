package top.fumiama.copymanga.ui.home

import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.to.aboomy.pager2banner.Banner
import com.to.aboomy.pager2banner.IndicatorView
import com.to.aboomy.pager2banner.ScaleInTransformer
import kotlinx.android.synthetic.main.card_book.view.*
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.line_1bookline.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.fumiama.copymanga.json.ComicStructure
import top.fumiama.copymanga.json.IndexStructure
import top.fumiama.copymanga.template.http.AutoDownloadHandler
import top.fumiama.copymanga.tools.api.CMApi
import top.fumiama.copymanga.tools.ui.GlideHideLottieViewListener
import top.fumiama.copymanga.tools.ui.Navigate
import top.fumiama.copymanga.tools.ui.UITools
import top.fumiama.dmzj.copymanga.R
import java.lang.Thread.sleep
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicInteger

class HomeHandler(private val that: WeakReference<HomeFragment>) : AutoDownloadHandler(
    that.get()?.getString(R.string.mainPageApiUrl)!!.format(CMApi.myHostApiUrl),
    IndexStructure::class.java,
    that.get(),
    9
) {
    private val homeF get() = that.get()
    var index: IndexStructure? = null
    var fhib: View? = null
        get() {
            Log.d("MyHH", "Get fhib.")
            if (field == null) {
                field = homeF?.layoutInflater?.inflate(R.layout.viewpage_banner, homeF?.fhl, false)
                homeF?.homeHandler?.sendEmptyMessage(3)
            }
            return field
        }
    private var indexLines = arrayOf<View>()

    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        when (msg.what) {
            -1 -> homeF?.swiperefresh?.isRefreshing = msg.obj as Boolean
            //0 -> setLayouts()
            1 -> inflateCardLines()
            2 -> homeF?.swiperefresh?.let { setSwipe(it) }
            3 -> setBanner(fhib as Banner)
            5 -> setBannerInfo(msg.obj as Banner)
            6 -> {
                homeF?.fhl?.let {
                    val oa = ObjectAnimator.ofFloat(it, "alpha", 1f, 0f).setDuration(233)
                    oa.doOnEnd { _ ->
                        it.removeAllViews()
                        it.alpha = 1f
                    }
                    oa.start()
                }
            }
            7 -> inflateBanner()
            8 -> {
                try {
                    homeF?.fhl?.addView(indexLines[msg.arg1])
                } catch (e: Exception) {
                    e.printStackTrace()
                    (indexLines[msg.arg1].parent as LinearLayout).removeAllViews()
                    homeF?.fhl?.addView(indexLines[msg.arg1])
                }
            }
            //9 -> checkIndex()
        }
    }

    override fun getGsonItem() = index
    override fun setGsonItem(gsonObj: Any) :Boolean {
        val pass = super.setGsonItem(gsonObj)
        index = gsonObj as IndexStructure
        var banners = arrayOf<IndexStructure.Results.Banners>()
        index?.results?.banners?.forEach {
            if(it.type == 1) {
                banners += it
            }
        }
        index?.results?.banners = banners
        return pass
    }
    override fun onError() {
        super.onError()
        if(exit) return
        Toast.makeText(homeF?.context, R.string.web_error, Toast.LENGTH_SHORT).show()
    }
    override suspend fun doWhenFinishDownload() = withContext(Dispatchers.IO) {
        super.doWhenFinishDownload()
        if(exit) return@withContext
        sendEmptyMessage(2)         //setSwipe
        sendEmptyMessage(7)         //inflateBanner
        sendEmptyMessage(1)         //inflateCardLines
    }

    private fun inflateBanner() = homeF?.fhl?.addView(fhib)

    private suspend fun inflateTopics() {
        index?.results?.topics?.list?.let {
            var comics = arrayOf<ComicStructure>()
            for((i, topic) in it.withIndex()){
                if(i > 2) break
                val newComic = ComicStructure()
                newComic.name = topic.title
                newComic.cover = topic.cover
                newComic.path_word = topic.path_word
                comics += newComic
            }
            if(comics.size == 3) allocateLine(homeF?.getString(R.string.topics_series)?:"", R.drawable.img_hot_serial, comics, isTopic = true)
        }
    }

    private suspend fun inflateRec() {
        index?.results?.recComics?.list?.let {
            var comics = arrayOf<ComicStructure>()
            for((i, rec) in it.withIndex()){
                if(i > 2) break
                comics += rec.comic
            }
            if(comics.size == 3) allocateLine(homeF?.getString(R.string.manga_rec)?:"", R.drawable.img_master_work, comics) {
                homeF?.findNavController()?.let { nav ->
                    Navigate.safeNavigateTo(nav, R.id.action_nav_home_to_nav_recommend)
                }
            }
        }
    }

    private suspend fun inflateRank(){
        var comics = arrayOf<ComicStructure>()
        index?.results?.rankDayComics?.list?.let {
            for((i, book) in it.withIndex()){
                if(i > 2) break
                comics += book.comic
            }
        }
        index?.results?.rankWeekComics?.list?.let {
            for((i, book) in it.withIndex()){
                if(i > 2) break
                comics += book.comic
            }
        }
        index?.results?.rankMonthComics?.list?.let {
            for((i, book) in it.withIndex()){
                if(i > 2) break
                comics += book.comic
            }
        }
        if(comics.size == 9) allocateLine(homeF?.getString(R.string.rank_list)?:"", R.drawable.img_novel_bill, comics) {
            homeF?.findNavController()?.navigate(R.id.nav_rank)
        }
    }

    private suspend fun inflateHot(){
        index?.results?.hotComics?.let {
            var comics = arrayOf<ComicStructure>()
            for((i, rec) in it.withIndex()){
                if(i > 8) break
                comics += rec.comic
            }
            if(comics.size == 9) allocateLine(homeF?.getString(R.string.hot_list)?:"", R.drawable.img_hot, comics)
        }
    }

    private suspend fun inflateNew(){
        index?.results?.newComics?.let {
            var comics = arrayOf<ComicStructure>()
            for((i, rec) in it.withIndex()){
                if(i > 8) break
                comics += rec.comic
            }
            if(comics.size == 9) allocateLine(homeF?.getString(R.string.new_list)?:"", R.drawable.img_latest_pub, comics) {
                homeF?.findNavController()?.let { nav ->
                    Navigate.safeNavigateTo(nav, R.id.action_nav_home_to_nav_newest)
                }
            }
        }
    }

    private suspend fun inflateFinish(){
        index?.results?.finishComics?.list?.let {
            var comics = arrayOf<ComicStructure>()
            for((i, rec) in it.withIndex()){
                if(i > 5) break
                comics += rec
            }
            if(comics.size == 6) allocateLine(homeF?.getString(R.string.complete)?:"", R.drawable.img_novel_eye, comics, true) {
                homeF?.findNavController()?.let { nav ->
                    Navigate.safeNavigateTo(nav, R.id.action_nav_home_to_nav_finish)
                }
            }
        }
    }

    private fun inflateCardLines() {
        homeF?.lifecycleScope?.launch {
            withContext(Dispatchers.IO) {
                if (indexLines.isNotEmpty()) indexLines = arrayOf()
                inflateRec()
                inflateTopics()
                inflateHot()
                inflateNew()
                inflateFinish()
                inflateRank()
                for(i in indexLines.indices) {
                    obtainMessage(8, i, 0).sendToTarget()
                    delay(512)
                }
                obtainMessage(-1, false).sendToTarget()                 //closeLoad
            }
        }
    }

    private fun setBanner(v: Banner): Banner {
        v.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                //Log.d("MyMy", "Width: ${v.width}")
                v.layoutParams.height = (v.width / 1.875 + 0.5).toInt()
                v.invalidate()
                v.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
        obtainMessage(5, v).sendToTarget() //setBannerInfo
        return v
    }

    private fun setBannerInfo(v: Banner){
        homeF?.context?.let { UITools(it) }?.let {
            v
                .addPageTransformer(ScaleInTransformer())
                .setPageMargin(it.dp2px(20) ?: 0, it.dp2px(10) ?: 0)
                .setIndicator(
                    IndicatorView(homeF!!.context)
                        .setIndicatorColor(Color.DKGRAY)
                        .setIndicatorSelectorColor(Color.WHITE)
                        .setIndicatorStyle(IndicatorView.IndicatorStyle.INDICATOR_BEZIER)
                ).adapter = homeF?.ViewData(v)?.RecyclerViewAdapter()
        }
        v.invalidate()
    }

    private fun setSwipe(sw: SwipeRefreshLayout) {
        homeF?.fhov?.swipeRefreshLayout = sw
        sw.setOnRefreshListener {
            Log.d("MyHFH", "Refresh items.")
            homeF?.lifecycleScope?.launch {
                withContext(Dispatchers.IO) {
                    index = null
                    //fhib = null
                    indexLines = arrayOf()
                    this@HomeHandler.sendEmptyMessage(6)    //removeAllViews
                    delay(300)
                    this@HomeHandler.sendEmptyMessage(0)    //setLayouts
                }
            }
        }
    }

    private suspend fun allocateLine(
        title: String, iconResId: Int, comics: Array<ComicStructure>,
        finish: Boolean = false, isTopic: Boolean = false, onClick: (() -> Unit)? = null
    ): Int = withContext(Dispatchers.IO) {
        val p = indexLines.size
        val c = comics.size / 3
        homeF?.layoutInflater?.inflate(
            when(c){
                1 -> R.layout.line_1bookline
                2 -> R.layout.line_2bookline
                3 -> R.layout.line_3bookline
                else -> return@withContext -1
        }, homeF!!.fhl, false)?.apply {
            withContext(Dispatchers.Main) {
                scanCards(this@apply, comics, finish, isTopic)
                rttitle.text = title
                ir.setImageResource(iconResId)
                setLineHeight(this@apply, c)
                if(onClick != null) setOnClickListener { onClick() }
            }
            indexLines += this
        }
        return@withContext p
    }

    private suspend fun scanCards(v: View, comics: Array<ComicStructure>, finish: Boolean, isTopic: Boolean) = withContext(Dispatchers.IO) {
        var id = v.rc1.id
        var card = v.findViewById<ConstraintLayout>(id)
        for (data in comics) {
            setCards(
                card.cic,
                data.path_word,
                data.name,
                data.cover,
                finish,
                isTopic
            )
            card = v.findViewById(++id)
        }
    }

    private var cardLoadingWaits = AtomicInteger()

    private suspend fun setCards(cv: CardView, pw: String, name: String, img: String, isFinal: Boolean, isTopic: Boolean) = withContext(Dispatchers.Main) {
        cv.tic.text = name
        homeF?.let {
            if(img.startsWith("http")) {
                Log.d("MyHH", "load card image: $img")
                val waitMillis = cardLoadingWaits.getAndIncrement().toLong()*200
                val g = Glide.with(it).load(GlideUrl(CMApi.proxy?.wrap(img)?:img, CMApi.myGlideHeaders))
                    .addListener(GlideHideLottieViewListener(WeakReference(cv.laic)) {
                        cardLoadingWaits.decrementAndGet()
                    })
                if (waitMillis > 0) cv.imic.postDelayed({
                    g.into(cv.imic)
                }, waitMillis) else g.into(cv.imic)
            }
        }
        if (isFinal) cv.sgnic.visibility = View.VISIBLE
        cv.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("path", pw)
            homeF?.findNavController()?.let { nav ->
                Navigate.safeNavigateTo(nav, if(isTopic) R.id.action_nav_home_to_nav_topic else R.id.action_nav_home_to_nav_book, bundle)
            }
        }
    }

    private fun setLineHeight(v: View, cardCount: Int) {
        v.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                homeF?.context?.let { UITools(it) }?.let {
                    val spaceTitle = it.dp2px(49)!!
                    val cardSpace = it.dp2px(16)!!
                    v.layoutParams.height =
                        ((v.width - cardSpace * 3) * cardCount * 4.0 / 9.0 + spaceTitle + cardSpace * cardCount + 0.5).toInt()
                    v.invalidate()
                    v.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
                //Log.d("MyVTOL", "Set card line: (${v.width}, ${v.height})")
            }
        })
    }
}
