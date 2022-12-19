package top.fumiama.copymanga.template.general

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

open class NoBackRefreshFragment(private val layoutToLoad: Int): Fragment() {
    private var _rootView: View? = null
    val rootView: View get() = _rootView!!
    var isFirstInflate = true
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if(_rootView == null){
            isFirstInflate = true
            _rootView = inflater.inflate(layoutToLoad, container, false)
            Log.d("MyNBRF", "is first inflate")
        } else {
            isFirstInflate = false
            Log.d("MyNBRF", "not first inflate")
        }
        return rootView
    }
    override fun onDestroy() {
        super.onDestroy()
        _rootView = null
        isFirstInflate = true
    }
}
