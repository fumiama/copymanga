package top.fumiama.copymanga.ui.chapter

import android.os.Bundle
import android.view.View
import top.fumiama.dmzj.copymanga.R
import top.fumiama.copymanga.template.general.NoBackRefreshFragment
import java.lang.Thread.sleep
import java.lang.ref.WeakReference

class ChapterFragment: NoBackRefreshFragment(R.layout.fragment_chapters) {
    var handler: ChapterHandler? = null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if(isFirstInflate){
            handler = arguments?.let { ChapterHandler(WeakReference(this), it.getString("path")?:"", it.getString("group")?:"") }
            Thread{
                sleep(600)
                handler?.startLoad()
            }.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler?.destroy()
    }
}