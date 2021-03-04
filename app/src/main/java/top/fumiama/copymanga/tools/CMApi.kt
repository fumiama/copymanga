package top.fumiama.copymanga.tools

import com.bumptech.glide.load.model.LazyHeaders
import top.fumiama.dmzj.copymanga.R
import top.fumiama.copymanga.MainActivity
import top.fumiama.copymanga.json.Chapter2Return
import java.io.File

object CMApi {
    var myGlideHeaders: LazyHeaders? = null
        get() {
            if(field === null) field = LazyHeaders.Builder().addHeader("referer", MainActivity.mainWeakReference?.get()?.getString(R.string.referUrl)!!).addHeader("User-Agent", MainActivity.mainWeakReference?.get()?.getString(R.string.pc_ua)!!).build()
            return field
        }
    fun getImgZipFileFromVM(exDir: File?, chapter2Return: Chapter2Return?) = File(exDir, "${chapter2Return?.results?.comic?.name}/${chapter2Return?.results?.chapter?.group_path_word}/${chapter2Return?.results?.chapter?.name}.zip")
    fun getZipFile(exDir: File?, manga: String, caption: CharSequence, name: CharSequence) = File(exDir, "$manga/$caption/$name.zip")
    fun getApiUrl(id: Int, arg1: String?, arg2: String?) = MainActivity.mainWeakReference?.get()?.getString(id)?.let { String.format(it, arg1, arg2) }
}