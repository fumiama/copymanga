package top.fumiama.copymanga.template.general

import android.animation.ObjectAnimator
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.animation.doOnEnd
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.coroutines.runBlocking
import top.fumiama.copymanga.MainActivity
import top.fumiama.copymanga.tools.ui.UITools
import java.util.concurrent.atomic.AtomicBoolean

open class NoBackRefreshFragment(private val layoutToLoad: Int): Fragment() {
    private var _rootView: View? = null
    val rootView: View get() = _rootView!!
    var isFirstInflate = true
    var navBarHeight = 0
    private val disableAnimation = MainActivity.mainWeakReference?.get()?.let {
        PreferenceManager.getDefaultSharedPreferences(it)
    }?.getBoolean("settings_cat_general_sw_disable_kanban_animation", false)?:false
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if(_rootView == null) {
            isFirstInflate = true
            _rootView = inflater.inflate(layoutToLoad, container, false)
            Log.d("MyNBRF", "is first inflate")
        } else {
            isFirstInflate = false
            Log.d("MyNBRF", "not first inflate")
        }
        navBarHeight = context?.let { UITools.getNavigationBarHeight(it) } ?: 0
        return rootView
    }
    override fun onDestroy() {
        hideKanban()
        _rootView = null
        isFirstInflate = true
        Log.d("MyNBRF", "destroyed")
        super.onDestroy()
    }
    fun showKanban() {
        if (disableAnimation) return
        (activity?:(MainActivity.mainWeakReference?.get()))?.apply {cmaini?.post {
            if(cmaini?.visibility == View.GONE) {
                Log.d("MyNBRF", "show: start, set h: ${window?.decorView?.height}")
                cmaini?.translationY = window?.decorView?.height?.toFloat()?:0f
                cmaini?.visibility = View.VISIBLE
                ObjectAnimator.ofFloat(cmaini, "translationY", cmaini?.translationY?:0f, 0f).setDuration(300).start()
            }
        }
        }?:Log.d("MyNBRF", "show: null kanban ImgView")
        Log.d("MyNBRF", "show: end")
    }
    private var isHideRunning = AtomicBoolean()
    fun hideKanban() {
        if (disableAnimation) return
        (activity?:(MainActivity.mainWeakReference?.get()))?.apply { cmaini?.post {
            if(!isHideRunning.get() && cmaini?.visibility == View.VISIBLE) {
                isHideRunning.set(true)
                Log.d("MyNBRF", "hide: start, set h: ${window?.decorView?.height}")
                ObjectAnimator.ofFloat(cmaini, "translationY", 0f, window?.decorView?.height?.toFloat()?:0f).setDuration(300).also {
                    it.doOnEnd {
                        cmaini?.visibility = View.GONE
                        isHideRunning.set(false)
                        Log.d("MyNBRF", "hide: set gone")
                    }
                }.start()
             }
         } }?:Log.d("MyNBRF", "hide: null kanban ImgView")
        Log.d("MyNBRF", "hide: end")
    }
}
