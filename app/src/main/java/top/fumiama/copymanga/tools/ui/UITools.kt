package top.fumiama.copymanga.tools.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.view.View
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.fumiama.dmzj.copymanga.R
import java.lang.ref.WeakReference
import kotlin.math.sqrt

class UITools(that: Context?, w: WeakReference<Activity>? = null) {
    private val zis = that
    private val weak = w
    constructor(w: WeakReference<Activity>): this(w.get()?.applicationContext, w)
    val transportStringNull = zis?.getString(R.string.TRANSPORT_NULL) ?: "TRANSPORT_NULL"
    val transportStringError = zis?.getString(R.string.TRANSPORT_ERROR) ?: "TRANSPORT_ERROR"
    val netInfo: String
        get() {
            val cm: ConnectivityManager =
                zis?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            return cm.getNetworkCapabilities(cm.activeNetwork)?.let {
                when {
                    it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> return@let zis.getString(
                        R.string.TRANSPORT_WIFI)
                    it.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> return@let zis.getString(
                        R.string.TRANSPORT_CELLULAR)
                    it.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> return@let zis.getString(
                        R.string.TRANSPORT_BLUETOOTH)
                    it.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> return@let zis.getString(
                        R.string.TRANSPORT_ETHERNET)
                    it.hasTransport(NetworkCapabilities.TRANSPORT_LOWPAN) -> return@let zis.getString(
                        R.string.TRANSPORT_LOWPAN)
                    it.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> return@let "VPN"
                    else -> return@let transportStringNull
                }
            } ?: transportStringError
        }
    suspend fun toastError(s: String, willFinish: Boolean = true) = withContext(Dispatchers.Main) {
        Toast.makeText(zis, s, Toast.LENGTH_SHORT).show()
        if (willFinish) weak?.get()?.finish()
    }
    suspend fun toastError(s: Int, willFinish: Boolean = true) = withContext(Dispatchers.Main) {
        Toast.makeText(zis, s, Toast.LENGTH_SHORT).show()
        if (willFinish) weak?.get()?.finish()
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
    fun buildAlertWithView(
        title: String,
        view: View,
        txtOk: String? = null,
        txtN: String? = null,
        txtCancel: String? = null,
        ok: (() -> Unit)? = null,
        neutral: (() -> Unit)? = null,
        cancel: (() -> Unit)? = null
    ): AlertDialog {
        val info = AlertDialog.Builder(zis)
        info.setIcon(R.drawable.ic_launcher_foreground)
        info.setTitle(title)

        info.setView(view)
        txtOk?.let { info.setPositiveButton(it) { _, _ -> ok?.let { it() } } }
        txtCancel?.let { info.setNegativeButton(it) { _, _ -> cancel?.let { it() } } }
        txtN?.let { info.setNeutralButton(it) { _, _ -> neutral?.let { it() } } }
        return info.show()
    }
    fun dp2px(dp:Int):Int?{
        return zis?.resources?.displayMetrics?.density?.let { (dp * it + 0.5).toInt()}
    }
    private fun px2dp(px:Int):Int?{
        return zis?.resources?.displayMetrics?.density?.let { (px.toDouble() / it + 0.5).toInt()}
    }
    fun calcWidthFromDp(marginLeftDp:Int, widthDp:Int):List<Int>{
        val margin = marginLeftDp.toDouble()
        val marginPx = dp2px(marginLeftDp)?:16
        val screenWidth = zis?.resources?.displayMetrics?.widthPixels?:1080
        val numPerRow = ((px2dp(screenWidth)?:400).toDouble() / (widthDp + 2 * margin) + 0.5).toInt()
        val w = (screenWidth - marginPx*numPerRow*2)/numPerRow
        val totalWidth = screenWidth/numPerRow
        return listOf(numPerRow, w, totalWidth)
    }
    private fun root(a:Double, b:Double, c:Double):List<Double>?{
        val d = b*b - 4.0 * a * c
        if(d < 0) return null
        val sd = sqrt(d)
        val x1 = (-b + sd)/(2.0 * a)
        val x2 = (-b - sd)/(2.0 * a)
        return listOf(x1, x2)
    }
    fun calcWidthFromDpRoot(marginLeftDp:Int, widthDp:Int):List<Int>{
        val margin = marginLeftDp.toDouble()
        val marginPx = dp2px(marginLeftDp)?:16
        val root = root(margin, widthDp.toDouble(), -((px2dp(zis?.resources?.displayMetrics?.widthPixels?:1080))?:400).toDouble())
        val numPerRow = root?.let { (it[0]+0.5).toInt()}?:3
        val w = ((zis?.resources?.displayMetrics?.widthPixels?:1080)-marginPx*(numPerRow+1))/numPerRow
        val totalWidth = ((zis?.resources?.displayMetrics?.widthPixels?:1080)-marginPx)/numPerRow
        return listOf(numPerRow, w, totalWidth)
    }
    companion object {
        fun toHexStr(byteArray: ByteArray) =
            with(StringBuilder()) {
                byteArray.forEach {
                    val hex = it.toInt() and (0xFF)
                    val hexStr = Integer.toHexString(hex)
                    if (hexStr.length == 1) append("0").append(hexStr)
                    else append(hexStr)
                }
                toString()
            }
        @SuppressLint("DiscouragedApi", "InternalInsetResource")
        fun getNavigationBarHeight(context: Context): Int {
            val resources = context.resources
            val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
            return if (resourceId > 0) {
                resources.getDimensionPixelSize(resourceId)
            } else {
                64
            }
        }
        @SuppressLint("DiscouragedApi", "InternalInsetResource")
        fun getStatusBarHeight(context: Context): Int {
            val resources = context.resources
            val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
            return if (resourceId > 0) {
                resources.getDimensionPixelSize(resourceId)
            } else {
                64
            }
        }
    }
}