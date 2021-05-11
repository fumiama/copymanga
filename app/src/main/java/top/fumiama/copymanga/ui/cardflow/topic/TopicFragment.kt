package top.fumiama.copymanga.ui.cardflow.topic

import android.os.Bundle
import android.view.View
import com.google.gson.Gson
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.line_lazybooklines.*
import kotlinx.android.synthetic.main.fragment_topic.*
import top.fumiama.copymanga.MainActivity.Companion.mainWeakReference
import top.fumiama.copymanga.json.TopicStructure
import top.fumiama.copymanga.template.http.AutoDownloadThread
import top.fumiama.copymanga.template.ui.InfoCardLoader
import top.fumiama.dmzj.copymanga.R

@ExperimentalStdlibApi
class TopicFragment : InfoCardLoader(R.layout.fragment_topic, R.id.action_nav_topic_to_nav_book) {
    private var type = 1
    override fun getApiUrl() =
        getString(R.string.topicContentApiUrl).let {
            String.format(it, arguments?.getString("path"), type, offset)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AutoDownloadThread(getString(R.string.topicApiUrl).let {
            String.format(it, arguments?.getString("path"))
        }) {
            it?.apply {
                val r = inputStream().reader()
                val topic = Gson().fromJson(r, TopicStructure::class.java)
                topic?.apply {
                    mainWeakReference?.get()?.let {
                        it.runOnUiThread {
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

    override fun onLoadFinish() {
        super.onLoadFinish()
        mainWeakReference?.get()?.runOnUiThread {
            mypl.visibility = View.GONE
        }
    }
}