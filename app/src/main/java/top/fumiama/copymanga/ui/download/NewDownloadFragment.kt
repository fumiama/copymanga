package top.fumiama.copymanga.ui.download

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.dialog_progress.view.*
import kotlinx.android.synthetic.main.line_header.view.*
import kotlinx.android.synthetic.main.line_lazybooklines.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.fumiama.copymanga.MainActivity
import top.fumiama.copymanga.api.Config.manga_dl_show_0m_manga
import top.fumiama.copymanga.api.manga.Downloader
import top.fumiama.copymanga.api.manga.Reader
import top.fumiama.sdict.io.Client
import top.fumiama.copymanga.storage.FileUtils
import top.fumiama.copymanga.storage.FileUtils.compressToUserFile
import top.fumiama.copymanga.view.interaction.Navigate
import top.fumiama.copymanga.view.interaction.UITools
import top.fumiama.copymanga.view.template.CardList
import top.fumiama.copymanga.view.template.MangaPagesFragmentTemplate
import top.fumiama.dmzj.copymanga.R
import java.io.File
import java.lang.ref.WeakReference

@OptIn(ExperimentalStdlibApi::class)
class NewDownloadFragment: MangaPagesFragmentTemplate(R.layout.fragment_newdownload, forceLoad = true) {
    private var sortedBookList: List<File>? = null
    private val oldDlCardName by lazy { getString(R.string.old_download_card_name) }
    private val extDir by lazy { activity?.getExternalFilesDir("") }
    private var isReverse = false
    private var isContentChanged = false
    private var exit = false
    private val showAll get() = manga_dl_show_0m_manga.value
    private var compressIntoFile: ActivityResultLauncher<String>? = null
    private var compressName = ""
    private var compressFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wn = WeakReference(this)

        compressIntoFile = FileUtils.registerZipExportLauncher(this@NewDownloadFragment) { uri ->
            val progressBar = layoutInflater.inflate(R.layout.dialog_progress, null, false)
            val progressHandler = object : Client.Progress{
                override fun notify(progressPercentage: Int) {
                    progressBar?.dpp?.progress = progressPercentage
                }
            }
            val info = (activity as MainActivity).toolsBox.buildAlertWithView("压缩${compressName}.zip", progressBar!!, "隐藏")
            val f = compressFile!!
            Thread{
                uri?.let { context?.let { ctx -> compressToUserFile(ctx, f, it) {
                    progressHandler.notify(it)
                    if (it >= 100) activity?.runOnUiThread { info.dismiss() }
                } } }
            }.start()
        }
    }

    override fun onPause() {
        super.onPause()
        exit = true
        wn = null
    }

    override fun onResume() {
        super.onResume()
        exit = false
        wn = WeakReference(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        wn = null
        exit = true
    }

    @SuppressLint("SetTextI18n")
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
                    sortedBookList = Downloader.getNonEmptyMangaList(sortedBookList) {
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
        sortedBookList?.let { lst ->
            withContext(Dispatchers.Main) {
                mysp?.footerView?.lht?.text = "$page+"
                activity?.findViewById<Toolbar>(R.id.toolbar)?.let { appbar ->
                    appbar.title.let { title ->
                        if (!title.endsWith(")")) {
                            appbar.title = "$title (${lst.size})"
                        }
                    }
                }
            }
            for(i in lst.listIterator(page)) {
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
                        cardList?.addCard(i.name, "\n本地读至 ${activity?.let { a ->
                            Reader.getLocalReadingProgress(a, i.name, Reader.getComicChapterNamesInFolder(i))
                        }}$bookSize")
                        cnt++
                    }
                }
                setProgress(80+20*(cnt-1)/size)
                if (cnt >= 21) break
            }
            if(page >= lst.size) {
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
                        .setItems(arrayOf("删除数据文件夹", "直接前往下载页", "导出压缩包")) { d, p ->
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
                                2 -> {
                                    compressName = name
                                    compressFile = chosenFile
                                    compressIntoFile?.launch("${name}.zip")
                                }
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

    fun importMangaFromZip() {

    }

    companion object {
        var wn: WeakReference<NewDownloadFragment>? = null
    }
}
