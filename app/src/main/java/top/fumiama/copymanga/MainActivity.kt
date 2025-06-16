package top.fumiama.copymanga

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.yalantis.ucrop.UCrop
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.nav_header_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.fumiama.copymanga.api.Config
import top.fumiama.copymanga.api.manga.Shelf
import top.fumiama.copymanga.view.interaction.UITools
import top.fumiama.copymanga.ui.book.BookFragment.Companion.bookHandler
import top.fumiama.copymanga.ui.book.BookHandler
import top.fumiama.copymanga.ui.cardflow.rank.RankFragment
import top.fumiama.copymanga.ui.comicdl.ComicDlFragment
import top.fumiama.copymanga.ui.download.DownloadFragment
import top.fumiama.copymanga.ui.download.NewDownloadFragment
import top.fumiama.copymanga.api.update.Update
import top.fumiama.copymanga.api.user.Member
import top.fumiama.copymanga.lib.Comancry
import top.fumiama.copymanga.lib.Comandy
import top.fumiama.copymanga.storage.ConfigLoader
import top.fumiama.copymanga.strings.Base16384
import top.fumiama.dmzj.copymanga.BuildConfig
import top.fumiama.dmzj.copymanga.R
import java.io.File
import java.io.FileInputStream
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {
    private var menuMain: Menu? = null
    private var navController: NavController? = null

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var headPic: File
    lateinit var toolsBox: UITools

    private var isMenuWaiting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(null)

        val darkModePrefKey = getString(R.string.darkModeKeyID)
        PreferenceManager.getDefaultSharedPreferences(this)?.apply {
            if (contains(darkModePrefKey)) getString(darkModePrefKey, "0")?.toInt()?.let {
                if (it > 0) {
                    AppCompatDelegate.setDefaultNightMode(listOf(
                        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
                        AppCompatDelegate.MODE_NIGHT_NO,
                        AppCompatDelegate.MODE_NIGHT_YES,
                    )[it])
                }
            }
        }

        // must init before setContentView because HomeF need them to init
        mainWeakReference = WeakReference(this)
        toolsBox = UITools(this)

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        coordiv.layoutParams.height = UITools.getStatusBarHeight(this)

        navController = findNavController(R.id.nav_host_fragment)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home,
                R.id.nav_sort,
                R.id.nav_rank,
                R.id.nav_sub,
                R.id.nav_history,
                R.id.nav_new_download,
                R.id.nav_settings
            ), drawer_layout
        )
        setupActionBarWithNavController(navController!!, appBarConfiguration)
        nav_view.setupWithNavController(navController!!)

        headPic = File(getExternalFilesDir(""), "headPic")
        drawer_layout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerClosed(drawerView: View) {
                Log.d("MyMain", "onDrawerClosed")
                isDrawerClosed = true
            }

            override fun onDrawerOpened(drawerView: View) {
                Log.d("MyMain", "onDrawerOpened")
                isDrawerClosed = false
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        refreshUserInfo()
                    }
                }
            }

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerStateChanged(newState: Int) {}
        })
        goCheckUpdate(false)

        ime = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        var latestDestination: Int
        navController!!.addOnDestinationChangedListener { controller, destination, _ ->
            latestDestination = destination.id
            Log.d("MyMain", "latestDestination: ${destination.label}")
            if (isMenuWaiting) {
                return@addOnDestinationChangedListener
            }
            if (latestDestination !in appBarConfiguration.topLevelDestinations) {
                drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            } else {
                drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            }
            isMenuWaiting = true
            Log.d("MyMain", "start menu waiting")
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    Config.api.init()
                }
                withContext(Dispatchers.IO) {
                    delay(1000)
                    withContext(Dispatchers.Main) {
                        isMenuWaiting = false
                        Log.d("MyMain", "finish menu waiting")
                        changeMenuList(latestDestination)
                    }
                }
            }
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        PreferenceManager.getDefaultSharedPreferences(this)?.apply {
            if (contains("settings_cat_general_sb_startup_menu")) getString("settings_cat_general_sb_startup_menu", "0")?.toInt()?.let {
                if (it > 0) {
                    Log.d("MyMain", "nav 2 dest $it")
                    navController!!.navigate(listOf(
                        R.id.nav_home,
                        R.id.nav_sort,
                        R.id.nav_rank,
                        R.id.nav_sub,
                        R.id.nav_history,
                        R.id.nav_new_download,
                        R.id.nav_settings
                    )[it])
                }
            }
        }
        if (Config.general_enable_transparent_system_bar.value) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
            window.statusBarColor = 0
            window.navigationBarColor = 0
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        menuMain = menu
        return true
    }

    @SuppressLint("CheckResult")
    @OptIn(ExperimentalStdlibApi::class)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_info -> {
                showAbout()
                true
            }
            R.id.action_download -> {
                when (navController?.currentDestination?.id) {
                    R.id.nav_new_download -> {
                        //TODO: fill it
                    }
                    R.id.nav_settings -> {
                        toolsBox.buildInfo("备份管理", "可选择导出或导入base16384格式配置项",
                            "导出", "导入", "取消", { // ok
                                MaterialDialog(this).show {
                                    input(prefill = Base16384.encode(ConfigLoader().toByteArray()))
                                    positiveButton(android.R.string.ok)
                                    title(null, "请复制配置文本并保存")
                                }
                            }, { // neutral
                                MaterialDialog(this).show {
                                    input { _, c ->
                                        try {
                                            ConfigLoader(Base16384.decode(c.toString())).settings.export()
                                            navController?.apply {
                                                currentDestination?.id?.let {
                                                    popBackStack()
                                                    navigate(it)
                                                }
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(this@MainActivity, e.message?:e::class.simpleName, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    positiveButton(android.R.string.ok)
                                    title(null, "请粘贴配置文本")
                                }
                            })
                    }
                    else -> bookHandler.get()?.sendEmptyMessage(BookHandler.NAVIGATE_TO_DOWNLOAD)
                }
                true
            }
            R.id.action_sort -> {
                ComicDlFragment.handler?.sendEmptyMessage(13)
                NewDownloadFragment.wn?.get()?.showReverseInfo(toolsBox)
                RankFragment.wr?.get()?.showSexInfo(toolsBox)
                true
            }
            R.id.action_del -> {
                if (DownloadFragment.wd != null) {
                    val dl = AlertDialog.Builder(this)
                    dl.setMessage(R.string.delele_all_empty_manga)
                    dl.setPositiveButton(android.R.string.ok) { _, _ ->
                        DownloadFragment.wd?.get()?.removeAllEmpty()
                    }
                    dl.show()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)

        checkHeadPicture()
        if (headPic.exists()) navhbg.setOnLongClickListener {
            if (headPic.exists()) {
                val dl = AlertDialog.Builder(this)
                dl.setMessage(R.string.clearHeadImgMsg)
                dl.setPositiveButton(android.R.string.ok) { _, _ ->
                    if (headPic.exists()) headPic.delete()
                    navhbg.setImageResource(R.drawable.illust_57793944_20190427_134853)
                }
                dl.show()
            }
            true
        }
        navtinfo.text = Config.navTextInfo.value
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MSG_CROP_IMAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) pickPicture()
                else Toast.makeText(this, R.string.permissionDenied, Toast.LENGTH_SHORT).show()
            }
        }
    }

    suspend fun refreshUserInfo(): Unit = withContext(Dispatchers.IO) {
        var name = Config.nickname.value
        if (name.isNullOrEmpty()) name = Config.username.value
        val avatar = Config.avatar.value
        navttitle.apply { post {
            if(!name.isNullOrEmpty()) text = name
            else setText(R.string.noLogin)
        } }
        navhicon.apply ic@ { post {
            if(!avatar.isNullOrEmpty())
                Glide.with(this@MainActivity).load(avatar)
                    .apply(RequestOptions.bitmapTransform(CircleCrop()))
                    .timeout(60000)
                    .into(this@ic)
            else setImageResource(R.mipmap.ic_launcher)
        } }
    }

    private fun changeMenuList(latestDestination: Int) {
        when (latestDestination) {
            R.id.nav_home -> {
                Log.d("MyMain", "enter home")
                menuMain?.findItem(R.id.action_info)?.isVisible = true
                menuMain?.findItem(R.id.action_download)?.isVisible = false
                menuMain?.findItem(R.id.action_sort)?.isVisible = false
                menuMain?.findItem(R.id.action_del)?.isVisible = false
            }
            R.id.nav_book -> {
                Log.d("MyMain", "enter book")
                menuMain?.findItem(R.id.action_info)?.isVisible = false
                menuMain?.findItem(R.id.action_download)?.isVisible = true
                menuMain?.findItem(R.id.action_sort)?.isVisible = false
                menuMain?.findItem(R.id.action_del)?.isVisible = false
            }
            R.id.nav_group -> {
                Log.d("MyMain", "enter group")
                menuMain?.findItem(R.id.action_info)?.isVisible = false
                menuMain?.findItem(R.id.action_download)?.isVisible = false
                menuMain?.findItem(R.id.action_sort)?.isVisible = true
                menuMain?.findItem(R.id.action_del)?.isVisible = false
            }
            R.id.nav_new_download -> {
                Log.d("MyMain", "enter new_download")
                menuMain?.findItem(R.id.action_info)?.isVisible = false
                menuMain?.findItem(R.id.action_download)?.isVisible = false
                menuMain?.findItem(R.id.action_sort)?.isVisible = true
                menuMain?.findItem(R.id.action_del)?.isVisible = false
            }
            R.id.nav_rank -> {
                Log.d("MyMain", "enter rank")
                menuMain?.findItem(R.id.action_info)?.isVisible = false
                menuMain?.findItem(R.id.action_download)?.isVisible = false
                menuMain?.findItem(R.id.action_sort)?.isVisible = true
                menuMain?.findItem(R.id.action_del)?.isVisible = false
            }
            R.id.nav_download -> {
                Log.d("MyMain", "enter old download")
                menuMain?.findItem(R.id.action_info)?.isVisible = false
                menuMain?.findItem(R.id.action_download)?.isVisible = false
                menuMain?.findItem(R.id.action_sort)?.isVisible = false
                menuMain?.findItem(R.id.action_del)?.isVisible = true
            }
            R.id.nav_settings -> {
                Log.d("MyMain", "enter settings")
                menuMain?.findItem(R.id.action_info)?.isVisible = false
                menuMain?.findItem(R.id.action_download)?.isVisible = true
                menuMain?.findItem(R.id.action_sort)?.isVisible = false
                menuMain?.findItem(R.id.action_del)?.isVisible = false
            }
            else -> {
                Log.d("MyMain", "enter others")
                menuMain?.findItem(R.id.action_info)?.isVisible = false
                menuMain?.findItem(R.id.action_download)?.isVisible = false
                menuMain?.findItem(R.id.action_sort)?.isVisible = false
                menuMain?.findItem(R.id.action_del)?.isVisible = false
            }
        }
    }

    private fun checkReadPermission(): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), MSG_CROP_IMAGE
            )
            false
        } else true
    }

    private var pickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) result.data?.data?.let {
            saveFile(it)
            cropImageUri()
        } else Toast.makeText(this, R.string.err_pick_img, Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("IntentReset")
    private fun pickPicture() {
        val i = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        i.type = "image/*"
        pickerLauncher.launch(i)
    }

    private fun saveFile(uri: Uri) {
        contentResolver.openFileDescriptor(uri, "r")?.use {
            it.fileDescriptor?.let { fd ->
                FileInputStream(fd).use { fi ->
                    headPic.outputStream().use { fo ->
                        fi.copyTo(fo)
                    }
                }
            }
        }
    }

    private fun checkHeadPicture() {
        //val hp = File(getExternalFilesDir(""), "headPic")
        if (headPic.exists()) navhbg.setImageURI(headPic.toUri())
    }

    private var cropLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            headPic.inputStream().use { fi ->
                navhbg.setImageBitmap(BitmapFactory.decodeStream(fi))
            }
        } else Toast.makeText(this, R.string.err_crop_img, Toast.LENGTH_SHORT).show()
    }

    private fun cropImageUri() {
        val op = UCrop.Options()
        val r = navhbg.width.toFloat() / navhbg.height.toFloat()
        Log.d("MyMain", "Img info: (${navhbg.width}, ${navhbg.height})")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            op.setCompressionFormat(Bitmap.CompressFormat.WEBP_LOSSY)
        } else {
            op.setCompressionFormat(Bitmap.CompressFormat.WEBP)
        }
        op.setStatusBarColor(resources.getColor(R.color.colorPrimaryDark, theme))
        op.setToolbarColor(resources.getColor(R.color.colorPrimary, theme))
        op.setActiveControlsWidgetColor(resources.getColor(R.color.colorAccent, theme))
        cropLauncher.launch(UCrop.of(headPic.toUri(), headPic.toUri())
            .withAspectRatio(r, 1F)
            .withMaxResultSize(navhbg.width, navhbg.height)
            .withOptions(op)
            .getIntent(this))
    }

    private fun goCheckUpdate(ignoreSkip: Boolean) {
        lifecycleScope.launch {
            Update.checkUpdate(this@MainActivity, toolsBox, ignoreSkip)
        }
    }

    private fun showAbout() {
        val dl = android.app.AlertDialog.Builder(this)
        val comandy = "网络增强: ${Comandy.instance.status}, 版本 ${Config.comandy_version.value}"
        val comancry = "API代理: ${Comancry.instance.status}, 版本 ${Config.comancry_version.value}"
        dl.setMessage("${getString(R.string.app_description)}\n" +
                "\n$comandy\n" +
                "$comancry\n\n"+ File("/proc/self/cmdline").readText() + "\n" +
                "当前API: ${Config.api.getApis().joinToString(", ")}")
        dl.setTitle("${getString(R.string.action_info)} ${BuildConfig.VERSION_NAME}")
        dl.setIcon(R.mipmap.ic_launcher)
        dl.setPositiveButton(android.R.string.ok) { _, _ -> }
        dl.setNeutralButton(R.string.check_update) {_, _ ->
            goCheckUpdate(true)
        }
        dl.show()
    }

    @SuppressLint("CheckResult")
    fun onNavTInfoClicked(it: View) {
        MaterialDialog(this).show {
            input(prefill = (it as TextView).text) { _, charSequence ->
                it.text = charSequence
                Config.navTextInfo.value = charSequence.toString()
            }
            positiveButton(android.R.string.ok)
            title(R.string.navTextInfoInputHint)
        }
    }

    fun onNavHBgClicked(v: View) {
        if (checkReadPermission()) pickPicture()
    }

    fun startLoginActivity(v: View){
        startActivity(Intent(this, LoginActivity::class.java))
    }

    companion object {
        var mainWeakReference: WeakReference<MainActivity>? = null
        var isDrawerClosed = true
        var ime: InputMethodManager? = null
        const val MSG_CROP_IMAGE = 1
        var shelf: Shelf? = null
            get() {
                if (field != null) return field
                return mainWeakReference?.get()?.let {
                    field = Shelf { id ->
                        return@Shelf it.getString(id)
                    }
                    field
                }
            }
        var member: Member? = null
            get() {
                if (field != null) return field
                return mainWeakReference?.get()?.let {
                    field = Member { id ->
                        return@Member it.getString(id)
                    }
                    field
                }
            }
    }
}