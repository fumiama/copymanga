package top.fumiama.copymanga

import android.Manifest
import android.annotation.SuppressLint
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
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.yalantis.ucrop.UCrop
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.nav_header_main.*
import top.fumiama.copymanga.manga.Shelf
import top.fumiama.copymanga.tools.api.CMApi
import top.fumiama.dmzj.copymanga.R
import top.fumiama.copymanga.tools.api.UITools
import top.fumiama.copymanga.ui.book.BookFragment.Companion.bookHandler
import top.fumiama.copymanga.ui.cardflow.rank.RankFragment
import top.fumiama.copymanga.ui.comicdl.ComicDlFragment
import top.fumiama.copymanga.ui.download.DownloadFragment
import top.fumiama.copymanga.ui.download.NewDownloadFragment
import top.fumiama.copymanga.update.Update
import top.fumiama.copymanga.user.Member
import top.fumiama.dmzj.copymanga.BuildConfig
import java.io.File
import java.io.FileInputStream
import java.lang.Thread.sleep
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {
    var isDrawerClosed = true
    private var menuMain: Menu? = null
    private var navController: NavController? = null

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var headPic: File
    private lateinit var toolsBox: UITools

    private var latestDestination = 0
    private var isMenuWaiting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainWeakReference = WeakReference(this)
        toolsBox = UITools(this)

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        //translucentStatusBar()
        //coordiv.layoutParams.height = getStatusBarHeight()

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
                DownloadFragment.currentDir = getExternalFilesDir("")
                refreshUserInfo()
            }

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerStateChanged(newState: Int) {}
        })
        checkUpdate(false)

        ime = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        navController!!.addOnDestinationChangedListener { _, destination, _ ->
            latestDestination = destination.id
            Log.d("MyMA", "latestDestination: $latestDestination")
            if (isMenuWaiting) {
                return@addOnDestinationChangedListener
            }
            isMenuWaiting = true
            Log.d("MyMA", "start menu waiting")
            Thread {
                sleep(1000)
                isMenuWaiting = false
                Log.d("MyMA", "finish menu waiting")
                runOnUiThread {
                    when (latestDestination) {
                        R.id.nav_home -> {
                            Log.d("MyMA", "enter home")
                            menuMain?.findItem(R.id.action_info)?.isVisible = true
                            menuMain?.findItem(R.id.action_download)?.isVisible = false
                            menuMain?.findItem(R.id.action_sort)?.isVisible = false
                        }
                        R.id.nav_book -> {
                            Log.d("MyMA", "enter book")
                            menuMain?.findItem(R.id.action_info)?.isVisible = false
                            menuMain?.findItem(R.id.action_download)?.isVisible = true
                            menuMain?.findItem(R.id.action_sort)?.isVisible = false
                        }
                        R.id.nav_group -> {
                            Log.d("MyMA", "enter group")
                            menuMain?.findItem(R.id.action_info)?.isVisible = false
                            menuMain?.findItem(R.id.action_download)?.isVisible = false
                            menuMain?.findItem(R.id.action_sort)?.isVisible = true
                        }
                        R.id.nav_new_download -> {
                            Log.d("MyMA", "enter new_download")
                            menuMain?.findItem(R.id.action_info)?.isVisible = false
                            menuMain?.findItem(R.id.action_download)?.isVisible = false
                            menuMain?.findItem(R.id.action_sort)?.isVisible = true
                        }
                        R.id.nav_rank -> {
                            Log.d("MyMA", "enter rank")
                            menuMain?.findItem(R.id.action_info)?.isVisible = false
                            menuMain?.findItem(R.id.action_download)?.isVisible = false
                            menuMain?.findItem(R.id.action_sort)?.isVisible = true
                        }
                        else -> {
                            Log.d("MyMA", "enter others")
                            menuMain?.findItem(R.id.action_info)?.isVisible = false
                            menuMain?.findItem(R.id.action_download)?.isVisible = false
                            menuMain?.findItem(R.id.action_sort)?.isVisible = false
                        }
                    }
                }
            }.start()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        menuMain = menu
        return true
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_info -> {
                showAbout()
                true
            }
            R.id.action_download -> {
                bookHandler?.sendEmptyMessage(6)
                true
            }
            R.id.action_sort -> {
                ComicDlFragment.handler?.sendEmptyMessage(13)
                NewDownloadFragment.wn?.get()?.showReverseInfo(toolsBox)
                RankFragment.wr?.get()?.showSexInfo(toolsBox)
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
        navtinfo.text = getPreferences(MODE_PRIVATE).getString("navTextInfo", getString(R.string.navTextInfo))
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) when (requestCode) {
            UCrop.REQUEST_CROP -> {
                val fi = headPic.inputStream()
                navhbg.setImageBitmap(BitmapFactory.decodeStream(fi))
                fi.close()
            }
            MSG_CROP_IMAGE -> {
                data?.data?.let {
                    saveFile(it)
                    cropImageUri()
                }
            }
        }
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

    fun refreshUserInfo() {
        getPreferences(MODE_PRIVATE)?.apply {
            val name = getString("nickname", getString("username", ""))
            val avatar = getString("avatar", "")
            if(name != "") navttitle.text = name
            else navttitle.setText(R.string.noLogin)
            if(avatar != "")
                Glide.with(this@MainActivity).load(avatar)
                    .apply(RequestOptions.bitmapTransform(CircleCrop()))
                    .into(navhicon)
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

    @SuppressLint("IntentReset")
    private fun pickPicture() {
        val i = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        i.type = "image/*"
        startActivityForResult(i, MSG_CROP_IMAGE)
    }

    private fun saveFile(uri: Uri) {
        //val f = File(getExternalFilesDir(""), "headPic")
        val fd = contentResolver.openFileDescriptor(uri, "r")
        fd?.fileDescriptor?.let {
            val fi = FileInputStream(it)
            val fo = headPic.outputStream()
            fi.copyTo(fo)
            fi.close()
            fo.close()
        }
        fd?.close()
    }

    private fun checkHeadPicture() {
        //val hp = File(getExternalFilesDir(""), "headPic")
        if (headPic.exists()) navhbg.setImageURI(headPic.toUri())
    }

    private fun cropImageUri() {
        val op = UCrop.Options()
        val r = navhbg.width.toFloat() / navhbg.height.toFloat()
        Log.d("MyMain", "Img info: (${navhbg.width}, ${navhbg.height})")
        Log.d("MyMain", "Result code: ${UCrop.REQUEST_CROP}")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            op.setCompressionFormat(Bitmap.CompressFormat.WEBP_LOSSY)
        } else {
            op.setCompressionFormat(Bitmap.CompressFormat.WEBP)
        }
        op.setStatusBarColor(resources.getColor(R.color.colorPrimaryDark, theme))
        op.setToolbarColor(resources.getColor(R.color.colorPrimary, theme))
        op.setActiveControlsWidgetColor(resources.getColor(R.color.colorAccent, theme))
        UCrop.of(headPic.toUri(), headPic.toUri())
            .withAspectRatio(r, 1F)
            .withMaxResultSize(navhbg.width, navhbg.height)
            .withOptions(op)
            .start(this)
    }

    /*private fun translucentStatusBar() {
        //添加Flag把状态栏设为可绘制模式
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        //如果为全透明模式，取消设置Window半透明的Flag
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        //设置状态栏为透明
        window.statusBarColor = Color.TRANSPARENT
        //设置window的状态栏不可见
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        //view不根据系统窗口来调整自己的布局
        val mContentView: ViewGroup = window.findViewById(Window.ID_ANDROID_CONTENT) as ViewGroup
        val mChildView: View = mContentView.getChildAt(0)
        ViewCompat.requestApplyInsets(mChildView)

        coordiv.layoutParams.height = getStatusBarHeight()
    }*/

    /*private fun getStatusBarHeight() =
        resources.getDimensionPixelOffset(
            resources.getIdentifier(
                "status_bar_height",
                "dimen",
                "android"
            )
        )*/

    private fun checkUpdate(ignoreSkip: Boolean) {
        Thread{
            Update.checkUpdate(this, toolsBox, ignoreSkip)
        }.start()
    }

    private fun showAbout() {
        val dl = android.app.AlertDialog.Builder(this)
        dl.setMessage(R.string.app_description)
        dl.setTitle("${getString(R.string.action_info)} ${BuildConfig.VERSION_NAME}")
        dl.setIcon(R.mipmap.ic_launcher)
        dl.setPositiveButton(android.R.string.ok) { _, _ -> }
        dl.setNeutralButton(R.string.check_update) {_, _ ->
            checkUpdate(true)
        }
        dl.show()
    }

    @SuppressLint("CheckResult")
    fun onNavTInfoClicked(it: View) {
        MaterialDialog(this).show {
            input(prefill = (it as TextView).text) { _, charSequence ->
                it.text = charSequence
                getPreferences(MODE_PRIVATE).edit {
                    putString("navTextInfo", charSequence.toString())
                    apply()
                }
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

    companion object{
        var mainWeakReference: WeakReference<MainActivity>? = null
        var ime: InputMethodManager? = null
        const val MSG_CROP_IMAGE = 1
        var shelf: Shelf? = null
            get() {
                if (field != null) return field
                return mainWeakReference?.get()?.let {
                    field = Shelf(
                        it.getPreferences(Context.MODE_PRIVATE)
                            .getString("token", "")?:return@let null) { id ->
                        return@Shelf it.getString(id)
                    }
                    field
                }
            }
        var member: Member? = null
            get() {
                if (field != null) return field
                return mainWeakReference?.get()?.let {
                    it.getPreferences(MODE_PRIVATE)?.let { pref ->
                        field = Member(pref) { id ->
                            return@Member it.getString(id)
                        }
                    }
                    field
                }
            }
    }
}