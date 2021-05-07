package top.fumiama.copymanga.ui.home

import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Bundle
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.navigation.Navigation
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.to.aboomy.pager2banner.Banner
import com.to.aboomy.pager2banner.IndicatorView
import com.to.aboomy.pager2banner.ScaleInTransformer
import kotlinx.android.synthetic.main.card_book.view.*
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.line_1bookline.view.*
import top.fumiama.dmzj.copymanga.R
import top.fumiama.copymanga.json.ComicStructure
import top.fumiama.copymanga.json.IndexStructure
import top.fumiama.copymanga.template.AutoDownloadHandler
import top.fumiama.copymanga.tools.CMApi
import top.fumiama.copymanga.tools.UITools
import java.lang.Thread.sleep
import java.lang.ref.WeakReference

class HomeHandler(that: WeakReference<HomeFragment>) :AutoDownloadHandler(
    that.get()?.getString(R.string.mainPageApiUrl) ?: "",
    IndexStructure::class.java,
    Looper.myLooper()!!,
    9
) {
    private val homeF = that.get()
    var index: IndexStructure? = null
    var fhib: View? = null
        get() {
            Log.d("MyHH", "Get fhib.")
            if(field == null){
                field = homeF?.layoutInflater?.inflate(R.layout.viewpage_banner, homeF.fhl, false)
                Thread{homeF?.homeHandler?.sendEmptyMessage(3)}.start()
            }
            return field
        }
    var indexLines = arrayOf<View>()

    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        when (msg.what) {
            -1 -> {
                homeF?.swiperefresh?.isEnabled = msg.obj as Boolean
                homeF?.swiperefresh?.isRefreshing = msg.obj as Boolean
            }
            //0 -> setLayouts()
            1 -> inflateCardLines()

            3 -> setBanner(fhib as Banner)

            5 -> setBannerInfo(msg.obj as Banner)
            6 -> {
                homeF?.fhl?.let {
                    ObjectAnimator.ofFloat(it, "alpha", 1f, 0f).setDuration(233).start()
                    it.postDelayed({
                        it.removeAllViews()
                        ObjectAnimator.ofFloat(it, "alpha", 0f, 1f).setDuration(233).start()
                    }, 233)
                }
            }
            7 -> inflateBanner()
            8 -> homeF?.fhl?.addView(indexLines[msg.arg1])
            //9 -> checkIndex()
        }
    }

    override fun getGsonItem() = index
    override fun setGsonItem(gsonObj: Any) {
        super.setGsonItem(gsonObj)
        index = gsonObj as IndexStructure
    }
    override fun onError() {
        super.onError()
        Toast.makeText(homeF?.context, R.string.web_error, Toast.LENGTH_SHORT).show()
    }
    override fun doWhenFinishDownload() {
        super.doWhenFinishDownload()
        try {
            Thread {
                sendEmptyMessage(7)         //inflateBanner
                sendEmptyMessage(1)         //inflateCardLines
            }.start()
        } catch (e: Exception) {
            Toast.makeText(homeF?.context, R.string.load_home_error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun inflateBanner() = homeF?.fhl?.addView(fhib)

    private fun inflateTopics(){
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

    private fun inflateRec(){
        index?.results?.recComics?.list?.let {
            var comics = arrayOf<ComicStructure>()
            for((i, rec) in it.withIndex()){
                if(i > 2) break
                comics += rec.comic
            }
            if(comics.size == 3) allocateLine(homeF?.getString(R.string.manga_rec)?:"", R.drawable.img_master_work, comics) {
                homeF?.rootView?.apply { Navigation.findNavController(this).navigate(R.id.action_nav_home_to_nav_recommend) }
            }
        }
    }

    private fun inflateRank(){
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
        if(comics.size == 9) allocateLine(homeF?.getString(R.string.rank_list)?:"", R.drawable.img_novel_bill, comics)
    }

    private fun inflateHot(){
        index?.results?.hotComics?.let {
            var comics = arrayOf<ComicStructure>()
            for((i, rec) in it.withIndex()){
                if(i > 8) break
                comics += rec.comic
            }
            if(comics.size == 9) allocateLine(homeF?.getString(R.string.hot_list)?:"", R.drawable.img_hot, comics)
        }
    }

    private fun inflateNew(){
        index?.results?.newComics?.let {
            var comics = arrayOf<ComicStructure>()
            for((i, rec) in it.withIndex()){
                if(i > 8) break
                comics += rec.comic
            }
            if(comics.size == 9) allocateLine(homeF?.getString(R.string.new_list)?:"", R.drawable.img_latest_pub, comics) {
                homeF?.rootView?.apply { Navigation.findNavController(this).navigate(R.id.action_nav_home_to_nav_newest) }
            }
        }
    }

    private fun inflateFinish(){
        index?.results?.finishComics?.list?.let {
            var comics = arrayOf<ComicStructure>()
            for((i, rec) in it.withIndex()){
                if(i > 5) break
                comics += rec
            }
            if(comics.size == 6) allocateLine(homeF?.getString(R.string.complete)?:"", R.drawable.img_novel_eye, comics, true) {
                homeF?.rootView?.apply { Navigation.findNavController(this).navigate(R.id.action_nav_home_to_nav_finish) }
            }
        }
    }

    private fun inflateCardLines() {
        inflateRec()
        inflateTopics()
        inflateHot()
        inflateNew()
        inflateFinish()
        inflateRank()
        Thread{
            for(i in indexLines.indices) obtainMessage(8, i, 0).sendToTarget()
            obtainMessage(-1, false).sendToTarget()                 //closeLoad
        }.start()
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
        Thread{this.obtainMessage(5, v).sendToTarget()}.start() //setBannerInfo
        return v
    }

    private fun setBannerInfo(v: Banner){
        homeF?.context?.let { UITools(it) }?.let {
            v
                .addPageTransformer(ScaleInTransformer())
                .setPageMargin(it.dp2px(20) ?: 0, it.dp2px(10) ?: 0)
                .setIndicator(
                    IndicatorView(homeF.context)
                        .setIndicatorColor(Color.DKGRAY)
                        .setIndicatorSelectorColor(Color.WHITE)
                        .setIndicatorStyle(IndicatorView.IndicatorStyle.INDICATOR_BEZIER)
                ).adapter = homeF.ViewData(v).RecyclerViewAdapter()
        }
        v.invalidate()
        homeF?.fhov?.swipeRefreshLayout = homeF?.swiperefresh
        homeF?.swiperefresh?.setOnRefreshListener {
            Log.d("MyHFH", "Refresh items.")
            //index = null
            //Thread{this@HomeHandler.obtainMessage(-1, true).sendToTarget()}.start()  //startLoad
            Thread{
                index = null
                //fhib = null
                indexLines = arrayOf()
                this@HomeHandler.sendEmptyMessage(6)    //removeAllViews
                sleep(300)
                this@HomeHandler.sendEmptyMessage(0)    //setLayouts
            }.start()
        }
    }

    private fun allocateLine(title: String, iconResId: Int, comics: Array<ComicStructure>, finish: Boolean = false, isTopic: Boolean = false, onClick: (() -> Unit)? = null): Int{
        val p = indexLines.size
        val c = comics.size / 3
        homeF?.layoutInflater?.inflate(
            when(c){
                1 -> R.layout.line_1bookline
                2 -> R.layout.line_2bookline
                3 -> R.layout.line_3bookline
                else -> return -1
            }, homeF.fhl, false)?.apply {
            scanCards(this, comics, finish, isTopic)
            rttitle.text = title
            ir.setImageResource(iconResId)
            setLineHeight(this, c)
            if(onClick != null) setOnClickListener { onClick() }
            indexLines += this
        }
        return p
    }

    /*private fun setLines(v: View, position: Int) {
        lines?.let {
            v.rttitle.text = it[position].title
            v.ir.setImageResource(
                when (position) {
                    1 -> R.drawable.img_novel_refresh
                    2 -> R.drawable.img_novel_more
                    3 -> R.drawable.img_novel_play
                    4 -> R.drawable.img_novel_eye
                    else -> R.drawable.img_novel_refresh
                }
            )
            Thread{this.obtainMessage(4, position, 0, v).sendToTarget()}.start()    //scanCards
            setLineHeight(v, if (position == 4) 2 else 1)
            if(position == 4) {
                Thread{ this.obtainMessage(-1, false).sendToTarget() }.start()    //closeLoad
            }
            if(position == 1) v.setOnClickListener {
                Navigation.findNavController(it).navigate(R.id.action_nav_home_to_nav_latest)
            }else v.rimore.visibility = View.GONE
        }
    }*/

    private fun scanCards(v: View, comics: Array<ComicStructure>, finish: Boolean, isTopic: Boolean){
        var id = v.rc1.id
        var card = v.findViewById<ConstraintLayout>(id)
        for (data in comics){
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

    private fun setCards(cv: CardView, pw: String, name: String, img: String, isFinal: Boolean, isTopic: Boolean) {
        cv.tic.text = name
        homeF?.let {
            Glide.with(it).load(GlideUrl(img, CMApi.myGlideHeaders)).timeout(10000).into(cv.imic)
        }
        if (isFinal) cv.sgnic.visibility = View.VISIBLE
        cv.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("path", pw)
            homeF?.rootView?.let { Navigation.findNavController(it).navigate(if(isTopic) R.id.action_nav_home_to_nav_topic else R.id.action_nav_home_to_nav_book, bundle) }
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