package top.fumiama.copymanga.template.ui

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import kotlinx.android.synthetic.main.card_book.view.*
import kotlinx.android.synthetic.main.line_horizonal_empty.view.*
import kotlinx.android.synthetic.main.line_lazybooklines.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.fumiama.copymanga.tools.api.CMApi
import top.fumiama.copymanga.tools.ui.GlideHideLottieViewListener
import top.fumiama.dmzj.copymanga.R
import java.io.File
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicInteger

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
    private var cardLoadingWaits = AtomicInteger()
    var initClickListeners: InitClickListeners? = null
    var exitCardList = false

    fun reset() {
        rows = arrayOfNulls(20)
        index = 0
        count = 0
        cardLoadingWaits.set(0)
        exitCardList = false
    }

    private suspend fun manageRow() {
        if(!exitCardList && count++ % cardPerRow == 0) inflateRow()
        Log.d("MyCL", "index: $index, cardPR: $cardPerRow")
    }

    private suspend fun inflateRow() = withContext(Dispatchers.IO) {
        that?.apply {
            layoutInflater.inflate(R.layout.line_horizonal_empty, mydll, false)?.let {
                if(exitCardList) return@withContext
                it.layoutParams.height = cardHeight + 16
                withContext(Dispatchers.Main) withMainContext@ {
                    if(exitCardList) return@withMainContext
                    mydll?.addView(it)
                }
                recycleOneRow(it)
                index++
            }
        }
    }
    private suspend fun recycleOneRow(v:View?) = withContext(Dispatchers.IO) {
        val relativeIndex = index % 20
        if(rows[relativeIndex] == null) rows[relativeIndex] = v
        else {
            val victim = rows[relativeIndex]
            that?.apply {
                withContext(Dispatchers.Main) withMainContext@ {
                    if(exitCardList) return@withMainContext
                    mydll?.removeView(victim)
                    mys?.scrollY = mys?.scrollY?.minus(cardHeight + 16)?:0
                }
            }
            rows[relativeIndex] = v
        }
    }

    @ExperimentalStdlibApi
    suspend fun addCard(name: String, append: String? = null, head: String? = null, path: String? = null, chapterUUID: String? = null, pn: Int? = null, isFinish: Boolean = false, isNew: Boolean = false) =
        withContext(Dispatchers.IO) {
        if (exitCardList) return@withContext
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
                addCard(it)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    @ExperimentalStdlibApi
    private suspend fun addCard(cardFrame: View) = withContext(Dispatchers.IO) withIO@ {
        val card = cardFrame.cic
        if (card.index < 0) return@withIO
        val name = card.name + (card.append?:"")
        val head = card.headImageUrl
        val file = File(that?.context?.getExternalFilesDir(""), card.name)
        if(exitCardList) return@withIO
        cardFrame.let {
            withContext(Dispatchers.Main) { it.tic.text = name }
            if(!file.exists()) {
                if(head != null) {
                    that?.context?.let { context ->
                        val waitMillis = cardLoadingWaits.getAndIncrement().toLong()*200
                        val g = Glide.with(context).load(
                            GlideUrl(CMApi.proxy?.wrap(head)?:head, CMApi.myGlideHeaders)
                        ).addListener(GlideHideLottieViewListener(WeakReference(it.laic)) {
                            if (exitCardList) return@GlideHideLottieViewListener
                            cardLoadingWaits.decrementAndGet()
                        })
                        withContext(Dispatchers.Main) {
                            if (waitMillis > 0) it.imic.postDelayed({
                                if (exitCardList) return@postDelayed
                                g.into(it.imic)
                            }, waitMillis) else g.into(it.imic)
                        }
                    }
                } else withContext(Dispatchers.Main) {
                    it.laic.pauseAnimation()
                    it.laic.visibility = View.GONE
                    it.imic.setImageResource(R.drawable.img_defmask)
                }
            } else {
                val img = File(file, "head.jpg")
                withContext(Dispatchers.Main) {
                    it.laic.pauseAnimation()
                    it.laic.visibility = View.GONE
                }
                if(img.exists()) {
                    withContext(Dispatchers.Main) {
                        it.imic.setImageURI(Uri.fromFile(img))
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        it.imic.setImageResource(R.drawable.img_defmask)
                    }
                }
            }
            withContext(Dispatchers.Main) {
                if(card.isFinish) it.sgnic.visibility = View.VISIBLE
                if(card.isNew) it.sgnnew.visibility = View.VISIBLE
                initClickListeners?.prepareListeners(card, card.name, card.path, card.chapterUUID, card.pageNumber)
                rows[card.index % 20]?.ltbtn?.addView(it)
                it.layoutParams?.height = cardHeight
                it.layoutParams?.width  = cardWidth
            }
        }
    }
    interface InitClickListeners {
        fun prepareListeners(v: View, name: String, path: String?, chapterUUID: String?, pn: Int?)
    }
}
