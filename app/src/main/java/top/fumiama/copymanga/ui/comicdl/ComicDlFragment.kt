package top.fumiama.copymanga.ui.comicdl

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.Menu
import android.view.View
import com.google.gson.Gson
import top.fumiama.dmzj.copymanga.R
import top.fumiama.copymanga.MainActivity.Companion.mainWeakReference
import top.fumiama.copymanga.json.ChapterStructure
import top.fumiama.copymanga.json.VolumeStructure
import top.fumiama.copymanga.template.AutoDownloadThread
import top.fumiama.copymanga.template.NoBackRefreshFragment
import top.fumiama.copymanga.tools.CMApi
import java.io.File
import java.lang.Thread.sleep
import java.lang.ref.WeakReference

class ComicDlFragment:NoBackRefreshFragment(R.layout.fragment_dlcomic) {
    var handler: ComicDlHandler? = null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if(isFirstInflate){
            when {
                arguments?.getBoolean("callFromOldDL", false) == true -> initOldComicData()
                arguments?.getBoolean("loadJson", false) == true -> context?.getExternalFilesDir("")?.let { home ->
                    arguments?.getString("name")?.let {
                        start2load(loadFromJson(), true, loadGroupsFromFile(File(home, "$it/grps.json")))
                    }
                }
                else -> initComicData(
                    arguments?.getString("path"),
                    arguments?.getStringArray("group"),
                    arguments?.getIntArray("count")
                )
            }
        }
        mainWeakReference?.get()?.menuMain?.let { setMenuVisible(it) }
    }

    /*override fun onDestroy() {
        super.onDestroy()
        mainWeakReference?.get()?.menuMain?.let { setMenuInvisible(it) }
    }*/

    private fun start2load(volumes: Array<VolumeStructure>, isFromFile: Boolean = false, groupArray: Array<String>? =null){
        handler = ComicDlHandler(Looper.myLooper()!!,
            WeakReference(this),
            volumes,
            arguments?.getString("name")?:"null",
            if(isFromFile) groupArray else arguments?.getStringArray("groupNames"))
        if(!isFromFile) Thread{
            context?.getExternalFilesDir("")?.let { home ->
                arguments?.getString("name")?.let { name ->
                    val mangaFolder = File(home, name)
                    if(!mangaFolder.exists()) mangaFolder.mkdirs()
                    File(mangaFolder, "info.json").writeText(Gson().toJson(volumes))
                    arguments?.getStringArray("groupNames")?.let {
                        File(mangaFolder, "grps.json").writeText(Gson().toJson(it))
                    }
                }
            }
        }.start()
        handler?.startLoad()
    }

    private fun loadFromJson() = Gson().fromJson(json, Array<VolumeStructure>::class.java)
    private fun loadGroupsFromFile(file: File) = Gson().fromJson(file.reader(), Array<String>::class.java)

    private fun setMenuVisible(menu: Menu) {
        val dl = menu.findItem(R.id.action_download)
        dl?.isVisible = true
        dl?.setIcon(R.drawable.ic_menu_sort)
        dl?.setOnMenuItemClickListener {
            if(handler?.complete == true && it.itemId == R.id.action_download){
                handler?.showMultiSelectInfo()
                true
            }
            else it.itemId == R.id.action_download
        }
    }

    /*private fun setMenuInvisible(menu: Menu){
        menu.findItem(R.id.action_download)?.isVisible = false
    }*/

    private fun initComicData(pw: String?, gpws: Array<String>?, counts: IntArray?) {
        var volumes = emptyArray<VolumeStructure>()
        if (gpws != null) {
            gpws.forEachIndexed { i, gpw ->
                Log.d("MyCDF", "下载:$gpw")
                var offset = 0
                val re = arrayOfNulls<VolumeStructure>(counts?.get(i)?:1)
                do {
                    counts?.set(i, counts[i] - 100)
                    CMApi.getApiUrl(R.string.groupInfoApiUrl, pw, gpw, offset)?.let {
                        AutoDownloadThread(it) { result ->
                            //Log.d("MyCDF", "返回:${result?.decodeToString()}")
                            val r = Gson().fromJson(result?.decodeToString(), VolumeStructure::class.java)
                            re[r.results.offset / 100] = r
                        }.start()
                        offset += 100
                    }
                } while ((counts?.get(i) ?: 0) > 0)
                Thread {
                    var c = 0
                    while (c++ < 80) {
                        sleep(100)
                        if(re.all { it != null }) break
                    }
                    if(re.size > 1) {
                        val r = re[0]
                        var s = emptyArray<ChapterStructure>()
                        re.forEach {
                            it?.results?.list?.forEach {
                                s += it
                            }
                        }
                        r?.results?.list = s
                        r?.apply { volumes += this }
                    } else re[0]?.apply { volumes += this }
                }.start()
            }
            Thread {
                var c = 0
                while (c < 80 && volumes.size != gpws.size) {
                    sleep(100)
                    Log.d("MyCDF", "已有：${volumes.size} 共：${gpws.size}")
                    c++
                }
                if (volumes.size == gpws.size) {
                    mainWeakReference?.get()?.runOnUiThread {
                        start2load(volumes)
                    }
                }
            }.start()
        }
    }

    private fun initOldComicData() {
        handler = ComicDlHandler(Looper.myLooper()!!,
            WeakReference(this),
            arguments?.getString("name")?:"null")
        handler?.startLoad()
    }

    companion object {
        var json: String? = null
    }
}