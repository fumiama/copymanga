package top.fumiama.copymanga.template.ui

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import kotlinx.android.synthetic.main.card_book.*
import kotlinx.android.synthetic.main.card_book.view.*
import kotlinx.android.synthetic.main.line_horizonal_empty.view.*
import kotlinx.android.synthetic.main.line_lazybooklines.*
import top.fumiama.copymanga.tools.api.CMApi
import top.fumiama.copymanga.tools.ui.GlideHideLottieViewListener
import top.fumiama.dmzj.copymanga.R
import java.io.File
import java.lang.ref.WeakReference

class CardList(
    private val fragment: WeakReference<Fragment>,
    private val cardWidth: Int,
    private val cardHeight: Int,
    private val cardPerRow: Int
) {
    private val that get() = fragment.get()
    private var rows:Array<View?> = arrayOfNulls(20)
    private var index = 0
    private var count = 0
    var initClickListeners: InitClickListeners? = null
    var exitCardList = false

    fun reset(){
        rows = arrayOfNulls(20)
        index = 0
        count = 0
        exitCardList = false
    }

    private fun manageRow() {
        if(!exitCardList && count++ % cardPerRow == 0) inflateRow()
        Log.d("MyCL", "index: $index, cardPR: $cardPerRow")
    }

    private fun inflateRow(){
        that?.apply {
            layoutInflater.inflate(R.layout.line_horizonal_empty, mydll, false)?.let {
                if(exitCardList) return
                it.layoutParams.height = cardHeight + 16
                activity?.runOnUiThread {
                    if(exitCardList) return@runOnUiThread
                    mydll?.addView(it)
                }
                recycleOneRow(it)
                index++
            }
        }
    }
    private fun recycleOneRow(v:View?){
        val relativeIndex = index % 20
        if(rows[relativeIndex] == null) rows[relativeIndex] = v
        else {
            val victim = rows[relativeIndex]
            that?.apply {
                activity?.runOnUiThread {
                    if(exitCardList) return@runOnUiThread
                    mydll?.removeView(victim)
                    mys?.scrollY = mys?.scrollY?.minus(cardHeight + 16)?:0
                }
            }
            rows[relativeIndex] = v
        }
    }

    @ExperimentalStdlibApi
    fun addCard(name: String, append: String? = null, head: String? = null, path: String? = null, chapterUUID: String? = null, pn: Int? = null, isFinish: Boolean = false, isNew: Boolean = false) {
        if (exitCardList) return
        manageRow()
        that?.apply {
            layoutInflater.inflate(R.layout.card_book, mydll?.ltbtn, false)?.let {
                val card = it.cic
                card.name = name
                card.append = append
                card.headImageUrl = head
                card.path = path
                card.index = index - 1
                card.chapterUUID = chapterUUID
                card.pageNumber = pn
                card.isFinish = isFinish
                card.isNew = isNew
                activity?.runOnUiThread {
                    if (exitCardList) return@runOnUiThread
                    addCard(it)
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    @ExperimentalStdlibApi
    fun addCard(cardFrame: View) {
        val card = cardFrame.cic
        if (card.index < 0) return
        val name = card.name + (card.append?:"")
        val head = card.headImageUrl
        val file = File(that?.context?.getExternalFilesDir(""), card.name)
        if(exitCardList) return
        cardFrame.let {
            it.tic.text = name
            if(!file.exists()){
                if(head != null) {
                    that?.context?.let { context ->
                        Glide.with(context).load(
                            GlideUrl(CMApi.proxy?.wrap(head)?:head, CMApi.myGlideHeaders)
                        ).addListener(GlideHideLottieViewListener(WeakReference(it.laic))).into(it.imic)
                    }
                } else {
                    it.laic.pauseAnimation()
                    it.laic.visibility = View.GONE
                    it.imic.setImageResource(R.drawable.img_defmask)
                }
            } else {
                val img = File(file, "head.jpg")
                it.laic.pauseAnimation()
                it.laic.visibility = View.GONE
                if(img.exists()) {
                    it.imic.setImageURI(Uri.fromFile(img))
                } else {
                    it.imic.setImageResource(R.drawable.img_defmask)
                }
            }
            if(card.isFinish) it.sgnic.visibility = View.VISIBLE
            if(card.isNew) it.sgnnew.visibility = View.VISIBLE
            initClickListeners?.prepareListeners(card, card.name, card.path, card.chapterUUID, card.pageNumber)
            rows[card.index % 20]?.ltbtn?.addView(it)
            it.layoutParams?.height = cardHeight
            it.layoutParams?.width  = cardWidth
        }
    }
    interface InitClickListeners{
        fun prepareListeners(v: View, name: String, path: String?, chapterUUID: String?, pn: Int?)
    }
}