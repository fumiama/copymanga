package top.fumiama.copymanga.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.activity_dlist.*
import kotlinx.android.synthetic.main.widget_titlebar.*
import top.fumiama.copymanga.R
import java.io.File

class DlListActivity:Activity() {
    private var exDir: File? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dlist)
        val titleText = intent.getStringExtra("title")
        ttitle.text = titleText?.substringAfterLast("/")
        exDir = getExternalFilesDir("")
        val innerDir = titleText?.substringAfter("我的下载")
        File(exDir, innerDir?:"").list()?.let {
            mylv.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, it)
            mylv.setOnItemClickListener { _, _, position, _ ->
                val chosenFile = File(exDir, "$innerDir/${it[position]}")
                val newTitle = "$titleText/${it[position]}"
                //Toast.makeText(this, "进入$chosenFile", Toast.LENGTH_SHORT).show()
                if (chosenFile.isDirectory) startActivity(
                    Intent(
                        this,
                        DlListActivity::class.java
                    ).putExtra("title", newTitle)
                )
                else{
                    ViewMangaActivity.zipFile = chosenFile
                    ViewMangaActivity.titleText = it[position]
                    startActivity(Intent(this, ViewMangaActivity::class.java))
                }
            }
        }
    }
}