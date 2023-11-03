package top.fumiama.copymanga.ui.cardflow.topic

import android.os.Bundle
import com.google.gson.Gson
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.fragment_topic.*
import top.fumiama.copymanga.MainActivity.Companion.mainWeakReference
import top.fumiama.copymanga.json.TopicStructure
import top.fumiama.copymanga.template.http.AutoDownloadThread
import top.fumiama.copymanga.template.ui.InfoCardLoader
import top.fumiama.copymanga.tools.api.CMApi
import top.fumiama.dmzj.copymanga.R

@ExperimentalStdlibApi
class TopicFragment : InfoCardLoader(R.layout.fragment_topic, R.id.action_nav_topic_to_nav_book) {
    private var type = 1
    override fun getApiUrl() =
        getString(R.string.topicContentApiUrl).format(CMApi.myHostApiUrl, arguments?.getString("path"), type, offset)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AutoDownloadThread(getString(R.string.topicApiUrl).format(CMApi.myHostApiUrl, arguments?.getString("path"))) { data ->
            if(ad?.exit == true) return@AutoDownloadThread
            data?.apply {
                val r = inputStream().reader()
                val topic = Gson().fromJson(r, TopicStructure::class.java)
                topic?.apply {
                    if(ad?.exit != false) return@AutoDownloadThread
                    activity?.let {
                        it.runOnUiThread {
                            if(ad?.exit != false) return@runOnUiThread
                            it.toolbar.title = results.title
                            ftttime.text = results.datetime_created
                            fttintro.text = results.intro
                            type = results.type
                        }
                    }
                }
            }
        }.start()
    }
}