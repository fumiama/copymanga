package top.fumiama.copymanga.ui.download

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.navigation.Navigation
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.fragment_download.*
import top.fumiama.dmzj.copymanga.R
import top.fumiama.copymanga.MainActivity.Companion.mainWeakReference
import top.fumiama.copymanga.template.general.NoBackRefreshFragment
import top.fumiama.copymanga.ui.comicdl.ComicDlFragment
import top.fumiama.copymanga.ui.vm.ViewMangaActivity
import java.io.File
import java.util.regex.Pattern
import java.util.zip.ZipInputStream

class DownloadFragment: NoBackRefreshFragment(R.layout.fragment_download) {
    private var nullZipDirStr = emptyArray<String>()
    private var handler: DlLHandler? = null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if(isFirstInflate) {
            arguments?.getString("title")?.let {
                mainWeakReference?.get()?.toolbar?.title = it
            }

            handler = DlLHandler(Looper.myLooper()!!, this)
            handler?.obtainMessage(3, currentDir)?.sendToTarget()       //call scanFile
        }
    }

    fun scanFile(cd: File?){
        val isRoot = cd == context?.getExternalFilesDir("")
        val jsonFile = File(cd, "info.bin")
        if(isRoot || !jsonFile.exists()) cd?.list()?.sortedArrayWith { o1, o2 ->
            if(o1.endsWith(".zip") && o2.endsWith(".zip")) (10000*getFloat(o1) - 10000*getFloat(o2) + 0.5).toInt()
            else o1[0] - o2[0]
        }?.let {
            mylv.apply {
                context.let { c ->
                    adapter = ArrayAdapter(c, android.R.layout.simple_list_item_1, it)
                    setOnItemClickListener { _, _, position, _ ->
                        val chosenFile = File(cd, it[position])
                        val chosenJson = File(chosenFile, "info.bin")
                        val newJson = File(chosenFile, "info.json")
                        //Toast.makeText(this, "进入$chosenFile", Toast.LENGTH_SHORT).show()
                        when {
                            chosenJson.exists() -> callDownloadFragment(chosenJson)
                            newJson.exists() -> callDownloadFragment(newJson, true)
                            chosenFile.isDirectory -> {
                                currentDir = chosenFile
                                callSelf(it[position])
                            }
                            chosenFile.name.endsWith(".zip") -> {
                                Toast.makeText(context, "加载中...", Toast.LENGTH_SHORT).show()
                                ViewMangaActivity.zipFile = chosenFile
                                ViewMangaActivity.comicName = it[position]
                                ViewMangaActivity.position = position
                                ViewMangaActivity.fileArray = it.map { File(cd, it) }.toTypedArray()
                                ViewMangaActivity.urlArray = Array(it.size) {return@Array ""}
                                startActivity(Intent(context, ViewMangaActivity::class.java))
                            }
                        }
                    }
                    setOnItemLongClickListener { _, _, position, _ ->
                        val chosenFile = File(cd, it[position])
                        AlertDialog.Builder(context)
                            .setIcon(R.drawable.ic_launcher_foreground).setMessage("在此执行删除/查错?")
                            .setTitle("提示").setPositiveButton("删除") { _, _ ->
                                if (chosenFile.exists()) handler?.obtainMessage(2, chosenFile)
                                    ?.sendToTarget()       //call rmrf
                                handler?.obtainMessage(3, cd)?.sendToTarget()       //call scanFile
                            }.setNegativeButton(android.R.string.cancel) { _, _ -> }
                            .setNeutralButton("查错") { _, _ ->
                                handler?.obtainMessage(1, chosenFile)?.sendToTarget()
                            }  //call checkDir
                            .show()
                        true
                    }
                }
            }
        }
    }


    fun rmrf(f: File) {
        if (f.isDirectory) f.listFiles()?.let {
            for (i in it)
                if (i.isDirectory) rmrf(i)
                else i.delete()
        }
        f.delete()
    }

    fun checkDir(f: File){
        nullZipDirStr = emptyArray()
        findNullWebpZipFileInDir(f)
        if(nullZipDirStr.isNotEmpty()) showErrorZip(nullZipDirStr.joinToString("\n"))
        else Toast.makeText(context, "未发现错误", Toast.LENGTH_SHORT).show()
    }

    private fun callDownloadFragment(jsonFile: File, isNew: Boolean = false){
        val bundle = Bundle()
        Log.d("MyDF", "Call dl and is new: $isNew")
        bundle.putBoolean(if(isNew) "loadJson" else "callFromOldDL", true)
        bundle.putString("name", jsonFile.parentFile?.name?:"Null")
        ComicDlFragment.json = jsonFile.readText()
        Log.d("MyDF", "root view: $rootView")
        rootView?.let {
            Log.d("MyDF", "action_nav_download_to_nav_group")
            Navigation.findNavController(it).navigate(R.id.action_nav_download_to_nav_group, bundle)
        }
    }

    private fun callSelf(title: String){
        val bundle = Bundle()
        bundle.putString("title", title)
        Log.d("MyDF", "Call self to $title")
        Log.d("MyDF", "root view: $rootView")
        rootView?.let {
            Log.d("MyDF", "action_nav_download_self")
            Navigation.findNavController(it).navigate(R.id.action_nav_download_self, bundle)
        }
    }

    private fun findNullWebpZipFileInDir(f: File){
        if (f.isDirectory) f.listFiles()?.let {
            for (i in it)
                if (i.isDirectory) findNullWebpZipFileInDir(i)
                else if(!checkZip(i)) nullZipDirStr += i.path.substringAfterLast(context?.getExternalFilesDir("").toString())
        }
    }

    private fun checkZip(f: File): Boolean{
        return try {
            val exist = f.exists()
            if (!exist) true
            else {
                var re = true
                val zip = ZipInputStream(f.inputStream().buffered())
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory){
                        if(zip.read() == -1 && entry.size == 0L){
                            re = false
                            break
                        }
                    }
                    entry = zip.nextEntry
                }
                zip.closeEntry()
                zip.close()
                re
            }
        } catch (e: Exception) {
            Toast.makeText(context, "读取${f.name}错误!", Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun showErrorZip(msg: CharSequence) = AlertDialog.Builder(context)
        .setIcon(R.drawable.ic_launcher_foreground)
        .setTitle("找到以下错误文件,是否删除?")
        .setMessage(msg)
        .setPositiveButton(android.R.string.ok){_, _ -> deleteErrorZip()}
        .setNegativeButton(android.R.string.cancel){_, _ ->}
        .show()

    private fun deleteErrorZip(){
        val exf = context?.getExternalFilesDir("")
        for(i in nullZipDirStr){
            val f = File(exf, i)
            if(f.exists()) f.delete()
        }
    }

    private fun getFloat(oldString: String): Float {
        val newString = StringBuffer()
        var matcher = Pattern.compile("\\d+.+\\d+").matcher(oldString)
        while (matcher.find()) newString.append(matcher.group())
        //Log.d("MyDLL1", newString.toString())
        if(newString.isEmpty()){
            matcher = Pattern.compile("\\d").matcher(oldString)
            while (matcher.find()) newString.append(matcher.group())
        }
        //Log.d("MyDLL2", newString.toString().toFloat().toString())
        return if(newString.isEmpty()) 0f else newString.toString().toFloat()
    }

    companion object{
        var currentDir: File? = null
    }
}