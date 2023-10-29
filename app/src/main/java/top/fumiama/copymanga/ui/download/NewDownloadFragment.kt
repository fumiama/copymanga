package top.fumiama.copymanga.ui.download

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.line_lazybooklines.*
import top.fumiama.copymanga.MainActivity
import top.fumiama.copymanga.manga.Reader
import top.fumiama.copymanga.template.general.MangaPagesFragmentTemplate
import top.fumiama.copymanga.template.ui.CardList
import top.fumiama.copymanga.tools.ui.Navigate
import top.fumiama.copymanga.tools.ui.UITools
import top.fumiama.copymanga.tools.file.FileUtils
import top.fumiama.copymanga.ui.comicdl.ComicDlFragment
import top.fumiama.dmzj.copymanga.R
import java.io.File
import java.lang.Thread.sleep
import java.lang.ref.WeakReference

@OptIn(ExperimentalStdlibApi::class)
class NewDownloadFragment: MangaPagesFragmentTemplate(R.layout.fragment_newdownload, forceLoad = true) {
    private var sortedBookList: List<File>? = null
    private val oldDlCardName = MainActivity.mainWeakReference?.get()?.getString(R.string.old_download_card_name)!!
    private val extDir = MainActivity.mainWeakReference?.get()?.getExternalFilesDir("")
    private var isReverse = false
    private var isContentChanged = false
    private var exit = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wn = WeakReference(this)
    }

    override fun onPause() {
        super.onPause()
        exit = true
    }

    override fun onResume() {
        super.onResume()
        exit = false
    }

    override fun onDestroy() {
        super.onDestroy()
        wn = null
        exit = true
    }

    override fun addPage() {
        super.addPage()
        if(isRefresh){
            page = 0
            isRefresh = false
        }
        if(!isEnd) {
            if(sortedBookList == null || isContentChanged) {
                Log.d("MyNDF", "Sorting books...")
                sortedBookList = extDir?.listFiles()?.sortedBy {
                    return@sortedBy Reader.getComicPathWordInFile(it)
                }
                if (isReverse) {
                    Log.d("MyNDF", "reversed...")
                    sortedBookList = sortedBookList?.asReversed()
                }
                isContentChanged = false
            }
            Log.d("MyNDF", "Start drawing cards")
            cardList?.addCard(oldDlCardName, path = oldDlCardName)
            var cnt = 1
            sortedBookList?.let {
                for(i in it.listIterator(page)) {
                    if(cardList?.exitCardList != false) return
                    page++ // page is actually count
                    val chosenJson = File(i, "info.bin")
                    val newJson = File(i, "info.json")
                    val bookSize = (FileUtils.sizeOf(i)/1048576).toInt()
                    when {
                        chosenJson.exists() -> continue // unsupported old folder
                        newJson.exists() -> {
                            if(cardList?.exitCardList != false) return
                            cardList?.addCard(i.name, "\n${bookSize}MB")
                            cnt++
                        }
                    }
                    if (cnt >= 21) break
                }
                if(page >= it.size) {
                    isEnd = true
                }
            }
        }
        onLoadFinish()
    }

    override fun initCardList(weakReference: WeakReference<Fragment>) {
        super.initCardList(weakReference)
        cardList = CardList(weakReference, cardWidth, cardHeight, cardPerRow)
        cardList?.initClickListeners = object : CardList.InitClickListeners {
            override fun prepareListeners(v: View, name: String, path: String?, chapterUUID: String?, pn: Int?) {
                v.setOnClickListener {
                    if(name==oldDlCardName && path == oldDlCardName) {
                        Navigate.safeNavigateTo(findNavController(), R.id.action_nav_new_download_to_nav_download)
                        return@setOnClickListener
                    }
                    callDownloadFragment(name)
                }
                v.setOnLongClickListener {
                    if (name == oldDlCardName && path == oldDlCardName) {
                        return@setOnLongClickListener false
                    }
                    val chosenFile = File(extDir, name)
                    AlertDialog.Builder(context)
                        .setIcon(R.drawable.ic_launcher_foreground)
                        .setTitle(R.string.new_download_card_option_hint)
                        .setItems(arrayOf("删除", "前往")) { d, p ->
                            d.cancel()
                            when (p) {
                                0 -> {
                                    AlertDialog.Builder(context)
                                        .setIcon(R.drawable.ic_launcher_foreground).setMessage("删除下载的此漫画吗?")
                                        .setTitle("提示").setPositiveButton(android.R.string.ok) { _, _ ->
                                            if (chosenFile.exists()) Thread {
                                                FileUtils.recursiveRemove(chosenFile)
                                                MainActivity.mainWeakReference?.get()?.runOnUiThread {
                                                    it.visibility = View.INVISIBLE
                                                }
                                            }.start()
                                        }.setNegativeButton(android.R.string.cancel) { _, _ -> }
                                        .show()
                                }
                                1 -> {
                                    val bundle = Bundle()
                                    bundle.putBoolean("loadJson", true)
                                    bundle.putString("name", name)
                                    Navigate.safeNavigateTo(findNavController(), R.id.action_nav_new_download_to_nav_book, bundle)
                                }
                            }
                        }
                        .show()

                    true
                }
            }
        }
    }

    private fun callDownloadFragment(name: String){
        val bundle = Bundle()
        Log.d("MyNDF", "Call dl and is new.")
        bundle.putBoolean("loadJson", true)
        bundle.putString("name", name)
        ComicDlFragment.json = File(File(extDir, name), "info.json").readText()
        Log.d("MyNDF", "root view: $rootView")
        Log.d("MyNDF", "action_nav_new_download_to_nav_group")
        Navigate.safeNavigateTo(findNavController(), R.id.action_nav_new_download_to_nav_group, bundle)
    }

    fun showReverseInfo(toolsBox: UITools) {
        if (exit) return
        toolsBox.buildInfo("反转排序", "将按当前顺序的倒序显示下载的漫画",
            "确定", null, "取消", {
                isReverse = !isReverse
                isContentChanged = true
                reset()
                Thread {
                    sleep(600)
                    addPage()
                }.start()
            }
        )
    }

    override fun onLoadFinish() {
        super.onLoadFinish()
        MainActivity.mainWeakReference?.get()?.runOnUiThread {
            mypl.visibility = View.GONE
        }
    }

    companion object {
        var wn: WeakReference<NewDownloadFragment>? = null
    }
}
