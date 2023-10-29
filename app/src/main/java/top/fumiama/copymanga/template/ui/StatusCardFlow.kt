package top.fumiama.copymanga.template.ui

import android.animation.ObjectAnimator
import android.view.View
import kotlinx.android.synthetic.main.anchor_popular.view.*
import kotlinx.android.synthetic.main.line_finish.*
import top.fumiama.copymanga.MainActivity.Companion.mainWeakReference
import top.fumiama.copymanga.tools.api.CMApi
import top.fumiama.dmzj.copymanga.R

@ExperimentalStdlibApi
open class StatusCardFlow(private val api: Int, nav: Int) : InfoCardLoader(R.layout.fragment_statuscardflow, nav) {
    val sortWay = listOf("datetime_updated", "-datetime_updated", "popular", "-popular")
    var sortValue = 0

    override fun getApiUrl() =
        getString(api).format(
            CMApi.myHostApiUrl,
            page * 21,
            sortWay[sortValue]
        )

    override fun setListeners() {
        super.setListeners()
        setUpdate(line_finish_time)
        setHot(line_finish_pop)
    }

    open fun setUpdate(that: View) {
        that.apply {
            apt.setText(R.string.menu_update_time)
            setOnClickListener {
                sortValue = if(apim.rotation == 0f) {
                    ObjectAnimator.ofFloat(apim, "rotation", 0f, 180f).setDuration(233).start()
                    1
                }else{
                    ObjectAnimator.ofFloat(apim, "rotation", 180f, 0f).setDuration(233).start()
                    0
                }
                Thread{
                    Thread.sleep(400)
                    mainWeakReference?.get()?.runOnUiThread {
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
                sortValue = if (apim.rotation == 0f) {
                    ObjectAnimator.ofFloat(apim, "rotation", 0f, 180f).setDuration(233).start()
                    1
                } else {
                    ObjectAnimator.ofFloat(apim, "rotation", 180f, 0f).setDuration(233).start()
                    0
                }
                Thread {
                    Thread.sleep(400)
                    mainWeakReference?.get()?.runOnUiThread {
                        reset()
                        addPage()
                    }
                }.start()
            }
        }
    }
}