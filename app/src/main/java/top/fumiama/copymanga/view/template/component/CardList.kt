package top.fumiama.copymanga.view.template.component

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import kotlinx.android.synthetic.main.card_book.view.*
import kotlinx.android.synthetic.main.line_horizonal_empty.view.*
import kotlinx.android.synthetic.main.line_lazybooklines.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.fumiama.copymanga.api.Config
import top.fumiama.copymanga.view.operation.GlideHideLottieViewListener
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
    private var mRows: Array<View?> = arrayOfNulls(20)
    private var mIndex = 0
    private var mCount = 0
    private var cardLoadingWaits = AtomicInteger()
    var initClickListeners: InitClickListeners? = null
    var exitCardList = false

    fun reset() {
        mRows = arrayOfNulls(20)
        mIndex = 0
        mCount = 0
        cardLoadingWaits.set(0)
        exitCardList = false
    }

    private suspend fun manageRow(whenFinish: suspend (index: Int) -> Unit) = withContext(Dispatchers.IO) {
        if (exitCardList) return@withContext
        if(mCount++ % cardPerRow == 0) {
            inflateRow(++mIndex-1, whenFinish)
        } else whenFinish(mIndex-1)
    }

    private suspend fun inflateRow(index: Int, whenFinish: suspend (index: Int)->Unit) = withContext(Dispatchers.IO) {
        Log.d("MyCL", "inflateRow: $index, cardPR: $cardPerRow")
        that?.apply {
            mydll?.let { m ->
                layoutInflater.inflate(R.layout.line_horizonal_empty, m, false)?.let {
                    if(exitCardList) return@withContext
                    it.layoutParams.height = cardHeight + 16
                    m.apply { post { addView(it) } }
                    recycleOneRow(it, index)
                    whenFinish(index)
                }
            }
        }
    }
    private suspend fun recycleOneRow(v:View?, i: Int) = withContext(Dispatchers.IO) {
        val relativeIndex = i % 20
        if(mRows[relativeIndex] == null) mRows[relativeIndex] = v
        else {
            val victim = mRows[relativeIndex]
            that?.apply {
                if(exitCardList) return@withContext
                mydll?.apply { post { removeView(victim) } }
                mys?.apply { post { scrollY -= cardHeight + 16 } }
            }
            mRows[relativeIndex] = v
        }
    }

    private fun postPauseLottie(v: LottieAnimationView) {
        v.apply {
            post {
                pauseAnimation()
                visibility = View.GONE
            }
        }
    }

    @ExperimentalStdlibApi
    suspend fun addCard(
        name: String, append: String? = null, head: String? = null,
        path: String? = null, chapterUUID: String? = null, pn: Int? = null,
        isFinish: Boolean = false, isNew: Boolean = false
    ) {
        if (exitCardList) return
        manageRow { i ->
            withContext(Dispatchers.IO) {
                that?.apply {
                    layoutInflater.inflate(R.layout.card_book, mydll?.ltbtn, false)?.let {
                        val card = it.cic
                        card.name = name
                        card.append = append
                        card.headImageUrl = head
                        card.path = path
                        card.index = i
                        card.chapterUUID = chapterUUID
                        card.pageNumber = pn
                        card.isFinish = isFinish
                        card.isNew = isNew
                        addCard(it)
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    @ExperimentalStdlibApi
    private suspend fun addCard(cardFrame: View) = withContext(Dispatchers.IO) {
        val card = cardFrame.cic
        if (card.index < 0) throw Exception("minus card index")
        Log.d("MyCL", "addCard: into index ${card.index}")
        val appendedName = card.name + (card.append?:"")
        val head = card.headImageUrl
        val file = File(that?.context?.getExternalFilesDir(""), card.name)
        if(exitCardList) return@withContext
        cardFrame.let {
            it.tic.apply { post { text = appendedName } }
            if(!file.exists()) {
                if(head != null) {
                    that?.context?.let { context ->
                        val waitMillis = cardLoadingWaits.getAndIncrement().toLong()*200
                        val g = Glide.with(context).load(
                            GlideUrl(Config.imageProxy?.wrap(head)?:head, Config.myGlideHeaders)
                        ).addListener(GlideHideLottieViewListener(WeakReference(it.laic)) {
                            if (exitCardList) return@GlideHideLottieViewListener
                            cardLoadingWaits.decrementAndGet()
                        }).timeout(60000)
                        if (waitMillis > 0) it.imic.postDelayed({
                            if (exitCardList) return@postDelayed
                            g.into(it.imic)
                        }, waitMillis) else it.imic.post { g.into(it.imic) }
                    }
                } else {
                    postPauseLottie(it.laic)
                    it.imic.apply { post { setImageResource(R.drawable.img_defmask) } }
                }
            } else {
                val img = File(file, "head.jpg")
                postPauseLottie(it.laic)
                if(img.exists()) {
                    it.imic.apply {
                        post {
                            setImageURI(Uri.fromFile(img))
                        }
                    }
                } else it.imic.apply { post { setImageResource(R.drawable.img_defmask) } }
            }
            card.apply {
                if(isFinish) it.sgnic.visibility = View.VISIBLE
                if(isNew) it.sgnnew.visibility = View.VISIBLE
                initClickListeners?.prepareListeners(this, name, path, chapterUUID, pageNumber)
                mRows[index % 20]?.ltbtn?.apply {
                    withContext(Dispatchers.Main) {
                        if(!exitCardList) {
                            addView(it)
                            it.layoutParams?.height = cardHeight
                            it.layoutParams?.width  = cardWidth
                        }
                    }
                }
            }
        }
    }
    interface InitClickListeners {
        fun prepareListeners(v: View, name: String, path: String?, chapterUUID: String?, pn: Int?)
    }
}
