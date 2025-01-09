package top.fumiama.copymanga.ui.book

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.card_book.*
import kotlinx.android.synthetic.main.fragment_book.*
import kotlinx.android.synthetic.main.line_booktandb.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.fumiama.copymanga.MainActivity
import top.fumiama.copymanga.manga.Book
import top.fumiama.copymanga.manga.Reader
import top.fumiama.copymanga.template.general.NoBackRefreshFragment
import top.fumiama.copymanga.tools.ui.Navigate
import top.fumiama.copymanga.ui.comicdl.ComicDlFragment
import top.fumiama.dmzj.copymanga.R
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicReference

class BookFragment: NoBackRefreshFragment(R.layout.fragment_book) {
    var isOnPause = false
    var book: Book? = null
    private var mBookHandler: BookHandler? = null


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ComicDlFragment.exit = false
        fbvp?.setPadding(0, 0, 0, navBarHeight)

        if(isFirstInflate) {
            lifecycleScope.launch {
                prepareHandler()
                try {
                    fbloading?.apply {
                        post {
                            progress = 0
                            invalidate()
                        }
                    }
                    book?.updateInfo()
                } catch (e: Exception) {
                    e.printStackTrace()
                    if(mBookHandler?.exit != false) return@launch
                    Toast.makeText(context, R.string.null_book, Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                    return@launch
                }
                Log.d("MyBF", "read path: ${book?.path}")
                for (i in 1..3) {
                    mBookHandler?.sendEmptyMessage(i)
                }
                try {
                    fbc?.apply {
                        alpha = 0f
                        visibility = View.VISIBLE
                        invalidate()
                        ObjectAnimator.ofFloat(this, "alpha", 0f, 1f)
                            .setDuration(300)
                            .start()
                    }
                    book?.updateVolumes({ fbloading?.apply { post {
                        val oa = ObjectAnimator.ofInt(this, "progress", 20 + it*8/10)
                            .setDuration(128)
                        oa.addUpdateListener { invalidate() }
                        oa.start()
                        Log.d("MyBF", "set progress $it")
                    } } }) {
                        mBookHandler?.obtainMessage(BookHandler.SET_VOLUMES, book?.version?:2, 0)?.sendToTarget()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    if(mBookHandler?.exit != false) return@launch
                    Toast.makeText(context, R.string.null_volume, Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                    return@launch
                }
            }
        } else {
            bookHandler.set(mBookHandler)
        }
    }

    override fun onResume() {
        super.onResume()
        isOnPause = false
        bookHandler.set(mBookHandler)
        activity?.apply {
            toolbar.title = book?.name
        }
        setStartRead()
    }

    override fun onPause() {
        super.onPause()
        isOnPause = true
    }

    override fun onDestroy() {
        super.onDestroy()
        mBookHandler?.exit = true
        book?.exit = true
        bookHandler.set(null)
    }

    fun setStartRead() {
        if(mBookHandler?.chapterNames?.isNotEmpty() != true) return
        activity?.apply {
            book?.name?.let { name ->
                getPreferences(MODE_PRIVATE).getInt(name, -1).let { p ->
                    this@BookFragment.lbbstart.apply {
                        var i = 0
                        if(p >= 0) mBookHandler!!.chapterNames.let {
                            i = if (p >= it.size) it.size-1 else p
                            text = it[i]
                        }
                        setOnClickListener {
                            mBookHandler?.apply {
                                Reader.start2viewManga(name, i, urlArray, uuidArray)
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun prepareHandler() = withContext(Dispatchers.IO) {
        arguments?.apply {
            if (getBoolean("loadJson")) {
                getString("name")?.let { name ->
                    try {
                        book = Book(name, {
                            return@Book getString(it)
                        }, activity?.getExternalFilesDir("")!!)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, R.string.null_book, Toast.LENGTH_SHORT).show()
                            findNavController().popBackStack()
                        }
                        return@withContext
                    }
                }
            } else getString("path").let {
                if (it != null) book = Book(it, { id ->
                    return@Book getString(id)
                }, activity?.getExternalFilesDir("")!!, false)
                else {
                    withContext(Dispatchers.Main) {
                        findNavController().popBackStack()
                    }
                    return@withContext
                }
            }
        }
        withContext(Dispatchers.Main) {
            mBookHandler = BookHandler(WeakReference(this@BookFragment))
            bookHandler.set(mBookHandler)
        }
    }

    private suspend fun queryCollect() {
        MainActivity.shelf?.query(book?.path!!)?.let { b ->
            mBookHandler?.collect = b.results?.collect?:-2
            Log.d("MyBF", "get collect of ${book?.path} = ${mBookHandler?.collect}")
            tic.text = b.results?.browse?.chapter_name?.let { name ->
                getString(R.string.text_format_cloud_read_to).format(name)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    fun setAddToShelf() {
        if(mBookHandler?.chapterNames?.isNotEmpty() != true) return
        lifecycleScope.launch {
            queryCollect()
            mBookHandler?.collect?.let { collect ->
                if (collect > 0) {
                    this@BookFragment.lbbsub.setText(R.string.button_sub_subscribed)
                }
            }
            book?.uuid?.let { uuid ->
                this@BookFragment.lbbsub.setOnClickListener {
                    lifecycleScope.launch clickLaunch@ {
                        if (this@BookFragment.lbbsub.text != getString(R.string.button_sub)) {
                            mBookHandler?.collect?.let { collect ->
                                if (collect < 0) return@clickLaunch
                                val re = MainActivity.shelf?.del(collect)
                                Toast.makeText(context, re, Toast.LENGTH_SHORT).show()
                                if (re == "请求成功") {
                                    this@BookFragment.lbbsub.setText(R.string.button_sub)
                                }
                            }
                            return@clickLaunch
                        }
                        val re = MainActivity.shelf?.add(uuid)
                        Toast.makeText(context, re, Toast.LENGTH_SHORT).show()
                        if (re == "修改成功") {
                            queryCollect()
                            this@BookFragment.lbbsub.setText(R.string.button_sub_subscribed)
                        }
                    }
                }
            }
        }
    }

    fun navigate2dl() {
        val bundle = Bundle()
        Log.d("MyBF", "nav2: ${arguments?.getString("path")?:"null"}")
        bundle.putString("path", arguments?.getString("path")?:"null")
        bundle.putString("name", book!!.name!!)
        if(book?.volumes != null && book?.json != null) {
            bundle.putString("loadJson", book!!.json)
        }
        bundle.putInt("version", book?.version?:2)
        findNavController().let {
            Navigate.safeNavigateTo(it, R.id.action_nav_book_to_nav_group, bundle)
        }
    }

    companion object {
        var bookHandler: AtomicReference<BookHandler?> = AtomicReference(null)
    }
}
