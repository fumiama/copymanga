package top.fumiama.copymanga.ui.cardflow.finish

import android.animation.ObjectAnimator
import android.view.View
import kotlinx.android.synthetic.main.anchor_popular.view.*
import kotlinx.android.synthetic.main.line_finish.*
import kotlinx.android.synthetic.main.line_lazybooklines.*
import top.fumiama.copymanga.MainActivity.Companion.mainWeakReference
import top.fumiama.copymanga.template.ui.InfoCardLoader
import top.fumiama.dmzj.copymanga.R
import java.lang.Thread.sleep

@ExperimentalStdlibApi
class FinishFragment : InfoCardLoader(R.layout.fragment_finish, R.id.action_nav_finish_to_nav_book) {
    private val sortWay = listOf("datetime_updated", "-datetime_updated", "popular", "-popular")
    private var sortValue = 0

    override fun getApiUrl() =
        getString(R.string.finishApiUrl).let {
            String.format(
                it,
                page * 21,
                sortWay[sortValue]
            )
        }

    override fun setListeners() {
        super.setListeners()
        setUpdate()
        setHot()
    }

    override fun onLoadFinish() {
        super.onLoadFinish()
        mainWeakReference?.get()?.runOnUiThread {
            mypl.visibility = View.GONE
        }
    }

    private fun setUpdate() {
        line_finish_time.apply {
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
                    sleep(400)
                    mh?.sendEmptyMessage(4)
                }.start()
            }
        }
    }

    private fun setHot() {
        line_finish_pop.apply {
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
                    sleep(400)
                    mh?.sendEmptyMessage(4)
                }.start()
            }
        }
    }
}