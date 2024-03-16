package top.fumiama.copymanga.manga

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.edit
import com.google.gson.Gson
import kotlinx.android.synthetic.main.button_tbutton.view.*
import top.fumiama.copymanga.MainActivity.Companion.mainWeakReference
import top.fumiama.copymanga.json.VolumeStructure
import top.fumiama.copymanga.ui.vm.ViewMangaActivity
import java.io.File

object Reader {
    var fileArray = arrayOf<File>()
    fun start2viewManga(name: String?, pos: Int, urlArray: Array<String>, uuidArray: Array<String>, fromFirstPage: Boolean = false) {
        Log.d("MyR", "viewMangaAt name $name, pos $pos")
        mainWeakReference?.get()?.apply {
            // ViewMangaActivity.dlhandler = null
            val intent = Intent(this, ViewMangaActivity::class.java)
            name?.let { n ->
                getPreferences(Context.MODE_PRIVATE)?.edit {
                    putInt(n, pos)
                    apply()
                    Log.d("MyR", "记录 $n 阅读到第 ${pos+1} 话")
                }?: Log.d("MyR", "无法获得 main pref")
                intent.putExtra("comicName", name)
            }
            intent.putExtra("position", pos)
            intent.putExtra("urlArray", urlArray)
            intent.putExtra("uuidArray", uuidArray)
            if (!fromFirstPage) {
                intent.putExtra("function", "log")
                intent.putExtra("pn", -2)
            }
            val zipFile = fileArray[pos]
            if (zipFile.exists()) {
                intent.putExtra("zipFile", zipFile.absolutePath)
                //intent.putExtra("callFrom", "zipFirst")
            }
            startActivity(intent)
        }
    }
    fun viewOldMangaZipFile(fileArray: Array<File>, name: String, pos: Int, zipFile: File) {
        Reader.fileArray = fileArray
        mainWeakReference?.get()?.apply {
            val intent = Intent(this, ViewMangaActivity::class.java)
            intent.putExtra("comicName", name)
            intent.putExtra("position", pos)
            intent.putExtra("zipFile", zipFile.absolutePath)
            startActivity(intent)
        }
    }
    fun viewMangaZipFile(pos: Int, urlArray: Array<String>, uuidArray: Array<String>, zipFile: File) {
        mainWeakReference?.get()?.apply {
            val intent = Intent(this, ViewMangaActivity::class.java)
            intent.putExtra("position", pos)
                .putExtra("urlArray", urlArray)
                .putExtra("uuidArray", uuidArray)
                .putExtra("callFrom", "zipFirst")
                .putExtra("zipFile", zipFile.absolutePath)
            startActivity(intent)
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
