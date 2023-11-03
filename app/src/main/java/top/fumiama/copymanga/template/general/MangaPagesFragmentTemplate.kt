package top.fumiama.copymanga.template.general

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.liaoinstan.springview.widget.SpringView
import kotlinx.android.synthetic.main.line_header.view.*
import kotlinx.android.synthetic.main.line_lazybooklines.*
import top.fumiama.copymanga.MainActivity
import top.fumiama.copymanga.template.ui.CardList
import top.fumiama.copymanga.tools.ui.UITools
import top.fumiama.dmzj.copymanga.R
import java.lang.Thread.sleep
import java.lang.ref.WeakReference

open class MangaPagesFragmentTemplate(inflateRes:Int, val isLazy: Boolean = true, val forceLoad: Boolean = false) : NoBackRefreshFragment(inflateRes) {
    var cardPerRow = 3
    var cardWidth = 0
    var cardHeight = 0
    var cardList: CardList? = null
    //var row: View? = null
    var isEnd = false
    //var jsonReaderNow: JsonReader? = null
    var page = 0

    var isRefresh = false

    private val transportStringNull = context?.getString(R.string.TRANSPORT_NULL) ?: "TRANSPORT_NULL"
    private val transportStringError = context?.getString(R.string.TRANSPORT_ERROR) ?: "TRANSPORT_ERROR"
    private val netInfo: String
        get() {
            val cm: ConnectivityManager =
                context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            return cm.getNetworkCapabilities(cm.activeNetwork)?.let {
                when {
                    it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> return@let context?.getString(
                        R.string.TRANSPORT_WIFI)
                    it.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> return@let context?.getString(
                        R.string.TRANSPORT_CELLULAR)
                    it.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> return@let context?.getString(
                        R.string.TRANSPORT_BLUETOOTH)
                    it.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> return@let context?.getString(
                        R.string.TRANSPORT_ETHERNET)
                    it.hasTransport(NetworkCapabilities.TRANSPORT_LOWPAN) -> return@let context?.getString(
                        R.string.TRANSPORT_LOWPAN)
                    it.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> return@let "VPN"
                    else -> return@let transportStringNull
                }
            } ?: transportStringError
        }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if(isFirstInflate) {
            if (!forceLoad && (netInfo == transportStringNull || netInfo == transportStringError)) {
                findNavController().popBackStack()
                return
            }
            Thread {
                sleep(600)
                activity?.runOnUiThread {
                    setLayouts()
                }
            }.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cardList?.exitCardList = true
        //row = null
        //jsonReaderNow = null
    }

    open fun setLayouts() {
        val toolsBox = this.context?.let { UITools(it) }
        val widthData = toolsBox?.calcWidthFromDp(8, 135)
        cardPerRow = widthData?.get(0) ?: 3
        cardWidth = widthData?.get(2) ?: 128
        cardHeight = (cardWidth / 0.75 + 0.5).toInt()
        mysp.footerView.lht.text = "加载"
        mysp.headerView.lht.text = "刷新"
        Log.d("MyMPAT", "Card per row: $cardPerRow")
        Log.d("MyMPAT", "Card width: $cardWidth")

        mydll?.setPadding(0, 0, 0, navBarHeight)

        initCardList(WeakReference(this))
        managePage()
        setListeners()
        //mypl.visibility = View.GONE
    }

    private fun managePage() {
        addPage()
        if (isLazy) mysp.setListener(object : SpringView.OnFreshListener {
            override fun onLoadmore() {
                addPage()
            }
            override fun onRefresh() {
                reset()
                Thread {
                    sleep(600)
                    activity?.runOnUiThread {
                        addPage()
                    }
                }.start()
            }
        })
    }

    open fun addPage() {}

    open fun onLoadFinish() {
        //myp?.visibility = View.GONE
        mysp?.onFinishFreshAndLoad()
        //mys?.fullScroll(ScrollView.FOCUS_UP)
    }
    
    open fun reset() {
        mydll.removeAllViews()
        isEnd = false
        page = 0
        cardList?.reset()
        mypl?.visibility = View.VISIBLE
    }

    open fun initCardList(weakReference: WeakReference<Fragment>) {}

    open fun setListeners() {}
}