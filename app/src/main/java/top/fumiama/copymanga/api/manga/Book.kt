package top.fumiama.copymanga.api.manga

import android.util.Log
import com.google.gson.Gson
import kotlinx.android.synthetic.main.card_book.*
import kotlinx.android.synthetic.main.line_booktandb.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.fumiama.copymanga.json.BookInfoStructure
import top.fumiama.copymanga.json.ThemeStructure
import top.fumiama.copymanga.json.VolumeStructure
import top.fumiama.copymanga.api.Config
import top.fumiama.copymanga.net.DownloadTools
import top.fumiama.dmzj.copymanga.R
import java.io.File

class Book(val path: String, private val getString: (Int) -> String, private val exDir: File, private val loadCache: Boolean = false, private val mPassName: String? = null) {
    private val mBookApiUrl = getString(R.string.bookInfoApiUrl).format(Config.myHostApiUrl.random(), path, Config.platform.value)
    private val mUserAgent = getString(R.string.pc_ua).format(Config.app_ver.value)
    private var mBook: BookInfoStructure? = null
    private var mGroupPathWords = arrayOf<String>()
    private var mKeys = arrayOf<String>()
    private var mCounts = intArrayOf()
    private var mVolumes = arrayOf<VolumeStructure>()
    private var mJsonString = ""
    var exit = false
    val name: String? get() = mBook?.results?.comic?.name?:mPassName
    val cover: String? get() = mBook?.results?.comic?.cover
    val cachedCover: File?
        get() {
            val mangaFolder = name?.let { File(exDir, it) }?:return null
            val head = File(mangaFolder, "head.jpg")
            if (!head.exists()) return null
            return head
        }
    val region get() = mBook?.results?.comic?.region?.display?:"未知"
    val author: Array<ThemeStructure>? get() = mBook?.results?.comic?.author
    val theme: Array<ThemeStructure>? get() = mBook?.results?.comic?.theme
    val keys get() = mKeys
    val popular get() = mBook?.results?.comic?.popular?:0
    val status get() = mBook?.results?.comic?.status?.display?:"未知"
    val updateTime get() = mBook?.results?.comic?.datetime_updated?:"未知"
    val brief get() = mBook?.results?.comic?.brief?:"空简介"
    val volumes get() = mVolumes
    val uuid get() = mBook?.results?.comic?.uuid
    val json get() = mJsonString
    val version get() = if (mBook?.results?.comic?.reclass != null) 1 else 2

    constructor(name: String, getString: (Int) -> String, exDir: File): this(
        Reader.getComicPathWordInFolder(File(exDir, name)),
        getString, exDir, true, name
    )

    /**
     * 更新云端最新图书信息并缓存到本地
     */
    suspend fun updateInfo() = withContext(Dispatchers.IO) {
        try {
            var isDownload = false
            val data: ByteArray = if (loadCache) {
                name?.let { loadInfo(it) } ?: run {
                    isDownload = true
                    Config.apiProxy?.comancry(mBookApiUrl) { url ->
                        DownloadTools.getHttpContent(url, null, mUserAgent)
                    }?:DownloadTools.getHttpContent(mBookApiUrl, null, mUserAgent)
                }
            } else {
                isDownload = true
                Config.apiProxy?.comancry(mBookApiUrl) { url ->
                    DownloadTools.getHttpContent(url, null, mUserAgent)
                }?:DownloadTools.getHttpContent(mBookApiUrl, null, mUserAgent)
            }?:DownloadTools.getHttpContent(mBookApiUrl, null, mUserAgent)
            mBook = data.inputStream().use {
                Gson().fromJson(it.reader(), BookInfoStructure::class.java)
            }
            if (isDownload) saveInfo(data)
            mGroupPathWords = arrayOf()
            mKeys = arrayOf()
            mCounts = intArrayOf()
            mBook?.results?.groups?.values?.forEach {
                mKeys += it.name
                mGroupPathWords += it.path_word
                if (it.count == 0) {
                    it.count = 1
                }
                mCounts += it.count
                Log.d("MyB", "Add caption: ${it.name} @ ${it.path_word} of ${it.count}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 更新云端最新章节信息并缓存到本地
     */
    suspend fun updateVolumes(setProgress: (Int) -> Unit, whenFinish: suspend () -> Unit) = withContext(Dispatchers.IO) withIO@ {
        var isDownload = false
        var volumes = if(loadCache && loadVolumes()) mVolumes else emptyArray<VolumeStructure>()
        if(mGroupPathWords.isEmpty()) return@withIO
        if(volumes.isEmpty()) {
            isDownload = true
            val delta = 100/mGroupPathWords.size
            mGroupPathWords.forEachIndexed { i, g ->
                Volume(path, g, getString, { return@Volume exit }) { p ->
                    setProgress(i*delta+100*p/delta)
                }.updateChapters(mCounts[i])?.let {
                    volumes += it
                }
            }
        }
        if (!exit && volumes.size == mGroupPathWords.size) {
            if(isDownload) {
                saveVolumes(volumes)
                mVolumes = volumes
            }
            goSaveHead(isDownload)
            setProgress(100)
            whenFinish()
        }
    }

    private suspend fun saveVolumes(volumes: Array<VolumeStructure>) = withContext(Dispatchers.IO) {
        name?.let { name ->
            val mangaFolder = File(exDir, name)
            if(!mangaFolder.exists()) mangaFolder.mkdirs()
            mJsonString = Gson().toJson(volumes)
            File(mangaFolder, "info.json").writeText(mJsonString)
            File(mangaFolder, "grps.json").writeText(Gson().toJson(mKeys))
        }
    }

    private fun goSaveHead(force: Boolean) {
        name?.let { name ->
            val mangaFolder = File(exDir, name)
            if(!mangaFolder.exists()) mangaFolder.mkdirs()
            val f = File(mangaFolder, "head.jpg")
            if(force || !f.exists()) (cover?.let { Config.imageProxy?.wrap(it) } ?:cover)?.let {
                Thread {
                    DownloadTools.getHttpContent(it, -1)?.let { data ->
                        f.writeBytes(data)
                    }
                }.start()
            }
        }
    }

    private suspend fun loadVolumes(): Boolean = withContext(Dispatchers.IO) {
        name?.let { name ->
            val mangaFolder = File(exDir, name)
            if(!mangaFolder.exists()) mangaFolder.mkdirs()
            val jsonFile = File(mangaFolder, "info.json")
            if (!jsonFile.exists()) return@let false
            mJsonString = jsonFile.readText()
            mVolumes = Gson().fromJson(mJsonString, Array<VolumeStructure>::class.java)
            val groupFile = File(mangaFolder, "grps.json")
            if (!groupFile.exists()) return@let false
            groupFile.inputStream().use {
                mKeys = Gson().fromJson(it.reader(), Array<String>::class.java)
            }
            return@let true
        }?:false
    }

    private suspend fun saveInfo(data: ByteArray) = withContext(Dispatchers.IO) {
        name?.let { name ->
            val mangaFolder = File(exDir, name)
            if(!mangaFolder.exists()) mangaFolder.mkdirs()
            File(mangaFolder, "meta.json").writeBytes(data)
        }
    }

    private suspend fun loadInfo(name: String): ByteArray? = withContext(Dispatchers.IO) {
        val mangaFolder = File(exDir, name)
        if(!mangaFolder.exists()) mangaFolder.mkdirs()
        val f = File(mangaFolder, "meta.json")
        if (!f.exists()) return@withContext null
        return@withContext f.readBytes()
    }
}
