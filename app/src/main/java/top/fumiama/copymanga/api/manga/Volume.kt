package top.fumiama.copymanga.api.manga

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.fumiama.copymanga.json.ChapterStructure
import top.fumiama.copymanga.json.VolumeStructure
import top.fumiama.copymanga.net.template.PausableDownloader
import top.fumiama.copymanga.api.Config
import top.fumiama.dmzj.copymanga.R

class Volume(private val path: String, private val groupPathWord: String, getString: (Int) -> String, private val isExit: ()->Boolean, private val setProgress: ((Int) -> Unit)? = null) {
    private val mGroupInfoApiUrlTemplate = getString(R.string.groupInfoApiUrl)
    private val exit: Boolean
        get() {
            if (!isExit()) return false
            // destroy
            mDownloaders.forEach { it.exit = true }
            return true
        }
    private var mDownloaders = arrayOf<PausableDownloader>()
    private var mVolume: VolumeStructure? = null
    private var mProgress = 0
        set(value) {
            setProgress?.let { it(field) }
            field = value
        }
    private var mDelta = 0
    suspend fun updateChapters(count: Int): VolumeStructure? = withContext(Dispatchers.IO) {
        val times = count / 100
        val remain = count % 100
        val re = arrayOfNulls<VolumeStructure>(if(remain != 0) (times+1) else (times))
        if (re.isEmpty()) return@withContext null
        Log.d("MyV", "${groupPathWord}卷共需加载${if(times == 0) 1 else times}次")
        mProgress = 0
        mDelta = 100/re.size
        download(re, 0, count)
        return@withContext mVolume
    }

    private fun getApiUrl(offset: Int) = mGroupInfoApiUrlTemplate.format(Config.myHostApiUrl.value, path, groupPathWord, offset)
    private suspend fun download(re: Array<VolumeStructure?>, offset: Int, c: Int) = withContext(Dispatchers.IO) {
        Log.d("MyV", "下载偏移: $offset")
        getApiUrl(offset).let {
            if (exit) return@withContext
            val ad = PausableDownloader(it, whenFinish = whenFinish(re, c-100, offset+100))
            mDownloaders += ad
            ad.run()
        }
        mProgress += mDelta
    }
    private fun whenFinish(re: Array<VolumeStructure?>, c: Int, offset: Int): suspend (ByteArray) -> Unit = lambda@ { result: ByteArray ->
        try {
            val r = Gson().fromJson(result.decodeToString(), VolumeStructure::class.java)
            val o = r.results.offset / 100
            re[o] = r
            Log.d("MyV", "获得${groupPathWord}卷的${r.results.list.size}章内容, 偏移$o*100=${r.results.offset}, 共${re.size}")
            if (c > 0) {
                download(re, offset, c)
                return@lambda
            }
            if (re.any { it == null }) { // have uncompleted items
                Log.d("MyV", "下载未完成, 存在空卷")
                return@lambda
            }
            if(re.isNotEmpty()) { // safer check, likely
                re[0]?.let {
                    var s = emptyArray<ChapterStructure>()
                    re.forEach { v ->
                        v?.results?.list?.forEach { chapter ->
                            s += chapter
                        }
                    }
                    it.results?.list = s
                }
                mVolume = re[0]
            }
            return@lambda
        } catch (e: Exception) {
            e.printStackTrace()
            return@lambda
        }
    }
}