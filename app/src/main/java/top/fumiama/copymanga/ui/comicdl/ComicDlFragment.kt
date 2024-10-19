package top.fumiama.copymanga.ui.comicdl

import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import kotlinx.android.synthetic.main.fragment_dlcomic.*
import kotlinx.android.synthetic.main.widget_downloadbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.fumiama.copymanga.json.VolumeStructure
import top.fumiama.copymanga.template.general.NoBackRefreshFragment
import top.fumiama.dmzj.copymanga.R
import java.io.File
import java.lang.ref.WeakReference

class ComicDlFragment: NoBackRefreshFragment(R.layout.fragment_dlcomic) {
    var ltbtn: View? = null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        exit = false
        ldwn?.setPadding(0, 0, 0, navBarHeight)
        dlsdwn?.translationY = -navBarHeight.toFloat()
        if(isFirstInflate) lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                when {
                    arguments?.getBoolean("callFromOldDL", false) == true -> initOldComicData()
                    arguments?.containsKey("loadJson") == true -> context?.getExternalFilesDir("")?.let { home ->
                        arguments?.getString("name")?.let {
                            delay(600)
                            Log.d("MyCDF", "loadJson by arguments")
                            start2load(
                                loadFromJson(arguments?.getString("loadJson")!!),
                                true, loadGroupsFromFile(File(home, "$it/grps.json"))
                            )
                        }
                    }
                    else -> findNavController().popBackStack()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //mainWeakReference?.get()?.menuMain?.let { setMenuInvisible(it) }
        handler?.downloading = false
        handler?.mangaDlTools?.exit = true
        handler?.dl?.dismiss()
        exit = true
        handler = null
    }

    private suspend fun start2load(volumes: Array<VolumeStructure>, isFromFile: Boolean = false, groupArray: Array<String>? =null) = withContext(Dispatchers.IO) {
        withContext(Dispatchers.Main) {
            handler = ComicDlHandler(Looper.myLooper()!!, WeakReference(this@ComicDlFragment),
                volumes, arguments?.getString("name")?:"null",
                if(isFromFile) groupArray else arguments?.getStringArray("groupNames")
            )
        }
        if(!isFromFile) {
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
        }
        handler?.startLoad()
    }

    private fun loadFromJson(json: String) = Gson().fromJson(json, Array<VolumeStructure>::class.java)
    private fun loadGroupsFromFile(file: File) = Gson().fromJson(file.reader(), Array<String>::class.java)

    private suspend fun initOldComicData() = withContext(Dispatchers.IO) {
        handler = ComicDlHandler(Looper.myLooper()!!, WeakReference(this@ComicDlFragment),
            arguments?.getString("name")?:"null")
        handler?.startLoad()
    }

    companion object {
        var handler: ComicDlHandler? = null
        var json: String? = null
        var exit = false
    }
}