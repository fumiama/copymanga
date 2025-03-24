package top.fumiama.copymanga.ui.cardflow.topic

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.fragment_topic.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.fumiama.copymanga.json.TopicStructure
import top.fumiama.copymanga.template.http.PausableDownloader
import top.fumiama.copymanga.template.ui.InfoCardLoader
import top.fumiama.copymanga.api.Config
import top.fumiama.dmzj.copymanga.R

@ExperimentalStdlibApi
class TopicFragment : InfoCardLoader(R.layout.fragment_topic, R.id.action_nav_topic_to_nav_book) {
    private var type = 1
    override fun getApiUrl() =
        getString(R.string.topicContentApiUrl).format(Config.myHostApiUrl.value, arguments?.getString("path"), type, offset)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            setProgress(5)
            PausableDownloader(getString(R.string.topicApiUrl).format(Config.myHostApiUrl.value, arguments?.getString("path"))) { data ->
                setProgress(10)
                withContext(Dispatchers.IO) {
                    if(ad?.exit == true) return@withContext
                    data.inputStream().use { i ->
                        val r = i.reader()
                        Gson().fromJson(r, TopicStructure::class.java)?.apply {
                            if(ad?.exit == true) return@withContext
                            withContext(Dispatchers.Main) withMain@ {
                                setProgress(15)
                                if(ad?.exit == true) return@withMain
                                activity?.toolbar?.title = results.title
                                ftttime.text = results.datetime_created
                                fttintro.text = results.intro
                                type = results.type
                            }
                        }
                    }
                }
            }.run()
        }
    }
}
