package top.fumiama.copymanga.ui.chapter

import android.os.Looper
import android.os.Message
import android.view.View
import android.widget.Toast
import androidx.navigation.Navigation
import kotlinx.android.synthetic.main.fragment_book.*
import kotlinx.android.synthetic.main.fragment_chapters.*
import kotlinx.android.synthetic.main.line_2chapters.view.*
import kotlinx.android.synthetic.main.line_chapter.view.*
import top.fumiama.dmzj.copymanga.R
import top.fumiama.copymanga.json.ChapterStructure
import top.fumiama.copymanga.json.VolumeStructure
import top.fumiama.copymanga.template.AutoDownloadHandler
import java.lang.ref.WeakReference

class ChapterHandler(that: WeakReference<ChapterFragment>, pw: String, gpw: String):AutoDownloadHandler(
    that.get()?.getString(R.string.groupInfoApiUrl)?.let { String.format(it, pw, gpw) } ?: "",
    VolumeStructure::class.java,
    Looper.myLooper()!!
) {
    private val that = that.get()
    var hasToastedError = false
        get(){
            val re = field
            field = true
            return re
        }
    private var chapters: VolumeStructure? = null

    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        when(msg.what){
            //0 -> setLayouts()
            1 -> inflateChapters()
        }

    }

    override fun getGsonItem() = chapters
    override fun setGsonItem(gsonObj: Any) {
        super.setGsonItem(gsonObj)
        chapters = gsonObj as VolumeStructure
    }

    override fun onError() {
        super.onError()
        if(!hasToastedError) {
            Toast.makeText(that?.context, R.string.null_book, Toast.LENGTH_SHORT).show()
            that?.rootView?.let { it1 ->
                Navigation.findNavController(it1).navigateUp()
            }
        }
    }
    override fun doWhenFinishDownload() {
        super.doWhenFinishDownload()
        Thread{ sendEmptyMessage(1) }.start()
    }
    private fun inflateChapters(){

        that?.fcloading?.visibility = View.GONE
    }
    private fun addLine(size: Int, name:String, onClick:(()->Unit)? = null){
        val line =
            that?.let { it.layoutInflater.inflate(R.layout.line_chapter, it.fbl, false) }
        line?.lct?.text = name
        onClick?.let {action->
            line?.lcc?.setOnClickListener {action()}
        }
        that?.fcl?.addView(line)
    }
    private fun loadChapter(chapter: ChapterStructure){
        /*val bundle = Bundle()

        bundle.putInt("id", id)
        bundle.putInt("volume", volId)
        bundle.putInt("chapter", cid)
        that?.rootView?.let { Navigation.findNavController(it).navigate(R.id.action_nav_chapter_to_nav_reader, bundle) }
    */}
}