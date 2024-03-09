package top.fumiama.copymanga.template.general

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import top.fumiama.copymanga.tools.ui.UITools

open class NoBackRefreshFragment(private val layoutToLoad: Int): Fragment() {
    private var _rootView: View? = null
    val rootView: View get() = _rootView!!
    var isFirstInflate = true
    var navBarHeight = 0
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
        super.onDestroy()
        _rootView = null
        isFirstInflate = true
    }
}
