package top.fumiama.copymanga.ui.comicdl

import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import com.google.gson.Gson
import kotlinx.android.synthetic.main.fragment_dlcomic.*
import top.fumiama.copymanga.json.ChapterStructure
import top.fumiama.copymanga.json.VolumeStructure
import top.fumiama.copymanga.template.http.AutoDownloadThread
import top.fumiama.copymanga.template.general.NoBackRefreshFragment
import top.fumiama.copymanga.tools.api.CMApi
import top.fumiama.dmzj.copymanga.R
import java.io.File
import java.lang.Thread.sleep
import java.lang.ref.WeakReference

class ComicDlFragment: NoBackRefreshFragment(R.layout.fragment_dlcomic) {
    var ltbtn: View? = null
    var ads = emptyArray<AutoDownloadThread>()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        exit = false
        ldwn?.setPadding(0, 0, 0, navBarHeight)
        if(isFirstInflate){
            when {
                arguments?.getBoolean("callFromOldDL", false) == true -> initOldComicData()
                arguments?.getBoolean("loadJson", false) == true -> context?.getExternalFilesDir("")?.let { home ->
                    arguments?.getString("name")?.let {
                        Thread{
                            sleep(600)
                            activity?.runOnUiThread {
                                start2load(loadFromJson(), true, loadGroupsFromFile(File(home, "$it/grps.json")))
                            }
                        }.start()
                    }
                }
                else -> initComicData(
                    arguments?.getString("path"),
                    arguments?.getStringArray("group"),
                    arguments?.getIntArray("count")
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //mainWeakReference?.get()?.menuMain?.let { setMenuInvisible(it) }
        handler?.downloading = false
        handler?.mangaDlTools?.exit = true
        handler?.dl?.dismiss()
        ads.forEach {
            it.exit = true
        }
        exit = true
        handler = null
    }

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

    /*private fun setMenuInvisible(menu: Menu){
        menu.findItem(R.id.action_download)?.isVisible = false
    }*/

    private fun initComicData(pw: String?, gpws: Array<String>?, counts: IntArray?) {
        var volumes = emptyArray<VolumeStructure>()
        if (gpws != null) {
            gpws.forEachIndexed { i, gpw ->
                Log.d("MyCDF", "下载:$gpw")
                var offset = 0
                val times = (counts?.get(i)?:1) / 100
                val remain = (counts?.get(i)?:1) % 100
                val re = arrayOfNulls<VolumeStructure>(if(remain != 0) (times+1) else (times))
                Log.d("MyCDF", "${i}卷共${if(times == 0) 1 else times}次加载")
                do {
                    counts?.set(i, counts[i] - 100)
                    CMApi.getGroupInfoApiUrl(pw, gpw, offset)?.let {
                        if(exit) return
                        val ad = AutoDownloadThread(it) { result ->
                            Log.d("MyCDF", "第${i}卷返回")
                            val r = Gson().fromJson(result?.decodeToString(), VolumeStructure::class.java)
                            re[r.results.offset / 100] = r
                        }
                        ads += ad
                        ad.start()
                        offset += 100
                    }
                } while ((counts?.get(i) ?: 0) > 0)
                Thread {
                    var c = 0
                    while (c++ < 80) {
                        sleep(1000)
                        if(exit) return@Thread
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
                    sleep(1000)
                    if(exit) return@Thread
                    Log.d("MyCDF", "已有：${volumes.size} 共：${gpws.size}")
                    c++
                }
                if (volumes.size == gpws.size) {
                    activity?.runOnUiThread {
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
        var handler: ComicDlHandler? = null
        var json: String? = null
        var exit = false
    }
}