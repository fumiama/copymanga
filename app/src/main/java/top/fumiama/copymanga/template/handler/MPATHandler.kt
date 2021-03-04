package top.fumiama.copymanga.template.handler

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Message
import android.view.View
import android.widget.Toast
import com.liaoinstan.springview.widget.SpringView
import top.fumiama.dmzj.copymanga.R
import kotlinx.android.synthetic.main.line_lazybooklines.*
import top.fumiama.copymanga.template.MangaPagesFragmentTemplate
import java.lang.ref.WeakReference


class MPATHandler(private val w: WeakReference<MangaPagesFragmentTemplate>) : Handler() {
    private val wa get() = w.get()
    private val netinfo: String
        get() {
            val cm: ConnectivityManager =
                wa?.context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            return cm.getNetworkCapabilities(cm.activeNetwork)?.let {
                when {
                    it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> return@let wa?.context?.getString(
                        R.string.TRANSPORT_WIFI)
                    it.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> return@let wa?.context?.getString(
                        R.string.TRANSPORT_CELLULAR)
                    it.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> return@let wa?.context?.getString(
                        R.string.TRANSPORT_BLUETOOTH)
                    it.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> return@let wa?.context?.getString(
                        R.string.TRANSPORT_ETHERNET)
                    it.hasTransport(NetworkCapabilities.TRANSPORT_LOWPAN) -> return@let wa?.context?.getString(
                        R.string.TRANSPORT_LOWPAN)
                    it.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> return@let "VPN"
                    else -> return@let wa?.context?.getString(R.string.TRANSPORT_NULL)
                }
            } ?: "错误"
        }
    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        if (wa?.forceLoad == true || netinfo != "无网络" && netinfo != "错误") {
            when (msg.what) {
                0 -> wa?.setLayouts()
                1 -> managePage()
                2 -> addPageHandler()
                3 -> {
                    wa?.pageHandler?.addPage()
                    //wa?.myp?.visibility = View.GONE
                    wa?.mysp?.onFinishFreshAndLoad()
                    //wa?.mys?.fullScroll(ScrollView.FOCUS_UP)
                }
                4 ->{
                    wa?.mydll?.removeAllViews()
                    wa?.isEnd = false
                    wa?.jsonReaderNow = null
                    wa?.page = 0
                    wa?.cardList?.reset()
                    addPageHandler()
                    wa?.mysp?.onFinishFreshAndLoad()
                    wa?.mypl?.visibility = View.VISIBLE
                }
            }
        } else Toast.makeText(wa?.context, "${netinfo}链接!", Toast.LENGTH_SHORT).show()
    }

    private fun managePage() {
        addPageHandler()
        if (wa?.isLazy == true) wa?.mysp?.setListener(object :SpringView.OnFreshListener{
            override fun onLoadmore() {
                Thread { this@MPATHandler.sendEmptyMessage(2) }.start()
            }
            override fun onRefresh() {
                Thread { this@MPATHandler.sendEmptyMessage(4) }.start()
            }
        })
    }

    private fun addPageHandler() {
        //wa?.myp?.visibility = View.VISIBLE
        Thread { this.sendEmptyMessage(3) }.start()
    }
}