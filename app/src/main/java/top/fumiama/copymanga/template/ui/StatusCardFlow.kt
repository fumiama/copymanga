package top.fumiama.copymanga.template.ui

import android.animation.ObjectAnimator
import android.view.View
import kotlinx.android.synthetic.main.anchor_popular.view.*
import top.fumiama.copymanga.tools.api.CMApi
import top.fumiama.dmzj.copymanga.R

@ExperimentalStdlibApi
open class StatusCardFlow(private val api: Int, nav: Int, inflateRes: Int,
                          isTypeBook: Boolean = false,
                          isHistoryBook: Boolean = false,
                          isShelfBook: Boolean = false) : InfoCardLoader(inflateRes, nav, isTypeBook, isHistoryBook, isShelfBook) {
    val sortWay = listOf("-datetime_updated", "datetime_updated", "-popular", "popular")
    var sortValue = 0
    var lineUpdate: View? = null
    var lineHot: View? = null

    override fun getApiUrl() =
        getString(api).format(
            CMApi.myHostApiUrl,
            page * 21,
            sortWay[sortValue]
        )

    override fun setListeners() {
        super.setListeners()
        lineUpdate?.let { setUpdate(it) }
        lineHot?.let { setHot(it) }
        lineUpdate?.alpha = 1f
        lineHot?.alpha = 0.5f
    }

    open fun setUpdate(that: View) {
        that.apply {
            apt.setText(R.string.menu_update_time)
            setOnClickListener {
                sortValue = triggerLine(false)
                Thread{
                    Thread.sleep(400)
                    activity?.runOnUiThread {
                        reset()
                        addPage()
                    }
                }.start()
            }
        }
    }

    open fun setHot(that: View) {
        that.apply {
            apt.setText(R.string.menu_hot)
            setOnClickListener {
                sortValue = triggerLine(true)
                Thread {
                    Thread.sleep(400)
                    activity?.runOnUiThread {
                        reset()
                        addPage()
                    }
                }.start()
            }
        }
    }

    open fun triggerLine(isHot: Boolean): Int {
        val hot = lineHot?:return 0
        val update = lineUpdate?:return 0
        if(sortValue >= 2) {
            if (isHot) {
                return if (hot.apim.rotation == 0f) {
                    ObjectAnimator.ofFloat(hot.apim, "rotation", 0f, 180f).setDuration(233).start()
                    3
                } else {
                    ObjectAnimator.ofFloat(hot.apim, "rotation", 180f, 0f).setDuration(233).start()
                    2
                }
            } else {
                update.alpha = 1f
                hot.alpha = 0.5f
                return if(update.apim.rotation == 0f) {
                    0
                } else {
                    1
                }
            }
        } else {
            if (!isHot) {
                return if(update.apim.rotation == 0f) {
                    ObjectAnimator.ofFloat(update.apim, "rotation", 0f, 180f).setDuration(233).start()
                    1
                }else{
                    ObjectAnimator.ofFloat(update.apim, "rotation", 180f, 0f).setDuration(233).start()
                    0
                }
            } else {
                hot.alpha = 1f
                update.alpha = 0.5f
                return  if (hot.apim.rotation == 0f) {
                    2
                } else {
                    3
                }
            }
        }
    }
}