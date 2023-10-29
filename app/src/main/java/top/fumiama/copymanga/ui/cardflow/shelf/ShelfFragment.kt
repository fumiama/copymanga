package top.fumiama.copymanga.ui.cardflow.shelf

import android.animation.ObjectAnimator
import kotlinx.android.synthetic.main.anchor_popular.view.*
import kotlinx.android.synthetic.main.line_shelf.*
import top.fumiama.copymanga.MainActivity
import top.fumiama.copymanga.template.ui.InfoCardLoader
import top.fumiama.copymanga.tools.api.CMApi
import top.fumiama.dmzj.copymanga.R
import java.lang.Thread.sleep

@ExperimentalStdlibApi
class ShelfFragment : InfoCardLoader(R.layout.fragment_shelf, R.id.action_nav_sub_to_nav_book, isShelfBook = true) {
    private val sortWay = listOf(
        "-datetime_updated",
        "datetime_updated",
        "-datetime_modifier",
        "datetime_modifier",
        "-datetime_browse",
        "datetime_browse"
    )
    private var sortValue = 0

    override fun getApiUrl() =
        getString(R.string.shelfApiUrl).format(
            CMApi.myHostApiUrl,
            page * 21,
            sortWay[sortValue]
        )

    override fun setListeners() {
        super.setListeners()
        setUpdate()
        setModify()
        setBrowse()
    }

    private fun setUpdate() {
        if (ad?.exit == true) return
        line_shelf_updated.apt.setText(R.string.menu_update_time)
        line_shelf_updated.setOnClickListener {
            sortValue = if (it.apim.rotation == 0f) {
                ObjectAnimator.ofFloat(it.apim, "rotation", 0f, 180f).setDuration(233).start()
                1
            } else {
                ObjectAnimator.ofFloat(it.apim, "rotation", 180f, 0f).setDuration(233).start()
                0
            }
            Thread {
                sleep(400)
                MainActivity.mainWeakReference?.get()?.runOnUiThread {
                    reset()
                    addPage()
                }
            }.start()
        }
    }

    private fun setModify() {
        if (ad?.exit == true) return
        line_shelf_modifier.apt.setText(R.string.menu_add_time)
        line_shelf_modifier.setOnClickListener {
            sortValue = if (it.apim.rotation == 0f) {
                ObjectAnimator.ofFloat(it.apim, "rotation", 0f, 180f).setDuration(233).start()
                3
            } else {
                ObjectAnimator.ofFloat(it.apim, "rotation", 180f, 0f).setDuration(233).start()
                2
            }
            Thread {
                sleep(400)
                MainActivity.mainWeakReference?.get()?.runOnUiThread {
                    reset()
                    addPage()
                }
            }.start()
        }
    }

    private fun setBrowse() {
        if (ad?.exit == true) return
        line_shelf_browse.apt.setText(R.string.menu_read_time)
        line_shelf_browse.setOnClickListener {
            sortValue = if (it.apim.rotation == 0f) {
                ObjectAnimator.ofFloat(it.apim, "rotation", 0f, 180f).setDuration(233).start()
                5
            } else {
                ObjectAnimator.ofFloat(it.apim, "rotation", 180f, 0f).setDuration(233).start()
                4
            }
            Thread {
                sleep(400)
                MainActivity.mainWeakReference?.get()?.runOnUiThread {
                    reset()
                    addPage()
                }
            }.start()
        }
    }
}