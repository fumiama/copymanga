package top.fumiama.copymanga.ui.cardflow.shelf

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.anchor_popular.view.*
import kotlinx.android.synthetic.main.line_shelf.*
import top.fumiama.copymanga.MainActivity
import top.fumiama.copymanga.view.template.InfoCardLoader
import top.fumiama.copymanga.api.Config
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
            Config.myHostApiUrl.random(),
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
        line_shelf_updated.apply { post {
            if (ad?.exit == true) return@post
            apt.setText(R.string.menu_update_time)
            setOnClickListener {
                val same = sortValue in 0..1
                sortValue = rotate(it.apim, same, 0)
                if (!same) fade()
                delayedRefresh(400)
            }
        } }
    }

    private fun setModify() {
        line_shelf_modifier.apply { post {
            if (ad?.exit == true) return@post
            apt.setText(R.string.menu_add_time)
            setOnClickListener {
                val same = sortValue in 2..3
                sortValue = rotate(it.apim, same, 2)
                if (!same) fade()
                delayedRefresh(400)
            }
        } }
    }

    private fun setBrowse() {
        line_shelf_browse.apply { post {
            if (ad?.exit == true) return@post
            apt.setText(R.string.menu_read_time)
            setOnClickListener {
                val same = sortValue>=4
                sortValue = rotate(it.apim, same, 4)
                if (!same) fade()
                delayedRefresh(400)
            }
        } }
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
        val alphae = when(sortValue) {
            0, 1 -> listOf(1f, 0.5f, 0.5f)
            2, 3 -> listOf(0.5f, 1f, 0.5f)
            4, 5 -> listOf(0.5f, 0.5f, 1f)
            else -> listOf(1f, 1f, 1f)
        }
        line_shelf_updated.apply { post { alpha = alphae[0] } }
        line_shelf_modifier.apply { post { alpha = alphae[1] } }
        line_shelf_browse.apply { post { alpha = alphae[2] } }
    }
}