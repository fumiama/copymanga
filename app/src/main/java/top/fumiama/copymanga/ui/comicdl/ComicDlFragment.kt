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
                    arguments?.getStringArray("group")
                )
            }
        }
        mainWeakReference?.get()?.menuMain?.let { setMenuVisible(it) }
    }

    /*override fun onDestroy() {
        super.onDestroy()
        mainWeakReference?.get()?.menuMain?.let { setMenuInvisible(it) }
    }*/

    fun start2load(volumes: Array<VolumeStructure>, isFromFile: Boolean = false, groupArray: Array<String>? =null){
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

    private fun initComicData(pw: String?, gpws: Array<String>?) {
        var volumes = arrayOf<VolumeStructure>()
        val waitHandler = WaitHandler(WeakReference(this))
        if (gpws != null) {
            gpws.forEach { gpw ->
                Log.d("MyCDF", "下载:$gpw")
                CMApi.getApiUrl(R.string.groupInfoApiUrl, pw, gpw)?.let {
                    AutoDownloadThread(it) { result ->
                        //Log.d("MyCDF", "返回:${result?.decodeToString()}")
                        volumes += Gson().fromJson(
                            result?.decodeToString(),
                            VolumeStructure::class.java
                        )
                    }.start()
                }
            }
            Thread {
                var c = 0
                while (c < 80 && volumes.size != gpws.size) {
                    sleep(100)
                    Log.d("MyCDF", "已有：${volumes.size} 共：${gpws.size}")
                    c++
                }
                if (volumes.size == gpws.size) {
                    waitHandler.obtainMessage(0, volumes).sendToTarget()
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

    class WaitHandler(private val that: WeakReference<ComicDlFragment>): Handler(Looper.myLooper()!!){
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when(msg.what){
                0 -> that.get()?.start2load(msg.obj as Array<VolumeStructure>)
            }
        }
    }

    companion object {
        var json: String? = null
    }
}