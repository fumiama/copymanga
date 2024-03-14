package top.fumiama.copymanga.ui.cardflow.shelf

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.anchor_popular.view.*
import kotlinx.android.synthetic.main.line_shelf.*
import top.fumiama.copymanga.MainActivity
import top.fumiama.copymanga.template.ui.InfoCardLoader
import top.fumiama.copymanga.tools.api.CMApi
import top.fumiama.dmzj.copymanga.R

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

    override fun onCreate(savedInstanceState: Bundle?) {
        if (MainActivity.member?.hasLogin != true) {
            Toast.makeText(context, R.string.noLogin, Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
        super.onCreate(savedInstanceState)
    }

    override fun setListeners() {
        super.setListeners()
        fade()
        setUpdate()
        setModify()
        setBrowse()
    }

    private fun setUpdate() {
        if (ad?.exit == true) return
        line_shelf_updated.apt.setText(R.string.menu_update_time)
        line_shelf_updated.setOnClickListener {
            val same = sortValue in 0..1
            sortValue = rotate(it.apim, same, 0)
            if (!same) fade()
            delayedRefresh(400)
        }
    }

    private fun setModify() {
        if (ad?.exit == true) return
        line_shelf_modifier.apt.setText(R.string.menu_add_time)
        line_shelf_modifier.setOnClickListener {
            val same = sortValue in 2..3
            sortValue = rotate(it.apim, same, 2)
            if (!same) fade()
            delayedRefresh(400)
        }
    }

    private fun setBrowse() {
        if (ad?.exit == true) return
        line_shelf_browse.apt.setText(R.string.menu_read_time)
        line_shelf_browse.setOnClickListener {
            val same = sortValue>=4
            sortValue = rotate(it.apim, same, 4)
            if (!same) fade()
            delayedRefresh(400)
        }
    }

    private fun rotate(img: View, isSameSlot: Boolean, offset: Int): Int {
        return if (isSameSlot) {
            if (img.rotation == 0f) {
                ObjectAnimator.ofFloat(img, "rotation", 0f, 180f).setDuration(233).start()
                offset+1
            } else {
                ObjectAnimator.ofFloat(img, "rotation", 180f, 0f).setDuration(233).start()
                offset
            }
        } else {
            if (img.rotation == 0f) {
                offset
            } else {
                offset+1
            }
        }
    }

    private fun fade() {
        when(sortValue) {
            0, 1 -> {
                line_shelf_updated.alpha = 1f
                line_shelf_modifier.alpha = 0.5f
                line_shelf_browse.alpha = 0.5f
            }
            2, 3 -> {
                line_shelf_updated.alpha = 0.5f
                line_shelf_modifier.alpha = 1f
                line_shelf_browse.alpha = 0.5f
            }
            4, 5 -> {
                line_shelf_updated.alpha = 0.5f
                line_shelf_modifier.alpha = 0.5f
                line_shelf_browse.alpha = 1f
            }
        }
    }
}