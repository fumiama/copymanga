package top.fumiama.copymanga.ui.book

import android.os.Bundle
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.Navigation
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.card_book.*
import kotlinx.android.synthetic.main.fragment_book.*
import kotlinx.android.synthetic.main.line_2chapters.view.*
import kotlinx.android.synthetic.main.line_bookinfo.*
import kotlinx.android.synthetic.main.line_bookinfo_text.*
import kotlinx.android.synthetic.main.line_caption.view.*
import kotlinx.android.synthetic.main.line_chapter.view.*
import top.fumiama.dmzj.copymanga.R
import top.fumiama.copymanga.MainActivity.Companion.mainWeakReference
import top.fumiama.copymanga.json.BookInfoStructure
import top.fumiama.copymanga.json.ThemeStructure
import top.fumiama.copymanga.template.http.AutoDownloadHandler
import top.fumiama.copymanga.tools.api.CMApi
import top.fumiama.copymanga.tools.api.GlideBlurTransformation
import java.lang.ref.WeakReference

class BookHandler(that: WeakReference<BookFragment>, path: String)
    : AutoDownloadHandler(
    that.get()?.getString(R.string.bookInfoApiUrl)?.let { String.format(it, path) } ?: "",
    BookInfoStructure::class.java,
    Looper.myLooper()!!){
    private val that = that.get()
    private var hasToastedError = false
    get(){
        val re = field
        field = true
        return re
    }
    var book: BookInfoStructure? = null
    var fbibinfo:View? = null
    var complete = false
    private val divider get() = that?.layoutInflater?.inflate(R.layout.div_h, that.fbl, false)
    private var fbtinfo: View? = null

    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        when(msg.what){
            //0 -> setLayouts()
            1 -> setCover()
            2 -> setTexts()
            3 -> fbibinfo?.let { setInfoHeight(it) }
            4 -> setThemes()
            5 -> setOverScale()
            6 -> endSetLayouts()
        }
    }

    override fun onError() {
        super.onError()
        if(exit) return
        if(!hasToastedError) {
            Toast.makeText(that?.context, R.string.null_book, Toast.LENGTH_SHORT).show()
            that?.rootView?.let { it1 ->
                Navigation.findNavController(it1).navigateUp()
            }
        }
    }

    override fun setGsonItem(gsonObj: Any) {
        super.setGsonItem(gsonObj)
        book = gsonObj as BookInfoStructure
    }

    override fun getGsonItem() = book
    override fun doWhenFinishDownload() {
        super.doWhenFinishDownload()
        if(exit) return
        inflateComponents()
        Thread{ for (i in 1..6) sendEmptyMessage(i) }.start()
    }

    private fun endSetLayouts(){
        that?.fbloading?.visibility = View.GONE
        complete = true
        Log.d("MyBH", "Set complete: true")
    }

    private fun inflateComponents(){
        fbibinfo = that?.layoutInflater?.inflate(R.layout.line_bookinfo, that.fbl, false)
        fbtinfo = that?.layoutInflater?.inflate(R.layout.line_text_info, that.fbl, false)
    }

    private fun setOverScale(){
        that?.fbov?.setScaleView(that.lbibg)
    }

    private fun setCover(){
        that?.apply {
            fbl.addView(fbibinfo)
            val load = Glide.with(this).load(
                GlideUrl(book?.results?.comic?.cover, CMApi.myGlideHeaders)
            ).timeout(10000)
            load.into(imic)
            context?.let { it1 -> GlideBlurTransformation(it1) }
                ?.let { it2 -> RequestOptions.bitmapTransform(it2) }
                ?.let { it3 -> load.apply(it3).into(lbibg) }
            imf.visibility = View.GONE
            fbl.addView(divider)
        }
    }

    private fun getThemeSeq(authors: Array<ThemeStructure>): CharSequence{
        var re = ""
        for(author in authors) re += author.name + ' '
        return re
    }

    private fun setTexts(){
        //that?.tic?.text = book?.name
        that?.tic?.visibility = View.GONE
        mainWeakReference?.get()?.toolbar?.title = book?.results?.comic?.name
        that?.btauth?.text = book?.results?.comic?.author?.let { getThemeSeq(it) }
        that?.bttag?.text = book?.results?.comic?.theme?.let { getThemeSeq(it) }
        that?.bthit?.text = that?.getString(R.string.text_format_hit)?.let { String.format(
            it,
            book?.results?.comic?.popular
        ) }?:""
        that?.btsub?.text = that?.getString(R.string.text_format_stat)?.let { String.format(
            it,
            book?.results?.comic?.status?.display
        ) }?:""
        that?.bttime?.text = book?.results?.comic?.datetime_updated
        (fbtinfo as TextView).text = book?.results?.comic?.brief
        that?.fbl?.addView(fbtinfo)
        that?.fbl?.addView(divider)
    }

    private fun setInfoHeight(v: View){
        v.viewTreeObserver.addOnGlobalLayoutListener {
            Log.d("MyMy", "Width: ${v.width}")
            val newH = (v.width * 4.0 / 9.0 + 0.5).toInt()
            v.layoutParams.height = newH
            v.invalidate()
        }
    }

    private fun setTheme(caption: String, themeStructure: Array<ThemeStructure>, nav: Int) {
        that?.apply {
            val t = layoutInflater.inflate(R.layout.line_caption, fbl, false)
            t.tcptn.text = caption
            fbl.addView(t)
            fbl.addView(layoutInflater.inflate(R.layout.div_h, fbl, false))
        }
        var line: View? = null
        val last = themeStructure.size - 1
        themeStructure.onEachIndexed { i, it ->
            if(line == null) {
                if(i == last) {
                    line = that?.layoutInflater?.inflate(R.layout.line_chapter, that.fbl, false)
                    line?.lcc?.apply {
                        lct.text = it.name
                        setOnClickListener { _ ->
                            loadVolume(it.name, it.path_word, nav)
                        }
                    }
                    that?.fbl?.addView(line)
                } else {
                    line = that?.layoutInflater?.inflate(R.layout.line_2chapters, that.fbl, false)
                    line?.l2cl?.apply {
                        lct.text = it.name
                        setOnClickListener { _ ->
                            loadVolume(it.name, it.path_word, nav)
                        }
                    }
                }
            } else line?.l2cr?.apply {
                lct.text = it.name
                setOnClickListener { _ ->
                    loadVolume(it.name, it.path_word, nav)
                }
                that?.fbl?.addView(line)
                line = null
            }
        }
    }

    private fun setThemes(){
        that?.apply {
            book?.results?.comic?.apply {
                author?.let { setTheme(getString(R.string.author), it, R.id.action_nav_book_to_nav_author) }
                theme?.let { setTheme(getString(R.string.caption), it, R.id.action_nav_book_to_nav_caption) }
            }
        }
    }

    private fun loadVolume(name: String, path: String, nav: Int){
        Log.d("MyBH", "start to load chapter")
        val bundle = Bundle()
        bundle.putString("name", name)
        bundle.putString("path", path)
        that?.rootView?.let { Navigation.findNavController(it).navigate(nav, bundle) }
    }
}