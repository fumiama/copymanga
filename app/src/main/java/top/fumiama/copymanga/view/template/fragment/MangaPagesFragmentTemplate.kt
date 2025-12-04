package top.fumiama.copymanga.view.template.fragment

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.animation.doOnEnd
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.liaoinstan.springview.widget.SpringView
import kotlinx.android.synthetic.main.line_header.view.*
import kotlinx.android.synthetic.main.line_lazybooklines.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.fumiama.copymanga.MainActivity
import top.fumiama.copymanga.view.interaction.UITools
import top.fumiama.copymanga.view.template.component.CardList
import java.lang.ref.WeakReference

open class MangaPagesFragmentTemplate(
    inflateRes:Int, private val isLazy: Boolean = true,
    val forceLoad: Boolean = false
) : NoBackRefreshFragment(inflateRes) {
    var cardPerRow = 3
    var cardWidth = 0
    var cardHeight = 0
    var cardList: CardList? = null
    var isEnd = false
    var page = 0

    var isRefresh = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if(isFirstInflate) {
            val tb = (activity as MainActivity).toolsBox
            val netInfo = tb.netInfo
            if (!forceLoad && (netInfo == tb.transportStringNull || netInfo == tb.transportStringError)) {
                findNavController().popBackStack()
                return
            }
            lifecycleScope.launch {
                showKanban()
                withContext(Dispatchers.IO) {
                    delay(600)
                    setLayouts()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cardList?.exitCardList = true
    }

    private suspend fun setLayouts() = withContext(Dispatchers.IO) {
        if (!isFirstInflate) return@withContext
        val toolsBox = this@MangaPagesFragmentTemplate.context?.let { UITools(it) }
        val widthData = toolsBox?.calcWidthFromDp(8, 135)
        cardPerRow = widthData?.get(0) ?: 3
        cardWidth = widthData?.get(2) ?: 128
        cardHeight = (cardWidth / 0.75 + 0.5).toInt()
        withContext(Dispatchers.Main) {
            mysp?.footerView?.lht?.text = "加载"
            mysp?.headerView?.lht?.text = "刷新"
            mydll?.setPadding(0, 0, 0, navBarHeight)
        }
        Log.d("MyMPAT", "Card per row: $cardPerRow")
        Log.d("MyMPAT", "Card width: $cardWidth")
        initCardList(WeakReference(this@MangaPagesFragmentTemplate))

        addPage()
        if (isLazy) { mysp.apply { post {
            setListener(object : SpringView.OnFreshListener {
                override fun onLoadmore() {
                    lifecycleScope.launch {
                        addPage()
                    }
                }
                override fun onRefresh() {
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            showKanban()
                            reset()
                            delay(600)
                            addPage()
                            hideKanban()
                        }
                    }
                }
            })
        } } }

        setListeners()
        hideKanban()
    }

    open suspend fun addPage() {}

    open suspend fun onLoadFinish() = withContext(Dispatchers.Main) {
        mypc?.visibility = View.GONE
        mysp?.onFinishFreshAndLoad()
        //mys?.fullScroll(ScrollView.FOCUS_UP)
    }

    open suspend fun reset() = withContext(Dispatchers.Main) {
        mydll.removeAllViews()
        isEnd = false
        page = 0
        cardList?.reset()
        mypc?.visibility = View.VISIBLE
        mypl?.progress = 0
    }

    open fun initCardList(weakReference: WeakReference<Fragment>) {}

    open fun setListeners() {}

    fun setProgress(p: Int) {
        var newP = p
        mypl?.post {
            if (p == mypl?.progress) return@post
            if (newP >= 100) newP = 100
            else if (newP < 0) newP = 0
            if (mypl?.progress == 0) {
                Log.d("MyMPFT", "set from 0, show")
                mypc?.apply {
                    visibility = View.VISIBLE
                    invalidate()
                    ObjectAnimator.ofFloat(this, "alpha", 0f, 1f)
                        .setDuration(300)
                        .start()
                }
            }
            if(newP == 100) {
                Log.d("MyMPFT", "set to 100, hide")
                mypc?.apply {
                    val oa = ObjectAnimator.ofFloat(this, "alpha", 1f, 0f).setDuration(300)
                    oa.doOnEnd { visibility = View.GONE }
                    oa.start()
                }
            }
            mypl?.apply {
                val oa = ObjectAnimator.ofInt(this, "progress", newP).setDuration(100)
                oa.addUpdateListener { invalidate() }
                oa.start()
                Log.d("MyMPFT", "set $progress")
            }
        }
    }
}
