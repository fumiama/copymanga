package top.fumiama.copymanga.ui.home

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.viewpage_horizonal.view.*
import top.fumiama.dmzj.copymanga.R
import top.fumiama.copymanga.MainActivity.Companion.mainWeakReference
import top.fumiama.copymanga.template.general.NoBackRefreshFragment
import top.fumiama.copymanga.tools.api.CMApi
import java.lang.Thread.sleep
import java.lang.ref.WeakReference

class HomeFragment : NoBackRefreshFragment(R.layout.fragment_home) {
    lateinit var homeHandler: HomeHandler

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if(isFirstInflate){
            val theme = resources.newTheme()
            swiperefresh.setColorSchemeColors(
                resources.getColor(R.color.colorAccent, theme),
                resources.getColor(R.color.colorBlue2, theme),
                resources.getColor(R.color.colorGreen, theme))

            Thread{
                homeHandler.obtainMessage(-1, true).sendToTarget()
                while(mainWeakReference?.get()?.isDrawerClosed != true) sleep(233)
                //homeHandler.sendEmptyMessage(6)    //removeAllViews
                homeHandler.fhib = null
                sleep(600)
                homeHandler.startLoad()
            }.start()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        homeHandler = HomeHandler(WeakReference(this))
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
                    //Log.d("MyHomeFVP", "Load img: $it")
                    Glide.with(this@HomeFragment).load(
                        GlideUrl(it, CMApi.myGlideHeaders)
                    ).timeout(10000).into(holder.itemView.vpi)
                }
                holder.itemView.vpt.text = thisBanner?.brief
                holder.itemView.vpc.setOnClickListener {
                    val bundle = Bundle()
                    homeHandler.index?.results?.banners?.get(position)?.comic?.path_word?.let { it1 -> bundle.putString("path", it1) }
                    rootView?.let { it1 -> Navigation.findNavController(it1).navigate(R.id.action_nav_home_to_nav_book, bundle) }
                }
            }

            override fun getItemCount(): Int = homeHandler.index?.results?.banners?.size?:0
        }
    }
}