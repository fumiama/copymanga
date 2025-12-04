package top.fumiama.copymanga.ui.home

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
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
import top.fumiama.copymanga.api.Config
import top.fumiama.copymanga.json.ComicStructure
import top.fumiama.copymanga.json.IndexStructure
import top.fumiama.copymanga.net.template.AutoDownloadHandler
import top.fumiama.copymanga.view.interaction.Navigate
import top.fumiama.copymanga.view.interaction.UITools
import top.fumiama.copymanga.view.operation.GlideHideLottieViewListener
import top.fumiama.dmzj.copymanga.R
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicInteger

class HomeHandler(private val that: WeakReference<HomeFragment>) : AutoDownloadHandler({
        that.get()?.getString(R.string.mainPageApiUrl)!!.format(Config.platform.value)
    },
    IndexStructure::class.java,
    that.get()
) {
    private val homeFragment get() = that.get()
    var homeIndex: IndexStructure? = null
    private var fhib: Banner? = null
        get() {
            Log.d("MyHH", "Get fhib.")
            if (field == null) {
                field = homeFragment?.layoutInflater?.inflate(R.layout.viewpage_banner, homeFragment?.fhl, false) as Banner
                homeFragment?.homeHandler?.sendEmptyMessage(3)
            }
            return field
        }

    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        when (msg.what) {
            -1 -> {
                homeFragment?.apply {
                    swiperefresh?.isRefreshing = msg.obj as Boolean
                    if(msg.obj as Boolean) showKanban() else hideKanban()
                }
            }
            //0 -> setLayouts()
            1 -> inflateCardLines()
            2 -> homeFragment?.swiperefresh?.let { setSwipe(it) }
            3 -> setBanner(fhib!!)
            5 -> setBannerInfo(msg.obj as Banner)
            7 -> inflateBanner()
        }
    }

    override fun getGsonItem() = homeIndex
    override fun setGsonItem(gsonObj: Any) :Boolean {
        val pass = super.setGsonItem(gsonObj)
        homeIndex = gsonObj as IndexStructure
        var banners = arrayOf<IndexStructure.Results.Banners>()
        homeIndex?.results?.banners?.forEach {
            if(it.type == 1) {
                banners += it
            }
        }
        homeIndex?.results?.banners = banners
        return pass
    }
    override suspend fun onError() {
        super.onError()
        if(exit) return
        sendEmptyMessage(2)         //setSwipe
        obtainMessage(-1, false).sendToTarget()                 //closeLoad
        withContext(Dispatchers.Main) {
            Toast.makeText(homeFragment?.context, R.string.web_error, Toast.LENGTH_SHORT).show()
        }
    }
    override suspend fun doWhenFinishDownload(): Unit = withContext(Dispatchers.IO) {
        super.doWhenFinishDownload()
        raw?.let {
            Log.d("MyHFH", "save raw: $it")
            homeFragment?.apply { activity?.runOnUiThread {
                vm.saveIndexStructure(it)
            } }
        }
    }

    private fun inflateBanner() {
        homeFragment?.fhl?.let { it.post {
            fhib = null
            it.addView(fhib)
        } }
    }

    private suspend fun inflateTopics() {
        homeIndex?.results?.topics?.list?.let {
            var comics = arrayOf<ComicStructure>()
            for((i, topic) in it.withIndex()){
                if(i > 2) break
                val newComic = ComicStructure()
                newComic.name = topic.title
                newComic.cover = topic.cover
                newComic.path_word = topic.path_word
                comics += newComic
            }
            if(comics.size == 3) allocateLine(homeFragment?.getString(R.string.topics_series)?:"", R.drawable.img_hot_serial, comics, isTopic = true)
        }
    }

    private suspend fun inflateRec() {
        homeIndex?.results?.recComics?.list?.let {
            var comics = arrayOf<ComicStructure>()
            for((i, rec) in it.withIndex()){
                if(i > 2) break
                comics += rec.comic
            }
            if(comics.size == 3) allocateLine(homeFragment?.getString(R.string.manga_rec)?:"", R.drawable.img_master_work, comics) {
                homeFragment?.findNavController()?.let { nav ->
                    Navigate.safeNavigateTo(nav, R.id.action_nav_home_to_nav_recommend)
                }
            }
        }
    }

    private suspend fun inflateRank(){
        var comics = arrayOf<ComicStructure>()
        if (homeIndex?.results?.rankDayComics == null) {
            // is in hotmanga
            homeIndex?.results?.rankWeeklyFreeComics?.list?.let {
                for((i, book) in it.withIndex()){
                    if(i > 2) break
                    comics += book.comic
                }
            }
            if(comics.size == 3) allocateLine(homeFragment?.getString(R.string.hot_rank_list)?:"", R.drawable.img_novel_bill, comics) {
                homeFragment?.findNavController()?.navigate(R.id.nav_rank)
            }
            return
        }
        homeIndex?.results?.rankDayComics?.list?.let {
            for((i, book) in it.withIndex()){
                if(i > 2) break
                comics += book.comic
            }
        }
        homeIndex?.results?.rankWeekComics?.list?.let {
            for((i, book) in it.withIndex()){
                if(i > 2) break
                comics += book.comic
            }
        }
        homeIndex?.results?.rankMonthComics?.list?.let {
            for((i, book) in it.withIndex()){
                if(i > 2) break
                comics += book.comic
            }
        }
        if(comics.size == 9) allocateLine(homeFragment?.getString(R.string.rank_list)?:"", R.drawable.img_novel_bill, comics) {
            homeFragment?.findNavController()?.navigate(R.id.nav_rank)
        }
    }

    private suspend fun inflateHot(){
        if (homeIndex?.results?.hotComics == null) {
            // is in hotmanga
            homeIndex?.results?.updateWeeklyFreeComics?.let {
                var comics = arrayOf<ComicStructure>()
                for((i, rec) in it.list.withIndex()){
                    if(i > 5) break
                    comics += rec.comic
                }
                if(comics.size == 6) allocateLine(homeFragment?.getString(R.string.hot_list)?:"", R.drawable.img_hot, comics)
            }
            return
        }
        homeIndex?.results?.hotComics?.let {
            var comics = arrayOf<ComicStructure>()
            for((i, rec) in it.withIndex()){
                if(i > 8) break
                comics += rec.comic
            }
            if(comics.size == 9) allocateLine(homeFragment?.getString(R.string.hot_list)?:"", R.drawable.img_hot, comics)
        }
    }

    private suspend fun inflateNew(){
        homeIndex?.results?.newComics?.let {
            var comics = arrayOf<ComicStructure>()
            for((i, rec) in it.withIndex()){
                if(i > 8) break
                comics += rec.comic
            }
            if(comics.size == 9) allocateLine(homeFragment?.getString(R.string.new_list)?:"", R.drawable.img_latest_pub, comics) {
                homeFragment?.findNavController()?.let { nav ->
                    Navigate.safeNavigateTo(nav, R.id.action_nav_home_to_nav_newest)
                }
            }
        }
    }

    private suspend fun inflateFinish(){
        homeIndex?.results?.finishComics?.list?.let {
            var comics = arrayOf<ComicStructure>()
            for((i, rec) in it.withIndex()){
                if(i > 5) break
                comics += rec
            }
            if(comics.size == 6) allocateLine(homeFragment?.getString(R.string.complete)?:"", R.drawable.img_novel_eye, comics, true) {
                homeFragment?.findNavController()?.let { nav ->
                    Navigate.safeNavigateTo(nav, R.id.action_nav_home_to_nav_finish)
                }
            }
        }
    }

    private fun inflateCardLines() {
        homeFragment?.lifecycleScope?.launch {
            withContext(Dispatchers.IO) {
                inflateRec()
                inflateTopics()
                inflateHot()
                inflateNew()
                inflateFinish()
                inflateRank()
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
        homeFragment?.context?.let { UITools(it) }?.let {
            v
                .addPageTransformer(ScaleInTransformer())
                .setPageMargin(it.dp2px(20) ?: 0, it.dp2px(10) ?: 0)
                .setIndicator(
                    IndicatorView(homeFragment!!.context)
                        .setIndicatorColor(Color.DKGRAY)
                        .setIndicatorSelectorColor(Color.WHITE)
                        .setIndicatorStyle(IndicatorView.IndicatorStyle.INDICATOR_BEZIER)
                ).adapter = homeFragment?.ViewData(v)?.RecyclerViewAdapter()
        }
        v.invalidate()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setSwipe(sw: SwipeRefreshLayout) {
        homeFragment?.fhov?.swipeRefreshLayout = sw
        sw.setOnRefreshListener {
            Log.d("MyHFH", "Refresh items.")
            homeFragment?.lifecycleScope?.launch {
                withContext(Dispatchers.IO) {
                    homeFragment?.showKanban()
                    fhib?.let {
                        it.isAutoPlay = false
                        homeIndex = null
                        it.adapter?.notifyDataSetChanged()
                    }
                    fhib = null
                    delay(300)
                    withContext(Dispatchers.Main) {
                        homeFragment?.fhl?.let {
                            val oa = ObjectAnimator.ofFloat(it, "alpha", 1f, 0f).setDuration(233)
                            oa.doOnEnd { _ ->
                                it.removeAllViews()
                                it.alpha = 1f
                                homeFragment?.vm?.saveIndexStructure(null) // reload
                            }
                            oa.start()
                        }
                    }
                }
            }
        }
    }

    private suspend fun allocateLine(
        title: String, iconResId: Int, comics: Array<ComicStructure>,
        finish: Boolean = false, isTopic: Boolean = false, onClick: (() -> Unit)? = null
    ): Unit = withContext(Dispatchers.IO) {
        val c = comics.size / 3
        homeFragment?.layoutInflater?.inflate(
            when(c){
                1 -> R.layout.line_1bookline
                2 -> R.layout.line_2bookline
                3 -> R.layout.line_3bookline
                else -> return@withContext
        }, null, false)?.apply {
            withContext(Dispatchers.Main) {
                scanCards(this@apply, comics, finish, isTopic)
                post {
                    rttitle.text = title
                    ir.setImageResource(iconResId)
                    setLineHeight(this@apply, c)
                    if(onClick != null) setOnClickListener { onClick() }
                }
            }
            homeFragment?.fhl?.let { it.post { it.addView(this) } }
        }
        return@withContext
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

    private fun setCards(cv: CardView, pw: String, name: String, img: String, isFinal: Boolean, isTopic: Boolean) {
        cv.tic.apply { post { text = name } }
        homeFragment?.let {
            if(img.startsWith("http")) {
                //Log.d("MyHH", "load card image: $img")
                val waitMillis = cardLoadingWaits.getAndIncrement().toLong()*200
                val g = Glide.with(it).load(GlideUrl(Config.imageProxy?.wrap(img)?:img, Config.myGlideHeaders))
                    .addListener(GlideHideLottieViewListener(WeakReference(cv.laic)) {
                        cardLoadingWaits.decrementAndGet()
                    }).timeout(60000)
                if (waitMillis > 0) cv.imic.postDelayed({
                    g.into(cv.imic)
                }, waitMillis) else cv.imic.post { g.into(cv.imic) }
            }
        }
        if (isFinal) cv.sgnic.apply { post { visibility = View.VISIBLE } }
        cv.post {
            cv.setOnClickListener {
                val bundle = Bundle()
                bundle.putString("path", pw)
                homeFragment?.findNavController()?.let { nav ->
                    Navigate.safeNavigateTo(nav, if(isTopic) R.id.action_nav_home_to_nav_topic else R.id.action_nav_home_to_nav_book, bundle)
                }
            }
        }
    }

    private fun setLineHeight(v: View, cardCount: Int) {
        v.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                homeFragment?.context?.let { UITools(it) }?.let {
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
