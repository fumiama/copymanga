package top.fumiama.copymanga.ui.download

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.fragment_download.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.fumiama.copymanga.MainActivity.Companion.mainWeakReference
import top.fumiama.copymanga.manga.Reader
import top.fumiama.copymanga.template.general.NoBackRefreshFragment
import top.fumiama.copymanga.tools.file.FileUtils
import top.fumiama.copymanga.tools.ui.Navigate
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
            lifecycleScope.launch {
                scanFile(arguments?.getString("file")?.let { File(it) }?:context?.getExternalFilesDir("")?:run {
                    findNavController().popBackStack()
                    return@launch
                })
            }
        }
    }

    private suspend fun scanFile(cd: File): Unit = withContext(Dispatchers.IO) {
        val isRoot = cd == context?.getExternalFilesDir("")
        val jsonFile = File(cd, "info.bin")
        if(isRoot || !jsonFile.exists()) cd.listFiles()?.filter { f -> return@filter f.isDirectory }?.map { f -> return@map f.name }?.sortedWith { o1, o2 ->
            if(o1.endsWith(".zip") && o2.endsWith(".zip")) (10000*getFloat(o1) - 10000*getFloat(o2) + 0.5).toInt()
            else o1[0] - o2[0]
        }?.let {
            mylv?.apply {
                val ad = ArrayAdapter(context, android.R.layout.simple_list_item_1, it)
                post {
                    setPadding(0, 0, 0, navBarHeight)
                    adapter = ad
                    setOnItemClickListener { _, _, position, _ ->
                        val chosenFile = File(cd, it[position])
                        val chosenJson = File(chosenFile, "info.bin")
                        val newJson = File(chosenFile, "info.json")
                        //Toast.makeText(this, "进入$chosenFile", Toast.LENGTH_SHORT).show()
                        when {
                            chosenJson.exists() -> callDownloadFragment(chosenJson)
                            newJson.exists() -> callDownloadFragment(newJson, true)
                            chosenFile.isDirectory -> {
                                callSelf(it[position], chosenFile)
                            }
                            chosenFile.name.endsWith(".zip") -> {
                                Toast.makeText(context, "加载中...", Toast.LENGTH_SHORT).show()
                                Reader.viewOldMangaZipFile(
                                    it.map { File(cd, it) }.toTypedArray(),
                                    it[position], position, chosenFile
                                )
                            }
                        }
                    }
                    setOnItemLongClickListener { _, _, position, _ ->
                        val chosenFile = File(cd, it[position])
                        Log.d("MyDF", "y: ${getChildAt(0).scrollY}")
                        AlertDialog.Builder(context)
                            .setIcon(R.drawable.ic_launcher_foreground).setMessage("删除?")
                            .setTitle("提示").setPositiveButton(android.R.string.ok) { _, _ ->
                                lifecycleScope.launch {
                                    withContext(Dispatchers.IO) {
                                        if (chosenFile.exists()) {
                                            FileUtils.recursiveRemove(chosenFile)
                                            scanFile(cd)
                                        }
                                    }
                                }
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
        if(isNew) {
            bundle.putString("loadJson", jsonFile.readText())
        } else {
            bundle.putBoolean("callFromOldDL", true)
        }
        bundle.putString("name", jsonFile.parentFile?.name?:"Null")
        Log.d("MyDF", "root view: $rootView")
        Log.d("MyDF", "action_nav_download_to_nav_group")
        Navigate.safeNavigateTo(findNavController(), R.id.action_nav_download_to_nav_group, bundle)
    }

    private fun callSelf(title: String, file: File){
        val bundle = Bundle()
        bundle.putString("title", title)
        bundle.putString("file", file.absolutePath)
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
}