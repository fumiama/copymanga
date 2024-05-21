package top.fumiama.copymanga.ui.download

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.fumiama.copymanga.MainActivity.Companion.mainWeakReference
import top.fumiama.copymanga.manga.MangaDlTools
import top.fumiama.copymanga.manga.Reader
import top.fumiama.copymanga.template.general.MangaPagesFragmentTemplate
import top.fumiama.copymanga.template.ui.CardList
import top.fumiama.copymanga.tools.file.FileUtils
import top.fumiama.copymanga.tools.ui.Navigate
import top.fumiama.copymanga.tools.ui.UITools
import top.fumiama.dmzj.copymanga.R
import java.io.File
import java.lang.ref.WeakReference

@OptIn(ExperimentalStdlibApi::class)
class NewDownloadFragment: MangaPagesFragmentTemplate(R.layout.fragment_newdownload, forceLoad = true) {
    private var sortedBookList: List<File>? = null
    private val oldDlCardName = mainWeakReference?.get()?.getString(R.string.old_download_card_name)!!
    private val extDir = mainWeakReference?.get()?.getExternalFilesDir("")
    private var isReverse = false
    private var isContentChanged = false
    private var exit = false
    private var showAll = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wn = WeakReference(this)
        showAll = activity?.let {
            PreferenceManager.getDefaultSharedPreferences(it)
        }?.getBoolean("settings_cat_md_sw_show_0m_manga", false)?:false
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

    override suspend fun addPage(): Unit = withContext(Dispatchers.IO) {
        super.addPage()
        if(isRefresh) {
            page = 0
            isRefresh = false
        }
        if (isEnd) {
            onLoadFinish()
            return@withContext
        }
        setProgress(20)
        if(sortedBookList == null || isContentChanged) {
            Log.d("MyNDF", "Sorting books...")
            sortedBookList = extDir?.listFiles()?.toList()
            var size = sortedBookList?.size?:0
            if (size > 0) {
                if (!showAll) {
                    sortedBookList = MangaDlTools.getNonEmptyMangaList(sortedBookList) {
                        setProgress(40+20*it/100)
                    }
                }
                setProgress(40)
                size = sortedBookList?.size?:0
                val cache = hashMapOf<String, String>()
                sortedBookList = sortedBookList?.sortedBy {
                    setProgress(60+20*cache.size/size)
                    return@sortedBy it.absolutePath.let { path ->
                        if (cache.containsKey(path)) cache[path]!!
                        else {
                            val s = Reader.getComicPathWordInFolder(it).lowercase()
                            cache[path] = s
                            s
                        }
                    }
                }
                setProgress(60)
                if (isReverse) {
                    Log.d("MyNDF", "reversed...")
                    sortedBookList = sortedBookList?.asReversed()
                }
                setProgress(80)
            }
            isContentChanged = false
        }
        Log.d("MyNDF", "Start drawing cards")
        var cnt = 0
        if(page == 0) {
            cardList?.addCard(oldDlCardName, path = oldDlCardName)
            cnt = 1
        }
        val size = sortedBookList?.size?:0
        sortedBookList?.let {
            for(i in it.listIterator(page)) {
                if(cardList?.exitCardList != false) return@withContext
                page++ // page is actually count
                val chosenJson = File(i, "info.bin")
                val newJson = File(i, "info.json")
                val bookSize = FileUtils.sizeOf(i).let { sz ->
                    (sz/1048576).toInt().let { m ->
                        if (m > 0) "\n${m}MB" else "\n${(sz/1024).toInt()}KB"
                    }
                }
                when {
                    chosenJson.exists() -> continue // unsupported old folder
                    newJson.exists() -> {
                        if(cardList?.exitCardList != false) return@withContext
                        cardList?.addCard(i.name, bookSize)
                        cnt++
                    }
                }
                setProgress(80+20*(cnt-1)/size)
                if (cnt >= 21) break
            }
            if(page >= it.size) {
                isEnd = true
            }
        }
        setProgress(99)
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
                    callBookFragment(name)
                }
                v.setOnLongClickListener {
                    if (name == oldDlCardName && path == oldDlCardName) {
                        return@setOnLongClickListener false
                    }
                    val chosenFile = File(extDir, name)
                    AlertDialog.Builder(context)
                        .setIcon(R.drawable.ic_launcher_foreground)
                        .setTitle(R.string.new_download_card_option_hint)
                        .setItems(arrayOf("删除数据文件夹", "直接前往下载页")) { d, p ->
                            d.cancel()
                            when (p) {
                                0 -> {
                                    AlertDialog.Builder(context)
                                        .setIcon(R.drawable.ic_launcher_foreground).setMessage("删除下载的漫画${name}吗?")
                                        .setTitle("提示").setPositiveButton(android.R.string.ok) { _, _ ->
                                            if (chosenFile.exists()) lifecycleScope.launch {
                                                withContext(Dispatchers.IO) {
                                                    FileUtils.recursiveRemove(chosenFile)
                                                    withContext(Dispatchers.Main) {
                                                        it.visibility = View.INVISIBLE
                                                    }
                                                }
                                            }
                                        }.setNegativeButton(android.R.string.cancel) { _, _ -> }
                                        .show()
                                }
                                1 -> callDownloadFragment(name)
                            }
                        }
                        .show()

                    true
                }
            }
        }
    }

    private fun callBookFragment(name: String) {
        val bundle = Bundle()
        bundle.putBoolean("loadJson", true)
        bundle.putString("name", name)
        Navigate.safeNavigateTo(findNavController(), R.id.action_nav_new_download_to_nav_book, bundle)
    }

    private fun callDownloadFragment(name: String) {
        val bundle = Bundle()
        Log.d("MyNDF", "Call dl and is new.")
        bundle.putString("loadJson", File(File(extDir, name), "info.json").readText())
        bundle.putString("name", name)
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
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        showKanban()
                        reset()
                        delay(600)
                        addPage()
                        hideKanban()
                    }
                }
            }
        )
    }

    companion object {
        var wn: WeakReference<NewDownloadFragment>? = null
    }
}
