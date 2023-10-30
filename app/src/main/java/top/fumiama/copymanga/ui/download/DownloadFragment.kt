package top.fumiama.copymanga.ui.download

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.fragment_download.*
import top.fumiama.copymanga.MainActivity.Companion.mainWeakReference
import top.fumiama.copymanga.template.general.NoBackRefreshFragment
import top.fumiama.copymanga.tools.ui.Navigate
import top.fumiama.copymanga.tools.file.FileUtils
import top.fumiama.copymanga.ui.comicdl.ComicDlFragment
import top.fumiama.copymanga.ui.vm.ViewMangaActivity
import top.fumiama.dmzj.copymanga.R
import java.io.File
import java.util.regex.Pattern

class DownloadFragment: NoBackRefreshFragment(R.layout.fragment_download) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if(isFirstInflate) {
            arguments?.getString("title")?.let {
                mainWeakReference?.get()?.toolbar?.title = it
            }
            scanFile(currentDir)
        }
    }

    private fun scanFile(cd: File?) {
        val isRoot = cd == context?.getExternalFilesDir("")
        val jsonFile = File(cd, "info.bin")
        if(isRoot || !jsonFile.exists()) cd?.list()?.sortedArrayWith { o1, o2 ->
            if(o1.endsWith(".zip") && o2.endsWith(".zip")) (10000*getFloat(o1) - 10000*getFloat(o2) + 0.5).toInt()
            else o1[0] - o2[0]
        }?.let {
            mylv?.apply {
                setPadding(0, 0, 0, navBarHeight)
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
                            .setIcon(R.drawable.ic_launcher_foreground).setMessage("删除?")
                            .setTitle("提示").setPositiveButton(android.R.string.ok) { _, _ ->
                                if (chosenFile.exists()) FileUtils.recursiveRemove(chosenFile)
                                scanFile(cd)
                            }.setNegativeButton(android.R.string.cancel) { _, _ -> }
                            .show()
                        true
                    }
                }
            }
        }
    }

    private fun callDownloadFragment(jsonFile: File, isNew: Boolean = false){
        val bundle = Bundle()
        Log.d("MyDF", "Call dl and is new: $isNew")
        bundle.putBoolean(if(isNew) "loadJson" else "callFromOldDL", true)
        bundle.putString("name", jsonFile.parentFile?.name?:"Null")
        ComicDlFragment.json = jsonFile.readText()
        Log.d("MyDF", "root view: $rootView")
        Log.d("MyDF", "action_nav_download_to_nav_group")
        Navigate.safeNavigateTo(findNavController(), R.id.action_nav_download_to_nav_group, bundle)
    }

    private fun callSelf(title: String){
        val bundle = Bundle()
        bundle.putString("title", title)
        Log.d("MyDF", "Call self to $title")
        Log.d("MyDF", "root view: $rootView")
        Log.d("MyDF", "action_nav_download_self")
        Navigate.safeNavigateTo(findNavController(), R.id.action_nav_download_self, bundle)
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