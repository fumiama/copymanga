package top.fumiama.copymanga.tool

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.widget.Toast
import top.fumiama.copymanga.R
import java.lang.ref.WeakReference
import java.util.*
import java.util.regex.Pattern
import kotlin.math.sqrt

class ToolsBox(w: WeakReference<Any>) {
    val zis = (w as WeakReference<Activity>).get()
    val week: String
        get() {
            val cal = Calendar.getInstance()
            return when (cal[Calendar.DAY_OF_WEEK]) {
                1 -> "周日"
                2 -> "周一"
                3 -> "周二"
                4 -> "周三"
                5 -> "周四"
                6 -> "周五"
                7 -> "周六"
                else -> ""
            }
        }
    val netinfo: String
        get() {
            val cm: ConnectivityManager =
                zis?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            return cm.getNetworkCapabilities(cm.activeNetwork)?.let {
                when {
                    it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> return@let "WIFI"
                    it.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> return@let "移动数据"
                    it.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> return@let "蓝牙"
                    it.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> return@let "以太网"
                    it.hasTransport(NetworkCapabilities.TRANSPORT_LOWPAN) -> return@let "LOWPAN"
                    it.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> return@let "VPN"
                    else -> return@let "无网络"
                }
            } ?: "错误"
        }
    fun toastError(s: String, willFinish: Boolean = true) {
        Toast.makeText(zis?.applicationContext, s, Toast.LENGTH_SHORT).show()
        if (willFinish) zis?.finish()
    }
    fun buildInfo(
        title: String,
        msg: String,
        txtOk: String? = null,
        txtN: String? = null,
        txtCancel: String? = null,
        ok: (() -> Unit)? = null,
        neutral: (() -> Unit)? = null,
        cancel: (() -> Unit)? = null
    ) {
        val info = AlertDialog.Builder(zis)
        info.setIcon(R.drawable.ic_launcher_foreground)
        info.setTitle(title)
        info.setMessage(msg)
        txtOk?.let { info.setPositiveButton(it) { _, _ -> ok?.let { it() } } }
        txtCancel?.let { info.setNegativeButton(it) { _, _ -> cancel?.let { it() } } }
        txtN?.let { info.setNeutralButton(it) { _, _ -> neutral?.let { it() } } }
        info.show()
    }
    fun dp2px(dp:Int):Int?{
        return zis?.resources?.displayMetrics?.density?.let { (dp * it + 0.5).toInt()}
    }
    private fun px2dp(px:Int):Int?{
        return zis?.resources?.displayMetrics?.density?.let { (px.toDouble() / it + 0.5).toInt()}
    }
    private fun root(a:Double, b:Double, c:Double):List<Double>?{
        val d = b*b - 4.0 * a * c
        if(d < 0) return null
        val sd = sqrt(d)
        val x1 = (-b + sd)/(2.0 * a)
        val x2 = (-b - sd)/(2.0 * a)
        return listOf(x1, x2)
    }
    fun calcWidthFromDp(marginLeftDp:Int, widthDp:Int):List<Int>{
        val margin = marginLeftDp.toDouble()
        val marginPx = dp2px(marginLeftDp)?:16
        val root = root(margin, widthDp.toDouble(), -((px2dp(zis?.resources?.displayMetrics?.widthPixels?:1080))?:400).toDouble())
        val numPerRow = root?.let { (it[0]+0.5).toInt()}?:3
        val w = ((zis?.resources?.displayMetrics?.widthPixels?:1080)-marginPx*(numPerRow+1))/numPerRow
        val totalWidth = ((zis?.resources?.displayMetrics?.widthPixels?:1080)-marginPx)/numPerRow
        return listOf(numPerRow, w, totalWidth)
    }
}
