package top.fumiama.copymanga.ui.home

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.lapism.search.internal.SearchLayout
import kotlinx.android.synthetic.main.card_book_plain.view.*
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.line_word.view.*
import kotlinx.android.synthetic.main.viewpage_horizonal.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.fumiama.copymanga.MainActivity
import top.fumiama.copymanga.MainActivity.Companion.ime
import top.fumiama.copymanga.api.Config
import top.fumiama.copymanga.json.BookListStructure
import top.fumiama.copymanga.net.template.PausableDownloader
import top.fumiama.copymanga.view.interaction.Navigate
import top.fumiama.copymanga.view.operation.GlideHideLottieViewListener
import top.fumiama.copymanga.view.template.NoBackRefreshFragment
import top.fumiama.dmzj.copymanga.R
import java.lang.ref.WeakReference
import java.net.URLEncoder
import java.nio.charset.Charset

class HomeFragment : NoBackRefreshFragment(R.layout.fragment_home) {
    lateinit var homeHandler: HomeHandler

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if(isFirstInflate) {
            val tb = (activity as MainActivity).toolsBox
            val netInfo = tb.netInfo
            if(netInfo != tb.transportStringNull && netInfo != tb.transportStringError)
                MainActivity.member?.apply { lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        Config.api.init()
                        try {
                            info()
                        } catch (e: Exception) {
                            Snackbar
                                .make(view, "${e::class.simpleName} ${e.message}", Snackbar.LENGTH_LONG)
                                .setTextMaxLines(10)
                                .show()
                        }
                    }
                } }
            homeHandler = HomeHandler(WeakReference(this))

            val theme = resources.newTheme()
            swiperefresh?.setColorSchemeColors(
                resources.getColor(R.color.colorAccent, theme),
                resources.getColor(R.color.colorBlue2, theme),
                resources.getColor(R.color.colorGreen, theme))
            swiperefresh?.isEnabled = true

            fhl?.setPadding(0, 0, 0, navBarHeight)

            fhs?.apply {
                isNestedScrollingEnabled = true
                val recyclerView = findViewById<RecyclerView>(R.id.search_recycler_view)
                recyclerView.isNestedScrollingEnabled = true
                recyclerView.setPadding(0, 0, 0, navBarHeight)
                setAdapterLayoutManager(LinearLayoutManager(context))
                val adapter = ListViewHolder(recyclerView).RecyclerViewAdapter()
                setAdapter(adapter)
                navigationIconSupport = SearchLayout.NavigationIconSupport.SEARCH
                setMicIconImageResource(R.drawable.ic_setting_search)
                val micView = findViewById<ImageButton>(R.id.search_image_view_mic)
                setClearFocusOnBackPressed(true)
                setOnNavigationClickListener(object : SearchLayout.OnNavigationClickListener {
                    override fun onNavigationClick(hasFocus: Boolean) {
                        if (hasFocus()) {
                            clearFocus()
                        }
                        else requestFocus()
                    }
                })
                setTextHint(android.R.string.search_go)

                var lastSearch = ""
                setOnQueryTextListener(object : SearchLayout.OnQueryTextListener {
                    var lastChangeTime = 0L
                    override fun onQueryTextChange(newText: CharSequence): Boolean {
                        if (newText.contentEquals("__notice_focus_change__") || newText.contentEquals(lastSearch)) return true
                        lastSearch = newText.toString()
                        postDelayed({
                            lifecycleScope.launch {
                                if (!newText.contentEquals(lastSearch)) return@launch
                                val diff = System.currentTimeMillis() - lastChangeTime
                                if(diff > 500) {
                                    if (newText.isNotEmpty()) {
                                        Log.d("MyHF", "new text: $newText")
                                        adapter.refresh(newText)
                                    }
                                }
                            }
                        }, 1024)
                        lastChangeTime = System.currentTimeMillis()
                        return true
                    }

                    override fun onQueryTextSubmit(query: CharSequence): Boolean {
                        /*if(query.isNotEmpty()) {
                            val key = query.toString()
                            Toast.makeText(context, key, Toast.LENGTH_SHORT).show()
                        }*/
                        Log.d("MyHF", "recover text: $lastSearch")
                        setTextQuery(lastSearch, false)
                        return true
                    }
                })

                setOnMicClickListener(object : SearchLayout.OnMicClickListener {
                    val types = arrayOf("", "name", "author", "local")
                    var i = 0
                    override fun onMicClick() {
                        val typeNames = resources.getStringArray(R.array.search_types)
                        AlertDialog.Builder(context)
                            .setTitle(R.string.set_search_types)
                            .setIcon(R.mipmap.ic_launcher)
                            .setSingleChoiceItems(ArrayAdapter(context, R.layout.line_choice_list, typeNames), i){ d, p ->
                                adapter.type = types[p]
                                i = p
                                d.cancel()
                            }.show()
                    }
                })

                var isInFocusWaiting = false
                setOnFocusChangeListener(object : SearchLayout.OnFocusChangeListener {
                    override fun onFocusChange(hasFocus: Boolean) {
                        Log.d("MyHF", "fhs onFocusChange: $hasFocus")
                        if (isInFocusWaiting) return
                        isInFocusWaiting = true
                        postDelayed({
                            navigationIconSupport = if (hasFocus) {
                                setTextQuery("__notice_focus_change__", true)
                                SearchLayout.NavigationIconSupport.ARROW
                            }
                            else {
                                if (lastSearch.isNotEmpty()) {
                                    micView?.visibility = View.VISIBLE
                                }
                                SearchLayout.NavigationIconSupport.SEARCH
                            }
                            isInFocusWaiting = false
                        }, 300)
                    }
                })

                setOnTouchListener { _, e ->
                    Log.d("MyHF", "fhns on touch")
                    if (e.action == MotionEvent.ACTION_UP && mSearchEditText?.text?.isNotEmpty() == true) {
                        ime?.hideSoftInputFromWindow(activity?.window?.decorView?.windowToken, 0)
                    }
                    false
                }
            }

            lifecycleScope.launch{
                withContext(Dispatchers.IO) {
                    homeHandler.obtainMessage(-1, true).sendToTarget()
                    while(!MainActivity.isDrawerClosed) delay(233)
                    //homeHandler.sendEmptyMessage(6)    //removeAllViews
                    //homeHandler.fhib = null
                    delay(300)
                    homeHandler.startLoad()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        swiperefresh?.isRefreshing = false
    }

    override fun onDestroy() {
        super.onDestroy()
        homeHandler.destroy()
    }

    inner class ViewData(itemView: View) : RecyclerView.ViewHolder(itemView) {
        inner class RecyclerViewAdapter :
            RecyclerView.Adapter<ViewData>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewData {
                return ViewData(layoutInflater.inflate(R.layout.viewpage_horizonal, parent, false))
            }

            override fun onBindViewHolder(holder: ViewData, position: Int) {
                val thisBanner = homeHandler.index?.results?.banners?.get(position)
                thisBanner?.cover?.let {
                    if(it.isEmpty()) return@let
                    //Log.d("MyHomeFVP", "Load img: $it")
                    Glide.with(this@HomeFragment).load(
                        GlideUrl(Config.imageProxy?.wrap(it)?:it, Config.myGlideHeaders)
                    )
                        .addListener(GlideHideLottieViewListener(WeakReference(holder.itemView.lai)))
                        .timeout(60000).into(holder.itemView.vpi)
                }
                holder.itemView.vpt.text = thisBanner?.brief
                holder.itemView.vpc.setOnClickListener {
                    val bundle = Bundle()
                    homeHandler.index?.results?.banners?.get(position)?.comic?.path_word?.let { it1 -> bundle.putString("path", it1) }
                    Navigate.safeNavigateTo(findNavController(), R.id.action_nav_home_to_nav_book, bundle)
                }
            }

            override fun getItemCount(): Int = homeHandler.index?.results?.banners?.size?:0
        }
    }

    inner class ListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        inner class RecyclerViewAdapter :
            RecyclerView.Adapter<ListViewHolder>() {
            private var results: BookListStructure? = null
            var type = ""
            private var query: String? = null
            private var count = 0
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
                return ListViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.line_word, parent, false)
                )
            }

            @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
            override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
                Log.d("MyMain", "Bind open at $position")
                if (position == itemCount-1) {
                    holder.itemView.apply { post {
                        tn.setText(R.string.button_more)
                        ta.text = "搜索 \"$query\""
                        tb.text = "共 $count 条结果"
                        context?.let {
                            Glide.with(it).load(R.drawable.img_defmask)
                                .addListener(GlideHideLottieViewListener(WeakReference(laic)))
                                .timeout(60000)
                                .into(imic)
                        }
                        cic.isClickable = false
                        lwc.setOnClickListener {
                            if (query?.isNotEmpty() != true) return@setOnClickListener
                            val bundle = Bundle()
                            bundle.putCharSequence("query", query)
                            bundle.putString("type", type)
                            Navigate.safeNavigateTo(findNavController(), R.id.action_nav_home_to_nav_search, bundle)
                        }
                        lwc.layoutParams.height = fhs.width / 4
                    } }
                    return
                }
                results?.results?.list?.get(position)?.apply {
                    holder.itemView.apply { post {
                        lwi.visibility = View.VISIBLE
                        tn.text = name
                        ta.text = author.let {
                            var t = ""
                            it.forEach { ts ->
                                t += ts.name + " "
                            }
                            return@let t
                        }
                        tb.text = popular.toString()
                        cic.isClickable = false
                        context?.let {
                            Glide.with(it)
                                .load(GlideUrl(Config.imageProxy?.wrap(cover)?:cover, Config.myGlideHeaders))
                                .addListener(GlideHideLottieViewListener(WeakReference(laic)))
                                .timeout(60000).into(imic)
                        }
                        lwc.setOnClickListener {
                            val bundle = Bundle()
                            bundle.putString("path", path_word)
                            Navigate.safeNavigateTo(findNavController(), R.id.action_nav_home_to_nav_book, bundle)
                        }
                        lwc.layoutParams.height = fhs.width / 4
                    } }
                }
            }

            override fun getItemCount() = (results?.results?.list?.size?:0) + if (query?.isNotEmpty() == true) 1 else 0

            suspend fun refresh(q: CharSequence) = withContext(Dispatchers.IO) {
                query = q.toString()
                activity?.apply {
                    PausableDownloader(getString(R.string.searchApiUrl).format(0,
                        URLEncoder.encode(q.toString(), Charset.defaultCharset().name()), type, Config.platform.value)) {
                        results = Gson().fromJson(it.decodeToString(), BookListStructure::class.java)
                        count = results?.results?.total?:0
                        withContext(Dispatchers.Main) {
                            notifyDataSetChanged()
                        }
                    }.run()
                }
            }
        }
    }
}