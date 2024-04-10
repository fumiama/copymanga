package top.fumiama.copymanga.template.general

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
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
import top.fumiama.copymanga.template.ui.CardList
import top.fumiama.copymanga.tools.ui.UITools
import top.fumiama.dmzj.copymanga.R
import java.lang.ref.WeakReference

open class MangaPagesFragmentTemplate(inflateRes:Int, private val isLazy: Boolean = true, val forceLoad: Boolean = false) : NoBackRefreshFragment(inflateRes) {
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
        //row = null
        //jsonReaderNow = null
    }

    open suspend fun setLayouts() = withContext(Dispatchers.IO) {
        val toolsBox = this@MangaPagesFragmentTemplate.context?.let { UITools(it) }
        val widthData = toolsBox?.calcWidthFromDp(8, 135)
        cardPerRow = widthData?.get(0) ?: 3
        cardWidth = widthData?.get(2) ?: 128
        cardHeight = (cardWidth / 0.75 + 0.5).toInt()
        withContext(Dispatchers.Main) {
            mysp.footerView.lht.text = "加载"
            mysp.headerView.lht.text = "刷新"
            mydll?.setPadding(0, 0, 0, navBarHeight)
        }
        Log.d("MyMPAT", "Card per row: $cardPerRow")
        Log.d("MyMPAT", "Card width: $cardWidth")
        initCardList(WeakReference(this@MangaPagesFragmentTemplate))
        managePage()
        setListeners()
        hideKanban()
    }

    private suspend fun managePage() {
        addPage()
        if (isLazy) {
            mysp.apply {
                post {
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
                }
            }
        }
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
            if (newP >= 100) {
                Log.d("MyMPFT", "set 100, hide")
                mypc?.visibility = View.GONE
                return@post
            }
            else if (newP < 0) newP = 0
            mypl?.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    setProgress(newP, true)
                } else progress = newP
                invalidate()
                Log.d("MyMPFT", "set ${mypl?.progress}")
            }
        }
    }
}
