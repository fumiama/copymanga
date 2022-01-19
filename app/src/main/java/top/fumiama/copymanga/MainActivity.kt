package top.fumiama.copymanga

import android.Manifest
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
import com.yalantis.ucrop.UCrop
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.nav_header_main.*
import top.fumiama.dmzj.copymanga.R
import top.fumiama.copymanga.tools.api.UITools
import top.fumiama.copymanga.ui.download.DownloadFragment
import top.fumiama.copymanga.update.Update
import java.io.File
import java.io.FileInputStream
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {
    var isDrawerClosed = true
    var menuMain: Menu? = null
    var navController: NavController? = null

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var headPic: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        //translucentStatusBar()
        coordiv.layoutParams.height = getStatusBarHeight()

        navController = findNavController(R.id.nav_host_fragment)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home,
                R.id.nav_sort,
                R.id.nav_rank,
                R.id.nav_sub,
                R.id.nav_history,
                R.id.nav_download,
                R.id.nav_settings
            ), drawer_layout
        )
        setupActionBarWithNavController(navController!!, appBarConfiguration)
        nav_view.setupWithNavController(navController!!)

        headPic = File(getExternalFilesDir(""), "headPic")
        mainWeakReference = WeakReference(this)
        drawer_layout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerClosed(drawerView: View) {
                Log.d("MyMain", "onDrawerClosed")
                isDrawerClosed = true
            }

            override fun onDrawerOpened(drawerView: View) {
                Log.d("MyMain", "onDrawerOpened")
                isDrawerClosed = false
                DownloadFragment.currentDir = getExternalFilesDir("")
            }

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerStateChanged(newState: Int) {}
        })
        checkUpdate(false)

        ime = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        menuMain = menu
        return true
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) when (requestCode) {
            UCrop.REQUEST_CROP -> {
                val fi = headPic.inputStream()
                navhbg.setImageBitmap(BitmapFactory.decodeStream(fi))
                fi.close()
            }
            1 -> {
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
            1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) pickPicture()
                else Toast.makeText(this, R.string.permissionDenied, Toast.LENGTH_SHORT).show()
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
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1
            )
            false
        } else true
    }

    private fun pickPicture() {
        val i = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        i.type = "image/*"
        startActivityForResult(i, 1)
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
        op.setCompressionFormat(Bitmap.CompressFormat.WEBP)
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

    private fun getStatusBarHeight() =
        resources.getDimensionPixelOffset(
            resources.getIdentifier(
                "status_bar_height",
                "dimen",
                "android"
            )
        )

    private fun checkUpdate(ignoreSkip: Boolean) {
        Thread{
            Update.checkUpdate(this, UITools(this), ignoreSkip)
        }.start()
    }

    fun showAbout(item: MenuItem) {
        val dl = android.app.AlertDialog.Builder(this)
        dl.setMessage(R.string.app_description)
        dl.setTitle(R.string.action_info)
        dl.setIcon(R.mipmap.ic_launcher)
        dl.setPositiveButton(android.R.string.ok) { _, _ -> }
        dl.setNeutralButton(R.string.check_update) {_, _ ->
            checkUpdate(true)
        }
        dl.show()
    }

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
    }
}