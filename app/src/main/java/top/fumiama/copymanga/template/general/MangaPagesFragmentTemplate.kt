package top.fumiama.copymanga.template.general

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.JsonReader
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.line_header.view.*
import kotlinx.android.synthetic.main.line_lazybooklines.*
import top.fumiama.copymanga.template.ui.CardList
import top.fumiama.copymanga.template.handler.MPATHandler
import top.fumiama.copymanga.tools.api.UITools
import java.lang.Thread.sleep
import java.lang.ref.WeakReference

open class MangaPagesFragmentTemplate(inflateRes:Int, val isLazy: Boolean = true, val forceLoad: Boolean = false) : NoBackRefreshFragment(inflateRes) {
    var cardPerRow = 3
    var cardWidth = 0
    var cardHeight = 0
    lateinit var cardList: CardList
    var mh: MPATHandler? = null
    var row: View? = null
    var isEnd = false
    var jsonReaderNow: JsonReader? = null
    var page = 0

    var isRefresh = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if(isFirstInflate) {
            mh = MPATHandler(WeakReference(this))
            Thread {
                sleep(600)
                mh?.sendEmptyMessage(0)
            }.start()
        }
    }

    fun setLayouts() {
        val toolsBox = this.context?.let { UITools(it) }
        val widthData = toolsBox?.calcWidthFromDp(8, 135)
        cardPerRow = widthData?.get(0) ?: 3
        cardWidth = widthData?.get(2) ?: 128
        cardHeight = (cardWidth / 0.75 + 0.5).toInt()
        mysp.footerView.lht.text = "加载"
        mysp.headerView.lht.text = "刷新"
        Log.d("MyMPAT", "Card per row: $cardPerRow")
        Log.d("MyMPAT", "Card width: $cardWidth")

        pageHandler?.initCardList(WeakReference(this))
        Thread { mh?.sendEmptyMessage(1) }.start()
        pageHandler?.setListeners()
        //mypl.visibility = View.GONE
    }

    var pageHandler: PageHandler? = null

    interface PageHandler {
        fun addPage()
        fun initCardList(weakReference: WeakReference<Fragment>)
        fun setListeners()
    }
}