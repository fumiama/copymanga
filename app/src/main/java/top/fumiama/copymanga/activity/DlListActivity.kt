package top.fumiama.copymanga.activity

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_dlist.*
import kotlinx.android.synthetic.main.widget_titlebar.*
import top.fumiama.copymanga.R
import java.io.File

class DlListActivity:Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dlist)
        ttitle.text = intent.getStringExtra("title")
        scanFile(currentDir)
    }

    private fun scanFile(cd: File?){
        cd?.list()?.sortedArray()?.let {
            mylv.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, it)
            mylv.setOnItemClickListener { _, _, position, _ ->
                val chosenFile = File(cd, it[position])
                //Toast.makeText(this, "进入$chosenFile", Toast.LENGTH_SHORT).show()
                if (chosenFile.isDirectory) {
                    currentDir = chosenFile
                    startActivity(
                        Intent(
                            this,
                            DlListActivity::class.java
                        ).putExtra("title", it[position])
                    )
                }
                else{
                    Toast.makeText(this, "加载中...", Toast.LENGTH_SHORT).show()
                    ViewMangaActivity.zipFile = chosenFile
                    ViewMangaActivity.titleText = it[position]
                    startActivity(Intent(this, ViewMangaActivity::class.java))
                }
            }
            mylv.setOnItemLongClickListener { _, _, position, _ ->
                val chosenFile = File(cd, it[position])
                AlertDialog.Builder(this)
                    .setIcon(R.drawable.ic_launcher_foreground).setMessage("是否删除?")
                    .setTitle("提示").setPositiveButton("确定"){ _, _ ->
                        if(chosenFile.exists()) rmrf(chosenFile)
                        scanFile(cd)
                    }.setNegativeButton("取消"){_, _ ->}.show()
                true
            }
        }
    }

    private fun rmrf(f: File) {
        if (f.isDirectory) f.listFiles()?.let {
            for (i in it)
                if (i.isDirectory) rmrf(i)
                else i.delete()
        }
        f.delete()
    }

    companion object{
        var currentDir: File? = null
    }
}

