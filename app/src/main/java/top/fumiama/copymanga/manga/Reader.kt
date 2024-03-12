package top.fumiama.copymanga.manga

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.edit
import com.google.gson.Gson
import top.fumiama.copymanga.MainActivity.Companion.mainWeakReference
import top.fumiama.copymanga.json.VolumeStructure
import top.fumiama.copymanga.ui.vm.ViewMangaActivity
import java.io.File

object Reader {
    fun start2viewManga(name: String, pos: Int, urlArray: Array<String>, fromFirstPage: Boolean = false) {
        Log.d("MyR", "viewMangaAt name $name, pos $pos")
        mainWeakReference?.get()?.apply {
            getPreferences(Context.MODE_PRIVATE)?.edit {
                putInt(name, pos)
                apply()
                Log.d("MyR", "记录 $name 阅读到第 ${pos+1} 话")
            }?: Log.d("MyR", "无法获得 main pref")
            // ViewMangaActivity.dlhandler = null
            ViewMangaActivity.position = pos
            ViewMangaActivity.comicName = name
            val zipf = ViewMangaActivity.fileArray[pos]
            val intent = Intent(this, ViewMangaActivity::class.java)
            intent.putExtra("urlArray", urlArray)
            if(!fromFirstPage) {
                intent.putExtra("function", "log")
                ViewMangaActivity.pn = -2
            }
            if (zipf.exists()) {
                ViewMangaActivity.zipFile = zipf
                //intent.putExtra("callFrom", "zipFirst")
                startActivity(intent)
            } else {
                ViewMangaActivity.zipFile = null
                startActivity(intent)
            }
        }
    }
    fun getComicPathWordInFolder(file: File): String {
        if(!file.exists()) {
            return "N/A:!file.exists()"
        }
        val jsonFile = File(file, "info.json")
        if(!jsonFile.exists()) {
            return  "N/A:!jsonFile.exists()"
        }
        Gson().fromJson(jsonFile.readText(), Array<VolumeStructure>::class.java)?.let { volumes ->
            if(volumes.isEmpty()) {
                return "N/A:volumes.isEmpty()"
            }
            if(volumes[0].results.list.isEmpty()) {
                return "N/A:volumes[0].results.list.isEmpty()"
            }
            return volumes[0].results.list[0].comic_path_word
        }
        return "N/A:null_gson"
    }
}
